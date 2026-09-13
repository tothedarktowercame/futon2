(ns futon2.aif.machine-slow-feedback-store-v2
  "Isolated-only E6b store-v2.  It durably binds each transaction to a
   revalidated structural provenance artifact.  No production constructor or
   authority upgrade is provided."
  (:require [clojure.edn :as edn]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-feedback-store :as legacy])
  (:import (java.io FileOutputStream PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.channels FileChannel)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path StandardCopyOption StandardOpenOption)
           (java.security MessageDigest)
           (java.util Base64 Arrays)
           (java.util.concurrent.locks ReentrantLock)))

(def ^:dynamic *stage-hook* nil)
(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data] (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- form-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn- strict-edn [^bytes bs path]
  (try
    (let [d (doto (.newDecoder StandardCharsets/UTF_8)
              (.onMalformedInput CodingErrorAction/REPORT)
              (.onUnmappableCharacter CodingErrorAction/REPORT))
          r (PushbackReader. (StringReader. (str (.decode d (ByteBuffer/wrap bs)))))
          eof (Object.) x (edn/read {:eof eof} r) tail (edn/read {:eof eof} r)]
      (when (or (identical? eof x) (not (identical? eof tail)))
        (refuse! :e6b-store-v2/invalid-edn-cardinality {:path (str path)}))
      x)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e (throw (ex-info "invalid UTF-8 EDN"
                                       {:refusal :e6b-store-v2/invalid-edn :path (str path)} e)))))
(defn- read-object [^Path p]
  (when-not (Files/isRegularFile p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
    (refuse! :e6b-store-v2/missing-object {:path (str p)}))
  (let [bs (Files/readAllBytes p)] {:bytes bs :sha256 (sha256 bs) :record (strict-edn bs p)}))
(defn- write-sync! [^Path p ^bytes bs]
  (with-open [out (FileOutputStream. (.toFile p))]
    (.write out bs) (.flush out) (.sync (.getFD out))))
(defn- force-dir! [^Path p]
  (with-open [ch (FileChannel/open p (into-array StandardOpenOption [StandardOpenOption/READ]))]
    (.force ch true)))
(defn- publish! [store kind ^Path dir record]
  (let [bs (form-bytes record) digest (sha256 bs) final (.resolve dir (str digest ".edn"))
        tmp (Files/createTempFile dir (str "." (name kind) "-") ".tmp"
                                  (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (write-sync! tmp bs)
      (when *stage-hook* (*stage-hook* (keyword (str (name kind) "-synced")) store))
      (try (Files/createLink final tmp)
           (catch java.nio.file.FileAlreadyExistsException _
             (when-not (Arrays/equals bs ^bytes (:bytes (read-object final)))
               (refuse! :e6b-store-v2/object-conflict {:kind kind :digest digest})))
           (catch UnsupportedOperationException e
             (throw (ex-info "no-overwrite unavailable"
                             {:refusal :e6b-store-v2/no-overwrite-unsupported} e))))
      (force-dir! dir)
      (when *stage-hook* (*stage-hook* (keyword (str (name kind) "-published")) store))
      digest
      (finally (Files/deleteIfExists tmp)))))
(defn- write-head! [store head]
  (let [root ^Path (:root store) tmp (Files/createTempFile root ".head-v2-" ".tmp"
                                                               (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (write-sync! tmp (form-bytes head))
      (when *stage-hook* (*stage-hook* :head-synced store))
      (Files/move tmp ^Path (:head store)
                  (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE
                                                  StandardCopyOption/REPLACE_EXISTING]))
      (when *stage-hook* (*stage-hook* :head-renamed store))
      (force-dir! root)
      (when *stage-hook* (*stage-hook* :head-durable store))
      head
      (finally (Files/deleteIfExists tmp)))))

(defmacro ^:private with-owner [store & body]
  `(let [^ReentrantLock lock# (:lock ~store)]
     (.lock lock#)
     (try
       (when @(:released? ~store) (refuse! :e6b-store-v2/owner-released {}))
       (when @(:poisoned? ~store) (refuse! :e6b-store-v2/owner-poisoned {}))
       ~@body
       (finally (.unlock lock#)))))

(defn isolated-store
  "Acquire a fresh isolated store-v2 owner. Existing non-v2 stores refuse."
  [root store-id]
  (let [s (legacy/isolated-store root store-id)
        pdir (.resolve ^Path (:root s) "provenance")
        marker (.resolve ^Path (:root s) "FORMAT.edn")
        expected {:schema :wm/e6b-store-format-v2 :scope :isolated-test :store/id store-id}]
    (try
      (if (Files/exists marker (make-array LinkOption 0))
        (when-not (= expected (:record (read-object marker)))
          (refuse! :e6b-store-v2/format-invalid {}))
        (do
          ;; A pre-existing HEAD or transaction is legacy/interrupted state;
          ;; never add a marker and silently reinterpret it as v2.
          (when (or (Files/exists ^Path (:head s) (make-array LinkOption 0))
                    (seq (iterator-seq (.iterator (Files/newDirectoryStream ^Path (:txdir s))))))
            (refuse! :e6b-store-v2/legacy-or-interrupted-store {}))
          (let [bs (form-bytes expected)]
            (Files/write marker bs (into-array StandardOpenOption
                                               [StandardOpenOption/CREATE_NEW
                                                StandardOpenOption/WRITE
                                                StandardOpenOption/SYNC]))
            (force-dir! ^Path (:root s)))))
      (Files/createDirectories pdir (make-array java.nio.file.attribute.FileAttribute 0))
      (assoc s :schema :wm/e6b-isolated-store-v2 :provenance-dir pdir :format marker)
      (catch Throwable e (legacy/release! s) (throw e)))))
(defn release! [store] (legacy/release! store))

(defn- provenance-path [store digest]
  (.resolve ^Path (:provenance-dir store) (str digest ".edn")))
(defn- tx-path [store digest] (.resolve ^Path (:txdir store) (str digest ".edn")))
(defn- read-provenance! [store digest]
  (when-not (re-matches hex64 (or digest ""))
    (refuse! :e6b-store-v2/provenance-digest-invalid {}))
  (let [{:keys [bytes sha256 record]} (read-object (provenance-path store digest))]
    (when-not (= digest sha256) (refuse! :e6b-store-v2/provenance-digest-mismatch {}))
    (let [validated (provenance/readback
                     {:bytes/base64 (.encodeToString (Base64/getEncoder) bytes)
                      :expected-sha256 digest})]
      (when-not (= record (:record validated))
        (refuse! :e6b-store-v2/provenance-record-mismatch {}))
      validated)))

(defn- expected-tx [store generation prior provenance-artifact provenance-sha]
  (let [r (:record provenance-artifact) projection (:carrier-projection r)
        app-view (:retrospective-application-view r) proposal (:proposal-evidence r)]
    {:schema :wm/e6b-state-transaction-v2 :store/id (:store-id store)
     :generation generation :prior (assoc prior :generation (dec generation))
     :next {:revision (get-in projection [:next :carrier :state/revision])
            :state (get-in projection [:next :carrier])
            :state-sha256 (get-in projection [:next :sha256])}
     :application (assoc app-view :transition/subject (:transition/subject proposal))
     :committed-at (:committed-at proposal) :provenance-sha256 provenance-sha}))
(defn- parent-head [store generation prior]
  {:store/id (:store-id store) :generation (dec generation)
   :transaction-sha256 (:transaction-sha256 prior)
   :state/revision (:revision prior) :state-sha256 (:state-sha256 prior)})
(defn- tx-joins? [store tx digest expected-generation child]
  (and (= #{:schema :store/id :generation :prior :next :application
            :committed-at :provenance-sha256} (set (keys tx)))
       (= :wm/e6b-state-transaction-v2 (:schema tx)) (= (:store-id store) (:store/id tx))
       (= expected-generation (:generation tx))
       (= (dec expected-generation) (get-in tx [:prior :generation]))
       (or (nil? child)
           (= (select-keys (:prior child) [:revision :state-sha256])
              {:revision (get-in tx [:next :revision]) :state-sha256 (get-in tx [:next :state-sha256])}))
       (= digest (sha256 (form-bytes tx)))))

(defn- recover* [store]
  (let [head-r (read-object ^Path (:head store)) head (:record head-r)]
    (when-not (and (= #{:schema :store/id :generation :state/revision :state-sha256
                        :transaction-sha256 :application-index} (set (keys head)))
                   (= :wm/e6b-state-head-v2 (:schema head)) (= (:store-id store) (:store/id head))
                   (nat-int? (:generation head)) (vector? (:application-index head))
                   (string? (:state/revision head)) (re-matches hex64 (:state-sha256 head ""))
                   (re-matches hex64 (:transaction-sha256 head "")))
      (refuse! :e6b-store-v2/head-invalid {}))
    (loop [digest (:transaction-sha256 head) generation (:generation head)
           child nil apps [] txs [] provs {} revisions #{(:state/revision head)}]
      (let [{:keys [record] actual-sha :sha256} (read-object (tx-path store digest)) tx record]
        (when-not (= digest actual-sha) (refuse! :e6b-store-v2/transaction-digest-mismatch {}))
        (when (and (nil? child)
                   (not= {:generation (:generation head) :revision (:state/revision head)
                          :state-sha256 (:state-sha256 head)}
                         (if (= :wm/e6b-state-genesis-v2 (:schema tx))
                           {:generation (:generation tx) :revision (:state/revision tx)
                            :state-sha256 (:state-sha256 tx)}
                           {:generation (:generation tx) :revision (get-in tx [:next :revision])
                            :state-sha256 (get-in tx [:next :state-sha256])})))
          (refuse! :e6b-store-v2/head-current-mismatch {}))
        (if (= :wm/e6b-state-genesis-v2 (:schema tx))
          (do
            (when-not (and (= #{:schema :store/id :generation :prior :state :state/revision
                                :state-sha256 :application :authority :committed-at}
                              (set (keys tx)))
                           (zero? generation) (= (:store-id store) (:store/id tx))
                           (= {} (:prior tx)) (nil? (:application tx))
                           (= digest (sha256 (form-bytes tx)))
                           (= (:state-sha256 tx) (sha256 (form-bytes (:state tx))))
                           (or (nil? child)
                               (= (select-keys (:prior child) [:revision :state-sha256])
                                  {:revision (:state/revision tx) :state-sha256 (:state-sha256 tx)}))
                           (= (vec (reverse apps)) (:application-index head)))
              (refuse! :e6b-store-v2/chain-invalid {}))
            {:head head :head-digest (:sha256 head-r) :current (first txs)
             :applications (vec (reverse apps)) :transactions (vec (reverse txs))
             :provenance provs :state-revisions revisions
             :chain-digests (vec (reverse (conj (mapv :digest txs) digest)))})
          (do
            (when-not (tx-joins? store tx digest generation child)
              (refuse! :e6b-store-v2/transaction-invalid {:digest digest}))
            (let [p (read-provenance! store (:provenance-sha256 tx))
                  _ (when-not (= (get-in p [:record :expected-head])
                                 (parent-head store generation (:prior tx)))
                      (refuse! :e6b-store-v2/provenance-parent-mismatch {:digest digest}))
                  expected (expected-tx store generation
                                       (dissoc (:prior tx) :generation) p (:provenance-sha256 tx))]
              (when-not (= tx expected)
                (refuse! :e6b-store-v2/transaction-provenance-mismatch {:digest digest}))
              (let [a (:application tx)
                    entry {:application/id (:application/id a)
                           :feedback/event-id (:feedback/event-id a)
                           :prior-state/revision (:prior-state/revision a)
                           :transaction-sha256 digest :provenance-sha256 (:provenance-sha256 tx)}]
                (when (or (some #(= (:application/id entry) (:application/id %)) apps)
                          (some #(= (:feedback/event-id entry) (:feedback/event-id %)) apps)
                          (contains? revisions (get-in tx [:prior :revision])))
                  (refuse! :e6b-store-v2/application-conflict {}))
                (recur (get-in tx [:prior :transaction-sha256]) (dec generation) tx
                       (conj apps entry) (conj txs (assoc tx :digest digest))
                       (assoc provs (:provenance-sha256 tx) p)
                       (conj revisions (get-in tx [:prior :revision])))))))))))

(defn initialize!
  [store {:keys [state revision authority committed-at] :as input}]
  (with-owner store
    (when (or (Files/exists ^Path (:head store) (make-array LinkOption 0))
              (seq (iterator-seq (.iterator (Files/newDirectoryStream ^Path (:txdir store))))))
      (refuse! :e6b-store-v2/already-initialized-or-interrupted {}))
    (when-not (and (= #{:state :revision :authority :committed-at} (set (keys input)))
                   (map? state) (seq state) (string? revision) (seq revision)
                   (map? authority) (string? committed-at)
                   (= state (strict-edn (form-bytes state) :genesis-state)))
      (refuse! :e6b-store-v2/genesis-input-invalid {}))
    (let [tx {:schema :wm/e6b-state-genesis-v2 :store/id (:store-id store) :generation 0
              :prior {} :state state :state/revision revision
              :state-sha256 (sha256 (form-bytes state)) :application nil
              :authority authority :committed-at committed-at}]
      (try
        (let [digest (publish! store :transaction ^Path (:txdir store) tx)
              head {:schema :wm/e6b-state-head-v2 :store/id (:store-id store) :generation 0
                    :state/revision revision :state-sha256 (:state-sha256 tx)
                    :transaction-sha256 digest :application-index []}]
          (write-head! store head) (recover* store))
        (catch Throwable e (reset! (:poisoned? store) true) (throw e))))))

(defn commit!
  "Publish a revalidated provenance artifact, then its transaction, then HEAD."
  [store provenance-input]
  (with-owner store
    (let [p (provenance/readback provenance-input)
          record (:record p) app-id (get-in record [:proposal-evidence :application/id])
          recovered (recover* store)
          existing (first (filter #(= app-id (:application/id %)) (:applications recovered)))]
      ;; Stable retry is intentionally decided before current HEAD comparison.
      (if existing
        (if (= (:sha256 p) (:provenance-sha256 existing))
          (:record (read-object (tx-path store (:transaction-sha256 existing))))
          (refuse! :e6b-store-v2/application-conflict {:application/id app-id}))
        (let [head (:head recovered) expected-head (:expected-head record)]
          (when-not (= expected-head
                       {:store/id (:store/id head) :generation (:generation head)
                        :transaction-sha256 (:transaction-sha256 head)
                        :state/revision (:state/revision head) :state-sha256 (:state-sha256 head)})
            (refuse! :e6b-store-v2/stale-head {}))
          (let [event-id (get-in record [:proposal-evidence :feedback/event-id])
                prior-revision (get-in record [:proposal-evidence :prior :state/revision])
                next-revision (get-in record [:proposal-evidence :next :state/revision])]
            (when (or (some #(= event-id (:feedback/event-id %)) (:applications recovered))
                      (some #(= prior-revision (:prior-state/revision %)) (:applications recovered))
                      (contains? (:state-revisions recovered) next-revision))
              (refuse! :e6b-store-v2/feedback-conflict {})))
          (try
            (let [pd (publish! store :provenance ^Path (:provenance-dir store) (:record p))
                  generation (inc (:generation head))
                  prior {:revision (:state/revision head)
                         :transaction-sha256 (:transaction-sha256 head)
                         :state-sha256 (:state-sha256 head)}
                  tx (expected-tx store generation prior p pd)
                  td (publish! store :transaction ^Path (:txdir store) tx)
                  a (:application tx)
                  entry {:application/id (:application/id a) :feedback/event-id (:feedback/event-id a)
                         :prior-state/revision (:prior-state/revision a)
                         :transaction-sha256 td :provenance-sha256 pd}
                  head' {:schema :wm/e6b-state-head-v2 :store/id (:store-id store)
                         :generation generation :state/revision (get-in tx [:next :revision])
                         :state-sha256 (get-in tx [:next :state-sha256])
                         :transaction-sha256 td
                         :application-index (conj (:applications recovered) entry)}]
              (write-head! store head') tx)
            (catch Throwable e (reset! (:poisoned? store) true) (throw e))))))))

(defn recover [store] (with-owner store (recover* store)))
(defn capture [store]
  (with-owner store
    (let [r (recover* store)
          tx-objects (into {} (for [d (:chain-digests r)]
                                [d (aclone ^bytes (:bytes (read-object (tx-path store d))))]))
          p-objects (into {} (for [d (keys (:provenance r))]
                               [d (aclone ^bytes (:bytes (read-object (provenance-path store d))))]))]
      {:schema :wm/e6b-store-capture-v2 :scope :isolated-test :store/id (:store-id store)
       :generation (get-in r [:head :generation]) :head-digest (:head-digest r)
       :chain-digests (:chain-digests r) :application-universe (:applications r)
       :transaction-objects tx-objects :provenance-objects p-objects
       :completeness-authority :absent :rollback-freshness? :unproved
       :restart-authorized? false})))
