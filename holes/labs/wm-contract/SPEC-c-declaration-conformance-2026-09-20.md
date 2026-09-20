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
| 2 | wants carry owners; any joint use is per-owner scores or typed refusal — no cross-owner scalar anywhere downstream | `OwnedPreferences.jointReport` (only joint query), `jointReport_scores_faithful` | entries NAME owners (ruled: "C entries carry owners", bb34b120) as PROVENANCE. RELAXATION 2026-09-20, recorded: the runtime is single-spec and consumes no :owner field today; `jointReport_subsingleton` proves the scalar path equals the owned model's report on a single-owner domain, so today's scores are correct with owners carried as metadata. Queued implementation (not latitude): thread ownership through loading/assembly/projection with typed rejection of unsupported joint ownership — rejection is guard-shaped and presumed unwanted during tuning, so it queues rather than blocks. NONCONFORMING today: presenting :owner as consumed, or scoring multiple effective specs through the scalar path |
| 3 | conflict = divergence of declared ruled-zero sets, reported, never resolved numerically | `conflictOn_iff_zeroed`, `jointReport_refusal_iff` | any cross-owner zeroing of a wanted outcome must surface as refusal, not smoothing |
| 4 | evidence tokens are presence-scored and content-blind | `PreferenceSpec.utility`, `preference_congr` | therefore the ARTIFACT a token binds as witness is the entire defense. AMENDED 2026-09-20 (second pass; exposure is cheapness, not only authorship — claude-4 review flag, grounded in the accepted correction 1: the model case is an authored commit bound at a sha, checkable in git vocabulary, unfabricable by bookkeeping): (a) an artifact the run itself writes FAILS; (b) an artifact whose whole content merely asserts the event FAILS unless it carries a resolvable reference (repo + sha/entry) AND the observation procedure RESOLVES it — the check is "the referenced object exists in the named repo", not "the assertion file exists". A witness satisfiable in a minute of file-writing without the event's own remains does not conform |
| 5 | ruled-zero only; never smoothed; zeroed ≠ univ | `PreferenceSpec.zeroed`, `zeroed_proper`, `preference_eq_zero_iff` | no default/smoothed zero masses. CORRECTED 2026-09-20 (claude-4/codex-3 review): the earlier "empty reachable-want domain refuses upstream rather than scoring" overclaimed Lean authority — the model permits a nonempty fallback want (want_nonempty holds) and the cited theorems govern zeroed sets, not caller behavior. The recorded judge decision stands (war_machine.clj:6201-6222): the PRODUCER's :no-reachable-want refusal is typed; the caller proceeds on the prior uniform spec and RECORDS :derived-no-overlap on the certificate. Required here: that record is present and cannot read as derived-and-agreed |
| 6 | lam, mu are DECLARED named parameters, not derived | `PreferenceSpec.lam/mu` fields + module header | scale parameters appear in the declaration with their declared-as-such label |
| 7 | monotone coverage: strictly less want progress at equal evidence presence is strictly dispreferred | `preference_lt_of_want_lt` | no declaration structure that rewards stalling (e.g. wants satisfied by the machine's own bookkeeping) — ties to external-truth-maker correction 1 |
| 8 | single-owner declarations are conservative: the owner dimension extends without rework | `jointReport_subsingleton` | packet 3 may land single-owner now; adding rob/joe/community owners later changes no existing behavior |

| 9 | C reaches the evaluator as a STEP-INDEXED family in `preferenceAt`'s shape: the declared `PreferenceSpec` at its declared step, a stated distribution elsewhere | `ZeroPreferenceExclusion.preferenceAt` (+ `preferenceAt_nonneg`, `preferenceAt_sum`); Holes.lean C591 deferral text: "a time-indexed Cτ family whose terminal member is the ruled outcome-kind distribution" | the producer supplies the family, not one spec applied at every step (`:constant-spec` is the producer collapsing what the model varies). The spec's step placement (terminal, per the C591 text, until ruled otherwise) and the elsewhere-distribution are DECLARED and recorded — never invented per run, and never varied merely to flip a verdict; uniform-over-non-ruled-zero elsewhere is the honest "no opinion before the due step" and is recorded as declared |

Contract-layer obligations OUTSIDE the Lean model (acquisition is
external by the module headers, still required for conformance):
producible (in pattern :produces after reinterpretation) and
generalizable (meaningful in another community's collaboration) per the
2026-09-20 declaration record; live-C weight arrives only via
:projection :mission-declared-wants, never hand-authored mass.
