;; claude-3 review controls for c88fca8f, beyond the author's focused tests.
;; Real retained receipt/fixture/source and shared validator; only process boundaries stubbed.
;; Run from futon2 root: bb -cp . <this file>
(require '[clojure.test :refer [deftest is run-tests]]
         '[clojure.edn :as edn]
         '[checks.positive-proof-receipt :as receipt]
         '[checks.variational-free-energy-witness :as witness])

(def proof (edn/read-string (slurp witness/receipt-path)))
(def fixture (edn/read-string (slurp witness/fixture-path)))
(def wsource (slurp witness/witness-source))

(defn stubs [f]
  (with-redefs [receipt/live-toolchain (fn [_] (:toolchain proof))
                receipt/elaborate (fn [_] (:result proof))
                witness/lean-exit (constantly 0)
                witness/lean-source-exit (constantly 0)]
    (f)))

(def no-mutation (fn [_] (throw (ex-info "mutation must not run" {}))))

(deftest baseline-toolchain-drift-is-setup-failure-not-negative-pass
  (stubs #(with-redefs [receipt/live-toolchain (fn [_] (assoc (:toolchain proof) :lean-version "Lean (version 0.0.0-drift)"))
                        witness/lean-source-exit no-mutation]
            (let [r (witness/weakened-control fixture proof wsource)]
              (is (= 1 (:exit r)))
              (is (= :baseline-failed (:status r)))))))

(deftest baseline-elaboration-result-drift-is-setup-failure
  (stubs #(with-redefs [receipt/elaborate (fn [_] (update (:result proof) :axioms conj "sorryAx"))
                        witness/lean-source-exit no-mutation]
            (let [r (witness/weakened-control fixture proof wsource)]
              (is (= 1 (:exit r)))
              (is (= :baseline-failed (:status r)))))))

(deftest edited-fixture-value-is-setup-failure
  (stubs #(with-redefs [witness/lean-source-exit no-mutation]
            (let [r (witness/weakened-control (assoc fixture :expected-variational-F 2) proof wsource)]
              (is (= 1 (:exit r)))
              (is (= :baseline-failed (:status r)))))))

(deftest drift-plus-elaboration-drift-is-not-specific
  (stubs #(let [validate receipt/validate]
            (with-redefs [receipt/validate (fn [r o] (if (seq o)
                                                        {:pass? false :failures [:elaboration-or-axiom-drift :positive-source-drift]}
                                                        (validate r o)))]
              (let [r (witness/weakened-control fixture proof wsource)]
                (is (= 2 (:exit r)))
                (is (= :unexpected-mutation-result (:status r))))))))

(deftest mutation-actually-changes-the-retained-proof-slice
  (let [m (witness/weaken-source wsource)]
    (is (not= wsource m))
    (is (= 1 (count (re-seq #"norm_num \[gaussianReference, variationalFreeEnergy, Channel.all\]" wsource))))
    (is (re-find #":= by\n  rfl" m))))

(let [r (run-tests)]
  (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
