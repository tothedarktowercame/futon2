(ns futon2.aif.typed-residual-remediation-test
  "Acceptance tests for the directly remediable typed residuals."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm]))

(def state
  {:observation {:loop-health 0.9
                 :support-coverage 0.9
                 :attack-coverage 0.9
                 :mission-health 0.3
                 :stack-pct 0.2
                 :consulting-pct 0.25
                 :portfolio-pct 0.25
                 :mathematics-pct 0.2
                 :active-repo-ratio 0.8
                 :sorry-count-norm 0.85
                 :coupling-density 0.2
                 :ticks-firing-ratio 0.0
                 :depositing-signal 0.1}
   :belief {}})

(def graph
  {:capabilities {:ready {:status :satisfied}
                  :blocked {:status :held}
                  :goal {:status :held :scope [:out]}
                  :out {:status :held}}
   :missions {"M-ready" {:scope [:ready] :produces [:out] :open-hole-count 2}
              "M-blocked" {:scope [:blocked] :produces [:out] :open-hole-count 1}}})

(deftest telemetry-only-controls-retain-measurements-but-stop-steering
  (let [action {:type :no-op}
        legacy (efe/compute-efe state action)
        retired (efe/compute-efe state action
                                 {:predictability-control-mode :telemetry-only
                                  :homeostatic-control-mode :telemetry-only})]
    (testing "library default remains the explicit historical mode"
      (is (= legacy
             (efe/compute-efe state action
                              {:predictability-control-mode :controller-augmentation
                               :homeostatic-control-mode :controller-augmentation}))))
    (testing "raw diagnostics remain comparable"
      (is (= (:predictability-bonus legacy) (:predictability-bonus retired)))
      (is (= (:homeostatic-pressure legacy) (:homeostatic-pressure retired))))
    (testing "retired diagnostics make exactly zero contribution"
      (is (zero? (get-in retired [:augmentation-terms :info])))
      (is (zero? (get-in retired [:augmentation-terms :survival])))
      (is (= :telemetry-only (:predictability-control-mode retired)))
      (is (= :telemetry-only (:homeostatic-control-mode retired)))
      (is (< (Math/abs
              (- (:controller-score retired)
                 (+ (:controller-score legacy)
                    (* efe/default-info-weight (:predictability-bonus legacy))
                    (- (* efe/default-survival-weight
                          (:homeostatic-pressure legacy))))))
             1.0e-9)))))

(deftest graph-feasibility-is-a-policy-domain-boundary
  (let [actions [{:type :open-mission :target "M-ready"}
                 {:type :open-mission :target "M-blocked"}
                 {:type :open-mission :target "M-off-map"}
                 {:type :no-op}]
        ranked (efe/rank-actions state actions
                                 {:capability-graph graph
                                  :pre-registered-goal :goal
                                  :graph-feasibility-mode :policy-support})
        excluded (-> ranked meta :policy-support/excluded)]
    (is (= #{[:open-mission "M-ready"] [:no-op nil]}
           (set (map (juxt #(get-in % [:action :type])
                           #(get-in % [:action :target])) ranked))))
    (is (= #{:required-capabilities-unsatisfied
             :mission-absent-from-capability-graph}
           (set (map :reason excluded))))
    (is (every? #(nil? (:controller-score %)) excluded)
        "excluded policies are classified, never value-scored")))

(deftest policy-support-removes-mask-from-value
  (let [action {:type :open-mission :target "M-blocked"}
        legacy (efe/graph-control-terms graph :goal action {})
        split (efe/graph-control-terms graph :goal action
                                       {:graph-feasibility-mode :policy-support})]
    (is (pos? (:graph-feasibility-penalty split)))
    (is (= (:graph-control-score-proxy split) (:graph-control-score split)))
    (is (= (+ (:graph-feasibility-penalty legacy)
              (:graph-control-score-proxy legacy))
           (:graph-control-score legacy)))))

(deftest live-arena-activates-the-repaired-dispositions
  (let [flags (wm/arena-mode-flags)]
    (is (= :telemetry-only (:predictability-control-mode flags)))
    (is (= :telemetry-only (:homeostatic-control-mode flags)))
    (is (= :policy-support (:graph-feasibility-mode flags)))))
