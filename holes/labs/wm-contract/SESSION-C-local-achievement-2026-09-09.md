# E-C-realization: one local calculation from a historical achievement

2026-09-09, Joe with codex-12. Continuation of
[process framing](SESSION-C-tau-process-framing-2026-09-09.md).

## Joe's direction, verbatim

> So I like that terminology or definitional shift. And I think and I think Talking about a family of preference distributions, we'll certainly make... Get it easier to think about this. And I would say that what we need to do is start to generalize from the simple snatch example towards... Several examples that are inspired by the historical work that's going on. Related to the futon stack. So. Each of the mission clusters in the. Cascade Live. Was worked on for some reason, and they're clusters because something was achieved. So I think all of those examples could be mined for a kind of... Set of... Desiderata or requirements that Thank you. Satisfaction. Entailed. At that time. And ultimately, of course, that relates in the end towards certified or working functionality, whether that's certified by live runs or by derived data or by. Unit tests. But what we are looking for is not just, we want lots of passing tests, because that's hardly useful. But we want a much more qualitative analysis of the... Existing. Achievments. To the mission. Clusters. Give us one way to think about that. But another one is the Capability analysis that develops in the capability star map. For that one, it's clear that there's a preference for growing personal and organizational. Capability. What hasn't yet happened is that this hasn't all been organized clearly into a cascade. But I suggest that working... Our way up, much as we've been doing with the how and why links, will provide us with the structure that we would need. In order to do some of these computations, and I would suggest that what we really should work on is doing at least one local calculation related to... Either the existing capability, Stars that have been achieved or the ones that are in progress or towards some of the missions that have been achieved.

No repair of the ambiguous “requirements that Thank you. Satisfaction. Entailed”
fragment is asserted. The surrounding sentences explicitly ask for qualitative
analysis of historical achievements and one local calculation. Joe accepts
“family of preference distributions” and explicitly names growing personal and
organizational capability as a preference. No numerical masses are specified.

## Chosen achievement and its original purpose

**M-first-flights, Phase A**, explicitly a member of Cascade Live cluster A,
“Records carry warrant”. Its HEAD describes the starting point as a flight
display that was “just a list of numbers (and nulls)”. The stated satisfaction
condition is: “records carry their derivations, so error has structure to
propagate into.” Source:
`futon3c/holes/missions/M-first-flights.md:19-39`.

The qualitative capability gained was to distinguish a usable measurement
from an unsupported number, and let both the operator and calibration code
read that distinction. The six Phase-A exits connect schema, checking, rendering,
storage/readback, a calibration-consumption demonstration, and Joe's review
(`M-first-flights.md:497-533`). Checkpoint 20 records Joe's “It passes my review!”
and explains that he could distinguish witnessed no-move from a censored copy,
and checked-empty knowledge from absence (`:896-913`). This is a historical
acceptance record, not acceptance newly inferred from today's check.

For one calculable part, exit 5 specifically requires a calibration-lane
consumption which fails on derivation-thin records. Checkpoint 15 reports that
the historical run separated the two usable records from 31 thin records
(`:782-804`). Today's calculation uses **one preserved real-flight witness**;
it does not rerun or claim to reconstruct that entire historical population.

## Calculation performed now

Files:

- [Reproducible calculation](runs/C-realization-first-flights/local_calculation.bb)
- [Machine-readable result](runs/C-realization-first-flights/result.edn)

From futon2:

```sh
bb holes/labs/wm-contract/runs/C-realization-first-flights/local_calculation.bb
```

The calculation verifies SHA256 pins on the historical witness and existing
flight verifier before reading/loading them. It runs in a standalone Babashka
process, uses the existing rules, and calls no live service.

The local satisfaction predicate is the verifier's existing calibration
admissibility rule (`futon3c/scripts/flight_spec_verify.clj:296-309,354-377`):

`admissible = conforms-to-F1..F9 AND full-record AND grounded-measurement`
`             AND class-in-{clean,null} AND settled-window`.

This is a logical conjunction grounded in the historical implementation, not
a newly assigned preference weight. “Settled” includes time-order and scan
requirements, not merely a small numerical difference.

For real flight `live-957a4836-977a-4083-a243-e61cdd9862e3`:

| Quantity | Recomputed result |
|---|---:|
| Predicted one-step G | -4.0790903614457825 |
| Realised one-step G | -4.039678471388555 |
| Absolute prediction error | 0.03941189005722734 |
| Disagreement between settled scans | 0.00007553502170054571 |
| Recorded tolerance | 0.005 |

All existing conformance conditions hold and the derived calibration mask is
**in**. As a labelled counterfactual, the calculation removes only the
measurement's `:ground` field. It retains every prediction and measurement
number, class, and scan. F1 then fails and the derived mask is **out**.

| Record | Same numeric error | Settled window | Measurement grounds | Admissible calibration pair |
|---|---|---|---|---|
| Historical witness | 0.03941189005722734 | Yes | Present | Yes |
| Counterfactual with grounds removed | 0.03941189005722734 | Yes | Absent | No |

The operational difference is not “one more passing test”. The first record
supplies an admissible error sample to the calibration interface; the second
does not, although an error-only dashboard would show identical values. This
replays a local part of the historically accepted achievement. The witness is
a pilot-authored record of a real flight; conformance checks its recorded
evidence structure, not the independent authenticity of every cited event.

## Working upward toward the preference family

The evidence supports this local chain:

`measurement + recorded grounds + satisfied record rules`
`  -> admissible calibration pair`
`  -> one instance of making flight records consumable`.

The relation of that capability to “Records carry warrant” is supported by the
mission's HEAD and its membership in the pinned cluster. It is a contribution
to the cluster, not a proof that the cluster or apex has been satisfied.
The mission explicitly leaves training the prior to later work; no trained
prior, future-success probability, policy-grade capability or full organizational
capability increase is certified by this one replay.

For a local preference distribution, the candidate outcome distinction is now
concrete: **a usable, warranted calibration record versus a numerically identical
record whose measurement cannot be admitted**. The historical requirement
provides the qualitative orientation. It provides no ratio of preference masses.
Thus this turn computes the satisfaction predicate and its evidence-sensitive
contrast; it does not manufacture a numerical C or a KL preference score.

This supplies one example beyond Snatch and a way to compare subsequent examples:
recover the original purpose, identify the concrete capability and consumer,
replay the satisfaction rule against recorded evidence, and expose which
dependency makes the claimed achievement possible. How these examples compose
into Cτ remains for the sitting to develop.

## Validation

The existing verifier accepted the preserved witness. The local calculation
asserts acceptance of that witness, rejection of the counterfactual specifically
by F1, and unchanged numeric measurements. clj-kondo: zero errors/warnings;
check-parens: OK on the calculation and result. A repeated calculation reproduced
the saved EDN byte-for-byte. No production code, registry or worklist changed.
