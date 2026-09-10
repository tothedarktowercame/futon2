(ns futon2.aif.run4-task-pin
  "Pure validation for immutable RUN4 operator-selected task pins.

  Validation establishes identity and freshness only.  The pin digest names
  the exact UTF-8 EDN bytes (including whitespace), not a canonicalized EDN
  value.  Validation does not select, dispatch, accept, or declare readiness."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.mission-registry :as missions]))

(def schema :wm/run4-task-pin-v1)

(defn- refuse! [reason & [data]]
  (throw (ex-info "RUN4 task pin refused"
                  (merge {:refused? true :reason reason} data))))

(defn- parse-one [text]
  (when-not (string? text) (refuse! :invalid-pin-text))
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
  (when-not (and (string? value) (not (str/blank? value))) (refuse! reason))
  value)

(defn- identifier? [value]
  (let [token (cond
                (keyword? value) (subs (str value) 1)
                (string? value) value
                :else nil)]
    (boolean (and token
                  (re-matches #"[A-Za-z0-9][A-Za-z0-9._:/-]*" token)))))

(defn- required-id! [value reason]
  (when-not (identifier? value) (refuse! reason))
  value)

(defn- verify-source! [read-text source]
  (when-not (map? source) (refuse! :invalid-source-pin))
  (let [{:keys [path sha256]} source]
    (required-string! path :invalid-source-path)
    (when-not (and (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
      (refuse! :invalid-source-digest {:path path}))
    (let [text (try
                 (read-text path)
                 (catch Exception _ (refuse! :unreadable-source {:path path})))]
      (when-not (string? text) (refuse! :invalid-source-content {:path path}))
      (when-not (= sha256 (digest/sha256 text))
        (refuse! :stale-source {:path path}))
      {:path path :sha256 sha256})))

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
    (required-id! series-id :invalid-series-id)
    (required-id! trial-id :invalid-trial-id)
    (when-not (= :as-declared series-order) (refuse! :unknown-series-order))
    (when-not (and (vector? candidate-task-ids) (seq candidate-task-ids))
      (refuse! :missing-candidate-set))
    (when-not (every? identifier? candidate-task-ids)
      (refuse! :invalid-candidate-id))
    (required-id! selected-task-id :invalid-selected-task-id)
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
          _ (when-not (map? casting) (refuse! :invalid-casting))
          author (required-string! (:author casting) :invalid-author)
          reviewer (required-string! (:reviewer casting) :invalid-reviewer)
          _ (required-string! (:repair-reviewer casting)
                              :invalid-repair-reviewer)
          _ (when (= author reviewer) (refuse! :author-is-reviewer))
          _ (when-not (= :operator-selected (:mode operator-selection))
              (refuse! :unknown-selection-mode))
          operator (required-string! (:operator operator-selection)
                                     :invalid-operator)
          authority-ref (required-string! (:authority-ref operator-selection)
                                          :invalid-selection-authority)
          mission-id (required-string! (:mission-id mapping)
                                       :invalid-mission-mapping)
          action (:action mapping)
          _ (when-not (map? action) (refuse! :invalid-action-mapping))
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
                  :digest-semantics :exact-utf8-pin-bytes
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
        :authority-status :declared-not-authenticated
        :authority-authentication-required-at-serving-boundary? true
        :outer-loop-ranking-match-required? false
        :inner-policy-selection-required-before-execution? true
        :silent-selector-override-permitted? false}
       :mission-action {:mission mission :action action}
       :executability {:status :not-evaluated}
       :acceptance {:status :not-evaluated}
       :launch {:permitted? false :reason :validation-only}})))
