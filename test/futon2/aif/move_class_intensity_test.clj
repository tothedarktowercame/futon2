(ns futon2.aif.move-class-intensity-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.move-class-intensity :as mci]
            [futon2.aif.trace :as trace]))

(def ^:private state
  {:observation {:mission-health 0.7
                 :sorry-count-norm 0.1
                 :active-repo-ratio 0.8}
   :belief (belief/initial-belief-state [:m])})

(deftest per-class-intensity-forms
  (testing "advance = kappa * W1 * (1 - resolvedness)"
    (let [i (mci/intensity {:type :advance-mission
                            :target "M-a"
                            :open-hole-count 3
                            :kappa 2.0
                            :W1 0.5})]
      (is (= :advance (:class i)))
      (is (< (Math/abs (- 0.75 (:value i))) 1e-9))
      (is (= 0.25 (get-in i [:components :resolvedness])))))
  (testing "close = kappa * W1 * resolvedness"
    (let [i (mci/intensity {:type :close-mission
                            :target "M-a"
                            :resolvedness 0.8
                            :kappa 2.0
                            :W1 0.5})]
      (is (= :close (:class i)))
      (is (< (Math/abs (- 0.8 (:value i))) 1e-9))))
  (testing "survey = kappa * W1 * staleness * (1 - resolvedness)"
    (let [i (mci/intensity {:type :survey-mission
                            :target "M-a"
                            :resolvedness 0.25
                            :staleness-days 7
                            :kappa 2.0
                            :W1 0.5})]
      (is (= :survey (:class i)))
      (is (< (Math/abs (- 0.375 (:value i))) 1e-9))
      (is (= 0.5 (get-in i [:components :staleness])))))
  (testing "apply-cascade reads escrowed coverage-dG as positive value"
    (let [i (mci/intensity {:type :apply-cascade
                            :target "M-a"
                            :fold-escrow {:delta-g -0.7}})]
      (is (= :apply-cascade (:class i)))
      (is (< (Math/abs (- 0.7 (:value i))) 1e-9)))))

(deftest bundle-score-values-portfolio-composition
  (let [same (mci/score-bundle [{:class :advance :value 1.0}
                                {:class :advance :value 1.0}])
        mixed (mci/score-bundle [{:class :advance :value 1.0}
                                 {:class :close :value 1.0}])
        costly (mci/score-bundle [{:class :advance :value 1.0 :cost 3.0}
                                  {:class :close :value 1.0 :cost 3.0}]
                                 {:budget 4.0})]
    (testing "same-class repeats get diminishing returns"
      (is (= 1.5 (:subtotal same))))
    (testing "mixed portfolio earns diversity bonus over equal raw values"
      (is (> (:score mixed) (:score same)))
      (is (= 1.1 (:diversity-multiplier mixed))))
    (testing "budget overrun is visible and penalized"
      (is (= 2.0 (:overrun costly)))
      (is (< (:score costly) (:score mixed))))))

(deftest efe-flag-off-is-identical
  (let [action {:type :advance-mission :target "M-a" :open-hole-count 3}
        default (efe/compute-efe state action)
        explicit-off (efe/compute-efe state action {:move-class-intensity-mode :off})]
    (is (= default explicit-off))
    (is (not (contains? default :move-class-intensity)))
    (is (not (contains? default :G-move-class-intensity)))))

(deftest efe-dark-mode-subtracts-intensity-from-g-total
  (let [action {:type :advance-mission :target "M-a" :open-hole-count 3}
        base (efe/compute-efe state action)
        dark (efe/compute-efe state action {:move-class-intensity-mode :v1})]
    (is (= :v1 (:move-class-intensity-mode dark)))
    (is (< (double (:G-total dark)) (double (:G-total base))))
    (is (< (Math/abs (- (double (:G-total dark))
                        (+ (double (:G-total base))
                           (double (:G-move-class-intensity dark)))))
           1e-9))
    (is (contains? (:augmentation-terms dark) :move-class-intensity))))

(deftest rank-actions-flag-off-order-is-identical
  (let [actions [{:type :advance-mission :target "M-a" :open-hole-count 1}
                 {:type :advance-mission :target "M-b" :open-hole-count 6}
                 {:type :no-op}]
        default (efe/rank-actions state actions)
        explicit-off (efe/rank-actions state actions {:move-class-intensity-mode :off})]
    (is (= default explicit-off))))

(deftest trace-persists-dark-contribution-at-birth
  (let [dark (efe/compute-efe state
                              {:type :apply-cascade
                               :target "M-a"
                               :fold-escrow {:delta-g -0.7}}
                              {:move-class-intensity-mode :v1})
        record (trace/trace-record {:observation (:observation state)
                                    :belief (:belief state)
                                    :ranked-actions [(assoc dark :rank 1)]
                                    :decision {:action (:action dark)}
                                    :mode :test})]
    (is (= :v1 (get-in record [:ranked-actions 0 :move-class-intensity-mode])))
    (is (= (:G-move-class-intensity dark)
           (get-in record [:ranked-actions 0 :G-move-class-intensity])))
    (is (= (:move-class-intensity dark)
           (get-in record [:ranked-actions 0 :move-class-intensity])))))
