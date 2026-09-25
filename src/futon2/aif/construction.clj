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

(defn- scored-g
  "Normalise an :evaluate-g result into {:value g :universe u}.

  A G value is meaningful only with the universe it was normalised over
  (H-VALUE-G-D §7, W6). :evaluate-g may therefore return either a bare
  number/:infinite — a LEGACY value whose universe was never recorded,
  normalised here to the typed absence :universe :not-recorded — or a map
  {:value g :universe u}, where u is the sorted token set (recorded
  sorted) or an opaque digest map. A map without :universe is the same
  typed absence; absence is never a substituted value."
  [r]
  (if (map? r)
    {:value (:value r)
     :universe (cond (sequential? (:universe r)) (vec (sort-by pr-str (:universe r)))
                     (some? (:universe r)) (:universe r)
                     :else :not-recorded)}
    {:value r :universe :not-recorded}))

(defn- best-g
  "Scored G ({:value … :universe …}) of the best candidate in FAMILY under
  :evaluate-g (lower value is better)."
  [evaluate-g family]
  (when (seq family)
    (reduce (fn [b c]
              (let [s (scored-g (evaluate-g c))]
                (if (< (g-norm (:value s)) (g-norm (:value b))) s b)))
            (scored-g (evaluate-g (first family)))
            (rest family))))

(defn- compare-g
  "The ONLY recorded comparison of two scored G values (W6/X6). Both
  operands must carry the same recorded universe: then {:delta …
  :universe …} with delta = a − b (positive means b improved). Anything
  else — different universes, or either universe the typed absence
  :not-recorded — is {:incommensurable {:universes [ua ub]}}: no
  improvement number exists and none is claimed."
  [a b]
  (if (and (not (identical? :not-recorded (:universe a)))
           (= (:universe a) (:universe b)))
    {:delta (- (g-norm (:value a)) (g-norm (:value b)))
     :universe (:universe a)}
    {:incommensurable {:universes [(:universe a) (:universe b)]}}))

(defn- established-tokens
  "Tokens already established in q0. q0 is accepted as a coll of tokens
  (a tick-like token state). Anything else establishes nothing here."
  [q0]
  (if (sequential? q0) (set q0) #{}))

(defn- reach-path
  "A path a → … → b following CHILDREN (strict), or nil. Depth-first; the
  seen set bounds it on cyclic input."
  [children a b]
  (letfn [(step [x seen]
            (cond (= x b) [b]
                  (contains? seen x) nil
                  :else (some (fn [y]
                                (when-let [p (step y (conj seen x))]
                                  (into [x] p)))
                              (get children x))))]
    (when-not (= a b)
      (step a #{}))))

(defn containment-order
  "PROOF-2a clause 0 (lines 128-212): a candidate's pattern structure as a
  containment order r over UNITS — one node per application of a pattern,
  the pattern id as an attribute — derived from the interpretations'
  produces/consumes. Unit A sits above unit B exactly when A contains B:
  A produces a token B's guard needs. This is the same producer→consumer
  reading as interpretation-construction's :need-edges; on the hand cascade
  M-futon-seams instance 6 the derived edges are exactly the recorded :above
  spans.

  Returns
    {:units         [{:unit u :pattern id} …]
     :descent       [[above below] …]     ; r itself
     :meets         {[a b] m …}           ; overlapping pairs with a meet
     :missing-meets [{:pair [a b] :common-maximal [u …]} …]}

  Meets follow DarkTower.WarMachine.CascadeOrder: Below is reflexive
  (a = b ∨ Reach r a b), and the semilattice condition is restricted to
  OVERLAPPING pairs — two units sharing a descendant must have their
  greatest common descendant as a unit of the cascade. An overlapping pair
  without one is the typed finding :missing-meets naming the pair and the
  maximal units of their common part, not a failure. Disjoint pairs need no
  meet and are not recorded.

  A cyclic containment is REFUSED with the typed reason
  :cyclic-containment naming the cycle: CascadeOrder.acyclicDescent r is
  required and over a cyclic relation no order exists to record. The
  candidate's :precedence stays as the chain case; when r is a chain its
  only linear extension is that precedence."
  [candidate]
  (let [pats (vec (:patterns candidate))
        ids (mapv :id pats)
        unique? (= (count ids) (count (set ids)))
        units (if unique?
                (mapv (fn [p] {:unit (:id p) :pattern (:id p)}) pats)
                (mapv (fn [i p] {:unit [(:id p) i] :pattern (:id p)})
                      (range) pats))
        by-unit (into {} (map (fn [{:keys [unit]} p] [unit p]) units pats))
        descent (vec (sort-by pr-str
                              (for [[a pa] by-unit [b pb] by-unit
                                    :when (not= a b)
                                    :when (seq (set/intersection
                                                (set (:produces pa))
                                                (set (get-in pb [:guard :needs]))))]
                                [a b])))
        children (reduce (fn [m [a b]] (update m a (fnil conj []) b))
                         {} descent)
        cycle (some (fn [[a b]]
                      (when-let [p (reach-path children b a)]
                        (into [a] p)))
                    descent)]
    (if cycle
      (refusal :cyclic-containment {:cycle (vec cycle)})
      (let [below (into {}
                        (for [u (keys by-unit)]
                          [u (loop [seen #{u} frontier (set (get children u))]
                               (if (empty? frontier)
                                 seen
                                 (recur (into seen frontier)
                                        (set/difference
                                         (set (mapcat #(get children %) frontier))
                                         seen))))]))
            unit-ids (vec (sort-by pr-str (keys by-unit)))
            pairs (for [a unit-ids b unit-ids
                        :when (neg? (compare (pr-str a) (pr-str b)))]
                    [a b])
            overlaps (for [[a b] pairs
                           :let [common (set/intersection (below a) (below b))]
                           :when (seq common)]
                       (let [maximal (vec (sort-by pr-str
                                                   (remove (fn [d]
                                                             (some #(contains? (below %) d)
                                                                   (disj common d)))
                                                           common)))
                             meet (when (= 1 (count maximal))
                                    (let [m (first maximal)]
                                      (when (every? #(contains? (below m) %)
                                                    common)
                                        m)))]
                         {:pair [a b] :common-maximal maximal :meet meet}))]
        {:units units
         :descent descent
         :meets (into {}
                      (keep (fn [{:keys [pair meet]}]
                              (when meet [pair meet])))
                      overlaps)
         :missing-meets (into []
                              (keep (fn [{:keys [pair common-maximal meet]}]
                                      (when-not meet
                                        {:pair pair
                                         :common-maximal common-maximal})))
                              overlaps)}))))

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
   :no-move-reasons {move-id reason …}
   :no-move-findings {move-id {what the move found anyway} …}}
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
         (let [move-id (:move-id result :unknown-move)
               ;; everything the move reported BEYOND being unable to move:
               ;; unmet needs, cycles, sibling gaps
               found (not-empty (dissoc result :status :move-id :reason))]
           (cond-> (update acc :no-move-reasons assoc move-id (:reason result))
             found (update :no-move-findings assoc move-id found)))
         (let [proposed (:proposed-family result)
               ;; pragmatic value is a COMPARISON of two G values; it exists
               ;; only over one shared recorded universe (W6). Otherwise the
               ;; comparison is recorded :incommensurable and contributes no
               ;; number — an incommensurable pragmatic is not a 0.
               g-cmp (compare-g (best-g evaluate-g family)
                                (best-g evaluate-g proposed))
               pragmatic (:delta g-cmp)
               est (:epistemic-estimate result)
               novelty? (= :novelty (:kind est))
               added? (contains? #{:novelty :parameter-information-gain}
                                 (:kind est))
               epistemic (if (and added? (number? (:value est)))
                           (double (:value est)) 0.0)
               cost (double (move-cost result cost-of))
               value (+ (or pragmatic 0.0) epistemic (- cost))]
           (update acc :evaluations conj
                   {:move-id (:move-id result)
                    :value value
                    :proposed-family proposed
                    :g-comparison g-cmp
                    :parts {:pragmatic (if (some? pragmatic)
                                         pragmatic
                                         :incommensurable)
                            :epistemic est
                            :epistemic-added epistemic
                            :cost cost
                            :includes-unformalised-novelty novelty?}})))))
   {:evaluations [] :no-move-reasons {} :no-move-findings {}}
   moves))

(defn- receipt
  "The construction receipt cascade-problems accepts as
  :construction-receipt (non-nil, carried verbatim). Stopping is never
  target success.

  Every scored G on the receipt carries its :universe beside its value
  (:g-of-best is {:value … :universe …}; a legacy value records the
  typed absence :universe :not-recorded). Every recorded COMPARISON of G
  values (:final-evaluation entries, each taken move's :g-comparison)
  asserts both operands shared one universe; otherwise it records
  {:incommensurable {:universes [...]}} and never a number."
  [target taken evaluations stop-reason budget-used horizon best checks
   no-move-findings]
  {:target target
   :moves (mapv #(dissoc % :proposed-family) taken)
   :family-searched (inc (count taken))
   :coverage {:moves-taken (vec (keep :move-id taken))
              :final-evaluation (into {}
                                      (map (fn [{:keys [move-id value g-comparison]}]
                                             [move-id (if (:incommensurable g-comparison)
                                                        g-comparison
                                                        value)]))
                                      evaluations)}
   :stop-reason stop-reason
   :budget-used budget-used
   :horizon horizon
   :g-of-best (select-keys best [:value :universe])
   :checks-added checks
   ;; what a move that could not move nonetheless FOUND: order-by-need's
   ;; unmet needs and cycles, borrow-a-sibling's gaps. Present-only. Without
   ;; this the findings died with the move's return value, and construction
   ;; could stop :no-admitted-move with nobody told which needs were unmet --
   ;; and an unmet need is exactly what a check candidate is made from
   :no-move-findings (not-empty no-move-findings)
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
    :evaluate-g      (fn [candidate] → G), either a bare number or :infinite
                     (LEGACY: recorded with the typed absence :universe
                     :not-recorded, and every comparison against it is
                     :incommensurable) or {:value g :universe tokens} —
                     the G value together with the universe it was
                     normalised over (H-VALUE-G-D §7, W6). Production
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

  Stop reasons: :acting-worth-more (best move's value <= 0 over one shared
  recorded universe), :g-universes-incommensurable (the best move's G
  comparison had no shared recorded universe, so no :acting-worth-more
  verdict exists), :budget-exhausted, :no-admitted-move (every move
  :no-move),
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
                           (best-g evaluate-g initial-family) [] nil)}
        (let [checks (checks-for input)]
          (if (contains? checks :status)
            checks
            (let [family0 (augment-with-checks (vec initial-family)
                                               (:checks checks))]
              (loop [family family0
                     taken []      ;; taken moves, newest last
                     budget-used 0]
                (let [{:keys [evaluations no-move-reasons no-move-findings]}
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
                               (:checks checks) no-move-findings)}

                    ;; hand-over-when-acting-is-worth-more: the best move
                    ;; is worth no more than acting on the best family now.
                    ;; But when the best move's G comparison is
                    ;; :incommensurable (different or unrecorded universes)
                    ;; that verdict was never reached: the stop is the typed
                    ;; :g-universes-incommensurable, not :acting-worth-more.
                    (or (nil? best) (<= (:value best) 0))
                    {:family family
                     :receipt (receipt target taken evaluations
                                       (if (and best
                                                (get-in best [:g-comparison :incommensurable]))
                                         :g-universes-incommensurable
                                         :acting-worth-more)
                                       budget-used horizon
                                       (best-g evaluate-g family)
                                       (:checks checks) no-move-findings)}

                    (>= budget-used (:max-moves budget))
                    {:family family
                     :receipt (receipt target taken evaluations
                                       :budget-exhausted budget-used horizon
                                       (best-g evaluate-g family)
                                       (:checks checks) no-move-findings)}

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
  no checkable (C3-C5) locator. Construction does not stop on unlocated tokens,
  since later moves may add or replace patterns. futon2.aif.cascade-problems
  refuses the problem at assembly while any remain, and the receipt names them
  so whoever builds the cascade can add locators.

  Input is as for construct*, plus :locators {token {:class :C3..:C5 ...}}.
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
