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
   the weighted channels (so G-pragmatic is small but the action-
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

(deftest compute-efe-purity-test
  (testing "compute-efe is pure"
    (let [action {:type :address-sorry :target :m1}
          o1 (efe/compute-efe base-state action)
          o2 (efe/compute-efe base-state action)]
      (is (= o1 o2)))))

(deftest compute-efe-shape-test
  (testing "compute-efe returns documented keys (v0.20 adds :G-structural-pressure)"
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (contains? o :action))
      (is (contains? o :prediction))
      (is (contains? o :G-risk))
      (is (contains? o :G-ambiguity))
      (is (contains? o :G-info))
      (is (contains? o :G-survival))
      (is (contains? o :G-structural-pressure))
      (is (contains? o :G-total))
      (is (contains? o :per-channel))))
  (testing "G-total composes all five principled terms"
    ;; G-total = G-risk + G-ambiguity − info-weight·G-info
    ;;           + survival-weight·G-survival
    ;;           − structural-pressure-weight·G-structural-pressure
    (let [o (efe/compute-efe base-state
                             {:type :address-sorry
                              :target :m1
                              :structural-pressure-per-action 2.2})
          info-w 0.4
          surv-w 1.2
          struct-w 0.35
          expected (+ (:G-risk o)
                      (:G-ambiguity o)
                      (- (* info-w (:G-info o)))
                      (* surv-w (:G-survival o))
                      (- (* struct-w (:G-structural-pressure o))))]
      (is (< (Math/abs (- (:G-total o) expected)) 1e-9)
          "G-total decomposition includes the structural-pressure subtraction")))
  (testing "G-risk, G-ambiguity, G-info, G-survival, G-structural-pressure are individually non-negative"
    ;; G-total CAN be negative because G-info has negative weight in G-total;
    ;; that's by design (informative actions are preferred).
    (let [o (efe/compute-efe base-state
                             {:type :address-sorry
                              :target :m1
                              :structural-pressure-per-action 1.0})]
      (is (>= (:G-risk o) 0.0))
      (is (>= (:G-ambiguity o) 0.0))
      (is (>= (:G-info o) 0.0))
      (is (>= (:G-survival o) 0.0))
      (is (>= (:G-structural-pressure o) 0.0)))))

(deftest compute-efe-two-principled-terms-by-name-test
  (testing "both R5a (risk) and R5b (ambiguity) are present by name and distinct"
    (let [o (efe/compute-efe base-state {:type :address-sorry :target :m1})]
      (is (number? (:G-risk o)) "R5a present")
      (is (number? (:G-ambiguity o)) "R5b present"))))

(deftest no-op-zero-ambiguity-test
  (testing ":no-op has zero predicted variance → zero ambiguity"
    (let [o (efe/compute-efe base-state {:type :no-op})]
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
  (testing "rank-actions returns actions in ascending G-total"
    (let [candidates [{:type :no-op}
                      {:type :address-sorry :target :m1}
                      {:type :open-mission :target :m-new}
                      {:type :fire-pattern :target :m1}]
          ranked (efe/rank-actions stressed-state candidates)]
      (is (= 4 (count ranked)))
      (let [gs (map :G-total ranked)]
        (is (apply <= gs) "G-totals are non-decreasing across ranks"))
      (is (= (range 1 5) (map :rank ranked)) "ranks are 1..N in order")))
  (testing "empty candidate list returns empty vec"
    (is (= [] (efe/rank-actions base-state [])))))

(deftest stressed-state-decomposition-test
  (testing "decomposition properties hold under a stressed state"
    ;; The combined ordering by G-total depends on the variance/preference-weight
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

(deftest intrinsic-value-credits-g-risk-test
  (testing "absent :intrinsic-value defaults to 0 — G-risk equals predicted G-pragmatic"
    (let [out (efe/compute-efe base-state {:type :no-op})
          fe-on-current (fe/compute-free-energy (:observation base-state))]
      (is (< (Math/abs (- (:G-risk out) (:G-pragmatic fe-on-current))) 1e-9)
          ":no-op preserves obs → G-risk == compute-free-energy(current).G-pragmatic")))
  (testing ":intrinsic-value subtracts from G-risk"
    (let [base-action {:type :no-op}
            credited-action {:type :no-op :intrinsic-value 0.1}
          base-out (efe/compute-efe base-state base-action)
          credited-out (efe/compute-efe base-state credited-action)]
      (is (< (Math/abs (- (:G-risk credited-out)
                          (- (:G-risk base-out) 0.1)))
             1e-9)
          ":intrinsic-value 0.1 lowers G-risk by exactly 0.1"))))

(deftest no-op-info-gain-maximal-test
  (testing ":no-op has zero predicted variance → G-info = 13 (one per channel, all 1-variance=1)"
    ;; :no-op predicts 13 channels with variance 0.0 (preserves observation);
    ;; info-gain = Σ max(0, 1 − v) over channels in next-mean.
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (= 13.0 (:G-info o))
          "deterministic prediction over 13 channels in next-mean → G-info = 13"))))

(deftest info-gain-decreases-with-predicted-variance-test
  (testing "actions with higher predicted variance have lower G-info"
    (let [no-op (efe/compute-efe base-state {:type :no-op})
          addr (efe/compute-efe base-state {:type :address-sorry :target :m1})
          open (efe/compute-efe base-state {:type :open-mission :target :m-new})]
      (is (>= (:G-info no-op)
              (:G-info addr)
              (:G-info open))
          "high-variance actions yield less info-gain"))))

(deftest survival-cost-zero-on-in-range-state-test
  (testing "all 4 R3a channels in preferred range → G-survival = 0"
    ;; healthy-obs has annotation-health 0.9 (in [0.7 1.0]), mission-health 0.7 (in [0.5 1.0]),
    ;; sorry-count-norm 0.1 (in [0.0 0.3]), active-repo-ratio 0.8 (in [0.5 1.0])
    (let [o (efe/compute-efe base-state {:type :no-op})]
      (is (zero? (:G-survival o)))))
  (testing "out-of-range channel → positive G-survival"
    (let [stressed-state-2 (assoc-in base-state [:observation :sorry-count-norm] 0.9)
          o (efe/compute-efe stressed-state-2 {:type :no-op})]
      (is (pos? (:G-survival o))
          "sorry-count-norm 0.9 above [0.0 0.3] → positive survival pressure"))))

(deftest time-pressure-scales-risk-and-survival-test
  (testing "v0.14: time-pressure scales G-risk + G-survival but not G-info or G-ambiguity"
    (let [stressed-state-2 (assoc-in base-state [:observation :sorry-count-norm] 0.85)
          base (efe/compute-efe stressed-state-2 {:type :no-op})
          urgent (efe/compute-efe stressed-state-2 {:type :no-op}
                                  {:time-pressure 1.0})]
      (is (< (:G-risk base) (:G-risk urgent))
          "G-risk under urgency > G-risk baseline")
      (is (< (:G-survival base) (:G-survival urgent))
          "G-survival under urgency > G-survival baseline")
      (is (= (:G-info base) (:G-info urgent))
          "G-info unchanged by time-pressure")
      (is (= (:G-ambiguity base) (:G-ambiguity urgent))
          "G-ambiguity unchanged by time-pressure"))))

(deftest time-pressure-zero-is-identity-test
  (testing "time-pressure 0 ⇒ G-total identical to no-time-pressure call"
    (let [base (efe/compute-efe base-state {:type :address-sorry :target :m1})
          zero (efe/compute-efe base-state {:type :address-sorry :target :m1}
                                {:time-pressure 0.0})]
      (is (< (Math/abs (- (:G-total base) (:G-total zero))) 1e-9)))))

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
      (is (= (:G-total default-out) (:G-total one-out))))))

(deftest rank-actions-threads-opts-test
  (testing "rank-actions threads opts to compute-efe per candidate"
    (let [stressed (assoc-in base-state [:observation :sorry-count-norm] 0.85)
          candidates [{:type :no-op}
                      {:type :address-sorry :target :m1}]
          baseline (efe/rank-actions stressed candidates)
          urgent (efe/rank-actions stressed candidates {:time-pressure 1.0})
          baseline-totals (map :G-total baseline)
          urgent-totals (map :G-total urgent)]
      (is (every? identity (map #(>= %2 %1) baseline-totals urgent-totals))
          "G-totals under urgency are ≥ baseline (more pressure on risk/survival)"))))

(deftest info-weight-and-survival-weight-tunable-test
  (testing "info-weight and survival-weight can be overridden via opts"
    (let [a (efe/compute-efe base-state {:type :address-sorry :target :m1})
          b (efe/compute-efe base-state {:type :address-sorry :target :m1}
                              {:info-weight 0.0 :survival-weight 0.0})]
      (is (not= (:G-total a) (:G-total b))
          "different weights produce different G-total")
      (is (< (Math/abs (- (:G-total b)
                          (+ (:G-risk b) (:G-ambiguity b))))
             1e-9)
          "with both new weights at 0, G-total reduces to v0.12 G-risk + G-ambiguity"))))

(deftest structural-pressure-weight-and-ordering-test
  (testing "structural-pressure lowers G-total by exactly weight × signal"
    (let [base-action {:type :address-sorry :target :m1}
          pressured-action {:type :address-sorry
                            :target :m1
                            :structural-pressure-per-action 2.2}
          base-out (efe/compute-efe base-state base-action)
          pressured-out (efe/compute-efe base-state pressured-action)]
      (is (= 2.2 (:G-structural-pressure pressured-out)))
      (is (< (Math/abs (- (:G-total pressured-out)
                          (- (:G-total base-out) (* 0.35 2.2))))
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
    ;; healthy-obs has small G-pragmatic; :intrinsic-value 0.1 credit
    ;; should be enough to push :learn-action-class below :no-op in G-total
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
      (is (zero? (:G-applicability selected)))
      (is (pos? (:G-applicability
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
      (is (zero? (:G-graph-pragmatic no-graph)))
      (is (not (:star-map? no-graph)))
      (is (true? (:star-map? with-graph)))
      (is (zero? (:G-applicability with-graph)))
      (is (= 3.0 (:G-body-size with-graph)))
      (is (= 6.0 (:G-ascent-progress with-graph)))
      (is (= -3.0 (:G-graph-pragmatic with-graph)))))

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
      (is (= (:G-total no-graph) (:G-total with-graph)))
      (is (= (:G-graph-pragmatic no-graph) (:G-graph-pragmatic with-graph)))
      (is (not (:star-map? with-graph))))))
