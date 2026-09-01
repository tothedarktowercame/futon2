(ns checks.mutable-read-set
  "Shared observation boundary for checks that consume mutable files.

   A captured entry derives its text and digest from the same byte array.
   `observe-files` then compares the captured digest with a second observation,
   making movement an explicit result rather than silently combining states."
  (:require [babashka.fs :as fs])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files Paths]
           [java.security MessageDigest]
           [java.time Instant]))

(defn sha256-bytes [bytes]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest bytes)
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn- read-bytes [path]
  (Files/readAllBytes (Paths/get (str path) (make-array String 0))))

(defn capture-file [path]
  (when-not (fs/regular-file? path)
    (throw (ex-info "mutable read-set input is absent" {:path (str path)})))
  (let [bytes (read-bytes path)]
    {:path (str path)
     :bytes bytes
     :text (String. bytes StandardCharsets/UTF_8)
     :size (alength bytes)
     :sha256 (sha256-bytes bytes)}))

(defn capture-files [paths]
  {:captured-at (str (Instant/now))
   :entries (mapv capture-file paths)})

(defn compare-current [{:keys [entries]}]
  (mapv (fn [{:keys [path sha256]}]
          (try
            (let [current (read-bytes path)
                  current-sha (sha256-bytes current)]
              {:path path
               :status (if (= sha256 current-sha) :unchanged :changed)
               :captured-sha256 sha256
               :current-sha256 current-sha})
            (catch Throwable failure
              {:path path :status :unavailable :captured-sha256 sha256
               :cause (ex-message failure)})))
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
