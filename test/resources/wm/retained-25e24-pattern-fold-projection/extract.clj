;; Project the retained machinery-150 attempt-002 prediction and fold into a
;; small live-pin fixture. The large checkpoint records are not test inputs.
;;
;; clojure -M test/resources/wm/retained-25e24-pattern-fold-projection/extract.clj
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(def root
  (or (first *command-line-args*)
      "data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002"))

(defn read-checkpoint [name]
  (edn/read-string (slurp (io/file root name))))

(let [selection (read-checkpoint "002-selection.edn")
      construction (read-checkpoint "003-construction.edn")
      prediction (get-in selection [:payload :judgment :token-outcome-prediction])
      conditions (->> (get-in construction [:payload :judgment :fold-output
                                            :wiring :boxes])
                      (mapcat :conditions)
                      (mapv (fn [condition]
                              {:status (:status condition)
                               :witness
                               {:occurrence-id (get-in condition [:witness :occurrence-id])
                                :authored-clause
                                (select-keys (get-in condition
                                                     [:witness :authored-clause])
                                             [:completion-check])}})))
      projection {:prediction (select-keys prediction [:schema :status :target :wanted])
                  :fold-conditions conditions
                  :after "547b191f8fa1e818b0edca02560abf0017b96abd"}
      out (io/file "test/resources/wm/retained-25e24-pattern-fold-projection.edn")]
  (spit out (with-out-str (pp/pprint projection)))
  (println "wrote" (.getPath out)))
