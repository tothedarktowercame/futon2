#!/usr/bin/env clojure
;; U21 -- selection -> clocking, the same-tick half (ported from zaif S4).
;;
;;   clojure -M holes/labs/wm-contract/u21_selection_focus.clj [outdir]
;;
;; WHAT THIS ROW OWES: the seam named with pointers, and a one-tick
;; demonstration (replay acceptable) in which the SELECTED mission's criteria
;; reach risk_mis via the U18 gauge path. Gaps that are not trivial come back
;; as rows, not silent fills.
;;
;; THE SHIPPED PATH IS WHAT IS MEASURED. Every number below comes from
;; `war-machine/tick-mission-focus` and `war-machine/mission-c-readback`
;; called on fields of the persisted 2026-09-02 trace records. This script
;; constructs exactly one thing, and says so where it does: a PROBE
;; observation carrying the three gauge observables, used only to show what
;; the same path returns once a producer for them exists. It is written to
;; disk as an artifact so the reviewer reads the bytes the measurement did.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only, no live tick, no run lock.

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         'futon2.report.war-machine)

(import '[java.security MessageDigest])

(def focus-of
  "The shipped focus resolver, var-quoted because it is private to
   war_machine. scripts/futon2/report/war_machine.clj:1569."
  #'futon2.report.war-machine/tick-mission-focus)

(def readback
  "The shipped C_mis readback. scripts/futon2/report/war_machine.clj:1679."
  #'futon2.report.war-machine/mission-c-readback)

(def selection-focus-flag #'futon2.report.war-machine/*selection-focus?*)

(defn fmt [f & args] (apply format f args))

;; ---------------------------------------------------------------------------
;; Corpus -- the same three records U12 read, selected by filename date so a
;; later tick enters without an edit here.

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

(defn sha256 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(fmt "%02x" %) d))))

;; ---------------------------------------------------------------------------
;; The probe. DECLARED, not planted into the reading: it supplies the three
;; observables the U18 gauges NAME and that nothing in R2 produces, and it
;; touches nothing else. Its whole purpose is to separate "the wiring does not
;; reach risk_mis" from "the wiring reaches risk_mis and the producer is
;; missing" -- which are two different repairs and one row.

(def probe-observation
  {:worklist-acceptance-state 1.0
   :reporting-gate-test-result 0.0
   :registry-gap-list-present 1.0})

(defn record-focus
  "The focus each of the two arms hands the readback, from THIS record's own
   fields: `:active-mission` is what the tick's S4 read returned, `:decision`
   is what the tick selected."
  [rec on?]
  (with-bindings {selection-focus-flag on?}
    (focus-of (:active-mission rec) (:decision rec))))

(defn arm [rec on? observation]
  (let [focus (record-focus rec on?)]
    {:focus focus
     :readback (with-bindings {selection-focus-flag on?}
                 (readback focus (:ranked-actions rec) observation))}))

(defn -main [& args]
  (let [outdir (io/file (or (first args) "holes/labs/wm-contract/runs/U21-selection-focus"))
        sb (StringBuilder.)
        emit (fn [& xs] (.append sb (str (str/join " " xs) "\n")) nil)
        rows
        (vec
         (for [rec records]
           (let [durable (arm rec false (:observation rec))
                 selected (arm rec true (:observation rec))
                 probed (arm rec true (merge (:observation rec) probe-observation))]
             {:run-id (:run/id rec)
              :timestamp (:timestamp rec)
              :selected-mission (get-in rec [:decision :action :target])
              :durable-clock-mission (get-in rec [:active-mission :mission-id])
              :durable-clock-reason (get-in rec [:active-mission :reason])
              :focus-flag-off (:focus durable)
              :focus-flag-on (:focus selected)
              :flag-off-readback
              (select-keys (:readback durable)
                           [:mission :status :reason :criterion-count
                            :measurable-count :criteria-source
                            :criteria-source-sha256 :declared-gauges])
              :flag-on-readback
              (select-keys (:readback selected)
                           [:mission :status :reason :criterion-count
                            :measurable-count :criteria-source
                            :criteria-source-sha256 :declared-gauges])
              :flag-on-unmeasurable-reasons
              (mapv :reason (:unmeasurable (:readback selected)))
              :probe-readback
              (select-keys (:readback probed)
                           [:mission :status :reason :criterion-count
                            :measurable-count :risk-mis-per-criterion
                            :action-sensitivity :mission-action-count
                            :non-mission-action-count])
              :probe-risk-values
              (vec (distinct (map :risk-mis
                                  (:per-mission-action (:readback probed)))))})))
        lagging (filterv #(and (:selected-mission %)
                               (not= (:selected-mission %)
                                     (:durable-clock-mission %)))
                         rows)
        measurements
        {:row :U21
         :generated-by "holes/labs/wm-contract/u21_selection_focus.clj"
         :corpus {:dir trace-dir :files (corpus-files)
                  :record-count (count records)}
         :seam
         {:focus-read "scripts/futon2/report/war_machine.clj:1539 load-active-mission, called at :5348"
          :clock-write "scripts/futon2/report/war_machine.clj:1786 record-selection-clock!, called from write-trace-and-clock! at :1828"
          :decision "scripts/futon2/report/war_machine.clj:5859 wm-decision"
          :focus-resolver "scripts/futon2/report/war_machine.clj:1569 tick-mission-focus, called at :5942"
          :readback "scripts/futon2/report/war_machine.clj:1679 mission-c-readback, called at :5947"
          :projection "scripts/futon2/report/war_machine.clj:1595 carry-mission-focus, applied at :6063"
          :gauges "scripts/futon2/report/war_machine.clj:1642 mission-c-declared-gauges (U18)"
          :flag "scripts/futon2/report/war_machine.clj:101 *selection-focus?* / FUTON_WM_SELECTION_FOCUS"}
         :probe {:observation probe-observation
                 :sha256 (sha256 (pr-str probe-observation))
                 :what "the three observables the U18 gauges name; supplied to
                        show what the same shipped path returns once a producer
                        exists, NOT to score any mission"}
         :lag-count (count lagging)
         :rows rows}]
    (.mkdirs outdir)
    (emit "U21 -- selection -> clocking, the same-tick half")
    (emit "=================================================")
    (emit "")
    (emit "Corpus:" (str/join ", " (corpus-files)) "--" (count records) "records.")
    (emit "Replay only: no live tick, no run lock, nothing written under data/.")
    (emit "")
    (emit "1. THE SEAM, WITH POINTERS")
    (emit "")
    (doseq [[k v] (sort-by key (:seam measurements))]
      (emit (fmt "   %-16s %s" (name k) v)))
    (emit "")
    (emit "   The tick reads the durable clock at its TOP (load-active-mission,")
    (emit "   called at war_machine.clj:5348) and writes the new edge AFTER the")
    (emit "   trace is persisted (record-selection-clock!, launched from")
    (emit "   write-trace-and-clock!). So the focus a tick reads is the PREVIOUS")
    (emit "   tick's selection, and the mission this tick selects parameterizes")
    (emit "   nothing until the next one.")
    (emit "")
    (emit "2. THE LAG, MEASURED ON THE CORPUS")
    (emit "")
    (emit (fmt "   %-38s %-38s %s" "SELECTED this tick" "FOCUS the tick read" "run"))
    (doseq [r rows]
      (emit (fmt "   %-38s %-38s %s"
                 (str (:selected-mission r))
                 (str (or (:durable-clock-mission r)
                          (str "-- " (name (or (:durable-clock-reason r) :unknown)))))
                 (subs (str (:run-id r)) 0 8))))
    (emit "")
    (emit (fmt "   %d of %d records selected a mission the focus read did not name."
               (count lagging) (count rows)))
    (emit "")
    (emit "3. FLAG ON: THE SELECTED MISSION'S CRITERIA REACH risk_mis")
    (emit "")
    (doseq [r rows]
      (emit (fmt "   run %s -- selected %s" (subs (str (:run-id r)) 0 8)
                 (:selected-mission r)))
      (emit (fmt "     flag off  mission=%-38s status=%s reason=%s"
                 (str (:mission (:flag-off-readback r)))
                 (name (or (:status (:flag-off-readback r)) :nil))
                 (name (or (:reason (:flag-off-readback r)) :none))))
      (emit (fmt "     flag on   mission=%-38s status=%s reason=%s"
                 (str (:mission (:flag-on-readback r)))
                 (name (or (:status (:flag-on-readback r)) :nil))
                 (name (or (:reason (:flag-on-readback r)) :none))))
      (emit (fmt "               criteria=%s measurable=%s gauges=%s"
                 (:criterion-count (:flag-on-readback r))
                 (:measurable-count (:flag-on-readback r))
                 (if (:declared-gauges (:flag-on-readback r))
                   (str (count (:declared-gauges (:flag-on-readback r)))
                        " declared (U18)")
                   "none for this mission")))
      (when (seq (:flag-on-unmeasurable-reasons r))
        (emit (fmt "               unmeasurable: %s"
                   (str/join ", " (map name (:flag-on-unmeasurable-reasons r))))))
      (emit (fmt "     probe     status=%s measurable=%s risk_mis=%s"
                 (name (or (:status (:probe-readback r)) :nil))
                 (:measurable-count (:probe-readback r))
                 (str (:probe-risk-values r))))
      (emit ""))
    (emit "4. WHAT THIS DOES NOT DO")
    (emit "")
    (emit "   The flag is default OFF and the flag-off judgement is byte-identical:")
    (emit "   tick-mission-focus returns the durable read unchanged, and")
    (emit "   carry-mission-focus adds no field. Flipping the default is J-gated.")
    (emit "   The focus is derived from wm-decision, which is final where it is")
    (emit "   bound, and is read only by mission-c-readback and the projection --")
    (emit "   both after ranking and selection, so nothing it changes can move a")
    (emit "   rank, a weight, a temperature, an admissibility verdict or a choice.")
    (emit "   It mints NO clock edge: a clock edge is witnessed evidence and this")
    (emit "   is a projection of the tick's own decision, which is why")
    (emit "   :active-mission keeps its meaning and :mission-focus is separate.")
    (emit "")
    (spit (io/file outdir "U21-SELECTION-FOCUS.txt") (str sb))
    (spit (io/file outdir "measurements.edn")
          (with-out-str (pp/pprint measurements)))
    (spit (io/file outdir "probe-observation.edn")
          (with-out-str (pp/pprint probe-observation)))
    (print (str sb))
    (flush)))

(apply -main *command-line-args*)
