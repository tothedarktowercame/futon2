(ns futon2.aif.tripwire-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.registered-run-telemetry :as telemetry]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.tripwire :as tripwire]
            [futon2.aif.wm.debugger :as debugger])
  (:import [java.nio.file Files]
           [java.time Instant]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "wm-tripwire-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- halted!
  "Run `observe!` under halt mode on a record expected to trip, returning the
  halt's ex-data.

  One tripwire, one shutdown was the default from 2026-09-18; since Joe's
  2026-09-19 ruling (T-wm-excessive-guardrails-19092026) halting is opt-in
  (`FUTON_WM_TRIPWIRE_HALT=1`), so these cases bind the mode on explicitly.
  They construct a tripping observation deliberately — the throw is the
  expected path, and what they actually assert is what the trip recorded on
  its way out."
  [opts record]
  (let [data (binding [tripwire/*halt-on-witness?* true]
               (try
                 (tripwire/observe! opts record)
                 ::no-halt
                 (catch clojure.lang.ExceptionInfo e
                   (ex-data e))))]
    (is (not= ::no-halt data) "expected the tripwire to halt the run")
    (is (= :tripwire-tripped (:failure-kind data)))
    data))

(defn- write-edn! [root child filename value]
  (let [file (io/file root child filename)]
    (io/make-parents file)
    (spit file (pr-str value))
    file))

(defn- coverage-source-pin! [root name text]
  (let [file (io/file root name)]
    (spit file text)
    {:path (.getPath file)
     :sha256 (#'tripwire/source-sha256 (.getPath file))}))

(deftest dismissal-directory-membership-closes-t8-sources
  (let [root (temp-dir)
        finding (fn [id] {:repair/id id :repair/status :open
                          :repair/class :machine-failure
                          :attempt-id (str id "-attempt")})]
    (doseq [[id status] [["echo-1" :dismissed-echo]
                         ["fixture-1" :dismissed-fixture-pollution]]]
      (write-edn! root "findings" (str id ".edn") (finding id))
      (write-edn! root "dismissals" (str id ".edn")
                  {:repair/id id :repair/status status}))
    (let [observation (#'tripwire/cross-run-observation
                       {:repair-root (.getPath root)
                        :cohort? true
                        :tripwire/cohort-history []
                        :tripwire/a-matrix-events []
                        :tripwire/grounding-witnesses []}
                       {:phase :opportunity :transition :start})]
      (is (contains? (:closed-repair-ids observation) "echo-1"))
      (is (contains? (:closed-repair-ids observation) "fixture-1"))
      (is (not (tripwire/repair-covered-witness?
                {:repair-ids ["echo-1" "fixture-1"]} observation))))))

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

(deftest default-mode-records-and-continues
  ;; Joe's 2026-09-19 ruling (T-wm-excessive-guardrails-19092026): without
  ;; FUTON_WM_TRIPWIRE_HALT=1 a witness is recorded durably and the run
  ;; continues — and the identical witness later in the same run does not
  ;; write a second report (the 09-18 anti-pile-up property, kept without
  ;; the shutdown).
  (let [now (str (Instant/now))
        finding (fn [id]
                  {:repair/id id :repair/status :open
                   :failure-kind :review-rejected
                   :target "M-x" :failed-commit "abc"
                   :opened-at now})
        report-root (temp-dir)
        opts {:tripwire/report-root (.getPath report-root)
              :run-id (str "test-run-" (System/nanoTime))
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))}
        record {:phase :construction :transition :start
                :attempt-id "attempt-note"
                :tripwire/force? true
                :findings (mapv finding ["r1" "r2" "r3"])
                :closed-repair-ids #{}}
        report-files #(count (filter (fn [f] (.isFile f)) (file-seq report-root)))]
    (binding [tripwire/*halt-on-witness?* false]
      (testing "the witness does not stop the run and is written durably"
        (is (= record (tripwire/observe! opts record))
            "observe! returns its record unchanged — the run continues")
        (is (pos? (report-files)) "the trip report was written"))
      (testing "the same witness in the same run records nothing further"
        (let [before (report-files)]
          (is (= record (tripwire/observe! opts record)))
          (is (= before (report-files))
              "an identical witness within one run is recorded once"))))))

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

(deftest incident-close-records-one-coverage-disposition-without-changing-closure
  ;; Historical context only: :attempt-006-late-author-artifact is one audited
  ;; calibration incident.  This test uses a new synthetic identity and never
  ;; reads or rewrites the museum's incident or blind-spot files.
  (let [root (temp-dir)
        incident {:incident/id "p22-synthetic-unwired-coverage"}
        closure {:status :closed
                 :incident/id (:incident/id incident)
                 :closure-evidence {:path "closures/p22.edn"
                                    :sha256 (apply str (repeat 64 "a"))}}
        opts {:tripwire/incident-coverage-root (.getPath root)
              :tripwire/incident-close-fn (fn [_ _] closure)}
        first-close (tripwire/close-incident! opts incident)
        repeated-close (tripwire/close-incident! opts incident)
        files (seq (.listFiles root))]
    (is (= :closed (:status first-close)))
    (is (= closure (:closure first-close)))
    (is (= :persisted (get-in first-close [:coverage :status])))
    (is (= :unresolved (get-in first-close [:coverage :receipt :disposition :status])))
    (is (= :unknown (get-in first-close [:coverage :receipt :disposition :owner])))
    (is (= :persisted (get-in repeated-close [:coverage :status])))
    (is (true? (get-in repeated-close [:coverage :idempotent?])))
    (is (= 1 (count files)))
    (is (= :unresolved
           (get-in (edn/read-string (slurp (first files))) [:disposition :status])))))

(deftest incident-close-requires-bound-wire-verification-and-keeps-close-on-write-failure
  (let [root (temp-dir)
        source-pin (coverage-source-pin! root "T1.clj" "verified T1 bytes")
        incident {:incident/id "p22-synthetic-wire-coverage"}
        closure {:status :closed :incident/id (:incident/id incident)
                 :closure-evidence {:path "closures/p22-wire.edn"
                                    :sha256 (apply str (repeat 64 "b"))}
                 :wire-reference
                 {:wire-id :T1
                  :verification {:status :verified
                                 :source-pin source-pin}}}
        opts {:tripwire/incident-close-fn (fn [_ _] closure)
              :tripwire/wire-verifier
              (fn [{:keys [wire-id verification]}]
                {:status :verified :wire-id wire-id
                 :source-pin (:source-pin verification)})
              :tripwire/coverage-writer (fn [_ _]
                                          {:status :persisted :path "/tmp/p22-wire.edn"
                                           :idempotent? false})}
        covered (tripwire/close-incident! opts incident)
        fabricated (tripwire/close-incident!
                    (dissoc opts :tripwire/wire-verifier) incident)
        unreadable (with-redefs-fn
                     {#'tripwire/source-sha256
                      (fn [_] (throw (java.io.IOException. "fixture unreadable")))}
                     #(tripwire/close-incident! opts incident))
        verifier-failed (tripwire/close-incident!
                         (assoc opts :tripwire/wire-verifier
                                (fn [_] (throw (ex-info "fixture verifier failed" {}))))
                         incident)
        drifted (do (spit (:path source-pin) "changed T1 bytes")
                    (tripwire/close-incident! opts incident))
        failed (tripwire/close-incident!
                (assoc opts :tripwire/coverage-writer
                       (fn [& _] (throw (ex-info "coverage store unavailable" {}))))
                incident)]
    (is (= :covered (get-in covered [:coverage :receipt :disposition :status])))
    (is (= :T1 (get-in covered [:coverage :receipt :disposition :wire-reference :wire-id])))
    (is (= :unresolved (get-in fabricated [:coverage :receipt :disposition :status])))
    (is (= :wire-reference-verification-unavailable
           (get-in fabricated [:coverage :receipt :disposition :reason])))
    (is (= :closed (:status unreadable)))
    (is (= closure (:closure unreadable)))
    (is (= :unresolved (get-in unreadable [:coverage :receipt :disposition :status])))
    (is (= :wire-reference-source-unavailable
           (get-in unreadable [:coverage :receipt :disposition :reason])))
    (is (= :closed (:status verifier-failed)))
    (is (= closure (:closure verifier-failed)))
    (is (= :unresolved (get-in verifier-failed [:coverage :receipt :disposition :status])))
    (is (= :wire-reference-verification-failed
           (get-in verifier-failed [:coverage :receipt :disposition :reason])))
    (is (= :unresolved (get-in drifted [:coverage :receipt :disposition :status])))
    (is (= :wire-reference-source-drift
           (get-in drifted [:coverage :receipt :disposition :reason])))
    (is (= :closed (:status failed)))
    (is (= closure (:closure failed)))
    (is (= {:status :unavailable :reason :coverage-persistence-failed}
           (:coverage failed)))))

(deftest incident-coverage-publication-is-atomic-and-writer-results-are-typed
  (let [root (temp-dir)
        receipt {:schema :wm/tripwire-incident-coverage-v1
                 :incident/id "p22-atomic-receipt"
                 :closure-evidence {:path "closures/atomic.edn"
                                    :sha256 (apply str (repeat 64 "e"))}
                 :disposition {:status :unresolved :reason :no-wire-reference
                               :owner :unknown}}
        final-file (io/file root "p22-atomic-receipt.edn")]
    (is (thrown? clojure.lang.ExceptionInfo
                 (with-redefs-fn
                   {#'tripwire/write-coverage-temp!
                    (fn [dir _]
                      (let [temp (io/file dir ".interrupted.tmp")]
                        (spit temp "{")
                        (throw (ex-info "interrupted write" {}))))}
                   #(tripwire/write-incident-coverage! (.getPath root) receipt))))
    (is (not (.exists final-file)))
    (is (= :persisted (:status (tripwire/write-incident-coverage! (.getPath root) receipt))))
    (let [original (slurp final-file)
          conflicting (assoc-in receipt [:disposition :reason] :different-disposition)]
      (is (= :conflict (:status (tripwire/write-incident-coverage!
                                 (.getPath root) conflicting))))
      (is (= original (slurp final-file))))
    (let [incident {:incident/id "p22-invalid-writer"}
          closure {:status :closed :incident/id (:incident/id incident)
                   :closure-evidence {:path "closures/invalid-writer.edn"
                                      :sha256 (apply str (repeat 64 "f"))}}
          result (tripwire/close-incident!
                  {:tripwire/incident-close-fn (fn [_ _] closure)
                   :tripwire/coverage-writer (fn [& _] nil)} incident)]
      (is (= :closed (:status result)))
      (is (= closure (:closure result)))
      (is (= {:status :unavailable :reason :coverage-writer-result-invalid}
             (:coverage result))))))

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

(deftest configured-tripwire-recipient-is-recorded-before-one-honest-notice-attempt
  (let [record (synthetic-trip-record)
        reports (atom [])
        deliveries (atom [])
        effects (atom [])
        off-roster {:tripwire/action :park-and-summon
                    :tripwire/responsible-seat "codex-off-roster"
                    :tripwire/report-writer #(do (swap! reports conj %) "/tmp/trip-off-roster.edn")
                    :tripwire/roster-fn (constantly #{"codex-live"})
                    :tripwire/park-fn (fn [& _] (swap! effects conj :park) {})
                    :tripwire/bell-fn (fn [& _] (swap! effects conj :bell))
                    :tripwire/delivery-observer #(swap! deliveries conj %)}]
    (is (= record (tripwire/observe! off-roster record)))
    (is (= {:status :undelivered :recipient "codex-off-roster"
            :reason :recipient-off-roster}
           (:trip/notification (first @reports))))
    (is (empty? @effects))
    (is (= :undelivered (get-in @deliveries [0 :delivery :status])))
    (reset! reports []) (reset! deliveries [])
    (let [ledger (atom {:order [] :jobs {}})
          registered {:tripwire/action :park-and-summon
                      :tripwire/responsible-seat "codex-live"
                      :run-id "run-p21" :click-id "click-p21"
                      :registered-run/job-ledger ledger
                      :tripwire/report-writer #(do (swap! reports conj %) "/tmp/trip-live-recipient.edn")
                      :tripwire/repair-record-fn (constantly nil)
                      :tripwire/roster-fn (constantly #{"codex-live"})
                      :tripwire/park-fn (fn [_ _] {})
                      :tripwire/bell-fn (fn [_ payload]
                                          (swap! effects conj payload)
                                          {:accepted true :job-id "p21-job"})
                      :tripwire/delivery-observer #(swap! deliveries conj %)}]
      (is (= record (tripwire/observe! registered record)))
      (is (= 1 (count @effects)))
      (is (= "codex-live" (:agent-id (first @effects))))
      (is (= :resolved (get-in (first @reports) [:trip/notification :status])))
      (is (= {:status :accepted-enqueue :recipient "codex-live"
              :job-id "p21-job" :delivery-status :unacknowledged}
             (get-in @deliveries [0 :delivery]))))))

(deftest nil-transport-and-report-write-failure-do-not-invent-delivery
  (let [record (synthetic-trip-record)
        reports (atom []) deliveries (atom []) calls (atom 0)
        opts {:tripwire/action :park-and-summon
              :tripwire/responsible-seat "codex-live"
              :run-id "run-p21-nil" :click-id "click-p21-nil"
              :registered-run/job-ledger (atom {:order [] :jobs {}})
              :tripwire/report-writer #(do (swap! reports conj %) "/tmp/trip-nil-transport.edn")
              :tripwire/repair-record-fn (constantly nil)
              :tripwire/roster-fn (constantly #{"codex-live"})
              :tripwire/park-fn (fn [_ _] {})
              :tripwire/bell-fn (fn [& _] nil)
              :tripwire/delivery-observer #(swap! deliveries conj %)}]
    (is (= record (tripwire/observe! opts record)))
    (is (= :resolved (get-in (first @reports) [:trip/notification :status])))
    (is (= {:status :unavailable :recipient "codex-live"
            :reason :notification-transport-unavailable}
           (get-in @deliveries [0 :delivery])))
    (let [failed-write (assoc opts
                              :tripwire/report-writer (fn [_] (throw (ex-info "write failed" {})))
                              :tripwire/bell-fn (fn [& _] (swap! calls inc)))]
      (is (= record (tripwire/observe! failed-write record)))
      (is (zero? @calls))
      (is (= 1 (count @deliveries))
          "a failed report produces neither a durable notice nor another delivery observation"))))

(deftest park-and-summon-builds-background-join-and-roster-checked-bell
  (let [finding (atom nil)
        park (atom nil)
        bell (atom nil)
        ledger (atom {:order [] :jobs {}})
        opts {:tripwire/action :park-and-summon
              :tripwire/responsible-seat "claude-6"
              :run-id "run-trip" :click-id "click-trip"
              :registered-run/job-ledger ledger
              :tripwire/report-writer (fn [_] "/tmp/trip-summon.edn")
              :tripwire/repair-record-fn #(reset! finding %)
              :tripwire/roster-fn (fn [_] #{"claude-6" "codex-7"})
              :tripwire/park-fn (fn [_ payload] (reset! park payload) {})
              :tripwire/bell-fn (fn [_ payload]
                                  (reset! bell payload)
                                  {:accepted true :job-id "trip-job"})}]
    (halted! opts (synthetic-trip-record))
    (is (= :invariant-tripped (:failure-kind @finding)))
    (is (= {:agent "claude-6" :surface "emacs-repl" :mode :background}
           (select-keys @park [:agent :surface :mode])))
    (is (= 1 (count (:awaiting @park))))
    (is (str/ends-with? (first (:awaiting @park)) "-investigation"))
    (is (= "claude-6" (:agent-id @bell)))
    (is (= {:kind :war-machine :basis :producer-context
            :execution-id "run-trip" :source-ref "click-trip"}
           (:harness @bell)))
    (is (= :typed-missing
           (:status (telemetry/model-usage ledger {:run-id "run-trip"
                                                   :click-id "click-trip"}))))
    (is (= :terminal-receipt-missing
           (get-in (telemetry/model-usage ledger {:run-id "run-trip"
                                                  :click-id "click-trip"})
                   [:jobs 0 :reason])))
    (is (re-find #"investigate then discharge or revise the wire"
                 (:prompt @bell)))))

(deftest summon-roster-accepts-real-agents-map-shape
  (let [effects (atom [])
        ledger (atom {:order [] :jobs {}})
        opts {:tripwire/action :park-and-summon
              :tripwire/responsible-seat "claude-6"
              :run-id "run-roster" :click-id "click-roster"
              :registered-run/job-ledger ledger
              :tripwire/report-writer (fn [_] "/tmp/trip-real-roster.edn")
              :tripwire/repair-record-fn (fn [_] (swap! effects conj :repair))
              :tripwire/park-fn (fn [& _] (swap! effects conj :park) {})
              :tripwire/bell-fn (fn [& _]
                                  (swap! effects conj :bell)
                                  {:accepted true :job-id "roster-job"})}]
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
        ledger (atom {:order [] :jobs {}})
        opts {:tripwire/action :park-and-summon
              :run-id "run-bell-fail" :click-id "click-bell-fail"
              :registered-run/job-ledger ledger
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
              :tripwire/responsible-seat "claude-6"
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
        ledger (atom {:order [] :jobs {}})
        opts {:tripwire/action :park-and-summon
              :tripwire/responsible-seat "claude-6"
              :run-id "run-bell-fail" :click-id "click-bell-fail"
              :registered-run/job-ledger ledger
              :tripwire/report-writer (fn [_] "/tmp/trip-bell-fail.edn")
              :tripwire/repair-record-fn
              (fn [_] (swap! effects conj :repair))
              :tripwire/roster-fn (fn [_] #{"claude-6"})
              :tripwire/park-fn (fn [& _] (swap! effects conj :park) {})
              :tripwire/bell-fn
              (fn [& _] (swap! effects conj :bell)
                (throw (ex-info "bell unavailable" {})))}]
    (binding [*err* err]
      (halted! opts record))
    (is (= [:repair :park :bell] @effects))
    (is (str/includes? (str err) "degraded to :stop-line"))))

(deftest tripwire-bell-ledger-refuses-missing-duplicate-and-wrong-join
  (let [payload {:agent-id "claude-6" :caller "wm-full-loop"
                 :mission-id "M-wm-tripwires-investigation"}
        missing (try (#'tripwire/post-bell! {} payload)
                     nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :wm-tripwire-telemetry-context-missing (:failure-kind missing))))
  (let [ledger (atom {:order [] :jobs {}})
        opts {:run-id "run-trip" :click-id "click-trip"
              :registered-run/job-ledger ledger
              :tripwire/bell-fn (fn [& _] {:accepted true :job-id "trip-job"})}
        payload {:agent-id "claude-6" :caller "wm-full-loop"
                 :mission-id "M-wm-tripwires-investigation"}]
    (#'tripwire/post-bell! opts payload)
    (is (= :wm-job-ledger-duplicate-job
           (try (#'tripwire/post-bell! opts payload)
                nil
                (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e))))))
    (telemetry/retain-terminal-job!
     ledger {:job-id "trip-job" :state "done"
             :harness {:kind :war-machine :basis :producer-context
                       :execution-id "wrong-run" :source-ref "click-trip"}
             :usage {:input_tokens 1 :output_tokens 2 :total_tokens 3}})
    (is (= :run-click-job-join-mismatch
           (get-in (telemetry/model-usage ledger {:run-id "run-trip"
                                                  :click-id "click-trip"})
                   [:jobs 0 :reason])))))

(deftest wm-agency-bell-call-sites-are-a-closed-enumerated-set
  (let [root (io/file "src/futon2/aif")
        paths (->> (file-seq root)
                   (filter #(.isFile %))
                   (filter #(str/includes? (slurp %) "/api/alpha/bell"))
                   (map #(.getPath %))
                   set)]
    (is (= #{"src/futon2/aif/full_loop_runner.clj"
             "src/futon2/aif/tripwire.clj"}
           paths))))

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

;; --- Tripwire debugger stops with per-firing outcome records (2026-10-22) ---

(defn- dbg-wait-for-stop
  [run-id]
  (loop [remaining 6000]
    (if-let [stop (first (filter #(= run-id (:run-id %)) (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not appear" {:run-id run-id}))))))

(defn- dbg-wait-for-new-stop
  "Wait for a stop of RUN-ID other than the one at PREV-STOPPED-AT. The
  re-stop after :retry can register within the same poll window in which
  the previous entry is removed, so waiting for 'gone then stopped' races."
  [run-id prev-stopped-at]
  (loop [remaining 6000]
    (if-let [stop (first (filter #(and (= run-id (:run-id %))
                                       (not= prev-stopped-at (:stopped-at %)))
                                 (debugger/stopped)))]
      stop
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Second debugger stop did not appear"
                        {:run-id run-id :prev-stopped-at prev-stopped-at}))))))

(defn- dbg-wait-gone
  [run-id]
  (loop [remaining 6000]
    (if (pos? (count (filter #(= run-id (:run-id %)) (debugger/stopped))))
      (if (pos? remaining)
        (do (Thread/sleep 5) (recur (dec remaining)))
        (throw (ex-info "Debugger stop did not clear" {:run-id run-id})))
      true)))

(defn- firing-files [root]
  (filter #(.isFile %) (file-seq (io/file root))))

(defn- read-firings [root]
  (filterv #(= :wm/tripwire-firing-v1 (:schema %))
           (map #(edn/read-string (slurp %)) (firing-files root))))

(defn- gated-witness-opts
  "Disable every registry wire and gate a synthetic witness through an atom,
  so a test controls whether the (re-)evaluation still witnesses."
  [run-id report-root firing-root gate]
  ;; T8 stays enabled so observe! reaches evaluate-wire (which the test
  ;; redefines to the gated synthetic witness); every other wire is off.
  {:tripwire/report-root (.getPath report-root)
   :tripwire/firing-root (.getPath firing-root)
   :run-id run-id
   :tripwire/disabled-wire-ids (vec (keys (dissoc @tripwire/wire-registry :T8)))
   :tripwire/gate gate})

(deftest attached-witness-stops-continues-and-writes-one-firing
  (let [run-id (str "dbg-trip-" (System/nanoTime))
        report-root (temp-dir) firing-root (temp-dir)
        opts (gated-witness-opts run-id report-root firing-root (atom true))
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/attach!)
    (try
      (let [gate (:tripwire/gate opts)
            running (future
                      (with-redefs [tripwire/evaluate-wire
                                    (fn [_wire _obs] (if @gate [{:kind :test-witness}] []))]
                        (tripwire/observe! opts record)))]
        (let [stop (dbg-wait-for-stop run-id)]
          (is (= :tripwire-tripped (get-in stop [:condition :kind])))
          (is (= :T8 (:tripwire/wire-id (:ex-data stop))))
          (is (= record (debugger/stop-value run-id))
              "the phase record is retained as the stop value"))
        ;; the trip report exists while the run is stopped
        (is (= 1 (count (filter #(.isFile %) (file-seq report-root))))
            "the trip was recorded durably before the stop")
        (debugger/continue! run-id :continue :nothing-to-fix)
        (is (= record (deref running 3000 ::timeout)))
        (dbg-wait-gone run-id)
        (let [firings (read-firings firing-root)]
          (is (= 1 (count firings)) "exactly one firing record")
          (is (= :wm/tripwire-firing-v1 (:schema (first firings))))
          (is (= :nothing-to-fix (:disposition (first firings))))
          (is (= :continue (:restart-choice (first firings))))
          (is (= run-id (:run-id (first firings))))
          (is (:trip/id (first firings)))
          (is (:report-path (first firings)))))
      (finally (debugger/detach!)))))

(deftest retry-re-evaluates-wires-on-the-same-record
  (testing ":retry after the condition is cleared resumes with no second stop"
    (let [run-id (str "dbg-retry-clear-" (System/nanoTime))
          report-root (temp-dir) firing-root (temp-dir)
          gate (atom true)
          opts (gated-witness-opts run-id report-root firing-root gate)
          record {:phase :construction :transition :start :attempt-id "a1"}]
      (debugger/attach!)
      (try
        (let [running (future
                        (with-redefs [tripwire/evaluate-wire
                                      (fn [_wire _obs] (if @gate [{:kind :test-witness}] []))]
                          (tripwire/observe! opts record)))]
          (dbg-wait-for-stop run-id)
          (reset! gate false)
          (debugger/continue! run-id :retry)
          (is (= record (deref running 3000 ::timeout)))
          (dbg-wait-gone run-id)
          (is (= [:retry] (mapv :restart-choice (read-firings firing-root)))
              "the retry resolution is still a firing outcome"))
        (finally (debugger/detach!)))))
  (testing ":retry while still witnessed stops again"
    (let [run-id (str "dbg-retry-again-" (System/nanoTime))
          report-root (temp-dir) firing-root (temp-dir)
          gate (atom true)
          opts (gated-witness-opts run-id report-root firing-root gate)
          record {:phase :construction :transition :start :attempt-id "a1"}]
      (debugger/attach!)
      (try
        (let [running (future
                        (with-redefs [tripwire/evaluate-wire
                                      (fn [_wire _obs] (if @gate [{:kind :test-witness}] []))]
                          (tripwire/observe! opts record)))]
          (let [first-stop (dbg-wait-for-stop run-id)]
            (debugger/continue! run-id :retry)
            ;; the first stop entry is removed by the run thread when it
            ;; takes the retry; wait for a DIFFERENT stop, not merely for
            ;; this one to clear — the re-stop can register in the same
            ;; poll window, and delivering to the spent promise is the bug.
            (dbg-wait-for-new-stop run-id (:stopped-at first-stop))
            (debugger/continue! run-id :continue :fixed))
          (is (= record (deref running 3000 ::timeout)))
          (dbg-wait-gone run-id)
          (is (= {:retry 1 :continue 1}
                 (frequencies (mapv :restart-choice (read-firings firing-root))))
              "one firing record per restart resolution")
          (is (= #{[:retry :unrecorded] [:continue :fixed]}
                 (set (mapv (juxt :restart-choice :disposition)
                            (read-firings firing-root))))))
        (finally (debugger/detach!))))))

(deftest abort-ends-the-run-with-one-stop-and-a-discharged-rethrow
  (let [run-id (str "dbg-abort-" (System/nanoTime))
        report-root (temp-dir) firing-root (temp-dir)
        gate (atom true)
        opts (gated-witness-opts run-id report-root firing-root gate)
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/attach!)
    (try
      (let [stops-seen (atom 0)
            running (future
                      (with-redefs [tripwire/evaluate-wire
                                    (fn [_wire _obs]
                                      (swap! stops-seen inc)
                                      (if @gate [{:kind :test-witness}] []))]
                        (try
                          (tripwire/observe! opts record)
                          ::no-throw
                          (catch clojure.lang.ExceptionInfo e e))))]
        (dbg-wait-for-stop run-id)
        (debugger/continue! run-id :abort :failed)
        (let [thrown (deref running 3000 ::timeout)]
          (is (instance? clojure.lang.ExceptionInfo thrown))
          (is (= :tripwire-tripped (:outcome (ex-data thrown))))
          (is (= :failed (:disposition (first (read-firings firing-root)))))
          (is (= :abort (:restart-choice (first (read-firings firing-root)))))
          (is (boolean (:debugger/discharged? (ex-data thrown)))
              "the abort rethrow is marked discharged")
          (is (false? (debugger/stoppable-failure? thrown))
              "run-phase!'s catch will not stop the same trip again"))
        (dbg-wait-gone run-id)
        (testing "the bad case: without the discharged mark the rethrow WOULD restop"
          ;; This is the exact throwable run-phase!'s catch consults. With the
          ;; mark removed (the pre-repair stoppable-failure?), this predicate
          ;; returned true and the abort produced a SECOND debugger stop.
          (is (true? (debugger/stoppable-failure?
                      (ex-info "tripwire abort" {:outcome :tripwire-tripped})))
              "an unmarked tripwire abort is still stoppable (the guard's input)")
          (is (false? (debugger/stoppable-failure?
                       (ex-info "tripwire abort" {:outcome :tripwire-tripped
                                                  :debugger/discharged? true})))
              "the discharged mark is exactly what prevents the second stop")))
        (finally (debugger/detach!)))))

(deftest run-phase-tripwire-abort-stops-exactly-once
  ;; The abort throw must survive run-phase!'s catch without a second stop.
  ;; Tripping on the :end :ok emission exercises the catch path (the :start
  ;; emission is outside run-phase!'s try).
  (let [run-id (str "dbg-run-abort-" (System/nanoTime))
        report-root (temp-dir) firing-root (temp-dir)
        ;; The witness fires only on the :end emission: the thunk succeeds,
        ;; the tripwire abort is thrown inside run-phase!'s try, and the
        ;; catch must NOT produce a second debugger stop.
        witnessing? (fn [obs] (= :end (:transition obs)))
        stops-seen (atom 0)
        opts {:tripwire/report-root (.getPath report-root)
              :tripwire/firing-root (.getPath firing-root)
              :run-id run-id
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))
              :phase-events (atom [])
              :phase-log-fn (constantly nil)
              :nano-time-fn #(System/nanoTime)}]
    (debugger/attach!)
    (try
      (let [running (future
                      (with-redefs [tripwire/evaluate-wire
                                    (fn [_wire obs]
                                      (when (witnessing? obs)
                                        (swap! stops-seen inc))
                                      (if (witnessing? obs) [{:kind :test-witness}] []))]
                        (try
                          (runner/run-phase! opts {:opportunity-id "op"
                                                   :attempt-id "attempt"}
                                             :phase-x (constantly :computed))
                          ::no-throw
                          (catch clojure.lang.ExceptionInfo e e))))]
        (dbg-wait-for-stop run-id)
        (is (= 1 @stops-seen) "the witness fired exactly once (on :end)")
        (debugger/continue! run-id :abort :failed)
        (let [thrown (deref running 5000 ::timeout)]
          (is (instance? clojure.lang.ExceptionInfo thrown))
          (is (= :tripwire-tripped (:outcome (ex-data thrown))))
          (is (boolean (:debugger/discharged? (ex-data thrown)))))
        (dbg-wait-gone run-id)
        (is (= 1 (count (read-firings firing-root)))
            "exactly one firing record for the single stop")
        (is (= 1 (:repeats (:T8 (tripwire/firing-tally (.getPath firing-root)))))
            "the :end :error re-emission was counted as a repeat")
        ;; The wire is re-evaluated on the :end :error emission and the repeat
        ;; is counted, not stopped (the log line above shows it); the proof
        ;; that no SECOND stop happened is that nothing is stoppable anymore.
        (is (empty? (filter #(= run-id (:run-id %)) (debugger/stopped)))
            "the stop registry is empty: exactly one debugger stop happened")
        (is (= :debugger/run-not-stopped
               (try (debugger/continue! run-id :continue)
                    nil
                    (catch clojure.lang.ExceptionInfo e
                      (:kind (ex-data e)))))
            "a second stop would be continuable; there is none"))
      (finally (debugger/detach!)))))

(deftest detached-witness-keeps-note-behaviour-and-writes-no-firing
  (let [run-id (str "dbg-detached-" (System/nanoTime))
        report-root (temp-dir) firing-root (temp-dir)
        gate (atom true)
        opts (gated-witness-opts run-id report-root firing-root gate)
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/detach!)
    (with-redefs [tripwire/evaluate-wire
                  (fn [_wire _obs] (if @gate [{:kind :test-witness}] []))]
      (is (= record (tripwire/observe! opts record))
          "detached: record-and-continue, byte-for-byte the note! behaviour")
      (is (= 1 (count (filter #(.isFile %) (file-seq report-root)))))
      (is (empty? (read-firings firing-root))
          "detached trips produce no firing records"))))

(deftest two-arg-continue-records-unrecorded-disposition
  (let [run-id (str "dbg-unrecorded-" (System/nanoTime))
        report-root (temp-dir) firing-root (temp-dir)
        gate (atom true)
        opts (gated-witness-opts run-id report-root firing-root gate)
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/attach!)
    (try
      (let [running (future
                      (with-redefs [tripwire/evaluate-wire
                                    (fn [_wire _obs] (if @gate [{:kind :test-witness}] []))]
                        (tripwire/observe! opts record)))]
        (dbg-wait-for-stop run-id)
        (debugger/continue! run-id :continue)
        (is (= record (deref running 3000 ::timeout)))
        (is (= [:unrecorded] (mapv :disposition (read-firings firing-root)))))
      (finally (debugger/detach!)))))

(deftest firing-tally-counts-by-disposition-per-wire
  (let [root (temp-dir)
        write! (fn [wire disposition]
                 (tripwire/write-firing! (.getPath root)
                                         {:trip/id "trip-t"
                                          :trip/wire-id wire
                                          :run-id "r"
                                          :phase :p :transition :start
                                          :restart-choice :continue
                                          :disposition disposition
                                          :report-path "x" :dwell-ms 1}))]
    (write! :T8 :nothing-to-fix)
    (write! :T8 :nothing-to-fix)
    (write! :T8 :nothing-to-fix)
    (write! :T8 :fixed)
    (write! :T7 :undecided)
    (is (= {:T8 {:total 4 :nothing-to-fix 3 :fixed 1}
            :T7 {:total 1 :undecided 1}}
           (tripwire/firing-tally (.getPath root))))))

(deftest run-phase-tripwire-abort-restops-without-the-discharged-mark
  ;; The bad case the mark is named for, end to end: with
  ;; stoppable-failure? ignoring :debugger/discharged? (its pre-2026-10-22
  ;; body), the abort thrown on the :end emission is caught by run-phase!
  ;; and stops the run a SECOND time.
  (let [run-id (str "dbg-run-abort-bad-" (System/nanoTime))
        opts {:tripwire/report-root (.getPath (temp-dir))
              :tripwire/firing-root (.getPath (temp-dir))
              :run-id run-id
              :tripwire/disabled-wire-ids
              (vec (keys (dissoc @tripwire/wire-registry :T8)))
              :phase-events (atom [])
              :phase-log-fn (constantly nil)}]
    (debugger/attach!)
    (try
      (with-redefs [debugger/stoppable-failure? (constantly true)
                    tripwire/evaluate-wire
                    (fn [_wire obs]
                      (if (= :end (:transition obs)) [{:kind :test-witness}] []))]
        (let [running (future
                        (try
                          (runner/run-phase! opts {:opportunity-id "op"
                                                   :attempt-id "attempt"}
                                             :phase-x (constantly :computed))
                          (catch Throwable e e)))
              first-stop (dbg-wait-for-stop run-id)]
          (debugger/continue! run-id :abort :failed)
          (let [second-stop (dbg-wait-for-new-stop run-id (:stopped-at first-stop))]
            (is (= :tripwire-tripped (get-in second-stop [:condition :kind]))
                "without the mark the same tripwire abort stops the run again"))
          (debugger/continue! run-id :abort)
          (deref running 5000 ::timeout)))
      (finally (debugger/detach!)))))

(deftest same-run-repeat-is-counted-not-stopped
  (let [run-id (str "dbg-repeat-" (System/nanoTime))
        firing-root (temp-dir)
        opts (gated-witness-opts run-id (temp-dir) firing-root (atom true))
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/attach!)
    (try
      (with-redefs [tripwire/evaluate-wire
                    (fn [_wire _obs] [{:kind :test-witness}])]
        (let [running (future (tripwire/observe! opts record))]
          (dbg-wait-for-stop run-id)
          (debugger/continue! run-id :continue :nothing-to-fix)
          (is (= record (deref running 3000 ::timeout))))
        (is (= record (tripwire/observe! opts record))
            "the repeat returns at once instead of stopping")
        (is (empty? (filter #(= run-id (:run-id %)) (debugger/stopped))))
        (is (= {:T8 {:total 1 :nothing-to-fix 1 :repeats 1}}
               (tripwire/firing-tally (.getPath firing-root)))))
      (finally (debugger/detach!)))))

(deftest retry-witnessed-by-another-wire-stops-under-that-wire
  (let [run-id (str "dbg-retry-other-" (System/nanoTime))
        firing-root (temp-dir)
        current (atom :T8)
        opts (assoc (gated-witness-opts run-id (temp-dir) firing-root (atom true))
                    :tripwire/disabled-wire-ids
                    (vec (keys (dissoc @tripwire/wire-registry :T8 :T7))))
        record {:phase :construction :transition :start :attempt-id "a1"}]
    (debugger/attach!)
    (try
      (with-redefs [tripwire/evaluate-wire
                    (fn [wire _obs] (if (= wire @current) [{:kind :test-witness}] []))]
        (let [running (future (tripwire/observe! opts record))
              first-stop (dbg-wait-for-stop run-id)]
          (reset! current :T7)
          (debugger/continue! run-id :retry :fixed)
          (let [second-stop (dbg-wait-for-new-stop run-id (:stopped-at first-stop))]
            (is (= :T7 (get-in second-stop [:ex-data :tripwire/wire-id]))))
          (debugger/continue! run-id :continue :nothing-to-fix)
          (is (= record (deref running 3000 ::timeout)))
          (is (= {:T8 {:total 1 :fixed 1} :T7 {:total 1 :nothing-to-fix 1}}
                 (tripwire/firing-tally (.getPath firing-root))))))
      (finally (debugger/detach!)))))
