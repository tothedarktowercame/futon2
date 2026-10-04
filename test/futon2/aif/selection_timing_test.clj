(ns futon2.aif.selection-timing-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.selection-timing :as sut]))

(defn- clock-of [values]
  (let [values (atom values)]
    (fn [] (let [value (first @values)]
             (swap! values rest)
             value))))

(deftest checkpoints-are-ordered-non-overlapping-and-reconcile-in-milliseconds
  (let [state (sut/new-state)
        ;; begin, two checkpoints, finish's checkpoint
        clock (clock-of [0 10000000 25000000 40000000])]
    (sut/begin! state clock :strategic-scans)
    (sut/checkpoint! state clock :registry-loading)
    (sut/checkpoint! state clock :g-admission)
    (sut/finish! state clock)
    (let [receipt (sut/receipt state 55)]
      (is (= :complete (:status receipt)))
      (is (= :monotonic (:clock receipt)))
      (is (= :millisecond (:unit receipt)))
      (is (= {:strategic-scans 10 :registry-loading 15 :g-admission 15}
             (:subphase-timings-ms receipt)))
      (is (= 40 (:measured-ms receipt)))
      (is (= 15 (:gap-ms receipt))))))

(deftest parent-is-debugger-adjusted-and-dwell-is-not-fabricated-as-a-child
  (let [state (sut/new-state)
        clock (clock-of [0 100000000])]
    (sut/begin! state clock :strategic-scans)
    (sut/finish! state clock)
    ;; The phase wall was 144ms and debugger dwell was 44ms. The caller passes
    ;; the already adjusted 100ms parent, so nested time reconciles to active
    ;; machine time and no debugger pseudo-subphase appears.
    (let [receipt (sut/receipt state 100)]
      (is (= 100 (:parent-active-selection-ms receipt)))
      (is (= 0 (:gap-ms receipt)))
      (is (not (contains? (:subphase-timings-ms receipt) :debugger-dwell))))))

(deftest incomplete-and-impossible-timings-stay-typed
  (testing "an aborted attempt retains measured evidence without inventing a gap"
    (let [state (sut/new-state)
          clock (clock-of [0 7000000])]
      (sut/begin! state clock :strategic-scans)
      (sut/abort! state clock :selection-judge-threw)
      (let [receipt (sut/receipt state 20)]
        (is (= :typed-missing (:status receipt)))
        (is (= :nested-measurement-incomplete (:reason receipt)))
        (is (not (contains? receipt :gap-ms))))))
  (testing "nested time greater than its parent is a typed clock defect"
    (let [state (sut/new-state)
          clock (clock-of [0 11000000])]
      (sut/begin! state clock :strategic-scans)
      (sut/finish! state clock)
      (let [receipt (sut/receipt state 10)]
        (is (= :typed-missing (:status receipt)))
        (is (= :nested-duration-exceeds-parent (:reason receipt)))
        (is (not (contains? receipt :gap-ms))))))
  (testing "a regressing clock cannot fabricate a zero-duration subphase"
    (let [state (sut/new-state)
          clock (clock-of [10 9 20])]
      (sut/begin! state clock :strategic-scans)
      (sut/checkpoint! state clock :registry-loading)
      (sut/finish! state clock)
      (let [receipt (sut/receipt state 20)]
        (is (= :typed-missing (:status receipt)))
        (is (= :monotonic-clock-invalid (:reason receipt)))
        (is (= :monotonic-clock-regressed
               (get-in receipt [:errors 0 :reason])))))))
