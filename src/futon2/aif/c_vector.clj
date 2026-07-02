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
  "Re-derive the live C-vector (stated channel from :7071) and FOLD the overlay
   channels (mess/incompleteness/應-voice), then swap the atom. OFF-CYCLE only
   (watcher / periodic probe) — never the per-action tick (§5 derive-and-cache).
   On an empty stated derive (store down) the atom is left UNCHANGED — the
   last-good C stays in force; the belly is never clobbered to empty."
  []
  (let [{:keys [entries signature n-source]} (derive-stated)]
    (if (seq entries)
      (let [overlay (read-overlay-channels)
            folded  (merge-entries entries overlay)]
        (reset! c-state {:entries folded :signature signature
                         :derived-at (java.time.Instant/now)
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
    (let [tail (peek (str/split s #"[/|]"))]   ; devmap sorries are |-delimited
      (cond-> #{s}
        (and tail (not= tail s)) (conj tail)
        ;; canonical <repo>-d/mission/<stem> ↔ M-<stem>
        (re-find #"(?:^|/)mission/" s) (conj (str "M-" tail))))))

(defn- ref-tokens
  "All match tokens of a C-entry's :outcome-ref, across the CHANNEL SHAPES
   (the 2026-07-02 finding: only the stated channel used :id — incompleteness
   carries :mission (\"M-x\"), the 應-voice reach channel carries :referent
   (\"mission/M-x\"); 314/455 live entries were unmatchable, so the belly's
   predictive term was CONSTANT across policies in every live tick)."
  [outcome-ref]
  (into #{} (mapcat id-tokens)
        (keep outcome-ref [:id :mission :referent])))

(defn advanced-outcome-ids
  "The set of match tokens for the C-entry outcomes a candidate action is
   predicted to advance — the IN-MEMORY core of the discharged-by join (the
   durable PROOF-store version is M-populate-substrate-2 D4, out of scope). It
   reuses the existing action / star-map structure rather than a parallel map:
   an explicit `:advances-outcomes` on the action (the override seam) ∪ the
   action's `:target` ∪ (for `:open-mission`) the mission's `:produces` caps
   from the capability graph. Tokenised (id-tokens) on both sides so keyword /
   alias / canonical vocabularies correlate."
  [action capability-graph]
  (let [target   (:target action)
        produced (when (= :open-mission (:type action))
                   (get-in capability-graph [:missions target :produces]))]
    (into #{} (mapcat id-tokens)
          (concat (:advances-outcomes action) [target] produced))))

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

   Deferred refinement (named): the durable `discharged-by` PROOF join
   (M-populate-substrate-2 D4) replacing the in-memory id-match — §11."
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
