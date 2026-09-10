(ns futon2.aif.run4-task-pin
  "Pure validation for immutable RUN4 operator-selected task pins.

  Validation establishes identity and freshness only.  It does not select,
  dispatch, accept, or declare a trial ready."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.mission-registry :as missions]))

(def schema :wm/run4-task-pin-v1)

(defn- refuse! [reason & [data]]
  (throw (ex-info "RUN4 task pin refused"
                  (merge {:refused? true :reason reason} data))))

(defn- parse-one [text]
  (try
    (with-open [reader (java.io.PushbackReader.
                        (java.io.StringReader. text))]
      (let [value (edn/read reader)]
        (when-not (= ::end (edn/read {:eof ::end} reader))
          (refuse! :trailing-pin-form))
        value))
    (catch Exception e
      (if (:refused? (ex-data e))
        (throw e)
        (refuse! :invalid-pin-edn)))))

(defn- required-string! [value reason]
  (when (str/blank? (str value)) (refuse! reason))
  value)

(defn- verify-source! [read-text {:keys [path sha256]}]
  (required-string! path :missing-source-path)
  (required-string! sha256 :missing-source-digest)
  (let [text (try
               (read-text path)
               (catch Exception _ (refuse! :unreadable-source {:path path})))]
    (when-not (= sha256 (digest/sha256 text))
      (refuse! :stale-source {:path path}))
    {:path path :sha256 sha256}))

(defn validate
  "Validate PIN-TEXT using injected read-only ports.

  Ports are `:read-text`, `:resolve-mission`, and `:action-admissible?`.
  The latter two must describe the current mission/action substrate.  The
  returned envelope is evidence of pin validity, never execution readiness."
  [pin-text {:keys [read-text resolve-mission action-admissible?]}]
  (when-not (and (fn? read-text) (fn? resolve-mission)
                 (fn? action-admissible?))
    (refuse! :missing-read-port))
  (let [pin (parse-one pin-text)
        {:keys [series-id trial-id series-order candidate-task-ids selected-task-id
                sources casting operator-selection config mapping]} pin]
    (when-not (= schema (:schema pin)) (refuse! :unknown-schema))
    (required-string! series-id :missing-series-id)
    (when (nil? trial-id) (refuse! :missing-trial-id))
    (when-not (= :as-declared series-order) (refuse! :unknown-series-order))
    (when-not (and (vector? candidate-task-ids) (seq candidate-task-ids))
      (refuse! :missing-candidate-set))
    (when-not (= (count candidate-task-ids)
                 (count (distinct candidate-task-ids)))
      (refuse! :duplicate-candidate-id))
    (when-not (some #{selected-task-id} candidate-task-ids)
      (refuse! :selected-id-absent))
    (when-not (= trial-id selected-task-id)
      (refuse! :trial-selection-mismatch))
    (when-not (and (vector? sources) (seq sources))
      (refuse! :missing-source-pins))
    (let [verified-sources (mapv #(verify-source! read-text %) sources)
          verified-config (verify-source! read-text config)
          author (required-string! (:author casting) :missing-author)
          reviewer (required-string! (:reviewer casting) :missing-reviewer)
          _ (when (= author reviewer) (refuse! :author-is-reviewer))
          _ (when-not (= :operator-selected (:mode operator-selection))
              (refuse! :unknown-selection-mode))
          operator (required-string! (:operator operator-selection)
                                     :missing-operator)
          authority-ref (required-string! (:authority-ref operator-selection)
                                          :missing-selection-authority)
          mission-id (required-string! (:mission-id mapping)
                                       :missing-mission-mapping)
          action (:action mapping)
          mission (resolve-mission mission-id)
          _ (when-not mission (refuse! :unknown-mission-mapping))
          _ (when-not (= mission-id (:id mission))
              (refuse! :mission-identity-mismatch))
          _ (when-not (= mission-id
                         (missions/mission-target-id (:target action)))
              (refuse! :action-mission-mismatch))
          _ (when-not (action-admissible? mission action)
              (refuse! :inadmissible-action-mapping))]
      {:schema :wm/run4-task-identity-envelope-v1
       :valid? true
       :task-pin {:sha256 (digest/sha256 pin-text)
                  :series-id series-id
                  :trial-id trial-id
                  :series-order series-order
                  :selected-task-id selected-task-id
                  :ordered-task-ids candidate-task-ids}
       :source-pins verified-sources
       :config-pin verified-config
       :casting casting
       :operator-selection
       {:mode :operator-selected
        :operator operator
        :authority-ref authority-ref
        :outer-loop-ranking-match-required? false
        :inner-policy-selection-required-before-execution? true
        :silent-selector-override-permitted? false}
       :mission-action {:mission mission :action action}
       :executability {:status :not-evaluated}
       :acceptance {:status :not-evaluated}
       :launch {:permitted? false :reason :validation-only}})))
