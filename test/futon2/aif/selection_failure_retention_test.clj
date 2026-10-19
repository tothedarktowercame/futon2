(ns futon2.aif.selection-failure-retention-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.wm.loop-node-exercise :as exercise]
            [futon2.test-support.runner-fixture :as fixture]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

;; Live pin: measured-frontier-click-stop.edn, run
;; 2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796. Counts below are
;; deliberately synthetic: that failed live run LOST its frontier counts.
(def action {:kind :cascade-candidate :target "M-custom-harness"
             :id "M-custom-harness/policy-e22941ff9f6a8f30c06a"})
(def frontier {:eligible 2 :assessed 1 :reading-absent 1
               :reading-absent-ids ["M-not-read"]})
(def judgement
  {:decision {:status :selected :action action
              :selection-certificate {:reading-frontier frontier}}
   :outer-task-selection {:schema :wm/outer-task-selection-v1
                          :status :selected :action action
                          :chosen {:id "M-custom-harness"}}})

(deftest selected-judgement-survives-validator-failure-in-written-record
  (binding [runner/*wm-status-reporting?* false]
    (with-redefs [exercise/require-complete!
                  (fn [_]
                    (throw (ex-info "Required loop nodes were not exercised by selection"
                                    {:outcome :selection-validation-failed
                                     :failure-kind :required-loop-node-unexercised
                                     :failure-stage :selection}))) ]
      (let [opts (assoc (fixture/isolated-runner-opts)
                        :run-id "selection-retention-live-pin"
                        :require-loop-node-exercise? true
                        :judge-fn (fn [_] {:judgement judgement}))
            result (#'runner/run-opportunity-core! opts)
            written (runner/persist-run-record! opts (:run-id opts)
                                                "2026-10-19T08:30:00Z" result)
            record (edn/read-string (slurp (:run-record written)))]
        (is (= :required-loop-node-unexercised (get-in record [:failure :kind])))
        (is (= "M-custom-harness" (get-in record [:outer-task-selection :action :target])))
        (is (= action (get-in result [:checkpoints :selection :judgment :controller-decision :action])))
        (is (= frontier (get-in record [:decision :selection-certificate :reading-frontier])))))))

(deftest original-failure-survives-close-wrapper
  (let [record (#'runner/run-record-failure
                {:data {:failure-kind :invalid-close-outcome :failure-stage :close
                        :original-failure {:failure-kind :required-loop-node-unexercised
                                           :failure-stage :selection}}})]
    (is (= :required-loop-node-unexercised (:kind record)))
    (is (= :selection (:stage record)))
    (is (= :invalid-close-outcome (get-in record [:detail :wrapped-by :failure-kind])))))
