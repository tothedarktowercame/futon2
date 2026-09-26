(ns futon2.aif.policy-prefix-f-test
  "F1c-I (PROOF-2a-PLAN <2>2d F1; F1c-D futon3c 8cc2d425 s5 row 4):
  production-ranked supplies each candidate's F from its admitted observed
  prefix. First layer: policy-prefix-evidence/prefix-f and production-ranked.
  Second layer (<2>3b, the first for F): the selection posterior moves by
  exactly exp(-dF), through cascade-selection/selection-posterior and through
  wm/cascade-decision on the tick-1 fixture."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy-prefix-admission :as adm]
            [futon2.aif.policy-prefix-evidence :as ppe]
            [futon2.report.cascade-decision-test :as cdt]
            [futon2.report.war-machine :as wm]))

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
                                  {:c1 (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25)
                                               (step "run-2" {#{:x} 1} {#{:t :u} 1} 0.5))})
        fp (:f-prefix (first pr))]
    (is (= :not-supplied (:status fp)) "never scored")
    (is (= :chain-broken (get-in fp [:refused :reason])))
    (is (= 1 (get-in fp [:refused :index]))))
  (testing "a prefix under another policy key is refused too"
    (let [fp (:f-prefix (first (ppe/production-ranked [entry] nil
                                                     {:c1 (assoc (prefix (step "run-1" {#{} 1} {#{:t} 1} 0.25))
                                                                 :policy-key [:pattern-cascade "M-a" [:p/b] {}])})))]
      (is (= :foreign-prefix (get-in fp [:refused :reason]))))))

(deftest the-posterior-moves-by-exp-minus-delta-f
  (testing "selection-posterior, two candidates identical but for F"
    (let [p (selection/selection-posterior {:beta 1 :candidates [{:id :a :habit 1 :f 0.5 :g 1}
                                                                {:id :b :habit 1 :f 1.5 :g 1}]})]
      (is (< (Math/abs (- (/ (get p :a) (get p :b)) (Math/exp 1.0))) 1e-12)))))

(defn- tick-decision [opts]
  (let [assembled (@#'cdt/assemble* {:targets [cdt/tick-1-target] :sources cdt/tick-1-sources})]
    (:decision (wm/cascade-decision assembled (merge cdt/live-c-opts opts)))))

(defn- steps-for [candidate f]
  {:step {:status :present :policy-key (adm/candidate-key candidate)
          :occurrence {:flight "fl" :click (str "run-" (name (:id candidate)))}
          :s-prev {:value {#{} 1}} :q {#{} 1} :f f}
   :path "p" :sha256 "h"})

(deftest the-tick-selects-with-its-f-term
  (let [base (tick-decision {})
        cands (sort-by :id (keys (get-in base [:selection-law :posterior])))
        [c1 c2 c3] cands
        with (tick-decision {:conditioning-steps {:steps [(steps-for c1 1.0) (steps-for c2 0.5)] :read [] :unread []}})
        pb (get-in base [:selection-law :posterior])
        pw (into {} (map (fn [[c v]] [(:id c) v]) (get-in with [:selection-law :posterior])))
        pb (into {} (map (fn [[c v]] [(:id c) v]) pb))]
    (is (= 3 (count cands)))
    (testing "the posterior moves, in the direction F dictates, by exactly exp(-dF)"
      (is (not= (pr-str pb) (pr-str pw)))
      (is (< (get pw (:id c1)) (get pw (:id c2)) (get pw (:id c3))) "higher F, lower weight; no F (not supplied) is the -F = 0 term")
      (is (< (Math/abs (- (/ (get pw (:id c2)) (get pw (:id c1))) (Math/exp 0.5))) 1e-12))
      (is (< (Math/abs (- (/ (get pw (:id c3)) (get pw (:id c2))) (Math/exp 0.5))) 1e-12)))
    (testing "the certificate says which candidates' F was computed"
      (let [fs (into {} (map (fn [c] [(let [i (:id c)] (if (map? i) (:id i) i)) (:f-status c)]) (get-in with [:selection-certificate :candidates])))]
        (is (= :computed (get fs (:id c1))))
        (is (= :computed (get fs (:id c2))))
        (is (= :not-supplied (get fs (:id c3))))))
    (testing "with no steps the posterior is today's (no admitted prefix anywhere)"
      (let [today (with-redefs [adm/prefixes (constantly nil)] (tick-decision {}))]
        (is (= (pr-str (get-in today [:selection-law :posterior]))
               (pr-str (get-in base [:selection-law :posterior]))))))))
