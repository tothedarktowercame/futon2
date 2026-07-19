(ns capability-zones-live-map
  "Build the read-only live-map projection of the accepted PCA-3 candidate."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.capability-zones :as zones]))

(def reduction-path "data/capability_zones/reduction-pca3-v1.json")
(def seeds-path "resources/capability_zones/action_class_seeds.json")
(def harvest-path "data/capability_zones/harvest-2026-07-19-3d.edn")
(def embeddings-path "/home/joe/code/data/notions/bge_mission_embeddings.json")
(def positions-path "/home/joe/code/futon6/data/mission-carpet-pos-embed.json")
(def output-path "/home/joe/code/futon3c/resources/capability_zones/live-map-pca3-v1.json")

;; A fixed categorical display palette, not a model parameter. Shapes and
;; line styles carry mixed/disagreement status so colour is never the only cue.
(def class-colors
  {:no-op "#4c78a8" :address-sorry "#f58518" :open-mission "#e45756"
   :advance-mission "#72b7b2" :close "#54a24b" :close-mission "#eeca3b"
   :close-hole "#b279a2" :survey "#ff9da6" :survey-mission "#9d755d"
   :apply-cascade "#bab0ac" :fire-pattern "#00a6d6"
   :learn-action-class "#ff6f91" :pursue "#6f4e7c" :decompose "#2ca02c"})

(defn- census [items]
  (frequencies (map :class items)))

(defn- mean-vector [vectors]
  (let [n (double (count vectors))]
    (apply mapv (fn [& xs] (/ (reduce + xs) n)) vectors)))

(defn -main [& _]
  (let [reduction (json/parse-string (slurp reduction-path) true)
        seeds (json/parse-string (slurp seeds-path) true)
        harvest (edn/read-string (slurp harvest-path))
        threshold (get-in harvest [:resistance :global-threshold])
        embeddings (json/parse-string (slurp embeddings-path) true)
        positions (json/parse-string (slurp positions-path) true)
        seeds3 (zones/seeds-3d reduction seeds)
        high-d-seeds (mapv (fn [seed]
                             (let [{:keys [class vector]} (zones/operative-seed seed)]
                               {:class (name class) :vector vector}))
                           seeds)
        mission-groups (->> embeddings (group-by :basename) (sort-by key))
        items
        (->> mission-groups
             (keep (fn [[mission-id missions]]
                     (let [vector (mean-vector (mapv :vector missions))
                           position (get positions (keyword mission-id))]
                       (when position
                         (let [zone3 (zones/zone-of-3d reduction seeds3 vector)
                               high (binding [zones/*seed-records* high-d-seeds
                                              zones/*seed-generation* :centroid]
                                      (zones/zone-of vector))
                               mixed? (< (:margin zone3) threshold)]
                           {:mission-id mission-id
                            :source-ids (mapv :id missions)
                            :repos (str/join "," (sort (distinct (map :home_repo missions))))
                            :embedding-record-count (count missions)
                            :x (first position) :y (second position)
                            :class (:class zone3) :margin (:margin zone3)
                            :distance (:distance zone3) :runner-up (:runner-up zone3)
                            :mixed? mixed?
                            :high-d-class (:class high)
                            :high-d-margin (:margin high)
                            :disagreement? (not= (:class zone3) (:class high))})))))
             (sort-by :mission-id)
             vec)
        counts (census items)
        legend (mapv (fn [class]
                       {:class class :color (get class-colors class)
                        :mission-count (get counts class 0)})
                     (map (comp keyword :class) seeds))
        artifact {:schema "capability-zones-live-map.v1"
                  :reduction-version (:version reduction)
                  :metric :interim-metric-3d
                  :projection-note "Zone identity is computed in operative PCA-3; x/y are display-only carpet coordinates."
                  :global-margin-threshold threshold
                  :summary {:embedded-missions (count embeddings)
                            :unique-embedded-missions (count mission-groups)
                            :positioned-missions (count items)
                            :positioned-embedding-records
                            (reduce + (map :embedding-record-count items))
                            :collapsed-duplicate-records
                            (reduce + (map #(dec (:embedding-record-count %)) items))
                            :mixed (count (filter :mixed? items))
                            :high-d-disagreements (count (filter :disagreement? items))}
                  :legend legend :items items}]
    (io/make-parents output-path)
    (spit output-path (str (json/generate-string artifact) "\n"))
    (println (pr-str (:summary artifact)))))

(apply -main *command-line-args*)
