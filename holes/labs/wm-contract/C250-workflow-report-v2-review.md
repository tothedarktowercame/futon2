# C250 — independent re-review of workflow report v2

Date: 2026-08-31

Reviewed:

- generator `p4ng` `d619c51`
- generated report `futon2` `02fec27`
- `p4ng/empirics-futon/control_workflow_report.sh`

## Verdict

**Keep the workflow table out of the paper pending another repair.** The v2 EDN
fixes the original lane recount, combined/suffixed identifiers, and total
parse-failure cases. The reader-facing TeX still presents the new values under
the old labels, and two attribution paths still turn unrecognised text into
plausible evidence.

## What C244's repair did satisfy

At the report's pinned revisions (`p4ng` case ledger `18ea243`, futon2 lane
registry `9b24dec`), an independent latest-heading recount agrees with the v2
EDN:

| lane | closed-attributed | open-holding |
|---|---:|---:|
| wm-nouns | 9 | 1 |
| wm-verbs | 7 | 1 |
| wm-organization | 8 | 1 |
| wm-evidence | 6 | 1 |

The parser now applies last-wins after collecting all `## C...` headings.
`C232, C236`, `C212a`, `C214a`, and `C220-dup` are represented rather than
silently dropped. The decisions file has explicit current-population sentinels,
and a normal `### ` decision outside them is rejected. The EDN schema bump to
v2 correctly prevents consumers from reading renamed fields under v1.

## Remaining findings

### 1. The paper table still makes the withdrawn claim

The generator fills its two numeric columns with `:closed-attributed` followed
by `:open-holding`, but the generated TeX labels those columns **Dispatched**
and **Completed** and retains the caption “cases dispatched and completed.”
For the pinned snapshot, the row `wm-nouns 9 1` therefore reads as nine
dispatched and one completed, while it actually means nine attributed closures
and one current holding. This is not merely stale wording: the column semantics
are reversed and the historical dispatch claim C244 required withdrawing is
still reader-facing.

### 2. Arbitrary parentheticals become owner repairs

`heading-attribution` takes the first parenthetical in the title and
`classify-attribution` classifies every nonblank value other than `wm-*` or
`dispatched` as `:owner`. In the live source this makes C16, C18, C146, C150,
C154, and C161 owner repairs merely because their first parenthetical is a
date or quotation. Only C213 says `my fix`.

Consequently the pinned report's `owner-repairs 7` is supported as **1**, and
`:cases-attributed 38` is supported as **32** (30 lane closures, one explicit
owner repair, one open dispatch). A date is not an attribution.

### 3. Unknown lanes are silently attributed and then disappear

Any string beginning `wm-` is accepted as a completed lane without validating
membership in the four declared lanes. A temporary fixture containing
`(wm-typo)` exited 0, counted the case in `:cases-attributed`, but counted it in
no lane. This breaks the accounting identity without a failure.

The same fixture placed `(detail)` before `(wm-nouns)`. It exited 0, counted the
case as an owner repair, and gave wm-nouns zero. The supplied control does not
exercise either mutation.

### 4. “Exact to the frontier” is not what the calculation checks

`systematic-from` finds an exact numeric suffix ending at the largest
**attributed ledger case**, not at `:frontier`, which also includes registry
holdings. At the pinned snapshot it reports C240 while the attributed list ends
at C243 and the frontier is C247. C244–C247 are holdings, not ledger
attributions. C240 is defensible only if holdings are explicitly added to the
coverage population; the current note instead says every case through the
frontier “carries an attribution.” The calculation and prose use different
populations.

### 5. The negative control mutates authoritative sources in place

`control_workflow_report.sh` copies the real queue and decision ledger, appends
mutations to those authoritative files, runs the generator, and restores with
`cp`. In this shared checkout, a legitimate edit landing between backup and
restore would be overwritten. A build or reader can also observe the mutated
source or generated output during the control. The control should operate on
isolated fixture roots; a trap does not make in-place mutation transactionally
safe.

The existing mutations establish only that one unattributed heading above the
frontier and one ordinary `### ` outside the decision sentinels reject. They do
not establish that malformed-but-classified attribution cannot pass.

## Silent-drop census

- Combined identifiers: represented.
- Suffixed identifiers: represented.
- Unclassified headings below the systematic window: retained in
  `:cases-in-ledger`, deliberately absent from lane totals.
- Unknown `wm-*` attribution: silently accepted, then omitted from all four
  lane totals.
- First non-attribution parenthetical: silently converted into owner evidence.
- Decision-like heading with a different Markdown shape: outside the parser's
  declared `### ` population and still invisible; the sentinel contract needs
  either an explicit allowed grammar or a broader decision-like-heading
  falsifier.

## Acceptance before restoration

1. Render `closed-attributed` and `open-holding` under those names in both
   header and caption.
2. Accept only the four named lanes, an explicit owner marker vocabulary, an
   explicit dispatch marker, or typed unclassified status.
3. Make component totals reconcile with `cases-attributed` and fail otherwise.
4. Define whether current holdings participate in systematic coverage, then
   compute and describe the same population.
5. Move controls to isolated fixture roots and add unknown-lane and
   title-parenthetical mutations.

Until these hold, v2 is an improved diagnostic EDN but not a defensible paper
table.
