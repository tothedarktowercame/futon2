#!/usr/bin/env bb
;; runtime_validation_check.bb -- the validator for U36's runtime-validation
;; catalog (runs/RUNTIME-VALIDATION-CATALOG.edn).
;;
;;   bb runtime_validation_check.bb                  ; check the catalog
;;   bb runtime_validation_check.bb --summary        ; print the COUNTS line only
;;   bb runtime_validation_check.bb --deposit <run-id> ; one run-era ledger row (RE6)
;;   bb runtime_validation_check.bb --deposit <run-id> --dry-run ; print it, write nothing
;;   bb runtime_validation_check.bb --run-basis <run-id> ; U58: the run's captured
;;       cross-repo identity set, compared to the tree now. Exit 0 unmoved, 1 moved,
;;       2 no capture. WORLD_RECORD=<path> overrides the run store's world-before.edn.
;;   CATALOG=/path/to/other.edn bb runtime_validation_check.bb
;;
;; WHAT IT CHECKS, and why each check is here rather than left to a reader:
;;   1. SCHEMA + VOCABULARY. Every row's :axis, :evidence-kind, :status,
;;      :substrate and :binding is drawn from the catalog's own declared
;;      vocabulary. An unrecognised value fails naming the row -- a status
;;      nobody recognises must not read as one of the good ones.
;;   2. POINTERS RESOLVE. Every pointer in every string of the catalog is
;;      resolved to a file and a line range that exists.
;;      THE CONVENTION IS DELIBERATELY DIFFERENT FROM pointer_check.bb: a
;;      catalog pointer is REPO-RELATIVE FROM /home/joe/code
;;      ("futon2/test/futon2/aif/efe_test.clj:127-205"), resolved by direct
;;      path join. pointer_check.bb matches a BARE FILENAME against a
;;      hand-maintained allowlist of directories, and its own header records
;;      six occasions on which a valid pointer reported "file not found"
;;      because nobody had cited that directory before (C472, C473, AC7, U11,
;;      U13, U23). This catalog cites five repositories and both test trees, so
;;      the allowlist form would have hit that defect on the first row.
;;   3. TEST ROWS NAME A RUN. A row claiming :evidence-kind :test-green must
;;      name a :ns that appears in :test-runs with a recorded exit code, and a
;;      row whose run exited non-zero must be typed :red, not :exists.
;;   4. FLIP REFERENCES CLOSE. Every :gates-flip entry is a key of :flips.
;;   5. NODES ARE DECLARED. Every :node of a :per-node row is in :node-set.
;;   6. THE NARRATIVE CANNOT DRIFT. The COUNTS line this script computes must
;;      appear verbatim in RUNTIME-VALIDATION-CATALOG.md, so the prose cannot
;;      quote a total the data no longer supports.
;; Exit 0 all clear, 1 on any failure (house convention).
(require '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def code-root (str (System/getProperty "user.home") "/code/"))
(def here (str code-root "futon2/holes/labs/wm-contract/"))
(def catalog-path (or (System/getenv "CATALOG") (str here "runs/RUNTIME-VALIDATION-CATALOG.edn")))
(def md-path (or (System/getenv "CATALOG_MD") (str here "runs/RUNTIME-VALIDATION-CATALOG.md")))

(def problems (atom []))
(defn fail! [& parts] (swap! problems conj (str/join " " (map str parts))))

(def catalog (edn/read-string (slurp catalog-path)))

;; --- 1. schema ------------------------------------------------------------
(when-not (= :wm/runtime-validation-catalog-v1 (:schema catalog))
  (fail! "schema is" (:schema catalog) "-- expected :wm/runtime-validation-catalog-v1"))

(def vocab (:vocabulary catalog))
(def node-set (set (:node-set catalog)))
(def flips (:flips catalog))
(def test-runs (:test-runs catalog))
(def rows (:rows catalog))

(when (empty? rows) (fail! "catalog has no rows -- a vacuous catalog passes nothing"))

;; --- 2. pointers ----------------------------------------------------------
;; Repo-relative from ~/code, optional :LINE or :A-B. The stem admits dots and
;; slashes; the extension list is open because the catalog cites .sh and .bb
;; runners as well as source.
(def ptr-re #"([A-Za-z0-9_./\-]+\.(?:clj|cljc|bb|lean|edn|sh|py|md|txt|tex)):(\d+)(?:-(\d+))?")

(defn check-pointer [ptr file a b where]
  (let [path (str code-root file)
        f (io/file path)]
    (cond
      (not (.isFile f)) (do (fail! "UNRESOLVED" ptr "(file not found) in" where) false)
      :else
      (let [n (count (str/split-lines (slurp path)))
            lo (Integer/parseInt a) hi (Integer/parseInt (or b a))]
        (cond
          (> lo hi) (do (fail! "UNRESOLVED" ptr "(inverted range) in" where) false)
          (> hi n) (do (fail! "UNRESOLVED" ptr (str "(end beyond file: " n " lines) in") where) false)
          (> lo n) (do (fail! "UNRESOLVED" ptr (str "(start beyond file: " n " lines) in") where) false)
          :else true)))))

(defn strings-of [x] (filter string? (tree-seq coll? seq x)))

(def pointer-results
  (doall
   (for [row (concat rows (vals flips) [(dissoc catalog :rows :flips)])
         :let [where (or (:id row) (:flip row) :catalog-header)]
         s (strings-of row)
         [ptr file a b] (re-seq ptr-re s)]
     (check-pointer ptr file a b where))))

;; --- 3. vocabulary + row shape -------------------------------------------
(def required #{:id :axis :validation :evidence-kind :status :pointer})

(doseq [row rows]
  (let [id (:id row)
        missing (remove #(contains? row %) required)]
    (when (seq missing) (fail! "row" id "is missing required keys" (vec missing)))
    (doseq [[k vocab-key] {:axis :axis :evidence-kind :evidence-kind
                           :status :status :substrate :substrate :binding :binding}]
      (when-let [v (get row k)]
        (when-not (contains? (set (get vocab vocab-key)) v)
          (fail! "row" id "has" k v "which is not in the declared vocabulary"
                 (vec (get vocab vocab-key))))))
    ;; 3a. per-node rows name a declared node
    (when (= :per-node (:axis row))
      (when-not (contains? node-set (:node row))
        (fail! "row" id "is :per-node but its :node" (:node row) "is not in :node-set")))
    ;; 3b. test rows name a recorded run, and a red run cannot be typed green
    (when (= :test-green (:evidence-kind row))
      (let [ns-name (:ns row)]
        (if-not (contains? test-runs ns-name)
          (fail! "row" id "claims :test-green but its :ns" (pr-str ns-name)
                 "is not in :test-runs")
          (let [run (get test-runs ns-name)]
            (when (and (not (zero? (:exit run))) (not= :red (:status row)))
              (fail! "row" id "names" ns-name "which exited" (:exit run)
                     "but the row is typed" (:status row) "-- a red run is :red"))
            (when (and (zero? (:exit run)) (= :red (:status row)))
              (fail! "row" id "is typed :red but" ns-name "exited 0"))))))
    ;; 3c. flip references close
    (doseq [f (:gates-flip row)]
      (when-not (contains? flips f)
        (fail! "row" id "gates flip" f "which is not a key of :flips")))))

;; --- 4. test-runs are self-consistent ------------------------------------
(doseq [[ns-name run] test-runs]
  (doseq [k [:repo :cmd :exit :tests :assertions :at]]
    (when-not (contains? run k) (fail! ":test-runs" ns-name "is missing" k)))
  (when (and (zero? (:exit run 0)) (pos? (+ (:failures run 0) (:errors run 0))))
    (fail! ":test-runs" ns-name "exited 0 with failures/errors recorded")))

;; --- 5. counts ------------------------------------------------------------
(defn tally [k] (frequencies (keep k rows)))
(def counts
  {:rows (count rows)
   :per-node (count (filter #(= :per-node (:axis %)) rows))
   :global-run (count (filter #(= :global-run (:axis %)) rows))
   :nodes-covered (count (distinct (keep :node rows)))
   :nodes-declared (count node-set)
   :exists (get (tally :status) :exists 0)
   :stale (get (tally :status) :exists-but-stale 0)
   :red (get (tally :status) :red 0)
   :gap (get (tally :status) :named-gap 0)
   :flips (count flips)
   :test-namespaces (count test-runs)
   :test-namespaces-green (count (filter #(zero? (:exit % 1)) (vals test-runs)))
   :pointers (count pointer-results)})

(def counts-line
  (format (str "COUNTS: %d rows (%d per-node, %d global-run) | nodes %d/%d | "
               "status exists=%d exists-but-stale=%d red=%d named-gap=%d | "
               "flips=%d | test namespaces %d/%d green | pointers=%d")
          (:rows counts) (:per-node counts) (:global-run counts)
          (:nodes-covered counts) (:nodes-declared counts)
          (:exists counts) (:stale counts) (:red counts) (:gap counts)
          (:flips counts)
          (:test-namespaces-green counts) (:test-namespaces counts)
          (:pointers counts)))

(when (contains? (set *command-line-args*) "--summary")
  (println counts-line)
  (System/exit 0))

(if (.isFile (io/file md-path))
  (when-not (str/includes? (slurp md-path) counts-line)
    (fail! "the narrative does not carry the computed COUNTS line; expected verbatim:\n  "
           counts-line))
  (fail! "narrative not found at" md-path))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE6)
;; ---------------------------------------------------------------------------
;;
;; WHAT VERDICT THIS DEPOSITS, and why it is a typed absence on every run this
;; catalog was not written beside. The catalog is a TREE artifact: its rows
;; point at source lines and its :test-runs record test invocations by
;; namespace and date. Nothing in it names a WM run, and no run of the machine
;; writes into it. So the deposit asks the run's own store, as RE3's two
;; tree-property checks do:
;;
;;   the run store holds a runtime-validation artifact -> deposit its verdict
;;   it does not                                       -> :typed-absence, notes
;;                                                        naming what was missing
;;
;; A failing check deposits :red whatever the store holds, because that failure
;; is about the tree the deposit is made from and a reader must see it.
(def deposit-run-id
  (second (drop-while #(not= "--deposit" %) *command-line-args*)))

(defn run-store-files [dir]
  (let [d (io/file dir)]
    (when (.isDirectory d)
      (let [base (str (.getPath d) "/")]
        (->> (file-seq d)
             (filter #(.isFile ^java.io.File %))
             (map #(str/replace-first (.getPath ^java.io.File %) base ""))
             sort vec)))))

;; ---------------------------------------------------------------------------
;; U58 -- the run-keyed basis, and --run-basis <run-id>
;; ---------------------------------------------------------------------------
;;
;; WHAT WAS MISSING, measured rather than presumed (C511-repair-or-elaborate.md
;; section 2). The catalogue input reproduces exactly -- its blob has not moved
;; and the COUNTS line re-computes byte-identically. The POINTER leg does not:
;; the catalogue's 181 pointers reach four repositories, 24 of them outside
;; futon2, and futon2's sha is the only repository identity a step recorded
;; (`:step/futon2-sha`, wm_step.sh:301). So "the pointer resolves" was a
;; property of the tree at reading time with no way to attribute it to the run --
;; for futon3c the accepted run's store held no identity at all.
;;
;; The stepper now captures the set (`wm_step_records.bb` `runtime-validation-capture`,
;; written into `world-before.edn` at `wm_step_records.bb:437` and copied into a run
;; store at `wm_step.sh:511`). This reads it back and names it AS THE BASIS OF THE
;; DEPOSIT: the identities the run's pointer resolution rested on, cited by the
;; row rather than left to a reader to reconstruct.
;;
;; IT DOES NOT MAKE THE CHECK RUN-SCOPED. The verdict is unchanged: the catalogue
;; carries no run identity (:why-the-catalog-cannot-be-run-scoped, :446-451), so a
;; run with no runtime-validation artifact in its store still deposits a typed
;; absence. What changes is that the absence now says WHICH tree the live
;; derivation was read from and which repositories that tree shares with the run.
(def world-record-override (System/getenv "WORLD_RECORD"))

(defn world-record-path [run-id]
  (or world-record-override (str here "runs/" run-id "/world-before.edn")))

(defn- futon2-rel [path]
  (let [root (str code-root "futon2/")]
    (if (str/starts-with? path root) (subs path (count root)) path)))

(defn- sha256-file [path]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        buf (byte-array 1048576)]
    (with-open [in (io/input-stream (io/file path))]
      (loop [] (let [n (.read in buf)] (when (pos? n) (.update md buf 0 n) (recur)))))
    (str/join (map #(format "%02x" (bit-and % 0xff)) (.digest md)))))

(defn- git-head [root]
  (when (.isDirectory (io/file root))
    (let [r (try (process/shell {:dir root :out :string :err :string :continue true}
                                "git" "rev-parse" "HEAD")
                 (catch Exception e {:exit 1 :out "" :err (str e)}))]
      (when (zero? (:exit r)) (str/trim (str (:out r)))))))

(defn- porcelain-count [root]
  (when (.isDirectory (io/file root))
    (let [r (try (process/shell {:dir root :out :string :err :string :continue true}
                                "git" "status" "--porcelain")
                 (catch Exception e {:exit 1 :out "" :err (str e)}))]
      (when (zero? (:exit r))
        (count (remove str/blank? (str/split-lines (str (:out r)))))))))

(defn run-keyed-basis
  "The identities the run's pointer resolution rested on, read from the run's own
   world record. EVERY FIELD COMES FROM THE CAPTURE and none from the tree, so
   the value is fixed by the run store: the deposit receipt embeds it and stays
   byte-stable, which is what lets the ledger require the receipt committed and
   unmodified and lets the same deposit repeat as :already-present. The
   comparison against the tree NOW is `--run-basis`, and it is deliberately not
   in the receipt.

   array-map, not a literal, for the same reason the receipt is: past eight keys
   a map literal is a hash-map and prints in hash order, and this value is read
   inside a receipt a person is expected to read."
  [run-id]
  (let [path (world-record-path run-id)
        f (io/file path)
        w (when (.isFile f) (edn/read-string {:default (fn [_ v] v)} (slurp path)))
        rv (:world/runtime-validation w)
        head (fn [status] (array-map
                           :schema :wm/runtime-validation-run-basis-v1
                           :run-id run-id
                           :world-record (futon2-rel path)
                           :produced-by "runtime_validation_check.bb run-keyed-basis (U58)"
                           :status status))]
    (cond
      (not (.isFile f))
      (assoc (head :no-world-record)
             :why (str "the run store holds no world-before.edn, so the run recorded no "
                       "repository identities and the pointer resolution behind any verdict "
                       "about it cannot be attributed to a tree"))

      (nil? rv)
      (assoc (head :predates-u58-capture)
             :why (str "the world record carries no :world/runtime-validation -- it was taken "
                       "before the U58 capture, so the catalogue's cross-repo identity set at "
                       "this run was never written down and cannot be recovered"))

      ;; One `array-map` CALL, not `assoc` onto the head: `assoc` past eight
      ;; entries returns a hash-map, which is the very thing the head is built
      ;; as an array-map to avoid.
      :else
      (array-map
       :schema :wm/runtime-validation-run-basis-v1
       :run-id run-id
       :world-record (futon2-rel path)
       :produced-by "runtime_validation_check.bb run-keyed-basis (U58)"
       :status :captured
       :catalog (array-map
                 :path (get-in rv [:catalog :path])
                 :repo (get-in rv [:catalog :repo])
                 :repo-rel (get-in rv [:catalog :repo-rel])
                 :sha256 (get-in rv [:catalog :sha256])
                 :last-commit (get-in rv [:catalog :last-commit])
                 :committed-blob-sha1 (get-in rv [:catalog :committed-blob-sha1])
                 :worktree-blob-sha1 (get-in rv [:catalog :worktree-blob-sha1])
                 :worktree-matches-commit? (get-in rv [:catalog :worktree-matches-commit?]))
       :pointers (:pointers rv)
       :repos (vec (for [r (:pointer-repos rv)]
                     (array-map :repo (:repo r) :pointers (:pointers r)
                                :head (:head r)
                                :porcelain-lines (count (:porcelain r)))))
       :what-this-establishes
       (str "the bytes of the catalogue this check validates, and the commit each "
            "repository its pointers reach stood at, AT THE RUN. A later reader can "
            "ask whether the tree it is resolving pointers against is that tree.")
       :what-this-does-not-establish
       (str "that the check's verdict is a property of the run. The catalogue carries "
            "no run identity and no tick writes into it, so the verdict here remains a "
            "property of the tree at deposit time; this basis names that tree's "
            "relation to the run's, it does not convert one into the other.")))))

(defn run-basis-report
  "`--run-basis <run-id>`: print the basis and compare it to the tree NOW. Exit
   0 when every captured identity still matches, 1 when one has MOVED, 2 when
   there is no capture to compare. Exit 1 is a FINDING and not a tool failure:
   it says the tree a pointer would resolve against today is not the tree the
   run read, which is precisely the inference C511 section 2 recorded as
   unavailable.

   Heads and the catalogue's content hash decide the exit. Porcelain LINE COUNTS
   are printed and decide nothing: uncommitted movement inside a repository is
   visible here as a count that differs, and this mode does not rule on it."
  [run-id]
  (let [b (run-keyed-basis run-id)]
    (pp/pprint b)
    (case (:status b)
      (:no-world-record :predates-u58-capture)
      (do (println (format "run-basis: NO CAPTURE for %s -- %s" run-id (:why b)))
          (System/exit 2))
      :captured
      (let [cat (:catalog b)
            now-cat (when (.isFile (io/file catalog-path)) (sha256-file catalog-path))
            moved (cond-> []
                    (not= (:sha256 cat) now-cat)
                    (conj (format "catalog: captured sha256 %s, now %s (%s)"
                                  (str (:sha256 cat)) (str now-cat) (futon2-rel catalog-path))))
            moved (into moved
                        (for [r (:repos b)
                              :let [now (git-head (str code-root (:repo r)))]
                              :when (not= (:head r) now)]
                          (format "%s: captured head %s, now %s -- %d of %d pointers resolve there"
                                  (:repo r) (str (:head r)) (str now) (:pointers r) (:pointers b))))]
        (println (format "run-basis: the catalog check at THIS tree: %s (%d problem%s) -- %s"
                         (if (seq @problems) "FAIL" "PASS") (count @problems)
                         (if (= 1 (count @problems)) "" "s") counts-line))
        (doseq [r (:repos b)]
          (println (format "run-basis:   %-9s pointers %3d  head %s  porcelain captured %d / now %s"
                           (:repo r) (:pointers r)
                           (let [h (str (:head r))] (if (>= (count h) 12) (subs h 0 12) h))
                           (:porcelain-lines r)
                           (str (porcelain-count (str code-root (:repo r)))))))
        (if (seq moved)
          (do (doseq [m moved] (println "run-basis: MOVED" m))
              (println (format "run-basis: %d of %d captured identities have MOVED since the run -- resolving a pointer against this tree is not resolving it against %s's"
                               (count moved) (inc (count (:repos b))) run-id))
              (System/exit 1))
          (do (println (format "run-basis: all %d captured identities unmoved -- this tree is %s's tree for every repository its %d pointers reach"
                               (inc (count (:repos b))) run-id (:pointers b)))
              (System/exit 0)))))))

(def run-basis-run-id
  (second (drop-while #(not= "--run-basis" %) *command-line-args*)))
(when (and (contains? (set *command-line-args*) "--run-basis") (nil? run-basis-run-id))
  (println "runtime_validation_check --run-basis needs a run-id")
  (System/exit 2))
;; Refused rather than ordered. `--run-basis` exits before the deposit block, so
;; the two given together would silently perform the report and swallow the
;; deposit -- a caller that asked for a ledger row and got a printout, with a
;; zero exit either way.
(when (and run-basis-run-id (contains? (set *command-line-args*) "--deposit"))
  (println "runtime_validation_check: --run-basis does not compose with --deposit -- the basis is a report and the deposit writes a ledger row; run them separately")
  (System/exit 2))
(when run-basis-run-id (run-basis-report run-basis-run-id))

(defn- ordered-map
  "An array-map from [k v] pairs, dropping nils. `array-map` takes a flat arg
   list and `assoc`/`conj` past eight entries silently returns a hash-map, so a
   receipt with an OPTIONAL key and a stable print order cannot be written as
   either. Here a key that is not carried is simply a nil pair."
  [& pairs]
  (apply array-map (apply concat (remove nil? pairs))))

(defn deposit-receipt [run-id]
  (let [store-dir (str here "runs/" run-id)
        holds (run-store-files store-dir)
        contemporaneous (vec (filter #(re-find #"(?i)runtime[-_]?validation" %) (or holds [])))
        basis (run-keyed-basis run-id)]
    ;; array-map, not a literal: a map literal of this size is a hash-map and
    ;; would print in hash order, so the receipt would not be stable to read.
    (ordered-map
     [:schema :wm/run-era-deposit-receipt-v1]
     [:row :RE6]
     [:check :per-node-runtime-validation]
     [:run-id run-id]
     [:produced-by "holes/labs/wm-contract/runtime_validation_check.bb --deposit"]
     [:deterministic
      (str "No wall-clock field. This receipt is rewritten byte-identically on every deposit, "
           "which is what lets the deposit require it to be committed and unmodified, and lets "
           "the same deposit repeat as :already-present.")]
     [:run-store {:dir (str "holes/labs/wm-contract/runs/" run-id)
                  :exists? (boolean holds)
                  :holds holds
                  :runtime-validation-artifacts contemporaneous}]
     ;; U58. The identities the run's pointer resolution rested on, read from the
     ;; run's own world record and from nothing else. It is cited here because
     ;; the live derivation below resolves 181 pointers into four repositories
     ;; and, before this, the row named the tree for exactly one of them.
     ;;
     ;; CARRIED ONLY WHEN THERE IS A CAPTURE TO CITE. The ledger is append-only
     ;; and compares a re-deposit field by field (run_era_ledger.bb:235-245), so
     ;; adding a key to the receipt of a run whose row is already deposited would
     ;; turn its replay from :already-present into a divergence refusal. A run
     ;; taken before the capture existed is answered by
     ;; `--run-basis <run-id>` at exit 2, which rewrites nothing.
     (when (= :captured (:status basis)) [:run-keyed-basis basis])
     [:verdict-deposited (cond (seq @problems) :red
                               (seq contemporaneous) :green
                               :else :typed-absence)]
     [:live-derivation
      {:read-from "the catalog and the source tree at deposit time, NOT the tree the run was taken at"
       :artifact "holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn"
       :counts-line counts-line
       :counts counts
       :test-runs-recorded (into (sorted-map)
                                 (for [[ns-name run] test-runs]
                                   [ns-name (select-keys run [:exit :tests :assertions :at])]))
       :problems (vec @problems)}]
     [:why-the-catalog-cannot-be-run-scoped
      (str "the catalog carries no run identity: its rows are keyed by node and axis, its "
           ":test-runs are keyed by namespace with an :at date, and neither the catalog nor any "
           "row names a wm run-id. A tick writes nothing into it. So a verdict about a NAMED RUN "
           "cannot be read out of it, and this check has no as-of-run mode that could reconstruct "
           "one.")]
     [:not-what-this-says
      (str "The live derivation above is a property of the tree at deposit time. It is recorded "
           "so the absence is legible, and it is NOT the deposited verdict.")])))

(defn- basis-note
  "One sentence naming the run's captured identities, empty when the receipt
   carries no basis. Deterministic: every value comes from the run store.

   Empty rather than a sentence about the absence, because a run deposited
   before the capture existed must keep the notes it was deposited with --
   otherwise replaying its deposit stops being :already-present and becomes the
   append-only ledger's divergence refusal (run_era_ledger.bb:235-245). What is
   missing for those runs is not silent: `--run-basis <run-id>` says it and
   exits 2."
  [receipt]
  (if-let [b (:run-keyed-basis receipt)]
    (str " The run's own capture (U58) names the identities its pointer resolution rested on: catalog sha256 "
         (subs (str (get-in b [:catalog :sha256])) 0 12) " at " (:repo-rel (:catalog b)) ", and "
         (str/join ", " (for [r (:repos b)]
                          (str (:repo r) " " (subs (str (:head r)) 0 12) " (" (:pointers r) " pointers)")))
         "; `bb runtime_validation_check.bb --run-basis " (:run-id b)
         "` compares them to the tree a reader is standing in.")
    ""))

(defn deposit-notes [run-id receipt]
  (let [d (:live-derivation receipt)
        store (:run-store receipt)]
    (case (:verdict-deposited receipt)
      :typed-absence
      (str "runs/" run-id "/ holds no runtime-validation artifact"
           (if (:exists? store)
             (str " -- the run store holds " (str/join ", " (:holds store)) " and nothing from this check")
             " -- the run store directory does not exist")
           ", and the catalog this check validates carries no run identity of its own: its rows "
           "are keyed by node and axis and its :test-runs by namespace and date, so no row can be "
           "attributed to a run. This check has no as-of-run mode, so the per-node validation "
           "state AT this run cannot be reconstructed. The check run at deposit time reports PASS "
           "with " (:counts-line d)
           "; that is a property of the tree at deposit time, is recorded in the artifact this row "
           "points at, and is not deposited as this run's verdict."
           (basis-note receipt))
      :red
      (str "the runtime-validation catalog check FAILS at deposit time: "
           (str/join "; " (:problems d))
           (basis-note receipt))
      :green
      (str "read from the run store's own runtime-validation artifact(s): "
           (str/join ", " (:runtime-validation-artifacts store))
           "; " (:counts-line d)
           (basis-note receipt)))))

(def deposit-dry-run? (contains? (set *command-line-args*) "--dry-run"))

(defn deposit! [run-id]
  (let [r (deposit-receipt run-id)
        rel (str "holes/labs/wm-contract/runs/RE6-check-deposits/runtime-validation-" run-id ".edn")
        path (str code-root "futon2/" rel)]
    ;; --dry-run prints the receipt and the notes the ledger row would carry and
    ;; writes NOTHING -- no receipt file, no ledger row. A run-era row is a claim
    ;; about an accepted run, so this is how the deposit's own citation can be
    ;; exhibited for a step that has not been accepted, without minting a row
    ;; about a run that does not exist.
    (when deposit-dry-run?
      (pp/pprint r)
      (println "runtime_validation_check --deposit --dry-run: notes the ledger row would carry:")
      (println " " (deposit-notes run-id r))
      (println "runtime_validation_check --deposit --dry-run: nothing written; receipt would be" rel)
      (System/exit 0))
    (io/make-parents path)
    (spit path (with-out-str (pp/pprint r)))
    (println "runtime_validation_check --deposit: receipt" rel)
    (let [{:keys [exit out err]}
          (process/shell {:dir (str code-root "futon2") :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":per-node-runtime-validation"
                         "--verdict" (str (:verdict-deposited r))
                         "--artifact" rel
                         "--author" "runtime_validation_check.bb --deposit"
                         "--deposited-by" "RE6 -- wire the four remaining catalogued checks"
                         "--notes" (deposit-notes run-id r))]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "runtime_validation_check --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" rel "and re-run")
        (System/exit 1))
      (System/exit 0))))

(when deposit-run-id (deposit! deposit-run-id))
(when (and (contains? (set *command-line-args*) "--deposit") (nil? deposit-run-id))
  (println "runtime_validation_check --deposit needs a run-id")
  (System/exit 1))

;; --- report ---------------------------------------------------------------
(println (format "runtime_validation_check: %s" counts-line))
(println (format "runtime_validation_check: %d pointers checked, %d unresolved"
                 (count pointer-results) (count (remove true? pointer-results))))
(doseq [p @problems] (println "  PROBLEM" p))
(if (seq @problems)
  (do (println (format "runtime_validation_check: FAIL (%d problems) exit-convention=0-pass/1-fail"
                       (count @problems)))
      (System/exit 1))
  (println "runtime_validation_check: PASS exit-convention=0-pass/1-fail"))
