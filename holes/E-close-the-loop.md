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
