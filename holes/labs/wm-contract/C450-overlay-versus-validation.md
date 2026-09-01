# C450 — the overlay layer, and what the run records actually show

**Date:** 2026-09-01. **Owner:** `claude-20`. From Joe, closing decision 5:

> We don't yet really have evidence that this system can work at all. […] The
> systemd's are going to be set up to call Python code or Clojure code or
> something. And so all of that code needs to exist. All of that code needs to
> be validated and vetted. And just having an entry point on systemd isn't that
> interesting. […] This is an overlay layer on top of the validation.

And, separately, a constraint I had not been weighing:

> If it's interfering with other work that I'm using this machine for, that's
> not my desired outcome. This machine is not being entirely given over to this
> project.

## The reclassification

Work in this campaign divides on an axis the register did not have:

- **Validation** — does the machine do what it claims, and can that be shown?
- **Overlay** — scheduling, fencing, certification of runs, systemd entry
  points, orchestration of pinned commits.

Overlay work is not wrong and some of it is good, but it is **built on top of a
claim not yet established**, and it consumes a shared machine that has other
projects on it. It is held as a source of ideas, not worked.

The register is re-marked accordingly. Nothing is deleted: an overlay item that
is later needed is still written down, with the reason it was parked.

## What the run records show

Two run records exist from the last two nights, in
`holes/labs/wm-contract/`. Both were produced by an actual traversal, not
constructed.

| | 2026-08-30 | 2026-08-31 |
|---|---|---|
| Wall time | ~20 s | ~24 s |
| Hops traversed | 9 | 9 |
| **Conformant to declared topology** | **3** | **3** |
| Unmapped hops | 6 | 6 |
| **Declared edges fired** | **3 of 21** | **3 of 21** |
| Trace written | true | true |
| Store basis count | 192,761 | 198,720 |
| `:selectorSeam` | `stub:first-ranked-authorized-mission` | same |

So the machine **does** run end to end: observation → precision → belief events
→ variational free energy → rank actions → select action → strategic selection
→ trace, in under half a minute, writing a trace each time.

**Three of twenty-one declared edges fire, and the selection at the end is a
diagnostic stub.** C110 demonstrated a production selector through the Agency
HTTP strategic-selection endpoint; C184 tested certification against a
production-*shaped* record by hand-editing the seam string. Neither makes the
nightly tick a production selection.

The operational certificate on disk,
`operational-certificate-00f4bf58-4da6-42bc-bb1d-5687e889e717.edn`, was issued
against the 2026-08-31 run — the one whose seam reads `stub:`.

## One thing I checked that looks worth fixing

`src/futon2/aif/full_loop_runner.clj:240` writes

```clojure
:selectorSeam "live:validated-selection"
```

as an unconditional literal inside the record map. On the one write site I read,
the field is **authored by the producer, not observed from the selection**. C184
records that the certificate "preserves the field as evidence"; a constant
preserved faithfully is not evidence about which selector ran.

This is the same class as C404 limit 3 and C423: a caller-authored label
persisted by a producer and then read as an observation. I have read one write
site and have not searched for others, so this is a lead rather than a finding.

## What this means for the campaign

The C380–C440 work built machinery to establish **which topology was used when
the machine runs**. The topology that runs is 3 of 21 declared edges, ending in
a stub. The certificate is sound about what it certifies; it certifies a
traversal whose selection step is diagnostic.

Joe's question — *is there evidence this system can work at all* — is therefore
not answered by the certificate, and was never going to be. It is a question
about the 18 edges that do not fire and the selector that does not select.

## Held as overlay, not worked

O19 (writer fence), O29 (pinned-commit orchestration), and the systemd/timer
aspects of O16/O17. O20 splits: keeping trees clean so agents are not misled by
them is validation-adjacent and stays; cleaning them *so a commit can be
certified* is overlay.
