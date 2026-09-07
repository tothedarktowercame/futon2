#!/usr/bin/env bb
;; f12_cascadediff_check.bb -- `:F12` slice 13.  CHECKS the sixth arm's premises
;; against sources it does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  The sixth arm of `:organise-carrier` is the
;; FUNCTION framing (slice 6) at the `CascadeDiff` codomain (slice 5's arm two).
;; Its claim is that all four O-laws are jointly statable of one thing; its cost
;; is what that costs.  Both are claims about FIELD LISTS, a SIGNATURE, PREDICATE
;; TEXT and a RECORDED RUN, and every one of them can be false while `lake build`
;; stays green:
;;
;;   1. "O4 is statable at this carrier and at no other arm's" is a claim about
;;      which fields four structures have.  Read out of the sources.
;;   2. "The arm IS the registered one" holds only if it uses
;;      `armTwoOrganiseType` (`F12D1Arms.lean:119`) rather than a fresh synonym.
;;      Read out of both files and compared with `organise`'s own type.
;;   3. "The three predicates differ in exactly one clause each" is what makes
;;      `o4CounterexampleOtherClauses` and `organiseCascadeDiffUnpinnedAuthoredSansOAuth`
;;      locate a failure rather than merely exhibit one.  Slices 7, 8, 10 and 11
;;      each shipped a comparison that a hand-written second predicate could have
;;      made about something else; here the two are diffed clause by clause.
;;   4. "`oauth` pins a field no other clause reads" is the review's finding and
;;      it is a fact about the predicate's TEXT.  Checked by scanning every
;;      clause for `.authoredEdges`.
;;   5. "The O4 clause is satisfied VACUOUSLY on the recorded run" is a claim
;;      about `zaif-cascade.edn`, and its complement -- whether ANY recorded run
;;      could exercise it -- is a claim about the whole corpus.  Both recomputed
;;      from the records rather than read off a `:holds?` key.
;;   6. The Lean `o4` clause mirrors a Clojure law
;;      (`futon3:checks/find_organise.clj:531`), and they must agree INCLUDING in
;;      their vacuity: the Clojure law is a disjunction whose first disjunct is
;;      the negated antecedent, so it too passes on a flat precedence.  Read out
;;      of the Clojure source rather than described.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling on D1, on the O3 field question, on
;; D2/D3, or on whether the sorry at `Holes.lean:861` should be discharged.  It
;; measures one unrun arm so the registry's two `:unmeasured` fields can be
;; filled with something a reader can check.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_ARM, F12_ARMS, F12_CONF,
;; F12_ADMIT, F12_HOLES, F12_CHECKS, F12_ORGANISE, F12_CONSTRUCT, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def wm (str home "/code/mathlib4/DarkTower/WarMachine"))
(def arm-path (or (System/getenv "F12_ARM") (str wm "/F12CascadeDiffArm.lean")))
(def arms-path (or (System/getenv "F12_ARMS") (str wm "/F12D1Arms.lean")))
(def conf-path (or (System/getenv "F12_CONF") (str wm "/F12Conformance.lean")))
(def admit-path (or (System/getenv "F12_ADMIT") (str wm "/F12AdmittingArm.lean")))
(def holes-path (or (System/getenv "F12_HOLES") (str wm "/Holes.lean")))
(def checks-dir (or (System/getenv "F12_CHECKS") (str home "/code/futon3/checks")))
(def organise-path (or (System/getenv "F12_ORGANISE") (str checks-dir "/find_organise.clj")))
(def construct-path (or (System/getenv "F12_CONSTRUCT") (str checks-dir "/construct_cascade.clj")))
(def out-path (or (System/getenv "F12_OUT")
                  (str lab "/runs/F12-organise/08-cascadediff-arm.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f12-cascadediff:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path))
    (slurp path)
    (die "file not found" {:what what :path path})))

(def arm (read-source arm-path :cascadediff-arm))
(def arms (read-source arms-path :d1-arms))
(def conf (read-source conf-path :conformance))
(def admit (read-source admit-path :admitting-arm))
(def holes (read-source holes-path :holes))
(def organise-src (read-source organise-path :find-organise))
(def construct-src (read-source construct-path :construct-cascade))

(defn- squeeze [s] (str/trim (str/replace s #"\s+" " ")))

;; --- 1. the signature is the REGISTERED one, not a fresh synonym ------------

(def organise-type-in-holes
  (if-let [m (re-find #"def organise \{Policy P : Type\*\} :\s*([^:=]+):= sorry" holes)]
    (squeeze (second m))
    (die "Holes.lean does not declare organise in the expected shape" {:path holes-path})))

(def arm-two-type
  (if-let [m (re-find #"abbrev armTwoOrganiseType \(Policy P Score : Type\*\) :=\s*\n?\s*([^\n]+)" arms)]
    (squeeze (second m))
    (die "F12D1Arms states no armTwoOrganiseType abbrev" {:path arms-path})))

;; The one edit the arm claims: the result carrier, and nothing before it.
(def expected-arm-two-type
  (str/replace organise-type-in-holes #"Cascade P$" "CascadeDiff P Score"))

;; C550 section 3's finding was that `armTwoOrganiseType` is an abbrev NO
;; theorem mentions, so the build checked only its well-formedness.  Running the
;; arm is supposed to end that, so the count is the thing to check -- not that
;; the name appears somewhere, which it always did.
(def arm-two-uses-in-arm-file
  (count (re-seq #"armTwoOrganiseType(?![A-Za-z0-9_'])" arm)))

(def arm-declares-its-own-signature-abbrev?
  (some? (re-find #"(?m)^abbrev \w+ \(Policy P Score : Type\*\) :=" arm)))

;; --- 2. the four carriers and where O4 is statable --------------------------

(defn- fields-of [src structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\([^)]*\\) where\n(.*?)\n\n"))
                      src)]
    (vec (sort (map second (re-seq #"(?m)^  (\w+)\s*:" (second m)))))
    (die "no fields found for structure" {:structure structure-name})))

(def cascade-fields (fields-of holes "Cascade"))
(def cascade-diff-fields (fields-of holes "CascadeDiff"))
(def arm-one-fields (fields-of arms "ArmOneCascade"))
(def repository-fields (fields-of holes "Repository"))
(def o4-fields ["actingOrderAfter" "actingOrderBefore" "precedenceAfter" "precedenceBefore"
                "scoreAfter" "scoreBefore"])
(def o1-origin-fields ["addedByOrganise" "admittedBy" "selected"])

(def o4-statable-at
  (vec (sort (for [[nm fs] {"ArmOneCascade" arm-one-fields
                            "Cascade" cascade-fields
                            "CascadeDiff" cascade-diff-fields
                            "Repository" repository-fields}
                   :when (empty? (remove (set fs) o4-fields))]
               nm))))

(def o1-three-way-statable-at
  (vec (sort (for [[nm fs] {"ArmOneCascade" arm-one-fields
                            "Cascade" cascade-fields
                            "CascadeDiff" cascade-diff-fields
                            "Repository" repository-fields}
                   :when (empty? (remove (set fs) o1-origin-fields))]
               nm))))

;; --- 3. the three predicates, diffed clause by clause -----------------------

(defn- clauses-of
  "The clause fields of a conformance structure, as squeezed source lines."
  [src structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\{Policy P[^}]*\\}.*?where\n(.*?)\n\n"))
                      src)]
    (into (sorted-map)
          (for [[_ k body] (re-seq #"(?m)^  (\w+) :((?:[^\n]|\n    )+)" (second m))]
            [(keyword k) (squeeze body)]))
    (die "no such conformance structure" {:structure structure-name})))

(def full-clauses (clauses-of arm "ConformantOrganiseCascadeDiff"))
(def sans-o4-clauses (clauses-of arm "ConformantOrganiseCascadeDiffSansO4"))
(def sans-oauth-clauses (clauses-of arm "ConformantOrganiseCascadeDiffSansOAuth"))
(def slice7-clauses (clauses-of admit "ConformantOrganiseAdmitting"))
(def slice6-clauses (clauses-of conf "ConformantOrganiseNodes"))

(defn- clause-diff
  "Keys on which two clause maps disagree, INCLUDING keys present in only one."
  [a b]
  (vec (sort (for [k (set/union (set (keys a)) (set (keys b)))
                   :when (not= (get a k) (get b k))]
               k))))

(def full-vs-sans-o4 (clause-diff full-clauses sans-o4-clauses))
(def full-vs-sans-oauth (clause-diff full-clauses sans-oauth-clauses))

;; The review finding, read out of the predicate rather than asserted: which
;; clauses mention the RESULT's authoredEdges field at all.  O2 and O3 here read
;; `repo.standsOn`, the ARGUMENT, so `oauth` is the only clause that touches the
;; twelfth field -- which is why pinning it changes nothing the laws read.
(def clauses-mentioning-result-authored
  (vec (sort (for [[k body] full-clauses
                   :when (str/includes? body ".authoredEdges")]
               k))))

;; The value-grain O2 in Holes.lean DOES read the field, because no repository is
;; in scope there.  Read out, so the contrast is checked and not remembered.
(def value-grain-o2-reads-the-field?
  (some? (re-find #"organiseO2AuthoredReachabilityZaif[\s\S]{0,400}?wmZaifCascadeDiffFixture\.authoredEdges"
                  holes)))

;; --- 4. the O4 corpus sweep, RECOMPUTED from the records --------------------

(def record-files
  (vec (sort (for [f (.listFiles (io/file checks-dir))
                   :let [n (.getName f)]
                   :when (re-matches #".*cascade.*\.edn" n)]
               n))))

(def o4-rows
  (vec (for [n record-files
             :let [m (edn/read-string (slurp (str checks-dir "/" n)))
                   o (:o4 m)]]
         (cond
           (map? o)
           (let [r (:row o)
                 ante (not= (:precedence-before r) (:precedence-after r))
                 acting (not= (:acting-order-before r) (:acting-order-after r))
                 score (not= (:score-before r) (:score-after r))]
             (sorted-map :record n :o4 :row
                         :antecedent-true? ante
                         :acting-order-moves? acting
                         :score-moves? score
                         ;; the clause as the Lean `o4` field states it
                         :clause-satisfied? (or (not ante) acting score)
                         ;; satisfied WITH the antecedent true: the only way the
                         ;; clause is doing work on a record
                         :non-vacuous? (and ante (or acting score))))
           (keyword? o) (sorted-map :record n :o4 :declined :reason o)
           (nil? o) (sorted-map :record n :o4 :absent)
           :else (sorted-map :record n :o4 :unexpected)))))

(def o4-sweep
  (sorted-map
   :records (count record-files)
   :with-a-row (count (filter #(= :row (:o4 %)) o4-rows))
   :declined-with-a-reason (count (filter #(= :declined (:o4 %)) o4-rows))
   :no-o4-key (count (filter #(= :absent (:o4 %)) o4-rows))
   :antecedent-true-on (vec (sort (map :record (filter :antecedent-true? o4-rows))))
   :non-vacuous-on (vec (sort (map :record (filter :non-vacuous? o4-rows))))
   ;; the run every D1 arm is instantiated at
   :zaif (first (filter #(= "zaif-cascade.edn" (:record %)) o4-rows))))

;; --- 5. the Clojure law the Lean clause mirrors -----------------------------

(def clojure-o4
  (if-let [m (re-find #"(?s)\(defn o4-precedence-governance.*?\n  \(or (.*?)\)\)\n" organise-src)]
    (squeeze (str "(or " (second m) "))"))
    (die "find_organise.clj states no o4-precedence-governance in the expected shape"
         {:path organise-path})))

;; Its FIRST disjunct is the negated antecedent, so the Clojure law passes on a
;; flat precedence exactly as the Lean clause does.  That agreement is the point:
;; the vacuity slice 13 measures is not an artefact of the Lean transcription.
(def clojure-o4-first-disjunct-is-the-negated-antecedent?
  (some? (re-find #"\(or \(= precedence-before precedence-after\)" clojure-o4)))

;; The producer writes both vectors as LITERALS.  If that ever becomes a computed
;; value the vacuity finding is stale, so this check fails rather than passes.
(def producer-writes-empty-precedence-literals?
  (some? (re-find #"(?m)^\s*:precedence-before \[\]\n\s*:precedence-after \[\]" construct-src)))

;; --- 6. declarations the slice and its review require -----------------------

(def required
  [[:predicate "structure ConformantOrganiseCascadeDiff"]
   [:predicate-sans-o4 "structure ConformantOrganiseCascadeDiffSansO4"]
   [:predicate-sans-oauth "structure ConformantOrganiseCascadeDiffSansOAuth"]
   [:witness "def organiseCascadeDiff"]
   [:witness-conformant "theorem organiseCascadeDiffConformant"]
   [:recorded-nodes "theorem organiseCascadeDiffZaifNodes"]
   [:recorded-selected "theorem organiseCascadeDiffZaifSelected"]
   [:recorded-admitted "theorem organiseCascadeDiffZaifAdmitted"]
   [:nonvacuity-edge "theorem organiseCascadeDiffZaifEdge"]
   [:nonvacuity-unauthored "theorem organiseCascadeDiffNoUnauthoredEdge"]
   [:o4-vacuous-general "theorem o4VacuousWhenPrecedenceFlat"]
   [:o4-variant "def organiseCascadeDiffRecordedVariant"]
   [:o4-variant-conformant "theorem organiseCascadeDiffRecordedVariantConformant"]
   [:o4-variant-conformant-sans "theorem organiseCascadeDiffRecordedVariantConformantSansO4"]
   [:o4-vacuity-exposed "theorem recordedVariantsExposeO4Vacuity"]
   [:o4-counterexample "def organiseCascadeDiffO4Counterexample"]
   [:o4-counterexample-other-clauses "theorem o4CounterexampleOtherClauses"]
   [:o4-counterexample-not-conformant "theorem o4CounterexampleNotConformant"]
   [:oauth-refutation "theorem sansOAuthStillForcesRepositoryReachability"]
   [:oauth-drift-witness "def organiseCascadeDiffUnpinnedAuthored"]
   [:oauth-drift-conformant-sans "theorem organiseCascadeDiffUnpinnedAuthoredSansOAuth"]
   [:oauth-drift-at-recorded-edge "theorem unpinnedAuthoredFieldDriftsAtRecordedEdge"]
   [:oauth-drift-not-conformant "theorem unpinnedAuthoredNotFullyConformant"]
   [:all-four-laws "theorem cascadeDiffRecordedAllLaws"]
   ;; review additions
   [:review-score-empty "theorem cascadeDiffArmIsEmptyAtAnEmptyScore"]
   [:review-score-comparand "theorem organiseTypeIsInhabitedAtTheSameArguments"]
   [:review-pin "def pinAuthored"]
   [:review-pin-agrees "theorem pinAuthoredAgreesOutsideAuthoredEdges"]
   [:review-pin-conformant "theorem pinAuthoredIsConformant"]
   [:review-pin-repairs "theorem pinAuthoredRepairsTheDriftingWitness"]])

(def declarations
  ;; TWO ways a presence test lies, both found by earlier slices' plants.
  ;; `(some? false)` is TRUE, so the obvious spelling records everything as
  ;; present.  And a bare substring match takes a PREFIX, so
  ;; `organiseCascadeDiffConformant` would silently match the longer
  ;; `organiseCascadeDiffConformantSansO4` beside it.  The needle must be
  ;; followed by something that cannot continue a Lean identifier.
  (into (sorted-map)
        (for [[k needle] required]
          [k (some? (re-find (re-pattern (str (java.util.regex.Pattern/quote needle)
                                              "(?![A-Za-z0-9_'])"))
                             arm))])))

;; --- verdict ----------------------------------------------------------------

;; Scanned over CODE, not prose: slice 6's first spelling of this check failed on
;; the word `admit` inside a docstring about the policy-grain `admit` edit.
(def code-only
  (-> arm
      (str/replace #"(?s)/--.*?-/" " ")
      (str/replace #"(?s)/-!.*?-/" " ")))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b|\badmit\b|\bsorryAx\b|\bnative_decide\b"
                code-only)))

(def imports-admitting-arm?
  (some? (re-find #"(?m)^import DarkTower\.WarMachine\.F12AdmittingArm\b" arm)))

(def redeclared
  (vec (sort (map second (re-seq #"(?m)^(?:private )?def (d1\w+|trivialPolicyCascade|zaifAdmittedFor)"
                                arm)))))

(def problems
  (cond-> []
    (not= arm-two-type expected-arm-two-type)
    (conj [:arm-two-signature-is-not-the-codomain-edit arm-two-type expected-arm-two-type])

    (< arm-two-uses-in-arm-file 1)
    (conj [:arm-does-not-use-the-registered-signature arm-two-uses-in-arm-file])

    arm-declares-its-own-signature-abbrev?
    (conj [:arm-declares-a-fresh-synonym-instead-of-reusing-arm-two])

    (not= ["CascadeDiff"] o4-statable-at)
    (conj (into [:o4-is-statable-somewhere-else-too] o4-statable-at))

    (not= ["ArmOneCascade" "CascadeDiff"] o1-three-way-statable-at)
    (conj (into [:three-way-o1-carriers-moved] o1-three-way-statable-at))

    (not= [:o4] full-vs-sans-o4)
    (conj (into [:sans-o4-predicate-differs-outside-o4] full-vs-sans-o4))

    (not= [:oauth] full-vs-sans-oauth)
    (conj (into [:sans-oauth-predicate-differs-outside-oauth] full-vs-sans-oauth))

    (not= [:oauth] clauses-mentioning-result-authored)
    (conj (into [:a-law-clause-reads-the-result-authored-field]
                clauses-mentioning-result-authored))

    (not value-grain-o2-reads-the-field?)
    (conj [:value-grain-o2-no-longer-reads-authoredEdges])

    (not (str/includes? (:o1 full-clauses) "admittedBy"))
    (conj [:o1-is-not-three-way (:o1 full-clauses)])

    (not= (:o1 full-clauses) (:o1 slice7-clauses))
    (conj [:o1-differs-from-slice-7 (:o1 full-clauses) (:o1 slice7-clauses)])

    (str/includes? (:o1 slice6-clauses) "admittedBy")
    (conj [:slice-6-o1-was-already-three-way (:o1 slice6-clauses)])

    (not (str/includes? (:o3 full-clauses) "(f t sel repo).nodes"))
    (conj [:o3-is-not-the-node-set-reading (:o3 full-clauses)])

    (not= 1 (:with-a-row o4-sweep))
    (conj [:o4-row-count-moved (:with-a-row o4-sweep)])

    (not= ["ants-cascade.edn"] (:non-vacuous-on o4-sweep))
    (conj (into [:non-vacuous-o4-records-moved] (:non-vacuous-on o4-sweep)))

    (not= :declined (:o4 (:zaif o4-sweep)))
    (conj [:the-zaif-run-now-carries-an-o4-row (:zaif o4-sweep)])

    (not clojure-o4-first-disjunct-is-the-negated-antecedent?)
    (conj [:clojure-o4-is-not-the-same-implication clojure-o4])

    (not producer-writes-empty-precedence-literals?)
    (conj [:producer-no-longer-writes-empty-precedence-literals])

    (not sorry-free?) (conj [:sorry-in-arm-file])
    (not imports-admitting-arm?) (conj [:does-not-import-the-slice-7-arm-file])
    (seq redeclared) (conj (into [:redeclares-earlier-slice-data] redeclared))

    (seq (for [[k present?] declarations :when (not present?)] k))
    (conj (into [:missing-declarations]
                (for [[k present?] declarations :when (not present?)] k)))))

(def result
  (sorted-map
   :basis (sorted-map :admitting-arm admit-path
                      :arm arm-path
                      :arms arms-path
                      :checks checks-dir
                      :conformance conf-path
                      :construct construct-path
                      :find-organise organise-path
                      :holes holes-path)
   :carriers (sorted-map :arm-one arm-one-fields
                         :cascade cascade-fields
                         :cascade-diff cascade-diff-fields
                         :repository repository-fields
                         :o4-statable-at o4-statable-at
                         :three-way-o1-statable-at o1-three-way-statable-at)
   :clauses (sorted-map :full full-clauses
                        :sans-o4 sans-o4-clauses
                        :sans-oauth sans-oauth-clauses
                        :full-vs-sans-o4 full-vs-sans-o4
                        :full-vs-sans-oauth full-vs-sans-oauth
                        :mentioning-result-authored-edges clauses-mentioning-result-authored
                        :value-grain-o2-reads-the-field value-grain-o2-reads-the-field?)
   :declarations declarations
   :o4 (sorted-map :clojure-law clojure-o4
                   :clojure-first-disjunct-is-negated-antecedent
                   clojure-o4-first-disjunct-is-the-negated-antecedent?
                   :producer-writes-empty-literals producer-writes-empty-precedence-literals?
                   :rows o4-rows
                   :sweep o4-sweep)
   :organise-type (sorted-map :arm-two arm-two-type
                              :arm-two-uses-in-the-arm-file arm-two-uses-in-arm-file
                              :expected-arm-two expected-arm-two-type
                              :in-holes organise-type-in-holes)
   :problems problems))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-cascadediff: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-cascadediff: FAIL" (pr-str problems))) (System/exit 1))
  (println (str "f12-cascadediff: PASS -- the registered arm-two signature reused and now used, "
                "four carrier field lists with O4 statable at CascadeDiff alone, three predicates "
                "diffed clause by clause, oauth the only clause reading the result's authoredEdges, "
                "and the O4 corpus sweep recomputed from the nine records")))
