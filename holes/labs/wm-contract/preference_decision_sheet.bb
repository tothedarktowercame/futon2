#!/usr/bin/env bb
(require '[clojure.edn :as edn]
         '[futon2.aif.preference-discovery :as discovery]
         '[futon2.aif.ruled-outcome-c :as ruled])

(let [spec (edn/read-string (slurp "holes/labs/wm-contract/preference-landscape.edn"))
      result (discovery/extract-landscape "." spec ruled/seeded-c)
      rendered (str (pr-str result) "\n")
      output "holes/labs/wm-contract/preference-decision-sheet.edn"]
  (if (= "--check" (first *command-line-args*))
    (when-not (= rendered (slurp output))
      (throw (ex-info "preference decision sheet is stale" {:output output})))
    (spit output rendered))
  (println "preference_decision_sheet: PASS --" (count (:proposed-components result))
           "proposed," (count (:decision-sheet result)) "questions"))
