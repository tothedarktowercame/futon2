(ns futon2.aif.machine-predictive-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-predictive :as predictive]
            [futon2.aif.machine-transition-test :as transition-test]
            [futon2.aif.machine-model-test :as model-test]
            [futon2.aif.machine-transition :as transition]))

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
