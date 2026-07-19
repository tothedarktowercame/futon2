(ns futon2.aif.intrinsic-values
  "Per-action-class intrinsic-value table — R12 narrow-take-up apparatus.

   Invariant: hyperparameters consumed by the inner loop's policy layer
   (`:intrinsic-value` on `:learn-action-class` actions, per
   `futon2.aif.efe`/`futon2.aif.policy`) become hidden state inferred by a
   slower outer loop. This ns owns the in-process state.

   Pattern: atom-with-bootstrap-replay (per `feedback_reload_safety` —
   atoms-without-bootstrap-replay are the unsafe case). Persistence is via
   `code/v05/wm-hyperparameter-update` hyperedges in futon1a XTDB; on JVM
   startup the atom rehydrates by reading the latest record per class.

   Design choices: see
   `~/code/futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md`.
   Contract: WM R12 (`~/code/futon2/docs/futon-aif-completeness.md:279`).

   Public API:
     (current)               → snapshot of the atom
     (credit-for class)      → intrinsic-value for class (defaults to prior mode)
     (rehydrate! records)    → seed atom from a seq of update records
     (apply-update! record)  → fold one update record into atom and return it"
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [futon2.aif.substrate :as substrate]))

;; ---------------------------------------------------------------------------
;; Beta-posterior arithmetic
;; ---------------------------------------------------------------------------

(def ^:const prior-alpha 1.0)
(def ^:const prior-beta 1.0)

(defn- posterior-mode
  "Posterior mode of Beta(α, β).
   For α, β > 1: (α - 1) / (α + β - 2). For Beta(1,1) this falls back to the
   mean 0.5; for α = β = 1 the distribution is uniform and any point in (0,1)
   is admissible — 0.5 is the maximum-entropy choice."
  [alpha beta]
  (cond
    (and (> alpha 1.0) (> beta 1.0)) (/ (- alpha 1.0) (- (+ alpha beta) 2.0))
    :else                            (/ alpha (+ alpha beta))))

(defn fresh-entry
  "An entry holding the Beta(1,1) prior — what every class starts at before
   any update record exists for it."
  []
  {:alpha prior-alpha
   :beta  prior-beta
   :intrinsic-value (posterior-mode prior-alpha prior-beta)
   :n-emissions 0
   :n-followthrough 0
   :as-of nil})

;; ---------------------------------------------------------------------------
;; Atom — global state, hydrated on demand
;; ---------------------------------------------------------------------------

(defonce ^{:doc "Per-class intrinsic-value table; rehydrate via bootstrap-replay."}
  state
  (atom {}))

(defn current
  "Return a snapshot of the atom."
  []
  @state)

(defn credit-for
  "Intrinsic-value credit for CLASS, defaulting to the Beta(1,1) prior mode
   (0.5) for any class that has no recorded updates yet. This is the function
   the inner-loop action-proposer consults at recommendation time."
  [class]
  (or (get-in @state [class :intrinsic-value])
      (:intrinsic-value (fresh-entry))))

;; ---------------------------------------------------------------------------
;; Update-record shape (mirrors the hyperedge props)
;; ---------------------------------------------------------------------------

;; A record is a plain map with at minimum:
;;   :class    — action-class keyword (e.g. :address-sorry)
;;   :as-of    — ISO-8601 timestamp string
;;   :alpha-post  :beta-post  :intrinsic-value-post
;;   :n-emissions-in-window  :n-followthrough-in-window
;; Optional bookkeeping:
;;   :alpha-pre :beta-pre :intrinsic-value-pre :evidence-refs :outer-loop-run-id

(defn- entry-from-record
  [record]
  {:alpha (:alpha-post record)
   :beta  (:beta-post record)
   :intrinsic-value (:intrinsic-value-post record)
   :n-emissions (:n-emissions-in-window record)
   :n-followthrough (:n-followthrough-in-window record)
   :as-of (:as-of record)})

(defn rehydrate!
  "Replace atom state with the latest-per-class entry derived from RECORDS.
   Records are folded in :as-of order; latest wins per class. Returns the
   new atom value.

   Why latest-wins instead of replay-all: each outer-loop update IS already
   the full Beta posterior given all evidence up to its :as-of. Replaying
   every record would double-count — this is the standard property of
   conjugate updates."
  [records]
  (let [grouped (group-by :class records)
        latest  (into {}
                      (map (fn [[class rs]]
                             (let [r (last (sort-by :as-of rs))]
                               [class (entry-from-record r)])))
                      grouped)]
    (reset! state latest)))

(defn apply-update!
  "Fold a single RECORD into the atom. Used by the outer loop to update the
   in-process state right after persisting the hyperedge."
  [record]
  (swap! state assoc (:class record) (entry-from-record record))
  record)

(defn reset-to-prior!
  "Test-helper: clear the atom. Production callers should use rehydrate!
   instead."
  []
  (reset! state {}))

;; ---------------------------------------------------------------------------
;; Inference: derive the next update record from prior + new observations
;; ---------------------------------------------------------------------------

(defn next-record
  "Compute the next-update record for CLASS given prior Beta params and a
   batch of fresh observations.

   PRIOR-ENTRY is the current atom entry (or `fresh-entry`). N-EMISSIONS is
   the count of opportunities (WM ticks that surfaced CLASS in ranked-actions
   or chose CLASS as decision — see `wm-outer-loop/extract-emissions`).
   N-FOLLOWTHROUGH is the count of Stream-B-derived operator-follow-through
   events in the same window.

   Beta arithmetic: α += min(n-followthrough, n-emissions); β += (n-emissions
   - min(n-followthrough, n-emissions)). The cap on followthrough is
   defensive: follow-through events derived from substrate (e.g., git-derived
   sorry-closures) can outnumber WM emissions for the class because the
   operator acts independently of WM recommendations. Treating
   uncapped-followthrough as α-evidence would inject signal not actually
   attributable to the WM. The cap is logged via `:n-followthrough-observed`
   when it bites, so the operator sees the original count.

   Returns a record map ready to persist and apply."
  [class prior-entry n-emissions n-followthrough
   {:keys [as-of outer-loop-run-id window-days evidence-refs]}]
  (let [alpha-pre  (:alpha prior-entry)
        beta-pre   (:beta prior-entry)
        capped-ft  (max 0 (min n-followthrough n-emissions))
        delta-a    (double capped-ft)
        delta-b    (double (- n-emissions capped-ft))
        alpha-post (+ alpha-pre delta-a)
        beta-post  (+ beta-pre delta-b)
        base       {:class class
                    :as-of as-of
                    :outer-loop-run-id outer-loop-run-id
                    :window-days window-days
                    :alpha-pre alpha-pre
                    :beta-pre  beta-pre
                    :alpha-post alpha-post
                    :beta-post  beta-post
                    :intrinsic-value-pre  (posterior-mode alpha-pre  beta-pre)
                    :intrinsic-value-post (posterior-mode alpha-post beta-post)
                    :n-emissions-in-window n-emissions
                    :n-followthrough-in-window capped-ft
                    :evidence-refs (or evidence-refs [])}]
    (if (= capped-ft n-followthrough)
      base
      (assoc base :n-followthrough-observed n-followthrough))))

(defn next-update-record
  "Named public entry point for producing a `wm-hyperparameter-update` record.
  This is the same pure Beta update as `next-record`; the explicit name keeps
  artifact-producing callers separate from the persistence API."
  [class prior-entry n-emissions n-followthrough opts]
  (next-record class prior-entry n-emissions n-followthrough opts))

;; ---------------------------------------------------------------------------
;; XTDB bootstrap-replay
;; ---------------------------------------------------------------------------

(def ^:const hyperedge-type "code/v05/wm-hyperparameter-update")

(def ^:private default-store-base
  (str (substrate/configured-url) "/api/alpha"))

(defn- normalise-record
  "Coerce one XTDB hyperedge response into the record shape used here.
   Props arrive as a map with keyword keys; class field as a keyword or
   string. Returns nil for malformed records (logged via metadata in caller
   if needed)."
  [hx]
  (let [props (:hx/props hx)
        class (let [c (:class props)]
                (cond
                  (keyword? c) c
                  (and (string? c) (not= "" c)) (keyword (str/replace c #"^:" ""))
                  :else nil))]
    (when (and class (number? (:alpha-post props)) (number? (:beta-post props)))
      (assoc props :class class))))

(defn fetch-records
  "Fetch all wm-hyperparameter-update hyperedges from the authoritative store.
  Transport failure is loud; [] means the authoritative query was empty."
  ([] (fetch-records {:store-base default-store-base :limit 1000}))
  ([{:keys [store-base limit] :or {store-base default-store-base
                                   limit 1000}}]
   (let [base (str/replace store-base #"/api/alpha/?$" "")
         hxs (substrate/hyperedges-by-type
              hyperedge-type {:substrate-url base :limit limit})]
     (vec (keep normalise-record hxs)))))

(defn rehydrate-from-store!
  "Convenience: fetch records from XTDB and reset the atom from them.
   Idempotent; safe to call multiple times. Returns the new atom value."
  ([] (rehydrate-from-store! nil))
  ([opts]
   (rehydrate! (fetch-records (or opts {})))))

;; ---------------------------------------------------------------------------
;; XTDB write: persist a record as a hyperedge
;; ---------------------------------------------------------------------------

(def ^:const default-penholder
  ;; The only currently-registered penholder on this machine is "api"
  ;; (FUTON1A_ALLOWED_PENHOLDERS=api). True authorship is recorded in the
  ;; props' :provenance/author field so records stay attributable. A later
  ;; session can register a dedicated "wm-outer-loop" penholder if desired
  ;; — see design-choices §12.
  (or (System/getenv "WM_OUTER_LOOP_PENHOLDER") "api"))

(def ^:const provenance-author "wm-outer-loop")

(defn- record->hyperedge-payload
  "Build a POST /hyperedge body from a record."
  [record]
  (let [class-id (str "wm-class:" (name (:class record)))
        run-id   (or (:outer-loop-run-id record)
                     (str "wm-ol-run:" (:as-of record)))]
    {:penholder default-penholder
     :hx/type hyperedge-type
     :hx/endpoints [class-id run-id]
     :props (assoc record :provenance/author provenance-author)}))

(defn persist-record!
  "POST one update RECORD to futon1a as a wm-hyperparameter-update hyperedge.
   Returns the parsed response body on success, or a map {:error ...} on
   failure (logged but not thrown — the outer loop continues so one bad
   record doesn't stall the schedule)."
  ([record] (persist-record! record {:store-base default-store-base}))
  ([record {:keys [store-base] :or {store-base default-store-base}}]
   (try
     (let [url (str store-base "/hyperedge")
           body (json/generate-string (record->hyperedge-payload record))
           resp (http/post url {:headers {"Content-Type" "application/json"
                                          "Accept" "application/json"}
                                :body body
                                :timeout 5000
                                :throw false})]
       (if (#{200 201} (:status resp))
         (json/parse-string (:body resp) true)
         {:error :http-status :status (:status resp) :body (:body resp)}))
     (catch Exception e
       {:error :exception :message (.getMessage e)}))))
