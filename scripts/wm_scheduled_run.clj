(ns wm-scheduled-run
  "Scheduled-execution entrypoint for the WM AIF apparatus (R10 graduation).

   One-shot invocation: scans the futon stack, runs the AIF judgement
   layer, persists a per-call trace record via `futon2.aif.trace`, and
   exits with a one-line summary to stdout. Designed to run under cron
   / systemd-timer / Drawbridge-driven schedule. The recommended cron
   cadence is hourly (`0 * * * *`) — see the §'R10' entry in
   `~/code/futon2/docs/futon-aif-completeness.md` for the schedule
   rationale and the operator cron command.

   Contract: contributes to R10 (live operation) per
   `futon2/docs/futon-aif-completeness.md`. Cross-maps to F2 (liveness
   invariant) at stack scope — WM's scheduled runs become a new
   liveness contributor beyond watcher daemon + satisficing-zapper.

   Resilience: all work wrapped in a top-level try/catch so an
   intermittent dependency (e.g. futon3c being momentarily down)
   doesn't break the schedule. Failure modes print to stderr with a
   non-zero exit code so cron can surface them via its standard
   error-mail mechanism."
  (:require [futon2.aif.trace :as trace]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.enact :as enact]
            [futon2.aif.fold-realized :as fr]
            [futon2.report.war-machine :as wm])
  (:import (java.time Instant)))

(defn- live-wire?
  "R16 enactment switch for THIS runner (Joe-ratified ON, 2026-07-02).
   `FUTON_WM_LIVE_WIRE=0` (or `false`) disables — the operator-visible
   escape hatch. The `fold-realized/*live-wire?*` dynamic var keeps its
   global default (false) for every other consumer; we bind it only
   around this runner's enactment step."
  []
  (not (contains? #{"0" "false"} (System/getenv "FUTON_WM_LIVE_WIRE"))))

(defn- summarise
  "One-line summary of a WM run for stdout/cron logs."
  [judgement trace-path]
  (let [decision (:decision judgement)
        action (:action decision)
        action-desc (cond
                      (= :abstain action)
                      (str "ABSTAIN reason=" (:reason decision)
                           " gaps=" (count (:gap-report decision)))

                      (map? action)
                      (str (name (:type action))
                           (cond
                             (:target action) (str " target=" (:target action))
                             (:target-class action) (str " target-class=" (:target-class action))
                             :else "")
                           " G=" (format "%.4f" (double (:G-total decision))))

                      :else (str "?" action))
        mode (:mode judgement)
        n-actions (count (:ranked-actions judgement))]
    (str (str (Instant/now))
         " mode=" (name mode)
         " candidates=" n-actions
         " decision=" action-desc
         " trace=" trace-path)))

(defn -main
  "Entrypoint. Optional first arg: scan-window-days (default 14)."
  [& args]
  (try
    (let [days (if (seq args) (Integer/parseInt (first args)) 14)
          ;; B-0a tick provenance (M-aif-faithfulness §2.0): stamp WHICH code
          ;; + WHICH config produced this tick — git sha/dirty of this one-shot
          ;; JVM's checkout, the arena-resolved mode flags (the same fns the
          ;; rank lanes call), and the trace schema version. Computed FIRST —
          ;; at JVM start, closest to what this JVM actually loaded — not at
          ;; trace-write time minutes later: in the shared live tree a mid-run
          ;; commit would otherwise shift the recorded sha off the loaded code
          ;; (observed on the first stamped tick, 2026-07-04 07:00Z).
          ;; :live-wire? joins below once resolved.
          version-stamp (trace/wm-version-stamp (wm/arena-mode-flags))
          ;; E-C-vector-live: keep the belly fresh BEFORE scoring. Off-cycle
          ;; (once per scheduled tick, not per candidate action) — derive the
          ;; live C only when the goal/hole corpus changed (maybe-refresh!).
          ;; Without this the belly is [] and EFE risk drops C entirely
          ;; (builds toward nothing). Degrades safely: store down -> [] -> the
          ;; static floor; never throws the run.
          belly (try (cv/maybe-refresh!) (catch Exception _ {:entries []}))
          {:keys [judgement]} (wm/generate-war-machine days)
          ;; R16 close-the-loop (live-wired 2026-07-02): act-gates over the
          ;; judged actions; first :pass is ENACTED (artifact-only — escrow
          ;; impl #2 else fold-engine impl #1) and the :realized-outcome
          ;; record rides the trace to R14's γ next tick. Tick = epoch-ms
          ;; (γ dedups on it). Guarded inside close-loop!: any failure
          ;; returns the judgement unchanged.
          wired? (live-wire?)
          judgement (if wired?
                      (binding [fr/*live-wire?* true]
                        (enact/close-loop! judgement (System/currentTimeMillis)))
                      judgement)
          ;; `(trace/wm-version-of record)` recovers the stamp built above.
          judgement (assoc judgement :wm-version
                           (assoc version-stamp :live-wire? wired?))
          trace-path (trace/write-trace! judgement)]
      (println (str (summarise judgement trace-path)
                    " belly=" (count (:entries belly))
                    (when (:derived-at belly) (str " derived=" (:derived-at belly)))
                    " gates=" (pr-str (frequencies (map :verdict (:act-gate-verdicts judgement))))
                    (when-let [e (:enactment judgement)]
                      (str " ENACTED=" (:mission e) " src=" (name (:source e))
                           " boxes=" (:boxes e) " holes=" (:policy-holes e)))
                    (when-let [ro (:realized-outcome judgement)]
                      (str " realizedG=" (:realized-G ro) " expectedG=" (:expected-G ro)))))
      (System/exit 0))
    (catch Throwable t
      (binding [*out* *err*]
        (println (str (Instant/now)) "ERROR" (.getMessage t)))
      (System/exit 1))))
