# Spec (c): couple SELECTION → AUTHORING so the system always runs forward

**Owner/reviewer:** claude-4 (author ≠ reviewer; Codex builds, claude-4 reviews)
**Date:** 2026-07-06
**Tracks:** `futon2/holes/aif-wiring-actuator.html` — this closes the missing
**upstream arc** the actuator depends on (selection currently never triggers
authoring, so a stuck mission never gets a blueprint).

## The gap (diagnosed)

The full deliberation pipeline works: a mission is selected and ranked. But when
the top lane mission has **no fold-turn deposit**, the gate returns
`:abstain-missing-leg` (`futon2/src/futon2/aif/close_loop.clj:93` — nil ΔF/ΔG)
and it stays stuck forever. Authoring a deposit **only** happens via the
wall-clock overnight cron (`futon3c/scripts/overnight_zai_flight.sh`), which
picks from the *last log*, not from a live selection. So a mission the WM is
actively selecting (e.g. M-learning-loop) can abstain every tick and never pull
its own blueprint. **Nothing couples selection to authoring.**

## Goal

A callable dispatcher: given the current WM selection, if the top-ranked lane
mission is `:abstain-missing-leg` (no deposit) AND `fold-authoring` mana is
available, **dispatch an authoring flight for THAT specific mission** — reusing
the existing overnight recipe, but parameterized by mission and bell-backed to a
reviewer. Idempotent, mana-bounded, dry-run-able.

## Design decisions (fixed by owner — flag to Joe if you disagree)

1. **Auto-fire within mana budget** (mana = the consent token), not
   recommend-only. Guards make it safe: idempotent + mana-bounded ⇒ no spam.
2. **Author bells the reviewer back** (`--from claude-4`), unlike the overnight
   no-reviewer path — this is the reviewed live lane.
3. **Callable-first, cron-re-enable-later.** Build the on-demand command so we
   can step it forward and watch it. Do NOT auto-re-enable the hourly WM cron
   (it is deliberately disabled in crontab); re-enabling with the coupling is a
   later, deliberate one-line change.

## Components

### (c1) Parameterized authoring flight — `futon3c/scripts/author_deposit_for.sh <mission-id> [--from <reviewer>]`
Generalize `overnight_zai_flight.sh`:
- Takes an explicit `<mission-id>` (skip its STEP 2 "pick from last log").
- Keep STEP 1 (consume 1 `fold-authoring` mana as first act; abort on refusal).
- Keep STEP 3/4 verbatim (full fold-turn contract; exemplars ft-autoclock-in-001
  golden bar, ft-peradam-mechanization-006 v2 shape; deposit to
  `futon6/data/fold-turns/`; prove loader acceptance + one tampered rejection).
- Pass `--from <reviewer>` to `agency_send.py` so completion bells back for review.
- Target an idle authoring agent from the roster (zai or codex).

### (c2) Dispatcher — `futon2/scripts/couple_selection_to_authoring.clj` (bb/clojure)
1. Obtain the current WM selection (reuse `wm_scheduled_run` decision path WITHOUT
   evidence emit; or read the latest decision record).
2. Find the top lane mission whose verdict is `:abstain-missing-leg` and which has
   NO deposit in `(esc/load-deposits)` (`futon2.aif.fold-escrow`).
3. **Idempotency guard:** skip if a deposit for that mission already exists in
   `futon6/data/fold-turns/`, or an authoring job for it is already in flight
   (check the invoke-jobs ledger / a lockfile).
4. **Mana guard:** read `futon2.aif.mana-gate` balance for `fold-authoring`; if 0,
   log `deferred — no mana` and stop.
5. If clear: invoke (c1) for that mission.
6. Log every decision (`fired` / `skipped-has-deposit` / `deferred-no-mana`) to
   `futon2/logs/selection-authoring-coupling.log`.
7. **`--dry-run`**: print what it WOULD do (mission chosen, guard results) without
   dispatching.

## Acceptance bar

- Mission w/o deposit + mana>0 → fires exactly ONE flight for THAT mission; logs `fired`.
- Same mission already has a deposit → `skipped-has-deposit`, fires nothing.
- mana=0 → `deferred-no-mana`, fires nothing.
- `--dry-run` against **M-learning-loop** (no deposit) → shows it WOULD author
  M-learning-loop, dispatches nothing.
- Live (mana permitting) end-to-end: a `ft-learning-loop-0NN.edn` appears in
  `futon6/data/fold-turns/`, loader-accepted, and a completion bell routes to claude-4.

## Gates (required before bell-back)

- `clj-kondo` clean on all Clojure.
- `futon4/dev/check-parens.el` clean on Clojure/Lisp.
- Existing `mana-gate` / `fold-escrow` tests still green; ADD a test for the
  dispatcher's idempotency + mana-defer branches (pure-fn level, no live dispatch).
- Determinism: `--dry-run` output stable across runs on unchanged inputs.

## Bell back

Bell **claude-4** back with a summary + commit SHAs. Do NOT re-enable the hourly
WM cron. Do NOT restart :7071 (see futon1a#6). Small-scale first pass is fine —
list any limitation honestly rather than widening scope.
