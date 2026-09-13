# Split contract review

Reviewed 6aa72c48/6b3eea81: six current and historical source pins match. The existing verifier requires committed ledger evidence; separating ledger-free transition validation is necessary and does not itself authorize writes. No tests rerun for this documentation packet.

Lead corrections: value hashes in current source use UTF-8 pr-str, not canonical EDN. Keep raw-source and value digests distinct and preserve existing conventions. Stable-ID retry must compare the existing retained proposal before requiring the old prior to match the now-advanced HEAD. Prior and next evidence records have different schemas: common store-state projection must be explicitly specified before composition, not assumed by comparing arbitrary record digests.

Accept design narrowly with these corrections. Next implementation is prospective validation only, reusing all canonical/context/outcome/time/replay checks without accepting ledger/universe inputs. Preserve existing retrospective result semantics/refusals and tests. No store adapter, filesystem publication, production authority or runtime integration in that packet. Independent review of these corrections comes first.
