(ns futon2.aif.meta-acceptance-review-correction-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-self-heal-production :as p]
            [futon2.aif.meta-self-heal-production-test :as fixtures]
            [futon2.aif.meta-acceptance-compile-correction-test :as compile-fixtures]))

(def fixture "test/fixtures/meta-self-heal/cfbcbad0-review-refusal.edn")
(def live-refusal (:refusal (edn/read-string (slurp fixture))))

(defn exercise [runs verdicts]
  (let [dir (str (java.nio.file.Files/createTempDirectory "m2f4-" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (.mkdirs (io/file dir))
        diagnosis (#'fixtures/produced-diagnosis! dir)
        requests (atom []) reviews (atom []) executed (atom [])
        opts {:meta-acceptance-root (str dir "/acceptances")
              :meta-repository-root (System/getProperty "user.dir")
              :meta-acceptance-fn
              (fn [r] (swap! requests conj r)
                {:status :proposed :acceptance-test
                 (assoc (#'fixtures/acceptance-case (str "review-" (count @requests)) :injury)
                        :expected {:exit 0})})
              :meta-acceptance-run-fn
              (fn [t] (let [n (count @executed)]
                        (swap! executed conj t) (nth runs n)))
              :meta-acceptance-review-fn
              (fn [r] (let [v (nth verdicts (count @reviews))]
                        (swap! reviews conj r)
                        (if (= :accept v) (assoc r :status :accepted)
                            {:status :refused :refusal live-refusal})))}
        result (p/prepare-acceptance opts diagnosis)]
    {:result result :requests @requests :reviews @reviews :executed @executed}))

(def ordinary compile-fixtures/ordinary-run)
(def broken compile-fixtures/compile-run)

(deftest review-refusal-then-accepted-revision
  (let [{:keys [result requests reviews executed]} (exercise [ordinary ordinary] [:refuse :accept])
        c (:review-correction (second requests))]
    (is (= 2 (count requests) (count executed) (count reviews)))
    (is (= :accepted (:status result)))
    (is (= live-refusal (:review-refusal c)))
    (is (= (get-in (first reviews) [:result :run-pin]) (:first-run-pin c)))
    (is (= (:proposal-evidence-pin (first reviews)) (:first-proposal-pin c)))
    (is (string? (:instruction c)))
    (is (not (contains? (first requests) :review-correction)))))

(deftest two-refusals-exhaust-and-retain-the-review-map
  (let [{:keys [result requests reviews]} (exercise [ordinary ordinary] [:refuse :refuse])]
    (is (= 2 (count requests) (count reviews)))
    (is (= :meta-self-heal/acceptance-review-correction-exhausted (:reason result)))
    (is (= live-refusal (get-in result [:first :review-refusal])
           (get-in result [:second :review-refusal])))
    (is (= (:run-pin ordinary) (:first-run-pin result) (:second-run-pin result)))
    (is (false? (:repair-claimed? result)))))

(deftest first-review-accepts-without-correction
  (let [{:keys [result requests reviews]} (exercise [ordinary] [:accept])]
    (is (= :accepted (:status result)))
    (is (= 1 (count requests) (count reviews)))
    (is (not (contains? (first requests) :review-correction)))))

(deftest compile-then-review-correction-has-three-proposals
  (let [{:keys [result requests reviews executed]} (exercise [broken ordinary ordinary] [:refuse :accept])]
    (is (= :accepted (:status result)))
    (is (= 3 (count requests) (count executed)))
    (is (= 2 (count reviews)))
    (is (= [#{} #{:correction} #{:review-correction}]
           (mapv #(set (keys (select-keys % [:correction :review-correction]))) requests)))
    (is (map? (:acceptance-compile-correction result)))))

(deftest review-revision-compile-failure-does-not-reask
  (let [{:keys [result requests reviews]} (exercise [ordinary broken] [:refuse])]
    (is (= 2 (count requests)))
    (is (= 1 (count reviews)))
    (is (= :meta-self-heal/acceptance-compile-correction-exhausted (:reason result)))
    (is (= broken (:second result)))))
