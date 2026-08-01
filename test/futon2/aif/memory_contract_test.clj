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
                                        #(vec (remove #{"lean/field-simp-denominator"} %)))))))
    (testing "memory-use kind has a closed vocabulary"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"invalid memory-use kind"
                            (memory-contract/compact-memory
                             (assoc-in math [:edge :hx/props :memory-use/kind]
                                       :mixed)))))))

(deftest attribution-and-wm-projection-use-distinct-receipts
  (let [{:keys [math-receipt wm-projection-receipt]} (fixtures)
        math (memory-contract/use-receipt math-receipt)
        wm (memory-contract/wm-projection-receipt wm-projection-receipt)]
    (is (= :agent-attribution (:memory-use/signal math)))
    (is (= :outcome-attached (:memory-use/status math)))
    (is (= ["e-math-1"] (:memory-use/used-ids math)))
    (is (= [] (:memory-use/unused-ids math)))
    (is (= :algorithmic-selection (:wm-projection/signal wm)))
    (is (= :pending-external-check (:wm-projection/status wm)))
    (is (= [] (:wm-projection/projection-selected-ids wm)))
    (is (not-any? #(= "used-ids" (name %)) (keys wm)))
    (is (= "wm-control-cascade-1" (:wm-projection/cascade-id wm)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"cannot contain algorithmic projection"
         (memory-contract/agent-attribution-corpus [math wm])))))

(deftest wm-used-ids-are-structurally-rejected
  (let [{:keys [wm-projection-receipt]} (fixtures)]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"not agent memory attribution"
         (memory-contract/use-receipt
          (-> wm-projection-receipt
              (dissoc :projection-selected-memory-ids)
              (assoc :used-memory-ids [])))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"structurally forbid used-id"
         (memory-contract/wm-projection-receipt
          (assoc wm-projection-receipt :used-memory-ids []))))))

(deftest one-witness-contract-joins-both-receipt-kinds
  (let [{:keys [math-receipt wm-projection-receipt]} (fixtures)
        math (memory-contract/use-receipt (dissoc math-receipt :outcome-id))
        wm (memory-contract/wm-projection-receipt
            (assoc wm-projection-receipt
                   :projection-selected-memory-ids ["e-wm-1"]))
        check-args
        {:evidence-id "e-shared-witness"
         :author "memory/test-external-checker"
         :session-id "memory-check-session"
         :at "2026-07-31T10:00:00Z"
         :outcome :pass
         :witness-status :independently-witnessed
         :checker "test-only external checker"}
        math-check
        (memory-contract/decision-keyed-external-check-entry
         (assoc check-args
                :decision-id "math-decision-1"
                :domain :mathematics))
        wm-check
        (memory-contract/decision-keyed-external-check-entry
         (assoc check-args
                :evidence-id "e-shared-wm-witness"
                :decision-id "wm-shadow-decision-1"
                :domain :war-machine))
        math-triple
        (memory-contract/witnessed-memory-outcome-triple math math-check)
        wm-triple
        (memory-contract/witnessed-memory-outcome-triple wm wm-check)]
    (is (= :agent-attribution
           (:memory-outcome-triple/selection-signal math-triple)))
    (is (= ["e-math-1"]
           (:memory-outcome-triple/selected-ids math-triple)))
    (is (= :algorithmic-selection
           (:memory-outcome-triple/selection-signal wm-triple)))
    (is (= ["e-wm-1"]
           (:memory-outcome-triple/selected-ids wm-triple)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"domains differ"
         (memory-contract/witnessed-memory-outcome-triple
          math
          (assoc-in wm-check [:evidence/subject :ref/id]
                    "math-decision-1"))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"decision ids differ"
         (memory-contract/witnessed-memory-outcome-triple
          math
          (assoc-in math-check [:evidence/subject :ref/id]
                    "another-decision"))))))

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

(deftest memory-use-kind-is-optional-and-closed
  (let [base (:math-receipt (fixtures))
        classified
        (memory-contract/use-receipt
         (assoc base :memory-use-kinds {"e-math-1" :substitutive}))
        unclassified (memory-contract/use-receipt base)]
    (is (= :substitutive
           (get-in classified
                   [:memory-use/inclusion-reasons 0 :memory-use/kind])))
    (is (not (contains? (first (:memory-use/inclusion-reasons unclassified))
                        :memory-use/kind)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"invalid memory-use kind"
         (memory-contract/use-receipt
          (assoc base :memory-use-kinds {"e-math-1" :mixed}))))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"only classify surfaced"
         (memory-contract/use-receipt
          (assoc base :memory-use-kinds {"not-surfaced" :regulative}))))))
