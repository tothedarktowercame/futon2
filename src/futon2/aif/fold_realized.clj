(ns futon2.aif.fold-realized
  "PRODUCER of the R16→R14 `:realized-outcome` trace — the enactor side of the
   close-the-loop ↔ precision-over-policies seam (E-close-the-loop ×
   E-precision-over-policies, contract committed with claude-11, 2026-06-26).

   When a fold's `:wiring` is ENACTED (`apply-cascade!`) and we re-observe, this
   builds the record γ reads next tick:

     {:policy <id> :expected-G <g> :realized-G <g'> :tick <enactment tick>}

   Both G legs are the SAME EFE quantity — the fold's coverage→rollout ΔG
   (`futon2.aif.fold-eval`) — evaluated over the PREDICTED wiring (`:expected-G`,
   = the `:delta-g` the act-gate consumed at decision time) vs the actually-
   ENACTED wiring (`:realized-G`, recomputed post-enactment: holes filled / boxes
   failed ⇒ different coverage). So γ's relative error is apples-to-apples, not a
   ΔG-vs-ΔF mismatch. The reader is `policy-precision/fold-realized-outcome`.

   STAGED (no-live-pilot-edit discipline; the 2026-06-26 incident): enactment is
   NOT wired yet (Joe's call on touching the recovered pilot). `*live-wire?*`
   defaults false ⇒ `with-realized-outcome` returns the judge-output UNCHANGED ⇒
   `trace-record` emits no `:realized-outcome` key ⇒ γ holds at the prior 1.0
   (reduction-safe). The PURE `realized-outcome-of` is always available for the
   end-to-end judge→trace→γ integration test claude-11 wires.

   Pure + no I/O + no loop + flag-gated: `fold.clj`'s expected-only invariant is
   untouched (this ns is the OBSERVE-side producer, not a fold-contract term)."
  (:require [futon2.aif.fold-eval :as fe]))

(def ^:dynamic *live-wire?*
  "Off until enactment (`apply-cascade!`) is live-wired into the pilot (Joe's
   call). Off ⇒ no `:realized-outcome` is produced (γ stays at the prior)."
  false)

(defn realized-outcome-of
  "PURE: the R16→R14 `:realized-outcome` record for an enacted decision.
     `decision`       — carries `:policy` and the expected leg: either an explicit
                        `:expected-G`, or `:fold` (the fold output, whose `:delta-g`
                        the gate consumed).
     `enacted-wiring` — the post-enactment wiring (`:boxes`/`:policy-holes`);
                        `:realized-G` = the SHARED coverage→rollout ΔG over it.
     `tick`           — the enactment tick (γ dedups on it).
   `:realized-G` is nil when nothing was enacted (no boxes) — γ then holds (it
   requires a number), the honest no-op."
  [{:keys [policy expected-G fold]} enacted-wiring tick]
  {:policy     policy
   :expected-G (if (number? expected-G) expected-G (:delta-g fold))
   :realized-G (fe/coverage-delta-g enacted-wiring)
   :tick       tick})

(defn with-realized-outcome
  "STAGED seam: thread the `:realized-outcome` onto a `judge`-style output so it
   flows through `trace-record` (claude-11's reader picks it up from
   `latest-trace-record` next tick). No-op unless `*live-wire?*` — enactment isn't
   wired (Joe's call). Returns `judge-output` UNCHANGED when staged-off, so the
   field stays ABSENT and γ holds at the prior."
  [judge-output decision enacted-wiring tick]
  (if *live-wire?*
    (assoc judge-output :realized-outcome (realized-outcome-of decision enacted-wiring tick))
    judge-output))
