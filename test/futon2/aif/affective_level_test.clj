(ns futon2.aif.affective-level-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.affective-level :as sut]))

(defn- close? [x y] (< (Math/abs (- (double x) (double y))) 1.0e-12))
(def half {:pos 0.5 :neg 0.5})

(deftest example-valence-moves
  (let [result (sut/step sut/hesp-level half 1.25 1.0)]
    (is (close? 7/11 (get-in result [:valence-after-update :pos])))
    (is (close? 4/11 (get-in result [:valence-after-update :neg])))
    (is (close? 23/22 (:beta-prior-next result)))
    (is (close? 0.25 (:charge result)))))

(deftest zero-charge-keeps-valence
  (let [q {:pos 0.3 :neg 0.7}
        updated (sut/valence-update sut/hesp-level q 1.25 1.25)]
    (is (= q updated))))

(deftest rising-precision-favours-positive
  (let [pos (sut/affective-evidence sut/hesp-level 1.25 1.0 :pos)
        neg (sut/affective-evidence sut/hesp-level 1.25 1.0 :neg)]
    (is (> pos neg))))

(deftest flat-level-keeps-beta-prior-over-five-steps
  ;; each step's prior is the previous step's :beta-prior-next, as in a run
  (loop [q {:pos 0.2 :neg 0.8}
         beta (sut/descend sut/flat-level {:pos 0.2 :neg 0.8})
         posterior [0.1 0.3 0.7 1.4 3.0]
         seen []]
    (if-let [beta-bar (first posterior)]
      (let [result (sut/step sut/flat-level q beta beta-bar)]
        (is (close? 1.0 (:beta-prior-next result)))
        (recur (:valence-next result) (:beta-prior-next result)
               (next posterior) (conj seen result)))
      (is (= 5 (count seen))))))

(deftest predict-uses-B-k-j-times-q-j
  ;; a non-identity B, so a transposed product would give 6.7/11, not 7.1/11
  (let [level (assoc sut/hesp-level :B {[:pos :pos] 0.9 [:neg :pos] 0.1
                                        [:pos :neg] 0.2 [:neg :neg] 0.8})
        result (sut/step level half 1.25 1.0)]
    (is (close? (/ 7.1 11) (get-in result [:valence-next :pos])))
    (is (close? (+ (* 0.5 (/ 7.1 11)) (* 2.0 (/ 3.9 11))) (:beta-prior-next result)))))

(deftest malformed-inputs-are-typed-absences
  (testing "an absent posterior is never zero charge"
    (is (= :beta-posterior-absent
           (:reason (sut/step sut/hesp-level half 1.25 {:status :absent})))))
  (testing "a missing posterior is never zero charge either"
    (is (= :beta-posterior-absent
           (:reason (sut/step sut/hesp-level half 1.25 nil)))))
  (testing "rate minus charge must stay positive"
    (is (= :affective-evidence-undefined
           (:reason (sut/step sut/hesp-level half 5.0 1.0)))))
  (testing "a valence belief is a probability vector"
    (is (= :valence-not-probability
           (:reason (sut/step sut/hesp-level {:pos 0.6 :neg 0.6} 1.25 1.0)))))
  (testing "each B column is stochastic"
    (is (= :transition-column-not-stochastic
           (:reason (sut/step (assoc-in sut/hesp-level [:B [:pos :pos]] 0.9)
                              half 1.25 1.0))))))
