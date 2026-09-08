#!/usr/bin/env bb
;; process_census.bb -- the Box 12 process-assurance census, as an instrument.
;;
;; Row :PA1z (zaif-harness board). Data lives in census-ledger.edn; read its
;; header first, it explains the mechanise-absence / pin-presence split and why
;; the split is where it is.
;;
;; USAGE
;;   ./process_census.bb                 # the negative control: re-derive
;;                                       # ALIGN's 42 censused cells and diff
;;   ./process_census.bb --node R2       # census one uncensused node (PA2z/3z/4z)
;;   ./process_census.bb --edn           # machine-readable verdicts on stdout
;;   ./process_census.bb --ledger P       # read the ledger from P (tests plant here)
;;   ./process_census.bb --pattern v1     # force a pattern version (default: the
;;                                       # ledger's :pattern-version). v1 is what
;;                                       # ALIGN's 42 were measured under.
;;
;; EXIT CODES (a rung is only real where something refuses -- see the ledger
;; header, and N-process-trap-recording-conventions.md s0):
;;   0  every adjudication still stands and the harness agrees with ALIGN
;;   3  STALE ADJUDICATION -- a pinned pointer no longer shows its token. The
;;      cell is NOT credited. Fix the pointer or re-adjudicate; do not paper.
;;   4  DISAGREEMENT with the census of record. Route to claude-1 (whose lab
;;      owns ALIGN). NEVER reconciled here in either direction.
;;   2  a search ERRORED. Deliberately distinct from "found nothing": reading a
;;      tool failure as an absence is how a census invents evidence.

(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell])

(def ^:private args (set *command-line-args*))
(def ^:private edn-out? (contains? args "--edn"))
(def ^:private one-node (second (drop-while #(not= "--node" %) *command-line-args*)))

(defn- die [code & msg]
  (binding [*out* *err*] (apply println "process_census:" msg))
  (System/exit code))

;; Paths resolve against the CODE ROOT derived from this script's own location,
;; never from the caller's cwd -- the C16 finding already recorded in
;; worklist_check.bb, which cost a debugging session when a loop invoked a
;; checker by absolute path from /tmp. The scope spans three sibling repos
;; (futon2, futon3c, p4ng), so the root is the futon2 git root's PARENT.
(def script-dir (.getParentFile (.getAbsoluteFile (io/file *file*))))
(def code-root
  (let [{:keys [exit out]} (shell/sh "git" "rev-parse" "--show-toplevel" :dir script-dir)]
    (when-not (zero? exit) (die 2 "cannot find the futon2 git root from" (str script-dir)))
    (.getParent (io/file (str/trim out)))))

;; --ledger lets a test PLANT a corrupted ledger and prove the refusal refuses.
;; Defaults to the real one beside this script.
(def ^:private ledger-path
  (or (second (drop-while #(not= "--ledger" %) *command-line-args*))
      (str (io/file script-dir "census-ledger.edn"))))
(def ledger (edn/read-string (slurp ledger-path)))
(when-not (= :box12/census-ledger-v1 (:schema ledger)) (die 2 "unexpected ledger schema"))

(defn- abs-path [rel] (str (io/file code-root rel)))

;; ---------------------------------------------------------------------------
;; Searching. rg exit 0 = matched, 1 = no match, 2 = error. Collapsing 1 and 2
;; is the bug this function exists to prevent.
;; ---------------------------------------------------------------------------
(defn- all-read-paths
  "Every path this census reads: the declared search scopes (all versions) plus
   the file of every adjudication pointer. A pointer file is as much an input as
   a scope path -- the three spurious stales that prompted this were pointer
   files, not scope directories."
  []
  (distinct
   ;; :scope also carries :not-searched, which is prose rather than a path list --
   ;; mapcat over a string yields characters, so take only the vector values.
   (concat (mapcat val (filter (comp vector? val) (:scope ledger)))
           (keep :file (mapcat :pointers (:adjudicated ledger))))))

;; ---------------------------------------------------------------------------
;; TREE CLEANLINESS. Added 2026-09-08 after this harness reported three spurious
;; stale adjudications: war_machine.clj and full_loop_runner.clj were MID-EDIT in
;; the working tree by another seat's row, and the census read them anyway.
;;
;; The census reads the WORKING TREE, not a committed tree. On a shared live
;; checkout that makes every result non-reproducible and, worse, indistinguishable
;; from a real finding -- a half-written file looks exactly like a rotted pointer.
;; The zaif-build-loop already refuses to run over someone's uncommitted board
;; edits; the instrument that audits the loop had no such rule, which is the
;; funnier version of the same omission.
;;
;; So: a census over a dirty read-scope REFUSES (exit 5) and names the files.
;; --allow-dirty runs anyway and stamps :tree-dirty into the output, so a result
;; taken knowingly over a moving tree still says so in its own artifact rather
;; than looking like a clean one.
(def ^:private allow-dirty? (some #{"--allow-dirty"} *command-line-args*))

(defn- dirty-files
  "Tracked modifications anywhere the census reads. Repo-rooted, so a path under
   futon2 is checked against futon2's index and futon3c's against futon3c's."
  []
  (let [repos (distinct (keep #(first (str/split % #"/")) (all-read-paths)))]
    (vec (mapcat
          (fn [repo]
            (let [dir (io/file code-root repo)]
              (when (.exists dir)
                (let [{:keys [out]} (shell/sh "git" "-C" (str dir) "status" "--porcelain")]
                  (->> (str/split-lines (or out ""))
                       (remove str/blank?)
                       (keep (fn [l]
                               (let [st (subs l 0 (min 2 (count l)))
                                     f  (str/trim (subs l (min 3 (count l))))]
                                 (when-not (str/starts-with? st "??")
                                   (let [rel (str repo "/" f)]
                                     (when (some #(or (str/starts-with? rel %)
                                                      (str/starts-with? % rel))
                                                 (all-read-paths))
                                       rel)))))))))))
          repos))))

(defn- rg [pattern paths]
  (let [existing (filterv #(.exists (io/file (abs-path %))) paths)
        missing  (remove #(.exists (io/file (abs-path %))) paths)]
    (when (seq missing)
      (die 2 "declared scope path does not exist:" (str/join ", " missing)
           "-- the scope is stated in census-ledger.edn and a moved path must be"
           "corrected there, not silently skipped"))
    (let [{:keys [exit out err]} (apply shell/sh "rg" "-n" "--no-heading" pattern
                                        (mapv abs-path existing))]
      (case (int exit)
        0 {:status :matched
           :hits (mapv #(str/replace % (str code-root "/") "")
                       (remove str/blank? (str/split-lines out)))
           :command (str "rg -n '" pattern "' " (str/join " " existing))}
        1 {:status :no-match :hits []
           :command (str "rg -n '" pattern "' " (str/join " " existing))}
        (die 2 "rg errored (exit" exit ") on pattern" (pr-str pattern) "--" (str/trim (str err))
             "\n  A tool failure is NOT an absence. Nothing is credited from this run.")))))

;; Truncation: rg is run without a head limit and the full output is counted,
;; so every enumeration below can state that it was untruncated. (Board header,
;; TRUNCATED-ENUMERATION RULE.)
(def ^:private pattern-version
  (or (some-> (second (drop-while #(not= "--pattern" %) *command-line-args*)) keyword)
      (:pattern-version ledger)
      :v1))
(def ^:private pattern-spec
  (or (get-in ledger [:node-link-patterns pattern-version])
      (die 2 "ledger has no pattern version" pattern-version)))

;; A version may carry SEVERAL forms (v2 = v1's literal-map form plus the
;; constructor-call form). A node is linked if ANY form matches; the hits are
;; merged so the report shows which form found what. Running every form even
;; after one matches is deliberate -- the point of v2 is to see linkage v1
;; misses, and that is only visible if both are evaluated.
(defn- node-link-search [node]
  (let [paths (get-in ledger [:scope (:scope-key pattern-spec)])
        runs (mapv (fn [form] (assoc (rg (format form node) paths) :form form))
                   (:forms pattern-spec))
        matched (filterv #(= :matched (:status %)) runs)]
    (if (seq matched)
      {:status :matched
       :hits (vec (distinct (mapcat :hits matched)))
       :command (str/join " ; " (map :command runs))
       :matched-forms (mapv :form matched)}
      {:status :no-match :hits []
       :command (str/join " ; " (map :command runs))})))

;; ---------------------------------------------------------------------------
;; Pinned-pointer check. A pointer must still SHOW its declared token inside its
;; declared line range, or the adjudication that cited it is stale.
;; ---------------------------------------------------------------------------
(defn- check-pointer [{:keys [file from to expect]}]
  (let [f (io/file (abs-path file))]
    (if-not (.exists f)
      {:ok? false :why :file-missing :file file :expect expect}
      (let [lines (vec (str/split-lines (slurp f)))
            n (count lines)
            in-range (subvec lines (max 0 (dec from)) (min n to))
            found? (some #(str/includes? % expect) in-range)]
        (if found?
          {:ok? true :file file :from from :to to :expect expect}
          ;; Locate the token elsewhere so a stale pointer carries its own
          ;; correction rather than just a complaint.
          (let [elsewhere (keep-indexed (fn [i l] (when (str/includes? l expect) (inc i))) lines)]
            {:ok? false :why (if (> to n) :range-past-eof :token-not-in-range)
             :file file :from from :to to :expect expect
             :file-lines n
             :token-found-at (vec (take 5 elsewhere))}))))))

(defn- adjudication-for [node cell]
  (first (filter #(and (= node (:node %)) (= cell (:cell %))) (:adjudicated ledger))))

(defn- parse-site
  "A declared site is [repo:]path:N or [repo:]path:N-M. Returns [path from to]."
  [site]
  (let [site (if-let [i (str/index-of site ":")]
               (if (re-find #"^[a-z0-9-]+:[^0-9]" site) (subs site (inc i)) site)
               site)
        [_ path a b] (re-matches #"^(.*):(\d+)(?:-(\d+))?$" site)]
    (when path [path (parse-long a) (parse-long (or b a))])))

(defn- hit-accounted?
  "True when HIT falls inside one of DECL's declared site ranges.

   Path match is a SUFFIX match: hits are rooted at the census root
   (futon2/src/...), declarations are repo-relative (src/...)."
  [decl hit]
  (let [[_ hpath hline] (re-matches #"^([^:]+):(\d+):.*$" hit)]
    (when hpath
      (let [hline (parse-long hline)]
        (boolean
         (some (fn [site]
                 (when-let [[path from to] (parse-site site)]
                   (and (str/ends-with? hpath path) (<= from hline to))))
               (:sites decl)))))))

(defn- serves-cells-verdict
  "Mechanism (a). A cell no adjudication claims is :absent ONLY IF the node has a
   :serves-cells declaration that does not name it AND every hit it found is
   accounted for by that declaration's sites. An UNACCOUNTED hit is conduct the
   census has never adjudicated, so it still needs adjudication -- silencing by
   node would blind the instrument to the next boundary anyone builds."
  [node cell hits]
  (when-let [decl (first (filter #(= node (:node %)) (:serves-cells-declarations ledger)))]
    (when-not (some #{cell} (:serves-cells decl))
      (let [unaccounted (remove #(hit-accounted? decl %) hits)]
        (when (empty? unaccounted)
          {:verdict :absent :basis :serves-cells-accounted
           :authority (:authority decl)
           :serves-cells (:serves-cells decl)})))))

(defn- route-hop-for
  "An adjudication claude-1 made in ALIGN (d232ea06) covering a whole node's
   route-hop cells. Consulted AFTER the per-cell adjudications above, so an
   explicit [E-T]/[N20] reading always wins over the node-wide entry."
  [node cell]
  (first (filter #(and (= node (:node %)) (some #{cell} (:cells %)))
                 (:route-hop-adjudicated ledger))))

(defn- targeted-for [node cell]
  (first (filter #(and (= node (:node %)) (some #{cell} (:cells %))) (:targeted-absence ledger))))

;; ---------------------------------------------------------------------------
;; One cell's verdict.
;; ---------------------------------------------------------------------------
(defn- guard-tree! []
  (let [dirty (dirty-files)]
    (when (seq dirty)
      (if allow-dirty?
        (binding [*out* *err*]
          (println "process_census: WARNING tree is dirty and --allow-dirty was given;"
                   "the result is stamped :tree-dirty --" (str/join ", " dirty)))
        (die 5 "read scope has uncommitted modifications, so a census now would not be"
             "reproducible and a half-written file is indistinguishable from a rotted"
             "pointer:" (str/join ", " dirty)
             "-- commit or stash, or pass --allow-dirty to record a knowingly-dirty run")))
    dirty))

(defn- verdict [node cell]
  (if-let [adj (adjudication-for node cell)]
    (let [checks (mapv check-pointer (:pointers adj))]
      (if (every? :ok? checks)
        ;; :authority and :qualification travel WITH the verdict, not just in the
        ;; ledger. A consumer that sees only :exists cannot tell a cell its census
        ;; of record adjudicated from one a work seat credited itself -- which is
        ;; the [E-T-S] fault, and the reason PA16z exists. Carrying the provenance
        ;; into the result is what lets a check assert it.
        (cond-> {:node node :cell cell :verdict (:verdict adj) :basis :pinned-adjudication
                 :tag (:tag adj) :pointers checks}
          (:authority adj)     (assoc :authority (:authority adj))
          (:qualification adj) (assoc :qualification (:qualification adj)))
        {:node node :cell cell :verdict :stale-adjudication :basis :pinned-adjudication
         :tag (:tag adj) :declared (:verdict adj)
         :pointers checks :stale (filterv (complement :ok?) checks)}))
    (if-let [rh (route-hop-for node cell)]
      {:node node :cell cell :verdict (:verdict rh) :basis :route-hop-adjudicated
       :authority (:authority rh) :hit (:hit rh)}
      (let [tgt (targeted-for node cell)
          r (if tgt (rg (:pattern tgt) (:files tgt)) (node-link-search node))]
      (if (= :no-match (:status r))
        {:node node :cell cell :verdict :absent
         :basis (if tgt :targeted-absence-search :node-link-search)
         :tag (:tag tgt) :command (:command r) :hits 0 :untruncated true}
        (if-let [sc (serves-cells-verdict node cell (:hits r))]
          (merge {:node node :cell cell :command (:command r) :hits (:hits r)
                  :untruncated true} sc)
          {:node node :cell cell :verdict :hit-needs-adjudication
           :basis (if tgt :targeted-absence-search :node-link-search)
           :command (:command r) :hits (:hits r) :untruncated true}))))))

(defn- declared-verdict [node cell]
  (if-let [adj (adjudication-for node cell)] (:verdict adj) :absent))

;; ---------------------------------------------------------------------------
;; Run
;; ---------------------------------------------------------------------------
(def nodes (if one-node [one-node] (:censused-nodes ledger)))
(def control? (nil? one-node))
;; Refuse BEFORE measuring, not after: a census computed over a moving tree is
;; not worth reporting even with a warning attached.
(def tree-dirty (guard-tree!))
(def results (vec (for [n nodes c (:cells ledger)] (verdict n c))))
(def stale (filterv #(= :stale-adjudication (:verdict %)) results))
(def disagreements
  (when control?
    (filterv (fn [r] (and (not= :stale-adjudication (:verdict r))
                          (not= (declared-verdict (:node r) (:cell r)) (:verdict r))))
             results)))

(if edn-out?
  (prn {:generated-by "process_census.bb" :row :PA1z :pattern-version pattern-version
        :tree-dirty tree-dirty
        :mode (if control? :negative-control :census-slice)
        :nodes nodes :results results
        :stale stale :disagreements disagreements})
  (do
    (println "Box 12 process census --" (if control? "NEGATIVE CONTROL over ALIGN's censused nodes" (str "slice: " (str/join ", " nodes))))
    (println "census of record:" (:census-of-record ledger) (str "(" (:census-of-record-dated ledger) ")"))
    (when (seq tree-dirty)
      (println "TREE DIRTY (--allow-dirty):" (str/join ", " tree-dirty)))
    (println "node-link pattern:" (name pattern-version)
             (str "(" (count (:forms pattern-spec)) " form(s), "
                  (count (get-in ledger [:scope (:scope-key pattern-spec)])) " scope paths)"))
    (println)
    (printf "%-7s %-14s %-24s %s%n" "node" "cell" "verdict" "basis")
    (doseq [r results]
      (printf "%-7s %-14s %-24s %s%n" (:node r) (name (:cell r)) (name (:verdict r))
              (str (name (:basis r)) (when (:tag r) (str " " (:tag r))))))
    (println)
    (let [tally (frequencies (map :verdict results))]
      (println "tally:" (into (sorted-map) tally) (str "(" (count results) " cells)")))
    (when (seq stale)
      (println)
      (println "STALE ADJUDICATIONS -- these cells are NOT credited:")
      (doseq [s stale, p (:stale s)]
        (printf "  %s %s %s: %s:%d-%d no longer shows %s (%s)%n"
                (:node s) (name (:cell s)) (:tag s) (:file p) (:from p) (:to p)
                (pr-str (:expect p)) (name (:why p)))
        (when (seq (:token-found-at p))
          (printf "      token is at line(s) %s -- pointer looks like drift, not deletion%n"
                  (str/join ", " (:token-found-at p))))))
    (when (seq disagreements)
      (println)
      (println "DISAGREEMENT WITH THE CENSUS OF RECORD -- route to claude-1, do not reconcile here:")
      (doseq [d disagreements]
        (printf "  %s %s: ALIGN says %s, harness says %s%n"
                (:node d) (name (:cell d)) (name (declared-verdict (:node d) (:cell d))) (name (:verdict d)))))))

(cond
  (seq stale) (System/exit 3)
  (seq disagreements) (System/exit 4)
  :else (System/exit 0))
