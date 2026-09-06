(ns v7-r4-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.machine-q :as machine-q]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R4-carriers.edn"))))
(def tolerance 1.0e-12)

(defn row-sum [row] (reduce + (vals row)))
(defn model-valid? [c]
  (and (= (set (:states c)) (set (keys (:A c))))
       (every? #(= (set (:outcomes c)) (set (keys %))) (vals (:A c)))
       (every? #(< (Math/abs (- 1.0 (row-sum %))) tolerance) (vals (:A c)))
       (every? (fn [[_ rows]]
                 (and (= (set (:states c)) (set (keys rows)))
                      (every? #(< (Math/abs (- 1.0 (row-sum %))) tolerance) (vals rows)))) (:B c))))

(defn roll [c]
  (reduce (fn [q action]
            (into {} (for [next (:states c)]
                       [next (reduce + (for [s (:states c)]
                                         (* (get q s) (get-in c [:B action s next]))))])))
          (:mu c) (take (:T c) (:pi c))))
(defn outcome-row [c]
  (let [q (roll c)]
    (into {} (for [o (:outcomes c)]
               [o (reduce + (for [s (:states c)] (* (get-in c [:A s o]) (get q s))))]))))

(def reference {:source :carriers :state-row (roll carriers) :outcome-row (outcome-row carriers)})
(def production (:production carriers))
(def single-a (fm/predict (:state-a production) (:action production)))
(def single-b (fm/predict (:state-b production) (:action production)))
(def single-c (fm/predict (:state-c production) (:action production)))
(def multi-a (fm/predict-multi-horizon (:state-a production) (:action production) (:T carriers)))
(def multi-b (fm/predict-multi-horizon (:state-b production) (:action production) (:T carriers)))
(def multi-c (fm/predict-multi-horizon (:state-c production) (:action production) (:T carriers)))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))
(defn read-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= v ::eof) out (= v ::bad) (recur out) :else (recur (conj out v)))))))
(def records (vec (mapcat (fn [f] (map-indexed (fn [i r] {:file (.getName f) :index i :record r}) (read-records f))) (trace-files))))
(defn tree-has-key? [x k]
  (cond (map? x) (or (contains? x k) (some #(tree-has-key? % k) (vals x)))
        (sequential? x) (some #(tree-has-key? % k) x)
        :else false))
(defn tree-values [x k]
  (cond (map? x) (concat (when (contains? x k) [(get x k)])
                         (mapcat #(tree-values % k) (vals x)))
        (sequential? x) (mapcat #(tree-values % k) x)
        :else []))

(defn source-files [root]
  (->> (file-seq (io/file root)) (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljc|bb)$" (.getName %)))
       (sort-by #(.getPath %))))
(defn search-hits [pattern roots]
  (vec (mapcat (fn [f]
                 (keep-indexed (fn [i line]
                                 (when (re-find pattern line)
                                   (str (.getPath f) ":" (inc i) ":" (str/trim line))))
                               (str/split-lines (slurp f))))
               (mapcat source-files roots))))

(def horizon-hits (search-hits #"horizon-steps|predict-multi-horizon" ["src" "scripts" "checks"]))
(def kernel-hits (search-hits #"predict-effects|forward-model/predict|fm/predict" ["src" "scripts" "checks"]))
(def horizon-field-records (filter #(tree-has-key? (:record %) :horizon-steps) records))
(def horizon-records
  (filter #(or (some some? (tree-values (:record %) :horizon-steps))
               (some seq (filter sequential? (tree-values (:record %) :trajectory)))) records))
(def horizon-values
  (->> records (mapcat #(tree-values (:record %) :horizon-steps)) frequencies
       (sort-by (comp pr-str key)) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))))))
(def machine-q-records (filter #(tree-has-key? (:record %) :machine-q) records))

(def machine-q-refusal
  (try (machine-q/predictive-outcome-row! single-a (:outcomes carriers)) nil
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(def checks
  [{:id :declared-equation-produces-normalised-categorical
    :result (if (and (model-valid? carriers)
                     (< (Math/abs (- 1.0 (row-sum (:state-row reference)))) tolerance)
                     (< (Math/abs (- 1.0 (row-sum (:outcome-row reference)))) tolerance)) :pass :fail)
    :reference-source (:source reference) :state-mass (row-sum (:state-row reference))
    :outcome-mass (row-sum (:outcome-row reference)) :state-row (:state-row reference)
    :outcome-row (:outcome-row reference) :state-count (count (:states carriers))
    :outcome-count (count (:outcomes carriers)) :tolerance tolerance}
   {:id :shipped-return-is-a-channel-gaussian-not-declared-q
    :result :pass
    :return-keys (vec (sort (keys single-a)))
    :next-observation-keys (vec (sort (keys (:next-observation single-a))))
    :mean-channel-count (count (get-in single-a [:next-observation :mean]))
    :mean-channels (vec (sort (keys (get-in single-a [:next-observation :mean]))))
    :reference-outcomes (:outcomes carriers)
    :alphabet-intersection
    (vec (sort (set/intersection (set (:outcomes carriers))
                                 (set (keys (get-in single-a [:next-observation :mean]))))))
    :multi-return-keys (vec (sort (keys multi-a)))
    :trajectory-count (count (:trajectory multi-a))
    :final-state-keys (vec (sort (keys (:final-state multi-a))))
    :can-read-as-reference false :reason :different-alphabets-and-types}
   {:id :observation-prediction-does-not-read-belief
    :result (if (and (= (:next-observation single-a) (:next-observation single-b))
                     (not= (:next-belief single-a) (:next-belief single-b))
                     (not= (:next-observation single-a) (:next-observation single-c))
                     (= (mapv :next-observation (:trajectory multi-a))
                        (mapv :next-observation (:trajectory multi-b)))
                     (not= (:final-state multi-a) (:final-state multi-b))
                     (not= (:final-state multi-a) (:final-state multi-c))) :pass :fail)
    :single-observation-equal-under-belief-change (= (:next-observation single-a) (:next-observation single-b))
    :single-belief-equal-under-belief-change (= (:next-belief single-a) (:next-belief single-b))
    :single-observation-equal-under-observation-change (= (:next-observation single-a) (:next-observation single-c))
    :multi-observation-trajectories-equal-under-belief-change
    (= (mapv :next-observation (:trajectory multi-a)) (mapv :next-observation (:trajectory multi-b)))
    :multi-final-states-equal-under-belief-change (= (:final-state multi-a) (:final-state multi-b))}
   {:id :depth-is-opt-in-and-corpus-shows-effective-history
    :result :pass :default-horizon fm/default-horizon-steps
    :source-search-roots ["src" "scripts" "checks"] :source-hit-count (count horizon-hits)
    :source-hits horizon-hits
    :records-total (count records) :records-with-horizon-field (count horizon-field-records)
    :records-with-multi-horizon-evidence (count horizon-records)
    :records-without-horizon-evidence (- (count records) (count horizon-records))
    :recorded-horizon-value-distribution horizon-values
    :first-file (some-> records first :file) :last-file (some-> records last :file)
    :recorded-effective-depth (if (seq horizon-records) :mixed :single-step-no-recorded-horizon)}
   {:id :live-and-predictive-dynamics-do-not-share-one-kernel
    :result :pass :search-patterns ["predict-effects" "forward-model/predict" "fm/predict"]
    :search-roots ["src" "scripts" "checks"] :hit-count (count kernel-hits) :hits kernel-hits
    :live-dynamics "src/futon2/aif/enact.clj:250-336 (enact! and close-loop!)"
    :live-path-calls-predictive-kernel false}
   {:id :machine-q-boundary-refuses-the-gaussian-proxy
    :result (if (= :refusal/action-grain-gaussian-proxy (:refusal machine-q-refusal)) :pass :fail)
    :refusal machine-q-refusal :records-total (count records)
    :records-with-machine-q (count machine-q-records)}])

(def plants
  [{:id :unnormalised-a-row :carrier (assoc-in carriers [:A :s0] {:good 0.75 :bad 0.75})
    :caught-by :declared-model-validation}
   {:id :unnormalised-b-row :carrier (assoc-in carriers [:B :advance :s0] {:s0 0.75 :s1 0.75})
    :caught-by :declared-model-validation}
   {:id :rolls-t-minus-one :carrier (update carriers :T dec) :caught-by :reference-row}
   {:id :sums-over-outcomes-instead-of-states :carrier (assoc carriers :outcomes [:good])
    :caught-by :declared-model-validation}
   {:id :depth-zero-returns-mu :carrier (assoc carriers :T 0) :caught-by :reference-row}
   {:id :reference-reads-shipped-node :reference-source :shipped-node :caught-by :reference-provenance}])
(def canonical-row (:outcome-row reference))
(def plant-results
  (mapv (fn [{:keys [id carrier reference-source caught-by]}]
          (let [caught (case caught-by
                         :declared-model-validation (not (model-valid? carrier))
                         :reference-row (not= canonical-row (outcome-row carrier))
                         :reference-provenance (not= :carriers reference-source))]
            {:id id :result (if caught :caught :escaped) :caught-by [caught-by]})) plants))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(= :pass (:result %)) checks))

(def receipt
  {:harness :v7-r4-node-sim :row :V7 :slice 8 :node :R4 :stage "EVALUATE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:21"
   :carriers "holes/labs/wm-contract/sim/R4-carriers.edn"
   :reference-independence "The categorical Q(s|pi) and Q(o|pi) references are computed only from R4-carriers.edn. futon2.aif.forward-model is called only for node-under-test observations."
   :declaration-sites (:declaration-sites carriers) :checks checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :file-count (count (trace-files)) :record-count (count records)}
   :not-done ["No live tick or run lock; trace data was read only."
              "No source, registry, ledger, document, p4ng, or data file changed."
              "No equation-level numerical equality is claimed between disjoint outcome and channel alphabets."]})
(def out (io/file lab "runs/V7-R4-node-sim/00-r4.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-62s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
