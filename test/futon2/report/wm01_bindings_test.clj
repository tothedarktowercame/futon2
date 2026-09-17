(ns futon2.report.wm01-bindings-test
  "WM-01-bindings, slice 1: bind State/Action/Outcome meanings, support
   mappings and model revisions across the NAMED ACTUAL producer/consumer
   objects of the selected occurrence (the tick's cascade decision for
   M-wm-aif-policy-grain-compliance), and record the consumer identity
   actually found rather than the one the documents assume.

   Actual binding found by reading the live lane (war_machine.clj
   cascade-lane / cascade-decision, R1→R16→R9):

   - State: cascade-model-manifest token-set belief states. R1
     (observed-belief) and R4 (forward-model rollout) carry beliefs over
     SETS of tokens; machine-model's :state-support / belief posterior
     vocabulary is NOT the state carrier of this occurrence.
   - Action: cascade-policy candidate-space precedences of pattern maps
     ({:guard {:needs #{[target token]}} :produces #{…}}), target-qualified;
     the R14 action marginal is keyed by each cascade's first acting
     pattern, with cascade-selection's declared tie-break.
   - Outcome: the common QUALIFIED-token universe of the joint family; G by
     efe/rank-actions → cascade-model-manifest/horizon-g-sparse with
     :rates :zero-adjudication-identity, recorded on the ranked meta. The
     ruled-outcome-c disposition carrier (machine-model/outcome-authority)
     is NOT this occurrence's outcome carrier.
   - Model revisions: no :wm/machine-model-v1 schema, no machine-model
     :model {:id :revision} identity travels this lane; pattern identity is
     the (target, pattern-id) pair. machine-model participates in this
     occurrence ONLY as a compile-time transitive require (efe requires
     machine-q-risk, which requires machine-model) — its admissions
     (row-sum / distribution-admission) are not applied to any row on this
     path. A compile-time require is not a runtime admission: generic
     support admission does not establish specialized reader
     applicability, and this test pins that machine-model's specialized
     vocabulary is absent from the occurrence's actual decision data."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [clojure.walk :as walk]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.report.war-machine :as wm]))

(def target :wm-tick-001-observation-crash)

(def universe
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
  {:patterns
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
   :receipts
   {:aif/structured-observation-vector {:receipt "S1" :source "03-R6"}
    :aif/placeholder-is-load-bearing {:receipt "PH" :source "03-R6"}
    :test-step-covering-missing-total-repos {:receipt "TS" :source "03-R6"}}})

(def receipt
  {:kind :construction-receipt :moves [:interpret :order]
   :family-searched 3 :coverage 1})

(def candidates
  [{:precedence [:test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/placeholder-is-load-bearing
                 :test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/structured-observation-vector]
    :construction-receipt receipt}])

(def sources
  {:universes {target universe}
   :interpretations {target interpretations}
   :wants {target want}
   :candidates {target candidates}
   :horizon-steps 3
   :beta-by-context {:tick-1 {:beta 1}}
   :context-of (fn [_] :tick-1)})

(defn- assembled* []
  (cp/assemble {:targets [target]
                :sources (locfix/locate-all
                          {:universes {target universe}
                           :interpretations {target interpretations}
                           :wants {target want}
                           :candidates {target candidates}
                           :horizon-steps 3
                           :beta-by-context {:tick-1 {:beta 1}}
                           :context-of (fn [_] :tick-1)})}))

(def ^:private machine-model-vocabulary
  "machine-model's specialized reader vocabulary. None of it may appear in
   the occurrence's actual decision data (compile-time requires are not
   runtime admissions)."
  [:wm/machine-model-v1 :state-support :outcome-authority
   :machine-model/refusal :policy-prior :observation-encoding])

(defn- contains-any-keyword? [x kws]
  (let [found (atom false)]
    (walk/postwalk
     (fn [v] (when (and (keyword? v) (some #(= v %) kws)) (reset! found true)) v)
     x)
    @found))

(deftest state-action-outcome-bindings-of-the-selected-occurrence
  (let [a (assembled*)
        problem (-> a :problems first :cascade-problem)
        _ (is (some? problem) "assembled problem exists for the target")
        lane (wm/cascade-lane problem)
        ;; the lane either completes or returns a typed refusal; for this
        ;; fixture it must COMPLETE (the decision is the occurrence).
        _ (is (nil? (:stopped-at lane)) (str "lane stopped: " (:refusal lane)))
        route (mapv :node (:route lane))
        ranked (:ranked lane)
        scoring (:cascade-scoring (meta ranked))]
    (is (= [:R1 :R6 :R13 :R4 :R5 :R14] (vec (take 6 route)))
        "producer identity: the route names the actual node functions")
    (is (= ["futon2.aif.cascade-model-manifest/observed-belief"
            "futon2.aif.cascade-policy/candidate-space"
            "futon2.aif.policy-depth/configured"
            "futon2.aif.forward-model/predict-multi-horizon"
            "futon2.aif.efe/rank-actions"
            "futon2.aif.policy/select-action-cascades"]
           (mapv :via (take 6 (:route lane))))
        "consumer identity: each node records the function actually used")
    ;; Outcome vocabulary: the common qualified-token universe, identity rates.
    (is (map? scoring) "ranked carries :cascade-scoring meta")
    (is (= 3 (:horizon scoring)) "the declared common horizon T")
    (is (= :zero-adjudication-identity (:rates scoring))
        "outcome channel: zero-adjudication identity, NOT ruled-outcome-c dispositions")
    (is (set? (:universe scoring)) "universe is a set of qualified tokens")
    ;; Action vocabulary: token-interpretation pattern maps — the recorded
    ;; binding is the INTERPRETED guard shape (clauses of present/absent
    ;; token sets), not the raw :needs/:forbids input spelling.
    (let [precedences (mapcat :precedence (map :action ranked))
          clause-tokens (fn [p]
                          (set (for [c (-> p :guard :clauses)
                                     t (concat (:present c) (:absent c))]
                                 t)))]
      (is (seq precedences))
      (is (every? #(and (map? %) (:id %)
                        (-> % :guard :clauses seq))
                  precedences)
          "actions are interpreted pattern maps (guard clauses over token sets)")
      (is (= #{:summary-without-total-repos-throws
               :summary-without-total-repos-observes-cleanly
               :coupling-density-reads-same-key-with-default
               :test-covers-missing-total-repos}
             (reduce set/union #{} (map clause-tokens precedences)))
          "guard tokens are the fixture's guard vocabulary — the State vocabulary"))
    ;; State vocabulary: beliefs are token-set keyed; no machine-model
    ;; state-support anywhere in the lane result.
    (is (not (contains-any-keyword? lane machine-model-vocabulary))
        "machine-model's specialized vocabulary is absent from the lane data")))

(deftest decision-law-and-revision-bindings
  (let [r (wm/cascade-decision (assembled*) {})
        decision (:decision r)]
    (is (map? decision))
    (is (not= :abstained (:status decision)) "the occurrence yields a decision")
    (is (= :cascade-selection-posterior
           (get-in decision [:selection-law :applied]))
        "the selection law actually applied")
    (is (= :declared (get-in decision [:beta :status])))
    (is (= 1 (get-in decision [:beta :value])) "declared β travels with the decision")
    (is (= :action-name-ascending
           (get-in decision [:selection-law :tie-break-rule]))
        "cascade-selection's declared tie-break is the recorded rule")
    (is (not (contains-any-keyword? decision machine-model-vocabulary))
        "no machine-model admission vocabulary in the decision")))
