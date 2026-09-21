(ns futon2.aif.history-admission-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-cohort-test :as cohort-test]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as runner-test]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores runner-test/with-hermetic-traces)
(use-fixtures :each
  (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(defn history-context []
  (let [root (cohort-test/tmp-root)
        path cohort-test/prereg-path
        _ (cohort/activate! path root)
        event (cohort-test/open! root "history/same-opportunity")]
    {:root root :event event
     :directory (io/file root (name (:cohort/id event)) (:attempt/id event))
     :binding {:preregistration path :data-root root :cohort-id (:cohort/id event)
               :sha256 (#'cohort/sha256 (slurp path))}}))

(defn run-initialization! [binding]
  (runner/run-opportunity!
   (assoc (runner-test/isolated-runner-opts)
          :execution-cohort (cohort/resolve-lineage! binding)
          :cohort? true :trigger :wallclock-cron
          :roster-fn (constantly {})
          :run-record-dir (cohort-test/tmp-root))))

(deftest poison-selection-proceeds-and-records-exclusion
  (let [{:keys [directory binding root event]} (history-context)
        source (io/file "/home/joe/code/futon2/data/wm-quarantine/machinery-67-attempt-001/002-selection.edn")
        poison (io/file directory "002-selection.edn")
        before (slurp source)]
    (io/copy source poison)
    (let [result (run-initialization! binding)
          record (cohort/read-edn (:run-record result))
          exclusions (get-in record [:execution-cohort :history-exclusions])
          state (cohort/ledger cohort-test/prereg-path root)]
      (is (= :present (:run-record-status result)))
      (is (not= :initialization (get-in result [:data :failure-stage]))
          (pr-str (select-keys (:data result) [:error :error-data])))
      (is (= 2 (:attempt-count state)))
      (is (= 1 (count exclusions)))
      (is (= :excluded (:history/status (first exclusions))))
      (is (= (.getAbsolutePath poison) (:path (first exclusions))))
      (is (= :unreadable-or-malformed-record (:reason (first exclusions))))
      (is (= event (first (:events (cohort/attempt-history directory)))))
      (is (= before (slurp source) (slurp poison))))))

(deftest unreadable-authoritative-identity-refuses-admission-without-crashing-tick
  (let [{:keys [directory binding root]} (history-context)
        path (io/file directory "001-time-step.edn")]
    (is (= "duplicate scheduler opportunity"
           (try (cohort-test/open! root "history/same-opportunity")
                nil (catch Exception e (.getMessage e)))))
    (spit path "{:invalid :hole/2f9b03b16170}")
    (let [refusal (try (cohort-test/open! root "history/same-opportunity")
                       nil (catch Exception e (ex-data e)))
          resolved (cohort/resolve-lineage! binding)
          result (run-initialization! binding)
          record (cohort/read-edn (:run-record result))]
      (is (= :history-identity-unavailable (:failure-kind refusal)))
      (is (= :cannot-prove-not-duplicate (:reason refusal)))
      (is (= (.getAbsolutePath path) (:path refusal)))
      (is (= refusal (:history-admission-refusal (cohort/lineage-history resolved))))
      (is (= :incomplete (:outcome result)))
      (is (= :history-identity-unavailable (get-in result [:data :failure-kind])))
      (is (= :present (:run-record-status result)))
      (is (= (.getAbsolutePath path) (get-in record [:history-admission-refusal :path])))
      (is (= refusal (get-in record [:execution-cohort :history-admission-refusal])))
      (is (= 1 (:recorded-attempt-count (cohort/ledger cohort-test/prereg-path root)))))))

(deftest identity-refusal-is-recomputed-without-lineage-metadata
  (doseq [bad-content [nil "{}" "{:invalid :hole/2f9b03b16170}"]]
    (let [{:keys [directory binding root]} (history-context)
          path (io/file directory "001-time-step.edn")]
      (if bad-content (spit path bad-content) (io/delete-file path))
      (let [error (try (cohort/execution-preflight binding)
                       nil (catch Exception e (ex-data e)))]
        (is (= :history-identity-unavailable (:failure-kind error)))
        (is (= (.getAbsolutePath path) (:path error)))
        (is (= 1 (:recorded-attempt-count (cohort/ledger cohort-test/prereg-path root))))))))
