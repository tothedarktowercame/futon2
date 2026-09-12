(ns futon2.aif.machine-q-risk-test
  (:require [clojure.test :refer [deftest is testing]]
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
