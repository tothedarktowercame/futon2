(ns futon2.aif.policy-prefix-f-test
  "F1c-I (PROOF-2a-PLAN <2>2d F1; F1c-D futon3c 8cc2d425 s5 row 4):
  production-ranked supplies each candidate's F from its admitted observed
  prefix. First layer: policy-prefix-evidence/prefix-f and production-ranked.
  Second layer (<2>3b, the first for F): the selection posterior moves by
  exactly exp(-dF), through cascade-selection/selection-posterior and through
  wm/cascade-decision on the tick-1 fixture."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy-prefix-admission :as adm]
            [futon2.aif.hermetic-repair-fixture]
            [futon2.aif.policy-prefix-evidence :as ppe]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once (fn [run] (futon2.aif.hermetic-repair-fixture/with-hermetic-stores run)))
(use-fixtures :each route/with-empty-learning-ledger)

(def cand {:id :c1 :target "M-a" :precedence [{:id :p/a}]})
(def k (adm/candidate-key cand))
(def entry {:action cand :controller-score 1.0})

(defn- step [click s-prev q f]
  {:status :present :policy-key k :occurrence {:flight "fl" :click click}
   :s-prev {:value s-prev} :q q :f f})

(defn- prefix [& steps] {:policy-key k :conditioning-status :admitted :observation-updates (vec steps)})

(deftest two-admitted-steps-sum-exactly
  (let [fp (ppe/prefix-f entry (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25)
                                        (step "run-2" {#{:t} 1} {#{:t :u} 1} 0.5))
                         nil)]
    (is (= :computed (:status fp)))
    (is (= 0.75 (:f fp)) "f = f1 + f2")
    (is (not= 0.5 (:f fp)) "SPEC-F bad case: not the last summand")
    (is (= 2 (:steps fp)))
    (is (= {#{:t :u} 1} (:posterior fp)) "the last step's q")
    (is (= :prefix-receipt (:presence fp)))))

(deftest no-prefix-is-not-supplied-with-the-admission's-reason
  (is (= :not-supplied (:status (ppe/prefix-f entry {:policy-key k :conditioning-status :no-flight-records
                                                    :observation-updates []} nil))))
  (is (= :no-flight-records (:reason (ppe/prefix-f entry {:policy-key k :conditioning-status :no-flight-records
                                                         :observation-updates []} nil)))))

(deftest a-contradiction-step-is-zero-support
  (let [fp (ppe/prefix-f entry (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25)
                                        (step "run-2" {#{:t} 1} {:status :refused} :contradiction))
                         nil)]
    (is (= :zero-support (:status fp)))
    (is (= {:flight "fl" :click "run-2"} (:at fp)))))

(deftest the-consumer-refuses-a-broken-chain-rather-than-summing-it
  (let [pr (ppe/production-ranked [entry] nil
                                  {k (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25)
                                             (step "run-2" {#{:x} 1} {#{:t :u} 1} 0.5))})
        fp (:f-prefix (first pr))]
    (is (= :not-supplied (:status fp)) "never scored")
    (is (= :chain-broken (get-in fp [:refused :reason])))
    (is (= 1 (get-in fp [:refused :index]))))
  (testing "a prefix under another policy key is refused too"
    (let [fp (:f-prefix (first (ppe/production-ranked [entry] nil
                                                     {k (assoc (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25))
                                                               :policy-key [:pattern-cascade "M-a" [:p/b] {}])})))]
      (is (= :foreign-prefix (get-in fp [:refused :reason]))))))

(deftest production-lookup-does-not-collide-on-local-candidate-label
  (let [a {:id :C1 :target "M-a" :precedence [{:id :p/a}]}
        b {:id :C1 :target "M-b" :precedence [{:id :p/b}]}
        ka (adm/candidate-key a)
        kb (adm/candidate-key b)
        prefix-for (fn [key click f]
                     {:policy-key key :conditioning-status :admitted
                      :observation-updates
                      [{:status :present :policy-key key
                        :occurrence {:flight "fl" :click click}
                        :s-prev {:value {#{} 1}} :q {#{} 1} :f f}]})
        ranked [{:action a :controller-score 1.0}
                {:action b :controller-score 1.0}]
        supplied (ppe/production-ranked ranked nil
                                        {ka (prefix-for ka "a" 0.25)
                                         kb (prefix-for kb "b" 0.75)})]
    (is (= [0.25 0.75] (mapv #(get-in % [:f-prefix :f]) supplied)))
    (is (= [ka kb]
           (mapv #(adm/candidate-key (get-in % [:f-prefix :policy]))
                 supplied)))))

(deftest the-posterior-moves-by-exp-minus-delta-f
  (testing "selection-posterior, two candidates identical but for F"
    (let [p (selection/selection-posterior {:beta 1 :candidates [{:id :a :habit 1 :f 0.5 :g 1}
                                                                {:id :b :habit 1 :f 1.5 :g 1}]})]
      (is (< (Math/abs (- (/ (get p :a) (get p :b)) (Math/exp 1.0))) 1e-12)))))

;; Migrated 2026-10-15 (Q5 route ledger): the tick-level control now runs
;; on the reading-family route. The two reading policies have different G,
;; so F's effect is isolated by comparing the posterior ratio with and
;; without a difference in admitted Prefix-F.

(deftest the-tick-selects-with-its-f-term
  (let [fx (route/fixture)
        keys (vals (route/policy-keys (:decision (route/decide fx))))
        [k1 k2] keys
        unread {:steps [] :read [] :unread [] :dir-status {:absent :no-flights-dir}}
        missing (route/refusal-of #(route/decide fx 1 {:conditioning-steps unread}))
        posterior (fn [f1 f2]
                    (let [d (:decision (route/decide fx 1 {:conditioning-steps
                                                           (route/with-prefixes fx {k1 f1 k2 f2})}))]
                      [d (into {} (map (fn [[c v]] [(adm/candidate-key c) v])
                                       (get-in d [:selection-law :posterior])))]))
        [_ equal] (posterior 0.0 0.0)
        [with p] (posterior 1.0 0.5)]
    (is (= 2 (count keys)))
    (is (= :free-energy-not-supplied (get-in missing [:refusal :kind])))
    (testing "the posterior moves, in the direction F dictates, by exactly exp(-dF)"
      (is (< (Math/abs (- (/ (/ (get p k2) (get p k1)) (/ (get equal k2) (get equal k1)))
                          (Math/exp 0.5)))
             1e-9)
          "k1's F is 0.5 higher: its weight falls by exp(0.5) relative to k2"))
    (testing "the certificate says which candidates' F was computed"
      (is (= #{:computed} (set (map :f-status (get-in with [:selection-certificate :candidates])))))
      (is (= #{1.0 0.5} (set (map :f-consumed (get-in with [:selection-certificate :candidates]))))))))
