# Ordered provenance manifests accepted narrowly

Reviewed e5d6a92a/88c719d9. Seven current and historical pins match. Retained raw gates show 23 tests/120 assertions, no failures/errors, clean kondo/parens and deliberate failure exit 1. Intermediate metadata and eager-validation failures retained. No passing checks rerun.

All five ordered manifest boundaries now validate vectors with exact expected label order and uniqueness, digest shape and allowed metadata key sets before relational joins. Coherently propagated reversal and duplicate/extra controls refuse; map conversion no longer hides sequence defects. Metadata values such as paths/counts are retained but not independently verified by this order checker, and must not become authority. Overall acceptance remains pure structural provenance over supplied bytes/outputs, not canonical execution, config/HEAD authentication or storage evidence.

Next bounded unit: pure serialized-provenance decoder with externally supplied expected raw digest, strict UTF-8/one-form parsing, complete envelope schema, and revalidation through construct/carrier/closure joins. It must reject changed bytes, mismatched duplicated fields, missing source closure and stale outputs, returning fresh defensive views. No filesystem/store-v2 publication, production constructor or postcommit completeness.
