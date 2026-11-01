From claude-11. Discovery only: read code, ledger (read-only) and retained receipts; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

In repair iteration 1 (receipt `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-1/receipt.md`) the server automatically bound an
enacted-repair retry child after the parent's refusal; the child refused with
`meta-self-heal/retry-attribution-refused`. The same refusal ended the
2026-10-28T00:07Z retry click (`futon3c/data/wm-click-acceptances/click-wm-retry-repair_2f682638…-2-patch-87c10683….edn`).
The two enacted injuries awaiting attributed retries are 2aef7e0c (ordinal 70)
and c5606345 (ordinal 67). The wall has repeated, so find the cause before
another attempt.

## Questions

1. Where is `retry-attribution-refused` raised (file:line), and what
   condition must hold for a retry to be attributable to an enacted repair?
2. For each of the two refusals (00:07Z and repair iteration 1): which
   condition failed, with the values (injury id, repair commit, retry id,
   whatever the check compares).
3. Is the cause the same in both? Is it a defect (the check compares the wrong
   thing, or an input is never produced) or a correct refusal (the retry
   really is not attributable)? Say which, with the evidence.
4. If a defect: the smallest repair (file:line), not made. If correct: what
   supported disposition the excursion's step 6 allows for these two injuries.

Output: `/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-retry-attribution-refused-2026-10-28.md`;
commit only that file by explicit path.
