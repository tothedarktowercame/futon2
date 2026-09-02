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
  ;; The boundary AC5 left open, now closed by AC6's refuse floor. When every
  ;; move is unscored the ranking population is empty; before AC6 the expansion
  ;; stopped with the prefix it had -- the SAME shape it already had for a state
  ;; with no reachable moves -- and best-rollout returned a real-looking
  ;; :policy-rollout-score of 0.0 for the empty policy. Now the rollout refuses.
  (let [moves [(ac5-move "u") (ac5-move "v")]
        best (rollout/best-rollout ac5-state moves :depth 1 :top-k 5)]
    (is (empty? (rollout/ranked-survivors ac5-state moves :top-k 5)))
    (is (= :candidate-set-emptied-by-exclusion
           (get-in best [:rollout/refusal :reason])))
    (is (not (contains? best :policy-rollout-score)))
    (is (= 2 (count (:move-score-events best)))
        "the records still ride out, so the empty set is explicable")))

;; ---------------------------------------------------------------------------
;; AC6 (Joe's 2026-09-02 ruling on C130 §6, futon2 2f34c26) -- the rollout COST
;; leg and the refuse floor under exclude-and-continue. Before this row
;; `move-cost` read `(or (:step-score-delta m) (- (double (or (:score m) 0.0))))`,
;; so a move nobody costed contributed exactly the same 0.0 to
;; `project-policy`'s accumulator as a truncated state, an already-satisfied
;; capability, and a move that genuinely scored zero.
;; ---------------------------------------------------------------------------

(def ^:private ac6-state
  {:arrows {} :cap-overlay {} :reachable #{"root"}})

(defn- ac6-move
  "A reachable move carrying a positive :prior, so AC5's score validation is on
   the prior path and does NOT exclude anything -- whatever this row's tests
   observe is AC6's cost leg, not AC5's."
  [id & {:as extra}]
  (merge {:move/id id :move/class :close-hole
          :have "root" :want (str "w-" id)
          :rank 1 :move/terminal? false :prior 0.5}
         extra))

(deftest ac6-uncosted-move-is-not-zero-cost
  ;; ABSENT: neither field supplied. Both keys are named, with which of them
  ;; was present, so a nil-valued key and a missing key stay apart.
  (let [record (rollout/move-cost-record ac6-state (ac6-move "u"))]
    (is (= :uncosted (:status record)))
    (is (= :cost-not-supplied (:reason record)))
    (is (not (contains? record :value))
        "an uncosted move must carry no numeric cost at all")
    (is (= [:step-score-delta :score] (mapv :field (:absent record))))
    (is (= [false false] (mapv :key-present? (:absent record))))
    (is (= :rollout-move-cost/v1 (:producer-contract record)))
    (is (nil? (rollout/move-cost ac6-state (ac6-move "u")))
        "the numeric projection is nil, not 0.0"))
  ;; ABSENT: keys present and nil. Same status, and the record says which.
  (let [record (rollout/move-cost-record
                ac6-state (ac6-move "n" :step-score-delta nil :score nil))]
    (is (= :uncosted (:status record)))
    (is (= [true true] (mapv :key-present? (:absent record))))))

(deftest ac6-measured-zero-is-costed
  ;; THE CONTROL the row turns on: same number, four different provenances, and
  ;; the old expression produced the identical 0.0 for all four.
  (let [delta-zero (rollout/move-cost-record ac6-state (ac6-move "d" :step-score-delta 0.0))
        score-zero (rollout/move-cost-record ac6-state (ac6-move "s" :score 0.0))
        truncated (rollout/move-cost-record (assoc ac6-state :truncated? true) (ac6-move "u"))
        satisfied (rollout/move-cost-record
                   (assoc ac6-state :cap-overlay
                          {"c" {:props {:capability/status :satisfied}}})
                   (ac6-move "c" :advances-cap "c"))
        unsupplied (rollout/move-cost-record ac6-state (ac6-move "u"))]
    (is (= [:present :present :present :present]
           (mapv :status [delta-zero score-zero truncated satisfied])))
    (is (= [0.0 0.0 0.0 0.0] (mapv :value [delta-zero score-zero truncated satisfied])))
    (is (= [:step-score-delta :negated-score :truncated-state :satisfied-capability]
           (mapv :basis [delta-zero score-zero truncated satisfied]))
        "four zeros, four bases -- the distinction the bare 0.0 destroyed")
    (is (= :uncosted (:status unsupplied)))
    ;; and the pre-AC6 expression collapsed exactly this distinction
    (let [pre (fn [m] (double (or (:step-score-delta m) (- (double (or (:score m) 0.0))))))]
      (is (= 0.0 (Math/abs (pre (ac6-move "d" :step-score-delta 0.0)))))
      (is (= 0.0 (Math/abs (pre (ac6-move "s" :score 0.0)))))
      (is (= 0.0 (Math/abs (pre (ac6-move "u"))))))))

(deftest ac6-delta-is-preferred-and-score-is-negated
  ;; The fallback chain itself, unchanged: the delta wins when both are there,
  ;; and the score enters negated (a benefit is negative cost).
  (is (= -3.0 (:value (rollout/move-cost-record ac6-state (ac6-move "a" :step-score-delta -3.0 :score 9.0)))))
  (is (= :step-score-delta (:basis (rollout/move-cost-record ac6-state (ac6-move "a" :step-score-delta -3.0 :score 9.0)))))
  (is (= -2.0 (:value (rollout/move-cost-record ac6-state (ac6-move "b" :score 2.0)))))
  (is (= :negated-score (:basis (rollout/move-cost-record ac6-state (ac6-move "b" :score 2.0))))))

(deftest ac6-malformed-cost-is-refused-loudly
  ;; MALFORMED: present but not a finite number, in either field. Refused, not
  ;; omitted, with :offending naming the field and the value it was given.
  (doseq [bad ["0.4" :zero true [] {} Double/NaN Double/POSITIVE_INFINITY
               Double/NEGATIVE_INFINITY]]
    (let [d (rollout/move-cost-record ac6-state (ac6-move "m" :step-score-delta bad))
          s (rollout/move-cost-record ac6-state (ac6-move "m" :score bad))]
      (is (= :refused (:status d)) (str "delta " (pr-str bad)))
      (is (= :malformed-move-cost (:reason d)))
      (is (= :step-score-delta (get-in d [:offending :field])))
      (is (= bad (get-in d [:offending :value])))
      (is (not (contains? d :value)))
      (is (= :refused (:status s)) (str "score " (pr-str bad)))
      (is (= :score (get-in s [:offending :field])))))
  ;; A malformed delta is refused rather than silently falling through to a
  ;; perfectly good :score -- absence omits, malformation refuses.
  (let [record (rollout/move-cost-record ac6-state (ac6-move "f" :step-score-delta "x" :score 1.0))]
    (is (= :refused (:status record)))
    (is (= :step-score-delta (get-in record [:offending :field]))))
  ;; Not a map at all, and loud even in a truncated state, where the old
  ;; expression short-circuited to 0.0 without ever reading the move.
  (is (= :malformed-move-record
         (:reason (rollout/move-cost-record ac6-state "not-a-move"))))
  (is (= :refused
         (:status (rollout/move-cost-record (assoc ac6-state :truncated? true) "not-a-move")))))

(deftest ac6-separation-of-uncosted-and-malformed
  ;; nil and "x" in the same field give different statuses and different
  ;; reasons -- the AC1 boundary, restated at the cost leg.
  (let [absent (rollout/move-cost-record ac6-state (ac6-move "a" :step-score-delta nil))
        refused (rollout/move-cost-record ac6-state (ac6-move "a" :step-score-delta "x"))]
    (is (= :uncosted (:status absent)))
    (is (= :refused (:status refused)))
    (is (not= (:reason absent) (:reason refused)))))

(deftest ac6-uncostable-move-is-excluded-and-the-search-continues
  ;; EXCLUDE-AND-CONTINUE, the ruling's first half. Both moves are on the
  ;; sharpened-:prior path so AC5 excludes nothing; the uncostable one is
  ;; dropped by AC6 and the costable one continues -- and takes the whole
  ;; renormalized prior mass, because the branching weights of a node are
  ;; rescaled over its actual candidates.
  (let [moves [(ac6-move "u") (ac6-move "s" :score 1.0)]
        {:keys [survivors move-score-events move-cost-events candidate-set-emptied?]}
        (rollout/ranked-survivors-with-records ac6-state moves :top-k 5)]
    (is (= ["s"] (mapv :move/id survivors)))
    (is (= 1.0 (:prior (first survivors))))
    ;; AC5 still REPORTS the absent score (its record rides on every
    ;; validation), but on the prior path it excludes nothing -- so the
    ;; exclusion under test here is AC6's.
    (is (= [false] (mapv :score-required? move-score-events)))
    (is (not-any? :excluded-from-ranking? move-score-events)
        "AC5 is on the prior path here and excludes nothing")
    (is (= ["u"] (mapv :move/id move-cost-events)))
    (is (true? (:excluded-from-rollout? (first move-cost-events))))
    (is (nil? candidate-set-emptied?))
    ;; and the search runs to a policy over the survivor
    (let [best (rollout/best-rollout ac6-state moves :depth 1 :top-k 5)]
      (is (= ["s"] (mapv :move/id (:policy best))))
      (is (= -1.0 (:policy-rollout-score best)))
      (is (= ["u"] (mapv :move/id (:move-cost-events best)))
          "the record rides out, or the exclusion is a silently smaller set"))))

(deftest ac6-refuse-floor-when-exclusion-empties-the-candidate-set
  ;; REFUSE FLOOR, condition one. Two reachable moves, neither costable: the
  ;; candidate set is emptied BY EXCLUSION, which is not the same as a node
  ;; with nothing reachable, and the rollout refuses instead of returning the
  ;; prefix it happened to have.
  (let [moves [(ac6-move "u") (ac6-move "v")]
        {:keys [survivors candidate-set-emptied? reachable-count]}
        (rollout/ranked-survivors-with-records ac6-state moves :top-k 5)
        best (rollout/best-rollout ac6-state moves :depth 1 :top-k 5)
        refusal (:rollout/refusal best)]
    (is (empty? survivors))
    (is (true? candidate-set-emptied?))
    (is (= 2 reachable-count))
    (is (= :rollout-refusal/v1 (:producer-contract refusal)))
    (is (= :candidate-set-emptied-by-exclusion (:reason refusal)))
    (is (= #{"u" "v"} (set (:excluded refusal))))
    (is (= [[]] (mapv :prefix (:emptied-nodes refusal))) "refused at the root")
    (is (not (contains? best :policy-rollout-score))
        "a refused rollout produces NO number -- the ΔG leg reads nil and the gate abstains")
    (is (not (contains? best :policy))))
  ;; THE OTHER DIRECTION: nothing reachable at all is a genuine terminal node,
  ;; not an exclusion, and does not refuse.
  (let [best (rollout/best-rollout {:arrows {} :cap-overlay {} :reachable #{}}
                                   [(ac6-move "u" :have "elsewhere")]
                                   :depth 1 :top-k 5)]
    (is (nil? (:rollout/refusal best)))
    (is (= 0.0 (:policy-rollout-score best)))))

(deftest ac6-refuse-floor-when-the-rollout-authorizes
  ;; REFUSE FLOOR, condition two. The SAME population that a diagnosing rollout
  ;; runs on (one uncostable move excluded, one survivor) refuses when the
  ;; caller declares it is choosing an action: a reduced candidate set changes
  ;; which action is chosen.
  (let [moves [(ac6-move "u") (ac6-move "s" :score 1.0)]
        diagnosing (rollout/best-rollout ac6-state moves :depth 1 :top-k 5 :authority :diagnose)
        authorizing (rollout/best-rollout ac6-state moves :depth 1 :top-k 5 :authority :authorize)]
    (is (= -1.0 (:policy-rollout-score diagnosing)))
    (is (nil? (:rollout/refusal diagnosing)))
    (is (= :exclusion-under-authorizing-rollout
           (get-in authorizing [:rollout/refusal :reason])))
    (is (true? (get-in authorizing [:rollout/refusal :authority-declared?])))
    (is (not (contains? authorizing :policy-rollout-score))))
  ;; An authorizing rollout with NOTHING excluded stands: the floor is about
  ;; exclusion, not about authority.
  (let [moves [(ac6-move "a" :score 1.0) (ac6-move "b" :score 2.0)]
        best (rollout/best-rollout ac6-state moves :depth 1 :top-k 5 :authority :authorize)]
    (is (nil? (:rollout/refusal best)))
    (is (number? (:policy-rollout-score best))))
  ;; An undeclared authority is reported as undeclared -- a default is not a
  ;; claim, so AC8's harvester can see the difference.
  (let [moves [(ac6-move "u") (ac6-move "v")]
        refusal (:rollout/refusal (rollout/best-rollout ac6-state moves :depth 1 :top-k 5))]
    (is (= :diagnose (:authority refusal)))
    (is (false? (:authority-declared? refusal))))
  ;; An authority that is neither is loud, not silently coerced to the default.
  (is (thrown? clojure.lang.ExceptionInfo
               (rollout/best-rollout ac6-state [(ac6-move "a" :score 1.0)]
                                     :depth 1 :top-k 5 :authority :whatever))))

(deftest ac6-project-policy-refuses-an-uncostable-step
  ;; A policy is an ORDERED plan: a step that cannot be costed cannot be
  ;; dropped with the rest kept, so the projection refuses rather than
  ;; returning a total one term short. The search never reaches this branch --
  ;; it is for a caller-supplied policy.
  (let [result (rollout/project-policy ac6-state [(ac6-move "s" :score 1.0) (ac6-move "u")])]
    (is (= :uncostable-move-in-policy (get-in result [:rollout/refusal :reason])))
    (is (= "u" (get-in result [:rollout/refusal :move/id])))
    (is (= 1 (get-in result [:rollout/refusal :step])) "refused at the second step")
    (is (not (contains? result :policy-rollout-score))))
  ;; select-policy returns control instead of sorting over a missing number.
  (let [decision (rollout/select-policy
                  (rollout/score-policies ac6-state [(ac6-move "u") (ac6-move "v")] :depth 1))]
    (is (= :refuse (:decision decision)))
    (is (= :candidate-set-emptied-by-exclusion (:reason decision)))
    (is (nil? (:selected decision)))))

(deftest ac6-fully-costable-population-is-unchanged
  ;; The regression floor: on a population where every move can be costed the
  ;; rollout is what it was, NO events key and no refusal key are written, and
  ;; the priors are not rescaled. This is the case both real v2 move-sets are
  ;; in (every move carries a finite :score, and :step-score-delta is absent on
  ;; 19/19 and 55/55, so :negated-score is the live basis there).
  (let [moves [(ac6-move "a" :score 1.0 :prior 0.25) (ac6-move "b" :score 2.0 :prior 0.75)]
        {:keys [survivors move-cost-events]}
        (rollout/ranked-survivors-with-records ac6-state moves :top-k 5)
        best (rollout/best-rollout ac6-state moves :depth 1 :top-k 5)]
    (is (empty? move-cost-events))
    (is (= [0.75 0.25] (mapv :prior survivors))
        "priors are left bit-for-bit alone when nothing was excluded")
    (is (not (contains? best :move-cost-events)))
    (is (not (contains? best :rollout/refusal)))
    (is (= -2.0 (:policy-rollout-score best))))
  ;; and the present-only projection over a clean record collection
  (is (= [] (rollout/move-cost-events
             [(rollout/move-cost-record ac6-state (ac6-move "s" :score 1.0))]))))
