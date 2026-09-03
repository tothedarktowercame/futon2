#!/usr/bin/env clojure
;; U42 -- the U21 replay re-run with the PROBE REMOVED FROM THE PATH.
;;
;;   clojure -M holes/labs/wm-contract/u42_gauge_producers.clj [outdir]
;;
;; WHAT U21 LEFT. U18 bound M-zaif-harness-v1's three completion criteria to
;; three observables; nothing produced them, so the criteria reached risk_mis
;; and stopped at 3/3 :undeclared-observable. U21 supplied a DECLARED PROBE
;; ({:worklist-acceptance-state 1.0 :reporting-gate-test-result 0.0
;;   :registry-gap-list-present 1.0}) to show what the same shipped path
;; returns once producers exist, and minted this row for the producers.
;;
;; WHAT THIS RUNS. The same two shipped functions U21 called
;; (`war-machine/tick-mission-focus`, `war-machine/mission-c-readback`) on the
;; same three persisted 2026-09-02 records, with the observation the readback
;; reads assembled the way `war_machine.clj` now assembles it: the tick's own
;; observation merged with `futon2.aif.mission-gauges/reading`'s measured
;; values. NOTHING IS CONSTRUCTED HERE. The probe is read from U21's committed
;; artifact for comparison only and is never handed to the readback.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only, no live tick, no run lock.

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.mission-gauges :as gauges]
         'futon2.report.war-machine)

(def focus-of
  "scripts/futon2/report/war_machine.clj:1569 tick-mission-focus."
  #'futon2.report.war-machine/tick-mission-focus)

(def readback
  "scripts/futon2/report/war_machine.clj:1679 mission-c-readback."
  #'futon2.report.war-machine/mission-c-readback)

(def selection-focus-flag #'futon2.report.war-machine/*selection-focus?*)

(defn fmt [f & args] (apply format f args))

(def trace-dir "data/wm-trace")
(def corpus-from "wm-trace-2026-09-02.edn")

(defn corpus-files []
  (->> (.listFiles (io/file trace-dir))
       (map #(.getName %))
       (filter #(and (str/starts-with? % "wm-trace-")
                     (str/ends-with? % ".edn")
                     (>= (compare % corpus-from) 0)))
       sort
       vec))

(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader (io/file trace-dir f)))]
    (loop [acc []]
      (let [x (read {:eof ::eof :default (fn [_ v] v)} r)]
        (if (= x ::eof) acc (recur (conj acc x)))))))

(def records (vec (mapcat read-records (corpus-files))))

(def u21-probe-path
  "holes/labs/wm-contract/runs/U21-selection-focus/probe-observation.edn")

(def u21-probe
  "U21's committed probe, read for COMPARISON ONLY -- it is never handed to the
   readback below."
  (read-string (slurp u21-probe-path)))

(defn arm
  "One flag-on readback on this record's own fields, with the observation given."
  [rec observation]
  (with-bindings {selection-focus-flag true}
    (let [focus (focus-of (:active-mission rec) (:decision rec))]
      {:focus focus :readback (readback focus (:ranked-actions rec) observation)})))

(defn -main [& args]
  (let [outdir (io/file (or (first args)
                            "holes/labs/wm-contract/runs/U42-producers"))
        sb (StringBuilder.)
        emit (fn [& xs] (.append sb (str (str/join " " xs) "\n")) nil)
        produced (gauges/reading)
        observables (:observables produced)
        rows
        (vec
         (for [rec records]
           (let [r (arm rec (merge (:observation rec) observables))
                 rb (:readback r)]
             {:run-id (:run/id rec)
              :timestamp (:timestamp rec)
              :selected-mission (get-in rec [:decision :action :target])
              :readback (select-keys rb [:mission :status :reason :criterion-count
                                         :measurable-count :criteria-source
                                         :criteria-source-sha256 :declared-gauges
                                         :risk-mis-per-criterion
                                         :action-sensitivity])
              :unmeasurable-reasons (mapv :reason (:unmeasurable rb))
              :risk-values (vec (distinct (map :risk-mis (:per-mission-action rb))))})))
        agreement
        (into (sorted-map)
              (for [[k probe-value] u21-probe]
                [k {:u21-probe probe-value
                    :produced (get observables k ::absent)
                    :agrees? (= probe-value (get observables k ::absent))}]))
        measurements
        {:row :U42
         :generated-by "holes/labs/wm-contract/u42_gauge_producers.clj"
         :corpus {:dir trace-dir :files (corpus-files) :record-count (count records)}
         :producers {:ns "src/futon2/aif/mission_gauges.clj"
                     :call-site "scripts/futon2/report/war_machine.clj mission-c-fields"
                     :reading produced}
         :probe-comparison {:probe-artifact u21-probe-path
                            :probe u21-probe
                            :per-observable agreement}
         :rows rows}]
    (.mkdirs outdir)
    (emit "U42 -- the three gauge observables, produced")
    (emit "==========================================")
    (emit "")
    (emit "Corpus:" (str/join ", " (corpus-files)) "--" (count records) "records.")
    (emit "Replay only: no live tick, no run lock, nothing written under data/.")
    (emit "")
    (emit "1. WHAT EACH PRODUCER READ")
    (emit "")
    (doseq [r (:records produced)]
      (emit (fmt "   %-28s %s"
                 (name (:observable r))
                 (if (= :measured (:status r))
                   (fmt "= %s" (:value r))
                   (fmt "ABSENT (%s)" (name (:reason r))))))
      (doseq [s (:sources r)]
        (emit (fmt "     source  %s%s" (:path s)
                   (if (:sha256 s) (fmt "  sha256 %s" (subs (:sha256 s) 0 16)) ""))))
      (when-let [wn (:would-need r)] (emit (fmt "     needs   %s" wn)))
      (when-let [reason (get-in r [:basis :reason])]
        (emit (fmt "     basis   %s" (name reason))))
      (emit ""))
    (emit "2. AGAINST U21'S DECLARED PROBE")
    (emit "")
    (emit (fmt "   %-28s %-12s %-12s %s" "observable" "U21 probe" "produced" "agrees?"))
    (doseq [[k v] agreement]
      (emit (fmt "   %-28s %-12s %-12s %s" (name k)
                 (str (:u21-probe v))
                 (if (= ::absent (:produced v)) "(no key)" (str (:produced v)))
                 (:agrees? v))))
    (emit "")
    (emit "3. THE READBACK, PROBE REMOVED FROM THE PATH")
    (emit "")
    (doseq [r rows]
      (emit (fmt "   run %s -- selected %s" (subs (str (:run-id r)) 0 8)
                 (:selected-mission r)))
      (emit (fmt "     mission=%-38s status=%s reason=%s"
                 (str (:mission (:readback r)))
                 (name (or (:status (:readback r)) :nil))
                 (name (or (:reason (:readback r)) :none))))
      (emit (fmt "     criteria=%s measurable=%s risk_mis=%s"
                 (:criterion-count (:readback r))
                 (:measurable-count (:readback r))
                 (str (:risk-values r))))
      (when (seq (:unmeasurable-reasons r))
        (emit (fmt "     unmeasurable: %s"
                   (str/join ", " (map name (:unmeasurable-reasons r))))))
      (emit ""))
    (spit (io/file outdir "U42-PRODUCERS.txt") (str sb))
    (spit (io/file outdir "measurements.edn")
          (with-out-str (pp/pprint measurements)))
    (print (str sb))
    (flush)))

(apply -main *command-line-args*)
