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

;; ---------------------------------------------------------------------------
;; AC5 (Joe's 2026-09-02 ruling on C130 §5): the validated rollout step
;; producer. Before this row `renormalize-priors` read `(or (:score m) 0.0)`,
;; so a move nobody scored got `exp(0.0) = 1.0` — the same branching weight as
;; a move that scored exactly zero.
;; ---------------------------------------------------------------------------

(def ^:private ac5-state
  {:arrows {} :cap-overlay {} :reachable #{"root"}})

(defn- ac5-move
  "A reachable move with no :prior, so the softmax(:score) fallback — the
   branch that held the fabricated zero — is the branch under test."
  [id & {:as extra}]
  (merge {:move/id id :move/class :close-hole
          :have "root" :want (str "w-" id)
          :rank 1 :move/terminal? false}
         extra))

(deftest ac5-unsupplied-score-is-unscored-not-zero
  ;; ABSENT: the key is not there at all.
  (let [record (rollout/move-score-record (ac5-move "u"))]
    (is (= :unscored (:status record)))
    (is (= :score-not-supplied (:reason record)))
    (is (not (contains? record :value))
        "an unscored move must carry no numeric value at all")
    (is (= :score (get-in record [:absent :field])))
    (is (false? (get-in record [:absent :key-present?])))
    (is (= :rollout-move-score/v1 (:producer-contract record))))
  ;; ABSENT: the key is there and nil. Same status, and the record says which.
  (let [record (rollout/move-score-record (ac5-move "n" :score nil))]
    (is (= :unscored (:status record)))
    (is (true? (get-in record [:absent :key-present?])))))

(deftest ac5-measured-zero-is-scored
  ;; THE CONTROL the row turns on: same number, different provenance. A move
  ;; that scored 0.0 is scored; a move nobody scored is not, and the old
  ;; expression could not tell them apart because it produced 0.0 for both.
  (let [measured (rollout/move-score-record (ac5-move "z" :score 0.0))
        unsupplied (rollout/move-score-record (ac5-move "u"))]
    (is (= :scored (:status measured)))
    (is (= 0.0 (:value measured)))
    (is (= :unscored (:status unsupplied)))
    (is (not= (:status measured) (:status unsupplied)))
    ;; and the pre-AC5 expression collapsed exactly this distinction
    (is (= (Math/exp (double (or (:score (ac5-move "z" :score 0.0)) 0.0)))
           (Math/exp (double (or (:score (ac5-move "u")) 0.0)))))))

(deftest ac5-malformed-score-is-refused-loudly
  ;; MALFORMED: present but not a finite number. Each is refused, not omitted,
  ;; and :offending names the field and the value it was given.
  (doseq [bad ["0.4" :zero true [] {} Double/NaN Double/POSITIVE_INFINITY
               Double/NEGATIVE_INFINITY]]
    (let [record (rollout/move-score-record (ac5-move "m" :score bad))]
      (is (= :refused (:status record)) (str "score " (pr-str bad)))
      (is (= :malformed-move-score (:reason record)))
      (is (not (contains? record :value)))
      (is (= :score (get-in record [:offending :field])))
      (is (= bad (get-in record [:offending :value])))))
  ;; The other numeric field the same expression reads. `(double "x")` used to
  ;; throw out of have-prior?; it is now a named refusal.
  (let [record (rollout/move-score-record (ac5-move "p" :score 1.0 :prior "x"))]
    (is (= :refused (:status record)))
    (is (= :prior (get-in record [:offending :field]))))
  ;; Not a map at all.
  (let [record (rollout/move-score-record "not-a-move")]
    (is (= :refused (:status record)))
    (is (= :malformed-move-record (:reason record)))))

(deftest ac5-separation-of-absent-and-malformed
  ;; nil and "x" in the same field give different statuses and different
  ;; reasons -- the AC1 boundary, restated at this site.
  (let [absent (rollout/move-score-record (ac5-move "a" :score nil))
        refused (rollout/move-score-record (ac5-move "a" :score "x"))]
    (is (= :unscored (:status absent)))
    (is (= :refused (:status refused)))
    (is (not= (:reason absent) (:reason refused)))))

(deftest ac5-unscored-move-cannot-enter-ranking-when-score-is-required
  ;; THE DEFECT, MEASURED BOTH DIRECTIONS. With no priors the weights come from
  ;; softmax(:score), so the score IS required: the unscored move used to be
  ;; weighted exp(0.0)=1.0 and now does not enter the population at all.
  (let [moves [(ac5-move "u") (ac5-move "s" :score 0.0)]
        {:keys [population score-required? move-score-events]}
        (rollout/validated-priors moves)]
    (is (true? score-required?))
    (is (= ["s"] (mapv :move/id population)))
    (is (= 1.0 (:prior (first population)))
        "the surviving move takes the whole renormalized mass")
    (is (= ["u"] (mapv :move/id move-score-events)))
    (is (true? (:excluded-from-ranking? (first move-score-events))))
    ;; and the same population through the ranking entry point
    (is (= ["s"] (mapv :move/id (rollout/ranked-survivors ac5-state moves :top-k 5))))))

(deftest ac5-score-is-not-required-on-the-prior-path
  ;; REQUIREMENT CONDITION, the AC4 discipline at this site: when every move
  ;; carries a positive finite :prior the softmax fallback is not taken, no
  ;; :score is read, and an unscored move is not excluded -- but its record
  ;; still reports the absence, with :score-required? false.
  (let [moves [(ac5-move "u" :prior 0.25) (ac5-move "s" :score 3.0 :prior 0.75)]
        {:keys [population score-required? move-score-events]}
        (rollout/validated-priors moves)]
    (is (false? score-required?))
    (is (= #{"u" "s"} (set (mapv :move/id population))))
    (is (= ["u"] (mapv :move/id move-score-events)))
    (is (= :unscored (:status (first move-score-events))))
    (is (false? (:score-required? (first move-score-events))))
    (is (not (contains? (first move-score-events) :excluded-from-ranking?)))))

(deftest ac5-fully-scored-population-is-byte-identical
  ;; The regression floor: on a population where every move carries a finite
  ;; score the renormalization is unchanged and NO events key is written --
  ;; present-only, as in AC1-AC4. This is the case the real v2 move-sets are
  ;; in (every move there carries both a finite :score and a positive :prior).
  (let [moves [(ac5-move "a" :score 1.0) (ac5-move "b" :score 2.0)]
        {:keys [population move-score-events]} (rollout/validated-priors moves)
        priors (mapv :prior population)]
    (is (empty? move-score-events))
    (is (= 2 (count population)))
    (is (< (Math/abs (- 1.0 (reduce + 0.0 priors))) 1.0e-12))
    ;; softmax(1,2) -- the pre-AC5 weights, unchanged
    (is (< (Math/abs (- (/ (Math/exp 1.0) (+ (Math/exp 1.0) (Math/exp 2.0)))
                        (first priors)))
           1.0e-12))
    ;; and the rollout result carries no key at all
    (is (not (contains? (rollout/best-rollout ac5-state moves :depth 1 :top-k 5)
                        :move-score-events)))))

(deftest ac5-records-ride-out-on-the-rollout-result
  ;; SELF-REPAIR CONDITION: the records have to LEAVE the search, or a refusal
  ;; is just a silently smaller candidate set. best-rollout carries them
  ;; present-only, which is what cascade-lane projects onto its entry and
  ;; close-loop! persists as :act-gate-verdicts.
  (let [moves [(ac5-move "u") (ac5-move "bad" :score "x") (ac5-move "s" :score 1.0)]
        best (rollout/best-rollout ac5-state moves :depth 1 :top-k 5)
        events (:move-score-events best)]
    (is (= 2 (count events)))
    (is (= #{:unscored :refused} (set (map :status events))))
    (is (= ["s"] (mapv :move/id (:policy best)))))
  ;; The projection is present-only over a record collection.
  (is (= [] (rollout/move-score-events
             [(rollout/move-score-record (ac5-move "s" :score 1.0))]))))

(deftest ac5-exclusion-can-empty-the-candidate-set
  ;; NOT DONE HERE, asserted so the boundary is visible rather than assumed:
  ;; when every move is unscored the ranking population is empty and the
  ;; expansion stops with the prefix it had -- the SAME shape it already had
  ;; for a state with no reachable moves. Telling those two apart is AC6's
  ;; refuse floor ("refuse when exclusion empties the candidate set"), and it
  ;; is deliberately not implemented at this row.
  (let [moves [(ac5-move "u") (ac5-move "v")]
        best (rollout/best-rollout ac5-state moves :depth 1 :top-k 5)]
    (is (empty? (rollout/ranked-survivors ac5-state moves :top-k 5)))
    (is (= [] (:policy best)))
    (is (= 2 (count (:move-score-events best)))
        "the records still ride out, so the empty set is explicable")))
