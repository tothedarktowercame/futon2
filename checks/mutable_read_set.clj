(ns checks.mutable-read-set
  "Shared observation boundary for checks that consume mutable files.

   A captured entry derives its text and digest from the same byte array.
   `observe-files` then compares the captured digest with a second observation,
   making movement an explicit result rather than silently combining states."
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
         stable? (every? #(= :unchanged (:status %)) comparison)]
     {:status (if stable? :stable :moved)
      :snapshot snapshot
      :comparison comparison})))

(defn require-stable! [observation]
  (when-not (= :stable (:status observation))
    (throw (ex-info "mutable read-set moved during observation"
                    (select-keys observation [:status :comparison]))))
  (:snapshot observation))

(defn entry-by-path [snapshot path]
  (some #(when (= (str path) (:path %)) %) (:entries snapshot)))
