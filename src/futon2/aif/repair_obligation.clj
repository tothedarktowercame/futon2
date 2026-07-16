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

(defn obligation-id
  ([attempt-id] (obligation-id attempt-id nil))
  ([attempt-id discriminator]
   (str "repair-" attempt-id
        (when (some? discriminator)
          (str "-" (name discriminator))))))

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
  "Record a zero-achievement stop-line without mis-typing every cause as a
  code defect. `:repair/class` distinguishes machine failures, environmental
  holds, and recoverable incomplete work. Selection fields are optional
  because readiness and substrate failures can precede policy selection."
  ([finding] (record-system-failure! default-root finding))
  ([root {:keys [attempt-id repair-id repair-class failure-stage outcome error]
          :as finding}]
   (when-not (and (string? attempt-id)
                  (#{:machine-failure :environmental-hold
                     :incomplete-recoverable} repair-class)
                  (keyword? failure-stage) (keyword? outcome)
                  (not (str/blank? (str error))))
     (throw (ex-info "System stop-the-line finding lacks required provenance"
                     {:finding finding})))
   (let [id (or repair-id
                (obligation-id attempt-id (:failure-kind finding)))
         record {:repair/id id
                 :repair/schema-version 3
                 :repair/status :open
                 :repair/class repair-class
                 :attempt-id attempt-id
                 :target (:target finding)
                 :selected-entry (:selected-entry finding)
                 :failure-stage failure-stage
                 :failure-outcome outcome
                 :failure-kind (:failure-kind finding)
                 :failure-error error
                 :failure-data (:failure-data finding)
                 :backtrace (:backtrace finding)
                 :discharge-contract (:discharge-contract finding)
                 :opened-at (str (Instant/now))}]
     (write-new! (io/file root "findings" (str id ".edn")) record)
     record)))

(defn- indexed-records [root child]
  (into {} (map (juxt :repair/id identity)
                (records (io/file root child)))))

(defn obligation-history
  "All immutable findings for an attempt, enriched with any implementation and
  resolution records. Unlike `open-obligations`, this is an audit view."
  ([attempt-id] (obligation-history default-root attempt-id))
  ([root attempt-id]
   (let [implementations (indexed-records root "implementations")
         resolutions (indexed-records root "resolutions")]
     (->> (records (io/file root "findings"))
          (filter #(= attempt-id (:attempt-id %)))
          (mapv (fn [finding]
                  (cond-> finding
                    (get implementations (:repair/id finding))
                    (assoc :repair/implementation
                           (get implementations (:repair/id finding)))
                    (get resolutions (:repair/id finding))
                    (assoc :repair/resolution
                           (get resolutions (:repair/id finding))))))))))

(defn open-obligations
  ([] (open-obligations default-root))
  ([root]
   (let [resolved (set (map :repair/id (records (io/file root "resolutions"))))
         implementations (indexed-records root "implementations")]
     (->> (records (io/file root "findings"))
          (remove #(contains? resolved (:repair/id %)))
          (mapv (fn [finding]
                  (if-let [implementation (get implementations (:repair/id finding))]
                    (assoc finding
                           :repair/status :awaiting-validation
                           :repair/implementation implementation)
                    finding)))
          (sort-by :opened-at)
          vec))))

(defn record-implementation!
  "Record independently reviewed, grounded implementation of a machine repair.
  This does not clear the line: a distinct production-shaped successor must
  still validate the repaired machine."
  ([obligation implementation]
   (record-implementation! default-root obligation implementation))
  ([root obligation {:keys [attempt-id commit reviewer review-job witness]
                     :as implementation}]
   (when-not (and (:repair/id obligation)
                  (#{:machine-failure :independent-review-failure}
                   (:repair/class obligation))
                  attempt-id commit reviewer review-job
                  (not= attempt-id (:attempt-id obligation))
                  (:resolved? witness) (:dial-moved? witness))
     (throw (ex-info "Machine repair implementation lacks grounded review evidence"
                     {:obligation obligation :implementation implementation})))
   (let [record {:repair/id (:repair/id obligation)
                 :repair/schema-version 1
                 :repair/status :awaiting-validation
                 :failed-attempt (:attempt-id obligation)
                 :implementation-attempt attempt-id
                 :replacement-commit commit
                 :reviewer reviewer
                 :review-job review-job
                 :witness witness
                 :implemented-at (str (Instant/now))}]
     (write-new! (io/file root "implementations"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record)))

(defn resolve!
  ([obligation resolution] (resolve! default-root obligation resolution))
  ([root obligation {:keys [attempt-id commit reviewer review-job witness]
                     :as resolution}]
   (let [implementation (:repair/implementation obligation)
         recoverable? (= :incomplete-recoverable (:repair/class obligation))
         environmental? (= :environmental-hold (:repair/class obligation))
         machine? (#{:machine-failure :independent-review-failure}
                   (:repair/class obligation))
         valid? (and (:repair/id obligation) attempt-id commit reviewer review-job
                     (not= attempt-id (:attempt-id obligation))
                     (:resolved? witness) (:dial-moved? witness)
                     (cond
                       recoverable? true
                       environmental? (true? (get-in resolution
                                                    [:validation :production-shaped?]))
                       machine? (and implementation
                                     (not= attempt-id
                                           (:implementation-attempt implementation))
                                     (true? (get-in resolution
                                                    [:validation :production-shaped?])))
                       :else false))]
   (when-not valid?
     (throw (ex-info "Stop-the-line resolution requires approved grounded evidence"
                     {:obligation obligation :resolution resolution})))
   (let [record {:repair/id (:repair/id obligation)
                 :repair/schema-version 1
                 :repair/status :resolved
                 :failed-attempt (:attempt-id obligation)
                 :validation-attempt attempt-id
                 :failed-commit (:failed-commit obligation)
                 :replacement-commit (or (:replacement-commit implementation)
                                         commit)
                 :reviewer reviewer
                 :review-job review-job
                 :witness witness
                 :validation (:validation resolution)
                 :resolved-at (str (Instant/now))}]
     (write-new! (io/file root "resolutions"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record))))
