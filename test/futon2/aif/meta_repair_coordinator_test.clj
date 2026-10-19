(ns futon2.aif.meta-repair-coordinator-test
  (:require [clojure.test :refer [deftest is testing]]
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
     :retry (fn [r] (receipt r :succeeded
                             {:repair-id (get-in r [:work-order :repair-id])
                              :patch-sha sha-a :retry-click "click-retry-repair-1"
                              :outcome :grounded-change}))}
    overrides)))

(defn- prepared [history options]
  (coordinator/prepare (durable history) options))

(deftest happy-path-is-dedicated-and-discharges-attributably
  (let [counts (atom {:author 0 :review 0 :enact 0 :retry 0 :forbidden 0})
        wrapped (into {}
                      (for [[phase port] (ports)]
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

(deftest rejection-and-port-failures-stop-without-silent-advance
  (doseq [[label override expected-state expected-reason]
          [[:author {:author (fn [r] (receipt r :failed {:reason :author-crash}))}
            :claimed :author-failed]
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

(deftest recorded-failure-is-not-dispatched-twice-on-resume
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
    (is (= :held (:status second)))
    (is (= 1 @calls))
    (is (= (:receipt-pin (:receipt first)) (:receipt-pin (:receipt second))))))

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
