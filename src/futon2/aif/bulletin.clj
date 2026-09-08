(ns futon2.aif.bulletin
  "The morning bulletin: the producer half of `futon2.aif.morning-brief`.

  One committed `BULLETIN-<date>.md` per day, plus one immutable morning-brief
  item carrying the same counts, so the operator reads what was accomplished
  and answers typed questions about it through the review path that already
  exists (`futon2.aif.morning-brief/review!`).

  Every fact here is read from a named source and rendered with its pointer --
  a commit sha, a run id, or a path; a section with no facts prints its absence
  rather than disappearing.

  `render` is a pure function of the collected facts -- no clock, no HEAD sha --
  and `generate!` rewrites the file only when those bytes differ from what is
  on disk. So regenerating on a day that gained no new facts is a no-op, and
  the file's mtime, which is what the voxterm backlog panel reports as its age,
  moves only when the day's content moved.

  It runs at the wm-build-loop's end-of-session stop (`wm-build-loop.sh`,
  `bulletin()`), not on a timer and not as a daemon."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.morning-brief :as brief])
  (:import [java.time LocalDate]))

(def ^:private home (System/getProperty "user.home"))

(def default-config
  "Where the bulletin reads from and writes to. Every path is explicit so a
  test can point the whole producer at a fixture tree."
  {:repos [{:name "futon2" :path (str home "/code/futon2")}
           {:name "p4ng" :path (str home "/code/p4ng")}]
   :boards [{:name "wm-contract"
             :repo (str home "/code/futon2")
             :rel "holes/labs/wm-contract/worklist.edn"}
            {:name "zaif-harness"
             :repo (str home "/code/futon2")
             :rel "holes/labs/zaif-harness/worklist.edn"}]
   :registry (str home "/code/futon2/holes/labs/wm-contract/aif-equations.edn")
   :run-era-ledger (str home "/code/futon2/holes/labs/wm-contract/run-era-ledger.edn")
   :runs-repo (str home "/code/futon2")
   :runs-rel "holes/labs/wm-contract/runs"
   :tripwire-root "/home/joe/code/futon2/data/wm-tripwires/trips"
   :trace-root "/home/joe/code/futon2/data/wm-trace"
   :trace-review-ledger (str home "/code/futon2/holes/labs/zaif-harness/trace-review-ledger.edn")
   :out-dir (str home "/code/futon2/holes/labs/wm-contract/bulletins")
   :bulletin-rel-dir "holes/labs/wm-contract/bulletins"
   :brief-root brief/default-root})

;; ---------------------------------------------------------------- git reads

(defn- git
  "Run git in `repo`, returning stdout, or nil if the command failed. A failed
  read becomes a typed absence at the rendering site, never an empty section
  that reads like 'nothing happened'."
  [repo & args]
  (let [{:keys [exit out]} (apply shell/sh "git" (concat args [:dir repo]))]
    (when (zero? exit) out)))

;; ASCII record/unit separators: they cannot occur in a commit subject or a
;; path, so the log parses without a delimiter that a subject could forge.
(def ^:private record-sep "\u001e")
(def ^:private field-sep "\u001f")

(defn parse-log
  "Parse the `--name-only` log `day-commits` asks for into commit maps."
  [text]
  (->> (str/split (or text "") (re-pattern record-sep))
       (remove str/blank?)
       (mapv (fn [chunk]
               (let [lines (str/split-lines (str/triml chunk))
                     [sha author subject] (str/split (first lines)
                                                     (re-pattern field-sep) 3)]
                 {:sha sha
                  :author author
                  :subject subject
                  :files (vec (remove str/blank? (rest lines)))})))))

(defn day-commits
  "Commits in `repo` whose date falls on `date`, oldest first. nil when the
  repository could not be read."
  [repo date]
  (some-> (git repo "log"
               (str "--since=" date " 00:00:00")
               (str "--until=" date " 23:59:59")
               (str "--pretty=format:" record-sep "%h" field-sep "%an"
                    field-sep "%s")
               "--name-only")
          parse-log
          reverse
          vec))

(defn area-of
  "The area a changed path belongs to: the lab under `holes/labs/`, or the
  top-level directory otherwise. This is the bulletin's only grouping rule."
  [path]
  (let [segs (str/split path #"/")]
    (cond
      (and (= "holes" (first segs)) (= "labs" (second segs)) (> (count segs) 3))
      (nth segs 2)
      ;; A file at the repository root has no directory to name it by; p4ng
      ;; keeps its generated figures and sections there, and grouping by the
      ;; file name made twelve regenerate commits look like twelve areas.
      (= 1 (count segs)) "(repository root)"
      :else (first segs))))

(defn primary-area
  "One area per commit, so the per-area counts sum to the commit count: the
  area holding the most changed files, ties broken alphabetically."
  [{:keys [files]}]
  (if (empty? files)
    "(no files)"
    (->> files
         (map area-of)
         frequencies
         (sort-by (juxt (comp - val) key))
         ffirst)))

(defn by-area
  "Commits grouped under `primary-area`, areas in descending commit count."
  [commits]
  (->> commits
       (group-by primary-area)
       (sort-by (juxt (comp - count val) key))
       (mapv (fn [[area cs]]
               {:area area
                :commits (vec cs)
                :extra-areas (vec (sort (disj (set (map area-of (mapcat :files cs)))
                                              area)))}))))

;; --------------------------------------------------------------- board reads

(defn- read-edn-file [text]
  (try (edn/read-string text) (catch Exception _ nil)))

(defn- statuses
  "id -> status for one board document."
  [doc]
  (into (sorted-map)
        (keep (fn [i] (when-let [id (:id i)] [id (:status i)])) (:items doc))))

(defn board-delta
  "How one board moved during `date`: rows added, rows whose `:status` changed,
  and rows that left, against the board as it stood at the last commit before
  the day. `:base` is that commit, or nil when the board has no history before
  `date` -- in which case every row reads as added, and the renderer says so."
  [{:keys [name repo rel]} date]
  (let [base (some-> (git repo "rev-list" "-1"
                          (str "--before=" date " 00:00:00") "HEAD" "--" rel)
                     str/trim
                     not-empty)
        before (when base
                 (some-> (git repo "show" (str base ":" rel)) read-edn-file))
        after (read-edn-file (slurp (io/file repo rel)))
        b (statuses before)
        a (statuses after)]
    {:board name
     :base base
     :readable? (some? after)
     :added (vec (for [[id st] a :when (not (contains? b id))] {:id id :status st}))
     :moved (vec (for [[id st] a
                       :when (and (contains? b id) (not= st (get b id)))]
                   {:id id :from (get b id) :to st}))
     :gone (vec (for [[id st] b :when (not (contains? a id))] {:id id :status st}))
     :rows (count a)}))

(defn waits-on-joe
  "Rows only the operator can move, by the rule the voxterm backlog already
  applies (`voxterm/server.py`, `agency_backlog`): owned by Joe, parked
  `:needs-joe`, a `:J` judgement row that is not done, or a blocked row
  whose `:blocker` text says its exit is a ruling (2026-09-05: four
  ruling-blocked rows surfaced as one when only the first three were read)."
  [{:keys [name repo rel]}]
  (let [doc (read-edn-file (slurp (io/file repo rel)))]
    (vec (for [i (:items doc)
               :let [owner (str/lower-case (str (:owner i)))
                     status (:status i)]
               :when (and (not= :done status)
                          (or (str/includes? owner "joe")
                              (= :needs-joe status)
                              (= :J (:class i))
                              (and (= :blocked status)
                                   (some? (re-find #"(?i)joe|ruling|reviewer decision"
                                                   (str (:blocker i)))))))]
           {:board name :id (:id i) :status status :class (:class i)
            :statement (:statement i)}))))

(defn tripwire-discharges
  "Recorded R20 refusals shaped for the existing waits-on-Joe bulletin section."
  [root]
  (let [dir (when root (io/file root))]
    (if-not (and dir (.isDirectory dir))
      []
      (->> (.listFiles dir)
           (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))
           (keep (fn [f]
                   (when-let [record (read-edn-file (slurp f))]
                     (when (and (= :R20 (:node record))
                                (= :refused (:tripwire/check record))
                                (= :needs-joe (:status record)))
                       {:board "R20-tripwire"
                        :id (:trip/id record)
                        :status (:status record)
                        :class (:class record)
                        :statement (:statement record)
                        :record (.getPath f)}))))
           (sort-by :id)
           vec))))

(defn trace-discharges
  "Persisted TRACE route records shaped for the existing waits-on-Joe section.
  The record itself is the substrate; `:record` keeps the channel's citation
  tied to the file from which the event was re-read."
  [root]
  (let [dir (when root (io/file root))]
    (if-not (and dir (.isDirectory dir))
      []
      (->> (.listFiles dir)
           (filter #(and (.isFile %) (str/ends-with? (.getName %) ".edn")))
           (mapcat (fn [f]
                     (with-open [r (java.io.PushbackReader. (io/reader f))]
                       (loop [records []]
                         (let [record (edn/read {:eof ::eof
                                                :default (fn [tag value]
                                                           {:trace/edn-tag tag
                                                            :trace/value value})}
                                               r)]
                           (if (= ::eof record)
                             records
                             (recur
                              (cond-> records
                                (some #(= :TRACE (:node %)) (:wm/route record))
                                (conj {:board "TRACE"
                                       :id (or (:run/id record) (:timestamp record))
                                       :at (:timestamp record)
                                       :status :needs-joe
                                       :class :J
                                       :statement "TRACE record surfaced for operator review"
                                       :record (.getPath f)})))))))))
           (sort-by :id)
           vec))))

(defn trace-review-dispositions
  "Read the append-only TRACE review ledger as record-id -> disposition.
  Each top-level form is one immutable disposition. Duplicate record ids are
  rejected: review history must be extended, never replaced or contradicted."
  [path]
  (let [f (when path (io/file path))]
    (if-not (and f (.isFile f))
      {}
      (with-open [r (java.io.PushbackReader. (io/reader f))]
        (loop [by-id {}]
          (let [entry (edn/read {:eof ::eof} r)]
            (if (= ::eof entry)
              by-id
              (let [id (:record/id entry)]
                (when-not (and id (:disposition entry) (:at entry) (:by entry))
                  (throw (ex-info "invalid TRACE review disposition" {:entry entry})))
                (when (contains? by-id id)
                  (throw (ex-info "duplicate TRACE review disposition" {:record/id id})))
                (recur (assoc by-id id entry))))))))))

(defn append-trace-review!
  "Append one disposition to PATH. Refuses an incomplete or already-reviewed
  record id; it never rewrites the ledger."
  [path entry]
  (let [id (:record/id entry)]
    (when-not (and id (:disposition entry) (:at entry) (:by entry))
      (throw (ex-info "invalid TRACE review disposition" {:entry entry})))
    (when (contains? (trace-review-dispositions path) id)
      (throw (ex-info "TRACE record already reviewed" {:record/id id})))
    (io/make-parents (io/file path))
    (spit path (str (pr-str entry) "\n") :append true)
    entry))

(defn untriaged-traces
  "TRACE records with no disposition, summarized without turning them into
  ruling-shaped decision rows."
  [trace-root ledger-path]
  (let [reviewed (trace-review-dispositions ledger-path)
        records (remove #(contains? reviewed (:id %))
                        (trace-discharges trace-root))]
    {:count (count records)
     :oldest-date (some->> records (keep :at) sort first (#(subs % 0 10)))}))

;; ------------------------------------------------------- registries and runs

(defn ledger-deposits
  "Run-era ledger rows deposited on `date`, in `:row/seq` order."
  [ledger-path date]
  (let [doc (read-edn-file (slurp ledger-path))]
    (->> (:rows doc)
         (filter #(str/starts-with? (str (:row/at %)) date))
         (sort-by :row/seq)
         vec)))

(defn machine-adopted
  "`aif-equations.edn :choices` entries the machine adopted on `date`, each
  with the reversal path its own entry records. An entry with no `:reversal`
  is reported as having none, not silently rendered as reversible."
  [registry-path date]
  (let [doc (read-edn-file (slurp registry-path))]
    (->> (:choices doc)
         (filter (fn [[_ v]]
                   (and (= :adopted-by-machine (:status v))
                        (or (= date (:at v))
                            (= date (get-in v [:adoption :at]))))))
         (sort-by key)
         (mapv (fn [[k v]]
                 {:choice k
                  :row (:row v)
                  :by (get-in v [:adoption :by])
                  :grounds (get-in v [:adoption :grounds])
                  :decision (get-in v [:adoption :decision])
                  :reversal (:reversal v)})))))

(defn- outcome-line
  "The opening paragraph of a run directory's own note -- where these
  directories record what the run found.

  Whole lines are joined up to the first blank one, because the notes are
  hard-wrapped: taking the first LINE cut every outcome mid-sentence (\"One
  tick, run by wm_step.sh step from the pin at /tmp/wm-step-u59/pin into the\")."
  [dir]
  (some (fn [n]
          (let [f (io/file dir n)]
            (when (.isFile f)
              (let [para (->> (str/split-lines (slurp f))
                              (drop-while #(or (str/blank? %)
                                               (str/starts-with? % "#")))
                              (take-while (complement str/blank?)))]
                (when (seq para) (str/join " " para))))))
        ["RECEIPT.md" "README.md" "FINDING.md" "NOTES.md"]))

(defn experiments
  "Run directories under `:runs-rel` that the day's commits wrote into: which
  directory, the commits that recorded it, and the outcome line the directory's
  own receipt states. Files sitting directly under `runs/` are not runs and are
  skipped."
  [{:keys [runs-repo runs-rel]} commits]
  (let [prefix (str runs-rel "/")]
    (->> (for [c commits
               f (:files c)
               :when (str/starts-with? f prefix)
               :let [segs (str/split (subs f (count prefix)) #"/")]
               :when (> (count segs) 1)]
           [(first segs) (:sha c)])
         (reduce (fn [m [d sha]] (update m d (fnil conj []) sha)) {})
         (sort-by key)
         (mapv (fn [[d shas]]
                 {:run d
                  :commits (vec (distinct shas))
                  :outcome (outcome-line (io/file runs-repo runs-rel d))})))))

;; ----------------------------------------------------------- fact gathering
(defn self-commit?
  "A commit that writes nothing but the bulletin directory is the digest
  recording itself, not the day's work.

  Dropping these is what makes the acceptance property true: without it,
  generating and committing a bulletin adds a commit to the day, so the next
  regeneration produces different bytes and commits again -- the digest chases
  its own tail once per session stop. With it, generate -> commit ->
  regenerate is a fixed point."
  [bulletin-rel-dir {:keys [files]}]
  (and (seq files)
       (every? #(str/starts-with? % (str bulletin-rel-dir "/")) files)))


(defn collect-facts
  "Everything the bulletin for `date` states, with nothing rendered yet."
  [cfg date]
  (let [self? (partial self-commit? (:bulletin-rel-dir cfg))
        repo-commits (mapv (fn [{:keys [name path]}]
                             {:repo name :path path
                              :commits (some->> (day-commits path date)
                                                (remove self?)
                                                vec)})
                           (:repos cfg))
        ;; The run store belongs to one repository, and it is the one named by
        ;; :runs-repo -- keyed on the path rather than on the repo's label, so
        ;; renaming or re-pointing a repo cannot silently empty this section.
        runs-commits (or (some #(when (= (:runs-repo cfg) (:path %)) (:commits %))
                               repo-commits)
                         [])
        deltas (mapv #(board-delta % date) (:boards cfg))
        joe (vec (concat (mapcat waits-on-joe (:boards cfg))
                         (tripwire-discharges (:tripwire-root cfg))))
        surfaced-untriaged (untriaged-traces (:trace-root cfg)
                                             (:trace-review-ledger cfg))
        deposits (ledger-deposits (:run-era-ledger cfg) date)
        adopted (machine-adopted (:registry cfg) date)
        runs (experiments cfg runs-commits)]
    {:date date
     :repo-commits repo-commits
     :deltas deltas
     :waits-on-joe joe
     :surfaced-untriaged surfaced-untriaged
     :deposits deposits
     :adopted adopted
     :experiments runs
     :counts {:commits (reduce + 0 (map (comp count :commits) repo-commits))
              :worklist-moves (reduce + 0 (map (comp count :moved) deltas))
              :worklist-added (reduce + 0 (map (comp count :added) deltas))
              :ledger-deposits (count deposits)
              :machine-adopted (count adopted)
              :experiments (count runs)
              :waits-on-joe (count joe)}}))

;; ----------------------------------------------------------------- rendering

(defn- plural [n one many]
  (let [n (or n 0)] (str n " " (if (= 1 n) one many))))

(defn- clip [n s]
  (let [s (str/replace (str s) #"\s+" " ")]
    (if (> (count s) n) (str (subs s 0 n) "...") s)))

(defn- section [title lines empty-line]
  (concat [(str "## " title) ""]
          (if (seq lines) lines [empty-line])
          [""]))

(defn- commit-lines [{:keys [repo commits]}]
  (if (nil? commits)
    [(str "- **" repo "**: repository not readable -- no commits reported.")]
    (cons (str "### " repo " -- " (count commits)
               (if (= 1 (count commits)) " commit" " commits"))
          (if (empty? commits)
            ["" (str "No commits in " repo " on this date.")]
            (concat
             [""]
             (mapcat (fn [{:keys [area commits extra-areas]}]
                       (concat
                        [(str "**" area "** (" (count commits) ")"
                              (when (seq extra-areas)
                                (str " -- also touching "
                                     (str/join ", " extra-areas))))]
                        (for [c commits]
                          (str "- `" (:sha c) "` " (clip 110 (:subject c))))
                        [""]))
                     (by-area commits)))))))

(defn- delta-lines [{:keys [board base readable? added moved gone rows]}]
  (if-not readable?
    [(str "- **" board "**: board not readable -- no delta reported.")]
    (concat
     [(str "- **" board "** (" rows " rows) against "
           (if base
             (str "`" (subs base 0 (min 8 (count base))) "`")
             "no commit before this date, so every row reads as added"))]
     (for [{:keys [id from to]} moved]
       (str "  - `" id "`: " from " -> " to))
     (for [{:keys [id status]} added]
       (str "  - `" id "`: added, " status))
     (for [{:keys [id status]} gone]
       (str "  - `" id "`: no longer on the board (was " status ")"))
     (when (and (empty? moved) (empty? added) (empty? gone))
       ["  - no row changed status."]))))

(defn- deposit-lines [deposits]
  (for [r deposits]
    (str "- `" (:row/run-id r) "` / `" (:row/check-id r) "` -> **"
         (:row/verdict r) "** (seq " (:row/seq r) ", " (:row/artifact r) ")"
         (when (= :typed-absence (:row/verdict r))
           (str " -- " (clip 160 (:row/notes r)))))))

(defn- adopted-lines [adopted]
  (mapcat (fn [{:keys [choice row by grounds decision reversal]}]
            [(str "- **`" choice "`** (row " row ", adopted by " by
                  ", grounds " grounds ")")
             (str "  - decision: " (clip 320 decision))
             (str "  - reversal: "
                  (if reversal
                    (clip 320 reversal)
                    (str "**none recorded in the entry** -- the reversal path "
                         "is not stated.")))])
          adopted))

(defn- experiment-lines [runs]
  (for [{:keys [run commits outcome]} runs]
    (str "- `" run "` (" (str/join ", " (map #(str "`" % "`") commits)) ") -- "
         (if outcome
           (clip 200 outcome)
           (str "no RECEIPT.md or README.md in the directory, so there is no "
                "outcome line to quote")))))

(defn- joe-lines [rows]
  (for [{:keys [board id status class statement]} rows]
    (str "- `" id "` (" board ", class " class ", " status ") -- "
         (clip 200 statement))))

(defn render
  "The bulletin's bytes. A pure function of `facts`: no clock and no HEAD sha,
  so regenerating on an unchanged tree produces the same file."
  [{:keys [date repo-commits deltas deposits adopted experiments waits-on-joe
           surfaced-untriaged counts]}]
  (str/join
   "\n"
   (concat
    [(str "# Morning bulletin -- " date)
     ""
     (str "What the machine did on " date ", read from the repositories and "
          "registries named at the foot of this file. Every entry carries a "
          "commit sha, a run id, or a path; a section with nothing in it says "
          "so rather than disappearing.")
     ""
     (str "Machine-readable twin: morning-brief item `bulletin-" date
          "` under `" brief/default-root "/items/`. Its two typed questions --"
          " *was this the best available policy selection?* and *did the result"
          " substantively advance the selected target?* -- are answered with"
          " `futon2.aif.morning-brief/review!`, the review path that already"
          " exists; no new reader of verdicts was written. The item is"
          " immutable and was minted at this day's first stop, so its"
          " `-at-queue` fields are a snapshot; THIS FILE is the day's current"
          " record.")
     ""
     (str "**Counts.** "
          (plural (:commits counts) "commit" "commits") ", "
          (plural (:worklist-moves counts) "worklist status move"
                  "worklist status moves") ", "
          (plural (:worklist-added counts) "row added" "rows added") ", "
          (plural (:ledger-deposits counts) "run-era deposit"
                  "run-era deposits") ", "
          (plural (:machine-adopted counts) "machine-adopted decision"
                  "machine-adopted decisions") ", "
          (plural (:experiments counts) "run directory written"
                  "run directories written") ", "
          (plural (:waits-on-joe counts) "item waiting on Joe"
                  "items waiting on Joe") ".")
     ""]
    (section "What was accomplished -- commits"
             (mapcat commit-lines repo-commits)
             "No commits in any watched repository on this date.")
    (section "Worklist deltas"
             (mapcat delta-lines deltas)
             "No board was readable.")
    (section "Run-era ledger deposits"
             (deposit-lines deposits)
             "No row was deposited into run-era-ledger.edn on this date.")
    (section "Machine-adopted decisions, and how to reverse each"
             (adopted-lines adopted)
             "No choice was adopted by the machine on this date.")
    (section "Experiments run"
             (experiment-lines experiments)
             (str "No run directory under holes/labs/wm-contract/runs was "
                  "written on this date."))
    (section "Decision sheet -- what waits on Joe"
             (joe-lines waits-on-joe)
             "Nothing on the watched boards is waiting on the operator.")
    (section "Surfaced TRACE records awaiting triage"
             [(str "- **" (or (:count surfaced-untriaged) 0)
                   " surfaced-untriaged**; oldest date: **"
                   (or (:oldest-date surfaced-untriaged) "none") "**.")]
             "- **0 surfaced-untriaged**; oldest date: **none**.")
    ["## Sources read"
     ""
     (str "- repositories: "
          (str/join ", " (map #(str "`" (:path %) "`") repo-commits)))
     (str "- boards: the `worklist.edn` of each watched lab, diffed against "
          "the last commit before this date")
     "- run-era ledger: `holes/labs/wm-contract/run-era-ledger.edn`"
     "- choices registry: `holes/labs/wm-contract/aif-equations.edn` `:choices`"
     "- run store: `holes/labs/wm-contract/runs/`"
     ""
     (str "Generated by `futon2.aif.bulletin/generate!` "
          "(`src/futon2/aif/bulletin.clj`). The generator writes this file "
          "only when its bytes change, so an unchanged day leaves the file "
          "and its mtime alone.")
     ""])))

;; ----------------------------------------------------------------- producing

(defn item-for
  "The morning-brief item that carries the bulletin's counts into the review
  path.

  It deliberately carries no `:outcome` and no `:failure`, so
  `morning-brief/item-objectives` yields exactly the two questions that apply
  to a day's work; adding an `:outcome` would also raise `:machine-response`,
  which asks whether a failed click stopped the line and has nothing to say
  about a digest.

  `:qa-targets :achievement :entity-id` is nil, as it is in 57 of the 72 items
  the store held before this producer was written, and that is a boundary
  rather than an omission. The A-matrix belief domain is the 414 section ids of
  `futon5a/holes/stack-annotations.edn` (`src/futon2/aif/belief.clj:489-508`),
  and `apply-morning-brief-events` HOLDS -- rather than applies -- any event
  whose entity is outside that domain
  (`scripts/futon2/report/war_machine.clj:829-845`). A day of ledger work is
  not one of those entities, so an invented id would mint an event held
  forever. With the id nil, `belief-event-for` mints no event at all
  (`src/futon2/aif/morning_brief.clj:174-186`) and the typed verdict is
  recorded without a claim on A -- which is what `:substantive-achievement`'s
  own spec says it does when no grounded entity target exists. Pass
  `:achievement-entity-id` to bind one when a grounded target exists.

  How far that channel is from carrying anything, measured on the store rather
  than assumed: of the 15 pre-existing items that DO carry an entity-id, 0 name
  an id in the 414-id domain -- every event they could mint would be held --
  and all 4 reviews ever recorded are `:feature-verdict`, which mints no event
  by construction. So no belief event has yet reached A from this store, with
  or without the bulletin. Binding a real grounded target is the work that
  would change that, and it is not this row's."
  [{:keys [date counts waits-on-joe adopted experiments]} bulletin-rel entity-id]
  {:attempt-id (str "bulletin-" date)
   :author "futon2.aif.bulletin"
   :selected-target (str "the day's work, " date)
   :bulletin/date date
   :bulletin/file bulletin-rel
   ;; Named for WHEN they were taken. The item is immutable and is minted at
   ;; the day's FIRST stop; the bulletin file keeps being regenerated at every
   ;; later stop, so these numbers are a snapshot and the file at
   ;; :bulletin/file is the day's current record. An unqualified :counts here
   ;; would read as the day's total and quietly stop being one.
   :bulletin/counts-at-queue counts
   :bulletin/waits-on-joe-at-queue (mapv :id waits-on-joe)
   :bulletin/machine-adopted-at-queue (mapv :choice adopted)
   :bulletin/experiments-at-queue (mapv :run experiments)
   :qa-targets {:selection {:policy nil}
                :achievement {:entity-id entity-id}}})

(defn- queue-once!
  "Queue the day's item unless it is already there. The store is append-only,
  so a regenerated bulletin does not mint a second item for the same day."
  [root item]
  (let [f (io/file root "items" (str (:attempt-id item) ".edn"))]
    (if (.exists f)
      {:status :already-queued :attempt-id (:attempt-id item) :ref (.getPath f)}
      {:status :queued :attempt-id (:attempt-id item)
       :ref (brief/queue-item! root item)})))

(defn generate!
  "Write the bulletin for `:date` (default today) and queue its morning-brief
  item. Returns what it did; writes the file only when its bytes changed."
  ([] (generate! {}))
  ([overrides]
   (let [cfg (merge default-config overrides)
         date (or (:date cfg) (str (LocalDate/now)))
         facts (collect-facts cfg date)
         file (io/file (:out-dir cfg) (str "BULLETIN-" date ".md"))
         bulletin-rel (str (:bulletin-rel-dir cfg) "/" (.getName file))
         text (render facts)
         changed? (or (not (.exists file)) (not= text (slurp file)))]
     (when changed?
       (io/make-parents file)
       (spit file text))
     {:date date
      :file (.getPath file)
      :rewritten? changed?
      :counts (:counts facts)
      :item (queue-once! (:brief-root cfg)
                         (item-for facts bulletin-rel
                                   (:achievement-entity-id cfg)))})))

(defn -main
  "CLI: `clojure -M -m futon2.aif.bulletin [YYYY-MM-DD]`."
  [& args]
  (let [date (first args)
        result (generate! (if date {:date date} {}))]
    (println (str "bulletin " (:date result) ": " (:file result)
                  (if (:rewritten? result) " (written)" " (unchanged)")))
    (println (str "  counts " (pr-str (:counts result))))
    (println (str "  item " (name (:status (:item result))) " "
                  (:attempt-id (:item result))))
    (flush)))
