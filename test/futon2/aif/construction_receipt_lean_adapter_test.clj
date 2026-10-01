(ns futon2.aif.construction-receipt-lean-adapter-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.construction-receipt-lean-adapter :as adapter]))

(def fixture "test/fixtures/construction-receipt-lean/two-unit.edn")
(defn fixture-bytes [] (java.nio.file.Files/readAllBytes (.toPath (io/file fixture))))
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo failure (:refusal (ex-data failure)))))

(deftest pinned-runtime-value-renders-the-lean-carrier
  (let [raw (fixture-bytes)
        sha (adapter/sha256 raw)
        {:keys [source-sha256 lean]} (adapter/adapt-bytes raw sha)]
    (is (= sha source-sha256))
    (is (.contains lean (str "Source SHA-256: " sha)))
    (is (.contains lean "def runtimeSemantics : Nat → UnitSemantics"))
    (is (.contains lean "| 0 => ⟨{0}, ∅⟩"))
    (is (.contains lean "| 1 => ⟨{1}, {0}⟩"))
    (is (.contains lean "support := [⟨0, 1, {0}⟩]"))
    (is (.contains lean "meets := [⟨0, 1, 1, [⟨0, 1, {0}⟩], []⟩]"))
    (is (.contains lean "theorem decoded_runtime_receipt_valid"))
    (is (= "99be41624409ce82e7b39ceaa824fb6b22156194faaf1918c61a4f3c38cc9abb"
           (adapter/sha256 (.getBytes lean java.nio.charset.StandardCharsets/UTF_8))))))

(deftest pins-and-domain-fail-closed
  (testing "changed bytes do not decode under the old pin"
    (is (= :adapter/source-pin-mismatch
           (refusal #(adapter/adapt-bytes (fixture-bytes) (apply str (repeat 64 "0")))))))
  (testing "trailing EDN is not ignored"
    (let [raw (.getBytes "{} {}" java.nio.charset.StandardCharsets/UTF_8)]
      (is (= :adapter/trailing-form
             (refusal #(adapter/adapt-bytes raw (adapter/sha256 raw)))))))
  (testing "receipt units cannot escape the interpreted domain"
    (let [raw (.getBytes
               (pr-str {:schema adapter/schema :precedence [:P]
                        :interpretations {:Q {:guard {:needs #{}} :produces #{}}}
                        :construction-receipt {}})
               java.nio.charset.StandardCharsets/UTF_8)]
      (is (= :adapter/domain-mismatch
             (refusal #(adapter/adapt-bytes raw (adapter/sha256 raw))))))))

(deftest forged-known-token-is-preserved-for-lean-to-refute
  (let [input {:schema adapter/schema :precedence [:P :Q]
               :interpretations
               {:P {:guard {:needs #{}} :produces #{:q :forged}}
                :Q {:guard {:needs #{:q}} :produces #{}}
                }
               :construction-receipt
               {:kind :machine-constructed
                :relations {:status :computed
                            :support {:relations [{:from :P :to :Q :tokens #{:forged}}]}
                            :meet {:relations []}
                            :precedence {:relations [{:from :P :to :Q :tokens #{:forged}}]
                                         :linear-extension [:P :Q] :violations []}}}}
        lean (adapter/render input (apply str (repeat 64 "a")))]
    ;; The adapter does not silently repair semantic lies. The exact forged
    ;; value reaches Lean, where decoded_runtime_receipt_valid cannot prove.
    (is (.contains lean "support := [⟨0, 1, {0}⟩]"))
    (is (.contains lean "| 1 => ⟨∅, {1}⟩"))))
