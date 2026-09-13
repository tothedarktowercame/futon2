(require '[clojure.edn :as edn])
(import '(java.nio.charset CodingErrorAction StandardCharsets)
        '(java.nio ByteBuffer)
        '(java.nio.file Files Path)
        '(java.security MessageDigest))

(def manifest-path
  (Path/of "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured/capture-manifest.edn"
           (make-array String 0)))
(defn sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn strict-read [^bytes bs]
  (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                  (.onMalformedInput CodingErrorAction/REPORT)
                  (.onUnmappableCharacter CodingErrorAction/REPORT))
        text (str (.decode decoder (ByteBuffer/wrap bs)))
        rdr (java.io.PushbackReader. (java.io.StringReader. text))
        eof (Object.) x (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
    (assert (not (identical? eof x)))
    (assert (identical? eof tail))
    x))

(let [manifest-bytes (Files/readAllBytes manifest-path)
      manifest (strict-read manifest-bytes)]
  (assert (= :wm/e6b-isolated-canonical-fixture-capture-v1 (:schema manifest)))
  (assert (false? (get-in manifest [:authority :authenticated-external?])))
  (doseq [[role {:keys [path sha256 value-sha256 bytes]}] (:records manifest)]
    (let [buffer (Files/readAllBytes (Path/of path (make-array String 0)))
          record (strict-read buffer)
          value-bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)]
      (assert (= bytes (alength buffer)) (str role " byte count"))
      (assert (= sha256 (user/sha256 buffer)) (str role " raw digest"))
      (assert (= value-sha256 (user/sha256 value-bytes)) (str role " value digest"))))
  (prn {:status :strict-readback-passed
        :records (count (:records manifest))
        :manifest-sha256 (sha256 manifest-bytes)
        :authority :unauthenticated-test-data}))
