;; THE nu TEST on the ant vocabulary — run before building, per oxf-claude-2.
;;
;; Their result (claimed, and NOT at the emitted sha — see darktower-claims.edn):
;; settles_if_nu_has_fixed_point — if nu has ANY fixed point p, the constant
;; colouring (fun _ => p) satisfies the constraint for EVERY shape map s. So a nu
;; with a fixed point settles instantly regardless of wiring, and settling is
;; death. The requirement on nu is FIXED-POINT-FREENESS, not involution.
;;
;; This is a one-liner per candidate and it kills bad vocabularies before anyone
;; builds one. That is the whole point: cheaper than finding out by running.
;;
;; Run: bb check_ants_nu.clj

(require '[clojure.edn :as edn] '[clojure.string :as str])

(def V (edn/read-string (slurp "ants-controlled-vocabulary-v0.edn")))
(def N (get-in V [:vocab/N :elements]))

(defn fixed-point-free? [P nu] (every? (fn [p] (not= (nu p) p)) P))

(println "=== nu FIXED-POINT test on candidate ant vocabularies")
(println "    N =" N "(index k IS action k — semantic, unlike the flexiarg slot list)\n")

(def candidates
  [{:name "P = {:up :down}, nu = swap"
    :P [:up :down]
    :nu {:up :down :down :up}}
   {:name "P = {:up :down :unchanged}, nu = swap (the COMFORTABLE one)"
    :P [:up :down :unchanged]
    :nu {:up :down :down :up :unchanged :unchanged}}
   {:name "P = {:up :down :unchanged}, nu = rotate (trying to dodge the fixed point)"
    :P [:up :down :unchanged]
    :nu {:up :down :down :unchanged :unchanged :up}}])

(doseq [{:keys [name P nu]} candidates]
  (let [free? (fixed-point-free? P nu)
        fps (filter #(= (nu %) %) P)]
    (println (format "  %-62s %s" name (if free? "ALIVE" "DEAD")))
    (when-not free? (println (format "     fixed point(s): %s -> settles for EVERY shape map s" (vec fps))))))

;; ---------------------------------------------------------------------------
;; The rotate candidate is the interesting one: it IS fixed-point-free, so the
;; three-valued vocabulary is not dead on arrival after all — but rotate is not
;; an involution, and it makes ":unchanged" cycle to ":up", which is nonsense as
;; a reading. Fixed-point-freeness is NECESSARY, not sufficient; it does not buy
;; you meaning.
(println "\n=== so a three-valued direction CAN be revived by a non-involutive nu,")
(println "    but 'unchanged becomes up' is not a reading anyone wants. The test")
(println "    prunes; it does not choose.")

;; ---------------------------------------------------------------------------
;; Does a propagator actually RUN on the live vocabulary?
(println "\n=== one propagator on white-space-scout, N = 4 actions, P = {:up :down}")
(def nu {:up :down :down :up})
(def then (:reading (first (filter #(= :white-space-scout (:id %)) (:patterns/then V)))))
(println "    pattern THEN (partial — :hold is SILENT in the prose):" then)

;; Totality is forced by P having no neutral. Filling the silence is AUTHORING.
;; Marked here rather than smuggled: :hold := :down is MY invention, not the text's.
(def g0 (merge {:hold :down} then))
(println "    totalised (:hold := :down is MINE, not the author's):" g0)

;; a shape map on ACTIONS. Non-injective + non-surjective, like emacsShift.
(def s {:hold :hold, :forage :hold, :return :forage, :pheromone :return})
(def FREE (remove (set (vals s)) N))
(println "    s =" s)
(println "    FREE = N \\ image(s) =" (vec FREE) "  <- never written, only read")

(defn step [g] (reduce (fn [acc k] (assoc acc (s k) (nu (g k)))) g N))
(println "\n    orbit:")
(loop [g g0 seen {} t 0]
  (let [key* (mapv g N)]
    (if-let [prev (seen key*)]
      (println (format "    t=%-2d %s  <- CYCLE, first seen t=%d, PERIOD %d" t (pr-str key*) prev (- t prev)))
      (do (println (format "    t=%-2d %s" t (pr-str key*)))
          (when (< t 12) (recur (step g) (assoc seen key* t) (inc t)))))))

(println "\n    (order of N above: " N ")")
(println "\n=== VERDICT")
(println "    A propagator RUNS on the ant action-vocabulary with a fixed-point-free nu,")
(println "    and its index is semantic: FREE names an ACTION, not a list position.")
(println "    That is strictly better than E-one-propagator-one-pattern.clj, which was")
(println "    a pun on the number 8.")
(println "    BUT the ants' own prose does not fit: every prescription is CONDITIONAL")
(println "    ('until a deposit fires', 'when near the nest'), and :pheromone is pushed")
(println "    BOTH ways under different conditions. A pattern is not (action -> dir); it")
(println "    is (condition -> (action -> dir)) = rho, the level-2 object. So the ant")
(println "    patterns are not propagators — they are what propagators get STAGED BY.")
