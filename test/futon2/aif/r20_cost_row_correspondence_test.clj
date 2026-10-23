(ns futon2.aif.r20-cost-row-correspondence-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.apparatus-certificates :as apparatus]))

(def fixture-path "test/resources/wm/r20-cost-row/run90ca-cost.edn")
(defn fixture [] (edn/read-string (slurp fixture-path)))

(defn cost-row
  [{:keys [model-usage timing]}]
  (->> (:rows (apparatus/receipt {:decision {} :participants {}
                                  :model-usage model-usage :timing timing}))
       (filter #(= :cost (:node %))) first))

(deftest retained-run90ca-matches-the-formal-instance
  (let [row (cost-row (fixture))]
    (is (= [:agrees :mostly-acting] ((juxt :verdict :case) row)))
    (is (= {:through-selection 0 :after-selection 2145482
            :total 2145482 :input 2124394 :output 21088
            :cached-input 2035904 :uncached-input 88490}
           (get-in row [:checked :tokens])))
    (is (= {:through-selection 247928 :after-selection 700366}
           (get-in row [:checked :active-ms])))
    (is (= {:through-selection 0 :after-selection 4}
           (get-in row [:checked :jobs])))))

(deftest aggregate-input-is-an-extra-runtime-check
  (let [changed (update-in (fixture) [:model-usage :input-tokens] inc)
        row (cost-row changed)]
    (is (= :disagrees (:verdict row)))
    (is (= {:recorded 2124395 :sum-over-jobs 2124394}
           (get-in row [:checked :total-mismatches :input-tokens])))
    (is (= 2145482 (get-in row [:checked :tokens :total]))
        "the existing Lean projection fields are unchanged")))

(deftest malformed-or-inconsistent-inputs-stay-distinct
  (testing "missing usage is absent"
    (is (= :not-recorded (:verdict (cost-row (dissoc (fixture) :model-usage))))))
  (testing "wrong job total is present and disagrees"
    (let [row (cost-row (update-in (fixture)
                                   [:model-usage :jobs 0 :total-tokens] inc))]
      (is (= :disagrees (:verdict row)))
      (is (= 256325 (get-in row [:checked :job-total-mismatches 0 :total-tokens]))))))

(deftest existing-phase-boundary-drives-the-split
  (let [moved (assoc-in (fixture) [:model-usage :jobs 0 :phase] :selection)
        row (cost-row moved)]
    (is (= :agrees (:verdict row)))
    (is (= {:through-selection 256324 :after-selection 1889158
            :total 2145482 :input 2124394 :output 21088
            :cached-input 2035904 :uncached-input 88490}
           (get-in row [:checked :tokens])))
    (is (= {:through-selection 1 :after-selection 3}
           (get-in row [:checked :jobs])))))

(deftest negative-integers-are-outside-this-correspondence-domain
  ;; Pin the boundary honestly: cost-row checks integer?, not nonnegative?. The
  ;; producer rejects negative usage; this test does not claim cost-row does.
  (let [changed (-> (fixture)
                    (assoc-in [:model-usage :jobs 0 :input-tokens] -1)
                    (assoc-in [:model-usage :jobs 0 :total-tokens] 5726)
                    (update-in [:model-usage :input-tokens] - 250598)
                    (update-in [:model-usage :total-tokens] - 250598))]
    (is (= :agrees (:verdict (cost-row changed)))
        "arbitrary negative integers must not be silently mapped to Lean Nat")))
