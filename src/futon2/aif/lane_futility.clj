(ns futon2.aif.lane-futility
  "E-live-loop-2 3-series: per-lane futility observables and dry-run feedback.

   A lane is the selected WM action class plus its selected target. A lane only
   gets success credit when that same target receives a :pass act-gate verdict in
   the trace record. Other passes in the same tick are real evidence, but not
   evidence that the selected lane paid."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.selection-gain :as selection-gain])
  (:import (java.io RandomAccessFile)
           (java.nio.file Files StandardCopyOption)))

(def default-trace-dir "data/wm-trace")
(def default-expected-coverage-dg -0.25)
(def default-futility-threshold 5)

(defn trace-files
  ([] (trace-files default-trace-dir))
  ([trace-dir]
   (->> (.listFiles (io/file trace-dir))
        (filter #(and (.isFile %)
                      (re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn"
                                  (.getName %))))
        (sort-by #(.getName %)))))

(defn read-trace-file
  "Read one wm-trace file. Each line is one EDN form; tagged Java objects are
   carried as their readable payloads because this report only inspects maps."
  [file]
  (with-open [r (io/reader file)]
    (doall
     (map #(edn/read-string {:default (fn [_tag value] value)} %)
          (remove str/blank? (line-seq r))))))

(defn trace-records
  ([] (trace-records default-trace-dir))
  ([trace-dir]
   (mapcat read-trace-file (trace-files trace-dir))))

(defn action-class [record]
  (get-in record [:decision :action :type]))

(defn action-target [record]
  (get-in record [:decision :action :target]))

(defn lane-id
  ([record] (lane-id (action-class record) (action-target record)))
  ([class target]
   (str (or (some-> class name) "unknown")
        (when (some? target) (str "/" target)))))

(defn lane-record
  [record]
  (when-let [class (action-class record)]
    {:action-class class
     :target (action-target record)
     :lane (lane-id record)}))

(defn target-pass?
  "True only when the selected lane's target itself receives a pass verdict."
  [record]
  (let [target (some-> (action-target record) str)]
    (boolean
     (and target
          (some #(and (= :pass (:verdict %))
                      (= target (some-> (:mission %) str)))
                (:act-gate-verdicts record))))))

(defn target-verdict
  [record]
  (let [target (some-> (action-target record) str)]
    (first
     (filter #(= target (some-> (:mission %) str))
             (:act-gate-verdicts record)))))

(def ^:private index-schema 1)
(def ^:private index-filename ".lane-futility-index.edn")
(def ^:private lock-filename ".lane-futility-index.lock")

(defn- corpus-fingerprint
  [trace-dir]
  (mapv (fn [^java.io.File f]
          [(.getName f) (.length f) (.lastModified f)])
        (trace-files trace-dir)))

(defn- empty-index-state []
  {:schema index-schema
   :record-count 0
   :lanes {}})

(defn- add-record-to-index
  [state record]
  (let [state (update state :record-count (fnil inc 0))]
    (if-let [{:keys [lane action-class target]} (lane-record record)]
      (update-in state [:lanes lane]
                 (fn [row]
                   (-> (or row {:lane lane
                                :action-class action-class
                                :target target
                                :attempts 0
                                :successes 0})
                       (update :attempts inc)
                       (update :successes + (if (target-pass? record) 1 0)))))
      state)))

(defn- index-state-from-records
  [records]
  (reduce add-record-to-index (empty-index-state) records))

(defn- summary-from-index-state
  [state]
  (let [rows (->> (vals (:lanes state))
                  (map #(assoc %
                               :failures (- (:attempts %) (:successes %))
                               :zero-for-n? (zero? (:successes %))))
                  (sort-by (juxt (comp - :attempts) :lane))
                  vec)
        class-rows (->> rows
                        (group-by :action-class)
                        (map (fn [[class xs]]
                               (let [attempts (reduce + (map :attempts xs))
                                     successes (reduce + (map :successes xs))]
                                 {:action-class class
                                  :attempts attempts
                                  :successes successes
                                  :failures (- attempts successes)
                                  :zero-for-n? (zero? successes)})))
                        (sort-by (juxt (comp - :attempts) (comp str :action-class)))
                        vec)]
    {:record-count (:record-count state)
     :attempt-count (reduce + 0 (map :attempts rows))
     :lane-count (count rows)
     :zero-lane-count (count (filter :zero-for-n? rows))
     :rows rows
     :action-classes class-rows}))

(defn- index-path [trace-dir]
  (io/file trace-dir index-filename))

(defn- read-index-state
  [trace-dir]
  (let [f (index-path trace-dir)]
    (when (.isFile f)
      (try
        (edn/read-string (slurp f))
        (catch Exception _ nil)))))

(defn- valid-index-state?
  [state fingerprint]
  (and (= index-schema (:schema state))
       (= fingerprint (:fingerprint state))
       (map? (:lanes state))))

(defn- write-index-state!
  [trace-dir state]
  (let [target (index-path trace-dir)
        tmp (io/file trace-dir (str index-filename "." (random-uuid) ".tmp"))]
    (io/make-parents target)
    (spit tmp (str (pr-str state) "\n"))
    (try
      (Files/move (.toPath tmp)
                  (.toPath target)
                  (into-array StandardCopyOption
                              [StandardCopyOption/ATOMIC_MOVE
                               StandardCopyOption/REPLACE_EXISTING]))
      (catch java.nio.file.AtomicMoveNotSupportedException _
        (Files/move (.toPath tmp)
                    (.toPath target)
                    (into-array StandardCopyOption
                                [StandardCopyOption/REPLACE_EXISTING]))))
    state))

(defn with-index-lock
  "Run F while holding the cross-process trace/index lock for TRACE-DIR."
  [trace-dir f]
  (let [lock-file (io/file trace-dir lock-filename)]
    (io/make-parents lock-file)
    (with-open [raf (RandomAccessFile. lock-file "rw")
                channel (.getChannel raf)
                _lock (.lock channel)]
      (f))))

(defn current-index-state!
  "Return the exact aggregate for TRACE-DIR. A missing or stale sidecar is
   rebuilt from the authoritative traces; it is never accepted approximately."
  [trace-dir]
  (let [fingerprint (corpus-fingerprint trace-dir)
        cached (read-index-state trace-dir)]
    (if (valid-index-state? cached fingerprint)
      cached
      (write-index-state!
       trace-dir
       (assoc (index-state-from-records (trace-records trace-dir))
              :fingerprint fingerprint)))))

(defn append-indexed-trace!
  "Append RECORD to PATH and advance the exact futility index under one
   cross-process lock. If prior trace/index coherence is absent, reconstruct it
   from the authoritative corpus before appending."
  [trace-dir path record]
  (with-index-lock
    trace-dir
    (fn []
      (let [state (current-index-state! trace-dir)]
        (spit path (str (pr-str record) "\n") :append true)
        (write-index-state!
         trace-dir
         (assoc (add-record-to-index state record)
                :fingerprint (corpus-fingerprint trace-dir))))
      path)))

(defn indexed-futility-summary
  "Return the exact all-history summary using a validated persistent index."
  ([] (indexed-futility-summary default-trace-dir))
  ([trace-dir]
   (with-index-lock trace-dir
     #(summary-from-index-state (current-index-state! trace-dir)))))

(defn futility-summary
  "Return per-lane 0-for-N counts from trace RECORDS."
  [records]
  (summary-from-index-state (index-state-from-records records)))

(defn hand-counts
  "Independent census used by the 3a gate."
  [records]
  (let [lanes (keep (fn [r]
                      (when-let [lane (lane-record r)]
                        [(:lane lane) (target-pass? r)]))
                    records)]
    {:attempts (frequencies (map first lanes))
     :successes (frequencies (map first (filter second lanes)))}))

(defn summary-matches-hand-counts?
  [summary hand]
  (every?
   (fn [{:keys [lane attempts successes]}]
     (and (= attempts (get-in hand [:attempts lane] 0))
          (= successes (get-in hand [:successes lane] 0))))
   (:rows summary)))

(defn chosen-ranked-action
  [record]
  (let [rank (get-in record [:decision :rank])]
    (or (first (filter #(= rank (:rank %)) (:ranked-actions record)))
        (first (:ranked-actions record)))))

(defn simulated-outcome
  "Build an R14-compatible outcome for a trace record.

   This is intentionally offline-only. A selected target with no matching pass
   is treated as realized coverage-dG 0.0: the lane did not land coverage. A
   matching pass uses the verdict's actual coverage-dG. The expected leg defaults
   to the act-gate improvement threshold, keeping the signal in coverage-dG units
   while avoiding live wiring."
  ([record] (simulated-outcome record default-expected-coverage-dg))
  ([record expected-coverage-dg]
   (when (lane-record record)
     (let [verdict (target-verdict record)
           pass? (= :pass (:verdict verdict))
           expected (double (or (:coverage-score-delta verdict) expected-coverage-dg))
           realized (double (if pass?
                              (or (:coverage-score-delta verdict) expected-coverage-dg)
                              0.0))]
       {:policy (:lane (lane-record record))
        :expected-score expected
        :realized-score realized
        :tick (:tick record)}))))

(defn fold-gamma
  [outcomes]
  (reduce selection-gain/fold-realized-outcome
          (selection-gain/initial-selection-gain-state)
          outcomes))

(defn gamma-trajectory
  [outcomes]
  (reductions selection-gain/fold-realized-outcome
              (selection-gain/initial-selection-gain-state)
              outcomes))

(defn historical-gamma-report
  [records]
  (let [outcomes (vec (keep-indexed
                       (fn [idx record]
                         (some-> (simulated-outcome record)
                                 (assoc :tick idx)))
                       records))
        final-state (fold-gamma outcomes)]
    {:samples (count outcomes)
     :final final-state
     :first-gamma (:selection-gain (first (gamma-trajectory outcomes)))
     :last-gamma (:selection-gain final-state)}))

(defn synthetic-paying-outcomes
  ([] (synthetic-paying-outcomes 12))
  ([n]
   (mapv (fn [idx]
           {:policy "synthetic-paying-lane"
            :expected-score default-expected-coverage-dg
            :realized-score (* 2.0 default-expected-coverage-dg)
            :tick idx})
         (range n))))

(defn synthetic-paying-gamma-report
  []
  (let [outcomes (synthetic-paying-outcomes)
        final-state (fold-gamma outcomes)]
    {:samples (count outcomes)
     :final final-state
     :last-gamma (:selection-gain final-state)}))

(defn dry-run-bulletins
  ([summary] (dry-run-bulletins summary default-futility-threshold))
  ([summary threshold]
   (->> (:rows summary)
        (filter #(and (:zero-for-n? %)
                      (>= (:attempts %) threshold)))
        (mapv (fn [{:keys [lane action-class target attempts]}]
                {:id (str "lane-futility/" lane)
                 :lane :nag
                 :level :nag
                 :dry-run? true
                 :title (str "Lane futility: " lane " is 0-for-" attempts)
                 :target target
                 :action-class action-class
                 :why (str "Selected lane has " attempts
                           " attempts and zero target-matched pass verdicts.")
                 :operator-dependent? true
                 :futon-important? true
                 :risk-mode? true
                 :acknowledged? true})))))

(defn synthetic-futility-summary
  ([] (synthetic-futility-summary 7))
  ([n]
   {:record-count n
    :attempt-count n
    :lane-count 1
    :zero-lane-count 1
    :rows [{:lane "advance-mission/M-synthetic-stuck"
            :action-class :advance-mission
            :target "M-synthetic-stuck"
            :attempts n
            :successes 0
            :failures n
            :zero-for-n? true}]
    :action-classes [{:action-class :advance-mission
                      :attempts n
                      :successes 0
                      :failures n
                      :zero-for-n? true}]}))
