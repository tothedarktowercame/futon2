(ns futon2.aif.pattern-retraction-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.pattern-retraction :as sut]))

(def python-tool "/home/joe/code/futon3c/scripts/pattern_retraction.py")

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "pattern-retraction"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn edge [a b] {:a a :b b :kind "why" :evidence []})

(def fixtures
  {:connected {:pattern_ids ["a" "b" "c"] :patterns 3 :records 1
               :edges [(edge "a" "b") (edge "b" "c")]}
   :disconnected {:pattern_ids ["a" "b" "c" "d"] :patterns 4 :records 1
                  :edges [(edge "a" "b") (edge "c" "d")]}
   :isolated {:pattern_ids ["a" "b" "z"] :patterns 3 :records 1
              :edges [(edge "a" "b")]}
   :alternatives {:pattern_ids ["a" "b" "c" "d"] :patterns 4 :records 1
                  :edges [(edge "a" "b") (edge "b" "d")
                          (edge "a" "c") (edge "c" "d")]}})

(defn fixture-file [kind]
  (let [file (io/file (temp-dir) (str (name kind) ".json"))]
    (spit file (json/generate-string (fixtures kind)))
    file))

(defn loaded-graph [file]
  (let [pin (graph-pin/pin! file)
        loaded (graph-pin/load-pinned file)]
    (is (= :wm/pattern-graph-pin-v1 (:schema pin)))
    (is (= :loaded (:status loaded)))
    (:graph loaded)))

(defn python-result [file seeds]
  (let [result (shell/sh "python3" python-tool "--graph" (.getPath file)
                         "--seeds" (str/join "," seeds) "--k" "3")]
    (assoc result :value (when (zero? (:exit result))
                           (json/parse-string (:out result) true)))))

(defn shape [retraction]
  {:nodes (set (:nodes retraction))
   :edges (set (map #(set [(:a %) (:b %)]) (:edges retraction)))})

(deftest parity-with-python-fixtures
  (doseq [[kind seeds] [[:connected ["a" "c"]]
                        [:disconnected ["a" "c"]]
                        [:isolated ["z"]]
                        [:alternatives ["a" "d"]]]]
    (testing (name kind)
      (let [file (fixture-file kind)
            clj (sut/retractions (loaded-graph file) {:seeds seeds :k 3})
            py (python-result file seeds)]
        (case kind
          :isolated
          (do (is (= 2 (:exit py)))
              (is (re-find #"isolated seed" (:err py)))
              (is (= :isolated-seed (get-in clj [:failures 0 :kind]))))

          :disconnected
          (do (is (zero? (:exit py)))
              (is (false? (get-in py [:value :component :seeds_connected])))
              (is (empty? (get-in py [:value :retractions])))
              (is (= :disconnected-seeds (get-in clj [:failures 0 :kind]))))

          (do (is (zero? (:exit py)) (:err py))
              (is (= (mapv shape (get-in py [:value :retractions]))
                     (mapv shape (:retractions clj))))))))))

(deftest used-together-links-are-priced-like-the-python-tool
  ;; a -- c is reachable through b by a used-together link (weight 2) and a
  ;; why link (1), or through d by two next-in-session links (3 each).
  (let [file (io/file (temp-dir) "used-together.json")
        _ (spit file (json/generate-string
                      {:pattern_ids ["a" "b" "c" "d"] :patterns 4 :records 1
                       :edges [{:a "a" :b "b" :kind "used-together"
                                :evidence [{:run "r" :order ["a" "b"] :positions [1 2]}]}
                               (edge "b" "c")
                               {:a "a" :b "d" :kind "next-in-session" :evidence []}
                               {:a "c" :b "d" :kind "next-in-session" :evidence []}]}))
        graph (loaded-graph file)
        clj (sut/retractions graph {:seeds ["a" "c"] :k 3})
        py (python-result file ["a" "c"])]
    (is (every? number? (map :weight (:edges graph))))
    (is (zero? (:exit py)) (:err py))
    (is (= 3 (:cost (first (:retractions clj)))))
    (is (= #{"a" "b" "c"} (set (:nodes (first (:retractions clj))))))
    (is (= (mapv shape (get-in py [:value :retractions]))
           (mapv shape (:retractions clj))))
    (is (= (mapv :cost (get-in py [:value :retractions]))
           (mapv :cost (:retractions clj))))))

(deftest isolated-seed-is-a-counted-failure-not-a-singleton
  (let [file (fixture-file :isolated)
        result (sut/retractions (loaded-graph file) {:seeds ["z"] :k 3})]
    (is (= :computed (:status result)))
    (is (= 1 (:failure-count result)))
    (is (= :isolated-seed (get-in result [:failures 0 :kind])))
    (is (empty? (:retractions result)))
    (is (not-any? #(= ["z"] (:nodes %)) (:retractions result)))))

(deftest disconnected-seeds-are-a-counted-failure
  (let [file (fixture-file :disconnected)
        result (sut/retractions (loaded-graph file) {:seeds ["a" "c"] :k 3})]
    (is (= :computed (:status result)))
    (is (= 1 (:failure-count result)))
    (is (= :disconnected-seeds (get-in result [:failures 0 :kind])))
    (is (empty? (:retractions result)))))

(deftest unknown-seed-is-distinct-from-isolated
  (let [file (fixture-file :isolated)
        result (sut/retractions (loaded-graph file) {:seeds ["not/library"] :k 3})]
    (is (= 1 (:failure-count result)))
    (is (= :seed-not-in-graph (get-in result [:failures 0 :kind])))
    (is (empty? (:retractions result)))))
