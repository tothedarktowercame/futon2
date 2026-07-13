(ns futon2.aif.fold-semilattice-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold :as fold]
            [futon2.aif.fold-semilattice :as fs]))

(def ^:private cascade
  ["agent/descent-root"
   "agent/descent-child"
   "agent/co-app-peer"])

(def ^:private semilattice
  {:descent [["agent/descent-root" "agent/descent-child"]]
   :co-app [["agent/descent-child" "agent/co-app-peer" 2]]})

(deftest semilattice-fold-satisfies-contract
  (let [out (fs/semilattice-fold cascade {:semilattice semilattice})]
    (is (fold/valid-fold-output? out))
    (is (number? (:coverage-score-delta out)))
    (is (neg? (:coverage-score-delta out)))))

(deftest semilattice-fold-builds-nondegenerate-wiring
  (let [out (fs/semilattice-fold cascade {:semilattice semilattice})
        wiring (:wiring out)]
    (is (= (count cascade) (count (:boxes wiring)))
        "one box per shown pattern")
    (is (= #{"descent-root" "descent-child" "co-app-peer"}
           (set (map :id (:boxes wiring)))))
    (is (some #(= :wire/seq (:type %)) (:wires wiring)))
    (is (some #(= :wire/copar (:type %)) (:wires wiring)))
    (is (empty? (:policy-holes out)))))

(deftest semilattice-fold-abstains-without-semilattice
  (let [out (fs/semilattice-fold cascade {})]
    (is (fold/valid-fold-output? out))
    (is (nil? (:coverage-score-delta out)))
    (is (empty? (get-in out [:wiring :boxes])))))

(deftest semilattice-fold-is-deterministic
  (is (= (fs/semilattice-fold cascade {:semilattice semilattice})
         (fs/semilattice-fold cascade {:semilattice semilattice}))))

(deftest close-loop-prefers-semilattice-fold
  (let [ag (cl/act-gate-from-lane-entry
            {:mission "M-semilattice"
             :cascade-score 0.3
             :policy-rollout-score nil
             :shown cascade
             :semilattice semilattice})]
    (is (= :fold-semilattice (:coverage-score/source ag)))
    (is (number? (:coverage-score-delta ag)))
    (is (neg? (:coverage-score-delta ag)))
    (is (= (count cascade) (count (get-in ag [:fold :wiring :boxes]))))))
