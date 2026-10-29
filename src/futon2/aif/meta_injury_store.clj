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

;; Live store root, fresh from 2026-10-22 (Joe: the pre-2026-10-22 historical
;; record is ignored and archived in place under data/, with an ARCHIVED.md
;; pointing here). All live meta-injury defaults (deposit root, ledger,
;; transaction, diagnosis, acceptance) resolve under this single definition.
(def default-root
  (or (System/getenv "FUTON_WM_META_INJURY_ROOT")
      "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22"))

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

(defn load-ledger
  ([path] (load-ledger path (constantly false)))
  ([path authority-verifier]
   (try
     (let [file (io/file path)]
       (if-not (.exists file)
         {:status :loaded :ledger ledger/empty-ledger :source :new}
         (let [read-result (read-one-form (slurp file))
               value (:value read-result)
               checked (when (= :read (:status read-result))
                         (ledger/validate-ledger value authority-verifier))]
           (cond
             (not= :read (:status read-result)) read-result
             (not= :valid (:status checked))
             (refusal :store/tampered-ledger {:defects (:defects checked)})
             :else {:status :loaded :ledger value :source :disk}))))
     (catch Throwable t
       (refusal :store/read-failed {:detail (ex-message t)})))))

(defn- load-durable-ledger* [path authority-verifier]
   (try
     (let [file (io/file path)]
       (if-not (.isFile file)
         (refusal :store/durable-ledger-required)
         ;; Parse, validate, and pin one byte snapshot. A second path read here
         ;; could return a ledger different from the bytes named by source-pin.
         (let [bytes (Files/readAllBytes (.toPath file))
               read-result (read-one-form (String. bytes "UTF-8"))
               value (:value read-result)
               checked (when (= :read (:status read-result))
                         (ledger/validate-ledger value authority-verifier))]
           (cond
             (not= :read (:status read-result)) read-result
             (not= :valid (:status checked))
             (refusal :store/tampered-ledger {:defects (:defects checked)})
             :else {:schema :wm/durable-meta-injury-ledger-v1
                    :status :loaded
                    :source-pin {:path path :sha256 (load-identity/sha256 bytes)}
                    :ledger value}))))
     (catch Throwable t
       (refusal :store/durable-load-failed {:detail (ex-message t)}))))

(defn load-durable-ledger
  "Load a disk-backed ledger with an exact source pin for coordinator input."
  ([path] (load-durable-ledger* path (constantly false)))
  ([path authority-verifier] (load-durable-ledger* path authority-verifier)))

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

(defn persist-ledger!
  ([path value] (persist-ledger! path value (constantly false)))
  ([path value authority-verifier]
  (try
    (let [checked (ledger/validate-ledger value authority-verifier)]
      (if-not (= :valid (:status checked))
        (refusal :store/invalid-ledger {:defects (:defects checked)})
        (do (atomic-write! path value)
            {:status :persisted :path path :ledger-pin (:ledger-pin value)
             :durability :atomic-one-form})))
    (catch Throwable t
      (refusal :store/persist-failed {:detail (ex-message t)})))))

(defn transact-classification!
  "Lock, validate, integrate, and atomically replace the one-form ledger.
  Duplicate replay is byte-preserving: no rewrite occurs."
  ([path classification] (transact-classification! path classification (constantly false)))
  ([path classification authority-verifier]
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
        (let [loaded (load-ledger path authority-verifier)]
          (if-not (= :loaded (:status loaded))
            loaded
            (let [result (terminal/integrate (:ledger loaded) classification
                                             authority-verifier)]
              (if (= :appended (:status result))
                (let [checked (ledger/validate-ledger (:ledger result) authority-verifier)]
                  (if-not (= :valid (:status checked))
                    (refusal :store/invalid-ledger {:defects (:defects checked)})
                    (do (atomic-write! path (:ledger result))
                        (assoc result :durability :atomic-locked-one-form))))
                result))))))
    (catch Throwable t
      (refusal :store/transaction-failed {:detail (ex-message t)})))))

(defn transact-medic-disposition!
  "Lock an existing durable ledger, apply one fully supplied medic operation,
  revalidate with the same external authority verifier, and atomically replace
  the one-form file only after an admitted append. Missing files never denote
  an empty ledger. Exact persisted replay is delegated to
  append-medic-repaired and remains byte-preserving before stale-root checks."
  ([path record-pin operation authority-verifier]
   (transact-medic-disposition! path record-pin operation nil authority-verifier))
  ([path record-pin operation attribution authority-verifier]
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
        (let [file (io/file path)]
          (if-not (.isFile file)
            (refusal :store/durable-ledger-required)
            (let [read-result (read-one-form (slurp file))
                  value (:value read-result)
                  checked (when (= :read (:status read-result))
                            (ledger/validate-ledger value authority-verifier))]
              (cond
                (not= :read (:status read-result)) read-result
                (not= :valid (:status checked))
                (refusal :store/tampered-ledger {:defects (:defects checked)})
                :else
                (let [result (ledger/append-medic-repaired
                              value record-pin operation attribution authority-verifier)]
                  (if-not (= :appended (:status result))
                    result
                    (let [result-check (ledger/validate-ledger
                                        (:ledger result) authority-verifier)]
                      (if-not (= :valid (:status result-check))
                        (refusal :store/invalid-medic-result
                                 {:defects (:defects result-check)})
                        (do (atomic-write! path (:ledger result))
                            (assoc result
                                   :durability :atomic-locked-one-form))))))))))))
    (catch java.nio.channels.OverlappingFileLockException _
      (refusal :store/lock-contended))
    (catch Throwable t
      (refusal :store/medic-transaction-failed {:detail (ex-message t)})))))

(defn ingest-record-file!
  "Pin exact record bytes, require exactly one EDN form, classify, then perform
  the locked atomic transaction."
  ([ledger-path record-path]
   (ingest-record-file! ledger-path record-path (constantly false)))
  ([ledger-path record-path authority-verifier]
  (try
    (let [bytes (Files/readAllBytes (.toPath (io/file record-path)))
          parsed (read-one-form (String. bytes "UTF-8"))]
      (if-not (= :read (:status parsed))
        parsed
        (let [pin {:path record-path :sha256 (load-identity/sha256 bytes)}
              classification (terminal/classify (:value parsed) pin)]
          (if-not (= :classified (:status classification))
            classification
            (transact-classification! ledger-path classification authority-verifier)))))
    (catch Throwable t
      (refusal :store/record-ingest-failed {:detail (ex-message t)})))))

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
   (try
     (let [head-file (io/file root "HEAD.edn")]
       (when (.isFile head-file)
         (let [{:keys [source-pin run-record-pin]}
               (edn/read-string (slurp head-file))]
           (when (and (.isFile (io/file (:path source-pin)))
                      (.isFile (io/file (:path run-record-pin))))
             {:source-bytes (read-bytes (:path source-pin))
              :expected-source-pin source-pin
              :run-record-bytes (read-bytes (:path run-record-pin))
              :expected-run-record-pin run-record-pin}))))
     (catch Throwable _ nil))))
