# Item 19c: E on the live strategic path — discovery and decision sheet

2026-09-09 · codex-17 · for claude-1, packet
`invoke-1788967865285-16514-757732f0`. Note only; no implementation, registry,
worklist, run configuration or live process changed.

Basis: futon2 `af896a9fce81aa275fa6d59e2a59c1eeb71c6680`, futon3c
`0f4ec84094207aad485cb478f69f56829fe5411b`. File pointers below are reread at
those checkouts. The packet's policy line 306 and war-machine line 4276 are
historical; current sites differ substantially.

## Finding and arm answer

**Item 19c does not reduce to flipping the controller's flag.** The final
reason-bearing strategic selector replaces the controller action. It receives
no numeric scheduler ln E, beta/tau or posterior. It already has a separate
strategic E_S calculation, fitted from frozen July selection events, not the
persisted scheduler-action habit. Meanwhile the production beta carry still
passes neither ln E nor a placement option and therefore uses `:none`.

The two registered placement arms differ in the beta solve whenever nonuniform
ln E makes their independently constructed posteriors differ; application of E
to selection is **not** required for that numerical distinction. They are not
two different E terms in the final selection expression. At fixed tau both
would supply the same selection ln E. They can affect downstream selection only
through an explicitly connected arm → beta → tau → deciding score path. That
path does not reach today's final reason-bearing choice. Therefore no different
LIVE selected action is established by enabling the controller flag alone.

The registry's `:ships-behind-flag :habit-prior-in-both` is an explicit interim
and source-fidelity recommendation, not an operator adjudication. Item 19c
explicitly leaves the choice open. Return the two-arm sheet below to Joe, along
with the additional grain/selection-boundary question. Do not turn a measured
beta difference into a claim that an arm selects better.

Authority: `futon2/holes/labs/wm-contract/RULINGS-walkthrough-2026-09-09.md:117-123`;
registry entries at `futon2/holes/labs/wm-contract/aif-equations.edn:317-321`
and `futon2/holes/labs/wm-contract/aif-equations.edn:346-386`.

## 1. Producer, consumer and final-decision census

### Scheduler-action habit: learned and attached, not final strategic authority

`futon2/src/futon2/aif/habit_prior.clj:26-36` identifies categories by action
type plus target/target-class. `:70-89` folds selected-action frequencies;
`:100-118` computes normalized predictive probabilities, sharing category mass
between duplicate menu entries; `:121-163` attaches ln E, replacing rather than
adding to any caller structural bias. This is habit, not outcome quality.
The optional span cap is default-off and requires an explicit decision to alter.

`futon2/scripts/futon2/report/war_machine.clj:6000-6019` reads the source mode
and persisted state/trace seed. `:6353-6360` attaches learned priors before
further filtering and ranking. The later selection population therefore needs
an explicit alignment/normalisation account: do not silently treat the original
menu and final admissible menu as identical. `:6595-6600` deliberately preserves
the prior unchanged instead of training scheduler habit on strategic choices;
its comment calls for a distinct E_S learned from reviewed strategic events.
`:6765-6766` persists the state. Enabling application must not silently restart
training on an incompatible event grain.

### Controller selector: three distinct rules, not one Boolean

| Boundary and rule | Actual choice | Habit claim |
| --- | --- | --- |
| `:strategic-recommendation`, default `:controller-head` | First non-no-op of G-ranked entries; posterior is recorded, not used to choose. | `:habit-prior-applied? false`. |
| Same boundary, `:full-score-posterior` **and** F_pi entered | First argmax of `ln E - G/tau - F_pi` (with declared F_pi scaling); excludes no-op when controller entries exist. | `true`; live in this controller score. |
| `:actuation`, nonzero prior vector | Last argmax of `ln E - G/tau`, then a G-unit abstain test against no-op. | `true` on the resulting non-abstaining decision. |

Current sites: `futon2/src/futon2/aif/policy.clj:503-536`, `:555-635`,
`:789-865`. The flag is derived at `:618` from `full-score?`, whose condition is
at `:582`; it is not an independent switch. Missing per-tick F_pi makes a
requested full-score law fall back to controller-head with an explicit refusal.
The flag-chain config check is separate, in
`futon2/scripts/futon2/report/war_machine.clj:352-380`. Calling that fallback
silent would repeat an error already corrected by the F8 review.

The F8 `:action` entry is in the registry's **equations vector**, not its
`:choices` map: `futon2/holes/labs/wm-contract/aif-equations.edn:183-191`.
Its `:lean-note` records these different rules, opposite tie breaks, no-op and
abstention differences, and the separate `close-loop!` first-passing-gate
actuator. An E-on implementation cannot borrow the actuation branch, change
candidate exclusions, or enable F_pi and call that only a habit change.

### Final reason-bearing selector: the intervening authority

`futon2/scripts/futon2/report/war_machine.clj:6468-6484` builds the scheduler
comparison ranking of three allowed mission ids and calls the strategic
selector with that ranking, controller ranking and trace id. `:6509-6525`
replaces the action and labels the final boundary `:reason-bearing-strategic-policy`.
`:5844-5868` updates some selection-law metadata, but does not recompute the
inherited habit flag or make scheduler ln E govern the returned policy.
Consequently even a controller `true` flag would be insufficient evidence for
the final action; provenance must name the boundary at which E was applied.

The actual selector is not merely an opaque callback:
`futon3c/src/futon3c/peripheral/live_wm_selection.clj:125-140` takes the first
ranked Phase-7 strategic policy. `:333-350` loads a frozen Phase-7 fixture and
passes the scheduler ranking as comparison data. Its
`futon3c/src/futon3c/peripheral/strategic_policies.clj:124-179` fits E_S from
strategic selection frequency and explicitly excludes tactical events;
`:211-265` ranks by `ln E_S - G_S/temperature`, breaking equal probability by
policy id. `:289-315` gets events, alpha and shadow-case settings from the
fixture. The canonical fixture is
`futon3c/holes/labs/M-typed-memories/phase7-strategic-policy-shadow.edn:1-33`,
marked frozen July 23, with alpha 0.5 and strategic/tactical event records.
It is not a live read of the persisted scheduler counts (whose alpha defaults
to 1). Do not merge those two distributions because both are named habit.
This is a source-path finding, not a live HTTP/JVM experiment.

## 2. The pi-zero arms, current wiring and decision for Joe

The solver constructs its own distributions in
`futon2/src/futon2/aif/policy_precision.clj:33-48`:

| Registered arm / solver option | pi | pi-zero | Interpretation |
| --- | --- | --- | --- |
| habit-prior-in-both / `:both` | softmax(ln E − F_pi − gamma G) | softmax(ln E − gamma G) | Both distributions share habit; their difference isolates adding F_pi, conditional on E. |
| habit-prior-in-pi-only / `:pi` | Same pi | softmax(−gamma G) | Difference also reflects habit relative to a habit-free baseline. |

The residual uses `(pi - pi-zero) dot G`, with gamma = 1/beta
(`futon2/src/futon2/aif/policy_precision.clj:50-61`). Solver default is `:none` (`:219-260`), not
`:both`. `futon2/scripts/futon2/report/war_machine.clj:588-595` currently calls `carry-beta` with identity
and score accessors only. `futon2/src/futon2/aif/policy_precision.clj:518-545` aligns F_pi/G and
forwards options to the solver; there is no automatic extraction/alignment of
candidate ln E in that call. The recorded interim is not evidence of live wiring.

`futon2/src/futon2/aif/policy.clj:85-147` uses beta as tau only under
`:variational-beta-gamma`. The other temperature modes do not acquire arm
sensitivity merely because E is applied. Even with variational tau, controller
head selection is invariant to that posterior; the final strategic selector
uses its own fixture temperature. These are distinct unconnected steps today.

**Two-arm decision sheet:** Joe has already ruled E-on, but not this arm.
Option A is to retain the explicitly recorded `:both` interim for a bounded,
opt-in RUN4 comparison, preserving `:open-branches` until adjudicated. It has the
registry's SPM-source-fidelity rationale and the recorded near-cancellation
measurement. Option B is `:pi`, matching the printed habit-free pi-zero formula;
it changes what drives beta and needs that interpretation stated in the run.
The existing U3 record reports 0/288 argmax differences on its bounded replay,
not superiority or universal equivalence. Its fallback/source-fidelity argument
is a recommendation; Item 19c does not convert it into a final decision.

Before either option can be called live, settle which same-grain posterior is
to consume beta, whether F_pi is actually supplied, and how that relates to the
already authorised reason-bearing law. If that decision retains an E_S/G_S
selector with fixed temperature, the placement arms remain a separate beta
experiment; applying E_S does not require pretending they have become a live
strategic discriminator. No scheduler-to-strategic grain mapping is inferred here.

## 3. Proposed implementation shape — after scope/arm review

An explicit **run opt**, default absent/off, should control the new application
and propagate all the way to the final selector, following the existing explicit
opts/flag approach. Proposed envelope (names are proposals, not new registry):

`{:habit-application {:enabled? true :grain ... :source ... :placement ...}}`

Presence with false must override an environment flag. Unknown source, grain or
placement must refuse before the broad controller fallback, so an invalid E-on
request cannot silently become an E-off run. Do not flip global source mode,
selection law, F_pi flag, temperature mode, alpha, span cap or abstention policy.

Smallest coherent path **if confirmed by Joe/reviewer**: keep the reason-bearing
strategic selection law and its admissible policies, and supply it with a pinned,
reviewed strategic-event E_S source in place of fixture habit. Reuse
`fit-strategic-habit` and its tactical-event exclusion. Establish a durable event
reader/write-once update at the reviewed strategic-selection boundary, including
restart/deduplication and pre/post counts. That needs a concrete source of real
strategic events; no such reader is identified in the inspected final-selector
path. If the ruling instead intends scheduler-action E to choose the mission,
that requires an explicit grain mapping and selection-authority decision. It is
not the smaller path of changing a flag in `policy.clj`.

For an authorised beta-connected variant, align ln E by the **same durable
identity and retained population** as F_pi/G, rather than passing the original
full-menu vector after a partial join. Forward the selected `:pi`/`:both` option,
record the solver's convergence/bracket/hold result, and wire its beta to the
same deciding policy domain. Do not use scheduler beta for G_S without a model
and ruling that justify the shared quantity. Off-path diagnostics remain labelled
as such. Learned priors replace structural bias; they are not added twice.

Each final decision should record:

- Requested/enabled/applied habit configuration, boundary, grain, source digest,
  policy identities, counts, alpha, update event ids and pre/post state pins.
- Actual applied ln E per candidate, menu/multiplicity and any declared cap;
  G, F_pi if present, tau and their sources; deciding score and posterior.
- Requested/applied selection law, eligible order, exact ties, winner and tie
  rule; separately the controller recommendation, final strategic policy and
  enacted action/receipt. `:habit-prior-applied? true` only where the actual
  deciding expression consumed E. “Applied” and “changed winner” are separate.
- If beta arms are active: placement, aligned identities, beta prior/posterior,
  gamma, residual, convergence/bracketing, held/absent reason, and the consuming
  temperature. If not consumed, explicitly say diagnostic-only.

### Required tests for Phase 2

1. Disabled opts preserve existing outputs; explicit false overrides flags;
   unknown/incoherent modes refuse before fallback. No default flip.
2. Use real-shaped candidate/event fixtures: multiple policy grains, duplicate
   menu identities, missing F_pi, partial alignment and actual policy identities.
   No history reset on malformed persisted data; no tactical-to-strategic count
   substitution; no duplicate training event on replay/restart.
3. A nonuniform-prior strict-margin example changes the **final** decision;
   a uniform prior is a control. Removing the final-consumer wire must fail the
   test, even if the controller still reports E applied.
4. Solver :pi/:both differ on nonuniform E, coincide on the uniform control;
   recorded held beta is preserved on failed solve. Test both variational and
   fixed-temperature paths and report non-discrimination where it occurs.
5. Preserve each boundary's existing tie rule. Strategic controller uses first
   maximum; actuation uses last; the final strategic policy uses policy-id order.
   Do not route through another boundary to obtain E. A tied fixture must not be
   counted as an E-effect; report equality separately from a strict winner change.
6. Follow the final chosen policy to the authoritative run actuator. The legacy
   first-passing-gate path and an action-record change are not interchangeable.
   Mock effects for tests; a later authorised run supplies the real witness.

## 4. Executed bounded probes (not a live run)

Pure production functions were loaded in a separate `bb -cp src` process; no
war-machine namespace, live selector, HTTP endpoint or actuator was invoked.
Output: `/tmp/E-live-discovery-probes-final.txt`, exit 0.

Two entries a/b, G=[0,0], normalized E=[0.5,0.5], tau=1,
F_pi=[0,0] for the full-score strategic branch:

| Branch | Chosen | habit-prior-applied? |
| --- | --- | --- |
| Strategic full-score | a | true |
| Actuation prior branch | b | true |
| Strategic controller-head | a | false |

This reproduces the opposite tie maxima with identical input entries, not an
improvement due to habit. For the pure solver, beta-prior=1, G=[0,1],
F_pi=[0.2,0.8]:

| E | Placement | beta-posterior | gamma |
| --- | --- | ---: | ---: |
| [0.1,0.9] | pi | 1.378917934412231 | 0.7252063194219414 |
| [0.1,0.9] | both | 0.8697486285837353 | 1.1497574898489475 |
| [0.5,0.5] | pi | 0.9050070878470322 | 1.1049637217526675 |
| [0.5,0.5] | both | 0.9050070878470322 | 1.1049637217526675 |

All four solves converged and bracketed. Synthetic probes establish reachable
function differences and a control, not a live policy or superior arm.
An earlier preliminary probe used unnormalised log weights; the table above
uses normalized probabilities and supersedes that scratch output.

## Delivery limits

Phase 1 is complete. Phase 2 needs the reviewed grain/source/final-boundary plan
and Joe's disposition of the arm sheet. The note does not claim that persisted
scheduler E, fixture E_S and a beta-arm choice are interchangeable. It does not
claim a current runtime snapshot from source inspection alone.

Pointer ranges resolve; `git diff --check` passes. Markdown only: clj-kondo and
Lisp check-parens are not applicable. No source or signed evidence changed.
Initial searches tried the packet's implied `src/.../war_machine.clj` location
and a singular strategic-policy filename; both were corrected to the actual
paths above. Those failed lookups were not evidence of absent implementations.
