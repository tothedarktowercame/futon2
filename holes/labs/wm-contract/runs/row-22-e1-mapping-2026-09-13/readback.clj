(require '[clojure.edn :as edn]
         '[futon2.aif.machine-budget-mapping :as mapping])

(def fixture
  (edn/read-string
   (slurp "holes/labs/wm-contract/runs/row-22-e1-mapping-2026-09-13/production-shaped-candidate-field.edn")))

(def attempted
  (try
    (mapping/map-ranked-support
     (merge {:schema/version mapping/schema-version
             :scope (:scope fixture)
             :ranked-support (:ranked-support fixture)
             :authorities {}}
            (:identity fixture)))
    (catch clojure.lang.ExceptionInfo failure
      {:refusal (:refusal (ex-data failure))
       :message (.getMessage failure)})))

(assert (= 3 (count (:ranked-support fixture))))
(assert (= (:action (get-in fixture [:ranked-support 0]))
           (:action (get-in fixture [:ranked-support 1]))))
(assert (not= (:candidate/id (get-in fixture [:ranked-support 0]))
              (:candidate/id (get-in fixture [:ranked-support 1]))))
(assert (= :r6-r11/authority-missing (:refusal attempted)))

(prn {:scope (:scope fixture)
      :source (:source fixture)
      :candidate-count (count (:ranked-support fixture))
      :duplicate-semantic-action-occurrences
      (mapv :candidate/id (take 2 (:ranked-support fixture)))
      :production-result attempted
      :assertions 4})
