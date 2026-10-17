(ns futon2.aif.reading-policy-preference-test
  "Positive and adversarial tests for the reading-policy preference receipt.

  Positive cases run the REAL shape scorer (cascade_shape_g/score-arranged)
  on a two-pattern arrangement, so the records under validation are actual
  scored reading-policy records, not hand-built lookalikes."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.reading-policy-preference :as rpp]))

(def two-pattern-cascade
  {:nodes [{:pattern :a/first :roles ["goal"]}
           {:pattern :b/second :roles []}]
   :edges [{:from :a/first :to :b/second :kind :precedes}]})

(defn scored []
  (shape/score-arranged :test/two :two two-pattern-cascade))

(defn progress-token-count [record]
  (count (get-in (meta (:scorer-result record))
                 [:cascade-scoring :model :progress-tokens])))

(defn without-model-authority [record]
  (update record :scorer-result #(with-meta (vec %) nil)))

(deftest mirror-matches-scorer-preference
  (testing "the local recompute is the same function the scorer ran"
    (let [priv #'shape/progress-preference]
      (is (= (priv 3 4) (rpp/recompute-progress-preference 3 4)))
      (is (= (priv 0 2) (rpp/recompute-progress-preference 0 2))))))

(deftest certifies-real-scored-record
  (let [record (scored)
        receipt (rpp/validate record)]
    (testing "the real scorer's record certifies with one C"
      (is (= :certified (:status receipt)) receipt)
      (is (= (keyword "wm/reading-policy-preference-receipt/v1") (:schema receipt)))
      (is (= (:horizon record) (:horizon receipt)))
      (is (= (:preference-at-each-step record) (:preference receipt)))
      (is (= (:g record) (:g receipt)))
      (is (true? (get-in receipt [:checks :normalized])))
      (is (true? (get-in receipt [:checks :one-declared-c]))))
    (testing "the authority binds to the shape scorer's declared model"
      (is (= "cascade_shape_g arrangement model" (get-in receipt [:authority :source])))
      (is (= :synthetic (get-in receipt [:authority :provenance-status])))
      (is (= :absent (get-in receipt [:authority :pins :status])))
      (is (true? (get-in receipt [:checks :schedule-recomputed])))
      (is (contains? receipt :authority)))
    (testing "old live-C is typed diagnostic-only, never a selector"
      (is (= :diagnostic-only (get-in receipt [:live-c :role])))
      (is (false? (get-in receipt [:live-c :selected]))))))

(deftest refuses-competing-preferences
  (testing "a second, different schedule in a declared-C slot refuses (§7.5.5)"
    (let [record (scored)
          m0 (meta (:scorer-result record))
          model (get-in m0 [:cascade-scoring :model])
          other (assoc-in model [:progress-preference]
                          (update-vals (:progress-preference model)
                                       #(assoc % [0 false] 0.5)))
          tampered (update record :scorer-result with-meta
                           (assoc-in m0 [:cascade-scoring :model] other))
          receipt (rpp/validate tampered)]
      (is (= :refused (:status receipt)))
      (is (= :competing-preferences (:kind receipt)))
      (is (contains? (set (:found receipt)) :model-progress-preference)))))

(deftest refuses-non-normalized-schedule
  (testing "one step whose weights do not sum to 1 refuses with that step"
    (let [record (without-model-authority (scored))
          broken (assoc record :preference-at-each-step
                        (update (:preference-at-each-step record) 1
                                #(assoc % [0 false] 0.9)))
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :preference-not-normalized (:kind receipt)))
      (is (= [1] (:steps receipt))))))

(deftest refuses-horizon-mismatch
  (testing "steps not covering 1..horizon refuses with both step sets"
    (let [record (scored)
          ;; claim a horizon the schedule does not cover
          broken (assoc record :horizon (inc (:horizon record)))
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :preference-steps-mismatch (:kind receipt))))
    (testing "model horizon disagreeing with the record's refuses"
      (let [record (scored)
            tampered (update record :scorer-result
                             with-meta (assoc-in (meta (:scorer-result record))
                                                 [:cascade-scoring :model :horizon] 7))
            receipt (rpp/validate tampered)]
        (is (= :refused (:status receipt)))
        (is (= :horizon-mismatch (:kind receipt)))))))

(deftest refuses-unrecomputable-schedule
  (testing "a schedule the model's own n/horizon cannot reproduce refuses"
    (let [record (scored)
          n (progress-token-count record)
          wrong (rpp/recompute-progress-preference (inc n) (:horizon record))
          m0 (meta (:scorer-result record))
          altered (-> record
                      (assoc :preference-at-each-step wrong)
                      (update :scorer-result with-meta
                              (assoc-in m0 [:cascade-scoring :model :progress-preference] wrong)))
          receipt (rpp/validate altered)]
      (is (= :refused (:status receipt)))
      (is (= :preference-not-recomputable (:kind receipt))))))

(deftest refuses-missing-authority-and-terms
  (testing "no schedule anywhere refuses with a precise absence"
    (let [record (scored)
          bare (dissoc record :preference-at-each-step :scorer-result)
          receipt (rpp/validate bare)]
      (is (= :refused (:status receipt)))
      (is (= :preference-schedule-absent (:kind receipt)))))
  (testing "dropped G terms refuse naming them"
    (let [record (scored)
          broken (-> record (dissoc :risk) (assoc :ambiguity "not-a-number"))
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :g-terms-absent (:kind receipt)))
      (is (= [:risk] (:missing receipt)))))
  (testing "non-finite terms refuse naming them"
    (let [record (scored)
          broken (assoc record :ambiguity ##NaN)
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :g-terms-not-finite (:kind receipt)))))
  (testing "record terms disagreeing with the certificate refuse"
    (let [record (scored)
          broken (assoc record :risk (+ 1e9 (or (:risk record) 0.0)))
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :g-composition-mismatch (:kind receipt))
          "the scorer's progress term is intact; the forged composed risk is caught at composition"))))

(deftest authority-absence-after-metadata-loss
  (testing "a JSON-shaped record (no metadata) refuses with a typed
            authority absence, never certifies unbound"
    (let [record (without-model-authority (scored))
          receipt (rpp/validate record)]
      (is (= :refused (:status receipt)))
      (is (= :preference-authority-absent (:kind receipt)))
      (is (= :model-provenance-not-retained-in-record (:architectural-gap receipt)))
      (is (some #{:model-provenance-not-retained-in-record}
                rpp/architectural-gaps)))))

(deftest refuses-unknown-model-kind
  (testing "an authority whose model kind has no recomputation rule refuses"
    (let [record (scored)
          m0 (meta (:scorer-result record))
          tampered (update record :scorer-result with-meta
                           (assoc-in m0 [:cascade-scoring :model :kind] :mystery-model))
          receipt (rpp/validate tampered)]
      (is (= :refused (:status receipt)))
      (is (= :preference-kind-not-recomputable (:kind receipt)))
      (is (= :mystery-model (:model-kind receipt))))))

(deftest refuses-negative-mass
  (testing "a negative weight paired with one above 1 sums to 1 but is not
            a distribution; it refuses as non-normalized"
    (let [record (without-model-authority (scored))
          bad {[0 false] -0.25, [1 true] 1.25}
          broken (assoc record :preference-at-each-step
                        (update (:preference-at-each-step record) 1 (constantly bad)))
          receipt (rpp/validate broken)]
      (is (= :refused (:status receipt)))
      (is (= :preference-not-normalized (:kind receipt)))
      (is (= [1] (:steps receipt)))))
  (testing "the helper itself rejects negative mass"
    (is (false? (rpp/normalized-distribution? {[0 false] -0.25 [1 true] 1.25})))
    (is (true? (rpp/normalized-distribution? {[0 false] 0.25 [1 true] 0.75})))))

(deftest competing-detection-is-set-based
  (testing "A,B,A stated in three slots is still exactly two distinct Cs,
            with a deterministic stated-at diagnostic"
    (let [record (scored)
          m0 (meta (:scorer-result record))
          model (get-in m0 [:cascade-scoring :model])
          sched-a (:preference-at-each-step record)
          sched-b (rpp/recompute-progress-preference 9 (:horizon record))
          ;; three stated slots: record=A, model-progress=B, and a second
          ;; restatement of A on the record level is impossible, so state
          ;; A,B,A via record + model-progress + a class-preference copy of A
          model3 (-> model
                     (assoc :progress-preference sched-b)
                     (assoc :class-preference sched-a))
          tampered (-> record
                       (update :scorer-result with-meta
                               (assoc-in m0 [:cascade-scoring :model] model3)))
          receipt (rpp/validate tampered)]
      (is (= :refused (:status receipt)))
      (is (= :competing-preferences (:kind receipt)))
      (is (= 2 (count (:stated-at receipt)))))))
