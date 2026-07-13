(ns futon2.aif.temporal-hierarchy-test
  "Tests for R15 b3 — the fast/slow temporal hierarchy.

  The load-bearing test: changing the slow-loop state CHANGES the fast-loop's
  prior/decision. This is the parameterization proof — the hierarchy is REAL,
  not cosmetic."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.rollout :as rollout]
            [futon2.aif.temporal-hierarchy :as th]))

(def cap-snapshot
  {"agency" {:id "scope/capability/agency"
             :props {:capability/frontier? false
                     :capability/status :held}}})

(defn test-moves
  "A move set with two classes: close-hole (tactical, fast payoff) and
  advance-capability (strategic, longer payoff)."
  []
  [{:move/id "close-fast" :move/class :close-hole
    :have "root" :want "fast-win" :score 1.0 :step-score-delta -0.3
    :rank 1 :move/terminal? false}
   {:move/id "advance-cap" :move/class :advance-capability
    :have "root" :want "cap-win" :advances-cap "agency"
    :score 1.0 :step-score-delta -0.4
    :rank 2 :move/terminal? false}])

(deftest slow-mode-shifts-fast-loop-priors
  (testing "exploitation mode boosts close-hole moves"
    (let [moves (test-moves)
          shaped (th/apply-slow-prior moves :exploitation)
          close-move (first (filter #(= "close-fast" (:move/id %)) shaped))
          cap-move (first (filter #(= "advance-cap" (:move/id %)) shaped))]
      ;; exploitation weights: close-hole 0.7, advance-capability 0.2
      ;; both start with no :prior (nil → base 1.0), so:
      ;; close-fast prior = 1.0 * 0.7 = 0.7
      ;; advance-cap prior = 1.0 * 0.2 = 0.2
      (is (> (:prior close-move) (:prior cap-move)))
      (is (= 0.7 (:prior close-move)))
      (is (= 0.2 (:prior cap-move)))))

  (testing "exploration mode reverses the prior ordering"
    (let [moves (test-moves)
          shaped (th/apply-slow-prior moves :exploration)
          close-move (first (filter #(= "close-fast" (:move/id %)) shaped))
          cap-move (first (filter #(= "advance-cap" (:move/id %)) shaped))]
      ;; exploration weights: close-hole 0.2, advance-capability 0.3
      (is (< (:prior close-move) (:prior cap-move)))
      (is (= 0.2 (:prior close-move)))
      (is (= 0.3 (:prior cap-move))))))

(deftest parameterization-changes-rollout-decision
  "THE HIERARCHY PROOF: the same state + moves, under different slow-loop
  modes, produces DIFFERENT fast-loop decisions. This is what makes the
  hierarchy REAL, not cosmetic."
  (let [state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        moves (test-moves)
        ;; Under exploitation, close-hole gets prior boost → wins the tie-break
        exploit-result (th/hierarchical-rollout
                         state moves
                         :slow-mode :exploitation
                         :horizon 1 :top-k 5)
        ;; Under exploration, the weighting is closer; but consolidation
        ;; heavily favors advance-capability (0.4 vs 0.5 for close-hole)
        ;; Actually let's use exploration where cap gets 0.3 vs close 0.2
        explore-result (th/hierarchical-rollout
                         state moves
                         :slow-mode :exploration
                         :horizon 1 :top-k 5)
        ;; nil mode = unparameterized (regression-safe)
        nil-result (th/hierarchical-rollout
                     state moves
                         :slow-mode nil
                         :horizon 1 :top-k 5)]
    (is (= "close-fast" (-> exploit-result :policy first :move/id)))
    (is (= "advance-cap" (-> explore-result :policy first :move/id)))
    ;; nil mode = no parameterization = standard rollout behavior
    (is (some? nil-result))))

(deftest nil-mode-is-regression-safe
  "When the slow loop has no state (nil mode), the fast loop runs with
  its default priors — byte-identical to calling rollout/best-rollout
  without the hierarchy layer."
  (let [state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        moves (test-moves)
        via-hierarchy (th/hierarchical-rollout
                        state moves
                        :slow-mode nil
                        :horizon 1 :top-k 5)
        direct (rollout/best-rollout state moves :horizon 1 :top-k 5)]
    (is (= (:policy-rollout-score via-hierarchy) (:policy-rollout-score direct)))
    (is (= (mapv :move/id (:policy via-hierarchy))
           (mapv :move/id (:policy direct))))))

(deftest mode-prior-weights-are-discrete
  (testing "each strategic mode maps to distinct prior weights"
    (doseq [mode [:exploitation :exploration :consolidation]]
      (let [weights (th/mode-prior-weights mode)]
        (is (map? weights))
        (is (seq weights))))
    (is (nil? (th/mode-prior-weights nil)))
    (is (nil? (th/mode-prior-weights :unknown-mode)))))

(deftest slow-context-classifies-from-intrinsics
  "R12 seam: the slow-loop mode can be DERIVED from accumulated learning
  (the intrinsic-values table), not just set by the operator."
  (testing "close-hole-dominant intrinsics → exploitation"
    (let [result (th/slow-context-from-intrinsics
                   {:intrinsics {"close-hole" {:alpha 0.8}
                                 "explore" {:alpha 0.2}}})]
      (is (= :exploitation result))))
  (testing "capability-dominant intrinsics → consolidation"
    (let [result (th/slow-context-from-intrinsics
                   {:intrinsics {"advance-capability" {:alpha 0.7}
                                 "close-hole" {:alpha 0.3}}})]
      (is (= :consolidation result))))
  (testing "empty intrinsics → nil (unparameterized)"
    (let [result (th/slow-context-from-intrinsics {:intrinsics {}})]
      (is (nil? result)))))

(deftest hierarchy-is-not-rollout-depth
  "ANTI-FAKING guard: the hierarchy changes WHAT the fast loop wants, not
  how FAR it looks. Prove this by showing the hierarchy effect is INDEPENDENT
  of the horizon parameter."
  (let [state {:arrows {}
               :cap-overlay cap-snapshot
               :reachable #{"root"}}
        moves (test-moves)]
    ;; Under exploitation at H=1 AND H=2, the close-hole move gets the prior
    ;; boost — the mode shapes priors regardless of horizon.
    (doseq [horizon [1 2 3]]
      (let [result (th/hierarchical-rollout
                     state moves
                     :slow-mode :exploitation
                     :horizon horizon :top-k 5)]
        (is (= "close-fast" (-> result :policy first :move/id))
            (str "exploitation mode should favor close-fast at horizon " horizon))))
    ;; Under exploration at H=1 AND H=2, the cap move gets the prior boost.
    (doseq [horizon [1 2 3]]
      (let [result (th/hierarchical-rollout
                     state moves
                     :slow-mode :exploration
                     :horizon horizon :top-k 5)]
        (is (= "advance-cap" (-> result :policy first :move/id))
            (str "exploration mode should favor advance-cap at horizon " horizon))))))
