;; Demo: print the outer-selection trace stages of a retained
;; selection file as a table.
;;
;;   clojure -M scripts/outer-selection-trace.clj <selection.edn>
(require '[clojure.edn :as edn]
         '[futon2.aif.outer-selection-trace :as ost])

(let [path (first *command-line-args*)
      receipt (get-in (edn/read-string (slurp path))
                      [:payload :judgment :outer-task-selection])
      t (ost/trace receipt)]
  (printf "%-28s %-64s %5s %5s  %s%n" :stage :where :in :out :dropped)
  (doseq [{:keys [stage where in out dropped] :as s} (:stages t)]
    (printf "%-28s %-64s %5s %5s  %s%n" stage where in out (pr-str dropped))
    (doseq [k (disj (set (keys s)) :stage :where :in :out :dropped)]
      (printf "    %-18s %s%n" k (pr-str (k s)))))
  (flush))
