(ns futon2.report.cascade-lane
  "v1 cascade-policy lane for the live WM (M-wm-policies go-live, 2026-06-09).

   ADDITIVE + self-contained — does NOT touch the judge's mission ranking. Given the WM's
   ranked missions, construct the coverage-saturated coherence-greedy *cascade-policy* (a
   pattern-semilattice = a scored ARGUE, Alexander) for each top mission's circumstance |psi>,
   budget-truncated (the parsimony ceiling, set from data). Shells out to the Python
   constructor (cascade_serve.py / minilm) — NOT in-JVM, like the notions path.

   v1-thin: this is THE visible non-degenerate-policy lane. The rollout-over-the-gradient-prior
   deepens at scope-grain v2 (in v1 its reachable set is 3 summits). Sim-only, read-only:
   constructs cascades over a state copy, never promotes/writes."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.lane-futility :as futility]
            [futon2.aif.rollout :as rollout])
  (:import [java.util.concurrent TimeUnit]))

(def ^:private py "/home/joe/code/futon3a/.venv/bin/python")
(def ^:private script "/home/joe/code/futon3a/holes/labs/M-memes-arrows/cascade_serve.py")
(def ^:private script-dir "/home/joe/code/futon3a/holes/labs/M-memes-arrows")
(def default-budget
  ;; 6→20 (operator ruling 2026-07-05, E-live-loop-2 deposit-002 finding):
  ;; at budget 6 the invariant-grade pattern arrivals sat at greedy ranks
  ;; 10-16, outside the fold window. Still a tunable constant, not a
  ;; principle — cascade shaping proper is its own project
  ;; (E-gflownets-fold; the cascade-peripheral idea; E-live-loop-3).
  20)

(def ^:private cascade-timeout-ms
  "Hard ceiling on the Python cascade constructor (minilm cold-load + build). A hung child must NEVER
   wedge the calling scheduler thread permanently (fable-1, WM cycle-#2 hardening). Env-overridable."
  (long (or (some-> (System/getenv "FUTON2_CASCADE_TIMEOUT_MS") parse-long) 30000)))

(defn- read-stream [stream]
  ;; drain in a future so a chatty child can't deadlock on a full pipe buffer
  (future (with-open [rdr (io/reader stream)] (slurp rdr))))

(defn- sh-timed
  "Run cmd (a seq of strings) in dir with a HARD timeout. Returns {:exit :out} or nil on
   timeout/failure. Mirrors futon3c watcher/projections/python.clj: ProcessBuilder + waitFor(timeout)
   + destroy/destroyForcibly, so a hung cascade_serve.py can never wedge the calling (scheduler)
   thread — unlike clojure.java.shell/sh, whose waitFor is unbounded."
  [cmd dir timeout-ms]
  (let [proc (-> (ProcessBuilder. ^java.util.List (vec cmd))
                 (.directory (io/file dir))
                 (.start))
        out* (read-stream (.getInputStream proc))
        _err* (read-stream (.getErrorStream proc))]
    (if (.waitFor proc timeout-ms TimeUnit/MILLISECONDS)
      {:exit (.exitValue proc) :out @out*}
      (do (.destroy proc)
          (when-not (.waitFor proc 1000 TimeUnit/MILLISECONDS)
            (.destroyForcibly proc)
            (.waitFor proc 1000 TimeUnit/MILLISECONDS))
          nil))))

;; A cascade is deterministic in (|psi>, budget) — the pattern library + minilm are stable —
;; so memoize across windows/ticks. Only successes are cached (failures retry).
(defonce ^:private !cache (atom {}))
(defn clear-cache! "Drop the cascade cache (e.g. after the pattern library changes)." [] (reset! !cache {}))

(defn cascade-policy-for
  "Construct the budget-truncated cascade-policy for a circumstance |psi> (text).
   Read-only, sim-only, memoized. Returns the parsed map or nil on failure."
  ([psi-text] (cascade-policy-for psi-text default-budget))
  ([psi-text budget]
   (or (get @!cache [psi-text budget])
       (let [v (try
                 (let [{:keys [exit out]} (sh-timed [py script psi-text (str budget)]
                                                    script-dir cascade-timeout-ms)]
                   (when (and exit (zero? exit))
                     (json/parse-string out true)))
                 (catch Exception _ nil))]
         (when v (swap! !cache assoc [psi-text budget] v))
         v))))

(defn- id-stem-psi
  "The v1 psi: strip M-, hyphens->spaces. Kept as the FALLBACK when no mission
   doc is found (and as the baseline the Q-B scorecard measured against)."
  [target]
  (-> (str target)
      (str/replace #"^M-" "")
      (str/replace #"-" " ")))

(def ^:private mission-doc-roots
  "Where mission docs live, per repo convention (holes/missions/ preferred,
   bare holes/ as secondary)."
  (let [home (System/getProperty "user.home")]
    (for [repo ["futon0" "futon1" "futon2" "futon3" "futon3a" "futon3b" "futon3c"
                "futon4" "futon5" "futon5a" "futon6" "futon7"]
          sub ["holes/missions" "holes"]]
      (str home "/code/" repo "/" sub))))

(defn- mission-doc-file [target]
  (->> mission-doc-roots
       (map #(io/file % (str target ".md")))
       (filter #(.exists ^java.io.File %))
       first))

;; ---------------------------------------------------------------------------
;; L2 (E-live-loop-3, 2026-07-05): sorry-grain ψ from the held-work ledger.
;; ADDITIVE: fires when the target mission has held-work items; falls through
;; to the banner ψ (the existing or-cascade) when it doesn't. The join is
;; id-stem substring match (handles both "M-diagramprover" and
;; "futon3c-d/mission/diagramprover"). Reject-loudly on unreadable ledger
;; (WARN to stderr once per pass, then banner fallback) — DISTINCT from
;; zero-items-for-mission (silent, normal, the additive fallback fires).
;; ---------------------------------------------------------------------------

(def ^:private held-work-ledger-path
  (str (System/getProperty "user.home")
       "/code/futon3c/holes/excursions/held-work-ledger.edn"))

(def ^:private ^:const sorry-psi-byte-cap 1024)

(defonce ^:private !held-work-state
  ;; ::unloaded sentinel (NOT a delay — F6 hygiene, 2026-07-05): the ledger
  ;; snapshot must be invalidatable when the ledger FILE is refreshed, or
  ;; `clear-psi-cache!` rebuilds ψ from a stale snapshot and the "refresh"
  ;; never lands. Fresh var name so a live `load-file` reload starts clean
  ;; (the old realized delay is orphaned, restart-equivalent).
  (atom ::unloaded))

(defn- held-work-items
  "The held-work ledger items, loaded once and cached in `!held-work-state`;
   `clear-psi-cache!` resets it so a refreshed ledger file is re-read.
   Unreadable ledger ⇒ WARN once per load, cache nil (banner-ψ fallback) —
   same semantics as the delay this replaces."
  []
  (let [v @!held-work-state]
    (if (not= ::unloaded v)
      v
      (let [loaded (try
                     (-> held-work-ledger-path slurp edn/read-string :items)
                     (catch Throwable e
                       (binding [*out* *err*]
                         (println "[cascade-lane] WARN held-work ledger unreadable at"
                                  held-work-ledger-path "— falling back to banner ψ for all missions."
                                  "Error:" (.getMessage e)))
                       nil))]
        (reset! !held-work-state loaded)
        loaded))))

(defn- held-items-for
  "Filter the held-work ledger to items whose :held/missions includes the
   target (id-stem substring match, case-insensitive). Returns [] when the
   ledger is nil (unreadable) or no items match."
  [target]
  (let [items (held-work-items)
        stem (-> (str target) (str/replace #".*/" ""))
        stem-lc (str/lower-case stem)]
    (if (or (nil? items) (empty? items))
      []
      (filter (fn [item]
                (some (fn [m]
                        (let [m-stem (-> (str m) (str/replace #".*/" ""))
                              m-lc (str/lower-case m-stem)]
                          (or (str/includes? m-lc stem-lc)
                              (str/includes? stem-lc m-lc))))
                      (:held/missions item)))
              items))))

(defn- sorry-grain-psi
  "Build sorry-grain ψ from held-work items for the target mission.
   Returns nil when no items match (the or-cascade falls through to banner).
   Recipe (S1 v2): WANT: <reason or evidence-condition>. HUNGRY-FOR: <kind>
   when available. HAVE: <re-entry>. Multiple items concatenate, capped at
   ~1KB (sorry-psi-byte-cap) matching the S1 corpus range."
  [target]
  (let [items (held-items-for target)]
    (when (seq items)
      (let [stem (id-stem-psi target)
            entry-str (fn [item]
                        (let [want (or (:held/reason item)
                                       (:held/evidence-condition item))
                              have (:held/re-entry item)
                              kind (:held/kind item)]
                          (str "WANT: " want
                               (when kind (str " HUNGRY-FOR: " kind))
                               (when have (str " HAVE: " have)))))
            full (str/join " " (map entry-str items))
            capped (subs full 0 (min sorry-psi-byte-cap (count full)))]
        (str stem " — " capped)))))

(defonce ^:private !psi-cache (atom {}))

(def ^:private ^:const futility-threshold 10)

(defonce ^:private !futility-state (atom ::unloaded))

(defn- load-futility-counts
  "Read the daily trace and build a map of target-id -> attempt-count for
   lanes with 0 successes (stuck lanes only). Uses the absolute trace path
   (the scheduled runner's cwd is futon2/, the serving JVM's is futon3c/)."
  []
  (try
    (let [trace-dir (str (System/getProperty "user.home") "/code/futon2/data/wm-trace")
          records (futility/trace-records trace-dir)
          summary (futility/futility-summary records)]
      (->> (:rows summary)
           (filter :zero-for-n?)
           (filter #(>= (:attempts %) futility-threshold))
           (map (fn [r] [(str (:target r)) (:attempts r)]))
           (into {})))
    (catch Throwable e
      (binding [*out* *err*]
        (println "[cascade-lane] WARN futility trace unreadable — STUCK lines omitted."
                 "Error:" (.getMessage e)))
      {})))

(defn- futility-count-for
  "Return the 0-for-N count for TARGET if it's stuck (>= threshold), else nil."
  [target]
  (when (= ::unloaded @!futility-state)
    (swap! !futility-state (fn [s] (if (= s ::unloaded) (load-futility-counts) s))))
  (get @!futility-state (str target)))

(defn clear-psi-cache!
  "Drop the ψ cache AND the held-work ledger snapshot (F6 hygiene: both, or a
   refreshed ledger file never lands — missions that gained held items would
   rebuild their ψ from the stale snapshot and get banner ψ again). Call after
   the held-work ledger is refreshed."
  []
  (reset! !held-work-state ::unloaded)
  (reset! !futility-state ::unloaded)
  (reset! !psi-cache {}))

(defn clear-all-caches!
  "THE cache-clear entry point (F6 hygiene, 2026-07-05): cascade memo + ψ memo
   + held-work ledger snapshot, one call. Use after the pattern library /
   embeddings artifact changes (cascades stale) or the held-work ledger is
   refreshed (ψ stale). NB `!rollout-g-cache`/`!rollout-moves` are NOT cleared
   here — the move-set file has its own lifecycle (flagged in F6, untouched)."
  []
  (clear-cache!)
  (clear-psi-cache!))

(defn mission->psi
  "Q-C (E-have-want-pairs, 2026-07-02): a mechanically-derived have→want meme
   psi — the Q-B scorecard's winning M_havewant recipe (mean ΔF +0.093 over the
   id-stem, 58.9% of 319 improved). want = the mission doc's title line (the
   goal phrasing); have = its **Status:** line (the current state). Both are
   read from the doc, truncated, memoized; NO doc found ⇒ the id-stem psi
   (byte-identical to the old behaviour — reduction-safe fallback)."
  [target]
  (or (get @!psi-cache target)
      (let [stem (id-stem-psi target)
            psi (or (sorry-grain-psi target)  ;; L2: sorry-grain from held-work ledger
                    (when-let [f (mission-doc-file target)]
                      (try
                        (let [lines (str/split-lines (slurp f))
                              clip (fn [s n] (let [s (str/trim (str s))]
                                               (subs s 0 (min n (count s)))))
                              title (some->> lines
                                             (filter #(str/starts-with? % "# "))
                                             first
                                             (re-find #"^#\s*(?:Mission:)?\s*(.*)")
                                             second)
                              status (some->> lines
                                              (filter #(re-find #"(?i)^(?:\*\*)?Status:?(?:\*\*)?" %))
                                              first
                                              (re-find #"(?i)^(?:\*\*)?Status:?(?:\*\*)?:?\s*(.*)")
                                              second)]
                          (when title
                            (str stem " — want: " (clip title 160)
                                 (when (seq (str status)) (str ". have: " (clip status 160))))))
                        (catch Throwable _ nil)))
                    stem)
            ;; psi-v3 (E-live-loop-3 Finding-6): append a futility line when
            ;; the target's lane is stuck (0-for-N, N >= 10). Additive — only
            ;; appends; never changes the base psi for healthy missions.
            stuck-n (futility-count-for target)
            psi (if stuck-n
                  (str psi " STUCK: selected " stuck-n " ticks, 0 passes")
                  psi)]
        (swap! !psi-cache assoc target psi)
        psi)))

;; ---------------------------------------------------------------------------
;; Seam 2 (M-wm-policies Car-3 / R16): join the rollout grain-3 G(π) into the lane,
;; so the act-gate's ΔG leg sits beside ΔF (the cascade F-free-energy) at EVAL.
;; Read-only / sim-only: best-rollout does ZERO :7071 writes (MUST-B). nil when the
;; mission has no moves in the v2 set (no rollout path → ΔG unavailable → gate abstains).
;; ---------------------------------------------------------------------------
(def ^:private rollout-move-set-path "/home/joe/code/futon6/data/diffsub-moves.edn")
(defonce ^:private !rollout-moves
  (delay (try (rollout/moves (rollout/load-move-set rollout-move-set-path)) (catch Throwable _ []))))
(defonce ^:private !rollout-cap-overlay
  (delay (into {} (for [cid (keep :advances-cap @!rollout-moves)]
                    [cid {:id (str "scope/capability/" cid)
                          :props {:capability/frontier? true :capability/status :held}}]))))
(defonce ^:private !rollout-g-cache (atom {}))

(defn rollout-g-for
  "grain-3 ΔG: best-rollout G(π) over the v2 move-set restricted to this mission's moves.
   Returns the (negative-better) G as a double, or nil if the mission has no moves in the
   set (no rollout path — ΔG genuinely unavailable, not zero). Memoized per stem."
  [mission-target]
  (let [stem (-> (str mission-target) (str/replace #"^M-" ""))]
    (if (contains? @!rollout-g-cache stem)
      (get @!rollout-g-cache stem)
      (let [g (try
                (let [pat (re-pattern (java.util.regex.Pattern/quote stem))
                      mv  (filter #(re-find pat (str (:have %) (:want %))) @!rollout-moves)]
                  (when (seq mv)
                    (let [seed (rollout/seed-roots {:arrows {} :cap-overlay @!rollout-cap-overlay :reachable #{}} mv)
                          best (rollout/best-rollout seed mv :depth 5 :top-k 3 :gamma 0.9)]
                      (some-> (:G best) double))))
                (catch Throwable _ nil))]
        (swap! !rollout-g-cache assoc stem g)
        g))))

(def ^:dynamic *gate-decision-target?*
  "When true (DEFAULT, operator ruling 2026-07-06: \"Yes, we should accept
  the decision so that the machine can act on its decision\"), the
  cascade-lane prepends the judge's rank-1 DECISION target as entry #1 --
  whatever its action type -- so the gate evaluates what the machine
  actually decided, not just the open-mission side-stream. Armed by
  claude-16 bell invoke-1783332928386-617-4f58c0cd. When false, the lane
  is byte-identical to the pre-ruling composition (open-mission only)."
  true)

(defn- decision-entry
  "Extract the rank-1 decision entry from ranked-actions (ANY action type
  with a :target). Returns nil when the first ranked action has no :target
  or when rank-1 IS an :open-mission (handled by the existing filter, so
  dedup is automatic)."
  [ranked-actions]
  (let [first-entry (first ranked-actions)
        action-type (get-in first-entry [:action :type])
        target      (get-in first-entry [:action :target])]
    (when (and target
               (not (#{:open-mission "open-mission"} action-type)))
      first-entry)))

(defn cascade-lane
  "The v1 cascade lane: for the top-n :open-mission targets in ranked-actions, build the
   cascade-policy for each circumstance. Returns
   [{:mission :psi :size :wholeness :budget :truncated :shown [pattern-ids...]} ...].
   When *gate-decision-target?* is true (default, operator ruling 2026-07-06), entry #1
   is the judge's actual top decision (any action type with a :target), so the gate
   checks what the machine decided -- not just the open-mission side-stream."
  ([ranked-actions] (cascade-lane ranked-actions {}))
  ([ranked-actions {:keys [n budget] :or {n 3 budget default-budget}}]
   (let [build-entry (fn [e]
                       (let [m (get-in e [:action :target])
                             psi (str/trim (str (mission->psi m) " "
                                                (or (get-in e [:action :rationale]) "")))
                             c (cascade-policy-for psi budget)]
                         (when c
                           (let [base-shown (mapv :pattern (:shown c))
                                 stuck-n (futility-count-for m)
                                 seated? (boolean stuck-n)
                                 shown (if seated?
                                         (vec (cons "agent/sense-deliberate-act" base-shown))
                                         base-shown)]
                             {:mission m :psi psi
                              :size (if seated? (inc (:size c)) (:size c))
                              :wholeness (:wholeness c) :budget (:budget c)
                              :truncated (:truncated c)
                              :F-free-energy (:F-free-energy c)
                              :G-rollout (rollout-g-for m)
                              :shown shown
                              :semilattice (:semilattice c)
                              :seat-injection (when seated?
                                                {:pattern "agent/sense-deliberate-act"
                                                 :stuck-n stuck-n
                                                 :mechanism :construction-not-retrieval
                                                 :card-3 "W-constructor-df-last-inch"})}))))
         om-entries (->> ranked-actions
                         ;; live-judgement comes through the JSON API -> :type is the STRING "open-mission";
                         ;; the in-process path uses the keyword. Accept both.
                         (filter #(#{:open-mission "open-mission"} (get-in % [:action :type])))
                         (take n)
                         (keep build-entry)
                         vec)
         decision-entries (when *gate-decision-target?*
                            (when-let [de (decision-entry ranked-actions)]
                              (let [dec-target (get-in de [:action :target])
                                    already-in? (some #(= (:mission %) dec-target) om-entries)]
                                (when-not already-in?
                                  (when-let [built (build-entry de)]
                                    [built])))))]
     (if *gate-decision-target?*
       (vec (concat decision-entries om-entries))
       om-entries))))

(def ^:private gap-free-energy-threshold
  "A cascade whose marginal-likelihood F = accuracy − λ·complexity falls at/below this is a
   defensive-driving GAP. F is the AIF-grounded grain-2 act-gate leg (M-wm-policies omission
   2): F > 0 = the cascade's ψ-coverage outweighs the complexity of justifying its patterns
   against the base-rate prior = a net-positive model expansion (Bayesian Occam accept). This
   REPLACES the Salingaros-C / coverage-size heuristics — C was an analogy (L=T·H is not
   formally a free-energy functional), size was scale-but-not-prior-aware. λ is set from data
   in cascade_construct.py (the rich/thin F=0 knee, λ=0.25 on the 2026-06-24 spectrum)."
  0.0)

(defn gap-lane
  "Defensive-driving HORIZON SCAN (M-wm-policies Track 3, proactive). For the top-n
   open-mission targets, construct each circumstance's cascade and flag those whose
   marginal-likelihood F ≤ gap-F-threshold (or no cascade) as PATTERN-GAPS — classes whose
   coverage does not pay for its pattern-complexity = the WM has no good-enough patterns for
   them yet, the places to seed cascades *before* it gets stuck. Reuses cascade-policy-for
   (read-only / sim-only / never promotes — WM-I4 intact) and its memo cache. F is the
   AIF act-gate leg; wholeness/size reported as context. Returns
   [{:mission :psi :F-free-energy :accuracy :complexity :wholeness :size :gap? :note} ...], gaps first (lowest F)."
  ([ranked-actions] (gap-lane ranked-actions {}))
  ([ranked-actions {:keys [n budget] :or {n 10 budget default-budget}}]
   (->> ranked-actions
        (filter #(#{:open-mission "open-mission"} (get-in % [:action :type])))
        (take n)
        (map (fn [e]
               (let [m   (get-in e [:action :target])
                     psi (str/trim (str (mission->psi m) " "
                                        (or (get-in e [:action :rationale]) "")))
                     c   (cascade-policy-for psi budget)
                     free-energy (:F-free-energy c)
                     gap? (or (nil? c) (nil? free-energy) (<= free-energy gap-free-energy-threshold))]
                 {:mission m :psi psi :F-free-energy free-energy
                  :G-rollout (rollout-g-for m)                 ; ΔG leg beside ΔF (Car-3 seam 2)
                  :accuracy (:accuracy c) :complexity (:complexity c)
                  :wholeness (:wholeness c) :size (:size c) :gap? gap?
                  :note (cond
                          (nil? c)
                          "no cascade constructed (constructor unavailable or no patterns — foothold needed)"
                          gap?
                          (format "F=%.2f ≤ 0 (acc %.2f < λ·complexity) — coverage doesn't pay for its patterns; seed here"
                                  (double (or free-energy 0.0)) (double (or (:accuracy c) 0.0)))
                          :else
                          (format "F=%.2f > 0 (acc %.2f vs λ·complexity) — net-positive cascade"
                                  (double free-energy) (double (or (:accuracy c) 0.0))))})))
        (sort-by (fn [r] [(if (:gap? r) 0 1) (or (:F-free-energy r) 0.0)]))
        vec)))
