(ns futon2.vm.tick-001-s11-tick-test
  "Virtual War Machine tick 1, step 11 (R10 wiring): the real tick runs the
  model's cascade node sequence when its input carries a cascade problem.

  Real machine (after the R10 wiring wave): war_machine.clj's public
  `cascade-lane` runs R1 → R6 → R13 → R4 → R5 → R14 → R16 → R9 through the
  REAL node functions (cascade-model-manifest/observed-belief,
  cascade-policy/candidate-space, policy-depth/configured,
  forward-model/predict-multi-horizon, efe/rank-actions,
  policy/select-action-cascades → controller-authority/authorize,
  receipt-construction/acting-order), and `judge` calls it when `scan-data`
  carries :cascade-problem, attaching the result and appending its route
  entries to :wm/route. Without :cascade-problem judge is unchanged.

  Inputs are tick 1's, from the p4ng step records 01..07
  (p4ng/wm-walkthroughs/build-loop/vm/tick-001/): R2's facts, R1's
  want-tokens, R6's interpretations/repository, R13's T = 3, R5's cascade
  spec, R14's declared β = 1. Required values are the recorded,
  R9-reproduced ones (10-R9-fix-wave-1.edn)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.report.war-machine :as wm]))

;; --- tick 1's problem, from the p4ng records 01-R2 … 07-R14 ---------------

(def facts
  "R2's adjudicated facts (01-R2.edn :computed :facts); the two false/
  unadjudicated tokens are R1's closed-world absences."
  {:summary-without-total-repos-throws true
   :active-repo-ratio-absent-default-is-0 true
   :coupling-density-reads-same-key-with-default true
   :observe-empty-does-not-throw true
   :test-covers-missing-total-repos false
   :summary-without-total-repos-observes-cleanly :unknown})

(def want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(def interpretations
  "R6's on-the-fly readings (03-R6.edn :computed :interpretations)."
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
    :produces #{:test-covers-missing-total-repos}}})

(def repository
  {:patterns (set (keys interpretations))
   :stands-on #{[:aif/placeholder-is-load-bearing
                 :aif/structured-observation-vector]}})

(def problem
  {:facts facts
   :want want
   :interpretations interpretations
   :repository repository
   :precedences [[:test-step-covering-missing-total-repos
                  :aif/structured-observation-vector]
                 [:aif/placeholder-is-load-bearing
                  :test-step-covering-missing-total-repos
                  :aif/structured-observation-vector]
                 [:aif/structured-observation-vector]]
   :horizon-steps 3
   :cascade-spec {:want (set want) :evidence #{} :lam 1 :mu 1 :zeroed #{}}
   :beta 1})

;; tick-level judge sources (H5b): targets and per-target maps are supplied
;; at the assertion below; the horizon and β come from the problem.
(def tick-1-sources-from-problem
  {:horizon-steps (:horizon-steps problem)
   :beta-by-context {:tick-1 {:beta (:beta problem)}}
   :context-of (fn [_] :tick-1)})

;; candidate-space ids C0..C3 by position: C1 test-first, C2 fix-first,
;; C3 fix-only (03-R6.edn :computed :cascades).
(defn- entry-of
  [lane cid]
  (some (fn [e] (when (= (:cascade-id e) cid) e)) (:ranked lane)))

(defn- g-of
  [lane cid]
  (:G-efe (entry-of lane cid)))

(defn- within-1e-9
  [a b]
  (and (number? a) (number? b) (< (Math/abs (- (double a) (double b))) 1e-9)))

(deftest s11-tick
  ;; 1. The lane runs the node sequence in the model's order, one route tag
  ;;    per node, each naming the real function.
  (let [lane (wm/cascade-lane problem)
        route (:route lane)]
    (is (= [:R1 :R6 :R13 :R4 :R5 :R14 :R16 :R9] (mapv :node route))
        "the cascade lane routes R1 → R6 → R13 → R4 → R5 → R14 → R16 → R9, in order")
    (is (= ["futon2.aif.cascade-model-manifest/observed-belief"
            "futon2.aif.cascade-policy/candidate-space"
            "futon2.aif.policy-depth/configured"
            "futon2.aif.forward-model/predict-multi-horizon"
            "futon2.aif.efe/rank-actions"
            "futon2.aif.policy/select-action-cascades"
            "futon2.aif.receipt-construction/acting-order"
            "futon2.report.war-machine/cascade-lane"]
           (mapv :via route))
        "each route tag names the real node function that ran")
    ;; 2. The model's required values on this tick's input.
    (is (and (within-1e-9 (g-of lane :C1) 11.434408130577516)
             (within-1e-9 (g-of lane :C2) 11.434408130577516)
             (within-1e-9 (g-of lane :C3) 12.101074797244184)
             (within-1e-9 (g-of lane :C0) 13.101074797244184))
        "G at the common T = 3: C1 = C2 = 11.434408130577516, C3 = 12.101074797244184, C0 = 13.101074797244184")
    (is (contains? (entry-of lane :C1) :g-tie)
        "the C1/C2 G tie is recorded, never broken at R5")
    (is (= :aif/placeholder-is-load-bearing
           (-> lane :decision :action :precedence first :id))
        "the chosen action is :aif/placeholder-is-load-bearing (the Bayes action at the declared β)")
    (is (and (within-1e-9 (get-in lane [:decision :softmax-weights
                                        :aif/placeholder-is-load-bearing])
                             0.37005613489124095)
             (= :declared (get-in lane [:decision :beta :status]))
             (= 1 (get-in lane [:decision :beta :value])))
        "the chosen action carries mass 0.37005613489124095 at the declared β = 1")
    (is (true? (get-in lane [:authorization :actuation :authorized?]))
        "controller-authority/authorize grants bounded authorization for the decision")
    (is (= :aif/placeholder-is-load-bearing
           (first (get-in lane [:enactment-plan :acting-order])))
        "the enactment plan (R16, acting-order on the facts) acts the chosen pattern first")
    (is (= :independent-check-required (get-in lane [:certification :status]))
        "R9: the enactment claim is marked :independent-check-required — the tick never certifies itself")
    (is (contains? (get lane :certification) :enactor)
        "the enactor is recorded with the claim")
    ;; 3. Every candidate was predicted at the same T (R4), not just scored.
    (is (and (= 4 (count (:predictions lane)))
             (every? #(= 3 (count (get-in % [:prediction :trajectory])))
                     (:predictions lane)))
        "R4 predicted all four candidates at the same declared T = 3"))
  ;; 4. The real judge now runs the cascade decision from :cascade-sources
  ;;    (H5b; the single :cascade-problem injection is gone with the flat
  ;;    path). The tick-1 sources key a REAL substrate target.
  (let [target (first (cp/substrate-targets))
        ;; every token needs a checkable locator (WM-04, futon2 705adb39);
        ;; fixture C3 locators, never observed here
        sources (locfix/locate-all
                 (assoc tick-1-sources-from-problem
                       :universes {target (:facts problem)}
                       :interpretations
                       {target {:patterns (:interpretations problem)
                                :receipts
                                {:aif/structured-observation-vector {:receipt "S"}
                                 :aif/placeholder-is-load-bearing {:receipt "P"}
                                 :test-step-covering-missing-total-repos {:receipt "T"}}}}
                       :wants {target (:want problem)}
                       :candidates
                       {target (mapv (fn [p] {:precedence p
                                              :construction-receipt
                                              {:kind :construction-receipt :moves []}})
                                     (:precedences problem))}))
        j1 (wm/judge {} {:cascade-sources sources})]
    (is (= :aif/placeholder-is-load-bearing
           (-> j1 :decision :action :precedence first :id))
        "judge with tick-1 sources decides :aif/placeholder-is-load-bearing for that target (gated)")
    (is (within-1e-9 (get-in j1 [:decision :chosen-action-mass])
                     0.37005613489124095)
        "the judge's chosen action carries mass 0.37005613489124095 at the declared β = 1")
    (is (= 1 (count (get-in j1 [:cascade-problems :problems])))
        "the target's problem is attached under :cascade-problems")
    (is (= [[:R1 :R6 :R13 :R4 :R5 :R14 :R16 :R9]]
           (mapv #(mapv :node (:route %)) (:cascade-lanes j1)))
        "the judgement records the target's full cascade-lane route under :cascade-lanes")
  ;; 5. judge WITHOUT sources abstains honestly: every substrate target is
  ;;    refused, no removed flat key is present, and no problem is assembled.
  (let [j0 (wm/judge {})]
    (is (= [:R2 :R7 :R3 :R8] (mapv :node (:wm/route j0)))
        "the substrate route remains; the flat R5/R6/R14 tags are gone with the flat path")
    (is (= :abstained (get-in j0 [:decision :status]))
        "no sources ⇒ the gated abstention")
    (is (pos? (count (get-in j0 [:decision :refusals])))
        "the abstention lists per-target refusals")
    (is (= [] (get-in j0 [:cascade-problems :problems]))
        "nothing is assembled without sources")
    (is (= :none-supplied (:cascade-sources j0))
        "the absent sources are recorded, never invented")
    (is (nil? (some #(contains? j0 %)
                    [:ranked-actions :admissible-actions :default-mode-events
                     :operator-actions :policy-support-exclusions
                     :cascade-policies :cascade-lane]))
        "no removed flat key is present on the judgement")))
  ;; 6. Typed refusals stop the lane with the route so far; nothing defaults.
  (let [no-beta (wm/cascade-lane (dissoc problem :beta))]
    (is (and (= :R14 (:stopped-at no-beta))
             (contains? no-beta :refusal))
        "a missing β is a typed refusal at R14 — never defaulted"))
  (let [no-T (wm/cascade-lane (dissoc problem :horizon-steps))]
    (is (and (= :R13 (:stopped-at no-T))
             (contains? no-T :refusal)
             (= [:R1 :R6 :R13] (mapv :node (:route no-T))))
        "a missing horizon is a typed refusal at R13, with the route so far")))
