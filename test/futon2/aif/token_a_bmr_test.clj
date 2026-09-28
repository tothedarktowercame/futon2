(ns futon2.aif.token-a-bmr-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.token-a-bmr :as token-a-bmr]))

(def prior {:alpha 1 :beta 1 :authority "ITEM6C-SCORE-I fixture"})

(defn- cell [k n]
  {:numerator k :denominator n :rate (/ k n)})

(defn- class-rates [fn-k fn-n fp-k fp-n]
  {:false-neg (cell fn-k fn-n) :false-pos (cell fp-k fp-n)})

(defn- log-factorial [n]
  (reduce + 0.0 (map #(Math/log (double %)) (range 1 (inc n)))))

(defn- integer-log-beta
  "Independent closed form: ln B(a,b)=ln((a-1)!)+ln((b-1)!)-ln((a+b-1)!)."
  [a b]
  (- (+ (log-factorial (dec a)) (log-factorial (dec b)))
     (log-factorial (dec (+ a b)))))

(defn- beta11-log-evidence [k n]
  (- (integer-log-beta (inc k) (inc (- n k)))
     (integer-log-beta 1 1)))

(deftest error-free-reduction-uses-the-registry-sign
  (let [result (token-a-bmr/score {:C (class-rates 0 1 0 20)} prior)
        fp (get-in result [:error-free [:C :false-pos]])]
    (is (< (Math/abs (- (:delta-f fp) (- (Math/log 21.0)))) 1e-12))
    (is (<= (:delta-f fp) (:threshold result)))
    (is (= {:numerator 0 :denominator 20} (:counts fp))))
  (let [fp (get-in (token-a-bmr/score {:C (class-rates 0 1 2 20)} prior)
                   [:error-free [:C :false-pos]])]
    (is (true? (:impossible-under-reduced fp)))
    (is (not (contains? fp :delta-f)))))

(deftest pooled-identical-rates-are-favoured
  (let [result (token-a-bmr/score {:A (class-rates 0 1 5 50)
                                   :B (class-rates 0 1 5 50)} prior)
        pooled (get-in result [:pooled :false-pos])
        expected (- (* 2 (beta11-log-evidence 5 50))
                    (beta11-log-evidence 10 100))]
    (is (neg? (:delta-f pooled)))
    (is (< (Math/abs (- (:delta-f pooled) expected)) 1e-12))
    (is (= [:A :B] (:classes pooled)))))

(deftest pooled-incompatible-rates-are-disfavoured
  (let [pooled (get-in (token-a-bmr/score {:A (class-rates 0 1 0 50)
                                           :B (class-rates 0 1 25 50)} prior)
                                     [:pooled :false-pos])]
    (is (pos? (:delta-f pooled)))))

(deftest unobserved-evidence-is-excluded-not-zero-counted
  (let [base (token-a-bmr/score {:A (class-rates 1 10 2 20)} prior)
        with-absent (token-a-bmr/score
                     {:A (class-rates 1 10 2 20)
                      :B {:false-neg {:status :unobserved}
                          :false-pos {:status :unobserved}}
                      :C {:status :unobserved}}
                     prior)]
    (is (= (:pooled base) (:pooled with-absent)))
    (is (= [{:class :B :kind :false-neg :reason :cell-unobserved}
            {:class :B :kind :false-pos :reason :cell-unobserved}
            {:class :C :reason :class-unobserved}]
           (:excluded with-absent)))
    (is (nil? (get-in with-absent [:error-free [:B :false-neg]])))))

(deftest prior-must-be-explicit-and-authorised
  (doseq [bad [{:alpha 1 :beta 1}
               {:alpha 0 :beta 1 :authority "x"}
               {:alpha 1 :beta 1 :authority "  "}]]
    (let [result (token-a-bmr/score {:A (class-rates 0 1 0 1)} bad)]
      (is (= :missing (:status result)))
      (is (= :invalid-prior (:kind result))))))

(deftest receipt-is-labelled-record-only-prototype
  (let [result (token-a-bmr/score {:A (class-rates 0 1 0 1)} prior)]
    (is (= :wm/token-a-bmr-v1 (:schema result)))
    (is (= token-a-bmr/label (:label result)))
    (is (= -3.0 (:threshold result)))
    (is (false? (:applied result)))
    (is (= prior (:prior result)))))

(defn- sourced [rates classes]
  {:status :sourced :source :fixture :rates rates :class-of classes})

(deftest adopt-only-favoured-error-free-cells
  (let [score (token-a-bmr/score {:C (class-rates 0 10 0 20)
                                  :D (class-rates 1 10 1 20)} prior)
        input (sourced {:a {:false-neg 1/11 :false-pos 1/21}
                        :b {:false-neg 2/11 :false-pos 2/21}}
                       {:a :C :b :D})
        result (token-a-bmr/adopt-error-free input score)]
    (is (= 0 (get-in result [:rates :a :false-pos])))
    (is (= 1/11 (get-in result [:rates :a :false-neg])))
    (is (= {:false-neg 2/11 :false-pos 2/21} (get-in result [:rates :b])))
    (is (= {:reduction :error-free
            :cells [[:C :false-pos]]
            :tokens {:a [:false-pos]}}
           (:adoption result)))))

(deftest adoption-obeys-the-minus-three-boundary
  (let [score (token-a-bmr/score {:C (class-rates 0 1 0 19)} prior)
        input (sourced {:a {:false-neg 0 :false-pos 1/20}} {:a :C})
        result (token-a-bmr/adopt-error-free input score)]
    (is (= 1/20 (get-in result [:rates :a :false-pos])))
    (is (= [] (get-in result [:adoption :cells])))))

(deftest absent-score-does-not-change-rates
  (let [input (sourced {:a {:false-neg 1/10 :false-pos 1/20}} {:a :C})
        result (token-a-bmr/adopt-error-free
                input {:status :absent :reason :no-observation-labels})]
    (is (= (:rates input) (:rates result)))
    (is (= {:status :absent :reason :no-observation-labels}
           (:adoption result)))))
