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
  (:import [java.io Writer]
           [java.security MessageDigest]
           [java.util HashMap IdentityHashMap]))

(def table-key :durable/interned)
(def ref-key :durable/ref)

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

(defn- digest-writer
  "A Writer that feeds UTF-8 bytes of what is printed into md."
  ^Writer [^MessageDigest md]
  (let [put (fn [^String s] (.update md (.getBytes s "UTF-8")))]
    (proxy [Writer] []
      (write
        ([x]
         (cond (string? x) (put x)
               (integer? x) (put (str (char x)))
               :else (put (String. ^chars x))))
        ([x off len]
         (if (string? x)
           (put (subs x off (+ off len)))
           (put (String. ^chars x (int off) (int len))))))
      (flush [])
      (close []))))

(defn- printed-sha256 [v]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (binding [*out* (digest-writer md) *print-length* nil *print-level* nil]
      (pr v))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md)))))

(defn- kind [x]
  (cond (map? x) :map (vector? x) :vector (set? x) :set (seq? x) :seq
        (sequential? x) :sequential :else (class x)))

(defn- typed=
  "Clojure = that also requires the same collection kinds and scalar classes
  throughout: = treats [1 2] and '(1 2), or 1 and 1N, as equal, and interning
  one in place of the other would change what a reader gets back."
  [a b]
  (or (identical? a b)
      (and (= (kind a) (kind b))
           (cond
             (map? a) (and (= (count a) (count b))
                           (every? (fn [[k v]]
                                     (let [e (find b k)]
                                       (and e (= (class k) (class (key e)))
                                            (typed= v (val e)))))
                                   a))
             (sequential? a) (and (= (count a) (count b))
                                  (every? true? (map typed= a b)))
             (set? a) (and (= a b)
                           (= (set (map (juxt identity class) a))
                              (set (map (juxt identity class) b))))
             :else (= a b)))))

(deftype ^:private Typed [v]
  Object
  (hashCode [_] (hash v))
  (equals [_ o] (and (instance? Typed o) (typed= v (.-v ^Typed o)))))

(defn- internable? [v]
  (and (coll? v) (not (record? v)) (> (count v) 1)))

(defn- occurrence-counts
  "Typed value -> occurrence count, walking each distinct subtree only once.
  Identity is checked first: in the live JVM most repeats are the same
  object, so this rarely needs a deep equality test."
  ^HashMap [root]
  (let [by-identity (IdentityHashMap.)
        by-value (HashMap.)]
    (letfn [(walk [v]
              (when (internable? v)
                (if (.containsKey by-identity v)
                  (let [canon (.get by-identity v)]
                    (.put by-value canon (inc (long (.get by-value canon)))))
                  (let [k (Typed. v)]
                    (if-let [n (.get by-value k)]
                      (do (.put by-identity v k)
                          (.put by-value k (inc (long n))))
                      (do (.put by-identity v k)
                          (.put by-value k 1)
                          (if (map? v)
                            (doseq [[_ y] v] (walk y))
                            (doseq [y v] (walk y)))))))))]
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
  neither is M itself."
  ([m] (encode m {}))
  ([m {:keys [min-bytes min-total-bytes]
       :or {min-bytes default-min-bytes min-total-bytes default-min-total-bytes}}]
   (if (or (not (map? m)) (< (printed-size m) min-total-bytes))
     m
     (let [counts (occurrence-counts m)
           chosen (HashMap.)]
       ;; Size only the repeated values; most subtrees occur once.
       (doseq [[^Typed k n] counts
               :let [v (.-v k)]
               :when (and (>= (long n) 2) (not (identical? v m))
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
             (let [body (rewrite m)]
               (assoc body table-key (persistent! @table))))))))))

(defn interned? [x]
  (and (map? x) (contains? x table-key)))

(defn- ref-id [v]
  (when (and (map? v) (= 1 (count v)))
    (get v ref-key)))

(defn hydrate
  "Inverse of encode. Returns X unchanged when it carries no table."
  [x]
  (if-not (interned? x)
    x
    (let [table (get x table-key)
          done (HashMap.)]
      (letfn [(resolve-id [id]
                (or (.get done id)
                    (let [entry (get table id ::missing)]
                      (when (identical? entry ::missing)
                        (throw (ex-info "Durable reference has no table entry"
                                        {:durable/ref id})))
                      (let [v (walk entry)]
                        (.put done id v)
                        v))))
              (walk [v]
                (if-let [id (ref-id v)]
                  (resolve-id id)
                  (cond
                    (map? v) (persistent!
                              (reduce-kv (fn [acc k y] (assoc! acc k (walk y)))
                                         (transient (empty v)) v))
                    (vector? v) (mapv walk v)
                    (set? v) (into (empty v) (map walk) v)
                    (seq? v) (doall (map walk v))
                    :else v)))]
        (walk (dissoc x table-key))))))
