# C380 — adversarial review of the quiet-run state machine

Date: 2026-09-01. Reviewed `scripts/wm_quiet_run_state.py` at `96118bf`
using temporary ledgers and fixture evidence only. No live fence, gate, reload,
click, certificate, or restoration was run.

## Verdict and fourth measurement

The state-machine shrink genuinely improves the **honest-path transition
logic**: no next-state skip is accepted through `advance`, release follows
`restored`, empty ledgers reject, and an initial-only ledger cannot release.

It does not yet make the evidence authoritative. This review found **six
defects**, predominantly gaps between what evidence files claim and what the
machine checks. The prior document counts, 9 → 8 → 8, are not directly
comparable to six in a new artifact. The character is decisive: five of six are
still claim/evidence binding failures, so shrinking reduced prose ambiguity but
did not remove the campaign's main cause.

## Findings

### 1. The hash chain is synthesizable and does not validate its state history

`receipt-sha256` is an unkeyed digest of caller-authored JSON. `load_ledger`
checks each digest and previous hash, but not that row 1 is `initial`, that
states follow `ORDER`, that fence IDs remain equal, or that each state's facts
and evidence meet its validator.

A one-row, correctly digested ledger whose genesis state was `click-issued`
loaded successfully. `advance --to click-terminal` then exited 0. This is a
mid-chain resume without any quiescence, fence, tested commit, or reload
evidence. The normal `advance` ordering check protects only ledgers previously
created through the normal interface.

### 2. Evidence hashes are recorded but never revalidated

Transitions store absolute path plus SHA-256. `load_ledger` and `status` never
re-read those paths or compare their current bytes. After a shape-only
quiescence transition, deleting its evidence file still left `status` at exit
0. Resume therefore relies on facts copied into the ledger, not the evidence
whose digest is presented as its authority. An earlier-attempt artifact can be
substituted at transition time and then disappear without invalidating state.

### 3. Several producer identities are only shapes

`quiescence` accepts the hand-written one-field object
`{"verdict":"QUIESCENT"}`. Reload, click-issued, terminal, and certificate
validators similarly inspect selected fields without binding to a producer
schema/receipt identity in every case. The committed full-chain test itself
uses hand-written gate, suite, click, certificate, restoration, journal, and
outcome files; this demonstrates that producer output is not required.

A different artifact of the same shape therefore fires a transition. Fence ID
narrows the fence pair, but reuse of an ID or a synthesized prior artifact is
not excluded by provenance.

### 4. “Actual gate start” is caller-authored

The 300-second age is computed from `gate["started-at"]`. The state machine does
not obtain that instant from the bounded service or compare it with current
time. A fence observation one day old was accepted at `tested-commit` after the
gate JSON claimed it began 100 seconds after that observation and finished 100
seconds later. The attestation merely expired in the future. Thus “actual” is
asserted by the receipt shape, not independently established.

### 5. Tested receipts are not joined to one tested basis

Gate and two suite receipts are individually required clean/stable and their
command strings form the expected population. Their repository heads, basis
identities, job IDs, and attempt/window identities are not compared. Receipts
from different attempts or commits can therefore be combined into one
`tested-commit` transition. The recorded fact takes only the gate finish head.

### 6. `restored` verifies records, not restoration reality

The transition checks `result.ok`, authenticated manifest shape, park journal
population, and outcome-row population. It performs no live post-restoration
observation. C372 established that attempt rows are forgeable; outcome rows
also carry a copied manifest tag rather than their own authentication. The
committed state-machine test hand-writes `{"ok": true}` and all outcome rows,
then reaches `restored` and `released`. Consequently `FENCE-RELEASE` is ordered
after a state named restored, but that state need not prove the writers were
actually restored.

## Empty and ordering controls that held

- A missing ledger rejects; a zero-length ledger rejects as
  `state-ledger-empty`.
- A legitimate initial row is nonempty state, not proof of later transitions;
  direct `initial -> released` rejects.
- Normal `advance` refuses skipped states.
- The state machine requires nonempty transition evidence except for the
  deliberately evidence-free release transition.
- The focused committed suite passed six tests.

## Additional parser boundary

`json_file` uses `raw_decode` and ignores the unconsumed suffix. Multiple-form
or trailing-garbage evidence can therefore be accepted by its first valid JSON
object. This reinforces the producer-binding findings above; it is not counted
as a seventh independent defect because strict producer parsing is part of the
same evidence-authority boundary.

The shrink worked on sequence logic. It did not yet work on evidence identity,
freshness authority, or restoration truth. This delivery reports only.
