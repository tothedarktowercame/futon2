;; contest_run.clj — exit-2/3: the four-way contest over circumstances-v0.
;; Run from the futon2-arguing-worlds worktree:
;;   clojure -M /home/joe/code/futon2/holes/labs/E-cascade-sampler-sampler/s4/contest_run.clj
;; Reads the GFlowNet entries (S4 Python side) and enters them through the
;; sampler protocol; the referee judges all four on realized-G only.
(require '[clojure.edn :as edn]
         '[clojure.data.json :as json]
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

(defn gflownet-results-by-circumstance []
  (let [data (json/read-str (slurp (str lab "s4/gflownet-entries.json")))]
    (into {}
          (map (fn [r]
                 [(get r "circumstance")
                  {:sampler/id :gflownet-tb
                   :wall-clock-ms (double (get r "wall-clock-ms"))
                   :buildouts (mapv (fn [e]
                                      (let [policy (mapv json-move->move (get e "policy"))]
                                        (assoc (#'futon2.aif.arguing-worlds/sampler-buildout
                                                (get e "buildout/id") policy)
                                               :C (get e "C"))))
                                    (get r "entries"))}]))
          (get data "results"))))

(let [circs (edn/read-string (slurp (str lab "circumstances-v0.edn")))
      gfn (gflownet-results-by-circumstance)
      verdicts
      (mapv (fn [c]
              (let [cid (name (:circumstance/id c))
                    field (conj (aw/run-sampler-field
                                 c
                                 [[:incumbent/budget-6 aw/incumbent-sampler]
                                  [:greedy-eps/eps-0-15 aw/greedy-eps-sampler]
                                  [:random-under-budget aw/random-under-budget-sampler]
                                  [:uniform-best-of-k aw/uniform-best-of-k-sampler]])
                                (or (get gfn cid)
                                    {:sampler/id :gflownet-tb :wall-clock-ms 0.0 :buildouts []}))
                    verdict (aw/referee-field-harness (:state c) field)]
                (assoc verdict :circumstance/id (:circumstance/id c))))
            circs)
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
  (spit (str lab "contest-verdicts-v0.edn")
        (with-out-str (clojure.pprint/pprint
                       {:run "contest-v0" :date "2026-06-12"
                        :verdict-counts (frequencies (map :verdict verdicts))
                        :wins wins
                        :per-sampler by-sampler
                        :verdicts (mapv #(select-keys % [:circumstance/id :verdict
                                                         :diversity :partials
                                                         :per-sampler-best])
                                        verdicts)})))
  (println "verdicts:" (frequencies (map :verdict verdicts)))
  (println "wins:" wins)
  (doseq [row by-sampler] (println row))
  (println "wrote" (str lab "contest-verdicts-v0.edn")))
