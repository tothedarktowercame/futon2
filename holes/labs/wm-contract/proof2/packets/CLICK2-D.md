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
