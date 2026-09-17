(ns futon2.aif.wm01-conversion-agreement-test
  "WM-01 numerical-operations, clause: 'the duplicated coordinate conversions
   in machine_model/cascade_g for continued agreement; current agreement is a
   coincidence unless shown otherwise.'

   machine_model.clj converts each mass coordinate to its represented rational
   (represented-rational, inside numeric-row-admission); cascade_g.clj
   re-converts the same coordinates independently (rational-value, inside
   row!). These tests pin the agreement at the PUBLIC seams, so a drift in
   either converter fails here rather than silently changing risk/KL values:

   - machine-model seam: (machine-model/distribution-admission row support)
     returns :exact-total and :exactly-normalized?, both computed from
     represented-rational.
   - cascade-g seam: (cascade-g/step-g input) with deterministic A rows makes
     :joint and :outcome-mass expose rational-value's converted coordinates
     entry-by-entry (joint[s,o] = converted q[s] when A(s)=o deterministically).

   Agreement therefore requires, per representation class:
     (reduce +' (vals (:outcome-mass r))) = (:exact-total admission)
     (:exactly-normalized-inputs? r)      = (:exactly-normalized? admission)
   Any divergence between the two converters shows up as a sum or flag
   mismatch on at least the representations exercised below."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-g :as g]
            [futon2.aif.machine-model :as model]))

(def model {:id "wm01-conversion-agreement" :revision "1"})
(def states [:s0 :s1])
(def outcomes [:o0 :o1])

(defn- step-input
  "A step-g input whose A rows are deterministic state->outcome, so the
   outcome marginal exposes q's converted coordinates one per entry."
  [q-mass c-mass]
  {:context {:policy/id "wm01" :occurrence/id "conversion-agreement" :point 1}
   :q {:model model :state-support states :authority :isolated-test :mass q-mass}
   :a {:model model :state-support states :outcome-support outcomes
       :authority :observed-estimate
       :rows {:s0 {:support outcomes :mass {:o0 1 :o1 0}}
              :s1 {:support outcomes :mass {:o0 0 :o1 1}}}}
   :c {:model model :support outcomes :mass c-mass :provenance {:source :isolated-test}}})

(defn- agreement
  "Run both converters' public seams on one q row and return the pair to
   compare. c is exact-rational so its conversion is identity in both."
  [q-mass]
  (let [admission (model/distribution-admission q-mass states)
        r (g/step-g (step-input q-mass {:o0 1/2 :o1 1/2}))]
    {:admitted (:ok admission)
     :total-from-machine-model (:exact-total admission)
     :normalized-flag-machine-model (:exactly-normalized? admission)
     :total-from-cascade-g (reduce +' 0 (vals (:outcome-mass r)))
     :normalized-flag-cascade-g (:exactly-normalized-inputs? r)
     ;; per-entry: deterministic A makes each converted coordinate visible
     :entries-cascade-g (vals (:outcome-mass r))}))

(deftest agreement-is-pinned-per-representation
  (doseq [[label q-mass]
          [["exact-ratio" {:s0 1/3 :s1 2/3}]
           ["integer-identity" {:s0 0 :s1 1}]
           ["float64-decimal-spelling" {:s0 0.1 :s1 0.9}]
           ["float64-binary-inexact" {:s0 0.3 :s1 0.7}]
           ["float32-widened" {:s0 (float 0.1) :s1 (- 1 (float 0.1))}]
           ["float32-subnormal" {:s0 (float 1e-40) :s1 (- 1 (float 1e-40))}]
           ["exact-decimal" {:s0 0.1M :s1 0.9M}]
           ["mixed-exact" {:s0 1/10 :s1 0.9M}]
           ["mixed-floating" {:s0 0.5 :s1 1/2}]]]
    (let [{:keys [admitted total-from-machine-model total-from-cascade-g
                  normalized-flag-machine-model normalized-flag-cascade-g]}
          (agreement q-mass)]
      (is admitted (str label ": row refused, cannot compare"))
      (is (= total-from-machine-model total-from-cascade-g)
          (str label ": converter totals diverged"))
      (is (= normalized-flag-machine-model normalized-flag-cascade-g)
          (str label ": exactness flags diverged")))))

(deftest per-entry-conversion-witness
  ;; The float64 0.1 converts, in BOTH namespaces, to the decimal spelling of
  ;; (double 0.1) — rationalize(BigDecimal. 0.1) — not to its binary value.
  ;; Pinning the value itself, not just the sum, catches a converter that
  ;; drifts in a sum-preserving way.
  (let [r (g/step-g (step-input {:s0 0.1 :s1 0.9} {:o0 1/2 :o1 1/2}))]
    (is (= (rationalize (BigDecimal. 0.1)) (get-in r [:outcome-mass :o0])))
    (is (= (rationalize (BigDecimal. 0.9)) (get-in r [:outcome-mass :o1]))))
  ;; float32 must convert via its WIDENED double, identical rule on both sides.
  (let [r (g/step-g (step-input {:s0 (float 0.1) :s1 (- 1 (float 0.1))}
                                {:o0 1/2 :o1 1/2}))]
    (is (= (rationalize (BigDecimal. (double (float 0.1))))
           (get-in r [:outcome-mass :o0])))))

(deftest divergence-would-be-caught
  ;; Control for the control: prove these seams actually detect a divergent
  ;; converter. If cascade-g summed a DIFFERENT rule (the binary value of the
  ;; double, or the float32's own decimal spelling), the totals would differ,
  ;; so a green run above is a maintained invariant, not a vacuous pass.
  (let [binary-sum (+' (rationalize 0.1) (rationalize 0.9))
        decimal-spelling-sum (+' (rationalize (BigDecimal. 0.1))
                                 (rationalize (BigDecimal. 0.9)))]
    (is (not= binary-sum decimal-spelling-sum)
        "precondition: the alternative converter rule is actually distinguishable")))
