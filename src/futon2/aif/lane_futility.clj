(ns futon2.aif.lane-futility
  "E-live-loop-2 3-series: per-lane futility observables and dry-run feedback.

   A lane is the selected WM action class plus its selected target. A lane only
   gets success credit when that same target receives a :pass act-gate verdict in
   the trace record. Other passes in the same tick are real evidence, but not
   evidence that the selected lane paid."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.selection-gain :as selection-gain]))

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

(defn futility-summary
  "Return per-lane 0-for-N counts from trace RECORDS."
  [records]
  (let [attempts (keep #(when-let [lane (lane-record %)]
                          (assoc lane :record % :success? (target-pass? %)))
                       records)
        rows (->> attempts
                  (group-by :lane)
                  (map (fn [[lane xs]]
                         (let [first-x (first xs)
                               attempts (count xs)
                               successes (count (filter :success? xs))]
                           {:lane lane
                            :action-class (:action-class first-x)
                            :target (:target first-x)
                            :attempts attempts
                            :successes successes
                            :failures (- attempts successes)
                            :zero-for-n? (zero? successes)})))
                  (sort-by (juxt (comp - :attempts) :lane))
                  vec)
        class-rows (->> attempts
                        (group-by :action-class)
                        (map (fn [[class xs]]
                               (let [attempts (count xs)
                                     successes (count (filter :success? xs))]
                                 {:action-class class
                                  :attempts attempts
                                  :successes successes
                                  :failures (- attempts successes)
                                  :zero-for-n? (zero? successes)})))
                        (sort-by (juxt (comp - :attempts) (comp str :action-class)))
                        vec)]
    {:record-count (count records)
     :attempt-count (count attempts)
     :lane-count (count rows)
     :zero-lane-count (count (filter :zero-for-n? rows))
     :rows rows
     :action-classes class-rows}))

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
