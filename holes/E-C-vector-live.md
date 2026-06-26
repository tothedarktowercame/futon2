# Excursion: a LIVE C-vector — the WM's preferences, kept current (E-C-vector-live)

**Date:** 2026-06-25 · **Status:** IDENTIFY — charter for handoff (owned end-to-end by one agent; Joe assigns).
**Authored by:** claude-2 (scoping only — not the driver).
**Parent / relates:**
[[M-goals-and-holes]] (the backward/goal dual — this excursion *delivers* the live C that its R19 / C-vector needs; it does NOT own the R19 contract text) ·
[[M-operational-vocabulary]] (the forward dual — methods/policy; its box run produced the one-time mined C-entries this excursion's snapshot step reads) ·
[[M-aif-wiring]] (R5 risk · R14 γ · R15 hierarchy · the **belly** / venter this fills) ·
[[M-populate-substrate-2]] (the now-LIVE bitemporal substrate the goals/holes live in — and the source of the **freshness-guard discipline** below) ·
[[E-mine-mission-transitions]].
**Repos:** futon2 (`aif/preferences.clj` — the static C to make live; `aif/efe.clj` — `compute-efe`/`:G-risk` the consumer; `aif-wiring-explainer.html` — the belly) · futon1a (`:7071` substrate-2 — the `:sorry` corpus + goal/hole entities) · futon0 ([[M-capability-star-map]] — the goal hierarchy) · futon7 ([[C-pudding-prover]] — the discharge standard).
**Lit anchors (the C-vector):** Da Costa, Parr, Sajid, Veselic, Neacsu & Friston, *Active inference on discrete state-spaces: a synthesis* (J. Math. Psych. 2020) — A/B/**C**/D/E + the risk/ambiguity split; Friston et al., *Active inference: a process theory* (2017); Parr, Pezzulo & Friston, *Active Inference* (MIT 2022); Smith, Friston & Whyte, *A step-by-step tutorial on active inference* (2022); *Prior preferences in active inference agents: soft, hard, and goal shaping* (arXiv 2512.03293) — **how** C is set.

---

## HEAD

The War Machine's preference component **C** — Friston's prior preferences over outcomes, the "belly" feeding EFE's risk — exists today only as a **static, hand-set `def`** (`futon2.aif.preferences/preferences`, a map of per-channel preference ranges that `efe/compute-efe` reads as `:G-risk`). M-goals-and-holes diagnosed this: C is a *silent miss / mislabel* (a canonical generative-model matrix present-but-frozen as hyperparameters). The forward+backward mining runs on Linode produced C-*entries*, but a one-time assembly of them is a **snapshot** — and Joe's requirement is explicit: **the C-vector must be LIVE.**

Goals and holes change continuously — sorries open and close, missions complete, the capability-star-map grows, `hole/latent` negative space gets named. A snapshot C means the WM computes risk against **stale setpoints** — it builds toward what we valued *then*, not now. With C ignored or stale, `G(π)` collapses toward pure information-gain: the agent explores but **builds toward nothing** (the "hand with no belly").

### The question

**What makes the C-vector live** — continuously derived from the current goal/hole corpus and kept fresh — and wired into `efe`'s risk so the WM's preferences track present goals, not a frozen mining snapshot?

### The hard-won discipline this excursion inherits (read first)

This session, substrate-2's code/history layer **silently froze for 5 weeks** because a live model had **no freshness alarm** — nobody noticed the data had stopped tracking reality (M-populate-substrate-2 D0/D7a). **The C-vector is the same shape of object: a derived view that must track a moving source.** So: **build the live C WITH a freshness guard from day one** — C is stale the moment the goal/hole corpus changes after C was last derived; an alarm (the D7a `invariant-queue-freshness`/`futon3c.watcher.freshness` pattern: derived-stale-vs-source) must say so loudly. Do not ship a live-C that can silently freeze. This is non-negotiable and is an exit condition.

---

## 1. IDENTIFY — the gap

- **C is static + mislabeled.** `aif/preferences.clj` is a hand-authored `def`; `efe.clj:296` reads `(get pref/preferences ch)` per channel for `:G-risk` (gap-from-preference on the predicted mean). Nothing derives it from goals; nothing updates it.
- **The mining gave a snapshot, not a setpoint.** M-operational-vocabulary's run characterized C-entries; useful for shape, but frozen.
- **Goals/holes are the real C.** M-goals-and-holes' `hole/{stated,incompleteness,latent,mess}` family ARE the preferred-outcome entries (and `hole/latent` = C's negative space). They live in substrate-2 (now live + bitemporal) + the star-map + the sorry corpus (293 entities; 110 real per claude-1's audit).
- **Cost of staleness is precise:** `risk = KL(predicted-outcomes ‖ C)`; a stale C measures distance to the wrong target. The WM accrues capability without building toward currently-valued things.

## 2. MAP — survey (facts to gather first; don't design yet)

- Read `aif/preferences.clj` (current C shape: which channels, what ranges) and `aif/efe.clj` `compute-efe` / the risk term (exactly how C enters; per-channel range → `max(0, gap)`).
- Read M-goals-and-holes (the hole family + completion criteria) and its `goals-holes-readiness.html`.
- Inventory the goal/hole sources: `:sorry` entities (`:7071 entities/latest?type=sorry`; use the new **`/api/alpha/census`** count endpoint for populations, not a timeout-prone scan), the star-map graph, devmaps, Mission-Control coverage/tensions, the mined C-entries from the box run.
- How does the WM observe? The 13–14 channel observation schema (`aif/observation.clj`) — C is a preference over *these* outcome modalities, so the goal/hole→C mapping must land on these channels.

## 3. The snapshot-first scaffold (Joe's "get a sense")

Step one is legitimately a **one-time belly assembler**: assemble C once from the mined C-entries + current goal/hole corpus, print the per-channel preference it implies. This is the IDENTIFY/MAP scaffold — it shows the *shape* of a derived C and validates the goal/hole→channel mapping **before** building the live loop. Flag it loudly as a throwaway snapshot, not the deliverable.

## 4. DERIVE — the open design (the driver owns this; framed, not decided)

1. **C representation** — keep the per-channel preference-range shape `efe` already consumes (least wiring), or a richer log-preference distribution? (Lit: C is log-preferences over outcomes; `efe` currently uses ranges.)
2. **goal/hole → preference mapping** — how a `hole/stated` (a sorry / star-map goal) becomes a preferred-outcome entry on the observation channels. `hole/latent` (negative space) = the interesting case.
3. **Live-update trigger** — re-derive C when the goal/hole corpus changes. Reuse the substrate-2 watcher's drainer/probe pattern (debounced, off-cycle) rather than recomputing per WM tick.
4. **Freshness guard** (mandatory, §HEAD) — a `futon3c.watcher.freshness`-style check: C stale iff the goal/hole corpus changed after C's last derivation; loud on transition.
5. **`efe` wiring** — make `compute-efe` read the live-derived C instead of the static `def` (ideally a 1-line indirection: `pref/current-C` fn over the live structure, default-falling-back to the static map so the change is regression-safe).

## 5. ARGUE — decisions to ratify

- **Live, with snapshot as scaffold** (not snapshot-as-deliverable) — Joe's call, ratified.
- **Derive-and-cache, not recompute-per-tick** — the WM tick reads a maintained C; derivation is off-cycle (watcher/probe), so the hot path stays cheap (mirrors why commit-ingest is decoupled — D0.2).
- **Augment, don't rip out** — keep the static `preferences` as the fallback/floor so a derivation failure degrades to the current behavior, never to an empty belly (an empty C ⇒ G = pure info-gain ⇒ builds toward nothing).
- **R19 stays with M-goals-and-holes** — this excursion delivers the live-C *mechanism*; the contract criterion R19 (and the A/B/C/D/E systematic diff) is M-goals-and-holes' to formalize. Don't edit M-aif-wiring's R-contract here; feed it.

## 6. Exit conditions (testable; provisional until ARGUE)

1. C is **derived** from the current goal/hole corpus (not a static `def`), landing on the observation channels.
2. C **updates** when the corpus changes (a sorry closes / a goal is added → C shifts), off the WM hot path.
3. **`efe`'s `:G-risk` reads the live C** — demonstrated: a goal/hole change shifts a preference, which shifts `G-risk`, which **re-ranks a policy** (the belly actually steering selection).
4. A **freshness guard** flags a stale C loudly (the D7a discipline) — verified by simulating a corpus change without re-derivation.
5. Degrades safely: derivation failure → falls back to the static preference floor, never an empty C.

## 7. Scope-out (named, not hidden)

Forward method-mining (M-operational-vocabulary); the substrate-2 PROOF store / forward↔backward join (M-populate-substrate-2 D4); arming/acting on the resulting EFE (WM-I4 / M-aif-wiring Car-3, gated); the R19 contract text + the full A/B/C/D/E faithfulness diff (M-goals-and-holes). This excursion is **only** the live, fresh, wired C — the belly that beats.

## 8. Disciplines

Never restart the JVM; reload via Drawbridge `load-file`. Use `/api/alpha/census` (per-type, count-pushdown) for substrate-2 populations — never a high-limit `?type=` scan (it times out; that footgun is why the census endpoint exists). Evidence-first (specific counts/files). Gates per AGENTS.md (clj-kondo, check-parens, tests). **Ship the freshness guard with the live C, not after** — §HEAD.
