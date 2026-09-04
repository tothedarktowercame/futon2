#!/usr/bin/env bb
;; runtime_validation_check.bb -- the validator for U36's runtime-validation
;; catalog (runs/RUNTIME-VALIDATION-CATALOG.edn).
;;
;;   bb runtime_validation_check.bb                  ; check the catalog
;;   bb runtime_validation_check.bb --summary        ; print the COUNTS line only
;;   bb runtime_validation_check.bb --deposit <run-id> ; one run-era ledger row (RE6)
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

(defn deposit-receipt [run-id]
  (let [store-dir (str here "runs/" run-id)
        holds (run-store-files store-dir)
        contemporaneous (vec (filter #(re-find #"(?i)runtime[-_]?validation" %) (or holds [])))]
    ;; array-map, not a literal: a map literal of this size is a hash-map and
    ;; would print in hash order, so the receipt would not be stable to read.
    (array-map
     :schema :wm/run-era-deposit-receipt-v1
     :row :RE6
     :check :per-node-runtime-validation
     :run-id run-id
     :produced-by "holes/labs/wm-contract/runtime_validation_check.bb --deposit"
     :deterministic
     (str "No wall-clock field. This receipt is rewritten byte-identically on every deposit, "
          "which is what lets the deposit require it to be committed and unmodified, and lets "
          "the same deposit repeat as :already-present.")
     :run-store {:dir (str "holes/labs/wm-contract/runs/" run-id)
                 :exists? (boolean holds)
                 :holds holds
                 :runtime-validation-artifacts contemporaneous}
     :verdict-deposited (cond (seq @problems) :red
                              (seq contemporaneous) :green
                              :else :typed-absence)
     :live-derivation
     {:read-from "the catalog and the source tree at deposit time, NOT the tree the run was taken at"
      :artifact "holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn"
      :counts-line counts-line
      :counts counts
      :test-runs-recorded (into (sorted-map)
                                (for [[ns-name run] test-runs]
                                  [ns-name (select-keys run [:exit :tests :assertions :at])]))
      :problems (vec @problems)}
     :why-the-catalog-cannot-be-run-scoped
     (str "the catalog carries no run identity: its rows are keyed by node and axis, its "
          ":test-runs are keyed by namespace with an :at date, and neither the catalog nor any "
          "row names a wm run-id. A tick writes nothing into it. So a verdict about a NAMED RUN "
          "cannot be read out of it, and this check has no as-of-run mode that could reconstruct "
          "one.")
     :not-what-this-says
     (str "The live derivation above is a property of the tree at deposit time. It is recorded "
          "so the absence is legible, and it is NOT the deposited verdict."))))

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
           "points at, and is not deposited as this run's verdict.")
      :red
      (str "the runtime-validation catalog check FAILS at deposit time: "
           (str/join "; " (:problems d)))
      :green
      (str "read from the run store's own runtime-validation artifact(s): "
           (str/join ", " (:runtime-validation-artifacts store))
           "; " (:counts-line d)))))

(defn deposit! [run-id]
  (let [r (deposit-receipt run-id)
        rel (str "holes/labs/wm-contract/runs/RE6-check-deposits/runtime-validation-" run-id ".edn")
        path (str code-root "futon2/" rel)]
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
