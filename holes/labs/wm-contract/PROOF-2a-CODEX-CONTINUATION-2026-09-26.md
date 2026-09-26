# PROOF-2a sequential continuation

codex-2, 2026-09-26, at Joe's direction while Claude's window is exhausted.
This follows the existing plan. Author validation below is not claude-8's
independent acceptance, a successful flight, or a scope ruling.

## Completed implementation packets

**WM-COVERAGE-FILE-I:** futon3c `7a5a9914`, `55e25258`, `f319732f`.
`wm_coverage.bb` accepts a ledger file as well as a committed revision,
hashes each file snapshot, and marks it uncommitted. Informational file
inputs require a separate output path; they cannot overwrite either input
or the canonical coverage companion. Revision-based generation remains the
publication path. The real-generator tests passed: 1 test, 23 assertions,
including changed input contents, revision/file equivalence apart from
provenance, and refusal cases. clj-kondo: zero errors/warnings. The Clojure
reader scan from `futon4/dev/check-parens.el` passed for both files.

**R14 output continuation:** the original job committed map `1713b872`
and organisation layer `9212230b`, then hit the window limit before finishing
the outputs. Its terminal Agency job status was not completion of its packet.
Generation initially refused three unplaced boxes already added by the F
packets. `f6ca9b37` adds them to the lane table: F-prefix supply and selection
candidate in selection; conditioning-step in the flight-record lane after
enactment. No refusal or map invariant was relaxed. The reader scan passed;
clj-kondo reports the same two unused-binding warnings as the parent, no
errors or new warnings.

`424a431d` regenerates wiring SVG, adjacency EDN/SVG and organisation layer
at map `1713b872`, with futon2 source `f4b99b54`. `289ebc48` pins the ledger
checks to that adjacency. `9666c455` regenerates the ledger and `401f18d5`
the coverage companion from committed inputs:

- 130 boxes, 170 wires; 193 organisation calls, 102/107 components with a
  caller; two unplaced organisation components remain recorded;
- ledger: **5 verified / 83 hermetic / 82 unverified**;
- coverage: **150 witness / 15 conditional / 4 failure-path / 1 unreachable**;
- 27 condition texts remain `:undecided`; classification retains their
  conditional obligations rather than assuming the successful path;
- the seven newly included wires remain unverified; no test or observation
  credit was inferred from adding their declarations.

The old ledger warrant was checked before execution, and refused
`:stale-sha` after the pin change. The refreshed namespace passed **4 tests,
532 assertions, zero failures/errors**, postcheck matched, warrant
`test-registry-3cfc1c76c91dbabf492dc58a364612c2a7f3b9d3c5cff08d339e0134506b73b8`.
The test's map/ledger/coverage join and planted bad cases remain enforced.

## Cτ returns checked against the acceptance text

CTAU-I landed registry rows at futon2 `cf6aca6af`, audit corrections at
`4c03446aa`, and the figure at p4ng `a13294bb`. CTAU-REC-I landed at
futon2 `26ecd057`. These are real deliveries, but do not complete ⟨1⟩4.

The CTAU-REC-I warrant refused `:stale-sha` at the current source. Its
namespace was run at `32d7e7c66`: **5 tests, 28 assertions, zero failures or
errors**, postcheck matched, warrant
`test-registry-f3117b139a2bedc4fb3bf81824c7242aa5262fb41a0e20f443352fd1bd1aeaca`.
This checks the actual scored placement, the contradictory-placement bad
case and unchanged scores; it does not test preference adequacy.

| Acceptance | Evidence and remaining work |
|---|---|
| Two preference families explicit | Registry rows `:preference-schedule` / `:class-preference-schedule` and Figure 6A nodes exist. Token C feeds the lane and constructor; class C feeds joint selection. |
| Diagram distinguishes conformance from tested/live consumption on each edge | **Incomplete.** `gen_aif_dag.bb` draws conformance classes but does not read the wire ledger. Registry `:live-status` and page notes do not supply the promised edge-by-edge evidence labels. Several C hops remain absent from the wiring map. Preserve that distinction when finishing the figure. |
| Actual placement recorded | `token-preference-schedule` reads the scored lane spec; `class-preference-schedule` reads the joint class model; lane receipts survive in the joint result. The registry's `:recorded-per-run` absence is now stale as an implementation statement, but an observed post-change flight remains absent. Refresh metadata without claiming such a flight occurred. |
| Constructor proposal grain | Source at `f4b99b544` proposes the supported family in one `:compose-by-need` move. The formerly untracked receipt is now archived at `32d7e7c66`; see the [receipt and its limits](runs/CTAU-constructor-2026-09-26/README.md). This supplies a historical construction example, not a progress witness. |
| Horizon relative to constructed work | The archived receipt has seven units, horizon 4, one construction move, and three wants explicitly `:beyond-horizon`. A move budget is not a prediction-step count. The measured-A/horizon discriminating test remains owed. |
| Joe's preference authority | 55/35/5/5 is **authored**, per `PROOF-wm-works-2026-09-22.md:388-390`. The audit already corrects this. Earlier dialogue/plan prose saying these class weights had no ruling must not be treated as current evidence. Class assignment is still unconfirmed; λ/μ authority remains a separate question. |
| Effective intermediate progress | The theorem's requirement and its representation are separate audit rows. No test here establishes later consumption of an intermediate token or adequate focus/excursion/return behaviour. This requirement is not closed. |

### Five compatibility checks promised in the dialogue

These were promised by claude-8 in job
`invoke-1790440504804-24990-b374ccb9`, but were not yet added to the plan's
acceptance paragraph. Carry them explicitly when judging modularity:

1. **Joint scorer consuming token C:** not established. At `f4b99b544`,
   `war_machine.clj`'s `class-observation-model` supplies class emissions;
   `cascade_observation_scoring.clj:144-160` explicitly branches between
   class probabilities and token-subset probabilities. Changing only C's
   producer is insufficient to demonstrate a compatible outcome domain.
   A proposed substitution must state the observation model, common domain,
   preference authority and discriminating score/selection test. This does
   not establish that a large rewrite is required either.
2. **Committed constructor receipt:** supplied, with the narrower historical
   claim above. The eight nested receipts must not be counted as eight clicks.
3. **Placement on the record:** implementation supplied by CTAU-REC-I;
   current test validation and a future observed-run claim are separate.
4. **Outer G inputs:** H-G-target part two remains the place to define them.
   No excursion/return policy is established by one-target flight control.
5. **Per-class reference mechanism:** must be checked against A-S Revision 3
   and its implementation. This continuation has not independently accepted
   those packets or asserted that a reference exists for a new observable.

The missing time-to-occurrence mapping remains implementation work. Its
absence is not a reason to call the corresponding preference optional.

## Next dependency in the node sequence: R6

WM-SUPERSET-D's old proposed new `r6-selection-posterior` box is already
present under `r14-selection-posterior` at map `1713b872`; do not duplicate
the same production site. The R14 comment records the remaining attribution
problems: the precision record is returned through `seal (merge ...)`, the
decision uses renamed destructuring, and the posterior destructures its
argument. The queued prover extension must justify those forms before the
temperature chain can be declared. A keyword in a docstring is not evidence
of a read. The already boxed containment-order path also needs comparison
with the old discovery before adding another R6 edge.

Thus the next linear packet is the existing prover work, with positive and
negative attribution cases and a map replay; R6 follows it. Neither a
duplicate box nor a weaker occurrence check is a completion of R6.
