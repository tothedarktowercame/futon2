# README-gamma.md — the belly's decisiveness: γ, and how to watch it earn it

*Companion to `README-loss.md` (R19 predictive risk) and `README-clicks-and-ticks.md`
(the clock the loop runs on). Excursion: `~/code/futon2/holes/E-precision-over-policies.md`.
This note is the **observability** layer for **γ — precision over policies (R14,
Friston's γ)**: the one quantity governing the WM's **decisiveness** that is meant
to be **earned over runs** rather than hand-set.*

## What γ is

R6 selects with `P(a) ∝ exp(−G(a)/τ)`. Until R14, τ was derived **only** from the
EFE *spread* of the current candidate set (`policy/adaptive-temperature`): a
within-the-moment heuristic that never learned from how past commitments turned
out. γ is the missing learned term — the agent's **confidence in its own
decision-making**, the inverse selection temperature:

```
τ_eff = adaptive-temperature(G-spread) / γ      (policy/effective-temperature)
```

- **high γ** → lower τ_eff → **sharper** softmax → commit harder;
- **low γ** → higher τ_eff → **flatter** softmax → hedge / abstain-lean;
- **γ = 1.0** → reduces *exactly* to today's spread-only temperature.

γ is the policy-scale sibling of R7's per-channel precision Π
(`futon2.aif.precision`) — same shape (a bounded quantity learned from a rolling
error history, persisted in the trace), one level up: over *policies*, not
channels. State lives in the trace as `:policy-precision`, read back each tick
exactly like `:precision-state`.

## What γ learns from (the signal)

The **policy prediction-error**: how well a chosen, *enacted* policy's **expected**
free energy matched its **realized** outcome. The realized signal is R16's
close-the-loop `:realized-outcome` trace contract (interface paired with
claude-10, `E-close-the-loop`):

```
:realized-outcome {:policy <id> :expected-G <g> :realized-G <g'> :tick <enactment tick>}
```

Both legs are the **same EFE quantity** — the fold's coverage→rollout ΔG, evaluated
over the *predicted* wiring (`:expected-G`) vs the actually-*enacted* wiring
(`:realized-G`). γ folds them via a **symmetric relative error** (scale-free, so no
G-unit constant is needed, and a ΔG-vs-ΔF mismatch is impossible):

```
ρ = |expected-G − realized-G| / (|expected-G| + |realized-G| + ε) ∈ [0,1]
γ = clamp( 2^(2·(ρ* − ρ̄)), floor, cap )       ; ρ* = 0.5, floor 0.5, cap 2.0
```

So ρ̄ = 0 (plans realize as predicted) → γ → cap (commit); ρ̄ = 1 (total miss) →
γ → floor (hedge); ρ̄ = ρ* → γ = 1.

**Why relative error, not R7's literal 1/variance** (a deliberate divergence from
the charter's R7-mirror wording, ratified E-precision-over-policies §4): variance
rewards a *consistent bias* (a policy always wrong by the same amount → low
variance → spuriously high confidence). Relative error correctly reads a
consistent miss as low confidence.

## The burn-in (what we hope to watch improve)

At cold start γ sits at the prior **1.0** and *stays there* — by two guards:

1. **No signal yet.** Enactment is not live-wired, so `:realized-outcome` is
   **absent** today. No outcome ⇒ no sample ⇒ γ holds at 1.0. The WM's
   decisiveness is **byte-identical** to the pre-R14 spread-only τ path.
2. **Burn-in gate.** Even once outcomes flow, γ stays exactly 1.0 until
   `min-history` (= 5) realized samples accrue — *default to the prior; sharpen on
   burn-in* (the R19-KL pattern). Only then does γ become outcome-derived.

Once R16 live-wires enactment and outcomes accrue, the curve to watch is **γ
moving off 1.0**: a run of well-realized policies pushes γ up (toward 2.0,
sharper commitment); a run of misses pushes it down (toward 0.5, more
abstain/explore). Bounds are tight on purpose — γ can at most halve or double the
spread-τ, a bounded and reversible blast radius.

## What to watch, and where

| Signal | Where | Healthy trajectory |
|---|---|---|
| γ itself | WM trace `:policy-precision :policy-precision` | holds 1.0 pre-signal; moves into (0.5, 2.0) as outcomes accrue |
| Mean realized error ρ̄ | trace `:policy-precision :mean-error` | nil during burn-in; drops over runs as plans pay off |
| Samples seen | trace `:policy-precision :samples` | climbs only as enacted outcomes arrive (not per tick) |
| Effective vs spread τ | decision `:tau` vs `:tau-spread` | equal at γ=1; `:tau` < `:tau-spread` when γ>1 (sharper) |
| Commitment | decision `:softmax-weights` peak mass | widens as γ rises |
| Dedup tick | `:policy-precision :last-outcome-tick` | advances once per *enactment*, never per tick |

## Honest caveats (read before reading the numbers)

- **γ = 1.0 everywhere today** — `:realized-outcome` is absent (enactment not yet
  live-wired). The *machinery + wiring + the trace seam* are live and tested; the
  *improvement* requires R16 closing the loop and many enacted outcomes. No effect
  on live selection now, by design (reduction-safe).
- **γ is a single scalar** — one decisiveness for the whole WM. Per-context /
  per-action-class γ is a deliberate later refinement (E-precision-over-policies
  §4, deferred), as is the hierarchical case where an upper level sets γ for a
  lower (R15).
- **The signal source is injectable.** γ's core (`policy-precision`) is
  signal-agnostic — it consumes a relative error; `fold-realized-outcome` adapts
  the R16 contract to it. If the realized term changes, only that one seam moves.

## See also

- `src/futon2/aif/policy_precision.clj` — the γ learner (`update-policy-precision`,
  `observe-outcome`, `fold-realized-outcome`, `gamma-for`).
- `src/futon2/aif/policy.clj` — `effective-temperature` (τ_eff = τ_spread / γ) and
  `select-action`'s `:policy-precision` opt.
- `src/futon2/aif/trace.clj` — `:policy-precision` persistence + the
  `:realized-outcome` seam.
- `scripts/futon2/report/war_machine.clj` — `judge` reads the prior γ-state, folds
  the realized outcome, feeds γ into selection.
- `src/futon2/aif/fold.clj` / `holes/E-close-the-loop.md` — the R16 producer side
  of `:realized-outcome`. `README-loss.md` — the sibling R19 burn-in note.
