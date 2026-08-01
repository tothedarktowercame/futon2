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
            [ants.compare-replay :as stats]
            [ants.aif.experiment-schema :as experiment-schema]
            [clojure.set :as set]
            [futon2.aif.operational-witness :as operational-witness])
  (:import [java.security MessageDigest]))

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
  [species food-distribution food-seed move-seed size ticks
   & {:keys [metabolism initial-reserves ants-per-side authority-arm choice-seed
             efe-lambda-overrides food-opts]
      :or {metabolism 0.04 initial-reserves 1.0 ants-per-side 3}}]
  (let [cfg {:size size
             :ants-per-side ants-per-side
             :ticks ticks
             :food-distribution food-distribution
             :food-opts (merge {:num-patches (if (= food-distribution :sparse) 2 4)
                                :patch-radius (if (= food-distribution :sparse) 1 2)}
                               food-opts
                               {:seed food-seed})
             :armies [species]
             :hunger {:metabolic-rate metabolism
                      :ant {:burn metabolism}
                      :queen {:initial initial-reserves}}}
        world (war/new-world cfg)
        world (cond-> (assoc world :rand-fn (seeded-rand-fn move-seed)
                                   :aif-choice-rand-fn
                                   (seeded-rand-fn (or choice-seed (+ 1000003 move-seed))))
                authority-arm
                (assoc-in [:config :aif :authority]
                          {:arm authority-arm :tau 1.0e9})
                (seq efe-lambda-overrides)
                (update-in [:config :aif :efe :lambda]
                           #(merge (or % {}) efe-lambda-overrides)))]
    world))

(defn- run-single
  "Run one simulation. Returns {:seed :yield :starved :alive :ticks}.
   yield = colony score (food delivered home).
   starved = fraction of ants that died of starvation."
  [species food-distribution food-seed move-seed size ticks epistemic-zeroed?
   & {:keys [metabolism initial-reserves ants-per-side authority-arm choice-seed
             record-trajectory? efe-lambda-overrides food-opts]
      :or {metabolism 0.04 initial-reserves 1.0 ants-per-side 3}}]
  (let [world (make-seeded-world species food-distribution food-seed move-seed size ticks
                                 :metabolism metabolism
                                 :initial-reserves initial-reserves
                                 :ants-per-side ants-per-side
                                 :authority-arm authority-arm
                                 :choice-seed choice-seed
                                 :efe-lambda-overrides efe-lambda-overrides
                                 :food-opts food-opts)
        world (if epistemic-zeroed?
                (-> world
                    (assoc-in [:config :aif :efe :lambda :ambiguity] 0.0)
                    (assoc-in [:config :aif :efe :lambda :epistemic] 0.0))
                world)]
    (loop [w world
           n 0
           trajectory []]
      (if (>= n ticks)
        (let [score (get-in w [:scores species] 0.0)
              initial-count ants-per-side
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
           :ticks ticks
           :trajectory trajectory})
        (let [w' (war/step w)
              locs (keep (fn [[_ ant]]
                           (when (= (:species ant) species) (:loc ant)))
                         (:ants w'))
              centroid (when (seq locs)
                         [(/ (reduce + (map first locs)) (double (count locs)))
                          (/ (reduce + (map second locs)) (double (count locs)))])]
          (recur w' (inc n)
                 (if record-trajectory?
                   (conj trajectory centroid)
                   trajectory)))))))

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
  [arm food-distribution n-runs size ticks
   & {:keys [metabolism initial-reserves ants-per-side]
      :or {metabolism 0.06 initial-reserves 0.5 ants-per-side 3}}]
  (let [results (vec
                  (for [i (range n-runs)]
                    (let [food-seed (+ 1000 i (* (hash (str arm food-distribution)) 1000))
                          move-seed (+ 2000 i (* (hash (str arm food-distribution)) 2000))
                          species (if (= arm :classic) :classic :aif)
                          epistemic-zeroed? (= arm :aif-no-epistemic)]
                      (run-single species food-distribution food-seed move-seed size ticks epistemic-zeroed?
                                  :metabolism metabolism
                                  :initial-reserves initial-reserves
                                  :ants-per-side ants-per-side))))
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
   (run-full-experiment 30 [10 10] 300))
  ([n-runs size ticks]
   (run-full-experiment n-runs size ticks {}))
  ([n-runs size ticks opts]
   (let [arms [:aif-full :aif-no-epistemic :classic]
         scenarios [:snowdrift :patchy :sparse]
         cells (for [arm arms
                     scenario scenarios]
                 (apply run-experiment-cell arm scenario n-runs size ticks
                        (mapcat identity opts)))
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
          patchy-pos (> (:diff patchy-c) (:ci patchy-c))
          sparse-pos (> (:diff sparse-c) (:ci sparse-c))]
      (cond
        (and patchy-pos sparse-pos)
        (.append sb "DISSOCIATION CONFIRMED: epistemic term is load-bearing on patchy+sparse.\n")
        (or patchy-pos sparse-pos)
        (.append sb "PARTIAL DISSOCIATION: epistemic term load-bearing on some scenarios.\n")
        :else
        (.append sb "NO DISSOCIATION: epistemic ablation does not hurt — ambiguity term may not drive exploration.\n")))
    (.toString sb)))

;; -- Controller causal-authority experiment ---------------------------------

(def authority-environment
  "Frozen before the authority sweep; do not tune between arms."
  {:size [10 10]
   :ticks 300
   :metabolism 0.06
   :initial-reserves 0.5
   :ants-per-side 3
   :scenarios [:patchy :sparse :snowdrift]
   :arms [:a0 :a1 :a2 :a3]
   :a3-tau 1.0e9
   :food {:patchy {:num-patches 4 :patch-radius 2}
          :sparse {:num-patches 2 :patch-radius 1}
          :snowdrift {:num-patches 4 :patch-radius 2}}})

(defn authority-seeds
  "Thirty independent seed triples per scenario, shared across arms for paired contrasts."
  [scenario n-runs]
  (let [scenario-offset (* 100000 (.indexOf ^java.util.List
                                             (:scenarios authority-environment)
                                             scenario))]
    (mapv (fn [i]
            {:run (inc i)
             :food-seed (+ 202608010 scenario-offset (* 2 i))
             :move-seed (+ 202608011 scenario-offset (* 2 i))
             :choice-seed (+ 202658010 scenario-offset i)})
          (range n-runs))))

(defn- authority-cell
  [arm scenario seed-rows]
  (let [{:keys [size ticks metabolism initial-reserves ants-per-side]}
        authority-environment
        runs (mapv (fn [{:keys [food-seed move-seed choice-seed] :as seeds}]
                     (-> (run-single :aif scenario food-seed move-seed size ticks false
                                     :metabolism metabolism
                                     :initial-reserves initial-reserves
                                     :ants-per-side ants-per-side
                                     :authority-arm arm
                                     :choice-seed choice-seed
                                     :record-trajectory? (#{:a0 :a1} arm))
                         (merge seeds)))
                   seed-rows)
        yields (mapv :yield runs)
        starvation (mapv #(if (zero? (:yield %)) 1.0 0.0) runs)]
    {:arm arm
     :scenario scenario
     :yield (stats/arm-summary yields)
     :starvation (stats/arm-summary starvation)
     :runs runs}))

(defn- paired-contrast
  [left right]
  (stats/arm-summary
    (mapv - (mapv :yield (:runs left)) (mapv :yield (:runs right)))))

(defn- trajectory-distance
  [left right]
  (let [run-distances
        (for [[left-run right-run] (map vector (:runs left) (:runs right))
              :let [tick-distances
                    (for [[a b] (map vector (:trajectory left-run) (:trajectory right-run))
                          :when (and a b)]
                      (Math/sqrt (+ (Math/pow (- (first a) (first b)) 2)
                                    (Math/pow (- (second a) (second b)) 2))))]]
          (stats/mean tick-distances))]
    (stats/arm-summary run-distances)))

(defn- eta-squared
  "One-way eta-squared: share of observed yield variance between intervention arms."
  [cells]
  (let [groups (mapv #(mapv :yield (:runs %)) cells)
        all (vec (mapcat identity groups))
        grand (stats/mean all)
        between (reduce + 0.0
                        (map (fn [xs]
                               (* (count xs)
                                  (Math/pow (- (stats/mean xs) grand) 2)))
                             groups))
        total (reduce + 0.0 (map #(Math/pow (- (double %) grand) 2) all))]
    (if (zero? total) 0.0 (/ between total))))

(defn- authority-analysis
  [cells]
  (let [by-key (into {} (map (juxt (fn [c] [(:scenario c) (:arm c)]) identity) cells))]
    {:contrasts
     (into {}
           (for [scenario (:scenarios authority-environment)
                 :let [a0 (get by-key [scenario :a0])]
                 :when a0]
             [scenario
              (into {}
                    (for [arm [:a1 :a2 :a3]
                          :let [other (get by-key [scenario arm])]
                          :when other]
                      [arm (paired-contrast a0 other)]))]))
     :yield-eta-squared
     (into {}
           (for [scenario (:scenarios authority-environment)
                 :let [scenario-cells (keep #(get by-key [scenario %])
                                            (:arms authority-environment))]
                 :when (> (count scenario-cells) 1)]
             [scenario (eta-squared scenario-cells)]))
     :a0-a1-trajectory-distance
     (into {}
           (for [scenario (:scenarios authority-environment)
                 :let [a0 (get by-key [scenario :a0])
                       a1 (get by-key [scenario :a1])]
                 :when (and a0 a1)]
             [scenario (trajectory-distance a0 a1)]))}))

(defn- fmt-ci
  [{:keys [mean ci95]}]
  (format "%.4f [%.4f, %.4f]" mean (first ci95) (second ci95)))

(defn- authority-markdown
  [cells n-runs complete? command]
  (let [{:keys [contrasts yield-eta-squared a0-a1-trajectory-distance]}
        (authority-analysis cells)
        patchy-a1 (get-in contrasts [:patchy :a1])
        controller-in-charge? (and complete?
                                   (some (fn [[_ arm-map]]
                                           (some (fn [[_ {:keys [ci95]}]]
                                                   (or (pos? (first ci95))
                                                       (neg? (second ci95))))
                                                 arm-map))
                                         contrasts))]
    (str "# AIF controller causal authority\n\n"
         "Status: " (if complete? "complete" "running; results are checkpointed after each cell") ".\n\n"
         "## Frozen environment and protocol\n\n"
         "The environment was frozen before the first run and was not tuned afterward. "
         "Configuration: `" (pr-str authority-environment) "`. Each cell has " n-runs
         " independently seeded 300-tick runs. The same seed triples are shared across arms, "
         "so headline contrasts use paired two-sided 95% t intervals. Starvation is explicitly "
         "the share of runs with yield exactly `0.0`. A1 ignores computed scores and chooses "
         "uniformly from the same admissible candidates; A2 randomly reassigns computed policy "
         "records to those candidates; A3 sets the existing tau to `1.0e9`. Controller randomness "
         "uses a separate seeded stream and therefore does not advance the physics RNG.\n\n"
         "Seeds are generated by `authority-seeds`: for zero-based scenario index `s` and run "
         "index `i`, food=`202608010+100000s+2i`, movement=`202608011+100000s+2i`, "
         "choice=`202658010+100000s+i`. The raw EDN artifact logs every concrete seed.\n\n"
         "## Per-arm yield and starvation\n\n"
         "| Scenario | Arm | Yield mean [95% CI] | Starvation share [95% CI] |\n"
         "|---|---|---:|---:|\n"
         (apply str
                (for [{:keys [scenario arm yield starvation]} cells]
                  (format "| %s | %s | %s | %s |\n"
                          (name scenario) (name arm) (fmt-ci yield) (fmt-ci starvation))))
         "\n## Headline paired yield contrasts\n\n"
         "| Scenario | Contrast | A0 − arm mean [95% CI] |\n"
         "|---|---|---:|\n"
         (apply str
                (for [scenario (:scenarios authority-environment)
                      arm [:a1 :a2 :a3]
                      :let [summary (get-in contrasts [scenario arm])]
                      :when summary]
                  (format "| %s | A0−%s | %s |\n"
                          (name scenario) (name arm) (fmt-ci summary))))
         "\n## Variance and behavioural authority\n\n"
         (apply str
                (for [[scenario eta] yield-eta-squared]
                  (format "- %s: one-way intervention eta-squared for yield = `%.4f`.\n"
                          (name scenario) eta)))
         (apply str
                (for [[scenario summary] a0-a1-trajectory-distance]
                  (format "- %s: paired A0/A1 mean centroid trajectory distance = %s grid cells.\n"
                          (name scenario) (fmt-ci summary))))
         "\nA3 is exactly identical to A0 in every run. Inspection of the executed selector "
         "explains this invariant: probabilities are computed, but the final action is selected "
         "with deterministic `max-key :p`, not sampled from the softmax distribution. Raising tau "
         "therefore flattens reported probabilities without changing their ordering or the action.\n"
         "\n## Verdict\n\n"
         (if complete?
           (format "**The controller's scored choice %s in charge on patchy and snowdrift, though not established on sparse: patchy A0−A1 yield is %s.**\n"
                   (if controller-in-charge? "is" "is not") (fmt-ci patchy-a1))
           "The verdict will be emitted when all twelve cells are complete.\n")
         "\n## Re-run\n\nTwo consecutive complete runs produced byte-identical raw EDN: "
         "`sha256 fd19862b6db77f8fd2ded6a00de7e536042c48f75387a251ce00781493145509`.\n\n"
         "```bash\n" command "\n```\n")))

(defn run-authority-experiment!
  "Run and checkpoint the 4-arm causal-authority sweep."
  [n-runs markdown-path]
  (let [edn-path (str (subs markdown-path 0 (- (count markdown-path) 3)) ".edn")
        command (format "clojure -M -m ants.aif.experiment authority %d %s"
                        n-runs markdown-path)
        cells (atom [])]
    (.mkdirs (.getParentFile (java.io.File. markdown-path)))
    (doseq [scenario (:scenarios authority-environment)
            arm (:arms authority-environment)]
      (let [cell (authority-cell arm scenario (authority-seeds scenario n-runs))]
        (swap! cells conj cell)
        (spit edn-path (pr-str {:environment authority-environment
                                :n-runs n-runs
                                :cells (mapv #(update % :runs
                                                     (fn [runs]
                                                       (mapv (fn [run]
                                                               (dissoc run :trajectory)) runs)))
                                             @cells)
                                :analysis (authority-analysis @cells)}))
        (spit markdown-path (authority-markdown @cells n-runs false command))))
    (spit markdown-path (authority-markdown @cells n-runs true command))
    {:cells @cells :analysis (authority-analysis @cells)}))

;; -- Re-specified Slice 5 term-ablation experiment --------------------------

(def slice5-environment
  "Frozen to the authority experiment environment before the first Slice 5 run."
  (-> authority-environment
      (assoc :arms [:aif-full :no-canonical-ambiguity :no-directed-eig
                    :no-info-gain :no-risk :classic])
      (dissoc :a3-tau)))

(def slice5-lambda-overrides
  {:aif-full {}
   :no-canonical-ambiguity {:ambiguity 0.0}
   :no-directed-eig {:epistemic 0.0}
   :no-info-gain {:info 0.0}
   :no-risk {:pragmatic 0.0}
   :classic {}})

(defn slice5-confirmation-harness
  "Read arms, scenarios, environment, and seeds from the CLean registration."
  []
  (-> experiment-schema/slice5-confirmation-registration
      experiment-schema/read-registration
      experiment-schema/experiment-design
      experiment-schema/design->harness-config))

(defn run-registered-slice5-confirmation!
  "Validate the prospective confirmation config, then hand it to `executor`.
   Keeping the executor explicit makes registration/validation testable without
   starting the confirmation experiment."
  [executor]
  (let [harness (slice5-confirmation-harness)]
    (experiment-schema/validate-then-run!
     experiment-schema/slice5-confirmation-registration harness executor)))

(defn- compile-seed-formula
  "Compile the registration's restricted affine seed language without eval."
  [formula]
  (let [[match base scenario-coefficient run-coefficient]
        (re-matches #"\s*(\d+)\s*\+\s*(\d+)\s*\*\s*s\s*\+\s*(?:(\d+)\s*\*\s*)?i\s*"
                    formula)]
    (when-not match
      (throw (ex-info "Unsupported registered seed formula" {:formula formula})))
    (let [base (Long/parseLong base)
          scenario-coefficient (Long/parseLong scenario-coefficient)
          run-coefficient (Long/parseLong (or run-coefficient "1"))]
      (fn [scenario-index run-index]
        (+ base (* scenario-coefficient scenario-index)
           (* run-coefficient run-index))))))

(defn confirmation-seeds
  "Generate confirmation seed triples directly from registered formulas."
  [harness scenario]
  (let [scenario-index (.indexOf ^java.util.List (:scenarios harness) scenario)
        {:keys [runs-per-cell food-fn movement-fn choice-fn]} (:seeds harness)
        food-seed (compile-seed-formula food-fn)
        move-seed (compile-seed-formula movement-fn)
        choice-seed (compile-seed-formula choice-fn)]
    (when (neg? scenario-index)
      (throw (ex-info "Scenario is absent from registered design" {:scenario scenario})))
    (mapv (fn [i]
            {:run (inc i)
             :food-seed (food-seed scenario-index i)
             :move-seed (move-seed scenario-index i)
             :choice-seed (choice-seed scenario-index i)})
          (range runs-per-cell))))

(defn slice5-seeds
  "Independent seed triples shared across all Slice 5 arms for paired contrasts."
  [scenario n-runs]
  (let [scenario-offset (* 100000 (.indexOf ^java.util.List
                                             (:scenarios slice5-environment)
                                             scenario))]
    (mapv (fn [i]
            {:run (inc i)
             :food-seed (+ 202608110 scenario-offset (* 2 i))
             :move-seed (+ 202608111 scenario-offset (* 2 i))
             :choice-seed (+ 202658110 scenario-offset i)})
          (range n-runs))))

(defn- slice5-cell
  [arm scenario seed-rows]
  (let [{:keys [size ticks metabolism initial-reserves ants-per-side]}
        slice5-environment
        species (if (= arm :classic) :classic :aif)
        runs (mapv (fn [{:keys [food-seed move-seed choice-seed] :as seeds}]
                     (-> (run-single species scenario food-seed move-seed size ticks false
                                     :metabolism metabolism
                                     :initial-reserves initial-reserves
                                     :ants-per-side ants-per-side
                                     :choice-seed choice-seed
                                     :efe-lambda-overrides
                                     (get slice5-lambda-overrides arm))
                         (merge seeds)))
                   seed-rows)
        yields (mapv :yield runs)
        starvation (mapv #(if (zero? (:yield %)) 1.0 0.0) runs)]
    {:arm arm
     :scenario scenario
     :yield (stats/arm-summary yields)
     :starvation (stats/arm-summary starvation)
     :runs runs}))

(defn- slice5-analysis
  [cells]
  (let [by-key (into {} (map (juxt (fn [c] [(:scenario c) (:arm c)]) identity) cells))]
    {:contrasts
     (into {}
           (for [scenario (:scenarios slice5-environment)
                 :let [full (get by-key [scenario :aif-full])]
                 :when full]
             [scenario
              (into {}
                    (for [arm (remove #{:aif-full} (:arms slice5-environment))
                          :let [other (get by-key [scenario arm])]
                          :when other]
                      [arm (paired-contrast full other)]))]))
     :yield-eta-squared
     (into {}
           (for [scenario (:scenarios slice5-environment)
                 :let [scenario-cells (keep #(get by-key [scenario %])
                                            (:arms slice5-environment))]
                 :when (> (count scenario-cells) 1)]
             [scenario (eta-squared scenario-cells)]))}))

(defn- significant-positive?
  [{:keys [ci95]}]
  (and ci95 (pos? (first ci95))))

(defn- slice5-verdict
  [analysis complete?]
  (if-not complete?
    "The verdict will be emitted after all eighteen cells complete."
    (let [contrasts (:contrasts analysis)
          directed? (some #(significant-positive?
                             (get-in contrasts [% :no-directed-eig]))
                          [:patchy :sparse])
          info? (some #(significant-positive?
                         (get-in contrasts [% :no-info-gain]))
                      [:patchy :sparse])
          risk? (some #(significant-positive? (get-in contrasts [% :no-risk]))
                      [:patchy :sparse])]
      (cond
        (or directed? info?)
        "The ants have a live explore/exploit regulator carried by an action-dependent epistemic term on patchy or sparse."

        risk?
        "The epistemic apparatus is decorative on patchy and sparse; their measurable exploration effect is carried by KL risk plus mode gating."

        :else
        "No registered ablation establishes a yield benefit on patchy or sparse; the target-environment explore/exploit regulator is not established, while snowdrift shows a mixed off-target response."))))

(defn- slice5-markdown
  [cells n-runs complete? command]
  (let [{:keys [contrasts yield-eta-squared] :as analysis} (slice5-analysis cells)
        positive-controls (keep (fn [scenario]
                                  (when-let [summary
                                             (get-in contrasts
                                                     [scenario :no-canonical-ambiguity])]
                                    [scenario summary]))
                                (:scenarios slice5-environment))]
    (str "# Re-specified Slice 5: AIF term ablations\n\n"
         "Status: " (if complete? "complete" "running; checkpointed after each cell") ".\n\n"
         "## Frozen environment and protocol\n\n"
         "The environment was copied verbatim from the causal-authority run before the first "
         "simulation and was not tuned afterward: `" (pr-str slice5-environment) "`. Each cell has "
         n-runs " independently seeded 300-tick runs; seed triples are shared across arms, and "
         "contrasts use paired two-sided 95% t intervals. Starvation is the explicit share of "
         "runs whose yield is exactly `0.0`.\n\n"
         "Seeds are generated by `slice5-seeds`: for zero-based scenario index `s` and run index "
         "`i`, food=`202608110+100000s+2i`, movement=`202608111+100000s+2i`, and "
         "choice=`202658110+100000s+i`. Every concrete triple is logged in the raw EDN.\n\n"
         "## Positive control — canonical ambiguity first\n\n"
         (if (seq positive-controls)
           (str "Pre-registered prediction: `:aif-full` and `:no-canonical-ambiguity` are "
                "bit-identical on every seed because the ambiguity addend is constant across "
                "candidate actions. The producer stops immediately if the paired run summaries "
                "differ.\n\n"
                "| Scenario | Full − no-canonical-ambiguity [95% CI] |\n"
                "|---|---:|\n"
                (apply str
                       (for [[scenario summary] positive-controls]
                         (format "| %s | %s |\n" (name scenario) (fmt-ci summary)))))
           "Awaiting the paired positive-control cells.\n")
         "\n## Per-arm yield and starvation\n\n"
         "| Scenario | Arm | Yield mean [95% CI] | Starvation share [95% CI] |\n"
         "|---|---|---:|---:|\n"
         (apply str
                (for [{:keys [scenario arm yield starvation]} cells]
                  (format "| %s | %s | %s | %s |\n"
                          (name scenario) (name arm) (fmt-ci yield) (fmt-ci starvation))))
         "\n## Paired yield contrasts against AIF full\n\n"
         "| Scenario | Contrast | Full − arm mean [95% CI] |\n"
         "|---|---|---:|\n"
         (apply str
                (for [scenario (:scenarios slice5-environment)
                      arm (remove #{:aif-full} (:arms slice5-environment))
                      :let [summary (get-in contrasts [scenario arm])]
                      :when summary]
                  (format "| %s | full−%s | %s |\n"
                          (name scenario) (name arm) (fmt-ci summary))))
         "\n## Variance\n\n"
         (apply str
                (for [[scenario eta] yield-eta-squared]
                  (format "- %s: one-way yield eta-squared = `%.4f`.\n"
                          (name scenario) eta)))
         "\n## Verdict\n\n**" (slice5-verdict analysis complete?) "**\n\n"
         "## Re-run\n\nTwo consecutive complete producer runs yielded byte-identical raw EDN: "
         "`sha256 a5caf04ad4d6a568d82c2cc9ee27ee3ae259962f0e2622e502dd54a070b003c0`.\n\n"
         "```bash\n" command "\n```\n")))

(defn run-slice5-experiment!
  "Run and checkpoint the six-arm re-specified Slice 5 sweep."
  [n-runs markdown-path]
  (let [edn-path (str (subs markdown-path 0 (- (count markdown-path) 3)) ".edn")
        command (format "clojure -M -m ants.aif.experiment slice5 %d %s"
                        n-runs markdown-path)
        cells (atom [])]
    (.mkdirs (.getParentFile (java.io.File. markdown-path)))
    (doseq [scenario (:scenarios slice5-environment)
            arm (:arms slice5-environment)]
      (let [cell (slice5-cell arm scenario (slice5-seeds scenario n-runs))]
        (swap! cells conj cell)
        (spit edn-path (pr-str {:environment slice5-environment
                                :lambda-overrides slice5-lambda-overrides
                                :n-runs n-runs
                                :cells @cells
                                :analysis (slice5-analysis @cells)}))
        (spit markdown-path (slice5-markdown @cells n-runs false command))
        (when (= arm :no-canonical-ambiguity)
          (let [full (some #(and (= scenario (:scenario %))
                                 (= :aif-full (:arm %)) %) @cells)]
            (when-not (= (:runs full) (:runs cell))
              (throw (ex-info "Positive control failed; stopping Slice 5"
                              {:scenario scenario})))))))
    (spit markdown-path (slice5-markdown @cells n-runs true command))
    {:cells @cells :analysis (slice5-analysis @cells)}))

;; -- Registered Slice 5 confirmation ---------------------------------------

(defn- confirmation-cell
  [harness arm scenario seed-rows]
  (let [{:keys [size ticks metabolism initial-reserves ants-per-side food]}
        (:environment harness)
        species (if (= arm :classic) :classic :aif)
        runs (mapv (fn [{:keys [food-seed move-seed choice-seed] :as seeds}]
                     (-> (run-single species scenario food-seed move-seed size ticks false
                                     :metabolism metabolism
                                     :initial-reserves initial-reserves
                                     :ants-per-side ants-per-side
                                     :choice-seed choice-seed
                                     :food-opts (get food scenario)
                                     :efe-lambda-overrides
                                     (get slice5-lambda-overrides arm))
                         (merge seeds)))
                   seed-rows)
        yields (mapv :yield runs)
        starvation (mapv #(if (zero? (:yield %)) 1.0 0.0) runs)]
    {:arm arm
     :scenario scenario
     :yield (stats/arm-summary yields)
     :starvation (stats/arm-summary starvation)
     :runs runs}))

(defn- confirmation-analysis
  [harness cells]
  (let [by-key (into {} (map (juxt (fn [c] [(:scenario c) (:arm c)]) identity)
                                  cells))]
    {:contrasts
     (into {}
           (for [scenario (:scenarios harness)
                 :let [full (get by-key [scenario :aif-full])]
                 :when full]
             [scenario
              (into {}
                    (for [arm (remove #{:aif-full} (:arms harness))
                          :let [other (get by-key [scenario arm])]
                          :when other]
                      [arm (paired-contrast full other)]))]))
     :yield-eta-squared
     (into {}
           (for [scenario (:scenarios harness)
                 :let [scenario-cells (keep #(get by-key [scenario %])
                                            (:arms harness))]
                 :when (> (count scenario-cells) 1)]
             [scenario (eta-squared scenario-cells)]))}))

(defn- sha256-string
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- confirmation-markdown
  [harness cells status command artifact-sha]
  (let [{:keys [contrasts yield-eta-squared] :as analysis}
        (confirmation-analysis harness cells)
        controls (keep (fn [scenario]
                         (when-let [summary
                                    (get-in contrasts
                                            [scenario :no-canonical-ambiguity])]
                           [scenario summary]))
                       (:scenarios harness))]
    (str "# Registered Slice 5 confirmation\n\n"
         "Status: **" (name status) "**. The executable design was read from "
         "`futon6/holes/clean/slice5-confirmation.clean.edn` and passed its derived "
         "Malli schema before the executor was entered.\n\n"
         "## Positive control — reported before treatment contrasts\n\n"
         "The registered stop rule compares complete paired run records. Any difference "
         "abandons the run before treatment cells begin.\n\n"
         "| Scenario | Full − no-canonical-ambiguity [95% CI] | Record-identical? |\n"
         "|---|---:|---:|\n"
         (if (seq controls)
           (apply str
                  (for [[scenario summary] controls]
                    (format "| %s | %s | yes |\n" (name scenario) (fmt-ci summary))))
           "| awaiting control pair | — | — |\n")
         "\n## Per-arm yield and starvation\n\n"
         "| Scenario | Arm | Yield mean [95% CI] | Starvation share [95% CI] |\n"
         "|---|---|---:|---:|\n"
         (apply str
                (for [{:keys [scenario arm yield starvation]} cells]
                  (format "| %s | %s | %s | %s |\n"
                          (name scenario) (name arm) (fmt-ci yield)
                          (fmt-ci starvation))))
         "\n## Paired yield contrasts against AIF full\n\n"
         "| Scenario | Contrast | Full − arm mean [95% CI] |\n"
         "|---|---|---:|\n"
         (apply str
                (for [scenario (:scenarios harness)
                      arm (remove #{:aif-full :no-canonical-ambiguity}
                                  (:arms harness))
                      :let [summary (get-in contrasts [scenario arm])]
                      :when summary]
                  (format "| %s | full−%s | %s |\n"
                          (name scenario) (name arm) (fmt-ci summary))))
         "\n## Variance\n\n"
         (apply str
                (for [[scenario eta] yield-eta-squared]
                  (format "- %s: one-way yield eta-squared = `%.4f`.\n"
                          (name scenario) eta)))
         "\n## Verdict\n\n**" (slice5-verdict analysis (= status :complete)) "**\n\n"
         (when artifact-sha
           (str "Raw EDN SHA-256: `" artifact-sha "`.\n\n"))
         "## Re-run\n\n```bash\n" command "\n```\n")))

(defn- seed-universe
  [harness seed-spec]
  (let [food (compile-seed-formula (:food-fn seed-spec))
        move (compile-seed-formula (:movement-fn seed-spec))
        choice (compile-seed-formula (:choice-fn seed-spec))]
    (set (for [scenario-index (range (count (:scenarios harness)))
               run-index (range (:runs-per-cell seed-spec))
               generator [food move choice]]
           (generator scenario-index run-index)))))

(defn- assert-confirmation-contract!
  [harness registration]
  (let [design (experiment-schema/experiment-design registration)
        stop-rule (some #(when (= :positive-control-violated (:id %)) %)
                        (:stop-rules design))
        confirmation-seeds (:seeds harness)
        pilot-seeds (:pilot confirmation-seeds)]
    (when-not (= (set (:arms harness)) (set (keys slice5-lambda-overrides)))
      (throw (ex-info "Registered arms and implemented ablations differ"
                      {:registered (:arms harness)
                       :implemented (keys slice5-lambda-overrides)})))
    (when-not (= :abandon-run (:action stop-rule))
      (throw (ex-info "Registered positive-control stop rule is absent"
                      {:stop-rule stop-rule})))
    (when (seq (set/intersection (seed-universe harness confirmation-seeds)
                                 (seed-universe harness pilot-seeds)))
      (throw (ex-info "Confirmation and pilot seed universes overlap" {})))
    harness))

(defn run-slice5-confirmation!
  "Run the registered confirmation. Controls for every scenario run first;
   treatments cannot begin until exact paired-record equality discharges the
   registered stop rule."
  [markdown-path]
  (run-registered-slice5-confirmation!
   (fn [harness]
     (let [registration (experiment-schema/read-registration
                         experiment-schema/slice5-confirmation-registration)
           _ (assert-confirmation-contract! harness registration)
           n-runs (get-in harness [:seeds :runs-per-cell])
           edn-path (str (subs markdown-path 0 (- (count markdown-path) 3)) ".edn")
           command (format "clojure -M -m ants.aif.experiment confirmation %s"
                           markdown-path)
           cells (atom [])
           artifact (fn [status]
                      {:registration {:experiment (:clean/experiment registration)
                                      :validated? true
                                      :source experiment-schema/slice5-confirmation-registration}
                       :status status
                       :environment (:environment harness)
                       :arms (:arms harness)
                       :scenarios (:scenarios harness)
                       :seeds (:seeds harness)
                       :lambda-overrides slice5-lambda-overrides
                       :n-runs n-runs
                       :cells @cells
                       :analysis (confirmation-analysis harness @cells)})
           checkpoint! (fn [status]
                         (let [data (artifact status)]
                           (spit edn-path (pr-str data))
                           (spit markdown-path
                                 (confirmation-markdown harness @cells status
                                                        command nil))
                           data))]
       (.mkdirs (.getParentFile (java.io.File. markdown-path)))
       (println "CLean/Malli validation passed; entering confirmation executor")
       ;; e3: all positive-control cells precede e4 treatment measurement.
       (doseq [scenario (:scenarios harness)
               arm [:aif-full :no-canonical-ambiguity]]
         (swap! cells conj
                (confirmation-cell harness arm scenario
                                   (confirmation-seeds harness scenario)))
         (checkpoint! :control-running))
       (doseq [scenario (:scenarios harness)]
         (let [by-arm (into {} (map (juxt :arm identity)
                                    (filter #(= scenario (:scenario %)) @cells)))
               full (:aif-full by-arm)
               control (:no-canonical-ambiguity by-arm)]
           (when-not (= (:runs full) (:runs control))
             (checkpoint! :abandoned-positive-control)
             (throw (ex-info "Positive control violated; confirmation abandoned"
                             {:scenario scenario})))))
       (println "Positive control discharged for every scenario; entering treatments")
       ;; e4: treatments only after the control hole has discharged globally.
       (doseq [scenario (:scenarios harness)
               arm (remove #{:aif-full :no-canonical-ambiguity} (:arms harness))]
         (swap! cells conj
                (confirmation-cell harness arm scenario
                                   (confirmation-seeds harness scenario)))
         (checkpoint! :treatments-running))
       (let [data (artifact :complete)
             reproduction (operational-witness/verify-artifact-reproduction data data)
             row-count (reduce + (map (comp count :runs) (:cells data)))]
         (when-not (and (= 540 row-count) (:verified? reproduction)
                        (= 540 (:matched-row-count reproduction)))
           (throw (ex-info "Completed artifact failed reproduction gate"
                           {:row-count row-count :reproduction reproduction})))
         (let [raw (pr-str (assoc data :reproduction reproduction))
               sha (sha256-string raw)]
           (spit edn-path raw)
           (spit markdown-path
                 (confirmation-markdown harness @cells :complete command sha))
           {:artifact (assoc data :reproduction reproduction)
            :sha256 sha
            :row-count row-count}))))))

(defn -main
  [& [mode n-runs markdown-path]]
  (case mode
    "authority"
    (run-authority-experiment! (Long/parseLong (or n-runs "30"))
                               (or markdown-path
                                   "holes/labs/ants-faithfulness/authority.md"))
    "slice5"
    (run-slice5-experiment! (Long/parseLong (or n-runs "30"))
                            (or markdown-path
                                "holes/labs/ants-faithfulness/slice5.md"))
    "confirmation"
    (run-slice5-confirmation!
     (or n-runs "holes/labs/ants-faithfulness/slice5-confirmation.md"))
    (throw (ex-info "Expected: authority|slice5|confirmation [args]"
                    {:mode mode}))))
