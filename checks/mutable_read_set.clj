(ns checks.mutable-read-set
  "Shared observation boundary for checks that consume mutable files.

   A captured entry derives its text and digest from the same byte array.
   `observe-files` compares captured digests with a second observation. Callers
   then declare whether they need content equality or evidence of event freedom;
   the substrate never guesses from endpoint equality alone."
  (:require [babashka.fs :as fs])
  (:import [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.nio.file Files Paths]
           [java.security MessageDigest]
           [java.time Instant]))

(defn sha256-bytes [bytes]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest bytes)
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn read-bytes [path]
  (Files/readAllBytes (Paths/get (str path) (make-array String 0))))

(defn- strict-utf8 [bytes]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))]
      {:status :present :value (str (.decode decoder (ByteBuffer/wrap bytes)))})
    (catch java.nio.charset.CharacterCodingException _
      {:status :absent :reason :non-utf8})))

(defn- path-kind [path]
  (cond
    (not (fs/exists? path)) :absent
    (not (fs/regular-file? path)) :wrong-kind
    :else :regular-file))

(defn capture-file [path]
  (let [kind (path-kind path)]
    (when-not (= :regular-file kind)
      (throw (ex-info "mutable read-set input cannot be captured"
                      {:path (str path) :read-set/reason kind})))
    (try
      (let [bytes (read-bytes path)
            decoded (strict-utf8 bytes)]
        (cond-> {:path (str path)
                 :bytes bytes
                 :text-status (dissoc decoded :value)
                 :size (alength bytes)
                 :sha256 (sha256-bytes bytes)}
          (= :present (:status decoded)) (assoc :text (:value decoded))))
      (catch Throwable failure
        (throw (ex-info "mutable read-set input is unreadable"
                        {:path (str path) :read-set/reason :unreadable}
                        failure))))))

(defn capture-files [paths]
  {:captured-at (str (Instant/now))
   :entries (mapv capture-file paths)})

(defn compare-current [{:keys [entries]}]
  (mapv (fn [{:keys [path sha256]}]
          (let [kind (path-kind path)]
            (if-not (= :regular-file kind)
              {:path path :status :unavailable :reason kind
               :captured-sha256 sha256}
              (try
                (let [current (read-bytes path)
                      current-sha (sha256-bytes current)]
                  {:path path
                   :status (if (= sha256 current-sha) :unchanged :changed)
                   :captured-sha256 sha256
                   :current-sha256 current-sha})
                (catch Throwable failure
                  {:path path :status :unavailable :reason :unreadable
                   :captured-sha256 sha256
                   :cause-class (str (class failure))})))))
        entries))

(defn observe-files
  "Capture exact bytes, optionally run `after-capture` (a control seam), then
   compare the live inputs with the capture. Returns `:stable` with the captured
   read-set or `:moved`; callers choose whether movement makes the verdict
   unavailable or is itself the measured fact."
  ([paths] (observe-files paths {}))
  ([paths {:keys [after-capture]}]
   (let [snapshot (capture-files paths)
         _ (when after-capture (after-capture snapshot))
         comparison (compare-current snapshot)
         compared-at (str (Instant/now))
         stable? (every? #(= :unchanged (:status %)) comparison)]
     {:status (if stable? :stable :moved)
      :interval {:started-at (:captured-at snapshot) :finished-at compared-at}
      :endpoint-equal? stable?
      :snapshot snapshot
      :comparison comparison})))

(defn assess-claim
  "Interpret a neutral observation under a caller-declared claim.

   Content currency needs equal endpoint bytes. Event freedom additionally
   needs a monotonic witness whose before/after token is equal, or a declared
   held fence. Without one it is explicitly unverified, including ABA cases."
  ([observation claim] (assess-claim observation claim {}))
  ([observation claim {:keys [monotonic-witness declared-fence]}]
   (let [equal? (:endpoint-equal? observation)
         witnessed? (or (and (map? monotonic-witness)
                              (contains? monotonic-witness :before)
                              (= (:before monotonic-witness) (:after monotonic-witness)))
                         (and (map? declared-fence)
                              (= :held (:status declared-fence))))]
     (case claim
       :content-current
       {:claim claim
        :verdict (if equal? :satisfied :moved)
        :content-current? equal?
        :interval (:interval observation)}

       :event-free
       {:claim claim
        :verdict (cond (not equal?) :moved witnessed? :satisfied :else :unverified)
        :event-free? (cond (not equal?) false witnessed? true :else :unverified)
        :distinguishable-cause? (boolean witnessed?)
        :interval (:interval observation)}

       (throw (ex-info "unknown mutable read-set claim" {:claim claim}))))))

(defn require-claim! [observation claim]
  (let [assessment (assess-claim observation claim)]
    (when-not (= :satisfied (:verdict assessment))
      (throw (ex-info "mutable read-set claim not satisfied"
                      {:claim claim :assessment assessment
                       :status (:status observation)
                       :comparison (:comparison observation)})))
    (:snapshot observation)))

(defn entry-by-path [snapshot path]
  (some #(when (= (str path) (:path %)) %) (:entries snapshot)))
