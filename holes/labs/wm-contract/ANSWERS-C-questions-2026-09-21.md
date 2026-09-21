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
