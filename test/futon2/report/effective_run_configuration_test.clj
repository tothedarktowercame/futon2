(ns futon2.report.effective-run-configuration-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.anticipation :as anticipation]
            [futon2.aif.policy-depth :as depth]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]))

(deftest loaded-values-and-evaluated-depth
  (let [file (java.io.File/createTempFile "effective-events" ".edn")
        opts {:run-id "test-run" :policy-depth {:anticipation 3 :cascade-rollout 5}}
        flag "FUTON_WM_FPI_POSTERIOR"
        ;; Deliberately contradict this tooling process's environment.
        opposite (not (= "1" (System/getenv flag)))]
    (try
      (binding [wm/*f-pi-posterior?* opposite wm/*f-pi-dark?* true
                trace/*persist-policy-trace-details?* true]
        (let [loaded (wm/effective-run-configuration opts)]
          (is (= opposite (:fpi-posterior? loaded)))
          (is (true? (:policy-details? loaded)))
          (is (true? (:fpi-dark? loaded)))
          (is (= :not-reached (:evaluation loaded)))
          (is (not (contains? loaded :effective-depth)))
          (doseq [[events h d] [[[] nil 1]
                               [[{:event/id "due" :event/at "2026-09-16T00:00:00Z"}] 3 3]]]
            (spit file (pr-str {:events events}))
            (let [snapshot (anticipation/anticipation-snapshot
                            {:path (.getPath file) :now (java.time.Instant/parse "2026-09-15T00:00:00Z")})
                  evaluated (wm/evaluated-run-configuration
                             loaded opts (:policy-depth opts) snapshot
                             (depth/anticipation snapshot (:policy-depth opts)) {})]
              (is (= h (:effective-horizon evaluated)))
              (is (= d (:effective-depth evaluated)))
              (is (= (:source-sha256 snapshot) (get-in evaluated [:anticipation :source-sha256])))
              (is (= (mapv :event/id events) (get-in evaluated [:anticipation :eligible-event-ids])))
              (is (= "test-run" (:run/id evaluated)))))))
      (finally (.delete file)))))

(deftest incomplete-posterior-is-recorded-from-decision
  (binding [wm/*f-pi-posterior?* true]
    (let [fields (wm/f-pi-posterior-opts {:f-pi-by-candidate-id {:status :present :by-candidate-id {}}}
                                       [{:action {:type :no-op}}])
          decision (policy/select-action
                    [{:action {:type :no-op} :controller-score 1.0}]
                    {:selection-boundary :strategic-recommendation :f-pi-opts fields})
          result (wm/evaluated-run-configuration
                  (wm/effective-run-configuration {}) {} nil {:events []}
                  {:horizon-steps nil} decision)]
      (is (= :incomplete-coverage (get-in result [:fpi-posterior :reason])))
      (is (false? (get-in result [:fpi-posterior :applied?])))
      (is (= (:f-pi-posterior decision) (:fpi-posterior result))))))
