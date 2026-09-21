(ns futon2.aif.h4-production-stage-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.report.cascade-habit-read-test :as stores]
            [futon2.report.war-machine :as wm]))

(use-fixtures :once hermetic/with-hermetic-stores)

(deftest actual-production-decision-records-missing-prefix
  (stores/with-store
   (fn [path]
     (with-redefs [habit/default-path path task/default-root (str path "-missing-task-store")]
       (let [assembled (problems/assemble
                        {:targets [fixture/tick-1-target]
                         :sources (locators/locate-all fixture/tick-1-sources)})
             result (wm/cascade-decision assembled (assoc fixture/live-c-opts :cascade-habit-path path))
             cs (get-in result [:decision :selection-certificate :candidates])]
         (is (seq cs) (pr-str result))
         (doseq [c cs]
           (is (= :not-supplied (:f-status c)))
           (is (nil? (:f c)))
           (is (= (:id c) (get-in c [:f-prefix :policy])))
           (is (= :no-admitted-policy-prefix (get-in c [:f-prefix :reason])))
           (is (= :d-conditioning-consumption-and-policy-prefix-admission
                  (get-in c [:f-prefix :pending-dependency]))))
         (println "H4-PRODUCTION-DECISION-TEMP-STORES-NO-CLICK"
                  (pr-str (mapv #(select-keys % [:id :f :f-status :f-prefix]) cs))))))))
