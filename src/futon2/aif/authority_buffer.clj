(ns futon2.aif.authority-buffer
  "Immutable, single-read authority acquisition. This helper authenticates a
  caller-configured path/digest pair; it grants no production authority."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files Path)
           (java.security MessageDigest)))

(defn- refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :refusal reason))))

(defn sha256 [^bytes bs]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bs))))

(defn- strict-utf8 [^bytes bs]
  (try
    (str (.decode (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
                  (ByteBuffer/wrap bs)))
    (catch Exception e
      (refuse! :authority-invalid-utf8 {:cause (.getMessage e)}))))

(defn- parse-one-edn [s]
  (try
    (with-open [r (PushbackReader. (StringReader. s))]
      (let [v (edn/read {:eof ::eof} r)
            tail (edn/read {:eof ::eof} r)]
        (when (= v ::eof) (refuse! :authority-malformed {:format :edn :reason :empty}))
        (when-not (= tail ::eof)
          (refuse! :authority-trailing-form {:format :edn}))
        v))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :authority-malformed {:format :edn :cause (.getMessage e)}))))

(defn- parse-one-json [s]
  (try
    ;; Cheshire/Jackson rejects non-whitespace trailing input.
    (json/parse-string-strict s true)
    (catch Exception e
      (refuse! :authority-malformed {:format :json :cause (.getMessage e)}))))

(defn capture!
  "Read path exactly once, clone its bytes, verify the externally configured
  digest, decode strict UTF-8 and parse one EDN or JSON value. Returned values
  contain no mutable backing byte array."
  [{:keys [path expected-sha256 format]}]
  (when-not (and (string? path) (seq path) (string? expected-sha256))
    (refuse! :authority-config-missing {:path path}))
  (let [bs (try (Files/readAllBytes (Path/of path (make-array String 0)))
                (catch Exception e
                  (refuse! :authority-source-missing {:path path :cause (.getMessage e)})))
        frozen (aclone bs)
        observed (sha256 frozen)]
    (when-not (= expected-sha256 observed)
      (refuse! :authority-pin-mismatch
               {:path path :expected expected-sha256 :observed observed}))
    (let [text (strict-utf8 frozen)
          value (case format
                  :edn (parse-one-edn text)
                  :json (parse-one-json text)
                  (refuse! :authority-format-unsupported {:format format}))]
      {:path path :format format :source-sha256 observed
       :source-text text :value value})))

(defn resolve-pointer!
  "Resolve an exact vector pointer from the captured parsed value. The raw
  source digest and canonical EDN value digest remain distinct."
  [capture pointer]
  (when-not (and (vector? pointer) (seq pointer))
    (refuse! :authority-pointer-ambiguous {:pointer pointer}))
  (loop [v (:value capture), ks pointer]
    (if-let [k (first ks)]
      (let [present? (cond (map? v) (contains? v k)
                           (vector? v) (and (integer? k) (<= 0 k) (< k (count v)))
                           :else false)]
        (when-not present?
          (refuse! :authority-pointer-missing {:pointer pointer :at k}))
        (recur (get v k) (next ks)))
      {:pointer pointer
       :source-sha256 (:source-sha256 capture)
       :value v
       :value-sha256 (sha256 (.getBytes (pr-str v) StandardCharsets/UTF_8))})))
