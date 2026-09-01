# C428 — new mutable-verdict population members

The three undeclared members are all **content-shaped**:

- `empty_subject_acceptance_lint.py` reads registered source files and reports
  whether their current contents contain the named subject proof;
- `exit_code_scope_check.clj` reads gate declarations and lossy-boundary source
  and reports their current structural relation;
- `repository_census_basis_check.clj` compares named committed contents and
  registry declarations. Its `:possibly-stale` result is a content comparison,
  not a claim that no event occurred between observations.

The reconciled population is therefore 73: 65 content-shaped, 6 event-shaped,
and 2 neither, with zero unexplained members.

The older omission began at commit `5d6eb5e2f23121bc0faa9a972e0a84f2221d04d7`
(2026-09-01 02:02:14 UTC), when the empty-subject lint was added. The population
and reconciliation gate already existed from `63ed9c8` at 01:25. The registry's
last update before that addition was `6591647` at 01:57. Thus the reconciler was
not passing for a bad reason: from the lint's introduction onward, any direct
reconciliation run would have failed. No later fixed-tree repository-wide gate
was used to expose and route that failure until C428.

Registration should remain deliberate. Membership is already derived and
reconciled automatically; classification as content, event, or neither is a
semantic judgement and must not receive a default. The existing failure is the
right hard-to-forget mechanism: a new matching check cannot pass the gate until
a human classifies it. Automatically copying gate registration into this file
would remove the review step the census exists to require.

Focused positive reconciliation and all three negative modes pass. Inventory
reports zero unknown and missing checks.
