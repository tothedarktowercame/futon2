(ns futon2.aif.operator-flip-test
  "Acceptance witnesses for Joe's 2026-07-13 Box 1 operator decisions."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.report.war-machine :as wm]))

(deftest production-mode-flags-record-both-flips
  (let [flags (wm/arena-mode-flags)]
    (testing "learned E(pi) replaces structural controller pressure"
      (is (= :habit-prior (:structural-pressure-mode flags)))
      (is (= :learned-frequency (:habit-prior-source flags))))
    (testing "selection gain alone carries R6 commitment sharpness"
      (is (= :selection-gain-only (:tau-mode flags))))
    (testing "scheduler habit is visible but cannot govern strategic selection"
      (is (= :controller-head (:strategic-selection-boundary flags)))
      (is (= :counterfactual-only (:scheduler-habit-authority flags)))
      (is (= :requires-reviewed-live-trace
             (:strategic-memory-authority flags))))))

(deftest configured-habit-prior-span-cap-is-provenance-stamped
  (with-redefs [wm/arena-habit-prior-span-ratio-cap (constantly 0.25)]
    (is (= 0.25 (:habit-prior-span-ratio-cap (wm/arena-mode-flags))))))
