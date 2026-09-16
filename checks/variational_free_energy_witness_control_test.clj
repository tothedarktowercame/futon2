(ns checks.variational-free-energy-witness-control-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [clojure.edn :as edn]
            [checks.positive-proof-receipt :as receipt]
            [checks.variational-free-energy-witness :as witness]))

(def proof (edn/read-string (slurp witness/receipt-path)))
(def fixture (edn/read-string (slurp witness/fixture-path)))
(def source (slurp witness/witness-source))

(defn with-process-stubs [f]
  ;; Retain actual source/fixture/receipt validation; stub only process boundaries.
  (with-redefs [receipt/live-toolchain (fn [_] (:toolchain proof))
                receipt/elaborate (fn [_] (:result proof))
                witness/lean-exit (constantly 0)
                witness/lean-source-exit (constantly 0)]
    (f)))

(deftest retained-baseline-and-real-weakening
  (with-process-stubs
    #(let [result (witness/weakened-control fixture proof source)]
       (is (= 0 (:exit result)))
       (is (= :expected-source-drift-detected (:status result)))
       (is (true? (get-in result [:baseline :pass?])))
       (is (= [:positive-source-drift] (get-in result [:mutation :failures]))))))

(deftest stale-baselines-are-not-successful-negatives
  (doseq [bad [(assoc-in proof [:source-basis 0 :declarations 0 :sha256] "stale")
               (assoc-in proof [:fixture :sha256] "stale")]]
    (with-process-stubs
      #(with-redefs [witness/lean-source-exit (fn [_] (throw (ex-info "mutation must not run" {})))]
         (let [result (witness/weakened-control fixture bad source)]
           (is (= 1 (:exit result)))
           (is (= :baseline-failed (:status result))))))))

(deftest current-baseline-source-drift-refuses-before-mutation
  (let [read-source slurp
        stale-source (witness/weaken-source source)]
    (with-process-stubs
      #(with-redefs [clojure.core/slurp (fn [path & opts]
                                        (if (= (str path) witness/witness-source)
                                          stale-source (apply read-source path opts)))
                     witness/lean-source-exit (fn [_] (throw (ex-info "mutation must not run" {})))]
         (let [result (witness/weakened-control fixture proof stale-source)]
           (is (= :baseline-failed (:status result)))
           (is (= [:positive-source-drift] (get-in result [:baseline :failures])))
           (is (= 1 (:exit result))))))))

(deftest mutation-must-change-elaborate-and-fail-only-the-intended-law
  (with-process-stubs
    #(do
       (with-redefs [witness/weaken-source identity]
         (is (= :mutation-unchanged (:status (witness/weakened-control fixture proof source)))))
       (with-redefs [witness/lean-source-exit (constantly 1)]
         (is (= :mutation-elaboration-failed (:status (witness/weakened-control fixture proof source)))))
       (with-redefs [witness/lean-exit (constantly 1)]
         (is (= :baseline-elaboration-failed (:status (witness/weakened-control fixture proof source)))))
       (doseq [report [{:pass? false :failures [:fixture-or-adapter-drift]}
                       {:pass? false :failures [:positive-source-drift :toolchain-drift]}
                       {:pass? true :failures []}]]
         (let [validate receipt/validate]
           (with-redefs [receipt/validate (fn [r overrides]
                                           (if (seq overrides) report (validate r overrides)))]
             (let [result (witness/weakened-control fixture proof source)]
               (is (= 2 (:exit result)))
               (is (= :unexpected-mutation-result (:status result))))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [r (run-tests 'checks.variational-free-energy-witness-control-test)]
    (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1))))
