(ns wm-outer-loop
  "Outer-loop scheduled-execution entrypoint for the WM AIF apparatus
   (R12 narrow take-up at stack-level Q6).

   What this does on each run:
     1. Read the trace window (last N daily wm-trace files via
        `futon2.aif.trace/read-trace-range`).
     2. Extract `:learn-action-class` emissions per `:target-class`.
     3. For each class with substrate (per Stream B definitions in the
        design-choices doc), derive follow-through count from git history.
     4. Compute Beta(α, β) update via `futon2.aif.intrinsic-values/next-record`.
     5. Persist one `code/v05/wm-hyperparameter-update` hyperedge per class
        to futon1a XTDB.
     6. Apply the record to the in-process atom (no-op in the cron-script
        case where the JVM exits after; useful for REPL invocations).
     7. Print a one-line summary per class, mirror wm_scheduled_run.

   Contract: contributes to R12 narrow take-up apparatus. Per the design-
   choices doc §9, on day 1 the Beta posterior remains at Beta(1,1) for
   every class until R10 has accumulated emission trace AND operator
   follow-through has happened against those emissions.

   Cadence (recommended): daily, `0 4 * * *`. Installation is operator
   action like :wm-scheduled. Failure-mode discipline mirrors
   `wm_scheduled_run`: try/catch around all work; per-class failures are
   skipped not faulted; non-zero exit signals systemic failure only.

   Substrate paths used (defaults; overridable via env):
     FUTON_REPO_ROOTS  — colon-sep list of repos to scan for
                         mission-doc additions; defaults to
                         '$HOME/code/futon0:$HOME/code/futon2:$HOME/code/futon3c:$HOME/code/futon4:$HOME/code/futon5a'
     FUTON1A_BASE_URL  — futon1a HTTP API base; defaults to
                         http://localhost:7071/api/alpha"
  (:require [futon2.aif.trace :as trace]
            [futon2.aif.intrinsic-values :as iv]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import (java.time Instant LocalDate ZoneOffset)
           (java.time.format DateTimeFormatter)))

(def ^:const default-window-days 14)

(def default-window-days-by-class
  "Per-class window overrides for the R12 Beta inference (§11.c).

   Different WM action-classes have different natural follow-through
   cadences. `:open-mission` is slow (1 new mission per 2-4 weeks is
   common); `:address-sorry` is faster (operator could close one per
   day); `:fire-pattern` doesn't matter today (substrate-unavailable).

   Operator overrides this map via `WM_OUTER_LOOP_WINDOW_BY_CLASS_EDN`
   env var (an EDN-encoded `{:class N ...}` map). Classes not in the
   resulting map fall back to `default-window-days`.

   The trace-read uses the MAX window across all classes so a single
   read covers everyone; per-class filtering then trims records by
   class-specific timestamp cutoff."
  (let [env (System/getenv "WM_OUTER_LOOP_WINDOW_BY_CLASS_EDN")
        overrides (when (and env (not (clojure.string/blank? env)))
                    (try (clojure.edn/read-string env) (catch Exception _ nil)))]
    (merge {:address-sorry 14
            :open-mission  30
            :fire-pattern  14}
           (or overrides {}))))

(defn- window-for-class [class]
  (get default-window-days-by-class class default-window-days))

(defn- max-window-days []
  (apply max default-window-days
         (vals default-window-days-by-class)))

(def ^:private home (System/getProperty "user.home"))

(def ^:private repo-roots
  (let [env (System/getenv "FUTON_REPO_ROOTS")]
    (if (and env (not (str/blank? env)))
      (vec (str/split env #":"))
      [(str home "/code/futon0")
       (str home "/code/futon2")
       (str home "/code/futon3c")
       (str home "/code/futon4")
       (str home "/code/futon5a")])))

(def ^:private wm-repo (str home "/code/futon2"))

;; ---------------------------------------------------------------------------
;; Trace window — emissions per class
;; ---------------------------------------------------------------------------

(defn- read-window-records
  "Read trace records covering the last WINDOW-DAYS UTC days (inclusive).
   Caller uses `max-window-days` so a single read covers the widest
   per-class window; per-class filtering trims by timestamp."
  [window-days]
  (let [end (LocalDate/now ZoneOffset/UTC)
        start (.minusDays end (long (dec window-days)))]
    (trace/read-trace-range start end)))

(defn- ms-since-record
  "Milliseconds between REC's `:timestamp` and now. Returns nil for records
   missing/malformed timestamps."
  [rec]
  (when-let [ts (:timestamp rec)]
    (try (- (.toEpochMilli (Instant/now))
            (.toEpochMilli (Instant/parse ts)))
         (catch Exception _ nil))))

(defn- records-within-window
  "Filter RECORDS to those whose `:timestamp` is within WINDOW-DAYS of now.
   Records with no/bad timestamp are excluded (defensive — don't fold
   undatable evidence into a windowed inference)."
  [records window-days]
  (let [cutoff-ms (* 1000 60 60 24 (long window-days))]
    (filter (fn [rec]
              (when-let [age-ms (ms-since-record rec)]
                (<= age-ms cutoff-ms)))
            records)))

(defn- extract-emissions
  "Walk RECORDS and emit per-class emission counts.

   §2.J reframe (per Joe directive 2026-05-21): an emission for class C is
   any of:
     (a) a `:learn-action-class :target-class C` recommendation in
         `:ranked-actions` — the historical gap-class signal
     (b) the WM's chosen `:decision :action :type C` — the WM endorsed
         this class on this tick

   We count each trace record AT MOST ONCE per class (the union of (a) and
   (b) is taken per-record), so the count is in opportunity-units —
   number of WM ticks that surfaced this class as either a gap recommendation
   OR a chosen action. This makes the R12 Beta inference apply to
   currently-addressable classes (e.g. `:address-sorry`) which never appear
   as `:learn-action-class` today; without this, their posterior stays
   vacuously at Beta(1,1) prior forever.

   See `~/code/futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md`
   §15 for the rationale; see `~/code/futon7/holes/M-war-machine-aif-last-mile.md`
   §2.I for the future-direction note about temporal decay on β-evidence."
  [records]
  (->> (for [rec records
             class (let [decision-class (some-> rec :decision :action :type)
                         learn-classes (->> (:ranked-actions rec)
                                            (map :action)
                                            (filter #(= :learn-action-class (:type %)))
                                            (map :target-class))]
                     ;; Union per-record; one record contributes one tick
                     ;; per class regardless of how many learn-action-class
                     ;; entries it carries for that class.
                     (cond-> (set learn-classes)
                       decision-class (conj decision-class)))]
         class)
       (frequencies)
       ;; Strip non-credit-eligible types (:no-op, :learn-action-class itself).
       (#(dissoc % :no-op :learn-action-class :abstain))))

;; ---------------------------------------------------------------------------
;; Stream B substrate derivation — follow-through counts per class
;; ---------------------------------------------------------------------------

(defn- since-flag
  "Build the --since=YYYY-MM-DD flag for a given window-days lookback."
  [window-days]
  (let [end (LocalDate/now ZoneOffset/UTC)
        start (.minusDays end (long window-days))]
    (str "--since=" (.format start DateTimeFormatter/ISO_LOCAL_DATE))))

(defn- run-git
  "Run git in DIR with ARGS; return stdout (trimmed) or nil on error."
  [dir & args]
  (let [{:keys [exit out]}
        (apply shell/sh
               (concat ["git" "-C" dir] args))]
    (when (zero? exit)
      (str/trim out))))

(defn- count-sorry-closures-in-window
  "Stream B follow-through for :address-sorry.

   For each commit in window that touched data/sorrys.edn, count the lines
   the commit INTRODUCED (prefix +) that match `:status :addressed` or
   `:status :foreclosed`. The sum across commits is the number of closure
   events in the window. Returns 0 if git is unavailable or the file has
   no commits in window."
  [window-days]
  (let [hashes (run-git wm-repo "log" (since-flag window-days)
                        "--pretty=format:%H" "--" "resources/sorrys.edn")]
    (if (str/blank? (or hashes ""))
      {:n 0 :evidence-refs []}
      (let [hs (str/split-lines hashes)]
        (reduce
         (fn [acc h]
           (let [diff (run-git wm-repo "show" h "--" "resources/sorrys.edn")
                 added-lines (when diff
                               (->> (str/split-lines diff)
                                    (filter #(str/starts-with? % "+"))
                                    (filter #(or (str/includes? % ":status :addressed")
                                                 (str/includes? % ":status :foreclosed")))))
                 c (count (or added-lines []))]
             (if (pos? c)
               (-> acc
                   (update :n + c)
                   (update :evidence-refs conj (str "git:futon2:" (subs h 0 (min 7 (count h))) ":sorrys.edn")))
               acc)))
         {:n 0 :evidence-refs []}
         hs)))))

(defn- count-mission-additions-in-window
  "Stream B follow-through for :open-mission.

   For each repo in FUTON_REPO_ROOTS, count git commits in window that
   ADD a new file matching holes/missions/M-*.md. Sum is the number of
   mission-open events in the window."
  [window-days]
  (reduce
   (fn [acc root]
     (let [out (run-git root "log" (since-flag window-days)
                        "--diff-filter=A" "--pretty=format:%H"
                        "--" "holes/missions/M-*.md")]
       (if (str/blank? (or out ""))
         acc
         (let [hs (str/split-lines out)
               c (count hs)
               short-name (last (str/split root #"/"))]
           (-> acc
               (update :n + c)
               (update :evidence-refs
                       (fn [refs]
                         (reduce (fn [rs h]
                                   (conj rs (str "git:" short-name ":"
                                                 (subs h 0 (min 7 (count h)))
                                                 ":holes/missions/")))
                                 refs hs))))))))
   {:n 0 :evidence-refs []}
   repo-roots))

;; Substrate dispatch — maps each WM action-class to either a follow-through
;; counter fn (returns {:n N :evidence-refs [...]}) or :substrate-unavailable.
(def substrate-dispatch
  {:address-sorry  count-sorry-closures-in-window
   :open-mission   count-mission-additions-in-window
   :fire-pattern   :substrate-unavailable})

(defn- followthrough-for-class
  [class window-days]
  (let [f (get substrate-dispatch class :substrate-unavailable)]
    (if (= :substrate-unavailable f)
      {:substrate-status :unavailable :n 0 :evidence-refs []}
      (assoc (f window-days) :substrate-status :available))))

;; ---------------------------------------------------------------------------
;; One run
;; ---------------------------------------------------------------------------

(defn- run-id
  [as-of]
  (str "wm-ol-run:" as-of))

(defn run-once!
  "Execute one outer-loop tick. Returns a map `{:records [...] :pre-run-state {...}}`
   where :records is one per class processed and :pre-run-state is the atom
   snapshot at the start of the run (after rehydration, before updates).
   Side effects: writes hyperedges to XTDB; updates atom.

   OPTS:
     :window-days  — DEPRECATED in favour of `default-window-days-by-class`
                     and `window-for-class`. When passed, overrides the
                     per-class window for ALL classes (uniform window
                     mode, the pre-§11.c behaviour). When nil, per-class
                     windows from `default-window-days-by-class` apply.
     :store?       — when false, don't persist to XTDB (useful for dry-run)
     :rehydrate?   — when true, pull current atom state from XTDB before
                     computing updates (default true)"
  [& {:keys [window-days store? rehydrate?]
      :or {store? true
           rehydrate? true}}]
  (when rehydrate?
    (iv/rehydrate-from-store!))
  (let [pre-run-state (iv/current)
        as-of   (str (Instant/now))
        run     (run-id as-of)
        ;; Read enough trace to cover the widest per-class window in
        ;; one shot; per-class filtering trims to class-specific window.
        read-window (or window-days (max-window-days))
        all-records (read-window-records read-window)
        ;; Always emit a record for the three known classes, even when
        ;; emissions are 0 — gives the operator a per-tick legibility row.
        classes [:address-sorry :open-mission :fire-pattern]]
    {:pre-run-state pre-run-state
     :records
     (doall
      (for [class classes
            :let [class-window (or window-days (window-for-class class))
                  records-in-class-window (records-within-window all-records class-window)
                  emissions (extract-emissions records-in-class-window)
                  n-em (get emissions class 0)
                  ft  (followthrough-for-class class class-window)
                  ;; Sliding-window semantics: each run computes the
                  ;; posterior from Beta(1,1) prior + the full window of
                  ;; evidence. This is what makes design-choice §6
                  ;; "latest-record-wins" correct — every record IS the
                  ;; full posterior given evidence up to its :as-of, with
                  ;; no carry-forward double-counting across runs.
                  prior-entry (iv/fresh-entry)
                  rec (if (= :unavailable (:substrate-status ft))
                        ;; No substrate: emit a no-update record that leaves
                        ;; Beta params unchanged (n-em counted as 0
                        ;; follow-through-applicable; no inference).
                        (-> (iv/next-record class prior-entry 0 0
                                            {:as-of as-of
                                             :outer-loop-run-id run
                                             :window-days class-window
                                             :evidence-refs []})
                            (assoc :n-emissions-observed n-em
                                   :substrate-status :unavailable))
                        (-> (iv/next-record class prior-entry n-em (:n ft)
                                            {:as-of as-of
                                             :outer-loop-run-id run
                                             :window-days class-window
                                             :evidence-refs (:evidence-refs ft)})
                            (assoc :substrate-status :available)))]]
        (do
          (iv/apply-update! rec)
          (when store?
            (iv/persist-record! rec))
          rec)))}))

(defn- format-delta
  "Render a Δ value with sign and color-free arrow; empty when zero."
  [pre post fmt]
  (let [d (- (double post) (double pre))]
    (cond
      (zero? d) ""
      (pos? d)  (format (str " (Δ +" fmt ")") d)
      :else     (format (str " (Δ "  fmt ")") d))))

(defn- narrative-for-class
  "Multi-line per-class narrative. PRE-ENTRY is the atom entry as it was at
   the start of this run (post-rehydration, pre-update); REC is the just-
   computed update record."
  [pre-entry rec]
  (let [class       (:class rec)
        substrate   (or (:substrate-status rec) :unknown)
        applied-em  (or (:n-emissions-in-window rec) 0)
        observed-em (or (:n-emissions-observed rec) applied-em)
        ft          (or (:n-followthrough-in-window rec) 0)
        observed-ft (or (:n-followthrough-observed rec) ft)
        ft-str      (if (= ft observed-ft)
                      (str ft)
                      (format "%d (capped from %d observed)" (int ft) (int observed-ft)))
        ignored     (max 0 (- applied-em ft))
        alpha-post  (double (:alpha-post rec))
        beta-post   (double (:beta-post rec))
        iv-post     (double (:intrinsic-value-post rec))
        iv-prev     (double (or (:intrinsic-value pre-entry) 0.5))
        delta-iv    (format-delta iv-prev iv-post "%.3f")
        evidence    (count (or (:evidence-refs rec) []))
        ;; Compose the per-class story
        header      (format "  %-16s  substrate=%s" (str class) (name substrate))
        window-line (cond
                      (= :unavailable substrate)
                      (format "    window: %d emissions observed; 0 applied (no substrate to derive follow-through)"
                              (int observed-em))
                      (zero? applied-em)
                      "    window: no :learn-action-class emissions (class is not a gap-class under current substrate)"
                      :else
                      (format "    window: %d emissions, %s followthrough, %d ignored (%.1f%% followthrough rate; %d evidence ref%s)"
                              (int applied-em) ft-str (int ignored)
                              (if (pos? applied-em)
                                (* 100.0 (/ (double ft) (double applied-em)))
                                0.0)
                              evidence
                              (if (= 1 evidence) "" "s")))
        posterior-line
        (format "    posterior: Beta(%.1f, %.1f); intrinsic-value %.3f%s"
                alpha-post beta-post iv-post delta-iv)
        interp-line
        (cond
          (= :unavailable substrate)
          "    interp: held at prior (substrate refinement is a future R12 direction)"
          (and (= alpha-post 1.0) (= beta-post 1.0))
          "    interp: held at Beta(1,1) prior — no evidence in window"
          (> iv-post 0.5)
          "    interp: posterior pulled toward α-side (operator follows recommendations)"
          (< iv-post 0.5)
          "    interp: posterior pulled toward β-side (operator rarely follows; class is overactive)"
          :else
          "    interp: posterior at prior mode")]
    (str/join "\n" [header window-line posterior-line interp-line])))

(defn- summarise
  "Narrative digest of one run. PRE-RUN-STATE is the atom snapshot at the
   start of this run; RECORDS are the per-class updates just computed."
  [pre-run-state records]
  (str/join "\n\n"
            (for [r records]
              (let [pre (or (get pre-run-state (:class r)) (iv/fresh-entry))]
                (narrative-for-class pre r)))))

(defn -main
  "Entrypoint. Optional first arg: uniform window-days override (suppresses
   per-class windows from `default-window-days-by-class`). When omitted,
   each class uses its own window."
  [& args]
  (try
    (let [uniform-window (when (seq args) (Integer/parseInt (first args)))
          {:keys [records pre-run-state]}
          (if uniform-window
            (run-once! :window-days uniform-window)
            (run-once!))
          window-summary (if uniform-window
                           (str "uniform window=" uniform-window " days")
                           (str "per-class windows="
                                (pr-str default-window-days-by-class)))]
      (println (str (Instant/now)) "wm-outer-loop" window-summary "\n")
      (println (summarise pre-run-state records))
      (System/exit 0))
    (catch Throwable t
      (binding [*out* *err*]
        (println (str (Instant/now)) "ERROR" (.getMessage t)))
      (System/exit 1))))
