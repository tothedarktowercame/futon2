(ns futon2.report.war-machine-accumulation-test
  (:require [clojure.test :refer [deftest is]] [futon2.report.war-machine :as wm]))
(def entity "e")
(def obs {:c0 1.0 :c1 0.0})
(def beliefs {entity {:spawned 1.0 :refined 0.0}})
(def init {:authority :declared :prior 1.0 :model/revision "v1"})
(defn step [previous id]
  (wm/accumulation-step-for-tick {:previous-record previous :tick-id id :entity-id entity
                                  :observation obs :belief-pre beliefs :belief-post beliefs
                                  :initialization init}))
(defn record [id result]
  {:run/id id :accumulation-state (:state result) :observation obs :mu-post beliefs
   :accumulation-update-input (:update-input result)})
(defn refusal [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(deftest three-tick-chain-and-refusals
  (let [a (step nil "t1") ar (record "t1" a) b (step ar "t2") br (record "t2" b)
        c (step br "t3") cr (record "t3" c)]
    (is (= [nil "t1" "t2"] (mapv #(get-in % [:accumulation-update-input :previous-id]) [ar br cr])))
    (doseq [r [ar br cr]]
      (is (= (:observation r) (get-in r [:accumulation-update-input :observation])))
      (is (= (get-in r [:mu-post entity]) (get-in r [:accumulation-update-input :belief-post]))))
    (is (= 4.0 (get-in cr [:accumulation-state :concentrations :c0 :spawned])))
    (is (= :accumulation-migration-required (refusal #(step (dissoc br :accumulation-state) "bad"))))
    (is (= :carry-chain-gap (refusal #(step (assoc-in ar [:accumulation-state :last-tick] "wrong") "bad"))))
    (is (= :support-mismatch
           (refusal #(wm/accumulation-step-for-tick
                      {:previous-record ar :tick-id "bad" :entity-id entity
                       :observation {:c0 1.0} :belief-pre beliefs :belief-post beliefs
                       :initialization init}))))))
