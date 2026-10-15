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

(defn certified-zero-history
  "The applicability carrier for a policy the pinned flight ledger proves has
  never run: CENSUS (policy-prefix-admission/history-census) is complete,
  its digest matches its content, and no recorded step names ENTRY's policy
  key. The historical-prefix term is then outside its domain, which is a
  positive finding, distinct from :not-supplied (history unknown or
  unreadable) and from a numeric F (there is no empty-sum 0 here). Returns
  nil whenever the census cannot prove zero history."
  [entry census]
  (let [k (admission/candidate-key (:action entry))]
    (when (= 0 (admission/census-history-count census k))
      {:schema :wm/policy-prefix-evidence-v1 :policy (:action entry)
       :status :not-applicable :reason :certified-zero-history
       :presence :applicability-certificate :z-semantics :per-step-redraw
       :policy-key k :history-count 0
       :ledger {:digest (:digest census) :dir (:dir census)
                :records (count (:records census))}
       :census census})))

(defn certified-zero-history?
  "True iff PREFIX is a valid certified-zero-history carrier for POLICY: the
  carrier's own census still recomputes to its digest, the digest is the one
  the carrier pins, the policy key is POLICY's, and that census has no step
  for it. A forged or edited census, an incomplete one, or one naming this
  policy fails."
  [prefix policy]
  (let [k (admission/candidate-key policy)
        census (:census prefix)]
    (and (= :wm/policy-prefix-evidence-v1 (:schema prefix))
         (= :not-applicable (:status prefix))
         (= :certified-zero-history (:reason prefix))
         (= :applicability-certificate (:presence prefix))
         (= policy (:policy prefix))
         (some? k) (= k (:policy-key prefix))
         (= 0 (:history-count prefix))
         (= (:digest census) (get-in prefix [:ledger :digest]))
         (= 0 (admission/census-history-count census k)))))

(defn production-ranked
  "Attach each ranked entry's :f-prefix. Without PREFIXES (the 2-arity, and
   any caller that has no admitted prefixes) every entry is :not-supplied with
   the reason chain preserved: caller options and synthetic receipts cannot
   authorize nonempty history. With PREFIXES ({candidate-id prefix}, the
   tick's policy-prefix-admission/prefixes), keyed by the complete policy key,
   each entry's F comes from its own
   admitted prefix (prefix-f). With CENSUS (the pinned ledger census those
   prefixes were read from), an entry left :not-supplied whose policy the
   census proves has no recorded step gets the certified-zero-history
   carrier instead; a policy with any recorded step keeps its own outcome.
   :f is removed either way: F reaches the selection only through :f-prefix."
  ([ranked conditioning] (production-ranked ranked conditioning nil nil))
  ([ranked conditioning prefixes] (production-ranked ranked conditioning prefixes nil))
  ([ranked conditioning prefixes census]
   (with-meta
     (mapv (fn [entry]
             (let [ranked-entry (dissoc entry :f)
                   fp (if-let [prefix (and prefixes
                                           (get prefixes
                                                (admission/candidate-key (:action entry))))]
                        (let [fp (prefix-f entry prefix conditioning)]
                          ;; an invalid prefix is refused as data, never
                          ;; scored: the selection sees it :not-supplied
                          (if (= :invalid (:status fp))
                            (assoc (not-supplied entry conditioning :invalid-policy-prefix) :refused fp)
                            fp))
                        (not-supplied entry conditioning :no-admitted-policy-prefix))]
               (assoc ranked-entry :f-prefix
                      (or (when (and census (= :not-supplied (:status fp))
                                     (not (:refused fp)))
                            (certified-zero-history entry census))
                          fp))))
           ranked)
     (meta ranked))))

(defn consumed-canonical-f?
  "True iff selection CANDIDATE (a selection-certificate :candidates row)
  consumed one of the two F carriers that may cross into enactment: a
  computed finite Prefix-F of its own policy, or the certified-zero-history
  carrier with the F term recorded as omitted for that reason. :not-supplied,
  zero-support, an edited carrier, or an F value that does not match the
  carrier all fail."
  [candidate]
  (let [prefix (:f-prefix candidate)
        input (get-in candidate [:inputs :f])
        f (:f prefix)]
    (and (= :wm/policy-prefix-evidence-v1 (:schema prefix))
         (= (:id candidate) (:policy prefix))
         (case (:status prefix)
           :computed
           (and (number? f) (Double/isFinite (double f))
                (= f (:f candidate))
                (= :computed (:f-status candidate))
                (= f (:f-consumed candidate))
                (= {:status :computed :value f :presence :prefix-receipt :reason nil}
                   (select-keys input [:status :value :presence :reason])))

           :not-applicable
           (and (certified-zero-history? prefix (:id candidate))
                (nil? (:f candidate))
                (= :not-applicable (:f-status candidate))
                (= {:status :not-applicable :reason :certified-zero-history}
                   (select-keys (:f-consumed candidate) [:status :reason]))
                (= {:status :not-applicable :value nil
                    :presence :applicability-certificate
                    :reason :certified-zero-history}
                   (select-keys input [:status :value :presence :reason])))

           false))))
