(ns futon2.aif.wm.open-cascade-resume
  "Packet D4: the debugger / repair-loop / runner-service boundary for
  SAME-CLICK RESUME after a typed open-cascade execution error (D3's seam
  error class family, §7 of E-implementing-G-over-cascades).

  After a cascade has been selected, an implementation defect at the
  materialise/validate/compile/enact/evidence seam is a machine error, not
  a decision: the click must NOT relabel it as no-selection, abstention or
  ordinary refusal, must NOT rerun outer selection, must NOT select a
  runner-up cascade, and must NOT spend another click.  The debugger holds
  the selected cascade identity, its holes and the retained phase; an
  authoring requisition is dispatched; the returned authoring result is
  validated against shared handoff contracts; a verified repair is
  hot-loaded; and the held phase is retried under the SAME cascade digest.

  D3's exact production carrier is pending, so this namespace implements
  the REUSABLE PROTOCOL against a documented port.  THE ONE ADAPTER FIELD
  D3 MUST SUPPLY is `:resume-port` on the error:

      {:kind :wm/open-cascade-execution-error   ;; recognized class
       :cascade-digest <64-hex>                 ;; canonical identity
       :holes [{:hole-id ... :owner ... :question ...}]
       :phase <keyword>                         ;; retained phase
       :attempts [...]                          ;; prior failed attempts
       :resume-port {:retry (fn [ctx] ...)}     ;; D3's ONLY required
       }                                        ;; adapter field: a retry
                                                ;; entrypoint that takes the
                                                ;; resume context and
                                                ;; re-performs the held
                                                ;; phase against the same
                                                ;; digest.  Everything else
                                                ;; is protocol-managed.

  Terminal states are typed errors (:status :error with :kind), never
  abstention or refusal values.  Every attempt — failed answers, malformed
  answers, unknown senders, retries — is preserved in the ledger,
  append-only."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [futon2.aif.interpretation-evidence :as evidence]))

(def resume-schema :wm/open-cascade-resume-v1)

(def error-class :wm/open-cascade-execution-error)
(def incomplete-class :resume-incomplete)

(def required-operational-clause-fields
  [:operation :capability :completion-check])
(def held-phases #{:materialise :validate :compile :enact :evidence})

(def ^:private hex64? #(and (string? %) (re-matches #"[0-9a-f]{64}" %)))

;; ---------------------------------------------------------------------------
;; Ledger
;; ---------------------------------------------------------------------------

(defn new-ledger
  "A resume ledger for one held click: the click identity, the cascade
  digest, the retained phase, the holes, and an append-only list of every
  requisition, answer, validation result, hot-load and retry."
  [click-id]
  (atom {:schema resume-schema
         :click-id click-id
         :attempts []
         :requisitions []
         :answers []
         :retries []
         :events []}))

(defn- record! [ledger k v]
  (swap! ledger
         (fn [state]
           (let [prior (some-> state :events last :event-digest)
                 event {:sequence (count (:events state))
                        :previous prior :kind k :value v}
                 event (assoc event :event-digest
                              (evidence/value-digest event))]
             (-> state (update k conj v) (update :events conj event)))))
  v)

(defn validate-ledger
  "Verify the append chain over every recorded protocol event."
  [ledger]
  (loop [events (:events @ledger) n 0 prior nil]
    (if-let [event (first events)]
      (let [expected (evidence/value-digest (dissoc event :event-digest))]
        (if (and (= n (:sequence event))
                 (= prior (:previous event))
                 (= expected (:event-digest event)))
          (recur (next events) (inc n) (:event-digest event))
          {:status :error :kind :resume-ledger-chain-broken
           :sequence n}))
      {:status :verified :event-count n :head prior})))

;; ---------------------------------------------------------------------------
;; Recognition
;; ---------------------------------------------------------------------------

(defn recognize
  "Recognize D3's typed open-cascade execution error and build the resume
  context.  Returns the context, or a typed error value — never a refusal
  or abstention — when the error is not the recognized class or is
  malformed (unknown class, missing digest/holes/phase, phase outside the
  seam family, missing the :resume-port adapter field D3 must supply)."
  [error]
  (let [bad (fn [kind repair]
              {:status :error :kind kind :repair repair})]
    (cond
      (not (map? error))
      (bad :resume-error-malformed {:error-type (type error)})

      (not (contains? #{error-class incomplete-class} (:kind error)))
      (bad :resume-error-unknown-class {:kind (:kind error)})

      (not (hex64? (:cascade-digest error)))
      (bad :resume-cascade-digest-missing {:cascade-digest (:cascade-digest error)})

      (not (sequential? (:holes error)))
      (bad :resume-holes-missing {:holes (:holes error)})

      (not (contains? held-phases (:phase error)))
      (bad :resume-phase-not-held {:phase (:phase error)
                                   :held (vec held-phases)})

      (not (and (map? (:resume-port error))
                (ifn? (:retry (:resume-port error)))))
      (bad :resume-port-missing
           {:hint "D3 must supply :resume-port {:retry fn} on the error"})

      :else
      {:status :held
       :cascade-digest (:cascade-digest error)
       ;; The selected cascade is immutable across a retry. The executable
       ;; refinement is expected to get a new digest when reviewed hole
       ;; authoring is applied, so keep the two identities distinct.
       :selected-cascade-digest (:selected-cascade-digest error)
       :holes (vec (:holes error))
       :phase (:phase error)
       :prior-attempts (vec (:attempts error))
       :prior-resume-cause (when (= incomplete-class (:kind error))
                             (select-keys error [:kind :holes :missing-fields]))
       :retry (get-in error [:resume-port :retry])})))

;; ---------------------------------------------------------------------------
;; The authoring requisition
;; ---------------------------------------------------------------------------

(defn requisition-id
  "A requisition's identity: bound to the cascade digest, phase and the
  holes it asks about."
  [ctx]
  (evidence/value-digest
   {:cascade-digest (:cascade-digest ctx)
    :phase (:phase ctx)
    :holes (:holes ctx)}))

(defn authoring-requisition
  "The explicit authoring requisition dispatched for a held phase: bound to
  the cascade digest, every hole (id, owner, question), the retained
  phase, and the author/reviewer identity pair the shared handoff
  contracts require (author and reviewer must be distinct agents)."
  [ctx author reviewer]
  (let [rid (requisition-id ctx)]
    (cond
      (not (and (string? author) (seq author)))
      {:status :error :kind :requisition-author-absent}
      (not (and (string? reviewer) (seq reviewer)))
      {:status :error :kind :requisition-reviewer-absent}
      (= author reviewer)
      {:status :error :kind :requisition-author-equals-reviewer
       :author author :reviewer reviewer}
      :else
      {:status :dispatched
       :schema :wm/open-cascade-authoring-requisition-v1
       :requisition-id rid
       :cascade-digest (:cascade-digest ctx)
       :phase (:phase ctx)
       :holes (:holes ctx)
       :operational-clause-schema
       {:required required-operational-clause-fields
        :optional [:consumes :produces]}
       :prior-resume-cause (:prior-resume-cause ctx)
       :author author
       :reviewer reviewer})))

(defn dispatch-requisition!
  "Record a requisition on the ledger (append-only)."
  [ledger requisition]
  (when (not= :dispatched (:status requisition))
    (throw (ex-info "not a dispatchable requisition"
                    {:kind :requisition-not-dispatchable
                     :status (:status requisition)})))
  (record! ledger :requisitions requisition))

;; ---------------------------------------------------------------------------
;; Answer validation (shared handoff contracts)
;; ---------------------------------------------------------------------------

(defn- decoded-hole-id [k]
  (cond
    (vector? k) k
    ;; JSON object keys representing vector ids arrive as keywords such as
    ;; :["p/x" 0]. Ordinary keyword hole ids must retain their namespace.
    (keyword? k) (let [s (subs (str k) 1)]
                   (if (.startsWith s "[")
                     (decoded-hole-id s)
                     k))
    (string? k) (try
                  (let [v (edn/read-string k)]
                    (if (vector? v) v k))
                  (catch Throwable _ k))
    :else k))

(defn- normalized-decisions [answer]
  (when (map? (:decisions answer))
    (into {} (map (fn [[k v]] [(decoded-hole-id k) v]))
          (:decisions answer))))

(defn validate-answer
  "Validate a returned authoring answer against the shared handoff
  contracts before loading: it names THIS requisition id and cascade digest,
  carries a claim attributed to the
  requisition's author and accepted by its reviewer (author and reviewer
  distinct), and provides a loadable patch.  Unknown or malformed answers
  are typed errors with everything preserved. Hole coverage is checked on
  the verified function output, never on the JSON claim."
  [requisition answer]
  (let [bad (fn [kind repair]
              {:status :error :kind kind :repair repair})
        decisions (normalized-decisions answer)]
    (cond
      (not (map? answer))
      (bad :answer-malformed {:answer-type (type answer)})

      (not= (:requisition-id requisition) (:requisition-id answer))
      (bad :answer-requisition-mismatch
           {:expected (:requisition-id requisition)
            :got (:requisition-id answer)})

      (not= (:cascade-digest requisition) (:cascade-digest answer))
      (bad :answer-cascade-digest-mismatch
           {:expected (:cascade-digest requisition)
            :got (:cascade-digest answer)})

      (not= (:author requisition) (:by answer))
      (bad :answer-author-mismatch {:expected (:author requisition)
                                    :by (:by answer)})

      (not= (:reviewer requisition) (:accepted-by answer))
      (bad :answer-reviewer-mismatch {:expected (:reviewer requisition)
                                      :accepted-by (:accepted-by answer)})

      (= (:by answer) (:accepted-by answer))
      (bad :answer-author-equals-reviewer {:who (:by answer)})

      (not (and (map? (:patch answer))
                (string? (get-in answer [:patch :namespace]))
                (string? (get-in answer [:patch :var]))
                (hex64? (get-in answer [:patch :source-digest]))))
      (bad :answer-patch-missing
           {:hint "a sealed namespace/var/source-digest descriptor is required"})

      :else
      {:status :accepted
       :requisition-id (:requisition-id answer)
       :cascade-digest (:cascade-digest answer)
       :decisions decisions
       :patch (:patch answer)
       :by (:by answer)
       :accepted-by (:accepted-by answer)})))

(defn- json-claim-matches? [claim decisions]
  ;; Compare in the transport's value domain: JSON spells keyword values as
  ;; strings. Map order is immaterial; occurrence keys were already decoded.
  (try
    (= (json/parse-string (json/generate-string claim))
       (json/parse-string (json/generate-string decisions)))
    (catch Exception _ false)))

(defn validate-product
  "Check answers from the reviewed function. JSON decisions are only a claim;
  a malformed or differing claim remains a typed defect even if code answers."
  [requisition accepted product]
  (let [decisions (normalized-decisions {:decisions product})
        required (set (map (comp decoded-hole-id :hole-id) (:holes requisition)))
        answered (set (keys decisions))
        missing (remove answered required)
        foreign (remove required answered)
        incomplete (into {} (keep (fn [id]
                                    (let [value (get decisions id)
                                          fields (filterv #(nil? (get value %))
                                                          [:operation :capability
                                                           :completion-check])]
                                      (when (seq fields) [id fields]))) required))
        claim (:decisions accepted)
        claim-matches? (json-claim-matches? claim decisions)
        claim-operational? (and (= required (set (keys claim)))
                                (every? (fn [v]
                                          (and (map? v)
                                               (every? #(some? (get v %))
                                                       [:operation :capability
                                                        :completion-check])))
                                        (vals claim)))
        delivery {:source :reviewed-patch-function
                  :patch (:patch accepted)
                  :requisition-id (:requisition-id requisition)
                  :answered (vec (sort-by pr-str answered))
                  :claim-matches? claim-matches?
                  :author-response-defects
                  (cond-> []
                    (or (not claim-operational?) (not claim-matches?))
                    (conj {:kind (if claim-operational?
                                   :author-response/decisions-mismatch
                                   :author-response/decisions-not-operational)
                           :claimed-decisions claim}))}]
    (if (and (map? product) (empty? missing) (empty? foreign) (empty? incomplete))
      {:status :accepted :decisions decisions :answer-delivery delivery}
      {:status :error :kind :answer-holes-unresolved
       :answer-delivery delivery
       :repair {:missing-ids (vec (sort-by pr-str missing))
                :foreign-ids (vec (sort-by pr-str foreign))
                :missing-fields incomplete
                :requisitioned (vec (sort-by pr-str required))
                :answered (vec (sort-by pr-str answered))}})))

(defn submit-answer!
  "Submit an answer for a dispatched requisition.  Validated answers are
  recorded as accepted; invalid ones are recorded WHOLE (the raw answer is
  preserved) and returned as typed errors — the requisition stays open for
  a corrected answer, and no attempt is ever discarded."
  [ledger requisition answer]
  (let [result (validate-answer requisition answer)]
    (record! ledger :answers
             (cond-> result
               (map? answer) (assoc :raw answer)))
    result))

;; ---------------------------------------------------------------------------
;; Hot-load and same-click resume
;; ---------------------------------------------------------------------------

(def ^:private hot-load-cause-keys
  "The bounded projection of a hot-load failure's typed cause: the kind the
  thrower named and the scalar fields that locate it.  Never the exception."
  [:kind :namespace :var :value-type :expected :loaded])

(defn- bounded-cause
  "The typed cause of a thrown hot-load failure, when the throwable carries
  one (ex-data with a keyword :kind): scalar fields only.  Absent otherwise;
  nothing is invented and no exception object is retained."
  [t]
  (let [data (ex-data t)]
    (when (keyword? (:kind data))
      (into {}
            (filter (fn [[_ v]]
                      (or (keyword? v) (string? v) (symbol? v)
                          (number? v) (boolean? v))))
            (select-keys data hot-load-cause-keys)))))

(def ^:private retained-stack-limit 24)

(defn- throwable-diagnostic
  "Serializable diagnostic evidence for a failed hot-load.  Stack frames are
  bounded so the durable run record cannot grow with an arbitrary throwable."
  [^Throwable t]
  {:throwable-class (.getName (class t))
   :message (ex-message t)
   :stack (mapv str (take retained-stack-limit (.getStackTrace t)))})

(defn hot-load!
  "Hot-load an accepted answer's patch: the patch is applied under the
  requisition's binding (same cascade digest, same phase).  The loader must
  yield a FUNCTION of the context map {:cascade-digest :phase}.  A data
  value that merely satisfies `ifn?` (a map, a set or a keyword answers a
  one-argument lookup) is refused as :patch-var-not-a-function before any
  retry: on 2026-10-23 (injury 41a00c62) the author named the bare decision
  map, the lookup returned nil, and the retry's typed refusal was read by
  `resume!` as a protocol violation.  A loader that yields nothing is the
  distinct :patch-descriptor-unresolved.  A patch that throws, or a loader
  that refuses, is a typed :hot-load-failed with the attempt preserved and
  the thrower's bounded cause retained; it never surfaces as abstention or
  refusal."
  [ledger requisition accepted patch-loader]
  (let [descriptor (:patch accepted)
        attempt {:requisition-id (:requisition-id accepted)
                 :cascade-digest (:cascade-digest accepted)
                 :phase (:phase requisition)
                 :patch descriptor
                 :at (str (java.time.Instant/now))}]
    (try
      (when-not (ifn? patch-loader)
        (throw (ex-info "trusted patch loader absent" {})))
      (let [patch (patch-loader descriptor)
            _ (when (nil? patch)
                (throw (ex-info "patch descriptor did not resolve"
                                {:kind :patch-descriptor-unresolved
                                 :namespace (:namespace descriptor)
                                 :var (:var descriptor)})))
            _ (when-not (fn? patch)
                (throw (ex-info "reviewed patch var is not a function of the context"
                                {:kind :patch-var-not-a-function
                                 :namespace (:namespace descriptor)
                                 :var (:var descriptor)
                                 :value-type (.getName (class patch))})))
            product (patch (assoc requisition
                                  :cascade-digest (:cascade-digest accepted)
                                  :phase (:phase requisition)))]
        (record! ledger :attempts (assoc attempt :outcome :loaded :product product)))
      (catch Throwable t
        (let [cause (bounded-cause t)
              diagnostic (throwable-diagnostic t)]
          (record! ledger :attempts
                   (cond-> (assoc attempt :outcome :failed :error (ex-message t)
                                  :throwable diagnostic)
                     cause (assoc :cause cause)))
          (cond-> {:status :error :kind :hot-load-failed
                   :error (ex-message t)
                   :throwable diagnostic}
            cause (assoc :cause cause)))))))

(defn resume!
  "Retry the held phase WITHOUT a new click, WITHOUT reselection, under
  the SAME cascade digest.  CLICK-BUDGET and SELECTION-COUNTER are the
  fixture-visible controls the runner boundary supplies: the protocol
  asserts their values are unchanged across the retry (the retry entrypoint
  receives the resume context, never a fresh selection).  Returns
  {:status :resumed :result {:status :cascade-ready …}} only when the retry
  produced a ready cascade. A still-open product returns :resume-incomplete
  with its per-hole missing fields. Terminal failures are :error, never
  abstention/refusal."
  ([ledger ctx] (resume! ledger ctx {}))
  ([ledger ctx {:keys [click-budget selection-counter]}]
   (let [before {:clicks (some-> click-budget deref)
                 :selections (some-> selection-counter deref)}
         attempt {:cascade-digest (:cascade-digest ctx)
                  :phase (:phase ctx)
                  :at (str (java.time.Instant/now))}]
     (try
       (let [result ((:retry ctx) ctx)
             selected (:selected-cascade-digest ctx)
             returned-selected (or (:selected-cascade-digest result)
                                   ;; Current :cascade-ready records expose
                                   ;; the selected identity in this field.
                                   (:cascade-digest result))
             checks {:result-map? (map? result)
                     :selected-identity-preserved?
                     (if selected
                       (= selected returned-selected)
                       (= (:cascade-digest ctx) (:cascade-digest result)))
                     :compiled-digest-present?
                     (or (nil? selected)
                         (boolean (hex64? (:compiled-digest result))))
                     :click-count-unchanged?
                     (or (nil? click-budget)
                         (= (:clicks before) @click-budget))
                     :selection-count-unchanged?
                     (or (nil? selection-counter)
                         (= (:selections before) @selection-counter))}
             controls {:click-budget-present? (some? click-budget)
                       :selection-counter-present? (some? selection-counter)}]
         (record! ledger :retries (assoc attempt :outcome :retried))
         ;; the runner boundary must prove the click and selection counts
         ;; are untouched by the retry
         (if (every? true? (vals checks))
           (if (= :cascade-ready (:status result))
             {:status :resumed
              :cascade-digest (:cascade-digest ctx)
              :phase (:phase ctx)
              :result result
              :clicks-spent 0
              :selections-run 0}
             (let [product (:authoring-product ctx)
                   hole-ids (mapv #(if (map? %) (:hole-id %) %)
                                  (or (:holes result) (:holes ctx)))
                   missing-fields
                   (mapv (fn [hole-id]
                           {:hole-id hole-id
                            :missing
                            (vec (remove #(some? (get (get product hole-id) %))
                                         required-operational-clause-fields))})
                         hole-ids)]
               {:status :error
                :kind incomplete-class
                :open-cascade true
                :repairable true
                :cascade-digest (:cascade-digest ctx)
                :selected-cascade-digest (:selected-cascade-digest ctx)
                :phase (:phase ctx)
                :holes (:holes ctx)
                :missing-fields missing-fields
                :compiler-result (dissoc result :resume-port)
                :attempts (conj (:prior-attempts ctx)
                                {:kind incomplete-class
                                 :missing-fields missing-fields})
                :resume-port {:retry (:retry ctx)}}))
           (do (record! ledger :retries
                        (assoc attempt :outcome :protocol-violation
                               :returned-digest (:cascade-digest result)
                               :checks checks :controls controls
                               :clicks [(:clicks before)
                                        (some-> click-budget deref)]
                               :selections [(:selections before)
                                            (some-> selection-counter deref)]))
               {:status :error
                :kind :resume-protocol-violation
                :checks checks :controls controls
                :repair {:clicks [(:clicks before)
                                  (some-> click-budget deref)]
                         :selections [(:selections before)
                                      (some-> selection-counter deref)]}})))
       (catch Throwable t
         (record! ledger :retries
                  (assoc attempt :outcome :failed :error (ex-message t)))
         {:status :error :kind :resume-retry-failed
          :error (ex-message t)})))))

;; ---------------------------------------------------------------------------
;; The one-click protocol, end to end (fixture/port driven)
;; ---------------------------------------------------------------------------

(defn handle-error!
  "The whole protocol for one held click: recognize the error, dispatch an
  authoring requisition (author/reviewer distinct), accept a validated
  answer, hot-load it, and resume the held phase in the SAME click.  Any
  typed error at any stage is returned as :status :error with the ledger
  preserving everything; the caller (debugger/runner boundary) retries or
  surfaces it — nothing here ever abstains or refuses."
  [ledger error author reviewer answer & [{:keys [click-budget
                                                 selection-counter
                                                 patch-loader]}]]
  (let [recognized (recognize error)]
    (if (not= :held (:status recognized))
      recognized
      (let [requisition (authoring-requisition recognized author reviewer)]
        (if (not= :dispatched (:status requisition))
          requisition
          (do (dispatch-requisition! ledger requisition)
              (let [accepted (submit-answer! ledger requisition answer)]
                (if (not= :accepted (:status accepted))
                  accepted
                  (let [loaded (hot-load! ledger requisition accepted
                                          patch-loader)]
                    (if (= :error (:status loaded))
                      loaded
                      (let [delivery (validate-product requisition accepted (:product loaded))]
                        (if (= :error (:status delivery))
                          delivery
                          (let [resumed (resume! ledger
                                                (assoc recognized
                                                       :authoring (assoc accepted :decisions (:decisions delivery))
                                                       :authoring-product (:decisions delivery))
                                                {:click-budget click-budget
                                                 :selection-counter selection-counter})]
                            (cond-> (assoc resumed :answer-delivery (:answer-delivery delivery))
                              (map? (:result resumed))
                              (assoc-in [:result :answer-delivery]
                                        (:answer-delivery delivery))))))))))))))))
