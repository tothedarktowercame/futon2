(ns ants.aif.experiment
  "M-aif-ants-port Slice 5: the honest foraging experiment (R16 witness).

   Fixes all three original cyberant confounds:
   - PER-RUN FOOD SEED: each run gets a unique food layout seed (not fixed 42)
   - COUNTERBALANCED SPAWN: single-army absolute yield (no spawn-position artifact)
   - REAL BASELINE: a :classic arm alongside :aif-full and :aif-no-epistemic

   Pre-registered contrast:
   Primary: (aif-full − aif-no-epistemic) > 0 on patchy AND sparse (95% CI excludes 0),
   ≈ 0 on snowdrift. Tests whether the epistemic/ambiguity term IS the explore driver.

   Reproducibility: every run is logged with its seed; re-running from the seed
   reproduces the result bit-identically (the R4 golden)."
  (:require [ants.war :as war]
            [ants.aif.core :as aif-core]
            [ants.aif.observe :as observe]
            [clojure.math :as math]))

(defn- seeded-rand-fn
  "Create a deterministic rand-fn from a seed."
  [seed]
  (let [rng (java.util.Random. (long seed))]
    (fn [coll]
      (let [v (vec coll)
            n (count v)]
        (if (pos? n)
          (v (.nextInt rng n))
          nil)))))

(defn make-seeded-world
  "Create a single-army world with a specific food seed and movement seed.
   Single-army removes the spawn-position confound."
  [species food-distribution food-seed move-seed size ticks]
  (let [cfg {:size size
             :ants-per-side 4
             :ticks ticks
             :food food-distribution
             :food-opts {:seed food-seed
                         :num-patches (if (= food-distribution :sparse) 3 5)
                         :patch-radius (if (= food-distribution :sparse) 2 3)}
             :armies [species]}
        world (war/new-world cfg)
        ;; Inject seeded rand-fn for reproducible movement
        world (assoc world :rand-fn (seeded-rand-fn move-seed))]
    world))

(defn- run-single
  "Run one simulation. Returns {:seed :yield :starved :alive :ticks}.
   yield = colony score (food delivered home).
   starved = fraction of ants that died of starvation."
  [species food-distribution food-seed move-seed size ticks epistemic-zeroed? metabolism]
  (let [world (make-seeded-world species food-distribution food-seed move-seed size ticks)
        ;; Harsher metabolism
        world (assoc-in world [:config :hunger :metabolic-rate]
                        (double (or metabolism 0.04)))
        ;; For epistemic ablation: zero BOTH ambiguity AND epistemic lambda
        world (if epistemic-zeroed?
                (-> world
                    (assoc-in [:config :aif :efe :lambda :ambiguity] 0.0)
                    (assoc-in [:config :aif :efe :lambda :epistemic] 0.0))
                world)]
    (loop [w world
           n 0]
      (if (>= n ticks)
        (let [score (get-in w [:scores species] 0.0)
              initial-count (* 4 1) ;; ants-per-side for single army
              final-ants (count (filter #(= (:species (second %)) species) (:ants w)))
              grave-count (count (filter #(and (= (:species %) species)
                                               (= (:cause %) :starvation))
                                         (:graveyard w)))
              starve-fraction (if (pos? initial-count)
                                (/ grave-count (double initial-count))
                                0.0)]
          {:seed food-seed
           :move-seed move-seed
           :yield (double score)
           :starved starve-fraction
           :alive final-ants
           :ticks ticks})
        (recur (war/step w) (inc n))))))

(defn- mean
  [xs]
  (if (seq xs)
    (/ (reduce + xs) (double (count xs)))
    0.0))

(defn- stddev
  [xs]
  (if (< (count xs) 2)
    0.0
    (let [m (mean xs)
          variance (/ (reduce + (map #(Math/pow (- % m) 2) xs))
                      (double (dec (count xs))))]
      (Math/sqrt variance))))

(defn- ci95
  "95% confidence interval half-width (normal approximation)."
  [xs]
  (if (< (count xs) 2)
    0.0
    (* 1.96 (/ (stddev xs) (Math/sqrt (count xs))))))

(defn- run-experiment-cell
  "Run n-runs independently-seeded simulations for one arm × scenario.
   Returns summary statistics."
  [arm food-distribution n-runs size ticks metabolism]
  (let [results (vec
                  (for [i (range n-runs)]
                    (let [food-seed (+ 1000 i (* (hash (str arm food-distribution)) 1000))
                          move-seed (+ 2000 i (* (hash (str arm food-distribution)) 2000))
                          species (if (= arm :classic) :classic :aif)
                          epistemic-zeroed? (= arm :aif-no-epistemic)]
                      (run-single species food-distribution food-seed move-seed size ticks epistemic-zeroed? metabolism))))
        yields (map :yield results)
        starvs (map :starved results)]
    {:arm arm
     :scenario food-distribution
     :n-runs n-runs
     :yield-mean (mean yields)
     :yield-ci (ci95 yields)
     :starve-mean (mean starvs)
     :starve-ci (ci95 starvs)
     :runs results}))

(defn run-full-experiment
  "Run the complete experiment: 3 arms × 3 scenarios × n-runs.

   Returns {:results [cell-summary ...]
            :contrast {:patchy {:diff :ci} :sparse {...} :snowdrift {...}}}
   where diff = aif-full yield − aif-no-epistemic yield."
  ([]
   (run-full-experiment 30 [12 12] 300 0.04))
  ([n-runs size ticks metabolism]
   (let [arms [:aif-full :aif-no-epistemic :classic]
         scenarios [:snowdrift :patchy :sparse]
         cells (for [arm arms
                     scenario scenarios]
                 (run-experiment-cell arm scenario n-runs size ticks metabolism))
         ;; Compute pre-registered contrast
         contrast (into {}
                        (for [scenario scenarios]
                          (let [full (some #(and (= (:arm %) :aif-full)
                                                 (= (:scenario %) scenario) %) cells)
                                noepi (some #(and (= (:arm %) :aif-no-epistemic)
                                                  (= (:scenario %) scenario) %) cells)
                                diff (- (:yield-mean full) (:yield-mean noepi))]
                            [scenario {:diff diff
                                       :full-yield (:yield-mean full)
                                       :noepi-yield (:yield-mean noepi)
                                       ;; CI of the difference (assuming independence)
                                       :ci (Math/sqrt (+ (Math/pow (:yield-ci full) 2)
                                                          (Math/pow (:yield-ci noepi) 2)))}])))
         result {:results cells
                 :contrast contrast}]
     result)))

(defn format-results
  "Format experiment results as a readable table."
  [{:keys [results contrast]}]
  (let [sb (StringBuilder.)]
    (.append sb "\n=== PER-ARM RESULTS ===\n")
    (.append sb (format "%-16s %-12s %6s %10s %10s %10s %10s\n"
                        "ARM" "SCENARIO" "N" "YIELD" "±CI" "STARVE%" "±CI"))
    (.append sb (apply str (repeat 80 "-")))
    (.append sb "\n")
    (doseq [cell (sort-by (juxt :scenario :arm) results)]
      (.append sb (format "%-16s %-12s %6d %10.2f %10.2f %10.1f %10.1f\n"
                          (:arm cell) (:scenario cell) (:n-runs cell)
                          (:yield-mean cell) (:yield-ci cell)
                          (* 100 (:starve-mean cell)) (* 100 (:starve-ci cell)))))
    (.append sb "\n=== PRE-REGISTERED CONTRAST (aif-full − aif-no-epistemic) ===\n")
    (doseq [scenario [:snowdrift :patchy :sparse]]
      (let [c (get contrast scenario)]
        (.append sb (format "%-12s: diff=%+.2f ± %.2f  (full=%.2f, no-epi=%.2f)  CI excludes 0: %s\n"
                            (name scenario) (:diff c) (:ci c)
                            (:full-yield c) (:noepi-yield c)
                            (if (> (Math/abs (:diff c)) (:ci c)) "YES" "no")))))
    (.append sb "\n=== HYPOTHESIS VERDICT ===\n")
    (let [patchy-c (get contrast :patchy)
          sparse-c (get contrast :sparse)
          snow-c (get contrast :snowdrift)
          patchy-pos (> (:diff patchy-c) (:ci patchy-c))
          sparse-pos (> (:diff sparse-c) (:ci sparse-c))
          snow-neutral (< (Math/abs (:diff snow-c)) (:ci snow-c))]
      (cond
        (and patchy-pos sparse-pos)
        (.append sb "DISSOCIATION CONFIRMED: epistemic term is load-bearing on patchy+sparse.\n")
        (or patchy-pos sparse-pos)
        (.append sb "PARTIAL DISSOCIATION: epistemic term load-bearing on some scenarios.\n")
        :else
        (.append sb "NO DISSOCIATION: epistemic ablation does not hurt — ambiguity term may not drive exploration.\n")))
    (.toString sb)))
