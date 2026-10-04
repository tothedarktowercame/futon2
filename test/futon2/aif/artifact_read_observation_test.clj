(ns futon2.aif.artifact-read-observation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.artifact-read-observation :as reads]))

(defn- temp-root []
  (.getPath (.toFile (java.nio.file.Files/createTempDirectory
                      "artifact-read-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))))

(deftest generation-is-not-a-read-and-real-consumption-is-counted
  (let [root (temp-root) artifact (str root "/product.edn")]
    (spit artifact "{:answer 42}")
    (is (empty? (reads/usage-projection root)))
    (let [sha (reads/sha256 (.getBytes (slurp artifact) "UTF-8"))
          authority {:consumer "field-desk" :path artifact :content-sha256 sha
                     :producer-run "run-1" :producer-click "click-1"
                     :producer-commit "abc" :purpose "render result" :parse :edn}
          result (reads/read-artifact! root authority)]
      (is (= {:answer 42} (:value result)))
      (is (= [{:product-sha256 sha :consumer "field-desk" :observed-reads 1}]
             (reads/usage-projection root)))
      (spit artifact "{:answer 43}")
      (is (= :artifact-sha-drift
             (:reason (ex-data (try (reads/read-artifact! root authority)
                                    (catch clojure.lang.ExceptionInfo e e)))))))))

(deftest producer-and-consumer-identities-are-required
  (let [root (temp-root) artifact (str root "/product.edn")]
    (spit artifact "{}")
    (is (= :artifact-read-identity-missing
           (:reason (ex-data
                     (try (reads/read-artifact! root
                                                {:path artifact :content-sha256 "x"})
                          (catch clojure.lang.ExceptionInfo e e))))))))
