(ns futon2.aif.wm.pattern-graph-view-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.wm.pattern-graph-view :as graph-view]))

(def graph-path "test/fixtures/pattern-graph-view/c9d25d6a-graph-extract.json")
(def expected-path "test/fixtures/pattern-graph-view/c9d25d6a.expected-view.edn")
(def action-path
  "test/fixtures/selected-want-outcome/2026-10-05-c9d25d6a-action.edn")

(defn- action [] (:action (edn/read-string (slurp action-path))))
(defn- pattern-ids []
  (mapv (fn [step] (str (namespace (:id step)) "/" (name (:id step))))
        (:precedence (action))))

(deftest recorded-cascade-matches-the-reviewed-view
  (let [graph (:graph (graph-pin/load-unpinned graph-path))
        expected (:expected (edn/read-string (slurp expected-path)))]
    (is (= expected (graph-view/view graph (pattern-ids))))))

(deftest for-action-retains-the-graph-byte-authority
  (let [loaded (graph-pin/load-unpinned graph-path)
        result (graph-view/for-action graph-path (action))]
    (is (= (:expected (edn/read-string (slurp expected-path)))
           (dissoc result :graph)))
    (is (= (:graph-ref loaded) (:graph result)))))

(deftest unknown-and-isolated-chosen-patterns-are-not-seeds
  (let [graph (:graph (graph-pin/load-unpinned graph-path))
        result (graph-view/view graph ["missing/pattern" "ukrns/reader-run-path"])]
    (is (= ["missing/pattern"] (:unknown result)))
    (is (= ["ukrns/reader-run-path"] (:without-links result)))
    (is (= [] (:seeds result)))
    (is (= [{:kind :fewer-than-two-linked-patterns :seeds []}]
           (:failures result)))))

(deftest one-linked-pattern-has-a-typed-fewer-than-two-failure
  (let [graph (:graph (graph-pin/load-unpinned graph-path))
        result (graph-view/view graph ["war-machine/state-capture"])]
    (is (= ["war-machine/state-capture"] (:seeds result)))
    (is (= [] (:connecting-cascades result)))
    (is (= [{:kind :fewer-than-two-linked-patterns
             :seeds ["war-machine/state-capture"]}]
           (:failures result)))))

(deftest a-repeated-pattern-is-one-seed-and-no-pair-with-itself
  (let [graph (:graph (graph-pin/load-unpinned graph-path))
        result (graph-view/view graph ["war-machine/state-capture"
                                       "war-machine/state-capture"
                                       "ukrns/reader-run-path"])]
    (is (= 3 (count (:patterns result))) "every position is still listed")
    (is (= [[1 3] [2 3]] (mapv :positions (:pairs result))))
    (is (= ["war-machine/state-capture"] (:seeds result)))
    (is (= ["ukrns/reader-run-path"] (:without-links result)))
    (is (= [] (:connecting-cascades result)))
    (is (= [{:kind :fewer-than-two-linked-patterns
             :seeds ["war-machine/state-capture"]}]
           (:failures result)))))

(deftest missing-graph-is-a-typed-absence
  (is (= {:status :absent :reason :graph-unreadable
          :graph {:path "test/fixtures/pattern-graph-view/missing.json"}}
         (graph-view/for-action "test/fixtures/pattern-graph-view/missing.json"
                                (action)))))
