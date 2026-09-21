(ns verify-bindings
  (:require [clojure.edn :as edn]))
(let [report (edn/read-string (slurp (first *command-line-args*)))
      rows (into {} (map (juxt :equation :lean)) (get-in report [:equation-lean-join :rows]))]
  (doseq [[id path line] [[:forward-model "DarkTower/WarMachine/PolicyRollout.lean" 114]
                        [:depth "DarkTower/WarMachine/PolicyHorizon.lean" 61]
                        [:dirichlet-accumulation "DarkTower/WarMachine/DirichletLearning.lean" 60]]]
    (assert (= {:found true :path path :line line :kind "def"} (get rows id))
            (str id " did not resolve to its checked declaration: " (pr-str (get rows id)))))
  (println "PASS: R4/R13/R17 resolve to checked source declaration locations"))
