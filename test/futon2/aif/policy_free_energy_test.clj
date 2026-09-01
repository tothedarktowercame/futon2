(ns futon2.aif.policy-free-energy-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-free-energy :as policy-free-energy]))

(defn- prediction
  [mean variance]
  {:prediction-mean mean
   :prediction-variance variance})

(deftest candidate-discrimination-and-sign-convention-test
  (let [observation {:x 0.0}
        matching (prediction {:x 0.0} {:x 1.0})
        distant (prediction {:x 2.0} {:x 1.0})
        values (policy-free-energy/f-pi-vector [matching distant] observation)]
    (testing "different predicted means produce different aligned values"
      (is (= 2 (count values)))
      (is (not= (first values) (second values))))
    (testing "lower F_pi means better fit and is the term B.9 subtracts"
      (is (< (first values) (second values))))))

(deftest identical-predictions-are-degenerate-test
  (let [candidate (prediction {:x 0.25 :y -0.5} {:x 0.2 :y 0.4})
        values (policy-free-energy/f-pi-vector
                [candidate candidate] {:x 0.0 :y 0.0})]
    (is (= (first values) (second values)))))

(deftest missing-channel-is-rejected-test
  (testing "an observation-only channel is not silently skipped"
    (let [error (try
                  (policy-free-energy/f-pi-for-candidate
                   (prediction {:x 0.0} {:x 1.0})
                   {:x 0.0 :y 1.0})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= :channel-mismatch (:error (ex-data error))))
      (is (= [:y] (:observation-only (ex-data error))))))
  (testing "a prediction-only channel is not silently skipped"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"identical channels"
         (policy-free-energy/f-pi-for-candidate
          (prediction {:x 0.0 :y 1.0} {:x 1.0 :y 1.0})
          {:x 0.0}))))
  (testing "an absent variance is not defaulted"
    (let [error (try
                  (policy-free-energy/f-pi-for-candidate
                   (prediction {:x 0.0 :y 1.0} {:x 1.0})
                   {:x 0.0 :y 1.0})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= :channel-mismatch (:error (ex-data error))))
      (is (= [:y] (:mean-without-variance (ex-data error)))))))

(deftest zero-variance-is-an-explicit-deterministic-constraint-test
  (testing "an exactly satisfied deterministic channel has finite zero contribution"
    (is (= 0.0
           (policy-free-energy/f-pi-for-candidate
            (prediction {:x 2.0} {:x 0.0}) {:x 2.0}))))
  (testing "a violated deterministic channel is rejected, never Infinity or NaN"
    (let [error (try
                  (policy-free-energy/f-pi-for-candidate
                   (prediction {:x 2.0} {:x 0.0}) {:x 2.1})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= :deterministic-mismatch (:error (ex-data error)))))))

(deftest deterministic-tolerance-is-explicit-test
  (is (= 0.0
         (policy-free-energy/f-pi-for-candidate
          (prediction {:x 2.0} {:x 0.0})
          {:x 2.0001}
          {:deterministic-tolerance 0.001}))))

;; ---------------------------------------------------------------------------
;; The fixture below is WM-shaped rather than minimal, because the minimal
;; fixtures above all pass while the function cannot be called on WM data.
;; `forward-model/predict` gives every channel an :advance-mission does not
;; touch a variance of 0.0 with :variance-status {:status :absent}. Measured on
;; wm-trace-2026-07-04.edn: 14 channels, 12 of them absent, 7 of them moving
;; between consecutive ticks — so the tick-t prediction scored against the
;; tick-(t+1) observation rejects on :mathematics-pct with residual 0.001.

(def ^:private wm-shaped-prediction
  "Two channels the action model touches, three it declares absent."
  {:prediction-mean {:mission-health 0.54 :sorry-count-norm 0.31
                     :mathematics-pct 0.42 :loop-health 0.7 :q-sat 0.9}
   :prediction-variance {:mission-health 0.015 :sorry-count-norm 0.01
                         :mathematics-pct 0.0 :loop-health 0.0 :q-sat 0.0}
   :variance-status {:mission-health {:status :present :value 0.015}
                     :sorry-count-norm {:status :present :value 0.01}
                     :mathematics-pct {:status :absent
                                       :reason :deterministic-by-action-model}
                     :loop-health {:status :absent
                                   :reason :deterministic-by-action-model}
                     :q-sat {:status :absent
                             :reason :deterministic-by-action-model}}})

(def ^:private next-tick-observation
  "The absent channels have moved, as they do between real ticks."
  {:mission-health 0.55 :sorry-count-norm 0.30
   :mathematics-pct 0.419 :loop-health 0.71 :q-sat 0.9})

(deftest absent-variance-reject-is-unusable-on-wm-shaped-data-test
  (testing "the default rejects a moved channel the action model never predicted"
    (let [error (try (policy-free-energy/f-pi-for-candidate
                      wm-shaped-prediction next-tick-observation)
                     nil
                     (catch clojure.lang.ExceptionInfo e e))]
      (is (= :deterministic-mismatch (:error (ex-data error))))
      (is (= :absent (get-in (ex-data error) [:variance-status :status]))
          "the rejection names the absent status rather than hiding it"))))

(deftest absent-variance-floor-is-finite-and-discriminating-test
  (let [opts {:absent-variance :floor}
        other (assoc-in wm-shaped-prediction
                        [:prediction-mean :mission-health] 0.20)
        [a b] (policy-free-energy/f-pi-vector
               [wm-shaped-prediction other] next-tick-observation opts)]
    (testing "finite, never Infinity or NaN"
      (is (Double/isFinite a))
      (is (Double/isFinite b)))
    (testing "the closer predicted mean scores lower, as B.9's subtraction needs"
      (is (< a b)))
    (testing "identical candidates remain degenerate under the floor"
      (is (= a (first (policy-free-energy/f-pi-vector
                       [wm-shaped-prediction] next-tick-observation opts)))))))

(deftest a-declared-zero-still-rejects-under-floor-test
  (testing "a zero variance with no :absent status is a real deterministic claim"
    (let [strict (update wm-shaped-prediction :variance-status
                         dissoc :mathematics-pct)
          error (try (policy-free-energy/f-pi-for-candidate
                      strict next-tick-observation {:absent-variance :floor})
                     nil
                     (catch clojure.lang.ExceptionInfo e e))]
      (is (= :deterministic-mismatch (:error (ex-data error)))))))
