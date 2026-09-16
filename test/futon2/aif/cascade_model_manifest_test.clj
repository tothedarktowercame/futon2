(ns futon2.aif.cascade-model-manifest-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.set :as cset]
            [futon2.aif.cascade-model-manifest :as m]))

(def pattern-text "  + IF: ready and not blocked\n  + HOWEVER: stalled\n  + THEN: produce evidence\n  + BECAUSE: test\n")
(def target-text "# Mission: evidence\n\n**Status:** kernel instantiated; contract open\ntrace accepted dark\n\n**Owner:** someone\n")
(deftest negation-and-uninterpreted-controls
  (let [p (m/interpret-pattern "p" "pattern" pattern-text)]
    (is (true? (m/guard-holds? p #{"ready" "stalled"})))
    (is (false? (m/guard-holds? p #{"ready" "stalled" "blocked"})))
    (is (false? (m/guard-holds? p #{"ready"})))
    (is (= :missing (:status (m/compile-clause "without an explicit dial, either thrash or tunnel")))))
  (let [p (m/interpret-pattern "p" "pattern" "  + THEN: evidence\n")]
    (is (= :missing (get-in p [:transition :status])))
    (is (nil? (m/guard-holds? p #{})))
    (is (= :missing (:status (m/transition-row p #{}))))))
(deftest source-extraction-controls
  (let [t (m/extract-target "m" "mission" target-text)]
    (is (contains? (:have t) "kernel"))
    (is (contains? (:have t) "trace"))
    (is (not (contains? (:have t) "contract")))
    (is (contains? (:want t) "contract"))
    (is (some #(= :built-not-live (:classification %)) (:clauses t))))
  (let [t (m/extract-target "stem-only" "absent" "")]
    (is (not (:ok t)))
    (is (= :refused (:status (m/build-manifest t [])))))
  (let [t (m/extract-target "m" "table" (str target-text "\n| Component | Status |\n|---|---|\n| arithmetic | Built; unwired |\n| observer | Candidate |\n"))]
    (is (= :prefer-status-table (:selection-rule t)))
    (is (some #(= :built-not-live (:classification %)) (:clauses t)))
    (is (contains? (:have t) "arithmetic"))
    (is (not (contains? (:have t) "observer")))))
(deftest kernels-and-roundtrip
  (let [t (m/extract-target "m" "mission" target-text)
        p (m/interpret-pattern "p" "pattern" pattern-text)
        manifest (m/build-manifest t [p])]
    (is (m/normalized-exact? (get-in manifest [:initial-belief :mass])))
    (is (m/normalized-exact? (m/transition-row p #{"ready" "racing"})))
    (is (contains? (first (keys (m/transition-row p #{"ready" "racing"}))) "racing"))
    (is (= {1/3 1} (m/observation-row #{"a" "b" "c"} #{"a"})))
    (is (m/normalized-exact? (m/observation-row #{"a" "b" "c"} #{"a"})))
    (is (not (m/normalized-exact? {:x 1.0})))
    (is (= manifest (edn/read-string (pr-str manifest))))
    (is (some #(= :guard-unreachable-in-declared-universe (:kind %)) (:findings manifest)))
    (is (false? (:scoring-permitted? manifest)))))
(deftest actual-mission-shape
  (let [p "holes/missions/M-aif-policy-conditioned-eig.md"
        t (m/extract-target "M-aif-policy-conditioned-eig" p (slurp p))]
    (is (:ok t))
    (is (= :prefer-status-table (:selection-rule t)))
    (is (some #(= :outstanding (:classification %)) (:clauses t)))
    (is (some #(= :unclassified-clause (:kind %)) (:findings t)))))

;; Lean correspondence: mathlib4 17fb61d038, DarkTower/WarMachine/TokenState.lean,
;; fixtures fixture_coverage_half, fixture_coverage_full, fixture_observedBelief
;; with tokens "t0" "t1" "t2" for Fin 3.
(deftest token-state-lean-fixture-correspondence
  (let [want #{"t0" "t1"} state #{"t1" "t2"}]
    ;; fixture_coverage_half: coverage {0,1} {1,2} = 1/2
    (is (= 1/2 (m/coverage want state)))
    ;; fixture_coverage_full: coverage {0,1} {0,1} = 1
    (is (= 1 (m/coverage want #{"t0" "t1"})))
    ;; fixture_observedBelief: point mass at fne = {t1 t2}
    (is (= 1 (get (m/observed-belief state) state 0)))
    (is (= 0 (get (m/observed-belief state) want 0)))))

;; Lean theorems: observedBelief_sum, independentBelief_sum,
;; independentBelief_eq_observedBelief, coverage_nonneg, coverage_le_one,
;; coverage_eq_one_iff, coverage_mono, over all 8 subsets of {t0 t1 t2}.
(deftest token-state-lean-theorem-properties
  (let [univ #{"t0" "t1" "t2"}
        subs (reduce (fn [ss v] (into ss (map #(conj % v)) ss)) #{#{}} univ)
        ib (m/independent-belief {"t0" 1/2 "t1" 1/4 "t2" 1} univ)
        want #{"t0" "t1"}
        cov (fn [s] (m/coverage want s))]
    ;; observedBelief_sum: sum over states of the point mass at {t1} = 1
    (is (= 1 (reduce + (map #(get (m/observed-belief #{"t1"}) % 0) subs))))
    ;; independentBelief_sum: exactly 1, exact rational
    (is (= 1 (reduce + (vals ib))))
    ;; spot values: {t0} = 1/2·3/4·0, {t0 t2} = 1/2·3/4·1
    (is (= 0 (get ib #{"t0"})))
    (is (= 3/8 (get ib #{"t0" "t2"})))
    ;; independentBelief_eq_observedBelief: 0/1 probabilities = membership in {t1}
    (let [p01 (m/independent-belief {"t0" 0 "t1" 1 "t2" 0} univ)
          ob (m/observed-belief #{"t1"})]
      (is (every? #(= (get ob % 0) (get p01 % 0)) subs)))
    ;; coverage bounds and coverage_eq_one_iff
    (doseq [s subs]
      (is (<= 0 (cov s) 1))
      (is (= (= 1 (cov s)) (cset/subset? want s))))
    ;; coverage_mono
    (doseq [s subs s' subs :when (cset/subset? s s')]
      (is (<= (cov s) (cov s'))))))

(deftest token-state-lean-falsifiers
  ;; Lean requires want.Nonempty; empty want refuses with the typed finding
  (is (= {:status :missing :kind :empty-want-signature} (m/coverage #{} #{"t0"})))
  (is (= {:status :missing :kind :empty-want-signature} (m/observation-row #{} #{"t0"})))
  ;; probability outside [0,1] refuses with the typed finding
  (is (= :missing (:status (m/independent-belief {"t0" 3/2 "t1" 1/2} #{"t0" "t1"}))))
  (is (= :invalid-token-probability (:kind (m/independent-belief {"t0" 3/2 "t1" 1/2} #{"t0" "t1"}))))
  ;; missing token probability also refuses
  (is (= :invalid-token-probability (:kind (m/independent-belief {"t0" 1/2} #{"t0" "t1"})))))
