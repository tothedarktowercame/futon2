#!/usr/bin/env bb
;; ancestry_check.bb -- the contract-fidelity gate (:F5).
;;
;; Two refusals, one instrument:
;;
;;   (1) A WORKLIST MINT WITH NO ANCESTRY. A row minted after the gate landed
;;       must name what commissioned it -- :basis, :epic, or a :covers-key into
;;       a named registry -- and the pointer must resolve. The mechanism this
;;       answers is DRIFT-2026-09-05-account.md:78: worklist 44cb1e18's genesis
;;       schema "offers no parent-plan field", so row D5d consumed an A noun as
;;       a bound proxy and nothing refused, because there was nothing to refuse
;;       WITH. A field is what a checker can be pointed at.
;;
;;   (2) A PLAN THAT DROPS A CLAUSE OF ITS COMMISSIONING ANCESTOR with no
;;       :decision/not-done record. Schema: N-process-trap-recording-conventions.md:273
;;       (§3). A dropped clause carries author, date and rationale, or it is not
;;       dropped, it is lost -- which is the shape of the whole DRIFT account.
;;       Plus §3's other named refusal: a plan with no ancestor, and a plan that
;;       orders a consumer before its producer.
;;
;; JURISDICTION (the :F5 statement): new rows from the gate's landing forward.
;; The gate pins a sha in worklist.edn's :ancestry-gate; every row id present in
;; the ledger at that sha is grandfathered, and the count is PRINTED rather than
;; assumed. Grandfathering excuses an ABSENCE of ancestry, never a FALSEHOOD:
;; ancestry a grandfathered row does declare is resolved like any other, so the
;; check has a live population on the board today and is not purely prospective.
;;
;; If the pinned sha cannot be read, this exits 1. A gate that cannot determine
;; its own jurisdiction refuses; it does not wave the board through (T2a: drift
;; goes where the checker is not, and "the checker could not tell" is that).
;;
;; Usage: ancestry_check.bb [--worklist PATH] [--plans PATH] [--repo PATH]
;; Exit 0 with a summary line; exit 1 naming the row or plan and what was wrong.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str])

(def ^:private script-dir
  (.getParentFile (.getAbsoluteFile (io/file *file*))))

(defn- arg [flag default]
  (let [args (vec *command-line-args*)
        i (.indexOf args flag)]
    (if (neg? i) default (get args (inc i)))))

(def repo-root
  (let [given (arg "--repo" nil)]
    (or given
        (let [{:keys [exit out]} (shell/sh "git" "rev-parse" "--show-toplevel" :dir script-dir)]
          (when (zero? exit) (str/trim out))))))

(def worklist-path (arg "--worklist" (str script-dir "/worklist.edn")))
(def plans-path (arg "--plans" (str script-dir "/plan-ancestry.edn")))

(def ^:private problems (atom []))
(defn- refuse! [& m] (swap! problems conj (str/join " " (map str m))))

(defn- die [& m]
  (binding [*out* *err*] (apply println "ancestry_check:" m))
  (System/exit 1))

(when-not repo-root (die "cannot find the repo root from" (str script-dir)))

;; ---------------------------------------------------------------------------
;; Pointer resolution. `path` or `path:line` or `path:A-B`, repo-relative.
;; Same standard as pointer_check.bb -- file exists, A <= B, both within the
;; file's line count -- but resolved from the REPO ROOT rather than from a
;; hand-maintained allowlist of directories, because these pointers name .md
;; plan artifacts, which pointer_check.bb's extension regex does not scan at all.

(def ^:private ptr-re #"^(.+?)(?::(\d+)(?:-(\d+))?)?$")

(defn- resolve-pointer
  "nil when the pointer resolves; a reason string when it does not."
  [p]
  (if-not (string? p)
    (str "not a string: " (pr-str p))
    (let [[_ path a b] (re-matches ptr-re p)
          f (io/file repo-root path)]
      (cond
        (not (.exists f)) (str "file not found: " path)
        (not (.isFile f)) (str "not a file: " path)
        (nil? a) nil
        :else (let [n (count (str/split-lines (slurp f)))
                    lo (Integer/parseInt a)
                    hi (Integer/parseInt (or b a))]
                (cond (> lo hi) (str "inverted range: " p)
                      (> hi n) (str "end beyond file (" n " lines): " p)
                      (> lo n) (str "start beyond file (" n " lines): " p)
                      :else nil))))))

(defn- pointer-strings [v]
  (cond (string? v) [v]
        (coll? v) (filter string? (tree-seq coll? seq v))
        :else []))

;; ---------------------------------------------------------------------------
;; Part 1 -- worklist mints.

(def worklist
  (try (edn/read-string (slurp worklist-path))
       (catch Exception e (die "cannot read" worklist-path "--" (.getMessage e)))))

(def gate (:ancestry-gate worklist))
(when-not (map? gate)
  (die worklist-path "carries no :ancestry-gate map; jurisdiction is undeterminable"))
(when (str/blank? (str (:landed-after gate)))
  (die worklist-path ":ancestry-gate names no :landed-after sha"))

(def gate-ledger-path
  (or (:ledger-path gate) "holes/labs/wm-contract/worklist.edn"))

(def grandfathered-ids
  (let [sha (:landed-after gate)
        {:keys [exit out]} (shell/sh "git" "show" (str sha ":" gate-ledger-path) :dir repo-root)]
    (when-not (zero? exit)
      (die "cannot read" gate-ledger-path "at" sha
           "-- the :ancestry-gate sha is not in this history, so the gate cannot tell"
           "a grandfathered row from a new mint and refuses rather than guess"))
    (set (map :id (:items (edn/read-string out))))))

(when (empty? grandfathered-ids)
  (die "the :ancestry-gate sha" (:landed-after gate) "yields an EMPTY grandfather set"
       "-- a vacuous jurisdiction would put the whole board under the gate at once"))

(defn- ancestry-claims
  "The ancestry a row declares, as [label pointer-or-key ...] pairs. A row may
   declare more than one; all of them are resolved, and at least one must exist
   for a row under jurisdiction."
  [i]
  (concat
   (when-let [b (:basis i)] (map (fn [p] [:basis p]) (pointer-strings b)))
   (when-let [e (:epic i)]
     (map (fn [p] [:epic (if (str/includes? p "/") p (str "holes/labs/wm-contract/" p))])
          (pointer-strings e)))
   ;; :covers-key names a registry ENTRY -- the trail to a ruling recorded in
   ;; aif-equations.edn :choices or control-map-edges.edn :decisions. :none is a
   ;; declaration that the row touched no registry entry, so it is not ancestry.
   ;; :registry-path is relative to :registry-repo when the row names one
   ;; (four rows sign a p4ng registry, one a mathlib4 contract) -- resolving it
   ;; against futon2 would report "file not found" for five valid trails, which
   ;; is the pointer_check.bb roots-list defect in a new place: a pointer the
   ;; checker cannot resolve is not a pointer that does not resolve.
   (when (and (:covers-key i) (not= :none (:covers-key i)))
     [[:covers-key (str (when-let [r (:registry-repo i)] (str r "/"))
                        (or (:registry-path i) "holes/labs/wm-contract/aif-equations.edn"))]])))

(def rows (:items worklist))

(def new-rows (remove #(contains? grandfathered-ids (:id %)) rows))

(doseq [i rows]
  (let [claims (ancestry-claims i)
        new? (not (contains? grandfathered-ids (:id i)))]
    ;; Falsehood is refused on every row, grandfathered or not.
    (doseq [[label p] claims]
      (when-let [why (resolve-pointer p)]
        (refuse! (:id i) "declares" label (pr-str p) "which does not resolve --" why)))
    ;; Absence is refused only under jurisdiction.
    (when (and new? (empty? claims))
      (refuse! (:id i)
               (str "was minted after the gate (:ancestry-gate :landed-after "
                    (:landed-after gate) ") and names no commissioning ancestor:")
               "give it :basis (a file or file:line pointer to the ruling or plan"
               "that commissioned it), :epic (a plan artifact), or a :covers-key"
               "naming the registry entry it descends from. See"
               "N-process-trap-recording-conventions.md:273."))))

;; ---------------------------------------------------------------------------
;; Part 2 -- plan artifacts.

(def plans-reg
  (try (edn/read-string (slurp plans-path))
       (catch Exception e (die "cannot read" plans-path "--" (.getMessage e)))))

(when-not (= :wm/plan-ancestry-v1 (:schema plans-reg))
  (die plans-path "has unexpected schema" (pr-str (:schema plans-reg))))

(def plans (:plans plans-reg))
(when (empty? plans)
  (die plans-path "declares no plans -- an empty plan registry is a vacuous gate"))

(def by-artifact (into {} (map (juxt :artifact identity) plans)))

(defn- ancestor-path [p] (first (str/split (str (:ancestor/commissioning p)) #":")))

(defn- root-plan?
  "A ROOT plan's commissioning ancestor is a passage inside the plan itself --
   the commissioner's words, quoted verbatim (BUILD-tech-lead-charter.md:3,
   TN-edge-review-aif-wiring.md:19, P-assured-process.md:20 are all this shape).
   The pointer still has to resolve, but the plan is not its own ancestor for the
   clause-completeness check: comparing a clause set against itself would report
   every clause of every root as unaccounted, which is a self-loop wearing a
   finding's clothes."
  [plan]
  (= (:artifact plan) (ancestor-path plan)))

(defn- ancestor-clauses
  "The clause map of PLAN's ancestor, or nil when the ancestor is not a
   registered plan and has no :ancestor-clauses entry. nil means the ancestor's
   clauses are not enumerable, so no completeness claim can be made about it --
   stated, not assumed (T4b: an absence claim carries its search)."
  [plan]
  (when-not (root-plan? plan)
    (or (:clauses/declared (get by-artifact (ancestor-path plan)))
        (get (:ancestor-clauses plans-reg) (:ancestor/commissioning plan)))))

;; A trail that loops is not a trail. Root plans are excluded above; any other
;; cycle among registered plans is refused, or "ancestry" could be satisfied by
;; two plans pointing at each other.
(doseq [p plans]
  (loop [seen #{(:artifact p)} cur (ancestor-path p)]
    (when (and cur (not (root-plan? (get by-artifact cur (:artifact p)))))
      (if (contains? seen cur)
        (refuse! "plan" (:artifact p) "has a cyclic ancestry trail through" cur)
        (when-let [nxt (get by-artifact cur)]
          (recur (conj seen cur) (ancestor-path nxt)))))))

(def unaccounted-report (atom []))

(doseq [p plans]
  (let [art (:artifact p)
        new? (not (:grandfathered p))]
    (when-let [why (resolve-pointer art)]
      (refuse! "plan" art "does not resolve --" why))
    ;; §3, first refusal: a plan with no ancestor.
    (if (str/blank? (str (:ancestor/commissioning p)))
      (refuse! "plan" art "names no :ancestor/commissioning"
               "-- N-process-trap-recording-conventions.md:280 refuses a plan with no ancestor")
      (when-let [why (resolve-pointer (:ancestor/commissioning p))]
        (refuse! "plan" art "names an :ancestor/commissioning that does not resolve --" why)))
    ;; Every declared clause pointer resolves.
    (doseq [[cid ptr] (:clauses/declared p)]
      (when-let [why (resolve-pointer ptr)]
        (refuse! "plan" art "clause" cid "has a pointer that does not resolve --" why)))
    ;; A dropped clause carries author, date and rationale, or it is not a
    ;; dropped clause, it is a lost one.
    (doseq [d (:clauses/dropped p)]
      (doseq [k [:clause :by :at :decision/not-done]]
        (when (str/blank? (str (get d k)))
          (refuse! "plan" art "drops" (pr-str (:clause d)) "without" k
                   "-- a dropped clause carries author, date and rationale"
                   "(N-process-trap-recording-conventions.md:275)"))))
    ;; Inherited and dropped clauses must be clauses the ancestor actually
    ;; declares. A claimed inheritance of a clause that is not there is a
    ;; fabricated trail, which is worse than no trail.
    (let [anc (ancestor-clauses p)]
      (when anc
        (doseq [cid (:clauses/inherited p)]
          (when-not (contains? anc cid)
            (refuse! "plan" art "claims to inherit" cid
                     "which its ancestor" (ancestor-path p) "does not declare")))
        (doseq [d (:clauses/dropped p)]
          (when-not (contains? anc (:clause d))
            (refuse! "plan" art "claims to drop" (pr-str (:clause d))
                     "which its ancestor" (ancestor-path p) "does not declare")))
        ;; THE SECOND REFUSAL: a clause of the ancestor that is neither carried
        ;; forward nor dropped-with-a-record has been dropped silently.
        (let [accounted (into (set (:clauses/inherited p))
                              (map :clause (:clauses/dropped p)))
              missing (remove accounted (keys anc))]
          (when (seq missing)
            (swap! unaccounted-report conj [art (vec missing)])
            (when new?
              (refuse! "plan" art "drops" (pr-str (vec missing)) "from its ancestor"
                       (ancestor-path p)
                       "with no :clauses/dropped record: a clause is carried forward"
                       "in :clauses/inherited or dropped with an author, a date and a"
                       ":decision/not-done rationale"))))))
    ;; §3, second refusal: a consumer ordered before its producer.
    (let [order (:order p)
          idx (into {} (map-indexed (fn [n c] [c n]) order))]
      (doseq [[producer consumer] (:deps/derived p)]
        (cond
          (and (seq order) (not (contains? idx producer)))
          (refuse! "plan" art "declares a dependency on producer" producer
                   "which its :order does not schedule")
          (and (seq order) (not (contains? idx consumer)))
          (refuse! "plan" art "declares a dependency for consumer" consumer
                   "which its :order does not schedule")
          (and (seq order) (>= (idx producer) (idx consumer)))
          (refuse! "plan" art "orders the consumer" consumer "before its producer"
                   producer "-- N-process-trap-recording-conventions.md:280"))))))

;; ---------------------------------------------------------------------------

(let [bad @problems]
  (doseq [b bad] (binding [*out* *err*] (println "ancestry_check: REFUSED --" b)))
  (println (format (str "ancestry_check: %d rows (%d grandfathered at %s, %d under jurisdiction), "
                        "%d plans (%d grandfathered), %d plans carry unaccounted ancestor clauses %s, "
                        "%d refusals")
                   (count rows)
                   (count (filter #(contains? grandfathered-ids (:id %)) rows))
                   (subs (str (:landed-after gate)) 0 (min 8 (count (str (:landed-after gate)))))
                   (count new-rows)
                   (count plans)
                   (count (filter :grandfathered plans))
                   (count @unaccounted-report)
                   (pr-str (into {} @unaccounted-report))
                   (count bad)))
  (when (seq bad) (System/exit 1)))
