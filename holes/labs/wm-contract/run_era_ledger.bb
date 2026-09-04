#!/usr/bin/env bb
;; RE2 -- THE RUN-ERA LEDGER: validator, validated append API, and the folds.
;;
;;   bb holes/labs/wm-contract/run_era_ledger.bb --check
;;   bb holes/labs/wm-contract/run_era_ledger.bb --report [outdir]
;;   bb holes/labs/wm-contract/run_era_ledger.bb --self-test [outdir]
;;   bb holes/labs/wm-contract/run_era_ledger.bb --append '<edn row map>'
;;   bb holes/labs/wm-contract/run_era_ledger.bb --append-file <path>
;;   (any mode) --ledger <path>   target a copy instead of the committed ledger
;;
;; `--check` is the gate: bare exit 0 when run-era-ledger.edn conforms to the
;; schema THAT FILE DECLARES, non-zero otherwise. There is no second copy of the
;; row shape in this script -- it reads :ledger/schema and :ledger/check-catalogue
;; out of the ledger, so file and validator cannot drift (the discipline of
;; u41_tension_ledger.bb, which this clones).
;;
;; `append-row!` is the SOLE write path. It validates the ledger as it stands,
;; validates the ledger as it would stand, refuses on any defect, and replaces
;; the file atomically (tmp + ATOMIC_MOVE). Replaying an identical row is
;; :already-present rather than a second row; a row that reuses an existing
;; (run-id, check-id) with any different authored field is a conflict and throws.
;;
;; APPEND-ONLY IS MECHANISED, NOT PROMISED. Every row carries :row/sha, a
;; SHA-256 chaining the previous row's sha to this row's authored fields in
;; canonical (sorted-key) form; :ledger/head-sha is the last row's sha. Editing
;; a row in place, or dropping the tail, breaks the chain and `--check` fails.
;; This is what makes "append-only" a check rather than a convention.
;;
;; The hand-written header comment block of run-era-ledger.edn is preserved
;; verbatim across appends: the writer keeps every leading comment/blank line
;; and re-emits it above the pretty-printed data. Control 9 shows it surviving.
;;
;; No tick, no run lock, no substrate call, no network. --check and --report are
;; read-only. --self-test writes only under a temporary directory and its outdir.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def repo-root ;; derived from the script location (holes/labs/wm-contract under the root) so a worktree run targets its own checkout -- U28z reviewer finding, 2026-09-03
  (-> (java.io.File. *file*) .getAbsoluteFile .getParentFile .getParentFile .getParentFile .getParentFile .getPath))
(def lab (io/file repo-root "holes/labs/wm-contract"))
(def default-ledger-path (io/file lab "run-era-ledger.edn"))

(def genesis "genesis")

;; ---------------------------------------------------------------------------
;; Canonical form and the sha chain.
;; ---------------------------------------------------------------------------

(defn- hex [^bytes bs]
  (str/join (map #(format "%02x" (bit-and % 0xff)) bs)))

(defn sha256 [^String s]
  (hex (.digest (java.security.MessageDigest/getInstance "SHA-256")
                (.getBytes s "UTF-8"))))

(defn canonical
  "The row's authored fields, sorted by key, printed. :row/sha is excluded --
   it is the digest OF this, so it cannot be inside it."
  [row]
  (pr-str (into (sorted-map) (dissoc row :row/sha))))

(defn row-sha [prev-sha row]
  (sha256 (str (or prev-sha genesis) "\n" (canonical row))))

(defn chain-shas
  "The sha each row SHOULD carry, in order."
  [rows]
  (loop [prev genesis, [r & more] rows, acc []]
    (if (nil? r)
      acc
      (let [s (row-sha prev r)]
        (recur s more (conj acc s))))))

;; ---------------------------------------------------------------------------
;; Pointer resolution.
;; ---------------------------------------------------------------------------

(defn strip-line-suffix
  "\"path:12\" and \"path:12-34\" resolve as \"path\"; anything else is the path."
  [p]
  (str/replace p #":\d+(-\d+)?$" ""))

(defn artifact-resolves? [p]
  (and (string? p)
       (not (str/blank? p))
       (let [root (.getCanonicalFile (io/file repo-root))
             f (.getCanonicalFile (io/file root (strip-line-suffix p)))
             root-prefix (str (.getPath root) java.io.File/separator)]
         (and (str/starts-with? (.getPath f) root-prefix)
              (.isFile f)))))

(defn instant? [s]
  (and (string? s)
       (try (java.time.Instant/parse s) true
            (catch Exception _ false))))

;; ---------------------------------------------------------------------------
;; Validation, against the schema the ledger declares about itself.
;; ---------------------------------------------------------------------------

(defn- blank-string? [x] (and (string? x) (str/blank? x)))

(defn check-map
  "Required keys present and non-nil/non-blank; no key outside required+optional;
   every enum-constrained value inside its declared set."
  [kind spec m]
  (let [{:keys [required optional enums]} spec
        allowed (set (concat required optional))
        id (or (:row/seq m) (:check/id m) m)]
    (concat
     (for [k required :when (or (not (contains? m k)) (nil? (get m k)) (blank-string? (get m k)))]
       {:kind kind :subject id :defect :missing-required-key :key k})
     (for [k (keys m) :when (not (contains? allowed k))]
       {:kind kind :subject id :defect :undeclared-key :key k})
     (for [[k allowed-vals] enums :when (contains? m k)
           :when (not (contains? allowed-vals (get m k)))]
       {:kind kind :subject id :defect :value-outside-declared-set
        :key k :value (get m k) :declared allowed-vals}))))

(defn validate
  "Every rule the ledger's :rules strings state, mechanised. Returns a vector of
   defect maps; empty means the ledger conforms."
  [ledger]
  (let [schema (:ledger/schema ledger)
        rows (vec (:rows ledger))
        catalogue (:ledger/check-catalogue ledger)
        check-ids (set (map :check/id catalogue))
        expected (chain-shas rows)]
    (vec
     (concat
      ;; the catalogue is itself a declared shape -- it is the check-id enum
      (mapcat #(check-map :check (:check schema) %) catalogue)
      (for [[id n] (frequencies (map :check/id catalogue)) :when (> n 1)]
        {:kind :check :subject id :defect :duplicate-check-id :n n})
      ;; row shape
      (mapcat #(check-map :row (:row schema) %) rows)
      ;; :row/check-id is drawn from the catalogue -- a typo cannot invent a check
      (for [r rows :when (not (contains? check-ids (:row/check-id r)))]
        {:kind :row :subject (:row/seq r) :defect :check-id-not-in-catalogue
         :value (:row/check-id r) :declared check-ids})
      ;; one row per (run-id, check-id)
      (for [[pair n] (frequencies (map (juxt :row/run-id :row/check-id) rows)) :when (> n 1)]
        {:kind :ledger :subject pair :defect :duplicate-run-id-check-id-pair :n n})
      ;; :row/seq unique, contiguous, ascending from 1
      (for [[s n] (frequencies (map :row/seq rows)) :when (> n 1)]
        {:kind :row :subject s :defect :duplicate-row-seq :n n})
      (when (not= (map :row/seq rows) (range 1 (inc (count rows))))
        [{:kind :ledger :subject :rows :defect :row-seq-not-contiguous-ascending-from-1
          :value (mapv :row/seq rows)}])
      ;; the artifact must be readable, or the verdict is not evidence
      (for [r rows :when (not (artifact-resolves? (:row/artifact r)))]
        {:kind :row :subject (:row/seq r) :defect :artifact-pointer-does-not-resolve
         :value (:row/artifact r)})
      ;; the honest store rule, mechanised
      (for [r rows
            :when (and (= :typed-absence (:row/verdict r))
                       (or (nil? (:row/notes r)) (blank-string? (:row/notes r))))]
        {:kind :row :subject (:row/seq r) :defect :typed-absence-without-notes})
      ;; a row is a point in a series, so its time must be a time
      (for [r rows :when (not (instant? (:row/at r)))]
        {:kind :row :subject (:row/seq r) :defect :at-is-not-an-iso-instant
         :value (:row/at r)})
      ;; the append-only chain
      (for [[r expect] (map vector rows expected)
            :when (not= (:row/sha r) expect)]
        {:kind :row :subject (:row/seq r) :defect :row-sha-mismatch
         :recorded (:row/sha r) :recomputed expect})
      (when (not= (:ledger/head-sha ledger) (last expected))
        [{:kind :ledger :subject :head :defect :head-sha-is-not-the-last-row-sha
          :recorded (:ledger/head-sha ledger) :recomputed (last expected)}])))))

;; ---------------------------------------------------------------------------
;; The write path.
;; ---------------------------------------------------------------------------

(defn header-of
  "The hand-written comment block at the top of the ledger file: every leading
   line that is a ;; comment or blank. Preserved verbatim across appends, so the
   schema documentation survives a write."
  [text]
  (let [lines (str/split-lines text)
        head (take-while #(or (str/blank? %) (str/starts-with? (str/triml %) ";;")) lines)]
    (if (seq head) (str (str/join "\n" head) "\n") "")))

(defn read-ledger [path]
  (let [text (slurp path)]
    {:text text :header (header-of text) :data (edn/read-string text)}))

(defn write-ledger!
  "Atomic tmp + ATOMIC_MOVE replace, header re-emitted above the data."
  [path header data]
  (let [f (io/file path)
        tmp (io/file (.getParentFile (.getAbsoluteFile f))
                     (str (.getName f) ".re2-append"))]
    (spit tmp (str header (with-out-str (pp/pprint data))))
    (java.nio.file.Files/move
     (.toPath tmp) (.toPath (.getAbsoluteFile f))
     (into-array java.nio.file.CopyOption
                 [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                  java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))

(def authored-keys
  "The fields a depositor writes. :row/seq and :row/sha are assigned by the API,
   so identity comparison for replay ignores them."
  [:row/run-id :row/check-id :row/verdict :row/artifact :row/at :row/author
   :row/notes :row/deposited-by])

(defn- authored [row] (select-keys row authored-keys))

(defn append-row!
  "Validated append-only API and the only write path into the ledger.
   Returns {:status :appended | :already-present ...}; throws on any refusal."
  ([row] (append-row! default-ledger-path row))
  ([path row]
   (let [{:keys [header data]} (read-ledger path)
         existing-defects (validate data)
         _ (when (seq existing-defects)
             (throw (ex-info "existing run-era ledger is invalid; refusing to append"
                             {:defects existing-defects})))
         proposed (authored row)
         _ (when-let [extra (seq (remove (set authored-keys) (keys row)))]
             (throw (ex-info "proposed row carries keys the depositor may not set"
                             {:keys (vec extra)})))
         old (some #(when (= [(:row/run-id row) (:row/check-id row)]
                             [(:row/run-id %) (:row/check-id %)]) %)
                   (:rows data))]
     (cond
       (and old (= proposed (authored old)))
       {:status :already-present :run-id (:row/run-id row) :check-id (:row/check-id row)
        :row/seq (:row/seq old) :row/sha (:row/sha old)}

       old
       (throw (ex-info "divergent row for an existing (run-id, check-id); the ledger is append-only"
                       {:run-id (:row/run-id row) :check-id (:row/check-id row)
                        :existing (authored old) :proposed proposed
                        :differing (vec (sort (keep (fn [k] (when (not= (get proposed k) (get (authored old) k)) k))
                                                    authored-keys)))}))

       :else
       (let [rows (vec (:rows data))
             seq-n (inc (count rows))
             prev (or (:row/sha (last rows)) genesis)
             base (assoc proposed :row/seq seq-n)
             sha (row-sha prev base)
             new-row (assoc base :row/sha sha)
             candidate (assoc data :rows (conj rows new-row) :ledger/head-sha sha)
             defects (validate candidate)]
         (when (seq defects)
           (throw (ex-info "proposed run-era row is invalid; nothing written"
                           {:defects defects})))
         (write-ledger! path header candidate)
         {:status :appended :run-id (:row/run-id row) :check-id (:row/check-id row)
          :row/seq seq-n :row/sha sha})))))

;; ---------------------------------------------------------------------------
;; The folds. There is no status field in the ledger; status is computed here.
;; ---------------------------------------------------------------------------

(defn fold-by-run
  "Per run: what each check said, which catalogued checks never deposited, and
   the run's rolled-up status. A run missing a catalogued check is :incomplete,
   never green -- the honest store rule at the run level."
  [ledger]
  (let [catalogue (set (map :check/id (:ledger/check-catalogue ledger)))]
    (vec
     (for [[run-id rows] (sort-by key (group-by :row/run-id (:rows ledger)))]
       (let [by-check (into (sorted-map) (map (juxt :row/check-id :row/verdict)) rows)
             missing (vec (sort (remove (set (keys by-check)) catalogue)))
             verdicts (set (vals by-check))]
         {:run-id run-id
          :checks by-check
          :checks-not-deposited missing
          :first-at (apply min-key #(.toEpochMilli (java.time.Instant/parse %)) (map :row/at rows))
          :status (cond (seq missing) :incomplete
                        (contains? verdicts :red) :red
                        (contains? verdicts :typed-absence) :incomplete
                        :else :green)})))))

(defn fold-by-check
  "Per check: its verdict series across runs, ordered in time -- the
   time-correlation the epic exists for (era flags in the Field Desk sense)."
  [ledger]
  (vec
   (for [[check-id rows] (sort-by key (group-by :row/check-id (:rows ledger)))]
     {:check-id check-id
      :runs (count rows)
      :series (mapv (fn [r] [(:row/at r) (:row/run-id r) (:row/verdict r)])
                    (sort-by :row/at rows))})))

;; ---------------------------------------------------------------------------
;; Controls. Every negative is exercised, not asserted.
;; ---------------------------------------------------------------------------

(def synthetic-row
  "The row the self-test appends to a TEMPORARY COPY of the ledger. Its :row/at
   is a fixed literal and its artifact is this script, so the self-test is
   deterministic and its artifact pointer really resolves."
  {:row/run-id "0000-00-00-self-test"
   :row/check-id :flip-readiness
   :row/verdict :green
   :row/artifact "holes/labs/wm-contract/run_era_ledger.bb"
   :row/at "2026-01-01T00:00:00Z"
   :row/author "run_era_ledger.bb --self-test"
   :row/notes "synthetic; written only to a temporary copy, never to the committed ledger"})

(defn- refusal
  "Run f, expecting it to throw. Returns the defects/keys it threw with."
  [f]
  (try (let [r (f)] {:threw? false :returned r})
       (catch clojure.lang.ExceptionInfo e
         {:threw? true :message (ex-message e) :data (ex-data e)})
       (catch Exception e
         {:threw? true :message (str (class e) ": " (.getMessage e))})))

(defn- defect-kinds [r]
  (set (map :defect (get-in r [:data :defects]))))

(defn- refused-for?
  "True when the refusal threw AND at least one of its defects matches every
   key/value in `expected`. Matching on the DEFECT rather than merely on \"it
   threw\" is what stops a control passing for an unrelated reason."
  [r expected]
  (boolean
   (and (:threw? r)
        (some (fn [d] (= expected (select-keys d (keys expected))))
              (get-in r [:data :defects])))))

(defn self-test
  "Appends one synthetic row to a temp copy, replays it, and exercises the
   rejecting controls. Returns an ordered map of control -> result."
  []
  (let [tmpdir (java.nio.file.Files/createTempDirectory
                "re2-run-era" (into-array java.nio.file.attribute.FileAttribute []))
        tmp (io/file (.toFile tmpdir) "run-era-ledger.edn")
        committed (read-ledger default-ledger-path)]
    (io/copy (io/file default-ledger-path) tmp)
    (let [empty-defects (validate (:data (read-ledger tmp)))
          appended (append-row! tmp synthetic-row)
          after (read-ledger tmp)
          after-defects (validate (:data after))
          replay (append-row! tmp synthetic-row)
          after-replay (read-ledger tmp)
          ;; the negative controls
          c-missing-run-id (refusal #(append-row! tmp (dissoc synthetic-row :row/run-id)))
          c-unknown-check (refusal #(append-row! tmp (assoc synthetic-row
                                                            :row/check-id :not-a-real-check
                                                            :row/run-id "0000-00-00-control-b")))
          c-dangling (refusal #(append-row! tmp (assoc synthetic-row
                                                       :row/artifact "holes/labs/wm-contract/no-such-artifact.edn"
                                                       :row/run-id "0000-00-00-control-c")))
          c-outside-repo (refusal #(append-row! tmp (assoc synthetic-row
                                                           :row/artifact "../futon2"
                                                           :row/run-id "0000-00-00-control-c2")))
          c-bad-verdict (refusal #(append-row! tmp (assoc synthetic-row
                                                          :row/verdict :probably-fine
                                                          :row/run-id "0000-00-00-control-d")))
          c-divergent (refusal #(append-row! tmp (assoc synthetic-row :row/verdict :red)))
          c-absence-silent (refusal #(append-row! tmp (-> synthetic-row
                                                          (assoc :row/verdict :typed-absence
                                                                 :row/run-id "0000-00-00-control-f")
                                                          (dissoc :row/notes))))
          ;; in-place edit: mutate a written row's verdict WITHOUT touching its sha.
          ;; :red is a legal enum value, so only the chain can catch this.
          edited (update (:data after-replay) :rows
                         (fn [rs] (assoc-in (vec rs) [0 :row/verdict] :red)))
          edited-defects (validate edited)
          ;; tail truncation: drop the last row, leave head-sha as it was
          truncated (update (:data after-replay) :rows (comp vec butlast))
          truncated-defects (validate truncated)
          ;; a depositor may not assign :row/seq or :row/sha
          c-forged-sha (refusal #(append-row! tmp (assoc synthetic-row
                                                         :row/run-id "0000-00-00-control-h"
                                                         :row/sha "deadbeef")))
          final (read-ledger tmp)]
      (array-map
       :positive/c1-committed-ledger-is-green
       {:defects empty-defects :rows (count (:rows (:data committed)))
        :pass? (empty? empty-defects)
        :why "the committed ledger -- empty at RE2 -- conforms to the schema it declares; this is what `--check` gates on"}

       :positive/c2-synthetic-row-appends-and-revalidates-green
       {:result appended :defects after-defects :rows (count (:rows (:data after)))
        :pass? (and (= :appended (:status appended)) (empty? after-defects)
                    (= 1 (count (:rows (:data after)))))
        :why "one row appended through the API, and the ledger it produced validates green -- otherwise every control below is a statement about a ledger nothing can write"}

       :positive/c3-identical-append-is-already-present
       {:result replay :rows (count (:rows (:data after-replay)))
        :byte-identical? (= (:text after) (:text after-replay))
        :pass? (and (= :already-present (:status replay))
                    (= 1 (count (:rows (:data after-replay))))
                    (= (:text after) (:text after-replay)))
        :why "replaying the identical row is a no-op returning :already-present and leaving the file byte-identical -- RE3's --deposit paths are re-runnable because of this"}

       :negative/c4-missing-run-id-is-refused
       {:defects (defect-kinds c-missing-run-id) :threw? (:threw? c-missing-run-id)
        :pass? (refused-for? c-missing-run-id
                             {:defect :missing-required-key :key :row/run-id})
        :why "a row with no run-id cannot be time-correlated to anything, which is the whole point of the ledger; it is refused before any write"}

       :negative/c5-unknown-check-id-is-refused
       {:defects (defect-kinds c-unknown-check) :threw? (:threw? c-unknown-check)
        :pass? (refused-for? c-unknown-check
                             {:defect :check-id-not-in-catalogue :value :not-a-real-check})
        :why "check ids come from :ledger/check-catalogue and nowhere else, so a typo cannot quietly mint a check nobody runs"}

       :negative/c6-dangling-artifact-pointer-is-refused
       {:defects (defect-kinds c-dangling) :threw? (:threw? c-dangling)
        :pass? (refused-for? c-dangling
                             {:defect :artifact-pointer-does-not-resolve
                              :value "holes/labs/wm-contract/no-such-artifact.edn"})
        :why "a verdict whose artifact cannot be opened is a claim without evidence; the campaign standard is a pointer that resolves"}

       :negative/c6b-artifact-pointer-outside-repository-is-refused
       {:defects (defect-kinds c-outside-repo) :threw? (:threw? c-outside-repo)
        :pass? (refused-for? c-outside-repo
                             {:defect :artifact-pointer-does-not-resolve
                              :value "../futon2"})
        :why "repository-relative means the canonical target must be a file inside futon2; traversal and directory pointers are not evidence artifacts"}

       :negative/c7-malformed-verdict-is-refused
       {:defects (defect-kinds c-bad-verdict) :threw? (:threw? c-bad-verdict)
        :pass? (refused-for? c-bad-verdict
                             {:defect :value-outside-declared-set
                              :key :row/verdict :value :probably-fine})
        :why "the verdict enum is declared in the ledger file; a value outside it is refused rather than folded as an unknown status"}

       :negative/c8-divergent-duplicate-pair-is-refused
       {:threw? (:threw? c-divergent) :differing (get-in c-divergent [:data :differing])
        :pass? (and (:threw? c-divergent)
                    (= [:row/verdict] (get-in c-divergent [:data :differing])))
        :why "the same (run-id, check-id) with a different verdict is the ledger being rewritten by another name; an exact replay is :already-present, a divergence throws and names the differing field"}

       :negative/c9-in-place-edit-of-a-written-row-is-refused
       {:defects (set (map :defect edited-defects))
        :pass? (contains? (set (map :defect edited-defects)) :row-sha-mismatch)
        :why "the edit flips a written row's verdict from :green to :red -- both legal enum values, so no shape rule can see it. The sha chain does, which is what makes append-only a check rather than a convention"}

       :negative/c10-tail-truncation-is-refused
       {:defects (set (map :defect truncated-defects))
        :pass? (contains? (set (map :defect truncated-defects))
                          :head-sha-is-not-the-last-row-sha)
        :why "dropping the last row leaves every remaining sha internally consistent; :ledger/head-sha is what anchors the end of the chain, so a quiet truncation is caught too"}

       :negative/c11-depositor-may-not-assign-the-sha
       {:threw? (:threw? c-forged-sha) :keys (get-in c-forged-sha [:data :keys])
        :pass? (and (:threw? c-forged-sha) (= [:row/sha] (get-in c-forged-sha [:data :keys])))
        :why "the chain is only worth checking if the API assigns it; a caller-supplied :row/sha or :row/seq is refused"}

       :negative/c12-typed-absence-with-no-notes-is-refused
       {:defects (defect-kinds c-absence-silent) :threw? (:threw? c-absence-silent)
        :pass? (refused-for? c-absence-silent {:defect :typed-absence-without-notes})
        :why "the honest store rule: an absence must say what the store did not hold, or it is silence wearing a verdict"}

       :positive/c13-the-schema-header-survives-a-write
       {:header-bytes (count (:header final))
        :pass? (and (= (:header committed) (:header final))
                    (str/includes? (:header final) "APPEND-ONLY"))
        :pass-note "compared byte-for-byte against the committed file's header"
        :why "the writer pretty-prints the data, so the hand-written schema documentation would be the natural casualty of the first append; it is re-emitted verbatim instead"}

       :positive/c14-no-write-touched-the-committed-ledger
       {:committed-rows (count (:rows (:data (read-ledger default-ledger-path))))
        :pass? (= (:text committed) (:text (read-ledger default-ledger-path)))
        :why "every append above went to a temp copy; the committed ledger is byte-identical to what the self-test started with, which is why `--check` on it stays a check of an empty ledger"}))))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn- pop-opt
  "Remove `flag` and the value after it from args; return [remaining value]."
  [args flag]
  (loop [in (seq args), kept [], found nil]
    (if-let [a (first in)]
      (if (= a flag)
        (recur (nnext in) kept (second in))
        (recur (next in) (conj kept a) found))
      [kept found])))

(defn- report-lines [ledger defects]
  (let [runs (fold-by-run ledger)
        checks (fold-by-check ledger)
        lines (atom [])
        emit (fn [& xs] (swap! lines conj (apply str xs)))]
    (emit "RE2 — RUN-ERA LEDGER: one row per (run-id, check-id), folded")
    (emit (format "  %d rows, %d catalogued checks, %d defects against the schema the ledger declares"
                  (count (:rows ledger)) (count (:ledger/check-catalogue ledger)) (count defects)))
    (doseq [d defects] (emit "    " (pr-str d)))
    (emit "")
    (emit "CHECK CATALOGUE — the check-id enum, each entry naming its machinery")
    (doseq [c (:ledger/check-catalogue ledger)]
      (emit (format "  %-30s %s" (str (:check/id c)) (:check/machinery c))))
    (emit "")
    (emit "BY RUN — a run missing a catalogued check is :incomplete, never green")
    (if (empty? runs)
      (emit "  (no runs: the ledger is empty. RE3 wires the first three checks to deposit; RE5 is the first correlated run.)")
      (doseq [r runs]
        (emit (format "  %-28s %-12s %d checks, not deposited: %s"
                      (:run-id r) (str (:status r)) (count (:checks r))
                      (pr-str (:checks-not-deposited r))))))
    (emit "")
    (emit "BY CHECK — the verdict series across runs, which is the time-correlation")
    (if (empty? checks)
      (emit "  (no series yet)")
      (doseq [c checks]
        (emit (format "  %-30s %d runs %s" (str (:check-id c)) (:runs c) (pr-str (:series c))))))
    @lines))

(defn -main [& args]
  (let [[args ledger-arg] (pop-opt (vec args) "--ledger")
        path (io/file (or ledger-arg default-ledger-path))
        mode (or (first (filter #(str/starts-with? % "--") args)) "--check")
        outdir-arg (first (remove #(str/starts-with? % "--") args))]
    (case mode
      "--check"
      (let [{:keys [data]} (read-ledger path)
            defects (validate data)]
        (println (str/join "\n" (report-lines data defects)))
        (when (seq defects) (System/exit 1)))

      "--report"
      (let [outdir (io/file (or outdir-arg (str (io/file lab "runs/RE2-run-era-ledger"))))
            _ (.mkdirs outdir)
            {:keys [data]} (read-ledger path)
            defects (validate data)
            lines (report-lines data defects)
            report {:reader {:id :re2-run-era-ledger :version "v1" :row :RE2
                             :script "holes/labs/wm-contract/run_era_ledger.bb"
                             :ledger "holes/labs/wm-contract/run-era-ledger.edn"
                             :implements "EPIC-run-era.md:31-60"
                             :read-only true
                             :writes-only-under (.getPath outdir)}
                    :schema (:ledger/schema data)
                    :check-catalogue (:ledger/check-catalogue data)
                    :validation {:defects defects :conforms? (empty? defects)}
                    :head-sha (:ledger/head-sha data)
                    :by-run (fold-by-run data)
                    :by-check (fold-by-check data)}]
        (spit (io/file outdir "RE2-RUN-ERA-LEDGER.txt") (str (str/join "\n" lines) "\n"))
        (spit (io/file outdir "run-era-report.edn") (with-out-str (pp/pprint report)))
        (println (str/join "\n" lines))
        (println)
        (println "wrote" (str (.getPath outdir) "/RE2-RUN-ERA-LEDGER.txt")
                 "and" (str (.getPath outdir) "/run-era-report.edn"))
        (when (seq defects) (System/exit 1)))

      "--self-test"
      (let [outdir (io/file (or outdir-arg (str (io/file lab "runs/RE2-run-era-ledger"))))
            _ (.mkdirs outdir)
            ctrls (self-test)
            lines (atom [])
            emit (fn [& xs] (swap! lines conj (apply str xs)))]
        (emit "RE2 — RUN-ERA LEDGER SELF-TEST: the append API and its rejecting controls")
        (emit "every append below goes to a TEMPORARY COPY; the committed ledger is not written")
        (emit "")
        (doseq [[k v] ctrls]
          (emit (format "  %-58s pass?=%s" (str k) (pr-str (:pass? v)))))
        (emit "")
        (emit (format "  %d controls, %d negative (each shown REFUSING), %d positive"
                      (count ctrls)
                      (count (filter #(str/starts-with? (str (key %)) ":negative/") ctrls))
                      (count (filter #(str/starts-with? (str (key %)) ":positive/") ctrls))))
        (spit (io/file outdir "RE2-SELF-TEST.txt") (str (str/join "\n" @lines) "\n"))
        (spit (io/file outdir "run-era-self-test.edn")
              (with-out-str (pp/pprint {:reader {:id :re2-run-era-ledger-self-test :row :RE2
                                                 :script "holes/labs/wm-contract/run_era_ledger.bb"
                                                 :writes-only-under (.getPath outdir)
                                                 :ledger-under-test "a temporary copy of run-era-ledger.edn"}
                                        :controls (mapv (fn [[k v]] [k v]) ctrls)})))
        (println (str/join "\n" @lines))
        (println)
        (println "wrote" (str (.getPath outdir) "/RE2-SELF-TEST.txt")
                 "and" (str (.getPath outdir) "/run-era-self-test.edn"))
        (when-not (every? :pass? (vals ctrls)) (System/exit 2)))

      "--append"
      (let [row (edn/read-string (or (second (drop-while #(not= "--append" %) args))
                                     (throw (ex-info "--append needs an EDN row map" {}))))]
        (prn (append-row! path row)))

      "--append-file"
      (let [f (or (second (drop-while #(not= "--append-file" %) args))
                  (throw (ex-info "--append-file needs a path" {})))]
        (prn (append-row! path (edn/read-string (slurp f)))))

      (do (println "unknown mode" mode)
          (println "modes: --check | --report [outdir] | --self-test [outdir] | --append '<edn>' | --append-file <path>  [--ledger <path>]")
          (System/exit 64)))))

(apply -main *command-line-args*)
