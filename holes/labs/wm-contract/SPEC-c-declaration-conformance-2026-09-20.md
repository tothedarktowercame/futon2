# SPEC — C declaration conformance (Lean-first), 2026-09-20

The Lean model of C is `mathlib4/DarkTower/WarMachine/TokenPreference.lean`
(P6, approved 2026-09-16) extended by `TokenPreferenceOwned.lean`
(mathlib4 4a7063b5e4, sorry-free, transcribing the 2026-09-20 multi-owner
ruling). A cascade-source declaration conforms when every row below
holds. This is the checklist for the packet-3 review; the click stays
gated behind it.

| # | requirement | Lean source | check on the declaration |
|---|---|---|---|
| 1 | want tokens nonempty | `PreferenceSpec.want_nonempty` | the target declares at least one unsatisfied want token |
| 2 | wants carry owners; any joint use is per-owner scores or typed refusal — no cross-owner scalar anywhere downstream | `OwnedPreferences.jointReport` (only joint query), `jointReport_scores_faithful` | entries name owners; no code path averages/reweights across owners |
| 3 | conflict = divergence of declared ruled-zero sets, reported, never resolved numerically | `conflictOn_iff_zeroed`, `jointReport_refusal_iff` | any cross-owner zeroing of a wanted outcome must surface as refusal, not smoothing |
| 4 | evidence tokens are presence-scored and content-blind | `PreferenceSpec.utility`, `preference_congr` | therefore the ARTIFACT a token binds as witness is the entire defense: an artifact the run itself writes FAILS review (claude-4 requirement 2026-09-20, upgrading authoring preference to gate) |
| 5 | ruled-zero only; never smoothed; zeroed ≠ univ | `PreferenceSpec.zeroed`, `zeroed_proper`, `preference_eq_zero_iff` | no default/smoothed zero masses; an empty reachable-want domain refuses upstream rather than scoring |
| 6 | lam, mu are DECLARED named parameters, not derived | `PreferenceSpec.lam/mu` fields + module header | scale parameters appear in the declaration with their declared-as-such label |
| 7 | monotone coverage: strictly less want progress at equal evidence presence is strictly dispreferred | `preference_lt_of_want_lt` | no declaration structure that rewards stalling (e.g. wants satisfied by the machine's own bookkeeping) — ties to external-truth-maker correction 1 |
| 8 | single-owner declarations are conservative: the owner dimension extends without rework | `jointReport_subsingleton` | packet 3 may land single-owner now; adding rob/joe/community owners later changes no existing behavior |

Contract-layer obligations OUTSIDE the Lean model (acquisition is
external by the module headers, still required for conformance):
producible (in pattern :produces after reinterpretation) and
generalizable (meaningful in another community's collaboration) per the
2026-09-20 declaration record; live-C weight arrives only via
:projection :mission-declared-wants, never hand-authored mass.
