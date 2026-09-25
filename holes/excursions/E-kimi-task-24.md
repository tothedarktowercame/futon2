# E-kimi-task-24 — H-interp: one interpretation grammar, keys normalised and :forces required

Clocked in by claude-8 for kimi-6 on 2026-09-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

From claude-8 (PROOF-2 coordinator). Read-only shared checkouts: futon2 is `/home/joe/code/futon2` on `main`, mathlib4 is `/home/joe/code/mathlib4`. Other lanes have dirty files there: stage ONLY the explicit paths you create, never `git commit -a`, never `git stash`, never amend. Do not fire WM clicks, do not write under `data/`, do not `load-file` anything into a shared JVM. No justification may appeal to an operator ruling or to who said what; settle by definitions, code, and records, or say the definition is missing and propose it as an amendment. Absence is a typed absence on the record, never a substituted value. When done, bell claude-8 back with a two-paragraph summary and the commit sha(s).

Governing documents (all under `futon2/holes/labs/wm-contract/`): `PROOF-2-ASSUME-draft-2026-09-24.md` (A1-A21), `PROOF-2-THEOREM-draft-2026-09-24.md` (W0-W6, R1-R8), `PROOF-2-STRATEGY-draft-2026-09-24.md` (packet rows), `proof2/packets/CERT-S.md` (certificate key paths and hashing).
Standing ruling (Joe, 2026-09-25 03:25Z): the machine is NOT to be run. No flight, no click on any mission, no `--run`, no writes under `data/`, no loads into shared JVMs (:6768/:7070). Work is on one hole of PROOF-2a (`futon2/holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md`, "Holes" table and the clause it serves); test the component in its own process. Explicit-path commits only (`git commit -- <paths>`), never amend, never stash. Register test runs with `AUTHOR=<your-id> scripts/wm/register-warrant.sh --pinned <sha> <ns>` from the repo's own checkout. Bell claude-8 with shas, warrant ids, and the answers asked for.
## Fix packet: H-interp, gaps 1 and 2 — one interpretation grammar: keys normalised at intake and :forces required
From H-INTERP-D.md (53494373, kimi-3; read §1, §2, §4 first). Two grammars named "interpretation" exist: the lenient want-interpretation reply `{:schema :wm/want-interpretation-response-v1 :pattern :guard {:needs :forbids} :produces :receipt {:source {:path :sha256} :reading :scope-limit :by}}` and the stricter job receipt `:wm/interpreted-pattern-set-v1`; claude-1's hand units spell `:scope`/`:author` and carry `:forces`. Gap 1 (mechanical): normalise `:scope -> :scope-limit` and `:author -> :by` at intake in `futon2/src/futon2/aif/want_interpretation.clj`'s response validation, so a hand unit and a seat reply are the same record. Gap 2: `:forces` becomes required in the response grammar (the pressure the pattern answers; kimi-3's exhibited bad case: tier-0 rank-6 `translation/test-by-reproducing-behaviour` answered with invented produces passes every machine check today). Change, futon2 only, `want_interpretation.clj` and its test namespace; do not touch the prompt or retrieval (E-kimi-task-23 does). Bad cases as tests: (1) claude-1's `gauntlet/placenta-transfer` unit from `futon3c/holes/labs/M-futon-seams/item6/instance-4.edn` (quote it verbatim into the test, live-pin rule) validates after normalisation with `:scope-limit` and `:by` populated from `:scope` and `:author`; (2) a reply lacking `:forces` is refused `:forces-required`; (3) a reply carrying both `:scope` and a different `:scope-limit` is refused, not silently merged. State in the bell whether any existing fixture or recorded response in the tree lacks `:forces` and would now be refused, with paths; do not edit those. Gates: clj-kondo, check-parens, the namespace once; register with AUTHOR=kimi-6. One commit, explicit paths. Bell claude-8.

## Outcome (kimi-6, 2026-09-25)

Implemented in `src/futon2/aif/want_interpretation.clj` + `test/futon2/aif/want_interpretation_test.clj`:

- Gap 1: `wi/normalise-receipt` renames hand-unit spellings `:scope` → `:scope-limit` and
  `:author` → `:by` at intake in `validate-response`; both spellings present with different
  values is refused `:receipt-key-conflict` (never merged). The validated interpretation and
  receipt carry the canonical keys.
- Gap 2: `:forces` is required; a reply without it (or blank) is rejected `:forces-required`,
  and a valid interpretation record now carries `:forces`.
- Tests: claude-1's `gauntlet/placenta-transfer` unit quoted verbatim from futon3c
  `holes/labs/M-futon-seams/proto/instance-4.edn` validates with normalised keys (library bytes
  pinned at `test/fixtures/want-interp-library/futon3/library/gauntlet/placenta-transfer.flexiarg`,
  sha256 9771eca5… matching the live file); `:forces` absence → `:forces-required`;
  conflicting `:scope`/`:scope-limit` → `:receipt-key-conflict`. Namespace: 21 tests,
  74 assertions, 0 failures. clj-kondo clean (no separate check-parens gate exists in the tree).

Commit: swept into Joe's `8f85742a` ("H-interp gap 3…"), which landed on the shared checkout
while the task-24 edits were in the working tree; the commit contains both lanes' changes
(its own message notes "E-kimi-task-24 owns receipt key normalisation"). Not amended.
Warrant: `test-registry-909d540982406ab6fa1d677206a47057b3978466017473c081dbd822dcb327f6`
(AUTHOR=kimi-6, pinned 8f85742a, `:warrant? true`, `:postcheck :matched`).

Recorded response now refused: the pinned proposal fixture
`test/fixtures/want-interp-library/M-futon-seams-interpretations@futon2-78439f58.edn`
carries no `:forces` on any entry (0 occurrences) — as recorded it would be refused
`:forces-required`. Not edited; the test's `response` helper supplies a `:forces` with a
comment saying why. The instance-6 fixture
(`M-futon-seams-instance-6@futon3c-6149272b.edn`) carries `:forces` on every entry and is
unaffected.
