(ns futon2.vm.tick-001-s09-r9-test
  "Virtual War Machine tick 1, step 9 (R9 no-self-certification).

  Real-machine REQUIREMENT test (VM-PROTOCOL.md): the real tick checks an
  enactment claim through an agent or process other than the enactor before
  counting it as done. This step's concrete input is R16's enactment claim
  (vm/tick-001/08-R16.edn, futon2 e688a07b45); R9 checked it independently.

  Requirement source:
  - futon2.aif.full-loop-runner is the real no-self-certification machinery:
    the full loop dispatches an author, dispatches a DISTINCT reviewer, and
    refuses when the selected reviewer is the author or is unavailable
    (full_loop_runner.clj:48-49 defaults differ; :3946 'Selected reviewer is
    unavailable or is the author').
  - independent-review-evidence (full_loop_runner.clj:1884) is the public
    read-only validator the adjudication uses: a review counts only when the
    job is done, its verdict marker is APPROVE, and execution evidence
    exists (reported or in the job's tool_use ledger). A text-only APPROVE
    with no execution evidence does not count.
  - historical-revalidation-entry (full_loop_runner.clj:1515) is the typed
    binding that refuses an admission whose author and reviewer are the same
    agent (not= check at :1526).
  - The model includes R9 as a node of the tick; war_machine.clj routes
    :R2 :6029, :R7/:R3, :R8, :R5, :R6, :R14, :TRACE but has no :R9 route
    tag, so the routing assertion fails until the node is wired. Failures
    are the node's open requirements, listed in the step record
    :requirements-open."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as flr]))

(def approved-review-job
  "A finished review job by an agent other than the enactor: terminal state
  done, APPROVE verdict marker first in :result-summary, and execution
  evidence both reported in the summary and corroborated by tool_use ledger
  events (review-execution-evidence reads both)."
  {:job-id "r9-independent-review-1"
   :state "done"
   :result-summary "FULL_LOOP_REVIEW: APPROVE reproduced the calls in a fresh JVM."
   :execution {:executed true :tool-events 3 :command-events 3}
   :events [{:type "tool_use" :tools ["Bash"] :text "java -cp … clojure.main"}
            {:type "tool_use" :tools ["Bash"] :text "clojure -M:test …"}]})

(def self-certified-job
  "Same terminal state and APPROVE text, but produced without any execution:
  no reported execution and an empty ledger. Self-certification must not
  count as a review."
  {:job-id "r9-self-certified-1"
   :state "done"
   :result-summary "FULL_LOOP_REVIEW: APPROVE trust me, it works."})

(def unfinished-job
  (assoc approved-review-job :state "running" :job-id "r9-unfinished-1"))

(deftest s09-r9
  ;; 1. A genuine independent review counts (real validator, real call).
  (is (true? (:valid? (flr/independent-review-evidence ["src/futon2/aif/observation.clj"]
                                                       approved-review-job))))
  ;; 2. APPROVE text with no execution does not count — not self-certifiable.
  (is (false? (:valid? (flr/independent-review-evidence ["src/futon2/aif/observation.clj"]
                                                        self-certified-job))))
  ;; 3. An unfinished job does not count even with full evidence.
  (is (false? (:valid? (flr/independent-review-evidence ["src/futon2/aif/observation.clj"]
                                                        unfinished-job))))
  ;; 4. Typed binding refuses author == reviewer (same agent on both sides).
  (let [obligation {:repair/id "T-self-cert"
                    :repair/status :open
                    :repair/class :machine-failure}
        base-admission {:schema :wm/historical-repair-admission-v1
                        :repair/id "T-self-cert"
                        :repair/status :awaiting-validation
                        :verification-id "v-1" :verification-artifact "a.edn"}
        distinct-casting {:author "zai-a" :repair-reviewer "codex-r"}
        self-casting {:author "zai-a" :repair-reviewer "zai-a"}]
    (is (some? (flr/historical-revalidation-entry
                obligation
                (assoc base-admission :actors {:author "zai-a" :reviewer "codex-r"})
                distinct-casting)))
    (is (nil? (flr/historical-revalidation-entry
               obligation
               (assoc base-admission :actors {:author "zai-a" :reviewer "zai-a"})
               self-casting))))
  ;; 5. Open requirement (accompanies the behavioural assertions above): the
  ;; real tick has no :R9 route — this step's independent check ran through
  ;; the R9 agent, not through the tick. Fails until the node is wired.
  (let [src (slurp (io/file "scripts/futon2/report/war_machine.clj"))]
    (is (boolean (re-find #":R9\s" src)))))
