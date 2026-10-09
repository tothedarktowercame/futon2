(ns futon2.aif.cascade-problems
  "Per-target cascade problem assembly for the cascade-only tick
  (SPEC-flat-removal-and-cascade-decision H2; Joe's 2026-09-17 ruling: the
  flat single-action decision is removed and made impossible to run — a
  cascade is a policy, and G is computed over policies).

  `assemble` takes target IDENTITIES (missions and tickets, not actions)
  and injected per-target sources, and yields one `:cascade-problem` per
  fully supplied target in the exact shape `futon2.report.war-machine/
  cascade-lane` consumes, or a typed refusal naming the first missing
  input. Every target lands in exactly one of `:problems` / `:refusals`.
  A missing input is a typed refusal, never a default and never a
  fallback: if every target is refused the tick abstains carrying this
  list (the abstention itself is the caller's; this namespace only
  refuses honestly).

  Refusal kinds, first applicable wins:
    1 :universe-not-admitted        no admitted fact universe for the target
    2 :no-query-time-slice         neither admitted interpretations nor a
                                    query-time library slice for the target
      :no-admitted-interpretation   a constructed candidate names a pattern
                                    without an admitted interpretation
    3 :want-not-declared            no want for the target
    4 :no-constructed-candidate     no constructed cascade (a non-empty
                                    precedence carrying a construction
                                    receipt); the empty cascade alone is
                                    never 'constructed'
    5 :beta-not-declared            no declared β for the target's context

  A missing :horizon-steps refuses ALL targets (:horizon-not-declared)."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.live-c :as live-c]))

(load-identity/register! *ns* *file*)

(defn substrate-targets
  "The target listing the tick's substrate scans already produce: the SAME
  ids `mission-enumerator-proposer` and `ticket-enumerator-proposer`
  enumerate (mission_registry.clj :322/:419) — live mission ids then live
  ticket ids — as TARGET IDENTITIES, not the flat :advance-mission /
  :advance-ticket actions those proposers construct. Pure listing; the
  enumeration itself is the tick's existing substrate read. The one-arg
  arity takes the mission registry doc already loaded (WM-MISSION-READ-ONCE-I:
  the selection reads it once), as open-missions' one-arg arity does."
  ([] (substrate-targets (registry/load-missions) (registry/load-tickets)
                         (registry/load-excursions)))
  ([loaded-missions]
   (substrate-targets loaded-missions (registry/load-tickets)
                      (registry/load-excursions)))
  ([loaded-missions loaded-tickets]
   (substrate-targets loaded-missions loaded-tickets
                      (registry/load-excursions)))
  ([loaded-missions loaded-tickets loaded-excursions]
   (vec (concat (map :id (registry/open-missions loaded-missions))
                (map :id (filter registry/live-excursion?
                                 (:excursions loaded-excursions)))
                (map :id (filter registry/live-ticket?
                                 (:tickets loaded-tickets)))))))

(defn- refusal
  [target kind missing & [more]]
  (merge {:target target :kind kind :missing missing} more))

(defn- constructed-candidates
  "A constructed cascade: non-empty :precedence AND a :construction-receipt
  (claude-7's account §5: the receipt — moves taken, family searched,
  coverage — is recorded with each candidate; stopping construction is not
  target success). Anything else is not a candidate for this family."
  [candidates]
  (filter (fn [c] (and (vector? (:precedence c))
                       (seq (:precedence c))
                       (some? (:construction-receipt c))))
          (map-indexed (fn [i c] (assoc c :candidate-id (keyword (str "C" (inc i)))))
                       (or candidates []))))

(defn- beta-for
  "β for TARGET's context, or nil. `:beta-by-context` maps
  context → {:beta β} (a bare number is also accepted); `:context-of`
  maps target → context. No context function or no entry ⇒ nil — the
  caller then sees :beta-not-declared, never a default."
  [sources target]
  (let [ctx-fn (:context-of sources)
        context (when (ifn? ctx-fn) (ctx-fn target))
        v (get-in sources [:beta-by-context context])]
    (cond (number? v) v
          (map? v) (:beta v)
          :else nil)))

(def checkable-classes
  "The mechanically checkable token classes of the WM-04 observation contract
  (resources/wm/observation-contract.edn). Class J (judgement) is not among
  them: Joe, 2026-09-17, a blinded study on each pass is too heavy, so every
  token the model predicts must be observable by a mechanical check. C8, a
  registered passing run at current content, is AR-41 (futon2 9d5525ee)."
  #{:C3 :C4 :C5 :C6 :C8})

(def provisional-policy-count
  "Two distinct query-slice policies per open target: the Q8 minimum."
  2)

(defn retrieval-effect
  "Declared provisional likelihood from rank r in a slice of k: the Weibull
  empirical-CDF plotting position (k-r+1)/(k+1). Interpretation replaces it."
  [candidate k]
  (let [rank (or (:slice-rank candidate) (:rank candidate))]
    (when (and (pos-int? k) (pos-int? rank) (<= rank k))
      {:theta (/ (inc (- k rank)) (inc k))
       :source :retrieval-rank-likelihood
       :rank rank :slice-size k :raw-score (:score candidate)
       :formula "(k-r+1)/(k+1)"})))

(defn problem-tokens
  "Every token a target's problem reads or writes: its facts, its want, and
  every interpreted pattern's guard and produces."
  [universe want patterns]
  (set (concat (keys universe)
               want
               (mapcat (fn [p]
                         (concat (get-in p [:guard :needs]) (get-in p [:guard :forbids])
                                 (mapcat :present (get-in p [:guard :clauses]))
                                 (mapcat :absent (get-in p [:guard :clauses]))
                                 (:produces p)))
                       (vals patterns)))))

;; A target with admitted interpretations but no declared candidate gets its
;; candidates from the constructor, when the sources supply :construction
;; {:construct futon2.aif.interpretation-construction/construct
;;  :budget … :move-cost … :evaluate-g (fn [problem candidate] G)}. The
;; constructor is injected because its namespace already depends on this
;; one. PROBLEM is the target's cascade problem without :precedences, so
;; the caller can score a candidate with the same G selection uses. Without
;; :construction nothing is constructed and the existing refusal stands.
;; The universe supplies the observation. A token the universe holds as
;; :unknown is passed as not established (false): construction only asks
;; which tokens a plan may start from, so an unknown token is one the plan
;; has to produce. (claude-10, 2026-09-24; no ruling found. The constructor
;; itself refuses unknowns; this is the caller's reading, and the tokens so
;; read are listed on each candidate's receipt.) A token absent from the
;; universe makes the constructor refuse, carried on the target's refusal.
(defn- constructed-from-interpretations
  [sources horizon target universe patterns want base-problem]
  (when-let [{:keys [construct budget move-cost evaluate-g]} (:construction sources)]
    (let [receipts (or (get-in sources [:interpretations target :receipts]) {})
          feedback (or (get-in sources [:pattern-feedback target])
                       (get-in sources [:pattern-feedback :wm/global]))
          tokens (problem-tokens universe want patterns)
          unknown (sort-by pr-str (filter #(= :unknown (get universe %)) tokens))
          observation (into {} (for [t tokens :let [v (get universe t)]
                                     :when (or (boolean? v) (= :unknown v))]
                                 [t (true? v)]))
          result (construct (cond-> {:target target :want (vec want) :observation observation
                                     :interpretations (into {} (for [[k p] patterns] [k (select-keys p [:guard :produces])]))
                                     :interpretation-receipts receipts
                                     :horizon horizon :move-cost (or move-cost 1)
                                     :budget budget
                                     :evaluate-g (fn [candidate] (evaluate-g base-problem candidate))}
                              ;; Provisional cascades are rebuilt from the
                              ;; current problem.  Prior runs are evidence
                              ;; about pattern use, never a canonical
                              ;; declaration or a replacement candidate.
                              feedback (assoc :pattern-feedback feedback)))]
      (if (= :constructed (:status result))
        {:candidates (mapv #(cond-> (assoc-in %
                                              [:construction-receipt :unknown-read-as-not-established]
                                              (vec unknown))
                              feedback (assoc-in [:construction-receipt :pattern-feedback]
                                                 feedback))
                           (:candidates result))}
        {:construction-refusal (dissoc result :status :candidates)}))))

(defn unlocated-tokens
  "Tokens with no locator of a checkable class."
  [locators tokens]
  (seq (sort-by pr-str (remove #(checkable-classes (:class (get locators %))) tokens))))

(defn- base-problem-data
  "One map builder. assemble-one needs the unvalidated map while constructing,
   before its candidate-before-beta refusal order has been decided."
  [sources horizon target]
  (let [universe (get-in sources [:universes target])
        interp (get-in sources [:interpretations target])
        patterns (:patterns interp)
        slice (get-in sources [:query-time-slices target])
        want (get-in sources [:wants target])
        scales (or (get-in sources [:preference-scales target])
                   (live-c/preference-scales {}))
        schedule (or (get-in sources [:preference-schedules target])
                     (live-c/preference-schedule {}))
        beta (beta-for sources target)
        locators (get-in sources [:locators target])
        problem {:facts universe
                      :preference-source-id target
                      :want (vec want)
                      :interpretations patterns
                      :repository {:patterns (set (keys patterns))
                                   :stands-on #{}}
                      :horizon-steps horizon
                      :c-schedule schedule
                      :observation-schedule (get-in sources [:observation-schedules target]
                                                    {:status :held :reason :observation-placement-not-declared})
                      :cascade-spec (cond-> {:want (set want) :c-schedule schedule
                                             :lam (get-in scales [:lam :value])
                                             :mu (get-in scales [:mu :value])
                                             :preference-scales scales}
                                      (contains? (get sources :c {}) target)
                                      (assoc :c (get-in sources [:c target])))
                      :preference-scales scales
                      :beta beta
                      :locators locators
                      :token-initialization (get-in sources [:token-initialization target])}]
    (cond-> problem
      (or (get-in sources [:pattern-feedback target])
          (get-in sources [:pattern-feedback :wm/global]))
      (assoc :pattern-feedback
             (or (get-in sources [:pattern-feedback target])
                 (get-in sources [:pattern-feedback :wm/global])))
      ;; The slice is construction provenance, not merely a bridge used until
      ;; an interpretation arrives.  Retain it after interpretation so the
      ;; eventual decision can say which library result bounded construction.
      (map? slice) (assoc :query-time-slice slice
                          :pattern-pool
                          (mapv #(select-keys % [:pattern :slice-rank :rank
                                                 :retriever-rank :retriever
                                                 :provenance :raw :judgment])
                                (:candidates slice))))))

(defn base-problem
  "Assemble the candidate-independent problem, or its first typed refusal:
   universe, interpretation, want, locators, beta. Candidate validation remains
   assemble-one's responsibility; no precedences are invented here."
  [sources horizon target]
  (let [problem (base-problem-data sources horizon target)
        {:keys [facts interpretations query-time-slice want beta locators]} problem
        unlocated (when (and (map? facts)
                             (or (map? interpretations) (map? query-time-slice)))
                    (unlocated-tokens locators (problem-tokens facts want interpretations)))]
    (cond
      (not (and (map? facts) (seq facts)))
      (refusal target :universe-not-admitted :universes)
      (and (not (and (map? interpretations) (seq interpretations)))
           (not (map? query-time-slice)))
      (refusal target :no-query-time-slice :query-time-slices)
      (not (and (sequential? (get-in sources [:wants target])) (seq want)))
      (refusal target :want-not-declared :wants)
      unlocated
      (refusal target :universe-not-admitted :locators
               {:tokens-without-checkable-locator (vec unlocated)})
      (nil? beta)
      (refusal target :beta-not-declared :beta-by-context
               {:context (let [f (:context-of sources)] (when (ifn? f) (f target)))})
      :else problem)))

(defn- assemble-one
  "Assemble one target's cascade problem, or its first applicable typed
  refusal (kind order: universe, interpretation, want, candidate, β).
  PURE: everything is read from `sources`."
  [sources horizon target]
  (let [base (base-problem-data sources horizon target)
        universe (:facts base)
        interp (get-in sources [:interpretations target])
        patterns (:interpretations base)
        slice (:query-time-slice base)
        ;; Rank, not collection order, is retrieval authority.  This keeps a
        ;; replay/serialization permutation from changing the policy family.
        slice-pool (->> (:candidates slice)
                        (map #(select-keys % [:pattern :slice-rank :rank :retriever-rank
                                              :retriever :provenance :raw :score :judgment]))
                        (sort-by (juxt #(or (:slice-rank %) (:rank %) Long/MAX_VALUE)
                                       (comp str :pattern)))
                        vec)
        slice-patterns (mapv :pattern slice-pool)
        want (get-in sources [:wants target])
        beta (:beta base)
        ctx-fn (:context-of sources)
        locators (:locators base)
        declared (get-in sources [:candidates target])
        ;; A declared candidate that produces no want still open (every token
        ;; it produces is already true) cannot advance the target, so it does
        ;; not stop construction: the machine builds from the interpretations
        ;; instead of re-selecting work that is done.
        open-wants (set (remove #(true? (get universe %)) want))
        advancing (filter (fn [c] (some (fn [pid] (seq (filter open-wants (get-in patterns [pid :produces]))))
                                        (:precedence c)))
                          (constructed-candidates declared))
        built (when (and (empty? advancing)
                         (map? patterns) (seq patterns) (sequential? want) (seq want) (map? universe))
                (constructed-from-interpretations sources horizon target universe patterns want
                                                  base))
        ;; Once construction ran, its result stands: declared candidates that
        ;; advance nothing are not a fallback for a constructor refusal.
        candidates (if built (or (:candidates built) []) declared)
        constructed (vec (constructed-candidates candidates))
        ;; every pattern of every candidate must have an admitted
        ;; interpretation; an uninterpreted pattern is exactly a missing
        ;; admitted interpretation.
        uninterpreted (seq (remove (set (keys patterns))
                                   (distinct (mapcat :precedence constructed))))
        unlocated (when (and (map? universe)
                             (or (map? patterns) (map? slice)))
                    (unlocated-tokens locators (problem-tokens universe want patterns)))]
    (cond
      (not (and (map? universe) (seq universe)))
      (refusal target :universe-not-admitted :universes)

      (and (not (and (map? patterns) (seq patterns)))
           (not (map? slice)))
      (refusal target :no-query-time-slice :query-time-slices)

      uninterpreted
      (refusal target :no-admitted-interpretation :interpretations
               {:patterns-without-interpretation
                (vec (sort uninterpreted))})

      (not (and (sequential? want) (seq want)))
      (refusal target :want-not-declared :wants)

      ;; P5 under Joe's 2026-09-17 answer: every token is observed
      ;; mechanically. A token without a checkable locator cannot be observed
      ;; on a pass, so the universe is not admitted; the refusal names the
      ;; tokens, so whoever builds the cascade can add locators.
      unlocated
      (refusal target :universe-not-admitted :locators
               {:tokens-without-checkable-locator (vec unlocated)})

      (and (not (seq patterns)) (map? slice) (nil? beta))
      (refusal target :beta-not-declared :beta-by-context
               {:context (when (ifn? ctx-fn) (ctx-fn target))})

      (and (not (seq patterns)) (map? slice))
      (let [k (count slice-pool)
            effects (into {} (keep (fn [candidate]
                                     (when-let [effect (retrieval-effect candidate k)]
                                       [(:pattern candidate) effect])))
                                   slice-pool)
            provisional-operators
            (into {} (map (fn [pattern]
                            (let [effect (get effects pattern)]
                              [pattern {:guard {:needs #{} :forbids #{}}
                                        :produces (set want)
                                        :theta (:theta effect)
                                        :theta-source (:source effect)
                                        :predicted-effect effect
                                        :status :interpretation-owed-after-selection}]))
                          slice-patterns))
            candidates
            (mapv (fn [i pattern]
                    {:candidate-id (keyword (str "C" (inc i)))
                     :precedence [pattern]
                     :construction-receipt
                     {:kind :query-time-pattern-selection
                      :status :provisional
                      :pattern pattern
                      :attested? false
                      :policy-limit provisional-policy-count
                      :predicted-effect (get effects pattern)}})
                  (range) (take provisional-policy-count slice-patterns))]
       {:target target
       :cascade-problem
       (assoc base
              :interpretations provisional-operators
              :repository {:patterns (set slice-patterns) :stands-on #{}}
              :precedences (mapv :precedence candidates)
              :pattern-pool slice-pool
              :pattern-operators {:status :provisional
                                  :reason :interpretation-owed-after-selection
                                  :patterns slice-patterns})
       :constructed-candidates candidates
       :interpretation-receipts {}
       :query-time-slice slice
       :slice-size (:slice-size slice)
       :library-size (:library-size slice)})

      (and (empty? constructed) (:construction-refusal built))
      (refusal target :no-constructed-candidate :construction
               {:constructor-refusal (:construction-refusal built)})

      (empty? constructed)
      (if (and (seq (or candidates []))
               (empty? (remove #(seq (:precedence %)) candidates)))
        ;; non-empty precedences exist but carry no construction receipt:
        ;; they are proposals, not constructed cascades.
        (refusal target :no-constructed-candidate :construction-receipt)
        (refusal target :no-constructed-candidate :candidates))

      (nil? beta)
      (refusal target :beta-not-declared :beta-by-context
               {:context (when (ifn? ctx-fn) (ctx-fn target))})

      :else
      {:target target
       :cascade-problem
       ;; Only constructed nonempty orders enter the executable family.
       (assoc (base-problem sources horizon target) :precedences (mapv :precedence constructed))
       :constructed-candidates
       (mapv #(select-keys % [:candidate-id :precedence :construction-receipt]) constructed)
       :interpretation-receipts
       ;; the interpretations source's own receipts, carried so the E1 gate
       ;; can require them per candidate without the caller reaching back
       ;; into the source map. Absent receipts stay {} — the gate then
       ;; refuses, honestly, rather than a default being invented here.
       (or (:receipts interp) {})
       :query-time-slice slice
       :slice-size (:slice-size slice)
       :library-size (:library-size slice)})))

(defn assemble
  "Assemble one `:cascade-problem` per fully supplied target, plus one typed
  refusal per target missing an input. INPUT:

    {:targets [target …]            mission/ticket identities
                                      (see `substrate-targets`)
     :sources {:universes {target {fact true|false|:unknown}}
               :query-time-slices {target {:candidates [{:pattern pattern-id …}]
                                            :slice-size n :library-size n}}
               :interpretations {target {:patterns {pattern-id
                                            {:guard {:needs #{} :forbids #{}}
                                             :produces #{}}}
                                          :refused {:clause …}}}   ; optional
               :wants {target [token …]}
               :locators {target {token {:class :C3..:C6 …locator}}}
               :candidates {target [{:precedence [pattern-id …]
                                     :construction-receipt …}]}
               :horizon-steps T
               :beta-by-context {context {:beta β}}
               :context-of (fn [target] context)}}

  Returns {:problems [{:target … :cascade-problem {…}
                       :constructed-candidates [{:candidate-id … :precedence […] :construction-receipt …}]}]
           :refusals [{:target … :kind … :missing …}]}. Every target lands
  in exactly one of the two. A missing :horizon-steps refuses ALL targets
  (:horizon-not-declared). Pure: no substrate reads here — inject the
  sources."
  [{:keys [targets sources]}]
  (let [horizon (:horizon-steps sources)]
    (if (nil? horizon)
      {:problems []
       :refusals (mapv #(refusal % :horizon-not-declared :horizon-steps)
                       (or targets []))}
      (let [assembled (map (partial assemble-one sources horizon)
                           (or targets []))]
        {:problems (vec (remove :kind assembled))
         :refusals (vec (filter :kind assembled))
         :dropped-candidates
         (vec (for [target targets
                    [i candidate] (map-indexed vector (get-in sources [:candidates target]))
                    :when (empty? (constructed-candidates [candidate]))]
                {:target target :candidate (keyword (str "C" (inc i))) :stage :construction-admission
                 :reason :unconstructed-proposal
                 :missing-evidence (cond-> []
                                     (not (and (vector? (:precedence candidate))
                                               (seq (:precedence candidate))))
                                     (conj :nonempty-precedence)
                                     (nil? (:construction-receipt candidate))
                                     (conj :construction-receipt))}))}))))
