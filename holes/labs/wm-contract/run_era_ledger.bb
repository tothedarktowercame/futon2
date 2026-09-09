#!/usr/bin/env bb
;; RE2 -- THE RUN-ERA LEDGER: validator, validated append API, and the folds.
;;
;;   bb holes/labs/wm-contract/run_era_ledger.bb --check
;;   bb holes/labs/wm-contract/run_era_ledger.bb --report [outdir]
;;   bb holes/labs/wm-contract/run_era_ledger.bb --self-test [outdir]
;;   bb holes/labs/wm-contract/run_era_ledger.bb --append '<edn row map>'
;;   bb holes/labs/wm-contract/run_era_ledger.bb --append-file <path>
;;   bb holes/labs/wm-contract/run_era_ledger.bb --deposit --run-id … --check-id …
;;       --verdict … --artifact … --author … [--notes …] [--at …]   (RE3)
;;   bb holes/labs/wm-contract/run_era_ledger.bb --catalogue-add --id … --machinery …
;;       --precedent … [--note …]                                    (RE7)
;;   bb holes/labs/wm-contract/run_era_ledger.bb --label-absence --run-id … --check-id …
;;       --kind … --basis … --by …                                   (RE6)
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

(require '[babashka.process :as process]
         '[clojure.edn :as edn]
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
        id (or (:row/seq m) (:check/id m)
               (when (:absence/check-id m) [(:absence/run-id m) (:absence/check-id m)])
               m)]
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
        absences (vec (:ledger/absence-kinds ledger))
        typed-absence-pairs (set (for [r rows :when (= :typed-absence (:row/verdict r))]
                                   [(:row/run-id r) (:row/check-id r)]))
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
      ;; RE6 -- the absence kinds. A label says which of the two responses a
      ;; standing typed absence calls for. It changes no verdict, so the one
      ;; thing it must not be is free-floating: it names a (run-id, check-id)
      ;; that really is a typed absence in this file, or it is a defect.
      (mapcat #(check-map :absence (:absence schema) %) absences)
      (for [[pair n] (frequencies (map (juxt :absence/run-id :absence/check-id) absences))
            :when (> n 1)]
        {:kind :absence :subject pair :defect :duplicate-absence-run-id-check-id-pair :n n})
      (for [a absences
            :let [pair [(:absence/run-id a) (:absence/check-id a)]]
            :when (not (contains? typed-absence-pairs pair))]
        {:kind :absence :subject pair :defect :absence-kind-names-no-typed-absence-row})
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
;; Minting a catalogued check. RE7.
;; ---------------------------------------------------------------------------
;;
;; The catalogue is the check-id enum, so a check that has no entry cannot
;; deposit at all -- `--deposit` is refused by :check-id-not-in-catalogue. Adding
;; the entry was the one part of the ledger with no API: the file says it is
;; never hand-edited, and the row shape is mechanised, but the catalogue was
;; reachable only with an editor. `catalogue-add!` closes that, through the same
;; validate-refuse-replace path `append-row!` takes.
;;
;; THE CATALOGUE'S OWN RULE IS ENFORCED HERE RATHER THAN ASSERTED. The ledger
;; states "a check enters the catalogue when its machinery exists"; this refuses
;; an entry whose :check/machinery does not begin with a pointer resolving to a
;; file under the pointer root. It is enforced at the ADD path and not added to
;; `validate`, because turning it on for the seven entries already written would
;; be a change to what `--check` means, and that is not this row's to make.
;;
;; ROWS ARE NOT TOUCHED. The sha chain covers rows only, so adding a catalogue
;; entry leaves every :row/sha and :ledger/head-sha exactly as they were; control
;; C22 shows it.

(def catalogue-authored-keys [:check/id :check/machinery :check/precedent :check/note])

(defn machinery-pointer
  "The leading path of a :check/machinery string -- the entries carry a pointer
   followed by an optional parenthetical gloss, so the pointer is the first
   whitespace-delimited token."
  [m]
  (when (string? m) (first (str/split (str/trim m) #"\s+"))))

(defn catalogue-add!
  "Validated append into :ledger/check-catalogue, and the only write path into it.
   Returns {:status :appended | :already-present ...}; throws on any refusal."
  ([entry] (catalogue-add! default-ledger-path entry))
  ([path entry]
   (let [{:keys [header data]} (read-ledger path)
         existing-defects (validate data)
         _ (when (seq existing-defects)
             (throw (ex-info "existing run-era ledger is invalid; refusing to add a check"
                             {:defects existing-defects})))
         proposed (select-keys entry catalogue-authored-keys)
         _ (when-let [extra (seq (remove (set catalogue-authored-keys) (keys entry)))]
             (throw (ex-info "proposed catalogue entry carries keys outside the declared shape"
                             {:keys (vec extra)})))
         old (some #(when (= (:check/id entry) (:check/id %)) %) (:ledger/check-catalogue data))]
     (cond
       (and old (= proposed (select-keys old catalogue-authored-keys)))
       {:status :already-present :check-id (:check/id entry)}

       old
       (throw (ex-info "divergent entry for an existing :check/id; the catalogue is append-only"
                       {:defects [{:kind :catalogue :subject (:check/id entry)
                                   :defect :divergent-existing-check-id
                                   :existing (select-keys old catalogue-authored-keys)
                                   :proposed proposed}]}))

       (not (artifact-resolves? (machinery-pointer (:check/machinery entry))))
       (throw (ex-info "a check enters the catalogue when its machinery exists; this pointer does not resolve"
                       {:defects [{:kind :catalogue :subject (:check/id entry)
                                   :defect :machinery-pointer-does-not-resolve
                                   :value (machinery-pointer (:check/machinery entry))}]}))

       :else
       (let [candidate (update data :ledger/check-catalogue (fnil conj []) proposed)
             defects (validate candidate)]
         (when (seq defects)
           (throw (ex-info "proposed catalogue entry is invalid; nothing written"
                           {:defects defects})))
         (write-ledger! path header candidate)
         {:status :appended :check-id (:check/id entry)
          :catalogue-size (count (:ledger/check-catalogue candidate))})))))

;; ---------------------------------------------------------------------------
;; Labelling a standing typed absence. RE6, under Joe's ruling of 2026-09-05.
;; ---------------------------------------------------------------------------
;;
;; Before the ruling, every typed absence folded to the single word :incomplete,
;; and that word stood for two states asking for opposite work: evidence that
;; had to be taken while the run ran and was not, and the run is closed
;; (instrument the NEXT run), and evidence that could still be reached without
;; changing what a run captures -- committed and derivable, or simply not
;; produced yet (wait for the datum, or derive it). EPIC-run-era.md:864-873 rules
;; that the fold must distinguish them; this is where the distinction is
;; recorded, and C511-repair-or-elaborate.md is the per-(run, check) measurement
;; each kind is read off.
;;
;; A LABEL IS NOT A REPAIR AND NOT A VERDICT. The rows are append-only, so a run
;; deposited :typed-absence stays :typed-absence -- C511-repair-or-elaborate.md
;; §5 is that a repair has nowhere to land. The label says which of the two
;; responses the standing absence calls for, nothing more, and the validator
;; refuses one that names a pair which is not a typed absence in this file.
;;
;; ROWS ARE NOT TOUCHED, for the same reason `catalogue-add!` does not touch
;; them: the sha chain covers :rows, so writing here leaves every :row/sha and
;; :ledger/head-sha exactly as they were (control C24 shows it).

(def absence-authored-keys
  [:absence/run-id :absence/check-id :absence/kind :absence/basis :absence/by])

(defn absence-kind!
  "Validated append into :ledger/absence-kinds, and the only write path into it.
   Returns {:status :appended | :already-present ...}; throws on any refusal."
  ([entry] (absence-kind! default-ledger-path entry))
  ([path entry]
   (let [{:keys [header data]} (read-ledger path)
         existing-defects (validate data)
         _ (when (seq existing-defects)
             (throw (ex-info "existing run-era ledger is invalid; refusing to label an absence"
                             {:defects existing-defects})))
         proposed (select-keys entry absence-authored-keys)
         _ (when-let [extra (seq (remove (set absence-authored-keys) (keys entry)))]
             (throw (ex-info "proposed absence label carries keys outside the declared shape"
                             {:keys (vec extra)})))
         pair [(:absence/run-id entry) (:absence/check-id entry)]
         old (some #(when (= pair [(:absence/run-id %) (:absence/check-id %)]) %)
                   (:ledger/absence-kinds data))]
     (cond
       (and old (= proposed (select-keys old absence-authored-keys)))
       {:status :already-present :pair pair :kind (:absence/kind old)}

       old
       (throw (ex-info "divergent label for an existing (run-id, check-id); absence kinds are append-only"
                       {:defects [{:kind :absence :subject pair
                                   :defect :divergent-existing-absence-kind
                                   :existing (select-keys old absence-authored-keys)
                                   :proposed proposed}]}))

       :else
       (let [candidate (update data :ledger/absence-kinds (fnil conj []) proposed)
             defects (validate candidate)]
         (when (seq defects)
           (throw (ex-info "proposed absence label is invalid; nothing written"
                           {:defects defects})))
         (write-ledger! path header candidate)
         {:status :appended :pair pair :kind (:absence/kind entry)
          :labels (count (:ledger/absence-kinds candidate))})))))

;; ---------------------------------------------------------------------------
;; Deposit -- the path a CHECK takes into the ledger. RE3.
;; ---------------------------------------------------------------------------
;;
;; A check does not build a row and it does not touch the EDN. It calls
;; `--deposit`, which assembles the row and hands it to `append-row!`. Two
;; things are enforced here rather than left to each check:
;;
;; THE ARTIFACT MUST BE COMMITTED AND UNMODIFIED. `--check` only requires the
;; pointer to resolve to a file on disk, which an uncommitted scratch file
;; does. A reviewer reading the row three months later has only the history, so
;; a row pointing at a file that was never committed -- or at a file whose
;; working copy has moved since -- points at nothing they can read. The deposit
;; refuses both, naming which.
;;
;; :row/at IS DERIVED, NOT STAMPED. Wall-clock at deposit time would make every
;; replay a different row, and a conflicting (run-id, check-id) rather than the
;; :already-present the append API offers. So the default :row/at is the
;; artifact's last commit instant -- when the evidence was recorded -- and a
;; check with a better answer (a verdict-time recorded inside the run store)
;; passes it explicitly with --at. Either way the same deposit repeats.

(defn- git-out
  "Run git in `dir`; {:exit n :out trimmed}. Never throws on a nonzero exit --
   a nonzero exit is an answer here (untracked, no history)."
  [dir & argv]
  (let [{:keys [exit out]} (apply process/shell
                                  {:dir dir :out :string :err :string :continue true}
                                  "git" argv)]
    {:exit exit :out (str/trim (or out ""))}))

(defn artifact-commit-state
  "What the history says about the artifact a proposed row points at:
   {:state :missing | :untracked | :dirty | :no-history | :committed, :at ...}.
   Only :committed may be deposited."
  [root relpath]
  (let [rel (strip-line-suffix relpath)
        f (io/file root rel)]
    (if-not (.isFile f)
      {:state :missing :path rel}
      (let [tracked (git-out root "ls-files" "--error-unmatch" "--" rel)
            status (git-out root "status" "--porcelain" "--" rel)
            last-commit (git-out root "log" "-1" "--format=%aI" "--" rel)]
        (cond
          (not (zero? (:exit tracked))) {:state :untracked :path rel}
          (not (str/blank? (:out status))) {:state :dirty :path rel :status (:out status)}
          (str/blank? (:out last-commit)) {:state :no-history :path rel}
          :else {:state :committed :path rel
                 :at (str (.toInstant (java.time.OffsetDateTime/parse (:out last-commit))))
                 :commit (:out (git-out root "log" "-1" "--format=%h" "--" rel))})))))

(defn deposit!
  "Assemble one row from a check's verdict and append it. Returns append-row!'s
   result map, with the artifact's commit state attached so the caller can print
   what the row was pinned to. Throws when the artifact is not committed-clean.
   `root` is the history the artifact is judged against; it is this repository
   everywhere but the self-test, which points it at a throwaway one."
  ([path opts] (deposit! path opts repo-root))
  ([path {:keys [run-id check-id verdict artifact author notes deposited-by at]} root]
   ;; Guard evidence is NOT an era-ledger deposit. Capture the computed verdict
   ;; before committed-artifact admission, including the first untracked pass.
   ;; With no capture path, existing callers' outputs and writes are unchanged.
   (when-let [capture (System/getenv "FUTON_WM_VERDICT_CAPTURE")]
     (spit capture
           (str (pr-str {:row/run-id run-id :row/check-id check-id
                         :row/verdict verdict :row/artifact artifact}) "\n")
           :append true))
   (let [state (artifact-commit-state root artifact)]
     (when-not (= :committed (:state state))
       (throw (ex-info (str "a ledger row may only point at a committed, unmodified file; this artifact is "
                            (name (:state state)))
                       {:defects [{:kind :deposit :subject artifact
                                   :defect (keyword (str "artifact-" (name (:state state))))
                                   :state state}]})))
     (assoc (append-row! path (cond-> {:row/run-id run-id
                                       :row/check-id check-id
                                       :row/verdict verdict
                                       :row/artifact artifact
                                       :row/at (or at (:at state))
                                       :row/author author}
                                notes (assoc :row/notes notes)
                                deposited-by (assoc :row/deposited-by deposited-by)))
            :artifact-state state))))

;; ---------------------------------------------------------------------------
;; The folds. There is no status field in the ledger; status is computed here.
;; ---------------------------------------------------------------------------

(defn absence-kind-index
  "{[run-id check-id] kind} over :ledger/absence-kinds."
  [ledger]
  (into {} (map (juxt (juxt :absence/run-id :absence/check-id) :absence/kind))
        (:ledger/absence-kinds ledger)))

(defn fold-by-run
  "Per run: what each check said, which catalogued checks never deposited, and
   the run's rolled-up status. A run missing a catalogued check is never green.

   THE STATUS WORD NAMES THE WORK THE RUN IS ASKING FOR (RE6, under Joe's ruling
   at EPIC-run-era.md:864-873). Before it, three unrelated states shared the word
   :incomplete, and a reader could not tell them apart:

     :incomplete                 a catalogued check has not been wired or not
                                 been run, or an absence carries no label --
                                 wire it, run it, or label it.
     :incomplete-uninstrumented  every check deposited and at least one absence
                                 is :not-contemporaneous: the evidence had to be
                                 taken while the run ran and the run is closed,
                                 so no deposit can close it. Instrument the next.
     :incomplete-data-pending    every check deposited and every absence is
                                 :data-pending: each could still be closed
                                 without changing what a run captures. Nothing to
                                 instrument -- wait for the datum, or derive it.

   The two-kind precedence is deliberate: a run carrying even one
   :not-contemporaneous absence reads as uninstrumented, because that is the
   response it needs, and :incomplete-data-pending is reserved for the run whose
   only absences are waiting on data (which is the case Joe's ruling says does
   not read as nonfunctional).

   :status-reason keeps naming WHICH checks, now split by kind, so a reader gets
   the actionable list and not only the word."
  [ledger]
  (let [catalogue (set (map :check/id (:ledger/check-catalogue ledger)))
        kinds (absence-kind-index ledger)]
    (vec
     (for [[run-id rows] (sort-by key (group-by :row/run-id (:rows ledger)))]
       (let [by-check (into (sorted-map) (map (juxt :row/check-id :row/verdict)) rows)
             missing (vec (sort (remove (set (keys by-check)) catalogue)))
             verdicts (set (vals by-check))
             absent (vec (sort (map key (filter #(= :typed-absence (val %)) by-check))))
             red (vec (sort (map key (filter #(= :red (val %)) by-check))))
             by-kind (group-by #(get kinds [run-id %] :unclassified) absent)
             not-contemporaneous (vec (sort (:not-contemporaneous by-kind)))
             data-pending (vec (sort (:data-pending by-kind)))
             unclassified (vec (sort (:unclassified by-kind)))]
         {:run-id run-id
          :checks by-check
          :checks-not-deposited missing
          :first-at (apply min-key #(.toEpochMilli (java.time.Instant/parse %)) (map :row/at rows))
          :status (cond (seq missing) :incomplete
                        (contains? verdicts :red) :red
                        (seq unclassified) :incomplete
                        (seq not-contemporaneous) :incomplete-uninstrumented
                        (seq data-pending) :incomplete-data-pending
                        :else :green)
          :status-reason (cond (seq missing) {:cause :checks-not-deposited :checks missing}
                               (contains? verdicts :red) {:cause :red-verdict :checks red}
                               (seq absent)
                               {:cause :typed-absences :checks absent
                                :not-contemporaneous not-contemporaneous
                                :data-pending data-pending
                                :unclassified unclassified}
                               :else {:cause :every-catalogued-check-green})})))))

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

(def deposit-artifact
  "The committed file the deposit controls point a row at: the pinned s5 run's
   own conformance record, chosen because nothing edits it."
  "holes/labs/wm-contract/runs/2026-09-01-s5/conformance.edn")

(defn- git-fixture!
  "A throwaway git repository under a temp dir holding one committed file, one
   committed-then-modified file and one untracked file. The artifact guard is
   about history, so it is exercised against a real history rather than a stub.
   The author date is fixed, so the instant the guard reads is asserted exactly."
  []
  (let [d (.toFile (java.nio.file.Files/createTempDirectory
                    "re3-artifact-guard" (into-array java.nio.file.attribute.FileAttribute [])))
        g (fn [& a] (apply process/shell {:dir d :out :string :err :string :continue true} "git" a))]
    (spit (io/file d "committed.edn") "{:fixture :committed}\n")
    (spit (io/file d "moved.edn") "{:fixture :as-committed}\n")
    (g "init" "-q")
    (g "config" "user.email" "re3@invalid.example")
    (g "config" "user.name" "re3 artifact-guard fixture")
    (g "add" "committed.edn" "moved.edn")
    (g "commit" "-q" "-m" "fixture" "--date" "2020-01-02T03:04:05+00:00")
    (spit (io/file d "moved.edn") "{:fixture :modified-since-the-commit}\n")
    (spit (io/file d "untracked.edn") "{:fixture :never-committed}\n")
    (.getPath d)))

(defn self-test
  "Appends one synthetic row to a temp copy, replays it, and exercises the
   rejecting controls. Returns an ordered map of control -> result."
  []
  (let [tmpdir (java.nio.file.Files/createTempDirectory
                "re2-run-era" (into-array java.nio.file.attribute.FileAttribute []))
        tmp (io/file (.toFile tmpdir) "run-era-ledger.edn")
        committed (read-ledger default-ledger-path)]
    (io/copy (io/file default-ledger-path) tmp)
    (let [rows-before (count (:rows (:data (read-ledger tmp))))
          empty-defects (validate (:data (read-ledger tmp)))
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
          ;; the deposit path (RE3) and its artifact guard
          deposited (deposit! tmp {:run-id "0000-00-00-self-test-deposit"
                                   :check-id :run-conformance
                                   :verdict :green
                                   :artifact deposit-artifact
                                   :author "run_era_ledger.bb --self-test"
                                   :notes "synthetic; written only to a temporary copy"})
          after-deposit (read-ledger tmp)
          deposited-row (last (:rows (:data after-deposit)))
          deposit-replay (deposit! tmp {:run-id "0000-00-00-self-test-deposit"
                                        :check-id :run-conformance
                                        :verdict :green
                                        :artifact deposit-artifact
                                        :author "run_era_ledger.bb --self-test"
                                        :notes "synthetic; written only to a temporary copy"})
          after-deposit-replay (read-ledger tmp)
          c-deposit-missing (refusal #(deposit! tmp {:run-id "0000-00-00-control-i"
                                                     :check-id :run-conformance :verdict :green
                                                     :artifact "holes/labs/wm-contract/no-such-artifact.edn"
                                                     :author "control"}))
          fixture (git-fixture!)
          c-deposit-untracked (refusal #(deposit! tmp {:run-id "0000-00-00-control-j"
                                                       :check-id :run-conformance :verdict :green
                                                       :artifact "untracked.edn" :author "control"}
                                                  fixture))
          c-deposit-dirty (refusal #(deposit! tmp {:run-id "0000-00-00-control-k"
                                                   :check-id :run-conformance :verdict :green
                                                   :artifact "moved.edn" :author "control"}
                                              fixture))
          final (read-ledger tmp)
          ;; RE7 -- the catalogue write path
          cat-entry {:check/id :0000-self-test-check
                     :check/machinery "holes/labs/wm-contract/run_era_ledger.bb (the self-test's own synthetic entry)"
                     :check/precedent "synthetic; written only to a temporary copy"}
          cat-before (read-ledger tmp)
          cat-added (catalogue-add! tmp cat-entry)
          cat-after (read-ledger tmp)
          cat-replay (catalogue-add! tmp cat-entry)
          c-cat-divergent (refusal #(catalogue-add! tmp (assoc cat-entry :check/precedent "a different precedent")))
          c-cat-machinery (refusal #(catalogue-add! tmp {:check/id :0000-self-test-no-machinery
                                                         :check/machinery "holes/labs/wm-contract/no-such-producer.bb"
                                                         :check/precedent "synthetic"}))
          cat-final (read-ledger tmp)
          ;; RE6 -- the absence-kind write path. Two synthetic typed-absence
          ;; rows first, because a label may only name a pair that really is one.
          abs-run "0000-00-00-self-test-absence"
          abs-row (assoc synthetic-row :row/run-id abs-run :row/verdict :typed-absence
                         :row/notes "synthetic; the store held nothing")
          abs-row2 (assoc abs-row :row/check-id :run-conformance)
          _ (append-row! tmp abs-row)
          _ (append-row! tmp abs-row2)
          abs-entry {:absence/run-id abs-run
                     :absence/check-id :flip-readiness
                     :absence/kind :data-pending
                     :absence/basis "synthetic; written only to a temporary copy"
                     :absence/by "run_era_ledger.bb --self-test"}
          abs-entry2 (assoc abs-entry :absence/check-id :run-conformance)
          abs-before (read-ledger tmp)
          abs-added (absence-kind! tmp abs-entry)
          abs-after (read-ledger tmp)
          abs-replay (absence-kind! tmp abs-entry)
          abs-final (read-ledger tmp)
          c-abs-divergent (refusal #(absence-kind! tmp (assoc abs-entry :absence/kind :not-contemporaneous)))
          c-abs-no-row (refusal #(absence-kind! tmp (assoc abs-entry :absence/run-id "0000-00-00-no-such-run")))
          c-abs-not-an-absence (refusal #(absence-kind! tmp (assoc abs-entry :absence/run-id "0000-00-00-self-test")))
          c-abs-bad-kind (refusal #(absence-kind! tmp (assoc abs-entry2 :absence/kind :probably-fine)))
          c-abs-blank-basis (refusal #(absence-kind! tmp (assoc abs-entry2 :absence/basis "")))
          ;; the fold's branch table, on a fixture rather than on this file, so
          ;; each of the four outcomes is reached by exactly one input change
          fold-fixture (fn [kinds]
                         {:ledger/check-catalogue [{:check/id :a} {:check/id :b} {:check/id :c}]
                          :rows [{:row/run-id "r" :row/check-id :a :row/verdict :green
                                  :row/at "2026-01-01T00:00:00Z"}
                                 {:row/run-id "r" :row/check-id :b :row/verdict :typed-absence
                                  :row/at "2026-01-01T00:00:00Z"}
                                 {:row/run-id "r" :row/check-id :c :row/verdict :typed-absence
                                  :row/at "2026-01-01T00:00:00Z"}]
                          :ledger/absence-kinds (vec (for [[ck k] kinds]
                                                       {:absence/run-id "r" :absence/check-id ck
                                                        :absence/kind k}))})
          fold-status (fn [kinds] (:status (first (fold-by-run (fold-fixture kinds)))))
          fold-table {:unlabelled (fold-status {})
                      :half-labelled (fold-status {:b :data-pending})
                      :both-data-pending (fold-status {:b :data-pending :c :data-pending})
                      :one-not-contemporaneous (fold-status {:b :data-pending :c :not-contemporaneous})
                      :both-not-contemporaneous (fold-status {:b :not-contemporaneous :c :not-contemporaneous})
                      :no-absences (:status (first (fold-by-run
                                                    (update (fold-fixture {}) :rows
                                                            (fn [rs] (mapv #(assoc % :row/verdict :green) rs))))))}]
      (array-map
       :positive/c1-committed-ledger-is-green
       {:defects empty-defects :rows (count (:rows (:data committed)))
        :pass? (empty? empty-defects)
        :why "the committed ledger conforms to the schema it declares; this is what `--check` gates on"}

       :positive/c2-synthetic-row-appends-and-revalidates-green
       ;; counted RELATIVE to the ledger the self-test started from. These two
       ;; controls asserted the absolute count 1, which held only while the
       ;; committed ledger was empty and broke the moment RE3 deposited into it.
       {:result appended :defects after-defects
        :rows-before rows-before :rows (count (:rows (:data after)))
        :pass? (and (= :appended (:status appended)) (empty? after-defects)
                    (= (inc rows-before) (count (:rows (:data after)))))
        :why "one row appended through the API, and the ledger it produced validates green -- otherwise every control below is a statement about a ledger nothing can write"}

       :positive/c3-identical-append-is-already-present
       {:result replay :rows (count (:rows (:data after-replay)))
        :byte-identical? (= (:text after) (:text after-replay))
        :pass? (and (= :already-present (:status replay))
                    (= (inc rows-before) (count (:rows (:data after-replay))))
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
        :why "every append above went to a temp copy; the committed ledger is byte-identical to what the self-test started with, which is why `--check` on it stays a check of committed history"}

       :positive/c15-deposit-derives-its-at-from-the-artifact-commit
       {:result (dissoc deposited :artifact-state)
        :artifact deposit-artifact
        :artifact-commit (:commit (:artifact-state deposited))
        :row-at (:row/at deposited-row)
        :replay (:status deposit-replay)
        :byte-identical? (= (:text after-deposit) (:text after-deposit-replay))
        :pass? (and (= :appended (:status deposited))
                    (= (:at (:artifact-state deposited)) (:row/at deposited-row))
                    (= :already-present (:status deposit-replay))
                    (= (:text after-deposit) (:text after-deposit-replay)))
        :why "a check calls --deposit, not the EDN: the row is assembled here and its :row/at is the artifact's commit instant, which is what makes the same deposit repeat as :already-present instead of colliding with itself"}

       :negative/c16-deposit-refuses-an-artifact-that-is-not-there
       {:defects (defect-kinds c-deposit-missing) :threw? (:threw? c-deposit-missing)
        :pass? (refused-for? c-deposit-missing {:defect :artifact-missing})
        :why "the guard runs before the append, so a row naming a file nobody wrote is refused at the deposit rather than at the validator"}

       :negative/c17-deposit-refuses-an-untracked-artifact
       {:state (artifact-commit-state fixture "untracked.edn")
        :defects (defect-kinds c-deposit-untracked) :threw? (:threw? c-deposit-untracked)
        :pass? (refused-for? c-deposit-untracked {:defect :artifact-untracked})
        :why "`--check` is satisfied by any file on disk; a reviewer reading this row later has only the history, so an artifact that was never committed is not evidence"}

       :negative/c18-deposit-refuses-an-artifact-modified-since-its-commit
       {:state (dissoc (artifact-commit-state fixture "moved.edn") :status)
        :defects (defect-kinds c-deposit-dirty) :threw? (:threw? c-deposit-dirty)
        :pass? (refused-for? c-deposit-dirty {:defect :artifact-dirty})
        :why "the row would name a path whose committed content is not what the check read; the working copy having moved is exactly the case a resolving pointer hides"}

       :positive/c19-the-guard-reads-the-real-commit-instant
       {:fixture-state (dissoc (artifact-commit-state fixture "committed.edn") :commit)
        :pass? (= {:state :committed :path "committed.edn" :at "2020-01-02T03:04:05Z"}
                  (dissoc (artifact-commit-state fixture "committed.edn") :commit))
        :why "without this the three refusals above would be indistinguishable from a guard that refuses everything"}

       :positive/c20-catalogue-add-appends-and-leaves-every-row-untouched
       {:result cat-added
        :catalogue-before (count (:ledger/check-catalogue (:data cat-before)))
        :catalogue-after (count (:ledger/check-catalogue (:data cat-after)))
        :rows-unchanged? (= (:rows (:data cat-before)) (:rows (:data cat-after)))
        :head-sha-unchanged? (= (:ledger/head-sha (:data cat-before))
                                (:ledger/head-sha (:data cat-after)))
        :pass? (and (= :appended (:status cat-added))
                    (= (inc (count (:ledger/check-catalogue (:data cat-before))))
                       (count (:ledger/check-catalogue (:data cat-after))))
                    (empty? (validate (:data cat-after)))
                    (= (:rows (:data cat-before)) (:rows (:data cat-after)))
                    (= (:ledger/head-sha (:data cat-before)) (:ledger/head-sha (:data cat-after))))
        :why "minting a check is a write through the API rather than an edit, and the sha chain covers rows only -- so this shows the rows and the head sha come through it untouched"}

       :positive/c21-identical-catalogue-add-is-already-present
       {:result cat-replay
        :byte-identical? (= (:text cat-after) (:text cat-final))
        :pass? (and (= :already-present (:status cat-replay))
                    (= (:text cat-after) (:text cat-final)))
        :why "the mint is re-runnable for the same reason a deposit is: a check wired twice must not double its catalogue entry"}

       :negative/c22-a-divergent-entry-for-an-existing-check-id-is-refused
       {:defects (defect-kinds c-cat-divergent) :threw? (:threw? c-cat-divergent)
        :pass? (refused-for? c-cat-divergent {:defect :divergent-existing-check-id})
        :why "the catalogue is what a row's :check-id means; rewriting an entry in place would silently change what every past row claims"}

       :negative/c23-an-entry-whose-machinery-does-not-resolve-is-refused
       {:defects (defect-kinds c-cat-machinery) :threw? (:threw? c-cat-machinery)
        :pass? (refused-for? c-cat-machinery {:defect :machinery-pointer-does-not-resolve})
        :why "the ledger's own rule is `a check enters the catalogue when its machinery exists`; without this the rule was a sentence, and a check id could be minted for a producer nobody wrote"}

       :positive/c24-labelling-an-absence-leaves-every-row-untouched
       {:result abs-added
        :labels-before (count (:ledger/absence-kinds (:data abs-before)))
        :labels-after (count (:ledger/absence-kinds (:data abs-after)))
        :rows-unchanged? (= (:rows (:data abs-before)) (:rows (:data abs-after)))
        :head-sha-unchanged? (= (:ledger/head-sha (:data abs-before))
                                (:ledger/head-sha (:data abs-after)))
        :pass? (and (= :appended (:status abs-added))
                    (= (inc (count (:ledger/absence-kinds (:data abs-before))))
                       (count (:ledger/absence-kinds (:data abs-after))))
                    (empty? (validate (:data abs-after)))
                    (= (:rows (:data abs-before)) (:rows (:data abs-after)))
                    (= (:ledger/head-sha (:data abs-before)) (:ledger/head-sha (:data abs-after))))
        :why "a label says which response a standing absence calls for and must not be a repair by another name; the deposited row and the whole sha chain come through it byte-for-byte"}

       :positive/c25-identical-label-is-already-present
       {:result abs-replay
        :byte-identical? (= (:text abs-after) (:text abs-final))
        :pass? (and (= :already-present (:status abs-replay))
                    (= (:text abs-after) (:text abs-final)))
        :why "relabelling is re-runnable for the same reason a deposit is, so a relabel pass can be replayed against the committed ledger without a second entry"}

       :positive/c26-the-fold-splits-typed-absences-by-kind
       {:table fold-table
        :pass? (= {:unlabelled :incomplete
                   :half-labelled :incomplete
                   :both-data-pending :incomplete-data-pending
                   :one-not-contemporaneous :incomplete-uninstrumented
                   :both-not-contemporaneous :incomplete-uninstrumented
                   :no-absences :green}
                  fold-table)
        :why "the whole point of the ruling, on a fixture where each row of the table differs from its neighbour by one label. It pins the precedence too: ONE :not-contemporaneous absence beside a :data-pending one reads as uninstrumented, because that is the response the run needs"}

       :negative/c27-an-unlabelled-absence-folds-to-neither-new-word
       {:unlabelled (:unlabelled fold-table) :half-labelled (:half-labelled fold-table)
        :pass? (and (= :incomplete (:unlabelled fold-table))
                    (= :incomplete (:half-labelled fold-table)))
        :why "the split must not launder an unexamined absence into the milder word. An absence nobody has labelled keeps the pre-split :incomplete, and one labelled absence beside one unlabelled is still :incomplete -- the fold names the unlabelled ones under :unclassified"}

       :negative/c28-a-label-for-a-pair-that-is-not-a-typed-absence-is-refused
       {:no-row (defect-kinds c-abs-no-row) :not-an-absence (defect-kinds c-abs-not-an-absence)
        :pass? (and (refused-for? c-abs-no-row {:defect :absence-kind-names-no-typed-absence-row})
                    (refused-for? c-abs-not-an-absence {:defect :absence-kind-names-no-typed-absence-row}))
        :why "both halves matter: a label for a run nobody deposited, and a label for a pair that WAS deposited and came back :green. Either would be a kind attached to nothing, which the fold would then never read"}

       :negative/c29-a-divergent-label-for-an-existing-pair-is-refused
       {:defects (defect-kinds c-abs-divergent) :threw? (:threw? c-abs-divergent)
        :pass? (refused-for? c-abs-divergent {:defect :divergent-existing-absence-kind})
        :why "re-labelling a pair in place would move a run between statuses with no record that it moved; changing a kind is a decision, so it has to be visible as one"}

       :negative/c30-a-kind-outside-the-declared-set-and-a-blank-basis-are-refused
       {:bad-kind (defect-kinds c-abs-bad-kind) :blank-basis (defect-kinds c-abs-blank-basis)
        :pass? (and (refused-for? c-abs-bad-kind
                                  {:defect :value-outside-declared-set
                                   :key :absence/kind :value :probably-fine})
                    (refused-for? c-abs-blank-basis
                                  {:defect :missing-required-key :key :absence/basis}))
        :why "the two ways a label could say nothing: a third kind the fold has no branch for, and a kind with no measurement behind it. The enum is declared in the ledger file, and the basis is required there"}))))

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

(defn- displayed-path [f]
  (let [root (.toPath (.getCanonicalFile (io/file repo-root)))
        path (.toPath (.getCanonicalFile (io/file f)))]
    (if (.startsWith path root)
      (str (.relativize root path))
      (str path))))

(def ^:private deposit-flags
  "--deposit's named inputs. The keyword-valued ones are read as EDN, so a
   mistyped check id arrives as a keyword the catalogue does not hold and is
   refused by the validator, rather than as a string that merely looks wrong."
  {"--run-id" [:run-id :string]
   "--check-id" [:check-id :edn]
   "--verdict" [:verdict :edn]
   "--artifact" [:artifact :string]
   "--author" [:author :string]
   "--notes" [:notes :string]
   "--deposited-by" [:deposited-by :string]
   "--at" [:at :string]})

(def ^:private catalogue-flags
  "--catalogue-add's named inputs. `--id` is read as EDN for the same reason
   `--check-id` is: a mistyped id arrives as the keyword it really is."
  {"--id" [:check/id :edn]
   "--machinery" [:check/machinery :string]
   "--precedent" [:check/precedent :string]
   "--note" [:check/note :string]})

(def ^:private absence-flags
  "--label-absence's named inputs. `--check-id` and `--kind` are read as EDN so a
   mistyped value arrives as the keyword it really is and meets the enum."
  {"--run-id" [:absence/run-id :string]
   "--check-id" [:absence/check-id :edn]
   "--kind" [:absence/kind :edn]
   "--basis" [:absence/basis :string]
   "--by" [:absence/by :string]})

(defn- parse-flags [flags mode args]
  (loop [[a & more] args, out {}]
    (cond
      (nil? a) out
      (= a mode) (recur more out)
      (contains? flags a)
      (let [[k kind] (get flags a)
            v (or (first more) (throw (ex-info (str a " needs a value") {:flag a})))]
        (recur (rest more) (assoc out k (if (= :edn kind) (edn/read-string v) v))))
      :else (throw (ex-info (str "unknown " mode " flag")
                            {:flag a :known (vec (sort (keys flags)))})))))

(defn- parse-deposit [args]
  (loop [[a & more] args, out {}]
    (cond
      (nil? a) out
      (= a "--deposit") (recur more out)
      (contains? deposit-flags a)
      (let [[k kind] (get deposit-flags a)
            v (or (first more) (throw (ex-info (str a " needs a value") {:flag a})))]
        (recur (rest more) (assoc out k (if (= :edn kind) (edn/read-string v) v))))
      :else (throw (ex-info "unknown --deposit flag"
                            {:flag a :known (vec (sort (keys deposit-flags)))})))))

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
    (emit "BY RUN — never green while a catalogued check is missing; a typed absence folds to the response it asks for")
    (if (empty? runs)
      (emit "  (no runs: the ledger is empty. RE3 wires the first three checks to deposit; RE5 is the first correlated run.)")
      (doseq [r runs]
        (emit (format "  %-28s %-27s %d checks, not deposited: %s"
                      (:run-id r) (str (:status r)) (count (:checks r))
                      (pr-str (:checks-not-deposited r))))
        (emit (format "  %-28s   because %s %s" ""
                      (str (:cause (:status-reason r)))
                      (pr-str (or (:checks (:status-reason r)) []))))
        (let [sr (:status-reason r)]
          (when (= :typed-absences (:cause sr))
            (emit (format "  %-28s   instrument the next run: %s | wait for the datum: %s%s" ""
                          (pr-str (:not-contemporaneous sr)) (pr-str (:data-pending sr))
                          (if (seq (:unclassified sr))
                            (str " | UNLABELLED: " (pr-str (:unclassified sr)))
                            "")))))))
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
            outdir-label (displayed-path outdir)
            _ (.mkdirs outdir)
            {:keys [data]} (read-ledger path)
            defects (validate data)
            lines (report-lines data defects)
            report {:reader {:id :re2-run-era-ledger :version "v1" :row :RE2
                             :script "holes/labs/wm-contract/run_era_ledger.bb"
                             :ledger "holes/labs/wm-contract/run-era-ledger.edn"
                             :implements "EPIC-run-era.md:31-60"
                             :read-only true
                             :writes-only-under outdir-label}
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
            outdir-label (displayed-path outdir)
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
                                                 :writes-only-under outdir-label
                                                 :ledger-under-test "a temporary copy of run-era-ledger.edn"}
                                        :controls (mapv (fn [[k v]] [k v]) ctrls)})))
        (println (str/join "\n" @lines))
        (println)
        (println "wrote" (str (.getPath outdir) "/RE2-SELF-TEST.txt")
                 "and" (str (.getPath outdir) "/run-era-self-test.edn"))
        (when-not (every? :pass? (vals ctrls)) (System/exit 2)))

      "--deposit"
      (let [opts (parse-deposit args)
            missing (vec (remove #(get opts %) [:run-id :check-id :verdict :artifact :author]))]
        (when (seq missing)
          (println "run_era_ledger --deposit: missing" (pr-str missing))
          (System/exit 1))
        (try
          (let [r (deposit! path opts)]
            (prn (dissoc r :artifact-state))
            (println (format "run_era_ledger: %s -- run %s, check %s, seq %s, at %s, artifact %s (commit %s)"
                             (name (:status r)) (:run-id r) (str (:check-id r)) (:row/seq r)
                             (or (:at opts) (:at (:artifact-state r)))
                             (:artifact opts) (:commit (:artifact-state r)))))
          (catch clojure.lang.ExceptionInfo e
            (println "run_era_ledger --deposit REFUSED:" (ex-message e))
            (println " " (pr-str (ex-data e)))
            (System/exit 1))))

      "--label-absence"
      (let [entry (parse-flags absence-flags "--label-absence" args)
            missing (vec (remove #(get entry %) absence-authored-keys))]
        (when (seq missing)
          (println "run_era_ledger --label-absence: missing" (pr-str missing))
          (System/exit 1))
        (try
          (let [r (absence-kind! path entry)]
            (prn r)
            (println (format "run_era_ledger: %s -- run %s, check %s, kind %s"
                             (name (:status r)) (:absence/run-id entry)
                             (str (:absence/check-id entry)) (str (:absence/kind entry)))))
          (catch clojure.lang.ExceptionInfo e
            (println "run_era_ledger --label-absence REFUSED:" (ex-message e))
            (println " " (pr-str (ex-data e)))
            (System/exit 1))))

      "--catalogue-add"
      (let [entry (parse-flags catalogue-flags "--catalogue-add" args)
            missing (vec (remove #(get entry %) [:check/id :check/machinery :check/precedent]))]
        (when (seq missing)
          (println "run_era_ledger --catalogue-add: missing" (pr-str missing))
          (System/exit 1))
        (try
          (let [r (catalogue-add! path entry)]
            (prn r)
            (println (format "run_era_ledger: %s -- check %s, machinery %s"
                             (name (:status r)) (str (:check/id entry))
                             (machinery-pointer (:check/machinery entry)))))
          (catch clojure.lang.ExceptionInfo e
            (println "run_era_ledger --catalogue-add REFUSED:" (ex-message e))
            (println " " (pr-str (ex-data e)))
            (System/exit 1))))

      "--append"
      (let [row (edn/read-string (or (second (drop-while #(not= "--append" %) args))
                                     (throw (ex-info "--append needs an EDN row map" {}))))]
        (prn (append-row! path row)))

      "--append-file"
      (let [f (or (second (drop-while #(not= "--append-file" %) args))
                  (throw (ex-info "--append-file needs a path" {})))]
        (prn (append-row! path (edn/read-string (slurp f)))))

      (do (println "unknown mode" mode)
          (println "modes: --check | --report [outdir] | --self-test [outdir] | --append '<edn>' | --append-file <path>")
          (println "       --catalogue-add --id <kw> --machinery <s> --precedent <s> [--note <s>]")
          (println "       --label-absence --run-id <id> --check-id <kw> --kind <kw> --basis <s> --by <s>")
          (println "       --deposit --run-id <id> --check-id <kw> --verdict <kw> --artifact <path> --author <s> [--notes <s>] [--deposited-by <s>] [--at <instant>]")
          (println "       (any mode) [--ledger <path>]")
          (System/exit 64)))))

(apply -main *command-line-args*)
