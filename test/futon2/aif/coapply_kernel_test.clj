(ns futon2.aif.coapply-kernel-test
  "M-wm-wiring step 14 (WM-COAPPLY-I): the co-application kernel of
  mathlib4 DarkTower/WarMachine/Proof2/CoApplicationKernel.lean (69c2432f2b)
  in the rollout, cascade-model-manifest/co-apply-kernel.

  FIXTURE: no real receipt with a non-chain order exists: 0 of the 49 run
  records under futon2 data/wm-runs carry :descent (checked 2026-09-26; the
  :order is attached only to constructed candidates, since 7a5f6c0b, and no
  tick has constructed one since). The orders here are computed by
  construction/containment-order on order-kernel-test's own candidates, plus
  the Lean ConflictFixture's two patterns."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.construction :as construction]
            [futon2.aif.efe :as efe]
            [futon2.aif.order-kernel-test :as ok]))

;; (a) the bad case first: the Lean ConflictFixture as data. p produces a
;; and forbids b; q produces b and forbids a; the order is empty; theta 4/5.
(def P {:id :p :theta 4/5 :produces #{:a}
        :guard {:status :interpreted :clauses [{:status :interpreted :present #{} :absent #{:b}}]}})
(def Q {:id :q :theta 4/5 :produces #{:b}
        :guard {:status :interpreted :clauses [{:status :interpreted :present #{} :absent #{:a}}]}})

(deftest co-application-and-the-list-differ-on-the-conflict-fixture
  (let [co (m/co-apply-kernel [:p :q] [] {:p P :q Q} #{})
        lst (m/cascade-kernel [P Q] #{})]
    (is (= {#{:a :b} 16/25 #{:a} 4/25 #{:b} 4/25 #{} 1/25} co)
        "coApply_ne_zero: {a b} in one step with probability theta_p * theta_q")
    (is (= {#{:a} 4/5 #{} 1/5} lst) "list_pq_zero: the list never reaches {a b} in one step")
    (is (not= co lst))
    (let [{:keys [evaluation]} (#'m/push-forward {:co-apply {:units [:p :q] :descent [] :patterns {:p P :q Q}}} {#{} 1} true)]
      (is (true? (:frontier-conflict (first (:states evaluation)))) "frontierConflict: the certificate carries the flag")
      (is (= :co-application-frontier-theta-v1 (get-in evaluation [:model :semantics]))))))

;; (b) the theorem as data: on the chain candidate the two kernels agree at
;; every state, and G is unchanged to the bit
(def chain-order (construction/containment-order {:patterns (mapv ok/interp [:A :B]) :precedence [:A :B]}))
(def chain-step {:co-apply {:units (mapv :unit (:units chain-order)) :descent (:descent chain-order)
                            :patterns (into {} (for [{:keys [unit pattern]} (:units chain-order)] [unit (ok/pat pattern)]))}})

(deftest on-a-chain-the-kernels-coincide
  (is (seq (:descent chain-order)) "the fixture is a chain with a descent edge")
  (doseq [s [#{} #{:x} #{:w} #{:x :w} #{:y} #{:x :y} #{:x :w :y}]]
    (is (= (m/cascade-kernel (mapv ok/pat [:A :B]) s)
           (:kernel (#'m/evaluate-state chain-step s false)))
        (str "state " s)))
  (is (= (m/rollout (constantly (mapv ok/pat [:A :B])) {#{} 1} 3)
         (m/rollout (constantly chain-step) {#{} 1} 3))
      "three rollout steps, exact rationals")
  (let [g-list (:G-efe (first (filter #(= :chain (:cascade-id %)) (#'ok/rank))))
        g-co (with-redefs [efe/order-use (fn [a] (if (= :chain (:id a))
                                                  {:precedence (:precedence a) :kernel-step chain-step :meta {:order :forced}}
                                                  {:precedence (:precedence a) :meta {}}))]
               (:G-efe (first (filter #(= :chain (:cascade-id %)) (#'ok/rank)))))]
    (is (= g-list g-co) "the chain's G through the co-application kernel, to the bit")))

;; (c) through rank-cascade-actions
(deftest the-ranking-scores-a-non-chain-by-co-application
  (let [r (into {} (map (juxt :cascade-id identity)) (#'ok/rank))
        list-g-independent 4.119711924440846] ; order-kernel fixture, f8e766a5
    (is (= {:order :co-application} (get-in r [:independent :order-use])))
    (is (not= list-g-independent (get-in r [:independent :G-efe])) "the non-chain's G moves")
    (is (= {:order :chain} (get-in r [:chain :order-use])))
    (is (= 3.4797119244408456 (get-in r [:chain :G-efe])) "the chain's G, to the bit")
    (is (re-find #"co-application-frontier-theta-v1" (pr-str (get-in r [:independent :certificate])))
        "the certificate names the co-application semantics")
    (is (not (re-find #"co-application-frontier-theta-v1" (pr-str (get-in r [:chain :certificate])))))))

(deftest a-theta-outside-the-unit-interval-refuses-as-pattern-kernel-does
  (is (= :invalid-pattern-interpretation
         (:kind (m/co-apply-kernel [:p] [] {:p (assoc P :theta 3/2)} #{})))))

(deftest an-empty-frontier-is-the-identity
  (is (= {#{:a :b} 1} (m/co-apply-kernel [:p :q] [] {:p P :q Q} #{:a :b}))))
