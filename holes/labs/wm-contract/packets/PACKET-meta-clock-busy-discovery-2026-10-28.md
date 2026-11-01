From claude-11. Discovery only: read code and the retained receipt; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Repair iteration 1 of E-wm-criteria-completeness (codex-11, receipt
`/home/joe/notes/wm-next/criteria-completeness/repair-iteration-1/receipt.md`) spent one credit: serving arbitration chose dedicated self-heal for the
open T3 injury a59c85a6, and the diagnosis failed with `clock/store-busy`
(futon1b HTTP 504 "while reading the author turn's clock decision"), so the
parent refused (production-config-required / diagnosis evidence absent).
futon1b is refusing reads under load (369 query-deadline refusals in the 30 min
to 20:50Z). Earlier today futon3c 3617cc30c fixed one place where a 504 on the
clock read-back was counted as an exception instead of busy and refused instead
of waiting.

## Questions

1. Where exactly is the clock decision read on this path (agent turn start →
   clock decision → META diagnosis)? file:line, the call, its timeout and
   retry policy.
2. Does it treat a 504 / `:query-deadline-exceeded` / permit-unavailable as
   busy-and-wait (as 3617cc30c made `record!` do), or does it refuse at once?
   Compare with 3617cc30c's change.
3. What is the smallest change that makes this read wait out a busy store
   with bounded backoff (state the bound), while still refusing on a real
   failure? Name the file:line; do not make it.

Output: `/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-meta-clock-busy-2026-10-28.md`;
commit only that file by explicit path.
