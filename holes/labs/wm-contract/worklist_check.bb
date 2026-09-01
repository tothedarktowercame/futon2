#!/usr/bin/env bb
;; worklist_check.bb -- prove the ledger before anyone acts on it (class 6a).
(require '[clojure.edn :as edn])
(def w (edn/read-string (slurp (or (first *command-line-args*)
                                    (str (.getParent (.getAbsoluteFile (java.io.File. *file*))) "/worklist.edn")))))
(defn die [& m] (binding [*out* *err*] (apply println "worklist_check:" m)) (System/exit 1))
(when-not (= :wm/worklist-v1 (:schema w)) (die "unexpected schema"))
(def ids (map :id (:items w)))
(when (not= (count ids) (count (set ids))) (die "duplicate ids"))
(doseq [i (:items w)]
  (doseq [k [:id :class :status :owner :statement :acceptance]] (when-not (contains? i k) (die (:id i) "lacks" k)))
  (when-not (contains? (:classes w) (:class i)) (die (:id i) "unknown class" (:class i)))
  (when-not (contains? (:statuses w) (:status i)) (die (:id i) "unknown status" (:status i)))
  (when (and (= :J (:class i)) (not (or (= :needs-joe (:status i)) (and (= :done (:status i)) (:ruling i) (= "joe" (:reviewed-by i)))))) (die (:id i) "class J must be :needs-joe, or :done with :ruling and :reviewed-by joe"))
  (when (and (#{:done-unreviewed :done} (:status i)) (not (:evidence i))) (die (:id i) "done without :evidence"))
  (when (and (= :done (:status i)) (not (:reviewed-by i))) (die (:id i) ":done without :reviewed-by")))

;; ---------------------------------------------------------------------------
;; A signature must not come to describe a later file state (claude-1's
;; proposal, 2026-09-01, after it happened twice in one session: C14 then C15).
;; A row that signed a registry entry names the entry with :covers-key and the
;; sha it read with :review-covers. If that entry's value in the file today
;; differs from its value at that sha, the signature is on something else and
;; the row is NOT reviewed.
;;
;; Scoped by KEY, not by file: any later commit touching aif-equations.edn
;; would otherwise invalidate every prior signature, which would make the
;; ledger unusable and teach everyone to ignore the check.
;;
;; Rows without :covers-key are not checked, and the count of unchecked rows is
;; printed rather than passed over -- an absence should be visible, not implied.

(require '[clojure.java.shell :as shell]
         '[clojure.string :as str])

(require '[clojure.java.io :as io])
;; Resolve every path against the REPO ROOT, found from this script's own
;; location -- not from the caller's cwd. wm-edge-loop.sh calls this script by
;; absolute path from wherever the loop was started; run from /tmp the old
;; relative slurp and cwd-bound `git show` reported "the signed sha is not in
;; this history", a cwd problem wearing a history problem's message (C16 review).
(def script-dir (let [f (io/file *file*)] (.getParentFile (.getAbsoluteFile f))))
(def repo-root (let [{:keys [exit out]} (shell/sh "git" "rev-parse" "--show-toplevel" :dir script-dir)]
                 (when (zero? exit) (str/trim out))))
(when-not repo-root (die "cannot find the git repo root from" (str script-dir)))

(defn- registry-at
  "The registry map as of SHA, or nil when the file did not exist there."
  [sha path]
  (let [{:keys [exit out]} (shell/sh "git" "show" (str sha ":" path) :dir repo-root)]
    (when (zero? exit) (edn/read-string out))))

(def superseded-rows
  ;; TN 9a: an entry changed after signature gets a NEW row naming the row it
  ;; supersedes, and the old signature is not touched. So a superseded row's
  ;; signature is expected to be stale -- that is the point of it -- and the
  ;; check skips it. The superseding row must exist, or "superseded" is just a
  ;; word that switches the check off.
  (filter #(and (= :done (:status %)) (:superseded-by %)) (:items w)))

(doseq [i superseded-rows]
  (when-not (some #(= (:superseded-by i) (:id %)) (:items w))
    (die (:id i) "names" (:superseded-by i) "as superseding it, and no such row exists")))

(def signed-registry-rows
  (filter #(and (= :done (:status %)) (vector? (:covers-key %))
                (not (:superseded-by %)))
          (:items w)))

;; `:covers-key :none` is a DECLARATION that the row touched no registry entry
;; (C16 covers a script; a report row covers a .md). It keeps the unchecked
;; count honest without hiding anything -- but it must not become the way a row
;; opts out of the check, so a row claiming :none may not also name a registry
;; path (claude-1's convention, C16 review).
(doseq [i (:items w)]
  (when (and (= :none (:covers-key i)) (:registry-path i))
    (die (:id i) "declares :covers-key :none and also names a :registry-path"
         "-- one of them is wrong; :none means the row touched no registry entry")))

(def unchecked-signed-rows
  ;; Deliberately a SUPERSET: every signed C row without a :covers-key, not
  ;; only those that declare a :registry-path. Filtering on :registry-path
  ;; would report zero unchecked rows while C rows that did touch the registry
  ;; sat unchecked -- an absence reported as a success, which is the defect
  ;; class this ledger exists to catch.
  (filter #(and (= :done (:status %)) (= :C (:class %)) (not (:covers-key %)))
          (:items w)))

(def declared-no-registry-rows
  (filter #(and (= :done (:status %)) (= :none (:covers-key %))) (:items w)))

(doseq [i signed-registry-rows]
  (let [sha (:review-covers i)
        path (or (:registry-path i) "holes/labs/wm-contract/aif-equations.edn")
        key-path (:covers-key i)]
    (when-not sha
      (die (:id i) "has :covers-key but no :review-covers -- the signature names no sha"))
    (let [then (registry-at sha path)]
      (when-not then
        (die (:id i) "cannot read" path "at" sha "-- the signed sha is not in this history"))
      (let [was (get-in then key-path ::absent)
            now (get-in (edn/read-string (slurp (io/file repo-root path))) key-path ::absent)]
        (when (not= was now)
          (die (:id i) "signature is stale:" (pr-str key-path) "in" path
               "changed after" sha "was signed by" (:reviewed-by i)
               "-- set the row back to :done-unreviewed, or open a new row that supersedes it (TN 9a)"))))))

(def by-status (frequencies (map :status (:items w))))
(println (format "worklist_check: %d items OK; %s; %d signed registry entries verified unchanged since signature, %d superseded and skipped, %d declared :covers-key :none, %d signed registry rows carry no :covers-key and are NOT checked"
                 (count (:items w)) (pr-str by-status)
                 (count signed-registry-rows) (count superseded-rows)
                 (count declared-no-registry-rows)
                 (count unchecked-signed-rows)))
