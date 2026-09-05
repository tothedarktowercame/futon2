#!/usr/bin/env bb
;; U55 -- the record half of the stepper. `wm_step.sh` is the operator surface;
;; this is every operation on EDN and on bytes that the shell would have had to
;; do badly: content hashes, the pin manifest, the world census, the step's
;; trace delta, the normalisation the determinism control compares, and the
;; planted-divergence perturbation the control needs to be non-vacuous.
;;
;;   bb wm_step_records.bb manifest  <dir> <out.edn>
;;   bb wm_step_records.bb verify     <dir> <manifest.edn>      ; exit 0 ok, 4 drift
;;   bb wm_step_records.bb verify-dirs <a-dir> <b-dir>           ; exit 0 ok, 4 drift
;;   bb wm_step_records.bb world     <out.edn>
;;   bb wm_step_records.bb delta     <pin-trace-dir> <sandbox-trace-dir> <out.edn>
;;   bb wm_step_records.bb normalize <in.edn> <out.edn>         ; prints the sha
;;   bb wm_step_records.bb compare   <a.edn> <b.edn> <out.edn>  ; exit 0 same, 5 divergent
;;   bb wm_step_records.bb plant     <trace-dir> mu-uniform     ; perturb a PINNED input
;;
;; READ-ONLY except for the named out-file, and `plant`, which is the one
;; subcommand that writes into a pin and says so in its own receipt.
;;
;; WHAT "BYTE-IDENTICAL ON THE DECISION RECORDS" CAN MEAN, and why it is not
;; literal file bytes. C509's handoff item 4 fixes the exclusion list: a tick
;; mints a fresh `:run/id` (run_tick_once.clj:268), stamps `:timestamp` /
;; `:startedAt`, and `route-tag` stamps `:at` on each of the nine route hops
;; (war_machine.clj:5836-5841). Those five are excluded, and NOTHING else is.
;; Everything the tick decided -- the ranking, every controller score, the
;; chosen action, the posterior, the precision state -- is inside the compared
;; form. The comparison is a SHA over a canonical serialisation (maps emitted in
;; sorted-key order, sets sorted), so two records that differ anywhere outside
;; the exclusion list produce different shas.
;;
;; The exclusion is applied by PATH, not by key name alone: `:at` is dropped
;; only inside a route (`:wm/route`, `:route`), never wherever else it appears,
;; so an evidence timestamp that moved is still a difference the compare sees.

(require '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn die! [code & msg]
  (binding [*out* *err*] (println (str/join " " msg)))
  (System/exit code))

;; ---------------------------------------------------------------------------
;; bytes
;; ---------------------------------------------------------------------------

(defn- hex [^bytes bs]
  (str/join (map #(format "%02x" (bit-and % 0xff)) bs)))

(defn sha256-file [path]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        buf (byte-array 1048576)]
    (with-open [in (io/input-stream (io/file path))]
      (loop []
        (let [n (.read in buf)]
          (when (pos? n) (.update md buf 0 n) (recur)))))
    (hex (.digest md))))

(defn sha256-str [^String s]
  (hex (.digest (java.security.MessageDigest/getInstance "SHA-256")
                (.getBytes s "UTF-8"))))

(defn- trace-files [dir]
  ;; Exactly the set `futon2.aif.trace/trace-files` folds (trace.clj:778-786):
  ;; wm-trace-YYYY-MM-DD.edn and nothing else. The lane-futility index is
  ;; DERIVED (lane_futility.clj:82-86, rebuilt when its fingerprint goes stale)
  ;; and .run-lock belongs to a process, so neither is pinned -- C509 W2/W7.
  (->> (.listFiles (io/file dir))
       (filter #(.isFile ^java.io.File %))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName ^java.io.File %)))
       (sort-by #(.getName ^java.io.File %))))

(defn manifest [dir]
  (let [fs (trace-files dir)]
    {:manifest/dir (str (.getAbsolutePath (io/file dir)))
     :manifest/at (str (java.time.Instant/now))
     :manifest/files (mapv (fn [^java.io.File f]
                             {:name (.getName f)
                              :bytes (.length f)
                              :sha256 (sha256-file (.getPath f))})
                           fs)
     :manifest/file-count (count fs)
     :manifest/total-bytes (reduce + 0 (map #(.length ^java.io.File %) fs))
     :manifest/excluded
     ["\\.run-lock -- holder identity (pid, pid-start-ms, token); replaying it re-asserts a dead holder (C509 W7)"
      "\\.lane-futility-index.edn (+ .lock, .tmp) -- derived from the corpus and rebuilt when its (name,length,mtime) fingerprint is stale (C509 W2)"
      "everything not matching wm-trace-YYYY-MM-DD.edn -- the trace reader folds nothing else"]}))

(defn- file-hashes [dir]
  (into {} (map (fn [^java.io.File f]
                  [(.getName f) {:name (.getName f) :bytes (.length f)
                                 :sha256 (sha256-file (.getPath f))}]))
        (trace-files dir)))

(defn verify-dirs
  "Content-hash equality of two corpora. This is the question `reset` asks --
   is the RESTORED state the pinned state? -- and it is deliberately not the
   question `step` asks of the pin, which is whether the pin still matches the
   manifest taken when the pin was made. Collapsing the two makes a legitimately
   advanced pin look like drift."
  [a-dir b-dir]
  (let [a (file-hashes a-dir) b (file-hashes b-dir)
        missing (sort (remove (set (keys b)) (keys a)))
        added (sort (remove (set (keys a)) (keys b)))
        changed (sort (keep (fn [[n x]] (when-let [y (get b n)]
                                          (when (not= (:sha256 x) (:sha256 y)) n)))
                            a))
        drift? (boolean (or (seq missing) (seq added) (seq changed)))]
    {:verify/a (str (.getAbsolutePath (io/file a-dir)))
     :verify/b (str (.getAbsolutePath (io/file b-dir)))
     :verify/files-checked (count a)
     :verify/missing (vec missing)
     :verify/added (vec added)
     :verify/changed (vec changed)
     :verify/verdict (if drift? :drift :identical)}))

(defn verify [dir manifest-path]
  (let [m (edn/read-string read-opts (slurp manifest-path))
        want (into {} (map (juxt :name identity)) (:manifest/files m))
        have (into {} (map (fn [^java.io.File f]
                             [(.getName f) {:name (.getName f)
                                            :bytes (.length f)
                                            :sha256 (sha256-file (.getPath f))}]))
                   (trace-files dir))
        missing (sort (remove (set (keys have)) (keys want)))
        added (sort (remove (set (keys want)) (keys have)))
        changed (sort (keep (fn [[n w]]
                              (when-let [h (get have n)]
                                (when (not= (:sha256 w) (:sha256 h)) n)))
                            want))
        drift? (boolean (or (seq missing) (seq added) (seq changed)))]
    {:verify/dir (str (.getAbsolutePath (io/file dir)))
     :verify/manifest manifest-path
     :verify/files-checked (count want)
     :verify/missing (vec missing)
     :verify/added (vec added)
     :verify/changed (vec changed)
     :verify/verdict (if drift? :drift :identical)}))

;; ---------------------------------------------------------------------------
;; the world the pin cannot redirect
;; ---------------------------------------------------------------------------

(def home (System/getProperty "user.home"))

(def world-files
  "C509 item R7's thirteen named cross-repo inputs (resolved to the paths the
   cited sites build), plus R6's mana snapshot, whose AGE and not only whose
   content decides the tick's mode. A step cannot redirect any of them -- they
   are unparameterised literals or user.home anchors -- so the stepper HASHES
   them instead and a comparison can attribute a divergence to one."
  [[:stack-annotations (str home "/code/futon5a/holes/stack-annotations.edn") "src/futon2/aif/belief.clj:475-476"]
   [:sorrys (str home "/code/futon2/resources/sorrys.edn") "src/futon2/aif/sorry_registry.clj:38-40"]
   [:calendar-events (str home "/code/calendar/events.edn") "src/futon2/aif/anticipation.clj:24-25"]
   [:strategic-vocabulary (str home "/code/futon5a/data/war-machine-strategic-vocabulary.edn") "scripts/futon2/report/war_machine.clj:696-698"]
   [:capability-star-map (str home "/code/futon0/holes/missions/M-capability-star-map.graph.edn") "scripts/futon2/report/war_machine.clj:718-719"]
   [:mission-fold-view (str home "/code/futon6/data/mission-fold-view.edn") "scripts/futon2/report/war_machine.clj:734-736"]
   [:mission-domain-ratified (str home "/code/futon6/data/mission-domain-ratified.edn") "scripts/futon2/report/war_machine.clj:734-736"]
   [:forward-model-centrality (str home "/code/futon7/holes/M-futon-forward-model.centrality.json") "scripts/futon2/report/war_machine.clj:737-740"]
   [:forward-model-roi (str home "/code/futon7/holes/M-futon-forward-model.roi-results.edn") "scripts/futon2/report/war_machine.clj:737-740"]
   [:zaif-s4-ingest (str home "/code/futon2/holes/labs/zaif-harness/runs/S4-identify-ingest.edn") "scripts/futon2/report/war_machine.clj:1916-1920"]
   [:curvature-artifact "/home/joe/code/futon3c/holes/missions/M-substrate-metric.R2-curvature-full.json" "src/futon2/aif2/tension.clj:178-182"]
   [:holes-contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json" "src/futon2/aif/selection_rationale.clj:59-63"]
   [:control-map "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn" "scripts/futon2/run_tick_once.clj:182-183"]
   [:c-vector-mess (str home "/code/futon6/data/c-vector/c-entries.mess.edn") "src/futon2/aif/c_vector.clj:218-224"]
   [:c-vector-incomplete (str home "/code/futon6/data/c-vector/c-entries.incomplete.edn") "src/futon2/aif/c_vector.clj:218-224"]
   [:c-vector-yingvoice (str home "/code/futon6/data/c-vector/c-entries.yingvoice.edn") "src/futon2/aif/c_vector.clj:218-224"]
   [:mana-snapshot (str home "/code/storage/futon0/mana-snapshot.json") "scripts/futon2/report/war_machine.clj:4055-4062,4159-4164"]])

(def commit-census-repos
  "`all-repos` verbatim (war_machine.clj:1041-1061), because the tick's
   observation is built from a git census over exactly this list."
  [["futon0" (str home "/code/futon0") :stack]
   ["futon1" (str home "/code/futon1") :stack]
   ["futon1a" (str home "/code/futon1a") :stack]
   ["futon2" (str home "/code/futon2") :stack]
   ["futon3" (str home "/code/futon3") :stack]
   ["futon3a" (str home "/code/futon3a") :stack]
   ["futon3b" (str home "/code/futon3b") :stack]
   ["futon3c" (str home "/code/futon3c") :stack]
   ["futon4" (str home "/code/futon4") :stack]
   ["futon5" (str home "/code/futon5") :stack]
   ["futon5a" (str home "/code/futon5a") :stack]
   ["futon6" (str home "/code/futon6") :mathematics]
   ["futon7" (str home "/code/futon7") :portfolio]
   ["futon7a" (str home "/code/futon7a") :portfolio]
   ["vsat" (str home "/vsat") :consulting]
   ["vsat.wiki" (str home "/vsat.wiki") :consulting]
   ["npt" (str home "/npt") :consulting]
   ["ukrn-services-simulation" (str home "/code/ukrn-services-simulation") :consulting]])

(defn commit-census
  "The workstream commit census the tick's observation is built from:
   `count-commits-since` over `all-repos` with `since-str days`
   (war_machine.clj:2781-2784,2794-2801,3652-3668), folded into the four
   :*-pct observation channels at :3828-3836 and read at
   observation.clj:53-56.

   It is here because a step DIVERGED on exactly these four channels while
   every hashed file input was unchanged: between two steps ten minutes apart
   the census total moved by one commit, and nothing in the step record said
   so. An input a comparison cannot see is an input a reader will explain
   wrongly."
  [days]
  (let [since (str (.minusDays (java.time.LocalDate/now (java.time.ZoneId/of "Europe/London")) (long days)))
        rows (vec (for [[label path ws] commit-census-repos
                        :when (.isDirectory (io/file path))]
                    (let [r (try (process/shell {:dir path :out :string :err :string :continue true}
                                                "git" "log" "--oneline" "--since" since)
                                 (catch Exception e {:exit 1 :out "" :err (str e)}))
                          n (if (zero? (:exit r))
                              (count (remove str/blank? (str/split-lines (str (:out r)))))
                              0)]
                      {:repo label :workstream ws :commits n})))
        by-ws (reduce (fn [m {:keys [workstream commits]}] (update m workstream (fnil + 0) commits)) {} rows)
        total (max 1 (reduce + 0 (vals by-ws)))]
    {:since since
     :days days
     :repos rows
     :by-workstream by-ws
     :total total
     :percentages (into {} (map (fn [[k v]] [k (/ (double v) total)])) by-ws)
     :where "war_machine.clj:2794-2801,3652-3668,3828-3836; observation.clj:53-56"}))

;; ---------------------------------------------------------------------------
;; U57 -- the flip-readiness capture
;; ---------------------------------------------------------------------------

(def flip-readiness-sources
  "The six files `flip_readiness_check.bb` reads at :69-74, each with the
   environment variable that overrides it (:69-75) and the repo it lives in.
   `world-files` above is a DIFFERENT set -- C509 R7's tick inputs -- and only
   one of these six is on it (:holes-contract, :167). The check derives its six
   lines from the state of these files at the moment of asking, so a run that
   does not pin them cannot have its verdict re-derived later; U56 measured that
   as the :flip-readiness absence's elaborate leg (C511 section 1).

   Each entry is [id path env-var repo where]. `env-var` is recorded, not used
   here: it is what lets an as-of derivation reach the check through the check's
   own override seam instead of a hand-assembled env block."
  [[:catalog    (str home "/code/futon2/holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn")
    "CATALOG"       "futon2"   "flip_readiness_check.bb:69"]
   [:accounting (str home "/code/futon2/holes/labs/wm-contract/variable-situation-accounting.edn")
    "ACCOUNTING"    "futon2"   "flip_readiness_check.bb:70"]
   [:hole-audit (str home "/code/futon2/holes/labs/wm-contract/runs/U27-hole-closability/audit.edn")
    "HOLE_AUDIT"    "futon2"   "flip_readiness_check.bb:71"]
   [:tally      (str home "/code/p4ng/empirics-futon/defect-repair-tally.edn")
    "TALLY"         "p4ng"     "flip_readiness_check.bb:72"]
   [:receipt    (str home "/code/p4ng/empirics-futon/wm-status-receipt.json")
    "RECEIPT"       "p4ng"     "flip_readiness_check.bb:73"]
   [:contract   (str home "/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
    "CONTRACT_JSON" "mathlib4" "flip_readiness_check.bb:74"]])

(def per-node-test-repos
  "`repo-dir` verbatim (flip_readiness_check.bb:205), the repos whose live git
   the :per-node-tests line reads with no override: `moved-since-head`
   (:215-226) is `git diff --name-only <catalog-head> HEAD` unioned with
   `git status --porcelain`, both evaluated at NOW. Capturing each repo's HEAD
   and its porcelain list is the as-of input that line has never had."
  [[:futon2 (str home "/code/futon2")]
   [:futon3c (str home "/code/futon3c")]])

(defn- git-out
  "stdout of a git command in `dir`, trimmed; nil if the command failed or the
   directory is not there. Failure is nil rather than an exception because a
   sibling repo that is absent is a fact the record should carry, not a crash."
  [dir & argv]
  (when (.isDirectory (io/file dir))
    (let [r (try (apply process/shell {:dir dir :out :string :err :string :continue true} argv)
                 (catch Exception e {:exit 1 :out "" :err (str e)}))]
      (when (zero? (:exit r)) (str/trim (str (:out r)))))))

(defn- porcelain-lines [dir]
  (vec (remove str/blank? (str/split-lines (or (git-out dir "git" "status" "--porcelain") "")))))

(defn- source-identity
  "One captured source: its content hash NOW, plus the git identity that lets
   the bytes be fetched back. The two are recorded together on purpose -- the
   commit says where to look and the sha256 says whether what came back is what
   the run read. A repo sha alone would not have caught the wrong-commit
   extraction C511 section 1 records (`69721b12` vs `4bbc7111`)."
  [[id path env repo where]]
  (let [f (io/file path)
        root (str home "/code/" repo)
        rel (when (str/starts-with? path (str root "/")) (subs path (inc (count root))))
        last-commit (when rel (git-out root "git" "log" "-1" "--format=%H" "--" rel))
        committed-blob (when (and rel last-commit)
                         (git-out root "git" "rev-parse" (str last-commit ":" rel)))
        worktree-blob (when (.isFile f) (git-out root "git" "hash-object" "--" path))]
    (merge {:id id :path path :env env :where where :exists? (.exists f)
            :repo repo :repo-rel rel
            :repo-root root
            :repo-head (git-out root "git" "rev-parse" "HEAD")
            :last-commit last-commit
            :last-commit-at (when (and rel last-commit)
                              (git-out root "git" "log" "-1" "--format=%cI" last-commit))
            :committed-blob-sha1 committed-blob
            :worktree-blob-sha1 worktree-blob
            ;; The one field an as-of resolver branches on: false means the bytes
            ;; the run read are in no commit, so `git show` cannot return them and
            ;; only an unmoved worktree can.
            :worktree-matches-commit? (boolean (and committed-blob worktree-blob
                                                    (= committed-blob worktree-blob)))}
           (when (.isFile f)
             {:bytes (.length f) :sha256 (sha256-file path) :mtime-ms (.lastModified f)}))))

(defn flip-readiness-capture
  "Everything `flip_readiness_check.bb` reads from outside its own code, pinned:
   the six sources by content hash and git identity, the two repos the
   :per-node-tests line reads live git in, and the one `git log` the
   :contract-pin line runs (:111) -- which is a source too, and is not a file."
  []
  {:sources (mapv source-identity flip-readiness-sources)
   :per-node-git (vec (for [[repo-kw root] per-node-test-repos]
                        {:repo repo-kw :root root
                         :head (git-out root "git" "rev-parse" "HEAD")
                         :porcelain (porcelain-lines root)
                         :where "flip_readiness_check.bb:215-226"}))
   :holes-lean-last-commit
   {:repo "mathlib4"
    :path "DarkTower/WarMachine/Holes.lean"
    :commit (git-out (str home "/code/mathlib4") "git" "log" "-1" "--format=%H"
                     "--" "DarkTower/WarMachine/Holes.lean")
    :where "flip_readiness_check.bb:111 -- the comparand C175 settled; NOT mathlib HEAD"}
   :where "flip_readiness_check.bb:69-75,111,215-226"
   :consumed-by "bb flip_readiness_check.bb --as-of <world-record.edn>"})

(defn world []
  (let [now (System/currentTimeMillis)
        mana (io/file (str home "/code/storage/futon0/mana-snapshot.json"))]
    {:world/at (str (java.time.Instant/now))
     :world/files
     (mapv (fn [[k path where]]
             (let [f (io/file path)]
               (merge {:id k :path path :where where :exists? (.exists f)}
                      (when (.exists f)
                        {:bytes (.length f) :sha256 (sha256-file path)
                         :mtime-ms (.lastModified f)}))))
           world-files)
     ;; C509 T5, the sharpest way a reset lies: :stale? is age > 60 min over the
     ;; mana snapshot's MTIME (war_machine.clj:4159-4164), consumed at :5936-5949
     ;; to suppress a :stop-the-line override. A byte-exact restore 61 minutes
     ;; later runs the tick in a different MODE. A step cannot pin this, so it
     ;; records the age it saw and which side of the threshold it was on.
     :world/mana-age
     (when (.exists mana)
       (let [age-min (/ (double (- now (.lastModified mana))) 60000.0)]
         {:age-min age-min
          :threshold-min 60.0
          :stale? (> age-min 60.0)
          :where "scripts/futon2/report/war_machine.clj:4159-4164,5936-5949"}))
     ;; C509 T3/T4: the one wall-clock read that enters the score is
     ;; wm-time-pressure (war_machine.clj:6250-6252). It is 0.0 whenever no
     ;; anticipated event is inside the horizon, so the step records the
     ;; CONDITION under which its determinism claim holds rather than asserting
     ;; the read is harmless.
     :world/commit-census (commit-census (Long/parseLong (or (System/getenv "FUTON_WM_STEP_DAYS") "14")))
     ;; U57. The six sources flip_readiness_check.bb:69-74 reads, plus the two
     ;; repos its :per-node-tests line reads live git in and the Holes.lean
     ;; `git log` its :contract-pin line runs. Captured BEFORE the tick, beside
     ;; the rest of the world, so `--as-of` can re-derive the run's own
     ;; flip-readiness verdict instead of the tree's verdict at reading time.
     :world/flip-readiness (flip-readiness-capture)
     :world/anticipation
     (let [p (str home "/code/calendar/events.edn")
           f (io/file p)]
       (if (.exists f)
         (let [forms (try (edn/read-string read-opts (slurp p)) (catch Exception e {:read-error (str e)}))
               evs (cond (map? forms) (or (:events forms) []) (sequential? forms) forms :else [])]
           {:path p :event-forms (count evs)
            :note "time-pressure is 0.0 when no event parses into the future inside the horizon (anticipation.clj:99-122); the tick's own record carries the realised value"})
         {:path p :exists? false}))}))

;; ---------------------------------------------------------------------------
;; the step's own records
;; ---------------------------------------------------------------------------

(defn read-forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [acc []]
      (let [f (edn/read (assoc read-opts :eof ::eof) r)]
        (if (= ::eof f) acc (recur (conj acc f)))))))

(defn- today-files [dir]
  (into {} (map (fn [^java.io.File f] [(.getName f) (.getPath f)])) (trace-files dir)))

(defn delta
  "The records the step APPENDED: every record in the sandbox corpus whose
   `:run/id` is in no pinned file. Selection is by run id, not by count or by
   timestamp, so a concurrent writer could not widen it."
  [pin-dir sandbox-dir]
  (let [pin (today-files pin-dir)
        sand (today-files sandbox-dir)
        new-files (sort (remove (set (keys pin)) (keys sand)))
        grown (sort (keep (fn [[n p]]
                            (when-let [pp (get pin n)]
                              (when (not= (.length (io/file p)) (.length (io/file pp))) n)))
                          sand))
        touched (concat new-files grown)
        pinned-ids (set (mapcat #(keep :run/id (read-forms (get pin %))) grown))
        added (vec (for [n touched
                         r (read-forms (get sand n))
                         :when (not (contains? pinned-ids (:run/id r)))]
                     r))]
    {:delta/pin-dir pin-dir
     :delta/sandbox-dir sandbox-dir
     :delta/new-files (vec new-files)
     :delta/grown-files (vec grown)
     :delta/added-records (count added)
     :delta/run-ids (vec (keep :run/id added))
     :delta/records added}))

;; ---------------------------------------------------------------------------
;; normalisation and comparison
;; ---------------------------------------------------------------------------

(def drop-anywhere
  "C509 handoff item 4. Everything a tick mints fresh per run and nothing else."
  #{:run/id :timestamp :startedAt})

(def route-key? #{:wm/route :route})
(def route-time-key? #{:at :at_})

(defn scrub
  "Drop the exclusion list. IN-ROUTE? is carried down so `:at` is dropped only
   under a route; anywhere else an `:at` is data and a difference in it is a
   difference."
  [x in-route?]
  (cond
    (map? x) (into {} (keep (fn [[k v]]
                              (cond
                                (contains? drop-anywhere k) nil
                                (and in-route? (contains? route-time-key? k)) nil
                                :else [k (scrub v (or in-route? (contains? route-key? k)))])))
                   x)
    (vector? x) (mapv #(scrub % in-route?) x)
    (set? x) (into #{} (map #(scrub % in-route?)) x)
    (seq? x) (mapv #(scrub % in-route?) x)
    :else x))

(defn canon
  "A canonical serialisation: maps in sorted-key order, sets sorted. Two forms
   with the same content have the same string whatever order they were built in."
  [x]
  (let [sb (StringBuilder.)]
    ((fn go [v]
       (cond
         (map? v) (do (.append sb "{")
                      (doseq [[k val] (sort-by (comp pr-str key) v)]
                        (.append sb (pr-str k)) (.append sb " ") (go val) (.append sb ", "))
                      (.append sb "}"))
         (set? v) (do (.append sb "#{")
                      (doseq [e (sort-by pr-str v)] (go e) (.append sb " "))
                      (.append sb "}"))
         (or (vector? v) (seq? v)) (do (.append sb "[")
                                       (doseq [e v] (go e) (.append sb " "))
                                       (.append sb "]"))
         :else (.append sb (pr-str v))))
     x)
    (str sb)))

(defn normalize [in-path]
  (let [forms (read-forms in-path)
        scrubbed (mapv #(scrub % false) forms)
        strings (mapv canon scrubbed)]
    {:normalized/source in-path
     :normalized/forms (count forms)
     :normalized/excluded {:anywhere (vec (sort drop-anywhere))
                           :in-route (vec (sort route-time-key?))
                           :where "C509-inter-tick-state-boundary.md, handoff item 4"}
     :normalized/per-form-sha256 (mapv sha256-str strings)
     :normalized/sha256 (sha256-str (str/join "\n" strings))
     :normalized/forms-data scrubbed}))

(defn- diff-paths
  "Where two scrubbed forms differ, to a bounded depth, as a vector of paths."
  [a b path depth acc]
  (cond
    (= a b) acc
    (>= depth 4) (conj acc {:path path :why :differs
                            :a-type (str (type a)) :b-type (str (type b))})
    (and (map? a) (map? b))
    (let [ks (sort-by pr-str (distinct (concat (keys a) (keys b))))]
      (reduce (fn [acc k]
                (if (= (get a k ::missing) (get b k ::missing))
                  acc
                  (diff-paths (get a k ::missing) (get b k ::missing)
                              (conj path k) (inc depth) acc)))
              acc ks))
    (and (vector? a) (vector? b))
    (if (not= (count a) (count b))
      (conj acc {:path path :why :different-length :a (count a) :b (count b)})
      (reduce (fn [acc i]
                (if (= (nth a i) (nth b i))
                  acc
                  (diff-paths (nth a i) (nth b i) (conj path i) (inc depth) acc)))
              acc (range (count a))))
    :else (conj acc {:path path :why :differs
                     :a (let [s (pr-str a)] (subs s 0 (min 120 (count s))))
                     :b (let [s (pr-str b)] (subs s 0 (min 120 (count s))))})))

(defn compare-normalized [a-path b-path]
  (let [a (edn/read-string read-opts (slurp a-path))
        b (edn/read-string read-opts (slurp b-path))
        same? (= (:normalized/sha256 a) (:normalized/sha256 b))
        fa (:normalized/forms-data a)
        fb (:normalized/forms-data b)
        diffs (when-not same?
                (if (not= (count fa) (count fb))
                  [{:path [] :why :different-form-count :a (count fa) :b (count fb)}]
                  (vec (take 40 (mapcat #(diff-paths %1 %2 [] 0 []) fa fb)))))]
    {:compare/a a-path
     :compare/b b-path
     :compare/a-sha256 (:normalized/sha256 a)
     :compare/b-sha256 (:normalized/sha256 b)
     :compare/forms [(count fa) (count fb)]
     :compare/verdict (if same? :identical :divergent)
     :compare/differing-paths (vec diffs)
     :compare/excluded (:normalized/excluded a)}))

;; ---------------------------------------------------------------------------
;; the planted divergence -- what makes the determinism control non-vacuous
;; ---------------------------------------------------------------------------

(defn plant-mu-uniform!
  "Perturb ONE pinned input and nothing else: the last pinned trace record's
   `:mu-post`, the strategic belief the next tick carries as its prior
   (C509 A0; belief carry at war_machine.clj:5959, trace.clj `:mu-post`).
   Every entity's channel distribution is replaced by the uniform one over the
   channels that entity already had, so the perturbation changes the numbers
   without changing the domain or the shape -- exactly the kind of drift a
   checkpoint is supposed to catch rather than absorb.

   Writes the file back with the record replaced, and returns what it did."
  [trace-dir]
  (let [fs (trace-files trace-dir)
        _ (when (empty? fs) (die! 2 "plant: no wm-trace-*.edn in" trace-dir))
        target (last fs)
        forms (read-forms (.getPath target))
        _ (when (empty? forms) (die! 2 "plant: no records in" (.getPath target)))
        idx (dec (count forms))
        before (nth forms idx)
        mu (:mu-post before)
        _ (when-not (map? mu) (die! 2 "plant: the last record carries no :mu-post map"))
        uniform (into {} (map (fn [[ent chans]]
                                [ent (if (and (map? chans) (seq chans))
                                       (let [u (/ 1.0 (count chans))]
                                         (into {} (map (fn [[c _]] [c u])) chans))
                                       chans)]))
                      mu)
        after (assoc before :mu-post uniform)
        out (mapv (fn [i f] (if (= i idx) after f)) (range (count forms)) forms)]
    (with-open [w (io/writer (.getPath target))]
      (doseq [f out] (.write w (pr-str f)) (.write w "\n")))
    {:plant/file (.getPath target)
     :plant/record-index idx
     :plant/run-id (:run/id before)
     :plant/what :mu-uniform
     :plant/entities (count mu)
     :plant/mu-before-sha256 (sha256-str (canon mu))
     :plant/mu-after-sha256 (sha256-str (canon uniform))
     :plant/note "the pinned corpus is now DIFFERENT from pin/manifest.edn; `wm_step.sh step` refuses on that drift unless --allow-pin-drift is passed"}))

;; ---------------------------------------------------------------------------

(defn- emit! [out-path data]
  (io/make-parents out-path)
  (spit out-path (with-out-str (pp/pprint data)))
  out-path)

(let [[cmd & args] *command-line-args*]
  (case cmd
    "manifest" (let [[dir out] args]
                 (emit! out (manifest dir))
                 (println (format "manifest: %s -> %s" dir out)))
    "verify-dirs" (let [[a b] args
                        r (verify-dirs a b)]
                    (println (format "verify-dirs: %s vs %s -> %s (missing %d, added %d, changed %d)"
                                     a b (name (:verify/verdict r))
                                     (count (:verify/missing r)) (count (:verify/added r))
                                     (count (:verify/changed r))))
                    (doseq [n (concat (:verify/missing r) (:verify/added r) (:verify/changed r))]
                      (println "verify-dirs:   drift ->" n))
                    (System/exit (if (= :drift (:verify/verdict r)) 4 0)))
    "verify" (let [[dir mpath] args
                   r (verify dir mpath)]
               (println (format "verify: %s vs %s -> %s (missing %d, added %d, changed %d)"
                                dir mpath (name (:verify/verdict r))
                                (count (:verify/missing r)) (count (:verify/added r))
                                (count (:verify/changed r))))
               (doseq [n (concat (:verify/missing r) (:verify/added r) (:verify/changed r))]
                 (println "verify:   drift ->" n))
               (System/exit (if (= :drift (:verify/verdict r)) 4 0)))
    "world" (let [[out] args
                  w (world)]
              (emit! out w)
              (println (format "world: %d files hashed; mana age %.2f min, stale? %s"
                               (count (:world/files w))
                               (double (or (get-in w [:world/mana-age :age-min]) -1.0))
                               (get-in w [:world/mana-age :stale?])))
              (let [fr (:world/flip-readiness w)
                    srcs (:sources fr)]
                (println (format "world: flip-readiness sources %d/%d hashed, %d in-commit; per-node repos %s"
                                 (count (filter :sha256 srcs)) (count srcs)
                                 (count (filter :worktree-matches-commit? srcs))
                                 (str/join "," (map (comp name :repo) (:per-node-git fr)))))))
    "delta" (let [[pin sand out] args
                  d (delta pin sand)]
              (emit! out d)
              (println (format "delta: %d record(s) appended; run-ids %s"
                               (:delta/added-records d) (pr-str (:delta/run-ids d)))))
    "normalize" (let [[in out] args
                      n (normalize in)]
                  (emit! out n)
                  (println (format "normalize: %d form(s) -> %s" (:normalized/forms n)
                                   (:normalized/sha256 n))))
    "compare" (let [[a b out] args
                    c (compare-normalized a b)]
                (when out (emit! out c))
                (println (format "compare: %s (a %s, b %s)"
                                 (name (:compare/verdict c))
                                 (subs (str (:compare/a-sha256 c)) 0 12)
                                 (subs (str (:compare/b-sha256 c)) 0 12)))
                (doseq [d (take 12 (:compare/differing-paths c))]
                  (println "compare:   differs at" (pr-str (:path d)) (name (:why d))))
                (System/exit (if (= :identical (:compare/verdict c)) 0 5)))
    "plant" (let [[dir what] args]
              (if (= what "mu-uniform")
                (let [r (plant-mu-uniform! dir)]
                  (pp/pprint r))
                (die! 2 "plant: unknown perturbation" (pr-str what) "-- only mu-uniform is defined")))
    (die! 2 "usage: wm_step_records.bb manifest|verify|verify-dirs|world|delta|normalize|compare|plant ...")))
