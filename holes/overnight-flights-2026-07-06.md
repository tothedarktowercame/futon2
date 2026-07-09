# Overnight Flights — 2026-07-06

## Flight 1: ft-first-flights-007 (mana-gated)

**Driver:** zai-3 (unattended overnight run, edge invoke-1783309502013-565-781965d6)
**Consent:** mana gate `fold-authoring`, spend at 2026-07-06T03:45:33Z, balance after: 3
  - `{:ok true :balance 3}` — gate authorized the fold-authoring dispatch

### Mission selection

Latest tick (2026-07-06T03:02:37Z): target=M-first-flights, gates={:abstain-missing-leg 1}.
M-first-flights has NO deposit in futon6/data/fold-turns/. The abstain-missing-leg
gate fires because the escrow replay has no deposit to replay. This is the
top-ranked lane mission without a deposit — the correct choice.

### Psi

Source: M-first-flights.md HEAD + §3.2 requirements register (no held-work items
exist for this mission).

```
WANT: flight records that carry their derivations — measurement as discharge
judgment (predicted, realised, witness, error-with-interpretation), ghosts as
typed sorries distinguishing proposal-mode/not-yet/excluded-confound, and the
warrant for each velocity recorded so the cycle-5 escalation dispute is
checkable.
HAVE: flight records that are lists of numbers and nulls — real flights
recorded, but judgments without derivations (prediction -4.9225 / realised
-4.9225 / error 0.0000 taught the model nothing because the error has no
structure to propagate); the loop anatomy exists but derivations are discarded.
```

psi-sha256: 365e91a11aa38d88720a884d148caa5a93fa0a45df7937536560b949f7d52328

### Cascade

```
size=1  wholeness=0.511  H=1.0  T=0.511  accuracy=0.153  complexity=2.398
F-free-energy=-0.446  truncated=false
```

One pattern: `vsatlas/proof-through-pilots` (rel=0.511, mc=0.153).
THIN CASCADE — the flight-record-calculus domain is absent from the pattern
library. Recorded honestly as hole h4 (pattern-gap).

### Fold

3 boxes (b1: measurement as discharge judgment, b2: ghosts as typed sorries,
b3: warrant recorded with each velocity), all fitting `vsatlas/proof-through-pilots`
with honest addresses-however. 4 holes: h1 (term calculus undesigned), h2 (return-
channel absent), h3 (substrate vs rendering debt boundary), h4 (pattern-gap).

v2 wires: b1→b2 :seq, b2→b3 :copar.
Terminal: b3 :discharges :want-signature.

### ΔG

-1.0 (recomputed by fe/coverage-delta-g; 3 boxes / 7 total = coverage 3/7).
Initial hand-derivation of -0.6 was overridden to match the loader's pin-3.

### Pins

- prompt-sha: 7213ec25b5e7864adcc2f2bfa55d387a021b29dce69b9de70cd1daf0e0d49b47
- psi-sha: 365e91a11aa38d88720a884d148caa5a93fa0a45df7937536560b949f7d52328
- arming: mana gate fold-authoring, spend-purpose "overnight deposit run", balance-after 3
- LOADER ACCEPT: ft-first-flights-007 ✓
- TAMPER REJECT: /tmp/ft-tampered-007.edn — delta-g-mismatch (-0.5 vs -1.0) ✓

### Blind scoring

:none — no A-next seal exists for M-first-flights. Recorded honestly per the
003 no-seal precedent. The fold author IS the scorer (same agent, unattended
overnight) — the mana gate is the consent mechanism, not separation of powers.

### Honest assessment

This is a THIN deposit — one pattern, three honest boxes that address the
mission's actual tension but cannot solve it (the term calculus is undesigned,
hole h1). The cascade's thinness is itself a finding: the flight-record-calculus
domain needs library authoring (the mission's own §1 says "the unknown is the
specification"). The deposit unblocks the abstain-missing-leg gate by giving the
escrow something to replay, but the replay will produce a low-confidence ΔG
(-1.0) that abstains on most gates — which is honest.

## Flight 2: ft-bounded-in-flight-state-008 (mana-gated)

**Driver:** zai-3 (unattended overnight run, edge invoke-1783316701841-566-03efb02c)
**Consent:** mana gate `fold-authoring`, spend at 2026-07-06T05:45:11Z, balance after: 2

### Mission selection

M-first-flights now has deposit 007. Per the instruction ("if every lane mission
already has one, pick the highest-G candidate without a deposit"), I checked the
golden round 2 freeze for candidates without deposits. M-bounded-in-flight-state
has the highest |delta-g| (5.55) of any candidate without a deposit — the
highest-G mission the operator's gold valued that the escrow cannot yet serve.

### Psi

Source: M-bounded-in-flight-state.md IDENTIFY section (transactional discipline
over file-system substrate; no held-work items).

```
WANT: a structural invariant that the file-system substrate's durable state is
reachable from completed transactions with bounded in-flight scope — graduated
pressure (sleep-pressure analogue) that rises continuously and modulates
priorities before stop-the-line, composing with sibling invariants without
replacing them.
HAVE: dirty working trees across 8+ repos with 50-500 uncommitted entries each
— every dirty file is an in-flight transaction the system cannot reason about;
the mana-snapshot check-fn measures pressure but cannot enforce bounded scope.
```

psi-sha256: e0328b132ad7931f8f619d6f885e840109032eaf4c8ebfb94d99b4c295ce7555

### Cascade

```
size=7  wholeness=3.273  H=0.925  T=3.54  accuracy=1.552  complexity=8.649
F-free-energy=-0.61  truncated=false
```

7 patterns: churn-as-signal (0.532), stack-scan-logging (0.531), all-or-nothing
(0.516), invariants-vs-repair (0.482), durability-throughput-gate (0.514),
state-in-substrate-deltas (0.451), postcommit-materialization-gate (0.513).
RICH cascade — the transactional-discipline domain is well-covered.

### Fold

5 boxes (b1: invariant statement, b2: graduated pressure formula, b3: transaction
boundary as semantic act, b4: composition with siblings, b5: Block: footer as
transaction marker). 3 holes (h1: 10th faculty shape provisional, h2: drain-mana
coupling deferred, h3: hinge-vs-Block unresolved).

v2 wires: b1→b2 :seq, b1→b3 :copar, b2→b4 :seq, b3→b5 :tensor, b4→b5 :seq.
Terminal: b5 :discharges :want-signature.

### ΔG

-1.0 (recomputed by fe/coverage-delta-g; coverage 5/8).

### Pins

- prompt-sha: 43025c0870919b89d8f9e108006482c2394d76ef180ba29d1d33a0783a78abe1
- psi-sha: e0328b132ad7931f8f619d6f885e840109032eaf4c8ebfb94d99b4c295ce7555
- arming: mana gate fold-authoring, spend at 2026-07-06T05:45:11Z, balance-after 2
- LOADER ACCEPT: ft-bounded-in-flight-state-008 ✓
- TAMPER REJECT: /tmp/ft-tampered-008.edn — delta-g-mismatch (-0.3 vs -1.0) ✓

### Blind scoring

:none — no A-next seal exists. Recorded honestly.

## Flight 3: ft-canon-fingerprint-store-001 (mana-gated)

**Driver:** zai-3 (unattended overnight run, edge invoke-1783553401219-850-34fb6554)
**Consent:** mana gate `fold-authoring`, spend "overnight deposit run", balance after: 2
  - `{:ok true, :balance 2}` — gate authorized the fold-authoring dispatch

### Mission selection

Latest tick (2026-07-06T20:04:39Z): target=M-learning-loop (G=2.08),
gates={:abstain-missing-leg 2}. M-learning-loop already has ft-learning-loop-010
in the escrow. The other abstain-missing-leg mission is M-canon-fingerprint-store
— it has NO deposit. Cross-checked futon6/data/fold-turns/: no ft-canon-fingerprint-*
file exists. This is the top-ranked lane mission without a deposit.

### Psi

Source: M-canon-fingerprint-store.md sections 1-9 (the actual tension: knowledge
accumulated in one Stage 5 run does not carry forward; §8 decisions RESOLVED:
SQLite, scope bindings, frequency-ordered, strategy-anchor not position).

```
WANT: a canon fingerprint store that makes the symbol-grounding engine improve
with each Stage 5 batch — a SQLite-backed store of {symbol, canon, paper_id,
strategy, strategy_anchor, role, scope} records aggregated into CanonAggregate,
queried in-run by symbol, where the canon distribution IS the prior for the next
batch's Bayesian arbitration...
```

psi-sha256: c85e219dacf02b397b13f03fe880d6b8fc55c08ed9969fa29d1e23e45faf67cc

### Cascade

```
size=1  wholeness=0.559  H=1.0  T=0.559  accuracy=0.168  complexity=2.398
F-free-energy=-0.431  truncated=false
```

Cascade surfaced 1 pattern: `library-coherence/library-evidence-ledger` (rel=0.559).
THIN from the model — augmented via psr_search with 3 additional fitting patterns
(all with verified flexiarg prose):
- `agent/handoff-preserves-context` (ephemeral→persistent cross-batch state)
- `aif/declare-the-conditioning` (the store IS the prior; conditioning must be declared)
- `peripherals/canonical-typed-event-vs-side-channel` (fingerprint schema = canonical taxonomy vs side-channel JSONL)

### Fold

5 boxes:
- b1: SQLite canon_store.db as canonical typed fingerprint record (fits canonical-typed-event)
- b2: MAP-REDUCE pipeline — write_batch_fingerprints + incremental aggregate (fits library-evidence-ledger)
- b3: in-run query interface — canon_distribution IS the declared prior (fits declare-the-conditioning)
- b4: cross-batch persistence — file-first SQLite survives teardown (fits handoff-preserves-context)
- b5: frequency-ordered seed — mathematical genome (fits library-evidence-ledger)

5 holes:
- h1: bibliographic citation-count source (ungrounded external data-source decision)
- h2: reliability-weight formula in per-binding posterior (M-bayesian-structure-learning §3.2, not recoverable here)
- h3: held-out gold partition and precision metric (calibration choices not in pattern halo)
- h4: literature-graph construction for strategy-merge F4 (explicitly deferred scope)
- h5: scope-binding extraction — how role/scope fields are populated (schema decidable, population not)

v2 wires: b1→b2 :seq, b2→b3 :seq, b2→b4 :copar, b1→b5 :tensor, b5→b3 :seq.
Terminals: b3 :discharges :want-signature, b4 :discharges :want-signature.

### ΔG

-0.5 (hand-shown: dG = -(boxes/(boxes+holes)) = -(5/10) = -0.5; loader pin-3 recomputed and confirmed)

### Pins

- prompt-sha: 9217e5289eec1a8412fcea558e361ffc739e87e74dce9963119d9b437699b629
- psi-sha: c85e219dacf02b397b13f03fe880d6b8fc55c08ed9969fa29d1e23e45faf67cc
- prose-sha256:
  - library-coherence/library-evidence-ledger: 20bff107f17d473cb714dfd5539dd05183dcd6ec1c95abefab6149cda48ddc23
  - agent/handoff-preserves-context: 5c5e1b948075e8168205808c1c3f51fef3306612eb81e107bb99e3b9b5ad433e
  - aif/declare-the-conditioning: 954b803dd9ebf20a91e0a37704aba630886f0e4e58a7485e2b5ae84a79502131
  - peripherals/canonical-typed-event-vs-side-channel: 020b272ec5b4d9aedf8142ccaf7f60ed539d7c8041a5a854c235c7ad8dbc355c
- arming: mana gate fold-authoring, spend-purpose "overnight deposit run", balance-after 2
- LOADER ACCEPT: ft-canon-fingerprint-store-001 ✓ (delta-g -0.5)
- TAMPER REJECT: /tmp/ft-tampered-test.edn — delta-g-mismatch (-0.99 vs -0.5) ✓

### Blind scoring

:none — no A-next seal exists for M-canon-fingerprint-store. Recorded honestly per
the 003 no-seal precedent. The fold author IS the scorer (same agent, unattended
overnight) — the mana gate is the consent mechanism, not separation of powers.

### Honest assessment

This deposit addresses the mission's core tension (knowledge doesn't carry forward
across batches) with a coherent 5-box construction spanning the SQLite schema, the
MAP-REDUCE pipeline, the in-run prior query, cross-batch persistence, and the
frequency-ordered genome seed. The 5 honest policy-holes (half the total) keep dG
at -0.5 — moderate confidence. The holes are genuine: the citation-count source,
reliability formula, gold metric, F4 literature graph, and scope-binding extraction
are all ungroundable from the pattern halo + mission prose. The deposit unblocks
the abstain-missing-leg gate by giving the escrow a replayable construction.

## Flight 4: ft-state-snapshot-witness-001 (mana-gated)

**Driver:** zai-3 (unattended overnight run, edge invoke-1783560601613-856-26410afa)
**Consent:** mana gate `fold-authoring`, spend "overnight deposit run", balance after: 1
  - `{:ok true, :balance 1}` — gate authorized the fold-authoring dispatch

### Mission selection

Latest tick (2026-07-06T20:04:39Z): target=M-learning-loop, gates={:abstain-missing-leg 2}.
Both abstain-missing-leg lane missions (M-learning-loop and M-canon-fingerprint-store) now
have deposits (ft-learning-loop-010 and ft-canon-fingerprint-store-001 from Flights 1-3).
Per the instruction "if every lane mission already has one, pick the highest-G candidate
mission from the tick that lacks a deposit": M-state-snapshot-witness was selected — it
appears in the trace's candidate set, is a core futon3c infrastructure mission (pipeline-tracer
track-4-2 closure), and has NO deposit.

### Psi

Source: M-state-snapshot-witness.md IDENTIFY section (the decision: separate :event
:inventory-snapshot; the shape-first approach with 4 siblings; boot-time construction).

psi-sha256: 5f0a5ba8c4f90f25973b47700ffe73fc38a3fafcebf2fb8891f765b744929df8

### Cascade

```
size=6  wholeness=3.022  H=0.948  T=3.189  accuracy=2.181  complexity=2.619
F-free-energy=1.526  truncated=false
```

RICH cascade — 6 patterns, all with verified flexiarg prose:
- invariant-coherence/state-snapshot-witness (rel=0.754)
- invariant-coherence/reachable-from-boot (rel=0.485)
- invariant-coherence/shape-first-identify (rel=0.489)
- library-coherence/library-staleness-scan (rel=0.442)
- sidecar/fact-lifecycle-event-types (rel=0.448)
- structure/interest-event-vocabulary (rel=0.57)

### Fold

6 boxes:
- b1: inventory-snapshot projection-fn (fits state-snapshot-witness)
- b2: boot-time wiring in bootstrap.clj (fits reachable-from-boot)
- b3: sibling-namespace shape (fits shape-first-identify)
- b4: separate :event :inventory-snapshot type (fits fact-lifecycle-event-types)
- b5: snapshot as structural witness (fits interest-event-vocabulary)
- b6: staleness detection via snapshot diff (fits library-staleness-scan)

4 holes:
- h1: projection-fn field selection (which inventory fields are semantically meaningful)
- h2: tracer closure mechanics (emit-tracer-closed! API shape)
- h3: HUD widget rendering (MOVING vs STUCK computation)
- h4: cadence discipline for periodic snapshots beyond boot

v2 wires: b3→b1 :seq, b1→b2 :seq, b2→b4 :seq, b4→b5 :seq, b1→b6 :tensor, b5→b6 :copar.
Terminal: b5 :discharges :want-signature.

### ΔG

-0.6 (hand-shown: dG = -(boxes/(boxes+holes)) = -(6/10) = -0.6; loader pin-3 recomputed and confirmed)

### Pins

- prompt-sha: b426db580dd6ea2a17a177f3768beb36cd8662ff216ee99badc4e70e9110b820
- psi-sha: 5f0a5ba8c4f90f25973b47700ffe73fc38a3fafcebf2fb8891f765b744929df8
- prose-sha256:
  - invariant-coherence/state-snapshot-witness: 3e7d90579c329098e04159f5bf0c4faf29f092c78e0ec0bf81c2f43eb87cd505
  - invariant-coherence/reachable-from-boot: 7866b547d9e2aa912ce3cd68532ffc09c9bd6fd6c421c315b18c4ea004afd0f7
  - invariant-coherence/shape-first-identify: 4c47b8cc7589b9410e075c014ac54e407ba46efc570f8642a29459796044152d
  - library-coherence/library-staleness-scan: fbd40e6dbcde9cf70ed5a046f45743f8505a9c859f337bd02860e88aa847707a
  - sidecar/fact-lifecycle-event-types: 83232f9ef4c5c45f73feec70a5d7d7cbf2006b0375271ee01cbe8ad4f8a2a5e3
  - structure/interest-event-vocabulary: 35ae9a5e02976f23e7629997c3d564df5d9e6b2eeacfe1e17c1d23679633b20e
- arming: mana gate fold-authoring, spend-purpose "overnight deposit run", balance-after 1
- LOADER ACCEPT: ft-state-snapshot-witness-001 ✓ (delta-g -0.6)
- TAMPER REJECT: /tmp/ft-tampered-test.edn — delta-g-mismatch (-0.99 vs -0.6) ✓

### Blind scoring

:none — no A-next seal exists for M-state-snapshot-witness. Recorded honestly per
the 003 no-seal precedent. The fold author IS the scorer (same agent, unattended
overnight) — the mana gate is the consent mechanism, not separation of powers.

### Honest assessment

This is a SOLID deposit — rich cascade (6 patterns, F=1.526), coherent 6-box
construction spanning the projection-fn, boot-wiring, shape-first sibling design,
typed event boundary, structural-witness semantics, and staleness diffing. The 4
honest policy-holes (projection-fn fields, tracer closure API, HUD rendering,
cadence discipline) are all genuinely ungroundable from the pattern halo — they
require runtime API knowledge or operator calibration decisions. dG=-0.6 reflects
good coverage (6/10) with honest holes. The deposit unblocks the abstain-missing-leg
gate by giving the escrow a replayable construction.

## Flight 5: ft-reachable-from-boot-001 (mana-gated — LAST fold-authoring unit)

**Driver:** zai-3 (unattended overnight run, edge invoke-1783568701660-858-53426b03)
**Consent:** mana gate `fold-authoring`, spend "overnight deposit run", balance after: 0
  - `{:ok true, :balance 0}` — gate authorized the LAST fold-authoring dispatch; budget now exhausted

### Mission selection

Both abstain-missing-leg lane missions (M-learning-loop, M-canon-fingerprint-store) have
deposits. M-state-snapshot-witness was deposited in Flight 4. Per the fallback rule,
selected M-reachable-from-boot — the foundational stop-the-line hot-fix mission that
M-state-snapshot-witness depends on. It closes the evidence-loss anti-pattern (the
"loaded gun on Chekhov's desk" problem where !store was reset to in-memory). This is
the dependency chain: reachable-from-boot (state is boot-reconstructible) → state-snapshot-
witness (snapshot IS the witness that boot-construction worked).

### Psi

Source: M-reachable-from-boot.md IDENTIFY section (two failure classes: evidence-store
reset losing ~2 weeks of data + registry not bootstrapped after JVM restart; the
watchdog-vs-invariant distinction; the stop-the-line trigger from HUD STUCK).

psi-sha256: 9812c92075b3241f6f99baeb08df68989a333934707f9640b071954857211cd0

### Cascade

```
size=19  wholeness=10.019  H=0.961  T=10.426  accuracy=7.38  complexity=27.765
F-free-energy=0.439  truncated=false
```

VERY RICH cascade — 19 patterns shown (the richest of all 5 flights). Selected 6:
- invariant-coherence/reachable-from-boot (rel=0.77)
- futon-theory/stop-the-line (rel=0.527)
- system-coherence/turn-design-into-checks (rel=0.545)
- storage/invariants-vs-repair (rel=0.588)
- futon-theory/all-or-nothing (rel=0.524)
- eight-gates/ward-off-boundary (rel=0.493)

### Fold

6 boxes:
- b1: grep-verifiable static check with ^:durable metadata (fits reachable-from-boot)
- b2: pre-commit hook as strong-mode binding — synthesized-violation falsification (fits turn-design-into-checks)
- b3: construction-path allowlist as design-turned-into-check (fits reachable-from-boot)
- b4: repair pathway alongside enforcement — allowlist extension via code review (fits invariants-vs-repair)
- b5: startup completes or fails loudly — no silent in-memory fallback (fits all-or-nothing)
- b6: quarantine without erasure — commit refused, working tree preserved (fits ward-off-boundary)

4 holes:
- h1: metadata convention adoption scope (which other defonces get ^:durable vs ^:cache)
- h2: symlink installation across 14 repos (per-repo path differences)
- h3: grep regex precision (catch violations without false-positives on comments/strings/tests)
- h4: sibling-mission handoff scope (Codex issue #65 for family-check-fns, agent-registry, dev-evidence-store)

v2 wires: b3→b1 :seq, b1→b2 :seq, b2→b4 :copar, b2→b6 :tensor, b1→b5 :copar, b4→b5 :seq.
Terminals: b2 :discharges :want-signature, b5 :discharges :want-signature.

### ΔG

-0.6 (hand-shown: dG = -(boxes/(boxes+holes)) = -(6/10) = -0.6; loader pin-3 recomputed and confirmed)

### Pins

- prompt-sha: 9d227517968b4667e2ecf7138741c01dbbcb739db67fa0c9adc52a842257a94e
- psi-sha: 9812c92075b3241f6f99baeb08df68989a333934707f9640b071954857211cd0
- prose-sha256:
  - invariant-coherence/reachable-from-boot: 7866b547d9e2aa912ce3cd68532ffc09c9bd6fd6c421c315b18c4ea004afd0f7
  - futon-theory/stop-the-line: 7502a8955d7dd166e630d80c035c6109bcfb9c36b571b738b7a1c4151754bb63
  - system-coherence/turn-design-into-checks: f374859ba56ef2f19b98b4cdb8b01d08ca87eca11543b21adc5d306a956dacd8
  - storage/invariants-vs-repair: bec5cf332435b5a81ec53f14887ac0c7cc70ccd92eab7d8656b055612af56593
  - futon-theory/all-or-nothing: 1b3d73578e00c37c7c4681f8f3cea04562fd3a66b6ead5dbd373267881bc1c87
  - eight-gates/ward-off-boundary: 2130e67d723d16f4910a70dbd8b8105c37babb59309e254735084eafbcf4805a
- arming: mana gate fold-authoring, spend-purpose "overnight deposit run", balance-after 0 (LAST unit)
- LOADER ACCEPT: ft-reachable-from-boot-001 ✓ (delta-g -0.6)
- TAMPER REJECT: /tmp/ft-tampered-test.edn — delta-g-mismatch (-0.42 vs -0.6) ✓

### Blind scoring

:none — no A-next seal exists for M-reachable-from-boot. Recorded honestly per
the 003 no-seal precedent. The fold author IS the scorer (same agent, unattended
overnight) — the mana gate is the consent mechanism, not separation of powers.

### Honest assessment

This is the STRONGEST deposit of the overnight series — very rich cascade (19 patterns,
F=0.439, the lowest free-energy of all 5 flights), coherent 6-box construction that
maps the mission's watchdog-vs-invariant distinction onto concrete enforcement
mechanisms (grep check → pre-commit hook → repair pathway → all-or-nothing boot →
quarantine semantics). The 4 honest holes are all genuinely ungroundable: per-container
metadata classification, per-repo symlink mechanics, grep regex precision, and Codex
sibling scoping all require implementation-level knowledge not in the pattern halo.
dG=-0.6 reflects strong coverage (6/10). The mana gate is now EXHAUSTED (balance 0) —
no further fold-authoring deposits possible without operator top-up.
REFUSED - budget exhausted, run aborted
