(ns futon2.aif.invariant-model-test
  "M-evaluate-policies VERIFY — the I1–I5 logic model (§8.11), per
   mission-coherence/logic-model-before-code (PSR-3).

   Checks the DESIGN, not the implementation: each invariant is a checkable
   predicate over an ABSTRACT trace (plain data, no I/O, no live state). The
   discipline: one CONFORMING WITNESS trace yields zero violations across all
   five invariants, and one ADVERSARIAL trace per invariant is caught by
   exactly the intended check. (futon2 has no core.logic dep; plain predicates
   carry the pattern's essence — checkable propositions + adversarial cases.)

   The recompute formula mirrors compute-efe's controller-score assembly
   (efe.clj:449-457) at the default weights — the same formula the corpus
   census (scripts/wm_trace_census.bb) uses."
  (:require [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; The abstract model

(def weights {:info 0.4 :survival 1.2 :structural-pressure 0.35})

(defn recompute-total
  "controller-score from persisted terms at default weights (post-D1a: gap and
   graph-pragmatic are persisted, so no hidden residual is tolerated)."
  [e]
  (if (map? (:augmentation-terms e))
    (+ (double (or (:G-core e) 0.0))
       (reduce + 0.0 (vals (:augmentation-terms e))))
    (+ (double (or (:G-risk e) 0.0))
       (double (or (:G-ambiguity e) 0.0))
       (- (* (:info weights) (double (or (:predictability-bonus e) 0.0))))
       (* (:survival weights) (double (or (:homeostatic-pressure e) 0.0)))
       (- (* (:structural-pressure weights)
             (double (or (:structural-pressure e) 0.0))))
       (double (or (:graph-control-score e) 0.0))
       (- (double (or (:gap-exploration-bonus e) 0.0)))
       (double (or (:G-goal-outcome e) 0.0)))))

(def decomposition-keys
  [:G-risk :G-ambiguity :predictability-bonus :homeostatic-pressure :structural-pressure])

(defn placeholder? [e] (= :placeholder (:score-provenance e)))
(defn decomposed?  [e] (every? #(number? (get e %)) decomposition-keys))

;; ---------------------------------------------------------------------------
;; The invariants as violation-collectors (empty seq = holds)

(defn i1-replay-equality
  "Recomputed total = persisted :controller-score for every decomposed entry (tol 1e-9)."
  [tick]
  (for [e (:ranked-actions tick)
        :when (decomposed? e)
        :let [d (Math/abs (- (recompute-total e) (double (:controller-score e))))]
        :when (> d 1e-9)]
    {:invariant :i1 :entry (:id e) :residual d}))

(defn i2-no-bare-constants
  "Every entry is fully decomposed OR carries an explicit placeholder marker."
  [tick]
  (for [e (:ranked-actions tick)
        :when (not (or (decomposed? e) (placeholder? e)))]
    {:invariant :i2 :entry (:id e)}))

(defn i3-core-additivity
  ":G-core = :G-risk + :G-ambiguity exactly, wherever :G-core is present."
  [tick]
  (for [e (:ranked-actions tick)
        :when (contains? e :G-core)
        :when (not= (double (:G-core e))
                    (+ (double (:G-risk e)) (double (:G-ambiguity e))))]
    {:invariant :i3 :entry (:id e)}))

(defn i4-no-silent-steering
  "The persisted keys of each decomposed entry cover every term its formula
   consumed (:formula-terms — the abstract stand-in for compute-efe's actual
   summand set)."
  [tick]
  (for [e (:ranked-actions tick)
        :when (decomposed? e)
        :let [missing (remove #(contains? e %) (:formula-terms e))]
        :when (seq missing)]
    {:invariant :i4 :entry (:id e) :missing (vec missing)}))

(defn i5-label-follows-code
  "No surface claims canonical sense (i) without having earned it."
  [surfaces]
  (for [s surfaces
        :when (and (= :sense-i (:declared-sense s)) (not (:earned? s)))]
    {:invariant :i5 :surface (:id s)}))

(defn all-violations [tick surfaces]
  (concat (i1-replay-equality tick) (i2-no-bare-constants tick)
          (i3-core-additivity tick) (i4-no-silent-steering tick)
          (i5-label-follows-code surfaces)))

;; ---------------------------------------------------------------------------
;; Conforming witness

(def full-terms
  [:G-risk :G-ambiguity :predictability-bonus :homeostatic-pressure :structural-pressure
   :gap-exploration-bonus :graph-control-score :G-goal-outcome])

(defn entry
  "A decomposed entry whose :controller-score is derived (so I1 holds by construction)
   and whose :G-core is exact (so I3 holds by construction)."
  [id terms]
  (let [e (merge {:id id :formula-terms full-terms} terms)]
    (assoc e
           :controller-score (recompute-total e)
           :G-core (+ (double (:G-risk e)) (double (:G-ambiguity e))))))

(def witness-tick
  {:ranked-actions
   [(entry :a1 {:G-risk 0.156 :G-ambiguity 0.015 :predictability-bonus 13.985
                :homeostatic-pressure 0.758 :structural-pressure 1.0
                :gap-exploration-bonus 0.3 :graph-control-score 0.05 :G-goal-outcome 0.0})
    (entry :a2 {:G-risk 0.31 :G-ambiguity 0.03 :predictability-bonus 13.97
                :homeostatic-pressure 0.61 :structural-pressure 0.0
                :gap-exploration-bonus 0.0 :graph-control-score 0.0 :G-goal-outcome 0.12})
    ;; the cascade row: unscored but SELF-DESCRIBING (D1b)
    {:id :casc :controller-score 0.0 :score-provenance :placeholder}]})

(def witness-surfaces
  [{:id :wm-blend          :declared-sense :sense-iii :earned? true}
   {:id :rollout           :declared-sense :sense-i   :earned? true}
   {:id :portfolio-surface :declared-sense :sense-i   :earned? true}
   {:id :field-diagrams    :declared-sense :sense-iv  :earned? true}
   {:id :paper-sentence    :declared-sense :sense-iii :earned? true}])

;; ---------------------------------------------------------------------------
;; Tests: witness clean; each adversarial caught by exactly its invariant

(deftest conforming-witness-yields-zero-violations
  (is (empty? (all-violations witness-tick witness-surfaces))))

(deftest telemetry-does-not-silently-reenter-the-score
  (let [base (entry :retired
                    {:G-risk 0.2 :G-ambiguity 0.1 :predictability-bonus 8.0
                     :homeostatic-pressure 0.7 :structural-pressure 0.0
                     :gap-exploration-bonus 0.0 :graph-control-score 0.0
                     :G-goal-outcome 0.0})
        retired (assoc base
                       :augmentation-terms {:risk-control 0.0 :info 0.0
                                            :survival 0.0 :structural-pressure 0.0
                                            :graph-control 0.0
                                            :model-uncertainty-bonus 0.0
                                            :gap 0.0 :goal-outcome 0.0}
                       :controller-score 0.3)]
    (is (< (Math/abs (- 0.3 (recompute-total retired))) 1.0e-12))
    (is (< (Math/abs
            (- 0.3 (recompute-total (assoc retired
                                           :predictability-bonus 8000.0
                                           :homeostatic-pressure 7000.0))))
           1.0e-12))))

(defn- only-caught-by [tick surfaces expected]
  (let [vs (all-violations tick surfaces)]
    (and (seq vs) (= #{expected} (set (map :invariant vs))))))

(deftest i1-catches-tampered-total
  (let [tick (update-in witness-tick [:ranked-actions 0 :controller-score] + 0.296)]
    (testing "the pre-D1a hidden-residual failure mode, replayed abstractly"
      (is (only-caught-by tick witness-surfaces :i1)))))

(deftest i2-catches-bare-constant
  (let [tick (update witness-tick :ranked-actions conj
                     {:id :bare :controller-score 0.0})] ; the OLD cascade-row shape
    (is (only-caught-by tick witness-surfaces :i2))))

(deftest i3-catches-core-drift
  (let [tick (update-in witness-tick [:ranked-actions 1 :G-core] + 1e-6)]
    (is (only-caught-by tick witness-surfaces :i3))))

(deftest i4-catches-unpersisted-formula-term
  ;; a formula that consumed :gap-exploration-bonus while the whitelist dropped it —
  ;; the exact pre-D1a defect (F2), as an abstract entry
  (let [e (-> (entry :a3 {:G-risk 0.2 :G-ambiguity 0.01 :predictability-bonus 14.0
                          :homeostatic-pressure 0.7 :structural-pressure 0.5
                          :gap-exploration-bonus 0.6 :graph-control-score 0.0 :G-goal-outcome 0.0})
              (dissoc :gap-exploration-bonus)
              ;; total still reflects the consumed gap ⇒ i1 would also fire;
              ;; persist the gap-less recomputation to isolate i4
              ((fn [e'] (assoc e' :controller-score (recompute-total e')))))
        tick (update witness-tick :ranked-actions conj e)]
    (is (only-caught-by tick witness-surfaces :i4))))

(deftest i5-catches-unearned-sense-claim
  (let [surfaces (conj witness-surfaces
                       {:id :blend-as-efe :declared-sense :sense-i :earned? false})]
    (is (only-caught-by witness-tick surfaces :i5))))
