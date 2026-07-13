#!/usr/bin/env bb
;; wm_trace_census.bb — M-evaluate-policies MAP Q1: term-liveness census +
;; per-term decision-influence (flip-rate) over the wm-trace corpus.
;;
;; A wm-trace-YYYY-MM-DD.edn file holds one EDN form PER TICK (appended).
;; Schema >=3 records the signed `:augmentation-terms` exactly as they enter
;; the controller score. Older traces are replayed with the historical default
;; weights. This matters after typed-residual remediation: predictability and
;; homeostasis remain raw telemetry but contribute zero in :telemetry-only mode.
;;
;;   controller-score = G-risk + G-ambiguity - 0.4*predictability-bonus + 1.2*homeostatic-pressure
;;             - 0.35*structural-pressure + graph-control-score - gap-exploration-bonus
;;             + G-goal-outcome
;;
;; Flip analysis: for each persisted term, remove its weighted contribution
;; from every candidate's controller-score and ask whether the argmin (the winner)
;; changes. `:hidden` = removing the residual (graph-pragmatic - gap).
;; `:core-only` = selecting on G-risk + G-ambiguity alone (Q2 preview).
;;
;; Usage: bb scripts/wm_trace_census.bb [trace-dir] [out-edn]
;; Defaults: data/wm-trace  holes/labs/M-evaluate-policies/q1-census.edn

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-file  (or (second *command-line-args*)
                   "holes/labs/M-evaluate-policies/q1-census.edn"))

(def w-info 0.4)
(def w-survival 1.2)
(def w-sp 0.35)

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(def analysis-terms
  [:G-risk :G-ambiguity :risk-control :predictability-bonus
   :homeostatic-pressure :structural-pressure :graph-control
   :model-uncertainty-bonus :gap :G-goal-outcome :move-class-intensity])

(defn contributions
  "Signed contribution of each persisted term to controller-score. Prefer the
   score's own augmentation ledger; reconstruct only pre-ledger records."
  [e]
  (if-let [a (:augmentation-terms e)]
    {:G-risk (double (or (:G-risk e) 0.0))
     :G-ambiguity (double (or (:G-ambiguity e) 0.0))
     :risk-control (double (or (:risk-control a) 0.0))
     :predictability-bonus (double (or (:info a) 0.0))
     :homeostatic-pressure (double (or (:survival a) 0.0))
     :structural-pressure (double (or (:structural-pressure a) 0.0))
     :graph-control (double (or (:graph-control a) 0.0))
     :model-uncertainty-bonus (double (or (:model-uncertainty-bonus a) 0.0))
     :gap (double (or (:gap a) 0.0))
     :G-goal-outcome (double (or (:goal-outcome a) 0.0))
     :move-class-intensity (double (or (:move-class-intensity a) 0.0))}
    {:G-risk (double (or (:G-risk e) 0.0))
     :G-ambiguity (double (or (:G-ambiguity e) 0.0))
     :risk-control 0.0
     :predictability-bonus (- (* w-info (double (or (:predictability-bonus e) 0.0))))
     :homeostatic-pressure (* w-survival (double (or (:homeostatic-pressure e) 0.0)))
     :structural-pressure (- (* w-sp (double (or (:structural-pressure e) 0.0))))
     :graph-control (double (or (:graph-control-score e) 0.0))
     :model-uncertainty-bonus 0.0
     :gap (- (double (or (:gap-exploration-bonus e) 0.0)))
     :G-goal-outcome (double (or (:G-goal-outcome e) 0.0))
     :move-class-intensity (double (or (:move-class-intensity-contribution e) 0.0))}))

(defn winner-idx
  "Index of the minimum score (first on ties)."
  [scores]
  (first (apply min-key second (map-indexed vector scores))))

(defn tick-stats [tick]
  ;; Lane guard (found during Q2): :apply-cascade candidates are persisted
  ;; with ONLY {:controller-score 0.0 :rank :action} — a placeholder from the cascade
  ;; lane, not a scored decomposition. Including them poisons flip analysis
  ;; (any term-removal that raises ordinary controller-scores past 0.0 "flips" to a
  ;; scoreless row). They are censused separately and EXCLUDED from flips.
  (let [ra-all (vec (:ranked-actions tick))
        ra (vec (filter #(some? (:G-risk %)) ra-all))
        n-laneless (- (count ra-all) (count ra))]
    (when (seq ra)
      (let [contribs   (mapv contributions ra)
            totals     (mapv #(double (or (:controller-score %) 0.0)) ra)
            recons     (mapv #(reduce + (vals %)) contribs)
            residuals  (mapv - totals recons)
            actual-w   (winner-idx totals)
            flip?      (fn [term]
                         (let [adjusted (mapv - totals (map term contribs))]
                           (not= actual-w (winner-idx adjusted))))
            hidden-flip? (not= actual-w (winner-idx recons))
            core       (mapv #(+ (double (or (:G-risk %) 0.0))
                                 (double (or (:G-ambiguity %) 0.0))) ra)
            core-w     (winner-idx core)]
        {:timestamp    (str (:timestamp tick))
         :n-candidates (count ra)
         :n-laneless   n-laneless
         :present-keys (set (mapcat keys ra))
         :residual-mean-abs (/ (reduce + (map #(Math/abs %) residuals))
                               (count residuals))
         :residual-max-abs  (apply max (map #(Math/abs %) residuals))
         :flips        (into {} (for [t analysis-terms]
                                  [t (flip? t)]))
         :hidden-flip? hidden-flip?
         :core-agrees? (= actual-w core-w)
         :winner-action (get-in ra [actual-w :action :type])
         :winner-target (get-in ra [actual-w :action :target])}))))

(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))

(println (format "Reading %d trace files from %s ..." (count files) trace-dir))

(def per-day
  (doall
   (for [f files]
     (let [ticks (keep tick-stats (read-all-forms f))]
       (println (format "  %s: %d ticks" (.getName f) (count ticks)))
       {:file (.getName f) :ticks ticks}))))

(def all-ticks (mapcat :ticks per-day))
(def n (count all-ticks))

(defn pct [k] (when (pos? n) (/ (Math/round (* 1000.0 (/ (count (filter k all-ticks)) (double n)))) 10.0)))

(def summary
  {:generated-by "scripts/wm_trace_census.bb (M-evaluate-policies Q1)"
   :corpus {:files (count files)
            :ticks n
            :candidates-mean (when (pos? n)
                               (/ (Math/round (* 10.0 (/ (reduce + (map :n-candidates all-ticks)) (double n)))) 10.0))
            :first (:timestamp (first all-ticks))
            :last  (:timestamp (last all-ticks))}
   :term-liveness
   (into (sorted-map)
         (for [t [:G-risk :G-ambiguity :predictability-bonus :homeostatic-pressure
                  :structural-pressure :G-goal-outcome :gap-exploration-bonus
                  :graph-control-score :augmentation-terms]]
           [t {:persisted-pct (pct #(contains? (:present-keys %) t))}]))
   :flip-rate-pct
   (into (sorted-map)
         (concat
          (for [t analysis-terms]
            [t (pct #(get-in % [:flips t]))])
          [[:hidden-residual (pct :hidden-flip?)]]))
   :core-only-agreement-pct (pct :core-agrees?)
   :cascade-lane {:ticks-with-laneless-candidates (count (filter #(pos? (:n-laneless %)) all-ticks))
                  :note "apply-cascade rows: {:controller-score 0.0, no decomposition}; excluded from flip analysis"}
   :residual {:mean-abs (when (pos? n)
                          (/ (reduce + (map :residual-mean-abs all-ticks)) n))
              :max-abs (when (pos? n)
                         (apply max (map :residual-max-abs all-ticks)))
              :note "schema >=3 should replay from :G-core + :augmentation-terms with zero residual; older records use the historical reconstruction"}
   :winner-actions (frequencies (map :winner-action all-ticks))})

(io/make-parents out-file)
(spit out-file (with-out-str (pp/pprint {:summary summary :per-day
                                         (for [{:keys [file ticks]} per-day]
                                           {:file file
                                            :ticks (count ticks)
                                            :flip-counts (into (sorted-map)
                                                               (for [t (keys (:flips (first ticks)))]
                                                                 [t (count (filter #(get-in % [:flips t]) ticks))]))
                                            :hidden-flips (count (filter :hidden-flip? ticks))
                                            :core-agrees (count (filter :core-agrees? ticks))})})))
(println "\n=== Q1 SUMMARY ===")
(pp/pprint summary)
(println "\nWrote" out-file)
