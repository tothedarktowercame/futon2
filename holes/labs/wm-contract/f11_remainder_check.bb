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
(def default-conformance (io/file mathlib-root "DarkTower/WarMachine/F11Conformance.lean"))
(def default-receipt (io/file mathlib-root "DarkTower/WarMachine/F11ReceiptCarrier.lean"))
(def default-f4 (io/file mathlib-root "DarkTower/WarMachine/F11F4Reading.lean"))
(def default-f3 (io/file mathlib-root "DarkTower/WarMachine/F11NonSelfCertifying.lean"))
(def default-discharge (io/file mathlib-root "DarkTower/WarMachine/F11DischargeArm.lean"))
(def default-reconciliation (io/file lab-dir "runs/F11-find/02-reconciliation.edn"))
(def default-f3-run (io/file lab-dir "runs/F11-find/13-non-self-certifying.edn"))
(def default-out (io/file lab-dir "runs/F11-find/14-remainder.edn"))

(defn env-file [key fallback] (io/file (or (System/getenv key) (str fallback))))

(def obligation-substrings
  (sorted-map
   :f2-falsifier-reconciled "F2 falsifier reconciled first (documented against the current library state)"
   :conformant-implementation "a conformant implementation at the s3e interface"
   :f1-stated-in-lean "laws stated in Lean"
   :f2-content-stated-in-lean "laws stated in Lean"
   :f3-citation-stated-in-lean "laws stated in Lean"
   :f4-stated-in-lean "laws stated in Lean"
   :witnessed-on-real-find-over-committed-library "each witnessed on a real find over the committed library"
   :sorry "discharge or amend the sorry at Holes.lean:264"))

;; Each quoted fragment is checked against the live question before it counts.
;; An empty vector is deliberate evidence that no registered choice gates it.
(def gating-table
  (sorted-map
   :f2-falsifier-reconciled []
   :conformant-implementation []
   :f1-stated-in-lean []
   :f2-content-stated-in-lean
   [[:find-f2-receipt-carrier "WHAT DOES A `Receipt` HOLD, AND THEREFORE WHAT CAN F2 STATE?"]]
   :f3-citation-stated-in-lean []
   :f4-stated-in-lean
   [[:find-f4-reading "WHICH OF THREE READINGS IS F4?"]]
   :witnessed-on-real-find-over-committed-library []
   :sorry [[:find-sorry "WHAT IS DONE WITH THE SORRY"]]))

(defn find-offset! [haystack needle context]
  (let [n (.indexOf ^String haystack ^String needle)]
    (when (neg? n)
      (throw (ex-info (str "substring not found for " context ": " (pr-str needle)) {})))
    n))

(defn line-hits [path pattern]
  (keep-indexed (fn [i line] (when (re-find pattern line)
                              {:line (inc i) :source-text line}))
                (str/split-lines (slurp path))))

(defn maybe-one-line
  "Absence is a measurement, while ambiguity is a hard failure."
  [path pattern label]
  (let [hits (vec (line-hits path pattern))]
    (when (< 1 (count hits))
      (throw (ex-info (str "expected at most one " label ", found " (count hits)) {})))
    (when-let [hit (first hits)]
      (assoc (into (sorted-map) hit)
             :file-line (str (.getCanonicalPath (io/file path)) ":" (:line hit))))))

(defn one-line! [path pattern label]
  (or (maybe-one-line path pattern label)
      (throw (ex-info (str "expected one " label ", found 0") {}))))

(defn edn-key-line
  "Locate a key in a persisted EDN record by NAME, so the pointer follows the key
  when the record is rewritten. `:not-found` is a measurement, not a failure."
  [path k]
  (let [lines (str/split-lines (slurp path))
        needle (re-pattern (str "^\\s*" (java.util.regex.Pattern/quote (str k)) "(\\s|$)"))
        idx (first (keep-indexed (fn [i l] (when (re-find needle l) i)) lines))]
    (if idx (str (.getCanonicalPath (io/file path)) ":" (inc idx)) :not-found)))

(defn receipt-fields
  "The fields of `structure Receipt`, read from the struct block by NAME. Slice 4
  measured that a `Prop` field carries no WHICH (`assertionFieldsDoNotFixAttribution`,
  F11ReceiptCarrier.lean:87), so the type is recorded alongside the name and the
  two obligations below are decided over the NON-`Prop` fields."
  [holes-path]
  (let [lines (str/split-lines (slurp holes-path))
        start (first (keep-indexed (fn [i l] (when (re-find #"^structure Receipt\b" l) i)) lines))]
    (when-not start
      (throw (ex-info "structure Receipt not found in Holes.lean" {})))
    (vec (for [[i l] (map-indexed vector
                                  (take-while #(re-find #"^\s+\S" %) (drop (inc start) lines)))
               :let [[_ nm ty] (re-find #"^\s+(\S+)\s*:\s*(.+?)\s*$" l)]
               :when nm]
           (sorted-map :field nm :type ty
                       :file-line (str (.getCanonicalPath (io/file holes-path)) ":"
                                       (+ start i 2)))))))

(defn data-fields [fields] (vec (remove #(= "Prop" (:type %)) fields)))

(def f2-ask-patterns
  (sorted-map :acknowledged-clause #"(?i)clause|acknowledg"
              :as-of #"(?i)as-?of"
              :retrieval-route #"(?i)route"))

(def f3-citation-pattern #"(?i)cite|citation|warrant")

(def f4-reading-specs
  [[:reading-a "FindFalsifiable" :conformance]
   [:reading-b "FindExcludesRecordedZeroMass" :discharge]
   [:reading-c "FindRespectsZeroMass" :f4]])

(defn adjudicate
  "A candidate refuter is CONFIRMED only when its obligation is measured unmet AND
  its gating vector is empty. Derived, so the key the dispatch added in order to be
  able to come out the other way can come out the other way."
  [finding gate]
  (cond (:satisfied-at-head? finding) :refuted-obligation-met-at-head
        (seq gate) :refuted-obligation-is-gated
        :else :confirmed-unmet-and-ungated))

(defn acceptance-measure [path]
  (let [w (edn/read-string (slurp path))
        rows (vec (filter #(= :F11 (:id %)) (:items w)))]
    (when-not (= 1 (count rows))
      (throw (ex-info (str "expected one F11 row, found " (count rows)) {})))
    (let [acceptance (:acceptance (first rows))
          clauses (mapv str/trim (str/split acceptance #";"))]
      (when-not (= 3 (count clauses))
        (throw (ex-info (str "expected 3 clauses, found " (count clauses)) {})))
      (sorted-map
       :acceptance acceptance
       :clause-count 3
       :clauses clauses
       :obligation-locations
       (into (sorted-map)
             (for [[k needle] obligation-substrings]
               [k (sorted-map :offset (find-offset! acceptance needle k)
                              :verbatim-substring needle)]))))))

(defn ptr [hit] (or (:file-line hit) :not-found))

(defn source-findings [holes conformance receipt f4 f3 discharge reconciliation f3-run]
  (let [rec (edn/read-string (slurp reconciliation))
        find-def (one-line! holes #"^def find\s" "def find")
        conformant (maybe-one-line conformance #"^structure ConformantFind\s" "ConformantFind")
        replay (maybe-one-line conformance #"^def findSnatchReplay\s" "findSnatchReplay")
        replay-ok (maybe-one-line conformance #"^theorem findSnatchReplayConformant\s" "findSnatchReplayConformant")
        f2-erasure (maybe-one-line receipt #"^theorem findRErasuresAreEqual\s" "findRErasuresAreEqual")
        f3-erasure (maybe-one-line f3 #"^theorem findCErasuresAreEqual\s" "findCErasuresAreEqual")
        reading-files {:conformance conformance :discharge discharge :f4 f4}
        readings (vec (for [[label decl file-key] f4-reading-specs
                            :let [hit (maybe-one-line (reading-files file-key)
                                                      (re-pattern (str "^def " decl "\\b"))
                                                      decl)]]
                        (sorted-map :declaration decl :file-line (ptr hit)
                                    :present? (some? hit) :reading label)))
        readings-present (filterv :present? readings)
        fields (receipt-fields holes)
        carried (data-fields fields)
        f2-asks (into (sorted-map)
                      (for [[ask pattern] f2-ask-patterns]
                        [ask (if-let [hit (first (filter #(re-find pattern (:field %)) carried))]
                               (:file-line hit) :not-found)]))
        f3-citation (if-let [hit (first (filter #(re-find f3-citation-pattern (:field %)) carried))]
                      (:file-line hit) :not-found)
        body-is-sorry? (boolean (re-find #":=\s*sorry\s*$" (:source-text find-def)))
        live-count (:repository-count-live rec)
        pin-count (:repository-count-pin rec)
        receipt-differences (:receipts-differing rec)
        live-ptr (edn-key-line reconciliation :repository-count-live)
        pin-ptr (edn-key-line reconciliation :repository-count-pin)
        differing-ptr (edn-key-line reconciliation :receipts-differing)
        library-satisfied? (and (= live-count pin-count) (zero? receipt-differences))]
    (sorted-map
     :f2-falsifier-reconciled
     (sorted-map :pointer (edn-key-line reconciliation :difference-is-line-coordinates-only?)
                 :satisfied-at-head? (and (:difference-is-line-coordinates-only? rec)
                                          (zero? (:drift-mismatch-count-live rec))))
     :conformant-implementation
     (sorted-map :pointers [(ptr conformant) (ptr replay) (ptr replay-ok)]
                 :satisfied-at-head? (every? some? [conformant replay replay-ok]))
     :f1-stated-in-lean
     (sorted-map :pointer (ptr conformant) :satisfied-at-head? (some? conformant))
     ;; F2's three asks and F3's citation are decided from the CARRIER, because
     ;; that is where slice 4 and slice 7 measured them to die: a `Prop` field
     ;; asserts that something is so and carries no WHICH, so only a non-`Prop`
     ;; field can state either ask.
     :f2-content-stated-in-lean
     (sorted-map :blindness-witness (ptr f2-erasure)
                 :f2-asks-carried-as-data f2-asks
                 :pointer (ptr conformant)
                 :receipt-data-fields (mapv :field carried)
                 :satisfied-at-head? (every? #(not= :not-found %) (vals f2-asks)))
     :f3-citation-stated-in-lean
     (sorted-map :blindness-witness (ptr f3-erasure)
                 :citation-carried-as-data f3-citation
                 :not-established-pointer (edn-key-line f3-run :not-established)
                 :receipt-data-fields (mapv :field carried)
                 :receipt-fields (mapv :field fields)
                 :registry-choice-for-citation-field :not-found
                 :satisfied-at-head? (not= :not-found f3-citation))
     ;; F4 is unmet not because no reading is stated but because THREE are and no
     ;; two are equivalent (slice 3 and slice 5); it is met when one stands alone.
     :f4-stated-in-lean
     (sorted-map :pointer (ptr (maybe-one-line f4 #"^def FindRespectsZeroMass\s" "FindRespectsZeroMass"))
                 :readings readings
                 :readings-present-count (count readings-present)
                 :satisfied-at-head? (= 1 (count readings-present)))
     :witnessed-on-real-find-over-committed-library
     (sorted-map :live-repository-count live-count
                 :pin-repository-count pin-count
                 :pointers [live-ptr pin-ptr differing-ptr]
                 :receipt-differences receipt-differences
                 :satisfied-at-head? library-satisfied?)
     :sorry
     (sorted-map :body-is-still-sorry? body-is-sorry?
                 :file-line (:file-line find-def)
                 :satisfied-at-head? (not body-is-sorry?))
     :candidate-refuters
     (sorted-map
      :f3-citation-blindness
      (sorted-map :citation-carried-as-data f3-citation
                  :pointers [(ptr f3-erasure) (edn-key-line f3-run :not-established)]
                  :priced-amendment-includes-citation-field? false
                  :registered-choice :not-found)
      :pinned-versus-live-library
      (sorted-map :live-repository-count live-count
                  :pin-repository-count pin-count
                  :pointers [live-ptr pin-ptr differing-ptr]
                  :receipt-differences receipt-differences)))))

(defn choice-measure [path table]
  (let [registry (edn/read-string (slurp path))
        choices (into (sorted-map) (filter (fn [[_ v]] (= :F11 (:row v)))) (:choices registry))
        expected #{:find-sorry :find-f2-receipt-carrier :find-f4-reading}]
    (when-not (= expected (set (keys choices)))
      (throw (ex-info (str "expected F11 choices " expected ", found " (set (keys choices))) {})))
    (into (sorted-map)
          (for [[obligation mappings] table]
            [obligation
             (mapv (fn [[choice needle]]
                     (let [entry (get choices choice)]
                       (when-not (and (= :observed-not-decided (:status entry))
                                      (not (contains? entry :ruling)))
                         (throw (ex-info (str "choice is not undecided and unruled: " choice) {})))
                       (sorted-map :choice choice
                                   :question-offset (find-offset! (:question entry) needle
                                                                 (str obligation "/" choice))
                                   :question-substring needle
                                   :status (:status entry)
                                   :ruling-key-present? (contains? entry :ruling))))
                   mappings)]))))

(defn verdict [findings gates]
  (let [obligations (dissoc findings :candidate-refuters)
        missing (vec (for [[k v] obligations
                           :when (and (not (:satisfied-at-head? v)) (empty? (get gates k)))] k))]
    (sorted-map :obligations-not-satisfied-and-not-gated missing
                :remainder-fully-gated? (empty? missing))))

(defn core [worklist aif holes conformance receipt f4 f3 discharge reconciliation f3-run table]
  (let [measured (source-findings holes conformance receipt f4 f3 discharge reconciliation f3-run)
        gates (choice-measure aif table)
        findings (-> measured
                     (assoc-in [:candidate-refuters :f3-citation-blindness :adjudication]
                               (adjudicate (:f3-citation-stated-in-lean measured)
                                           (get gates :f3-citation-stated-in-lean)))
                     (assoc-in [:candidate-refuters :pinned-versus-live-library :adjudication]
                               (adjudicate (:witnessed-on-real-find-over-committed-library measured)
                                           (get gates :witnessed-on-real-find-over-committed-library))))]
    (merge (sorted-map :acceptance (acceptance-measure worklist)
                       :gating gates :obligations findings :schema :f11/remainder-v1)
           (verdict findings gates))))

(defn temp-dir [] (.toFile (Files/createTempDirectory "f11-remainder-" (make-array FileAttribute 0))))
(defn write-edn! [f x] (spit f (with-out-str (pp/pprint x))))
(defn control [name planted before thunk]
  (try (sorted-map :after (thunk) :before before :control name :plant-verified? planted)
       (catch Exception e (sorted-map :after-hard-failure (.getMessage e) :before before
                                      :control name :plant-verified? planted))))

(defn controls [worklist aif holes conformance receipt f4 f3 discharge reconciliation f3-run baseline]
  (let [tmp (temp-dir)
        run #(core %1 %2 %3 %4 receipt %5 f3 %6 %7 f3-run %8)
        verdict-keys [:obligations-not-satisfied-and-not-gated :remainder-fully-gated?]
        no-sorry-gate (dissoc gating-table :sorry)
        aif-copy (io/file tmp "aif.edn")
        aif-data (edn/read-string (slurp aif))
        original-question (get-in aif-data [:choices :find-sorry :question])
        changed-question (str/replace-first original-question "WHAT IS DONE WITH THE SORRY"
                                            "WHAT BECOMES OF THE REFUSAL")
        _ (write-edn! aif-copy (assoc-in aif-data [:choices :find-sorry :question]
                                        changed-question))
        rec-copy (io/file tmp "reconciliation.edn")
        rec-data (assoc (edn/read-string (slurp reconciliation)) :repository-count-live 18 :receipts-differing 0)
        _ (write-edn! rec-copy rec-data)
        holes-copy (io/file tmp "Holes.lean")
        holes-text (str/replace-first (slurp holes) #"(def find[^\n]*:=) sorry" "$1 CONTROL_BODY")
        _ (spit holes-copy holes-text)
        cite-copy (io/file tmp "Holes-cited.lean")
        cite-text (str/replace-first (slurp holes) #"(?m)^(structure Receipt\b[^\n]*\n)"
                                     "$1  citedPatternText : String\n")
        _ (spit cite-copy cite-text)
        conformance-copy (io/file tmp "F11Conformance.lean")
        _ (spit conformance-copy (str/replace-first (slurp conformance) #"(?m)^def FindFalsifiable\b"
                                                    "def ControlRemovedReadingA"))
        discharge-copy (io/file tmp "F11DischargeArm.lean")
        _ (spit discharge-copy (str/replace-first (slurp discharge) #"(?m)^def FindExcludesRecordedZeroMass\b"
                                                  "def ControlRemovedReadingB"))
        work-copy (io/file tmp "worklist.edn")
        w (edn/read-string (slurp worklist))
        idx (first (keep-indexed #(when (= :F11 (:id %2)) %1) (:items w)))
        w2 (update-in w [:items idx :acceptance] #(str/replace-first % "laws stated in Lean" "laws; stated in Lean"))
        _ (write-edn! work-copy w2)
        invented-fragment "citation field is not present"
        no-f3-gate (assoc gating-table :f3-citation-stated-in-lean
                          [[:find-sorry invented-fragment]])
        ruled-copy (io/file tmp "ruled-aif.edn")
        ar (edn/read-string (slurp aif))
        _ (write-edn! ruled-copy (assoc-in ar [:choices :find-sorry :ruling] :control))
        library-keys [:satisfied-at-head? :live-repository-count :pin-repository-count :receipt-differences]
        library-slice (fn [result]
                        (sorted-map
                         :adjudication (get-in result [:obligations :candidate-refuters
                                                       :pinned-versus-live-library :adjudication])
                         :obligation (select-keys
                                      (get-in result [:obligations :witnessed-on-real-find-over-committed-library])
                                      library-keys)))
        citation-slice (fn [result]
                         (sorted-map
                          :adjudication (get-in result [:obligations :candidate-refuters
                                                        :f3-citation-blindness :adjudication])
                          :citation-carried-as-data
                          (if (= :not-found (get-in result [:obligations :f3-citation-stated-in-lean
                                                            :citation-carried-as-data]))
                            :not-found :carried)
                          :satisfied-at-head? (get-in result [:obligations :f3-citation-stated-in-lean
                                                              :satisfied-at-head?])
                          :vector (get result :obligations-not-satisfied-and-not-gated)))
        f4-slice (fn [result]
                   (select-keys (get-in result [:obligations :f4-stated-in-lean])
                                [:readings-present-count :satisfied-at-head?]))
        sorry-slice (fn [result]
                      (select-keys (get-in result [:obligations :sorry])
                                   [:body-is-still-sorry? :satisfied-at-head?]))]
    [(control :delete-sorry-gate (not (contains? no-sorry-gate :sorry))
              (select-keys baseline verdict-keys)
              #(select-keys (run worklist aif holes conformance f4 discharge reconciliation no-sorry-gate)
                            verdict-keys))
     (control :perturb-gating-fragment
              (and (str/includes? changed-question "WHAT BECOMES OF THE REFUSAL")
                   (not (str/includes? changed-question "WHAT IS DONE WITH THE SORRY")))
              :measurement-succeeds
              #(run worklist aif-copy holes conformance f4 discharge reconciliation gating-table))
     (control :make-live-fixture-match-pin
              (and (= 18 (:repository-count-live (edn/read-string (slurp rec-copy))))
                   (zero? (:receipts-differing (edn/read-string (slurp rec-copy)))))
              (library-slice baseline)
              #(library-slice (run worklist aif holes conformance f4 discharge rec-copy gating-table)))
     (control :replace-find-body
              (and (str/includes? (slurp holes-copy) "CONTROL_BODY")
                   (not (re-find #"def find[^\n]*:= sorry" (slurp holes-copy))))
              (sorry-slice baseline)
              #(sorry-slice (run worklist aif holes-copy conformance f4 discharge reconciliation gating-table)))
     ;; R1 repair, shown separating: the citation obligation is decided from the
     ;; carrier, so giving `Receipt` a citation DATUM must move it and its refuter.
     (control :add-citation-field-to-receipt
              (and (str/includes? (slurp cite-copy) "citedPatternText : String")
                   (not (str/includes? (slurp holes) "citedPatternText")))
              (citation-slice baseline)
              #(citation-slice (run worklist aif cite-copy conformance f4 discharge reconciliation gating-table)))
     ;; R1 repair, shown separating: F4 is unmet because THREE inequivalent
     ;; readings stand; leaving one must make it satisfied.
     (control :collapse-f4-readings
              (and (str/includes? (slurp conformance-copy) "def ControlRemovedReadingA")
                   (not (re-find #"(?m)^def FindFalsifiable\b" (slurp conformance-copy)))
                   (str/includes? (slurp discharge-copy) "def ControlRemovedReadingB")
                   (not (re-find #"(?m)^def FindExcludesRecordedZeroMass\b" (slurp discharge-copy))))
              (f4-slice baseline)
              #(f4-slice (run worklist aif holes conformance-copy f4 discharge-copy reconciliation gating-table)))
     (control :change-acceptance-clause-count
              (let [planted-acceptance (get-in (edn/read-string (slurp work-copy))
                                               [:items idx :acceptance])]
                (and (str/includes? planted-acceptance "laws; stated in Lean")
                     (not (str/includes? planted-acceptance "laws stated in Lean"))))
              :three-clauses
              #(run work-copy aif holes conformance f4 discharge reconciliation gating-table))
     (control :invent-f3-gate
              (and (= [[:find-sorry invented-fragment]] (get no-f3-gate :f3-citation-stated-in-lean))
                   (not (str/includes? original-question invented-fragment)))
              :measurement-succeeds
              #(run worklist aif holes conformance f4 discharge reconciliation no-f3-gate))
     (control :rule-a-gating-choice
              (and (= :control (get-in (edn/read-string (slurp ruled-copy))
                                       [:choices :find-sorry :ruling]))
                   (not (contains? (get-in (edn/read-string (slurp aif))
                                           [:choices :find-sorry]) :ruling)))
              :measurement-succeeds
              #(run worklist ruled-copy holes conformance f4 discharge reconciliation gating-table))]))

(defn measure []
  (let [worklist (env-file "F11_WORKLIST" default-worklist)
        aif (env-file "F11_AIF_EQ" default-aif)
        holes (env-file "F11_HOLES" default-holes)
        conformance (env-file "F11_CONFORMANCE" default-conformance)
        receipt (env-file "F11_RECEIPT" default-receipt)
        f4 (env-file "F11_F4" default-f4)
        f3 (env-file "F11_F3" default-f3)
        reconciliation (env-file "F11_RECONCILIATION" default-reconciliation)
        discharge (env-file "F11_DISCHARGE" default-discharge)
        f3-run (env-file "F11_F3_RUN" default-f3-run)
        base (core worklist aif holes conformance receipt f4 f3 discharge reconciliation
                   f3-run gating-table)]
    (assoc base :controls (controls worklist aif holes conformance receipt f4 f3 discharge
                                   reconciliation f3-run base))))

(try
  (let [out (env-file "F11_OUT" default-out)]
    (.mkdirs (.getParentFile out))
    (write-edn! out (measure))
    (println out))
  (catch Exception e
    (binding [*out* *err*] (println "f11_remainder_check:" (.getMessage e)))
    (System/exit 1)))
