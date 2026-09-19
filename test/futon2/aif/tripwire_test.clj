(ns futon2.aif.tripwire-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.time Instant]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "wm-tripwire-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- halted!
  "Run `observe!` on a record expected to trip, returning the halt's ex-data.

  A witness stops the run as of 2026-09-18: one tripwire, one shutdown. The
  cases below construct a tripping observation deliberately, so the throw is
  the expected path — what they actually assert is what the trip recorded on
  its way out."
  [opts record]
  (let [data (try
               (tripwire/observe! opts record)
               ::no-halt
               (catch clojure.lang.ExceptionInfo e
                 (ex-data e)))]
    (is (not= ::no-halt data) "expected the tripwire to halt the run")
    (is (= :tripwire-tripped (:failure-kind data)))
    data))

(defn- write-edn! [root child filename value]
  (let [file (io/file root child filename)]
    (io/make-parents file)
    (spit file (pr-str value))
    file))

(defn- synthetic-trip-record []
  {:phase :synthetic
   :attempt-id "action-test"
   :tripwire/snapshot {:runner/dispatched-turns 2
                       :agency/dispatch-count 1}})

(deftest production-repair-root-is-unreachable-during-tripwire-suite
  (is (not= hermetic/production-repair-root repair/default-root))
  (is (not= hermetic/production-trip-root tripwire/default-trip-root)))

(deftest t1-turn-conservation-trips-on-ledger-drift
  (is (= [{:kind :turn-conservation
           :runner/dispatched-turns 2
           :agency/dispatch-count 1}]
         (tripwire/evaluate-wire :T1
                                 {:runner/dispatched-turns 2
                                  :agency/dispatch-count 1}))))

(deftest t2-ledger-closure-trips-on-missing-job-and-commit
  (let [violations
        (tripwire/evaluate-wire
         :T2 {:referenced-job-ids ["author" "reviewer"]
              :agency/job-ids #{"author"}
              :referenced-commits [{:repo "/missing" :sha "bad"}]})]
    (is (= [:missing-agency-jobs :missing-commits]
           (mapv :kind violations)))
    (is (= ["reviewer"] (:job-ids (first violations))))))

(deftest t3-exit-completeness-rereads-durable-findings
  (let [root (temp-dir)
        observation {:phase :opportunity :transition :end
                     :outcome :incomplete :attempt-id "attempt-T3"
                     :repair-root (.getPath root) :tripwire/force? true}]
    (testing "a zero-achievement exit without an on-disk finding trips"
      (is (= [:missing-durable-stop-line]
             (mapv :kind (tripwire/evaluate-wire :T3 observation)))))
    (write-edn! root "findings" "repair-T3.edn"
                {:repair/id "repair-T3" :repair/status :open
                 :attempt-id "attempt-T3"})
    (testing "the same exit clears only after the finding is re-read"
      (is (empty? (tripwire/evaluate-wire :T3 observation))))
    (testing "an outcome outside the cohort alphabet independently trips"
      (is (= :unknown-outcome
             (:kind (first (tripwire/evaluate-wire
                            :T3 (assoc observation :outcome :surprise)))))))))

(deftest t3-prefers-qualified-external-attempt-identity
  (let [root (temp-dir)
        _ (repair/record-system-failure!
           (.getPath root)
           {:attempt-id "run4-a--attempt-001"
            :repair-class :environmental-hold
            :failure-stage :author-readiness
            :failure-kind :agent-unavailable
            :outcome :agent-unavailable
            :error "fixture"})
        observation {:phase :opportunity :transition :end
                     :outcome :agent-unavailable :cohort? true
                     :attempt-id "attempt-001"
                     :external-attempt-id "run4-a--attempt-001"
                     :repair-root (.getPath root)}]
    (is (empty? (tripwire/evaluate-wire :T3 observation)))))

(deftest t4-a-matrix-provenance-trips-without-grounding-witness
  (let [event {:event-id "qa-1" :entity-id "impl-1"}
        violations (tripwire/evaluate-wire
                    :T4 {:a-matrix-events [event]
                         :grounding-witnesses []})]
    (is (= [:belief-witness-count-mismatch
            :belief-event-without-grounding]
           (mapv :kind violations)))
    (is (empty? (tripwire/evaluate-wire
                 :T4 {:a-matrix-events [event]
                      :grounding-witnesses [{:implementation-id "impl-1"}]})))))

(deftest t5-review-commit-binding-trips-on-different-sha
  (let [sha "0123456789abcdef"
        base {:grounded-commit sha :tripwire/force? true}]
    (is (= :review-grounding-commit-mismatch
           (:kind (first (tripwire/evaluate-wire
                          :T5 (assoc base :reviewer-job
                                    {:job-id "review"
                                     :prompt "Review fedcba9876543210"}))))))
    (is (empty? (tripwire/evaluate-wire
                 :T5 (assoc base :reviewer-job
                            {:job-id "review"
                             :events [{:type "prompt"
                                       :text (str "Review commit " sha)}]}))))))

(deftest t6-append-only-repair-store-trips-on-rewrite
  (let [before {"findings/r.edn"
                {:sha256 "old" :record {:repair/id "r" :repair/status :open}}}
        after {"findings/r.edn"
               {:sha256 "new" :record {:repair/id "r" :repair/status :open}}}]
    (is (= [:repair-record-mutated]
           (mapv :kind (tripwire/evaluate-wire
                        :T6 {:repair-before before :repair-after after}))))))

(deftest t6-status-lattice-trips-on-invalid-edge
  (let [before {"findings/r.edn"
                {:sha256 "same" :record {:repair/id "r" :repair/status :open}}}
        after (assoc before "resolutions/r.edn"
                     {:sha256 "resolution"
                      :record {:repair/id "r" :repair/status :resolved}})]
    (is (= [{:repair/id "r" :from :open :to :resolved}]
           (:edges (first (tripwire/evaluate-wire
                           :T6 {:repair-before before :repair-after after})))))))

(deftest t9-wall-clock-trips-beyond-double-budget
  (is (= [{:kind :phase-budget-exceeded :phase :author-wait
           :duration-ms 2001 :budget-ms 1000 :multiple 2.001}]
         (tripwire/evaluate-wire :T9 {:phase :author-wait :transition :end
                                     :duration-ms 2001 :phase-budget-ms 1000}))))

(deftest t7-no-new-work-triple-still-trips
  (let [history [{:attempt-id "a1" :selected-stop-line "repair-x"
                  :failure-kind :construction-failed}
                 {:attempt-id "a2" :selected-stop-line "repair-x"
                  :failure-kind :construction-failed}
                 {:attempt-id "a3" :selected-stop-line "repair-x"
                  :failure-kind :construction-failed}]]
    (is (= :consecutive-stop-line-wedge
           (:kind (first (tripwire/evaluate-wire
                          :T7 {:cohort-history history
                               :closed-repair-ids #{}
                               :tripwire/force? true})))))
    (is (= :construction-failed
           (:repeated-failure-kind
            (first (tripwire/evaluate-wire
                    :T7 {:cohort-history history
                         :closed-repair-ids #{}
                         :tripwire/force? true})))))
    (is (empty? (tripwire/evaluate-wire
                 :T7 {:cohort-history history
                      :closed-repair-ids #{"repair-x"}
                      :tripwire/force? true})))))

(deftest t7-run-6-7-8-convergent-lineage-does-not-trip
  (let [history [{:attempt-id "attempt-014"
                  :selected-stop-line "repair-attempt-010"
                  :fresh-commit nil}
                 {:attempt-id "attempt-015"
                  :selected-stop-line "repair-attempt-010"
                  :fresh-commit "2b912f04fa16c"}
                 {:attempt-id "attempt-016"
                  :selected-stop-line "repair-attempt-010"
                  :fresh-commit "bf14dcafc585"}]]
    (is (empty? (tripwire/evaluate-wire
                 :T7 {:cohort-history history
                      :closed-repair-ids #{}
                      :tripwire/force? true})))))

(deftest t8-livelock-trips-on-third-duplicate-finding
  (let [finding (fn [id] {:repair/id id :failure-kind :review-rejected
                          :target "M-x" :failed-commit "abc"})]
    (is (= :duplicate-finding-livelock
           (:kind (first (tripwire/evaluate-wire
                          :T8 {:findings (mapv finding ["r1" "r2" "r3"])
                               :tripwire/force? true})))))
    (is (empty? (tripwire/evaluate-wire
                 :T8 {:findings (mapv finding ["r1" "r2"])
                      :tripwire/force? true})))))

(deftest t8-witness-defers-to-repair-at-pre-selection-start
  ;; 2026-09-15..19 standstill class: a T8 witness over three live, open,
  ;; repair-selectable findings at :opportunity/:start must NOT halt the run —
  ;; halting there removes the only route to discharging those findings.
  (let [root (temp-dir)
        report-root (temp-dir)
        now (str (Instant/now))
        finding (fn [id]
                  {:repair/id id :repair/status :open
                   :failure-kind :review-rejected
                   :target "M-x" :failed-commit "abc"
                   :opened-at now})
        opts {:repair-root (.getPath root)
              :tripwire/report-root (.getPath report-root)
              :cohort? true
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))}
        record {:phase :opportunity :transition :start
                :attempt-id "attempt-defer"}]
    (doseq [id ["r1" "r2" "r3"]]
      (write-edn! root "findings" (str id ".edn") (finding id)))
    (testing "the witness exists (T8 fires on the three findings)"
      (is (= :duplicate-finding-livelock
             (:kind (first (tripwire/evaluate-wire
                            :T8 {:findings (mapv finding ["r1" "r2" "r3"])
                                 :tripwire/force? true}))))))
    (testing "observe! defers instead of halting and returns the record"
      (is (identical? record (tripwire/observe! opts record))))
    (testing "the deferral is durably recorded, visibly"
      (let [reports (->> (file-seq report-root)
                         (filter #(str/ends-with? (.getPath %) ".edn"))
                         (mapv #(edn/read-string (slurp %))))]
        (is (= 1 (count reports)) "one deferred report, not one per phase")
        (is (= :deferred-to-repair (:trip/action (first reports))))
        (is (= [:T8] (mapv :trip/wire-id reports)))
        (is (= #{"r1" "r2" "r3"}
               (set (:trip/deferred-repair-ids (first reports)))))))))

(deftest t8-witness-still-halts-when-no-repair-route-exists
  ;; Deferral requires every named obligation live AND at least one
  ;; repair-selectable. All-environmental-hold findings have no stop-line
  ;; repair route, so the same witness still stops the run.
  (let [root (temp-dir)
        now (str (Instant/now))
        finding (fn [id]
                  {:repair/id id :repair/status :open
                   :repair/class :environmental-hold
                   :failure-kind :review-rejected
                   :target "M-x" :failed-commit "abc"
                   :opened-at now})
        opts {:repair-root (.getPath root)
              :tripwire/report-root (.getPath (temp-dir))
              :cohort? true
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))}
        record {:phase :opportunity :transition :start
                :attempt-id "attempt-hold"}]
    (doseq [id ["r1" "r2" "r3"]]
      (write-edn! root "findings" (str id ".edn") (finding id)))
    (let [data (halted! opts record)]
      (is (= :T8 (:tripwire/wire-id data))))))

(deftest deferral-is-pre-selection-only
  ;; The same repair-covered witness at a phase start after selection (or at
  ;; any :end) still halts: deferral buys the repair route, nothing else.
  ;; The findings ride the record directly (the cross-run snapshot only loads
  ;; at :opportunity/:start), and :tripwire/force? makes T8 evaluate wherever
  ;; the phase event lands.
  (let [now (str (Instant/now))
        finding (fn [id]
                  {:repair/id id :repair/status :open
                   :failure-kind :review-rejected
                   :target "M-x" :failed-commit "abc"
                   :opened-at now})
        findings (mapv finding ["r1" "r2" "r3"])
        opts {:tripwire/report-root (.getPath (temp-dir))
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))}
        base {:attempt-id "attempt-late"
              :tripwire/force? true
              :findings findings
              :closed-repair-ids #{}}]
    (testing "a construction-phase start halts"
      (let [data (halted! opts (merge base {:phase :construction
                                            :transition :start}))]
        (is (= :T8 (:tripwire/wire-id data)))))
    (testing "a selection :end halts"
      (let [data (halted! opts (merge base {:phase :selection
                                            :transition :end}))]
        (is (= :T8 (:tripwire/wire-id data)))))))

(deftest t10-trips-on-a-var-replaced-at-runtime
  ;; The old wire simulated a mixed image by rebinding a var and noticing the
  ;; fn's class name changed against a stored baseline. Same evidence, no
  ;; baseline: a public fn whose class does not name its own namespace did not
  ;; come from that namespace's file.
  (let [original @#'repair/open-obligations]
    (try
      (alter-var-root #'repair/open-obligations
                      (constantly (fn [& _] [:synthetic-mixed-image])))
      (is (= :loaded-file-code-mismatch
             (:kind (first (tripwire/evaluate-wire
                            :T10 {:tripwire/force? true})))))
      (is (seq (tripwire/foreign-var-roots 'futon2.aif.repair-obligation)))
      (finally
        (alter-var-root #'repair/open-obligations
                        (constantly original))))
    (testing "and goes clear once the image matches its files again"
      (is (empty? (tripwire/foreign-var-roots 'futon2.aif.repair-obligation)))
      (is (empty? (tripwire/evaluate-wire :T10 {:tripwire/force? true}))))))

(deftest t11-job-alphabet-trips-on-unknown-state-and-bad-time
  (let [violations
        (tripwire/evaluate-wire :T11
                                {:job-snapshot {:job-id "j1" :state "mystery"
                                                :created-at "yesterday"}})]
    (is (= [:unknown-job-state :unparseable-job-time]
           (mapv :kind violations)))))

(deftest record-action-is-create-new-and-parseable
  (let [root (temp-dir)
        path (tripwire/write-trip-report!
              (.getPath root)
              {:trip/id "trip-fixed" :trip/wire-id :T1
               :trip/action :record :trip/witness {:drift 1}})]
    (is (= :T1 (:trip/wire-id (edn/read-string (slurp path)))))
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (tripwire/write-trip-report!
                  (.getPath root) {:trip/id "trip-fixed" :trip/wire-id :T1})))))

(deftest record-action-attempts-no-stop-line-park-or-bell-effects
  (let [record (synthetic-trip-record)
        reports (atom [])
        effects (atom [])
        opts {:tripwire/action :record
              :tripwire/report-writer
              (fn [report] (swap! reports conj report) "/tmp/trip-record.edn")
              :tripwire/repair-record-fn
              (fn [_] (swap! effects conj :repair))
              :tripwire/roster-fn
              (fn [_] (swap! effects conj :roster) #{"claude-6"})
              :tripwire/park-fn
              (fn [& _] (swap! effects conj :park))
              :tripwire/bell-fn
              (fn [& _] (swap! effects conj :bell))}]
    (halted! opts record)
    (is (= 1 (count @reports)))
    (is (empty? @effects)
        ":record must not even attempt any escalation-shaped effect")))

(deftest stop-line-action-records-typed-finding-linked-to-report
  (let [finding (atom nil)
        opts {:tripwire/action :stop-line
              :tripwire/report-writer (fn [_] "/tmp/trip-stop.edn")
              :tripwire/repair-record-fn #(reset! finding %)}]
    (halted! opts (synthetic-trip-record))
    (is (= :machine-failure (:repair-class @finding)))
    (is (= :invariant-tripped (:failure-kind @finding)))
    (is (= :T1 (get-in @finding [:failure-data :trip/wire-id])))
    (is (string? (get-in @finding [:failure-data :trip/id])))
    (is (= "/tmp/trip-stop.edn" (get-in @finding [:backtrace :trip-report])))))

(deftest r20-check-returns-a-refusal-result
  ;; Live record 0a18c4f7-R20.edn pins these values verbatim: :status :absent,
  ;; :reason :no-record-field.  The new boundary replaces that recorded absence.
  (let [root (temp-dir)
        result (tripwire/check! {:tripwire/report-root (.getPath root)}
                                :T1
                                {:trip/id "0a18c4f7-R20-test"
                                 :runner/dispatched-turns 2
                                 :agency/dispatch-count 1})]
    (is (= {:node :R20 :tripwire/check :refused :status :needs-joe}
           (select-keys result [:node :tripwire/check :status])))
    (is (= :turn-conservation
           (-> result :trip/witnesses first :kind)))))

(deftest park-and-summon-builds-background-join-and-roster-checked-bell
  (let [finding (atom nil)
        park (atom nil)
        bell (atom nil)
        opts {:tripwire/action :park-and-summon
              :tripwire/report-writer (fn [_] "/tmp/trip-summon.edn")
              :tripwire/repair-record-fn #(reset! finding %)
              :tripwire/roster-fn (fn [_] #{"claude-6" "codex-7"})
              :tripwire/park-fn (fn [_ payload] (reset! park payload))
              :tripwire/bell-fn (fn [_ payload] (reset! bell payload))}]
    (halted! opts (synthetic-trip-record))
    (is (= :invariant-tripped (:failure-kind @finding)))
    (is (= {:agent "claude-6" :surface "emacs-repl" :mode :background}
           (select-keys @park [:agent :surface :mode])))
    (is (= 1 (count (:awaiting @park))))
    (is (str/ends-with? (first (:awaiting @park)) "-investigation"))
    (is (= "claude-6" (:agent-id @bell)))
    (is (re-find #"investigate then discharge or revise the wire"
                 (:prompt @bell)))))

(deftest summon-roster-accepts-real-agents-map-shape
  (let [effects (atom [])
        opts {:tripwire/action :park-and-summon
              :tripwire/report-writer (fn [_] "/tmp/trip-real-roster.edn")
              :tripwire/repair-record-fn (fn [_] (swap! effects conj :repair))
              :tripwire/park-fn (fn [& _] (swap! effects conj :park))
              :tripwire/bell-fn (fn [& _] (swap! effects conj :bell))}]
    (with-redefs [http/get
                  (fn [& _]
                    {:status 200
                     :body (json/generate-string
                            {:ok true
                             :agents {"claude-6" {:status "connected"}}})})]
      (halted! opts (synthetic-trip-record)))
    (is (= [:repair :park :bell] @effects))))

(deftest stop-line-failure-degrades-to-record-without-escalating
  (let [record (synthetic-trip-record)
        effects (atom [])
        err (java.io.StringWriter.)
        opts {:tripwire/action :park-and-summon
              :tripwire/report-writer (fn [_] "/tmp/trip-degraded.edn")
              :tripwire/repair-record-fn
              (fn [_] (throw (ex-info "repair store unavailable" {})))
              :tripwire/roster-fn
              (fn [_] (swap! effects conj :roster) #{"claude-6"})
              :tripwire/park-fn
              (fn [& _] (swap! effects conj :park))
              :tripwire/bell-fn
              (fn [& _] (swap! effects conj :bell))}]
    (binding [*err* err]
      (halted! opts record))
    (is (empty? @effects))
    (is (str/includes? (str err) "degraded to durable :record"))))

(deftest park-failure-degrades-to-existing-stop-line-without-bell
  (let [effects (atom [])
        err (java.io.StringWriter.)
        opts {:tripwire/action :park-and-summon
              :tripwire/report-writer (fn [_] "/tmp/trip-park-fail.edn")
              :tripwire/repair-record-fn
              (fn [_] (swap! effects conj :repair))
              :tripwire/roster-fn (fn [_] #{"claude-6"})
              :tripwire/park-fn
              (fn [& _] (swap! effects conj :park)
                (throw (ex-info "park unavailable" {})))
              :tripwire/bell-fn
              (fn [& _] (swap! effects conj :bell))}]
    (binding [*err* err]
      (halted! opts (synthetic-trip-record)))
    (is (= [:repair :park] @effects))
    (is (str/includes? (str err) "degraded to :stop-line"))))

(deftest bell-failure-degrades-to-existing-stop-line-without-escape
  (let [record (synthetic-trip-record)
        effects (atom [])
        err (java.io.StringWriter.)
        opts {:tripwire/action :park-and-summon
              :tripwire/report-writer (fn [_] "/tmp/trip-bell-fail.edn")
              :tripwire/repair-record-fn
              (fn [_] (swap! effects conj :repair))
              :tripwire/roster-fn (fn [_] #{"claude-6"})
              :tripwire/park-fn (fn [& _] (swap! effects conj :park))
              :tripwire/bell-fn
              (fn [& _] (swap! effects conj :bell)
                (throw (ex-info "bell unavailable" {})))}]
    (binding [*err* err]
      (halted! opts record))
    (is (= [:repair :park :bell] @effects))
    (is (str/includes? (str err) "degraded to :stop-line"))))

(deftest t12-is-chartered-disabled-and-inert
  (is (= {:title "four-opportunity zero-grounding target wedge"
          :enabled? false :status :chartered-stub}
         (get @tripwire/wire-registry :T12)))
  (is (empty? (tripwire/evaluate-wire :T12 {:tripwire/force? true}))))

(def run-5b-repo "/home/joe/code/futon5a")

(defn- instant-ms [timestamp]
  (.toEpochMilli (java.time.Instant/parse timestamp)))

(deftest t13-trips-when-reviewer-artifact-is-absent-from-target-repo
  (is (= [{:kind :reviewer-artifact-absent
           :repo run-5b-repo :reviewer-commit "ffffffffffff"}]
         (tripwire/evaluate-wire
          :T13 {:tripwire/force? true
                :artifact-binding/fresh-author? true
                :artifact-binding/repo run-5b-repo
                :artifact-binding/reviewer-commit "ffffffffffff"
                :artifact-binding/author-window-start-ms
                (instant-ms "2026-07-16T12:00:13Z")
                :artifact-binding/failed-commits []}))))

(deftest t13-trips-when-reviewer-artifact-predates-author-window
  (is (= :reviewer-artifact-predates-author-window
         (-> (tripwire/evaluate-wire
              :T13 {:tripwire/force? true
                    :artifact-binding/fresh-author? true
                    :artifact-binding/repo run-5b-repo
                    :artifact-binding/reviewer-commit "c9e7aaf"
                    :artifact-binding/author-window-start-ms
                    (instant-ms "2026-07-16T12:00:13Z")
                    :artifact-binding/failed-commits []})
             first :kind))))

(deftest t13-trips-when-reviewer-artifact-is-a-failed-predecessor
  (is (= [{:kind :reviewer-artifact-is-failed-predecessor
           :repo run-5b-repo :reviewer-commit "c9e7aaf"
           :failed-commit "c9e7aaf80f1bc6576f2b1874ebb4b44d64c2c219"}]
         (tripwire/evaluate-wire
          :T13 {:tripwire/force? true
                :artifact-binding/fresh-author? true
                :artifact-binding/repo run-5b-repo
                :artifact-binding/reviewer-commit "c9e7aaf"
                :artifact-binding/author-window-start-ms
                (instant-ms "2026-07-16T11:40:00Z")
                :artifact-binding/failed-commits
                ["c9e7aaf80f1bc6576f2b1874ebb4b44d64c2c219"]}))))

(deftest t13-allows-a-fresh-repo-commit-within-the-author-window
  (is (empty?
       (tripwire/evaluate-wire
        :T13 {:tripwire/force? true
              :artifact-binding/fresh-author? true
              :artifact-binding/repo run-5b-repo
              :artifact-binding/reviewer-commit "099906e"
              :artifact-binding/author-window-start-ms
              (instant-ms "2026-07-16T12:00:13Z")
              :artifact-binding/author-window-end-ms
              (instant-ms "2026-07-16T12:12:12Z")
              :artifact-binding/failed-commits ["d908b2c"]}))))

(deftest every-wire-is-individually-disableable
  (let [reports (atom [])
        record {:phase :synthetic
                :tripwire/snapshot {:runner/dispatched-turns 2
                                    :agency/dispatch-count 1}}]
    (try
      (tripwire/set-wire-enabled! :T1 false)
      (is (identical? record
                      (tripwire/observe! {:tripwire/report-writer
                                          #(swap! reports conj %)}
                                         record)))
      (is (empty? @reports))
      (finally
        (tripwire/set-wire-enabled! :T1 true)))))

(deftest trip-during-trip-degrades-without-recursion-or-escape
  (let [root (temp-dir)
        writes (atom 0)
        record {:phase :synthetic
                :tripwire/snapshot {:runner/dispatched-turns 2
                                    :agency/dispatch-count 1}}
        opts (atom nil)
        writer (fn [_]
                 (swap! writes inc)
                 ;; This nested violation is evaluated while the outer trip is
                 ;; being handled. It must print a diagnostic, not recurse.
                 (tripwire/observe! @opts record))]
    (reset! opts {:tripwire/report-writer writer
                  :tripwire/report-root (.getPath root)})
    (halted! @opts record)
    (is (= 1 @writes))
    (is (= 1 (count (filter #(.isFile %)
                            (or (.listFiles root) [])))))))

(deftest valid-job-snapshot-and-boundary-values-do-not-trip
  (let [now (str (Instant/now))]
    (is (empty? (tripwire/evaluate-wire
                 :T11 {:job-snapshot {:job-id "j" :state "done"
                                      :created-at now :events [{:at now}]}})))
    (is (empty? (tripwire/evaluate-wire
                 :T9 {:phase :p :transition :end :duration-ms 2000
                      :phase-budget-ms 1000})))
    (is (empty? (tripwire/evaluate-wire
                 :T1 {:runner/dispatched-turns 2
                      :agency/dispatch-count 2})))))

(deftest t10-does-not-trip-on-a-file-that-was-edited-and-reloaded
  ;; Until 2026-09-19 this wire compared the file's hash against a hash of the
  ;; same file taken when the observer loaded, so it fired whether or not the
  ;; namespace had been reloaded. It tripped 60 times between 2026-07-16 and
  ;; 2026-09-15 and was read as noise -- and inside that noise the serving JVM
  ;; ran a runner 28 hours out of date, ignored the author's correct commit
  ;; claim, and opened the three build-failed findings that became a livelock.
  ;;
  ;; Every tracked namespace here was edited and reloaded today, so a
  ;; hash-against-hash wire would fire on all of them. Proven evidence does not.
  (testing "loaded definition lines agree with the files they were loaded from"
    (doseq [[ns-sym path] tripwire/runner-namespace-sources
            :when (find-ns ns-sym)]
      (is (empty? (tripwire/image-file-divergence ns-sym path))
          (str ns-sym " image disagrees with " path))))
  (testing "so the wire is clear"
    (is (empty? (tripwire/evaluate-wire :T10 {:tripwire/force? true})))))

;; ---------------------------------------------------------------------------
;; Durable trip reports are bounded (2026-09-18)
;; ---------------------------------------------------------------------------
;;
;; cross-run-observation folds the whole repair store into the observation at
;; :opportunity/:start, and observe! writes a full copy of that observation into
;; one trip report per firing wire.  Measured on 2026-09-18: 91,336,366 bytes
;; per report, ~3m43s each under pprint, five per click — roughly 19 minutes of
;; a click spent serialising the machine's own history, which nothing reads back
;; from disk.  Replaying that same observation through the bounded writer:
;; 5,096 bytes in 4 ms, with only :findings elided.

(deftest durable-trip-report-is-bounded-test
  (let [root (temp-dir)
        big  (vec (repeatedly 5000 #(hash-map :id (str (java.util.UUID/randomUUID))
                                              :backtrace {:phase-events (vec (range 50))})))
        observation {:phase :opportunity
                     :transition :start
                     :attempt-id "attempt-777"
                     :machine-state {:started-at "2026-09-18T23:24:51Z"}
                     :findings big}
        report {:trip/wire-id :T1
                :trip/witness {:reason :probe}
                :trip/observation observation}
        path (tripwire/write-trip-report! (.getPath root) report)
        stored (edn/read-string (slurp path))
        stored-obs (:trip/observation stored)]

    (testing "small entries survive verbatim, so the report stays legible"
      (is (= :opportunity (:phase stored-obs)))
      (is (= "attempt-777" (:attempt-id stored-obs)))
      (is (= {:started-at "2026-09-18T23:24:51Z"} (:machine-state stored-obs))))

    (testing "the oversized entry is replaced by a description of what was there"
      (is (= :exceeds-durable-trip-report-budget
             (get-in stored-obs [:findings :elided/reason])))
      (is (= :vector (get-in stored-obs [:findings :elided/type])))
      (is (= 5000 (get-in stored-obs [:findings :elided/count]))))

    (testing "the elision is what keeps the file small"
      (is (< (.length (io/file path)) 8192)))

    (testing "the in-memory report is untouched — its consumers still see it all"
      (is (= 5000 (count (get-in report [:trip/observation :findings])))))))

;; ---------------------------------------------------------------------------
;; T8 counts unresolved, identified repetition (2026-09-18)
;; ---------------------------------------------------------------------------

(deftest t8-livelock-excludes-closed-and-unidentified-findings-test
  (let [f (fn [id kind target] {:repair/id id :failure-kind kind :target target})]

    (testing "three open findings against one target are the livelock T8 is for"
      (is (= [{:kind :duplicate-finding-livelock
               :signature [:build-failed "repair-x" nil]
               :repair-ids ["a" "b" "c"]
               :finding-count 3}]
             (tripwire/livelock-violations
              [(f "a" :build-failed "repair-x")
               (f "b" :build-failed "repair-x")
               (f "c" :build-failed "repair-x")]))))

    (testing "a closed repair is progress, not repetition"
      ;; The store is append-only: a resolved finding still reads
      ;; :repair/status :open in its own record, so closure has to be passed in.
      (is (empty? (tripwire/livelock-violations
                   [(f "a" :build-failed "repair-x")
                    (f "b" :build-failed "repair-x")
                    (f "c" :build-failed "repair-x")]
                   #{"c"}))))

    (testing "findings with no target and no failed-commit are not the same thing"
      (is (empty? (tripwire/livelock-violations
                   [(f "a" :agent-unavailable nil)
                    (f "b" :agent-unavailable nil)
                    (f "c" :agent-unavailable nil)]))))

    (testing "a failed-commit is discriminator enough on its own"
      (is (= 1 (count (tripwire/livelock-violations
                       [{:repair/id "a" :failure-kind :build-failed :failed-commit "deadbeef"}
                        {:repair/id "b" :failure-kind :build-failed :failed-commit "deadbeef"}
                        {:repair/id "c" :failure-kind :build-failed :failed-commit "deadbeef"}])))))))

(deftest t8-livelock-is-repetition-now-not-a-backlog
  ;; The three findings opened 02:51, 03:22 and 04:14 on 2026-09-15 WERE a
  ;; livelock that morning. With no time dimension they went on reporting one
  ;; four days later with no attempt in between -- and since witnesses halt
  ;; (a8ac1615) that stopped every click, while the only honest discharge for
  ;; those findings needed a repair attempt the halt itself prevented.
  (let [at (fn [iso] {:repair/id (str "r-" iso) :failure-kind :build-failed
                      :target "repair-ea1-504ad863--attempt-001" :failed-commit nil
                      :opened-at iso})
        group [(at "2026-09-15T02:51:23Z")
               (at "2026-09-15T03:22:23Z")
               (at "2026-09-15T04:14:53Z")]
        ms #(.toEpochMilli (java.time.Instant/parse %))]

    (testing "on the morning it happened, this is exactly what T8 is for"
      (let [v (tripwire/livelock-violations group #{} (ms "2026-09-15T04:20:00Z"))]
        (is (= 1 (count v)))
        (is (= :duplicate-finding-livelock (:kind (first v))))
        (is (= 3 (:finding-count (first v))))))

    (testing "four days later, with nothing new, it is a backlog and not a halt"
      (is (empty? (tripwire/livelock-violations group #{} (ms "2026-09-19T00:54:00Z")))))

    (testing "the grouping itself is not loosened -- two is still not a livelock"
      (is (empty? (tripwire/livelock-violations (take 2 group) #{}
                                                (ms "2026-09-15T04:20:00Z")))))

    (testing "a group that is still being added to keeps tripping"
      (is (seq (tripwire/livelock-violations
                (conj group (at "2026-09-19T00:30:00Z")) #{}
                (ms "2026-09-19T00:54:00Z")))))

    (testing "the violation says how old the newest member is, so the trip report shows why"
      (let [v (first (tripwire/livelock-violations
                      group #{} (ms "2026-09-15T06:14:53Z")))]
        (is (= 2 (:age-hours v)))
        (is (= "2026-09-15T04:14:53Z" (:newest-opened-at v)))))))
