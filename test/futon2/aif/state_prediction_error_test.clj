(ns futon2.aif.state-prediction-error-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.state-prediction-error :as spe]))

(defn- close? [x y] (< (Math/abs (- (double x) (double y))) 1.0e-12))

(def example
  {:horizon 2
   :carrier #{:a :b}
   :likelihood {:a 0.8 :b 0.3}
   :transition-in (fn [s x] (get-in {:a {:a 0.9 :b 0.1}
                                      :b {:a 0.2 :b 0.8}} [s x]))
   :transition-out (fn [x s] (get-in {:a {:a 0.6 :b 0.4}
                                       :b {:a 0.3 :b 0.7}} [x s]))
   :s-prev {:a 0.25 :b 0.75}
   :s-current {:a 0.6 :b 0.4}
   :s-next {:a 0.7 :b 0.3}})

(deftest hand-computed-two-state-equation
  ;; a: ln .8 + .25 ln .9 + .75 ln .2 + .7 ln .6 + .3 ln .4 - ln .6
  ;; b: ln .3 + .25 ln .1 + .75 ln .8 + .7 ln .3 + .3 ln .7 - ln .4
  (let [values (:values (spe/state-prediction-error example))]
    (is (close? -1.5782016469866906 (:a values)))
    (is (close? -1.9804694553957245 (:b values)))))

(deftest undefined-log-refuses-the-whole-step
  (let [result (spe/state-prediction-error (assoc example :likelihood {:a 0 :b 0.3}))]
    (is (= {:status :absent :reason :log-undefined
            :where {:term :likelihood :state :a}}
           (select-keys result [:status :reason :where])))
    (is (not-any? #(and (number? %) (not (Double/isFinite (double %))))
                  (tree-seq coll? seq result)))))

(deftest horizon-is-part-of-the-lean-domain
  (is (= :horizon-not-recorded (:reason (spe/state-prediction-error (dissoc example :horizon)))))
  (is (= {:status :absent :reason :out-of-horizon :tau 1 :horizon 1}
         (select-keys (spe/state-prediction-error (assoc example :horizon 1))
                      [:status :reason :tau :horizon]))))

(deftest stationary-mean-field-case-is-log-z
  ;; Point prior and a constant future message make q(x) proportional to
  ;; A(o|x)B(x|s0); then every epsilon(x) is ln Z (registry stationary note).
  (let [a {:a 0.8 :b 0.2}
        bin (fn [_ x] ({:a 0.6 :b 0.4} x))
        bout (fn [_ _] 1)
        z (+ (* 0.8 0.6) (* 0.2 0.4))
        q {:a (/ (* 0.8 0.6) z) :b (/ (* 0.2 0.4) z)}
        result (spe/state-prediction-error
                {:horizon 2 :carrier #{:a :b} :likelihood a
                 :transition-in bin :transition-out bout
                 :s-prev {:s0 1} :s-current q :s-next {:next 1}})]
    (doseq [v (vals (:values result))]
      (is (close? (Math/log z) v)))))
