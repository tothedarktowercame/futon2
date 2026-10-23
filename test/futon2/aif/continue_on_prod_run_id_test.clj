(ns futon2.aif.continue-on-prod-run-id-test
  "A selection-free continuation installs a job ledger, so every dispatch it
  makes must carry a run id: full-loop-runner/register-job! throws on a nil
  run id after the bell has already gone out (M2y-4c, 2026-10-23)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.wm.cascade-continuation :as cascade-continuation]))

(defn- run-continuation [opts]
  (let [seen (atom {})]
    (with-redefs [runtime/resume-on-prod-cascade!
                  (fn [o]
                    (swap! seen assoc :resume (:run-id o))
                    {:run-id (:run-id o) :result {:status :cascade-ready}})
                  runtime/production-cascade-executors
                  (fn [o]
                    (swap! seen assoc :executors (:run-id o)
                           :ledger (:registered-run/job-ledger o)
                           :click-id (:click-id o))
                    {})
                  cascade-continuation/continue-certified-cascade!
                  (fn [_] {:status :stubbed})]
      (runtime/continue-on-prod-cascade!
       (merge {:source-run-id "run-a" :source-attempt-id "attempt-001"}
              opts)))
    @seen))

(deftest executors-see-the-resume-run-id
  (testing "no caller run id: one is minted and shared"
    (let [{:keys [resume executors ledger click-id]} (run-continuation {})]
      (is (string? executors))
      (is (not (str/blank? executors)))
      (is (= resume executors))
      (is (some? ledger))
      (is (= "potted-history:run-a/attempt-001" click-id))))
  (testing "a caller run id is kept"
    (let [{:keys [resume executors]} (run-continuation {:run-id "given"})]
      (is (= "given" resume executors)))))
