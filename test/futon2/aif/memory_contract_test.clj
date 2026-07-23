(ns futon2.aif.memory-contract-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.memory-contract :as memory-contract]))

(defn- fixtures []
  (-> "fixtures/shared_memory_contract_fixtures.edn"
      io/resource
      slurp
      edn/read-string))

(deftest both-domains-use-the-same-compact-projection
  (let [{:keys [mathematics war-machine]} (fixtures)
        math (memory-contract/compact-memory mathematics)
        wm (memory-contract/compact-memory war-machine)]
    (is (= :mathematics (:memory/domain math)))
    (is (= ["lean/field-simp-denominator"] (:memory/pattern-ids math)))
    (is (= :independently-witnessed (:memory/witness-status math)))
    (is (= :war-machine (:memory/domain wm)))
    (is (= ["p4ng/R15"] (:memory/pattern-ids wm)))
    (is (= ["wm/mission/strategic-selection"] (:memory/mission-ids wm)))
    (is (true? (:memory/volatile? wm)))
    (is (not (contains? math :memory/value)))
    (is (not (contains? wm :memory/score)))))

(deftest compact-projection-fails-closed
  (let [math (:mathematics (fixtures))]
    (testing "domain is explicit"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"domain"
                            (memory-contract/compact-memory
                             (dissoc math :domain)))))
    (testing "provenance is mandatory"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"provenance"
                            (memory-contract/compact-memory
                             (update math :entry dissoc :evidence/author)))))
    (testing "role endpoints must be materialized on the edge"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"role endpoint"
                            (memory-contract/compact-memory
                             (update-in math [:edge :hx/endpoints]
                                        #(vec (remove #{"lean/field-simp-denominator"} %)))))))))

(deftest both-domains-use-the-same-use-receipt
  (let [{:keys [math-receipt wm-receipt]} (fixtures)
        math (memory-contract/use-receipt math-receipt)
        wm (memory-contract/use-receipt wm-receipt)]
    (is (= :outcome-attached (:memory-use/status math)))
    (is (= ["e-math-1"] (:memory-use/used-ids math)))
    (is (= [] (:memory-use/unused-ids math)))
    (is (= :pending-outcome (:memory-use/status wm)))
    (is (= [] (:memory-use/used-ids wm)))
    (is (= ["e-wm-1"] (:memory-use/unused-ids wm)))
    (is (= "wm-control-cascade-1" (:memory-use/cascade-id wm)))))

(deftest use-receipt-rejects-unseen-use-and-unreasoned-surface
  (let [base (:math-receipt (fixtures))]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"must have been surfaced"
                          (memory-contract/use-receipt
                           (assoc base :used-memory-ids ["e-not-surfaced"]))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"inclusion reason"
                          (memory-contract/use-receipt
                           (assoc base :inclusion-reasons {}))))))

(deftest use-receipt-classifies-rejected-and-unused-memories
  (let [base (-> (:math-receipt (fixtures))
                 (assoc :surfaced-memory-ids ["e-math-1" "e-math-2" "e-math-3"]
                        :used-memory-ids ["e-math-1"]
                        :rejected-memory-ids ["e-math-2"]
                        :inclusion-reasons
                        {"e-math-1" "reviewed pattern attachment"
                         "e-math-2" "reviewed pattern attachment"
                         "e-math-3" "reviewed pattern attachment"}
                        :rejection-reasons
                        {"e-math-2" "counterexample has incompatible hypotheses"}))
        receipt (memory-contract/use-receipt base)]
    (is (= ["e-math-2"] (:memory-use/rejected-ids receipt)))
    (is (= ["e-math-3"] (:memory-use/unused-ids receipt)))
    (is (= [{:memory-id "e-math-2"
             :reason "counterexample has incompatible hypotheses"}]
           (:memory-use/rejection-reasons receipt)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"both used and rejected"
         (memory-contract/use-receipt
          (assoc base :rejected-memory-ids ["e-math-1"]))))))

(deftest use-receipt-measures-retrieval-to-use-latency
  (let [base (:math-receipt (fixtures))
        receipt
        (memory-contract/use-receipt
         (assoc base
                :surfaced-at "2026-07-23T09:00:00Z"
                :recorded-at "2026-07-23T09:00:02.250Z"))]
    (is (= 2250 (:memory-use/retrieval-to-use-ms receipt)))
    (is (= "2026-07-23T09:00:00Z"
           (:memory-use/surfaced-at receipt)))))
