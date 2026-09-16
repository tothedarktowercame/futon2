# WM-04-resolver-1 — strict observation authority resolver

Author: codex-4. Independent reviewer: claude-3 (pending).
Author verdict: implemented within scope; all final required gates pass.

## Authority and scope

Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-04-resolver-1.md`, SHA-256
`a9c685fd06707ab596f63d6e517ac55a944c2853784a41137e188b9ae4925ad7`.
Starting futon2 HEAD: `4fec3894`. Other agents committed concurrently; this packet
changes only its three authorized Clojure files and this receipt directory.

The initial source search found no equivalent resolver. `actuator_a3`'s resolver
is unrelated. `categorical_state_close_attachment` consumes a caller's resolver
for its close/context join; it does not construct the missing observation authority
resolver. The new namespace does not replace either.

## Unchanged contracts and exact pins

| Source | SHA-256 |
|---|---|
| `src/futon2/aif/categorical_state_observation.clj` | `8d48ecf867e2b6df7ce720b48ace3cf699bb9e19c05481c3d11369fb8d678fb2` |
| `src/futon2/aif/evidence_manifest.clj` | `6a10f78fe93a67d4c4c653658b8d5b8e1275e94b5185bbc099f20b9acfc4e0d9` |

These pins were checked before and after implementation. `git diff --exit-code
4fec3894 -- <these two files>` exits 0. No conflict with the existing contracts.

`authority-record!` calls `(resolver kind ref)` for `:evidence`, `:observer`,
`:review` and passes its result to `read-pinned-form!`. The locator really is
`{:path :sha256}`; there are no extra required keys. That existing reader remains
the only file/form/digest reader used by this resolver and the observation validator.

The manifest's closed schema has no entity or occurrence fields. The new pinned
index therefore binds the manifest digest to the complete external `:expected`
context. This does not change the manifest schema or invent a manifest subject.

## API

Namespace: `futon2.aif.observation-authority-resolver`.

```clojure
(build-resolver!
 {:index-source {:path index-path :sha256 index-sha256}
  :manifest admitted-manifest
  :expected expected-context
  :commission/ref externally-established-commission-ref
  ;; Optional only for isolated read controls:
  :io-opts {}})
;; => {:resolver (fn [kind ref] ...) :expected expected-context
;;     :io-opts ... :identities ...}
```

Pass the returned map unchanged to `validate-observation!` as its second argument.
The function takes no candidate observation. The caller must obtain these inputs
independently; neither an index nor a commission reference proves its own institutional
authorization. `:commission/ref` is mandatory, separately supplied provenance, not
an automatically authenticated commission. No production commission was fabricated.

Pinned index form:

```clojure
{:schema :wm/observation-authority-index-v1
 :index/id index-id
 :expected expected-context
 :manifest-sha256 admitted-manifest-digest
 :entries [{:kind :observer :ref observer-ref :source {:path p :sha256 h}}
           {:kind :review :ref review-ref :source {:path p2 :sha256 h2}}
           {:kind :evidence :ref evidence-ref :source {:path p3 :sha256 h3}
            :evidence/id admitted-evidence-id}]}
```

The returned identities retain commission reference, index ID and source pin,
manifest digest, exact subject and point (including cutoff), authority scope and
provenance. The closure retains immutable validated entries. Changing the index
file later cannot redirect it; a new construction with a stale pin refuses.
Resolved record bytes are still checked on each observation validation.

## Branches and division of responsibility

| Branch | Rule enforced |
|---|---|
| Constructor input check | Accept only named external inputs; require a scalar commission reference, complete expected entity/occurrence/timestamps and scope/provenance, and a well-formed index pointer. |
| Manifest validation | Reuse `validate-manifest`; do not trust a caller's claim that a manifest was validated. No source rehashing here. |
| Index read | Delegate byte digest, strict UTF-8, one-form parsing and mutation detection to existing `read-pinned-form!`. |
| Index/context/manifest join | Closed index shape and schema; exact equality of full expected context; exact manifest digest. |
| Entry shape | Only the three supported kinds and scalar identities; reuse the exact pinned pointer shape. Evidence entries must name an admitted identity. |
| Evidence admission | Index evidence identity selects the manifest entry; source path and hash must be identical; admission time must be at or before cutoff. No path inference, normalization or fallback. |
| Binding multiplicity | More than one entry for `[kind ref]` refuses, even when duplicates are identical. |
| Resolver closure | Reject unsupported kinds, structured/blank references, or absent exact bindings; otherwise return only the fixed locator. |
| Existing validator | Read referenced evidence/observer/review bytes; enforce evidence semantics, subject and occurrence, roles/provenance, independent reviewer and exact acceptance subject. |
| Review timing | No evidence-admission cutoff is imposed on observer/review record creation. Existing review validation remains intact; review of frozen eligible bytes after cutoff is permitted. |

## Controls (all fixture data, not acquired observations)

The integration fixture reuses the existing categorical observation, authority,
evidence-claim and full acceptance-subject shapes. Its evidence manifest is built
from literal files via `build-manifest`. Observer and review records are supplied
through the external index; the review accepts exactly the validator's frozen
subject, including evidence and observer source pins and expected context.

Positive: actual `validate-observation!` call qualifies the test fixture. Evidence
is admitted exactly at the cutoff (inclusive). The independently supplied test
review is created the following day; the late-review case also qualifies.

| Negative | Asserted refusal |
|---|---|
| Unsupported lookup/index kind | `:unsupported-authority-kind` |
| Known reference under wrong kind; unknown ref; missing binding | `:authority-not-found` |
| Inline/structured authority or review reference | `:invalid-authority-reference` |
| Duplicate observer, review or evidence binding | `:ambiguous-authority-binding` |
| Missing separate commission | `:commission-required` |
| Missing index source | `:missing-source-pointer` |
| Missing manifest | manifest `:shape-invalid` |
| Candidate passed as constructor input | `:resolver-input-invalid` |
| Index trying to include its own commissioning field | `:authority-index-invalid` |
| Different valid external manifest | `:authority-manifest-mismatch` |
| Evidence identity absent from manifest | `:evidence-not-admitted` |
| Index evidence path or hash altered | `:evidence-admission-mismatch` |
| Later admission, with valid rebuilt manifest and matching index digest | `:evidence-admitted-after-cutoff` |
| Tampered manifest digest | manifest `:manifest-sha256-mismatch` |
| External/index entity, run, cohort, attempt, checkpoint or cutoff differs | `:authority-context-mismatch` |
| Candidate entity or occurrence differs | `:observation-entity-mismatch` / `:observation-identity-mismatch` |
| Evidence/observer/review bytes changed after resolver construction | existing `:source-digest-mismatch` |
| Observer/review hash changed | existing `:source-digest-mismatch` |
| Observer/review path absent | existing `:missing-source` |
| Candidate-owned review | existing `:candidate-owned-review` |
| Candidate attaches index/manifest/resolver to fill missing external observer | `:authority-not-found` |
| Accepted review refers to different subject or digest | existing `:review-subject-mismatch` |
| Observer reviews self | existing `:self-review` |
| Review scope differs | existing `:review-unauthorized` |
| Index bytes changed, then rebuilt with original pin | existing `:source-digest-mismatch` |

## Final gates

- Resolver namespace: **8 tests, 56 assertions; 0 failures/errors; exit 0**.
- Categorical observation namespace: **8 tests, 44 assertions; 0 failures/errors; exit 0**.
- Evidence manifest namespace: **4 tests, 19 assertions; 0 failures/errors; exit 0**.
- clj-kondo on all three changed Clojure files: exit 0, no warnings/errors.
- check-parens on all three changed files, listed after `--`: exit 0.
- Source contract diff, scoped whitespace check, Python helper syntax: exit 0.

Each test namespace ran in its own isolated process, with no live JVM interaction.
The initial lint/parens attempt found excess closing parentheses in the new test
file; those were corrected before any test invocation. Both failed outputs and
passing second attempts are retained. No validator, test assertion, or invariant
was weakened to make a check pass.

## Exact command index

All commands below ran with cwd `/home/joe/code/futon2`. Unfiltered stdout/stderr
and command/cwd/exit metadata are in `evidence/<name>.{stdout,stderr,json}`.
No gate output was piped. `check.py` retains subsequent attempts under new names.

| Record | Command | Exit |
|---|---|---|
| kondo | `clj-kondo --lint src/futon2/aif/observation_authority_resolver.clj test/futon2/aif/observation_authority_resolver_test.clj test/futon2/aif/categorical_state_observation_test.clj` | 3 |
| parens | `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/observation_authority_resolver.clj test/futon2/aif/observation_authority_resolver_test.clj test/futon2/aif/categorical_state_observation_test.clj` | 1 |
| kondo-2 | `clj-kondo --lint src/futon2/aif/observation_authority_resolver.clj test/futon2/aif/observation_authority_resolver_test.clj test/futon2/aif/categorical_state_observation_test.clj` | 0 |
| parens-2 | `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- src/futon2/aif/observation_authority_resolver.clj test/futon2/aif/observation_authority_resolver_test.clj test/futon2/aif/categorical_state_observation_test.clj` | 0 |
| observation-authority-resolver-test | `clojure -X:test :nses '[futon2.aif.observation-authority-resolver-test]'` | 0 |
| evidence-manifest-test | `clojure -X:test :nses '[futon2.aif.evidence-manifest-test]'` | 0 |
| categorical-state-observation-test | `clojure -X:test :nses '[futon2.aif.categorical-state-observation-test]'` | 0 |
| source-pins | `sha256sum src/futon2/aif/categorical_state_observation.clj src/futon2/aif/evidence_manifest.clj /home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-04-resolver-1.md` | 0 |
| source-contracts-unchanged | `git diff --exit-code 4fec3894 -- src/futon2/aif/categorical_state_observation.clj src/futon2/aif/evidence_manifest.clj` | 0 |
| diff-check | `git diff --check -- src/futon2/aif/observation_authority_resolver.clj test/futon2/aif/observation_authority_resolver_test.clj test/futon2/aif/categorical_state_observation_test.clj` | 0 |
| python-syntax | `python3 -B -c 'from pathlib import Path; p=Path("holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-04-resolver-1/check.py"); compile(p.read_bytes(),str(p),"exec"); print("PASS")'` | 0 |

## Limits

This implements one acquisition interface only. Fixture statuses and test authority
records are test data. No acquired observation, real independent label, measured A,
coverage, production authorization, observation commission, serving activation,
reload, click, push or WM-04 closure is claimed. Authentication of the externally
commissioned authority remains the commissioning caller's responsibility. No
runtime caller was changed. Independent review by claude-3 remains pending.
