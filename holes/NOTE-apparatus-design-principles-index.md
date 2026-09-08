# Apparatus design principles — index

**Status: PROMOTED (Joe's ruling 2026-09-08), after the APM repair-program
field trial.** Evidence: the scoring ledger,
`futon3c/holes/technotes/TN-apm-principle-scoring-ledger.md` @ b0c36489 —
13 principles, 21 scoring occasions (earned/partial/misled tallied per
principle; two honest misleads kept prominent), plus four uncovered
findings and a four-instance instrument-error lesson (a negative result is
only evidence if the instrument could have produced a positive one). Trial amendments landed in the pattern texts (P2 second
signature: a vocabulary member nothing can produce; P6: an
arbitrarily-movable deadline is not a bound; P7: commissioned by mutation;
P8: extends to fences — observation against live data, not agreement with
a declaration). A 15th pattern was discovered BY the trial and joins the
set: `repairs-name-defects-not-neighborhoods` (a repair citation names the
checkable defect it closes; the forward hedge is the violation's primary
signature). One filing is flagged as a judgment call rather than a settled
fit (claude-5, entry 14): P2's second signature — coverage resembling
capability — may belong under monitors-measure-the-work or
new-failure-class-is-a-design-defect instead; recorded here so the choice
is visible, not implied.

## The `:assures` schema (authoritative declaration — C583 agreement, claude-1/claude-9)

Declared once, here; owned by claude-9. Instances live on WM worklist tickets
(the ticket is the authority); the library-side pattern→tickets index is
GENERATED from tickets and marked derived — never hand-maintained.

```edn
:assures [{:pattern   "apparatus/<flexiarg-id>"   ; id under futon3/library/apparatus/, never @status
           :signature "<violation-signature CLAUSE, quoted or stably indexed>"
                      ; the clause, not the pattern, is the unit of assurance
           :state     :witnessed | :ticketed | :not-applicable  ; closed enum — no fourth state
           :witness   {:mechanism-sha "…" :run "<induced-run record>"}}]  ; required iff :witnessed
```

Closure rule (apparatus/repairs-name-defects-not-neighborhoods): closures are
signature-denominated — the closing record fills "signature this mechanism
makes absent"; a sha alone cannot satisfy it; a closure that hedges forward is
not closed. Witness rule (apparatus/pin-moves-with-the-population +
done-is-observed-running fence amendment): a witness pins the mechanism sha
and an induced observation; when the mechanism moves, the standing staleness
comparator on the render path degrades :witnessed → :ticketed automatically.
Both frontier fences carry commissioning evidence before the render is
trusted: the no-fourth-state lint (commissioned by deleting a ticket and
watching it fail naming the uncovered pair) and the staleness comparator
(commissioned by editing a witnessed mechanism WITHOUT re-witnessing — degrade
observed — and WITH re-witnessing — hold observed).

Original charter below, kept as written.

**Status: PROPOSED, pending Joe's ruling.** Extracted 2026-09-08 from the APM
failure record at Joe's instruction; drafted by claude-9, redlined by claude-1
(bell `invoke-1788833602062-14401-acca3986`), agreed between them with no open
disputes. **The authoritative text is the patterns; this note is pointers only
— do not restate content here** (that would violate principle 1).

Lineage: the same principles stated positively in the Sierpinski-tetrahedron
war-room model (`p4ng/empirics-futon/gen_war_room_tetrahedron.bb`: one
authoritative registry per lane, missing data as a named gap, malformed
registry = exit 1), and in the PLOP-paper excavation that produced the war
machine. APM derived them a third way, by inversion. Every pattern carries a
`@violation-signature` field — the observable that says it is being broken —
because a principle whose violations cannot be detected is commentary.

Primary evidence: `futon3c/holes/T-apm-recurring-failure-end-to-end.md`
(790c0491), `futon3c/holes/technotes/TN-apm-defect-register.md`,
`futon3c/holes/T-typed-submission-wrapper-cancellation-evidence.md` (987e8574).

## The patterns (futon3/library/apparatus/)

| # | pattern | one line |
|---|---|---|
| 1 | `one-authority-per-question` | one reader per fact; second copies are pins (comparator-only, loud on drift) or defects; mutations go through the owning gate |
| 2 | `success-must-not-resemble-failure` | the normal path may not produce fault-shaped evidence |
| 4 | `default-to-the-cheap-error` | asymmetric error costs → default verdict to the cheap side; affirmative evidence to cross |
| 5 | `loudness-is-conserved` | a remedy that quiets a failure must restore equal loudness to the same party, or fail review |
| 6 | `every-wait-has-a-deadline` | an inexpirable wait is a hang; deadlines verifiably absolute |
| 7 | `monitors-measure-the-work` | cursor total over the work object; nil = alarm; commissioned in both directions by induced runs |
| 8 | `done-is-observed-running` | a mechanism exists when seen acting live; standing comparator, not one-off scans |
| 9 | `evidence-to-disposition-once` | one closed-enum join at collection; consumers match totally; blame = pure function of the origin's mechanism envelope (absorbs the old #3) |
| 10 | `replayable-not-precious` | units re-run cheaply from pinned inputs, or preciousness is named and paid for once |
| 11 | `model-upstream-and-coupled` | nothing ships without its contract obligation; model↔runtime coupling enforced or the model is commentary |
| 12 | `new-failure-class-is-a-design-defect` | census closed and shrinking; new class → one-layer-down elimination; growth rate reported |
| 13 | `the-system-stops-on-schedule` | scheduled halts; defect register is the work queue between waves |
| 14 | `pin-moves-with-the-population` | the hand that moves a pinned population carries the re-pin, in the same change |
| 15 | `repairs-name-defects-not-neighborhoods` | a repair citation names the checkable defect it closes, never the commit it landed near; a forward hedge is an admission (added post-trial, 2026-09-08) |

(#3 was merged into #9 during the redline; numbering kept for traceability to
the drafting thread.)

## War-machine scorecard (claude-1, from 2026-09-07/08 direct evidence)

Satisfies now: **5** (moved-population reds stopped the publish and reached the
responsible lane within minutes, twice), **4** (affirmative-only belief events;
rebind refusal on failing witnesses), **10** (immutable CREATE_NEW records,
replayable slices, revert-on-failure gap writes), **2** largely (but inherits
Agency's accepted-vs-running ambiguity at every dispatch), **13** in spirit
(self-halting loop bells its owner; ruled walkthroughs; steward drain
contract).

On track to violate — the open items, each with an owner-visible artifact:

- **7**: the repaired wm stall detector (futon2 `e84c114e`) is commissioned by
  reasoning, not by an induced true stall; the two-direction test is
  outstanding (claude-1 owns).
- **8**: the F10 no-production-caller finding came from a one-off scan (C580);
  it must become a timed comparator or it was true exactly once.
- **11**: F10's `FlightDisposition.all` enumeration not yet pinned by a theorem
  (a constructor could silently drop); the runner/gate outcome-vocabulary seam
  is a live uncoupled join. Both flagged in review, not landed.
- **14**: the 37 strict-stale positive-proof receipts are heading toward a
  steward instead of at-source re-attestation (RED-COMPONENTS item 1); this
  principles set is where that decision should be made from.
