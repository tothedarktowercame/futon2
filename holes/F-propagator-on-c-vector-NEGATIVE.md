# F-propagator-on-c-vector — FIRST NEGATIVE: the propagator does not transfer to the ant's C-vector

**Status:** NEGATIVE, measured, 2026-07-16. Owner: claude-3.
**Update 2026-07-16 (later):** the falsifying sweep below was RUN, widened from
cadence-only to cadence × magnitude — **no cell beats baseline; the refutation is
strengthened** along rate and magnitude, and the dose–response confirms amnesia as the
mechanism. See "The sweep was run" below. Remaining outs: locus (per-ant C) and
precision-not-preference.
**Context:** M-propagators L5 (transfer) × M-aif-ants-port (Port 1).
**Apparatus:** `futon2/README-xeno-loop.md`, `scripts/{ant_authority_gate,xeno_loop,xeno_gentle_sweep,xeno_gentle_confirm}.clj`.

---

## One line

Running the propagator on the ant's C-vector **degrades foraging at every cadence tried**.
Every arm with a live propagator loses to no propagator at all. The most likely
explanation is not a tuning failure: **the rule byte and the C-vector play different
roles**, and propagating a goal is amnesia, not evolution.

---

## What was claimed, and why it looked good

The transfer analogy was structural and tight:

| MetaCA | AIF ant |
|---|---|
| 8 neighbourhoods → binary response | 14 obs channels → preference (per mode) |
| **the rule byte** | **the C-vector** |
| propagator couples responses; its fixed point selects the rule | propagator would couple preferences; its fixed point selects the C |

And it was *not* the old cyberant mistake: the C-vector is a **gate-passed live actuator**
(`ant_authority_gate.clj`, 20 paired seeds × 300 ticks) — inverting C drives yield to
**0.000 on every seed** (sign-p .0078) while the sham moves *exactly* nothing. The knob is
real. Joe's framing also fit: the C-vector is Friston's preferred-future prior, and a
propagator's fixed point on it is Spinoza's conatus — a tendency with no representation.

---

## The result

Rewired so the propagator fires **during** the ant's life (every tick, live C), σ well-typed
over the union of per-mode channels, boards screened arm-independently, 6 gens × pop 6 ×
8 train boards × 300 ticks, scored on **16 held-out boards never selected against**.

**Train** (selection is working — hill-climbing is monotone):

| gen | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| evolved | 18.73 | 19.43 | 19.43 | 23.19 | 28.26 | **31.50** |
| drift | 18.73 | 18.90 | 18.90 | 15.66 | 18.81 | 28.35 |

**Held-out — the baseline beats everything:**

| arm | yield | |
|---|---:|---|
| **baseline** | **34.7** | shipped C, **no propagator** |
| drift | 20.2 | live propagator, no selection |
| evolved | 11.3 | live propagator, selection ON |
| identity | 8.6 | live propagator, no coupling |

`evolved vs baseline` Δ **−23.4**, wins 2/9, p .18. `evolved vs drift` Δ −8.9, p .73.

**Every live-propagator arm is worse than no propagator.** The ordering is
`baseline > drift > evolved > identity`.

---

## Reading: a third category error, not a tuning failure

Two prior category errors were found and fixed, and each changed the picture:

1. **Frozen propagator** (run 2). Applied 8× then frozen = *preprocessing wearing a
   dynamical operator's name*. Our own JAX result proves freezing cannot work: **no σ has a
   single-byte attractor** (0.0% at support ≤1) — the fixed point is an **invariant set**
   the system keeps cycling within, so "apply 8× and freeze" grabs *one arbitrary phase of
   an orbit*. That is why **drift beat evolved**: an arbitrary phase makes the σ-landscape
   noise, so selection maximised noise while drift declined to chase it. *Joe diagnosed this
   from the symptom alone.*
2. **σ's domain.** Built from `:outbound` (10 channels) but applied to modes with 9 →
   silent no-ops. The obvious repair (intersection) **deletes `:food`**, the channel
   foraging is about. Fixed via the union with per-mode partial application.

**The third is the one this finding is about, and it is structural:**

> The propagator sets `C[k] := 1 − C[k]`. In the MetaCA the rule byte is a **mutable
> substrate** — the thing that *varies*, with the phenotype adapting to it. In the ant, C
> is the **goal**: a normative reference that must *persist* for behaviour to be coherent.
> Flipping an agent's preferences every tick is not "coupling preferences" — it is
> **amnesia**. The ant cannot forage because it no longer wants anything long enough to act.

| MetaCA rule byte | ant C-vector |
|---|---|
| substrate that **varies** | reference that must **persist** |
| propagating it = **evolution** | propagating it = **destroying agency** |

**Two pieces of internal evidence for this reading:**
- **`identity` is the WORST arm (8.6).** Identity is pure preference-flipping with *no
  coupling* — the maximal-amnesia condition. If the problem were "the wrong σ", identity
  should be middling; it is the floor.
- **drift beats evolved (20.2 v 11.3).** Selection is optimising *how fast to scramble the
  goals*, which is not a thing worth optimising — so committing to a σ is worse than not.

---

## What would falsify this reading

**A cadence sweep.** `EVERY` = ticks between applications; this run used 1 (≈300
applications). If *some* cadence beats baseline, the transfer is **untuned**. If none does
— and note the limit `EVERY → ∞` is the frozen version, already shown to be noise — the
transfer is **refuted** rather than untuned, and there may be no good value between "too
fast to want anything" and "an arbitrary frozen constant".

That sweep is the single cheapest next experiment and it is decisive either way.

## The sweep was run (2026-07-16, later) — refutation STRENGTHENED

`scripts/xeno_gentle_sweep.clj`, widened from cadence-only to **cadence × magnitude**:
the amnesia is carried by the operator being the *largest* move a continuous C-mean
admits (on the MetaCA's binary byte a flip is the *smallest*), so the sweep generalises to
a rate-limited relaxation `C[σ(k)] ← (1−ε)·C[σ(k)] + ε·(1−C[k])`; ε=1 reproduces the
original operator **bit-exactly** (per-seed replication of this run's evolved/identity
arms verified in-script, along with the baseline audit and an ε=0 sham that ties baseline
per-seed). Grid: (EVERY, ε) ∈ {1}×{.05,.15,.4,1} ∪ {10}×{.4,1} ∪ {50}×{1}; σ ∈
{evolved-from-this-run, seeded-random, identity}; same 16 held-out boards; preregistered.

- **No cell beats baseline** (best p among positive-Δ cells: .625). One cell is
  significantly *worse* (evolved, EVERY 10, ε 1: wins 0/6, p .031).
- **The dose–response confirms amnesia as the mechanism**: within evolved σ, Δ vs
  baseline shrinks monotonically as the perturbation rate drops — −23.4 (every tick) →
  −17.6 (EVERY 10) → −2.8 (EVERY 50). Less propagation = less damage.
- **But the recovery only approaches baseline from below** — even ε=.05 nudges compound
  (each channel ~30 hits/300 ticks; the C still leaves the shipped point, just slower).
  Coupling C toward inverse-targets is unhelpful at *every* tested rate, not merely the
  amnesic one.
- Two cells (random σ, slow cadence) had **positive means** (+11.6, +28.6) with flat sign
  tests. Per preregistration they were re-tested on a **fresh screened pool**
  (`scripts/xeno_gentle_confirm.clj`, seeds 401–429): both go **negative with 0 wins**
  (0/4 p .125; 0/5 p .0625). Board-luck, as the zero-inflation caveat predicts of means.

So the "amnesia" assumption was real and is now *measured* (the dose–response), but
removing it by tuning rate/magnitude only recovers *toward* no-propagator. The transfer
in the global-C-over-time formulation is refuted, not untuned. What survives is the
**locus** reformulation below — per-cell-across-space is the CA fact the port dropped —
and **precision-not-preference**. The locus repair is scoped and small: `policy.clj:619`
reads the global `default-c-vectors`; `core.clj:194` already threads `:ant` into
`choose-action`, so `(or (get-in opts [:ant :c-vectors mode]) …)` is a backward-compatible
3-line change, after which σ can couple preferences **across ants** (`C_j[σ(k)] ← 1 −
C_i[k]`), leaving each ant's own goal persistent while the colony's preference field
cycles.

---

## Scope, honestly

- **Established:** the C-vector is a live actuator; the board screen restores measurement
  (held-out baseline .058 → 34.7); with the propagator live, **no arm beats baseline**.
- **NOT established:** that the transfer is impossible. One cadence was tried.
- **Limits:** 8 train boards, pop 6, 6 generations is a small search; yield magnitude is
  dominated by the few productive boards, so **direction** (sign test) is trustworthy and
  **effect size** is not. `p` values here (.18, .73) are not significant — the honest
  statement is "no arm beats baseline", not "the propagator significantly harms".
- **This stays out of the paper** (`futon5/holes/tech-notes/paper/`), which deliberately
  excludes the transfer thesis for exactly this reason.

## The question for a reviewer

Is there a formulation in which a propagator acts on an AIF agent **without** dissolving
the stability its agency requires? Candidates not yet tried:

- **Propagate the precision, not the preference** (`:tau`, `:Pi-o`) — that is *how strongly*
  the ant holds its goals, not *what they are*. Coupling confidence may be coherent where
  coupling desire is not. NB this is the one channel `cyber.clj` actually read. **Still open.**
- **Propagate a slower layer** — ~~cadence/magnitude on the C means~~ **TRIED (sweep above):
  slower/gentler only recovers toward baseline, never above it.** The mode-conditioning or
  `actions-by-mode` ranking variant is untried.
- **Propagate over the colony, not the ant** — σ couples preferences *across ants*
  (a population, like the CA's cells), leaving each ant's own C stable. The MetaCA's rule
  byte varies *per cell across space*; our port made it vary *per ant across time*. That
  may be the actual mis-mapping. **Still open, now scoped** (3-line `policy.clj` change +
  a colony-stepping loop; see the sweep section). **This is the strongest remaining
  candidate** — it is the one reformulation that keeps per-agent goal persistence *and*
  live coupling, which the sweep showed are jointly required.
- **Accept the negative.** A rule byte is not a goal; that is a finding about what
  propagators are, and it is worth stating. The sweep upgrades this from a reading to a
  measurement for the global-C formulation.
