(ns futon2.aif.cascade-beta-update-test
  "Per-context β update over cascade candidates (approved 2026-09-17,
  PROPOSAL-policy-precision-learning.md §1–§4, corrected 2026-09-17: no
  order-only tie merge — B.19 already treats equal-G ties correctly when both
  candidates are interior in π; @R14 implementation).

  Required behaviour, from the model:
  - The β update (Parr B.19 / Friston 2.7 fixed point) over tick-1 G with
    F from the observation (C0 = C1 = ##Inf contradicted, C2 = C3 = 0),
    prior β = 1, context :R, converges with β DECREASING (γ increasing),
    because the evidence moved mass toward lower-G candidates.
  - The corrected tie premise, asserted where it is TRUE: moving probability
    between equal-G candidates that are BOTH interior in π contributes
    exactly zero to (π − π₀)·G — swapping two finite F values between the
    equal-G pair C1/C2 leaves the solved β unchanged; and SWAPPING ##Inf
    between the pair (C1 = ##Inf, C2 = 0 vs C1 = 0, C2 = ##Inf) also leaves
    β unchanged, because the pair's equal G and equal prior make its total
    contribution identical. (The variant claude-4 first proposed — C1 from
    ##Inf to C2's finite value — is false: renormalisation pulls mass from
    C3, a different-G candidate; measured β 0.9848 vs 0.9072, reported and
    corrected 2026-09-17.)
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

(defn- solved-beta
  [f]
  (get-in (pp/cascade-beta-update {} :R tick-1-candidates f {})
          [:R :beta]))

(deftest cascade-beta-update-tick-1-test
  (let [states (pp/cascade-beta-update {} :R tick-1-candidates tick-1-f {})
        st (:R states)]
    (is (contains? states :R))
    (is (and (map? st)
             (true? (get-in st [:solve :converged?]))
             (true? (get-in st [:solve :bracketed?])))
        "the tick-1 solve converges and brackets")
    (is (and (number? (:beta st)) (< (:beta st) (:beta-prior st)))
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
    (is (= [:C0-empty :C1-test-first] (:excluded-infinite-f st))
        "contradicted candidates (F = ##Inf) are excluded from π with p = 0,
        exactly, and recorded")
    (is (= 1.0 (:beta-prior st))
        "the prior is recorded")
    (is (every? (set (keys st))
                [:context :beta-source :beta-prior :beta
                 :candidates :g :f :solve])
        "the state records every declared value (the required keys at least)")))

(deftest equal-g-tie-contributes-nothing-when-interior-test
  ;; The corrected premise where it holds: C1 and C2 share G, and swapping
  ;; their FINITE F values moves probability between equal-G interior
  ;; candidates — (π − π₀)·G is unchanged, so β is unchanged.
  (let [f-a {:C0-empty 0.0 :C1-test-first 3.0 :C2-fix-first 0.0 :C3-fix-only 0.0}
        f-b {:C0-empty 0.0 :C1-test-first 0.0 :C2-fix-first 3.0 :C3-fix-only 0.0}
        beta-a (solved-beta f-a)
        beta-b (solved-beta f-b)]
    (is (< (Math/abs (- beta-a beta-b)) 1.0e-9)
        (str "equal-G interior swap leaves the solved β unchanged: "
             beta-a " vs " beta-b))))

(deftest boundary-tie-swap-is-neutral-test
  ;; Corrected invariance (claude-4, 2026-09-17): SWAP C1's and C2's F —
  ;; (a) C1 = ##Inf, C2 = 0; (b) C1 = 0, C2 = ##Inf; C0 = ##Inf, C3 = 0 in
  ;; both. C1 and C2 have equal G and equal prior, so the pair's total
  ;; contribution to (π − π₀)·G is identical in (a) and (b) and the solved β
  ;; is the same. (The earlier variant — changing C1 from ##Inf to C2's
  ;; finite value — is NOT invariant: it re-includes C1 and pulls mass from
  ;; C3, a different-G candidate; measured 0.9848 vs 0.9072, reported
  ;; 2026-09-17.)
  (let [beta-a (solved-beta tick-1-f)
        beta-b (solved-beta {:C0-empty ##Inf :C1-test-first 0.0
                             :C2-fix-first ##Inf :C3-fix-only 0.0})]
    (is (< (Math/abs (- beta-a beta-b)) 1.0e-12)
        (str "swapping F between the equal-G pair leaves the solved β
             unchanged: (a) " beta-a " vs (b) " beta-b))))

(deftest cascade-beta-update-contexts-independent-test
  (let [wm-state {:status :present :beta 2.0 :beta-source :converged-posterior
                  :solved-tick-count 3}
        states (pp/cascade-beta-update {:WM wm-state} :R
                                       tick-1-candidates tick-1-f {})]
    (is (= wm-state (get states :WM))
        "an update in :R leaves :WM's state unchanged")
    (is (and (contains? states :R) (contains? states :WM)))))

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
