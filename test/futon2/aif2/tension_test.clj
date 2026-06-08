(ns futon2.aif2.tension-test
  "Slice-1 INSTANTIATE tests for the tension-proposer (M-aif2 §5 Stage-C +
   §3e invariants), transcribed over the real proposer. Fixture mirrors the
   keystone E1 witness trace (substrate_metric_e1_invariants.clj): a firing
   mission + firing sorry, a complete-bridge (Stage-B guard), a flat node, an
   unknown-resolvedness node, and a non-actionable file."
  (:require [clojure.test :refer [deftest testing is]]
            [futon2.aif2.tension :as t]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.intrinsic-values :as iv]))

(def signal
  [{:node/id :m-open  :node/type :mission
    :curvature/strain? true :curvature/min-incident-kappa -0.5556
    :curvature/strain-edge [:m-open :m-edit-cycle]
    :resolution-state/resolvedness 0.10 :resolution-state/actionable? true}
   {:node/id :s-open  :node/type :sorry
    :curvature/strain? true :curvature/min-incident-kappa -0.40
    :curvature/strain-edge [:s-open :m-b]
    :resolution-state/resolvedness 0.0 :resolution-state/actionable? true}
   ;; complete mission on a SHARP negative bridge — must NOT fire (Stage-B guard)
   {:node/id :m-complete :node/type :mission
    :curvature/strain? true :curvature/min-incident-kappa -0.80
    :resolution-state/resolvedness 1.0 :resolution-state/actionable? false}
   ;; unresolved + actionable but NOT strained (positive κ) — must NOT fire
   {:node/id :s-flat :node/type :sorry
    :curvature/strain? false :curvature/min-incident-kappa 0.10
    :resolution-state/resolvedness 0.0 :resolution-state/actionable? true}
   ;; no native resolution-state — must NOT fire (numeric guard)
   {:node/id :p-unknown :node/type :pattern
    :curvature/strain? true :curvature/min-incident-kappa -0.20
    :resolution-state/resolvedness :unknown :resolution-state/actionable? false}
   ;; file: strained via file->mission but non-actionable directly — must NOT fire
   {:node/id :f-file :node/type :file
    :curvature/strain? true :curvature/min-incident-kappa -0.30
    :resolution-state/resolvedness :unknown :resolution-state/actionable? false}])

(defn- emit [] (t/propose-from-signal t/tension-entry t/default-consent signal))

(deftest acc-tension-generates
  (testing "high-tension fixture yields concrete, non-:learn-action-class candidates"
    (let [cands (emit)]
      (is (= 2 (count cands)) "exactly the firing mission + firing sorry")
      (is (every? #(not (#{:no-op :learn-action-class} (:type %))) cands)))))

(deftest kappa-routing
  (testing "κ maps node-type to the existing S2 action-class"
    (let [by-target (into {} (map (juxt :target :type) (emit)))]
      (is (= :open-mission  (by-target :m-open)))
      (is (= :address-sorry (by-target :s-open))))))

(deftest stage-b-guard
  (testing "a COMPLETE node on a sharp negative bridge does NOT fire"
    (let [targets (set (map :target (emit)))]
      (is (not (contains? targets :m-complete))
          "resolvedness 1.0 ⇒ non-actionable, even at min-κ −0.80"))))

(deftest gate-non-firing-cases
  (testing "flat (no strain), unknown resolvedness, and non-actionable file are all gated out"
    (let [targets (set (map :target (emit)))]
      (is (not (contains? targets :s-flat))   "no strain ⇒ no fire")
      (is (not (contains? targets :p-unknown)) ":unknown resolvedness ⇒ no fire")
      (is (not (contains? targets :f-file))    "non-actionable file ⇒ no fire (routes via file→mission)"))))

(deftest inv-provenance
  (testing "every candidate carries a valid type, a target, proposer-id, and numeric credit"
    (doseq [c (emit)]
      (is (contains? fm/action-types (:type c)) "type is an existing action-class")
      (is (some? (:target c)))
      (is (= :s1/tension (get-in c [:provenance :proposer-id])))
      (is (number? (:intrinsic-value c)) "live Beta credit keyed by entry id"))))

(deftest inv-no-bypass
  (testing "propose-here? requires strain ∧ numeric-unresolved ∧ actionable (no short-circuit)"
    (is (t/propose-here? (first signal)))                       ; m-open: all three hold
    (is (not (t/propose-here? {:curvature/strain? true          ; missing actionable?
                               :resolution-state/resolvedness 0.0})))
    (is (not (t/propose-here? {:curvature/strain? false         ; missing strain
                               :resolution-state/resolvedness 0.0
                               :resolution-state/actionable? true})))
    (is (not (t/propose-here? {:curvature/strain? true          ; resolvedness = 1.0
                               :resolution-state/resolvedness 1.0
                               :resolution-state/actionable? true})))))

(deftest inv-consent-credited-and-gated
  (testing "the entry has a Beta credit AND is admissibility-gated, not hardcoded always-on"
    (is (number? (iv/credit-for :s1/tension)))
    (is (seq (emit)) "fires under operator-ratified consent")
    (is (= [] (t/propose-from-signal t/tension-entry {:operator-ratified? false} signal))
        "deny consent ⇒ emits nothing (proves the gate)")
    (is (= [] (t/propose-from-signal (assoc t/tension-entry :status :pruned) t/default-consent signal))
        "pruned entry ⇒ emits nothing")))

(deftest inv-reduction-additive
  (testing "no curvature signal ⇒ silent: frozen `aif` behaviour preserved"
    (is (true? (t/emits-nothing-without-signal?)))
    (is (= [] (t/propose-from-signal t/tension-entry t/default-consent [])))))

(def artifact-candidates
  "Shape of the keystone R2 `top_propose_candidates` entries."
  [{:node "futon4-d/mission/essays-diachronic-model" :node_type "mission"
    :min_incident_kappa -0.5556 :resolvedness 0.1
    :strain_edge ["futon4-d/mission/essays-diachronic-model" "futon4-elisp-d/mission/essays-edit-cycle"]}
   {:node "sorry-x" :node_type "sorry" :min_incident_kappa -0.4 :resolvedness 0.0
    :strain_edge ["sorry-x" "mission-y"]}])

(deftest seam-candidates-to-signal-and-propose
  (testing "artifact candidates → curvature-signal → proposer emits κ-routed candidates"
    (let [sig   (t/candidates->signal artifact-candidates)
          cands (t/propose-from-signal t/tension-entry t/default-consent sig)
          by-t  (into {} (map (juxt :target :type) cands))]
      (is (= 2 (count cands)))
      (is (= :open-mission  (by-t "futon4-d/mission/essays-diachronic-model")))
      (is (= :address-sorry (by-t "sorry-x"))))))

(deftest seam-read-curvature-signal-failsafe
  (testing "absent/malformed artifact ⇒ [] (fail-safe: WM left unchanged)"
    (is (= [] (t/read-curvature-signal "/nonexistent/path/nope.json")))))

(deftest proposer-protocol
  (testing "ActionProposer impl reads (:curvature-signal state) and has a stable id"
    (let [p (t/tension-proposer)]
      (is (= :s1/tension (ap/proposer-id p)))
      (is (= 2 (count (ap/propose p {:curvature-signal signal}))))
      (is (= [] (ap/propose p {}))  "no signal in state ⇒ empty"))))
