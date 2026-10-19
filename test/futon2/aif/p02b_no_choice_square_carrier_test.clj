(ns futon2.aif.p02b-no-choice-square-carrier-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.wm.cascade-decision :as cascade])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn pinned-graph [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 0 :patterns 2 :pattern_ids ["p/a" "p/b"]
                 :summary [{:through "why" :edges 0} {:through "how" :edges 0}]
                 :edges []}))
    (graph-pin/pin! file)
    file))

(deftest no-choice-fallback-forms-an-empty-inner-field-not-the-outer-field
  ;; The assembled input is the shape generate-war-machine reaches after a
  ;; refused receipt: selected-task-id nil -> cascade-targets [] -> target
  ;; source declarations [].  The graph and reading root are real temporary
  ;; read ports; cascade-decision and selected-need are not stubbed.
  (let [graph (pinned-graph (temp-dir "p02b-graph-"))
        outer-refusal {:target {:absent :outer-task-not-selected}
                       :kind :outer-task-selection-refused
                       :missing :outer-task-selection}
        result (cascade/cascade-decision
                {:target-sources [] :refusals [outer-refusal]}
                {:pattern-graph-path (str graph)
                 :target-reading-root (str (temp-dir "p02b-readings-"))})
        judgement {:decision (:decision result)}]
    (is (= :abstained (get-in judgement [:decision :status])))
    (is (= [] (get-in judgement [:decision :selection-certificate
                                 :target-policy-field :families])))
    (is (nil? (square-authoring/selected-need judgement)))))
