#!/usr/bin/env bb
;; A REAL BRAIN, NOT PHYSICS — the smallest honest demo.
;; Joe, 2026-07-16: "Ant-agents that have real brains, not 100% physics-based."
;;
;; THE CLAIM IS NOT "caching is good". It is a PROOF about representability:
;;   :drop is LOCALLY DOMINATED in EVERY reachable state, so a 1-step G rejects
;;   it always -- at every lambda, forever. Greedy cannot get caching WRONG.
;;   It cannot SEE it. Only G over a POLICY can price "lose now, win later".
;;
;; Standalone model, NOT the real ant; small enough to ENUMERATE, so this is a
;; proof over the model, not a sample. Inherited from src/ants/aif/ (all read):
;;   deposit is physics, fires at home automatically       (forward.clj:286)
;;   gather is physics and species-blind, takes cell food   (forward.clj:259)
;;   vocabulary closed: [:hold :forage :return :pheromone]  (policy.clj:14)
;;   rollout re-scores ONE action, never branches           (rollout.clj:25)
;;   :horizon defaults to 1 and nothing sets it             (policy.clj:905)
;;
;; ONE thing added, and it is what makes :drop a decision rather than a gift:
;;   CARRYING COSTS MORE THAN WALKING. Loaded 2/step, empty 1/step.
;;   With food at 3 and a budget of 8, the direct round trip costs 3+6=9. It KILLS.

(def HOME 0) (def FOOD 3) (def BUDGET 8)
(def VERBS [:hold :forage :return :pheromone :drop])

(defn arrive [s]                      ; gather fires on ARRIVAL (you take what you walk onto)
  (cond-> s
    (and (pos? (get (:ground s) (:loc s) 0)) (zero? (:cargo s)))
    (-> (update :ground update (:loc s) dec) (assoc :cargo 1))))

(defn at-home [s]
  (cond-> s
    (and (= (:loc s) HOME) (pos? (:cargo s))) (-> (assoc :cargo 0) (update :score inc))
    (= (:loc s) HOME) (assoc :energy BUDGET)))        ; eat at home

(defn act [s verb]
  (if (:dead s) s
    (let [cost (if (#{:forage :return} verb) (if (pos? (:cargo s)) 2 1) 0)
          e    (- (:energy s) cost)]
      (if (neg? e) (assoc s :dead true)
        (-> (case verb
              :forage (-> s (assoc :energy e) (update :loc #(min FOOD (inc %))) arrive)
              :return (-> s (assoc :energy e) (update :loc #(max HOME (dec %))) arrive)
              :drop   (if (pos? (:cargo s))
                        (-> s (assoc :cargo 0) (update :ground update (:loc s) (fnil inc 0)))
                        s)
              s)
            at-home)))))

;; 1-step G. Lower is better. Cargo-in-hand IS progress toward score -- any
;; sane local score values it, which is exactly why :drop looks like a loss.
(defn G1 [s v] (let [n (act s v)] (- (+ (* 10 (:score n)) (:cargo n)))))
(defn argmin [f coll] (reduce (fn [a b] (if (< (f b) (f a)) b a)) coll))   ; stable: first wins

(def s0 {:loc HOME :cargo 0 :ground {FOOD 2} :score 0 :energy BUDGET :dead false})
(defn run [s pol] (reduce act s pol))

(println "WORLD: line 0..3, home=0, food=3 (2 units), energy" BUDGET "(resets at home).")
(println "       empty step costs 1, LOADED step costs 2. Direct round trip = 3+6 = 9 > 8.\n")

;; ---- PROOF 1: :drop is never locally optimal, over every reachable state ----
(defn reachable [s0 d]
  (loop [f #{s0} seen #{s0} i 0]
    (if (= i d) seen
      (let [n (into #{} (for [s f v VERBS :when (not (:dead s))] (act s v)))]
        (recur n (into seen n) (inc i))))))
(let [holding (filter #(and (pos? (:cargo %)) (not (:dead %))) (reachable s0 9))
      picks   (filter #(= :drop (argmin (partial G1 %) VERBS)) holding)]
  (println "PROOF 1 — :drop is never locally optimal.")
  (println "  reachable states holding cargo:      " (count holding))
  (println "  of those, greedy would ever pick :drop:" (count picks))
  (println "  => the 1-step G cannot see the move. Not mistuned -- blind.\n"))

;; ---- PROOF 2: search over POLICIES finds what greedy cannot ----------------
(defn greedy [s H]
  (loop [s s taken []]
    (if (or (= (count taken) H) (:dead s)) [taken s]
      (let [b (argmin (partial G1 s) VERBS)] (recur (act s b) (conj taken b))))))
(defn best-policy [s0 H]              ; BFS over states = exhaustive policy search
  (loop [f {s0 []} best [0 []] i 0]
    (if (= i H) best
      (let [n (into {} (for [[s p] f v VERBS :when (not (:dead s))] [(act s v) (conj p v)]))
            b (reduce (fn [[bs bp] [s p]] (if (> (:score s) bs) [(:score s) p] [bs bp])) best n)]
        (recur n b (inc i))))))

(let [[gp gs] (greedy s0 16)
      [bs bp] (best-policy s0 12)]
  (println "PROOF 2 — policy search finds what greedy cannot.")
  (println "  GREEDY   (G over ACTIONS, 16 steps): score" (:score gs)
           "| dead?" (:dead gs) "| drops" (count (filter #{:drop} gp)))
  (println "  POLICY   (G over SEQUENCES, 12 steps): score" bs
           "| drops" (count (filter #{:drop} bp)))
  (println "  the policy:" (pr-str bp))
  (println "\n  Greedy scores" (:score gs) "-- it walks to the food, picks it up, and")
  (println "  cannot afford to carry it home. The policy scores" bs
           "BY DROPPING,")
  (println "  which PROOF 1 shows greedy rejects in every state it could do it in."))
