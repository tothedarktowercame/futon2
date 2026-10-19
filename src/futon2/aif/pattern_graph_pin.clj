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
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file FileAlreadyExistsException Files StandardCopyOption]))

(identity/register! *ns* *file*)

(def default-weights
  ;; "used-together" comes from applied War Machine run diffs (futon3c
  ;; scripts/pattern_graph_diff.py). Its weight equals co-cited's and matches
  ;; futon3c scripts/pattern_retraction.py: an interim choice by claude-2,
  ;; 2026-10-05, not a ruling, and the same whatever the run's outcome.
  {"why" 1 "how" 1 "used-together" 2 "co-cited" 2 "rejected-beside" 3
   "next-in-session" 3 "co-rejected" 8})

(defn- pin-file [graph-path] (io/file (str graph-path ".pin.edn")))
(defn- publication-file [graph-path] (io/file (str graph-path ".publication.edn")))

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

(defn- create-only-bytes!
  "Make DESTINATION a hard link to a fully written sibling temp.  createLink is
   create-only: unlike move with ATOMIC_MOVE it cannot replace a concurrent
   destination.  Reuse is accepted only when the already-visible bytes match."
  [destination bytes]
  (.mkdirs (.getParentFile destination))
  (let [tmp (io/file (.getParentFile destination)
                     (str "." (.getName destination) "." (System/nanoTime) ".tmp"))]
    (Files/write (.toPath tmp) bytes (make-array java.nio.file.OpenOption 0))
    (try
      (Files/createLink (.toPath destination) (.toPath tmp))
      (catch FileAlreadyExistsException _
        (when-not (and (.isFile destination)
                       (= (identity/sha256 bytes) (file-sha256 destination)))
          (throw (ex-info "generation path has different bytes" {:path (str destination)}))))
      (finally
        (Files/deleteIfExists (.toPath tmp)))))
  destination)

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

(defn- load-pinned-at [graph-file pin-path reported-path]
  (try
    (let [raw (json/parse-string (slurp graph-file) true)
          graph (normalized-graph raw)]
      (if-let [refusal (graph-refusal raw graph)]
        (assoc refusal :path reported-path)
        (if-not (.isFile pin-path)
          {:status :refused :kind :graph-pin-missing :path (.getPath pin-path)}
          (let [actual (file-sha256 graph-file)
                pin (edn/read-string (slurp pin-path))]
            (if (not= actual (:sha256 pin))
              {:status :refused :kind :graph-pin-mismatch
               :path reported-path :expected (:sha256 pin) :actual actual}
              (let [observed (pin-value graph-file raw graph)]
                (if (= (dissoc pin :sha256) (dissoc observed :sha256))
                  {:status :loaded :graph graph :pin pin}
                  {:status :refused :kind :graph-pin-mismatch
                   :path reported-path :expected pin :actual observed})))))))
    (catch Exception e
      {:status :refused :kind :graph-unreadable :path reported-path
       :message (ex-message e)})))

(defn publish!
  "Publish SOURCE's validated graph/pin generation by atomically switching ROOT's
   manifest.  Generation files are complete before the manifest moves, so an
   interruption leaves the prior readable generation selected.  BEFORE-SWITCH
   is test-only fault injection."
  ([source root] (publish! source root {}))
  ([source root {:keys [before-switch after-snapshot]}]
   (try
     (let [bytes (Files/readAllBytes (.toPath (io/file source)))
           _ (when after-snapshot (after-snapshot))
           raw (json/parse-string (String. bytes StandardCharsets/UTF_8) true)
           graph (normalized-graph raw)]
       (if-let [refusal (graph-refusal raw graph)]
         refusal
         (let [sha (identity/sha256 bytes)
               generation (io/file (str root ".generations") sha)
               graph-file (io/file generation "graph.json")
               pin-path (pin-file graph-file)]
           (create-only-bytes! graph-file bytes)
           (let [pin (pin-value graph-file raw graph)]
             (create-only-bytes! pin-path (.getBytes (str (pr-str pin) "\n") StandardCharsets/UTF_8)))
           (let [checked (load-pinned-at graph-file pin-path (str root))]
             (if (not= :loaded (:status checked))
               checked
               (do
                 (when before-switch (before-switch))
                 (atomic-write! (publication-file root)
                                {:schema :wm/pattern-graph-publication-v1
                                 :graph-path (.getCanonicalPath graph-file)
                                 :pin-path (.getCanonicalPath pin-path)
                                 :sha256 (:sha256 (:pin checked))})
                 {:status :published :path (str root) :pin (:pin checked)}))))))
     (catch Exception e
       {:status :refused :kind :graph-publication-failed :path (str root)
        :message (ex-message e)}))))

(defn load-pinned
  "Load a directly pinned graph, or the coherent generation named by ROOT's
   atomically switched publication manifest.  This reader never pins bytes."
  [graph-path]
  (let [graph-file (io/file graph-path)
        pin-path (pin-file graph-path)
        manifest-path (publication-file graph-path)]
    (cond
      (.isFile manifest-path)
      (try
        (let [{:keys [schema graph-path pin-path sha256]} (edn/read-string (slurp manifest-path))
              published-graph (io/file graph-path)
              published-pin (io/file pin-path)
              loaded (when (and (= :wm/pattern-graph-publication-v1 schema)
                                (= (.getCanonicalPath (pin-file published-graph)) (.getCanonicalPath published-pin)))
                       (load-pinned-at published-graph published-pin (str graph-file)))]
          (if (and (= :wm/pattern-graph-publication-v1 schema)
                   (= (.getCanonicalPath (pin-file published-graph)) (.getCanonicalPath published-pin))
                   (= sha256 (get-in loaded [:pin :sha256])))
            loaded
            {:status :refused :kind :graph-publication-invalid :path (str manifest-path)}))
        (catch Exception e
          {:status :refused :kind :graph-publication-invalid :path (str manifest-path)
           :message (ex-message e)}))

      (not (.isFile graph-file))
      {:status :refused :kind :graph-unreadable :path (str graph-path)
       :reason :graph-file-missing}

      :else
      (load-pinned-at graph-file pin-path (str graph-path)))))

(defn load-unpinned
  "Load and normalize GRAPH-PATH's current bytes without requiring a pin.
  Intended for observational views, never for an authoritative decision."
  [graph-path]
  (try
    (let [graph-file (io/file graph-path)
          raw (json/parse-string (slurp graph-file) true)
          graph (normalized-graph raw)]
      (if-let [refusal (graph-refusal raw graph)]
        (assoc refusal :path (str graph-path))
        {:status :loaded :graph graph
         :graph-ref {:path (str graph-path) :sha256 (file-sha256 graph-file)}}))
    (catch Exception e
      {:status :refused :kind :graph-unreadable :path (str graph-path)
       :message (ex-message e)})))
