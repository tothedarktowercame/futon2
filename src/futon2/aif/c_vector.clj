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
            [clojure.java.io :as io]
            [babashka.http-client :as http]
            [futon2.aif.preferences :as pref]))

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

(defn- frac
  "Normalise a one-sided shortfall to [0,1] as a fraction of the target — so a
   range channel (mess coherence, 0–35) is COMMENSURABLE with a :becomes channel
   (0/1) when both feed one mean (the W4 scale concern). 0 target ⇒ any shortfall
   is full risk."
  [shortfall value]
  (let [v (Math/abs (double value))]
    (if (zero? v) (if (pos? shortfall) 1.0 0.0)
        (min 1.0 (/ (double shortfall) v)))))

(defn- divergence
  "Normalised distance ([0,1]) of the current outcome from the preferred floor.
   `:becomes` = a binary goal (cap attested / sorry closed): full unit risk
   while unmet, 0 when reached. Range ops (`:>=`/`:<=`) return the fractional
   shortfall so channels stay commensurable (W4)."
  [current {:keys [op value]}]
  (case op
    :>=      (frac (max 0.0 (- (double value) (double current))) value)
    :<=      (frac (max 0.0 (- (double current) (double value))) value)
    :becomes (if (= current value) 0.0 1.0)
    (min 1.0 (Math/abs (double (- (double current) (double value)))))))

(defn- current-outcome
  "The current observable outcome a C-entry is scored against. `:becomes`
   derived entries are emitted only while UNMET ⇒ the not-yet sentinel; range
   channels carry their current reading in `:provenance` as `:current` (live),
   `:L` (mess per-mission coherence), or `:value` (mess standing-regularizer)."
  [e]
  (or (get-in e [:provenance :current])
      (get-in e [:provenance :L])
      (get-in e [:provenance :value])
      ::unmet))

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

(declare merge-entries)
(declare !durable-join-fn)
(declare build-durable-adv)
(declare durable-join-stats)

;; The non-stated channels (mess / incompleteness / 應-voice) are produced by
;; futon6/scripts/c_vector.bb into overlay EDN. The stated channel is derived
;; natively + LIVE here; these are folded in SEMI-LIVE (as fresh as the last
;; c_vector.bb run) — graceful if futon6 isn't present (override the dir via
;; C_VECTOR_OVERLAY_DIR). R19-CHANNELS.
(def ^:private overlay-dir
  (or (System/getenv "C_VECTOR_OVERLAY_DIR")
      "/home/joe/code/futon6/data/c-vector"))

(def ^:private overlay-files
  ["c-entries.mess.edn" "c-entries.incomplete.edn" "c-entries.yingvoice.edn"])

(defn read-overlay-channels
  "Read the non-stated channels from the c_vector.bb overlays. Returns the
   merged entry vector ([] if absent/unreadable — the belly degrades to the
   live stated channel)."
  []
  (vec (mapcat (fn [fname]
                 (let [f (io/file overlay-dir fname)]
                   (when (.exists f)
                     (try (:entries (edn/read-string (slurp f))) (catch Exception _ nil)))))
               overlay-files)))

(defn refresh!
  "Re-derive the live C-vector (stated channel from :7071), FOLD the overlay
   channels (mess/incompleteness/應-voice), and re-pull the DURABLE
   discharged-by join (§11 step 4 — compiled to the `:durable-adv` forward-model
   lookup), then swap the atom. OFF-CYCLE only (watcher / periodic probe) —
   never the per-action tick (§5 derive-and-cache). On an empty stated derive
   (store down) the atom is left UNCHANGED — the last-good C stays in force;
   likewise a failed join fetch keeps the last-good `:durable-adv` (the belly
   is never clobbered to empty). NB the corpus signature does not observe
   relation-only changes, so the join's staleness is bounded by the refresh
   cadence (any corpus change, or `ensure-belly-fresh!`'s debounce window),
   not detected by `stale?`."
  []
  (let [{:keys [entries signature n-source]} (derive-stated)]
    (if (seq entries)
      (let [overlay (read-overlay-channels)
            folded  (merge-entries entries overlay)
            join    (try (@!durable-join-fn) (catch Throwable _ nil))
            adv     (when join (build-durable-adv join))]
        (reset! c-state {:entries folded :signature signature
                         :derived-at (java.time.Instant/now)
                         :durable-adv (or adv (:durable-adv @c-state))
                         :durable-join-meta (or (durable-join-stats join)
                                                (:durable-join-meta @c-state))
                         :n-source (assoc n-source :overlay (count overlay)
                                          :stated (count entries) :total (count folded))}))
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

(def ^:private default-debounce-ms
  "Refresh the belly at most once per this window when demand-driven (5 min)."
  300000)

(defonce ^:private !last-refresh-ms (atom 0))

(defn ensure-belly-fresh!
  "DEMAND-DRIVEN, debounced belly refresh — the safe replacement for a
   background poll loop (post-incident 2026-06-26; see E-arxana-clock). Call at
   SCORE TIME (when the belly is about to be read for EFE); it re-derives at most
   once per `debounce-ms` (default 5 min) and ONLY when actually called — so
   there is no perpetual loop competing with the request path. Concurrency-safe
   via compare-and-set! (one caller wins the refresh; the rest read the current
   atom). Synchronous + bounded (a couple :7071 reads + the overlay files, rare);
   reads substrate-2, never the evidence/invoke path. Returns the live C-vector.

   NB this never refreshes inside a unit test that doesn't call it — nothing
   auto-invokes it; the scoring entrypoints (wm.scheduler tick, wm_scheduled_run)
   do."
  ([] (ensure-belly-fresh! default-debounce-ms))
  ([debounce-ms]
   (let [now  (System/currentTimeMillis)
         last @!last-refresh-ms]
     (when (and (> (- now last) (long debounce-ms))
                (compare-and-set! !last-refresh-ms last now))
       (try (refresh!) (catch Throwable _ nil))))
   (current-c-vector)))

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
   C-entries (often strings) on a common normal form. Keywords keep their
   NAMESPACE (str (symbol kw)) — (name kw) silently dropped :sorry/foo → foo,
   half of the 2026-07-02 vocabulary-correlation gap."
  [x]
  (when (some? x)
    (if (keyword? x) (str (symbol x)) (str x))))

(defn- id-tokens
  "The normalized MATCH TOKENS of one id: the raw normal form plus its
   known-alias tails, so the mission/sorry vocabularies correlate —
   \"mission/M-x\" ↔ \"M-x\" ↔ \"<repo>-d/mission/x\" tail, \"sorry/y\" ↔ \"y\".
   Bounded alias set, documented over-match risk accepted for the IN-MEMORY
   stand-in; canonical identity is the durable join's job (§11)."
  [x]
  (when-let [s (norm-id x)]
    ;; blank ids (e.g. a correction entry's empty :ref-id) yield NO tokens —
    ;; a \"\" token would make every blank-id entry match every join expansion
    (when-not (str/blank? s)
      (let [tail (peek (str/split s #"[/|]"))]   ; devmap sorries are |-delimited
        (cond-> #{s}
          (and tail (not= tail s)) (conj tail)
          ;; canonical <repo>-d/mission/<stem> ↔ M-<stem>
          (re-find #"(?:^|/)mission/" s) (conj (str "M-" tail)))))))

(defn- ref-tokens
  "All match tokens of a C-entry's :outcome-ref, across the CHANNEL SHAPES
   (the 2026-07-02 finding: only the stated channel used :id — incompleteness
   carries :mission (\"M-x\"), the 應-voice reach channel carries :referent
   (\"mission/M-x\"); 314/455 live entries were unmatchable, so the belly's
   predictive term was CONSTANT across policies in every live tick)."
  [outcome-ref]
  (into #{} (mapcat id-tokens)
        (keep outcome-ref [:id :mission :referent])))

;; ---------------------------------------------------------------------------
;; The DURABLE discharged-by join (E-C-vector-live §11 step 4) — read at refresh
;; time from substrate-2's persisted relations, cached in c-state, consulted by
;; `advanced-outcome-ids` with the token-match as the uncovered-slice fallback.
;; ---------------------------------------------------------------------------

(def ^:private uuid-re
  #"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

(defn- fetch-durable-join*
  "Read the persisted join from substrate-2: the `:outcome-ref` relations
   (c-entry → canonical mission node, 189 as of 2026-07-02) and the
   `:discharged-by` relations (c-entry → method/class ∪ mission → commit, the
   proof-mine grain). Relations are `:relation/*` docs with NO HTTP read route
   (the 2026-07-02 claude-3 finding), so this reads the in-JVM XTDB node via
   requiring-resolve — the same dynamic-resolve discipline as
   `credit-satisfy-prob`: in the live serving JVM (where the WM scheduler and
   the store cohabit) it resolves; in bb / futon2's own test JVM it returns nil
   and the belly degrades to the in-memory token-match. Zero writes.

   Returns {:oref [[c-entry-name mission-name] …]
            :disch [[from-name to-name] …]
            :entry-refs {c-entry-name outcome-ref-map}} or nil."
  []
  (try
    (when-let [sysvar (try (requiring-resolve 'futon3c.dev/!f1-sys)
                           (catch Throwable _ nil))]
      (when-let [node (:node @@sysvar)]
        (let [dbf  @(requiring-resolve 'xtdb.api/db)
              q    @(requiring-resolve 'xtdb.api/q)
              db   (dbf node)
              rel  (fn [ty] (q db '{:find [f t] :in [ty]
                                    :where [[r :relation/type ty]
                                            [r :relation/from f] [r :relation/to t]]}
                               ty))
              oref  (rel :outcome-ref)
              disch (rel :discharged-by)
              uuid? (fn [x] (and (string? x) (re-matches uuid-re x)))
              ids   (into #{} (filter uuid?)
                          (mapcat identity (concat oref disch)))
              named (into {} (keep (fn [i]
                                     (when-let [[n p] (first (q db '{:find [n p] :in [i]
                                                                     :where [[e :entity/id i]
                                                                             [e :entity/name n]
                                                                             [e :entity/props p]]}
                                                                i))]
                                       [i {:name n :props p}])))
                          ids)
              nm    (fn [x] (get-in named [x :name] x))
              entry-refs (into {} (keep (fn [[_ {ename :name props :props}]]
                                          (when-let [ee (:entry-edn props)]
                                            (when-let [r (try (:outcome-ref (edn/read-string (str ee)))
                                                              (catch Throwable _ nil))]
                                              [ename r]))))
                               named)]
          {:oref  (mapv (fn [[f t]] [(nm f) (nm t)]) oref)
           :disch (mapv (fn [[f t]] [(nm f) (nm t)]) disch)
           :entry-refs entry-refs})))
    (catch Throwable _ nil)))

(defonce ^{:doc "Injectable source of the durable join (0-arity → the
  fetch-durable-join* shape, or nil). A seam so tests inject fixture relations
  and other hosts can supply their own store access."}
  !durable-join-fn
  (atom fetch-durable-join*))

(defn build-durable-adv
  "Pure: compile the fetched durable join into the forward-model lookup
   {advancer-token → #{c-entry ref tokens}}. Two edge families feed it:
   - `:outcome-ref` (c-entry → mission): an action reaching the mission (by any
     of its id-tokens) advances that c-entry — this is what covers entries whose
     own :outcome-ref vocabulary the token-match cannot correlate.
   - `:discharged-by` where the FROM side is a c-entry (c-entry → method/class):
     an action of that method class advances the entry — the \"which C-entries
     does this policy's method discharge?\" read (§11 step 4).
   Mission → commit `:discharged-by` rows (the proof-mine grain) are past
   discharge EVIDENCE, not a forward mapping — they are not compiled in (they
   feed the step-5 reconcile report), which `durable-join-stats` counts."
  [{:keys [oref disch entry-refs]}]
  (let [etoks (fn [cname]
                (into (or (some-> (get entry-refs cname) ref-tokens) #{})
                      (id-tokens cname)))
        add   (fn [m advancer cname]
                (let [ts (etoks cname)]
                  (reduce (fn [m tok] (update m tok (fnil into #{}) ts))
                          m (id-tokens advancer))))]
    (as-> {} m
      (reduce (fn [m [c mission]] (add m mission c)) m oref)
      (reduce (fn [m [f t]] (if (contains? entry-refs f) (add m t f) m)) m disch))))

(defn durable-join-stats
  "The reconcile numbers (§11 step 5) of one fetched join: edge counts by
   family + grain, and how many c-entries the compiled forward map can reach."
  [{:keys [oref disch entry-refs] :as join}]
  (when join
    {:outcome-ref        (count oref)
     :disch-entry->method (count (filter #(contains? entry-refs (first %)) disch))
     :disch-mission->commit (count (remove #(contains? entry-refs (first %)) disch))
     :c-entries-resolved (count entry-refs)
     :advancer-tokens    (count (build-durable-adv join))}))

(defn advanced-outcome-ids
  "The set of match tokens for the C-entry outcomes a candidate action is
   predicted to advance. The base set reuses the existing action / star-map
   structure rather than a parallel map: an explicit `:advances-outcomes` on
   the action (the override seam) ∪ the action's `:target` ∪ (for
   `:open-mission`) the mission's `:produces` caps from the capability graph.
   Tokenised (id-tokens) on both sides so keyword / alias / canonical
   vocabularies correlate.

   The base set is then EXPANDED through the durable discharged-by join
   (§11 step 4): every base token — plus the action's method/`:type` tokens,
   which is how a method-class edge fires — that keys the compiled
   {advancer-token → #{entry tokens}} map unions in the c-entries substrate-2
   says that mission/method discharges. The 2-arity reads the join cached in
   `c-state` by `refresh!` (nil ⇒ no expansion ⇒ the pre-durable behaviour,
   which stays the fallback for the uncovered slice)."
  ([action capability-graph]
   (advanced-outcome-ids action capability-graph (:durable-adv @c-state)))
  ([action capability-graph durable-adv]
   (let [target   (:target action)
         produced (when (= :open-mission (:type action))
                    (get-in capability-graph [:missions target :produces]))
         base     (into #{} (mapcat id-tokens)
                        (concat (:advances-outcomes action) [target] produced))]
     (if (seq durable-adv)
       (into base (mapcat #(get durable-adv %))
             (into base (id-tokens (:type action))))
       base))))

(defn ref-advanced?
  "Does the action's advanced-token set intersect this outcome-ref's tokens?
   The in-memory join predicate (both sides tokenised)."
  [adv outcome-ref]
  (boolean (seq (filter adv (ref-tokens outcome-ref)))))

(defn credit-satisfy-prob
  "R19-KL: P(an action of this class actually satisfies its goal) — the
   probability the predictive risk weights with. Sourced from the R12 Beta-credit
   learner (`futon2.aif.intrinsic-values/credit-for`, in [0,1], 0.5 prior).
   Dynamic-resolve keeps this ns babashka-loadable (R19-UNIFY) and degrades to
   the point-mass p=1 when the learner isn't on the classpath. As the learner
   accumulates real follow-through, p sharpens away from the 0.5 prior and the
   belly's predictive risk improves — the burn-in (README-loss.md)."
  [action]
  (or (when-let [cls (:type action)]
        (when-let [cf (try (requiring-resolve 'futon2.aif.intrinsic-values/credit-for)
                           (catch Throwable _ nil))]
          (try (let [p (cf cls)] (when (number? p) (double p))) (catch Throwable _ nil))))
      1.0))

(defn predictive-goal-outcome-risk
  "The CANONICAL EFE goal-outcome risk: weight·MEAN divergence of the PREDICTED
   outcomes under the policy from C. An entry the action advances is predicted
   satisfied with probability p = `(satisfy-prob-fn action)` ⇒ it contributes its
   EXPECTED residual risk `(1-p)·risk-of` (R19-KL); the rest stay at their
   current divergence. The denominator is the CURRENT open count (fixed), so
   advancing a goal can only LOWER risk. Action-dependent ⇒ it re-ranks policies.

   `satisfy-prob-fn` defaults to `credit-satisfy-prob` (the R12-credit-weighted,
   burn-in form). **Point-mass** (the prior deterministic flip) is the special
   case `(constantly 1.0)` ⇒ advanced entries contribute 0. With no advanced
   entries (e.g. :no-op) it reduces to `goal-outcome-risk`; [] ⇒ 0.0 (floor).

   The advanced set now reads the DURABLE discharged-by join for the covered
   slice (§11 step 4, via the `:durable-adv` cache in `c-state`); the in-memory
   token-match remains the fallback for the uncovered entries."
  ([entries action capability-graph]
   (predictive-goal-outcome-risk entries action capability-graph default-goal-outcome-weight credit-satisfy-prob))
  ([entries action capability-graph weight]
   (predictive-goal-outcome-risk entries action capability-graph weight credit-satisfy-prob))
  ([entries action capability-graph weight satisfy-prob-fn]
   (let [open (filter #(= :open (:status %)) entries)
         n    (count open)
         adv  (advanced-outcome-ids action capability-graph)
         ;; (1-p): expected residual risk of an advanced entry — p chance it is
         ;; satisfied (0 risk), (1-p) chance it still carries its divergence.
         residual (- 1.0 (max 0.0 (min 1.0 (double (satisfy-prob-fn action)))))]
     (if (zero? n)
       0.0
       (* (double weight)
          (/ (reduce + 0.0
                     (for [e open]
                       (if (ref-advanced? adv (:outcome-ref e))
                         (* residual (risk-of e nil))
                         (risk-of e nil))))
             (double n)))))))

;; ---------------------------------------------------------------------------
;; KL form of the goal-outcome risk — the item-5 Bernoulli consumer
;; (E-KL-refinements item 5; contract E-C-vector-live §12 boundary note).
;; LIVE since 2026-07-04 (D-1e operator flip, M-aif-faithfulness §2.1): the
;; arena resolves :goal-outcome-mode :kl by default (arena-goal-outcome-mode
;; in war_machine.clj; FUTON_WM_GOAL_OUTCOME_MODE=hinge escape hatch); the
;; library default in compute-efe stays :hinge, so tests and non-arena
;; callers are byte-identical. W1 CONSUMES pref/c-distribution + pref/kl —
;; no private C.
;; ---------------------------------------------------------------------------

(defn kl-risk-of
  "One OPEN `:becomes` entry's goal-outcome risk in the KL form (nats):
   weight · KL(Bernoulli(q-sat) ‖ pref/c-distribution {:becomes 1}), where
   q-sat is the policy's predicted probability the entry's outcome is MET.
   The preference target is always 1 (met) in outcome space — W1's `:becomes`
   values are domain keywords (:attested / :closed) naming WHAT is met, not a
   binary to hit, so the Bernoulli target is fixed and the keyword stays in
   the entry's provenance. Non-`:becomes` / non-open entries ⇒ 0.0 (their KL
   form needs the forward model's per-channel Gaussian Q — efe's lane, not
   here). NB units: at temperature T an unmet entry (q≈0) costs ≈ 1/T nats
   (T=0.1 ⇒ ≈10), vs the hinge form's ≤ weight·1.0 — the scale friction is
   item 3's calibration question, named in the §12 round-trip note."
  ([e q-sat] (kl-risk-of e q-sat pref/default-c-temperature))
  ([{:keys [preferred weight status]} q-sat temperature]
   (if (and (= status :open) (= :becomes (:op preferred)))
     (* (double (:value weight))
        (pref/kl {:kind :bernoulli :p (double q-sat)}
                 (pref/c-distribution {:becomes 1} :temperature temperature)))
     0.0)))

(defn predictive-goal-outcome-risk-kl
  "KL twin of `predictive-goal-outcome-risk` with the `:becomes` entries
   scored by the exact Bernoulli KL (item 5) instead of the hinge: an advanced
   entry's q-sat = (satisfy-prob-fn action); a non-advanced entry's q-sat = 0.0
   (still unmet under this policy; `pref/kl` clamps to 1e-9). Range entries
   keep their hinge divergence — mixing nats (:becomes, scale ~1/T) with the
   hinge's [0,1] in one mean is UNIT-MIXING, deliberate and visible. Same
   fixed denominator and [] ⇒ 0.0 floor as the hinge form.

   HONESTY: LIVE in the arena since 2026-07-04 (D-1e operator flip; was the
   dark twin, eb06565). The flip was taken WITH the known ≈×9.6 nats-vs-hinge
   scale and WITHOUT a fitted T — t-calibration (22b0024) showed no T matches
   the hinge's dispersion (structural gap), so calibration could not gate it.
   T = pref/default-c-temperature 0.1; escape hatch
   FUTON_WM_GOAL_OUTCOME_MODE=hinge (arena-goal-outcome-mode,
   war_machine.clj)."
  ([entries action capability-graph]
   (predictive-goal-outcome-risk-kl entries action capability-graph
                                    default-goal-outcome-weight credit-satisfy-prob
                                    pref/default-c-temperature))
  ([entries action capability-graph weight satisfy-prob-fn temperature]
   (let [open (filter #(= :open (:status %)) entries)
         n    (count open)
         adv  (advanced-outcome-ids action capability-graph)
         p    (max 0.0 (min 1.0 (double (satisfy-prob-fn action))))]
     (if (zero? n)
       0.0
       (* (double weight)
          (/ (reduce + 0.0
                     (for [e open]
                       (if (= :becomes (get-in e [:preferred :op]))
                         (kl-risk-of e (if (ref-advanced? adv (:outcome-ref e)) p 0.0)
                                     temperature)
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
