(ns futon2.aif.cascade-selection-test
  "Behavioural fixtures for futon2.aif.cascade-selection (WM-11, design P7).

   Each test replays the inputs of the named Lean fixture/theorem and asserts
   the values the theorem proves — not just constants — plus the refusal
   behaviour Lean states.

   Lean references (mathlib4, DarkTower/WarMachine):
   - PolicySelection.lean @ a434947c63: selectionWeight_top /
     selectionPosterior_eq_zero_of_risk_top (infinite G ⇒ probability 0),
     selectionPosterior_finite (normalise over finite candidates only),
     selectionPosterior_all_finite (all-finite case is the real
     precision-weighted posterior of PolicyPrecision).
   - PolicyPrecision.lean @ ba1b2c42dd: γ = 1/β, higher precision sharpens.
   - ActionMarginal.lean @ 6b55652425: IsBayesAction maximises the summed
     action marginal, not the per-policy argmax."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-selection :as cs]
            [futon2.aif.g-term-decomposition :as gtd]))

(def ^:private tol 1e-12)

(defn- refusal-kind [thunk]
  (try
    (thunk)
    (catch clojure.lang.ExceptionInfo e
      (get-in (ex-data e) [:refusal :kind]))))

(defn- fixture-two-finite
  "Two finite candidates, equal habit and F, G₁ = 1, G₂ = 2 (β supplied)."
  [beta]
  (cs/selection-posterior
   {:beta beta
    :candidates [{:id :c1 :habit 1.0 :f 0.0 :g 1.0}
                 {:id :c2 :habit 1.0 :f 0.0 :g 2.0}]}))

(deftest selection-posterior-lean-correspondence
  (testing "two equal-habit equal-F finite candidates: p2/p1 = exp((G1−G2)/β), sums to 1
            (selectionPosterior_all_finite ↔ PolicyPrecision.precisionWeightedPosterior,
            γ = 1/β)"
    (let [post (fixture-two-finite 1.0)]
      (is (< (abs (- (/ (get post :c2) (get post :c1))
                     (Math/exp -1.0)))
             tol))
      (is (< (abs (- (+ (get post :c1) (get post :c2)) 1.0)) tol)))
    (testing "higher precision sharpens (higherPrecisionSharpens): smaller β ⇒ smaller ratio"
      (let [r (fn [beta]
                (/ (get (fixture-two-finite beta) :c2)
                   (get (fixture-two-finite beta) :c1)))]
        (is (< (r 0.25) (r 1.0) (r 4.0)))
        (is (< (abs (- (r 0.25) (Math/exp -4.0))) tol))))))

(deftest selection-infinite-excluded
  (testing "a candidate with G = :infinite gets exactly 0.0 at every β, the finite
            candidates are renormalised over themselves only
            (selectionWeight_top / selectionPosterior_eq_zero_of_risk_top /
            selectionPosterior_finite)"
    (doseq [beta [0.25 1.0 4.0]]
      (let [post (cs/selection-posterior
                  {:beta beta
                   :candidates [{:id :c1 :habit 1.0 :f 0.0 :g 1.0}
                                {:id :c2 :habit 1.0 :f 0.0 :g 2.0}
                                {:id :bad :habit 1.0 :f 0.0 :g :infinite}]})]
        (is (= 0.0 (get post :bad)))
        (is (< (abs (- (+ (get post :c1) (get post :c2)) 1.0)) tol))
        ;; renormalised over the finite ones only: the c2/c1 ratio is unchanged
        ;; from the all-finite case at the same β
        (is (< (abs (- (/ (get post :c2) (get post :c1))
                       (Math/exp (/ (- 1.0 2.0) beta))))
               tol))))))

(deftest selection-refusals
  (testing "typed refusals the Lean also states"
    (is (= :no-admissible-candidate
           (refusal-kind #(cs/selection-posterior
                           {:beta 1.0
                            :candidates [{:id :c1 :habit 1.0 :f 0.0 :g :infinite}]}))))
    (is (= :no-admissible-candidate
           (refusal-kind #(cs/selection-posterior {:beta 1.0 :candidates []}))))
    (is (= :invalid-temperature
           (refusal-kind #(cs/selection-posterior
                           {:beta 0.0
                            :candidates [{:id :c1 :habit 1.0 :f 0.0 :g 1.0}]}))))
    (is (= :invalid-habit
           (refusal-kind #(cs/selection-posterior
                           {:beta 1.0
                            :candidates [{:id :c1 :habit 0.0 :f 0.0 :g 1.0}]}))))))

(deftest bayes-choice-aggregates
  (testing "posterior mass is summed per action; the audit example: masses in
            ratio 1:1:1:e, three policies → :a and one → :b, so :a wins
            because 3 > e (ActionMarginal.IsBayesAction); per-policy argmax
            would pick :b"
    (let [x (/ 1.0 (+ 3.0 Math/E))
          posterior {:p1 x :p2 x :p3 x :p4 (* Math/E x)}
          choice (cs/bayes-choice posterior
                                  {:p1 :a :p2 :a :p3 :a :p4 :b})]
      (is (= :a (:action choice)))
      (is (< (abs (- (:mass choice) (* 3.0 x))) tol))
      (is (= :action-name-ascending (:tie-break-rule choice)))))
  (testing "ties break by the declared stable order (ascending action name)"
    (let [choice (cs/bayes-choice {:p1 0.5 :p2 0.5} {:p1 :b :p2 :a})]
      (is (= :a (:action choice)))))
  (testing "an unmapped candidate refuses"
    (is (= :unmapped-candidate
           (refusal-kind #(cs/bayes-choice {:p1 1.0} {}))))))


(deftest selection-posterior-f-and-habit-enter-with-lean-signs
  (testing "selectionWeight = habit * exp(-gamma G - F): at equal G and habit,
            larger F lowers probability, p2/p1 = exp(F1 - F2); at equal G and F,
            probability is proportional to habit (review falsifier: +F sign)"
    (let [p (cs/selection-posterior {:beta 1 :candidates [{:id :p1 :habit 1 :f 0 :g 1}
                                                           {:id :p2 :habit 1 :f 1 :g 1}]})]
      (is (< (Math/abs (- (/ (:p2 p) (:p1 p)) (Math/exp -1))) tol))
      (is (< (:p2 p) (:p1 p))))
    (let [p (cs/selection-posterior {:beta 2 :candidates [{:id :p1 :habit 1 :f 0.5 :g 3}
                                                           {:id :p2 :habit 3 :f 0.5 :g 3}]})]
      (is (< (Math/abs (- (/ (:p2 p) (:p1 p)) 3.0)) tol)))))

;; ---------------------------------------------------------------------------
;; PROOF-2 packet 27 / F-ABS (2026-09-24): the receipt names the law that ran.
;; ---------------------------------------------------------------------------

(deftest law-receipt-all-absent-names-reduced-law
  (testing "FALSIFIER: every candidate :f nil :f-status :not-supplied. Pre-fix,
            the certificate presented σ(log E − F − γG) while σ(log E − γG) ran;
            a receipt claiming the full law on this field must be impossible.
            Catches: receipt hard-coded to the full law, or the all-absent case
            collapsed into it."
    (let [field [{:id :c1 :habit 1.0 :f nil :f-status :not-supplied :g 1.0}
                 {:id :c2 :habit 1.0 :f nil :f-status :not-supplied :g 2.0}]]
      (is (= {:law :sigma-log-E-minus-gamma-G
              :omitted-terms [:F]
              :reason :f-not-supplied
              :candidates-without-f [:c1 :c2]}
             (cs/law-receipt field)))
      (is (= {:status :absent :reason :not-supplied}
             (cs/f-consumed-record (first field)))))))

(deftest law-receipt-all-finite-names-full-law
  (testing "all-finite field → full-law receipt, :f-consumed equals the input F.
            Catches: the receipt flipping the full-law case to reduced, and
            :f-consumed recording the absent map over a supplied F."
    (let [field [{:id :c1 :habit 1.0 :f 0.5 :f-status :attached :g 1.0}
                 {:id :c2 :habit 1.0 :f 1.5 :f-status :attached :g 2.0}]]
      (is (= {:law :sigma-log-E-minus-F-minus-gamma-G}
             (cs/law-receipt field)))
      (is (= 0.5 (cs/f-consumed-record (first field))))
      (is (= 1.5 (cs/f-consumed-record (second field)))))))

(deftest law-receipt-mixed-field-is-its-own-case
  (testing "mixed field → mixed receipt with BOTH per-candidate lists, not
            collapsed to either pure law. Catches: a boolean receipt that
            picks one law for a per-candidate omission."
    (is (= {:law :sigma-log-E-minus-gamma-G-with-F-where-supplied
            :omitted-terms [:F]
            :reason :f-partially-supplied
            :candidates-with-f [:c2]
            :candidates-without-f [:c1]}
           (cs/law-receipt [{:id :c1 :habit 1.0 :f nil :f-status :not-supplied :g 1.0}
                            {:id :c2 :habit 1.0 :f 0.5 :f-status :attached :g 2.0}])))))

(deftest law-receipt-arithmetic-unchanged
  (testing "ARITHMETIC-UNCHANGED CONTROL: on the all-absent field the posterior
            and the Bayes action are BYTE-IDENTICAL to what the pre-change code
            produced. Pre-change the omitted term scored exactly like a supplied
            F = 0.0 (both contribute 0.0), so the absent-F field must equal the
            existing :f 0.0 fixture number for number. Catches: any diff that
            moved the law's arithmetic while adding the receipt."
    (let [absent-field [{:id :c1 :habit 1.0 :f nil :f-status :not-supplied :g 1.0}
                        {:id :c2 :habit 1.0 :f nil :f-status :not-supplied :g 2.0}]
          supplied-zero [{:id :c1 :habit 1.0 :f 0.0 :f-status :attached :g 1.0}
                         {:id :c2 :habit 1.0 :f 0.0 :f-status :attached :g 2.0}]
          post-absent (cs/selection-posterior {:beta 1.0 :candidates absent-field})
          post-zero (cs/selection-posterior {:beta 1.0 :candidates supplied-zero})]
      (is (= post-zero post-absent))
      (is (= (cs/bayes-choice post-zero {:c1 :a :c2 :b})
             (cs/bayes-choice post-absent {:c1 :a :c2 :b})))
      ;; and the numbers themselves are the pinned fixture values, not NaN
      (is (< (abs (- (/ (get post-absent :c2) (get post-absent :c1))
                     (Math/exp -1.0)))
             tol)))))

(deftest g-term-decomposition-f-absent-says-omitted-from-law
  (testing "the decomposition's F term says :status :absent :reason
            :omitted-from-law when F was not supplied — not
            :consumed-value-not-recorded — and only the F case changed.
            Catches: typed absence re-labelled as a lost value, and collateral
            re-labelling of the other terms' missing case."
    (let [ranked [{:certificate {:consumed-g {:A nil :C nil :D nil :Q nil}}}]
          candidates [{:id :c1 :habit 1.0 :f nil :f-status :not-supplied}]
          terms (get-in (gtd/census ranked candidates) [:policies 0 :terms])]
      (is (= {:status :absent :value nil :reason :omitted-from-law}
             (:F terms)))
      (is (= {:status :missing :value nil :reason :consumed-value-not-recorded}
             (:A terms)))
      ;; a supplied F keeps its verdict path (zero consumed F is degenerate)
      (is (= :present
             (:status (gtd/verdict :F 0.0)))))))
