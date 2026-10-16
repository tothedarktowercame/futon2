# Q9 diagnosis: the production C is neither factorized nor authorized (2026-10-16)

Requested by codex-68 (invoke-1792115407799-15001-8fdab744). Bases: futon2
`969361417` (`claude-10-q5-family`), mathlib4 `9e79af1bee` (`darktower`).
Diagnosis only; no runtime, exporter or Lean change. Stopped for a ruling,
because the runtime and the theory conflict.

## What Q9 claims (mathlib4 `9e79af1bee`)

`Requirements.Q9`: `factorizedPreference = some c` and
`FactorizedPreferenceSemantics.Certificate.Valid calibrationAuthorizations
factorizationAuthorizations c`. That needs:

1. a `RelevanceCalibration` over **focused / related / unrelated** with
   `focusedCost < relatedCost < unrelatedCost`, nonzero identity, version,
   authority and registration token, and `calibration.authority ≠
   selectorAuthority`;
2. a calibration registry entry with that receipt, that exact calibration,
   `authorizedBy = calibration.authority`;
3. a factorization registry entry with the certificate's identity, version
   and authority, `authority ≠ selectorAuthority`, and law
   `completionThenConditionalRelevance`, in which completion is
   lexicographically prior (`completion_always_outranks_noncompletion`,
   `relevance_cannot_reverse_completion`).

`flat_table_without_receipt_is_not_certificate`: no table alone can
discharge it.

## What production does (futon2 `969361417`)

**The C that selects.** On the reading-family route the one C that enters G
is `cascade-shape-g/progress-preference` (`src/futon2/aif/cascade_shape_g.clj:159`),
a distribution over `[completed-pattern-count want-met?]` with weight
`exp(4·done/n + 2·met?)`. Its model provenance is `{:status :synthetic
:calibrated false :source "cascade_shape_g arrangement model"}`.
`reading-policy-preference` (zai-5, `482e4e74d`) certifies this as the single
C and types the old 55/35/5/5 class table as `:diagnostic-only/did-not-select`.
`cascade-decision/precision-config` names its authority
`:cascade-shape-g/progress-preference`, version "progress-preference v1".

Against each Q9 condition:

| Q9 needs | production has |
|---|---|
| a relevance coordinate (focused/related/unrelated) | none: every policy in the field is for the one selected task; C has no relevance outcome |
| completion lexicographically prior | not so: for n ≥ 5 progress tokens, `[n−1, not met]` outweighs `[1, met]` (n = 5: e^3.2 ≈ 24.5 vs e^2.8 ≈ 16.4; n = 8: 33.1 vs 12.2). Progress trades against completion |
| calibration authority ≠ selector authority | the constants 4.0 and 2.0 are in the scorer's own source; no authority outside it |
| calibration and factorization registries | none, anywhere: no config, storage, or code produces `:wm/factorized-preference-v1` or a registry (repeat of the 2026-10-11 search in `PLAN-post-click-51-recovery`, re-checked in futon2 `resources/` and `~/code/storage`; futon2 has no `config/`) |

**Who produces the carrier now.** Only `factorized-preference-sut/compose-and-score!`,
in `:controlled-sut` mode, from registries the caller supplies. Production
mode refuses it (`:production-registries-unconfigured`), and the namespace
is not on the serving path.

**What the exporter reads.** `wm_run_facts/factorized-preference-facts`
reads `[:decision :factorized-preference]` and checks the registry joins.
Nothing on the production route writes that key, so production Q9 is NR.
The carrier it would accept is self-asserted: it is not sealed and not bound
to the record, so a record edited to contain one would export as Q9 true.

**Does production record enough causal evidence?** For a *negative* verdict,
yes: the scored record keeps the schedule it scored with
(`:preference-at-each-step`), the precision config names its authority, and
`reading-policy-preference` recomputes it. That is enough to show,
recomputably, which C selected and that it is not a valid factorized
certificate. For a *positive* verdict, no: there is no calibration, no
relevance outcome, and no external authorization to record.

## The recurring missing fixture

`wm-run-facts-test/q9-historical-click-record-has-no-authorized-factorization`
reads `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-….edn` by relative
path. `data/*` is gitignored, so the record (5.3 MB) exists only in the
shared checkout `/home/joe/code/futon2`, and the test errors in every
worktree. That is why it recurs in each disposition. It says nothing about
Q9 itself: the record (click of 2026-10-05) has flat class weights and no
carrier, and its Q9 is NR in the shared checkout. I have not copied,
trimmed or reconstructed it.

## The conflict

Q9 says completion is structurally prior and relevance is calibrated by an
authority outside the selector. The production selector scores with a C in
which progress trades against completion, has no relevance dimension, and is
calibrated by its own source. A sealed carrier on the current route can only
certify that Q9 does not hold. Making Q9 true requires changing the C that
selects (the design note's stage 4, "cut scoring over … under an explicit
authority record") and an authority that is not mine or the selector's.

## Options for the ruling

- **(a) Certify the negative now.** A sealed, record-bound carrier on the
  reading route recording the C that selected (schedule digest, authority,
  recomputation) and the typed reasons it is not a certificate
  (`:no-relevance-coordinate`, `:completion-not-lexicographic`,
  `:calibration-authority-is-selector`, `:no-registry`). The exporter emits
  `factorizedPreference := none` from a verified carrier (Q9 **false**, not
  NR) and NR when the carrier is absent or tampered; forged positive
  carriers are refused. No change to selection. Lean unchanged (`none`
  already means "cannot establish Q9").
- **(b) Factorize the production C.** Replace `progress-preference` with a
  completion-first law (completion lexicographically prior, progress only
  within a completion tier) and record it. Changes selection, so Q5's
  precision identity changes (beta re-armed by declaration) and Q4/Q6
  fixtures move. Still Q9 false without (c), and the relevance coordinate
  remains empty on a single-task field, so Theory may need to say what
  "relevance" means inside the selected task's family.
- **(c) Commission the authority.** Someone other than the selector (Joe, or
  a registry Joe names) supplies the calibration and factorization
  registries, pinned in `config/`; production loads them, and the carrier
  binds to their pins. Only (b)+(c) together can make Q9 true.
- **(d) Amend Q9.** If relevance has no meaning inside one task's family,
  Theory restates Q9 for the reading route (for example, completion
  lexicographically prior over the progress outcome, authority separate),
  and the runtime follows.

For the fixture, independent of the ruling: point the test at a named data
root (an env var or the shared checkout) and skip with a typed reason when it
is absent, rather than erroring.

I recommend (a) now, since it is a faithful repair that moves Q9 from NR to
a recomputed false without touching selection, with (b)/(c)/(d) as the
decision about what the production C should be.

## Checked

- Lean: read `Requirements.lean` (Q9, witnesses, `click20_not_Q9`) and
  `FactorizedPreferenceSemantics.lean` at `9e79af1bee`. Not built.
- Runtime: read `cascade_shape_g.clj` (progress-preference, score-arranged),
  `reading_policy_preference.clj`, `cascade_decision.clj` (precision-config,
  class table), `factorized_preference_sut.clj`, the Q9 exporter and Lean
  term generator, and the Q9 tests. The completion-vs-progress inversion is
  computed from the weight formula, not from a run.
- Fixture: located, size and ignore rule checked; not read.
- Not run: tests, live click, hot load.
