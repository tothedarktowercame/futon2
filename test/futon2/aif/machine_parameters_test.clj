(ns futon2.aif.machine-parameters-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.machine-model :as machine-model]
            [futon2.aif.machine-parameters :as parameters]))

(def dir "holes/labs/wm-contract/runs/row-11-parameters")
(def states [:addressed :falsified :foreclosed :refined :reopened :spawned :strengthened])
(def outcomes (:support (machine-model/outcome-authority)))
(defn read-h [file sha]
  (let [r (edn/read-string (slurp (str dir "/" file)))]
    {:id (:id r) :revision (:revision r) :likelihood (:likelihood r)
     :registration {:path (str dir "/" file) :sha256 sha}}))
(def h1 (read-h "identity-transition.edn" "f422378a56316306da66a80c2c7a2c9456c38885871534a139a4d28db8d50b18"))
(def h2 (read-h "controlled-transition.edn" "7b17469efb3b7b5f4703ddefe7d90c1e53c406ebf6cdc63a078b39cb9bb26f48"))
(def model {:model {:id "wm-parameters" :revision "v1"}
            :state-support states :outcome (machine-model/outcome-authority)})
(def policies [{:id "advance-twice"} {:id "advance-then-cascade"}])
(defn state [s] (assoc (zipmap states (repeat 0)) s 1))
(def parameter-state {:model (:model model) :hypotheses [h1 h2]
                      :prior {"identity-transition" 1/2 "controlled-transition" 1/2}
                      :authority :declared-prior :state-distribution (state :spawned)})

(deftest bayes-kernels-and-controls
  (let [result (parameters/parameter-kernels model parameter-state policies outcomes)
        moved (some (fn [[[p _] x]] (and (= p "advance-twice") (:ok x)
                                          (not= (:mass x) (:prior parameter-state))))
                    (:posterior-kernel result))]
    (is (:ok result))
    (is (= (:posterior-predictive result) (:likelihood-marginal result)))
    (is moved)
    (is (some #(= :zero-evidence-conditioning (get-in % [:refusal :kind]))
              (vals (:posterior-kernel result))))
    (is (= :support-mismatch
           (get-in (parameters/parameter-kernels model parameter-state policies (vec (butlast outcomes)))
                   [:refusal :kind])))
    (is (= :registration-pin-mismatch
           (get-in (parameters/parameter-kernels
                    model (assoc-in parameter-state [:hypotheses 0 :registration :path]
                                    (str dir "/identity-transition-mutated.edn"))
                    policies outcomes) [:refusal :kind])))))
