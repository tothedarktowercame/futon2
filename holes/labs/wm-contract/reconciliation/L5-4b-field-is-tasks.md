# L5-4b: open-task field and declared reach prior

Base: `30d5585e8`. Branch: `codex11/l5-4b-field-is-tasks`.
Implementation only; no merge, serving reload, click or live certification.

## Field and pins

`outer-observation/giraffe-field` uses the same live/open registry predicates as
`selection-world/default-task-sets` for missions, excursions and tickets. It
preserves pass order, then appends open tasks absent from the pass. Non-open
registry entries are annotations (`:registry/non-open`), outside the open set.
It refuses duplicate registry IDs, duplicate pass IDs, missing admitted row pins,
unknown exclusion reasons and any non-exact admitted/excluded partition.

`:field-facts` has `:enumerated`, `:admitted {target fact}`, `:excluded [row]`,
and `:giraffe/pass-sha256`. Each admitted fact retains status, cone, unblocks,
rank, nested dependency-reading record pin and the reach record. No unavailable
metric is converted to zero. Exclusions retain the reason, pin and evidence:
source defects pin the retained registry entry; unread tasks pin the pass in
which they have no row; source-change exclusions pin the manifest declaring it.
The selector validates the partition again and carries it even on scorer errors.

The pinned 11-01 fixture has **609 admitted + 17 unread = 626**. The two drift
members absent from the pinned open set do not become field entries. Registry
snapshot inputs in this test use the pinned IDs with available source declarations;
this is not a new live registry census. The real pass hash is checked before use.
`M-futonzero-prelim-practice`, excluded in ac5bc477 for satisfied inner policies,
is admitted. The test makes any policy-satisfaction lookup throw.

## Exclusion reason audit

| Historical reason | Producer | Disposition and reason |
|---|---|---|
| `:open`, `:active`, `:identify` | old `outer-observation/excluded-candidate` preferred registry status over declaration failure | Never exclusion reasons. Open entries with usable sources are admitted; actual source defects get their own reason. |
| `:inactive`, `:operator-gated` | same substitution | Non-open under shared registry predicates; move to registry annotations, not the open partition. |
| `:target-source-path-absent` | `construction-inputs/target-source-declarations` | Keep, registry-entry pin: no task source locator. |
| `:target-source-conflict` | same | Keep, registry-entry pin and retained declaration. |
| `:target-item-line-absent` | same | Keep, registry-entry pin: item source lacks locator. |
| `:unread-new` | pass absence / old drift helper and ticket-only supplementation | Keep for every open category, pass pin; no unenumerated drift extras. |
| `:registry-open-not-in-giraffe-field` | old ticket-only supplementation | Normalize to `:unread-new`; same evidence gap for all task categories. |
| `:reading-source-changed` | manifest reading drift | Keep, manifest pin. |
| `:all-policies-already-satisfied` | `policy-satisfaction/all-satisfied-exclusion` call in observer | Remove call. Inner policy satisfaction cannot close an open registry task. Other callers untouched. |
| `:blocking-effective-run-on-same-giraffe-field` | selector's effective-run history filter | Remove. `blocking-row?` requires **finished** measured work and inner policy/check/source identity, not in-flight task state. Inner `require-novel!` unchanged. |
| `:outer-score/graph-term-unavailable` (previously no row reason) | selector's `known-term-field` | No longer an exclusion. Unknown metrics stay admitted; an order requiring them refuses explicitly. |
| `:repeat-choice-after-refusal` | runtime `reading-reselection-port`, after selection | Retain as reading-refusal annotation. Do not remove the open task. See continuation limit below. |

The closed set is declared once in `outer-observation/task-exclusion-reasons`:
source-path-absent, source-conflict, item-line-absent, unread-new,
reading-source-changed. There is **no** measured in-flight task-state producer;
no misleading `:in-flight-on-this-field` reason was minted.

## Reach and projection

`task-reach` reads only the pass row's status and dependency record pin. Kit v1:
ready **3/4**, undetermined **1/2**, not-ready **1/4**. The record is
`{:value v :kind :prior :source :giraffe-status-readiness-prior-v1
  :inputs {:status s :pin row-sha} :kit-version 1}`.
This is an empirical prior, replaceable by Joe, not a calibrated measurement of
success. Unknown status or missing evidence is typed absence. Makeup reach is
ignored (`:default-reach-ignored true` when the obsolete key exists); tests change
it from 0 to 999 with byte-equal observations. Prior run/policy inputs do nothing.

The projector uses the selected task's retained reach record, checks agreement
with its candidate value, and emits observed provenance as its digest. Old records
keep legacy defaulted/receipt behavior. Exclusion target/reason/pin rows project
under `:diagnostics :taskExclusions`; no unsupported Lean RunFacts field is invented.

## Authorized scope adjustments and limits

Coordinator jobs `9315` and `9317` approved the declared prior, unknown-metric
admission, completed-run filter removal, and a narrow runtime-port change.
`reading-reselection-port` retains the refusal and returns no replacement with
`:task-field-reselection-not-supported`. It preserves the original task, complete
field and pending carrier. **Forbidden-target minimum selection is not implemented**;
L5-4c must supply that continuation. No source/record is fabricated to claim it works.

The live makeup's `find-out-before-building` orders undetermined tasks by cone,
but 52 retained rows lack cone/unblocks. It now refuses `graph-term-unavailable`
instead of excluding those tasks. A one-cascade ready-gated control scores the
same full 626-member field and retains it. The scorer and makeup were not changed.
The real cascade is applied once, including generic function steps; a counter
witness prevents duplicate execution while checking reach before scoring.

## Validation

Exact commands, environment, counts, log hashes and failure-name comparison are
in `L5-4b-test-comparison.json`. The requested 12 outer namespaces and both
projector namespaces were run on untouched base and the branch. The full suite
is **not green**: the same 17 failures and 2 errors remain. Historical verifier
errors and stale wiring/learning fixtures are not represented as this slice's
success. The persistence fixture now compares the loaded runtime to this
worktree's actual bytes, rather than the different canonical checkout; no source
checker is disabled. Synthetic Giraffe rows now supply explicit dependency pins.

Canonical `data/` file count was **45,737 before and after** every test run.
All discovered FUTON_WM store/path overrides were assigned scratch paths. Counts
establish no added/deleted canonical files, not a whole-tree byte audit.
