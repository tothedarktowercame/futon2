(ns c464-beta-carry-experiment
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.policy-free-energy :as pfe]
            [futon2.aif.policy-precision :as precision]))

(def fields
  ["data/wm-trace/wm-trace-2026-07-04.edn"
   "data/wm-trace/wm-trace-2026-07-05.edn"])

(def beta-zeroes [0.5 1.0 2.0 5.0])

(defn read-forms [path]
  (with-open [reader (java.io.PushbackReader. (io/reader path))]
    (loop [forms []]
      (let [form (edn/read {:eof ::eof} reader)]
        (if (= ::eof form) forms (recur (conj forms form)))))))

(defn prediction-input [prediction]
  {:prediction-mean (get-in prediction [:next-observation :mean])
   :prediction-variance (get-in prediction [:next-observation :variance])
   :variance-status (get-in prediction [:next-observation :variance-status])})

(defn rank-order [values direction]
  (let [signed (if (= direction :ascending) identity -)]
    (vec (sort-by (fn [idx] [(signed (double (nth values idx))) idx])
                  (range (count values))))))

(defn rank-map [ordering]
  (into {} (map-indexed (fn [rank idx] [idx (inc rank)]) ordering)))

(defn compare-order [control-order experimental-order]
  (let [control-ranks (rank-map control-order)
        experimental-ranks (rank-map experimental-order)
        changes (mapv (fn [idx]
                        (Math/abs (long (- (get control-ranks idx)
                                           (get experimental-ranks idx)))))
                      control-order)]
    {:changed (count (filter pos? changes))
     :largest (apply max changes)
     :argmax-changed? (not= (first control-order)
                            (first experimental-order))}))

(defn transition-input [[tick next-tick]]
  (let [entries (vec (:ranked-actions tick))
        state {:observation (:observation tick) :belief (:mu-post tick)}
        predictions (mapv #(prediction-input (fm/predict state (:action %)))
                          entries)]
    {:g (mapv #(double (:G-total %)) entries)
     :f-pi (pfe/f-pi-vector predictions (:observation next-tick)
                            {:absent-variance :floor})}))

(defn solve-field [path beta-zero arm]
  (let [inputs (mapv transition-input (partition 2 1 (read-forms path)))]
    (loop [remaining inputs beta-prior beta-zero rows []]
      (if-let [{:keys [g f-pi]} (first remaining)]
        (let [solution (precision/converge-beta beta-prior g f-pi)
              control-order (rank-order g :ascending)
              posterior-order (rank-order (:pi solution) :descending)
              comparison (compare-order control-order posterior-order)
              row (merge (select-keys solution [:beta-prior :beta-posterior
                                                 :gamma :iterations :converged?
                                                 :bracketed?])
                         {:posterior-order posterior-order}
                         comparison)]
          (recur (next remaining)
                 (if (= arm :carried-prior)
                   (:beta-posterior solution)
                   beta-zero)
                 (conj rows row)))
        rows))))

(defn summarize [path beta-zero arm rows]
  {:field path
   :records (count (read-forms path))
   :transitions (count rows)
   :arm arm
   :beta-0 beta-zero
   :beta-prior (mapv :beta-prior rows)
   :beta-posterior (mapv :beta-posterior rows)
   :gamma (mapv :gamma rows)
   :iterations (mapv :iterations rows)
   :bracketed (mapv :bracketed? rows)
   :converged (mapv :converged? rows)
   :rank-changed-counts (mapv :changed rows)
   :largest-rank-changes (mapv :largest rows)
   :argmax-change-ticks (vec (keep-indexed #(when (:argmax-changed? %2) %1) rows))
   :beta-min (apply min (map :beta-posterior rows))
   :beta-max (apply max (map :beta-posterior rows))
   :beta-final (:beta-posterior (last rows))})

(defn arm-difference [carried fixed]
  (vec (keep-indexed
        (fn [idx [c f]]
          (when (not= (:posterior-order c) (:posterior-order f))
            idx))
        (map vector carried fixed))))

(defn -main [& _]
  (doseq [path fields beta-zero beta-zeroes]
    (let [carried (solve-field path beta-zero :carried-prior)
          fixed (solve-field path beta-zero :fixed-prior)]
      (prn (assoc (summarize path beta-zero :carried-prior carried)
                  :different-from-fixed-ticks (arm-difference carried fixed)))
      (prn (summarize path beta-zero :fixed-prior fixed)))))

(apply -main *command-line-args*)
