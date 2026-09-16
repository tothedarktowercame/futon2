# WM-08-09-review-1 — independent source acceptance assessment

Assessment author: codex-3. Reviewer: claude-3 (pending).
This is source/test assessment, not deployment, serving activation or ordinary-run evidence.
Git attributes the three commits to Joseph Corneli, with Claude co-author trailers.
Attribution to claude-20 is from the dispatch/TODO, not inferred from Git identity.

## 706ca3e8 — REJECT

Required: legacy bootstrap must not reset established state merely because
history is missing or corrupt. The exact diff is retained as 706ca3e8.diff.
Current receipt_construction.clj has no subsequent commits after 983d4c49;
that later order change does not repair the legacy classification.

`same-target-close` labels a candidate legacy solely when its judgment lacks
`:receipted-construction`. There is no positive legacy-version or epoch check.
This branch runs before manifest verification. Removing that key from a modern,
previously valid construction therefore bypasses its surviving close manifest
and returns first-attempt-cascade with no admissions. Recording hashes of the
now-damaged files is provenance of the reset, not evidence authorizing it.

A missing construction or missing `:cascade` excludes the record altogether;
a missing close excludes the attempt at discovery. Empty candidates become
`:first-attempt-no-admissions`. These discovery weaknesses predate this fix;
the new legacy branch additionally converts a deleted modern carrier into a
successful legacy reset. Truncated EDN and altered bytes in the validated modern
branch do refuse. Missing, corrupt-modern and genuinely legacy are therefore
not reliably distinguished.

The committed legacy test covers a genuine old-shaped record and verifies its
reset provenance. The existing modern test covers a mismatched occurrence.
Neither tests the deleted modern carrier or missing-file cases. Additional
receipt-local characterization controls and their outcomes are recorded below; their passing assertions describe defects, not contract acceptance.

## c4130ba5 — ACCEPT (source scope)

Required: interpretation failure remains typed, opens an environmental hold,
and does not become a machine repair or successful construction. The exact
seven-line classifier change and eleven-line test addition are in c4130ba5.diff.
`repair-class-for` and `failure-kind-from` are byte-identical as source sections
between that commit and the current runner (runner-attribution.json). The added
branch maps keyword failures in the interpretation namespace to environmental
hold, after the existing recoverable budget/stall cases. Unrelated build failure
remains machine-failure.

Attribution correction: there are TWO later runner commits, not only cae5f6d9.
24dc6068 wires receipt construction into the interpretation job and avoids a
second legacy construction. cae5f6d9 adds measurement begin/end and an
observe-end! call at the catch/close boundaries. These alter surrounding
execution, but neither changes failure classification. For interpretation
failures before construction, measurement-state remains nil, so observe-end!
is a no-op. Current end-to-end results therefore test the combined current
runner; only the unchanged classifier behavior is credited to c4130ba5.

The receipt-local cases invalid receipt, no relevant pattern, nothing firing,
changed source and genesis-required preserve their exact typed failure kinds;
all return incomplete with environmental-hold obligations, zero legacy
constructor calls and no author/build dispatch. The construction checkpoint is
explicitly `{:sorry {:outcome :incomplete :kind :not-reached-construction}}`,
not a successful construction. The committed positive valid-receipt control
and budget/build classification controls distinguish refusing everything from
the intended behavior. This accepts the fix's bounded failure classification,
not a claim that a hold was later cleared in production.

## 983d4c49 — LIMITED ACCEPTANCE

Accepted: reuse of the same validated predecessor construction's RECORDED order,
without requiring new interpretations of every old pattern. Not established:
that this recorded order is that occurrence's actual enacted order. Exact diff:
983d4c49.diff. Current receipt_construction.clj has no later changes.

`previous!` validates retained occurrence, data root, semantic epoch, start hash,
construction/close manifest agreement and diff digest, and requires the recorded
order to be a vector. These checks reject simple file alteration and occurrence
substitution. The old test checks only a caller-supplied `previous` map. Our new
positive path obtains previous from the validated on-disk fixture and passes it
to construct, preserving [:b :a] in the before arm. The occurrence-mismatch
control changes only run/id, retaining valid timestamps and action digest.

The wrong-order producer control retains [:a :b] with consistent diff and
manifest hashes, while the prior precedence is [:b :a] and its edge is [:a :b].
It is accepted and reused as the next before arm. This is NOT a claim that
hashes fail to detect byte edits: the unsealed byte-edit control is rejected.
It shows that digest-consistent retained values are trusted, with no independent
check of their firing semantics or actual execution. No actual enactment record
is consulted by previous! or construct. The fixture models a producer supplying
a wrong order; it does not claim arbitrary file mutation evades a fixed digest.

The producer explicitly computes acting-order-after by applying documented
interpretation guards/effects to q0, before execution. The record honestly says
`:effect-authority :documented-interpretation-not-measured-success`. Selected
patterns, interpreted effects, and the later author action are not thereby the
same thing. The change preserves those fields and avoids re-simulation, but its
comment 'actually recorded' must not be read as evidence of actual enactment.
The missing case is a separately observed, same-occurrence enacted order joined
to the retained interpretation/policy and distinguished from a swapped order.
This packet does not invent that relation or repair its producer.

## Executed evidence

| Run | Exit | Result |
|---|---:|---|
| interpretation-job-test | 0 | 10 tests, 184 assertions, no failures/errors |
| receipt-construction-test | 0 | 7 tests, 31 assertions, no failures/errors |
| review-probe | 0 | 3 tests, 48 assertions, no failures/errors; includes characterization of the defects |
| clj-kondo, final probe | 0 | No errors/warnings |
| check-parens, final probe | 0 | OK |
| Python run.py syntax | 0 | PASS |

Each namespace ran in its own tooling JVM against canonical source. The probe
adds only the receipt directory and test fixtures through a `:wm-review`
classpath alias; it runs only review-probe's three tests. It reuses the existing
hermetic repair/trace fixtures and stubbed interpreter/author ports. The
`run-opportunity!` calls are fixture executions, not serving clicks. The
construction controls create isolated temporary histories and remove them.
No shared-JVM reload, Lean build, full suite or production source/test edit
was performed.

`probe-results.edn` retains concrete outcomes. Key observations:

| Mutation of a previously accepted modern predecessor | Observed result |
|---|---|
| None | Carried admissions and recorded order |
| Delete 003-construction.edn | Accepted first-attempt-no-admissions |
| Delete 007-closed.edn | Accepted first-attempt-no-admissions |
| Remove judgment :cascade | Accepted first-attempt-no-admissions |
| Remove judgment :receipted-construction, leave original close manifest | Accepted legacy-predecessor-no-ruled-admissions |
| Truncate construction EDN | Parse exception; no reset |
| Change acting order without updating hashes | previous-manifest-source-mismatch |
| Change only close occurrence run/id | previous-occurrence-mismatch |
| Producer supplies reversed order and matching diff/manifest hashes | Accepted; reused as before arm despite contrary retained precedence |

Positive legacy coverage is the existing `legacy-predecessor-resets-with-recorded-provenance`
test. Positive modern/order coverage uses the real history fixture and current
reader. The final interpretation probes verify the exact typed failure, an
environmental hold, incomplete outcome, explicit not-reached-construction
checkpoint, and absence of author/build dispatch for five scenarios.

Development diagnostics are retained separately. Initial probe execution exited
1 with six incorrect test expectations: five expected nil instead of the
explicit not-reached checkpoint, and the initial occurrence substitution used
a future action timestamp, so it correctly failed temporal validation before
reaching the identity mismatch. The corrected probe changes only run/id and
checks the typed not-reached checkpoint. Initial lint caught an unmatched paren;
final lint/parens pass. These probe corrections did not change source or the
reviewed contracts. `test-thread-dump.txt` was a read-only diagnostic of the
longer interpretation namespace run; it showed fixture trip-report pretty
printing, and that process subsequently completed successfully.

## Remaining WM-08 / WM-09 obligations

This assessment supplies independent bounded source evidence for c4130ba5 and
only the recorded-value aspect of 983d4c49. It does not discharge the rejected
history boundary or the missing actual-enactment join.

- **WM-08 C1–C3:** ordinary problem-derived query/retrieval and complete pinned
  context, actual source reading and task-bound interpretations still need
  ordinary-use evidence. These fixtures do not acquire it.
- **WM-08 C4:** independent F2 expectations and independent F4 designation;
  empty designation must remain explicitly vacuous. The qualifying nonvacuity
  decision remains unresolved, not settled by this review.
- **WM-08 C5–C6:** opaque-interface refinement/conformance, source/revision-bound
  running finder evidence, downstream useful use, and retained search findings
  for empty/unhelpful results. Serving activation and an ordinary receipt-mode
  run remain outstanding.
- **WM-09 C1–C3:** a trustworthy before-state/admission history boundary, all four
  independently attributed inputs, authored-relation preservation/reachability,
  and nonvacuous ruled no-bootstrap/O3 checks at the actual operation. The
  modern-history reset cases above remain unresolved.
- **WM-09 C4–C5:** independently grounded same-occurrence precedence/acting
  consequences (or appropriate score evidence), exact delivered policy identity,
  and distinctions between selection, interpreted simulation and enactment.
  Canonical G/full score-dependent checks depend on WM-10/12; this review supplies
  none of them.
- **WM-09 C6:** connect the replacement to the legacy organise obligation (its
  old sorry is not closed here), plus serving activation, an ordinary constructed
  family and a qualifying click receipt with nonvacuous law evidence. Historical
  diff acceptance is not evidence of deployment.

No checklist or registry changes. No successor work chosen or repair attempted.

## Identities and exact commands

Source snapshot HEAD: `4fec38948a3714158e4d3efac9bf1e87096f6039`. Source hashes remained unchanged at review completion.

Dispatch SHA-256: `0ff615f9fba6021eaf5b04c5c52fe4df1e74188aa1109121411aae131af2c1ee`.

Deadline plan SHA-256: `ee2e212897fa8408be8c9050d931e63561b91cff3cd6c2c5df9de23b0d666c7d`.

- Reviewed `c4130ba5592f37ccb55936ce3dd243e0161cbb55`; full exact diff retained as `c4130ba5.diff`.
- Reviewed `706ca3e83355d215a4b54464c16efa547768f31e`; full exact diff retained as `706ca3e8.diff`.
- Reviewed `983d4c49b088a869d9b31ef209cee029bc50ba70`; full exact diff retained as `983d4c49.diff`.

| Current file | SHA-256 |
|---|---|
| src/futon2/aif/full_loop_runner.clj | `c1c52aaa28cb23c0edaf645a292056caadf56cc33431fd429530f29da720e353` |
| src/futon2/aif/receipt_construction.clj | `1b4f766c5b5b4ce540a44c2c22f345274d2a8fb21f994c5c9091c2a3cfe60338` |
| test/futon2/aif/interpretation_job_test.clj | `af0bdb37c391468b215ec2a7d8d78f65fcd99926c0bb04dd25bd6540fa73f658` |
| test/futon2/aif/receipt_construction_test.clj | `72f4876f6ed8067a68f39d749b21cdd28f7ac8581231a5e109edbd4fe7382271` |

Raw stdout/stderr and argument vectors are retained by run.py without pipelines.

- `attempt1-kondo`: exit 0; cwd `/home/joe/code/futon2`; argv `["clj-kondo", "--lint", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/review_probe.clj"]`.
- `attempt1-parens`: exit 0; cwd `/home/joe/code/futon2`; argv `["emacs", "-Q", "--batch", "-l", "/home/joe/code/futon4/dev/check-parens.el", "--eval", "(arxana-check-parens-cli)", "--", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/review_probe.clj"]`.
- `attempt1-probes`: exit 1; cwd `/home/joe/code/futon2`; argv `["clojure", "-Sdeps", "{:aliases {:wm-review {:extra-paths [\"test\" \"holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1\"]}}}", "-M:wm-review", "-m", "review-probe", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/probe-results.edn"]`.
- `construction-tests`: exit 0; cwd `/home/joe/code/futon2`; argv `["clojure", "-X:test", ":nses", "[futon2.aif.receipt-construction-test]"]`.
- `interpretation-tests`: exit 0; cwd `/home/joe/code/futon2`; argv `["clojure", "-X:test", ":nses", "[futon2.aif.interpretation-job-test]"]`.
- `kondo-attempt1`: exit 3; cwd `/home/joe/code/futon2`; argv `["clj-kondo", "--lint", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/review_probe.clj"]`.
- `kondo`: exit 0; cwd `/home/joe/code/futon2`; argv `["clj-kondo", "--lint", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/review_probe.clj"]`.
- `parens`: exit 0; cwd `/home/joe/code/futon2`; argv `["emacs", "-Q", "--batch", "-l", "/home/joe/code/futon4/dev/check-parens.el", "--eval", "(arxana-check-parens-cli)", "--", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/review_probe.clj"]`.
- `probes`: exit 0; cwd `/home/joe/code/futon2`; argv `["clojure", "-Sdeps", "{:aliases {:wm-review {:extra-paths [\"test\" \"holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1\"]}}}", "-M:wm-review", "-m", "review-probe", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/probe-results.edn"]`.
- `python-syntax`: exit 0; cwd `/home/joe/code`; argv `["python3", "-c", "from pathlib import Path; p=Path(\"futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/run.py\"); compile(p.read_bytes(),str(p),\"exec\"); print(\"PASS run.py\")"]`.
