#!/usr/bin/env bb
;; Emit the build order for the AIF equation registry.
;;
;; Joe's sequencing rule (2026-09-01): implement in dependency order -- whatever
;; consumes a variable is built after the thing that defines it -- working up
;; from the base of the DAG.
;;
;; The registry already carries that relation: every equation has :defines (one
;; symbol) and :imports (the symbols it consumes). This script reads them and
;; emits the layers.
;;
;; ONE THING THE RULE MEETS HERE: the tick-level graph is NOT a DAG. Active
;; inference is a loop, and the registry is a control map. :belief-state imports
;; :mu-next, which the PREVIOUS tick's :belief-update defined -- a carry across
;; the tick boundary, not a dependency within a tick. Cutting exactly that edge
;; makes the within-tick graph acyclic. Any further cycle is a real finding and
;; is printed as one, not silently cut.
(require '[clojure.edn :as edn]
         '[clojure.set :as set])

(def across-tick-carries
  "Edges [consumer producer] where the consumed value comes from the previous
   tick. Each is an input to the tick, not a dependency within it. Adding to
   this set is a claim about the machine and belongs in the technote."
  #{[:belief-state :belief-update]})

(defn -main [& args]
  (let [path (or (first args) "aif-equations.edn")
        m (edn/read-string (slurp path))
        eqs (:equations m)
        def-by (into {} (map (juxt :defines :id)) eqs)
        holes (into {} (map (fn [h] [(or (:edge h) (:edges h)) (:status h)])) (:holes m))
        deps (into {} (map (fn [e]
                             [(:id e)
                              (set (remove #(across-tick-carries [(:id e) %])
                                           (keep def-by (:imports e))))]))
                   eqs)
        by-id (into {} (map (juxt :id identity)) eqs)]
    (println (format "build_order: %d equations, %d across-tick carries cut"
                     (count eqs) (count across-tick-carries)))
    (loop [layers [] left (set (map :id eqs))]
      (if (empty? left)
        (doseq [[i lvl] (map-indexed vector layers)
                id (sort lvl)
                :let [e (by-id id)]]
          (println (format "L%d  %-26s %-6s defines %-10s %s"
                           i (str id) (str (:node e)) (str (:defines e))
                           (if (= :shortcut-to-retire (:status e))
                             "[shortcut-to-retire]" ""))))
        (let [ready (set (filter (fn [id] (every? #(not (left %)) (deps id))) left))]
          (if (empty? ready)
            (do (println "CYCLE, not cut and not ignored:" (pr-str (vec (sort left))))
                (System/exit 1))
            (recur (conj layers ready) (set/difference left ready))))))
    (when (seq holes)
      (println)
      (println "holes (edges whose equation is not realised):")
      (doseq [[edge status] holes] (println " " (pr-str edge) status)))))

(apply -main *command-line-args*)
