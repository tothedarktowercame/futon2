(ns futon2.aif.answer-evidence
  "Bounded, content-addressed evidence for an agent answer rejected on form."
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]))

(def schema :wm/rejected-answer-evidence-v1)
(def prefix-limit-bytes (* 64 1024))

(defn- sha256 [^bytes bytes]
  (apply str
         (map #(format "%02x" (bit-and 0xff %))
              (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- utf8-prefix [^String text]
  (loop [offset 0 bytes 0]
    (if (= offset (.length text))
      text
      (let [code-point (.codePointAt text offset)
            width (Character/charCount code-point)
            next-offset (+ offset width)
            encoded-length (alength (.getBytes (.substring text offset next-offset)
                                                 StandardCharsets/UTF_8))]
        (if (> (+ bytes encoded-length) prefix-limit-bytes)
          (.substring text 0 offset)
          (recur next-offset (+ bytes encoded-length)))))))

(defn retain
  "Represent RAW as text and retain at most its first 64 KiB, plus the full
  UTF-8 byte length and digest. DEFECT is the exact parser/validator finding."
  [{:keys [raw agent job-id defect]}]
  (let [text (if (string? raw) raw (pr-str raw))
        bytes (.getBytes text StandardCharsets/UTF_8)
        prefix (utf8-prefix text)]
    {:schema schema
     :answer-prefix prefix
     :prefix-length-bytes (alength (.getBytes prefix StandardCharsets/UTF_8))
     :total-length-bytes (alength bytes)
     :sha256 (sha256 bytes)
     :truncated? (> (alength bytes) prefix-limit-bytes)
     :agent agent
     :job-id job-id
     :defect defect}))
