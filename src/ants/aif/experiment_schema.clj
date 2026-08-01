(ns ants.aif.experiment-schema
  "Executable validation for CLean experiment registrations.

   The registration EDN is authoritative.  Its experiment design is turned
   into a Malli schema whose closed values must match the live harness before
   an executor is called."
  (:require [clojure.edn :as edn]
            [malli.core :as m]
            [malli.error :as me]))

(def slice5-confirmation-registration
  "../futon6/holes/clean/slice5-confirmation.clean.edn")

(defn read-registration
  [path]
  (edn/read-string (slurp path)))

(defn experiment-design
  [registration]
  (or (:clean/experiment-design registration)
      (throw (ex-info "CLean registration has no experiment design"
                      {:registration (:clean/experiment registration)}))))

(defn design->harness-config
  "Project the registered design onto the values owned by the live harness."
  [design]
  {:arms (mapv :id (:arms design))
   :scenarios (:scenarios design)
   :environment (:environment design)
   :seeds (:seeds design)})

(defn experiment-design-schema
  "Derive the closed Malli schema for a harness from a CLean experiment design.
   Arm/scenario order is immaterial, but membership and cardinality are exact."
  [design]
  (let [{:keys [arms scenarios environment seeds]}
        (design->harness-config design)
        arm-set (set arms)
        scenario-set (set scenarios)]
    [:map {:closed true}
     [:arms
      [:and
       [:vector (into [:enum {:error/message "arm is absent from the registration"}]
                      arms)]
       [:fn {:error/message "registered and harness arms must be identical"}
        (fn [xs] (and (= (count arms) (count xs))
                      (= arm-set (set xs))))]]]
     [:scenarios
      [:and
       [:vector (into [:enum] scenarios)]
       [:fn {:error/message "registered and harness scenarios must be identical"}
        (fn [xs] (and (= (count scenarios) (count xs))
                      (= scenario-set (set xs))))]]]
     [:environment [:= {} environment]]
     [:seeds [:= {} seeds]]]))

(defn validate-harness!
  "Validate a harness against registration EDN. Returns the harness or throws
   before simulation begins. Ex-data includes directional arm differences."
  [registration harness]
  (let [design (experiment-design registration)
        schema (experiment-design-schema design)
        registered (design->harness-config design)]
    (when-not (m/validate schema harness)
      (throw
       (ex-info "Experiment harness does not match its CLean registration"
                {:errors (me/humanize (m/explain schema harness))
                 :unregistered-arms (vec (remove (set (:arms registered))
                                                 (:arms harness)))
                 :unimplemented-arms (vec (remove (set (:arms harness))
                                                  (:arms registered)))})))
    harness))

(defn validate-file!
  [registration-path harness]
  (validate-harness! (read-registration registration-path) harness))

(defn validate-then-run!
  "Startup boundary: `executor` cannot be entered until validation succeeds."
  [registration-path harness executor]
  (validate-file! registration-path harness)
  (executor harness))
