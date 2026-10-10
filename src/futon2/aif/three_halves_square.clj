(ns futon2.aif.three-halves-square
  "Decidable runtime projection of one ThreeHalvesBlend.Square."
  (:require [clojure.string :as str]))

(def schema :wm/three-halves-square-v1)
(def roles [:G :I1 :I2 :B])
(def map-ends {:a1 [:G :I1] :a2 [:G :I2] :b1 [:I1 :B] :b2 [:I2 :B]})

(defn- theory-valid? [{:keys [elems names axioms]}]
  (and (set? elems) (seq elems) (set? axioms)
       (map? names) (= elems (set (keys names)))
       (every? #(not (str/blank? (str %))) (vals names))
       (every? #(and (vector? %) (= 3 (count %)) (every? elems %)) axioms)))

(defn- pinned-object? [{:keys [text source theory]}]
  (and (not (str/blank? (str text)))
       (string? (:path source)) (not (str/blank? (:path source)))
       (string? (:sha256 source)) (re-matches #"[0-9a-f]{64}" (:sha256 source))
       (theory-valid? theory)))

(defn- functional? [rel]
  (every? (fn [[_ rows]] (= 1 (count (set (map second rows)))))
          (group-by first rel)))

(defn- map-valid? [objects map-id rel]
  (let [[from to] (map-ends map-id)
        domain (get-in objects [from :theory :elems])
        codomain (get-in objects [to :theory :elems])]
    (and (set? rel) (functional? rel)
         (every? (fn [pair]
                   (and (vector? pair) (= 2 (count pair))
                        (contains? domain (first pair))
                        (contains? codomain (second pair))))
                 rel))))

(defn- compose [left right]
  (set (for [[a b] left [b' c] right :when (= b b')] [a c])))

(defn validate [receipt]
  (let [objects (:objects receipt)
        maps (:maps receipt)
        aux (:auxiliary receipt)
        object-gap (vec (for [role roles :when (not (pinned-object? (get objects role)))] role))
        map-gap (vec (for [m (keys map-ends) :when (not (map-valid? objects m (get maps m)))] m))
        route1 (when (empty? map-gap) (compose (:a1 maps) (:b1 maps)))
        route2 (when (empty? map-gap) (compose (:a2 maps) (:b2 maps)))
        required-commutes? (or (:b1 aux) (:b2 aux) (= route1 route2))
        active-routes (concat (when-not (:b1 aux) route1)
                              (when-not (:b2 aux) route2))
        consistent? (and (empty? map-gap) (functional? active-routes))
        gaps (cond-> []
               (not= schema (:schema receipt)) (conj :square-schema)
               (seq object-gap) (conj {:kind :object-pin-or-theory :roles object-gap})
               (seq map-gap) (conj {:kind :map-direction-or-functionality :maps map-gap})
               (and (empty? map-gap) (not required-commutes?)) (conj :noncommutation)
               (and (empty? map-gap) (not consistent?)) (conj :inconsistency))]
    (if (seq gaps)
      {:status :refused :kind :three-halves-square-invalid :missing-evidence gaps}
      (assoc receipt :status :valid))))

(defn valid? [receipt] (= :valid (:status (validate receipt))))
