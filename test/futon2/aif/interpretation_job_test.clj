(ns futon2.aif.interpretation-job-test
  "Controls for the still-live interpretation-job boundary.

  The former tests drove this job through an interpreted-action runner fixture.
  Production now selects reading policies and holds them at selected-only
  interpretation; that fixture stopped before this job and produced hundreds
  of misleading failures. Runner transport, source-pin, fact-measurement and
  close-cutoff laws have their own current-route namespaces. These tests retain
  only contracts actually owned by interpretation-job."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-evidence-test :as receipt-fixture]
            [futon2.aif.interpretation-job :as job]))

(deftest interpretation-contract-is-immutable-issued-evidence
  (let [contract (evidence/acceptance-contract)]
    (is (= :wm/handoff-contract-v1 (:schema contract)))
    (is (= :wm/interpretation-evidence (get-in contract [:validator :id])))
    (is (= evidence/schemas (set (get-in contract [:response :schemas]))))
    (is (some #(= :wm/interpretation-source-bytes (:id %))
              (get-in contract [:response :semantic-gates])))
    (is (.contains (job/prompt "/tmp/interpretation")
                   "interpretation-contract.source"))
    (is (not (.contains (job/prompt "/tmp/interpretation")
                        "Read src/futon2/aif/interpretation_evidence.clj")))))

(deftest interpretation-requires-transport-authority-before-writing
  (is (= :interpretation/transport-classifier-missing
         (try (job/run! {} {} {} nil {})
              nil
              (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e)))))))

(deftest failure-schema-is-closed-and-success-stays-strict
  (let [{:keys [record]} @receipt-fixture/fixture
        identity (:identity record)
        failure {:schema evidence/failure-schema :identity identity :stage :prepare
                 :sources []
                 :absent {:target {:status :none :reason :target-unresolved}}
                 :failure {:kind :interpretation/target-unresolved
                           :identity identity :stage :prepare :source-refs []
                           :elapsed-ms 0 :partial-artifacts []}}]
    (is (= failure (evidence/validate-record failure)))
    (is (= :shape-invalid
           (try (evidence/validate-record (assoc failure :target {})) nil
                (catch clojure.lang.ExceptionInfo e
                  (:interpretation-evidence/refusal (ex-data e))))))))

(deftest interpretation-failures-retain-their-disposition
  (doseq [kind [:interpretation/no-relevant-pattern
                :interpretation/no-citable-tension
                :interpretation/invalid-receipt
                :interpretation/source-changed
                :interpretation/genesis-required
                :interpretation/retrieval-unavailable]]
    (is (= :environmental-hold (#'runner/repair-class-for kind)) (str kind)))
  (is (= :incomplete-recoverable
         (#'runner/repair-class-for :agent-budget-expired)))
  (is (= :machine-failure (#'runner/repair-class-for :build-failed))))
