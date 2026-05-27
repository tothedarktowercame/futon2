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
            [futon2.report.war-machine :as wm])
  (:import (java.time Instant)))

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
          {:keys [judgement]} (wm/generate-war-machine days)
          trace-path (trace/write-trace! judgement)]
      (println (summarise judgement trace-path))
      (System/exit 0))
    (catch Throwable t
      (binding [*out* *err*]
        (println (str (Instant/now)) "ERROR" (.getMessage t)))
      (System/exit 1))))
