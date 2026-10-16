(ns futon2.aif.wm.q4-preference-route-test
  "Requirements Q4 on the production reading-family route: the decision's
  Q4 carrier, built from the certified preference receipts of the policies
  the one G selection compared (its posterior, after any Q6 exclusion), is
  persisted with the decision and accepted by the exporter only when the
  persisted decision recomputes it. Each compared policy keeps its own
  horizon; horizons are comparable only under the one recognized per-step
  G normalization, bound to the G terms the selector compared."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.q4-preference :as q4]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-report-card :as card]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(defn- click! [fx dir]
  (let [calls (atom 0)
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:run-record-dir (str dir)
                        :judge-fn (fn [_]
                                    (let [r (route/decide fx)]
                                      (swap! calls + (:selector-calls r))
                                      {:judgement (judgement-for (:decision r))}))
                        :repair-system-record-fn (fn [m] {:repair/id "repair-test"
                                                          :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
    {:record (edn/read-string {:default tagged-literal} (slurp (:run-record result)))
     :calls @calls}))

(defn- q4-of [record] (get-in record [:decision :selection-certificate :q4]))

(defn- holds?
  "Requirements.Q4 (mirrored) as the exporter accepts RECORD; :nr when it
  does not."
  [record]
  (let [v (q4/verify-record record)]
    (if (= :verified (:status v)) (q4/lean-q4 (:carrier v)) :nr)))

(defn- reseal
  "RECORD with its receipts rewritten by F and the Q4 carrier recomputed,
  as a consistent forger would."
  [record f]
  (let [r (update-in record [:decision :selection-certificate :preference-receipts] (comp vec f))
        d (update (:decision r) :selection-certificate dissoc :q4)]
    (assoc-in r [:decision :selection-certificate :q4] (q4/carrier d))))

(defn- r0
  "Receipts with the first rewritten by F."
  [receipts f]
  (assoc (vec receipts) 0 (f (first receipts))))

(defn- raw-cumulative
  "RECEIPT re-declared as raw cumulative G, its raw sums set to its terms,
  so the declaration still reproduces the G that was compared."
  [receipt]
  (-> receipt
      (assoc-in [:normalization :declaration] {:law :raw-cumulative :version 1})
      (assoc-in [:normalization :raw] (get-in receipt [:normalization :terms]))))

(defn- per-policy [c k] (mapv k (:policies c)))

(defn- verdict [export]
  (:status (first (filter #(= :Q4 (:requirement %)) (card/verdict-rows export)))))

(deftest compared-policies-carry-every-term-at-every-step
  (let [{:keys [record calls]} (click! (route/fixture) (route/temp-dir "q4-"))
        c (q4-of record)]
    (is (= 1 calls))
    (is (= :certified (:status c)))
    (is (= {:status :verified :carrier c} (q4/verify-record record)) "bound to the record")
    (testing "compared = the posterior, by structural key; one record each"
      (is (= 2 (count (:compared c))))
      (is (= (set (:compared c))
             (set (map #(get-in % [:id :policy-key])
                       (get-in record [:decision :selection-certificate :candidates])))))
      (is (= (:compared c) (per-policy c :policy-key))))
    (testing "horizon, steps, grading and normalization from each policy's receipt"
      (is (= [2 2] (per-policy c :horizon)))
      (is (= [[0 1] [0 1]] (per-policy c :preference-steps) (per-policy c :graded-steps)))
      (is (= [:progressive :progressive] (per-policy c :semantics)))
      (is (= (repeat 2 {:law :per-step-capacity-and-pattern :version 1})
             (per-policy c :normalization))))
    (testing "the scorer's declaration travels in every receipt"
      (doseq [r (get-in record [:decision :selection-certificate :preference-receipts])]
        (is (= :per-step-capacity-and-pattern (get-in r [:normalization :declaration :law])))
        (is (= (:horizon r) (get-in r [:normalization :declaration :horizon-steps])))))
    (testing "every term evaluated for every compared policy"
      (is (= {:risk true :ambiguity true :information-gain true} (:g-terms c)))
      (is (= {:risk 2 :ambiguity 2 :information-gain 2} (:counts c))))
    (is (true? (holds? record)))
    (testing "the exporter and report card read the carrier"
      (let [export (facts/facts-for-record record "r" {})
            f (:facts export)]
        (is (= [2 2] (map #(% "horizon") (f "policyPreferences"))))
        (is (= (f "comparedPolicies") (map #(% "policy") (f "policyPreferences"))))
        (is (= #{"perStepCapacityAndPattern"} (set (map #(% "normalization") (f "policyPreferences")))))
        (is (contains? (f "horizonLength") "not-recomputable") "no run-wide horizon")
        (is (= :pass (verdict export)))))))

(deftest a-q6-excluded-policy-is-not-compared
  (let [fx (route/fixture)
        dir (route/temp-dir "q4-")
        _ (click! fx dir)
        {:keys [record]} (click! fx dir)
        c (q4-of record)
        excluded (get-in record [:decision :selection-certificate :q6 :excluded-keys])]
    (is (= 1 (count excluded)))
    (is (= 2 (count (get-in record [:decision :selection-certificate :preference-receipts])))
        "both policies were scored and certified")
    (is (= 1 (count (:compared c)) (count (:policies c))) "only the post-Q6 menu was compared")
    (is (not-any? (set excluded) (:compared c)))
    (is (= {:risk 1 :ambiguity 1 :information-gain 1} (:counts c)))
    (is (true? (holds? record)))))

(deftest mixed-horizons-hold-under-the-per-step-normalization
  ;; the p/a + p/c reading compares a horizon-2 and a horizon-3 policy in
  ;; one G selection
  (let [{:keys [record calls]} (click! (route/fixture ["p/a" "p/c"]) (route/temp-dir "q4-"))
        c (q4-of record)]
    (is (= 1 calls))
    (is (= :certified (:status c)))
    (is (= #{2 3} (set (per-policy c :horizon))))
    (is (every? (fn [p] (= (vec (range (:horizon p))) (:preference-steps p) (:graded-steps p)))
                (:policies c)))
    (is (true? (holds? record)))
    (let [export (facts/facts-for-record record "r" {})]
      (is (= #{2 3} (set (map #(% "horizon") ((:facts export) "policyPreferences")))))
      (is (= :pass (verdict export))))
    (testing "the same horizons as raw cumulative sums are not comparable"
      (let [raw (reseal record #(mapv raw-cumulative %))]
        (is (= :certified (:status (q4-of raw))))
        (is (= #{{:law :raw-cumulative :version 1}} (set (per-policy (q4-of raw) :normalization))))
        (is (false? (holds? raw)))
        (is (= :fail (verdict (facts/facts-for-record raw "r" {}))))))
    (testing "two laws in one comparison"
      (let [mixed (reseal record #(r0 % raw-cumulative))]
        (is (= :certified (:status (q4-of mixed))))
        (is (false? (holds? mixed)))))))

(defn- zero-information-gain
  "RECORD with the first receipt's information gain 0, consistently: raw
  sum, normalized term, recorded term, and the G both the receipt and the
  compared certificate row carry."
  [record]
  (let [receipt (get-in record [:decision :selection-certificate :preference-receipts 0])
        delta (get-in receipt [:normalization :terms :information-gain])
        pid (:policy-id receipt)
        row? #(= pid (nth (prefix-admission/candidate-key (:id %)) 2 nil))
        r (update-in record [:decision :selection-certificate :candidates]
                     (fn [rows] (mapv #(if (row? %) (update % :g + delta) %) rows)))]
    (reseal r #(r0 % (fn [x] (-> x
                                 (assoc-in [:normalization :raw :information-gain] 0.0)
                                 (assoc-in [:normalization :terms :information-gain] 0.0)
                                 (assoc-in [:g-terms :information-gain] 0.0)
                                 (update :g + delta)))))))

(deftest forged-and-lost-evidence-is-refused
  (let [{:keys [record]} (click! (route/fixture) (route/temp-dir "q4-"))]
    (doseq [[label f reason]
            [[:missing-term #(r0 % (fn [r] (update r :g-terms dissoc :information-gain))) nil]
             [:nonfinite-term #(r0 % (fn [r] (assoc-in r [:g-terms :risk] ##NaN))) nil]
             [:competing-c #(r0 % (fn [r] (assoc-in r [:preference 2 [0 false]] 0.5)))
              :preference-not-recomputable]
             [:lost-authority #(r0 % (fn [r] (dissoc r :authority))) :preference-authority-lost]
             [:horizon-not-authority's #(r0 % (fn [r] (assoc-in r [:authority :model-horizon] 3)))
              :horizon-not-the-authority's]
             [:live-c-selected #(r0 % (fn [r] (assoc r :live-c {:role :selection :selected true})))
              :live-c-not-diagnostic]
             [:receipt-for-another-g #(r0 % (fn [r] (update r :g + 1.0)))
              :receipt-not-bound-to-compared-g]
             [:compared-without-receipt (comp vec rest) :compared-policy-without-receipt]
             [:two-receipts-one-policy #(conj (vec %) (first %)) :competing-receipts]
             [:normalization-absent #(r0 % (fn [r] (dissoc r :normalization))) :normalization-absent]
             [:normalization-scorer-absent
              #(r0 % (fn [r] (assoc r :normalization {:status :absent}))) :normalization-absent]
             [:normalization-unknown-law
              #(r0 % (fn [r] (assoc-in r [:normalization :declaration :law] :per-step-capacity)))
              :normalization-unrecognized]
             [:normalization-unknown-version
              #(r0 % (fn [r] (assoc-in r [:normalization :declaration :version] 2)))
              :normalization-unrecognized]
             [:normalization-inputs-not-the-receipt's
              #(r0 % (fn [r] (assoc-in r [:normalization :declaration :horizon-steps] 3)))
              :normalization-inputs-not-the-receipt's]
             [:normalization-raw-sums-edited
              #(r0 % (fn [r] (update-in r [:normalization :raw :risk] * 2.0)))
              :normalization-not-bound-to-g-terms]
             [:normalized-terms-not-the-compared-g
              #(r0 % (fn [r] (update-in r [:normalization :terms :risk] + 1.0)))
              :normalization-not-bound-to-g-terms]
             [:recorded-term-not-the-normalized-one
              #(r0 % (fn [r] (update-in r [:g-terms :risk] + 1.0)))
              :normalization-not-bound-to-g-terms]]]
      (testing (name label)
        (let [forged (reseal record f)]
          (if reason
            (is (= [:not-recomputable reason] ((juxt :status :reason) (q4-of forged))))
            (is (= :certified (:status (q4-of forged)))))
          (is (not (true? (holds? forged))) "never a pass"))))
    (testing "a zero term is evaluated, not absent"
      (let [forged (zero-information-gain record)]
        (is (= :certified (:status (q4-of forged))))
        (is (= 0.0 (get-in forged [:decision :selection-certificate :preference-receipts 0
                                   :g-terms :information-gain])))
        (is (true? (holds? forged)))))
    (testing "omitted, extra and foreign policy records"
      (let [c (q4-of record)
            [a b] (:policies c)
            foreign (assoc b :policy-key (assoc (:policy-key b) 2 "p/foreign"))]
        (doseq [[label policies] [[:omitted [a]] [:extra [a b b]] [:foreign [a foreign]]]]
          (testing (name label)
            (let [forged (q4/seal (assoc (dissoc c :sha256) :policies policies))]
              (is (false? (q4/lean-q4 forged)) "Q4 fails on the records")
              (is (= :q4-carrier-not-bound-to-record
                     (:reason (q4/verify-record
                               (assoc-in record [:decision :selection-certificate :q4] forged))))))))))
    (testing "an unbound or edited carrier"
      (is (= :q4-carrier-tampered
             (:reason (q4/verify-record (assoc-in record [:decision :selection-certificate :q4 :policies 0 :horizon] 3)))))
      (is (= :q4-carrier-not-bound-to-record
             (:reason (q4/verify-record
                       (update-in record [:decision :selection-certificate :preference-receipts 0 :g-terms :risk] + 1.0)))))
      (is (= :q4-carrier-absent
             (:reason (q4/verify-record (update-in record [:decision :selection-certificate] dissoc :q4))))))))
