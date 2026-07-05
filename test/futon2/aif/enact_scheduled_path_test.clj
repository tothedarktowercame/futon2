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
     :eval {:delta-g -0.5 :g-grain :coverage}}))

(deftest scheduled-path-replays-escrow-for-matching-mission
  (binding [cl/*escrow-replay?* true]
    (let [deposits [(mock-deposit)]
          ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/scheduled-mock"
               :shown mock-cascade :F-free-energy 0.5 :G-rollout nil}
              mock-circumstance
              {:escrow-turn-fn (esc/escrow-turn-fn deposits)
               :prose-fn mock-prose-fn})]
      (is (= -0.5 (:delta-G ag)) "escrowed delta-G leg fires")
      (is (= :fold-escrow (:delta-G/source ag)))
      (is (= :pass (cl/preview-verdict ag))
          "verdict passes: dF 0.5 > 0 and dG -0.5 < 0"))))

(deftest scheduled-path-falls-through-when-no-deposit-matches
  (binding [cl/*escrow-replay?* true]
    (let [ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/no-deposit-here"
               :shown mock-cascade :F-free-energy 0.5 :G-rollout nil}
              {:mission "test-d/mission/no-deposit-here" :psi "WANT: x. HAVE: y."})]
      (is (nil? (:delta-G ag)) "no deposit => classical abstains => dG nil")
      (is (= :abstain-missing-leg (cl/preview-verdict ag))))))

(deftest scheduled-path-prompt-drift-abstains-pin-working
  (binding [cl/*escrow-replay?* true]
    (let [deposits [(mock-deposit)]
          ag (cl/act-gate-from-lane-entry
              {:mission "test-d/mission/scheduled-mock"
               :shown mock-cascade :F-free-energy 0.5 :G-rollout nil}
              {:mission "test-d/mission/scheduled-mock" :psi "WANT: DIFFERENT. HAVE: DIFFERENT."}
              {:escrow-turn-fn (esc/escrow-turn-fn deposits)
               :prose-fn mock-prose-fn})]
      (is (nil? (:delta-G ag))
          "prompt drift => sha mismatch => abstain, the pin WORKING")
      (is (not (contains? ag :fold-escrow))))))
