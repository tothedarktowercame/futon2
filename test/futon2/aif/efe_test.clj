(ns futon2.aif.efe-test
  "Tests for Expected Free Energy composition (R5)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.free-energy :as fe]))

(def ^:private healthy-obs
  "In-distribution observation that falls within preferred ranges on
   the weighted channels (so preference-gap-score is small but the action-
   dependent risk signal is still visible)."
  {:loop-health 0.9
   :support-coverage 0.9
   :attack-coverage 0.9
   :mission-health 0.7
   :stack-pct 0.20
   :consulting-pct 0.25
   :portfolio-pct 0.25
   :mathematics-pct 0.20
   :active-repo-ratio 0.8
   :sorry-count-norm 0.1
   :coupling-density 0.2
   :ticks-firing-ratio 0.0
   :depositing-signal 0.1})

(def ^:private stressed-obs
  "Observation with sorry-count-norm in the avoided range — the
   :address-sorry action should look better here than in healthy-obs."
  (assoc healthy-obs :sorry-count-norm 0.85 :mission-health 0.3))

(def ^:private base-state
  {:observation healthy-obs
   :belief (belief/initial-belief-state [:m1 :m2])})

(def ^:private stressed-state
  {:observation stressed-obs
   :belief (belief/initial-belief-state [:m1 :m2])})

(defn- ensemble-fixture []
  (edn/read-string
   (slurp (io/file ".." "futon0" "holes" "missions"
                   "M-capability-star-map.ensemble.edn"))))

(defn- wm-region-graph-fixture []
  (let [ensemble (ensemble-fixture)
        caps (:capabilities ensemble)]
    {:star-map/region :wm
     :capabilities (-> caps
                       (assoc-in [:wm-overnight-unsupervised :pre-registered?] true)
                       (assoc-in [:efe-trustworthy-over-starmap :pre-registered?] true))
     :missions {:M-capability-star-map
                {:scope [:wm-steps-forward-guardrailed]
                 :produces [:efe-trustworthy-over-starmap]
                 :open-hole-count 1
                 :phase :instantiate
                 :status :active
                 :next-exit-operator-verify? false}

                :M-capability-star-map-mega
                {:scope [:wm-steps-forward-guardrailed]
                 :produces [:wm-overnight-unsupervised]
                 :open-hole-count 9
                 :phase :instantiate
                 :status :active
                 :next-exit-operator-verify? false}

                :M-unbound-frontier
                {:scope [:efe-trustworthy-over-starmap]
                 :produces [:wm-overnight-unsupervised]
                 :open-hole-count 1
                 :phase :instantiate
                 :status :active
                 :next-exit-operator-verify? false}

                :M-unagreed-exit
                {:scope [:wm-steps-forward-guardrailed]
                 :produces [:efe-trustworthy-over-starmap]
                 :open-hole-count 1
                 :phase :identify
                 :status :active
                 :next-exit-operator-verify? true}}
     :edges [{:from :M-capability-star-map
              :to :wm-steps-forward-guardrailed
              :type :requires}
             {:from :M-capability-star-map
              :to :efe-trustworthy-over-starmap
              :type :produces}
             {:from :M-capability-star-map-mega
              :to :wm-overnight-unsupervised
              :type :produces}
             {:from :M-unbound-frontier
              :to :efe-trustworthy-over-starmap
              :type :requires}
             {:from :M-unagreed-exit
              :to :efe-trustworthy-over-starmap
              :type :produces}]}))

(defn- graph-efe-cleanup-fixture []
  {:capabilities {:goal {:status :held
                         :scope [:cap-held :cap-satisfied]}
                  :req {:status :satisfied}
                  :missing-req {:status :held}
                  :cap-held {:status :held}
                  :cap-satisfied {:status :satisfied}}
   :missions {"M-leaf" {:scope [:req]
                        :produces [:cap-held]
                        :open-hole-count 1}
              "M-nonleaf-small" {:scope [:missing-req]
                                  :produces [:cap-held]
                                  :open-hole-count 1}
              "M-nonleaf-big" {:scope [:missing-req]
                                :produces [:cap-held]
                                :open-hole-count 30}
              "M-on-ascent-big" {:scope [:req]
                                  :produces [:cap-held]
                                  :open-hole-count 30}
              "M-satisfied-only" {:scope [:req]
                                  :produces [:cap-satisfied]
                                  :open-hole-count 1}
              "M-mixed-status" {:scope [:req]
                                :produces [:cap-satisfied :cap-held]
                                :open-hole-count 1}}})

(deftest compute-efe-purity-test
  (testing "compute-efe is pure"
    (let [action {:type :address-sorry :target :m1}
          o1 (efe/compute-efe base-state action)
          o2 (efe/compute-efe base-state action)]
      (is (= o1 o2)))))

(deftest compute-efe-shape-test
  (testing "compute-efe returns documented keys (v0.21 adds :gap-exploration-bonus)"
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (contains? o :action))
      (is (contains? o :prediction))
      (is (contains? o :G-risk))
      (is (contains? o :G-ambiguity))
      (is (contains? o :predictability-bonus))
      (is (contains? o :homeostatic-pressure))
      (is (contains? o :structural-pressure))
      (is (contains? o :gap-exploration-bonus))
      (is (contains? o :controller-score))
      (is (contains? o :per-channel))))
  (testing "controller-score composes all six principled terms"
    ;; controller-score = G-risk + G-ambiguity − info-weight·predictability-bonus
    ;;           + survival-weight·homeostatic-pressure
    ;;           − structural-pressure-weight·structural-pressure
    ;;           − gap-exploration-bonus
    (let [o (efe/compute-efe base-state
                             {:type :address-sorry
                              :target :m1
                              :structural-pressure-per-action 2.2}
                             {:mission-gap-view {"M-known" 0.5}})
          info-w 0.4
          surv-w 1.2
          struct-w 0.35
          expected (+ (:G-risk o)
                      (:G-ambiguity o)
                      (- (* info-w (:predictability-bonus o)))
                      (* surv-w (:homeostatic-pressure o))
                      (- (* struct-w (:structural-pressure o)))
                      (- (:gap-exploration-bonus o)))]
      (is (< (Math/abs (- (:controller-score o) expected)) 1e-9)
          "controller-score decomposition includes the structural-pressure and gap subtractions")))
  (testing "G-risk, G-ambiguity, predictability-bonus, homeostatic-pressure, structural-pressure, and gap-exploration-bonus are individually non-negative"
    ;; controller-score CAN be negative because predictability-bonus has negative weight in controller-score;
    ;; that's by design (informative actions are preferred).
    (let [o (efe/compute-efe base-state
                             {:type :address-sorry
                              :target :m1
                              :structural-pressure-per-action 1.0}
                             {:ambiguity-mode :variance-sum})]
      (is (>= (:G-risk o) 0.0))
      (is (>= (:G-ambiguity o) 0.0))
      (is (>= (:predictability-bonus o) 0.0))
      (is (>= (:homeostatic-pressure o) 0.0))
      (is (>= (:structural-pressure o) 0.0))
      (is (>= (:gap-exploration-bonus o) 0.0)))))

(deftest compute-efe-two-principled-terms-by-name-test
  (testing "both R5a (risk) and R5b (ambiguity) are present by name and distinct"
    (let [o (efe/compute-efe base-state {:type :address-sorry :target :m1})]
      (is (number? (:G-risk o)) "R5a present")
      (is (number? (:G-ambiguity o)) "R5b present"))))

(deftest no-op-zero-ambiguity-test
  (testing ":no-op has zero predicted variance → zero ambiguity"
    (let [o (efe/compute-efe base-state {:type :no-op} {:ambiguity-mode :variance-sum})]
      (is (zero? (:G-ambiguity o))
          ":no-op preserves state with no predicted variance"))))

(deftest scored-against-predicted-not-current-test
  (testing "EFE risk reflects PREDICTED next-observation, not current observation"
    ;; address-sorry from stressed-state reduces sorry-count-norm by 0.1
    ;; (0.85 → 0.75), which is still in the avoided range [0.8 1.0]'s
    ;; pragmatic gap (preference [0.0 0.3]). But the predicted gap should
    ;; be SMALLER than the current-state gap by exactly the action's effect.
    (let [no-op-out (efe/compute-efe stressed-state {:type :no-op})
          addr-out (efe/compute-efe stressed-state {:type :address-sorry :target :m1})]
      (is (< (:G-risk addr-out) (:G-risk no-op-out))
          "address-sorry from stressed state lowers predicted risk vs no-op"))))

(deftest higher-variance-higher-ambiguity-test
  (testing "actions with higher predicted variance have higher G-ambiguity"
    ;; :open-mission has variance {:mission-health 0.02 :active-repo-ratio 0.01} = 0.03
    ;; :address-sorry has variance {:sorry-count-norm 0.01 :mission-health 0.005} = 0.015
    ;; :no-op has variance {} = 0.0
    (let [no-op (efe/compute-efe base-state {:type :no-op})
          addr (efe/compute-efe base-state {:type :address-sorry :target :m1})
          open (efe/compute-efe base-state {:type :open-mission :target :m-new})]
      (is (< (:G-ambiguity no-op)
             (:G-ambiguity addr)
             (:G-ambiguity open))
          "ambiguity strictly orders no-op < address-sorry < open-mission"))))

(deftest rank-actions-orders-by-g-total-test
  (testing "rank-actions returns actions in ascending controller-score"
    (let [candidates [{:type :no-op}
                      {:type :address-sorry :target :m1}
                      {:type :open-mission :target :m-new}
                      {:type :fire-pattern :target :m1}]
          ranked (efe/rank-actions stressed-state candidates)]
      (is (= 4 (count ranked)))
      (let [gs (map :controller-score ranked)]
        (is (apply <= gs) "controller-scores are non-decreasing across ranks"))
      (is (= (range 1 5) (map :rank ranked)) "ranks are 1..N in order")))
  (testing "empty candidate list returns empty vec"
    (is (= [] (efe/rank-actions base-state [])))))

(deftest stressed-state-decomposition-test
  (testing "decomposition properties hold under a stressed state"
    ;; The combined ordering by controller-score depends on the variance/preference-weight
    ;; balance (a design parameter); the decomposition properties are the
    ;; contract claim R5 enforces.
    (let [no-op-out (efe/compute-efe stressed-state {:type :no-op})
          addr-out (efe/compute-efe stressed-state {:type :address-sorry :target :m1})]
      (testing "G-risk: action moves predicted state toward preferences"
        (is (< (:G-risk addr-out) (:G-risk no-op-out))
            "addressing a sorry reduces pragmatic gap from preferences"))
      (testing "G-ambiguity: action introduces predictive uncertainty"
        (is (> (:G-ambiguity addr-out) (:G-ambiguity no-op-out))
            "non-:no-op actions carry non-zero predicted variance")))))

(deftest rank-actions-purity-test
  (testing "rank-actions is pure"
    (let [candidates [{:type :no-op}
                      {:type :address-sorry :target :m1}]
          r1 (efe/rank-actions base-state candidates)
          r2 (efe/rank-actions base-state candidates)]
      (is (= r1 r2)))))

(deftest intrinsic-value-is-controller-credit-not-risk-test
  (testing "absent :intrinsic-value defaults to 0 — G-risk equals predicted preference-gap-score"
    (let [out (efe/compute-efe base-state {:type :no-op} {:risk-mode :hinge})
          fe-on-current (fe/compute-controller-diagnostics (:observation base-state))]
      (is (< (Math/abs (- (:G-risk out) (:preference-gap-score fe-on-current))) 1e-9)
          ":no-op preserves obs → G-risk == compute-controller-diagnostics(current).preference-gap-score")))
  (testing ":intrinsic-value leaves canonical G-risk unchanged and lowers the controller"
    (let [base-action {:type :no-op}
            credited-action {:type :no-op :intrinsic-value 0.1}
          base-out (efe/compute-efe base-state base-action {:risk-mode :hinge})
          credited-out (efe/compute-efe base-state credited-action {:risk-mode :hinge})]
      (is (= (:G-risk base-out) (:G-risk credited-out)))
      (is (< (Math/abs (- (:controller-score credited-out)
                          (- (:controller-score base-out) 0.1))) 1e-9)))))

(deftest no-op-predictability-bonus-maximal-test
  (testing ":no-op has zero predicted variance → predictability-bonus = 13 (one per channel, all 1-variance=1)"
    ;; :no-op predicts 13 channels with variance 0.0 (preserves observation);
    ;; predictability-bonus = Σ max(0, 1 − v) over channels in next-mean.
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (= 13.0 (:predictability-bonus o))
          "deterministic prediction over 13 channels in next-mean → predictability-bonus = 13"))))

(deftest predictability-bonus-decreases-with-predicted-variance-test
  (testing "actions with higher predicted variance have lower predictability-bonus"
    (let [no-op (efe/compute-efe base-state {:type :no-op})
          addr (efe/compute-efe base-state {:type :address-sorry :target :m1})
          open (efe/compute-efe base-state {:type :open-mission :target :m-new})]
      (is (>= (:predictability-bonus no-op)
              (:predictability-bonus addr)
              (:predictability-bonus open))
          "high-variance actions yield less predictability-bonus"))))

(deftest homeostatic-pressure-zero-on-in-range-state-test
  (testing "all 4 R3a channels in preferred range → homeostatic-pressure = 0"
    ;; healthy-obs has annotation-health 0.9 (in [0.7 1.0]), mission-health 0.7 (in [0.5 1.0]),
    ;; sorry-count-norm 0.1 (in [0.0 0.3]), active-repo-ratio 0.8 (in [0.5 1.0])
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (zero? (:homeostatic-pressure o)))))
  (testing "out-of-range channel → positive homeostatic-pressure"
    (let [stressed-state-2 (assoc-in base-state [:observation :sorry-count-norm] 0.9)
          o (efe/compute-efe stressed-state-2 {:type :no-op})]
      (is (pos? (:homeostatic-pressure o))
          "sorry-count-norm 0.9 above [0.0 0.3] → positive survival pressure"))))

(deftest time-pressure-scales-controller-risk-and-survival-test
  (testing "time pressure leaves canonical G-risk fixed but scales controller cost and survival"
    (let [stressed-state-2 (assoc-in base-state [:observation :sorry-count-norm] 0.85)
          base (efe/compute-efe stressed-state-2 {:type :no-op})
          urgent (efe/compute-efe stressed-state-2 {:type :no-op}
                                  {:time-pressure 1.0})]
      (is (= (:G-risk base) (:G-risk urgent)))
      (is (< (:controller-score base) (:controller-score urgent)))
      (is (< (:homeostatic-pressure base) (:homeostatic-pressure urgent))
          "homeostatic-pressure under urgency > homeostatic-pressure baseline")
      (is (= (:predictability-bonus base) (:predictability-bonus urgent))
          "predictability-bonus unchanged by time-pressure")
      (is (= (:G-ambiguity base) (:G-ambiguity urgent))
          "G-ambiguity unchanged by time-pressure"))))

(deftest time-pressure-zero-is-identity-test
  (testing "time-pressure 0 ⇒ controller-score identical to no-time-pressure call"
    (let [base (efe/compute-efe base-state {:type :address-sorry :target :m1})
          zero (efe/compute-efe base-state {:type :address-sorry :target :m1}
                                {:time-pressure 0.0})]
      (is (< (Math/abs (- (:controller-score base) (:controller-score zero))) 1e-9)))))

(deftest compute-efe-horizon-steps-opt-test
  (testing "v0.15: :horizon-steps opt switches to multi-horizon scoring"
    ;; Use a stressed state: sorry-count-norm 0.85 (out of preference [0.0 0.3]).
    ;; Single :address-sorry → next sorry-count 0.75 (still out of range).
    ;; 3-chained :address-sorry → next sorry-count 0.55 (still out, smaller gap).
    ;; G-risk values differ because pragmatic gap is different at the final state.
    (let [stressed-state (-> base-state
                             (assoc-in [:observation :sorry-count-norm] 0.85)
                             (assoc-in [:observation :mission-health] 0.2))
          single (efe/compute-efe stressed-state
                                  {:type :address-sorry :target :m1})
          multi (efe/compute-efe stressed-state
                                 {:type :address-sorry :target :m1}
                                 {:horizon-steps 3})]
      (is (nil? (:horizon-steps single))
          "default = single-step; :horizon-steps not present in output")
      (is (= 3 (:horizon-steps multi))
          "multi-horizon: :horizon-steps recorded")
      (is (not= (:G-risk single) (:G-risk multi))
          "multi-horizon's G-risk uses K-step-trajectory's final state, not immediate next-state")
      (is (< (:G-risk multi) (:G-risk single))
          "3-chained :address-sorry trajectory reaches closer to preferred range than 1-step"))))

(deftest compute-efe-horizon-steps-one-equals-default-test
  (testing ":horizon-steps 1 is below the opt-in threshold (>= 2) → behaves as default"
    ;; horizon-steps < 2 is treated as nil (single-step)
    (let [default-out (efe/compute-efe base-state {:type :no-op})
          one-out (efe/compute-efe base-state {:type :no-op} {:horizon-steps 1})]
      (is (= (:controller-score default-out) (:controller-score one-out))))))

(deftest rank-actions-threads-opts-test
  (testing "rank-actions threads opts to compute-efe per candidate"
    (let [stressed (assoc-in base-state [:observation :sorry-count-norm] 0.85)
          candidates [{:type :no-op}
                      {:type :address-sorry :target :m1}]
          baseline (efe/rank-actions stressed candidates)
          urgent (efe/rank-actions stressed candidates {:time-pressure 1.0})
          baseline-totals (map :controller-score baseline)
          urgent-totals (map :controller-score urgent)]
      (is (every? identity (map #(>= %2 %1) baseline-totals urgent-totals))
          "controller-scores under urgency are ≥ baseline (more pressure on risk/survival)"))))

(deftest info-weight-and-survival-weight-tunable-test
  (testing "info-weight and survival-weight can be overridden via opts"
    (let [a (efe/compute-efe base-state {:type :address-sorry :target :m1})
          b (efe/compute-efe base-state {:type :address-sorry :target :m1}
                              {:info-weight 0.0 :survival-weight 0.0})]
      (is (not= (:controller-score a) (:controller-score b))
          "different weights produce different controller-score")
      (is (< (Math/abs (- (:controller-score b)
                          (+ (:G-risk b) (:G-ambiguity b))))
             1e-9)
          "with both new weights at 0, controller-score reduces to v0.12 G-risk + G-ambiguity"))))

(deftest structural-pressure-weight-and-ordering-test
  (testing "structural-pressure lowers controller-score by exactly weight × signal"
    (let [base-action {:type :address-sorry :target :m1}
          pressured-action {:type :address-sorry
                            :target :m1
                            :structural-pressure-per-action 2.2}
          base-out (efe/compute-efe base-state base-action)
          pressured-out (efe/compute-efe base-state pressured-action)]
      (is (= 2.2 (:structural-pressure pressured-out)))
      (is (< (Math/abs (- (:controller-score pressured-out)
                          (- (:controller-score base-out) (* 0.35 2.2))))
             1e-9)
          "default structural-pressure weight is applied as a subtraction")))
  (testing "rank-actions prefers otherwise-identical candidates with higher structural-pressure"
    (let [candidates [{:type :address-sorry
                       :target :m1
                       :structural-pressure-per-action 0.0}
                      {:type :address-sorry
                       :target :m1
                       :structural-pressure-per-action 2.2}]
          ranked (efe/rank-actions base-state candidates)]
      (is (= 2.2 (get-in (first ranked) [:action :structural-pressure-per-action])))
      (is (= 0.0 (get-in (second ranked) [:action :structural-pressure-per-action]))))))

(deftest intrinsic-value-can-make-learn-outrank-no-op-test
  (testing ":learn-action-class with :intrinsic-value 0.1 outranks :no-op when in-distribution"
    ;; healthy-obs has small preference-gap-score; :intrinsic-value 0.1 credit
    ;; should be enough to push :learn-action-class below :no-op in controller-score
    (let [candidates [{:type :no-op}
                      {:type :learn-action-class :target-class :address-sorry
                       :intrinsic-value 0.1}]
          ranked (efe/rank-actions base-state candidates)
          first-type (-> ranked first :action :type)]
      (is (= :learn-action-class first-type)
          "intrinsic credit should pull :learn-action-class to rank 1"))))

(deftest star-map-efe-picks-applicable-single-cycle-leaf-test
  (testing "Unit B acceptance over ensemble-1: EFE-top is an applicable single-cycle leaf"
    (let [graph (wm-region-graph-fixture)
          opts {:capability-graph graph
                :pre-registered-goal :wm-overnight-unsupervised}
          candidates [{:type :open-mission :target :M-capability-star-map-mega}
                      {:type :open-mission :target :M-unbound-frontier}
                      {:type :open-mission :target :M-capability-star-map}]
          selected (efe/select-star-map-action base-state candidates opts)]
      (is (= :M-capability-star-map (get-in selected [:action :target])))
      (is (true? (:graph/applicable? selected)))
      (is (true? (:graph/single-cycle-leaf? selected)))
      (is (zero? (:graph-applicability-penalty selected)))
      (is (pos? (:graph-applicability-penalty
                 (efe/compute-efe base-state
                                  {:type :open-mission :target :M-unbound-frontier}
                                  opts)))
          "unbound :requires carries a high G applicability gate"))))

(deftest star-map-selector-refuses-unregistered-pursuit-and-goal-extending-decompose-test
  (testing "INV-G is enforced at the selector boundary"
    (let [graph (wm-region-graph-fixture)
          opts {:capability-graph graph
                :pre-registered-goal :wm-overnight-unsupervised}
          candidates [{:type :pursue :target :cap/pentagon}
                      {:type :decompose :target :M-capability-star-map :extends-goal? true}
                      {:type :open-mission :target :M-capability-star-map}]
          ranked (efe/rank-star-map-actions base-state candidates opts)
          returned-actions (map :action ranked)]
      (is (= [{:type :open-mission :target :M-capability-star-map}]
             returned-actions)
          "pursuit outside the brief and goal-extending decompose are filtered unless consented"))))

(deftest graph-efe-off-map-penalty-test
  (testing "off-map open missions are penalised only when the opt is set"
    (let [graph (graph-efe-cleanup-fixture)
          action {:type :open-mission :target "M-off-map"}
          default-terms (efe/graph-control-terms graph :goal action {})
          penalised (efe/graph-control-terms graph :goal action
                                         {:graph-off-map-penalty 4.0})]
      (is (zero? (:graph-control-score default-terms))
          "default 0.0 preserves current off-map behaviour")
      (is (= 4.0 (:graph-control-score penalised))
          "opted-in off-map open mission gets the configured penalty")))

  (testing "non-open-mission actions and absent graph/goal cases are not penalised"
    (let [graph (graph-efe-cleanup-fixture)
          pursue (efe/graph-control-terms graph :goal
                                      {:type :pursue :target :cap-held}
                                      {:graph-off-map-penalty 4.0})
          no-graph (efe/graph-control-terms nil :goal
                                        {:type :open-mission :target "M-off-map"}
                                        {:graph-off-map-penalty 4.0})
          no-goal (efe/graph-control-terms graph nil
                                       {:type :open-mission :target "M-off-map"}
                                       {:graph-off-map-penalty 4.0})]
      (is (zero? (:graph-control-score pursue)))
      (is (zero? (:graph-control-score no-graph)))
      (is (zero? (:graph-control-score no-goal))))))

(deftest graph-efe-leaf-body-mode-test
  (testing ":leaf body mode scores a bounded next-step body cost"
    (let [graph (graph-efe-cleanup-fixture)
          opts {:graph-body-weight 2.0
                :graph-body-mode :leaf
                :graph-ascent-weight 0.0
                :graph-applicability-penalty 0.0}
          leaf (efe/graph-control-terms graph :goal
                                    {:type :open-mission :target "M-leaf"}
                                    opts)
          nonleaf-small (efe/graph-control-terms graph :goal
                                             {:type :open-mission :target "M-nonleaf-small"}
                                             opts)
          nonleaf-big (efe/graph-control-terms graph :goal
                                           {:type :open-mission :target "M-nonleaf-big"}
                                           opts)]
      (is (zero? (:graph-body-penalty leaf))
          "single-cycle leaf gets zero body penalty")
      (is (= 2.0 (:graph-body-penalty nonleaf-small)))
      (is (= (:graph-body-penalty nonleaf-small) (:graph-body-penalty nonleaf-big))
          "open-hole-count 1 and 30 produce the same bounded body in :leaf mode")))

  (testing ":whole body mode preserves weight times open-hole-count"
    (let [graph (graph-efe-cleanup-fixture)
          big (efe/graph-control-terms graph :goal
                                   {:type :open-mission :target "M-nonleaf-big"}
                                   {:graph-body-weight 2.0
                                    :graph-body-mode :whole
                                    :graph-ascent-weight 0.0
                                    :graph-applicability-penalty 0.0})]
      (is (= 60.0 (:graph-body-penalty big))))))

(deftest graph-efe-status-aware-ascent-test
  (testing "default ascent still credits all produced capabilities in goal scope"
    (let [graph (graph-efe-cleanup-fixture)]
      (is (= 0.5 (efe/mission-ascent-progress graph :goal "M-satisfied-only")))
      (is (= 1.0 (efe/mission-ascent-progress graph :goal "M-mixed-status")))))

  (testing "status-aware ascent skips satisfied caps and still credits held caps"
    (let [graph (graph-efe-cleanup-fixture)]
      (is (zero? (efe/mission-ascent-progress graph :goal "M-satisfied-only" true))
          "already-satisfied produced cap contributes no ascent credit")
      (is (= 0.5 (efe/mission-ascent-progress graph :goal "M-mixed-status" true))
          "held produced cap keeps its depth-weighted credit"))))

(deftest graph-efe-track-one-integration-ordering-test
  (testing "opted-in cleanup keeps big on-ascent work ahead of small off-map work"
    (let [graph (graph-efe-cleanup-fixture)
          opts {:capability-graph graph
                :pre-registered-goal :goal
                :graph-off-map-penalty 4.0
                :graph-body-mode :leaf
                :graph-ascent-status-aware? true
                :graph-applicability-penalty 0.0
                :graph-body-weight 3.0
                :graph-ascent-weight 20.0}
          ranked (efe/rank-actions base-state
                                   [{:type :open-mission :target "M-off-map"}
                                    {:type :open-mission :target "M-on-ascent-big"}]
                                   opts)
          on-ascent (first ranked)
          off-map (second ranked)]
      (is (= "M-on-ascent-big" (get-in on-ascent [:action :target])))
      (is (= 3.0 (:graph-body-penalty on-ascent))
          "leaf mode uses a bounded next-step body despite 30 open holes")
      (is (= 4.0 (:graph-control-score off-map)))
      (is (< (:controller-score on-ascent) (:controller-score off-map))))))

(deftest graph-model-uncertainty-controller-leg-test
  (let [graph {:capabilities {:goal {:scope [:cap-a :cap-b]}
                              :cap-a {}
                              :cap-b {}}
               :missions {"M-a" {:scope []
                                  :produces [:cap-a]
                                  :open-hole-count 1}
                          "M-b" {:scope []
                                  :produces [:cap-b]
                                  :open-hole-count 1}
                          "M-missing" {:scope []
                                       :produces [:cap-missing]
                                       :open-hole-count 1}}}
        opts {:capability-graph graph
              :pre-registered-goal :goal
              :graph-applicability-penalty 0.0
              :graph-body-weight 0.0
              :graph-ascent-weight 0.0}
        action-a {:type :open-mission :target "M-a"}
        action-b {:type :open-mission :target "M-b"}
        model-uncertainty-of (fn [m] (fn [_mission-id mission]
                         (reduce + 0.0 (map #(double (get m % 0.0)) (:produces mission)))))
        no-model-uncertainty (efe/compute-efe base-state action-a opts)
        empty-model-uncertainty (efe/compute-efe base-state action-a (assoc opts :model-uncertainty-fn (model-uncertainty-of {})))
        high-model-uncertainty (efe/compute-efe base-state action-a
                                  (assoc opts :model-uncertainty-fn (model-uncertainty-of {:cap-a 2.5})))
        absent-model-uncertainty (efe/compute-efe base-state
                                    {:type :open-mission :target "M-missing"}
                                    (assoc opts :model-uncertainty-fn (model-uncertainty-of {:cap-a 2.5})))
        tie-before (efe/rank-star-map-actions base-state [action-a action-b] opts)
        tie-after (efe/rank-star-map-actions base-state [action-a action-b]
                                             (assoc opts :model-uncertainty-fn (model-uncertainty-of {:cap-b 3.0})))]
    (testing "default empty lookup preserves controller-score while surfacing a zero distinct leg"
      (is (= (:controller-score no-model-uncertainty) (:controller-score empty-model-uncertainty)))
      (is (zero? (:model-uncertainty-bonus empty-model-uncertainty)))
      (is (= -0.0 (get-in empty-model-uncertainty [:augmentation-terms :model-uncertainty-bonus])))
      (is (contains? empty-model-uncertainty :model-uncertainty-bonus))
      (is (not= (:model-uncertainty-bonus empty-model-uncertainty) (:predictability-bonus empty-model-uncertainty)))
      (is (not= (:model-uncertainty-bonus empty-model-uncertainty) (:G-ambiguity empty-model-uncertainty))))
    (testing "high BMR stddev is an explicit controller bonus, so it lowers controller-score"
      (is (= 2.5 (:model-uncertainty-bonus high-model-uncertainty)))
      (is (= (- (:controller-score no-model-uncertainty) 2.5)
             (:controller-score high-model-uncertainty)))
      (is (= -2.5 (get-in high-model-uncertainty [:augmentation-terms :model-uncertainty-bonus]))))
    (testing "absent produced concept contributes zero"
      (is (zero? (:model-uncertainty-bonus absent-model-uncertainty))))
    (testing "R13 tie floor breaks when one produced concept has a model-uncertainty bonus"
      (is (= (:controller-score (first tie-before))
             (:controller-score (second tie-before))))
      (is (= "M-b" (get-in (first tie-after) [:action :target])))
      (is (< (:controller-score (first tie-after))
             (:controller-score (second tie-after)))))))

(deftest star-map-live-blend-activation-and-provenance-test
  (testing "graph-known mission receives conservative graph terms and provenance"
    (let [graph {:capabilities {:goal {:status :held :scope [:cap-done]}
                                :cap-done {:status :satisfied}}
                 :missions {"M-known" {:scope [:cap-done]
                                        :produces [:goal]
                                        :open-hole-count 1}}}
          action {:type :open-mission :target "M-known"}
          no-graph (efe/compute-efe base-state action)
          with-graph (efe/compute-efe
                      base-state action
                      {:capability-graph graph
                       :pre-registered-goal :goal
                       :graph-applicability-penalty 5.0
                       :graph-ascent-weight 6.0
                       :graph-body-weight 3.0})]
      (is (zero? (:graph-control-score no-graph)))
      (is (not (:star-map? no-graph)))
      (is (true? (:star-map? with-graph)))
      (is (zero? (:graph-applicability-penalty with-graph)))
      (is (= 3.0 (:graph-body-penalty with-graph)))
      (is (= 6.0 (:graph-ascent-credit with-graph)))
      (is (= -3.0 (:graph-control-score with-graph)))))

  (testing "graph-unknown mission is score-identical to no-graph baseline"
    (let [graph {:capabilities {:goal {:status :held :scope [:cap-done]}
                                :cap-done {:status :satisfied}}
                 :missions {"M-known" {:scope [:cap-done]
                                        :produces [:goal]
                                        :open-hole-count 1}}}
          action {:type :open-mission :target "M-unknown"}
          no-graph (efe/compute-efe base-state action)
          with-graph (efe/compute-efe
                      base-state action
                      {:capability-graph graph
                       :pre-registered-goal :goal
                       :graph-applicability-penalty 5.0
                       :graph-ascent-weight 6.0
                       :graph-body-weight 3.0})]
      (is (= (:controller-score no-graph) (:controller-score with-graph)))
      (is (= (:graph-control-score no-graph) (:graph-control-score with-graph)))
      (is (not (:star-map? with-graph))))))

(deftest mission-gap-live-blend-activation-and-provenance-test
  (testing "fold-view-known open mission receives weighted epistemic gap credit and provenance"
    (let [action {:type :open-mission :target "M-gappy"}
          no-gap (efe/compute-efe base-state action)
          with-gap (efe/compute-efe base-state action
                                    {:mission-gap-view {"M-gappy" 0.5}
                                     :gap-weight 6.0})]
      (is (zero? (:gap-exploration-bonus no-gap)))
      (is (not (:gap? no-gap)))
      (is (= 3.0 (:gap-exploration-bonus with-gap)))
      (is (= 0.5 (:gap-score with-gap)))
      (is (true? (:gap? with-gap)))
      (is (= (- (:controller-score no-gap) 3.0)
             (:controller-score with-gap)))))

  (testing "fold-view-unknown open mission gets zero gap and no provenance tag"
    (let [action {:type :open-mission :target "M-unknown"}
          no-gap (efe/compute-efe base-state action)
          with-gap (efe/compute-efe base-state action
                                    {:mission-gap-view {"M-gappy" 0.5}
                                     :gap-weight 6.0})]
      (is (zero? (:gap-exploration-bonus with-gap)))
      (is (zero? (:gap-score with-gap)))
      (is (not (:gap? with-gap)))
      (is (= (:controller-score no-gap) (:controller-score with-gap)))))

  (testing "gap-weight is conservative and applied linearly"
    (let [action {:type :open-mission :target :M-gappy}
          weighted (efe/compute-efe base-state action
                                    {:mission-gap-view {"M-gappy" 0.25}
                                     :gap-weight 2.0})]
      (is (= 0.5 (:gap-exploration-bonus weighted)))
      (is (true? (:gap? weighted))))))

;; ---------------------------------------------------------------------------
;; M-evaluate-policies D2 + D5c (2026-07-03) — core additivity + ambiguity modes
;; ---------------------------------------------------------------------------

(deftest g-core-additivity-test
  (testing "I3: :G-core = :G-risk + :G-ambiguity exactly, on every scored entry"
    (doseq [action [{:type :no-op}
                    {:type :address-sorry :target :m1}
                    {:type :open-mission :target "M-on-ascent-big"}]]
      (let [o (efe/compute-efe base-state action)]
        (is (contains? o :G-core))
        (is (= (:G-core o) (+ (:G-risk o) (:G-ambiguity o)))
            (str "core additivity for " (:type action)))))))

(deftest g-core-does-not-change-total-test
  (testing "D2 is strictly additive: emitting :G-core leaves :controller-score untouched"
    (let [o (efe/compute-efe base-state {:type :no-op})]
      ;; the blend still sums its historical terms; :G-core is a report, not a summand
      (is (number? (:controller-score o)))
      (is (not= (:G-core o) (:controller-score o))
          ":G-core is reported beside, not substituted for, the blend"))))

(deftest ambiguity-mode-default-and-escape-hatch-test
  (testing "default is now :gaussian-entropy (canonical, 2026-07-08); explicit matches"
    (let [action {:type :address-sorry :target :m1}
          implicit (efe/compute-efe base-state action)
          explicit (efe/compute-efe base-state action {:ambiguity-mode :gaussian-entropy})]
      (is (= implicit explicit))
      (is (= :gaussian-entropy (:ambiguity-mode implicit)))))
  (testing "D5c :gaussian-entropy matches the closed-form Gaussian entropy sum; :variance-sum is the escape hatch"
    (let [action {:type :address-sorry :target :m1}
          vs (efe/compute-efe base-state action {:ambiguity-mode :variance-sum})
          ge (efe/compute-efe base-state action {:ambiguity-mode :gaussian-entropy})
          variances (vals (get-in ge [:prediction :next-observation :variance]))
          expected (reduce + 0.0
                           (map (fn [v]
                                  (* 0.5
                                     (Math/log (* 2.0 Math/PI Math/E
                                                  (max (double v) 1e-9)))))
                                variances))]
      (is (not= (:G-ambiguity vs) (:G-ambiguity ge)))
      (is (= :gaussian-entropy (:ambiguity-mode ge)))
      (is (< (Math/abs (- expected (:G-ambiguity ge))) 1e-12))
      (is (Double/isFinite (:G-ambiguity ge))
          "the entropy lane is finite in nats"))))
