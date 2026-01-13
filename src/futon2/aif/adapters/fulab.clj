(ns futon2.aif.adapters.fulab
  "AIF adapter for fulab (fucodex/fuclaude) with proper softmax sampling.

   Implements deterministic stochastic policy sampling:
   - Given candidates and G scores, computes softmax probabilities over -G/tau
   - Uses seeded RNG for reproducibility: seed = hash(session-id, turn, candidates)
   - Supports abstain policy when tau < min-sample threshold"
  (:require [clojure.string :as str]
            [futon2.aif.adapter :as adapter])
  (:import [java.util Random]))

(defn- text-score [value]
  (cond
    (string? value) (count (str/split value #"\s+"))
    (coll? value) (count value)
    :else 1))

(defn- uncertainty-score [value]
  (cond
    (nil? value) 1.0
    (number? value) (double (max 0.1 value))
    (string? value) (double (max 1 (count (str/split value #"\s+"))))
    (coll? value) (double (max 1 (count value)))
    :else 1.0))

(def default-config
  {:g/weights {:base 0.1
               :anchors 0.05
               :forecast 0.02}
   :evidence/weights {:read -0.02
                      :off-trail 0.12
                      :implement -0.08
                      :update -0.05}
   :evidence/min -0.3
   :evidence/max 0.3
   :tau/scale 1.0
   :tau/min 0.1
   :tau/max 2.0
   :tau/min-sample 0.55})  ;; below this, abstain from auto-selection

(defn- clamp [value min-val max-val]
  (-> value (max min-val) (min max-val)))


(defn- compute-g [candidate context config]
  (let [{:keys [anchors forecast]} (:g/weights config)
        ;; Use candidate score directly as G base (lower is better)
        candidate-score (get (:candidate-scores context) candidate 0.5)
        anchor-score (text-score (:anchors context))
        forecast-score (text-score (:forecast context))]
    ;; G = candidate score + small adjustments from anchors/forecast
    (+ (if (number? candidate-score)
         (double candidate-score)
         (double (/ (text-score candidate) 10.0)))

(defn- normalize-action [action]
  (cond
    (keyword? action) action
    (string? action) (keyword action)
    :else :unknown))

(defn- candidate-base-score [candidate context]
  (if-let [score (get-in context [:candidate-scores candidate])]
    (double score)
    (text-score candidate)))

(defn- evidence-score [state candidate config]
  (let [weights (merge (:evidence/weights default-config)
                       (:evidence/weights config))
        counts (get-in state [:pattern-evidence candidate])
        raw (reduce-kv (fn [acc action count]
                         (+ acc (* (double (get weights action 0.0))
                                   (double (or count 0)))))
                       0.0
                       (or counts {}))]
    (clamp raw
           (double (or (:evidence/min config) (:evidence/min default-config)))
           (double (or (:evidence/max config) (:evidence/max default-config))))))

(defn- compute-g [candidate state context config]
  (let [{:keys [base anchors forecast]} (:g/weights config)
        base-score (candidate-base-score candidate context)
        anchor-score (text-score (:anchors context))
        forecast-score (text-score (:forecast context))
        evidence (evidence-score state candidate config)]
    (+ (* base (+ base-score evidence))
>>>>>>> c4b4c74 (Accept numeric uncertainty for AIF tau)
       (* anchors anchor-score)
       (* forecast forecast-score))))

(defn- compute-tau [context config]
<<<<<<< HEAD
  (let [uncertainty (if-let [u (:uncertainty context)]
                      (double (max 0.1 u))
                      1.0)
=======
  (let [uncertainty (uncertainty-score (:uncertainty context))
>>>>>>> c4b4c74 (Accept numeric uncertainty for AIF tau)
        tau (double (/ (:tau/scale config) uncertainty))]
    (clamp tau (:tau/min config) (:tau/max config))))

;; Softmax sampling implementation

(defn- compute-seed
  "Deterministic seed from session-id, turn, and candidates."
  [context]
  (let [session-id (or (:session/id context) "default")
        turn (or (:turn context)
                 (some-> (:decision/id context) (str/split #":turn-") last))
        candidates (sort (:candidates context))]
    (hash [session-id turn candidates])))

(defn- softmax-probs
  "Compute softmax probabilities over -G/tau (lower G = higher prob).
   Returns map of {candidate-id probability}."
  [g-scores tau]
  (when (seq g-scores)
    (let [logits (map (fn [[id g]] [id (/ (- (double g)) (double tau))]) g-scores)
          max-logit (apply max (map second logits))
          ;; Subtract max for numerical stability
          exp-logits (map (fn [[id l]] [id (Math/exp (- (double l) max-logit))]) logits)
          z (reduce + (map second exp-logits))]
      (into {} (map (fn [[id e]] [id (/ (double e) z)]) exp-logits)))))

(defn- sample-from-probs
  "Sample a candidate given probabilities and a seeded random value [0,1)."
  [probs rng-value]
  (when (seq probs)
    (let [sorted (sort-by (comp - val) probs)]  ;; Sort by descending probability
      (loop [[[id p] & rest] sorted
             cumulative 0.0]
        (let [cumulative (+ cumulative (double p))]
          (if (or (nil? rest) (< rng-value cumulative))
            id
            (recur rest cumulative)))))))

(defrecord FulabAdapter [config]
  adapter/AifAdapter
  (select-pattern [_ _state context]
    (let [candidates (vec (:candidates context))
          scored (into {}
                       (for [c candidates]
                         [c (compute-g c context config)]))
          tau (compute-tau context config)
          min-sample (or (:tau/min-sample config) 0.55)
          abstain? (< tau min-sample)
          ;; Compute softmax probabilities
          probs (softmax-probs scored tau)
          ;; Deterministic sampling
          seed (compute-seed context)
          rng (Random. (long seed))
          rng-value (.nextDouble rng)
          sampled (sample-from-probs probs rng-value)
          ;; Use explicit choice if provided, otherwise sampled
          ;; chosen (or (:chosen context) sampled)
          ;; sampled? (and (nil? (:chosen context)) (some? sampled))
          ;;                [c (compute-g c _state context config)]))
          chosen (or (:chosen context)
                     (when (seq candidates)
                       (first (sort-by scored candidates))))
          tau (compute-tau context config)]
      (let [result {:decision/id (:decision/id context)
                    :candidates candidates
                    :chosen chosen
                    :aif {:G-chosen (get scored chosen)
                          :G-rejected (apply dissoc scored [chosen])
                          :tau tau
                          :min-sample min-sample
                          :probs probs
                          :sampled? sampled?
                          :abstain? abstain?
                          :seed seed
                          :belief-id (or (:belief-id context) (:decision/id context))}}]
        (tap> (merge {:type :aif/fulab
                      :event :select
                      :session/id (:session/id context)}
                     result))
        result)))
  (update-beliefs [_ _state observation]
    (if (and (:pattern/id observation) (:pattern/action observation))
      (let [pattern-id (:pattern/id observation)
            action (normalize-action (:pattern/action observation))
            prev-score (evidence-score _state pattern-id config)
            updated (update-in _state [:pattern-evidence pattern-id action] (fnil inc 0))
            next-score (evidence-score updated pattern-id config)
            counts (get-in updated [:pattern-evidence pattern-id])
            tau (compute-tau observation config)
            result {:aif/state updated
                    :aif {:tau-updated tau
                          :evidence-score next-score
                          :evidence-delta (when (and (number? next-score) (number? prev-score))
                                            (- (double next-score) (double prev-score)))
                          :evidence-counts counts
                          :belief-delta {:decision/id (:decision/id observation)
                                         :pattern/id pattern-id
                                         :action action
                                         :status (or (:status observation) :observed)}}}]
        (tap> (merge {:type :aif/fulab
                      :event :pattern-action
                      :session/id (:session/id observation)}
                     result))
        result)
      (let [error (double (max 0.0 (dec (text-score (:outcome observation)))))
            tau (compute-tau observation config)]
        (let [result {:aif/state {:belief-updated true}
                      :aif {:prediction-error error
                            :tau-updated tau
                            :belief-delta {:decision/id (:decision/id observation)
                                           :status (or (:status observation) :unknown)}}}]
          (tap> (merge {:type :aif/fulab
                        :event :update
                        :session/id (:session/id observation)}
                       result))
          result)))))

(defn new-adapter
  ([] (->FulabAdapter default-config))
  ([config] (->FulabAdapter (merge default-config config))))
