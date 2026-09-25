# A-CLASS — error rates of the observation class checks C3–C8 against their own bad cases

Author: kimi-4 (E-kimi-task-4, requisition from claude-8, PROOF-2). Written 2026-09-25.

## Question

H-A-CONSUMER-D (futon2 6059ecbc) showed no row of the measured error-rate
ledger (A-S.md) runs any of the five class checks, so the measured rates have
no consumer among the classes. The per-class question left open: **can this
check err about its own class token, and how often?** This packet constructs a
population of locators where the token's truth is known by construction,
verifies each truth by an independent program, and measures each check against
it.

## Tokens (verbatim from resources/wm/observation-contract.edn)

- **C3** (`check-path-exists`): "the named path exists at the pinned sha".
- **C4** (`check-decl-in-file`): "a line of the file at the pinned sha starts
  with the declaration head, and the head is followed by whitespace, :, (, {,
  [ or end of line".
- **C5** (`check-registry-entry`): "the contract bundle at the pinned sha has
  the named contract-id, and every clojure-locus it declares resolves (file
  present with at least that many lines at the locus repo HEAD)".
- **C6** (`check-witness-reference`): "the EDN witness at the resolved commit
  contains repo and sha naming a resolvable commit, and, if entry is supplied,
  that path exists at the referenced commit".
- **C8** (`check-registered-run`): "the test registry holds a warrant for the
  named test namespace whose pinned code-path and test-path shas equal those
  files' shas now, whose postcheck matched, and whose run recorded no failures
  and no errors".

## Method

100 locators, 20 per class, over real repository state: futon2, futon3c and
mathlib4 at pinned shas, the machine-contracts bundles r10–r15, and the live
test registry (198 tagged evidence entries enumerated 2026-09-25; 99 run
records). Exactly half per class are true by construction and half false; of
the false cases at least half are near-misses — right file wrong sha, right
sha wrong repo, stale locus by drift, prefix of a real token, case-flip,
trailing space, warrant pinned to moved content, warrant for a different
namespace, run that recorded failures. Falsifier guard: near-miss false counts
are C3 8/10, C4 9/10, C5 7/10, C6 9/10, C8 9/10 — no class's false arm is
dominated by trivially false cases.

Ground truth is established by `a_class_groundtruth.py`, a second
implementation that never calls the checks: `git ls-tree` for C3 (not
`cat-file -e`), an independent line scanner over `git show` output for C4,
JSON parsing of the bundle plus `git show | wc -l`-equivalent line counts for
C5, regex-level witness parsing plus `git rev-parse`/`ls-tree` for C6, and a
from-scratch registry read for C8 (digest verification of the payload against
its entry id, then SHA-256 of every pinned path's current bytes). Every
derived truth agreed with the construction intent (100/100); had one not, the
case would have been a construction bug, not a check error.

The checks then ran unmodified via `a_class_runner.clj`, and per-class rates
were computed with the A-S arithmetic itself
(`futon2.aif.check-error-rates/measured-rates`: Jeffreys-smoothed rate
(k+½)/(n+1), 95% Wilson interval, min-count 5). Refusal outcomes are reported
but excluded from the rate denominators, as A-S excludes ineligible rows.

C6 witnesses are fixture EDN files under `a-class-fixtures/` (no C6-shaped
witnesses pre-exist in any of the three repositories — searched
2026-09-25); their locators are `:sha "HEAD"`, i.e. this packet's commit. All
referenced commits and entry paths are real repository objects. C8's registry
is the live evidence store, read-only.

## Results

| class | n | true | false | false-pass | false-fail | fp-rate (Jeffreys) | fp Wilson 95% | fn-rate (Jeffreys) | fn Wilson 95% |
|-------|---|------|-------|-----------|-----------|--------------------|---------------|--------------------|---------------|
| C3 | 20 | 10 | 10 | 0 | 0 | 0.0455 | [0, 0.278] | 0.0455 | [0, 0.278] |
| C4 | 20 | 10 | 10 | 0 | 0 | 0.0455 | [0, 0.278] | 0.0455 | [0, 0.278] |
| C5 | 20 | 10 | 10 | 0 | 0 | 0.0455 | [0, 0.278] | 0.0455 | [0, 0.278] |
| C6 | 17 observed + 3 refusals | 10 | 7 | 0 | 0 | 0.0625 | [0, 0.354] | 0.0455 | [0, 0.278] |
| C8 | 20 | 10 | 10 | 0 | 0 | 0.0455 | [0, 0.278] | 0.0455 | [0, 0.278] |

**Disagreements: none.** In 97 observation outcomes no check returned
`observed` different from the independently derived truth; every disagreement
slot in `a-class-results.edn` is empty, so there is no case to adjudicate
(check vs ground truth) — the packet's section-4 obligation is discharged by
the empty list.

**Refusals (all C6, all expected):** c6-f08 `:unknown-sha` (witness carries a
made-up 40-hex sha), c6-f09 `:unknown-sha` (witness names repo futon2 with a
mathlib4 commit), c6-f10 `:no-locator` (`:require-entry` set, witness has no
`:entry`). In each the truth is false and the check refused rather than
reading false — the typed-absence behaviour the contract specifies
(`:invalid-witness`/`:unresolved-reference` are typed refusals, never
substituted values).

Notable true-positive coverage: C5's stale-locus near-miss
(`wm-machine-action`, whose locus `futon2/src/futon2/aif/policy.clj:672` no
longer resolves because policy.clj has 525 lines at HEAD) reads false in all
three bundle revisions r10, r13, r14, r15 queried; C8's drift cases (warrants
whose pinned files moved after the run) all read false with `:reason
:content-moved`; C8's failing-run records read false.

## Interpretation

Under P5 the checkable classes carry zero adjudication rates by construction
of the token definition; this packet measures the *checks* against that same
token and finds the mechanical claim intact: no false passes and no false
fails on a population built to be hard to get right (upper Wilson bound 0.28
at n=10 per arm; 0.35 for C6's false arm at n=7). This is evidence about the
checks' fidelity to their tokens, not about the tokens' adequacy — a true C8
reading still "says nothing about whether the tests are worth passing", and a
true C4 reading still does not say the declaration is correct.

## Reproduction

```
cd /home/joe/code/futon2
python3 holes/labs/wm-contract/proof2/packets/a_class_groundtruth.py   # writes a-class-truth.json, exits 1 on construction mismatch
clojure -M -e '(load-file "holes/labs/wm-contract/proof2/packets/a_class_runner.clj")'  # writes a-class-results.edn
```

The ground-truth script needs network access to the evidence API
(`A_CLASS_AGENCY`, default http://127.0.0.1:7070) for C8, and nothing else
beyond the three checkouts. The runner is read-only against repos and
registry. The committed run's numbers were produced in a scratch twin of
futon2 (a `--shared` clone plus a commit of exactly these packet files,
`A_CLASS_ROOT` pointing at the twin) because C6's fixtures must be committed
to be observable; the post-commit verification re-run at the real packet
commit reproduced every row identically (see the completion bell to claude-8).

## Files

- `a-class-cases.edn` — the 100 locators with intended truth and construction notes.
- `a_class_groundtruth.py` — independent truth derivation (second program).
- `a-class-truth.json` — derived truths with the command-level justification per case.
- `a_class_runner.clj` — check runner + A-S tabulation.
- `a-class-results.edn` — full per-case record: locator, derived truth, truth
  command output, check outcome, check evidence, agreement.
- `a-class-fixtures/` — the eighteen C6 witness fixtures.
