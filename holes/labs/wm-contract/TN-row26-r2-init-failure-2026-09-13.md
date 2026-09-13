# Row 26 discovery: r2 construction/close seam mismatch

Date: 2026-09-13.  Scope: read-only reconstruction of run
`wm-machinery-test-2026-09-13-claude-15-r2`; no serving-JVM access and no
production changes.

## Verdict

The construction function returned normally.  The failure was the subsequent
write of the `:construction` checkpoint: the repair construction's fold output
contained two `:policy-holes` carrying only `:unfolded-pattern`, while the
cohort validator required `:free`, `:why`, and `:obligation-id` on each.  The
append therefore threw `"invalid checkpoint cell"`.  The core catch then
started `close!`, which persisted the pending selection and manufactured typed
`not-reached` cells for dispatch/build/adjudication, but did **not** manufacture
the missing construction cell.  `close-attempt!` consequently threw the
secondary `"required checkpoints missing" {:missing [:construction]}`.  Since
`closing?` was already true, that secondary exception escaped the core and the
outer catch mislabeled it `:initialization-failed`.

Thus `:initialization-failed` names the outer catch's untyped fallback, not the
phase in which r2 failed.  The primary durable finding correctly places the
failure at `:construction`; the terminal run record contains the secondary
close failure.

## 1. Exact seam

The phase ledger records construction start/end with `:outcome :ok` at
`data/wm-full-loop-phases.edn.log:7148-7149`.  That is consistent with the code:
`run-phase!` encloses only `construct-for-decision`; fold classification is
computed after it returns (`src/futon2/aif/full_loop_runner.clj:3257-3269`).
The runner then calls `persist-selection!` and `checkpoint! :construction`
(`:3281-3329`).  Append validation checks the fold and its policy-hole fields
(`src/futon2/aif/full_loop_cohort.clj:51-93`).  Importantly, the runner tests
`wiring-result :invalid` only *after* attempting the checkpoint
(`full_loop_runner.clj:3330-3343`).

The primary finding is
`data/wm-repair-obligations/findings/repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-001-untyped-failure.edn`:
`failure-error` is `invalid checkpoint cell`, stage `:construction`, and its six
errors are the missing `:free`, `:why`, and `:obligation-id` fields on both
policy holes.  Its captured cell also proves the construction existed.

The close path sets `closing?`, persists the selection, and fills missing
members of `required-checkpoints` with sorries (`full_loop_runner.clj:2803-2809`).
The resulting files are selection plus `003-dispatch`, `004-build`, and
`005-adjudication`; construction remains absent.  The cohort close validator
refuses any missing required checkpoint (`full_loop_cohort.clj:442-460`).  The
secondary finding,
`data/wm-repair-obligations/findings/repair-initialization-a9177cab-e783-464e-83e2-21a6371484a4-initialization-failed.edn`, records exactly
`{:missing [:construction]}`.  The core rethrows any exception once `closing?`
is true (`full_loop_runner.clj:3838-3840`), and the outer boundary assigns an
untyped escape `:initialization-failed` and hard-codes stage `:initialization`
(`:3892-3896`, `:3935-3972`).

The absent trace has a separate, deliberate cause.  Trace emission is guarded
by `(not repair-action?)` (`full_loop_runner.clj:3281-3287`).  Therefore the
selection cell's generic `:trace-persistence :after-construction` wording is
false for this stop-line repair; no trace write was attempted.  The late close
persisted selection with nil trace (`:2787-2802`), explaining
`:traceWritten false` in
`holes/labs/wm-contract/tick-run-record-wm-machinery-test-2026-09-13-claude-15-r2.edn:1`.

## 2. Tripwires during r2

Four trip reports have `:trip/recorded-at` inside
17:08:19--17:11:49 UTC:

| time | wire | witness kind | relevance |
|---|---|---|---|
| 17:08:33.032 | `:T10` | `:loaded-file-code-mismatch` | Reports loaded/source drift. It is a real environmental warning, but its witness does not name the checkpoint cell or fold-policy-hole errors. |
| 17:09:09.316 | `:T8` | `:duplicate-finding-livelock` (`[:agent-unavailable nil nil]`) | Open-finding census; not the seam exception. |
| 17:09:45.454 | `:T8` | `:duplicate-finding-livelock` (`[:untyped-failure nil nil]`) | Open-finding census; not the seam exception. |
| 17:10:23.603 | `:T8` | `:duplicate-finding-livelock` (`[:initialization-failed nil nil]`) | Open-finding census; it precedes r2 construction and does not describe its secondary close exception. |

The files are respectively `trip-0a0ca92d-c5d1-4a20-9349-225bbd6eff2d.edn`,
`trip-db5b60ff-d7ae-45f9-8ff7-073d9f89d8e5.edn`,
`trip-b429240b-934b-47b3-98dc-d198047ecf90.edn`, and
`trip-30cd9a1e-7f42-4eff-939e-905db9b84542.edn` under
`data/wm-tripwires/trips/`.  None explains the primary seam failure; the
primary finding's exact validator errors do.

## 3. Selected repair obligation

The authoritative ledger record is
`data/wm-repair-obligations/findings/repair-attempt-001.edn`.  It was opened at
`2026-09-12T17:34:21.100100303Z` for local `attempt-001`, after independent
review job `invoke-1789234385934-20447-8c802427` rejected commit
`98d0dcb115b18934b84a9992bfc48fcbba6b683f`.  The phase ledger binds that
attempt to external execution
`ea1-418bb56e1f0d7ad8986839e45f5268a98e62dcbf9c8ad3634bd998f421a293ae--attempt-001`
(`data/wm-full-loop-phases.edn.log:6868-6870`).  The finding schema does not
retain a `:cohort/id`, and the retained cohort directories do not contain that
historical attempt, so a cohort name is **not reconstructible from the retained
record**; naming one would be an inference, not an exact join.

Its discharge contract requires all four of:

1. `:distinct-repair-commit`;
2. `:independent-review`;
3. `:grounded-repair`; and
4. `:distinct-production-shaped-successor`;

with `:artifact-shape :code-commit`.  The rejected documentation-only commit
was therefore insufficient.  r2's stop-line pre-emption was correct; its
construction serialization was not.

## 4. Smallest r3 condition/change boundary

Before r3, the repair construction must yield a cohort-valid fold: every
policy hole must carry the validator-required `:free`, `:why`, and
`:obligation-id` data.  The runner must also classify/refuse an invalid wiring
result *before* trying to append it, with a typed construction failure.  Its
failure close must persist a typed `:construction` refusal/sorry so close
validation cannot replace the primary error with `:initialization-failed`.

For a valid construction, selection and construction must be durably appended
before dispatch.  Trace policy must be made truthful: either retain the repair
construction in the audit trace without treating it as ordinary habit evidence,
or explicitly record a typed `:repair-action-not-traced` disposition instead of
promising `:after-construction`.  Thereafter dispatch must either proceed or
produce its own typed dispatch refusal.  This is the smallest behavioral
boundary; it does not waive or auto-discharge `repair-attempt-001`.

## Pins

All file/line references above were checked in the working tree on 2026-09-13.
This note does not claim the ignored mutable data corpus is Git-frozen; it names
the exact retained records used for the reconstruction.
