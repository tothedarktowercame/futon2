(ns futon2.aif.calibration-admission-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.calibration-admission :as admission]))

;; Live Agency record invoke-1788708049924-13300-38749afe carries these values
;; verbatim: commission PA11z-library-annotator-exemplar, state done, and
;; delivery status delivered.  PA11z captured the record under
;; holes/labs/zaif-harness/runs/PA11z-exemplar/dispatch-job-record.json.
(def live-commission
  {:commission/id "PA11z-library-annotator-exemplar" :node :R12})
(def live-return
  {:return/id "invoke-1788708049924-13300-38749afe"
   :commission/id "PA11z-library-annotator-exemplar"
   :node :R12
   :artifact {:state "done" :delivery/status "delivered"}})
(def live-check
  {:check/id "PA7z-check-invoke-1788708049924-13300-38749afe"
   :commission/id "PA11z-library-annotator-exemplar"
   :return/id "invoke-1788708049924-13300-38749afe"
   :node :R12
   :verdict :approve})

(deftest untied-return-is-refused
  (let [result (admission/admit! live-commission
                                 (assoc live-return :commission/id "other")
                                 live-check)]
    (is (false? (:ok result)))
    (is (= :r12/untied-return (:error/code result)))))

(deftest unchecked-return-is-refused
  (let [result (admission/admit! live-commission live-return
                                 (assoc live-check :verdict :pending))]
    (is (false? (:ok result)))
    (is (= :r12/unchecked-return (:error/code result)))))

(deftest tied-and-checked-return-is-admitted
  (let [result (admission/admit! live-commission live-return live-check)]
    (is (:ok result))
    (is (= :admitted (:status result)))
    (is (= "invoke-1788708049924-13300-38749afe"
           (get-in result [:returned :return/id])))
    (is (= :approve (get-in result [:checked :verdict])))))
