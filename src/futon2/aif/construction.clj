(ns futon2.aif.construction
  "H7c-1 of SPEC-flat-removal-and-cascade-decision (p4ng
  wm-walkthroughs/build-loop/closure/SPEC-…, section H7). Joe's rulings of
  2026-09-17:

    - a cascade is a policy and G is computed over policies;
    - 'unknown' means find out, not do nothing (D3 amendments);
    - construction is a policy for creating policies: epistemic, with a
      definition of done, never endless construction without work;
    - action-dependent observation channels are approved (H7a);
    - the initial horizon is T = 2.

  `construct` runs the construction-level policy whose moves are injected
  functions (H7c-2 implements the futon3 cascade-construction library's
  moves; here the caller injects them) and whose stopping rule is
  hand-over-when-acting-is-worth-more
  (futon3/library/cascade-construction/hand-over-when-acting-is-worth-more.flexiarg):
  stop and hand the best family to selection when the best move's value
  is <= 0 — further construction is worth no more than acting on the
  current best family.

  Value of a move = (G of best candidate in the current family − G of best
  candidate in the proposed family) + epistemic value − cost. The
  pragmatic part comes from :evaluate-g; production passes
  futon2.aif.active-horizon-g/active-horizon-g, whose action-dependent
  channels already value checks' state information — so a :state-information
  estimate is RECORDED but never added a second time. Novelty
  (interpretation or parameter information) is not formalised; it enters
  only as a recorded typed estimate {:kind :novelty …} and marks the
  value :includes-unformalised-novelty true.

  Unknowns are not held in construction: gating unknown facts become CHECK
  CANDIDATES (futon2.aif.check-candidates) added to the family before
  scoring, so they are resolved by acting.

  The receipt is what futon2.aif.cascade-problems accepts as
  :construction-receipt. Stopping is not target success."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.check-candidates :as cc]))

(defn- refusal [kind & [data]]
  (merge {:status :refused :kind kind} data))

(defn- g-norm
  "Ordering value of a G result: :infinite is worst (largest)."
  [g]
  (if (identical? g :infinite) ##Inf (double g)))

(defn- best-g
  "G of the best candidate in FAMILY under :evaluate-g (lower is better)."
  [evaluate-g family]
  (when (seq family)
    (reduce (fn [b c]
              (let [g (evaluate-g c)]
                (if (< (g-norm g) (g-norm b)) g b)))
            (evaluate-g (first family))
            (rest family))))

(defn- established-tokens
  "Tokens already established in q0. q0 is accepted as a coll of tokens
  (a tick-like token state). Anything else establishes nothing here."
  [q0]
  (if (sequential? q0) (set q0) #{}))

(defn- checks-for
  "Gating unknown facts as check candidates, through check-candidates.
  Returns check-candidates' {:checks […] :not-gating […]} or a typed
  refusal. Not supplied ⇒ no checks, honestly skipped."
  [{:keys [facts want patterns check-theta]}]
  (if (or (nil? facts) (nil? want) (nil? patterns))
    {:checks [] :not-gating [] :skipped :not-supplied}
    (try
      (cc/check-patterns {:facts facts :want want
                          :patterns patterns :check-theta check-theta})
      (catch clojure.lang.ExceptionInfo e
        (refusal :check-candidates-refused (:law (ex-data e)))))))

(defn- augment-with-checks
  "Add CHECKS to every candidate of FAMILY: each candidate's :patterns gains
  the check patterns and its :precedence gains their ids, so the checks can
  act inside the rollout (active-horizon-g's acting check) and unknowns are
  found out by acting, not by constructing further."
  [family checks]
  (if (empty? checks)
    family
    (mapv (fn [c]
            (let [c (cond-> c
                      (nil? (:precedence c)) (assoc :precedence [])
                      (nil? (:patterns c)) (assoc :patterns []))]
              (-> c
                  (update :precedence into (map :id checks))
                  (update :patterns into checks))))
          family)))

(defn- move-cost
  "A move's declared :cost, else (:cost-of move-id), else 0 (recorded)."
  [result cost-of]
  (cond (number? (:cost result)) (:cost result)
        (ifn? cost-of) (cost-of (:move-id result))
        :else 0))

(defn- evaluate-moves
  "Evaluate every move against FAMILY. Returns
  {:evaluations [{:move-id … :value … :proposed-family … :parts {…}} …]
   :no-move-reasons {move-id reason …}}
  where value = pragmatic + epistemic − cost.

  Which epistemic estimates are ADDED:
  - :state-information is recorded and NOT added — active-horizon-g already
    values a check's state information, so adding it here would count it
    twice;
  - :novelty (interpretation novelty) is added and marks the value
    :includes-unformalised-novelty true — it is a typed estimate with no
    formal definition behind it;
  - :parameter-information-gain is added and marks nothing unformalised: it
    is the Lean expectedInformationGain (DarkTower.WarMachine.Holes),
    computed by futon2.aif.parameter-delivery from a delivered parameter
    posterior. It is the value of learning about the PARAMETERS, which no
    other term here carries — G stays free of an epistemic term (that is
    SPEC-flat-removal S2, unapproved)."
  [moves family evaluate-g cost-of]
  (reduce
   (fn [acc move]
     (let [result (move family)]
       (if (identical? :no-move (:status result))
         (update acc :no-move-reasons assoc
                 (:move-id result :unknown-move) (:reason result))
         (let [proposed (:proposed-family result)
               pragmatic (- (g-norm (best-g evaluate-g family))
                            (g-norm (best-g evaluate-g proposed)))
               est (:epistemic-estimate result)
               novelty? (= :novelty (:kind est))
               added? (contains? #{:novelty :parameter-information-gain}
                                 (:kind est))
               epistemic (if (and added? (number? (:value est)))
                           (double (:value est)) 0.0)
               cost (double (move-cost result cost-of))
               value (+ pragmatic epistemic (- cost))]
           (update acc :evaluations conj
                   {:move-id (:move-id result)
                    :value value
                    :proposed-family proposed
                    :parts {:pragmatic pragmatic
                            :epistemic est
                            :epistemic-added epistemic
                            :cost cost
                            :includes-unformalised-novelty novelty?}})))))
   {:evaluations [] :no-move-reasons {}}
   moves))

(defn- receipt
  "The construction receipt cascade-problems accepts as
  :construction-receipt (non-nil, carried verbatim). Stopping is never
  target success."
  [target taken evaluations stop-reason budget-used horizon g-of-best checks]
  {:target target
   :moves (mapv #(dissoc % :proposed-family) taken)
   :family-searched (inc (count taken))
   :coverage {:moves-taken (vec (keep :move-id taken))
              :final-evaluation (into {} (map (juxt :move-id :value))
                                      evaluations)}
   :stop-reason stop-reason
   :budget-used budget-used
   :horizon horizon
   :g-of-best g-of-best
   :checks-added checks
   :stopped-is-not-success true})

(defn- construct*
  "Run the construction-level policy and return {:family […] :receipt {…}}.

  Input:
    :target          target identity (mission/ticket)
    :initial-family  coll of candidates; the empty cascade may be among them
    :moves           coll of injected move fns (fn [family] →
                     {:proposed-family … :move-id …
                      :epistemic-estimate {:value v
                                           :kind :state-information|:novelty
                                           :basis …}
                      :cost c}
                     or the typed {:status :no-move :reason …})
    :evaluate-g      (fn [candidate] → G: number or :infinite); production
                     passes futon2.aif.active-horizon-g/active-horizon-g
    :budget          {:max-moves n} REQUIRED, no default — missing is the
                     typed refusal :budget-required
    :horizon         T, the common declared prediction horizon of the
                     candidate family (initially 2, Joe 2026-09-17);
                     required, :horizon-required when missing
    :cost-of         optional (fn [move-id] → cost) for moves that declare
                     no :cost
    :facts/:want/:patterns/:check-theta
                     optional; when supplied, gating unknown facts become
                     check candidates (futon2.aif.check-candidates) added to
                     the family before scoring
    :q0              optional coll of established tokens; when the want is
                     already observed there, construction stops immediately
                     (:want-already-observed)

  Stop reasons: :acting-worth-more (best move's value <= 0),
  :budget-exhausted, :no-admitted-move (every move :no-move),
  :needs-routed-human-input (every move :no-move with reason
  :needs-human-input), :want-already-observed. Stopping is not target
  success; the receipt says so (:stopped-is-not-success true).

  Pure: moves, evaluation and costs are all injected."
  [{:keys [target initial-family moves evaluate-g budget horizon cost-of
           want q0]
    :as input}]
  (cond
    (not (and (map? budget)
              (integer? (:max-moves budget))
              (<= 0 (:max-moves budget))))
    (refusal :budget-required
             {:action :declare
              :budget {:max-moves "declared move budget, no default"}})

    (not (and (integer? horizon) (pos? horizon)))
    (refusal :horizon-required {:action :declare :horizon horizon})

    (not (and (ifn? evaluate-g)
              (sequential? initial-family)
              (seq initial-family)))
    (refusal :invalid-input {:evaluate-g (ifn? evaluate-g)
                             :initial-family (and (sequential? initial-family)
                                                  (seq initial-family))})

    :else
    (let [want-tokens (when (sequential? want) (set want))]
      (if (and want-tokens q0 (set/subset? want-tokens (established-tokens q0)))
        ;; the target's want is already observed: nothing to construct for
        {:family (vec initial-family)
         :receipt (receipt target [] {} :want-already-observed 0 horizon
                           (best-g evaluate-g initial-family) [])}
        (let [checks (checks-for input)]
          (if (contains? checks :status)
            checks
            (let [family0 (augment-with-checks (vec initial-family)
                                               (:checks checks))]
              (loop [family family0
                     taken []      ;; taken moves, newest last
                     budget-used 0]
                (let [{:keys [evaluations no-move-reasons]}
                      (evaluate-moves (vec moves) family evaluate-g cost-of)
                      best (when (seq evaluations)
                             (reduce (fn [a b] (if (> (:value b) (:value a)) b a))
                                     evaluations))]
                  (cond
                    ;; every move refused: distinguish routed human input
                    (and (empty? evaluations) (seq no-move-reasons))
                    {:family family
                     :receipt (receipt
                               target taken evaluations
                               (if (every? #(identical? :needs-human-input %)
                                           (vals no-move-reasons))
                                 :needs-routed-human-input
                                 :no-admitted-move)
                               budget-used horizon
                               (best-g evaluate-g family)
                               (:checks checks))}

                    ;; hand-over-when-acting-is-worth-more: the best move
                    ;; is worth no more than acting on the best family now
                    (or (nil? best) (<= (:value best) 0))
                    {:family family
                     :receipt (receipt target taken evaluations
                                       :acting-worth-more budget-used horizon
                                       (best-g evaluate-g family)
                                       (:checks checks))}

                    (>= budget-used (:max-moves budget))
                    {:family family
                     :receipt (receipt target taken evaluations
                                       :budget-exhausted budget-used horizon
                                       (best-g evaluate-g family)
                                       (:checks checks))}

                    :else
                    (recur (:proposed-family best)
                           (conj taken best)
                           (inc budget-used))))))))))))

(defn construct
  "construct* with the construction receipt recording observation locators.

  P5 under Joe's 2026-09-17 answer requires every token the family's problem
  reads or writes (facts, want, pattern guards and produces) to be observable
  by a mechanical check. The receipt therefore carries :locators, the supplied
  locators restricted to those tokens, and :unlocated-tokens, the tokens with
  no checkable (C1-C5) locator. Construction does not stop on unlocated tokens,
  since later moves may add or replace patterns. futon2.aif.cascade-problems
  refuses the problem at assembly while any remain, and the receipt names them
  so whoever builds the cascade can add locators.

  Input is as for construct*, plus :locators {token {:class :C1..:C5 ...}}.
  Without :facts, :want and :patterns the token set is unknown, and the receipt
  records :locator-coverage :token-set-not-supplied instead of claiming
  coverage."
  [{:keys [facts want patterns locators] :as input}]
  (let [result (construct* input)]
    (if-not (:receipt result)
      result
      (let [supplied? (and (map? facts) (sequential? want)
                           (or (map? patterns) (sequential? patterns)))
            ;; construct* takes patterns as a vector (check-candidates' shape);
            ;; cascade-problems keys them by id
            by-id (if (map? patterns) patterns (into {} (map (juxt :id identity)) patterns))
            tokens (when supplied? (problems/problem-tokens facts want by-id))]
        (update result :receipt merge
                (if supplied?
                  {:locators (select-keys (or locators {}) tokens)
                   :unlocated-tokens (vec (problems/unlocated-tokens (or locators {}) tokens))}
                  {:locator-coverage :token-set-not-supplied}))))))
