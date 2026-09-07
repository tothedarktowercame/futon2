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

(defn source-findings [holes conformance receipt f4 f3 reconciliation f3-run]
  (let [rec (edn/read-string (slurp reconciliation))
        find-def (one-line! holes #"^def find\s" "def find")
        conformant (maybe-one-line conformance #"^structure ConformantFind\s" "ConformantFind")
        replay (maybe-one-line conformance #"^def findSnatchReplay\s" "findSnatchReplay")
        replay-ok (maybe-one-line conformance #"^theorem findSnatchReplayConformant\s" "findSnatchReplayConformant")
        f2-erasure (maybe-one-line receipt #"^theorem findRErasuresAreEqual\s" "findRErasuresAreEqual")
        f3-erasure (maybe-one-line f3 #"^theorem findCErasuresAreEqual\s" "findCErasuresAreEqual")
        f4-reading (maybe-one-line f4 #"^def FindRespectsZeroMass\s" "FindRespectsZeroMass")
        live-count (:repository-count-live rec)
        pin-count (:repository-count-pin rec)
        receipt-differences (:receipts-differing rec)]
    (sorted-map
     :f2-falsifier-reconciled
     (sorted-map :pointer (str (.getCanonicalPath reconciliation) ":3")
                 :satisfied-at-head? (and (:difference-is-line-coordinates-only? rec)
                                          (zero? (:drift-mismatch-count-live rec))))
     :conformant-implementation
     (sorted-map :pointers [(ptr conformant) (ptr replay) (ptr replay-ok)]
                 :satisfied-at-head? (every? some? [conformant replay replay-ok]))
     :f1-stated-in-lean
     (sorted-map :pointer (ptr conformant) :satisfied-at-head? (some? conformant))
     :f2-content-stated-in-lean
     (sorted-map :blindness-witness (ptr f2-erasure)
                 :pointer (ptr conformant) :satisfied-at-head? false)
     :f3-citation-stated-in-lean
     (sorted-map :blindness-witness (ptr f3-erasure)
                 :not-established-pointer (str (.getCanonicalPath f3-run) ":114")
                 :registry-choice-for-citation-field :not-found
                 :satisfied-at-head? false)
     :f4-stated-in-lean
     (sorted-map :pointer (ptr f4-reading) :satisfied-at-head? false)
     :witnessed-on-real-find-over-committed-library
     (sorted-map :live-repository-count live-count
                 :pin-repository-count pin-count
                 :receipt-differences receipt-differences
                 :pointer (str (.getCanonicalPath reconciliation) ":13")
                 :satisfied-at-head? (and (= live-count pin-count)
                                          (zero? receipt-differences)))
     :sorry
     (sorted-map :body-is-still-sorry? (boolean (re-find #":=\s*sorry\s*$" (:source-text find-def)))
                 :file-line (:file-line find-def)
                 :satisfied-at-head? false)
     :candidate-refuters
     (sorted-map
      :f3-citation-blindness
      (sorted-map :adjudication :confirmed-unmet-and-ungated
                  :pointers [(ptr f3-erasure)
                             (str (.getCanonicalPath f3-run) ":114")]
                  :priced-amendment-includes-citation-field? false
                  :registered-choice :not-found)
      :pinned-versus-live-library
      (sorted-map :adjudication :confirmed-unmet-and-ungated
                  :live-repository-count live-count :pin-repository-count pin-count
                  :receipt-differences receipt-differences
                  :pointer (str (.getCanonicalPath reconciliation) ":13"))))))

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

(defn core [worklist aif holes conformance receipt f4 f3 reconciliation f3-run table]
  (let [findings (source-findings holes conformance receipt f4 f3 reconciliation f3-run)
        gates (choice-measure aif table)]
    (merge (sorted-map :acceptance (acceptance-measure worklist)
                       :gating gates :obligations findings :schema :f11/remainder-v1)
           (verdict findings gates))))

(defn temp-dir [] (.toFile (Files/createTempDirectory "f11-remainder-" (make-array FileAttribute 0))))
(defn write-edn! [f x] (spit f (with-out-str (pp/pprint x))))
(defn control [name planted before thunk]
  (try (sorted-map :after (thunk) :before before :control name :plant-verified? planted)
       (catch Exception e (sorted-map :after-hard-failure (.getMessage e) :before before
                                      :control name :plant-verified? planted))))

(defn controls [worklist aif holes conformance receipt f4 f3 reconciliation f3-run baseline]
  (let [tmp (temp-dir)
        run #(core %1 %2 %3 conformance receipt f4 f3 %4 f3-run %5)
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
        work-copy (io/file tmp "worklist.edn")
        w (edn/read-string (slurp worklist))
        idx (first (keep-indexed #(when (= :F11 (:id %2)) %1) (:items w)))
        w2 (update-in w [:items idx :acceptance] #(str/replace-first % "laws stated in Lean" "laws; stated in Lean"))
        _ (write-edn! work-copy w2)
        no-f3-gate (assoc gating-table :f3-citation-stated-in-lean
                          [[:find-sorry "citation field is not present"]])
        ruled-copy (io/file tmp "ruled-aif.edn")
        ar (edn/read-string (slurp aif))
        _ (write-edn! ruled-copy (assoc-in ar [:choices :find-sorry :ruling] :control))]
    [(control :delete-sorry-gate (not (contains? no-sorry-gate :sorry))
              (select-keys baseline verdict-keys)
              #(select-keys (run worklist aif holes reconciliation no-sorry-gate) verdict-keys))
     (control :perturb-gating-fragment
              (and (str/includes? changed-question "WHAT BECOMES OF THE REFUSAL")
                   (not (str/includes? changed-question "WHAT IS DONE WITH THE SORRY")))
              :measurement-succeeds #(run worklist aif-copy holes reconciliation gating-table))
     (control :make-live-fixture-match-pin
              (and (= 18 (:repository-count-live (edn/read-string (slurp rec-copy))))
                   (zero? (:receipts-differing (edn/read-string (slurp rec-copy)))))
              (select-keys (get-in baseline [:obligations :witnessed-on-real-find-over-committed-library])
                           [:satisfied-at-head? :live-repository-count :pin-repository-count :receipt-differences])
              #(select-keys (get-in (run worklist aif holes rec-copy gating-table)
                                    [:obligations :witnessed-on-real-find-over-committed-library])
                            [:satisfied-at-head? :live-repository-count :pin-repository-count :receipt-differences]))
     (control :replace-find-body
              (and (str/includes? (slurp holes-copy) "CONTROL_BODY")
                   (not (re-find #"def find[^\n]*:= sorry" (slurp holes-copy))))
              (get-in baseline [:obligations :sorry :body-is-still-sorry?])
              #(get-in (run worklist aif holes-copy reconciliation gating-table)
                       [:obligations :sorry :body-is-still-sorry?]))
     (control :change-acceptance-clause-count
              (let [planted-acceptance (get-in (edn/read-string (slurp work-copy))
                                               [:items idx :acceptance])]
                (and (str/includes? planted-acceptance "laws; stated in Lean")
                     (not (str/includes? planted-acceptance "laws stated in Lean"))))
              :three-clauses #(run work-copy aif holes reconciliation gating-table))
     (control :invent-f3-gate true :measurement-succeeds
              #(run worklist aif holes reconciliation no-f3-gate))
     (control :rule-a-gating-choice
              (and (= :control (get-in (edn/read-string (slurp ruled-copy))
                                       [:choices :find-sorry :ruling]))
                   (not (contains? (get-in (edn/read-string (slurp aif))
                                           [:choices :find-sorry]) :ruling)))
              :measurement-succeeds #(run worklist ruled-copy holes reconciliation gating-table))]))

(defn measure []
  (let [worklist (env-file "F11_WORKLIST" default-worklist)
        aif (env-file "F11_AIF_EQ" default-aif)
        holes (env-file "F11_HOLES" default-holes)
        conformance (env-file "F11_CONFORMANCE" default-conformance)
        receipt (env-file "F11_RECEIPT" default-receipt)
        f4 (env-file "F11_F4" default-f4)
        f3 (env-file "F11_F3" default-f3)
        reconciliation (env-file "F11_RECONCILIATION" default-reconciliation)
        f3-run (env-file "F11_F3_RUN" default-f3-run)
        base (core worklist aif holes conformance receipt f4 f3 reconciliation f3-run gating-table)]
    (assoc base :controls (controls worklist aif holes conformance receipt f4 f3
                                   reconciliation f3-run base))))

(try
  (let [out (env-file "F11_OUT" default-out)]
    (.mkdirs (.getParentFile out))
    (write-edn! out (measure))
    (println out))
  (catch Exception e
    (binding [*out* *err*] (println "f11_remainder_check:" (.getMessage e)))
    (System/exit 1)))
