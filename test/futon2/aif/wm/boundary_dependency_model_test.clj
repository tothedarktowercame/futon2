(ns futon2.aif.wm.boundary-dependency-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.boundary-dependency-model :as model]))

(def facts-path "holes/labs/wm-contract/logic-model/p32/boundary-facts.edn")

(defn fact [facts id]
  (first (filter #(= id (:id %)) facts)))

(defn replace-fact [facts id f]
  (mapv #(if (= id (:id %)) (f %) %) facts))

(deftest source-pinned-facts-distinguish-blocking-cycles-from-refused-feedback
  (let [facts (model/load-facts facts-path)
        cycles (model/selection-preparation-cycles facts)
        feedback (model/measured-preparation-feedback facts)]
    (is (= #{[:c1-reading-selection-historical :p1-reading-preparation-historical]
             [:c2-square-selection :p2-square-authoring]}
           (set (map (juxt :consumer :producer) cycles))))
    (is (every? #(and (= :present (:status %))
                      (= 2 (count (:source-pins %)))) cycles))
    (is (= [{:kind :measured-preparation-feedback :status :present
             :consumer :c1-reading-selection-current
             :producer :p1-reading-preparation-current
             :input :target-reading}]
           feedback))))

(deftest source-pin-validation-rejects-prose-only-pins
  (let [facts (model/load-facts facts-path)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"resolvable source pin"
                          (model/validate-facts!
                           (replace-fact facts :c2-square-selection
                                         #(assoc % :source-pin "consumers row 26")))))))

(deftest constructed-bad-case-detects-missing-producer-and-keeps-unknown-unknown
  (let [facts (model/load-facts facts-path)
        without-reading-producer (vec (remove #(= :target-reading (:output %)) facts))
        findings (model/unproduced-inputs without-reading-producer)]
    (testing "the constructed missing-reading producer is absent, not a zero reading"
      (is (= :missing (:status (first (filter #(= :c1-reading-selection-historical (:consumer %)) findings))))))
    (testing "an unknown retention fact remains unknown"
      (is (= :unknown (:status (first (filter #(= :r6-prospective-carry-retention (:edge %))
                                               (model/lost-retention-edges facts)))))))))

(deftest corrected-retention-clears-only-its-repaired-edge
  (let [facts (model/load-facts facts-path)
        bad-facts (-> facts
                      (replace-fact :r6-selected-frontier-retained #(assoc % :status :absent))
                      (replace-fact :r6-prospective-carry-retention #(assoc % :status :absent)))
        corrected (replace-fact bad-facts :r6-selected-frontier-retained #(assoc % :status :present))]
    (is (= #{:r6-selected-frontier-retained
             :r6-abstained-frontier-retention-historical
             :r6-prospective-carry-retention}
           (set (map :edge (model/lost-retention-edges bad-facts)))))
    (is (= #{:r6-abstained-frontier-retention-historical
             :r6-prospective-carry-retention}
           (set (map :edge (model/lost-retention-edges corrected)))))))

(deftest unknown-potentially-covering-producer-is-not-erased
  (is (= :unknown
         (model/producer-status {:scope :eligible-targets}
                                [{:status :present :scope :selected-target}
                                 {:status :unknown :scope :all}]))))
