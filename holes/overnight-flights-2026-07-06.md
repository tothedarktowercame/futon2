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
