(ns futon2.aif.full-loop-runtime-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.flight-runner :as flight-runner]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.reading-policy-precision :as reading-precision]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.open-cascade-resume :as open-resume]
            [futon2.report.war-machine :as wm]))

(defn- digest [x] (evidence/value-digest x))

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
  (let [seen (atom nil)
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
              (runtime/run-opportunity!
               {:run-id "resume-production"
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
  (let [{:keys [exit out err]}
        (shell/sh "clojure" "-M:test" "-e"
                  (str "(require 'futon2.aif.flight-runner) "
                       "(prn (boolean (find-ns 'futon2.report.war-machine)))"))]
    (is (zero? exit) err)
    (is (= false (edn/read-string out)))))

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
      (is (= {:accumulation :configured
              :run-id "runtime-test"
              :flight {:target "T"}
              :cascade-feedback-path "/tmp/runtime-test-feedback.edn"
              :outer-task-policy :meta
              :trace? false
              :include-advisory-lanes? false
              :defer-render? true}
             (dissoc (second @seen) :outer-task-selection-fn)))
      (is (ifn? (:outer-task-selection-fn (second @seen)))))))

(deftest production-pins-one-outer-selection-across-redecision
  (let [calls (atom [])
        invocations (atom 0)
        selector (fn [input]
                   (swap! calls conj input)
                   {:chosen {:id (str "M-choice-" (count @calls))}})
        defaults
        (with-redefs [wm/accumulation-config (constantly {})
                      wm/generate-war-machine
                      (fn [_ opts]
                        (let [n (swap! invocations inc)
                              receipt ((:outer-task-selection-fn opts)
                                       {:tasks [(str "population-" n)]})]
                          {:judgement {:outer-task-selection receipt}}))]
          (let [defaults (runtime/production-defaults
                          {:run-id "same-click"
                           :outer-task-selection-fn selector})]
            ((:judge-fn defaults) 1)
            ((:judge-fn defaults) 1)
            defaults))]
    (is (= 1 (count @calls)))
    (is (= {:tasks ["population-1"]} (first @calls)))
    (is (= 2 @invocations))
    (is (fn? (:judge-fn defaults)))))

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
  (let [runner-opts (atom nil)
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
                    :ran)
                  selected-target-reading/repair!
                  (fn [opts _]
                    ((:answer-fn opts) {:target "M-production"})
                    repair-result)]
      (is (= :ran
             (runtime/run-opportunity!
              {:run-id "production-selected-reading"
               :author "wm-author"
               :reviewer "wm-reviewer"
               :repair-reviewer "wm-repair-reviewer"
               :selected-target-reading-answer-fn
               (fn [issued]
                 (swap! contacted conj [(:target issued) "wm-author"])
                 {:state :not-contacted :reason :test-probe})}))))
    (is (fn? (:selected-target-reading-fn @runner-opts))
        "the real production wrapper must pass the port on runner opts")
    (is (= [["M-production" "wm-author"]] @contacted))))

(deftest production-runner-receives-occurrence-square-authoring-port
  (let [seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    ((:occurrence-square-authoring-fn opts) {} {})
                    :ran)
                  square-authoring/repair! (fn [opts _]
                                             (is (fn? (:answer-fn opts)))
                                             {:status :probe})]
      (is (= :ran (runtime/run-opportunity!
                   {:run-id "production-square-authoring"
                    :author "wm-author" :reviewer "wm-reviewer"}))))
    (is (true? (:require-occurrence-squares? @seen)))
    (is (fn? (:occurrence-square-authoring-fn @seen)))))

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
  (let [seen (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! seen opts)
                    :ran)]
      (is (= :ran (runtime/run-opportunity! {:run-id "feedback-default"})))
      (is (= cascade-feedback/default-path
             (:cascade-feedback-path @seen)))
      (is (= runtime/default-pattern-graph-path (:pattern-graph-path @seen)))
      (is (= runtime/default-pattern-graph-diff-dir (:pattern-graph-diff-dir @seen)))
      (is (map? (:cascade-feedback-metadata @seen))))))

(deftest production-composition-shares-one-selection-timing-collector
  (let [runner-opts (atom nil)
        judge-opts (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    :ran)
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (runtime/run-opportunity! {:run-id "timing-state"})))
      (is (instance? clojure.lang.IAtom
                     (:selection-timing/state @runner-opts)))
      (is (identical? (:selection-timing/state @runner-opts)
                      (:selection-timing/state @judge-opts))))))

(deftest production-mints-run-identity-before-closing-over-the-judge
  (let [runner-opts (atom nil)
        judge-opts (atom nil)]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (reset! runner-opts opts)
                    ((:judge-fn runner/*runtime-defaults*) 1)
                    :ran)
                  wm/generate-war-machine
                  (fn [_ opts]
                    (reset! judge-opts opts)
                    {:judgement {}})]
      (is (= :ran (runtime/run-opportunity! {})))
      (is (string? (:run-id @runner-opts)))
      (is (= (:run-id @runner-opts) (:run-id @judge-opts))
          "accumulation and the durable run record must receive one identity"))))
