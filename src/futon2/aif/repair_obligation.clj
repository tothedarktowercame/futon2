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
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.substrate :as substrate])
  (:import [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [java.nio.file Files StandardOpenOption LinkOption OpenOption]
           [java.time Instant]))

(def default-root "/home/joe/code/futon2/data/wm-repair-obligations")

(def artifact-shapes #{:code-commit :data-deposit :spec-document})
(defonce ^:private finding-publication-monitors (atom {}))

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

(defn- grounded-review-evidence?
  "Require the independently computed review gate and observed author artifact
  binding to agree with the repair implementation.  Schema-3 findings were
  created after these records existed and must not be discharged by the old
  self-asserted reviewer/job/witness labels alone."
  [obligation {:keys [commit reviewer review-job review-evidence
                      artifact-binding]}]
  (let [machine-repo (:machine-repo obligation)]
    (and (map? review-evidence)
         (true? (:valid? review-evidence))
         (= review-job (:job-id review-evidence))
         (= reviewer (:reviewer review-evidence))
         (= "done" (:state review-evidence))
         (= :approve (:verdict review-evidence))
         (true? (get-in review-evidence [:execution :executed]))
         (pos-int? (get-in review-evidence [:execution :tool-events]))
         (map? artifact-binding)
         (= commit (:commit artifact-binding))
         (or (nil? machine-repo) (= machine-repo (:repo artifact-binding)))
         (true? (:fresh-author? artifact-binding))
         (true? (:descendant? artifact-binding))
         (true? (:in-author-window? artifact-binding))
         (true? (:corroborates? artifact-binding))
         (false? (:disagreement? artifact-binding)))))

(defn- with-contended-store-lock
  "Bounded retry around the deliberately NON-BLOCKING store lock: the lock
  layer refuses same-instant acquisition as :interoceptive/lock-contention
  (a contract other stores pin), while this store's writes are replay-safe
  and must serialize under concurrent identical findings (the race-safe
  contract of this namespace's tests). Exhausted contention still propagates
  the typed refusal — bounded wait, never an escape hatch."
  [root f]
  (loop [attempt 1]
    (let [outcome (try
                    {:ok (store-lock/with-store-lock-for root f)}
                    (catch clojure.lang.ExceptionInfo e
                      (if (and (= :interoceptive/lock-contention
                                  (:refusal (ex-data e)))
                               (< attempt 40))
                        {:retry true}
                        (throw e))))]
      (if (contains? outcome :ok)
        (:ok outcome)
        (do (Thread/sleep 25)
            (recur (inc attempt)))))))

(def ^:private finding-node-budget
  "Collection nodes one finding entry may carry into a durable finding file.
  Measured on the real 19.2 MB finding of 2026-09-19
  (repair-ea1-4e68d87b...--attempt-001-build-failed): :backtrace/:checkpoints
  alone was 10.1 MB (>4096 nodes) and :selected-entry/:action 1.7 MB, while
  every subtree a consumer reads back (:failure-data 12 nodes, :backtrace
  {:job-id,:code-state} 5, :phase-events 34) sits far below this budget.
  See `durable-finding` for why the disk copy is bounded at all."
  1024)

(def ^:private finding-string-budget
  "Characters one scalar string entry may carry into a durable finding file.
  `within-finding-node-budget?` counts collection nodes, so a megabyte string
  would pass it; strings are capped separately."
  8192)

(defn- within-finding-node-budget?
  "True when V holds at most `finding-node-budget` collection nodes. Same
  early-exit walk as tripwire's `within-node-budget?`: the cost is the budget,
  not the size of V, because V is routinely a whole store snapshot."
  [v]
  (loop [stack (list v) n 0]
    (cond
      (> n finding-node-budget) false
      (empty? stack) true
      :else (let [x (first stack) more (rest stack)]
              (cond
                (map? x) (recur (into more cat x) (inc n))
                (coll? x) (recur (into more x) (inc n))
                :else (recur more n))))))

(defn- describe-elided-finding-value
  "Stand in for a finding entry too large to store, saying what was there.
  Same shape as tripwire's `describe-elided`, so both durable copies read the
  same way."
  [v]
  (cond-> {:elided/reason :exceeds-durable-finding-budget
           :elided/type (cond (map? v) :map
                              (vector? v) :vector
                              (set? v) :set
                              (sequential? v) :seq
                              (string? v) :string
                              :else :value)}
    (coll? v) (assoc :elided/count (bounded-count 100000 v))
    (map? v) (assoc :elided/keys (vec (take 64 (sort-by str (keys v)))))
    (string? v) (assoc :elided/count (count v))))

(def ^:private finding-entry-cap
  "Entries a wide-but-shallow collection may carry into a durable finding
  file. The node walk counts collections, not scalars, so a map of 1500 scalar
  entries is a single node; the entry cap closes that hole."
  512)

(defn- finding-entry-oversized?
  [v]
  (or (and (string? v) (> (count v) finding-string-budget))
      (and (coll? v)
           (> (bounded-count (inc finding-entry-cap) v) finding-entry-cap))
      (not (within-finding-node-budget? v))))

(defn- bounded-finding-entry
  "Bound one entry at depth 2 or deeper: over ANY budget (string length,
  entry width, or node count) the whole entry is elided; under all of them it
  keeps its structure with each of its entries bounded the same way."
  [v]
  (cond
    (finding-entry-oversized? v) (describe-elided-finding-value v)
    (map? v) (reduce-kv (fn [m k x] (assoc m k (bounded-finding-entry x))) {} v)
    (coll? v) (into (empty v) (map bounded-finding-entry) v)
    :else v))

(defn- durable-finding
  "Project a finding RECORD down to what a durable finding file should carry.

  The stop-line path embeds whole backtraces and phase histories: the finding
  opened 2026-09-19T01:09 was 19,220,142 bytes for one failed attempt, and
  those bytes were serialised while holding the publication lock. Nothing
  reads the bulk back — :backtrace/:checkpoints (10.1 MB) and
  :selected-entry/:action (1.7 MB) have no consumer — while the small keys
  that ARE read back (:backtrace {:job-id,:code-state {:repo}},
  :failure-data {:trip/id,:job-id}) sit far under the budget and survive
  verbatim next to visible elisions.

  A TOP-LEVEL map field recurses into its entries whenever its width is
  within finding-entry-cap, so a deep-but-narrow field like :backtrace keeps
  its small keys while its bulk entries are elided; every entry at depth 2 or
  deeper must itself fit finding-node-budget nodes and finding-entry-cap
  entries. Total disk size is bounded by (top fields x finding-entry-cap
  entries x finding-node-budget nodes).

  The in-memory record is left whole (`record-system-failure!` returns the
  unprojected record); only the disk copy is bounded, matching tripwire's
  `durable-observation` discipline."
  [record]
  (reduce-kv
   (fn [m k v]
     (assoc m k
            (cond
              (and (map? v)
                   (> (bounded-count (inc finding-entry-cap) v) finding-entry-cap))
              (describe-elided-finding-value v)

              (map? v)
              (reduce-kv (fn [m2 k2 x] (assoc m2 k2 (bounded-finding-entry x))) {} v)

              :else (bounded-finding-entry v))))
   {} record))

(defn- write-new! [path value]
  (with-contended-store-lock (.getParent (io/file path))
   (fn []
    (let [file (io/file path)]
    (io/make-parents file)
    (Files/write (.toPath file)
                 ;; pr-str, not pprint: see write-new-or-identical! below.
                 (.getBytes (pr-str value) "UTF-8")
                 (into-array StandardOpenOption
                             [StandardOpenOption/CREATE_NEW
                              StandardOpenOption/WRITE]))
      (.getPath file)))))

(defn- finding-directory! [root]
  (let [supplied (io/file root)
        normalized (.toFile (.normalize (.toPath (.getAbsoluteFile supplied))))
        canonical (.getCanonicalFile supplied)]
    (when-not (and (.isDirectory supplied)
                   (= normalized canonical)
                   (not (Files/isSymbolicLink (.toPath supplied))))
      (throw (ex-info "Repair finding root outside authority"
                      {:reason :repair-finding-root-refused})))
    (let [directory (io/file canonical "findings")
          path (.toPath directory)]
      (when (and (.exists directory)
                 (or (Files/isSymbolicLink path) (not (.isDirectory directory))))
        (throw (ex-info "Repair finding directory outside authority"
                        {:reason :repair-finding-root-refused})))
      (when-not (.exists directory)
        (try
          (Files/createDirectory path
                                 (make-array java.nio.file.attribute.FileAttribute 0))
          (with-open [parent (FileChannel/open (.toPath canonical)
                                               (make-array OpenOption 0))]
            (.force parent true))
          (catch java.nio.file.FileAlreadyExistsException _ nil)))
      (when-not (and (.isDirectory directory)
                     (not (Files/isSymbolicLink path))
                     (= canonical (.getCanonicalFile (.getParentFile directory)))
                     (= directory (.getCanonicalFile directory)))
        (throw (ex-info "Repair finding directory outside authority"
                        {:reason :repair-finding-root-refused})))
      directory)))

(defn- write-new-or-identical!
  "Publish immutable EDN, acknowledging replay only when the exact bytes are
  already present. Callers that want replay semantics must supply every
  unstable field (notably :opened-at); semantic-map equality is deliberately
  insufficient."
  [root record-id value]
  (with-contended-store-lock root
   (fn []
    (let [directory (finding-directory! root)
        file (io/file directory (str record-id ".edn"))
        file-path (.toPath file)
        lock-path (.toPath (io/file directory ".publication.lock"))
        ;; pr-str, not pprint. Measured on a real 19.4 MB finding
        ;; (repair-ea1-dd81768f...--attempt-001-build-failed) on 2026-09-19:
        ;; pprint 47,306 ms / 19,412,725 bytes; pr-str 212 ms / 11,988,996
        ;; bytes. 223x faster and 38% smaller, and those 47 seconds were spent
        ;; holding with-contended-store-lock, so every other store user waited
        ;; them out. Nothing reads these files for their layout.
        ;; The disk copy is BOUNDED, not whole: durable-finding elides entries
        ;; over finding-node-budget with a visible :elided/ marker. The same
        ;; lock-holding argument applies to the bulk: 19.2 MB for one finding.
        bytes (.getBytes (pr-str (durable-finding value)) "UTF-8")
        ;; Replay below is decided by exact bytes, deliberately. Findings
        ;; written before this change are on disk UNBOUNDED (pr-str of the
        ;; whole record), and before the pr-str switch in pprint form, so a
        ;; genuine replay of one of them must still be acknowledged rather
        ;; than raised as a conflict. Computed only on the already-exists
        ;; path, so neither slow serialisation is ever on the ordinary write.
        raw-bytes (delay (.getBytes (pr-str value) "UTF-8"))
        legacy-bytes (delay (.getBytes (with-out-str (pp/pprint value)) "UTF-8"))
        monitor-key (.getPath directory)
        monitor (get (swap! finding-publication-monitors
                            #(if (contains? % monitor-key)
                               % (assoc % monitor-key (Object.))))
                     monitor-key)]
    (when-not (= directory (.getCanonicalFile (.getParentFile file)))
      (throw (ex-info "Repair finding output outside authority"
                      {:reason :repair-finding-root-refused})))
    (locking monitor
      (with-open [lock-channel
                  (FileChannel/open lock-path
                                    (into-array OpenOption
                                                [StandardOpenOption/CREATE
                                                 StandardOpenOption/WRITE
                                                 LinkOption/NOFOLLOW_LINKS]))]
        (when-not (Files/isRegularFile lock-path
                                       (into-array LinkOption
                                                   [LinkOption/NOFOLLOW_LINKS]))
          (throw (ex-info "Repair finding publication lock malformed"
                          {:reason :repair-finding-root-refused})))
        (with-open [_lock (.lock lock-channel)]
          (try
        (with-open [ch (FileChannel/open file-path
                                        (into-array OpenOption
                                                    [StandardOpenOption/CREATE_NEW
                                                     StandardOpenOption/WRITE
                                                     LinkOption/NOFOLLOW_LINKS]))]
          (let [buffer (ByteBuffer/wrap bytes)]
            (while (.hasRemaining buffer) (.write ch buffer)))
          (.force ch true))
        (with-open [parent (FileChannel/open (.toPath directory)
                                             (make-array OpenOption 0))]
          (.force parent true))
        (.getPath file)
        (catch java.nio.file.FileAlreadyExistsException e
          (if (and (Files/isRegularFile file-path
                                        (into-array LinkOption
                                                    [LinkOption/NOFOLLOW_LINKS]))
                   (not (Files/isSymbolicLink file-path))
                   (= directory (.getCanonicalFile (.getParentFile file)))
                   (let [existing (Files/readAllBytes file-path)]
                     (or (java.util.Arrays/equals bytes existing)
                         (java.util.Arrays/equals ^bytes @raw-bytes existing)
                         (java.util.Arrays/equals ^bytes @legacy-bytes existing)
                         ;; Occurrence records retain the FIRST opened-at.
                         ;; A later observation reconstructs the same semantic
                         ;; payload with a later observation timestamp; that is
                         ;; replay, not a second finding or a byte conflict.
                         (when (:repair/occurrence value)
                           (try
                             (= (dissoc (edn/read-string
                                         (String. existing "UTF-8"))
                                        :opened-at)
                                (dissoc (durable-finding value) :opened-at))
                             (catch Throwable _ false))))))
            (.getPath file)
              (throw (ex-info "Immutable repair finding conflicts with existing bytes"
                              {:reason :repair-finding-conflict
                               :path (.getPath file)} e))))))))))))

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

(defn occurrence-identity
  "Mint the stable, authority-qualified identity of one failed occurrence.

  ORIGIN names the authority/domain that created the occurrence (normally the
  runner run id plus repository), EVENT-ID is the durable job/event identity,
  and FAILURE-KIND is the typed originating failure.  Display ordinals such as
  attempt-001 are never used as the identity itself.  Reconstructing these
  exact inputs reconstructs the same id, which is required when containment
  layers observe the same failure more than once."
  [{:keys [origin event-id failure-kind]}]
  (when-not (and (nonblank? origin) (nonblank? event-id)
                 (keyword? failure-kind))
    (throw (ex-info "Repair occurrence identity lacks authority"
                    {:repair-occurrence/refusal :occurrence-identity-invalid
                     :origin origin :event-id event-id
                     :failure-kind failure-kind})))
  (let [subject [origin event-id failure-kind]
        id (str "occ-" (digest/sha256 (pr-str subject)))]
    {:occurrence/id id
     :occurrence/origin origin
     :occurrence/event-id event-id
     :occurrence/failure-kind failure-kind}))

(declare occurrence-finding-id)

(defn- validate-occurrence! [occurrence failure-kind repair-id]
  (when occurrence
    (let [expected (occurrence-identity
                    {:origin (:occurrence/origin occurrence)
                     :event-id (:occurrence/event-id occurrence)
                     :failure-kind (:occurrence/failure-kind occurrence)})
          expected-id (occurrence-finding-id expected)]
      (when-not (and (= (set (keys expected)) (set (keys occurrence)))
                     (= expected occurrence)
                     (= failure-kind (:occurrence/failure-kind occurrence))
                     (or (nil? repair-id) (= repair-id expected-id)))
        (throw (ex-info "Repair occurrence identity conflicts with finding"
                        {:repair-occurrence/refusal :occurrence-identity-conflict
                         :occurrence occurrence :failure-kind failure-kind
                         :repair-id repair-id :expected-repair-id expected-id})))
      expected)))

(defn- occurrence-finding-id [occurrence]
  (str "repair-" (:occurrence/id occurrence)))

(defn- occurrence-evidence!
  "Append one immutable observation of an occurrence.  The finding itself is
  published/reused by occurrence id; these records retain later observations
  without minting another open finding."
  [root occurrence finding-id observation]
  (let [observation-id (or (:observation/id observation)
                           (:occurrence/event-id occurrence))
        record {:schema :wm/repair-occurrence-observation-v1
                :occurrence/id (:occurrence/id occurrence)
                :finding/id finding-id
                :observation/id observation-id
                :observed-at (or (:observed-at observation) (str (Instant/now)))
                :source (or (:source observation)
                            (:occurrence/origin occurrence))}
        path (io/file root "occurrence-evidence" (:occurrence/id occurrence)
                      (str (digest/sha256 (str observation-id)) ".edn"))]
    ;; The lock authority requires an existing canonical parent. Directory
    ;; creation is idempotent; CREATE_NEW still governs the evidence file.
    (io/make-parents path)
    (try
      (write-new! path record)
      (catch java.nio.file.FileAlreadyExistsException _
        (let [existing (edn/read-string (slurp path))]
          (when-not (= (dissoc existing :observed-at)
                       (dissoc record :observed-at))
            (throw (ex-info "Repair occurrence observation conflicts"
                            {:reason :repair-occurrence-observation-conflict
                             :occurrence/id (:occurrence/id occurrence)
                             :observation/id observation-id}))))))
    record))

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
                 review-verdict review-text occurrence observation]
          :as finding}]
   (when-not (and (string? attempt-id) target commit selected-entry reviewer
                  review-job (#{:request-changes :reject} review-verdict)
                  (not (str/blank? (str review-text))))
     (throw (ex-info "Stop-the-line finding lacks required provenance"
                     {:finding finding})))
   (let [failure-kind (case review-verdict
                        :request-changes :review-request-changes
                        :reject :review-rejected)
         occurrence (validate-occurrence! occurrence failure-kind nil)
         id (if occurrence
              (occurrence-finding-id occurrence)
              (obligation-id attempt-id failure-kind))
         record (cond-> {:repair/id id
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
                 :failure-kind failure-kind
                 :discharge-contract review-failure-discharge-contract
                 :opened-at (or (:opened-at finding)
                                (:observed-at observation)
                                (str (Instant/now)))}
                  occurrence (assoc :repair/occurrence occurrence))]
     (write-new-or-identical! root id record)
     (when occurrence
       (occurrence-evidence! root occurrence id observation))
     (if occurrence
       (edn/read-string (slurp (io/file root "findings" (str id ".edn"))))
       record))))

(defn record-system-failure!
  "Record a zero-achievement stop-line without mis-typing every cause as a
  code defect. `:repair/class` distinguishes machine failures, environmental
  holds, and recoverable incomplete work. Selection fields are optional
  because readiness and substrate failures can precede policy selection."
  ([finding] (record-system-failure! default-root finding))
  ([root {:keys [attempt-id repair-id repair-class failure-stage outcome error
                 occurrence observation]
          :as finding}]
   (when-not (and (string? attempt-id)
                  (#{:machine-failure :environmental-hold
                     :incomplete-recoverable} repair-class)
                  (keyword? failure-stage) (keyword? outcome)
                  (not (str/blank? (str error))))
     (throw (ex-info "System stop-the-line finding lacks required provenance"
                     {:finding finding})))
   (let [occurrence (validate-occurrence! occurrence
                                          (:failure-kind finding) repair-id)
         id (or (when occurrence (occurrence-finding-id occurrence))
                repair-id
                (obligation-id attempt-id (:failure-kind finding)))
         record (cond-> {:repair/id id
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
                 :opened-at (or (:opened-at finding)
                                (:observed-at observation)
                                (str (Instant/now)))}
                  occurrence (assoc :repair/occurrence occurrence))]
     (write-new-or-identical! root id record)
     (when occurrence
       (occurrence-evidence! root occurrence id observation))
     (if occurrence
       (edn/read-string (slurp (io/file root "findings" (str id ".edn"))))
       record))))

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
  (with-contended-store-lock root
   (fn []
    (when-not (safe-id? record-id) (throw (ex-info "Unsafe repair identity" {})))
    (let [base (historical-directory! root child true)
        target (io/file base (str record-id ".edn"))
        ;; pr-str, not pprint: see write-new-or-identical! above.
        bytes (.getBytes (pr-str value) "UTF-8")]
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
       :sha256 (digest/sha256 (String. bytes "UTF-8"))}))))

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

(defn- validated-execution? [execution identity]
  (and (= :runner-execution (:kind execution) (:kind identity))
       (keyword? (:cohort-id execution))
       (safe-id? (:attempt-id execution))
       (string? (:cohort-sha256 execution))
       (re-matches #"[0-9a-f]{64}" (:cohort-sha256 execution))
       (case (:identity-version execution)
         1 (and (= #{:kind :id :identity-version :cohort-id :cohort-sha256
                     :data-root-sha256 :authority-id :attempt-id}
                   (set (keys execution)))
                (= identity (select-keys execution [:kind :id]))
                (every? #(and (string? %) (re-matches #"[0-9a-f]{64}" %))
                        ((juxt :data-root-sha256 :authority-id) execution)))
         0 (and (= #{:kind :id :legacy-id :identity-version :cohort-id
                     :cohort-sha256 :attempt-id}
                   (set (keys execution)))
                (= (:id execution) (:legacy-id execution)
                   (str (name (:cohort-id execution)) "--" (:attempt-id execution)))
                (contains? #{{:kind :runner-execution :id (:attempt-id execution)}
                             {:kind :runner-execution :id (:legacy-id execution)}}
                           identity))
         false)))

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
     (when-not (and (contains? #{:wm/historical-repair-resolution-v1
                                 :wm/historical-repair-resolution-v2}
                               (:schema record))
                    (= repair-id (:repair/id record) (:repair/id admission))
                    (= (:verification-id admission) (:verification-id record))
                    (= (:verification-attempt admission) (:verification-attempt record))
                    (= :resolved (:repair/status record))
                    (= :succeeded (:task-result record))
                    (= :safe (:infrastructure record))
                    (execution-identity? (:validation-attempt record))
                    (execution-identity? (:verification-attempt record))
                    (if (= :wm/historical-repair-resolution-v2 (:schema record))
                      (and (validated-execution? (:verification-execution record)
                                                 (:verification-attempt record))
                           (validated-execution? (:validation-execution record)
                                                 (:validation-attempt record))
                           (not= (:verification-execution record)
                                 (:validation-execution record)))
                      (and (= #{:cohort-id :cohort-sha256 :attempt-id}
                              (set (keys (:verification-execution record))))
                           (keyword? (get-in record [:verification-execution :cohort-id]))
                           (= (get-in record [:verification-attempt :id])
                              (get-in record [:verification-execution :attempt-id]))
                           (string? (get-in record [:verification-execution :cohort-sha256]))
                           (re-matches #"[0-9a-f]{64}"
                                       (get-in record [:verification-execution :cohort-sha256]))
                           (= #{:cohort-id :cohort-sha256 :attempt-id}
                              (set (keys (:validation-execution record))))
                           (keyword? (get-in record [:validation-execution :cohort-id]))
                           (safe-id? (get-in record [:validation-execution :attempt-id]))
                           (string? (get-in record [:validation-execution :cohort-sha256]))
                           (re-matches #"[0-9a-f]{64}"
                                       (get-in record [:validation-execution :cohort-sha256]))
                           (= (get-in record [:validation-attempt :id])
                              (str (name (get-in record [:validation-execution :cohort-id]))
                                   "--" (get-in record [:validation-execution :attempt-id])))
                           (not= (select-keys (:validation-execution record)
                                              [:cohort-id :attempt-id])
                                 (select-keys (:verification-execution record)
                                              [:cohort-id :attempt-id]))))
                    (not= (:validation-attempt record) (:verification-attempt record))
                    (not= (get-in record [:validation-attempt :id]) (:attempt-id finding))
                    (safe-id? (:click-id record)) (safe-id? (:run-id record))
                    (every? #(and (string? %) (re-matches #"[0-9a-f]{64}" %))
                            ((juxt :projection-digest :run-record-digest) record)))
       (throw (ex-info "Historical successor evidence refused" {:repair/id repair-id})))
     (try
       (write-new-durable! root "resolutions" repair-id record)
       (catch java.nio.file.FileAlreadyExistsException _
         (let [directory (historical-directory! root "resolutions" false)
               existing (capture-under! directory (io/file directory (str repair-id ".edn")))]
           (when-not (= record (:value existing))
             (throw (ex-info "Historical resolution conflicts with retained evidence"
                             {:repair/id repair-id}))))))
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
  resolution or administrative-dismissal records. Unlike `open-obligations`,
  this is an audit view."
  ([attempt-id] (obligation-history default-root attempt-id))
  ([root attempt-id]
   (let [implementations (indexed-records root "implementations")
         verifications (verified-admissions root)
         resolutions (indexed-records root "resolutions")
         dismissals (indexed-records root "dismissals")]
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
                           (get resolutions (:repair/id finding)))
                    (get dismissals (:repair/id finding))
                    (assoc :repair/status (:repair/status
                                           (get dismissals (:repair/id finding)))
                           :repair/dismissal
                           (get dismissals (:repair/id finding))))))))))

(defn open-obligations
  ([] (open-obligations default-root))
  ([root]
   (let [resolved (set (map :repair/id (records (io/file root "resolutions"))))
         dismissed (set (map :repair/id (records (io/file root "dismissals"))))
         implementations (indexed-records root "implementations")
         verifications (verified-admissions root)]
     (->> (records (io/file root "findings"))
          (remove #(or (contains? resolved (:repair/id %))
                       (contains? dismissed (:repair/id %))))
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

(defn- dismissal-refuse! [reason data]
  (throw (ex-info "Unexecuted repair finding dismissal refused"
                  (assoc data :repair-dismissal/refusal reason))))

(defn dismiss-wontfix!
  "Append an operator-authorized WONTFIX disposition, never a repair success.
  Authority must cite the operator ruling; reason must state this finding's
  evidence of permanent unfixability. These are accountable human judgments,
  not facts inferred from age, a missing agent, or a failed readiness check.
  An implementation awaiting validation must complete its existing route."
  ([finding-id disposition]
   (dismiss-wontfix! default-root finding-id disposition))
  ([root finding-id {:keys [authority reason actor] :as disposition}]
   (when-not (and (string? finding-id)
                  (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (doseq [[k v] [[:authority authority] [:reason reason] [:actor actor]]]
     (when-not (nonblank? v)
       (dismissal-refuse! :disposition-invalid {:repair/id finding-id :field k})))
   (when-not (= #{:authority :reason :actor} (set (keys disposition)))
     (dismissal-refuse! :disposition-invalid {:repair/id finding-id}))
   (when (get (indexed-records root "dismissals") finding-id)
     (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
   (when (get (indexed-records root "resolutions") finding-id)
     (dismissal-refuse! :already-resolved {:repair/id finding-id}))
   (let [finding (get (indexed-records root "findings") finding-id)]
     (when-not finding
       (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
     (when (or (not= :open (:repair/status finding))
               (get (indexed-records root "implementations") finding-id)
               (get (verified-admissions root) finding-id))
       (dismissal-refuse! :finding-not-open {:repair/id finding-id}))
     (let [record {:repair/id finding-id :repair/schema-version 1
                   :repair/status :dismissed-wontfix :dismissal/kind :wontfix
                   :failed-attempt (:attempt-id finding)
                   :authority authority :reason reason :actor actor
                   :dismissed-at (str (Instant/now))}]
       (write-new! (io/file root "dismissals" (str finding-id ".edn")) record)
       record))))

(defn dismiss-condition-cleared!
  "Dismiss a moot environmental hold with a retained, dated re-check.
  Evidence is {:checked-at ISO-instant :source nonblank-string
  :observation nonempty-map}. The actor is accountable for its relevance and
  interpretation; this is neither repair validation nor permanent retirement."
  ([finding-id disposition]
   (dismiss-condition-cleared! default-root finding-id disposition))
  ([root finding-id {:keys [evidence actor reason] :as disposition}]
   (when-not (and (string? finding-id)
                  (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (when-not (and (= #{:evidence :actor :reason} (set (keys disposition)))
                  (nonblank? actor) (nonblank? reason))
     (dismissal-refuse! :disposition-invalid {:repair/id finding-id}))
   (when-not (and (map? evidence)
                  (nonblank? (:source evidence))
                  (map? (:observation evidence)) (seq (:observation evidence))
                  (nonblank? (:checked-at evidence))
                  (try (Instant/parse (:checked-at evidence))
                       (catch Exception _ nil)))
     (dismissal-refuse! :evidence-invalid {:repair/id finding-id}))
   (when (get (indexed-records root "dismissals") finding-id)
     (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
   (when (get (indexed-records root "resolutions") finding-id)
     (dismissal-refuse! :already-resolved {:repair/id finding-id}))
   (let [finding (get (indexed-records root "findings") finding-id)]
     (when-not finding
       (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
     (when (or (not= :open (:repair/status finding))
               (get (indexed-records root "implementations") finding-id)
               (get (verified-admissions root) finding-id))
       (dismissal-refuse! :finding-not-open {:repair/id finding-id}))
     (let [record {:repair/id finding-id :repair/schema-version 1
                   :repair/status :dismissed-condition-cleared
                   :dismissal/kind :condition-cleared
                   :failed-attempt (:attempt-id finding)
                   :evidence evidence :actor actor :reason reason
                   :dismissed-at (str (Instant/now))}]
       (write-new! (io/file root "dismissals" (str finding-id ".edn")) record)
       record))))

(defn dismiss-unexecuted!
  "Append an administrative disposition only when the immutable finding itself
  proves that its dispatched job never executed. This is not a repair success,
  resolution, or supersession: the finding remains unchanged and audit-visible.

  The sole accepted execution carrier is
  [:failure-data :author-job :execution]. Missing or internally inconsistent
  evidence receives no benefit of the doubt."
  ([finding-id disposition]
   (dismiss-unexecuted! default-root finding-id disposition))
  ([root finding-id {:keys [authority reason cause-fix actor] :as disposition}]
   (when-not (and (string? finding-id)
                  (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (let [dismissals (indexed-records root "dismissals")]
     (when (contains? dismissals finding-id)
       (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
     (let [finding (first (filter #(= finding-id (:repair/id %))
                                  (records (io/file root "findings"))))
           resolutions (indexed-records root "resolutions")
           implementation (get (indexed-records root "implementations") finding-id)
           verification (get (verified-admissions root) finding-id)
           effective-status (cond
                              (get resolutions finding-id) :resolved
                              (or implementation verification) :awaiting-validation
                              finding (:repair/status finding))
           execution (get-in finding [:failure-data :author-job :execution])]
       (when-not finding
         (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
       (when-not (= :open effective-status)
         (dismissal-refuse! :finding-not-open
                             {:repair/id finding-id :repair/status effective-status}))
       (cond
         (true? (:executed execution))
         (dismissal-refuse! :finding-executed {:repair/id finding-id})

         (not (and (map? execution)
                   (false? (:executed execution))
                   (= 0 (:tool-events execution))
                   (= 0 (:command-events execution))))
         (dismissal-refuse! :execution-not-retained {:repair/id finding-id})

         (not (and (nonblank? authority) (keyword? reason)
                   (nonblank? actor)
                   (or (nil? cause-fix) (nonblank? cause-fix))
                   (= (cond-> #{:authority :reason :actor}
                        (some? cause-fix) (conj :cause-fix))
                      (set (keys disposition)))))
         (dismissal-refuse! :disposition-invalid {:repair/id finding-id})

         :else
         (let [record (cond->
                       {:repair/id finding-id
                        :repair/schema-version 1
                        :repair/status :dismissed-unexecuted
                        :failed-attempt (:attempt-id finding)
                        :authority authority
                        :reason reason
                        :actor actor
                        :execution execution
                        :dismissed-at (str (Instant/now))}
                        cause-fix (assoc :cause-fix cause-fix))]
           (write-new! (io/file root "dismissals" (str finding-id ".edn"))
                       record)
           record))))))

(defn dismiss-echo!
  "Append a closing disposition for a finding whose own retained T8 witness
  cites only already-disposed source findings. The accepted proof carrier is
  exactly [:failure-data :tripwire/witness :repair-ids]; callers cannot supply
  or augment its membership. Findings and their source records remain
  immutable and audit-visible."
  ([finding-id disposition]
   (dismiss-echo! default-root finding-id disposition))
  ([root finding-id {:keys [authority reason actor cause-note] :as disposition}]
   (when-not (and (string? finding-id)
                  (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (let [dismissals (indexed-records root "dismissals")]
     (when (contains? dismissals finding-id)
       (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
     (let [finding (first (filter #(= finding-id (:repair/id %))
                                  (records (io/file root "findings"))))
           resolutions (indexed-records root "resolutions")
           implementation (get (indexed-records root "implementations") finding-id)
           verification (get (verified-admissions root) finding-id)
           effective-status (cond
                              (get resolutions finding-id) :resolved
                              (or implementation verification) :awaiting-validation
                              finding (:repair/status finding))
           source-ids (get-in finding
                              [:failure-data :tripwire/witness :repair-ids])]
       (when-not finding
         (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
       (when-not (= :open effective-status)
         (dismissal-refuse! :finding-not-open
                             {:repair/id finding-id :repair/status effective-status}))
       (when-not (and (vector? source-ids)
                      (seq source-ids)
                      (every? #(and (string? %)
                                    (re-matches #"[A-Za-z0-9._-]+" %))
                              source-ids)
                      (= (count source-ids) (count (distinct source-ids))))
         (dismissal-refuse! :no-witness-retained {:repair/id finding-id}))
       (let [source-statuses
             (mapv (fn [source-id]
                     {:repair/id source-id
                      :status-at-dismissal
                      (cond
                        (get dismissals source-id)
                        (:repair/status (get dismissals source-id))
                        (get resolutions source-id)
                        (:repair/status (get resolutions source-id)))})
                   source-ids)
             live-ids (mapv :repair/id
                            (filter #(nil? (:status-at-dismissal %))
                                    source-statuses))]
         (when (seq live-ids)
           (dismissal-refuse! :sources-not-disposed
                               {:repair/id finding-id
                                :live-source-ids live-ids}))
         (when-not (and (nonblank? authority) (keyword? reason)
                        (nonblank? actor)
                        (or (nil? cause-note) (nonblank? cause-note))
                        (= (cond-> #{:authority :reason :actor}
                             (some? cause-note) (conj :cause-note))
                           (set (keys disposition))))
           (dismissal-refuse! :disposition-invalid {:repair/id finding-id}))
         (let [record (cond->
                       {:repair/id finding-id
                        :repair/schema-version 1
                        :repair/status :dismissed-echo
                        :failed-attempt (:attempt-id finding)
                        :authority authority
                        :reason reason
                        :actor actor
                        :witness-sources source-statuses
                        :dismissed-at (str (Instant/now))}
                        cause-note (assoc :cause-note cause-note))]
           (write-new! (io/file root "dismissals" (str finding-id ".edn"))
                       record)
           record))))))

(defn dismiss-fixture-pollution!
  "Append a closing disposition only when a finding's retained repository
  resolution proves that its artifact subject resolved outside the repository
  claimed by its artifact binding. The caller cannot supply this proof.

  A binding that names the finding's production machine repository refuses
  before considering absent resolution data. Otherwise both claimed and
  resolved repository paths must be retained and distinct."
  ([finding-id disposition]
   (dismiss-fixture-pollution! default-root finding-id disposition))
  ([root finding-id {:keys [authority reason actor cause-fix] :as disposition}]
   (when-not (and (string? finding-id)
                  (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (let [dismissals (indexed-records root "dismissals")]
     (when (contains? dismissals finding-id)
       (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
     (let [finding (first (filter #(= finding-id (:repair/id %))
                                  (records (io/file root "findings"))))
           resolutions (indexed-records root "resolutions")
           implementation (get (indexed-records root "implementations") finding-id)
           verification (get (verified-admissions root) finding-id)
           effective-status (cond
                              (get resolutions finding-id) :resolved
                              (or implementation verification) :awaiting-validation
                              finding (:repair/status finding))
           machine-repo (:machine-repo finding)
           claimed-repo (get-in finding [:failure-data :artifact-binding :repo])
           resolved-repo (get-in finding [:failure-data :resolved-repository])
           canonical (fn [path]
                       (when (nonblank? path)
                         (.getCanonicalPath (io/file path))))]
       (when-not finding
         (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
       (when-not (= :open effective-status)
         (dismissal-refuse! :finding-not-open
                             {:repair/id finding-id :repair/status effective-status}))
       (when (and (nonblank? machine-repo) (nonblank? claimed-repo)
                  (= (canonical machine-repo) (canonical claimed-repo)))
         (dismissal-refuse! :subject-is-production
                             {:repair/id finding-id
                              :machine-repo machine-repo
                              :claimed-repository claimed-repo}))
       (when-not (and (nonblank? claimed-repo) (nonblank? resolved-repo))
         (dismissal-refuse! :resolution-not-retained {:repair/id finding-id}))
       (when (= (canonical claimed-repo) (canonical resolved-repo))
         (dismissal-refuse! :subject-is-production
                             {:repair/id finding-id
                              :claimed-repository claimed-repo
                              :resolved-repository resolved-repo}))
       (when-not (and (nonblank? authority) (keyword? reason)
                      (nonblank? actor)
                      (or (nil? cause-fix) (nonblank? cause-fix))
                      (= (cond-> #{:authority :reason :actor}
                           (some? cause-fix) (conj :cause-fix))
                         (set (keys disposition))))
         (dismissal-refuse! :disposition-invalid {:repair/id finding-id}))
       (let [resolution-evidence
             {:machine-repo machine-repo
              :claimed-repository claimed-repo
              :resolved-repository resolved-repo}
             record (cond->
                     {:repair/id finding-id
                      :repair/schema-version 1
                      :repair/status :dismissed-fixture-pollution
                      :failed-attempt (:attempt-id finding)
                      :authority authority
                      :reason reason
                      :actor actor
                      :resolution-evidence resolution-evidence
                      :dismissed-at (str (Instant/now))}
                      cause-fix (assoc :cause-fix cause-fix))]
         (write-new! (io/file root "dismissals" (str finding-id ".edn")) record)
         record)))))

(defn- retained-attempt-target [root finding]
  ;; These carriers name this attempt's selected target. Never recursively
  ;; collect ancestor :target fields: those describe different repairs.
  (let [ids (distinct (remove nil? [(:target finding)
                                    (get-in finding [:selected-entry :action :target])
                                    (get-in finding [:selected-entry :action
                                                     :repair-obligation :repair/id])]))
        id (first ids)]
    (when (and (= 1 (count ids)) (nonblank? id)
               (some #(= id (:repair/id %)) (records (io/file root "findings"))))
      id)))

(defn dismiss-superseded-attempt!
  "Dismiss only from retained target and later, distinct implementation proof.
  Ancestor targets, caller-supplied proof and ambiguous short attempt aliases
  cannot establish supersession. This does not validate the target's repair."
  ([finding-id disposition]
   (dismiss-superseded-attempt! default-root finding-id disposition))
  ([root finding-id {:keys [authority reason actor] :as disposition}]
   (when-not (and (string? finding-id)
                 (re-matches #"[A-Za-z0-9._-]+" finding-id))
     (dismissal-refuse! :finding-id-invalid {:repair/id finding-id}))
   (when (get (indexed-records root "dismissals") finding-id)
     (dismissal-refuse! :already-dismissed {:repair/id finding-id}))
   (let [finding (get (indexed-records root "findings") finding-id)
         implementations (indexed-records root "implementations")
         resolutions (indexed-records root "resolutions")]
     (when-not finding
       (dismissal-refuse! :finding-not-found {:repair/id finding-id}))
     (when (or (not= :open (:repair/status finding))
               (get implementations finding-id) (get resolutions finding-id)
               (get (verified-admissions root) finding-id))
       (dismissal-refuse! :finding-not-open {:repair/id finding-id}))
     (when-not (and (= #{:authority :reason :actor} (set (keys disposition)))
                    (nonblank? authority) (keyword? reason) (nonblank? actor))
       (dismissal-refuse! :disposition-invalid {:repair/id finding-id}))
     (let [target (retained-attempt-target root finding)
           _ (when-not target
               (dismissal-refuse! :target-not-resolvable {:repair/id finding-id}))
           ;; Prefer the actual implementation. A later validation must not
           ;; disguise a same-attempt implementation as a different repair.
           impl (get implementations target)
           record (or impl (get resolutions target))
           attempt (if impl (:implementation-attempt record)
                       (:validation-attempt record))
           failed (:attempt-id finding)
           timestamp (if impl (:implemented-at record) (:resolved-at record))
           instant (fn [s] (try (Instant/parse s) (catch Exception _ nil)))
           opened (instant (:opened-at finding))
           implemented (instant timestamp)]
       (when-not (and record (nonblank? attempt) (nonblank? failed)
                      (if impl (= :awaiting-validation (:repair/status record))
                          (= :resolved (:repair/status record))))
         (dismissal-refuse! :target-not-implemented {:repair/id finding-id}))
       (when (or (= attempt failed)
                 (str/ends-with? failed (str "--" attempt))
                 (str/ends-with? attempt (str "--" failed)))
         (dismissal-refuse! :self-implementation {:repair/id finding-id}))
       (when-not (and opened implemented (.isAfter ^Instant implemented opened))
         (dismissal-refuse! :implementation-predates-attempt {:repair/id finding-id}))
       (let [dismissal {:repair/id finding-id :repair/schema-version 1
                        :repair/status :dismissed-superseded-attempt
                        :failed-attempt failed :target-id target
                        :implementation-record-id (:repair/id record)
                        :implementation-record-path
                        (str (io/file root (if impl "implementations" "resolutions")
                                      (str target ".edn")))
                        :implementation-attempt attempt :implemented-at timestamp
                        :authority authority :reason reason :actor actor
                        :dismissed-at (str (Instant/now))}]
         (write-new! (io/file root "dismissals" (str finding-id ".edn")) dismissal)
         dismissal)))))

(defn discharge-record
  "Read one immutable record, retaining its exact UTF-8 bytes for a derived
   discharge receipt. No pending queue membership is required."
  [root child id]
  (when-not (and (#{"findings" "implementations" "resolutions"} child)
                 (string? id) (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]*" id))
    (throw (ex-info "Invalid discharge store key" {:child child :repair/id id})))
  (let [file (io/file root child (str id ".edn"))]
    (when (.exists file)
      (let [capture (capture-under! root file)
            bytes (Files/readAllBytes (.toPath file))
            text (:text capture)]
        (when-not (and (java.util.Arrays/equals bytes (.getBytes text "UTF-8"))
                       (= id (get-in capture [:value :repair/id])))
          (throw (ex-info "Discharge store record binding invalid" {:path (str file)})))
        {:id id :path (str child "/" id ".edn") :sha256 (:sha256 capture)
         :edn-text text :value (:value capture)}))))

(defn discharge-resolution-ids
  "Enumerate the authority, not the open queue, so resolved-but-unpublished
   findings survive a crash. Each record is validated separately by its caller."
  [root]
  (->> (or (.listFiles (io/file root "resolutions")) [])
       (filter #(.isFile ^java.io.File %))
       (map #(.getName ^java.io.File %))
       (filter #(str/ends-with? % ".edn"))
       (map #(subs % 0 (- (count %) 4))) sort vec))

(defn record-discharge-operation!
  "Append an intent or outcome through the store. Content-addressed operation
   records are replay-idempotent only when the existing exact value agrees."
  [root kind value]
  (when-not (#{:intent :outcome} kind)
    (throw (ex-info "Unknown discharge operation" {:kind kind})))
  (let [record {:schema :wm/repair-discharge-operation-v1 :kind kind :value value}
        text (pr-str record)
        _ (when-not (= record (strict-read text :discharge-operation))
            (throw (ex-info "Unreadable discharge operation" {:kind kind})))
        id (digest/sha256 text)
        path (io/file root "discharge-operations" (str id ".edn"))]
    (try
      (write-new! path record)
      (catch java.nio.file.FileAlreadyExistsException e
        (when-not (= record (strict-read (slurp path) path)) (throw e))))
    {:id id :path (.getCanonicalPath path) :kind kind}))

(defn- discharge-context! [phase obligation record]
  (when-let [context (:repair/discharge-context record)]
    (when-not (and (= :wm/repair-discharge-context-v1 (:schema context))
                   (= phase (:phase context))
                   (= (:repair/id obligation) (:repair/id context))
                   (= (:attempt-id record) (get-in context [:close :attempt/id]))
                   (= (:review-job record) (get-in context [:review-job :job-id]))
                   (= context (strict-read (pr-str context) :discharge-context)))
      (throw (ex-info "Discharge context does not bind the store transition"
                      {:phase phase :repair/id (:repair/id obligation)})))
    context))

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
   (let [context (discharge-context! :implementation obligation implementation)
         shape (artifact-shape obligation)
         ;; Every conjunct of the historical guard, refusing identically but
         ;; NAMING what failed. The 2026-09-12 stop-line failure (repair-ea1-
         ;; 7093...-untyped-failure) refused here on attempt-distinctness while
         ;; the message claimed missing grounded review evidence; the conflated
         ;; conjuncts made that undiagnosable from the retained record.
         evidence-ok? (try (boolean (artifact-evidence? obligation
                                                        implementation true))
                           (catch Throwable _ false))
         failed (cond-> []
                  (not (contains? artifact-shapes shape))
                  (conj :artifact-shape-unknown)
                  (not (:repair/id obligation))
                  (conj :obligation-id-missing)
                  (not (#{:machine-failure :independent-review-failure}
                        (:repair/class obligation)))
                  (conj :repair-class-invalid)
                  (not attempt-id) (conj :implementation-attempt-missing)
                  (not reviewer) (conj :reviewer-missing)
                  (not review-job) (conj :review-job-missing)
                  (and (= 3 (:repair/schema-version obligation))
                       (not (grounded-review-evidence? obligation implementation)))
                  (conj :grounded-review-evidence-invalid)
                  (and attempt-id (= attempt-id (:attempt-id obligation)))
                  (conj :implementation-attempt-not-distinct)
                  (not evidence-ok?) (conj :artifact-evidence-invalid)
                  (not (:resolved? witness)) (conj :witness-not-resolved)
                  (not (:dial-moved? witness)) (conj :witness-dial-not-moved))]
     (when (seq failed)
       (throw (ex-info "Machine repair implementation lacks grounded review evidence"
                       {:outcome :incomplete
                        :failure-kind :machine-repair-lacks-grounded-review-evidence
                        :failure-stage :stop-line-resolution
                        :failure-detail failed
                        :obligation obligation
                        :implementation implementation})))
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
                    context
                    (assoc :repair/phase :implementation
                           :repair/discharge-context context)
                    (= 3 (:repair/schema-version obligation))
                    (assoc :grounded-review-evidence
                           (:review-evidence implementation)
                           :artifact-binding
                           (:artifact-binding implementation))
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
   (let [context (discharge-context! :successor-validation obligation resolution)
         implementation (:repair/implementation obligation)
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
                         (artifact-record shape resolution))
                  context
                  (assoc :repair/phase :successor-validation
                         :repair/discharge-context context)
                  (:successor-relation resolution)
                  (assoc :successor-relation (:successor-relation resolution)))]
     (write-new! (io/file root "resolutions"
                          (str (:repair/id obligation) ".edn"))
                 record)
     record))))

(def ^:private successor-relation-schema :wm/repair-successor-relation-v1)

(defn- successor-refuse! [reason data]
  (throw (ex-info "Repair successor evidence refused"
                  (assoc data :repair-successor/refusal reason))))

(defn- strict-keys? [m ks]
  (and (map? m) (= ks (set (keys m)))))

(defn- sha256-string? [x]
  (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))

(defn- parsed-instant [x]
  (try (when (string? x) (Instant/parse x)) (catch Throwable _ nil)))

(defn successor-resolution!
  "Validate retained repairing-close R and later grounded successor-close S,
  then delegate to the existing authorized resolution writer. The injected
  readers/writer make the boundary testable without touching the live store.
  A pre-existing resolution is returned as a stable no-op."
  [{:keys [obligation repair-close successor-close authority
           resolution-read-fn resolve-fn]}]
  (when-not successor-close
    (successor-refuse! :successor-missing {:repair/id (:repair/id obligation)}))
  (let [repair-keys #{:attempt/id :run/id :repair/id :closed-at :commit
                      :review-receipt-ids :review-sha256 :grounded?}
        successor-keys #{:attempt/id :run/id :repair/id :closed-at
                         :witness-ref :witness-sha256 :grounded?
                         :production-shaped? :witness}
        authority-keys #{:decided-by :review-job}
        repair-at (parsed-instant (:closed-at repair-close))
        successor-at (parsed-instant (:closed-at successor-close))
        repair-id (:repair/id obligation)]
    (when-not (and (strict-keys? repair-close repair-keys)
                   (= true (:grounded? repair-close))
                   (string? (:attempt/id repair-close))
                   (string? (:run/id repair-close))
                   (string? (:commit repair-close))
                   (vector? (:review-receipt-ids repair-close))
                   (seq (:review-receipt-ids repair-close))
                   (every? #(and (string? %) (not (str/blank? %)))
                           (:review-receipt-ids repair-close))
                   (sha256-string? (:review-sha256 repair-close)) repair-at)
      (successor-refuse! :repair-evidence-invalid {:repair-close repair-close}))
    (when-not (and (strict-keys? successor-close successor-keys)
                   (= true (:grounded? successor-close))
                   (= true (:production-shaped? successor-close))
                   (string? (:attempt/id successor-close))
                   (string? (:run/id successor-close))
                   (string? (:witness-ref successor-close))
                   (sha256-string? (:witness-sha256 successor-close))
                   (true? (get-in successor-close [:witness :resolved?]))
                   (true? (get-in successor-close [:witness :dial-moved?]))
                   successor-at)
      (successor-refuse! :successor-evidence-invalid
                         {:successor-close successor-close}))
    (when-not (and (strict-keys? authority authority-keys)
                   (every? #(and (string? %) (not (str/blank? %)))
                           [(:decided-by authority) (:review-job authority)]))
      (successor-refuse! :resolution-authority-invalid {:authority authority}))
    (when-not (and (= repair-id (:repair/id repair-close))
                   (= repair-id (:repair/id successor-close)))
      (successor-refuse! :successor-lineage-mismatch
                         {:repair/id repair-id
                          :repair-close/id (:repair/id repair-close)
                          :successor-close/id (:repair/id successor-close)}))
    (when (or (= (:attempt/id repair-close) (:attempt/id successor-close))
              (= (:run/id repair-close) (:run/id successor-close)))
      (successor-refuse! :self-successor
                         {:repair-attempt (:attempt/id repair-close)
                          :successor-attempt (:attempt/id successor-close)}))
    (when-not (.isBefore repair-at successor-at)
      (successor-refuse! :successor-not-later
                         {:repair-closed-at (:closed-at repair-close)
                          :successor-closed-at (:closed-at successor-close)}))
    (if-let [existing (resolution-read-fn repair-id)]
      {:status :already-resolved :resolution existing}
      (let [relation {:schema successor-relation-schema
                      :repair/id repair-id
                      :repair-attempt/id (:attempt/id repair-close)
                      :repair-run/id (:run/id repair-close)
                      :repair-closed-at (:closed-at repair-close)
                      :repair-commit (:commit repair-close)
                      :repair-review-receipt-ids (:review-receipt-ids repair-close)
                      :repair-review-sha256 (:review-sha256 repair-close)
                      :successor-attempt/id (:attempt/id successor-close)
                      :successor-run/id (:run/id successor-close)
                      :successor-closed-at (:closed-at successor-close)
                      :successor-witness-ref (:witness-ref successor-close)
                      :successor-witness-sha256 (:witness-sha256 successor-close)
                      :decided-by (:decided-by authority)
                      :authority-review-job (:review-job authority)}
            resolution (resolve-fn
                        obligation
                        {:attempt-id (:attempt/id successor-close)
                         :commit (:commit repair-close)
                         :reviewer (:decided-by authority)
                         :review-job (:review-job authority)
                         :witness (:witness successor-close)
                         :validation {:kind :production-shaped-successor
                                      :production-shaped? true}
                         :successor-relation relation})]
        {:status :resolved :resolution resolution :relation relation}))))

(defn repair-derived-state
  "Pure cutoff readback of immutable finding plus optional resolution. A
  resolution after CUTOFF is excluded rather than projected backward."
  [repair-id cutoff finding resolution]
  (let [cutoff-at (parsed-instant cutoff)]
    (when-not (and cutoff-at (= repair-id (:repair/id finding)))
      (successor-refuse! :finding-readback-invalid
                         {:repair/id repair-id :cutoff cutoff}))
    (when (and resolution (not= repair-id (:repair/id resolution)))
      (successor-refuse! :resolution-readback-invalid
                         {:repair/id repair-id
                          :resolution-repair-id (:repair/id resolution)}))
    (let [resolution-at (some-> resolution :resolved-at parsed-instant)
          admitted? (and resolution
                         (= repair-id (:repair/id resolution))
                         resolution-at
                         (not (.isAfter resolution-at cutoff-at)))]
      {:schema :wm/repair-derived-state-v1
       :repair/id repair-id
       :cutoff cutoff
       :derived-status (if admitted? :resolved :open)
       :finding finding
       :resolution (when admitted? resolution)
       :derived-from (cond-> [:finding]
                       admitted? (conj :resolution))})))
