(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])
(defn rd [f] (try (edn/read-string {:default (fn [_ v] v)} (slurp f)) (catch Exception _ nil)))
(def sels (filter #(= "002-selection.edn" (.getName %)) (file-seq (io/file "data"))))
(def rows
  (for [s sels
        :let [dir (.getParentFile s) sel (rd s)
              closed (rd (io/file dir "007-closed.edn"))
              a (or (get-in sel [:payload :judgment :selected-action])
                    (get-in sel [:payload :judgment :controller-decision :action]))
              t (or (:type a) (:kind a))
              d (or (get-in sel [:recorded-at]) (:recorded-at closed))]]
    {:date (some-> d str (subs 0 10))
     :class (cond (nil? a) :none
                  (or (= t :repair-machine-failure) (:repair-obligation a) (:repair/id a)) :self-repair
                  (= t :cascade-candidate) :mission-work
                  :else (keyword (str "other-" (name (or t :untyped)))))
     :outcome (get-in closed [:payload :judgment :outcome] :not-closed)}))
(prn :attempts (count rows))
(prn :by-class (frequencies (map :class rows)))
(doseq [[d xs] (sort (group-by :date rows))] (prn d (frequencies (map :class xs))))
(prn :class-x-outcome (sort-by str (frequencies (map (juxt :class :outcome) rows))))
