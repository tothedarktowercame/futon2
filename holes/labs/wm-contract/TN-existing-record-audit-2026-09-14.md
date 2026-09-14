# Existing-record audit for WORK-REMAINING rows 13, 15, and 23

Date: 2026-09-14.  Status: read-only discovery.  Repository revision read:
`714822b6d571db87c86bf9c3f113c5be198882c9`.  This audit made no runtime,
configuration, corpus, or source change.  Every EDN file named below was read
as exactly one form.  A path is not treated as identity: joins use the literal
cohort/attempt/event identity and the whole-file SHA-256.  This is required
because 23 current/archive `(cohort, attempt)` names denote conflicting bytes
(`TN-a-pairs-discovery-2026-09-14.md:165-173,188-202`).

## Result

| Row | Verdict from the retained machinery population |
|---|---|
| 13 | `:insufficient-retention` — no 47--53 cohort record, machinery tick receipt, or retained WM trace contains the post-migration `:accumulation-update-input` tick record required to verify the first normal carried step. |
| 15 | `:insufficient-retention` for the remaining work — the population contains selection summaries and exact selected/enacted captures, but no production belief-update envelope and no production depth fields.  It therefore does not supply belief-update, production-depth, or a production positive selector replay witness. |
| 23 | `:usable-exact-joins-retained` — twelve natural construction checkpoints contain the direct `:selection-enaction` comparison; all twelve say `:verdict :match` and retain equal `:selected` and `:enacted` values plus evidence. |

The verdicts are per row, not pooled: row 23's action correspondence does not
stand in for row 15's action-law proof, and a wrapper selection record does not
stand in for a WM belief-update trace.

## Row 13 — live accumulation

### Required witness

Row 13 requires accumulation at the live `judge` anchor, carry in the daily
trace, one configuration record shared by both production callers, and a
three-tick full-judge recurrence witness (`WORK-REMAINING.md:224-240`).  After
the predecessor migration, the specifically outstanding evidence is the next
normal live tick: its retained `:accumulation-update-input` must bind the
preceding trace identity and carried state to the current observation,
pre/post selected belief rows, supports, and model revision
(`WORK-REMAINING.md:240-246`; `TN-row13-live-accumulation-2026-09-12.md:110-141`).

### Records inspected and verdict

The 47--53 roots contain 15 time-step checkpoints, 15 selection checkpoints,
14 construction checkpoints (twelve with the row-23 capture), and ten closes.  Their cohort records and the
seven named machinery receipts
`tick-run-record-wm-machinery-test-2026-09-{13,14}-claude-15-r*.edn` contain no
`:accumulation-update-input`.  A repository-wide exact-key read of retained
EDN found that key only in the migration receipt, not in a produced tick
record.  That receipt is
`holes/labs/wm-contract/runs/row-13-live-accumulation-2026-09-12/migration-receipt.edn`,
SHA-256 `f4527adbba16b9927d782c35b073f15fe0f0133c0a82b3f1ffc2a6e30d57ba3d`;
it documents the predecessor surgery, while the row itself explicitly calls
for the *next* trace write (`WORK-REMAINING.md:234-246`).

Verdict: **`:insufficient-retention`**, missing artifact class
`:post-migration-live-wm-trace-record-with-accumulation-update-input`.  The
already retained redirected three-tick test cannot be relabelled as that live
successor.

## Row 15 — remaining measurement proofs

### Required witness

The row asks for proofs of belief state, belief update, depth, temperature and
action (`WORK-REMAINING.md:415-430`).  State, temperature, machinery-scoped
depth, internal action branches and the live-selector refutation are already
settled; the explicit remainder is belief update, production depth, and the
positive reason-bearing-selector witness, all waiting on production capture
records (`WORK-REMAINING.md:511-527`).

The belief-update witness needs one identical pin carrying pre-row,
per-entity attributed events, resolved A/B bytes or hashes and model revision,
mode, and post-row (`WORK-REMAINING.md:419-428`).  The shared row-13 envelope
names its core fields at
`TN-row13-live-accumulation-2026-09-12.md:126-141`.  Production depth needs the
actual `:horizon-steps` and derived `:policy-depth-used`; nil is meaningful and
selects the single-step path (`WORK-REMAINING.md:436-453`).  A positive live
selector witness needs the complete policy table, temperature and tie inputs,
not merely the selected summary (`TN-row15-selector-spec-2026-09-12.md:66-92,125-153`).

### Candidate evidence and verdict

The selection checkpoints do retain a complete reason-bearing
`:selection-proof-input` (two policies, temperature `0.5`, ascending-policy-id
tie break, selected policy) and an outcome-side
`:f-pi-posterior {:status :absent :reason :flag-off ...}`.  For example,
`data/wm-full-loop-machinery-49/wm-contract-machinery-49-v1/attempt-002/002-selection.edn:1`
has identity `[:wm-contract-machinery-49-v1 "attempt-002" 2]` and SHA-256
`1a03c22e757653937093197c4fd66be7d36c81b6cb3fe551126305b1f26434ac`.
This is usable selector-input material, but it is not joined to a retained
production WM trace or belief update.  Across the audited population there is
no `:accumulation-update-input`, `:policy-depth-used`, or tick-level
`:horizon-steps`.  The only retained `:policy-depth-used` found by the wider
read is the already admitted machinery fixture
`runs/row-15-depth-capture-2026-09-12/capture.edn`, whose machinery-only scope
is stated at `WORK-REMAINING.md:446-453`.

Verdict: **`:insufficient-retention`**, with three missing artifact classes:

1. `:production-belief-update-envelope` joining pre/post rows, attributed
   events, A/B identity and model revision at one tick;
2. `:production-depth-trace-fields` carrying requested horizon and effective
   depth at that same production trace authority; and
3. `:production-selector-proof-input-joined-to-wm-trace` for the positive
   selector witness.  The standalone selection checkpoint is a candidate half
   of (3), not an exact cross-record join.

## Row 23 — R6 to R16 selection/enaction

### Required witness

Row 23 asks the build-phase runs to settle whether the R6-selected action is
the R16-enacted action (`WORK-REMAINING.md:1064-1086`).  The required record is
the construction checkpoint's exact selected/enacted comparison, with either
`:match` or a typed divergence and its evidence
(`TN-row23-verdict-2026-09-12.md:22-41,125-136`).

### Usable exact joins

The following are direct observations, not reconstructed joins.  Each file is
event sequence 3 for its literal cohort and attempt, and each contains
`[:payload :judgment :selection-enaction]` with keys `:verdict`, `:selected`,
`:enacted`, and `:evidence`.  All twelve have `:verdict :match`, exact Clojure
equality of selected/enacted, and `[:evidence :source] :runner-selection`.

| Cohort / attempt | Recorded at | Construction SHA-256 |
|---|---|---|
| 47 / attempt-002 | 2026-09-13T17:52:23.639319510Z | `47529336297b9b59929441bff92448055d6643d35d9550dad523d40d2a01fc0a` |
| 47 / attempt-003 | 2026-09-13T22:16:22.809999127Z | `00ac642df7c8eb0d5457bb56bce1212310a15e8d1c7ebfd4abd681cd8ba4e3dd` |
| 48 / attempt-001 | 2026-09-13T22:36:23.082927446Z | `ce92f601c0b1fdb3abf83889ea785b3ccb38d31e55bbadcbbae6169c1730cd8e` |
| 48 / attempt-002 | 2026-09-13T22:54:10.825911107Z | `5ab8e988b37a46064ef05ba7223a5e7033cc10d683e9a856322f990b868134f9` |
| 49 / attempt-001 | 2026-09-13T23:34:06.714567204Z | `0eb7d17291281dc29064f4ec9f18b53c95a17accc041930e7b05e599c10e1111` |
| 49 / attempt-002 | 2026-09-14T03:31:27.635511055Z | `95f7d9c7da284f3ba13d81dc84fdfb518999388f65a2c5fff819be0090714464` |
| 50 / attempt-002 | 2026-09-14T17:03:04.088443763Z | `c6e8446684afa66108acbdaff3e9059c6bf1b771d1bae62c6e1c46ef36b6f573` |
| 51 / attempt-001 | 2026-09-14T18:18:41.458718236Z | `205981199186576060a52672eb8eb03071da78bf6a92d3944f51a4fd756ce043` |
| 51 / attempt-002 | 2026-09-14T19:02:30.007845686Z | `e439e412c1c4a457f8d962c73e128fb0360538bee885b904fa3054c72a32b1dd` |
| 52 / attempt-001 | 2026-09-14T19:45:06.607450204Z | `94daaf21c4ee4554d60b18cf5481d34c80f26035bd9c790326715c69a108804a` |
| 53 / attempt-001 | 2026-09-14T20:47:03.845293644Z | `78f6b28c32a5f5fc70b7550166a7e122c5d2d1045ca1ee3245680ec2a6955db4` |
| 53 / attempt-002 | 2026-09-14T21:57:22.243965767Z | `85a7ee27f8f5d48c25d3eab81237f393f93e7e12881345128aad59c44439c038` |

Paths are mechanically
`data/wm-full-loop-machinery-N/wm-contract-machinery-N-v1/attempt-M/003-construction.edn:1`.
The literal identity, event sequence, timestamp and digest make each row safe
against the cross-root collision described above.  Later checkpoint copies of
the same construction value and morning-brief projections were not counted as
new observations.

Verdict: **`:usable-exact-joins-retained`** for these twelve per-run matches.
This answers the row at the observed pins; it does not assert that every future
run matches or turn twelve matches into a population-independent theorem.

## Effective configuration readback

The row-24 requirement is explicit: a qualifying configuration must state the
effective horizon policy and all three detail/F-pi controls (previous-tick
policy details, `FUTON_WM_FPI_DARK`, and `FUTON_WM_FPI_POSTERIOR` with complete
coverage) (`WORK-REMAINING.md:1054-1062`).

The retained machinery records do **not** contain that effective configuration
as a single readable object:

- time-step records embed a code-state `:configuration-digest` and
  `:resolved-mode-flags`, but the resolved map omits horizon, policy-detail,
  FPI-dark and FPI-posterior settings.  For example the 53/attempt-001
  time-step is SHA-256
  `5b91d886343927263d44027a2f4fd352ae461f48e189794ce10e6034f81bdad6`
  and carries digest
  `a32972d07cd1889b4dbe6ac65cc0c186e5b1d8d07884fc1ad1dbc91e15fded8a`
  at `data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-001/001-time-step.edn:1`;
- selection records show the *observed outcome* that F-pi posterior was absent
  because `:reason :flag-off`; this supports “posterior did not enter this
  selection,” not a complete configuration record;
- the rich ranked candidate and selection-proof data show that detail was
  retained for those decisions, but no record names the effective detail flag;
- no audited cohort record carries requested/effective horizon or
  `:policy-depth-used`; and no record names the FPI-dark flag.

Configuration verdict: **`:insufficient-retention`**, missing artifact class
`:effective-horizon-detail-fpi-configuration-record`.  The observable
FPI-posterior absence is retained; the complete effective settings needed by
the separate configuration node are not.
