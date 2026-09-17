(ns futon2.aif.check-candidates
  "H7b of SPEC-flat-removal-and-cascade-decision (p4ng
  wm-walkthroughs/build-loop/closure/, section H7; Joe approved D3 with
  amendments, p4ng eec2c9f, PROPOSAL-pattern-interpretation.md):

    - an unknown fact is a belief;
    - checks are candidates;
    - an unknown fact is never a terminal refusal.

  check-patterns turns the unknown facts of a belief into CHECK CANDIDATES:
  for every fact that is :unknown AND gates a step towards the want — a
  pattern that could establish a want token directly or through the produces
  of other patterns — it emits one check pattern

    {:id :check/<fact> :kind :check :fact <f>
     :guard {:unknown #{<f>}} :opens <f> :produces #{} :theta <check-theta>}

  The check establishes an OBSERVATION, not an effect: its value comes from
  the observation channel (H7a active-horizon-g opens the fact), never from
  produces. :theta is the check's probability of returning an answer; it is
  REQUIRED as :check-theta in the input and never silently defaulted — its
  absence is the typed refusal :check-theta-required.

  Unknown facts that gate nothing get no check; they are recorded under
  :not-gating. Known facts (true/false) never yield checks.

  Guard shapes accepted, because tick records carry both: the manifest
  pattern shape {:guard {:clauses [{:present #{} :absent #{}}]}} and the
  on-the-fly token shape {:guard {:needs #{} :forbids #{}}} (03-R6.edn)."
  (:require [clojure.set :as set]))

(defn- guard-facts
  "Every fact a pattern's guard tests, present or absent, from either guard
  shape. nil when the guard is in neither known shape."
  [pattern]
  (let [g (:guard pattern)]
    (cond
      (seq (:clauses g))
      (reduce set/union #{} (map #(set/union (set (:present %)) (set (:absent %)))
                                 (:clauses g)))

      (or (contains? g :needs) (contains? g :forbids))
      (set/union (set (:needs g)) (set (:forbids g)))

      :else nil)))

(defn- guard-shape-error
  "The pattern's :id when its guard is in neither known shape."
  [pattern]
  (when (nil? (guard-facts pattern)) (:id pattern)))

(defn- check-refusal [law data]
  (throw (ex-info "check-patterns refused"
                  (merge {:finding :check-candidates/refusal :law law} data))))

(defn- enabling-facts
  "The facts a pattern's guard needs PRESENT, from either guard shape. A
  pattern that produces one of these is a step towards enabling it."
  [pattern]
  (let [g (:guard pattern)]
    (if (seq (:clauses g))
      (reduce set/union #{} (map #(set (:present %)) (:clauses g)))
      (set (:needs g)))))

(defn- relevant-patterns
  "Backward fixpoint from the want: a pattern is relevant if it produces a
  want token directly, or produces a fact that a relevant pattern's guard
  needs present (it enables a step towards the want). Producing a token a
  relevant guard FORBIDS disables that step, so it does not make a pattern
  relevant."
  [patterns want]
  (let [want (set want)
        produce-want? (fn [p] (seq (set/intersection (set (:produces p)) want)))]
    (loop [relevant (into {} (map (juxt :id identity))
                          (filter produce-want? patterns))]
      (let [needed (reduce set/union #{} (map enabling-facts (vals relevant)))
            newly (filter (fn [p]
                            (and (not (contains? relevant (:id p)))
                                 (seq (set/intersection (set (:produces p)) needed))))
                          patterns)]
        (if (empty? newly)
          (vals relevant)
          (recur (into relevant (map (juxt :id identity)) newly)))))))

(defn check-patterns
  "Input map:
    :facts       {fact true | false | :unknown}
    :want        coll of want tokens (non-empty)
    :patterns    coll of patterns with guards (either shape, see ns docstring)
    :check-theta REQUIRED number in [0,1]: the check's probability of
                 returning an answer. Typed refusal :check-theta-required
                 when absent, :invalid-check-theta when out of range.

  Returns {:checks [...] :not-gating [...] :basis {...}}."
  [{:keys [facts want patterns check-theta]}]
  (when-not (and (map? facts) (seq want) (sequential? patterns))
    (check-refusal :invalid-input-shape {:facts (map? facts)
                                         :want (boolean (seq want))
                                         :patterns (sequential? patterns)}))
  (when-not (some? check-theta)
    (check-refusal :check-theta-required
                   {:action :declare
                    :check-theta "probability the check returns an answer"}))
  (when-not (and (number? check-theta) (<= 0 check-theta 1))
    (check-refusal :invalid-check-theta {:check-theta check-theta}))
  (let [patterns (mapv #(update % :produces (fnil set #{})) patterns)
        _ (when-let [bad (seq (keep guard-shape-error patterns))]
            (check-refusal :unknown-guard-shape {:patterns (vec bad)}))
        relevant (relevant-patterns patterns want)
        gating (reduce set/union #{} (map guard-facts relevant))
        unknown (set (keep (fn [[f v]] (when (identical? v :unknown) f)) facts))
        gating-unknown (set/intersection unknown gating)]
    {:checks (mapv (fn [f]
                     {:id (keyword "check" (name f))
                      :kind :check
                      :fact f
                      :guard {:unknown #{f}}
                      :opens f
                      :produces #{}
                      :theta check-theta})
                   (sort gating-unknown))
     :not-gating (vec (sort (set/difference unknown gating)))
     :basis {:decision "D3 amendments (p4ng eec2c9f)"
             :unknown-fact-is :belief
             :unknown-is-never :terminal-refusal
             :value-source :observation-channel-H7a
             :produces #{} :produces-note "a check establishes an observation, not an effect"
             :theta check-theta :theta-source :declared-input
             :gating-rule "unknown fact in the guard (present or absent) of a pattern that could establish a want token directly or through other patterns' produces"}}))
