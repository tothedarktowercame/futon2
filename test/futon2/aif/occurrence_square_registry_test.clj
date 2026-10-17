(ns futon2.aif.occurrence-square-registry-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.occurrence-square-registry :as registry])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def square-base
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))
(def square-parts
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence-parts.edn")))
(def square
  (reduce-kv (fn [receipt role text]
               (assoc-in receipt [:objects role :text] text))
             square-base (:texts square-parts)))
(def pattern :ukrns/publication-cadence)
(def oid [pattern 3])
(def pattern-text
  (slurp "/home/joe/code/futon3/library/ukrns/publication-cadence.flexiarg"))

(defn- root []
  (.toFile (Files/createTempDirectory "occurrence-squares-"
                                      (make-array FileAttribute 0))))

(defn- record []
  {:target-id "M-live"
   :excerpt-digest (apply str (repeat 64 "e"))
   :analysis-digest (apply str (repeat 64 "a"))
   :entries {oid {:occurrence-id oid :pattern pattern
                  :pattern-sha256 (get-in square [:objects :G :source :sha256])
                  :square square}}})

(deftest publish-load-and-project-occurrence-square
  (let [dir (root)
        published (registry/publish! dir (record) {pattern pattern-text})
        loaded (registry/current dir "M-live" (:analysis-digest (record))
                                 {pattern pattern-text})]
    (is (= registry/schema (:schema published)))
    (is (= :valid (:status (registry/validate-record loaded
                                                     {pattern pattern-text}))))
    (is (= square (get (registry/squares loaded) oid)))
    (is (= 1 (count (filter #(.isFile %) (file-seq dir)))))))

(deftest wrong-pin-and-pattern-grain-key-are-refused
  (let [dir (root)
        wrong-pin (assoc-in (record) [:entries oid :pattern-sha256]
                            (apply str (repeat 64 "0")))
        pattern-grain (assoc (record) :entries
                             {pattern (get-in (record) [:entries oid])})]
    (is (= :occurrence-squares-invalid
           (:kind (registry/publish! dir wrong-pin {pattern pattern-text}))))
    (is (= :occurrence-squares-invalid
           (:kind (registry/publish! dir pattern-grain {pattern pattern-text}))))))

(deftest missing-record-is-typed-before-g
  (is (= :occurrence-squares-not-authored
         (:kind (registry/current (root) "M-live"
                                  (:analysis-digest (record))
                                  {pattern pattern-text}))))
  (is (= pattern-text ((registry/library-resolver) pattern))))
