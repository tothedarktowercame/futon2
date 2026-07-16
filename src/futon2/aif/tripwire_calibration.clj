(ns futon2.aif.tripwire-calibration
  "Retro-trip identifiability calibration over the War Machine incident museum."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]))

(def default-phase-log
  "/home/joe/code/futon2/data/wm-tripwires/incidents/attempt-006-author-wait.edn.log")
(def attempt-006-source-ledger
  "/home/joe/code/futon2/data/wm-full-loop-phases.edn.log")
(def default-coverage-path
  "/home/joe/code/futon2/data/wm-tripwires/coverage-v2.edn")
(def run-5b-incident-path
  "/home/joe/code/futon2/data/wm-tripwires/incidents/run-5b-artifact-binding.edn")

(defn- temp-root [prefix]
  (.getPath (.toFile (Files/createTempDirectory
                      prefix
                      (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- read-edn-lines [path]
  (with-open [reader (io/reader path)]
    (->> (line-seq reader)
         (keep (fn [line]
                 (try (edn/read-string line) (catch Throwable _ nil))))
         doall)))

(defn attempt-006-observation
  "Reconstruct the recorded late-author timeout boundary from the real phase
  ledger. The then-live inactivity budget was 300000 ms."
  ([] (attempt-006-observation default-phase-log))
  ([phase-log]
   (let [event (->> (read-edn-lines phase-log)
                    (filter #(and (= "attempt-006" (:attempt-id %))
                                  (= :author-wait (:phase %))
                                  (= :end (:transition %))))
                    last)]
     (when-not event
       (throw (ex-info "attempt-006 author-wait event is absent"
                       {:phase-log phase-log})))
     (assoc event :phase-budget-ms 300000))))

(defn- tripped-wire-ids [observation]
  (->> (keys tripwire/wire-evaluators)
       (filter #(seq (tripwire/evaluate-wire % observation)))
       set))

(defn- mixed-image-wire-ids []
  (let [original @#'repair/open-obligations]
    (try
      (alter-var-root #'repair/open-obligations
                      (constantly (fn [& _] [:finding-7-mixed-image])))
      (tripped-wire-ids {:tripwire/force? true})
      (finally
        (alter-var-root #'repair/open-obligations (constantly original))))))

(defn calibration-incidents []
  (let [c1-history [{:attempt-id "c1-1" :selected-stop-line "repair-terminal"
                     :fresh-commit nil :failure-kind :recovery-job-terminal}
                    {:attempt-id "c1-2" :selected-stop-line "repair-terminal"
                     :fresh-commit nil :failure-kind :recovery-job-terminal}
                    {:attempt-id "c1-3" :selected-stop-line "repair-terminal"
                     :fresh-commit nil :failure-kind :recovery-job-terminal}]
        duplicate (fn [id]
                    {:repair/id id :failure-kind :independent-review-rejected
                     :target "M-rejected" :failed-commit "deadbeef"})]
    [{:incident/id :attempt-006-late-author-artifact
      :incident/kind :false-inactivity-timeout
      :source {:extracted-phase-record default-phase-log
               :source-ledger attempt-006-source-ledger
               :attempt-id "attempt-006"}
      :expected-wires #{:T9}
      :tripped-wires (tripped-wire-ids (attempt-006-observation))}
     {:incident/id :c1-terminal-wedge
      :incident/kind :unresolved-stop-line-reselected
      :source :synthetic-from-c1-ledger
      :expected-wires #{:T7}
      :tripped-wires
      (tripped-wire-ids {:cohort-history c1-history
                         :closed-repair-ids #{}
                         :tripwire/force? true})}
     {:incident/id :c2-rejection-livelock
      :incident/kind :duplicate-finding-mint
      :source :synthetic-from-c2-ledger
      :expected-wires #{:T8}
      :tripped-wires
      (tripped-wire-ids {:findings (mapv duplicate ["c2-1" "c2-2" "c2-3"])
                         :tripwire/force? true})}
     {:incident/id :finding-7-mixed-image
      :incident/kind :loaded-file-code-divergence
      :source :synthetic-var-redefinition
      :expected-wires #{:T10}
      :tripped-wires (mixed-image-wire-ids)}
     {:incident/id :run-5b-author-artifact-binding
      :incident/kind :narrated-predecessor-bound-to-review
      :source {:extracted-incident run-5b-incident-path
               :attempt-id "attempt-013"}
      :expected-wires #{:T13}
      :tripped-wires
      (tripped-wire-ids
       (assoc (edn/read-string (slurp run-5b-incident-path))
              :tripwire/force? true))}]))

(defn coverage-report []
  (let [incidents (calibration-incidents)
        identified (count (filter #(seq (:tripped-wires %)) incidents))
        total (count incidents)]
    (doseq [{:keys [incident/id expected-wires tripped-wires]} incidents]
      (when-not (= expected-wires tripped-wires)
        (throw (ex-info "Calibration incident did not identify as specified"
                        {:incident/id id :expected expected-wires
                         :actual tripped-wires}))))
    {:tripwire-coverage/schema-version 1
     :generated-at (str (Instant/now))
     :metric :identified-pathology-classes/known-pathology-classes
     :identified identified
     :total total
     :coverage (/ (double identified) total)
     :incidents incidents
     :uncovered-incidents
     (mapv :incident/id (remove #(seq (:tripped-wires %)) incidents))}))

(defn- report-comparable [report]
  (dissoc report :generated-at))

(defn persist-coverage!
  "CREATE_NEW on first calibration. A repeat run verifies the immutable report
  rather than silently replacing museum evidence."
  ([report] (persist-coverage! default-coverage-path report))
  ([path report]
   (let [file (io/file path)]
     (if (.exists file)
       (let [persisted (edn/read-string (slurp file))]
         (when-not (= (report-comparable persisted) (report-comparable report))
           (throw (ex-info "Immutable coverage report differs from replay"
                           {:path path :persisted persisted :replay report})))
         path)
       (do
         (io/make-parents file)
         (Files/write (.toPath file)
                      (.getBytes (with-out-str (pp/pprint report)) "UTF-8")
                      (into-array StandardOpenOption
                                  [StandardOpenOption/CREATE_NEW
                                   StandardOpenOption/WRITE]))
         path)))))

(defn -main [& _]
  (let [repair-root (temp-root "wm-calibration-repair-")
        trip-root (temp-root "wm-calibration-trips-")]
    (with-redefs [repair/default-root repair-root
                  tripwire/default-trip-root trip-root]
      (let [report (coverage-report)
            path (persist-coverage! report)]
        (doseq [{:keys [incident/id tripped-wires]} (:incidents report)]
          (println (str (name id) ": " (pr-str (vec (sort tripped-wires))))))
        (println (format "COVERAGE %d/%d = %.3f"
                         (:identified report) (:total report) (:coverage report)))
        (println "coverage-report:" path)
        (println "repair-root:" repair-root "(temporary)")
        report))))
