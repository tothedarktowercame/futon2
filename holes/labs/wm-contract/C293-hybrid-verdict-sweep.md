# C293 — hybrid-verdict sweep

Date: 2026-09-01

Scope: executable checks under `futon2/checks`. This is a discovery pass; no
checker was repaired.

## Method and coverage

The initial lexical census selected checks containing filesystem reads,
repository queries, directory enumeration, or Agency HTTP reads. It found 113
candidate files. This pass reviewed the 23 highest-risk checks: every check
with two or more explicit mutable-boundary operations. The remaining 90 are
mostly single-input witnesses, but checks that enumerate a directory with one
lexical `slurp` remain a tail requiring a second pass; lexical occurrence count
does not prove observation count.

A hybrid verdict is reachable when two observations of state another process
can change are combined without either (a) reading one immutable snapshot or
(b) comparing a before/after identity and returning unavailable/moved.

## Reviewed population

### Movement detected or reads intentionally describe movement (4)

| Check | Boundary | Disposition |
|---|---|---|
| `wm_workspace_gate.clj` | four Git repositories | Protected: captures four-repository provenance before and after; movement is `repository-basis-changed`. |
| `quiescence_check.py` | repository and lane-registry samples | Honest moving-subject check: the two observations are the measurement; identity/digests are compared. |
| `wm_click_resource_observer.clj` | cgroup counters before/after a click | Honest delta measurement. It does not assert the two samples are one state. |
| `contract_lint.clj` pinned fixtures | Git objects named by SHA | Immutable for pinned-git bindings. Its live contract/registry pair remains exposed separately below. |

### One captured value or mode-separated reads (4)

| Check | Boundary | Disposition |
|---|---|---|
| `preference_stack_binding_check.clj` | binding then the witness it names | Reachability bounded by an immutable committed witness in normal mode; negative modes are separate invocations. |
| `generative_model_witness.clj` | fixture; Lean source only in negative mode | The two reads do not contribute to one positive verdict. |
| `model_uncertainty_eig_witness.clj` | Lean source basis or receipt | Mode-separated; positive receipt validation does not combine a second live source observation. |
| `certify_live_run.clj` | run/receipt candidate discovery | Each selected file is read once; run-id matching prevents a different receipt being accepted, though directory-enumeration movement belongs in the tail audit. |

### Hybrid verdict reachable in principle (15)

| Check | Mutable observations combined | Honest remedy |
|---|---|---|
| `preemptive_repair_suite.clj` / `preemptive_repair_lint.clj` | One C289 in-memory corpus still spans tracked files in three live worktrees. The bounded suite detects basis movement, but standalone use has no sandwich. | Observe repository basis before/after capture and return unavailable on movement; or scan immutable Git objects if dirty worktree findings are not required. |
| `control_map_figure_agreement_check.clj` | edge EDN, SVG, and tracked PDF | Snapshot the three bytes once and validate those bytes; optionally sandwich their digests. |
| `control_organization_check.clj` | organization, stages, and edges EDN | Snapshot all three once with their digests; reject movement. |
| `control_map_lint.clj` | edge EDN plus many node records | Snapshot the population once; a basis sandwich is needed because records are enumerated/read sequentially. |
| `r9_proof_receipt_check.clj` | receipt then live proof source | Prefer the receipt's pinned source blob; otherwise sandwich source identity. |
| `r8_pinned_snapshot_witness.clj` | live Lean source, generated Lean source, EDN fixture | Snapshot three byte strings once and bind the verdict to their digests. |
| `wm_route_conformance.clj` | control-map data then tick receipt | Snapshot both; a receipt should name the topology digest it is judged against. |
| `r17_generator_disposer_check.clj` | the same file is read once for SHA and again for text | Read bytes once, derive both SHA and text from them. This is the smallest definite double-read defect. |
| `holder_check.clj` | generated contract JSON and holder registry EDN | Snapshot both and check their authority relationship from those bytes. |
| `contract_lint.clj` live authority | contract JSON and witness registry EDN are read separately | Snapshot the pair; retain immutable Git reads for pinned fixtures. |
| `lane_registry_check.clj` | registry file then one Agency request per held lane | Agency is a legitimately moving subject: record observation times/job states and return unavailable or moved when the sample cannot represent one interval. A frozen snapshot is not available from the API. |
| `reader_portability_lint.bb` | a live source population enumerated and read sequentially | Basis sandwich, because its subject is deliberately the dirty worktree and Git-object snapshotting would omit the condition it checks. |
| `r2_channel_contract.clj` | declarations/corpus plus repeated live source reads | Capture each source once and pass the byte strings through all subchecks. |
| `absent_is_loud_lint.clj` | tracked source corpus plus repository HEAD annotations | Capture source bytes once and sandwich repo identities; HEAD labels alone do not identify dirty bytes. |
| `q_interface_completeness_check.clj` | current source record plus Git history used to explain its pins | Historical blobs are immutable, but bind the explanation to the captured current-source digest or report movement. |

## Remedy classification

- **Snapshot once** fits a bounded set of files whose exact bytes are the
  subject (`R17`, figure agreement, organization, R8, route conformance,
  holder/contract pairs).
- **Observe movement** fits a live population or service (`preemptive` corpus,
  portability lint, Agency lane states). These need a before/after basis or an
  explicit observation interval and an unavailable/moved verdict.
- **Neither is directly available:** Agency exposes per-job reads but no
  multi-job snapshot token. `lane_registry_check` therefore cannot claim an
  instantaneous roster. It can only report a time-bounded observation or ask
  Agency for a snapshot-capable endpoint; silently freezing the first answers
  would not solve the problem.

## Conclusion

This is a population, not four isolated fixes: 15 of 23 high-risk checks have
a reachable hybrid-verdict window, four are protected/intentional, and four
are mode-separated or identity-bound. The remaining 90-candidate tail should
be audited before designing a lint, because directory enumeration and helper
functions defeat a purely lexical rule.
