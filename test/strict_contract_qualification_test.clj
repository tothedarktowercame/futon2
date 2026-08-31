#!/usr/bin/env bb
(ns strict-contract-qualification-test
  (:require [checks.contract-lint :as lint]
            [clojure.test :refer [deftest is run-tests testing]]))

(def source {:module "DarkTower.WarMachine.Holes" :git-sha "contract-sha"})
(def declaration
  {:name "bound" :kind "hole" :owner "owner" :holder "holder"
   :evidence "UnimplementedShape"})
(def binding
  {:witnesses "bound"
   :fixture {:repo "fixture" :path "fixture.edn"}
   :check {:repo "fixture" :path "check.clj" :entrypoint "-main"}
   :run-sha "run-sha" :contract-sha "contract-sha"
   :result :passed :recorded-at "2026-08-31T00:00:00Z"})

(defn lint-with [sha]
  (lint/lint-data {:contract {:source source :declarations [declaration]}
                   :registry [binding]
                   :authority "contract-sha"
                   :sha-fn (constantly sha)
                   :read-fixture (constantly {})}))

(deftest structural-and-freshness-verdicts-are-independent
  (testing "fresh binding passes both verdicts"
    (let [result (lint-with "run-sha")]
      (is (true? (get-in result [:summary :qualification :structural-valid?])))
      (is (true? (get-in result [:summary :qualification :bindings-fresh?])))
      (is (true? (get-in result [:summary :qualification :strict-pass?])))))
  (testing "stale binding preserves structural validity but fails strict qualification"
    (let [result (lint-with "new-run-sha")]
      (is (true? (get-in result [:summary :pass?])))
      (is (true? (get-in result [:summary :qualification :structural-valid?])))
      (is (false? (get-in result [:summary :qualification :bindings-fresh?])))
      (is (false? (get-in result [:summary :qualification :strict-pass?])))
      (is (= ["bound"] (get-in result [:summary :qualification :stale-declarations])))
      (is (= {:rerun-and-rebind 1}
             (get-in result [:summary :qualification :stale-remediation-counts]))))))

(when (= *file* (System/getProperty "babashka.file"))
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
