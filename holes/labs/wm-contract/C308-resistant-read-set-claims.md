# C308 — what the eight resistant checks are unsound about

Date: 2026-09-01. Reviewer: `wm-evidence`. Assessment only; no check was
converted or repaired.

## Result

None of the eight is already wholly sound merely by interpreting its result as
`:content-current`. Seven make **dynamic or mixed content-population claims**;
one (`lane_registry_check`) makes an **event/roster claim** across separate
Agency reads. Every hybrid window can produce a wrong answer, not only an
unattributable one, although some individual findings may remain true while
their population/count provenance becomes unattributable.

The C303/C305 coordinated writer fence covers all eight operationally during
the drain, provided it includes repository writers, trace producers, Agency
dispatch/job transitions, generators, and operator processes. The checks do
not prove that fence. Outside the drain, the seven file/population checks remain
mechanically open and the Agency check remains open pending a service revision
or explicitly interval-valued semantics.

## Per check

| Check | Claim and C306 class | Reachable failure | Writer-fence disposition |
|---|---|---|---|
| Preemptive repair corpus | “These findings/counts cover the tracked source population in three worktrees.” Dynamic **content-population-current**, not event-free. | Enumeration and reads can span versions: a newly added unsafe file may be omitted (false clean), a repaired file may retain a stale finding, or counts can describe no repository state. The first two are wrong answers; even locally true findings have an unattributable total/basis. | Covered if all three repository writers are fenced. Afterwards it still needs enumeration plus repository-basis sandwich for standalone use. |
| `control_map_lint.clj` | “Every drawn edge has the reported endpoint-record/schema disposition.” Dynamic **content-population-current** over the edge file and discovered node records. | Edge and record populations can cross: an edge can be judged against old/missing endpoint records, producing false agreement/disagreement, wrong specified counts, or false pass/fail. | Covered by the drain's p4ng/Futon2 writer fence. A fixed capture of only discovered paths is insufficient without proving enumeration stability. |
| `contract_lint.clj` | “This live contract/registry pair is structurally valid and its bindings are fresh/inspectable against immutable pinned evidence.” Mixed **content-current** plus immutable Git objects. | The immutable blobs remain sound, but live contract and registry can be from different moments. Qualification, stale counts, authority, or strict PASS can therefore be wrong; its Git evidence may be valid yet attributed to the wrong live declaration set. | Covered when mathlib/Futon2 writers and contract regeneration are fenced. Mechanically, split one live captured pair from immutable object reads. |
| `lane_registry_check.clj` | “The four registry rows are valid, and each held job has the reported live Agency state.” **Event/interval** claim. | Separate job reads can represent no coexisting roster. A job can terminate after its read, or a holding can change while states are sampled, yielding a false non-stale/stale answer at completion as well as unattributable roster time. | Covered during the drain only when dispatch is frozen, active work has drained, rows are idle, and Agency transitions are excluded. Outside it, a service-issued ledger revision is the honest remedy; endpoint equality cannot exclude ABA. |
| `reader_portability_lint.bb` | “No file in the current source population contains an unexempted persisted source-reader boundary.” Dynamic **content-population-current**. | Addition/removal or edit during enumeration can omit a new violation (false clean), report a repaired one, or give a file count that never existed. | Covered by the five-repository writer fence for its configured roots. Standalone use needs a basis sandwich around enumeration/capture. |
| `r2_channel_contract.clj` | “Current declarations and the discovered trace corpus jointly have these channel/era/conformance counts and this content pin.” Dynamic **content-population-current**. | Trace files can arrive while enumerated, and observation/belief sources are revisited by subchecks. The result can miss a violating tick, combine declaration versions, emit a digest for a partial population, or pass/fail wrongly. | Covered only if both repository writers **and the trace producer** are fenced. A repository-only quiet state is insufficient when traces may be appended externally. |
| `absent_is_loud_lint.clj` | “Every relevant helper/call site in the current tracked corpus makes absence loud; HEAD annotations identify that scan.” Dynamic **content-population-current** with provenance. | A new/changed unsafe source can be absent from the scan (false PASS); repaired code can leave stale findings; HEAD may be unchanged while dirty bytes differ, making even correct findings falsely attributed. | Covered by the repository/generator writer fence. Standalone use needs captured dirty bytes plus before/after population basis; HEAD alone is not the content identity. |
| `q_interface_completeness_check.clj` | “Each live source pin is current or has this historically evidenced `PIN_BEHIND` diagnosis.” Mixed **content-current** live bytes plus immutable Git history. | The live file is hashed repeatedly while history is queried. A concurrent edit can turn CURRENT into PIN_BEHIND or attach a valid historical explanation to different live bytes, yielding a wrong state/remedy. Historical blobs themselves remain sound. | Covered by the mathlib/Futon2 binding writer fence. Mechanically, capture the live source once, bind that digest to the historical explanation, and refuse movement. |

## Which remedy reaches what

- **Content-current plus population identity:** preemptive corpus, control-map
  lint, reader portability, R2 channel contract, and absence-is-loud.
- **Content-current with immutable-history separation:** contract lint and Q
  interface.
- **Event/interval semantics:** lane registry; it cannot obtain an
  instantaneous multi-job fact from the current API.

There is no ninth, unreachable remedy class in this population. All eight are
operationally closed by a genuine full writer fence during Joe's drain. That
does not make their standalone mechanics sound, and a C292 `QUIESCENT` result
must not be treated as proof that the fence existed; C303 already records that
authority boundary.

## Inventory in the delivering commit

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

The command is run after the delivering commit; its exact result is reported
with this delivery. It inventories check classification, not read-set
soundness.
