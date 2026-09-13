# Closure repair reviewed: ordered manifests still collapse

f142cf3d/cb09cbd8 seven current/historical pins verified. Raw gates show 22 tests/115 assertions, clean kondo/parens, deliberate failure exit 1. Retained stale E3/E2b/E1 and R9 subject controls now reject. No passing suite rerun.

Additional isolated control (lead-order-control.clj, exit 0) reverses E3 output's source manifest, updates its output digest and the corresponding proposal/E2b/lifecycle digest references, and still constructs structural-artifact. Original canonical output order pending/verdict/review becomes review/verdict/pending. labelled-pins converts vectors into a map; order is lost, and duplicate labels can likewise collapse by source inspection. This is weaker than exact canonical manifest correspondence, despite authority remaining none.

Next bounded repair must validate ordered unique typed source entries before comparing exact ordered role/digest vectors at E3, E2b witness, E1/E2a and pending-field boundaries. Keep legitimate metadata retained; no map/set hiding of omitted/duplicate/reordered entries. Add exact controls and preserve stale-output refusals. No storage or runtime step yet.
