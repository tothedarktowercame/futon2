(ns futon2.aif.c-vector
  "E-C-vector-live: the War Machine's preference component **C**, kept LIVE.

   The static channel-range C (`futon2.aif.preferences/preferences`) is the
   *floor*; this ns adds the **goal-outcome** half of C — Friston's preferences
   over goal-satisfaction outcomes — DERIVED from the live goal/hole corpus in
   substrate-2 (:7071) rather than hand-set, kept fresh by a corpus-signature
   freshness guard, and contributed to EFE's risk as an additive term.

   Division of labour (E-C-vector-live §HEAD / §5):
   - The C-vector *model* (R19, the C-entry shape, the 5 channel flavours, the
     A/B/C/D/E faithfulness diff) is owned by M-goals-and-holes. This ns is the
     **live + fresh + wired mechanism only** — it does not own the contract.
   - The **stated** channel (caps + clean open sorries) is :7071-native and is
     derived LIVE here. Its shape is kept in sync with
     `futon6/scripts/c_vector.bb` `produce-stated` (the validated snapshot
     producer); the duplication is deliberate so futon2 needs no futon6 files
     to keep its belly beating. KNOWN FOLLOW-UP: unify the two producers.
   - The **mess / incompleteness / 應-voice** channels are produced by
     `c_vector.bb`; fold them in via `merge-entries` reading its overlays
     (the extension point — not duplicated here).

   Risk is **static** (distance of the CURRENT corpus from C). The **predictive**
   risk (a policy π's predicted outcomes vs C — the canonical KL term) is the
   W1-gated follow-on: it needs the forward model to predict goal-progress under
   π, i.e. the goals↔methods PROOF join (M-populate-substrate-2 D4). The seam is
   `goal-outcome-risk` — see its :TODO. Do not remove it (Joe, 2026-06-26).

   Disciplines: read-only against :7071 (zero writes); degrades to [] (⇒ the
   static floor) on an unreachable store — never an empty belly that silently
   freezes (the D7a derived-stale lesson; cf. `futon3c.watcher.freshness`)."
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [babashka.http-client :as http]))

(def ^:private default-store-base
  (or (System/getenv "FUTON1A_BASE_URL")
      "http://localhost:7071/api/alpha"))

;; 33 open sorries share this templated :if — the boilerplate the audit found
;; (143 open → 110 clean). Kept identical to c_vector.bb/BOILERPLATE-IF.
(def ^:const boilerplate-if "Work requires a structured plan")

;; ---------------------------------------------------------------------------
;; C-entry — the shared record (mirrors c_vector.bb / M-goals-and-holes DERIVE §3)
;; ---------------------------------------------------------------------------

(defn c-entry
  "One preferred outcome. `:status` is :open until an earned closure (I3);
   `:weight` carries its `:basis` so orientation is auditable (I2). I1: never a
   preference without provenance."
  [{:keys [flavour outcome-ref preferred weight status provenance]
    :or   {status :open weight {:value 0.3 :basis :default-unoriented}}}]
  {:pre [flavour outcome-ref preferred provenance]}
  {:flavour flavour :outcome-ref outcome-ref :preferred preferred
   :weight weight :status status :provenance provenance})

;; ---------------------------------------------------------------------------
;; Static risk — divergence + risk-of (kept identical to c_vector.bb)
;; ---------------------------------------------------------------------------

(defn- divergence
  "One-sided distance of the current outcome from the preferred floor.
   `:becomes` = a binary goal (cap attested / sorry closed): full unit risk
   while unmet, 0 when reached."
  [current {:keys [op value]}]
  (case op
    :>=      (max 0.0 (- (double value) (double current)))
    :<=      (max 0.0 (- (double current) (double value)))
    :becomes (if (= current value) 0.0 1.0)
    (Math/abs (double (- (double current) (double value))))))

(defn- current-outcome
  "The current observable outcome a C-entry is scored against. Derived entries
   are emitted only while UNMET, so the default is a not-yet sentinel; range
   channels (e.g. mess L) carry their current reading in `:provenance :current`."
  [e]
  (get-in e [:provenance :current] ::unmet))

(defn risk-of
  "Static R5 contribution of one open C-entry: weight·divergence(current, preferred)."
  [{:keys [preferred weight status] :as e} current]
  (if (= status :open)
    (* (double (:value weight))
       (divergence (if (some? current) current (current-outcome e)) preferred))
    0.0))

;; ---------------------------------------------------------------------------
;; DERIVE — the stated channel, LIVE from substrate-2 (:7071)
;; ---------------------------------------------------------------------------

(defn- fetch-entities
  "Read entities of one type from substrate-2 (EDN). Read-only. Returns [] on
   any failure (exit-5 safe degrade — a fresh/unreachable store starts empty,
   which reduces the belly to the static floor, never crashes the loop)."
  [type]
  (try
    (let [url  (str default-store-base "/entities/latest?type=" type "&limit=2000")
          resp (http/get url {:headers {"Accept" "application/edn"}
                              :timeout 8000 :throw false})]
      (if (= 200 (:status resp))
        (or (:entities (edn/read-string (:body resp))) [])
        []))
    (catch Exception _ [])))

;; the 2 literal meta placeholders are schema artifacts, never real goals
(def ^:private cap-meta?
  #{"scope/capability/capabilities" "scope/capability/capability"})

(defn entries-from-corpus
  "Pure: derive the stated-channel C-entries from already-fetched corpus maps.
   Split out so it is testable without the store (exit-1/3). `caps`/`sorries`
   are the `:entities` vectors from :7071. A cap not yet attested → a preferred
   `:attested` outcome; a clean open sorry → a preferred `:closed` outcome."
  [caps sorries]
  (let [unmet   (->> caps
                     (remove #(get-in % [:props :capability/attested?]))
                     (remove #(cap-meta? (:id %))))
        cap-es  (for [c unmet
                      :let [p (:props c) st (:capability/status p)
                            ref (or (:capability/id p) (:name c))]]
                  (c-entry {:flavour :stated
                            :outcome-ref {:kind :capability :id ref :metric :attested}
                            :preferred {:op :becomes :value :attested}
                            :weight {:value (if (= :held st) 0.6 0.4) :basis :star-map-status}
                            :provenance {:source ":7071/capability" :id (:capability/id p)
                                         :substrate-id (:id c) :status st}}))
        open    (filter #(= "open" (get-in % [:props :sorry/status])) sorries)
        clean   (remove #(str/starts-with? (str (get-in % [:props :sorry/if]))
                                           boilerplate-if)
                        open)
        sorry-es (for [s clean :let [p (:props s)]]
                   (c-entry {:flavour :stated
                             :outcome-ref {:kind :sorry :id (:sorry/id p) :metric :closed}
                             :preferred {:op :becomes :value :closed}
                             :provenance {:source ":7071/sorry" :id (:sorry/id p)
                                          :substrate-id (:id s) :title (:sorry/title p)}}))]
    (vec (concat cap-es sorry-es))))

(defn- corpus-signature-of
  "Hash over (entity-id, satisfaction-state) so a corpus change — a sorry
   opening/closing, a cap attested — flips the signature even under a
   constant count (a close+open swap). The derived-stale-vs-source key."
  [caps sorries]
  (hash (sort-by first
                 (concat
                  (for [c caps] [(str (:id c)) (boolean (get-in c [:props :capability/attested?]))])
                  (for [s sorries] [(str (:id s)) (get-in s [:props :sorry/status])])))))

(defn derive-stated
  "Fetch the corpus LIVE and return {:entries :signature}. One fetch feeds both
   the entries and the freshness signature."
  []
  (let [caps    (fetch-entities "capability")
        sorries (fetch-entities "sorry")]
    {:entries   (entries-from-corpus caps sorries)
     :signature (corpus-signature-of caps sorries)
     :n-source  {:caps (count caps) :sorries (count sorries)}}))

;; ---------------------------------------------------------------------------
;; The atom-backed live state + freshness guard (Joe: "an atom is good")
;; ---------------------------------------------------------------------------

(defonce ^{:doc "The maintained live C-vector. Read-only on the WM tick via
  `current-c-vector`; mutated only by `refresh!` off-cycle."}
  c-state
  (atom {:entries [] :signature nil :derived-at nil :n-source nil}))

(defn current-c-vector
  "The maintained live goal-outcome C-vector. Reads the atom only — cheap,
   WM-tick-safe. [] when never derived / store unreachable ⇒ goal-outcome-risk
   0 ⇒ EFE reduces to the static-channel floor (exit-5, augment-don't-rip-out)."
  []
  (:entries @c-state))

(defn refresh!
  "Re-derive the live C-vector from the current corpus and swap the atom.
   OFF-CYCLE only (watcher / periodic probe) — never the per-action tick
   (§5 derive-and-cache, not recompute-per-tick). On an empty derive (store
   down) the atom is left UNCHANGED — the last-good C (or the static floor)
   stays in force; the belly is never silently clobbered to empty."
  []
  (let [{:keys [entries signature n-source]} (derive-stated)]
    (if (seq entries)
      (reset! c-state {:entries entries :signature signature
                       :derived-at (java.time.Instant/now) :n-source n-source})
      @c-state)))

(defn live-signature
  "Cheap-ish probe of the live corpus signature (one fetch). The freshness
   comparator's source of truth."
  []
  (:signature (derive-stated)))

(defn stale?
  "True iff the corpus changed since C was last derived (or C was never
   derived). The mandatory freshness guard (§HEAD; D7a derived-stale-vs-source).
   The 1-arity injects the live signature (hermetic; for tests + reusing a
   probe already in hand); the 0-arity fetches it."
  ([] (stale? (live-signature)))
  ([live]
   (let [stored (:signature @c-state)]
     (or (nil? stored) (not= stored live)))))

(defn freshness-check
  "LOUD freshness report (cf. futon3c.watcher.freshness). Returns
   {:fresh? :stored-signature :live-signature :derived-at}. When stale it also
   warns on *err* so a silently-frozen C cannot hide (the 5-week-freeze lesson).
   The 1-arity injects the live signature (hermetic)."
  ([] (freshness-check (live-signature)))
  ([live]
   (let [stored (:signature @c-state)
         fresh? (boolean (and stored (= stored live)))]
     (when-not fresh?
       (binding [*out* *err*]
         (println (format "[c-vector] STALE C — derived-sig=%s live-sig=%s derived-at=%s — run (refresh!) before trusting risk"
                          stored live (:derived-at @c-state)))))
     {:fresh? fresh? :stored-signature stored :live-signature live
      :derived-at (:derived-at @c-state)})))

(defn maybe-refresh!
  "Off-cycle live-update trigger: re-derive ONLY when the corpus changed.
   Cheap when fresh (one signature fetch, no swap). Call from the WM outer loop
   / a periodic probe — NOT the per-action tick. This is exit-2's update
   mechanism + the §5 derive-and-cache contract in one."
  []
  (if (stale?) (refresh!) @c-state))

;; ---------------------------------------------------------------------------
;; The additive goal-outcome risk term (the wiring into EFE's R5 risk)
;; ---------------------------------------------------------------------------

(def default-goal-outcome-weight
  "W4 normalising weight. goal-outcome-risk is MEAN-normalised over open entries
   so N goal-outcomes stay commensurable with the 14 channel gaps (≈ mean
   entry-weight, in [0,1]); this scalar then scales that mean into G-total."
  1.0)

(defn goal-outcome-risk
  "The additive goal-outcome contribution to EFE risk: the weight·MEAN of
   risk-of over the live C-vector's OPEN entries. STATIC risk — the current
   outcome is the entry's still-unmet state, so this measures distance of the
   CURRENT corpus from C (action-independent: a constant offset across policies).
   When a goal/hole is satisfied it drops from the derived set and the term falls
   → the belly tracks present goals. The action-dependent form that re-ranks
   policies is `predictive-goal-outcome-risk`.

   [] (never derived / store down) ⇒ 0.0 ⇒ EFE reduces to the static floor."
  ([entries] (goal-outcome-risk entries default-goal-outcome-weight))
  ([entries weight]
   (let [open (filter #(= :open (:status %)) entries)
         n    (count open)]
     (if (zero? n)
       0.0
       (* (double weight)
          (/ (reduce + 0.0 (map #(risk-of % nil) open)) (double n)))))))

;; ---------------------------------------------------------------------------
;; PREDICTIVE goal-outcome risk — the canonical EFE term (the §HEAD W1 seam,
;; now filled in its in-memory form; Joe: "push ahead with predictive-risk").
;; ---------------------------------------------------------------------------

(defn- norm-id
  "Match outcome-ref ids across action targets (often keywords) and live
   C-entries (often strings) on a common normal form."
  [x]
  (when (some? x) (if (keyword? x) (name x) (str x))))

(defn advanced-outcome-ids
  "The set of C-entry outcome-ref ids a candidate action is predicted to advance
   toward satisfaction — the IN-MEMORY core of the discharged-by join (the
   durable PROOF-store version is M-populate-substrate-2 D4, out of scope). It
   reuses the existing action / star-map structure rather than a parallel map:
   an explicit `:advances-outcomes` on the action (the override seam) ∪ the
   action's `:target` ∪ (for `:open-mission`) the mission's `:produces` caps
   from the capability graph. Normalised so keyword targets match string ids."
  [action capability-graph]
  (let [target   (:target action)
        produced (when (= :open-mission (:type action))
                   (get-in capability-graph [:missions target :produces]))]
    (into #{} (keep norm-id (concat (:advances-outcomes action) [target] produced)))))

(defn predictive-goal-outcome-risk
  "The CANONICAL EFE goal-outcome risk: weight·MEAN divergence of the PREDICTED
   outcomes under the policy from C. Entries the action advances are predicted
   satisfied ⇒ contribute 0; the rest stay at their current divergence. The
   denominator is the CURRENT open count (fixed), so advancing a goal can only
   LOWER risk — never raise it via a shrinking mean. Action-dependent ⇒ it
   re-ranks policies (the belly steers selection — the W1 deliverable in its
   in-memory, deterministic-point-mass form).

   Reduces to `goal-outcome-risk` exactly when the action advances nothing
   (e.g. :no-op) ⇒ static behaviour preserved; [] ⇒ 0.0 ⇒ the static floor.

   Deferred refinements (named, not hidden): the durable `discharged-by` PROOF
   join (M-populate-substrate-2 D4) and a PROBABILISTIC predicted outcome
   (advance with probability p, the full KL) rather than this point-mass flip."
  ([entries action capability-graph]
   (predictive-goal-outcome-risk entries action capability-graph default-goal-outcome-weight))
  ([entries action capability-graph weight]
   (let [open (filter #(= :open (:status %)) entries)
         n    (count open)
         adv  (advanced-outcome-ids action capability-graph)]
     (if (zero? n)
       0.0
       (* (double weight)
          (/ (reduce + 0.0
                     (for [e open]
                       (if (contains? adv (norm-id (-> e :outcome-ref :id)))
                         0.0
                         (risk-of e nil))))
             (double n)))))))

;; ---------------------------------------------------------------------------
;; Extension point — fold the c_vector.bb overlay channels (mess/incomplete/應)
;; ---------------------------------------------------------------------------

(defn merge-entries
  "Fold additional C-entries (e.g. read from c_vector.bb's overlay EDN for the
   mess / incompleteness / 應-voice channels) into the live stated entries,
   de-duplicating by outcome-ref. The extension path that brings the other four
   channels live without duplicating their producers here."
  [base extra]
  (let [seen (set (map :outcome-ref base))]
    (into (vec base) (remove #(seen (:outcome-ref %)) extra))))
