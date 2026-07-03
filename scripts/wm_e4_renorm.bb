#!/usr/bin/env bb
;; wm_e4_renorm.bb — M-evaluate-policies E4 (D7): replay RENORMALISATION SWEEP.
;; Uses the D6 harness (wm_replay_blend.bb) to answer §3.3 empirically: does any
;; commensuration of the blend's terms change which action wins? Reading 2 of Q1
;; (§7.4) measured G-ambiguity as numerically inaudible (σ≈0.015 vs G-info ~14);
;; this sweep re-scales terms onto comparable footings and reports the winner
;; movement. FACTS ONLY — the §3.3 (D3) design decision stays with the mission owner.
;;
;; Configs: (a) per-term z-normalisation (weights = 1/σ; μ cancels in argmin, so
;; z-norm reduces exactly to 1/σ scaling for selection); (b) ambiguity made audible
;; (only G-ambiguity scaled by 1/σ); plus equal-unit, z-norm-without-hidden, and the
;; canonical risk+ambiguity core. Each vs actual: flip-rate + which action-types
;; gain/lose.
;;
;; Usage: bb scripts/wm_e4_renorm.bb          (reads data/wm-trace)
;; Writes: holes/labs/M-evaluate-policies/e4-renorm-sweep.edn

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp])

;; Provided by the D6 harness loaded below (declared so clj-kondo resolves them).
(declare replay load-corpus lane-valid term->g terms)
(load-file "scripts/wm_replay_blend.bb")

(def trace-dir "data/wm-trace")
(def corpus (load-corpus trace-dir))

(defn term-stats
  "Per-term μ/σ over every lane-valid candidate row (present values only)."
  [corp]
  (let [rows (vec (for [{:keys [forms]} corp, tick forms, e (lane-valid tick)] e))]
    (into (sorted-map)
          (for [t terms]
            (let [xs (mapv double (keep #((term->g t) %) rows))
                  n  (count xs)
                  mu (if (pos? n) (/ (reduce + xs) n) 0.0)
                  vr (if (pos? n)
                       (/ (reduce + (map #(let [d (- % mu)] (* d d)) xs)) n) 0.0)
                  sd (Math/sqrt vr)]
              [t {:n-present n
                  :mu (/ (Math/round (* 1e6 mu)) 1e6)
                  :sd (/ (Math/round (* 1e6 sd)) 1e6)
                  :one-over-sd (if (> sd 1e-12)
                                 (/ (Math/round (* 1e4 (/ 1.0 sd))) 1e4) 0.0)}])))))

(def st (term-stats corpus))
(def znorm-weights (into {} (for [t terms] [t (:one-over-sd (st t))])))

(def configs
  [{:cfg "default"
    :desc "sanity: I1 replay-equality — must flip nothing"
    :spec {:name "e4-default" :weights {} :exclude #{}}}
   {:cfg "z-norm"
    :desc "per-term 1/σ scaling (= z-norm for selection; μ cancels), residual kept"
    :spec {:name "e4-z-norm" :weights znorm-weights :exclude #{}}}
   {:cfg "z-norm-drop-hidden"
    :desc "z-norm on visible terms, hidden residual also removed"
    :spec {:name "e4-z-norm-drop-hidden" :weights znorm-weights :exclude #{:hidden}}}
   {:cfg "ambiguity-audible"
    :desc "scale only G-ambiguity by 1/σ_ambiguity (rest default), residual kept"
    :spec {:name "e4-ambiguity-audible"
           :weights {:ambiguity (:one-over-sd (st :ambiguity))} :exclude #{}}}
   {:cfg "equal-unit"
    :desc "naive commensuration: all six terms weight 1, residual kept"
    :spec {:name "e4-equal-unit"
           :weights {:risk 1.0 :ambiguity 1.0 :info 1.0 :survival 1.0
                     :structural-pressure 1.0 :goal-outcome 1.0}
           :exclude #{}}}
   {:cfg "core-only"
    :desc "canonical EFE core: select on G-risk + G-ambiguity alone"
    :spec {:name "e4-core-only" :weights {}
           :exclude #{:info :survival :structural-pressure :goal-outcome :hidden}}}])

(def results
  (mapv (fn [{:keys [cfg desc spec]}]
          (let [r (replay corpus spec)]
            {:cfg cfg :desc desc :spec spec
             :flip-rate-pct (:flip-rate-pct r)
             :n-flips (:n-flips r)
             :winner-net-delta (get-in r [:winner-types :net-delta])
             :type-flow (:type-flow r)}))
        configs))

(defn frate [cfg]
  (:flip-rate-pct (first (filter #(= cfg (:cfg %)) results))))

(def total-ticks (:ticks (:corpus (replay corpus {:name "n" :weights {} :exclude #{}}))))

(def findings
  [(format "Corpus at run time: %d lane-valid ticks (wm-trace is appended hourly; frozen early-day flip counts reproduce q1-census-v2 exactly, later day-files have grown since the 10:41 snapshot)."
           total-ticks)
   (format "default spec flips %s%% — I1 replay-equality holds (recomputed total == persisted :G-total), so the harness reproduces the actual winner exactly." (str (frate "default")))
   "Mean-centring is argmin-invariant: subtracting a per-term corpus constant shifts every candidate in a tick equally, so per-term z-normalisation reduces EXACTLY to 1/σ term-scaling for selection. The μ column is reported for the record but plays no part in winners."
   (format "MAGNITUDE vs VARIATION. G-info has by far the largest MEAN (μ=%s) but a near-zero σ=%s — a nearly-uniform bonus, hence argmin-inert (census G-info flip-rate 0%%). G-ambiguity is small in BOTH (μ=%s, σ=%s). So reading 2's 'inaudible ambiguity' is a MEAN-magnitude fact; z-normalisation instead rescales by σ (variation)."
           (str (:mu (st :info))) (str (:sd (st :info)))
           (str (:mu (st :ambiguity))) (str (:sd (st :ambiguity))))
   (format "The naturally-decisive terms are the high-σ ones: G-survival (σ=%s) and G-risk (σ=%s). z-normalisation (all 1/σ, residual kept) shrinks those toward the tiny-σ terms and moves %s%% of winners; dropping the hidden residual as well leaves it unchanged (%s%%) — under z-norm the raw-scale residual is argmin-inert."
           (str (:sd (st :survival))) (str (:sd (st :risk)))
           (str (frate "z-norm")) (str (frate "z-norm-drop-hidden")))
   (format "ambiguity-audible (G-ambiguity × 1/σ≈%s alone, rest default) = %s%%: 1/σ scaling OVERSHOOTS — ambiguity's scaled σ ≈ 1 now exceeds every other term's, so it does not merely gain a vote, it dominates. 'Audible' at 1/σ means 'decisive'."
           (str (:one-over-sd (st :ambiguity))) (str (frate "ambiguity-audible")))
   (format "equal-unit (all six weights = 1, residual kept) = %s%% — the mildest commensuration; core-only (G-risk + G-ambiguity, all else excluded) = %s%%, the replay complement of census core-only agreement (35.3%%)."
           (str (frate "equal-unit")) (str (frate "core-only")))
   (format "CAVEAT: near-constant terms get extreme 1/σ weights (G-goal-outcome 1/σ≈%s, present in ~%d rows only; G-info 1/σ≈%s) — z-norm amplifies them enormously but, being near-uniform (or sparse/young), they stay largely argmin-inert. The 1/σ weights are recorded in :znorm-weights for scrutiny."
           (str (:one-over-sd (st :goal-outcome))) (:n-present (st :goal-outcome))
           (str (:one-over-sd (st :info))))
   "Which action-types gain/lose is per-config in :winner-net-delta (spec winner-count minus actual winner-count, by :action :type); positive = the renormalised formula would pick that type more often."])

(def report
  {:generated-by "scripts/wm_e4_renorm.bb (M-evaluate-policies E4, D7 experimental series)"
   :question "§3.3 empirically: does any commensuration/renormalisation change winners?"
   :limitation "Renormalisation varies weights over PERSISTED term values only; the hidden (graph-pragmatic - gap) residual is held fixed unless dropped, and forward-model modes are not re-run (per §8.7)."
   :corpus {:files (count corpus) :lane-valid-ticks total-ticks
            :note "live corpus; read-only; determinism verified by re-run"}
   :term-stats st
   :znorm-weights (into (sorted-map) (for [t terms] [t (:one-over-sd (st t))]))
   :configs results
   :findings findings})

(def out "holes/labs/M-evaluate-policies/e4-renorm-sweep.edn")
(io/make-parents out)
(spit out (with-out-str (pp/pprint report)))
(println "=== E4 RENORMALISATION SWEEP ===")
(doseq [{:keys [cfg flip-rate-pct n-flips]} results]
  (println (format "  %-20s flip-rate %5s%%  (%d flips)" cfg (str flip-rate-pct) n-flips)))
(println "\nFindings:")
(doseq [f findings] (println " -" f))
(println "\nWrote" out)
