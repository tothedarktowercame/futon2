(ns futon2.aif.observation-rates-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-rates :as rates]
            [futon2.aif.cascade-model-manifest :as manifest]))

(def contract
  {:schema :wm/observation-contract-v1
   :classes [{:id :C5 :kind :checkable :token "named entry in the contract bundle"}
             {:id :J :kind :judgement :question "..."}
             {:id :U :kind :judgement :question "..."}
             {:id :V :kind :judgement :question "..."}]})

(def labels
  ;; class :J — 2 subjects, 5 labels. One label has an admitted reference
  ;; but NO recorded verdict: it counts towards coverage only, and towards
  ;; no denominator. Compared admitted-absent cells: 2, of which 1 recorded
  ;; true (raw false-pos 1/2). Compared admitted-present cells: 2, of which
  ;; 1 recorded false (raw false-neg 1/2).
  [{:token-class :J :recorded true :admitted :absent}
   {:token-class :J :recorded false :admitted :absent}
   {:token-class :J :recorded false :admitted :present}
   {:token-class :J :recorded true :admitted :present}
   {:token-class :J :recorded nil :admitted :absent}
   ;; class :K — observed but zero errors: raw rates 0/3 and 0/1, not smoothed.
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded true :admitted :present}
   ;; class :U — labels but none admitted: wholly unobserved.
   {:token-class :U :recorded true :admitted nil}
   ;; class :V — admitted on one direction only: false-neg cell observed
   ;; (0/1), false-pos cell :unobserved (zero admitted-absent comparisons).
   {:token-class :V :recorded true :admitted :present}])

(deftest raw-counts-and-coverage
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})]
    (is (= {:numerator 1 :denominator 2 :rate 1/2} (get-in r [:J :false-pos])))
    (is (= {:numerator 1 :denominator 2 :rate 1/2} (get-in r [:J :false-neg])))
    (is (= 5/2 (get-in r [:J :coverage])))          ; 5 labels / 2 subjects
    ;; zero errors is raw zero, not a smoothed posterior
    (is (= {:numerator 0 :denominator 3 :rate 0} (get-in r [:K :false-pos])))
    (is (= {:numerator 0 :denominator 1 :rate 0} (get-in r [:K :false-neg])))
    ;; per-cell unobserved: V has no admitted-absent comparison at all
    (is (= :unobserved (get-in r [:V :false-pos :status])))
    (is (= {:numerator 0 :denominator 1 :rate 0} (get-in r [:V :false-neg])))))

(deftest unobserved-class-and-unknown-subjects
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})]
    (is (= :unobserved (get-in r [:U :status])))
    (is (nil? (get-in r [:U :false-pos]))))
  (is (= {:status :missing :kind :unknown-subject-count :class :J}
         (get (rates/rates-by-class labels {:K 8 :U 1 :V 4}) :J))))

(deftest priors-explicit-only
  ;; an unauthorised prior is refused
  (is (= :invalid-prior (:kind (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                                      {:alpha 1 :beta 1}))))
  (is (= :invalid-prior (:kind (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                                      {:alpha 1 :beta 1 :authority "  "}))))
  ;; an authorised prior is recorded next to the raw counts, adds posterior
  ;; means, and changes no raw count
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                {:alpha 1 :beta 1
                                 :authority "pending: Beta(1,1) not approved"})]
    (is (= {:numerator 1 :denominator 2 :rate 1/2 :posterior-mean 2/4}
           (get-in r [:J :false-pos])))
    (is (= {:prior {:alpha 1 :beta 1 :authority "pending: Beta(1,1) not approved"}}
           (select-keys (get r :J) [:prior])))
    (is (= {:numerator 0 :denominator 3 :rate 0 :posterior-mean 1/5}
           (get-in r [:K :false-pos])))
    ;; a prior never turns an unobserved cell into a rate by itself
    (is (= :unobserved (get-in r [:V :false-pos :status])))))

(deftest assembly-checkable-zero-and-refusals
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})]
    (is (= {:false-neg 0 :false-pos 0 :basis :checkable} (:t1 m)))
    (is (= {:false-neg 1/2 :false-pos 1/2 :basis :estimated} (:t2 m)))
    ;; unobserved judgement class: typed refusal, never a default
    (is (= {:status :missing :kind :unsupported-class :class :U :token :u}
           (rates/token-likelihood-rates r contract {:u :U})))
    ;; partially unobserved class (V) is refused too
    (is (= {:status :missing :kind :unsupported-class :class :V :token :v}
           (rates/token-likelihood-rates r contract {:v :V})))
    ;; class absent from the contract
    (is (= {:status :missing :kind :unknown-class :class :Z :token :z}
           (rates/token-likelihood-rates r contract {:z :Z})))))

(deftest accepted-by-real-token-likelihood
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})
        dist (manifest/observation-distribution m #{:t2})]
    ;; column sums to exactly 1 (tokenLikelihood_colsum)
    (is (= 1 (reduce + (vals dist))))
    ;; checkable token keeps the identity factor in the kernel
    (is (= 1/4 (get dist #{:t2})))))                ; only t2 flips, prob 1/2 each way
