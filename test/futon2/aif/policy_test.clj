(ns futon2.aif.policy-test
  "Tests for R6: action selection policy (softmax + abstain)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy :as policy]))

;; ---------------------------------------------------------------------------
;; adaptive-temperature
;; ---------------------------------------------------------------------------

(deftest adaptive-temperature-empty-test
  (testing "empty input returns tau-min"
    (is (= 0.01 (policy/adaptive-temperature [])))))

(deftest adaptive-temperature-floor-test
  (testing "identical inputs (zero spread) return tau-min"
    (is (= 0.01 (policy/adaptive-temperature [0.5 0.5 0.5])))))

(deftest adaptive-temperature-scales-with-spread-test
  (testing "higher spread → higher τ"
    (let [tight (policy/adaptive-temperature [0.10 0.11 0.12])
          wide (policy/adaptive-temperature [0.00 1.00])]
      (is (< tight wide))))
  (testing "τ = spread / k for non-degenerate input"
    (is (< (Math/abs (- 0.2 (policy/adaptive-temperature [0.0 1.0]))) 1e-9)
        "spread 1.0 / k=5.0 = 0.2"))
  (testing "k tunable via opts"
    (is (< (Math/abs (- 0.5 (policy/adaptive-temperature [0.0 1.0] {:k 2.0}))) 1e-9))))

;; ---------------------------------------------------------------------------
;; softmax-weights
;; ---------------------------------------------------------------------------

(deftest softmax-weights-sum-to-one-test
  (testing "softmax-weights sum to 1.0"
    (let [w (policy/softmax-weights [0.0 0.5 1.0] 0.2)]
      (is (< (Math/abs (- 1.0 (reduce + w))) 1e-9)))))

(deftest softmax-weights-monotone-test
  (testing "lower G → higher weight (under uniform τ)"
    (let [w (policy/softmax-weights [0.0 0.5 1.0] 0.2)]
      (is (apply > w)))))

(deftest softmax-weights-empty-test
  (testing "empty input returns nil"
    (is (nil? (policy/softmax-weights [] 0.1)))))

;; ---------------------------------------------------------------------------
;; select-action — chosen branch
;; ---------------------------------------------------------------------------

(defn- ranked
  "Helper: build a ranked list quickly."
  [pairs]
  (mapv (fn [[t g]] {:action {:type t} :G-total g}) pairs))

(deftest select-action-chooses-best-when-clearly-better-test
  (testing "best action wins by >= epsilon over :no-op → returned as chosen"
    (let [ranked-acts (ranked [[:address-sorry 0.1]
                               [:no-op 0.5]
                               [:fire-pattern 0.8]])
          out (policy/select-action ranked-acts)]
      (is (= :address-sorry (-> out :action :type)))
      (is (= 1 (:rank out)))
      (is (number? (:tau out)))
      (is (map? (:softmax-weights out))))))

(deftest select-action-softmax-weights-valid-test
  (testing "softmax-weights map sums to 1.0 across ranked candidates"
    (let [ranked-acts (ranked [[:address-sorry 0.1]
                               [:no-op 0.5]])
          out (policy/select-action ranked-acts)
          weights (vals (:softmax-weights out))]
      (is (< (Math/abs (- 1.0 (reduce + weights))) 1e-9)))))

;; ---------------------------------------------------------------------------
;; select-action — abstain branch
;; ---------------------------------------------------------------------------

(deftest select-action-empty-input-abstains-test
  (testing "empty ranked input → abstain with :no-candidates reason"
    (let [out (policy/select-action [])]
      (is (= :abstain (:action out)))
      (is (= :no-candidates (:reason out))))))

(deftest select-action-abstains-when-no-action-beats-no-op-test
  (testing "best action not meaningfully better than :no-op → abstain"
    (let [ranked-acts (ranked [[:no-op 0.5]
                               [:address-sorry 0.499]])  ; barely better
          out (policy/select-action ranked-acts)]
      (is (= :abstain (:action out)))
      (is (= :no-action-beats-no-op (:reason out))))))

(deftest select-action-abstain-no-op-only-test
  (testing ":no-op alone in the candidates → abstain (no improvement possible)"
    (let [out (policy/select-action (ranked [[:no-op 0.0]]))]
      (is (= :abstain (:action out)))
      (is (= :no-action-beats-no-op (:reason out))))))

(deftest select-action-abstain-gap-report-test
  (testing "abstain branch surfaces :learn-action-class actions as gap-report"
    (let [ranked-acts [{:action {:type :no-op} :G-total 0.5}
                      {:action {:type :learn-action-class :target-class :address-sorry
                                :intrinsic-value 0.1}
                       :G-total 0.501}  ; barely worse than no-op
                      {:action {:type :learn-action-class :target-class :open-mission
                                :intrinsic-value 0.1}
                       :G-total 0.502}]
          out (policy/select-action ranked-acts)]
      (is (= :abstain (:action out)))
      (is (= 2 (count (:gap-report out))))
      (is (= #{:address-sorry :open-mission}
             (set (map :target-class (:gap-report out))))))))

(deftest select-action-no-no-op-returns-best-test
  (testing "if :no-op isn't in candidates, best is always returned (no abstain)"
    (let [ranked-acts (ranked [[:address-sorry 0.1]
                               [:fire-pattern 0.2]])
          out (policy/select-action ranked-acts)]
      (is (= :address-sorry (-> out :action :type)))
      (is (not= :abstain (:action out))))))

(deftest select-action-epsilon-tunable-test
  (testing "raising abstain-epsilon causes more abstentions"
    (let [ranked-acts (ranked [[:address-sorry 0.3]
                               [:no-op 0.5]])]
      ;; Default epsilon 0.01: 0.5 - 0.3 = 0.2 >> 0.01 → choose address-sorry
      (is (not= :abstain (:action (policy/select-action ranked-acts))))
      ;; epsilon 0.5: 0.2 < 0.5 → abstain
      (is (= :abstain (:action (policy/select-action ranked-acts {:abstain-epsilon 0.5})))))))

;; ---------------------------------------------------------------------------
;; v0.13: default-mode-select — I6 compositional closure fallback
;; ---------------------------------------------------------------------------

(defn- ranked-with-actions [pairs]
  (->> pairs
       (mapv (fn [[a g]] {:action a :G-total g}))
       (sort-by :G-total)
       (map-indexed (fn [i e] (assoc e :rank (inc i))))
       vec))

(deftest default-mode-select-high-sorry-pressure-test
  (testing "high :sorry-count-norm → first :address-sorry candidate chosen"
    (let [state {:observation {:sorry-count-norm 0.7}}
          candidates (ranked-with-actions
                      [[{:type :no-op} 0.5]
                       [{:type :address-sorry :target :sorry/x} 0.6]
                       [{:type :address-sorry :target :sorry/y} 0.7]
                       [{:type :learn-action-class :target-class :open-mission
                         :intrinsic-value 0.1} 0.4]])
          out (policy/default-mode-select state candidates)]
      (is (= :address-sorry (-> out :action :type))
          "high sorry pressure → address-sorry first")
      (is (= :default-mode (:source out))))))

(deftest default-mode-select-no-sorry-pressure-prefers-learn-test
  (testing "low :sorry-count-norm → highest-intrinsic-value :learn-action-class chosen"
    (let [state {:observation {:sorry-count-norm 0.1}}
          candidates (ranked-with-actions
                      [[{:type :no-op} 0.5]
                       [{:type :learn-action-class :target-class :a :intrinsic-value 0.1} 0.6]
                       [{:type :learn-action-class :target-class :b :intrinsic-value 0.3} 0.4]])
          out (policy/default-mode-select state candidates)]
      (is (= :learn-action-class (-> out :action :type)))
      (is (= :b (-> out :action :target-class))
          "highest intrinsic-value chosen"))))

(deftest default-mode-select-no-op-only-test
  (testing "only :no-op available → returns :no-op (not abstain)"
    (let [state {:observation {}}
          candidates (ranked-with-actions [[{:type :no-op} 0.5]])
          out (policy/default-mode-select state candidates)]
      (is (= :no-op (-> out :action :type)))
      (is (= :default-mode (:source out))))))

(deftest default-mode-select-empty-test
  (testing "no candidates → abstain with :default-mode source"
    (let [out (policy/default-mode-select {:observation {}} [])]
      (is (= :abstain (:action out)))
      (is (= :default-mode (:source out))))))

(deftest default-mode-select-address-sorry-low-pressure-test
  (testing ":address-sorry chosen at low pressure when no :learn-action-class available"
    (let [state {:observation {:sorry-count-norm 0.05}}
          candidates (ranked-with-actions
                      [[{:type :no-op} 0.5]
                       [{:type :address-sorry :target :sorry/x} 0.4]])
          out (policy/default-mode-select state candidates)]
      (is (= :address-sorry (-> out :action :type))
          "even at low pressure, addressable work beats no-op"))))
