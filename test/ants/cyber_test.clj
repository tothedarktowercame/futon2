(ns ants.cyber-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.cyber :as cyber]))

(deftest default-pattern-listed
  (let [ids (set (map :id (cyber/available-patterns)))]
    (is (contains? ids cyber/default-pattern-id))))

(deftest attach-config-populates-metadata
  (let [ant {:species :cyber}
        updated (cyber/attach-config ant cyber/default-pattern-id)]
    (is (map? (:aif-config updated)))
    (is (map? (:cyber-pattern updated)))
    (is (= cyber/default-pattern-id (get-in updated [:cyber-pattern :id])))))

(deftest attach-config-falls-back-to-default
  (let [ant {:species :cyber}
        updated (cyber/attach-config ant ::missing-pattern)
        fallback (cyber/cyber-config cyber/default-pattern-id)]
    (is (= (:id fallback) (get-in updated [:cyber-pattern :id])))))

(deftest describe-pattern-smoke
  (let [desc (cyber/describe-pattern cyber/default-pattern-id)]
    (testing "shape"
      (is (= cyber/default-pattern-id (:pattern desc)))
      (is (string? (:title desc)))
      (is (string? (:summary desc))))
    (when-let [excerpt (:excerpt desc)]
      (is (string? excerpt)))))
