(ns futon2.aif.cascade-order-check
  "Pure mirror of CascadeOrder.lean for recorded cascade descent relations.

  `acyclicDescent` is enforced by callers only after Joe chooses a fold seam;
  this namespace merely returns typed findings. `hasMeets` is independently
  reported over the carrier named by the edge endpoints.")

(defn- normalize-edge [edge]
  (cond
    (map? edge) [(:from edge) (:to edge)]
    (sequential? edge) (vec (take 2 edge))
    :else ::invalid))

(defn- descent-edges [input]
  (let [raw (if (map? input) (:descent input ::missing) input)
        elements (when (map? input) (:elements input ::missing))]
    (cond
      (= ::missing raw) {:error :descent-missing}
      (not (sequential? raw)) {:error :descent-not-sequential}
      :else
      (let [edges (mapv normalize-edge raw)]
        (if (some #(or (= ::invalid %) (not= 2 (count %))
                       (some nil? %)) edges)
          {:error :descent-edge-malformed}
          {:edges edges
           :elements elements})))))

(defn- adjacency [edges]
  (reduce (fn [m [from to]] (update m from (fnil conj []) to)) {} edges))

(defn- cycle-from [graph start]
  (letfn [(walk [node path positions]
            (if-let [index (get positions node)]
              (conj (subvec path index) node)
              (let [next-path (conj path node)
                    next-positions (assoc positions node (count path))]
                (some #(walk % next-path next-positions)
                      (sort-by pr-str (get graph node))))))]
    (walk start [] {})))

(defn- find-cycle [edges]
  (let [graph (adjacency edges)
        nodes (set (mapcat identity edges))]
    (some #(cycle-from graph %) (sort-by pr-str nodes))))

(defn- reachability [nodes edges]
  (loop [reachable (into (set (map (fn [x] [x x]) nodes)) edges)]
    (let [expanded
          (into reachable
                (for [[a b] reachable
                      [c d] reachable
                      :when (= b c)]
                  [a d]))]
      (if (= reachable expanded) reachable (recur expanded)))))

(defn- missing-meet [nodes reachable]
  (some
   (fn [[a b]]
     (let [lowers (set (filter #(and (contains? reachable [% a])
                                     (contains? reachable [% b]))
                               nodes))
           meets (filter (fn [m]
                           (every? #(contains? reachable [% m]) lowers))
                         lowers)]
       (when (empty? meets) {:pair [a b] :lower-bounds lowers})))
   (for [a (sort-by pr-str nodes) b (sort-by pr-str nodes)] [a b])))

(defn check-cascade-order
  "Return separate typed verdicts for `acyclicDescent` and `hasMeets`.

  INPUT is either `{:descent [[from to] ...]}` or the registry's bare vector
  of `{:from from :to to}` maps. A map may also declare the cascade carrier as
  `:elements`; without it, `hasMeets` is unwitnessable because isolated elements
  cannot be recovered from edges. A cycle failure includes a closed path."
  [input]
  (let [{:keys [edges elements error]} (descent-edges input)]
    (if error
      {:acyclic-descent {:result :unwitnessable :why error}
       :has-meets {:result :unwitnessable :why error}}
      (let [nodes (if (= ::missing elements)
                    (set (mapcat identity edges))
                    (set elements))
            cycle (find-cycle edges)
            acyclic (if cycle
                      {:result :failed :why :descent-cycle :cycle cycle}
                      {:result :passed})
            meets (if (= ::missing elements)
                    {:result :unwitnessable :why :carrier-not-declared}
                    (let [missing (missing-meet nodes
                                                (reachability nodes edges))]
                      (if missing
                        (merge {:result :failed :why :pair-has-no-meet} missing)
                        {:result :passed})))]
        {:acyclic-descent acyclic
         :has-meets meets}))))
