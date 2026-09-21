# H4 after H3 — combined committed-code revalidation

Author: codex-1. Requested by claude-12; H3 landing notification from codex-6.
Tested revision: `7ff19823f325b3e9e05156f8046dc2ef6dbd4a5d`, containing
H4 `acc4f3c4640b4092710cceaf9d88f50acaeb0663` and H3
`991e27a4170983c17e33b80c73fea5f4cd88d5c3`.

## Result

The deferred two-run production registration is complete **for the combined
committed code**. Arithmetic was also registered twice against that revision.
All four runs returned `warrant? true` and bound their subjects. These are not
warrants for the canonical checkout's additional uncommitted work.

| Scope | Run | Tests/assertions | Warrant |
|---|---:|---:|---|
| Production staging | 1 | 1/18 | test-registry-9bfbf4b65860d8e3a4519242e8f0d947aa23c9550d136e12e88f75653ff9bbc3 |
| Production staging | 2 | 1/18 | test-registry-ac959f1bb2c61666eadbe90229e317ab9227053b0ac044b88561d71e7b652b8e |
| Prefix arithmetic | 1 | 8/43 | test-registry-efa07f352d62300fc0319de9e6bb497a53cf9c30265dc435660479f0cfdb3387 |
| Prefix arithmetic | 2 | 8/43 | test-registry-df4e5883f77aea7f6767b844f6cb555fb896e4c76749f30ffd5c10f70747bd6d |

Zero failures/errors in each. Production runs use the actual cascade-decision
function with temporary stores and the hermetic repair/trip fixture; its store
file-set assertions passed. Arithmetic remains explicitly synthetic. H4 stays
open: this does not demonstrate a consumed, nonempty admitted policy prefix.
No click, shared-JVM reload, source edit or production admission occurred.

## Why a detached checkout

A fresh canonical-checkout registration passed 1 test / 18 assertions but
correctly refused a warrant. Record:
`test-registry-6fca4eda3c9301d0e4b7db291752b0282453f18e1e134425a614b9b48c844715`.
The evidence endpoint's postcheck reported:

```
{:reason :scope-not-committed
 :details {:stage :load-closure :count 2 :reasons {:dirty 2}
           :paths ["src/futon2/aif/d_predecessor_task_authority.clj"
                   "src/futon2/aif/repair_obligation.clj"]
           :next-action :commit-before-registering}}
```

Those are other lanes' work. Rather than register their uncommitted bytes or
alter them, validation used a clean detached checkout at the revision above:
`/home/joe/code/futon2-h4-validation-4U8t18`. No source patch was applied there.
Its status was clean after the runs. This is committed-code validation, not a
claim that the live checkout or serving JVM has these exact bytes.

## Commands and reproduction

From futon3c, execute each command twice, sequentially:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/h4-staged-prefix-2026-09-21/combined-production-registry.edn
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/h4-staged-prefix-2026-09-21/combined-arithmetic-registry.edn
```

Both configs specify the detached repo root. The registered logs and load
closures are retained alongside this note. The temporary checkout is removed
after validation; reproduce with `git worktree add --detach` at the recorded
path and exact revision before using those configs again. No force or reset
was used. EDN artifacts were checked for exactly one form before commit.
