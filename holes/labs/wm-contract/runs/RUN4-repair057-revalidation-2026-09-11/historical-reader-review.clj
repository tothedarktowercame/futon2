(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[futon2.aif.c-fold-config :as digest]
         '[futon3c.wm.run4-terminal-evidence-test :as t]
         '[futon3c.wm.run4-historical-projection :as h])
(#'t/fixture
 (fn [{:keys [roots run-file binding-file binding]}]
   (let [record (assoc (edn/read-string (slurp run-file))
                       :run4/controller-attempt-id (:attempt-id t/request))
         _ (spit run-file (pr-str record))
         sha (digest/sha256 (slurp run-file))
         projection {:schema :wm/run4-historical-admission-projection-v1
                     :click/id "click-1" :run/id "run-1"
                     :controller-attempt/id (:attempt-id t/request)
                     :requested-pin {:identity {:pin-sha256 (:sha256 t/pin)}}
                     :repair {:status :awaiting-validation :resolved? false}
                     :source {:run-record (.getPath run-file) :run-record-sha256 sha}}
         file (io/file (:projections roots) "historical.edn")]
     (spit file (pr-str projection))
     (spit binding-file (pr-str (assoc (dissoc binding :run4/terminal-projection) :run4/historical-projection
                                    {:path (.getPath file)
                                     :sha256 (digest/sha256 (pr-str projection))
                                     :source-sha256 sha})))
     (prn (select-keys (h/read-bundle! roots t/request t/started)
                       [:schema :classification])))))
