(ns futon2.aif.mission-gauges-test
  "U42 acceptance: one test per producer against the REAL artifact it reads,
   plus the typed-absence path for each.

   LIVE PIN. Every `-reads-the-real-artifact` test below runs the shipped
   producer against the tracked bytes in this checkout — the two worklist
   ledgers, the gate receipt written from a live Z1 read, and the registry —
   and asserts the value together with the provenance that produced it. A
   producer that starts answering from somewhere else, or an artifact that
   moves, fails here rather than passing quietly.

   THE ABSENCE TESTS ARE THE OTHER HALF. `never a value standing in for a
   measurement` is only worth asserting if the missing-artifact path is
   exercised, so each producer is also run against a fixture tree where its
   artifact is not there, and the assertion is that it supplies NO KEY."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.mission-gauges :as gauges]))

(def repo-root
  "The checkout the producers read by default."
  (str (System/getProperty "user.home") "/code/futon2"))

(defn- empty-tree
  "A directory with none of the artifacts in it — the `cannot read` tree."
  []
  (str (java.nio.file.Files/createTempDirectory
        "mission-gauges-absent"
        (into-array java.nio.file.attribute.FileAttribute []))))

;; ---------------------------------------------------------------------------
;; (1) :worklist-acceptance-state

(deftest worklist-acceptance-state-reads-the-real-ledgers
  (let [r (gauges/worklist-acceptance-state)
        rows (get-in r [:basis :rows])]
    (is (= :measured (:status r)))
    (is (= :worklist-acceptance-state (:observable r)))
    (is (= [:U6 :U7 :U8 :U9] (mapv :id rows))
        "the four rows the criterion :u-rows-green names")
    (is (= ["holes/labs/wm-contract/worklist.edn"
            "holes/labs/zaif-harness/worklist.edn"
            "holes/labs/zaif-harness/worklist.edn"
            "holes/labs/zaif-harness/worklist.edn"]
           (mapv :ledger rows))
        "U6 is a wm-contract row; U7-U9 are zaif-harness rows")
    (is (every? :found? rows) "every named row is in the ledger it is claimed for")
    ;; The live pin: the VALUE and the statuses that produced it, together.
    (is (= 1.0 (:value r)))
    (is (every? #(= :done (:status %)) rows))
    (is (= 4 (get-in r [:basis :green-count])))
    (is (every? (comp string? :sha256) (:sources r))
        "each ledger read is pinned by the digest of the bytes read")))

(deftest worklist-acceptance-state-is-zero-only-when-a-row-is-not-accepted
  (testing ":done-unreviewed is delivered, not accepted"
    (is (not (contains? gauges/accepted-status :done-unreviewed))
        "worklist_check refuses :done without :reviewed-by, so :done is the
         accepted state and :done-unreviewed is not")))

(deftest worklist-acceptance-state-types-a-missing-ledger
  (binding [gauges/*repo-root* (empty-tree)]
    (let [r (gauges/worklist-acceptance-state)]
      (is (= :absent (:status r)))
      (is (= :file-absent (:reason r)))
      (is (not (contains? r :value)) "no value stands in for the measurement")
      (is (re-find #"worklist\.edn" (:would-need r))
          "the absence names what would have to exist"))))

;; ---------------------------------------------------------------------------
;; (2) :reporting-gate-test-result

(deftest reporting-gate-test-result-reads-the-real-receipt
  (let [r (gauges/reporting-gate-test-result)]
    (is (= :measured (:status r)))
    ;; The live pin: this receipt was written by running the U8 gate against
    ;; the Z1 store at :7073 over a real recorded :zaif-arm-choice.
    (is (= 0.0 (:value r))
        "the U8 gate does NOT hold on a real zaif decision today")
    (is (= :u8/report-backed-by-typed-records (get-in r [:basis :gate])))
    (is (= [:u8/decision-mission-attribution-absent]
           (get-in r [:basis :failures]))
        "the one standing failure U8's own test pins")
    (is (= 1 (get-in r [:basis :failed-claim-count])))
    (is (= 5 (get-in r [:basis :claim-count])))
    (is (= :zaif-arm-choice (get-in r [:basis :subject :event])))
    (is (nil? (get-in r [:basis :subject :recorded-mission]))
        "the decision carries no mission — which is exactly the failure")))

(deftest reporting-gate-test-result-types-a-missing-receipt
  (binding [gauges/*repo-root* (empty-tree)]
    (let [r (gauges/reporting-gate-test-result)]
      (is (= :absent (:status r)))
      (is (= :file-absent (:reason r)))
      (is (not (contains? r :value))
          "`the gate has not been run` is not `the gate ran and did not hold`")
      (is (re-find #"u42_gate_receipt\.bb" (:would-need r))))))

;; ---------------------------------------------------------------------------
;; (3) :registry-gap-list-present

(deftest registry-gap-list-present-reads-the-real-registry
  (let [r (gauges/registry-gap-list-present)]
    (is (= :measured (:status r)))
    ;; The live pin, and the finding U42 records: the registry declares NO
    ;; gap-list pointer, so the gap list is not where the registry points.
    (is (= 0.0 (:value r)))
    (is (nil? (get-in r [:basis :registry-pointer])))
    (is (= :no-registry-pointer (get-in r [:basis :reason])))
    (is (false? (get-in r [:basis :file-present?])))
    (is (seq (get-in r [:basis :gap-lists-found-not-registry-pointed]))
        "a gap list IS written; it is the registry pointer that is missing")))

(deftest registry-gap-list-present-is-one-when-the-registry-points-at-a-file
  (let [root (empty-tree)
        registry (io/file root "holes/labs/wm-contract/aif-equations.edn")
        target "holes/labs/wm-contract/gap-list.edn"]
    (io/make-parents registry)
    (spit (io/file root target) "{:gap-list :here}")
    (testing "pointer resolving to a file"
      (spit registry (pr-str {:zaif-node-gap-list target}))
      (binding [gauges/*repo-root* root]
        (let [r (gauges/registry-gap-list-present)]
          (is (= 1.0 (:value r)))
          (is (= target (get-in r [:basis :registry-pointer]))))))
    (testing "pointer resolving to nothing is 0.0, not an absence"
      (spit registry (pr-str {:zaif-node-gap-list "holes/labs/wm-contract/not-there.edn"}))
      (binding [gauges/*repo-root* root]
        (let [r (gauges/registry-gap-list-present)]
          (is (= :measured (:status r)))
          (is (= 0.0 (:value r)))
          (is (false? (get-in r [:basis :file-present?]))))))
    (testing "an equation entry may carry the pointer instead"
      (spit registry (pr-str {:equations [{:id :risk} {:id :observe :gap-list target}]}))
      (binding [gauges/*repo-root* root]
        (is (= 1.0 (:value (gauges/registry-gap-list-present))))))
    (testing "an unreadable registry is an absence, not a 0.0"
      (spit registry "{:unbalanced ")
      (binding [gauges/*repo-root* root]
        (let [r (gauges/registry-gap-list-present)]
          (is (= :absent (:status r)))
          (is (= :unreadable (:reason r)))
          (is (not (contains? r :value))))))))

;; ---------------------------------------------------------------------------
;; The reading as a whole

(deftest reading-supplies-only-what-was-measured
  (let [r (gauges/reading)]
    (is (= gauges/version (:version r)))
    (is (= 9 (count (:records r))) "one record per declared producer")
    (is (= (set (keys gauges/producers))
           (set (map :observable (:records r)))))
    (is (= {:worklist-acceptance-state 1.0
            :reporting-gate-test-result 0.0
            :registry-gap-list-present 0.0}
           (:observables r))
        "the U18 observables measure; all six U79 branches report typed absence")))

(deftest an-absent-producer-supplies-no-key
  (binding [gauges/*repo-root* (empty-tree)]
    (let [r (gauges/reading)]
      (is (= 9 (count (:records r))))
      (is (every? #(= :absent (:status %)) (:records r)))
      (is (= {} (:observables r))
          "nothing is supplied, so every criterion stays :undeclared-observable
           exactly as it was before this namespace existed")
      (is (every? :would-need (:records r))
          "each absence names what would have to exist"))))

(deftest all-six-eoi-producer-branches-are-runnable
  (let [records (mapv gauges/eoi-criterion-reading
                      (keys gauges/eoi-producer-inventory))]
    (is (= 6 (count records)))
    (is (= (set (keys gauges/eoi-producer-inventory))
           (set (map :observable records))))
    (is (every? #(= :producer-input-not-declared (:reason %)) records))
    (is (every? :basis records))
    (is (every? :would-need records))))

(deftest eoi-producer-measured-false-and-true-branches
  (let [root (empty-tree)
        observable :eoi-prior-constrains-drafting
        paths {:self-note-path "self-note.edn"
               :drafting-occasion-ledger "drafting.edn"}]
    (doseq [[_ path] paths]
      (spit (io/file root path) (pr-str {:criterion-holds? false})))
    (binding [gauges/*repo-root* root gauges/*eoi-producer-inputs* paths]
      (let [r (gauges/eoi-criterion-reading observable)]
        (is (= :measured (:status r)))
        (is (= 0.0 (:value r)))
        (is (= 2 (count (:sources r))))))
    (doseq [[_ path] paths]
      (spit (io/file root path) (pr-str {:criterion-holds? true})))
    (binding [gauges/*repo-root* root gauges/*eoi-producer-inputs* paths]
      (is (= 1.0 (:value (gauges/eoi-criterion-reading observable)))))))
