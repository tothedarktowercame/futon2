(ns futon2.aif.machine-q-risk-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.epistemic-value :as eig]
            [futon2.aif.machine-q :as machine-q]
            [futon2.aif.machine-q-risk :as risk]))

(def support (mapv #(keyword (str "o" %)) (range 12)))
(def model {:id "m" :revision "r1"})
(defn row [a b]
  (into (array-map) (concat [[(support 0) a] [(support 1) b]]
                            (map #(vector % 0) (drop 2 support)))))
(defn q [id mass] {:policy/id id :model model :support support :mass mass
                   :authority :declared :pins {:snapshot "sha"}})
(defn c [mass] {:model model :support support :mass mass
                :provenance {:source "pin"}})
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))

(deftest exact-risk-and-ordering
  (let [fixed (c (row 1/4 3/4))
        ;; 1/2 ln((1/2)/(1/4)) + 1/2 ln((1/2)/(3/4)) = 1/2 ln(4/3).
        hand (:risk (risk/risk (q :balanced (row 1/2 1/2)) fixed))
        preferred (:risk (risk/risk (q :matches (row 1/4 3/4)) fixed))]
    (is (= (* 0.5 (Math/log (/ 4.0 3.0))) hand))
    (is (< preferred hand))
    (is (> (:risk (risk/risk (q :flipped (row 3/4 1/4)) fixed)) hand))))

(deftest typed-refusals
  (let [base-q (q :p (row 1/2 1/2)) base-c (c (row 1/2 1/2))]
    (is (= :support-mismatch (refusal #(risk/risk base-q (assoc base-c :support (vec (reverse support)))))))
    (is (= :model-revision-mismatch (refusal #(risk/risk base-q (assoc-in base-c [:model :revision] "r2")))))
    (is (= :missing-pins (refusal #(risk/risk (dissoc base-q :pins) base-c))))
    (is (= :missing-pins (refusal #(risk/risk base-q (dissoc base-c :provenance)))))
    (is (= :unnormalized-input (refusal #(risk/risk (assoc base-q :mass (row 1/2 1/4)) base-c))))
    (testing "the ruled seven-zero shape is never smoothed"
      (let [seven-zero (assoc (row 1/2 1/2) (support 0) 0 (support 1) 1)]
        (is (= :infinite-risk (refusal #(risk/risk base-q (c seven-zero)))))))))

(deftest float-carried-row
  ;; These are the exact binary64 inputs; BigDecimal admission sees sum 1.0.
  (let [mass (into (array-map) (map vector support (concat (repeat 10 0.1) [0.0 0.0])))
        result (risk/risk (q :float mass) (c mass))]
    (is (= :float-carried (get-in result [:admission :Q])))
    (is (zero? (:risk result)))))

(defn payload [support mass]
  {:schema :wm/predictive-outcome-row-v1
   :policy/id :inspect
   :policy {:id :inspect :pins {:policy "policy-sha"}}
   :model model
   :source {:reading :machine-q/F1 :pins {:model "model-sha"
                                          :source "source-sha"}}
   :outcome-domain {:id :evidence-v1 :support support}
   :mass mass
   :normalization {:residual 0.0}})

(deftest one-payload-serves-risk-and-eig-with-relabeling-falsifier
  (let [outcomes [:ordinary :no-result :failure :timeout :conflict :missing]
        declared-mass (zipmap outcomes [1/2 1/4 1/8 1/16 1/32 1/32])
        generator {:states [:evidence-state]
                   :outcomes outcomes
                   :transition {[:evidence-state :inspect]
                                {:evidence-state 1.0}}
                   :observation {:evidence-state declared-mass}}
        reading {:id :machine-q/F1
                 :plan {:inspect :inspect}
                 :belief-mass (fn [_ _] 1.0)}
        kernel (machine-q/predictive-outcome-kernel
                (machine-q/generative-model! generator)
                (machine-q/q-reading! reading generator)
                {})
        mass (get-in kernel [:rows :inspect])
        p (payload outcomes mass)
        admitted (risk/predictive-payload-row! p)
        preference {:model model :support outcomes :mass mass
                    :provenance {:source "preference-sha"}}
        prior {:a 1/2 :b 1/2}
        posteriors (zipmap outcomes (repeat prior))
        eig-model {:prior prior :predicted-observations admitted
                   :posteriors posteriors}
        permutation (zipmap outcomes (reverse outcomes))
        relabel (fn [row] (into {} (map (fn [[o probability]]
                                          [(permutation o) probability])) row))
        relabelled-support (mapv permutation outcomes)
        relabelled-mass (relabel mass)
        relabelled-payload (payload relabelled-support relabelled-mass)
        relabelled-preference {:model model :support relabelled-support
                               :mass relabelled-mass
                               :provenance {:source "preference-sha"}}]
    (is (= mass admitted))
    (is (zero? (:risk (risk/risk-payload p preference))))
    (is (zero? (eig/expected-information-gain eig-model)))
    (is (= (:risk (risk/risk-payload p preference))
           (:risk (risk/risk-payload relabelled-payload relabelled-preference))))
    (is (= (eig/expected-information-gain eig-model)
           (eig/expected-information-gain
            {:prior prior :predicted-observations
             (risk/predictive-payload-row! relabelled-payload)
             :posteriors (relabel posteriors)})))))

(deftest predictive-payload-refuses-domain-pin-and-receipt-mismatches
  (let [outcomes [:ordinary :missing]
        p (payload outcomes {:ordinary 3/4 :missing 1/4})]
    (is (= :missing-pins
           (refusal #(risk/predictive-payload-row!
                      (assoc-in p [:source :pins] {})))))
    (is (= :policy-payload-mismatch
           (refusal #(risk/predictive-payload-row!
                      (assoc-in p [:policy :pins] {})))))
    (is (= :outcome-domain-mismatch
           (refusal #(risk/predictive-payload-row!
                      (assoc-in p [:outcome-domain :support] [:ordinary])))))
    (is (= :normalization-receipt-mismatch
           (refusal #(risk/predictive-payload-row!
                      (assoc-in p [:normalization :residual] 0.1)))))))
