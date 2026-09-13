(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-enactment-correspondence :as c]
         '[futon2.aif.machine-enactment-correspondence-test])
(let [ns-sym 'futon2.aif.machine-enactment-correspondence-test
      root ((ns-resolve ns-sym 'copy-witnesses))]
  (doseq [name ["selection.edn" "enactment.edn"]]
    (let [file (io/file root name)]
      (spit file (pr-str (dissoc (edn/read-string (slurp file)) :cohort/id)))))
  (let [result (c/verify-correspondence ((ns-resolve ns-sym 'config) root))]
    (assert (= :exact-occurrence-and-action (:correspondence result)))
    (assert (nil? (:cohort/id result)))
    (prn (select-keys result [:scope :cohort/id :correspondence]))))
