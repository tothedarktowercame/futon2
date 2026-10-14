(ns futon2.aif.outer-learning-runtime-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-learning-runtime :as runtime]))

(deftest pinned-demonstration-run-populates-production-shadow
  (let [shadow (runtime/load-shadow)
        learned (:belief shadow)
        target "M-interim-director-proxy-metric-inventory"]
    (is (= :shadow (:mode shadow)))
    (is (= :produced (:status learned)))
    (is (= {:rows 1 :applied 1 :unresolved 0} (:counts learned)))
    (is (= 2 (get-in learned [:targets target :alpha
                              :grounded-collateral-progress])))
    (is (= 721840 (get-in learned [:targets target :resource-use
                                   :wall-clock-ms])))
    (is (= 4223565 (get-in learned [:targets target :resource-use :tokens])))
    (is (= :grounded-collateral-progress
           (get-in learned [:events 0 :observation :class])))
    (let [event (first (:events learned))]
      (is (= :applied (:update event)))
      (is (= {:selected 4 :positive 0 :no-want-effect 4}
             (select-keys (get-in event [:row-evidence :pattern-reinforcement])
                          [:selected :positive :no-want-effect]))))))
