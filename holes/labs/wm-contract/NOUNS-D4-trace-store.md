# NOUNS-D4 — TRACE is a store member

TRACE is now an explicit `:kind :store` member in `p4ng/empirics-futon/control-stages.edn`, generated through `gen_control_stages.py`. It is placed in the **ACT** column because `war_machine.clj:4763` appends the terminal `:TRACE` route tag and invokes `write-trace!` after selection, and in the **assurance** band because persistence observes and records the loop rather than acting in its cycle.

The carrier is both route and stack at different boundaries: one trace records the executed route; `:store/wm-trace` is the append-only stack of those records. The existing typed hyper-edge precedent already names `:store/wm-trace` separately from `:node/R2`, `:mediator/R3a`, and `:node/R7`, so promoting it to an R-number would erase an existing useful distinction.

**`sec-catalog.tex` does not need a TRACE entry.** The catalogue contains behavioural patterns; the trace store is infrastructure consumed by those patterns and checks, not a pattern that proposes or governs action. It therefore gets no R-number.

Gates: the generated EDN parses with 19 members; `scripts/edge_census.bb` remains at 61 distinct edges; the organization check passes after refreshing both pinned committed input hashes; its `--negative` mutation is rejected.
