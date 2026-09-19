(require '[futon2.aif.repair-obligation :as repair]
         '[clojure.java.io :as io]
         '[clojure.edn :as edn])
(def packet "holes/labs/wm-contract/runs/hermetic-fixtures-2026-09-19/")
(def ids (mapv #(.getName %) (filter #(re-find #"^repair-ea1-(a7a5fc7c|b28b40fe).*\.edn$" (.getName %))
                                   (.listFiles (io/file repair/default-root "findings")))))
(defn eligible [] (count (filter #(and (= :open (:repair/status %))
                                      (not= :environmental-hold (:repair/class %)))
                                (repair/open-obligations))))
(def before (eligible))
(prn :before-eligible before)
(doseq [file ids
        :let [obligation (edn/read-string (slurp (io/file repair/default-root "findings" file)))
              implementation {:attempt-id "hermetic-fixtures-2026-09-19-codex-2"
                              :commit "6cdb308a"
                              :warrant-id "test-registry-15940d9edb71eff4d10a9a103d5bd5071a5e8e0f6e9e9d58d4df83ef3560deaf"
                              :witness {:scope :isolated-regression
                                        :production-shaped? false}}]]
  (try
    (prn :implementation (repair/record-implementation! obligation implementation))
    (catch clojure.lang.ExceptionInfo e
      (spit (str packet file ".refusal.edn") (pr-str {:message (.getMessage e) :data (ex-data e)}))
      (prn :finding (:repair/id obligation) :message (.getMessage e)
           :refusal (dissoc (ex-data e) :obligation :implementation)))))
(prn :after-eligible (eligible))
(shutdown-agents)
