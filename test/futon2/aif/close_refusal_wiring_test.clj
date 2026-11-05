(ns futon2.aif.close-refusal-wiring-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(defn- drive [attached? await outcome data]
  (let [events (atom [])
        result (runner/validated-close!
                outcome data #(swap! events conj :observe)
                (fn [o d] (swap! events conj [:close o d]) {:outcome o :data d})
                attached? await (constantly nil) (fn [_]))]
    {:events @events :result result}))

(deftest illegal-close-holds-before-persistence
  (let [events (atom [])
        result (runner/validated-close!
                :already-satisfied {:failure-kind :already-satisfied}
                #(swap! events conj :observe)
                (fn [o d] (swap! events conj [:close o d]) d)
                true
                (fn [_]
                  (is (empty? @events))
                  {:action :continue})
                (constantly nil) (fn [_]))]
    (is (= :observe (first @events)))
    (is (= :debugger-error (get-in result [:refusal/disposition :disposition])))))

(deftest work-close-is-unchanged
  (let [{:keys [events result]}
        (drive true (fn [_] (throw (AssertionError. "await called")))
               :grounded-change {:same true})]
    (is (= {:outcome :grounded-change :data {:same true}} result))
    (is (= [:observe [:close :grounded-change {:same true}]] events))))

(deftest detached-refusal-retains-disposition
  (let [{:keys [result]}
        (drive false (fn [_] (throw (AssertionError. "await called")))
               :guardrail-refusal {})]
    (is (= :debugger-error
           (get-in result [:data :refusal/disposition :disposition])))))

(deftest pending-exclusion-does-not-hold
  (let [{:keys [events result]}
        (drive true (fn [_] (throw (AssertionError. "await called")))
               :already-satisfied
               {:failure-kind :already-satisfied
                :mission-observation {:status :admitted}
                :mission-source-sha256 "mission"})]
    (is (= :pending-exclusion
           (get-in result [:data :refusal/disposition :disposition])))
    (is (= :observe (first events)))))

(deftest await-throw-prevents-persistence
  (let [events (atom [])]
    (is (thrown-with-msg?
         Exception #"await failed"
         (runner/validated-close!
          :already-satisfied {} #(swap! events conj :observe)
          (fn [& _] (swap! events conj :close)) true
          (fn [_] (throw (Exception. "await failed")))
          (constantly nil) (fn [_]))))
    (is (empty? @events))))
