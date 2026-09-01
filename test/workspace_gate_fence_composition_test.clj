(ns workspace-gate-fence-composition-test
  (:require [clojure.test :refer [deftest is]]
            [checks.wm-workspace-gate :as gate]
            [writer-fence-capability :as fence]))

(deftest authority-constituents-explicitly-clear-fence-transport
  (let [positive (some #(when (= :contract-authority-current (:name %)) %)
                       (gate/commands))
        negative (some #(when (= :c175-stale-contract-authority (:name %)) %)
                       (gate/control-commands))]
    (doseq [argv [(:argv positive) (:argv negative)]]
      (is (= ["env" "-u" "FUTON_WRITER_FENCE_ID"
              "-u" "FUTON_WRITER_FENCE_EVIDENCE"]
             (subvec argv 0 5))))))

(deftest only-outer-gate-can-qualify-the-receipt
  (let [movement {:status :stable}
        content-only (gate/gate-event-claim movement nil nil "s" "f")
        conditional (with-redefs [fence/assess
                                  (fn [interval moved? id path]
                                    {:claim :event-free :interval interval
                                     :writer-fence {:status :observed-held
                                                    :id id :path path}
                                     :event-free? (if moved? false true)
                                     :distinguishable-cause? true})]
                      (gate/gate-event-claim movement "fence-1" "receipt.json"
                                             "s" "f"))]
    (is (= :unverified (:event-free? content-only)))
    (is (= :absent (get-in content-only [:writer-fence :status])))
    (is (= true (:event-free? conditional)))
    (is (= :observed-held (get-in conditional [:writer-fence :status])))))
