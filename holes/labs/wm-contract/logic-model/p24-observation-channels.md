# P24 — R2 observation-channel reading packet

**Disposition:** documentation discrepancy, not a runtime repair. This packet is
a source reading at futon2 `ce554eb4a56d7120c4499e2bc4e330428e7a1c1a` (2026-10-19)
and makes no claim of a new serving observation, ingestion, validation, or test
run. It reconciles annotation A9's two R2 entries with the current source and
with the PLOP wording.

## Finding

The second A9/R2 annotation is correct: the paper's examples—test status,
compile status, diff size, failing-spec count, user constraints, time since an
anchor, and contradiction flags—are **not** channels of this observation
vector. The current vector has exactly fourteen entries, not an inferred
number: the literal ordered vector in `futon2` commit
`ce554eb4a56d7120c4499e2bc4e330428e7a1c1a`,
`src/futon2/aif/observation.clj::observation-channels:18-31`, has fourteen
keywords. The A9 anchor commit `e6a0a5f36932d0c8931f52cd3a6d032894a8159b`
is an ancestor of that reading commit; the contract is present at the current
location, while its later absence-provenance implementation came in
`f94df3991a18f3526f1456ded7d66baa08469d7f`.

The first A9/R2 annotation is only partly safe as prose. It correctly says
that the object is a fixed, normalized, typed map when its envelope travels
with it. It should not be paraphrased as though every numeric reader is
envelope-safe: `observation/observe` deliberately retains a backward-compatible
numeric `0.0` for a missing source (`observation.clj::observe:103-146`), while
the separate envelope carries provenance (`::channel-statuses:41-76`,
`observation-envelope:84-101`). The only vectorizer,
`sense->vector:148-158`, rejects an unmatched envelope, but the direct source
consumer census below contains consumers of an observation map rather than
calls to that vectorizer.

## The actual ordered stack contract

The contract is the following order, exactly as declared by
`observation-channels:18-31`; `observe:121-145` supplies the numeric
projections and `channel-statuses:49-65` supplies the required raw paths.

| position | channel | raw requirement(s) / numeric projection |
|---:|---|---|
| 1 | `:loop-health` | `[:loop-health :overall]` |
| 2 | `:support-coverage` | `[:support-attack :support-coverage]` |
| 3 | `:attack-coverage` | `[:support-attack :attack-coverage]` |
| 4 | `:mission-health` | `[:mission-triage :health]` |
| 5 | `:stack-pct` | `[:graph :dynamics :commit-percentages :stack]` |
| 6 | `:consulting-pct` | `[:graph :dynamics :commit-percentages :consulting]` |
| 7 | `:portfolio-pct` | `[:graph :dynamics :commit-percentages :portfolio]` |
| 8 | `:mathematics-pct` | `[:graph :dynamics :commit-percentages :mathematics]` |
| 9 | `:active-repo-ratio` | `[:graph :summary :active-repos]` and `[:graph :summary :total-repos]`; zero if denominator is not positive |
| 10 | `:sorry-count-norm` | `[:graph :summary :total-sorrys]`, divided by 10 and capped at 1 |
| 11 | `:coupling-density` | `[:graph :summary :coupling-edges]` and `[:graph :summary :total-repos]`; zero if the derived maximum is not positive |
| 12 | `:ticks-firing-ratio` | `[:graph :dynamics :ticks]` and `[:graph :summary :ticks-firing]`; zero if there are no ticks |
| 13 | `:depositing-signal` | `[:frames :depositing-signal]` |
| 14 | `:annotation-health` | `[:annotation-graph :health]` |

Thus the vector is a stack/scan projection, not a record of an interactive
coding session. Several channels have a calculation-specific zero condition
(positions 9, 11, and 12); that condition does not erase the source-status
requirement above.

## Absent is not observed zero

`present-value?` treats a path as present only when it exists and is non-nil
(`observation.clj:36-39`). If any requirement for a channel is absent,
`channel-statuses` attaches
`{:variant :absent :reason :source-field-missing :paths [...] :coerced-to 0.0}`
(`:67-76`). If all required paths are present, it attaches
`{:variant :observed}`. `observation-envelope` then persists either the
observed `:value` or that absent record plus its numeric value (`:84-101`).

A retained, historical example demonstrates the distinction without claiming a
current measurement: `holes/labs/wm-contract/runs/2026-09-05-u59-a/normalized.edn:4016-4040`
contains a persisted `:futon2.aif/tagged-observation` envelope. In it,
`:depositing-signal` is `{:variant :observed, :value 0.0}` (line 4023) and
`:ticks-firing-ratio` is likewise observed at zero (line 4032). They are
readings of zero, not absent sources. Conversely, when an input is absent the
numeric map may still hold `0.0`; its meaning is available only from the
metadata/envelope. This retained artifact has all channels marked observed, so
it is an example of observed zero, not evidence about an absent channel or
about today's serving state.

## Direct consumer and evidence-route census

A repository search for the four public observation operations at the pinned
reading found these production source consumers; it found no production caller
of `sense->vector`.

| boundary | current pin | what it preserves / consumes |
|---|---|---|
| trace persistence | `src/futon2/aif/trace.clj::trace-record:437-449` | Derives `observation-envelope` from the exact `:observation` used by the judgement and retains both numeric observation and envelope. Its own comment prohibits re-observing at persistence (`:438-441`). |
| controller diagnostics | `src/futon2/aif/free_energy.clj::channel-source-status:33-54`, `::avoidance-verdict:65-82` | Reads `observation-status`; absent channels become reason-bearing `:absent`/`:unknown`, while an explicit legacy map may still be treated as present. |
| cascade fold | `src/futon2/aif/fold_cascade.clj::realize:51-69` | Calls `observation/observe` over selected-action locators before evaluating pattern conditions. This call site is a numeric-map consumer; it does not itself persist an envelope. |

The session evidence named by the stale R2 paragraph arrives through distinct
records and boundaries, not through `observation-channels`:

* Review execution is derived from review-job `:execution` fields and/or its
  tool-use event ledger by `task_execution_evidence.clj::review-execution-evidence:220-246`,
  then surfaced by `review-execution-gate:248-264`; the R9 checker rejects a
  missing executed review at `r9_checker.clj:106-107`.
* Test-run evidence is a test-registry warrant outcome, acquired at the HTTP
  boundary in `increment_attestation.clj::register-warrant-http:46-89`; it
  reports a refusal, HTTP error, or unreachable registry separately from a
  warranted run. It is not a scalar R2 channel.
* Pattern-graph diffs are written as a separate unapplied JSON artifact by
  `wm/pattern_graph_diff.clj::write-diff!:96-102` (and the wider store census
  names `data/wm-pattern-graph-diffs/` as a store). This is not an input path
  in `observation.clj::channel-statuses`.

This is deliberately a reachability statement about the cited source paths,
not a claim that any of those records was produced or consumed in a particular
live run. User constraints, time-since-anchor, and contradiction flags do not
appear in the declared fourteen paths; this reading makes no substitute claim
for where each might be represented elsewhere.

## Precise paper replacement

**Replacement target:** replace the sentence beginning “Then maintain” in
`/home/joe/code/p4ng/sec-catalog.tex:229` (the R2 paragraph) through the
sentence ending “policy scoring.” This packet does not edit the p4ng worktree.

> **Then** maintain a fixed, ordered fourteen-channel stack-observation map,
> normalized to the interval $[0,1]$: loop health; support and attack coverage;
> mission health; stack, consulting, portfolio, and mathematics commit shares;
> active-repository ratio; normalized open-sorry count; coupling density;
> ticks-firing ratio; depositing signal; and annotation health. Carry a tagged
> envelope beside the numeric projection: an observed numeric zero is a
> measurement, whereas a missing source is marked absent with its missing paths
> even where compatibility projects its numeric value as $0.0$. Session test
> results, review execution, warrants, and pattern-graph diffs are separate
> evidence records, not asserted members of this vector. **Because** a stable
> stack observation contract permits comparison without converting unavailable
> evidence into an observed value.

The existing worknote's “fourteen zeros” should be retained only as an
empty-input shape witness, if its witness is independently current; it must not
be read as fourteen observed zero measurements or as validation of an operating
serving path.

## Proposed correlation-model update

Replace the correlation entry for **A9/R2** with:

| annotation | status | paper claim | implementation anchor | evidence / limit | disposition |
|---|---|---|---|---|---|
| A9/R2, typed normalized map | exists, not newly validated | fixed ordered stack observation with a provenance envelope | `futon2@ce554eb4a`, `observation.clj::observation-channels:11-32`, `::observe:103-146`, `::observation-envelope:84-101`, `::sense->vector:148-158` | retained historical envelope at `runs/2026-09-05-u59-a/normalized.edn:4016-4040`; no current serving observation claimed | document the actual fourteen channels and absent-vs-observed-zero distinction |
| A9/R2, session-feature examples | refuted | test/compile/diff/session facts are vector channels | same fourteen-channel literal and `::channel-statuses:49-65` | review execution, test warrants, and pattern-graph diff have separate cited boundaries above; no claim about untraced examples | replace stale paper examples; do not add ingestion |

This mapping retains the annotation's unreviewed status and changes only the
documentation conclusion: it is not evidence that R2 or the entire loop has
been validated end to end.
