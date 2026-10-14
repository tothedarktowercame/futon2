(ns futon2.aif.self-documenting-stack-redecision-replay-test
  "Real-data replay of the 2026-10-02 post-ask redecision input.  This runs
  the current mission-derived wants and machine-published interpretations
  through the same assembly, constructor, G evaluator, and candidate admission
  functions used by the production judge.  It does not run a click."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.cascade-policy :as cascade-policy]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.mission-hole-wants :as mission-wants]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.prefix-free-energy-sut-test :as prefix-f-fixture]
            [futon2.aif.want-interpretation :as want-interpretation]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.construction-inputs :as construction-inputs]
            [futon2.aif.wm.loop-node-exercise :as loop-exercise]))

(def ^:private target "M-self-documenting-stack")

(def ^:private replay-square
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/reproduce-the-recorded-run.edn")))

(def ^:private replay-pin
  {:observed-futon2-commit "eaa94a82f3827662c499bc804d3496dd59db77be"
   :interpretations
   {:path "data/wm-interpretations/M-self-documenting-stack.edn"
    :sha256 "5a276c66925fdc64d4bd61d3d3a4ccd28afaab74d3d8a60c310db1a9e4d7dba8"}
   :mission
   {:repo "/home/joe/code/futon7"
    :commit "2a780d64f6ce7f93c6a7ac5e26b957f601a0c73e"
    :path "holes/M-self-documenting-stack.md"
    :sha256 "a6abfeac7540cb972144eb6cb98baaabb1880cbe07fd0b5b07f8875dc646d1e0"}})

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
        assembled0
        (construction-inputs/assemble-cascade-problems-with-published
         want-interpretation/default-store
         {:targets [target] :sources sources})
        ;; The historical publication predates square carriage.  Replay it
        ;; through the exact, source-pinned controlled carrier rather than
        ;; weakening current construction admission.
        assembled
        (update assembled0 :problems
                (fn [problems]
                  (mapv (fn [problem]
                          (-> problem
                              (assoc-in [:interpretation-receipts
                                         :pattern-interpretation/reproduce-the-recorded-run
                                         :blend-square]
                                        replay-square)
                              (update :constructed-candidates
                                      (fn [candidates]
                                        (mapv #(-> %
                                                   (assoc-in [:interpretation-receipts
                                                              :pattern-interpretation/reproduce-the-recorded-run
                                                              :blend-square]
                                                             replay-square)
                                                   (assoc-in [:construction-receipt :blend-squares
                                                              :pattern-interpretation/reproduce-the-recorded-run]
                                                             replay-square))
                                              candidates)))))
                        problems)))
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
    ;; The replay is pinned to a historical mission blob.  The repository may
    ;; legitimately advance while this suite runs; requiring its current HEAD
    ;; and worktree to equal the historical pin made unrelated commits fail CI.
    (is (= (:sha256 mission)
           (identity/sha256 (.getBytes mission-bytes "UTF-8"))))
    (is (empty? (:refusals assembled)))
    (is (= [:hole/h54d2f6cb14fa]
           (get-in row [:cascade-problem :want])))
    (is (= :C1 (:candidate-id candidate)))
    (is (= [:pattern-interpretation/reproduce-the-recorded-run]
           (:precedence candidate)))
    (is (= :machine-constructed
           (get-in candidate [:construction-receipt :kind])))
    (is (empty? (get-in candidate [:construction-receipt :unreached-wants])))
    (is (nil? (:refusal admission)))
    (is (= [candidate]
           (get-in admission [:problem :constructed-candidates])))))

(deftest admitted-real-candidate-retains-specific-loop-node-evidence
  (let [{:keys [assembled admission]} (production-replay)
        problem (:problem admission)
        t (:target problem)
        cp (:cascade-problem problem)
        recorded (first (:constructed-candidates problem))
        qualify (fn [token] [t token])
        patterns (into {}
                       (map (fn [[id {:keys [guard produces]}]]
                              [id (-> (cascade-policy/token-interpretation
                                      id {:guard {:needs (set (map qualify (:needs guard)))
                                                  :forbids (set (map qualify (:forbids guard)))}
                                          :produces (set (map qualify produces))})
                                     (assoc :target t))]))
                       (:interpretations cp))
        candidate {:kind :cascade-candidate
                   :id (:candidate-id recorded)
                   :target t
                   :want (vec (:want cp))
                   :precedence (mapv patterns (:precedence recorded))
                   :observation-locators
                   (into {} (map (fn [[token locator]] [(qualify token) locator]))
                         (:locators cp))
                   :construction-receipt (:construction-receipt recorded)
                   :interpretation-receipts (:interpretation-receipts problem)}
        decision (:decision
                  (cascade-decision/cascade-decision
                   assembled
                   {:prefix-f-commissioning
                    (prefix-f-fixture/commissioning-for candidate)}))
        receipt (loop-exercise/receipt decision)]
    (is (= :complete (:status receipt)))
    (is (= :singleton-admissible-candidate
           (get-in receipt [:by-node :R6 :because])))
    (is (= :bypassed (get-in receipt [:by-node :R13 :status])))
    (is (= :single-step-policy (get-in receipt [:by-node :R13 :because])))
    (is (= [[:pattern-interpretation/reproduce-the-recorded-run]]
           (get-in receipt [:by-node :R13 :evidence :precedences])))
    (is (= :singleton-policy-posterior
           (get-in receipt [:by-node :R14 :because])))
    (is (= :present (get-in receipt [:by-node :CTAU-CLASS :status])))
    (is (= :class-emission
           (get-in receipt [:by-node :CTAU-CLASS :evidence :preference-kind])))
    (is (identical? decision (loop-exercise/require-complete! decision)))))
