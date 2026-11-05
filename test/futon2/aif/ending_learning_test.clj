(ns futon2.aif.ending-learning-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.run-ending-failure-close-classification-test :as failure-fixture]))

(def makeup
  {:schema :wm/outer-makeup-v1
   :makeup/sha256 (apply str (repeat 64 "b"))
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 1
   :state-kit {:ready {:doable 1 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}
   :eta-c 1 :omega-c 9/10
   :eta-state 1 :omega-state 9/10
   :eta-gain 1 :omega-gain 9/10})

(def run-id "2026-10-29-f74df9cc-c27b-420b-91ec-7a32db708c41")
(def candidate {:target "M-selected" :reach 1/2})
(def selection
  {:makeup/sha256 (:makeup/sha256 makeup)
   :cascade-index 0 :cascade-id :ready :steps [:ready-gate]
   :selection {:selected candidate}
   :focus "WM" :selected-relation :associated})
(def pending {:run-id run-id :giraffe/run-id "old-run" :selection selection})
(def observation
  {:giraffe/run-id "old-run"
   :giraffe/manifest-sha256 (apply str (repeat 64 "c"))
   :focus "WM"
   :relation (fn [_ _] :associated)
   :field [candidate]
   :facts {"M-selected" {:status :ready :unblocks 1 :cone 1}}})

(defn state-with-pending []
  (assoc (outer-state/init-state makeup) :pending pending))

(deftest unclassified-ending-defers-resolution-without-consumption
  (let [ending (failure-fixture/nonterminal-ending)
        before (state-with-pending)
        resolved (with-redefs-fn
                   {#'runtime/pending-run-ending (constantly ending)
                    #'runtime/previous-giraffe-observation
                    (fn [_ _ _] observation)}
                   #(#'runtime/resolve-pending-learning
                     {} makeup before nil observation))]
    (is (= :deferred (:status resolved)))
    (is (= :terminal-ending-class-absent (:reason resolved)))
    (is (= before (:state resolved)))
    (is (nil? (get-in resolved [:state :outer/consumptions run-id])))))

(deftest typed-failure-ending-is-consumed-once
  (let [ending (failure-fixture/typed-failure-ending)
        before (state-with-pending)
        resolved (with-redefs-fn
                   {#'runtime/pending-run-ending (constantly ending)
                    #'runtime/previous-giraffe-observation
                    (fn [_ _ _] observation)}
                   #(#'runtime/resolve-pending-learning
                     {} makeup before nil observation))]
    (is (= :applied (:status resolved)))
    (is (= :stop-the-line (:ending-class resolved)))
    (is (nil? (get-in resolved [:state :pending])))
    (is (= :stop-the-line
           (get-in resolved [:state :outer/consumptions run-id :ending :ending-class])))))

(deftest durable-consumption-requires-a-terminal-ending-class
  (let [before (state-with-pending)
        base-ending {:status :ready :run-id run-id
                     :record-pin {:path "fixture.edn" :sha256 "fixture-pin"}
                     :observed-gain :none}]
    (testing "not-yet-evaluated is not a consumable terminal classification"
      (let [failure (try
                      (outer-state/consume-observation
                       makeup before pending observation
                       (assoc base-ending :ending-class :not-yet-evaluated))
                      nil
                      (catch clojure.lang.ExceptionInfo e e))]
        (is (= :outer-ending-unclassified (:kind (ex-data failure))))))
    (testing "explicit classification absence is independently refused"
      (let [failure (try
                      (outer-state/consume-observation
                       makeup before pending observation
                       (assoc base-ending
                              :ending-class :related
                              :classification-absence
                              {:reason :terminal-ending-class-unavailable}))
                      nil
                      (catch clojure.lang.ExceptionInfo e e))]
        (is (= :outer-ending-unclassified (:kind (ex-data failure))))))
    (testing "an ordinary classified ending is still consumed"
      (let [consumed (outer-state/consume-observation
                      makeup before pending observation
                      (assoc base-ending :ending-class :related))]
        (is (= :applied (:status consumed)))
        (is (= 1 (get-in consumed [:state :click-count])))
        (is (map? (get-in consumed [:state :outer/consumptions run-id])))))))
