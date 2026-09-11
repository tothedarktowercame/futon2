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
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.substrate :as substrate])
  (:import [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [java.nio.file Files StandardOpenOption]
           [java.time Instant]))

(def default-root "/home/joe/code/futon2/data/wm-repair-obligations")

(def artifact-shapes #{:code-commit :data-deposit :spec-document})

(defprotocol HistoricalSuccessorAuthority
  (historical-successor-record [authority]
    "Return a record only from an authoritative durable-evidence reader."))

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

(def review-failure-discharge-contract
  "Typed discharge contract minted onto every independent-review-failure
  finding. Schema-1 findings carried no discharge contract at all, so the
  downstream repair contract reached construction with :discharge nil and
  the discharge requirements were never machine-visible (attempt-054)."
  {:requires [:distinct-repair-commit :independent-review
              :grounded-repair :distinct-production-shaped-successor]
   :artifact-shape :code-commit})

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
                 :repair/schema-version 2
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
                 :failure-stage :independent-review
                 :failure-kind (case review-verdict
                                 :request-changes :review-request-changes
                                 :reject :review-rejected)
                 :discharge-contract review-failure-discharge-contract
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

(defn- strict-read [text path]
  (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
    (let [v (edn/read {:eof ::empty} r)]
      (when (or (= ::empty v) (not= ::end (edn/read {:eof ::end} r)))
        (throw (ex-info "Historical verification artifact corrupt" {:path (str path)})))
      v)))

(defn- safe-id? [x]
  (and (string? x) (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]{0,127}" x)))

(defn- execution-identity? [x]
  (and (map? x) (= #{:kind :id} (set (keys x)))
       (= :runner-execution (:kind x)) (safe-id? (:id x))))

(defn- historical-directory! [root child create?]
  (let [base (.getCanonicalFile (io/file root))
        directory (io/file base child)
        path (.toPath directory)]
    (when-not (and (.isDirectory base)
                   (not (Files/isSymbolicLink path))
                   (= base (.getCanonicalFile (.getParentFile directory)))
                   (= directory (.getCanonicalFile directory)))
      (throw (ex-info "Historical store directory outside authority" {:child child})))
    (when (and create? (not (.exists directory)))
      (Files/createDirectory path (make-array java.nio.file.attribute.FileAttribute 0))
      (with-open [parent (FileChannel/open (.toPath base)
                                           (make-array StandardOpenOption 0))]
        (.force parent true)))
    (when (and (.exists directory) (not (.isDirectory directory)))
      (throw (ex-info "Historical store directory malformed" {:child child})))
    (when (.isDirectory directory) directory)))

(defn- write-new-durable! [root child record-id value]
  (when-not (safe-id? record-id) (throw (ex-info "Unsafe repair identity" {})))
  (let [base (historical-directory! root child true)
        target (io/file base (str record-id ".edn"))
        bytes (.getBytes (with-out-str (pp/pprint value)) "UTF-8")]
    (when-not (and (= base (.getCanonicalFile (.getParentFile target)))
                   (not (Files/isSymbolicLink (.toPath target))))
      (throw (ex-info "Historical admission output outside authority" {})))
    (with-open [ch (FileChannel/open (.toPath target)
                                     (into-array StandardOpenOption
                                                 [StandardOpenOption/CREATE_NEW
                                                  StandardOpenOption/WRITE]))]
      (let [buf (ByteBuffer/wrap bytes)]
        (while (.hasRemaining buf) (.write ch buf)))
      (.force ch true))
    (with-open [parent (FileChannel/open (.toPath base)
                                         (make-array StandardOpenOption 0))]
      (.force parent true))
    {:path (.getPath target)
     :sha256 (digest/sha256 (String. bytes "UTF-8"))}))

(defn- capture-under! [root path]
  (let [base (.getCanonicalFile (io/file root))
        file (.getCanonicalFile (io/file path))]
    (when-not (and (.isDirectory base) (.isFile file)
                   (not (Files/isSymbolicLink (.toPath (io/file path))))
                   (.startsWith (.toPath file) (.toPath base)))
      (throw (ex-info "Historical verification path outside authority" {:path path})))
    (let [text (slurp file)]
      {:file file :text text :sha256 (digest/sha256 text)
       :value (strict-read text file)})))

(defn- verification-value? [value]
  (and (= #{:schema :verification-id :repair-id :state :repair-resolved?
            :actors :review :qualification :finding :implementation}
          (set (keys value)))
       (= :wm/historical-repair-verification-v1 (:schema value))
       (safe-id? (:verification-id value)) (safe-id? (:repair-id value))
       (= :awaiting-validation (:state value)) (false? (:repair-resolved? value))
       (every? nonblank? ((juxt :author :reviewer) (:actors value)))
       (not= (get-in value [:actors :author]) (get-in value [:actors :reviewer]))
       (= :approve (get-in value [:review :verdict]))
       (true? (get-in value [:review :execution :executed]))
       (nonblank? (get-in value [:review :job-id]))
       (= #{:path :sha256 :check-ids} (set (keys (:qualification value))))
       (= #{:path :sha256} (set (keys (:finding value))))
       (seq (get-in value [:qualification :check-ids]))
       (= (count (get-in value [:qualification :check-ids]))
          (count (distinct (get-in value [:qualification :check-ids]))))
       (every? keyword? (get-in value [:qualification :check-ids]))
       (every? #(and (string? %) (re-matches #"[0-9a-f]{64}" %))
               [(get-in value [:qualification :sha256])
                (get-in value [:finding :sha256])])))

(defn- finding-capture! [root repair-id]
  (when-not (safe-id? repair-id)
    (throw (ex-info "Unsafe repair identity" {:repair/id repair-id})))
  (let [directory (historical-directory! root "findings" false)]
    (when-not directory (throw (ex-info "Historical finding directory missing" {})))
    (capture-under! directory (io/file directory (str repair-id ".edn")))))

(defn- admission-from! [root {:keys [verification-root path sha256]}]
  (let [cap (capture-under! verification-root path)
        value (:value cap)
        _ (when-not (and (= sha256 (:sha256 cap)) (verification-value? value))
            (throw (ex-info "Historical verification lacks admitted evidence" {})))
        finding (finding-capture! root (:repair-id value))]
    (when-not (and (= (:repair-id value) (get-in finding [:value :repair/id]))
                   (= :open (get-in finding [:value :repair/status]))
                   (= :machine-failure (get-in finding [:value :repair/class]))
                   (= (:sha256 finding) (get-in value [:finding :sha256]))
                   (= (.getCanonicalPath ^java.io.File (:file finding))
                      (.getCanonicalPath (io/file (get-in value [:finding :path])))))
      (throw (ex-info "Historical verification finding join refused"
                      {:repair/id (:repair-id value)})))
    {:schema :wm/historical-repair-admission-v1
     :repair/id (:repair-id value) :repair/schema-version 1
     :repair/status :awaiting-validation
     :failed-attempt (get-in finding [:value :attempt-id])
     :verification-id (:verification-id value)
     :verification-artifact {:path (.getPath ^java.io.File (:file cap)) :sha256 sha256}
     :finding-artifact {:path (.getPath ^java.io.File (:file finding))
                        :sha256 (:sha256 finding)}
     :actors (:actors value) :review (:review value) :implementation (:implementation value)}))

(defn historical-verification-candidate
  "Read and validate a candidate without changing stop-line state."
  ([evidence] (historical-verification-candidate default-root evidence))
  ([root evidence] (admission-from! root evidence)))

(defn commit-historical-verification!
  "Execute a selected historical verification action. Canonical finding and
  verification bytes are the only identity sources."
  ([execution-attempt evidence]
   (commit-historical-verification! default-root execution-attempt evidence))
  ([root execution-attempt evidence]
   (when-not (execution-identity? execution-attempt)
     (throw (ex-info "Historical verification execution identity invalid" {})))
   (let [candidate (admission-from! root evidence)
         source (:verification-artifact candidate)
         cap (capture-under! (:verification-root evidence) (:path evidence))
         _ (when-not (= (:sha256 source) (:sha256 cap))
             (throw (ex-info "Historical verification changed before copying" {})))
         copy (write-new-durable! root "verification-evidence"
                                  (:verification-id candidate) (:value cap))
         record (assoc candidate :verification-attempt execution-attempt
                                 :verification-source source
                                 :verification-artifact copy)]
     (write-new-durable! root "verifications" (:repair/id record) record)
     record)))

(defn record-historical-verification!
  "Compatibility wrapper. The supplied obligation is deliberately not an
  authority; canonical store bytes determine the transition."
  ([obligation evidence] (record-historical-verification! default-root obligation evidence))
  ([root _obligation evidence]
   (commit-historical-verification!
    root {:kind :runner-execution
          :id (:verification-id (historical-verification-candidate root evidence))}
    evidence)))

(declare verified-admissions)

(defn commit-historical-resolution!
  "Persist a terminal-reader-authorized, distinct production successor. Maps
  and caller witness flags are deliberately not accepted."
  ([repair-id authority] (commit-historical-resolution! default-root repair-id authority))
  ([root repair-id authority]
   (when-not (satisfies? HistoricalSuccessorAuthority authority)
     (throw (ex-info "Historical successor lacks reader authority" {})))
   (let [record (historical-successor-record authority)
         admissions (verified-admissions root)
         admission (get admissions repair-id)
         finding (:value (finding-capture! root repair-id))]
     (when-not (and (= :wm/historical-repair-resolution-v1 (:schema record))
                    (= repair-id (:repair/id record) (:repair/id admission))
                    (= (:verification-id admission) (:verification-id record))
                    (= (:verification-attempt admission) (:verification-attempt record))
                    (= :resolved (:repair/status record))
                    (= :succeeded (:task-result record))
                    (= :safe (:infrastructure record))
                    (execution-identity? (:validation-attempt record))
                    (execution-identity? (:verification-attempt record))
                    (not= (:validation-attempt record) (:verification-attempt record))
                    (not= (get-in record [:validation-attempt :id]) (:attempt-id finding))
                    (safe-id? (:click-id record)) (safe-id? (:run-id record))
                    (every? #(and (string? %) (re-matches #"[0-9a-f]{64}" %))
                            ((juxt :projection-digest :run-record-digest) record)))
       (throw (ex-info "Historical successor evidence refused" {:repair/id repair-id})))
     (write-new-durable! root "resolutions" repair-id record)
     record)))

(defn- verified-admissions [root]
  (let [directory (historical-directory! root "verifications" false)
        evidence-directory (historical-directory! root "verification-evidence" false)]
    (into {}
        (map (fn [file]
               (let [cap (capture-under! directory file)
                     stored (:value cap)
                     artifact (:verification-artifact stored)
                     _ (when-not (and evidence-directory
                                      (execution-identity? (:verification-attempt stored))
                                      (= #{:schema :repair/id :repair/schema-version
                                           :repair/status :failed-attempt :verification-id
                                           :verification-attempt
                                           :verification-artifact :verification-source
                                           :finding-artifact :actors
                                           :review :implementation}
                                         (set (keys stored)))
                                      (= #{:path :sha256} (set (keys artifact)))
                                      (nonblank? (:path artifact))
                                      (= (.getCanonicalPath
                                          (io/file evidence-directory (str (:verification-id stored) ".edn")))
                                         (.getCanonicalPath (io/file (:path artifact)))))
                         (throw (ex-info "Historical admission record corrupt"
                                         {:path (.getPath ^java.io.File file)})))
                     candidate (admission-from!
                               root {:verification-root (.getParent (io/file (:path artifact)))
                                     :path (:path artifact)
                                     :sha256 (:sha256 artifact)})
                     expected (assoc candidate
                                     :verification-attempt (:verification-attempt stored)
                                     :verification-artifact artifact
                                               :verification-source (:verification-source stored))]
                 (when-not (= stored expected)
                   (throw (ex-info "Historical admission record corrupt"
                                   {:path (.getPath ^java.io.File file)})))
                 [(:repair/id stored) stored])))
        (->> (or (when directory (.listFiles ^java.io.File directory)) [])
             (filter #(str/ends-with? (.getName %) ".edn"))))))

(defn obligation-history
  "All immutable findings for an attempt, enriched with any implementation and
  resolution records. Unlike `open-obligations`, this is an audit view."
  ([attempt-id] (obligation-history default-root attempt-id))
  ([root attempt-id]
   (let [implementations (indexed-records root "implementations")
         verifications (verified-admissions root)
         resolutions (indexed-records root "resolutions")]
     (->> (records (io/file root "findings"))
          (filter #(= attempt-id (:attempt-id %)))
          (mapv (fn [finding]
                  (cond-> finding
                    (get implementations (:repair/id finding))
                    (assoc :repair/implementation
                           (get implementations (:repair/id finding)))
                    (get verifications (:repair/id finding))
                    (assoc :repair/verification (get verifications (:repair/id finding)))
                    (get resolutions (:repair/id finding))
                    (assoc :repair/resolution
                           (get resolutions (:repair/id finding))))))))))

(defn open-obligations
  ([] (open-obligations default-root))
  ([root]
   (let [resolved (set (map :repair/id (records (io/file root "resolutions"))))
         implementations (indexed-records root "implementations")
         verifications (verified-admissions root)]
     (->> (records (io/file root "findings"))
          (remove #(contains? resolved (:repair/id %)))
          (mapv (fn [finding]
                  (let [finding (update finding :repair/class
                                        #(if (= :system-actuation-failure %)
                                           :machine-failure %))]
                    (if-let [verification (get verifications (:repair/id finding))]
                      (assoc finding :repair/status :awaiting-validation
                             :repair/verification verification)
                      (if-let [implementation (get implementations
                                                    (:repair/id finding))]
                      (assoc finding
                             :repair/status :awaiting-validation
                             :repair/implementation implementation)
                      finding)))))
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
