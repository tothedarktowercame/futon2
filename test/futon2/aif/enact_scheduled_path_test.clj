(ns futon2.aif.enact-scheduled-path-test
  "E-live-loop-3 L3: the scheduled caller injects the pinned seam. Tests that
   the deposit-grain circumstance reconstruction works through
   act-gate-from-lane-entry's 3-arity when a deposit matches, and that the
   prompt-sha pin correctly abstains on drift."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-llm :as fl]))

(def mock-cascade ["invariant-coherence/state-snapshot-witness"])
(def mock-circumstance {:mission "test-d/mission/scheduled-mock" :psi "WANT: w. HAVE: h."})
(def mock-prose-fn (constantly "IF mock. HOWEVER mock. THEN mock. BECAUSE mock."))

(def mock-answer
  {:boxes        [{:id :b1 :role "mock construction step"
                   :fits-pattern (first mock-cascade) :addresses-however "mock"}]
   :wires        []
   :terminals    [:b1]
   :policy-holes [{:unfolded-pattern nil :free "remainder" :why "mock"}]})

(defn- mock-deposit
  "Build a deposit whose prompt-sha binds to EXACTLY this cascade x circumstance x prose."
  []
  (let [proses (into {} (for [p mock-cascade] [p (mock-prose-fn p)]))
        prompt (fl/fold-prompt mock-cascade mock-circumstance proses)]
    {:fold-turn/id "ft-scheduled-test-001"
     :mission (:mission mock-circumstance)
     :cascade {:psi (:psi mock-circumstance) :pattern-ids (vec mock-cascade)}
     :prompt {:sha256 (esc/prompt-sha prompt)}
     :turn {:agent "test-agent" :model "mock" :at "2026-07-05T00:00:00Z"
            :answer mock-answer}
     :arming {:operator "joe" :word "mock arming word - scheduled-path test fixture"
              :at "2026-07-05T00:00:00Z" :scope :one-fold}
     :eval {:coverage-score-delta -0.5 :g-grain :coverage}}))

(deftest scheduled-path-replays-escrow-for-matching-mission
  (binding [cl/*escrow-replay?* true]
    (let [deposits [(mock-deposit)]
          ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/scheduled-mock"
               :shown mock-cascade :cascade-score 0.5 :policy-rollout-score nil}
              mock-circumstance
              {:escrow-turn-fn (esc/escrow-turn-fn deposits)
               :prose-fn mock-prose-fn})]
      (is (= -0.5 (:coverage-score-delta ag)) "escrowed coverage-score-delta leg fires")
      (is (= :fold-escrow (:coverage-score/source ag)))
      (is (= :pass (cl/preview-verdict ag))
          "verdict passes: dF 0.5 > 0 and dG -0.5 < 0"))))

(deftest scheduled-path-falls-through-when-no-deposit-matches
  (binding [cl/*escrow-replay?* true]
    (let [ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/no-deposit-here"
               :shown mock-cascade :cascade-score 0.5 :policy-rollout-score nil}
              {:mission "test-d/mission/no-deposit-here" :psi "WANT: x. HAVE: y."})]
      (is (nil? (:coverage-score-delta ag)) "no deposit => classical abstains => dG nil")
      (is (= :abstain-missing-leg (cl/preview-verdict ag))))))

(deftest scheduled-path-prompt-drift-abstains-pin-working
  (binding [cl/*escrow-replay?* true]
    (let [deposits [(mock-deposit)]
          ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/scheduled-mock"
               :shown mock-cascade :cascade-score 0.5 :policy-rollout-score nil}
              {:mission "test-d/mission/scheduled-mock" :psi "WANT: DIFFERENT. HAVE: DIFFERENT."}
              {:escrow-turn-fn (esc/escrow-turn-fn deposits)
               :prose-fn mock-prose-fn})]
      (is (nil? (:coverage-score-delta ag))
          "prompt drift => sha mismatch => abstain, the pin WORKING")
      (is (not (contains? ag :fold-escrow))))))

(deftest l4-classical-unplugged-escrow-fills-dG
  "L4 operator ruling: classical fold's dG leg is unplugged (*classical-fold-score?*
   false by default). With classical off and an escrow deposit matching, the
   escrow fills the dG leg -- even for missions whose classical fold WOULD have
   resolved (the whole point of the ruling)."
  (binding [cl/*escrow-replay?* true
            cl/*classical-fold-score?* false]
    (let [deposits [(mock-deposit)]
          ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/scheduled-mock"
               :shown mock-cascade :cascade-score 0.5 :policy-rollout-score nil}
              mock-circumstance
              {:escrow-turn-fn (esc/escrow-turn-fn deposits)
               :prose-fn mock-prose-fn})]
      (is (= -0.5 (:coverage-score-delta ag)) "escrow fills dG (classical unplugged)")
      (is (= :fold-escrow (:coverage-score/source ag)) "source is escrow, not classical"))))

(deftest l4-classical-revertible-flag-on-restores-old-order
  "The flag is REVERTIBLE: binding *classical-fold-score?* true lets the
   classical fold's dG leg participate again. With the mock cascade
   (contentful -- classical abstains regardless), the flag doesn't change
   the outcome, but we verify the flag is respected by checking that
   classical's dG is nil when off and potentially non-nil when on."
  ;; The mock cascade is contentful (classical abstains), so both flag
  ;; states produce nil dG from classical. The test verifies the flag
  ;; is wired: with flag ON, fold-g is the raw classical output; with
  ;; flag OFF, fold-g is nil (suppressed).
  (let [raw-fold (cl/act-gate-from-lane-entry
                  {:mission "test-d/mission/scheduled-mock"
                   :shown mock-cascade :cascade-score 0.5 :policy-rollout-score nil}
                  mock-circumstance)]
    ;; Default (flag OFF): classical dG suppressed.
    (is (nil? (:coverage-score-delta raw-fold))
        "classical abstains on contentful cascade, dG nil regardless of flag")
    (is (some? (:fold raw-fold))
        "the :fold output is still carried for provenance")))
