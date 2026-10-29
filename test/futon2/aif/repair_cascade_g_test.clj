(ns futon2.aif.repair-cascade-g-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.finite-step-g :as finite]
            [futon2.aif.outer-score :as outer]
            [futon2.aif.repair-cascade-g :as repair]))

(def preference
  (zipmap repair/outcomes
          (map #(/ % 22) [4 4 2 4 2 1 1 1 1 1 1])))

(def provenance {:source :lean-example-preference})
(def base-counts (zipmap repair/pattern-ids (repeat [100 100])))

(defn score-at [injury cascade w counts]
  (repair/score {:injury-belief {injury 1}
                 :cascade cascade
                 :cost-model {:max-length 9 :w w}
                 :preference preference
                 :preference-provenance provenance
                 :pattern-counts counts}))

(deftest recorded-cascade-guard-check
  (is (= 5 (repair/admitted-count :never-defect repair/genuine-repair)))
  (is (= 2 (repair/admitted-count :never-defect repair/historical-misclassification)))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/genuine-repair
                                         (repair/initial-repair-state :never-defect)))))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/historical-misclassification
                                         (repair/initial-repair-state :never-defect)))))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/genuine-repair
                                         (repair/initial-repair-state :real-defect)))))
  (is (= :injury-persistence (repair/predicted-outcome
                              (repair/apply-cascade repair/historical-misclassification
                                                    (repair/initial-repair-state :real-defect))))))

(def fix-then-reproduce
  {:id :fix-then-reproduce
   :patterns [:record-the-finding-with-its-target :locate-the-producer-first
              :a-correct-refusal-is-not-the-defect :fix-the-red-before-the-repair
              :repair-the-producer-not-the-checker :reproduce-on-retained-inputs
              :witness-the-path-not-the-helper]})

(def reproduce-then-fix
  {:id :reproduce-then-fix
   :patterns [:record-the-finding-with-its-target :locate-the-producer-first
              :reproduce-on-retained-inputs :a-correct-refusal-is-not-the-defect
              :fix-the-red-before-the-repair :repair-the-producer-not-the-checker
              :witness-the-path-not-the-helper]})

(deftest order-changes-g
  (is (< (:g (score-at :real-defect reproduce-then-fix 0 base-counts))
         (:g (score-at :real-defect fix-then-reproduce 0 base-counts)))))

(deftest never-defect-cost-gap
  (let [gap (fn [w] (- (:g (score-at :never-defect repair/genuine-repair w base-counts))
                       (:g (score-at :never-defect repair/historical-misclassification
                                      w base-counts))))]
    (is (< (Math/abs (double (gap 0))) 1e-12))
    (is (pos? (gap 1/20)))
    (is (< (gap 1/20) (gap 1/10)))))

(deftest lean-example-flip
  (let [raw-gap (- (:g (score-at :never-defect repair/genuine-repair 1/10 base-counts))
                   (:g (score-at :never-defect repair/historical-misclassification
                                  1/10 base-counts)))
        counts (zipmap repair/pattern-ids
                       (repeat [(/ 100 raw-gap) (/ 100 raw-gap)]))
        result (repair/select {:injury-belief {:never-defect 1}
                               :cost-model {:max-length 9 :w 1/10}
                               :preference preference
                               :preference-provenance provenance
                               :pattern-counts counts}
                              [repair/genuine-repair repair/historical-misclassification])]
    (is (= :selected (:status result)))
    (is (= :historical-misclassification (:chosen-id result)))))

(deftest real-defect-prefers-genuine-at-zero-cost
  (let [result (repair/select {:injury-belief {:real-defect 1}
                               :cost-model {:max-length 9 :w 0}
                               :preference preference
                               :preference-provenance provenance
                               :pattern-counts base-counts}
                              [repair/genuine-repair repair/historical-misclassification])]
    (is (= :genuine-repair (:chosen-id result)))))

(deftest distinct-tie-is-reported
  (let [copy (assoc repair/genuine-repair :id :genuine-copy)
        result (repair/select {:injury-belief {:never-defect 1}
                               :cost-model {:max-length 9 :w 0}
                               :preference preference
                               :preference-provenance provenance
                               :pattern-counts base-counts}
                              [repair/genuine-repair copy])]
    (is (= :tie (:status result)))
    (is (nil? (:chosen-id result)))
    (is (= [:genuine-repair :genuine-copy] (:tied-ids result)))))

(deftest finite-step-g-matches-outer-private-sums
  (let [input {:focus :t1
               :relation (fn [focus target] (when (= focus target) :focus))
               :facts {:t1 {:status :ready :unblocks 1}}
               :field [{:target :t1 :reach 3/4}]
               :state-counts {:ready {:doable 3 :blocked 1}}
               :cascade []
               :ending-counts {:focused 4 :related 1 :unrelated 1
                               :stop-the-line 1 :not-yet-evaluated 1}
               :gain-counts {:none 1 :one 3 :several 1}}
        model (outer/outer-model input)
        c (outer/outer-preference (:ending-counts input) (:gain-counts input))
        direct (finite/step-g {:q (:q model) :kernel (:kernel model) :preference c})
        outer-risk ((deref #'futon2.aif.outer-score/risk) (outer/push model) c)
        outer-ambiguity ((deref #'futon2.aif.outer-score/ambiguity) model)]
    (testing "the shared finite sums equal outer-score's existing private sums"
      (is (< (Math/abs (- (:risk direct) outer-risk)) 1e-12))
      (is (< (Math/abs (- (:ambiguity direct) outer-ambiguity)) 1e-12))
      (is (< (Math/abs (- (:g direct) (+ outer-risk outer-ambiguity))) 1e-12)))))
