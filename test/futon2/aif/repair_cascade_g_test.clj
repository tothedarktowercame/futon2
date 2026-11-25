(ns futon2.aif.repair-cascade-g-test
  (:require [clojure.test :refer [deftest is]]
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
  (is (= 4 (repair/admitted-count :never-defect repair/historical-misclassification)))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/genuine-repair
                                         (repair/initial-repair-state :never-defect)))))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/historical-misclassification
                                         (repair/initial-repair-state :never-defect)))))
  (is (= :closure (repair/predicted-outcome
                   (repair/apply-cascade repair/genuine-repair
                                         (repair/initial-repair-state :real-defect)))))
  (is (= :useful-blocker-discovery
         (repair/predicted-outcome
          (repair/apply-cascade repair/historical-misclassification
                                (repair/initial-repair-state :real-defect)))))
  (is (= "3b7c08368ea4fea599b2159b916759aabea03daa"
         (:commit repair/model-provenance))))

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
