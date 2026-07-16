# README-xeno-loop — the propagator/ant transfer, and the trap next to it

**Read the first section before touching any ant code.** (M-propagators L5 /
M-aif-ants-port, 2026-07-16.)

---

## 0. STOP: `cyber.clj` is NOT the apparatus. It is the post-mortem subject.

`src/ants/` contains **two unrelated generations of ants**, and the wrong one is
alphabetically earlier, has "cyber" in the name, and is what you find first.

| | |
|---|---|
| **USE THIS** | `src/ants/aif/` — the **modern AIF forager** (`core`, `policy`, `efe`, `forward`, `rollout`, `perceive`, `food_belief`, `experiment`). Built 2026-07-14/15 as M-aif-ants-port **Port 1**. |
| **DO NOT BUILD ON THIS** | `src/ants/cyber.clj` — the **old cyberants**. Kept as evidence of a failure, not as a component. |

**Why `cyber.clj` is a trap, concretely.** `config->aif-delta` (`cyber.clj:205-212`)
merges **only `:precision`** (`:tau`, `:Pi-o`) into the live ant. `:policy-priors`,
`:pattern-sense`, `:adapt-config` are stored under `:cyber-pattern → :config` via
`select-keys` and **never read** — only `:id` and `:ticks-active` are consumed anywhere.
So the old "domain transfer" experiment's `random-wiring` control permuted **inert
fields**: it was *operationally byte-identical* to the treatment. The resulting "null" was
a **tautology**, and it was read as refuting the hypothesis rather than the apparatus.

That is the mistake this whole line of work exists to not repeat. If you find yourself
wiring something into `:cyber-pattern`, stop.

---

## 1. What the xeno loop is

Joe's definition of a **xenotype**: *"a place where these exotypes are being evolved,
e.g. selection pressure coming from ants that plays back into our design of new types of
blending apparatus."*

Concretely, four parts — each one **gated before use**:

| part | what it is | status |
|---|---|---|
| **GENOME** | σ, a permutation over the C-vector's preference channels — the propagator, moved off the MetaCA rule byte and onto **preferences** | — |
| **ACTUATOR** | the ant's **C-vector** (`policy/default-c-vectors`) | **GATE-PASSED**, see §3 |
| **SELECTION** | ant foraging **yield**. The ants just like to eat — a *grounded* objective, unlike the tokamak's unanchored transport proxy (M-propagators §4b) | — |
| **MUTATION** | a single **transposition** of σ | **measured, not guessed** — see §4 |

### The propagator, on preferences

Same operator as the MetaCA, new substrate:

```
pick channel k;   C[σ(k)].mean := 1 − C[k].mean
```

Copy the **inverse** of the preference for channel `k` into the preference for `σ(k)`.
σ = identity is ordinary preference noise (a random walk, no coupling); a non-trivial σ
**couples** preferences. That is the conatus reading: the attractor is not represented
anywhere — it is what the operator's structure makes the ant tend toward.

---

## 2. THE CATEGORY ERROR — why the propagator must fire *during* the run

**This is the thing most likely to be silently re-broken.** Joe diagnosed it from the
symptom ("mutation without selection outperforms selection is a sign of a category error").

The earlier version did:

```clojure
;; WRONG — this is preprocessing wearing a dynamical operator's name
(let [c' (apply-propagator base-c sigma 8 rng)]   ; 8 applications, ONCE
  (with-redefs [policy/default-c-vectors c']      ; then FROZEN
    (run-the-ants)))                              ; ants run a CONSTANT
```

In the MetaCA the propagator fires **every generation, inside the dynamics** — that is the
whole difference between a propagator and a permutation. Frozen, the ants never experience
it; they run an arbitrarily scrambled constant.

**And our own JAX result proves freezing cannot work.** `propagator_fixedpoints_jax.py`
(futon5) computed the exact attractor of all 40,320 σ: **no σ has a single-byte
attractor** (0.0% at support ≤1). The fixed point is an **invariant set** (4–64 bytes) the
system keeps *cycling within*. So "apply 8 times and freeze" grabs **one arbitrary phase
of an orbit**. The old docstring said "the C that sigma's propagator settles on" — nothing
settles; the propagator never stops moving.

That is exactly why **drift beat evolved**: an arbitrary orbit phase makes the σ-fitness
landscape **noise**, so selection maximised noise on 8 boards while drift, refusing to
commit, did not chase it. *Drift > evolved was the signature of the category error*, not a
statistical fluke.

**Correct (current, `run-live`):** the propagator fires every tick against the **live** C,
so preferences continuously cycle within the invariant set σ selects. This is also the
first version where the ant and the MetaCA run **the same operator in the same role** —
the only version where "does it transfer?" is a meaningful question.

### σ's domain: the union, not the intersection

The three modes have **different** channel sets — `:outbound` and `:maintain` have 10,
`:homebound` has 9 (no `:food`: an ant carrying cargo home genuinely has no food
preference). Two wrong answers and the right one:

- ❌ σ over `:outbound` only, applied everywhere → mappings touching `:food` **silently
  no-op** on homebound. Ill-typed and quiet about it.
- ❌ σ over the **intersection** (9) → well-typed, but **deletes `:food`**, the single
  channel most relevant to foraging. *A fix that breaks the experiment is not a fix.*
- ✅ σ over the **union** (10); each mode applies it only where **both** `k` and `σ(k)`
  exist there. Partial **by design** — the per-mode difference is the operator being
  *semantic*, not a defect to erase.

---

## 3. The authority gate — run it before believing anything

`scripts/ant_authority_gate.clj` — **the thing `cyber.clj` never had.**

```sh
cd futon2 && clojure -M scripts/ant_authority_gate.clj [n-seeds] [ticks]   # e.g. 20 300
```

Proves the knob moves the ant *before* a propagator goes anywhere near it. Result at 20
paired seeds × 300 ticks (patchy, 30×30):

| arm | Δ yield | sign-p | reading |
|---|---|---|---|
| `identity` (**sham**) | **0.000** (20 ties) | — | harness sound |
| `flatten` | +11.9 | .289 | within noise |
| `shuffle` | +34.3 | **.0034** | moves the ant |
| `invert` | **−19.6 → yield 0.000 on every seed** | **.0078** | moves the ant |

**Inverting C abolishes foraging.** The C-vector is a live actuator.

**The sham is load-bearing.** `identity` returning *exactly* 0.000 is what proves a "no
effect" reading is the statistic's fault and not the ant's. The gate's four near-misses
are documented in its docstring — wrong noise term, ties counted as failures, paired-t
collapsing on a constant treatment, arbitrary thresholds — **each of which produced a
confident AUTHORITY FAIL**, i.e. the cyberant error again. Do not remove the sham.

---

## 4. Mutation = transposition, and why

Not a guess. Ollivier-Ricci curvature over the propagator space (M-propagators §2b.4):
**80% of the most negatively-curved edges connect σ differing by a single transposition**
(83% with near-duplicates excluded, so it is not a 1/d artifact). Negative curvature =
where the space **branches** = where structure is not yet decided. The geometry told us
where to mutate before there was anything to mutate.

---

## 5. The board screen — and why it is not cheating

`xeno_loop.clj` screens boards **before** splitting train/test.

Yield is wildly zero-inflated (authority gate baseline: mean 19.6, **sd 67.5** — most
boards yield nothing). Run 1's held-out seeds yielded ~0 for **every arm including
baseline**, so "no improvement" meant **"no measurement"**.

- ❌ Screening on **baseline yield > 0** is biased: it keeps boards where the *baseline*
  works, and would hide the effect if evolution's benefit is rescuing boards the baseline
  fails on.
- ✅ Screen on **board geometry**: `distance(home → nearest food) ≤ 8`. No simulation, no
  arm, so it cannot favour anyone. **Measured**: productive boards mean nearest 3.7, dead
  11.3; every board with no food within 10 of home yields *exactly* 0. `total-food` was
  **rejected** as a screen — productive 78.9 vs dead 81.2, no signal.

It removes boards that are **impossible**, not boards that are baseline-unfriendly: of 17
screened, 10 are baseline-productive and **7 are reachable-but-baseline-fails**. Those 7
are kept on purpose — they are the headroom.

---

## 6. Running it

```sh
cd futon2
clojure -M scripts/ant_authority_gate.clj 20 300      # gate first, always
clojure -M scripts/xeno_loop.clj [gens] [pop] [train-seeds] [ticks]   # e.g. 6 6 8 300
```

Arms, all run identically:

- `:evolved` — selection ON (the claim). Hill-climbs; returns the **best-ever** σ.
- `:drift` — **THE KEY NULL.** Same mutations, *random* survivor. Isolates whether
  SELECTION did anything vs mere churn.
- `:identity` — σ = identity forever (preference noise, no coupling).
- `:baseline` — no propagator at all (the shipped C).

**Preregistered verdict:** the loop works **iff** `:evolved` beats **both** `:drift` and
`:baseline` on **held-out** seeds by a sign test (p<.05). If it beats baseline but not
drift, **the loop is churn, not evolution** — report that, do not dress it up.

Watch `distinct-fitnesses k/pop` in the output: if every candidate ties, `max-key` and a
random pick **coincide** and `:drift` is not a control at all — it is `:evolved` wearing a
hat. That happened at smoke scale (60 ticks, 3 seeds).

---

## 7. Status — what is and is not established

**Established:**
- The C-vector is a **live actuator** (§3). Unlike `cyber.clj`'s wiring.
- The board screen restores measurement (held-out baseline 0.058 → 34.7).

**NOT established:** the xeno loop itself. With the *frozen* propagator, **drift (88.4)
beat evolved (54.4)** — which we now believe was the category error (§2), not a result.
The rewired version is under test. **Do not cite the loop as working.** It is deliberately
excluded from the paper draft (`futon5/holes/tech-notes/paper/`) for exactly this reason.

**Known limits:** 8 train boards, pop 6, 6 generations is a small search; yield magnitudes
are dominated by the few productive boards, so *direction* (sign test) is trustworthy and
*effect size* is not.

---

## 8. Files

| file | what |
|---|---|
| `scripts/ant_authority_gate.clj` | the gate. Run first. |
| `scripts/xeno_loop.clj` | the loop: screen → evolve → held-out verdict. |
| `src/ants/aif/**` | **the ants.** Modern AIF forager. |
| `src/ants/cyber.clj` | **NOT the ants.** Post-mortem subject; see §0. |
| `holes/M-aif-ants-port.md` | the reframe + the three ground-truth findings behind §0. |
| `../futon5/holes/missions/M-propagators.md` | the propagator, the geometry, the curvature. |
| `../futon5/scripts/propagator_fixedpoints_jax.py` | the exact-attractor result behind §2. |
