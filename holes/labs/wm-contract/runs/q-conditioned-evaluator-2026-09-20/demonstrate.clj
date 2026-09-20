(ns demonstrate
  "Generate isolated Q demonstration records; no production admission or tick."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.conditioned-trajectory-test :as t]))

(def demonstrations (t/demonstrations))
(def frozen (into {} (map (fn [[id fixture]] [id (t/score-frozen-example fixture)])
                         t/frozen-examples)))
(defn terms [fixture]
  (mapv #(select-keys (get-in demonstrations [fixture % :certificate :steps 0])
                      [:risk :ambiguity]) [:open-loop :conditioned]))
(def summary
  {:identity (:reason (t/verdict (:identity demonstrations)))
   :symmetric (terms :symmetric) :asymmetric (terms :asymmetric)
   :refused (:reason (t/verdict (:refused demonstrations)))
   :threading (mapv #(get-in demonstrations [:threading % :posterior]) [:first :second])
   :frozen-verdicts
   (into {} (map (fn [[id r]]
                   [id (if (map? (:g r)) (select-keys (:g r) [:status :kind])
                           (select-keys (t/verdict r) [:verdict :reason]))]) frozen))})
(spit (io/file (.getParent (io/file *file*)) "demonstrations.edn")
      (with-out-str
        (pp/pprint {:scope :isolated-mathematical-fixtures
                    :production-admission :not-claimed :summary summary
                    :demonstrations demonstrations :frozen-schema-examples frozen})))
(prn summary)
