;; contest_run_v1.clj — the SIX-arm contest v1 over circumstances-v0.
;; v1 = v0 field + the metric-trained GFlowNet arm (M-substrate-metric v1
;; curvature proxy; metric-C spread verified non-flat 1.06-2.74 before
;; training, per the spread-check discipline).
;; Run from the futon2-arguing-worlds worktree:
;;   clojure -M /home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/s4/contest_run_v1.clj
;; Both GFlowNet arms enter through the sampler protocol from their JSON
;; entry files; the referee judges all six on realized-G only (C never judges).
(require '[clojure.edn :as edn]
         '[clojure.data.json :as json]
         '[clojure.pprint :as pprint]
         '[futon2.aif.arguing-worlds :as aw])

(def lab "/home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/")

(defn json-move->move
  "Invert the s4 JSON export key mapping (namespace_name, keyword->name)."
  [m]
  (into {}
        (map (fn [[k v]]
               (let [kw (case k
                          "move_id" :move/id
                          "move_class" :move/class
                          "move_terminal?" :move/terminal?
                          "source_action" :source/action
                          (keyword k))
                     vv (case kw
                          :move/class (keyword v)
                          v)]
                 [kw vv])))
        m))

(defn gfn-results-by-circumstance
  "Load one GFlowNet arm's entries file into {circumstance-id field-entry}."
  [file sampler-id]
  (let [data (json/read-str (slurp (str lab file)))]
    (into {}
          (map (fn [r]
                 [(get r "circumstance")
                  {:sampler/id sampler-id
                   :wall-clock-ms (double (get r "wall-clock-ms"))
                   :buildouts (mapv (fn [e]
                                      (let [policy (mapv json-move->move (get e "policy"))]
                                        (assoc (#'futon2.aif.arguing-worlds/sampler-buildout
                                                (get e "buildout/id") policy)
                                               :C (get e "C"))))
                                    (get r "entries"))}]))
          (get data "results"))))

(let [circs (edn/read-string (slurp (str lab "circumstances-v0.edn")))
      gfn-arms [["s4/gflownet-entries.json" :gflownet-tb]
                ["s4/gflownet-metric-entries.json" :gflownet-metric]]
      gfn (into {} (map (fn [[f sid]] [sid (gfn-results-by-circumstance f sid)])
                        gfn-arms))
      fields+verdicts
      (mapv (fn [c]
              (let [cid (name (:circumstance/id c))
                    field (into (aw/run-sampler-field
                                 c
                                 [[:incumbent/budget-6 aw/incumbent-sampler]
                                  [:greedy-eps/eps-0-15 aw/greedy-eps-sampler]
                                  [:random-under-budget aw/random-under-budget-sampler]
                                  [:uniform-best-of-k aw/uniform-best-of-k-sampler]])
                                (map (fn [[_ sid]]
                                       (or (get-in gfn [sid cid])
                                           {:sampler/id sid :wall-clock-ms 0.0 :buildouts []}))
                                     gfn-arms))
                    verdict (aw/referee-field-harness (:state c) field)]
                {:field field
                 :verdict (assoc verdict :circumstance/id (:circumstance/id c))}))
            circs)
      verdicts (mapv :verdict fields+verdicts)
      ;; per-sampler entry counts, disclosed (the standing discipline)
      entry-counts (->> fields+verdicts
                        (mapcat :field)
                        (group-by :sampler/id)
                        (map (fn [[sid es]]
                               [sid {:circumstances (count es)
                                     :total-buildouts (reduce + (map (comp count :buildouts) es))}]))
                        (into (sorted-map)))
      wins (frequencies (keep (comp :sampler/id :winner) verdicts))
      by-sampler (->> verdicts
                      (mapcat :per-sampler-best)
                      (group-by :sampler/id)
                      (map (fn [[sid bs]]
                             (let [gs (sort (map :realized-G bs))]
                               {:sampler sid
                                :n (count gs)
                                :median-G (nth gs (quot (count gs) 2))
                                :best-G (first gs)
                                :worst-G (last gs)})))
                      (sort-by :median-G))]
  (spit (str lab "contest-verdicts-v1.edn")
        (with-out-str (pprint/pprint
                       {:run "contest-v1" :date "2026-06-12"
                        :field-note (str "six arms; gflownet-metric trained on metric-C "
                                         "(M-substrate-metric v1), entries truncated to 8 "
                                         "by token-C (flat) = arbitrary-stable subset of "
                                         "distinct selections — conservative for that arm")
                        :entry-counts entry-counts
                        :verdict-counts (frequencies (map :verdict verdicts))
                        :wins wins
                        :per-sampler by-sampler
                        :verdicts (mapv #(select-keys % [:circumstance/id :verdict
                                                         :diversity :partials
                                                         :per-sampler-best])
                                        verdicts)})))
  (println "verdicts:" (frequencies (map :verdict verdicts)))
  (println "wins:" wins)
  (println "entry-counts:" entry-counts)
  (doseq [row by-sampler] (println row))
  (println "wrote" (str lab "contest-verdicts-v1.edn")))
