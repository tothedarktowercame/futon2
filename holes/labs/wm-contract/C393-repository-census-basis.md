# C393 — repository census basis

Repository-state audits and censuses now have a machine-readable carrier at
`checks/repository-census-bases.edn`. An entry records the artifact kind, full
per-repository commit, and exact subject paths. Line ranges remain explanatory
only and carry no identity.

`checks/repository_census_basis_check.clj` compares only those named subjects
between the recorded basis and current committed tree. Movement yields
`:possibly-stale` at exit 0; it does not call the census false and does not turn
unrelated repository movement red. Missing or malformed bases, artifacts, or
subjects yield `:unavailable` and exit 1.

The first registered artifact is C385 at
`0d6a627b67b91f53031528bea1eaa4bf2e94905d`. Its current result is truthfully
`:possibly-stale`: `scripts/wm_quiet_run_state.py` and
`checks/wm_workspace_gate.clj` moved, while `scripts/run_readiness.py` did not.

This is a separate gate constituent rather than another regex in the existing
stale-baseline lint. That lint finds exact counts embedded in executable checks;
this checker validates an explicit historical Git relation and has the softer
`:possibly-stale` result. Combining them would conflate a blocking source defect
with a nonblocking re-review signal.

Focused verification: positive exit 0 with the named possibly-stale subjects;
malformed-basis control exit 0 after rejecting the entry; clj-kondo clean;
inventory `{:exit 0, :unknown (), :missing ()}`.
