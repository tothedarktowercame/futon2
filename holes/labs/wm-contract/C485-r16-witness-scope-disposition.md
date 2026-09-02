# C485 — R16's witness scope: is C78's refusal the repair?

Worklist `:D61b` (class `:V`). Disposition, with pointers, of the Figure-4
tally instance `:r16-engine-wiring` — class 3, *witness measuring the wrong
object* — at `defect-repair-tally.edn:28`, whose evidence field read
"P-R16.md:111-118; outward actuation remains distinct".

**Boundary held.** Nothing here arms, wires, or proposes arming an outward
effect. No `:decisions` and no `:choices` entry was written. No generator was
run into a publish.

## 1. What has to agree for this instance to close

Two objects are in play.

- What the witness *measures*: `engine-wiring` shells the futon3a fold engine
  and returns an in-model construction result (`enact.clj:130-160`).
- What the claim *said* it measured: an outward act that R2 re-observes —
  `Actuation := {mission, act, witness : ExternalWitness}`, the R16→R2 payload
  being that witness (`P-R16.md:42-45`), the edge labelled "re-observe"
  (`P-R16.md:62`).

`P-R16.md:110-113` is where the two were found not to agree: under R9's own
predicate (`r9_independence.clj:13-15`, `futon2/checks/`) the payload's
independence verdict is `:self` while re-observation requires `:independent`,
so "the clause is not currently satisfiable".

A class-3 instance closes exactly one of two ways: move the witness to the
object the claim names (build the actuator), or move the claim to the object
the witness measures (re-scope to construction). `P-R16.md:119-124` states
this as a fork and hands it to Joe — (a) build, (b) rename.

## 2. C78 is not the repair, and this is the finding

C78 (`C78-outward-act-refusal.md`) is a **refusal to build**. It records that
no authored binding supplies the three required ports and declines to invent
one. A refusal changes what the machine *does not do*; it does not change what
any artefact *says*. So C78 cannot be the repair of a wrong-object claim, and
the tally's counting rule — ":repaired requires a named commit, record, or
re-verified artefact" (`defect-repair-tally.edn:5-8`) — is not met by it.

What actually moved the claim was a different, dated action three weeks earlier
in the same day's work: **p4ng `199252b`, 2026-08-31 10:21:26Z, "Correct R16 R2
edge to construction observation"**. C78 is what keeps that re-scoping from
being reversed by fiat: it names the falsifier ("locate an existing authored
binding and current consumer satisfying all three ports") and the census found
none.

C78's own evidence pointers re-verified today, one drifted:

| C78 cites | state 2026-09-02 |
|---|---|
| `enact.clj:130-147` | holds — `engine-wiring` at `:130-160` |
| `enact.clj:243-285` | holds — `enact!` at `:250-292` |
| `observation.clj:18-31` | holds — the fourteen channels are at `:18-32`; none is an act-witness channel |
| `belief.clj:970-1015` | holds — `channel-emission-matrix` `:969-984` and `channel-health-signs` `:1003-1020`, the eight likelihood channels; no act-witness verdict enters |
| `trace.clj:317-330` | **drifted** — that range is now support-typed scoring. The claim ("the writer and the claimant are the same boundary") still holds at `trace.clj:582-594`, where `:realized-outcome`, `:act-gate-verdicts` and `:enactment` are persisted |

## 3. Claim-site sweep: where the wrong object was asserted, and what each says now

Withdrawn, with the old claim preserved as dated history:

| site | now |
|---|---|
| `control-map-edges.edn:23-27` | label `"observe construction"`; `:label-amendment` dated 2026-08-31 carries `:old-label "re-observe"` and the reason |
| `control-map-edges.edn:28-33` | payload retyped to `:construction`/`:absence`, no witness field |
| `control-map-edges.edn:34` | schema-note: "AMENDED 2026-08-31: ExternalWitness/re-observe was a false outward-act claim" |
| `hyper-edge-schema.edn:115-124` | `:ExternalWitness` survives only under `:amendment :old-claim` |
| `edge-fragments/_control-map_R16-to-R2.edn:63-71` | same shape; `:ports [:R16/external-witness :R2/reobserve]` is history, the live ports are `:R16/construction-result` and `:R2/construction-observation` (`:16`, `:35`) |
| `enact.clj:130-133` | docstring: "Returns a typed result: `:constructed-wiring`, `:no-construction`, or `:enactment-failed`" |
| `enact.clj:283-292` | the persisted `:enactment :result` is that union |
| `p4ng/sec-catalog.tex:309` | "Second revision (2026-08-31)": the enactor "is the mirror this pattern warns against… So `P-R16` §solved-2's claim of 'a witness outside the model' is unsatisfiable as coded" |
| `aif-equations.edn:142-145`, `:262-265` | the `:action` row and the `[:R6 :R16] :path-dependent` hole make no outward-act claim |
| `DECISIONS-PENDING.md:73-74` | "R16 remains honestly refused as outward actuation, while its typed construction result continues to operate" |

Never asserted in Lean — recorded as **not found**: `P-R16.md:75,97` named
`GroundingRegistry`, `ExternalWitness` and `actGate` as the declarations to
ratify first. `GroundingRegistry` and `ExternalWitness` have **zero** hits in
any `.lean` file repository-wide. `actGate` does exist —
`Holes.lean:97-101` — and it is a two-leg score predicate over
`cascadeScore`/`coverageScoreDelta` returning `pass`/`fail`/`abstainMissingLeg`,
which claims nothing about outward effect. So the Lean side never carried the
wrong object either.

Still asserting the old object:

- `P-R16.md:62` — the Edges section still names the edge `R16→R2 re-observe
  (**the witness**, above)`, the label the map amended away on 2026-08-31.
- `P-R16.md:42-47` — clause 2 still states the outward-act contract as R16's
  requirement. This one is arguably correct as written: it is a standing
  obligation with a falsifier, and `P-R16.md:109-113` marks it CONTESTED with
  the refutation adjacent. Withdrawing it is fork option (b), which is Joe's.
- `control-stages.edn:20` — the R16 node label is `"Grounded actuation"` with
  no amendment, while the one edge leaving that box in Figure 5B now reads
  "observe construction". The box and its edge disagree.

## 4. The replacement schema has drifted from the code it describes

The re-scoping retyped the payload, and then the code moved out from under the
new type within the same day. Two named defects, both live at 2026-09-02:

**(i) A reason keyword the code does not emit.** All five registry locations
carry `:no-construction {:reason :engine-returned-nil}` —
`control-map-edges.edn:31`, `hyper-edge-schema.edn:50`, `:87`,
`edge-fragments/_control-map_R16-to-R2.edn:14`, `:44`. `:engine-returned-nil`
has **zero** occurrences in any `.clj` or `.lean` file. The code emits
`:engine-returned-no-wiring` (`enact.clj:147`) and `:empty-cascade`
(`enact.clj:262`).

**(ii) A third union arm the schema does not admit.** The code's union has
three arms (`enact.clj:133`, `:283-292`); the schema's has two everywhere. A
tick whose enactment throws or whose engine exits nonzero produces
`:enactment-failed` (`enact.clj:148-160`, `:290-292`), an arm the R16→R2 edge
contract does not describe.

The chronology is exact and is what makes this a drift rather than an error:

| when | what |
|---|---|
| p4ng `199252b`, 08-31 10:21:26Z | schema authored **ahead of the code** — `engine-wiring` then returned "the enacted wiring map or nil" with no union at all; `:engine-returned-nil` was the schema's own name for the nil case |
| futon2 `ae88c55`, 08-31 12:59:57Z | "Preserve typed enactment failures in trace records" — the code adopted the union and the keyword verbatim (`:engine-returned-nil` at that commit's `enact.clj:238`). Schema and code **agreed** |
| futon2 `09200bf`, 08-31 13:38:19Z | "Keep engine failures distinct from no construction" — the code split the keyword and added the third arm. The registries were not updated |

Drift window 3 h 17 m; open ever since. Nothing caught it, and the reason is
stated in the checker itself: `pointer_check.bb:6-8` — it "does NOT check that
the pointed code says what the field claims".

**And the amendment's own citation has drifted.** All five locations cite
`futon2/src/futon2/aif/enact.clj:113`. That line today is an escrow-unreadable
warning `println` (`enact.clj:110-114`); `engine-wiring` is at `:130`. This is
the lesson `P-R16.md:115-117` recorded on 2026-08-31 — "line-level citations
require a dated or commit-pinned reading context" — recurring inside the
artefact that fixed the previous instance of it.

## 5. Disposition

**`:partial`.** Named subset, per the counting rule.

*Repaired subset:* every consumer-facing claim site. The outward-act claim
("ExternalWitness", "re-observe") is withdrawn from the drawn map, the
hyperedge schema, the edge fragment, the executor's docstring and its
persisted result, and the paper's pattern entry, with the old claim preserved
as dated history at each. Named commit: p4ng `199252b` (2026-08-31), with
futon2 `ae88c55`/`09200bf` on the code side.

*Not repaired, and blocked:* `P-R16`'s own text. Clause 2 (`P-R16.md:42-47`)
and the Edges label (`P-R16.md:62`) still state the outward-act contract, and
the fork that would settle either way (`P-R16.md:119-124`) is
**DECISIONS-PENDING #2** (`DECISIONS-PENDING.md:61-76`) — Joe's
safety/authority call. Option (a) builds the actuator; option (b) withdraws the
clause and shrinks the machine's claim. Neither is D61b's to take.

*Not repaired, not blocked:* §4's schema drift and §3's `control-stages.edn:20`
label. These need no ruling.

## 6. The minimal witness re-aim that touches no actuation

Stated as the acceptance asks, **not applied here** — the schema and stage
files feed Figures 5A/5B, so they are class-D drawing/registry corrections
under TN §6 and fall under the §9a second-reader gate; D61b is a `:V` row and
the loop's rule 4 forbids regenerating into a publish.

1. Correct the union in all five locations: replace
   `{:reason :engine-returned-nil}` with the two reasons the code emits
   (`:engine-returned-no-wiring`, `:empty-cascade`) and add the third arm
   `:enactment-failed`. Re-pin the `:cite` to `enact.clj:130-160` — or, better,
   to `enact.clj` with a commit sha, which is what `P-R16.md:115-117` asked for.
2. Give `control-stages.edn:20` a dated amendment in the same shape the edge
   got, so the R16 box and its outgoing edge stop disagreeing in Figure 5B.
3. Give `P-R16.md:62` the same dated amendment, so the specification and the
   drawn map agree on the edge's name.

None of the three satisfies clause 2. Clause 2 is satisfied only by fork
option (a), which is DECISIONS-PENDING #2.

**What would *not* be a re-aim**, from C78's own analysis: adding a second
reader of the construction result. "A second file-reading function would be a
separate reader, not an independent witness." Re-aiming the witness at a new
in-model artefact reproduces the defect one level down.

## 7. What a reviewer should check

- That §4(i)–(ii) reproduce: `grep -rn 'engine-returned-nil' --include=*.clj .`
  returns nothing, and `enact.clj:147`, `:262`, `:283-292` say what is claimed.
- That the three commits' timestamps and messages are as tabled.
- That `GroundingRegistry` and `ExternalWitness` have zero `.lean` hits.
- That the `:partial` subset in §5 is the right cut — the alternative reading
  is `:repaired` (the wrong-object claim *is* withdrawn everywhere a consumer
  reads it, and what remains is an unbuilt capability honestly marked). I took
  `:partial` because the counting rule resolves ambiguity toward the weaker
  status, and because §4 shows the replacement type is itself wrong about the
  object it now describes.
