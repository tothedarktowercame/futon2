(ns futon2.aif.enumeration-completeness
  "Does the selector's candidate enumeration cover everything there is to work on?

   The War Machine's candidate pool is built by proposers
   (`war_machine.clj:6005-6013`). Each proposer walks its own substrate and
   emits candidates. Nothing downstream can tell the difference between \"this
   proposer emitted every available item\" and \"this proposer emitted four of
   them\": a narrowed producer looks exactly like a small world. That is not
   hypothetical here -- `holes/NOTE-the-whitelist-provenance.md` records a
   grounded producer whose domain contracted to four missions inside one
   working day, with no commit message, docstring or excursion recording the
   narrowing.

   This namespace recomputes the AVAILABLE population by a path that shares no
   code with the proposers: a direct filesystem scan under a declared per-kind
   contract, its own status classifier, its own fences. It then compares that
   population with the candidates a tick actually enumerated and returns a
   typed record.

   The discipline the record enforces: every member of the available
   population that the enumerator did not emit is either
     - EXCLUDED by a fence this scan applied, and then it carries the fence's
       typed reason and never reaches the comparison at all; or
     - MISSING, which is untyped by construction and fails the check.
   Silence is not a filter. A candidate the enumerator emitted that the scan
   cannot find on disk is a PHANTOM and fails the same way.

   Independence, precisely: nothing here calls `mission-registry`,
   `pattern-registry` or any proposer, and the comparison consumes the tick's
   RECORDED candidates rather than re-running the tick."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)))

(def ^:dynamic *enumeration-assert?*
  "U37 per-tick enumeration-completeness assertion, read once when this
   namespace loads. `FUTON_WM_ENUMERATION_ASSERT=1` makes the tick recompute
   the available population by the independent scan below and attach the typed
   completeness record to its decision.

   Default OFF, and off is byte-identical: with the flag clear the tick does
   not scan, does not attach the key, and no selection path reads it. Off is
   the default because this is a new read on the tick path, not because the
   scan is expensive -- the three-kind scan measures at 127-196 ms
   (`holes/labs/wm-contract/runs/U37-enumeration-completeness/scan-cost-2026-09-03.txt`),
   against a tick that takes tens of seconds.

   Dynamic binding exists only for isolated tests."
  (= "1" (System/getenv "FUTON_WM_ENUMERATION_ASSERT")))

(def default-code-root
  (str (System/getProperty "user.home") "/code"))

;; ---------------------------------------------------------------------------
;; The per-kind scan contracts
;; ---------------------------------------------------------------------------

(def path-fences
  "Path patterns that keep non-primary checkouts out of the population, each
   with the typed reason a file excluded by it carries.

   These mirror `mission-registry/non-primary-path-patterns` in INTENT and are
   written separately on purpose: a fence copied by reference cannot disagree
   with the thing it audits, and disagreement is the finding this check exists
   to produce."
  [[#"/\.state/" :sandbox-path]
   [#"/\.worktrees/" :git-worktree]
   [#"-health-main/" :health-main-worktree]
   [#"-index-check/" :index-check-copy]])

(def kinds
  "The kinds this check knows how to count, and for each one:

     :file-pattern     the scan contract -- which files ARE the population
     :contract-source  :code (the pattern is what the enumerator's own code
                       matches, with the pointer) or :declared-by-this-check
                       (no enumerator exists, so no code defines a contract and
                       this check states one; it is declared, not derived)
     :candidate-types  the action types a tick emits for this kind
     :enumerator       the proposer that emits them, or nil

   `:enumerator nil` is the load-bearing entry: a kind with no proposer
   contributes zero candidates on every tick, and the count of its available
   population is the size of what the selector cannot see."
  {:mission
   {:dir "missions"
    :file-pattern #".*/holes/missions/(M-[^/]+)\.md$"
    :contract-source :code
    :contract-pointer "src/futon2/aif/mission_registry.clj:28-29"
    :candidate-types #{:advance-mission :open-mission}
    :enumerator {:proposer :mission-enumerator
                 :pointer "src/futon2/aif/mission_registry.clj:322"}}
   :excursion
   {:dir "excursions"
    :file-pattern #".*/holes/excursions/(E-[^/]+)\.md$"
    :contract-source :declared-by-this-check
    :contract-pointer nil
    :candidate-types #{}
    :enumerator nil}
   :ticket
   {:dir "tickets"
    :file-pattern #".*/holes/tickets/(T-[^/]+)\.md$"
    :contract-source :code
    :contract-pointer "src/futon2/aif/mission_registry.clj:load-tickets"
    :candidate-types #{:advance-ticket}
    :enumerator {:proposer :ticket-enumerator
                 :pointer "src/futon2/aif/mission_registry.clj:ticket-enumerator-proposer"}}})

(def proposer-list-pointer
  "Where the tick's proposer list is composed. A kind absent from that vector
   is a kind the selector never enumerates."
  "scripts/futon2/report/war_machine.clj:6005-6013")

;; ---------------------------------------------------------------------------
;; Status classification (this check's own reading of a doc's leading state)
;; ---------------------------------------------------------------------------

(def ^:private status-line-pattern
  #"(?i)^\s*(?:[-*]\s*)?(?:#+\s*)?(?:\*\*)?Status:?(?:\*\*)?\s*:?\s*(.+)$")

(def ^:private terminal-leads
  "Leading state tokens that put a doc out of the work queue. Prefix-matched,
   so SUPERSEDED-AS-MISSION lands with SUPERSEDED."
  ["ARCHIVED" "PARKED" "SUPERSEDED" "ABANDONED" "DEFERRED"
   "COMPLETE" "COMPLETED" "CLOSED" "DONE" "DISCHARGED" "ANSWERED" "DISSOLVED"])

(defn classify-doc-status
  "Classify a doc from the LEADING token of its first Status line.

   Only the leading token decides, because these Status lines describe
   per-phase progress: \"HEAD complete; IDENTIFY drafted\" is a live mission
   with the word `complete` in it. An unrecognised lead, or no Status line at
   all, classifies :live -- a work queue that hides work on an unparsed line is
   worse than one that offers too much.

   Returns `{:status-line s :status-class :live|:terminal|:draft}`."
  [text]
  (let [lines (take 20 (str/split-lines (or text "")))
        status-line (some (fn [line]
                            (second (re-matches status-line-pattern line)))
                          lines)
        upper (str/upper-case (or status-line ""))
        lead (-> upper (str/replace #"^[\s>*#`_~-]+" "") str/trim)
        head (or (re-find #"[A-Z][A-Z-]*" lead) "")]
    {:status-line status-line
     :status-class (cond
                     (str/includes? upper "SPECIFIED, NOT YET IMPLEMENTED") :draft
                     (= "DRAFT" head) :draft
                     (some #(str/starts-with? head %) terminal-leads) :terminal
                     :else :live)}))

(defn- classify-ticket-doc-status
  "Independent reading of the ticket status table (lead packet 21033)."
  [text]
  (let [status (some #(second (re-find #"(?i)^\s*\*\*Status(?:\s*\([^)]*\))?\s*:\s*(.*)$" %))
                     (str/split-lines text))
        lead (-> (or status "") str/upper-case (str/replace #"^[\s*_]+" ""))]
    {:status-line status
     :status-class (cond
                     (re-find #"JOE['’]S CALL|AWAIT[^.]*JOE|^(WATCH|FINDING|DESIGN CONSTRAINT)\b" lead) :draft
                     (re-find #"^(DONE|SUPERSEDED|DEFERRED|PARKED|ARCHIVED)" lead) :terminal
                     :else :live)}))

;; ---------------------------------------------------------------------------
;; The scan
;; ---------------------------------------------------------------------------

(defn- fence-reason
  [path]
  (some (fn [[pattern reason]] (when (re-find pattern path) reason)) path-fences))

(defn- kind-files
  "Every file under `<code-root>/<repo>/holes/<dir>/`. Enumerated one repo
   directory at a time rather than by walking the root: walking ~/code also
   descends build trees and node_modules, which turns a scan into minutes."
  [code-root dir]
  (->> (or (.listFiles (io/file code-root)) (make-array File 0))
       (filter #(.isDirectory ^File %))
       (sort-by #(.getName ^File %))
       (mapcat #(or (.listFiles ^File (io/file % "holes" dir))
                    (make-array File 0)))
       (filter #(.isFile ^File %))
       (map #(.getAbsolutePath ^File %))))

(defn scan-population
  "Scan one kind and return its available population plus a typed exclusion
   ledger.

   Every file the contract matches lands in exactly one of `:available` or
   `:excluded`, and every `:excluded` entry names its reason:

     :non-contract-filename    the file is under the directory but is not what
                               the kind's file pattern admits
     :sandbox-path / :git-worktree / :health-main-worktree / :index-check-copy
                               a path fence (see `path-fences`)
     :derived-id               a dotted id -- a handoff/journal doc named after
                               its parent, not an item of its own
     :duplicate-id             a second copy of an id already taken from a
                               shorter path (the primary checkout)
     :status-terminal / :status-draft
                               this check's own reading of the doc's leading
                               state token

   `:counts` is computed from the ledger, not asserted beside it."
  ([kind] (scan-population kind default-code-root))
  ([kind code-root]
   (let [{:keys [dir file-pattern]} (get kinds kind)
         all-files (kind-files code-root dir)
         [matched non-contract] ((juxt filter remove)
                                 #(re-matches file-pattern %) all-files)
         entry (fn [path]
                 (merge {:kind kind
                         :id (second (re-matches file-pattern path))
                         :path path}
                        ((if (= kind :ticket) classify-ticket-doc-status classify-doc-status) (slurp path))))
         ;; Shortest path first, so the primary checkout of a duplicated id is
         ;; the copy that survives dedupe and the copies are what get typed.
         ordered (sort-by (juxt count identity) matched)
         stepped (reduce
                  (fn [acc path]
                    (let [id (second (re-matches file-pattern path))
                          reason (or (fence-reason path)
                                     (cond
                                       (and (not= kind :ticket) (str/includes? (or id "") ".")) :derived-id
                                       (contains? (:seen acc) id) :duplicate-id))]
                      (if reason
                        (update acc :excluded conj {:kind kind :id id :path path
                                                    :reason reason})
                        (let [e (entry path)]
                          (-> acc
                              (update :seen conj id)
                              (update :kept conj e))))))
                  {:seen #{} :kept [] :excluded []}
                  ordered)
         by-class (group-by :status-class (:kept stepped))
         available (vec (:live by-class))
         status-excluded (for [[cls es] (dissoc by-class :live)
                               e es]
                           (assoc (select-keys e [:kind :id :path :status-line])
                                  :reason (case cls
                                            :terminal :status-terminal
                                            :draft :status-draft
                                            :status-unclassified)))
         excluded (vec (concat (map (fn [p] {:kind kind :id nil :path p
                                             :reason :non-contract-filename})
                                    non-contract)
                               (:excluded stepped)
                               status-excluded))]
     {:kind kind
      :code-root code-root
      :contract-source (:contract-source (get kinds kind))
      :contract-pointer (:contract-pointer (get kinds kind))
      :available available
      :available-ids (vec (sort (map :id available)))
      ;; Which checkout each surviving item came from. The fences and the
      ;; dedupe are supposed to leave only primary checkouts; a sibling copy
      ;; (`futon3c-codex10-f18`, `futon6-old-copy`) appearing here is an item
      ;; that exists on a branch and nowhere else, which is a finding about the
      ;; population and not a defect in the check.
      :available-by-repo (into (sorted-map)
                               (frequencies
                                (keep #(second (re-find #"/([^/]+)/holes/" (:path %)))
                                      available)))
      :excluded excluded
      :counts {:files-under-dir (count all-files)
               :contract-matched (count matched)
               :available (count available)
               :excluded (count excluded)}
      :exclusions-by-reason (into (sorted-map)
                                  (frequencies (map :reason excluded)))})))

;; ---------------------------------------------------------------------------
;; The comparison
;; ---------------------------------------------------------------------------

(defn enumerated-targets
  "The targets a tick enumerated for KIND, read off recorded candidates.

   CANDIDATES is any sequence carrying `{:action {:type _ :target _}}` (a
   record's `:controller-ranking` or `:ranked-actions`) or the bare
   `{:type _ :target _}` action maps the proposers emit."
  [kind candidates]
  (let [types (:candidate-types (get kinds kind))]
    (->> candidates
         (map #(or (:action %) %))
         (filter #(contains? types (:type %)))
         (map #(let [t (:target %)] (if (keyword? t) (name t) (str t))))
         distinct
         sort
         vec)))

(defn compare-kind
  "Compare one kind's scanned population with what a tick enumerated.

   Verdicts:
     :complete             both sets agree, member for member
     :incomplete           the enumerator skipped available items, or emitted
                           targets the scan cannot find -- either way untyped
     :kind-not-enumerated  no proposer exists for this kind, so the whole
                           population is outside the selector's view. Typed,
                           with the pointer to the proposer list that omits it;
                           NOT counted as agreement."
  [scan candidates]
  (let [kind (:kind scan)
        {:keys [enumerator]} (get kinds kind)
        available (set (:available-ids scan))
        enumerated (set (enumerated-targets kind candidates))
        missing (vec (sort (remove enumerated available)))
        phantom (vec (sort (remove available enumerated)))]
    (cond-> {:kind kind
             :enumerator (or enumerator
                             {:proposer nil :pointer proposer-list-pointer})
             :available-count (count available)
             :enumerated-count (count enumerated)
             :missing missing
             :phantom phantom
             :scan-counts (:counts scan)
             :exclusions-by-reason (:exclusions-by-reason scan)
             :available-by-repo (:available-by-repo scan)
             :verdict (cond
                        (nil? enumerator) :kind-not-enumerated
                        (and (empty? missing) (empty? phantom)) :complete
                        :else :incomplete)}
      (nil? enumerator)
      (assoc :reason :no-proposer-for-kind
             :would-need "a proposer for this kind in the tick's proposer list"))))

(defn completeness-record
  "The whole typed record: one comparison per kind, plus the verdict over them.

   `:verdict` is :complete only when every kind that HAS an enumerator agrees
   with its scan. Kinds without an enumerator cannot make the record green and
   cannot make it red either -- they are carried in `:typed-kind-absences`,
   where a reader counts them."
  ([candidates] (completeness-record candidates {}))
  ([candidates {:keys [code-root kinds-to-check]
                :or {code-root default-code-root}}]
   (let [ks (or kinds-to-check (sort (keys kinds)))
         comparisons (mapv (fn [k]
                             (compare-kind (scan-population k code-root) candidates))
                           ks)
         enumerated-kinds (filter #(not= :kind-not-enumerated (:verdict %)) comparisons)
         absences (filterv #(= :kind-not-enumerated (:verdict %)) comparisons)]
     {:version :enumeration-completeness/v1
      :at (str (java.time.Instant/now))
      :code-root code-root
      :independent-of proposer-list-pointer
      :kinds comparisons
      :typed-kind-absences (mapv (fn [c] (select-keys c [:kind :available-count
                                                         :reason :would-need]))
                                 absences)
      :verdict (if (every? #(= :complete (:verdict %)) enumerated-kinds)
                 :complete
                 :incomplete)})))
