# Can a mission's tl;dr be drafted from its IDENTIFY alone?

Joe's question, 2026-09-20: base the `### tl;dr` tickboxes on IDENTIFY alone,
even for late-phase missions, then run a second pass to see whether the boxes
actually tick.

## Method

Pool: 49 registry missions that are `:complete`, or live with INSTANTIATE /
VERIFY / DOCUMENT in the status line, and that have a section whose heading
LEADS with IDENTIFY. Those sections were extracted by the reviewer (not the
drafting agent) into `sections/S01..S49.md`, opaque ids, mapping held in
`MANIFEST.edn`.

- **Pass 1** (codex-7, `d89ae8c8`): drafted 223 unchecked items from the
  sections alone. It never saw a mission document or the mapping.
- **Pass 2** (codex-2, `678e3c27`): read each mission's FULL document and
  judged every item, with a quoted line and line number required for each
  tickable or partial. It was given `JUDGE-MAP.md` with the stratum labels
  removed, so it could not know which sids were expected to score well.

Checks the reviewer ran: 223/223 verdicts present; 185/185 quoted citations
found verbatim in the cited file within a few lines of the cited number; no
box ticked anywhere; no mission document edited by either pass. A blinding
audit over all 223 items looked for identifier-like tokens absent from a
section but present in its full document — 8 hits, all traceable to words the
section does contain, so no evidence pass 1 used anything but its section.

## Result

| | sids | items | tickable | partial | not | ill-formed |
|---|---:|---:|---:|---:|---:|---:|
| clean IDENTIFY | 15 | 61 | 77.0% | 13.1% | 9.8% | 0 |
| IDENTIFY names an outcome | 32 | 162 | 67.9% | 18.5% | 13.6% | 0 |
| **all** | **47** | **223** | **70.4%** | **17.0%** | **12.6%** | **0** |

`S22` and `S36` were refused at pass 1 as too thin to summarise, and are
excluded. Of the 28 `not` verdicts, **25 are `absent`** — the document never
reaches the item — and only **3 are `contradicted`**, where the work went
another way.

## Two findings

**1. Drafting from IDENTIFY alone mostly works, and it fails by being early
rather than wrong.** About seven items in ten were tickable on the document's
own recorded evidence, and about nine in ten were tickable or partly so. The
failures are overwhelmingly work that had not happened, not claims that
turned out false. That is the behaviour Joe's framing asks for: an item that
is not yet done simply stays unticked, and more items get added as they are
discovered.

**2. The reviewer's contamination worry did not survive the test.** The
prediction was that sections already saying DONE/COMPLETE/CLOSED would score
HIGHER, because a drafter could read outcomes off them. They scored LOWER
(67.9% against 77.0%), and the gap is about 1.4 standard errors — no
detectable difference in the predicted direction or any other.

What does track is **how long the section is**:

| | sids | mean tickable rate |
|---|---:|---:|
| shorter half of sections | 23 | 81.9% |
| longer half of sections | 24 | 63.6% |

Median section length is 3,799 bytes in the clean group and 7,339 in the
other, so the strata differ in length by about a factor of two, and the
18-point spread by length is wider than the 9-point spread by stratum. The
plausible mechanism: a long IDENTIFY names more distinct things, a summary of
it commits to more of them, and more of what it commits to has not been built
yet.

## Limits

- One judge, no second rater, so no inter-rater agreement is established.
- "Tickable" means the document records it. A mission that did the work and
  never wrote it down scores `absent`; 39 of 217 live missions sit at a late
  phase without a close, so that undercount is real but unmeasured here.
- The length effect is observational. Nothing here separates "long sections
  are harder to summarise" from "missions with long IDENTIFY sections are
  less finished".
