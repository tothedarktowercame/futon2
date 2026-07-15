(ns futon2.aif.repair-obligation
  "Durable stop-the-line memory for full-loop failures.

  Independent-review failures and system failures are distinct finding
  classes. Findings and resolutions are separate immutable records. An
  obligation is open until an approved, grounded successor run records a
  resolution that names the original failure and its replacement commit."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]))

(def default-root "/home/joe/code/futon2/data/wm-repair-obligations")

(defn- write-new! [path value]
  (let [file (io/file path)]
    (io/make-parents file)
    (Files/write (.toPath file)
                 (.getBytes (with-out-str (pp/pprint value)) "UTF-8")
                 (into-array StandardOpenOption
                             [StandardOpenOption/CREATE_NEW
                              StandardOpenOption/WRITE]))
    (.getPath file)))

(defn- records [dir]
  (->> (or (.listFiles (io/file dir)) [])
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".edn"))
       (sort-by #(.getName %))
       (mapv #(edn/read-string (slurp %)))))

(defn obligation-id [attempt-id]
  (str "repair-" attempt-id))

(defn record-review-failure!
  ([finding] (record-review-failure! default-root finding))
  ([root {:keys [attempt-id target commit selected-entry reviewer review-job
                 review-verdict review-text]
          :as finding}]
   (when-not (and (string? attempt-id) target commit selected-entry reviewer
                  review-job (#{:request-changes :reject} review-verdict)
                  (not (str/blank? (str review-text))))
     (throw (ex-info "Stop-the-line finding lacks required provenance"
                     {:finding finding})))
   (let [id (obligation-id attempt-id)
         record {:repair/id id
                 :repair/schema-version 1
                 :repair/status :open
                 :repair/class :independent-review-failure
                 :attempt-id attempt-id
                 :target target
                 :failed-commit commit
                 :selected-entry selected-entry
                 :reviewer reviewer
                 :review-job review-job
                 :review-verdict review-verdict
                 :review-text review-text
                 :opened-at (str (Instant/now))}]
     (write-new! (io/file root "findings" (str id ".edn")) record)
     record)))

(defn record-system-failure!
  "Record a post-selection machine failure without mis-typing it as reviewer
  feedback. The selected entry is retained so the next opportunity repairs
  the failed actuation path before returning to ordinary policy selection."
  ([finding] (record-system-failure! default-root finding))
  ([root {:keys [attempt-id target selected-entry failure-stage outcome error]
          :as finding}]
   (when-not (and (string? attempt-id) target (map? selected-entry)
                  (keyword? failure-stage) (keyword? outcome)
                  (not (str/blank? (str error))))
     (throw (ex-info "System stop-the-line finding lacks required provenance"
                     {:finding finding})))
   (let [id (obligation-id attempt-id)
         record {:repair/id id
                 :repair/schema-version 2
                 :repair/status :open
                 :repair/class :system-actuation-failure
                 :attempt-id attempt-id
                 :target target
                 :selected-entry selected-entry
                 :failure-stage failure-stage
                 :failure-outcome outcome
                 :failure-error error
                 :failure-data (:failure-data finding)
                 :opened-at (str (Instant/now))}]
     (write-new! (io/file root "findings" (str id ".edn")) record)
     record)))

(defn open-obligations
  ([] (open-obligations default-root))
  ([root]
   (let [resolved (set (map :repair/id (records (io/file root "resolutions"))))]
     (->> (records (io/file root "findings"))
          (remove #(contains? resolved (:repair/id %)))
          (sort-by :opened-at)
          vec))))

(defn resolve!
  ([obligation resolution] (resolve! default-root obligation resolution))
  ([root obligation {:keys [attempt-id commit reviewer review-job witness]
                     :as resolution}]
   (when-not (and (:repair/id obligation) attempt-id commit reviewer review-job
                  (not= attempt-id (:attempt-id obligation))
                  (or (nil? (:failed-commit obligation))
                      (not= commit (:failed-commit obligation)))
                  (:resolved? witness) (:dial-moved? witness))
     (throw (ex-info "Stop-the-line resolution requires approved grounded evidence"
                     {:obligation obligation :resolution resolution})))
   (let [record {:repair/id (:repair/id obligation)
                 :repair/schema-version 1
                 :repair/status :resolved
                 :failed-attempt (:attempt-id obligation)
                 :validation-attempt attempt-id
                 :failed-commit (:failed-commit obligation)
                 :replacement-commit commit
                 :reviewer reviewer
                 :review-job review-job
                 :witness witness
                 :resolved-at (str (Instant/now))}]
     (write-new! (io/file root "resolutions"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record)))
