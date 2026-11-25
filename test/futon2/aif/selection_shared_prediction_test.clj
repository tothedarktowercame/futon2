(ns futon2.aif.selection-shared-prediction-test
  "Step 2a (Joe 2026-11-25 19:34): selection classes and stopping-equivalence
  are decided on the SHARED scorer's prediction
  (selection-shared-prediction). The shadow step was 1eb8666b6; the shadow
  key is gone, the shared grouping IS the decision.

  Covers: the claude-14 a/b/c case through select (a,b same trajectory ->
  one class; c same pick-level outcomes under the old digest but a different
  trajectory -> a separate class); stopping-equivalence through select (a
  never-firing cascade -> :stopping-equivalent; a menu of only such cascades
  abstains :all-policies-predict-stopping); the record keys keep their names
  (:classes, :stopping-equivalent, :prediction, :choice-input-digest)."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-menu-fixture :as menu-fixture]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-select-test :as outer-select-test]
            [futon2.aif.selection-canonical-g :as selection-g]
            [futon2.aif.selection-shared-prediction :as ssp]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

;;; ## The record keeps its keys; the decision runs on the shared prediction

(deftest record-keys-stay
  (testing ":classes, :stopping-equivalent, :prediction, :choice-input-digest
  keep their names; :prediction content is the shared trajectory plus the Q11
  parameters; the shadow key is gone (it is the decision now)"
    (let [r (outer-select/select outer-select-test/base-makeup
                                 outer-select-test/base-state
                                 outer-select-test/base-observation)]
      (is (contains? r :classes))
      (is (contains? r :stopping-equivalent))
      (is (contains? r :prediction))
      (is (contains? r :choice-input-digest))
      (is (not (contains? r :shared-prediction-shadow)))
      (is (= :ok (get-in r [:prediction :trajectory :status])))
      (is (contains? (:prediction r) :parameters))
      (is (= (:choice-input-digest r)
             (outer-select/choice-input-digest r (:focus outer-select-test/base-observation))))
      (doseq [c (:classes r)]
        (is (string? (:prediction-digest c)))))))

;;; ## The a/b/c case through select (claude-14's bad case)

(def gamma-interpretation
  "A pattern whose guard never holds, supplied to select through the
  makeup's :task-selection-interpretations."
  {"gamma-pattern" {:consumes #{:token-absent-everywhere} :forbids #{}
                    :produces #{:can-begin}}})

(deftest a-b-one-class-c-separate
  (testing "a and b drive the same trajectory (same guard and produces, two
  pattern names) -> ONE class. c's guard never holds -> its trajectory keeps
  the initial state, which is exactly stopping's: c is stopping-equivalent,
  not merged with a/b. The old outer-outcome digest merged a and c when
  their outer-score outcomes and G coincided; the shared prediction keeps
  them apart (see mutant-restoring-outer-outcome-digest-fails)."
    (let [observation (assoc outer-select-test/base-observation
                             :relation (fn [_ _] :associated))
          ;; a and b are the same policy stated two ways (gate keyword vs a
          ;; function step): same pattern, same pick, same trajectory, same
          ;; predicted parameters. c's pattern never fires.
          makeup {:task-selection-interpretations gamma-interpretation
                  :cascades [{:id :a :patterns ["test"] :steps [:ready-gate]}
                             {:id :b :patterns ["test"]
                              :steps [{:gate #(= "M-ready" (:target %))}]}
                             {:id :c :patterns ["gamma-pattern"] :steps [:ready-gate]}]}
          r (outer-select/select makeup outer-select-test/base-state observation)
          classes (:classes r)
          members (set (mapcat :members classes))]
      (is (= #{:a :b} members) "a and b are the one compared policy")
      (is (= 1 (count classes)))
      (is (= [:c] (:members (first (:stopping-equivalent r))))
          "c never fires: it predicts stopping")
      (is (= {:status :chosen} (:outcome r)))
      (is (= 1 (get-in r [:construction :policy-count]))))))

(deftest menu-of-never-firing-cascades-abstains
  (testing "every policy predicts stopping -> :all-policies-predict-stopping"
    (let [observation (assoc outer-select-test/base-observation
                             :relation (fn [_ _] :associated))
          makeup {:task-selection-interpretations gamma-interpretation
                  :cascades [{:id :a :patterns ["gamma-pattern"] :steps [:ready-gate]}
                             {:id :b :patterns ["gamma-pattern"] :steps [:ready-gate]}]}
          r (outer-select/select makeup outer-select-test/base-state observation)]
      (is (= {:status :abstained :case :all-policies-predict-stopping}
             (:outcome r)))
      (is (empty? (:classes r)))
      (is (= #{:a :b} (set (mapcat :members (:stopping-equivalent r)))))
      (is (= :ok (get-in r [:prediction :trajectory :status]))
          "the abstained outcome's prediction is stopping's trajectory"))))

;;; ## Mutant: restore the outer outcome digest as the class key

(deftest mutant-restoring-outer-outcome-digest-fails
  (testing "PLANTED MUTANT: key the classes on the outer-score outcome
  digest again (selection-shared-prediction/canonical-prediction-classes
  replaced by a key on [nil g-units parameters-digest] -- no trajectory).
  Run by mutating the source; then a-b-one-class-c-separate fails because c
  (whose outer outcomes and pick-level G coincide with a/b's on this
  fixture) merges into their class: members become #{:a :b :c}. Here we pin
  the discrimination at the function level: the trajectory key separates c
  from a/b, the mutant key does not."
    (binding [selection-g/*extra-interpretations*
              (merge {"test" {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}}
                     gamma-interpretation)]
      (let [facts {"M-ready" (get-in outer-select-test/base-observation [:facts "M-ready"])}
            entries [{:id :a :patterns ["test"] :pick "M-ready" :relation-class :associated}
                     {:id :b :patterns ["test"] :pick "M-ready" :relation-class :associated}
                     {:id :c :patterns ["gamma-pattern"] :pick "M-ready" :relation-class :associated}]
            canonical (selection-g/score entries facts {})
            cascades (mapv (fn [e]
                             {:id (:id e) :pick (:pick e)
                              :G (:G-cascade (get-in canonical [:by-id (:id e)]))
                              :prediction {:parameters {}}})
                           entries)
            real (ssp/canonical-prediction-classes cascades (:by-id canonical)
                                                   outer-select/g-units
                                                   outer-select/parameters-digest)
            mutant-groups (vals (group-by (fn [c] [::outer-outcomes
                                                   (outer-select/g-units (:G c))
                                                   (outer-select/parameters-digest c)])
                                          cascades))
            c-class (filter #(some #{:c} (:members %)) real)]
        (is (= 3 (count cascades)))
        (is (not= (ssp/shared-prediction-digest (get-in canonical [:by-id :a]))
                  (ssp/shared-prediction-digest (get-in canonical [:by-id :c])))
            "fixture check: a and c have different trajectories")
        (is (= (ssp/shared-prediction-digest (get-in canonical [:by-id :a]))
               (ssp/shared-prediction-digest (get-in canonical [:by-id :b])))
            "fixture check: a and b share their trajectory")
        (is (= 1 (count c-class)) "the real key keeps c in its own class")
        (is (< (count mutant-groups) 3)
            "the mutant merges at least two of a/b/c -- a-b-one-class-c-separate then fails")))))
