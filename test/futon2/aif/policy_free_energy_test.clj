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
