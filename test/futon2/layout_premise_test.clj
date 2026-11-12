(ns futon2.layout-premise-test
  "T-ladder-06-layout-premise acceptance: genotype-step is not invariant
  on the 77|90|178 field under even8 and produces interface blends 95
  and 250, while propagator-only steps preserve the field exactly."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.layout-premise :as lp]
            [futon2.materials-catalogue :as materials]))

(deftest layout-materials-are-even8-fixed
  (is (every? (materials/fixed-set lp/even8-writing) lp/layout-materials)
      "77, 90 and 178 must all be fixed bytes of even8 for the premise to apply"))

(deftest propagator-only-steps-preserve-the-field-exactly
  (let [field (lp/layout-field 5)]
    (is (every? (fn [rule]
                  (= #{rule} (set (map #(lp/propagate-at rule lp/even8-writing %)
                                       (range lp/bit-count)))))
                lp/layout-materials)
        "each layout material is fixed under the invert write at EVERY source position")
    (dotimes [shift 8]
      (is (= field (lp/propagator-only-step field lp/even8-writing
                                             (fn [i] (mod (+ i shift) 8))))
          "propagator-only step preserves the field for every deterministic source schedule"))))

(deftest interface-blends-are-95-and-250
  (is (= {:left-interface-77-90 95 :right-interface-90-178 250}
         (lp/interface-blends))
      "the 77->90 interface blends to 95 and the 90->178 interface to 250")
  (is (not-any? (materials/fixed-set lp/even8-writing) [95 250])
      "the blend bytes are outside Fix(even8), so they are not preserved materials"))

(deftest genotype-step-is-not-invariant
  (let [report (lp/probe-report 5)]
    (is (false? (:genotype-step-invariant report))
        "the blend-then-write step changes the field")
    (is (= [95 250]
           (distinct (filter #{95 250} (lp/blended-field (lp/layout-field 5)))))
        "the blend stage of genotype-step produces exactly the interface blends 95 and 250 in the field")))

(deftest probe-report-is-the-ticket-evidence
  (let [r (lp/probe-report 5)]
    (is (:fixed-set-has-layout-materials r))
    (is (:interface-blends-outside-fix r))
    (is (:propagator-only-preserves-every-source r))
    (is (false? (:genotype-step-invariant r)))))
