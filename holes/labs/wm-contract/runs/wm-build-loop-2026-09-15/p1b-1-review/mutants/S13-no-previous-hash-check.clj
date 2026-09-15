(ns futon2.aif.work-target-store
  "Durable, explicitly established work-target snapshot authority. See P1b-1 PROTOCOL.md."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.channels FileChannel)
           (java.nio.file Files Path StandardOpenOption StandardCopyOption LinkOption)
           (java.security MessageDigest)))

(defonce ^:private mutexes (atom {}))
(def ^:dynamic *event* "Test instrumentation, called after successful IO." (fn [_] nil))
(def ^:dynamic *failpoint* "Test hook called at named boundaries; may throw IOException." (fn [_] nil))

(defn open-store
  "Construct a handle without creating anything. Validator is mandatory."
  [path {:keys [payload-validator]}]
  (when-not (ifn? payload-validator)
    (throw (ex-info "A payload validator is required" {:status :invalid-validator})))
  {:path (.toPath (.getCanonicalFile (io/file path))) :payload-validator payload-validator})

(defn- path ^Path [store name] (.resolve ^Path (:path store) ^String name))
(defn- exists? [^Path p] (Files/exists p (make-array LinkOption 0)))
(defn- refusal! [status reason & [details]]
  (throw (ex-info (name reason) (merge {:status status :reason reason} details))))
(defn- require! [condition reason]
  (when-not condition (refusal! :damaged reason)))
(defn- digest [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bs))))

(defn- canonical [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [(canonical k) (canonical v)])) x)
    (set? x) (into (sorted-set-by #(compare (pr-str %1) (pr-str %2))) (map canonical) x)
    (vector? x) (mapv canonical x)
    (list? x) (apply list (map canonical x))
    :else x))
(defn- parse [^bytes bs]
  (try
    (with-open [r (PushbackReader. (StringReader. (String. bs "UTF-8")))]
      (let [eof (Object.) opts {:eof eof} value (edn/read opts r)]
        (when (or (identical? eof value) (not (identical? eof (edn/read opts r))))
          (throw (Exception. "Expected exactly one EDN form")))
        value))
    (catch Exception e (refusal! :damaged :parse-failure {:message (.getMessage e)}))))
(defn- encode [x]
  (binding [*print-length* nil *print-level* nil *print-meta* false
            *print-namespace-maps* false *print-readably* true *print-dup* false]
    (let [bs (.getBytes (str (pr-str (canonical x)) "\n") "UTF-8")]
      (require! (= x (parse bs)) :non-edn-value)
      bs)))
(defn- record-at [^Path p]
  (let [bs (Files/readAllBytes p)] {:value (parse bs) :sha256 (digest bs)}))
(defn- children [^Path p]
  (if (exists? p)
    (with-open [stream (Files/list p)] (vec (iterator-seq (.iterator stream))))
    []))
(defn- fname [^Path p] (str (.getFileName p)))
(defn- mutex [store]
  (let [k (str (:path store))]
    (get (swap! mutexes #(if (contains? % k) % (assoc % k (Object.)))) k)))
(defn- locked [store f]
  (let [monitor (mutex store)]
   (locking monitor
    (with-open [channel (FileChannel/open (path store ".writer.lock")
                                         (into-array java.nio.file.OpenOption
                                                     [StandardOpenOption/CREATE StandardOpenOption/WRITE]))
                lock (.lock channel)]
      (require! (.isValid lock) :invalid-lock)
      ;; Coordination metadata is also exactly one EDN form. Only bootstrap
      ;; a new, otherwise pristine directory; never heal a truncated lock file.
      (when (and (zero? (.size channel))
                 (= [".writer.lock"] (mapv fname (children (:path store)))))
        (let [buffer (ByteBuffer/wrap (encode {:schema :wm/work-target-writer-lock-v1}))]
          (while (.hasRemaining buffer) (.write channel buffer)))
        (.force channel true))
      (f)))))
(defn- caught [f]
  (try (f)
       (catch clojure.lang.ExceptionInfo e (ex-data e))
       (catch Exception e {:status :damaged :reason :io-failure :message (.getMessage e)})))
(defn- hash? [x] (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))
(defn- genesis-valid! [g]
  (require! (and (map? g) (= :wm/work-target-store-genesis-v1 (:schema g))
                 (uuid? (:store/id g)) (= "v1" (:storage-protocol/revision g))
                 (string? (get-in g [:declaration :path]))
                 (hash? (get-in g [:declaration :sha256]))
                 (some? (get-in g [:declaration :interpretation-revision]))
                 (vector? (:decision-refs g)) (seq (:decision-refs g))
                 (some? (:created-at g)) (some? (get-in g [:authorized-by :actor]))
                 (some? (get-in g [:authorized-by :commission]))
                 (= "rollout genesis, not historical initialization" (:statement g)))
            :genesis-envelope))
(defn- declaration-valid! [g bs]
  (require! (= (get-in g [:declaration :sha256]) (digest bs)) :declaration-hash-mismatch)
  (let [d (parse bs)]
    (require! (= (get-in g [:declaration :interpretation-revision]) (:revision d))
              :declaration-revision-mismatch)))
(defn- payload-valid! [store payload]
  (require! (map? payload) :payload-not-map)
  (let [result (try ((:payload-validator store) payload)
                    (catch Exception e {:status :validator-threw :message (.getMessage e)}))]
    (when-not (= :ok result)
      (refusal! :damaged :payload-validation-failed {:validator-refusal result}))))
(defn- operation-valid! [op]
  (require! (and (map? op) (some? (:id op)) (keyword? (:kind op))
                 (keyword? (:caller-identity-type op)) (some? (:information-cutoff op)))
            :operation-envelope))
(defn- intent-hash [head op payload] (digest (encode [head op payload])))
(defn- empty-head [g gh]
  {:store/id (:store/id g) :genesis-sha256 gh :status :established-no-snapshots
   :seq 0 :snapshot-sha256 nil :operation/id nil})
(defn- snapshot-head [s sha]
  {:store/id (:store/id s) :genesis-sha256 (:genesis-sha256 s) :status :committed
   :seq (:seq s) :snapshot-sha256 sha :operation/id (get-in s [:operation :id])})
(defn- reference [s sha] {:store/id (:store/id s) :seq (:seq s) :sha256 sha})

(defn- inspect
  "Caller holds both locks. Enumerate every file, but derive ancestry only from HEAD."
  [store]
  (let [root-files (children (:path store))
        snapshot-files (children (path store "snapshots"))
        ;; A .tmp file is an uncommitted preparation. An interrupted write can
        ;; leave it empty or partial, so it is counted toward pending recovery,
        ;; never parsed as authority. Every other record is parsed strictly.
        prep? #(str/ends-with? (fname %) ".tmp")
        preps (filterv prep? (concat root-files snapshot-files))
        files (remove #(or (= "snapshots" (fname %)) (prep? %)) root-files)
        all-records (into {} (map (fn [p] [(fname p) (record-at p)])) files)
        _ (require! (= {:schema :wm/work-target-writer-lock-v1}
                       (get-in all-records [".writer.lock" :value])) :lock-envelope)
        records (dissoc all-records ".writer.lock")
        snaps (mapv (fn [p] (assoc (record-at p) :filename (fname p)))
                    (remove prep? snapshot-files))
        g (get-in records ["genesis.edn" :value])
        gh (get-in records ["genesis.edn" :sha256])
        init (get-in records ["INIT.edn" :value])
        established (get-in records ["INITIALIZED.edn" :value])
        h (get-in records ["HEAD.edn" :value])]
    (if-not (contains? records "genesis.edn")
      {:status (if (or (seq records) (seq snaps) (seq preps)) :pending-recovery :model-not-established)
       :reason :missing-genesis}
      (do
        (genesis-valid! g)
        (require! (exists? (path store "declaration.edn")) :missing-declaration)
        (declaration-valid! g (Files/readAllBytes (path store "declaration.edn")))
        (when init (require! (= init {:schema :wm/work-target-init-v1 :genesis g}) :wrong-genesis))
        (when established
          (require! (= established {:schema :wm/work-target-initialized-v1
                                    :store/id (:store/id g) :genesis-sha256 gh}) :wrong-genesis)
          (require! init :missing-initialization-record)
          (require! (Files/isDirectory (path store "snapshots") (make-array LinkOption 0))
                    :missing-snapshot-directory))
        (cond
          (nil? h)
          {:status (cond established :damaged
                         (and init (empty? snaps)) :initialization-incomplete
                         :else :pending-recovery)
           :reason :missing-head}

          (or (nil? init) (nil? established))
          {:status :pending-recovery :reason :initialization-unconfirmed}

          :else
          (do
            (require! (= (:store/id g) (:store/id h)) :wrong-store-id)
            (require! (= gh (:genesis-sha256 h)) :wrong-genesis)
            (require! (and (integer? (:seq h)) (<= 0 (:seq h))
                           (#{:committed :established-no-snapshots} (:status h))) :head-envelope)
            (when (= :established-no-snapshots (:status h))
              (require! (= h (empty-head g gh)) :head-envelope))
            (when (= :committed (:status h))
              (require! (and (pos? (:seq h)) (hash? (:snapshot-sha256 h))
                             (some? (:operation/id h))) :head-envelope))
            (let [finals (filterv #(re-matches #"[0-9]+\.edn" (:filename %)) snaps)
                  seqs (mapv #(get-in % [:value :seq]) finals)
                  all-seqs (keep #(get-in % [:value :seq]) snaps)
                  _ (require! (= (count all-seqs) (count (set all-seqs))) :duplicate-seq)
                  _ (require! (every? pos-int? seqs) :snapshot-envelope)
                  _ (require! (= (count seqs) (count (set seqs))) :duplicate-seq)
                  _ (doseq [{:keys [value filename]} finals]
                      (require! (= filename (str (:seq value) ".edn")) :non-contiguous-seq))
                  by-seq (into {} (map (juxt (comp :seq :value) identity)) finals)
                  _ (doseq [n (range 1 (inc (:seq h)))]
                      (require! (contains? by-seq n)
                                (if (= n (:seq h)) :missing-head-snapshot :middle-gap)))
                  chain (loop [n 1 prev (empty-head g gh) result [] ids #{}]
                          (if (> n (:seq h))
                            (do (require! (= prev h) :head-envelope) result)
                            (let [{s :value sha :sha256} (get by-seq n)]
                              (require! s (if (= n (:seq h)) :missing-head-snapshot :middle-gap))
                              (require! (= sha (if (= n (:seq h)) (:snapshot-sha256 h)
                                                  (get-in by-seq [(inc n) :value :previous-sha256])))
                                        (if (and (< n (:seq h)) (nil? (get by-seq (inc n))))
                                          :middle-gap :hash-mismatch))
                              (require! (= :wm/work-target-snapshot-v1 (:schema s)) :snapshot-envelope)
                              (require! (= (:store/id g) (:store/id s)) :wrong-store-id)
                              (require! (= gh (:genesis-sha256 s)) :wrong-genesis)
                              nil
                              (require! (= (:expected-head s) prev) :expected-head-mismatch)
                              (operation-valid! (:operation s))
                              (require! (= (:information-cutoff s) (get-in s [:operation :information-cutoff]))
                                        :information-cutoff-mismatch)
                              (require! (= (:committed-intent-sha256 s)
                                           (intent-hash prev (:operation s) (:payload s))) :intent-hash-mismatch)
                              (payload-valid! store (:payload s))
                              (require! (not (contains? ids (get-in s [:operation :id]))) :duplicate-operation-id)
                              (recur (inc n) (snapshot-head s sha)
                                     (conj result {:snapshot s :sha256 sha :ref (reference s sha)})
                                     (conj ids (get-in s [:operation :id]))))))
                  extras (remove #{"genesis.edn" "declaration.edn" "HEAD.edn" "INIT.edn" "INITIALIZED.edn"}
                                 (keys records))
                  pending? (or (seq extras) (seq preps) (> (count snaps) (:seq h)))
                  tip (last chain)]
              (merge {:status (if pending? :pending-recovery (:status h))
                      :head h :genesis g :genesis-sha256 gh :chain chain}
                     (when pending? {:reason :prepared-artifacts})
                     (when tip (assoc tip :seq (:seq h)))))))))))

(defn read-store
  "Strictly verify the complete authority; never create a genesis or adopt a tail."
  [store]
  (caught #(if (exists? (:path store)) (locked store (fn [] (inspect store)))
               {:status :model-not-established})))

(defn- force-directory! [^Path p]
  (with-open [ch (FileChannel/open p (into-array java.nio.file.OpenOption [StandardOpenOption/READ]))]
    (.force ch true))
  (*event* {:io :directory-force :path (str p)}))
(defn- stage! [stage name] (reset! stage name) (*failpoint* name))
(defn- write-forced! [^Path p bs stage prefix]
  (stage! stage (keyword (str prefix "-before-write")))
  (with-open [ch (FileChannel/open p (into-array java.nio.file.OpenOption
                                               [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))]
    (let [buffer (ByteBuffer/wrap bs)]
      (while (.hasRemaining buffer) (.write ch buffer)))
    (*event* {:io :write :path (str p)})
    (stage! stage (keyword (str prefix "-after-write")))
    (.force ch true)
    (*event* {:io :file-force :path (str p)}))
  (stage! stage (keyword (str prefix "-after-force"))))
(defn- publish! [store name value stage]
  (let [target (path store name) temp (path store (str name ".tmp"))]
    (require! (not (exists? target)) :immutable-file-exists)
    (write-forced! temp (encode value) stage name)
    (reset! stage (keyword (str name "-rename")))
    (Files/move temp target (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
    (*event* {:io :atomic-move :path (str target)})
    (stage! stage (keyword (str name "-after-rename")))
    (force-directory! (.getParent target))
    (stage! stage (keyword (str name "-after-directory-force")))))
(defn- persistence [stage committed f]
  (try (f)
       (catch clojure.lang.ExceptionInfo e (ex-data e))
       (catch Exception e {:status :persistence-failed :stage @stage
                          :commit-point-reached? @committed
                          :exception (.getName (class e)) :message (.getMessage e)})))

(defn initialize!
  "Explicit create-once transaction. Incomplete/damaged stores are never reset."
  [store genesis]
  (let [stage (atom :validate-genesis) committed (atom false)]
    (persistence stage committed
      (fn []
        (genesis-valid! genesis)
        (let [decl (Files/readAllBytes
                    (if (exists? (path store "genesis.edn"))
                      (path store "declaration.edn")
                      (.toPath (io/file (get-in genesis [:declaration :path])))))]
          (declaration-valid! genesis decl)
          (encode genesis)
          (let [monitor (mutex store)]
           (locking monitor
            (when-not (exists? (:path store))
              (reset! stage :create-store-directory)
              (Files/createDirectory (:path store) (make-array java.nio.file.attribute.FileAttribute 0))
              (force-directory! (.getParent ^Path (:path store))))
            (locked store
              (fn []
                (let [current (inspect store)]
                  (cond
                    (#{:committed :established-no-snapshots} (:status current))
                    (if (= genesis (:genesis current))
                      (assoc current :status :already-established)
                      {:status :genesis-conflict})

                    (not= :model-not-established (:status current)) current

                    :else
                    (do
                      (publish! store "INIT.edn" {:schema :wm/work-target-init-v1 :genesis genesis} stage)
                      ;; Retain literal declaration bytes, including comments.
                      (write-forced! (path store "declaration.edn.tmp") decl stage "declaration.edn")
                      (reset! stage :declaration-rename)
                      (Files/move (path store "declaration.edn.tmp") (path store "declaration.edn")
                                  (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
                      (force-directory! (:path store))
                      (reset! stage :create-snapshots-directory)
                      (Files/createDirectory (path store "snapshots") (make-array java.nio.file.attribute.FileAttribute 0))
                      (force-directory! (:path store))
                      (publish! store "genesis.edn" genesis stage)
                      (let [gh (:sha256 (record-at (path store "genesis.edn")))]
                        (publish! store "HEAD.edn" (empty-head genesis gh) stage)
                        (publish! store "INITIALIZED.edn"
                                  {:schema :wm/work-target-initialized-v1 :store/id (:store/id genesis)
                                   :genesis-sha256 gh} stage))
                      (reset! committed true)
                      (inspect store)))))))))))))

(defn commit!
  "Commit only against expected-head; identical operation retries return the original ref."
  [store expected-head operation payload]
  (let [stage (atom :lock) committed (atom false)]
    (persistence stage committed
      #(if-not (exists? (:path store))
         {:status :model-not-established}
         (locked store
           (fn []
             (let [current (inspect store)]
               (if-not (#{:committed :established-no-snapshots} (:status current))
                 current
                 (do
                   (operation-valid! operation)
                   (let [intent (intent-hash expected-head operation payload)
                         previous (some (fn [entry]
                                          (when (= (:id operation) (get-in entry [:snapshot :operation :id])) entry))
                                        (:chain current))]
                     (cond
                       previous
                       (if (= intent (get-in previous [:snapshot :committed-intent-sha256]))
                         {:status :committed :ref (:ref previous) :idempotent? true}
                         {:status :operation-id-reused})

                       (not= expected-head (:head current)) {:status :stale-predecessor :head (:head current)}

                       :else
                       (let [_ (payload-valid! store payload)
                             h (:head current)
                             s {:schema :wm/work-target-snapshot-v1 :store/id (:store/id h)
                                :seq (inc (:seq h)) :genesis-sha256 (:genesis-sha256 h)
                                :previous-sha256 (:snapshot-sha256 h) :operation operation
                                :expected-head h :information-cutoff (:information-cutoff operation)
                                :committed-intent-sha256 intent :payload payload}
                             bs (encode s) sha (digest bs)
                             target (path store (str "snapshots/" (:seq s) ".edn"))
                             temp (path store "snapshots/snapshot.tmp")
                             new-head (snapshot-head s sha)]
                         (publish! store "PENDING.edn"
                                   {:schema :wm/work-target-pending-v1 :expected-head h
                                    :proposed-head new-head :committed-intent-sha256 intent} stage)
                         (write-forced! temp bs stage "snapshot")
                         (require! (not (exists? target)) :immutable-file-exists)
                         (reset! stage :snapshot-rename)
                         (Files/move temp target (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
                         (*event* {:io :atomic-move :path (str target)})
                         (stage! stage :snapshot-after-rename)
                         (force-directory! (path store "snapshots"))
                         (stage! stage :snapshot-after-directory-force)
                         (write-forced! (path store "HEAD.edn.tmp") (encode new-head) stage "head")
                         (reset! stage :head-rename)
                         (Files/move (path store "HEAD.edn.tmp") (path store "HEAD.edn")
                                     (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE]))
                         (*event* {:io :atomic-move :path (str (path store "HEAD.edn"))})
                         (stage! stage :head-after-rename)
                         (reset! stage :head-directory-force)
                         (force-directory! (:path store))
                         (reset! committed true)
                         (stage! stage :head-after-directory-force)
                         (reset! stage :pending-delete)
                         (Files/delete (path store "PENDING.edn"))
                         (stage! stage :pending-after-delete)
                         (force-directory! (:path store))
                         (stage! stage :response)
                         {:status :committed :ref (reference s sha)}))))))))))))

(defn resolve-reference
  "Accept a committed ancestor; never require equality with the newest snapshot."
  [store ref]
  (let [current (read-store store)]
    (if-not (#{:committed :established-no-snapshots} (:status current))
      current
      (cond
        (not= (:store/id ref) (get-in current [:head :store/id]))
        {:status :reference-refused :reason :wrong-store-id}
        (not (pos-int? (:seq ref))) {:status :reference-refused :reason :missing-reference}
        (> (:seq ref) (get-in current [:head :seq])) {:status :reference-refused :reason :ahead-of-head}
        :else (let [entry (nth (:chain current) (dec (:seq ref)) nil)]
                (cond
                  (nil? entry) {:status :reference-refused :reason :missing-reference}
                  (not= ref (:ref entry)) {:status :reference-refused :reason :off-chain}
                  :else {:status :resolved :ref ref :snapshot (:snapshot entry)}))))))
