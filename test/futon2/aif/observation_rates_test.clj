(ns futon2.aif.observation-rates-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-rates :as rates]
            [futon2.aif.cascade-model-manifest :as manifest]))

(def contract
  {:schema :wm/observation-contract-v1
   :classes [{:id :C5 :kind :checkable :token "named entry in the contract bundle"}
             {:id :J :kind :judgement :question "..."}
             {:id :U :kind :judgement :question "..."}]})

(def labels
  ;; class :J — 2 subjects, 4 labels. admitted-absent cells: 1 recorded
  ;; true (false positive) of 2; admitted-present cells: 1 recorded false
  ;; (false negative) of 2. Beta(1,1) posterior means: 2/4 and 2/4.
  [{:token-class :J :recorded true :admitted :absent}
   {:token-class :J :recorded false :admitted :absent}
   {:token-class :J :recorded false :admitted :present}
   {:token-class :J :recorded true :admitted :present}
   ;; class :K — admitted labels but zero errors: rates are posterior means,
   ;; not zero. 3 admitted-absent, 1 admitted-present, no errors.
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded true :admitted :present}
   ;; class :U — labels present but none admitted (e.g. all ambiguous):
   ;; unobserved, no rate at all.
   {:token-class :U :recorded true :admitted nil}])

(deftest exact-rationals-and-denominators
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1})]
    (is (= 1/2 (get-in r [:J :false-pos])))
    (is (= 1/2 (get-in r [:J :false-neg])))
    (is (= {:admitted-absent 2 :admitted-present 2} (get-in r [:J :denominators])))
    (is (= 2 (get-in r [:J :coverage])))           ; 4 labels / 2 subjects
    ;; zero errors ≠ zero rate: Beta(1,1) posterior means 1/5 and 1/3.
    (is (= 1/5 (get-in r [:K :false-pos])))
    (is (= 1/3 (get-in r [:K :false-neg])))
    (is (= {:admitted-absent 3 :admitted-present 1} (get-in r [:K :denominators])))))

(deftest unobserved-and-unknown-subjects
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1})]
    (is (= :unobserved (get-in r [:U :status])))
    (is (nil? (get-in r [:U :false-pos])))
    (is (nil? (get-in r [:U :false-neg]))))
  (is (= {:status :missing :kind :unknown-subject-count :class :J}
         (get (rates/rates-by-class labels {:K 8 :U 1}) :J))))

(deftest assembly-checkable-zero-and-unsupported-refusal
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})]
    (is (= {:false-neg 0 :false-pos 0 :basis :checkable} (:t1 m)))
    (is (= {:false-neg 1/2 :false-pos 1/2 :basis :estimated} (:t2 m)))
    ;; unobserved judgement class is a typed refusal, never a default
    (is (= {:status :missing :kind :unsupported-class :class :U :token :u}
           (rates/token-likelihood-rates r contract {:u :U})))
    ;; class absent from the contract is refused too
    (is (= {:status :missing :kind :unknown-class :class :Z :token :z}
           (rates/token-likelihood-rates r contract {:z :Z})))))

(deftest accepted-by-real-token-likelihood
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})
        dist (manifest/observation-distribution m #{:t2})]
    ;; column sums to exactly 1 (tokenLikelihood_colsum)
    (is (= 1 (reduce + (vals dist))))
    ;; checkable token keeps the identity factor in the kernel
    (is (= 1/4 (get dist #{:t2})))))                ; only t2 flips, prob 1/2 each way
