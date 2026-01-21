(ns futon2.aif.adapters.fulab
  "AIF adapter for fulab (fucodex/fuclaude/fubar) with softmax sampling.

   Implements deterministic stochastic policy sampling:
   - Given candidates and G scores, computes softmax probabilities over -G/tau
   - Uses seeded RNG for reproducibility: seed = hash(session-id, decision-id, candidates)
   - Supports abstain policy when tau < min-sample threshold
   - Tracks pattern evidence to adjust G scores over time"
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
   :tau/min-sample 0.55})

(defn- clamp [value min-val max-val]
  (-> value (max min-val) (min max-val)))

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
        raw (reduce-kv (fn [acc action cnt]
                         (+ acc (* (double (get weights action 0.0))
                                   (double (or cnt 0)))))
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
       (* anchors anchor-score)
       (* forecast forecast-score))))

(defn- compute-tau [context config]
  (let [uncertainty (uncertainty-score (:uncertainty context))
        error (double (max 0.0 (or (:prediction-error context) 0.0)))
        combined (+ uncertainty error)
        tau (double (/ (:tau/scale config) (max 1.0e-6 combined)))]
    (clamp tau (:tau/min config) (:tau/max config))))

;; Softmax sampling implementation

(defn- stable-seed [context]
  (let [sid (:session/id context)
        decision (:decision/id context)
        candidates (:candidates context)
        basis (str sid "|" decision "|" (str/join "," (sort candidates)))]
    (long (hash basis))))

(defn- logits-from-g [g-map tau]
  (into {}
        (map (fn [[k g]]
               [k (/ (- (double g)) (double tau))]))
        g-map))

(defn- softmax [logits]
  (let [values (vals logits)]
    (if (seq values)
      (let [max-logit (apply max values)
            exp-map (into {}
                          (map (fn [[k v]]
                                 [k (Math/exp (- (double v) (double max-logit)))]))
                          logits)
            total (reduce + (vals exp-map))]
        (into {}
              (map (fn [[k v]]
                     [k (if (pos? total)
                          (/ (double v) (double total))
                          0.0)]))
              exp-map))
      {})))

(defn- sample-choice [probs seed]
  (when (seq probs)
    (let [sorted (sort-by key probs)
          rng (Random. (long seed))
          target (.nextDouble rng)]
      (loop [remaining sorted
             acc 0.0]
        (when-let [[k p] (first remaining)]
          (let [next-acc (+ acc (double p))]
            (if (<= target next-acc)
              k
              (recur (rest remaining) next-acc))))))))

(defrecord FulabAdapter [config]
  adapter/AifAdapter
  (select-pattern [_ state context]
    (let [candidates (vec (:candidates context))
          scored (into {}
                       (for [c candidates]
                         [c (compute-g c state context config)]))
          tau (compute-tau context config)
          seed (or (:seed context) (stable-seed context))
          min-sample (double (or (:tau/min-sample config) 0.55))
          logits (when (and (seq candidates) (pos? tau))
                   (logits-from-g scored tau))
          probs (when (map? logits) (softmax logits))
          abstain? (and (nil? (:chosen context))
                        (number? tau)
                        (< (double tau) min-sample))
          sampled (when (and (not abstain?) (nil? (:chosen context)) (seq probs))
                    (sample-choice probs seed))
          chosen (cond
                   (:chosen context) (:chosen context)
                   abstain? nil
                   sampled sampled
                   (seq candidates) (first (sort-by scored candidates))
                   :else nil)
          sampled? (and sampled (nil? (:chosen context)))
          result {:decision/id (:decision/id context)
                  :candidates candidates
                  :chosen chosen
                  :aif {:G-chosen (get scored chosen)
                        :G-rejected (apply dissoc scored [chosen])
                        :G-scores scored
                        :tau tau
                        :logits logits
                        :probs probs
                        :seed seed
                        :sampled? sampled?
                        :abstain? abstain?
                        :min-sample min-sample
                        :belief-id (or (:belief-id context) (:decision/id context))}}]
      (tap> (merge {:type :aif/fulab
                    :event :select
                    :session/id (:session/id context)}
                   result))
      result))

  (update-beliefs [_ state observation]
    (if (and (:pattern/id observation) (:pattern/action observation))
      ;; Pattern action observation - update evidence counts
      (let [pattern-id (:pattern/id observation)
            action (normalize-action (:pattern/action observation))
            prev-score (evidence-score state pattern-id config)
            updated (update-in state [:pattern-evidence pattern-id action] (fnil inc 0))
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
      ;; Generic observation - compute prediction error
      (let [error (double (max 0.0 (dec (text-score (:outcome observation)))))
            tau (compute-tau (assoc observation :prediction-error error) config)
            result {:aif/state {:belief-updated true}
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
