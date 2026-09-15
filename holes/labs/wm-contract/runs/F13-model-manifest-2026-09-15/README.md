# Correction (claude-20, 2026-09-15)

Two decisions behind this run are withdrawn.

1. **Target scope.** The three missions compared here are the Phase 8 canary allow-list of M-shared-memory-control-build-test (`futon3c/src/futon3c/peripheral/live_wm_selection.clj:434`, `919d9755`, 2026-07-24). Restricting F13's targets to them was claude-20's error (decision D1) and against Joe's intent. Targets range over all open missions, and the allow-list goes with the fixture selector.
2. **Guards compiled from IF/HOWEVER tokens.** This method refuses most real pattern prose and makes the rest unreachable. Relevant patterns are interpreted by agents, as documented interpretations over named mission facts, and moves the library lacks are authored as new patterns (`futon3/README-pattern-mining.md`).

The constructor's loosely related picks for this target also mean the retrieval query, which is currently the mission's title and Status line, has to be reconsidered. The findings below stand as a record of what the withdrawn approach produced.

# Follow-up: partial manifest built after q0 ruling

Owner resolution: `invoke-1789457902962-21000-0d50b14a`. The historical admission refusal below is retained; its q0 extraction question is now resolved by the verbatim rule in `partial-manifest.edn`.

`futon2.aif.cascade-model-manifest` implements that rule, preferring tables with a Status column, retaining full Status continuation lines, giving outstanding markers precedence and tagging built/unwired availability. Both marker lists and lexical stopwords are in the manifest. Bare id-stem inputs refuse. The implementation does not change the existing live adapter.

| Mission | HAVE tokens | Constructed firing list | Interpreted guards + transitions |
|---|---:|---:|---:|
| M-aif-policy-conditioned-eig | 21 | 12 | 0 |
| M-wm-aif-policy-grain-compliance | 4 | 9 | 1 |
| M-shared-memory-control-build-test | 14 | 7 | 0 |

Selected **M-wm-aif-policy-grain-compliance**, by most interpreted patterns, canonical mission ID for ties. The only interpreted member is `storage/open-world-velocity-validation`. The guard interpreter is deliberately partial: positive clauses mean conjunction of lexical-token presence; exact literal conjunctions support negation as absence; complex scopes, disjunction, modalities and substructures are missing, not permissive guards. All source clauses remain available for review.

**This is a partial model, not an executable/scored candidate family.** The one interpreted guard requires `background`, `knowledge`, `quickly`, `coherent`, and `mirrorable`, which lie outside the ruled universe. It is therefore false throughout that universe. This is a typed `:guard-unreachable-in-declared-universe` finding, not evidence of successful composition; no tokens were added to make it work. The eight other patterns have missing interpretations. Future interpretation review must resolve this before any production witness can qualify.

The finite state carrier is represented symbolically as the complete powerset of its explicit token universe, not enumerated or truncated. q0 and deterministic transition/observation rows use exact integer/ratio masses. A observes want coverage, explicitly a “token-level proxy for true discharge”; ambiguity is zero. C is missing and scoring is prohibited. T is 9, the construction-order firing count; legal stands-on orders and their cap remain a future packet, not an admissibility claim about the current order.

Findings: unclassified clause; missing C coverage mapping; eight missing pattern interpretations; unreachable interpreted guard; uncommissioned admissible precedence; success-history availability; separate documented-A validator contract. Aggregate grounded and self-graded posterior tables exist and selected-pattern rows are retained, but do not establish mission-conditioned stochastic transition statistics. No stochastic rows are implemented.

Reproduce from futon2: `clojure -M holes/labs/wm-contract/runs/F13-model-manifest-2026-09-15/build_manifest.clj`. This reads the retained construction results and verifies their mission digests before applying the new extraction. It does not select via the fixture chooser or contact a serving JVM.

Validation: `clj-kondo --lint` on the new namespace, focused test namespace and build script: 0 errors/0 warnings; `futon4/dev/check-parens.el` on those files plus the EDN: OK; `clojure -M:test -m cognitect.test-runner -n futon2.aif.cascade-model-manifest-test`: 4 tests, 31 assertions, 0 failures/errors, fresh short-lived JVM. EDN round-trip and exact q0 normalization also checked during manifest construction. No reload, click, configuration/environment change or scoring.

---

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
