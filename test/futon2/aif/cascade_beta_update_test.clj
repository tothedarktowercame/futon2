(ns futon2.aif.cascade-beta-update-test
  "Per-context β update over cascade candidates (approved 2026-09-17,
  PROPOSAL-policy-precision-learning.md §1–§4; @R14 implementation).

  Required behaviour, from the model:
  - Order-only tie groups: candidates with equal G (1e-12) and the SAME
    pattern set. Tick 1's C1/C2 share G but their precedence SETS differ
    ([test sov] vs [placeholder test sov]), so tick 1 has NO group; a
    constructed pair of permutations of one set IS grouped.
  - The β update (Parr B.19 / Friston 2.7 fixed point) over tick-1 G with
    F from the observation (C0 = C1 = ##Inf contradicted, C2 = C3 = 0),
    prior β = 1, context :R, converges with β DECREASING (γ increasing),
    because the evidence moved mass toward lower-G candidates.
  - Contexts are independent: updating :R leaves :WM absent or unchanged."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.policy-precision :as pp]))

(def tick-1-g
  {:C0-empty 13.101074797244184
   :C1-test-first 11.434408130577516
   :C2-fix-first 11.434408130577516
   :C3-fix-only 12.101074797244184})

(def tick-1-candidates
  [{:id :C0-empty :precedence [] :g (:C0-empty tick-1-g)}
   {:id :C1-test-first :precedence [:test-step-covering-missing-total-repos
                                    :aif/structured-observation-vector]
    :g (:C1-test-first tick-1-g)}
   {:id :C2-fix-first :precedence [:aif/placeholder-is-load-bearing
                                   :test-step-covering-missing-total-repos
                                   :aif/structured-observation-vector]
    :g (:C2-fix-first tick-1-g)}
   {:id :C3-fix-only :precedence [:aif/structured-observation-vector]
    :g (:C3-fix-only tick-1-g)}])

;; Tick 2's observation per the proposal §2: partial summary observes cleanly,
;; no covering test yet — the "not clean at tau=1" predictions of C0/C1 are
;; contradicted (F = ##Inf), C2/C3 fit (F = 0).
(def tick-1-f
  {:C0-empty ##Inf :C1-test-first ##Inf :C2-fix-first 0.0 :C3-fix-only 0.0})

(deftest order-only-tie-groups-test
  (is (= [] (pp/order-only-tie-groups tick-1-candidates))
      "tick 1: C1/C2 share G but their pattern SETS differ, so no group —
      separating them by order is not excluded from the update")
  (is (= 1 (count (pp/order-only-tie-groups
                   [{:id :a :precedence [:x :y] :g 5.0}
                    {:id :b :precedence [:y :x] :g 5.0}])))
      "two equal-G permutations of one set form one order-only tie group")
  (is (= 3 (count (first (pp/order-only-tie-groups
                          [{:id :a :precedence [:x :y] :g 5.0}
                           {:id :b :precedence [:y :x] :g 5.0}
                           {:id :c :precedence [:x :y] :g 5.0}]))))
      "a third member of the same set and G joins the group")
  (is (= [] (pp/order-only-tie-groups
             [{:id :a :precedence [:x :y] :g 5.0}
              {:id :b :precedence [:y :x] :g 5.5}]))
      "same set but different G is not an order-only tie"))

(deftest cascade-beta-update-tick-1-test
  (let [states (pp/cascade-beta-update {} :R tick-1-candidates tick-1-f {})
        st (:R states)]
    (is (contains? states :R))
    (is (and (map? st)
             (true? (get-in st [:solve :converged?]))
             (true? (get-in st [:solve :bracketed?])))
        "the tick-1 solve converges and brackets")
    (is (and (number? (:beta st)) (< (:beta st) (:beta-prior st) ))
        "β decreases (γ = 1/β increases): evidence moved mass toward lower-G
        candidates (C0/C1 contradicted), so confidence in G rises")
    (is (> (/ 1.0 (:beta st)) (/ 1.0 (:beta-prior st)))
        "γ increases with the same β")
    ;; Fixed point vs one-step: the hand value 0.983 is ONE B.19 step at
    ;; β = 1; the solver closes the fixed point β = 1 + (π − π₀)·G, whose
    ;; dot weakens toward 0 as γ grows, so the root sits slightly ABOVE the
    ;; one-step value (measured: +0.00183).
    (is (< (Math/abs (- (:beta st) 0.983)) 0.01)
        "solved β is within 0.01 of claude-4's one-step hand value 0.983
        (fixed point, not one step — see the ns docstring)")
    (is (= [] (:merged-ties st))
        "tick 1 has no order-only tie group, so nothing is merged")
    (is (= [:C0-empty :C1-test-first] (:excluded-infinite-f st))
        "contradicted candidates (F = ##Inf) are excluded from π with p = 0,
        exactly, and recorded")
    (is (= 1.0 (:beta-prior st))
        "the prior is recorded")
    (is (every? (set (keys st))
                [:context :beta-source :beta-prior :beta :merged-ties
                 :candidates :g :f :solve])
        "the state records every declared value (the required keys at least)")))

(deftest cascade-beta-update-contexts-independent-test
  (let [wm-state {:status :present :beta 2.0 :beta-source :converged-posterior
                  :solved-tick-count 3}
        states (pp/cascade-beta-update {:WM wm-state} :R
                                       tick-1-candidates tick-1-f {})]
    (is (= wm-state (get states :WM))
        "an update in :R leaves :WM's state unchanged")
    (is (and (contains? states :R) (contains? states :WM)))))

(deftest cascade-beta-update-merges-order-only-ties-test
  ;; Constructed: two permutations of [:x :y] with equal G and unequal F; the
  ;; merge keeps the group's G with the group's MINIMUM F, and records it.
  (let [candidates [{:id :p :precedence [:x :y] :g 5.0}
                    {:id :q :precedence [:y :x] :g 5.0}
                    {:id :r :precedence [:z] :g 6.0}]
        states (pp/cascade-beta-update {} :R candidates
                                       {:p 0.0 :q 3.0 :r 0.0} {})
        st (:R states)]
    (is (= 1 (count (:merged-ties st)))
        "the order-only tie group is merged before solving")
    (is (= [:p :q] (-> st :merged-ties first :ids))
        "the merge records which candidates it merged")
    (is (= 0.0 (-> st :merged-ties first :f))
        "the representative carries the group's minimum F (at least one
        ordering fits the observation)")
    (is (true? (get-in st [:solve :converged?])))))

(deftest cascade-beta-update-refusals-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"numeric F"
                        (pp/cascade-beta-update {} :R
                                                [{:id :a :precedence [:x] :g 1.0}]
                                                {} {}))
      "a missing F is a typed refusal, not a default")
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"numeric F"
                        (pp/cascade-beta-update {} :R
                                                [{:id :a :precedence [:x] :g 1.0}]
                                                {:a :nope} {}))
      "a non-number F is a typed refusal")
  (let [st (get (pp/cascade-beta-update {} :R tick-1-candidates
                                        (assoc tick-1-f :C2-fix-first ##Inf
                                               :C3-fix-only ##Inf) {})
                :R)]
    (is (= :absent (:status st))
        "all candidates contradicted: no finite F remains, the state is held
        absent with the reason, never solved by substitution")))
