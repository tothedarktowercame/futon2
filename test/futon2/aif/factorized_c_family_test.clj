(ns futon2.aif.factorized-c-family-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as m]))

(def universe #{:a :b})
(def rates {:a {:false-neg 1/4 :false-pos 1/8}
            :b {:false-neg 1/3 :false-pos 1/5}})
(def spec {:want #{:a} :evidence #{:b} :lam 2 :mu 1 :zeroed #{}})
(def family (assoc spec :c-schedule
                   {:placement {:value :terminal :status :declared}
                    :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}}))
(def outcomes [#{} #{:a} #{:b} #{:a :b}])
(defn close? [a b] (< (Math/abs (- (double a) (double b))) 1e-12))
(defn model [q s rs]
  {:rates rs :q0 q :precedence-fn (constantly []) :horizon 2
   :spec s :universe universe})

(deftest family-enumeration-and-constant-controls
  (doseq [[label q] [[:point {#{:a} 1}]
                    [:product (m/independent-belief {:a 1/3 :b 2/5} universe)]]
          [shape s] [[:terminal family] [:constant spec]]
          [rate-kind rs] [[:rated rates] [:zero (zipmap universe (repeat {:false-neg 0 :false-pos 0}))]]]
    (testing (str label " " shape " " rate-kind)
      (let [result (m/horizon-g-sparse-cert (model q s rs))
            expected (mapv
                      (fn [tau]
                        ;; Independent finite enumeration: uniform at the
                        ;; nonterminal step, ordinary declared spec otherwise.
                        (let [c (if (and (= shape :terminal) (= tau 1))
                                  (zipmap outcomes (repeat 0.25))
                                  (m/preference-distribution spec universe))]
                          {:tau tau :c c
                           :risk (m/outcome-risk (m/predict-observations rs q) c)
                           :ambiguity (m/step-ambiguity rs q)})) [1 2])
            steps (get-in result [:certificate :steps])
            members (get-in result [:certificate :consumed-g :C :steps])]
        (is (= 2 (count steps) (count members)))
        (doseq [[actual e member] (map vector steps expected members)]
          (is (close? (:risk actual) (:risk e)))
          (is (close? (:ambiguity actual) (:ambiguity e)))
          (is (= (:tau actual) (:tau member)))
          (let [log-p (m/member-log-probability (:distribution member))]
            (doseq [o outcomes]
              (is (close? (Math/exp (log-p o)) (get (:c e) o)))))
        (is (close? (:g result) (reduce + (map #(+ (:risk %) (:ambiguity %)) expected))))
        (println "C-FAMILY-COMPARISON" (pr-str {:belief label :shape shape :rates rate-kind
                                               :expected expected :actual steps :total (:g result)})))))))

(deftest representation-boundaries-refuse
  (doseq [[label change expected]
          [[:pointwise {:c-fn-pointwise (fn [_] (constantly 1/4))} :c-form-unsupported-with-rates]
           [:excluded {:spec (assoc family :zeroed #{#{}})} :zeroed-unsupported-with-rates]
           [:schedule {:spec (assoc-in family [:c-schedule :placement :value] :invalid)} :invalid-preference-schedule]
           [:correlated {:q0 {#{} 1/2 universe 1/2}} :non-factorizable-belief]]]
    (let [r (m/horizon-g-sparse-cert (merge (model {#{} 1} family rates) change))]
      (is (= expected (get-in r [:g :kind])))
      (is (= :missing (get-in r [:g :status])))
      (is (nil? (:certificate r)))
      (println "C-FAMILY-NEGATIVE" (pr-str {:case label :result r})))))
