(ns c-module-v1
  "Replay the current-paper pilot through C v1. Run from futon2 with
   clojure -M checks/c_module_v1.clj. No live endpoints or shared-JVM loads."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [futon2.aif.preference-module :as c]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(defn sha256 [path]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (java.nio.file.Files/readAllBytes
                         (.toPath (java.io.File. path))))]
    (format "%064x" (java.math.BigInteger. 1 digest))))

(doseq [[path expected] (edn/read-string
                        (slurp "holes/labs/wm-contract/runs/C-module-v1/source-pins.edn"))]
  (when-not (= expected (sha256 path))
    (throw (ex-info "C module replay source changed" {:path path}))))

(let [profile (edn/read-string (slurp "resources/c-modules/current-work-v1.edn"))
      prior (json/parse-string
             (slurp "holes/labs/wm-contract/runs/C-current-paper-diagnostic/result.json") true)
      readings (into {} (for [[id value] (:current-satisfaction prior)
                             :when (boolean? value)]
                         [id {:version 1 :criterion (str (name id) "/v1")
                              :status :observed :value value
                              :evidence "C-current-paper-diagnostic/result.json@befdd98f"}]))
      corrected (assoc-in readings [:claim-warrant :value] true)
      seed {:support (mapv #(vector :organization %) (sort (:support ruled/seeded-c)))
            :mass (into {} (map (fn [[k v]] [[:organization k] v]) (:mass ruled/seeded-c)))}
      module {:schema :preference-module/v1 :id :recorded-seed :version 1
              :context :wm-flight-dispositions
              :entries [{:id :disposition :version 1 :criterion "flight-disposition/v1"
                         :kind :finite :distribution seed
                         :source {:ref "src/futon2/aif/ruled_outcome_c.clj"
                                  :basis "D1 illustrative seed, not a new ruling"
                                  :status :illustrative-seed}}]}
      q (fn [d] {:support [[:organization d]] :mass {[:organization d] 1}})
      action (fn [id d]
               {:id id :type :no-op
                :preference-readings
                {:disposition {:version 1 :criterion "flight-disposition/v1"
                               :status :predicted :distribution (q d)
                               :evidence "synthetic-adapter-control-not-live-forecast"}}})
      comparison (efe/rank-local-preference-actions
                  {:observation {:loop-health 0.8}}
                  [(action :build-failure :build-failed) (action :grounded :grounded-change)]
                  {:preference-module module} :disposition)]
  (pp/pprint
   {:schema :c-module-v1-replay
    :paper-assessment (c/assess profile readings)
    :hypothetical-correction (c/compare-satisfaction profile readings corrected)
    :seed-risk (c/risk seed (q :grounded-change))
    :seed-zero-refusal (c/risk seed (q :cancelled))
    :wm-local-ranking (:local-preference-ranking comparison)
    :wm-adapter-controls :synthetic-predictions
    :production-observation-model :not-supplied
    :numeric-preference-authority :existing-illustrative-seed-only}))
