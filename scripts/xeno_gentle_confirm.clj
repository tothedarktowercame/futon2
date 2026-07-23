(ns xeno-gentle-confirm
  "CONFIRMATION RUN for the two positive-mean cells from xeno_gentle_sweep.clj
  (random sigma @ (EVERY 10, eps 0.4) and (EVERY 50, eps 1.0)), on a FRESH
  screened board pool (seeds 400+), as preregistered in that script: a winner
  only counts if it beats baseline on boards it has never seen.

  Both cells had positive mean delta but flat sign tests (3/4 p .625, 3/5 p 1.0)
  on the original 16 held-out boards — means in this metric are zero-inflation-
  dominated, so this run decides whether the hint is real or board-luck.

  Run: cd futon2 && clojure -M scripts/xeno_gentle_confirm.clj [ticks]"
  (:require [ants.aif.policy :as policy]
            [ants.aif.experiment :as experiment]
            [ants.war]))

(def run-single #'experiment/run-single)
(def base-c @#'policy/default-c-vectors)

(def SPECIES :aif)
(def FOOD :patchy)
(def SIZE [30 30])
(def NEAREST-MAX 8.0)

;; board screen, as xeno_loop.clj (arm-independent geometry)
(defn- board-nearest-food [seed]
  (let [w (experiment/make-seeded-world SPECIES FOOD seed (+ 10000 seed) SIZE 300)
        [hx hy] (get-in w [:homes SPECIES])
        ds (keep (fn [[[x y] c]]
                   (when (> (:food c) 0.05)
                     (Math/sqrt (+ (* (- x hx) (- x hx)) (* (- y hy) (- y hy))))))
                 (get-in w [:grid :cells]))]
    (if (seq ds) (apply min ds) 999.0)))

(defn- screen-seeds [candidates want]
  (->> candidates
       (map (fn [s] [s (board-nearest-food s)]))
       (filter (fn [[_ d]] (<= d NEAREST-MAX)))
       (map first)
       (take want)
       vec))

;; propagator, as xeno_gentle_sweep.clj
(defn- pref-channels [c mode]
  (vec (sort (keep (fn [[k v]] (when (number? (:mean v)) k)) (get c mode)))))

(defn- apply-propagator [c sigma eps rng]
  (reduce
   (fn [a mode]
     (let [here (set (pref-channels a mode))
           valid (vec (sort (filter #(and (here %) (here (get sigma % %))) here)))]
       (if (empty? valid)
         a
         (let [k (nth valid (.nextInt ^java.util.Random rng (count valid)))
               dst (get sigma k k)
               old (double (get-in a [mode dst :mean]))
               tgt (- 1.0 (double (get-in a [mode k :mean])))]
           (assoc-in a [mode dst :mean]
                     (+ (* (- 1.0 eps) old) (* eps tgt)))))))
   c (keys c)))

(defn- run-live [sigma seed ticks every eps]
  (let [w0 (experiment/make-seeded-world SPECIES FOOD seed (+ 10000 seed) SIZE ticks)
        rng (java.util.Random. (+ 999 seed))]
    (loop [w w0, n 0, c base-c]
      (if (>= n ticks)
        (double (get-in w [:scores SPECIES] 0.0))
        (let [c' (if (and (pos? eps) (zero? (mod n every)))
                   (apply-propagator c sigma eps rng)
                   c)
              w' (with-redefs [policy/default-c-vectors c']
                   (ants.war/step w))]
          (recur w' (inc n) c'))))))

(defn- mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))
(defn- choose-nk [n k] (reduce * 1.0 (map (fn [i] (/ (double (- n i)) (inc i))) (range k))))
(defn- sign-p [n k]
  (if (zero? n) 1.0
      (min 1.0 (* 2.0 (reduce + (map #(* (choose-nk n %) (Math/pow 0.5 n)) (range k (inc n))))))))
(defn- verdict [a b]
  (let [d (mapv - a b)
        nz (filterv #(not (zero? %)) d)
        k (max (count (filter pos? nz)) (count (filter neg? nz)) 0)
        p (sign-p (count nz) k)]
    {:mean-delta (mean d) :n (count nz) :wins (count (filter pos? nz)) :p p
     :beats (and (< p 0.05) (pos? (mean d)))}))

;; the random sigma from the sweep — MUST be bit-identical (seed 42, 3 transpositions)
(defn- identity-sigma []
  (into {} (map (fn [k] [k k])
                [:food :pher :home-prox :enemy-prox :h :ingest
                 :trail-grad :dist-home :reserve-home :cargo])))

(defn- random-sigma [seed]
  (let [rng (java.util.Random. (long seed))]
    (reduce (fn [s _]
              (let [ks (vec (sort (keys s)))
                    a (nth ks (.nextInt rng (count ks)))
                    b (nth ks (.nextInt rng (count ks)))]
                (assoc s a (get s b) b (get s a))))
            (identity-sigma) (range 3))))

(defn -main [& args]
  (let [ticks (Integer/parseInt (or (first args) "300"))
        fresh (screen-seeds (range 400 900) 16)
        _ (assert (= 16 (count fresh)) "not enough screened fresh boards")
        sigma (random-sigma 42)
        _ (println (format "=== GENTLE-SWEEP CONFIRMATION (fresh pool) ===\n  fresh screened seeds: %s\n  sigma (random, seed 42): %s\n  cells: (EVERY 10, eps 0.4) and (EVERY 50, eps 1.0); ticks %d\n"
                           fresh (pr-str (into (sorted-map) sigma)) ticks))
        y-base (mapv (fn [s] (double (:yield (run-single SPECIES FOOD s (+ 10000 s) SIZE ticks false)))) fresh)
        _ (println (format "  baseline (fresh)          mean %8.3f" (mean y-base)))
        cells [[10 0.4] [50 1.0]]
        results (vec (for [[every eps] cells]
                       (let [ys (mapv (fn [s] (run-live sigma s ticks every eps)) fresh)
                             v (verdict ys y-base)]
                         (println (format "  random EVERY %3d eps %.2f  mean %8.3f   Δ %+8.3f  wins %2d/%2d  p %.4f%s"
                                          every eps (mean ys) (:mean-delta v) (:wins v) (:n v) (:p v)
                                          (if (:beats v) "   *** CONFIRMS ***" "")))
                         (flush)
                         {:every every :eps eps :ys ys :verdict v})))]
    (println (str "\n  verdict: "
                  (if (some #(get-in % [:verdict :beats]) results)
                    "AT LEAST ONE CELL CONFIRMS on fresh boards — re-evolve sigma at that cell next."
                    "NEITHER CELL CONFIRMS — the positive means were board-luck; the sweep's null stands.")))
    (spit "/tmp/xeno-gentle-confirm.edn"
          (pr-str {:ticks ticks :fresh fresh :y-base y-base :sigma sigma :results results}))
    (println "  wrote /tmp/xeno-gentle-confirm.edn")
    (flush)
    (shutdown-agents)))

(apply -main *command-line-args*)
