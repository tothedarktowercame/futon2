(require '[futon3c.wm.run4-series-controller :as controller] '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.test :as t]
         '[futon3c.wm.run4-historical-roundtrip-test :as h]
         '[futon3c.wm.run4-u88-roundtrip-test :as u]
         '[futon3c.wm.run4-historical-action :as action]
         '[futon3c.wm.run4-deployment-config :as deployment]
         '[futon2.aif.repair-obligation :as repair]
         '[futon2.aif.hermetic-repair-fixture :as hermetic])
(binding [t/*report-counters* (ref t/*initial-report-counters*)]
(hermetic/with-hermetic-stores
 (fn []
  (h/with-authority
   (fn [deps]
    (let [ports (action/runner-ports (:historical-action deps))
          root (get-in deps [:historical-action :repair-root])
          obligation (first (repair/open-obligations root))
          candidate ((:historical-verification-candidate-fn ports) obligation)
          admitted ((:historical-verification-execute-fn ports)
                    {:execution-identity {:kind :runner-execution :id "verification-prior"}
                     :obligation obligation :candidate candidate})
          original deployment/materialize controller-step controller/step! calls (atom 0)]
      (with-redefs [controller/step! (fn [& args]
                       (if (= 2 (swap! calls inc))
                         (do (prn {:interleaved-terminal (apply controller-step args)})
                             (apply controller-step args))
                         (apply controller-step args)))
                    deployment/materialize
                    (fn [text dependencies]
                      (let [template (edn/read-string text)
                            manifest (edn/read-string
                                      (slurp (io/file (:authority-root template)
                                                      (get-in template [:manifest :ref]))))]
                        (original text (merge dependencies deps
                          {:historical-successor
                           {:repair-id "repair-057"
                            :verification-id (:verification-id admitted)
                            :verification-attempt (:verification-attempt admitted)
                            :successor {:series-id (:series-id manifest)
                                        :trial-id (get-in manifest [:trials 0 :trial-id])
                                        :attempt-id (get-in manifest [:trials 0 :attempt-id])}}}))))]
        (t/test-vars [#'u/async-wrapper-persists-to-reader-roots-and-terminal-roundtrips])
        (prn {:open-repairs-after-controller-completion (mapv :repair/id (repair/open-obligations root))})
        (prn {:simulation :terminal-written-between-service-precheck-and-controller-lock})))))))

(prn @t/*report-counters*)

)
