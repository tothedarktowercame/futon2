# Independent review — typed nil selection retention

**Reviewer:** codex-23
**Review job:** `invoke-1789845496196-22483-2150813f`
**Author:** codex-2
**Reviewed package:** `b8a9399d75c9f71f1ec312fe3579ad0f825fa50f` and final `1fcef69d127bc9c0f7205f9cc2824df039e981ad`
**Prior production fix:** `b30d915ecc9a0b4547724658d4035a6d381b2e0b`
**Warrant:** `test-registry-1aee489d4d02247ab846f8c62a070eb655ddcb914765609bf72b829b307637a7`

## Verdict

**APPROVED.** The prior production commit repairs the identified missing
producer-to-renderer carrier and makes an absent carrier render as an explicit
wiring gap rather than throwing during selection. The fresh package adds a
warranted regression that exercises the real generator/render handoff and the
runner's selected-entry consumer, while honestly stopping short of claiming
policy-scoring coverage.

## Warrant and scope

I consumed the existing warrant through
`POST /api/alpha/test-registry/check`; I did not re-run the suite. The check
returned `:warrant? true`, run HEAD
`1fcef69d127bc9c0f7205f9cc2824df039e981ad`, 2 tests / 11 assertions / 0
failures / 0 errors. Its declared and checked scope is exactly:

- `scripts/futon2/report/war_machine.clj`;
- `test/futon2/report/selection_nil_receipt_test.clj`.

The check returned both paths in `:diff-paths` and an empty
`:outside-closure`. The runner selected-entry implementation used by the test
is present in the recorded load closure.

## Adequacy against the retained finding

The schema-v3 finding
`repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure`
retains:

- `:failure-stage :selection` and `:failure-kind :untyped-failure`;
- `:failure-error "Cannot invoke \"clojure.lang.Named.getName()\" because
  \"x\" is null"`;
- nil `:selected-entry`, nil `:failure-data`, and not-reached downstream
  checkpoints.

The finding proves that selection rendering called `name` on nil and prevented
an action from being selected. It does **not**, by itself, retain the identity
of the nil carrier. Attribution to the omitted `:r12-admission` handoff rests
on the live reproduction documented in the historical production commit
`b30d915e`: that commit identifies the failing renderer site, records the
pre-fix reproduction, threads the existing admission receipt into the
hand-listed render map, and changes a genuinely absent receipt from an unsafe
`name nil` path to labelled `NOT WIRED` output.

The fresh regression package validates that repair without pretending to add
a second production fix:

1. An explicitly nil `:r12-admission` passed to the real renderer returns a
   string containing `NOT WIRED`, does not print `REFUSED`, and does not throw.
2. Both admitted and refused receipts produced by the stubbed admission
   boundary travel through the real `generate-war-machine` map into the real
   renderer and remain present in returned data.
3. A valid action in a structurally valid judgement survives generation and
   is recovered by the actual runner `selected-entry` consumer.

This is adequate for the recorded nil-selection root cause: the attributed
nil no longer terminates selection, and a valid selection remains consumable.
The valid judgement, policy scan ports, and admission return are controlled
fixtures. Accordingly, the warrant proves producer-to-renderer wiring,
renderer totality for this nil, and selected-entry consumption; it does not
prove live policy scoring, candidate ranking, or a complete production click.

## Schema-v3 recording requirements

Because the finding has `:repair/schema-version 3`, a later
`record-implementation!` call must provide independently observed grounded
review evidence, not only self-asserted labels. It needs:

- `:reviewer "codex-23"` and
  `:review-job "invoke-1789845496196-22483-2150813f"`;
- matching `:review-evidence` with `:state "done"`, `:verdict :approve`,
  `:valid? true`, and positive executed tool evidence;
- `:artifact-binding` for final package commit
  `1fcef69d127bc9c0f7205f9cc2824df039e981ad` in
  `/home/joe/code/futon2`, with fresh-author, descendant, in-author-window,
  and corroboration true and disagreement false;
- a distinct implementation attempt id; and
- witness fields `:resolved? true` and `:dial-moved? true`.

This note supplies the review judgment and its bounds. The implementation
recorder must independently supply and validate the job/tool observation and
artifact-binding maps. Neither this review nor implementation recording is a
store resolution or the required distinct production-shaped successor.

## Witness attestation

- **`:resolved? true`** — bounded to the retained failure and warranted
  regression: the historically attributed nil carrier is now accepted by the
  renderer as explicit `NOT WIRED` output, admitted/refused producer receipts
  reach the renderer, and a valid action reaches the real selected-entry
  consumer. The historical selection-ending shape is therefore
  retained-and-accepted in the regression rather than erased.
- **`:dial-moved? true`** — before `b30d915e`, the producer omitted
  `:r12-admission`; the consumer reached `name nil`, selection ended with nil
  selected-entry, and downstream phases were not reached. After the fix and
  fresh regression package, the receipt is handed over, an actually absent
  receipt is labelled without throwing, and a valid action remains selected.

These attestations do not claim that this fixture is a live production
successor, that policy scoring was exercised, or that the repair obligation is
resolved in the store.
