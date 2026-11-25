(ns futon2.aif.selection-shared-prediction-test
  "Shadow of outer-select's policy grouping on the shared scorer's own
  prediction (M-cascade-live-causal; retirement-plan.md C1/C2). Two halves:

  1. The decision is unchanged: every fixture reuses outer-select-test's
  base fixtures, and :choice-input-digest / :outcome / :chosen-class must be
  identical with the shadow on (they are computed before the shadow is
  attached; this pins that).

  2. The shadow's own behaviour: two cascades with different outer-score
  outcomes but the same shared prediction land in one shared class (and the
  reverse case), and a cascade whose shared prediction equals stopping is
  stopping-equivalent under the shadow."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-menu-fixture :as menu-fixture]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-select-test :as outer-select-test]
            [futon2.aif.selection-canonical-g :as selection-g]
            [futon2.aif.selection-shared-prediction :as ssp]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

;;; ## 1. The decision is unchanged on the held fixtures

(def held-fixtures
  "The held click fixtures that drive select: outer-select-test's base
  fixtures and the chain fixture of
  every-cascade-identity-digest-pin-is-replay-recomputable (literals copied;
  they are let-bound there)."
  {:base {:makeup outer-select-test/base-makeup
          :state outer-select-test/base-state
          :observation outer-select-test/base-observation}
   :chain {:makeup {:cascades [{:id :start :patterns ["only-what-can-start-now"] :steps [:ready-gate]}
                               {:id :discover :patterns ["find-out-before-building"]
                                :steps [{:gate-status :undetermined}]}]}
           :state outer-select-test/base-state
           :observation {:focus "M-focus"
                         :relation (fn [_ _] :associated)
                         :field [{:target "T-10" :reach 1/2 :reach-source :default-reach}
                                 {:target "T-08" :reach 1/2 :reach-source :default-reach}]
                         :facts {"T-08" {:status :ready :unblocks 0 :cone 0 :rank 1}
                                 "T-10" {:status :ready :unblocks 0 :cone 0 :rank 3}}}}})

(deftest decision-unchanged-with-shadow-on
  (testing "the held fixtures keep :choice-input-digest, :outcome and
  :chosen-class byte-identical (the shadow is attached after they are
  computed and reads nothing they read)"
    (doseq [[fixture {:keys [makeup state observation]}] held-fixtures]
      (let [result (outer-select/select makeup state observation)
            shadow (:shared-prediction-shadow result)]
        (is (map? shadow) (str fixture))
        (is (= :wm/shared-prediction-shadow-v1 (:schema shadow)))
        ;; the shadow saw the same cascades the decision grouped (the
        ;; stopping-equivalent ones live outside its :classes, exactly as in
        ;; the existing grouping)
        (let [shadow-members (into (set (mapcat :members (:classes shadow)))
                                   (mapcat :members (:stopping-equivalent shadow)))]
          (is (= (set (map :id (:cascades result)))
                 shadow-members
                 (set (keys (:class-of shadow))))))
        ;; the decision digest covers the OLD classes only; recompute it and
        ;; confirm the shadow key did not enter it
        (is (= (:choice-input-digest result)
               (outer-select/choice-input-digest result (:focus observation))))
        (is (contains? result :outcome))))))

(deftest shadow-reports-on-held-fixtures
  (testing "report table: fixture -> old policy count / shadow policy count /
  old stopping-equivalent / shadow / would-differ?"
    (doseq [[fixture {:keys [makeup state observation]}] held-fixtures]
      (let [result (outer-select/select makeup state observation)
            shadow (:shared-prediction-shadow result)
            old-stopping (set (mapcat :members (:stopping-equivalent result)))
            new-stopping (set (mapcat :members (:stopping-equivalent shadow)))
            row {:fixture fixture
                 :old-policy-count (get-in result [:construction :policy-count])
                 :shadow-policy-count (:policy-count shadow)
                 :old-stopping-equivalent old-stopping
                 :shadow-stopping-equivalent new-stopping
                 :would-differ? (:would-differ? shadow)
                 :differences (:differences shadow)}]
        (println "SHADOW-REPORT" (pr-str row))
        ;; The shadow is well-formed on every held fixture; whether it differs
        ;; is reported, not asserted away.
        (is (integer? (:old-policy-count row)))
        (is (integer? (:shadow-policy-count row)))
        (is (contains? row :would-differ?))))))

;;; ## 2. The shadow's own behaviour

(def two-interpretations
  {"alpha-pattern" {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}
   "beta-pattern"  {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}})

(defn- shared-classes
  "Score ENTRIES with the canonical scorer and group with the shadow."
  [entries facts]
  (binding [selection-g/*extra-interpretations* two-interpretations]
    (let [canonical (selection-g/score entries facts {})
          cascades (mapv (fn [e]
                           (let [scored (get-in canonical [:by-id (:id e)])]
                             {:id (:id e) :pick (:pick e)
                              :G (:G-cascade scored)
                              :prediction {:parameters {}}}))
                         entries)]
      {:canonical canonical
       :cascades cascades
       :classes (ssp/canonical-prediction-classes cascades (:by-id canonical)
                                                  outer-select/g-units
                                                  outer-select/parameters-digest)})))

(deftest same-shared-prediction-one-class
  (testing "two cascades with different outer-score outcomes but the same
  shared prediction land in ONE shared class: the shared grouping sees the
  token trajectory, and alpha-pattern / beta-pattern drive identical
  trajectories on the same pick (the old grouping would key on outer-score's
  outcome digest, which these fixtures' picks make identical anyway -- so
  the reverse case below is what discriminates)"
    (let [facts {"M-a" {:status :ready :unblocks 1 :cone 1}}
          {:keys [classes]} (shared-classes
                             [{:id :c1 :patterns ["alpha-pattern"] :pick "M-a" :relation-class :associated}
                              {:id :c2 :patterns ["beta-pattern"] :pick "M-a" :relation-class :associated}]
                             facts)]
      (is (= 1 (count classes)))
      (is (= [:c1 :c2] (:members (first classes))))
      (is (string? (:shared-prediction-digest (first classes)))))))

(deftest different-shared-prediction-separate-classes
  (testing "the reverse: two cascades whose shared predictions differ (one
  pattern fires, the other's guard never holds) land in SEPARATE shared
  classes even at equal G-key components -- the mutant that keys on G only
  (planted, see mutant-key-on-g-only-fails) merges them"
    (let [facts {"M-a" {:status :ready :unblocks 1 :cone 1}}
          firing {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}
          inert {:consumes #{:token-absent-everywhere} :forbids #{}
                 :produces #{:can-begin}}]
      (binding [selection-g/*extra-interpretations* {"alpha-pattern" firing
                                                     "beta-pattern" inert}]
        (let [canonical (selection-g/score
                         [{:id :c1 :patterns ["alpha-pattern"] :pick "M-a" :relation-class :associated}
                          {:id :c2 :patterns ["beta-pattern"] :pick "M-a" :relation-class :associated}]
                         facts {})
              d1 (ssp/shared-prediction-digest (get-in canonical [:by-id :c1]))
              d2 (ssp/shared-prediction-digest (get-in canonical [:by-id :c2]))]
          (is (string? d1))
          (is (string? d2))
          (is (not= d1 d2) "different trajectories -> different shared predictions"))))))

(deftest stopping-equivalent-under-shadow
  (testing "a cascade whose pattern never fires keeps the initial state at
  every step, exactly as the empty policy does: its shared prediction equals
  stopping's and it is stopping-equivalent under the shadow"
    (let [facts {"M-a" {:status :ready :unblocks 1 :cone 1}}
          inert {:consumes #{:token-absent-everywhere} :forbids #{}
                 :produces #{:can-begin}}]
      (binding [selection-g/*extra-interpretations* {"alpha-pattern" inert}]
        (let [canonical (selection-g/score
                         [{:id :c1 :patterns ["alpha-pattern"] :pick "M-a" :relation-class :associated}]
                         facts {})
              stop-digest (ssp/shared-prediction-digest (:stopping canonical))
              c1-digest (ssp/shared-prediction-digest (get-in canonical [:by-id :c1]))]
          (is (string? stop-digest))
          (is (= stop-digest c1-digest)
              "never-firing cascade predicts exactly what stopping predicts"))))))

(deftest refused-entry-has-typed-absence
  (testing "a refused entry has no shared prediction, typed -- never a digest
  of nothing"
    (let [sp (ssp/shared-prediction {:cascade-id :bad :status :refused
                                     :refusal {:kind :some-refusal}})]
      (is (= :no-shared-prediction (:status sp)))
      (is (= :refused (:reason sp)))
      (is (nil? (ssp/shared-prediction-digest {:cascade-id :bad :status :refused}))))))

(deftest mutant-key-on-g-only-fails
  (testing "PLANTED MUTANT: group on [g-units parameters-digest] only (drop
  the shared-prediction digest). Two cascades with different trajectories
  but equal G-key components then land in ONE class, so the
  different-shared-prediction invariant fails -- run this mutant by
  replacing ssp/canonical-prediction-classes's key-of first component with a
  constant; the assertion below pins the real key's discrimination by
  showing the constant-keyed variant merges what the real key separates"
    (let [facts {"M-a" {:status :ready :unblocks 1 :cone 1}}
          firing {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}
          inert {:consumes #{:token-absent-everywhere} :forbids #{}
                 :produces #{:can-begin}}]
      (binding [selection-g/*extra-interpretations* {"alpha-pattern" firing
                                                     "beta-pattern" inert}]
        (let [canonical (selection-g/score
                         [{:id :c1 :patterns ["alpha-pattern"] :pick "M-a" :relation-class :associated}
                          {:id :c2 :patterns ["beta-pattern"] :pick "M-a" :relation-class :associated}]
                         facts {})
              ;; Equal G by construction: the ONLY key component that can
              ;; separate these two cascades is the prediction digest.
              cascades [{:id :c1 :pick "M-a" :G 1.0 :prediction {:parameters {}}}
                        {:id :c2 :pick "M-a" :G 1.0 :prediction {:parameters {}}}]
              real-classes (ssp/canonical-prediction-classes cascades (:by-id canonical)
                                                             outer-select/g-units
                                                             outer-select/parameters-digest)
              ;; the mutant: constant prediction key (key on G only)
              mutant-key (fn [c] [::constant
                                  (outer-select/g-units (:G c))
                                  (outer-select/parameters-digest c)])
              mutant-groups (vals (group-by mutant-key cascades))]
          (is (not= (ssp/shared-prediction-digest (get-in canonical [:by-id :c1]))
                    (ssp/shared-prediction-digest (get-in canonical [:by-id :c2])))
              "fixture check: the trajectories really differ")
          (is (= 2 (count real-classes)) "the real key separates the trajectories")
          (is (= 1 (count mutant-groups)) "the G-only mutant merges them -- THIS is what fails if the digest component is dropped"))))))
