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

(def default-config
  {:g/weights {:base 0.1
               :anchors 0.05
               :forecast 0.02}
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
       (* anchors anchor-score)
       (* forecast forecast-score))))

(defn- compute-tau [context config]
  (let [uncertainty (if-let [u (:uncertainty context)]
                      (double (max 0.1 u))
                      1.0)
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
          chosen (or (:chosen context) sampled)
          sampled? (and (nil? (:chosen context)) (some? sampled))]
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
        result))))

(defn new-adapter
  ([] (->FulabAdapter default-config))
  ([config] (->FulabAdapter (merge default-config config))))
