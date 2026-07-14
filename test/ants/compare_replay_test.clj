(ns ants.compare-replay-test
  (:require [ants.compare-replay :as replay]
            [clojure.test :refer [deftest is testing]]))

(def fixture-config
  {:species :fixture
   :policy-priors {:forage 0.6 :return 0.2 :hold 0.1 :pheromone 0.1}
   :precision {:Pi-o 0.6 :tau 0.5}
   :pattern-sense {:trail-follow 0.7 :gradient-use 0.8}
   :adapt-config {:enabled? true
                  :threshold 0.5
                  :switch-to {:pattern-sense {:trail-follow 0.0
                                              :gradient-use 0.0
                                              :novelty-seek 0.8}}}
   :phenotype-coupling {:enabled? false :hunger-weight 0.1 :cargo-weight 0.2}})

(deftest random-wiring-preserves-terminal-shape-and-values
  (let [control (replay/random-wiring-control fixture-config 17)]
    (testing "base and creative sense ports keep keys and value multisets"
      (doseq [path [[:pattern-sense]
                    [:adapt-config :switch-to :pattern-sense]]]
        (is (= (set (keys (get-in fixture-config path)))
               (set (keys (get-in control path)))))
        (is (= (sort (vals (get-in fixture-config path)))
               (sort (vals (get-in control path)))))))
    (is (not= (:pattern-sense fixture-config) (:pattern-sense control)))
    (is (= :random-wiring (get-in control [:replay-control :kind])))))

(deftest shuffled-parameters-preserve-block-shape-and-multisets
  (let [control (replay/shuffled-parameter-control fixture-config 19)]
    (doseq [path replay/shuffled-parameter-paths]
      (let [before (get-in fixture-config path)
            after (get-in control path)]
        (is (= (set (keys before)) (set (keys after))))
        (is (= (sort (filter number? (vals before)))
               (sort (filter number? (vals after)))))))
    (is (not= (:precision fixture-config) (:precision control)))
    (is (= :shuffled-parameter (get-in control [:replay-control :kind])))))

(deftest arm-summary-exposes-zeros-and-confidence-interval
  (let [summary (replay/arm-summary [0.0 2.0 4.0])]
    (is (= 3 (:n summary)))
    (is (= 2.0 (:mean summary)))
    (is (= 1 (:starvation-count summary)))
    (is (= (/ 1.0 3.0) (:starvation-fraction summary)))
    (is (< (first (:ci95 summary)) 2.0 (second (:ci95 summary))))))

(deftest difference-summary-keeps-left-minus-right-sign
  (let [summary (replay/difference-summary [4.0 5.0 6.0] [1.0 2.0 3.0])]
    (is (= 3.0 (:delta summary)))
    (is (< (first (:ci95 summary)) (:delta summary) (second (:ci95 summary))))))
