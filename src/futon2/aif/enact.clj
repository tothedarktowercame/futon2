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
            [futon2.aif.close-loop :as cl]
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
   pattern-ids). Reconciliation adds the ESCROW source (impl #2) after
   close-loop's rollout/classical: a recorded fold turn's coverage ΔG fills a
   nil :delta-G leg, with `:delta-G/source :llm-escrow` for the bake-off audit."
  [ranked-actions]
  (let [lane ((requiring-resolve 'futon2.report.cascade-lane/cascade-lane) ranked-actions)]
    (mapv (fn [entry]
            (let [ag (cl/act-gate-from-lane-entry entry)
                  ag (if (some? (:delta-G ag))
                       ag
                       (if-let [ew (escrow-wiring (:mission entry))]
                         (if-let [g (fe/coverage-delta-g ew)]
                           (assoc ag :delta-G g
                                  :delta-G/source :llm-escrow
                                  :fold {:wiring ew
                                         :delta-g g
                                         :policy-holes (vec (:policy-holes ew))})
                           ag)
                         ag))]
              {:mission (:mission entry)
               :shown (vec (:shown entry))
               :act-gate ag
               :verdict (cl/preview-verdict ag)}))
          lane)))

(defn enact!
  "Enact ONE passed act-gate entry via the deterministic executor. Returns
   {:enacted <wiring-or-nil> :decision {:policy :expected-G :fold}
    :enactment <audit>}. `:enacted` nil ⇒ the executor reproduced nothing —
   the realized-outcome will carry `:realized-G` nil and γ holds (honest)."
  [{:keys [mission shown act-gate]}]
  (let [enacted (when (seq shown) (engine-wiring shown))]
    {:enacted enacted
     :decision {:policy mission
                :expected-G (:delta-G act-gate)
                :fold (:fold act-gate)}
     :enactment {:mission mission
                 :source :classical-engine
                 :predicted-via (:delta-G/source act-gate)
                 :expected-G (:delta-G act-gate)
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
