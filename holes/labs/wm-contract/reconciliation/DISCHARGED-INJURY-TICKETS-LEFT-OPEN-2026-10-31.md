# Row DISCHARGED-INJURY-TICKETS-LEFT-OPEN — OPEN repair tickets joined to the meta-injury ledger

Generated 2026-10-31 by zai-6 (read-only evidence lane). Sources: `holes/tickets/T-repair-*.md` (Status OPEN), `data/wm-repair-obligations/findings/`, `data/wm-meta-injuries-live-2026-10-22/ledger.edn` (live snapshot while a click is running; nothing was closed or mutated).

**Join method.** The ledger canonically holds one injury per failure-kind cause; OPEN tickets are occurrences whose finding `:failure-kind` names that cause. Attempt-id text matching against pinned tick-run records was tried and rejected: every recent run record enumerates thousands of ticket/attempt ids (the 2026-10-31-4955b6e6 record's Q1 diagnostic alone lists all 609 task names), making any text match a false positive. The table below is therefore keyed on failure-kind, with ambiguity flagged where one kind has more than one ledger injury. Nothing here closes a ticket.

OPEN tickets: **162**. Ledger injuries (live-2026-10-22, by resolved injury id): **20**.

## Summary counts

| Ledger outcome of the kind | Distinct kinds | OPEN tickets |
|---|---|---|
| discharged injury | 4 | 8 |
| medic-stamped injury | 8 | 28 |
| live injury (open/claimed/authored/reviewed/enacted/reopened/abandoned) | 1 | 1 |
| no ledger injury of that kind | 25 | 130 |
| ambiguous (kind has >1 injury) | 1 | 10 |

## Per failure-kind join

| failure-kind | OPEN tickets | ledger injuries (short id, ordinals, state, medic, fixed-by / integration) | verdict |
|---|---|---|---|
| abstained | 58 | — | live work, no ledger entry (repair-obligation lane only) |
| untyped-failure | 14 | — | live work, no ledger entry (repair-obligation lane only) |
| reviewer-falsifier-failed | 10 | f7e97cc6 ord [121 122 123], open, medic-repaired at ord [122] fixed-by 5712b9067d9e71b4e8086ef252e9691df7bf1da0; cfbcbad0 ord [6 11 12], enacted, medic-repaired at ord [39] fixed-by , integration 809a7f7; 8467a7d5 ord [25 30 31], reopened, medic-repaired at ord [44] fixed-by , integration 56fd92d; 04e14314 ord [1 2 3], enacted, medic-repaired at ord [41] fixed-by , integration 6edaf1c | ambiguous — see injury ids |
| invalid-close-outcome | 8 | — | live work, no ledger entry (repair-obligation lane only) |
| agent-unavailable | 8 | — | live work, no ledger entry (repair-obligation lane only) |
| grounded-no-change | 6 | — | live work, no ledger entry (repair-obligation lane only) |
| invalid-author-refusal | 5 | 2aef7e0c ord [47 48 51], discharged, medic-repaired at ord [48] fixed-by , integration 02e9d5e | discharged |
| resume-protocol-violation | 5 | 41a00c62 ord [18 43 117], reopened, medic-repaired at ord [43 118] fixed-by  | medic-stamped (state still open) |
| required-loop-node-unexercised | 4 | — | live work, no ledger entry (repair-obligation lane only) |
| open-cascade-review-answer-invalid | 3 | — | live work, no ledger entry (repair-obligation lane only) |
| close-exception | 3 | — | live work, no ledger entry (repair-obligation lane only) |
| answer-holes-unresolved | 3 | 94091b05 ord [9 21 22], enacted, medic-repaired at ord [40] fixed-by , integration d9fb5ff | medic-stamped (state still open) |
| trigger-ineligible | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| guardrail-refusal | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| hot-load-failed | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| tripwire-tripped | 2 | a59c85a6 ord [59 71], open, medic-repaired at ord [71] fixed-by  | medic-stamped (state still open) |
| no-selection | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| transport-timeout | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| fold-output-invalid | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| cohort-checkpoint-oversized | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| occurrence-square-redecision-failed | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| required-checkpoints-missing | 2 | — | live work, no ledger entry (repair-obligation lane only) |
| selected-target-reading-redecision-failed | 1 | 6d673e5d ord [49 50], open, medic-repaired at ord [50] fixed-by  | medic-stamped (state still open) |
| wm-job-ledger-unavailable | 1 | c5606345 ord [56 61 62], discharged, integration 6f1f854 | discharged |
| breakpoint | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| identity-forged | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| artifact-binding-mismatch | 1 | 9ccf92a2 ord [28 42], open, medic-repaired at ord [42] fixed-by  | medic-stamped (state still open) |
| dispatch-failed | 1 | ad038c48 ord [76 80 81], discharged, integration a1939da | discharged |
| cascade-enactment-open | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| open-cascade-author-answer-invalid | 1 | 0cb09c47 ord [124 125 126], open, medic-repaired at ord [125] fixed-by  | medic-stamped (state still open) |
| duplicate-effective-run | 1 | a67d3e49 ord [133], open | live (open) |
| invalid-checkpoint-cell | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| open-cascade-execution-error | 1 | e43a348c ord [94 110 111], discharged, integration 942ead5 | discharged |
| carry-identity-unavailable | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| source-unavailable | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| review-request-changes | 1 | — | live work, no ledger entry (repair-obligation lane only) |
| resume-error-unknown-class | 1 | — | live work, no ledger entry (repair-obligation lane only) |

## Tickets belonging to discharged or medic-stamped injuries (the row's headline)

- kind **answer-holes-unresolved** -> injury `injury/94091b05988598abb17ce238088862c5d7f47201` medic-stamped: ledger state still `enacted`, medic-repaired at ordinal(s) [40], fixed-by . OPEN tickets of this kind: T-repair-ea1-4c9c589db930a12238afcf87908511fd6e35ebc908f2a571d090d988a0342f5c--attempt-001-answer-holes-unresolved, T-repair-ea1-52835e7be09440de82a95b9022b73b9f849282609a3b8756a58b56adbd65094e--attempt-001-answer-holes-unresolved, T-repair-occ-18d00f51d4b31ac6d1aeb99b90837383f171e725225f7b7c22d93a4c24372689
- kind **artifact-binding-mismatch** -> injury `injury/9ccf92a21f042bca987d0014ebce3b2b9e3dcd1f` medic-stamped: ledger state still `open`, medic-repaired at ordinal(s) [42], fixed-by . OPEN tickets of this kind: T-repair-occ-eb4c2e8d0cd9951c9b028b8faca4865f666badbbbc996869445090261cd0ed84
- kind **dispatch-failed** -> injury `injury/ad038c4804bdd146142db0a9ac74dfe42a863e52` DISCHARGED: ledger ordinals [76 80 81 82 83 84 85 87 88 89 90 92 93 95 107], integration commit(s) a1939dabaa7444a9ace0916af1e897e92abdb52d. OPEN tickets of this kind: T-repair-occ-c15da9b67a118021479198ff97026a98c2cb61508ae21224a32ec8b03c7cf849
- kind **invalid-author-refusal** -> injury `injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246` DISCHARGED: ledger ordinals [47 48 51 52 53 54 55 57 58 60 65 68 70 72 73 74 75 77 78 79 108], integration commit(s) 02e9d5e7e11e92479d586ad078ce960902c36fa1. OPEN tickets of this kind: T-repair-occ-2ff102a09d24d55886bd0e015afd6651115974b0b28afb5b0ed9f7b9d15b7521, T-repair-occ-6782b2b02d58a8857171c102d07738aea3710db72f77cf124d5d9911f2b71c09, T-repair-occ-7ecc0bdeb4fc9d0457f3fdbbd4a7878ab17775fa22e6b8a3c61a19c14b204fcd, T-repair-occ-c86a4898faee019ef5558b6f2ab7a7ec0c374e56c82327d33b9241ee28c33ba8, T-repair-occ-ea02c83c70ebbf0bea21bea410204bf6a148d7bf5c6d17e9bc078260ff884529
- kind **open-cascade-author-answer-invalid** -> injury `injury/0cb09c470db8ba94a5ea649c7ce1c17f24e982ae` medic-stamped: ledger state still `open`, medic-repaired at ordinal(s) [125], fixed-by . OPEN tickets of this kind: T-repair-ea1-e39c50434fe304affa571228e724dea92d7433a474aa24aa51ed42c5195d42d3--attempt-001-open-cascade-author-answer-invalid
- kind **resume-protocol-violation** -> injury `injury/41a00c626ed7f9f9d5454e148745c951ec3db80e` medic-stamped: ledger state still `reopened`, medic-repaired at ordinal(s) [43 118], fixed-by . OPEN tickets of this kind: T-repair-ea1-60b8bfca54a7601a688b2ee75db1db721db026b2ec21fb1d4586308d9c666851--attempt-002-resume-protocol-violation, T-repair-occ-950b5e58640d272150d96d6e2ad8827f89c4cac4dc4097c1cbe9c74d1cd09205, T-repair-occ-d265c507a643c3a1bdca0297451a09698e107a78d9ab0112ff04fbb7512391ce, T-repair-occ-e2c18a46bbc13f546121374c489a44780536dc14b8ac097c0deced7dc4994c49, T-repair-occ-fa1ef846963fd8242c0f511cd18c27ff77e3d0c7193b0f8da4b62edb1a8a24f2
- kind **reviewer-falsifier-failed** -> injury `injury/cfbcbad062bde07d539c7e2abd97a6a189f910f4` medic-stamped: ledger state still `enacted`, medic-repaired at ordinal(s) [39], fixed-by . OPEN tickets of this kind: T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b, T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483, T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179, T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52, T-repair-occ-73bca8beb75dfcf93acbdd9201f9980da80a35128863f84905aef27d173dfb23, T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc, T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f, T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97, T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f, T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887
- kind **reviewer-falsifier-failed** -> injury `injury/8467a7d5b6b4a3203d9053561c1d0d59f5fa1c1d` medic-stamped: ledger state still `reopened`, medic-repaired at ordinal(s) [44], fixed-by . OPEN tickets of this kind: T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b, T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483, T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179, T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52, T-repair-occ-73bca8beb75dfcf93acbdd9201f9980da80a35128863f84905aef27d173dfb23, T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc, T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f, T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97, T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f, T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887
- kind **reviewer-falsifier-failed** -> injury `injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d` medic-stamped: ledger state still `enacted`, medic-repaired at ordinal(s) [41], fixed-by . OPEN tickets of this kind: T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b, T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483, T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179, T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52, T-repair-occ-73bca8beb75dfcf93acbdd9201f9980da80a35128863f84905aef27d173dfb23, T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc, T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f, T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97, T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f, T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887
- kind **reviewer-falsifier-failed** -> injury `injury/f7e97cc6653b45dda8b492c7e9c7a288a1289831` medic-stamped: ledger state still `open`, medic-repaired at ordinal(s) [122], fixed-by 5712b9067d9e71b4e8086ef252e9691df7bf1da0. OPEN tickets of this kind: T-repair-occ-02dc6384e2f7b77f2956203b925e2c185586f522fa4d4a785a5dc814f410890b, T-repair-occ-0552e1353cb6075be1e50824603b0b5898590527f79089df09a212410aaf6483, T-repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179, T-repair-occ-402bbfc68fd00fe028e9a3148474df20115581d5d3bb7aa889064fd01077db52, T-repair-occ-73bca8beb75dfcf93acbdd9201f9980da80a35128863f84905aef27d173dfb23, T-repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc, T-repair-occ-a1f9953c35ee4ed8a16473911f2bafeaa54b2e8609d3da40fe8e32a1a226423f, T-repair-occ-b69768de20c53f0e8a0b5e461aca49031e9bd40c138e52dee5b1b78e75745a97, T-repair-occ-be076a458ed64481117508045fa0dcdb5632b5c9eba71f365e49916d2a94b04f, T-repair-occ-c8da1b1833592a40088c17f4bc64aa1a35fb818d713c3fc6de23d31f78630887
- kind **selected-target-reading-redecision-failed** -> injury `injury/6d673e5de5096788c9784d41e3dc268efd24ed87` medic-stamped: ledger state still `open`, medic-repaired at ordinal(s) [50], fixed-by . OPEN tickets of this kind: T-repair-occ-e62431354b9285c3a249e7331b8582475b1dc426123a35a0432c156ba813844f
- kind **tripwire-tripped** -> injury `injury/a59c85a61dadfd80d53fe7abee61d6e6be68ce88` medic-stamped: ledger state still `open`, medic-repaired at ordinal(s) [71], fixed-by . OPEN tickets of this kind: T-repair-occ-e0b7d987118bcafd40f9a87e9dd37ad88b0d670533ca6e973d0a0e7c565996dc, T-repair-occ-f91e2dc1aa62bdd27449cf14428cee60a83818c2f6d8f989714cec7af21e2e36
- kind **wm-job-ledger-unavailable** -> injury `injury/c5606345ac99815bbb5387aa0954822de8054319` DISCHARGED: ledger ordinals [56 61 62 63 64 66 67 69 109], integration commit(s) 6f1f8543cc4223e782bb8acdab3713eaef7daaef. OPEN tickets of this kind: T-repair-occ-f291316c231c3fe3bc8e43c3874666c43852910e1bb335c0e10514b54f2624a1
- kind **open-cascade-execution-error** -> injury `injury/e43a348c8b9fa28a0e95636d90b01af1a732bc56` DISCHARGED: ledger ordinals [94 110 111 112 113 114], integration commit(s) 942ead5d2b2a5817155c5a2a415e70f42ace780c. OPEN tickets of this kind: T-repair-occ-ac098bf4971f5535b7ed4a6698b261871b632161955a6fd973bb31d1469f703e

## Ledger injuries with no OPEN ticket of their kind

- `injury/1e3f6a623656df0e1a0291a61743e274d3d4742f` kind [:already-satisfied], state open, ordinals (115 116)
- `injury/9c6220388033cbd1fbc888cdf6bb3a5b9a3082c0` kind [:none], state discharged, ordinals (45 46 86 91 96)
- `injury/a46779bc8935e575ff19ea6719aa1be7122f8c40` kind [:capability-route-missing], state open, ordinals (119 120)
- `injury/a9b900a41f704673f9d7f35f545f0482b06135ea` kind [:already-satisfied], state open, ordinals (34 38)
- `injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c` kind [:author-delivery-wrong-repository], state open, ordinals (130 131 132)
