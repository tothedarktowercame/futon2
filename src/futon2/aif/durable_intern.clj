(ns futon2.aif.durable-intern
  "Lossless durable encoding for large War Machine records: every repeated
  large subtree is written once.

  Click 51's selection checkpoint was 2,380,766,904 bytes, of which
  2,268,364,187 were exact repeats of subtrees already written elsewhere in
  the same file (the whole controller decision again under :ground
  :decision, each scorer's C source, each policy identity repeated in every
  carrier...). The live JVM shares those subtrees in memory; printing them
  duplicated every copy. This namespace keeps the sharing on disk.

  Encoding: a map value whose repeated subtrees (each at least min-bytes
  when printed) are replaced by {:durable/ref id}, with
  {:durable/interned {id subtree}} added at the top level. id is the SHA-256
  of the subtree's printed EDN. Table entries are themselves encoded, so
  nested repeats are written once too. A value with nothing to intern is
  returned unchanged, so small checkpoints keep their exact bytes.

  hydrate is the inverse. It returns the value the original would read back
  as, with each interned subtree shared rather than copied, so a hydrated
  record also needs far less heap than reading the original did."
  (:require [futon2.aif.durable-hydrate :as durable-hydrate])
  (:import [java.io Writer]
           [java.security MessageDigest]
           [java.util HashMap HashSet IdentityHashMap]))

(def table-key durable-hydrate/table-key)
(def ref-key durable-hydrate/ref-key)

(def default-min-bytes
  "Subtrees smaller than this stay inline. Measured on click 51: almost all
  of the repetition is in subtrees far larger than this."
  4096)

(defn- counting-writer
  "A Writer that only counts characters; never holds them."
  ^Writer [^longs n]
  (proxy [Writer] []
    (write
      ([x]
       (cond (string? x) (aset n 0 (+ (aget n 0) (count x)))
             (integer? x) (aset n 0 (inc (aget n 0)))
             :else (aset n 0 (+ (aget n 0) (alength ^chars x)))))
      ([x off len] (aset n 0 (+ (aget n 0) (long len)))))
    (flush [])
    (close [])))

(defn- printed-size [v]
  (let [n (long-array 1)]
    (binding [*out* (counting-writer n) *print-length* nil *print-level* nil]
      (pr v))
    (aget n 0)))

(defn printed-sha256
  "Lowercase hex SHA-256 of V's pr output as UTF-8, computed by streaming:
  the same digest as (sha256 (.getBytes (pr-str v) \"UTF-8\")) without ever
  holding the printed form, which for a run record can exceed the 2 GB
  String limit. A real UTF-8 encoder is required: pr writes strings one
  char at a time, so a character outside the BMP arrives as two surrogate
  halves that must be encoded as one code point."
  [v]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (with-open [out (java.io.OutputStreamWriter.
                     (java.security.DigestOutputStream.
                      (java.io.OutputStream/nullOutputStream) md)
                     java.nio.charset.StandardCharsets/UTF_8)]
      (binding [*out* out *print-length* nil *print-level* nil]
        (pr v)))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md)))))

(defn- kind [x]
  (cond (map? x) :map (vector? x) :vector (set? x) :set (seq? x) :seq
        (sequential? x) :sequential :else (class x)))

(declare typed=)

(deftype ^:private Typed [v]
  Object
  (hashCode [_] (hash v))
  (equals [_ o] (and (instance? Typed o) (typed= v (.-v ^Typed o)))))

(defn- typed=
  "Clojure = that also requires the same collection kinds and scalar classes
  throughout: = treats [1 2] and '(1 2), or 1 and 1N, as equal, and interning
  one in place of the other would change what a reader gets back."
  [a b]
  (or (identical? a b)
      (and (= (kind a) (kind b))
           (cond
             (map? a) (and (= (count a) (count b))
                           (let [typed-b (HashMap.)]
                             (doseq [[k v] b] (.put typed-b (Typed. k) v))
                             (every? (fn [[k v]]
                                       (let [tk (Typed. k)]
                                         (and (.containsKey typed-b tk)
                                              (typed= v (.get typed-b tk)))))
                                     a)))
             (sequential? a) (and (= (count a) (count b))
                                  (every? true? (map typed= a b)))
             (set? a) (and (= (count a) (count b))
                           (let [typed-b (HashSet.)]
                             (doseq [v b] (.add typed-b (Typed. v)))
                             (every? #(.contains typed-b (Typed. %)) a)))
             :else (= a b)))))

(defn- internable? [v]
  (and (coll? v) (not (record? v)) (> (count v) 1)))

(defn- occurrence-counts
  "Typed value -> occurrence count, walking each distinct subtree only once.
  Identity is checked first: in the live JVM most repeats are the same
  object, so this rarely needs a deep equality test."
  ^HashMap [root]
  (let [by-identity (IdentityHashMap.)
        by-value (HashMap.)]
    (letfn [(descend [v]
              (if (map? v)
                (doseq [[_ y] v] (walk y))
                (doseq [y v] (walk y))))
            (walk [v]
              ;; Descend into EVERY collection; count only the internable
              ;; ones. (Gating the descent on internable? hid every repeat
              ;; beneath a one-key map or one-element vector.)
              (cond
                (not (coll? v)) nil
                (not (internable? v)) (descend v)
                (.containsKey by-identity v)
                (let [canon (.get by-identity v)]
                  (.put by-value canon (inc (long (.get by-value canon)))))
                :else
                (let [k (Typed. v)]
                  (if-let [n (.get by-value k)]
                    (do (.put by-identity v k)
                        (.put by-value k (inc (long n))))
                    (do (.put by-identity v k)
                        (.put by-value k 1)
                        (descend v))))))]
      (walk root))
    by-value))

(def default-min-total-bytes
  "Records smaller than this are written exactly as before: interning is for
  the large records whose repeats dominate, and ordinary checkpoints keep
  their familiar bytes."
  (* 8 1024 1024))

(defn encode
  "Return M with each repeated subtree of at least MIN-BYTES written once,
  when M prints to at least MIN-TOTAL-BYTES. Map keys are never interned;
  neither is M itself. With ONLY-KEYS, only the values under those top-level
  keys are searched and rewritten; every other top-level value is written
  exactly as given, so readers of those fields need not hydrate."
  ([m] (encode m {}))
  ([m {:keys [min-bytes min-total-bytes only-keys]
       :or {min-bytes default-min-bytes min-total-bytes default-min-total-bytes}}]
   (if (or (not (map? m)) (< (printed-size m) min-total-bytes))
     m
     (let [scope (if only-keys (select-keys m only-keys) m)
           counts (occurrence-counts scope)
           chosen (HashMap.)]
       ;; Size only the repeated values; most subtrees occur once.
       (doseq [[^Typed k n] counts
               :let [v (.-v k)]
               :when (and (>= (long n) 2) (not (identical? v scope))
                          (>= (printed-size v) min-bytes))]
         (.put chosen k true))
       (if (.isEmpty chosen)
         m
         (let [ids (HashMap.)
               table (volatile! (transient {}))]
           (letfn [(rewrite [v]
                     (cond
                       (map? v) (persistent!
                                 (reduce-kv (fn [acc k y] (assoc! acc k (ref-or-inline y)))
                                            (transient (empty v)) v))
                       (vector? v) (mapv ref-or-inline v)
                       (set? v) (into (empty v) (map ref-or-inline) v)
                       (seq? v) (doall (map ref-or-inline v))
                       :else v))
                   (ref-or-inline [v]
                     (if (and (internable? v) (.containsKey chosen (Typed. v)))
                       (let [k (Typed. v)
                             id (or (.get ids k)
                                    (let [id (printed-sha256 v)
                                          _ (.put ids k id)
                                          ;; Rewrite BEFORE touching the table:
                                          ;; the rewrite adds nested entries,
                                          ;; and vswap! would read the table
                                          ;; first and drop them.
                                          entry (rewrite v)]
                                      (vswap! table assoc! id entry)
                                      id))]
                         {ref-key id})
                       (rewrite v)))]
             (let [body (if only-keys
                          (reduce (fn [acc k] (if (contains? m k)
                                                (assoc acc k (ref-or-inline (get m k)))
                                                acc))
                                  m only-keys)
                          (rewrite m))]
               (assoc body table-key (persistent! @table))))))))))

(def interned? durable-hydrate/interned?)

(def hydrate
  "Inverse of encode; see futon2.aif.durable-hydrate."
  durable-hydrate/hydrate)
