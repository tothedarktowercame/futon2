(ns futon2.aif.selection-order-incident-test
  "Executable negative cases for INCIDENT-2026-09-30-one-policy-selection-
   repeated-refusal.  These tests state Joe's select-then-interpret order.

   Both tests are tagged :incident, which the normal green-suite selection
   excludes.  Run this open incident contract explicitly with:
     clojure -M:test -m cognitect.test-runner -n futon2.aif.selection-order-incident-test -i :incident"
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.construction-inputs :as inputs]
            [futon2.aif.ticket-queue :as ticket-queue])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))


(def targets (mapv #(str "M-selection-order-" %) (range 5)))

(def library-slice
  [{:pattern :library/inspect-evidence :rank 1
    :provenance {:retriever :embedding :query "close the target"}}
   {:pattern :library/repair-smallest-gap :rank 2
    :provenance {:retriever :tier0 :query "close the target"}}
   {:pattern :library/verify-result :rank 3
    :provenance {:retriever :embedding :query "close the target"}}])

(defn- pinned-graph
  "A real pinned graph naming the slice's three patterns, so the field forms
  and the refusal is the incident's own (no reading mints a policy), not a
  missing graph path."
  [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 0 :patterns 3
                 :pattern_ids (mapv #(subs (str (:pattern %)) 1) library-slice)
                 :summary [{:through "why" :edges 0} {:through "how" :edges 0}]
                 :edges []}))
    (graph-pin/pin! file)
    file))

(defn fixture-input
  "Five open targets with query-time library candidates and deliberately no
   published target-specific interpretation.  :query-time-slices is the input
   the incident says must enter construction and assembly must retain it."
  []
  {:targets targets
   :sources
   {:universes (into {} (map (fn [t] [t {:work/open true :work/closed false}]) targets))
    :wants (into {} (map (fn [t] [t [:work/closed]]) targets))
    :locators (into {}
                    (map (fn [t]
                           [t {:work/open {:class :C4 :fixture true}
                               :work/closed {:class :C4 :fixture true}}])
                         targets))
    :interpretations (into {} (map (fn [t] [t {:patterns {} :receipts {}}]) targets))
    :query-time-slices
    (into {} (map (fn [t]
                    [t {:schema :wm/query-time-library-slice-v1
                        :target t
                        :query "close the target"
                        :candidates library-slice
                        :failures []
                        :slice-size (count library-slice)
                        :library-size 100}])
                  targets))
    :horizon-steps 2
    :beta-by-context {:WM {:beta 1}}
    :context-of (constantly :WM)}})

(defn run-fixture []
  (let [assembled (inputs/assemble-cascade-problems (fixture-input))]
    {:assembled assembled
     :result (decision/cascade-decision
              assembled
              {;; Since a04b157d2 the caller supplies one selected task and
               ;; the field is that task's reading-policy family. All five
               ;; incident targets remain assembled above.
               :task {:id (first targets)}
               :pattern-graph-path (str (pinned-graph (temp-dir "selection-order-graph-")))
               :target-reading-root (str (temp-dir "selection-order-readings-"))
               :ticket-queue ticket-queue/empty-declaration})}))

(deftest ^:incident query-slices-do-not-bypass-construction-admission
  (let [{:keys [result]} (run-fixture)
        scored (get-in result [:decision :selection-certificate :candidates])]
    (is (empty? scored))
    (is (= :no-acting-cascade-candidate (get-in result [:decision :reason])))
    ;; a04b157d2: only the supplied task is in the field, refused typed.
    (is (= #{(first targets)} (set (map :target (get-in result [:decision :refusals])))))
    (is (every? #(= :no-policy-family (:kind %)) (get-in result [:decision :refusals])))))

(deftest ^:incident uninterpreted-pattern-remains-a-typed-construction-debt
  (let [{:keys [assembled result]} (run-fixture)
        chosen (get-in result [:decision :action])
        owed (get-in result [:decision :selection-certificate :interpretations-owed])]
    (testing "query retrieval is retained but cannot mint an acting policy"
      (is (nil? chosen))
      (is (empty? (:refusals assembled)))
      (is (every? empty?
                  (map :pool (get-in result [:decision :selection-certificate
                                             :target-construction])))))
    (testing "no post-selection debt is fabricated when selection never occurred"
      (is (nil? owed)))))
