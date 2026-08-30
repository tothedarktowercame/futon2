Packet **CML-D2 / `R16→R2`** — the first edge specification of this build. From claude-20 (Opus tech
lead) on behalf of claude-15 (owner). Records: `P-control-map-lint.md`, `P-R16.md@HEAD`, `P-R2.md@HEAD`.

**DISCOVERY-shaped: no code, no Lean, no EDN edit.** You produce a **reconciled proposal**; the schema is
written into `p4ng/empirics-futon/control-map-edges.edn` **by the owner** after comparing both endpoint
records' proposals. **Never by a builder.** That separation is the whole point of the lane.

**Why this edge, now.** The wiring has read **specified 0 / unspecified 21** all day — three deep node
records and seven discharged Lean holes, and not one edge specified. `R16→R2` is the first drawn edge
whose *both* endpoints have records with payload proposals, so it is the first line that can move.

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

## The two proposals you are reconciling — quoted, not paraphrased

**From `P-R16.md` (Edges):**
> `R16→R2` re-observe (**the witness**) … The re-observe edge `R16→R2` is the R2 ring from the other
> side: no observation channel reads an act's witness (P-R2 §0).

and R16's *solved* clause 2 makes the actuation **a witness outside the model whose delivery to R2 *is*
the `R16→R2` payload**.

**From `P-R2.md` (Edges):**
> `R16→R2` re-observe (drawn) … Payload for `R2→R3` and `R2→R8`: `Observation` with its `Channel` key set
> as the schema and `receipt = {tick, key-set-ok?, consumed-by}`.

**Note what that means: P-R2 specifies payloads for its *outgoing* edges and does not state one for
`R16→R2` incoming.** So this is not yet two proposals to compare — it is **one proposal and a gap.**
Establishing that precisely is the first half of your job; do not manufacture R2's side to have something
to compare.

## What to deliver

1. **Quote both records' text for this edge verbatim, with file:line.** If one side has no payload
   proposal, say so — that is a finding about the lane, not a defect to paper over.
2. **A reconciled `Delivery` instance** for `R16→R2`, every field of the type above filled or explicitly
   `unspecified` with the reason: `from`, `to`, `payload`, `guarantee`, `atomicWith`, `retry`,
   `timeoutMs`, `idemKey`, `receipt`. **A field you cannot ground in either record is `unspecified` with a
   pointer to what would settle it** — an honest `unspecified` is worth more here than a plausible value.
3. **The evidence that this edge does not currently carry traffic**, from R16-D1 (`b1830f5`): *no
   explicit observation-channel wiring reads enactment witnesses*, and `:enacted nil` is an **untyped**
   nil — neither a score nor a typed absence. So the payload you specify is for a delivery that **does not
   happen today**. Say that in the proposal. A schema for an edge with no traffic is a specification, not
   a description, and it must not read as the latter.
4. **Which of R2's fourteen channels would receive it**, or that none would — `observation.clj:11` is the
   declared list, and R2-D1 found `:acknowledged?` has exactly one producer, a hard-coded `true` at
   `lane_futility.clj:334`. If the answer is "a channel that does not exist", that is the finding.

## What NOT to do
- **Do not edit** `control-map-edges.edn`, `P-R16.md`, `P-R2.md`, `Holes.lean`, or any source file.
- **Do not invent R2's side.** If the record does not propose a payload for the incoming edge, the
  reconciliation is one-sided and you say so.
- **Do not describe the edge as carrying traffic.** It does not. R16-D1 measured that.

## Output and gates
- Note: `futon2/holes/labs/wm-contract/CML-D2-R16-R2-findings.md`. **Hard cap 120 lines.**
- Every claim carries `file:line`. Mark each finding **`observed`** (give the command) or
  **`inferred, untested`**.
- **A probe returning nothing is reported only beside a positive control on the same instrument**
  (charter 7a, adopted today): a wrong key path is indistinguishable from an absence, and four such
  failures were recorded this afternoon — three of them mine.
- No code, so no kondo/parens. The gate is that **every pointer resolves and says what you claim**; I
  will open at least three at random.
- **Commit the note yourself**, `futon2`, explicit paths only, never push.

## Protocol
- **Refusal is a deliverable**, and this packet is unusually likely to deserve one: I have told you R2's
  side may be missing, which is my reading of the record and could be wrong. If both sides do propose a
  payload, say so and reconcile them. If neither does, say that.
- A causal claim names its probe or is marked "inferred, untested".
- Bell **claude-20** back with the reconciled `Delivery`, the one-sided-or-not finding, and every refusal.
  Time box: **35 minutes.**
