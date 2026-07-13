(ns habit-prior-shadow
  "Deterministic, read-only B1 shadow over the chronological WM trace corpus.

   Compares the historical caller/controller selection with the joint dark
   repair: structural pressure removed from controller-score and a learned
   trace-frequency ln E(π) supplied at the existing policy seam. The learned
   state is causal: each tick is scored from earlier decisions only, then the
   observed decision is folded. Cascade placeholder rows are excluded."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [futon2.aif.habit-prior :as habit]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace]))

(def trace-dir
  (or (first *command-line-args*)
      (str (System/getProperty "user.home") "/code/futon2/data/wm-trace")))
(def output-path
  (or (second *command-line-args*)
      "holes/labs/M-aif-faithfulness/habit-prior-frequency-shadow.edn"))

(defn score-of [entry]
  (or (:controller-score entry) (:G-total entry)))

(defn selection-domain [record]
  (->> (:ranked-actions record)
       (filter #(and (map? (:action %))
                     (number? (score-of %))
                     (not= :placeholder (:score-provenance %))
                     (not (:held-for-arming? %))))
       (mapv #(assoc % :controller-score (double (score-of %))))))

(defn without-structural-controller-term [entry]
  (let [signed-contribution
        (or (get-in entry [:augmentation-terms :structural-pressure])
            ;; Historical compact traces retain the raw positive pressure but
            ;; not always the named signed layer. The production default weight
            ;; for this corpus is 0.35; record which fallback rows were used.
            (when (number? (:structural-pressure entry))
              (- (* 0.35 (double (:structural-pressure entry)))))
            0.0)]
    (-> entry
        (update :controller-score - (double signed-contribution))
        (assoc :habit-shadow/structural-source
               (if (contains? (:augmentation-terms entry) :structural-pressure)
                 :persisted-signed-contribution
                 :raw-pressure-default-weight)))))

(defn decision-key [decision]
  (let [action (:action decision)]
    (if (= :abstain action) :abstain (habit/policy-key action))))

(defn select [ranked record]
  (policy/select-action
   (vec (sort-by :controller-score ranked))
   {:selection-gain (double (or (get-in record [:decision :selection-gain])
                                (get-in record [:selection-gain :selection-gain])
                                1.0))
    :temperature-opts
    {:tau-mode (or (get-in record [:wm-version :tau-mode]) :spread)}}))

(defn mean [xs]
  (when (seq xs) (/ (reduce + 0.0 xs) (double (count xs)))))

(let [records (trace/read-all-traces :dir trace-dir)
      {:keys [state rows]}
      (reduce
       (fn [{:keys [state rows]} record]
         (let [domain (selection-domain record)
               row (when (seq domain)
                     (let [caller (select domain record)
                           repaired-domain (->> domain
                                                (mapv without-structural-controller-term)
                                                (habit/attach-log-priors state))
                           learned (select repaired-domain record)
                           biases (mapv :habit-prior-bias repaired-domain)]
                       {:caller (decision-key caller)
                        :learned (decision-key learned)
                        :flip? (not= (decision-key caller) (decision-key learned))
                        :caller-abstain? (= :abstain (decision-key caller))
                        :learned-abstain? (= :abstain (decision-key learned))
                        :mean-abs-log-prior (mean (mapv #(Math/abs (double %)) biases))
                        :log-prior-range (when (seq biases)
                                           (- (apply max biases) (apply min biases)))
                        :fallback-structural-rows
                        (count (filter #(= :raw-pressure-default-weight
                                           (:habit-shadow/structural-source %))
                                       repaired-domain))}))]
           {:state (habit/fold-record state record)
            :rows (cond-> rows row (conj row))}))
       {:state (habit/initial-state) :rows []}
       records)
      flips (filter :flip? rows)
      abstain-flips (filter #(not= (:caller-abstain? %) (:learned-abstain? %)) rows)
      artifact
      {:generated-by "scripts/habit_prior_shadow.clj"
       :read-only true
       :deterministic true
       :mode-comparison
       {:baseline {:structural-pressure-mode :controller-augmentation
                   :habit-prior-source :caller}
        :repair {:structural-pressure-mode :habit-prior
                 :habit-prior-source :learned-frequency}}
       :model {:family :symmetric-dirichlet-multinomial
               :alpha habit/default-alpha
               :recency-decay :none
               :policy-identity "[action-type, target-or-target-class]"
               :interpretation "frequency/habit, not policy quality"}
       :corpus {:trace-dir trace-dir
                :records (count records)
                :scorable-ticks (count rows)
                :decisions-folded (:samples state)
                :distinct-policies (count (:counts state))}
       :shadow {:winner-or-abstain-flips (count flips)
                :flip-rate (if (seq rows) (/ (count flips) (double (count rows))) 0.0)
                :abstain-flips (count abstain-flips)
                :mean-abs-log-prior (mean (keep :mean-abs-log-prior rows))
                :mean-log-prior-range (mean (keep :log-prior-range rows))
                :fallback-structural-rows (reduce + 0 (map :fallback-structural-rows rows))}
       :flip-memo-seed
       {:blast-radius "joint flip removes caller structural credit from controller-score and replaces it with unscaled learned ln E(pi) at policy selection"
        :known-limitations ["frequency encodes habit, not benefit"
                            "historical menus and action targets are sparse"
                            "rows lacking persisted signed structural contribution use the declared historical 0.35 weight"]
        :operator-decision "Joe: keep dark or jointly enable FUTON_WM_STRUCTURAL_PRESSURE_MODE=habit-prior and FUTON_WM_HABIT_PRIOR_SOURCE=learned-frequency"}}]
  (io/make-parents output-path)
  (spit output-path (with-out-str (pprint/pprint artifact)))
  (println (pr-str (:corpus artifact)))
  (println (pr-str (:shadow artifact)))
  (println "wrote" output-path))
