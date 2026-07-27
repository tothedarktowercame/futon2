# M-digital-nomad-patterns — a pattern language for moving on the professional landscape

**Status:** IDENTIFY (2026-07-27). **Owner:** Joe + claude-4.
**Spawned from:** the zaif-transplant application (p4ng appendix) plus the
landscape probe — the observation that the probe's output is a *terrain*, and
navigating a terrain is what pattern languages are for.
**Kept out of the paper for now** (Joe, 2026-07-27); tracked as another
application of the same machinery.

## HEAD

The deep-research landscape position plays the role **Futon City** plays in
the paper's motivating picture: a landscape. This mission takes
`~/code/TN-deep-research-landscape-position-2026-07.md` and its
`...FINDINGS-2026-07-27.md` as the terrain input and asks *"what do we do
about that"* — with the answer expressed as a small pattern language for
movement, authored below (Joe, 2026-07-27, verbatim), rather than as a
one-off plan.

## Inputs

- `TN-deep-research-landscape-position-2026-07.md` — the probe spec (six lanes:
  capital, durability+digestion, informal proof-checking, learning loops,
  funders+collaborators, verification).
- `TN-deep-research-landscape-position-FINDINGS-2026-07-27.md` — the terrain:
  45+ org roster, the capital-flood shape, the digestion gap, and the
  held-out-control finding (depintel: invisible under field-framing,
  adjacent under thesis-framing — the room is bigger than the roster).

## The patterns (Joe, 2026-07-27)

**Choose The Grid.** *Context:* you're being scored and losing. *Forces:*
scoring functions are fixed per-venue and cheap to change venue. *Solution:*
stop optimising the score, change which function applies. *Failure mode:* a
grid you win on that nobody is holding.

**Live Yardstick.** The guard on the pattern above. The audience must
already be measuring, with money and a problem, before the fit means
anything.

**Owner Of The Problem.** *Context:* your value is compositional and no ad
describes it. *Forces:* ads are specifications of pre-funded slots; problems
live with individuals holding discretion. *Solution:* route to the person,
not the posting. Instrument is a proposal, not an application.

**Skip The Flood.** *Context:* your field just got capitalised. *Forces:*
capital converges on the layer with the nearest enterprise exit and vacates
the adjacent ones. *Solution:* occupy what it's skipping — orchestration,
digestion, evaluation — which also survives the next release from the people
it's flooding.

**Self-Certifying Artifact.** *Context:* gatekeepers use subjective proxies.
*Forces:* judgement is expensive, so it gets proxied; proxies encode
disciplinary claim. *Solution:* produce outputs that carry their own
validity — a merged PR, a verified proof — so no judgement intervenes.

**Author The Yardstick.** The strong form. When the field's admitted
bottleneck has no measure, the measure is the position, and vendors are
structurally disqualified from building it.

**Letter From The Future.** *Context:* you don't know what to build.
*Forces:* building is expensive, specifying is cheap, and buyers are only
discoverable by addressing them. *Solution:* write the letter you want to
send in two months; every clause that isn't yet true is a work item.
*Variant:* write it to three addressees — invariant sentences are your
highest-value build, divergent ones are named bets.

## First observations (terrain ∘ patterns)

1. **The held-out-control miss is a live Choose-The-Grid datum.** Under
   field-framing ("AI for mathematics") depintel is invisible; under
   thesis-framing ("auditable, attributable instrumentation of reasoning")
   it is adjacent competition/company. The probe itself demonstrated that
   changing the grid changes the room — and the FINDINGS §0 consequence
   ("the competitor set was drawn by field, not by thesis") is the pattern's
   first work item: redraw the roster by thesis.
2. **Skip The Flood is the probe's L1/L2 shape read as strategy.** Capital
   converges on prover-adjacent layers with enterprise exits; digestion,
   evaluation, and orchestration are the vacated layers — and they are
   exactly where the learning-loop machinery (receipts, typed memories,
   audit trails) already lives.
3. **Self-Certifying Artifact is what the loop produces natively.** A
   Lean-checked solve with its receipt chain is the artifact that carries
   its own validity; the zaif appendix row is one sentence away from being
   a portfolio item under this pattern.
4. **Author The Yardstick ∘ Live Yardstick is the proofcheck-readiness
   position** (futon6 entry point): the field's admitted bottleneck
   (checking informal mathematics at scale) has no measure; whoever builds
   the benchmark holds the grid — subject to the Live Yardstick guard,
   which is the open empirical question (who is already measuring, with
   money?).
5. **Letter From The Future is the executable next step** — the pattern
   that generates work items from the terrain. Three addressees suggests
   itself from the FINDINGS: one to a funder lane (L5), one to a
   collaborator lane, one to the thesis-framed adjacent room the control
   revealed. Invariant clauses across the three = the build; divergent
   clauses = the named bets.

## Machinery note (application, not build)

These are patterns in the store's strict sense — trigger-class, with
contexts, forces, and failure modes — and the domain has real witnesses:
Letter From The Future clauses are checkable work items; Live Yardstick is
money-measured by definition; Self-Certifying Artifacts certify themselves.
If this mission matures, the patterns can be minted into the store under a
:positioning domain and accrue use receipts like any others (the
mathematisization-of-X criterion, applied to careers). Not built now;
recorded so the option is priced.

## Next moves (Joe-gated)

1. Run **Letter From The Future ×3** against the FINDINGS terrain (cheap,
   specifying, generates the work-list).
2. Redraw the competitor/room roster **by thesis** (Choose The Grid's work
   item from §0 of the FINDINGS).
3. Inventory existing Self-Certifying Artifacts (the loop's receipts and
   solves; merged PRs; the p4ng appendix) against the three letters.

## Test case (2026-07-27): the Gowers engagement requirement

The FINDINGS addendum records the live test the patterns must pass: a
landscape peak (Gowers: Cambridge chair, Fields, KBE) holding a funded
version of the neighbouring thesis. Non-competable grid; the patterns must
deliver ENGAGEMENT capability instead — engage-the-peak via complementary
self-certifying artifacts + page-one concession (Gowers archetype), or
climb-the-adjacent-ridge via assembled signal stack + upward network
composition (Carina Hong archetype). This is the mission's first concrete
acceptance test: can the pattern language generate the Gowers letter and
the Hong-style plan, and do they differ in the ways the patterns predict?

**Refinement from the test case:** the Gowers contact is warm-but-faded, and
direct approach is the failure mode. Candidate eighth pattern — **Licensed
Ground** (guard on Owner Of The Problem, as Live Yardstick guards Choose The
Grid): engage where the convening itself licenses the conversation; arrive
by contributing what the convening needs; letter addressee = the organizers.
Instance: Big Proof lineage at the INI. Work items in the FINDINGS addendum.

## Canonical home (2026-07-27)

The patterns now live as `futon3/library/nomad/*.flexiarg` (8 files, house
style, names generalized — this mission doc retains the sensitive instances).
Note: futon3a's nightly index (04:30 cron, --minilm) sweeps the futon3
library, so the nomad patterns enter the shared search corpus automatically.

## B-series: the business-landscape embedding (design, 2026-07-27)

A SECOND embedding, distinct from the mission index but coordinated with it:

- **Coordination = same encoder** (miniLM), distinct corpora → the spaces
  are cross-queryable by construction (mission artifact ↔ nearest business
  items in one call). Separate refresh cadences (business intel decays in
  weeks; :as-of dates + staleness policy required).
- **Cross-correlation = the evidence-shapes.** Each nomad pattern's
  evidence-shape is the record schema for one class of business intel; the
  corpus is built ATTACHED to patterns from birth, not correlated after.
- **Bootstrap = a scribe pass over the FINDINGS TN** (B0): atomize into
  typed records instantiating the shapes (org/grid/layer/vacancy/convening/
  yardstick-gap items), each tagged with its nomad pattern(s).
- **Staged build:** B0 atomize (codex, drafts) → B1 sibling index (biz
  corpus JSON + same encoder; sibling of index_patterns.sh) → B2 refresh
  discipline (probes update records, not documents; receipts = replies,
  invitations, grants).
- **Loop symmetry, priced:** TN=transcript, atomize=scribe, records=memories,
  nomad patterns=trigger-classes, letters=dispatches, replies=receipts.
  Positioning is WM-able by the demarcation criterion; Joe is the send gate
  everywhere (cold-outbox discipline).
