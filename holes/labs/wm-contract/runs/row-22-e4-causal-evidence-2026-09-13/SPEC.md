# Row 22 E4 causal evidence verifier

Scope: pure isolated evidence verification. It neither schedules work nor
asserts that a production edge fired.

Canonical boundaries at implementation time:

* R10's real boundary validates and records commission and dispatch identity in
  `futon3c/src/futon3c/social/coordination_ledger.clj:92-136`, SHA-256
  `faef08569048de0637bd0e671859d4a612283323ee300e777177b87f07d539c8`.
* The existing tick receipt carries `:run/id`, and the same identity is passed
  to the traced judgement at `scripts/futon2/run_tick_once.clj:217-250`,
  SHA-256 `df7106339729b9a872c8b4e2fcd1981c6260504611058151e62be7b299718cc2`.
  That entrypoint is one tick, not authority for pretending a multi-tick
  commission is multiple independent jobs.
* R8 reads previous-tick predictions and the current observation at
  `scripts/futon2/report/war_machine.clj:458-575`. Complete-or-off coverage is
  enforced at `:643-722`; its source SHA-256 is
  `50d02bb7179087597f468f361e16206d93400b1e410e27ce1c261bdde801981f`.

The verifier resolves nine independently configured, exact-byte EDN roles:
commission, dispatch, declared-idempotent run launch, launch history, ordered
tick entries, R2 observations, an independently fixed occurrence universe,
predecessor predictions, and R8 occurrences. Every role is
digest-pinned before parsing. The declared tick plan must equal every retained
tick sequence exactly. Later ticks require complete ordered candidate coverage
and predecessor `t-1`; tick zero requires typed `:off/:initial-tick`. Scheduler
metadata is recorded provenance and never an F-pi operand.

Observation payloads are nonempty ordered typed channel/value rows. Prediction
payloads carry finite channel-keyed means and positive variances plus a typed
action. Each R8 occurrence repeats the exact resolved observation and prediction
input and references the independently pinned source digest, tick, and candidate.
This establishes the evidence join only: `:r8-arithmetic-verified?` is always
false, because the existing R8 computation proof remains separate. Equal action
bytes at multiple occurrence IDs are retained and are not deduplicated.

`:launch/semantics :idempotent` is only the declared launch rule. Fixture
verification additionally requires exactly one matching launch in the resolved
history. No complete independently owned production launch history exists, so
actual uniqueness is unavailable and production authority refuses
unconditionally.

The positive fixture is explicitly `:isolated-fixture`. Production remains
unavailable until independently retained sources bind a successful R10
commission/dispatch to an idempotent launch, the complete ordered tick plan,
tick-entry records, R2 records, predecessor prediction records, and R8
occurrences. No runtime consumer currently calls this verifier. The eventual
consumer seam is after durable tick-plan completion and before any claim that
the decomposed causal route fired; it must supply configured production
resolvers rather than evidence-selected paths.
