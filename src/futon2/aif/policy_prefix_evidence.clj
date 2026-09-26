(ns futon2.aif.policy-prefix-evidence
  "H4 staged observed-prefix arithmetic. Synthetic evaluation is NOT admission.
   Production remains not-supplied until D owns and admits the policy/execution/
   observation join. Per-step redraw only; F is unscaled in ln E-F-G/beta."
  (:require [futon2.aif.observation-model :as om]
            [futon2.aif.policy-prefix-admission :as admission]))

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

(defn- not-supplied [entry conditioning reason]
  ;; the pending dependency is named only while no admission ran; once it has,
  ;; the absence is the admission's own reason
  (cond-> {:schema :wm/policy-prefix-evidence-v1 :policy (:action entry)
           :status :not-supplied :z-semantics :per-step-redraw
           :reason reason
           :conditioning (or conditioning {:conditioning-status :not-wired})}
    (= :no-admitted-policy-prefix reason) (assoc :pending-dependency pending-dependency)))

(defn prefix-f
  "F1c-I: the F of ENTRY's candidate from its admitted observed prefix PREFIX
  (policy-prefix-admission/admit's result for this candidate), as the
  :f-prefix the selection reads. The consumer verifies what it sums:
    - the prefix names the entry's own policy key (the same cascade-prior
      scheme), else :invalid-policy-prefix :foreign-prefix;
    - each admitted step's sPrev is the previous step's q, else
      :invalid-policy-prefix :chain-broken with the index;
  then: a step whose :f is :contradiction (P(o) = 0 on the prefix) makes the
  policy :zero-support (weight 0 under selection-posterior); otherwise
  :computed with :f the exact sum of the steps' :f (SPEC-F s1: an unweighted
  sum over the observed history, not the last summand). An empty prefix is
  :not-supplied with the admission's :conditioning-status as the reason: a
  never-executed policy borrows no history (SPEC-L)."
  [entry prefix conditioning]
  (let [steps (vec (:observation-updates prefix))
        k (admission/candidate-key (:action entry))
        base {:schema :wm/policy-prefix-evidence-v1 :policy (:action entry)
              :z-semantics :per-step-redraw :presence :prefix-receipt}
        broken (first (keep-indexed (fn [i [a b]] (when (not= (get-in b [:s-prev :value]) (:q a)) (inc i)))
                                    (partition 2 1 steps)))
        contradiction (first (filter #(= :contradiction (:f %)) steps))]
    (cond
      (empty? steps)
      (not-supplied entry conditioning (or (:conditioning-status prefix) :no-admitted-policy-prefix))

      (not= k (:policy-key prefix))
      (assoc base :status :invalid :kind :invalid-policy-prefix :reason :foreign-prefix
             :prefix-key (:policy-key prefix) :candidate-key k)

      broken
      (assoc base :status :invalid :kind :invalid-policy-prefix :reason :chain-broken :index broken)

      contradiction
      (assoc base :status :zero-support :reason :contradiction :at (:occurrence contradiction)
             :steps (count steps) :probability 0)

      (not-every? #(number? (:f %)) steps)
      (assoc base :status :invalid :kind :invalid-policy-prefix :reason :non-numeric-f)

      :else
      (assoc base :status :computed
             :f (reduce + 0.0 (map :f steps))
             :steps (count steps)
             :occurrences (mapv :occurrence steps)
             :posterior (:q (peek steps))
             :prefix-conditioning-status (:conditioning-status prefix)))))

(defn production-ranked
  "Attach each ranked entry's :f-prefix. Without PREFIXES (the 2-arity, and
   any caller that has no admitted prefixes) every entry is :not-supplied with
   the reason chain preserved: caller options and synthetic receipts cannot
   authorize nonempty history. With PREFIXES ({candidate-id prefix}, the
   tick's policy-prefix-admission/prefixes), each entry's F comes from its own
   admitted prefix (prefix-f). :f is removed either way: F reaches the
   selection only through :f-prefix."
  ([ranked conditioning] (production-ranked ranked conditioning nil))
  ([ranked conditioning prefixes]
   (with-meta
     (mapv (fn [entry]
             (let [ranked-entry (dissoc entry :f)]
               (assoc ranked-entry :f-prefix
                      (if-let [prefix (and prefixes (get prefixes (:id (:action entry))))]
                        (let [fp (prefix-f entry prefix conditioning)]
                          ;; an invalid prefix is refused as data, never
                          ;; scored: the selection sees it :not-supplied
                          (if (= :invalid (:status fp))
                            (assoc (not-supplied entry conditioning :invalid-policy-prefix) :refused fp)
                            fp))
                        (not-supplied entry conditioning :no-admitted-policy-prefix)))))
           ranked)
     (meta ranked))))
