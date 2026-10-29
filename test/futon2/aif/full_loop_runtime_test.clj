(ns futon2.aif.full-loop-runtime-test
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-field-observation :as meta-field]
            [futon2.aif.ticket-readiness-inputs :as readiness-inputs]
            [futon2.aif.ticket-readiness :as ticket-readiness]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.reading-policy-precision :as reading-precision]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.report.war-machine :as wm]))

(defn- run-supplied-task! [opts]
  (runtime/run-opportunity! (merge {:task {:id "M-production"}} opts)))

(defn- digest [x] (evidence/value-digest x))

(defn- f32-root []
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             "wm-f32-carried-bindings"
             (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- f32-fixture [name]
  (edn/read-string
   (slurp (str "test/futon2/aif/fixtures/click-7-" name ".edn"))))

(defn- f32-seal [request status payload]
  (let [work-order (:work-order request)]
    (coordinator/seal-port-receipt
     {:phase (:phase request) :injury-id (:injury-id work-order)
      :repair-id (:repair-id work-order) :attempt (:attempt work-order)
      :predecessor-pin (:predecessor-pin request)
     :status status :payload payload})))

(deftest production-default-readiness-unavailable-is-fair-and-preview-free
  (let [dir (f32-root)
        manifest (str dir "/readiness.edn")
        contacts (atom 0)
        snapshot {:graph {:section-status {}}
                  :agency-source {:path "fixture://agency" :sha256 (digest :agency)}
                  :agency {:agents {}}}
        opts {:ticket-readiness-manifest-path manifest
              :author "fixture-unavailable"}
        config (with-redefs [readiness-inputs/fetch-pipeline-snapshot
                             (constantly snapshot)
                             runner/agent-roster (constantly {})
                             flight-runner/agency-answer-fn
                             (fn [_]
                               (fn [_]
                                 (swap! contacts inc)
                                 {:state :not-contacted}))]
                 (runtime/ticket-readiness-refresh-config opts))
        results (mapv (fn [_] (ticket-readiness/refresh-one! config)) (range 3))
        before (slurp manifest)
        preview (with-redefs [flight-runner/agency-answer-fn
                              (fn [_]
                                (fn [_]
                                  (swap! contacts inc)
                                  {:state :not-contacted}))]
                  ;; Constructing production defaults is the public preview-side
                  ;; composition boundary: it must not execute refresh-one!.
                  (runtime/production-defaults opts))]
    (is (= 0 @contacts) "unavailable designated producer performs no transport")
    (is (= 3 (count (distinct (map :ticket-id results))))
        "durable last-attempt ordering advances across three canonical tickets")
    (is (every? #(= :unknown (get-in % [:assessment :classification])) results))
    (is (= before (slurp manifest)) "read-only composition does not assess or write")
    (is (fn? (:ticket-readiness-refresh preview)))
    (is (every? #(re-matches #"T-.+" (:ticket-id %)) results))))

(defn- readiness-proposal-with-sources [n]
  {:schema :wm/cascade-proposal-request-v1
   :status :proposed
   :origin :retrieval-proposed
   :target {:id "T-evidence-pinned-to-mutable-prose-26082026"
            :path "/home/joe/code/futon3c/holes/tickets/T-evidence-pinned-to-mutable-prose-26082026.md"
            :sha256 (apply str (repeat 64 "a"))}
   :sources
   (mapv (fn [i]
           {:id (str "/retained/source/" i "#" (apply str (repeat 64 "b")))
            :path (str "/retained/source/" i)
            :sha256 (apply str (repeat 64 "b"))
            :revision (apply str (repeat 40 "c"))
            :body (apply str (repeat 4096 "unbounded source body "))})
         (range n))})

(deftest retained-readiness-shape-produces-a-bounded-pinned-prompt
  ;; Iteration 8's failed prompt was for this exact ticket and began with an
  ;; unbounded :sources vector.  Preserve that population shape, including
  ;; bodies which the old `(pr-str proposal)` sent to the provider.
  (let [proposal (readiness-proposal-with-sources 64)
        old-chars (count (str "Proposal: " (pr-str proposal)))
        result (#'runtime/ticket-readiness-prompt
                {:ticket-readiness-prompt-budget-chars
                 runtime/default-ticket-readiness-prompt-budget-chars
                 :ticket-readiness-source-summary-limit
                 runtime/default-ticket-readiness-source-summary-limit}
                proposal)]
    (is (> old-chars runtime/default-ticket-readiness-prompt-budget-chars))
    (is (= :ready (:status result)))
    (is (< (:prompt-chars result) (:prompt-budget-chars result)))
    (is (= 64 (get-in result [:proposal-summary :source-count])))
    (is (= runtime/default-ticket-readiness-source-summary-limit
           (count (get-in result [:proposal-summary :source-pins]))))
    (is (not (clojure.string/includes? (:prompt result) "unbounded source body")))))

(deftest over-budget-readiness-refuses-before-provider-contact
  (let [contacts (atom 0)
        proposal (readiness-proposal-with-sources 1)
        config (with-redefs [flight-runner/agency-answer-fn
                             (fn [_]
                               (fn [_]
                                 (swap! contacts inc)
                                 {:state :completed :text "unexpected"}))]
                 (runtime/ticket-readiness-refresh-config
                  {:author "wm-author"
                   :ticket-readiness-prompt-budget-chars 1
                   :ticket-readiness-source-summary-limit 1}))
        result ((:answer-fn config) proposal)]
    (is (= 0 @contacts))
    (is (= :refused (:status result)))
    (is (= :readiness/prompt-over-budget (:reason result)))
    (is (pos? (:prompt-chars result)))
    (is (= 1 (:prompt-budget-chars result)))))

(deftest claimed-injury-carries-intake-bindings-from-its-transaction
  (let [dir (f32-root)
        ledger-path (str dir "/ledger.edn")
        transaction-path (str dir "/transaction.edn")
        history (f32-fixture "ledger")
        transaction (f32-fixture "transaction")
        order (:work-order transaction)
        calls (atom {:diagnosis 0 :acceptance 0 :run 0 :admission-review 0})
        never (fn [key]
                (fn [& _]
                  (swap! calls update key inc)
                  {:status :refused :reason :unexpected-intake-call}))
        ports {:author #(f32-seal % :succeeded
                                  {:patch-sha (apply str (repeat 40 "a"))
                                   :worktree dir})
               :review #(f32-seal % :failed {:reason :review-offline})
               :enact #(f32-seal % :failed {:reason :unused})
               :retry #(f32-seal % :failed {:reason :unused})}
        _ (spit ledger-path (str (pr-str history) "\n"))
        _ (spit transaction-path (str (pr-str transaction) "\n"))
        result (run-supplied-task!
                {:meta-injury-ledger-path ledger-path
                 :meta-repair-transaction-path transaction-path
                 :meta-diagnosis-fn (never :diagnosis)
                 :meta-acceptance-fn (never :acceptance)
                 :meta-acceptance-run-fn (never :run)
                 :meta-acceptance-review-fn (never :admission-review)
                 :meta-repair-ports ports
                 :author (:author order) :reviewer (:reviewer order)
                 :repair-reviewer "fixture/repair-reviewer"
                 :author-session "fixture-author-attempt-two"
                 :reviewer-session "fixture-reviewer-attempt-two"
                 :repair-reviewer-session "fixture-repair-reviewer-attempt-two"
                 :target-repository (:target-repository order)
                 :target-worktree dir})]
    (is (= {:diagnosis 0 :acceptance 0 :run 0 :admission-review 0} @calls))
    (is (= {:status :present :source :supplied}
           (select-keys (:diagnosis-observation result) [:status :source])))
    (is (= :present (get-in result [:acceptance-observation :status])))
    (is (= :carried-transaction (get-in result [:carried-bindings :source])))
    (is (= :dedicated-meta-self-heal (:route result)))
    (is (= 2 (:attempt result)))
    (is (= 1 (get-in result [:continued-from 0 :attempt])))))

(deftest missing-transaction-preserves-diagnosis-production
  (let [dir (f32-root)
        ledger-path (str dir "/ledger.edn")
        calls (atom 0)
        _ (spit ledger-path (str (pr-str (f32-fixture "ledger")) "\n"))
        result (run-supplied-task!
                {:meta-injury-ledger-path ledger-path
                 :meta-repair-transaction-path (str dir "/missing.edn")
                 :meta-diagnosis-fn (fn [& _]
                                      (swap! calls inc)
                                      {:status :unavailable
                                       :reason :fixture-unavailable})})]
    (is (= 1 @calls))
    (is (= {:status :absent :reason :no-transaction}
           (:carried-bindings result)))))

(deftest production-registers-the-bounded-beta-campaign
  (runtime/production-defaults {})
  (let [status (reading-precision/campaign-status)]
    (is (= :registered (:status status)))
    (is (= 1 (:beta status)))
    (is (= 4 (:identity-limit status)))
    (is (= 4 (:remaining status)))))

(deftest occurrence-source-carries-the-actions-validated-square
  (let [square {:schema :wm/three-halves-square-v1 :fixture true}
        entry {:target "M-square"
               :action {:id "policy-square"
                        :arrangement
                        {:nodes [{:occurrence-id ["p/x" 0]
                                  :fragment-span {:start 1 :end 2}
                                  :source-sha256 (apply str (repeat 64 "a"))
                                  :blend-square square}]}}}
        readings (cascade-decision/occurrence-readings
                  entry {:excerpt-digest (digest :excerpt)
                         :analysis-digest (digest :analysis)})]
    (is (= square (get-in readings [["p/x" 0] :square])))
    (is (= ["p/x" 0] (first (keys readings))))))

(deftest production-composition-installs-the-reviewed-same-click-handler
  ;; No active injury: the production gate reads the ledger and transaction
  ;; paths it is given, never the live data/wm-meta-injuries files.
  (let [seen (atom nil)
        dir (f32-root)
        d (digest :cascade)
        holes [{:hole-id :h :owner :wm :question :operation}]
        patch {:namespace "futon2.aif.full-loop-runtime-test"
               :var "identity"
               :source-digest (digest :source)}
        error {:kind :wm/open-cascade-execution-error
               :cascade-digest d :holes holes :phase :compile :attempts []
               :resume-port {:retry (fn [_] {:cascade-digest d :outcome :redone})}}
        fenced (fn [value]
                 {:state "done"
                  :result (str "```json\n" (json/generate-string value) "\n```")})
        author-answer (fn [req]
                        (fenced
                         {:requisition-id (:requisition-id req)
                          :cascade-digest d :by "wm-author"
                          :decisions {:h {:decision :implement}}
                          :patch patch}))
        reviewer-answer
        (fn [{:keys [requisition author-answer]}]
          (fenced
           (assoc author-answer :requisition-id (:requisition-id requisition)
                  :accepted-by "wm-reviewer")))]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    ((:open-cascade-resume-fn opts) error))]
      (is (= :resumed
             (:status
              (run-supplied-task!
               {:run-id "resume-production"
                :meta-injury-ledger-path (str dir "/ledger.edn")
                :meta-repair-transaction-path (str dir "/transaction.edn")
                :author "wm-author" :reviewer "wm-reviewer"
                :open-cascade-author-answer-fn author-answer
                :open-cascade-reviewer-answer-fn reviewer-answer
                :open-cascade-patch-loader (constantly identity)})))))
    (is (fn? (:open-cascade-resume-fn @seen)))
    (is (some? (:click-budget @seen)))
    (is (some? (:selection-counter @seen)))
    (is (= :held (:status (open-resume/recognize error))))))

(deftest production-resume-contacts-the-explicit-cast-in-work-mode
  (let [configurations (atom [])
        live-ledger (atom {:order [] :jobs {}})]
    (with-redefs [flight-runner/agency-answer-fn
                  (fn [configuration]
                    (swap! configurations conj configuration)
                    (constantly {:state :not-contacted}))]
      (let [handler (runtime/open-cascade-resume-handler
                     {:author "wm-author" :reviewer "wm-reviewer"})]
        (is (fn? handler))
        ;; Contacts are constructed at invocation, after the runner has minted
        ;; the registered ledger, rather than against stale composition opts.
        (handler {:kind :not-a-held-cascade}
                 {:registered-run/job-ledger live-ledger})))
    (is (= ["wm-author" "wm-reviewer"] (mapv :seat @configurations)))
    (is (= ["work" "work"]
           (mapv #(get-in % [:opts :invoke-mode]) @configurations)))
    (is (every? #(identical? live-ledger
                             (get-in % [:opts :registered-run/job-ledger]))
                @configurations))))

(deftest flight-runner-does-not-load-the-report
  ;; No :test alias in the child: since b463d85cc (2026-10-20) the alias
  ;; carries :main-opts for cognitect.test-runner, so "-M:test -e" started
  ;; the whole suite again, including this test, without end.  The child
  ;; only needs src, which the root :paths provide.
  (let [{:keys [exit out err]}
        (shell/sh "clojure" "-M" "-e"
                  (str "(require 'futon2.aif.flight-runner) "
                       "(prn (boolean (find-ns 'futon2.report.war-machine)))"))]
    (is (zero? exit) err)
    ;; JVM startup may emit container thread/timing diagnostics on stdout;
    ;; the child contract is its final EDN form.
    (is (= false (edn/read-string (last (str/split-lines out)))))))

(deftest runtime-supplies-the-report-backed-judge
  (let [seen (atom nil)
        opts {:run-id "runtime-test" :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"}
        expected {:judgement :selected}]
    (with-redefs [wm/accumulation-config (constantly {:accumulation :configured})
                  wm/generate-war-machine
                  (fn [days judge-opts]
                    (reset! seen [days judge-opts])
                    expected)]
      (let [defaults (runtime/production-defaults opts)]
        (is (fn? (:cascade-revision-proposals-fn defaults)))
        (is (= expected ((:judge-fn defaults) 17))))
      (is (= 17 (first @seen)))
      (let [actual (dissoc (second @seen) :outer-task-selection-fn)
            observation (:meta-field-observation actual)
            source-pin (:meta-field-source-pin actual)]
        (is (= (merge {:accumulation :configured
              :run-id "runtime-test"
              :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"
              :trace? false
              :include-advisory-lanes? false
              :defer-render? true}
                      {:meta-field-observation observation
                       :meta-field-source-pin source-pin})
               actual))
        (is (= :verified
               (:status (meta-field/verify
                         observation {:expected-snapshot-pin source-pin}))))
        (is (= :external-snapshot-pin-mismatch
               (:reason (meta-field/verify
                         observation
                         {:expected-snapshot-pin
                          (assoc source-pin :sha256 (apply str (repeat 64 "0")))}))))
        (is (= :external-snapshot-authority-invalid
               (:reason (meta-field/verify observation
                                           {:expected-snapshot-pin nil})))))
      (is (nil? (:outer-task-selection-fn (second @seen)))))))

(deftest selected-target-reading-contacts-only-the-designated-author
  (let [captured (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"random-idle-seat"
                               {:status :idle :invoke-ready? true}})
                  selected-target-reading/repair!
                  (fn [opts _]
                    (reset! captured ((:answer-fn opts) {:target "M-selected"}))
                    @captured)]
      (let [defaults (runtime/production-defaults
                      {:run-id "designated-author" :author "wm-author"
                       :reviewer "wm-reviewer"
                       :repair-reviewer "wm-repair-reviewer"})]
        ((:selected-target-reading-fn defaults) {} {})))
    (is (= {:state :not-contacted
            :reason :designated-author-unavailable
            :author "wm-author"}
           @captured))))

(deftest production-runner-receives-and-exercises-selected-target-reading-port
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        runner-opts (atom nil)
        contacted (atom [])
        repair-result {:schema :wm/selected-target-reading-request-v1
                       :status :refused :published false
                       :kind :probe-complete}]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:selected-target-reading-fn opts)
                     {}
                     {:outer-task-selection {:chosen {:id "M-production"}}})
                    {:status :ran})
                  selected-target-reading/repair!
                  (fn [opts _]
                    ((:answer-fn opts) {:target "M-production"})
                    repair-result)]
      (is (= :ran
             (:status
              (run-supplied-task!
               (assoc isolated
                      :run-id "production-selected-reading"
                      :author "wm-author"
                      :reviewer "wm-reviewer"
                      :repair-reviewer "wm-repair-reviewer"
                      :selected-target-reading-answer-fn
                      (fn [issued]
                        (swap! contacted conj [(:target issued) "wm-author"])
                        {:state :not-contacted :reason :test-probe})))))))
    (is (fn? (:selected-target-reading-fn @runner-opts))
        "the real production wrapper must pass the port on runner opts")
    (is (= [["M-production" "wm-author"]] @contacted))))

(deftest production-runner-receives-occurrence-square-authoring-port
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    ((:occurrence-square-authoring-fn opts) {} {})
                    {:status :ran})
                  square-authoring/repair! (fn [opts _]
                                             (is (fn? (:answer-fn opts)))
                                             {:status :probe})]
      (is (= :ran (:status
                   (run-supplied-task!
                    (assoc isolated
                           :run-id "production-square-authoring"
                           :author "wm-author" :reviewer "wm-reviewer"))))))
    (is (true? (:require-occurrence-squares? @seen)))
    (is (fn? (:occurrence-square-authoring-fn @seen)))))

(deftest production-runner-receives-ticket-readiness-refresh-port
  (let [dir (f32-root)
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    {:status :ran})]
      (is (= :ran (:status
                   (run-supplied-task!
                    {:meta-injury-ledger-path (str dir "/ledger.edn")
                     :meta-repair-transaction-path (str dir "/transaction.edn")
                     :run-id "production-ticket-readiness"
                     :author "wm-author" :reviewer "wm-reviewer"})))))
    (is (fn? (:ticket-readiness-refresh @seen))
        "the runner reads readiness refresh from explicit options")))

(deftest readiness-dispatch-uses-the-run-local-job-ledger
  (let [dir (f32-root)
        ledger (atom {:order [] :jobs {}})
        posted (atom nil)
        resolved-opts (atom nil)
        failure (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  http/post (fn [_ request]
                              (reset! posted (json/parse-string (:body request) true))
                              {:status 200 :body "{\"job-id\":\"readiness-job\"}"})
                  runner/poll-job! (fn [_ _] {:state "done" :result "{}"})
                  runner/read-job! (fn [opts _]
                                     (reset! resolved-opts opts)
                                     {:state "done"})
                  ticket-readiness/refresh-one!
                  (fn [config]
                    (try
                      ((:answer-fn config) {:target "T-readiness"})
                      (catch clojure.lang.ExceptionInfo e
                        (reset! failure (ex-data e))))
                    ((:contact-resolver config) "readiness-job"))
                  runner/run-opportunity!
                  (fn [opts]
                    (let [live-opts (assoc opts :registered-run/job-ledger ledger
                                               :click-id "readiness-live-click")]
                      (runner/ticket-readiness-refresh-step live-opts)
                      {:status :ran}))]
      (run-supplied-task!
       {:meta-injury-ledger-path (str dir "/ledger.edn")
        :meta-repair-transaction-path (str dir "/transaction.edn")
        :ticket-readiness-manifest-path (str dir "/readiness.edn")
        :run-id "readiness-live-run" :author "wm-author"}))
    (is (nil? @failure) (pr-str @failure))
    (is (= "readiness-live-click" (get-in @posted [:harness :source-ref])))
    (is (= "readiness-live-run" (get-in @posted [:harness :execution-id])))
    (is (= ["readiness-job"] (:order @ledger)))
    (is (= "readiness-live-click" (get-in @ledger [:jobs "readiness-job" :click-id])))
    (is (identical? ledger (:registered-run/job-ledger @resolved-opts)))
    (is (= "readiness-live-click" (:click-id @resolved-opts)))))

(deftest selected-target-answer-is-built-with-the-run-local-job-ledger
  (let [ledger (atom {:order [] :jobs {}})
        answer-opts (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  flight-runner/agency-answer-fn
                  (fn [{:keys [opts]}]
                    (reset! answer-opts opts)
                    (fn [_] {:state :not-contacted :reason :test-probe}))
                  selected-target-reading/repair!
                  (fn [opts _]
                    ((:answer-fn opts) {:target "M-production"}))]
      (let [defaults (runtime/production-defaults
                      {:run-id "production-selected-reading"
                       :author "wm-author"})]
        ((:selected-target-reading-fn defaults)
         {:click-id "wm-click-probe"
          :registered-run/job-ledger ledger}
         {})))
    (is (identical? ledger (:registered-run/job-ledger @answer-opts)))
    (is (= "wm-click-probe" (:click-id @answer-opts)))))

(deftest selected-policy-reading-is-wired-late-to-the-designated-author
  (let [ledger (atom {:order [] :jobs {}})
        answer-opts (atom nil)
        prompt-fn (atom nil)]
    (with-redefs [runner/agent-roster
                  (constantly {"wm-author" {:status :idle :invoke-ready? true}})
                  flight-runner/agency-answer-fn
                  (fn [{:keys [opts] :as configuration}]
                    (reset! answer-opts opts)
                    (reset! prompt-fn (:prompt-fn configuration))
                    (fn [_] {:request-id "r" :verdicts [] :by "wm-author"}))]
      (let [defaults (runtime/production-defaults
                      {:run-id "production-policy-reading"
                       :author "wm-author"})
            ask ((:selected-reading-ask-builder defaults)
                 {:click-id "wm-click-policy-reading"
                  :registered-run/job-ledger ledger})]
        (is (map? (ask {:request-id "r"}))))
      (is (identical? ledger (:registered-run/job-ledger @answer-opts)))
      (is (= "wm-click-policy-reading" (:click-id @answer-opts)))
      (is (identical? selected-reading/prompt @prompt-fn)))))

(deftest runner-refuses-an-absent-runtime-default
  (binding [runner/*runtime-defaults* nil]
    (let [failure (try
                    (runner/runtime-default {} :judge-fn)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (some? failure))
      (is (= {:failure-kind :missing-runtime-default
              :missing-default :judge-fn
              :supplied-by 'futon2.aif.full-loop-runtime}
             (ex-data failure))))))

(deftest production-composition-supplies-the-feedback-store
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    {:status :ran})]
      (is (= :ran (:status (run-supplied-task!
                            (assoc isolated :run-id "feedback-default")))))
      (is (= cascade-feedback/default-path
             (:cascade-feedback-path @seen)))
      (is (= runtime/default-pattern-graph-path (:pattern-graph-path @seen)))
      (is (= runtime/default-pattern-graph-diff-dir (:pattern-graph-diff-dir @seen)))
      (is (map? (:cascade-feedback-metadata @seen))))))

(deftest production-composition-shares-one-selection-timing-collector
  (let [dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}
        runner-opts (atom nil)
        judge-opts (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    {:status :ran})
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (:status (run-supplied-task!
                            (assoc isolated :run-id "timing-state")))))
      (is (instance? clojure.lang.IAtom
                     (:selection-timing/state @runner-opts)))
      (is (identical? (:selection-timing/state @runner-opts)
                      (:selection-timing/state @judge-opts))))))

(deftest production-mints-run-identity-before-closing-over-the-judge
  (let [runner-opts (atom nil)
        judge-opts (atom nil)
        dir (f32-root)
        isolated {:meta-injury-ledger-path (str dir "/ledger.edn")
                  :meta-repair-transaction-path (str dir "/transaction.edn")}]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    {:status :ran})
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (:status (run-supplied-task! isolated))))
      (is (string? (:run-id @runner-opts)))
      (is (= (:run-id @runner-opts) (:run-id @judge-opts))
          "accumulation and the durable run record must receive one identity"))))
