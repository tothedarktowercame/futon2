(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[futon2.aif.meta-live-outer-selector :as live])
(def frozen
  (with-open [in (java.util.zip.GZIPInputStream.
                 (io/input-stream "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/selection-stop.edn.gz"))]
    (edn/read-string {:readers {'wm/keyword (fn [[n k]] (keyword n k))}} (slurp in))))
(def policy (get-in frozen [:stop :ex-data :judgement :outer-task-selection :policy]))
(def input {:outer-learning {:mode :live :belief {:status :produced}}
            :selected-carrier (:selected-outer-cascade-policy policy)
            :policy-evidence (:policy-evidence policy)
            :policy-objective (:policy-objective policy)
            :menu-pin (get-in policy [:sealed-menu :menu-pin])
            :provider-source-pin (:evidence-provider-source-pin policy)})
;; The authority projection follows the author's test. No live calls or writes.
(def cases
  {:frozen-positive input
   :missing-provider (dissoc input :provider-source-pin)
   :evidence-refused (assoc-in input [:policy-evidence :status] :refused)
   :corrupted-objective-seal (assoc-in input [:selected-carrier :objective-pin :sha256] (apply str (repeat 64 "0")))
   :minimal-unpinned {:outer-learning (:outer-learning input)
                     :selected-carrier {:status :sealed :objective-receipt {:status :selected}}
                     :policy-objective {:status :selected}}})
(doseq [[case input] cases]
  (prn {:case case :certificate (select-keys (live/ordinary-learning-certificate input)
                                            [:status :reason :provider-source-pin :menu-pin :evidence-pin :objective-pin :carrier-pin :selected-policy])}))
