(ns futon2.aif.ticket-selected-wants-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.selected-mission-contract :as contract])
  (:import [java.time Instant]
           [java.util UUID]))

(def ticket "T-car3-queue-routing-design-note")

(def occurrences
  [["agent/evidence-over-assertion" 2]
   ["agent/handoff-preserves-context" 3]
   ["agent/provisional-claims-ledger" 1]
   ["agent/scope-before-action" 0]
   ["agent/scope-before-action" 4]
   ["contracts/holder-states-the-claim" 2]])

(def outputs
  (conj (set (map #(into [ticket :pattern-done] %) occurrences))
        [ticket :overlap
         ["[\"agent/evidence-over-assertion\" 2]"
          "[\"contracts/holder-states-the-claim\" 2]"]]))

(defn cascade-action [target produced]
  {:kind :cascade-candidate
   :id (str target "/policy")
   :target target
   :precedence {:co-apply
                {:patterns
                 {[:selected-pattern 0]
                  {:id [:selected-pattern 0]
                   :target target
                   :produces produced}}}}})

(deftest run-ede75c1a-ticket-wants-exist-before-occurrence-minting
  (let [expected-wants
        (set (map (fn [output]
                    [ticket (vec (rest output))])
                  outputs))
        selected (contract/attach (cascade-action ticket outputs) nil)
        occurrence (retention/mint-occurrence
                    {:run-id "2026-11-04-ede75c1a-146a-44b1-b229-d6b5a1b6a647"
                     :cohort-id "wm-contract-machinery-175-v1"
                     :attempt-id "attempt-002"
                     :selected-action selected
                     :now #(Instant/parse "2026-11-04T05:12:00Z")
                     :uuid-fn #(UUID/fromString
                                "00000000-0000-0000-0000-000000000001")})]
    (is (= expected-wants (set (:want selected))))
    (is (= 7 (count (:want selected))))
    (is (= (:want selected) (get-in occurrence [:action/value :want])))
    (is (= (:observation-locators selected)
           (get-in occurrence [:action/value :observation-locators])))))

(deftest ticket-source-contract-precedes-cascade-output-fallback
  (let [target "T-source-declared"
        path "/home/joe/code/futon2/holes/tickets/T-source-declared.md"
        source-contract (contract/source-contract
                         "/home/joe/code" target path
                         "# Ticket\n\n- [ ] Retain the source-declared result\n")
        selected (contract/attach
                  (cascade-action target #{[target :cascade-output]})
                  source-contract)]
    (is (= (:want source-contract) (:want selected)))
    (is (seq (:want selected)))
    (is (= (:observation-locators source-contract)
           (:observation-locators selected)))
    (is (map? (:mission-want-source selected)))))

(deftest mission-and-excursion-contracts-are-unchanged
  (doseq [target ["M-control" "E-control"]]
    (let [path (str "/home/joe/code/futon2/holes/"
                    (if (.startsWith target "M-") "missions/" "excursions/")
                    target ".md")
          source-contract (contract/source-contract
                           "/home/joe/code" target path
                           "# Work\n\n- [ ] Preserve this source want\n")
          selected (contract/attach
                    (cascade-action target #{[target :cascade-output]})
                    source-contract)]
      (testing target
        (is (= (:want source-contract) (:want selected)))
        (is (= (:observation-locators source-contract)
               (:observation-locators selected)))
        (is (= (:mission-want-source source-contract)
               (:mission-want-source selected)))))))

(deftest output-free-ticket-retains-an-explicit-empty-want
  (let [selected (contract/attach (cascade-action "T-empty" #{}) nil)]
    (is (= [] (:want selected)))
    (is (= {} (:observation-locators selected)))))
