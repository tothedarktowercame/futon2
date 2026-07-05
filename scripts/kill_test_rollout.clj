;; kill_test_rollout.clj — E-rollout-kill-test (futon2/holes/E-rollout-kill-test.md)
;; Pre-registered (futonzero-alphazero.md §5, 2026-06-09): after scope-grain v2,
;; >= ~15% of top-ranked policies should be NON-GREEDY (multi-step G(pi)
;; strictly beating its greedy prefix) — else the rollout is over-engineering.
;; Read-only. Run: clojure -M scripts/kill_test_rollout.clj [move-set-path]
(ns kill-test-rollout
  (:require [futon2.aif.rollout :as ro]))

(def mined-path "/home/joe/code/futon6/data/diffsub-moves-mined.edn")

(defn -main [& args]
  (let [path (or (first args) mined-path)
        move-set (ro/load-move-set path)
        moves (ro/moves move-set)
        state (ro/seed-roots {:arrows {} :cap-overlay {} :reachable #{}} moves)
        ;; K = 20 (the WM lane's wide selection window); depth 3 to give
        ;; multi-step room beyond the witness case's depth 2.
        scored (ro/score-policies state moves :depth 3 :top-k 5)
        top-k (vec (take 20 scored))
        prefix-g (fn [{:keys [policy]}]
                   (when (seq policy)
                     (:G (ro/project-policy state [(first policy)]))))
        rows (mapv (fn [p]
                     (let [pg (prefix-g p)
                           multi? (> (count (:policy p)) 1)
                           beats? (and multi? pg (< (double (:G p)) (double pg)))]
                       {:len (count (:policy p))
                        :G (:G p)
                        :prefix-G pg
                        :non-greedy? (boolean beats?)
                        :first-move (some-> (first (:policy p)) :move/id)}))
                   top-k)
        n (count rows)
        non-greedy (count (filter :non-greedy? rows))
        frac (if (pos? n) (double (/ non-greedy n)) 0.0)
        greedy-best (ro/greedy-one-step state moves)
        rollout-best (ro/best-rollout state moves :depth 3)]
    (println "== E-rollout-kill-test ==")
    (println "move-set:" path "| moves:" (count moves)
             "| policies scored:" (count scored) "| top-K:" n)
    (println "\nlen distribution (top-K):"
             (frequencies (map :len rows)))
    (println "non-greedy (G < own 1-step prefix G, len>1):" non-greedy "/" n
             (format "= %.1f%%" (* 100 frac)))
    (println "\nglobal best 1-step G:" (:G greedy-best)
             "| global best rollout G:" (:G rollout-best)
             "| rollout strictly better:" (< (double (:G rollout-best))
                                             (double (:G greedy-best)))
             "| best rollout len:" (count (:policy rollout-best)))
    (println "\nper-policy rows:")
    (doseq [r rows] (println " " r))
    (println "\nVERDICT (pre-registered 15% threshold, NOT adjusted):"
             (if (>= frac 0.15) "KEEP — non-greedy structure is dense enough"
                 "KILL-RECOMMENDED — rollout over-engineering at this grain"))))

(-main)
