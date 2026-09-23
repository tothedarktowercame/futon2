# Remaining-steps walkthrough — zai-1's independent verdict

zai-1, 2026-09-23. Read-only. I read the proof file, the sign-off note
under ⟨1⟩10, the recorded closes for machinery-72 attempt-002 and
machinery-73 attempt-001, the banked ledger, and both split declarations.
My verdicts, disagreements stated plainly.

## 1. ⟨1⟩6 (accepted close) — I AGREE: not reachable now. Record it as
blocked with the arithmetic.

The chain from here to an accepted close is: register the v2 window
(a commit, registrable now) → 2 ticket-attempts recorded after the
registration instant → the collect limb acceptable → the calibration limb →
the ticket's Status written DONE → (c) observable. That is a minimum of
**two live clicks on this ticket** plus the acceptance write, against
three unspent clicks — but the decisive constraint is not the count, it is
Joe's method ruling: clicks are for the end-to-end claim only, and the
end-to-end claim is not what ⟨1⟩6's check tests. **What would falsify my
"blocked" verdict:** a path to (c) that needs no new ticket-attempt — e.g.
a Joe ruling that the acceptance for THIS ticket is something other than
the Status-DONE line, or that already-recorded runs count into the v2
window. Neither exists; the v2 registration instant exists precisely to
exclude the recorded runs. **Do not restate the step** — I agree with your
reading. The honest record is: the predicate works (proved on attempt-002
and in-process), the chain is closable as written (the fixture run at
`97152006`), and the check is blocked on click budget under the method
ruling. That is a statement about budget and rulings, not about the
machine.

## 2. ⟨1⟩7 (measured and attested) — I AGREE it is settleable today,
read-only, on the two closes; every part has a runnable form. Exactly what
I would run:

- **Part 1, measured not missing:** on machinery-72 attempt-002, the
  d-task evidence's after-token rows for the ticket's universe exist (the
  token-outcome receipt's comparison row is one of them: `[T
  :restoration-accepted]` :predicted-not-observed :observed **false** — a
  measured false, not a missing). The remaining wanted tokens' rows are in
  the same `:after-token-evidence` vector (task-stated measured true,
  split-declared-valid measured **true** at 0798f96a — the shape fix made
  that a measured true). machinery-73 attempt-001 the same: its own rows,
  measured, whatever they say. The runnability command: the load-identity
  readout (`bash scripts/wm_load_identity.sh` or the equivalent
  proof-eval form) at the recorded commits — both closes record their
  commits, and the load-identity registry pins digests.
- **Part 2, attested class recorded:** the closes' `:run-ending-classification`
  receipts — attempt-002 carries one (its schema is on the close; I read
  it). If machinery-73's close lacks the receipt, that part fails for
  that close and should be REPORTED as failing, not worked around.
- **Part 3, the classifier RERUN:** `run_ending_classification/verify-close`
  (pure) over each close's judgment + the recorded classification — and
  the rerun with the focus-receipt inputs from the same close, through the
  shared classify-target, comparing classes. All inputs are on the records.

**What would falsify:** a measured token reading `:missing` on a close
whose build actually delivered (it should not — the d-task rows cover the
universe), a missing attestation receipt, or the rerun disagreeing with
the recorded class. If part 2's receipt is absent on machinery-73, ⟨1⟩7
settles on attempt-002 alone and says so.

## 3. ⟨1⟩8 — I read the sign-off note the SAME way, with one sharpening.

The ⟨1⟩10 note is explicit: the discriminating part is the frozen
reference field recompute with and without the update; the next-click
receipt is corroboration. The theta identity fix (`f5e380b7`, `6cf508b6`)
changed what "the update" is — it is now keyed by the pattern that declared
the effect, and the banked ledger holds a real family (:contracts/
holder-states-the-claim, 3 trials / 0 successes / theta 1/8) that sits in
C2's precedence. So the discriminating half is settleable in-process
**against real trials**: recompute the reference field's G with theta
1/8 from the ledger vs theta 1 documented-default, and show the
difference. The corroboration half (a live next-click receipt showing
:theta-source :recorded-trials) needs a click and the third click ran
pre-fix — record it as unavailable without a click, and say which half is
which. **One sharpening I would add:** the with/without difference at
theta 1/8 (0 successes) moves G in a knowable direction but is a
*pessimistic* update — the falsification to pin is that G changes AT ALL
in the direction the update rule states, on the frozen input, which the
check already words correctly.

## 4. ⟨1⟩9 (Q.E.D.) — the verdict paragraph needs TWO changes.

The current paragraph says "demonstrated in-process for ⟨1⟩6–⟨1⟩8." As of
today that is now wrong in both directions:
- ⟨1⟩6 is no longer "demonstrated in-process" in the sense the paragraph
  implied — it is **blocked**, with the predicate demonstrated and the
  acceptance blocked on click budget under the method ruling. Say that.
- ⟨1⟩7 is now settleable on REAL closes (per answer 2), which is stronger
  than "in-process over fixtures" — if it settles, the paragraph should
  say so.
- ⟨1⟩8's discriminating half is settleable against real banked trials
  (per answer 3) — again stronger than fixture.

My proposed revision: *"Proved for ⟨1⟩1–⟨1⟩5. ⟨1⟩7 proved [settled
read-only on the recorded closes, if it settles as expected]. ⟨1⟩8's
discriminating half proved in-process against the banked trials; its
corroboration half and ⟨1⟩6's accepted close are blocked on live clicks
under the method ruling and remain unproved end to end. The full theorem
is not yet proved end to end."*

## Redundant or unfalsifiable checks — three cuts I would make

1. **The synthetic-success version of ⟨1⟩8's B-update test** (the
   `:after-observation true` fixture row in `theta_consumption_test` /
   the b-update tests' hand-recorded successes) is now redundant against
   the REAL banked family — the with/without recompute over the real
   3-trials/0-successes theta discriminates more than any synthetic
   success. Keep exactly one synthetic case (the cold-start Laplace
   arithmetic) and cut the rest.
2. **The v1 declaration's window checks** — its held-out labels can never
   be minted, so any check on the v1 window "closing" can no longer fail
   in an informative way (it fails by construction, forever). The v1 file
   is a record, not a live declaration; its only live check is that it
   still parses and satisfies its locator (already pinned). Cut anything
   that tests the v1 window's semantics.
3. **The `:no-acceptance-declared` re-pinning.** The acceptance plug
   (`8527577e`) and its no-declaration case are pinned in
   accepted-increment-test AND re-pinned in the wiring tests AND in the
   limbs tests. Two of the three copies can no longer fail independently;
   keep one.

**Nothing in ⟨1⟩7 is unfalsifiable** — every part can fail on the real
records. **⟨₁⟩6's check is currently unfalsifiable-by-execution** (it
cannot pass OR fail without clicks) — which is exactly why recording it
as blocked, rather than restating it, is the honest form.

---

# ⟨1⟩7 settlement — read-only on the two existing closes (zai-1, 2026-09-23)

Run as scoped: no clicks, no fixtures, no substitutions. Each close reported
separately. The runnability command: `bash scripts/wm_load_identity.sh`,
output `counts: {"current" 42, "unavailable" 2, "unregistered" 27}` — the
loaded source matches the canonical checkout (42 namespaces current; the 27
unregistered are namespaces that never registered, listed never omitted;
the 2 unavailable are outside this assessment's inputs).

## machinery-72 attempt-002 (commit 0798f96a)

- **Part 1, measured not missing: PASS.** All six wanted tokens have d-task
  after-token rows, each with a boolean `:result :observed`:
  `:admission/task-stated` **true**, `:repair/split-declared-valid`
  **true** (the shape fix's own measurement), `:obstruction-observed-cleared`
  **false**, `:held-out-observations-collected` **false**,
  `:calibration-evidence-present` **false**, `:restoration-accepted`
  **false**. No row is `:missing`.
- **Part 2, attested class recorded: FAIL — recorded as :unknown.** The
  run-ending-classification receipt EXISTS on the close and in retained/
  (`verify-close` returns **true** — the record is internally consistent),
  but its `:class` is **:unknown** with `:missing [:attested-increment]`:
  no route-attested increment, so the kernel honestly records unknown.
  The step's check asks for "an attested run-ending class recorded" —
  what is recorded is an honest unknown, not an attestation. **That part
  fails for this close as it stands.**
- **Part 3, classifier rerun agreement: PASS as an agreement.** The
  rerun (`verify-close`, pure, on the actual close + the recorded receipt)
  agrees — and `:class :unknown` is exactly what the rerun of an
  unattested close must produce. The agreement is genuine; the class it
  agrees on is unknown. (The full classifier rerun with focus inputs goes
  through `classify*` requiring the attested increment — absent — so it
  reproduces :unknown from the same inputs.)

## machinery-73 attempt-001 (commit 9c8032f6)

- **Part 1, measured not missing: PASS.** Identical shape: all six tokens
  with boolean observed values (task-stated true, split-declared-valid
  true, the other four false). No :missing. (I report these as found —
  they happen to match attempt-002's because the same ticket's evidence
  state was read at each close.)
- **Part 2, attested class recorded: FAIL — recorded as :unknown**, same
  cause: `:missing [:attested-increment]`, `verify-close` true. The
  receipt is not ABSENT (my walkthrough's tentative example was wrong —
  both closes carry receipts); the receipts record unknown honestly.

  One difference from attempt-002 worth naming: machinery-73's close is
  **on a different target's commit shape** — its d-task rows are the same
  six ticket tokens, which means the third click also selected this
  ticket (machinery-73 attempt-001 is the third click on
  T-repair-occ-444fb018, per the walkthrough's earlier finding).
- **Part 3, classifier rerun agreement: PASS as an agreement** — same as
  above: the rerun reproduces :unknown from the same inputs.

## ⟨1⟩7 CHECK verdict: **NOT MET — for either close.**

Part 1 passes on both closes: the measurement producer recorded every
wanted token as measured true or false, none missing. Part 3 passes on
both as an agreement check. **Part 2 fails on both closes**: the recorded
run-ending class is :unknown with `:missing [:attested-increment]` — no
attested increment exists, because an attestation requires the
route-attested increment the machine has not yet produced (an accepted
increment). The step's check as signed — "an attested run-ending class
recorded" — is not met on today's records. This is the honest coupling:
⟨1⟩7's part 2 is downstream of ⟨1⟩6's accepted close, and both closes
predate one. **A step that half-settles is recorded as half-settled:
part 1 and part 3 settled PASS on both closes; part 2 FAIL on both.**

## Falsification note

What would change part 2's verdict: a close carrying a route-attested
increment (which requires the accepted-increment path to have fired on
accepted work) — i.e. ⟨1⟩6 landing first. No read-only recomputation can
produce an attestation the machine never wrote; that is the correct
behavior of the classifier, not a defect in this settlement.
