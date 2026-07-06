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
