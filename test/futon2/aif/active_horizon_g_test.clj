(ns futon2.aif.active-horizon-g-test
  "H7a of SPEC-flat-removal-and-cascade-decision (Joe approved 2026-09-17,
  p4ng 462aa79): tests for futon2.aif.active-horizon-g, aligned to mathlib4
  3b19f6225e DarkTower.WarMachine.EpistemicValue.

  (a) The Lean fixture (ckModel/ckC): one unknown binary fact, prior 1/2,
      identity dynamics, indifferent C — fixture_noop_G (G = ln 2),
      fixture_check_G (G = 0), fixture_check_strictly_better.
  (b) Lean activeHorizonEFE_eq_horizonEFE: with no masked facts,
      active-horizon-g equals horizon-g-sparse on tick 1's recorded inputs
      (vm/tick-001/06-R5.edn, T = 3).
  (c) Refusals: a missing unknown prior refuses; exceeding the cap refuses."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.active-horizon-g :as ah]
            [futon2.aif.cascade-model-manifest :as m]))

(defn- within [tol a b]
  (and (number? a) (number? b) (< (Math/abs (- (double a) (double b))) tol)))

;;; ------------------------------------------------------- (a) Lean fixture

(def fixture-fact :f)

(def fixture-universe #{fixture-fact})

(def fixture-rates {fixture-fact {:false-neg 0 :false-pos 0}})

(def fixture-q0 (m/observed-belief #{}))

(def indifferent-c
  "Lean ckC: the indifferent preference, c(o) = 1/2 for every observation."
  (fn [_tau _o] 1/2))

(defn- fixture-input
  [precedence]
  {:q0 fixture-q0
   :precedence-fn (fn [_] precedence)
   :horizon 1
   :c-fn-pointwise indifferent-c
   :universe fixture-universe
   :rates fixture-rates
   :unknown-prior {fixture-fact 1/2}
   :masked #{fixture-fact}
   :cap 1})

(def check-f
  "The check pattern in futon2.aif.check-candidates' shape."
  {:id :check/f :kind :check :fact fixture-fact
   :guard {:unknown #{fixture-fact}} :opens fixture-fact
   :produces #{} :theta 1})

(deftest lean-fixture-noop-g-is-ln2
  (let [g (ah/active-horizon-g (fixture-input []))]
    (is (within 1e-12 g (Math/log 2))
        "fixture_noop_G: doing nothing observes a constant, G = ln 2")))

(deftest lean-fixture-check-g-is-zero
  (let [g (ah/active-horizon-g (fixture-input [check-f]))]
    (is (within 1e-12 g 0.0)
        "fixture_check_G: the check reveals the fact against an indifferent C, G = 0")))

(deftest lean-fixture-check-strictly-better
  (let [g-noop (ah/active-horizon-g (fixture-input []))
        g-check (ah/active-horizon-g (fixture-input [check-f]))]
    (is (< g-check g-noop)
        "fixture_check_strictly_better: by exactly the information the check gains (ln 2)")))

;;; --------------------------------- (b) special case: equals horizon-g-sparse

;; tick 1's recorded inputs (vm/tick-001/06-R5.edn :computed :params), T = 3.
(def tick-universe
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw
    :summary-without-total-repos-observes-cleanly
    :test-covers-missing-total-repos})

(def tick-rates (zipmap tick-universe (repeat {:false-neg 0 :false-pos 0})))

(def tick-q0
  (m/observed-belief #{:summary-without-total-repos-throws
                       :active-repo-ratio-absent-default-is-0
                       :coupling-density-reads-same-key-with-default
                       :observe-empty-does-not-throw}))

(def patt-sov
  {:id :aif/structured-observation-vector
   :produces #{:summary-without-total-repos-observes-cleanly}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-test
  {:id :test-step-covering-missing-total-repos
   :produces #{:test-covers-missing-total-repos}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:test-covers-missing-total-repos}}]}})

(def tick-spec
  {:want #{:summary-without-total-repos-observes-cleanly
           :active-repo-ratio-absent-default-is-0
           :test-covers-missing-total-repos}
   :evidence #{}
   :lam 1
   :mu 1
   :zeroed #{}})

(defn- tick-input
  [precedence]
  {:q0 tick-q0
   :precedence-fn (fn [_] precedence)
   :horizon 3
   :spec tick-spec
   :universe tick-universe
   :rates tick-rates
   :masked #{}
   :cap 0})

(deftest no-masked-facts-equals-horizon-g-sparse
  (doseq [[cid prec expected]
          [[:C1-test-first [patt-test patt-sov] 11.434408130577516]
           [:C2-fix-first [patt-sov patt-test] 11.434408130577516]]]
    (is (within 1e-9 (ah/active-horizon-g (tick-input prec)) expected)
        (str cid ": active-horizon-g with no masked facts equals the recorded horizon-g-sparse G")))
  (is (within 1e-9
              (ah/active-horizon-g (tick-input [patt-test patt-sov]))
              (m/horizon-g-sparse {:rates tick-rates
                                   :q0 tick-q0
                                   :precedence-fn (fn [_] [patt-test patt-sov])
                                   :horizon 3
                                   :spec tick-spec
                                   :universe tick-universe}))
      "C1: direct equality with horizon-g-sparse on identical inputs (activeHorizonEFE_eq_horizonEFE)"))

;;; ------------------------------------------------------- (c) refusals

(deftest missing-unknown-prior-refuses
  (let [r (ah/active-horizon-g (-> (fixture-input [check-f])
                                   (dissoc :unknown-prior)))]
    (is (= :missing (:status r))
        "a missing prior is a typed refusal")
    (is (= :missing-unknown-prior (:kind r))
        "never 1/2 by default")))

(deftest over-cap-refuses
  (let [r (ah/active-horizon-g (-> (fixture-input [check-f])
                                   (assoc :cap 0)))]
    (is (= :missing (:status r)))
    (is (= :unknown-facts-over-cap (:kind r))
        "the spread support grows as 2^k; above the declared cap is a typed refusal")))

(deftest missing-cap-refuses
  (let [r (ah/active-horizon-g (dissoc (fixture-input [check-f]) :cap))]
    (is (= :missing (:status r)))
    (is (= :cap-required (:kind r))
        "the cap is an input with no default")))

;;; --------------------------- (d)(e)(f) ONE acting pattern per step (P10)

(def facts-a-b [:a :b])

(def two-fact-universe #{:a :b})

(def two-fact-rates {:a {:false-neg 0 :false-pos 0}
                     :b {:false-neg 0 :false-pos 0}})

(defn- two-fact-input
  [precedence]
  {:q0 (m/observed-belief #{})
   :precedence-fn (fn [_] precedence)
   :horizon 1
   :c-fn-pointwise indifferent-c
   :universe two-fact-universe
   :rates two-fact-rates
   :unknown-prior {:a 1/2 :b 1/2}
   :masked #{:a :b}
   :cap 2})

(defn- check [fact]
  {:id (keyword "check" (name fact)) :kind :check :fact fact
   :guard {:unknown #{fact}} :opens fact :produces #{} :theta 1})

(deftest d-one-acting-check-per-step
  (let [g-both-listed (ah/active-horizon-g (two-fact-input [(check :a) (check :b)]))
        g-a-only (ah/active-horizon-g (two-fact-input [(check :a)]))]
    (is (within 1e-12 g-both-listed g-a-only)
        "[check-a check-b]: only the first-enabled check acts, so G equals the single [check-a] case")
    (is (not (within 1e-9 g-both-listed (Math/log 2)))
        "…and NOT the case where both channels open (uniform Q over 4 observations against c = 1/2 would score ln 2)")))

(def fix-pattern
  "An ordinary pattern whose guard is enabled in the fixture state."
  {:id :fix :produces #{:done} :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{}
                      :absent #{:done}}]}})

(defn- fix-universe-input
  [precedence]
  {:q0 (m/observed-belief #{})
   :precedence-fn (fn [_] precedence)
   :horizon 1
   :c-fn-pointwise indifferent-c
   ;; :done is a state token outside the observation universe
   :universe #{:a}
   :rates {:a {:false-neg 0 :false-pos 0}}
   :unknown-prior {:a 1/2}
   :masked #{:a}
   :cap 1})

(deftest e-ordinary-pattern-acts-check-does-not-open
  (let [g-fix-check (ah/active-horizon-g (fix-universe-input [fix-pattern (check :a)]))
        g-fix-alone (ah/active-horizon-g (fix-universe-input [fix-pattern]))]
    (is (within 1e-12 g-fix-check g-fix-alone)
        "[fix check-a] with fix's guard enabled: fix acts, the channel is the default, G equals [fix] alone with :a still masked")))

(deftest f-check-first-acts-check-observes
  (let [g-check-fix (ah/active-horizon-g (fix-universe-input [(check :a) fix-pattern]))
        g-check-alone (ah/active-horizon-g (fix-universe-input [(check :a)]))]
    (is (within 1e-12 g-check-fix g-check-alone)
        "[check-a fix] at T = 1: the check acts (transition doing-nothing, a's channel opens), G equals the single check")))
