(ns futon2.aif.experiment-registration
  "The harness-side gate for experiment-shaped CLean registrations
  (M-experiment-build-match). One EDN registration is the single source:
  clean_to_lean.py renders its formal reading, and THIS namespace derives its
  executable reading — a malli schema the harness validates against BEFORE a
  run starts, so an unregistered arm is a startup failure, not a review
  finding.

  Registration vocabulary (futon6/holes/clean/slice5-confirmation.clean.edn):

    {:clean/experiment  id
     :clean/title       string
     :clean/status      :registered-not-run
     :clean/schema-version  n          ;; the version this registration holds
     :clean/experiment-design
     {:arms [ {:id :aif-full :ablates []} … ]
      :scenarios [ :patchy … ]
      :seeds {:runs-per-cell 30 …}
      :axes […] :stop-rules […] :decision {…}}}

  Amendments are registrations too: a schema-version change is recorded as an
  AMENDMENT EVENT beside the original, never by mutating an in-flight
  registration. A registration validates as-of the version it carries, so a
  later schema tightening cannot silently invalidate (or revalidate) a live
  run."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m]))

(def current-schema-version
  "The schema version new registrations are made under."
  1)

(def known-schema-versions
  "Versions this build can validate against. A registration carrying an
   unknown version is refused, never guessed."
  #{1 2})

(def ^:private version-requirements
  "Per-version required top-level keys. Version 2 additionally requires an
   explicit (possibly empty) :clean/amendments vector, so a registration that
   predates amendments cannot silently pass as current."
  {1 [:clean/experiment :clean/title :clean/status :clean/schema-version
      :clean/experiment-design]
   2 [:clean/experiment :clean/title :clean/status :clean/schema-version
      :clean/experiment-design :clean/amendments]})

(defn- experiment-design-schema [version]
  (let [base [:map
              [:arms [:sequential
                      [:map
                       [:id :keyword]
                       [:ablates [:sequential :keyword]]]]]
              [:scenarios [:sequential :keyword]]
              [:axes [:sequential
                      [:map
                       [:id :keyword]
                       [:score-varies? :boolean]
                       [:justification :string]]]]
              [:seeds [:map [:runs-per-cell pos-int?]]]
              [:stop-rules [:sequential map?]]
              [:decision map?]]]
    (if (= 2 version)
      [:map
       [:arms [:sequential
               [:map
                [:id :keyword]
                [:ablates [:sequential :keyword]]]]]
       [:scenarios [:sequential :keyword]]
       [:axes [:sequential
               [:map
                [:id :keyword]
                [:score-varies? :boolean]
                [:justification :string]]]]
       [:seeds [:map [:runs-per-cell pos-int?]]]
       [:stop-rules [:sequential map?]]
       [:decision map?]
       [:amendments {:optional true} [:sequential map?]]]
      base)))

(defn validate-registration
  "Validate a registration map. `as-of` defaults to the version the
   registration itself carries, so an in-flight registration keeps validating
   under the schema it was registered against even after the current version
   advances. Returns {:ok true :validated-against-version v} or
   {:ok false :errors […] :refusal …}."
  ([registration] (validate-registration registration nil))
  ([registration {:keys [as-of]}]
   (let [version (:clean/schema-version registration)
         as-of (or as-of version)
         errors (cond
                  (not (contains? known-schema-versions version))
                  [(str "unknown :clean/schema-version " (pr-str version)
                        "; known versions " (pr-str (sort known-schema-versions)))]
                  (not (contains? known-schema-versions as-of))
                  [(str "unknown as-of version " (pr-str as-of))]
                  :else
                  (let [missing-required
                        (keep (fn [k]
                                (when-not (contains? registration k)
                                  (str "registration is missing required key " (pr-str k))))
                              (version-requirements as-of))
                        schema [:map
                                [:clean/experiment :keyword]
                                [:clean/title :string]
                                [:clean/status keyword?]
                                [:clean/schema-version int?]
                                [:clean/experiment-design (experiment-design-schema as-of)]
                                (if (= 2 as-of)
                                  [:clean/amendments [:sequential map?]]
                                  [:clean/amendments {:optional true} [:sequential map?]])]
                        explain (m/explain schema registration)]
                    (concat
                     missing-required
                     (mapv (fn [e] (str "registration violates its schema at "
                                        (pr-str (vec (:in e))) ": " (:message e)))
                           (when explain (:errors explain))))))]
     (if (seq errors)
       {:ok false :errors errors :refusal :registration-invalid
        :schema-version version :validated-against-version as-of}
       {:ok true :schema-version version :validated-against-version as-of}))))

(defn load-registration
  "Read and validate an experiment-shaped CLean registration from `path`."
  ([path] (load-registration path nil))
  ([path opts]
   (let [registration (edn/read-string {:default (fn [_ v] v)}
                                       (slurp (io/file path)))]
     (assoc (validate-registration registration opts)
            :registration registration))))

(defn registered-arms
  "The arm ids the registration permits, as a set."
  [registration]
  (set (map :id (get-in registration [:clean/experiment-design :arms]))))

(defn registered-scenarios
  "The scenario ids the registration permits, as a set."
  [registration]
  (set (get-in registration [:clean/experiment-design :scenarios])))

(defn run-config-schema
  "The malli schema for one harness run configuration, DERIVED from the
   registration: arm and scenario are enums of the registered values. This is
   the executable reading of the registration — the harness derives it and
   validates every run configuration against it before the run starts."
  [registration]
  (let [arms (mapv :id (get-in registration [:clean/experiment-design :arms]))
        scenarios (vec (get-in registration [:clean/experiment-design :scenarios]))]
    [:map
     [:arm (into [:enum] arms)]
     [:scenario (into [:enum] scenarios)]
     [:seeds [:map-of :keyword :int]]]))

(defn validate-run-config
  "The pre-start gate. Derives the run-config schema from the registration and
   validates `config` against it BEFORE any run starts. Returns
   {:ok true …} or {:ok false :errors […] :refusal :unregistered-run-config
   :stage :before-run}. An unregistered arm or scenario is a startup failure."
  [registration config]
  (let [schema (run-config-schema registration)
        explain (m/explain schema config)]
    (if explain
      {:ok false :stage :before-run :refusal :unregistered-run-config
       :schema-version (:clean/schema-version registration)
       :errors (mapv (fn [e] (str "run config refused at " (pr-str (vec (:in e)))
                                  " for value " (pr-str (:value e))
                                  ": " (:message e)))
                     (:errors explain))}
      {:ok true :stage :before-run
       :schema-version (:clean/schema-version registration)
       :arm (:arm config) :scenario (:scenario config)})))

(defn amendment
  "Record a schema-version change as an AMENDMENT EVENT. Pure: returns the
   event (and a derived successor registration) and never mutates the
   original — an in-flight run completes under the registration it was
   dispatched against."
  [registration to-version {:keys [changes note]}]
  {:pre [(contains? known-schema-versions to-version)]}
  (let [from (:clean/schema-version registration)]
    {:event {:schema :experiment-registration/amendment-v1
             :amends (:clean/experiment registration)
             :from-schema-version from
             :to-schema-version to-version
             :changes (or changes {})
             :note (or note "schema version change")
             :recorded-as :amendment-not-mutation}
     :successor (-> registration
                    (assoc :clean/schema-version to-version)
                    (update :clean/amendments (fnil conj [])
                            {:from-schema-version from
                             :to-schema-version to-version
                             :note (or note "schema version change")})
                    (vary-meta assoc :derived-from (:clean/experiment registration)))}))
