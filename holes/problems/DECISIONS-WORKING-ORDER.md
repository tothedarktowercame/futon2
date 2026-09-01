# Working through the decisions

Opened 2026-09-01 by `claude-20`, after Joe:

> I can't just rule on things because I don't have a great view of the code or
> the build process. And so you say, your decision needed, but I just don't
> have the context to make those decisions.

That is a correct description of a defect on my side, not on his.
[`DECISIONS-REGISTER.md`](DECISIONS-REGISTER.md) lists 24 open decisions. Most
are phrased as open questions addressed to someone who has read the file they
name. Asking "omit the channel or refuse the update?" of the person who has not
read `belief.clj` is not requesting a decision; it is transferring work to
whoever is least equipped to do it.

## What changes

**Every decision gets a recommendation.** Joe should be confirming or
overruling a proposal, not originating an answer from a blank question. Where I
have no basis to recommend, that is itself the finding, and the decision is not
ready to be asked.

**A question with no recommendation goes back in the queue, not to Joe.**

**Each brief states the criterion** — what would make one option right — before
it states the options. C166 is the honest specimen of the failure: it asks for
a vocabulary *and* asks "what independent rule makes one useful?" in the same
breath. The second question is the decision; the first cannot be answered until
it is.

## Triage: what is actually blocking each answer

The register grouped decisions by subject. Subject is the wrong axis for
scheduling, because it does not predict what work has to happen before an
answer is possible. This grouping does.

- **R — Return to owner.** Not Joe's call. I escalated an engineering judgement
  because it was written down as a question. I decide, record, and Joe overrules
  if he disagrees.
- **P — Ready for Joe.** A preference, risk-appetite, or priority call. Needs no
  code context once the tradeoff is stated in its own terms. Answerable in the
  next sitting.
- **M — Needs a measurement.** A number nobody has yet. I go get it, and it
  becomes P.
- **D — Needs a demonstration.** Joe has to see the behaviour before the
  question means anything. Needs an excursion, not a paragraph.
- **B — Blocked on another decision.** Sequenced; not asked yet.

| # | Decision | Class | What has to happen first |
|---:|---|:---:|---|
| O1 | Strategic outcome vocabulary | **D** | The carrier family has to be shown doing something before "is this useful" has content. |
| O2 | R16 outward-act binding | **P** | I propose one specific bounded first act, arming rule and read-back. Joe approves that, not the abstract authority. |
| O3 | Avoided-range hard guard | **M** | How often does `:unknown` actually occur? A veto that fires constantly is a different decision from one that never fires. |
| O4 | Prediction triple | **R** | C195: no option-effect measurement, judgement call. Mine. |
| O5 | Belief aggregation | **R** | Directional evidence only, and it is an engineering judgement. Mine. |
| O6 | Strategic-mode inference | **R→P** | I recommend; Joe sees it because a fallback can select while blind. |
| O7 | Missing sorry pressure | **R→P** | Same: I recommend, Joe rules because it can act while blind. |
| O8 | Validated rollout producer | **R** | Mine. |
| O9 | Unscored rollout moves | **B** | After O8. |
| O10 | Fulab surplus absence | **R** | No live caller. Mine, and small. |
| O11 | Morning Brief epoch | **P** | C209 already recommends. **Gates the operator run** — the run is itself the boundary event. |
| O12 | Support-typed shadow → live | **M** | Two sequentially dependent records is not a denominator. |
| O13 | EDN vs SQLite ledger | **M** | Growth rate and per-mutation write cost at 134.6 MB / 6,195 jobs. |
| O14 | Cascade meet semantics | **D** | A modelling decision a lane must not invent. Needs the semantics laid out, not summarised. |
| O15 | Lean check cadence | **P** | **Measured today — see the brief.** |
| O16 | Any check on a timer | **P** | Follows O15. |
| O17 | Acceptable gate distance | **B** | After O15. |
| O18 | Census basis refresh | **R** | Mine. |
| O19 | Send the writer-fence parking request | **P** | Ready. Nothing is parked. |
| O20 | Who cleans the dirty trees | **P** | futon3c 6 APM files, futon3 1 probe artifact. Not mine to commit. |
| O22 | Repair or accept C404 limit 3 | **R** | Mine. |
| O23 | Ledger recording discipline | **R** | Mine. |
| O24 | Lane campaign continuation | **P** | All four idle. |

O21 was not a decision; it was a sequencing constraint on O11, and is folded
into it.

## What this reduces to

**Joe's pile today is 9 decisions, not 24** — seven P, plus O6 and O7, which are
mine to recommend but his to rule on because each can act while blind. Seven are
mine outright and come back as recommendations he can overrule. Three need me to
measure something first. Two need a demonstration built. Two are sequenced
behind another decision. (23 rows: O21 was a sequencing constraint, not a
decision.)

### The order we work them

1. ~~**O15** Lean cadence~~ — **DONE 2026-09-01: option C.**
2. ~~**O16** any check on a timer~~ — **DONE: no timer**, answered by the same sitting. O17 is now blocked on the new O25 (define "major milestone").
3. ~~**O24** lane campaign continuation~~ — **DONE 2026-09-01: yes, continuously and in parallel, once tree hygiene is a facility.** My framing that quiescence only comes from stopping was wrong: that is a consequence of a broken helper, not a law. See C446 and the new O27.
4. **O11** Morning Brief epoch — gates the operator run.
5. **O19** send the writer-fence parking request.
6. **O20** who cleans the dirty trees.
7. **O2** R16 outward-act binding — I bring one concrete proposed act.
8. **O6** strategic-mode inference.
9. **O7** missing sorry pressure.

Items 3–6 are one sitting: they are all about getting to a certified run.

Of the eight, three touch the operator run and can be taken as one sitting:
**O11** (epoch boundary, gates the run), **O19** (send the parking request),
**O20** (who cleans the trees). O24 is a yes/no on continuing the lanes.

## Brief format

One page per decision in [`decision-briefs/`](decision-briefs/). Sections, in
this order, because the order is the point:

1. **The question** — one sentence, no file paths, no type names.
2. **Why it exists** — what happens today, in behaviour rather than code.
3. **The criterion** — what would make one option right. If this cannot be
   written, the brief stops here and the decision is not ready.
4. **Options** — two or three, each with its cost when wrong.
5. **What I measured** — or, honestly, that I did not.
6. **Recommendation** — mine, with the reason.
7. **What deferring costs** — including "nothing", which is often true.

## Order of work

1. Return the nine R-class decisions as recommendations. *(next)*
2. Write briefs for the eight P-class ones. **O15 is written as the specimen.**
3. Take the three measurements (O3, O12, O13).
4. Build the two demonstrations (O1, O14) — these are excursions and the largest
   piece of work here.

Joe reads one brief and says whether the format works before I write seven more.
