(ns futon2.aif.target-policy-family-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.target-policy-family :as sut]
            [futon2.aif.target-reading-registry :as registry]))

(defn pattern-ref [id rationale]
  {:status "candidate" :id id :rationale rationale
   :source_sha256 (apply str (repeat 64 "a"))})

(defn analysis-map
  ([] (analysis-map "first rationale"))
  ([rationale]
   {:status "analyzed"
    :sentences [{:id "s1" :fragments [{:start 0 :end 1 :text "a" :intent "act"
                                        :target "x" :rationale "a" :relations ["context"]
                                        :pattern_refs [(pattern-ref "p/a" rationale)]
                                        :pattern_rejections []}]}
                {:id "s2" :fragments [{:start 2 :end 3 :text "b" :intent "act"
                                        :target "x" :rationale "b" :relations ["goal"]
                                        :pattern_refs [(pattern-ref "p/b" "second rationale")]
                                        :pattern_rejections []}]}]}))

(defn reading [analysis]
  {:status :current-candidate :target-id "M-test"
   :excerpt-digest (apply str (repeat 64 "1"))
   :analysis-digest (identity/sha256 (.getBytes (pr-str analysis) "UTF-8"))
   :analysis analysis})

(def graph
  {:pattern-ids ["p/a" "p/b" "p/c"] :nodes ["p/a" "p/b" "p/c"]
   :edges [{:a "p/a" :b "p/b" :kind "why" :weight 1 :evidence []}
           {:a "p/b" :b "p/c" :kind "why" :weight 1 :evidence []}]})

(defn- retraction-policy [analysis graph]
  (first (filter #(= :retraction (:kind %))
                 (:policies (sut/policy-family {:reading (reading analysis)
                                                :graph graph})))))

(defn- roots [policy]
  (let [co (get-in (shape-g/arranged->candidate
                    (:target policy) (:policy-id policy) (:cascade policy))
                   [:precedence :co-apply])]
    (vec (remove (set (map second (:descent co))) (:units co)))))

(deftest authored-retraction-direction-defines-descent
  (let [g {:pattern-ids ["p/a" "p/b"] :nodes ["p/a" "p/b"]
           :edges [{:a "p/a" :b "p/b" :kind "why" :weight 1
                    :evidence [{:file "/library/p/b.flexiarg"}]}]}
        policy (retraction-policy (analysis-map) g)]
    (is (= [{:from "p/b" :to "p/a" :kind :precedes
             :kinds ["why"] :kind-used "why"
             :evidence [{:file "/library/p/b.flexiarg"}]
             :authored-direction {:from "p/b" :to "p/a"}}]
           (get-in policy [:cascade :edges])))
    (is (= ["p/b" "p/a"] (get-in policy [:cascade :precedence])))
    (is (= ["p/b"] (roots policy)))
    (is (= :reading-order-then-target-stable-hash
           (get-in policy [:cascade :unit-order-rule])))))

(deftest unauthored-retraction-relation-is-overlap
  (let [g {:pattern-ids ["p/a" "p/b"] :nodes ["p/a" "p/b"]
           :edges [{:a "p/a" :b "p/b" :kind "co-cited" :weight 2
                    :evidence [{:at "turn"}]}]}
        policy (retraction-policy (analysis-map) g)
        candidate (shape-g/arranged->candidate
                   (:target policy) (:policy-id policy) (:cascade policy))]
    (is (= :overlap (get-in policy [:cascade :edges 0 :kind])))
    (is (nil? (get-in policy [:cascade :edges 0 :authored-direction])))
    (is (empty? (get-in candidate [:precedence :co-apply :descent])))
    (is (= ["p/a" "p/b"] (roots policy)))))

(deftest renaming-does-not-turn-alphabet-into-arrangement
  (let [original-graph {:pattern-ids ["p/a" "p/b"] :nodes ["p/a" "p/b"]
                        :edges [{:a "p/a" :b "p/b" :kind "why" :weight 1
                                 :evidence [{:file "/library/p/b.flexiarg"}]}]}
        renamed-analysis
        {:status "analyzed"
         :sentences [{:id "s1" :fragments [{:start 0 :end 1 :text "z"
                                             :intent "act" :target "x" :rationale "a"
                                             :relations ["context"]
                                             :pattern_refs [(pattern-ref "p/z" "a")]}]}
                     {:id "s2" :fragments [{:start 2 :end 3 :text "a"
                                             :intent "act" :target "x" :rationale "b"
                                             :relations ["goal"]
                                             :pattern_refs [(pattern-ref "p/a" "b")]}]}]}
        renamed-graph {:pattern-ids ["p/a" "p/z"] :nodes ["p/a" "p/z"]
                       :edges [{:a "p/a" :b "p/z" :kind "why" :weight 1
                                :evidence [{:file "/library/p/a.flexiarg"}]}]}
        original (retraction-policy (analysis-map) original-graph)
        renamed (retraction-policy renamed-analysis renamed-graph)
        original-score (shape-g/score-policy original)
        renamed-score (shape-g/score-policy renamed)]
    (is (= ["p/b"] (roots original)))
    (is (= ["p/a"] (roots renamed)))
    (is (= (:g original-score) (:g renamed-score))
        "renaming a structurally identical authored edge does not change G")))

(deftest reading-and-retraction-form-one-family
  (let [result (sut/policy-family {:reading (reading (analysis-map)) :graph graph})]
    (is (= :computed (:status result)))
    (is (= 3 (:reported-count result)))
    (is (= 2 (:distinct-count result)))
    (is (= #{:reading-alternatives :retraction} (set (map :kind (:policies result)))))
    (is (every? :policy-id (:policies result)))
    (is (zero? (:failure-count result)))))

(deftest rationale-annotations-do-not-change-structural-count-or-id
  (let [one (sut/policy-family {:reading (reading (analysis-map "rationale one"))
                                :graph (assoc graph :edges [])})
        two (sut/policy-family {:reading (reading (analysis-map "different rationale"))
                                :graph (assoc graph :edges [])})]
    ;; Alternatives and overlap each report the same cascade; annotation text
    ;; changes between calls, but each family is one structural policy.
    (is (= 2 (:reported-count one) (:reported-count two)))
    (is (= 1 (:distinct-count one) (:distinct-count two)))
    (is (= (mapv :policy-id (:policies one)) (mapv :policy-id (:policies two))))))

(deftest absent-reading-preserves-the-failure
  (let [result (sut/policy-family
                {:reading {:status :absent :kind :stale-target-reading
                           :target-id "M-stale" :expected-digest "new"
                           :found-digests ["old"]}
                 :graph graph})]
    (is (= :failed (:status result)))
    (is (empty? (:policies result)))
    (is (= :stale-target-reading (get-in result [:failures 0 :kind])))
    (is (= 1 (:failure-count result)))))

(deftest isolated-seeds-keep-reading-policies-and-no-retraction
  (let [a (analysis-map)
        isolated {:pattern-ids ["p/a" "p/b" "p/x" "p/y"]
                  :nodes ["p/x" "p/y"]
                  :edges [{:a "p/x" :b "p/y" :kind "why" :weight 1 :evidence []}]}
        result (sut/policy-family {:reading (reading a) :graph isolated})]
    (is (= :computed (:status result)))
    (is (= 1 (:distinct-count result)))
    (is (= #{:reading-alternatives} (set (map :kind (:policies result)))))
    (is (= {:kind :isolated-seed :seeds ["p/a" "p/b"]} (get-in result [:failures 0])))
    (is (= :no-usable-retraction-seed (get-in result [:failures 1 :kind])))
    (is (= 2 (:failure-count result)))))

(deftest one-isolated-seed-does-not-remove-the-other-seeds-retractions
  ;; Bad case found on the real graph: four of seven targets lost every
  ;; retraction because a single reading pattern had no edges.
  (let [g {:pattern-ids ["p/a" "p/b" "p/c"] :nodes ["p/b" "p/c"]
           :edges [{:a "p/b" :b "p/c" :kind "why" :weight 1 :evidence []}]}
        result (sut/policy-family {:reading (reading (analysis-map)) :graph g})
        retractions (filter #(= :retraction (:kind %)) (:policies result))]
    (is (= [{:kind :isolated-seed :seeds ["p/a"]}] (:failures result)))
    (is (= ["p/b"] (get-in result [:provenance :retraction :seeds])))
    (is (seq retractions))
    (is (not-any? #(some #{"p/a"} (map (fn [n] (or (:pattern n) n))
                                       (get-in % [:cascade :nodes])))
                  retractions))))

(def lab-root "holes/labs/wm-contract/mission-head-cascades-2026-09-30")
(def graph-path "/home/joe/code/storage/operator-turns/mined-pattern-graph.json")

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "target-policy-family"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn digest [text] (identity/sha256 (.getBytes text "UTF-8")))

(deftest ^:slow seven-real-policy-families-report-current-graph-differences
  (let [dir (temp-dir) graph-copy (io/file dir "graph.json")
        _ (io/copy (io/file graph-path) graph-copy)
        _ (graph-pin/pin! graph-copy)
        graph (get-in (graph-pin/load-pinned graph-copy) [:graph])
        stems ["02-M-metric-harness" "03-M-distributed-proofreaders"
               "04-M-web-arxana-ui-improvements" "05-M-self-documenting-stack"
               "06-M-war-machine-aif-completion" "07-M-essays-diachronic-model"
               "08-M-value-creation-loop"]
        registry-root (io/file dir "registry")
        rows
        (mapv
         (fn [stem]
           (let [request (json/parse-string (slurp (io/file lab-root (str stem ".request.json"))) true)
                 a (json/parse-string
                    (slurp (io/file lab-root (str stem ".request.json.analysis.json"))) true)
                 target (get-in request [:task :target_id]) excerpt-digest (digest (:source_text request))
                 published (registry/publish! registry-root
                                              {:target-id target :source-path (get-in request [:task :file_path])
                                               :excerpt-digest excerpt-digest
                                               :source-file-digest (get-in request [:task :content_sha256])
                                               :request request :analysis a :validator-version 1})
                 current (when (= :current-candidate (:status published))
                           (registry/current-reading registry-root target excerpt-digest))
                 family (when current (sut/policy-family {:reading current :graph graph}))
                 old (json/parse-string (slurp (io/file lab-root (str stem ".retractions.json"))) true)
                 reading-reported (+ (count (:cascades (analysis/analysis->cascades a {:mode :alternatives})))
                                     (count (:cascades (analysis/analysis->cascades a {:mode :overlap}))))]
             (if family
               {:target target :status :computed
                :reported (:reported-count family) :distinct (:distinct-count family)
                :by-kind (frequencies (map :kind (:policies family)))
                :reading-reported reading-reported
                :old-retractions (count (:retractions old))
                :current-retractions (count (filter #(= :retraction (:kind %)) (:policies family)))
                :failures (mapv :kind (:failures family))}
               {:target target :status :refused
                :failures (:invalid-fields published)})))
         stems)]
    (println "TARGET-POLICY-FAMILIES" (pr-str rows))
    (is (= 7 (count rows)))
    (is (= 6 (count (filter #(= :computed (:status %)) rows))))
    (is (= ["M-web-arxana-ui-improvements"]
           (mapv :target (filter #(= :refused (:status %)) rows))))))
