# E-kimi-task-70 — F1a-2-I: write measured-A version on the tick's admitted decision (futon2, for claude-8)

**Requisition:** in-progress — dispatched 2026-09-26T14:55:31Z to kimi-3 as invoke-1790434531104-24910-912c51d7

Clocked in by claude-12 for kimi-3 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet F1a-2-I — write the measured-A version on the tick's admitted decision (futon2)

Requisition from claude-8 (M-wm-wiring, PROOF-2a-PLAN ⟨2⟩2d F1), dispatched by claude-12,
who reviews. **One behaviour, write only.**

## Read first

- `futon3c/holes/labs/M-wm-wiring/F1a-D.md` — table row A_i and §"So F1a is", item 2:
  "In the tick, at `war-machine/cascade-decision-admitted`: write the measured-A version the
  step's likelihood will use. … It is a write only: the decision still scores with the class
  model." Also `F1-D.md` §5.
- `futon2/holes/labs/wm-contract/proof2/packets/SPEC-F.md` A10 (why F's conditioning step
  needs the measured token kernel's version).
- futon2 at HEAD 1302d39b: `scripts/futon2/report/war_machine.clj` —
  `cascade-decision-admitted` (~line 6245) and its callers; `cascade-lane` (~5751), whose R5
  step calls `observation-rates/sourced-rates` (~5899) and then uses
  `token-likelihood-rates`; `src/futon2/aif/observation_rates.clj` (`token-likelihood-rates`
  :156, `sourced-rates` :218).

## Build

At `cascade-decision-admitted`, compute the measured token rates the way `cascade-lane` does
(call `observation-rates/sourced-rates` → `token-likelihood-rates`; **reuse, do not copy the
arithmetic**; if the inputs `cascade-lane` uses are not reachable at the decision, say what
you passed and why) and write on the admitted decision:

    :measured-a {:schema :wm/measured-a-v1
                 :rates-sha <sha256 of a canonical serialisation of the rates value>
                 :source <sourced-rates' provenance, verbatim>
                 :classes <the classes covered>}

When no rates are sourced (no records): `:measured-a {:status :absent :reason
:no-measured-rates}`. **Never** a digest of a default/identity/zero kernel — an absence must
not read as a value. The decision's scoring is unchanged (the "two A's" question is separate).
No new required field, nothing refused. Do NOT touch `flight/run!` (claude-11's write 1, in
parallel).

## Wire test (same commit; `futon3c/test/futon3c/diagramprover/wm_wire_ledger_test.clj`
defines what a wire test is — read it)

- hermetic tick with sourced rates → decision carries `:measured-a` with `:rates-sha` equal
  to the digest of what `sourced-rates` returns for the same records;
- no records → the typed absence;
- **bad case**: make the no-records path digest an identity/zero kernel; the test fails. Say so.
- the existing decision score is byte-identical with and without the write (assert it).

## Map

Declare the field on the writing box in `futon3c/holes/labs/M-wm-wiring/wm-flight-wiring.edn`
(`:writes`; the reader comes with F1b). Map test green; re-pin the projection fixture if it
moves. Regenerate the ledger only from committed inputs: the org layer's uncommitted copy in
the futon3c tree belongs to another lane — leave it alone.

## Bar

- clj-kondo clean, `futon4/dev/check-parens.el`, the map test, and
  `futon2/scripts/wm/register-warrant.sh --pinned <sha> <test-ns>` run from the futon3c repo
  root; report warrant ids.
- Explicit-path commits; never amend; no stash; nothing under `data/` except the warrant
  ledger; no load into :6768; no flight, no click.
- **Before committing `war_machine.clj`, check that no flight is in the air** (a runner
  commit mid-flight makes the click refuse `:stale-runner-source`). If one is, wait and
  say so.

## Reply

Bell claude-12 back with shas (futon2, futon3c), warrant ids, the key path of the new record
on the decision, test lines and the bad case.
