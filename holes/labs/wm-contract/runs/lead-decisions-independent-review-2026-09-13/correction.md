# Correction to review `90fefed4`

Date: 2026-09-13  
Corrects: `review.md` in this directory; failed history is preserved.  
Reviewed follow-up: `9f37f503edc2d0ef6e4297bb1c69a461aac3a135`.

## Defect

The original review claimed that three mechanically recomputed digests matched,
but printed values that match neither the execution receipt nor the files:
`CertificateStates.lean`, `CertificateStates.olean`, and `lean-toolchain`.
Those were transcription errors: they were not copied from the mechanical
command output. Consequently the original match assertion for those three pins
was false even though its substantive Lean reading was unaffected.

## Mechanical recomputation

Command (run once for this correction; this is a digest/readback, not a proof
rerun):

```sh
sha256sum holes/labs/wm-contract/LEAD-DECISIONS-2026-09-12.md holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/execution-receipt.json holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/gain_probe.bb holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/input.edn holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/result.edn src/futon2/aif/policy.clj data/wm-trace/wm-trace-2026-09-12.edn holes/labs/wm-contract/runs/row-24-lead-audit-2026-09-12/execution-receipt.json holes/labs/wm-contract/runs/row-24-lead-audit-2026-09-12/CurrentPredicateCounterexample.lean /home/joe/code/mathlib4/DarkTower/WarMachine/CertificateStates.lean /home/joe/code/mathlib4/.lake/build/lib/lean/DarkTower/WarMachine/CertificateStates.olean /home/joe/code/mathlib4/lean-toolchain
```

Exact output:

```text
19c088c7a8817d2fff0df11419b37262140674ca7b986c1374c526898c04c48b  holes/labs/wm-contract/LEAD-DECISIONS-2026-09-12.md
48b979fb40de70533a5ea5378ed39d4a4a6bc1db5493b3d1109a570802358342  holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/execution-receipt.json
8ac55b0f02e3c79d17eb31e82a80caab0172496408a248d1758d0f445af94723  holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/gain_probe.bb
1818dd14eabe7274815e1298b7d59909b06f98d853db52a84ec1f3e229f20235  holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/input.edn
8193604566d620cda2a3a29856dd9efa6a47b51421652d2077100816fd8f97fd  holes/labs/wm-contract/runs/row-18-lead-audit-2026-09-12/result.edn
b0dc9a1a440c18ec1464ee96abb9c76401a02295e8e3e7f0840b105242236628  src/futon2/aif/policy.clj
3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175  data/wm-trace/wm-trace-2026-09-12.edn
2b8446fd20e196a7935175bc7e6a913d8df4df6c07b4394cc9cb4fb0a291f15e  holes/labs/wm-contract/runs/row-24-lead-audit-2026-09-12/execution-receipt.json
ee8bc03c6e4e5786a9b103efe04ea1716af3f5b24a0689aed475aa08ef47f2fd  holes/labs/wm-contract/runs/row-24-lead-audit-2026-09-12/CurrentPredicateCounterexample.lean
83bb3bbbea3bad9a9d56568704300427f94e1bf1539ad322041e3cae8beab778  /home/joe/code/mathlib4/DarkTower/WarMachine/CertificateStates.lean
7898d439a49a0887e324229c5acd247ddba1697c245892cefdb1032c8bb4c118  /home/joe/code/mathlib4/.lake/build/lib/lean/DarkTower/WarMachine/CertificateStates.olean
33cbab0d3ba76bdf58d9f3638748f12cb9e3befb1336b223ddbd3567589a09e8  /home/joe/code/mathlib4/lean-toolchain
```

Every digest pinned by either execution receipt matches the actual bytes. The
daily trace claim is also confirmed at exact full-file digest
`3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175`.
The corrected three Lean/toolchain digests are exactly those in
`lead-pin-discrepancies.json` and the row-24 execution receipt. No proof was
rerun.

## Follow-up decision review

Commit `9f37f503` implements F1 through F4 faithfully: saturation and
variational inapplicability are non-qualifying; genesis authority must be
external to the candidate record; the retired `R7->R14` identity is removed
from positive work pending declaration-grain replacement; and latent estimates
cannot discharge measured categorical A. No conflict remains in those textual
repairs. Two implementation dependencies remain rather than policy defects:
the external authority record and verifier must still be located/built, and the
correct beta/gamma/interoception declaration seam must be specified before row
18 can claim theory-aligned qualification.

