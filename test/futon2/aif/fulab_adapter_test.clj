(ns futon2.aif.fulab-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.adapter :as adapter]
            [futon2.aif.adapters.fulab :as fulab]))

(deftest generic-outcome-size-surplus-preserves-temperature-behaviour
  (testing "three outcome words produce surplus two and the former tau"
    (let [result (adapter/update-beliefs
                  (fulab/new-adapter) {}
                  {:decision/id :d1 :outcome "one two three"})]
      (is (= 2.0 (get-in result [:aif :outcome-size-surplus])))
      (is (= (/ 1.0 3.0) (get-in result [:aif :tau-updated])))
      (is (not (contains? (:aif result) :prediction-error)))
      (testing "the branch measures its own surplus, so nothing is reported"
        (is (= [] (get-in result [:aif :temperature-events])))))))

;; ---------------------------------------------------------------------------
;; AC7: the temperature input is typed, and sampling refuses without it.
;; ---------------------------------------------------------------------------

(deftest absent-surplus-refuses-to-sample
  (testing "AC7 replaces the pending 0.0 default: no surplus, no sample"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d0 :candidates [:a]})
          [record :as events] (get-in result [:aif :temperature-events])]
      (is (nil? (:chosen result)) "the adapter chose nothing")
      (is (true? (get-in result [:aif :refused?])))
      (is (not (contains? (:aif result) :tau))
          "no temperature is reported, where the pre-AC7 code reported 1.0")
      (is (not (contains? (:aif result) :probs)))
      (is (not (contains? (:aif result) :logits)))
      (is (= 1 (count events)))
      (is (= :absent (:status record)))
      (is (= :fulab-temperature/v1 (:producer-contract record)))
      (is (= :outcome-size-surplus-not-supplied (:reason record)))
      (is (true? (:required? record)))
      (is (not (contains? record :value)) "no value is invented")
      (is (= [{:field :outcome-size-surplus :key-present? false}]
             (:absent record)))
      (testing "the G scores it did compute are still reported"
        (is (contains? (get-in result [:aif :G-scores]) :a))))))

(deftest nil-valued-surplus-key-is-absent-not-malformed
  (testing "a key present with nil is absence, and says the key was there"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d0b :candidates [:a] :outcome-size-surplus nil})
          record (first (get-in result [:aif :temperature-events]))]
      (is (= :absent (:status record)))
      (is (= [{:field :outcome-size-surplus :key-present? true}]
             (:absent record))))))

(deftest measured-zero-surplus-is-a-reading
  (testing "a supplied 0.0 samples; it is an outcome of minimum size, not an absence"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d1b :candidates [:a] :outcome-size-surplus 0.0})]
      (is (= 1.0 (get-in result [:aif :tau])) "the pre-AC7 number, now earned")
      (is (false? (get-in result [:aif :refused?])))
      (is (= [] (get-in result [:aif :temperature-events]))))))

(deftest supplied-surplus-preserves-the-former-temperature
  (testing "uncertainty 1.0 plus surplus 1.0 gives tau 0.5, unchanged by AC7"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d1c :candidates [:a :b] :outcome-size-surplus 1.0})]
      (is (= 0.5 (get-in result [:aif :tau])))
      (is (= [] (get-in result [:aif :temperature-events]))))))

(deftest temperature-is-required-only-when-the-call-would-sample
  (testing "a caller-supplied :chosen is not overturned by an unread surplus"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d4 :candidates [:a :b] :chosen :b})
          record (first (get-in result [:aif :temperature-events]))]
      (is (= :b (:chosen result)) "the caller's choice stands")
      (is (false? (get-in result [:aif :refused?])))
      (is (not (contains? (:aif result) :tau)) "but no temperature is invented")
      (is (= :absent (:status record)))
      (is (false? (:required? record)) "reported, and marked not required")))
  (testing "an empty candidate set has nothing to sample"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d5 :candidates []})]
      (is (nil? (:chosen result)))
      (is (false? (get-in result [:aif :refused?])))
      (is (false? (:required? (first (get-in result [:aif :temperature-events]))))))))

(deftest signed-value-is-refused-at-surplus-seam
  (testing "a canonical signed-error-shaped value cannot be silently clamped"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d2 :candidates [:a] :outcome-size-surplus -2.0})
          record (first (get-in result [:aif :temperature-events]))]
      (is (nil? (:chosen result)))
      (is (true? (get-in result [:aif :refused?])))
      (is (not (contains? (:aif result) :tau)))
      (is (= :refused (:status record)))
      (is (= :signed-value-at-surplus-seam (:reason record)))
      (is (= {:field :outcome-size-surplus :status :negative :value -2.0}
             (:offending record))))))

(deftest non-finite-surplus-is-refused
  (testing "NaN, an infinity and a non-number each refuse, and name themselves"
    (doseq [bad [##NaN ##Inf "two"]]
      (let [result (adapter/select-pattern
                    (fulab/new-adapter) {}
                    {:decision/id :d6 :candidates [:a] :outcome-size-surplus bad})
            record (first (get-in result [:aif :temperature-events]))]
        (is (nil? (:chosen result)) (str "chose nothing on " (pr-str bad)))
        (is (= :refused (:status record)))
        (is (= :malformed-outcome-size-surplus (:reason record)))
        (is (= :not-finite (get-in record [:offending :status])))
        (is (= :outcome-size-surplus (get-in record [:offending :field])))))))

(deftest canonical-prediction-error-key-is-refused
  (testing "the old ambiguous key cannot enter the Fulab temperature seam"
    (let [result (adapter/select-pattern
                  (fulab/new-adapter) {}
                  {:decision/id :d3 :candidates [:a] :prediction-error -2.0})
          record (first (get-in result [:aif :temperature-events]))]
      (is (nil? (:chosen result)))
      (is (true? (get-in result [:aif :refused?])))
      (is (not (contains? (:aif result) :tau)))
      (is (= :refused (:status record)))
      (is (= :canonical-signed-error-at-surplus-seam (:reason record)))
      (is (= {:field :prediction-error :status :wrong-quantity :value -2.0}
             (:offending record)))))
  (testing "it is named even when a well-formed surplus was also supplied"
    (let [record (fulab/temperature-record {:outcome-size-surplus 1.0
                                            :prediction-error -2.0})]
      (is (= :canonical-signed-error-at-surplus-seam (:reason record)))))
  (testing "and it refuses the generic update's own measured surplus"
    (let [result (adapter/update-beliefs
                  (fulab/new-adapter) {}
                  {:decision/id :d3b :outcome "one two three" :prediction-error -2.0})
          record (first (get-in result [:aif :temperature-events]))]
      (is (not (contains? (:aif result) :tau-updated)))
      (is (= :refused (:status record))))))

(deftest pattern-action-update-keeps-its-counts-when-the-surplus-is-unread
  (testing "the evidence update is the belief change; tau is only reported"
    (let [result (adapter/update-beliefs
                  (fulab/new-adapter) {}
                  {:decision/id :d7 :pattern/id :p1 :pattern/action :implement})
          record (first (get-in result [:aif :temperature-events]))]
      (is (= 1 (get-in result [:aif/state :pattern-evidence :p1 :implement]))
          "the counts still update")
      (is (not (contains? (:aif result) :tau-updated)) "no temperature is invented")
      (is (= :absent (:status record)))
      (is (false? (:required? record)))))
  (testing "a supplied surplus still yields the temperature it used to"
    (let [result (adapter/update-beliefs
                  (fulab/new-adapter) {}
                  {:decision/id :d8 :pattern/id :p1 :pattern/action :implement
                   :outcome-size-surplus 1.0})]
      (is (= 0.5 (get-in result [:aif :tau-updated])))
      (is (= [] (get-in result [:aif :temperature-events]))))))

(deftest temperature-events-is-present-only
  (testing "the projection is empty exactly when the surplus was read"
    (is (= [] (fulab/temperature-events
               (fulab/temperature-record {:outcome-size-surplus 0.0}))))
    (is (= 1 (count (fulab/temperature-events
                     (fulab/temperature-record {})))))
    (is (= 1 (count (fulab/temperature-events
                     (fulab/temperature-record {:outcome-size-surplus -1.0})))))
    (is (= [] (fulab/temperature-events nil)))))

(deftest supplied-and-computed-surplus-are-told-apart
  (testing "the basis names which producer measured the surplus"
    (is (= :outcome-size-surplus
           (:basis (fulab/temperature-record {:outcome-size-surplus 2.0})))
        "the caller supplied it")
    (is (= :computed-outcome-size
           (:basis (fulab/temperature-record {:outcome-size-surplus 1.0}
                                             :computed-outcome-size)))
        "the generic update measured it off the observed outcome")
    (testing "and the basis is only stamped on a reading"
      (is (not (contains? (fulab/temperature-record {} :computed-outcome-size)
                          :basis))))))
