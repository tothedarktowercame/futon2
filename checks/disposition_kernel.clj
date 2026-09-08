(ns checks.disposition-kernel
  "Fit the empirical terminal-disposition kernel recorded by a cohort ledger.

  Run from the repository root with:
    clojure -M -m checks.disposition-kernel [ledger.edn]"
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [futon2.aif.full-loop-cohort :as cohort])
  (:import [java.security MessageDigest]))

(def default-ledger
  "holes/labs/M-aif-full-loop-46/ledger.edn")

(defn sha256 [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (format "%064x"
            (BigInteger. 1 (.digest digest (java.nio.file.Files/readAllBytes
                                            (.toPath (java.io.File. path))))))))

(defn observation-summary
  "The ledger does not retain a channel-valued observation vector. Its only
  common pre-outcome trajectory summary is the ordered checkpoint sequence.
  Conditioning more finely on selected missions or checkpoint payloads would
  condition on actions, code snapshots, and unique attempt details rather than
  on a repeatedly observed state."
  [attempt]
  {:checkpoint-trajectory (:checkpoints attempt)})

(defn- support-vector []
  (vec (sort cohort/outcome-kinds)))

(defn- complete-counts [attempts]
  (let [observed (frequencies (map :outcome attempts))]
    (into (sorted-map)
          (map (fn [kind] [kind (get observed kind 0)]))
          (support-vector))))

(defn fit-kernel
  "Return exact empirical P(disposition | observation-summary).

  Unsupported outcome kinds remain present with count and probability zero;
  no pseudocounts or smoothing are applied."
  [ledger ledger-path ledger-sha]
  (let [closed (filterv #(and (:closed? %) (= :closed (last (:checkpoints %))))
                        (:attempts ledger))
        invalid (seq (remove cohort/outcome-kinds (map :outcome closed)))
        groups (group-by observation-summary closed)]
    (when invalid
      (throw (ex-info "closed attempts contain outcomes outside cohort/outcome-kinds"
                      {:outcomes (vec (sort (set invalid)))})))
    (let [states
          (mapv (fn [[summary attempts]]
                  (let [n (count attempts)
                        counts (complete-counts attempts)]
                    {:observation-summary summary
                     :sample-size n
                     :counts counts
                     :probability
                     (into (sorted-map)
                           (map (fn [[kind count]]
                                  [kind (if (zero? n) 0 (/ count n))]))
                           counts)}))
                (sort-by (comp pr-str key) groups))
          totals (complete-counts closed)
          supported (vec (for [[kind n] totals :when (pos? n)] kind))
          unsupported (vec (for [[kind n] totals :when (zero? n)] kind))]
      {:schema :wm/disposition-kernel-v1
       :source {:ledger ledger-path :sha256 ledger-sha}
       :conditioning
       {:grain :checkpoint-trajectory
        :reason "The cohort ledger retains an ordered checkpoint trajectory but no channel-valued observation vector; action and payload fields are not substituted for observations."}
       :support (support-vector)
       :sample-size (count closed)
       :supported-outcomes supported
       :unsupported-outcomes unsupported
       :outcome-counts totals
       :states states})))

(defn read-kernel [path]
  (fit-kernel (edn/read-string (slurp path)) path (sha256 path)))

(defn -main [& [path]]
  (pp/pprint (read-kernel (or path default-ledger))))
