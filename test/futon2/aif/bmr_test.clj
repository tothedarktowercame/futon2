(ns futon2.aif.bmr-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.bmr :as bmr]))

(defn- near?
  ([expected actual] (near? expected actual 1e-12))
  ([expected actual tolerance]
   (<= (Math/abs (- (double expected) (double actual))) tolerance)))

(defn- near-vec?
  [expected actual tolerance]
  (and (= (count expected) (count actual))
       (every? true? (map #(near? %1 %2 tolerance) expected actual))))

(deftest redundant-parameter-prunes
  (testing "posterior mass only on retained states accepts the reduced model"
    (let [result (bmr/bayesian-model-reduction
                  [1.0 1.0 1.0 1.0]
                  [20.0 20.0 1.0 1.0]
                  [1.0 1.0 0.01 0.01])]
      (is (near-vec? [20.0 20.0 0.01 0.01]
                     (:reduced-posterior result)
                     1e-12))
      (is (<= (:delta-F result) -3.0))
      (is (:accept? result)))))

(deftest informative-parameter-rejects
  (testing "posterior mass on pruned states rejects the reduced model"
    (let [result (bmr/bmr
                  [1.0 1.0 1.0 1.0]
                  [20.0 20.0 20.0 20.0]
                  [1.0 1.0 0.01 0.01])]
      (is (> (:delta-F result) -3.0))
      (is (false? (:accept? result))))))

(deftest identity-no-op
  (testing "a' = a leaves the posterior unchanged and delta-F at zero"
    (let [posterior [2.0 5.0 11.0]
          result (bmr/bmr [1.0 1.0 1.0]
                          posterior
                          [1.0 1.0 1.0])]
      (is (= posterior (:reduced-posterior result)))
      (is (near? 0.0 (:delta-F result)))
      (is (false? (:accept? result))))))

(deftest dirichlet-moments-return-stddev
  (testing "means and stddevs match the closed form"
    (let [moments (bmr/dirichlet-moments [2.0 3.0 5.0])]
      (is (near-vec? [0.2 0.3 0.5]
                     (mapv :mean moments)
                     1e-12))
      (is (near-vec? [(Math/sqrt (/ 16.0 1100.0))
                      (Math/sqrt (/ 21.0 1100.0))
                      (Math/sqrt (/ 25.0 1100.0))]
                     (mapv :stddev moments)
                     1e-12))
      (is (every? #(contains? % :stddev) moments))
      (is (not-any? #(contains? % :variance) moments)))))

(deftest deterministic-and-ordered
  (testing "same ordered inputs produce identical outputs"
    (let [args [[1.0 1.0 1.0 1.0]
                [20.0 20.0 1.0 1.0]
                [1.0 1.0 0.01 0.01]]
          result-1 (apply bmr/bmr args)
          result-2 (apply bmr/bmr args)]
      (is (= result-1 result-2))))
  (testing "lists and vectors with the same order produce the same result"
    (is (= (bmr/bmr [1.0 1.0 1.0 1.0]
                    [20.0 20.0 1.0 1.0]
                    [1.0 1.0 0.01 0.01])
           (bmr/bmr '(1.0 1.0 1.0 1.0)
                    '(20.0 20.0 1.0 1.0)
                    '(1.0 1.0 0.01 0.01)))))
  (testing "unordered collections are rejected rather than silently reordered"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"ordered sequential"
                          (bmr/bmr #{1.0 2.0 3.0}
                                   [2.0 2.0 2.0]
                                   [1.0 1.0 1.0])))))
