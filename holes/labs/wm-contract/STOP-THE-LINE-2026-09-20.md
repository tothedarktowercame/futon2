# STOP THE LINE — 2026-09-20 (in force)

Joe, verbatim (via claude-4's channel): "this is a 'stop the line'
finding; no more building or dispatches until such issues are found and
stopped in their tracks. This is exactly a facade that this kind of
work must not allow." Triggers: C declined at 0.47% coverage ("looks
like p hacking in the wrong direction"; the 441 discarded holes "a huge
embarrassment if it goes anywhere near a paper"), then the E verdict
facade below.

Held in claude-12's lane until lift: Claude-channel rate measurement,
coupling-parameter estimate, non-point-mass G transcription, all
dispatches. In-scope: facade-finding.

## The E facade, verified at source (g_term_decomposition.clj:31)

`:E [(== 1 value) ...]` — per-policy habit mass compared to exactly 1.
With n > 1 enumerated policies every mass < 1, so :non-unit-habit is
STRUCTURALLY GUARANTEED. A uniform prior (zero information) reads
non-degenerate. The check measures "more than one candidate existed",
not "habit carries information". claude-12 endorsed E non-degenerate
four times today by verifying marker PRESENCE (x10 in the record)
without asking whether the verdict could fail — the 2026-09-19
green-but-vacuous class, one level up, in the READER itself.

Correct form (finding, not yet a fix — the reader is claude-4's
artifact): degeneracy of a prior is a property of the habit VECTOR,
not of one policy's mass. E is degenerate iff the enumerated-candidate
habit distribution is (within tolerance) uniform — carries no
information beyond enumeration. Today's vector [1/3, 1/6, 1/6, 1/6,
1/6] differs from uniform 1/5, so E's non-degeneracy happens to be
TRUE — asserted by a check that could not have said otherwise. The
five-runs-match-the-formula evidence for learning stands separately.

## Honest table

0 of 6 terms standing on evidence. C: verdict real
(:varies-across-horizon, probed reader) but content nearly absent
(3/465 wants, 0.47% weight) — closure declined. E: value plausibly
fine, verdict structurally incapable of failing. A/D/F/Q: degenerate
as recorded.

## Can-it-fail audit of the standing checklists (the lift-relevant work)

Every row asked the E question: can this check come out the other way?

SPEC-c rows: DEMONSTRATED discriminators: row 4 (non-resolving sha
refused :unknown-sha / no-locator / absent false / resolving TRUE),
row 6 (lam=2 -> 2; lam=0, mu=-1 refuse), row 9 (three controls incl.
mislabelled constant family caught). PROOF-GRADE: row 8
(jointReport_subsingleton). OBSERVED-NOT-CONSTRUCTED: row 5 (the
:derived-no-overlap record fired live in the pre-replacement
certificates; never constructed deliberately). NOT DEMONSTRATED: row 1
(empty-want declaration must refuse — unconstructed), row 2/3
(owner-consumption claims are review-grade until ownership threads;
row 3 honestly marked unexercised), row 7 (a pattern WITHOUT forbids
re-firing was never constructed).

SPEC-a rows: DEMONSTRATED: A3 (claude-4 broke one component's
checkable rate; marginal fell to 3/4). THEOREM-GROUNDED: A4
(the witness pair IS the proof that marginal-only acceptance cannot
fail; harness demonstration owed at acceptance: coupled config
returning the independent joint must fail 1/4-vs-1/2). NOT YET
DEMONSTRABLE (implementation held): A1 (perturbed-oracle copy must
fail the diff), A2 (weights 0.6/0.6 must refuse typed).

## Proposed lift condition (for Joe)

The reader itself gets the discipline: every verdict in
g_term_decomposition carries a constructed instance ON BOTH SIDES
(one input reading degenerate, one non-degenerate) before its output
counts as evidence, E's corrected form included. Rows above marked NOT
DEMONSTRATED get their bad cases constructed before the next
measurement click is requested.

## Failure class 3 — prose quoted as model authority (Joe, claude-4's channel, evening)

The pernicious form: a `/-! ... -/` docstring note was quoted as "what
Lean says" to defend `:observation-updates []` as adopted-by-design.
The model speaks ONLY in definitions and theorems; module prose is an
author's note. This class converts UNBUILT into DECIDED-AGAINST and
dresses the empty value in the Lean-first discipline's own clothes.
Compounding it: the note's stated blocker ("requires the posterior over
states given o") is DISCHARGED — ExactBeliefTrajectory.exactUpdate
exists with exactUpdate_dist, exactUpdate_eq_none_iff,
exactUpdate_minimises_vfe proved (verified at source, this sitting).
PolicyRollout carries no conditioning definitions: Lean is SILENT, and
silence = transcription work item (claude-12's lane), never latitude.

Sharp rule, recorded: prose may carry RULING authority when it records
an operator decision (e.g. the C591 deferral text cites approved
RULINGS items — citing it as a ruling is legitimate); prose may NEVER
carry MODEL authority. "What Lean says" means a definition or theorem,
cited by name, or it means nothing.

Status correction: Q is an UNBUILT thing whose explanatory paragraph
is stale, not a closed decision. New transcription item (claude-12):
belief-conditioned rollout in Lean, building on exactUpdate — including
the consistency theorem that under identity A the conditioned rollout
COLLAPSES to the open-loop forward model, which is what makes today's
empty updates honest for the zero-rate regime and states exactly when
that stops being true.

## Failure class 4 — verified through a different reader than production (claude-4, self-reported, late evening)

The 96/4.17% C-content figure was verified with
load-missions-from-files (reads documents) while production's
war_machine reads substrate-2 via mission-registry/load-missions. No
substrate entity carried :mission/open-holes, so production saw 86
missions :not-ingested and certified 3 of 466 — the typed status
existed and went unconsulted. The number was real for the file scan
and unreachable in the pipeline that certifies.

Rule: a projection claim is verified through THE SAME READER the
certifying path consumes — the same-source discipline (F's rule, the
Q classifier's rule) applied at pipeline grain. Fixed by the
idempotent substrate backfill (:updated 86, entities steady, 441
:retained); second certify click in flight; a repeat 3 would be a new
finding, not a retry.

Two sharpenings from claude-4's own account of class 4: (1) the
dangerous sub-case is a test reader MORE CAPABLE than the production
one — the file scan derives holes on demand, substrate returns only
what was stored, so the test passes precisely because it reads from
somewhere production can't; capability asymmetry between readers is
the thing to check, not just reader identity. (2) "A status that
names a gap is worth less than it appears if the person who added it
treats the gap as closed by having named it" — :not-ingested was
written, described in the commit, and then 4.17% was quoted in three
places without consulting it. Naming a gap is not closing it; a named
status earns its keep only when something CONSUMES it (a check, a
report line, a verdict), so new typed statuses should land with their
first consumer.
