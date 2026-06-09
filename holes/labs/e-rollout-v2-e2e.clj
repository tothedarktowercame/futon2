;; e-rollout-v2-e2e.clj — v2 scope-grain seam END-TO-END (claude-4, 2026-06-09).
;; Runs against claude-3's LANDED producer (futon6/data/diffsub-moves.edn,
;; branch diffsub/scope-grain-v2, 801dc62 + 34776c5). Two checks:
;;  (1) zero-drift handshake: root taxonomy counts == claude-3's asserts.
;;  (2) transitive reachability: seed the roots, unroll the chains, confirm the
;;      44 close-holes light up from the 21 mission seeds + 3 summits, islands dark.
;; Run: cd ~/code/futon2 && clojure -M -e "(load-file \"holes/labs/e-rollout-v2-e2e.clj\")"
(require '[futon2.aif.rollout :as rollout])

(def moves (rollout/moves (rollout/load-move-set "/home/joe/code/futon6/data/diffsub-moves.edn")))

;; synthetic cap-overlay: every :advances-cap target as a frontier/held cap, so
;; meme.step can route an advance-capability construction without the "unknown
;; capability id" guard firing. (Structural reachability check; the real overlay
;; lives on :7071 — claude-3's note: reachability is purely structural.)
(def cap-overlay
  (into {} (for [cid (keep :advances-cap moves)]
             [cid {:id (str "scope/capability/" cid)
                   :props {:capability/frontier? true :capability/status :held}}])))

(defn fresh [] (rollout/seed-roots {:arrows {} :cap-overlay cap-overlay :reachable #{}} moves))

(defn transitive-reachable
  "Fixpoint: seed the axiom roots, then repeatedly construct every newly-
   reachable NON-terminal move (adding its :want to :reachable) until nothing
   new opens. Returns the set of move-ids ever made reachable (chains unrolled).
   The opened-guard guarantees termination: terminal moves (:centre-mess) never
   mark their arrow :constructed, so without the guard they'd re-offer forever."
  []
  (loop [state (fresh) opened #{}]
    (let [fresh-reach (remove #(opened (:move/id %)) (rollout/reachable-moves state moves))]
      (if (empty? fresh-reach)
        opened
        (recur (reduce rollout/apply-move state (remove :move/terminal? fresh-reach))
               (into opened (map :move/id fresh-reach)))))))

(let [m-roots (rollout/mission-roots moves)
      c-roots (rollout/capability-roots moves)
      j-roots (rollout/conjectural-roots moves)
      d-roots (rollout/drift-roots moves)
      by-class (group-by :move/class moves)
      close-holes (set (map :move/id (:close-hole by-class)))
      adv (:advance-capability by-class)
      summit-ids (set (map :move/id (filter #(c-roots (:have %)) adv)))
      island-ids (set (map :move/id (filter #(j-roots (:have %)) adv)))
      opened (transitive-reachable)
      opened-close (count (filter close-holes opened))
      summits-reachable (count (filter opened summit-ids))
      islands-dark (count (remove opened island-ids))
      ;; the hypergraph-operator depth-5 chain via the actual search
      hg-moves (filter #(re-find #"hypergraph-operator" (str (:have %) (:want %))) moves)
      hg-seed (rollout/seed-roots {:arrows {} :cap-overlay cap-overlay :reachable #{}} hg-moves)
      hg-best (rollout/best-rollout hg-seed hg-moves :depth 5 :top-k 3 :gamma 0.9)
      ok (and (= 21 (count m-roots)) (= 3 (count c-roots)) (= 7 (count j-roots))
              (empty? d-roots) (= opened-close (count close-holes))
              (= 3 summits-reachable) (= 7 islands-dark))]
  (println "\n=== (1) ZERO-DRIFT HANDSHAKE ===")
  (println (format "  mission-roots    = %2d   (expect 21) -> %s" (count m-roots) (= 21 (count m-roots))))
  (println (format "  capability-roots = %2d   (expect  3) -> %s" (count c-roots)
                   (= c-roots #{"scope/capability/apm-prelim-corpus-substrate"
                                "scope/capability/math-ct-prior-substrate"
                                "scope/capability/wm-steps-forward-guardrailed"})))
  (println (format "  conjectural-roots= %2d   (expect  7) -> %s" (count j-roots) (= 7 (count j-roots))))
  (println (format "  drift-roots      = %2d   (expect  0) -> %s" (count d-roots) (empty? d-roots)))
  (when (seq d-roots) (println "    DRIFT:" d-roots))
  (println "\n=== (2) TRANSITIVE REACHABILITY (seed -> unroll chains) ===")
  (println (format "  close-holes reachable from 21 seeds: %d / %d -> %s"
                   opened-close (count close-holes) (= opened-close (count close-holes))))
  (println (format "  cap summits reachable:               %d / 3 -> %s" summits-reachable (= 3 summits-reachable)))
  (println (format "  conjectural islands still DARK:      %d / 7 -> %s" islands-dark (= 7 islands-dark)))
  (println "\n=== (3) hypergraph-operator depth-5 chain (live search) ===")
  (println "  policy:" (mapv :move/id (:policy hg-best)))
  (println "\n=== VERDICT ===")
  (println (if ok
             "  SEAM CONFIRMED — zero drift, all 44 close-holes light up, 3 summits reachable, 7 islands dark."
             "  MISMATCH — see above.")))
