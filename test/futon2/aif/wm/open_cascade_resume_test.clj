(ns futon2.aif.wm.open-cascade-resume-test
  "Packet D4 focused tests: the same-click resume protocol over a
  documented fixture/port.  Every planted control names the defect it must
  catch."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.open-cascade-resume :as r]))

(def cascade-digest (evidence/value-digest [:packet-d4 :cascade]))

(def holes [{:hole-id :hole/guard-source
             :owner "claude-2"
             :question "which situation source backs the guard?"}])

(def patch-descriptor
  {:namespace "futon2.aif.repairs.open-cascade"
   :var "apply-repair!"
   :source-digest (evidence/value-digest [:packet-d4 :patch-source])})

(defn- patch-loader [_]
  (fn [_] :loaded))

(defn- held-error
  "D3's typed open-cascade execution error against the documented port.
  The ONE adapter field D3 supplies is :resume-port {:retry fn}."
  [& [{:keys [retry]
       :or {retry (fn [_] {:cascade-digest cascade-digest
                           :outcome :phase-redone})}}]]
  {:kind :wm/open-cascade-execution-error
   :cascade-digest cascade-digest
   :holes holes
   :phase :enact
   :attempts [{:attempt 1 :error "first try"}]
   :resume-port {:retry (or retry (fn [_] {:cascade-digest cascade-digest
                                           :outcome :phase-redone}))}})

(defn- answer-for
  [requisition & [overrides]]
  (merge {:requisition-id (:requisition-id requisition)
          :cascade-digest (:cascade-digest requisition)
          :by (:author requisition)
          :accepted-by (:reviewer requisition)
          :decisions {:hole/guard-source {:decision "use I1 observation"
                                          :authority "authoring"}}
          :patch patch-descriptor}
         overrides))

(deftest recognition-is-typed
  (is (= :held (:status (r/recognize (held-error)))))
  (is (= :resume-error-unknown-class
         (:kind (r/recognize {:kind :some-other-error}))))
  (is (= :resume-cascade-digest-missing
         (:kind (r/recognize (assoc (held-error) :cascade-digest "x")))))
  (is (= :resume-phase-not-held
         (:kind (r/recognize (assoc (held-error) :phase :select)))))
  ;; missing the adapter field D3 must supply
  (is (= :resume-port-missing
         (:kind (r/recognize (dissoc (held-error) :resume-port))))))

(deftest requisition-binds-digest-holes-phase-and-identities
  (let [ctx (r/recognize (held-error))
        req (r/authoring-requisition ctx "claude-2" "codex-68")]
    (is (= :dispatched (:status req)))
    (is (= cascade-digest (:cascade-digest req)))
    (is (= :enact (:phase req)))
    (is (= holes (:holes req)))
    (is (= "claude-2" (:author req)))
    (is (= "codex-68" (:reviewer req)))
    ;; author and reviewer must be distinct agents
    (is (= :requisition-author-equals-reviewer
           (:kind (r/authoring-requisition ctx "claude-2" "claude-2"))))
    ;; stable identity bound to digest+phase+holes
    (is (= (:requisition-id req)
           (:requisition-id (r/authoring-requisition ctx "claude-2" "codex-68"))))))

(deftest answer-validation-contracts
  (let [ctx (r/recognize (held-error))
        req (r/authoring-requisition ctx "claude-2" "codex-68")
        ok (answer-for req)]
    (is (= :accepted (:status (r/validate-answer req ok))))
    ;; wrong requisition
    (is (= :answer-requisition-mismatch
           (:kind (r/validate-answer req (assoc ok :requisition-id "0")))))
    ;; wrong cascade digest
    (is (= :answer-cascade-digest-mismatch
           (:kind (r/validate-answer
                   req (assoc ok :cascade-digest
                              (evidence/value-digest [:other]))))))
    ;; author/reviewer identities must match the requisition
    (is (= :answer-author-mismatch
           (:kind (r/validate-answer req (assoc ok :by "someone-else")))))
    (is (= :answer-reviewer-mismatch
           (:kind (r/validate-answer
                   req (assoc ok :accepted-by "someone-else")))))
    ;; holes unresolved or foreign hole invented
    (is (= :answer-holes-unresolved
           (:kind (r/validate-answer
                   req (assoc ok :decisions
                              {:hole/other {:decision "x"}})))))
    ;; loadable patch required
    (is (= :answer-patch-missing
           (:kind (r/validate-answer req (assoc ok :patch nil)))))))

(deftest malformed-answers-preserve-everything
  (let [ledger (r/new-ledger "click-1")
        ctx (r/recognize (held-error))
        req (r/authoring-requisition ctx "claude-2" "codex-68")
        _ (r/dispatch-requisition! ledger req)
        bad (r/submit-answer! ledger req {:nonsense true})
        good (r/submit-answer! ledger req (answer-for req))]
    (is (= :answer-requisition-mismatch (:kind bad)))
    (is (= :accepted (:status good)))
    ;; BOTH the failed and the successful answer are preserved, raw
    (is (= 2 (count (:answers @ledger))))
    (is (= {:nonsense true} (:raw (first (:answers @ledger)))))))

(deftest same-click-resume-without-reselection
  (let [ledger (r/new-ledger "click-1")
        click-budget (atom 1)
        selection-counter (atom 1)
        retry-ctx (atom nil)
        error (held-error {:retry (fn [ctx] (reset! retry-ctx ctx)
                                    {:cascade-digest cascade-digest
                                     :outcome :phase-redone})})
        ctx (r/recognize error)
        req (r/authoring-requisition ctx "claude-2" "codex-68")
        _ (r/dispatch-requisition! ledger req)
        answer (answer-for req)
        accepted (r/submit-answer! ledger req answer)
        _ (is (= :accepted (:status accepted)))
        loaded (r/hot-load! ledger req accepted patch-loader)
        _ (is (not= :error (:status loaded)))
        resumed (r/resume! ledger ctx {:click-budget click-budget
                                       :selection-counter selection-counter})]
    (is (= :resumed (:status resumed)))
    (is (= :phase-redone (get-in resumed [:result :outcome])))
    ;; NO new click, NO selection
    (is (= 0 (:clicks-spent resumed)))
    (is (= 0 (:selections-run resumed)))
    (is (= 1 @click-budget))
    (is (= 1 @selection-counter))
    ;; the retry received the SAME cascade digest and phase
    (is (= cascade-digest (:cascade-digest @retry-ctx)))
    (is (= :enact (:phase @retry-ctx)))))

(deftest protocol-violation-reselection-is-caught
  ;; planted defect: the retry entrypoint secretly runs selection again
  (let [ledger (r/new-ledger "click-1")
        click-budget (atom 1)
        selection-counter (atom 1)
        error (held-error {:retry (fn [_] (swap! selection-counter inc)
                                    {:cascade-digest cascade-digest
                                     :outcome :done})})
        ctx (r/recognize error)]
    (is (= :resume-protocol-violation
           (:kind (r/resume! ledger ctx {:click-budget click-budget
                                         :selection-counter selection-counter}))))
    ;; the violation is preserved in the ledger with both counts recorded
    (is (some #(= :protocol-violation (:outcome %)) (:retries @ledger)))))

(deftest terminal-failures-are-errors-never-abstention
  ;; retry throws
  (let [ledger (r/new-ledger "click-1")
        ctx (r/recognize (held-error {:retry (fn [_]
                                               (throw (ex-info "boom"
                                                               {})))}))]
    (is (= :resume-retry-failed (:kind (r/resume! ledger ctx))))
    (is (= :error (:status (r/resume! ledger ctx)))))
  ;; hot-load patch throws
  (let [ledger (r/new-ledger "click-1")
        ctx (r/recognize (held-error))
        req (r/authoring-requisition ctx "claude-2" "codex-68")
        answer (answer-for req)
        accepted (r/validate-answer req answer)
        loaded (r/hot-load! ledger req accepted
                            (fn [_]
                              (fn [_] (throw (ex-info "patch boom" {})))))]
    (is (= :hot-load-failed (:kind loaded)))
    (is (= :error (:status loaded)))
    (is (= :failed (:outcome (first (:attempts @ledger))))))
  ;; nothing anywhere is :abstained/:refused/:no-selection
  (let [ledger (r/new-ledger "click-1")
        outcomes [(r/recognize {:kind :other})
                  (r/handle-error! ledger (held-error) "a" "a" {})]]
    (is (every? #(= :error (:status %)) outcomes))
    (is (not-any? #{:abstained :refused :no-selection}
                  (map :status outcomes)))))

(deftest retry-history-is-preserved
  (let [ledger (r/new-ledger "click-1")
        error (held-error {:attempts [{:attempt 1 :error "first try"}]})]
    (is (= [{:attempt 1 :error "first try"}]
           (:prior-attempts (r/recognize error))))
    ;; a failing retry then a succeeding retry: both recorded, first never
    ;; discarded
    (let [ctx (r/recognize error)
          failing (r/recognize (held-error
                                {:retry (fn [_]
                                          (throw (ex-info "x" {})))}))
          _ (r/resume! ledger failing)
          _ (r/resume! ledger ctx)]
      (is (= 2 (count (:retries @ledger))))
      (is (= :failed (:outcome (first (:retries @ledger)))))
      (is (= :retried (:outcome (second (:retries @ledger))))))))

(deftest end-to-end-handle-error-happy-path
  (let [ledger (r/new-ledger "click-1")
        click-budget (atom 3)
        selection-counter (atom 2)
        error (held-error)
        ctx (r/recognize error)
        req (r/authoring-requisition ctx "claude-2" "codex-68")
        answer (answer-for req)
        result (r/handle-error! ledger error "claude-2" "codex-68"
                                answer
                                {:click-budget click-budget
                                 :selection-counter selection-counter
                                 :patch-loader patch-loader})]
    (is (= :resumed (:status result)))
    (is (= 0 (:clicks-spent result)))
    (is (= 0 (:selections-run result)))
    (is (= 3 @click-budget))
    (is (= 2 @selection-counter))
    ;; the whole paper trail: requisition, accepted answer, loaded
    ;; attempt, retried phase
    (is (= 1 (count (:requisitions @ledger))))
    (is (= 1 (count (:answers @ledger))))
    (is (= 1 (count (:attempts @ledger))))
    (is (= 1 (count (:retries @ledger))))
    (is (= :verified (:status (r/validate-ledger ledger))))))

(deftest retry-result-must-be-bound-to-the-held-cascade
  (let [ledger (r/new-ledger "click-1")
        unbound (r/recognize (held-error {:retry (constantly {:outcome :done})}))
        foreign (r/recognize
                 (held-error {:retry (constantly
                                      {:cascade-digest
                                       (evidence/value-digest [:foreign])})}))]
    (is (= :resume-protocol-violation (:kind (r/resume! ledger unbound))))
    (is (= :resume-protocol-violation (:kind (r/resume! ledger foreign))))))

(deftest ledger-chain-detects-mutation
  (let [ledger (r/new-ledger "click-1")
        ctx (r/recognize (held-error))
        req (r/authoring-requisition ctx "claude-2" "codex-68")]
    (r/dispatch-requisition! ledger req)
    (is (= :verified (:status (r/validate-ledger ledger))))
    (swap! ledger assoc-in [:events 0 :value :phase] :changed)
    (is (= :resume-ledger-chain-broken
           (:kind (r/validate-ledger ledger))))))
