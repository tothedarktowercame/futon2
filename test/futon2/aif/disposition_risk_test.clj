(ns futon2.aif.disposition-risk-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.disposition-risk :as disposition]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def state
  {:belief {:x 0.5}
   :observation {:mission-health 0.5}})

(defn kernel [observation]
  (let [preferred? (> (double (get observation :mission-health 0.0)) 0.52)]
    (merge (zipmap (:support ruled/seeded-c) (repeat 0.0))
           (if preferred?
             {:grounded-change 1.0}
             {:agent-unavailable 1.0}))))

(def opts
  {:ruled-outcome-c-enabled? true
   :disposition-kernel kernel
   :ambiguity-mode :variance-sum
   :risk-mode :hinge})

(deftest t1-selection-moves-with-disposition-risk
  (let [positive {:type :open-mission :target :m1}
        negative {:type :no-op}
        ranked (efe/rank-actions state [negative positive] opts)]
    (is (= positive (:action (first ranked))))
    (is (< (:G-ruled-outcome-c (first ranked))
           (:G-ruled-outcome-c (second ranked))))))

(deftest t3-support-and-named-zeros-are-enforced
  (testing "zero Q mass over every named zero is a finite KL"
    (is (Double/isFinite
         (disposition/disposition-risk {:mission-health 1.0}
                                       kernel ruled/seeded-c))))
  (testing "removing a named zero from support refuses by name"
    (let [missing (first (sort ruled/named-zero-dispositions))
          planted (update ruled/seeded-c :support disj missing)]
      (try
        (disposition/disposition-risk {:mission-health 1.0} kernel planted)
        (is false "planted missing support was accepted")
        (catch clojure.lang.ExceptionInfo e
          (is (= :outcome-outside-disposition-support
                 (:reason (ex-data e))))
          (is (= [missing] (:outside (ex-data e)))))))))

(deftest t5-fold-layer-is-removable-and-nonzero
  (let [action {:type :no-op}
        with-layer (efe/compute-efe state action opts)
        without-layer (efe/compute-efe state action
                                       (assoc opts :ruled-outcome-c-enabled? false))]
    (is (pos? (:G-ruled-outcome-c with-layer)))
    (is (zero? (:G-ruled-outcome-c without-layer)))
    (is (< (Math/abs
            (- (:G-ruled-outcome-c with-layer)
               (- (:controller-score with-layer)
                  (:controller-score without-layer))))
           1.0e-12))))
