(require '[clojure.edn :as edn]
         '[futon2.aif.machine-model :as machine-model]
         '[futon2.aif.machine-predictive :as predictive])

(def dir "holes/labs/wm-contract/runs/row-11-parameters")
(def row8 (edn/read-string (slurp "holes/labs/wm-contract/runs/row-8-controlled-transition/production-match.edn")))
(def states (:state-support row8))
(def outcome (machine-model/outcome-authority))
(def a (:rows (predictive/declared-outcome-a {:state-support states :outcome outcome})))
(def controlled (get-in row8 [:complete-rows :successor]))
(def action :advance-mission)
(def caveat {:content-status :placeholder-a-content
             :pointer "WORK-REMAINING.md row 6 review note; futon2 fc8c24a5"})
(defn likelihood [id successor]
  {:authority :declared-prior :name (str id "-enacted-action-likelihood")
   :rows (into {} (for [s states] [s (get a (successor s))]))})
(doseq [[filename id successor]
        [["identity-transition.edn" "identity-transition" identity]
         ["controlled-transition.edn" "controlled-transition"
          #(get controlled [% action])]]]
  (spit (str dir "/" filename)
        (pr-str {:schema :wm/parameter-hypothesis-v1 :id id :revision "v1"
                 :likelihood (likelihood id successor)
                 :authority :declared-prior :enacted-action action
                 :content-status (:content-status caveat) :caveat caveat})))
(spit (str dir "/identity-transition-mutated.edn")
      (str (slurp (str dir "/identity-transition.edn")) "\n"))
