# Rulings walkthrough, 2026-09-08 (morning)

Joe, in session with claude-1 (emacs-repl), answering the five-item
outstanding-decisions sheet presented after the codex/zai build loop drained
the board (181 done, 5 blocked, bulletin 745bdfa1). Joe's words are quoted
verbatim, typos preserved; each item records the transcription taken.

## Item 1 — accepted-red entries REFUSED; tickets in a dependency-first DAG instead

> "only really 'accepted' if they appear in a live workplan, otherwise they
> are just part of an ambiguous workplan. What we could do if not doe
> alreadyvis write Tickets for outstanding items, and organise them as a
> dependency first DAG, like I have done with the topology loop. That way we
> wiuld have a full picture of what to build when and would not need invented
> categories."

The "accepted-red" classification proposed for RED-COMPONENTS items 6-7
(sorry-category count, workspace mid-work dirt) is NOT taken. Instead: every
outstanding register item gets a ticket in the live workplan, and the workplan
is organised as a dependency-first DAG (the topology-contract loop at
apm-lean/holes/labs/topology-contract is the exemplar). No invented
categories; the full picture of what to build when. Transcription: worklist
row :U70 commissions the DAG organisation.

## Item 2 — receipt re-attestation ruling ACCEPTED

> "ok, and if receipts camed with a pinned sha, that would prevent staleness
> of this sort (though it wouldn't prevent drift if the lean changes, but at
> least that would become obvious)."

The proposed design is ruled: a lane drains the existing backlog of
positive-proof receipts whose pinned declaration text has drifted (37 strict
stale at last count), and the FLOW is fixed at source -- any slice that edits
a Lean declaration carrying a positive receipt re-emits that receipt in the
same slice, gated. Joe's note confirms the pin's purpose: the pinned sha does
not prevent drift, it makes drift obvious. Transcription: rows :U71 (the
at-source gate) and :U72 (the backlog drain).

## Item 3 — F11: refusal-becomes-pattern, a warrant to proceed

> "this sounds like a case where refusal-becomes-pattern would be ideal, and
> the pattern would act as a warrant to proceed with this implementation and
> gather evidence as NEXT STEPS that will support or modify it. That
> experience will be more useful than a ruling from me. (And this itself is
> looking like a meta-pattern we could use, as many needs joe situations are
> really needs evidence situations, and we can't get that as efficiently if
> we wait gated on me to say so.)"

The deliberate implementation refusal at
mathlib4/DarkTower/WarMachine/Holes.lean:264 (`find`) is not discharged by an
operator ruling and not left standing as a block: it is converted into a
PATTERN (refusal-becomes-pattern), and the written pattern acts as a warrant
to proceed with an implementation, with the evidence gathered as NEXT STEPS
that will support or modify the pattern. The dependent choices (:find-sorry
ruled to this disposition; :find-f2-receipt-carrier and :find-f4-reading stay
:observed-not-decided) are to be DECIDED BY THE EVIDENCE the implementation
produces, not guessed first.

THE META-PATTERN, recorded for the pattern library and the APM
design-principles list (claude-9's redline thread): many needs-joe situations
are really needs-evidence situations; gating them on the operator serialises
what evidence-gathering could parallelise. A ruling-shaped block should first
be asked: what evidence would decide this, and can a warranted slice gather
it?

## Item 4 — F12 exemplar proviso: ruled to a NEW disposition (f)

> "(e) is the best of the given options, but I would propose instead (f) to
> use these 3 existing examples as as a source of inspiration to find a real
> working example not on the list. Otherwise tge inly way to pass is
> saticficing, rather that building a real reusable tool. This is a great
> example fir tge 'What problems are we solving' paper and it should be
> written up there. Making snatch reproducible is a good warm up and bridge
> to the new naturalistic example."

The proviso on the ARM 6 carrier ruling is discharged by NONE of the five
C582 arms as they stand. Ruled disposition (f): use ants, mining and snatch
as inspiration to FIND a real working example not on the list -- a
naturalistic exemplar -- and exercise the choice on that. Of the listed arms,
(e) (discharge conditionally, divergence carried open) is the best, and (f)
subsumes its honesty without the satisficing: the aim is a reusable tool, not
a passed gate. Sequence: (1) make snatch reproduce its own run's organised
edge set -- the warm-up and bridge; (2) find the naturalistic exemplar; the
proviso discharges when the choice is exercised on it. The episode is to be
written up in the paper (p4ng/futon-2026.tex, "What Problems Are We
Solving?") -- transcription: row :U69.

## Item 5 — F10: the C580 §6 sequencing recommendation ACCEPTED

> "I accept tge recommendation"

C580 §6 (holes/labs/wm-contract/C580-F10-rider-gap.md): choose the seed's
computation boundary (C580 §5 decision i: EFE fold vs terminal close) BEFORE
deciding whether to restart the dormant cohort (decision ii). Accepting the
recommendation fixes the ORDER; it does not choose the boundary. F10 stays
:blocked, its blocker narrowed to decision (i) (with (iii), whether the rider
requires canonical Q(o|pi) scoring, alongside), and per the Item-3
meta-pattern the boundary question should arrive back at Joe with the
evidence that decides it, not as a bare choice.

## Item 6 — F10 decision (i): the seed is computed in the EFE FOLD

> "So this is very simple because I agree with you. The Fe fold is what we
> really need."

(Evening walkthrough, claude-1 surface.) Decision (i) of C580 §5 is ruled:
`:ruled-outcome-c` enters the EFE fold as a live preference layer that shapes
policy selection — the substance ("the machine acts on C"), not the
terminal-close letter. Consequences: the `:folded? false` declaration at
src/futon2/aif/ruled_outcome_c.clj:56-62 is to be flipped BY THE IMPLEMENTING
SLICE with this ruling as its citation, and the fold grain is that slice's
first design question, returned with evidence if it is itself ruling-shaped.
Decision (iii) — whether the rider requires canonical Q(o|pi) scoring — was
NOT covered by this exchange (the recommendation Joe accepted was about (i)
only) and remains open; F10's blocker narrows to (iii) plus the fold
implementation.

## Item 7 — F11: extend the Lean find interface

> "And we want the signature extended."

The Holes.lean Tension/Repository interface is to grow the matching and
receipt operations the executable warrant requires (find_organise.clj:227-253:
caller-supplied `fires?` predicate; receipts that cannot drop :route or
:warrant). Lean edits are Joe's by name, so the extension arrives as a DRAFT
for Joe's review: C590-F11-find-signature-draft.md. F11 unblocks into the
draft-review slice.

## Item 8 — F12: the three futon3 inputs are committed as-is

> "And we should commit the files as is."

Committed unmodified as futon3 2695419 (two untracked flexiargs plus the
modified patterns-index.tsv). The cascade constructor's live-corpus read now
has a committed basis; F12 unblocks into the naturalistic-exemplar slice
against that commit.

(Also surfaced during the same walkthrough, not a ruling: RUN4 appeared on the
bulletin decision sheet despite its own surface-only-when-READY contract —
run4_readiness.bb said BLOCKED-ON [regenerates lean-probe closability-audit]
at walkthrough time. The bulletin's sheet should read readiness before listing
an :owner :joe row; queued as agent work.)

## Item 9 — PA17z and PA5z: settled by standing principle, ratified by delegation

> "I'm happy to go with your recommendations if you think that those are the
> right recommendations to make. But... In general, these look like ones that
> would be settled by standard principles. So for example, yes, author is not
> equal to reviewer is a standard principle around here. And we shouldn't just
> suddenly start making exceptions in the middle of work."

PA17z (content pins, v2 pointer schema) and PA5z (census scope v3 to include
the lane harness, so the seat-coincidence refusal can land) proceed on
claude-1's recommendations. The ruling of record is the PRINCIPLE, not the
particulars: author != reviewer is standing law and mid-work exceptions are
not made — which decides PA5z's fork (grow the scope, never weaken the
acceptance) without any operator preference being consulted.

ROUTING FINDING, same exchange: both rows reached Joe's sheet because their
blocker text was ruling-SHAPED while their content was principle-settleable.
Joe: "I'm not quite sure why they're coming to me for approval, because they
have effectively nothing to do with me or my opinions about anything." The
needs-joe channel should ask "does an existing recorded principle decide
this?" before surfacing; only genuine preference/priority/scope-of-authority
questions are his. Queued alongside the RUN4 readiness-before-surfacing
defect from Item 8's postscript.

## Item 10 — F10 decision (iii): the rider REQUIRES canonical Q(o|pi) scoring

> "Yes, require canonical."

The demonstration run's performance half is measured on the machine's real
predictive scorer (:forward-model, forward_model.clj:280-369), not a proxy —
same substance-over-letter ground as Item 6. FLAG CARRIED WITH THE RULING:
the canonical scorer path has standing F8 findings stated in Lean and left
for the I-track (gTermsDisagreeOnDepth: risk and homeostatic pressure at
depth K while ambiguity and the predictability bonus are frozen at depth 1;
epistemicTermsAreHorizonBlind: predicted variance is state-blind; four call
sites declare depths 2/3/3/5). The demonstration inherits these documented
properties; if an I-track fix later changes the scorer's depth semantics,
the demonstration is re-run rather than grandfathered.

## Item 11 — Box 2 becomes a Kanban with ownership; Box 6 holes name their closing packets

> "Wouldn't it be great to get it so that the paper was reporting not having
> holes in our lean formalism. [...] we had at one time talked about trying to
> get Box 2 set up so that it could work like a Kanban board, where things were
> being progressed from typed to formalized and witnessed. [...] there are 61
> things which have been typed but not formalized. [...] I personally don't,
> from this box two, have great visibility into where that sits in our work
> queue. It doesn't seem that different each time when I look at it. So maybe
> we should start to have not just those four columns, but a sort of sign-off
> on which packet those items are being carried in and moved along to the next
> stage. [...] it would be good to be organizing this thing as a DAG, so that
> we get some sense of the priorities for the next steps. And right now this
> Box 2 reads a little bit more like a slush."

Direction, not a technical ruling: the generated boxes gain an OWNERSHIP
dimension joined live from the worklist. Box 6: every live hole names its
closing packet and stage; unowned holes render loudly as UNOWNED (measured at
this walkthrough: wmRunsOnce and Strategic-mission-selection are the two
unowned; find->F11, organise->F12, the run-gated six -> U80/RUN4; C is a
deliberate refusal and should render as REFUSED-BY-DESIGN, distinct from an
open hole, so the paper's honest end state is nine closed + one explicit
refusal). Box 2: per family, "N moving, carried by <packet ids> / M unowned",
with packet order read from the dependency frontier so the box exposes the
DAG of next steps. Rows :U85 (Box 6) and :U86 (Box 2) mint this; the
never-changing-box complaint is the acceptance test — a worklist status
change must change the regenerated box.

## Item 12 — find: all four C590 choices as recommended; C: a calculation proposal, not more definitions

> "for find, sure, all four are recommended for find."

C590's open choices are ruled: inductive Route pinning structuredAntecedent;
Warrant as the standsOn descent chain (List P with chain proof); fires? in the
FindQuery wrapper (Tension's CLOSED-BY-RECORD marker survives); Prop-level laws.
Implementation dispatched.

> "the machine isn't going to invent my preferences, but [...] you can read my
> preferences off of all of the work that we've been doing [...] the keyword
> being discover, not invent. [...] C is to some extent a matter of a process
> of discovery. And that doesn't mean it should be indefinitely deferred. It
> means that it needs to include some discovery aspect. [...] What I don't yet
> have in front of me is a proposal about how this c-vector [...] is going to
> become something that turns into a calculation. [...] Rather than just keep
> pushing formal definitions around, we need to understand how that turns into
> a real evaluation. And that's focusing in on one specific part of the system
> and maybe developing some unit tests."

C's framing is corrected on the record: DEFERRAL WITH A DISCOVERY OBLIGATION,
not refusal. The response is C591-C-as-calculation-proposal.md: the missing
computational object is the disposition kernel P(d|o) bridging predicted
channel observations to the 12 outcome-kinds seeded-C is declared over —
discoverable from cohort records, not invented; then disposition-risk(π) =
KL[Q(d|π) ‖ seeded-C] enters G as the :ruled-outcome-c layer (Items 6/10).
Discovery loop reads the evidence landscape into provenance-carrying
proposed-C whose DIFF against the ruled seed is a decision sheet; the
extractor never writes preferences. Six unit tests named; four slices;
Joe reviews the proposal before slices dispatch.

## Item 14 — the section opens with what a preference vector IS and where it sits

> "Maybe the section could start with a little introduction to what is a
> preference vector, and how does it fit into the wiring topologically?
> Because in principle there could be lots of different preferences. I could
> have a preference for winning at Go, or I could have a preference for
> improving at Go. Playing one stone in front of a wall of three. [...] It
> would be helpful to know how often do we need to compute C? Do we need to
> return to computing C lots of times? Our preferences, as we talked about
> before, are they distributed across four different tetrahedral vertices
> [...] But other than that, the outline looks good."

Outline approved with a rung 0. The interpretive key for rung 0, stated here
so the draft can be checked against it: Joe's three Go preferences map to
three DIFFERENT slots of the formalism — winning at Go is a pragmatic outcome
preference and lives in C (the risk leg scores divergence from it); improving
at Go is epistemic value and lives in the ambiguity/information leg (computed,
never declared — the same logic as the evidence-vertex exclusion and the C
hole's original marker); the stone-in-front-of-a-wall idiom is a policy
habit and lives in the prior E. "Preference" in ordinary speech spans all
three; C carries only the first, and the section says so before anything
else. Vertices: C is declared per pragmatic vertex only (people, money,
organisations; evidence excluded by type, Holes.lean Vertex + the C
signature's v ≠ evidence premise) — Joe's money/society dimensions are
vertex-indexed, not new machinery. Cadence: C is CONSULTED at tick frequency
(every G evaluation reads the risk leg) and REVISED at ruling frequency (the
R13 two-timescale hierarchy: slow strategic loop updates preferences, fast
tactical loop acts against them; discovery-loop turns propose, rulings fix).
Topologically: C was literally not a drawn node in the PLOP control map
(sec-overview-plop.tex:29-30 says so) — this section draws it.

## Item 15 — processual preferences, and evidence as a legitimate outcome facet

> "outcomes have to sometimes be thought about in a processual way. [...] my
> preference would strongly be to have all those [APM] problems be solved. But
> along the way, I have the intermediate preference [...] to have the error
> codes be diagnosable [...] spreading out the preference for solving all the
> problems across the running of the machine, so that the running of the
> machine tends towards the solution of the problems. And even if that's at a
> different layer, they need to connect up, otherwise you'll sit around having
> a preference for solved problems and no way of getting there."

> "I thought it was interesting that you returned to the original crown
> triangle — money, people, and organizations — whereas the tetrahedral model
> abstracted that into nouns, verbs, organization, and evidence. I actually
> feel that evidence could be an important outcome [...] you also need
> evidence that they work, and that evidence needs to be in a communicable
> form. [...] if the c-vector only gets to talk about what outcome it is,
> that's a bit limiting in terms of understanding how you get there."

Two amendments to the section, both answerable inside the formalism:

(1) PROCESSUAL: da Costa's C is C_tau — preferences over observations AT EACH
STEP of the policy horizon; G sums risk over tau. So trajectory preferences
are native, and the channel C is RE-RANKED, not deprecated: it IS the
processual layer ("stay diagnosable, stay healthy along the way" — Joe's
error-codes preference is literally a channel target at intermediate tau).
The CONNECT-UP Joe demands is the disposition kernel read in the other
direction: P(d|o) values each intermediate state by where it tends, so the
terminal preference back-propagates over the running of the machine — the
"spreading out" is a Bayesian identity, not a metaphor. (Implementation
honesty: the F8 findings show our tau-sum is structurally shallow — the
epistemic legs are horizon-blind — so the formalism's answer is ahead of the
machine's, and the section must say which claims are formal and which are
running.)

(2) EVIDENCE FACET: the evidence-vertex exclusion bars preferring evidence
CONTENT (wanting the data to say X — wishful thinking; epistemic terms own
information value). It does not bar preferring that communicable evidence
EXIST — a witness, a receipt, a record in shareable form — which is an
ARTIFACT outcome, and this system already distinguishes witnessed from
unwitnessed groundings. Outcome kinds gain facets ("works well" vs
"works well AND carries a communicable witness"). OPEN QUESTION FOR JOE,
flagged not decided: the live Vertex type (Holes.lean) is
people/money/organisations/evidence, while Joe cites the tetrahedral model
as nouns/verbs/organization/evidence — which is canonical, and is the
divergence itself a row?

## Item 16 — the NEW tetrahedral vertex model is canonical

> "It's the new tetrahedral one that I want to use. The other one is a
> specialization of that, as I think you can see. The generalization to the
> new model is something I came up with that we were using in this paper, and
> it's based on an earlier model, which was itself an adaptation of something
> from Aaron Krowne, who founded PlanetMath, and it's written up in his
> master's thesis, I believe. But for our purposes, the new version is
> probably going to be more useful."

(Transcript said "Crown"; recorded as Aaron Krowne, PlanetMath founder.)
Canonical: NOUNS / VERBS / ORGANIZATION / EVIDENCE. The
people-money-organisations(-evidence) reading is a SPECIALIZATION of it,
via Joe's earlier model adapting Krowne's. Consequence: the live Lean
Vertex inductive (Holes.lean: people | money | organisations | evidence)
implements the specialization, not the canon — a divergence with scope
(the C signature's v != evidence premise, Outcome = Sigma Obs, seeded-C
vertex attachments, the paper's tetrahedron figure). The v != evidence
exclusion survives unchanged under either model since both carry the
evidence vertex. Reconciliation is a minted row, not a casual edit: the
Vertex change is Joe-gated Lean surgery rippling through the contract.

## Item 17 — GO on deriving C; Lean types to Codex now; routing doctrine

> "we really have pretty much everything we would need to start to derive a
> version of this C [...] that actually does what we need it to do. [...] In
> the Cascade Live page we have this overall problem statement written down in
> an induced form [...] because we can follow those inductive arrows back, we
> can see how that breaks down into work that's been done. And that breakdown
> is the same kind of concept as your C sub tau [...] the desired end itself
> was induced as a statement from looking at those things that have been
> achieved. [...] this is not a refusal. I don't think it's any more something
> that can even be stated as a refusal [...] What needs to happen is an
> organized project of working through those things. [...] when we're doing
> automated work, it may be impossible for an agent to work through all of
> those things right now without some help from me. And that's okay. We need
> to send the automated work to the automated loops, and we need the
> interactive loops for the ones where I really need to have an input. So
> that's another preference. But we can't sit here kicking the can down the
> road [...] parts of it that can be built now, which I suspect are the Lean
> types, need to be built right away, probably by Codex. And then we need to
> get around to turning those abstract or qualitative preferences into
> something that can be computed, across these different levels and across
> different timescales."

Consequences, executed tonight: (1) Lean preference-ladder types dispatched
to Codex as a standalone draft module (C_tau family, disposition-kernel
carrier, bridge identity, outcome facets; Vertex-parameterized pending U87),
including proposed replacement text for the C hole's marker — the
"deliberate implementation refusal" wording is no longer accurate by Joe's
own statement and becomes "deferral under an organized discovery project"
at his sitting. (2) F10 unblocks into the fold implementation (C591 slices
2-3) in the AUTOMATED lane per the routing doctrine. (3) The organized
project rows: U88 (Cascade-Live C_tau derivation — INTERACTIVE lane,
:owner :joe, first step pins the induced problem statement and its
inductive arrows verbatim), U89 (preference extractor, automated), U90
(Snatch mini-C, automated). The routing doctrine itself — automated work
to automated loops, operator-input work to interactive lanes — is recorded
here as a standing preference (an organization-vertex preference, fittingly).
