(ns futon2.aif.machine-model
  "MachineModelSpec v1. Structural admission, not empirical model validation.
   No inference defaults or tick integration. Evidence vocabulary remains owed."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.belief :as belief]
            [futon2.aif.ruled-outcome-c :as outcome])
  (:import [java.io File]
           [java.security MessageDigest]))

(def schema :wm/machine-model-v1)

(defn outcome-authority []
  {:vertices outcome/ruled-vertices
   :support (vec (sort (map #(vector :organization %) outcome/disposition-outcomes)))
   :open-obligations #{:evidence-vocabulary}})

(defn- refuse! [kind path]
  (throw (ex-info "Machine model refused" {:machine-model/refusal kind :path path})))

(defn- demand! [test kind path]
  (when-not test (refuse! kind path)))

(defn- named? [x] (and (string? x) (not (str/blank? x))))

(defn- required! [m fields path]
  (demand! (map? m) :malformed-map path)
  (doseq [k fields]
    (demand! (and (contains? m k) (some? (get m k))) :missing-field (conj path k))))

(defn- support-refusal-kind [xs]
  (cond
    (not (and (vector? xs) (seq xs))) :missing-support
    (not= (count xs) (count (set xs))) :duplicate-support
    :else nil))

(defn- support! [xs path]
  (when-let [kind (support-refusal-kind xs)]
    (refuse! kind path)))

(def float-row-tolerance
  "Contract v1.1 declared numerical error criterion (reviewer-declared per
   SPEC-fundamentals-build common evidence rules: 'prove/declare the
   numerical error criterion under review; do not quietly expand a
   tolerance'). Float-carried rows are summed EXACTLY at their IEEE values
   (exact rational arithmetic), so the sum is order-independent; admission requires
   |sum - 1| <= this bound. Masses are never renormalized. Evidence basis:
   row-7 production rows sum to 0.9999999999999999 / ...98 (one-ulp float
   error; runs/row-7-belief-state-2026-09-12/readback.edn)."
  1e-12M)

(defn represented-rational
  "Classify one supported numeric coordinate and return its exact rational
   represented value. Float and Double deliberately mean the decimal spelling
   of their widened double value, not their IEEE bits.

   This is the one authority for coordinate conversion. A second copy in a
   consumer would not be an independent comparator: it would repeat the same
   three-case reasoning and therefore share its conceptual errors. Unsupported
   values return a typed refusal instead of leaking an incidental case or Java
   coercion exception."
  [v]
  (cond
    (integer? v) {:ok true :representation :integer :rational v}
    (ratio? v) {:ok true :representation :ratio :rational v}
    (instance? BigDecimal v)
    {:ok true :representation :decimal :rational (rationalize v)}
    (instance? Float v)
    (if (Double/isFinite (double v))
      {:ok true :representation :float32
       :rational (rationalize (BigDecimal. (double v)))}
      {:ok false :refusal {:kind :invalid-mass}})
    (instance? Double v)
    (if (Double/isFinite (double v))
      {:ok true :representation :float64
       :rational (rationalize (BigDecimal. (double v)))}
      {:ok false :refusal {:kind :invalid-mass}})
    :else
    {:ok false
     :refusal {:kind :unsupported-numeric-type
               :type (if (nil? v) "nil" (.getName (class v)))}}))

(defn- representation-class [representations]
  (let [kinds (set (vals representations))
        ieee? (some #{:float32 :float64} kinds)
        exact? (some #{:integer :ratio :decimal} kinds)]
    (cond
      (and ieee? exact?) :mixed-floating
      ieee? :ieee-floating
      (= #{:decimal} kinds) :exact-decimal
      (contains? kinds :decimal) :mixed-exact
      :else :exact-rational)))

(defn numeric-row-admission
  "Shared represented-value admission, with exact rational totals/deviations.
   Integers, ratios and BigDecimal denote exact values; Float/Double denote
   exact finite IEEE values. Integer/ratio-only rows require equality; every
   other supported row uses the unchanged v1.1 absolute criterion. Representation
   and criterion are separate. No mass is repaired.

   :values preserves numeric values (= the input row). Float is widened EXACTLY
   to Double only in this EDN evidence: pr-str/read-string of raw Float can lose
   its value. :representations retains float32 identity. Consumer rows are never
   rewritten. Unsupported/nonfinite values return typed refusals without opaque
   values. Admission is not a composed prediction or logarithmic error bound."
  [row]
  (if-not (map? row)
    {:ok false :refusal {:kind :missing-distribution :path []}}
    (let [coordinates (update-vals row represented-rational)
          representations (update-vals coordinates :representation)
          invalid (some (fn [[k v]]
                          (cond
                            (not (:ok (get coordinates k)))
                            (assoc (:refusal (get coordinates k)) :path [k])
                            (neg? v) {:kind :invalid-mass :path [k]})) row)]
      (if invalid
        {:ok false :refusal invalid}
        (let [representation (representation-class representations)
              toleranced? (not= :exact-rational representation)
              total (reduce +' 0 (map :rational (vals coordinates)))
              deviation (abs (-' total 1))
              bound (if toleranced? (rationalize float-row-tolerance) 0)
              admitted? (<= deviation bound)
              result {:ok admitted?
                      :values (into {} (map (fn [[k v]] [k (if (= :float32 (get representations k))
                                                            (double v) v)])) row)
                      :representation representation :representations representations
                      :exact-total total :exact-deviation deviation
                      :exactly-normalized? (zero? deviation)
                      :criterion {:id (if toleranced? :absolute-row-sum :exact-row-sum)
                                  :revision (if toleranced? "v1.1" "exact-represented-v1")
                                  :target 1 :max-absolute-deviation bound}
                      :admission (when admitted? (if toleranced? :float-carried :exact))}]
          (cond-> result
            (not admitted?) (assoc :refusal {:kind :unnormalized-row :path []})))))))

(defn row-sum-admission
  "Compatibility projection of numeric-row-admission; not a second validator.
   Integer/ratio-only admitted rows -> :exact; every other supported admitted
   row -> :float-carried; all refusals -> nil. :float-carried is a historical
   compatibility label, not a claim that a decimal row contains floats or is
   inexactly normalized. Read :representations and :exactly-normalized? separately."
  [row]
  (:admission (numeric-row-admission row)))

(defn distribution-admission
  "The shared full row boundary: nonempty vector of distinct support identities,
   exact row-key coverage, and numeric-1 mass admission. The model's support!
   uses this same support check. Preserve order/masses; return refusals as data."
  [row support]
  (if-let [kind (support-refusal-kind support)]
    {:ok false :refusal {:kind kind :path []}}
    (cond
      (not (map? row)) {:ok false :refusal {:kind :missing-distribution :path []}}
      (not= (set (keys row)) (set support))
      {:ok false :refusal {:kind :distribution-support-mismatch :path []}}
      :else (assoc (numeric-row-admission row) :support support))))

(defn- distribution! [row support path]
  (let [admission (distribution-admission row support)]
    (when-not (:ok admission)
      (refuse! (get-in admission [:refusal :kind]) path))
    admission))

(defn- measurement! [{:keys [path sha256]} at]
  (demand! (and (named? path) (string? sha256)
                (re-matches #"[0-9a-f]{64}" sha256)) :measurement-pointer-missing at)
  (let [f (File. ^String path)]
    (demand! (.isFile f) :measurement-artifact-missing at)
    (let [bytes (java.nio.file.Files/readAllBytes (.toPath f))
          digest (.digest (MessageDigest/getInstance "SHA-256") bytes)
          actual (apply str (map #(format "%02x" (bit-and 255 %)) digest))]
      (demand! (= sha256 actual) :measurement-pin-mismatch at)
      (try
        (with-open [reader (java.io.PushbackReader. (java.io.StringReader. (String. bytes java.nio.charset.StandardCharsets/UTF_8)))]
          (let [record (edn/read {:eof ::eof} reader)]
            (demand! (and (map? record) (= ::eof (edn/read {:eof ::eof} reader)))
                     :malformed-authority-record at)
            record))
        (catch RuntimeException _ (refuse! :malformed-authority-record at))))))

(defn- authority! [kernel path]
  (required! kernel [:authority :name :rows] path)
  (demand! (named? (:name kernel)) :unnamed-kernel path)
  (demand! (#{:declared-prior :observed-estimate} (:authority kernel))
           :undeclared-authority path)
  (when (= :observed-estimate (:authority kernel))
    (let [record (measurement! (:measurement kernel) (conj path :measurement))]
      (demand! (and (= :wm/kernel-measurement-v1 (:schema record))
                    (= (:name kernel) (:kernel/name record))
                    (= (:rows kernel) (:rows record))
                    (= :measured (:outcome record)))
               :measurement-kernel-mismatch (conj path :measurement)))))

(defn- kernel! [k inputs outputs path]
  (authority! k path)
  (demand! (map? (:rows k)) :missing-kernel-rows path)
  (demand! (= (set inputs) (set (keys (:rows k)))) :kernel-input-mismatch path)
  (doseq [i inputs] (distribution! (get-in k [:rows i]) outputs (conj path :rows i))))

(defn- identity! [x path]
  (required! x [:id :revision] path)
  (demand! (and (named? (:id x)) (named? (:revision x))) :missing-identity path))

(defn- validate* [m]
  (required! m [:schema :model :context :state-support :belief :actions :policies
                :outcome :A :B :D :policy-prior :parameters :observation-encoding
                :semantics] [])
  (demand! (= schema (:schema m)) :unsupported-schema [:schema])
  (identity! (:model m) [:model])
  (required! (:context m) [:entity/id] [:context])
  (demand! (named? (get-in m [:context :entity/id])) :missing-entity [:context])
  (let [states (:state-support m) actions (:actions m)
        policies (:policies m) entity (get-in m [:context :entity/id])
        outcomes (get-in m [:outcome :support])]
    (support! states [:state-support])
    (demand! (= belief/status-set (set states)) :state-carrier-mismatch [:state-support])
    (support! actions [:actions])
    (demand! (every? keyword? actions) :invalid-action [:actions])
    (required! (:belief m) [:mode :posteriors] [:belief])
    (demand! (= :single-entity (get-in m [:belief :mode])) :entity-averaging-forbidden [:belief :mode])
    (demand! (map? (get-in m [:belief :posteriors])) :missing-belief [:belief])
    (distribution! (get-in m [:belief :posteriors entity]) states [:belief :posteriors entity])
    (demand! (= (outcome-authority) (:outcome m)) :outcome-authority-mismatch [:outcome])
    (support! policies [:policies])
    (doseq [p policies]
      (identity! p [:policies])
      (required! p [:entity/id :cascade :actions] [:policies])
      (demand! (= entity (:entity/id p)) :joint-construction-required [:policies :entity/id])
      (identity! (:cascade p) [:policies :cascade])
      (support! (get-in p [:cascade :nodes]) [:policies :cascade :nodes])
      (demand! (and (vector? (:actions p)) (seq (:actions p))
                    (every? (set actions) (:actions p))) :undeclared-policy-action [:policies :actions]))
    (let [ids (mapv :id policies)]
      (support! ids [:policies :ids])
      (distribution! (:policy-prior m) ids [:policy-prior]))
    (kernel! (:A m) states outcomes [:A])
    (kernel! (:B m) (vec (for [s states a actions] [s a])) states [:B])
    (let [identity-b? (every? (fn [[[s _] row]] (= 1 (get row s))) (get-in m [:B :rows]))]
      (when identity-b?
        (demand! (= :declared-prior (get-in m [:B :authority])) :identity-b-must-be-declared [:B])))
    (distribution! (:D m) states [:D])
    (required! (:observation-encoding m) [:id :revision :support] [:observation-encoding])
    (identity! (:observation-encoding m) [:observation-encoding])
    (demand! (= outcomes (get-in m [:observation-encoding :support])) :encoding-support-mismatch [:observation-encoding])
    (demand! (= {:missing-producer :refuse :zero-mass :retain
                 :impossible-outcome :typed-refusal} (:semantics m)) :fallback-forbidden [:semantics])
    (required! (:parameters m) [:kind :hypotheses :prior] [:parameters])
    (demand! (= :finite-registered-hypotheses (get-in m [:parameters :kind])) :parameter-representation-excluded [:parameters])
    (let [hs (get-in m [:parameters :hypotheses])]
      (support! hs [:parameters :hypotheses])
      (doseq [h hs]
        (identity! h [:parameters :hypotheses])
        (required! h [:registration :likelihood] [:parameters :hypotheses])
        (let [record (measurement! (:registration h) [:parameters :hypotheses :registration])]
          (demand! (= {:schema :wm/parameter-hypothesis-v1
                       :id (:id h) :revision (:revision h) :likelihood (:likelihood h)}
                     (select-keys record [:schema :id :revision :likelihood]))
                   :hypothesis-registration-mismatch [:parameters :hypotheses :registration]))
        (kernel! (:likelihood h) states outcomes [:parameters :hypotheses :likelihood]))
      (support! (mapv :id hs) [:parameters :hypotheses :ids])
      (distribution! (get-in m [:parameters :prior]) (mapv :id hs) [:parameters :prior])))
  {:ok true :schema schema :open-obligations #{:evidence-vocabulary}})

(defn validate
  "Validate explicit finite inputs; pointer checks read pinned local artifacts.
   A successful result is contract admission, not proof of measured dynamics."
  [model]
  (try (validate* model)
       (catch clojure.lang.ExceptionInfo e
         (if-let [kind (:machine-model/refusal (ex-data e))]
           {:ok false :refusal {:kind kind :path (:path (ex-data e))}}
           (throw e)))
       (catch java.io.IOException _
         {:ok false :refusal {:kind :artifact-read-failed :path []}})))

(defn require-outcome-vocabulary [model vertex]
  (let [v (validate model)]
    (if-not (:ok v) v
      (if (= :evidence vertex)
        {:ok false :refusal {:kind :evidence-vocabulary-owed :path [:outcome :vertices :evidence]}}
        (if (= :ruled (get-in model [:outcome :vertices vertex :status]))
          {:ok true :support (get-in model [:outcome :vertices vertex :carrier])}
          {:ok false :refusal {:kind :outcome-vocabulary-unavailable :path [:outcome :vertices vertex]}})))))

(defn require-producer
  "Consumers must ask for an explicit callable producer; no substitution."
  [producers id]
  (if-let [producer (get producers id)]
    (if (fn? producer) {:ok true :producer producer}
        {:ok false :refusal {:kind :invalid-producer :path [:producers id]}})
    {:ok false :refusal {:kind :missing-producer :path [:producers id]}}))
