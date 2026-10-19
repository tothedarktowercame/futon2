# P13 — Q1–Q10 certificate producers and absence paths

Static reconciliation at futon2 `4eabbda7ad367cc12773269a7630e17f14eb8c69`
(2026-10-19).  This is a source-and-artifact reading, not a click, gate run, or
claim that a current serving process has used these paths.

## The two surfaces are not interchangeable

`data/wm-certificates/` is a historical Lean-artifact surface, not the current
run verdict producer: the store census records no futon2 writer or reader and
describes 16 old `.lean` files (the newest named artifact is dated 2026-10-01).
The P13 scan found no `data/wm-certificates` directory in this checkout.
The source pin is `4eabbda7:holes/labs/wm-contract/logic-model/stores.md:62,92,134,142`.
It must therefore not be read as a current Q1–Q10 result.

The current surface is the sidecar adjacent to a *particular* persisted run:
`<run-record>.requirements.edn`.  `wm_run_alert.py` creates that path, exports
the record to temporary JSON, generates a temporary Lean file, evaluates it,
then atomically writes the computed result; it separately records evidence
publication state.  The source pins are
`5ce3db21:scripts/wm_run_alert.py:71-82,92-114,119-153` and
`4eabbda7:scripts/wm_click.sh:378-390`.  Thus the sidecar is a recomputation
from a run record at alert time, not a pre-existing certificate directory and
not a run-record field written by `persist-run-record!`.

## Producer map

`persist-run-record!` first retains the decision/sorry route, outer receipt,
selection world, Q6 and Q8, then attaches Q2/Q9/Q10 carriers before streaming
the record.  Its source pins are
`89e68971:src/futon2/aif/full_loop_runner.clj:persist-run-record!:1113-1154,
1221-1263,1317-1338`.  In particular, a Q2 carrier is evidence, not the guard
for retaining a frontier or loop-node receipt (`:q2-construction`,
`:target-policy-field`, `:reading-frontier`, and `:loop-node-exercise` remain
on the selection-sorry route): `89e68971:...full_loop_runner.clj:1233-1248`.

The exporter returns values with field-level `{"not-recomputable": reason}`
when a required carrier/input is absent; it also returns a `sources` map and a
`diagnostics.notRecomputable` field list.  It does not substitute zero or a
passing value.  Source pins: `2079cc54:scripts/wm_run_facts.clj:1-6,
520-522,617-722`.  The Lean generator turns any absent/NR required field into
`Evidence.nr`; only fully known dependencies become `Evidence.known`:
`745a0de5:scripts/wm_check_run_facts.clj:9-10,95-116,198-209`.

| Q | Required exported inputs / immediate producer | Honest absence path | Predicate pin |
|---|---|---|---|
| Q1 | `world-at-selection` open IDs and enumerated IDs, with snapshot fallback only for open populations | absent enumerated ID set becomes NR; it is not a count-based equality | exporter `2079cc54:623-630`; `Requirements.lean:252` |
| Q2 | reading-family-only sealed `:q2` carrier: outer actions, selected construction, library/constructor counts; it is recomputed against the record | another route, absent/tampered/unbound/non-certified Q2 carrier, or any Q8 dependency becomes NR | `2079cc54:92-126`; `afa6cfc6:q2_construction.clj:247-267`; `Requirements.lean:267-292` |
| Q3 | selection certificate candidate identities and finite per-candidate G | missing candidates or numeric G becomes NR | `2079cc54:631-652`; `Requirements.lean:295` |
| Q4 | reading-family Q4 carrier / coherent per-policy preference, normalization, and G-term inputs | a non-reading route or missing/non-uniform data is NR, never a synthetic preference model | `2079cc54:600-610,653-683,703-704`; `Requirements.lean:309-323` |
| Q5 | recorded selection and interpretation instants/status in `:world-at-selection` | absent/unreadable instant, or owed-but-unissued interpretation, is NR | `2079cc54:451-475`; `Requirements.lean:326-327` |
| Q6 | sealed predecessor and this run's sealed terminal carrier | absent/tampered predecessor/terminal or typed absence in either choice/digest is NR | `2079cc54:484-518`; `Requirements.lean:331-333` |
| Q7 | `absence-paths` over the retained record | this is a measured count; the explicit absences make Q7 false rather than disappearing | `2079cc54:611-612,685,717-721`; `Requirements.lean:335-339` |
| Q8 | sealed/recomputed `:q8` carrier: census, refresh, selected entry, cascade; open population is from retained world or snapshot | absent/tampered/unbound/non-certified carrier makes all four Q8 fields NR | `2079cc54:131-168,623-628,699-702`; `2079cc54:q8_frontier.clj:303-336,357-384`; `Requirements.lean:357-365` |
| Q9 | reading-family sealed Q9 factorization/calibration authorization carrier | a route without it or an invalid carrier yields NR; a verified negative carrier remains a checked false proposition, not NR | `2079cc54:401-426,703-704`; `Requirements.lean:396-399` |
| Q10 | reading-family sealed Q10 arrangement census | another route or invalid/missing carrier becomes NR | `2079cc54:431-449,703-704`; `Requirements.lean:404-406` |

The final distinction is made in mathlib, not by a default in the exporter:
`Evidence.nr` assesses to `Outcome.unverifiable`, is retained in the alert,
and cannot conform (`64c7b058:DarkTower/WarMachine/RequirementsReader.lean:3-5,
10-16,30-51`).

## P07c assessed-population Q2/Q8 trace

The searched `assessed-population` call sites form this static chain (not just
the initially named selector pointer):

1. `outer_policy_objective/evaluate` creates a partial assessed-population
   receipt; it can record `:no-choice`, refusal, or selected state.
   Pin: `4eabbda7:src/futon2/aif/outer_policy_objective.clj:120-145`.
2. `assessed_policy_basis/seal` stores the menu/evidence/receipt and verifies
   it without IO.  Pin: `4eabbda7:src/futon2/aif/assessed_policy_basis.clj:1-45`.
3. `selected_outer_cascade_policy/seal-assessed` labels this basis and explicitly puts
   `{:status :absent :reason :assessed-population-basis}` in the Q8
   recomputation; its verifier recognizes the assessed scope and requires the
   partial assessed receipt.  Pin:
   `4eabbda7:src/futon2/aif/selected_outer_cascade_policy.clj:65-89,91-142`.
4. `meta_live_outer_selector` projects that Q8 absence (with basis/scope) and
   a separate Q2 outer-action absence into the outer receipt.  Pin:
   `4eabbda7:src/futon2/aif/meta_live_outer_selector.clj:770-806`.
5. `persist-run-record!` retains the outer receipt and calls Q8/Q2 certificate
   construction from the record.  Pin:
   `89e68971:src/futon2/aif/full_loop_runner.clj:1129-1154,1255-1259,1317-1338`.
6. The Q2 and Q8 verifiers return `:not-recomputable` for a non-certified
   carrier rather than throwing; the exporter serializes that status as the
   field-level NR object.  Pin: `afa6cfc6:q2_construction.clj:254-267`;
   `2079cc54:q8_frontier.clj:366-384`;
   `2079cc54:scripts/wm_run_facts.clj:92-106,131-140`.
7. The generator emits `.nr` if any dependency is NR, and the reader maps it
   to `Outcome.unverifiable`.  Pin:
   `745a0de5:scripts/wm_check_run_facts.clj:95-116,198-209`;
   `64c7b058:DarkTower/WarMachine/RequirementsReader.lean:30-51`.

Finding: in this downstream certificate/report chain, P07c Q2/Q8 absence is
measured, retained as not-recomputable, and assessed as `unverifiable`; the
enumerated consumers do not throw and do not reclassify it as zero, success,
or a recomputable value.  No concrete *certificate-chain* downstream veto was
found by this static enumeration.  This is deliberately narrower than a claim
about every runtime consumer of assessed-policy carriers.

## Recorded-shape comparison (read only)

The L2 selected-but-lost artifact is
`/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796.edn`.
Its persisted record has `:pattern-graph-diff {:status :absent :reason
:no-selected-action}` and an incomplete loop-node receipt, while its adjacent
requirements sidecar is
`...f9abc990-b43a-491b-8f2b-d8b14c7fa796.edn.requirements.edn`.  The latter
is the only current verdict artifact identified here; it records Q2/Q8 as
unverifiable rather than supplying a lost selected identity.  This comparison
is a direct read of those two files on 2026-10-19, not an inferred count.

For contrast, the actual no-choice record read was
`/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-16-d845555a-88f3-442b-aef5-f6d17379cae9.edn`.
Its existing `.scan.md` identifies the recorded outcome as `:no-selection`.
That is a distinct persisted shape: an explicit no-selection record, not a
selected judgement whose action was later lost.  No aggregate number of either
shape is claimed here.

## Proposed coordinator updates (not applied)

- `producer P26`: “Current Q1–Q10 output is the atomic per-run
  `<run-record>.requirements.edn` recomputation; `data/wm-certificates/` is a
  historical writerless Lean-artifact store, not P26 output.”
- `store run-record`: “The run record retains Q2/Q8 carriers and typed absence;
  the requirements sidecar is a separate adjacent artifact generated later
  from that record.”
- `correlation 10 / A24 certificate_coverage`: “For assessed-population P07c,
  Q2/Q8 carrier absence reaches the reader as NR/unverifiable.  It is measured
  absence, neither pass/fail substitution nor a documented certificate-chain
  throw.”

## Limits

This packet did not run the exporter, Lean, alert script, a click, or a serving
JVM.  The historical-store conclusion is limited to the cited store census and
the checkout scan.  The record comparison names the two read artifacts and
does not claim population totals or live reproducibility.
