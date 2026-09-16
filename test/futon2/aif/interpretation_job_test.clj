(ns futon2.aif.interpretation-job-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [clojure.walk :as walk]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.find-receipt-test :as find-fixture]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.interpretation-evidence-test :as receipt-fixture]
            [futon2.aif.interpretation-job :as job]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-cohort :as cohort-store]
            [futon2.aif.full-loop-runner-test :as runner-fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic])
  (:import [java.nio.file Files]
           [java.util UUID]))

(use-fixtures :once hermetic/with-hermetic-stores runner-fixture/with-hermetic-traces)

(defn run-case [mode scenario]
  (let [{:keys [root] :as cohort} (#'runner-fixture/retention-cohort "interpretation-job-test")
        dir (io/file root "test-cohort-exhaustion" "attempt-001" "evidence")
        {:keys [record captured]} (find-fixture/sample)
        action (cond-> (get-in record [:identity :occurrence :action/value])
                 (= scenario :unsupported) (assoc :type :fire-pattern)
                 (= scenario :ticket) (assoc :type :advance-ticket :target "T-receipt-construction"))
        calls (atom []) constructors (atom 0) organised (atom 0) organise policy/organise saved (atom nil)
        adjudication-observations (atom [])
        append-checkpoint cohort-store/append-checkpoint!
        base (#'runner-fixture/retention-success-opts cohort)
        prepare (fn [a identity {:keys [on-capture]}]
                  (when (#{:interpretation/target-unresolved :interpretation/source-unavailable} scenario)
                    (throw (ex-info "controlled prepare refusal" {:interpretation/refusal scenario})))
                  (.mkdirs dir)
                  (doseq [[file bs] captured]
                    (Files/write (.toPath (io/file dir file)) bs (make-array java.nio.file.OpenOption 0)))
                  (doseq [s (:sources record)] (on-capture s))
                  (let [r (-> record (assoc :identity identity)
                              (assoc-in [:target :action] a)
                              (assoc-in [:target :id] (:target a))
                              (assoc-in [:target :kind] (if (= :advance-ticket (:type a)) :ticket :mission)))]
                    (reset! saved r)
                    (when (= :interpretation/no-citable-tension scenario)
                      (throw (ex-info "controlled empty tension" {:interpretation/refusal scenario :sources (:sources r)})))
                    (when (= :interpretation/retrieval-unavailable scenario)
                      (throw (ex-info "controlled failed retrieval" {:interpretation/refusal scenario :request r})))
                    (assoc r :schema :wm/interpretation-request-v1)))
        emit-receipt (fn []
                       (let [plan (when (#{:bad-reader :bad-parameter :bad-fact :bad-citation} scenario)
                                    [{:fact (if (= scenario :bad-fact) "not-declared" (get-in @saved [:facts 0 :id]))
                                      :reader (if (= scenario :bad-reader) :made-up :artifact)
                                      :parameters (cond-> {:repository "futon2" :path "component"}
                                                    (= scenario :bad-parameter) (assoc :outcome :success))
                                      :citations (cond-> (get-in @saved [:facts 0 :citations])
                                                   (= scenario :bad-citation) (assoc-in [0 :quote] "invented"))}])
                             _ (spit (io/file dir "interpretation-reader-plan.edn") (pr-str (vec plan)))
                             identity (:identity (edn/read-string (slurp (io/file dir "interpretation-job.source"))))
                             metadata (map job/source (filter #(and (.isFile %) (.startsWith (.getName %) "interpretation-"))
                                                              (.listFiles dir)))
                             r (-> @saved
                                   (assoc :identity identity :sources (into (:sources @saved) metadata))
                                   (update :interpretations
                                           (fn [xs] (mapv (fn [x]
                                                            (let [x (assoc x :author (:author identity))]
                                                              (assoc x :sha256 (evidence/value-digest (dissoc x :sha256))))) xs))))
                             r (cond (= scenario :nothing-fires) (assoc-in r [:facts 0 :value] false)
                                     (= scenario :no-relevant) (assoc r :interpretations [])
                                     (#{:genesis :unmeasurable} scenario)
                                     {:schema evidence/failure-schema :identity identity :stage :interpretation
                                      :sources (:sources r) :absent {:interpretations {:status :none :reason :not-interpretable}}
                                      :failure {:kind (if (= scenario :genesis) :interpretation/genesis-required
                                                         :interpretation/unmeasurable-fact)
                                                :identity identity :stage :interpretation
                                                :source-refs [] :elapsed-ms 1 :partial-artifacts []}}
                                     :else r)]
                         (when (= scenario :source-changed)
                           (spit (io/file dir (:file (first (:sources r)))) "changed source"))
                         (spit (io/file dir "interpretation-receipt.edn")
                               (if (= scenario :invalid) "this is not one valid receipt" (pr-str r)))))
        opts (assoc (cond-> base
                      (= scenario :success)
                      (assoc :author-artifact-observer-fn
                             (fn [repo _ _] {:repo repo :commit "abc123" :corroborates? true :disagreement? false})))
                    :trace-fn (constantly "/fixture/trace.edn") :run-id (str (UUID/randomUUID)) :interpretation-mode mode :interpreter "interpreter"
                    :judge-fn (fn [_] {:judgement (walk/postwalk-replace
                                                  {runner-fixture/selected-action action} runner-fixture/judgement)})
                    :interpretation-history-roots [root]
                    :interpretation-prepare-fn prepare
                    :interpretation-readiness-fn (fn [_ _]
                                                   (when (= scenario :unavailable)
                                                     (throw (ex-info "no interpreter" {:failure-kind :agent-unavailable}))))
                    :repo-head-observation-fn (fn [_] {:head "before" :observed-at-ms 1000})
                    :construct-fn (fn [_] (swap! constructors inc)
                                    {:shown [] :semilattice {:descent [] :co_app []}})
                    :dispatch-fn (fn [o actor caller mission prompt]
                                   (swap! calls conj actor)
                                   (if (= actor "interpreter") {:job-id "interpretation-one"}
                                       ((:dispatch-fn base) o actor caller mission prompt)))
                    :poll-fn (fn [o id]
                               (if (= id "interpretation-one")
                                 (if (= scenario :budget)
                                   (throw (ex-info "budget expired" {:outcome :incomplete :failure-kind :agent-budget-expired
                                                                    :job-id id}))
                                   (do (emit-receipt) {:state "done" :job-id id}))
                                 ((:poll-fn base) o id))))]
    (try
      (let [result (binding [runner/*wm-status-reporting?* false]
                     (with-redefs [runner/ensure-dispatch-seat! (fn [_])
                                   cohort-store/append-checkpoint!
                                   (fn [& args]
                                     (when (some #{:adjudication} args)
                                       (swap! adjudication-observations conj
                                              (.exists (io/file dir "fact-end.edn"))))
                                     (apply append-checkpoint args))
                                   policy/organise (fn [& args] (swap! organised inc) (apply organise args))]
                       (runner/run-opportunity! opts)))
            failures (when (.exists dir)
                       (mapv #(edn/read-string (slurp %))
                             (filter #(.startsWith (.getName %) "interpretation-failure-") (.listFiles dir))))
            close-file (io/file (.getParentFile dir) "007-closed.edn")
            close (when (.exists close-file) (edn/read-string (slurp close-file)))]
        {:result result :calls @calls :action action :organised @organised :constructors @constructors :failures failures :close close
         :adjudication-observations @adjudication-observations
         :diagnostics (when (.exists dir)
                        (mapv #(edn/read-string (slurp %))
                              (filter #(.startsWith (.getName %) "interpretation-diagnostic-") (.listFiles dir))))
         :rejected-returns (when (.exists dir)
                             (mapv #(edn/read-string (slurp %))
                                   (filter #(.startsWith (.getName %) "interpretation-return-") (.listFiles dir))))
         :observations (into {} (for [n ["fact-pre.edn" "fact-end.edn"]
                                     :let [f (io/file dir n)] :when (.exists f)]
                                 [n (edn/read-string (slurp f))]))})
      (finally (doseq [file (reverse (file-seq (io/file root)))] (Files/delete (.toPath file)))))))

(deftest flag-off-preserves-construction-checkpoint
  (let [a (run-case nil :invalid) b (run-case :off :invalid)]
    (is (= 1 (:constructors a) (:constructors b)))
    (is (= (:calls a) (:calls b)))
    (is (not-any? #{"interpreter"} (:calls a)))
    (is (= (get-in a [:result :checkpoints :construction])
           (get-in b [:result :checkpoints :construction])))
    (is (empty? (:failures a)))))

(deftest valid-receipt-is-bound-and-admitted
  (let [{:keys [result close calls constructors organised action]} (run-case :receipt :valid)]
    (is (= 1 (count (filter #{"interpreter"} calls))))
    (is (zero? constructors))
    (is (= 1 organised))
    (is (= (pr-str action) (pr-str (get-in result [:checkpoints :construction :judgment :cascade :selected-action]))))
    (is (get-in result [:checkpoints :construction :judgment :interpretation-receipt :sha256]))
    (is (some #(.endsWith (:source-path %) "interpretation-receipt.edn")
              (get-in close [:payload :close-evidence-manifest :entries])))
    (is (= (count calls) (get-in close [:payload :judgment :resource-use :agent-turns])))))

(deftest failures-stop-before-old-construction-or-build
  (doseq [[scenario expected] [[:unavailable :interpretation/agent-unavailable]
                              [:invalid :interpretation/invalid-receipt]
                              [:no-relevant :interpretation/no-relevant-pattern]
                              [:nothing-fires :interpretation/no-relevant-pattern]
                              [:genesis :interpretation/genesis-required]
                              [:unmeasurable :interpretation/unmeasurable-fact]
                              [:source-changed :interpretation/source-changed]
                              [:unsupported :interpretation/action-type-unsupported]
                              [:interpretation/target-unresolved :interpretation/target-unresolved]
                              [:interpretation/source-unavailable :interpretation/source-unavailable]
                              [:interpretation/no-citable-tension :interpretation/no-citable-tension]
                              [:interpretation/retrieval-unavailable :interpretation/retrieval-unavailable]]]
    (let [{:keys [result constructors calls failures close]} (run-case :receipt scenario)]
      (is (zero? constructors) (str scenario))
      (is (every? #{"interpreter"} calls) (str scenario))
      (is (= expected (get-in failures [0 :failure :kind])) (str scenario))
      (is (= :environmental-hold (get-in result [:data :repair-obligation :repair/class]))
          (str scenario))
      (is (= evidence/failure-schema (:schema (first failures))))
      (is (seq (get-in close [:payload :close-evidence-manifest :entries])) (str scenario)))))

(deftest budget-closes-with-original-job-and-recoverable-obligation
  (let [{:keys [result close calls failures constructors]} (run-case :receipt :budget)]
    (is (= ["interpreter"] calls)) (is (zero? constructors))
    (is (= :incomplete (:outcome result)))
    (is (= :agent-budget-expired (get-in result [:data :failure-kind])))
    (is (= :interpretation/budget-exceeded (get-in failures [0 :failure :kind])))
    (is (= "interpretation-one" (get-in failures [0 :identity :interpreter-job])))
    (is (= :incomplete-recoverable (get-in result [:data :repair-obligation :repair/class])))
    (is (= "interpretation-one" (get-in result [:data :repair-obligation :failure-data :job-id])))
    (is (= 1 (get-in close [:payload :judgment :resource-use :agent-turns])))))

(deftest failure-schema-is-closed-and-success-stays-strict
  (let [{:keys [record]} @receipt-fixture/fixture
        identity (:identity record)
        failure {:schema evidence/failure-schema :identity identity :stage :prepare :sources []
                 :absent {:target {:status :none :reason :target-unresolved}}
                 :failure {:kind :interpretation/target-unresolved :identity identity :stage :prepare
                           :source-refs [] :elapsed-ms 0 :partial-artifacts []}}]
    (is (= failure (evidence/validate-record failure)))
    (let [machine (assoc-in failure [:failure :kind] :interpretation/machine-failure)]
      (is (= machine (evidence/validate-record machine))))
    (doseq [[mutate reason] [[#(assoc-in % [:absent :identity] {:status :none :reason :missing}) :absence-section-invalid]
                             [#(assoc % :target {}) :shape-invalid]
                             [#(assoc-in % [:failure :kind] :invented) :failure-kind-invalid]]]
      (is (= reason (receipt-fixture/refusal #(evidence/validate-record (mutate failure))))))
    (is (= :text-invalid (receipt-fixture/refusal #(evidence/validate-record (assoc-in record [:retrieval :query] "")))))))

(deftest interpretation-failures-never-open-a-machine-repair
  ;; A machine-failure obligation pins selection to stop-the-line repair;
  ;; an interpretation gap must instead wait for a valid successor attempt.
  (doseq [kind [:interpretation/no-relevant-pattern :interpretation/no-citable-tension
                :interpretation/invalid-receipt :interpretation/source-changed
                :interpretation/genesis-required :interpretation/retrieval-unavailable]]
    (is (= :environmental-hold (#'runner/repair-class-for kind)) (str kind)))
  (is (= :incomplete-recoverable (#'runner/repair-class-for :agent-budget-expired)))
  (is (= :environmental-hold (#'runner/repair-class-for :agent-unavailable)))
  (is (= :machine-failure (#'runner/repair-class-for :build-failed))))

(deftest construction-machine-faults-keep-their-repair-contract
  ;; Use the real receipt-mode runner with its namespace's hermetic stores.
  ;; A successful interpretation followed by a code fault is not a content gap.
  (doseq [[fault expected] [[(ex-info "controlled construction fault"
                                    {:failure-kind :build-failed}) :build-failed]
                            [(NullPointerException. "controlled null fault") :untyped-failure]
                            [(ex-info "wrapped fault" {}
                                      (ex-info "typed cause" {:failure-kind :build-failed})) :build-failed]
                            [(ex-info "duplicate dispatch" {:interpretation/refusal :interpretation/job-already-dispatched})
                             :interpretation/job-already-dispatched]
                            [(ex-info "attempt mismatch" {:interpretation/refusal :interpretation/attempt-identity-mismatch})
                             :interpretation/attempt-identity-mismatch]]]
    (let [caught (atom nil)
          run-job job/run!
          {:keys [result calls constructors failures close diagnostics rejected-returns]}
          (with-redefs [job/run! (fn [& args]
                                  (try (apply run-job args)
                                       (catch Exception e (reset! caught e) (throw e))))
                        construction/construct!
                                                          (fn [& _] (throw fault))]
            (run-case :receipt :valid))]
      (is (= ["interpreter"] calls))
      (is (zero? constructors))
      (is (= expected (get-in result [:data :failure-kind])))
      (is (= :machine-failure (get-in result [:data :repair-obligation :repair/class])))
      (is (identical? fault (.getCause ^Throwable @caught)))
      (is (= expected (get-in diagnostics [0 :failure-kind])))
      (is (= {:class (.getName (class fault)) :message (.getMessage ^Throwable fault) :data (ex-data fault)}
             (get-in diagnostics [0 :causes 0])))
      (is (= (:causes (first diagnostics)) (:interpretation/causes (ex-data @caught))))
      (is (= (if (#{:build-failed :untyped-failure} expected)
               :interpretation/machine-failure expected)
             (get-in failures [0 :failure :kind])))
      (is (seq (get-in close [:payload :close-evidence-manifest :entries])))
      (doseq [prefix ["interpretation-failure-" "interpretation-diagnostic-" "interpretation-return-"]]
        (is (some #(.startsWith (.getName (io/file (:source-path %))) prefix)
                  (get-in close [:payload :close-evidence-manifest :entries])) prefix))
      (is (string? (:returned-bytes-base64 (first rejected-returns)))))))

(deftest unknown-and-invariant-interpretation-kinds-are-machine-failures
  (doseq [kind [:interpretation/machine-failure :interpretation/new-unknown-kind
               :interpretation/job-already-dispatched :interpretation/attempt-identity-mismatch
               :interpretation/attempt-path-invalid :interpretation/action-mismatch
               :interpretation/retriever-set-invalid]]
    (is (= :machine-failure (#'runner/repair-class-for kind)) (str kind))))

(deftest interpretation-ports-preserve-the-runner-transport-policy
  (doseq [port [:ready! :dispatch! :poll!]
          wrapped? [false true]
          [make-fault expected] [[#(java.net.ConnectException. "Connection refused") :transport-unavailable]
                                 [#(java.net.SocketTimeoutException. "Read timed out") :transport-timeout]
                                 [#(NullPointerException. "port bug") :untyped-failure]
                                 [#(ex-info "typed bug" {:failure-kind :build-failed}
                                            (java.net.ConnectException. "nested transport")) :build-failed]]]
    (let [raw (make-fault)
          fault (if wrapped? (ex-info "port wrapper" {} raw) raw)
          run-job job/run!
          {:keys [result failures close diagnostics calls constructors]}
          (with-redefs [job/run! (fn [opts action identity dir ports]
                                  (run-job opts action identity dir
                                           (assoc ports port (fn [& _] (throw fault)))))]
            (run-case :receipt :valid))
          transport? (#{:transport-unavailable :transport-timeout} expected)
          label (str port " wrapped=" wrapped? " expected=" expected)]
      (is (= expected (#'runner/failure-kind-from fault)) label)
      (is (= expected (get-in result [:data :failure-kind])) label)
      (is (= (if transport? :environmental-hold :machine-failure)
             (get-in result [:data :repair-obligation :repair/class])) label)
      (is (= (if transport? :interpretation/agent-unavailable :interpretation/machine-failure)
             (get-in failures [0 :failure :kind])) label)
      (is (= (.getName (class fault)) (get-in diagnostics [0 :causes 0 :class])) label)
      (is (some #(.startsWith (.getName (io/file (:source-path %))) "interpretation-failure-")
                (get-in close [:payload :close-evidence-manifest :entries])) label)
      (is (every? #{"interpreter"} calls) label)
      (is (zero? constructors) label))))

(deftest interpretation-requires-transport-authority-before-writing
  (is (= :interpretation/transport-classifier-missing
         (try (job/run! {} {} {} nil {})
              nil
              (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e)))))))

(deftest ticket-construction-preserves-action-and-rules
  (let [{:keys [result action constructors organised]} (run-case :receipt :ticket)
        judgment (get-in result [:checkpoints :construction :judgment])
        retained (:receipted-construction judgment)]
    (is (zero? constructors)) (is (= 1 organised))
    (is (= (pr-str action) (pr-str (get-in judgment [:cascade :selected-action]))))
    (is (= action (get-in retained [:identity :occurrence :action/value])))
    (is (= :authored-reachability-topological (:precedence-rule retained)))
    (is (= :first-true-unfired-guard-apply-effects-from-q0 (:acting-rule retained)))
    (is (= :vacuous (get-in retained [:find-result :f4])))))

(deftest fact-pairs-start-after-construction-and-survive-build-failure
  (let [{:keys [result observations close]} (run-case :receipt :valid)
        entries (get-in close [:payload :close-evidence-manifest :entries])]
    (is (= :build-failed (:outcome result)))
    (is (= #{"fact-pre.edn" "fact-end.edn"} (set (keys observations))))
    (is (= :pre (get-in observations ["fact-pre.edn" :phase])))
    (is (= :end (get-in observations ["fact-end.edn" :phase])))
    (doseq [name ["fact-pre.edn" "fact-end.edn"]]
      (is (some #(.endsWith (:source-path %) name) entries))))
  (doseq [scenario [:invalid :nothing-fires]]
    (is (empty? (:observations (run-case :receipt scenario)))))
  (is (empty? (:observations (run-case nil :valid)))))

(deftest invalid-reader-plan-stops-construction
  (doseq [scenario [:bad-reader :bad-parameter :bad-fact :bad-citation]]
    (let [{:keys [organised constructors failures observations calls]} (run-case :receipt scenario)]
      (is (= :interpretation/invalid-receipt (get-in failures [0 :failure :kind])))
      (is (zero? organised))
      (is (zero? constructors))
      (is (= ["interpreter"] calls))
      (is (empty? observations)))))

(deftest end-freezes-before-adjudication-and-close-cutoff
  (let [{:keys [result observations close adjudication-observations]} (run-case :receipt :success)]
    (is (seq adjudication-observations))
    (is (every? true? adjudication-observations))
    (is (= #{"fact-pre.edn" "fact-end.edn"} (set (keys observations))))
    (is (= :absent (get-in result [:close-retention :state :status])))
    (doseq [entry (get-in close [:payload :close-evidence-manifest :entries])]
      (is (not (.isAfter (java.time.Instant/parse (:admitted-at entry))
                        (java.time.Instant/parse (:recorded-at close))))))))
