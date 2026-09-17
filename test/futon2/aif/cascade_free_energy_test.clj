(ns futon2.aif.cascade-free-energy-test
  "Tick 1 F_π over the four candidate cascades (R6's pattern
  interpretations, reproduced verbatim from R4's pattern records as cited in
  vm/tick-001/06-R5.edn), observation o at τ = 1 = q0 ∪
  {`:summary-without-total-repos-observes-cleanly`,
  `:active-repo-ratio-absent-default-is-0`} and zero adjudication rates, so
  A is the identity kernel and p(o|π) is the rollout mass on exactly o.

  These assertions state the REQUIRED values (computed by hand), not current
  behaviour.

  θ = 1 (deterministic rollouts):
  - C3-fix-only [sov]: its only pattern fires from q0 and produces exactly
    the clean token, so the τ = 1 state IS o: p = 1, F = 0.
  - C2-fix-first [ph, test, sov]: ph fires first and produces
    {clean, active-repo-ratio-absent-default-is-0}; q0 already holds
    active-repo-ratio, so the τ = 1 state is again exactly o: F = 0.
  - C1-test-first [test, sov]: test fires first and produces
    test-covers-missing-total-repos, which o does not contain; with identity
    A the observation is impossible under C1: p = 0, F = ##Inf.
  - C0-empty: the rollout stays at q0 forever, and o contains the clean
    token: p = 0, F = ##Inf.

  θ = 0.9:
  - C3-fix-only: the τ = 1 distribution is {o 0.9, q0 0.1}; zero-rate A
    gives likelihood 1 on o and 0 on q0, so p = 0.9 and F = −ln 0.9.
  - C2-fix-first: ph's kernel gives {q0∪{clean,arr} 0.9, q0 0.1} and
    q0∪{clean,arr} is exactly o (arr ∈ q0 already), so p = 0.9 and
    F = −ln 0.9 as well — C2 lands on the observation with probability θ
    just like C3, only via a different pattern.
  - C1-test-first: both branches of test's kernel (q0∪{test-covers} and
    q0) miss the clean token, so p = 0 and F = ##Inf — a contradicted
    prediction at θ < 1 is infinitely surprising, the P8 §2 lesson that
    θ = 1 certainty is what makes contradicted predictions explode.
  - C0-empty: as at θ = 1, F = ##Inf."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-free-energy :as cfe]
            [futon2.aif.cascade-model-manifest :as m]))

(def q0
  (m/observed-belief #{:summary-without-total-repos-throws
                       :active-repo-ratio-absent-default-is-0
                       :coupling-density-reads-same-key-with-default
                       :observe-empty-does-not-throw}))

(def patt-sov
  {:id :aif/structured-observation-vector
   :produces #{:summary-without-total-repos-observes-cleanly}
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-ph
  {:id :aif/placeholder-is-load-bearing
   :produces #{:summary-without-total-repos-observes-cleanly
               :active-repo-ratio-absent-default-is-0}
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:coupling-density-reads-same-key-with-default
                                 :summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-test
  {:id :test-step-covering-missing-total-repos
   :produces #{:test-covers-missing-total-repos}
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:test-covers-missing-total-repos}}]}})

(def universe
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw
    :summary-without-total-repos-observes-cleanly
    :test-covers-missing-total-repos})

(def rates (zipmap universe (repeat {:false-neg 0 :false-pos 0})))

(def observed
  "o at τ = 1: q0 ∪ {clean, active-repo-ratio} (the latter already in q0)."
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw
    :summary-without-total-repos-observes-cleanly})

(defn- with-theta [theta coll] (mapv #(assoc % :theta theta) coll))

(defn- call [theta]
  (cfe/policy-free-energy
   {:q0 q0
    :candidates [{:id :C0-empty :precedence (with-theta theta [])}
                 {:id :C1-test-first :precedence (with-theta theta [patt-test patt-sov])}
                 {:id :C2-fix-first :precedence (with-theta theta [patt-ph patt-test patt-sov])}
                 {:id :C3-fix-only :precedence (with-theta theta [patt-sov])}]
    :tau 1
    :observed-tokens observed
    :rates rates}))

(deftest f-pi-theta-one
  (let [{:keys [f tau params]} (call 1)]
    (is (= 1 tau))
    (is (= 0.0 (:C2-fix-first f)))
    (is (= 0.0 (:C3-fix-only f)))
    (is (= ##Inf (:C0-empty f)))
    (is (= ##Inf (:C1-test-first f)))
    (is (zero? (:complexity-term params)))
    (is (= observed (:observed params)))))

(deftest f-pi-theta-0.9
  (let [{f :f} (call 9/10)
        expected (double (- (Math/log 0.9)))]
    (is (= expected (:C3-fix-only f)))
    (is (= expected (:C2-fix-first f)))
    (is (= ##Inf (:C1-test-first f)))
    (is (= ##Inf (:C0-empty f)))))

(deftest f-pi-refuses-missing-inputs
  (let [r (cfe/policy-free-energy {:q0 q0 :candidates [{:id :C0-empty :precedence []}]
                                   :tau 1 :rates rates})]
    (is (= :missing (:status r)))
    (is (= :missing-free-energy-input (:kind r)))
    (is (= :observed-tokens (:input r)))))
