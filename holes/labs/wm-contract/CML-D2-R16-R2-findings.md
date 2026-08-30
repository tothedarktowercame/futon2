# CML-D2 — `R16→R2` reconciled edge proposal

Date: 2026-08-30. Discovery only; no edge or source was edited.

## Instruments

- **Observed.** `rg -n -C 3 'R16→R2|re-observe|actuation|witness outside|payload'
  holes/problems/P-R16.md holes/problems/P-R2.md` located both endpoint
  passages. Direct inspection supplied the quotations below
  (`holes/problems/P-R16.md:42-45,59-64`; `holes/problems/P-R2.md:41-45`).
- **Observed negative with positive control.** `rg -n
  ':enacted|:enactment|witness' src/futon2/aif scripts/futon2 --glob '*.clj'`
  found the enactment writer and trace path, while inspection of the positive
  observation producer found all fourteen channel values and no explicit
  witness input (`holes/labs/wm-contract/R16-D1-findings.md:19-21,28-31`;
  `src/futon2/aif/observation.clj:11-74`). The instrument cannot exclude a
  witness hidden inside a generic upstream scan field; that indirect route is
  **inferred, untested** (`holes/labs/wm-contract/R16-D1-findings.md:30`).

## Endpoint records, verbatim

**Observed — R16.** Its solved clause says:

> **An actuation is a witness outside the model.** `Actuation := {mission, act, witness : ExternalWitness}` where
> `ExternalWitness` is a record in a substrate the generative model does not write (a commit sha, a file
> digest, a substrate-1 artefact), and `R16→R2`'s payload IS that witness — the `Delivery` schema for the
> re-observe edge is `{tick, mission, witness}`; a tick whose observation vector does not read it is the ring.

(`holes/problems/P-R16.md:42-45`). Its edge list says:

> `R9→R16` (assurance into actuation — P-R9's proposed `{claim, witness, verdict}`: an act carries an independent
> witness of its precondition), `R16→R2` re-observe (**the witness**, above).

(`holes/problems/P-R16.md:61-62`).

**Observed — R2.** Its complete edge paragraph says:

> `R2→R3` observe (drawn), `R16→R2` re-observe (drawn), `R2→R8`, `R2→R7`, `R10→R2`, `R9→R2` (derived).
> Deliveries e1–e7 as in worksheet §4, each with the undeclared field named there; the edge schemas are fixed
> in `P-control-map-lint.md`'s fixtures. Payload for `R2→R3` and `R2→R8`: `Observation` with its `Channel`
> key set as the schema and `receipt = {tick, key-set-ok?, consumed-by}`.

(`holes/problems/P-R2.md:42-45`). This names `R16→R2` but specifies only the
two outgoing payloads. **Finding:** the reconciliation is one-sided; R2 has no
incoming payload or receipt proposal here. I refuse to reuse its outgoing-edge
receipt as though it governed `R16→R2`.

## Proposed `Delivery`

```edn
{:from :R16
 :to :R2
 :payload {:tick :unspecified-type
           :mission :unspecified-type
           :witness :ExternalWitness}
 :guarantee :unspecified
 :atomicWith :unspecified
 :retry :unspecified
 :timeoutMs :unspecified
 :idemKey :unspecified
 :receipt :unspecified}
```

- **Observed.** `from`, `to`, and the three payload field names come directly
  from R16 (`P-R16.md:42-45,59-64`). `ExternalWitness` is described by allowed
  substrate examples, but neither endpoint fixes the concrete representation
  of `tick`, `mission`, or the witness sum; R16-D2's planned declarations would
  settle that (`P-R16.md:42-47,75-77`).
- **Observed.** Neither endpoint states delivery guarantee, atomic writes,
  retry cap/same-identity rule, timeout, idempotence key, or receipt for this
  incoming edge (`P-R16.md:42-45,59-64`; `P-R2.md:41-45`). They remain
  `unspecified`. Each requires an endpoint declaration naming that field;
  plausible operational defaults are not evidence.

## Current traffic and receiving channel

- **Observed.** This is a specification for traffic that does not currently
  occur. The source audit found no explicit observation-channel wiring from an
  enactment witness, and nil enactment remains untyped nil rather than a score
  or typed absence
  (`holes/labs/wm-contract/R16-D1-findings.md:19-21,28-31,40-42`).
- **Observed.** None of R2's fourteen channels receives this payload. The
  declared vector is `active-repo-ratio`, `annotation-health`,
  `attack-coverage`, `consulting-pct`, `coupling-density`, `depositing-signal`,
  `loop-health`, `mathematics-pct`, `mission-health`, `portfolio-pct`,
  `sorry-count-norm`, `stack-pct`, `support-coverage`, and
  `ticks-firing-ratio` (`src/futon2/aif/observation.clj:11-32`;
  `holes/labs/wm-contract/R2-D1-findings.md:46-51`). A witness-receiving
  channel does not exist.
- **Observed.** `:acknowledged?` is not such a channel: the only non-fixture
  producer is hard-coded `true` in synthetic lane-futility nags, and the trace
  has no acknowledgement keys
  (`holes/labs/wm-contract/R2-D1-findings.md:69-85,129-131`;
  `src/futon2/aif/lane_futility.clj:321-334`).

## Disposition

The owner can record the grounded `from`, `to`, and payload field names now,
but the remaining delivery fields and R2 receiving channel require an endpoint
decision. Marking this edge fully specified before those declarations would
turn R16's one-sided proposal into an invented R2 contract.
