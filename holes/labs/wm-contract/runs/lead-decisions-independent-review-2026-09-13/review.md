# Independent review of delegated execution decisions

Date: 2026-09-13  
Reviewer: codex-24  
Reviewed commit: `867565de906f5774c0187995cb2b0cdcf08c7622`  
Reviewed file: `holes/labs/wm-contract/LEAD-DECISIONS-2026-09-12.md`  
Disposition: **accepted as delegated execution direction, with four blocking
specification/acceptance findings below**. This review grants no implementation,
commissioning, witness, admission, or full-run credit.

## Review method and retained-audit validation

This was a read-only review of the committed decision, its two retained audits,
the sources named by those audits, Item 5's completion rule, and the edge
disposition sources. Existing executions were validated from their receipts and
pinned bytes; no test or probe was rerun.

### Row 18 controller-sensitivity audit

The receipt at
`runs/row-18-lead-audit-2026-09-12/execution-receipt.json` reports exit 0 at
futon2 `845ad99695616b9ecfe9425e6ef1477160b888a0`, 147 candidates, clean kondo
and parens, and the following retained hashes, all independently recomputed and
matched:

* `gain_probe.bb`: `8ac55b0f02e3c79d17eb31e82a80caab0172496408a248d1758d0f445af94723`
* `input.edn`: `1818dd14eabe7274815e1298b7d59909b06f98d853db52a84ec1f3e229f20235`
* `result.edn`: `8193604566d620cda2a3a29856dd9efa6a47b51421652d2077100816fd8f97fd`

The receipt's full daily-trace hash also matches the retained source named by
the probe. The probe honestly establishes only controller-score replay on one
147-candidate production-shaped field: both applicable engineering modes move
weights/entropy/temperature; `:variational-beta-gamma` is invariant; a base
gain of 0.005 is floor-saturated at both multipliers 1 and 1/2. It does **not**
establish a complete live posterior, trip-rate calibration, safer behavior,
ranking movement, selection movement, enactment movement, or a full run. Its
input has no usable complete run envelope, so those nonclaims are material.

### Row 24 predicate counterexample

The receipt at
`runs/row-24-lead-audit-2026-09-12/execution-receipt.json` reports exit 0 and
prints only `[propext, Classical.choice, Quot.sound]`; it reports no `sorryAx`.
The following exact pins were independently recomputed and matched the receipt:

* `CurrentPredicateCounterexample.lean`:
  `ee8bc03c6e4e5786a9b103efe04ea1716af3f5b24a0689aed475aa08ef47f2fd`
* imported `CertificateStates.lean`:
  `83bb26577a0f949dc318ad71c3908ed52392c23ae32be8d329519e4829352acb`
* imported `CertificateStates.olean`:
  `7898a5f4b484506118f3b7095f21358981c0d52473aad2beadd15d5434589ff1`
* Lean toolchain pin:
  `33cb1eef6946140512bf870633253eec2d5c5e14211006f9de53311c45a15bb6`

The imported source makes `qualifyingRun` depend on a nonempty bytes string,
the reading, and node positivity, but not record-family consistency,
mandatory-edge firing, selection/enaction acceptability, or negative-scope
legitimacy. The elaborating counterexample supplies gaps, a mandatory-unfired
edge, refused selection, and an unruled exclusion while satisfying that current
predicate. This is a sound narrow counterexample to the precursor predicate,
not a full-run certificate claim.

### Confirmed policy boundaries

The retired legacy R8 scalar-producer obligation is the only stated negative
scope. Its original J2 source pin
`2fed9f7c5d4a3c375807dab5e0e3f24c82852bbbd9cef949a2fac8d7c22479da`
at futon2 `845ad99695616b9ecfe9425e6ef1477160b888a0` was validated. The decision
does not waive R8 as a node or the live `F_pi` path. The row-24 policy also
correctly forbids whole-node exemptions and requires positive closure for a
qualifying run; honest typed partials/refutations remain census facts rather
than positive qualifying evidence.

## Actionable findings

### F1 — Row 18 must refuse saturated or inapplicable commitment links

Severity: blocking before row-18 commissioning/admission.

The decision says floor saturation is reported rather than credited, but its
acceptance language does not yet force a refusal when an open genuine trip is
claimed to modulate the controller and the actual applied gain/temperature is
unchanged. The retained control demonstrates this exact case at base gain
0.005. Likewise the variational mode ignores task gain by construction.

Required repair: the composer/checker must emit a typed
`:modulation-saturated` (or equivalent) non-qualifying result whenever `k>0`
and the composed value is absorbed by the floor, and a typed inapplicable-mode
result for `:variational-beta-gamma`. Neither may count as a fired commitment
link. A qualifying row-18 run must use an applicable mode with enough headroom
to lower the **actual applied** gain, while retaining the unchanged
task-gain/posterior separation. This does not require claiming that ranking or
behavior becomes safer.

### F2 — Row 19 delegated acceptance needs an authority root outside its own candidate record

Severity: blocking before genesis acceptance.

Delegation authorizes codex-26 to make the technical decisions; it does not
make a mutable branch name or a self-authored acceptance map an authenticated
genesis root. The decision correctly rejects `{:status :anchored :authority
"..."}` shape checks, but “operator-owned canonical branch” is not by itself a
verifiable identity primitive. A checker whose candidate inputs include both
the delegation assertion and its acceptance remains circular.

Required repair: pin an independently retained operator/delegation event (or
another genuinely operator-controlled signature/append-only authority record),
then bind the branch acceptance, author job, distinct reviewer job, review
commission digest, source/test bytes, and predecessor checker to that external
root. Delegated acceptance must remain labeled as such; no Joe signature is
required or to be fabricated. Branch membership alone is insufficient.

### F3 — `R7->R14` is already a ruled specification error, not required positive work

Severity: blocking before treating the seven class-(c) strokes as one required
implementation batch.

The decision's conditional escape (“unless source-level review establishes a
mistaken connection claim”) is already discharged for `R7->R14` by the sources
it governs. `p4ng/empirics-futon/control-map-edges.edn:250` records J1's ruling
that the drawn edge conflates channel precision with selection gain and directs
work toward theory-aligned gamma; line 310 says to retire the edge as drawn.
`holes/labs/wm-contract/aif-equations.edn:300-302` preserves that retirement.
The row-22 audit itself calls out this conflation. Requiring firing evidence for
the retired drawn identity would contradict the specification.

Required repair: remove the exact drawn `R7->R14` identity from required
positive firing and represent the theory-aligned gamma obligation under its
correct declaration/connection identity. Before implementing the other six as
a batch, audit each endpoint at declaration grain (not merely R-number labels),
especially `R11->R16`, whose hierarchical-budget and actuation carriers are
not interchangeable. This is specification repair, not an aspirational scope
waiver; valid replacement obligations remain required.

### F4 — A latent estimator cannot silently become measured categorical A authority

Severity: blocking before measured-A admission, not before retention continues.

The refusal to treat unique argmax of a near-uniform belief as observed
categorical state is sound. The alternative phrase “or a reviewed estimator
that handles latent-state uncertainty,” however, could allow a model-derived
fractional/latent estimate to be relabeled as the contract's measured
status-at-close counts. That would evade Joe's observed-estimate authority and
the no-invented-mass/no-smoothing rule.

Required repair: observed categorical records are the authority for measured
A. If they do not exist for a row/cell, that measurement remains typed absent.
A latent-state estimator may be retained only as a separately named
model-derived proxy with its own authority, uncertainty and correspondence
witness; it cannot discharge the measured-A cell without an explicit compatible
authority ruling. Measured zeros remain zero and zero-data rows remain absent.

## Review disposition and next dependency

The row-18 and row-24 retained audits are valid at their expressly narrow
scopes. The no-full-run, no-whole-node-exemption, and legacy-R8/live-`F_pi`
boundaries are confirmed. Commit `867565de` is usable for continued execution
only with F1–F4 treated as blocking acceptance constraints, not optional review
notes.

Next dependency: lead records the four clarifications/spec repairs (especially
the retired `R7->R14` identity and row-19 external authority root) before
dispatching the affected admission packets. No existing source execution was
rerun and this review receipt itself is not independent admission evidence for
any node.
