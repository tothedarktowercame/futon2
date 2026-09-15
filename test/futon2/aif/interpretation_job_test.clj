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
            [futon2.aif.full-loop-runner :as runner]
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
                       (let [identity (:identity (edn/read-string (slurp (io/file dir "interpretation-job.source"))))
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
        opts (assoc base :trace-fn (constantly "/fixture/trace.edn") :run-id (str (UUID/randomUUID)) :interpretation-mode mode :interpreter "interpreter"
                    :judge-fn (fn [_] {:judgement (walk/postwalk-replace
                                                  {runner-fixture/selected-action action} runner-fixture/judgement)})
                    :interpretation-history-roots [root]
                    :interpretation-prepare-fn prepare
                    :interpretation-readiness-fn (fn [_ _]
                                                   (when (= scenario :unavailable)
                                                     (throw (ex-info "no interpreter" {}))))
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
                                   policy/organise (fn [& args] (swap! organised inc) (apply organise args))]
                       (runner/run-opportunity! opts)))
            failures (when (.exists dir)
                       (mapv #(edn/read-string (slurp %))
                             (filter #(.startsWith (.getName %) "interpretation-failure-") (.listFiles dir))))
            close-file (io/file (.getParentFile dir) "007-closed.edn")
            close (when (.exists close-file) (edn/read-string (slurp close-file)))]
        {:result result :calls @calls :action action :organised @organised :constructors @constructors :failures failures :close close})
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
    (let [{:keys [constructors calls failures close]} (run-case :receipt scenario)]
      (is (zero? constructors) (str scenario))
      (is (every? #{"interpreter"} calls) (str scenario))
      (is (= expected (get-in failures [0 :failure :kind])) (str scenario))
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
