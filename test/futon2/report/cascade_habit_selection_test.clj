(ns futon2.report.cascade-habit-selection-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as store]
            [futon2.report.war-machine :as wm]))

(deftest selection-does-not-reinforce
  (store/with-store
   (fn [path]
     (spit path (str (pr-str (habit/read-state path)) "\n"))
     (let [before (slurp path)
           assembled (problems/assemble {:targets [fixture/tick-1-target]
                                         :sources (locators/locate-all fixture/tick-1-sources)})
           result (wm/select-and-record-cascade! assembled (assoc fixture/live-c-opts :cascade-habit-path path))]
       (is (some? (get-in result [:decision :action])))
       (is (= before (slurp path)) "selection leaves the habit store byte-identical")
       (is (not (.exists (io/file (str path ".lock")))))))))
