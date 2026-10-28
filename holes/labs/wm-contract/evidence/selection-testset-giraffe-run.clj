(require '[futon3c.giraffe.pass :as gp] '[clojure.edn :as edn])
(doseq [[ev {:keys [chosen names input]}] (sort-by key (edn/read-string (slurp (first *command-line-args*))))]
  (let [r (gp/pass input)]
    (println (str ev "\torder=" (pr-str (mapv (fn [o] [(names (:id o)) (:status o) (:unblocks o)]) (:order r)))
                  "\tclosed=" (pr-str (vec (keep #(when (#{:completed :cancelled} (:state-group %)) (names (:id %))) (:items input)))))
                  "\tchosen=" chosen)))
