(ns futon2.aif.categorical-ambiguity-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.categorical-ambiguity :as categorical]))

(def states [:s0 :s1])
(def outcomes [:o0 :o1])
(def model {:id "isolated-test-model" :revision "r1"})

(defn q
  [mass]
  {:scope :isolated-test :model model :state-support states :mass mass
   :authority :isolated-test})

(defn a
  [row0 row1]
  {:scope :isolated-test :model model :state-support states
   :outcome-support outcomes :authority :observed-estimate
   :rows {:s0 {:support outcomes :mass row0}
          :s1 {:support outcomes :mass row1}}})

(def deterministic-0 {:o0 1 :o1 0})
(def deterministic-1 {:o0 0 :o1 1})
(def uniform {:o0 1/2 :o1 1/2})

(defn refusal
  [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e
         (get-in (ex-data e) [:refusal :kind]))))

(deftest closed-form-reference-values
  (testing "deterministic A rows have exactly zero ambiguity"
    (is (zero? (:ambiguity
                (categorical/ambiguity (q {:s0 1/3 :s1 2/3})
                                       (a deterministic-0 deterministic-1))))))
  (testing "one uniform row contributes its Q mass times ln 2"
    (let [actual (:ambiguity
                  (categorical/ambiguity (q {:s0 1/4 :s1 3/4})
                                         (a uniform deterministic-1)))]
      (is (= (* 0.25 (Math/log 2.0)) actual))))
  (testing "mixed Q and mixed A matches a separately written closed form"
    (let [mixed {:o0 1/4 :o1 3/4}
          actual (:ambiguity
                  (categorical/ambiguity (q {:s0 1/4 :s1 3/4})
                                         (a uniform mixed)))
          ;; 1/4 H[1/2,1/2] + 3/4 H[1/4,3/4]. This expression does
          ;; not call the production entropy implementation.
          expected (+ (* 0.25 (Math/log 2.0))
                      (* 0.75 (+ (- (* 0.25 (Math/log 0.25)))
                                 (- (* 0.75 (Math/log 0.75))))))]
      (is (= expected actual)))))

(deftest support-and-identity-refusals
  (let [base-q (q {:s0 1/2 :s1 1/2})
        base-a (a uniform uniform)]
    (is (= :support-mismatch
           (refusal #(categorical/ambiguity
                      (assoc base-q :state-support [:s1 :s0]) base-a))))
    (is (= :support-mismatch
           (refusal #(categorical/ambiguity
                      (assoc base-q :state-support [:s0 :foreign]
                             :mass {:s0 1/2 :foreign 1/2})
                      base-a))))
    (is (= :support-mismatch
           (refusal #(categorical/ambiguity
                      base-q (assoc-in base-a [:rows :s0 :support] [:o1 :o0])))))
    (is (= :model-revision-mismatch
           (refusal #(categorical/ambiguity
                      base-q (assoc-in base-a [:model :revision] "r2")))))))

(deftest authority-mass-and-row-refusals
  (let [base-q (q {:s0 1/2 :s1 1/2})
        base-a (a uniform uniform)]
    (is (= :declared-prior-refused
           (refusal #(categorical/ambiguity
                      base-q (assoc base-a :authority :declared-prior)))))
    (is (= :authority-unlicensed
           (refusal #(categorical/ambiguity
                      base-q (assoc base-a :authority :isolated-test)))))
    (is (= :mass-invalid
           (refusal #(categorical/ambiguity
                      (assoc base-q :mass {:s0 -1/2 :s1 3/2}) base-a))))
    (is (= :support-mismatch
           (refusal #(categorical/ambiguity
                      base-q (assoc-in base-a [:rows :s0 :mass]
                                       {:o0 1/2 :o1 1/2 :invented 0})))))
    (is (= :missing-rows
           (refusal #(categorical/ambiguity base-q (assoc base-a :rows {})))))
    (is (= :missing-field
           (refusal #(categorical/ambiguity base-q (dissoc base-a :rows)))))
    (is (= :support-mismatch
           (refusal #(categorical/ambiguity base-q
                                            (update base-a :rows dissoc :s1)))))))
