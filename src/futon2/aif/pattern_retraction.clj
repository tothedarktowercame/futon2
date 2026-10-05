(ns futon2.aif.pattern-retraction
  "Pure Clojure translation of futon3c/scripts/pattern_retraction.py.

  canonical-edges mirrors canonical_graph; pair-graph mirrors pair_graph;
  adjacency, shortest-path, prune-nonseed-leaves and steiner-tree mirror their
  same-named Python functions; authored-direction and render-tree mirror the
  receipt rendering; components mirrors components; and retractions mirrors
  retractions plus the required typed failure boundary. No fallback tree is
  invented for an unknown, isolated, or disconnected seed set."
  (:require [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]))

(identity/register! *ns* *file*)

(def kind-order ["why" "how" "used-together" "co-cited" "rejected-beside"
                 "next-in-session" "co-rejected"])
(def kind-rank (zipmap kind-order (range)))

(defn- canonical-edges [graph]
  (->> (:edges graph)
       (map (fn [{:keys [a b kind evidence]}]
              (let [[left right] (sort [a b])]
                {:a left :b right :kind kind
                 :evidence (vec (sort-by pr-str (or evidence [])))})))
       (sort-by (juxt :a :b :kind #(pr-str (:evidence %))))
       vec))

(defn- kind-key [weights kind]
  [(get weights kind) (get kind-rank kind (count kind-order)) kind])

(defn- pair-graph [edges weights]
  (->> edges
       (reduce (fn [m {:keys [a b kind evidence]}]
                 (-> m
                     (update-in [[a b] :kinds] (fnil #(vec (distinct (conj % kind))) []))
                     (update-in [[a b] :by-kind kind] (fnil into []) evidence)))
               {})
       (reduce-kv
        (fn [m pair value]
          (let [kinds (vec (sort-by #(kind-key weights %) (:kinds value)))
                used (first kinds)]
            (assoc m pair (assoc value :kinds kinds :kind-used used
                                 :cost (get weights used))))) {})))

(defn- adjacency
  ([pairs] (adjacency pairs #{}))
  ([pairs removed]
   (->> pairs
        (reduce-kv (fn [m [a b :as pair] {:keys [cost]}]
                     (if (removed pair) m
                         (-> m (update a (fnil conj []) [b cost pair])
                             (update b (fnil conj []) [a cost pair])))) {})
        (reduce-kv (fn [m node xs]
                     (assoc m node (vec (sort-by (juxt second first #(nth % 2)) xs)))) {}))))

(defn- shortest-path [adj source target]
  (loop [queue (sorted-set [0 [source] source []]) best {}]
    (when-let [[cost nodes node path :as entry] (first queue)]
      (let [queue (disj queue entry) key [cost nodes]]
        (if (and (contains? best node) (not (pos? (compare (get best node) key))))
          (recur queue best)
          (if (= node target)
            [cost path]
            (let [best (assoc best node key)
                  additions (for [[neighbour edge-cost edge] (get adj node [])
                                  :when (not (some #{neighbour} nodes))]
                              [(+ cost edge-cost) (conj nodes neighbour)
                               neighbour (conj path edge)])]
              (recur (into queue additions) best))))))))

(defn- prune-nonseed-leaves [edge-set seeds]
  (loop [edges (set edge-set)]
    (let [degree (frequencies (mapcat identity edges))
          leaf (first (sort (for [[node n] degree
                                  :when (and (= 1 n) (not (seeds node)))] node)))]
      (if-not leaf edges
          (recur (disj edges (first (filter #(some #{leaf} %) edges))))))))

(defn- root [parent node]
  (loop [node node]
    (let [p (get parent node node)]
      (if (= p node) node (recur p)))))

(defn- steiner-tree
  ([pairs seeds] (steiner-tree pairs seeds #{}))
  ([pairs seeds removed]
   (let [adj (adjacency pairs removed)
         closure (vec (for [i (range (count seeds))
                            right (subvec seeds (inc i))
                            :let [left (nth seeds i)
                                  found (shortest-path adj left right)]
                            :when found]
                        [(first found) left right (second found)]))]
     (when (= (count closure) (/ (* (count seeds) (dec (count seeds))) 2))
       (let [{:keys [parent expanded]}
             (reduce (fn [{:keys [parent expanded] :as state}
                          [_ left right path]]
                       (let [a (root parent left) b (root parent right)]
                         (if (= a b) state
                             {:parent (assoc parent a b)
                              :expanded (into expanded path)})))
                     {:parent (zipmap seeds seeds) :expanded #{}}
                     (sort closure))]
         (when (= 1 (count (set (map #(root parent %) seeds))))
           (prune-nonseed-leaves expanded (set seeds))))))))

(defn- authored-direction [[a b] kinds by-kind]
  (let [directions
        (for [kind kinds :when (contains? #{"why" "how"} kind)
              evidence (get by-kind kind)
              :let [path (str/replace (or (:file evidence) "") "\\" "/")
                    source (first (filter #(str/ends-with? path (str "/" % ".flexiarg")) [a b]))]
              :when source]
          [source (if (= source a) b a)])]
    (when-let [[from to] (first (sort (distinct directions)))]
      {:from from :to to})))

(defn- render-tree [edge-set pairs]
  (let [edges
        (mapv (fn [[a b :as pair]]
                (let [{:keys [kinds by-kind kind-used]} (get pairs pair)]
                  {:a a :b b :kinds kinds :kind-used kind-used
                   :direction (authored-direction pair kinds by-kind)
                   :evidence (vec (sort-by pr-str (mapcat #(get by-kind %) kinds)))}))
              (sort edge-set))
        nodes (vec (sort (set (mapcat identity edge-set))))]
    {:cost (reduce + 0 (map #(get-in pairs [% :cost]) edge-set))
     :size (count nodes) :weak-edges (count (filter #(= "co-rejected" (:kind-used %)) edges))
     :nodes nodes :edges edges}))

(defn- components [nodes pairs]
  (let [adj (adjacency pairs)]
    (->> (loop [unseen (set nodes) result []]
           (if (empty? unseen) result
               (let [start (first (sort unseen))
                     found (loop [stack [start] seen #{}]
                             (if-let [node (peek stack)]
                               (if (seen node)
                                 (recur (pop stack) seen)
                                 (recur (into (pop stack) (map first (get adj node [])))
                                        (conj seen node)))
                               seen))]
                 (recur (apply disj unseen found) (conj result (vec (sort found)))))))
         (sort-by (juxt (comp - count) identity)) vec)))

(defn- failure [kind seeds]
  {:status :computed :retractions []
   :failures [{:kind kind :seeds (vec (sort seeds))}] :failure-count 1})

(defn retractions
  "Compute up to k deterministic connected retractions over a pinned graph."
  [graph {:keys [seeds k weights]
          :or {k 3 weights graph-pin/default-weights}}]
  (let [seeds (vec (sort (distinct seeds)))
        weights (merge graph-pin/default-weights weights)
        edges (canonical-edges graph)
        pairs (pair-graph edges weights)
        edge-nodes (set (mapcat (juxt :a :b) edges))
        library (set (:pattern-ids graph))
        unknown (remove library seeds)
        isolated (filter #(and (library %) (not (edge-nodes %))) seeds)]
    (cond
      (seq unknown) (failure :seed-not-in-graph unknown)
      (seq isolated) (failure :isolated-seed isolated)
      :else
      (let [comps (components (vec (sort edge-nodes)) pairs)
            seed-components (vec (keep-indexed
                                  (fn [i comp]
                                    (let [found (vec (sort (filter (set comp) seeds)))]
                                      (when (seq found)
                                        {:component (inc i) :size (count comp) :seeds found})))
                                  comps))]
        (if (not= 1 (count seed-components))
          (failure :disconnected-seeds seeds)
          (let [first-tree (if (= 1 (count seeds)) #{} (steiner-tree pairs seeds))
                first-rendered (when (some? first-tree)
                                 (cond-> (render-tree first-tree pairs)
                                   (= 1 (count seeds)) (assoc :nodes seeds :size 1)))
                branch (fn [rendered already-removed seen]
                         (reduce
                          (fn [{:keys [candidates seen] :as acc} edge]
                            (let [removed (conj already-removed edge)]
                              (if (seen removed) acc
                                  (let [seen (conj seen removed)
                                        tree (steiner-tree pairs seeds removed)]
                                    {:seen seen
                                     :candidates (cond-> candidates
                                                   (some? tree)
                                                   (conj {:rendered (render-tree tree pairs)
                                                          :removed removed}))}))))
                          {:candidates [] :seen seen}
                          (sort (map (juxt :a :b) (:edges rendered)))))
                initial-state (if first-rendered
                                (branch first-rendered #{} #{#{}})
                                {:candidates [] :seen #{#{}}})
                answers
                (loop [answer (cond-> [] first-rendered (conj first-rendered))
                       candidates (:candidates initial-state)
                       seen (:seen initial-state)]
                  (if (or (>= (count answer) k) (empty? candidates)) answer
                      (let [candidates (vec (sort-by (fn [{:keys [rendered]}]
                                                       [(:cost rendered) (:nodes rendered)
                                                        (mapv (juxt :a :b :kind-used)
                                                              (:edges rendered))])
                                                     candidates))
                            chosen (first candidates)
                            more (branch (:rendered chosen) (:removed chosen) seen)
                            rendered (:rendered chosen)
                            new? (not (some #(= (:nodes %) (:nodes rendered)) answer))]
                        (recur (cond-> answer new? (conj rendered))
                               (into (subvec candidates 1) (:candidates more))
                               (:seen more)))))
                ranked (mapv #(assoc %1 :rank %2)
                             (sort-by (juxt :cost :nodes) answers) (iterate inc 1))]
            {:status :computed :retractions ranked :failures [] :failure-count 0
             :component {:size (:size (first seed-components))
                         :seeds-connected true :seed-components seed-components}
             :params {:k k :weights weights}}))))))
