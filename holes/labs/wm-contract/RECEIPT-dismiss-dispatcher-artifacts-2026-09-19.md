# Receipt: dismiss seven never-executed dispatcher artifacts

Authority: `Joe -> claude-12 repair-queue ownership 2026-09-19;
CLASSIFY-repair-queue-2026-09-19.md @ 3db4e8e3`.

Mechanism: `futon2.aif.repair-obligation/dismiss-unexecuted!` from
`333ec13e6d49e7ff86afc5ae173295e4aa347bc4`, warranted as
`test-registry-108e2264e772dd152ec6ea14e0bac0e02ff63584f6d4357d484c338fcf0a3f65`.

No click occurred. No finding file was changed. Exactly seven new append-only
records were written under `data/wm-repair-obligations/dismissals/`.

## Counts

- Before: **20** runner-eligible records, read via `open-obligations` and the
  exact runner predicate (`:open` and not `:environmental-hold`). This is the
  immediately preceding classified snapshot; the mutation batch also computed
  this value before its first write.
- After: **13**, independently read after all files existed through the same
  reader and predicate.

The batch process completed all writes but exceeded the command tool's
30-second output window while performing the final store reread, so it emitted
no captured summary. It was not replayed. A separate read-only validation
returned all seven records and `{:after 13}`. There were no typed refusals.

## Appended records

| Path | SHA-256 |
|---|---|
| `data/wm-repair-obligations/dismissals/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-003-build-failed.edn` | `0149eff25f72e1a1afc0473f7e7720e7122b35a73b02afb2adc5a89de91f645a` |
| `data/wm-repair-obligations/dismissals/repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-001-build-failed.edn` | `a28838bd2a2b45e4216ee4987ef21564d3e6ae350f60b696b91e271e27c1e093` |
| `data/wm-repair-obligations/dismissals/repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-002-build-failed.edn` | `68e9f15a1f37fc5ad1c9e531784ea6d5ae3fdac86d3c956150bd815ab3b83416` |
| `data/wm-repair-obligations/dismissals/repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-001-build-failed.edn` | `ff65015c8bbae54ae1c33bf15017d9424d17a1e1e04b111543aa1c3c04e32ba2` |
| `data/wm-repair-obligations/dismissals/repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-001-build-failed.edn` | `847fae15b7066d1037304e8d2a786d220b804ea9e25c8f4b06b0ebc6017668b8` |
| `data/wm-repair-obligations/dismissals/repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-002-build-failed.edn` | `3b3b93175bfc43bf1e03b8ec1d5025a549c7366b07c51862cd72fe99dfd5ad4c` |
| `data/wm-repair-obligations/dismissals/repair-ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-001-build-failed.edn` | `b4d00a589161c5a35bce801f5da7ba5f30097f458ddfa5af3c9eb8fddf5d11d1` |

Each record retains `:execution {:executed false :tool-events 0
:command-events 0}`, actor `codex-24`, and reason
`:never-executed-transport-failure`. Only the `b82bec43` record carries
`:cause-fix "futon3c 7829ea83 (input_too_large)"`; this is a cause citation,
not certification that a running adapter has deployed that commit.
