# Cascade proposal withhold repair-store fixtures

Verbatim copies of two actionable-open repair records used by
`cascade-proposals-withhold-test`.

Source: `/home/joe/code/futon2/data/wm-repair-obligations/` in the live,
gitignored futon2 store. Copied 2026-11-03 when the canonical futon2 HEAD was
`27c336dd6116895e2ef1b688cda0362aa74af9e0`.

Why copied: packet F4c repairs tests after b13461d2e made the test data root
hermetic and after 072dc0404 renamed the proposal scan from
`:open-finding-ids` to `:actionable-ids`. The fixture preserves the real record
bytes while the test supplies its own declared source where needed. These
copies pin the records as they were at copy time; they do not track the live
store.

The first ID has both a finding and ticket link in the live store. The second
has only a finding. The empty lifecycle child directories are retained so the
fixture is a complete, test-owned repair-store root.

sha256 at copy time:

```text
1f38b263fe589d8e95e294584c7d6265a9611649be3cb4bc3ba7252e6cb2ebf9  findings/repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository.edn
8992e455dc529026873f08acc50b0110d4ab03b23e82d848f045e9299aca4fc6  findings/repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged.edn
569d07f0ecfca2e944b7587ef45e46be697b7b06a9ea55a250b302a346cb3265  ticket-links/repair-ea1-19929445d661380bc279f9e439dd2808eec422ce3bf96f7efd0c168da7e3b153--attempt-002-author-delivery-wrong-repository.edn
```
