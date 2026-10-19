(ns futon2.aif.wm.boundary-dependency-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.boundary-dependency-model :as model]))

(def facts-path "holes/labs/wm-contract/logic-model/p32/boundary-facts.edn")

(defn fact [facts id]
  (first (filter #(= id (:id %)) facts)))

(defn replace-fact [facts id f]
  (mapv #(if (= id (:id %)) (f %) %) facts))

(deftest source-pinned-facts-model-the-two-selection-preparation-cycles
  (let [facts (model/load-facts facts-path)
        cycles (model/selection-preparation-cycles facts)]
    (is (= #{[:c1-reading-selection :p1-reading-preparation]
             [:c2-square-selection :p2-square-authoring]}
           (set (map (juxt :consumer :producer) cycles))))
    (is (every? #(and (= :present (:status %))
                      (= 2 (count (:source-pins %)))) cycles))))

(deftest constructed-bad-case-detects-missing-producer-and-keeps-unknown-unknown
  (let [facts (model/load-facts facts-path)
        without-reading-producer (vec (remove #(= :p1-reading-preparation (:id %)) facts))
        findings (model/unproduced-inputs without-reading-producer)]
    (testing "the constructed missing-reading producer is absent, not a zero reading"
      (is (= :missing (:status (first (filter #(= :c1-reading-selection (:consumer %)) findings))))))
    (testing "an unknown retention fact remains unknown"
      (is (= :unknown (:status (first (model/lost-retention-edges facts))))))))

(deftest corrected-retention-clears-only-its-repaired-edge
  (let [facts (model/load-facts facts-path)
        bad-facts (-> facts
                      (replace-fact :r6-selection-frontier-retained #(assoc % :status :absent))
                      (replace-fact :r6-prospective-carry-retention #(assoc % :status :absent)))
        corrected (replace-fact bad-facts :r6-selection-frontier-retained #(assoc % :status :present))]
    (is (= #{:r6-selection-frontier-retained :r6-prospective-carry-retention}
           (set (map :edge (model/lost-retention-edges bad-facts)))))
    (is (= #{:r6-prospective-carry-retention}
           (set (map :edge (model/lost-retention-edges corrected)))))))
