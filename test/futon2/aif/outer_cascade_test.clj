(ns futon2.aif.outer-cascade-test
  "H-T-CALLER-I: the outer cascade's `select`. Pure; over a fixture field of
  two eligible targets, one a requisition makes ineligible, and one exclusion."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-cascade :as oc])
  (:import [java.util SplittableRandom]))

(def field
  {:considered [{:target "M-a" :kind :mission :repo "futon3c" :path "holes/missions/M-a.md"}
                {:target "M-b" :kind :mission :repo "futon3c" :path "holes/missions/M-b.md"}
                {:target "M-c" :kind :mission :repo "futon3c" :path "holes/missions/M-c.md"}
                {:target "E-x" :kind :excursion :repo "futon2" :path "holes/E-x.md"}]
   :feasible [{:target "M-b" :kind :mission :next-step :ready :eligible true}
              {:target "M-c" :kind :mission :next-step :read-criteria :eligible false
               :ineligible-reason :requisition/mooted}
              {:target "M-a" :kind :mission :next-step :ask-interpretation :eligible true}]
   :exclusions [{:target "E-x" :kind :excursion :reason :not-lifecycle-shaped
                 :what-would-make-feasible {:form "futon4/holes/mission-lifecycle.md"}}]})

(deftest chooses-only-from-the-eligible-support
  (let [r (oc/select {:field field :seed 42 :trigger :wallclock-cron})
        s (:selection r)]
    (is (= ["M-a" "M-b"] (:support s)) "sorted by target id, the ineligible and the excluded out")
    (is (contains? #{"M-a" "M-b"} (:chosen-target r)))
    (is (= (:chosen-target r) (:chosen s)))
    (is (= 42 (:draw-seed r)))
    (testing "the excluded list is on the record, each with its reason"
      (is (= [{:target "E-x" :kind :excluded :reason :not-lifecycle-shaped
               :what-would-make-feasible {:form "futon4/holes/mission-lifecycle.md"}}
              {:target "M-c" :kind :ineligible :reason :requisition/mooted}]
             (:excluded s))))
    (is (= {:considered 4 :feasible 3 :eligible 2 :excluded 2} (:counts s)))
    (is (= :wallclock-cron (:trigger s)))))

(deftest the-seeded-draw-is-reproducible
  (let [a (oc/select {:field field :seed 7})
        b (oc/select {:field field :seed 7})]
    (is (= a b) "same seed, same field: same record")
    (testing "and recomputable from the record alone"
      (let [{:keys [seed value index order]} (get-in a [:selection :draw])]
        (is (= "java.util.SplittableRandom" (get-in a [:selection :draw :generator])))
        (is (= value (.nextDouble (SplittableRandom. (long seed)))))
        (is (= (:chosen-target a) (nth order index)))
        (is (= (:chosen-target a) (if (< value 0.5) "M-a" "M-b")) "two equal masses split at one half")))
    (testing "the order of the feasible list does not matter"
      (is (= a (oc/select {:field (update field :feasible (comp vec reverse)) :seed 7}))))
    (testing "over many seeds both eligible targets are reached and no other"
      (let [chosen (set (for [s (range 60)] (:chosen-target (oc/select {:field field :seed s}))))]
        (is (= #{"M-a" "M-b"} chosen))))))

(deftest the-posterior-is-e-over-the-support-and-g-is-a-typed-absence
  (let [s (:selection (oc/select {:field field :seed 1}))]
    (is (= {"M-a" 1/2 "M-b" 1/2} (:posterior s)))
    (is (= 1 (reduce + (vals (:posterior s)))))
    (is (= {:basis :uniform-no-data} (:E s)))
    (is (= {:absent :no-target-grain-g} (:g s)) "never a number standing in")
    (is (= :seeded-draw-from-E (:rule s)))))

(deftest one-eligible-target-is-chosen-whatever-the-seed
  (let [f (update field :feasible (fn [fs] (mapv #(if (= "M-b" (:target %)) (assoc % :eligible false :ineligible-reason :requisition/voted) %) fs)))]
    (doseq [seed [0 1 99 -5]]
      (is (= "M-a" (:chosen-target (oc/select {:field f :seed seed})))))))

(deftest an-empty-support-is-a-recorded-absence-not-a-refusal
  (let [f (update field :feasible (fn [fs] (mapv #(assoc % :eligible false :ineligible-reason :requisition/mooted) fs)))
        r (oc/select {:field f :seed 3})]
    (is (not (contains? r :chosen-target)))
    (is (not (contains? r :draw-seed)))
    (is (= {:absent :no-eligible-target} (get-in r [:selection :chosen])))
    (is (= {:absent :no-eligible-target} (get-in r [:selection :draw])))
    (is (= [] (get-in r [:selection :support])))
    (is (= 4 (count (get-in r [:selection :excluded]))) "everything not in the support is still listed")))

(deftest a-missing-seed-is-typed-and-no-target-is-chosen
  (let [r (oc/select {:field field})]
    (is (not (contains? r :chosen-target)))
    (is (= {:absent :no-seed} (get-in r [:selection :chosen])))
    (is (= ["M-a" "M-b"] (get-in r [:selection :support])))))
