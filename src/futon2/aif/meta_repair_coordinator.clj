(ns futon2.aif.meta-repair-coordinator
  "Dedicated META self-heal coordinator. It has no ordinary task/policy/fold
  dependencies; all effects are explicit author/review/enact/retry ports."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
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
                    (and (some? (:canonical-base-sha order))
                         (not (sha? (:canonical-base-sha order))))
                    (conj :canonical-base-sha)
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
                    (not (#{:succeeded :failed :rejected :queued} (:status receipt)))
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
(defn- authored-transition [history injury-id repair-id attempt]
  (last (filter #(and (= :authored (:state %))
                      (= injury-id (:injury-id %))
                      (= repair-id (:repair-id %))
                      (= attempt (:attempt %)))
                (:entries history))))
(defn- phase-transition [history injury-id repair-id attempt state]
  (let [matches (filter #(and (= state (:state %))
                              (= injury-id (:injury-id %))
                              (= repair-id (:repair-id %))
                              (= attempt (:attempt %)))
                        (:entries history))]
    ;; Requeues are also :enacted transitions, but they do not admit a new
    ;; enact-port receipt. Prefer the phase transition carrying its receipt;
    ;; for legacy histories without pins, the first transition is the phase
    ;; admission and later enacted transitions are requeues.
    (or (last (filter :port-receipt-pin matches))
        (first matches))))
(defn- unadmitted-phase-receipt
  "Refuse a transaction receipt when the ledger has already advanced past its
  phase without admitting that exact receipt. Older review entries fall back
  to reviewer SHA; older enact entries fall back to integration commit and
  retry identity when no receipt pin was recorded."
  [history receipt injury-id repair-id attempt phase]
  (let [[state fallback-keys reason]
        (case phase
          :review [:reviewed [:reviewer-sha]
                   :coordinator/unadmitted-review-receipt]
          :enact [:enacted [:integration-commit :retry-click]
                  :coordinator/unadmitted-enact-receipt])
        admitted (phase-transition history injury-id repair-id attempt state)
        admitted-pin (:port-receipt-pin admitted)
        received-pin (:receipt-pin receipt)
        recorded-keys (filterv #(contains? admitted %) fallback-keys)
        admitted-fallback (select-keys admitted recorded-keys)
        received-fallback (select-keys (:payload receipt) recorded-keys)
        admitted? (cond
                    admitted-pin (= admitted-pin received-pin)
                    (seq admitted-fallback)
                    (= admitted-fallback received-fallback)
                    :else false)]
    (when-not admitted?
      (refusal reason
               {:phase phase :injury-id injury-id :repair-id repair-id
                :attempt attempt :admitted-receipt-pin admitted-pin
                :received-receipt-pin received-pin
                :admitted-values admitted-fallback
                :received-values received-fallback}))))
(defn- resume-identity-refusal
  "The Lean law MetaRepairResumeIdentity.resumeIdentityOK: after :claimed,
  the transaction's retained author receipt is the ledger-admitted receipt."
  [history transaction injury-id repair-id attempt state]
  (when (#{:authored :reviewed :enacted} state)
    (let [ledger-author (authored-transition history injury-id repair-id attempt)
          transaction-author (some #(when (= :author (:phase %)) %)
                                   (:receipts transaction))
          ledger-patch-sha (:patch-sha ledger-author)
          transaction-patch-sha (get-in transaction-author [:payload :patch-sha])]
      (when-not (and ledger-author
                     transaction-author
                     (= (:port-receipt-pin ledger-author)
                        (:receipt-pin transaction-author))
                     (= ledger-patch-sha transaction-patch-sha))
        (refusal :coordinator/resume-identity-refused
                 {:state state
                  :ledger-patch-sha ledger-patch-sha
                  :transaction-patch-sha transaction-patch-sha
                  :ledger history
                  :transaction transaction})))))
(defn- active-entry [durable injury-id authority-verifier]
  (get-in (ledger/effective-active-injuries (:ledger durable) authority-verifier)
          [:injuries injury-id]))
(defn- cause-pin [history entry]
  ;; A claimed-author-failed reopen carries the sealed failed-port receipt,
  ;; not a new failure record.  Recover the original causal pin from this
  ;; injury's append-only history when preparing its next attempt.
  (or (:new-cause-pin entry) (:cause-record-pin entry)
      (some->> (:entries history)
               (filter #(and (= (:injury-id entry) (:injury-id %))
                             (or (:new-cause-pin %) (:cause-record-pin %))))
               last
               (#(or (:new-cause-pin %) (:cause-record-pin %))))))

(defn prepare
  "Build a dedicated work order from a disk-pinned active injury. No active
  injury means an explicit stop, never fallback to ordinary work."
  ([durable options] (prepare durable options (constantly false)))
  ([durable options authority-verifier]
  (try
    (let [history (:ledger durable)
          active-result (ledger/effective-active-injuries history authority-verifier)
          active (:injuries active-result)
          injury-id (or (:injury-id options) (first (sort (keys active))))
          entry (get active injury-id)
          attempt (:attempt entry)
          repair-id (or (:repair-id entry)
                        (str "repair/" (subs (identity/digest
                                             [:meta-repair injury-id]) 0 40)))
          target-worktree (or (:target-worktree options)
                              (when-let [root (:target-worktree-root options)]
                                (str (str/replace root #"/+$" "") "/"
                                     repair-id "/" attempt)))
          order (seal-work-order
                 (merge (select-keys options
                                     [:diagnosis-evidence-pin :acceptance-test
                                      :author :reviewer :repair-reviewer
                                      :author-session :reviewer-session
                                      :repair-reviewer-session
                                      :target-repository :canonical-base-sha])
                        {:injury-id injury-id :repair-id repair-id :attempt attempt
                         :target-worktree target-worktree
                         ;; F38 (2026-10-20): the effective limit, not the
                         ;; ledger's raw :attempt-limit, so that a corrected
                         ;; reopen's attempt (4 under a limit of 3 plus two
                         ;; refused receipts) passes verify-work-order.
                         :attempt-limit (ledger/effective-attempt-limit history injury-id)
                         :cause-record-pin (cause-pin history entry)
                         :ledger-pin (:ledger-pin history)
                         :ledger-source-pin (:source-pin durable)}))
          defects (cond-> []
                    (not= durable-ledger-schema (:schema durable))
                    (conj :durable-ledger/schema)
                    (not= :loaded (:status durable)) (conj :durable-ledger/status)
                    (not (pin? (:source-pin durable))) (conj :durable-ledger/source-pin)
                    (not= :valid (:status (ledger/validate-ledger history authority-verifier)))
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
      (refusal :coordinator/preparation-malformed {:detail (ex-message t)})))))

(defn- receipt-for [transaction phase]
  (some #(when (= phase (:phase %)) %) (:receipts transaction)))
(defn- append-receipt [transaction receipt]
  (seal-transaction (update transaction :receipts conj receipt)))
(defn- expected [order phase predecessor-pin]
  {:phase phase :injury-id (:injury-id order) :repair-id (:repair-id order)
   :attempt (:attempt order) :predecessor-pin predecessor-pin})
(defn- invoke-once
  ([transaction phase predecessor-pin port]
   (invoke-once transaction phase predecessor-pin port {}))
  ([transaction phase predecessor-pin port extra]
   (if-let [existing (receipt-for transaction phase)]
     {:status :recorded :receipt existing :transaction transaction}
     (try
       (let [response (port (merge {:work-order (:work-order transaction)
                                    :transaction transaction
                                    :phase phase
                                    :predecessor-pin predecessor-pin}
                                   extra))
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
                   :transaction transaction}))))))

(defn- append-transition! [history event authority-verifier]
  (let [result (ledger/append-transition history event authority-verifier)]
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

(defn- requeue-phase [n]
  (keyword (str "retry-requeue-" n)))

(defn- entry-classification-pin [entry]
  (or (:new-cause-pin entry) (:cause-record-pin entry) (:record-pin entry)))

(defn- pinned-run-record-click [pin]
  (try
    (let [file (io/file (:path pin))]
      (when (.isFile file)
        (let [bytes (java.nio.file.Files/readAllBytes (.toPath file))]
          (when (= (:sha256 pin) (load-identity/sha256 bytes))
            (let [record (edn/read-string (String. bytes "UTF-8"))]
              (or (:click/id record) (:click-id record)))))))
    (catch Throwable _ nil)))

(defn- already-ingested-other-injury-pin
  [history injury-id retry-click]
  (let [enacted (last (filter #(and (= injury-id (:injury-id %))
                                    (= :enacted (:state %))
                                    (= retry-click (:retry-click %)))
                              (:entries history)))]
    (when enacted
      (some (fn [entry]
              (let [pin (entry-classification-pin entry)]
                (when (and (> (:ordinal entry) (:ordinal enacted))
                           (not= injury-id (:injury-id entry))
                           (pin? pin)
                           (= retry-click (pinned-run-record-click pin)))
                  pin)))
            (rseq (:entries history))))))

(defn- retry-child-observation
  "Read durable evidence for one meta-retry child. First reconcile a pinned
   run record already classified onto another injury; otherwise consult the
   narrow :retry-click-status-fn fallback for service failure or completed
   typed non-success without a run record. Other states return nil."
  [history ports injury-id retry-click]
  (if-let [ingested-pin (already-ingested-other-injury-pin
                         history injury-id retry-click)]
    {:status :completed :evidence-pin ingested-pin}
    (when-let [status-fn (:retry-click-status-fn ports)]
      (let [observed (try (status-fn retry-click) (catch Throwable _ nil))]
        (when (map? observed)
          (let [service-failed? (and (= :failed (:status observed))
                                     (= :service-failed
                                        (get-in observed [:result :outcome])))
                completed-without-run-record?
                (and (= :completed (:status observed))
                     (keyword? (get-in observed [:result :runtime-disposition :reason]))
                     (= :absent (:run-record-status observed))
                     (= :runner-did-not-return-run-record
                        (:run-record-absence observed))
                     (false? (:run-record-reference-conflict? observed)))]
            (cond
              (not (or service-failed? completed-without-run-record?)) nil
              (not (pin? (:evidence-pin observed)))
              (refusal :coordinator/failed-child-evidence-refused
                       {:retry-click retry-click
                        :result (select-keys (:result observed) [:outcome :error])})
              :else observed)))))))

(defn- observe-retry-child!
  "Append the :retry-inconclusive observation terminal ingestion would have
   appended for a child that failed service-side before any run record
   existed (one-click frontier 2026-10-22: wm-requeue-7-4-repair/239cf988
   died at the stale-runner-source refusal, before persist-run-record!, so
   the ledger never saw it). The observation cites the child's own durable
   acceptance evidence by pin; no failure or success is synthesized. A
   retry-attributed observation is never appended twice, while a pin already
   consumed by a different injury may receive the missing retry attribution.
   Returns the advanced history, or nil when there is nothing durable to observe."
  [history ports injury-id retry-click authority-verifier]
  (when-let [observed (retry-child-observation
                       history ports injury-id retry-click)]
    (if (= :refused (:status observed))
      (stop! observed)
      (let [pin (:evidence-pin observed)]
        (when-not (some #(and (= :retry-inconclusive (:observation-kind %))
                              (= pin (:record-pin %))
                              (= injury-id (:injury-id %))
                              (= retry-click (:retry-click %)))
                        (:entries history))
          (let [appended (ledger/append-observation
                          history pin :retry-inconclusive
                          {:injury-id injury-id :retry-click retry-click}
                          authority-verifier)]
            (if (= :appended (:status appended))
              (:ledger appended)
              (stop! (refusal :coordinator/failed-child-observation-refused
                              {:ledger-result appended
                               :retry-click retry-click})))))))))

(defn- last-receipt-pin [transaction]
  (:receipt-pin (last (:receipts transaction))))

(defn- coordinate-queued-retry
  [history transaction ports injury-id repair-id attempt authority-verifier]
  (loop [history history
         transaction transaction]
    (let [latest-before-observation (latest-transition history injury-id)
          pending-before-observation?
          (ledger/pending-requeue history injury-id authority-verifier)
          observed-history
          (when-not pending-before-observation?
            (observe-retry-child! history ports injury-id
                                  (:retry-click latest-before-observation)
                                  authority-verifier))
          history (or observed-history history)
          latest (latest-transition history injury-id)
          n (or (ledger/requeue-ordinal history injury-id) 0)
          pending-requeue? (ledger/pending-requeue history injury-id
                                                   authority-verifier)
          queued (when (and (pos? n) (not pending-requeue?))
                   (accepted!
                    (invoke-once transaction (requeue-phase n)
                                 (last-receipt-pin transaction) (:retry ports)
                                 {:retry-click (:retry-click latest)
                                  :requeue-ordinal n})))
          retry-receipt (:receipt queued)
          transaction (or (:transaction queued) transaction)
          authored (authored-transition history injury-id repair-id attempt)
          patch-sha (:patch-sha authored)
          enacted (latest-transition history injury-id)
          enacted-patch (:patch-sha enacted)
          integration-commit (:integration-commit enacted)
          _ (when-not authored
              (stop! (refusal :coordinator/retry-authored-transition-unavailable
                              {:injury-id injury-id :repair-id repair-id
                               :attempt attempt})))
          _ (when-not (and (string? patch-sha)
                           (re-matches #"[0-9a-fA-F]{40}" patch-sha))
              (stop! (refusal :coordinator/retry-authored-patch-invalid
                              {:injury-id injury-id :repair-id repair-id
                               :attempt attempt :patch-sha patch-sha})))
          disagreement (cond
                         (and (some? enacted-patch)
                              (not= patch-sha enacted-patch))
                         [:patch-sha enacted-patch]

                         (and (some? integration-commit)
                              (not= patch-sha integration-commit))
                         [:integration-commit integration-commit]

                         (and (nil? enacted-patch)
                              (nil? integration-commit))
                         [:integration-commit nil])
          _ (when disagreement
              (stop! (refusal :coordinator/retry-enacted-patch-disagrees
                              {:injury-id injury-id :repair-id repair-id
                               :attempt attempt :field (first disagreement)
                               :expected patch-sha
                               :actual (second disagreement)})))]
      (when (and retry-receipt
                 (not (and (= :queued (:status retry-receipt))
                           (= (:retry-click latest)
                              (get-in retry-receipt [:payload :retry-click])))))
        (stop! (refusal :coordinator/retry-attribution-refused
                        {:ledger history :transaction transaction})))
      (if-not pending-requeue?
        {:status :awaiting-retry
         :ledger history
         :transaction transaction
         :retry-click (:retry-click latest)}
        (let [next-n (inc n)
              retry-click-id (:retry-click-id ports)
              _ (when-not (fn? retry-click-id)
                  (stop! (refusal :coordinator/retry-click-id-unavailable
                                  {:ledger history :transaction transaction})))
              new-id (retry-click-id repair-id attempt next-n patch-sha)
              requeued (accepted! (ledger/append-requeue history injury-id new-id
                                                         authority-verifier))
              history' (:ledger requeued)]
          (if (= :abandoned (get-in requeued [:entry :state]))
            {:status :abandoned :reason :retry-budget-spent
             :ledger history' :transaction transaction}
            ;; Persist the ledger entry before calling the retry port. A resume
            ;; queues an appended entry that lacks a receipt; if the port ran but
            ;; transaction persistence failed, its stable click id names the same
            ;; meta-retry child on the repeated call.
            (recur history' transaction)))))))

(defn coordinate
  "Run or resume one dedicated transaction. Every effect receipt is retained,
  so a resumed transaction never redispatches an already-recorded phase."
  ([durable transaction ports] (coordinate durable transaction ports (constantly false)))
  ([durable transaction ports authority-verifier]
  (try
    (let [order (:work-order transaction)
          injury-id (:injury-id order)
          repair-id (:repair-id order)
          attempt (:attempt order)
          history0 (:ledger durable)
          active (active-entry durable injury-id authority-verifier)
          defects (cond-> []
                    (not= :valid (:status (verify-transaction transaction)))
                    (conj :transaction)
                    (not= :valid (:status (ledger/validate-ledger history0 authority-verifier)))
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
                                :work-order-pin (:work-order-pin order)}
                      authority-verifier))
                    {:ledger history0})
            history1 (:ledger claim)
            claim-entry (latest-transition history1 injury-id)
            _ (when-not (contains? #{:claimed :authored :reviewed :enacted}
                                   (:state claim-entry))
                (stop! (refusal :coordinator/resume-state-refused
                                {:state (:state claim-entry)})))

            _ (when-let [refusal (resume-identity-refusal
                                  history1 transaction injury-id repair-id attempt
                                  (:state claim-entry))]
                (stop! refusal))

            _ (checkpoint! ports :claim history1 transaction)

            author-result (accepted!
                           (invoke-once transaction :author
                                        (:entry-pin claim-entry) (:author ports)))
            author-receipt (:receipt author-result)
            tx1 (:transaction author-result)]
        (when-not (= :succeeded (:status author-receipt))
          ;; A failed sealed author answer is evidence for a fresh attempt,
          ;; not a terminal hold.  append-retry-result supplies the existing
          ;; attempt-limit behavior and validates the claim predecessor.
          (let [reopened (accepted!
                          (ledger/append-retry-result
                           history1 {:injury-id injury-id :repair-id repair-id
                                     :reason :author-failed
                                     :port-receipt-pin
                                     (:receipt-pin author-receipt)}
                           authority-verifier))]
            (held! :author-failed :author (:ledger reopened) tx1 author-receipt)))
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
                                     :port-receipt-pin (:receipt-pin author-receipt)}
                           authority-verifier))
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
          (when (not= :authored (:state (latest-transition history2 injury-id)))
            (when-let [refusal (unadmitted-phase-receipt
                                history2 review-receipt injury-id repair-id
                                attempt :review)]
              (stop! refusal)))
          (let [reviewed (if (= :authored (:state (latest-transition history2 injury-id)))
                           (accepted!
                            (append-transition!
                             history2 {:injury-id injury-id :state :reviewed
                                       :attempt attempt :repair-id repair-id
                                       :verdict :approved
                                       :reviewer-sha
                                       (get-in review-receipt [:payload :reviewer-sha])
                                       :reviewer-session (:reviewer-session order)
                                       :port-receipt-pin (:receipt-pin review-receipt)}
                             authority-verifier))
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
              (when (not= :reviewed (:state (latest-transition history3 injury-id)))
                (when-let [refusal (unadmitted-phase-receipt
                                    history3 enact-receipt injury-id repair-id
                                    attempt :enact)]
                  (stop! refusal)))
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
                                                        :integration-commit]))
                                authority-verifier))
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
                (cond
                  (= :queued (:status retry-receipt))
                  (coordinate-queued-retry history4 tx4 ports injury-id repair-id
                                           attempt authority-verifier)

                  (= :succeeded (:status retry-receipt))
                  (let [closed (accepted!
                                (append-transition!
                                 history4 {:injury-id injury-id :state :discharged
                                           :attempt attempt :repair-id repair-id
                                           :patch-sha patch-sha
                                           :retry-click retry-click
                                           :outcome (or (:outcome retry-payload) :success)
                                           :port-receipt-pin
                                           (:receipt-pin retry-receipt)}
                                 authority-verifier))]
                    {:status :discharged :ledger (:ledger closed) :transaction tx4})

                  :else
                  (let [reopened (accepted!
                                  (ledger/append-retry-result
                                   history4 {:injury-id injury-id :repair-id repair-id
                                             :patch-sha patch-sha
                                             :retry-click retry-click
                                             :new-cause-pin (:new-cause-pin retry-payload)
                                             :port-receipt-pin
                                             (:receipt-pin retry-receipt)}
                                   authority-verifier))]
                    {:status (get-in reopened [:entry :state])
                     :ledger (:ledger reopened) :transaction tx4}))))))))
    (catch clojure.lang.ExceptionInfo e
      (or (:result (ex-data e))
          (refusal :coordinator/malformed {:detail (ex-message e)})))
    (catch Throwable t
      (refusal :coordinator/malformed {:detail (ex-message t)})))))
