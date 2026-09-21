(ns futon2.aif.action-identity
  "Versioned, total EDN action identity; never a projection that drops fields."
  (:require [futon2.aif.load-identity :as load-identity])
  (:import [java.security MessageDigest]
           [java.util Date UUID]))

(load-identity/register! *ns* *file*)

(defn- refuse! [value]
  (throw (ex-info "Unsupported action identity value"
                  {:close-retention/refusal :action-identity-value-unsupported
                   :type (str (type value))})))

(defn with-printer
  "Execute with a fixed readable printer; legacy mode is explicit."
  [namespace-maps f]
  (binding [*print-namespace-maps* namespace-maps *print-length* nil
            *print-level* nil *print-meta* false *print-dup* false
            *print-readably* true]
    (f)))

(defn printed [namespace-maps value]
  (with-printer namespace-maps #(pr-str value)))

(declare canonical)
(defn canonical
  "Tagged tree: maps/sets unordered, lists/vectors ordered and distinct.
   Integers normalize width; ratios exact; decimals normalize scale; doubles
   retain finite IEEE value including signed zero. Metadata and non-EDN objects
   (including Float, lazy sequences and records) are rejected."
  [x]
  (when (or (seq (meta x)) (record? x)) (refuse! x))
  (cond
    (nil? x) [:nil]
    (boolean? x) [:boolean x]
    (string? x) [:string x]
    (char? x) [:character (str x)]
    (keyword? x) [:keyword (namespace x) (name x)]
    (symbol? x) [:symbol (namespace x) (name x)]
    (integer? x) [:integer (str x)]
    (ratio? x) (let [normalized (/ (numerator x) (denominator x))]
                 (if (integer? normalized) [:integer (str normalized)]
                     [:ratio (str (numerator normalized)) (str (denominator normalized))]))
    (instance? java.math.BigDecimal x)
    [:decimal (.toPlainString (.stripTrailingZeros ^java.math.BigDecimal x))]
    (instance? Double x)
    (if (Double/isFinite x) [:double (Double/toHexString x)] (refuse! x))
    (instance? UUID x) [:uuid (str x)]
    (= Date (type x)) [:instant (str (.getTime ^Date x))]
    (map? x) [:map (vec (sort-by #(printed false (first %))
                                (map (fn [[k v]] [(canonical k) (canonical v)]) x)))]
    (set? x) [:set (vec (sort-by #(printed false %) (map canonical x)))]
    (vector? x) [:vector (mapv canonical x)]
    (list? x) [:list (mapv canonical x)]
    :else (refuse! x)))

(defn sha256 [text]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String text "UTF-8")))))

(defn digest [action]
  (sha256 (printed false [:wm/action-identity-v2 (canonical action)])))

(defn legacy-matches
  "Recognized v1 serializers: serving/root false and clojure.main true.
   Match the retained digest exactly; no field removal or digest replacement."
  [action expected]
  (vec (for [mode [false true]
             :when (= expected (sha256 (printed mode action)))] mode)))
