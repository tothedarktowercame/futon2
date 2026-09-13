(ns codex23-review-origin-induced-test
  (:require [clojure.test :refer [deftest is]]))

(deftest deliberate-failure
  (is (= :expected-failure :actual-value)))
