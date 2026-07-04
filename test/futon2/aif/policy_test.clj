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
;; effective-temperature — R14 γ modulation (τ_eff = τ_spread / γ)
;; ---------------------------------------------------------------------------

(deftest effective-temperature-reduces-at-unity-test
  (testing "γ = 1.0 → τ_eff == adaptive-temperature (reduction-safe)"
    (let [g [0.0 0.5 1.0]]
      (is (= (policy/adaptive-temperature g)
             (policy/effective-temperature g 1.0))))))

(deftest effective-temperature-monotone-in-gamma-test
  (testing "higher γ → lower effective temperature (sharper)"
    (let [g [0.0 1.0]
          lo (policy/effective-temperature g 0.5)
          mid (policy/effective-temperature g 1.0)
          hi (policy/effective-temperature g 2.0)]
      (is (> lo mid))
      (is (> mid hi))
      (is (< (Math/abs (- (/ mid 2.0) hi)) 1e-9) "τ_eff = τ_spread / γ exactly"))))

(deftest effective-temperature-floors-degenerate-gamma-test
  (testing "γ ≤ 0 is floored at tau-min so τ_eff never divides by ~0 or flips sign"
    (let [g [0.0 1.0]
          t (policy/effective-temperature g 0.0)]
      (is (pos? t))
      (is (< (Math/abs (- t (/ (policy/adaptive-temperature g) 0.01))) 1e-9)))))

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
;; select-action — R14 γ (policy-precision) modulation of selection
;; ---------------------------------------------------------------------------

(deftest select-action-default-gamma-is-reduction-safe-test
  (testing "omitting :policy-precision ⇒ τ and softmax identical to the spread path"
    (let [ranked-acts (ranked [[:address-sorry 0.0]
                               [:no-op 0.5]
                               [:fire-pattern 1.0]])
          out (policy/select-action ranked-acts)
          g-totals (mapv :G-total ranked-acts)]
      (is (= (policy/adaptive-temperature g-totals) (:tau out))
          "effective τ equals spread-only τ at the γ=1.0 default")
      (is (= 1.0 (:policy-precision out)))
      (is (= (policy/softmax-weights g-totals (policy/adaptive-temperature g-totals))
             (vals (:softmax-weights out)))))))

(deftest select-action-high-gamma-sharpens-test
  (testing "higher γ sharpens the softmax — more probability mass on the best action"
    (let [ranked-acts (ranked [[:address-sorry 0.0]
                               [:fire-pattern 1.0]])
          best-w (fn [g] (apply max (vals (:softmax-weights
                                           (policy/select-action
                                            ranked-acts {:policy-precision g})))))
          w-lo (best-w 0.5)
          w-mid (best-w 1.0)
          w-hi (best-w 2.0)]
      (is (> w-hi w-mid) "γ=2 commits harder than γ=1")
      (is (> w-mid w-lo) "γ=1 commits harder than γ=0.5 (more abstain-leaning)"))))

(deftest select-action-records-tau-spread-test
  (testing "the chosen branch records both effective τ and the pre-γ spread τ"
    (let [ranked-acts (ranked [[:address-sorry 0.0] [:fire-pattern 1.0]])
          out (policy/select-action ranked-acts {:policy-precision 2.0})]
      (is (= (policy/adaptive-temperature (mapv :G-total ranked-acts))
             (:tau-spread out)))
      (is (< (:tau out) (:tau-spread out)) "γ=2 lowered the effective τ")
      (is (= 2.0 (:policy-precision out))))))

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

;; ---------------------------------------------------------------------------
;; B-2d: τ-layer separation (M-aif-faithfulness §2.2) — dark build.
;; Default :spread must be byte-identical to the historical stacked form;
;; :gamma-only is the canonical γ-carries-sharpness form, dark until Joe flips.
;; ---------------------------------------------------------------------------

(deftest effective-temperature-default-mode-witness-test
  (testing "WITNESS: no :tau-mode == explicit :spread == historical formula, exactly"
    (doseq [g-totals [[] [0.5] [0.0 0.5 1.0] [0.10 0.11 0.12] [0.0 1.0]]
            gamma [0.0 0.5 1.0 1.7 25.0]]
      (let [historical (/ (policy/adaptive-temperature g-totals)
                          (max 0.01 (double gamma)))]
        (is (= historical (policy/effective-temperature g-totals gamma))
            "bare arity = historical")
        (is (= historical
               (policy/effective-temperature g-totals gamma {:tau-mode :spread}))
            "explicit :spread = historical")))))

(deftest select-action-default-mode-witness-test
  (testing "WITNESS: select-action decision map is identical with and without
            an explicit {:tau-mode :spread} — the dark flag changes nothing
            by default, at the decision level"
    (let [ranked (ranked-with-actions
                  [[{:type :address-sorry :target :sorry/x} 0.2]
                   [{:type :learn-action-class :target-class :a} 0.5]
                   [{:type :no-op} 0.9]])]
      (doseq [gamma [0.5 1.0 2.0]]
        (is (= (policy/select-action ranked {:policy-precision gamma})
               (policy/select-action ranked {:policy-precision gamma
                                             :temperature-opts {:tau-mode :spread}}))
            "byte-identical decision under default vs explicit :spread")))))

(deftest effective-temperature-gamma-only-test
  (testing ":gamma-only → τ_eff = 1/γ; the spread layer is OFF"
    (let [opts {:tau-mode :gamma-only}]
      (is (= 1.0 (policy/effective-temperature [0.0 1.0] 1.0 opts))
          "γ = 1 → τ_eff = 1 regardless of spread")
      (is (= (policy/effective-temperature [0.0 99.0] 2.0 opts)
             (policy/effective-temperature [0.10 0.11] 2.0 opts))
          "spread does not enter :gamma-only at all")
      (is (< (Math/abs (- 0.5 (policy/effective-temperature [0.0 1.0] 2.0 opts)))
             1e-12)
          "τ_eff = 1/γ exactly")
      (is (= (/ 1.0 0.01) (policy/effective-temperature [0.0 1.0] 0.0 opts))
          "degenerate γ still floored at tau-min"))))

(deftest effective-temperature-unknown-mode-throws-test
  (testing "an unknown :tau-mode is a loud config error, not a silent default"
    (is (thrown? clojure.lang.ExceptionInfo
                 (policy/effective-temperature [0.0 1.0] 1.0
                                               {:tau-mode :typo-mode})))))
