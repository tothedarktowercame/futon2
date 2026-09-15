(ns futon2.aif.machine-predictive-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-predictive :as predictive]
            [futon2.aif.machine-transition-test :as transition-test]
            [futon2.aif.machine-model-test :as model-test]
            [futon2.aif.machine-transition :as transition]
            [futon2.aif.machine-model :as model]
            [futon2.aif.machine-belief :as reader]))

(def entity "mission/example")
(def belief-input {:mode :single-entity :posteriors {entity (transition-test/point :spawned)}})
(defn policy [id actions]
  {:id id :revision "v1" :entity/id entity :model/revision "v1" :actions actions})

(deftest full-policy-prediction-and-horizon-control
  (model-test/with-example
    (fn [example]
      (let [kernel (transition/controlled-transition-kernel
                    (transition-test/model example) transition-test/actions transition-test/params)
            p (predictive/predicted-state-plan belief-input kernel
                                                (policy "twice" [:advance-mission :advance-mission]))
            q (predictive/predicted-state-plan belief-input kernel
                                                (policy "branch" [:advance-mission :apply-cascade]))]
        (is (:ok p))
        (is (= 3 (count (:steps p))))
        (is (= (transition-test/point :spawned) (get-in p [:steps 0 :distribution])))
        (is (= (transition-test/point :refined) (get-in p [:steps 1 :distribution])))
        (is (= (transition-test/point :strengthened) (:terminal p)))
        (is (= (transition-test/point :refined) (:terminal q)))
        (is (not= (:terminal p) (:terminal q)))))))

(deftest policy-refusals
  (model-test/with-example
    (fn [example]
      (let [kernel (transition/controlled-transition-kernel
                    (transition-test/model example) transition-test/actions transition-test/params)
            kind #(get-in (predictive/predicted-state-plan belief-input kernel %) [:refusal :kind])]
        (doseq [[label expected p]
                [[:empty :empty-policy (policy "empty" [])]
                 [:undeclared :undeclared-action (policy "bad" [:advance-mission :foreign])]
                 [:revision :model-revision-mismatch
                  (assoc (policy "foreign" [:advance-mission]) :model/revision "v2")]]]
          (testing (name label) (is (= expected (kind p)))))))))

(deftest predictive-outcome-composition-and-refusals
  (model-test/with-example
    (fn [example]
      (let [base (transition-test/model example)
            a (predictive/declared-outcome-a base)
            model (assoc base :A (dissoc a :ok))
            kernel (transition/controlled-transition-kernel
                    model transition-test/actions transition-test/params)
            policies [(policy "p" [:advance-mission :advance-mission])]
            result (predictive/predictive-outcome-kernel model belief-input kernel policies)]
        (is (:ok result))
        (is (= 1 (reduce + (vals (get-in result [:rows "p"])))))
        (is (= :missing-a-support
               (get-in (predictive/predictive-outcome-kernel
                        (update-in model [:A :rows] dissoc :spawned)
                        belief-input kernel policies) [:refusal :kind])))
        (is (= :model-revision-mismatch
               (get-in (predictive/predictive-outcome-kernel
                        (assoc-in model [:model :revision] "foreign")
                        belief-input kernel policies) [:refusal :kind])))
        (is (= :evidence-vocabulary-owed
               (get-in (predictive/predictive-outcome-kernel
                        (assoc model :outcome-vertex :evidence)
                        belief-input kernel policies) [:refusal :kind])))))))

(deftest reader-to-predictor-consumes-and-retains-every-numeric-row
  (model-test/with-example
    (fn [example]
      (let [kernel (transition/controlled-transition-kernel
                    (transition-test/model example) transition-test/actions transition-test/params)
            row (assoc (zipmap reader/state-support (repeat 0.0)) :spawned 0.25 :refined 0.75)
            context {:entity/id entity :model (:model kernel) :state-support reader/state-support
                     :mode :single-entity :policy-entities [entity]}
            read-result (reader/belief-state-distribution context {entity row})
            plan (predictive/predicted-state-plan (:belief-input read-result) kernel
                                                 (policy "numeric" [:advance-mission :advance-mission]))]
        (is (:ok read-result))
        (is (:ok plan))
        (is (identical? row (get-in plan [:steps 0 :distribution])))
        ;; Reader and kernel declare different orders for the same named
        ;; support. Preserve those orders; the represented numeric row agrees.
        (is (= (dissoc (:numeric-admission read-result) :support)
               (dissoc (get-in plan [:steps 0 :numeric-admission]) :support)))
        (is (= reader/state-support (get-in read-result [:numeric-admission :support])))
        (is (= (:state-support kernel) (get-in plan [:steps 0 :numeric-admission :support])))
        (is (= 3 (count (:steps plan))))
        (doseq [step (:steps plan)]
          (let [actual (:distribution step) admission (:numeric-admission step)]
            (is (= (:model kernel) (:model step)))
            (is (= (:state-support kernel) (:support admission)))
            (is (= actual (:values admission)))
            (is (= (model/distribution-admission actual (:state-support kernel)) admission))
            (is (= :float-carried (:admission step)))
            (is (:exactly-normalized? admission))))
        (is (not= (get-in plan [:steps 0 :numeric-admission :values])
                  (get-in plan [:steps 1 :numeric-admission :values]))
            "a reused initial admission cannot stand in for later rows")
        (is (= (:terminal plan) (get-in plan [:steps 2 :numeric-admission :values])))))))

(deftest predictor-shared-boundary-rejects-initial-and-produced-bad-rows
  (model-test/with-example
    (fn [example]
      (let [kernel (transition/controlled-transition-kernel
                    (transition-test/model example) transition-test/actions transition-test/params)
            p (policy "guard" [:advance-mission])]
        (doseq [row [(assoc (transition-test/point :spawned) :extra 0)
                     (assoc (transition-test/point :spawned) :spawned -1 :refined 2)
                     (assoc (transition-test/point :spawned) :spawned Double/NaN)
                     (assoc (transition-test/point :spawned) :spawned Double/POSITIVE_INFINITY)]]
          (is (= :invalid-mass
                 (get-in (predictive/predicted-state-plan
                          {:mode :single-entity :posteriors {entity row}} kernel p) [:refusal :kind]))))
        (with-redefs [transition/apply-belief (fn [_ _ _]
                                               {:ok true :distribution
                                                (assoc (transition-test/point :spawned) :spawned -1 :refined 2)})]
          (is (= {:ok false :refusal {:kind :invalid-mass :path [:steps 1]}}
                 (predictive/predicted-state-plan belief-input kernel p))))))))
