(ns futon2.aif.outer-cascade-test
  "H-T-CALLER-I: the outer cascade's `select`. Pure; over a fixture field of
  two eligible targets, one a requisition makes ineligible, and one exclusion."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-cascade :as oc]
            [futon2.aif.target-field :as tf]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.flight-runner :as fr])
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
        s (:target-selection r)]
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
      (let [{:keys [seed value index order]} (get-in a [:target-selection :draw])]
        (is (= "java.util.SplittableRandom" (get-in a [:target-selection :draw :generator])))
        (is (= value (.nextDouble (SplittableRandom. (long seed)))))
        (is (= (:chosen-target a) (nth order index)))
        (is (= (:chosen-target a) (if (< value 0.5) "M-a" "M-b")) "two equal masses split at one half")))
    (testing "the order of the feasible list does not matter"
      (is (= a (oc/select {:field (update field :feasible (comp vec reverse)) :seed 7}))))
    (testing "over many seeds both eligible targets are reached and no other"
      (let [chosen (set (for [s (range 60)] (:chosen-target (oc/select {:field field :seed s}))))]
        (is (= #{"M-a" "M-b"} chosen))))))

(deftest the-posterior-is-e-over-the-support-and-g-is-a-typed-absence
  (let [s (:target-selection (oc/select {:field field :seed 1}))]
    (is (= {"M-a" 1/2 "M-b" 1/2} (:posterior s)))
    (is (= 1 (reduce + (vals (:posterior s)))))
    (is (= {:basis :uniform-no-data} (:E s)))
    (is (= :E-only (:law s)) "no entry carries a :delta-g value, so the posterior is E alone")
    (is (= [] (:g-defined-on s)))
    (is (= {"M-a" {:absent :no-target-grain-g} "M-b" {:absent :no-target-grain-g}} (:g s))
        "per-target: no :delta-g key on the entry is the HG2-Ia default, never a number")
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
    (is (= {:absent :no-eligible-target} (get-in r [:target-selection :chosen])))
    (is (= {:absent :no-eligible-target} (get-in r [:target-selection :draw])))
    (is (= [] (get-in r [:target-selection :support])))
    (is (= 4 (count (get-in r [:target-selection :excluded]))) "everything not in the support is still listed")))

(deftest a-missing-seed-is-typed-and-no-target-is-chosen
  (let [r (oc/select {:field field})]
    (is (not (contains? r :chosen-target)))
    (is (= {:absent :no-seed} (get-in r [:target-selection :chosen])))
    (is (= ["M-a" "M-b"] (get-in r [:target-selection :support])))))

(defn declared-input-fixture []
  (let [text (slurp "test/fixtures/target-field/M-autoclock-in@futon3c-7466251c.md")
        entries (mapv (fn [id]
                        (tf/assess {:read-text (fn [& _] text) :observe (constantly false)
                                    :sources {} :store "/nonexistent/outer-inputs"}
                                   {:target id :kind :mission :repo "futon3c"
                                    :path "holes/missions/M-autoclock-in.md"
                                    :status-line (re-find #"(?m)^\*\*Status:\*\*.*$" text)}))
                      ["M-autoclock-in" "M-other"])
        entries (tf/with-pair-overlap
                 (mapv #(assoc % :universe #{:shared}
                               :constructed-candidate {:produces #{:shared}}) entries))
        receipts (mapv (fn [click]
                         (eh/increment {:click click :candidate :c
                                        :attempts [{:pattern :p :success true}]}
                                       [:pattern-cascade "M-autoclock-in" [:p] {}] []))
                       ["click-1" "click-2"])
        records (:enactment-records (eh/fold nil receipts))
        publication (:publication-observed ((fr/observe-publication-fn {})
                                            {:target "M-autoclock-in"} {:click-id "click-2"}))]
    {:field {:considered entries :feasible entries :exclusions []}
     :seed 42 :enactment-records records :publication-observed publication
     ;; persist-clock!'s durable edge props: string keys, not keywordised.
     ;; This shape is caller data, not a claim of invoking futon3c's writer.
     :clock-lineage {"agent-id" "codex-1" "session-id" "test-session"
                     "clocked-at-ms" 1000 "mission-id" "M-autoclock-in"
                     "witness" "test"}}))

(deftest declared-inputs-reach-the-record-without-changing-the-law
  (let [opts (declared-input-fixture)
        result (oc/select opts)
        selection (:target-selection result)
        inputs (:inputs selection)
        law #(dissoc (:target-selection %) :inputs)]
    (is (= 2 (count (:support selection))))
    (is (= [:eligible :delta-g] (:law-uses selection)))
    (doseq [k [:next-step :pair-overlap]]
      (is (= (into {} (map (juxt :target k) (get-in opts [:field :feasible]))) (get inputs k)))
      (is (every? some? (vals (get inputs k)))))
    (is (every? keyword? (vals (:next-step inputs))))
    (is (= {:incommensurable {:shared-tokens [:shared]}}
           (get-in inputs [:pair-overlap "M-autoclock-in" "M-other"])))
    (is (= 2 (count (:enactment-records inputs))))
    (doseq [k [:enactment-records :publication-observed :clock-lineage]]
      (is (= (get opts k) (get inputs k)))
      (doseq [missing [::omitted nil]]
        (let [changed (oc/select (if (= missing ::omitted) (dissoc opts k) (assoc opts k nil)))]
          (is (= {:absent :not-supplied} (get-in changed [:target-selection :inputs k])))
          (is (= (law result) (law changed)))
          (is (= (:chosen-target result) (:chosen-target changed)))))
      (let [changed (oc/select (assoc opts k {:absent :writer-unavailable}))]
        (is (= {:absent :writer-unavailable} (get-in changed [:target-selection :inputs k])))
        (is (= (law result) (law changed)))))
    (doseq [k [:next-step :pair-overlap]]
      (let [changed (oc/select (update-in opts [:field :feasible]
                                         #(mapv (fn [e] (dissoc e k)) %)))]
        (is (every? #(= {:absent :no-such-key-on-entry} %)
                    (vals (get-in changed [:target-selection :inputs k]))))
        (is (= (law result) (law changed)))
        (is (= (:chosen-target result) (:chosen-target changed)))))
    (doseq [k [:enactment-records :publication-observed :clock-lineage]]
      (let [changed (oc/select (assoc opts k {:different :value}))]
        (is (= {:different :value} (get-in changed [:target-selection :inputs k])))
        (is (= (law result) (law changed)))))
    (is (= {:basis :uniform-no-data} (:E selection)))
    (is (= [:eligible :delta-g] (:law-uses selection)))
    (is (= :E-only (:law selection)))
    (is (every? #(contains? % :absent) (vals (:g selection)))
        "every target's :g entry is a typed absence, never a number")))

;;; HG2-Ib: the mixture law over per-target ΔG (H-G-TARGET part 2, law side).

(def field-with-delta-g
  "Two eligible targets: M-a carries a recorded ΔG value (HG2-Ia shape), M-b a
  typed absence."
  (update field :feasible
          (fn [fs]
            (mapv #(case (:target %)
                     "M-a" (assoc % :delta-g {:value 2.0 :universe [:u]
                                              :receipt-digest "served/abc"
                                              :baseline-g {:value :universe}
                                              :g-of-best {:value 4.0 :universe [:u]}})
                     "M-b" (assoc % :delta-g {:absent :no-constructed-candidate
                                              :next-step :ready})
                     %)
                  fs))))

(deftest a-single-member-d-cannot-move-mass
  (let [s (:target-selection (oc/select {:field field-with-delta-g :seed 11}))]
    (is (= :mixed (:law s)))
    (is (= ["M-a"] (:g-defined-on s)) "the sorted vector of D")
    (is (= {"M-a" {:delta 2.0 :universe [:u]}
            "M-b" {:absent :no-constructed-candidate :next-step :ready}}
           (:g s))
        "value re-keyed to {:delta :universe}; the absent target's :g entry is its typed absence, never a number")
    (testing "p(a) = E(D)·E_a·e^(−Δ)/Σ_{s∈D} E_s·e^(−ΔG_s); with D = {a} the softmax factor is 1, so p(a) = E(D) = E_a — a single-member D cannot move mass"
      (let [p (:posterior s)]
        (is (= 0.5 (get p "M-a")) "D member recorded as a double")
        (is (= 1/2 (get p "M-b")) "t ∉ D keeps the exact ratio E_t")
        (is (= 1.0 (double (reduce + (vals p)))))))))

(def three-target-field
  {:considered [{:target "t-a"} {:target "t-b"} {:target "t-c"}]
   :feasible [{:target "t-b" :eligible true
               :delta-g {:value 2.0 :universe [:u] :receipt-digest "served/b"}}
              {:target "t-c" :eligible true :delta-g {:absent :no-evaluator-supplied}}
              {:target "t-a" :eligible true
               :delta-g {:value 0.0 :universe [:u] :receipt-digest "served/a"}}]
   :exclusions []})

(deftest the-softmax-moves-mass-between-d-members-only
  (let [s (:target-selection (oc/select {:field three-target-field :seed 5}))
        p (:posterior s)
        wa 1.0 wb (Math/exp -2.0)
        e-d 2/3]
    (is (= ["t-a" "t-b"] (:g-defined-on s)))
    (is (= :mixed (:law s)))
    (testing "the law's terms, shown: E uniform 1/3, D = {t-a,t-b}, E(D) = 2/3"
      (is (= (double (* e-d (/ wa (+ wa wb)))) (get p "t-a")))
      (is (= (double (* e-d (/ wb (+ wa wb)))) (get p "t-b")))
      (is (= 1/3 (get p "t-c")) "the absent target keeps exactly E_t, an exact ratio"))
    (testing "mass visibly moved between the D members, and only there"
      (is (> (get p "t-a") 1/3) "the smaller ΔG draws mass")
      (is (< (get p "t-b") 1/3))
      (is (< (Math/abs (- 1.0 (double (reduce + (vals p))))) 1e-12)))))

(deftest the-draw-is-over-the-mixture-posterior-and-replays
  (let [a (oc/select {:field three-target-field :seed 5})
        b (oc/select {:field three-target-field :seed 5})
        s (:target-selection a)]
    (is (= a b) "same seed and field: identical record")
    (is (= (oc/draw (:posterior s) 5) (:draw s)) "the seeded draw from that posterior")
    (is (= (:chosen-target a) (nth (get-in s [:draw :order]) (get-in s [:draw :index]))))
    (is (= 5 (:draw-seed a)))
    (is (= {"t-c" {:absent :no-evaluator-supplied}
            "t-a" {:delta 0.0 :universe [:u]}
            "t-b" {:delta 2.0 :universe [:u]}}
           (:g s)))))

(deftest a-malformed-delta-g-is-a-typed-absence-never-a-number
  (let [f (assoc three-target-field
                 :feasible [{:target "t-a" :eligible true
                             :delta-g {:value 1.0}} ; no :universe: forged
                            {:target "t-b" :eligible true
                             :delta-g {:value 2.0 :universe [:u] :receipt-digest "served/b"}}
                            {:target "t-c" :eligible true :delta-g {:absent :no-evaluator-supplied}}])
        s (:target-selection (oc/select {:field f :seed 5}))]
    (is (= {:absent :delta-g-malformed} (get (:g s) "t-a"))
        "a :delta-g value with no :universe is a typed absence")
    (is (= ["t-b"] (:g-defined-on s)) "the forged entry is not in D")
    (is (= 1/3 (get (:posterior s) "t-a")) "it keeps exactly E_t — never given a number")
    (is (every? (fn [[t v]] (if (= t "t-b") (double? v) (ratio? v))) (:posterior s)))
    (testing "and when nothing well-formed remains, D is empty and the law is E-only"
      (let [f2 (assoc three-target-field
                      :feasible [{:target "t-a" :eligible true :delta-g {:value 1.0}}
                                 {:target "t-b" :eligible true :delta-g {:absent :no-evaluator-supplied}}])
            s2 (:target-selection (oc/select {:field f2 :seed 5}))]
        (is (= :E-only (:law s2)))
        (is (= [] (:g-defined-on s2)))
        (is (= {"t-a" 1/2 "t-b" 1/2} (:posterior s2)))
        (is (= {"t-a" {:absent :delta-g-malformed}
                "t-b" {:absent :no-evaluator-supplied}}
               (:g s2)))))))
