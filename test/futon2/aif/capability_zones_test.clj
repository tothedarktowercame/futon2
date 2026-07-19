(ns futon2.aif.capability-zones-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.capability-zones :as zones]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.intrinsic-values :as iv]))

(deftest checked-seeds-cover-the-action-vocabulary
  (let [seeds (json/parse-string
               (slurp "resources/capability_zones/action_class_seeds.json") true)]
    (is (= fm/action-types (set (map (comp keyword :class) seeds))))
    (is (= 14 (count seeds)))
    (is (every? #(and (string? (:text %)) (not-empty (:text %))) seeds))))

(deftest zone-of-is-deterministic-and-reports-margin
  (let [file (java.io.File/createTempFile "capability-zone-seeds" ".json")]
    (try
      (spit file (json/generate-string
                  [{:class "survey" :text_seed [1.0 0.0]}
                   {:class "pursue" :text_seed [0.0 1.0]}
                   {:class "no-op" :text_seed [-1.0 0.0]}]))
      (binding [zones/*seeds-path* (.getPath file)
                zones/*seed-generation* :text]
        (let [result (zones/zone-of [0.8 0.6])]
          (is (= :survey (:class result)))
          (is (= :pursue (:runner-up result)))
          (is (< (Math/abs (- 0.2 (:margin result))) 1.0e-12))
          (is (= :interim-metric (:metric result)))
          (is (= :interim-metric (:metric (meta result))))))
      (finally (.delete file)))))

(deftest pca3-transform-membership-and-seed-generation
  (let [reduction {:version "pca3-v1" :mean [0.0 0.0 0.0 0.0]
                   :components [[1.0 0.0 0.0 0.0]
                                [0.0 1.0 0.0 0.0]
                                [0.0 0.0 1.0 0.0]]}
        vector [0.8 0.2 0.0 9.0]
        first-transform (zones/transform-3d reduction vector)
        second-transform (zones/transform-3d reduction vector)
        seeds [{:class "survey" :centroid_evidence_count 0
                :text_seed [1.0 0.0 0.0 0.0]
                :centroid_seed [9.0 9.0 9.0 9.0]}
               {:class "pursue" :centroid_evidence_count 3
                :text_seed [9.0 9.0 9.0 9.0]
                :centroid_seed [0.0 1.0 0.0 0.0]}]
        seeds3 (zones/seeds-3d reduction seeds)
        result (zones/zone-of-3d reduction seeds3 vector)]
    (is (= first-transform second-transform))
    (is (= [0.8 0.2 0.0] first-transform))
    (is (= [:text-seed :centroid-seed] (mapv :generation seeds3)))
    (is (= :survey (:class result)))
    (is (= :pursue (:runner-up result)))
    (is (< (Math/abs (- (:margin result)
                        (- (Math/sqrt 1.28) (Math/sqrt 0.08))))
           1.0e-12))
    (is (= "pca3-v1" (:reduction-version result)))
    (is (= :interim-metric-3d (:metric result)))
    (is (= {:metric :interim-metric-3d :reduction-version "pca3-v1"}
           (meta result)))))

(deftest harvest-records-rehydrate-with-non-default-credit
  (let [path (io/file "data/capability_zones/harvest-2026-07-19.edn")]
    (is (.isFile path) "full harvest artifact must be checked in")
    (let [artifact (edn/read-string (slurp path))
          records (:records artifact)
          original (iv/current)]
      (try
        (testing "records have the pure next-update-record shape"
          (is (seq records))
          (is (every? #(and (keyword? (:class %))
                            (string? (:as-of %))
                            (number? (:alpha-post %))
                            (number? (:beta-post %))
                            (vector? (:evidence-refs %)))
                      records)))
        (let [state (iv/rehydrate! records)
              observed (filter (fn [[_ entry]]
                                 (pos? (+ (:n-emissions entry)
                                          (:n-followthrough entry))))
                               state)
              non-default (filter (fn [[class _]]
                                    (not= 0.5 (iv/credit-for class)))
                                  observed)]
          (is (>= (count observed) 7))
          (is (>= (count non-default) 7)))
        (finally (reset! iv/state original))))))

(deftest pca3-harvest-records-rehydrate
  (let [artifact (edn/read-string
                  (slurp "data/capability_zones/harvest-2026-07-19-3d.edn"))
        records (:records artifact)
        original (iv/current)]
    (try
      (is (every? #(and (keyword? (:class %))
                        (string? (:as-of %))
                        (number? (:alpha-post %))
                        (number? (:beta-post %))
                        (number? (:intrinsic-value-post %))
                        (vector? (:evidence-refs %)))
                  records))
      (let [state (iv/rehydrate! records)]
        (is (= 6810 (get-in artifact [:summary :items])))
        (is (= 14 (get-in artifact [:summary :classes-with-evidence])))
        (is (= 14 (count state)))
        (is (pos? (get-in artifact [:summary :disagreements])))
        (is (every? #(not= 0.5 (iv/credit-for %)) (keys state))))
      (finally (reset! iv/state original)))))
