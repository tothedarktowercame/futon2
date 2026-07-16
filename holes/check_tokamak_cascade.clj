;; A PROPAGATOR ALTERING THE TOKAMAK CONTROL CASCADE.
;;
;; g : BINS -> ACTS is the controller's policy. A propagator rewrites it:
;;     g[s(bin)] := nu(g[bin])
;; with s : BINS -> BINS and nu : ACTS -> ACTS. Both sides typed; no English.
;;
;; This is the first time today a propagator acts on a carrier where BOTH the
;; index and the value are the domain's own objects. Slots were a pun on 8; ant
;; actions had a semantic index but prose values; here P = propagators, so the
;; theory is rewriting its own controller.
;;
;; Run: bb check_tokamak_cascade.clj

(require '[clojure.edn :as edn] '[clojure.string :as str])

(def C (edn/read-string (slurp "cascade-tokamak-v0.edn")))
(def BINS (get-in C [:cascade/carrier :N :elements]))
(def ACTS (get-in C [:cascade/carrier :P :elements]))
(def g0   (:cascade/policy C))

(println "=== the carrier, from futon5 tokamak_advantage.clj")
(println "    N (bins, ordered by rule count):" BINS)
(println "    P (arms, each a propagator)   :" ACTS)
(println "\n=== the cascade's policy g : BINS -> ACTS")
(doseq [b BINS]
  (let [box (first (filter #(= b (:bin %)) (:cascade/boxes C)))]
    (println (format "    %-12s -> %-12s  [evidence: %s]" b (g0 b) (name (:evidence box))))))

;; ---- the nu test FIRST (oxf-claude-2: it may save you the trip) -------------
(println "\n=== nu : ACTS -> ACTS must be FIXED-POINT-FREE (settles_if_nu_has_fixed_point)")
(defn fpf? [nu dom] (every? #(not= (nu %) %) dom))
(def nu-rotate (zipmap ACTS (concat (rest ACTS) [(first ACTS)])))  ; a 5-cycle
(def nu-bad    (assoc nu-rotate :identity :identity))               ; one fixed point
(println (format "    5-cycle on the arms        : %s" (if (fpf? nu-rotate ACTS) "FIXED-POINT-FREE -> alive" "dead")))
(println (format "    same, but :identity fixed  : %s" (if (fpf? nu-bad ACTS) "alive" "DEAD — settles for EVERY s")))
(println "    !5 = 44 derangements exist, so a live nu is cheap. The test PRUNES, it does not choose.")

;; ---- the shape map ---------------------------------------------------------
;; emacsShift's shape, transported to the bins: each bin writes the one BELOW it
;; (toward death), and :rich is never written.
(def order (vec BINS))
(def s (zipmap order (cons (first order) (butlast order))))   ; k |-> max(k-1,0), on bins
(def FREE (vec (remove (set (vals s)) BINS)))

(println "\n=== s : BINS -> BINS  (emacsShift's shape, on the regime scale)")
(doseq [b BINS] (println (format "    %-12s -> %s" b (s b))))
(println "    image(s) =" (vec (distinct (vals s))))
(println "    FREE = BINS \\ image(s) =" FREE)
(println "    ^ FREE names a REGIME, not a list position: :rich is the bin no")
(println "      rewrite can write. The scaffold is 'the field is rich' — the")
(println "      one state the controller cannot talk itself out of.")
(println "    non-injective: s(:dead) = s(:collapsing) = :dead — two regimes")
(println "      write one target, which is the contention the propagator MAKES.")

;; ---- run it ----------------------------------------------------------------
(defn step [g] (reduce (fn [acc b] (assoc acc (s b) (nu-rotate (g b)))) g BINS))

(println "\n=== the propagator running on the cascade")
(println (format "    %-4s %s" "t" (str/join "  " (map #(format "%-12s" (name %)) BINS))))
(loop [g g0 seen {} t 0]
  (let [row (mapv g BINS)]
    (if-let [prev (seen row)]
      (println (format "    t=%-2d %s  <- CYCLE, first seen t=%d, PERIOD %d" t
                       (str/join "  " (map #(format "%-12s" (name %)) row)) prev (- t prev)))
      (do (println (format "    t=%-2d %s" t (str/join "  " (map #(format "%-12s" (name %)) row))))
          (when (< t 14) (recur (step g) (assoc seen row t) (inc t)))))))

(println "\n=== READ THE ORBIT AS A CONTROLLER, not as a number")
(println "    Each row IS a complete tokamak policy. The propagator is not tuning")
(println "    parameters — it is REWIRING which regime gets which propagator, and")
(println "    every row is a controller you could actually run.")
(println "    :rich holds its arm throughout (FREE), so the rewrite has a fixed")
(println "    point of its own: the one regime whose prescription no amount of")
(println "    propagation can touch.")

(println "\n=== WHAT THIS DOES NOT SHOW")
(println "    Nothing about whether any of these controllers is GOOD. §4b parked")
(println "    the tokamak because the objective is broken (argmax on a band;")
(println "    class membership undecidable), and this cascade deliberately carries")
(println "    no objective. It can be rewritten; it cannot be scored. Two of its")
(println "    five boxes (:dead, :collapsing) have NO evidence — those bins came")
(println "    back EMPTY in the tokamak's own run — so the propagator is happily")
(println "    permuting two authored guesses along with three measured claims,")
(println "    and it cannot tell the difference. Neither can the orbit.")
