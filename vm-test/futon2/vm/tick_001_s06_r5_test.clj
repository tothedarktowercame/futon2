(ns futon2.vm.tick-001-s06-r5-test
  "Tick 1, step 6, R5 (expected free energy): the real tick's R5 scoring
  (futon2.aif.efe/rank-actions, route :R5 at war_machine.clj:6454) must score
  this tick's candidate cascades at the common declared horizon T = 3
  (R13, vm/tick-001/04-R13.edn), its G per candidate must equal the model's
  horizon-g-sparse value, and its ranking must follow G.

  Model basis:
  - mathlib4 DarkTower.WarMachine.PolicyHorizon.horizonEFE + OutcomeRiskKL
    (branch darktower): G(π) at common T is Σ_τ risk_τ + ambiguity_τ; at zero
    adjudication rates A is the identity kernel so ambiguity ≡ 0 and risk is
    the exact KL of the predicted token state against C.
  - futon2.aif.cascade-model-manifest/horizon-g-sparse (:566) is the aligned
    Clojure function (cites that Lean declaration).
  - R6/R4 supplied the four candidates and their predicted rollouts; C1 and
    C2 have identical predicted outcomes at every τ, so the model requires
    they tie.

  Real machine: rank-actions scores (state, action) via compute-efe — channel
  means against the live C-vector. Since the tick-1 R5 fix wave, a list of
  cascade candidates ({:kind :cascade-candidate …}) is scored by the cascade
  path: G by cascade-model-manifest/horizon-g-sparse at the declared common
  horizon over one common universe, equal G a recorded tie. These
  assertions are the requirement on that path, live on this tick's input."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm]))

(def T 3)

(def q0
  (m/observed-belief #{:summary-without-total-repos-throws
                       :active-repo-ratio-absent-default-is-0
                       :coupling-density-reads-same-key-with-default
                       :observe-empty-does-not-throw}))

;; One common universe: q0 ∪ want ∪ every token named by any pattern guard.
(def universe
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw
    :summary-without-total-repos-observes-cleanly
    :test-covers-missing-total-repos})

(def rates (zipmap universe (repeat {:false-neg 0 :false-pos 0})))

;; Patterns exactly as R4 built them from 03-R6's interpretations.
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

(def spec {:want #{:summary-without-total-repos-observes-cleanly
                   :active-repo-ratio-absent-default-is-0
                   :test-covers-missing-total-repos}
           :evidence #{}
           :lam 1
           :mu 1
           :zeroed #{}})

(defn- g
  "Model G for one cascade at one horizon (aligned runtime function)."
  [cid horizon]
  (m/horizon-g-sparse {:rates rates
                       :q0 q0
                       :precedence-fn (fn [_] (get cascades cid))
                       :horizon horizon
                       :spec spec
                       :universe universe}))

;; G at the common T = 3, live-run verified 2026-09-17.
(def G-at-T
  {:C0-empty 13.101074797244184
   :C1-test-first 11.434408130577516
   :C2-fix-first 11.434408130577516
   :C3-fix-only 12.101074797244184})

(defn- cascade-action
  [cid]
  {:kind :cascade-candidate
   :id cid
   :precedence (get cascades cid)})

(def cascade-state {:cascade-belief q0})

(def real-opts
  {:horizon-steps T
   :cascade-spec {:want (:want spec)
                  :evidence #{}
                  :lam 1
                  :mu 1
                  :zeroed #{}}})

(defn- call-real
  "Call a real efe function, refusing symbolically instead of erroring."
  [f & args]
  (try (apply f args)
       (catch clojure.lang.ExceptionInfo e
         {:status :refused :message (ex-message e)})))

(defn- within-1e-9
  [a b]
  (and (number? a) (number? b) (< (Math/abs (- (double a) (double b))) 1e-9)))

(deftest s06-r5
  ;; --- Model side (baseline the real tick must equal; passes).
  (is (every? #(within-1e-9 (g % T) (G-at-T %)) (keys cascades))
      "model G at the common T = 3 matches the recorded live-run values")
  (is (within-1e-9 (g :C1-test-first T) (g :C2-fix-first T))
      "C1 and C2 have the same predicted outcomes at every tau, so G ties")
  (is (< (g :C1-test-first T) (g :C3-fix-only T) (g :C0-empty T))
      "ordering: C1 = C2 < C3 < C0 (fix-and-test beats fix-only beats nothing)")
  (is (and (pos? (- (g :C0-empty 1) 0))
           (within-1e-9 (- (g :C0-empty T) (g :C0-empty 2))
                        (- (g :C0-empty 2) (g :C0-empty 1))))
      "per-step risk on the identity stall is constant (ambiguity 0 at zero rates)")
  ;; --- Real side: the requirement on this tick's candidates. One real call
  ;; --- over the whole list (the universe must be common to all candidates).
  (let [ranked (call-real efe/rank-actions
                          cascade-state
                          (mapv cascade-action (keys cascades))
                          real-opts)]
    (is (and (vector? ranked) (= 4 (count ranked)))
        "the real R5 scorer accepts every cascade candidate of this tick and returns one ranked entry per candidate"))
  (let [entries (into {}
                      (map (fn [{:keys [cascade-id] :as e}] [cascade-id e]))
                      (try (efe/rank-actions cascade-state
                                             (mapv cascade-action (keys cascades))
                                             real-opts)
                           (catch clojure.lang.ExceptionInfo _e
                             [])))]
    (is (every? (fn [pair] (within-1e-9 (:G-efe (val pair)) (G-at-T (key pair))))
                entries)
        "the real scorer's G per candidate equals the model's horizon-g-sparse value at T = 3"))
  (let [ranked (try
                 (efe/rank-actions cascade-state
                                   (mapv cascade-action (keys cascades))
                                   real-opts)
                 (catch clojure.lang.ExceptionInfo e
                   {:status :refused :message (ex-message e)}))
        order (mapv :cascade-id (sort-by :rank ranked))]
    (is (= [:C1-test-first :C2-fix-first :C3-fix-only :C0-empty]
           order)
        "the real ranking follows G: C1 and C2 tie ahead of C3 ahead of C0"))
  ;; Equal G is a recorded tie, not broken by an undeclared rule: C1 and C2
  ;; share one :rank and each carries :g-tie naming both.
  (let [ranked (try
                 (efe/rank-actions cascade-state
                                   (mapv cascade-action (keys cascades))
                                   real-opts)
                 (catch clojure.lang.ExceptionInfo _e
                   []))
        by-id (into {} (map (fn [{:keys [cascade-id] :as e}] [cascade-id e]) ranked))
        c1 (get by-id :C1-test-first)
        c2 (get by-id :C2-fix-first)]
    (is (and (within-1e-9 (:G-efe c1) 11.434408130577516)
             (within-1e-9 (:G-efe (get by-id :C0-empty)) 13.101074797244184)
             (< (:G-efe c1) (:G-efe (get by-id :C0-empty)))
             (= (:rank c1) (:rank c2))
             (contains? c1 :g-tie)
             (set (:g-tie c1))
             (contains? (set (:g-tie c1)) :C2-fix-first))
        "the real scorer distinguishes best from doing nothing at T = 3 and records the C1/C2 G tie explicitly"))

  ;; R10 wiring (s11): the tick's cascade lane routes THIS node — the real
  ;; rank-actions — under :R5 on this tick's problem.
  (let [interpretations
        {:aif/structured-observation-vector
         {:guard {:needs #{:summary-without-total-repos-throws}
                  :forbids #{:summary-without-total-repos-observes-cleanly}}
          :produces #{:summary-without-total-repos-observes-cleanly}}
         :aif/placeholder-is-load-bearing
         {:guard {:needs #{:coupling-density-reads-same-key-with-default
                           :summary-without-total-repos-throws}
                  :forbids #{:summary-without-total-repos-observes-cleanly}}
          :produces #{:summary-without-total-repos-observes-cleanly
                      :active-repo-ratio-absent-default-is-0}}
         :test-step-covering-missing-total-repos
         {:guard {:needs #{:summary-without-total-repos-throws}
                  :forbids #{:test-covers-missing-total-repos}}
          :produces #{:test-covers-missing-total-repos}}}
        lane (wm/cascade-lane
              {:facts {:summary-without-total-repos-throws true
                       :active-repo-ratio-absent-default-is-0 true
                       :coupling-density-reads-same-key-with-default true
                       :observe-empty-does-not-throw true
                       :test-covers-missing-total-repos false}
               :want [:summary-without-total-repos-observes-cleanly
                      :active-repo-ratio-absent-default-is-0
                      :test-covers-missing-total-repos]
               :interpretations interpretations
               :repository {:patterns (set (keys interpretations)) :stands-on #{}}
               :precedences [[:test-step-covering-missing-total-repos
                              :aif/structured-observation-vector]
                             [:aif/placeholder-is-load-bearing
                              :test-step-covering-missing-total-repos
                              :aif/structured-observation-vector]
                             [:aif/structured-observation-vector]]
               :horizon-steps T
               :cascade-spec {:want (:want spec)}
               :beta 1})]
    (is (boolean (some #(and (= :R5 (:node %))
                             (= "futon2.aif.efe/rank-actions" (:via %)))
                       (:route lane)))
        "the cascade lane's route contains :R5 with futon2.aif.efe/rank-actions (behavioural: the lane scored this tick's candidates through the real scorer)")))
