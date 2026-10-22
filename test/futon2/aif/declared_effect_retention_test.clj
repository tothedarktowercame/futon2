(ns futon2.aif.declared-effect-retention-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.declared-effect-authority :as authority]))

(defn- refusal [f]
  (try (f) :unexpected-success
       (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))

(deftest repeated-retention-is-idempotent-and-conflicts-preserve-bytes
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "retention-check-" (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file dir "source.edn")
        content "{:target \"fixture\"}\n"
        record {:declaration {:status :pinned :path (str source)
                              :sha256 (identity/sha256 content)}}
        destination (str (io/file dir "retained"))]
    (try
      (spit source content)
      (let [pin (authority/retain-declaration! record destination)]
        (is (= pin (authority/retain-declaration! record destination)))
        (is (= content (slurp (:path pin))))
        (spit (:path pin) "conflicting bytes")
        (is (= :retained-declaration-conflict
               (refusal #(authority/retain-declaration! record destination))))
        (is (= "conflicting bytes" (slurp (:path pin)))))
      (doseq [r [{} {:declaration {:status :absent}}
                   {:declaration {:status :pinned :path nil :sha256 "bad"}}]]
        (is (= :declaration-not-pinned
               (refusal #(authority/retain-declaration! r destination)))))
      (finally (doseq [file (reverse (file-seq dir))] (.delete file))))))
