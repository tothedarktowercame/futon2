(ns futon2.aif.meta-repair-coordinator-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-repair-coordinator :as coordinator]))

(def sha-a (apply str (repeat 40 "a")))
(def sha-b (apply str (repeat 40 "b")))
(def pin-a {:path "data/wm-learning/runs/2026-10-19/11250189.edn"
            :sha256 (apply str (repeat 64 "1"))})
(def diagnosis-pin {:path "holes/labs/wm-contract/meta-self-heal-diagnosis.edn"
                    :sha256 (apply str (repeat 64 "d"))})
(def source-pin {:path "/tmp/meta-injury-ledger.edn"
            :sha256 (apply str (repeat 64 "e"))})

(defn- click-7-fixture [name]
  ;; Captured click-7 evidence, not a reconstructed ledger/transaction.
  (edn/read-string (slurp (str "test/futon2/aif/fixtures/click-7-" name ".edn"))))

(defn- record-pin [suffix digit]
  {:path (str "data/wm-runs/" suffix ".edn")
   :sha256 (apply str (repeat 64 digit))})

(defn- append! [history event]
  (let [result (ledger/append-transition history event)]
    (assert (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- opened-ledger []
  (append! ledger/empty-ledger
           {:injury-id "injury/2026-10-19/reload"
            :state :open :attempt 1 :cause-record-pin pin-a
            :failure-stage :reload :failure-kind :phase-failure
            :detected-click "click-2026-10-19-4"}))

(defn- durable [history]
  {:schema coordinator/durable-ledger-schema :status :loaded
   :source-pin source-pin :ledger history})

(defn- options
  ([] (options 1))
  ([attempt]
   {:diagnosis-evidence-pin diagnosis-pin
    :acceptance-test {:id "retry-original-failure"
                      :command ["clojure" "-M:test" "-n" "focused"]
                      :expected {:exit 0}}
    :author "agent/author" :reviewer "agent/reviewer"
    :repair-reviewer "agent/repair-reviewer"
    :author-session (str "author-session-" attempt)
    :reviewer-session (str "reviewer-session-" attempt)
    :repair-reviewer-session (str "repair-reviewer-session-" attempt)
    :target-repository "futon2"
    :target-worktree "/home/joe/worktrees/futon2-meta-self-heal-repair"}))

(defn- receipt [request status payload]
  (let [order (:work-order request)]
    (coordinator/seal-port-receipt
     {:phase (:phase request) :injury-id (:injury-id order)
      :repair-id (:repair-id order) :attempt (:attempt order)
      :predecessor-pin (:predecessor-pin request)
      :status status :payload payload})))

(defn- ports
  ([] (ports {}))
  ([overrides]
   (merge
    {:author (fn [r] (receipt r :succeeded
                              {:patch-sha sha-a
                               :worktree (get-in r [:work-order :target-worktree])}))
     :review (fn [r] (receipt r :succeeded
                              {:patch-sha sha-a :reviewer-sha sha-b}))
     :enact (fn [r] (receipt r :succeeded
                             {:patch-sha sha-a :integration-commit sha-a
                              :retry-click "click-retry-repair-1"}))
     :retry-click-id (fn [_repair-id _attempt ordinal _patch-sha]
                       (str "click-retry-requeue-" ordinal))
     :retry (fn [r] (receipt r :succeeded
                             {:repair-id (get-in r [:work-order :repair-id])
                              :patch-sha sha-a :retry-click "click-retry-repair-1"
                              :outcome :grounded-change}))}
    overrides)))

(defn- prepared [history options]
  (coordinator/prepare (durable history) options))

(deftest happy-path-is-dedicated-and-discharges-attributably
  (let [counts (atom {:author 0 :review 0 :enact 0 :retry 0 :forbidden 0})
        base-ports (ports)
        wrapped (into {:retry-click-id (:retry-click-id base-ports)}
                      (for [[phase port] (select-keys base-ports coordinator/phases)]
                        [phase (fn [request]
                                 (swap! counts update phase inc)
                                 (port request))]))
        forbidden (fn [& _] (swap! counts update :forbidden inc)
                    (throw (ex-info "ordinary machinery invoked" {})))
        all-ports (assoc wrapped
                         :ordinary-loop-node forbidden :pattern-fold forbidden
                         :d-task forbidden :policy-selection forbidden)
        start (prepared (opened-ledger) (options))
        result (coordinator/coordinate (durable (opened-ledger))
                                       (:transaction start) all-ports)]
    (is (= :prepared (:status start)))
    (is (= :discharged (:status result)))
    (is (empty? (:injuries (ledger/active-injuries (:ledger result)))))
    (is (= {:author 1 :review 1 :enact 1 :retry 1 :forbidden 0} @counts))
    (is (= [:open :claimed :authored :reviewed :enacted :discharged]
           (->> (get-in result [:ledger :entries]) (keep :state) vec)))
    (is (= "click-retry-repair-1"
           (get-in result [:ledger :entries 5 :retry-click])))))

(defn- awaiting-retry
  ([history] (awaiting-retry history {}))
  ([history overrides]
   (let [start (prepared history (options))]
     (coordinator/coordinate
      (durable history) (:transaction start)
      (ports (merge {:retry (fn [r]
                              (receipt r :queued
                                       {:repair-id (get-in r [:work-order :repair-id])
                                        :patch-sha sha-a
                                        :retry-click "click-retry-repair-1"}))}
                    overrides))))))

(defn- inconclusive [history suffix retry-click]
  (let [result (ledger/append-observation
                history (record-pin suffix (subs suffix 0 1))
                :retry-inconclusive
                {:injury-id "injury/2026-10-19/reload"
                 :retry-click retry-click})]
    (assert (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(deftest inconclusive-retries-requeue-in-order-and-replay-without-effects
  ;; Pins the ledger-first +1 ordinal, request attribution, dynamic phase, and
  ;; receipt replay rules for first and second inconclusive retry outcomes.
  (let [initial (awaiting-retry (opened-ledger))
        history1 (inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        calls (atom [])
        ps (ports {:retry (fn [r]
                            (swap! calls conj r)
                            (receipt r :queued {:retry-click (:retry-click r)}))})
        first (coordinator/coordinate (durable history1) (:transaction initial) ps)
        calls-after-first (vec @calls)
        replay (coordinator/coordinate (durable (:ledger first))
                                       (:transaction first) ps)
        calls-after-replay (vec @calls)
        history2 (inconclusive (:ledger replay) "3-second"
                               "click-retry-requeue-1")
        second (coordinator/coordinate (durable history2) (:transaction replay) ps)]
    (is (= :awaiting-retry (:status first)))
    (is (= "click-retry-requeue-1" (:retry-click first)))
    (is (= 1 (-> first :ledger :entries last :requeues)))
    (is (= 1 (count calls-after-first)))
    (is (= {:phase :retry-requeue-1
            :retry-click "click-retry-requeue-1"
            :requeue-ordinal 1}
           (select-keys (clojure.core/first calls-after-first)
                        [:phase :retry-click :requeue-ordinal])))
    (is (some #(= :retry-requeue-1 (:phase %))
              (get-in first [:transaction :receipts])))
    (is (= (:ledger first) (:ledger replay)))
    (is (= calls-after-first calls-after-replay))
    (is (= :awaiting-retry (:status second)))
    (is (= "click-retry-requeue-2" (:retry-click second)))
    (is (= 2 (-> second :ledger :entries last :requeues)))
    (is (= :retry-requeue-2 (-> @calls last :phase)))
    (is (= 2 (-> @calls last :requeue-ordinal)))))

(deftest appended-requeue-without-receipt-is-queued-on-resume
  ;; Pins crash repair between the durable append and the retry-port receipt.
  (let [initial (awaiting-retry (opened-ledger))
        observed (inconclusive (:ledger initial) "4-crash" "click-retry-repair-1")
        appended (ledger/append-requeue observed "injury/2026-10-19/reload"
                                        "click-retry-requeue-1")
        calls (atom [])
        ps (ports {:retry (fn [r]
                            (swap! calls conj r)
                            (receipt r :queued {:retry-click (:retry-click r)}))})
        result (coordinator/coordinate (durable (:ledger appended))
                                       (:transaction initial) ps)]
    (is (= :awaiting-retry (:status result)))
    (is (= (:ledger appended) (:ledger result)))
    (is (= 1 (count @calls)))
    (is (= :retry-requeue-1 (-> @calls first :phase)))
    (is (= "click-retry-requeue-1" (:retry-click result)))))

(deftest spent-retry-budget-abandons-before-another-port-call
  ;; Pins the separate retry budget and the no-effect terminal boundary.
  (let [limited (ledger/seal-ledger (assoc ledger/empty-ledger :retry-limit 1))
        history (append! limited
                         {:injury-id "injury/2026-10-19/reload"
                          :state :open :attempt 1 :cause-record-pin pin-a
                          :failure-stage :reload :failure-kind :phase-failure
                          :detected-click "click-2026-10-19-4"})
        initial (awaiting-retry history)
        observed1 (inconclusive (:ledger initial) "5-first" "click-retry-repair-1")
        calls (atom 0)
        ps (ports {:retry (fn [r]
                            (swap! calls inc)
                            (receipt r :queued {:retry-click (:retry-click r)}))})
        requeued (coordinator/coordinate (durable observed1)
                                         (:transaction initial) ps)
        observed2 (inconclusive (:ledger requeued) "6-spent"
                                "click-retry-requeue-1")
        _ (reset! calls 0)
        result (coordinator/coordinate (durable observed2)
                                       (:transaction requeued) ps)]
    (is (= :abandoned (:status result)))
    (is (= :retry-budget-spent (:reason result)))
    (is (= :abandoned (-> result :ledger :entries last :state)))
    (is (zero? @calls))))

(deftest requeue-port-must-echo-the-minted-retry-click
  ;; Pins attribution validation for the dynamically minted retry identity.
  (let [initial (awaiting-retry (opened-ledger))
        observed (inconclusive (:ledger initial) "7-wrong" "click-retry-repair-1")
        result (coordinator/coordinate
                (durable observed) (:transaction initial)
                (ports {:retry (fn [r]
                                 (receipt r :queued
                                          {:retry-click "click/unrelated"}))}))]
    (is (= :coordinator/retry-attribution-refused (:reason result)))
    (is (= 1 (-> result :ledger :entries last :requeues)))))

(deftest requeue-without-a-minting-port-refuses-typed-before-any-ledger-append
  ;; M10b-3 production wiring (claude-5, 2026-10-20): the futon3c adapter's
  ;; :retry-click-id-fn reaches the coordinator as :retry-click-id through
  ;; production effect-ports and runner_service select-keys. If either hop
  ;; drops it, the requeue must refuse by name, not throw, and must not call
  ;; the retry port.
  (let [initial (awaiting-retry (opened-ledger))
        observed (inconclusive (:ledger initial) "8-nofn" "click-retry-repair-1")
        calls (atom 0)
        ps (dissoc (ports {:retry (fn [r]
                                    (swap! calls inc)
                                    (receipt r :queued {:retry-click (:retry-click r)}))})
                   :retry-click-id)
        result (coordinator/coordinate (durable observed) (:transaction initial) ps)]
    (is (= :refused (:status result)))
    (is (= :coordinator/retry-click-id-unavailable (:reason result)))
    (is (zero? @calls))
    (is (= 0 (or (-> result :ledger :entries last :requeues) 0))
        "nothing is appended before the port is known to exist")))

(deftest rejection-and-port-failures-stop-without-silent-advance
  (doseq [[label override expected-state expected-reason]
          [[:author {:author (fn [r] (receipt r :failed {:reason :author-crash}))}
            :reopened :author-failed]
           [:review {:review (fn [r] (receipt r :rejected {:reason :changes}))}
            :authored :review-rejected]
           [:reload {:enact (fn [r] (receipt r :failed {:reason :reload-failed}))}
            :reviewed :enact-failed]]]
    (testing (name label)
      (let [history (opened-ledger)
            start (prepared history (options))
            result (coordinator/coordinate (durable history) (:transaction start)
                                           (ports override))]
        (is (= :held (:status result)))
        (is (= expected-reason (:reason result)))
        (is (= expected-state (-> result :ledger :entries last :state)))))))

(deftest malformed-and-substituted-port-receipts-refuse-typedly
  (let [history (opened-ledger)
        tx (:transaction (prepared history (options)))
        malformed (coordinator/coordinate
                   (durable history) tx (ports {:author (constantly {:bad true})}))
        substitute (coordinator/coordinate
                    (durable history) tx
                    (ports {:author
                            (fn [r]
                              (-> (receipt r :succeeded
                                           {:patch-sha sha-a :worktree "/tmp/w"})
                                  (assoc :repair-id "repair/substituted")))}))]
    (is (= :coordinator/port-receipt-refused (:reason malformed)))
    (is (= :coordinator/port-receipt-refused (:reason substitute)))
    (is (= :author (:phase substitute)))))

(deftest resumed-transaction-must-bind-the-ledger-authored-receipt
  ;; Lean: MetaRepairResumeIdentity.matching_enacted_resume_allowed and
  ;; empty_enacted_resume_refused.  The first transaction reaches :enacted;
  ;; a fresh empty transaction for that same attempt must not mint another
  ;; author receipt which the ledger will later skip appending.
  (let [history (opened-ledger)
        started (prepared history (options))
        paused (coordinator/coordinate
                (durable history) (:transaction started)
                (assoc (ports) :stop-after :enact))
        enacted-history (:ledger paused)
        ;; This is the transaction prepared before the first process stopped;
        ;; it has the same sealed work order but no retained port receipts.
        empty-resume (:transaction started)
        author-calls (atom 0)
        refusal (coordinator/coordinate
                 (durable enacted-history) empty-resume
                 (ports {:author (fn [request]
                                   (swap! author-calls inc)
                                   (receipt request :succeeded
                                            {:patch-sha sha-b :worktree "/tmp/substituted"}))}))
        matching (coordinator/coordinate (durable enacted-history)
                                         (:transaction paused) (ports))]
    (is (= :checkpoint (:status paused)))
    (is (= :enacted (-> enacted-history :entries last :state)))
    (is (= :refused (:status refusal)))
    (is (= :coordinator/resume-identity-refused (:reason refusal)))
    (is (zero? @author-calls))
    (is (= enacted-history (:ledger refusal)))
    (is (= :discharged (:status matching)))))

(deftest retry-failure-reopens-and-unattributed-success-cannot-discharge
  (let [history (opened-ledger)
        tx (:transaction (prepared history (options)))
        failed (coordinator/coordinate
                (durable history) tx
                (ports {:retry
                        (fn [r]
                          (receipt r :failed
                                   {:repair-id (get-in r [:work-order :repair-id])
                                    :patch-sha sha-a
                                    :retry-click "click-retry-repair-1"
                                    :new-cause-pin {:path "runs/retry-failed.edn"
                                                    :sha256 (apply str (repeat 64 "f"))}}))}))
        unattributed (coordinator/coordinate
                      (durable history) tx
                      (ports {:retry
                              (fn [r]
                                (receipt r :succeeded
                                         {:repair-id "repair/unrelated"
                                          :patch-sha sha-a
                                          :retry-click "click-retry-repair-1"}))}))]
    (is (= :reopened (:status failed)))
    (is (= 2 (get-in failed [:ledger :entries 5 :attempt])))
    (is (= :coordinator/retry-attribution-refused (:reason unattributed)))
    (is (= :enacted (-> unattributed :ledger :entries last :state)))))

(deftest recorded-failure-cannot-replay-from-a-reopened-transaction
  (let [calls (atom 0)
        history (opened-ledger)
        tx (:transaction (prepared history (options)))
        ps (ports {:author (fn [r]
                             (swap! calls inc)
                             (receipt r :failed {:reason :offline}))})
        first (coordinator/coordinate (durable history) tx ps)
        second (coordinator/coordinate (durable (:ledger first))
                                       (:transaction first) ps)]
    (is (= :held (:status first)))
    ;; Production retires this stale transaction before it can get here.  The
    ;; coordinator itself still refuses the illegal second reopen rather than
    ;; dispatching the recorded failed author receipt again.
    (is (= :refused (:status second)))
    (is (= :coordinator/input-refused (:reason second)))
    (is (= 1 @calls))
    (is (= :reopened (-> first :ledger :entries last :state)))))

(deftest click-7-author-failure-appends-a-reopened-second-attempt
  (let [history (click-7-fixture "ledger")
        transaction (click-7-fixture "transaction")
        calls (atom 0)
        result (coordinator/coordinate
                (durable history) transaction
                (ports {:author (fn [request]
                                  (swap! calls inc)
                                  (receipt request :failed
                                           {:reason :agency-response-not-one-edn-map}))}))
        appended (last (:entries (:ledger result)))]
    (is (= :held (:status result)))
    (is (= :author-failed (:reason result)))
    (is (= :reopened (:state appended)))
    (is (= 2 (:attempt appended)))
    (is (= :author-failed (:reason appended)))
    (is (= (:receipt-pin (:receipt result)) (:port-receipt-pin appended)))
    (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))
    ;; The recorded failed receipt is replayed; no new author effect is sent.
    (is (zero? @calls))))

(defn- failed-attempt [history attempt]
  (let [injury "injury/2026-10-19/reload"
        repair "repair/exhaustion"
        history (append! history
                         {:injury-id injury :state :claimed :attempt attempt
                          :repair-id repair :algorithm "dedicated-meta-self-heal"
                          :author-session (str "a" attempt)
                          :reviewer-session (str "r" attempt)})
        history (append! history
                         {:injury-id injury :state :authored :attempt attempt
                          :repair-id repair :patch-sha sha-a :worktree "/tmp/w"})
        history (append! history
                         {:injury-id injury :state :reviewed :attempt attempt
                          :repair-id repair :verdict :approved :reviewer-sha sha-b
                          :reviewer-session (str "r" attempt)})
        history (append! history
                         {:injury-id injury :state :enacted :attempt attempt
                          :repair-id repair :retry-click (str "retry-" attempt)
                          :integration-commit sha-a})]
    (ledger/append-retry-result
     history {:injury-id injury :repair-id repair :patch-sha sha-a
              :retry-click (str "retry-" attempt)
              :new-cause-pin {:path (str "runs/failure-" attempt ".edn")
                              :sha256 (apply str (repeat 64 (str attempt)))}})))

(deftest attempt-limit-exhaustion-is-typed-abandoned
  (let [a1 (failed-attempt (opened-ledger) 1)
        a2 (failed-attempt (:ledger a1) 2)
        history (:ledger a2)
        start (prepared history (options 3))
        a3 (coordinator/coordinate
            (durable history) (:transaction start)
            (ports {:retry
                    (fn [r]
                      (receipt r :failed
                               {:repair-id (get-in r [:work-order :repair-id])
                                :patch-sha sha-a
                                :retry-click "click-retry-repair-1"
                                :new-cause-pin
                                {:path "runs/failure-3.edn"
                                 :sha256 (apply str (repeat 64 "3"))}}))}))]
    (is (= :reopened (get-in a1 [:entry :state])))
    (is (= :reopened (get-in a2 [:entry :state])))
    (is (= :abandoned (:status a3)))
    (is (= :abandoned (-> a3 :ledger :entries last :state)))
    (is (= :attempt-limit-exhausted (-> a3 :ledger :entries last :reason)))
    (is (empty? (:injuries (ledger/active-injuries (:ledger a3)))))))

(deftest preparation-requires-active-durable-ledger-and-fresh-sessions
  (is (= :coordinator/preparation-refused
         (:reason (prepared ledger/empty-ledger (options)))))
  (let [bad (assoc (options) :reviewer-session "author-session-1")]
    (is (= :coordinator/preparation-refused
           (:reason (prepared (opened-ledger) bad))))))

(deftest preparation-carries-the-effective-attempt-limit-after-a-corrected-reopen
  ;; F38 (claude-5, 2026-10-20). Live pin: the byte-identical ledger snapshot of
  ;; injury d755c886 abandoned at attempt 3 (F35 fixture), reopened by the
  ;; correction that names the two refused author receipts. Before F38 the
  ;; work order copied the raw :attempt-limit 3 and verify-work-order refused
  ;; attempt 4 as :attempt/exceeds-limit, so prepare could never run it.
  (let [history (edn/read-string
                 (slurp "test/futon2/aif/fixtures/meta-injury-ledger-2026-10-20-d755c886-abandoned.edn"))
        injury "injury/d755c886a22d6fc8c04db7fada64089e07157f62"
        repair "repair/239cf98836afc977cd589141c224ec3f4bca83dc"
        by-ordinal (into {} (map (juxt :ordinal identity)) (:entries history))
        corrected (append! history
                           {:injury-id injury :repair-id repair
                            :state :reopened :attempt 4
                            :reason :port-refusal-corrected
                            :correction-commit "f63a3da99bb19f4c2217e20ed6f47572e996ccbc"
                            :corrected-port-receipt-pins [(:port-receipt-pin (by-ordinal 9))
                                                          (:port-receipt-pin (by-ordinal 11))]})
        result (prepared corrected (options 4))
        order (:work-order result)]
    (is (= :prepared (:status result)) (pr-str (select-keys result [:status :reason :defects])))
    (is (= 4 (:attempt order)))
    (is (= 5 (:attempt-limit order)) "limit 3 plus the two refused receipts")
    (is (= :valid (:status (coordinator/verify-work-order order))))
    ;; The check F38 satisfies is still live: the same order under the raw limit.
    (let [raw (coordinator/seal-work-order (assoc (dissoc order :work-order-pin) :attempt-limit 3))
          verdict (coordinator/verify-work-order raw)]
      (is (= :refused (:status verdict)))
      (is (some #{:attempt/exceeds-limit} (:defects verdict))))))

;; --- One-click frontier repair (2026-10-22): durable service-failed child ---

(defn- child-acceptance-pin
  "Each failed child's durable acceptance file is its own evidence; pins are
  per-click, as in production."
  [click-id]
  {:path (str "data/wm-click-acceptances/click-" click-id ".edn")
   :sha256 (str (apply str (repeat 63 "7"))
               (nth "0123456789abcdef" (mod (count click-id) 16)))})

(defn- failed-child-status-fn
  "Read-only port shaped like futon3c runner_service click-status/acceptance
  state: only the named child is durably :failed/:service-failed, and the
  evidence pin seals the exact acceptance bytes (message preserved there —
  'Serving runner source drifts from the canonical checkout', attempt-id nil;
  no typed data is invented)."
  [failed-click]
  (fn [click-id]
    (when (= failed-click click-id)
      {:status :failed
       :result {:attempt-id nil :outcome :service-failed
                :error "Serving runner source drifts from the canonical checkout"}
       :evidence-pin (child-acceptance-pin click-id)})))

(defn- completed-without-run-record-status-fn
  [refused-click reason]
  (fn [click-id]
    (when (= refused-click click-id)
      {:status :completed
       :run-record-status :absent
       :run-record-absence :runner-did-not-return-run-record
       :run-record-reference-conflict? false
       :result {:outcome :unknown
                :runtime-disposition
                {:status :refused
                 :reason reason}}
       :evidence-pin (child-acceptance-pin click-id)})))

(defn- observation-count
  ([history]
   (count (filter #(= :retry-inconclusive (:observation-kind %))
                  (:entries history))))
  ([history pin]
   (count (filter #(and (= :retry-inconclusive (:observation-kind %))
                        (= pin (:record-pin %)))
                  (:entries history)))))

(defn- requeue-ports
  ([] (requeue-ports {}))
  ([overrides]
   (ports (merge {:retry (fn [r]
                           (receipt r :queued {:retry-click (:retry-click r)}))}
                 overrides))))

(deftest service-failed-child-without-run-record-is-observed-and-requeued
  ;; The real shape of click wm-requeue-7-4-repair/239cf988: the enacted
  ;; retry child failed service-side BEFORE any run record, so terminal
  ;; ingestion appended nothing and every resume starved at :awaiting-retry.
  ;; With the status port the failure is observed from its own durable
  ;; acceptance evidence and the requeue loop advances.
  (let [initial (awaiting-retry (opened-ledger))
        _ (is (= :awaiting-retry (:status initial)))
        _ (is (= "click-retry-repair-1" (:retry-click initial)))
        ;; Control: without the port the same resume stays observation-starved.
        control (coordinator/coordinate
                 (durable (:ledger initial)) (:transaction initial)
                 (requeue-ports))
        _ (is (= :awaiting-retry (:status control)))
        _ (is (= "click-retry-repair-1" (:retry-click control)))
        _ (is (zero? (observation-count (:ledger control))))
        calls (atom [])
        ps (requeue-ports
            {:retry (fn [r]
                      (swap! calls conj r)
                      (receipt r :queued {:retry-click (:retry-click r)}))
             :retry-click-status-fn (failed-child-status-fn "click-retry-repair-1")})
        result (coordinator/coordinate (durable (:ledger initial))
                                       (:transaction initial) ps)
        entries (-> result :ledger :entries)]
    (is (= :awaiting-retry (:status result)))
    (is (= "click-retry-requeue-1" (:retry-click result)))
    (is (= 1 (observation-count (:ledger result)))
        "exactly one observation cites the child's acceptance evidence")
    (is (= {:injury-id "injury/2026-10-19/reload"
            :retry-click "click-retry-repair-1"}
           (-> (filter #(= :retry-inconclusive (:observation-kind %)) entries)
               first (select-keys [:injury-id :retry-click]))))
    (is (= (child-acceptance-pin "click-retry-repair-1")
           (-> (filter #(= :retry-inconclusive (:observation-kind %)) entries)
               first :record-pin)))
    (is (= 1 (-> result :ledger :entries last :requeues)))
    (is (= :retry-requeue-1 (-> @calls first :phase)))))

(deftest attribution-refused-requeue-is-replaced-before-old-id-dispatch
  (let [initial (awaiting-retry (opened-ledger))
        observed (inconclusive (:ledger initial) "2-first" "click-retry-repair-1")
        malformed-id (str "wm-requeue-1-1-repair/example-patch-" sha-b)
        malformed (ledger/append-requeue observed
                                         "injury/2026-10-19/reload"
                                         malformed-id)
        _ (is (= :appended (:status malformed)))
        dispatched (atom [])
        ps (requeue-ports
            {:retry-click-id
             (fn [repair-id attempt ordinal patch-sha]
               (str "wm-requeue-" ordinal "-" attempt "-" repair-id
                    "-patch-" patch-sha))
             :retry (fn [r]
                      (when (= malformed-id (:retry-click r))
                        (throw (ex-info "malformed retry was redispatched" {})))
                      (swap! dispatched conj (:retry-click r))
                      (receipt r :queued {:retry-click (:retry-click r)}))
             :retry-click-status-fn
             (completed-without-run-record-status-fn
              malformed-id :meta-self-heal/retry-attribution-refused)})
        result (coordinator/coordinate (durable (:ledger malformed))
                                       (:transaction initial) ps)
        replacement (:retry-click result)]
    (is (= :awaiting-retry (:status result)))
    (is (not= malformed-id replacement))
    (is (.endsWith replacement (str "-patch-" sha-a))
        "the replacement identity comes from the injury's authored patch")
    (is (= [replacement] @dispatched)
        "the malformed id is observed and replaced before the retry port runs")
    (is (= 2 (-> result :ledger :entries last :requeues)))
    (is (= 1 (observation-count (:ledger result)
                                (child-acceptance-pin malformed-id))))))

(deftest arbitrary-typed-completion-without-run-record-is-observed
  (doseq [reason [:makeup-unavailable :another-typed-refusal]]
    (let [initial (awaiting-retry (opened-ledger))
          click (:retry-click initial)
          result (coordinator/coordinate
                  (durable (:ledger initial)) (:transaction initial)
                  (requeue-ports
                   {:retry-click-status-fn
                    (completed-without-run-record-status-fn click reason)}))]
      (is (= :awaiting-retry (:status result)) (name reason))
      (is (= 1 (observation-count (:ledger result)
                                  (child-acceptance-pin click)))
          (name reason)))))

(deftest failed-child-observation-is-not-duplicated
  ;; Crash repair between the observation append and the requeue append, plus
  ;; a plain replay: the evidence pin is never observed twice.
  (let [initial (awaiting-retry (opened-ledger))
        ;; Simulate the crash-after-observation state directly, the same way
        ;; terminal ingestion would have appended it.
        pin (child-acceptance-pin "click-retry-repair-1")
        pre (ledger/append-observation
             (:ledger initial) pin :retry-inconclusive
             {:injury-id "injury/2026-10-19/reload"
              :retry-click "click-retry-repair-1"})
        _ (is (= :appended (:status pre)))
        observed (:ledger pre)
        _ (is (= 1 (observation-count observed pin)))
        calls (atom [])
        ps (requeue-ports
            {:retry (fn [r]
                      (swap! calls conj r)
                      (receipt r :queued {:retry-click (:retry-click r)}))
             :retry-click-status-fn (failed-child-status-fn "click-retry-repair-1")})
        result (coordinator/coordinate (durable observed)
                                       (:transaction initial) ps)
        ;; Replay of the completed resume: the port still reports the same
        ;; failed child, but its evidence is already ingested.
        replay (coordinator/coordinate (durable (:ledger result))
                                       (:transaction result) ps)]
    (is (= :awaiting-retry (:status result)))
    (is (= "click-retry-requeue-1" (:retry-click result)))
    (is (= 1 (observation-count (:ledger result) pin)))
    (is (= (:ledger result) (:ledger replay)))
    (is (= 1 (observation-count (:ledger replay) pin)))))

(deftest failed-child-observation-respects-spent-retry-budget-honestly
  ;; Observing a durably failed child must not buy budget: when the retry
  ;; budget is already spent and no other open injury explains the child,
  ;; the requeue loop appends the typed :abandoned/:retry-budget-spent
  ;; terminal instead of another requeue or a synthetic success.
  (let [limited (ledger/seal-ledger (assoc ledger/empty-ledger :retry-limit 1))
        history (append! limited
                         {:injury-id "injury/2026-10-19/reload"
                          :state :open :attempt 1 :cause-record-pin pin-a
                          :failure-stage :reload :failure-kind :phase-failure
                          :detected-click "click-2026-10-19-4"})
        initial (awaiting-retry history)
        ;; First failed child observed -> requeue 1 (budget 1 allows it).
        requeued (coordinator/coordinate
                   (durable (:ledger initial))
                   (:transaction initial)
                   (requeue-ports {:retry-click-status-fn
                                   (failed-child-status-fn "click-retry-repair-1")}))
        _ (is (= :awaiting-retry (:status requeued)))
        _ (is (= "click-retry-requeue-1" (:retry-click requeued)))
        ;; The requeued child also fails service-side; budget is now spent.
        calls (atom 0)
        result (coordinator/coordinate
                 (durable (:ledger requeued))
                 (:transaction requeued)
                 (requeue-ports {:retry (fn [r]
                                          (swap! calls inc)
                                          (receipt r :queued
                                                   {:retry-click (:retry-click r)}))
                                 :retry-click-status-fn
                                 (failed-child-status-fn "click-retry-requeue-1")}))]
    (is (= :abandoned (:status result)))
    (is (= :retry-budget-spent (:reason result)))
    (is (= :abandoned (-> result :ledger :entries last :state)))
    (is (zero? @calls))
    (is (= 2 (observation-count (:ledger result))))))

(deftest running-or-completed-child-is-never-observed
  ;; Only a durable :service-failed outcome is evidence: a completed, running
  ;; or unknown child must not append anything.
  (doseq [observed [{:status :completed :result {:outcome :cohort-complete}
                     :run-record-status :present}
                    {:status :completed
                     :result {:runtime-disposition {:reason :makeup-unavailable}}
                     :run-record-status :present
                     :run-record-absence :runner-did-not-return-run-record
                     :run-record-reference-conflict? false}
                    {:status :completed
                     :result {:runtime-disposition {:reason :makeup-unavailable}}
                     :run-record-status :absent
                     :run-record-absence :runner-did-not-return-run-record
                     :run-record-reference-conflict? true}
                    {:status :running}
                    {:status :not-found}
                    nil]]
    (let [initial (awaiting-retry (opened-ledger))
          result (coordinator/coordinate
                  (durable (:ledger initial)) (:transaction initial)
                  (requeue-ports {:retry-click-status-fn (fn [_] observed)}))]
      (is (= :awaiting-retry (:status result)))
      (is (= "click-retry-repair-1" (:retry-click result)))
      (is (zero? (observation-count (:ledger result)))))))

(deftest failed-child-without-evidence-pin-refuses-typedly
  ;; The legacy child carries only a message and a nil attempt-id in its
  ;; acceptance file; a status port that cannot pin the bytes it reports
  ;; must refuse by name rather than append an unsourced observation.
  (let [initial (awaiting-retry (opened-ledger))
        result (coordinator/coordinate
                (durable (:ledger initial)) (:transaction initial)
                (requeue-ports
                 {:retry-click-status-fn
                  (fn [_] {:status :failed
                           :result {:outcome :service-failed :error "x"}})}))]
    (is (= :refused (:status result)))
    (is (= :coordinator/failed-child-evidence-refused (:reason result)))
    (is (zero? (observation-count (:ledger result))))))
