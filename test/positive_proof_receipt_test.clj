(ns positive-proof-receipt-test
  (:require [checks.positive-proof-receipt :as receipt]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests]]))

(def baseline
  (edn/read-string
   (slurp "holes/labs/wm-contract/softmax-positive-receipt.edn")))

(defn rejected? [candidate]
  (not (:pass? (receipt/validate candidate))))

(deftest honest-positive-still-passes
  (is (:pass? (receipt/validate baseline))))

(deftest required-components-are-load-bearing
  (is (rejected? (assoc baseline :source-basis [])) "empty source basis")
  (is (rejected? (update baseline :source-basis pop)) "missing semantic dependency")
  (is (rejected? (assoc-in baseline [:adapter :mappings] [])) "empty adapter")
  (is (rejected? (dissoc baseline :adapter)) "missing adapter")
  (is (rejected? (assoc-in baseline [:adapter :mappings 0 :lean-field]
                           "unrelatedField")) "unbound Lean field")
  (is (rejected? (dissoc baseline :dependency-closure))
      "unrecorded dependency-closure boundary"))

(deftest reproducible-failure-is-not-verification
  (is (rejected? (assoc-in baseline [:result :exit] 1)))
  (is (rejected? (assoc-in baseline [:result :axioms] nil))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
