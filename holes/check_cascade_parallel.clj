;; THE PROPAGATOR VERSION, RUN IN PARALLEL — merge, not last-write-wins.
;;
;; Joe, 2026-07-17: "I think you could run the propagator version in parallel!"
;;
;; Two things that turn out to be the same fix. My `step` was a SEQUENTIAL fold:
;;     (reduce (fn [acc b] (assoc acc (s b) (nu (g b)))) g BINS)
;; s is non-injective — s(:dead) = s(:collapsing) = :dead — so two writes land in
;; one cell and assoc keeps the last. Seq order picked the answer.
;;
;; PARALLEL UPDATE is what a propagator network actually does: every cell computes
;; from the PREVIOUS state, simultaneously; colliding writes MERGE; an inconsistent
;; merge is a CONTRADICTION, which the network REPORTS rather than resolves. That
;; is the discipline my fold was missing, and it is the difference between "it
;; settled" and "my fold ran out of bins".
;;
;; NOTE the SAT question and the ITERATION question are different, and I conflated
;; them last turn. check_cascade_sat.clj asked "is there a g with g[s(b)] = nu(g[b])
;; for ALL b simultaneously?" — UNSAT, because the self-loop wants a fixed point of
;; nu. That is a fact about FIXED POINTS OF THE REWRITE. It does NOT say the rewrite
;; cannot be ITERATED. Each row of the orbit is still a policy. Running them is
;; exactly what Joe is asking for, and it is a different experiment.
;;
;; Run: bb check_cascade_parallel.clj

(require '[clojure.edn :as edn] '[clojure.string :as str])

(def C (edn/read-string (slurp "cascade-tokamak-v0.edn")))
(def BINS (get-in C [:cascade/carrier :N :elements]))
;; the policy's VALUES are arm names in the EDN; the rewrite needs the sigmas
;; themselves, since a.sigma leaves the five named arms immediately.
(def g0-names (:cascade/policy C))

(def positional
  {:rotate+2   [2 3 4 5 6 7 0 1]
   :three+five [1 2 0 4 5 6 7 3]
   :sigma-5127 [5 1 2 7 6 0 4 3]
   :rotate+1   [1 2 3 4 5 6 7 0]
   :identity   [0 1 2 3 4 5 6 7]})
(def ACTS (vec (keys positional)))
(defn compose [a b] (mapv #(nth a %) b))
(def name-of (into {} (map (fn [[k v]] [v k]) positional)))
(defn pname [p] (cond (= p :BOTTOM) "_|_ CONTRA" (name-of p) (name (name-of p)) :else "*unnamed*"))

(def g0 (into {} (map (fn [[b arm]] [b (positional arm)]) g0-names)))

(def order-bins (vec BINS))
(def s-shift (zipmap order-bins (cons (first order-bins) (butlast order-bins))))

;; ---- parallel step with MERGE ----------------------------------------------
;; writes-into(c) = { nu(g[b]) : s(b) = c }.  A cell with no writer KEEPS its
;; value — that is the FREE bin, the scaffold. A cell whose writers disagree is
;; :BOTTOM, a contradiction. :BOTTOM is absorbing: nu(:BOTTOM) = :BOTTOM.
(defn pstep [a g]
  (let [nu (fn [v] (if (= v :BOTTOM) :BOTTOM (compose (positional a) v)))]
    (into {}
          (for [c BINS]
            (let [writers (filter #(= c (s-shift %)) BINS)
                  vals (distinct (map #(nu (g %)) writers))]
              [c (cond (empty? writers) (g c)          ; FREE — keeps its value
                       (= 1 (count vals)) (first vals) ; agreed
                       :else :BOTTOM)])))))            ; disagreed — CONTRADICTION

(println "=== who writes each cell (s = emacsShift on the regime scale)")
(doseq [c BINS]
  (let [writers (filter #(= c (s-shift %)) BINS)]
    (println (format "    %-12s <- %-28s %s" c (pr-str (vec writers))
                     (cond (empty? writers) "FREE — the scaffold, no writer"
                           (> (count writers) 1) "<- COLLISION, two writers, one cell"
                           :else "")))))

(println "\n=== WHERE THE CONTRADICTION IS, and why it is CONTINGENT")
(println "    :dead's writers are :dead (the self-loop) and :collapsing. They")
(println "    agree iff nu(g[:dead]) = nu(g[:collapsing]); nu is left-multiplication")
(println "    by a permutation, hence INJECTIVE, so that holds iff")
(println "        g[:dead] = g[:collapsing].")
(println "    The cascade as written has :dead -> :identity, :collapsing -> :rotate+2.")
(println "    DIFFERENT. So the collision is inconsistent at t=0 and :dead goes")
(println "    to _|_ on the first step, for EVERY a. The contradiction is not")
(println "    created by the multiplication — it is ALREADY IN THE CASCADE, and")
(println "    multiplying by a is just what makes it visible.")

(doseq [a [:rotate+2 :rotate+1 :sigma-5127 :identity]]
  (println (format "\n=== cascade*%s, PARALLEL update" (name a)))
  (println (format "    %-4s %s" "t" (str/join "  " (map #(format "%-12s" (name %)) BINS))))
  (loop [g g0 seen {} t 0]
    (let [row (mapv g BINS)]
      (if-let [prev (seen row)]
        (println (format "    t=%-2d %s  <- CYCLE t=%d, PERIOD %d" t
                         (str/join "  " (map #(format "%-12s" (pname %)) row)) prev (- t prev)))
        (do (println (format "    t=%-2d %s" t (str/join "  " (map #(format "%-12s" (pname %)) row))))
            (when (< t 9) (recur (pstep a g) (assoc seen row t) (inc t))))))))

;; ---- the part that matters for the tokamak ---------------------------------
(println "\n=== IS THE CONTRADICTION VISIBLE TO THE TOKAMAK?")
(println "    E-cascade-tokamak-run1 measured, over 12 seeds x 6 windows:")
(println "        cascade visits :dead 0 times, :collapsing 0 times.")
(println "    The contradiction lives in :dead. The controller never queries :dead.")
(println "    So a cascade with a _|_ in it and a cascade without one produce the")
(println "    SAME TRAJECTORIES. The tokamak cannot tell them apart.")
(println "    That is worth stating plainly: the propagator finds a contradiction")
(println "    at exactly the box we independently established was DECORATION")
(println "    (:evidence :NONE, never fires). The unsat core and the dead box are")
(println "    THE SAME BOX. Two independent methods landed on it.")
(println "    A run cannot falsify a claim about a branch it never takes — TK-2,")
(println "    now about the REWRITE rather than about the controller.")
