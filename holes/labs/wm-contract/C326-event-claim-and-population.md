# C326 — authority interval and enumerable mutable-verdict population

Date: 2026-09-01

## Gate-facing event claim

`checks/contract_authority_current.clj` now brackets its reads with the mathlib
HEAD and `Holes.lean` blob identity. Its output carries the observation
interval and an explicit `:event-free` claim.

- without a declared fence, a content-correct check reports
  `PASS-CONTENT-ONLY (event-free unverified)`;
- with `--writer-fence ID` or `FUTON_WRITER_FENCE_ID=ID`, it reports
  `PASS (FENCE-CONDITIONAL ID)` and names that ID in structured output;
- endpoint movement is `:repository-basis-moved`, makes the substantive check
  fail, and records `:event-free? false`.

The workspace-gate command in C319 now supplies its already-established fence
ID through the environment. This is a declaration, not a lock acquisition.
The existing negative mutation remains rejected.

`checks/writer_fence_evidence.py` remains intentionally event/interval-shaped.
It observes two endpoints and distinguishes breach, indeterminate movement,
and verifiable fenced state. It should not be converted to a content claim.

## Reconstructable population

The exact lexical criterion behind C293's unlisted “90” cannot be recovered.
Rather than pretending otherwise, C326 defines a new versioned criterion in
`scripts/mutable_verdict_population.bb`:

- top-level executable `.clj`, `.bb`, and `.py` files under `checks`;
- fixtures excluded;
- source contains at least one named filesystem, process, Git, or HTTP mutable
  boundary token;
- names, criterion, and count are emitted together as EDN.

Canonical invocation:

```sh
bb scripts/mutable_verdict_population.bb
```

At this commit it emits **68 named members**. The count can no longer survive
without its referents because both are produced by the same evaluation. This
is a deliberately broad discovery population, not an assertion that all 68
have a hybrid window.

## Remaining limitation

The unknown 29-member arithmetic remainder is retired as an unauditable
historical count, not silently declared complete. The replacement limitation
is now actionable:

> 68 programs are named by `:mutable-verdict-population/v1`; their audit status
> must be reconciled by name. `contract_authority_current` is sound only as a
> content verdict when unfenced and as an event verdict under the named fence.

C326 does not audit or bulk-declare the 68 programs.
