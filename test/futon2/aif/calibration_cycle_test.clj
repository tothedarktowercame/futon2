(ns futon2.aif.calibration-cycle-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.calibration-admission :as admission]
            [futon2.aif.calibration-cycle :as cycle]))

(def available-scan
  {:available? true
   :per-class {:learn-action-class {:alpha 3.0 :beta 2.0
                                    :intrinsic-value 0.6
                                    :n-emissions 5}
               :review {:alpha 1 :beta 4 :intrinsic-value 0.2 :n-emissions 2}}
   :total-records 7
   :class-count 2})

(def unavailable-scan
  {:available? false :error "XTDB unreachable"})

(deftest available-scan-is-admitted-with-labels
  (let [result (cycle/admit-apparatus! "wm-scan/r12-apparatus"
                                       "wm-scan/r12-apparatus/2026-09-18T00:00Z"
                                       available-scan)]
    (is (:ok result) (pr-str result))
    (is (= :admitted (:status result)))
    (is (= :approve (get-in result [:check :verdict])))
    (is (= true (get-in result [:check :scan/available?])))
    (let [artifact (get-in result [:returned :artifact])]
      (is (= available-scan (:scan artifact)))
      ;; Layer 1 travels labelled never-value-evidence.
      (is (= :never (get-in artifact [:layer-1/label :value-evidence])))
      ;; Layer 2 is reported not-available, never fabricated.
      (is (= :not-available
             (get-in artifact [:layer-2/independent-evidence :status]))))))

(deftest unavailable-scan-is-a-valid-approved-negative
  (let [result (cycle/admit-apparatus! "wm-scan/r12-apparatus" "ret-1"
                                       unavailable-scan)]
    (is (:ok result))
    (is (= :approve (get-in result [:check :verdict])))
    (is (= false (get-in result [:check :scan/available?])))))

(deftest nonnumeric-posterior-is-refused-with-reason
  (let [result (cycle/admit-apparatus!
                "wm-scan/r12-apparatus" "ret-2"
                (assoc-in available-scan [:per-class :review :alpha] nil))]
    (is (false? (:ok result)))
    (is (= :r12/unchecked-return (:error/code result)))
    (is (= :r12/scan-nonnumeric-posterior (get-in result [:check :reason])))))

(deftest missing-availability-is-refused
  (let [result (cycle/admit-apparatus! "wm-scan/r12-apparatus" "ret-3"
                                       (dissoc available-scan :available?))]
    (is (false? (:ok result)))
    (is (= :r12/scan-missing-availability (get-in result [:check :reason])))))

(deftest count-mismatch-is-refused
  (let [result (cycle/admit-apparatus! "wm-scan/r12-apparatus" "ret-4"
                                       (assoc available-scan :class-count 3))]
    (is (false? (:ok result)))
    (is (= :r12/scan-count-mismatch (get-in result [:check :reason])))))

(deftest untied-ids-are-refused-by-the-boundary
  ;; A return whose commission id does not tie must never admit, even when
  ;; the scan is well formed and the check approves it.
  (let [c (cycle/commission "commission-b")
        r (cycle/apparatus-return "commission-a" "ret-6" available-scan)
        k (cycle/check-apparatus "commission-a" "ret-6" (:artifact r))
        result (admission/admit! c r k)]
    (is (false? (:ok result)))
    (is (= :r12/untied-return (:error/code result)))))

(deftest layer-1-is-never-promoted
  ;; No admitted artifact may carry a value-evidence claim anywhere on
  ;; layer 1; the only value path is layer 2, which is not-available.
  (let [artifact (get-in (cycle/admit-apparatus! "wm-scan/r12-apparatus"
                                                 "ret-7" available-scan)
                         [:returned :artifact])]
    (is (nil? (find artifact :value-evidence)))
    (is (= #{:layer :consistency-scope :value-evidence :source}
           (set (keys (:layer-1/label artifact)))))
    (is (not (contains? (:layer-1/label artifact) :outcome)))))

(deftest non-map-scan-is-refused
  (let [result (cycle/admit-apparatus! "wm-scan/r12-apparatus" "ret-8" [1 2])]
    (is (false? (:ok result)))
    (is (= :r12/scan-not-a-map (get-in result [:check :reason])))))
