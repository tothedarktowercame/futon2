#!/usr/bin/env bb
;; harvest_refusals.bb -- the tail-eater v0 (worklist row AC8).
;;
;; Joe's 2026-09-02 C130 ruling decided the seven typed-absence migrations
;; (AC1-AC7) ON ONE CONDITION: "refusals and :unscored etc are fed back in as
;; issues needing to be addressed, so the machine can become self repairing."
;; AC1-AC7 made the records exist and be typed. This script is what turns them
;; into work.
;;
;; WHAT IT DOES, in one sentence per stage:
;;   1. sweeps the typed non-present records out of the WM trace and out of the
;;      C130 absence-coercion lint, counting only what is NEW since the last
;;      sweep (an append-only line watermark per trace file, a finding-key set
;;      for the lint);
;;   2. aggregates them by (site, reason) -- site is the record's own
;;      :producer-contract, so the aggregation key is the producer's published
;;      identity and not a string this script invents;
;;   3. rewrites PROPOSED-ROWS.md with one proposal per class.
;;   4. mints a DRAFT flexiarg in futon3/library/problems/ for every class whose
;;      CUMULATIVE count has reached the recurrence threshold.
;;
;; WHAT IT DOES NOT DO. It does not write worklist.edn. One row, one writer:
;; a proposal becomes a ledger row when a person reads it and files it. It does
;; not decide anything, and the draft patterns it mints claim no authority (see
;; the quarantine note on the generated flexiarg).
;;
;; AN EMPTY SWEEP IS A RESULT AND IS WRITTEN AS ONE. The failure mode this
;; script exists to prevent is a silent zero: a harvester that writes nothing
;; when it finds nothing is indistinguishable from a harvester that did not run,
;; and that is precisely the standing-red the ruling is against. So an empty
;; sweep rewrites the file with an explicit EMPTY SWEEP section that names every
;; source it read and every declared event key it did NOT find, and prints a
;; banner on stderr.
;;
;; CONTROLS RUN ON EVERY INVOCATION (--no-controls to skip). A planted refusal
;; record must reach the proposals and a swept-clean corpus must produce the
;; loud empty sweep; both are checked in a temp directory before the real sweep
;; touches anything. A control failure exits 2 and writes nothing.
;;
;; Efficiency beyond v0 is explicitly future work per the ruling.

(ns harvest-refusals
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.string :as str]))

(def repo (or (System/getenv "FUTON2") "/home/joe/code/futon2"))
(def contract-dir (str repo "/holes/labs/wm-contract"))

(defn env [k d] (or (not-empty (str (or (System/getenv k) ""))) d))

(def trace-dir      (env "WM_HARVEST_TRACE_DIR" (str repo "/data/wm-trace")))
(def state-path     (env "WM_HARVEST_STATE"     (str contract-dir "/refusal-sweep-state.edn")))
(def proposals-path (env "WM_HARVEST_PROPOSALS" (str contract-dir "/PROPOSED-ROWS.md")))
(def drafts-dir     (env "WM_HARVEST_DRAFTS_DIR" "/home/joe/code/futon3/library/problems"))
(def lint-edn       (env "WM_HARVEST_LINT_EDN"  nil))   ; pre-captured lint output, else run the lint
(def threshold      (parse-long (env "WM_HARVEST_THRESHOLD" "3")))

;; ---------------------------------------------------------------------------
;; What counts as a record, and what is declared to exist.
;;
;; COLLECTION IS BY SHAPE, NOT BY KEY. Every typed record AC1-AC7 emits stamps
;; itself with a `:producer-contract` and a `:status`, so the sweep walks the
;; whole trace record and collects every map carrying both. That was not the
;; first design and the correction is worth keeping: a fixed list of top-level
;; trace keys MISSED AC5 and AC6 entirely, because their records do not travel
;; under `:move-score-events` in the trace -- the cascade lane folds them into
;; `:policy-rollout-events` on an act-gate entry
;; (scripts/futon2/report/cascade_lane.clj:384-386,399-409), which reaches the
;; record through `:act-gate-verdicts` and `:enactment`
;; (src/futon2/aif/close_loop.clj:131-132, src/futon2/aif/enact.clj:216-217,317-318,
;; persisted at src/futon2/aif/trace.clj:588-594). Collecting by shape follows
;; the record wherever a caller decides to put it; collecting by key follows
;; only where somebody remembered to look.
;;
;; The DECLARED table below is therefore a coverage report, not the collector.
;; It exists so the sweep can say "not found" about a producer it looked for,
;; which is a different claim from not having looked.
;; ---------------------------------------------------------------------------

(def declared-producers
  [{:contract :prediction-error/v1        :row :AC1
    :reaches-trace "src/futon2/aif/trace.clj:530-538 (:prediction-triple-events)"}
   {:contract :r3d-aggregate-driver/v1    :row :AC2
    :reaches-trace "src/futon2/aif/trace.clj:539-546 (:belief-aggregation-events)"}
   {:contract :strategic-mode/v1          :row :AC3
    :reaches-trace "src/futon2/aif/trace.clj:547-554 (:strategic-mode-events)"}
   {:contract :default-mode-pressure/v1   :row :AC4
    :reaches-trace "src/futon2/aif/trace.clj:555-564 (:default-mode-events)"}
   {:contract :rollout-move-score/v1      :row :AC5
    :reaches-trace "cascade_lane.clj:384 -> :policy-rollout-events -> :act-gate-verdicts/:enactment"}
   {:contract :rollout-move-cost/v1       :row :AC6
    :reaches-trace "cascade_lane.clj:385 -> :policy-rollout-events -> :act-gate-verdicts/:enactment"}
   {:contract :rollout-refusal/v1         :row :AC6
    :reaches-trace "cascade_lane.clj:386 -> :policy-rollout-events -> :act-gate-verdicts/:enactment"}
   {:contract :fulab-temperature/v1       :row :AC7
    :reaches-trace nil}])

;; The prefilter. A trace line is ~1 MB of EDN and parsing all of them costs
;; more than the rest of the sweep, so only lines whose raw text mentions the
;; stamp are read. Substring containment can only over-select.
(def prefilter-marker "producer-contract")

;; A record's :status decides whether it is work. The OK set is closed and
;; small; EVERYTHING ELSE is a proposal, including a status this script has
;; never seen -- an unrecognised status must not be silently dropped, which is
;; the same defect class (silent absence) the AC rows just removed.
(def ok-statuses #{:present :scored :contributing})
(def known-attention-statuses
  #{:absent :refused :unknown :unscored :omitted :rejected :uncosted})

(defn attention? [record]
  (and (map? record) (not (contains? ok-statuses (:status record)))))

;; ---------------------------------------------------------------------------
;; Sweep: trace
;; ---------------------------------------------------------------------------

(defn line-may-carry-events? [line] (str/includes? line prefilter-marker))

(defn read-record [line]
  (try (edn/read-string {:default (fn [_tag v] v)} line)
       (catch Exception e {::unreadable (.getMessage e)})))

(defn typed-records
  "Every map under X that carries the producer stamp and a status -- at any
  depth, because a caller may fold a record into a structure of its own. A
  member verdict inside :offending carries a :status but NO :producer-contract
  and is correctly not collected: it is part of a record, not one."
  [x]
  (let [out (volatile! [])]
    ((fn walk [node]
       (cond
         (map? node)
         (do (when (and (contains? node :producer-contract) (contains? node :status))
               (vswap! out conj node))
             (run! walk (vals node)))
         (sequential? node) (run! walk node)
         (set? node) (run! walk node)
         :else nil))
     x)
    @out))

(defn record->finding
  [{:keys [file lineno timestamp run-id event-key]} record]
  (let [site (or (:producer-contract record) (keyword (name event-key) "no-producer-contract"))
        reason (or (:reason record) :unstated-reason)]
    {:source :trace
     :file file :line lineno
     :timestamp timestamp :run-id run-id
     :event-key event-key
     :site site
     :reason reason
     :status (:status record)
     :status-unclassified (not (contains? known-attention-statuses (:status record)))
     :reason-unstated (nil? (:reason record))
     :detail (not-empty (select-keys record [:channel :absent-member :offending :absent
                                             :feature :move :excluded-from-rollout?]))}))

(defn sweep-trace-file
  "Findings in FILE strictly after the SKIP-LINES already swept, plus the new
  watermark. Trace files are append-only per date, so a line count is an exact
  watermark; a file that got SHORTER was rewritten and the whole file is re-read
  with :reset? true so the report can say so."
  [file skip-lines]
  (let [reset? (atom false)
        findings (atom [])
        n (atom 0)]
    (with-open [r (io/reader file)]
      (doseq [[i line] (map-indexed vector (line-seq r))]
        (reset! n (inc i))
        (when (and (> (inc i) skip-lines) (line-may-carry-events? line))
          (let [record (read-record line)
                ts (:timestamp record)
                run-id (get record :run/id)]
            (if (::unreadable record)
              ;; A line that mentions a declared event key and then will not
              ;; parse is the same defect this whole apparatus is against: it
              ;; would drop refusal records silently. It is a finding, not a
              ;; skip.
              (swap! findings conj
                     {:source :trace :file file :line (inc i)
                      :timestamp nil :run-id nil
                      :event-key :unreadable-trace-line
                      :site :trace-reader/v1
                      :reason :unreadable-trace-line
                      :status :refused
                      :status-unclassified false :reason-unstated false
                      :detail {:reader-error (::unreadable record)}})
              ;; The top-level key is kept as :event-key so a reader can see
              ;; WHERE in the record a refusal travelled -- that is how the
              ;; AC5/AC6 path through :act-gate-verdicts became visible.
              (doseq [[k v] (dissoc record :timestamp :run/id)
                      ev (typed-records v)
                      :when (attention? ev)]
                (swap! findings conj
                       (record->finding {:file file :lineno (inc i) :timestamp ts
                                         :run-id run-id :event-key k}
                                        ev))))))))
    (when (< @n skip-lines) (reset! reset? true))
    {:findings (if @reset?
                 ;; shrunk: everything we just skipped is gone, re-read from 0
                 (:findings (sweep-trace-file file 0))
                 @findings)
     :lines @n
     :reset? @reset?}))

(defn sweep-trace [state]
  (let [files (if (fs/directory? trace-dir)
                (sort (map str (fs/glob trace-dir "wm-trace-*.edn")))
                [])
        per-file (for [f files
                       :let [rel (str (fs/file-name f))
                             skip (get-in state [:trace rel :lines] 0)
                             {:keys [findings lines reset?]} (sweep-trace-file f skip)]]
                   {:file f :rel rel :skipped skip :lines lines
                    :reset? reset? :findings findings})]
    {:files-read (count files)
     :per-file (vec per-file)
     :findings (vec (mapcat :findings per-file))
     :watermark (into {} (for [{:keys [rel lines]} per-file] [rel {:lines lines}]))}))

;; ---------------------------------------------------------------------------
;; Sweep: the C130 absence-coercion lint
;;
;; checks/preemptive_absence_coercion_lint.clj prints a machine-readable map as
;; its first stdout line. It has no clock, so "since the last sweep" is set
;; difference against the previous sweep's finding keys, kept in the state file.
;; ---------------------------------------------------------------------------

(def lint-script (str repo "/checks/preemptive_absence_coercion_lint.clj"))

(defn read-lint []
  (try
    (let [raw (if lint-edn
                (slurp lint-edn)
                (let [{:keys [out]} (p/sh {:dir repo} "bb" lint-script)] out))
          line (first (remove str/blank? (str/split-lines (str raw))))]
      (if line
        {:ok? true :report (edn/read-string {:default (fn [_t v] v)} line)
         :source (or lint-edn lint-script)}
        {:ok? false :error "lint produced no output" :source (or lint-edn lint-script)}))
    (catch Exception e
      {:ok? false :error (.getMessage e) :source (or lint-edn lint-script)})))

(defn lint-finding->finding [f]
  {:source :lint
   :file (or (:file f) (:at f) "unknown")
   :line (:line f)
   :timestamp nil :run-id nil
   :event-key :absence-coercion-lint
   :site (or (:site f) :absence-coercion-lint/v1)
   :reason (or (:reason f) :absence-coercion-finding)
   :status :refused
   :status-unclassified false
   :reason-unstated (nil? (:reason f))
   :detail (not-empty (dissoc f :file :line :reason :site))})

(defn finding-key [f]
  (str (:file f) ":" (:line f) "|" (:site f) "|" (:reason f)))

(defn sweep-lint [state]
  (let [{:keys [ok? report error source]} (read-lint)
        all (when ok? (mapv lint-finding->finding (:findings report)))
        seen (set (get-in state [:lint :finding-keys] []))
        fresh (vec (remove #(contains? seen (finding-key %)) (or all [])))]
    {:ok? ok? :error error :source source
     :total (count all)
     :findings fresh
     :finding-keys (vec (sort (map finding-key (or all []))))}))

;; ---------------------------------------------------------------------------
;; Aggregate by (site, reason)
;; ---------------------------------------------------------------------------

(defn slug [x]
  (-> (str x)
      (str/replace #"^:" "")
      (str/lower-case)
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-+|-+$" "")))

(defn class-id [{:keys [site reason]}] (str (slug site) "--" (slug reason)))

(defn aggregate
  "Fold NEW findings into the previous per-class state. Cumulative counts are
  what the recurrence threshold reads, so they must survive a sweep that finds
  nothing; the previous state is carried forward untouched for classes with no
  new findings."
  [prev-classes findings now]
  (reduce
   (fn [acc f]
     (let [id (class-id f)
           prev (get acc id)
           ex {:file (:file f) :line (:line f) :timestamp (:timestamp f)
               :run-id (:run-id f) :status (:status f) :detail (:detail f)
               :source (:source f)}]
       (assoc acc id
              {:id id
               :site (:site f) :reason (:reason f)
               :sources (vec (sort (distinct (conj (:sources prev []) (:source f)))))
               :statuses (vec (sort (distinct (conj (:statuses prev []) (str (:status f))))))
               :count (inc (:count prev 0))
               :new-this-sweep (inc (:new-this-sweep prev 0))
               :first-seen (or (:first-seen prev) (:timestamp f) now)
               :last-seen (or (:timestamp f) now)
               :status-unclassified (boolean (or (:status-unclassified prev)
                                                 (:status-unclassified f)))
               :reason-unstated (boolean (or (:reason-unstated prev) (:reason-unstated f)))
               ;; Evidence is capped so a class that fires ten thousand times
               ;; does not make the proposals unreadable; :count is the honest
               ;; total and the cap is stated in the report.
               :evidence (vec (take 12 (distinct (conj (vec (:evidence prev [])) ex))))})))
   (into {} (for [[k v] prev-classes] [k (assoc v :new-this-sweep 0)]))
   findings))

;; ---------------------------------------------------------------------------
;; PROPOSED-ROWS.md
;; ---------------------------------------------------------------------------

(defn ev-line [e]
  (str "    - `" (:file e) (when (:line e) (str ":" (:line e))) "`"
       (when (:timestamp e) (str " @ " (:timestamp e)))
       (when (:run-id e) (str " run " (:run-id e)))
       (when (:detail e) (str " " (pr-str (:detail e))))))

(defn proposal-block [c drafted]
  (let [recurring? (>= (:count c) threshold)]
    (str "### " (:id c) "\n\n"
         "- **site** `" (:site c) "` &nbsp; **reason** `" (:reason c) "`\n"
         "- statuses seen: " (str/join ", " (map #(str "`" % "`") (:statuses c))) "\n"
         "- sources: " (str/join ", " (map #(str "`" % "`") (:sources c))) "\n"
         "- cumulative occurrences: **" (:count c) "**"
         " (new this sweep: " (:new-this-sweep c) ")\n"
         "- first seen " (:first-seen c) ", last seen " (:last-seen c) "\n"
         (when (:status-unclassified c)
           (str "- **UNCLASSIFIED STATUS**: at least one record carried a `:status` that is"
                " neither in the OK set " (pr-str ok-statuses) " nor in the known attention set "
                (pr-str known-attention-statuses) ". Read the record before filing.\n"))
         (when (:reason-unstated c)
           "- **REASON UNSTATED**: at least one record carried no `:reason`. The producer owes one.\n")
         "- recurring at threshold " threshold "? **" (if recurring? "YES" "no") "**"
         (if recurring?
           (str " -> draft pattern `" (get drafted (:id c) "(not written)") "`\n")
           "\n")
         "- evidence (first " (count (:evidence c)) " of " (:count c) "):\n"
         (str/join "\n" (map ev-line (:evidence c))) "\n\n"
         "- proposed ledger row (COPY BY HAND after reading; this file writes no ledger):\n\n"
         "```clojure\n"
         "  {:id :TBD :class :I :status :open :owner :any\n"
         "   :covers-key :none\n"
         "   :statement \"Harvested refusal class " (:id c) ": `" (:site c)
         "` emitted `" (:reason c) "` " (:count c) " time(s). "
         "Decide whether the producer's refusal is correct and the CALLER must change, "
         "or the refusal is the defect.\"\n"
         "   :acceptance \"State which of the two it is with a file:line pointer, and either "
         "repair the caller or repair the producer. Gates: clj-kondo, check-parens, tests.\"}\n"
         "```\n")))

(defn not-found-table [trace-sweep]
  (let [by-site (group-by :site (:findings trace-sweep))]
    (str "| declared producer | row | how its records reach the trace | this sweep |\n"
         "|---|---|---|---|\n"
         (str/join "\n"
                   (for [{:keys [contract row reaches-trace]} declared-producers
                         :let [hits (get by-site contract)]]
                     (str "| `" contract "` | " row " | "
                          (if reaches-trace (str "`" reaches-trace "`")
                              "**no path to the trace** -- the producer has no caller (C226/AC7)")
                          " | "
                          (if (seq hits)
                            (str (count hits) " under "
                                 (str/join ", " (map #(str "`" % "`")
                                                     (sort (distinct (map :event-key hits))))))
                            "**not found**")
                          " |")))
         "\n\nCollection is by record SHAPE (`:producer-contract` + `:status`), so a\n"
         "producer not listed above is still swept; the table is coverage, not scope.\n")))

(defn render [{:keys [now sweep-n trace-sweep lint-sweep classes drafted]}]
  (let [ordered (->> (vals classes)
                     (sort-by (juxt (comp - :count) :id)))
        active (remove #(zero? (:count %)) ordered)
        new-count (reduce + 0 (map :new-this-sweep ordered))]
    (str
     "# PROPOSED-ROWS -- harvested refusal, abstention, :unknown and :unscored records\n\n"
     "GENERATED by `holes/labs/wm-contract/harvest_refusals.bb` (worklist row AC8).\n"
     "**Do not edit by hand** -- the next sweep overwrites this file.\n\n"
     "**These are PROPOSALS FOR REVIEW, not ledger rows.** One row, one writer:\n"
     "nothing here reaches `worklist.edn` except by a person who read it and filed it.\n"
     "No ruling is recorded here; a ruling goes to `aif-equations.edn :choices` or\n"
     "`control-map-edges.edn :decisions`, and only Joe writes those.\n\n"
     "Why this file exists: Joe's 2026-09-02 C130 ruling decided AC1-AC7 on the\n"
     "condition that refusals be \"fed back in as issues needing to be addressed, so\n"
     "the machine can become self repairing\" (`holes/problems/DECISIONS-PENDING.md:343-364`).\n\n"
     "---\n\n"
     "## Sweep " sweep-n " -- " now "\n\n"
     "- trace: `" trace-dir "`, " (:files-read trace-sweep) " file(s) read, "
     (count (:findings trace-sweep)) " new record(s) past the watermark\n"
     "- lint: `" (:source lint-sweep) "` -- "
     (if (:ok? lint-sweep)
       (str (:total lint-sweep) " finding(s), " (count (:findings lint-sweep)) " new since last sweep")
       (str "**COULD NOT BE READ**: " (:error lint-sweep)))
     "\n"
     "- recurrence threshold: **" threshold "** cumulative occurrences of one (site, reason)\n"
     "- classes: " (count active) " with occurrences, " new-count " new record(s) this sweep\n\n"
     (when (some :reset? (:per-file trace-sweep))
       (str "**A TRACE FILE SHRANK** and was re-read from line 0 -- the watermark was\n"
            "for a longer file, so it was not written by an append. Files: "
            (str/join ", " (map :rel (filter :reset? (:per-file trace-sweep)))) "\n\n"))
     "### Declared event keys and what this sweep found\n\n"
     (not-found-table trace-sweep) "\n"
     (if (seq active)
       (str "## Proposals (" (count active) " class(es))\n\n"
            (str/join "\n" (map #(proposal-block % drafted) active)))
       (str "## EMPTY SWEEP -- nothing found, and that is the finding\n\n"
            "This sweep found **zero** refusal, abstention, `:unknown` or `:unscored`\n"
            "records. An empty result is written out in full rather than left as an\n"
            "absent file, because a harvester that writes nothing when it finds nothing\n"
            "cannot be told apart from one that did not run.\n\n"
            "What was read, so the zero is a measurement and not a silence:\n\n"
            "- " (:files-read trace-sweep) " trace file(s) under `" trace-dir "`, "
            (reduce + 0 (map :lines (:per-file trace-sweep))) " line(s) total, "
            (reduce + 0 (map :skipped (:per-file trace-sweep))) " already swept before this run\n"
            "- the C130 absence-coercion lint at `" (:source lint-sweep) "`: "
            (if (:ok? lint-sweep) (str (:total lint-sweep) " finding(s)")
                (str "COULD NOT BE READ -- " (:error lint-sweep)))
            "\n- every map in those records carrying a `:producer-contract` and a\n"
            "  `:status`, at any depth -- see the coverage table above for the "
            (count declared-producers) " declared producers\n\n"
            "The standing caveat on this zero: "
            (count (remove :reaches-trace declared-producers))
            " declared producer(s) have no path to the trace at\n"
            "all (`:fulab-temperature/v1` -- the adapter has no caller, C226), so their\n"
            "refusals are invisible here by construction, not by absence.\n"))
     "\n---\n\n"
     "State (watermarks, cumulative counts): `" (fs/file-name state-path) "`.\n")))

;; ---------------------------------------------------------------------------
;; Draft patterns (the ruling extension, Joe 2026-09-02 second exchange)
;;
;; A recurring refusal class is minted as a DRAFT design pattern: the tension
;; half recorded before its resolution exists. IF and HOWEVER come from the
;; records; THEN is EMPTY and says so, because nobody has resolved it yet.
;; One file per class, rewritten in place on every sweep -- a re-sweep updates
;; the evidence list, it never mints a sibling.
;; ---------------------------------------------------------------------------

(defn draft-path [c] (str drafts-dir "/refusal-" (:id c) ".flexiarg"))

(defn draft-text [c now]
  (str
   "@flexiarg problems/refusal-" (:id c) "\n"
   "@title Refusal: " (:site c) " / " (:reason c) "\n"
   "@style pattern\n"
   "@audience wm-organization\n"
   "@tone plain\n"
   "@draft wm-contract/harvest_refusals.bb\n"
   "\n"
   ";; QUARANTINE. This pattern was MINTED BY A SCRIPT from " (:count c) " refusal\n"
   ";; record(s), under Joe's 2026-09-02 ruling extension: a recurring refusal is\n"
   ";; recorded as a draft design pattern so the refusals become design knowledge\n"
   ";; instead of standing red. It is a DRAFT and is quarantined exactly as a\n"
   ";; @why-posthoc edge is -- counted separately by checks/library_graph_lint.clj,\n"
   ";; admitted to no authored-authority law, and outside the organised fraction.\n"
   ";; It carries no @why: a script has no rationale to claim. It earns standing the\n"
   ";; ordinary way -- a person removes @draft and writes the @why, and that edge\n"
   ";; then needs its attestation like any other.\n"
   ";; Regenerated on every sweep. Hand edits to the body will be overwritten;\n"
   ";; promote it out of draft instead.\n"
   "\n"
   "! conclusion: A recurring refusal at " (:site c) " is unresolved design work, not a\n"
   "  standing red. Someone must decide whether the caller or the producer is wrong.\n"
   "\n"
   "  + context: " (:count c) " record(s) of `" (:reason c) "` from `" (:site c) "`,\n"
   "    first seen " (:first-seen c) ", last seen " (:last-seen c) ". Harvested\n"
   "    " now " by holes/labs/wm-contract/harvest_refusals.bb from the WM trace and\n"
   "    the C130 absence-coercion lint. Statuses: "
   (str/join ", " (:statuses c)) ".\n"
   "    + salience: the producer refuses REPEATEDLY and the refusal has no consumer.\n"
   "      That is the refuse-and-stop failure mode the ruling of 2026-09-02 names,\n"
   "      seen from the producer's side.\n"
   "    + whose problem: the caller's, until someone shows the producer is wrong to\n"
   "      refuse. The producer stated a reason; nothing downstream acts on it.\n"
   "\n"
   "  + IF:\n"
   "    " (:site c) " is asked for a value it can only supply from an input that was\n"
   "    not observed or was malformed.\n"
   "\n"
   "  + HOWEVER:\n"
   "    It refuses with `" (:reason c) "`, and has done so " (:count c) " time(s); no\n"
   "    caller repairs the input and no reader turns the refusal into work.\n"
   "\n"
   "  + THEN:\n"
   "    EMPTY. No resolution has been written. This half of the pattern is the\n"
   "    tension recorded before its resolution exists, which is what the ruling\n"
   "    extension asked for; it is NOT a claim that a resolution is known.\n"
   "\n"
   "  + BECAUSE:\n"
   "    A refusal that recurs is a statement about the design, not about the tick.\n"
   "    ?evidence(required): each line below is one harvested record.\n"
   (str/join "\n"
             (for [e (:evidence c)]
               (str "    ?evidence: " (:file e) (when (:line e) (str ":" (:line e)))
                    (when (:timestamp e) (str " @ " (:timestamp e)))
                    " status " (:status e)
                    (when (:detail e) (str " " (pr-str (:detail e)))))))
   "\n"))

(defn mint-drafts! [classes now]
  (let [recurring (filter #(>= (:count %) threshold) (vals classes))]
    (when (seq recurring) (fs/create-dirs drafts-dir))
    (into {} (for [c recurring
                   :let [path (draft-path c)]]
               (do (spit path (draft-text c now))
                   [(:id c) (str (fs/relativize (fs/parent (fs/parent drafts-dir)) path))])))))

;; ---------------------------------------------------------------------------
;; Sweep
;; ---------------------------------------------------------------------------

(defn read-state []
  (if (fs/exists? state-path)
    (edn/read-string {:default (fn [_t v] v)} (slurp state-path))
    {:schema 1 :sweeps 0 :trace {} :lint {} :classes {}}))

(defn sweep! [{:keys [write-drafts?] :or {write-drafts? true}}]
  (let [now (str (java.time.Instant/now))
        state (read-state)
        sweep-n (inc (:sweeps state 0))
        trace-sweep (sweep-trace state)
        lint-sweep (sweep-lint state)
        findings (into (:findings trace-sweep) (:findings lint-sweep))
        classes (aggregate (:classes state) findings now)
        drafted (if write-drafts? (mint-drafts! classes now) {})
        md (render {:now now :sweep-n sweep-n :trace-sweep trace-sweep
                    :lint-sweep lint-sweep :classes classes :drafted drafted})
        next-state {:schema 1
                    :sweeps sweep-n
                    :last-sweep-at now
                    :threshold threshold
                    :trace (:watermark trace-sweep)
                    :lint {:ok? (:ok? lint-sweep)
                           :finding-keys (:finding-keys lint-sweep)}
                    :classes classes}]
    (fs/create-dirs (fs/parent proposals-path))
    ;; A sweep whose CONTENT is unchanged leaves the tracked proposals file
    ;; alone: rewriting only its sweep-number/timestamp header dirties the
    ;; tree every publish and trips the build loop's mid-edit pre-flight
    ;; (2026-09-02, wm loop iterations 1-60 — twice: first on empty sweeps,
    ;; then again once a class stood at threshold, because mint-drafts!
    ;; returns the standing class every sweep so a findings/drafted test
    ;; stays truthy forever). Compare bodies with the volatile header
    ;; normalised; the state file below still advances the watermark.
    (let [strip-header #(str/replace % #"(?m)^## Sweep \d+ -- \S+$" "## Sweep")
          wrote? (or (not (fs/exists? proposals-path))
                     (not= (strip-header md)
                           (strip-header (slurp (str proposals-path)))))]
      (when wrote? (spit proposals-path md))
      (fs/create-dirs (fs/parent state-path))
      (spit state-path (str (with-out-str (clojure.pprint/pprint next-state))))
      {:new-findings (count findings)
       :classes (count (remove #(zero? (:count %)) (vals classes)))
       :drafts (count drafted)
       :lint-ok? (:ok? lint-sweep)
       :wrote-proposals? (boolean wrote?)
       :empty? (zero? (count (remove #(zero? (:count %)) (vals classes))))})))

;; ---------------------------------------------------------------------------
;; Controls. Both run before the real sweep, in a temp directory, on every
;; invocation. The planted refusal is the acceptance test named in AC8.
;; ---------------------------------------------------------------------------

(def planted-record
  {:timestamp "2999-01-01T00:00:00.000000000Z"
   :run/id "planted-control-run"
   :prediction-triple-events
   [{:producer-contract :prediction-error/v1
     :channel :control-channel
     :status :refused
     :reason :malformed-prediction-triple
     :offending [{:member :mean :status :missing}]}]})

(defn control! [label {:keys [lines expect-empty? expect-class]}]
  (let [tmp (str (fs/create-temp-dir {:prefix "harvest-refusals-control-"}))
        td (str tmp "/trace")
        _ (fs/create-dirs td)
        _ (spit (str td "/wm-trace-2999-01-01.edn")
                (str/join "\n" (map pr-str lines)))
        _ (spit (str tmp "/lint.edn") (pr-str {:kind :absence :findings [] :counts {}}))
        proposals (str tmp "/PROPOSED-ROWS.md")
        state (str tmp "/state.edn")
        out (with-redefs [trace-dir td
                          lint-edn (str tmp "/lint.edn")
                          state-path state
                          proposals-path proposals
                          drafts-dir (str tmp "/drafts")]
              (sweep! {:write-drafts? false}))
        text (slurp proposals)
        problems
        (cond-> []
          (and expect-empty? (not (:empty? out)))
          (conj "expected an empty sweep, got proposals")
          (and expect-empty? (not (str/includes? text "EMPTY SWEEP")))
          (conj "empty sweep did not announce itself in PROPOSED-ROWS.md")
          (and expect-class (not (str/includes? text expect-class)))
          (conj (str "planted record did not reach the proposals as class " expect-class))
          (and expect-class (:empty? out))
          (conj "planted record produced an empty sweep"))]
    (fs/delete-tree tmp)
    (when (seq problems)
      (binding [*out* *err*]
        (println (str "harvest_refusals: CONTROL FAILED (" label "): " (str/join "; " problems))))
      (System/exit 2))
    (println (str "harvest_refusals: control " label " OK"))))

;; The regression guard for the collector's own history. The first version of
;; this script swept a fixed list of top-level trace keys and MISSED this shape
;; entirely: AC5's and AC6's records reach the trace nested two levels down,
;; inside :policy-rollout-events on an act-gate entry. A collector that walks by
;; record shape finds it; one that walks by key does not, and would report the
;; same confident zero.
(def planted-nested-record
  {:timestamp "2999-01-01T00:00:01.000000000Z"
   :run/id "planted-control-run"
   :act-gate-verdicts
   [{:target "M-control"
     :policy-rollout-events
     [{:producer-contract :rollout-move-cost/v1
       :move/id "u"
       :status :uncosted
       :reason :cost-not-supplied}]}]})

(defn run-controls! []
  (control! "planted-refusal-reaches-proposals"
            {:lines [planted-record]
             :expect-class "prediction-error-v1--malformed-prediction-triple"})
  (control! "planted-refusal-nested-two-levels-reaches-proposals"
            {:lines [planted-nested-record]
             :expect-class "rollout-move-cost-v1--cost-not-supplied"})
  (control! "clean-corpus-says-empty-loudly"
            {:lines [{:timestamp "2999-01-01T00:00:00.000000000Z"
                      :prediction-triple-events
                      [{:producer-contract :prediction-error/v1 :status :present
                        :observed 1.0 :predicted-mean 1.0 :predicted-variance 1.0}]}]
             :expect-empty? true}))

(defn -main [& args]
  (let [args (set args)]
    (when (contains? args "--help")
      (println "usage: harvest_refusals.bb [--no-controls] [--no-drafts]")
      (println "env: WM_HARVEST_TRACE_DIR WM_HARVEST_STATE WM_HARVEST_PROPOSALS")
      (println "     WM_HARVEST_DRAFTS_DIR WM_HARVEST_LINT_EDN WM_HARVEST_THRESHOLD")
      (System/exit 0))
    (when-not (contains? args "--no-controls") (run-controls!))
    (let [{:keys [new-findings classes drafts lint-ok? wrote-proposals? empty?]}
          (sweep! {:write-drafts? (not (contains? args "--no-drafts"))})]
      (when-not lint-ok?
        (binding [*out* *err*]
          (println "harvest_refusals: WARNING -- the C130 lint could not be read; the sweep covers the trace only")))
      (when empty?
        (binding [*out* *err*]
          (println "harvest_refusals: ================ EMPTY SWEEP ================")
          (println "harvest_refusals: zero refusal/abstention/:unknown/:unscored records.")
          (println (str "harvest_refusals: "
                        (if wrote-proposals? "written in full to " "unchanged at ")
                        proposals-path
                        " -- read it before believing the zero."))))
      (println (str "harvest_refusals: " new-findings " new record(s), "
                    classes " class(es) with occurrences, "
                    drafts " draft pattern(s), threshold " threshold))
      (println (str "harvest_refusals: proposals "
                    (if wrote-proposals? "-> " "unchanged: ")
                    proposals-path))
      (System/exit 0))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
