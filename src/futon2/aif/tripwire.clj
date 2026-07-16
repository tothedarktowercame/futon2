(ns futon2.aif.tripwire
  "Read-only invariant observations for the live War Machine.

  `observe!` is deliberately total: it returns its phase record unchanged and
  no exception from a wire or trip action is allowed to reach the runner.  The
  richer `:tripwire/snapshot` key is an observational input seam for ledgers
  whose facts are not themselves phase telemetry."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.trace :as trace])
  (:import [java.nio.file Files StandardOpenOption]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def default-trip-root "/home/joe/code/futon2/data/wm-tripwires/trips")
(def default-action :record)
(def default-agency-base "http://127.0.0.1:7070")
(def summon-recipient "claude-6")
(def investigation-window-ms (* 45 60 1000))
(def repair-children ["findings" "implementations" "resolutions"])
(def known-job-states
  #{"queued" "pending" "running" "done" "failed" "cancelled" "timed-out"})

(defn- sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(def runner-namespace-sources
  {'futon2.aif.full-loop-runner "src/futon2/aif/full_loop_runner.clj"
   'futon2.aif.full-loop-cohort "src/futon2/aif/full_loop_cohort.clj"
   'futon2.aif.repair-obligation "src/futon2/aif/repair_obligation.clj"
   'futon2.aif.morning-brief "src/futon2/aif/morning_brief.clj"
   'futon2.aif.trace "src/futon2/aif/trace.clj"})

(defn- source-sha256 [path]
  (let [file (io/file path)]
    (when (.isFile file)
      (sha256-bytes (Files/readAllBytes (.toPath file))))))

(defn- public-var-fingerprint [namespace]
  (when-let [loaded (find-ns namespace)]
    (into (sorted-map)
          (keep (fn [[symbol var]]
                  (when (and (.isBound ^clojure.lang.Var var)
                             (fn? @var))
                    [symbol (.getName (class @var))])))
          (ns-publics loaded))))

(defn composition-snapshot
  "Hash the repo source and fingerprint loaded public function roots for the
  runner's own namespaces. Missing namespaces are omitted until loaded."
  []
  (into {}
        (keep (fn [[namespace path]]
                (when (find-ns namespace)
                  [namespace {:source-path path
                              :source-sha256 (source-sha256 path)
                              :public-functions
                              (public-var-fingerprint namespace)}])))
        runner-namespace-sources))

;; Captured as this observer loads. This is the loaded-source witness against
;; which each run-start snapshot is compared; namespaces loaded later (the
;; runner itself during its circular require) are admitted exactly once.
(defonce composition-baseline (atom (composition-snapshot)))

(def wire-registry
  "The S1 registry. Registry toggles are process-local and independently
  addressable; all wires start enabled in the default :record mode."
  (atom
   {:T1 {:title "turn conservation" :enabled? true}
    :T2 {:title "ledger closure" :enabled? true}
    :T3 {:title "exit and stop-line completeness" :enabled? true}
    :T4 {:title "A-matrix grounding provenance" :enabled? true}
    :T5 {:title "review commit binding" :enabled? true}
    :T6 {:title "repair-store immutability and status lattice" :enabled? true}
    :T7 {:title "consecutive stop-line wedge" :enabled? true}
    :T8 {:title "duplicate-finding livelock" :enabled? true}
    :T9 {:title "phase wall-clock budget" :enabled? true}
    :T10 {:title "loaded/file code coherence" :enabled? true}
    :T11 {:title "Agency job-state alphabet" :enabled? true}
    :T12 {:title "four-opportunity zero-grounding target wedge"
          :enabled? false :status :chartered-stub}}))

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

(defn- t4 [{:keys [a-matrix-events grounding-witnesses]}]
  (when (some? a-matrix-events)
    (let [events (vec a-matrix-events)
          witnesses (vec grounding-witnesses)
          implementation-ids (set (keep :implementation-id witnesses))
          ungrounded (vec (remove #(contains? implementation-ids (:entity-id %))
                                  events))
          unnamed (vec (remove :implementation-id witnesses))]
      (cond-> []
        (not= (count events) (count witnesses))
        (conj {:kind :belief-witness-count-mismatch
               :event-count (count events) :witness-count (count witnesses)})
        (seq ungrounded)
        (conj {:kind :belief-event-without-grounding
               :event-ids (mapv :event-id ungrounded)
               :entity-ids (mapv :entity-id ungrounded)})
        (seq unnamed)
        (conj {:kind :grounding-witness-without-implementation
               :witnesses unnamed})))))

(defn- reviewer-prompt [job]
  (or (:prompt job)
      (get-in job [:request :prompt])
      (get-in job [:payload :prompt])
      (some (fn [event]
              (when (= "prompt" (name (:type event))) (:text event)))
            (:events job))))

(defn- exact-sha-in-text? [text sha]
  (and (string? text) (string? sha)
       (boolean
        (re-find (re-pattern
                  (str "(?i)(?<![0-9a-f])"
                       (java.util.regex.Pattern/quote sha)
                       "(?![0-9a-f])"))
                 text))))

(defn- t5 [{:keys [grounded-commit reviewer-job cohort?] :as observation}]
  (when (and grounded-commit
             (or cohort? (:tripwire/force? observation)))
    (let [prompt (reviewer-prompt reviewer-job)]
      (when-not (exact-sha-in-text? prompt grounded-commit)
        [{:kind :review-grounding-commit-mismatch
          :grounded-commit grounded-commit
          :review-job-id (:job-id reviewer-job)
          :prompt-present? (string? prompt)}]))))

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

(defn wedge-violations
  "Return a T7 witness when the same unresolved stop-line occupies the last
  three opportunities. History entries carry `:selected-stop-line`."
  [cohort-history closed-repair-ids]
  (let [recent (vec (take-last 3 cohort-history))
        selected (mapv :selected-stop-line recent)
        repair-id (first selected)]
    (when (and (= 3 (count recent)) repair-id
               (apply = selected)
               (not (contains? (set closed-repair-ids) repair-id)))
      [{:kind :consecutive-stop-line-wedge
        :repair/id repair-id
        :attempt-ids (mapv #(or (:attempt-id %) (:attempt/id %)) recent)
        :consecutive-count 3}])))

(defn- t7 [{:keys [phase transition cohort-history closed-repair-ids]
            :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    (wedge-violations cohort-history closed-repair-ids)))

(defn livelock-violations
  "Group immutable findings by the T8 identity and return groups above K=2."
  [findings]
  (->> findings
       (group-by (juxt #(or (:failure-kind %) (:repair/class %))
                       :target :failed-commit))
       (keep (fn [[signature records]]
               (when (> (count records) 2)
                 {:kind :duplicate-finding-livelock
                  :signature signature
                  :repair-ids (mapv :repair/id records)
                  :finding-count (count records)})))
       vec))

(defn- t8 [{:keys [phase transition findings] :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    (livelock-violations findings)))

(defn- composition-drift [baseline current]
  (into []
        (keep (fn [[namespace expected]]
                (let [actual (get current namespace)]
                  (when (not= expected actual)
                    {:namespace namespace :loaded expected :repo/live actual}))))
        baseline))

(defn- t10 [{:keys [phase transition composition/current]
             :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    (let [current (or current (composition-snapshot))
          missing (apply dissoc current (keys @composition-baseline))
          _ (when (seq missing) (swap! composition-baseline merge missing))
          ;; A baseline fingerprint captured mid-load (the runner's circular
          ;; require) is EMPTY — a missing observation, not an observation of
          ;; emptiness. Admit the first real fingerprint exactly once, but only
          ;; while the source hash is unchanged; a sha mismatch is real
          ;; evidence regardless. (Shadow run 1: 15 false T10 trips against an
          ;; empty baseline, 2026-07-16.)
          _ (doseq [[ns-sym cur] current
                    :let [base (get @composition-baseline ns-sym)]
                    :when (and base
                               (empty? (:public-functions base))
                               (seq (:public-functions cur))
                               (= (:source-sha256 base) (:source-sha256 cur)))]
              (swap! composition-baseline assoc ns-sym cur))
          drift (composition-drift @composition-baseline current)]
      (when (seq drift)
        [{:kind :loaded-file-code-mismatch :drift drift}]))))

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

(defn- t12 [_]
  ;; Chartered by the third shadow run. Implementation waits for a calibrated
  ;; distinction between a productive multi-turn target and a true soft wedge.
  nil)

(def wire-evaluators
  {:T1 t1 :T2 t2 :T3 t3 :T4 t4 :T5 t5 :T6 t6 :T7 t7 :T8 t8
   :T9 t9 :T10 t10 :T11 t11 :T12 t12})

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

(defn- response-json [response]
  (let [body (:body response)]
    (cond
      (map? body) body
      (string? body) (json/parse-string body true)
      :else {})))

(defn- successful-response! [operation response]
  (let [status (:status response)
        body (response-json response)]
    (when-not (and (number? status) (<= 200 status 299)
                   (not= false (:ok body)))
      (throw (ex-info (str operation " failed")
                      {:operation operation :status status :response body})))
    body))

(defn- agency-base [opts]
  (or (:tripwire/agency-base opts) (:agency-base opts) default-agency-base))

(defn- registered-agents [opts]
  (if-let [roster-fn (:tripwire/roster-fn opts)]
    (set (roster-fn opts))
    (let [response (http/get (str (agency-base opts) "/api/alpha/agents")
                             {:timeout 10000 :throw false})
          body (successful-response! :tripwire-roster response)]
      (set (keys (:agents body))))))

(defn- post-park! [opts payload]
  (if-let [park-fn (:tripwire/park-fn opts)]
    (park-fn opts payload)
    (successful-response!
     :tripwire-park
     (http/post (str (agency-base opts) "/api/alpha/park")
                {:headers {"Content-Type" "application/json"}
                 :body (json/generate-string payload)
                 :timeout 10000 :throw false}))))

(defn- post-bell! [opts payload]
  (if-let [bell-fn (:tripwire/bell-fn opts)]
    (bell-fn opts payload)
    (let [body (successful-response!
                :tripwire-bell
                (http/post (str (agency-base opts) "/api/alpha/bell")
                           {:headers {"Content-Type" "application/json"}
                            :body (json/generate-string payload)
                            :timeout 10000 :throw false}))]
      (when-not (:accepted body)
        (throw (ex-info "Tripwire bell was not accepted" {:response body})))
      body)))

(defn- record-finding! [opts report report-path]
  (let [observation (:trip/observation report)
        wire-id (:trip/wire-id report)
        trip-id (:trip/id report)
        finding {:attempt-id (str (or (:attempt-id observation) "unscoped")
                                  "-" trip-id)
                 :repair-class :machine-failure
                 :target (or (:selected-target observation)
                             (:target observation)
                             (str "tripwire/" (name wire-id)))
                 :selected-entry (:selected-entry observation)
                 :failure-stage (or (:phase observation) :tripwire)
                 :outcome :incomplete
                 :failure-kind :invariant-tripped
                 :error (str "War Machine invariant " (name wire-id) " tripped")
                 :failure-data {:trip/id trip-id :trip/wire-id wire-id}
                 :backtrace {:trip-report report-path}
                 :discharge-contract
                 {:requires [:investigate-invariant-trip
                             :repair-machine-or-revise-wire
                             :production-shaped-successor]}}]
    ((or (:tripwire/repair-record-fn opts) repair/record-system-failure!) finding)))

(defn- witness-summary [witness]
  (let [rendered (pr-str witness)]
    (subs rendered 0 (min 1000 (count rendered)))))

(defn- investigation-text [report report-path]
  (str "WAR MACHINE TRIPWIRE INVESTIGATION\n"
       "trip: " (:trip/id report) "\n"
       "wire: " (name (:trip/wire-id report)) "\n"
       "witness: " (witness-summary (:trip/witness report)) "\n"
       "report: " report-path "\n"
       "Checklist: investigate then discharge or revise the wire per the practical contract."))

(defn- summon! [opts report report-path]
  (when-not (contains? (registered-agents opts) summon-recipient)
    (throw (ex-info "Tripwire summon recipient is not registered"
                    {:recipient summon-recipient})))
  (let [text (investigation-text report report-path)
        dependency-id (str (:trip/id report) "-investigation")]
    (post-park! opts {:agent summon-recipient
                      :surface "emacs-repl"
                      :mode :background
                      :awaiting [dependency-id]
                      :deadline-ms (+ (System/currentTimeMillis)
                                      investigation-window-ms)
                      :payload text})
    (post-bell! opts {:agent-id summon-recipient
                      :caller "wm-full-loop"
                      :surface "bell"
                      :mission-id "M-wm-tripwires-investigation"
                      :type "request"
                      :prompt text})))

(defn- handle-action! [opts report report-path]
  (case (:trip/action report)
    :record nil
    :stop-line
    (try
      (record-finding! opts report report-path)
      (catch Throwable e
        (stderr! "stop-line action failed; degraded to durable :record" e)))
    :park-and-summon
    (try
      (record-finding! opts report report-path)
      (try
        (summon! opts report report-path)
        (catch Throwable e
          (stderr! "park/summon action failed; degraded to :stop-line" e)))
      (catch Throwable e
        (stderr! "stop-line action failed; degraded to durable :record" e)))
    (stderr! "unknown trip action; degraded to durable :record" nil)))

(defn- record-trip! [opts raw-report]
  (if *handling-trip?*
    (do
      (stderr! "trip during trip handling; degraded to durable :record" nil)
      (try
        (write-trip-report! (or (:tripwire/report-root opts) default-trip-root)
                            (assoc raw-report :trip/action :record
                                              :trip/degraded? true))
        (catch Throwable e
          (stderr! "degraded trip report failed; recursion remains contained" e))))
    (binding [*handling-trip?* true]
      (try
        (let [report (assoc raw-report
                            :trip/id (or (:trip/id raw-report)
                                         (str "trip-" (UUID/randomUUID)))
                            :trip/action (or (:tripwire/action opts)
                                             default-action))
              report-path
              ((or (:tripwire/report-writer opts)
                   #(write-trip-report! (or (:tripwire/report-root opts)
                                            default-trip-root)
                                        %))
               report)]
          (when (str/blank? (str report-path))
            (throw (ex-info "Trip report writer returned no artifact path" {})))
          (handle-action! opts report report-path))
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

(defn- cross-run-observation [opts observation]
  (if (and (= :opportunity (:phase observation))
           (= :start (:transition observation))
           (or (:cohort? opts) (:tripwire/force? observation)))
    (let [repair-state (repair-snapshot (or (:repair-root opts)
                                            repair/default-root))
          findings (->> repair-state
                        (keep (fn [[path {:keys [record]}]]
                                (when (str/starts-with? path "findings/")
                                  record)))
                        vec)
          finding-ids (set (keep :repair/id findings))
          statuses (effective-statuses repair-state)
          closed-ids (into #{}
                           (keep (fn [[id status]]
                                   (when (#{:resolved :superseded} status) id)))
                           statuses)
          attempts (or (:tripwire/cohort-history opts)
                       (try (:attempts (cohort/ledger))
                            (catch Throwable _ [])))
          a-matrix-events
          (if (contains? opts :tripwire/a-matrix-events)
            (:tripwire/a-matrix-events opts)
            (try (:morning-brief-events (trace/latest-trace-record))
                 (catch Throwable _ [])))
          grounding-witnesses
          (if (contains? opts :tripwire/grounding-witnesses)
            (:tripwire/grounding-witnesses opts)
            (let [entity-ids (set (keep :entity-id a-matrix-events))]
              (try (->> (brief/items)
                        (keep :witness)
                        (filter #(contains? entity-ids (:implementation-id %)))
                        vec)
                   (catch Throwable _ []))))
          history (mapv (fn [attempt]
                          (let [selected (or (:selected-stop-line attempt)
                                             (:selected-mission attempt))]
                            {:attempt-id (or (:attempt-id attempt)
                                             (:attempt/id attempt))
                             :selected-stop-line
                             (when (contains? finding-ids selected) selected)}))
                        attempts)]
      (merge observation
             {:cohort-history history
              :closed-repair-ids closed-ids
              :findings findings
              :a-matrix-events (vec a-matrix-events)
              :grounding-witnesses grounding-witnesses}
             (:tripwire/cross-run-snapshot opts)))
    observation))

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
                              %))
                          (#(cross-run-observation opts %)))]
      (doseq [[wire-id _] @wire-registry
              :when (enabled? opts wire-id)
              witness (evaluate-wire wire-id observation)]
        (record-trip! opts {:trip/wire-id wire-id
                            :trip/witness witness
                            :trip/observation observation})))
    (catch Throwable e
      (stderr! "wire evaluation failed; runner remains untouched" e)))
  record)
