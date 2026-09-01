# C432 — cross-producer attempt authority limit

Date: 2026-09-01.

The quiet-run state machine now emits and persists a fifth
`authority-limit-not-pending-local-repair`: current local evidence cannot prove
that a bounded test job and a serving run belong to one independently assigned
attempt. The only value shared across those producers is a caller-authored
label.

C410's label-equality check remains enforced. It catches an honest mismatch and
therefore establishes consistency among the presented records. It does not
establish independent attempt identity.

The nearest producer-assigned material is systemd's unit identity and
`ExecMainStartTimestampMonotonic`. Both are genuine observations outside the
caller's authorship, but neither is carried into the serving run, so neither
closes the join.

The recorded clearing condition is an independently issued, verify-at-use
attempt capability bound and verified by both the bounded-job producer and the
serving-run producer. Copying another caller-selected label into the run record
would not meet that condition.
