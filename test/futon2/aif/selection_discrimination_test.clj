(ns futon2.aif.selection-discrimination-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy :as policy]))

(defn fixture [id]
  (edn/read-string (slurp (io/resource (str "fixtures/narrative-discrimination/" id ".edn")))))

(defn select
  "Replay a recorded run against the habit state it recorded (M-wm-wiring
  step 8: selection no longer reads the store; the replay-only :habit-state
  names the state and says :recorded-run on the record)."
  [ranked opts snapshot]
  (policy/select-action-cascades
   ranked (assoc opts :habit-state {:state (edn/read-string (or snapshot "nil"))
                                    :source :recorded-run})))

(defn replay [f]
  (select (mapv (fn [c]
                  (cond-> {:action (:id c) :controller-score (:g c)
                           :certificate {:f (:computed-f c)}}
                    (= :attached (:f-status c)) (assoc :f (:f c)))) (:candidates f))
          {:beta (get-in f [:law :beta])
           :near-tie-threshold {:value 0.01 :status :declared}}
          (:habit-snapshot f)))

(defn approx [expected actual]
  (and (number? actual) (< (abs (- expected actual)) 1.0e-12)))

(deftest recorded-runs-under-the-strict-minimum
  (let [f (fixture "1789964661") d (replay f) law (:selection-law d)
        p (:policy-comparison law)
        smallest (apply min (map :g (:candidates f)))]
    (is (= (get-in f [:law :posterior]) (:posterior law)) "the posterior is still the record")
    (is (not (contains? law :action-comparison)))
    (is (= smallest (:controller-score d)) "the smallest G is enacted")
    (is (= :prediction-class (:comparison-domain p)))
    (is (= :G (:decided-by p)))
    (is (= :recorded-run (get-in law [:e-source :source])) "a replay, never a live E")
    (is (approx (Math/log (/ (get-in p [:winner :posterior]) (get-in p [:runner-up :posterior])))
                (get-in p [:contributions :total]))))
  (testing "this run's two lowest G are exactly equal and nothing records that they
            predict alike, so the habit no longer decides it: the law refuses"
    (is (= :tie-at-minimum
           (try (replay (fixture "1789952479"))
                nil
                (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))))

(defn simple [g1 g2]
  [{:action :a :controller-score g1} {:action :b :controller-score g2}])

(deftest declared-and-undeclared-thresholds
  (let [d (select (simple 0 2) {:beta 1 :near-tie-threshold {:value 0.01 :status :declared}} nil)
        p (get-in d [:selection-law :policy-comparison])]
    (is (= :G (:decided-by p)))
    (is (= 2.0 (get-in p [:contributions :G])))
    (is (false? (:near-tie? p))))
  (is (= :threshold-undeclared
         (get-in (select (simple 0 2) {:beta 1} nil)
                 [:selection-law :policy-comparison :near-tie?])))
  (is (= :threshold-invalid
         (get-in (select (simple 0 2) {:beta 1 :near-tie-threshold {:value -1 :status :declared}} nil)
                 [:selection-law :policy-comparison :near-tie?]))))

(deftest exact-ties-and-single-action
  (is (= :tie-at-minimum
         (try (select (simple 0 0) {:beta 1} nil)
              (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))
  (let [law (:selection-law (select [{:action :a :controller-score 0}] {:beta 1} nil))]
    (is (= :no-competing-policy (get-in law [:policy-comparison :status])))
    (is (= :no-competing-policy (get-in law [:policy-comparison :decided-by])))))

(deftest free-energy-sign-and-status
  (let [d (select [{:action :a :controller-score 2 :f 0}
                   {:action :b :controller-score 0 :f 4}]
                  {:beta 2} nil)
        p (get-in d [:selection-law :policy-comparison])]
    (is (= :b (:action d)) "G decides; the posterior would have preferred :a")
    (is (= {:habit 0.0 :free-energy -4.0 :G 1.0 :total -3.0} (:contributions p)))
    (is (= :G (:decided-by p)))
    (is (= :G (:favoured-by p))))
  (let [entry {:action :a :controller-score 0
               :f-prefix {:policy :a :status :not-supplied :f nil}}
        refusal (try
                  (select [entry {:action :b :controller-score 2 :f 0}] {:beta 1} nil)
                  nil
                  (catch clojure.lang.ExceptionInfo e
                    (get-in (ex-data e) [:refusal :kind])))]
    (is (= :free-energy-not-supplied refusal)
        "an admitted policy population cannot silently replace missing F with zero"))
  (is (= :invalid-free-energy
         (try (select [{:action :a :controller-score 0 :f ##Inf}] {:beta 1} nil)
              (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))))

(deftest exclusions-and-shared-first-action
  (let [d (select [{:action :b :controller-score 2}
                   {:action :a :controller-score 0
                    :f-prefix {:policy :a :status :zero-support :f nil}}
                   {:action :c :controller-score ##Inf}]
                  {:beta 1} nil)]
    (is (= :b (:action d)) "no support and no finite G are not candidates")
    (is (= :no-competing-policy (get-in d [:selection-law :policy-comparison :status]))))
  (let [d (select [{:action {:id :p1 :precedence [:a]} :controller-score 0}
                   {:action {:id :p2 :precedence [:a :b]} :controller-score 1}]
                  {:beta 1} nil)]
    (is (= :compared (get-in d [:selection-law :policy-comparison :status])))))
