### `agent/handoff-preserves-context`

Verdict: **evidence-found**

Verbatim problem statement:

> “Handoff machinery exists un-assured: bells/parks/packets are conventions in CLAUDE.md prose, not typed objects any checker refuses on.”

Source: `/home/joe/code/futon2/holes/problems/P-assured-process.md:41-43`. The same record says handoff contracts must become typed objects with mechanically checked returns at lines 55-65.

Proposed edge, following the committed syntax at `/home/joe/code/futon3/library/aif/grounded-actuation-not-reobservation.flexiarg:13`:

```text
@why problems/process-conduct-is-unassured (PA11z stakeholder annotation; basis: P-assured-process states that handoff machinery is unassured prose rather than typed, refusing contracts; source: futon2 holes problems P-assured-process.md:41-43,55-65)
```

### `agent/trail-enables-return`

Verdict: **no-committed-source**

No committed problem source states the pattern’s specific problem: without a queryable trail, an agent cannot retrace decisions, distinguish rejected paths, or recover without repetition.

`P-assured-process.md:31-65` discusses lifecycle recording and unassured handoffs, but does not state loss of return/recovery caused by a missing trail. Linking it would stretch a process-assurance problem into the pattern’s navigation-history problem. No edge proposed.

### `cascades/declared-skeleton`

Verdict: **no-committed-source**

No committed problem source states the pattern’s specific problem: stable cascade relations that remain only in momentary judgment cannot be audited, versioned, replayed, or diffed.

I rejected two loose candidates:

- `problems/most-patterns-have-no-globally-recognised-rationale` concerns missing `@why` rationale reachability, not undeclared stable pattern-to-pattern structure.
- `problems/g-over-cascade-is-undefined` concerns undefined cascade valuation and policy semantics, not declaration of stable edges.

No edge proposed.

### Search log

- `rg -n --glob '*.flexi …[trimmed]