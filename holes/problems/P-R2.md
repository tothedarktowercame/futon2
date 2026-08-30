# P-R2 — The observation vector: declared channels, and the one channel it does not read

Problem record (delivery-lifecycle v2). Node R2 (PERCEIVE). Lane 2 of the R-node build.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. Source: `futon2/holes/labs/wm-contract/R2-glossary-formalisation.md` (carry its §3–§5 forward; do not re-derive).

## S1

**problem.** R2's noun is stack-defined: a 14-channel vector (`observation.clj:18–32`) whose docstring says
13, with two trace records carrying 13 keys — schema drift on 05-18 with no receipt (worksheet §4 e1). Eight
channels have a likelihood; six are delivered to no consumer (`:n-a-by-design`) and the record cannot tell
present-and-unused from absent. The ring finding (worksheet §0): the one channel the machine cannot
fabricate — a typed operator turn (✘ ✓ 💡) — is the one channel it does not read; `:acknowledged?` has no
producer and `nag?` is a 4-term AND with one input unwired (e6). PREREG invariant: the vector's key set
is a type, not a docstring.

**now.** Exactly-once turn storage is witnessed (e3, C1 20/20) — storage, not reading. `gen_turn_chain.py`
joins turns to patterns for the *paper* (e4), not for the vector (e5 missing). `promotion-tests.edn:36–50`
states the requirement `readsTurns` (an inference over a ≥111-item window must differ when the turn channel
is held constant) with two refusing witnesses named and no accepting witness — no tick has ever carried the
channel.

**solved:**
1. `Channel : Type` enumerated (14), `Observation := Channel → [0,1]`, `likelihood : Channel → Option …`
   (None for the six, as a type), and the record contract `∀ tick, keys tick.observation = Channel` checked
   over `futon2/data/wm-trace/` with the **reader loop over all top-level forms** (never `edn/read-string`).
   **Falsifier:** the two 13-key records of 05-18 must fire; if the checker passes the whole corpus, it is
   reading the first form only (the error of 08-27, lifecycle log row 1).
2. The turn channel: **blocked on a design decision** (what content of a turn→pattern association
   normalises to [0,1]; what hidden state a ✘/✓/💡 bears on). The deliverable for this half is the refusal
   with the two candidate answers laid out for Joe — not `:operator-turn-count`, which the excursion already
   names as the defect recreated (presence, not content).

**facades:** "13 harmonized channels. Real." (a count, and the wrong one); a docstring as a schema; a
turn *count* channel; `:morning-brief-*` present-and-empty read as observed; the C1 20/20 offered as
evidence of reading.

**status.** open.
**holder.** claude-20 (tech lead) → codex-8 (R2-D1)  
**parent.** BUILD  *(fifth precept, §0.10 — added 2026-08-30)*

## Edges (overlap points)
`R2→R3` observe (drawn), `R16→R2` re-observe (drawn), `R2→R8`, `R2→R7`, `R10→R2`, `R9→R2` (derived).
Deliveries e1–e7 as in worksheet §4, each with the undeclared field named there; the edge schemas are fixed
in `P-control-map-lint.md`'s fixtures. Payload for `R2→R3` and `R2→R8`: `Observation` with its `Channel`
key set as the schema and `receipt = {tick, key-set-ok?, consumed-by}`.

## deliveries
- **R2-D1 — discovery, no code.** Census over the trace with the reader loop: per record, schema version,
  key set, which channels carry a likelihood, which are `:n-a-by-design`; the two 13-key records identified
  by tick; `:acknowledged?` producers (expected: none); `:morning-brief-*` present/empty/absent counts.
  ≤ 200 lines, file:line and tick ids. Refusal permitted.
- **R2-D2 — build (after review).** The `Channel` type + record-contract checker as a check script
  (`futon2/checks/` or where review says), acceptance = the falsifier fires on the two records and nothing
  else; kondo/parens; run twice, deterministic.
- **R2-D3 — decision, not a build.** The turn-channel design question written up for Joe from D1's data.

## log
- 2026-08-30 record written (claude-15).
