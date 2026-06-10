;; t1_rent.clj — Kill-criterion T1: does multi-step rollout search pay rent?
;; (M-differentiable-substrate §kill-criteria / C-falsifiable-missions A2.)
;; Run: cd futon2 && clojure -M -e "(load-file \"scripts/t1_rent.clj\")"
;;
;; Method: from each root start-state, compare a beam-1 (follow the policy-head
;; :prior greedily) depth-2 rollout against a wide-beam (explore, pick min-G)
;; depth-2 rollout. "Rent" = the wide beam's G strictly beats the greedy-prior G,
;; i.e. lookahead found value the prior missed. Threshold (Fable): >=15% -> keep.
;; cap-overlay is synthesized :claimed for every referenced cap (no farm-zeroing,
;; no "unknown capability id" errors); start-states are the mission+capability roots.
(require '[futon2.aif.rollout :as r] '[clojure.set :as set])
(def MOVES-PATH "/home/joe/code/futon6/data/diffsub-moves.edn")
(def mvs (r/moves (r/load-move-set MOVES-PATH)))
(def cap-overlay (into {} (for [c (distinct (keep :advances-cap mvs))]
                            [c {:props {:capability/status :claimed}}])))
(def roots (vec (set/union (r/mission-roots mvs) (r/capability-roots mvs))))
(def eps 1e-9)
(defn row [node]
  (let [st {:reachable #{node} :arrows {} :cap-overlay cap-overlay}
        gp (r/best-rollout st mvs :depth 2 :top-k 1)
        gb (r/best-rollout st mvs :depth 2 :top-k 8)]
    {:node node :g1 (:G gp) :g8 (:G gb)
     :step1 (count (r/reachable-moves st mvs))
     :policies (count (r/score-policies st mvs :depth 2 :top-k 8))
     :diff-first (not= (get-in gp [:policy 0 :move/id]) (get-in gb [:policy 0 :move/id]))
     :rent (boolean (and (:G gp) (:G gb) (< (double (:G gb)) (- (double (:G gp)) eps))))}))
(def rows (mapv row roots))
(def branchy (filter #(>= (:step1 %) 2) rows))
(def rent (filter :rent rows))
(println "T1 rent measurement on" MOVES-PATH)
(println "  moves" (count mvs) "| roots" (count roots)
         "| branchy roots (>=2 immediate moves)" (count branchy)
         "| beam explored >1 policy for" (count (filter #(> (:policies %) 1) rows)) "roots")
(println "  wide-beam diverged from greedy-prior on first move:" (count (filter :diff-first rows)) "/" (count rows))
(println "  RENT (wide-beam G strictly beats greedy-prior G):" (count rent) "/" (count rows)
         (format "= %.1f%%" (* 100.0 (/ (count rent) (double (count rows))))))
(println "  VERDICT (thresh 15%):" (if (>= (/ (count rent) (double (count rows))) 0.15)
                                     "KEEP rollout"
                                     "0 rent on SELF-GRADED value — prior==value by construction; kill-until-grounded"))
