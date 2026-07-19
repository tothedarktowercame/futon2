(ns capability-zones-reassign-3d
  "Pure S1.5 PCA-3 reassignment artifact builder; never calls persistence."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.capability-zones :as zones]
            [futon2.aif.intrinsic-values :as iv])
  (:import (java.time YearMonth)))

(def prepared-path "data/capability_zones/harvest-prepared.json")
(def s1-path "data/capability_zones/harvest-2026-07-19.edn")
(def reduction-path "data/capability_zones/reduction-pca3-v1.json")
(def seeds-path "resources/capability_zones/action_class_seeds.json")
(def output-path "data/capability_zones/harvest-2026-07-19-3d.edn")

(defn- quantile [sorted-values q]
  (let [n (count sorted-values)
        index (long (Math/floor (* q (dec n))))]
    (nth sorted-values index)))

(defn- deciles [values]
  (let [s (vec (sort values))]
    (into (sorted-map)
          (for [i (range 11)]
            [(keyword (str "p" (* 10 i))) (quantile s (/ i 10.0))]))))

(defn- record-entry [record]
  {:alpha (:alpha-post record) :beta (:beta-post record)
   :intrinsic-value (:intrinsic-value-post record)
   :n-emissions (:n-emissions-in-window record)
   :n-followthrough (:n-followthrough-in-window record)
   :as-of (:as-of record)})

(defn- update-records [assignments]
  (let [windows (sort-by key (group-by (juxt :month :class)
                                       (remove :resisted? assignments)))]
    (:records
     (reduce
      (fn [{:keys [priors records]} [[month class] items]]
        (let [prior (get priors class (iv/fresh-entry))
              emissions (count items)
              followthrough (count (filter :followthrough items))
              record (iv/next-update-record
                      class prior emissions followthrough
                      {:as-of (last (sort (map :committed_at items)))
                       :outer-loop-run-id (str "capability-zone-pca3-v1:" month)
                       :window-days (.lengthOfMonth (YearMonth/parse month))
                       :evidence-refs (mapv :id items)})]
          {:priors (assoc priors class (record-entry record))
           :records (conj records record)}))
      {:priors {} :records []} windows))))

(defn -main [& _]
  (let [prepared (json/parse-string (slurp prepared-path) true)
        reduction (json/parse-string (slurp reduction-path) true)
        seeds (json/parse-string (slurp seeds-path) true)
        seeds3 (zones/seeds-3d reduction seeds)
        s1 (edn/read-string (slurp s1-path))
        s1-by-id (into {} (map (juxt :id identity) (:assignments s1)))
        provisional
        (mapv (fn [item]
                (let [old (get s1-by-id (:id item))
                      zone (zones/zone-of-3d reduction seeds3 (:embedding item))]
                  (-> old
                      (assoc :s1-class (:class old) :s1-margin (:margin old)
                             :class (:class zone) :distance (:distance zone)
                             :margin (:margin zone) :runner-up (:runner-up zone)
                             :point-3d (:point zone)
                             :reduction-version (:reduction-version zone)
                             :metric (:metric zone))
                      (dissoc :cosine :resisted?))))
              (:items prepared))
        margin-deciles (deciles (map :margin provisional))
        threshold (:p10 margin-deciles)
        assignments (mapv #(assoc % :resisted? (< (:margin %) threshold)) provisional)
        disagreements (->> assignments
                           (filter #(not= (:s1-class %) (:class %)))
                           (mapv #(select-keys % [:id :repo :sha :subject
                                                  :s1-class :s1-margin :class
                                                  :margin :resisted?])))
        pair-counts (->> disagreements
                         (group-by (juxt :s1-class :class))
                         (map (fn [[[from to] xs]]
                                {:from from :to to :count (count xs)}))
                         (sort-by (comp - :count)) vec)
        records (update-records assignments)
        artifact
        {:schema :capability-zone-harvest-3d.v1
         :generated-at "2026-07-19T00:00:00Z"
         :partition {:reduction-version "pca3-v1"
                     :metric :interim-metric-3d
                     :membership :nearest-euclidean-seed
                     :seeds (mapv #(select-keys % [:class :generation :point]) seeds3)}
         :resistance {:global-threshold threshold
                      :derivation :empirical-p10-of-all-3d-margins
                      :rationale "The lowest global decile is marked mixed: this exposes the thinnest relative Voronoi boundaries while avoiding per-class tuning."
                      :margin-deciles margin-deciles}
         :observation-semantics (:observation-semantics s1)
         :summary {:items (count assignments)
                   :assigned (count (remove :resisted? assignments))
                   :resisted (count (filter :resisted? assignments))
                   :records (count records)
                   :classes-with-evidence (count (set (map :class
                                                           (remove :resisted? assignments))))
                   :disagreements (count disagreements)}
         :disagreement {:items disagreements :class-pair-counts pair-counts}
         :records records :assignments assignments}]
    (io/make-parents output-path)
    (spit output-path (str (pr-str artifact) "\n"))
    (println (pr-str (:summary artifact)))
    (println (pr-str (:resistance artifact)))))

(apply -main *command-line-args*)
