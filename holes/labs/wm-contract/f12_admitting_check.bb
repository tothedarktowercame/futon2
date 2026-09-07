#!/usr/bin/env bb
;; f12_admitting_check.bb -- `:F12` slice 7.  CHECKS the admitting-arm file's
;; premises against sources it does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  Slice 7 keeps `organise`'s argument list from
;; `Holes.lean:861` and widens only its RESULT carrier to slice 5's
;; `ArmOneCascade`.  Every verdict it reaches is therefore a verdict about a
;; signature and a field list, and four things can go wrong while `lake build`
;; stays green, because each of them leaves the file elaborating perfectly
;; while being about something else:
;;
;;   1. `AdmittingOrganiseType` could differ from `organise`'s type in more
;;      than the codomain -- and then the slice's "same arguments, wider
;;      result" reading is false and its comparison with slice 6 does not
;;      transfer.  Checked by reading BOTH types out of their own files and
;;      substituting only the final carrier.
;;   2. The arm still cannot state O4, and that is a claim about which fields
;;      `ArmOneCascade` has.  A comment cannot be wrong in a way the build
;;      notices.  Checked by reading three field lists and requiring the six
;;      O4 fields absent from `Cascade` and `ArmOneCascade` and present in
;;      `CascadeDiff`.
;;   3. `ConformantOrganiseAdmitting` adds a clause the record's O-laws do not
;;      have (`osel`, pinning the widened `selected` field to the function's
;;      argument), and its O1 is the THREE-way union where slice 6's is the
;;      two-way one.  Both are visible only in the predicate's own text, so
;;      the text is read clause by clause and compared with slice 6's.
;;   4. The two witnesses are recorded-data lookups, not algorithms.  That is
;;      what makes the underdetermination result cheap to obtain and it is
;;      what a reader needs told; the shape is read out of `zaifAdmittedFor`
;;      rather than described in prose.
;;
;; It also RECOMPUTES the fast-forward relation over the recorded nodes and
;; over the recorded selected set from the record's own edge list, rather than
;; reading slice 2's stored answer, because the two Lean statements the review
;; added (`admittingZaifEdgeNonVacuous`, which needs edge 18-19 to exist, and
;; `admittingSelectedReadingEdgesEmptyOnRecorded`, which needs the selected
;; relation to be empty) are exactly claims about those two relations.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling on D1, on the O3 field question, on
;; D2/D3, or on whether the sorry at `Holes.lean:861` should be discharged.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two
;; runs over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_ARM, F12_ARMS, F12_CONF,
;; F12_HOLES, F12_RECORD, F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def wm (str home "/code/mathlib4/DarkTower/WarMachine"))
(def arm-path (or (System/getenv "F12_ARM") (str wm "/F12AdmittingArm.lean")))
(def arms-path (or (System/getenv "F12_ARMS") (str wm "/F12D1Arms.lean")))
(def conf-path (or (System/getenv "F12_CONF") (str wm "/F12Conformance.lean")))
(def holes-path (or (System/getenv "F12_HOLES") (str wm "/Holes.lean")))
(def record-path (or (System/getenv "F12_RECORD")
                     (str lab "/runs/F12-organise/01-zaif-transcription.edn")))
(def out-path (or (System/getenv "F12_OUT") (str lab "/runs/F12-organise/05-admitting-arm.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f12-admitting:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path))
    (slurp path)
    (die "file not found" {:what what :path path})))

(def arm (read-source arm-path :admitting-arm))
(def arms (read-source arms-path :d1-arms))
(def conf (read-source conf-path :conformance))
(def holes (read-source holes-path :holes))
(def record (edn/read-string (slurp record-path)))

(defn- squeeze [s] (str/trim (str/replace s #"\s+" " ")))

;; --- 1. the signature, changed in the codomain and nowhere else -------------

(def organise-type-in-holes
  (if-let [m (re-find #"def organise \{Policy P : Type\*\} :\s*([^:=]+):= sorry" holes)]
    (squeeze (second m))
    (die "Holes.lean does not declare organise in the expected shape" {:path holes-path})))

(def arm-type
  (if-let [m (re-find #"abbrev AdmittingOrganiseType \(Policy P : Type\*\) :=\s*\n?\s*([^\n]+)" arm)]
    (squeeze (second m))
    (die "the arm file states no AdmittingOrganiseType abbrev" {:path arm-path})))

;; The one edit the slice claims to have made: the result carrier, and nothing
;; before it.
(def expected-arm-type (str/replace organise-type-in-holes #"Cascade P$" "ArmOneCascade P"))

;; --- 2. the three carriers and the O4 non-statement -------------------------

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
(def o1-origin-fields ["addedByOrganise" "admittedBy" "selected"])

;; --- 3. the conformance predicates, clause by clause ------------------------

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

(def admitting-clauses (clauses-of arm "ConformantOrganiseAdmitting"))
(def admitting-selected-clauses (clauses-of arm "ConformantOrganiseAdmittingSelected"))
(def slice6-nodes-clauses (clauses-of conf "ConformantOrganiseNodes"))
(def slice6-selected-clauses (clauses-of conf "ConformantOrganiseSelected"))

(def clauses-added-over-slice-6
  (vec (sort (set/difference (set (keys admitting-clauses)) (set (keys slice6-nodes-clauses))))))

(def readings-differ-only-in
  (vec (sort (for [k (set/union (set (keys admitting-clauses))
                                (set (keys admitting-selected-clauses)))
                   :when (not= (get admitting-clauses k) (get admitting-selected-clauses k))]
               k))))

;; --- 4. what shape the witnesses have ---------------------------------------

(def admitted-lookup
  (if-let [m (re-find #"(?s)def zaifAdmittedFor \(sel : Set Nat\) : Set Nat :=\s*\n\s*(.*?)\n\n" arm)]
    (squeeze (second m))
    (die "the arm file states no zaifAdmittedFor" {:path arm-path})))

(def witness-shape
  (if (= admitted-lookup "if sel = d1Selected then d1Admitted else ∅")
    :recorded-lookup-table
    :other))

;; --- 5. the two edge relations, RECOMPUTED from the record ------------------

(def authored (set (map vec (:authored-edges record))))
(def enc (:encoding record))
(def selected-below (:selected-below enc))
(def admitted-from (:admitted-from enc))
(def nodes-below (:nodes-below enc))
(def selected-set (set (range selected-below)))
(def admitted-set (set (range admitted-from nodes-below)))
(def nodes-set (set (range nodes-below)))
(def vertices (into (set/union nodes-set) (mapcat identity authored)))

(defn- reach-outside
  "Pairs (u, v) joined by an authored path whose INTERMEDIATE vertices are all
  outside `inside`.  This is `ReachOutside` at `Holes.lean:823-826`."
  [inside]
  (loop [paths (set (map vec authored))]
    (let [grown (into paths
                      (for [[u x] paths
                            [x2 v] authored
                            :when (and (= x x2) (not (inside x)))]
                        [u v]))]
      (if (= grown paths) paths (recur grown)))))

(defn- fast-forward
  "`fastForward` at `Holes.lean:829-831`: both endpoints inside, path outside."
  [inside]
  (vec (sort (for [[u v] (reach-outside inside) :when (and (inside u) (inside v))] [u v]))))

(def ff-over-nodes (fast-forward nodes-set))
(def ff-over-selected (fast-forward selected-set))

(def vertex-facts
  (sorted-map
   ;; the vertex both split theorems read the disagreement at
   :split-witness 11
   :split-witness-admitted? (contains? admitted-set 11)
   :split-witness-selected? (contains? selected-set 11)
   ;; the edge the non-vacuity theorem names
   :nonvacuity-edge [18 19]
   :nonvacuity-edge-authored? (contains? authored [18 19])
   :nonvacuity-edge-endpoints-selected? (mapv #(contains? selected-set %) [18 19])
   :nonvacuity-edge-endpoints-nodes? (mapv #(contains? nodes-set %) [18 19])))

;; --- 6. declarations the slice and its review require -----------------------

(def required
  [[:signature "abbrev AdmittingOrganiseType"]
   [:predicate-nodes-reading "structure ConformantOrganiseAdmitting"]
   [:predicate-selected-reading "structure ConformantOrganiseAdmittingSelected"]
   [:o2-at-recorded-repo "theorem admittingArmO2AtZaifRepo"]
   [:o3-at-recorded-repo "theorem admittingArmO3AtZaifRepo"]
   [:recorded-lookup "def zaifAdmittedFor"]
   [:witness-admitting "def organiseAdmitting"]
   [:witness-admitting-conformant "theorem organiseAdmittingConformant"]
   [:witness-mirror "def organiseAdmittingMirror"]
   [:witness-mirror-conformant "theorem organiseAdmittingMirrorConformant"]
   [:o1-third-origin-nine "theorem organiseAdmittingZaifAdmitsNine"]
   [:o1-nodes-are-the-twenty "theorem organiseAdmittingZaifNodes"]
   [:agree-on-nodes "theorem admittingAgreeOnNodes"]
   [:agree-on-edges "theorem admittingAgreeOnEdges"]
   [:split "theorem admittingSplitUnderdetermined"]
   [:split-general "theorem admittingThirdOriginUnconstrainedGeneral"]
   [:review-nonvacuity "theorem admittingZaifEdgeNonVacuous"]
   [:review-witness-selected-reading "def organiseAdmittingSelectedReading"]
   [:review-witness-selected-reading-mirror "def organiseAdmittingSelectedReadingMirror"]
   [:review-conformant-selected-reading "theorem organiseAdmittingSelectedReadingConformant"]
   [:review-conformant-selected-reading-mirror
    "theorem organiseAdmittingSelectedReadingMirrorConformant"]
   [:review-split-selected-reading "theorem admittingSplitUnderdeterminedSelectedReading"]
   [:review-rank-zero "theorem d1Rank_eq_zero_of_selected"]
   [:review-selected-reading-edges-empty "theorem admittingSelectedReadingEdgesEmptyOnRecorded"]])

(def declarations
  ;; TWO ways a presence test lies, one caught by a slice-5 plant and one by a
  ;; plant against THIS checker.  `(some? false)` is TRUE, so the obvious
  ;; spelling records every declaration as present whatever the source says.
  ;; And `str/includes?` matches a PREFIX: renaming `admittingZaifEdgeNonVacuous`
  ;; to `admittingZaifEdgeNonVacuousRENAMED` left this check green, and so would
  ;; `admittingSplitUnderdetermined` silently matching the longer
  ;; `admittingSplitUnderdeterminedSelectedReading` beside it.  So the needle
  ;; must be followed by something that cannot continue a Lean identifier.
  (into (sorted-map)
        (for [[k needle] required]
          [k (some? (re-find (re-pattern (str (java.util.regex.Pattern/quote needle)
                                              "(?![A-Za-z0-9_'])"))
                             arm))])))

;; --- verdict ----------------------------------------------------------------

;; Scanned over CODE, not prose: slice 6's first spelling of this check failed
;; on the word `admit` inside a docstring about the policy-grain `admit` edit,
;; a true sentence about the subject matter.
(def code-only
  (-> arm
      (str/replace #"(?s)/--.*?-/" " ")
      (str/replace #"(?s)/-!.*?-/" " ")))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b|\badmit\b|\bsorryAx\b|\bnative_decide\b"
                code-only)))

(def imports-conformance?
  (some? (re-find #"(?m)^import DarkTower\.WarMachine\.F12Conformance\b" arm)))

(def redeclared
  (vec (sort (map second (re-seq #"(?m)^(?:private )?def (d1\w+|trivialPolicyCascade)" arm)))))

(def states-an-o4? (some? (re-find #"(?m)^  o4 :" arm)))

(def problems
  (cond-> []
    (not= arm-type expected-arm-type)
    (conj [:signature-is-not-the-codomain-edit arm-type expected-arm-type])

    (not= (vec (sort (concat cascade-fields ["admittedBy" "selected"]))) arm-one-fields)
    (conj [:arm-carrier-is-not-cascade-plus-two-origins arm-one-fields cascade-fields])

    (seq (filter (set arm-one-fields) o4-fields))
    (conj (into [:arm-carrier-has-an-o4-field] (sort (filter (set arm-one-fields) o4-fields))))

    (seq (remove (set cascade-diff-fields) o4-fields))
    (conj (into [:cascadediff-lacks-an-o4-field]
                (sort (remove (set cascade-diff-fields) o4-fields))))

    (seq (remove (set arm-one-fields) o1-origin-fields))
    (conj (into [:arm-carrier-lacks-an-o1-origin]
                (sort (remove (set arm-one-fields) o1-origin-fields))))

    states-an-o4?
    (conj [:arm-file-states-an-o4-clause])

    (not= [:osel] clauses-added-over-slice-6)
    (conj (into [:clauses-added-over-slice-6-are-not-osel-alone] clauses-added-over-slice-6))

    (not= "∀ t sel repo, (f t sel repo).selected = sel" (:osel admitting-clauses))
    (conj [:osel-is-not-the-field-fidelity-equation (:osel admitting-clauses)])

    (not (str/includes? (:o1 admitting-clauses) "(f t sel repo).admittedBy"))
    (conj [:admitting-o1-is-not-three-way (:o1 admitting-clauses)])

    (str/includes? (:o1 slice6-nodes-clauses) "admittedBy")
    (conj [:slice-6-o1-was-already-three-way (:o1 slice6-nodes-clauses)])

    (not= (:o3 admitting-clauses) (:o3 slice6-nodes-clauses))
    (conj [:arm-o3-is-not-slice-6-node-set-reading
           (:o3 admitting-clauses) (:o3 slice6-nodes-clauses)])

    (not= (:o3 admitting-selected-clauses) (:o3 slice6-selected-clauses))
    (conj [:arm-selected-o3-is-not-slice-6-selected-reading
           (:o3 admitting-selected-clauses) (:o3 slice6-selected-clauses)])

    (not= [:o3] readings-differ-only-in)
    (conj (into [:the-two-readings-differ-outside-o3] readings-differ-only-in))

    (not= :recorded-lookup-table witness-shape)
    (conj [:witness-is-not-the-recorded-lookup admitted-lookup])

    (not= [[18 19]] ff-over-nodes)
    (conj [:fast-forward-over-nodes-moved ff-over-nodes])

    (seq ff-over-selected)
    (conj (into [:fast-forward-over-selected-not-empty] ff-over-selected))

    (not= ff-over-nodes (mapv vec (:over-nodes (:fast-forward record))))
    (conj [:recomputation-disagrees-with-the-record
           ff-over-nodes (:over-nodes (:fast-forward record))])

    (not (:split-witness-admitted? vertex-facts))
    (conj [:split-witness-is-not-admitted 11])

    (:split-witness-selected? vertex-facts)
    (conj [:split-witness-is-selected 11])

    (not (:nonvacuity-edge-authored? vertex-facts))
    (conj [:nonvacuity-edge-is-not-authored [18 19]])

    (some true? (:nonvacuity-edge-endpoints-selected? vertex-facts))
    (conj [:nonvacuity-edge-endpoint-is-selected
           (:nonvacuity-edge-endpoints-selected? vertex-facts)])

    (not (every? true? (:nonvacuity-edge-endpoints-nodes? vertex-facts)))
    (conj [:nonvacuity-edge-endpoint-is-not-a-node
           (:nonvacuity-edge-endpoints-nodes? vertex-facts)])

    (not sorry-free?) (conj [:sorry-in-arm-file])
    (not imports-conformance?) (conj [:does-not-import-the-slice-6-conformance-file])
    (seq redeclared) (conj (into [:redeclares-earlier-slice-data] redeclared))

    (seq (for [[k present?] declarations :when (not present?)] k))
    (conj (into [:missing-declarations]
                (for [[k present?] declarations :when (not present?)] k)))))

(def result
  (sorted-map
   :basis (sorted-map :arm arm-path
                      :arms arms-path
                      :conformance conf-path
                      :holes holes-path
                      :record record-path
                      :run (get-in record [:basis :run])
                      :zaif-basis (get-in record [:basis :sha]))
   :carriers (sorted-map :arm-one arm-one-fields
                         :cascade cascade-fields
                         :cascade-diff cascade-diff-fields
                         :o4-fields-absent-from-arm-one
                         (vec (sort (remove (set arm-one-fields) o4-fields)))
                         :o1-origins-present-in-arm-one
                         (vec (sort (filter (set arm-one-fields) o1-origin-fields))))
   :clauses (sorted-map :admitting-nodes-reading admitting-clauses
                        :admitting-selected-reading admitting-selected-clauses
                        :added-over-slice-6 clauses-added-over-slice-6
                        :readings-differ-only-in readings-differ-only-in
                        :slice-6-nodes-reading slice6-nodes-clauses)
   :declarations declarations
   :edges (sorted-map :over-nodes ff-over-nodes
                      :over-selected ff-over-selected
                      :recomputed-from :authored-edges
                      :record-over-nodes (mapv vec (:over-nodes (:fast-forward record))))
   :organise-type (sorted-map :expected-arm expected-arm-type
                              :in-arm arm-type
                              :in-holes organise-type-in-holes)
   :problems problems
   :vertex-count (count vertices)
   :vertices vertex-facts
   :witness (sorted-map :admitted-lookup admitted-lookup :shape witness-shape)))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-admitting: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-admitting: FAIL" (pr-str problems))) (System/exit 1))
  (println (str "f12-admitting: PASS -- codomain-only signature edit, three carrier field "
                "lists, both O3 readings against slice 6's, the recorded-lookup witness shape, "
                "and both recomputed edge relations agree with their sources")))
