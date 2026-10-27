(ns futon2.aif.meta-acceptance-injury-2aef7e0c-test
  "Acceptance pin for injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246
   (tick-run-record-2026-10-27-39eb74f5-9db6-4f1d-8d28-03694c6f1fba.edn,
   sha256 a543d0066d7384fd7b281e6ee6c39936ff8b1797d39d9eb0f0c3b4cf70282690).

   The injury: at :author-wait on target M-recommendation-bindings the author
   (zai-2, job invoke-1793108667170-5478-8c4ff130) refused with no typed
   reason and an Agency-extracted :artifact-ref (09a7cb8d...) that is a
   sha-shaped string naming a commit that exists in NO primary repository,
   while the author's durable feature card states the want is already
   satisfied and carries :proof-ref naming a commit that DOES exist. The
   injured classifier treats any sha-shaped artifact-ref as authoritative
   even when it exists nowhere, so the card's real :proof-ref is never
   grounded and the close falls to :invalid-author-refusal.

   The repaired contract this test pins: when the sha-shaped artifact-ref
   names no existing commit, the card's :proof-ref sha is tried as the
   grounded commit; a card-stated already-satisfied want grounded by an
   EXISTING commit closes :already-satisfied (PROOF-2b family), and
   throw-if-author-refused! closes it with that outcome, routed by
   terminal-receipt as exactly one :failure receipt.

   No stubs: the commit-existence check is the real commit-exists-in?
   (git cat-file against /home/joe/code/futon2), and
   :strategic-selection-invoke-fn is the production injection seam named in
   strategic-selection!'s own docstring, replaying the recorded
   :selection-event shape from the pinned run record (one attempt, no
   retries, no network)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

(def ^:const injury-id "injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246")

(def ^:const target "M-recommendation-bindings")

;; The Agency-extracted ref from the injured job: sha-shaped, but it names
;; no commit in any primary repository (verified by git cat-file at proposal
;; time in /home/joe/code/futon2 and /home/joe/code/futon7).
(def ^:const ungrounded-artifact-ref "09a7cb8d3468031ed3fa2beb6db21c602617a572")

;; A commit that exists in /home/joe/code/futon2 — the last commit touching
;; full_loop_runner.clj, fixed at proposal time; the card's :proof-ref.
(def ^:const grounded-sha "d8a61c13ac678a2875126ba6c669ae4a0ce1bf03")

(def author-job
  "The injured job's shape: prose summary, a bare REFUSE marker with no
   reason, a feature card stating the want already holds with :proof-ref
   naming the existing commit, and the extracted artifact-ref naming a
   commit that exists nowhere."
  {:agent-id "zai-2"
   :caller "wm-full-loop"
   :job-id "invoke-1793108667170-5478-8c4ff130"
   :artifact-ref ungrounded-artifact-ref
   :result-summary
   "All twelve reviewed completion checks are satisfied mechanically by the mission header itself at base head `09a7cb8d`"
   :result
   (str
     "All twelve reviewed completion checks are satisfied mechanically by the mission header itself at base head `09a7cb8d` — title goal span, dated DERIVE status, owner handoff line, and all eight cross-ref lines are present.\n"
     "FULL_LOOP_AUTHOR: REFUSE\n"
     "FULL_LOOP_FEATURE_CARD: {:built \"none: the want already holds at the cited base head\" :want-coverage \"already true\" :proof-ref \"" grounded-sha "\"}")})

(deftest selection-entrypoint-replays-the-pinned-selection
  (testing "strategic-selection! (selection-entrypoint) returns the selector's recorded choice for this click"
    (let [replayed-selection {:status "selected"
                              :target target
                              :actuation {:status "enacted" :authority "runner"}}
          selection (runner/strategic-selection!
                      {:strategic-selection-invoke-fn
                       (fn [_request] {:ok true :selection replayed-selection})}
                      {:click/id "wm-click-2aef7e0c"})]
      (is (map? selection))
      (is (= :selected (:status selection)))
      (is (= target (:target selection))))))

(deftest required-loop-node-evaluator-annotates-the-decision
  (testing "loop-node-exercise/require-complete! (required-loop-node-evaluator) annotates without vetoing"
    (let [annotated (loop-node-exercise/require-complete! {:selection-certificate {}})
          exercise (get-in annotated [:selection-certificate :loop-node-exercise])]
      (is (map? exercise))
      (is (= :wm/loop-node-exercise-v1 (:schema exercise)))
      (is (contains? #{:complete :incomplete} (:status exercise))))))

(deftest ungrounded-artifact-ref-does-not-condemn-the-cards-proof-ref
  (testing "the injury's exact shape classifies :already-satisfied, grounding the card's :proof-ref commit"
    (let [classification (runner/author-refusal-classification
                           author-job "/home/joe/code/futon2")]
      (is (some? classification) "a REFUSE verdict must classify, never return nil")
      (is (= :already-satisfied (:outcome classification))
          (str "injured code closes :invalid-author-refusal here (injury " injury-id "); "
               "a sha-shaped artifact-ref that exists in no repository must not condemn "
               "a card whose :proof-ref names an existing commit"))
      (is (= {:repo "futon2" :sha grounded-sha}
             (:grounded-commit classification))))))

(deftest closure-carries-the-repaired-outcome
  (testing "throw-if-author-refused! closes the repaired classification, not the injured one"
    (let [data (try
                 (runner/throw-if-author-refused! author-job target :author-wait)
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (map? data) "the refusal must close with a typed throw")
      (is (= :already-satisfied (:failure-kind data)))
      (is (= target (:target data)))
      (is (= {:repo "futon2" :sha grounded-sha} (:grounded-commit data))))))

(deftest terminal-routing-emits-exactly-one-failure-receipt
  (testing "terminal-receipt (terminal-routing-entrypoint) routes the repaired close as one :failure receipt"
    (let [record {:run/id "tick-run-record-2026-10-27-39eb74f5"
                  :startedAt "2026-10-27T13:45:04.760746350Z"
                  :click/id "wm-click-2aef7e0c"
                  :selection-event {:event :cascade-selected :target target}
                  :failure {:kind :already-satisfied}}
          receipt (terminal-receipt/terminal-receipt record)]
      (is (= :failure (:kind receipt)))
      (is (= :already-satisfied (:failure-kind receipt)))
      (is (= target (:target receipt)))
      (is (= :mission (:target-kind receipt))))))
