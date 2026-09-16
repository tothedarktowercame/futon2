# WM-04-resolver-1: claude-3 review

**Reviewer verdict: ACCEPT** for futon2 `a621c7c9` (author codex-4). The strict authority resolver joins the existing admitted-evidence manifest to the unchanged observation validator, and it refuses what the dispatch requires it to refuse.

This is one acquisition **interface**. It is not an acquired observation, a real label, measured A, coverage, a commission, serving activation, or WM-04 closure, and codex-4 claims none of those.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-04-resolver-1.md` (sha256 `a9c685fd…`), elected by codex-28 in `invoke-1789562930424-21302-dc2e5d01`.
- Author job: `invoke-1789563045839-21303-3e4b9d11`. Handoff: `../handoffs/WM-04-resolver-1-to-codex-4.md` (sha256 `2772172b…`).

## What I checked

1. **Scope.** `a621c7c9` touches only `src/futon2/aif/observation_authority_resolver.clj` (128 lines), its test, a 40-line addition to `categorical_state_observation_test.clj`, and this receipt directory. The validator `categorical_state_observation.clj` (`8d48ecf8…`) and `evidence_manifest.clj` (`6a10f78f…`) are unchanged, with no commits touching either since `4fec3894`. Nothing pushed.
2. **The resolver against the validator's real contract.** `authority-record!` (line 217) calls `(resolver kind ref)`, demands a map, and passes it to `read-pinned-form!`. The resolver returns exactly the validated `{:path :sha256}` pointer and nothing else, so byte, form and digest verification stays in the existing reader.
3. **The rules, read in source:**
   - `build-resolver!`'s input keys are a closed set (`:index-source :manifest :expected :commission/ref :io-opts`), so no candidate observation can reach it.
   - The manifest is re-validated with `validate-manifest` rather than trusted.
   - The index is read through `read-pinned-form!`, has a closed shape, must equal `:expected` exactly, and must name the manifest's digest.
   - Bindings are grouped by `[kind ref]`: an unknown binding refuses with `:authority-not-found`, and more than one refuses with `:ambiguous-authority-binding`.
   - An evidence entry's `:evidence/id` must be admitted, its source must equal the manifest entry's path and hash exactly, and its `:admitted-at` must not be after the cutoff (inclusive).
   - No cutoff is applied to observer or review records, so a late review of frozen eligible bytes is permitted.
   - No default, no path inference, no live search. Identities of commission ref, index, manifest digest, subject, point, scope and provenance are retained.
4. **Tests, re-run by me in isolated processes:** resolver test 8 tests / 56 assertions, categorical observation test 8 / 44, evidence manifest test 4 / 19 — all 0 failures, exit 0. clj-kondo 0 errors and 0 warnings, and check-parens OK, on all three changed files.
5. **codex-4's negatives assert specific refusals**, not just "throws": 25 rows in its receipt, covering every case the dispatch named (wrong kind or ref, absent authority, ambiguous binding, missing manifest entry, altered path or hash, admitted after cutoff, wrong entity or occurrence, candidate-owned authority or review, review of another subject), plus a positive late-review case.

## My own adversarial controls

In `/tmp`, reusing the real fixtures, against canonical source (canonical `src` and `test` untouched):

| Case | Result |
|---|---|
| Baseline indexed fixture | qualifies (setup is sound) |
| Index adds a **second ref aliasing the same admitted evidence** | builds — alias refs are not refused at construction |
| Candidate cites **both alias refs** | refused, `:review-subject-mismatch` |
| Candidate cites **the same ref twice** | refused, `:review-subject-mismatch` |
| Evidence **properly admitted, but recording `entity/other`**, bound by an otherwise valid index for `entity/test-1` | refused, `:evidence-entity-mismatch` |

So a candidate cannot inflate its evidence by repeating or aliasing citations: the independent review binds the exact claim list. Even if it could, `classify-rubric!` reduces assertions to a set, so repetition cannot change the classified status. And an index author cannot launder evidence about a different entity through a correct-looking binding, because the validator checks the claim's own subject.

## Findings (none blocking)

- **F1. Alias refs are allowed.** Two different refs may bind the same admitted evidence. As shown above this is harmless today, because the review freezes the claim list and the rubric uses set semantics. It becomes relevant only if a future consumer counts citations rather than distinct evidence.
- **F2. The index is the trust root for binding context.** The manifest's schema has no entity or occurrence, so the binding of admitted evidence to one exact context rests entirely on the index, which the resolver treats as externally authorized. codex-4 documents this plainly. It is the right division given the unchanged manifest schema, but whoever commissions an index is asserting that binding.
- **F3. `:commission/ref` is presence-checked provenance, not authentication**, as documented. Establishing that a commission is genuine stays with the commissioning caller.

## Limits

- Interface only. No production commission, real independent label, measured A, coverage, runtime caller change, reload, serving activation or click.
- Fixture statuses and authority records are test data.
- WM-04 is **not** checked. Its remaining clauses — independently authorized and blinded observation, predeclared eligibility and sampling, accepted labels, measured A and admitted assembly — are untouched by this packet.
