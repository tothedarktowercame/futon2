(ns futon2.aif.accumulation-bmr-test
  "ITEM6-ADAPTER-I tests: seven-column (channel-given-status) BMR adapter
   and aggregate score receipt. Fixture and bad cases per
   holes/labs/wm-contract/ITEM6-NORMALIZER-D.md sections 2 and 5.

   Label: prototyping our way forward, not closure of the proof."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.accumulation-bmr :as accum-bmr]
            [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.machine-accumulation :as acc]
            [futon2.aif.observation :as obs]))

;; ITEM6-NORMALIZER-D section 2 fixture, reproduced exactly and driven
;; through the REAL accumulator (machine_accumulation initialize + step),
;; not a hand-built matrix: all-ones init, one tick with o_c = c/14 and
;; mu_s = s/28, so A[c,s] = 1 + c*s/392 (exact rationals).

(def ^:private channels (vec (sort obs/observation-channels)))
(def ^:private statuses (vec (sort belief/status-set)))

(defn- fixture-state []
  (let [o (zipmap channels (map #(/ % 14) (range 1 15)))
        mu (zipmap statuses (map #(/ % 28) (range 1 8)))
        init (acc/initialize channels statuses 1)]
    (assert (:ok init))
    (acc/step init {:id "item6-fixture-1" :previous-id nil
                    :observation o :belief mu})))

(def ^:private fixture-deltas {[:active-repo-ratio :addressed] 1})

(defn- approx= [^double expected ^double actual ^double tol]
  (< (Math/abs (- expected actual)) tol))

(deftest fixture-matrix-is-the-real-accumulators
  (testing "A[c,s] = 1 + c*s/392 falls out of the real accumulator step"
    (let [state (fixture-state)]
      (is (:ok state))
      (is (acc/recurrence-valid?
           (acc/initialize channels statuses 1)
           {:id "item6-fixture-1" :previous-id nil
            :observation (zipmap channels (map #(/ % 14) (range 1 15)))
            :belief (zipmap statuses (map #(/ % 28) (range 1 8)))}
           state))
      (is (= (+ 1 (/ (* 1 1) 392))
             (get-in state [:concentrations :active-repo-ratio :addressed])))
      (is (= (+ 1 (/ (* 14 7) 392))
             (get-in state [:concentrations :ticks-firing-ratio :strengthened]))))))

(deftest score-receipt-matches-item6-fixture
  (let [state (fixture-state)
        receipt (accum-bmr/score state fixture-deltas)]
    (testing "receipt envelope"
      (is (= :wm/accumulation-bmr-score-v1 (:schema receipt)))
      (is (= :channel-given-status (:declared-model receipt)))
      (is (= 7 (:factors receipt)))
      (is (= "prototyping our way forward, not closure of the proof"
             (:label receipt)))
      (is (= {:threshold -3 :applied false} (:rule receipt)))
      (is (not (contains? receipt :accepted?))))
    (testing "aggregate delta-F is the channel-given-status score"
      (is (approx= 0.016404153337 (:delta-f receipt) 1e-9)))
    (testing "the one changed factor carries all of it"
      (let [per-factor (:per-factor receipt)
            addressed (first (filter #(= :addressed (:status %)) per-factor))]
        (is (= 7 (count per-factor)))
        (is (= statuses (mapv :status per-factor)))
        (is (approx= 0.016404153337 (:delta-f addressed) 1e-9))
        (doseq [factor (remove #(= :addressed (:status %)) per-factor)]
          (is (approx= 0.0 (:delta-f factor) 1e-12)
              (str "unchanged factor " (:status factor) " must cancel")))))
    (testing "the two other readings are NOT what this adapter returns"
      ;; Compute the joint and status-given-channel readings live from the
      ;; same state via the real bmr machinery and assert both differ, so
      ;; the adapter's factorisation is pinned and cannot silently drift
      ;; to a flattened or per-row call.
      (let [a (accum-bmr/parent-prior state)
            big-a (accum-bmr/factors state) ;; [status][channel]
            rows (mapv (fn [c]
                         (mapv (fn [s] (get-in state [:concentrations c s]))
                               statuses))
                       channels)
            prior-rows (mapv (fn [_c] (mapv (fn [_s] 1) statuses)) channels)
            reduced-rows (assoc-in prior-rows [0 0] 2)
            joint-score (:delta-F (bmr/bayesian-model-reduction
                                   (vec (mapcat identity a))
                                   (vec (apply mapcat vector big-a))
                                   (vec (mapcat identity
                                                (assoc-in a [0 0]
                                                          (inc (get-in a [0 0])))))))
            row-scores (mapv bmr/bayesian-model-reduction
                             prior-rows rows reduced-rows)
            joint-delta-f joint-score
            status-given-channel-delta-f (reduce + (map :delta-F row-scores))]
        (is (approx= 0.071195702167 joint-delta-f 1e-9))
        (is (approx= 0.007604599385 status-given-channel-delta-f 1e-9))
        (is (not (approx= joint-delta-f (:delta-f receipt) 1e-6)))
        (is (not (approx= status-given-channel-delta-f (:delta-f receipt) 1e-6)))))))

(deftest factors-respect-declared-support-order
  (let [state (fixture-state)
        fs (accum-bmr/factors state)]
    (is (= 7 (count fs)))
    (is (every? #(= 14 (count %)) fs))
    (testing "column s is [A[c1,s] .. A[c14,s]] in declared channel order"
      (doseq [[si s] (map-indexed vector statuses)
              [ci c] (map-indexed vector channels)]
        (is (= (get-in state [:concentrations c s])
               (get-in fs [si ci])))))))

(deftest parent-prior-is-the-declared-initialization
  (testing "read back from a state produced by machine_accumulation's
            initialization with a NON-unit prior, so uniform-by-assumption
            cannot pass"
    (let [init (acc/initialize channels statuses 3/2)
          prior (accum-bmr/parent-prior init)]
      (is (= 7 (count prior)))
      (is (every? #(= (vec (repeat 14 3/2)) %) prior))
      (is (= 3/2 (get-in init [:initialization :prior])))))
  (testing "fixture state's prior is the scalar 1 broadcast"
    (is (every? #(= (vec (repeat 14 1)) %)
                (accum-bmr/parent-prior (fixture-state))))))

(deftest proposal-is-a-positive-soft-prior
  (let [state (fixture-state)
        reduced (accum-bmr/proposal state fixture-deltas)
        prior (accum-bmr/parent-prior state)]
    (is (= (inc (get-in prior [0 0])) (get-in reduced [0 0])))
    (is (= (vec (rest prior)) (vec (rest reduced))))))

(deftest bad-cases-refuse-or-absent
  (let [state (fixture-state)]
    (testing "a state without support order is refused"
      (let [unordered (dissoc state :support)]
        (is (= {:status :refused :kind :unordered-supports}
               (accum-bmr/factors unordered)))
        (is (= :unordered-supports (:kind (accum-bmr/score unordered fixture-deltas))))))
    (testing "a missing cell is a typed absence on the receipt, no :delta-f"
      (let [broken (update-in state [:concentrations :loop-health]
                              dissoc :refined)
            receipt (accum-bmr/score broken fixture-deltas)]
        (is (= :absent (:status receipt)))
        (is (= :missing-cell (:kind receipt)))
        (is (= :loop-health (:channel receipt)))
        (is (= :refined (:status-key receipt)))
        (is (not (contains? receipt :delta-f)))
        (is (= :absent (:status (accum-bmr/factors broken))))))
    (testing "zero or negative deltas are refused"
      (is (= :non-positive-delta
             (:kind (accum-bmr/proposal state {[:loop-health :refined] 0}))))
      (is (= :non-positive-delta
             (:kind (accum-bmr/score state {[:loop-health :refined] -1})))))
    (testing "a delta on a cell outside the supports is refused"
      (is (= :unknown-cell
             (:kind (accum-bmr/proposal state {[:not-a-channel :addressed] 1}))))
      (is (= :unknown-cell
             (:kind (accum-bmr/score state {[:loop-health :not-a-status] 1})))))))

(deftest replay-is-deterministic
  (let [state (fixture-state)
        receipt-1 (accum-bmr/score state fixture-deltas)
        receipt-2 (accum-bmr/score (fixture-state) fixture-deltas)
        receipt-3 (accum-bmr/score state {[:active-repo-ratio :addressed] 2})]
    (is (= (pr-str receipt-1) (pr-str receipt-2)))
    (is (not= (get-in receipt-1 [:inputs :deltas-digest])
              (get-in receipt-3 [:inputs :deltas-digest])))
    (is (= (get-in receipt-1 [:inputs :support-order])
           {:observation channels :state statuses}))))
