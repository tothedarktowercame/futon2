# B0 — landscape business-intelligence drafts

This directory atomizes
`TN-deep-research-landscape-position-FINDINGS-2026-07-27.md` into dated,
typed EDN records. The records are drafts: no store writes or external
refreshes were performed.

The split is claim-level. Organization records describe the player and its
stack position; capital, vacancy, funder, yardstick, correction, and scope
records remain separate so later probes can refresh one fact without
rewriting the organization.

## Counts

| Kind | Records |
|---|---:|
| `:org` | 43 |
| `:capital` | 6 |
| `:vacancy` | 19 |
| `:funder` | 9 |
| `:yardstick-gap` | 7 |
| `:convening` | 3 |
| `:refuted` | 12 |
| `:scope-finding` | 7 |
| **Total** | **106** |

Sensitivity split: 99 public, 7 private. Private records contain
operator-discussion engagement context, personal-target routing, or
contact-oriented collaborator intelligence.

## Pattern coverage

Counts are attachment counts; a record may instantiate more than one
pattern.

| Pattern | Instances | Coverage |
|---|---:|---|
| `nomad/choose-the-grid` | 13 | present |
| `nomad/live-yardstick` | 37 | present |
| `nomad/owner-of-the-problem` | 9 | present |
| `nomad/skip-the-flood` | 59 | present |
| `nomad/self-certifying-artifact` | 17 | present |
| `nomad/author-the-yardstick` | 30 | present |
| `nomad/letter-from-the-future` | 7 | present |
| `nomad/licensed-ground` | 3 | present |

Uncovered patterns: none.

## Corpus gaps retained as data

- The FINDINGS say the L1 roster contained 45+ organizations but do not
  reproduce the full roster. This B0 file contains 43 organization/project
  records recoverable from the report itself. Sparse roster mentions use
  `:unknown` or `:not-stated-in-findings`; they are refresh targets, not
  inferred facts.
- The XAI and model-checking/formal-methods commercial lane was outside the
  original scope. Depintel is recorded as the held-out scope datum, not as a
  completed adjacent-market sweep.
- The search budget was exhausted, four organization leads retain explicit
  verification debt, and the required red-team lane was not run.
- The proposed weak-convergence/uniform-boundedness route, future INI
  convening, and adjacent-ridge engagement plan are marked untested rather
  than promoted as observed outcomes.

## Validation

```sh
bb -e '(clojure.edn/read-string (slurp "holes/labs/M-digital-nomad-patterns/b0-records.edn"))'
```
