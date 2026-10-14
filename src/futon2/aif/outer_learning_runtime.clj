(ns futon2.aif.outer-learning-runtime
  "Production composition for the default-off/live-shadow outer learner."
  (:require [clojure.edn :as edn]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-outcome-dataset :as dataset]
            [futon2.aif.outer-target-belief :as belief]))

(def schema :wm/outer-learning-authority-v1)
(def default-authority-path
  (str (System/getProperty "user.home")
       "/code/futon2/resources/wm/outer-learning-authority.edn"))

(defn- manifest-from [entries]
  (let [body {:schema dataset/manifest-schema :entries (vec entries)}]
    (assoc body :source-pin
           {:path "wm://run-record-manifest-v1" :sha256 (field/digest body)})))

(defn load-shadow
  ([] (load-shadow default-authority-path))
  ([path]
   (try
     (let [bytes (java.nio.file.Files/readAllBytes
                  (.toPath (java.io.File. path)))
           authority (edn/read-string (String. ^bytes bytes "UTF-8"))
           manifest (manifest-from (:run-records authority))
           outcomes (dataset/produce
                     {:manifest manifest
                      :expected-manifest-pin (:source-pin manifest)})
           prior-spec (:prior authority)
           learned (belief/build
                    {:dataset outcomes
                     :prior (belief/prior (:concentration prior-spec)
                                          (:authority prior-spec))})]
       (if (not= schema (:schema authority))
         {:mode :shadow
          :belief {:status :refused :reason :learning-authority-schema-invalid}}
         {:mode :shadow
          :belief learned
          :preference-authority
          {:kind (get-in authority [:preferences :authority])
           :source {:path path :sha256 (field/sha256 bytes)}}
          :preference-costs (get-in authority [:preferences :costs])
          :controller (:controller authority)}))
     (catch Throwable t
       {:mode :shadow
        :belief {:status :refused :reason :learning-authority-unavailable
                 :details {:path path :error-class (.getName (class t))
                           :error-message (.getMessage t)}}}))))
