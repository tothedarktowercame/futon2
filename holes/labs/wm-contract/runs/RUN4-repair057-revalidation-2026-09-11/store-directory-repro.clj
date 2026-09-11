(require '[clojure.test :as t] '[clojure.java.io :as io]
         '[futon2.aif.repair-obligation :as r]
         '[futon2.aif.historical-repair-revalidation-test :as rt])
(let [external (.toFile (java.nio.file.Files/createTempDirectory
                         "outside-historical-store" (make-array java.nio.file.attribute.FileAttribute 0)))
      original r/commit-historical-verification!]
  (with-redefs [r/commit-historical-verification!
                (fn [root evidence]
                  (let [dir (.toPath (io/file root "verifications"))]
                    (when-not (java.nio.file.Files/exists dir (make-array java.nio.file.LinkOption 0))
                      (java.nio.file.Files/createSymbolicLink
                       dir (.toPath external) (make-array java.nio.file.attribute.FileAttribute 0))))
                  (original root evidence))]
    (t/test-vars [#'rt/selectable-construction-and-store-transition]))
  (prn {:admission-written-outside-store (.isFile (io/file external "repair-057.edn"))}))
