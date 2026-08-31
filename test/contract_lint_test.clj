#!/usr/bin/env bb
(ns contract-lint-test
  (:require [checks.contract-lint :as lint]
            [cheshire.core :as json]
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

(deftest evidence-shape-checks-have-rejecting-cases
  (let [find-good {:scenarios [{:round-results [{:find {:selected [:p]
                                                         :receipts {:p {:route :r
                                                                        :warrant {:file "p.flexiarg"}}}}}]}]}
        verdict-good {:runs {:ledger-alone {:rows [{:row "O1" :verdict :unknown
                                                     :declaration-source :paper}]}
                             :declared {:rows [{:row "O1" :verdict :self
                                                :declaration-source :paper}]}}
                      :checks {:per-row-sources? true :declared-sound? true}}
        r2-good {:summary {:forms 1} :content-pin {:sha256 (apply str (repeat 64 "a"))}
                 :channel {:values (vec (range 14))}}
        ill-good {:r2ContractCensusWmTrace {:ill-formed 1
                                             :ill-formed-ticks [{:file "tick.edn"
                                                                  :missing [:channel]}]}}
        r8-good {:summary {:forms 3} :content-pin {:sha256 (apply str (repeat 64 "b"))}
                 :r8CensusWmTrace {:counts {:insufficient-inputs 1
                                             :missing-F-computable 1 :stored-F 1}
                                    :ticks {}}}
        tick-good {:startedAt "2026-08-31T00:00:00Z" :traceWritten true
                   :storeBasisCount 1
                   :route [{:fromNode "R2" :toNode "R7" :via "f" :at_ "t"}]}]
    (doseq [[check good bad]
            [[lint/find-receipt-table? find-good (assoc-in find-good [:scenarios 0 :round-results 0 :find :receipts] {})]
             [lint/verdict-table? verdict-good (assoc-in verdict-good [:checks :declared-sound?] false)]
             [lint/r2-tick-list? r2-good (assoc-in r2-good [:channel :values] [])]
             [lint/ill-formed-list? ill-good (assoc-in ill-good [:r2ContractCensusWmTrace :ill-formed] 0)]
             [lint/r8-tick-list? r8-good (assoc-in r8-good [:content-pin :sha256] "short")]
             [lint/r8-disposition-evidence? r8-good (assoc-in r8-good [:r8CensusWmTrace :counts :stored-F] 2)]
             [lint/tick-run-witness? tick-good (assoc tick-good :route [])]]]
      (is (true? (boolean (check good))))
      (is (false? (boolean (check bad)))))))

(deftest live-contract-registered-counts
  (let [contract-path "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"
        authority (get-in (json/parse-string (slurp contract-path) true) [:source :git-sha])
        result (lint/lint-file
                {:contract "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json"
                 :registry "checks/witness-registry.edn"
                 :authority authority})]
    (is (true? (get-in result [:summary :pass?])))
    (is (= {:closed-by-record 49 :refused-implementation 5
            :stale 16 :unwitnessed 12}
           (get-in result [:summary :counts])))))

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
