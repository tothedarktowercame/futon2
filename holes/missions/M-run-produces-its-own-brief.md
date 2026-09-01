# M — a run produces its own brief

Opened 2026-09-01 by `claude-20`, from Joe in the decision session:

> This isn't about cadence. This is simply about when the machine runs, is it
> capable of producing something like what we're doing right now as a list of
> outstanding decisions that need to be thought about? And is it capable of
> producing some numbers and qualifications that explain where it's at in its
> build?

That is an acceptance bar for a run, stated plainly, and the earlier prototype
did not meet it. Joe: *"the reporting basically just needs to be some kind of
predefined or agreed reporting mechanism whereby there's some oversight of any
key questions or issues that arise. And that's fairly straightforward. The
earlier prototype didn't seem to manage to achieve that one simple criterion."*

## The bar

A run is complete when it has emitted, without a person assembling it:

1. **The decisions.** What is outstanding, what was settled since the last run,
   and what newly needs an operator call — at the altitude of
   `DECISIONS-REGISTER.md`, not at the altitude of a lint finding.
2. **The state of the build.** Numbers with their qualifications: what is
   established, what is declared as not establishable, what is stale and by how
   much.

Both must carry their own basis, so a reader can tell a current claim from a
remembered one.

## Where each half stands today

**The numbers half largely exists, and is pointed at a paper rather than at
Joe.** p4ng carries 22 generators in `empirics-futon/gen_*`, and they already do
the hard part — each artifact states its own provenance. From
`sec-lane-campaign-generated.tex`:

> Source registry as-of 2026-08-31T15:39:14Z; futon2 revision `0725848`,
> SHA-256 prefix `ab55f11fccaee0a5`; rendered 2026-09-01T06:54:10Z.

`gen_workflow_report.bb` is the seed of the second half and has already proved
it behaves honestly: on 2026-09-01 its frontier moved backwards, 421 to 382,
because the ledger it reads had gaps. It refused to claim across them (C441).
A generator that would rather report a worse number than an unsupported one is
the right starting material.

Joe's own summary of this prototype: live feeds from four helper agents
aggregated into one Sierpinski-gasket infographic that says at a glance what is
going on — *"aggregating not just one overnight run, but a lot of independent,
autonomous work."*

**The decisions half has no producer at all.** `DECISIONS-REGISTER.md` was
assembled by hand this morning from a conversation buffer. If that session had
been lost, it could not have been regenerated — the source was a transcript, not
an artifact.

This is C441 one level up. There, the ledger degraded as throughput rose and a
generator caught it. Here, the artifact Joe has said he values most has no
generator to catch anything, because there is nothing to generate it from.

## What has to be true before a producer can exist

A decision list can only be emitted if decisions are recorded as data at the
moment they arise, rather than narrated in prose and mined afterwards. Today
they arise in three places and are durable in none of them: a lane report's
prose, an owner review in `CLEANUP-QUEUE.md`, and a REPL conversation.

**This is the same gap that stalled O15 and O11**: the workspace has vocabulary
for authority and evidence provenance and none for *occasions* — no milestone,
no epoch, no run-as-a-recordable-thing. A decision has to attach to an occasion
to be reportable "since the last run."

So `O25` (define "major milestone") is not a small follow-up to a cadence
question. It is a prerequisite for this mission.

## Also in scope, per Joe

**The architecture of a run.** Joe: *"rather than having two agents like we used
to, maybe we want to have four agents exactly with this kind of handoff... the
architecture of a run needs to be rethought."* The four-lane campaign was run by
hand-written handoffs from a coordinating owner; the machine's own run does not
work that way. Whether it should is an open architecture question, not a
refactor.

**Operator-facing surfaces.** Ten currently face Joe: six `make` targets
(`status`, `pre-merge`, `workspace-gate`, `gate-last-receipt`, `ci`,
`run-readiness`), the Morning Brief `DECISION-DUE` queue, the parking request,
the RUNBOOK, and three decision documents added 2026-09-01. Several overlap and
one — the brief — has no command at all. Joe: *"the operator-facing aspects of
the war machine need a fairly comprehensive rethink."*

## Not in scope

Processing the 72 historical Morning Brief items. Joe closed that in the same
message: *"they're just there as a log of how the system was working in some
earlier iterations, and as we've seen, it comprehensively was not working very
well."* They are retained, not opened. See register O11 / S30.
