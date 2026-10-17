# Packet B receipt: selection/G identity binding

**Excursion:** E-implementing-G-over-cascades, Packet B
**Worktree:** `/home/joe/code/futon2-pktb-zai7`, branch
`zai-7/packet-b-selection-identity`, base `92295c522` (main).
**Agent:** zai-7 (requisition invoke-1792196349701-15970 from codex-68).
**Date:** 2026-10-17

## What was built

`src/futon2/aif/wm/selection_cascade_identity.clj` (new): the pre-selector
join.  For each ranked entry it (1) refuses a pattern-grain arranged
candidate at the earliest point (`:occurrence-identity-absent`, catching the
shape scorer's bare-pattern-id occurrence fallback), (2) builds the
canonical structural layer from the entry's EXACT arranged candidate plus
occurrence-keyed 象 readings via Packet A's `build-structural`, (3) refines
it via `build` — authored operational additions only, every unauthored node
an EXPLICIT open hole (`:owner :wm/enactment-compiler`,
`:re-entry :packet-c-enactment-compiler`), never a default — (4) validates
with the one validator, and (5) binds the certified cascade digest into the
entry, its `:action`, its scorer `:certificate`, and its `:selection-law`
receipt.  An incoming action that already carries a different
`:cascade-digest` is refused (`:identity-forged`) — a pre-substituted chain
is not adopted.

`verify-selected-identity` re-checks after selection: the chosen action's
digest names exactly one ranked receipt, and that entry's carried record
still re-digests and re-validates to the same identity.

`src/futon2/aif/wm/family_selection.clj`: one opt-in hook
`:bind-cascade-identity {:readings fn :authored fn}`, applied between
ranking and `:attach-f`.  Refused entries are removed from the ranked set
with typed failures (added to `:failures` and to the row's policy summaries
as `:cascade-refused`); the one selector then sees only canonically bound
candidates.  Without the option, selection is byte-for-byte unchanged
(existing family/decision tests green).  Runner, enactment, executable
validator internals, journal code, Lean: untouched.

## Gates (focused runs, futon2 `clojure -M:test`)

- Reviewed combined gate: **34 tests / 168 assertions, 0 failures**, covering
  `selection-cascade-identity-test`, `family-selection-test`, and
  `executable-cascade-test`.
- `reading-family-decision-test` + `target-policy-field-decision-test`: 0 failures
- `q6-repeat-after-refusal-test`: 8 failures — PRE-EXISTING at base
  92295c522 (verified by stashing this packet's changes and rerunning:
  same 8 failures; also fails without any of my files present)
- clj-kondo 0/0 on all three touched files; check-parens clean.

## Planted controls

1. **Pattern-grain fallback unselectable.** Without the join, a bare-pattern
   cascade reaches the selector and can be chosen; with the join it is
   refused `:occurrence-identity-absent` before canonical construction and
   the occurrence-grain sibling is selected instead.
2. **Forged incoming digest.** An action pre-marked with a foreign
   `:cascade-digest` is refused (`:identity-forged`); the rebuilt digest is
   returned in the refusal.
3. **Whole-chain re-digest substitution.** (a) Outer record tampered and
   re-digested only: fails verification (`:record-re-digest-differs` /
   digest mismatch). (b) A fully resealed, internally consistent different
   chain: its digest matches no ranked receipt —
   `:digest-not-unique-in-menu` → `:cascade-identity-mismatch`.  The
   selection receipts bound pre-selector are what reject it.
4. **Descriptive order stays non-operational.** Real relation kinds
   (`:context-enables`, `:precedes`, witnessed `:overlap`) survive into
   `:reading-order-edges`/`:gluing`; `:application-order` stays empty
   without independent evidence; the frontier is honestly empty without
   authored clauses and opens only under authored clauses + evidence.
5. **Selector output substitution.** The production branch itself invokes
   `verify-selected-identity`; a planted selector that drops the chosen
   digest returns typed `:cascade-identity-mismatch` rather than `:selected`.

## Honest gaps

- The join is opt-in (`:bind-cascade-identity`); the live runner does not
  yet pass it — wiring into the runner/click loop is Packet C/D territory
  (forbidden here).
- 象 four-map squares in production are still typed unread at the shape
  layer; tests supply validated square receipts (the Packet A fixture).
  When real squares arrive, void/unread censuses flow through unchanged.
- The post-selection check is now mandatory inside the opted-in selection
  branch and its result is returned as `:cascade-identity`. The run record
  does not yet persist it (later runner wiring).
- The pre-existing q6 regression (8 failures at base) is untouched and
  reported, per the no-Q6-loosening constraint.
- Receipts recorded in the structural layer capture scorer version/G terms
  from the ranked entry; `:scorer-version` is not carried on the scorer
  entry itself today (it lives on the score map), so the receipt may read
  nil until family-selection threads it — cosmetic, digest binding unaffected.
