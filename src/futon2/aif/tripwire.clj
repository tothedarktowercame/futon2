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
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.interoceptive-store-lock :as store-lock]
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
(def artifact-window-tolerance-ms (* 2 60 1000))
(def repair-children ["findings" "implementations" "resolutions" "dismissals"])
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
          :enabled? false :status :chartered-stub}
    :T13 {:title "author-artifact binding" :enabled? true}}))

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
  "Immutable audit snapshot of the repair-record directories, including dispositions."
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
                     (or ({"findings" 0 "implementations" 1 "resolutions" 2 "dismissals" 3}
                          (first (str/split path #"/")))
                         4))
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
    (let [durable-attempt-id (or (:external-attempt-id observation) attempt-id)
          cohort-complete? (= :cohort-complete outcome)
          enumerated? (or cohort-complete?
                          (contains? cohort/outcome-kinds outcome))
          admitted-verification? (= :historical-verification-awaiting-validation outcome)
          zero-achievement? (and (not= :grounded-change outcome)
                                 (not cohort-complete?)
                                 (not admitted-verification?))
          statuses (when (and zero-achievement? durable-attempt-id)
                     (attempt-finding-statuses
                      (or (:repair-root observation) repair/default-root)
                      durable-attempt-id))]
      (cond-> []
        (not enumerated?)
        (conj {:kind :unknown-outcome :outcome outcome})
        (and zero-achievement?
             (empty? (set/intersection #{:open :superseded} statuses)))
        (conj {:kind :missing-durable-stop-line
               :attempt-id durable-attempt-id :statuses statuses})))))

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

(defn- t9 [{:keys [transition phase duration-ms phase-budget-ms job-liveness]}]
  (if (= :stalled-job (:kind job-liveness))
    [job-liveness]
    (when (and (= :end transition) (number? duration-ms)
             (number? phase-budget-ms) (pos? phase-budget-ms)
             (> duration-ms (* 2 phase-budget-ms)))
    [{:kind :phase-budget-exceeded :phase phase :duration-ms duration-ms
      :budget-ms phase-budget-ms :multiple (/ (double duration-ms)
                                               phase-budget-ms)}])))

(defn wedge-violations
  "Return a T7 witness when the same unresolved stop-line occupies the last
  three opportunities without any fresh repository artifact. Distinct commits
  are convergent refinement, not a wedge."
  [cohort-history closed-repair-ids]
  (let [recent (vec (take-last 3 cohort-history))
        selected (mapv :selected-stop-line recent)
        repair-id (first selected)
        fresh-commits (vec (keep :fresh-commit recent))
        failure-kinds (vec (keep :failure-kind recent))]
    (when (and (= 3 (count recent)) repair-id
               (apply = selected)
               (not (contains? (set closed-repair-ids) repair-id))
               (empty? fresh-commits))
      [{:kind :consecutive-stop-line-wedge
        :repair/id repair-id
        :attempt-ids (mapv #(or (:attempt-id %) (:attempt/id %)) recent)
        :consecutive-count 3
        :fresh-commits fresh-commits
        :repeated-failure-kind
        (when (and (= 3 (count failure-kinds)) (apply = failure-kinds))
          (first failure-kinds))}])))

(defn- t7 [{:keys [phase transition cohort-history closed-repair-ids]
            :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    (wedge-violations cohort-history closed-repair-ids)))

(def livelock-window-ms
  "How recent the newest finding in a group must be for it to count as the
  machine circling rather than a backlog. 24h."
  (* 24 60 60 1000))

(defn livelock-violations
  "Group unresolved findings by the T8 identity and return groups above K=2.

  Two exclusions, both about not calling different things the same thing.

  A finding whose repair is closed is progress, not repetition. The store is
  append-only, so a resolved finding keeps :repair/status :open in its own
  record and only `effective-statuses` knows better — which is why the caller
  passes CLOSED-REPAIR-IDS rather than reading the finding. Counting closed
  findings had T8 report a livelock over [:artifact-binding-mismatch
  \"M-selected\" nil] on 2026-09-18, where two of the four findings were
  resolved canary fixtures.

  A finding carrying neither :target nor :failed-commit says nothing about
  WHICH thing recurred, and the signature's juxt maps every such finding to
  [kind nil nil]. That grouped an :agent-unavailable opened 2026-07-25 with one
  opened 2026-09-11 and called them the same livelock; 23 of 48 open findings
  had no discriminator at all. Absence of an identifier is not evidence of
  sameness.

  Measured on the live store, 90 findings: 5 groups counting everything, 4
  once closed findings are dropped, 1 once a discriminator is required.

  THIRD EXCLUSION, and it is about time. A livelock is the machine circling
  NOW. This had no time dimension at all: it grouped every unresolved finding
  by signature and fired above two, so the three findings opened between
  02:51 and 04:14 on 2026-09-15 — which WERE a livelock that morning, 84
  minutes apart — went on reporting one four days later with no attempt made in
  between. That is a backlog, and stopping the machine over it (witnesses halt
  since a8ac1615) meant the only route to discharging those findings was a
  repair attempt the halt itself prevented. A wire that blocks the repair it
  demands is not a safety property.

  So a group fires only when its NEWEST member is within `livelock-window-ms`.
  Nothing about the grouping is loosened: the same three findings from
  2026-09-15 still trip this on 2026-09-15.

  The trade, stated: a machine circling more slowly than the window — one
  failure every few days — will not be caught here. That is deliberate. The
  alternative on offer was halting forever on debris, and a backlog of stale
  findings is a thing to work through, not a reason to stop. All 50 open
  findings carry a readable :opened-at (measured 2026-09-19), so nothing is
  silently dropped for want of a timestamp."
  ([findings] (livelock-violations findings #{}))
  ([findings closed-repair-ids]
   (livelock-violations findings closed-repair-ids (System/currentTimeMillis)))
  ([findings closed-repair-ids now-ms]
   (let [closed (set closed-repair-ids)
         opened-ms (fn [f]
                     (try (.toEpochMilli (Instant/parse (str (:opened-at f))))
                          (catch Throwable _ nil)))]
     (->> findings
          (remove #(contains? closed (:repair/id %)))
          (filter #(or (:target %) (:failed-commit %)))
          (group-by (juxt #(or (:failure-kind %) (:repair/class %))
                          :target :failed-commit))
          (keep (fn [[signature records]]
                  (let [newest (when (seq (keep opened-ms records))
                                 (apply max (keep opened-ms records)))]
                    ;; Recency SUPPRESSES a halting wire, so absence of a
                    ;; timestamp must not suppress it: a group nobody can date
                    ;; cannot be shown to be stale, so it still fires. Fixtures
                    ;; without :opened-at take this path; all 50 live findings
                    ;; are datable, so in practice the window decides.
                    (when (and (> (count records) 2)
                               (or (nil? newest)
                                   (<= (- now-ms newest) livelock-window-ms)))
                      (cond-> {:kind :duplicate-finding-livelock
                               :signature signature
                               :repair-ids (mapv :repair/id records)
                               :finding-count (count records)}
                        ;; Present only when there is something to say: an
                        ;; undatable group reports no age rather than a nil one.
                        newest
                        (assoc :newest-opened-at (str (Instant/ofEpochMilli newest))
                               :age-hours (int (/ (- now-ms newest) 3600000.0))))))))
          vec))))

(defn- t8 [{:keys [phase transition findings closed-repair-ids] :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    (livelock-violations findings closed-repair-ids)))

(defn- loaded-definition-lines
  "Line of every public fn AS THE RUNNING IMAGE REPORTS IT.

  Var :line metadata comes from the code that was actually compiled, so this is
  evidence about the image. Everything else in this file that claims to observe
  loaded code observes the file instead."
  [namespace]
  (when-let [loaded (find-ns namespace)]
    (into (sorted-map)
          (keep (fn [[sym var]]
                  (when-let [l (:line (meta var))] [sym l])))
          (ns-publics loaded))))

(defn- file-definition-lines
  "Line of every top-level def form AS THE FILE NOW READS.

  Deliberately textual: the comparison is file-against-image, so this side must
  not be reached through the loaded namespace."
  [path]
  (let [file (io/file path)]
    (when (.isFile file)
      (with-open [r (io/reader file)]
        (into (sorted-map)
              (keep-indexed
               (fn [i line]
                 ;; `(defmethod ...` re-mentions a name the defmulti already
                 ;; defined; counting each method as a fresh definition made
                 ;; the file side report the LAST method's line against the
                 ;; defmulti var's own line -- a manufactured divergence that
                 ;; halted every opportunity-start in the test world (T10 red,
                 ;; ~280 failures, 2026-09-19). Dispatch methods are not
                 ;; definitions; exclude them.
                 (when-let [m (re-find #"^\(def(?!method)\S*\s+(?:\^\S+\s+)*([^\s\[(]+)" line)]
                   [(symbol (second m)) (inc i)])))
              (line-seq r))))))

(defn image-file-divergence
  "Public fns whose loaded line differs from the line the file gives them now.

  This is POSITIVE evidence that the running image is not this file. Comparing
  a file hash against a hash of the same file taken earlier — which is what
  this wire did until 2026-09-19 — cannot distinguish an edit that was reloaded
  from one that was not, so it fired on both. T10 tripped 60 times between
  2026-07-16 and 2026-09-15 and was read as noise; inside that noise, the
  serving JVM ran a runner 28 hours out of date, ignored the author's correct
  commit claim, and opened three build-failed findings that became a livelock.

  Only symbols present on BOTH sides are compared, so a def that is public in
  one and not the other is not mistaken for staleness."
  [namespace path]
  (let [loaded (loaded-definition-lines namespace)
        on-disk (file-definition-lines path)]
    (when (and (seq loaded) (seq on-disk))
      (vec (for [[sym line] loaded
                 :let [disk (get on-disk sym)]
                 :when (and disk (not= line disk))]
             {:fn sym :loaded-line line :file-line disk})))))

(defn foreign-var-roots
  "Public fns in NAMESPACE whose implementation did not come from NAMESPACE.

  A var rebound at runtime -- alter-var-root, with-redefs left in place, a
  half-applied patch -- keeps its :line but its fn class names the namespace
  that actually defined it. That is a mixed image, provable from the image
  alone with no baseline to go stale. Measured clean (zero) across all five
  tracked namespaces on 2026-09-19 before this was adopted as a halting
  signal.

  DYNAMIC vars are excluded: rebinding them with `binding` is the language's
  sanctioned seam, and the runner itself is built around it
  (`*r16-park-fn*`, read at full_loop_runner.clj:195, rebound by the test
  fixture). A thread-local binding under test is not a mixed image; flagging
  it fired T10 on the test world's own declared override point (2026-09-19)."
  [namespace]
  (when-let [loaded (find-ns namespace)]
    (let [prefix (str (munge (name namespace)) "$")]
      (vec (for [[sym v] (ns-publics loaded)
                 :when (and (.isBound ^clojure.lang.Var v)
                            (fn? @v)
                            (not (.isDynamic ^clojure.lang.Var v)))
                 :let [cls (.getName (class @v))]
                 :when (not (str/starts-with? cls prefix))]
             {:fn sym :implemented-by cls})))))

(defn- t10 [{:keys [phase transition] :as observation}]
  (when (or (:tripwire/force? observation)
            (and (= :opportunity phase) (= :start transition)))
    ;; Halt only on what can be PROVEN. A file whose hash moved may have been
    ;; reloaded correctly; divergent definition lines cannot have been. Now
    ;; that a witness stops the run, a wire that fires on suspicion stops it
    ;; for nothing -- and a wire that fires on everything is how a real
    ;; staleness went unread 60 times.
    ;;
    ;; Known gap, stated rather than papered over: an edit that changes a form
    ;; in place without moving any definition line leaves the lines agreeing,
    ;; and this will call that clear.
    (let [stale (into []
                      (keep (fn [[ns-sym path]]
                              (when (find-ns ns-sym)
                                (let [d (seq (image-file-divergence ns-sym path))
                                      f (seq (foreign-var-roots ns-sym))]
                                  (when (or d f)
                                    (cond-> {:namespace ns-sym :source-path path}
                                      d (assoc :divergent-count (count d)
                                               :divergence (vec (take 10 d)))
                                      f (assoc :foreign-roots (vec (take 10 f)))))))))
                      runner-namespace-sources)]
      (when (seq stale)
        [{:kind :loaded-file-code-mismatch :stale stale}]))))

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

(defn- commit-sha [repo commit]
  (when (and (string? repo) (not (str/blank? (str commit))))
    (let [result (shell/sh "git" "-C" repo "rev-parse"
                           (str commit "^{commit}"))]
      (when (zero? (:exit result)) (str/trim (:out result))))))

(defn- commit-time-ms [repo commit]
  (when-let [sha (commit-sha repo commit)]
    (let [result (shell/sh "git" "-C" repo "show" "-s" "--format=%cI" sha)]
      (when (zero? (:exit result))
        (try (.toEpochMilli (Instant/parse (str/trim (:out result))))
             (catch Throwable _ nil))))))

(defn- same-commit? [repo left right]
  (let [left-sha (commit-sha repo left)
        right-sha (commit-sha repo right)]
    (and left-sha right-sha (= left-sha right-sha))))

(defn- t13 [{:keys [phase transition]
             fresh-author? :artifact-binding/fresh-author?
             repo :artifact-binding/repo
             reviewer-commit :artifact-binding/reviewer-commit
             window-start-ms :artifact-binding/author-window-start-ms
             failed-commits :artifact-binding/failed-commits
             :as observation}]
  (when (and fresh-author?
             (or (:cohort? observation) (:tripwire/force? observation))
             (or (:tripwire/force? observation)
                 (and (= :reviewer-dispatch phase) (= :start transition))))
    (let [sha (commit-sha repo reviewer-commit)
          timestamp-ms (when sha (commit-time-ms repo sha))
          predecessor (some #(when (same-commit? repo reviewer-commit %) %)
                            failed-commits)]
      (cond-> []
        (nil? sha)
        (conj {:kind :reviewer-artifact-absent
               :repo repo :reviewer-commit reviewer-commit})

        (and sha (number? window-start-ms)
             (or (nil? timestamp-ms)
                 (< timestamp-ms
                    (- window-start-ms artifact-window-tolerance-ms))))
        (conj {:kind :reviewer-artifact-predates-author-window
               :repo repo :reviewer-commit reviewer-commit
               :commit-time-ms timestamp-ms
               :author-window-start-ms window-start-ms})

        predecessor
        (conj {:kind :reviewer-artifact-is-failed-predecessor
               :repo repo :reviewer-commit reviewer-commit
               :failed-commit predecessor})))))

(def wire-evaluators
  {:T1 t1 :T2 t2 :T3 t3 :T4 t4 :T5 t5 :T6 t6 :T7 t7 :T8 t8
   :T9 t9 :T10 t10 :T11 t11 :T12 t12 :T13 t13})

(defn evaluate-wire
  "Return this wire's violation witnesses for one complete observation."
  [wire-id observation]
  (if-let [evaluate (get wire-evaluators wire-id)]
    (vec (or (evaluate observation) []))
    (throw (ex-info "Unknown tripwire id" {:wire-id wire-id}))))

(def ^:private observation-node-budget
  "Collection nodes one observation entry may carry into a durable trip report.
  See `durable-observation` for why the disk copy is bounded at all."
  256)

(defn- within-node-budget?
  "True when V holds at most `observation-node-budget` collection nodes.

  The walk stops as soon as the budget is passed, so its cost is the budget
  rather than the size of V.  That matters: V is routinely the whole repair
  store, and measuring it by serialising it would reintroduce the cost this
  bound exists to avoid."
  [v]
  (loop [stack (list v) n 0]
    (cond
      (> n observation-node-budget) false
      (empty? stack) true
      :else (let [x (first stack) more (rest stack)]
              (cond
                (map? x) (recur (into more cat x) (inc n))
                (coll? x) (recur (into more x) (inc n))
                :else (recur more n))))))

(defn- describe-elided
  "Stand in for an observation entry too large to store, saying what was there."
  [v]
  (cond-> {:elided/reason :exceeds-durable-trip-report-budget
           :elided/type (cond (map? v) :map
                              (vector? v) :vector
                              (set? v) :set
                              (sequential? v) :seq
                              :else :value)}
    (coll? v) (assoc :elided/count (bounded-count 100000 v))
    (map? v) (assoc :elided/keys (vec (take 64 (sort-by str (keys v)))))))

(defn- durable-observation
  "Project OBSERVATION down to what a durable trip report should carry.

  `cross-run-observation` folds the entire repair store into the observation at
  :opportunity/:start, and `observe!` then writes a full copy of that
  observation into a separate trip report for every wire that fires.  On
  2026-09-18 that was 91 MB per report, five reports per click, at ~3m43s each
  — about 19 minutes of a click spent serialising the machine's own history.

  Nothing reads `:trip/observation` back from disk.  The in-code consumers
  (`record-finding!`, `handle-action!`) are handed the in-memory report, which
  is left whole; only the disk copy is bounded.  Small entries are kept
  verbatim so a report stays legible; large ones are replaced by a description
  of what stood there, so the elision is visible rather than silent."
  [observation]
  (if-not (map? observation)
    observation
    (reduce-kv (fn [m k v]
                 (assoc m k (if (within-node-budget? v) v (describe-elided v))))
               {}
               observation)))

(defn write-trip-report!
  "Durably create one append-only EDN trip report. CREATE_NEW forbids rewrite."
  ([report] (write-trip-report! default-trip-root report))
  ([root report]
   (store-lock/with-store-lock-for root
    (fn []
     (let [id (or (:trip/id report) (str "trip-" (UUID/randomUUID)))
         path (io/file root (str id ".edn"))
         record (cond-> (merge {:trip/id id :trip/schema-version 1
                                :trip/recorded-at (str (Instant/now))}
                               report)
                  (contains? report :trip/observation)
                  (update :trip/observation durable-observation))]
     (io/make-parents path)
     ;; pr-str, not pprint: this file is read by machines, and the layout
     ;; engine was the dominant cost of writing it.
     (Files/write (.toPath path)
                  (.getBytes (pr-str record) "UTF-8")
                  (into-array StandardOpenOption
                              [StandardOpenOption/CREATE_NEW
                               StandardOpenOption/WRITE]))
       (.getPath path))))))

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
    (set (map #(if (keyword? %) (name %) (str %)) (roster-fn opts)))
    (let [response (http/get (str (agency-base opts) "/api/alpha/agents")
                             {:timeout 10000 :throw false})
          body (successful-response! :tripwire-roster response)]
      (set (map #(if (keyword? %) (name %) (str %))
                (keys (:agents body)))))))

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
          (handle-action! opts report report-path)
          {:report report :report-path report-path})
        (catch Throwable e
          (stderr! "trip report action failed; runner remains untouched" e))))))

(defn check!
  "Run one interoceptive check at the R20 boundary.  A violation returns a
  refusing result and durably records the discharge that the bulletin reads;
  a clear check returns an explicit pass.  Unlike `observe!`, this is an
  admission boundary, so persistence failure is allowed to refuse the call."
  [opts wire-id observation]
  (let [witnesses (evaluate-wire wire-id observation)]
    (if (empty? witnesses)
      {:node :R20 :tripwire/check :passed :trip/wire-id wire-id}
      (let [trip-id (or (:trip/id observation) (str "trip-" (UUID/randomUUID)))
            discharge {:node :R20
                       :tripwire/check :refused
                       :trip/wire-id wire-id
                       :trip/id trip-id
                       :status :needs-joe
                       :class :J
                       :statement (str "R20 tripwire " (name wire-id)
                                       " refused the transition")
                       :blocker "Tripwire refusal requires investigation and discharge"
                       :trip/witnesses witnesses}
            report-path (write-trip-report!
                         (or (:tripwire/report-root opts) default-trip-root)
                         (assoc discharge :trip/action :discharge
                                          :trip/observation observation))]
        (assoc discharge :trip/report-path report-path)))))

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
          ;; Store invariant: every record admitted under resolutions/ or
          ;; dismissals/ is a closing disposition by construction of the
          ;; repair store's verbs. Closure therefore follows directory
          ;; membership, not an ever-growing enumeration of status keywords.
          closed-ids (into #{}
                           (keep (fn [[path {:keys [record]}]]
                                   (when (or (str/starts-with? path "resolutions/")
                                             (str/starts-with? path "dismissals/"))
                                     (:repair/id record))))
                           repair-state)
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
                                             (:selected-mission attempt))
                                finding (some #(when (= selected (:repair/id %))
                                                 %)
                                              findings)]
                            {:attempt-id (or (:attempt-id attempt)
                                             (:attempt/id attempt))
                             :selected-stop-line
                             (when (contains? finding-ids selected) selected)
                             :fresh-commit (:fresh-commit attempt)
                             :artifact-binding (:artifact-binding attempt)
                             :failure-kind (:failure-kind finding)}))
                        attempts)]
      (merge observation
             {:cohort-history history
              :closed-repair-ids closed-ids
              :findings findings
              :a-matrix-events (vec a-matrix-events)
              :grounding-witnesses grounding-witnesses}
             (:tripwire/cross-run-snapshot opts)))
    observation))

(def ^:dynamic *halt-on-witness?*
  "One tripwire, one shutdown (Joe, 2026-09-18) — made OPT-IN on 2026-09-19
  (Joe's ruling in T-wm-excessive-guardrails-19092026: any security system
  that gets in the way of a real run is not wanted while the machine is being
  tuned; what is wanted is validation that a run is real). Default: a witness
  is recorded durably and announced on stderr, and the run continues.
  `FUTON_WM_TRIPWIRE_HALT=1` restores the 09-18 stop-the-run behavior.
  Dynamic binding exists for tests."
  (= "1" (System/getenv "FUTON_WM_TRIPWIRE_HALT")))

(defonce ^:private !noted-trips
  ;; Run-scoped dedupe for note!: a run that continues past a witness must not
  ;; re-record the identical witness at every later phase transition — that
  ;; would resurrect the 2026-09-18 pile-up (205 copies of one complaint) in
  ;; the non-halting mode. Keyed [run-id wire-id witness-hash]; reset wholesale
  ;; if it ever grows past 1024 keys (the cost of losing it is one duplicate
  ;; report, not correctness).
  (atom #{}))

(defn- note!
  "Record a witness durably and let the run continue (the default since
  2026-09-19). Same durable artifact as a halt, same stderr announcement,
  no throw: the witness is evidence for repair selection and for the
  operator, not a veto. First occurrence per [run wire witness] writes;
  repeats within the run are silent."
  [opts record {:keys [wire-id witness observation]}]
  (let [title (get-in @wire-registry [wire-id :title])
        run-key [(:run-id opts) wire-id (hash witness)]]
    (when-not (contains? @!noted-trips run-key)
      (swap! !noted-trips (fn [s] (conj (if (> (count s) 1024) #{} s) run-key)))
      (let [recorded (record-trip! opts {:trip/wire-id wire-id
                                         :trip/witness witness
                                         :trip/observation observation})]
        (stderr! (str "TRIP " (name wire-id) " (" title ") at phase "
                      (:phase record) "/" (:transition record)
                      " — recorded; run continues"
                      " (FUTON_WM_TRIPWIRE_HALT=1 restores one-trip-one-shutdown)") nil)
        (stderr! (str "  witness: " (pr-str witness)) nil)
        (stderr! (str "  report:  " (or (:report-path recorded) "(not written)")) nil)))))

(defn- halt!
  "Record the trip, say plainly what tripped, and stop the run.

  One tripwire equals one shutdown (Joe, 2026-09-18). The wires exist to say a
  machine invariant is broken; carrying on past that produced runs whose output
  was a pile of repetitions of the same complaint. On 2026-09-18 one click wrote
  five reports for five T8 witnesses of a single kind, at ~3m43s each, and then
  failed anyway. One report, one message, one stop is more useful than all of
  it."
  [opts record {:keys [wire-id witness observation]}]
  (let [title (get-in @wire-registry [wire-id :title])
        recorded (record-trip! opts {:trip/wire-id wire-id
                                     :trip/witness witness
                                     :trip/observation observation})
        report-path (:report-path recorded)]
    (stderr! (str "HALT " (name wire-id) " (" title ") tripped at phase "
                  (:phase record) "/" (:transition record)) nil)
    (stderr! (str "  witness: " (pr-str witness)) nil)
    (stderr! (str "  report:  " (or report-path "(not written)")) nil)
    (stderr! "  the run is stopped; fix the condition above" nil)
    (throw (ex-info (str "War Machine tripwire " (name wire-id) " tripped: " title)
                    {:outcome :tripwire-tripped
                     :failure-kind :tripwire-tripped
                     :failure-stage (:phase record)
                     :tripwire/wire-id wire-id
                     :tripwire/title title
                     :tripwire/witness witness
                     :tripwire/report report-path}))))

(def ^:private pre-selection-phase-start?
  "The runner's phases at-or-before selection: :opportunity/:start (emitted
  before anything else), :stop-line-memory/:start (before the ledger is read)
  and :selection/:start (before the judge runs). A witness deferred here
  leaves the run able to reach the stop-line repair; the same witness at any
  later phase start, or at an :end, still halts."
  (fn [record]
    (and (= :start (:transition record))
         (contains? #{:opportunity :stop-line-memory :selection} (:phase record)))))

(defn repair-covered-witness?
  "True when a witness names open repair obligations (the `:repair-ids` shape,
  today only T8's livelock witness) that are ALL still live in the ledger and
  at least one is repair-selectable — open and not an environmental hold, the
  same filter the runner's :stop-line-memory phase uses to put an obligation in
  front of selection as a :repair-machine-failure stop-line action.

  Such a witness is the ledger talking about itself. Halting on it at a
  pre-selection phase start removes the only route to discharging those very
  obligations — the four-day standstill of 2026-09-15..19, where three stale
  findings tripped T8 at :opportunity/:start and the demanded repair could
  never be selected. Deliberately NOT applied to the T7 wedge witness
  (:repair/id, a single obligation already selected three times with no fresh
  artifact): there the evidence is that the repair itself is wedged, and
  re-running it unchanged is circling, not discharge."
  [witness {:keys [findings closed-repair-ids]}]
  (when-let [ids (seq (:repair-ids witness))]
    (let [by-id (into {} (map (fn [f] [(:repair/id f) f])) findings)
          closed (set closed-repair-ids)
          repair-class (fn [f] (if (= :system-actuation-failure (:repair/class f))
                                 :machine-failure (:repair/class f)))
          live-ids (filterv (fn [id] (and (contains? by-id id)
                                          (not (contains? closed id))))
                            ids)]
      (and (= (count live-ids) (count ids))
           (boolean (some (fn [id]
                            (let [f (get by-id id)]
                              (and (= :open (:repair/status f))
                                   (not= :environmental-hold (repair-class f)))))
                          live-ids))))))

(defn- defer!
  "Record a repair-covered witness WITHOUT halting, so the run can select and
  enact the repair the witness demands. The trip is still written durably
  (append-only report, :trip/action :deferred-to-repair) and announced on
  stderr; it is visible, never silent. No stop-line park, no bell, no
  handle-action!: the obligation the witness names already exists in the
  ledger — that is the coverage condition — so the durable record here is
  evidence, not a second obligation."
  [opts record {:keys [wire-id witness observation]}]
  (let [title (get-in @wire-registry [wire-id :title])]
    (try
      (write-trip-report! (or (:tripwire/report-root opts) default-trip-root)
                          {:trip/wire-id wire-id
                           :trip/witness witness
                           :trip/observation observation
                           :trip/action :deferred-to-repair
                           :trip/deferred-repair-ids (vec (:repair-ids witness))})
      (catch Throwable e
        (stderr! "deferred trip report failed; the run still continues to repair" e)))
    (stderr! (str "DEFER " (name wire-id) " (" title ") at phase "
                  (:phase record) "/" (:transition record)
                  ": witness names open repair obligations; continuing so their repair can be selected") nil)
    (stderr! (str "  witness: " (pr-str witness)) nil))
  record)

(defn observe!
  "Evaluate enabled wires; a witness is RECORDED, and by default nothing stops.

  Two failures are deliberately kept apart. A wire that THROWS is a bug in the
  wire, and must not take the runner with it — that is why this seam was total
  to begin with, and it stays total for that case. A wire that YIELDS A WITNESS
  has found a broken machine invariant; what happens next is governed by
  `*halt-on-witness?*`:

  - default (since Joe's 2026-09-19 ruling, T-wm-excessive-guardrails-19092026):
    the witness is written durably and announced, and the run continues —
    evidence for repair selection and the operator, not a veto. Identical
    witnesses within one run are recorded once (`note!`), which preserves the
    09-18 anti-pile-up property (one report, not 205) without the shutdown.
  - `FUTON_WM_TRIPWIRE_HALT=1`: one tripwire, one shutdown (Joe, 2026-09-18),
    except that at a pre-selection phase start a witness whose evidence is a
    set of live, repair-selectable open obligations is DEFERRED, not thrown —
    see `repair-covered-witness?` — so the run can reach the repair the
    witness demands.

  Evaluation stops at the first witness: `for` is lazy and `first` realises one
  element, so no later wire runs and no second report is written. Returns
  `record` identically when nothing tripped."
  [opts record]
  (let [tripped
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
            (first (for [[wire-id _] @wire-registry
                         :when (enabled? opts wire-id)
                         witness (evaluate-wire wire-id observation)]
                     {:wire-id wire-id
                      :witness witness
                      :observation observation})))
          (catch Throwable e
            (stderr! "wire evaluation failed; runner remains untouched" e)
            nil))]
    (when tripped
      (cond
        (and (pre-selection-phase-start? record)
             (repair-covered-witness? (:witness tripped) (:observation tripped)))
        (defer! opts record tripped)

        *halt-on-witness?*
        (halt! opts record tripped)

        :else
        (note! opts record tripped)))
    record))
