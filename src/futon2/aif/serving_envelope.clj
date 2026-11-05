(ns futon2.aif.serving-envelope
  "A2-ENVELOPE-RECEIPT-ABSENT: capture one serving-envelope receipt at a
  run's start, before author dispatch, so the retained tick run record can
  answer \"what was the machine serving on when this run began?\".

  The criterion asks for a DECLARED envelope with honest gaps, not complete
  coverage: components the machine cannot observe from inside the runner JVM
  are listed under :unknown-components rather than asserted. What it already
  has and uses:

  * heap after GC, measured the way scripts/wm_click.sh preflight reports it
    (old-generation use after an explicit GC against the heap maximum);
  * the Agency invoke queue and seat roster at start (the same /api/alpha
    observations the runner already makes for readiness);
  * the admission opts the run was actually admitted with (the effective
    config projection).

  Receivers read the receipt as the top-level :serving-envelope key of the
  tick run record (data/wm-runs/tick-run-record-<run-id>.edn); the
  reconciliation matrix (holes/labs/wm-contract/reconciliation/MATRIX-A1-E4-*)
  reads that path."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import [java.lang.management ManagementFactory MemoryType]
           [java.time Instant]))

(def schema :wm/serving-envelope-v1)

(def admission-opt-keys
  "The bounded projection of the effective config that decides how the run
  is served. Anything not listed here did not gate admission."
  [:agency-base :substrate-url :author :reviewer :repair-reviewer
   :agent-budget-ms :poll-ms :window-days :phase-log :trigger :cohort?
   :semantic-epoch :revision-rounds :author-response-corrections
   :review-evidence-corrections :author-infrastructure-retries
   :build-cure-retries])

(def baseline-unknown-components
  "Components of the serving envelope the runner JVM cannot observe at all;
  they are declared unknown rather than silently absent."
  [:provider-quota-and-rate-limit-state
   :substrate-jvm-heap
   :os-memory-pressure
   :agency-jvm-heap])

(defn- mib
  "MiB rounding up, as wm_click.sh's ((kib+1023)//1024) does."
  [bytes]
  (quot (+ (long bytes) 1048575) 1048576))

(defn- old-gen-pool
  "The tenured heap pool (G1 Old Gen, Parallel Old, ...); nil when the
  collector names it something unrecognised."
  []
  (->> (ManagementFactory/getMemoryPoolMXBeans)
       (filter #(= MemoryType/HEAP (.getType ^java.lang.management.MemoryPoolMXBean %)))
       (filter #(str/includes? (str/lower-case (.getName ^java.lang.management.MemoryPoolMXBean %))
                               "old"))
       first))

(defn heap-receipt
  "Heap after an explicit GC, the same measurement wm_click.sh's probe_heap
  preflight takes via jcmd GC.run + GC.heap_info (old-gen use of heap max).
  `:gc-fn` is injectable so tests do not force a real GC."
  ([] (heap-receipt {}))
  ([{:keys [gc-fn] :or {gc-fn (fn [] (System/gc))}}]
   (try
     (gc-fn)
     (let [heap-usage (.getHeapMemoryUsage (ManagementFactory/getMemoryMXBean))
           pool (old-gen-pool)]
       (if pool
         (let [usage (.getUsage pool)]
           {:status :measured
            :basis :memory-pool-mx-bean-after-explicit-gc
            :old-gen-used-mib (mib (.getUsed usage))
            :old-gen-max-mib (mib (.getMax usage))
            :heap-used-mib (mib (.getUsed heap-usage))
            :heap-max-mib (mib (.getMax heap-usage))})
         {:status :measured
          :basis :heap-memory-mx-bean-after-explicit-gc
          :old-gen-used-mib :unknown
          :heap-used-mib (mib (.getUsed heap-usage))
          :heap-max-mib (mib (.getMax heap-usage))}))
     (catch Throwable e
       {:status :unknown :reason :heap-measurement-failed
        :error (.getName (class e))}))))

(defn- get-json
  [url timeout-ms]
  (let [r (http/get url {:timeout timeout-ms :throw false})]
    (when-not (= 200 (:status r))
      (throw (ex-info "Agency observation unavailable"
                      {:url url :status (:status r)})))
    (json/parse-string (:body r) true)))

(defn queue-snapshot
  "Agency invoke-queue state at run start: total and per-state counts from
  GET /api/alpha/invoke/jobs?limit=4000. Best-effort: any failure is
  reported as :status :unknown, never thrown into the run.

  The injection key is :invoke-jobs-fn, not :queue-fn: capture receives the
  runner's whole option map, where :queue-fn is the Morning Brief queuer.
  Sharing the key called that queuer with the Agency URL and recorded its
  return value as a measured, empty Agency queue."
  [{:keys [agency-base invoke-jobs-fn]}]
  (try
    (let [jobs (:jobs ((or invoke-jobs-fn
                           (fn [base]
                             (get-json (str base "/api/alpha/invoke/jobs?limit=4000")
                                       10000)))
                       agency-base))
          by-state (frequencies (map #(str (:state %)) jobs))]
      {:status :measured
       :source :agency-invoke-jobs-endpoint
       :total (count jobs)
       :by-state (into (sorted-map) by-state)})
    (catch Throwable e
      {:status :unknown :reason :queue-observation-failed
       :error (.getName (class e))})))

(defn seat-snapshot
  "Agency seat roster at run start, via the same observation the runner's
  readiness check uses. Retains each seat's status and invoke-readiness,
  not session transcripts."
  [{:keys [agency-base roster-fn]}]
  (try
    (let [roster ((or roster-fn
                      (fn [base]
                        (:agents (get-json (str base "/api/alpha/agents") 10000))))
                  agency-base)
          seats (into (sorted-map)
                      (map (fn [[seat record]]
                             [(name seat)
                              {:status (some-> (:status record) name)
                               :invoke-ready? (true? (:invoke-ready? record))}]))
                      roster)]
      {:status :measured
       :source :agency-roster-endpoint
       :seat-count (count seats)
       :seats seats})
    (catch Throwable e
      {:status :unknown :reason :roster-observation-failed
       :error (.getName (class e))})))

(defn admission-opts-receipt
  "The admission opts the run was actually admitted with: the bounded
  projection of the effective config at run start. Values are retained
  verbatim; absence of a key means the default applied."
  [effective-config]
  {:status :measured
   :opts (select-keys effective-config admission-opt-keys)})

(defn capture
  "One serving-envelope receipt for the run beginning now. Captures heap
  after GC, the Agency queue and seats, and the admission opts; every
  section that could not be measured is named in :unknown-components.
  Never throws: an envelope with honest gaps beats a run refused for its
  own instrumentation."
  [raw-opts effective-config]
  (let [observe (assoc raw-opts :agency-base (:agency-base effective-config))
        heap (heap-receipt raw-opts)
        queue (queue-snapshot observe)
        seats (seat-snapshot observe)
        failed (cond-> []
                 (not= :measured (:status heap)) (conj :heap)
                 (not= :measured (:status queue)) (conj :agency-queue)
                 (not= :measured (:status seats)) (conj :agency-seats))]
    {:schema schema
     :captured-at (str (Instant/now))
     :capture-point :run-start-before-author-dispatch
     :heap heap
     :agency {:queue queue :seats seats}
     :admission-opts (admission-opts-receipt effective-config)
     :unknown-components (into baseline-unknown-components failed)}))

(def absent-receipt
  {:schema schema
   :status :absent
   :reason :envelope-not-captured})
