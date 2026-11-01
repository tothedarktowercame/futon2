(ns futon2.aif.pending-discharge-test
  "A pending learning step whose legacy selection has no digest and no typed
  steps refuses on every click (clicks 3-4 of 2026-11-01, run ac5bc477). Its
  discharge is typed, backed up, receipted, and refuses what it must."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.outer-state :as state]
            [futon2.aif.makeup-pin-migration-test :as migration]))

(defn- legacy-pending [steps]
  {:run-id "2026-11-01-legacy" :giraffe/run-id "20261101T031213.797Z-legacy"
   :selection (cond-> {:cascade-id :what-others-wait-on-first :cascade-index 0
                       :selection {:selected {:target "M-legacy"}}}
                steps (assoc :steps steps))})

(defn- with-pending [steps]
  (let [{:keys [old path before]} (migration/fixture)
        planted (assoc (dissoc before :source) :pending (legacy-pending steps))]
    (state/write-state! path planted)
    {:old old :path path :bytes (slurp path) :planted planted}))

(deftest unrecoverable-pending-is-discharged-with-backup-and-receipt
  (let [{:keys [old path bytes planted]} (with-pending nil)
        result (state/discharge-unrecoverable-pending! path old :legacy-selection-without-typed-steps)
        after (state/read-state old path)
        receipt (edn/read-string (slurp (:receipt-path result)))]
    (is (= :discharged (:status result)))
    (is (nil? (:pending after)))
    (is (= (dissoc planted :pending) (dissoc after :source)) "learned counts and pin untouched")
    (is (= bytes (slurp (:backup result))) "exact old bytes retained")
    (is (= :pending-steps-unrecoverable (get-in receipt [:refusal :kind])))
    (is (= "2026-11-01-legacy" (:run-id receipt)))
    (is (= :what-others-wait-on-first (:cascade-id receipt)))
    (is (= :no-pending-step
           (:kind (ex-data (try (state/discharge-unrecoverable-pending! path old :again)
                                (catch clojure.lang.ExceptionInfo e e))))))))

(deftest recoverable-pending-is-not-discharged
  (let [{:keys [old path bytes]} (with-pending [:ready-gate :by-rank])]
    (is (= :pending-step-recoverable
           (:kind (ex-data (try (state/discharge-unrecoverable-pending! path old :not-needed)
                                (catch clojure.lang.ExceptionInfo e e))))))
    (is (= bytes (slurp path)) "nothing written")
    (is (empty? (filter #(re-find #"pending-discharge" (.getName %)) (.listFiles (.getParentFile (io/file path))))))))
