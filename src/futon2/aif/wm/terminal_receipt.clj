(ns futon2.aif.wm.terminal-receipt
  "PROOF-2b step ⟨0⟩0: every click's run record ends in EXACTLY ONE terminal
  receipt, either an :action-receipt (the click selected and enacted work)
  or a :failure (an abstention, a refusal to select, or an exception before
  selection reached the record). Never both, never neither; persisting the
  run record never fails because of its receipt.

  The receipt reuses fields the run record already holds (click id,
  selection-event target, the decision's selection-law action kind and
  g-term-decomposition, the d-task-enactment revision pair, the
  participants' reviewer-of-record, the abstention carrier, the failure
  block, startedAt); no decision logic is added here — only a reading of
  what the record says into the one receipt shape. Pure: `terminal-receipt`
  never reads the substrate, never writes, and throws
  {:failure-kind :terminal-receipt-invalid} on a record that indicates both
  a selection and a failure, or neither.

  persist-run-record! (futon2.aif.full-loop-runner) threads the RESULT's
  already-computed :outcome into the record map before calling
  `terminal-receipt`, so the receipt's :outcome is the outcome the close
  already recorded, not a new judgement."
  (:require [clojure.string :as str]))

(defn target-kind
  "The target's kind from its id prefix, as the target field's enumerators
  name them: M- mission, E- excursion, T- ticket, A- algorithm; a target
  with none of these prefixes is typed :unclassified, never guessed."
  [target]
  (let [s (str target)]
    (cond
      (str/starts-with? s "M-") :mission
      (str/starts-with? s "E-") :excursion
      (str/starts-with? s "T-") :ticket
      (str/starts-with? s "A-") :algorithm
      :else :unclassified)))

(defn- selected-target
  "The target the record's :selection-event says was selected, or nil. The
  event verb (:cascade-selected) is the record's own; no re-derivation."
  [record]
  (when (and (= :cascade-selected (:event (:selection-event record)))
             (not (str/blank? (str (:target (:selection-event record))))))
    (:target (:selection-event record))))

(defn- abstained?
  "True when the record's :decision carries an abstention carrier with
  :status :abstained (the D8/AR-16 sorry-cell reading persist-run-record!
  already writes)."
  [record]
  (= :abstained (get-in record [:decision :abstention :status])))

(defn- failed?
  "True when the record carries a failure with a :kind (run-record-failure's
  {:absent :no-failure} reads false here) or an abstention."
  [record]
  (boolean (or (:kind (:failure record)) (abstained? record))))

(defn action-receipt
  "The :action-receipt for a selected-and-enacted RECORD: click id, target
  and its kind, the action kind and G terms the decision already records
  (verbatim), the close's outcome, and for a mutating action the commit sha
  from the d-task-enactment revision pair and the participants'
  reviewer-of-record. Absent values stay typed-absent, never invented."
  [record]
  {:kind :action-receipt
   :click-id (:click/id record)
   :target (selected-target record)
   :target-kind (target-kind (selected-target record))
   :action-kind (or (get-in record [:decision :selection-law
                                    :per-policy-argmax :action :kind])
                    {:absent :no-recorded-action-kind})
   :G (get-in record [:decision :g-term-decomposition])
   :outcome (:outcome record)
   :commit (get-in record [:d-task-enactment :verification :revision-pair :after])
   :reviewer (get-in record [:participants :roles :reviewer-of-record :identity])})

(defn failure-receipt
  "The :failure receipt for a RECORD that abstained, refused to select, or
  closed on an exception before selection: a stable id over the run id and
  the failure kind, the failure kind the record already states (:abstained
  for an abstention, carrying the abstention status), this builder as the
  source, and the record's own startedAt as :at — no new clock is read."
  [record]
  (let [kind (cond (abstained? record) :abstained
                   (:kind (:failure record)) (:kind (:failure record))
                   :else :no-terminal-state)
        status (get-in record [:decision :abstention :status])]
    (cond-> {:kind :failure
             :id (str (:run/id record) "-" (name kind))
             :failure-kind kind
             :source "futon2.aif.wm.terminal-receipt/terminal-receipt"
             :at (:startedAt record)
             :abstention-status (when (abstained? record) status)}
      ;; A click that selected and then failed names what it failed on.
      (selected-target record) (assoc :target (selected-target record)
                                      :target-kind (target-kind (selected-target record))))))

(defn validate-receipt
  "RECEIPT as passed through, or throw {:failure-kind
  :terminal-receipt-invalid}. Exactly one of the two kinds, and each kind's
  required keys present and non-nil: an :action-receipt needs :click-id,
  :target, :target-kind, :action-kind, :G, :outcome; a :failure needs :id,
  :failure-kind, :source, :at. A map carrying both kinds' markers, or
  neither, is invalid — this is the same exactly-one contract
  `terminal-receipt` enforces on the record."
  [receipt]
  (let [kinds (cond-> #{}
                (contains? receipt :kind) (conj (:kind receipt))
                ;; both-kinds markers: an action-receipt key AND a failure key
                (and (contains? receipt :outcome) (contains? receipt :failure-kind))
                (conj :action-receipt :failure))]
    (when (not= 1 (count kinds))
      (throw (ex-info "terminal receipt is not exactly one kind"
                      {:failure-kind :terminal-receipt-invalid
                       :kinds (vec kinds)})))
    (let [missing (case (:kind receipt)
                    :action-receipt
                    (for [k [:kind :click-id :target :target-kind :action-kind :G :outcome]
                          :when (nil? (get receipt k))] k)
                    :failure
                    (for [k [:kind :id :failure-kind :source :at]
                          :when (nil? (get receipt k))] k)
                    [:kind])]
      (when (seq missing)
        (throw (ex-info "terminal receipt is missing required keys"
                        {:failure-kind :terminal-receipt-invalid
                         :kind (:kind receipt)
                         :missing (vec missing)}))))
    receipt))

(defn terminal-receipt
  "The one terminal receipt for RECORD, validated. A record whose
  :selection-event says :cascade-selected and which carries no failure and
  no abstention gets an :action-receipt. Every other record gets a :failure:
  an abstention, a failure after selection (the receipt names the target),
  or a record showing neither a selection nor a failure
  (:no-terminal-state, itself a defect to repair). The RECEIPT is exactly
  one kind; the record's state never makes this throw (review, claude-1
  2026-09-30: 35 of 63 past records were both or neither, and a throw here
  would have left their run records unwritten)."
  [record]
  (validate-receipt
   (if (and (selected-target record) (not (failed? record)))
     (action-receipt record)
     (failure-receipt record))))

(defn terminal-receipt-digest
  "The sha256 of the receipt's pr-str, hex, for the click binding's
  :terminal-receipt-digest — the binding cites the receipt by digest
  without duplicating it. Delegates to the JVM's MessageDigest; pure."
  ^String [receipt]
  (let [bytes (.digest (java.security.MessageDigest/getInstance "SHA-256")
                       (.getBytes (pr-str receipt) "UTF-8"))]
    (apply str (map #(format "%02x" %) bytes))))

(defn attach
  "RECORD with its terminal receipt and that receipt's digest attached. The
  close's OUTCOME — already computed, never re-derived here — is threaded
  in so the receipt's :outcome is the outcome the close recorded. For
  cond-> threading convenience the map is the LAST argument."
  [outcome record]
  (let [receipt (try
                  (terminal-receipt (assoc record :outcome outcome))
                  (catch clojure.lang.ExceptionInfo e
                    ;; Never lose the run record over its receipt: an
                    ;; ill-formed receipt becomes a typed failure receipt.
                    {:kind :failure
                     :id (str (:run/id record) "-terminal-receipt-invalid")
                     :failure-kind :terminal-receipt-invalid
                     :source "futon2.aif.wm.terminal-receipt/attach"
                     :at (:startedAt record)
                     :detail (ex-data e)}))]
    (assoc record
           :terminal-receipt receipt
           :terminal-receipt-digest (terminal-receipt-digest receipt))))
