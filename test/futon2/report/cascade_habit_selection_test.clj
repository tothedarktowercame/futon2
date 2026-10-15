(ns futon2.report.cascade-habit-selection-test
  "Selection never reinforces habit. Migrated 2026-10-15 (Q5 route ledger):
  the selection is the production reading-family decision; the habit store
  path it is handed stays byte-identical and unlocked."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as store]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)

(deftest selection-does-not-reinforce
  (store/with-store
   (fn [path]
     (spit path (str (pr-str (habit/read-state path)) "\n"))
     (let [before (slurp path)
           result (route/decide (route/fixture) 1 {:cascade-habit-path path})]
       (is (= 1 (:selector-calls result)))
       (is (some? (get-in result [:decision :action])))
       (is (= before (slurp path)) "selection leaves the habit store byte-identical")
       (is (not (.exists (io/file (str path ".lock")))))))))
