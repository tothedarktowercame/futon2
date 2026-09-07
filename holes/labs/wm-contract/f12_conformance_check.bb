#!/usr/bin/env bb
;; f12_conformance_check.bb -- `:F12` slice 6.  CHECKS the conformance file's
;; three load-bearing premises against sources it does not itself write.
;;
;; WHY A CHECK AND NOT A READ.  Slice 6 states the O-laws of the FUNCTION
;; `organise` rather than of a cascade value, and every one of its results is a
;; result ABOUT THE s3e SIGNATURE.  Three things can therefore go silently
;; wrong in a way `lake build` cannot see, because each of them makes the file
;; elaborate perfectly while being about something else:
;;
;;   1. `OrganiseType` could drift from the type `organise` actually has at
;;      `Holes.lean`.  Then every theorem is a theorem about a different
;;      interface and the slice's verdicts do not transfer.  Checked by reading
;;      BOTH types out of their own files and comparing them.
;;   2. The "O4 is not stateable here" non-statement is a claim about which
;;      FIELDS `Cascade` has.  A comment cannot be wrong in a way the build
;;      notices.  Checked by reading the field lists of `Cascade` and
;;      `CascadeDiff` and requiring the four O4 fields to be absent from the
;;      first and present in the second.
;;   3. The two conformance predicates are supposed to differ in exactly one
;;      place -- which set O3's `fastForward` quantifies over.  If they drifted
;;      anywhere else the comparison between them would not be a comparison of
;;      the two O3 readings.  Checked field by field.
;;
;; It also re-checks the zaif vertices the slice's two measurements name (26 for
;; the swappability witness, 18-19 for the O3 reading) against slice 2's
;; derivation, so no measurement can quietly be about a vertex that is not
;; where the record puts it.
;;
;; WHAT IT DOES NOT DO.  It takes no ruling on D1, on the O3 field question, or
;; on whether the sorry should be discharged.  What it writes is a record of
;; what elaborated and what the sources say.
;;
;; Determinism: every collection sorted before writing; no wall clock.  Two runs
;; over an unchanged tree are byte-identical.
;;
;; Negative controls point it at planted copies: F12_CONF, F12_HOLES, F12_RECORD,
;; F12_OUT.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def lab (str home "/code/futon2/holes/labs/wm-contract"))
(def conf-path (or (System/getenv "F12_CONF")
                   (str home "/code/mathlib4/DarkTower/WarMachine/F12Conformance.lean")))
(def holes-path (or (System/getenv "F12_HOLES")
                    (str home "/code/mathlib4/DarkTower/WarMachine/Holes.lean")))
(def record-path (or (System/getenv "F12_RECORD")
                     (str lab "/runs/F12-organise/01-zaif-transcription.edn")))
(def out-path (or (System/getenv "F12_OUT") (str lab "/runs/F12-organise/04-conformance.edn")))

(defn die [m data]
  (binding [*out* *err*] (println "f12-conformance:" m (pr-str data)))
  (System/exit 1))

(defn- read-source [path what]
  (if (.exists (io/file path))
    (slurp path)
    (die "file not found" {:what what :path path})))

(def src (read-source conf-path :conformance-file))
(def holes (read-source holes-path :holes))
(def record (edn/read-string (slurp record-path)))

(defn- squeeze
  "Whitespace-insensitive form of a Lean type, so a line break cannot make two
  identical types compare unequal."
  [s]
  (str/trim (str/replace s #"\s+" " ")))

;; --- 1. the signature -------------------------------------------------------

(def organise-type-in-holes
  (if-let [m (re-find #"def organise \{Policy P : Type\*\} :\s*([^:=]+):= sorry" holes)]
    (squeeze (second m))
    (die "Holes.lean does not declare organise in the expected shape" {:path holes-path})))

(def organise-type-in-slice
  (if-let [m (re-find #"abbrev OrganiseType \(Policy P : Type\*\) :=\s*\n?\s*([^\n]+)" src)]
    (squeeze (second m))
    (die "the conformance file states no OrganiseType abbrev" {:path conf-path})))

;; --- 2. the O4 non-statement ------------------------------------------------

(defn- fields-of [structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\([^)]*\\) where\n(.*?)\n\n"))
                      holes)]
    (vec (sort (map second (re-seq #"(?m)^  (\w+)\s*:" (second m)))))
    (die "Holes.lean states no fields for" {:structure structure-name})))

(def cascade-fields (fields-of "Cascade"))
(def cascade-diff-fields (fields-of "CascadeDiff"))
(def o4-fields ["actingOrderAfter" "actingOrderBefore" "precedenceAfter" "precedenceBefore"
                "scoreAfter" "scoreBefore"])

;; --- 3. the two conformance predicates --------------------------------------

(defn- law-lines
  "The three law fields of a conformance structure, as squeezed source lines."
  [structure-name]
  (if-let [m (re-find (re-pattern (str "(?s)structure " structure-name
                                       " \\{Policy P[^}]*\\}.*?where\n(.*?)\n\n"))
                      src)]
    (into (sorted-map)
          (for [[_ k body] (re-seq #"(?m)^  (o[1-4]) :((?:[^\n]|\n    )+)" (second m))]
            [(keyword k) (squeeze body)]))
    (die "the conformance file states no such conformance structure"
         {:structure structure-name})))

(def selected-laws (law-lines "ConformantOrganiseSelected"))
(def nodes-laws (law-lines "ConformantOrganiseNodes"))

;; --- 4. declarations the packet required ------------------------------------

(def required
  [[:signature "abbrev OrganiseType"]
   [:predicate-selected-reading "structure ConformantOrganiseSelected"]
   [:predicate-nodes-reading "structure ConformantOrganiseNodes"]
   [:lemma-reach-trans "reach_trans"]
   [:lemma-fastforward-acyclic "fastForward_acyclic"]
   [:witness-one "def organiseSelectedOnly"]
   [:witness-two "def organiseUpClosure"]
   [:witness-one-conformant "organiseSelectedOnlyConformant"]
   [:witness-two-conformant "organiseUpClosureConformant"]
   [:policy-inhabitant "trivialPolicyCascade"]
   [:swappability "organiseWitnessesDifferOnZaif"]
   [:o1-non-vacuity "organiseUpClosureAddsSomething"]
   [:measurement-admitted-indistinguishable "admittedIndistinguishableFromAdded"]
   [:measurement-selected-reading-blocks-edge "selectedReadingCannotYieldZaifEdge"]
   [:measurement-edge-returns-with-input "selectedOnlyYieldsZaifEdgeWhenAdmittedSupplied"]])

(def declarations
  ;; `(some? false)` is TRUE, so the obvious spelling records every declaration
  ;; as present whatever the source says; a slice-5 plant caught that.
  (into (sorted-map) (for [[k needle] required] [k (str/includes? src needle)])))

;; --- 5. the vertices the measurements name, from slice 2's derivation --------

(def authored (set (map vec (:authored-edges record))))
(def enc (:encoding record))
(def selected-below (:selected-below enc))
(def nodes-below (:nodes-below enc))

(def vertex-facts
  (sorted-map
   ;; the swappability witness: 26 is an authored target of a SELECTED vertex
   ;; and is not itself a node, so the up-closure adds it and `nodes := sel`
   ;; does not.
   :swappability-witness 26
   :swappability-source-edge (some #(when (= 26 (second %)) (vec %)) (sort authored))
   :swappability-source-selected? (boolean (some #(when (= 26 (second %))
                                                    (< (first %) selected-below))
                                                 authored))
   :swappability-witness-is-node? (< 26 nodes-below)
   ;; the O3 reading measurement: both endpoints of the one organised edge are
   ;; admitted, not selected.
   :organised-edge (first (:over-nodes (:fast-forward record)))
   :fast-forward-over-selected (:over-selected (:fast-forward record))
   :organised-edge-endpoints-selected?
   (mapv #(< % selected-below) (or (first (:over-nodes (:fast-forward record))) []))))

;; --- 6. the up-closure the Lean witness computes, recomputed from the record --
;;
;; `organiseUpClosureOmitsAdmittedWitness` (`F12Conformance.lean:216`) states in
;; Lean that ONE admitted vertex is out of the up-closure's reach.  The general
;; claim its docstring makes -- that the up-closure of the recorded selected set
;; adds exactly `{20, 21, 25, 26}` and none of the nine admitted nodes -- is a
;; claim about the RECORD, so it is recomputed here from the record's own edge
;; list rather than asserted in a comment.  If it failed, the Lean witness would
;; still be true and the docstring would be wrong, which is the case a build
;; cannot see.

(def up-closure
  (loop [seen (set (range selected-below))]
    (let [nxt (into seen (for [[u v] authored :when (seen u)] v))]
      (if (= nxt seen) seen (recur nxt)))))

(def admitted-set (set (range (:admitted-from enc) nodes-below)))

(def closure-facts
  (sorted-map
   :adds (vec (sort (remove #(< % selected-below) up-closure)))
   :admitted-reached (vec (sort (filter admitted-set up-closure)))
   :edges-from-selected-into-admitted
   (vec (sort (for [[u v] authored :when (and (< u selected-below) (admitted-set v))] [u v])))
   :up-closure (vec (sort up-closure))))

;; --- verdict ----------------------------------------------------------------

;; Scanned over CODE, not prose.  The first spelling of this check scanned the
;; whole file and failed on the word `admit` inside a docstring describing the
;; policy-grain `admit` edit that put the nine admitted nodes in the recorded
;; cascade -- a true sentence about the subject matter.  A checker that makes
;; accurate prose fail teaches the next slice to soften the prose, so it reads
;; the file with its `/-- -/` and `/-! -/` blocks removed.
(def code-only
  (-> src
      (str/replace #"(?s)/--.*?-/" " ")
      (str/replace #"(?s)/-!.*?-/" " ")))

(def sorry-free?
  (not (re-find #"(?m)^\s*sorry\b|[^A-Za-z]sorry\b|\badmit\b|\bsorryAx\b" code-only)))

(def imports-arms?
  (some? (re-find #"(?m)^import DarkTower\.WarMachine\.F12D1Arms\b" src)))

;; The slice reuses slice 5's zaif declarations rather than re-declaring them.
;; A re-declaration would reopen exactly the drift surface slice 5's checker
;; exists to close, one file further along.
(def redeclared
  (vec (sort (map second (re-seq #"(?m)^(?:private )?def (d1\w+)" src)))))

(def problems
  (cond-> []
    (not= organise-type-in-slice organise-type-in-holes)
    (conj [:signature-drift organise-type-in-slice organise-type-in-holes])

    (seq (filter (set cascade-fields) o4-fields))
    (conj (into [:cascade-has-an-o4-field] (sort (filter (set cascade-fields) o4-fields))))

    (seq (remove (set cascade-diff-fields) o4-fields))
    (conj (into [:cascadediff-lacks-an-o4-field] (sort (remove (set cascade-diff-fields) o4-fields))))

    (not= (:o1 selected-laws) (:o1 nodes-laws))
    (conj [:conformance-predicates-differ-in-o1 (:o1 selected-laws) (:o1 nodes-laws)])

    (not= (:o2 selected-laws) (:o2 nodes-laws))
    (conj [:conformance-predicates-differ-in-o2 (:o2 selected-laws) (:o2 nodes-laws)])

    (= (:o3 selected-laws) (:o3 nodes-laws))
    (conj [:conformance-predicates-agree-in-o3 (:o3 selected-laws)])

    (not (str/includes? (:o3 selected-laws) "fastForward sel"))
    (conj [:selected-reading-does-not-quantify-over-sel (:o3 selected-laws)])

    (not (re-find #"fastForward \(f t sel repo\)\.nodes" (:o3 nodes-laws)))
    (conj [:nodes-reading-does-not-quantify-over-nodes (:o3 nodes-laws)])

    (not= [6 26] (:swappability-source-edge vertex-facts))
    (conj [:swappability-witness-not-authored-from-6 (:swappability-source-edge vertex-facts)])

    (not (:swappability-source-selected? vertex-facts))
    (conj [:swappability-witness-not-reachable-from-a-selected-vertex])

    (:swappability-witness-is-node? vertex-facts)
    (conj [:swappability-witness-is-already-a-node 26 nodes-below])

    (not= [18 19] (:organised-edge vertex-facts))
    (conj [:organised-edge-moved (:organised-edge vertex-facts)])

    (seq (:fast-forward-over-selected (:fast-forward record)))
    (conj [:fast-forward-over-selected-not-empty (:over-selected (:fast-forward record))])

    (some true? (:organised-edge-endpoints-selected? vertex-facts))
    (conj [:organised-edge-endpoint-is-selected (:organised-edge-endpoints-selected? vertex-facts)])

    (seq (:admitted-reached closure-facts))
    (conj (into [:up-closure-reaches-an-admitted-node] (:admitted-reached closure-facts)))

    (not= [20 21 25 26] (:adds closure-facts))
    (conj [:up-closure-adds-moved (:adds closure-facts)])

    (not sorry-free?) (conj [:sorry-in-conformance-file])
    (not imports-arms?) (conj [:does-not-import-the-slice-5-arms])
    (seq redeclared) (conj (into [:redeclares-slice-5-zaif-data] redeclared))

    (seq (for [[k present?] declarations :when (not present?)] k))
    (conj (into [:missing-declarations]
                (for [[k present?] declarations :when (not present?)] k)))))

(def result
  (sorted-map
   :basis (sorted-map :conformance-file conf-path
                      :holes holes-path
                      :record record-path
                      :zaif-basis (get-in record [:basis :sha])
                      :run (get-in record [:basis :run]))
   :cascade-fields cascade-fields
   :cascade-diff-fields cascade-diff-fields
   :conformance-predicates (sorted-map :nodes-reading nodes-laws
                                       :selected-reading selected-laws)
   :up-closure-of-selected closure-facts
   :declarations declarations
   :o4-fields-absent-from-cascade (vec (sort (remove (set cascade-fields) o4-fields)))
   :organise-type (sorted-map :in-holes organise-type-in-holes
                              :in-slice organise-type-in-slice)
   :problems problems
   :vertices vertex-facts))

(io/make-parents out-path)
(spit out-path (with-out-str (pprint/pprint result)))
(println "f12-conformance: wrote" out-path)
(if (seq problems)
  (do (binding [*out* *err*] (println "f12-conformance: FAIL" (pr-str problems))) (System/exit 1))
  (println "f12-conformance: PASS -- signature, O4 field claim, both O3 readings, both zaif vertices and the up-closure agree with their sources"))
