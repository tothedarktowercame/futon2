From claude-11. Discovery only: read code, ledger (read-only), transaction state and receipts; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Injury 2aef7e0c has refused three times on patch identity (retry-attribution
twice, then `:coordinator/resume-identity-refused` in repair iteration 3,
`/home/joe/notes/wm-next/criteria-completeness/repair-iteration-3/receipt.md`).
The earlier discovery's premise was wrong: `87c10683a` is not an unrelated
HEAD. It is a second repair of the same injury, authored by the machine
itself (trailers: `Agent-Id: wm-author`, `Agency-Job:
invoke-1793142252991-5686-9491e828`, `Dispatched-By: wm-full-loop`, 10-28
00:06, "author refusal: classify already-satisfied REFUSE report text
(injury 2aef7e0c)"), built on the ledger's recorded repair `02e9d5e7e`
(zai-2, 10-27 14:14). The transaction carries 87c10683 as its author receipt;
the ledger has no authored transition for it. So the machine's own repair
attempt was written to the transaction but never to the ledger.

## Questions

1. Which repair transaction/attempt authored 87c10683 (transaction id,
   attempt number, the click that ran it)? Cite the retained transaction
   state and receipts.
2. Where in the coordinator should the `:authored` ledger transition for that
   attempt have been appended (file:line), and why was it not (store-busy,
   crash, ordering, a branch that skips it)? Evidence from logs/receipts with
   timestamps.
3. Is there a supported recovery path in the coordinator / medic code that
   reconciles a transaction author receipt into the ledger (or that resumes
   from the ledger's last durable state, abandoning the transaction's
   unrecorded progress)? Cite it, or say there is none.
4. Smallest correct repair, not made: either (a) make the authored-transition
   append and the transaction receipt atomic / ordered so this cannot
   recur, plus the supported recovery for the existing divergence; or (b) if
   the design intends the ledger to lag, what the resume check should compare
   instead. Say which and why.

Output: `/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-2aef-ledger-transaction-divergence-2026-10-28.md`;
commit only that file by explicit path.
