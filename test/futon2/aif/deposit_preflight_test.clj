(ns futon2.aif.deposit-preflight-test
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
           [futon2.aif.deposit-preflight :as preflight])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target
  "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-002-artifact-binding-mismatch")

(def template "holes/labs/wm-contract/deposit-templates/T-addressed")

(defn- temp-copy []
  (let [root (.toFile (Files/createTempDirectory
                       "deposit-preflight-test-" (make-array FileAttribute 0)))]
    (doseq [file (.listFiles (io/file template))]
      (Files/copy (.toPath file) (.toPath (io/file root (.getName file)))
                  (make-array java.nio.file.CopyOption 0)))
    root))

(deftest worked-template-runs-through-real-admission
  (let [result (preflight/preflight-dir template target)]
    (is (:ok? result))
    (is (every? #{:admitted-as-record :admitted-as-companion}
                (vals (:verdicts result))))))

(deftest known-deposit-failures-refuse
  (doseq [[label mutate expected]
           [[:identical-pair
            #(let [before (Files/readAllBytes
                           (.toPath (io/file % "subject-before.edn")))
                   pair-file (io/file % "subject-repair.edn")
                   pair (edn/read-string (slurp pair-file))
                   sha "1266151c767ae8e51545f3c5f12683cb1830edbe835928cff49b6382733f572c"]
               (Files/write (.toPath (io/file % "subject-after.edn")) before
                            (make-array java.nio.file.OpenOption 0))
               (spit pair-file
                     (pr-str (assoc pair :after {:file "subject-after.edn"
                                                 :sha256 sha :bytes 262478}))))
            :revision-unchanged]
           [:stray #(spit (io/file % "stray.stderr") "")
            :evidence-not-single-edn]
           [:unknown-schema
            #(spit (io/file % "unknown.edn") "{:schema :wm/unknown}\n")
            :schema-mismatch]
           [:subject-mismatch
            #(spit (io/file % "subject-repair.edn")
                   (str/replace
                    (slurp (io/file % "subject-repair.edn")) target "wrong-target"))
            :subject-entity-mismatch]
           [:ambiguous-role
            #(Files/move (.toPath (io/file % "supporting-resolved.edn"))
                         (.toPath (io/file % "ambiguous.edn"))
                         (make-array java.nio.file.CopyOption 0))
            :revision-role-ambiguous]]]
    (testing (name label)
      (let [dir (temp-copy)]
        (mutate dir)
        (is (= expected (:refusal (preflight/preflight-dir dir target))))))))
