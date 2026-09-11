(require '[clojure.java.io :as io]
         '[futon2.aif.repair-obligation :as r]
         '[futon2.aif.full-loop-runner :as runner])
(let [root (.toFile (java.nio.file.Files/createTempDirectory
                     "historical-store-review" (make-array java.nio.file.attribute.FileAttribute 0)))
      obligation {:repair/id "repair-057" :repair/status :open
                  :repair/class :machine-failure :attempt-id "attempt-057"}
      finding (io/file root "findings/repair-057.edn")
      verification (io/file root "verifications/repair-057.edn")]
  (io/make-parents finding)
  (io/make-parents verification)
  (spit finding (pr-str obligation))
  (spit verification (pr-str {:repair/id "repair-057"}))
  (prn {:status-with-invalid-verification
        (:repair/status (first (r/open-obligations (.getPath root))))}))
