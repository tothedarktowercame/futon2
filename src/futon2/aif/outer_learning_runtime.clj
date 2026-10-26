(ns futon2.aif.outer-learning-runtime
  "Production composition for the default-off/live-shadow outer learner."
  (:require [clojure.edn :as edn]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-outcome-dataset :as dataset]
            [futon2.aif.outer-learning-manifest :as learning-manifest]
            [futon2.aif.outer-target-belief :as belief]))

(def schema :wm/outer-learning-authority-v1)
(def default-authority-path
  (str (System/getProperty "user.home")
       "/code/futon2/resources/wm/outer-learning-authority.edn"))

(defn- manifest-from [entries]
  (let [body {:schema dataset/manifest-schema :entries (vec entries)}]
    (assoc body :source-pin
           (dataset/manifest-source-pin entries))))

(defn- exception-refusal
  ([reason path exception]
   (exception-refusal reason path exception nil))
  ([reason path exception extra-details]
   {:mode :shadow
    :belief {:status :refused
             :reason reason
             :details (merge {:path path
                              :error-class (.getName (class exception))
                              :error-message (.getMessage exception)}
                             extra-details)}}))

(defn- read-authority [path]
  (let [bytes (java.nio.file.Files/readAllBytes
               (.toPath (java.io.File. path)))]
    {:bytes bytes
     :authority (edn/read-string (String. ^bytes bytes "UTF-8"))}))

(defn- merged-manifest [authority]
  (let [dynamic (learning-manifest/read-manifest)
        entries (->> (concat (:run-records authority) (:entries dynamic))
                     (reduce (fn [by-path entry]
                                (if-let [prior (get by-path (:path entry))]
                                  (if (= (dataset/record-entry prior)
                                         (dataset/record-entry entry))
                                    (assoc by-path (:path entry)
                                           (merge prior entry))
                                    (throw (ex-info "Conflicting outer-learning pins"
                                                    {:path (:path entry)})))
                                  (assoc by-path (:path entry) entry))) {})
                     vals (sort-by :path) vec)]
    (manifest-from entries)))

(defn load-shadow
  ([] (load-shadow default-authority-path))
  ([path]
   (let [authority-stage
         (try
           {:value (read-authority path)}
           (catch Exception exception
             {:refusal (exception-refusal
                        :learning-authority-unavailable path exception)}))]
     (if-let [refusal (:refusal authority-stage)]
       refusal
       (let [{:keys [bytes authority]} (:value authority-stage)
             manifest-stage
             (try
               {:value (merged-manifest authority)}
               (catch Exception exception
                 {:refusal
                  (exception-refusal
                   :learning-manifest-unreadable
                   learning-manifest/default-path
                   exception
                   (when-let [conflicting-path (:path (ex-data exception))]
                     {:conflicting-path conflicting-path}))}))]
         (if-let [refusal (:refusal manifest-stage)]
           refusal
           (let [manifest (:value manifest-stage)
                 dataset-stage
                 (try
                   {:value (dataset/produce
                            {:manifest manifest
                             :expected-manifest-pin (:source-pin manifest)})}
                   (catch Exception exception
                     {:refusal
                      (exception-refusal
                       :learning-dataset-failed
                       learning-manifest/default-path exception)}))]
             (if-let [refusal (:refusal dataset-stage)]
               refusal
               (let [outcomes (:value dataset-stage)
                     prior-spec (:prior authority)
                     belief-stage
                     (try
                       {:value (belief/build
                                {:dataset outcomes
                                 :prior (belief/prior
                                         (:concentration prior-spec)
                                         (:authority prior-spec))})}
                       (catch Exception exception
                         {:refusal
                          (exception-refusal
                           :learning-belief-failed
                           learning-manifest/default-path exception)}))]
                 (if-let [refusal (:refusal belief-stage)]
                   refusal
                   (let [learned (:value belief-stage)]
                     (if (not= schema (:schema authority))
                       {:mode (:mode authority)
                        :belief {:status :refused
                                 :reason :learning-authority-schema-invalid}}
                       {:mode (:mode authority)
                        :authority authority
                        :dataset outcomes
                        :belief learned
                        :preference-authority
                        {:kind (get-in authority [:preferences :authority])
                         :source {:path path :sha256 (field/sha256 bytes)}}
                        :preference-costs (get-in authority [:preferences :costs])
                        :preference-transform (get-in authority
                                                      [:preferences :transform])
                         :controller (:controller authority)}))))))))))))
