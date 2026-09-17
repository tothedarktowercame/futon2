# Mission: Wire G Through the War Machine (M-G-wm-wiring)

**Date:** 2026-09-15
**Status:** HEAD. Campaign chartered by Joe (emacs-repl, 2026-09-15): "we need to get real evidence that it can be completed… a new M-G-wm-wiring.md campaign that indexes into these checklist items as its (complex) gap, and that develops a strategy for working through them in a reasonable order that will get us to completion."
**Owner:** Joe. Build/review seats to be assigned per stage (current active: claude-20 lead/F13, codex-26 spec+Lean, codex-27 kernel, zai-7/zai-8 Zaif-fitness + verification).
**Repos:** p4ng (the checklist, `CHECKLIST-fundamentals.md`, revision 4 = `b2c7d1d`), futon2 (wm-contract lab; runtime), mathlib4/DarkTower/WarMachine (the Lean model), futon3/library (flexiargs, buffer-cleaner exercise).
**Cross-ref:** [[M-G-over-cascades]] (the definition this mission operationalises in production), the ratified checklist revision 4 (`p4ng b2c7d1d`, SHA256 466a2e19…), SPEC-cascade-policy-semantics-2026-09-15.md, ZAIF-FITNESS-acceptance-draft.md, the buffer-cleaner exercise (futon2/holes/labs/wm-contract/buffer-cleaner-adapter/ + declarations in futon3/library/buffer-cleaner/).

---

## HEAD (operator intake)

*Provenance: Joe, emacs-repl, 2026-09-15.*

"OK, this is itself a ton of work and we need to get real evidence that it can be completed — overlap with C is incidental to that question. Since the checklist is complex, I think we should create a new M-G-wm-wiring.md campaign that indexes into these checklist items as its (complex) gap, and that develops a strategy for working through them in a reasonable order that will get us to completion."

Two rulings embedded here:

1. **The completion question is primary; C-family overlap is incidental.** WM-13's broader C-family is not on this campaign's critical path except where a G computation concretely needs that member (the ruled terminal `machineC` suffices for the first end-to-end wiring; episode-grain scoring per `Holes.C`'s terminal member is in-scope; the full grain/time-indexed family stays with the checklist).
2. **The deliverable is *evidence of completability*, worked in a defensible order** — each stage must produce a checkable artifact, not accumulated machinery. A stage that cannot show its own completion evidence blocks re-ordering, per the checklist's substitution list.

## The gap, indexed (checklist revision 4, `#ck-` anchors)

The campaign's gap is the set of checklist items that stand between "canonical Lean cascade-G definition exists" and "G is computed, selected on, and witnessed in a production run." In dependency order, not checklist order:

**Stage 0 — settle G's semantics. SETTLED IN PART by Joe, 2026-09-15 (memory e-ca7f3431):** G is defined over cascades **in any state of completion**; cascades are **constructed progressively and per problem** (like a mission's ARGUE phase); **EFE is minimised** — length is endogenous (extend if G decreases, stop if it increases). Joe states this is *not a ruling but a property of the computation*. Consequences: (a) the A/B fork as a global formula choice dissolves — no structural length penalty or reward, and the scorer must compute G on partial cascades; (b) Π (candidate set) is **formed per problem**, NOT a static authored subset — zai-8's earlier "near-agreed authored-subset pin" was rejected by Joe outright. Remaining open (Q2 episode-grain, Q3 fuel, Q4 mid-run transitions, Q5 yield): Joe has no opinion; agents work them out **by extending the published blog post with argumentation and examples**, not by fiat — the buffer-cleaner example is the vehicle.

**Stage 1 — the probability foundation.**
- `#ck-WM-01` — kernel repair acceptance: independent acceptance receipt for `480a666ad2`; dependent proofs re-bound to the repaired source (no admission inherited by filename).

**Stage 2 — the Lean model, proved.**
- `#ck-WM-10` (Lean half) — packet 1b/1c: the finite generative model (A, cascade-determined B_π, C, D, T) with G derived, the eight acceptance theorems (incl. composition control §3.6 and the Alexander controls), no sorry/native_decide/new axioms, axiom dependencies printed.

**Stage 3 — the substrate G scores.**
- `#ck-WM-08` — `find` serving activation, receipt-mode run with source-bound interpretations, honest F4 designation.
- `#ck-WM-09` — `organise` serving activation, ordinary constructed cascade family, click receipt; legacy `organise`'s sorry explicitly retired by connection.

**Stage 4 — the inputs G consumes.**
- `#ck-WM-04` — measured A with observed authority (annotation admission; no smoothing).
- `#ck-WM-03` — B interpretation coverage over the production pattern domain.
- `#ck-WM-02` — current belief reaching the cascade predictor as the model's state distribution.
- `#ck-WM-05` — Q(s|π)/Q(o|π) from those inputs over the full policy, feeding the actual scorer.
- `#ck-WM-06` — matched Q/C consumption at the scorer (terminal machineC + the disposition bridge, per owner bell `invoke-1789481249314-21037`).

**Stage 5 — the runtime scorer and live selection.**
- `#ck-WM-10` (runtime half) — F13: runtime cascade-G implementation, independent numeric correspondence, real candidate family, and a selection that consumes these scores. First live exercise already chartered: the buffer-cleaner domain (real flexiargs from futon3/library/buffer-cleaner/, machinery in the wm-contract lab, G computed over the real 137-buffer packet).
- `#ck-WM-11` — candidate prior/posterior preserving full cascade+precedence identity; selected-to-enacted correspondence; fuel accounting only if Joe rules it in.

**Stage 6 — production witness.**
- `#ck-R5` — WM-06/WM-10 connected and witnessed in production.
- `#ck-E05` (G-relevant slice) — the qualifying timestamped run + Lean-computed validation of its runtime certificate; `:cascade-g-not-computed` must be impossible to emit on the qualifying path.

## Strategy and order

**The order above is the dependency order, and it is also the evidence order**: each stage's completion evidence is the next stage's admission ticket. Concretely:

1. **Stage 0 first, and its Joe-facing part is settled** (memory e-ca7f3431): the A/B fork is dissolved — G over partial cascades, progressive construction, EFE-minimal endogenous length, per-problem Π. What remains of Stage 0 is agent work: Q2–Q5 argued out on the blog post with examples (not by fiat), and the spec now carries the properties (`0fe3ed68`). Nothing downstream may adopt any open axis (episode-grain, fuel, mid-run transitions, yield) as a default. The buffer-cleaner example demonstrates the settled properties with real numbers.
2. **Stage 1 is small and already in flight (codex-27).** Independent acceptance of the kernel repair unblocks everything mathematical.
3. **Stages 2 and 3 can proceed in parallel once 0–1 land** — the Lean proofs and the find/organise serving activation are independent seams. Parallel tracks must not merge evidence: each carries its own receipts (checklist substitution list).
4. **Stage 4 is the long pole and the honest-risk center** (measured A especially: `#ck-WM-04` is OPEN with a 0/86 licensable-close census on record). Strategy: drive it by the scorer's actual consumption needs — measure A where the first real candidate family actually observes, not the whole domain up front. Completion evidence per row, not per sweep.
5. **Stage 5 lands G in two steps**: first the buffer-cleaner domain (small, real, fully-declared flexiargs, adversarially reviewed inputs — already in motion), then the mission/ticket domain (the controller's current action-grain scoring remains the transitional state until the cascade scorer replaces it; regression guard against the three-mission restriction per `#ck-R6`).
6. **Stage 6 is the checklist's E05 discipline** — the qualifying run validates every required node and connection in one record; no stage-5 success substitutes for it.

**Re-ordering rule:** the campaign may re-order within a stage, but not across stages, without a recorded reason that shows the dependency is actually absent. "It seemed efficient" is not a dependency argument; the 09-14 DAG built without Joe's inventory is the cautionary precedent.

**Evidence-of-completability, per Joe's framing:** the campaign is succeeding when each stage closes its checklist items with the checklist's own evidence standard (definition + proof deps + implementation + correspondence checks + production caller + joined runtime evidence). The first end-to-end slice — buffer cleaner: Stage 0 decision → Stage 2 Lean at that grain → Stage 5 scorer over the real packet → a live selection between the two wirings with the rule-verdict column checked — is the campaign's earliest proof that the whole can be completed, and doubles as the blog post Joe asked for.

## Non-goals

- The full C-family (`#ck-WM-13` beyond the terminal-member slice) — stays with the checklist.
- Paper reconciliation (`#ck-E06`), cross-paper promises (`#ck-E07`) — separate obligations; this campaign reports facts to them, does not close them.
- Fuel accounting, yield pricing, mid-run board transitions — pending Joe; adopted only on ruling.
- Any substitution from the checklist's substitution list. Forbidden here as there.

## Checkpoints

- [ ] CP0: ~~A/B ruling recorded~~ **SETTLED 2026-09-15** (Joe, memory e-ca7f3431): fork dissolved into the progressive/partial-cascade property; per-problem Π. Remaining: Q2–Q5 argued on the blog post with worked examples; spec carries the properties (`0fe3ed68`) — *zai-7 + codex-26, verified by zai-8.*
- [ ] CP1: kernel repair independent acceptance receipt — **owner must be independent of the repair author**; live dispatch (2026-09-15): codex-3 writes the verdict, claude-3 reviews (job invoke-1789507763981-21248); receipt lands in runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/; WM-01 not ticked by the receipt alone. *(Corrected 2026-09-15: the original text named codex-27, the repair author — a non-independent seat — reviewed by claude-20; found by claude-2, exit-interview Finding A. Owner fields in stage lines are proposals subject to independence and live-owner rules, not authority.)*
- [ ] CP2: packet 1b/1c proved, axiom receipts printed — *codex-26.*
- [ ] CP3: find/organise serving activation with receipt-mode clicks — *codex-27/26, reviewed by claude-20.*
- [ ] CP4: measured-A rows for the first real candidate family's observations; B/belief/Q feeding the scorer — *owner to assign.*
- [ ] CP5a: buffer-cleaner end-to-end: G computed over the real packet, both wirings, rule-verdict checked — *codex-26 + zai-7, verified by zai-8.*
- [ ] CP5b: mission/ticket-domain cascade-G scorer live, replacing the transitional controller — *claude-20 (F13).*
- [ ] CP6: qualifying run with Lean-computed certificate; `#ck-R5` and the E05 G-slice closed — *all seats + Joe's acceptance.*
