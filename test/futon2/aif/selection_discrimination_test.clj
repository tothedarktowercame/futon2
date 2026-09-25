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

(deftest recorded-runs
  (doseq [id ["1789964661" "1789952479"]]
    (let [f (fixture id) d (replay f) law (:selection-law d)
          p (:policy-comparison law) a (:action-comparison law)]
      (testing id
        (is (= (get-in f [:law :posterior]) (:posterior law)))
        (is (= (get-in f [:law :action-marginal]) (:action-marginal law)))
        (is (= (:chosen-action d) (get-in a [:winner :action])))
        (is (= (if (= id "1789964661") "M-aif-policy-conditioned-eig" "M-expressions-of-interest")
               (get-in d [:action :target])))
        (is (= :acting-policy (:comparison-domain p)))
        (is (= :recorded-run (get-in law [:e-source :source])) "a replay, never a live E")
        (is (every? #(= :recorded-run (get-in % [:habit-provenance :source]))
                    (get-in d [:selection-certificate :candidates])))
        (is (= :action (:comparison-domain a)))
        (if (= id "1789964661")
          (do
            (is (= :G (:decided-by p)))
            (is (= #{:G} (:decided-by a)))
            (is (true? (get-in a [:flips :G :flipped?])))
            (is (= :apparatus/done-is-observed-running
                   (get-in a [:flips :G :winner :action :id])))
            (is (approx (double (/ 443 350694)) (get-in p [:contributions :G])))
            (is (true? (:near-tie? p))))
          (do
            (is (= :habit (:decided-by p)))
            (is (= :robust (:decided-by a)))
            (is (every? false? (map :flipped? (vals (:flips a)))))
            (is (= 0.0 (get-in p [:contributions :G])))
            (is (approx (Math/log 5) (get-in p [:contributions :habit])))
            (is (= :C2 (get-in p [:runner-up :id :id])))))
        (is (and (number? (get-in p [:winner :posterior]))
                 (number? (get-in p [:runner-up :posterior]))
                 (approx (Math/log (/ (get-in p [:winner :posterior])
                                     (get-in p [:runner-up :posterior])))
                         (get-in p [:contributions :total]))))
        (is (= #{:habit :free-energy :G} (set (keys (:flips a)))))
        (is (= :computed-not-attached (get-in p [:winner :f-status])))))))

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
  (let [law (:selection-law (select (simple 0 0) {:beta 1} nil))]
    (is (= :tie-break (get-in law [:policy-comparison :decided-by])))
    (is (= :tie-break (get-in law [:action-comparison :decided-by]))))
  (let [law (:selection-law (select [{:action :a :controller-score 0}] {:beta 1} nil))]
    (is (= :no-competing-policy (get-in law [:policy-comparison :status])))
    (is (= :no-competing-action (get-in law [:action-comparison :runner-up])))))

(deftest free-energy-sign-and-status
  (let [d (select [{:action :a :controller-score 2 :f 0}
                   {:action :b :controller-score 0 :f 4}]
                  {:beta 2} nil)
        p (get-in d [:selection-law :policy-comparison])
        a (get-in d [:selection-law :action-comparison])]
    (is (= {:habit 0.0 :free-energy 4.0 :G -1.0 :total 3.0} (:contributions p)))
    (is (= :free-energy (:decided-by p)))
    (is (= #{:free-energy} (:decided-by a))))
  (let [entry {:action :a :controller-score 0
               :f-prefix {:policy :a :status :not-supplied :f nil}}
        d (select [entry {:action :b :controller-score 2 :f 0}] {:beta 1} nil)
        p (get-in d [:selection-law :policy-comparison])]
    (is (= :not-supplied (get-in p [:winner :f-status])))
    (is (nil? (get-in p [:winner :f])))
    (is (= 0.0 (get-in p [:contributions :free-energy]))))
  (is (= :invalid-free-energy
         (try (select [{:action :a :controller-score 0 :f ##Inf}] {:beta 1} nil)
              (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))))

(deftest exclusions-and-pooled-single-action
  (let [d (select [{:action :b :controller-score 2}
                   {:action :a :controller-score 0
                    :f-prefix {:policy :a :status :zero-support :f nil}}
                   {:action :c :controller-score ##Inf}]
                  {:beta 1} nil)
        a (get-in d [:selection-law :action-comparison])]
    (is (= :no-competing-action (:runner-up a)))
    (is (= :robust (:decided-by a)))
    (is (every? #(= :b (get-in % [:winner :action])) (vals (:flips a)))))
  (let [d (select [{:action {:id :p1 :precedence [:a]} :controller-score 0}
                   {:action {:id :p2 :precedence [:a :b]} :controller-score 1}]
                  {:beta 1} nil)]
    (is (= :compared (get-in d [:selection-law :policy-comparison :status])))
    (is (= :no-competing-action (get-in d [:selection-law :action-comparison :runner-up])))))
