(ns futon2.aif.cascade-model-manifest-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.set :as cset]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.receipt-construction :as construction]))

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

;; Lean correspondence: mathlib4 c1caf481a2,
;; DarkTower/WarMachine/CascadeTransition.lean, with tokens "t0" "t1" "t2"
;; for Fin 3 (fixture_rollout_two, fixture_one_step_reversed,
;; fixture_p3_enabled_at_empty, fixture_p3_not_enabled_at_two,
;; fixture_p3_identity_when_blocked).
(defn ct-pattern
  "A pattern with an interpreted guard (present/absent token sets, absent
   acting as forbids), produces, and an interpretation theta."
  [id present absent produces theta]
  {:id id :produces produces :theta theta
   :guard {:status :interpreted :operator :and
           :clauses [{:status :interpreted :present present :absent absent}]}})

(deftest cascade-transition-lean-fixture-correspondence
  (let [p1 (ct-pattern :p1 #{} #{} #{"t0" "t1"} 1/2)
        p2 (ct-pattern :p2 #{"t0" "t1"} #{} #{"t2"} 1/3)
        prec0 [p1 p2] prec1 [p2 p1]
        q0 {#{} 1}
        two-step (m/rollout (fn [k] (if (zero? k) prec0 prec1)) q0 2)]
    ;; fixture_rollout_two: rollout at n=2 gives #{t0 t1 t2} mass θ₁·θ₂ = 1/6
    (is (= 1/6 (get two-step #{"t0" "t1" "t2"} 0)))
    ;; the distribution still sums to 1 exactly
    (is (= 1 (reduce + (vals two-step))))
    ;; fixture_one_step_reversed: [p2 p1] and [p1 p2] agree from #{} (only p1
    ;; is enabled there)
    (is (= (m/cascade-kernel prec1 #{})
           (m/cascade-kernel prec0 #{})))
    ;; fixture_p3_enabled_at_empty / fixture_p3_not_enabled_at_two /
    ;; fixture_p3_identity_when_blocked
    (let [p3 (ct-pattern :p3 #{} #{"t2"} #{"t0"} 1/2)]
      (is (true? (m/guard-holds? p3 #{})))
      (is (false? (m/guard-holds? p3 #{"t2"})))
      (is (= {#{"t2"} 1} (m/cascade-kernel [p3] #{"t2"}))))))

(deftest cascade-transition-lean-theorem-properties
  (let [univ #{"t0" "t1" "t2"}
        subs (reduce (fn [ss v] (into ss (map #(conj % v)) ss)) #{#{}} univ)
        p1 (ct-pattern :p1 #{} #{} #{"t0" "t1"} 1/2)
        p2 (ct-pattern :p2 #{"t0" "t1"} #{} #{"t2"} 1/3)
        p3 (ct-pattern :p3 #{} #{"t2"} #{"t0"} 1/2)
        precs [[p1 p2] [p2 p1] [p3] []]]
    (doseq [prec precs s subs]
      (let [k (m/cascade-kernel prec s)]
        ;; patternKernel_nonneg / cascadeKernel_nonneg
        (is (every? #(<= 0 %) (vals k)))
        ;; patternKernel_rowsum / cascadeKernel_rowsum
        (is (= 1 (reduce + (vals k)))))) 
    ;; patternKernel_of_achieved: an achieved pattern gives the identity
    (is (= {#{"t0" "t1"} 1} (m/pattern-kernel p1 #{"t0" "t1"})))
    ;; cascadeKernel_of_noEnabled: nothing enabled gives the identity
    (is (= {#{"t0"} 1} (m/cascade-kernel [p2 p3] #{"t0"})))))

(deftest cascade-transition-lean-falsifiers
  ;; theta outside [0,1] refuses with the typed outcome (Lean bounds the
  ;; structure field; the falsifier input is 3/2)
  (is (= :missing (:status (m/pattern-kernel (ct-pattern :bad #{} #{} #{"t0"} 3/2) #{}))))
  (is (= :invalid-pattern-interpretation (:kind (m/pattern-kernel (ct-pattern :bad #{} #{} #{"t0"} 3/2) #{}))))
  ;; a precedence containing a pattern without an interpretation is the typed
  ;; hole (interpret_eq_none_iff)
  (is (= :missing-pattern-interpretation
         (:kind (m/cascade-kernel [{:id :hole :produces #{"t0"}
                                    :guard {:status :missing :kind :missing-pattern-interpretation}}]
                                  #{}))))
  ;; a missing theta is the declared documented default 1, recorded, never
  ;; silent (the default is the declared interpretation, not a gap)
  (is (= {:id :doc :produces #{"t0"} :theta 1 :theta-source :documented-default}
         (m/with-pattern-theta {:id :doc :produces #{"t0"}})))
  (is (= 1 (get (m/pattern-kernel {:produces #{"t0"}} #{}) #{"t0"} 0))))

;; Lean correspondence: mathlib4 889429e6bf,
;; DarkTower/WarMachine/TokenObservation.lean, fixtures on V = Fin 2 with
;; tokens "t0" "t1" (tokenLikelihood_zeroRates_identity,
;; tokenLikelihood_noisy_miss, tokenLikelihood_noisy_hit).
(deftest token-observation-lean-fixture-correspondence
  (let [zero {"t0" {:false-neg 0 :false-pos 0}
              "t1" {:false-neg 0 :false-pos 0}}
        noisy {"t0" {:false-neg 1/10 :false-pos 0}
               "t1" {:false-neg 0 :false-pos 0}}]
    ;; tokenLikelihood_zeroRates_identity: zero rates give the identity row
    ;; (sparse: only the state itself carries mass 1)
    (is (= #{#{}} (set (keys (m/observation-distribution zero #{})))))
    (is (= 1 (get (m/observation-distribution zero #{"t0"}) #{"t0"} 0)))
    (is (= {#{"t0"} 1} (m/observation-distribution zero #{"t0"})))
    ;; tokenLikelihood_noisy_miss: falseNeg t0 = 1/10, state {t0}, obs ∅
    (is (= 1/10 (m/token-likelihood noisy #{"t0"} #{})))
    ;; tokenLikelihood_noisy_hit: same rates, obs {t0}
    (is (= 9/10 (m/token-likelihood noisy #{"t0"} #{"t0"})))))

;; Lean theorems: tokenLikelihood_nonneg, tokenLikelihood_colsum,
;; tokenLikelihood_checkable, predictedOutcome_eq_rolloutState, over all 4
;; subsets of {t0 t1}.
(deftest token-observation-lean-theorem-properties
  (let [rates {"t0" {:false-neg 1/10 :false-pos 1/5}
               "t1" {:false-neg 0 :false-pos 1/3}}
        univ #{"t0" "t1"}
        subs (reduce (fn [ss v] (into ss (map #(conj % v)) ss)) #{#{}} univ)]
    ;; tokenLikelihood_nonneg and tokenLikelihood_colsum for every state
    (doseq [s subs]
      (is (every? #(<= 0 %) (map (partial m/token-likelihood rates s) subs)))
      (is (= 1 (reduce + (vals (m/observation-distribution rates s))))))
    ;; predictedOutcome_eq_rolloutState: with zero rates,
    ;; predict-observations of a rollout distribution is that distribution
    (let [zero {"t0" {:false-neg 0 :false-pos 0}
                "t1" {:false-neg 0 :false-pos 0}}
          p1 (ct-pattern :q1 #{} #{} #{"t0"} 1/2)
          p2 (ct-pattern :q2 #{"t0"} #{} #{"t1"} 1/3)
          rolled (m/rollout (fn [k] (if (zero? k) [p1 p2] [p2 p1])) {#{} 1} 2)]
      (is (not (contains? rolled :status)))
      (is (= rolled (m/predict-observations zero rolled))))))

(deftest token-observation-lean-falsifiers
  ;; falseNeg outside [0,1] refuses with the typed outcome
  (is (= :missing (:status (m/token-likelihood {"t0" {:false-neg 3/2 :false-pos 0}
                                                "t1" {:false-neg 0 :false-pos 0}}
                                               #{"t0"} #{"t0"}))))
  (is (= :invalid-adjudication-rate
         (:kind (m/token-likelihood {"t0" {:false-neg 3/2 :false-pos 0}
                                     "t1" {:false-neg 0 :false-pos 0}}
                                    #{"t0"} #{"t0"}))))
  ;; a missing token rate also refuses (state token absent from rates)
  (is (= :invalid-adjudication-rate
         (:kind (m/token-likelihood {"t0" {:false-neg 1/10 :false-pos 0}}
                                    #{"t0" "t1"} #{"t0"}))))
  ;; an entry missing a :false-pos key refuses too
  (is (= :invalid-adjudication-rate
         (:kind (m/token-likelihood {"t0" {:false-neg 1/10}} #{"t0"} #{"t0"}))))
  ;; refusals propagate through observation-distribution and predict-observations
  (is (= :invalid-adjudication-rate
         (:kind (m/observation-distribution {"t0" {:false-neg 3/2 :false-pos 0}} #{}))))
  (is (= :invalid-adjudication-rate
         (:kind (m/predict-observations {"t0" {:false-neg 3/2 :false-pos 0}}
                                        {#{} 1})))))

(deftest enactment-prediction-agreement-add-only
  "Design P3: with add-only effects and theta = 1, once-only enactment and
   repeat-enabled prediction agree (patternKernel_of_achieved is why)."
  (let [interpretations {:a {:guard [:fact "start"] :effect {"base" true}}
                         :b {:guard [:fact "base"] :effect {"cap" true}}}
        acted (construction/acting-order interpretations {"start" true} [:a :b])
        enacted (reduce #(merge %1 (:effect (get interpretations %2)))
                        {"start" true} acted)
        pa (ct-pattern :a #{"start"} #{} #{"base"} 1)
        pb (ct-pattern :b #{"base"} #{} #{"cap"} 1)
        ;; precedence puts the not-yet-enabled pattern first; an achieved
        ;; pattern is idempotent (patternKernel_of_achieved), so re-firing it
        ;; is a no-op and the later pattern still fires
        rolled (m/rollout (constantly [pb pa]) {#{"start"} 1} (count acted))]
    (is (= [:a :b] acted))
    ;; single support state with probability 1
    (is (= 1 (count rolled)))
    (is (= #{#{"start" "base" "cap"}} (set (keys rolled))))
    ;; the facts acting-order makes true equal the predicted support state
    (is (= (set (keys (filter val enacted))) (first (keys rolled))))))

(deftest enactment-prediction-divergence-with-retraction
  "Divergence case, recorded not fixed: when an effect retracts a fact, the
   add-only cascade kernel (set-union with theta) cannot follow it. Here :a
   retracts \"start\" while producing \"mid\", so enactment (acting-order)
   blocks :b, whose guard needs \"start\"; the add-only prediction keeps
   \"start\" true forever (patternKernel_of_achieved keeps union monotone) and
   so fires :b's kernel and retains \"start\". The two disagree on the final
   state, exactly as the retraction example of
   first-firing-applies-effects-and-never-repeats shows enactment can undo."
  (let [interpretations {:a {:guard [:fact "start"] :effect {"start" false "mid" true}}
                         :b {:guard [:fact "start"] :effect {"cap" true}}}
        acted (construction/acting-order interpretations {"start" true} [:a :b])
        enacted (set (keys (filter val (reduce #(merge %1 (:effect (get interpretations %2)))
                                               {"start" true} acted))))
        pa (ct-pattern :a #{"start"} #{} #{"mid"} 1)
        pb (ct-pattern :b #{"start"} #{} #{"cap"} 1)
        rolled (m/rollout (constantly [pa pb]) {#{"start"} 1} (count acted))
        predicted (first (keys rolled))]
    (is (= [:a] acted))
    (is (not= enacted predicted))
    ;; enacted lost "start" (retracted) and never got "cap" (:b blocked)
    (is (contains? predicted "start"))
    (is (not (contains? enacted "start")))
    (is (not (contains? enacted "cap")))))

