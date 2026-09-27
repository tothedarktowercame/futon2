(ns futon2.aif.accumulation-bmr-test
  "ITEM6-ADAPTER-I tests: seven-column (channel-given-status) BMR adapter
   and aggregate score receipt. Fixture and bad cases per
   holes/labs/wm-contract/ITEM6-NORMALIZER-D.md sections 2 and 5.

   Label: prototyping our way forward, not closure of the proof."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.accumulation-bmr :as accum-bmr]
            [futon2.aif.belief :as belief]
            [futon2.aif.bmr :as bmr]
            [futon2.aif.machine-accumulation :as acc]
            [futon2.aif.observation :as obs]
            [futon2.aif.trace :as trace])
  (:import [java.io PushbackReader]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

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

;; ---------------------------------------------------------------------
;; ITEM6-CONSUMER-I: the declared proposal family scored from the
;; PUBLISHED accumulation, retained as one receipt on every route.
;; Label: prototyping our way forward, not closure of the proof.

(defn- declared-family []
  (with-open [r (PushbackReader. (io/reader "holes/labs/wm-contract/aif-equations.edn"))]
    (get-in (first (filter #(= :model-reduction (:id %))
                           (:equations (edn/read r))))
            [:declared-model :proposal-family])))

(defn- expected-family-score
  "Independent computation of a common-profile member's aggregate delta-F:
   a'_s = k * (sum_c a[c,s]) * (1/14) per cell, per-factor
   bmr/bayesian-model-reduction on hand-built vectors, summed over the
   seven status factors. Tolerance for comparisons: 1e-9."
  [state k]
  (let [a (accum-bmr/parent-prior state)
        big-a (accum-bmr/factors state)]
    (reduce +
            (map (fn [a-v A-v]
                   (let [n (count a-v)
                         a'-v (vec (repeat n (/ (* k (reduce + a-v)) n)))]
                     (:delta-F (bmr/bayesian-model-reduction a-v A-v a'-v))))
                 a big-a))))

(defn- publishable-record [state id]
  {:run/id id
   :decision {:action {:kind :cascade-candidate :target :fixture}}
   :accumulation-state state
   :accumulation-update-input {:previous-id nil :tick-id id}
   :accumulation-initialization (:initialization state)
   :accumulation-receipt {:status :accumulated :tick-id id}})

(defn- with-temp-dir [f]
  (let [d (.toFile (Files/createTempDirectory "bmr-consumer-" (make-array FileAttribute 0)))]
    (try (f d)
         (finally (doseq [x (reverse (file-seq d))] (io/delete-file x true))))))

(deftest declared-family-is-the-registry-declaration
  (let [family (declared-family)]
    (is (= 13 (count family)))
    (is (= :wm/accumulation-bmr-proposal-family-v1 (:id family)))
    (is (= "prototyping our way forward, not closure of the proof" (:label family)))
    (is (true? (:fixed-independently-of-posterior family)))
    (is (= {:threshold -3 :applied false :sum-compared-once true} (:rule family)))
    (is (= [:identity :common-profile-k2 :common-profile-k4]
           (mapv :id (:proposals family))))
    (doseq [p (:proposals family)]
      (is (= #{:id :kind :reference-profile :concentration-multiplier :author :reason}
             (set (keys p))))
      (is (= :soft-prior-constraint (:kind p)))
      (is (= :uniform-channels (:reference-profile p)))
      (is (= "kimi-4, ITEM6-CONSUMER-I, 2026-09-27" (:author p))))))

(deftest consumer-scores-the-published-accumulation
  (with-temp-dir
   (fn [dir]
     (let [state (fixture-state)
           publication (trace/write-trace! (publishable-record state "t1")
                                           :dir (str dir) :date-str "2026-09-27"
                                           :return-record? true)
           record (:record publication)
           receipt (:bmr-receipt record)
           proposals (into {} (map (juxt :id identity)) (:proposals receipt))]
       (testing "the receipt envelope"
         (is (= :wm/accumulation-bmr-v1 (:schema receipt)))
         (is (= :scored (:status receipt)))
         (is (= "prototyping our way forward, not closure of the proof" (:label receipt)))
         (is (= :wm/accumulation-bmr-proposal-family-v1 (:family receipt)))
         (is (= {:threshold -3 :applied false :sum-compared-once true} (:rule receipt)))
         (is (not (contains? receipt :accepted?)))
         (is (= {:observation channels :state statuses} (:support-order receipt)))
         (is (string? (:origin-digest receipt)))
         (is (string? (:accumulation-digest receipt)))
         (is (string? (:support-order-digest receipt))))
       (testing "identity is exactly 0, by the scorer's own computation (a' = a), not a constant"
         (is (zero? (:delta-f (proposals :identity))))
         (is (= 7 (count (:per-factor (proposals :identity)))))
         (is (every? #(zero? (:delta-f %)) (:per-factor (proposals :identity))))
         (is (= (:delta-f (proposals :identity))
                (:delta-f (accum-bmr/score state {})))
             "the identity member IS the scorer run with no deltas"))
       (testing "profile proposals match the independent computation (tol 1e-9)"
         (is (approx= (expected-family-score state 2)
                      (:delta-f (proposals :common-profile-k2)) 1e-9))
         (is (approx= (expected-family-score state 4)
                      (:delta-f (proposals :common-profile-k4)) 1e-9)))
       (testing "the SAME receipt rides under [:decision :accumulation-bmr]"
         (is (= receipt (get-in record [:decision :accumulation-bmr]))))
       (testing "readback from disk is byte-identical: no second scoring"
         (let [disk (peek (:records (trace/read-history-strict 1 :dir (str dir))))]
           (is (= (pr-str receipt) (pr-str (:bmr-receipt disk))))
           (is (= (pr-str record) (pr-str disk)))))))))

(deftest consumer-absences-carry-the-actual-cause
  (let [family (declared-family)]
    (testing "a stale predecessor absence is the cause, and there are no proposals"
      (let [cause {:status :absent :reason :accumulation-stale-predecessor
                   :expected nil :actual "a"}
            receipt (accum-bmr/receipt-for-record {:accumulation-receipt cause} family)]
        (is (= :absent (:status receipt)))
        (is (= :accumulation-stale-predecessor (:reason receipt)))
        (is (= cause (:cause receipt)))
        (is (not (contains? receipt :proposals)))))
    (testing "a configuration refusal absence is the cause"
      (let [cause {:status :absent :reason :accumulation-configuration-invalid}
            receipt (accum-bmr/receipt-for-record {:accumulation-receipt cause} family)]
        (is (= :accumulation-configuration-invalid (:reason receipt)))
        (is (= cause (:cause receipt)))))
    (testing "state present but unordered supports -> the adapter kind"
      (let [state (dissoc (fixture-state) :support)
            receipt (accum-bmr/receipt-for-record
                     {:accumulation-state state
                      :accumulation-initialization (:initialization state)
                      :accumulation-receipt {:status :accumulated}}
                     family)]
        (is (= :absent (:status receipt)))
        (is (= :unordered-supports (:reason receipt)))))
    (testing "state present with a missing cell -> the adapter absence kind"
      (let [state (update-in (fixture-state) [:concentrations :loop-health]
                             dissoc :refined)
            receipt (accum-bmr/receipt-for-record
                     {:accumulation-state state
                      :accumulation-initialization (:initialization state)
                      :accumulation-receipt {:status :accumulated}}
                     family)]
        (is (= :absent (:status receipt)))
        (is (= :missing-cell (:reason receipt)))))
    (testing "a numerical failure is :bmr-unavailable, never an exception"
      (let [broken-family (assoc-in family [:proposals 1] {:id :broken})
            receipt (accum-bmr/receipt-for-record
                     {:accumulation-state (fixture-state)
                      :accumulation-initialization (:initialization (fixture-state))
                      :accumulation-receipt {:status :accumulated}}
                     broken-family)]
        (is (= :absent (:status receipt)))
        (is (= :bmr-unavailable (:reason receipt)))
        (is (string? (get-in receipt [:error :class])))))))

(deftest zero-data-seed-scores-zero-for-every-proposal
  ;; A = a (initialized, never stepped): each factor's delta-F cancels.
  (let [state (acc/initialize channels statuses 1)
        family (declared-family)
        receipt (accum-bmr/receipt-for-record
                 {:accumulation-state state
                  :accumulation-initialization (:initialization state)
                  :accumulation-receipt {:status :accumulated}}
                 family)]
    (is (= :scored (:status receipt)))
    (doseq [p (:proposals receipt)]
      (is (approx= 0.0 (:delta-f p) 1e-9) (str (:id p))))))

(deftest factorized-sum-differs-from-the-wrong-flat-score
  ;; The 98-category joint reading of the same k2 proposal is NOT the
  ;; receipt's per-factor sum.
  (let [state (fixture-state)
        family (declared-family)
        receipt (accum-bmr/receipt-for-record
                 {:accumulation-state state
                  :accumulation-initialization (:initialization state)
                  :accumulation-receipt {:status :accumulated}}
                 family)
        scored (first (filter #(= :common-profile-k2 (:id %)) (:proposals receipt)))
        a (accum-bmr/parent-prior state)
        big-a (accum-bmr/factors state)
        flat-a (vec (mapcat identity a))
        flat-A (vec (apply mapcat vector big-a))
        flat-a' (vec (mapcat (fn [a-v]
                               (let [n (count a-v)]
                                 (repeat n (/ (* 2 (reduce + a-v)) n))))
                             a))
        joint (:delta-F (bmr/bayesian-model-reduction flat-a flat-A flat-a'))]
    (is (number? joint))
    (is (not (approx= joint (:delta-f scored) 1e-6)))))

(deftest support-reorder-out-of-band-changes-the-recorded-digest
  ;; Finding 3 of ITEM6-ADAPTER-I: `step` never re-checks support order, so
  ;; an out-of-band reorder silently changes the factorisation. The guard
  ;; is the RECORDED digest, not a refusal.
  (let [state (fixture-state)
        reordered (update-in state [:support :observation] (comp vec rseq))
        family (declared-family)
        record-for (fn [s] {:accumulation-state s
                            :accumulation-initialization (:initialization s)
                            :accumulation-receipt {:status :accumulated}})
        receipt (accum-bmr/receipt-for-record (record-for state) family)
        receipt' (accum-bmr/receipt-for-record (record-for reordered) family)]
    (is (= :scored (:status receipt')))
    (is (not= (:support-order-digest receipt) (:support-order-digest receipt')))
    (is (= (vec (rseq channels)) (get-in receipt' [:support-order :observation])))))


(deftest production-shaped-double-prior-scores-the-profile-proposals
  ;; The live config declares the scalar prior as the double 1.0
  ;; (holes/labs/wm-contract/machine-accumulation-config.edn). Before the
  ;; adapter rationalized it, the k2/k4 deltas were doubles and were refused
  ;; per proposal as :non-positive-delta, so only the identity control ever
  ;; scored in production. Bad case: the same state with prior 1.0 must
  ;; score k2/k4 and agree with the rational-prior fixture.
  (with-temp-dir
   (fn [dir]
     (let [o (zipmap channels (map #(/ % 14) (range 1 15)))
           mu (zipmap statuses (map #(/ % 28) (range 1 8)))
           init (acc/initialize channels statuses 1.0)
           _ (assert (:ok init))
           state (acc/step init {:id "item6-fixture-double" :previous-id nil
                                 :observation o :belief mu})
           publication (trace/write-trace! (publishable-record state "t-double")
                                           :dir (str dir) :date-str "2026-09-27"
                                           :return-record? true)
           receipt (:bmr-receipt (:record publication))
           proposals (into {} (map (juxt :id identity)) (:proposals receipt))
           rational-state (fixture-state)]
       (is (= :scored (:status receipt)))
       (is (= 1.0 (get-in state [:initialization :prior])) "the state still records the double it was given")
       (is (zero? (:delta-f (proposals :identity))))
       (doseq [k [:common-profile-k2 :common-profile-k4]]
         (is (not (contains? (proposals k) :status)) (str (name k) " is scored, not refused"))
         (is (number? (:delta-f (proposals k))) (name k)))
       (is (approx= (expected-family-score rational-state 2) (:delta-f (proposals :common-profile-k2)) 1e-9))
       (is (approx= (expected-family-score rational-state 4) (:delta-f (proposals :common-profile-k4)) 1e-9))))))
