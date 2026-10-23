(ns futon2.aif.wm.cascade-continuation-failure-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.cascade-continuation :as sut]
            [futon2.aif.wm.executable-cascade :as cascade]
            [futon2.aif.wm.cascade-enactment-compiler-test :as compiler-test])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- fixture []
  (let [structural (#'compiler-test/structural)
        clauses (into {}
                      (map (fn [n]
                             [(:occurrence-id n)
                              {:operation :apply :capability :wm/pattern-apply
                               :consumes :before :produces :after
                               :completion-check [:observed :after]}]))
                      (:nodes structural))]
    (cascade/build structural
                   {:application-order [] :operational-clauses clauses :holes {}})))

(defn- three-node-record [base]
  (let [node (first (:frontier base))
        record (assoc base :frontier
                      (mapv #(assoc node :occurrence-id [:test/occurrence %]
                                         :pattern :test/pattern)
                            (range 3)))]
    (assoc record :digest (evidence/value-digest (dissoc record :digest)))))

(defn- ready [record]
  {:status :cascade-ready
   :cascade-digest (evidence/value-digest [:selected])
   :compiled-digest (:digest record)
   :capability-routing
   {:status :routed :cascade-digest (:digest record)
    :steps (mapv #(select-keys % [:occurrence-id :operation :capability])
                 (:frontier record))}})

(defn- sealed-executor [n]
  (fn [{:keys [cascade-digest node]}]
    (dispatch/seal-receipt
     {:schema dispatch/receipt-schema
      :cascade-digest cascade-digest
      :occurrence-id (:occurrence-id node)
      :operation (get-in node [:clause :operation])
      :capability (get-in node [:clause :capability])
      :evidence-kind :verified-effect
      :effect {:status :verified
               :grounding {:kind :test
                           :digest (evidence/value-digest [n (:occurrence-id node)])}}})))

(defn- run-failure [record opts]
  (let [cascades (temp-dir "continuation-failure-cascades-")
        _ (cascade/save! record cascades)
        calls (atom 0)
        close-calls (atom 0)]
    (assoc
     (sut/continue-certified-cascade!
      (merge {:ready (ready record)
              :executable-cascade-root cascades
              :execution-journal-root (temp-dir "continuation-failure-journals-")
              :close-fn (fn [packet]
                          (swap! close-calls inc)
                          {:status :closed :cascade-digest (:cascade-digest packet)
                           :close-receipt {:evidence-kind :verified-effect
                                           :digest (evidence/value-digest packet)}})
              :instant-fn #(Instant/parse "2026-10-23T00:00:00Z")
              :executors
              {:wm/pattern-apply
               (fn [{:keys [node] :as packet}]
                 (swap! calls inc)
                 (if (= [:test/occurrence 1] (:occurrence-id node))
                   {:status :error :kind :test/second-node-refused}
                   ((sealed-executor 1) packet)))}}
             opts))
     :calls @calls :close-calls @close-calls)))

(deftest failed-frontier-node-is-sealed-beside-completed-receipts
  (let [record (three-node-record (fixture))
        root (temp-dir "continuation-failure-records-")
        failure! (sut/production-failure-adapter
                  {:root root :source-run-id "run-1" :source-attempt-id "attempt-1"})
        result (run-failure record {:failure-fn failure!})
        failure-record (:failure-record result)
        path (get-in failure-record [:failure-receipt :path])
        on-disk (edn/read-string (slurp path))
        first-node (first (:frontier record))
        second-node (second (:frontier record))]
    (is (= :capability-effect-receipt-invalid (:kind result)))
    (is (= 2 (:calls result)))
    (is (zero? (:close-calls result)))
    (is (= :recorded (:status failure-record)))
    (is (= (:occurrence-id second-node) (:failed-occurrence-id on-disk)))
    (is (= (:pattern second-node) (:failed-pattern on-disk)))
    (is (= 1 (count (:completed-receipts on-disk))))
    (is (= (:completed result) (:completed-receipts on-disk)))
    (is (= (:occurrence-id first-node)
           (get-in on-disk [:completed-receipts 0 :occurrence-id])))
    (is (= (:digest on-disk)
           (evidence/value-digest (dissoc on-disk :digest))))
    (is (= (mapv :digest (:completed-receipts on-disk))
           (:completed-receipt-digests on-disk)))
    (is (= 1 (count (filter #(.isFile %) (or (.listFiles root) [])))))))

(deftest absent-or-invalid-failure-port-preserves-the-dispatch-failure
  (let [record (three-node-record (fixture))
        baseline (run-failure record {})
        garbage (run-failure record {:failure-fn (constantly :garbage)})
        thrower (run-failure record {:failure-fn (fn [_] (throw (ex-info "boom" {})))})]
    (testing "the optional port preserves the pre-packet result when absent"
      (is (= :capability-effect-receipt-invalid (:kind baseline)))
      (is (not (contains? baseline :failure-record)))
      (is (= 1 (count (:completed baseline)))))
    (testing "a bad recorder cannot overwrite the original failure reason"
      (is (= :capability-effect-receipt-invalid (:kind garbage)))
      (is (= :failure-record-unverified
             (get-in garbage [:failure-record :kind])))
      (is (= baseline (dissoc garbage :failure-record))))
    (testing "a throwing recorder is typed and leaves the dispatch result unchanged"
      (is (= :failure-record-unverified
             (get-in thrower [:failure-record :kind])))
      (is (= "boom" (get-in thrower [:failure-record :message])))
      (is (= baseline (dissoc thrower :failure-record))))))

(deftest successful-continuation-does-not-call-the-failure-port
  (let [record (fixture)
        cascades (temp-dir "continuation-success-cascades-")
        _ (cascade/save! record cascades)
        failures (atom 0)
        result (sut/continue-certified-cascade!
                {:ready (ready record)
                 :executable-cascade-root cascades
                 :execution-journal-root (temp-dir "continuation-success-journals-")
                 :executors {:wm/pattern-apply (sealed-executor 1)}
                 :instant-fn #(Instant/parse "2026-10-23T00:00:00Z")
                 :failure-fn (fn [_] (swap! failures inc))
                 :close-fn (fn [packet]
                             {:status :closed :cascade-digest (:cascade-digest packet)
                              :close-receipt {:evidence-kind :verified-effect
                                              :digest (evidence/value-digest packet)}})})]
    (is (= :closed (:status result)))
    (is (zero? @failures))))
