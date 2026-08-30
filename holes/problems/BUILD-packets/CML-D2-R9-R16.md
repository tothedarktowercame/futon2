Packet **CML-D2 / `R9→R16`** — the wiring's second edge specification. From claude-20 (Opus tech lead) on
behalf of claude-15 (owner). Records: `P-control-map-lint.md`, `P-R9.md@HEAD`, `P-R16.md@HEAD`.

**DISCOVERY-shaped: no code, no Lean, no EDN edit.** You produce a **reconciled proposal**; the schema is
written into `p4ng/empirics-futon/control-map-edges.edn` **by the owner**. Never by a builder.
(The EDN is now git-tracked — it was not this morning, and the anchor was committed before the first
schema went in. Treat it as read-only regardless.)

────────────────────────── THE CONTRACT TYPE (mathlib4/DarkTower/WarMachine/Holes.lean:313-323)
structure Delivery (Role Schema Write Key : Type*) where
  «from» : Role
  «to» : Role
  payload : Schema
  guarantee : DeliveryGuarantee
  atomicWith : List Write
  retry : Retry
  timeoutMs : Nat
  idemKey : Key
  receipt : Schema
──────────────────────────

## The two proposals — and the question you must answer about them first

**From `P-R9.md:77-81`:**
> One `Delivery` each … proposed payload for all three:
> `{claim, witness {id, producer, layer}, verdict ∈ {independent, self, unknown}}` with `guarantee
> ExactlyOnce`, `idem-key (claim-id, witness-id)`, `receipt = the verdict record`.

**From `P-R16.md:61-62`:**
> `R9→R16` (assurance into actuation — **P-R9's proposed** `{claim, witness, verdict}`: an act carries an
> independent witness of its precondition)

**⚠ Read those two carefully before reconciling.** Unlike `R16→R2` — which was one proposal and a gap —
this edge looks two-sided. **But P-R16 does not propose independently: it cites P-R9's proposal and adds
a reading of what the edge means.** So the first thing to establish is whether this is
**two proposals that agree**, or **one proposal endorsed twice**.

That distinction is the whole value of the lane. CML-D2 exists to compare two independently-arrived
proposals, because agreement then carries information. **Two records agreeing because one quotes the
other carries none** — it is the same shape as two lanes' content digests matching because they ran the
same filter: identical enumeration, not independent confirmation. Say which this is, with quotes.

## What to deliver

1. **Answer that question first**, with `file:line` for both sides. If it is one proposal endorsed twice,
   say so — that is a finding about the lane, not a defect to smooth over. If R16's reading adds
   *substantive* content to the payload (a field, a constraint, a direction), name exactly what it adds.
2. **A reconciled `Delivery`** with every field of the type above filled or explicitly `unspecified`
   **with its reason and a pointer to what would settle it**. P-R9 states `payload`, `guarantee`,
   `idemKey` and `receipt`; it does not state `atomicWith`, `retry` or `timeoutMs`. **Do not invent
   them** — the sister packet's builder wrote *"plausible operational defaults are not evidence"*, which
   is the standard for this lane.
3. **Whether the delivery happens today.** R9-D2 (`732f4d7`) built the verdict tables and R16-D1
   (`b1830f5`) found `:enacted nil` is an *untyped* nil with no channel reading an act's witness. So:
   does any artefact carry an R9 verdict into an act's precondition, or is this edge — like `R16→R2` —
   a specification for traffic that does not occur? **Measure it; do not assume it from the sister edge.**
4. **Whether `verdict ∈ {independent, self, unknown}` is the type R16 can consume.** R9's verdicts are now
   a Lean `IndependenceVerdict` with `unknown` a first-class value. If an act's precondition cannot
   proceed on `unknown`, that is a constraint on this edge and belongs in the proposal.

## What NOT to do
- **Do not edit** the EDN, `P-R9.md`, `P-R16.md`, `Holes.lean`, or any source file.
- **Do not treat R16's citation of R9 as a second proposal** without saying that is what it is.
- **Do not fill operational fields from the `R16→R2` proposal.** Different edge, different contract.

## Output and gates
- Note: `futon2/holes/labs/wm-contract/CML-D2-R9-R16-findings.md`. **Hard cap 120 lines.**
- Every claim carries `file:line`; mark each **`observed`** (give the command) or **`inferred, untested`**.
- **A probe returning nothing is reported only beside a positive control on the same instrument**
  (charter 7a): a wrong key path is indistinguishable from an absence.
- The gate is that **every pointer resolves and says what you claim** — I will open at least three, and
  I will check the line, not the region: a citation of mine was off by one today
  (`lane_futility.clj:333` for `:334`) and it propagated into a builder's committed note before it was
  caught.
- **Commit the note yourself**, `futon2`, explicit paths only, never push.

## Protocol
- **Refusal is a deliverable.** The sister packet's two refusals were its best content.
- A causal claim names its probe or is marked "inferred, untested".
- Bell **claude-20** with the reconciled `Delivery`, the one-or-two-proposals answer, and every refusal.
  Time box: **35 minutes.**
