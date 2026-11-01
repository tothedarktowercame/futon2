# L5-11 — stopping-equivalent classes are retained outside comparison

Base: 31626f2b3 (packet-containing main). Branch: codex11/l5-11. Worktree only.

The selector computes prediction classes as before, then partitions them by the
canonical digest of prediction outcomes against the abstention prediction digest.
Equality of G is not an additional condition. Matching classes stay in
stopping-equivalent with class id, members, pick, landing, g-units and
reason predicts-stopping. Every cascade score and task remains retained.
Only non-stopping classes participate in the minimum, policy-count and choice digest.
Construction arrangements retain their class identities for provenance.

The modern exporter uses the surviving class ids for constructed policies as well
as compared policies, G and prediction rows. The stopping-equivalent rows appear
only in diagnostics. The key's presence selects this new projection; historical
records without it keep their old constructed-policy projection.

If all classes predict stopping, selection returns no pick with
outcome {:status :abstained :case :all-policies-predict-stopping}.
The runtime now preserves that typed case instead of relabelling it empty-field.
This packet does not change Lean: the existing exporter still renders a no-pick
outer decision as abstainEmptyField. On a nonempty field that does not satisfy Q11;
the typed all-stopping case is retained, not falsely certified. A new Lean outer
abstention case would be separate work. The remaining-pick fixture below does hold.

## Tests and intentional expectation updates

- Outer selector: no-pick and unknown-relation-only menus now return typed
  abstention instead of pick-not-below-abstention; mixed menu retains the stopped
  member while selecting the acting class.
- Outer relation admission: unknown relations preserve field/cascade evidence
  on the abstention result rather than an exception.
- Runtime wiring: no inner task starts on the all-stopping case.
- Menu wiring/projector: three arrangements remain, but the no-pick discovery
  arrangement no longer counts as a compared class (2 classes; 1 after the two
  ready arrangements collapse). Construction equality excludes only the derived
  policy count from the raw menu comparison.
- New projection regression checks compared/constructed/construction-policy ids
  agree, every compared prediction differs from stopping, diagnostics preserve
  the folded row, and old records retain the historical projection.

| Arm | Tests | Assertions | Failures | Errors |
|---|---:|---:|---:|---:|
| Base outer + projector | 175 | 732 | 19 | 2 |
| Final broad branch | 177 | 751 | 19 | 2 |
| Final named selector + projector | 104 | 383 | 0 | 0 |

Broad failure/error test names and locations match base exactly.
Final named run includes the complete field carrier added to the Lean fixture
(the first fixture attempt lacked runtime-added enumeration and honestly failed Q11).
Kondo zero errors/warnings; check-parens.el OK; diff check clean.

Scratch root overrides and the fresh test-JVM root were enabled. Canonical data
counts: baseline 45930→45931, broad branch 45931→45932. Both exited 86:
global guard saw concurrent click-loop logs/ledger/alerts, STOP, and ordinary-click
consumption changes. The active claude-2 repair loop was observed (PID 184097).
No guard bypass, live reload, click, production data edit or merge was performed
by this packet. Named final counts/exit are recorded below.

## Checker and Lean

Fresh real-scorer fixture (small menu constructor seam, no score/prediction stub):
fixture.json → checker → fixture.lean → lake env lean.
**Q11 holds.** Its compared policy has G 3.4666479700346544 versus abstention
4.787491742782046; the stopped class is absent from all compared-policy carriers.
Other requirement outcomes are not claimed as conformance of this small fixture.

Unmodified historical click 11:
 /home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-7adb0f36-8dce-43b1-933b-95eaedd52f2a.edn

Branch export: /home/joe/notes/l5-11/click11-selection.json.
Checker output and Lean log are beside it. **Q11 remains violated**, as required
by historical compatibility: stopped class 2288c7271615d30b0bb6eca889ffcf2c2980fbbe2896b838cc4f70d6dbfa86f2
has G 3.1628312294328658 and prediction 20c74a30e68826fd0f45481bfbcaf9499057bc371f345f2c9f878e518b798d7d,
equal to abstention. Acting class ff0a9bb5313f90ad4a242a86ea5b977a45f3a6c95b3e28a4685ad8021c421dc0
has G 2.8745399183909477. No historical record was edited or silently repaired.

## Evidence

All commands, overrides, counts, exports and logs: /home/joe/notes/l5-11/.

Named final canonical counts: 45934→45934; exit 0.
- baseline.log: SHA256 1f5e4345465199e4d24042dee038d10347b0bfc4075256d81f1e1d7670957920
- verified.log: SHA256 6f0ef14ef2fcea3aadde0ddd53cee30a416d225d44967aaf55c7c27c2fcf1163
- named-final.log: SHA256 20d4e1b13b9001d8db27ca06e9f737008f4894a2c8ccd88baee977302930451b
- fixture-lean.log: SHA256 cd27cedbf4717d72a24e8b6fe42023129b81ce59cf8d52c814190032ac0003ba
- click11-selection.json: SHA256 5b1ae1887e3246cbf6e917e51cfdd84daabecb01ad2f2a92abac647c5db0a349
- click11-selection-lean.log: SHA256 d51f9641bc33ad7cc08ce402bee40e9bf2f691f425cceaec15456a32cdb7cd3c
