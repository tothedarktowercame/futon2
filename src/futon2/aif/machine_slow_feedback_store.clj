(ns futon2.aif.machine-slow-feedback-store
  "Isolated tempfile implementation of the E6b revisioned state/application
   store. There is deliberately no production constructor."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io PushbackReader StringReader FileOutputStream OutputStreamWriter BufferedWriter)
           (java.nio ByteBuffer)
           (java.nio.channels FileChannel OverlappingFileLockException)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path StandardCopyOption StandardOpenOption)
           (java.security MessageDigest)
           (java.time Instant)
           (java.util.concurrent.locks ReentrantLock)))

(def ^:dynamic *stage-hook* nil)
(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- instant? [x] (try (Instant/parse x) true (catch Throwable _ false)))

(defn- strict-edn [^bytes bs path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
          eof (Object.) form (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
      (when (or (identical? eof form) (not (identical? eof tail)))
        (refuse! :e6b-store/invalid-edn-cardinality {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e (throw (ex-info "invalid strict UTF-8 EDN"
                                       {:refusal :e6b-store/invalid-edn :path (str path)} e)))))

(defn- read-form [^Path path]
  (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
    (refuse! :e6b-store/missing-object {:path (str path)}))
  (let [bs (Files/readAllBytes path)] {:bytes bs :digest (sha256 bs) :form (strict-edn bs path)}))
(defn- force-dir! [^Path p]
  (with-open [ch (FileChannel/open p (into-array StandardOpenOption [StandardOpenOption/READ]))]
    (.force ch true)))
(defn- write-sync! [^Path p ^bytes bs]
  (with-open [stream (FileOutputStream. (.toFile p))]
    (.write stream bs) (.flush stream) (.sync (.getFD stream))))

(defn- valid-state! [state]
  (when-not (and (map? state) (seq state))
    (refuse! :e6b-store/state-invalid {}))
  (let [back (strict-edn (bytes state) "state")]
    (when-not (= state back) (refuse! :e6b-store/state-unserializable {})))
  state)
(defn- valid-authority! [a]
  (when-not (and (map? a) (re-matches hex64 (:verifier/source-sha256 a ""))
                 (map? (:evidence-source-sha256s a))
                 (every? #(re-matches hex64 %) (vals (:evidence-source-sha256s a))))
    (refuse! :e6b-store/authority-invalid {})) a)

(defmacro ^:private with-lock [store & body]
  `(let [^ReentrantLock l# (:lock ~store)]
     (.lock l#)
     (try
       (when @(:released? ~store) (refuse! :e6b-store/owner-released {}))
       (when @(:poisoned? ~store) (refuse! :e6b-store/owner-poisoned {}))
       ~@body
       (finally (.unlock l#)))))

(defn isolated-store
  "Acquire an isolated tempfile root for the lifetime of the returned owner."
  [root store-id]
  (when-not (and (nonblank? store-id) (Files/isDirectory (.toPath (io/file root)) (make-array LinkOption 0)))
    (refuse! :e6b-store/isolated-config-invalid {}))
  (let [base (.toAbsolutePath (.normalize (.toPath (io/file root))))
        txdir (.resolve base "transactions")
        _ (Files/createDirectories txdir (make-array java.nio.file.attribute.FileAttribute 0))
        lock-path (.resolve base "owner.lock")
        channel (FileChannel/open lock-path (into-array StandardOpenOption
                                                        [StandardOpenOption/CREATE StandardOpenOption/WRITE]))
        lease (try (.tryLock channel) (catch OverlappingFileLockException _ nil))]
    (when-not lease (.close channel) (refuse! :e6b-store/already-owned {:root (str base)}))
    {:scope :isolated-test :root base :txdir txdir :head (.resolve base "HEAD.edn")
     :store-id store-id :channel channel :lease lease :lock (ReentrantLock.)
     :released? (atom false) :poisoned? (atom false)}))

(defn release! [store]
  (let [^ReentrantLock l (:lock store)]
    (.lock l)
    (try
      (when-not @(:released? store)
        (.release ^java.nio.channels.FileLock (:lease store))
        (.close ^FileChannel (:channel store))
        (reset! (:released? store) true))
      true (finally (.unlock l)))))

(defn- publish-object! [store form]
  (let [bs (bytes form) digest (sha256 bs) final (.resolve ^Path (:txdir store) (str digest ".edn"))
        tmp (Files/createTempFile ^Path (:txdir store) ".transaction-" ".tmp"
                                  (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (write-sync! tmp bs)
      (when *stage-hook* (*stage-hook* :transaction-synced store))
      (try
        (Files/createLink final tmp)
        (catch java.nio.file.FileAlreadyExistsException _
          (let [existing (read-form final)]
            (when-not (java.util.Arrays/equals bs ^bytes (:bytes existing))
              (refuse! :e6b-store/digest-object-conflict {:digest digest}))))
        (catch UnsupportedOperationException e
          (throw (ex-info "atomic no-overwrite publication unsupported"
                          {:refusal :e6b-store/no-overwrite-unsupported} e))))
      (force-dir! ^Path (:txdir store))
      (when *stage-hook* (*stage-hook* :transaction-published store))
      digest
      (finally (Files/deleteIfExists tmp)))))

(defn- write-head! [store head]
  (let [bs (bytes head) root ^Path (:root store)
        tmp (Files/createTempFile root ".head-" ".tmp"
                                  (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (write-sync! tmp bs)
      (when *stage-hook* (*stage-hook* :head-synced store))
      (Files/move tmp ^Path (:head store)
                  (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE
                                                  StandardCopyOption/REPLACE_EXISTING]))
      (when *stage-hook* (*stage-hook* :head-renamed store))
      (force-dir! root)
      (when *stage-hook* (*stage-hook* :head-durable store))
      head
      (finally (Files/deleteIfExists tmp)))))

(defn- tx-path [store digest] (.resolve ^Path (:txdir store) (str digest ".edn")))
(defn- validate-chain! [store]
  (let [head-r (read-form ^Path (:head store)) head (:form head-r)]
    (when-not (and (= :wm/e6b-state-head-v1 (:schema head))
                   (= (:store-id store) (:store/id head))
                   (nat-int? (:generation head)) (re-matches hex64 (:transaction-sha256 head ""))
                   (vector? (:application-index head)))
      (refuse! :e6b-store/head-invalid {}))
    (loop [digest (:transaction-sha256 head) expected (:generation head)
           ids #{} events #{} priors #{} collected []]
      (let [r (read-form (tx-path store digest)) tx (:form r)]
        (when-not (= digest (:digest r)) (refuse! :e6b-store/object-digest-mismatch {:digest digest}))
        (when-not (and (= (:store-id store) (:store/id tx)) (= expected (:generation tx)))
          (refuse! :e6b-store/chain-invalid {:digest digest}))
        (if (= :wm/e6b-state-genesis-v1 (:schema tx))
          (do
            (when-not (and (zero? expected) (= #{} (set (keys (:prior tx))))
                           (nil? (:application tx))
                           (= (:state-sha256 tx) (sha256 (bytes (:state tx)))))
              (refuse! :e6b-store/genesis-invalid {}))
            (let [ordered (vec (reverse collected))]
              (when-not (= ordered (:application-index head))
                (refuse! :e6b-store/application-index-invalid {}))
              {:head head :head-digest (:digest head-r) :current tx
               :applications ordered}))
          (let [app (:application tx) id (:application/id app) event (:feedback/event-id app)
                prior-rev (get-in tx [:prior :revision])
                entry {:application/id id :feedback/event-id event
                       :prior-state/revision prior-rev :transaction-sha256 digest}]
            (when-not (and (= :wm/e6b-state-transaction-v1 (:schema tx)) (pos-int? expected)
                           (= (dec expected) (get-in tx [:prior :generation]))
                           (every? nonblank? [id event prior-rev (get-in tx [:next :revision])])
                           (= (get-in tx [:next :state-sha256])
                              (sha256 (bytes (get-in tx [:next :state]))))
                           (not (ids id)) (not (events event)) (not (priors prior-rev)))
              (refuse! :e6b-store/transaction-invalid {:digest digest}))
            (recur (get-in tx [:prior :transaction-sha256]) (dec expected)
                   (conj ids id) (conj events event) (conj priors prior-rev)
                   (conj collected entry))))))))

(defn initialize!
  [store {:keys [state revision authority committed-at]}]
  (with-lock store
    (when (or (Files/exists ^Path (:head store) (make-array LinkOption 0))
              (seq (iterator-seq (.iterator (Files/newDirectoryStream ^Path (:txdir store))))))
      (refuse! :e6b-store/already-initialized-or-interrupted {}))
    (valid-state! state) (valid-authority! authority)
    (when-not (and (nonblank? revision) (instant? committed-at))
      (refuse! :e6b-store/genesis-input-invalid {}))
    (let [tx {:schema :wm/e6b-state-genesis-v1 :store/id (:store-id store) :generation 0
              :prior {} :state state :state/revision revision
              :state-sha256 (sha256 (bytes state)) :application nil
              :authority authority :committed-at committed-at}]
      (try
        (let [digest (publish-object! store tx)
              head {:schema :wm/e6b-state-head-v1 :store/id (:store-id store) :generation 0
                    :state/revision revision :state-sha256 (:state-sha256 tx)
                    :transaction-sha256 digest :application-index []}]
          (write-head! store head) (validate-chain! store))
        (catch Throwable e (reset! (:poisoned? store) true) (throw e))))))

(defn compare-and-commit!
  [store {:keys [prior next application authority committed-at] :as proposal}]
  (with-lock store
    (let [{:keys [head applications]} (validate-chain! store)
          existing (first (filter #(= (:application/id application) (:application/id %)) applications))]
      (if existing
        (let [old (:form (read-form (tx-path store (:transaction-sha256 existing))))]
          (if (= proposal (:proposal old))
            old
            (refuse! :e6b-store/application-conflict {:application/id (:application/id application)}))))
        (do
      (when-not (and (= (:revision prior) (:state/revision head))
                     (= (:transaction-sha256 prior) (:transaction-sha256 head))
                     (= (:state-sha256 prior) (:state-sha256 head)))
        (refuse! :e6b-store/stale-prior {}))
      (when (or (some #(= (:feedback/event-id application) (:feedback/event-id %)) applications)
                (some #(= (:revision prior) (:prior-state/revision %)) applications))
        (refuse! :e6b-store/feedback-conflict {}))
      (valid-state! (:state next)) (valid-authority! authority)
      (when-not (and (every? nonblank? [(:revision next) (:application/id application)
                                        (:feedback/event-id application) committed-at])
                     (instant? committed-at) (= :committed (:status application)))
        (refuse! :e6b-store/proposal-invalid {}))
      (let [generation (inc (:generation head))
            tx {:schema :wm/e6b-state-transaction-v1 :store/id (:store-id store)
                :generation generation :prior (assoc prior :generation (:generation head))
                :next (assoc next :state-sha256 (sha256 (bytes (:state next))))
                :application application :authority authority :committed-at committed-at
                :proposal proposal}]
        (try
          (let [digest (publish-object! store tx)
                entry {:application/id (:application/id application)
                       :feedback/event-id (:feedback/event-id application)
                       :prior-state/revision (:revision prior) :transaction-sha256 digest}
                head' {:schema :wm/e6b-state-head-v1 :store/id (:store-id store)
                       :generation generation :state/revision (:revision next)
                       :state-sha256 (get-in tx [:next :state-sha256])
                       :transaction-sha256 digest :application-index (conj applications entry)}]
            (write-head! store head') tx)
          (catch Throwable e (reset! (:poisoned? store) true) (throw e))))))))

(defn recover [store] (with-lock store (validate-chain! store)))
(defn capture [store]
  (with-lock store
    (let [{:keys [head head-digest applications]} (validate-chain! store)
          digests (into [(:transaction-sha256 head)] (map :transaction-sha256 applications))
          objects (into {} (for [d (distinct digests)
                                 :let [bs (:bytes (read-form (tx-path store d)))]]
                             [d (aclone ^bytes bs)]))]
      {:schema :wm/e6b-store-capture-v1 :scope :isolated-test
       :store/id (:store-id store) :generation (:generation head)
       :head-digest head-digest :application-universe applications :objects objects
       :completeness-authority :absent :local-chain-consistent? true
       :rollback-freshness? :unproved :restart-authorized? false})))
