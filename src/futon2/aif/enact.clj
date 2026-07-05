(ns futon2.aif.enact
  "R16 ENACTMENT — the write side of close-the-loop (E-close-the-loop 'Remaining'
   item (a), Joe-ratified live 2026-07-02).

   Runs in the SCHEDULED one-shot JVM (`wm_scheduled_run`), never the serving
   JVM: per tick, build act-gates for the judged ranked-actions, take the first
   `:pass`, ENACT it, and thread the `:realized-outcome` record onto the
   judgement via the committed `fold-realized/with-realized-outcome` seam, so
   `trace/write-trace!` persists it and R14's γ folds it next tick
   (`policy-precision/fold-realized-outcome`).

   ENACTMENT IS ARTIFACT-ONLY (read-only in apply-cascade!'s sense): the fold
   engine produces a construction — boxes + honestly-surfaced policy-holes — and
   we re-observe it. NO substrate write, NO outward action; promoting a wiring
   to :constructed and FIRING substantive actions stay operator-gated (WM-I4 /
   R16-ARM — governance, not inference).

   PREDICTION vs EXECUTION (the build axis, live — E-close-the-loop §6b):
     The GATE's ΔG leg (prediction) reconciles three sources, in order:
       1. rollout `:G-rollout` / classical fold — `close-loop/act-gate-from-lane-entry`;
       2. ESCROW (impl #2 live): a recorded LLM fold turn at
          `data/fold-escrow/<mission>.edn` (`{:boxes [...] :policy-holes [...]}`),
          written by an inhabiting agent OUT-OF-PROCESS (no LLM in any JVM);
          its coverage→rollout ΔG (shared `fold-eval`) fills the missing leg.
     EXECUTION is always the deterministic executor — the futon3a fold engine
     (`fold_engine.clj apply`, the Car-3 apply-cascade! executor) over the SAME
     cascade the gate evaluated. `:realized-G` = coverage ΔG over what the
     executor ACTUALLY reproduced. Adopting a predicted (escrow) wiring as its
     own realization would make realized ≡ expected — a tautology laundered
     into γ; the executor cross-check is the honest v1 realized signal. When
     the executor reproduces nothing, `:realized-G` is nil and γ HOLDS (the
     contract's honest no-op) — γ accrues slowly rather than falsely.

   The two G legs are therefore the SAME EFE quantity (fold-eval coverage→
   rollout ΔG): expected over the predicted wiring, realized over the enacted
   one — apples-to-apples for γ's signed perf ratio.

   Resilience: every side-effecting step is guarded; any failure returns the
   judgement UNCHANGED (the field stays absent ⇒ γ holds). Never throws into
   the scheduled run."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.fold-realized :as fr]))

(def ^:private futon3a-dir "/home/joe/code/futon3a")
(def ^:private fold-engine-rel "holes/labs/M-memes-arrows/fold_engine.clj")
(def ^:private escrow-dir
  (str (System/getProperty "user.home") "/code/futon2/data/fold-escrow"))

(defn- escrow-wiring
  "impl #2's recorded LLM fold turn for this mission, if an inhabiting agent
   left one. `{:boxes [...] :policy-holes [...]}` EDN, else nil."
  [mission]
  (try
    (let [f (io/file escrow-dir (str mission ".edn"))]
      (when (.exists f)
        (let [w (edn/read-string (slurp f))]
          (when (and (map? w) (seq (:boxes w))) w))))
    (catch Throwable _ nil)))

;; ---------------------------------------------------------------------------
;; L3 (E-live-loop-3, 2026-07-05): the scheduled caller injects the pinned seam.
;; Deposits are loaded once per tick (delay-cached); for each lane entry that
;; has a matching deposit, the caller reconstructs the DEPOSIT-GRAIN circumstance
;; (the deposit's own ψ + pattern-ids — NOT the lane's ψ, which may differ) and
;; calls the 3-arity with escrow-turn-fn + prose-fn. Entries without deposits
;; use the 1-arity (byte-identical to pre-L3 behavior).
;;
;; The prompt-sha pin means the lane's ψ will never match the deposit's pinned
;; prompt — autoclock-in's lane ψ is banner/sorry-grain, but the deposit pins
;; the S1 sorry-grain ψ. The caller must therefore reconstruct the deposit's own
;; circumstance for the sha check (the deposit carries everything needed).
;; ---------------------------------------------------------------------------

(def ^:private flexiarg-dir "/home/joe/code/futon3/library")

(defn- prose-fn
  "The prose source for the fold-prompt: verbatim slurp of the pattern's
   flexiarg file. This is the same source the deposit was constructed with
   (per the deposit's :prose-source field)."
  [pattern-id]
  (try (slurp (str flexiarg-dir "/" pattern-id ".flexiarg"))
       (catch Throwable _ nil)))

(def ^:private !deposits-cache
  (delay
    (try
      (:deposits (esc/load-deposits))
      (catch Throwable _
        (binding [*out* *err*]
          (println "[enact] WARN fold-turns escrow unreadable — escrow replay disabled for this tick"))
        []))))

(defn- deposit-for-mission
  "Find the first deposit whose :mission matches the target mission
   (id-stem substring match, case-insensitive — handles both 'M-autoclock-in'
   and 'futon3c-d/mission/autoclock-in'). Returns nil if no match."
  [deposits mission]
  (let [stem-lc (str/lower-case (-> (str mission) (str/replace #".*/" "")))]
    (->> deposits
         (filter (fn [d]
                   (let [d-stem (-> (str (:mission d)) (str/replace #".*/" ""))
                         d-lc (str/lower-case d-stem)]
                     (or (str/includes? d-lc stem-lc)
                         (str/includes? stem-lc d-lc)))))
         first)))

(defn- engine-wiring
  "The deterministic EXECUTOR: shell the futon3a fold engine (the Car-3
   apply-cascade! executor) over the cascade's shown patterns. Returns the
   enacted wiring map (`:boxes`/`:policy-holes`) or nil."
  [shown]
  (try
    (let [{:keys [exit out]}
          (shell/sh "bb" "--classpath" "src" fold-engine-rel
                    "apply" (json/generate-string (vec shown))
                    "MissionState -> {Wiring, PolicyHoles}"
                    :dir futon3a-dir)]
      (when (zero? exit)
        ;; the engine returns {:wiring {...} :box-ids :policy-holes ...}; the
        ;; :wiring map itself carries :boxes + :policy-holes — the shape
        ;; fold-eval/coverage consumes.
        (:wiring (json/parse-string out true))))
    (catch Throwable _ nil)))

(defn- act-gates-with-shown
  "Act-gates over the cascade lane, carrying each entry's :shown (the cascade's
   pattern-ids). Reconciliation order: rollout G → classical fold → PINNED
   escrow (L3: the scheduled caller now injects :escrow-turn-fn/:prose-fn with
   the DEPOSIT-GRAIN circumstance, so a recorded fold-turn's coverage ΔG can
   fill a nil :delta-G leg on the live scheduled path — ledger §10's real test).
   Entries without a matching deposit use the 1-arity (byte-identical to pre-L3).
   Legacy :llm-escrow files are still loud-ignored (L1, deprecated)."
  [ranked-actions]
  (let [lane ((requiring-resolve 'futon2.report.cascade-lane/cascade-lane) ranked-actions)
        deposits @!deposits-cache]
    (mapv (fn [entry]
            (let [;; First try the 1-arity (rollout + classical fold).
                  ag (cl/act-gate-from-lane-entry entry)
                  ag (if (some? (:delta-G ag))
                       ag
                       ;; ΔG nil — try the pinned escrow seam (L3).
                       (if-let [dep (deposit-for-mission deposits (:mission entry))]
                         ;; Reconstruct the deposit's OWN circumstance for the
                         ;; sha pin (the lane's ψ won't match — see L3 docstring).
                         (let [dep-circumstance {:mission (:mission dep)
                                                 :psi (get-in dep [:cascade :psi])}
                               dep-shown (get-in dep [:cascade :pattern-ids])
                               ag2 (cl/act-gate-from-lane-entry
                                    (assoc entry :shown dep-shown)
                                    dep-circumstance
                                    {:escrow-turn-fn (esc/escrow-turn-fn deposits)
                                     :prose-fn prose-fn})]
                           ;; The escrow may still abstain (sha mismatch, etc).
                           ;; In that case ag2's delta-G is nil — same as ag.
                           ag2)
                         ;; No deposit for this mission. Legacy loud-ignore (L1):
                         (do (when (escrow-wiring (:mission entry))
                               (binding [*out* *err*]
                                 (println (str "[enact] WARN legacy un-pinned :llm-escrow wiring present for "
                                               (:mission entry)
                                               " — IGNORED (deprecated 2026-07-05; migrate to the pinned fold-turns escrow)"))))
                             ag)))]
              {:mission (:mission entry)
               :shown (vec (:shown entry))
               :act-gate ag
               :verdict (cl/preview-verdict ag)}))
          lane)))

(def ^:dynamic *gamma-escrow-feed?*
  "γ-FEED REWIRE (operator-armed 2026-07-05, bell edge
   invoke-1783280248832-512-8130dc7b): when the gate's ΔG leg came from the
   pinned escrow, that same coverage-ΔG feeds γ's expected leg. Satisfies the
   claude-10 scale-match pin BY CONSTRUCTION — `[:fold-escrow :delta-g]` is
   fold-eval coverage-ΔG over the replayed construction, the same quantity as
   `:realized-G`. Context: classical-off (the 2026-07-05 ruling) severed γ's
   only expected-G source on the live path as an UNLOGGED SIDE EFFECT — γ had
   been coasting at the prior since. Rollout-G remains EXCLUDED from γ (the
   pin's original target). Bind false to revert to classical-only feeding."
  true)

(defn gamma-fold-of
  "The fold output whose coverage-ΔG feeds γ — SOURCE-CONSISTENT with the
   gate's decision:
   - `:delta-G/source :fold-escrow` (flag-gated) ⇒ the ESCROW fold. The
     classical fold's number — when it exists — is deliberately NOT fed for
     escrow-sourced decisions: with classical off as a route it is the known
     under-estimate the ruling distrusted (bayesian: −0.077 vs deposit −0.7),
     and γ must calibrate on the prediction the gate actually acted on.
   - otherwise ⇒ the CLASSICAL fold (unchanged pre-rewire behaviour; covers
     `:fold` and the rollout-sourced case — the gate may verdict on
     rollout-G, but γ's leg is never rollout-G, per the scale-match pin).
   Returning nil/no-ΔG ⇒ γ holds (honest)."
  [act-gate]
  (if (and *gamma-escrow-feed?*
           (= :fold-escrow (:delta-G/source act-gate)))
    (:fold-escrow act-gate)
    (:fold act-gate)))

(defn enact!
  "Enact ONE passed act-gate entry via the deterministic executor. Returns
   {:enacted <wiring-or-nil> :decision {:policy :expected-G :fold}
    :enactment <audit>}. `:enacted` nil ⇒ the executor reproduced nothing —
   the realized-outcome will carry `:realized-G` nil and γ holds (honest).
   γ's expected leg is coverage-ΔG from `gamma-fold-of` (source-consistent;
   never rollout-G). The `:decision`'s `:fold` is the SAME fold γ's leg came
   from — `fold-realized/realized-outcome-of` prefers `(:delta-g fold)`, so
   passing a different fold there would silently override the fed value."
  [{:keys [mission shown act-gate]}]
  (let [enacted (when (seq shown) (engine-wiring shown))
        gfold (gamma-fold-of act-gate)
        gamma-expected (:delta-g gfold)]
    {:enacted enacted
     :decision {:policy mission
                :expected-G gamma-expected
                :fold gfold}
     :enactment {:mission mission
                 :source :classical-engine
                 :predicted-via (:delta-G/source act-gate)
                 :gate-delta-G (:delta-G act-gate)
                 :gamma-expected-G gamma-expected
                 :gamma-source (if (identical? gfold (:fold-escrow act-gate))
                                 :fold-escrow
                                 :fold)
                 :cascade shown
                 :boxes (count (:boxes enacted))
                 :policy-holes (count (:policy-holes enacted))}}))

(defn close-loop!
  "The R16 tick step: act-gates → first :pass → enact → thread the outcome onto
   the judgement via `fold-realized/with-realized-outcome` (the committed seam —
   it honours `fr/*live-wire?*`; bind true in the caller to go live). Also
   attaches `:act-gate-verdicts` + `:enactment` audit fields for the trace.
   Any failure ⇒ judgement returned unchanged; never throws into the run."
  [judgement tick]
  (try
    (let [gates (act-gates-with-shown (:ranked-actions judgement))
          verdicts (mapv (fn [g]
                           {:mission (:mission g)
                            :verdict (:verdict g)
                            :delta-F (get-in g [:act-gate :delta-F])
                            :delta-G (get-in g [:act-gate :delta-G])
                            :delta-G-source (get-in g [:act-gate :delta-G/source])})
                         gates)
          passed (first (filter #(= :pass (:verdict %)) gates))
          {:keys [enacted decision enactment]} (when passed (enact! passed))]
      (cond-> (assoc judgement :act-gate-verdicts verdicts)
        enactment (assoc :enactment enactment)
        passed    (fr/with-realized-outcome decision enacted tick)))
    (catch Throwable _ judgement)))
