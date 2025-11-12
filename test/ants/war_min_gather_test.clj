(ns ants.war-min-gather-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.war :as war]))

(def gather-food* (deref (resolve 'ants.war/gather-food)))

(defn- setup-world
  []
  (let [world (war/new-world {:size [5 5]
                              :ants-per-side 1})
        ant-id :aif-0
        loc [1 1]
        orig (get-in world [:ants ant-id :loc])]
    (-> world
        (assoc-in [:grid :cells orig :ant] nil)
        (assoc-in [:grid :cells loc :ant] ant-id)
        (assoc-in [:ants ant-id :loc] loc)
        (assoc-in [:grid :cells loc :home] nil)
        (assoc-in [:grid :cells loc :food] 0.6)
        (assoc-in [:grid :cells orig :food] 0.6))))

(deftest gather-skips-white-cells
  (let [world (setup-world)
        ant (get-in world [:ants :aif-0])
        white-world (assoc-in world [:grid :cells (:loc ant) :food] 0.05)
        white-result (gather-food* white-world ant)]
    (testing "skip gather when food is negligible"
      (is (zero? (:gather white-result)))
      (is (= (:cargo ant) (:cargo (:ant white-result)))))
    (let [rich-world (assoc-in world [:grid :cells (:loc ant) :food] 0.6)
          rich-result (gather-food* rich-world ant)]
      (testing "gather proceeds when food is ample"
        (is (pos? (:gather rich-result)))
        (is (> (:cargo (:ant rich-result)) (:cargo ant)))))))
