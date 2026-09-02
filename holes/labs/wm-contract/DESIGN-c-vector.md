# DESIGN: the C vector — two C's, one per loop

claude-1 with Joe, 2026-09-02 evening. Joe: "I'd like to get it to be as
complete as possible tonight... we should take very seriously anything that
looks like a defect at the level of this C vector... I really like your
suggestion of two C's, one per loop." This document answers P-validated-R5
Q3 ("what is C?") as a PROPOSAL for Joe's confirmation, in the DERIVE
discipline: every non-obvious choice carries IF/HOWEVER/THEN/BECAUSE.

## 0. The finding the design rests on

C is not absent from this machine; it exists at the wrong grain.
`futon2/src/futon2/aif/preferences.clj:193 c-distribution` builds NORMALISED
per-channel preference densities — `[lo hi]` range specs with exponential
tails, `{:becomes b}` / `{:p1 p}` Bernoulli targets with temperature — and
risk is a proper KL against them. But the channels are the machine's own
proprioception (sorry-count-norm, support-coverage, ...). The theory's C, and
every arc that hit a wall today, needs preferences over OUTCOMES AT MISSION
GRAIN. That is the recorded S1 refusal — "a C over channels offered as a C
over outcomes" — and it is one question behind seven doors (§4).

## 1. The two C's, named and separated

- **C_int** (interoceptive): the existing R19 stack. Preferences over the
  machine's own health channels. Grain: the machine loop. UNCHANGED by this
  design — it is correct for what it is.
- **C_mis** (mission-grain): preferences over the outcomes of the CLOCKED
  mission — what the world should look like if this mission advances.
  Grain: the mission loop. NEW; this design specifies it.

IF the machine has two nested loops (R15; the lifecycle alignment's three
timescales), HOWEVER a single C must otherwise serve both the machine's
self-maintenance and every mission's ends at once, THEN C is a family
indexed by loop grain, with C_int and C_mis composed only at the boundary
where a tick scores mission actions (§3), BECAUSE a preference that cannot
say WHOSE preference it is reproduces exactly the channel/outcome conflation
the S1 refusal recorded.

## 2. C_mis: the carrier is the mission's own completion criteria

The lifecycle already demands what C_mis needs: IDENTIFY must state
"testable conditions — how will we know it's done?" The alignment table
(futon4 mission-lifecycle-wm-alignment.md §2) already says IDENTIFY deposits
the C-vector. The hand-built exemplar exists:
`holes/labs/zaif-harness/runs/S4-identify-ingest.edn` types three criteria,
each with `:measurable-by`.

**The mapping**: each completion criterion declares
`{:criterion <id> :observable <how it is measured> :spec <c-dist spec>}`
where `:spec` is one of the EXISTING shapes: `{:becomes 1}` for
green/holds/published criteria (most of them — "U-rows green" is literally a
Bernoulli target), `[lo hi]` for threshold criteria, `{:p1 p}` for
empirically-set preference mass. C_mis(mission) is then the FACTORED density
`{observable -> (c-distribution spec)}` — the same factored form C_int
takes over channels, built by the same constructor.

IF criteria vary wildly in kind, HOWEVER inventing a new preference algebra
tonight would orphan the audited machinery, THEN v1 reuses `c-distribution`
per criterion verbatim and composes by weighted log-sum (uniform weights
default, `:criterion-weights` a declared per-mission override), BECAUSE the
per-channel risk sum already composes exactly this way and the reuse is what
makes the Lean statement small (§5).

**Unmeasurable criteria fail loud**: a criterion with no `:measurable-by`
contributes NOTHING silently — it produces a typed
`{:criterion k :status :unmeasurable}` record on every read (C130
discipline). A mission whose criteria are all unmeasurable has NO C_mis and
selection must see that as a typed absence, never as flat preference.

## 3. Where C_mis binds: the clocked mission parameterizes risk

app-zaif commitment 1, until now asserted and unbuilt: "G is parameterized
by the clocked mission, whose preferences enter as the risk term's targets."
The build: when the tick's S4 focus read returns a clocked mission,
`risk_mis(π) = Σ_k w_k · KL(Q(o_k|π) ‖ C_k)` over that mission's criterion
observables, added to the candidate scoring for mission actions — behind a
declared input, default off, flip J-gated, per the day's discipline.

**The honest hole, named not papered**: Q(o_k|π) — the forward model at
mission grain — does not exist yet. v0 of the binding therefore scores
CRITERION DISTANCE: risk against the CURRENT measured value of each
observable (equivalently Q(o|π) = status quo), which already yields the
compliance gradient Joe asked for — the clocked mission's unmet, measurable
criteria pull G toward actions that address them, and a mission whose
criteria are met stops pulling (completion-gate from first principles).
The real Q(o_k|π) is the cascade catalog's job (S6 §3: precedent as the
playout record) and is NOT built tonight. IF v0's status-quo forward model
makes risk action-insensitive within a mission, HOWEVER waiting for the
catalog blocks the whole binding, THEN v0 ships distance-only with the
limitation stated in the registry entry, BECAUSE a binding that exists and
understates is repairable, while an asserted binding that doesn't exist is
today's recurring defect class.

## 4. The seven doors, each with its disposition

1. **Selection risk blind to missions** → C_mis binding (§3), rows U11/U12.
2. **No compliance channel** (surprise ledger frame) → the clocked mission's
   C_mis IS the binding pin; operator priority = punching the clock.
3. **U4 ambiguity possibly inert over channel-C** → re-run U4 against
   C_mis-bearing fields once U12's corpus risk exists; outcome (c) of U4's
   typed results becomes testable.
4. **zaif arms had nothing to KL against** → zaif's mission criteria (from
   M-zaif-harness-v1's ingest) give the arms a real C_mis; the planted R4/R8
   layer in U6 gets its re-grounding path (S7).
5. **Lean `C` hole** (Holes.lean:153) → §5 states the intended definition;
   closing stays owned by P-validated-R5 §2a.
6. **Tally row `:c-cost-vs-distribution`** → §6 is the bridge; the row
   closes when U13 records it with pointers.
7. **`preferenceStackLiveRecorded`** → the C_mis records U11 writes are
   exactly the live preference-stack evidence the attestation wants;
   close-by-record once U11's records exist on a real tick.

## 5. The Lean statement (intended shape, not tonight's proof)

`C` stops being one function and becomes the family the code now mirrors:
a grain-indexed assignment of normalised densities over DECLARED observables
— `C_int : Channel → Density` (exists as data today) and
`C_mis : Mission → Criterion → Density` with the factored-product
composition and the `:unmeasurable` refusal as a typed constructor. The
current hole `def C (v : Vertex) ... : Obs v → ℝ := sorry` is the
channel-grain half stated too widely; the repair is to split it as above.
Owner unchanged (P-validated-R5 §2a); this section is the design input the
sorry was waiting for.

## 6. The scalar bridge (defect tally, class 7, last open row)

The bridge statement: **a scalar payoff is an affine image of log-C at the
outcome the action targets.** Where the codebase carries a bare scalar
(zaif's arm constants, ROI figures, `operator-attention-cost`), that scalar
is a DECLARED PRIOR standing in for a log-density evaluation whose C has not
been declared yet — legitimate, but typed as `:scalar-awaiting-density`
rather than passed off as C itself. No scalar is deleted; each is either
(a) re-derived as log-C_mis once its mission's criteria exist, or (b) kept
with the type tag. That is the reconciliation the tally row demands: not
choosing between cost and distribution, but stating the function between
them and typing every place it has not been applied yet.

## 7. Falsifier (stated before any flip)

Over the S7 corpus (the three 2026-09-02 tick records, plus any run
tonight): with M-zaif-harness-v1's ingested criteria and M-eoi's criteria
(U11 extracts them), mission-grain risk computed per candidate must
(a) reproduce deterministically from record fields alone, (b) DISCRIMINATE:
the clocked mission's advance actions must carry lower risk_mis than
non-mission actions on at least one recorded tick with named numbers, and
(c) go to zero as criteria are met on a planted field where all criteria
read satisfied. Failure of (b) on all fields = the status-quo forward model
is too weak, and the row that flips anything on reverts.

## Rows minted tonight (wm-contract): U11 build, U12 measure, U13 record.
