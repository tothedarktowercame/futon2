(ns futon2.aif.wm.q4-preference-route-test
  "Requirements Q4 on the production reading-family route: the decision's
  Q4 carrier, built from the certified preference receipts of the policies
  the one G selection compared (its posterior, after any Q6 exclusion), is
  persisted with the decision and accepted by the exporter only when the
  persisted decision recomputes it."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
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
  "RECORD with receipt I rewritten by F and the Q4 carrier recomputed, as a
  consistent forger would."
  [record f]
  (let [r (update-in record [:decision :selection-certificate :preference-receipts] (comp vec f))
        d (update (:decision r) :selection-certificate dissoc :q4)]
    (assoc-in r [:decision :selection-certificate :q4] (q4/carrier d))))

(deftest compared-policies-carry-every-term-at-every-step
  (let [{:keys [record calls]} (click! (route/fixture) (route/temp-dir "q4-"))
        c (q4-of record)]
    (is (= 1 calls))
    (is (= :certified (:status c)))
    (is (= {:status :verified :carrier c} (q4/verify-record record)) "bound to the record")
    (testing "compared = the posterior, by structural key"
      (is (= 2 (count (:compared c))))
      (is (= (set (:compared c))
             (set (map #(get-in % [:id :policy-key])
                       (get-in record [:decision :selection-certificate :candidates]))))))
    (testing "horizon, steps and grading from each policy's certified C"
      (is (= 2 (:horizon c)))
      (is (= [0 1] (:preference-steps c) (:graded-steps c)))
      (is (= :progressive (:semantics c)))
      (is (every? #(= [1 2] (:steps %) (:graded-steps %)) (:policies c))))
    (testing "every term evaluated for every compared policy"
      (is (= {:risk true :ambiguity true :information-gain true} (:g-terms c)))
      (is (= {:risk 2 :ambiguity 2 :information-gain 2} (:counts c))))
    (is (true? (holds? record)))
    (testing "the exporter and report card read the carrier"
      (let [export (facts/facts-for-record record "r" {})
            f (:facts export)]
        (is (= 2 (f "horizonLength") (count (f "comparedPolicies"))))
        (is (= "progressive" (f "preferenceSemantics")))
        (is (= [0 1] (f "gradedPreferenceSteps")))
        (is (= :pass (:status (first (filter #(= :Q4 (:requirement %))
                                             (card/verdict-rows export))))))))))

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
    (is (= 1 (count (:compared c))) "only the post-Q6 menu was compared")
    (is (not-any? (set excluded) (:compared c)))
    (is (= {:risk 1 :ambiguity 1 :information-gain 1} (:counts c)))
    (is (true? (holds? record)))))

(deftest mixed-horizons-are-not-recomputable
  ;; the p/a + p/c reading compares a horizon-2 and a horizon-3 policy in
  ;; one G selection; Lean's Q4 states one horizon per run
  (let [{:keys [record calls]} (click! (route/fixture ["p/a" "p/c"]) (route/temp-dir "q4-"))]
    (is (= 1 calls))
    (is (= [:not-recomputable :mixed-horizons [2 3]]
           ((juxt :status :reason :horizons) (q4-of record))))
    (is (= :nr (holds? record)))
    (is (contains? ((:facts (facts/facts-for-record record "r" {})) "horizonLength")
                   "not-recomputable"))))

(deftest forged-and-lost-evidence-is-refused
  (let [{:keys [record]} (click! (route/fixture) (route/temp-dir "q4-"))
        r0 #(assoc-in (vec %1) [0] (%2 (first %1)))]
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
             [:two-receipts-one-policy #(conj (vec %) (first %)) :competing-receipts]]]
      (testing (name label)
        (let [forged (reseal record f)]
          (if reason
            (is (= [:not-recomputable reason] ((juxt :status :reason) (q4-of forged))))
            (is (= :certified (:status (q4-of forged)))))
          (is (not (true? (holds? forged))) "never a pass"))))
    (testing "a zero term is evaluated, not absent"
      (let [forged (reseal record #(r0 % (fn [r] (assoc-in r [:g-terms :information-gain] 0.0))))]
        (is (= :certified (:status (q4-of forged))))
        (is (true? (holds? forged)))))
    (testing "an unbound or edited carrier"
      (is (= :q4-carrier-tampered
             (:reason (q4/verify-record (assoc-in record [:decision :selection-certificate :q4 :horizon] 3)))))
      (is (= :q4-carrier-not-bound-to-record
             (:reason (q4/verify-record
                       (update-in record [:decision :selection-certificate :preference-receipts 0 :g-terms :risk] + 1.0)))))
      (is (= :q4-carrier-absent
             (:reason (q4/verify-record (update-in record [:decision :selection-certificate] dissoc :q4))))))))
