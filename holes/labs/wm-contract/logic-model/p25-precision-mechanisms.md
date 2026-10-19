# P25 — R7: three distinct precision mechanisms

**Disposition:** documentation correction only. The implementation contains
three separately scoped precision quantities; treating them as one registry is
the paper discrepancy identified by annotation A13, not an instruction to wire
them together. The fixed-vacuous zeta improvement remains deferred until
partial end-to-end operation, as P25 requires.

## Reading base and boundary

This reading is pinned to futon2 `b16534506f5fc0aecb62b9e35266e8c7a54a6ff3`
and p4ng `2e67e34d7bbc237518b7387ebf8b2c10fd293399`. P25 is at
`logic-model/PACKETS-2026-10-19.md:575-587`; the frontier ruling requires
absence to be retained rather than replaced by a number
(`RULING-frontier-is-measured-not-pass-fail-2026-10-19.md:25-49`).

The annotation records are unreviewed. A12 says the channel registry exists
but is `EXISTS-UNVALIDATED`; A13 marks the phrase “apply it wherever prediction
error, scoring, or gating occurs” `REFUTED`
(`p4ng/plop-2026-code-annotations.json:252-302`). The source reasoning below
establishes interfaces and static dataflow only: it does not establish a
current serving-JVM run, calibration, or a new precision witness.

## The three mechanisms

| Quantity | Owner/state | Consumer/effect | Not the same as |
|---|---|---|---|
| Channel precision Pi | `precision.clj::update-precision-state`; one value/history per observation channel | R3 inner loop reweights prediction errors before aggregation | zeta or reading-policy beta |
| Likelihood precision zeta | `likelihood_precision.clj::zeta-declaration`; fixed Gibbs exponent on token likelihood A | EFE scorer tempers nonzero token rates before risk/ambiguity | channel history weight or beta |
| Reading-policy precision beta | `reading_policy_precision.clj::declaration-for`; sealed, identity-bound declaration/carry | reading-family selector's policy posterior | general channel trust or zeta |

### Channel precision Pi

`initial-precision-state` makes one state entry for every
`belief/channels-with-likelihood`, initially 1.0 with empty history
(`src/futon2/aif/precision.clj:87-95`). `update-precision-state` updates only
the supplied error channels, using bounded inverse rolling error variance;
preference-gap salience is explicitly separate from production Pi
(`precision.clj:160-193`). `weighted-error` writes the adaptive Pi and
calculates `:weighted-error = :error * Pi`, while retaining the different
per-call precision for trace provenance (`precision.clj:212-233`).

The actual strategic-loop consumer is static and direct: `r3-inner-loop`
forms present channel errors, calls `update-precision-state`, creates weighted
errors, and passes them to `belief/r3d-aggregate-driver`
(`scripts/futon2/report/war_machine.clj:6224-6255`). It retains the precision
state and weighted errors in its result (`war_machine.clj:6310-6316`). This
supports that the registry exists; it does not prove a current live tick.

### Likelihood precision zeta

zeta is a nonnegative Gibbs exponent for a likelihood row,
`p -> p^zeta / sum(p^zeta)` (`src/futon2/aif/likelihood_precision.clj:32-98`).
`zeta-declaration` names it `:likelihood-precision-zeta`, declared fixed, with
no zeta prior or update law (`likelihood_precision.clj:184-223`).

For the relevant token A, `horizon-g-sparse*` applies
`lprec/tempered-rates` to nonzero adjudication rates before risk and ambiguity
are computed (`src/futon2/aif/cascade_model_manifest.clj:816-830`). A non-unit
zeta with all-zero identity rates is refused as
`:zeta-with-identity-rates`, not silently ignored (`:838-849`).
`efe/rank-cascade-actions` passes `:zeta` (default 1) into that scorer
(`src/futon2/aif/efe.clj:1218-1243`). Its certificate records the zeta,
basis, tempering status, and distinguishes identity-path
`:declared-fixed-vacuous` from unavailable
(`cascade_model_manifest.clj:1198-1231`).

Therefore fixed-vacuous means that a fixed declaration was considered but the
identity kernel offers no nontrivial likelihood for zeta to alter. It does not
mean zeta is absent, learned, or live-validated.

### Reading-policy precision beta

`declare-beta` requires a finite positive beta, declared authority/version, and
a reading-precision identity. `declaration-for` returns only an explicit
declaration or an identity-bounded campaign-issued declaration
(`src/futon2/aif/reading_policy_precision.clj:198-278`). It does not read the
channel precision state.

The reading-family path obtains a common beta from
`assembled[:declared-betas target]`, refusing absent or heterogeneous values
(`src/futon2/aif/wm/cascade_decision.clj:1147-1161`).
`certify-reading-family` uses an exact identity and calls
`reading-policy-precision/declaration-for`; the resulting carry supplies beta
(`cascade_decision.clj:1107-1145`). The family selector receives that certified
beta (`cascade_decision.clj:1231-1260`) and `policy/select-action-cascades`
uses caller-supplied beta in its posterior, with no default
(`src/futon2/aif/policy.clj:351-363,461-469`).

## Why a Pi change does not imply a beta change

This is static source reasoning, not a numerical claim.

1. Pi consumes a prior channel state and prediction errors, producing a
   channel-keyed state and weighted errors (`precision.clj:160-193,212-233`).
2. The beta path consumes `:declared-betas`, a prior reading-precision receipt,
   and an exact sealed declaration (`cascade_decision.clj:1100-1161`). None is
   `:precision-state` or `weighted-error`.
3. The selector accepts beta explicitly and validates an optional beta state
   only for beta/tau/gamma consistency (`policy.clj:416-469`); it does not read
   channel Pi.

Thus changing Pi can affect R3 error aggregation, but cannot change declared
reading beta or that selector through these boundaries. It would take a
separate producer changing `:declared-betas` or the sealed declaration. No
such connection appeared in this bounded caller census. This is P25's required
bad case: a changed channel precision with declared beta 1 must not be claimed
to alter selection.

zeta is separate for the same structural reason: it is passed to likelihood
scoring, whereas beta is passed to the policy posterior. Other uses of the
word “beta” (for example, Beta distributions) are outside this mapping.

## Historical, static, and live evidence

`zeta-declaration` cites historical `RUN-r7-zeta-effect-2026-09-18` in its
metadata (`likelihood_precision.clj:206-208`); this packet did not recheck it.
The A12/A13 annotations and their worknote witnesses are likewise unreviewed.
No serving probe, reload, click, test, warrant, or ledger action was performed.
Accordingly this packet makes no live-evidence or calibration claim.

## Exact proposed R7 replacement

Replace the R7 `Then` clause in p4ng `sec-catalog.tex:238-240`—from “keep a
per-channel …” through “gating occurs”—with:

> **Then** make each precision explicit at the boundary it controls. Maintain
> a per-observation-channel evidence precision \(\Pi\), derived from that
> channel's bounded prediction-error history, for weighting its prediction
> error. Where a token observation likelihood is evaluated, record the
> declared likelihood precision \(\zeta\) that tempers that likelihood; if the
> identity likelihood is used, retain the declaration as fixed-vacuous rather
> than treating it as an observation. For a reading-policy comparison, require
> an identity-bound, declared policy precision \(\beta\) and record it with the
> policy posterior. Do not infer any one of these quantities from another, and
> retain an unavailable declaration as unavailable rather than substituting a
> numeric value.

The existing `If`, `However`, and `Because` may remain. This text does not
claim learned zeta, an improvement to the deferred fixed-vacuous route, a beta
change caused by Pi, or live validation.

## Proposed correlation update (not applied)

Add this row to `logic-model/correlation.md` section 3 after the R7 row at
current line 251:

| pattern | annotation verdict | anchor | what the tables say about that code on the live path |
|---|---|---|---|
| R7 | A12 `EXISTS-UNVALIDATED`; A13 `REFUTED` only for one-registry-everywhere prose; docs correction proposed | Pi: `precision.clj::update-precision-state` (`b1653450:160-193`) -> `war_machine.clj::r3-inner-loop` (`:6244-6255`); zeta: `likelihood_precision.clj::zeta-declaration` (`:184-223`) -> `cascade_model_manifest.clj::horizon-g-sparse-cert` (`:816-849`, `:1198-1231`); beta: `reading_policy_precision.clj::declaration-for` (`:259-278`) -> `wm/cascade_decision.clj::certify-reading-family` (`:1107-1145`) | Pi weights channel errors; fixed zeta tempers nonzero token likelihood and is explicitly fixed-vacuous on identity A; identity-bound beta controls the reading-policy posterior. No caller census edge makes Pi alter beta. Static and historical evidence do not establish a current live/calibrated route. |

Do not upgrade A13 to validated: its accurate outcome is that the universal
registry assertion is refuted while the distinct precision boundaries are
AIF-valid and should be documented.

## Limits

No implementation discrepancy was found within P25's boundary. The concrete
discrepancy is only R7's claim that one channel registry applies wherever
precision is used. No source, tests, paper, shared table, serving state, or
repair-obligation artifact was changed.
