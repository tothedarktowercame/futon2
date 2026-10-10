(ns futon2.aif.wm-production-replay-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.data-paths :as data-paths]
            [wm-production-replay :as replay])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

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

(deftest production-shaped-duration-events-are-reported
  (is (= {:selection {:elapsed-ms 42 :transition :end}}
         (replay/phase-summary
          [{:phase :selection :transition :start}
           {:phase :selection :transition :end :duration-ms 42}]))))

(deftest attempt-discovery-searches-the-actual-cohort-root
  (let [root (.toFile (Files/createTempDirectory
                       "wm-replay-attempt-test-" (make-array FileAttribute 0)))
        attempt (io/file root "cohort" "attempt-001")]
    (.mkdirs attempt)
    (is (= (.getCanonicalPath attempt)
           (.getCanonicalPath (replay/find-attempt-dir root "attempt-001"))))))

(deftest isolation-change-is-a-typed-refusal
  (is (true? (replay/assert-isolation! {:head "a"} {:head "a"})))
  (try
    (replay/assert-isolation! {:head "a"} {:head "b"})
    (is false "changed canonical state must refuse")
    (catch clojure.lang.ExceptionInfo e
      (is (= :replay-isolation-violation (:failure-kind (ex-data e)))))))

(deftest interpretation-ask-is-explicitly-disabled
  (is (fn? replay/no-interpretation-ask))
  (is (nil? (replay/no-interpretation-ask :any :arguments))))
