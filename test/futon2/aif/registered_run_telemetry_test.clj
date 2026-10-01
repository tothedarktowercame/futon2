(ns futon2.aif.registered-run-telemetry-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.registered-run-telemetry :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest aggregates-monotonic-phase-ends
  (is (= {:select 7 :wait 11}
         (:phase-timings-ms
          (sut/phase-timings [{:phase :select :transition :start}
                              {:phase :select :transition :end :duration-ms 7}
                              {:phase :wait :transition :end :duration-ms 11}])))))

(deftest aggregates-provider-usage-without-imputation
  (let [r (sut/model-usage
           {:author-job {:job-id "a" :usage {:input_tokens 10
                                               :output_tokens 4
                                               :total_tokens 14}}
            :review-job {:job-id "r" :usage {:prompt_tokens 7
                                               :completion_tokens 3}}})]
    (is (= :complete (:status r)))
    (is (= [17 7 24] ((juxt :input-tokens :output-tokens :total-tokens) r)))))

(deftest missing-provider-receipt-stays-typed
  (let [r (sut/model-usage {:author-job {:job-id "a" :usage {:input_tokens 1
                                                               :output_tokens 2}}
                            :review-job {:job-id "r"}})]
    (is (= :partial (:status r)))
    (is (= ["r"] (:missing-job-ids r)))))

(deftest aggregates-zai-cost-schema-and-retains-model
  (let [r (sut/model-usage
           {:review-job {:job-id "z"
                         :usage {:cost/input-tokens 9
                                 :cost/output-tokens 4
                                 :cost/total-tokens 13
                                 :cost/source :zai
                                 :cost/model "glm-4.6"}}})]
    (is (= :complete (:status r)))
    (is (= 13 (:total-tokens r)))
    (is (= {:input-tokens 9 :output-tokens 4 :total-tokens 13
            :model "glm-4.6" :provider :zai :job-id "z"}
           (first (:jobs r))))))

(deftest chronology-cites-revisions-and-change
  (with-redefs [sut/source-revisions
                (fn [_] {"futon2" "after" "futon3-pattern-library" "patterns"})]
    (let [start {:prior-opportunity {:source-revisions-after {"futon2" "before"}}
                 :source-revisions-before {"futon2" "after"}}
          r (sut/chronology-finish start {} {:freshness {:missions 3}})]
      (is (= "patterns" (:pattern-library-digest r)))
      (is (= {:before "before" :after "after"}
             (get-in r [:intervening-change-receipts "futon2"])))
      (is (= 64 (count (:substrate-snapshot-digest r)))))))

(deftest production-record-boundary-retains-all-three-preflight-surfaces
  (let [dir (.toFile (Files/createTempDirectory
                      "registered-telemetry-" (make-array FileAttribute 0)))
        phases (atom [{:phase :selection :transition :end :duration-ms 9}])
        opts {:run-record-dir (.getPath dir)
              :scan-render-fn (fn [& _] nil)
              :phase-events/state phases
              :run-timing/start-nanos 0
              :nano-time-fn (constantly 12000000)
              :registered-run/source-roots {"futon2" (.getPath dir)
                                             "futon3-pattern-library" (.getPath dir)}
              :registered-run/chronology-start
              {:schema :wm/registered-run-chronology-v1
               :source-revisions-before {"futon2" "a"
                                         "futon3-pattern-library" "p"}}}
        result {:outcome :offline-selection-replay
                :author-job {:job-id "a" :usage {:input_tokens 2
                                                  :output_tokens 1}}
                :review-job {:job-id "r" :usage {:input_tokens 3
                                                  :output_tokens 2}}
                :checkpoints {}}]
    (with-redefs [sut/source-revisions
                  (fn [_] {"futon2" "b" "futon3-pattern-library" "q"})]
      (let [written (#'runner/persist-run-record!
                     opts "telemetry-001" "2026-10-01T00:00:00Z" result)
            record (edn/read-string (slurp (:run-record written)))]
        (is (= 12 (get-in record [:registered-run/timing :wall-clock-ms])))
        (is (= 9 (get-in record [:registered-run/timing
                                 :phase-timings-ms :selection])))
        (is (= 8 (get-in record [:registered-run/model-usage :total-tokens])))
        (is (= "q" (get-in record [:registered-run/chronology
                                    :pattern-library-digest])))
        (is (= 64 (count (get-in record [:registered-run/chronology
                                         :substrate-snapshot-digest]))))))))
