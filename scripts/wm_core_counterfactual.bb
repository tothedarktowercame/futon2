#!/usr/bin/env bb
;; wm_core_counterfactual.bb — M-evaluate-policies MAP Q2: tick-by-tick
;; core-only counterfactual. "Core" = canonical EFE G = G-risk + G-ambiguity
;; (the two terms with a claim to the literature's risk+ambiguity reading;
;; see docs/futon-aif-completeness.md §R5 and the R18 audit).
;;
;; For every tick in data/wm-trace/: who wins under the full blend
;; (persisted :controller-score argmin) vs under core-only? When they disagree:
;;  - does the action TYPE change, or only the target within a type?
;;  - how deep in the blend's ranking did the core winner sit (blend-rank)?
;;  - how deep in the core ranking did the blend winner sit (core-rank)?
;; NOTE the feasibility-mask half of §3.2 is NOT computable from traces:
;; :graph-control-score (which smuggles the 1000·not-applicable mask) is
;; stripped at persist time (trace.clj strip-ranked-action) — so this is
;; core WITHOUT mask, the honest lower bound on core-only behaviour.
;;
;; Usage: bb scripts/wm_core_counterfactual.bb [trace-dir] [out-edn]

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-file  (or (second *command-line-args*)
                   "holes/labs/M-evaluate-policies/q2-core-counterfactual.edn"))

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn core-score [e]
  (+ (double (or (:G-risk e) 0.0)) (double (or (:G-ambiguity e) 0.0))))

(defn rank-of
  "1-based position of index i when candidates are ordered by scorer asc."
  [scores i]
  (inc (count (filter #(< % (nth scores i)) scores))))

(defn tick-diff [tick]
  (let [ra (vec (:ranked-actions tick))]
    (when (seq ra)
      (let [totals (mapv #(double (or (:controller-score %) 0.0)) ra)
            cores  (mapv core-score ra)
            bw     (first (apply min-key second (map-indexed vector totals)))
            cw     (first (apply min-key second (map-indexed vector cores)))
            bact   (:action (nth ra bw))
            cact   (:action (nth ra cw))
            agree? (= bw cw)]
        {:timestamp (str (:timestamp tick))
         :agree? agree?
         :blend-winner {:type (:type bact) :target (:target bact)}
         :core-winner  {:type (:type cact) :target (:target cact)}
         :type-switch? (and (not agree?) (not= (:type bact) (:type cact)))
         ;; where did the core winner sit in the blend's eyes, and vice versa?
         :core-winner-blend-rank (rank-of totals cw)
         :blend-winner-core-rank (rank-of cores bw)}))))

(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))

(println (format "Reading %d trace files..." (count files)))

(def diffs
  (doall (mapcat (fn [f]
                   (let [ds (keep tick-diff (read-all-forms f))]
                     (println (format "  %s: %d ticks" (.getName f) (count ds)))
                     ds))
                 files)))

(def n (count diffs))
(def disagreements (remove :agree? diffs))
(def nd (count disagreements))

(defn pctf [x] (when (pos? n) (/ (Math/round (* 1000.0 x)) 10.0)))

(def summary
  {:generated-by "scripts/wm_core_counterfactual.bb (M-evaluate-policies Q2)"
   :core-definition "G-risk + G-ambiguity (canonical risk+ambiguity; NO feasibility mask — graph-pragmatic stripped from traces)"
   :ticks n
   :agreement-pct (pctf (/ (- n nd) (double n)))
   :disagreements nd
   :type-switch-pct-of-disagreements
   (when (pos? nd) (/ (Math/round (* 1000.0 (/ (count (filter :type-switch? disagreements)) (double nd)))) 10.0))
   :blend-winner-types (frequencies (map (comp :type :blend-winner) diffs))
   :core-winner-types  (frequencies (map (comp :type :core-winner) diffs))
   :type-flow (frequencies (map (juxt (comp :type :blend-winner) (comp :type :core-winner)) disagreements))
   :core-winner-blend-rank {:median (let [s (sort (map :core-winner-blend-rank disagreements))]
                                      (when (seq s) (nth s (quot (count s) 2))))
                            :max (when (seq disagreements) (apply max (map :core-winner-blend-rank disagreements)))}
   :blend-winner-core-rank {:median (let [s (sort (map :blend-winner-core-rank disagreements))]
                                      (when (seq s) (nth s (quot (count s) 2))))
                            :max (when (seq disagreements) (apply max (map :blend-winner-core-rank disagreements)))}
   :top-core-winner-targets (->> disagreements
                                 (map (comp :target :core-winner))
                                 frequencies (sort-by val >) (take 8) (into {}))
   :top-blend-winner-targets-lost (->> disagreements
                                       (map (comp :target :blend-winner))
                                       frequencies (sort-by val >) (take 8) (into {}))})

(io/make-parents out-file)
(spit out-file (with-out-str (pp/pprint {:summary summary :ticks diffs})))
(println "\n=== Q2 SUMMARY ===")
(pp/pprint summary)
(println "\nWrote" out-file)
