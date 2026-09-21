# fix-10c: ambient print settings changed the occurrence hash

codex-12, 2026-09-21. Base `66b379c2`; no production changes or stored digest
rewrites. The cause is **`*print-namespace-maps*`**, not changed action content.
The occurrence hash is SHA256 of unqualified `(pr-str action)`, which depends
on the caller's dynamic print bindings. With the flag false the retained action
hashes to its stored `721dfd52…`; with it true the same EDN value hashes to
`53528c49…`.

This corrects the scope of fix-10b's blocker: fresh CLI replay failed, but the
record is verifiable with its original serialization regime. Current v1 then
admits it and retains the updater in `:unknown`; current v2 admits it and
reports updater `:artifact-observation :observed false`. No record was changed.

## Then versus now

The most recent reachable commits before `2026-09-21T04:24:00Z` are:

| Repository | Commit | Commit time |
|---|---|---|
| futon2 | `2ea86caf9c61a00f34b971f0e17710edfc7b11e6` | 04:20:53Z |
| futon3c | `4d8a84a74b143fac5ec1d58742e8de3386f56863` | 00:27:08Z |

These are Git-time evidence, not an assertion that every namespace in the live
JVM was freshly loaded. Stronger evidence for the relevant runner source:

```sh
git show 2ea86caf:src/futon2/aif/full_loop_runner.clj | sha256sum
# 14891f707d86b70e6a122403aa8a7707e8a46cc9944018f4e01db051bacf2ce9
```

That exactly matches the reference run's recorded runner source digest. A
separate sibling worktree `/home/joe/code/futon2-fix-10c-then` at `2ea86caf`
ran the probe with its own futon2 code and Clojure 1.11.1. Current worktree
`/home/joe/code/futon2-fix-10c` at base `66b379c2` ran the same probe. Sibling
local dependencies remain those in this workspace; this is a replay of the
relevant code, not a reconstruction of the entire historical service process.
The occurrence hash itself depends only on Clojure printing and Java SHA256.

| Code | Print namespace maps | SHA256 of retained action | Occurrence | V1 | V2 updater |
|---|---|---|---|---|---|
| Run-time checkout | false | `721dfd52def94b1d53ff22cf8e11cb40e98b9e2acca77cb699741e51182e9e2e` | valid | admitted, unknown | API absent |
| Run-time checkout | true | `53528c495fe523feb20dd4d22f018d07d74c22124ff07a91735dc28f366c3c7f` | drift refusal | invalid | API absent |
| Current checkout | false | `721dfd52def94b1d53ff22cf8e11cb40e98b9e2acca77cb699741e51182e9e2e` | valid | admitted, unknown | admitted, false |
| Current checkout | true | `53528c495fe523feb20dd4d22f018d07d74c22124ff07a91735dc28f366c3c7f` | drift refusal | invalid | refused |

Both complete outputs are adjacent: `fix-10c-then.edn`, `fix-10c-now.edn`.
Both retain the unchanged record-byte hash
`b4e759a9cdb1be48ffad5b82fcb3c7fb13e3608c3c332ee92478a9831f5e7895`.

## Distinguishing the proposed causes

**(a) Self-inconsistent when written: no evidence of that.** The stored digest
is exactly the digest of the retained action under the false print flag, and
the run-time verifier admits the whole D-task record in that regime. The raw
record need not literally contain the bytes hashed: minting hashes the action's
printed form, while verification parses and reprints the action. The missing
part of the record is the identity serialization convention. The exact stored
digest and Clojure's root flag false strongly support the writer using that
regime; the writer's dynamic bindings were not retained, so this is an inference,
not a live-JVM measurement.

**(b) Digest/canonicalization code changed: no.**
`git diff 2ea86caf 66b379c2 -- src/futon2/aif/close_retention.clj` is empty.
`git log -S 'action/value-sha256'` and `git log -S 'pr-str selected-action'` name
`0103c396` (2026-09-14), the original occurrence carrier. The only subsequent
file commit is `0db7de6c`, also September 14. The functions used here are:

```clojure
;; mint-occurrence
(let [action-bytes (pr-str selected-action)] ... (sha256 action-bytes) ...)
;; validate-occurrence
(let [actual (sha256 (pr-str (:action/value occurrence)))] ...)
```

They do **not** call `interpretation-evidence/value-digest`. That separate
helper was introduced in `52a8e25e` (September 15); `git log -S` on its definition
and recursive `stable` also finds that introduction. It sorts maps recursively
through maps/vectors but leaves sets unchanged and still uses ambient `pr-str`.
Simply substituting it would neither preserve existing hashes nor fully solve
canonical serialization.

What changed is the **calling environment**. Both probe processes report root
`*print-namespace-maps*` false, but thread-bound value true. Installed Clojure
1.11.1's `clojure/main.clj:89` binds the flag to true in its `with-bindings`
macro. A server thread using the root false and a CLI invocation using true
can therefore disagree with identical code and identical data.

For example the action's single-entry interpretation receipt map prints as
`{:apparatus/one-authority-per-question {...}}` with false and
`#:apparatus{:one-authority-per-question {...}}` with true. The probe asserts
by comparison that reading the two complete printed action strings yields
**equal EDN values**. Neither token names nor interpretation content changed.

**(c) Action reconstructed differently now: no for this failure.**
`d-predecessor-task-authority/verify!` passes the directly parsed
`[:dispatch :occurrence]` to `close-retention/validate-occurrence` before its
job reads. That validator hashes the retained `:action/value` itself. There is
no call here to selected-cascade, candidate construction, current source loading,
or an adapter that adds observation locators. Fix-1, fix-16/fix-5a and
`28a90c73` cannot alter this already parsed map. The same failure exists in the
old checkout, and toggling only one print binding accounts for the exact hashes.

## Reproduction and current-record durability

The adjacent `fix-10c-probe.clj` is the full reproducer. It parses the original
record without mutation, calls the actual occurrence validator under both
bindings, and replays v1/v2 with the retained author/reviewer job snapshots as
the read-job capability. These are retained-evidence replays, not fresh Agency
job fetches. Current false-mode v2 reaches the actual Git/C3/C4 checks.

```sh
# In /home/joe/code/futon2-fix-10c
clojure -M holes/labs/wm-contract/runs/fixlist-2026-09-21/fix-10c-probe.clj
# In /home/joe/code/futon2-fix-10c-then
clojure -M /home/joe/code/futon2-fix-10c/holes/labs/wm-contract/runs/fixlist-2026-09-21/fix-10c-probe.clj
```

The essential form is:

```clojure
(binding [*print-namespace-maps* false]
  (retention/validate-occurrence occurrence)) ; unchanged occurrence validates
(binding [*print-namespace-maps* true]
  (retention/validate-occurrence occurrence)) ; exact reported drift refusal
```

**A click today still has this durability defect.** The probe also mints a new
occurrence through the current `mint-occurrence` using the same action: minted
with false, it validates with false and fails with true. This is not confined
to old data. No click was fired; minting is a pure constructor with injected
clock/UUID functions.

The hash covers the **entire action map**, including keys and nested receipts,
not a declared canonical projection. Later code adding fields to *new* actions
does not itself change a retained old action, so schema growth alone need not
break old validation. But a reader upgrading a retained map before validation
will change its hash. Print settings, collection iteration representation and
future printer behavior are not sealed by the current schema. This task proves
the namespace-printing failure specifically; it does not claim a demonstrated
number-printing or set-order failure in this record.

## Smallest honest repair proposal (not implemented)

1. Specify a versioned action-identity encoding, independent of caller print
   settings. For a new occurrence version, encode a declared, versioned action
   value with recursive map/set ordering, explicit sequence/type/number rules,
   and fixed readable, unlimited printer settings (including namespace maps).
   Hash those canonical bytes in both mint and validate. Reject unsupported
   values rather than stringify arbitrary objects. Keep every identity-bearing
   field, including locators and interpretation receipts. Do not casually drop
   new fields into an unversioned “projection” to make hashes agree.
2. Keep old occurrences immutable. A legacy verifier needs an explicit,
   evidence-backed serialization rule for the original v1 producers. This
   record's false-mode bytes are established here; do not silently assign that
   mode to all v1 records or replace their hashes. If other producers used true,
   specify that historical distinction before migration. A global flag flip in
   the runner is not a durable identity protocol.
3. First acceptance: mint under each caller flag, serialize/read, and validate
   under the opposite flag; both must agree for the new version. Reorder maps
   and sets as controls. Changing a token, guard, locator or receipt must still
   refuse. Retain this exact historical action and expected digest as a legacy
   fixture, plus a truly changed-action negative. Test schema growth explicitly:
   old version validates unchanged bytes; upgraded semantic content requires
   its declared new identity, not reuse of the old digest.

Probe gates: clj-kondo 0 errors/0 warnings; check-parens OK; both checkout runs
exit 0 with the table above. No production/test namespace changed, so no test
warrant is claimed. `git diff --check` is clean. No shared checkout edits,
shared-JVM loads, production store writes or digest rewrites occurred.
