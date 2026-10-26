;; Extract the outer-selection-trace projection fixture from a real
;; retained selection receipt (19.6 MB, gitignored). Writes ONLY the
;; keys futon2.aif.outer-selection-trace/trace reads, values verbatim.
;;
;;   clojure -M test/resources/outer-selection-trace/extract.clj [selection.edn] [out.edn]
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

(defn- project
  [ots]
  {:support (mapv #(select-keys % [:id]) (:support ots))
   :excluded (mapv #(select-keys % [:id :ineligible-reason]) (:excluded ots))
   :policy-field
   {:families (mapv (fn [f]
                      (cond-> (select-keys f [:target-id :status])
                        (:policies f)
                        (assoc :policies (mapv #(select-keys % [:policy-id]) (:policies f)))
                        (:failures f)
                        (assoc :failures (mapv #(select-keys % [:kind]) (:failures f)))))
                    (get-in ots [:policy-field :families]))}
   :policy
   {:selected-outer-cascade-policy
    {:sealed-menu
     (when-let [m (get-in ots [:policy :selected-outer-cascade-policy :sealed-menu])]
       {:entries (mapv #(select-keys % [:policy-id :target-id]) (:entries m))
        :refusals (mapv #(select-keys % [:kind :policy-id]) (:refusals m))
        :failures (mapv #(select-keys % [:target-id :kind]) (:failures m))})}
    :canonical-g-frontier
    (when-let [fr (get-in ots [:policy :canonical-g-frontier])]
      {:entries (mapv #(select-keys % [:target-id :G :rank :status]) (:entries fr))
       :refused (:refused fr)
       :horizon-choice (select-keys (:horizon-choice fr) [:horizon])})
    :canonical-policy-selection
    (when-let [cps (get-in ots [:policy :canonical-policy-selection])]
      {:status (:status cps)
       :basis (:basis cps)
       :selected (select-keys (:selected cps) [:target-id])
       :action (select-keys (:action cps) [:target])
       :draw (:draw cps)})}})

(let [[in out] *command-line-args*
      in (or in "data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/002-selection.edn")
      out (or out "test/resources/outer-selection-trace/attempt-002-projection.edn")
      ots (get-in (edn/read-string (slurp (io/file in)))                  [:payload :judgment :outer-task-selection])]
  (spit (io/file out) (with-out-str (pp/pprint (project ots))))
  (println "wrote" out))
