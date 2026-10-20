(ns futon2.aif.hermetic-repair-fixture-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as fixture]))

(use-fixtures :once fixture/with-hermetic-stores)

(deftest phase-log-default-is-hermetic-under-the-repair-fixture
  (let [before (.length (io/file fixture/production-phase-log))
        opts (runner/config {})]
    (runner/emit-phase! opts {:opportunity-id "test/f28-fixture"} {:phase :fixture :transition :start})
    (is (.exists (io/file (:phase-log opts))))
    (is (= before (.length (io/file fixture/production-phase-log)))
        "the default phase emitter must not append the production phase log")))
