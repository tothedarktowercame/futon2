(ns futon2.report.wm-regulator-sweep-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.report.war-machine :as wm]
            [futon2.report.wm-regulator-sweep :as sweep]))

(def mission-ids ["M-e" "M-d" "M-c" "M-b" "M-a"])

(defn- mission [id holes]
  {:id id
   :title id
   :status-class :in-progress
   :open-hole-count holes})

(defn- fixture-snapshot []
  (let [holes {"M-a" 1 "M-b" 2 "M-c" 3 "M-d" 4 "M-e" 5}
        missions (mapv #(mission % (holes %)) mission-ids)
        graph {:capabilities {:wm-overnight-unsupervised {:status :held :scope []}}
               :missions (into {}
                               (map (fn [[id n]]
                                      [id {:scope []
                                           :produces []
                                           :open-hole-count n}])
                                    holes))}
        gap-view (into {}
                       (map (fn [[id n]] [id (* 0.3 n)]) holes))]
    {:wm-state {:observation {}
                :belief {}
                :sorrys []
                :missions missions}
     :candidates (mapv (fn [id]
                         {:type :open-mission :target id :weight 1.0})
                       mission-ids)
     :wm-missions missions
     :structure {:capability-graph graph
                 :pre-registered-goal :wm-overnight-unsupervised
                 :mission-gap-view gap-view
                 :mission-domain-view {}}
     :grounding {:centrality {"M-a" 1.0 "M-b" 0.8 "M-c" 0.6 "M-d" 0.4 "M-e" 0.2}
                 :valuable-path #{"M-a" "M-b" "M-c"}
                 :roi-map {"M-a" {:expected-roi-gbp 100.0}
                           "M-c" {:expected-roi-gbp 40.0}}}
     :live-weights {:gap-weight 6.0}}))

(defn- without-live-io [f]
  ;; Pin the arena mode flips too (D5c ambiguity, §15 risk-KL): these tests
  ;; exercise the GAP-WEIGHT regulator axis ceteris paribus, and the fixture's
  ;; expectations are calibrated against the historical hinge/variance-sum
  ;; units. Under :risk-mode :kl the risk term is in nats (×20 dispersion) and
  ;; gap-weight 10.0 no longer dominates — a REAL post-flip regulator-range
  ;; question (E-possible-world-regulator), not a defect in the sweep.
  (with-redefs-fn {#'wm/capability-star-map (fn [] nil)
                   #'wm/mission-gap-view (fn [] nil)
                   #'wm/mission-domain-ratified (fn [] nil)
                   #'wm/arena-risk-mode (fn [] :hinge)
                   #'wm/arena-ambiguity-mode (fn [] :variance-sum)}
    f))

(defn- targets [rollout]
  (mapv #(get-in % [:action :target]) (:ranked rollout)))

(deftest rollout-is-deterministic
  (without-live-io
    (fn []
      (let [snapshot (fixture-snapshot)
            weights {:gap-weight 2.0}
            left (wm/rollout-snapshot-under-weights snapshot weights)
            right (wm/rollout-snapshot-under-weights snapshot weights)]
        (is (= (:ranked left) (:ranked right)))
        (is (= (:bundle left) (:bundle right)))))))

(deftest swept-gap-weight-changes-ranking
  (without-live-io
    (fn []
      (let [snapshot (fixture-snapshot)
            low (wm/rollout-snapshot-under-weights snapshot {:gap-weight 0.0})
            high (wm/rollout-snapshot-under-weights snapshot {:gap-weight 10.0})]
        (is (not= (targets low) (targets high)))
        (is (= "M-a" (first (targets low))))
        (is (= "M-e" (first (targets high))))))))

(deftest score-vector-has-regulator-axes
  (without-live-io
    (fn []
      (let [snapshot (fixture-snapshot)
            moderate (wm/rollout-snapshot-under-weights snapshot {:gap-weight 2.0})
            saturated (wm/rollout-snapshot-under-weights snapshot {:gap-weight 10.0})
            sm (sweep/score-vector snapshot moderate)
            ss (sweep/score-vector snapshot saturated)]
        (is (= #{:useful :automatable :first-autonomous-rank :sustainability :discrimination}
               (set (keys sm))))
        (doseq [k [:useful :automatable :first-autonomous-rank :sustainability :discrimination]]
          (is (number? (get sm k))))
        (is (< (:discrimination ss) (:discrimination sm)))))))

(deftest question-functions-return-numbers-and-enforce-floor
  (let [good {:useful 0.5 :automatable 0.8 :first-autonomous-rank 1
              :sustainability 100.0 :discrimination 3}
        saturated (assoc good :discrimination 1)]
    (testing "ordinary scores are numeric"
      (is (number? (sweep/question-best-bundle good)))
      (is (number? (sweep/question-most-automatable-now good))))
    (testing "discrimination floor refuses saturated worlds"
      (is (= ##-Inf (sweep/question-best-bundle saturated)))
      (is (= ##-Inf (sweep/question-most-automatable-now saturated))))))

(deftest sweep-ranking-and-pareto-front-are-shaped
  (without-live-io
    (fn []
      (let [snapshot (fixture-snapshot)
            swept (sweep/sweep snapshot [{:gap-weight 0.0}
                                         {:gap-weight 2.0}
                                         {:gap-weight 10.0}])
            ranked (sweep/rank-by-question swept sweep/question-best-bundle)]
        (is (= 3 (count swept)))
        (is (= 3 (count (:ranked ranked))))
        (is (seq (:pareto-front ranked)))
        (is (every? #(contains? % :question-score) (:ranked ranked)))))))
