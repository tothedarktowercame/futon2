(ns futon2.aif.machine-accumulation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.machine-accumulation :as a]))
(def os [:o0 :o1]) (def ss [:spawned :refined])
(def t1 {:id 1 :previous-id nil :observation {:o0 1 :o1 0} :belief {:spawned 1/2 :refined 1/2}})
(def t2 {:id 2 :previous-id 1 :observation {:o0 0 :o1 1} :belief {:spawned 1/4 :refined 3/4}})
(deftest recurrence-and-controls
  (let [z (a/initialize os ss 1) x (a/step z t1) y (a/step x t2)]
    (is (:ok z)) (is (:ok x)) (is (:ok y))
    (is (= 3/2 (get-in x [:concentrations :o0 :spawned])))
    (is (= 5/4 (get-in y [:concentrations :o1 :spawned])))
    (is (a/recurrence-valid? x t2 y))
    (is (= :missing-carried-state (get-in (a/step nil t1) [:refusal :kind])))
    (is (= :carry-chain-gap (get-in (a/step x (assoc t2 :previous-id nil)) [:refusal :kind])))
    (is (= :support-mismatch (get-in (a/step x (update t2 :belief dissoc :refined)) [:refusal :kind])))
    (is (= :invalid-increment (get-in (a/step x (assoc-in t2 [:belief :refined] -1)) [:refusal :kind])))
    (let [recount (a/step (a/initialize os ss 1) (assoc t2 :previous-id nil))]
      (is (false? (a/recurrence-valid? x t2 recount))))))
