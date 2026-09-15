# F13 redo: agent-authored interpretations over mission facts

Target: **M-zaif-harness-v1**, OPEN. Rule: the allowed tension-clarity fallback over the four requested mission roots, without the Phase-8 allow-list. IDENTIFY 44–47 names built components but missing readiness evidence; MAP 101–110 names ready and missing pieces; the later checkpoint 208–215 records fixes and pending independent replay. `target-selection.edn` retains the full mission-file inventory and an unrestricted 148-entry ranking found in the latest inspected tick. That ranking is inside inherited repair backtraces, not a fresh ranking; its third entry corroborates this choice but does not authorize or determine it. The first two ranked IDs have no mission file in the requested roots. No current unrestricted ranking was established by this read-only inspection.

Output: `interpreted-pattern-set.edn`. It includes every returned candidate's authored relevance judgment and source clauses, nine named Boolean facts with exact mission spans and q0, seven documented interpretations, one new pattern, and typed findings. `retrieval.json` retains both retrievers' actual tension-query and title/Status comparison outputs; no new pattern was in either retrieval arm. Existing embedding construction used its defaults and local model snapshot 1110a243fdf4706b3f48f1d95db1a4f5529b4d41, with no truncation. Tier-0 used `cas_select.retrieve_all(k=8)` over the whole index, not its mathematics-only loader. Retrieval proposes; the agent read and judged IF/HOWEVER/THEN. No mechanical prose-to-guard compiler was used.

| Retriever | Tension query: relevant/returned | Title/Status query: relevant/returned |
|---|---:|---:|
| Existing embedding constructor | 8/10 | 1/3 |
| Existing whole-index Tier-0 | 3/8 | 1/8 |

These are one author's relevance judgments for one mission, with unequal constructor lengths; not a general quality estimate. All candidates, including rejected ones and the missing local `f2/p4` source, are retained. `retrieval-index.tsv` reproduces exactly the pre-genesis index digest recorded during retrieval.

New pattern: `coordination/bind-promotion-to-post-repair-replay`, authored in futon3 with one index row and a validated/stamped canonical sigil. It fills the retrieved cross-validation pattern's missing revision binding and request/completion distinction. Its projected effect is request recorded plus certificate pending, **never replay successful**. Both facts are already true in q0, so it is idempotent here and does not claim progress. Revision matching is not represented by these Booleans; actual certificate promotion remains a hole.

The other interpretations describe successful-work postconditions, not guaranteed transition probabilities. Building tests can fail; a replay can fail. No stochastic or deterministic runtime scorer consumes these contracts in this packet. q0 is what the pinned mission asserts, including its later checkpoint, not fresh independent runtime measurement. Earlier Missing per-node suite and reporting gate assertions remain source-bound but require refresh before live use.

Findings: C missing (Joe's question); runtime facts unverified outside the source document; stochastic outcomes unmodelled; revision identity absent from the Boolean projection; six relevant-pattern interpretation holes (provisional ledger fields, devmap maturity tags, composed-dimension census, problem-node routing, port-level types, cascade-order carrier). No scoring, reloads, clicks, or configuration changes.

Validation: kondo 0/0 and futon4 check-parens on all three new EDN artifacts; fresh tooling JVM checks EDN round-trip, Boolean q0, seven guards/effects over the named vocabulary, and no effect claiming replay success or promotion. Source digest checks include the historical index snapshot. No production Clojure code changed. `assemble.py` packages the explicit judgments into JSON; EDN is its exported form. The sigil receipt is `sigil-validation.edn`.

Shared-tree discipline: the lead's discovery note and parent README are untouched. The futon3 index had pre-existing unstaged edits; `new-index-row.patch` stages only this packet's appended row against HEAD, leaving those edits unstaged.
