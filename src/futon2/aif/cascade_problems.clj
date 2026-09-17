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
    2 :no-admitted-interpretation   no admitted interpretations (or a
                                    candidate pattern without one); the
                                    failing clause is included when the
                                    source gives one
    3 :want-not-declared            no want for the target
    4 :no-constructed-candidate     no constructed cascade (a non-empty
                                    precedence carrying a construction
                                    receipt); the empty cascade alone is
                                    never 'constructed'
    5 :beta-not-declared            no declared β for the target's context

  A missing :horizon-steps refuses ALL targets (:horizon-not-declared)."
  (:require [futon2.aif.mission-registry :as registry]))

(defn substrate-targets
  "The target listing the tick's substrate scans already produce: the SAME
  ids `mission-enumerator-proposer` and `ticket-enumerator-proposer`
  enumerate (mission_registry.clj :322/:419) — live mission ids then live
  ticket ids — as TARGET IDENTITIES, not the flat :advance-mission /
  :advance-ticket actions those proposers construct. Pure listing; the
  enumeration itself is the tick's existing substrate read."
  []
  (vec (concat (map :id (registry/open-missions))
               (map :id (filter registry/live-ticket?
                                (:tickets (registry/load-tickets)))))))

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
          (or candidates [])))

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

(defn- assemble-one
  "Assemble one target's cascade problem, or its first applicable typed
  refusal (kind order: universe, interpretation, want, candidate, β).
  PURE: everything is read from `sources`."
  [sources horizon target]
  (let [universe (get-in sources [:universes target])
        interp (get-in sources [:interpretations target])
        patterns (:patterns interp)
        want (get-in sources [:wants target])
        candidates (get-in sources [:candidates target])
        constructed (vec (constructed-candidates candidates))
        ;; every pattern of every candidate must have an admitted
        ;; interpretation; an uninterpreted pattern is exactly a missing
        ;; admitted interpretation.
        uninterpreted (seq (remove (set (keys patterns))
                                   (distinct (mapcat :precedence constructed))))
        beta (beta-for sources target)
        ctx-fn (:context-of sources)]
    (cond
      (not (and (map? universe) (seq universe)))
      (refusal target :universe-not-admitted :universes)

      (not (and (map? patterns) (seq patterns)))
      (refusal target :no-admitted-interpretation :interpretations
               (when-let [clause (:clause (:refused interp))]
                 {:clause clause}))

      uninterpreted
      (refusal target :no-admitted-interpretation :interpretations
               {:patterns-without-interpretation
                (vec (sort uninterpreted))})

      (not (and (sequential? want) (seq want)))
      (refusal target :want-not-declared :wants)

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
       {:facts universe
        :want (vec want)
        :interpretations patterns
        :repository {:patterns (set (keys patterns))
                     :stands-on #{}}
        ;; the family: the target's constructed cascades plus its empty
        ;; cascade, always present, always first.
        :precedences (vec (distinct
                           (cons []
                                 (map #(vec (:precedence %)) constructed))))
        :horizon-steps horizon
        :cascade-spec {:want (set want)}
        :beta beta}
       :construction-receipts
       (mapv :construction-receipt constructed)
       :interpretation-receipts
       ;; the interpretations source's own receipts, carried so the E1 gate
       ;; can require them per candidate without the caller reaching back
       ;; into the source map. Absent receipts stay {} — the gate then
       ;; refuses, honestly, rather than a default being invented here.
       (or (:receipts interp) {})})))

(defn assemble
  "Assemble one `:cascade-problem` per fully supplied target, plus one typed
  refusal per target missing an input. INPUT:

    {:targets [target …]            mission/ticket identities
                                      (see `substrate-targets`)
     :sources {:universes {target {fact true|false|:unknown}}
               :interpretations {target {:patterns {pattern-id
                                            {:guard {:needs #{} :forbids #{}}
                                             :produces #{}}}
                                          :refused {:clause …}}}   ; optional
               :wants {target [token …]}
               :candidates {target [{:precedence [pattern-id …]
                                     :construction-receipt …}]}
               :horizon-steps T
               :beta-by-context {context {:beta β}}
               :context-of (fn [target] context)}}

  Returns {:problems [{:target … :cascade-problem {…}
                       :construction-receipts […]}]
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
         :refusals (vec (filter :kind assembled))}))))
