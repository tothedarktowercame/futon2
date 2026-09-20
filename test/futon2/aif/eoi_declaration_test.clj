(ns futon2.aif.eoi-declaration-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-sources :as sources]))

(def target "M-expressions-of-interest")
(def resource "wm/cascade-sources/M-expressions-of-interest.edn")

(defn assemble [loaded]
  (problems/assemble {:targets [target]
                      :sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)}))

(deftest declaration-admits-with-unsatisfied-presence-wants
  (let [loaded (sources/load-declared)
        result (assemble loaded)
        observation (get-in loaded [:observations target])
        wants (set (get-in loaded [:wants target]))
        facts (get-in loaded [:universes target])]
    (is (= 3 (count wants)))
    (is (= wants (set (keys facts))))
    (is (every? false? (vals facts)))
    (is (empty? (:refused observation)))
    (is (every? #(= :C3 (:check %)) (vals (:results observation))))
    (is (every? #(re-matches #"[0-9a-f]{40}" (get-in % [:evidence :resolved-sha]))
                (vals (:results observation))))
    (is (empty? (:refusals result)))
    (is (= 1 (count (:problems result))))
    (is (= 3 (count (get-in result [:problems 0 :cascade-problem :precedences]))))
    (doseq [[_ receipt] (get-in loaded [:interpretations target :receipts])]
      (is (re-matches #"[0-9a-f]{40}" (get-in receipt [:source :sha])))
      (is (re-matches #"[0-9a-f]{64}" (get-in receipt [:source :sha256]))))
    (doseq [[bad expected] [[(assoc-in loaded [:wants target] []) :want-not-declared]
                            [(assoc-in loaded [:locators target] {}) :universe-not-admitted]]]
      (is (not= loaded bad))
      (is (= expected (get-in (assemble bad) [:refusals 0 :kind]))))
    (println "EOI-DECLARATION-OBSERVATIONS" (pr-str observation))))

(deftest reinterpretation-source-pin-is-enforced
  (let [declaration (edn/read-string (slurp (io/resource resource)))
        pattern :coordination/bounded-execution
        bad (assoc-in declaration [:interpretation-receipts pattern :source :sha256] "wrong")
        dir (.toFile (java.nio.file.Files/createTempDirectory
                      "eoi-declaration" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file dir "source.edn")]
    (try
      (is (not= declaration bad))
      (spit file (pr-str bad))
      (is (= :interpretation-source-hash-mismatch
             (try (sources/load-declared (str dir)) nil
                  (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))))
      (finally (io/delete-file file true) (io/delete-file dir true)))))
