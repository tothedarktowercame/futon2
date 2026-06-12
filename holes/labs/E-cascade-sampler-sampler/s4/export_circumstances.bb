#!/usr/bin/env bb
;; EDN -> JSON bridge for the Python side (S4). Deterministic, mechanical.
(require '[clojure.edn :as edn]
         '[cheshire.core :as json])
(let [circs (edn/read-string (slurp "../circumstances-v0.edn"))]
  (spit "circumstances-v0.json"
        (json/generate-string
         (mapv (fn [c]
                 {:id (name (:circumstance/id c))
                  :psi (:psi c)
                  :moves (mapv #(into {} (map (fn [[k v]]
                                                [(if (namespace k)
                                                   (str (namespace k) "_" (name k))
                                                   (name k))
                                                 (if (keyword? v) (name v) v)])
                                              %))
                               (:moves c))})
               circs)
         {:pretty true}))
  (println "exported" (count circs) "circumstances"))
