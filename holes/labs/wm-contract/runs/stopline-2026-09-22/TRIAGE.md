# Stop-the-line repair triage — 2026-09-22

Eight findings have corresponding code fixes on main with regression evidence; two are still unjustified evidence deposits, for which no correcting implementation was established. **All ten remain open.** No status-changing verb was called. A passing regression is not a production-shaped successor validation and does not, by itself, authorize resolution.

## Scope and evidence authority

Read-only discovery against futon2 main `3134b61f` in worktree `futon2-stopline-triage-2026-09-22`, branch `fix/stopline-triage-2026-09-22`; futon3c master observed at `24d31c1604f4983094bfac77bdd682af2b6cc047`. The identified fixes below are in futon2 main's ancestry; no separate futon3c fix is claimed. Disk ancestry says nothing about the code loaded in the serving JVM. No serving-JVM evaluation, clicks, obligation mutations, or production code changes were performed.

Finding authority: `/home/joe/code/futon2/data/wm-repair-obligations/findings/<full-id>.edn`, using `:failure-kind`, `:stage`, `:target`, `:error`, `:failure-data`, `:opened-at`, and retained occurrence/backtrace fields. The full ids below identify the exact files. The corresponding implementation, verification, resolution and dismissal records were sought; no per-finding closure evidence was established for these ten. The target implementation records relevant to rows 1–2 are discussed separately below.

Read first: [repair-obligation verbs](../../../../../src/futon2/aif/repair_obligation.clj), [September 19 classification](../../CLASSIFY-repair-queue-2026-09-19.md), and [selection-precedence ruling](../../RULING-selection-precedence-2026-09-19.md). The September 19 permission to continue clicking is superseded by Joe's September 22 stop-the-line instruction.

## Findings and proposed disposition

“Resolution route” below means `record-implementation!` with independent review and admitted grounded witness, then `resolve!` / `successor-resolution!` with a distinct production-shaped validation and required evidence. It is a proposed route, **not a ready-to-submit resolution**. Retained historical evidence may qualify through `record-historical-verification!` and `commit-historical-resolution!`; it must satisfy those gates. This packet neither fabricates successor evidence nor proposes another WM click.

| # / full obligation id | What failed and cause | Fix on main and regression evidence | Honest closing verb / remaining work |
|---|---|---|---|
| 1. `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged` | September 14 close: “Limb evidence refused”, `:revision-unchanged`, path `[:revision-pair]`. The before/after claim supplied identical content; the September 19 classification identifies an invalid deposit, not a validator defect. | **None established for this obligation.** `limb-evidence-test/standing-and-revision-refusals` preserves the correct rejection. Target implementation `1344347a269620ee207ba5b79736c2c7ddb66d96` is not proof that this refusal was repaired. | **FIX NEEDED:** produce a valid evidence deposit with genuinely distinct before/after bytes, or omit the optional revision claim, and pin acceptance while preserving identical-byte rejection. Resolution route afterward; current supersession evidence fails the distinct-attempt requirement. |
| 2. `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn` | September 15 close: “Attempt evidence must contain one EDN form”, `:evidence-not-single-edn`; source `data/wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-003/evidence/derived.stderr`. An unreferenced companion was enumerated as a standalone EDN record. | **None established for this obligation.** September 19 classification says the stray-file rejection is correct. Target implementation `76c7088b11d4e8d97952ace8cf67eef48a5c5289` does not establish correction of the evidence deposit. | **FIX NEEDED:** reference the companion through the supported receipt contract or exclude it from the evidence deposit, with acceptance evidence that leaves stray-file rejection intact. Resolution route afterward; same-attempt target implementation cannot justify dismissal. |
| 3. `repair-occ-d36b1b271427fc21dc365b44a3297d65d6761f2d667850154ef578658f27205f` | September 20 selection: `:untyped-failure`, “incompatible preference schedules”; `:kind :incommensurable-family`, generated default `:every-step` versus declared `:terminal` with uniform elsewhere. Generated mission-hole targets lacked the source family's declared schedule/scales. | `a38becc9ae6d5477a62da24f6753556395aa6271`; `mission-hole-wants-test/generated-targets-adopt-the-family-schedule-and-scales`. Fresh main pass and parent-source failures below. | **Resolution route citing a38becc9**, subject to per-obligation implementation/review and qualifying successor evidence. Do not weaken the incommensurability refusal. |
| 4. `repair-occ-5ef53a512ad80d55f4bde136724d6ad13e6293cc548a092c90ab0317674753b9` | September 20 selection, run `2026-09-20-1789940260`: same exact schedule mismatch and failure data as row 3. | Same `a38becc9` fix and schedule regression. | **Resolution route citing a38becc9**, with this occurrence bound separately to its validation. Duplicate cause alone is not a proof-carrying dismissal. |
| 5. `repair-occ-652aa5f8e36ebcddd88ac69aa13a65e6c3f260cb1e2d056474ee9f46a5006546` | September 20 selection, run `2026-09-20-1789948650`: same mismatch. Recorded clean disk SHA `94628159878b026e41b99452fce0ccfec710bbc1` already contains a38becc9; disk ancestry alone cannot establish the executed function version or input provenance. | Same `a38becc9` and regression fix the reproduced producer defect. **Why this occurrence persisted after that disk commit is not proven**; a stale loaded implementation is a hypothesis, not a finding. | **Resolution route citing a38becc9 only with execution-bound successor evidence** showing generated schedules at this boundary. If the mismatch persists under that code, a further fix is needed; do not call the historical occurrence validated from ancestry alone. |
| 6. `repair-occ-e9eec70e60b38c5f3d0d073dc33832511f8b88a75cb22d4de6dad1a7d49bc4ff` | September 21 initialization, run `2026-09-21-1789948972`: “Invalid token: :hole/2f9b03b16170”; failure-data nil. Hash-derived keyword names could start with a digit: printable but not EDN-readable. | `29fb2a83130877f3e8926f72ad42ebb3b3ab4b18` prefixes names with `h`; `mission-hole-wants-test/want-tokens-survive-a-print-read-round-trip`. Parent reproduces this exact token error. | **Resolution route citing 29fb2a83.** Fix prevents new malformed tokens; it does not migrate already-written malformed records or prove they can now be read. |
| 7. `repair-occ-88bb128331ef06bb2f9b50021e70d3f42d2d33504fcf22d8736b43a044c49d2b` | September 21 close, same run as row 6, target string `":C0"`: `:close-exception`, same invalid token; failure-data `{}`. Same serialization defect reached the close boundary. | Same `29fb2a83` and round-trip regression. The target-id correction is a separate issue and is not the cause of this exception. | **Resolution route citing 29fb2a83**, requiring close-boundary validation. Same run/error is insufficient for `dismiss-echo!` without that verb's retained source/disposition witnesses. |
| 8. `repair-occ-f22800c6792de6edc80e03cb44c235cbb38a7e4e75c34b51f5f88c7c8672365a` | September 21 selection, run `2026-09-21-1789951020`: 21 empty cascades pooled under nil action, mass 0.785275, beat acting keys 0.179031 and 0.035694. Chosen empty `:C0` / M-dionysus-winddown, posterior 0.071389; best individual policy `:C1` / M-expressions-of-interest, 0.143225. Recorded marginal also overwrote duplicate keys instead of summing. | `d168d34890d65d2f5c849ec2d734194ed98f1047` excludes empty actions and sums marginals; independent decision-gate counterpart `8f60f819104d66043ac015877fa146612883f3e3`; admission refinement `c155d690cc1b27f42094ea55a2159bed82c136d7`. Selection tests and historical warrants below. | **Resolution route citing d168d348 + 8f60f819**, preserving real typed no-ops and all-empty refusal. No dismissal on grounds of age or later selection success alone. |
| 9. `repair-occ-0576180e03115d74ce0a32eff59df655649d732e8cdc10f12a86f98b15f51879` | September 21 construction, run `2026-09-21-1789952178`, selected `:C1` / M-expressions-of-interest. `:fold-output-invalid`: three policy holes each lacked `:free`, `:why`, `:obligation/id` (nine findings). Raw classical-fold remainder shape did not meet the production contract. | `c91261fdbc7b80e903d3203ca53cc865291cae9f`, `ground-cascade-policy-holes`; `cascade-fold-repair-test/recorded-expressions-through-production-fold` exercises the retained selection through the real dependency and production validator. Fresh main pass; raw unadapted output demonstrably invalid. Independent review/warrant below. | **Resolution route citing c91261fd** for this schema defect. Contract-valid incomplete output is not successful construction or discharge; do not claim wanted tokens completed. |
| 10. `repair-occ-6c6ecaf858aff5c7f9aeef1b92ad205e7ed31ed29882e89fa52f2724d3d039c8` | September 21 close, run `2026-09-21-1790033693`, M-aif-policy-conditioned-eig: `:explanation-invalid`, path `[:standing-decision :explanation]`. Reviewer instructions listed keys without an exact conforming record; the deposited explanation was nested rather than a sufficiently long top-level string. | `6d45e8b7bb001a7634b6e4a04375d12b9adc6813` supplies a literal standing-decision template with exact target and explanation shape; `full-loop-runner-test/reviewer-standing-template-passes-the-close-validator`. Fresh main pass and parent failure below. | **Resolution route citing 6d45e8b7**, with actual corrected reviewer deposit and close validation; a conforming template does not guarantee a model's future compliance. |

## Why the first two cannot simply be dismissed as superseded

Row 1 targets `repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch`. Its retained implementation is dated `2026-09-15T00:10:09.232045124Z`, uses commit `1344347a269620ee207ba5b79736c2c7ddb66d96`, and names implementation attempt `attempt-001`. That aliases the failing external attempt ending `--attempt-001`. Row 2 targets `repair-initialization-a3e2319f-9562-4182-b651-54f409b90e39-initialization-failed`; its implementation is dated `2026-09-15T02:27:53.785596225Z`, uses `76c7088b11d4e8d97952ace8cf67eef48a5c5289`, and names the exact failed `ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003` attempt. Both target records are awaiting validation.

`dismiss-superseded-attempt!` explicitly prefers the actual implementation over a later resolution and rejects both exact equality and short-attempt suffix aliases with `:self-implementation`. These retained records therefore cannot justify that dismissal. No verb was invoked to test this: this conclusion follows directly from its predicates and the stored fields. No proof of environmental clearance, fixture pollution, or unexecuted attempts was established either. Evidence correction need not mean a production code patch; the guard should remain strict.

## Regression evidence and its limits

Fresh probes ran in standalone Clojure processes in the triage worktree, against existing test vars only. Parent comparisons loaded an extracted parent **producer source** in that isolated process after requiring current test code. This is a focused counterfactual, not a checkout-wide historical build, a serving-JVM load, a full namespace gate, or a production validation. No test or script was added to the repository.

| Probe | Fresh current-main counters | Counterfactual result |
|---|---|---|
| Schedule adoption plus token round-trip | 2 tests, 11 passes, 0 failures, 0 errors | Schedule producer at a38becc9 parent: 1 test, 1 pass, 2 failures. Token producer at 29fb2a83 parent: 1 test, 2 passes, 6 errors. |
| Empty-cascade selection, duplicate marginal, recorded expressions fold | 3 tests, 35 passes, 0 failures, 0 errors | Policy producer at d168d348 parent, first two tests: 2 tests, 2 passes, 6 failures, 1 error. Fold test itself checks the malformed raw dependency output before the corrected path. |
| Reviewer standing template | 1 test, 5 passes, 0 failures, 0 errors | Runner at 6d45e8b7 parent: 1 test, 2 passes, 0 failures, 1 error. The error is the new six-argument contract missing on the parent, not itself an explanation-validator rejection. A separate read-only call using the parent's supported five-argument signature returned `:parent-has-standing-template false`, confirming the actual missing template independently of arity. The positive test parses and validates the filled template through the real close validator. |

Decisive pre-fix output:

```text
FAIL generated-targets-adopt-the-family-schedule-and-scales (mission_hole_wants_test.clj:74)
expected: (= terminal-schedule (get-in merged [:preference-schedules "M-probe"]))
actual: terminal schedule versus nil
FAIL at line 77: declared preference scales versus nil

ERROR want-tokens-survive-a-print-read-round-trip
expected: (= tok (edn/read-string (pr-str tok)))
actual: java.lang.RuntimeException: Invalid token: :hole/2f9b03b16170

FAIL empty-cascades-cannot-outvote-an-acting-one-by-count (selection_certificate_test.clj:149)
expected: (= (pattern :pattern/do-the-thing "M-real") (:chosen-action decision))
actual: {:id :pattern/do-the-thing, :target "M-real"} versus nil
FAIL recorded-marginal-sums-duplicate-keys-rather-than-overwriting (line 183)
expected: (== (get weights shared) shared-mass)
actual: (not (== 0.33333333333333354 0.6666666666666671))

ERROR reviewer-standing-template-passes-the-close-validator
actual: clojure.lang.ArityException: Wrong number of args (6) passed to:
futon2.aif.full-loop-runner/evidence-deposit-instruction
```

Reproduction pattern (standalone CLI only; replace namespace, vars, source and parent with the table entries):

```sh
git show 'a38becc9^:src/futon2/aif/mission_hole_wants.clj' > /tmp/triage-before-schedule.clj
clojure -M:test -e "(require 'futon2.aif.mission-hole-wants-test)
(binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)]
  (clojure.test/test-vars [(ns-resolve 'futon2.aif.mission-hole-wants-test
    'generated-targets-adopt-the-family-schedule-and-scales)])
  (prn :main @clojure.test/*report-counters*))
(load-file \"/tmp/triage-before-schedule.clj\")
(binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)]
  (clojure.test/test-vars [(ns-resolve 'futon2.aif.mission-hole-wants-test
    'generated-targets-adopt-the-family-schedule-and-scales)])
  (prn :parent @clojure.test/*report-counters*))"
```

The other exact vars are named in the rows above; policy vars are `empty-cascades-cannot-outvote-an-acting-one-by-count` and `recorded-marginal-sums-duplicate-keys-rather-than-overwriting` in `futon2.aif.selection-certificate-test`. Their parent source is `src/futon2/aif/policy.clj`; reviewer parent source is `src/futon2/aif/full_loop_runner.clj`; token parent source is `src/futon2/aif/mission_hole_wants.clj`. Clojure test counters, not process exit zero from `-e`, determine the result.

Additional retained evidence:

- [Nonempty cascade landing](../nonempty-cascades-2026-09-21/LANDING.md) records canonical selection warrant `test-registry-fc4d8cecd6dfadc0b20e7abcc7611fa9b3c9654d79f247e10a66843b754f8333` (9 tests / 109 assertions) and decision-gate warrant `test-registry-f08d640acf6072bbf8944a265a7265e8f40e82893218db0b2401de4fa2d99f5f` (13 / 91). `the-2026-09-21-recapture-is-exactly-the-pooled-nil-exclusion` also compares retained pre-fix decisions with the corrected selector.
- [Cascade fold report](../cascade-fold-repair-2026-09-20/REPORT.md) and its `post-commit-receipt.json` record independent reviewer claude-4 approval, 3 tests / 58 assertions, and warrant `test-registry-2a63f8df59e059e06b80306347583d918e03fd85b4b52e975218386a7b1c78f0`. That warrant was pinned to the report's worktree; canonical checking refused an environment mismatch. The fresh expressions test here passes 26 assertions including the raw malformed dependency and corrected contract; it explicitly leaves zero boxes and incomplete discharge.

These are historical warrants read from retained receipts, not newly verified warrants for today's head. No registry write was performed. The fresh targeted counterfactual probes supply the additional failure-without-fix evidence required for this triage.

## Closure constraints for the owner

All ten are machine-failure obligations, not incomplete-recoverable obligations eligible for `supersede!`. `record-implementation!` only moves them to awaiting validation. `resolve!` requires independently reviewed grounded repair evidence and a validation attempt distinct from both failure and implementation, with `:validation {:production-shaped? true}`. Existing later runs may supply evidence, but their relationship, artifacts, source authority and independent review must be admitted through the verbs; this report does not establish those bindings. In particular row 5 needs execution evidence because its recorded disk revision already contained the candidate fix.

No row presently has sufficient proof for an unconditional dismissal or resolution. The next packets should correct rows 1–2's evidence and assemble the per-occurrence review/validation records for the eight code-fixed defects. If the required successor evidence is not already retained, surface the conflict with the no-click instruction to the owner rather than pretending a unit test is a production successor.

Document-only gate: `git diff --check` and `git diff --cached --check` passed with no output before commit. No Clojure source or scripts changed, so clj-kondo/check-parens are not applicable to this packet.
