(ns futon2.vm.tick-001-s05-r4-test
  "Tick 1, step 5, R4 (forward model): the real tick must predict every
  candidate cascade's future state over the common declared horizon T = 3
  (R13, vm/tick-001/04-R13.edn), and that prediction must equal the model's
  rollout for the same candidate.

  Model basis:
  - mathlib4 DarkTower.WarMachine.PolicyRollout.lean (07b094c59b):
    Q(s_{τ+1}|π) = Σ_s B(π τ) Q(s_τ|π) from q0, and Q(o_τ|π) = Σ_s A Q(s_τ|π).
  - futon2.aif.cascade-model-manifest/rollout:298 is the aligned Clojure
    rollout (cites that Lean declaration); cascade-kernel/first-enabled apply
    the P10 continuing guards.
  - SPEC-cascade-policy-semantics §2 / PolicyHorizon.lean: every candidate is
    predicted at the SAME declared T, never at its own pattern count.

  Real machine: futon2.aif.forward-model (fm) is the tick's forward model
  (war_machine.clj uses fm/can-execute? ~:1450, ~:6540; efe composes
  fm/predict and fm/predict-multi-horizon). Since the tick-1 fix wave the
  real forward model accepts cascade candidates ({:kind
  :cascade-candidate …}) and predicts token-state trajectories through the
  aligned manifest kernel. The remaining failing assertion is the :R4 route
  tag in war_machine.clj, which stays failing until the serial wiring wave."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.policy-depth :as policy-depth]))

(def T 3)

(def q0
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

(def patt-ph
  {:id :aif/placeholder-is-load-bearing
   :produces #{:summary-without-total-repos-observes-cleanly
               :active-repo-ratio-absent-default-is-0}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:coupling-density-reads-same-key-with-default
                                 :summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-test
  {:id :test-step-covering-missing-total-repos
   :produces #{:test-covers-missing-total-repos}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:test-covers-missing-total-repos}}]}})

(def cascades
  {:C0-empty []
   :C1-test-first [patt-test patt-sov]
   :C2-fix-first [patt-ph patt-test patt-sov]
   :C3-fix-only [patt-sov]})

(def want
  #{:summary-without-total-repos-observes-cleanly
    :active-repo-ratio-absent-default-is-0
    :test-covers-missing-total-repos})

(defn- roll
  "Model rollout for one cascade over the common T: {tau dist}."
  [prec]
  (into (sorted-map)
        (map (fn [tau] [tau (m/rollout (fn [_] prec) q0 tau)]))
        (range 1 (inc T))))

(defn- want-holding
  "Which want tokens hold in the (point-mass) predicted state at tau."
  [q]
  (when (and (= 1 (count q)) (= 1 (val (first q))))
    (set/intersection want (key (first q)))))

;; The candidates as actions for the real forward model: cascade candidates
;; per the R4 requirement ({:kind :cascade-candidate :id … :precedence
;; [patterns…]}), patterns carrying guard, produces and theta as interpreted
;; by R6 (03-R6.edn).
(defn- cascade-action [cid]
  {:kind :cascade-candidate
   :id cid
   :precedence (cid cascades)})

(defn- call-real
  "Call a real fm function, refusing symbolically instead of erroring."
  [f & args]
  (try (apply f args)
       (catch clojure.lang.ExceptionInfo e
         {:status :refused :message (ex-message e)})))

(defn- war-machine-source
  []
  (or (some-> (io/resource "futon2/report/war_machine.clj") slurp)
      (try (slurp "scripts/futon2/report/war_machine.clj")
           (catch Exception _ nil))))

(def cascade-state {:cascade-belief q0})

(deftest s05-r4
  ;; --- Model side (the aligned runtime function; passes, it is the
  ;; --- baseline the real tick must equal).
  (is (= 1 (val (first q0)))
      "q0 is the observed point mass on R1's token state")
  (let [r (roll (:C1-test-first cascades))]
    (is (= {#{:active-repo-ratio-absent-default-is-0
                :coupling-density-reads-same-key-with-default
                :observe-empty-does-not-throw
                :summary-without-total-repos-throws
               :test-covers-missing-total-repos} 1}
           (get r 1))
        "C1 tau 1: test step fires (theta 1), point mass")
    (is (= true (set/subset? want (want-holding (get r 2))))
        "C1 want complete at tau 2, model agrees with R13"))
  (let [r (roll (:C3-fix-only cascades))]
    (is (= #{:active-repo-ratio-absent-default-is-0
             :summary-without-total-repos-observes-cleanly}
           (want-holding (get r 3)))
        "C3 at T=3 holds only clean-observe (plus the already-held default token), matches R13"))
  ;; --- Real side: requirement is prediction per candidate over the common T.
  (let [preds (into {}
                    (map (fn [cid]
                           [cid (call-real fm/predict cascade-state (cascade-action cid))]))
                    (keys cascades))]
    (is (every? (comp #(and (map? %) (not (contains? % :status))) val) preds)
        "the real forward model accepts every cascade candidate of this tick and predicts its next state"))
  (let [c1 (call-real fm/predict-multi-horizon cascade-state
                      (cascade-action :C1-test-first) T)
        tau2-state (some-> (get-in c1 [:trajectory 1 :next-token-state])
                           first key)]
    (is (and (map? c1) (not (contains? c1 :status))
             (contains? tau2-state :summary-without-total-repos-observes-cleanly))
        "the real prediction expresses want-token outcomes (C1's tau-2 predicted token state carries the clean-observe want token), not only channel means"))
  ;; Equality with the model's rollout: every tau, and the terminal state.
  (let [model (roll (:C1-test-first cascades))
        real (call-real fm/predict-multi-horizon cascade-state
                        (cascade-action :C1-test-first) T)]
    (is (and (map? real) (not (contains? real :status))
             (= (mapv (fn [tau] (get model tau)) (range 1 (inc T)))
                (mapv :next-token-state (:trajectory real)))
             (= (get model T) (get-in real [:final-state :cascade-belief])))
        "the real forward model's trajectory at every tau <= T equals the model's rollout for the same candidate"))
  ;; Common T, through real calls: every candidate predicted at the same T=3.
  (let [horizons (into {}
                       (map (fn [cid]
                              [cid (call-real fm/predict-multi-horizon cascade-state
                                              (cascade-action cid) T)]))
                       (keys cascades))]
    (is (and (seq horizons)
             (every? (comp #(and (map? %)
                                 (not (contains? % :status))
                                 (= T (:horizon-steps %))
                                 (= T (count (:trajectory %))))
                           val)
                     horizons))
        "every candidate of the family is predicted at the SAME declared T = 3"))
  ;; The tick declares the common T as a typed value the forward model reads.
  (is (= {:anticipation 3 :cascade-rollout 3}
         (policy-depth/configured {:policy-depth {:anticipation 3 :cascade-rollout 3}}))
      "the declared common horizon is readable as a typed value (T = 3, family-level)")
  ;; R4 has no route tag in the real tick (protocol: nodes without a
  ;; route-tagged call fail until wired).
  (let [src (war-machine-source)]
    (is (some? src) "war_machine.clj is readable by the test")
    (is (boolean (and src (re-find #":R4\s+\"futon2\.aif\.forward-model" src)))
        "war_machine.clj routes the forward-model call with an :R4 route tag naming futon2.aif.forward-model")))
