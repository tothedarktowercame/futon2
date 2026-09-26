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
    (is (= [:eligible] (:law-uses selection)))
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
    (is (= {:absent :no-target-grain-g} (:g selection)))))
