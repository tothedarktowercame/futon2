(ns futon2.aif.mission-registry
  "Mission registry adapter for the WM AIF apparatus.

   AUTHORITATIVE SOURCE (2026-09-17, Joe): substrate-2 (futon1b :7073). The
   zero-arg `load-missions` reads mission entities from there — one per
   mission doc the file contract admits, written by the ingester
   `scripts/futon2/aif/mission_substrate_ingest.clj` — and refuses with a
   typed reason if the store is unreachable or empty. It never falls back to
   the filesystem. The explicit file scan survives as
   `load-missions-from-files` / the one-arg `load-missions`, for the ingester
   and the test suite.

   The interface (`load-missions` / `open-missions` /
   `can-propose? :open-mission` / the enumerator proposer) is unchanged;
   entries keep the file-scan shape (`:id :path :title :status-line
   :status-class :open-hole-count`) with substrate provenance under
   `:provenance/*` props."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.substrate :as substrate])
  (:import (java.io File)))

(def default-code-root
  "The scan root the registry reads mission documents under. Public because a
   consumer deriving wants from those documents needs the same root to compute
   a repo-relative locator path."
  (str (System/getProperty "user.home") "/code"))

(def ^:private mission-path-pattern
  #".*/holes/missions/(M-[^/]+)\.md$")

(def ^:private status-line-pattern
  #"(?i)^\s*(?:[-*]\s*)?(?:#+\s*)?(?:\*\*)?Status:?(?:\*\*)?\s*:?\s*(.+)$")

(def ^:private unchecked-task-pattern
  #"^\s*[-*]\s+\[\s\]\s+\S.*$")

(def ^:private explicit-work-marker-pattern
  #"(?i)(?:^|[\s`(*_:-])(?:TODO|FIXME|XXX|TBD)(?:$|[\s`),.:;_-])|(?:\b(?:open\s+)?(?:hole|sorry)\s*:)|\bsorry/[-A-Za-z0-9_.]+")

(def ^:private pending-lifecycle-pattern
  #"(?i)\b(?:HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE)\b[^.\n;|]*\b(?:pending|next|open|blocked|active|in[ -]progress|remaining|not yet|needed)\b|\b(?:pending|next|open|blocked|active|in[ -]progress|remaining|not yet|needed)\b[^.\n;|]*\b(?:HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE)\b")

(def ^:private open-section-heading-pattern
  #"(?i)^\s*#{2,6}\s+(?:open questions?|open tasks?|remaining work|remaining tasks?|pending work|next steps)\b.*$")

(def ^:private list-item-pattern
  #"^\s*(?:[-*+]\s+|\d+[.)]\s+)\S.*$")

(defn- mission-id-from-path
  [path]
  (second (re-matches mission-path-pattern path)))

(defn- sandbox-path?
  [path]
  (str/includes? path "/.state/"))

(def ^:private non-primary-path-patterns
  "Patterns that identify non-primary checkout paths — git worktrees,
   directory copies, and other sources of duplicate mission-doc hits.

   Each entry is [pattern source-name]. These exclusions supplement the
   structural .git directory/file check at repository enumeration. They retain
   cross-repo exclusions and known directory-copy exclusions that .git alone
   cannot distinguish. Plain directories without .git remain eligible; duplicate
   IDs among admitted directories still rely on the path-length dedupe heuristic.

   The patterns are deliberately structural (not per-directory-name):
     - /.worktrees/    — the standard git worktree location
     - -health-main    — futon5's health-check worktree
     - -index-check    — futon3c's index-check directory copy
     - futon3b/        — futon3b is a separate repo that duplicates some futon3 docs

   Adding a new drift source = adding a pattern here with its source name.
   The census script (scripts/mission_scan_census.bb) reports which patterns fire
   so future drift has a one-line explanation."
  [[#"/\.worktrees/" "git-worktree"]
   [#"-health-main/" "health-main-worktree"]
   [#"-index-check/" "index-check-copy"]
   [#"futon3b/holes/missions/" "futon3b-cross-repo"]])

(defn- non-primary-path?
  "True when PATH matches a known worktree, directory-copy, or cross-repo
   exclusion. Repository enumeration separately excludes .git-file worktrees."
  [path]
  (some (fn [[pattern _]] (re-find pattern path)) non-primary-path-patterns))

(defn- derived-mission-id?
  [mission-id]
  (str/includes? (or mission-id "") "."))

(defn- mission-title-from-lines
  [mission-id lines]
  (or (some (fn [line]
              (when-let [[_ title] (re-matches #"^#\s+(.+)$" line)]
                title))
            lines)
      mission-id))

(defn- classify-status
  "Classify a mission from its Status line. Terminal / draft / inactive states are
   recognised ONLY from the LEADING state token (the mission's overall state, after
   stripping markdown emphasis). Mission statuses describe per-phase progress, so a
   mid-line keyword — \"HEAD complete; IDENTIFY drafted\", \"… 4 done\", \"MAP completed\",
   \"PARTIAL (Phases 2-4 deferred)\", \"revised-draft landed\" — must NOT terminally
   classify a still-live mission; the leading token wins. Bias is toward :unknown
   (kept live) when the lead is unrecognised — a work queue should not silently hide
   work."
  [status-line]
  (let [upper (str/upper-case (or status-line ""))
        lead  (-> upper (str/replace #"^[\s>*#`_~-]+" "") str/trim)
        head  (or (re-find #"[A-Z][A-Z-]*" lead) "")
        prefix? (fn [coll] (some #(str/starts-with? head %) coll))]
    (cond
      (str/includes? upper "SPECIFIED, NOT YET IMPLEMENTED")            :draft
      (= "DRAFT" head)                                                  :draft
      ;; Finding-2 (E-live-loop-3): prefix match catches compound forms
      ;; like SUPERSEDED-AS-MISSION that the old exact-match missed.
      (prefix? #{"ARCHIVED" "PARKED" "SUPERSEDED" "ABANDONED" "DEFERRED"}) :inactive
      (prefix? #{"COMPLETE" "COMPLETED" "CLOSED" "DONE" "DISCHARGED"
                 "ANSWERED" "DISSOLVED"})                                :complete
      (= "ACTIVE" head)                                                 :active
      (= "OPEN" head)                                                   :open
      (= "PARTIAL" head)                                                :partial
      (= "IDENTIFY" head)                                               :identify
      (= "HEAD" head)                                                   :open
      ;; a leading lifecycle-phase token = work in flight = live
      (#{"INSTANTIATE" "MAP" "DERIVE" "ARGUE" "VERIFY"} head)           :active
      :else                                                            :unknown)))

(defn- heading-level
  [line]
  (some-> (re-find #"^\s*(#{1,6})\s+" line) second count))

(defn- open-section-items
  "The list items under an open-work heading, retained rather than summed.
   Same traversal as the count it replaces."
  [lines]
  (loop [remaining (map-indexed (fn [i l] [(inc i) l]) lines)
         active-level nil
         acc []]
    (if-let [[n line] (first remaining)]
      (let [level (heading-level line)
            next-active-level (cond
                                (re-find open-section-heading-pattern line) level
                                (and active-level level (<= level active-level)) nil
                                :else active-level)
            count-line? (and next-active-level
                             (not (re-find open-section-heading-pattern line))
                             (re-find list-item-pattern line))]
        (recur (rest remaining)
               next-active-level
               (cond-> acc
                 count-line? (conj {:kind :open-section-item :line n
                                    :text (str/trim line)}))))
      acc)))

(defn- hole-identity
  "Stable identity for a retained hole: the mission id with a SHA-256 prefix of
   the whitespace-normalised item text, so a hole keeps its identity when the
   surrounding document is edited and its line number moves."
  [mission-id text]
  (let [norm (str/replace (str/trim text) #"\s+" " ")
        md (java.security.MessageDigest/getInstance "SHA-256")
        bs (.digest md (.getBytes (str mission-id "\u0000" norm) "UTF-8"))]
    (str mission-id "#" (apply str (map #(format "%02x" %) (take 6 bs))))))

(defn open-holes
  "The remaining-work items a mission document states, RETAINED rather than
   summed away.

   These are the same matches `open-hole-count` has always made -- unchecked
   tasks, explicit work markers, pending lifecycle lines, and list items under
   an open-work heading. Until 2026-09-20 only their total survived, so 441
   stated pieces of remaining work across 86 live missions were recomputed on
   every run and discarded (Joe: \"a huge embarrassment ... exactly a facade\").
   They are the missions' own statements of what they want done, and C needs
   wants; a count cannot project into an outcome domain and an item can.

   One item per (pattern, line) match, exactly as the count was formed -- a line
   matching two kinds yields two items -- so `(count (open-holes ...))` equals
   the count this replaced, for every mission. Terminal/draft/inactive states
   have none."
  [mission-id status-class lines]
  (if (contains? #{:complete :inactive :draft} status-class)
    []
    (let [numbered (map-indexed (fn [i l] [(inc i) l]) lines)
          matching (fn [kind pat]
                     (for [[n l] numbered :when (re-find pat l)]
                       {:kind kind :line n :text (str/trim l)}))]
      (mapv (fn [h] (assoc h :id (hole-identity mission-id (:text h))))
            (concat (matching :unchecked-task unchecked-task-pattern)
                    (matching :work-marker explicit-work-marker-pattern)
                    (matching :pending-lifecycle pending-lifecycle-pattern)
                    (open-section-items lines))))))

(defn- open-hole-count
  "Count of the retained items. Derived from `open-holes` so the count and the
   items it summarises cannot disagree."
  [mission-id status-class lines]
  (count (open-holes mission-id status-class lines)))

(defn- mission-doc->entry
  [path]
  (let [lines (str/split-lines (slurp path))
        mission-id (mission-id-from-path path)
        status-line (some (fn [line]
                            (when-let [[_ status] (re-matches status-line-pattern line)]
                              status))
                          (take 20 lines))
        status-class (classify-status status-line)]
    {:id mission-id
     :path path
     :title (mission-title-from-lines mission-id lines)
     :status-line status-line
     :status-class status-class
     :open-holes (open-holes mission-id status-class lines)
     :open-hole-count (open-hole-count mission-id status-class lines)}))

(defn- dedupe-by-id
  "Keep the first entry per mission id after path-length/alphabetic sorting.
   This resolves remaining duplicates among admitted primary checkouts and
   plain directories; the sort itself does not establish checkout authority."
  [entries]
  (let [seen (atom #{})]
    (remove (fn [e]
              (if (contains? @seen (:id e))
                true
                (do (swap! seen conj (:id e))
                    false)))
            entries)))

;; ---------- mission record writes (futon3c watcher lane calls these) ----------
;; Single source for the machine's mission-entity record. The watcher's
;; scope lane (futon3c.watcher.multi/reingest-mission-scopes!) refreshes the
;; record after its scope reingest by calling `upsert-mission-record!`;
;; scripts/futon2/aif/mission_substrate_ingest.clj uses the same builders
;; for whole-corpus backfill.

(defn- sha256-file ^String [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (make-array Byte/TYPE 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- repo-of
  "Source repo for an absolute mission-doc path: the path segment immediately
   under the scan root CODE-ROOT."
  [code-root path]
  (let [rel (str/replace-first (str path) (str code-root "/") "")]
    (first (str/split rel #"/"))))

(defn mission-record-props
  "The props the mission-record writer OWNS for one file-scan ENTRY. Status is
   carried un-reclassified: :mission/status-line is the raw line and
   :mission/status-class the registry classification as a string (unparseable
   stays \"unknown\", never silently defaulted live). Nil-valued keys are
   dropped: XTDB will not store them (learned 2026-09-17, e-f1b19720)."
  [code-root entry]
  (into {}
        (remove (comp nil? val))
        {:scope/role "mission"
         :mission/relation "relates-to"
         :mission/title (:title entry)
         :mission/status-line (:status-line entry)
         :mission/status-class (some-> (:status-class entry) name)
         :mission/open-hole-count (some-> (:open-hole-count entry) long)
         ;; The items the count summarises. Until 2026-09-20 only the total was
         ;; stored, so every mission's stated remaining work was recomputed and
         ;; discarded on each scan; a count cannot project into an outcome
         ;; domain and an item can. Omitted when empty (XTDB drops nil).
         :mission/open-holes (when (seq (:open-holes entry)) (vec (:open-holes entry)))
         :provenance/repo (repo-of code-root (:path entry))
         :provenance/path (:path entry)
         :provenance/sha256 (sha256-file (:path entry))}))

(defn- record-parse-props [props]
  (cond
    (map? props) props
    (string? props) (try (edn/read-string props) (catch Throwable _ {}))
    :else {}))

(defn mission-entity-index
  "All substrate-2 mission entities indexed by :entity/external-id (one
   bounded query; callers reuse it across a corpus run instead of N+1)."
  []
  (->> (substrate/entities-by-type "mission" {:limit 1000})
       (filter :entity/external-id)
       (map (fn [e] [(:entity/external-id e) e]))
       (into {})))

(defn upsert-mission-record!
  "Refresh ONE mission entity in substrate-2 from its doc file (the watcher
   lane's per-land call; also usable standalone). Idempotent: foreign props
   already on the entity are preserved and an unchanged record is a no-op.
   Returns {:id .. :status :created|:updated|:unchanged}. The file scan must
   admit PATH — a path the contract rejects is an :error, not a silent skip.

   `:existing` is the entity this record belongs to. OMIT the key and it is
   looked up here (one bounded query); pass it — including as nil — and that
   answer is taken as given, which is what a corpus run does with a single
   shared `mission-entity-index`. Omitting it used to mean `nil`, and that
   silently cost all three of the guarantees above: with no existing entity
   to merge against, foreign props were replaced rather than preserved,
   `:unchanged` was unreachable so every land wrote, and `:entity/source` was
   overwritten on each pass. The watcher lane calls with `{:path path}`, so
   it got exactly that."
  ([] (upsert-mission-record! {:path nil}))
  ([{:keys [code-root path] :as opts}]
   (let [code-root (or code-root default-code-root)]
     (if-not (and path (re-matches mission-path-pattern (str path)))
       {:status :error :reason :path-not-admitted :path path}
       (let [entry (mission-doc->entry (str path))
             id (:id entry)
             existing (if (contains? opts :existing)
                        (:existing opts)
                        (get (mission-entity-index) id))
             props (record-parse-props (:entity/props existing))
             merged (into {} (remove (comp nil? val))
                          (merge props (mission-record-props code-root entry)))]
         (if (and existing (= props merged))
           {:id id :status :unchanged}
           (do (substrate/put-doc!
                (cond-> {:entity/name (str "mission|" id)
                         :entity/type :mission
                         :entity/external-id id
                         :entity/props merged}
                  (:entity/id existing) (assoc :entity/id (:entity/id existing))
                  ;; ALWAYS send the source. futon1b's write-entity! builds a
                  ;; fresh document from the payload and puts it, so a field
                  ;; left out is erased rather than preserved — omitting an
                  ;; existing foreign source (e.g. "hinge-log-bridge") drops
                  ;; it on the first refresh.
                  true (assoc :entity/source
                              (or (:entity/source existing) "mission-doc-ingest"))))
               {:id id :status (if existing :updated :created)})))))))

(defn- primary-checkout?
  "A repository is a primary checkout exactly when its .git is a directory."
  [repo]
  (.isDirectory (io/file repo ".git")))

(defn- mission-scan-repo?
  "Admit primary checkouts and preserve historical scans of plain directories
   without .git. A .git file identifies a worktree and is excluded before any
   mission documents are enumerated; no git subprocess or recursive walk."
  [repo]
  (or (primary-checkout? repo)
      (not (.exists (io/file repo ".git")))))

(defn load-missions-from-files
  "The explicit FILE scan. `<code-root>/<repo>/holes/missions/M-*.md` only,
   with the scan-root fences and dedupe documented below. This is the
   ingester's reader (scripts/futon2/aif/mission_substrate_ingest.clj) and the
   form the test suite uses against tmpdirs; the machine's default load
   (`load-missions`, zero-arg) reads substrate-2 and never falls back here."
  ([code-root]
   (let [root (io/file code-root)
         ;; The contract admits only <code-root>/<repo>/holes/missions/M-*.md.
         ;; Enumerate exactly those directories. Walking all of ~/code also
         ;; traversed build trees, worktrees and node_modules before filtering
         ;; them out, turning a single construction lookup into minutes of IO.
         mission-files (->> (or (.listFiles root) (make-array File 0))
                            (filter #(.isDirectory ^File %))
                            (filter mission-scan-repo?)
                            (map #(io/file % "holes" "missions"))
                            (filter #(.isDirectory ^File %))
                            (mapcat #(or (.listFiles ^File %)
                                         (make-array File 0))))
         missions (->> mission-files
                       (filter #(.isFile %))
                       (map #(.getAbsolutePath %))
                       (filter #(re-matches mission-path-pattern %))
                       (remove sandbox-path?)
                       ;; Keep cross-repo and known-copy exclusions alongside
                       ;; the structural repository filter above.
                       (remove non-primary-path?)
                       ;; Resolve duplicate IDs among admitted checkouts/plain
                       ;; directories by path length, then alphabetically. This
                       ;; heuristic is not evidence of checkout authority.
                       (sort-by (juxt count identity))
                       (map mission-doc->entry)
                       (remove #(derived-mission-id? (:id %)))
                       (dedupe-by-id)
                       vec)]
     {:missions missions})))

(defn- parse-entity-props
  "The entities read route returns :entity/props as an EDN string; normalize
   to a map (nil → {})."
  [props]
  (cond
    (map? props) props
    (string? props) (try (edn/read-string props) (catch Throwable _ {}))
    :else {}))

(defn- substrate-entity->entry
  "Map one substrate-2 mission entity to the file-scan entry shape
   (`:id :path :title :status-line :status-class :open-hole-count`).
   A mission entity with no recorded status class reads as `:unknown` — the
   same honest-unparseable bias `classify-status` gives a file whose Status
   line it cannot read; it is NOT silently defaulted to a live class."
  [entity]
  (let [props (parse-entity-props (:entity/props entity))]
    {:id (:entity/external-id entity)
     :path (:provenance/path props)
     :title (or (:mission/title props) (:entity/name entity) (:entity/external-id entity))
     :status-line (:mission/status-line props)
     :status-class (if-some [sc (:mission/status-class props)]
                     (keyword sc)
                     :unknown)
     :open-hole-count (long (or (:mission/open-hole-count props) 0))
     :open-holes (vec (or (:mission/open-holes props) []))
     ;; A stored count with no stored items means this entity predates hole
     ;; retention: the mission HAS that many stated wants and substrate cannot
     ;; name them until re-ingest. Typed so the gap is legible instead of
     ;; reading as "no holes" -- that silence is the facade being fixed.
     :open-holes-status (cond
                          (seq (:mission/open-holes props)) :retained
                          (pos? (long (or (:mission/open-hole-count props) 0))) :not-ingested
                          :else :none)}))

(defn load-missions-from-substrate
  "Read the mission registry from substrate-2 (futon1b, :7073) and return the
   same `{:missions [...]}` shape the file scan produced. NO FALLBACK: an
   unreachable store or an empty registry is a typed refusal
   (`:kind :substrate-unreachable` / `:kind :substrate-mission-registry-empty`)
   that the caller must surface — this namespace never quietly re-scans files
   instead (Joe 2026-09-17: substrate-2 holds all the missions and the machine
   points at it, not at a second source it doesn't announce)."
  ([] (load-missions-from-substrate {}))
  ([opts]
   (let [entities (try
                    (substrate/entities-by-type "mission" (assoc opts :limit 1000))
                    (catch Throwable t
                      (throw (ex-info "substrate-2 mission registry unreachable"
                                      {:kind :substrate-unreachable}
                                      t))))
         entries (->> (or entities [])
                      (filter #(str/starts-with? (str (:entity/external-id %)) "M-"))
                      (map substrate-entity->entry)
                      vec)]
     (when (empty? entries)
       (throw (ex-info "substrate-2 mission registry returned no missions"
                       {:kind :substrate-mission-registry-empty})))
     {:missions entries})))

(defn load-missions
  "Load the mission registry the machine points at: substrate-2
   (`load-missions-from-substrate`). Zero-arg is the machine's read and NEVER
   falls back to the filesystem. The one-arg form is the EXPLICIT file scan
   (same as `load-missions-from-files`) — kept for the ingester and the test
   suite, never as an automatic fallback."
  ([] (load-missions-from-substrate))
  ([code-root] (load-missions-from-files code-root)))

(def ^:private missions-cache (atom nil))

(def missions-cache-ttl-ms
  "TTL for the load-missions snapshot. Since 2026-09-17 the zero-arg load is a
   bounded substrate-2 query (cheap), so the TTL exists only to coalesce the
   per-action hot path (the WM guardrail selector calls `mission-status` once
   per ranked :open-mission — dozens per selection). It is deliberately short
   so a stale store is re-read within one selection cycle rather than hidden."
  5000)

(defn load-missions-cached
  "Memoized `load-missions` (TTL = `missions-cache-ttl-ms`). Within one WM
   selection the per-action calls hit the cache after the first scan; across
   cycles the TTL refreshes. Use this on hot per-action paths, not `load-missions`."
  ([] (load-missions-cached nil))
  ([code-root]
   (let [now (System/currentTimeMillis)
         c   @missions-cache]
     (if (and c (= code-root (:code-root c)) (< (- now (:at c)) missions-cache-ttl-ms))
       (:doc c)
       ;; nil code-root => zero-arg load-missions (preserves any rebinding of the
       ;; zero-arity var, e.g. test redefs pointing at a tmpdir).
       (let [doc (if code-root (load-missions code-root) (load-missions))]
         (reset! missions-cache {:at now :code-root code-root :doc doc})
         doc)))))

(defn live-mission?
  "True when a loaded mission entry is eligible for WM `:open-mission`
   enumeration/ranking."
  [mission]
  (not (contains? #{:complete :inactive :draft}
                  (:status-class mission))))

(defn open-missions
  "Filter loaded missions to those that are live. Zero-arg variant
   scans the default code root; one-arg variant accepts a pre-loaded doc."
  ([] (open-missions (load-missions)))
  ([loaded]
   (vec (filter live-mission? (:missions loaded)))))

(defn mission-target-id
  "Normalize a mission action target to the file-backed registry id when possible.
   Substrate-2 mission endpoints such as `futon4-d/mission/foo` normalize to
   `M-foo`; ordinary registry ids such as `M-foo` are returned unchanged."
  [target]
  (let [target (cond
                 (keyword? target) (name target)
                 (some? target) (str target)
                 :else nil)]
    (cond
      (nil? target) nil
      (str/starts-with? target "M-") target
      :else (when-let [[_ local-id] (re-find #"/mission/([^/]+)$" target)]
              (str "M-" local-id)))))

(defn live-mission-target?
  "True if TARGET resolves to one of MISSIONS' live registry ids."
  [missions target]
  (let [live-ids (set (map :id (filter live-mission? missions)))]
    (contains? live-ids (mission-target-id target))))

(defn mission-status
  "Return WM guardrail status for TARGET from the mission registry.
   The result is deliberately small and stable for requiring-resolve callers:
   `{:open? <bool> :open-hole-count <n>}`. Unknown targets are closed with
   zero holes."
  [target]
  (let [target-id (mission-target-id target)
        mission (some #(when (= target-id (:id %)) %)
                      (:missions (load-missions-cached)))]
    {:open? (boolean (and mission (live-mission? mission)))
     :open-hole-count (long (or (:open-hole-count mission) 0))}))

(defmethod fm/can-propose? :open-mission
  [state _action-type]
  (boolean (seq (:missions state))))

(defmethod fm/can-execute? :open-mission
  [state action]
  (live-mission-target? (:missions state []) (:target action)))

(defmethod fm/can-propose? :advance-mission
  [state _action-type]
  (boolean (seq (:missions state))))

(defmethod fm/can-execute? :advance-mission
  [state action]
  (live-mission-target? (:missions state []) (:target action)))

(def mission-enumerator-proposer
  "Proposer that emits one mission candidate per addressable mission in the
   state map. State must carry `:missions` (populated by `open-missions`).

   A live mission doc IS an already-open mission (the doc exists because the
   mission was opened), so the candidate type is `:advance-mission` — engage
   the mission's open holes — not `:open-mission`. Proposing `:open-mission`
   for already-open missions made the whole top of the WM differential
   un-earnable (teleport): the forward model predicted :spawned for missions
   that already existed (pilot cycle #1 finding, 2026-06-10). `:open-mission`
   remains a recognised type for a future substrate that can distinguish
   genuinely-unopened missions (e.g. proposals/drafts greenlit by the
   operator)."
  (reify ap/ActionProposer
    (propose [_ state]
      (for [m (:missions state)]
        {:type :advance-mission
         :target (:id m)
         :weight 1.0
         :mission-path (:path m)
         ;; carried so the forward model can be TARGET-SENSITIVE without a
         ;; registry dependency cycle (predict reads the action, not the
         ;; registry) — same pattern as :intrinsic-value on address-sorry.
         :open-hole-count (:open-hole-count m)
         :rationale (str "mission substrate: " (:title m)
                         " [" (name (:status-class m))
                         "; advance open holes]")}))
    (proposer-id [_] :mission-enumerator)))

;; Ticket status table (Joe / claude-20, packet 21033, 2026-09-15):
;; DONE* -> complete; SUPERSEDED/DEFERRED/PARKED/ARCHIVED -> inactive;
;; WATCH/FINDING/DESIGN CONSTRAINT or awaiting Joe/Joe's call -> not-actionable;
;; PARTIAL/OPEN/STILL-OPEN/SCOPED/DESIGNED/RECLASSIFY and unknown -> live.
(defn ticket-status-text [lines]
  (some #(second (re-find #"(?i)^\s*\*\*Status(?:\s*\([^)]*\))?\s*:\s*(.*)$" %)) lines))

(defn classify-ticket-status [text]
  (let [s (-> (or text "") str/upper-case (str/replace #"^[\s*_]+" ""))]
    (cond
      (re-find #"JOE['’]S CALL|AWAIT[^.]*JOE" s) :not-actionable
      (str/starts-with? s "DONE") :complete
      (re-find #"^(SUPERSEDED|DEFERRED|PARKED|ARCHIVED)\b" s) :inactive
      (re-find #"^(WATCH|FINDING|DESIGN CONSTRAINT)\b" s) :not-actionable
      :else :live)))

(defn live-ticket? [ticket] (= :live (:status-class ticket)))

(defn load-tickets
  "Immediate primary-checkout holes/tickets/T-*.md only. Same mission scan
   fences before ID deduplication; bare holes/T-* discovery notes excluded."
  ([] (load-tickets default-code-root))
  ([code-root]
   {:tickets
    (->> (or (.listFiles (io/file code-root)) (make-array File 0))
         (filter #(.isDirectory ^File %))
         (map #(io/file % "holes" "tickets"))
         (mapcat #(or (.listFiles ^File %) (make-array File 0)))
         (filter #(.isFile ^File %))
         (map #(.getAbsolutePath ^File %))
         (filter #(re-matches #".*/holes/tickets/T-[^/]+\.md$" %))
         (remove sandbox-path?)
         (remove #(non-primary-path? (str/replace % "/holes/tickets/" "/holes/missions/")))
         (sort-by (juxt count identity))
         (map (fn [path]
                (let [id (str/replace (.getName (io/file path)) #"\.md$" "")
                      lines (str/split-lines (slurp path))
                      status (ticket-status-text lines)]
                  {:id id :kind :ticket :path path
                   :title (mission-title-from-lines id lines)
                   :status-line status :status-class (classify-ticket-status status)
                   :parent (some #(when (re-find #"(?i)parent" %)
                                    (re-find #"M-[A-Za-z0-9_-]+" %)) lines)})))
         dedupe-by-id vec)}))

(defn ticket-entry [target]
  (when (and (string? target) (str/starts-with? target "T-"))
    (some #(when (= target (:id %)) %) (:tickets (load-tickets)))))

(defn live-ticket-target? [tickets target]
  (boolean (some #(and (= target (:id %)) (live-ticket? %)) tickets)))

(defn ticket-status [target]
  (let [live? (boolean (some-> (ticket-entry target) live-ticket?))]
    {:open? live? :open-hole-count (if live? 1 0)}))

(defn work-target-status
  "Mission semantics unchanged; T- identities resolve only in the ticket registry."
  [target]
  (if (and (string? target) (str/starts-with? target "T-"))
    (ticket-status target)
    (mission-status target)))

(defmethod fm/can-propose? :advance-ticket [state _]
  (boolean (some live-ticket? (:tickets state))))
(defmethod fm/can-execute? :advance-ticket [state action]
  (live-ticket-target? (:tickets state) (:target action)))

(def ticket-enumerator-proposer
  (reify ap/ActionProposer
    (propose [_ state]
      (for [t (:tickets state) :when (live-ticket? t)]
        {:type :advance-ticket :target (:id t) :weight 1.0
         :ticket-path (:path t) :open-hole-count 1
         :rationale (str "ticket substrate: " (:title t))}))
    (proposer-id [_] :ticket-enumerator)))
