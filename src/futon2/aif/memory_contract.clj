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
    (cond-> {:memory-use/decision-id decision-id
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
