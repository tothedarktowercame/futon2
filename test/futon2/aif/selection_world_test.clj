(ns futon2.aif.selection-world-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.selection-world :as world]))

(def tasks {:missions ["M-a"] :excursions ["E-a"] :tickets ["T-a"]})
(def decision {:controller-ranking
               [{:action {:type :advance-mission :target "M-a"}}
                {:action {:type :advance-excursion :target "E-a"}}
                {:action {:type :advance-ticket :target "T-a"}}]})
(def opts {:world-task-sets-fn (fn [_] tasks)
           :world-pattern-manifest-fn (fn [_] {:file-count 2 :digest "patterns"})
           :world-now-fn (constantly "2026-09-30T00:00:00Z")})

(deftest snapshot-is-complete-and-deterministic
  (let [a (world/capture decision {:author {:type :codex}} nil opts)
        b (world/capture decision {:author {:type :codex}} nil opts)
        changed (world/capture decision {:author {:type :codex}} nil
                               (assoc opts :world-task-sets-fn
                                      (fn [_] (update tasks :missions conj "M-b"))))]
    (is (= 0 (:failure-count a)))
    (is (= 2 (get-in a [:enumerated-tasks :count])))
    (is (= (:selection-input-digest a) (:selection-input-digest b)))
    (is (not= (:selection-input-digest a) (:selection-input-digest changed)))))

(deftest snapshot-does-not-change-decision
  (let [before (pr-str decision)]
    (world/capture decision {} nil opts)
    (is (= before (pr-str decision)))))

(deftest failed-roster-is-counted
  (let [x (world/capture decision nil nil opts)]
    (is (= 1 (:failure-count x)))
    (is (= :seat-roster (get-in x [:failures 0 :part])))
    (is (= :failed (get-in x [:seat-roster :status])))))

(deftest wrong-enumerator-is-detectable
  (let [x (world/capture decision {} nil opts)]
    ;; Plant: the decision enumerator has no excursion candidate type. Exact
    ;; set comparison exposes the mismatch rather than accepting equal counts.
    (is (not= (set (mapcat :ids (vals (:open-tasks x))))
              (set (get-in x [:enumerated-tasks :ids]))))
    (is (not= #{"M-wrong"}
              (set (get-in x [:enumerated-tasks :ids]))))))
