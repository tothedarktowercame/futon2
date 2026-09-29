(ns futon2.aif.scan-bmr-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.scan-bmr :as scan-bmr]
            [futon2.aif.scan-learn :as scan-learn]))

(defn- close? [x y]
  (< (Math/abs (- (double x) (double y))) 1.0e-10))

(defn- state-with [prior key rows]
  (assoc-in prior [:concentrations key] rows))

(deftest hand-computed-two-status-binomial
  ;; Priors are Beta(1,1). Counts a=[3,1], b=[1,3].
  ;; full = 2*(ln B(4,2)-ln B(1,1)) = 2 ln(1/20)
  ;; tied has uniform total kappa=2 => Beta(1,1), totals [4,4],
  ;; tied = ln B(5,5)-ln B(1,1). Hand-set p_a=.75, p_b=.25:
  ;; hand = 6 ln(.75) + 2 ln(.25).
  (let [prior {:statuses [:a :b]
               :concentrations {:support {:a [1 1] :b [1 1]}}}
        learned (state-with prior :support {:a [4 2] :b [2 4]})
        matrix-var #'belief/channel-emission-matrix
        original @matrix-var]
    (with-redefs-fn {matrix-var (assoc original :support-coverage {:a 0.75 :b 0.25})}
      #(let [row (get-in (scan-bmr/score prior learned {:support 20})
                         [:channels :support])
             full (* 2.0 (Math/log (/ 1.0 20.0)))
             tied (- (bmr/log-multivariate-beta [5 5])
                     (bmr/log-multivariate-beta [1 1]))
             hand (+ (* 6.0 (Math/log 0.75)) (* 2.0 (Math/log 0.25)))]
         (is (close? full (get-in row [:learned :log-evidence])))
         (is (close? tied (get-in row [:tied :log-evidence])))
         (is (close? hand (get-in row [:hand-set :log-evidence])))
         (is (close? (- full tied) (get-in row [:tied :delta-f])))
         (is (close? (- full hand) (get-in row [:hand-set :delta-f])))))))

(deftest tied-and-learned-win-on-their-respective-data
  (let [prior {:statuses [:a :b]
               :concentrations {:support {:a [19/10 1/10] :b [1/10 19/10]}}}
        identical (state-with prior :support
                              {:a [(+ 5000 19/10) (+ 5000 1/10)]
                               :b [(+ 5000 1/10) (+ 5000 19/10)]})
        different (state-with prior :support
                              {:a [(+ 100 19/10) 1/10]
                               :b [1/10 (+ 100 19/10)]})
        tied-row (get-in (scan-bmr/score prior identical {:support 20})
                         [:channels :support])
        learned-row (get-in (scan-bmr/score prior different {:support 20})
                            [:channels :support])]
    (is (<= (get-in tied-row [:tied :delta-f]) -3))
    (is (close? (- (bmr/log-multivariate-beta [10001 10001])
                   (bmr/log-multivariate-beta [1 1]))
                (get-in tied-row [:tied :log-evidence])))
    (is (= :tied (:chosen-model tied-row)))
    (is (> (get-in learned-row [:tied :delta-f]) -3))
    (is (= :learned (:chosen-model learned-row)))))

(deftest double-one-with-fractional-failure-is-impossible
  (let [prior {:statuses [:strengthened]
               :concentrations {:support {:strengthened [19/10 1/10]}}}
        learned (state-with prior :support {:strengthened [29/10 3/5]})
        row (get-in (scan-bmr/score prior learned {:support 20})
                    [:channels :support :hand-set])]
    (is (= {:impossible-under-hand-set true} row))))

(deftest exposure-floor-boundary
  (let [prior {:statuses [:a :b]
               :concentrations {:support {:a [1 1] :b [1 1]}}}
        learned (state-with prior :support {:a [5001 5001] :b [5001 5001]})]
    (is (false? (get-in (scan-bmr/score prior learned {:support 19})
                        [:channels :support :eligible])))
    (is (true? (get-in (scan-bmr/score prior learned {:support 20})
                       [:channels :support :eligible])))))

(deftest channel-without-hand-set-row
  (let [prior (scan-learn/prior-state)
        row (get-in (scan-bmr/score prior prior {:annotation 20})
                    [:channels :annotation :hand-set])]
    (is (= {:status :no-hand-set-row} row))))

(deftest envelope-is-record-only
  (let [prior (scan-learn/prior-state)
        result (scan-bmr/score prior prior {})]
    (is (= {:schema :wm/scan-bmr-v1 :threshold -3.0
            :exposure-floor 20 :applied false}
           (select-keys result [:schema :threshold :exposure-floor :applied])))
    (is (= (set (keys (:concentrations prior)))
           (set (keys (:channels result)))))))
