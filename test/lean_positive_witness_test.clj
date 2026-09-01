(ns lean-positive-witness-test
  (:require [checks.lean-positive-witness :as witness]
            [clojure.test :refer [deftest is testing]]))

(deftest positive-lean-source-must-be-substantive
  (testing "empty and comment-only files are vacuous"
    (is (false? (witness/source-valid? "")))
    (is (false? (witness/source-valid? "-- theorem fake : True := by trivial\n"))))
  (testing "declarations containing sorry are not witnesses"
    (is (false? (witness/source-valid? "theorem unfinished : True := by\n  sorry\n"))))
  (testing "a completed declaration is substantive"
    (is (true? (witness/source-valid? "theorem finished : True := by\n  trivial\n"))))
  (testing "a vocabulary constructor containing 'sorry' is not the sorry term"
    (is (true? (witness/source-valid?
                "def channels := [.sorryCountNorm]\n")))))
