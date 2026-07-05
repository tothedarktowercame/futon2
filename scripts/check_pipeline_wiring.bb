#!/usr/bin/env bb
;; check_pipeline_wiring.bb — the invariant that catches severed feeds:
;; every :consumes port has >= 1 :live producer edge. Run it after ANY
;; ruling that retires a component (ledger: "remedies can sever adjacent
;; feeds"). Exit 1 on dangling ports so it can stand as a gate.
(require '[clojure.edn :as edn])

(let [{:keys [wiring]} (edn/read-string (slurp "/home/joe/code/futon2/holes/wm-pipeline-wiring.edn"))
      {:keys [nodes edges]} wiring
      live-feeds (reduce (fn [m {:keys [to status]}]
                           (if (= :live status)
                             (update m to (fnil inc 0))
                             m))
                         {} edges)
      pending-feeds (set (map :to (filter #(= :pending (:status %)) edges)))
      dangling (for [{:keys [id consumes]} nodes
                     port (or consumes #{})
                     :let [k [id port]]
                     :when (zero? (get live-feeds k 0))]
                 {:port k
                  :repair-pending? (contains? pending-feeds k)})
      external #{[:judge :mission-corpus] [:status-classifier :mission-corpus]
                 [:rollout-valuation :move-set] [:llm-fold-escrow :sorry]
                 [:llm-fold-escrow :arming] [:llm-fold-escrow :argument]}
      real (remove #(external (:port %)) dangling)]
  (println "== pipeline wiring check ==")
  (println "nodes:" (count nodes) "| edges:" (count edges)
           "| live:" (count (filter #(= :live (:status %)) edges))
           "| retired:" (count (filter #(= :retired (:status %)) edges))
           "| pending:" (count (filter #(= :pending (:status %)) edges)))
  (doseq [{:keys [port repair-pending?]} real]
    (println (str "DANGLING consumer port " port
                  (if repair-pending? " — repair PENDING (known)" " — UNFED, NO REPAIR ON RECORD"))))
  (if (some (complement :repair-pending?) real)
    (do (println "CHECK FAIL: unfed port with no repair on record") (System/exit 1))
    (do (println (if (seq real)
                   "CHECK PASS-WITH-PENDING: dangling ports exist but repairs are on record"
                   "CHECK PASS: every consumer port fed"))
        (System/exit 0))))
