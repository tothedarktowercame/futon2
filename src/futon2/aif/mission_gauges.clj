(ns futon2.aif.mission-gauges
  "U42 — the producers for the three observables M-zaif-harness-v1's DECLARED
   GAUGES name (`war-machine/mission-c-declared-gauges`, U18).

   WHAT WAS MISSING. U18 bound each of that mission's three completion criteria
   to an observable — `:worklist-acceptance-state`, `:reporting-gate-test-result`,
   `:registry-gap-list-present`. None of the three is an R2 channel and nothing
   produced them, so U21's replay measured all three criteria reaching risk_mis
   and stopping there: 3/3 `:undeclared-observable`
   (`holes/labs/wm-contract/runs/U21-selection-focus/measurements.edn`). U21
   supplied a DECLARED PROBE to show what the shipped path returns once
   producers exist. This namespace is those producers, and the probe comes out
   of the path.

   WHAT A PRODUCER IS ALLOWED TO DO. Read one named artifact that already
   exists in the tree and report what is in it. It may not compute a stand-in,
   default to a neutral value, or fall back to a second source when the first
   is missing. A producer that cannot read its artifact returns
   `:status :absent` with a `:reason` and a `:would-need` naming what would
   have to exist — and supplies NO KEY to the observation, so the criterion
   reads `:undeclared-observable` exactly as it did before this namespace
   existed. That is the difference between a missing measurement and a
   measurement of zero, and the two want different repairs.

   WHERE THE VALUES GO. `reading` returns `:observables` (the measured keys
   only) and `:records` (all three, typed). `war-machine` merges `:observables`
   into the observation IT HANDS THE READBACK ONLY; the tick's own
   `observation` is untouched, so no channel, weight, G term, admissibility
   verdict or selector can see any of this. The typed `:records` ride on the
   readback as `:gauge-observables`.

   NO NETWORK. Every read is a file read under the futon2 checkout. The one
   observable whose source is a live store — the U8 reporting gate, which
   queries Z1 at :7073 — is read from a RECEIPT written by
   `holes/labs/wm-contract/u42_gate_receipt.bb`, because a producer inside a
   tick must not make a call whose latency it cannot bound nor silently re-pick
   a different subject decision every tick."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

(def version :mission-gauges/v2)

(def ^:dynamic *repo-root*
  "The futon2 checkout the artifacts are read from. Same convention as
   `war-machine/mission-c-criteria-sources`; dynamic so tests can point the
   producers at a fixture tree without touching the real one."
  (str (System/getProperty "user.home") "/code/futon2"))

(defn- artifact
  "A repo-relative path resolved against `*repo-root*`, kept as the relative
   string on the record so a pointer is quotable."
  [rel]
  (io/file *repo-root* rel))

(defn- sha256
  [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn- slurp-edn
  "Read one EDN artifact. Returns `{:ok true :value _ :sha256 _}` or
   `{:ok false :reason :file-absent|:unreadable :message _}` — never throws and
   never returns a partial value."
  [rel]
  (let [f (artifact rel)]
    (if-not (.isFile f)
      {:ok false :reason :file-absent :path rel}
      (try
        (let [bytes (slurp f)]
          {:ok true :value (edn/read-string bytes) :sha256 (sha256 bytes) :path rel})
        (catch Throwable e
          {:ok false :reason :unreadable :path rel :message (ex-message e)})))))

(defn- absent
  [observable producer reason would-need extra]
  (merge {:observable observable :producer producer
          :status :absent :reason reason :would-need would-need}
         extra))

(defn- measured
  [observable producer value extra]
  (merge {:observable observable :producer producer
          :status :measured :value value}
         extra))

;; ---------------------------------------------------------------------------
;; (1) :worklist-acceptance-state — "the U6-U9 worklist rows' acceptance state;
;;     worklist_check's output is machine-readable" (U18 gauge prose).

(def worklist-acceptance-rows
  "The four rows the criterion `:u-rows-green` names, each with the ledger it
   actually lives in. The criterion's own carrier says `zaif-harness +
   wm-contract worklists` (S4-identify-ingest.edn:31) and it means it: U6 is a
   wm-contract row, U7/U8/U9 are zaif-harness rows."
  [{:id :U6 :ledger "holes/labs/wm-contract/worklist.edn"}
   {:id :U7 :ledger "holes/labs/zaif-harness/worklist.edn"}
   {:id :U8 :ledger "holes/labs/zaif-harness/worklist.edn"}
   {:id :U9 :ledger "holes/labs/zaif-harness/worklist.edn"}])

(def accepted-status
  "GREEN is `:done` and only `:done`. `worklist_check.bb` refuses a `:done` row
   without `:reviewed-by`, so `:done` already carries the second reader;
   `:done-unreviewed` is work delivered and not yet accepted, which is what the
   criterion is asking about."
  #{:done})

(defn worklist-acceptance-state
  "1.0 iff every one of `worklist-acceptance-rows` is `:done` in its own
   ledger, 0.0 if any is not. A ledger that will not read, or a named row that
   is not in it, is a typed absence: the criterion is about four specific rows
   and three of four is not a smaller version of the same measurement."
  []
  (let [ledgers (into {} (map (juxt identity slurp-edn))
                      (distinct (map :ledger worklist-acceptance-rows)))
        bad (first (remove (comp :ok val) ledgers))]
    (if bad
      (absent :worklist-acceptance-state :worklist-acceptance-state/v1
              (:reason (val bad))
              (str "a readable EDN ledger at " (key bad))
              {:sources [{:path (key bad) :reason (:reason (val bad))}]})
      (let [rows (mapv (fn [{:keys [id ledger]}]
                         (let [items (:items (:value (get ledgers ledger)))
                               row (first (filter #(= id (:id %)) items))]
                           {:id id :ledger ledger
                            :found? (some? row)
                            :status (:status row)
                            :reviewed-by (:reviewed-by row)
                            :green? (boolean (accepted-status (:status row)))}))
                       worklist-acceptance-rows)
            missing (filterv (complement :found?) rows)
            sources (mapv (fn [[path r]] {:path path :sha256 (:sha256 r)}) ledgers)]
        (if (seq missing)
          (absent :worklist-acceptance-state :worklist-acceptance-state/v1
                  :row-not-found
                  (str "rows " (pr-str (mapv :id missing))
                       " in the ledgers the criterion names")
                  {:sources sources :basis {:rows rows}})
          (measured :worklist-acceptance-state :worklist-acceptance-state/v1
                    (if (every? :green? rows) 1.0 0.0)
                    {:sources sources
                     :basis {:rows rows
                             :green-count (count (filterv :green? rows))
                             :row-count (count rows)
                             :accepted-status accepted-status}}))))))

;; ---------------------------------------------------------------------------
;; (2) :reporting-gate-test-result — "U8's gate test result on one real zaif
;;     decision" (U18 gauge prose).

(def gate-receipt-path
  "holes/labs/wm-contract/runs/U42-producers/gate-receipt.edn")

(defn reporting-gate-test-result
  "1.0 iff the recorded U8 gate run HELD on the real decision it adjudicated,
   0.0 iff it did not. The criterion (`:reporting-gate-holds`) asks whether
   every report claim was re-derivable, so the observable is the gate's own
   `:verdict :ok` and not whether the test process exited zero — the test is
   green today precisely because it asserts the failure.

   No receipt is a typed absence, not a 0.0: `the gate has not been run` and
   `the gate ran and did not hold` are different facts and the second is the
   only one that scores."
  []
  (let [r (slurp-edn gate-receipt-path)
        would-need (str "a gate receipt at " gate-receipt-path
                        " (produced by holes/labs/wm-contract/u42_gate_receipt.bb,"
                        " which runs holes/labs/zaif-harness/report_gate.clj"
                        " against a real recorded zaif decision)")]
    (if-not (:ok r)
      (absent :reporting-gate-test-result :reporting-gate-test-result/v1
              (:reason r) would-need {:sources [{:path gate-receipt-path
                                                 :reason (:reason r)}]})
      (let [receipt (:value r)
            ok (get-in receipt [:verdict :ok])]
        (if-not (boolean? ok)
          (absent :reporting-gate-test-result :reporting-gate-test-result/v1
                  :receipt-carries-no-verdict
                  (str would-need "; the one present carries no boolean"
                       " [:verdict :ok]")
                  {:sources [{:path gate-receipt-path :sha256 (:sha256 r)}]})
          (measured :reporting-gate-test-result :reporting-gate-test-result/v1
                    (if ok 1.0 0.0)
                    {:sources [{:path gate-receipt-path :sha256 (:sha256 r)}]
                     :basis {:gate (get-in receipt [:gate :name])
                             :recorded-at (:receipt/at receipt)
                             :subject (:subject receipt)
                             :failures (get-in receipt [:verdict :failures])
                             :failed-claim-count (get-in receipt [:verdict :failed-claim-count])
                             :claim-count (get-in receipt [:verdict :claim-count])}}))))))

;; ---------------------------------------------------------------------------
;; (3) :registry-gap-list-present — "presence of the gap-list artifact where
;;     aif-equations.edn points" (U18 gauge prose).

(def registry-path "holes/labs/wm-contract/aif-equations.edn")

(def gap-list-pointer-keys
  "Where a gap-list pointer would be declared: a top-level `:zaif-node-gap-list`
   on the registry, or a `:gap-list` on any `:equations` entry. Both are read;
   neither exists today, which is the measurement."
  {:registry :zaif-node-gap-list :equation :gap-list})

(def known-gap-list-artifacts
  "Gap lists that EXIST in the tree, recorded on the absence so the repair is
   legible: the criterion is not `is a gap list written` (one is —
   C489-u6-zaif-full-loop.md §6 and U10-node-coverage.edn) but `is it recorded
   where the registry points`, and the registry points at neither."
  ["holes/labs/zaif-harness/runs/U10-node-coverage.edn"
   "holes/labs/wm-contract/C489-u6-zaif-full-loop.md"])

(defn- declared-gap-list-pointer
  [registry]
  (or (get registry (:registry gap-list-pointer-keys))
      (some (:equation gap-list-pointer-keys) (:equations registry))))

(defn registry-gap-list-present
  "1.0 iff the registry declares a gap-list pointer AND a file is there; 0.0
   otherwise. The registry declaring nothing is a MEASUREMENT of this
   observable, not an absence of one — the observable is `is the gap list where
   the registry points`, and a registry that points nowhere answers it. The
   absence path is reserved for a registry this producer cannot read at all."
  []
  (let [r (slurp-edn registry-path)]
    (if-not (:ok r)
      (absent :registry-gap-list-present :registry-gap-list-present/v1
              (:reason r)
              (str "a readable EDN registry at " registry-path)
              {:sources [{:path registry-path :reason (:reason r)}]})
      (let [pointer (declared-gap-list-pointer (:value r))
            present? (boolean (and pointer (.isFile (artifact (str pointer)))))
            unpointed (filterv #(.isFile (artifact %)) known-gap-list-artifacts)]
        (measured :registry-gap-list-present :registry-gap-list-present/v1
                  (if present? 1.0 0.0)
                  {:sources [{:path registry-path :sha256 (:sha256 r)}]
                   :basis (cond-> {:registry-pointer pointer
                                   :pointer-keys gap-list-pointer-keys
                                   :file-present? present?}
                            (nil? pointer)
                            (assoc :reason :no-registry-pointer
                                   :gap-lists-found-not-registry-pointed unpointed))})))))

;; ---------------------------------------------------------------------------
;; U79 -- M-expressions-of-interest producer inventory.

(def eoi-producer-inventory
  "The six producer branches required by the mission's six completion
   criteria.  These are deliberately contracts, not guesses at answers.  Each
   names the records from which a future boolean can be computed and the
   mission lines that require them.  A branch with no declared input path
   returns typed absence; it never turns an unrecorded human judgement into
   zero."
  {:eoi-prior-constrains-drafting
   {:criterion :criterion-1 :basis "futon5a/holes/missions/M-expressions-of-interest.md:174"
    :requires [:self-note-path :drafting-occasion-ledger]
    :would-need "a declared self-note path and dated drafting ledger recording reread and constrained booleans"}
   :eoi-basin-geometry-live
   {:criterion :criterion-2 :basis "futon5a/holes/missions/M-expressions-of-interest.md:177"
    :requires [:basin-register-path :basin-liveness-verdicts-path]
    :would-need "a declared basin register with satisfies/forecloses/costs fields and recorded liveness verdicts"}
   :eoi-template-artifacts-really
   {:criterion :criterion-3 :basis "futon5a/holes/missions/M-expressions-of-interest.md:181"
    :requires [:artifact-register-path :really-verdicts-path]
    :would-need "a per-artifact template-conformance record and Joe-entered really? verdict"}
   :eoi-hyperreal-brief-nonforeclosure
   {:criterion :criterion-4 :basis "futon5a/holes/missions/M-expressions-of-interest.md:183"
    :requires [:hyperreal-brief-path :nonforeclosure-verdict-path]
    :would-need "a declared brief path, page-equivalent measurement, and recorded non-foreclosure judgement"}
   :eoi-diagnoses-speech-acts
   {:criterion :criterion-5 :basis "futon5a/holes/missions/M-expressions-of-interest.md:185"
    :requires [:gary-diagnosis-path :bristol-diagnosis-path]
    :would-need "declared Gary and Bristol diagnosis paths, each with a Template speech-act attribution"}
   :eoi-generative-new-basin
   {:criterion :criterion-6 :basis "futon5a/holes/missions/M-expressions-of-interest.md:188"
    :requires [:basin-register-path :basin-baseline-path]
    :would-need "a dated basin register and declared baseline from which previously-unnamed is computed"}})

(def ^:dynamic *eoi-producer-inputs*
  "Declared input paths for the EOI producer branches.  Empty until the
   mission names authorities; tests bind this map to exercise the measured
   branch without granting fixture paths production authority."
  {})

(defn eoi-criterion-reading
  "Run one EOI producer branch. Input records are EDN maps. A complete set of
   readable inputs must carry a boolean `:criterion-holds?`; disagreement or a
   missing verdict is absence rather than a fabricated value."
  [observable]
  (let [{:keys [criterion basis requires would-need] :as contract}
        (get eoi-producer-inventory observable)
        paths (select-keys *eoi-producer-inputs* requires)]
    (cond
      (nil? contract)
      (absent observable :eoi/unknown :unknown-producer
              "an observable in eoi-producer-inventory" {})

      (not= (set requires) (set (keys paths)))
      (absent observable (keyword "eoi" (str (name observable) "-v1"))
              :producer-input-not-declared would-need
              {:criterion criterion :basis basis :requires requires
               :declared-inputs (into (sorted-map) paths)})

      :else
      (let [reads (mapv (fn [k]
                          (assoc (slurp-edn (get paths k)) :input k))
                        requires)
            failed (first (remove :ok reads))
            verdicts (mapv #(get-in % [:value :criterion-holds?]) reads)]
        (cond
          failed
          (absent observable (keyword "eoi" (str (name observable) "-v1"))
                  (:reason failed) would-need
                  {:criterion criterion :basis basis
                   :sources (mapv #(select-keys % [:input :path :sha256 :reason]) reads)})

          (not (every? boolean? verdicts))
          (absent observable (keyword "eoi" (str (name observable) "-v1"))
                  :input-carries-no-verdict would-need
                  {:criterion criterion :basis basis
                   :sources (mapv #(select-keys % [:input :path :sha256]) reads)})

          (not (apply = verdicts))
          (absent observable (keyword "eoi" (str (name observable) "-v1"))
                  :input-verdicts-disagree would-need
                  {:criterion criterion :basis basis :verdicts verdicts
                   :sources (mapv #(select-keys % [:input :path :sha256]) reads)})

          :else
          (measured observable (keyword "eoi" (str (name observable) "-v1"))
                    (if (first verdicts) 1.0 0.0)
                    {:criterion criterion :basis basis
                     :sources (mapv #(select-keys % [:input :path :sha256]) reads)}))))))

;; ---------------------------------------------------------------------------

(def producers
  "Observable -> producer fn. The set is closed: an observable a gauge names
   and this map does not carry has no producer, and says so by being absent."
  (merge {:worklist-acceptance-state worklist-acceptance-state
          :reporting-gate-test-result reporting-gate-test-result
          :registry-gap-list-present registry-gap-list-present}
         (into {} (map (fn [observable]
                         [observable #(eoi-criterion-reading observable)]))
               (keys eoi-producer-inventory))))

(defn reading
  "Run every producer. `:observables` carries ONLY the measured values, so an
   absent producer leaves its criterion exactly as unmeasurable as it was
   before; `:records` carries all of them, typed, for the record.

   A producer that throws is caught and typed here rather than failing the
   tick — the readback is a terminal projection and must not be able to take
   the judgement down."
  []
  (let [records (mapv (fn [[observable f]]
                        (try (f)
                             (catch Throwable e
                               (absent observable :unknown :producer-threw
                                       "a producer that does not throw"
                                       {:message (ex-message e)}))))
                      (sort-by key producers))]
    {:version version
     :records records
     :observables (into {} (keep (fn [r]
                                   (when (= :measured (:status r))
                                     [(:observable r) (:value r)])))
                        records)}))
