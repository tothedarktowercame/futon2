# CLICK2-D — why click 2 (wm-click-2b77ec0d) constructed no candidate

Discovery packet for the B4 owner (zai-2), 2026-09-24. No code changed.

## Where construction looks

For an admitted target, candidates come from ONE source read: the declared
cascade-source file `resources/wm/cascade-sources/<target>.edn`, read by
`cascade_sources.clj/load-declared` (:138) at judgement assembly
(`war_machine.clj:7029–7037`), which keeps `[:candidates target]` (each with
`:precedence` and `:construction-receipt`) alongside the observed facts,
want, locators and interpretation receipts. There is no other candidate
store: `cascade_problems.clj/constructed-candidates` (:53) filters that list
— a constructed cascade is a non-empty `:precedence` vector PLUS a
`:construction-receipt`. No ledger, no registry read at construction.

## The deciding code path

`cascade_problems/assemble-one` admits the problem (universe, patterns,
want, locators, β all pass — the file is unchanged and still carries C1/C2
with receipts), and then `war_machine.clj/admit-cascade-problem` (:6596)
runs `candidate-want-progress` (:6549) per candidate: it rolls the
candidate's patterns out on the target's FRESH TRUE FACTS and requires that
some initially-absent want token reach positive terminal probability. When
every candidate declines, `target-decline` emits
`{:kind :no-constructed-candidate :missing […]}` — the row the scan table
shows. (Alternatively, when `constructed-candidates` itself is empty,
`assemble-one`'s own `(empty? constructed)` branch emits the same kind with
`:missing :candidates`/`:construction-receipt` — not today's case.)

## Why T-repair-occ-444fb018 had C1/C2 yesterday and nothing today

The declared file is unchanged (same path; sha256
`a163748f…` in both run records). What changed is the OBSERVED WORLD:
`:restoration-accepted` — the target's sole want token — is
`{:observed false, … resolved-sha a1957b7c…}` in run 1790199409 (2026-09-23)
and `{:observed true, :check :C4, … "**Status:** DONE", resolved-sha
aceb8f26…}` in run 1790225596 (2026-09-24): the ticket file now reads
DONE, and `observe-facts` reads it through the C4 locator at load time.
With the want already true at admission, `initial-wanted` is non-empty and
`new-wanted` comes out empty for EVERY candidate — add-only transitions
cannot newly satisfy an already-satisfied want — so each candidate declines
`:no-new-wanted-token` → `:new-wanted-token-within-horizon`, and the target
refuses `:no-constructed-candidate`. The refusal is honest: the work the
candidates existed to do is, by the record's own mechanical check, done.
The other three targets in the table fail the same admission gate for their
own fact/want states (their files also carry receipted candidates).

## Are the B4 slices on this path?

No. `candidate_derivations.clj` runs only AFTER selection, on the decision
certificate — it cannot influence admission. The `:cascade-sources`
threading (2c) feeds only the derivations carrier. `produces-of` (2a)
affects `pattern-kernel`/`transition-row` for patterns carrying only one
spelling of produces; the declared record's patterns carry both, so the
rollout used by `candidate-want-progress` is byte-for-byte unaffected
today. None of the three slices is on the construction/admission path.

## One carrier gap this exposed (finding, no fix)

The abstained run record EDN carries NO decline or refusal detail — zero
occurrences of `no-constructed-candidate`, `:no-new-wanted-token`, or
`:declines` in `tick-run-record-2026-09-24-1790225596.edn`; the per-target
reasoning survives only in the generated scan markdown. The record's
absence of the want-already-satisfied evidence is an untyped absence on
this click: a reader of the EDN alone cannot distinguish "no candidates
declared" from "want already true". If PROOF-2 wants P₀'s refusals readable
from the record, the admission declines need a typed carrier on the
abstention path too.

## Part 2 (2026-09-24, zai-2) — the judge's mechanical check, run offline per target

Method: `cascade-sources/load-declared` + `cascade-problems/assemble` +
`candidate-want-progress` (war_machine.clj:6549) verbatim, on the current
C4-observed facts, in a private process (no shared JVM, no clicks, no
writes; script run from /tmp, never staged). Declared horizon = 4.

| Target | receipted candidates | want tokens | want already true at HEAD | decline reason |
|---|---|---|---|---|
| M-f11-find-production-successor | 1/1 (precedence `[:apparatus/done-is-observed-running]`) | `:hole/h9ab212b3281d`, `:hole/h2045faa0e7cc` | `:hole/h9ab212b3281d` | `:no-new-wanted-token` — the absent want is produced by NO declared interpretation |
| M-aif-policy-conditioned-eig | 2/2 (`[:apparatus/one-authority-per-question]`, `[:contracts/every-entry-has-a-falsifier]`) | `:hole/h6378c65a4012`, `:hole/h0e270aa090bc`, `:hole/h42fceb4ad48b` | `h6378c65a4012`, `h0e270aa090bc` | `:no-new-wanted-token` — the candidates' patterns produce already-true tokens (guards skip them as completed) |
| M-wm-08-external-f2 | 1/1 (`[:cascade-construction/run-it-on-a-real-case]`) | `:route-a-rehearsal-reported` | `:route-a-rehearsal-reported` (sole want) | `:no-new-wanted-token` |
| T-repair-occ-444fb018… | 2/2 (C1, C2) | `:restoration-accepted` | `:restoration-accepted` (sole want; ticket `**Status:** DONE`) | `:no-new-wanted-token` |

All four declined `:no-new-wanted-token`; none for want of receipts or
precedence. What would have to change IN THE WORLD (not the declaration):

- **T-repair-occ-444fb018**: the ticket's Status line no longer reading
  DONE at HEAD, so `:restoration-accepted` observes false again; C1/C2's
  rollouts then newly produce it (run 1790199409 is the existence proof).
  This is the only one of the four where a pure world change re-enables
  construction under the declaration as it stands.
- **M-wm-08-external-f2**: the rehearsal report locator (`:decl` no longer
  matching) observing false, with both guard needs
  (`external-expectation-validator-exists`,
  `independent-expectations-written`) still true — then the candidate newly
  produces `:route-a-rehearsal-reported`. A world change works here too,
  though "un-report a rehearsal" is not a change the world makes forward.
- **M-f11-find-production-successor**: NO world change suffices. The
  absent want `:hole/h2045faa0e7cc` is produced by no declared pattern, so
  no add-only transition can reach it; and if it simply became true, the
  candidate still adds no new want. Only a declaration change (an
  interpretation that produces it, and a candidate citing it) constructs.
- **M-aif-policy-conditioned-eig**: no world change suffices either. The
  absent want `:hole/h42fceb4ad48b` IS producible — the declared
  interpretation `:aif/two-layer-calibration` (needs `h6378c65a4012`,
  true; forbids the want itself) produces it — but NEITHER declared
  candidate's precedence contains that pattern, and the candidates' own
  patterns produce only already-true tokens. A declaration-side change
  (a candidate whose precedence includes `:aif/two-layer-calibration`)
  would construct today.

**Plain answer:** none of the four can construct a candidate today under
any forward world change except by undoing observed work (T-repair-occ
ticket status; wm-08 rehearsal report). Two of the four (f11, aif-eig) are
construction-blocked by the DECLARATIONS themselves — reachable or
unreachable false wants with no candidate routed at them — which is B4's
authoring gap in miniature, not a world state. If clicks 3–10 are to
construct rather than abstain, either the two mission targets get
candidates aimed at their false wants (declaration work), or new targets
with false, pattern-producible wants enter the sources. Holding clicks on
"no candidate can newly satisfy any want" is correct at HEAD.
