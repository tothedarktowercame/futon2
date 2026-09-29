(ns futon2.aif.scan-shadow-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.belief :as belief]
            [futon2.aif.scan-learn :as scan-learn]
            [futon2.aif.scan-shadow :as scan-shadow]))

(defn- close? [x y]
  (< (Math/abs (- (double x) (double y))) 1.0e-12))

(defn- log-sum-exp [xs]
  (let [m (apply max xs)]
    (+ m (Math/log (reduce + (map #(Math/exp (- % m)) xs))))))

(def worked-state
  {:schema :test :rho 0 :statuses [:a :b] :q {:a 1/2 :b 1/2}
   :concentrations {:support {:a [1 1] :b [2 1]}}
   :admitted-run-ids #{}})

(def worked-exposures {:support {:covered 1 :claims 1}})

(deftest status-log-likelihoods-use-the-step-predictive
  (let [{:keys [log-likelihoods used unused]}
        (scan-learn/status-log-likelihoods
          worked-state
          (assoc worked-exposures :attack {:status :absent :reason :no-claims})
          [:support :attack])]
    (is (close? (Math/log 0.5) (:a log-likelihoods)))
    (is (close? (Math/log (/ 2.0 3.0)) (:b log-likelihoods)))
    (is (= [:support] used))
    (is (= [:attack] unused)))
  (let [ll (:log-likelihoods
             (scan-learn/status-log-likelihoods worked-state worked-exposures [:support]))
        expected (log-sum-exp (for [s (:statuses worked-state)]
                                (+ (Math/log (double (get-in worked-state [:q s])))
                                   (get ll s))))
        receipt (:receipt (scan-learn/step
                            worked-state
                            {:run/id "same-formula" :scan-exposures worked-exposures}))]
    (is (close? expected (:log-evidence receipt)))))

(deftest channel-map-matches-the-belief-likelihood-block
  (is (= (set (vals scan-shadow/channel-map)) belief/channels-with-likelihood))
  (is (= #{:workstream-commits :loop-health :depositing-signal}
         (set (remove (set (keys scan-shadow/channel-map))
                      (keys (:concentrations (scan-learn/prior-state))))))))

(deftest adoption-separates-learned-tied-and-unchanged-rows
  (let [result
        (scan-shadow/adoption
          {:channels
           {:support {:eligible true :chosen-model :learned}
            :attack {:eligible true :chosen-model :tied}
            :active-repos {:eligible false :chosen-model :learned}
            :coupling {:eligible true :chosen-model :hand-set}
            :ticks {:eligible true :chosen-model :inconclusive}
            :workstream-commits {:eligible true :chosen-model :learned}}})]
    (is (= [:support :workstream-commits] (:adopted result)))
    (is (= [:attack] (:tied result)))
    (is (= #{:support-coverage :attack-coverage} (:exclude-channels result))))
  (is (= {:status :absent :reason :no-predecessor-bmr}
         (scan-shadow/adoption nil)))
  (is (= {:status :absent :reason :predecessor-bmr-refused}
         (scan-shadow/adoption {:status :refused :reason :bad}))))

(deftest shadow-row-is-the-hand-computed-bayes-update
  ;; (1/2*1/2, 1/2*2/3), normalised, is (3/7, 4/7).
  (let [row (scan-shadow/shadow-row
              {:mu-excl {:a 1/2 :b 1/2}
               :learner-state worked-state
               :exposures worked-exposures
               :adopted [:support]})]
    (is (close? (/ 3.0 7.0) (:a row)))
    (is (close? (/ 4.0 7.0) (:b row)))
    (is (close? 1.0 (reduce + (vals row))))))

(deftest empty-or-tied-only-adoption-has-no-effect
  (let [mu {:a 1/3 :b 2/3}]
    (is (identical? mu (scan-shadow/shadow-row
                         {:mu-excl mu :learner-state worked-state
                          :exposures worked-exposures :adopted []})))
    ;; A tied key can be present in exposures, but is deliberately absent from
    ;; the adopted vector supplied to the likelihood update.
    (is (= mu (scan-shadow/shadow-row
                {:mu-excl mu :learner-state worked-state
                 :exposures worked-exposures :adopted []})))))

(deftest zero-mass-status-stays-zero
  (let [row (scan-shadow/shadow-row
              {:mu-excl {:a 0.0 :b 1.0}
               :learner-state worked-state
               :exposures worked-exposures
               :adopted [:support]})]
    (is (= 0.0 (:a row)))
    (is (close? 1.0 (:b row)))))

(deftest mismatched-or-refused-rows-are-refused-not-imputed
  (let [args {:learner-state worked-state :exposures worked-exposures
              :adopted [:support]}]
    (is (= {:status :refused :reason :status-set-mismatch}
           (scan-shadow/shadow-row (assoc args :mu-excl {:a 1/2 :b 1/4 :c 1/4}))))
    (is (= {:status :refused :reason :status-set-mismatch}
           (scan-shadow/shadow-row (assoc args :mu-excl {:a 1.0}))))
    (is (= {:status :refused :reason :mu-excl-not-a-distribution}
           (scan-shadow/shadow-row
             (assoc args :mu-excl {:status :refused :reason :impossible}))))))
