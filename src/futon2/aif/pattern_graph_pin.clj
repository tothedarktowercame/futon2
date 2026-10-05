(ns futon2.aif.pattern-graph-pin
  "Load a byte-pinned mined pattern graph for in-process retraction.

  The retained shape matches pattern_retraction.py's calculation: sorted
  undirected endpoints, edge kind, evidence (needed to recover authored
  direction), and the default weight selected by kind.  Graph-level input
  counts are retained on the pin.  Presentation summaries, cumulative
  component tables, and the 象-family report are dropped because retraction
  recomputes connectivity. Legacy :co-rejected edges are counted and removed;
  Joe ruled that they are too weak to write down, and the current miner no
  longer emits them."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity])
  (:import [java.nio.file Files StandardCopyOption]))

(identity/register! *ns* *file*)

(def default-weights
  ;; "used-together" comes from applied War Machine run diffs (futon3c
  ;; scripts/pattern_graph_diff.py). Its weight equals co-cited's and matches
  ;; futon3c scripts/pattern_retraction.py: an interim choice by claude-2,
  ;; 2026-10-05, not a ruling, and the same whatever the run's outcome.
  {"why" 1 "how" 1 "used-together" 2 "co-cited" 2 "rejected-beside" 3
   "next-in-session" 3 "co-rejected" 8})

(defn- pin-file [graph-path] (io/file (str graph-path ".pin.edn")))

(defn- file-sha256 [file]
  (identity/sha256 (Files/readAllBytes (.toPath (io/file file)))))

(defn- canonical-edge [{:keys [a b kind evidence]}]
  (let [[left right] (sort [a b])]
    {:a left :b right :kind kind
     :weight (get default-weights kind)
     :evidence (vec (sort-by pr-str (or evidence [])))}))

(defn- normalized-graph [raw]
  (let [pattern-ids (vec (sort (distinct (:pattern_ids raw))))
        raw-edges (vec (:edges raw))
        removed (filterv #(= "co-rejected" (:kind %)) raw-edges)
        edges (->> raw-edges
                   (remove #(= "co-rejected" (:kind %)))
                   (map canonical-edge)
                   (sort-by (juxt :a :b :kind :evidence)) vec)
        nodes (vec (sort (set (mapcat (juxt :a :b) edges))))]
    {:pattern-ids pattern-ids :nodes nodes :edges edges
     :reported-node-count (:patterns raw)
     :removed-edges {:co-rejected (count removed)}}))

(defn- graph-refusal [raw graph]
  (let [declared (set (:pattern-ids graph))
        endpoints (set (:nodes graph))
        outside (vec (sort (remove declared endpoints)))]
    (cond
      (not (vector? (:pattern_ids raw)))
      {:status :refused :kind :graph-without-pattern-ids}

      (seq outside)
      {:status :refused :kind :graph-endpoint-outside-pattern-ids
       :count (count outside) :endpoints outside})))

(defn- component-sizes [nodes edges]
  (let [adj (reduce (fn [m {:keys [a b]}]
                      (-> m (update a (fnil conj #{}) b)
                          (update b (fnil conj #{}) a)))
                    {} edges)]
    (loop [unseen (set nodes) sizes []]
      (if (empty? unseen)
        sizes
        (let [start (first unseen)
              found (loop [todo [start] seen #{}]
                      (if-let [n (peek todo)]
                        (if (seen n)
                          (recur (pop todo) seen)
                          (recur (into (pop todo) (get adj n)) (conj seen n)))
                        seen))]
          (recur (apply disj unseen found) (conj sizes (count found))))))))

(defn- summary-count [raw kind]
  (:edges (some #(when (= kind (:through %)) %) (:summary raw))))

(defn- pin-value [graph-path raw graph]
  (let [reported (or (:reported-node-count graph) (count (:nodes graph)))
        component-sizes (component-sizes (:nodes graph) (:edges graph))]
    {:schema :wm/pattern-graph-pin-v1
     :sha256 (file-sha256 graph-path)
     :node-count reported
     :pattern-id-count (count (:pattern-ids graph))
     :edge-count (count (:edges graph))
     :giant-component-size (reduce max 0 component-sizes)
     :nodes-without-edges (max 0 (- reported (count (:nodes graph))))
     :removed-edges (:removed-edges graph)
     :inputs {:analyses (:records raw)
              :authored-relations
              {:why (summary-count raw "why")
               :how (summary-count raw "how")}}}))

(defn- atomic-write! [file value]
  (.mkdirs (.getParentFile file))
  (let [tmp (io/file (.getParentFile file)
                     (str "." (.getName file) "." (System/nanoTime) ".tmp"))]
    (spit tmp (str (pr-str value) "\n"))
    (Files/move (.toPath tmp) (.toPath file)
                (into-array StandardCopyOption
                            [StandardCopyOption/ATOMIC_MOVE
                             StandardCopyOption/REPLACE_EXISTING]))
    value))

(defn pin!
  "Pin GRAPH-PATH's current bytes and effective (co-rejected-free) topology."
  [graph-path]
  (try
    (let [raw (json/parse-string (slurp graph-path) true)
          graph (normalized-graph raw)]
      (if-let [refusal (graph-refusal raw graph)]
        refusal
        (atomic-write! (pin-file graph-path) (pin-value graph-path raw graph))))
    (catch Exception e
      {:status :refused :kind :graph-unreadable :path (str graph-path)
       :message (ex-message e)})))

(defn load-pinned
  "Load the effective graph only when its adjacent pin matches its bytes."
  [graph-path]
  (let [graph-file (io/file graph-path)
        pin-path (pin-file graph-path)]
    (cond
      (not (.isFile graph-file))
      {:status :refused :kind :graph-unreadable :path (str graph-path)
       :reason :graph-file-missing}

      :else
      (try
        (let [raw (json/parse-string (slurp graph-file) true)
              graph (normalized-graph raw)]
          (if-let [refusal (graph-refusal raw graph)]
            (assoc refusal :path (str graph-path))
            (if-not (.isFile pin-path)
              {:status :refused :kind :graph-pin-missing :path (.getPath pin-path)}
              (let [actual (file-sha256 graph-file)
                    pin (edn/read-string (slurp pin-path))]
                (if (not= actual (:sha256 pin))
                  {:status :refused :kind :graph-pin-mismatch
                   :path (str graph-path) :expected (:sha256 pin) :actual actual}
                  (let [observed (pin-value graph-file raw graph)]
                    (if (= (dissoc pin :sha256) (dissoc observed :sha256))
                      {:status :loaded :graph graph :pin pin}
                      {:status :refused :kind :graph-pin-mismatch
                       :path (str graph-path) :expected pin :actual observed})))))))
        (catch Exception e
          {:status :refused :kind :graph-unreadable :path (str graph-path)
           :message (ex-message e)})))))
