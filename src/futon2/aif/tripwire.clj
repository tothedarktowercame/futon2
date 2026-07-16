(ns futon2.aif.tripwire
  "Read-only invariant observations for the live War Machine.

  `observe!` is deliberately total: it returns its phase record unchanged and
  no exception from a wire or trip action is allowed to reach the runner.  The
  richer `:tripwire/snapshot` key is an observational input seam for ledgers
  whose facts are not themselves phase telemetry."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files StandardOpenOption]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-trip-root "/home/joe/code/futon2/data/wm-tripwires/trips")
(def repair-children ["findings" "implementations" "resolutions"])
(def known-job-states
  #{"queued" "pending" "running" "done" "failed" "cancelled" "timed-out"})

(def wire-registry
  "The S1 registry. Registry toggles are process-local and independently
  addressable; all wires start enabled in the default :record mode."
  (atom
   {:T1 {:title "turn conservation" :enabled? true}
    :T2 {:title "ledger closure" :enabled? true}
    :T3 {:title "exit and stop-line completeness" :enabled? true}
    :T6 {:title "repair-store immutability and status lattice" :enabled? true}
    :T9 {:title "phase wall-clock budget" :enabled? true}
    :T11 {:title "Agency job-state alphabet" :enabled? true}}))

(defonce ^:private phase-snapshots (atom {}))
(def ^:dynamic *handling-trip?* false)

(defn set-wire-enabled!
  "Enable or disable one registered wire. Returns its updated registry entry."
  [wire-id enabled?]
  (when-not (contains? @wire-registry wire-id)
    (throw (ex-info "Unknown tripwire id" {:wire-id wire-id})))
  (get (swap! wire-registry assoc-in [wire-id :enabled?] (boolean enabled?))
       wire-id))

(defn- enabled? [opts wire-id]
  (and (true? (get-in @wire-registry [wire-id :enabled?]))
       (not (contains? (set (:tripwire/disabled-wire-ids opts)) wire-id))))

(defn- sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- read-record [file]
  (try
    (edn/read-string (slurp file))
    (catch Throwable e
      {:tripwire/unreadable? true :tripwire/error (.getMessage e)})))

(defn repair-snapshot
  "Immutable audit snapshot of the three repair-record directories."
  ([] (repair-snapshot repair/default-root))
  ([root]
   (into {}
         (for [child repair-children
               file (or (.listFiles (io/file root child)) [])
               :when (and (.isFile file) (str/ends-with? (.getName file) ".edn"))]
           (let [relative (str child "/" (.getName file))]
             [relative {:sha256 (sha256-bytes (Files/readAllBytes (.toPath file)))
                        :record (read-record file)}])))))

(defn- effective-statuses [snapshot]
  (reduce (fn [statuses [_ {:keys [record]}]]
            (let [id (:repair/id record)
                  status (:repair/status record)]
              (if (and id status)
                (assoc statuses id status)
                statuses)))
          {}
          (sort-by (fn [[path _]]
                     (or ({"findings" 0 "implementations" 1 "resolutions" 2}
                          (first (str/split path #"/")))
                         3))
                   snapshot)))

(def allowed-status-edges
  #{[:open :awaiting-validation]
    [:awaiting-validation :resolved]
    [:open :superseded]})

(defn- t1 [{:keys [runner/dispatched-turns agency/dispatch-count]}]
  (when (and (some? dispatched-turns) (some? dispatch-count)
             (not= dispatched-turns dispatch-count))
    [{:kind :turn-conservation
      :runner/dispatched-turns dispatched-turns
      :agency/dispatch-count dispatch-count}]))

(defn- commit-present? [{:keys [repo sha]}]
  (and (string? repo) (string? sha)
       (zero? (:exit (shell/sh "git" "-C" repo "cat-file" "-e"
                               (str sha "^{commit}"))))))

(defn- t2 [{:keys [referenced-job-ids agency/job-ids referenced-commits]}]
  (let [ledger-ids (set job-ids)
        missing-jobs (when (some? referenced-job-ids)
                       (vec (remove ledger-ids referenced-job-ids)))
        missing-commits (when (some? referenced-commits)
                          (vec (remove commit-present? referenced-commits)))]
    (cond-> []
      (seq missing-jobs)
      (conj {:kind :missing-agency-jobs :job-ids missing-jobs})
      (seq missing-commits)
      (conj {:kind :missing-commits :commits missing-commits}))))

(defn- attempt-finding-statuses [root attempt-id]
  (let [snapshot (repair-snapshot root)
        finding-ids (into #{}
                          (keep (fn [[_ {:keys [record]}]]
                                  (when (= attempt-id (:attempt-id record))
                                    (:repair/id record))))
                          snapshot)
        statuses (effective-statuses snapshot)]
    (set (keep statuses finding-ids))))

(defn- t3 [{:keys [phase transition outcome attempt-id cohort?]
            :as observation}]
  (when (and (= :opportunity phase) (= :end transition)
             (or cohort? (:tripwire/force? observation)))
    (let [enumerated? (contains? cohort/outcome-kinds outcome)
          zero-achievement? (not= :grounded-change outcome)
          statuses (when (and zero-achievement? attempt-id)
                     (attempt-finding-statuses
                      (or (:repair-root observation) repair/default-root)
                      attempt-id))]
      (cond-> []
        (not enumerated?)
        (conj {:kind :unknown-outcome :outcome outcome})
        (and zero-achievement?
             (empty? (set/intersection #{:open :superseded} statuses)))
        (conj {:kind :missing-durable-stop-line
               :attempt-id attempt-id :statuses statuses})))))

(defn- t6 [{:keys [repair-before repair-after]}]
  (when (and repair-before repair-after)
    (let [changed (into []
                        (keep (fn [[path before]]
                                (when-let [after (get repair-after path)]
                                  (when (not= (:sha256 before) (:sha256 after))
                                    path))))
                        repair-before)
          deleted (vec (remove #(contains? repair-after %) (keys repair-before)))
          before-status (effective-statuses repair-before)
          after-status (effective-statuses repair-after)
          invalid-edges
          (into []
                (keep (fn [[id after]]
                        (let [before (get before-status id)]
                          (when (and (not= before after)
                                     (not= [nil :open] [before after])
                                     (not (contains? allowed-status-edges
                                                     [before after])))
                            {:repair/id id :from before :to after}))))
                after-status)]
      (cond-> []
        (seq changed) (conj {:kind :repair-record-mutated :paths changed})
        (seq deleted) (conj {:kind :repair-record-deleted :paths deleted})
        (seq invalid-edges) (conj {:kind :invalid-repair-status-edge
                                   :edges invalid-edges})))))

(defn- t9 [{:keys [transition phase duration-ms phase-budget-ms]}]
  (when (and (= :end transition) (number? duration-ms)
             (number? phase-budget-ms) (pos? phase-budget-ms)
             (> duration-ms (* 2 phase-budget-ms)))
    [{:kind :phase-budget-exceeded :phase phase :duration-ms duration-ms
      :budget-ms phase-budget-ms :multiple (/ (double duration-ms)
                                               phase-budget-ms)}]))

(defn- timestamp-values [job]
  (concat (keep job [:created-at :started-at :completed-at :updated-at])
          (keep :at (:events job))))

(defn- parseable-instant? [x]
  (and (string? x)
       (try (Instant/parse x) true (catch Throwable _ false))))

(defn- t11 [{:keys [job-snapshot job-snapshots]}]
  (let [jobs (cond job-snapshot [job-snapshot]
                   (some? job-snapshots) job-snapshots
                   :else nil)]
    (into []
          (mapcat (fn [job]
                    (let [timestamps (vec (timestamp-values job))]
                      (cond-> []
                        (not (contains? known-job-states (:state job)))
                        (conj {:kind :unknown-job-state :job-id (:job-id job)
                               :state (:state job)})
                        (or (empty? timestamps)
                            (not-every? parseable-instant? timestamps))
                        (conj {:kind :unparseable-job-time :job-id (:job-id job)
                               :timestamps timestamps}))))
          jobs))))

(def wire-evaluators
  {:T1 t1 :T2 t2 :T3 t3 :T6 t6 :T9 t9 :T11 t11})

(defn evaluate-wire
  "Return this wire's violation witnesses for one complete observation."
  [wire-id observation]
  (if-let [evaluate (get wire-evaluators wire-id)]
    (vec (or (evaluate observation) []))
    (throw (ex-info "Unknown tripwire id" {:wire-id wire-id}))))

(defn write-trip-report!
  "Durably create one append-only EDN trip report. CREATE_NEW forbids rewrite."
  ([report] (write-trip-report! default-trip-root report))
  ([root report]
   (let [id (or (:trip/id report) (str "trip-" (UUID/randomUUID)))
         path (io/file root (str id ".edn"))
         record (merge {:trip/id id :trip/schema-version 1
                        :trip/recorded-at (str (Instant/now))}
                       report)]
     (io/make-parents path)
     (Files/write (.toPath path)
                  (.getBytes (with-out-str (pp/pprint record)) "UTF-8")
                  (into-array StandardOpenOption
                              [StandardOpenOption/CREATE_NEW
                               StandardOpenOption/WRITE]))
     (.getPath path))))

(defn- stderr! [message throwable]
  (binding [*out* *err*]
    (println "[wm-tripwire]" message
             (when throwable (str "-" (.getMessage throwable))))
    (flush)))

(declare observe!)

(defn- record-trip! [opts report]
  (if *handling-trip?*
    (do
      (stderr! "trip during trip handling; degraded to durable :record" nil)
      (try
        (write-trip-report! (or (:tripwire/report-root opts) default-trip-root)
                            (assoc report :trip/action :record
                                          :trip/degraded? true))
        (catch Throwable e
          (stderr! "degraded trip report failed; recursion remains contained" e))))
    (binding [*handling-trip?* true]
      (try
        ((or (:tripwire/report-writer opts)
             #(write-trip-report! (or (:tripwire/report-root opts)
                                      default-trip-root)
                                  %))
         report)
        (catch Throwable e
          (stderr! "trip report action failed; runner remains untouched" e))))))

(defn- phase-budget [opts record]
  (or (get (:tripwire/phase-budgets-ms opts) (:phase record))
      (when (#{:author-wait :reviewer-wait} (:phase record))
        (:agent-budget-ms opts))
      (:tripwire/default-phase-budget-ms opts)))

(defn- with-repair-boundary [opts record]
  (let [key [(:opportunity-id record) (:phase record)]
        root (or (:repair-root opts) repair/default-root)]
    (case (:transition record)
      :start (do (swap! phase-snapshots assoc key (repair-snapshot root)) record)
      :end (if-let [before (get @phase-snapshots key)]
             (do (swap! phase-snapshots dissoc key)
                 (assoc record :repair-before before
                               :repair-after (repair-snapshot root)))
             record)
      record)))

(defn observe!
  "Evaluate enabled wires and perform their actions, returning `record`
  identically. No exception is permitted to escape this observational seam."
  [opts record]
  (try
    (let [observation (-> (merge record (:tripwire/snapshot record)
                                 {:cohort? (:cohort? opts)
                                  :repair-root (or (:repair-root opts)
                                                   repair/default-root)})
                          (assoc :phase-budget-ms (phase-budget opts record))
                          (#(if (enabled? opts :T6)
                              (with-repair-boundary opts %)
                              %)))]
      (doseq [[wire-id _] @wire-registry
              :when (enabled? opts wire-id)
              witness (evaluate-wire wire-id observation)]
        (record-trip! opts {:trip/wire-id wire-id
                            :trip/action (or (:tripwire/action opts) :record)
                            :trip/witness witness
                            :trip/observation observation})))
    (catch Throwable e
      (stderr! "wire evaluation failed; runner remains untouched" e)))
  record)
