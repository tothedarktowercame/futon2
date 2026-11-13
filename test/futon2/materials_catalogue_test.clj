(ns futon2.materials-catalogue-test
  "T-ladder-04-materials-catalogue acceptance: fixed-set sizes 4/16/8/2/4,
  union size 16, even8 exactly {77,90,101,114,141,154,165,178}, entropy
  rounded to three decimals 77=0.694, 78=141=0.406, 90=178=0, and 113 has
  no bi-infinite quiescent word."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.materials-catalogue :as mc]))

(deftest fixed-set-sizes-and-order
  (testing "catalogue fixed-set sizes are 4/16/8/2/4 in ticket order"
    (is (= [4 16 8 2 4]
           (map (comp count mc/catalogue) [:w4a :w16 :even8 :w2 :w4b])))))

(deftest union-is-the-sixteen-materials
  (testing "the union of all five fixed sets has size 16"
    (is (= 16 (count (mc/materials))))
    (is (= #{77 78 89 90 101 102 113 114 141 142 153 154 165 166 177 178}
           (mc/materials))))
  (testing "the fixed sets nest: every set is contained in the skeleton's 16"
    (doseq [[k s] mc/catalogue]
      (is (every? (:w16 mc/catalogue) s) {:set k :offenders (remove (:w16 mc/catalogue) s)}))))

(deftest even8-is-exactly-the-eight-named-materials
  (testing "Fix(:even8) = {77,90,101,114,141,154,165,178}"
    (is (= #{77 90 101 114 141 154 165 178} (:even8 mc/catalogue)))))

(deftest quiescent-entropy-at-three-decimals
  (testing "entropy rounded to three decimals"
    (is (= 0.694 (/ (Math/round (* 1000 (mc/quiescent-entropy 77))) 1000.0)))
    (is (= 0.406 (/ (Math/round (* 1000 (mc/quiescent-entropy 78))) 1000.0)))
    (is (= 0.406 (/ (Math/round (* 1000 (mc/quiescent-entropy 141))) 1000.0)))
    (is (= 0.000 (/ (Math/round (* 1000 (mc/quiescent-entropy 90))) 1000.0)))
    (is (= 0.000 (/ (Math/round (* 1000 (mc/quiescent-entropy 178))) 1000.0)))))

(deftest entropy-subshift-identities
  (testing "77's quiescent subshift has the golden ratio's growth rate (log2 phi = 0.6942)"
    (is (< (abs (- 0.6942 (mc/quiescent-entropy 77))) 0.0005)))
  (testing "90/178 quiescent subshifts are finite (zero entropy but nonempty)"
    (is (seq (mc/quiescent-edges 90)))
    (is (seq (mc/quiescent-edges 178)))))

(deftest state-113-has-no-bi-infinite-quiescent-word
  (testing "113's quiescent edge set is acyclic (two isolated edges, no cycle)"
    (is (= #{[[0 0] [0 1]] [[1 1] [1 0]]} (mc/quiescent-edges 113)))
    (is (not (mc/has-bi-infinite-quiescent-word? 113)))
    (is (zero? (mc/quiescent-entropy 113))))
  (testing "cycle detection is not vacuous: rules with periodic quiescent words do have one"
    (is (mc/has-bi-infinite-quiescent-word? 204))
    (is (mc/has-bi-infinite-quiescent-word? 90))))
