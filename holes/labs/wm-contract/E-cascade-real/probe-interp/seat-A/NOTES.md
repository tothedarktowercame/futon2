# Seat A interpretation probe — notes (kimi-6, 2026-09-24)

Defect D11 probe (`holes/E-cascade-real.md`): write cascade-source-v1
interpretations for the 10 targets in `../inputs.edn` (frozen at 96fae955).
Time spent: ~55 minutes, most of it reading candidate flexiargs before
committing to them. Method: `futon3c/scripts/xlate.py find` (BM25) per task
cluster, then **read the `! conclusion` of every pattern before using it** —
no pattern was used on its title or BM25 rank alone. Header keys
(`:schema :context :token-initialization :beta :lam :mu :c-schedule`) copied
verbatim from the EIG example; `:facts` = the want tokens, `:locators` copied
from inputs.edn unchanged; `:candidates []` (the key is required by
`check-file!`; the constructor builds the content).

Guard discipline: `:needs` follows the mission documents' task order
(the `:holes` line numbers in inputs.edn); `:forbids` is the pattern's own
produced token(s), so a pattern fires only while its task is undone. No
token was invented; where a needed precondition had no token, it is a
missing-token finding below.

## Per target

### M-action-cost-modelling — 4 patterns, 5 of 6 wants covered
- `ukrns/scale-register` → h730434653957 (cost-signal: per-action `:scale`
  published as an inspectable register entry with rationale).
- `campaign-coherence/campaign-as-temporary-institution` →
  h68dbc60a7584 + hbe5e9b666bbc (the mission-or-Campaign decision and its
  resolution are one application of the pattern's criterion; the pattern's
  own provenance cites this mission, §3.5, as an origin).
- `contracts/every-entry-has-a-falsifier` → hb7f5a89877eb (the live
  discriminating test ships with its falsifier named:
  `:minute`/`:hour` above `:campaign`).
- `math-strategy/missing-dependency-protocol` → ha664b5558c56 (the
  "file a finding localizing the gap" branch of the stack decision).
- **Not covered: h6e84a77745cb (work-breakdown gate CTA).** Nearest hit was
  `futon-theory/coordination-protocol` (multi-agent handoff) — rejected:
  the task is a UI gate on action size, not a handoff protocol. Missing
  pattern: "gate the presentation of an action on its estimated size."
- Rejections: `orchestration/pattern-warranted-choice-point` (autonomous
  loop forks, not an operator-visible CTA decision); `futon-theory/
  mission-dependency` (dependency graphs, not the Mission/Campaign
  criterion); `ukrns/provision-at-network-scale` (institutional scale, not
  action annotation).

### M-canon-fingerprint-store — 4 patterns, 4 of 6 wants covered
- `storage/durability-first` → hae4a98689e37 (F1: a store is durable
  writes-confirmed or it is not a store).
- `portal/first-class-query-interface` → hf6110df5c7b0 (F2: in-run query as
  a first-class operation).
- `aif/two-layer-calibration` → h72b677883cde (F4: held-out gold validation
  lives in the empirical layer, not in-sample consistency).
- `sidecar/append-only-semantic-audit` → hb0224092661b (F5: the
  startup-read → prior → MAP-append → REDUCE loop as append-only merges
  with provenance).
- **Not covered: h61f4c17570aa (F3 per-binding canon posterior)** —
  `aif/status-gated-belief-update` rejected (it is about *not* updating
  unobserved instances, not about consuming a store as prior).
- **Not covered: h3b2303097926 (follow-on proof fingerprints)** — the hole
  itself says "defer until"; left unproduced deliberately.
- Rejections: `sidecar/tri-store-separation` (facts/memes/notions truth
  levels, not scope bindings); `enrichment/rational-reconstruction`
  (bulk import, not a store schema).

### M-chipwitz-corps — 5 patterns, 5 of 6 wants covered
- `futon-theory/derive-exits-on-a-minted-sorry` → hb9354e61ab3e (the hole
  literally begins "DERIVE: the warrant threshold"; the pattern names
  DERIVE's exit as a minted typed sorry, not a picked number).
- `musn/use-requires-evidence` → h09128afee45a (typed PXR channel as the
  PSR/PUR evidence obligation).
- `orchestration/pattern-warranted-choice-point` → h5963cec1a3df (wiring
  the choice-point check with its warrant and one specific gap).
- `aif/two-layer-calibration` → h2c51566a32b4 (`:pxr` evidence kind must
  not read self-produced scores as calibrated).
- `math-strategy/missing-dependency-protocol` → h4bd2d74d8763 (no-warrant
  asks → localized sorry → registry candidate).
- **Not covered: h056f5583d59e (name which corps disciplines relate)** — a
  survey/naming task; no honest pattern found.
- Rejections: `test-registry/bind-warrant-to-the-diff` (reviewer-side
  warrant check, not the pilot's choice-point); `mmca/threshold-shaped-
  events` (episode-length detection, not warrant thresholds).

### M-daily-scan — SKIPPED, no file
No honest interpretation. The task is a partial audit of static probes for
named-vs-queried misalignment. Nearest hits, all rejected:
`repository-transition/strawman` (research-repository culture
misalignments, not probe configuration),
`devmap-coherence/prototype-structure-checklist` (a maturity template, not
an audit move), `plos-npt-with-small-n/coreq-checklist-adherence`
(qualitative reporting checklist). Missing pattern: "audit the named
against the queried, and record the delta." Skipping is the honest result
here.

### M-distributed-frontiermath — 3 patterns, all 4 wants covered
- `agent/student-dispatch` → h16025dd5deeb (mentor session as
  student-explorer dispatch, REPL-driven).
- `f6/stratum-bridge` → h8606857c4374 + hd4bfb746db9b (Rob's #math access
  and zcodex reachability are one bridge in two directions).
- `orchestration/recorded-handoff` → h022fc0e7fab5 (bell-driven phase
  transitions as recorded handoff objects).
- Rejections: `agency/self-attribution` (speaking for oneself, not bridge
  access); `war-machine/coordination-bottleneck` (a problem diagnosis,
  not a mechanism).

### M-federated-agency-hardening — 4 patterns, all 4 wants covered
- `realtime/listener-leases` → hd6dd12860265 ("no 17070 listener anywhere"
  as checkable listener hygiene).
- `realtime/rendezvous-handshake` → hb8a6fa5239dc (zero-action reboot
  reconnect via signed hello+ack).
- `agency/state-atomicity` → h260a1b85a356 (peer-entry removal completes
  fully or reverts).
- `agency/single-routing-authority` → hf4b4e2392af5 (uplink routes only to
  existing agents: one routing authority, no second route table).
- Rejections: none beyond BM25 noise (`data-mining/checkpoint-the-long-run`
  was considered for reboot-recovery and rejected — it is about checkpointing
  paid mining runs, not connection re-establishment).

### M-futon-forward-model — 4 patterns, all 6 wants covered
- `social/verify-before-compose` → h6adae34e06ad + he92ac929f4c4 +
  h8b2289908aa2 (kondo/parens/tests are one pre-composition verification
  discipline; the three gate tasks are its steps for the new files).
- `aif/shared-kernel-predictive-forward-model` → h30576d62bffb (the
  manifold reader as the pure shared kernel's consumer).
- `peripherals/read-only-first-then-extend` → ha3f46a5c0073 (the task's
  "read-only + pure logic only" is the pattern verbatim).
- `orchestration/recorded-handoff` → h3547e9033d0d (the task line is the
  pattern's return leg, literally: "bell claude-1 back with a summary +
  commit shas").
- Rejections: none; this mission's tasks map unusually cleanly.

### M-futonzero-generative — 2 patterns, both wants covered
- `aif/two-layer-calibration` → h46ed944f95da (G-SIM adequacy earned by
  measured calibration pairs in the empirical layer).
- `futon-theory/crime-relocates-to-a-scarcer-witness` → hb5b297019e1e
  (G-REWARD anti-laundering as a witness ladder; the G1 arrow-witness
  binding is one rung, and the pattern warns it is not the last).
- Rejections: `contracts/every-entry-has-a-falsifier` for G-REWARD
  (considered; the witness-ladder is the more accurate reading — the task
  is about laundering resistance, not a single falsifier).

### M-kangaroo — 4 patterns, 4 of 6 wants covered
- `social/verify-before-compose` → h625bf9588b21 (W1 keystone: verify the
  warm process on its own before composing it with routing).
- `agency/state-atomicity` → hb674018208b0 (crash fallback is an
  all-or-nothing transition, never a stranded half-state).
- `agency/single-routing-authority` → h6e670ac8054e (exactly one warm
  process per identity; eviction keeps "one" true over time).
- `agency/identifier-separation` → h40dbfc082f77 (stretch: REPL + agency
  share one process without overloading transport/session identifiers).
- **Not covered: hf27981fd598b (measurably faster than cold)** — no
  honest benchmark pattern found (BM25 returned only math-formalization
  noise). Missing pattern: "claim a speedup only against a measured
  baseline, with the benchmark retained."
- **Not covered: he91ecffec2e7 (flag OFF = byte-for-byte cold)** —
  `realtime/mode-gate` rejected (chat-vs-work drift gating, not
  feature-flag byte-identity); `coordination/bind-promotion-to-post-repair-
  replay` rejected (certificate promotion replay, not a flag toggle).
  Missing pattern: "a flag gates a new path only when the old path is
  byte-identical under it."
- Rejections: `data-mining/checkpoint-the-long-run` (checkpointing paid
  runs, not process-crash fallback — though it was the closest; the
  fallback task is about transition atomicity, not durability of partial
  results).

### M-wm-aif-policy-grain-compliance — 4 patterns, all 4 wants covered
- `peripherals/read-only-first-then-extend` → h26fc1070c3e6 (read-only
  diversity source, the task's stated shape).
- `sidecar/append-only-semantic-audit` → h4d3638710e7b (prefix-local,
  auditable-by-construction scoring).
- `futon-theory/minimum-viable-events` → ha04170c8d22b (the habit return
  event as a named decision point made durable).
- `musn/use-requires-evidence` → hbc48e7c4dbdf (a dark shadow run must
  leave evidence records, not a story).
- Rejections: none material.

## Gate: load-declared

Fresh process (`clojure -M`, never the shared JVM):

```
(cs/load-declared "holes/labs/wm-contract/E-cascade-real/probe-interp/seat-A")
:targets ("M-action-cost-modelling" "M-canon-fingerprint-store"
          "M-chipwitz-corps" "M-distributed-frontiermath"
          "M-federated-agency-hardening" "M-futon-forward-model"
          "M-futonzero-generative" "M-kangaroo"
          "M-wm-aif-policy-grain-compliance")
:interpretations {… 4, 4, 5, 3, 4, 4, 2, 4, 4}
:collisions {}
:files 9
```

All 9 files load without refusal; no target collisions.

## Constructor smoke check (informational, not a gate)

`interpretation-construction/construct` with `budget {:max-moves 10
:max-expansions 2000}`, `horizon 8`, `move-cost 0`:

- With `evaluate-g (constantly 0.0)`: every target refuses
  `:construction-not-taken` — correct behavior (a flat G gives no move an
  improvement to take), not a defect in the chains.
- With a G that rewards precedence length: **:constructed** for
  M-futonzero-generative, M-federated-agency-hardening,
  M-futon-forward-model (the three fully-covered targets tried). The
  chains are mechanically viable; the real constructibility measure under
  the judge's G is the probe's downstream step, not this seat's.

## Missing-token findings

None — every needed precondition exists as a want token of its target.
The two uncovered kangaroo tasks and the daily-scan audit are missing
*patterns* (library gaps), listed above; those are gap-report candidates,
not token gaps.

## Summary

| target | files | patterns | wants covered |
|---|---|---|---|
| M-action-cost-modelling | yes | 4 | 5/6 |
| M-canon-fingerprint-store | yes | 4 | 4/6 |
| M-chipwitz-corps | yes | 5 | 5/6 |
| M-daily-scan | **skipped** | 0 | 0/2 (no honest pattern) |
| M-distributed-frontiermath | yes | 3 | 4/4 |
| M-federated-agency-hardening | yes | 4 | 4/4 |
| M-futon-forward-model | yes | 4 | 6/6 |
| M-futonzero-generative | yes | 2 | 2/2 |
| M-kangaroo | yes | 4 | 4/6 |
| M-wm-aif-policy-grain-compliance | yes | 4 | 4/4 |

Patterns drawn from 18 library families; every pattern id verified against
its file's `@flexiarg` line and every `:sha256` computed from the file at
probe time.
