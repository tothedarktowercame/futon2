(ns futon2.aif.hermetic-repair-fixture-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.declaration-reads-test :as declaration-reads]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as fixture]
            [futon2.aif.meta-injury-store :as meta-injury-store]))

(use-fixtures :once fixture/with-hermetic-stores)

(deftest phase-log-default-is-hermetic-under-the-repair-fixture
  (let [before (.length (io/file fixture/production-phase-log))
        opts (runner/config {})]
    (runner/emit-phase! opts {:opportunity-id "test/f28-fixture"} {:phase :fixture :transition :start})
    (is (.exists (io/file (:phase-log opts))))
    (is (= before (.length (io/file fixture/production-phase-log)))
        "the default phase emitter must not append the production phase log")))

(deftest declaration-run-deposits-only-under-the-hermetic-injury-root
  (let [before (fixture/production-file-set fixture/production-meta-injury-root)
        _ (declaration-reads/record-run (fn [] nil))]
    (is (seq (filter #(.isFile %) (file-seq (io/file meta-injury-store/default-root)))))
    (is (= before (fixture/production-file-set fixture/production-meta-injury-root)))))
