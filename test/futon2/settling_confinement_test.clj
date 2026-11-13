(ns futon2.settling-confinement-test
  "T-ladder-05-settling-confinement acceptance: the exhaustive record over
  every binary ring for n = 3..20 (settling) and every fixed-point ring
  flipped at every site for n = 4..20 (damage) must match the cited
  independent reference (claude-4, 2026-11-11): maximum transient
  floor(n/2)-1, first exceeding 6 at n = 16 and 9 at n = 20 with worst
  start a single 1; exactly two rings per n, the uniform ones, never
  settle; damage depth 1 for every n = 4..20; settling claim refuted,
  confinement claim upheld."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.settling-confinement :as sc]))

(def ^:dynamic *record* nil)

(defn- weight ^long [^long v] (Long/bitCount v))

(defn- fixture [f]
  (binding [*record* (sc/record)] (f)))

(use-fixtures :once fixture)

(deftest exhaustive-scan-matches-reference
  (testing "one per-n entry for every n = 3..20, no ring excluded"
    (is (= (range 3 21) (mapv :n (:per-n *record*)))))
  (testing "maximum transient is floor(n/2) - 1 for every n"
    (is (every? (fn [{:keys [n max-transient]}]
                  (= (dec (quot n 2)) max-transient))
                (:per-n *record*))))
  (testing "it first exceeds 6 at n = 16 and is 9 at n = 20"
    (is (= 6 (-> (:per-n *record*) (nth (- 15 3)) :max-transient)))
    (is (= 7 (-> (:per-n *record*) (nth (- 16 3)) :max-transient)))
    (is (= 9 (-> (:per-n *record*) (nth (- 20 3)) :max-transient))))
  (testing "worst-case starting ring is a single 1 at every n"
    (is (every? (fn [{:keys [n worst-start]}]
                  (and (= 1 (weight (:value worst-start)))
                       (= (str (apply str (repeat (dec n) \0)) "1") (:ring worst-start))))
                (:per-n *record*))))
  (testing "exactly two rings per n never settle, the uniform ones"
    (is (every? (fn [{:keys [n never-settling-count never-settling-rings]}]
                  (and (= 2 never-settling-count)
                       (= (sorted-set (apply str (repeat n \0))
                                      (apply str (repeat n \1)))
                          (set (map :ring never-settling-rings)))))
                (:per-n *record*))))
  (testing "damage depth is 1 for every n = 4..20, both endpoints included"
    (is (every? (fn [{:keys [n max-damage-depth]}]
                  (or (< n 4) (= 1 max-damage-depth)))
                (:per-n *record*)))))

(deftest tn-claims-verdicts
  (testing "settling claim (at most 6 steps) is REFUTED with smallest counterexample"
    (let [c (:settling (:claims *record*))]
      (is (= :refuted (:verdict c)))
      (is (= {:n 3 :value 0 :ring "000"} (:smallest-counterexample c)))))
  (testing "confinement claim (damage depth at most 1) is UPHELD"
    (let [c (:confinement (:claims *record*))]
      (is (= :upheld (:verdict c)))
      (is (nil? (:smallest-counterexample c))))))

(deftest record-is-deterministic
  (testing "a second full enumeration yields the identical record"
    (is (= *record* (sc/record)))))
