(ns futon2.aif.h4-production-stage-test
  "H4 at the production decision, on the Q5 reading-family route: a menu
  policy whose Prefix-F is not supplied refuses selection, and the exact
  admitted Prefix-F population proceeds to the one selector.

  Migrated 2026-10-15 (Q5 route ledger). Retired with the interpreted route:
  the assertion that interpreted-construction admission declines a missing
  three-halves square first; that admission no longer exists. Its claim,
  that an earlier typed boundary precedes Prefix-F, now lives in the
  model-contract and debt gates (reading-model-contracts-test)."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)

(deftest actual-production-decision-refuses-missing-prefix
  (let [fx (route/fixture)
        ;; the policy keys the decision compares, from a run on the complete ledger
        keys (vals (route/policy-keys (:decision (route/decide fx))))
        unread-ledger {:steps [] :read [] :unread [] :dir-status {:absent :no-flights-dir}}
        refusal (route/refusal-of #(route/decide fx 1 {:conditioning-steps unread-ledger}))
        selected (route/decide fx 1 {:conditioning-steps
                                     (route/with-prefixes fx (zipmap keys [0.0 0.0]))})]
    (is (= 2 (count keys)))
    (is (= :free-energy-not-supplied (get-in refusal [:refusal :kind]))
        "unknown history: Prefix-F not supplied, the selector refuses")
    (is (= 2 (get-in refusal [:refusal :detail :n-candidates])))
    (is (= 2 (get-in refusal [:refusal :detail :n-without-f])))
    (is (= 1 (:selector-calls selected)))
    (is (some? (get-in selected [:decision :action]))
        "the exact admitted Prefix-F population proceeds to selection")
    (is (every? #(= :computed (:f-status %))
                (get-in selected [:decision :selection-certificate :candidates])))))
