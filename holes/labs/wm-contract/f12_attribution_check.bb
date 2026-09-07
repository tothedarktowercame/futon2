#!/usr/bin/env bb
;; f12_attribution_check.bb -- `:F12` slice 8.  CHECKS the attribution arm
;; against the recorded provenance it claims to transcribe, and against the
;; two signatures it claims to sit between.
;;
;; WHY A CHECK AND NOT A READ.  Slice 8 runs the arm C545 section 10 named as
;; one of two exits: pin O1's third origin to something outside the laws.  The
;; something is real and recorded -- `futon3:checks/zaif-cascade.edn` carries,
;; for run `:widen-to-a-budget`, a per-pattern `[:cascade :provenance]` map
;; written by the `:admit` arm of `apply-edit`
;; (`futon3:checks/find_organise.clj:412`, the assoc-in at `:422`).  Five
;; things can go wrong while `lake build` stays green, because each leaves the
;; Lean file elaborating perfectly while being about something else:
;;
;;   1. `zaifProvenance` could not be the recorded map.  Its support is what
;;      the determination result is ABOUT, so the support is recomputed here
;;      from the raw record and its patterns resolved through slice 2's index,
;;      rather than compared against slice 2's stored answer.
;;   2. `AttributingOrganiseType` could differ from slice 7's
;;      `AdmittingOrganiseType` in more than the added argument -- and then the
;;      slice's price ("one argument, and nothing else") is not what it says.
;;      Both types are read out of their own files and the insertion is
;;      reconstructed.
;;   3. `ConformantOrganiseAttributing` could differ from slice 7's predicate
;;      in more than the added `oattr` clause, and then the comparison with
;;      slice 7's underdetermination result does not transfer.  The clauses are
;;      read and compared one by one, with the new argument normalised away.
;;   4. The arm still cannot state O4 -- a claim about which fields the carrier
;;      has, which a comment cannot be wrong about in a way the build notices.
;;   5. THE RULE GRAIN IS NOT EXERCISED ANYWHERE, and that limit is the one a
;;      reader is most likely to lose.  Every admission in every provenance map
;;      in `futon3:checks/*cascade*.edn` names ONE rule id, so on all recorded
;;      data the attribution input is indistinguishable from a boolean
;;      admitted-or-not flag.  Recomputed over the whole corpus here rather
;;      than asserted in prose, so a record that later carries two rules moves
;;      the number instead of leaving the caveat stale.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling on D1, on the O3 field question, on
;; D2/D3, or on whether the sorry at `Holes.lean:861` is discharged or amended.
;; It records that `Holes.lean` and `holes-contract.json` did not move, which is
;; a fact about this slice, not a decision about the next.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two
;; runs over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_ATTR, F12_ARM, F12_ARMS,
;; F12_CONF, F12_HOLES, F12_RECORD, F12_RAW, F12_CHECKS, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :refer [sh]]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def wm (str home "/code/mathlib4/DarkTower/WarMachine"))
(def attr-path (or (System/getenv "F12_ATTR") (str wm "/F12AttributionArm.lean")))
(def arm-path (or (System/getenv "F12_ARM") (str wm "/F12AdmittingArm.lean")))
(def arms-path (or (System/getenv "F12_ARMS") (str wm "/F12D1Arms.lean")))
(def conf-path (or (System/getenv "F12_CONF") (str wm "/F12Conformance.lean")))
(def holes-path (or (System/getenv "F12_HOLES") (str wm "/Holes.lean")))
(def record-path (or (System/getenv "F12_RECORD")
                     (str lab "/runs/F12-organise/01-zaif-transcription.edn")))
(def raw-path (or (System/getenv "F12_RAW") (str home "/code/futon3/checks/zaif-cascade.edn")))
(def checks-dir (or (System/getenv "F12_CHECKS") (str home "/code/futon3/checks")))
(def construct-path (or (System/getenv "F12_CONSTRUCT") (str checks-dir "/construct_cascade.clj")))
(def out-path (or (System/getenv "F12_OUT") (str lab "/runs/F12-organise/06-attribution-arm.edn")))

;; The two files slice 2 and C542 pinned.  Slice 8 must not move either: the
;; contract authority is `git log -1 -- Holes.lean` (`Emit.lean:52-62`), so an
;; edit to Holes.lean turns C176's gate red for every lane.
(def pinned
  {"DarkTower/WarMachine/Holes.lean" "61c4825dc3e373fd1b761b800814bf85f5770b88"
   "DarkTower/WarMachine/holes-contract.json" "50fa53469ab23c68e620aa2fd316315a9015dcb1"})

(defn die [m data]
  (binding [*out* *err*] (println "f12-attribution:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path))
    (slurp path)
    (die "file not found" {:what what :path path})))

(def attr (read-source attr-path :attribution-arm))
(def arm (read-source arm-path :admitting-arm))
(def arms (read-source arms-path :d1-arms))
(def conf (read-source conf-path :conformance))
(def holes (read-source holes-path :holes))
(def record (edn/read-string (slurp record-path)))
(def raw (edn/read-string (slurp raw-path)))
(def construct (read-source construct-path :construct-cascade))

(defn- squeeze [s] (str/trim (str/replace s #"\s+" " ")))

;; --- 1. the recorded attribution, RECOMPUTED from the raw record ------------

(def run-key (get-in record [:basis :run]))
(def provenance
  (or (get-in raw [:runs run-key :cascade :provenance])
      (die "the record carries no provenance for the transcribed run"
           {:run run-key :path raw-path})))

;; slice 2's index is the only thing that turns a pattern keyword into the
;; vertex number the Lean file names.
(def index (into {} (map (fn [[i k]] [k i])) (:index record)))

(defn- admission? [v] (and (vector? v) (= :admitted-by (first v))))

(def admitted-patterns (vec (sort (keep (fn [[k v]] (when (admission? v) k)) provenance))))
(def found-patterns (vec (sort (keep (fn [[k v]] (when (= :found v) k)) provenance))))
(def other-entries (vec (sort (keep (fn [[k v]] (when-not (or (admission? v) (= :found v)) k))
                                    provenance))))
(def unindexed (vec (sort (remove index (keys provenance)))))

(def admitted-vertices (vec (sort (keep index admitted-patterns))))
(def found-vertices (vec (sort (keep index found-patterns))))
(def rule-ids (vec (sort (set (map (comp second val)
                                   (filter (comp admission? val) provenance))))))

;; the encoding slice 2 wrote into Holes.lean and slice 5 into `d1Admitted`.
(def enc (:encoding record))
(def expected-admitted (vec (range (:admitted-from enc) (:nodes-below enc))))
(def expected-found (vec (range (:selected-below enc))))

;; --- 2. the rule grain, over the WHOLE recorded corpus ----------------------

(defn- provenance-maps
  "Every `:provenance` map anywhere in a record, in document order."
  [x]
  (cond (map? x) (concat (when (map? (:provenance x)) [(:provenance x)])
                         (mapcat provenance-maps (vals x)))
        (sequential? x) (mapcat provenance-maps x)
        :else nil))

(def corpus
  (into (sorted-map)
        (for [f (sort (map #(.getName %) (.listFiles (io/file checks-dir))))
              :when (re-matches #".*cascade.*\.edn" f)]
          (let [r (try (edn/read-string (slurp (str checks-dir "/" f)))
                       (catch Exception _ ::unreadable))
                ms (if (= ::unreadable r) [] (provenance-maps r))]
            [f (sorted-map
                :provenance-maps (count ms)
                :admissions (reduce + 0 (map #(count (filter admission? (vals %))) ms))
                :rule-ids (vec (sort (set (mapcat #(map second (filter admission? (vals %)))
                                                  ms)))))]))))

(def corpus-rule-ids (vec (sort (set (mapcat (comp :rule-ids val) corpus)))))
(def corpus-admissions (reduce + 0 (map (comp :admissions val) corpus)))
(def records-without-provenance
  (vec (sort (keep (fn [[f m]] (when (zero? (:provenance-maps m)) f)) corpus))))

;; --- 3. the temperament, which is the argument the type ALREADY has ---------

(def temperament (:temperaments raw))
(def temperament-keys (vec (sort (keys temperament))))
(def temperament-shared-nodes (vec (sort (:shared-nodes temperament))))

;; The record reports the temperaments only through `differ-only-in-the-stop`
;; (`futon3:checks/construct_cascade.clj:338`), which reports the shared nodes
;; with the STOP RULE REMOVED -- so `:shared-nodes` is one node where the
;; temperament that ran has two.  The temperament literal is therefore read
;; from its own source and cross-checked against the record, rather than
;; reconstructed from the record's summary.
(def budgeted-temperament
  (if-let [m (re-find #"(?s)\(def budgeted-temperament\n\s*\{(.*?)\}\)" construct)]
    (edn/read-string (str "{" (second m) "}"))
    (die "construct_cascade.clj does not declare budgeted-temperament"
         {:path construct-path})))

(def temperament-nodes (vec (:nodes budgeted-temperament)))
(def temperament-precedence (into (sorted-map) (:precedence budgeted-temperament)))
(def temperament-stops (vec (sort (remove (set temperament-shared-nodes) temperament-nodes))))

;; A key whose value maps patterns to rules would let the attribution be read
;; off the first argument.  There is none; recorded as a key list, not prose.
(def temperament-pattern-keyed
  (vec (sort (keep (fn [[k v]] (when (and (map? v) (some index (keys v))) k)) temperament))))

;; --- 4. the signature: slice 7's, with one argument inserted ----------------

(def organise-type-in-holes
  (if-let [m (re-find #"def organise \{Policy P : Type\*\} :\s*([^:=]+):= sorry" holes)]
    (squeeze (second m))
    (die "Holes.lean does not declare organise in the expected shape" {:path holes-path})))

(def admitting-type
  (if-let [m (re-find #"abbrev AdmittingOrganiseType \(Policy P : Type\*\) :=\s*\n?\s*([^\n]+)" arm)]
    (squeeze (second m))
    (die "the slice 7 file states no AdmittingOrganiseType abbrev" {:path arm-path})))

(def attributing-type
  (if-let [m (re-find #"abbrev AttributingOrganiseType \(Policy P Rule : Type\*\) :=\s*\n?\s*([^\n]+)"
                      attr)]
    (squeeze (second m))
    (die "the arm file states no AttributingOrganiseType abbrev" {:path attr-path})))

;; the ONE edit: an attribution argument inserted before the codomain.
(def expected-attributing-type
  (str/replace admitting-type #"→ ArmOneCascade P$" "→ (P → Option Rule) → ArmOneCascade P"))

;; --- 5. the conformance predicate, clause by clause -------------------------

(defn- clauses-of
  [src structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\{Policy P[^}]*\\}.*?where\n(.*?)\n\n"))
                      src)]
    (into (sorted-map)
          (for [[_ k body] (re-seq #"(?m)^  (\w+) :((?:[^\n]|\n    )+)" (second m))]
            [(keyword k) (squeeze body)]))
    (die "no such conformance structure" {:structure structure-name})))

(def attributing-clauses (clauses-of attr "ConformantOrganiseAttributing"))
(def admitting-clauses (clauses-of arm "ConformantOrganiseAdmitting"))

;; The four inherited clauses differ from slice 7's only by carrying the new
;; argument.  Normalising it away is what lets them be compared as the SAME
;; propositions rather than eyeballed as similar ones.
(defn- drop-prov [s]
  (-> s
      (str/replace "(f t sel repo prov)" "(f t sel repo)")
      (str/replace "∀ t sel repo prov," "∀ t sel repo,")
      (str/replace "∀ t sel repo prov u v," "∀ t sel repo u v,")))

(def sans-attr-clauses (clauses-of attr "ConformantOrganiseAttributingSansAttr"))

(def clauses-added-over-slice-7
  (vec (sort (set/difference (set (keys attributing-clauses)) (set (keys admitting-clauses))))))

(def inherited-clauses-that-moved
  (vec (sort (for [k (keys admitting-clauses)
                   :when (not= (drop-prov (get attributing-clauses k ""))
                               (get admitting-clauses k))]
               k))))

;; --- 6. the transcription's own shape ---------------------------------------

(def provenance-body
  (if-let [m (re-find #"(?s)def zaifProvenance \(n : Nat\) : Option ZaifPolicyRule :=\s*\n(.*?)\n\n" attr)]
    (squeeze (second m))
    (die "the arm file states no zaifProvenance" {:path attr-path})))

(def rule-constructors
  (if-let [m (re-find #"(?s)inductive ZaifPolicyRule where\n(.*?)\n\n" attr)]
    (vec (sort (map second (re-seq #"(?m)^  \| (\w+)" (first (str/split (second m) #"\n  deriving"))))))
    (die "the arm file states no ZaifPolicyRule inductive" {:path attr-path})))

;; The Lean temperament must be the RECORDED one and not the record's
;; stop-removed summary of it, so its node count and precedence length are read
;; out and compared with `budgeted-temperament`'s own literal.
(def policy-cascade
  (if-let [m (re-find #"(?s)def zaifPolicyCascade : Cascade ZaifPolicyRule where\n(.*?)\n\n" attr)]
    (second m)
    (die "the arm file states no zaifPolicyCascade" {:path attr-path})))

(def policy-cascade-nodes
  (vec (sort (map second (re-seq #"\.(\w+)"
                                 (or (second (re-find #"(?m)^  nodes := \{(.*)\}" policy-cascade)) ""))))))

(def policy-cascade-precedence
  (vec (map second (re-seq #"\.(\w+)"
                           (or (second (re-find #"(?m)^  precedence := \[(.*)\]" policy-cascade)) "")))))

;; The transcription must NAME its source keywords, so a reader can get from the
;; Lean constructor back to the record entry without guessing the camelisation.
(def names-the-recorded-rules?
  (every? #(str/includes? attr (str %)) temperament-nodes))

;; --- 7. carriers and the O4 non-statement -----------------------------------

(defn- fields-of [src structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\([^)]*\\) where\n(.*?)\n\n"))
                      src)]
    (vec (sort (map second (re-seq #"(?m)^  (\w+)\s*:" (second m)))))
    (die "no fields found for structure" {:structure structure-name})))

(def cascade-fields (fields-of holes "Cascade"))
(def cascade-diff-fields (fields-of holes "CascadeDiff"))
(def arm-one-fields (fields-of arms "ArmOneCascade"))
(def o4-fields ["actingOrderAfter" "actingOrderBefore" "precedenceAfter" "precedenceBefore"
                "scoreAfter" "scoreBefore"])
(def states-an-o4? (some? (re-find #"(?m)^  o4 :" attr)))
(def declares-a-carrier? (some? (re-find #"(?m)^structure \w+Cascade\b" attr)))

;; --- 8. declarations the slice and its review require -----------------------

(def required
  [[:rule-inductive "inductive ZaifPolicyRule"]
   [:provenance-transcription "def zaifProvenance"]
   [:provenance-support "theorem zaifProvenanceSupport"]
   [:provenance-silent-on-selected "theorem zaifProvenanceSilentOnSelected"]
   [:signature "abbrev AttributingOrganiseType"]
   [:predicate "structure ConformantOrganiseAttributing"]
   [:witness "def organiseAttributing"]
   [:witness-conformant "theorem organiseAttributingConformant"]
   [:mirror-refuted "theorem attributingMirrorNotConformant"]
   [:determined "theorem attributingThirdOriginDetermined"]
   [:determined-at-the-record "theorem attributingRecordedAdmissions"]
   [:policy-cascade "def zaifPolicyCascade"]
   [:temperament-refuted "theorem attributionNotFixedByTemperament"]
   [:review-provenance-range "theorem zaifProvenanceNeverTheStopRule"]
   [:review-predicate-without-the-clause "structure ConformantOrganiseAttributingSansAttr"]
   [:review-witness-without-the-clause "theorem organiseAttributingConformantSansAttr"]
   [:review-mirror-without-the-clause "theorem organiseAttributingMirrorConformantSansAttr"]
   [:review-agree-on-nodes "theorem attributingAgreeOnNodes"]
   [:review-agree-on-edges "theorem attributingAgreeOnEdges"]
   [:review-nonvacuity "theorem attributingZaifEdgeNonVacuous"]
   [:review-split-survives "theorem attributingSplitSurvivesWithoutOattr"]
   [:review-temperament-refuted-generally "theorem attributionNotFixedByAnyTemperament"]])

(def declarations
  ;; `(some? false)` is TRUE and `str/includes?` matches a PREFIX; slice 5 and
  ;; slice 7 each lost a check to one of those.  The needle must be followed by
  ;; something that cannot continue a Lean identifier.
  (into (sorted-map)
        (for [[k needle] required]
          [k (some? (re-find (re-pattern (str (java.util.regex.Pattern/quote needle)
                                              "(?![A-Za-z0-9_'])"))
                             attr))])))

;; --- 9. hygiene --------------------------------------------------------------

(def code-only
  (-> attr
      (str/replace #"(?s)/--.*?-/" " ")
      (str/replace #"(?s)/-!.*?-/" " ")))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b|\badmit\b|\bsorryAx\b|\bnative_decide\b"
                code-only)))

(def imports-slice-7?
  (some? (re-find #"(?m)^import DarkTower\.WarMachine\.F12AdmittingArm\b" attr)))

(def redeclared
  (vec (sort (map second (re-seq #"(?m)^(?:private )?def (d1\w+|trivialPolicyCascade|zaifAdmittedFor|organiseAdmitting\w*)"
                                 attr)))))

(defn- last-commit [rel]
  (let [{:keys [exit out]} (sh "git" "-C" (str home "/code/mathlib4")
                               "log" "-1" "--format=%H" "--" rel)]
    (when (zero? exit) (str/trim out))))

(def pins (into (sorted-map) (for [[rel sha] pinned] [rel (sorted-map :expected sha :actual (last-commit rel))])))
(def pins-moved (vec (sort (keep (fn [[rel m]] (when (not= (:expected m) (:actual m)) rel)) pins))))

;; --- verdict -----------------------------------------------------------------

(def problems
  (cond-> []
    (not= admitted-vertices expected-admitted)
    (conj [:recorded-admissions-are-not-the-transcribed-block admitted-vertices expected-admitted])

    (not= found-vertices expected-found)
    (conj [:recorded-found-entries-are-not-the-selected-block found-vertices expected-found])

    (seq other-entries)
    (conj (into [:provenance-entry-is-neither-found-nor-an-admission] other-entries))

    (seq unindexed)
    (conj (into [:provenance-names-a-pattern-outside-the-index] unindexed))

    (not= (count provenance) (:nodes-below enc))
    (conj [:provenance-does-not-cover-the-recorded-nodes (count provenance) (:nodes-below enc)])

    (not= 1 (count rule-ids))
    (conj (into [:the-run-names-more-than-one-rule] rule-ids))

    (not= rule-ids corpus-rule-ids)
    (conj [:the-corpus-names-a-rule-this-run-does-not rule-ids corpus-rule-ids])

    (not= (count rule-constructors) (count temperament-nodes))
    (conj [:constructors-do-not-match-the-recorded-temperament
           rule-constructors temperament-nodes])

    (not= (count policy-cascade-nodes) (count temperament-nodes))
    (conj [:policy-cascade-is-not-the-recorded-temperament
           policy-cascade-nodes temperament-nodes])

    (not= (count policy-cascade-precedence) (count temperament-precedence))
    (conj [:policy-cascade-precedence-is-not-the-recorded-one
           policy-cascade-precedence temperament-precedence])

    (not= policy-cascade-nodes (vec (sort policy-cascade-precedence)))
    (conj [:policy-cascade-orders-nodes-it-does-not-have
           policy-cascade-nodes policy-cascade-precedence])

    (not (every? (set (map name temperament-nodes)) (map name rule-ids)))
    (conj [:the-provenance-names-a-rule-outside-the-temperament rule-ids temperament-nodes])

    (= (count rule-ids) (count temperament-nodes))
    (conj [:the-provenance-exercises-every-temperament-node rule-ids temperament-nodes])

    (not names-the-recorded-rules?)
    (conj (into [:transcription-does-not-name-its-source-keywords] temperament-nodes))

    (not (re-find (re-pattern (str "if " (:admitted-from enc) " ≤ n ∧ n < " (:nodes-below enc)))
                  provenance-body))
    (conj [:transcription-bounds-are-not-the-recorded-block provenance-body])

    (seq temperament-pattern-keyed)
    (conj (into [:the-temperament-carries-a-pattern-keyed-map] temperament-pattern-keyed))

    (not= temperament-shared-nodes rule-ids)
    (conj [:temperament-nodes-are-not-the-recorded-rule temperament-shared-nodes rule-ids])

    (not= attributing-type expected-attributing-type)
    (conj [:signature-is-not-slice-7-plus-one-argument attributing-type expected-attributing-type])

    (not= [:oattr] clauses-added-over-slice-7)
    (conj (into [:clauses-added-over-slice-7-are-not-oattr-alone] clauses-added-over-slice-7))

    (seq inherited-clauses-that-moved)
    (conj (into [:an-inherited-clause-changed] inherited-clauses-that-moved))

    (not= "∀ t sel repo prov, (f t sel repo prov).admittedBy = {p | (prov p).isSome}"
          (:oattr attributing-clauses))
    (conj [:oattr-is-not-the-projection-equation (:oattr attributing-clauses)])

    (not= [:oattr] (vec (sort (set/difference (set (keys attributing-clauses))
                                              (set (keys sans-attr-clauses))))))
    (conj (into [:sans-attr-predicate-drops-more-than-oattr]
                (sort (set/difference (set (keys attributing-clauses))
                                      (set (keys sans-attr-clauses))))))

    (seq (for [k (keys sans-attr-clauses)
               :when (not= (get sans-attr-clauses k) (get attributing-clauses k))] k))
    (conj (into [:sans-attr-predicate-changed-an-inherited-clause]
                (for [k (keys sans-attr-clauses)
                      :when (not= (get sans-attr-clauses k) (get attributing-clauses k))] k)))

    states-an-o4?
    (conj [:arm-file-states-an-o4-clause])

    declares-a-carrier?
    (conj [:arm-file-declares-its-own-carrier])

    (seq (filter (set arm-one-fields) o4-fields))
    (conj (into [:arm-carrier-has-an-o4-field] (sort (filter (set arm-one-fields) o4-fields))))

    (seq (remove (set cascade-diff-fields) o4-fields))
    (conj (into [:cascadediff-lacks-an-o4-field]
                (sort (remove (set cascade-diff-fields) o4-fields))))

    (seq pins-moved)
    (conj (into [:a-pinned-file-moved] pins-moved))

    (not sorry-free?) (conj [:sorry-in-arm-file])
    (not imports-slice-7?) (conj [:does-not-import-the-slice-7-file])
    (seq redeclared) (conj (into [:redeclares-earlier-slice-data] redeclared))

    (seq (for [[k present?] declarations :when (not present?)] k))
    (conj (into [:missing-declarations]
                (for [[k present?] declarations :when (not present?)] k)))))

(def result
  (sorted-map
   :attribution
   (sorted-map :admitted-patterns admitted-patterns
               :admitted-vertices admitted-vertices
               :entries (count provenance)
               :expected-admitted expected-admitted
               :expected-found expected-found
               :found-vertices found-vertices
               :recomputed-from [:runs run-key :cascade :provenance]
               :rule-ids rule-ids)
   :basis (sorted-map :arm attr-path
                      :checks-dir checks-dir
                      :conformance conf-path
                      :d1-arms arms-path
                      :holes holes-path
                      :raw-record raw-path
                      :run run-key
                      :slice-7 arm-path
                      :transcription record-path
                      :zaif-basis (get-in record [:basis :sha]))
   :carriers (sorted-map :arm-one arm-one-fields
                         :cascade cascade-fields
                         :cascade-diff cascade-diff-fields
                         :o4-fields-absent-from-arm-one
                         (vec (sort (remove (set arm-one-fields) o4-fields))))
   :clauses (sorted-map :added-over-slice-7 clauses-added-over-slice-7
                        :attributing attributing-clauses
                        :inherited-that-moved inherited-clauses-that-moved
                        :sans-attr sans-attr-clauses
                        :slice-7 admitting-clauses)
   :declarations declarations
   :organise-type (sorted-map :attributing attributing-type
                              :admitting admitting-type
                              :expected-attributing expected-attributing-type
                              :in-holes organise-type-in-holes)
   :pins pins
   :problems problems
   :rule-grain (sorted-map :corpus corpus
                           :corpus-admissions corpus-admissions
                           :corpus-rule-ids corpus-rule-ids
                           :distinct-rule-ids-in-the-corpus (count corpus-rule-ids)
                           :note (str "every admission in every provenance map under "
                                      "futon3:checks names one rule id, so on all recorded "
                                      "data the attribution input is indistinguishable from "
                                      "a boolean admitted-or-not flag")
                           :records-without-provenance records-without-provenance)
   :temperament (sorted-map :keys temperament-keys
                            :nodes temperament-nodes
                            :pattern-keyed-maps temperament-pattern-keyed
                            :precedence temperament-precedence
                            :shared-nodes temperament-shared-nodes
                            :stops temperament-stops)
   :transcription (sorted-map :policy-cascade-nodes policy-cascade-nodes
                              :policy-cascade-precedence policy-cascade-precedence
                              :provenance-body provenance-body
                              :rule-constructors rule-constructors)))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-attribution: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-attribution: FAIL" (pr-str problems))) (System/exit 1))
  (println (str "f12-attribution: PASS -- the recorded provenance recomputed and resolved "
                "through the index, the corpus rule grain, the one-argument signature edit, "
                "the one added clause, the O4 non-statement, and both pinned files unmoved")))
