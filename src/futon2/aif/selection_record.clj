(ns futon2.aif.selection-record
  "Immutable outer decisions. Completion is a separate receipt: a tick's decision
  reference never changes. The append-only index orders decisions by start time,
  including refusals that never entered the inner runner. Readers verify bytes."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity])
  (:import [java.nio.file Files StandardCopyOption]
           [java.time Instant]
           [java.util UUID]))

(def schema :wm/selection-record-v1)
(def index-name "selection-record-index.edn")
(defn file-sha [file] (identity/sha256 (Files/readAllBytes (.toPath (io/file file)))))
(defn- read-edn [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (let [x (edn/read {:eof ::eof} r)]
      (when-not (= ::eof (edn/read {:eof ::eof} r))
        (throw (ex-info "Trailing selection record data" {:kind :selection-record/trailing-data})))
      x)))
(defn read-reference [{:keys [path sha256]}]
  (when-not (and path sha256 (.isFile (io/file path)) (= sha256 (file-sha path)))
    (throw (ex-info "Selection reference does not match bytes"
                    {:kind :selection-record/reference-invalid :path path})))
  (let [r (read-edn path)]
    (when-not (= schema (:schema r))
      (throw (ex-info "Not a selection record" {:kind :selection-record/schema-invalid :path path})))
    r))

(defn read-chain [terminal]
  (loop [ref terminal seen #{} entries []]
    (when (contains? seen (:sha256 ref))
      (throw (ex-info "Selection decision cycle" {:kind :selection-record/chain-invalid})))
    (let [r (read-reference ref) parent (:supersedes-record r)
          entries (conj entries {:reference ref :record r})]
      (if parent
        (let [p (read-reference parent)]
          (when-not (and (= (:supersedes r) (:sha256 parent))
                         (= (select-keys r [:run/id :click/id :startedAt])
                            (select-keys p [:run/id :click/id :startedAt]))
                         (= (:chain-index r) (inc (:chain-index p))))
            (throw (ex-info "Broken selection decision chain" {:kind :selection-record/chain-invalid})))
          (recur parent (conj seen (:sha256 ref)) entries))
        (do
          (when-not (= 1 (:chain-index r))
            (throw (ex-info "Missing selection root" {:kind :selection-record/chain-invalid})))
          {:root ref :terminal terminal :length (count entries) :entries (vec (reverse entries))})))))

(defn- write-new! [file value]
  (io/make-parents file)
  (let [bytes (.getBytes (str (pr-str value) "\n") "UTF-8")
        tmp (io/file (str file "." (UUID/randomUUID) ".tmp"))]
    (try
      (with-open [out (io/output-stream tmp)] (.write out bytes))
      ;; No REPLACE_EXISTING: repeated identities cannot overwrite a decision.
      (Files/move (.toPath tmp) (.toPath (io/file file)) (make-array StandardCopyOption 0))
      {:path (.getCanonicalPath (io/file file)) :sha256 (identity/sha256 bytes)}
      (finally (Files/deleteIfExists (.toPath tmp))))))

(defn write-decision!
  [dir {:keys [run-id startedAt click-id selection-predecessor selection-cause]} section outcome]
  (when-not (and (string? run-id) (re-matches #"[A-Za-z0-9._-]+" run-id)
                 (or (nil? click-id) (and (string? click-id) (re-matches #"[A-Za-z0-9._-]+" click-id))))
    (throw (ex-info "Unsafe selection run identity" {:kind :selection-record/run-id-invalid})))
  (let [at (str (Instant/parse startedAt))
        parent (when selection-predecessor (read-reference selection-predecessor))
        chain-index (if parent (inc (:chain-index parent)) 1)
        _ (when (and parent (not= [run-id at] [(:run/id parent) (:startedAt parent)]))
            (throw (ex-info "Foreign selection parent" {:kind :selection-record/chain-invalid})))
        file (io/file dir (str "selection-record-" (subs at 0 10) "-" (or click-id run-id)
                               (when parent (str "-decision-" chain-index)) ".edn"))
        record (cond-> {:schema schema :run/id run-id :click/id (or click-id run-id)
                :startedAt at :outer/selection section :selection-outcome outcome
                :pins (select-keys section [:giraffe/run-id :giraffe/manifest-sha256 :makeup/sha256])
                :focus (:focus section) :chain-index chain-index}
                 parent (assoc :supersedes (:sha256 selection-predecessor)
                               :supersedes-record selection-predecessor :cause selection-cause))
        ref (write-new! file record)]
    (spit (io/file dir index-name)
          (str (pr-str {:run/id run-id :startedAt at :chain-index chain-index :reference ref}) "\n") :append true)
    ref))

(defn complete!
  "No decision is restated. Tick absence is typed; it never means work succeeded."
  [ref result]
  (let [tick (:run-record result)
        receipt {:schema :wm/selection-completion-v1 :selection-record ref
                 :selection-root (:root (read-chain ref))
                 :tick-record (if (and tick (.isFile (io/file tick)))
                                {:path (str tick) :sha256 (file-sha tick)}
                                {:status :absent :reason :work-record-not-produced})
                 :end-status (or (:outcome result) (:status result) (:absent result)
                                 :returned-without-status)}]
    (write-new! (str (get-in receipt [:selection-root :path]) ".completion.edn") receipt)))

(defn read-completion
  "A completion refers to this exact decision; any tick bytes must still match."
  [ref]
  (let [root (:root (read-chain ref))
        file (io/file (str (:path root) ".completion.edn"))]
    (when (.isFile file)
      (let [r (read-edn file) tick (:tick-record r)]
        (when-not (and (= :wm/selection-completion-v1 (:schema r))
                       (= root (:selection-root r) (:root (read-chain (:selection-record r))))
                       (some #(= ref (:reference %)) (:entries (read-chain (:selection-record r))))
                       (or (= {:status :absent :reason :work-record-not-produced} tick)
                           (and (:path tick) (:sha256 tick) (.isFile (io/file (:path tick)))
                                (= (:sha256 tick) (file-sha (:path tick))))))
          (throw (ex-info "Invalid selection completion" {:kind :selection-record/completion-invalid})))
        r))))

(defn latest
  "Latest indexed decision strictly preceding this start, never latest filename.
  Invalid index, ties, missing or changed bytes refuse; they do not skip backwards."
  [dir run-id started-at]
  (let [index (io/file dir index-name)
        decisions (set (for [f (.listFiles (io/file dir))
                             :let [n (.getName f)]
                             :when (and (re-matches #"selection-record-\d{4}-\d{2}-\d{2}-.+\.edn" n)
                                        (not (str/ends-with? n ".completion.edn"))
                                        (not (str/ends-with? n ".requirements.edn")))]
                         (.getCanonicalPath f)))]
    (if-not (.isFile index)
      (if (seq decisions) {:status :not-recomputable :reason :selection-index-missing}
          {:status :absent :reason :no-selection-record-history})
      (try
        (let [now (Instant/parse started-at)
              events (mapv edn/read-string (remove str/blank? (str/split-lines (slurp index))))
              _ (when-not (every? (set (map #(get-in % [:reference :path]) events)) decisions)
                  (throw (ex-info "Unindexed selection decision" {:kind :selection-record/index-incomplete})))
              prior (filterv #(and (not= run-id (:run/id %))
                                   (.isBefore (Instant/parse (:startedAt %)) now)) events)
              ordered (sort-by #(vector (Instant/parse (:startedAt %)) (:chain-index %)) prior)
              last-event (last ordered)]
          (if-not last-event
            {:status :absent :reason :no-prior-selection}
            (if (> (count (set (map :run/id (filter #(= (:startedAt last-event) (:startedAt %)) prior)))) 1)
              {:status :not-recomputable :reason :selection-predecessor-time-tie}
              (let [ref (:reference last-event) record (read-reference ref)
                    chain (read-chain ref)
                    events (filter #(= (:run/id last-event) (:run/id %)) prior)]
                (when-not (= (mapv :reference (sort-by :chain-index events))
                             (mapv :reference (:entries chain)))
                  (throw (ex-info "Selection index is forked or incomplete" {:kind :selection-record/chain-invalid})))
                (when-not (= (select-keys last-event [:run/id :startedAt])
                             (select-keys record [:run/id :startedAt]))
                  (throw (ex-info "Selection index identity mismatch" {})))
                {:status :present :record record :reference ref :chain-length (:length chain)}))))
        (catch Exception e
          {:status :not-recomputable :reason :selection-history-invalid
           :detail (.getMessage e)})))))
