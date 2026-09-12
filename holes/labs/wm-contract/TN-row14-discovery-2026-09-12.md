# TN row 14 discovery: machine-Q link

Verified at futon2 HEAD `7ae9f82fae60e9bd20f2d364567ee3db3d97844c` on
2026-09-12. This note changes no production source.

## 1. The seam

Two outcome-producing families exist, but neither currently supplies the live
scorer with the row-6/9 machine Q.

The live path is `war-machine/judge` (`scripts/futon2/report/war_machine.clj:
5979-6011`) -> `efe/rank-actions` (`war_machine.clj:6391-6426`) -> one
`efe/compute-efe` per candidate (`src/futon2/aif/efe.clj:967-998`) ->
`forward-model/predict` (`efe.clj:629-642`; implementation
`src/futon2/aif/forward_model.clj:312-340`). That predictor returns continuous
14-channel means/variances and a next belief, not a distribution on the twelve
tagged outcomes (`forward_model.clj:315-325`). `compute-efe` consumes those
means/variances directly for channel risk (`efe.clj:646-701`). Its optional
ruled-outcome lane calls `disposition-risk/disposition-risk` with `next-mean`, a
separate disposition kernel, and C (`efe.clj:702-712`); that adapter constructs
Q(d|pi) from predicted channel observations (`src/futon2/aif/disposition_risk.clj:
99-128`). It is not the machine predictive-outcome kernel.

The source-module machine Q is already constructible off-line:
`machine-predictive/predicted-state-kernel` produces terminal Q(s|pi)
(`src/futon2/aif/machine_predictive.clj:61-76`) and
`predictive-outcome-kernel` composes it with A to produce twelve-wide Q(o|pi)
(`machine_predictive.clj:96-128`). A repository call census finds this namespace
only in its tests; the older, different `machine-q/predictive-outcome-kernel` is
used by `src/futon2/aif/node_sim.clj:352`, not by `efe` or `war_machine`.

The exact entry boundary should therefore be `efe/compute-efe`'s option map.
Today it receives `state`, one candidate action, and options including the
legacy `:disposition-kernel`, `:seeded-c`, and C provenance
(`efe.clj:474-594`). The build should add one admitted machine-Q row plus its
policy/model/support pins (or a function that returns that row for the exact
candidate policy), and score that row against the ruled machine C. The result
shape must add the Q row, C distribution, their shared support/pins, and the
machine-Q risk contribution; `:G-risk`/`:G-core` then change only when this
explicit lane is enabled. Passing only a scalar would make the claimed link
unreviewable.

## 2. A content

The production machine state carrier is exactly the seven statuses
`:spawned`, `:refined`, `:strengthened`, `:addressed`, `:falsified`,
`:foreclosed`, `:reopened` (`src/futon2/aif/belief.clj:37-44`), enforced by the
machine-model contract (`src/futon2/aif/machine_model.clj:119-130`). The outcome
support is the twelve organization-tagged full-loop dispositions, derived from
`full-loop-cohort/outcome-kinds` after excluding the two historical verification
labels (`src/futon2/aif/ruled_outcome_c.clj:22-32`) and exposed by
`machine-model/outcome-authority` (`machine_model.clj:13-16`). The twelve names
are at `src/futon2/aif/full_loop_cohort.clj:26-36`.

The current A is explicitly positional placeholder content. The constructor
sorts/uses the seven states' indices and sends state i one-hot to outcome
i modulo 12 (`src/futon2/aif/machine_predictive.clj:78-94`). The row-6 witness
states the same mapping as `positionalA` and derives the first seven sorted
outcomes (`holes/labs/wm-contract/runs/row-6-predictive-outcome/
generate_forward_witness.clj:8-16,40-48`). Row 11's committed registrations
label this `:content-status :placeholder-a-content`, for example
`runs/row-11-parameters/controlled-transition.edn:1`; their likelihood tables
therefore inherit placeholder content even though their registration pins and
normalisation are real. `machine_parameters/parameter-kernels` consumes those
registered state-to-outcome likelihoods at lines 17-32 and 58-96.

No existing authority inspected declares semantic correspondences such as
`:refined -> [:organization :grounded-change]`. Statuses describe an entity's
lifecycle; dispositions classify a full-loop attempt. Sorting two enumerations
does not supply a semantic bridge. Thus no non-placeholder A can honestly be
derived from current authority.

Discharging the caveat requires a reviewed declaration artifact that names:
(a) the exact seven-state and twelve-outcome supports in contract order; (b) a
complete normalized row for every state; (c) authority `:declared-prior` and a
stable name/revision; (d) a rationale/source for every nonzero correspondence;
(e) explicit named zeros; and (f) the model revision and digest. It must then
replace the placeholder A in both the row-6 model and both H1/H2 registration
likelihoods, followed by production-match and changed-correspondence negative
controls. Measurement-derived rows would instead require the contract's pinned
measurement record (`machine_model.clj:71-99`) and must not be relabelled
declared.

## 3. Q/C retention

At score time, the current scorer holds the full continuous prediction, the
optional disposition-derived Q internally, and the supplied seeded C
(`efe.clj:629-712`). Its return retains the full forward prediction and only the
scalar disposition risk/C contribution (`efe.clj:880-902`). The trace normally
drops `:prediction`; it retains the scalar ruled contribution and predicted
disposition risk only when C-fold provenance is present
(`src/futon2/aif/trace.clj:84-142`). Even policy-detail mode retains continuous
mean/variance, not the twelve-wide machine Q (`trace.clj:142-150`). There is no
retained machine-Q/C pair today.

The minimal additive per-ranked-action record is:
`{:machine-q {:schema :wm/predictive-outcome-kernel-v1 :policy/id ...
:policy/revision ... :model ... :support [...] :mass {...} :authority ...}
:machine-c {:schema :wm/machine-preference-v1 :model ... :support [...] :mass
{...} :provenance ...} :G-machine-q-risk n}`. Q and C must carry identical
ordered support, model/revision, and their authority pins; named zeros and an
infinite-risk refusal must survive rather than be smoothed.

The row-13 `:accumulation-update-input` family is not the right carrier: it is a
once-per-tick envelope for the selected entity's observation and pre/post belief
at `war_machine.clj:6327-6334,6798-6803`. Machine Q/C is per candidate policy at
scoring time. It belongs on each ranked-action entry and in
`trace/strip-ranked-action`; only shared model/A/C provenance may be factored to
a tick-level companion record keyed by an exact revision.

## 4. Recommended implementation split

1. **Declared A artifact, one file.** Add a reviewed, pinned correspondence
   record; no scorer changes. Acceptance: contract validation, all 7x12 cells,
   normalization/named-zero checks, and mutations for reordered support,
   unpinned authority, and missing row. This packet is blocked on the actual
   owner-reviewed semantics; positional order is not acceptable content.

2. **Model assembly, one behaviour in `machine_predictive.clj`.** Consume only
   that artifact and build the admitted A/Q kernel, removing the positional
   constructor from the qualifying path. Acceptance: row-6 F8 replay at
   identical pins plus a changed-policy witness and Lean composition values.

3. **Scoring adapter, one new source module.** Given an admitted Q row and
   admitted machine C on identical support, compute KL/refuse on Q-positive
   C-zero. Acceptance: exact reference values, support/policy/model mismatch
   refusals, and a two-policy ordering that changes when Q differs. This keeps
   probability validation out of the large scorer.

4. **`efe/compute-efe` injection, one behaviour.** Add an explicit opt-in
   machine-Q provider/record to the option boundary and fold only the adapter's
   result into risk. Acceptance: scorer calls the actual row-9/6 producer for
   each policy, baseline remains byte-identical when disabled, and identical
   plans give identical rows while differing declared effects can differ.

5. **Retention, one `trace.clj` behaviour.** Whitelist the complete Q/C pair and
   pins per ranked action. Acceptance: redirected full judge readback exactly
   matches the objects seen by scoring; support-order mutation and dropped Q or
   C refuse before successful trace append. Do not reuse the accumulation
   envelope.

6. **Row-11 registration refresh, one data packet.** Replace both placeholder
   likelihood registrations with the reviewed A revision and update their pins.
   Acceptance: H1/H2 posterior/predictive witnesses re-run with the new content,
   plus stale-registration and mixed-A-revision refusals. Only this reviewed
   packet may discharge `:placeholder-a-content`.

No registry edit or live-mode flip belongs to these packets; those follow the
reviewed source, proof, full-judge retention, and production-match evidence.
