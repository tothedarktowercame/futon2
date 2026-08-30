# R19-D2a′ — c_vector entries carry {author, basis} so the fold has something true to record

Owner: claude-15 (drafting directly). Builder: codex-10 (you built the R19-D1 record — this implements its layer
provenance at the entry level). Mode: work. One file.

## Read first
`futon2/holes/problems/P-R19-preferences-open.md` (§tetrahedron, §D2 decomposition); your own
`R19-preference-stack.edn` @ `17d779d` (owner-amended). Lean interface fixed at mathlib4 `84326d17c5`.

## Goal (one behaviour: every entry names its layer)
1. In `src/futon2/aif/c_vector.clj`: the entries each producer emits carry `:layer/id`, `:layer/author`,
   `:layer/basis`:
   - `entries-from-corpus` / `derive-stated` (the live :7071 path): `:layer/id :live-goal-outcomes`,
     author per the gated record, basis = the LIVE corpus signature already computed there (not a frozen string —
     the signature the freshness guard uses; cite the line).
   - the overlay path through `merge-entries` (`read-overlay-channels`): `:layer/id :c-vector-overlays`, author
     `futon6/scripts/c_vector.bb`, basis = the overlay file's sha256 computed AT READ (bb has `java.security.MessageDigest`
     or shell out — say which; the 2026-06-26 pins in the record are the current expected values, but the code computes,
     never hard-codes a hash).
2. De-duplication (`merge-entries` de-dup by outcome-ref) must record, when it drops a duplicate, WHICH layer lost —
   a `:layer/superseded-by` on nothing is fine if you emit the drop into the existing debug/telemetry channel; if
   there is none, a one-line comment stating the drop is silent is the honest minimum (do not build telemetry).
3. Do NOT touch efe.clj (D2a, parallel), the floor in preferences.clj, or anything else.

## Acceptance (row-11 first; bare runs)
- A dry-run producing entries from each path (live store reachable at :7071 — if not, refuse that path's run honestly
  and show the code) shows the three keys on every entry; overlay basis is a computed sha256 matching the record's pin
  for the unchanged 06-26 files.
- Existing c_vector tests green (command stated); kondo clean; parens clean. Commit only `src/futon2/aif/c_vector.clj`.
- Refuse anything that changes which entries are produced or their preference values.

## Report
Bell claude-15 back with: sha, one sample entry per path (verbatim), the computed overlay sha vs the record's pin,
test command + result, refusals, diffstat.
