(ns futon2.aif.pattern-graph-pin-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as sut]))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "pattern-graph-pin"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(def fixture
  {:records 12 :patterns 5 :pattern_ids ["p/a" "p/b" "p/c" "p/d" "p/isolated"]
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
    (is (= ["p/a" "p/b" "p/c" "p/d" "p/isolated"]
           (get-in loaded [:graph :pattern-ids])))
    (is (= {:node-count 5 :pattern-id-count 5 :edge-count 2 :giant-component-size 3
            :nodes-without-edges 2 :removed-edges {:co-rejected 1}}
           (select-keys pin [:node-count :pattern-id-count :edge-count :giant-component-size
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

(deftest publication-switch-keeps-last-coherent-generation-on-interruption
  (let [dir (temp-dir)
        root (io/file dir "published-graph.json")
        old-source (write-graph! dir fixture)
        new-source (let [file (io/file dir "new-graph.json")]
                     (spit file (json/generate-string (assoc fixture :records 13)))
                     file)]
    (is (= :published (:status (sut/publish! old-source root))))
    ;; Reusing the content-addressed generation is accepted only for its same bytes.
    (is (= :published (:status (sut/publish! old-source root))))
    (is (= 12 (get-in (sut/load-pinned root) [:pin :inputs :analyses])))
    (is (= :refused
           (:status (sut/publish! new-source root
                                  {:before-switch #(throw (ex-info "interrupt" {}))}))))
    ;; The manifest never moved, so the new bytes are not exposed with an old pin.
    (let [loaded (sut/load-pinned root)]
      (is (= :loaded (:status loaded)))
      (is (= 12 (get-in loaded [:pin :inputs :analyses])))
      (is (not= (get-in (sut/publish! new-source root) [:pin :sha256])
                (get-in loaded [:pin :sha256]))))
    (is (= 13 (get-in (sut/load-pinned root) [:pin :inputs :analyses])))))

(deftest publication-uses-one-snapshot-and-binds-manifest-digest
  (let [dir (temp-dir)
        root (io/file dir "published-graph.json")
        source (write-graph! dir fixture)
        changed (assoc fixture :records 13)]
    ;; The producer mutates its input after publication snapshots it.  The
    ;; selected generation still contains only the snapshot's old bytes.
    (is (= :published
           (:status (sut/publish! source root
                                  {:after-snapshot #(spit source (json/generate-string changed))}))))
    (is (= 12 (get-in (sut/load-pinned root) [:pin :inputs :analyses])))
    (let [manifest (io/file (str root ".publication.edn"))]
      (spit manifest (pr-str (assoc (edn/read-string (slurp manifest)) :sha256 "substituted")))
      (let [refusal (sut/load-pinned root)]
        (is (= :refused (:status refusal)))
        (is (= :graph-publication-invalid (:kind refusal)))
        (is (nil? (:graph refusal)))))))

(deftest generation-conflicts-never-replace-graph-or-pin
  (let [dir (temp-dir)
        source (write-graph! dir fixture)
        sha (identity/sha256 (.getBytes (slurp source) "UTF-8"))
        graph-root (io/file dir "graph-conflict.json")
        graph-file (io/file (str graph-root ".generations") sha "graph.json")]
    (.mkdirs (.getParentFile graph-file))
    (spit graph-file "conflicting graph bytes")
    (is (= :refused (:status (sut/publish! source graph-root))))
    (is (= "conflicting graph bytes" (slurp graph-file))))
  (let [dir (temp-dir)
        source (write-graph! dir fixture)
        sha (identity/sha256 (.getBytes (slurp source) "UTF-8"))
        pin-root (io/file dir "pin-conflict.json")
        graph-file (io/file (str pin-root ".generations") sha "graph.json")
        pin-file (io/file (str graph-file ".pin.edn"))]
    (.mkdirs (.getParentFile graph-file))
    (io/copy source graph-file)
    (spit pin-file "{:conflicting true}\n")
    (is (= :refused (:status (sut/publish! source pin-root))))
    (is (= "{:conflicting true}\n" (slurp pin-file)))))

(deftest missing-pin-is-typed
  (let [file (write-graph! (temp-dir) fixture)
        result (sut/load-pinned file)]
    (is (= :refused (:status result)))
    (is (= :graph-pin-missing (:kind result)))))

(deftest graph-without-pattern-ids-is-typed
  (let [file (write-graph! (temp-dir) (dissoc fixture :pattern_ids))
        pinned (sut/pin! file)
        result (sut/load-pinned file)]
    (is (= :graph-without-pattern-ids (:kind pinned)))
    (is (= :refused (:status result)))
    (is (= :graph-without-pattern-ids (:kind result)))))

(deftest graph-endpoint-outside-pattern-ids-is-typed
  (let [file (write-graph! (temp-dir) (update fixture :pattern_ids pop))
        pinned (sut/pin! file)
        result (sut/load-pinned file)]
    ;; p/isolated was popped, so make the missing id an endpoint instead.
    (is (map? pinned))
    (is (= :loaded (:status result)))
    (let [bad (write-graph! (temp-dir) (update fixture :pattern_ids
                                             #(vec (remove #{"p/c"} %))))
          refusal (sut/load-pinned bad)]
      (is (= :refused (:status refusal)))
      (is (= :graph-endpoint-outside-pattern-ids (:kind refusal)))
      (is (= 1 (:count refusal))))))

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
          (is (= (:pattern-id-count pin) (count (get-in loaded [:graph :pattern-ids]))))
          (is (pos? (:node-count pin)))
          (is (pos? (:edge-count pin))))))))
