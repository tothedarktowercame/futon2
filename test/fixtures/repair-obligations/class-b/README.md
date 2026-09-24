# H-PUBLISH-D class-B resolution records (fixture copies)

Verbatim copies of the nine class-B resolutions named in
`holes/labs/wm-contract/proof2/packets/H-PUBLISH-D.md`: records written
2026-09-24T04:04Z by `runs/stop-line-discharge-2026-09-24/discharge.clj`
through context-free callers. None carries `:repair/discharge-context`.

Source: `data/wm-repair-obligations/resolutions/<id>.edn` in the live
futon2 checkout (gitignored store), copied 2026-09-24 by claude-11 when
futon2 HEAD was 942beb0df6f65dc98b4c4d2288998264248a2b9c.

Why copied: `repair-discharge-context-test/pin-class-b-records-lack-the-context-today`
read these from the live store, so a pinned-worktree registration (no
`data/`) could never pass it. The test now reads this directory. These
copies pin the records as they were at copy time; they do not track the
live store.

sha256 at copy time:

```
5678b7e02f488f7a202552a5c4cc4ccbc8e0bf1032fb188ef7f1f890777b83c5  repair-attempt-051-feature-card-missing-or-invalid.edn
88f2f4f95acc3bf7320404068dcc99c3d8b9d418afa944c9bc966f2b7d536b59  repair-attempt-052-strategic-selection-unavailable.edn
163a4880ec7b041ba508d7179537d2ce21106e4e673fdc807aedbe3ce3766853  repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-001-untyped-failure.edn
54be108fb10aa92a4e2e13a57f886d9e9d69a3e3872b5b70fed65cb72e5a7269  repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch.edn
a6d6de801e033323587cfc7ab02e1562bd4c6927d9bbd21c99fec37725e56b5b  repair-ea1-504ad8630070adf67604aab739c0c0184b5ccf632f552ec6d2b55fac7ad71c87--attempt-001-feature-card-missing-or-invalid.edn
85cd43e0b9bb43644848907a71e52eb993d8ad2f29fb83ef27bc8227d2dde2aa  repair-ea1-7093b8fbb1ca8fc99a18899b69ca39f1e5a1a129e76739ebddbbee974299f94f--attempt-001-untyped-failure.edn
937f7d9167d024245935377806f938d69405cc8035b02c9003f2f02064dc5d49  repair-ea1-b0eeafa0e59b4dc0dd9e0abe1cbbed0e687c5827b3ade8320c98c7f82a79032b--attempt-001-machine-repair-lacks-grounded-review-evidence.edn
87c45e2731c4951792098503c4664e4c5ece8709e643e5f6c320bfe7e4a45c5f  repair-initialization-6d5da36a-04f0-42ee-ba91-55bee5801a01-initialization-failed.edn
4c80414fd67e0ea67cf7d2908feeaff5843fcb489c293cbdeadbdd3232474f4d  repair-initialization-a9177cab-e783-464e-83e2-21a6371484a4-initialization-failed.edn
```
