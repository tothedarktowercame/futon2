(ns futon2.aif.policy-free-energy-measurement-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy-free-energy :as fpi]))

(def opts {:deterministic-tolerance 0.0 :absent-variance :floor :variance-floor 0.01})

(defn refusal [prediction observation]
  (try (fpi/f-pi-for-candidate prediction observation opts) nil
       (catch clojure.lang.ExceptionInfo e (:error (ex-data e)))))

(deftest typed-variance-branches
  (testing "negative variance refuses"
    (is (= :invalid-variance
           (refusal {:prediction-mean {:x 0.0} :prediction-variance {:x -1.0}}
                    {:x 0.0}))))
  (testing "a mismatching declared deterministic channel refuses"
    (is (= :deterministic-mismatch
           (refusal {:prediction-mean {:x 0.0} :prediction-variance {:x 0.0}}
                    {:x 1.0}))))
  (testing "a matching deterministic channel contributes zero"
    (is (zero? (fpi/f-pi-for-candidate
                {:prediction-mean {:x 1.0} :prediction-variance {:x 0.0}}
                {:x 1.0} opts))))
  (testing "an explicitly absent zero uses the production floor arm"
    (is (number? (fpi/f-pi-for-candidate
                  {:prediction-mean {:x 0.0} :prediction-variance {:x 0.0}
                   :variance-status {:x {:status :absent}}}
                  {:x 0.25} opts)))))
