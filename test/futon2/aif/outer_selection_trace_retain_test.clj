(ns futon2.aif.outer-selection-trace-retain-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.outer-selection-trace :as ost]))

;; The committed C1a projection is a verbatim-values receipt for the live-pin
;; rule; the retain behaviour under test is independent of which receipt rides.

(def attempt-002-receipt
  (edn/read-string (slurp "test/resources/outer-selection-trace/attempt-002-projection.edn")))

(deftest retain-outer-task-selection-carries-trace-beside-receipt
  ;; 1. selected? true: receipt unchanged at its path, trace beside it,
  ;;    existing judgment keys untouched.
  (let [cell (runner/retain-outer-task-selection {:judgment {:x 1}} true attempt-002-receipt)
        receipt' (get-in cell [:judgment :outer-task-selection])
        trace' (get-in cell [:judgment :outer-selection-trace])]
    (is (= attempt-002-receipt receipt'))
    (is (identical? attempt-002-receipt receipt'))
    (is (= (ost/trace attempt-002-receipt) trace'))
    (is (= :wm/outer-selection-trace-v1 (:schema trace')))
    (is (= 1 (get-in cell [:judgment :x])))
    (is (not (contains? (:judgment cell) :sorry)))
    (is (nil? (get-in cell [:sorry :outer-selection-trace]))))
  ;; 2. selected? false: both keys land under :sorry, :judgment untouched.
  (let [cell (runner/retain-outer-task-selection {:judgment {:x 1}} false attempt-002-receipt)]
    (is (= attempt-002-receipt (get-in cell [:sorry :outer-task-selection])))
    (is (= (ost/trace attempt-002-receipt)
           (get-in cell [:sorry :outer-selection-trace])))
    (is (= {:x 1} (get cell :judgment))))
  ;; 3. absent receipt: no throw, typed absences, never nil counts.
  (let [absent {:schema :wm/outer-task-selection-v1 :status :absent}
        cell (runner/retain-outer-task-selection {} true absent)
        trace' (get-in cell [:judgment :outer-selection-trace])]
    (is (= :wm/outer-selection-trace-v1 (:schema trace')))
    (is (= 7 (count (:stages trace'))))
    (doseq [{:keys [in out]} (:stages trace')]
      (is (some? in))
      (is (some? out))
      (when-not (number? in) (is (contains? in :absent)))
      (when-not (number? out) (is (contains? out :absent))))))
