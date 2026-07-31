(ns futon2.aif.memory-contract
  "Pure cross-domain contract for compact memory projections and use receipts.

   This namespace deliberately knows nothing about XTDB, HTTP, Zaif, or the WM
   scheduler. Futon3c and futon2 consume the same shapes so sharing a backend
   cannot silently become sharing untyped evidence."
  (:require [clojure.set :as cset]
            [clojure.string :as str])
  (:import [java.time Duration Instant]))

(def memory-states
  #{:current :challenged :retracted :superseded})

(def witness-statuses
  #{:self-asserted :independently-witnessed :challenged :unknown})

(def external-witness-statuses
  #{:independently-witnessed :challenged})

(defn- nonblank-string?
  [x]
  (and (string? x) (not (str/blank? x))))

(defn- explicit-domain?
  [x]
  (and (keyword? x) (some? (name x))))

(defn- string-vector?
  [x]
  (and (vector? x) (every? nonblank-string? x)))

(defn- fail!
  [message data]
  (throw (ex-info message data)))

(defn compact-memory
  "Validate and project one evidence-entry + memory/assert hyperedge.

   Required contextual fields are explicit because old P0 records did not put
   domain or witness state in the body:

   {:entry <evidence map> :edge <hyperedge map> :domain <keyword>
    :witness-status <keyword> :state <keyword>
    :valid-time <optional value> :system-time <optional value>}

   The compact result carries retrieval/audit metadata, not a value, rank,
   embedding score, or full memory body."
  [{:keys [entry edge domain witness-status state valid-time system-time]
    :or {state :current witness-status :unknown}
    :as input}]
  (let [memory-id (:evidence/id entry)
        roles (get-in edge [:hx/props :roles])
        role-entry (:entry roles)
        endpoints (set (:hx/endpoints edge))
        patterns (vec (distinct (or (:patterns roles) [])))
        missions (vec (distinct (concat (or (:missions roles) [])
                                        (when-let [mission (:mission roles)]
                                          [mission]))))
        subjects (vec (distinct (or (:subjects roles) [])))
        provenance (cond-> {:author (:evidence/author entry)
                            :session-id (:evidence/session-id entry)}
                     (seq (:provenance roles))
                     (assoc :edge (:provenance roles)))]
    (when-not (map? entry)
      (fail! "memory entry must be a map" {:input input}))
    (when-not (map? edge)
      (fail! "memory edge must be a map" {:input input}))
    (when-not (and (nonblank-string? memory-id)
                   (= :memory (:evidence/type entry))
                   (= :memory/assert (:hx/type edge))
                   (= memory-id role-entry)
                   (contains? endpoints memory-id))
      (fail! "entry and memory edge do not identify the same memory"
             {:memory-id memory-id :edge-id (:hx/id edge)}))
    (when-not (explicit-domain? domain)
      (fail! "memory domain must be an explicit keyword" {:domain domain}))
    (when-not (contains? witness-statuses witness-status)
      (fail! "invalid memory witness status" {:witness-status witness-status}))
    (when-not (contains? memory-states state)
      (fail! "invalid memory state" {:state state}))
    (when-not (and (nonblank-string? (:evidence/author entry))
                   (nonblank-string? (:evidence/session-id entry)))
      (fail! "memory provenance requires author and session"
             {:memory-id memory-id :provenance provenance}))
    (when-not (every? string-vector? [patterns missions subjects])
      (fail! "memory role endpoints must be vectors of nonblank strings"
             {:patterns patterns :missions missions :subjects subjects}))
    (when-not (every? endpoints (concat patterns missions subjects))
      (fail! "memory role endpoint is absent from hx/endpoints"
             {:edge-id (:hx/id edge)
              :roles {:patterns patterns :missions missions :subjects subjects}}))
    (cond-> {:memory/id memory-id
             :memory/hyperedge-id (:hx/id edge)
             :memory/domain domain
             :memory/act (:evidence/claim-type entry)
             :memory/kind (get-in edge [:hx/props :kind])
             :memory/hook (or (get-in edge [:hx/props :hook])
                              (get-in entry [:evidence/body :hook]))
             :memory/provenance provenance
             :memory/volatile? (boolean (get-in edge [:hx/props :volatile?]))
             :memory/state state
             :memory/witness-status witness-status
             :memory/pattern-ids patterns
             :memory/mission-ids missions
             :memory/subject-ids subjects}
      (some? valid-time) (assoc :memory/valid-time valid-time)
      (some? system-time) (assoc :memory/system-time system-time))))

(defn use-receipt
  "Validate and normalize a controller-stamped memory-use receipt.

   Used memories must be a subset of surfaced memories. Every surfaced memory
   has a nonblank inclusion reason. Rejected memories are a disjoint subset of
   surfaced memories and require reasons; any remaining surfaced memories are
   explicitly classified as unused. The receipt reports an outcome id only;
   the independently witnessed outcome remains a separate record."
  [{:keys [decision-id session-id domain surfaced-memory-ids used-memory-ids
           rejected-memory-ids inclusion-reasons rejection-reasons pattern-id
           cascade-id outcome-id surfaced-at recorded-at]
    :as receipt}]
  (when (= :war-machine domain)
    (fail! "War Machine projection selection is not agent memory attribution"
           {:domain domain
            :required-receipt :wm-projection
            :receipt receipt}))
  (let [surfaced (vec (distinct surfaced-memory-ids))
        used (vec (distinct used-memory-ids))
        rejected (vec (distinct (or rejected-memory-ids [])))
        surfaced-set (set surfaced)
        used-set (set used)
        rejected-set (set rejected)
        unused (filterv #(and (not (contains? used-set %))
                              (not (contains? rejected-set %)))
                        surfaced)]
    (when-not (and (nonblank-string? decision-id)
                   (nonblank-string? session-id)
                   (explicit-domain? domain)
                   (string-vector? surfaced-memory-ids)
                   (string-vector? used-memory-ids)
                   (string-vector? (or rejected-memory-ids []))
                   (map? inclusion-reasons)
                   (map? (or rejection-reasons {}))
                   (or (nonblank-string? pattern-id)
                       (nonblank-string? cascade-id))
                   (or (nil? outcome-id) (nonblank-string? outcome-id))
                   (or (nil? surfaced-at) (nonblank-string? surfaced-at))
                   (or (nil? recorded-at) (nonblank-string? recorded-at)))
      (fail! "invalid memory-use receipt shape" {:receipt receipt}))
    (when-not (every? surfaced-set used)
      (fail! "used memories must have been surfaced"
             {:surfaced surfaced :used used}))
    (when-not (every? surfaced-set rejected)
      (fail! "rejected memories must have been surfaced"
             {:surfaced surfaced :rejected rejected}))
    (when (seq (cset/intersection used-set rejected-set))
      (fail! "a memory cannot be both used and rejected"
             {:used used :rejected rejected}))
    (when-not (every? (fn [memory-id]
                        (nonblank-string? (get inclusion-reasons memory-id)))
                      surfaced)
      (fail! "every surfaced memory requires an inclusion reason"
             {:surfaced surfaced :inclusion-reasons inclusion-reasons}))
    (when-not (every? (fn [memory-id]
                        (nonblank-string?
                         (get (or rejection-reasons {}) memory-id)))
                      rejected)
      (fail! "every rejected memory requires a rejection reason"
             {:rejected rejected :rejection-reasons rejection-reasons}))
    (let [latency-ms
          (when (and surfaced-at recorded-at)
            (try
              (max 0 (.toMillis
                      (Duration/between (Instant/parse surfaced-at)
                                        (Instant/parse recorded-at))))
              (catch Throwable _
                (fail! "memory-use timestamps must be ISO-8601 instants"
                       {:surfaced-at surfaced-at
                        :recorded-at recorded-at}))))]
    (cond-> {:memory-use/signal :agent-attribution
             :memory-use/decision-id decision-id
             :memory-use/session-id session-id
             :memory-use/domain domain
             :memory-use/surfaced-ids surfaced
             :memory-use/used-ids used
             :memory-use/rejected-ids rejected
             :memory-use/unused-ids unused
             :memory-use/inclusion-reasons
             (mapv (fn [memory-id]
                     {:memory-id memory-id
                      :reason (get inclusion-reasons memory-id)})
                   surfaced)
             :memory-use/rejection-reasons
             (mapv (fn [memory-id]
                     {:memory-id memory-id
                      :reason (get (or rejection-reasons {}) memory-id)})
                   rejected)
             :memory-use/status (if outcome-id
                                  :outcome-attached
                                  :pending-outcome)}
      pattern-id (assoc :memory-use/pattern-id pattern-id)
      cascade-id (assoc :memory-use/cascade-id cascade-id)
      outcome-id (assoc :memory-use/outcome-id outcome-id)
      surfaced-at (assoc :memory-use/surfaced-at surfaced-at)
      recorded-at (assoc :memory-use/recorded-at recorded-at)
      (some? latency-ms) (assoc :memory-use/retrieval-to-use-ms latency-ms)))))

(def ^:private projection-forbidden-keys
  #{:used-ids :used-memory-ids :memory-use/used-ids})

(defn wm-projection-receipt
  "Validate one War Machine algorithmic-selection receipt.

   This is deliberately not a memory-use receipt. Projection-selected ids are
   what the algorithm selected, not an agent's attribution of memories used.
   Used-id fields are rejected rather than ignored."
  [{:keys [decision-id session-id domain surfaced-memory-ids
           projection-selected-memory-ids inclusion-reasons pattern-id
           cascade-id surfaced-at recorded-at]
    :as receipt}]
  (let [forbidden (cset/intersection projection-forbidden-keys
                                     (set (keys receipt)))
        surfaced (vec (distinct surfaced-memory-ids))
        selected (vec (distinct projection-selected-memory-ids))
        surfaced-set (set surfaced)]
    (when (seq forbidden)
      (fail! "WM projection receipts structurally forbid used-id fields"
             {:forbidden-keys forbidden :receipt receipt}))
    (when-not (and (nonblank-string? decision-id)
                   (nonblank-string? session-id)
                   (= :war-machine domain)
                   (string-vector? surfaced-memory-ids)
                   (string-vector? projection-selected-memory-ids)
                   (map? inclusion-reasons)
                   (or (nonblank-string? pattern-id)
                       (nonblank-string? cascade-id))
                   (or (nil? surfaced-at) (nonblank-string? surfaced-at))
                   (or (nil? recorded-at) (nonblank-string? recorded-at)))
      (fail! "invalid WM projection receipt shape" {:receipt receipt}))
    (when-not (every? surfaced-set selected)
      (fail! "projection-selected memories must have been surfaced"
             {:surfaced surfaced :projection-selected selected}))
    (when-not (every? (fn [memory-id]
                        (nonblank-string? (get inclusion-reasons memory-id)))
                      surfaced)
      (fail! "every surfaced WM memory requires an inclusion reason"
             {:surfaced surfaced :inclusion-reasons inclusion-reasons}))
    (cond-> {:wm-projection/signal :algorithmic-selection
             :wm-projection/decision-id decision-id
             :wm-projection/session-id session-id
             :wm-projection/domain :war-machine
             :wm-projection/surfaced-ids surfaced
             :wm-projection/projection-selected-ids selected
             :wm-projection/inclusion-reasons
             (mapv (fn [memory-id]
                     {:memory-id memory-id
                      :reason (get inclusion-reasons memory-id)})
                   surfaced)
             :wm-projection/status :pending-external-check}
      pattern-id (assoc :wm-projection/pattern-id pattern-id)
      cascade-id (assoc :wm-projection/cascade-id cascade-id)
      surfaced-at (assoc :wm-projection/surfaced-at surfaced-at)
      recorded-at (assoc :wm-projection/recorded-at recorded-at))))

(defn decision-keyed-external-check-entry
  "Construct an independently authored outcome check for any memory client.

   DOMAIN identifies the client lane and becomes an explicit evidence tag and
   body field. Decision identity is the only supported outcome join key."
  [{:keys [evidence-id decision-id domain author session-id at outcome
           witness-status checker]}]
  (when-not (and (every? nonblank-string?
                         [evidence-id decision-id author session-id at checker])
                 (explicit-domain? domain)
                 (keyword? outcome)
                 (contains? external-witness-statuses witness-status))
    (fail! "invalid decision-keyed external memory check"
           {:evidence-id evidence-id
            :decision-id decision-id
            :domain domain
            :author author
            :session-id session-id
            :at at
            :outcome outcome
            :witness-status witness-status
            :checker checker}))
  {:evidence/id evidence-id
   :evidence/subject {:ref/type :decision :ref/id decision-id}
   :evidence/type :pattern-outcome
   :evidence/claim-type :observation
   :evidence/author author
   :evidence/session-id session-id
   :evidence/at at
   :evidence/body {:outcome outcome
                   :memory-outcome/domain domain
                   :memory-outcome/witness-status witness-status
                   :checker checker}
   :evidence/tags [domain :external-check]})

(defn- validated-decision-check
  [check]
  (let [decision-id (get-in check [:evidence/subject :ref/id])
        domain (get-in check [:evidence/body :memory-outcome/domain])
        witness-status (get-in check
                               [:evidence/body
                                :memory-outcome/witness-status])]
    (when-not (and (= :decision (get-in check
                                         [:evidence/subject :ref/type]))
                   (nonblank-string? decision-id)
                   (= :pattern-outcome (:evidence/type check))
                   (= :observation (:evidence/claim-type check))
                   (nonblank-string? (:evidence/id check))
                   (nonblank-string? (:evidence/author check))
                   (explicit-domain? domain)
                   (some #{domain} (:evidence/tags check))
                   (some #{:external-check} (:evidence/tags check))
                   (keyword? (get-in check [:evidence/body :outcome]))
                   (contains? external-witness-statuses witness-status))
      (fail! "external memory checks must carry an exact decision and domain"
             {:check-id (:evidence/id check)
              :subject (:evidence/subject check)
              :domain domain}))
    check))

(defn- receipt-selection
  [receipt]
  (case (or (:memory-use/signal receipt)
            (:wm-projection/signal receipt))
    :agent-attribution
    {:signal :agent-attribution
     :decision-id (:memory-use/decision-id receipt)
     :domain (:memory-use/domain receipt)
     :offered-ids (:memory-use/surfaced-ids receipt)
     :selected-ids (:memory-use/used-ids receipt)}

    :algorithmic-selection
    {:signal :algorithmic-selection
     :decision-id (:wm-projection/decision-id receipt)
     :domain (:wm-projection/domain receipt)
     :offered-ids (:wm-projection/surfaced-ids receipt)
     :selected-ids (:wm-projection/projection-selected-ids receipt)}

    (fail! "expected a typed memory-client receipt" {:receipt receipt})))

(defn witnessed-memory-outcome-triple
  "Join offered -> selected -> independently witnessed for either receipt kind.

   Agent attribution uses its reported used ids; algorithmic selection uses
   projection-selected ids. They remain distinct signals in the result."
  [receipt external-check]
  (let [external-check (validated-decision-check external-check)
        {:keys [signal decision-id domain offered-ids selected-ids]}
        (receipt-selection receipt)
        check-decision (get-in external-check [:evidence/subject :ref/id])
        check-domain (get-in external-check
                             [:evidence/body :memory-outcome/domain])]
    (when-not (= decision-id check-decision)
      (fail! "memory receipt and external check decision ids differ"
             {:receipt-decision-id decision-id
              :check-decision-id check-decision}))
    (when-not (= domain check-domain)
      (fail! "memory receipt and external check domains differ"
             {:receipt-domain domain :check-domain check-domain}))
    (when-not (and (seq offered-ids) (seq selected-ids))
      (fail! "witnessed memory outcome requires offered and selected memories"
             {:offered offered-ids :selected selected-ids}))
    {:memory-outcome-triple/type :offered-selected-witnessed
     :memory-outcome-triple/selection-signal signal
     :memory-outcome-triple/decision-id decision-id
     :memory-outcome-triple/domain domain
     :memory-outcome-triple/offered-ids offered-ids
     :memory-outcome-triple/selected-ids selected-ids
     :memory-outcome-triple/witness-evidence-id (:evidence/id external-check)
     :memory-outcome-triple/witness-status
     (get-in external-check [:evidence/body
                             :memory-outcome/witness-status])
     :memory-outcome-triple/outcome
     (get-in external-check [:evidence/body :outcome])
     :memory-outcome-triple/checker (:evidence/author external-check)}))

(defn agent-attribution-corpus
  "Construct a homogeneous corpus of agent-attribution receipts.

   Algorithmic WM projection receipts fail closed instead of being pooled with
   solver or agent self-attribution."
  [receipts]
  (when-not (and (sequential? receipts)
                 (every? #(= :agent-attribution (:memory-use/signal %))
                         receipts))
    (fail! "agent-attribution corpus cannot contain algorithmic projection receipts"
           {:receipt-signals
            (mapv #(or (:memory-use/signal %)
                       (:wm-projection/signal %)
                       :untyped)
                  receipts)}))
  (vec receipts))
