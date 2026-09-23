(ns futon2.aif.held-out-split-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.held-out-split :as split]))

(def valid
  {:schema split/schema
   :disposition split/disposition
   :locator {:root "data/wm-full-loop-machinery-72/wm-contract-machinery-72-v1"
             :record "<attempt-id>/007-closed.edn"}
   :training-set ["attempt-001"]
   :held-out-set ["attempt-002" "attempt-003"]
   :outcome-classes [:result :no-result :failure :timeout]
   :window {:opens-after {:event :git-commit-containing-declaration}
            :minimum-observations 2}
   :calibration-authority :none})

(defn refusal [x]
  (try (split/validate x) nil
       (catch clojure.lang.ExceptionInfo e (:held-out-split/refusal (ex-data e)))))

(deftest valid-prospective-declaration
  (is (= valid (split/validate valid)))
  (let [declared (-> "wm/eig/held-out-split.edn" io/resource slurp edn/read-string)]
    (is (= declared (split/validate declared)))
    (is (false? (get-in declared [:claims :observations-collected?])))))

(deftest malformed-or-retrospective-declarations-refuse
  (testing "every exceptional outcome remains in the declared alphabet"
    (is (= :outcome-classes-incomplete
           (refusal (update valid :outcome-classes pop)))))
  (is (= :partition-overlap
         (refusal (assoc valid :training-set ["attempt-002"]))))
  (is (= :window-not-prospective
         (refusal (assoc-in valid [:window :opens-after] {:event :already-observed}))))
  (is (= :invalid-held-out-set
         (refusal (assoc valid :held-out-set ["attempt-002" "attempt-002"]))))
  (is (= :invalid-locator
         (refusal (assoc-in valid [:locator :record] ""))))
  (is (= :premature-calibration-authority
         (refusal (assoc valid :calibration-authority :passing)))))
