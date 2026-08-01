(ns ants.aif.experiment-schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.experiment :as experiment]
            [ants.aif.experiment-schema :as schema]))

(def registration
  (schema/read-registration schema/slice5-confirmation-registration))

(def harness (experiment/slice5-confirmation-harness))

(deftest confirmation-harness-matches-registration
  (is (= harness
         (schema/validate-harness! registration
                                   harness))))

(deftest rejects-unregistered-harness-arm
  (let [bad (update harness :arms conj :secret-arm)]
    (try
      (schema/validate-harness! registration bad)
      (is false "unregistered arm must fail startup")
      (catch clojure.lang.ExceptionInfo e
        (is (= [:secret-arm] (:unregistered-arms (ex-data e))))))))

(deftest rejects-registered-arm-absent-from-harness
  (let [bad (update harness :arms pop)]
    (try
      (schema/validate-harness! registration bad)
      (is false "unimplemented registered arm must fail startup")
      (catch clojure.lang.ExceptionInfo e
        (is (= [:classic] (:unimplemented-arms (ex-data e))))))))

(deftest rejects-environment-drift
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"does not match"
       (schema/validate-harness!
        registration
        (assoc-in harness
                  [:environment :ticks] 301)))))

(deftest rejects-seed-formula-drift-before-executor
  (testing "validation fails before the first run"
    (let [entered? (atom false)
          bad (assoc-in harness
                        [:seeds :food-fn]
                        "202609111 + 100000*s + 2*i")]
      (is (thrown? clojure.lang.ExceptionInfo
                   (schema/validate-then-run!
                    schema/slice5-confirmation-registration bad
                    (fn [_] (reset! entered? true)))))
      (is (false? @entered?)))))

(deftest confirmation-seeds-come-from-registered-holdout-formulas
  (let [patchy (experiment/confirmation-seeds harness :patchy)
        snowdrift (experiment/confirmation-seeds harness :snowdrift)]
    (is (= {:run 1 :food-seed 202609110 :move-seed 202609111
            :choice-seed 202659110}
           (first patchy)))
    (is (= {:run 30 :food-seed 202809168 :move-seed 202809169
            :choice-seed 202859139}
           (last snowdrift)))
    (is (= 30 (count patchy)))))
