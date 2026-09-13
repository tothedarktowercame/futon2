(ns futon2.aif.machine-slow-feedback-store
  "Isolated tempfile implementation of the E6b revisioned state/application
   store. There is deliberately no production constructor."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io PushbackReader StringReader FileOutputStream)
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
(defn- form-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
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
  (let [back (strict-edn (form-bytes state) "state")]
    (when-not (= state back) (refuse! :e6b-store/state-unserializable {})))
  state)
(defn- valid-authority! [a]
  (when-not (and (= #{:verifier/source-sha256 :evidence-source-sha256s} (set (keys a)))
                 (re-matches hex64 (:verifier/source-sha256 a ""))
                 (map? (:evidence-source-sha256s a))
                 (every? keyword? (keys (:evidence-source-sha256s a)))
                 (every? #(re-matches hex64 %) (vals (:evidence-source-sha256s a))))
    (refuse! :e6b-store/authority-invalid {})) a)
(defn- roundtrip! [label x]
  (try
    (when-not (= x (strict-edn (form-bytes x) label))
      (refuse! :e6b-store/unserializable {:label label}))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (throw (ex-info "value is not strict EDN"
                      {:refusal :e6b-store/unserializable :label label} e))))
  x)
(defn- valid-application! [a]
  (when-not (and (= #{:application/id :feedback/event-id :transition/subject
                      :input/digests :output/digest :status} (set (keys a)))
                 (every? nonblank? ((juxt :application/id :feedback/event-id) a))
                 (map? (:transition/subject a)) (seq (:transition/subject a))
                 (map? (:input/digests a)) (seq (:input/digests a))
                 (every? #(re-matches hex64 %) (vals (:input/digests a)))
                 (re-matches hex64 (:output/digest a "")) (= :committed (:status a)))
    (refuse! :e6b-store/application-invalid {}))
  (roundtrip! :application a))
(defn- valid-proposal! [p]
  (let [{:keys [prior next application authority committed-at]} p]
    (when-not (and (= #{:prior :next :application :authority :committed-at} (set (keys p)))
                   (= #{:revision :transaction-sha256 :state-sha256} (set (keys prior)))
                   (= #{:revision :state} (set (keys next)))
                   (every? nonblank? [(:revision prior) (:revision next)])
                   (not= (:revision prior) (:revision next))
                   (every? #(re-matches hex64 %) [(:transaction-sha256 prior "")
                                                  (:state-sha256 prior "")])
                   (instant? committed-at))
      (refuse! :e6b-store/proposal-invalid {}))
    (valid-state! (:state next)) (valid-application! application) (valid-authority! authority)
    (roundtrip! :proposal p)))
(defn- state-view [tx]
  (if (= :wm/e6b-state-genesis-v1 (:schema tx))
    {:revision (:state/revision tx) :state-sha256 (:state-sha256 tx)}
    {:revision (get-in tx [:next :revision]) :state-sha256 (get-in tx [:next :state-sha256])}))

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
  (let [bs (form-bytes form) digest (sha256 bs) final (.resolve ^Path (:txdir store) (str digest ".edn"))
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
  (let [bs (form-bytes head) root ^Path (:root store)
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
  (let [head-r (read-form ^Path (:head store)) head (:form head-r)
        head-tx (:form (read-form (tx-path store (:transaction-sha256 head))))]
    (when-not (and (= #{:schema :store/id :generation :state/revision :state-sha256
                        :transaction-sha256 :application-index} (set (keys head)))
                   (= :wm/e6b-state-head-v1 (:schema head))
                   (= (:store-id store) (:store/id head))
                   (nat-int? (:generation head)) (re-matches hex64 (:transaction-sha256 head ""))
                   (re-matches hex64 (:state-sha256 head ""))
                   (nonblank? (:state/revision head)) (vector? (:application-index head))
                   (= (:generation head) (:generation head-tx))
                   (= {:revision (:state/revision head) :state-sha256 (:state-sha256 head)}
                      (state-view head-tx)))
      (refuse! :e6b-store/head-invalid {}))
    (loop [digest (:transaction-sha256 head) expected (:generation head)
           child-prior nil ids #{} events #{} priors #{} collected [] chain []]
      (let [r (read-form (tx-path store digest)) tx (:form r)]
        (when-not (= digest (:digest r)) (refuse! :e6b-store/object-digest-mismatch {:digest digest}))
        (when-not (and (= (:store-id store) (:store/id tx)) (= expected (:generation tx))
                       (or (nil? child-prior)
                           (= (select-keys child-prior [:revision :state-sha256]) (state-view tx))))
          (refuse! :e6b-store/chain-invalid {:digest digest}))
        (if (= :wm/e6b-state-genesis-v1 (:schema tx))
          (do
            (when-not (and (= #{:schema :store/id :generation :prior :state :state/revision
                                :state-sha256 :application :authority :committed-at}
                              (set (keys tx)))
                           (zero? expected) (= {} (:prior tx))
                           (nil? (:application tx))
                           (nonblank? (:state/revision tx)) (instant? (:committed-at tx))
                           (= (:state-sha256 tx) (sha256 (form-bytes (:state tx)))))
              (refuse! :e6b-store/genesis-invalid {}))
            (valid-state! (:state tx)) (valid-authority! (:authority tx)) (roundtrip! :genesis tx)
            (let [ordered (vec (reverse collected))]
              (when-not (= ordered (:application-index head))
                (refuse! :e6b-store/application-index-invalid {}))
              {:head head :head-digest (:digest head-r) :current head-tx
               :applications ordered :chain-digests (vec (reverse (conj chain digest)))}))
          (let [app (:application tx) id (:application/id app) event (:feedback/event-id app)
                prior-rev (get-in tx [:prior :revision])
                entry {:application/id id :feedback/event-id event
                       :prior-state/revision prior-rev :transaction-sha256 digest}]
            (when-not (and (= #{:schema :store/id :generation :prior :next :application
                                :authority :committed-at :proposal} (set (keys tx)))
                           (= :wm/e6b-state-transaction-v1 (:schema tx)) (pos-int? expected)
                           (= #{:revision :transaction-sha256 :state-sha256 :generation}
                              (set (keys (:prior tx))))
                           (= #{:revision :state :state-sha256} (set (keys (:next tx))))
                           (= (dec expected) (get-in tx [:prior :generation]))
                           (re-matches hex64 (get-in tx [:prior :transaction-sha256] ""))
                           (re-matches hex64 (get-in tx [:prior :state-sha256] ""))
                           (every? nonblank? [id event prior-rev (get-in tx [:next :revision])])
                           (not= prior-rev (get-in tx [:next :revision]))
                           (= (get-in tx [:next :state-sha256])
                              (sha256 (form-bytes (get-in tx [:next :state]))))
                           (= (:committed-at tx) (get-in tx [:proposal :committed-at]))
                           (= (:application tx) (get-in tx [:proposal :application]))
                           (= (:authority tx) (get-in tx [:proposal :authority]))
                           (= (dissoc (:prior tx) :generation) (get-in tx [:proposal :prior]))
                           (= (dissoc (:next tx) :state-sha256) (get-in tx [:proposal :next]))
                           (not (ids id)) (not (events event)) (not (priors prior-rev)))
              (refuse! :e6b-store/transaction-invalid {:digest digest}))
            (valid-proposal! (:proposal tx)) (roundtrip! :transaction tx)
            (recur (get-in tx [:prior :transaction-sha256]) (dec expected)
                   (:prior tx) (conj ids id) (conj events event) (conj priors prior-rev)
                   (conj collected entry) (conj chain digest))))))))

(defn initialize!
  [store {:keys [state revision authority committed-at] :as input}]
  (with-lock store
    (when (or (Files/exists ^Path (:head store) (make-array LinkOption 0))
              (seq (iterator-seq (.iterator (Files/newDirectoryStream ^Path (:txdir store))))))
      (refuse! :e6b-store/already-initialized-or-interrupted {}))
    (valid-state! state) (valid-authority! authority) (roundtrip! :genesis-input input)
    (when-not (and (= #{:state :revision :authority :committed-at} (set (keys input)))
                   (nonblank? revision) (instant? committed-at))
      (refuse! :e6b-store/genesis-input-invalid {}))
    (let [tx {:schema :wm/e6b-state-genesis-v1 :store/id (:store-id store) :generation 0
              :prior {} :state state :state/revision revision
              :state-sha256 (sha256 (form-bytes state)) :application nil
              :authority authority :committed-at committed-at}]
      (roundtrip! :genesis tx)
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
    (valid-proposal! proposal)
    (let [{:keys [head applications]} (validate-chain! store)
          existing (first (filter #(= (:application/id application) (:application/id %)) applications))]
      (if existing
        (let [old (:form (read-form (tx-path store (:transaction-sha256 existing))))]
          (if (= proposal (:proposal old))
            old
            (refuse! :e6b-store/application-conflict {:application/id (:application/id application)})))
        (do
      (when-not (and (= (:revision prior) (:state/revision head))
                     (= (:transaction-sha256 prior) (:transaction-sha256 head))
                     (= (:state-sha256 prior) (:state-sha256 head)))
        (refuse! :e6b-store/stale-prior {}))
      (when (or (some #(= (:feedback/event-id application) (:feedback/event-id %)) applications)
                (some #(= (:revision prior) (:prior-state/revision %)) applications))
        (refuse! :e6b-store/feedback-conflict {}))
      (let [generation (inc (:generation head))
            tx {:schema :wm/e6b-state-transaction-v1 :store/id (:store-id store)
                :generation generation :prior (assoc prior :generation (:generation head))
                :next (assoc next :state-sha256 (sha256 (form-bytes (:state next))))
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
          (catch Throwable e (reset! (:poisoned? store) true) (throw e)))))))))

(defn recover [store] (with-lock store (validate-chain! store)))
(defn capture [store]
  (with-lock store
    (let [{:keys [head head-digest applications chain-digests]} (validate-chain! store)
          objects (into {} (for [d chain-digests
                                 :let [bs (:bytes (read-form (tx-path store d)))]]
                             [d (aclone ^bytes bs)]))]
      {:schema :wm/e6b-store-capture-v1 :scope :isolated-test
       :store/id (:store-id store) :generation (:generation head)
       :head-digest head-digest :chain-digests chain-digests
       :application-universe applications :objects objects
       :completeness-authority :absent :local-chain-consistent? true
       :rollback-freshness? :unproved :restart-authorized? false})))
