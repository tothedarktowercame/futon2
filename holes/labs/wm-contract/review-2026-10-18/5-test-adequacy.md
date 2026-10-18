# 5 — Test adequacy for the week's repair commits

Reviewer: Claude (Opus), read-only. Reviewed at futon2 HEAD
`0975c7df7b5a21e17bd925dcb1c2c1c1bacbfa68` (line numbers below are at that sha).

Commits read (diff + test): 48e8ae2d0, ee7275ffc, 6b9f02660, 0a45789b8,
c70495fc9, 03c8830af, 3037b082f, 74c35e797, 311689b8b, ccf6d30d8, 0975c7df7;
sampled from the repair/fix list: 485960046, 9cdada7de, c83808a53, 9ed8c4525
(and the stat of c77c619c3, 18cb8cc12, 2edd58840, 51eb13f19).

Evidence used beyond reading: the two saved Zai replies from clicks 3 and 4
(GET `/api/alpha/invoke/jobs/<id>`, copies in `scratch5/`); a Python
emulation of the 0a45789b8 and HEAD decoders over those bytes; one short
tooling JVM (`clojure -M -i scratch5/cases.clj`) that ran 8 bad cases through
the loaded `handoff-contract/response-json-object` (no test runner, no
:7070 contact). Warrant checks: the `test-registry/check` POST plus read-only
reads of `~/code/storage/test-registry/warrant-index.sqlite` and
`futon2-suite/index.json`.

## Background: the click-3 regression did not use the click-3 reply

This is history, not a forward risk, but it shows how the tests are being
written. The TN says of 0a45789b8: "The regression reproduces the live answer:
the old decoder returns nil and the new decoder returns the analysis map."
It does not. The test (`selected_target_reading_test.clj`,
`fenced-json-reply-is-decoded`) builds a fence around a four-key toy map. The
actual click-3 reply (job `invoke-1792271230520-16329-52ac4729`, 11,920 chars)
is a fenced object **followed by a completion note**:

    ...}\n```\n\nValidated end-to-end against the authoritative `validate()` in ...
    Not published; nothing changed in any repository.

The 0a45789b8 rule (`trimmed` must start and end with a fence) returns nil on
those bytes (checked by emulation). So the commit that was hot-loaded as the
click-3 repair would have refused the click-3 answer again, and click 4 spent
a click finding the same reply shape (job `...16348-d6084234`, same trailing
note). If the saved reply had been loaded from the job record, the test would
have failed before click 4. 03c8830af's test still uses a hand-written note,
not the saved bytes; nothing in the suite loads either saved reply.

## Findings, ranked by how likely the gap is to let the next live click fail

### F1. 3037b082f dropped the offset diagnostics from offset-failure corrections (regression, untested)
- `src/futon2/aif/selected_target_reading.clj:422-438`. Before 3037b082f the
  correction feedback was `(str (:err validated0) "Exact offset diagnostics: " ...)`,
  and it was sent whenever the Python validator rejected the answer (9cdada7de
  added it for the 10-16 offset failures). After it, the diagnostics are
  inside `(when (zero? (:exit validated0)) ...)`, so they are sent **only when
  the validator passed**, which is the case where offsets are already right.
  An offset/cue-span error (the commonest Zai reading failure on 10-16: see
  c77c619c3, 18cb8cc12, 9cdada7de, 485960046) now gets the validator's stderr,
  plus an irrelevant "Pattern citation diagnostics: []" and the
  every-fragment-must-cite paragraph, and loses the exact expected slices.
- Why no test caught it: 485960046's test plants a bad display cue but checks
  only that 2 contacts happened; 9cdada7de's test calls `offset-diagnostics`
  directly. No test asserts the correction feedback carries offset diagnostics.
  The new 3037b082f test only covers the validator-passed case.
- Failure scenario: Zai's first reading has a cue outside its fragment, so the
  correction prompt has no slice diagnostics. If the second answer is wrong
  again, the result is `:selected-target-reading-validation-refused`, i.e. an
  abstained click.
- Severity: likely-later, and it could be the next click. Confidence: confirmed
  by reading the diff and the HEAD code.

### F2. Neighbouring reply shapes the shared decoder refuses, none tested
`handoff_contract.clj:21-32`. Results from running the loaded function:

| input | result |
|---|---|
| fenced object + note containing a second fence (e.g. ```` ```bash python3 validate.py``` ````) | nil |
| `"Here is the analysis:\n{...}"` (prose then a bare object) | nil |
| fenced object whose JSON string contains a literal ```` ``` ```` | nil (the lazy regex ends the body early) |
| one-line ```` ```json {...}``` ```` | nil |
| `"{...}\nDone, validated."` (bare object then prose) | **accepted** |
| `"{\"a\":1}\n{\"b\":2}"` (two bare objects) | **accepted, first one wins** |

- The first two rows are the nearest neighbours of the click-3/4 shape. Both
  saved replies already end with a prose note that uses inline backticks, so a
  note quoting a command in a fence is a small step away. Either row gives
  `:response-invalid`, then `:selected-target-reading-answer-invalid`, then an
  abstained click. The decoder's own docstring says these are meant to be
  refused, so this is a policy choice. But it is the class of input that has
  already cost two clicks, and the tests do not pin it.
- Row 3: fragment `text` fields quote source lines verbatim, and every
  nonblank line of the mission HEAD is a sentence (`wm_task_reading.py:60`).
  Across 264 mission heads only 1
  (`futon3c/holes/missions/M-operational-readiness-traceability.md`) contains
  a fence, so this is rare.
- Rows 5-6: the unfenced branch uses cheshire `parse-string`, which ignores
  trailing tokens. The docstring and test say "unfenced prose ... refused" and
  "exactly one object". That is false for prose or objects *after* a bare
  object. This is permissive rather than a failure risk, but the two branches
  apply different rules, and the test does not show it.
- Severity: likely-later. Confidence: confirmed by execution of the loaded fn.

### F3. The path a cascade takes after `:cascade-ready` has never run, and its tests stop before it
- 0975c7df7 turns a routed frontier into `{:status :cascade-ready}`
  (`cascade_decision.clj:1640-1659`). The runner then continues the ordinary
  author/build/review/close path with the cascade entry
  (`full_loop_runner.clj:6526`). The route is a keyword
  (`:full-loop-build-review-close`); nothing checks that the ordinary build and
  close accept a cascade action (`[:action :precedence :co-apply]`).
- Test coverage: `cascade_runner_seam_test/authored-frontier-routes-into-the-existing-full-loop`
  passes when the run stops at `:author-dispatch`, which a stubbed
  `:dispatch-fn` throws on purpose (line 108). The recorded flight stops at
  `:production-close-evidence-missing` by design. So no test, replay or live
  click has run a cascade entry through author dispatch, build, review or close.
- Production has no `:cascade-operational-authoring` source
  (`cascade_decision.clj:1471-1481` falls back to empty authoring). A live
  cascade therefore opens every hole and goes to the D4 handler
  (`full_loop_runtime.clj:104-133`). That handler needs Zai to return a patch
  whose source digest matches and a reviewer map that echoes the author answer
  plus `:accepted-by`. The D4 tests (`full_loop_runtime_test.clj:50-67`) use
  hand-made author/reviewer answers built from the request itself. No real
  agent output of that shape exists anywhere.
- Severity: blocks-next-click if selection and reading succeed. Confidence:
  plausible (the path is untested; I did not trace the build/close code for a
  specific refusal).

### F4. The pattern-ref rule still has two copies; the corrected answer is not re-checked
- The published acceptance contract (`futon3c/scripts/session_turn_analysis.py:80-85`,
  `pattern_references: {"container": "array", ...}`) still does not say a
  citation is required. The registry still refuses an analysis with no
  validated ref (`target_reading_registry.clj:121-126` via
  `analysis_cascade/analysis->cascades`). 3037b082f did not change the contract
  the author sees first. It made the mismatch cost one correction round instead
  of a click.
- After correction, only the Python validator reruns (`selected_target_reading.clj:441-444`).
  `cascade-failures` is not called on the corrected answer. A second zero-ref
  answer therefore goes to publication and is refused there, with the same
  abstain as click 5. No test covers a correction that is still deficient: the
  3037b082f test's second answer is always fully correct.
- Neighbouring cases checked: a missing `pattern_refs` key, `[]`, and
  `null` all behave correctly (`refs = fragment.get("pattern_refs", [])`; a
  non-list raises an error, so the validator exits 1 and a correction runs).
  An unknown pattern id makes the validator exit 1, so a correction runs. A
  *partial* citation (some fragments cited) constructs and publishes, because
  `analysis->cascades` drops uncited fragments. The TN's "required at least one
  validated pattern reference on every fragment" is wrong: the requirement is
  at least one in the whole analysis. The correction text ("Every fragment must
  cite...") is stricter than the check.
- Severity: likely-later. Confidence: confirmed by reading.

### F5. 6b9f02660: the new identity refusals have no negative controls
- `cascade_decision.clj:1532-1557` adds three refusals on the retained record:
  carried digest ≠ retained digest, re-digest mismatch, and validation failure.
  No test reaches any of them (grep finds no `:post-selector-retained-identity`
  or `:post-selector-record-redigest` in test/). The commit rewrote the old
  negative test `identity-mismatch-is-a-typed-machine-error` into a positive
  one. The "existing substitution, re-digest and validation controls" in the TN
  belong to `selection_cascade_identity` (pre-selector bind and the fallback),
  not to this seam.
- Bad case: an entry whose `:canonical-cascade` has a tampered `:structural`
  but an unchanged `:digest`. That should refuse at the re-digest step, but no
  test would notice if the check were deleted.
- The fallback (`retained` nil, so reconstruct via `bind-entry`) remains and
  rebuilds the identity in a way click 2 showed differs. It is reached whenever
  family selection does not attach the record (`family_selection.clj:346-358`
  attaches only when `bound`). The live route sets `bound` through
  `:require-occurrence-squares?` (`cascade_decision.clj:1258-1262`), so the
  live click is protected. But nothing asserts that a production entry never
  takes the fallback.
- Severity: minor for the next click (live path attaches the record). Confidence:
  confirmed by reading.

### F6. recorded_tick_evaluation tests a record shape production does not write
- `recorded_tick_evaluation.clj:26` reads `(:trace-written? production-close-record)`.
  The production run record writes `:traceWritten`
  (`full_loop_runner.clj:1106`; the click-5 run record has `:traceWritten false`),
  and so does `scripts/futon2/run_tick_once.clj:231`. The tests
  (`recorded_tick_evaluation_test.clj`) build `{:run/id .. :trace-written? true}`
  by hand. A real production close record passed in would always give
  `:production-trace-not-written`. The error is on the refusing side, and this
  is recorded-flight only, not the live path.
- Severity: minor (replay only). Confidence: confirmed by grep.

### F7. Smaller test-adequacy notes
- `recorded_flight_test.clj:60-65`, `canonical-cascade-uses-the-bound-selection-certificate`,
  greps the source file for a keyword. It passes if the keyword appears in a
  comment and tests no behaviour.
- ee7275ffc: the regression asserts membership in `cohort/outcome-kinds`, which
  is the consumer's own set, so the test pins the right thing. The old test it
  replaced pinned `{:outcome :refused}`, a value the cohort ledger rejects:
  a test asserting a structure production could not close on. `outcome-from`
  (`full_loop_runner.clj:4334-4342`) still passes any unlisted `:outcome`
  through unchanged. I found no current thrower with an outcome outside the
  set, but no test enumerates them.
- 48e8ae2d0: the Python `--class-emission-control` feeds `class_emission_result`
  a hand-made dict, so it tests the Python predicate, not the scorer. The serving
  probe (`cascade_shape_g.clj:390-417`) scores a canned one-node cascade. The
  Clojure negative control (`loop_node_exercise_test`, singleton test with
  `consumed-in-g? false`) does discriminate. Click 2 then crossed selection
  live, so this is adequate in practice. The two-candidate neighbour (one
  candidate lacking `:class-emission`, caught by the count check at
  `loop_node_exercise.clj:97`) is untested.

## Warrants named in the TN

All eight exist in the registry. Each recorded `revision` equals the commit it
was named for, and test/assertion counts match the TN's figures:

| warrant (prefix) | namespace | revision | tests/assertions |
|---|---|---|---|
| 5b709316 | wm.reading-refusal-terminal-test | ee7275ffc | 3/10 |
| 05fc959f | wm.cascade-runner-seam-test | 6b9f02660 | 5/55 |
| 9c973ae9 | wm.selection-cascade-identity-test | 6b9f02660 | 6/47 (sum 11/102 as TN) |
| 295b34c4 | selected-target-reading-test | 0a45789b8 | (TN 15/62) |
| 31ad9e60 | handoff-contract-test | c70495fc9 | 7/39 |
| 3d50d05d | occurrence-square-authoring-test | c70495fc9 | 5/14 |
| 11974e9f | selected-target-reading-test | c70495fc9 | 15/62 |
| 412fdce4 | full-loop-runtime-test | c70495fc9 | 16/50 (sum 43/165 as TN) |

- `check` now refuses all eight (`:stale-sha` / `:environment-mismatch`, with
  committed drift). That is expected, because the code has moved since.
- No warrant exists at revision 03c8830af, 3037b082f, 48e8ae2d0, 74c35e797,
  311689b8b, ccf6d30d8 or 0975c7df7. The TN names none for 03c8830af/3037b082f
  (it reports test counts only). Under the registry note this means no record
  was minted (a dirty tree), not that the work is invalid. But it means the
  decoder and correction code the next click runs has no checkable record.
- `GET /api/alpha/evidence/<entry-id>` returns `evidence-not-found` for all
  eight, although `check` finds them. The mint verdicts could only be read
  from the sqlite index. That is a tooling gap: the documented lookup does not
  work for these.

## Not done
- Did not trace the ordinary build/review/close code for a cascade entry (F3)
  to a specific refusal line. Another reviewer's area covers that path.
- Did not read 311689b8b/74c35e797 beyond confirming that
  `dispatch-frontier`/`recording-pattern-executor` are used only by
  `recorded_flight.clj`.
- Did not run any test namespace.

## Readiness verdict for my area
Not ready. The week's repairs fix the exact failing case, but their tests use
hand-made fixtures, not the saved artifacts. The click-3 fix, checked against
the click-3 reply it was meant to fix, would have refused it, and click 4 paid
for that. The same pattern continues. The shared decoder still refuses the
next-nearest reply shapes (a second fence in the completion note, prose before
a bare object) and no test pins them. 3037b082f silently removed offset
diagnostics from the commonest correction case (F1), a one-line inversion that
no test checks. Downstream of reading, the cascade path from `:cascade-ready`
through D4 and the ordinary close has never run anywhere: the tests stop at a
stubbed author dispatch, and the replay stops before close by design. Before
the next click I would want: F1 inverted back with a test on the feedback
text; regressions that load the two saved Zai replies byte-for-byte; and at
least one serving-JVM replay that takes a cascade entry past author dispatch.
