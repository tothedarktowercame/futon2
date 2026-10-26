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

(defn load-shadow
  ([] (load-shadow default-authority-path))
  ([path]
   (try
     (let [bytes (java.nio.file.Files/readAllBytes
                  (.toPath (java.io.File. path)))
           authority (edn/read-string (String. ^bytes bytes "UTF-8"))
           dynamic (learning-manifest/read-manifest)
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
                        vals (sort-by :path) vec)
           manifest (manifest-from entries)
           outcomes (dataset/produce
                     {:manifest manifest
                      :expected-manifest-pin (:source-pin manifest)})
           prior-spec (:prior authority)
           learned (belief/build
                    {:dataset outcomes
                     :prior (belief/prior (:concentration prior-spec)
                                          (:authority prior-spec))})]
       (if (not= schema (:schema authority))
         {:mode (:mode authority)
          :belief {:status :refused :reason :learning-authority-schema-invalid}}
         {:mode (:mode authority)
          :authority authority
          :dataset outcomes
          :belief learned
          :preference-authority
          {:kind (get-in authority [:preferences :authority])
           :source {:path path :sha256 (field/sha256 bytes)}}
          :preference-costs (get-in authority [:preferences :costs])
          :preference-transform (get-in authority [:preferences :transform])
          :controller (:controller authority)}))
     (catch Throwable t
       {:mode :shadow
        :belief {:status :refused :reason :learning-authority-unavailable
                 :details {:path path :error-class (.getName (class t))
                           :error-message (.getMessage t)}}}))))
