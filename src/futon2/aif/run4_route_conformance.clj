(ns futon2.aif.run4-route-conformance
  "Pure classifier shared by RUN4 consumers. Semantics are the U49
  transcription rules: code-retired measured pairs are dependency exclusions,
  other code-retired pairs refutations, ruling-retired pairs unrealised."
  (:require [clojure.string :as str]))

(defn- node [x]
  (cond (keyword? x) (name x)
        (symbol? x) (name x)
        (and (string? x) (not (str/blank? x))) x
        :else nil))

(defn- edge-pair [e]
  (let [a (node (:from e)) b (node (:to e))]
    (when (and a b) [a b])))

(defn index [control-map]
  (when-not (and (map? control-map) (vector? (:edges control-map))
                 (vector? (:route-measured-drawn control-map))
                 (map? (:decisions control-map)))
    (throw (ex-info "Invalid RUN4 control map" {:reason :invalid-control-map})))
  (let [drawn (mapv edge-pair (:edges control-map))
        measured (mapv edge-pair (:route-measured-drawn control-map))]
    (when (or (some nil? drawn) (some nil? measured))
      (throw (ex-info "Invalid RUN4 control-map edge" {:reason :invalid-control-map})))
    {:drawn (set drawn) :measured (set measured)
     :retired
     (reduce (fn [acc [_ decision]]
               (reduce (fn [a pair]
                         (let [p (mapv node pair)]
                           (when-not (and (= 2 (count p)) (every? some? p))
                             (throw (ex-info "Invalid retired pair"
                                             {:reason :invalid-control-map})))
                           (update a p (fnil conj #{}) (:grounds decision))))
                       acc (:retires decision)))
             {} (:decisions control-map))}))

(defn classify [idx hop]
  (let [grounds ((:retired idx) hop)]
    (cond
      (and grounds (:code grounds) ((:measured idx) hop)) :excluded-dependency-grain
      (and grounds (:code grounds)) :refutation
      (and grounds (:ruling grounds)) :ruling-unrealised
      ((:drawn idx) hop) :drawn
      ((:measured idx) hop) :route-measured
      :else :unmapped)))

(defn run-record-route
  "Validate the full-loop edge carrier and recover the lossless node route."
  [run-record]
  (let [edges (:route run-record)]
    (when-not (and (vector? edges) (seq edges))
      (throw (ex-info "Empty RUN4 route" {:reason :empty-route})))
    (let [pairs (mapv (fn [e]
                        (when-not (and (map? e) (node (:fromNode e)) (node (:toNode e))
                                       (string? (:via e)) (not (str/blank? (:via e))))
                          (throw (ex-info "Malformed RUN4 route edge"
                                          {:reason :malformed-route-edge})))
                        [(node (:fromNode e)) (node (:toNode e))]) edges)]
      (when-not (every? true? (map (fn [[a b]] (= (second a) (first b)))
                                   (partition 2 1 pairs)))
        (throw (ex-info "Discontinuous RUN4 route" {:reason :discontinuous-route})))
      {:nodes (vec (cons (ffirst pairs) (map second pairs))) :hops pairs})))

(defn verdict [control-map run-record]
  (let [idx (index control-map)
        {:keys [nodes hops]} (run-record-route run-record)
        classified (mapv (fn [hop] {:hop hop :class (classify idx hop)}) hops)
        bad #(filterv (fn [x] (= % (:class x))) classified)]
    {:route-nodes nodes :hops classified
     :unmapped-hops (bad :unmapped) :refutations (bad :refutation)
     :conforms? (and (seq hops) (empty? (bad :unmapped)) (empty? (bad :refutation)))}))

(defn conforms-routes? [control-map routes]
  (let [idx (index control-map)
        hops (mapcat #(mapv vec (partition 2 1 %)) routes)]
    (and (seq routes) (every? seq routes)
         (not-any? #(= :unmapped (classify idx %)) hops)
         (not-any? #(= :refutation (classify idx %)) hops))))
