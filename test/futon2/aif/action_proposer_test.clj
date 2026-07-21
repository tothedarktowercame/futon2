(ns futon2.aif.action-proposer-test
  "Tests for the action-proposer protocol and bootstrap-proposer."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.intrinsic-values :as iv]))

(use-fixtures :each
  (fn [t]
    (iv/reset-to-prior!)
    (try (t) (finally (iv/reset-to-prior!)))))

(def ^:private dummy-state
  {:observation {} :belief {}})

(deftest bootstrap-proposer-includes-no-op-test
  (testing "bootstrap-proposer always includes :no-op (for abstain comparison)"
    (let [proposed (ap/propose ap/bootstrap-proposer dummy-state)
          types (set (map :type proposed))]
      (is (contains? types :no-op)))))

(deftest bootstrap-proposer-surfaces-gaps-test
  (testing "bootstrap-proposer emits :learn-action-class for non-proposable action types"
    ;; Current state of substrate: :address-sorry, :open-mission, :fire-pattern
    ;; all return false from can-propose? → bootstrap should emit a
    ;; :learn-action-class for at least one of them.
    ;;
    ;; Deduplication: when multiple gap classes share the same intrinsic-value
    ;; (the Beta(1,1) prior = 0.5 for all), only one representative survives
    ;; to avoid tripping the policy-nondiscrimination gate with a flat EFE
    ;; landscape. The omitted classes are still modelled as gaps (their
    ;; absence is the signal).
    (let [proposed (ap/propose ap/bootstrap-proposer dummy-state)
          learn-actions (filter #(= :learn-action-class (:type %)) proposed)
          target-classes (set (map :target-class learn-actions))]
      (is (seq learn-actions)
          "at least one :learn-action-class is surfaced")
      (is (some #(contains? target-classes %)
                [:address-sorry :open-mission :fire-pattern])
          "the representative gap is one of the non-proposable classes")
      (is (not (contains? target-classes :no-op))
          ":no-op is always proposable, no gap")
      (is (not (contains? target-classes :learn-action-class))
          ":learn-action-class is itself always proposable, no recursion")
      ;; At prior (all 0.5), deduplication collapses to exactly one.
      (is (= 1 (count learn-actions))
          "at Beta(1,1) prior all gaps share 0.5; one representative remains"))))

(deftest learn-actions-carry-intrinsic-value-test
  (testing "bootstrap-proposer assigns :intrinsic-value to :learn-action-class actions"
    (let [learn-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      (is (every? :intrinsic-value learn-actions)
          "every :learn-action-class action carries :intrinsic-value")
      (is (every? #(pos? (:intrinsic-value %)) learn-actions)
          "intrinsic-value is positive (credit)"))))

(deftest learn-actions-intrinsic-value-tracks-atom-test
  (testing "R12 narrow-take-up: :intrinsic-value is sourced from the
            intrinsic-values atom, not the historical static 0.1"
    ;; Empty atom → Beta(1,1) prior → 0.5; dedup collapses to one representative.
    (let [learn-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      (is (every? #(= 0.5 (double (:intrinsic-value %))) learn-actions)
          "with empty atom, every :learn-action-class carries prior mode 0.5")
      (is (every? #(not= 0.1 (double (:intrinsic-value %))) learn-actions)
          "no longer the historical static 0.1"))
    ;; Install a known posterior for :address-sorry; recheck.
    (iv/apply-update! {:class :address-sorry
                       :as-of "2026-05-21T12:00:00Z"
                       :alpha-post 5.0 :beta-post 1.0 :intrinsic-value-post 1.0
                       :n-emissions-in-window 4 :n-followthrough-in-window 4})
    (let [learn-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      ;; Now :address-sorry has 1.0 while others stay at 0.5: two distinct
      ;; intrinsic-value bands → two gap-actions survive dedup.
      (is (= 2 (count learn-actions))
          "differentiated classes survive dedup: one at 1.0, one at 0.5")
      (is (some #(= 1.0 (double (:intrinsic-value %))) learn-actions)
          "atom posterior for :address-sorry is reflected in proposal")
      (is (some #(= 0.5 (double (:intrinsic-value %))) learn-actions)
          "other classes still at prior"))))

(deftest learn-actions-carry-rationale-test
  (testing ":learn-action-class actions carry a human-readable rationale"
    (let [learn-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      (is (every? string? (map :rationale learn-actions))))))

(deftest gap-actions-deduplicated-by-intrinsic-value-test
  (testing "gap-actions with identical intrinsic-value are deduplicated
            to prevent policy-nondiscrimination (flat EFE landscape)"
    ;; At prior (empty atom), all gap classes have intrinsic-value 0.5.
    ;; Without dedup, N gaps × 0.5 = N identical candidates → discrimination
    ;; gate fails. With dedup, exactly one representative survives.
    (let [prior-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      (is (= 1 (count prior-actions))
          "at Beta(1,1) prior all gaps share 0.5; one representative"))
    ;; Differentiate two classes: now two distinct intrinsic-value bands.
    (iv/apply-update! {:class :address-sorry
                       :as-of "2026-06-01T00:00:00Z"
                       :alpha-post 5.0 :beta-post 1.0 :intrinsic-value-post 0.8
                       :n-emissions-in-window 4 :n-followthrough-in-window 4})
    (iv/apply-update! {:class :open-mission
                       :as-of "2026-06-01T00:00:01Z"
                       :alpha-post 1.0 :beta-post 5.0 :intrinsic-value-post 0.2
                       :n-emissions-in-window 4 :n-followthrough-in-window 0})
    (let [diff-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                            (filter #(= :learn-action-class (:type %))))
          values (map :intrinsic-value diff-actions)]
      (is (= 3 (count diff-actions))
          "three distinct bands (0.8, 0.5, 0.2) → three gap-actions")
      (is (= (count values) (count (distinct values)))
          "no two gap-actions share an intrinsic-value"))))

;; (The "registering can-propose?=true removes the gap" property is covered
;;  by `futon2.aif.sorry-registry-test/integration-bootstrap-no-longer-
;;  surfaces-address-sorry-gap-test`, which uses real state shape rather
;;  than mutating defmulti globally. The earlier dance with defmethod /
;;  remove-method here violated test isolation once sorry-registry started
;;  installing a permanent :address-sorry arm at namespace load.)

(deftest proposer-id-test
  (testing "bootstrap-proposer has stable identifier"
    (is (= :bootstrap (ap/proposer-id ap/bootstrap-proposer)))))

(deftest compose-proposers-test
  (testing "compose-proposers concatenates and de-duplicates"
    (let [p1 (reify ap/ActionProposer
               (propose [_ _] [{:type :no-op}
                               {:type :learn-action-class :target-class :x
                                :intrinsic-value 0.1}])
               (proposer-id [_] :p1))
          p2 (reify ap/ActionProposer
               (propose [_ _] [{:type :no-op}  ; duplicate
                               {:type :learn-action-class :target-class :y
                                :intrinsic-value 0.1}])
               (proposer-id [_] :p2))
          composed (ap/compose-proposers [p1 p2] dummy-state)]
      (is (= 3 (count composed)) "no-op de-duplicated"))))
