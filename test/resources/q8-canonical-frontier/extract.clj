;; Extract the q8-canonical-frontier projection fixture from the real
;; retained selection receipt (19.6 MB, gitignored). Writes ONLY the paths
;; futon2.aif.q8-frontier/canonical-outer-frontier reads, values verbatim.
;;
;;   clojure -M test/resources/q8-canonical-frontier/extract.clj [selection.edn] [out.edn]
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
                        (:failures f)
                        (assoc :failures (mapv #(select-keys % [:kind]) (:failures f)))))
                    (get-in ots [:policy-field :families]))}
   :policy
   {:selected-outer-cascade-policy
    {:sealed-menu
     (when-let [m (get-in ots [:policy :selected-outer-cascade-policy :sealed-menu])]
       {:entries (mapv #(select-keys % [:policy-id :target-id]) (:entries m))
        :failures (mapv #(select-keys % [:target-id :kind]) (:failures m))
        :refusals (mapv #(select-keys % [:policy-id :kind]) (:refusals m))})}
    :canonical-g-frontier
    (when-let [fr (get-in ots [:policy :canonical-g-frontier])]
      {:status (:status fr)
       :entries (mapv #(select-keys % [:policy-id :target-id :status :G
                                       :cascade-digest :action-pin :reason])
                      (:entries fr))
       :horizon-choice (select-keys (:horizon-choice fr) [:horizon])})
    :canonical-policy-selection
    (when-let [cps (get-in ots [:policy :canonical-policy-selection])]
      {:status (:status cps)
       :action (select-keys (:action cps) [:target])})}})

(let [[in out] *command-line-args*
      in (or in "data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/002-selection.edn")
      out (or out "test/resources/q8-canonical-frontier/attempt-002-projection.edn")
      ots (get-in (edn/read-string (slurp (io/file in)))
                  [:payload :judgment :outer-task-selection])]
  (spit (io/file out) (with-out-str (pp/pprint (project ots))))
  (println "wrote" out))
