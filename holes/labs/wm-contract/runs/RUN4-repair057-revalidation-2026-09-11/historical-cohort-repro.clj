(require '[clojure.edn :as edn]
         '[futon2.aif.full-loop-runner :as r]
         '[futon2.aif.full-loop-runner-test :as rt]
         '[futon2.aif.full-loop-cohort :as c]
         '[futon2.aif.c-fold-config :as d]
         '[futon2.aif.hermetic-repair-fixture :as h])
(h/with-hermetic-stores
 (fn []
   (binding [r/*wm-status-reporting?* false]
     (let [root (.toString (java.nio.file.Files/createTempDirectory
                            "historical-cohort-review" (make-array java.nio.file.attribute.FileAttribute 0)))
           path (str root "/cohort.edn")
           raw (pr-str (-> (edn/read-string (slurp c/default-preregistration))
                           (assoc :cohort/id :historical-review)
                           (assoc-in [:stopping-rule :target] 1)))
           stop-line {:repair/id "repair-057" :repair/status :open
                      :repair/class :machine-failure :attempt-id "failed-057"}
           admission {:schema :wm/historical-repair-admission-v1
                      :repair/id "repair-057" :repair/status :awaiting-validation
                      :verification-id "v" :actors {:author "zai-5" :reviewer "codex-1"}}
           executed (atom 0)]
       (spit path raw)
       (c/activate! path root)
       (let [result (r/run-opportunity!
                     (merge (rt/isolated-runner-opts)
                            {:cohort? true
                             :execution-cohort {:preregistration path :data-root root
                                                :cohort-id :historical-review :sha256 (d/sha256 raw)}
                             :repair-open-fn (constantly [stop-line])
                             :historical-verification-candidate-fn (constantly admission)
                             :historical-verification-execute-fn
                             (fn [_] (swap! executed inc) admission)}))
             ledger (c/ledger path root)]
         (prn {:execution-count @executed :outcome (:outcome result)
               :error (get-in result [:data :error])
               :attempt-count (:attempt-count ledger) :closed-count (:closed-count ledger)}))))))
