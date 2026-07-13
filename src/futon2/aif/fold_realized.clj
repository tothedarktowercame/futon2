(ns futon2.aif.fold-realized
  "PRODUCER of the R16→R14 `:realized-outcome` trace — the enactor side of the
   close-the-loop ↔ precision-over-policies seam (E-close-the-loop ×
   E-precision-over-policies, contract committed with claude-11, 2026-06-26).

   When a fold's `:wiring` is ENACTED (`apply-cascade!`) and we re-observe, this
   builds the record γ reads next tick:

     {:policy <id> :expected-score <g> :realized-score <g'> :tick <enactment tick>}

   Both G legs are the SAME EFE quantity — the fold's coverage→rollout ΔG
   (`futon2.aif.fold-eval`) — evaluated over the PREDICTED wiring (`:expected-score`,
   = the `:coverage-score-delta` the act-gate consumed at decision time) vs the actually-
   ENACTED wiring (`:realized-score`, recomputed post-enactment: holes filled / boxes
   failed ⇒ different coverage). So γ's relative error is apples-to-apples, not a
   ΔG-vs-ΔF mismatch. The reader is `selection-gain/fold-realized-outcome`.

   STAGED (no-live-pilot-edit discipline; the 2026-06-26 incident): enactment is
   NOT wired yet (Joe's call on touching the recovered pilot). `*live-wire?*`
   defaults false ⇒ `with-realized-outcome` returns the judge-output UNCHANGED ⇒
   `trace-record` emits no `:realized-outcome` key ⇒ γ holds at the prior 1.0
   (reduction-safe). The PURE `realized-outcome-of` is always available for the
   end-to-end judge→trace→γ integration test claude-11 wires.

   Pure + no I/O + no loop + flag-gated: `fold.clj`'s expected-only invariant is
   untouched (this ns is the OBSERVE-side producer, not a fold-contract term)."
  (:require [clojure.edn :as edn]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.fold-eval :as fe]))

(def ^:dynamic *live-wire?*
  "Default ON as of 2026-07-08 (Joe-directed): enactment is live-wired. When ON,
   the `:realized-outcome` is produced and rides the trace to γ. LATENT until R10
   (the live loop) runs — arming this makes enactment live-CAPABLE, not
   live-RUNNING; real substrate writes happen only when the loop is invoked. Bind
   false to restore the no-op (γ stays at the prior)."
  true)

(def ^:dynamic *selection-gain-grounded-feed?*
  "R14 live-wire migration (default ON as of 2026-07-08, Joe-directed; a SEPARATE
   arm from `*live-wire?*`). When ON — and only when `*live-wire?*` is also ON —
   the `:realized-outcome` feeding γ is the A5 SUBSTRATE DIAL
   (`realized-outcome-grounded`, bound−inhabited endpoint counts) instead of
   `realized-outcome-of` (coverage→ΔG, the reproduction mirror the R16 exhibit
   named). INERT UNTIL DATA: γ stays starved until real fold-variance flows, so
   arming has no effect until a run produces grounded samples. The grounded
   producer reads an A3 build-match snapshot (a live substrate read per
   enactment); the enact path is `try`-wrapped so a read failure returns the
   judgement unchanged. Bind false to restore coverage-ΔG."
  true)

(defn- realized-coverage
  "ZERO-COVERAGE SEMANTICS (claude-16 ruling, 2026-07-06, T-0 fix for the
   gamma realizedG=nil blocker): an executor that constructs 0 boxes against a
   plan with N obligations (policy-holes) has produced a measured outcome of
   ZERO coverage, not an unmeasurable nil. realized-G should be 0.0 in that
   case — a real (if bad) sample for γ to calibrate on.

   This DIFFERS from `fold-eval/coverage` deliberately: the GATE returns nil
   for zero boxes (⇒ abstain — no construction to evaluate), which is correct
   for the gate's decision. But the REALIZED path is post-enactment
   observation: the enactment happened, the executor ran, it produced zero —
   that is a measurement, not an absence.

   nil is preserved ONLY for the genuinely unmeasurable case: no boxes AND no
   holes (nothing was enacted at all, or the wiring is nil/empty)."
  [wiring]
  (let [folded (count (:boxes wiring))
        holes  (count (:policy-holes wiring))
        total  (+ folded holes)]
    (cond
      (pos? folded) (/ (double folded) (double total))
      (pos? total)  0.0           ; zero boxes but obligations exist → measured zero
      :else         nil)))        ; no boxes AND no holes → genuinely unmeasurable

(defn realized-outcome-of
  "PURE: the R16→R14 `:realized-outcome` record for an enacted decision.
     `decision`       — carries `:policy` and the expected leg: either an explicit
                        `:expected-score`, or `:fold` (the fold output, whose `:coverage-score-delta`
                        the gate consumed).
     `enacted-wiring` — the post-enactment wiring (`:boxes`/`:policy-holes`);
                        `:realized-score` = the SHARED coverage→rollout ΔG over it.
     `tick`           — the enactment tick (γ dedups on it).
   `:realized-score` is nil ONLY when nothing was enacted at all (no boxes AND no
   holes) — γ then holds (it requires a number). When the executor produced
   zero boxes against N obligations, realized-G is 0.0 (zero-coverage
   semantics, claude-16 ruling 2026-07-06).

   SCALE-MATCH PIN (claude-10 review must-fix, 2026-07-02): `:expected-score`
   PREFERS the fold's coverage-ΔG leg whenever present — never a gate-side
   rollout-G — because `:realized-score` is always coverage-ΔG, and a rollout-vs-
   coverage pair would feed γ the same scale-mismatched junk class that pinned
   it at 0.5 (the retired v0 feed). The gate still verdicts on its own
   reconciled leg; only γ's calibration pair is constrained here, at the
   contract, so no future caller can reintroduce the mismatch."
  [{:keys [policy expected-score fold]} enacted-wiring tick]
  {:policy     policy
   :expected-score (or (:coverage-score-delta fold)
                       (when (number? expected-score) expected-score))
   :realized-score (fe/coverage->score-delta (realized-coverage enacted-wiring))
   :tick       tick})

(defn- deposit-for-mission
  [mission-id]
  (let [target (a3/mission-key mission-id)]
    (->> (vals (a3/deposits-by-id))
         (filter #(= target (a3/mission-key (:mission %))))
         first)))

(defn- reviewed-clean-for
  [mission-id deposit]
  (let [mission (:mission deposit mission-id)
        path (or (get a3/reviewed-candidate-cleans mission)
                 (get a3/reviewed-candidate-cleans mission-id))]
    (when path
      (edn/read-string (slurp path)))))

(defn- grounded-deposit
  [mission-id {:keys [deposit clean box-bindings] :as _opts}]
  (let [base (or deposit
                 (deposit-for-mission mission-id)
                 {:mission mission-id})
        clean' (or clean
                   (:clean base)
                   (reviewed-clean-for mission-id base))
        mission (or (:mission base) mission-id)]
    (cond-> (assoc base :mission mission)
      clean' (assoc :clean clean')
      box-bindings (assoc :box-bindings box-bindings))))

(defn- fold-plan-coverage
  [deposit]
  (some (fn [[source wiring]]
          (when-let [coverage (fe/coverage wiring)]
            {:expected-source source
             :predicted-coverage coverage}))
        [[:fold-coverage (get-in deposit [:turn :answer])]
         [:fold-coverage (:wiring deposit)]]))

(defn- expected-remaining
  [bound deposit]
  (if-let [{:keys [expected-source predicted-coverage]} (fold-plan-coverage deposit)]
    {:expected-score (long (Math/round (double (* bound (- 1.0 predicted-coverage)))))
     :expected-source expected-source
     :predicted-coverage predicted-coverage}
    {:expected-score 0
     :expected-source :perfection-target}))

(defn realized-outcome-grounded
  "A5 grounded realized outcome: read the WORLD dial via A3 build-match.

   `:realized-score` is the remaining count of bound CLean boxes whose reviewed
   substrate endpoints are not inhabited: `bound - inhabited`. `:expected-score` is
   the fold's predicted remaining endpoint count when a fold plan is present,
   otherwise an explicitly labelled perfection-target fallback. Both legs are
   endpoint counts, never coverage-ΔG mixed with substrate state."
  ([mission-id decision] (realized-outcome-grounded mission-id decision {}))
  ([mission-id {:keys [policy tick] :as _decision} opts]
   (let [deposit (grounded-deposit mission-id opts)
         snapshot (a3/box-match-snapshot deposit opts)
         bound (count snapshot)
         inhabited (count (filter :inhabited? snapshot))
         remaining (when (pos? bound) (- bound inhabited))
         expected (expected-remaining bound deposit)
         expected-g (:expected-score expected)
         exact? (and (number? remaining) (= expected-g remaining))]
     (cond-> {:policy (or policy mission-id)
              :mission (:mission deposit mission-id)
              :expected-score expected-g
              :expected-source (:expected-source expected)
              :predicted-coverage (:predicted-coverage expected)
              :realized-score remaining
              :realized-source :substrate-dial
              :scale :endpoint-count
              :dial {:inhabited inhabited
                     :bound bound
                     :remaining remaining
                     :discharged? (and (pos? bound) (zero? remaining))}
              :box-snapshot snapshot
              :anti-tautology-flag (when exact? :exact-grounded-forecast)}
       (or tick (:tick opts)) (assoc :tick (or tick (:tick opts)))))))

(defn with-realized-outcome
  "STAGED seam: thread the `:realized-outcome` onto a `judge`-style output so it
   flows through `trace-record` (claude-11's reader picks it up from
   `latest-trace-record` next tick). No-op unless `*live-wire?*` — enactment isn't
   wired (Joe's call). Returns `judge-output` UNCHANGED when staged-off, so the
   field stays ABSENT and γ holds at the prior.

   Under `*selection-gain-grounded-feed?*` (the R14 migration, also armed) the realized
   outcome is the A5 substrate dial (`realized-outcome-grounded`) rather than
   `realized-outcome-of` (coverage). mission-id = `(:policy decision)`."
  [judge-output decision enacted-wiring tick]
  (if *live-wire?*
    (assoc judge-output :realized-outcome
           (if *selection-gain-grounded-feed?*
             (realized-outcome-grounded (:policy decision) decision {:tick tick})
             (realized-outcome-of decision enacted-wiring tick)))
    judge-output))
