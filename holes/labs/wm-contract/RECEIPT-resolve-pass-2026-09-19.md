# Resolve pass — 2026-09-19

Scoped tally: **3 resolved; 3 left awaiting validation; 0 typed refusals**. Three authorized `resolve!` calls, no other store writes, no source changes, no clicks or reloads.

## Contract and evidence

Read `src/futon2/aif/repair_obligation.clj:1329` (`resolve!`). Machine findings require an existing implementation, an attempt distinct from both the original and implementation attempts, artifact evidence, reviewer/job, resolved/dial-moved witness and `:validation {:production-shaped? true}`. Those booleans do not establish their own truth: this pass supplies a bounded production-path attestation and exact retained evidence. All six findings declare distinct repair commit, independent review, grounded repair and distinct production-shaped successor. Their reviewed implementations supply the prior repair limb; this pass assesses the successor limb.

Both run EDNs were read completely (70,996 and 71,360 characters), including their decision, source identity, route and provenance. Selection, dispatch, build and closed artifacts were also read from:

- **R4:** `data/wm-runs/tick-run-record-2026-09-19-1789848916.edn`; **A4:** `data/wm-full-loop-machinery-61/wm-contract-machinery-61-v1/attempt-002/`.
- **R5:** `data/wm-runs/tick-run-record-2026-09-19-1789849189.edn`; **A5:** `data/wm-full-loop-machinery-62/wm-contract-machinery-62-v1/attempt-001/`.

Material correction to `RECEIPT-validation-clicks-4-5-2026-09-19.md`: **both** routes reach FULL_LOOP_CLOSE with `:via :incomplete`. Both `007-closed.edn` judgments say `:outcome :incomplete`, `:grounded? false`, witness nil, agent-turns 0, and no authored commit. The receipt's click-4 “complete” wording is not evidence of grounded work. Both `004-dispatch.edn` payloads explicitly say `:not-reached-dispatch`. The reported `{:valid 1}` concerns run validity; it does not imply successful binding or implementation discharge.

Nevertheless, both `002-selection.edn` artifacts retain the ordinary controller decision and selected mission `:C1`; both closed records successfully retain duration/resource-use and a typed incomplete terminal. This supplies bounded production validation of selection completion and the close writer's repaired required-field path. It does not re-inject the historical initialization exception or claim a grounded selected mission. R4's authority-qualified execution identity is `ea1-34838db302be9b75a18540c560b2ecf46e768a8c87bf64afff4e6289b67c5907--attempt-002`, distinct from all three resolved findings' original and implementation attempts, and later than those implementations.

## Per-finding outcomes

| Finding | Outcome | Evidence and reason |
|---|---|---|
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception` | resolved | Reviewed implementation `06310fcb16d74c0432125f643dc203983b404576`; A4/007-closed.edn accepted a typed terminal with both historically missing fields `:duration-ms` and `:resource-use`. R4 independently retains FULL_LOOP_CLOSE. |
| `repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed` | resolved | Same reviewed close repair; A4 retained the terminal instead of losing it in the historical invalid-close/initialization collapse. Attestation is production terminal retention, not a deliberately re-thrown initialization exception. |
| `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure` | resolved | Reviewed package `1fcef69d127bc9c0f7205f9cc2824df039e981ad`; A4/002-selection.edn retains controller decision and selected mission; the run progresses beyond the renderer/selection NPE boundary and closes. R5 corroborates. |
| `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch` | left-awaiting | Implementation `190568dd4296f56189a7e61b1e427eaab381d7af` repairs commit identity binding. A4/A5 dispatch and build are not reached; no author commit was bound. Neither run validates this clause. No resolve! attempted. |
| `repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch` | left-awaiting | Implementation `1344347a269620ee207ba5b79736c2c7ddb66d96` repairs source-byte identity and retained runner-source checks. Both records report `:runner/source-check :current`, validating that mitigation, but neither reaches the author binding which originally failed. Partial evidence is not a binding-success witness. No resolve! attempted. |
| `repair-ea1-b0eeafa0e59b4dc0dd9e0abe1cbbed0e687c5827b3ade8320c98c7f82a79032b--attempt-001-machine-repair-lacks-grounded-review-evidence` | left-awaiting | Implementation `0476071e5378675124a279491d07c6ad1f2de37a` adds grounded review evidence/artifact binding retention to schema-3 implementation records (opened its exact diff). Neither run reaches a production repair implementation write or reviewed grounded successor. No resolve! attempted. |

## Store results and authority

`open-obligations` before: **43 total = 18 open + 25 awaiting-validation; runner-eligible 4**. After: **40 total = 18 open + 22 awaiting-validation; runner-eligible 4**. These are full-store counts; the packet considered only the six named findings.

Production validation reviewer is codex-2 in this authorized pass, job `invoke-1789849443626-22521-c02d5238`. This does not misattribute review of these later runs to the earlier independent implementation reviewers: their reviewer/job are retained separately under `:validation`. Witness scope is explicitly production-selection-completed or production-close-terminal-retained, with the incomplete-run limit stated in each input.

Exact inputs, accepted resolution copies, six outcomes, stdout/stderr, counts and evidence SHA-256s are committed under `runs/resolve-pass-2026-09-19/`. Authoritative accepted records are `data/wm-repair-obligations/resolutions/<full-finding-id>.edn` for the three resolved rows. No contract refused, so there is no refusal payload to reproduce. Remaining three findings retain their implementation records and :awaiting-validation status.
