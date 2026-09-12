(ns futon2.aif.machine-policy-set-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-policy-set :as policy-set]))

(def sample
  [{:rank 1 :controller-score 0.5 :action {:type :advance-mission}}
   {:rank 2 :controller-score -0.0 :action {:type :no-op}}])

(deftest projection-is-explicit-and-lossless-test
  (is (= [{:id 1 :score (Long/valueOf (Double/doubleToRawLongBits 0.5)) :no-op false}
          {:id 2 :score (Long/valueOf (Double/doubleToRawLongBits -0.0)) :no-op true}]
         (policy-set/project-ranked-actions sample))))

(deftest exact-comparison-and-controls-test
  (let [expected (policy-set/project-ranked-actions sample)]
    (is (= :matched (:status (policy-set/compare-projections! expected expected))))
    (doseq [[label changed] [[:reordered (vec (reverse expected))]
                             [:dropped (pop expected)]]]
      (testing (name label)
        (try
          (policy-set/compare-projections! expected changed)
          (is false "mutation must refuse")
          (catch clojure.lang.ExceptionInfo e
            (is (= :policy-set-projection-mismatch (:refusal (ex-data e))))))))))

(deftest malformed-input-refuses-test
  (doseq [row [{:rank 0 :controller-score 1.0 :action {:type :no-op}}
               {:rank 1 :controller-score ##NaN :action {:type :no-op}}
               {:rank 1 :controller-score 1.0 :action nil}]]
    (is (thrown? clojure.lang.ExceptionInfo
                 (policy-set/project-candidate row)))))
