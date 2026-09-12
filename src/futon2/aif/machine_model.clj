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

(defn- support! [xs path]
  (demand! (and (vector? xs) (seq xs)) :missing-support path)
  (demand! (= (count xs) (count (set xs))) :duplicate-support path))

(defn- distribution! [row support path]
  (demand! (map? row) :missing-distribution path)
  (demand! (= (set (keys row)) (set support)) :distribution-support-mismatch path)
  (doseq [v (vals row)]
    (demand! (and (number? v) (Double/isFinite (double v)) (not (neg? v)))
             :invalid-mass path))
  ;; Exact arithmetic when inputs are ratios; no silent epsilon or renormalization.
  (demand! (== 1 (reduce + (vals row))) :unnormalized-row path))

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
