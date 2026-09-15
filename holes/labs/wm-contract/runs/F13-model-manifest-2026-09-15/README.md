# F13 packet 1 — model admission stopped at q0

Status: **blocked; no model admitted and no implementation claimed**. Author: codex-27, 2026-09-15. This is the explicit source-admission stop requested in the dispatch, not a successful packet-1 manifest.

The existing constructor was actually run in a short-lived Python process against all three source-pinned mission documents, using its default epsilon 0.15 and pool budget 40, with no output truncation. The embedding model came from the existing local snapshot with `local_files_only=True`; Python `-B` disabled bytecode writes. No serving-JVM interaction occurred.

| Allowed target | Constructed patterns | Interpretable firing count |
|---|---:|---|
| M-aif-policy-conditioned-eig | 12 | Not assessed: q0 admission stopped |
| M-wm-aif-policy-grain-compliance | 9 | Not assessed: q0 admission stopped |
| M-shared-memory-control-build-test | 7 | Not assessed: q0 admission stopped |

These counts are constructor results, **not** enabled-guard counts or a target-selection verdict. The full results, exact queries, mission digests and parameters are in `construction-comparison.json`. Typed findings and 31 source digests are in `admission-refusal.edn`; `admission-refusal.json` is its JSON source. Mission digests were checked again after construction. Constructor/library dependency digests were captured after execution, not atomically locked during it.

## Blocker and resolution question

`:missing-source-bound-have` names an unresolved interpretation, not a missing mission file. `scripts/futon2/report/cascade_lane.clj:306-334` explicitly uses a mission's Status line as the retrieval query's HAVE. That supplies source-bound text; it does not specify which words denote established capabilities in D2's state. None of the three documents has an explicit HAVE/WANT field (the scan and paths are retained in the refusal).

Concrete ambiguity: `holes/missions/M-aif-policy-conditioned-eig.md:4` says “pure kernel instantiated; generative contract open”. Treating all its salient words as established tokens gives positive presence to both “generative” and “contract”; dropping the open clause or encoding an absent contract is a different initial state. The component table at lines 112-115 likewise distinguishes built/unwired, candidate, and absent prediction capabilities. `holes/missions/M-wm-aif-policy-grain-compliance.md:4` mixes completed dark slices with open live seams. `futon3c/holes/missions/M-shared-memory-control-build-test.md:4-5` distinguishes live, dark/shadow, replay and operator-gated enactment; the current query adapter also omits the continuation line.

**Owner question:** should q0 mean literal tokens occurring in the complete Status assertion (including open/negated capabilities, explicitly a document-content proxy), or tokens of established capabilities extracted from particular affirmative spans? If the latter, please settle that extraction rule or identify the existing measured HAVE producer. These differ on the actual source, so I did not select one silently. This is not a claim that no honest documented extraction is possible.

The other finding, `:missing-coverage-preference-map`, is independently unresolved: `src/futon2/aif/ruled_outcome_c.clj:6-15,52-59` provides named disposition masses, not a mapping to coverage fractions. No map was established during this inspection. Per D3, that finding would permit a partial manifest but prohibit scoring; it is not the reason this packet stopped.

No namespace or test file was added, so Clojure lint, paren and focused-test implementation gates are not claimed. Evidence validation checks EDN round-trip, typed refusal tags, all three constructor counts, mission source digests and the construction-artifact digest. Per-pattern success-history discovery, guard comparison, target choice, kernel normalization controls and manifest implementation remain unperformed pending the q0 decision.
