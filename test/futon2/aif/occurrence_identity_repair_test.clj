(ns futon2.aif.occurrence-identity-repair-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.target-policy-family :as family]
            [futon2.aif.wm.family-selection :as selection]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.selection-cascade-identity :as identity]
            [futon2.aif.decision-gate :as gate]))
(defn frozen [] (edn/read-string (slurp (io/resource "fixtures/occurrence-identity/pattern-mining.edn"))))
(defn rebuilt [r]
  (let [reading (:reading r)
        voids (set (keys (get-in r [:squares :voids])))
        a (#'family/without-void-citations (:analysis reading) voids)]
    (#'family/deduplicate (#'family/reading-policies "M-pattern-mining" a
                           (into {} (map (fn [[k v]] [k (:square v)])) (get-in r [:squares :entries]))))))
(deftest retained-three-policies-bind
  (let [r (frozen) ps (rebuilt r)
        rows (selection/score-family-field
              [{:status :computed :target-id "M-pattern-mining" :policies ps
                :provenance (select-keys (:reading r) [:analysis-digest :excerpt-digest])}])
        ranked (:ranked (selection/construct-ranked rows))
        bound (identity/bind-ranked ranked #(decision/occurrence-readings % (:reading-pin %)))]
    (is (= 3 (count ps)))
    (is (= 3 (count ranked)))
    (is (= 3 (count (:ranked bound))))
    (is (empty? (:refusals bound)))
    (is (every? (fn [entry] (seq (:cascade-digest entry))) (:ranked bound)))))
(deftest actual-prior-numbering-keeps-all-refusals
  (let [r (frozen)
        old (fn [a] (->> (:sentences a)
                         (mapcat (fn [s] (map #(assoc % :sentence-id (:id s)) (:fragments s))))
                         (sort-by (juxt :start :end :sentence-id)) vec))
        ps (with-redefs-fn {#'analysis/fragment-records old} #(rebuilt r))
        result (selection/select-over-families
                [{:status :computed :target-id "M-pattern-mining" :policies ps
                  :provenance (select-keys (:reading r) [:analysis-digest :excerpt-digest])}]
                {:bind-cascade-identity {:readings #(decision/occurrence-readings % (:reading-pin %))}})
        refusals (#'decision/family-refusals (:target-policy-families result))]
    (is (= :abstained (:status result)))
    (is (= 3 (count refusals)))
    (is (= (set (map :policy-id ps)) (set (map :policy-id refusals))))
    (is (every? #(= :graph-endpoint-foreign (:reason %)) refusals))
    (is (every? #(some (fn [x]
                        (and (= ["futon-theory/progress-signal" 5] (:source-occurrence-id x))
                             (= ["futon-theory/progress-signal" 6] (:arranged-occurrence-id x))))
                      (get-in % [:failures 0 :occurrence-index-evidence])) refusals))
    (is (= refusals (:refusals (gate/emit! {:status :abstained :refusals refusals}))))))
(deftest no-candidates-is-explicit
  (is (= :no-candidates (:kind (first (#'decision/family-refusals [{:target-id "none" :policies [] :failures []}]))))))
