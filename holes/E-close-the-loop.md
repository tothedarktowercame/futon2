# Excursion: close the loop — nail the fold INTERFACE first (E-close-the-loop)

**Date:** 2026-06-26 · **Status:** DERIVE (the interface, as a sorry) — charter for the driver (claude-10).
**Authored by:** claude-10 (with Joe, 2026-06-26).
**Parent / relates:**
[[M-aif-wiring]] (R16 — close the loop, R10→R2; this excursion delivers the fold *interface* it needs; the R16 contract text stays with M-aif-wiring) ·
`cascade-sorry-wiring-symbol-grounding.html` (futon3c — the (have→want)→cascade→sorry→wiring lifecycle this instantiates) ·
[[E-fold-engine]] (futon3a — the deterministic `fold_engine.clj`, the live `:apply-cascade` executor; impl #1) ·
[[E-llm-fold]] (futon3a — the LLM-turn fold; impl #2) ·
[[E-C-vector-live]] · [[E-precision-over-policies]] (the R19 / R14 siblings — same "wire the canonical AIF quantity" shape).
**Repos:** futon3c (`peripheral/war_machine_pilot_backend.clj` — the act-gate that consumes ΔG; `apply-cascade!`) · futon2 (`aif/efe.clj` G, `aif/rollout.clj`) · futon3a (the fold impls + arrow/meme stores — *solution-side*).

---

## HEAD

R16 is the **one arc from R10 (act) back to R2 (observe)** — the loop closure. The act-gate
(`war_machine_pilot_backend.clj:239`) is `:pass` iff `ΔF>0 ∧ ΔG<0`, but emits
**`:abstain-missing-leg` whenever ΔG is nil** — and ΔG is always nil because no served
mission has a **construction to roll out**. The thing that produces a construction from a
mission's gap is the **fold** (cascade → wiring), and — per E-llm-fold — the fold function
`f(IF, HOWEVER, circumstance) → construction` **can be realized classically, by an LLM turn,
or in embedding space**. There is **no single right realization.**

**So the first move is not to pick a fold — it is to nail the INTERFACE the folds work to**,
so any realization plugs into the same socket and they can be compared. And — Joe, 2026-06-26
— *this very task is an instance of the lifecycle the symbol-grounding HTML names*: we **have**
an abstaining gate, we **want** a closed loop; the cascade is the three fold docs + the gate
code; and the right next artifact is the **sorry** — the interface as a *typed hole, ports
known, construction absent*. We DERIVE that sorry here.

### The question

**What is the fold/closure INTERFACE — the ports and types a construction must satisfy to give
the act-gate its ΔG — specified so that classical, LLM, and embedding folds are all pluggable
and comparable, with no data source baked in?**

### The load-bearing distinction (Joe, 2026-06-26)

**Data is solution-side, not interface-side.** If code-is-data / data-is-code, then the arrow
store, the embedding corpus, the LLM weights, `pattern_phylogeny`, the meme arrows — *which
data a fold draws on is part of its CONSTRUCTION*, not part of the contract it's built to. The
interface names **only ports and types**; it must mention **no** data source. (My earlier
"which arrow source feeds the rollout?" fork was a category error — that's a per-impl choice
downstream of this interface.)

### Discipline this inherits

- **No background loops / no live-pilot-request-path edits** until proven ([[feedback_no_perpetual_loop_shared_jvm]]; the 2026-06-26 incident). Impl work is sim-only first.
- **The interface carries no data source** (above). A leak of any store/corpus into the contract is the failure mode to guard against.
- Evidence-first; gates per AGENTS.md; never restart the JVM.

---

## 1. The sorry — the interface as a typed hole

Following `cascade-sorry-wiring-symbol-grounding.html`'s lifecycle (`:correlated → :open → :constructed`):

- **meme (have→want):** have = the act-gate abstaining (ΔG nil, no construction); want = a closed loop (the gate gets a real ΔG from a fold).
- **cascade (correlation):** the three fold docs (deterministic / LLM / embedding) + the gate + rollout/efe code — the neighbourhood that expresses the gap.
- **sorry (the upgraded arrow — THIS excursion's artifact):** the **fold interface**, ports known, construction absent:

  ```
  fold : (cascade, circumstance) → {:wiring       <a construction — boxes/wires/terminals; the thing apply-cascade! runs>
                                    :delta-g      <number — the rollout G(π) over that construction; the act-gate's missing leg>
                                    :policy-holes [<what the fold left FREE / could not derive — surfaced, never silently dropped>]}
  ```
  - **`cascade`** = the pattern halo condensed around the mission's (have→want) meme. *(How it's obtained — query, embedding, store — is solution-side.)*
  - **`circumstance`** = the mission/sorry context the construction must fit. *(Its source is solution-side.)*
  - **`:delta-g`** = the socket the act-gate already consumes (`{:delta-F … :delta-G …}` → `:pass`/`:fail`/`:abstain`). Negative = the construction descends EFE (discharges the sorry).
  - **`:policy-holes`** = the construction's free content (the fold's coverage discipline, per E-llm-fold).
  - **Construction absent:** *how* `:wiring`/`:delta-g` are computed is the realizer — left to the impls. This is the Kolmogorov problem the sorry names.

- **wiring (`:constructed`, later):** the first fold impl satisfying this sorry, witnessed by the gate flipping `:abstain` → `:pass`/`:fail` on a real served mission (sim-only).

## 2. The impl roster (each a construction to the SAME sorry; each picks its OWN data)

| impl | realizer | data it draws on (solution-side) | status |
|---|---|---|---|
| **#1 classical** | `fold_engine.clj` (rule-table) | the ~10 pre-compiled cascade patterns | **live** as `:apply-cascade`; deterministic ⇒ sim-verifiable |
| **#2 LLM-turn** | an inhabiting agent evaluates the fold | the NL cascade + circumstance (no store needed) | proven on music (E-llm-fold) |
| **#3 embedding** | fold run in embedding space (DarkTower-flow) | the joint pattern/mission/wiring embedding | research frontier |

All three target `{:wiring :delta-g :policy-holes}`. Comparing them = running them behind the
one interface on the same served missions and scoring (does the gate pass? is the construction
sound? how honest are the holes?). **The interface is what makes the comparison possible.**

## 3. ARGUE — decisions to ratify

- **Interface first, data-agnostic** (Joe-ratified): the contract names ports/types only; no store/corpus/weights. ✓
- **First realization = the classical `fold_engine`** — deterministic ⇒ sim-verifiable (post-incident safety), already live; the LLM-turn and embedding folds plug into the same socket later for comparison. *(Open: Joe may prefer to start impl #1 with the LLM-turn fold.)*
- **`:delta-g` derivation** — is it the rollout G(π) over the folded wiring (reuse `aif/rollout`), or a coherence/wholeness score of the construction? (The contract fixes the *port*; the *derivation* is impl-side — but a default for impl #1 must be chosen.)

## 4. Exit conditions (provisional)

1. The **fold interface** is minted as a typed artifact (a contract spec / sorry), **data-agnostic** — no store/corpus named in the ports.
2. **≥1 impl** satisfies it and flips the act-gate `:abstain-missing-leg` → `:pass`/`:fail` on a real served mission — **sim-only** (no arming, no writes, no background process).
3. The interface is shown **pluggable**: a second impl (different realization AND different data) targets the same ports without changing the gate.
4. No data source has leaked into the contract (the load-bearing distinction holds under review).

## 5. Scope-out (named)

Arming / acting on a `:pass` (R16-ARM / WM-I4 — governance, not inference); the full multi-impl
*comparison study* (this excursion delivers the interface + impl #1; the bake-off is its own
follow-on); the R16 contract text + A/B/C/D/E diff (M-aif-wiring). This excursion is **only**
the fold interface (the sorry) + the first construction that closes the loop through it.

## 6b. INSTANTIATE — exit-1 + exit-2 done (impl #1; 2026-06-26)

- **exit-1 — interface minted.** `futon2.aif.fold` — `fold : (cascade, circumstance) → {:wiring :delta-g :policy-holes}` + `valid-fold-output?` / `act-gate-leg` (nil ⇒ abstain) / `closes?` (ΔG<0). **Data-agnostic** (no store/corpus/weights named). (`9f0a5bf`)
- **exit-2 — impl #1 closes the gate (sim-only).** `meme.fold` (the classical rule-table fold, promoted lab→lib, `82ff44a`) + `futon2.aif.fold-classical` (`ae016ec`): the fold's **coverage** (folded boxes vs policy-holes) → a discharge move → **`rollout/project-policy` G(π)** = ΔG. Verified in a clean test JVM (no live-pilot touch, no writes, no loop): foldable cascade ⇒ ΔG=−1.0 ⇒ gate **`:pass`**; foreign cascade ⇒ ΔG nil ⇒ **`:abstain-missing-leg`**; foreign patterns surfaced as policy-holes. So the loop demonstrably closes through the interface.

### The two comparison axes (Joe, 2026-06-26 — write both up)

A close-the-loop solution varies independently along **two** axes; the interface is the fixed socket both plug into:

| axis | options (all to the same `fold`/`:delta-g` ports) |
|---|---|
| **build** (how `:wiring` is produced) | **classical** rule-table (`meme.fold`, impl #1 ✅) · **LLM-turn** (inhabiting agent) · **embedding-flow** (DarkTower) |
| **evaluation** (how `:delta-g` is produced) | **coverage→rollout G(π)** (impl #1 ✅) · **multi-move rollout** over the box sequence · **coherence/wholeness** (Salingaros) · the belly's **predictive-goal-outcome-risk** |

Because **data is solution-side** (a build × evaluation cell picks its own data), the cells are a comparison grid: any build can be paired with any evaluation behind the one interface, and scored on the same served missions. impl #1 is the (classical × coverage-rollout) cell; the bake-off across the grid is the named follow-on.

## 6c. INSTANTIATE — impl #2 (LLM-turn fold) + exit-3 (2026-06-26)

Joe, 2026-06-26: impl #1 is a *toy* — its rule-table only "constructs" the ~10
pre-compiled patterns, so a real minilm cascade rarely hits it (it abstains).
Feeding it exactly those patterns is itself an impl-#3 (embedding) capability. So
**don't polish impl #1** — build impl #2 and "see if we get something that makes
a bit more sense in context."

- **The evaluation axis is now SHARED** — `futon2.aif.fold-eval` (coverage→rollout ΔG)
  factored out of impl #1; both impls use it, so the comparison isolates the BUILD.
  impl #1 refactored to it (behaviour identical; regression green). (`1e8f465`)
- **impl #2 — `futon2.aif.fold-llm`** (the LLM-turn build): an inhabiting agent
  reads the cascade's NL prose (IF/HOWEVER/THEN) + circumstance and constructs a
  *fitted* wiring, surfacing honest policy-holes. `turn-fn`/`prose-fn` **injected**
  ⇒ incident-safe (NO LLM in the serving JVM; nil turn-fn ⇒ abstain, never
  blocks/spawns) and pure-testable.
- **Demonstrated on the REAL `M-value-creation-loop` cascade** (`scripts/.../fold_llm_demo.clj`),
  the inhabiting agent (claude-10) as the engine per E-llm-fold:
  - **THIN** (budget 12, cascade `[f6/self-play-loop]`, ΔF=−0.214): the construction
    leg **closes** (5 boxes / 3 holes ⇒ ΔG=−0.625) but the gate **`:fail`s on ΔF**
    — honest: ΔF below the Bayesian-Occam knee is a *cascade-richness* problem, not
    a fold problem.
  - **RICH** (fuller psi, 4-pattern cascade `[self-play-loop · pattern-as-strategy ·
    q-turnstile-a · trail-enables-return]`, ΔF=+0.707): both legs close (7 boxes / 4
    holes ⇒ ΔG=−0.636) ⇒ the gate **`:PASS`es** — the loop fully closes through impl #2.
  - **impl #1 abstains on both** (`:abstain` — the rule-table can't fold these
    contentful library patterns). This *is* the reach difference.
- **exit-2 ✓** (abstain → :pass/:fail on a real served mission) and **exit-3 ✓**
  (a second impl, different build AND different data — the NL library vs the
  rule-table — targets the same ports without changing the gate). exit-1 (interface)
  and exit-4 (no data leak into the contract) hold.

### Honest finding (carry forward)
The fold supplies the ΔG leg the gate was abstaining for; whether the gate then
:passes depends on **ΔF (cascade richness)** — a different knob (the cascade
constructor's psi/budget), upstream of the fold. THIN-cascade missions need a
richer cascade to pass, independent of which fold builds the construction.

### Remaining
- Wire a fold into the **live** act-gate path (sim-verified off-path today) —
  carefully, given the gate lives in the just-recovered pilot backend; reuse the
  existing scheduler tick, no new loop. (Joe's call on touching the live pilot.)
- impl #2's `turn-fn` is hand-fed today (the agent turn encoded as data). A real
  out-of-process producer — a **bell to an agent** or a **recorded fold-turn read
  from escrow** — is the productionization (still no LLM in the serving JVM).
- impl #3 (embedding) build; alt evaluations (b/c/d in `fold-eval`) — the grid bake-off.
