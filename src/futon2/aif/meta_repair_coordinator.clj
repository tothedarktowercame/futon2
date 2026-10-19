(ns futon2.aif.meta-repair-coordinator
  "Dedicated META self-heal coordinator. It has no ordinary task/policy/fold
  dependencies; all effects are explicit author/review/enact/retry ports."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]))

(def work-order-schema :wm/meta-repair-work-order-v1)
(def receipt-schema :wm/meta-repair-port-receipt-v1)
(def transaction-schema :wm/meta-repair-transaction-v1)
(def durable-ledger-schema :wm/durable-meta-injury-ledger-v1)
(def phases [:author :review :enact :retry])
(def git-re #"[0-9a-f]{40,64}")
(def pin-re #"[0-9a-f]{64}")

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- sha? [x] (and (string? x) (boolean (re-matches git-re x))))
(defn- pin? [x]
  (and (map? x) (nonblank? (:path x))
       (string? (:sha256 x)) (boolean (re-matches pin-re (:sha256 x)))))
(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))
(defn- sealed [tag path value pin-key]
  (let [body (dissoc value pin-key)
        digest (identity/digest [tag body])]
    (assoc body pin-key {:path path :sha256 digest})))
(defn- seal-valid? [tag value pin-key]
  (let [pin (get value pin-key)]
    (and (pin? pin)
         (= (:sha256 pin)
            (identity/digest [tag (dissoc value pin-key)])))))

(defn seal-work-order [order]
  (sealed :meta-repair-work-order
          (str "meta-repair-work-orders/" (:repair-id order) "/"
               (:attempt order) ".edn")
          (assoc order :schema work-order-schema) :work-order-pin))
(defn verify-work-order [order]
  (try
    (let [sessions [(:author-session order) (:reviewer-session order)
                    (:repair-reviewer-session order)]
          defects (cond-> []
                    (not= work-order-schema (:schema order)) (conj :schema)
                    (not (seal-valid? :meta-repair-work-order order :work-order-pin))
                    (conj :seal)
                    (not (every? nonblank?
                                 [(:injury-id order) (:repair-id order)
                                  (:author order) (:reviewer order)
                                  (:repair-reviewer order) (:target-repository order)
                                  (:target-worktree order)])) (conj :identities)
                    (not (= 3 (count (set sessions)))) (conj :sessions/distinct)
                    (not (every? nonblank? sessions)) (conj :sessions/present)
                    (not (pos-int? (:attempt order))) (conj :attempt)
                    (not (pos-int? (:attempt-limit order))) (conj :attempt-limit)
                    (> (:attempt order 1) (:attempt-limit order 0))
                    (conj :attempt/exceeds-limit)
                    (not (pin? (:cause-record-pin order))) (conj :cause-pin)
                    (not (pin? (:diagnosis-evidence-pin order)))
                    (conj :diagnosis-evidence)
                    (not (and (map? (:acceptance-test order))
                              (nonblank? (get-in order [:acceptance-test :id]))
                              (vector? (get-in order [:acceptance-test :command]))
                              (seq (get-in order [:acceptance-test :command]))
                              (contains? (:acceptance-test order) :expected)))
                    (conj :acceptance-test)
                    (not (pin? (:ledger-pin order))) (conj :ledger-pin)
                    (not (pin? (:ledger-source-pin order))) (conj :ledger-source-pin))]
      (if (seq defects) (refusal :work-order/invalid {:defects defects})
          {:status :valid}))
    (catch Throwable t
      (refusal :work-order/malformed {:detail (ex-message t)}))))

(defn seal-port-receipt
  "Seal a deterministic fake/real port response. Payload is phase-specific."
  [receipt]
  (sealed :meta-repair-port-receipt
          (str "meta-repair-port-receipts/" (:repair-id receipt) "/"
               (:attempt receipt) "/" (name (:phase receipt)) ".edn")
          (assoc receipt :schema receipt-schema) :receipt-pin))
(defn verify-port-receipt [receipt expected]
  (try
    (let [defects (cond-> []
                    (not= receipt-schema (:schema receipt)) (conj :schema)
                    (not (seal-valid? :meta-repair-port-receipt receipt :receipt-pin))
                    (conj :seal)
                    (not= (:phase expected) (:phase receipt)) (conj :phase)
                    (not= (:injury-id expected) (:injury-id receipt)) (conj :injury-id)
                    (not= (:repair-id expected) (:repair-id receipt)) (conj :repair-id)
                    (not= (:attempt expected) (:attempt receipt)) (conj :attempt)
                    (not= (:predecessor-pin expected) (:predecessor-pin receipt))
                    (conj :predecessor-pin)
                    (not (#{:succeeded :failed :rejected} (:status receipt)))
                    (conj :status))]
      (if (seq defects) (refusal :port-receipt/invalid {:defects defects})
          {:status :valid}))
    (catch Throwable t
      (refusal :port-receipt/malformed {:detail (ex-message t)}))))

(defn- seal-transaction [transaction]
  (sealed :meta-repair-transaction
          (str "meta-repair-transactions/"
               (get-in transaction [:work-order :repair-id]) "/"
               (get-in transaction [:work-order :attempt]) ".edn")
          (assoc transaction :schema transaction-schema) :transaction-pin))
(defn verify-transaction [transaction]
  (try
    (if (and (= transaction-schema (:schema transaction))
             (seal-valid? :meta-repair-transaction transaction :transaction-pin)
             (= :valid (:status (verify-work-order (:work-order transaction))))
             (vector? (:receipts transaction)))
      {:status :valid}
      (refusal :transaction/invalid))
    (catch Throwable t
      (refusal :transaction/malformed {:detail (ex-message t)}))))

(defn- latest-transition [history injury-id]
  (last (filter #(and (= ledger/transition-schema (:schema %))
                      (= injury-id (:injury-id %))) (:entries history))))
(defn- active-entry [durable injury-id]
  (get-in (ledger/active-injuries (:ledger durable)) [:injuries injury-id]))
(defn- cause-pin [entry]
  (or (:new-cause-pin entry) (:cause-record-pin entry)))

(defn prepare
  "Build a dedicated work order from a disk-pinned active injury. No active
  injury means an explicit stop, never fallback to ordinary work."
  [durable options]
  (try
    (let [history (:ledger durable)
          active-result (ledger/active-injuries history)
          active (:injuries active-result)
          injury-id (or (:injury-id options) (first (sort (keys active))))
          entry (get active injury-id)
          attempt (:attempt entry)
          repair-id (or (:repair-id entry)
                        (str "repair/" (subs (identity/digest
                                             [:meta-repair injury-id]) 0 40)))
          order (seal-work-order
                 (merge (select-keys options
                                     [:diagnosis-evidence-pin :acceptance-test
                                      :author :reviewer :repair-reviewer
                                      :author-session :reviewer-session
                                      :repair-reviewer-session
                                      :target-repository :target-worktree])
                        {:injury-id injury-id :repair-id repair-id :attempt attempt
                         :attempt-limit (:attempt-limit history)
                         :cause-record-pin (cause-pin entry)
                         :ledger-pin (:ledger-pin history)
                         :ledger-source-pin (:source-pin durable)}))
          defects (cond-> []
                    (not= durable-ledger-schema (:schema durable))
                    (conj :durable-ledger/schema)
                    (not= :loaded (:status durable)) (conj :durable-ledger/status)
                    (not (pin? (:source-pin durable))) (conj :durable-ledger/source-pin)
                    (not= :valid (:status (ledger/validate-ledger history)))
                    (conj :ledger/invalid)
                    (not= :computed (:status active-result)) (conj :active-set/invalid)
                    (empty? active) (conj :active-injury/required)
                    (nil? entry) (conj :active-injury/not-found)
                    (not= :valid (:status (verify-work-order order)))
                    (conj :work-order/invalid))]
      (if (seq defects)
        (refusal :coordinator/preparation-refused {:defects defects})
        {:status :prepared :work-order order
         :transaction (seal-transaction {:status :active :work-order order
                                         :receipts []})}))
    (catch Throwable t
      (refusal :coordinator/preparation-malformed {:detail (ex-message t)}))))

(defn- receipt-for [transaction phase]
  (some #(when (= phase (:phase %)) %) (:receipts transaction)))
(defn- append-receipt [transaction receipt]
  (seal-transaction (update transaction :receipts conj receipt)))
(defn- expected [order phase predecessor-pin]
  {:phase phase :injury-id (:injury-id order) :repair-id (:repair-id order)
   :attempt (:attempt order) :predecessor-pin predecessor-pin})
(defn- invoke-once [transaction phase predecessor-pin port]
  (if-let [existing (receipt-for transaction phase)]
    {:status :recorded :receipt existing :transaction transaction}
    (try
      (let [response (port {:work-order (:work-order transaction)
                            :phase phase :predecessor-pin predecessor-pin})
            checked (verify-port-receipt
                     response (expected (:work-order transaction)
                                        phase predecessor-pin))]
        (if-not (= :valid (:status checked))
          (refusal :coordinator/port-receipt-refused
                   {:phase phase :receipt-defects (:defects checked)
                    :transaction transaction})
          {:status :accepted :receipt response
           :transaction (append-receipt transaction response)}))
      (catch Throwable t
        (refusal :coordinator/port-threw
                 {:phase phase :detail (ex-message t)
                  :transaction transaction})))))

(defn- append-transition! [history event]
  (let [result (ledger/append-transition history event)]
    (if (= :appended (:status result)) result
        (refusal :coordinator/ledger-transition-refused
                 {:ledger-result result :ledger history}))))

(defn- stop! [result] (throw (ex-info "coordinator stop" {:result result})))
(defn- accepted! [result]
  (when (= :refused (:status result)) (stop! result))
  result)
(defn- held! [reason phase history transaction receipt]
  (stop! {:status :held :reason reason :phase phase :ledger history
          :transaction transaction :receipt receipt}))
(defn- checkpoint! [ports phase history transaction]
  (when (= phase (:stop-after ports))
    (stop! {:status :checkpoint :phase phase :ledger history
            :transaction transaction})))

(defn coordinate
  "Run or resume one dedicated transaction. Every effect receipt is retained,
  so a resumed transaction never redispatches an already-recorded phase."
  [durable transaction ports]
  (try
    (let [order (:work-order transaction)
          injury-id (:injury-id order)
          repair-id (:repair-id order)
          attempt (:attempt order)
          history0 (:ledger durable)
          active (active-entry durable injury-id)
          defects (cond-> []
                    (not= :valid (:status (verify-transaction transaction)))
                    (conj :transaction)
                    (not= :valid (:status (ledger/validate-ledger history0)))
                    (conj :ledger)
                    (nil? active) (conj :active-injury)
                    (not= attempt (:attempt active)) (conj :attempt)
                    (and (:repair-id active) (not= repair-id (:repair-id active)))
                    (conj :repair-id))]
      (when (seq defects)
        (stop! (refusal :coordinator/input-refused {:defects defects})))

      ;; Claim is the only coordinator-created boundary without a port receipt.
      (let [latest0 (latest-transition history0 injury-id)
            claim (if (#{:open :reopened} (:state latest0))
                    (accepted!
                     (append-transition!
                      history0 {:injury-id injury-id :state :claimed
                                :attempt attempt :repair-id repair-id
                                :algorithm "dedicated-meta-self-heal"
                                :author-session (:author-session order)
                                :reviewer-session (:reviewer-session order)
                                :repair-reviewer-session
                                (:repair-reviewer-session order)
                                :work-order-pin (:work-order-pin order)}))
                    {:ledger history0})
            history1 (:ledger claim)
            claim-entry (latest-transition history1 injury-id)
            _ (when-not (contains? #{:claimed :authored :reviewed :enacted}
                                   (:state claim-entry))
                (stop! (refusal :coordinator/resume-state-refused
                                {:state (:state claim-entry)})))

            _ (checkpoint! ports :claim history1 transaction)

            author-result (accepted!
                           (invoke-once transaction :author
                                        (:entry-pin claim-entry) (:author ports)))
            author-receipt (:receipt author-result)
            tx1 (:transaction author-result)]
        (when-not (= :succeeded (:status author-receipt))
          (held! :author-failed :author history1 tx1 author-receipt))
        (let [patch-sha (get-in author-receipt [:payload :patch-sha])
              worktree (get-in author-receipt [:payload :worktree])
              _ (when-not (and (sha? patch-sha) (nonblank? worktree))
                  (stop! (refusal :coordinator/author-payload-refused
                                  {:ledger history1 :transaction tx1})))
              authored (if (= :claimed (:state (latest-transition history1 injury-id)))
                         (accepted!
                          (append-transition!
                           history1 {:injury-id injury-id :state :authored
                                     :attempt attempt :repair-id repair-id
                                     :patch-sha patch-sha :worktree worktree
                                     :port-receipt-pin (:receipt-pin author-receipt)}))
                         {:ledger history1})
              history2 (:ledger authored)
              _ (checkpoint! ports :author history2 tx1)
              review-result (accepted!
                             (invoke-once tx1 :review (:receipt-pin author-receipt)
                                          (:review ports)))
              review-receipt (:receipt review-result)
              tx2 (:transaction review-result)]
          (when-not (= :succeeded (:status review-receipt))
            (held! :review-rejected :review history2 tx2 review-receipt))
          (when-not (= patch-sha (get-in review-receipt [:payload :patch-sha]))
            (stop! (refusal :coordinator/review-patch-substitution
                            {:ledger history2 :transaction tx2})))
          (let [reviewed (if (= :authored (:state (latest-transition history2 injury-id)))
                           (accepted!
                            (append-transition!
                             history2 {:injury-id injury-id :state :reviewed
                                       :attempt attempt :repair-id repair-id
                                       :verdict :approved
                                       :reviewer-sha
                                       (get-in review-receipt [:payload :reviewer-sha])
                                       :reviewer-session (:reviewer-session order)
                                       :port-receipt-pin (:receipt-pin review-receipt)}))
                           {:ledger history2})
                history3 (:ledger reviewed)
                _ (checkpoint! ports :review history3 tx2)
                enact-result (accepted!
                              (invoke-once tx2 :enact (:receipt-pin review-receipt)
                                           (:enact ports)))
                enact-receipt (:receipt enact-result)
                tx3 (:transaction enact-result)]
            (when-not (= :succeeded (:status enact-receipt))
              (held! :enact-failed :enact history3 tx3 enact-receipt))
            (let [enact-payload (:payload enact-receipt)
                  retry-click (:retry-click enact-payload)]
              (when-not (and (= patch-sha (:patch-sha enact-payload))
                             (nonblank? retry-click))
                (stop! (refusal :coordinator/enact-payload-refused
                                {:ledger history3 :transaction tx3})))
              (let [enacted (if (= :reviewed
                                    (:state (latest-transition history3 injury-id)))
                              (accepted!
                               (append-transition!
                                history3 (merge
                                          {:injury-id injury-id :state :enacted
                                           :attempt attempt :repair-id repair-id
                                           :retry-click retry-click
                                           :port-receipt-pin
                                           (:receipt-pin enact-receipt)}
                                          (select-keys enact-payload
                                                       [:reload-receipt
                                                        :integration-commit]))))
                              {:ledger history3})
                    history4 (:ledger enacted)
                    _ (checkpoint! ports :enact history4 tx3)
                    retry-result (accepted!
                                  (invoke-once tx3 :retry (:receipt-pin enact-receipt)
                                               (:retry ports)))
                    retry-receipt (:receipt retry-result)
                    tx4 (:transaction retry-result)
                    retry-payload (:payload retry-receipt)]
                (when-not (and (= repair-id (:repair-id retry-payload))
                               (= patch-sha (:patch-sha retry-payload))
                               (= retry-click (:retry-click retry-payload)))
                  (stop! (refusal :coordinator/retry-attribution-refused
                                  {:ledger history4 :transaction tx4})))
                (if (= :succeeded (:status retry-receipt))
                  (let [closed (accepted!
                                (append-transition!
                                 history4 {:injury-id injury-id :state :discharged
                                           :attempt attempt :repair-id repair-id
                                           :patch-sha patch-sha
                                           :retry-click retry-click
                                           :outcome (or (:outcome retry-payload) :success)
                                           :port-receipt-pin
                                           (:receipt-pin retry-receipt)}))]
                    {:status :discharged :ledger (:ledger closed) :transaction tx4})
                  (let [reopened (accepted!
                                  (ledger/append-retry-result
                                   history4 {:injury-id injury-id :repair-id repair-id
                                             :patch-sha patch-sha
                                             :retry-click retry-click
                                             :new-cause-pin (:new-cause-pin retry-payload)
                                             :port-receipt-pin
                                             (:receipt-pin retry-receipt)}))]
                    {:status (get-in reopened [:entry :state])
                     :ledger (:ledger reopened) :transaction tx4}))))))))
    (catch clojure.lang.ExceptionInfo e
      (or (:result (ex-data e))
          (refusal :coordinator/malformed {:detail (ex-message e)})))
    (catch Throwable t
      (refusal :coordinator/malformed {:detail (ex-message t)}))))
