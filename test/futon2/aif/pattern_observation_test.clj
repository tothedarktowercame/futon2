(ns futon2.aif.pattern-observation-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.pattern-observation :as pattern-observation]))

(def fixture
  (edn/read-string
   (slurp "test/resources/wm/retained-25e24-pattern-fold-projection.edn")))

(def revision {:repo "futon5a" :sha (:after fixture)})
(def source
  {:record-path
   "data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/003-construction.edn"
   :record-sha256 "15d855b11777c3afae1ec7d523ee91433098cd7b8dc4d9812e15a0f63e25c16c"})

(defn supply
  ([] (supply (:fold-conditions fixture)))
  ([conditions]
   (pattern-observation/supply
    {:prediction (:prediction fixture)
     :fold-conditions conditions
     :revision revision
     :source source})))

(defn row-for [result occurrence]
  (first (filter #(= occurrence (:occurrence %)) (:rows result))))

(deftest retained-attempt-supplies-typed-absences
  (let [result (supply)
        rows (:rows result)
        occurrences (mapv :occurrence rows)]
    (is (= :supplied (:status result)))
    (is (= [["gauntlet/world-is-hypergraph" 0]
            ["gauntlet/world-is-hypergraph" 19]]
           occurrences))
    (is (= (mapv :token (get-in fixture [:prediction :wanted]))
           (mapv :effect rows)))
    (is (every? #(= revision (:revision %)) rows))
    (is (every? #(= {:status :missing
                     :reason :completion-check-not-executable}
                    (:observed %))
                rows))
    (is (every? #(nil? (:authority-evidence %)) rows))
    (is (every? #(= source (select-keys (:source %) [:record-path :record-sha256]))
                rows))
    (is (= :emission-per-mission
           (get-in (row-for result ["gauntlet/world-is-hypergraph" 19])
                   [:source :completion-check 0])))
    (is (= (evidence/value-digest rows) (:rows-sha256 result)))
    (is (re-matches #"[0-9a-f]{64}" (:rows-sha256 result)))))

(deftest fold-status-is-not-an-observation
  (let [conditions (mapv #(if (= ["gauntlet/world-is-hypergraph" 19]
                                  (get-in % [:witness :occurrence-id]))
                            (assoc % :status :established)
                            %)
                         (:fold-conditions fixture))
        row (row-for (supply conditions) ["gauntlet/world-is-hypergraph" 19])]
    (is (= :established (get-in row [:source :fold-status])))
    (is (= {:status :missing :reason :completion-check-not-executable}
           (:observed row)))))

(deftest boolean-looking-fold-fields-are-not-observations
  (let [conditions (mapv #(if (= ["gauntlet/world-is-hypergraph" 19]
                                  (get-in % [:witness :occurrence-id]))
                            (assoc % :observed true
                                   :result {:observed true :check :C4})
                            %)
                         (:fold-conditions fixture))
        row (row-for (supply conditions) ["gauntlet/world-is-hypergraph" 19])]
    (is (= {:status :missing :reason :completion-check-not-executable}
           (:observed row)))
    (is (nil? (:authority-evidence row)))))

(deftest missing-occurrence-is-not-relabelled
  (let [zero ["gauntlet/world-is-hypergraph" 0]
        nineteen ["gauntlet/world-is-hypergraph" 19]
        conditions (remove #(= zero (get-in % [:witness :occurrence-id]))
                           (:fold-conditions fixture))
        result (supply conditions)]
    (is (= {:status :missing :reason :occurrence-not-in-fold}
           (:observed (row-for result zero))))
    (is (= {:status :missing :reason :completion-check-not-executable}
           (:observed (row-for result nineteen))))
    (is (every? (fn [{:keys [effect occurrence]}]
                  (= occurrence (nth effect 2)))
                (:rows result)))))

(deftest duplicate-occurrence-is-typed-ambiguous
  (let [occurrence ["gauntlet/world-is-hypergraph" 19]
        condition (first (filter #(= occurrence
                                     (get-in % [:witness :occurrence-id]))
                                 (:fold-conditions fixture)))
        row (row-for (supply (conj (:fold-conditions fixture) condition)) occurrence)]
    (is (= {:status :missing :reason :occurrence-ambiguous-in-fold :count 2}
           (:observed row)))))

(deftest mission-domain-is-not-applicable
  (let [holes [:hole/h3b6587f68a09 :hole/h367d19f57c3e :hole/h2ec21424642e
               :hole/h459471323b5c :hole/hbc31d1f47ff8 :hole/h66d04c790126]
        prediction {:schema :wm/token-outcome-prediction-v1
                    :status :frozen
                    :target "M-war-machine-wiring"
                    :wanted (mapv (fn [hole]
                                    {:token ["M-war-machine-wiring" hole]
                                     :predicted 0})
                                  holes)}
        result (pattern-observation/supply
                {:prediction prediction :fold-conditions (:fold-conditions fixture)
                 :revision revision :source source})]
    (is (= [:not-applicable :prediction-not-pattern-domain "M-war-machine-wiring"]
           ((juxt :status :kind :target) result)))
    (is (not (contains? result :rows)))))

(deftest after-revision-must-be-a-commit
  (let [result (pattern-observation/supply
                {:prediction (:prediction fixture)
                 :fold-conditions (:fold-conditions fixture)
                 :revision {:repo "futon5a" :sha "HEAD"}
                 :source source})]
    (is (= [:refused :after-revision-missing]
           ((juxt :status :kind) result)))
    (is (not (contains? result :rows)))))

(defn with-check [occurrence check]
  (mapv #(if (= occurrence (get-in % [:witness :occurrence-id]))
           (assoc-in % [:witness :authored-clause :completion-check] check)
           %)
        (:fold-conditions fixture)))

(def mission-locator
  [:artifact-locator {:class :C3 :repo "futon5a"
                      :path "holes/missions/M-war-machine-wiring.md"}])

(deftest executable-check-yields-a-boolean-at-the-after-revision
  (let [nineteen ["gauntlet/world-is-hypergraph" 19]
        zero ["gauntlet/world-is-hypergraph" 0]
        result (supply (with-check nineteen mission-locator))
        row (row-for result nineteen)]
    (is (= true (:observed row)))
    (is (= :C3 (get-in row [:authority-evidence :check])))
    (is (= (:after fixture)
           (get-in row [:authority-evidence :evidence :resolved-sha])))
    (is (= :artifact-locator (get-in row [:source :completion-check 0])))
    (is (= {:status :missing :reason :completion-check-not-executable}
           (:observed (row-for result zero))))
    (is (nil? (:authority-evidence (row-for result zero))))))

(deftest executable-check-false-overrides-an-established-fold-status
  (let [nineteen ["gauntlet/world-is-hypergraph" 19]
        conditions (mapv #(if (= nineteen (get-in % [:witness :occurrence-id]))
                            (assoc % :status :established :observed true)
                            %)
                         (with-check nineteen
                           [:artifact-locator
                            {:class :C3 :repo "futon5a"
                             :path "holes/missions/no-such-file.md"}]))
        row (row-for (supply conditions) nineteen)]
    (is (= :established (get-in row [:source :fold-status])))
    (is (= false (:observed row)))
    (is (= :C3 (get-in row [:authority-evidence :check])))))

(deftest evaluator-refusals-reach-the-row-typed
  (let [nineteen ["gauntlet/world-is-hypergraph" 19]
        row (row-for (supply (with-check nineteen
                               [:artifact-locator
                                {:class :C3 :repo "futon2" :path "deps.edn"}]))
                     nineteen)]
    (is (= [:missing :different-artifact-repository]
           ((juxt :status :reason) (:observed row))))
    (is (nil? (:authority-evidence row)))))
