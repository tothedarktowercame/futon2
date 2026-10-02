(ns futon2.aif.self-documenting-stack-redecision-replay-test
  "Real-data replay of the 2026-10-02 post-ask redecision input.  This runs
  the current mission-derived wants and machine-published interpretations
  through the same assembly, constructor, G evaluator, and candidate admission
  functions used by the production judge.  It does not run a click."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.mission-hole-wants :as mission-wants]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.want-interpretation :as want-interpretation]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.construction-inputs :as construction-inputs]))

(def ^:private target "M-self-documenting-stack")

(def ^:private replay-pin
  {:observed-futon2-commit "eaa94a82f3827662c499bc804d3496dd59db77be"
   :interpretations
   {:path "data/wm-interpretations/M-self-documenting-stack.edn"
    :sha256 "5a276c66925fdc64d4bd61d3d3a4ccd28afaab74d3d8a60c310db1a9e4d7dba8"}
   :mission
   {:repo "/home/joe/code/futon7"
    :commit "78c1a308b3bee4ce714d80708e669a52fa2a333e"
    :path "holes/M-self-documenting-stack.md"
    :sha256 "8c43549b7c859545de5bd420a54aaf1a06532ee3faf644a3171aebddeb2599a4"}})

(defn- sha256-file [path]
  (identity/sha256 (identity/read-bytes (io/file path))))

(defn- production-replay []
  (let [missions (registry/load-missions)
        base (cascade-sources/with-context-fn
              (mission-wants/merge-into-sources
               (cascade-sources/load-declared)
               registry/default-code-root
               (:missions missions)
               :WM))
        sources
        (assoc base :construction
               {:construct construction/construct
                :budget (:value (construction-inputs/construction-budget base))
                :move-cost (:value construction-inputs/construction-move-cost)
                :evaluate-g
                (fn [problem candidate]
                  (cascade-decision/constructed-candidate-g
                   problem candidate {}))})
        assembled
        (construction-inputs/assemble-cascade-problems-with-published
         want-interpretation/default-store
         {:targets [target] :sources sources})
        row (first (:problems assembled))
        admission (#'cascade-decision/admit-cascade-problem row)]
    {:assembled assembled :row row :admission admission}))

(deftest published-interpretations-construct-an-admissible-current-candidate
  (let [{:keys [interpretations mission]} replay-pin
        mission-bytes (:out (sh/sh "git" "-C" (:repo mission) "show"
                                   (str (:commit mission) ":" (:path mission))))
        futon2-pin-check (sh/sh "git" "merge-base" "--is-ancestor"
                                (:observed-futon2-commit replay-pin) "HEAD")
        {:keys [assembled row admission]} (production-replay)
        candidate (first (:constructed-candidates row))]
    (is (zero? (:exit futon2-pin-check)))
    (is (= (:sha256 interpretations) (sha256-file (:path interpretations))))
    (is (= (:commit mission)
           (str/trim (:out (sh/sh "git" "-C" (:repo mission) "rev-parse" "HEAD")))))
    (is (= (:sha256 mission)
           (sha256-file (io/file (:repo mission) (:path mission)))))
    (is (= (:sha256 mission)
           (identity/sha256 (.getBytes mission-bytes "UTF-8"))))
    (is (empty? (:refusals assembled)))
    (is (= [:hole/h54d2f6cb14fa :hole/h048dfec887c1]
           (get-in row [:cascade-problem :want])))
    (is (= :C1 (:candidate-id candidate)))
    (is (= [:apparatus/evidence-to-disposition-once
            :pattern-interpretation/reproduce-the-recorded-run]
           (:precedence candidate)))
    (is (= :machine-constructed
           (get-in candidate [:construction-receipt :kind])))
    (is (empty? (get-in candidate [:construction-receipt :unreached-wants])))
    (is (nil? (:refusal admission)))
    (is (= [candidate]
           (get-in admission [:problem :constructed-candidates])))))
