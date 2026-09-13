# Independent review of 56b426c6

The correction is accepted. `verify-feedback` constructs an exact six-field
ledger entry, including `:prior-state/revision` and excluding the store-only
transition subject (`machine_slow_feedback_evidence.clj:390-419`). Its replay
also calls the configured canonical E3 and E2b verifiers, whose transitive
inputs cannot be reproduced from their output digests or the seven transition
records. Therefore durable replay must retain or independently resolve that
input closure. This review grants no production or storage authority.
