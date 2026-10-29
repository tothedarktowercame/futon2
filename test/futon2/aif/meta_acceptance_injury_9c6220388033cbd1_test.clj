(ns futon2.aif.meta-acceptance-injury-9c6220388033cbd1-test
  "Acceptance pin (round 2, correcting first-proposal pin
   801c0794d9013888acf411dfef252404a71344b8ee6154ed7b88eb3f1620aaa1 per review
   refusal :acceptance/fails-for-unrelated-reason) for
   injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0
   (tick-run-record-2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0.edn,
   sha256 1cf5cba29be9db338e3e69277836c9c99e63c792c63cb28de1da3618eb9172b8).

   The injury: run 2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0 selected the
   cascade-candidate M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194
   (selection-event :cascade-selected, selected-target-reading :published,
   loop-node-exercise :complete) and closed :incomplete only because, at
   :cascade-resume, futon2.aif.wm.open-cascade-resume/validate-answer typed the
   authoring answer :error :kind :answer-holes-unresolved — the set of
   requisitioned hole ids did not equal the set of answered decision keys.
   That refusal — not any selection or authoring defect — left the run
   ungrounded (:run-output :absent :run-not-grounded), produced the fallback
   route RUNNER→FULL_LOOP_CLOSE :via :incomplete (full_loop_runner.clj
   terminal-fallback-route else-branch) and an :incomplete terminal receipt
   (digest 568a1b52b8a94d383d0f0b450124f3bc0a25dd3d66897ff6c61df4a4d649d02a).

   The injured condition, concretely: the cascade's holes carry vector ids of
   the pinned policy's shape ([\"problems/r10-scheduled-entrypoint\" 6] and
   kin, visible in the record's :selection-event policy-key :nodes), and the
   requisition reaches validate-answer over the JSON transport, where those
   ids arrive as ENCODED strings/keywords. validate-answer normalizes the
   ANSWER's decision keys via decoded-hole-id but compares them against
   (:hole-id (:holes requisition)) verbatim, so a semantically complete answer
   whose requisition-side hole ids still carry the transport encoding is
   refused with :answer-holes-unresolved. The repaired contract this test
   pins: the hole-id set comparison runs over DECODED hole ids on BOTH sides,
   so the complete answer is accepted and the resume can proceed under the
   same cascade digest without a new click.

   Round-2 scope fix: every deftest that can fail before repair fails ONLY on
   the hole-id decode comparison. The loop-node-evaluator entrypoint is still
   exercised (it is one of the diagnosis's required production functions) but
   its assertion is scoped to what that entrypoint's own contract guarantees —
   annotation without veto, per the 2026-10-19 frontier ruling — so it is green
   both before and after the repair and cannot mask or masquerade as the
   injury's failure (review refusal detail, first run lines 117-119).

   No stubs: every function under test is production. The only injected seam
   is :strategic-selection-invoke-fn, the replacement bridge named in
   strategic-selection!'s own docstring, replaying the pinned run's recorded
   selection-event shape (one attempt, no retries, no network); the
   :resume-port {:retry fn} adapter field is likewise the seam recognize's
   own docstring names as D3's only required adapter field."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.loop-node-exercise :as loop-node-exercise]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

(def ^:const injury-id "injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0")

(def ^:const target "M-futonzero-prelim-practice")

(def ^:const cascade-id "M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194")

(def ^:const cascade-digest (apply str (repeat 32 "ab")))

(def ^:const patch-source-digest (apply str (repeat 32 "cd")))

;; The pinned cascade's hole ids (policy-key :nodes shape). The requisition
;; arrives over the JSON transport, so its :holes carry the ids ENCODED —
;; exactly the string form decoded-hole-id exists to undo on the answer side.
(def transport-hole-ids
  ["[\"problems/r10-scheduled-entrypoint\" 6]"
   "[\"apparatus/the-system-stops-on-schedule\" 10]"])

(def decoded-hole-ids
  [["problems/r10-scheduled-entrypoint" 6]
   ["apparatus/the-system-stops-on-schedule" 10]])

(defn- held-context
  "recognize the pinned run's typed execution error with the transport-encoded
   hole ids it actually carried, using the resume-port adapter seam recognize
   documents as D3's only required adapter field."
  []
  (resume/recognize
    {:kind :wm/open-cascade-execution-error
     :cascade-digest cascade-digest
     :holes (map (fn [id] {:hole-id id
                           :owner "author"
                           :question "resolve the held phase hole"})
                transport-hole-ids)
     :phase :materialise
     :attempts []
     :resume-port {:retry (fn [ctx] {:status :cascade-ready :ctx ctx})}}))

(defn- complete-answer
  "The author's answer for the SAME requisition: every hole resolved by id,
   decision attributed to the requisition's author and accepted by its
   distinct reviewer, and a loadable patch. Its decision keys are the DECODED
   vector ids — the canonical form decoded-hole-id maps encoded keys to."
  [requisition]
  {:requisition-id (:requisition-id requisition)
   :cascade-digest cascade-digest
   :by "zai-2"
   :accepted-by "claude-reviewer"
   :decisions (zipmap decoded-hole-ids (repeat {:decision "resolved"}))
   :patch {:namespace "futon2.aif.wm.debugger"
           :var "held-phase"
           :source-digest patch-source-digest}})

(deftest selection-entrypoint-replays-the-pinned-selection
  (testing "strategic-selection! (selection-entrypoint) returns the selector's recorded cascade choice for this click"
    (let [replayed-selection {:status "selected"
                              :target target
                              :kind :cascade-candidate
                              :id cascade-id
                              :actuation {:status "enacted" :authority "runner"}}
          selection (runner/strategic-selection!
                      {:strategic-selection-invoke-fn
                       (fn [_request] {:ok true :selection replayed-selection})}
                      {:click/id "wm-retry-repair/c667a5c7974f8a4d30cef6f9be8bd1042c0c5f8c-2-patch-a1939dabaa7444a9ace0916af1e897e92abdb52d"})]
      (is (map? selection))
      (is (= :selected (:status selection)))
      (is (= target (:target selection))))))

(deftest required-loop-node-evaluator-annotates-without-vetoing
  (testing "loop-node-exercise/require-complete! (required-loop-node-evaluator) annotates without vetoing — green before and after repair"
    (let [annotated (loop-node-exercise/require-complete! {:selection-certificate {}})
          exercise (get-in annotated [:selection-certificate :loop-node-exercise])]
      (is (map? exercise))
      (is (= :wm/loop-node-exercise-v1 (:schema exercise)))
      (is (contains? #{:complete :incomplete} (:status exercise))
          "annotation only: this entrypoint never vetoes, so it is scoped out of the injury's failure attribution (review refusal, first run line 117)"))))

(deftest held-context-and-requisition-bind-the-pinned-cascade
  (testing "recognize holds the error and authoring-requisition dispatches for the pinned holes"
    (let [ctx (held-context)]
      (is (= :held (:status ctx)))
      (is (= cascade-digest (:cascade-digest ctx)))
      (let [requisition (resume/authoring-requisition ctx "zai-2" "claude-reviewer")]
        (is (= :dispatched (:status requisition)))
        (is (= 2 (count (:holes requisition))))
        (is (= :materialise (:phase requisition)))))))

(deftest complete-answer-with-transport-encoded-requisition-holes-is-accepted
  (testing "validate-answer (refusal-production) accepts the complete answer — the injury's exact refusal shape"
    (let [ctx (held-context)
          requisition (resume/authoring-requisition ctx "zai-2" "claude-reviewer")
          result (resume/validate-answer requisition (complete-answer requisition))]
      (is (not= :answer-holes-unresolved (:kind result))
          (str "injured code refuses here (injury " injury-id "): the requisition's "
               "transport-encoded hole-id strings are compared against the answer's "
               "decoded vector keys verbatim, so a complete answer is typed "
               ":answer-holes-unresolved and the run closes :incomplete; the "
               "comparison must run over decoded hole ids on both sides"))
      (is (= :accepted (:status result)))
      (is (= (:requisition-id requisition) (:requisition-id result)))
      (is (= decoded-hole-ids (sort-by pr-str (keys (:decisions result))))))))

(deftest submit-answer-preserves-and-records-whatever-it-returns
  (testing "submit-answer! records the whole answer append-only and the ledger chain stays verified"
    (let [ledger (resume/new-ledger "wm-retry-repair/c667a5c7974f8a4d30cef6f9be8bd1042c0c5f8c-2-patch-a1939dabaa7444a9ace0916af1e897e92abdb52d")
          ctx (held-context)
          requisition (resume/authoring-requisition ctx "zai-2" "claude-reviewer")]
      (resume/dispatch-requisition! ledger requisition)
      (let [answer (complete-answer requisition)
            result (resume/submit-answer! ledger requisition answer)]
        (is (= (:status (resume/validate-answer requisition answer))
               (:status result)))
        (is (= :verified (:status (resume/validate-ledger ledger))))
        (is (= 1 (count (:answers @ledger)))
            "no attempt is ever discarded, refused or accepted")))))

(deftest terminal-routing-emits-exactly-one-receipt
  (testing "terminal-receipt (terminal-routing-entrypoint) routes the injured close as exactly one :failure receipt"
    (let [record {:run/id "2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0"
                  :startedAt "2026-10-29T03:21:33.301712294Z"
                  :click/id "wm-retry-repair/c667a5c7974f8a4d30cef6f9be8bd1042c0c5f8c-2-patch-a1939dabaa7444a9ace0916af1e897e92abdb52d"
                  :selection-event {:event :cascade-selected :target target}
                  :failure {:kind :answer-holes-unresolved :stage :cascade-resume}}
          receipt (terminal-receipt/terminal-receipt record)]
      (is (= :failure (:kind receipt)))
      (is (= :answer-holes-unresolved (:failure-kind receipt)))
      (is (= target (:target receipt)))
      (is (= :mission (:target-kind receipt))))))
