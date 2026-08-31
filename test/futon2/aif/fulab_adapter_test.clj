(ns futon2.aif.fulab-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.adapter :as adapter]
            [futon2.aif.adapters.fulab :as fulab]))

(deftest generic-outcome-size-surplus-preserves-temperature-behaviour
  (testing "three outcome words produce surplus two and the former tau"
    (let [result (adapter/update-beliefs
                  (fulab/new-adapter) {}
                  {:decision/id :d1 :outcome "one two three"})]
      (is (= 2.0 (get-in result [:aif :outcome-size-surplus])))
      (is (= (/ 1.0 3.0) (get-in result [:aif :tau-updated])))
      (is (not (contains? (:aif result) :prediction-error))))))

(deftest absent-surplus-retains-pending-zero-default
  (testing "the rename does not decide Fulab's pending absence policy"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d0 :candidates [:a]})]
      (is (= 1.0 (get-in result [:aif :tau]))))))

(deftest signed-value-is-refused-at-surplus-seam
  (testing "a canonical signed-error-shaped value cannot be silently clamped"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"outcome-size surplus must be nonnegative"
         (adapter/select-pattern
          (fulab/new-adapter) {}
          {:decision/id :d2
           :candidates [:a]
           :outcome-size-surplus -2.0})))))

(deftest canonical-prediction-error-key-is-refused
  (testing "the old ambiguous key cannot enter the Fulab temperature seam"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"does not consume canonical signed prediction error"
         (adapter/select-pattern
          (fulab/new-adapter) {}
          {:decision/id :d3
           :candidates [:a]
           :prediction-error -2.0})))))
