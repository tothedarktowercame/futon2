(ns futon2.aif.wm.resume-capability-contract-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.wm.cascade-decision :as cascade]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.authored.req-aab759a8 :as authored]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)

(defn frozen [name]
  (edn/read-string (slurp (str "test/resources/wm/resume-capability/" name ".edn"))))

(defn isolated-opts []
  (let [root (str (java.nio.file.Files/createTempDirectory "capability-contract-"
                   (make-array java.nio.file.attribute.FileAttribute 0)))]
    {:executable-cascade-root (str root "/cascades")
     :execution-journal-root (str root "/journal")}))

(defn routed-clauses []
  (into {} (map (fn [[id clause]] [id (assoc clause :capability :wm/pattern-apply)])
                authored/clauses)))

(deftest frozen-clauses-refused-before-review
  (let [entry (:selected-entry (frozen "checkpoint"))
        error (cascade/execute-selected-cascade! entry (isolated-opts))
        reviews (atom 0) requests (atom [])
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [req] (swap! requests conj req)
                    {:requisition-id (:requisition-id req)
                              :cascade-digest (:cascade-digest req)
                              :by "wm-author" :decisions {:summary "three holes"}
                              :patch {:namespace "futon2.aif.wm.authored.req-aab759a8"
                                      :var "decisions" :source-digest "c7339cca4c123fc7fe3994541db206917e668b774b20d38f48d9cd522ef11498"}})
                  :open-cascade-reviewer-answer-fn (fn [_] (swap! reviews inc))
                  :open-cascade-patch-loader (fn [_] (constantly authored/clauses))})
        result (with-redefs [delivery/check-delivery (fn [_] {:status :delivered})]
                 (handler error))]
    (is (= :answer-capability-unroutable (:kind result)))
    (is (= (zipmap (keys authored/clauses) (repeat :wm/enactment-compile)) (:clauses result)))
    (is (zero? @reviews))
    (is (= (vec (keys dispatch/default-routes)) (:routable-capabilities (first @requests))))
    (is (.contains (#'runtime/open-cascade-author-prompt (first @requests)) ":wm/pattern-apply"))
    (is (= :accepted (:status (resume/validate-product (first @requests) {} (routed-clauses)))))
    (println "DELIVERY" (pr-str (select-keys result [:kind :clauses :routable-capabilities])))))

(deftest frozen-routing-failure-is-not-a-protocol-violation
  (let [entry (:selected-entry (frozen "checkpoint"))
        opts (isolated-opts)
        error (cascade/execute-selected-cascade! entry opts)
        ctx (assoc (resume/recognize error)
                   :authoring {:by "wm-author" :accepted-by "wm-reviewer"}
                   :authoring-product authored/clauses)
        raw ((:retry ctx) ctx)
        result (resume/resume! (resume/new-ledger "frozen-capability") ctx
                              {:click-budget (atom 0) :selection-counter (atom 0)})
        failure (#'runner/no-entry-failure-data {:decision {:status :selected}} result)
        written (runner/persist-run-record!
                  {:run-record-dir runner/default-run-record-dir
                   :observation-labels-path (str (io/file runner/default-run-record-dir "labels.edn"))
                   :scan-render-fn (fn [& _] nil) :nano-time-fn (constantly 0)}
                  "frozen-capability-refusal" "2026-10-29T20:25:50Z"
                  {:outcome (:outcome failure)
                   :data {:failure-kind (:failure-kind failure) :failure-stage :cascade-resume
                          :error "Frozen capability refusal" :error-data failure}})
        record (edn/read-string (slurp (:run-record written)))]
    (is (= :capability-route-missing (:kind raw)))
    (is (= raw result) "typed retry refusal passes through unchanged")
    (is (= :capability-route-missing (get-in record [:failure :kind])))
    (println "RETAINED-REFUSAL" (pr-str (:failure record)))))

(deftest frozen-routable-clauses-reach-construction
  (doseq [routable? [false true]]
  (let [d (frozen "decision")
        decision (assoc d :action (get-in (frozen "checkpoint") [:selected-entry :action]))
        constructed (atom nil) events (atom [])
        ;; Exercise the real resume/compiler with only the capability corrected.
        handler (fn [error & _]
                  (let [ctx (assoc (resume/recognize error)
                                   :authoring {:by "wm-author" :accepted-by "wm-reviewer"}
                                   :authoring-product (if routable? (routed-clauses) authored/clauses))]
                    (resume/resume! (resume/new-ledger "frozen-construction") ctx
                                    {:click-budget (atom 0) :selection-counter (atom 0)})))
        result (binding [runner/*wm-status-reporting?* false]
                 (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)]
                   (runner/run-opportunity!
                     (merge (fixture/isolated-runner-opts) (isolated-opts)
                       {:run-id (str "frozen-capability-construction-" routable?)
                        :author "wm-author" :reviewer "wm-reviewer"
                        :roster-fn (fn [_] {:wm-author {:status "idle" :invoke-ready? true}
                                            :wm-reviewer {:status "idle" :invoke-ready? true}
                                            :codex-1 {:status "idle" :invoke-ready? true}})
                        :judge-fn (fn [_] {:judgement {:decision decision :belief {} :belief-pre {}
                                                       :observation {} :free-energy {} :prediction-errors {}
                                                       :precision-state {} :micro-step-trace [] :mode :maintain}})
                        :open-cascade-resume-fn handler
                        :phase-log-fn #(swap! events conj %)
                        :construct-fn (fn [entry]
                                        (reset! constructed (runner/construct-for-decision entry))
                                        (throw (ex-info "Witness reached construction"
                                                       {:outcome :incomplete :failure-stage :construction
                                                        :failure-kind :test/construction-stop})))
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected contact" {})))}))))]
    (is (= routable? (some? @constructed)))
    (if routable?
      (is (some #(and (= :cascade-resume (:phase %)) (= :resumed (:status %))) @events))
      (let [record (edn/read-string
                     (slurp (io/file runner/default-run-record-dir
                              "tick-run-record-frozen-capability-construction-false.edn")))]
        (is (= :capability-route-missing (get-in record [:failure :kind])))
        (println "RUNNER-RETAINED-REFUSAL" (pr-str (:failure record)))))
    (println "CONSTRUCTION" (pr-str {:constructed? (some? @constructed) :outcome (:outcome result)
                                     :phases (mapv #(select-keys % [:phase :status :kind]) @events)})))))
