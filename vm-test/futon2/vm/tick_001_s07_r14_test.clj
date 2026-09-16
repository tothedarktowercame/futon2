(ns futon2.vm.tick-001-s07-r14-test
  "Tick 1, step 7, R14 (commitment temperature / selection): the real tick's
  selection must choose by the precision-weighted posterior over this tick's
  cascades and the Bayes action marginal.

  Model basis:
  - mathlib4 DarkTower.WarMachine.PolicyPrecision (commit ba1b2c42dd):
    PolicyTemperature β > 0, γ = 1/β, precisionWeightedPosterior
    σ(ln E − F − γG); PolicySelection.selectionPosterior (a434947c63);
    ActionMarginal.IsBayesAction — the Bayes action maximises posterior mass
    summed over the policies that choose it.
  - futon2.aif.cascade-selection/selection-posterior (:51) and /bayes-choice
    (:104) are the aligned Clojure functions; the declared tie-break rule is
    :action-name-ascending (bayes-choice-tie-rule).
  - Inputs from the tick: G from R5 (vm/tick-001/06-R5.edn), actions from R6
    (03-R6.edn): each cascade's first acting pattern; C0 is an explicit no-op.
    habit = 1 and F = 0 for every candidate (no habit prior / no F_π exists on
    this tick — recorded as typed missing inputs in 07-R14.edn, not defaults
    of the real machine). β = 1 is the declared default (no approved source
    fixes β); the posterior is also reported at β ∈ {1/4, 4}.

  Real machine: the tick's selection route is
  futon2.aif.policy/select-action (route :R6, war_machine.clj:6570) feeding
  futon2.aif.controller-authority/authorize (route :R14, :6571). The model
  requires the chosen action to be the Bayes action of the posterior and the
  recorded posterior to be the σ(ln E − F − G/β) one at β = 1. These are
  requirement tests: failures are the node's open requirements."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-selection :as cs]
            [futon2.aif.policy :as policy]
            [futon2.aif.controller-authority :as controller-authority]
            [futon2.report.war-machine :as wm]))

;; G at T = 3 from R5 (06-R5.edn :computed :G), reproduced by claude-4.
(def G-at-T
  {:C0-empty 13.101074797244184
   :C1-test-first 11.434408130577516
   :C2-fix-first 11.434408130577516
   :C3-fix-only 12.101074797244184})

;; habit 1 (no habit prior on this tick), F 0 (no F_π on this tick).
(def candidates
  (mapv (fn [[id g]] {:id id :habit 1 :f 0 :g g}) G-at-T))

;; Each cascade's action at this step is its first acting pattern; C0 is the
;; explicit no-op (dispatch instruction, step 7).
(def action-of
  {:C1-test-first :test-step-covering-missing-total-repos
   :C2-fix-first :aif/placeholder-is-load-bearing
   :C3-fix-only :aif/structured-observation-vector
   :C0-empty :no-op})

;; Model values, live JVM run 2026-09-17 (/tmp/r14-sel.clj).
(def posterior-beta-1
  {:C0-empty 0.06989457556132114
   :C1-test-first 0.37005613489124095
   :C2-fix-first 0.37005613489124095
   :C3-fix-only 0.18999315465619696})

(def model-choice
  {:action :aif/placeholder-is-load-bearing
   :cascade :C2-fix-first
   :mass 0.37005613489124095
   :tie-break :action-name-ascending})

(defn- within-1e-9
  [a b]
  (and (number? a) (number? b) (< (Math/abs (- (double a) (double b))) 1e-9)))

(defn- call-real
  "Call a real selection/authorization function, refusing symbolically
  instead of erroring."
  [f & args]
  (try (apply f args)
       (catch clojure.lang.ExceptionInfo e
         {:status :refused :message (ex-message e)
          :reason (:reason (ex-data e))})))

(defn- cascade-action
  [cid]
  (if (= cid :C0-empty)
    {:type :no-op :target :wm-tick-001-observation-crash}
    {:type :cascade-rollout
     :target :wm-tick-001-observation-crash
     :precedence (case cid
                   :C1-test-first [:test-step-covering-missing-total-repos
                                   :aif/structured-observation-vector]
                   :C2-fix-first [:aif/placeholder-is-load-bearing
                                  :test-step-covering-missing-total-repos
                                  :aif/structured-observation-vector]
                   :C3-fix-only [:aif/structured-observation-vector])}))

;; ranked-actions in the shape efe/rank-actions emits and select-action
;; consumes, carrying the model's G as :controller-score, in G order.
(def ranked
  (mapv (fn [cid]
          {:action (cascade-action cid)
           :controller-score (get G-at-T cid)
           :rank 1})
        [:C1-test-first :C2-fix-first :C3-fix-only :C0-empty]))

(deftest s07-r14
  ;; --- Model side (baseline the real tick must equal; passes).
  (is (= posterior-beta-1 (cs/selection-posterior {:beta 1 :candidates candidates}))
      "model posterior at the declared default β = 1: σ(ln 1 − 0 − G/1)")
  (is (let [p (cs/selection-posterior {:beta 1 :candidates candidates})]
        (and (within-1e-9 (:C1-test-first p) (:C2-fix-first p))
             (> (:C1-test-first p) (:C3-fix-only p) (:C0-empty p))))
      "the C1/C2 G tie is a posterior tie, not something to break with judgement")
  (is (let [c1 (cs/bayes-choice (cs/selection-posterior {:beta 1 :candidates candidates}) action-of)
            c¼ (cs/bayes-choice (cs/selection-posterior {:beta 0.25 :candidates candidates}) action-of)
            c4 (cs/bayes-choice (cs/selection-posterior {:beta 4 :candidates candidates}) action-of)]
        (and (= (:action c1) (:action c¼) (:action c4) (:action model-choice))
             (within-1e-9 (:mass c1) (:mass model-choice))
             (= (:tie-break-rule c1) (:tie-break model-choice))))
      "the Bayes action is :aif/placeholder-is-load-bearing at every β, decided by the declared :action-name-ascending tie-break")
  (is (let [p¼ (:C1-test-first (cs/selection-posterior {:beta 0.25 :candidates candidates}))
            p1 (:C1-test-first (cs/selection-posterior {:beta 1 :candidates candidates}))
            p4 (:C1-test-first (cs/selection-posterior {:beta 4 :candidates candidates}))]
        (> p¼ p1 p4))
      "sharper precision (smaller β) concentrates mass on the tied-lowest-G cascades")
  ;; --- Real side: the requirement on this tick's candidates.
  ;; Re-pointed (tick-1 fix, claude-4 2026-09-17) from policy/select-action to
  ;; the real cascade entry point policy/select-action-cascades built for it:
  ;; select-action is the single-action controller-head path (it can never
  ;; choose by the action marginal) and select-action-cascades is the cascade
  ;; path on the same route seam. Required values are unchanged.
  (let [decision (call-real policy/select-action-cascades ranked {:beta 1})]
    (is (and (map? decision)
             (not= :refused (:status decision))
             (= (:action model-choice)
                (-> decision :action :precedence first)))
        "the real R6/R14 selection chooses the model's Bayes action (:aif/placeholder-is-load-bearing, cascade C2)"))
  (let [decision (call-real policy/select-action-cascades ranked {:beta 1})
        weights (:softmax-weights decision)]
    (is (and (map? weights)
             (within-1e-9 (get weights (:action model-choice)) (:mass model-choice))
             (within-1e-9 (get weights :test-step-covering-missing-total-repos)
                          (:C1-test-first posterior-beta-1))
             (within-1e-9 (get weights :aif/structured-observation-vector)
                          (:C3-fix-only posterior-beta-1))
             (within-1e-9 (get weights :no-op) (:C0-empty posterior-beta-1))
             (= :declared (get-in decision [:beta :status])))
        "the real selection's recorded posterior is σ(ln E − F − G/β) at β = 1 (declared, never silently defaulted), giving the chosen action mass 0.37005613489124095"))
  (let [decision (call-real policy/select-action-cascades ranked {:beta 1})
        authorized (call-real controller-authority/authorize decision ranked)]
    (is (and (map? authorized)
             (true? (get-in authorized [:actuation :authorized?]))
             (= :machine-authorized-bounded-autonomy
                (get-in authorized [:actuation :status]))
             (not= :refused (:status authorized)))
        "the real R14 authorize grants bounded authorization for the selected cascade decision, reading the recorded selection law"))
  ;; β stays explicit: no β, no silent default — the typed refusal fires.
  (let [decision (call-real policy/select-action-cascades ranked {})]
    (is (= :refused (:status decision))
        "β is never defaulted: without a declared β the cascade selection refuses (:invalid-temperature)"))

  ;; R10 wiring (s11): the tick's cascade lane routes THIS node — the real
  ;; cascade selection at a declared β — under :R14 on this tick's problem.
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
               :horizon-steps 3
               :cascade-spec {:want #{:summary-without-total-repos-observes-cleanly
                                      :active-repo-ratio-absent-default-is-0
                                      :test-covers-missing-total-repos}}
               :beta 1})]
    (is (boolean (some #(and (= :R14 (:node %))
                             (= "futon2.aif.policy/select-action-cascades" (:via %)))
                       (:route lane)))
        "the cascade lane's route contains :R14 with futon2.aif.policy/select-action-cascades (behavioural: the lane selected at the declared β through the real cascade selection)")))
