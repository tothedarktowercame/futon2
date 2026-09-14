# Close-retention v1

Status: proposed pure carrier contract. This packet does not wire the runner or
cohort writer and produces no production record or categorical label.

## Occurrence

After selection discrimination and before construction, the runner mints one
`:wm/action-transition-occurrence-v1` map with exact keys: schema, run/cohort/
attempt ids, fresh transition and action occurrence ids, the exact selected
action value, SHA-256 of its UTF-8 `pr-str`, and action time. The UUID-backed
occurrence ids are new identities; click, attempt, event-sequence, job,
candidate, and other existing ids cannot substitute. Every checkpoint or trace
that repeats the occurrence must carry this entire map byte-identically. A
changed action value fails digest recomputation.

## Retention block

`:wm/close-retention-v1` has exactly `:schema`, `:occurrence`, `:state`,
`:model`, `:closed-at`, `:evidence-cutoff`, and `:admitted-evidence`.
`closed-at` is the cohort writer's one `:recorded-at`; cutoff is declared equal
to it. Evidence identities are ordered, unique strings admitted by that instant.

The state port is either `{:status :observed, :method
:independent-categorical-observation, :state, :state-at, :observed-at,
:evidence/id}` or `{:status :absent :reason <keyword>}`. Selection belief,
argmax, and disposition methods refuse. Absence is the expected current
production result, not an invented label.

The model port is either `{:status :present :source :declared-machine-model
:model/id ... :model/revision ...}` or exactly `{:status :absent :reason
:declared-model-identity-unthreaded}`. Code SHA, configuration digest,
`:wm-version`, and cohort pins are provenance, not model revision.

The temporal law is `action-at <= state-at <= closed-at = evidence-cutoff` for
an observed state. The observation's evidence freeze is separate and strictly
earlier: `observed-at < evidence-cutoff`. Equality refuses as
`:collapsed-evidence-freezes`; later evidence refuses as
`:evidence-after-cutoff`. Typed state/model absence has no fabricated time.
An observed state's evidence id must occur in `:admitted-evidence`.

## Refusals

The vocabulary is `:shape-invalid`, `:schema-mismatch`, `:identity-invalid`,
`:timestamp-invalid`, `:mint-capability-missing`, `:action-id-coercion`,
`:transition-id-coercion`, `:action-value-missing`,
`:occurrence-action-drift`, `:state-port-invalid`,
`:state-absence-reason-invalid`, `:selection-belief-as-state`,
`:argmax-as-state`, `:disposition-as-state`, `:state-method-invalid`,
`:state-value-invalid`, `:state-observation-after-state`,
`:collapsed-evidence-freezes`, `:evidence-after-cutoff`,
`:model-port-invalid`, `:model-absence-reason-invalid`,
`:model-revision-coercion`, `:cutoff-not-closed-at`,
`:temporal-order-invalid`, `:admitted-evidence-invalid`, and
`:state-evidence-not-admitted`.
