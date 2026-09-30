(ns futon2.aif.pattern-graph-pin-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.pattern-graph-pin :as sut]))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "pattern-graph-pin"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(def fixture
  {:records 12 :patterns 5
   :summary [{:through "why" :edges 1} {:through "how" :edges 0}]
   :edges [{:a "p/a" :b "p/b" :kind "why" :evidence [{:file "p/a.flexiarg"}]}
           {:a "p/b" :b "p/c" :kind "co-cited" :evidence [{:at "turn-1"}]}
           {:a "p/a" :b "p/c" :kind "co-rejected" :evidence [{:at "old"}]}]})

(defn write-graph! [dir value]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string value))
    file))

(deftest pin-and-load-effective-graph
  (let [file (write-graph! (temp-dir) fixture)
        pin (sut/pin! file)
        loaded (sut/load-pinned file)]
    (is (= :loaded (:status loaded)))
    (is (= pin (:pin loaded)))
    (is (= {:node-count 5 :edge-count 2 :giant-component-size 3
            :nodes-without-edges 2 :removed-edges {:co-rejected 1}}
           (select-keys pin [:node-count :edge-count :giant-component-size
                             :nodes-without-edges :removed-edges])))
    (is (= #{"why" "co-cited"} (set (map :kind (get-in loaded [:graph :edges])))))
    (is (= #{1 2} (set (map :weight (get-in loaded [:graph :edges])))))))

(deftest changed-graph-bytes-refuse-without-returning-graph
  (let [file (write-graph! (temp-dir) fixture)
        _ (sut/pin! file)]
    (spit file " " :append true)
    (let [result (sut/load-pinned file)]
      (is (= :refused (:status result)))
      (is (= :graph-pin-mismatch (:kind result)))
      (is (nil? (:graph result))))))

(deftest missing-pin-is-typed
  (let [file (write-graph! (temp-dir) fixture)
        result (sut/load-pinned file)]
    (is (= :refused (:status result)))
    (is (= :graph-pin-missing (:kind result)))))

(deftest real-graph-copy-can-be-pinned
  (let [source (io/file "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")]
    (if-not (.isFile source)
      (println "PATTERN-GRAPH-REAL absent; produce with futon3c/scripts/mined_pattern_graph.py --out"
               (.getPath source))
      (let [dir (temp-dir)
            copy (io/file dir "mined-pattern-graph.json")]
        (io/copy source copy)
        (let [pin (sut/pin! copy)
              loaded (sut/load-pinned copy)]
          (println "PATTERN-GRAPH-REAL" (pr-str pin))
          (is (= :loaded (:status loaded)))
          (is (pos? (:node-count pin)))
          (is (pos? (:edge-count pin))))))))

