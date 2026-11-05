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

(def terminal-ending-absent
  {:status :deferred
   :reason :terminal-ending-class-absent
   :run-id "2026-11-01-legacy"
   :classification-absence
   {:reason :terminal-ending-class-unavailable
    :recorded-class :known-typed-failure
    :completed true
    :failure {:kind :pre-author-baseline-unavailable :stage :construction}}})

(deftest terminal-ending-class-absence-discharges-a-recoverable-pending
  (let [{:keys [old path bytes planted]} (with-pending [:ready-gate :by-rank])
        invocation (try
                     {:result (state/discharge-unrecoverable-pending!
                               path old :terminal-ending-class-absent
                               terminal-ending-absent)}
                     (catch Throwable e {:error e}))]
    ;; On pre-PENDING-1 main this is an ArityException, retained as a failed
    ;; assertion rather than a test error for clean red/green evidence.
    (is (nil? (:error invocation)) (some-> (:error invocation) str))
    (when-let [result (:result invocation)]
      (let [after (state/read-state old path)
            receipt (edn/read-string (slurp (:receipt-path result)))]
        (is (= :discharged (:status result)))
        (is (nil? (:pending after)))
        (is (= (dissoc planted :pending) (dissoc after :source))
            "all learned values are unchanged")
        (is (= bytes (slurp (:backup result))) "exact old bytes retained")
        (is (= :terminal-ending-class-absent (get-in receipt [:refusal :kind])))
        (is (= (:classification-absence terminal-ending-absent)
               (get-in receipt [:refusal :classification-absence])))
        (is (= :durable (:source after)) "read-state accepts the receipt")))))

(deftest terminal-ending-class-is-still-recoverable
  (let [{:keys [old path bytes]} (with-pending [:ready-gate :by-rank])
        failure (try
                  (state/discharge-unrecoverable-pending!
                   path old :must-not-discharge
                   {:status :ready :run-id "2026-11-01-legacy"
                    :ending-class :related})
                  nil
                  (catch Throwable e e))]
    (is (= :pending-step-recoverable (:kind (ex-data failure))))
    (is (= bytes (slurp path)) "nothing written")))

(deftest a-state-written-after-inspection-is-not-overwritten
  (let [{:keys [old path bytes planted]} (with-pending nil)
        newer (update planted :click-count inc)]
    ;; a concurrent click's write, as raw bytes: write-state! would re-enter the hook
    (binding [state/*before-state-move* (fn [_ _] (spit path (str (pr-str newer) "\n")))]
      (is (= :pending-discharge-source-changed
             (:kind (ex-data (try (state/discharge-unrecoverable-pending! path old :raced)
                                  (catch clojure.lang.ExceptionInfo e e)))))))
    (is (= (dissoc newer :source) (dissoc (state/read-state old path) :source)) "the newer state stands")
    (is (not= bytes (slurp path)))
    (is (empty? (filter #(re-find #"pending-discharge" (.getName %)) (.listFiles (.getParentFile (io/file path)))))
        "no receipt survives a refused replacement")))

(deftest tampered-discharge-receipt-or-backup-refuses-the-read
  (let [{:keys [old path]} (with-pending nil)
        result (state/discharge-unrecoverable-pending! path old :legacy)
        receipt-path (:receipt-path result)
        receipt (edn/read-string (slurp receipt-path))
        backup (:backup result)
        backup-bytes (slurp backup)]
    (is (= :durable (:source (state/read-state old path))) "a sound receipt reads")
    (spit receipt-path (pr-str (assoc receipt :learning-parameters-equal false)))
    (is (= :invalid-discharge-receipt
           (:kind (ex-data (try (state/read-state old path) (catch clojure.lang.ExceptionInfo e e))))))
    (spit receipt-path (pr-str (assoc-in receipt [:refusal :kind] :unsupported-discharge)))
    (is (= :invalid-discharge-receipt
           (:kind (ex-data (try (state/read-state old path) (catch clojure.lang.ExceptionInfo e e)))))
        "only the two declared refusal kinds are admitted")
    (spit receipt-path (pr-str receipt))
    (spit backup (str backup-bytes "\n"))
    (is (= :invalid-discharge-receipt
           (:kind (ex-data (try (state/read-state old path) (catch clojure.lang.ExceptionInfo e e)))))
        "a backup whose bytes are not the pinned old state")
    (spit backup backup-bytes)
    (is (= :durable (:source (state/read-state old path))))))
