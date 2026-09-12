(require '[clojure.edn :as edn] '[clojure.pprint :as pp])
(let [path "futon2/holes/labs/wm-contract/worklist.edn"
      w (edn/read-string (slurp path))
      ids #{:RUN4 :RUN13 :F10 :F12 :F11 :U80 :U83 :U84 :U88 :U91 :U92}
      rows (filterv #(ids (:id %)) (:items w))
      prerequisite-ids (set (mapcat :depends-on rows))
      prerequisites (filterv #(and (prerequisite-ids (:id %)) (not (ids (:id %)))) (:items w))
      q (-> w (assoc :source path :as-of "2026-09-11"
                    :items (vec (concat prerequisites rows))
                    :queue-note "Build-worklist input. Canonical statuses preserved. No live RUN4 manifest, capacity or activation. Refresh from canonical worklist before each dispatch; do not infer repair-store eligibility from this snapshot.")
            (dissoc :ancestry-gate))]
 (spit "futon2/holes/labs/wm-contract/runs/WM-BACKLOG-2026-09-11/worklist.edn"
       (with-out-str (pp/pprint q)))
 (prn {:queued-rows (count rows) :prerequisite-rows (count prerequisites)}))
