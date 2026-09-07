#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])
(import '[java.nio.file Files]
        '[java.nio.file.attribute FileAttribute])

(def lab-dir (.getParentFile (.getAbsoluteFile (io/file *file*))))
(def repo-root (.getCanonicalFile (io/file lab-dir "../../..")))
(def mathlib-root (.getCanonicalFile (io/file repo-root "../mathlib4")))
(def default-worklist (io/file lab-dir "worklist.edn"))
(def default-aif (io/file lab-dir "aif-equations.edn"))
(def default-holes (io/file mathlib-root "DarkTower/WarMachine/Holes.lean"))
(def default-conformance (io/file mathlib-root "DarkTower/WarMachine/F12Conformance.lean"))
(def default-admitting (io/file mathlib-root "DarkTower/WarMachine/F12AdmittingArm.lean"))
(def default-discharge (io/file mathlib-root "DarkTower/WarMachine/F12DischargeArm.lean"))
(def default-zaif (io/file lab-dir "runs/F12-organise/01-zaif-transcription.edn"))
(def default-out (io/file lab-dir "runs/F12-organise/15-remainder.edn"))

(defn env-file [key fallback]
  (io/file (or (System/getenv key) (str fallback))))

(def obligation-substrings
  (sorted-map
   :conformant-implementation "A conformant implementation at the s3e interface"
   :laws-stated-against-cascade-carrier "the four O-laws stated in Lean against the existing Cascade carrier"
   :laws-witnessed-on-real-cascade "each law witnessed on a REAL constructed cascade"
   :sorry "discharge or amend the sorry at Holes.lean:861"
   :swappability "swappability clause honoured"))

;; Each quoted fragment is checked against the live question before it can
;; count as a gate. The table is evidence-bearing input to the computation.
(def gating-table
  (sorted-map
   :conformant-implementation
   [[:organise-sorry "The declaration is a total function"]]
   :laws-stated-against-cascade-carrier
   [[:organise-carrier "WHICH CARRIER are O1-O4 stated of?"]]
   :laws-witnessed-on-real-cascade
   [[:organise-carrier "The four laws as proved today are proved of a CascadeDiff VALUE instead"]]
   :sorry
   [[:organise-sorry "WHAT IS DONE WITH THE SORRY"]]
   :swappability
   [[:organise-sorry "does not select one canonical implementation"]]))

(defn find-offset! [haystack needle context]
  (let [n (.indexOf ^String haystack ^String needle)]
    (when (neg? n)
      (throw (ex-info (str "substring not found for " context ": " (pr-str needle))
                      {:context context :substring needle})))
    n))

(defn line-hits [path pattern]
  (keep-indexed (fn [i line]
                  (when (re-find pattern line)
                    (sorted-map :line (inc i) :source-text line)))
                (str/split-lines (slurp path))))

(defn maybe-one-line
  "Nil when the declaration is absent; throws when it is ambiguous. The absent
  case is a MEASUREMENT (a law with no witness), not a script failure."
  [path pattern label]
  (let [hits (vec (line-hits path pattern))]
    (when (< 1 (count hits))
      (throw (ex-info (str "expected at most one " label ", found " (count hits)) {})))
    (when-let [hit (first hits)]
      (assoc hit :file-line (str (.getCanonicalPath (io/file path)) ":" (:line hit))))))

(defn one-line! [path pattern label]
  (or (maybe-one-line path pattern label)
      (throw (ex-info (str "expected one " label ", found 0") {}))))

(defn acceptance-measure [worklist-path]
  (let [w (edn/read-string (slurp worklist-path))
        rows (vec (filter #(= :F12 (:id %)) (:items w)))]
    (when-not (= 1 (count rows))
      (throw (ex-info (str "expected one F12 row, found " (count rows)) {})))
    (let [acceptance (:acceptance (first rows))
          clauses (mapv str/trim (str/split acceptance #";"))]
      (when-not (= 3 (count clauses))
        (throw (ex-info (str "expected 3 clauses, found " (count clauses))
                        {:count (count clauses)})))
      (let [gate-offset (.indexOf ^String acceptance "Gates:")
            obligation-text (if (neg? gate-offset) acceptance
                                (subs acceptance 0 gate-offset))]
        (sorted-map
         :acceptance acceptance
         :clause-count (count clauses)
         :clauses (mapv #(str/trim (str/replace % #"\s*Gates:.*$" "")) clauses)
         :gate-tail-offset (if (neg? gate-offset) :not-found gate-offset)
         :obligation-locations
         (into (sorted-map)
               (for [[k needle] obligation-substrings]
                 [k (sorted-map :offset (find-offset! obligation-text needle k)
                                :verbatim-substring needle)])))))))

(def law-specs
  [[:o1 "organiseO1NodesRecorded" "organiseO1NodesRecordedZaif"]
   [:o2 "organiseO2AuthoredReachability" "organiseO2AuthoredReachabilityZaif"]
   [:o3 "organiseO3FastForward" "organiseO3FastForwardZaif"]
   [:o4 "organiseO4PrecedenceGovernance" "organiseO4PrecedenceGovernanceZaif"]])

(def real-cascade-fixture-kinds
  "Fixture kinds that count as a REAL constructed cascade for the acceptance's
  \"F7 record or fresh construction from the library\" clause. The C59 fixture is
  hand-derived (`Holes.lean:871` doc comment) and is deliberately not in this set."
  #{:zaif-1239-pattern-transcription})

(defn fixture-carrier [holes-path fixture]
  (let [row (one-line! holes-path
                       (re-pattern (str "^def " fixture "\\s*:\\s*([^ ]+)"))
                       (str "fixture " fixture))
        carrier (second (re-find (re-pattern (str "^def " fixture "\\s*:\\s*([^ ]+)"))
                                 (:source-text row)))]
    (sorted-map :carrier (keyword carrier)
                :declaration fixture
                :file-line (:file-line row))))

(defn law-row
  "The declaration's absence is recorded, not thrown: a law with no witness on
  this fixture is exactly what the real-cascade obligation has to be able to see."
  [holes-path law declaration fixture fixture-kind]
  (if-let [decl (maybe-one-line holes-path
                                (re-pattern (str "^def " declaration "(?:\\s|$)"))
                                declaration)]
    (let [carrier (fixture-carrier holes-path fixture)]
      (sorted-map :carrier (:carrier carrier)
                  :declaration declaration
                  :file-line (:file-line decl)
                  :fixture fixture
                  :fixture-file-line (:file-line carrier)
                  :fixture-kind fixture-kind
                  :law law))
    (sorted-map :declaration declaration
                :file-line :not-found
                :fixture fixture
                :fixture-kind :not-found
                :law law)))

(defn parse-int-after [text pattern label]
  (if-let [m (re-find pattern text)]
    (parse-long (second m))
    (throw (ex-info (str "not found: " label) {}))))

(defn zaif-cross-check [holes-path zaif-path]
  (let [source (slurp holes-path)
        artifact (edn/read-string (slurp zaif-path))
        selected (parse-int-after source #"private def zaifSelected[^\n]*n < (\d+)" "zaifSelected bound")
        admitted-from (parse-int-after source #"private def zaifAdmitted[^\n]*\{n \| (\d+) ≤" "zaifAdmitted lower bound")
        admitted-to (parse-int-after source #"private def zaifAdmitted[^\n]*n < (\d+)" "zaifAdmitted upper bound")
        nodes (parse-int-after source #"private def zaifNodes[^\n]*n < (\d+)" "zaifNodes bound")
        block (second (re-find #"(?s)private def zaifAuthored.*?\n(.*?)\n  \| _, _ => False" source))
        edge-count (count (re-seq #"\d+, \d+" block))
        lean-values (sorted-map :admitted (- admitted-to admitted-from)
                                :authored-edges edge-count
                                :nodes nodes
                                :selected selected)
        artifact-values (select-keys (:counts artifact)
                                     [:admitted :authored-edges :nodes :selected])]
    (sorted-map :agree? (= lean-values artifact-values)
                :artifact artifact-values
                :artifact-basis-patterns (get-in artifact [:as-of-recorded :patterns] :not-found)
                :lean-source lean-values)))

(defn source-findings [holes-path conformance-path admitting-path discharge-path zaif-path]
  (let [organise (one-line! holes-path #"^def organise\s" "def organise")
        body-sorry? (boolean (re-find #":=\s*sorry\s*$" (:source-text organise)))
        main-laws (mapv (fn [[law decl _]]
                          (law-row holes-path law decl "wmCascadeDiffFixture" :c59-hand-fixture))
                        law-specs)
        zaif-laws (mapv (fn [[law _ decl]]
                          (law-row holes-path law decl "wmZaifCascadeDiffFixture"
                                   :zaif-1239-pattern-transcription))
                        law-specs)
        ;; Which fixture kinds witness each law, read off the two rows rather
        ;; than assumed from the size of the law table.
        witnesses-by-law
        (into (sorted-map)
              (for [[law _ _] law-specs]
                (let [kinds (into (sorted-set)
                                  (comp (filter #(= law (:law %)))
                                        (map :fixture-kind)
                                        (remove #(= :not-found %)))
                                  (concat main-laws zaif-laws))]
                  [law (sorted-map
                        :real-cascade-witness?
                        (boolean (some real-cascade-fixture-kinds kinds))
                        :witnessing-fixture-kinds (vec kinds))])))
        conformance-predicate (one-line! conformance-path
                                         #"^structure ConformantOrganiseSelected\s"
                                         "ConformantOrganiseSelected")
        selected-witness (one-line! conformance-path #"^def organiseSelectedOnly\s"
                                    "organiseSelectedOnly")
        admitting-predicate (one-line! admitting-path
                                        #"^structure ConformantOrganiseAdmitting\s"
                                        "ConformantOrganiseAdmitting")
        admitting-witness (one-line! admitting-path #"^def organiseAdmitting\s"
                                      "organiseAdmitting")
        swap-selected (one-line! conformance-path #"^theorem organiseWitnessesDifferOnZaif\s"
                                 "organiseWitnessesDifferOnZaif")
        swap-nodes (one-line! discharge-path #"^theorem organiseDischargeNotUniqueNodes\s"
                              "organiseDischargeNotUniqueNodes")]
    (sorted-map
     :conformant-implementation
     (sorted-map :pointers [(:file-line conformance-predicate) (:file-line selected-witness)
                            (:file-line admitting-predicate) (:file-line admitting-witness)]
                 :satisfied-at-head? true)
     :laws-stated-against-cascade-carrier
     (sorted-map :laws main-laws
                 :satisfied-at-head? (every? #(= :Cascade (:carrier %)) main-laws))
     :laws-witnessed-on-real-cascade
     (sorted-map :c59-laws main-laws
                 :real-cascade-fixture-kinds (vec real-cascade-fixture-kinds)
                 ;; "each law witnessed on a REAL constructed cascade": every
                 ;; law in the table needs a witness on a fixture transcribed
                 ;; from a recorded cascade. The C59 fixture is hand-derived and
                 ;; does not discharge this, so a law witnessed only there fails.
                 :satisfied-at-head? (every? :real-cascade-witness?
                                             (vals witnesses-by-law))
                 :witnesses-by-law witnesses-by-law
                 :zaif-cross-check (zaif-cross-check holes-path zaif-path)
                 :zaif-laws zaif-laws)
     :sorry
     (sorted-map :acceptance-line-agrees? (= 861 (:line organise))
                 :body-is-still-sorry? body-sorry?
                 :file-line (:file-line organise)
                 :satisfied-at-head? (not body-sorry?))
     :swappability
     (sorted-map :pointers [(:file-line swap-selected) (:file-line swap-nodes)]
                 :satisfied-at-head? true))))

(defn choice-measure [aif-path table]
  (let [registry (edn/read-string (slurp aif-path))
        choices (into (sorted-map)
                      (filter (fn [[_ v]] (= :F12 (:row v))))
                      (:choices registry))]
    (when-not (= 6 (count choices))
      (throw (ex-info (str "expected 6 F12 choices, found " (count choices)) {})))
    (into (sorted-map)
          (for [[obligation mappings] table]
            [obligation
             (mapv (fn [[choice needle]]
                     (let [entry (get choices choice)]
                       (when-not entry
                         (throw (ex-info (str "F12 choice not found: " choice) {})))
                       (sorted-map :choice choice
                                   :question-offset
                                   (find-offset! (:question entry) needle
                                                 (str obligation "/" choice))
                                   :question-substring needle)))
                   mappings)]))))

(defn verdict [findings gates]
  (let [missing (vec (for [[k finding] findings
                           :when (and (not (:satisfied-at-head? finding))
                                      (empty? (get gates k)))]
                       k))]
    (sorted-map :obligations-not-satisfied-and-not-gated missing
                :remainder-fully-gated? (empty? missing))))

(defn measure-core [worklist aif holes conformance admitting discharge zaif table]
  (let [acceptance (acceptance-measure worklist)
        findings (source-findings holes conformance admitting discharge zaif)
        gates (choice-measure aif table)]
    (merge (sorted-map :acceptance acceptance
                       :gating gates
                       :obligations findings
                       :schema :f12/remainder-v1)
           (verdict findings gates))))

(defn temp-dir []
  (.toFile (Files/createTempDirectory "f12-remainder-" (make-array FileAttribute 0))))

(defn write-edn! [f x]
  (spit f (with-out-str (pp/pprint x))))

(defn control-result [control planted before thunk]
  (try
    (sorted-map :after (thunk) :before before :control control
                :plant-verified? planted)
    (catch Exception e
      (sorted-map :after-hard-failure (.getMessage e) :before before
                  :control control :plant-verified? planted))))

(defn controls [worklist aif holes conformance admitting discharge zaif baseline]
  (let [tmp (temp-dir)
        core (fn [w a h table]
               (measure-core w a h conformance admitting discharge zaif table))
        c1-table (dissoc gating-table :sorry)
        c1-planted (not (contains? c1-table :sorry))
        c2-aif (io/file tmp "aif.edn")
        c2-text (str/replace-first (slurp aif) "WHAT IS DONE WITH THE SORRY"
                                   "WHAT HAPPENS TO THE REFUSAL")
        _ (spit c2-aif c2-text)
        c2-planted (str/includes? (slurp c2-aif) "WHAT HAPPENS TO THE REFUSAL")
        c3-holes (io/file tmp "Holes-body.lean")
        c3-text (str/replace-first (slurp holes)
                                   #"(def organise[^\n]*:=) sorry"
                                   "$1 CONTROL_BODY")
        _ (spit c3-holes c3-text)
        c3-planted (boolean (re-find #"def organise[^\n]*:= CONTROL_BODY" (slurp c3-holes)))
        c4-worklist (io/file tmp "worklist.edn")
        w (edn/read-string (slurp worklist))
        idx (first (keep-indexed (fn [i row] (when (= :F12 (:id row)) i)) (:items w)))
        c4-w (update-in w [:items idx :acceptance]
                        #(str/replace-first % "swappability clause honoured"
                                            "swappability; clause honoured"))
        _ (write-edn! c4-worklist c4-w)
        c4-planted (str/includes? (:acceptance (nth (:items (edn/read-string (slurp c4-worklist))) idx))
                                  "swappability; clause")
        c5-holes (io/file tmp "Holes-carrier.lean")
        c5-text (str/replace-first (slurp holes)
                                   "def wmCascadeDiffFixture : CascadeDiff Nat Int"
                                   "def wmCascadeDiffFixture : Cascade Nat")
        _ (spit c5-holes c5-text)
        c5-planted (str/includes? (slurp c5-holes) "def wmCascadeDiffFixture : Cascade Nat")
        c6-holes (io/file tmp "Holes-zaif-o4.lean")
        _ (spit c6-holes (str (slurp holes)
                              "\ndef organiseO4PrecedenceGovernanceZaif : True := trivial\n"))
        c6-planted (boolean (re-find #"(?m)^def organiseO4PrecedenceGovernanceZaif"
                                     (slurp c6-holes)))
        c7-table (dissoc gating-table :laws-witnessed-on-real-cascade)
        c7-planted (not (contains? c7-table :laws-witnessed-on-real-cascade))
        witness-keys [:satisfied-at-head? :witnesses-by-law]
        verdict-keys [:obligations-not-satisfied-and-not-gated :remainder-fully-gated?]]
    [(control-result :gating-mapping-deleted c1-planted
                     (select-keys baseline [:remainder-fully-gated?
                                            :obligations-not-satisfied-and-not-gated])
                     #(select-keys (core worklist aif holes c1-table)
                                   [:remainder-fully-gated?
                                    :obligations-not-satisfied-and-not-gated]))
     (control-result :registry-substring-perturbed c2-planted :measurement-succeeds
                     #(core worklist c2-aif holes gating-table))
     (control-result :organise-body-replaced c3-planted
                     (select-keys (get-in baseline [:obligations :sorry])
                                  [:acceptance-line-agrees? :body-is-still-sorry?
                                   :satisfied-at-head?])
                     #(select-keys
                       (get-in (core worklist aif c3-holes gating-table)
                               [:obligations :sorry])
                       [:acceptance-line-agrees? :body-is-still-sorry?
                        :satisfied-at-head?]))
     (control-result :acceptance-clause-added c4-planted :three-clauses
                     #(core c4-worklist aif holes gating-table))
     (control-result :law-carrier-restated c5-planted
                     (mapv #(select-keys % [:carrier :declaration :law])
                           (get-in baseline
                                   [:obligations :laws-stated-against-cascade-carrier :laws]))
                     #(mapv
                       (fn [law] (select-keys law [:carrier :declaration :law]))
                       (get-in (core worklist aif c5-holes gating-table)
                               [:obligations :laws-stated-against-cascade-carrier :laws])))
     (control-result :zaif-o4-witness-planted c6-planted
                     (select-keys (get-in baseline
                                          [:obligations :laws-witnessed-on-real-cascade])
                                  witness-keys)
                     #(select-keys (get-in (core worklist aif c6-holes gating-table)
                                           [:obligations :laws-witnessed-on-real-cascade])
                                   witness-keys))
     (control-result :real-cascade-gating-mapping-deleted c7-planted
                     (select-keys baseline verdict-keys)
                     #(select-keys (core worklist aif holes c7-table) verdict-keys))]))

(defn measure []
  (let [worklist (env-file "F12_WORKLIST" default-worklist)
        aif (env-file "F12_AIF_EQ" default-aif)
        holes (env-file "F12_HOLES" default-holes)
        conformance (env-file "F12_CONFORMANCE" default-conformance)
        admitting (env-file "F12_ADMITTING" default-admitting)
        discharge (env-file "F12_DISCHARGE" default-discharge)
        zaif (env-file "F12_ZAIF" default-zaif)
        base (measure-core worklist aif holes conformance admitting discharge zaif gating-table)]
    (assoc base :controls
           (controls worklist aif holes conformance admitting discharge zaif base))))

(try
  (let [out (env-file "F12_OUT" default-out)]
    (.mkdirs (.getParentFile out))
    (write-edn! out (measure))
    (println (str out)))
  (catch Exception e
    (binding [*out* *err*]
      (println "f12_remainder_check:" (.getMessage e)))
    (System/exit 1)))
