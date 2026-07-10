# Spec: A3 — the thin, content-free actuator ("build this")

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews the *generality*).
**Tracks:** `futon2/holes/aif-wiring-actuator.html`. Consumes the reviewed
blueprint corpus (fold-turn deposits) and executes it into real substrate change.

## What A3 is (and is NOT)

A3 is a **general, content-free** executor. Given a reviewed blueprint it runs
the *identical* code path regardless of domain — it only ever touches
`{sorry, wiring, policy-holes, mission-path}`, never the blueprint's meaning.
The domain smarts (build a vocabulary, a SQLite store, a proof, …) are
**quarantined inside an outsourced builder**; the actuator dispatches "build
this" and verifies the result. It is NOT a vocabulary/pattern/whatever builder.

Generality is inherited from the fold's spec quality: the sorry is a **typespec**
and the wiring is a **structure-spec (DarkTower)**, so "build this, handoffs as
needed" is a well-posed instruction. If the specs are as good as we think, one
generic executor actuates all of them.

## Procedure (thin)

1. **Extract** the build package from a deposit (by `:fold-turn/id` or mission):
   the typespec (`:arrow-candidate` / want-signature) + structure-spec
   (`:wiring` nodes + `:turn :answer :policy-holes` — read holes from the ANSWER,
   per the ft-learning-loop-010 review finding).
2. **Dispatch "build this"** to a builder agent (reuse `agency_send.py`):
   *"Here is a typespec (sorry) and a DarkTower structure-spec (wiring). Build it;
   use handoffs as needed. Write the artifacts to the substrate endpoints the
   typespec names; advance the mission doc to close the corresponding hole(s).
   Return, for each closed hole, an EVIDENCE-REF: a substrate query (Drawbridge
   form, or a file+assertion) that independently witnesses the built artifact."*
3. **Verify the witness (the anti-gaming gate)** — for each claimed closure the
   executor **independently re-runs the evidence-ref** and confirms it resolves
   (content-free: confirms existence, does not interpret the artifact).
   **No witness, or witness does not resolve ⇒ the closure is REJECTED and the
   dial does not count.** (Mirrors `war_machine_pilot` `:executed? true requires
   :evidence-ref`.)
4. **Check the dials** (content-free acceptance):
   - **Universal**: the mission's `open-hole-count` decremented (re-parse the doc
     via `mission-registry`), AND the decrement is backed by a resolved witness
     from step 3.
   - **Substrate** (where the blueprint declares typed endpoints): the named
     endpoint's population changed, verified via **Drawbridge :6768** — NOT the
     :7071 HTTP API (blind; futon1a#6).
5. **Log** the actuation + dial deltas + witnesses to
   `futon2/logs/actuator-a3.log`.
6. **`--dry-run`**: extract + print the build package(s) it WOULD dispatch and the
   witness/dial checks it WOULD run — dispatch nothing, write nothing.

## Design decisions (owner-fixed; flag if you disagree)

1. **Thin actuator, builder does the write** — the executor dispatches + verifies;
   it does not itself mutate substrate. Control comes from the witness gate, not
   from the executor doing the write.
2. **Witness-gated dial** — a hole counts as closed ONLY with an executor-verified
   evidence-ref. This is non-negotiable: it is what stops the dial being gamed by
   a doc edit.
3. **Content-free** — no per-blueprint branches. Reads only structure + mission-path.
4. **Dry-run first** — no live builder dispatch until the mechanism is inspected.

## Acceptance bar

- **Generality**: the IDENTICAL executor runs on all 4 corpus blueprints
  (ft-learning-loop-010, ft-chipwitz-corps-001, ft-futonzero-generative-011,
  ft-pattern-mining-011) with **no per-blueprint code**.
- **Witness discipline (unit-testable, no live dispatch)**: a fake builder return
  with NO evidence-ref, or an evidence-ref that does not resolve, ⇒ closure
  rejected, dial does NOT move. A return with a resolving witness ⇒ closure
  accepted, dial moves.
- **Dry-run** over all 4 prints their build packages deterministically, dispatches
  nothing.
- **Spec-quality signal**: if a blueprint cannot produce a build package (missing
  typespec/structure), the executor reports WHY (spec-too-weak vs. instruction-
  inadequate) rather than silently skipping.

(Live end-to-end runs — where real builders mutate substrate and dials move — are
a SEPARATE operator-gated step after this mechanism passes review. Do NOT live-fire
builders as part of this build; ship the mechanism + dry-run + unit tests.)

## Gates (before bell-back)

- `clj-kondo` clean on all Clojure.
- `futon4/dev/check-parens.el` clean.
- Unit tests for: extract (all 4 deposits → build package), witness-gate
  (no-witness / unresolved-witness → reject; resolved → accept), dry-run
  determinism. NO test may live-dispatch a builder or write substrate.

## Constraints

Do NOT re-enable the hourly WM cron. Do NOT restart :7071 (futon1a#6). Verify
substrate populations via Drawbridge :6768, never the :7071 HTTP read API.
Bell **claude-4** back with a summary + commit SHAs. Small first pass fine — list
limitations honestly.
