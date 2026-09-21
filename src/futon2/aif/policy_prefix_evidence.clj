(ns futon2.aif.policy-prefix-evidence
  "H4 staged observed-prefix arithmetic. Synthetic evaluation is NOT admission.
   Production remains not-supplied until D owns and admits the policy/execution/
   observation join. Per-step redraw only; F is unscaled in ln E-F-G/beta."
  (:require [futon2.aif.observation-model :as om]))

(def pending-dependency :d-conditioning-consumption-and-policy-prefix-admission)

(defn- require! [p kind]
  (when-not p (throw (ex-info (name kind) {:status :invalid :kind kind}))))

(defn- distribution? [q]
  (and (map? q) (seq q)
       (every? #(and (or (integer? %) (ratio? %)) (<= 0 %)) (vals q))
       (= 1 (reduce +' 0 (vals q)))))

(defn evaluate-synthetic
  "Evaluate a fixed-model prefix once, in order. Each step contains a declared
   transition row per supported state, context, and a subsequently observed
   event. Retain every consumed pre-prediction/posterior, never rescore the
   posterior against that observation. This result grants no live authority."
  [{:keys [policy model prior steps z-semantics]}]
  (let [base {:schema :wm/policy-prefix-evidence-v1 :policy policy
              :scope :synthetic-prefix-arithmetic-only :model model
              :z-semantics z-semantics}]
    (try
      (require! (= :per-step-redraw z-semantics) :unsupported-z-semantics)
      (require! (some? policy) :missing-policy-identity)
      (om/validate! model)
      (require! (distribution? prior) :invalid-prefix-prior)
      (require! (and (vector? steps) (seq steps)) :missing-observed-prefix)
      (loop [q prior remaining steps seen #{} receipts [] total 0.0]
        (if-let [{:keys [transition context observation]} (first remaining)]
          (let [occ (:occurrence-id context)]
            (require! (and (some? occ) (not (contains? seen occ))) :duplicate-or-missing-occurrence)
            (require! (= (inc (count receipts)) (:tau context)) :prefix-horizon-mismatch)
            (require! (every? #(distribution? (get transition %)) (keys q)) :invalid-prefix-transition)
            (let [prediction (reduce-kv
                              (fn [out s mass]
                                (merge-with +' out (update-vals (get transition s) #(*' mass %))))
                              {} q)
                  result (om/query model {:op :condition :belief prediction
                                          :context context :observation observation})
                  receipt {:context context :observation observation :transition transition
                           :prior q :prediction prediction :result result}
                  retained (conj receipts receipt)]
              (case (:status result)
                :computed (recur (:posterior result) (next remaining) (conj seen occ)
                                 retained (+ total (:f result)))
                :contradiction (assoc base :status :zero-support :reason :impossible-observed-prefix
                                      :steps retained :probability 0)
                (assoc base :status :invalid :kind (:kind result) :steps retained))))
          (assoc base :status :computed :f total :posterior q :steps receipts)))
      (catch clojure.lang.ExceptionInfo e (merge base (ex-data e))))))

(defn production-ranked
  "Stage the default route without inventing admission. Caller options and
   synthetic receipts cannot authorize nonempty history. Preserve reason chain."
  [ranked conditioning]
  (with-meta
    (mapv (fn [entry]
            (-> entry
                (dissoc :f)
                (assoc :f-prefix
                       {:schema :wm/policy-prefix-evidence-v1 :policy (:action entry)
                        :status :not-supplied :z-semantics :per-step-redraw
                        :reason :no-admitted-policy-prefix
                        :conditioning (or conditioning {:conditioning-status :not-wired})
                        :pending-dependency pending-dependency}))) ranked)
    (meta ranked)))
