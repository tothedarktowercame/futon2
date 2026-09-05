(ns futon2.report.cascade-lane-decision-target-test
  "‑:F9. Which target the cascade lane builds for.

  The lane has always prepended a `decision` entry (operator ruling
  2026-07-06, quoted at `cascade-lane/*gate-decision-target?*`). What it had
  no way to know was WHICH decision: before :F9 it took the head of
  `ranked-actions`, which is the RANKING's head and not the target the tick
  committed to. Over the 48 recorded S1b/S2/S4/S5 ticks those two disagree 48
  times out of 48
  (`holes/labs/wm-contract/runs/F9-cascade-decision/00-corpus-target-gap.edn`),
  so these tests use a ranked field where they disagree — a fixture in which
  they agreed would pass either way and measure nothing."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.report.cascade-lane :as lane]))

(def ^:private ranked
  "Rank-1 is an :advance-mission for M-ranking-head; the committed decision
  below is for a different mission. Two :open-mission entries make up the
  side-stream the lane also builds for."
  [{:rank 1 :action {:type :advance-mission :target "M-ranking-head"}}
   {:rank 2 :action {:type :open-mission :target "M-side-a"}}
   {:rank 3 :action {:type :open-mission :target "M-side-b"}}])

(def ^:private decision
  "The shape war-machine's `wm-decision` has: a map with an `:action`."
  {:action {:type :advance-mission :target "M-committed"}
   :reason :reviewed-live-reason-bearing-policy})

(defn- with-stub-constructor
  "Run `f` with the inputs the lane reaches outside itself replaced: the
  Python cascade constructor, the psi read and the rollout. What is left is
  the lane's own target selection, which is what these tests are about.

  The futility ledger is NOT stubbed — it is private, and it answers nil for
  every mission in this fixture (none of them exists), so it changes `:size`
  and `:shown` for nobody here and never the `:mission` these tests read."
  [f]
  (with-redefs [lane/cascade-policy-for
                (fn [psi _budget]
                  {:size 2
                   :shown [{:pattern "p/a"} {:pattern "p/b"}]
                   :semilattice {:descent [] :co_app []}
                   :cascade-score 1.0 :wholeness 1.5 :truncated false
                   :budget 6 :psi psi})
                lane/mission->psi (fn [m] (str "psi:" m))
                lane/policy-rollout (fn [_] -0.5)
                lane/policy-rollout-events (fn [_] [])]
    (f)))

(deftest supplied-decision-is-the-target-the-lane-builds-for
  (with-stub-constructor
    (fn []
      (let [entries (lane/cascade-lane ranked {:n 3 :decision decision})]
        (testing "entry #1 is the committed decision, not the ranking head"
          (is (= "M-committed" (:mission (first entries)))))
        (testing "the ranking head is not built for at all"
          (is (not (contains? (set (map :mission entries)) "M-ranking-head"))))
        (testing "the open-mission side-stream is unchanged"
          (is (= ["M-committed" "M-side-a" "M-side-b"]
                 (mapv :mission entries))))))))

(deftest without-a-decision-the-ranking-head-is-used
  (testing "the pre-:F9 behaviour is what the 1-arity path still gives"
    (with-stub-constructor
      (fn []
        (let [entries (lane/cascade-lane ranked {:n 3})]
          (is (= "M-ranking-head" (:mission (first entries))))
          (is (not (contains? (set (map :mission entries)) "M-committed"))))))))

(deftest a-decision-already-in-the-side-stream-is-not-duplicated
  (with-stub-constructor
    (fn []
      (let [entries (lane/cascade-lane
                     ranked {:n 3 :decision {:action {:type :advance-mission
                                                      :target "M-side-b"}}})]
        (is (= ["M-side-a" "M-side-b"] (mapv :mission entries))
            "one entry for the decision target, and it keeps its side-stream place")
        (is (= 1 (count (filter #(= "M-side-b" (:mission %)) entries))))))))

(deftest an-open-mission-decision-is-left-to-the-side-stream
  (testing "the existing :open-mission exclusion still applies to a supplied decision"
    (with-stub-constructor
      (fn []
        (let [entries (lane/cascade-lane
                       ranked {:n 3 :decision {:action {:type :open-mission
                                                        :target "M-side-a"}}})]
          (is (= ["M-side-a" "M-side-b"] (mapv :mission entries))))))))

(deftest a-decision-without-a-target-builds-no-decision-entry
  (with-stub-constructor
    (fn []
      (let [entries (lane/cascade-lane
                     ranked {:n 3 :decision {:action {:type :no-op}}})]
        (is (= ["M-side-a" "M-side-b"] (mapv :mission entries))
            "no target means no decision entry -- and NOT a silent fall back to rank-1")))))

(deftest the-gate-flag-still-turns-the-decision-entry-off
  (binding [lane/*gate-decision-target?* false]
    (with-stub-constructor
      (fn []
        (let [entries (lane/cascade-lane ranked {:n 3 :decision decision})]
          (is (= ["M-side-a" "M-side-b"] (mapv :mission entries))
              "with the gate off the supplied decision is ignored, as rank-1 was"))))))
