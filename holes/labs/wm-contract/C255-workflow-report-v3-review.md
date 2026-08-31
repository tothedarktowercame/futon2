# C255 — third independent workflow-report review

Date: 2026-08-31

Reviewed p4ng `bee763a` and futon2 `aee69ff` using isolated black-box
fixtures. No authoritative source or generated paper artefact was modified.

## Verdict

**The table is still not fit for publication. Withdraw it permanently from
this paper.** The generator is useful as an internal diagnostic after further
repair, but three review rounds have shown that its prose surface repeatedly
outpaces its source semantics. The paper should not wait for a fourth repair
cycle.

## Repairs that hold

- The four supplied negative controls pass for the intended reasons: baseline
  copies generate; an unclassifiable in-window case, an outside-sentinel
  `### ` decision, `wm-typo`, and a leading `(detail)` attribution all reject.
- The control uses scratch queue/decision/output files rather than mutating the
  authoritative shared sources.
- The four lane names and explicit owner marker vocabulary are now bounded.
- Owner repairs in the current snapshot correctly fall from seven to one
  (`C213`).
- `:ledger-frontier` separates ledger coverage from registry holdings.
- Combined and suffixed case identifiers contribute to the case map and lane
  counts.

## Remaining black-box findings

### 1. The withdrawn dispatch claim remains in the caption

The column headers are now `Closed (attributed)` and `Open holding`, but the
generated caption still says “cases dispatched and completed since the
campaign began.” There is no dispatch log. The generator contains no assertion
over the caption or column semantics despite the repair report saying the
labels were fixed “with assertions.” A repository-wide search found no such
assertion.

### 2. An incomplete registry passes as four idle lanes

An isolated valid-version registry containing only the wm-nouns row exited 0.
The report silently synthesized wm-verbs, wm-organization, and wm-evidence as
idle. Registry version validity is checked; exact membership and uniqueness of
the four vertices are not. Absence is again converted to a legitimate value.

### 3. A stale holding is hidden rather than reported

The same fixture made wm-nouns hold C1 while the ledger's latest C1 heading was
closed. Generation exited 0 and emitted all three simultaneously:

```
:holding :C1-already-closed
:open-holding 0
:in-flight :none
```

That is a record saying two things. A holding/closure collision should fail or
be typed as stale; it must not render as no open holding.

### 4. Suffixed identities collapse in the evidence list

A fixture with C4 and C4-dup counts five distinct cases correctly, but emits
`:attributed-cases ["C1" "C2" "C3" "C4" "C4"]`. The report maps identifiers
to numeric case numbers before rendering the evidence list. The suffix is
represented during counting and then lost at the reader-facing evidence
boundary, leaving duplicate C4 and no C4-dup.

### 5. A malformed decision inside the sentinels remains invisible

With one valid `### One decision` and one `###Malformed second decision`
inside the current-decision sentinels, generation exited 0 and reported one
pending decision. The supplied control proves only that an exactly parseable
`### ` heading outside the sentinels rejects. It does not prove that
decision-like malformed content inside the declared population is loud.

### 6. Below-window unclassified forms are retained only as an aggregate gap

Restricting attribution has not literally dropped below-window cases: they
remain in `:cases-in-ledger`. But their identifiers, raw attempted attribution,
and reason are absent from the report. A newly malformed historical
attribution is indistinguishable from an intentionally unattributed historical
case inside an already-large aggregate gap. The boundary is acceptable only
if that population is explicitly declared out of scope; it is not an auditable
claim that the parser lost nothing.

## Independent fixture result

The incomplete-registry / stale-holding / suffixed-ID fixture exited 0 and
reported:

```
cases-in-ledger 5
cases-attributed 5
closed-by-lanes 5
wm-nouns {:closed-attributed 1, :open-holding 0,
          :in-flight :none, :holding :C1-already-closed}
attributed-cases [C1 C2 C3 C4 C4]
```

The malformed-decision variant also exited 0 with `pending-decisions 1` rather
than rejecting the second decision-like line.

## Publication boundary

The interior can render without this table. That is preferable to shipping a
live-looking table whose caption claims nonexistent dispatch evidence, whose
registry absences become idle lanes, and whose stale holding can disappear.
Further work should treat the generator as internal tooling until an
independent fixture suite establishes exact registry membership, holding/
closure consistency, identity-preserving case lists, and a delimited decision
grammar that rejects malformed entries.
