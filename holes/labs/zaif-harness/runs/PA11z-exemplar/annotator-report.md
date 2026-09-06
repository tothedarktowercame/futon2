<!-- RECONSTRUCTED 2026-09-06. The job record stores this report TRIMMED at
     2011 chars, ending in a literal "…[trimmed]" mid-search-log. The complete
     text below is the head of the stored event plus the full tail as delivered
     by the Agency auto-bellback to the caller. The trimmed artifact as actually
     stored is kept beside this file as annotator-report-AS-STORED-trimmed.md,
     because the gap between the two IS a PA11z finding: see lifecycle-census.edn
     :returned. -->

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

- `rg -n --glob '*.flexiarg' --glob '*.md' 'handoff-preserves-context|trail-enables-return|declared-skeleton' /home/joe/code/futon3/library /home/joe/code/futon3/problems /home/joe/code/futon2/holes` — output truncated; also reported that `/home/joe/code/futon3/problems` does not exist. Not used as an exhaustive enumeration.
- `rg -n --glob '*.flexiarg' '^@why|[[:space:]]@why' /home/joe/code/futon3/library` — output truncated. Used only to locate syntax examples, then verified directly.
- `find /home/joe/code/futon3 -maxdepth 2 -type d -printf '%p\n' | sort` — not truncated.
- `find /home/joe/code/futon3/library -type f -name '*.flexiarg' | wc -l` — not truncated; returned `1305`.
- `find /home/joe/code/futon2/holes -type f | wc -l` — not truncated; returned `2102`.
- `git -C /home/joe/code/futon3 grep -n -i -E 'cold start|context collapse|context.*handoff|handoff.*context|successor|resume|return|backtrack|trail|undeclared cascade|cascade.*declar|declared.*cascade|momentary judgment|repeat(ed)? (discover|mistake)' HEAD -- 'library/problems/*.flexiarg'` — combined output was truncated; not relied upon for absence.
- `git -C /home/joe/code/futon2 grep -n -i -E 'cold start|context collapse|context.*handoff|handoff.*context|successor|resume|return|backtrack|trail|undeclared cascade|cascade.*declar|declared.*cascade|momentary judgment|repeat(ed)? (discover|mistake)' HEAD -- 'holes/**'` — truncated, largely because committed JSON records contain very long lines; not relied upon for absence.
- `git -C /home/joe/code/futon3 grep -n -i -E 'cascade|declar' HEAD -- 'library/problems/*.flexiarg'` — its relevant problem-corpus portion was complete, but the combined tool response was truncated by the following futon2 search.
- `git -C /home/joe/code/futon3 grep -n -i -E 'handoff|successor|cold start|context collapse|retrace|backtrack|trail' HEAD -- 'library/problems/*.flexiarg'` — complete; only `process-conduct-is-unassured.flexiarg` matched.
- `git -C /home/joe/code/futon2 grep -n -i -E 'handoff machinery|handoff.*typed|context.*handoff|handoff.*context|cold start|context collapse|retrace|backtrack|undeclared cascade|cascade.*cannot be.*measur|structure.*momentary judgment' HEAD -- 'holes/*.md' 'holes/problems/*.md' 'holes/missions/*.md' 'holes/labs/**/*.md'` — truncated; candidate files were subsequently enumerated without line output.
- `git -C /home/joe/code/futon2 grep -n -i -E 'declared skeleton|declared edge|edges.*declar|relations.*declar|undeclared.*(edge|relation|structure)|structure.*(undeclared|momentary)|cascade.*(audit|diff|review|reproduc)' HEAD -- 'holes/M-apm-demonstration*' 'holes/E-cascade-sampler-four-2026-08-26.md' 'holes/problems/P-validated-R5.md' 'holes/labs/library-loop/**/*.md'` — truncated because report lines are exceptionally long; no exact problem statement found in the visible matches.
- `git -C /home/joe/code/futon3 grep -n -i -E 'undeclared.*(edge|relation|structure)|structure.*(undeclared|momentary)|cascade.*(audit|diff|review|reproduc)|definite graph|stable.*relation' HEAD -- 'library/problems/*.flexiarg'` — complete problem-corpus result; no match.
- `git -C /home/joe/code/futon3 grep -I -i -l -E 'handoff|successor|cold start|context collapse|retrace|backtrack|trail' HEAD -- 'library/problems/*.flexiarg'` — not truncated; only `process-conduct-is-unassured.flexiarg`.
- `git -C /home/joe/code/futon3 grep -I -i -l -E 'undeclared cascade|declared (edge|relation|structure)|cascade.*(audit|diff|review|reproduc)|momentary judgment|definite graph|stable.*relation' HEAD -- 'library/problems/*.flexiarg'` — not truncated; no matches.
- `git -C /home/joe/code/futon2 grep -I -i -l -E 'handoff machinery|handoff.*typed|context.*handoff|handoff.*context|cold start|context collapse|retrace|backtrack' HEAD -- 'holes/*.md' 'holes/problems/*.md' 'holes/missions/*.md' 'holes/labs/**/*.md'` — not truncated; candidate-file enumeration complete.
- `git -C /home/joe/code/futon2 grep -I -i -l -E 'undeclared cascade|declared (edge|relation|structure)|cascade.*(audit|diff|review|reproduc)|momentary judgment|definite graph|stable.*relation' HEAD -- 'holes/*.md' 'holes/problems/*.md' 'holes/missions/*.md' 'holes/labs/**/*.md'` — not truncated; candidate-file enumeration complete.

No files were written, staged, or committed.
