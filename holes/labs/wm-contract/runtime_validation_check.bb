#!/usr/bin/env bb
;; runtime_validation_check.bb -- the validator for U36's runtime-validation
;; catalog (runs/RUNTIME-VALIDATION-CATALOG.edn).
;;
;;   bb runtime_validation_check.bb                  ; check the catalog
;;   bb runtime_validation_check.bb --summary        ; print the COUNTS line only
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
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])

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
