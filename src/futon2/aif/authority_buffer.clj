(ns futon2.aif.authority-buffer
  "Immutable, single-read authority acquisition. This helper authenticates a
  caller-configured path/digest pair; it grants no production authority."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files Path)
           (java.security MessageDigest)
           (com.fasterxml.jackson.core JsonFactory JsonParser$Feature JsonToken)))

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
      (let [eof (Object.)
            v (edn/read {:eof eof} r)
            tail (edn/read {:eof eof} r)]
        (when (identical? v eof) (refuse! :authority-malformed {:format :edn :reason :empty}))
        (when-not (identical? tail eof)
          (refuse! :authority-trailing-form {:format :edn}))
        v))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :authority-malformed {:format :edn :cause (.getMessage e)}))))

(defn- parse-one-json [s]
  (try
    (let [factory (doto (JsonFactory.)
                    (.enable JsonParser$Feature/STRICT_DUPLICATE_DETECTION))
          roots (atom 0)]
      (with-open [parser (.createParser factory ^String s)]
        (loop [depth 0]
          (if-let [token (.nextToken parser)]
            (let [start? (or (= token JsonToken/START_OBJECT) (= token JsonToken/START_ARRAY))
                  end? (or (= token JsonToken/END_OBJECT) (= token JsonToken/END_ARRAY))
                  scalar? (.isScalarValue token)
                  next-depth (+ depth (if start? 1 0) (if end? -1 0))]
              (when (or (and scalar? (zero? depth)) (and end? (zero? next-depth)))
                (swap! roots inc))
              (recur next-depth))
            (do
              (when (zero? @roots)
                (refuse! :authority-malformed {:format :json :reason :empty}))
              (when-not (= 1 @roots)
                (refuse! :authority-trailing-form {:format :json}))))))
      (json/parse-string-strict s true))
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
    (let [text (strict-utf8 frozen)]
      ;; Parse now to refuse bad input, but retain only immutable source text;
      ;; pointer reads reparse after rechecking its digest.
      (case format
        :edn (parse-one-edn text)
        :json (parse-one-json text)
        (refuse! :authority-format-unsupported {:format format}))
      {:path path :format format :source-sha256 observed :source-text text})))

(defn canonical [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [k (canonical v)])) x)
    (set? x) (mapv canonical (sort-by pr-str x))
    (sequential? x) (mapv canonical x)
    :else x))

(defn resolve-pointer!
  "Resolve an exact vector pointer from the captured parsed value. The raw
  source digest and canonical EDN value digest remain distinct."
  [capture pointer]
  (when-not (and (vector? pointer) (seq pointer))
    (refuse! :authority-pointer-ambiguous {:pointer pointer}))
  (let [text (:source-text capture)
        observed (sha256 (.getBytes ^String text StandardCharsets/UTF_8))]
    (when-not (= observed (:source-sha256 capture))
      (refuse! :authority-capture-mutated {:expected (:source-sha256 capture)
                                           :observed observed}))
    (loop [v (case (:format capture)
               :edn (parse-one-edn text)
               :json (parse-one-json text)
               (refuse! :authority-format-unsupported {:format (:format capture)}))
           idx 0]
      (if (< idx (count pointer))
      (let [k (nth pointer idx)
            _ (when-not (or (keyword? k) (string? k) (integer? k) (nil? k) (false? k))
                (refuse! :authority-pointer-ambiguous {:pointer pointer :at k}))
            present? (cond (map? v) (contains? v k)
                           (vector? v) (and (integer? k) (<= 0 k) (< k (count v)))
                           :else false)]
        (when-not present?
          (refuse! :authority-pointer-missing {:pointer pointer :at k}))
        (recur (get v k) (inc idx)))
      {:pointer pointer
       :source-sha256 (:source-sha256 capture)
       :value v
       :value-sha256 (sha256 (.getBytes (pr-str (canonical v)) StandardCharsets/UTF_8))}))))
