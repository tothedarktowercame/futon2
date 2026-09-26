# E-kimi-task-71 — A-S-R3-I: mechanical reviewer form in observation-admission (futon2, for claude-8)

**Requisition:** in-progress — dispatched 2026-09-26T16:33:12Z to kimi-4 as invoke-1790440392431-24984-ca86afad

Clocked in by claude-12 for kimi-4 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet A-S-R3-I: the mechanical reviewer form in observation-admission (futon2)

Requisition from claude-8 (M-wm-wiring, PROOF-2a-PLAN ⟨2⟩2d F1). Dispatched by claude-12,
who reviews. The definition is already committed. **Read it first:** futon2 c9cba0b82,
`holes/labs/wm-contract/proof2/packets/A-S.md` §"Revision 3", items 1 and 2. Implement
exactly what item 1 says, plus the two pure helpers named below. **No store, no writer at
`observe-facts`, no reader.** Those are the next packets (A-LABELS-I, A-LABELS-WIRE-I).

## Files

- `src/futon2/aif/observation_admission.clj` (read the whole ns: `observer-view`,
  `view-digest`, `adjudication`, `review`, `admit` and its refusal order);
- `test/futon2/aif/observation_admission_test.clj` (existing tests must stay green).

## Build

1. **`mechanical-review`** `[reviewer-id subject adjudication]`: a pure function returning
   a review record of the same shape as `review`'s: `{:reviewer :of :finding :verdict}`,
   plus `:mechanical true` and, when not concurring, `:reason`.
   - `:verdict` is `:concur` exactly when all three hold:
     - the adjudication's `:observer` (the recomputation mechanism id) differs from the
       subject's declared check mechanism id (choose the key, e.g. `:check-mechanism`,
       and document it);
     - `(:view-digest adjudication) = (view-digest (observer-view subject))`;
     - `(:cutoff adjudication) = ` the subject's check cutoff (choose the key, e.g.
       `:check-cutoff`).
   - Otherwise `:verdict :dispute` with `:reason` naming the first failure, one of
     `:self-truthed`, `:view-mismatch`, `:cutoff-mismatch`, in that order. Say the
     order in the docstring.
   - It reads nothing else: no evidence, no recorded verdict.
   - A subject with no declared check mechanism id refuses typed (it cannot be judged
     `:self-truthed`); choose the refusal kind, e.g. `:check-mechanism-undeclared`.
   - A reviewer id equal to the observer, or to the check's mechanism id, must not
     concur. `admit` already refuses `:observer-is-reviewer` for the first; make sure
     the second cannot concur either, and say how.
2. **`admit`**, unchanged except that when it refuses `:review-not-concur` for a
   mechanical review, the refusal's data carries the review's `:reason`, so the record
   says why. Every other refusal and its order stays byte-for-byte the same; check the
   existing tests.
3. **Two pure helpers**, for Revision 3 item 2's "one label per subject" and "never
   stored":
   - `label-key` `[subject]` → `[class repo resolved-sha path-or-entry decl-or-nil
     check-mechanism]`, reading the evidence pointers;
   - `label-record` `[subject admit-result]` → `{:token-class … :recorded … :admitted …
     :label-key … :admission …}` for an `:admitted` result, **nil for any refusal**.
     A refused admission never becomes a label.

## Tests (add to `observation_admission_test.clj`)

The six falsifiers of Revision 3 item 3 that apply to this ns:

- **self-truthed:** mechanism id = check mechanism id → `:dispute :self-truthed`; `admit`
  refuses `:review-not-concur` with `:reason :self-truthed`; `label-record` is nil;
- **`:view-mismatch`** and **`:cutoff-mismatch`**, each the same way;
- **no reviewer:** `admit` refuses `:reviewer-missing`, and `label-record` is nil;
- **undeclared authorship:** `:authorship-undeclared`;
- **counted once:** the same subject checked on two ticks (differing only in tick or
  run id) gives equal `label-key`s, so `(count (distinct (map :label-key …)))` is 1.
  A changed check mechanism id gives a different key;
- **positive case:** a recomputation mechanism id ≠ the check's, same digest and cutoff,
  `:present` finding, author and enactor `:none` → `:admitted`, and `label-record`
  returns the label.
- **Bad case, run it and report it** (`git show <sha>^:<path>` into a temp dir on
  `-Sdeps :paths`; **no git stash**): make `mechanical-review` ignore the mechanism
  check. The self-truthed test must fail. Say so.

## Bar

- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, the admission test
  namespace plus `futon2.aif.observation-rates-test` if it exists.
- Warrant, **run from the futon2 root**:
  `AUTHOR=<you> scripts/wm/register-warrant.sh --pinned <sha>
  futon2.aif.observation-admission-test`. Report the warrant id.
- Commit only your two files, by explicit path. Never amend, never stash. Nothing under
  `data/`. Before committing, run `git diff <file>` and confirm no one else's hunks are
  in your files; if there are, stop and say so.

## Reply

Bell claude-12 back with the sha, the warrant id, the key names you chose, the refusal
order, the test lines, and the bad case.
