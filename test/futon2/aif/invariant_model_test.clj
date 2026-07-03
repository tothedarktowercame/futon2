(ns futon2.aif.invariant-model-test
  "M-evaluate-policies VERIFY — the I1–I5 logic model (§8.11), per
   mission-coherence/logic-model-before-code (PSR-3).

   Checks the DESIGN, not the implementation: each invariant is a checkable
   predicate over an ABSTRACT trace (plain data, no I/O, no live state). The
   discipline: one CONFORMING WITNESS trace yields zero violations across all
   five invariants, and one ADVERSARIAL trace per invariant is caught by
   exactly the intended check. (futon2 has no core.logic dep; plain predicates
   carry the pattern's essence — checkable propositions + adversarial cases.)

   The recompute formula mirrors compute-efe's G-total assembly
   (efe.clj:449-457) at the default weights — the same formula the corpus
   census (scripts/wm_trace_census.bb) uses."
  (:require [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; The abstract model

(def weights {:info 0.4 :survival 1.2 :structural-pressure 0.35})

(defn recompute-total
  "G-total from persisted terms at default weights (post-D1a: gap and
   graph-pragmatic are persisted, so no hidden residual is tolerated)."
  [e]
  (+ (double (or (:G-risk e) 0.0))
     (double (or (:G-ambiguity e) 0.0))
     (- (* (:info weights) (double (or (:G-info e) 0.0))))
     (* (:survival weights) (double (or (:G-survival e) 0.0)))
     (- (* (:structural-pressure weights)
           (double (or (:G-structural-pressure e) 0.0))))
     (double (or (:G-graph-pragmatic e) 0.0))
     (- (double (or (:G-gap e) 0.0)))
     (double (or (:G-goal-outcome e) 0.0))))

(def decomposition-keys
  [:G-risk :G-ambiguity :G-info :G-survival :G-structural-pressure])

(defn placeholder? [e] (= :placeholder (:score-provenance e)))
(defn decomposed?  [e] (every? #(number? (get e %)) decomposition-keys))

;; ---------------------------------------------------------------------------
;; The invariants as violation-collectors (empty seq = holds)

(defn i1-replay-equality
  "Recomputed total = persisted :G-total for every decomposed entry (tol 1e-9)."
  [tick]
  (for [e (:ranked-actions tick)
        :when (decomposed? e)
        :let [d (Math/abs (- (recompute-total e) (double (:G-total e))))]
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
  [:G-risk :G-ambiguity :G-info :G-survival :G-structural-pressure
   :G-gap :G-graph-pragmatic :G-goal-outcome])

(defn entry
  "A decomposed entry whose :G-total is derived (so I1 holds by construction)
   and whose :G-core is exact (so I3 holds by construction)."
  [id terms]
  (let [e (merge {:id id :formula-terms full-terms} terms)]
    (assoc e
           :G-total (recompute-total e)
           :G-core (+ (double (:G-risk e)) (double (:G-ambiguity e))))))

(def witness-tick
  {:ranked-actions
   [(entry :a1 {:G-risk 0.156 :G-ambiguity 0.015 :G-info 13.985
                :G-survival 0.758 :G-structural-pressure 1.0
                :G-gap 0.3 :G-graph-pragmatic 0.05 :G-goal-outcome 0.0})
    (entry :a2 {:G-risk 0.31 :G-ambiguity 0.03 :G-info 13.97
                :G-survival 0.61 :G-structural-pressure 0.0
                :G-gap 0.0 :G-graph-pragmatic 0.0 :G-goal-outcome 0.12})
    ;; the cascade row: unscored but SELF-DESCRIBING (D1b)
    {:id :casc :G-total 0.0 :score-provenance :placeholder}]})

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

(defn- only-caught-by [tick surfaces expected]
  (let [vs (all-violations tick surfaces)]
    (and (seq vs) (= #{expected} (set (map :invariant vs))))))

(deftest i1-catches-tampered-total
  (let [tick (update-in witness-tick [:ranked-actions 0 :G-total] + 0.296)]
    (testing "the pre-D1a hidden-residual failure mode, replayed abstractly"
      (is (only-caught-by tick witness-surfaces :i1)))))

(deftest i2-catches-bare-constant
  (let [tick (update witness-tick :ranked-actions conj
                     {:id :bare :G-total 0.0})] ; the OLD cascade-row shape
    (is (only-caught-by tick witness-surfaces :i2))))

(deftest i3-catches-core-drift
  (let [tick (update-in witness-tick [:ranked-actions 1 :G-core] + 1e-6)]
    (is (only-caught-by tick witness-surfaces :i3))))

(deftest i4-catches-unpersisted-formula-term
  ;; a formula that consumed :G-gap while the whitelist dropped it —
  ;; the exact pre-D1a defect (F2), as an abstract entry
  (let [e (-> (entry :a3 {:G-risk 0.2 :G-ambiguity 0.01 :G-info 14.0
                          :G-survival 0.7 :G-structural-pressure 0.5
                          :G-gap 0.6 :G-graph-pragmatic 0.0 :G-goal-outcome 0.0})
              (dissoc :G-gap)
              ;; total still reflects the consumed gap ⇒ i1 would also fire;
              ;; persist the gap-less recomputation to isolate i4
              ((fn [e'] (assoc e' :G-total (recompute-total e')))))
        tick (update witness-tick :ranked-actions conj e)]
    (is (only-caught-by tick witness-surfaces :i4))))

(deftest i5-catches-unearned-sense-claim
  (let [surfaces (conj witness-surfaces
                       {:id :blend-as-efe :declared-sense :sense-i :earned? false})]
    (is (only-caught-by witness-tick surfaces :i5))))
