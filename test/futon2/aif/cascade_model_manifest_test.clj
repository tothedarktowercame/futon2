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
  "Design P3/P10: with add-only effects, theta = 1 and the SAME precedence on
   both sides ([:a :b] enactment, [pa pb] prediction), once-per-step enactment
   with achievement-as-completion and the continuing-guard rollout agree."
  (let [interpretations {:a {:guard [:fact "start"] :effect {"base" true}}
                         :b {:guard [:fact "base"] :effect {"cap" true}}}
        acted (construction/acting-order interpretations {"start" true} [:a :b])
        enacted (set (keys (filter val (reduce #(merge %1 (:effect (get interpretations %2)))
                                               {"start" true} acted))))
        pa (ct-pattern :a #{"start"} #{} #{"base"} 1)
        pb (ct-pattern :b #{"base"} #{} #{"cap"} 1)
        rolled (m/rollout (constantly [pa pb]) {#{"start"} 1} (count acted))]
    (is (= [:a :b] acted))
    (is (= 1 (count rolled)))
    (is (= #{#{"start" "base" "cap"}} (set (keys rolled))))
    (is (= enacted (first (keys rolled))))))

(deftest retraction-refused-withdrawal-token-agrees
  "P10 item 2: retraction is refused (:retracting-effect-forbidden); the same
   scenario written add-only, with a withdrawal token the dependent guard
   forbids, agrees between enactment and prediction with probability 1."
  ;; enactment refuses the retracting interpretation
  (is (= :retracting-effect-forbidden
         (:construction/refusal
           (try (construction/acting-order
                  {:a {:guard [:fact "start"] :effect {"start" false "mid" true}}}
                  {"start" true} [:a])
                nil
                (catch clojure.lang.ExceptionInfo e (ex-data e))))))
  ;; the same scenario with a withdrawal token: :a produces "mid" and
  ;; "start-withdrawn"; :b guards on "start" and not "start-withdrawn"
  (let [interpretations {:a {:guard [:fact "start"]
                             :effect {"mid" true "start-withdrawn" true}}
                         :b {:guard [:and [:fact "start"] [:not [:fact "start-withdrawn"]]]
                             :effect {"cap" true}}}
        acted (construction/acting-order interpretations {"start" true} [:a :b])
        enacted (set (keys (filter val (reduce #(merge %1 (:effect (get interpretations %2)))
                                               {"start" true} acted))))
        pa (ct-pattern :a #{"start"} #{} #{"mid" "start-withdrawn"} 1)
        pb (ct-pattern :b #{"start"} #{"start-withdrawn"} #{"cap"} 1)
        rolled (m/rollout (constantly [pa pb]) {#{"start"} 1} (count acted))]
    (is (= [:a] acted))
    ;; prediction with the same precedence gives the same final true-fact set
    ;; with probability 1 (the withdrawal token blocks pb's guard)
    (is (= {enacted 1} rolled))))

;; P10 continuing guard: mathlib4 95127698bd,
;; DarkTower/WarMachine/CascadeTransition.lean — firstEnabled_skips_achieved,
;; fixture_same_precedence_chain, fixture_situation_i … _v. The guard now
;; also requires produces ⊄ state, so a completed pattern no longer wins
;; precedence (guards are re-evaluated every step).
(deftest cascade-transition-p10-same-precedence-chain
  (let [p1 (ct-pattern :p1 #{} #{} #{"t0" "t1"} 1)
        p2 (ct-pattern :p2 #{"t0" "t1"} #{} #{"t2"} 1)]
    ;; firstEnabled_skips_achieved: at {t0 t1} the achieved p1 is skipped
    (is (= :p2 (:id (m/first-enabled [p1 p2] #{"t0" "t1"}))))
    ;; fixture_same_precedence_chain: the SAME precedence at both steps
    ;; reaches the full state with probability 1 (the old guard returned the
    ;; identity here and p2 never fired)
    (is (= {#{"t0" "t1" "t2"} 1}
           (m/rollout (constantly [p1 p2]) {#{} 1} 2)))))

;; Lean fixture_situation_i … fixture_situation_v over the permission tokens,
;; θ = 1/2.
(deftest cascade-transition-p10-five-situations
  (let [patA (ct-pattern :a #{"perm"} #{"withdrawn"} #{"aOut"} 1/2)
        patB (ct-pattern :b #{"perm"} #{"withdrawn"} #{"bOut"} 1/2)
        patB2 (ct-pattern :b2 #{"perm2"} #{"withdrawn2"} #{"bOut"} 1/2)]
    ;; (i) permission holds, aOut not achieved: A is enabled
    (is (true? (m/guard-holds? patA #{"perm"})))
    ;; (ii) failed attempt leaves the state unchanged with mass 1 − θ, and A
    ;; is still enabled there at the next step
    (is (= 1/2 (get (m/pattern-kernel patA #{"perm"}) #{"perm"} 0)))
    (is (true? (m/guard-holds? patA #{"perm"})))
    ;; (iii) at {perm, withdrawn} neither A nor B is enabled: identity
    (is (= {#{"perm" "withdrawn"} 1}
           (m/cascade-kernel [patA patB] #{"perm" "withdrawn"})))
    ;; (iv) withdrawal present before B starts: B is not enabled
    (is (false? (m/guard-holds? patB #{"perm" "withdrawn"})))
    ;; (v) identity under both precedence orders; only a renewed permission
    ;; perm2 with its own pattern B2 enables production of bOut
    (is (= {#{"perm" "withdrawn"} 1}
           (m/cascade-kernel [patB patA] #{"perm" "withdrawn"})))
    (is (= :b2 (:id (m/first-enabled [patB2 patA patB]
                                     #{"perm" "withdrawn" "perm2"}))))))

(deftest cascade-transition-p10-achieved-first-is-skipped
  "Falsifier for the old guard: an achieved pattern placed first in
   precedence is skipped and the next pattern fires. Under the old guard
   cascade-kernel returned the identity {state 1} here."
  (let [p1 (ct-pattern :p1 #{} #{} #{"t0" "t1"} 1)
        p2 (ct-pattern :p2 #{"t0" "t1"} #{} #{"t2"} 1)]
    (is (not= {#{"t0" "t1"} 1} (m/cascade-kernel [p1 p2] #{"t0" "t1"})))
    (is (= {#{"t0" "t1" "t2"} 1} (m/cascade-kernel [p1 p2] #{"t0" "t1"})))))

;; P10 part 2: Joe's five situations through BOTH enactment
;; (construction/acting-order, continuing guards, achievement-as-completion)
;; and prediction (m/rollout), theta = 1, the SAME precedence on both sides.
(defn p10-interps []
  {:a {:guard [:and [:fact "perm"] [:not [:fact "withdrawn"]]] :effect {"aOut" true}}
   :b {:guard [:and [:fact "perm"] [:not [:fact "withdrawn"]]] :effect {"bOut" true}}
   :b2 {:guard [:and [:fact "perm2"] [:not [:fact "withdrawn2"]]] :effect {"bOut" true}}
   :w {:guard [:fact "perm"] :effect {"withdrawn" true}}})

(defn p10-enacted
  "The true facts after acting-order runs, folded from q0."
  [interps q0 acted]
  (set (keys (filter val (reduce #(merge %1 (:effect (get interps %2))) q0 acted)))))

(def p10-token-universe
  "Under strong Kleene an absent fact is :unknown and a [:not ...] guard over
   it is unknown (never true), so the fact map carries every token in the
   universe explicitly: true if in the state, false otherwise."
  ["perm" "withdrawn" "aOut" "bOut" "perm2" "withdrawn2"])

(defn p10-agrees [interps order state precedence]
  (let [facts0 (into {} (map (fn [t] [t (contains? state t)])) p10-token-universe)
        acted (construction/acting-order interps facts0 order)
        enacted (p10-enacted interps facts0 acted)
        rolled (m/rollout (constantly precedence) {state 1} (count acted))]
    {:acted acted :enacted enacted :rolled rolled}))

(deftest p10-five-situations-enactment-agrees
  (let [interps (p10-interps)
        pa (ct-pattern :a #{"perm"} #{"withdrawn"} #{"aOut"} 1)
        pb (ct-pattern :b #{"perm"} #{"withdrawn"} #{"bOut"} 1)
        pb2 (ct-pattern :b2 #{"perm2"} #{"withdrawn2"} #{"bOut"} 1)
        pw (ct-pattern :w #{"perm"} #{} #{"withdrawn"} 1)
        agree (fn [order state prec]
                (p10-agrees interps order (into (sorted-set) state) prec))]
    ;; (i) from {perm} with order [:a]: A acts, the final state has aOut
    (let [{:keys [acted enacted rolled]} (agree [:a] ["perm"] [pa])]
      (is (= [:a] acted))
      (is (contains? enacted "aOut"))
      (is (= {enacted 1} rolled)))
    ;; (ii) from {perm, aOut}: A does not act again (achieved is completion)
    (let [{:keys [acted enacted rolled]} (agree [:a] ["perm" "aOut"] [pa])]
      (is (= [] acted))
      (is (= #{"perm" "aOut"} enacted))
      (is (= {enacted 1} rolled)))
    ;; (iii) from {perm, withdrawn} with [:a :b]: nothing acts, unchanged
    (let [{:keys [acted enacted rolled]} (agree [:a :b] ["perm" "withdrawn"] [pa pb])]
      (is (= [] acted))
      (is (= #{"perm" "withdrawn"} enacted))
      (is (= {enacted 1} rolled)))
    ;; (iv) from {perm, withdrawn} with [:b]: B doesn't start
    (let [{:keys [acted]} (agree [:b] ["perm" "withdrawn"] [pb])]
      (is (= [] acted)))
    ;; (v) from {perm, withdrawn, perm2} with [:b2 :a :b]: only B2 acts,
    ;; yielding bOut
    (let [{:keys [acted enacted rolled]} (agree [:b2 :a :b] ["perm" "withdrawn" "perm2"]
                                                 [pb2 pa pb])]
      (is (= [:b2] acted))
      (is (contains? enacted "bOut"))
      (is (= {enacted 1} rolled)))
    ;; mid-run withdrawal: :w produces "withdrawn" from {perm}; order
    ;; [:w :b] acts W first and the withdrawal token stops B. Both engines
    ;; agree on the final state.
    (let [{:keys [acted enacted rolled]} (p10-agrees interps [:w :b] #{"perm"} [pw pb])]
      (is (= [:w] acted))
      (is (= #{"perm" "withdrawn"} enacted))
      (is (= {enacted 1} rolled)))))


(deftest token-preference-lean-fixture-correspondence
  "Lean TokenPreference.Fixture (mathlib4 678c797666) replayed through the
   runtime functions: c0 has want {t0,t1}, evidence {t2}, lam = mu = 1,
   zeroed ∅, universe {t0,t1,t2}. Asserts the exact rational utilities
   (u_full = 2, u_01 = 1, u_0 = 1/2, u_empty = 0), the strict fixture_chain
   with < on doubles, and fixture_sum within 1e-12."
  (let [spec (m/preference-spec {:want #{"t0" "t1"} :evidence #{"t2"}
                                 :lam 1 :mu 1 :zeroed #{}})
        universe #{"t0" "t1" "t2"}]
    (is (and (map? spec) (not (contains? spec :status))))
    (is (= 2 (m/token-utility spec #{"t0" "t1" "t2"})))
    (is (= 1 (m/token-utility spec #{"t0" "t1"})))
    (is (= 1/2 (m/token-utility spec #{"t0"})))
    (is (= 0 (m/token-utility spec #{})))
    (let [p (m/preference-distribution spec universe)]
      (is (< (p #{"t0" "t1"}) (p #{"t0" "t1" "t2"})))
      (is (< (p #{"t0"}) (p #{"t0" "t1"})))
      (is (< (p #{}) (p #{"t0"})))
      (is (< (Math/abs (- 1.0 (reduce + (vals p)))) 1e-12)))))

(deftest token-preference-lean-theorem-properties
  "Lean preference_pos_iff / preference_eq_zero_iff / preference_sum /
   preference_lt_of_want_lt / preference_congr with zeroed = #{#{t2}}."
  (let [spec (m/preference-spec {:want #{:t0 :t1} :evidence #{:t2}
                                 :lam 1 :mu 1 :zeroed #{#{:t2}}})
        universe #{:t0 :t1 :t2}
        p (m/preference-distribution spec universe)]
    ;; nonneg and normalized (preference_nonneg, preference_sum)
    (is (every? (fn [v] (or (zero? v) (pos? v))) (vals p)))
    (is (< (Math/abs (- 1.0 (reduce + (vals p)))) 1e-12))
    ;; zero exactly on the zeroed set, positive elsewhere (pos_iff/eq_zero_iff)
    (is (zero? (p #{:t2})))
    (is (every? pos? (vals (dissoc p #{:t2}))))
    ;; stalling: equal evidence counts, strictly more want coverage strictly
    ;; preferred (preference_lt_of_want_lt)
    (is (= (count (clojure.set/intersection (:evidence spec) #{:t0}))
           (count (clojure.set/intersection (:evidence spec) #{:t0 :t1}))))
    (is (< (count (clojure.set/intersection (:want spec) #{:t0}))
           (count (clojure.set/intersection (:want spec) #{:t0 :t1}))))
    (is (< (p #{:t0}) (p #{:t0 :t1})))
    ;; content-blind: equal counts give equal preference (preference_congr)
    (is (= (p #{:t0}) (p #{:t1})))))

(deftest token-preference-lean-falsifiers
  ;; Lean lam_pos and want_nonempty as typed refusals.
  (let [bad-lam (m/preference-spec {:want #{"t0"} :evidence #{}
                                    :lam 0 :mu 1 :zeroed #{}})
        bad-want (m/preference-spec {:want #{} :evidence #{}
                                     :lam 1 :mu 1 :zeroed #{}})]
    (is (= :missing (:status bad-lam)))
    (is (= :invalid-preference-spec (:kind bad-lam)))
    (is (= :lam (:field bad-lam)))
    (is (= :missing (:status bad-want)))
    (is (= :invalid-preference-spec (:kind bad-want)))
    (is (= :want (:field bad-want)))))


;; Lean fixture PolicyHorizon.fxModel replayed on the token carriers: states
;; are subsets of #{:a :b} with false ↦ #{} and true ↦ #{:b}; A is the identity
;; kernel via all-zero adjudication rates (tokenLikelihood_checkable); q0 is
;; the point mass at false. πstay never fires; πflip fires one guard-true
;; θ=1 pattern producing :b at step 2 only, so the two policies share their
;; first action exactly as fx stay/flip do.
(def fx-rates {:a {:false-neg 0 :false-pos 0} :b {:false-neg 0 :false-pos 0}})
(def fx-q0 {#{} 1})
(def fx-stay (constantly []))
(def fx-flip-pattern
  {:id :flip
   :guard {:status :interpreted :operator :and
           :clauses [{:status :interpreted :present #{} :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{:b}}
   :produces #{:b}})
(def fx-flip (fn [k] (if (= k 1) [fx-flip-pattern] [])))
;; fxC: outcome false preferred 3/4; fxC' (step 2 only): true preferred 3/4.
(def fx-c1 {#{} 3/4 #{:b} 1/4})
(def fx-c2 {#{} 1/4 #{:b} 3/4})
(def fx-step-indexed-c (fn [tau] (if (= tau 2) fx-c2 fx-c1)))
(def fx-constant-c (constantly fx-c1))

(deftest horizon-g-lean-fixture-correspondence
  (let [ln43 (Math/log (/ 4.0 3.0)) ln4 (Math/log 4.0)
        base {:rates fx-rates :q0 fx-q0 :horizon 2}
        g (fn [prec c-fn] (m/horizon-g (assoc base :precedence-fn prec :c-fn c-fn)))]
    ;; fixture_stepIndexed_preference: stay = ln(4/3)+ln 4, flip = 2·ln(4/3)
    (is (< (Math/abs (- (g fx-stay fx-step-indexed-c) (+ ln43 ln4))) 1e-12))
    (is (< (Math/abs (- (g fx-flip fx-step-indexed-c) (* 2 ln43))) 1e-12))
    ;; and the ranking reverses against the constant C (fixture_depth_two_differs)
    (is (< (Math/abs (- (g fx-stay fx-constant-c) (* 2 ln43))) 1e-12))
    (is (< (Math/abs (- (g fx-flip fx-constant-c) (+ ln43 ln4))) 1e-12))
    (is (< (g fx-flip fx-step-indexed-c) (g fx-stay fx-step-indexed-c)))
    (is (< (g fx-stay fx-constant-c) (g fx-flip fx-constant-c)))
    ;; horizonEFE_succ: G at T=2 is the T=1 value plus the step-2 term.
    (let [g1-flip (m/horizon-g {:rates fx-rates :q0 fx-q0 :precedence-fn fx-flip
                                :horizon 1 :c-fn fx-step-indexed-c})
          step2-flip (+ (m/outcome-risk {#{:b} 1} (fx-step-indexed-c 2))
                        (m/step-ambiguity fx-rates {#{:b} 1}))]
      (is (< (Math/abs (- g1-flip ln43)) 1e-12))
      (is (< (Math/abs (- (g fx-flip fx-step-indexed-c) (+ g1-flip step2-flip))) 1e-12)))))

(deftest horizon-g-infinite-risk
  ;; horizonEFE_eq_top_iff: C zero on an outcome the rollout reaches with
  ;; positive mass gives :infinite at that step, hence for the horizon.
  (is (= :infinite (m/horizon-g {:rates fx-rates :q0 fx-q0 :precedence-fn fx-stay
                                 :horizon 2 :c-fn (constantly {#{:b} 1.0})})))
  ;; the typed refusals propagate: an out-of-range rate and a bad horizon.
  (is (= :missing (:status (m/horizon-g {:rates {:a {:false-neg 0 :false-pos 2}}
                                         :q0 fx-q0 :precedence-fn fx-stay
                                         :horizon 1 :c-fn fx-constant-c}))))
  (is (= :invalid-adjudication-rate
         (:kind (m/horizon-g {:rates {:a {:false-neg 0 :false-pos 2}}
                              :q0 fx-q0 :precedence-fn fx-stay
                              :horizon 1 :c-fn fx-constant-c}))))
  (is (= :invalid-horizon
         (:kind (m/horizon-g {:rates fx-rates :q0 fx-q0 :precedence-fn fx-stay
                              :horizon 0 :c-fn fx-constant-c})))))

(deftest outcome-risk-properties
  ;; stepRisk_nonneg (Gibbs): KL >= 0 on proper distributions ...
  (is (<= 0 (m/outcome-risk {:x 0.5 :y 0.5} {:x 0.25 :y 0.75})))
  (is (<= 0 (m/outcome-risk {:x 0.9 :y 0.1} {:x 0.5 :y 0.5})))
  ;; ... and 0 exactly when q = c.
  (is (= 0.0 (m/outcome-risk {:x 0.25 :y 0.75} {:x 0.25 :y 0.75})))
  ;; outcomeRisk: :infinite iff some q(o) > 0 has c(o) = 0, missing key = 0.
  (is (= :infinite (m/outcome-risk {:x 0.5 :y 0.5} {:x 1.0})))
  ;; a zero-preferred outcome with zero predicted mass is NOT infinite, and
  ;; the sum runs only over positive q mass.
  (is (= 0.0 (m/outcome-risk {:x 1.0} {:x 1.0 :y 0.0})))
  (is (< (Math/abs (- (m/outcome-risk {#{:b} 1} {#{} 1/4 #{:b} 3/4})
                     (Math/log (/ 4.0 3.0)))) 1e-12)))

;; --- P11 step 1b-i: exact G at mission scale (no powerset enumeration) ---

(defn- sparse-spec [n-want n-evidence zeroed]
  (let [want (into #{} (map (partial str "t")) (range n-want))
        evidence (into #{} (map (partial str "e")) (range n-evidence))]
    {:want want :evidence evidence :lam 2 :mu 1/2 :zeroed zeroed}))

(defn- sparse-rates [spec]
  (into {} (map (fn [t] [t {:false-neg 0 :false-pos 0}]))
        (cset/union (:want spec) (:evidence spec) #{:seed})))

(deftest preference-fn-equals-distribution
  ;; On a 4-token universe (2 want + 2 evidence, one zeroed subset), the
  ;; closed-form pointwise C equals preference-distribution on all 16 subsets.
  (let [spec (m/preference-spec (sparse-spec 2 2 #{#{"t0" "e1"}}))
        universe (:universe spec)
        pd (m/preference-distribution spec universe)
        pf (m/preference-fn (sparse-spec 2 2 #{#{"t0" "e1"}}))]
    (is (fn? pf))
    (let [subsets (reduce (fn [ss v] (into ss (map #(cset/union % #{v})) ss)) [#{}] universe)]
      (is (= 16 (count subsets)))
      (doseq [s subsets]
        (is (< (Math/abs (- (pf s) (get pd s 0.0))) 1e-12))))))

(deftest sparse-g-equals-enumerating-g
  ;; On 2-, 3- and 4-token universes at zero rates, the sparse evaluation
  ;; gives the same numbers as the enumerating horizon-g within 1e-12, and
  ;; the same :infinite when a zeroed outcome is reached with positive mass.
  (doseq [n [2 3 4]
          :let [spec (m/preference-spec (sparse-spec (dec n) 1 #{}))
                universe (:universe spec)
                rates (sparse-rates spec)
                cmap (m/preference-distribution spec universe)
                q0 {#{} 1}
                p1 {:id :p1 :guard {:status :interpreted :operator :and
                                    :clauses [{:status :interpreted :present #{} :absent #{}}]}
                    :transition {:status :interpreted :operator :union :produces #{(first (:want spec))}}
                    :produces #{(first (:want spec))}}
                p2 {:id :p2 :guard {:status :interpreted :operator :and
                                    :clauses [{:status :interpreted :present (into #{} (take 1 (:want spec))) :absent #{}}]}
                    :transition {:status :interpreted :operator :union :produces #{(first (:evidence spec))}}
                    :produces #{(first (:evidence spec))}}
                cascades [(constantly [])
                          (fn [k] (if (even? k) [p1] []))
                          (fn [k] (if (= k 1) [p2] [p1]))]]]
    (doseq [prec cascades
            horizon [1 2 3]]
      (is (< (Math/abs (- (m/horizon-g-sparse {:rates rates :q0 q0 :precedence-fn prec
                                               :horizon horizon :spec spec})
                         (m/horizon-g {:rates rates :q0 q0 :precedence-fn prec
                                       :horizon horizon :c-fn (constantly cmap)})))
             1e-12)
          (str "n=" n " h=" horizon))))
  ;; zeroed outcome reached: both :infinite (preference_eq_zero_iff +
  ;; horizonEFE_eq_top_iff).
  (let [spec (m/preference-spec (sparse-spec 2 1 #{#{"t0"}}))
        universe (:universe spec)
        rates (sparse-rates spec)
        cmap (m/preference-distribution spec universe)
        q0 {#{} 1}
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{"t0"}}
              :produces #{"t0"}}]
    (is (= :infinite
           (m/horizon-g-sparse {:rates rates :q0 q0 :precedence-fn (constantly [fire])
                                :horizon 2 :spec spec})))
    (is (= :infinite
           (m/horizon-g {:rates rates :q0 q0 :precedence-fn (constantly [fire])
                         :horizon 2 :c-fn (constantly cmap)})))))

(deftest sparse-g-mission-scale
  ;; 40 tokens, 20 want + 5 evidence, three firing steps, horizon 3: exact
  ;; and fast — no 2^40 enumeration anywhere.
  (let [want (into #{} (map (partial str "t")) (range 20))
        evidence (into #{} (map (partial str "e")) (range 5))
        spec {:want want :evidence evidence :lam 3 :mu 1/4 :zeroed #{}}
        rates (into {} (map (fn [t] [t {:false-neg 0 :false-pos 0}]))
                    (cset/union want evidence))
        have (into #{} (map (partial str "h")) (range 5))
        q0 {have 1}
        p1 {:id :p1 :guard {:status :interpreted :operator :and
                            :clauses [{:status :interpreted :present #{} :absent #{}}]}
            :transition {:status :interpreted :operator :union :produces #{"t0" "t1"}}
            :produces #{"t0" "t1"}}
        p2 {:id :p2 :guard {:status :interpreted :operator :and
                            :clauses [{:status :interpreted :present #{"t0"} :absent #{}}]}
            :transition {:status :interpreted :operator :union :produces #{"t2" "e0"}}
            :produces #{"t2" "e0"}}
        prec (fn [k] (cond (= k 0) [p1] (= k 1) [p2] :else []))
        t0 (System/nanoTime)
        ;; WM-06: the established `have` tokens are part of Q's support, so C's
        ;; universe must contain them — the live tick (efe/rank-cascade-actions,
        ;; shadow-cascade-g) always unions q0's support into the universe.
        g (m/horizon-g-sparse {:rates rates :q0 q0 :precedence-fn prec
                               :horizon 3 :spec spec
                               :universe (cset/union want evidence have)})
        elapsed (- (System/nanoTime) t0)]
    (is (and (double? g) (Double/isFinite g) (pos? g)))
    (is (< elapsed 5e9) (str "elapsed ns: " elapsed))))

(deftest sparse-g-judgement-rates-refused
  ;; Non-zero adjudication rates: the typed declared-limitation refusal, not
  ;; an approximation.
  (let [r (m/horizon-g-sparse {:rates {:a {:false-neg 1/8 :false-pos 0}}
                               :q0 {#{} 1} :precedence-fn (constantly [])
                               :horizon 1 :spec (sparse-spec 1 1 #{})})]
    (is (= :missing (:status r)))
    (is (= :judgement-rates-not-supported-at-scale (:kind r))))
  ;; an invalid preference spec still refuses like preference-spec
  (let [r (m/horizon-g-sparse {:rates {:a {:false-neg 0 :false-pos 0}}
                               :q0 {#{} 1} :precedence-fn (constantly [])
                               :horizon 1 :spec {:want #{} :evidence #{} :lam 1 :mu 1 :zeroed #{}}})]
    (is (= :missing (:status r)))
    (is (= :invalid-preference-spec (:kind r)))))

(deftest preference-fn-universe-matches-enumerating
  ;; Review fix (claude-4): C is over subsets of the comparison's full token
  ;; universe V, not just want ∪ evidence ∪ zeroed. With one extra
  ;; zero-weight token, preference-fn with that universe equals
  ;; preference-distribution over the enlarged universe on all subsets.
  (let [raw (sparse-spec 2 1 #{})
        spec (m/preference-spec raw)
        big (cset/union (:universe spec) #{"x0"})
        pd (m/preference-distribution spec big)
        pf (m/preference-fn raw big)
        subsets (reduce (fn [ss v] (into ss (map #(cset/union % #{v})) ss)) [#{}] big)]
    (is (= 16 (count subsets)))
    (doseq [s subsets]
      (is (< (Math/abs (- (pf s) (get pd s 0.0))) 1e-12)))))

(deftest log-preference-fn-scales-and-zeroed-check-safe
  ;; Review fix (claude-4): log-space Z stays finite at 1200 tokens (a plain
  ;; product of (1 + e^w) overflows near 1000), and the zeroed-covers-universe
  ;; check does not misfire past 62 tokens (a long shift wraps mod 64).
  (let [raw (sparse-spec 20 5 #{})
        extra (into #{} (map (partial str "x")) (range 1175))
        lpf (m/log-preference-fn raw extra)
        some-o #{"t0" "e0"}]
    (is (fn? lpf))
    (is (Double/isFinite (lpf some-o)))
    (is (neg? (lpf some-o))))
  (let [raw (sparse-spec 40 30 #{#{"t0"}})
        lpf (m/log-preference-fn raw)]
    (is (fn? lpf) "one zeroed subset in a 70-token universe must not be refused")
    (is (= ##-Inf (lpf #{"t0"})))))

(deftest horizon-g-sparse-universe-offset
  ;; Review fix (claude-4): enlarging the comparison universe by k zero-weight
  ;; tokens lowers every ln c(o) by k·ln 2, so G rises by T·k·ln 2. Candidates
  ;; must therefore be scored over one common universe.
  (let [spec (sparse-spec 2 1 #{})
        rates (sparse-rates spec)
        p1 {:id :p1 :guard {:status :interpreted :operator :and
                            :clauses [{:status :interpreted :present #{} :absent #{}}]}
            :transition {:status :interpreted :operator :union :produces #{"t0"}}
            :produces #{"t0"} :theta 1}
        prec (constantly [p1])
        base {:rates rates :q0 {#{} 1} :precedence-fn prec :horizon 2 :spec spec}
        g0 (m/horizon-g-sparse base)
        ;; the larger universe must still CONTAIN the scored tokens (WM-06:
        ;; Q and C meet on one domain); the offset comes from extra
        ;; zero-weight tokens, not from dropping scored tokens out of C.
        g3 (m/horizon-g-sparse
            (assoc base :universe (cset/union (:want spec) (:evidence spec)
                                               #{"x0" "x1" "x2"})))]
    (is (double? g0))
    (is (< (Math/abs (- (- g3 g0) (* 2 3 (Math/log 2)))) 1e-9))))

;; WM-02 P12: Lean DarkTower.WarMachine.ExactBeliefTrajectory (mathlib4
;; ba0eda16df): exact-update (exactUpdate), token-belief-at (tokenBeliefAt),
;; fixture_exact_accepts, tokenBeliefAt_dist, exactUpdate_eq_meanField_observed,
;; exactUpdate_eq_none_iff / BeliefTrajectory.fixture_meanField_refuses_possible.

(defn- ebt-noisy [s o] (if (= s o) 9/10 1/10))       ; BeliefTrajectory.fxAnoisy
(defn- ebt-flip [] {:t {:f 1} :f {:t 1}})            ; BeliefTrajectory.fxB true

(deftest exact-belief-lean-fixture-correspondence
  ;; fixture_exact_accepts: a uniform belief through the deterministic flip,
  ;; observed :t with the noisy 9/10 likelihood: the exact posterior is
  ;; {t 9/10, f 1/10}.
  (is (= {:t 9/10 :f 1/10} (m/exact-update ebt-noisy {:t 1/2 :f 1/2} :t)))
  ;; P(o) = 1/2 (the second conjunct of fixture_meanField_refuses_possible)
  (is (= 1/2 (reduce + (map (fn [[s mass]] (* (ebt-noisy s :t) mass)) {:t 1/2 :f 1/2})))))

(deftest exact-belief-properties
  (let [rates {"a" {:false-neg 1/4 :false-pos 0} "b" {:false-neg 1/4 :false-pos 0}}
        rates0 {"a" {:false-neg 0 :false-pos 0} "b" {:false-neg 0 :false-pos 0}}
        p (assoc (m/interpret-pattern "p" "pattern" "  + IF: a\n  + HOWEVER: not blocked\n  + THEN: b\n") :theta 1/2)
        s0 #{"a"} target #{"a" "b"}
        plans (constantly [p])
        obs1 (fn [k] (if (= k 1) target #{"a" "b"}))]
    ;; tokenBeliefAt_dist: every stored belief is a distribution, exact rationals
    (doseq [t (range 3)
            :let [b (m/token-belief-at rates0 (m/observed-belief s0) plans obs1 t)]]
      (is (map? b))
      (is (every? #(and (or (ratio? %) (integer? %)) (<= 0 %)) (vals b)))
      (is (= 1 (reduce + (vals b)))))
    ;; the first two stored beliefs are as computed by hand (zero-mass entries
    ;; retained by exact-update, as Lean's function does; dropped for comparison)
    (is (= {s0 1} (m/token-belief-at rates0 (m/observed-belief s0) plans obs1 0)))
    (is (= {target 1}
           (into {} (remove (comp zero? val))
                 (m/token-belief-at rates0 (m/observed-belief s0) plans obs1 1))))
    ;; exactUpdate_eq_meanField_observed: from a point-mass previous belief the
    ;; exact update equals the one-step Bayes formula
    ;; A(o|x)·B(s₀,x) / Σ_y A(o|y)·B(s₀,y)
    (let [pushed (m/cascade-kernel [p] s0)
          o target
          like (fn [s ob] (m/token-likelihood rates s ob))
          exact (m/exact-update like pushed o)
          bayes (let [num (fn [x] (* (like x o) (get pushed x 0)))
                      po (reduce + (map num (keys pushed)))]
                  (into {} (map (fn [[x _]] [x (/ (num x) po)]) pushed)))
          drop-zero (fn [d] (into {} (remove (comp zero? val)) d))]
      (is (= (drop-zero bayes) (drop-zero exact))))
    ;; exactUpdate_eq_none_iff: an observation of zero predictive probability
    ;; gives the typed refusal, carried forward for later t
    (let [obs (fn [k] (if (= k 1) target #{"a"}))
          traj2 (m/token-belief-at rates0 (m/observed-belief s0) plans obs 2)
          traj3 (m/token-belief-at rates0 (m/observed-belief s0) plans obs 3)]
      (is (= {:status :missing :kind :zero-predictive-probability}
             (select-keys traj2 [:status :kind])))
      ;; the refusal is carried forward
      (is (= traj2 traj3)))
    ;; with no pattern firing (identity cascade), observing the state itself
    ;; with identity rates keeps the point mass
    (is (= {s0 1} (m/token-belief-at rates0 (m/observed-belief s0)
                                     (constantly []) (constantly s0) 1)))))

(deftest exact-belief-falsifiers
  ;; Negative control (BeliefTrajectory.fixture_meanField_refuses_possible /
  ;; stateWeight_eq_zero): a mean-field-style update, whose state weight is
  ;; ∏_{s ∈ supp(q)} (A(x,o)·B(s,x))^{q(s)} and which refuses when every
  ;; state's weight contains a zero factor, fails the fixture input that
  ;; exact-update accepts.
  (let [a ebt-noisy
        flip (ebt-flip)
        q {:t 1/2 :f 1/2}
        states [:t :f]
        zero-factor (fn [x] (some (fn [s] (and (pos? (get q s 0))
                                               (zero? (* (a x :t) (get-in flip [s x] 0)))))
                                  states))
        mean-field (fn []
                     (if (every? zero-factor states)
                       {:status :missing :kind :mean-field-no-solution}
                       (throw (ex-info "control: not refused" {}))))]
    ;; the mean-field weights of both states contain a zero factor
    (is (every? zero-factor states))
    ;; so the mean-field update refuses this input
    (is (= :missing (:status (mean-field))))
    ;; while the exact update accepts and computes the 9/10 posterior
    (is (= {:t 9/10 :f 1/10} (m/exact-update a q :t)))
    ;; and the P(o) = 0 falsifier gives the typed refusal
    (is (= {:status :missing :kind :zero-predictive-probability}
           (select-keys (m/exact-update (fn [_ _] 0) {:t 1} :t) [:status :kind])))))

;;; WM-06: Q and C meet on ONE outcome domain; zero-preference preserved.

(deftest wm-06-q-support-outside-c-universe-refuses
  ;; Positive Q mass on a state carrying tokens outside C's universe is the
  ;; typed refusal :q-support-outside-c-universe — never a silent projection
  ;; (C's u(o) sums only over the universe, so distinct outcomes outside it
  ;; would collapse to one C value).
  (let [spec (sparse-spec 2 1 #{})
        rates (sparse-rates spec)
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{"t0"}}
              :produces #{"t0"} :theta 1}]
    ;; the declared universe omits the produced want token: refused at step 1
    (is (= {:status :missing :kind :q-support-outside-c-universe
            :state #{"t0"} :step 1 :universe 1}
           (m/horizon-g-sparse {:rates rates :q0 {#{} 1}
                                :precedence-fn (constantly [fire])
                                :horizon 2 :spec spec
                                :universe #{"e0"}})))
    ;; the derived universe (want ∪ evidence) omits q0's established token
    (is (= :q-support-outside-c-universe
           (:kind (m/horizon-g-sparse {:rates rates :q0 {#{"h0"} 1}
                                       :precedence-fn (constantly [])
                                       :horizon 1 :spec spec}))))
    ;; :c-fn-pointwise declares its own domain: no universe check applies
    (is (double? (m/horizon-g-sparse {:rates rates :q0 {#{"h0"} 1}
                                      :precedence-fn (constantly [])
                                      :horizon 1
                                      :c-fn-pointwise (fn [_tau] (fn [_o] 1/2))})))))

(deftest wm-06-zero-preference-behavior-preserved
  ;; One spec, one zeroed outcome (#{t0}), two candidates: the one reaching
  ;; the zeroed outcome with positive Q mass scores :infinite (never a
  ;; smoothed finite value); the one that stays away scores a finite double.
  (let [spec (sparse-spec 2 1 #{#{:t0}})
        rates (sparse-rates spec)
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{:t0}}
              :produces #{:t0} :theta 1}
        base {:rates rates :q0 {#{} 1} :horizon 2 :spec spec}]
    (is (= :infinite
           (m/horizon-g-sparse (assoc base :precedence-fn (constantly [fire])))))
    (let [g (m/horizon-g-sparse (assoc base :precedence-fn (constantly [])))]
      (is (and (double? g) (Double/isFinite g) (pos? g))))))
