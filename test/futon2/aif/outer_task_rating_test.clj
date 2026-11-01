(ns futon2.aif.outer-task-rating-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.outer-select :as select]
            [futon2.aif.outer-score :as score]))

(def fixture (edn/read-string (slurp "test/fixtures/l5-19-click-1-rating.edn")))
(def input
  (merge (:state fixture)
         {:focus (:focus fixture) :facts (:facts fixture)
          ;; The old field did not retain relations for unpicked tasks.
          ;; Historical replay must not manufacture them from current sources.
          :relation (fn [_ t] (get (:relations fixture) t :unknown))
          :field (mapv (fn [[t f]] {:target t :reach (get-in f [:reach :value])})
                       (:facts fixture))}))
(defn near? [a b] (< (Math/abs (- (double a) (double b))) 1.0e-9))

(deftest retained-picks-and-stopping-share-the-real-scorer
  (let [rows (select/per-task-ratings input) by-id (into {} (map (juxt :target identity)) rows)]
    (is (= 591 (count rows)))
    (doseq [c (:cascades fixture)]
      (is (near? (:G c) (:G (by-id (:pick c)))))))
  (is (near? (get-in fixture [:abstention :G]) (:G (score/abstention-score input))))
  (let [r (first (filter #(= "M-interim-director" (:target %)) (select/per-task-ratings input)))]
    (is (= (get-in fixture [:abstention :terms :altitude]) (get-in r [:terms :altitude])))))

(deftest unknown-relation-is-not-unrelated
  (let [r (first (select/per-task-ratings (assoc input :relation (fn [_ _] :unknown))))]
    (is (= :unknown (get-in r [:relation :class])))
    (is (near? (:G r) (:G (score/abstention-score input))))))

(deftest top-ten-is-a-diagnostic-order-with-retained-ranks
  (let [rows (select/per-task-ratings input) top (select/rating-top-ten rows)]
    (is (= 10 (count top)))
    (is (apply <= (map :G top)))
    (is (every? #(= (:rank %) (get-in fixture [:facts (:target %) :rank])) top))))

(deftest unformable-model-stays-in-the-field-as-typed-absence
  (let [bad (assoc-in input [:field 0 :reach] nil)
        rows (select/per-task-ratings bad)]
    (is (= 591 (count rows)))
    (is (= :reach-unavailable (get-in rows [0 :absence :kind])))
    (is (not (contains? (first rows) :G)))
    (is (= 10 (count (select/rating-top-ten rows))))))

(deftest diagnostic-fact-failure-does-not-refuse-selection
  (let [rows (select/per-task-ratings
              (assoc input :facts (fn [_] (throw (ex-info "missing" {:kind :fact-unavailable})))))]
    (is (= 591 (count rows)))
    (is (every? #(= :fact-unavailable (get-in % [:absence :kind])) rows))))
