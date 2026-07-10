# Spec: A3 — "review-any-partial" stage + executor-driven witness-gated doc advancement

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews the anti-gaming property).
**Extends:** `src/futon2/aif/actuator_a3.clj` (built + reviewed; commits 7c78518, 08391af, 6f9312d).
**Motivated by:** the first live run (zai-5 × ft-learning-loop-010). It built REAL artifacts
(5/8 witnesses resolve) but the dial did NOT move: `open-hole-count` stayed 6 because the
mission doc was never advanced. We must add the closure loop WITHOUT opening a gaming vector.

## The gaming vector to close

If we simply tell a builder "make the dial move," it can delete a hole marker from the
mission doc — count drops, it already has witnesses, and the gate `(≥1 witness AND count
decreased)` passes on a cosmetic edit. That is "announced is not sent" one level up.
**Rule: the `open-hole-count` decrement must never be a lever the builder controls.**

## Design

1. **Closure schema gains `:closes-mission-hole`** (optional, verbatim doc text):
   `{:hole-id :b1 :evidence-ref {...} :closes-mission-hole "<verbatim marker text from the mission doc>"}`
   The builder DECLARES which mission-doc hole a box closes; it does NOT edit the doc.

2. **`advance-mission-doc!` — executor-only, witness-gated.** For each closure, close its
   declared mission-doc hole IFF BOTH hold:
   - the closure's `:evidence-ref` **resolves** (independently re-run — existing witness gate), AND
   - `:closes-mission-hole` text is **present verbatim** in the mission doc.
   Then the executor edits the doc to mark that hole closed (check the task / strike the
   marker) and records before/after `open-hole-count`. The builder NEVER edits the doc.
   Idempotent: re-running does not double-close an already-closed hole.

3. **Tightened dial.** `dial-moved?` ⇔ the executor closed ≥1 mission-hole, each backed by a
   resolving witness AND a verbatim doc match. (Replaces the looser "≥1 witness AND any
   count drop".) A witness with no matching hole, or a declared hole with no witness, closes
   nothing.

4. **`review-partial` stage.** After witness-verify + `advance-mission-doc!`, emit a
   structured report:
   `{:resolved [...] :rejected [{:hole-id :reason ...}] :holes-closed [...] :holes-remaining [...] :next-feedback "<string>"}`
   - `:rejected` names each failed witness with its reason (unresolved / missing-evidence /
     hole-text-not-in-doc).
   - `:next-feedback` is the gate-anchored retry prompt (the verbatim rejection table + what's
     needed), suitable to re-dispatch to the builder. It must be the machine's findings, not
     free-form encouragement.
   The report surfaces to the reviewer AND is the builder's retry input.

5. **Retry** may be manual for the first pass (the owner re-dispatches the builder with
   `:next-feedback`). A bounded auto-retry loop is a follow-on, not this build.

## Acceptance (unit-testable — NO live dispatch, NO real substrate writes; use a temp mission-doc fixture)

- resolving witness + declared hole PRESENT in fixture doc → hole closed, `dial-moved?` true, count decremented.
- resolving witness + declared hole ABSENT from doc → nothing closed (no invented hole), `dial-moved?` false, `:rejected` names `:hole-text-not-in-doc`.
- NO/​unresolved witness + declared hole PRESENT → nothing closed (no gaming), `:rejected` names the witness reason.
- `advance-mission-doc!` idempotent: second run closes nothing new, count stable.
- `review-partial` breakdown correct on a mixed set (some resolved+closed, some rejected).

## Gates (before bell-back)

- clj-kondo clean; `futon4/dev/check-parens.el` clean.
- The acceptance tests above green. NO test may live-dispatch a builder or edit a real
  mission doc — operate on a temp fixture doc.

## Constraints

Do NOT restart :7071. Drawbridge witnesses are known-impractical (`:6768` forbids
`slurp`/`load-file`) — file-based witnesses are the reliable kind; note this, don't rely on
Drawbridge in tests. Bell **claude-4** back with a summary + commit SHAs.
