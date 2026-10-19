(ns futon2.aif.meta-injury-store
  "Locked, atomic, one-form durable storage for the META injury ledger."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-observation :as injury]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal])
  (:import [java.io PushbackReader StringReader]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [java.nio.file Files Path StandardCopyOption StandardOpenOption]
           [java.nio.file.attribute FileAttribute]
           [java.util UUID]))

(def default-root "/home/joe/code/futon2/data/wm-meta-injuries")

(def eof-token (Object.))
(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))

(defn read-one-form
  "Total exact-one-EDN-form reader."
  [text]
  (try
    (with-open [reader (PushbackReader. (StringReader. text))]
      (let [value (edn/read {:eof eof-token} reader)
            trailing (edn/read {:eof eof-token} reader)]
        (cond
          (identical? eof-token value) (refusal :store/empty)
          (not (identical? eof-token trailing)) (refusal :store/multiple-forms)
          :else {:status :read :value value})))
    (catch Throwable t
      (refusal :store/malformed-edn {:detail (ex-message t)}))))

(defn load-ledger [path]
  (try
    (let [file (io/file path)]
      (if-not (.exists file)
        {:status :loaded :ledger ledger/empty-ledger :source :new}
        (let [read-result (read-one-form (slurp file))
              value (:value read-result)
              checked (when (= :read (:status read-result))
                        (ledger/validate-ledger value))]
          (cond
            (not= :read (:status read-result)) read-result
            (not= :valid (:status checked))
            (refusal :store/tampered-ledger {:defects (:defects checked)})
            :else {:status :loaded :ledger value :source :disk}))))
    (catch Throwable t
      (refusal :store/read-failed {:detail (ex-message t)}))))

(defn- force-write! [^Path path text]
  (with-open [channel (FileChannel/open
                       path (into-array StandardOpenOption
                                        [StandardOpenOption/WRITE
                                         StandardOpenOption/TRUNCATE_EXISTING]))]
    (let [bytes (.getBytes ^String text "UTF-8")]
      (.write channel (ByteBuffer/wrap bytes))
      (.force channel true))))

(defn- atomic-write! [path value]
  (let [target (.toPath (io/file path))
        parent (or (.getParent target) (.toPath (io/file ".")))
        _ (Files/createDirectories parent (make-array FileAttribute 0))
        temp (Files/createTempFile parent ".meta-injury-ledger-" ".edn"
                                   (make-array FileAttribute 0))]
    (try
      (force-write! temp (str (pr-str value) "\n"))
      (Files/move temp target
                  (into-array StandardCopyOption
                              [StandardCopyOption/ATOMIC_MOVE
                               StandardCopyOption/REPLACE_EXISTING]))
      ;; Persist the directory entry as well as the file payload.
      (with-open [directory-channel
                  (FileChannel/open parent
                                    (into-array StandardOpenOption
                                                [StandardOpenOption/READ]))]
        (.force directory-channel true))
      (finally (Files/deleteIfExists temp)))))

(defn transact-classification!
  "Lock, validate, integrate, and atomically replace the one-form ledger.
  Duplicate replay is byte-preserving: no rewrite occurs."
  [path classification]
  (try
    (let [lock-path (str path ".lock")
          lock-file (io/file lock-path)]
      (when-let [parent (.getParentFile lock-file)] (.mkdirs parent))
      (with-open [channel (FileChannel/open
                           (.toPath lock-file)
                           (into-array StandardOpenOption
                                       [StandardOpenOption/CREATE
                                        StandardOpenOption/WRITE]))
                  _lock (.lock channel)]
        (let [loaded (load-ledger path)]
          (if-not (= :loaded (:status loaded))
            loaded
            (let [result (terminal/integrate (:ledger loaded) classification)]
              (if (= :appended (:status result))
                (do (atomic-write! path (:ledger result))
                    (assoc result :durability :atomic-locked-one-form))
                result))))))
    (catch Throwable t
      (refusal :store/transaction-failed {:detail (ex-message t)}))))

(defn ingest-record-file!
  "Pin exact record bytes, require exactly one EDN form, classify, then perform
  the locked atomic transaction."
  [ledger-path record-path]
  (try
    (let [bytes (Files/readAllBytes (.toPath (io/file record-path)))
          parsed (read-one-form (String. bytes "UTF-8"))]
      (if-not (= :read (:status parsed))
        parsed
        (let [pin {:path record-path :sha256 (load-identity/sha256 bytes)}
              classification (terminal/classify (:value parsed) pin)]
          (if-not (= :classified (:status classification))
            classification
            (transact-classification! ledger-path classification)))))
    (catch Throwable t
      (refusal :store/record-ingest-failed {:detail (ex-message t)}))))

;; Compatibility boundary for the pre-ledger observation route. Packet 2 does
;; not use HEAD.edn to derive active injuries, but existing callers remain
;; byte-for-byte compatible until the later quarantine packet.
(defn- read-bytes [path] (Files/readAllBytes (.toPath (io/file path))))
(defn- legacy-atomic-write! [target text]
  (let [target (io/file target)
        tmp (io/file (.getParentFile target)
                     (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
    (io/make-parents target)
    (spit tmp text)
    (Files/move (.toPath tmp) (.toPath target)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    (.getCanonicalPath target)))

(defn deposit!
  "Legacy observation deposit retained for compatibility. It is not an
  authority for :wm/meta-injury-ledger-v1 active state."
  ([run-record-path] (deposit! default-root run-record-path))
  ([root run-record-path]
   (let [record-bytes (read-bytes run-record-path)
         record (edn/read-string (String. record-bytes "UTF-8"))
         run-pin {:path (.getCanonicalPath (io/file run-record-path))
                  :sha256 (load-identity/sha256 record-bytes)}
         projection (injury/project-run-record record run-pin)
         projection-path (io/file root (str (:run-id projection) ".edn"))
         projection-path (legacy-atomic-write! projection-path
                                               (str (pr-str projection) "\n"))
         projection-bytes (read-bytes projection-path)
         source-pin {:path projection-path
                     :sha256 (load-identity/sha256 projection-bytes)}
         authority {:schema :wm/meta-injury-authority-v1
                    :source-pin source-pin :run-record-pin run-pin}
         observation (injury/produce {:source-bytes projection-bytes
                                      :expected-source-pin source-pin
                                      :run-record-bytes record-bytes
                                      :expected-run-record-pin run-pin})
         head (assoc authority :status (:status observation)
                     :capability (:capability observation)
                     :run-id (:run-id projection))
         head-path (legacy-atomic-write! (io/file root "HEAD.edn")
                                         (str (pr-str head) "\n"))]
     (assoc head :head-path head-path :observation observation))))

(defn load-authority
  "Legacy pinned observation authority; never consulted by ledger active-set."
  ([] (load-authority default-root))
  ([root]
   (let [head-file (io/file root "HEAD.edn")]
     (when (.isFile head-file)
       (let [{:keys [source-pin run-record-pin]}
             (edn/read-string (slurp head-file))]
         {:source-bytes (read-bytes (:path source-pin))
          :expected-source-pin source-pin
          :run-record-bytes (read-bytes (:path run-record-pin))
          :expected-run-record-pin run-record-pin})))))
