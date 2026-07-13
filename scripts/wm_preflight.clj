(ns wm-preflight
  "G4 — readiness preflight for a War Machine end-to-end run.

   Answers: if the loop ran over mission M right now, would it actually
   deliberate AND act — or silently no-op? It surfaces the toggles + deposit
   state that otherwise fail *silently* (selection gain holds, no error), which is exactly
   how a fresh-mission run stalls invisibly.

   Usage:
     clojure -M:wm-preflight                 # global arm/toggle state + deposit coverage
     clojure -M:wm-preflight M-foo M-bar ...  # per-mission READY / NOT-READY verdict

   Reads code-default arm values (a standalone JVM has no bindings, so these are
   the defaults a scheduled tick starts from) and the FUTON_WM_* env escape
   hatches. It does NOT mutate anything."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.fold-realized :as fr]
            [futon2.aif.actuator-a6 :as a6]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [clojure.string :as str]))

(defn- onoff [b] (if b "ON " "off"))

(defn- stem [x] (-> (str x) (str/replace #".*/" "") str/lower-case))

(defn- deposit-for?
  "Mirror of enact/deposit-for-mission: case-insensitive id-stem substring match."
  [missions target]
  (let [s (stem target)]
    (boolean (some #(let [m (stem %)] (or (str/includes? m s) (str/includes? s m)))
                   missions))))

(defn -main [& missions]
  (println "══ WM readiness preflight (G4) ══")

  (println "\n── Deliberation arms (Tier-1, world-inert; change thinking) ──")
  (doseq [[label v] [["pattern model-uncertainty bonus" a6/*pattern-grain-model-uncertainty?*]
                     ["r3d-multichannel?   (R3d 8-channel)" belief/*r3d-multichannel?*]
                     ["risk-mode           (R5a)"          (or (System/getenv "FUTON_WM_RISK_MODE") ":kl (default)")]
                     ["ambiguity-mode      (R5b)"          (or (System/getenv "FUTON_WM_AMBIGUITY_MODE") ":gaussian-entropy (default)")]]]
    (println (format "  %-38s %s" label (if (boolean? v) (onoff v) v))))

  (println "\n── Acting arms (Tier-2; move real dials on a run) ──")
  (let [live-wire   fr/*live-wire?*
        selection-gain-feed fr/*selection-gain-grounded-feed?*
        escrow      cl/*escrow-replay?*
        classical   cl/*classical-fold-score?*
        env-wire    (System/getenv "FUTON_WM_LIVE_WIRE")
        enact-armed (and live-wire escrow (not classical))]
    (doseq [[label v] [["live-wire?          (R16 enactment)"    live-wire]
                       ["selection-gain grounded feed (R14)" selection-gain-feed]
                       ["escrow-replay?      (fold replay seam)"  escrow]
                       ["classical-fold-score?  (must be OFF)"       classical]]]
      (println (format "  %-38s %s" label (onoff v))))
    (when env-wire (println (format "  %-38s %s" "[env] FUTON_WM_LIVE_WIRE" env-wire)))
    (println (format "  → enactment ARMED (live-wire ∧ escrow-replay ∧ ¬classical): %s"
                     (if enact-armed "YES" "NO")))

    (println "\n── Deposits (the #1 silent-stall gate) ──")
    (let [{:keys [deposits]} (try (esc/load-deposits)
                                  (catch Throwable t {:deposits ::err :err (.getMessage t)}))]
      (if (= deposits ::err)
        (println "  ✗ escrow unreadable — escrow replay disabled; every mission NOT-READY")
        (let [dmissions (mapv :mission deposits)]
          (println (format "  %d deposit(s) loaded + sha-gated." (count deposits)))
          (if (seq missions)
            (do
              (println "\n── Per-mission verdict ──")
              (doseq [m missions]
                (let [has (deposit-for? dmissions m)
                      ready (and enact-armed has)]
                  (println (format "  %-32s deposit:%-4s → %s"
                                   m (if has "yes" "NONE") (if ready "READY ✓" "NOT-READY ✗")))
                  (when-not has
                    (println "      ↳ no deposit → fold abstains, ΔG nil, γ holds (silent no-op — the G1 gap)"))
                  (when-not enact-armed
                    (println "      ↳ acting arms not aligned (see Tier-2 above)")))))
            (do
              (println "  missions WITH a deposit (runnable without new authoring):")
              (doseq [m (sort (distinct dmissions))] (println "    •" m))
              (println "\n  (pass mission ids as args for a per-mission READY verdict)"))))))))
