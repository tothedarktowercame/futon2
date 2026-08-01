(ns ants.aif.experiment-schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.aif.experiment :as experiment]
            [ants.aif.experiment-schema :as schema]))

(def registration
  (schema/read-registration schema/slice5-confirmation-registration))

(deftest confirmation-harness-matches-registration
  (is (= experiment/slice5-confirmation-harness
         (schema/validate-harness! registration
                                   experiment/slice5-confirmation-harness))))

(deftest rejects-unregistered-harness-arm
  (let [bad (update experiment/slice5-confirmation-harness :arms conj :secret-arm)]
    (try
      (schema/validate-harness! registration bad)
      (is false "unregistered arm must fail startup")
      (catch clojure.lang.ExceptionInfo e
        (is (= [:secret-arm] (:unregistered-arms (ex-data e))))))))

(deftest rejects-registered-arm-absent-from-harness
  (let [bad (update experiment/slice5-confirmation-harness :arms pop)]
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
        (assoc-in experiment/slice5-confirmation-harness
                  [:environment :ticks] 301)))))

(deftest rejects-seed-formula-drift-before-executor
  (testing "validation fails before the first run"
    (let [entered? (atom false)
          bad (assoc-in experiment/slice5-confirmation-harness
                        [:seeds :food-fn]
                        "202609111 + 100000*s + 2*i")]
      (is (thrown? clojure.lang.ExceptionInfo
                   (schema/validate-then-run!
                    schema/slice5-confirmation-registration bad
                    (fn [_] (reset! entered? true)))))
      (is (false? @entered?)))))
