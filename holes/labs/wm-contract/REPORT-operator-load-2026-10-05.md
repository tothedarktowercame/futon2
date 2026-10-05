# Operator-turn load for the persisted 107-item ranking — 2026-10-05

Reproduce in a fresh process from `/home/joe/code/futon2`:

```sh
clojure -M scripts/operator_load.clj
```

futon2 HEAD before generation: `d8c85edf8129279b1b82ff95cc4f29480f496f16`. Run-record pin: `data/wm-runs/tick-run-record-2026-10-05-c9d25d6a-f2bb-42bf-a162-2c4a000e804f.edn` / `9de9e4bbb286680ebc74daa642a1fd8987009238928bddef60b387954d4d10c2`. B1 pattern-edge graph pin: `http://127.0.0.1:7070/api/alpha/cascade-real/graph` / `4812de576e88dae38d942a2b4e51b3bf01d80c823a76791b3cbd9fad5dc8ccc2`; it contains 1715 pattern edges, 1715 applied edges, over 249 canonical missions.

## Rule

```edn
{:history-window {:at-or-after "2026-09-30T02:32:06Z", :introduced-by {:repo "futon3c", :commit "3981fb38"}}, :machine-trailer-keys ["Agent-Id" "Agent-Session" "Agency-Job"], :markers ["HIT" "Joe decides" "Joe to decide" "operator ruling" "needs Joe" "🈸" "ask Joe"]}
```

For each source document, the script reads commits at or after the signing-hook boundary with `git log --since=<at-or-after> --format=... -- <path>`. Commits before that boundary are `:unattributable`, because trailers did not yet exist and the shared author name does not distinguish Joe from agents. Within the window, a commit carrying any configured trailer is machine-authored and one carrying none is an operator touch. Touch share is `operator-touches / N-found`; a document with no in-window commit has `:typed-absence` (`:no-attributable-history`). Text-marker count still examines the complete HEAD document independently.

**Headline:** 4 of the 107 ranked items have any commit in the attributable window; 103 do not.

The proxy-metric hand reference resolves to commits `7a9113d`, `e9b4101`, `ae45d54`, all `Agent-Id: wm-author`, hence 0/3 and share 0.0. Commit `0e35337` predates the boundary and is `:unattributable`. `M-categorical-code` has no in-window commit.

## Distribution by kind

| kind | items | measured | typed absence | mean N found | mean operator touches | mean touch share | items with markers | marker lines |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `:excursion` | 3 | 1 | 2 | 5.000000 | 0.000000 | 0.000000 | 0 | 0 |
| `:mission` | 104 | 3 | 101 | 4.000000 | 0.000000 | 0.000000 | 14 | 61 |

## Share and author distributions

| operator-load share | items |
|---:|---:|
| 0.000000 | 4 |

| `Agent-Id` set seen in an item's in-window commits | items |
|---|---:|
| `#{"codex-10"}` | 1 |
| `#{"codex-11" "codex-12" "codex-13" "wm-author"}` | 1 |
| `#{"codex-11"}` | 1 |
| `#{"wm-author"}` | 1 |

`Agent-Id` sets exclude the in-window commits with no machine trailer; those commits are the operator-touch numerator.

## Correlations

Spearman rho uses average ranks for ties and only the 4 rows with measured touch shares. Occurrence counts are the persisted selector observations; B1 (b) is recomputed from the pinned [G] applied-pattern relation as the number of other missions sharing at least one pattern.

| comparison with touch share | rho | rows |
|---|---:|---:|
| persisted occurrence count | `:undefined-zero-variance` | 4 |
| B1 (b), shared-pattern missions | `:undefined-zero-variance` | 4 |

## Proposed token

`{:name :meta/operator-load-share :type :ratio :range [0 1] :raw [:operator-touches :n-found]}` is emitted by the proposed operator-load reading square. As a support change, it could be consumed beside `injury` to admit or exclude a candidate before scoring. As a preference term, it could be consumed by `minimise-g-over-filled-meta-policies` as predicted operator demand. B3 leaves that choice to Joe. At HEAD it is computable for only 4 items; the share becomes available only after documents receive work under the signing hook.

## Per-item values

| id | kind | source document / SHA-256 | in-window N | operator touches | share | last operator touch | `Agent-Id` set | `:last-touch` state | marker lines | matched markers | B1 (b) |
|---|---|---|---:|---:|---:|---|---|---|---:|---|---:|
| `M-interim-director-proxy-metric-inventory` | `:mission` | `/home/joe/code/futon7/holes/M-interim-director-proxy-metric-inventory.md` / `f9fcb8c2b6dd2700687d7732de395bbc6a495d5c60f4781cd27e98f2f8601d98` | 3 | 0 | 0.000000 | — | `#{"wm-author"}` | `:war-machine-authored` | 1 | #{"needs Joe"} | 68 |
| `M-categorical-code` | `:mission` | `/home/joe/code/futon5/holes/missions/M-categorical-code.md` / `7c4b9ef5dd70db2b4f1048c359e8cf8dc760e6c2a1b7c227785f5d957c83d379` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 53 |
| `M-or-training-as-learning-system` | `:mission` | `/home/joe/code/futon4/holes/missions/M-or-training-as-learning-system.md` / `6cff141985b827d81c2201b800a0d2434eb80aa9536c9b2f0675f9ca82d8855c` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 32 |
| `M-simulating-or-training-as-learning-system` | `:mission` | `/home/joe/code/futon4/holes/missions/M-simulating-or-training-as-learning-system.md` / `8920f74b5b719a0bbe8c1744f73a8721594274aef8f69225e60178ebbdff5a61` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 37 |
| `M-the-perfect-crime` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-the-perfect-crime.md` / `31b5c44004de83cc5aa843c6a82536045c91437ba941d5946ae6219a29297bc1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 2 |
| `M-futonzero-mvp` | `:mission` | `/home/joe/code/futon0/holes/missions/M-futonzero-mvp.md` / `ae00504218a9806c5567a86d1de11688944ab8245cc88c3943f8d653cecae356` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 32 |
| `M-vsatarcs-writer` | `:mission` | `/home/joe/code/futon4/holes/missions/M-vsatarcs-writer.md` / `672364172c14221a25e6ecadb2ba7fba2d5ffe0beebe75bf6828e743a44b74e4` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 33 |
| `M-vsatarcs-invariants-integration` | `:mission` | `/home/joe/code/futon4/holes/missions/M-vsatarcs-invariants-integration.md` / `3a69ed0949e622828ad8d51cae7d92483257c4b408897df12d3f2132f0fa0840` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 58 |
| `M-weird-modernism` | `:mission` | `/home/joe/code/futon3/holes/missions/M-weird-modernism.md` / `d216ac4aa75d1d79d2253c7308ad7fd6fbcf3b1b34eb3a007a90842dbd10e15f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 35 |
| `M-futon-enrichment` | `:mission` | `/home/joe/code/futon4/holes/missions/M-futon-enrichment.md` / `6b51632ae387f6a543e8071095d6184180c2e37a8b47e1b8bb658ccf463a4573` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 48 |
| `M-stack-stereolithography` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-stack-stereolithography.md` / `9909b110447ae4bf044400bbd357ee9b837ddad19fd5196160a4cd5f2cdc98a7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 18 |
| `M-essays-edit-cycle` | `:mission` | `/home/joe/code/futon4/holes/missions/M-essays-edit-cycle.md` / `ce5dd9bd97b5af2d333e7f22e4b81b11d171e3e76ebb7eef99bc34e0f4b12015` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-trip-journal` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-trip-journal.md` / `027ffc5b7c049a2511c752a8368be68fdaa9651284e1fda127e3ca310b8455fd` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 26 |
| `M-pattern-application-diagnostic` | `:mission` | `/home/joe/code/futon3/holes/missions/M-pattern-application-diagnostic.md` / `610f24efc3d14122ec3710ceec807e178906bd9306dffd44cd09cc8e44064727` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 35 |
| `M-patterns-done-right` | `:mission` | `/home/joe/code/futon0/holes/missions/M-patterns-done-right.md` / `ad8457d4eaca87bcd4fd68ad2d23ae6c50294274a06672d882b6ebea8578e989` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 12 |
| `M-joe-told-me-about-futon` | `:mission` | `/home/joe/code/futon0/holes/missions/M-joe-told-me-about-futon.md` / `b29879eb2d193fe5ab66f6fac75ca08bece4b7ef14563626e66588f8ec3face6` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"ask Joe"} | 20 |
| `M-a-sorry-enterprise` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-a-sorry-enterprise.md` / `042300374d12daab0fb17de9124dedde37a0fa08eb4f527719472abebbc893c9` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 27 |
| `M-expressions-of-interest` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-expressions-of-interest.md` / `f8b4e9868e7ad777649ce94c1070ac667f660a23fcfd31c9a3d0ead9d9439530` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 15 |
| `M-web-arxana-ui-improvements` | `:mission` | `/home/joe/code/futon4/holes/missions/M-web-arxana-ui-improvements.md` / `624350faea5ab9bc7e1b91b3bb671f0eb1d41826c2d7d3c69da953c200cb7143` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 16 |
| `M-goals-and-holes` | `:mission` | `/home/joe/code/futon2/holes/M-goals-and-holes.md` / `df8b8a38be3509fabad74f2052d9dd24c37a00c3ffc72922700190bbf4fc5776` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 38 |
| `M-the-futon-stack` | `:mission` | `/home/joe/code/futon0/holes/missions/M-the-futon-stack.md` / `5dba51d5cdde4400e8ce8c7035f1bade0478c2dea3d92d1f6307ecd90ff5351c` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 48 |
| `M-custom-harness` | `:mission` | `/home/joe/code/futon2/holes/M-custom-harness.md` / `5e53754c6ce143723081c948af77c55bcfca5c6d8ff1d78311f217d721ae10f9` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 41 |
| `M-stack-geometry` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-stack-geometry.md` / `7238b91508c4f47c12b286bafdf114d2af9fb4458fbb54999adc9b827ffb91c4` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 7 |
| `M-pattern-posteriors` | `:mission` | `/home/joe/code/futon3a/holes/missions/M-pattern-posteriors.md` / `1a541e859c7abf6631edc39649b3a3067d33eea9715d74324e6c75a109f0d6bd` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 24 |
| `M-value-creation-loop` | `:mission` | `/home/joe/code/futon7/holes/missions/M-value-creation-loop.md` / `a4e5d1f8d1522fee095a0b287193ac6077138b7cb905bd5d74ad03330433d39c` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 5 |
| `M-recommendation-bindings` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-recommendation-bindings.md` / `9662fd74deda6a7fb2b8dceb290be54d41eda3f5af77ee145893b19a7393b142` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"ask Joe"} | 24 |
| `M-pattern-retrieval-calibration` | `:mission` | `/home/joe/code/futon3/holes/missions/M-pattern-retrieval-calibration.md` / `5c141a9dfa1517c5e5e2ab9835084d876da9fb27acb04df4959ea2a5e9f0eba7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 44 | #{"HIT"} | 10 |
| `M-aif-a-matrix-faithfulness` | `:mission` | `/home/joe/code/futon2/holes/missions/M-aif-a-matrix-faithfulness.md` / `4b01334393ff3f0fe5cdc06e9e4795651f73b5896f8514d060016aba20062aa1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 32 |
| `M-pattern-mining` | `:mission` | `/home/joe/code/futon3/holes/missions/M-pattern-mining.md` / `b3cce85230495dd0ebceb9c0c66cbdff604a0b2e2b90629736a292bc2c7f7b5d` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-digital-nomad-patterns` | `:mission` | `/home/joe/code/futon2/holes/M-digital-nomad-patterns.md` / `56f8c48f3a6a0ca4422084afeb7071a21ff3922858cc4e69e416c662301e6675` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-formal-war-machine` | `:mission` | `/home/joe/code/futon2/holes/missions/M-formal-war-machine.md` / `c7d6bc76e60964e5f7c706794e310dbfbe8bb359106e8a03352695e61bd87b96` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 36 |
| `M-self-improvement-loop` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-self-improvement-loop.md` / `790e5a7d1ed9c39190b468d5518f1fe1a08c2afdf7d1ef7446db112b04af9af6` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 4 |
| `M-war-machine-wiring` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-war-machine-wiring.md` / `f75873416ac1f328698a2d2292aa5515366bf722ebc232e2098d61ea847a4cdf` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 11 |
| `M-mission-coherence-patterns` | `:mission` | `/home/joe/code/futon3/holes/missions/M-mission-coherence-patterns.md` / `e3fb2a57c0d802fe3dd1fee12fb51184b7efecb01beebcaf078d8a453c38bf25` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 9 |
| `M-hypergraph-operator` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-hypergraph-operator.md` / `f12c6ee31171a3b87744177eedb3edb7b77e8c55200ad81568a24ffed10af29f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"operator ruling"} | 2 |
| `M-essay-corpus-substrate` | `:mission` | `/home/joe/code/futon4/holes/missions/M-essay-corpus-substrate.md` / `42e4db8493a2eea15da7945bfd0b8ed02117b6274d070d4e067471fe769e3e68` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 11 |
| `M-futonzero-generative` | `:mission` | `/home/joe/code/futon0/holes/missions/M-futonzero-generative.md` / `45f574fd58ffcb43010cbab85aa7c2c41935fb631ca3353906f256de368da575` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-zaif-harness-v1` | `:mission` | `/home/joe/code/futon2/holes/missions/M-zaif-harness-v1.md` / `1bf9db2559e9c565bd2664c293d30bdb20ba960005421dae0c749af8cb5c7697` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 1 |
| `E-kimi-task-28` | `:excursion` | `/home/joe/code/futon2/holes/excursions/E-kimi-task-28.md` / `ba34a5b1b5eda3429c89b55996af8848037acaa558f641030976ca2eb0d26042` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-codex-sorry-loop` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-codex-sorry-loop.md` / `54683435741dbbd3cb2bf3a89c8fa723832ef26878d1eff16875da9ade0c7a3d` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-daily-scan` | `:mission` | `/home/joe/code/futon7/holes/missions/M-daily-scan.md` / `c9b3559b0b049e17d57a13c0dce7c374c25948eb660d43b254e2d77f87c63084` | 1 | 0 | 0.000000 | — | `#{"codex-11"}` | `:agent-authored` | 1 | #{"Joe decides"} | 5 |
| `M-interim-director` | `:mission` | `/home/joe/code/futon7/holes/M-interim-director.md` / `aad85c4b70e1e68ec659f21c0ec07a4d886a4520bad48555d30aca84a3511dd3` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 20 |
| `M-shared-memory-control-build-test` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-shared-memory-control-build-test.md` / `6043c528cb97a358527881485bc1ea3e31d88bde00a00413110efdd7a894bbb9` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 20 |
| `M-war-machine-pilot` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-war-machine-pilot.md` / `cc95027a7a4c3d7534d774fbbf4d49cf3d2489b939c8f1a6f0701f3fbc155c9e` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 41 |
| `M-warrant-limit` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-warrant-limit.md` / `4e262d494506941ce035a08e5b5eb108188cdfe5bb368268c79e8014a1e003dc` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-zai-learning-loop` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-zai-learning-loop.md` / `427d3a1e641abce7bc4de59b6fd3dd343c808412ebb08c53a155cb980e849bca` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 4 | #{"HIT"} | 0 |
| `M-differentiable-code` | `:mission` | `/home/joe/code/futon5/holes/missions/M-differentiable-code.md` / `8d9a1e5393964527bcc893df7ed4bb9c02aed4de2aab8b6cf2be5f881bff57dd` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 1 |
| `M-inbox-zero-claim-lifecycle` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-inbox-zero-claim-lifecycle.md` / `99f31a0e64537ea7c6c9b0e72632d9aca827604490862c2c0059159f966cd79f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 1 |
| `M-aif-faithfulness` | `:mission` | `/home/joe/code/futon2/holes/M-aif-faithfulness.md` / `588083fd95a23b51f03d7e5cb289276869ca61cc7404be46ac0632010c4ab61b` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"Joe decides"} | 0 |
| `M-operational-vocabulary` | `:mission` | `/home/joe/code/futon2/holes/M-operational-vocabulary.md` / `b4eeb0f78c50f48fb4b041c4124ef8ed153a021aea0e7c9e966e8333cbe09eb3` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 1 |
| `M-autoclock-in` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-autoclock-in.md` / `bf5ec2aeb3e2c94f01aec190811172e6d910ac24503cbb2553aea56c6792aa86` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-autonomous-doc-maintenance` | `:mission` | `/home/joe/code/futon7/holes/M-autonomous-doc-maintenance.md` / `b8f427ec37721885137aba93160dd2f01107cad36774ff88d265456700034603` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-essays-diachronic-model` | `:mission` | `/home/joe/code/futon4/holes/missions/M-essays-diachronic-model.md` / `be274a0532af060091e7d4c3f3f09f8c795a25d25c7589d488a13f618fda2f0f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-pudding-peradams` | `:mission` | `/home/joe/code/futon7/holes/M-pudding-peradams.md` / `b03727bbdc0d8521a907318d5e8a3deaba9b2d02863c18030328b19fa3e1f229` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-war-machine-frontend-upgrade1` | `:mission` | `/home/joe/code/futon7/holes/M-war-machine-frontend-upgrade1.md` / `fc964dcb4835327ba11771834f64335d7c57bd9e0fa63820192533502553a891` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 35 |
| `M-landing-practice` | `:mission` | `/home/joe/code/futon5a/holes/missions/M-landing-practice.md` / `1a25494fc25b52650a1db3538f2a66547de22d15edc06c48e1b9329d9dc7dd97` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-mission-scopes-into-substrate-2` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-mission-scopes-into-substrate-2.md` / `434e35ec147a92669cf7fb130775aeb37cca4ddb6b6afc009908d835619f89d2` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 54 |
| `M-action-cost-modelling` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-action-cost-modelling.md` / `580eb59a2cbb4f16b6eb51fb4e0e0b2d564ab852ad716ca61196b246e4e7cbd9` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"HIT"} | 64 |
| `M-tpg-coupling-evolution` | `:mission` | `/home/joe/code/futon5/holes/missions/M-tpg-coupling-evolution.md` / `a348398dccdf0647cd926854318dff04dd7016e453c83f912326c74165ae9bd8` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-xor-coupling-probe` | `:mission` | `/home/joe/code/futon5/holes/missions/M-xor-coupling-probe.md` / `dacc31d7503d1ae74b808751c0492c6109d5611778e99bb6fd2e089614dda5a1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-webarxana` | `:mission` | `/home/joe/code/futon4/holes/missions/M-webarxana.md` / `819382c89dba41d66b29b106ac34474d994d1f81db84449416fc03b64a5e5877` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-war-machine-vsatarcs-interop` | `:mission` | `/home/joe/code/futon4/holes/missions/M-war-machine-vsatarcs-interop.md` / `1fa4a53881baed154079dfbde96afca7031d6817ffbb2f6332d1aaed410cbf5f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-web-arxana-missions` | `:mission` | `/home/joe/code/futon4/holes/missions/M-web-arxana-missions.md` / `d385b633d1a4e9f7e1a808a282712b4c273ed3821bcf5e9aaacdaa2d5a72e7ce` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-fulab-wiring-survey` | `:mission` | `/home/joe/code/futon5/holes/missions/M-fulab-wiring-survey.md` / `229599d7d0836299730d14567c793d352cb6a62ce5f86dc9a59fe72ac6d14cc7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-coupling-as-constraint` | `:mission` | `/home/joe/code/futon5/holes/missions/M-coupling-as-constraint.md` / `283384cfca8d5b45b64099fccdedea3a98be01153a20136f980522bef01c9e09` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-essays-retraction-visibility` | `:mission` | `/home/joe/code/futon4/holes/missions/M-essays-retraction-visibility.md` / `47a18be9709bfa3eb0b26284d9e53d96b2522c23783ff1b2de1e66f163826a00` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-arxana-roundtrip` | `:mission` | `/home/joe/code/futon4/holes/missions/M-arxana-roundtrip.md` / `b541c107be0420ecdcf3c33e6907ffa684b1cf5f6ce4d34528bfb694eba8456b` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-editorial-assistant` | `:mission` | `/home/joe/code/futon4/holes/missions/M-editorial-assistant.md` / `f24b5154a4888926376eb9110451ca749a7c776b30ca42db2858b663500f2b17` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-reachable-from-boot` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-reachable-from-boot.md` / `3953f0b9acac73f8c710b78980e2c09af5dcfd552e9dbfedff5819860d7dbed1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 40 |
| `E-cascade-real` | `:excursion` | `/home/joe/code/futon2/holes/E-cascade-real.md` / `ce2813a930efce4fee55baf0a7b4735455bc07a40eb5e67fbbf92e44edceacb8` | 5 | 0 | 0.000000 | — | `#{"codex-10"}` | `:agent-authored` | 0 | — | 0 |
| `E-kimi-task-70` | `:excursion` | `/home/joe/code/futon2/holes/excursions/E-kimi-task-70.md` / `7d3a33d0bbb0a70338505d9d0e17508f4936160529559b991c68d2d645e2b6bc` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-a-wmc-scaling` | `:mission` | `/home/joe/code/futon2/holes/M-a-wmc-scaling.md` / `312c8b5bc6441b0bcba2935e87619d2eb002d462655f856314c3b05af7e9a2b2` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-aif-ants-port` | `:mission` | `/home/joe/code/futon2/holes/M-aif-ants-port.md` / `166eff1033db42587d3dd3406e1548b2e498c53cbc8d5ba72bbb0263b69e9113` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-aif-policy-conditioned-eig` | `:mission` | `/home/joe/code/futon2/holes/missions/M-aif-policy-conditioned-eig.md` / `c154be5833c81f5770d6319ba03b20829c98fedd6fd40768c38814bba36ec2f9` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-another-university` | `:mission` | `/home/joe/code/futon7/holes/M-another-university.md` / `2c7accee5c2d143feaf937ededf6a84ed3f363fa74e0d3a70ccbcbbd275f9c3d` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-buyer-discovery` | `:mission` | `/home/joe/code/futon7/holes/M-buyer-discovery.md` / `be1baf97945f7611598b7588dab55b45aec9f4d07a39bd3e19fe38af03b7a628` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"HIT"} | 0 |
| `M-daily-scan-multi-axis-queue` | `:mission` | `/home/joe/code/futon7/holes/M-daily-scan-multi-axis-queue.md` / `1b3fc471da8ddb2634148f0d5556ac2825ac01a01bd7f1030d27b38fa97385b7` | 8 | 0 | 0.000000 | — | `#{"codex-11" "codex-12" "codex-13" "wm-author"}` | `:war-machine-authored` | 1 | #{"Joe decides"} | 0 |
| `M-interim-director-long-tail` | `:mission` | `/home/joe/code/futon7/holes/M-interim-director-long-tail.md` / `af3d323c6fdc026955b57aba1f14f725be7050f43c88bbffbeedde61a945885d` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-signal-roll-up` | `:mission` | `/home/joe/code/futon7/holes/M-signal-roll-up.md` / `5bd024038ed1c67b2f34402b4b4196e3765646de588202aae8b0f1f245dc4707` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-turns-first` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-turns-first.md` / `1ef532c58005df2829b8868b45ee5da3befcdd682b421c930e1b184b5d8866b7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-war-machine-aif-last-mile` | `:mission` | `/home/joe/code/futon7/holes/M-war-machine-aif-last-mile.md` / `bc0116b8649c324b2937e182ce190e6fdb9984d1f4d253e71a62e97a5a72f1ab` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 2 | #{"HIT"} | 0 |
| `M-state-snapshot-witness` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-state-snapshot-witness.md` / `f1a5b9d081cd185a996527b6e8aed2930f7c60fd7e7701a22edcc571c482a460` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-xenotype-its` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-xenotype-its.md` / `65d2bc5d20345cee856a1977ea3b579b578d46f4884612861fef186be79ff601` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 3 |
| `M-single-entry-point` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-single-entry-point.md` / `60fbf4372f1732b99d604ed4ee71478b8e330f53268d164fa1b8ef7b592c46d1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 3 |
| `M-typed-memories` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-typed-memories.md` / `fa9566fbf6c1adc5299530ef3d0c9caf50f4831cb154ed11b059a06229d8044a` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 7 |
| `M-bounded-disposition` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-bounded-disposition.md` / `ae8554e3ca304d270c7c8d56af7aff6ffcf8a4b80a0124752cdfad41490992c5` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-stack-hud-refactor` | `:mission` | `/home/joe/code/futon0/holes/missions/M-stack-hud-refactor.md` / `4a104c7d8a61d4c2b24c165546a285f3e035a4a6be640c5484c1cc847866b44d` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-superpod-mark3` | `:mission` | `/home/joe/code/futon6/holes/missions/M-superpod-mark3.md` / `3dde2d834d7a87f3d6f5bd5bd1f6fc1865e697ca9104ef2a9c19e003ed0b3ae7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 15 |
| `M-aif4iad` | `:mission` | `/home/joe/code/futon2/holes/M-aif4iad.md` / `1dcf5976579f90528c02fafe63d9ec5534bad91a359e38dc5d73449fb2dcf3f0` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-open-learning-system` | `:mission` | `/home/joe/code/futon2/holes/M-open-learning-system.md` / `915aba3585284482117ed5c7430257c87960d735669778b803724d251e43c617` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-patchboard-viz` | `:mission` | `/home/joe/code/futon2/holes/M-patchboard-viz.md` / `be9bcf64c04e0b93773e25322bc78dd534ec077baf87b57eca3d6cdde26890c0` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"needs Joe"} | 2 |
| `M-reflective-discipline` | `:mission` | `/home/joe/code/futon2/holes/missions/M-reflective-discipline.md` / `0f44bde3c1cf69f3b2ab62285e3cee7ee113c01f1f4737d1681a923896506ce5` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-wm-aif-policy-grain-compliance` | `:mission` | `/home/joe/code/futon2/holes/missions/M-wm-aif-policy-grain-compliance.md` / `2c5a6c186024186db7d7710da0fd9b0118493d6e314c8b666b9fd1d56dada550` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 13 |
| `M-smart-emacs-cursor` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-smart-emacs-cursor.md` / `b22c580f9dfd15f43ed86a2c8d1b2092a7dfb651214a1c30a1fe141a2e40162a` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 15 |
| `M-chipwitz-corps` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-chipwitz-corps.md` / `c2c37e3c494d529dd45e5b1f6a5bd8dc4aafdf0d07f2c1b48eb8257b9779bf46` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 1 |
| `M-artificial-stack-exchange` | `:mission` | `/home/joe/code/futon6/holes/missions/M-artificial-stack-exchange.md` / `3be2bd81bf3409acc42d2d01d81a913f585e4431c5f3a8f122fdd4c4efaa25cf` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 10 |
| `M-metric-harness` | `:mission` | `/home/joe/code/futon6/holes/missions/M-metric-harness.md` / `f62b55f0a2c44e78375881598d7b1df0d294b9feb660a65857ddf55504781f60` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 2 |
| `M-intent-curvature` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-intent-curvature.md` / `4099316a5da8056866d079f4708973e785100f9ae453c1302aec94a73e3e8a14` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-federated-agency-hardening` | `:mission` | `/home/joe/code/futon3c/holes/missions/M-federated-agency-hardening.md` / `ca5d579faf3ee2c521444074c77d1347db37761d0d0fe8377aa23d80e6bad032` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 1 | #{"needs Joe"} | 0 |
| `M-differentiable-substrate` | `:mission` | `/home/joe/code/futon6/holes/missions/M-differentiable-substrate.md` / `6ee89193c297df43a6cfa885eca15a367eabb6e56949e280b8618c017c5a9947` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-canon-fingerprint-store` | `:mission` | `/home/joe/code/futon6/holes/missions/M-canon-fingerprint-store.md` / `e7bf182d4d10d6e86b2ab58390ce6dde2747a3c632b0e13c6d2d37940a83c7e8` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-differentiable-math` | `:mission` | `/home/joe/code/futon6/holes/missions/M-differentiable-math.md` / `a17f57b0fb4e4d5f47886e0a18693904a269e6c4fde02f00d2f8552583025ef1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-prior-mathematics` | `:mission` | `/home/joe/code/futon6/holes/missions/M-prior-mathematics.md` / `baa159a7a27fe581d22812b2a70bfb2a93eb20d4d1fad65c969a148c2a8f8aa7` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-superpod-mark2` | `:mission` | `/home/joe/code/futon6/holes/missions/M-superpod-mark2.md` / `e9ff6d05d8bc246b9eb9d9e511542a90041ee94b6b9104616bb9c8ae4568145f` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-hyperreal-dictionary-planning` | `:mission` | `/home/joe/code/futon6/holes/missions/M-hyperreal-dictionary-planning.md` / `177c31ff19528e03b96e56bb2b94ac8eecf862909a6b880ec9fd95b24a6b9137` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-paper-reverse-morphogenesis` | `:mission` | `/home/joe/code/futon6/holes/missions/M-paper-reverse-morphogenesis.md` / `deab80c1392ce734634f210f45a8711bfa8679a63e193b94c0ca363e1e6e4fb1` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |
| `M-distributed-frontiermath` | `:mission` | `/home/joe/code/futon6/holes/missions/M-distributed-frontiermath.md` / `a2d6b8af9c2ef9825ceea1b47aecb2c9ae44b12228775b41ffaf9e81520a2a91` | `:typed-absence` (`:no-attributable-history`) | — | — | — | `#{}` | `:unknown` | 0 | — | 0 |

## Typed absences

- `M-categorical-code`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-categorical-code.md`.
- `M-or-training-as-learning-system`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-or-training-as-learning-system.md`.
- `M-simulating-or-training-as-learning-system`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-simulating-or-training-as-learning-system.md`.
- `M-the-perfect-crime`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-the-perfect-crime.md`.
- `M-futonzero-mvp`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-futonzero-mvp.md`.
- `M-vsatarcs-writer`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-vsatarcs-writer.md`.
- `M-vsatarcs-invariants-integration`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-vsatarcs-invariants-integration.md`.
- `M-weird-modernism`: `:no-attributable-history`; source `/home/joe/code/futon3/holes/missions/M-weird-modernism.md`.
- `M-futon-enrichment`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-futon-enrichment.md`.
- `M-stack-stereolithography`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-stack-stereolithography.md`.
- `M-essays-edit-cycle`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-essays-edit-cycle.md`.
- `M-trip-journal`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-trip-journal.md`.
- `M-pattern-application-diagnostic`: `:no-attributable-history`; source `/home/joe/code/futon3/holes/missions/M-pattern-application-diagnostic.md`.
- `M-patterns-done-right`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-patterns-done-right.md`.
- `M-joe-told-me-about-futon`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-joe-told-me-about-futon.md`.
- `M-a-sorry-enterprise`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-a-sorry-enterprise.md`.
- `M-expressions-of-interest`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-expressions-of-interest.md`.
- `M-web-arxana-ui-improvements`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-web-arxana-ui-improvements.md`.
- `M-goals-and-holes`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-goals-and-holes.md`.
- `M-the-futon-stack`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-the-futon-stack.md`.
- `M-custom-harness`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-custom-harness.md`.
- `M-stack-geometry`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-stack-geometry.md`.
- `M-pattern-posteriors`: `:no-attributable-history`; source `/home/joe/code/futon3a/holes/missions/M-pattern-posteriors.md`.
- `M-value-creation-loop`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/missions/M-value-creation-loop.md`.
- `M-recommendation-bindings`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-recommendation-bindings.md`.
- `M-pattern-retrieval-calibration`: `:no-attributable-history`; source `/home/joe/code/futon3/holes/missions/M-pattern-retrieval-calibration.md`.
- `M-aif-a-matrix-faithfulness`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-aif-a-matrix-faithfulness.md`.
- `M-pattern-mining`: `:no-attributable-history`; source `/home/joe/code/futon3/holes/missions/M-pattern-mining.md`.
- `M-digital-nomad-patterns`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-digital-nomad-patterns.md`.
- `M-formal-war-machine`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-formal-war-machine.md`.
- `M-self-improvement-loop`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-self-improvement-loop.md`.
- `M-war-machine-wiring`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-war-machine-wiring.md`.
- `M-mission-coherence-patterns`: `:no-attributable-history`; source `/home/joe/code/futon3/holes/missions/M-mission-coherence-patterns.md`.
- `M-hypergraph-operator`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-hypergraph-operator.md`.
- `M-essay-corpus-substrate`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-essay-corpus-substrate.md`.
- `M-futonzero-generative`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-futonzero-generative.md`.
- `M-zaif-harness-v1`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-zaif-harness-v1.md`.
- `E-kimi-task-28`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/excursions/E-kimi-task-28.md`.
- `M-codex-sorry-loop`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-codex-sorry-loop.md`.
- `M-interim-director`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-interim-director.md`.
- `M-shared-memory-control-build-test`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-shared-memory-control-build-test.md`.
- `M-war-machine-pilot`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-war-machine-pilot.md`.
- `M-warrant-limit`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-warrant-limit.md`.
- `M-zai-learning-loop`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-zai-learning-loop.md`.
- `M-differentiable-code`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-differentiable-code.md`.
- `M-inbox-zero-claim-lifecycle`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-inbox-zero-claim-lifecycle.md`.
- `M-aif-faithfulness`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-aif-faithfulness.md`.
- `M-operational-vocabulary`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-operational-vocabulary.md`.
- `M-autoclock-in`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-autoclock-in.md`.
- `M-autonomous-doc-maintenance`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-autonomous-doc-maintenance.md`.
- `M-essays-diachronic-model`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-essays-diachronic-model.md`.
- `M-pudding-peradams`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-pudding-peradams.md`.
- `M-war-machine-frontend-upgrade1`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-war-machine-frontend-upgrade1.md`.
- `M-landing-practice`: `:no-attributable-history`; source `/home/joe/code/futon5a/holes/missions/M-landing-practice.md`.
- `M-mission-scopes-into-substrate-2`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-mission-scopes-into-substrate-2.md`.
- `M-action-cost-modelling`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-action-cost-modelling.md`.
- `M-tpg-coupling-evolution`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-tpg-coupling-evolution.md`.
- `M-xor-coupling-probe`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-xor-coupling-probe.md`.
- `M-webarxana`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-webarxana.md`.
- `M-war-machine-vsatarcs-interop`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-war-machine-vsatarcs-interop.md`.
- `M-web-arxana-missions`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-web-arxana-missions.md`.
- `M-fulab-wiring-survey`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-fulab-wiring-survey.md`.
- `M-coupling-as-constraint`: `:no-attributable-history`; source `/home/joe/code/futon5/holes/missions/M-coupling-as-constraint.md`.
- `M-essays-retraction-visibility`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-essays-retraction-visibility.md`.
- `M-arxana-roundtrip`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-arxana-roundtrip.md`.
- `M-editorial-assistant`: `:no-attributable-history`; source `/home/joe/code/futon4/holes/missions/M-editorial-assistant.md`.
- `M-reachable-from-boot`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-reachable-from-boot.md`.
- `E-kimi-task-70`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/excursions/E-kimi-task-70.md`.
- `M-a-wmc-scaling`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-a-wmc-scaling.md`.
- `M-aif-ants-port`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-aif-ants-port.md`.
- `M-aif-policy-conditioned-eig`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-aif-policy-conditioned-eig.md`.
- `M-another-university`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-another-university.md`.
- `M-buyer-discovery`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-buyer-discovery.md`.
- `M-interim-director-long-tail`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-interim-director-long-tail.md`.
- `M-signal-roll-up`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-signal-roll-up.md`.
- `M-turns-first`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-turns-first.md`.
- `M-war-machine-aif-last-mile`: `:no-attributable-history`; source `/home/joe/code/futon7/holes/M-war-machine-aif-last-mile.md`.
- `M-state-snapshot-witness`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-state-snapshot-witness.md`.
- `M-xenotype-its`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-xenotype-its.md`.
- `M-single-entry-point`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-single-entry-point.md`.
- `M-typed-memories`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-typed-memories.md`.
- `M-bounded-disposition`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-bounded-disposition.md`.
- `M-stack-hud-refactor`: `:no-attributable-history`; source `/home/joe/code/futon0/holes/missions/M-stack-hud-refactor.md`.
- `M-superpod-mark3`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-superpod-mark3.md`.
- `M-aif4iad`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-aif4iad.md`.
- `M-open-learning-system`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-open-learning-system.md`.
- `M-patchboard-viz`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/M-patchboard-viz.md`.
- `M-reflective-discipline`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-reflective-discipline.md`.
- `M-wm-aif-policy-grain-compliance`: `:no-attributable-history`; source `/home/joe/code/futon2/holes/missions/M-wm-aif-policy-grain-compliance.md`.
- `M-smart-emacs-cursor`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-smart-emacs-cursor.md`.
- `M-chipwitz-corps`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-chipwitz-corps.md`.
- `M-artificial-stack-exchange`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-artificial-stack-exchange.md`.
- `M-metric-harness`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-metric-harness.md`.
- `M-intent-curvature`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-intent-curvature.md`.
- `M-federated-agency-hardening`: `:no-attributable-history`; source `/home/joe/code/futon3c/holes/missions/M-federated-agency-hardening.md`.
- `M-differentiable-substrate`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-differentiable-substrate.md`.
- `M-canon-fingerprint-store`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-canon-fingerprint-store.md`.
- `M-differentiable-math`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-differentiable-math.md`.
- `M-prior-mathematics`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-prior-mathematics.md`.
- `M-superpod-mark2`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-superpod-mark2.md`.
- `M-hyperreal-dictionary-planning`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-hyperreal-dictionary-planning.md`.
- `M-paper-reverse-morphogenesis`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-paper-reverse-morphogenesis.md`.
- `M-distributed-frontiermath`: `:no-attributable-history`; source `/home/joe/code/futon6/holes/missions/M-distributed-frontiermath.md`.

## What the numbers say

4 of 107 documents have a commit in the post-hook window and 103 have `:no-attributable-history` or another typed absence. The mean measured touch share is 0.000000; 14 documents contain at least one configured text marker. Both requested Spearman coefficients are `:undefined-zero-variance`, because all four measured shares are 0.0. These are two separate observations—commit provenance and literal document markers—not a feasibility verdict.
