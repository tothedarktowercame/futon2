(ns ants.compare-replay
  "Controlled statistical replay of the audited CyberAnts boundary.

   The simulation remains unseeded, so reproducibility is distributional:
   raw rows, confidence intervals, and zero-score fractions are persisted.
   `ants.compare` is invoked unchanged for every experimental arm."
  (:require [ants.compare :as compare]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import (java.util ArrayList Collections Random)))

(def default-l5-path
  "data/cyberants-replay/config/l5-creative.edn")

(def default-sigil-path
  "data/cyberants-replay/config/sigil-gradient.edn")

(def default-output-dir
  "data/cyberants-replay")

(def default-control-seed
  20260714)

(def scenarios
  [:patchy :sparse :snowdrift])

(defn- read-edn
  [path]
  (edn/read-string (slurp path)))

(defn- write-edn!
  [path value]
  (io/make-parents path)
  (spit path (str (pr-str value) "\n")))

(defn- write-text!
  [path value]
  (io/make-parents path)
  (spit path value))

(defn- selected-config
  [bundle]
  (or (:cyberant bundle)
      (first (:cyberants bundle))
      (when (map? bundle) bundle)
      (throw (ex-info "No cyberant config in bundle" {:bundle bundle}))))

(defn- shuffled-nonidentity
  [^Random rng values]
  (let [values (vec values)
        copy (ArrayList. values)]
    (Collections/shuffle copy rng)
    (let [candidate (vec copy)]
      (if (and (> (count values) 1) (= candidate values))
        (vec (concat (rest values) [(first values)]))
        candidate))))

(defn- permute-map-values
  [m ^Random rng]
  (if (> (count m) 1)
    (let [ks (vec (sort (keys m)))
          values (shuffled-nonidentity rng (mapv m ks))]
      (reduce (fn [result [k value]] (assoc result k value))
              m
              (map vector ks values)))
    m))

(defn- permute-numeric-values
  [m ^Random rng]
  (let [ks (->> (keys m) (filter #(number? (get m %))) sort vec)]
    (if (> (count ks) 1)
      (let [values (shuffled-nonidentity rng (mapv m ks))]
        (reduce (fn [result [k value]] (assoc result k value))
                m
                (map vector ks values)))
      m)))

(defn- update-map
  [config path f]
  (if (map? (get-in config path))
    (update-in config path f)
    config))

(defn random-wiring-control
  "Permute values among type-compatible pattern-sense destinations.

   Map shape, keys, and value multisets are preserved. This randomizes which
   behavioral signal feeds each named sense terminal without inventing ports."
  [config seed]
  (let [rng (Random. (long seed))]
    (-> config
        (update-map [:pattern-sense] #(permute-map-values % rng))
        (update-map [:adapt-config :switch-to :pattern-sense]
                    #(permute-map-values % rng))
        (assoc :species :cyber-wiring-random-control
               :wiring-id :random-wiring-control
               :replay-control {:kind :random-wiring
                                :seed seed
                                :invariant :type-compatible-destination-permutation}))))

(def shuffled-parameter-paths
  [[:policy-priors]
   [:precision]
   [:pattern-sense]
   [:adapt-config]
   [:adapt-config :switch-to :pattern-sense]
   [:phenotype-coupling]])

(defn shuffled-parameter-control
  "Permute numeric values within each parameter block while preserving keys.

   This keeps the wiring graph fixed and preserves each block's numeric
   multiset (including normalized policy-prior mass)."
  [config seed]
  (let [rng (Random. (long seed))]
    (-> (reduce (fn [result path]
                  (update-map result path #(permute-numeric-values % rng)))
                config
                shuffled-parameter-paths)
        (assoc :species :cyber-wiring-shuffled-parameter-control
               :wiring-id :shuffled-parameter-control
               :replay-control {:kind :shuffled-parameter
                                :seed seed
                                :invariant :within-block-numeric-permutation}))))

(defn generate-controls!
  "Generate both controls from the selected L5 config and return their paths."
  [l5-path output-dir seed]
  (let [base (selected-config (read-edn l5-path))
        config-dir (str output-dir "/config")
        random-path (str config-dir "/random-wiring.edn")
        shuffled-path (str config-dir "/shuffled-parameter.edn")]
    (write-edn! random-path {:cyberant (random-wiring-control base seed)})
    (write-edn! shuffled-path
                {:cyberant (shuffled-parameter-control base (inc seed))})
    {:random-wiring random-path
     :shuffled-parameter shuffled-path}))

(defn mean
  [xs]
  (if (seq xs)
    (/ (reduce + 0.0 xs) (count xs))
    0.0))

(defn sample-variance
  [xs]
  (if (> (count xs) 1)
    (let [m (mean xs)]
      (/ (reduce + 0.0 (map #(let [d (- (double %) m)] (* d d)) xs))
         (dec (count xs))))
    0.0))

(defn- t-critical-95
  [degrees-of-freedom]
  (cond
    (<= degrees-of-freedom 1) 12.706
    (<= degrees-of-freedom 2) 4.303
    (<= degrees-of-freedom 3) 3.182
    (<= degrees-of-freedom 4) 2.776
    (<= degrees-of-freedom 5) 2.571
    (<= degrees-of-freedom 6) 2.447
    (<= degrees-of-freedom 7) 2.365
    (<= degrees-of-freedom 8) 2.306
    (<= degrees-of-freedom 9) 2.262
    (<= degrees-of-freedom 10) 2.228
    (<= degrees-of-freedom 12) 2.179
    (<= degrees-of-freedom 15) 2.131
    (<= degrees-of-freedom 20) 2.086
    (<= degrees-of-freedom 25) 2.060
    (<= degrees-of-freedom 30) 2.042
    (<= degrees-of-freedom 40) 2.021
    (<= degrees-of-freedom 60) 2.000
    :else 1.960))

(defn arm-summary
  "Summarize scores with a two-sided 95% t interval and explicit zeros."
  [scores]
  (let [scores (mapv double scores)
        n (count scores)
        m (mean scores)
        variance (sample-variance scores)
        sd (Math/sqrt variance)
        se (if (pos? n) (/ sd (Math/sqrt n)) 0.0)
        margin (* (t-critical-95 (max 1 (dec n))) se)
        zero-count (count (filter zero? scores))]
    {:n n
     :mean m
     :sd sd
     :ci95 [(- m margin) (+ m margin)]
     :ci95-margin margin
     :starvation-count zero-count
     :starvation-fraction (if (pos? n) (/ zero-count (double n)) 0.0)}))

(defn difference-summary
  "Independent-sample mean difference and conservative 95% t interval."
  [left-scores right-scores]
  (let [left-scores (mapv double left-scores)
        right-scores (mapv double right-scores)
        n-left (count left-scores)
        n-right (count right-scores)
        delta (- (mean left-scores) (mean right-scores))
        se (Math/sqrt (+ (/ (sample-variance left-scores) (max 1 n-left))
                         (/ (sample-variance right-scores) (max 1 n-right))))
        df (max 1 (min (dec n-left) (dec n-right)))
        margin (* (t-critical-95 df) se)]
    {:delta delta
     :ci95 [(- delta margin) (+ delta margin)]
     :ci95-margin margin}))

(defn- row-scores
  [artifact species]
  (mapv #(double (get-in % [:scores species] 0.0)) (:rows artifact)))

(defn- run-harness!
  [scenario arm hex-path sigil-path runs ticks raw-dir]
  (let [out (str raw-dir "/" (name scenario) "-" (name arm) "-vs-sigil.edn")]
    (apply compare/-main
           ["--hex" hex-path
            "--sigil" sigil-path
            "--runs" (str runs)
            "--ticks" (str ticks)
            "--food" (name scenario)
            "--out" out])
    out))

(defn- git-output
  [& args]
  (let [{:keys [exit out err]} (apply shell/sh "git" args)]
    (when-not (zero? exit)
      (throw (ex-info "git command failed" {:args args :err err})))
    (str/trim out)))

(defn- parse-long-value
  [s]
  (Long/parseLong s))

(defn- parse-args
  [args]
  (loop [remaining args
         options {}]
    (if-let [flag (first remaining)]
      (let [value (second remaining)]
        (case flag
          "--runs" (recur (nnext remaining) (assoc options :runs (parse-long-value value)))
          "--ticks" (recur (nnext remaining) (assoc options :ticks (parse-long-value value)))
          "--l5" (recur (nnext remaining) (assoc options :l5-path value))
          "--sigil" (recur (nnext remaining) (assoc options :sigil-path value))
          "--out-dir" (recur (nnext remaining) (assoc options :output-dir value))
          "--control-seed" (recur (nnext remaining)
                                    (assoc options :control-seed (parse-long-value value)))
          (throw (ex-info "Unknown replay option" {:option flag}))))
      options)))

(defn- fmt
  [value]
  (format "%.3f" (double value)))

(defn- fmt-ci
  [{:keys [mean ci95]}]
  (format "%s [%s, %s]" (fmt mean) (fmt (first ci95)) (fmt (second ci95))))

(defn- comparison-supported?
  [direction {:keys [ci95]}]
  (case direction
    :greater (pos? (first ci95))
    :less (neg? (second ci95))))

(defn- claims-markdown
  [{:keys [generated-at harness-sha compare-blob config-blobs runs ticks control-seed
           results comparisons]}]
  (let [arm-order [:l5 :sigil-gradient :random-wiring :shuffled-parameter]
        boundary-direction {:patchy :greater :sparse :greater :snowdrift :less}
        lines (atom
               ["# CyberAnts controlled statistical replay"
                ""
                (str "Generated: `" generated-at "`")
                (str "Pinned futon2 harness SHA: `" harness-sha "`")
                (str "Pinned `src/ants/compare.clj` blob: `" compare-blob "`")
                (str "Pinned config blobs: `" (pr-str config-blobs) "`")
                (str "Protocol: " runs " unseeded runs/arm/scenario, " ticks
                     " ticks/run; deterministic control seed `" control-seed "`.")
                ""
                "Simulation rows are inherently stochastic. Intervals are two-sided 95% t intervals; starvation is the explicit share of score `0.0`."
                ""
                "## Arm summaries"
                ""
                "| Scenario | Arm | Mean [95% CI] | Starvation |"
                "|---|---|---:|---:|"])]
    (doseq [scenario scenarios
            arm arm-order]
      (let [summary (get-in results [scenario arm])]
        (swap! lines conj
               (format "| %s | %s | %s | %s (%d/%d) |"
                       (name scenario)
                       (name arm)
                       (fmt-ci summary)
                       (fmt (:starvation-fraction summary))
                       (:starvation-count summary)
                       (:n summary)))))
    (doseq [line [""
                  "## Claims table"
                  ""
                  "A claim is marked supported only when the independent-difference 95% CI excludes zero in the preregistered direction."
                  ""
                  "| Scenario | Comparison | Expected | Delta [95% CI] | Supported? |"
                  "|---|---|---|---:|---:|"]]
      (swap! lines conj line))
    (doseq [scenario scenarios]
      (let [boundary (get-in comparisons [scenario :l5-vs-sigil])
            direction (get boundary-direction scenario)]
        (swap! lines conj
               (format "| %s | L5 - sigil-gradient | %s | %s [%s, %s] | %s |"
                       (name scenario)
                       (if (= direction :greater) "L5 > sigil" "L5 < sigil")
                       (fmt (:delta boundary))
                       (fmt (first (:ci95 boundary)))
                       (fmt (second (:ci95 boundary)))
                       (if (comparison-supported? direction boundary) "yes" "no")))
      (doseq [control [:random-wiring :shuffled-parameter]]
        (let [comparison (get-in comparisons [scenario (keyword (str "l5-vs-" (name control)))])]
          (swap! lines conj
                 (format "| %s | L5 - %s | L5 > control | %s [%s, %s] | %s |"
                         (name scenario)
                         (name control)
                         (fmt (:delta comparison))
                         (fmt (first (:ci95 comparison)))
                         (fmt (second (:ci95 comparison)))
                         (if (comparison-supported? :greater comparison) "yes" "no")))))))
    (doseq [line [""
                  "## Interpretation constraint"
                  ""
                  "The current futon2 external-config adapter applies numeric precision to live AIF state, while retaining policy, pattern-sense, and adaptation wiring mainly as provenance. The random-wiring control is therefore a valid structural permutation but may be operationally equivalent to L5 in this harness. The shuffled-parameter control includes a precision permutation and can be operationally distinct. Any null control result is evidence about this executed boundary, not evidence that arbitrary wiring is generally equivalent."]]
      (swap! lines conj line))
    (str (str/join "\n" @lines) "\n")))

(defn run-replay!
  [{:keys [runs ticks l5-path sigil-path output-dir control-seed]
    :or {runs 30
         ticks 300
         l5-path default-l5-path
         sigil-path default-sigil-path
         output-dir default-output-dir
         control-seed default-control-seed}}]
  (when (< runs 30)
    (throw (ex-info "Replay requires at least 30 runs per arm/scenario" {:runs runs})))
  (let [harness-sha (git-output "rev-parse" "HEAD")
        compare-hash (git-output "hash-object" "src/ants/compare.clj")
        controls (generate-controls! l5-path output-dir control-seed)
        arms {:l5 l5-path
              :random-wiring (:random-wiring controls)
              :shuffled-parameter (:shuffled-parameter controls)}
        config-paths (assoc arms :sigil-gradient sigil-path)
        config-blobs (into {}
                           (map (fn [[arm path]]
                                  [arm (git-output "hash-object" path)])
                                config-paths))
        raw-dir (str output-dir "/raw")
        raw-paths (into {}
                        (for [scenario scenarios]
                          [scenario
                           (into {}
                                 (for [[arm path] arms]
                                   [arm (run-harness! scenario arm path sigil-path
                                                      runs ticks raw-dir)]))]))
        score-data (into {}
                         (for [scenario scenarios]
                           (let [artifacts (into {}
                                                 (map (fn [[arm path]] [arm (read-edn path)])
                                                      (get raw-paths scenario)))]
                             [scenario
                              {:l5 (row-scores (:l5 artifacts) :cyber)
                               :sigil-gradient (row-scores (:l5 artifacts) :cyber-sigil)
                               :random-wiring (row-scores (:random-wiring artifacts) :cyber)
                               :shuffled-parameter
                               (row-scores (:shuffled-parameter artifacts) :cyber)}])))
        results (into {}
                      (map (fn [[scenario arms-scores]]
                             [scenario (into {}
                                             (map (fn [[arm scores]]
                                                    [arm (arm-summary scores)])
                                                  arms-scores))])
                           score-data))
        comparisons (into {}
                          (map (fn [[scenario arms-scores]]
                                 [scenario
                                  {:l5-vs-sigil
                                   (difference-summary (:l5 arms-scores)
                                                       (:sigil-gradient arms-scores))
                                   :l5-vs-random-wiring
                                   (difference-summary (:l5 arms-scores)
                                                       (:random-wiring arms-scores))
                                   :l5-vs-shuffled-parameter
                                   (difference-summary (:l5 arms-scores)
                                                       (:shuffled-parameter arms-scores))}])
                               score-data))
        generated-at (.toString (java.time.Instant/now))
        summary {:generated-at generated-at
                 :route :statistical-unseeded
                 :harness-sha harness-sha
                 :compare-blob compare-hash
                 :runs-per-arm-scenario runs
                 :ticks ticks
                 :control-seed control-seed
                 :inputs {:l5 l5-path :sigil-gradient sigil-path}
                 :controls controls
                 :config-blobs config-blobs
                 :raw-paths raw-paths
                 :results results
                 :comparisons comparisons}
        summary-path (str output-dir "/summary.edn")
        claims-path (str output-dir "/CLAIMS.md")]
    (write-edn! summary-path summary)
    (write-text! claims-path (claims-markdown (assoc summary :runs runs)))
    {:summary summary-path :claims claims-path :data summary}))

(defn -main
  [& args]
  (let [options (merge {:runs 30
                        :ticks 300
                        :l5-path default-l5-path
                        :sigil-path default-sigil-path
                        :output-dir default-output-dir
                        :control-seed default-control-seed}
                       (parse-args args))
        {:keys [summary claims]} (run-replay! options)]
    (println "Replay summary:" summary)
    (println "Claims table:" claims)))
