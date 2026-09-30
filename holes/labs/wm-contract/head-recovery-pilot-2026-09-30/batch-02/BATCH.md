# HEAD recovery batch 02 — ten more mission HEADs from Joe's recorded words (2026-09-30)

zai-2 for claude-1 (War Machine selection seam), following the pilot
(PILOT.md, same directory). Method unchanged: git creation
(`git log --diff-filter=A --follow`), then the evidence store text search
(sequential, limit ≤ 50, ≤ 4 queries per mission, no paging, no /health),
keeping only operator turns, then on-disk turns (all ten predate the
2026-08-22 local turn files, so the store was the only recorded source; the
one local mention — M-exotype-xenotype-eoc, claude-18, 2026-09-27 — is a
later UI remark, not commissioning). Operator turn = body `event
"chat-turn"` + `role "user"` + author `joe` (one caveat below), with
relayed agent output excluded. No mission file was edited; no HEAD was
invented.

26 store queries across the ten missions (M-librarian 4, M-becoming-nomad 3,
M-latex-wysiwyg 3, M-case-studies 3, M-xtdb-22x-benchmarking 4,
M-exotype-xenotype-eoc 2, M-xenotype-its 3, M-memory-retrieval 1,
M-wm-capability-claim 2, M-aif-stack 1).

## Table

| mission | created (UTC) | committed by | class | passages | earliest operator turn found |
|---|---|---|---|---|---|
| M-librarian | 2026-08-14T15:31:59 | Joseph Corneli (`6dc1684b1`) | **C** | 0 | (nearby turns 2026-08-14, laptop-return session; none about the mission) |
| M-becoming-nomad | 2026-08-14T15:28:47 | Joseph Corneli (`c089f1f37`) | **A** | 1 | 2026-07-30T12:32:51 |
| M-latex-wysiwyg | 2026-08-14T10:41:39 | Joseph Corneli (`6e88bc256`) | **B** | 2 | 2026-08-15T14:35:54 (post-creation) |
| M-case-studies | 2026-08-11T22:33:47 | Joseph Corneli (`3c99bc878`) | **A** | 1 | 2026-08-11T22:32:47 |
| M-xtdb-22x-benchmarking | 2026-08-05T12:40:38 | Joseph Corneli (`99b9a8eb7`) | **C** | 0 | (nearby Henderson-call turns from 2026-07-29; none about the mission) |
| M-exotype-xenotype-eoc | 2026-08-03T06:37:33 | Joseph Corneli (`0c4b957ca`) | **B** | 1 | 2026-07-15T17:13:16 |
| M-xenotype-its | 2026-08-01T16:16:18 | Joseph Corneli (`e0adbc3a8`) | **A** | 1 | 2026-07-28T13:59:45 |
| M-memory-retrieval | 2026-08-01T16:16:18 | Joseph Corneli (`e0adbc3a8`) | **B** | 2 | 2026-07-28T10:45:07 |
| M-wm-capability-claim | 2026-08-01T15:01:00 | Joseph Corneli (`c6b14e4f5`) | **B** | 1 | 2026-08-15T18:11:23 (post-creation) |
| M-aif-stack | 2026-08-01T14:35:30 | Joseph Corneli (`d1a877057`) | **A** | 1 | 2026-08-01T14:33:00 |

Counts: **A 4, B 4, C 2, D 0.**

## Verbatim passages

### M-aif-stack — A (2.5 minutes before the creating commit)

> OK, if Slice 5 is worth running, let's do it now.  But I think what we
> should do is use that as the "opening act" of a new M-aif-stack that
> intrudeces ants, War Machine, and AIF² as three examples of "the same
> thing" —  so that the Slice 5 finding (whatever it may be) reflects on our
> grasp of AIF as a whole.  The reason for thinking about things this way is
> that the War Machine is substantially "complete" but unproven and I want
> to start thinking about a science paper as opposed to an engineering
> paper —  something that we could use to talk about the "so what" of the
> War Machine.  AIF² gives an interesting "so what" at the level of writing
> —  but we would want a similar so-what at the level of coding.

— 2026-08-01T14:33:00.798140500Z, evidence id `emacs-6c32f5995bef4287449606d71a8a5fb0`,
turn `claude-4-turn-21` (claude-4 session, emacs-claude-repl). ("intrudeces"
is Joe's typo, kept verbatim.)

### M-becoming-nomad — A (15 days before the commit; the mission file itself
quotes this turn's closing words)

> So... the thing is, regarding "Self-application before calibration *is*
> the comfortable pi-hermit." ... I'd say we have arguably already done the
> first pass of that, not that it's wrong or right, but e.g., the
> file:///home/joe/code/futon6/data/mission-efe-field-embed.html followed by
> the ~/code/powerbi-tui/hyperreal-freelancing.pdf diagram that abstracts
> that into features.  That's not quite a representation of a way of working
> — but e.g. Rob *also* uses Flexiargs and Missions in his work, and we have
> a shared way of working even though we are working as a loosely-coupled
> rather than highly coordinated system (and indeed, that might be the
> *best* way for us to work, even if we were trying to run a company
> together).  The other thought in this direction is that perhaps the only
> reason we *think* that what we're talking about could work is that we have
> (jointly and severally) made considerable progress on modelling the way
> the *business* of doing mathematics works.  It's entirely possible that
> another month, 3 months, 6 months, or a year of that would give us some
> really significant and transferrable breakthroughs.  But this then
> immediately puts some pressure on "modelling the way we work" because a
> month-to-year spread is lots of room for drift.  Anyway, I'd say the major
> strategy point in war-bulletin-14 would be to do with sorting out these
> coordination aspects.  Charlie has proposed daily standups, Rob may be
> willing to join some or all.  But the difference between standups and
> coffee chats has to do with a surrounding workflow.  So, sort off like
> M-interim-director I think we might want something like M-becoming-nomad
> or something so that I can have a plan for this month

— 2026-07-30T12:32:51.187364074Z, evidence id
`emacs-a6933b4a545b7c7cbfaafdd3a74a7cc9`, turn `claude-1-turn-6` (claude-1
session). The mission file's "Why this name" section cites the
loosely-coupled sentence and the file records "opened 2026-07-30 by claude-1
at Joe's direction" — this turn is that direction.

### M-case-studies — A (one minute before the creating commit)

> Well, I almost agree but I want each problem taken one by one and closed
> in whatever order we pick.  So, construction-ready obstructions are OK but
> we should not then move on to another problem until the construction is
> done.  Having done 46% (maybe more if we commit and push) I am pretty
> confident that we can get to 100% but that's inevitably going to be done 1
> problem at a time.

— 2026-08-11T22:32:47.624071126Z, evidence id
`emacs-3af2099deb53d5bf31474e59f9cb5cc3`, turn `claude-1-turn-49` (claude-1
session). The commit one minute later is titled "M-case-studies: one problem
at a time, closed before moving on (Joe's strategy, draft for edit)".

### M-xenotype-its — A (four days before the sweep commit; the file itself
is dated 2026-07-28 and credits this session)

> Can you drop the M-xenotype-its notes into a document, then read ##
> Second derivative in M-zai-learning-loop ?  We could figure out how to
> reliably install the second derivative signals into sessions like this
> one that are doing a coordination role

— 2026-07-28T13:59:45.797316571Z, evidence id
`emacs-30582ef97fdebedd49a2d65cdac21c13`, turn `claude-6-turn-27` (claude-6
session). The mission file opens "**Status:** OPEN — **PROPOSED** (Joe's
design, this date, recorded by claude-6" — this turn commissions that
recording; the design words themselves were not retrieved within the query
budget (the "xenotype" top-50 held only one, earlier, definitional turn).

### M-latex-wysiwyg — B (describing words one and two days AFTER creation;
created in a "never staged" sweep commit)

> So, now we are well on the way to something that I definitely want.
> Buried inside the various futon* repos are several "products" for which I
> am the only user at present.  The Claude + Codex + Zai REPLs are one.
> Arxana is another.  It'd actually be kind of nice to know what these
> things are and see if we could split them out as a little show-case of
> installable, usable things.  Like, the other day I build most of a WYSIWYG
> LaTeX editor.  Its nice.  It adds the feature of voice-activated,
> pattern-mediated agentic AI corrections — so, it's probably the best
> WYSIWYG LaTeX editor in existence.  It took me about half a day to build
> it and another day or so to test it, but I haven't gotten bac

— 2026-08-15T14:35:54.636437205Z, evidence id
`emacs-fdea49b3bad76cb145d16f75276ed214`, turn `claude-6-turn-85` (claude-6
session). Truncated at the store's own preview boundary — the quote is the
preview, verbatim.

> So, regarding the *audio* features in WP3, it is worth mentioning that I
> have a few other audio-interface systems on the go.  #1 something called
> voxterm which is an attempt to use voice to communicate with Fable while
> it is coding (like I chat with Opus on the web app), which is new and
> doesn't yet work perfectly, and #2 a voice-activated WYSIWYG LaTeX editor
> which has been working quite well.  It's worth being aware of these (both
> use Whisper in the backend) before we jump into something with VSAT.

— 2026-08-16T18:35:22.278276446Z, evidence id
`emacs-72dc0f445b647f4bf82af0d927e63efb`, turn `claude-8-turn-36` (claude-8
session).

### M-exotype-xenotype-eoc — B (the underlying question, posed 19 days
before creation, in the MetaCA work the mission operationalises)

> So... the transport measurement is cool!
>
> I creating Wolfram numbers (or should that be Corneli-Claude numbers) for
> MetaCAs would be a nice outcome.  What's bugging me is that we don't yet
> have an *effective* way (sensu Turing) of noticing that something is or is
> not edge-of-chaos.  Do we even have a definition*
>
> I mean, I can look, and I can be wrong.  I don't know if we'll ever be
> able to say that something is *provably* edge of chaos in the MetaCA
> domain?  Or maybe they don't even have edge-of-chaos as it is normally
> defined!  So, this bugs me.
>
> But what we'd know, *if we had wiring diagrams*, and if they transfer to
> other domains... we will know if they work.

— 2026-07-15T17:13:16.144408409Z, evidence id
`e-7b6b3fe4-b782-470f-9c90-a3d8d97942a8`, turn `claude-3-turn-90` (claude-3
session). The mission ("can the substrate DISCOVER edge-of-chaos?") answers
exactly this question, but Joe never names the mission in any retrieved
turn; the only mission-naming operator turn is the later UI remark
(2026-09-27, claude-18-turn-21, "The popup for M-exotype-xenotype-eoc does
not explain what the white X on the node label means" — a UI bug report,
not a description).

### M-memory-retrieval — B (the commissioning handoff session, before the
commit, but the words only partly describe the mission)

> Hi, please do WS2 via a Codex handoff

— 2026-07-28T10:45:07.959288955Z, evidence id
`emacs-450894db4ade233b112ad17c8c55f6e1`, turn `claude-6-turn-4` (claude-6
session; the file is dated 2026-07-27 and credits "Joe's handoff" — WS2 is
the workstream this mission organises).

> Sounds good, we presumably also want to make sure we're not *just* doing
> the proofs but also mining new memories at this level (like we do for Zai)

— 2026-07-28T13:35:38.804561558Z, evidence id
`emacs-112b41c993d6d2f318d5f8035f83df81`, turn `claude-6-turn-22` (same
session). Neither turn states what the retrieval subsystem is for.

### M-wm-capability-claim — B (words about the mission exist, only later,
and are about ordering not content)

> OK so, just so you know, I have escalated M-diagramprover on Zone,
> because M-apm-demonstration has proved to be way more tedious to implement
> than I thought it would; M-wm-capability-claim should probably depend on
> those (pending) changes, otherwise we will get into the same kind of
> implementation frustrations with the War Machine (or worse, because the
> War Machine is complicated!).  With all that said, I'm not sure how much
> more we can do on our "capability" threads.  We've covered a lot of
> ground... actually, *that* itself makes me think of something else:
> file:///home/joe/code/python-practice/factors.html +
> file:///home/joe/code/python-practice/attention.html are aggregate, but we
> could

— 2026-08-15T18:11:23.815235949Z, evidence id
`emacs-e876c6efd78484ce0cf9dcae7ce9904c`, turn `claude-6-turn-117` (claude-6
session; truncated at the store's preview boundary). No pre-creation
operator turn was found that says what the War Machine should be able to do.

### M-librarian — C

The file entered git during the 2026-08-14 laptop-return cleanup (an
assistant turn in session e67171d9-5ada-4dd5-aea8-7a1fc9fdc453 lists
`futon4/holes/missions/M-librarian.md` as untracked that day, and the commit
"M-librarian: commit the mission" lands there). The session's only user-role
turns retrieved are relays. The mission file says it was "Chartered
2026-08-10 from a 2026-08-09 operator remark during invoice-202506
planning", but no such remark was retrieved within budget (queries:
M-librarian, Steam Frame, 3D editing, session-scoped "untracked"). The
2026-05-29 Steam Frame turn found ("I can imagine coming in to the office in
the morning, putting on a Steam Frame, and navigating a new set of
constellations...") is about WM→VSATARCS storytelling, a different use of
the same hardware idea, so it is not quoted as a passage about this mission.

### M-xtdb-22x-benchmarking — C

The mission file says "authored same day as the Henderson call" by claude
(Fable session), "Driver: Joe". Henderson-related operator turns exist
nearby (from 2026-07-29, about XTDB issue #5637 outreach, and 2026-08-15
about the call itself), but none of the retrieved turns describes or
commissions the benchmarking mission.

## What I could not tell

- **Earliest operator turn seen in this batch's queries**: 2026-05-12T14:27:55Z
  (the xenotype-definitions turn, claude-? session; retrieved under
  q=xenotype). The pilot's absolute earliest in the store (2026-02-18,
  author=joe walk) still stands and was not re-measured here.
- **Which fields identify an operator turn**: as in the pilot — body
  `event "chat-turn"` + body `role "user"` + `evidence/author "joe"`. New
  caveat from this batch: some emacs-claude-repl entries carry
  `:evidence/author "Joseph Corneli"` (full name) rather than `"joe"`,
  including assistant turns in the same transport — so author spelling alone
  is even less reliable than the pilot noted; the role field is the
  discriminator and relays (bell-dispatch text typed as user turns, e.g.
  "claude-10 lifecycle-shape amendment…", or "joe: …⇐auto-bellback" bodies)
  had to be excluded by inspection.
- All ten missions predate the on-disk operator-turn files (first stored
  local turn 2026-08-22), so source (b) contributed nothing; the store was
  the only recorded source, and its operator turns plainly reach back to at
  least May for these topics — the "created before first stored turn" era
  label in the batch input refers to local files only, not to the store.
- The text index is relevance-ranked with limit 50 and no paging (by
  discipline); operator turns beyond the top 50 of a query were not seen.
  Two quotes end at the store's own ~preview boundary and are marked as
  such; they are the preview, verbatim.
- For M-xenotype-its and M-memory-retrieval the substantive DESIGN words Joe
  spoke in the 2026-07-28 claude-6 session (the mission file credits them)
  were not retrievable within the 4-query budget; only the commissioning and
  scope words quoted above were found.
