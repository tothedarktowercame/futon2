(ns futon2.aif.wm-production-replay-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.data-paths :as data-paths]
            [wm-production-replay :as replay]))

(deftest checkpoint-judgement-retains-the-real-decision
  (let [decision {:action {:target "T-real" :kind :cascade-candidate}
                  :selection-law {:applied :cascade-selection-posterior}}
        j (replay/checkpoint->judgement
           {:judgment {:controller-decision decision
                       :outer-task-selection {:enumerated 5381}
                       :effective-run-configuration {:evaluation :complete}}})]
    (is (= decision (:decision j)))
    (is (= {:enumerated 5381} (:outer-task-selection j)))
    (is (= {:evaluation :complete} (:effective-run-configuration j)))))

(deftest checkpoint-event-payload-is-unwrapped
  (let [decision {:action {:target "T-real" :kind :cascade-candidate}}
        j (replay/checkpoint->judgement
           {:checkpoint/type :selection
            :payload {:judgment {:controller-decision decision}}})]
    (is (= decision (:decision j)))))

(deftest checkpoint-without-an-enacted-decision-is-refused
  (testing "the replay cannot silently substitute a synthetic selection"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"no enacted controller decision"
                          (replay/checkpoint->judgement
                           {:judgment {:controller-decision {:status :abstained}}})))))

(deftest test-process-selects-its-data-root-before-namespace-load
  (is (true? data-paths/test-mode?))
  (is (string? data-paths/test-data-root))
  (is (not= data-paths/production-data-root data-paths/test-data-root)))
