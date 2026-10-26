(ns futon2.aif.reading-preparation-retain-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.outer-selection-trace :as ost]
            [futon2.aif.reading-preparation-outcome :as rpo]
            [futon2.aif.selected-target-reading :as reading]))

;; Live pin: the C1a projection of wm-full-loop-machinery-150 attempt-002,
;; values verbatim, used as the retained receipt.
(def receipt
  (edn/read-string (slurp "test/resources/outer-selection-trace/attempt-002-projection.edn")))

(def judgement {:outer-task-selection receipt})

(deftest retain-outer-task-selection-carries-reading-preparation
  (let [outcome (rpo/outcome judgement nil)]
    (testing "the pin is not vacuous: no selected need on this judgement"
      (is (nil? (reading/selected-need judgement))))
    (testing "4-arity, selected?: receipt identical, trace and outcome beside it"
      (let [cell (runner/retain-outer-task-selection {:judgment {:x 1}} true receipt outcome)
            j (:judgment cell)]
        (is (identical? receipt (:outer-task-selection j)))
        (is (= (ost/trace receipt) (:outer-selection-trace j)))
        (is (= outcome (:reading-preparation j)))
        (is (= :not-run (:status (:reading-preparation j))))
        (is (= :no-selected-need (:reason (:reading-preparation j))))
        (is (= 1 (:x j)))
        (is (not (contains? cell :sorry)))))
    (testing "4-arity, not selected?: all three under :sorry, :judgment untouched"
      (let [cell (runner/retain-outer-task-selection {:judgment {:x 1}} false receipt outcome)
            s (:sorry cell)]
        (is (= receipt (:outer-task-selection s)))
        (is (= (ost/trace receipt) (:outer-selection-trace s)))
        (is (= outcome (:reading-preparation s)))
        (is (= {:x 1} (:judgment cell)))))
    (testing "3-arity unchanged: no :reading-preparation on either arm"
      (let [cell (runner/retain-outer-task-selection {} true receipt)]
        (is (not (contains? (:judgment cell) :reading-preparation))))
      (let [cell (runner/retain-outer-task-selection {} false receipt)]
        (is (not (contains? (:sorry cell) :reading-preparation)))))
    (testing "a preparation record's outcome is retained as :ran, bounded"
      (let [record {:schema :wm/target-reading-preparation-v1
                    :status :pending :reason :batch-limit
                    :deadline-ms 12345 :field-source-pin "pin-1"
                    :records [{:need {:target "A"} :published true :analysis {:big 1}}
                              {:need {:target "B"} :published false :analysis {:big 2}}]
                    :pending [{:target "C" :excerpt-digest "d"}]
                    :frontier {:huge true}}
            cell (runner/retain-outer-task-selection {} true receipt
                                                      (rpo/outcome judgement record))
            rp (get-in cell [:judgment :reading-preparation])]
        (is (= :ran (:status rp)))
        (is (not (contains? rp :records)))
        (is (not (contains? rp :frontier)))))
    (testing "bad case: nil outcome must not throw and retains nil"
      (let [cell (runner/retain-outer-task-selection {} true receipt nil)]
        (is (nil? (get-in cell [:judgment :reading-preparation])))
        (is (contains? (:judgment cell) :reading-preparation))))))
