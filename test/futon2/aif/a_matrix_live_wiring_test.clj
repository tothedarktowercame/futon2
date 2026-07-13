(ns futon2.aif.a-matrix-live-wiring-test
  "Production-wiring acceptance tests for M-aif-a-matrix-faithfulness."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.report.war-machine :as wm]))

(deftest live-arena-declares-aif-observation-model
  (let [flags (wm/arena-mode-flags)]
    (is (= :aif (:likelihood-mode flags)))
    (is (= {:likelihood-mode :aif} (wm/arena-belief-update-opts)))
    (is (= (belief/model-manifest) (:belief-model-manifest flags)))
    (is (= #{:observation-model-hash :transition-model-hash
             :initial-prior-hash}
           (set (keys (:belief-model-manifest flags)))))))

(deftest live-arena-executes-the-aif-filter-path
  (let [prior {:spawned 0.05 :refined 0.10 :strengthened 0.10
               :addressed 0.10 :falsified 0.45 :foreclosed 0.10
               :reopened 0.10}
        event {:entity-id :m1 :type :strengthened :weight 1.0}
        actual (get (wm/apply-arena-belief-events {:m1 prior} [event]) :m1)
        expected (belief/update-entity-belief prior event
                                               {:likelihood-mode :aif})
        legacy (belief/update-entity-belief prior event
                                             {:likelihood-mode :legacy})]
    (testing "the named production seam reaches the exact AIF path"
      (is (= expected actual))
      (is (not= legacy actual)))
    (testing "the live off-diagonal contradiction is behaviorally active"
      (is (< (:falsified actual) (:falsified legacy))))))

(deftest policy-rollouts-share-the-live-aif-filter
  (let [prior {:spawned 0.05 :refined 0.10 :strengthened 0.10
               :addressed 0.10 :falsified 0.45 :foreclosed 0.10
               :reopened 0.10}
        state {:observation {} :belief {:m1 prior}}
        action {:type :fire-pattern :target :m1 :weight 1.0}
        opts (wm/arena-belief-update-opts)
        direct (fm/predict state action opts)
        scored (efe/compute-efe state action {:belief-update-opts opts})
        legacy (fm/predict state action {:likelihood-mode :legacy})]
    (testing "EFE passes the arena's declared belief model into its rollout"
      (is (= (:next-belief direct)
             (get-in scored [:prediction :next-belief])))
      (is (not= (:next-belief legacy) (:next-belief direct))))))
