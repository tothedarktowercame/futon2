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
    ;; :learn-action-class for each.
    (let [proposed (ap/propose ap/bootstrap-proposer dummy-state)
          learn-actions (filter #(= :learn-action-class (:type %)) proposed)
          target-classes (set (map :target-class learn-actions))]
      (is (contains? target-classes :address-sorry))
      (is (contains? target-classes :open-mission))
      (is (contains? target-classes :fire-pattern))
      (is (not (contains? target-classes :no-op))
          ":no-op is always proposable, no gap")
      (is (not (contains? target-classes :learn-action-class))
          ":learn-action-class is itself always proposable, no recursion"))))

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
    ;; Empty atom → Beta(1,1) prior → 0.5
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
                             (filter #(= :learn-action-class (:type %))))
          by-class (into {} (map (juxt :target-class :intrinsic-value)
                                 learn-actions))]
      (is (= 1.0 (double (get by-class :address-sorry)))
          "atom posterior for :address-sorry is reflected in proposal")
      (is (= 0.5 (double (get by-class :open-mission)))
          "other classes still at prior")
      (is (= 0.5 (double (get by-class :fire-pattern)))
          "other classes still at prior"))))

(deftest learn-actions-carry-rationale-test
  (testing ":learn-action-class actions carry a human-readable rationale"
    (let [learn-actions (->> (ap/propose ap/bootstrap-proposer dummy-state)
                             (filter #(= :learn-action-class (:type %))))]
      (is (every? string? (map :rationale learn-actions))))))

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
