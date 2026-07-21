(ns futon2.aif.repair-obligation
  "Durable stop-the-line memory for full-loop failures.

  Independent-review failures and system failures are distinct finding
  classes. Findings and resolutions are separate immutable records. An
  obligation is open until an approved, grounded successor run records a
  resolution that names the original failure and its replacement artifact."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.substrate :as substrate])
  (:import [java.nio.file Files StandardOpenOption]
           [java.time Instant]))

(def default-root "/home/joe/code/futon2/data/wm-repair-obligations")

(def artifact-shapes #{:code-commit :data-deposit :spec-document})

(def ^:dynamic *store-count-reader*
  "Read the current number of RECORD-TYPE records at STORE-URL. Bind in tests
  to exercise validation without writing to, or depending on, a live store."
  (fn [store-url record-type]
    (count (substrate/hyperedges-by-type
            record-type {:substrate-url store-url :limit 100000}))))

(defn- artifact-shape [obligation]
  (get-in obligation [:discharge-contract :artifact-shape] :code-commit))

(defn- nonblank? [x]
  (and (string? x) (not (str/blank? x))))

(defn- data-deposit-evidence?
  [{:keys [store-url record-type count-before count-after deposit-run-id]}]
  (and (nonblank? store-url)
       (or (keyword? record-type) (nonblank? record-type))
       (nat-int? count-before)
       (nat-int? count-after)
       (not= count-before count-after)
       (nonblank? deposit-run-id)
       (try
         (= count-after (*store-count-reader* store-url record-type))
         (catch Throwable _ false))))

(defn- git-command [repo & args]
  (apply shell/sh "git" "-C" repo args))

(defn- spec-document-evidence?
  [obligation {:keys [path git-sha]}]
  (and (nonblank? path)
       (nonblank? git-sha)
       (let [declared (io/file path)
             base (io/file (or (:machine-repo obligation) "."))
             file (if (.isAbsolute declared)
                    declared
                    (io/file base path))]
         (and (.isFile file)
              (try
                (let [repo-result (git-command (.getPath (.getParentFile file))
                                               "rev-parse" "--show-toplevel")
                      repo (str/trim (:out repo-result))
                      repo-path (.toPath (.getCanonicalFile (io/file repo)))
                      file-path (.toPath (.getCanonicalFile file))]
                  (and (zero? (:exit repo-result))
                       (.startsWith file-path repo-path)
                       (let [relative (str (.relativize repo-path file-path))
                             commit (git-command repo "rev-parse" "--verify"
                                                 (str git-sha "^{commit}"))
                             ancestor (git-command repo "merge-base" "--is-ancestor"
                                                   git-sha "HEAD")
                             touched (git-command repo "diff-tree" "--root"
                                                  "--no-commit-id" "--name-only"
                                                  "-r" git-sha "--" relative)]
                         (and (zero? (:exit commit))
                              (zero? (:exit ancestor))
                              (zero? (:exit touched))
                              (some #{relative}
                                    (str/split-lines (:out touched)))))))
                (catch Throwable _ false))))))

(defn- artifact-evidence?
  [obligation evidence implementation?]
  (case (artifact-shape obligation)
    :code-commit
    (let [commit (:commit evidence)]
      (and commit
           (or (not implementation?)
               (nil? (:failed-commit obligation))
               (not= commit (:failed-commit obligation)))))

    :data-deposit (data-deposit-evidence? evidence)
    :spec-document (spec-document-evidence? obligation evidence)
    false))

(defn- artifact-record
  [shape evidence]
  (case shape
    :data-deposit (select-keys evidence [:store-url :record-type :count-before
                                         :count-after :deposit-run-id])
    :spec-document (select-keys evidence [:path :git-sha])))

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
                 :machine-repo (:machine-repo finding)
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
                  (let [finding (update finding :repair/class
                                        #(if (= :system-actuation-failure %)
                                           :machine-failure %))]
                    (if-let [implementation (get implementations
                                                    (:repair/id finding))]
                      (assoc finding
                             :repair/status :awaiting-validation
                             :repair/implementation implementation)
                      finding))))
          (sort-by :opened-at)
          vec))))

(defn record-implementation!
  "Record independently reviewed, grounded implementation of a machine repair.
  Evidence is validated according to the discharge contract's artifact shape;
  an absent shape retains the historical code-commit contract. This does not
  clear the line: a distinct production-shaped successor must still validate
  the repaired machine."
  ([obligation implementation]
   (record-implementation! default-root obligation implementation))
  ([root obligation {:keys [attempt-id commit reviewer review-job witness]
                     :as implementation}]
   (let [shape (artifact-shape obligation)]
     (when-not (and (contains? artifact-shapes shape)
                  (:repair/id obligation)
                  (#{:machine-failure :independent-review-failure}
                   (:repair/class obligation))
                  attempt-id reviewer review-job
                  (not= attempt-id (:attempt-id obligation))
                  (artifact-evidence? obligation implementation true)
                  (:resolved? witness) (:dial-moved? witness))
       (throw (ex-info "Machine repair implementation lacks grounded review evidence"
                       {:obligation obligation :implementation implementation})))
     (let [record (cond->
                   {:repair/id (:repair/id obligation)
                    :repair/schema-version 1
                    :repair/status :awaiting-validation
                    :failed-attempt (:attempt-id obligation)
                    :implementation-attempt attempt-id
                    :reviewer reviewer
                    :review-job review-job
                    :witness witness
                    :implemented-at (str (Instant/now))}
                    (= :code-commit shape)
                    (assoc :replacement-commit commit)
                    (not= :code-commit shape)
                    (assoc :artifact-shape shape
                           :replacement-artifact
                           (artifact-record shape implementation)))]
       (write-new! (io/file root "implementations"
                            (str (:repair/id obligation) ".edn"))
                   record)
       record))))

(defn supersede!
  "Close an obligation whose promised recovery has become impossible, linking
  it to the typed successor finding that now owns the stop line. This is not a
  successful repair resolution and requires no fabricated grounding witness."
  ([obligation successor reason]
   (supersede! default-root obligation successor reason))
  ([root obligation successor reason]
   (when-not (and (= :incomplete-recoverable (:repair/class obligation))
                  (= :open (:repair/status obligation))
                  (:repair/id successor)
                  (not= (:repair/id obligation) (:repair/id successor))
                  (keyword? reason))
     (throw (ex-info "Invalid recoverable-obligation transition"
                     {:obligation obligation :successor successor
                      :reason reason})))
   (let [record {:repair/id (:repair/id obligation)
                 :repair/schema-version 2
                 :repair/status :superseded
                 :failed-attempt (:attempt-id obligation)
                 :successor-repair-id (:repair/id successor)
                 :supersession-reason reason
                 :resolved-at (str (Instant/now))}]
     (write-new! (io/file root "resolutions"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record)))

(defn resolve!
  ([obligation resolution] (resolve! default-root obligation resolution))
  ([root obligation {:keys [attempt-id commit reviewer review-job witness]
                     :as resolution}]
   (let [implementation (:repair/implementation obligation)
         shape (artifact-shape obligation)
         recoverable? (= :incomplete-recoverable (:repair/class obligation))
         environmental? (= :environmental-hold (:repair/class obligation))
         machine? (#{:machine-failure :independent-review-failure}
                   (:repair/class obligation))
         valid? (and (contains? artifact-shapes shape)
                     (:repair/id obligation) attempt-id reviewer review-job
                     (artifact-evidence? obligation resolution false)
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
   (let [record (cond->
                 {:repair/id (:repair/id obligation)
                  :repair/schema-version 1
                  :repair/status :resolved
                  :failed-attempt (:attempt-id obligation)
                  :validation-attempt attempt-id
                  :failed-commit (:failed-commit obligation)
                  :reviewer reviewer
                  :review-job review-job
                  :witness witness
                  :validation (:validation resolution)
                  :resolved-at (str (Instant/now))}
                  (= :code-commit shape)
                  (assoc :replacement-commit
                         (or (:replacement-commit implementation) commit))
                  (not= :code-commit shape)
                  (assoc :artifact-shape shape
                         :replacement-artifact
                         (or (:replacement-artifact implementation)
                             (artifact-record shape resolution))
                         :validation-artifact
                         (artifact-record shape resolution)))]
     (write-new! (io/file root "resolutions"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record))))
