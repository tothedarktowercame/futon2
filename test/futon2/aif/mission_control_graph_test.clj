(ns futon2.aif.mission-control-graph-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.mission-control-graph :as graph]))

(def witnessed-edge
  {:mission-id "M-typed-memories"
   :control-pattern-id "p4ng/R6-candidate-pattern-action-space"
   :relation :requires-control
   :status :witnessed
   :provenance [{:kind :mission-text :ref "M-typed-memories.md#retrieval-gap"}]
   :memory-ids ["e-6bcbb51e"]})

(deftest witnessed-typed-relations-project-reason-bearing-candidates
  (let [second-edge
        {:mission-id "M-wm-policy-grain"
         :control-pattern-id "p4ng/R15-hierarchical-temporal-depth"
         :relation :repairs-control
         :status :witnessed
         :provenance [{:kind :test :ref "cascade_prior_test"}]
         :memory-ids []}
        proposal (assoc witnessed-edge
                        :mission-id "M-unwitnessed"
                        :status :proposed
                        :provenance [{:kind :embedding :model "mission-p4ng-v0"}])
        result
        (graph/candidate-projection
         ["p4ng/R6-candidate-pattern-action-space"
          "p4ng/R15-hierarchical-temporal-depth"]
         [witnessed-edge second-edge proposal])]
    (is (= ["M-typed-memories" "M-wm-policy-grain"]
           (mapv :mission-id (:candidates result))))
    (is (= ["e-6bcbb51e"] (-> result :candidates first :memory-ids)))
    (is (= 1 (get-in result [:audit :proposal-count])))
    (is (= 2 (get-in result [:audit :witnessed-support-count])))))

(deftest witnessed-block-is-support-removal-not-zero-valued-ranking
  (let [blocked (assoc witnessed-edge
                       :relation :blocked-by-control
                       :memory-ids ["e-blocker"])
        result (graph/candidate-projection
                [(:control-pattern-id witnessed-edge)]
                [witnessed-edge blocked])]
    (is (empty? (:candidates result)))
    (is (= 1 (get-in result [:audit :witnessed-block-count])))
    (is (= [{:mission-id "M-typed-memories"
             :exclusion :witnessed-block
             :blocking-relations
             [{:control-pattern-id
               "p4ng/R6-candidate-pattern-action-space"
               :relation :blocked-by-control
               :provenance
               [{:kind :mission-text
                 :ref "M-typed-memories.md#retrieval-gap"}]
               :memory-ids ["e-blocker"]}]}]
           (:excluded-missions result)))))

(deftest embeddings-propose-but-do-not-certify
  (let [proposal (assoc witnessed-edge
                        :status :proposed
                        :provenance [{:kind :embedding
                                      :space :mission-plus-p4ng-control}])
        result (graph/candidate-projection
                [(:control-pattern-id proposal)] [proposal])]
    (is (empty? (:candidates result)))
    (is (= 1 (get-in result [:audit :proposal-count])))))

(deftest malformed-or-unattributed-edges-fail-closed
  (testing "ordinary library patterns cannot masquerade as p4ng controls"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"invalid mission-control edge"
         (graph/validate-edge
          (assoc witnessed-edge :control-pattern-id "agent/sense-deliberate-act")))))
  (testing "a relation without provenance is not graph evidence"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"invalid mission-control edge"
         (graph/validate-edge (assoc witnessed-edge :provenance []))))))

(defn- wm-memory
  [id pattern mission body]
  {:memory/id id
   :memory/domain :war-machine
   :memory/state :current
   :memory/attachment-status :reviewed
   :memory/witness-status :independently-witnessed
   :memory/pattern-ids [pattern]
   :memory/mission-ids [mission]
   :memory/hook (:hook body)
   :memory/body body})

(deftest strict-dark-projection-carries-bodies-and-fails-closed
  (let [pattern "p4ng/R15-hierarchical-temporal-depth"
        mission "M-wm-strategic-mission-selection"
        support (assoc witnessed-edge
                       :mission-id mission
                       :control-pattern-id pattern
                       :memory-ids ["e-wm-support"])
        proposed (assoc support
                        :mission-id "M-proposed"
                        :status :proposed
                        :memory-ids ["e-cross-domain"])
        memories [(wm-memory
                   "e-wm-support" pattern mission
                   {:hook "Select strategic and tactical policies separately."
                    :observation :policy-grain-mismatch})
                  (assoc
                   (wm-memory "e-cross-domain" pattern "M-proposed"
                              {:hook "A mathematics analogy."})
                   :memory/domain :mathematics)]
        result (graph/candidate-projection
                [pattern] [support proposed] memories)
        candidate (first (:candidates result))]
    (is (= [mission] (mapv :mission-id (:candidates result))))
    (is (= :policy-grain-mismatch
           (get-in candidate
                   [:support-relations 0 :memories 0
                    :memory/body :observation])))
    (is (= 1 (get-in result [:audit :proposal-count])))
    (is (= 1 (get-in result [:audit :cross-domain-memory-count])))))

(deftest strict-dark-projection-requires-wm-witness-and-honours-blocks
  (let [pattern "p4ng/R9-independent-witness"
        mission "M-wm-tripwires"
        support (assoc witnessed-edge
                       :mission-id mission
                       :control-pattern-id pattern
                       :memory-ids ["e-self"])
        block (assoc support
                     :relation :blocked-by-control
                     :memory-ids ["e-block"])
        self-memory
        (assoc (wm-memory "e-self" pattern mission
                          {:hook "Controller reported its own success."})
               :memory/witness-status :self-asserted)
        witnessed-support (wm-memory
                           "e-self" pattern mission
                           {:hook "Independent check passed."})
        blocker (wm-memory
                 "e-block" pattern mission
                 {:hook "Tripwire found an unsafe transition."})]
    (testing "self-assertion and absent bodies cannot admit"
      (let [result (graph/candidate-projection
                    [pattern] [support] [self-memory])]
        (is (empty? (:candidates result)))
        (is (= 1 (get-in result
                         [:audit :ineligible-witnessed-edge-count])))))
    (testing "a concrete independently witnessed counterexample blocks"
      (let [result
            (graph/candidate-projection
             [pattern] [support block] [witnessed-support blocker])]
        (is (empty? (:candidates result)))
        (is (= "e-block"
               (get-in result
                       [:excluded-missions 0 :blocking-relations 0
                        :memories 0 :memory/id])))))))
