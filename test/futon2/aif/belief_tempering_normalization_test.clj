(ns futon2.aif.belief-tempering-normalization-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]))

(defn- approx= [a b]
  (< (Math/abs (- (double a) (double b))) 1.0e-12))

(defn- column-sum [A state]
  (reduce + (map #(double (get-in A [% state])) (keys A))))

(defn- old-row-only-update [A observed kappa prior]
  (let [raw (into {} (for [[s p] prior]
                       [s (* p (Math/pow (double (get-in A [observed s]))
                                        (double kappa)))]))
        z (reduce + (vals raw))]
    (update-vals raw #(/ % z))))

(deftest production-shaped-seven-by-seven-tempering-laws
  (let [A belief/observation-model-v1
        identity (belief/tempered-observation-model A 1.0)
        flat (belief/tempered-observation-model A 0.0)
        sharp (belief/tempered-observation-model A 2.0)]
    (is (= 7 (count A)))
    (is (every? #(= 7 (count %)) (vals A)))
    (is (= A identity) "kappa=1 must preserve the declared normalized model")
    (doseq [s belief/status-set]
      (is (approx= 1.0 (column-sum flat s)))
      (is (approx= 1.0 (column-sum sharp s)))
      (doseq [o belief/status-set]
        (is (approx= (/ 1.0 7.0) (get-in flat [o s])))))
    (let [posterior (belief/update-step A :strengthened 3.0
                                        (belief/uniform-prior))]
      (is (belief/valid-distribution? posterior)))))

(deftest asymmetric-column-denominators-materially-change-bayes
  (let [A belief/observation-model-v1
        prior (belief/uniform-prior)
        ;; w=3 gives kappa=2, deliberately away from the kappa=1 control.
        repaired (belief/update-step A :strengthened 3.0 prior)
        row-only (old-row-only-update A :strengthened 2.0 prior)]
    (is (some (fn [s] (not (approx= (get repaired s) (get row-only s))))
              belief/status-set)
        "the missing per-state denominators must be observable")
    (is (belief/valid-distribution? repaired))))

(deftest zero-nonfinite-impossible-and-invalid-weight-refusals
  (let [A belief/observation-model-v1
        zero-column (into {}
                          (for [[o row] A]
                            [o (assoc row :spawned 0.0)]))
        impossible-row
        (reduce (fn [m s]
                  (let [p (get-in m [:spawned s])]
                    (-> m
                        (assoc-in [:spawned s] 0.0)
                        (update-in [:refined s] + p))))
                A belief/status-set)]
    (is (= :invalid-observation-model
           (:reason (belief/tempered-observation-model zero-column 2.0))))
    (is (= :invalid-observation-model
           (:reason (belief/tempered-observation-model
                     (assoc-in A [:spawned :spawned] ##NaN) 2.0))))
    (is (= :invalid-likelihood-precision
           (:reason (belief/tempered-observation-model A ##Inf))))
    (is (belief/valid-observation-model? impossible-row))
    (is (= :impossible-observation
           (:reason (belief/update-step impossible-row :spawned 3.0
                                        (belief/uniform-prior)))))
    (doseq [w [-1.0 ##NaN ##Inf "bad"]]
      (let [result (belief/update-entity-belief
                    (belief/uniform-prior) {:type :strengthened :weight w}
                    {:likelihood-mode :aif})]
        (is (= :refused (:status result)) (str "weight " w))
        (is (= :invalid-observation-weight (:reason result)) (str "weight " w))))))

(deftest zero-weight-is-uninformative-and-compatibility-routes-are-unchanged
  (let [prior {:spawned 0.05 :refined 0.10 :strengthened 0.20
               :addressed 0.10 :falsified 0.25 :foreclosed 0.20 :reopened 0.10}
        event {:type :strengthened :weight 0.0}
        formal (belief/update-entity-belief prior event {:likelihood-mode :aif})]
    (is (= prior formal))
    (testing "legacy remains its historical diagonal multiplier"
      (let [legacy (belief/update-entity-belief prior event
                                                 {:likelihood-mode :legacy})]
        (is (every? #(approx= (get prior %) (get legacy %))
                    belief/status-set))))
    (testing "likelihood-ratio :a-matrix retains row-only exponent semantics"
      (let [event' (assoc event :weight 3.0)
            actual (belief/update-entity-belief
                    prior event' {:likelihood-mode :a-matrix
                                  :a-matrix belief/a-matrix-v0})
            L (belief/likelihood-vector belief/a-matrix-v0 :strengthened 3.0)
            raw (into {} (map (fn [[s p]] [s (* p (get L s))]) prior))
            z (reduce + (vals raw))]
        (is (= (update-vals raw #(/ % z)) actual))))))

(deftest provenance-distinguishes-the-repaired-law
  (let [manifest (belief/model-manifest)]
    (is (= :power-then-normalise-over-outcomes-per-state
           (get-in manifest [:likelihood-tempering-semantics :operator])))
    (is (= :log2-one-plus-weight
           (get-in manifest [:likelihood-tempering-semantics :kappa-law])))
    (is (re-matches #"0x[0-9a-f]{8}" (:likelihood-tempering-hash manifest)))))
