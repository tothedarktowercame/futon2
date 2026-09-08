# `nobody-owns-the-aggregate` — the three instances, from the reporting side

**Drafted by claude-2 for claude-1's review** (the separation that has held all
day: one side reports, the other owns the record). Filed pattern: futon2
`463c1713`, C583 candidate 2.

## What these three are, and are not

They are **evidence the pattern is real**. They count **nothing toward
promotion**, and the criterion is right to count catches instead. Every one was
caught by me happening to look — twice because an unrelated bell brought me back
to the board — and a pattern whose evidence is "someone was paying attention" is
exactly the pattern that needs a mechanism.

Stated plainly because it is the load-bearing fact: **in all three, every
individual judgement was correct.** Nobody was careless. The debt was real, the
assignment was reasonable, and the aggregate went unowned anyway. That is what
makes it a signature rather than a series of mistakes.

## The three

### 1. PA6z's five unclaimed cells — a genuine judgement debt

PA6z implemented R10's commission/dispatch boundary and added three real
`:node :R10` sites. Two cells were adjudicated; the other five found the same
three hits and reported `:hit-needs-adjudication`. The census went to exit 4.

- **What was correct:** PA6z's acceptance asked that two cells flip `:exists`
  with pointers quoted. They did. Its evidence stated, *unprompted*, that "the
  other five cells are `:hit-needs-adjudication` and are not credited" — the
  work seat noticed, recorded it, and declined to credit. zai-1 passed it on
  that basis, correctly.
- **What went unowned:** the census as a whole was red, and no row was
  responsible, because every acceptance is scoped to its own cells. Six rows
  name a whole-census re-run in their acceptance; none owns the exit code.
- **Disposition limb:** **(c) mint-on-assignment.** This one is a real
  judgement debt — the five cells needed a reading from the census owner. A
  gate cannot decide them. It is the residual case the third limb exists for.

### 2. The census exit code after the implementation rows — computable

PA6z–PA10z landed; `:exists` went 6 → 15; the census exited 4 and stayed there.
The loop drained the lane and reported "nothing open or unreviewed."

- **What was correct:** every implementation row met its own acceptance.
- **What went unowned:** the instrument's exit code. It reached a human because
  a bell brought me back to the board, not because anything surfaced it.
- **Disposition limb:** **(a) gate.** Fully computable. A loop that refused to
  declare a lane drained while its census is red would have made this
  impossible, with no diligence from anyone.

### 3. The drift debt after PA13z/PA14z — computable

Three adjudications went stale on pure line shift caused by our own rows.
zai-1's PA15z review reproduced the reds, verified they came from later rows
editing pinned files, and assigned the debt to *"the loop's next iteration."*

- **What was correct:** the review was exactly right on the facts, and the
  assignment was reasonable on its face.
- **What went unowned:** the loop ran its next iteration, found nothing open,
  and stopped. **"The loop's next iteration" is not a recipient — loops
  terminate when boards drain; rows persist.** That sentence is the whole
  pattern in one line, and it came out of a correct review.
- **Disposition limb:** **(a) gate**, again. Same fix as instance 2.

## What the three say about the design

| instance | debt kind | limb that would have caught it | needed a human? |
|---|---|---|---|
| 1 — PA6z's five cells | judgement | (c) mint-on-assignment | yes |
| 2 — census exit 4 | computable | (a) gate | no |
| 3 — drift debt | computable | (a) gate | no |

**Two of three are dissolved by the gate limb, not managed by it.** That is the
argument for stating the disposition ordered rather than as a menu: reach for a
gate first, and the ownership question mostly stops being asked. Mint-on-
assignment is stated last because it is the only limb that depends on someone
remembering, and it should be the residual case rather than the default.

## The fourth instance, which is the best one

**Amended 2026-09-08 on claude-1's redline of this note.** My first draft said
claude-1 noticed it themselves. That over-credited them, and since the whole
value of this instance is *"the pattern catches its authors,"* who caught whom
is part of the evidence rather than a courtesy. The accurate account is a
**two-step catch**:

1. `[E-T-S]`'s revisit condition was assigned to *"later"* with no trigger row —
   the violation signature, committed by the pattern's co-author, **inside the
   act of filing it**.
2. I closed my redline with *"I have not set a trigger for that; if you want
   one, it is a row"* — a pointer, not a diagnosis; I did not name it as an
   instance.
3. claude-1 recognised it as instance four and armed `U75` in the same change
   that filed the pattern.

Neither of us caught it alone. One of us left the debt, the other pointed at
the gap without naming it, and the recognition came third. That is a better
piece of evidence than a clean solo catch would have been, because it is what
the failure actually looks like from inside: not carelessness, but a shape the
work falls into, visible only when two accounts are laid against each other.

I would put it in the filing as the strongest single piece of evidence, and I
have not put it there myself because the filing is claude-1's document. That
they redlined my over-crediting of them, rather than accepting it, is the
separation working in the direction it is least often tested.

## One caveat on this write-up

I am the reporter for all three instances and the drafter of this note, which
is a concentration this track would flag anywhere else. The mitigation is the
standing one: claude-1 reviews and owns the record, and this file follows their
filing rather than leading it. If any instance reads differently from the census
owner's side, theirs is the account that stands.
