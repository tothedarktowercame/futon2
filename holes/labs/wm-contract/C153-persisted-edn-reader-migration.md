# C153 — persisted EDN reader migration

Date: 2026-08-31

All eleven remaining persisted-file boundaries now use
`clojure.edn/read-string` rather than Clojure's source reader.  The sites span
five files: `holder_check.clj`, `edge_census.bb`, `merge_edges.bb`,
`merge_witnesses.bb`, and `work_units.bb`.  The former sixth file was the
daily-scan boundary repaired separately in C145.

The reader-portability lint moved from 11 findings to 0.  Behaviour is
otherwise unchanged: malformed EDN still throws and fails loudly, while valid
EDN that happens not to be valid Clojure source is now accepted as the format
contract requires.  Both fragment merger round trips, the edge census, work
unit generation, the live holder check, and the complete workspace gate pass.

Canonical invocations:

```sh
bb checks/reader_portability_lint.bb
bb scripts/merge_edges.bb --check
bb scripts/merge_witnesses.bb --check
bb scripts/edge_census.bb
bb scripts/work_units.bb
bb checks/holder_check.clj
bb checks/wm_workspace_gate.clj
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```
