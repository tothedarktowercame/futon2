(ns futon2.aif.rollout-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.rollout :as rollout]
            [meme.step :as meme-step]))

(def cap-snapshot
  {"agency" {:id "scope/capability/agency"
             :props {:capability/frontier? false
                     :capability/status :held}}
   "ai-passes-prelims" {:id "scope/capability/ai-passes-prelims"
                        :props {:capability/frontier? true
                                :capability/status :held}}})

(deftest shared-step-routes-ordinary-and-frontier-caps
  (let [ordinary-state {:arrows {["h" "w"] {:have "h" :want "w"
                                            :status :open
                                            :advances-cap "agency"}}
                        :cap-overlay cap-snapshot
                        :reachable #{"h"}}
        frontier-state {:arrows {["h" "w"] {:have "h" :want "w"
                                            :status :open
                                            :advances-cap "ai-passes-prelims"}}
                        :cap-overlay cap-snapshot
                        :reachable #{"h"}}
        ordinary (meme-step/step ordinary-state {:have "h" :want "w"
                                                 :advances-cap "agency"})
        frontier (meme-step/step frontier-state {:have "h" :want "w"
                                                 :advances-cap "ai-passes-prelims"})]
    (is (= :constructed (get-in ordinary [:arrows ["h" "w"] :status])))
    (is (= :satisfied (get-in ordinary [:cap-overlay "agency" :props :capability/status])))
    (is (= :claimed (get-in frontier [:cap-overlay "ai-passes-prelims" :props :capability/status])))))

(deftest rollout-search-unlocks-second-step
  (let [moves [{:move/id "a" :move/class :close-hole
                :have "root" :want "bridge" :score 1.0 :step-score-delta -0.1
                :rank 1 :move/terminal? false}
               {:move/id "b" :move/class :advance-capability
                :have "bridge" :want "goal" :advances-cap "agency"
                :score 3.0 :step-score-delta -1.0 :rank 2 :move/terminal? false}
               {:move/id "greedy" :move/class :close-hole
                :have "root" :want "small" :score 2.0 :step-score-delta -0.2
                :rank 3 :move/terminal? false}]
        state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        greedy (rollout/greedy-one-step state moves :top-k 3)
        best (rollout/best-rollout state moves :depth 2 :top-k 3 :gamma 0.9)]
    (is (= ["greedy"] (mapv :move/id (:policy greedy))))
    (is (= ["a" "b"] (mapv :move/id (:policy best))))
    (is (< (:policy-rollout-score best) (:policy-rollout-score greedy)))))

(deftest horizon-h-unlocks-delayed-temporal-payoff
  (let [moves [{:move/id "a" :move/class :close-hole
                :have "root" :want "bridge" :score 3.0 :step-score-delta -0.05
                :rank 1 :move/terminal? false}
               {:move/id "b" :move/class :close-hole
                :have "bridge" :want "ledge" :score 3.0 :step-score-delta -0.05
                :rank 2 :move/terminal? false}
               {:move/id "c" :move/class :advance-capability
                :have "ledge" :want "goal" :advances-cap "agency"
                :score 3.0 :step-score-delta -10.0 :rank 3 :move/terminal? false}
               {:move/id "greedy" :move/class :close-hole
                :have "root" :want "small" :score 2.0 :step-score-delta -0.5
                :rank 4 :move/terminal? false}]
        state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        depth-2 (rollout/best-rollout state moves :horizon 2 :top-k 4 :temporal-discount 0.9)
        horizon-3 (rollout/best-rollout state moves :horizon 3 :top-k 4 :temporal-discount 0.9)
        legacy-alias (rollout/best-rollout state moves :depth 3 :top-k 4 :gamma 0.5)
        horizon-alias (rollout/best-rollout state moves :horizon 3 :top-k 4 :temporal-discount 0.5)]
    ;; H=2 cannot see the delayed third-step payoff, so the local shortcut wins first.
    (is (= "greedy" (-> depth-2 :policy first :move/id)))
    ;; H=3 sees the delayed payoff under the same flat rollout model.
    (is (= ["a" "b" "c"] (mapv :move/id (:policy horizon-3))))
    ;; The R15 names are API aliases for the existing R13 depth/gamma mechanics.
    (is (= (:policy-rollout-score legacy-alias) (:policy-rollout-score horizon-alias)))
    (is (not= (:policy-rollout-score horizon-3) (:policy-rollout-score horizon-alias)))))

(deftest root-seed-ignites-phase-chain
  ;; claude-3's hypergraph-operator example (v2 scope-grain seam): one
  ;; mission-entity seed -> the full depth-5 detached-phase chain unrolls.
  ;; Proves the consumer half string-exact BEFORE the producer lands.
  (let [mission "futon5a-d/mission/hypergraph-operator"
        chain [["derive"      mission                          "hypergraph-operator/derive"]
               ["argue"       "hypergraph-operator/derive"     "hypergraph-operator/argue"]
               ["verify"      "hypergraph-operator/argue"      "hypergraph-operator/verify"]
               ["document"    "hypergraph-operator/verify"     "hypergraph-operator/document"]
               ["instantiate" "hypergraph-operator/document"   "hypergraph-operator/instantiate"]]
        moves (vec (map-indexed
                    (fn [i [id have want]]
                      {:move/id id :move/class :close-hole
                       :have have :want want :score 1.0 :step-score-delta -0.1
                       :rank (inc i) :move/terminal? false})
                    chain))
        seeded (rollout/seed-roots {:arrows {} :cap-overlay {} :reachable #{}} moves)
        unseeded {:arrows {} :cap-overlay {} :reachable #{}}
        best (rollout/best-rollout seeded moves :depth 5 :top-k 3 :gamma 0.9)]
    ;; the lone axiom is the mission entity — not any phase scope
    (is (= #{mission} (rollout/mission-roots moves)))
    ;; every root is a known class (no producer drift)
    (is (empty? (rollout/drift-roots moves)))
    ;; no seed -> nothing ignites (missions aren't constructed)
    (is (empty? (rollout/reachable-moves unseeded moves)))
    ;; seeded -> the depth-5 chain unrolls from one ignition
    (is (= ["derive" "argue" "verify" "document" "instantiate"]
           (mapv :move/id (:policy best))))))

(deftest root-taxonomy-seeds-axioms-not-islands
  ;; claude-3's 3-way root taxonomy: mission entity + claimed capability = SEED
  ;; (axioms ignite at t=0); conjectural foothold = intended-DARK island (stays
  ;; unreachable until a foothold is constructed — that darkness is the signal).
  (let [mission "futon3c-d/mission/war-machine"
        claimed-cap "scope/capability/wm-steps-forward-guardrailed"
        island "scope/conjectural/kit-outbox-foothold"
        moves [;; mission chain root (close-hole)
               {:move/id "phase" :move/class :close-hole
                :have mission :want "war-machine/derive"
                :score 1.0 :step-score-delta -0.1 :rank 1 :move/terminal? false}
               ;; reachable summit: :have = a claimed cap (achieved axiom)
               {:move/id "summit" :move/class :advance-capability
                :have claimed-cap :want "scope/capability/wm-overnight-unsupervised"
                :advances-cap "wm-overnight-unsupervised"
                :score 2.0 :step-score-delta -1.0 :rank 2 :move/terminal? false}
               ;; island: :have = a conjectural foothold, intended dark
               {:move/id "island" :move/class :advance-capability
                :have island :want "scope/capability/kit-outbox"
                :advances-cap "kit-outbox"
                :score 2.0 :step-score-delta -1.0 :rank 3 :move/terminal? false}]
        seeded (rollout/seed-roots {:arrows {} :cap-overlay {} :reachable #{}} moves)
        reachable-ids (set (mapv :move/id (rollout/reachable-moves seeded moves)))]
    ;; the three classes partition cleanly, with no drift
    (is (= #{mission} (rollout/mission-roots moves)))
    (is (= #{claimed-cap} (rollout/capability-roots moves)))
    (is (= #{island} (rollout/conjectural-roots moves)))
    (is (empty? (rollout/drift-roots moves)))
    ;; seeded axioms ignite; the conjectural island stays dark
    (is (contains? reachable-ids "phase"))
    (is (contains? reachable-ids "summit"))
    (is (not (contains? reachable-ids "island")))))

(deftest real-stub-loads-and-masks-reachable-moves
  (let [move-set (rollout/load-move-set)
        ms (rollout/moves move-set)
        state {:reachable #{"scope/interim-director-proxy-metric-inventory/pattern#open"}
               :arrows {}
               :cap-overlay {}}]
    (is (= 19 (count ms)))
    (is (every? :move/id ms))
    (is (= ["scope/interim-director-proxy-metric-inventory/pattern#open->scope/interim-director-proxy-metric-inventory/pattern#closed"]
           (mapv :move/id (rollout/reachable-moves state ms))))))
