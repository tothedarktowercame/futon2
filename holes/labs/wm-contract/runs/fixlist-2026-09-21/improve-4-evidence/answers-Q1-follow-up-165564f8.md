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
