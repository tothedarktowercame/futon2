# L5-6 — Medic route: retire the legacy unsealed terminals so Q6's count can be 0 honestly (claude-2 → claude-3, 2026-11-01)

Base: futon2 `main` at `4a138a56d` (L5-2L-3 wired: `previous_run.clj` carrier keeps
`:refusal-history-unsealed`; mathlib4 `b3fcae050f` Q6 requires it 0). Finding in the plan
("Finding 2026-11-01 18:30Z"): the live stream has 8 closed terminals with no `:sha256` key, all
2026-10-22..24, pre-sealing; `intact?` counts them as unsealed; every later click would read Q6
violated until they are gone. Clicks are held; no click may run to test this. Read-only on
`data/` except as this packet says.

## Behaviour

1. **Distinguish.** `refusal-history` (and the predecessor carrier) classify a non-intact terminal
   as `:unsigned-legacy` when it has no `:sha256` key and its `:startedAt` precedes the first sealed
   terminal in the stream, else `:tampered`. Return `{:entries … :unsealed n :unsigned-legacy k
   :tampered m}` with `n = k + m` until retirement is applied.
2. **Retire.** A medic function appends to the run stream a sealed event
   `{:event :terminal-retired :run/id <id> :reason :unsigned-legacy :retired-at … :by <medic
   session> :sha256 <digest of the rest>}` for a legacy terminal; it refuses to retire a
   `:tampered` one (`:cannot-retire-tampered`). A retired terminal is excluded from the history
   and from the unsealed count; the retirement itself is recorded in the injury ledger
   (`meta_injury_store`, the medic's existing act shape; read how the medic records acts today and
   reuse it).
3. **Apply once, by hand, with a receipt.** Run the medic over the live stream for exactly the
   eight run ids in the finding, under your own session id, writing the retirement events; keep the
   list of appended lines and their digests in `~/notes/l5-6/retirements.edn` and paste them. After
   that, `refusal-history` on the live stream must report `:unsealed 0`; paste the probe output
   (`/tmp/claude2-unsealed4.clj` is the probe I used; copy it under `~/notes/l5-6/`).
4. **Projector.** `refusalHistoryUnsealed` stays the post-retirement count; add
   `refusalHistoryRetired` as a fact (count) for the report card; no Lean change.

## Tests

- A stream fixture with one legacy and one tampered terminal: classification; retiring the legacy
  one drops it from history and count; retiring the tampered one refuses; the retirement event is
  sealed and verifies.
- The 11-01 record's export: `refusalHistoryUnsealed` typed absent (old record), unchanged.
- Named and inner-law namespaces vs `main`; the hermetic root guards tests (the live append in
  step 3 is the one deliberate write, outside tests).

## Gates

kondo, parens, no reload, no merge. Branch `claude3/l5-6-retire-legacy`. Bell claude-2 with sha,
the eight retirement lines, the probe output showing 0, counts vs main.
