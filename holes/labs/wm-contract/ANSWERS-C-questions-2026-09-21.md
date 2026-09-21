# Joe's answers to the improve-2/fix-17 preference questions — 2026-09-21

Questions from improve-2-fix-17-DISCOVERY.md §4 (04e34c8b), put to Joe one at a
time by claude-3 in the operator buffer. Joe's words are voice-transcribed;
lightly punctuated, not reworded.

## Q1 — what does C value: completion claims, attested work results, or flight dispositions?

Joe, verbatim:

> It's really all of those, so they're all important. Attested work results,
> like, did we actually deliver? So, you know, you can look at how the machine
> currently [works]. Those things like warrants. So we got tests that pass when
> work is dispatched to an agent and those [are] cashed with a warrant in a new
> test registry, so that's like a completed work. So that's kind of a blend of
> A and B, but attested work results. Like it wasn't completed, but — so an
> incremental work result is okay, and that exists. And then something I've
> really emphasized is how was the work done? And I think that relates more to
> your C, item C, of flight dispositions, because I don't just have preferences
> about the terminal result. We've got preferences about what happened along
> the way, so you could think of it as a little bit like an alleycat messenger
> race: you might want to stamp several places that you visited on your route
> through the city in order to actually complete the race. And I've discussed
> this quite a lot ... I've talked about Ostrom, I've talked about institutions,
> and it sounds as though right now that has been either built dark or written
> up as documentation only, but not actually added into the framework ... I
> would actually prefer if my specification was incorporated into the build
> result.

claude-3's reading (for Joe to correct):
1. Outcome half: value attested work results, including incremental ones. The
   attestation that already exists is the test-registry warrant (a registered,
   checkable test run for work dispatched to an agent). A completion claim
   alone is not the valued thing.
2. Process half: preferences over the route, not only the endpoint — checkpoints
   the work must pass through (the alleycat stamps). This is the half recorded
   in NOTE-joes-view-of-C.md §1 "Half 2" (Ostrom/IAD deontics, 2026-09-17
   quotes) and PLAN-c-replacement-2026-09-20.md (IAD vector from corneli2016
   Table 2), and it is not in the code or Lean: production passes a constant
   C at every step (horizon-g-sparse :spec); the step-indexed Cτ path exists
   and is unused; the IAD vector is not wired.
3. Instruction: incorporate the existing written specification into the build,
   rather than leave it as documentation.

## Q1 follow-up — are institutions beside C (rules that refuse) or inside it (route preferences)?

claude-3 asked: is the split "a stamp the race requires is a rule that refuses;
a route you'd rather take is a weighted preference over the route" right?

Joe, verbatim:

> So, indeed. Ostrom's institutions talk about rules that can fire. So you may
> be permitted, or you may have to. They have deontics. You may, you may not.
> Or you must. Okay. And those are valences that operationalize preferences in
> my case. So if you look at my considerations for design requirements, those
> would be preferences, but those preferences don't apply to every single case.
> The design requirements are not requirements that must be applied globally,
> but they must be applied ... particularly. So yeah, you got stamps that you
> must collect. You have stamps that you may collect. You have routes that are
> preferential and you may or may not be able to take those routes. So I would
> say IAD is kind of an operator over preferences, because otherwise you say,
> well, I may be able to do something, but you don't care because you don't
> prefer it or you're uninterested. That institution isn't turned on. So yeah,
> I would say IAD operationalizes preferences.

claude-3's reading (for Joe to correct):
1. Preferences come first (C over outcomes and over the route, Cτ). An
   institution is an operator over them: in a particular action situation it
   gives a preference a deontic valence — MUST (a required stamp: the attempt
   does not count without it), MAY (a permitted stamp or route, weighted by
   the preference it operationalizes), MAY NOT (forbidden).
2. Institutions are situational, not global: a design requirement applies in
   the situations its rule names ("applied particularly"), and is inert where
   no preference is engaged ("that institution isn't turned on").
3. So neither pure "beside C" nor pure "inside C": IAD maps (situation,
   preference) → valence; MUST/MAY NOT act as guards on the route, MAY carries
   the preference's weight into Cτ.

## Q2 — budget scope: should adding an unrelated mission weaken existing missions' odds?

Options put: (a) portfolio budget, today — adding any source dilutes all;
(b) per-target budget; (c) no shared budget — each source's weight stands alone.

Joe, verbatim:

> So, this is an interesting question. I would say value [is] very related to
> probability ... The probability of being selected sounds a lot like an analog
> of value. So if we have a high probability [for] the mission, because it's a
> focus — say we're focusing on building the war machine, for example — it's
> high probability for lots of reasons. I've declared it to be a priority.
> We've done a lot of recent work on it, so we've got a habit of working on it
> and so forth. I presumably value the completion of that work. Or I may even
> enjoy doing the work for its own right. Whatever. So if I add some other
> mission, first of all, it's most likely I'd add another mission that's like a
> related spinoff of that war machine work. And indeed, I did that recently by
> creating a WMC mission. Then I didn't say, let's go start working on that
> mission. I said, let's create that as a spin-off and stick with what we're
> working on now, maybe come back to the other one later. But that doesn't mean
> that that value persists forever. Because once the War Machine is complete,
> no doubt I'll move on to other things. Maybe I'll get back to my WMC mission,
> for example. So I don't see how adding an additional mission changes the
> value of any existing mission much. And again, if every mission is equally
> likely to be selected, that just means that we don't have a very good model
> of value. So I think that answers your question.

claude-3's reading (for Joe to correct):
1. Budget scope: not (a). Adding a mission (the WMC spin-off,
   holes/M-a-wmc-scaling.md) should not materially change an existing
   mission's value → (c), no shared budget.
2. A declared priority/focus is a source of value in its own right ("I've
   declared it to be a priority"), and it is time-scoped: it holds while the
   focus lasts and lapses when the work completes; a spin-off is created
   without being given focus.
3. Near-uniform selection over missions is a symptom of a poor value model,
   not a neutral default.
4. Joe lists several reasons for high selection probability: declared priority,
   recent work/habit, valuing completion, enjoying the work. In the AIF model
   these land in different places: habit is E (fix-8 now learns it only from
   observed wants), valued completion is C over outcomes, enjoying the work is
   a process preference (C over the route, improve-4), declared priority is a
   declaration C does not yet read. They combine in the posterior over
   policies, which is the "probability of being selected"; keeping them in
   separate terms avoids counting the same reason twice.

## Relayed by claude-5 (2026-09-21): on near-uniform C — input to Q3, not a ruling on values

Joe (as relayed): near-uniform C is "plausible and interesting and sort of bad";
possibly fine as a DEFAULT-MODE baseline, but Cτ "should not be uniform": at a
given time "I have a focus. I have a preference. I have a qualitative analysis
of what's going on in front of me. I've got an aesthetic sensibility." Example:
working on War Machine, he does not want to be distracted by Agency work —
though sometimes Agency work is needed in order to do the WM work.

claude-3's filing: the Cτ in the code is indexed by step within the planning
horizon (terminal vs earlier; PolicyHorizon.lean, live_c schedule). Joe's is
conditioned on the declared current focus. They compose (a focus-conditioned
family whose members are step-indexed); only the first exists. Consistent with
the Q2 answer ("I've declared it to be a priority", time-scoped). The Agency
example reads as a MAY valence (Q1 follow-up): off-focus work permitted where
the focus's own work requires it, otherwise dispreferred. Filed as improve-7.

## Q3 — strength, asked per focus

Joe, verbatim:

> So, it doesn't even need to be the declared focus. It just needs to be, let's
> say, the discovered focus. And, you know, the discovered focus is in the
> embedding landscape as a graph of where work has happened, for example. And
> there could be any other way to discover the focus. War Machine itself might
> have a focus, but, you know, I don't prefer, I dis-prefer random things.
> Teleports across this landscape. If there's a warrant, if there's a reason
> why we need to move between one task and another, like, we found some
> interesting connection, I'm not saying we should discard that. But if we're
> just randomly selecting missions, I dis-prefer that, right? And this is what
> AIF is telling us. It's telling us that we should pick the things that we
> have a high degree of certainty about. And I would say that if I've been
> working on something for months and I haven't declared it finished, I'd have
> a high preference to continue focusing on that until such time as it is
> finished, unless I find some other ... adjacent [topic] ... that's required to
> get it finished. You could see this in the relationship of missions to
> excursions, for example. I might prefer to finish the mission, but then I
> find an interesting excursion that's either relevant to finishing the mission
> or relevant to some other thread that I had going back in the background. If
> I had to put a number I'd put 60% on the current focus and 40% on these
> adjacent topics. And 0% on irrelevant topics. Now, that doesn't mean that I
> might not be working on multiple things in parallel. That's another thing. I
> might be working, for example, on setting up a war machine and setting up an
> APM problem-solving machine. But if you think about it, those are related.
> Those are semantically related, so I would see them as facets of one overall
> thought process, not as two distinct multitracked episodes, even though it
> might sometimes feel that way. So again, this is where the Salingaros
> measures would be appropriate to understanding how my focus is divided
> across Emacs, windows, agent sessions, missions.

claude-3's reading (for Joe to correct):
1. Focus is DISCOVERED, not only declared: e.g. from the embedding landscape as
   a graph of where work has happened (Embedding Live); other discovery methods
   allowed. Long-running unfinished work keeps focus until finished.
2. Preference mass over topics relative to the focus: 60% current focus, 40%
   adjacent (required to finish the focus, or relevant to a background thread;
   mission→excursion is the pattern), 0% irrelevant.
3. A move between tasks is fine when warranted (a found connection, a
   requirement); an unwarranted jump ("teleport") is dispreferred.
4. Parallel threads that are semantically related (WM and APM) are facets of
   one focus, not separate episodes; Salingaros measures describe how focus is
   divided across Emacs windows, agent sessions and missions.
5. This answers improve-2 Q3 as a distribution rather than odds, and gives
   improve-7 its content (focus-conditioned C). Open: whether "0%" is a ruled
   zero (infinite risk; in IAD terms MAY NOT) or a very small mass, and where
   the outcome "nothing delivered" sits relative to 60/40.

## Q3 follow-up — the zero, and "nothing delivered"

Joe, verbatim:

> Okay, if we really have to put numbers on it, sure, we can make it. We can
> add that extra, let's call it 35%, 55%, and 5%. We'll do a breakdown like
> that. And nothing delivered? Boy. Um... Ask that in a more specific way if
> you need an answer.

(Superseded by the confirmation below.)

### Confirmed split

Joe, verbatim:

> Oh, I also just realized my percentages didn't actually add up right. So
> yeah, let's allow thirty-five percent for associated work, 55% for focused
> work, and 5% each: 5% for relevant work that's delivered — or sort of
> irrelevant work that's delivered, relevant to something else. Let's say
> interruptions, you know, emergency services, that kind of stuff. And then
> another 5% for nothing delivered, which is weird because that's not my
> preference. I don't prefer that nothing's delivered. But at least I'd rather
> know if nothing was delivered than not know about it. So this 5% is kind of
> the tax for allowing the possibility of dealing with failure modes, and
> that's maybe where I would put in things like the stop the line. So, you
> know, I really value stopping the line when something is going wrong, even
> though it means that no work is getting delivered at that time. So hopefully
> that breakdown makes a little bit more sense.

Ruled terminal C over how a run ends, relative to the (discovered) focus:

| outcome class | mass |
|---|---:|
| attested increment on the current focus | 0.55 |
| attested increment on associated (adjacent) work | 0.35 |
| attested increment on work outside the focus, useful elsewhere (interruptions, emergencies) | 0.05 |
| nothing delivered, and KNOWN: an observed, typed failure or a stop-the-line | 0.05 |

Notes (claude-3):
- The last 5% is for knowing that nothing was delivered (a recorded failure
  mode, stop-the-line), not a preference for non-delivery. An outcome that is
  not observed at all is not in this class; the split gives it no mass.
- Implied odds: focus : associated : elsewhere : known-failure =
  11 : 7 : 1 : 1 (focus vs known failure = ln 11 ≈ 2.40 nats).

### Global estimate vs local focus (hierarchy)

Joe, verbatim:

> Yeah, and as we were talking about taking C and tau, I would say that these
> preferences are a global estimate, but in a local estimate, it could shift
> considerably because I will have a focused project. And I'll be in it. And
> that's kind of it. That's what it means to have a focused project. So it
> doesn't mean that I always am dividing all my attention 60-40 or 35-45, 65,
> blah, who knows? I will be dividing it not equally in any given point in time
> once I've selected the focus. So this is where the hierarchical model, I
> think, has to kick in so that I get into that 60-40 split, and then I say,
> well, I'm in one of those partitions, and I'm going to stay in that partition
> until I get back out of it.

claude-3's reading (for Joe to correct):
1. 55/35/5/5 is a GLOBAL estimate: the long-run share across partitions, not
   the C of any single tick.
2. Locally, once a partition is entered, the work stays in it until it exits
   (finished, or a warranted move); per-tick C concentrates on the current
   partition.
3. So the model is two-level: a slow state = which partition the work is in,
   with sticky transitions (exit on completion or warrant) whose long-run
   occupancy is the global split; a fast level whose Cτ is conditioned on the
   current partition. This is the map's R15 (hierarchy and timescale), for
   which no dynamics exist yet in Lean or code.

### Example: the War Machine's own work classes

Joe, verbatim:

> I mean, here's a good example for the war machine itself. How many ticks are
> spent on self-repair? Versus how many are spent on greenfield or incremental
> work. We can break down the classes of work that the machine works on, and
> they're all preferred, but if all it ever does is repair, that's not
> preferred.

claude-3's reading: a preference over the long-run distribution of work classes
across ticks (self-repair, incremental, greenfield): each class is preferred,
but an occupancy concentrated on repair is not. The same two-level structure
as the focus split: a global occupancy preference over classes, with per-tick
work inside one class.

Rough census from the retained cohort attempts (claude-3, heuristic
classification; script and output in runs/fixlist-2026-09-21/work-class-census/):
134 attempts, 2026-07-14 .. 2026-09-21. Self-repair 58; mission work 25
(cascade 12 + older advance-mission 13); learn-action-class 26 (22 with no
selection); no selection 23. 2026-09-13..15: 23 self-repair, 0 mission work.
2026-09-19..21: 12 mission work, 5 self-repair. Grounded changes: 19 from
self-repair, 5 from mission work.

### Relay (claude-5, 2026-09-21): backlog of unfinished missions

Joe, near-verbatim via claude-5:

> We've been talking about my preference for staying focused, but I have a
> global dispreference for accruing a massive backlog of unfinished missions
> (which does seem to be the case at the moment). I have had a lot of
> difficulty getting my README-inbox-zero.md features to actually work but
> another form of Inbox Zero would be that missions are all complete. We are
> very far from that at the moment.

claude-5's rough scale (heuristic status parse of [MCE]-*.md under holes/, order
of magnitude only): 642 docs, 493 not marked closed.

claude-3's reading, for C:
1. This is a preference over a stock, not over a tick: preference falls as the
   count of open missions rises. Opening a mission therefore costs something,
   and closing one has value even when it is outside the current focus. It
   sits at the same global level as the 55/35/5/5 occupancy and the work-class
   occupancy, not inside per-tick Cτ.
2. It pulls against improve-5/6 (novelty, surprise, capability), which are
   largest on new missions. The two are traded in one G, not both maximised.
3. Only a witnessed closure lowers the stock: every obligation discharged, or
   an explicit retirement with a recorded reason. A status edit alone does not
   count (futon3c M-the-perfect-crime).
4. Retirement counts as closure ("inbox zero"). Reducing the stock honestly by
   retirement is operator/curation work, not WM work, and is the first move.
5. For the WM itself it gives an observable: net missions opened minus closed
   per run, scored against this preference.
Needs before it can enter C: a mission-status census whose closure test uses
the witnesses in (3), not a status-line regex.

Baseline (claude-5 census, futon2 16abf75a, runs/mission-census-2026-09-21/,
witness-based closure test): 642 mission docs; closed-witnessed 71,
closed-unwitnessed 45, open 359, undetermined 167. Retirement candidates 307 by
age alone (open, untouched 30+ days), not judged. The stock for the WM measure
is 359 open, plus 167 that cannot be scored until classified.
