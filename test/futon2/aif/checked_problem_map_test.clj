(ns futon2.aif.checked-problem-map-test
  "Lean-synthetic, adapted fixture from mathlib4 CheckedProblemMap.lean at
  f2ed1412229687109b44a0a046e4d41d597ea364.  It is not a live decoder and
  supplies no proof authentication.

  Adaptations: m0's six statement IDs are keywords; nil and false are added
  only to exercise Clojure membership; report labels/claims are deliberately
  varied to prove they are inert; and metadata is added to prove preservation.
  The source fixture is otherwise the reviewed foundation/adapter/consumer,
  hard alternative, and unseeded two-node-cycle shape."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.checked-problem-map :as checked]))

(def universe #{:foundation :adapter :consumer :hard :cyc-a :cyc-b nil false})

(def m0
  {:universe universe
   :artifacts []
   :reductions [{:head :consumer :tails #{:foundation :adapter} :route :via-foundation}
                {:head :consumer :tails #{:hard} :route :via-hard}
                {:head :cyc-a :tails #{:cyc-b}}
                {:head :cyc-b :tails #{:cyc-a}}]
   :obligations #{:consumer :cyc-a}})

(def foundation-only
  {:label :closed :claimed-closed #{:foundation :consumer}
   :checked {:artifacts [{:stmt :foundation :status :witnessed :verified? true}]
             :reductions []}})

(def foundation-and-adapter
  {:label :censored :claimed-closed #{:consumer}
   :checked {:artifacts [{:stmt :foundation} {:stmt :adapter}]
             :reductions []}})

(def prediction-only
  {:label :prediction :claimed-closed #{:consumer}
   :checked {:artifacts [] :reductions []}})

(def censored-helper
  {:label :censored :claimed-closed #{:consumer}
   :checked {:artifacts [{:stmt :adapter :verdict :closed :receipt "not evidence"}]
             :reductions []}})

(defn- refusal [thunk]
  (try
    (thunk)
    nil
    (catch clojure.lang.ExceptionInfo error
      (:refusal (ex-data error)))))

(defn- rule-closed? [problem-map candidate]
  (and (every? #(contains? candidate (:stmt %)) (:artifacts problem-map))
       (every? (fn [{:keys [head tails]}]
                 (or (not (every? #(contains? candidate %) tails))
                     (contains? candidate head)))
               (:reductions problem-map))))

(defn- subsets [items]
  (reduce (fn [sets item] (into sets (map #(conj % item) sets))) [#{}] items))

(defn- wrong-claimed-closed-seeding [problem-map report]
  (into (checked/closed-set problem-map) (:claimed-closed report)))

(defn- assert-exhaustive-least! [problem-map]
  (let [result (checked/closed-set problem-map)
        candidates (filter #(rule-closed? problem-map %) (subsets (:universe problem-map)))]
    (is (rule-closed? problem-map result))
    (is (every? #(set/subset? result %) candidates))))

(deftest admits-only-supplied-artifacts-and-and-rules
  (testing "Lean foundation_adapter_discharge and one_alternative_suffices"
    (let [closed (checked/closed-set (checked/update m0 foundation-and-adapter))]
      (is (contains? closed :consumer))
      (is (not (contains? closed :hard)))
      (is (= #{:cyc-a}
             (checked/open-obligations (checked/update m0 foundation-and-adapter))))))
  (testing "Lean foundation_without_adapter_unclosed and reduction_not_discharge"
    (let [after-foundation (checked/update m0 foundation-only)]
      (is (not (checked/closed? after-foundation :consumer)))
      (is (checked/has-reduction? after-foundation :consumer))
      (is (contains? (checked/open-obligations after-foundation) :consumer))))
  (testing "labels, claims and checker-shaped keys never seed closure"
    (is (= (checked/closed-set m0) (checked/closed-set (checked/update m0 prediction-only))))
    (is (= #{:adapter} (checked/return-obs m0 #{:adapter :consumer} censored-helper)))
    (is (not (checked/closed? (checked/update m0 censored-helper) :consumer)))))

(deftest fixed-point-controls-and-exhaustive-least-oracle
  (testing "chain needs more than one step and an empty-tail rule fires"
    (let [chain (assoc m0 :artifacts [{:stmt :foundation}]
                           :reductions [{:head :adapter :tails #{:foundation}}
                                        {:head :consumer :tails #{:adapter}}
                                        {:head :hard :tails #{}}])]
      (is (= #{:foundation :adapter :consumer :hard} (checked/closed-set chain)))))
  (testing "unseeded cycles stay open without blocking a separate component"
    (let [after (checked/update m0 foundation-and-adapter)]
      (is (not (checked/closed? after :cyc-a)))
      (is (not (checked/closed? after :cyc-b)))
      (is (checked/closed? after :consumer))))
  (testing "false and nil work as reduction tails and heads, not just seeds"
    (let [typed (assoc m0 :artifacts [{:stmt :foundation}]
                          :reductions [{:head false :tails #{:foundation}}
                                       {:head nil :tails #{false}}])]
      (is (= #{:foundation false nil} (checked/closed-set typed)))
      (is (checked/has-reduction? typed false))
      (is (checked/has-reduction? typed nil))))
  (testing "the closure is rule-closed and least among all 64 subsets on nonempty cases"
    (let [six #{:foundation :adapter :consumer :hard :cyc-a :cyc-b}
          chain {:universe six :artifacts [{:stmt :foundation}]
                 :reductions [{:head :adapter :tails #{:foundation}}
                              {:head :consumer :tails #{:adapter}}
                              {:head :hard :tails #{}}]
                 :obligations #{:consumer :cyc-a}}
          seeded-alternative {:universe six :artifacts [{:stmt :hard}]
                              :reductions (:reductions m0) :obligations #{:consumer :cyc-a}}
          seeded-cycle {:universe six :artifacts [{:stmt :cyc-a}]
                        :reductions (:reductions m0) :obligations #{:consumer :cyc-a}}]
      (doseq [problem-map [chain seeded-alternative seeded-cycle]]
        (assert-exhaustive-least! problem-map)))))

(deftest update-and-return-observation-boundaries
  (let [m1 (checked/update m0 foundation-and-adapter)]
    (testing "absent return preserves a valid map identity and old closure"
      (is (identical? m1 (checked/update m1 nil)))
      (is (= #{:consumer} (checked/return-obs m1 #{:consumer :hard} nil))))
    (testing "same checked data ignores label/claim; same label can differ by checked data"
      (is (= (checked/return-obs m0 #{:consumer} foundation-and-adapter)
             (checked/return-obs m0 #{:consumer}
                                 (assoc foundation-and-adapter :label :anything
                                        :claimed-closed #{:hard}))))
      (is (not= (checked/return-obs m0 #{:consumer} foundation-and-adapter)
                (checked/return-obs m0 #{:consumer} foundation-only))))
    (testing "declared false and nil IDs use membership, not truthiness"
      (let [with-false (assoc m0 :artifacts [{:stmt false} {:stmt nil}])]
        (is (checked/closed? with-false false))
        (is (checked/closed? with-false nil))))))

(deftest admission-preserves-supplied-vectors-and-is-monotone
  (let [base (assoc m0
                    :artifacts [{:stmt :foundation :metadata {:origin :base}}]
                    :reductions [{:head :adapter :tails #{:foundation} :metadata {:rank 1}}])
        checked-part {:artifacts [{:stmt :hard :metadata {:origin :report}}]
                      :reductions [{:head :consumer :tails #{:adapter} :metadata {:rank 2}}]}
        admitted (checked/admit base checked-part)
        reordered (assoc admitted
                         :artifacts (vec (reverse (concat (:artifacts admitted)
                                                          (:artifacts admitted))))
                         :reductions (vec (reverse (concat (:reductions admitted)
                                                           (:reductions admitted)))))]
    (testing "admit changes neither fixed set and preserves vector order and entry metadata"
      (is (= (:universe base) (:universe admitted)))
      (is (= (:obligations base) (:obligations admitted)))
      (is (= [{:stmt :foundation :metadata {:origin :base}}
              {:stmt :hard :metadata {:origin :report}}]
             (:artifacts admitted)))
      (is (= {:head :adapter :tails #{:foundation} :metadata {:rank 1}}
             (first (:reductions admitted)))))
    (testing "order and duplicates do not change closure; admission is monotone"
      (is (= (checked/closed-set admitted) (checked/closed-set reordered)))
      (is (set/subset? (checked/closed-set base) (checked/closed-set admitted))))
    (testing "sequential helper then foundation closes consumer"
      (is (checked/closed? (checked/update (checked/update m0 censored-helper)
                                             foundation-only)
                          :consumer)))
    (testing "same label and same claims, different checked content differs"
      (let [same-header (assoc foundation-and-adapter
                               :label (:label foundation-only)
                               :claimed-closed (:claimed-closed foundation-only))]
        (is (not= (checked/return-obs m0 #{:consumer} foundation-only)
                  (checked/return-obs m0 #{:consumer} same-header)))))))

(deftest typed-input-refusals
  (is (= :undeclared-statement
         (refusal #(checked/validate (assoc-in m0 [:reductions 0 :tails] #{:ghost})))))
  (is (= :undeclared-statement (refusal #(checked/return-obs m0 #{:ghost} nil))))
  (is (= :report-checked-absent (refusal #(checked/update m0 {:label :closed}))))
  (is (= :malformed-report
         (refusal #(checked/update m0 {:checked {:artifacts [] :reductions #{}}}))))
  (is (= :malformed-map
         (refusal #(checked/validate (dissoc m0 :obligations)))))
  (is (= :malformed-map
         (refusal #(checked/validate (assoc-in m0 [:reductions 0 :tails] []))))))

(deftest mutation-controls-are-meaningful
  (testing "AND -> OR would make foundation-only close consumer"
    (let [wrong-or (assoc m0 :reductions [{:head :consumer :tails #{:foundation}}])]
      (is (not (checked/closed? (checked/update m0 foundation-only) :consumer)))
      (is (checked/closed? (checked/update wrong-or foundation-only) :consumer))))
  (testing "seeding claimed-closed would close consumer despite empty checked data"
    (is (not (checked/closed? (checked/update m0 prediction-only) :consumer)))
    (is (contains? (wrong-claimed-closed-seeding m0 prediction-only) :consumer))))
