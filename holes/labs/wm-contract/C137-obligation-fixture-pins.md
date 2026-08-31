# C137 — witnessed-obligation fixture pins

Date: 2026-08-31

## Finding

All five `WITNESSED-INSTANCE OBLIGATION` declarations named an evidence kind,
but none named a fixture path or a content digest in its Lean docstring. The
proof bodies were therefore self-contained, while the source-level explanation
of what recorded instance they track was not protected against fixture drift.

The ablation fixture is unchanged from its recorded run commit
`f54d502acb131f74da40b430a726792a4002dae8`:

- `futon2:holes/labs/wm-contract/ablation-exact-dyadic.edn`
- SHA-256 `f315b748420540688ef81086101b5789a4ecb2bd2a84c7a2b491f94fe8c56261`

The shared F1–F4 fixture did change after the registry's old run commit
`594127059d6a31c5b21f5d34ebfac5b14b1ad993`. That change was the intentional
C65 repair which removed 21 duplicated antecedent mismatches. Its present bytes
are exactly the historical object committed at `futon3` `ab2b290` when the
fixture-indexed F4 claim was validated:

- `futon3:checks/find-snatch.edn`
- SHA-256 `839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa`

Thus the ablation pin was mechanical. The Find pin was not taken from the live
file: it was recovered from and compared with the post-repair historical commit.
The registry's older `run-sha` remains a distinct provenance field and was not
silently presented as the fixture pin.

## Enforced rule

`checks/lean_sorry_category_check.clj` now requires every witnessed-instance
obligation to carry exactly one `fixture: ` repository/path marker and exactly
one 64-hex `fixture-sha256:` marker. The repository vocabulary is closed, the
file must exist, and its bytes must match the recorded digest. The existing
proved-body rule remains in force.

Controls:

- `--negative-missing-fixture` replaces the ablation path with an absent file
  and is rejected.
- `--negative-fixture-drift` changes only its recorded digest and is rejected.

Both controls are classified in `checks/wm_workspace_gate.clj`; discovery does
not silently execute or ignore them.
