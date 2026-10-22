(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[futon2.aif.meta-live-outer-selector :as live])
(defn order-comparator [xs]
  (let [positions (zipmap xs (range))]
    (fn [a b] (if (= a b) 0
      (compare [(get positions a (count positions)) (pr-str a)]
               [(get positions b (count positions)) (pr-str b)])))))
(def readers {'wm/keyword (fn [[n k]] (keyword n k))
              'wm/map (fn [pairs] (into (sorted-map-by (order-comparator (mapv first pairs))) pairs))
              'wm/set (fn [xs] (into (sorted-set-by (order-comparator xs)) xs))})
(def frozen
  (with-open [in (java.util.zip.GZIPInputStream.
                 (io/input-stream "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/selection-ordered.edn.gz"))]
    (edn/read-string {:readers readers} (slurp in))))
(def policy (get-in frozen [:stop :ex-data :judgement :outer-task-selection :policy]))
(def input {:outer-learning {:mode :live :belief {:status :produced}}
            :carrier (:selected-outer-cascade-policy policy)
            :policy-evidence (:policy-evidence policy)
            :policy-objective (:policy-objective policy)
            :menu-pin (get-in policy [:sealed-menu :menu-pin])
            :provider-source-pin (:evidence-provider-source-pin policy)})
;; The authority projection follows the author's test. No live calls or writes.
(def cases
  {:frozen-positive input
   :missing-provider (dissoc input :provider-source-pin)
   :evidence-refused (assoc-in input [:policy-evidence :status] :refused)
   :corrupted-objective-seal (assoc-in input [:carrier :objective-pin :sha256] (apply str (repeat 64 "0")))
   :minimal-unpinned {:outer-learning (:outer-learning input)
                     :carrier {:status :sealed :objective-receipt {:status :selected}}
                     :policy-objective {:status :selected}}})
(doseq [flag [true false]]
  (binding [*print-namespace-maps* flag]
    (prn {:namespace-maps flag :pin (futon2.aif.selected-outer-cascade-policy/carrier-pin (:carrier input)) :defect (futon2.aif.selected-outer-cascade-policy/verify (:carrier input))})))
