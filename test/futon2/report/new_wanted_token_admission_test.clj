(ns futon2.report.new-wanted-token-admission-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.report.war-machine :as wm]))

(defn fixture []
  (edn/read-string (slurp (io/resource "fixtures/new-wanted-token/1789964661.edn"))))

(defn admit [p] (#'wm/admit-cascade-problem p))

(defn candidate-decline [r]
  (first (filter #(= :candidate-admission (:stage %)) (:declines r))))

(deftest recorded-no-op-is-refused
  (let [{:keys [problems recorded-D]} (fixture)
        results (mapv admit problems)
        f2 (last results)
        drop (candidate-decline f2)]
    (is (= recorded-D (:value (receipts/initial-belief problems))))
    (is (= ["M-aif-policy-conditioned-eig" "M-f11-find-production-successor"]
           (mapv #(get-in % [:problem :target]) (take 2 results))))
    (is (nil? (:problem f2)))
    (is (= :no-new-wanted-token (:reason drop)))
    (is (= #{:route-a-rehearsal-reported} (get-in drop [:evidence :initial-wanted-tokens])))
    (is (= {#{:route-a-rehearsal-reported} 1} (get-in drop [:evidence :terminal-wanted-belief])))
    (is (= #{} (get-in drop [:evidence :new-wanted-tokens])))
    (is (= 2 (get-in drop [:evidence :horizon])))))

(defn problem [horizon]
  {:target "M-test"
   :constructed-candidates [{:candidate-id :C1 :precedence [:finish :prepare]
                             :construction-receipt {:source :test}}]
   :interpretation-receipts {:finish {:source :test} :prepare {:source :test}}
   :cascade-problem
   {:facts {:ready true :bridge false :done false} :want [:done] :beta 1
    :horizon-steps horizon
    :interpretations
    {:prepare {:guard {:needs #{:ready} :forbids #{:bridge}} :produces #{:bridge}}
     :finish {:guard {:needs #{:bridge} :forbids #{:done}} :produces #{:done}}}}})

(deftest horizon-is-a-real-boundary
  (let [short (admit (problem 1)) long (admit (problem 2))]
    (is (= :no-new-wanted-token (:reason (candidate-decline short))))
    (is (= {#{} 1} (get-in (candidate-decline short) [:evidence :terminal-wanted-belief])))
    (is (some? (:problem long)))
    (is (empty? (:declines long)))))

(deftest adding-one-wanted-token-is-enough
  (let [p (-> (problem 1)
              (assoc-in [:cascade-problem :facts :bridge] true)
              (assoc-in [:cascade-problem :want] [:done :other]))]
    (is (some? (:problem (admit p))))))

(deftest missing-horizon-is-not-a-no-progress-finding
  (let [drop (candidate-decline (admit (problem nil)))]
    (is (= :candidate-prediction-refused (:reason drop)))
    (is (= :missing-common-horizon (get-in drop [:evidence :refusal :kind])))))

(deftest all-refused-uses-existing-abstention
  (let [f2 (last (:problems (fixture)))
        r (wm/cascade-decision {:problems [f2] :refusals []} {})]
    (is (= :abstained (get-in r [:decision :status])))
    (is (= :no-acting-cascade-candidate (get-in r [:decision :reason])))
    (is (= :no-constructed-candidate (get-in r [:decision :refusals 0 :kind])))
    (is (empty? (:lanes r)))
    (is (= :no-new-wanted-token (:reason (first (:dropped-candidates r)))))
    (is (= (:dropped-candidates r) (get-in r [:cascade-problems :dropped-candidates])))))

(deftest rejection-cannot-hide-an-incommensurable-family
  (let [ps (:problems (fixture))
        mixed (assoc-in ps [2 :cascade-problem :horizon-steps] 3)]
    (is (= :incommensurable-family
           (try (wm/cascade-decision {:problems mixed :refusals []} {})
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
