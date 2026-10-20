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
     :retry-click-id (fn [_repair-id _attempt ordinal]
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
