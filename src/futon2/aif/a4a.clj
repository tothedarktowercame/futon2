(ns futon2.aif.a4a
  "A4a/R17 capability concept-formation.

   This namespace is pure: no substrate, HTTP, Drawbridge, clock, or random
   dependencies. Demo-phase concepts produced by this mechanism are
   demo-validated until real :capability/* production writes flow."
  (:require [clojure.string :as str]
            [futon2.aif.bmr :as bmr])
  (:import [java.net URLEncoder]))

(def prior
  "Uniform Dirichlet prior concentration for every capability/mission cell."
  0.1)

(defn- raw-id
  [x]
  (if (map? x)
    (or (:xt/id x)
        (:entity/id x)
        (:capability/id x)
        (:id x)
        (:name x))
    x))

(defn- stable-id
  [x]
  (str (raw-id x)))

(defn- id-string
  [x]
  (cond
    (keyword? x) (if-let [ns-part (namespace x)]
                   (str ns-part "/" (name x))
                   (name x))
    (symbol? x) (if-let [ns-part (namespace x)]
                  (str ns-part "/" (name x))
                  (name x))
    :else (str (raw-id x))))

(defn normalize-mission-id
  "Normalise mission IDs across graph keys and mission-pattern-scopes rows."
  [mission-id]
  (str/replace (id-string mission-id) #"@.*$" ""))

(defn pattern-stem
  "Return the last path/name segment of a pattern id such as ns/stem."
  [pattern-id]
  (last (str/split (id-string pattern-id) #"/")))

(defn- capability-discharge?
  [discharge]
  (= "capability" (if (keyword? (:type discharge))
                    (name (:type discharge))
                    (str (:type discharge)))))

(defn- edge-pair
  [edge]
  (cond
    (and (vector? edge) (= 2 (count edge)))
    [(stable-id (first edge)) (stable-id (second edge))]

    (map? edge)
    [(stable-id (or (:capability edge) (:capability/id edge) (:cap edge)))
     (stable-id (or (:mission edge) (:mission/id edge) (:outcome edge)))]

    :else
    (throw (ex-info "capability edge must be [capability mission] or a map"
                    {:edge edge}))))

(defn- discharge-pair
  [discharge]
  [(stable-id (:endpoint discharge)) (stable-id (:mission discharge))])

(defn- sorted-ids
  [xs]
  (vec (sort-by str (distinct (remove str/blank? (map stable-id xs))))))

(defn- increment-cell
  [matrix outcomes-by-id [capability mission]]
  (let [outcome-index (get outcomes-by-id mission)]
    (if (and (contains? matrix capability) outcome-index)
      (update-in matrix [capability outcome-index] + 1.0)
      matrix)))

(defn corpus->concentration
  "Build the capability x mission concentration matrix from a corpus.

   Input shape:
   {:capabilities [...]
    :edges [[capability-id mission-id] ...]
    :discharges [{:mission mission-id :endpoint capability-id :type :capability} ...]}

   Each observed edge/discharge adds one count to its cell, on top of the
   uniform 0.1 prior."
  [{:keys [capabilities edges discharges]}]
  (let [edge-pairs (mapv edge-pair (or edges []))
        capability-discharges (filterv capability-discharge? (or discharges []))
        discharge-pairs (mapv discharge-pair capability-discharges)
        capability-ids (sorted-ids (concat capabilities
                                           (map first edge-pairs)
                                           (map first discharge-pairs)))
        outcomes (sorted-ids (concat (map second edge-pairs)
                                     (map second discharge-pairs)))
        outcomes-by-id (into {} (map-indexed (fn [i outcome] [outcome i]) outcomes))
        empty-row (vec (repeat (count outcomes) prior))
        initial (into {} (map (fn [capability] [capability empty-row]) capability-ids))
        matrix (reduce #(increment-cell %1 outcomes-by-id %2)
                       initial
                       (concat edge-pairs discharge-pairs))]
    {:capabilities capability-ids
     :outcomes outcomes
     :concentrations matrix
     :prior prior}))

(defn- concentration-input
  [x]
  (if (and (map? x) (:concentrations x) (:outcomes x) (:capabilities x))
    x
    (corpus->concentration x)))

(defn- pair-reduced-prior
  [row-i row-j]
  (let [pooled (mapv #(/ (+ %1 %2) 2.0) row-i row-j)]
    (vec (concat pooled pooled))))

(defn- score-pair
  [row-i row-j]
  (let [full-prior (vec (repeat (+ (count row-i) (count row-j)) prior))
        full-posterior (vec (concat row-i row-j))
        reduced-prior (pair-reduced-prior row-i row-j)]
    (bmr/bayesian-model-reduction full-prior full-posterior reduced-prior)))

(defn- find-root
  [parents x]
  (let [parent (get parents x x)]
    (if (= parent x)
      x
      (recur parents parent))))

(defn- union-roots
  [parents a b]
  (let [ra (find-root parents a)
        rb (find-root parents b)
        survivor (first (sort-by str [ra rb]))
        retired (if (= survivor ra) rb ra)]
    (if (= ra rb)
      parents
      (assoc parents retired survivor))))

(defn- class-row
  [concentrations members]
  (let [rows (mapv concentrations members)]
    (if (= 1 (count rows))
      (first rows)
      (mapv (fn [values]
              (+ prior (reduce + (map #(- % prior) values))))
            (apply map vector rows)))))

(defn reduce-concepts
  "Score pairwise BMR merge hypotheses and return surviving capability concepts.

   Accepted candidate pairs have delta-F <= -3. The returned concepts are the
   deterministic representative IDs of their equivalence classes."
  [corpus-or-concentrations]
  (let [{:keys [capabilities outcomes concentrations] :as concentration}
        (concentration-input corpus-or-concentrations)
        pairs (for [i (range (count capabilities))
                    j (range (inc i) (count capabilities))]
                [(nth capabilities i) (nth capabilities j)])
        scores (mapv (fn [[cap-i cap-j]]
                       (let [score (if (seq outcomes)
                                     (score-pair (get concentrations cap-i)
                                                 (get concentrations cap-j))
                                     {:reduced-posterior []
                                      :delta-F 0.0
                                      :accept? false})]
                         (assoc score :pair [cap-i cap-j])))
                     pairs)
        accepted (filterv :accept? scores)
        parents (reduce (fn [acc capability] (assoc acc capability capability))
                        {}
                        capabilities)
        merged-parents (reduce (fn [acc {:keys [pair]}]
                                 (union-roots acc (first pair) (second pair)))
                               parents
                               accepted)
        groups (reduce (fn [acc capability]
                         (update acc (find-root merged-parents capability) conj capability))
                       {}
                       capabilities)
        classes (into (sorted-map)
                      (map (fn [[representative members]]
                             [representative (vec (sort-by str members))]))
                      groups)
        concepts (vec (keys classes))
        concept-concentrations (into (sorted-map)
                                     (map (fn [[concept members]]
                                            [concept (class-row concentrations members)]))
                                     classes)]
    (assoc concentration
           :merge-scores scores
           :accepted-merges accepted
           :concepts concepts
           :equivalence-classes classes
           :concept-concentrations concept-concentrations)))

(defn- rms
  [xs]
  (if (seq xs)
    (Math/sqrt (/ (reduce + (map #(* % %) xs)) (count xs)))
    0.0))

(defn concept->stddev
  "Return {concept -> aggregate stddev} in endpoint-count units, never variance."
  [reduced-concepts]
  (let [reduced (if (:concept-concentrations reduced-concepts)
                  reduced-concepts
                  (reduce-concepts reduced-concepts))]
    (into (sorted-map)
          (map (fn [[concept row]]
                 [concept (rms (map :stddev (bmr/dirichlet-moments row)))]))
          (:concept-concentrations reduced))))

(defn capability->concept
  "Return {member-capability -> representative-concept} from reduced concepts.

   In the no-merge case this is the identity map over singleton classes."
  [reduced-concepts]
  (into (sorted-map)
        (mapcat (fn [[concept members]]
                  (map (fn [member] [member concept]) members)))
        (:equivalence-classes reduced-concepts)))

(defn- resolve-capability
  [resolver capability]
  (or (get resolver capability)
      (get resolver (stable-id capability))))

(defn model-uncertainty-for-produces
  "Return posterior-spread uncertainty for a mission's produced capabilities.

   Produced capabilities resolve through BMR equivalence classes; each distinct
   concept contributes its stddev once, in endpoint-count units. Unknown
   capabilities contribute 0."
  [reduced-concepts produces-set]
  (let [resolver (capability->concept reduced-concepts)
        stddevs (concept->stddev reduced-concepts)
        concepts (set (keep #(resolve-capability resolver %) produces-set))]
    (reduce + 0.0 (map #(get stddevs % 0.0) concepts))))

(defn make-model-uncertainty-fn
  "Return a pure two-argument model-uncertainty closure for efe/graph-control-terms.

   The mission node is accepted for arity compatibility; this pattern-grain
   resolver keys on the normalised mission id."
  [mission->model-uncertainty]
  (fn [mission-id _mission]
    (double (or (get mission->model-uncertainty mission-id)
                (get mission->model-uncertainty (normalize-mission-id mission-id))
                0.0))))

(defn- add-edge-count
  [matrix outcomes-by-id [pattern mission]]
  (let [outcome-index (get outcomes-by-id mission)]
    (if (and (contains? matrix pattern) outcome-index)
      (update-in matrix [pattern outcome-index] + 1.0)
      matrix)))

(defn- aggregate-rows
  [rows]
  (if (seq rows)
    (mapv (fn [values]
            (+ prior (reduce + (map #(- % prior) values))))
          (apply map vector rows))
    []))

(defn constellation->model-uncertainty
  "Return {constellation -> aggregate posterior stddev} from pattern co-occurrence.

   `pattern->constellation` maps pattern stems to constellation ids.
   `corpus-edges` is a seq of [pattern mission] observations."
  [pattern->constellation corpus-edges]
  (let [pattern->constellation (into {} (map (fn [[pattern constellation]]
                                               [(pattern-stem pattern) constellation]))
                                     pattern->constellation)
        edges (mapv (fn [[pattern mission]]
                      [(pattern-stem pattern) (normalize-mission-id mission)])
                    corpus-edges)
        patterns (vec (sort (keys pattern->constellation)))
        missions (vec (sort (distinct (map second edges))))
        outcomes-by-id (into {} (map-indexed (fn [i mission] [mission i]) missions))
        empty-row (vec (repeat (count missions) prior))
        pattern-rows (reduce #(add-edge-count %1 outcomes-by-id %2)
                             (into {} (map (fn [pattern] [pattern empty-row]) patterns))
                             edges)
        constellation-rows (reduce-kv
                            (fn [acc pattern constellation]
                              (update acc constellation conj (get pattern-rows pattern empty-row)))
                            (sorted-map)
                            pattern->constellation)]
    (into (sorted-map)
          (map (fn [[constellation rows]]
                 (let [row (aggregate-rows rows)]
                   [constellation (if (seq row)
                                    (rms (map :stddev
                                              (bmr/dirichlet-moments row)))
                                    0.0)])))
          constellation-rows)))

(defn mission->model-uncertainty
  "Return {mission-id -> sum of distinct constellation stddevs for its patterns}."
  [mission->patterns pattern->constellation constellation->model-uncertainty]
  (let [pattern->constellation (into {} (map (fn [[pattern constellation]]
                                               [(pattern-stem pattern) constellation]))
                                     pattern->constellation)]
    (into (sorted-map)
          (map (fn [[mission patterns]]
                 (let [constellations (set (keep #(get pattern->constellation
                                                        (pattern-stem %))
                                                   patterns))]
                   [(normalize-mission-id mission)
                    (reduce + 0.0 (map #(double (get constellation->model-uncertainty % 0.0))
                                       constellations))])))
          mission->patterns)))

(defn- uri-token
  [s]
  (URLEncoder/encode (str s) "UTF-8"))

(defn concept-star-id
  [concept]
  (str "a4a-bmr/capability-star/" (uri-token concept)))

(defn concept->star-doc
  "Build the Seam-1 capability-star identity doc for a demo-validated concept.

   A4a owns identity only; it does not write capability-star status."
  [concept]
  {:xt/id (concept-star-id concept)
   :entity/type :capability-star
   :star/capability concept
   :star/updated-by :a4a-bmr})
