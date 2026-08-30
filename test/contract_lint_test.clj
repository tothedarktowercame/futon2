#!/usr/bin/env bb
(ns contract-lint-test
  (:require [checks.contract-lint :as lint]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is run-tests testing]]))

(def source {:module "DarkTower.WarMachine.Holes" :git-sha "contract-sha"})
(defn decl [name kind evidence]
  {:name name :kind kind :owner "owner" :holder "holder" :evidence evidence})
(defn witness-binding [name result fixture]
  {:witnesses name :fixture {:repo "fixture" :path fixture}
   :run-sha "run-sha" :contract-sha "contract-sha"
   :result result :recorded-at "2026-08-30T00:00:00Z"})
(def fixtures
  {"good" (edn/read-string (slurp "test/fixtures/contract/ablation-good.edn"))
   "bad" (edn/read-string (slurp "test/fixtures/contract/ablation-bad.edn"))})
(defn run [decls registry & [authority sha-fn]]
  (lint/lint-data {:contract {:source source :declarations decls}
                   :registry registry :authority (or authority "contract-sha")
                   :sha-fn (or sha-fn (constantly "run-sha"))
                   :read-fixture #(fixtures (:path %))}))

(deftest every-judgement-has-a-falsifier
  (let [ds [(decl "closed" "closed" nil)
            (decl "refused" "hole" nil)
            (decl "missing" "hole" "OtherTable")
            (decl "failed" "hole" "OtherTable")
            (decl "untyped" "hole" "OtherTable")
            (decl "bad" "hole" "AblationTable")
            (decl "good" "hole" "AblationTable")]
        bs [(witness-binding "failed" :failed "good")
            (witness-binding "untyped" :passed "good")
            (witness-binding "bad" :passed "bad")
            (witness-binding "good" :passed "good")]
        result (run ds bs)
        by-name (into {} (map (juxt :name :judgement) (:declarations result)))]
    (is (= {"closed" :closed-by-record "refused" :refused-implementation
            "missing" :unwitnessed "failed" :witness-failed "untyped" :witnessed
            "bad" :wrong-shape "good" :conformant} by-name))
    (testing "current bindings become stale when either recorded sha drifts"
      (is (= :stale (-> (run [(decl "good" "hole" "AblationTable")]
                              [(witness-binding "good" :passed "good")]
                              nil (constantly "new-sha"))
                         :declarations first :judgement))))
    (testing "authority mismatch precedes witness judgement"
      (is (= :wrong-authority
             (-> (run [(decl "good" "hole" "AblationTable")]
                      [(witness-binding "good" :passed "good")] "wrong")
                 :declarations first :judgement))))))

(deftest malformed-registry-fails-closed
  (doseq [b [(witness-binding "not-declared" :passed "good")
             (dissoc (witness-binding "good" :passed "good") :result)
             (dissoc (witness-binding "good" :passed "good") :recorded-at)]]
    (is (false? (get-in (run [(decl "good" "hole" "AblationTable")] [b])
                        [:summary :pass?])))))

(deftest live-contract-registered-counts
  (let [result (lint/lint-file
                {:contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"
                 :registry "checks/witness-registry.edn"
                 :authority "32b929692533cf0d6294fbb7a5937f9199c1552c"})]
    (is (true? (get-in result [:summary :pass?])))
    (is (= {:closed-by-record 24 :conformant 1 :refused-implementation 3
            :unwitnessed 16 :witnessed 4}
           (get-in result [:summary :counts])))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
