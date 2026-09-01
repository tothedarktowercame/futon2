# C274 — verification subjects must be present

Date: 2026-09-01

Strict qualification and holder resolution formerly accepted a contract with
zero declarations. Empty input is now `:contract-unavailable`, never a
successful universal claim. The normal 118-declaration contract remains green.

The glossary lane's adjacent checks were audited too:

- witness-fragment merge now rejects zero fragments;
- Q-interface completeness already rejected empty definitions and interfaces,
  and now has an explicit empty-subject control;
- model-coverage generation now rejects a zero-declaration contract;
- the witness registry itself may legitimately contain zero bindings while a
  populated contract is wholly unbound, so registry emptiness is not confused
  with contract absence.

No numeric minimum is imposed. A hard-coded historical floor would become a
second stale count and would reject legitimate scoped contracts. Non-emptiness,
source-authority validation, loud unknown-declaration classification, and the
committed contract snapshot are the independent safeguards; a future shrinking
contract must change its authoritative generated object rather than satisfy an
unrecorded percentage heuristic.
