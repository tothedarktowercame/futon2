# T-type repair proposal supply

Authority: Joe's `6402ed8a`, refined by Claude-12's
`invoke-1789963751731-22873-182dc00f`. The target spelling is
`T-<repair/id>` in full. The original failed target stays in evidence.

## Delivered

`repair-proposals/supply` uses the existing repair-store `open-obligations`
reader. Resolution and dismissal records exclude their finding identities;
implementation alone does not. Every tick reads the store afresh, so a cached
open proposal cannot outlive a subsequent disposition in this supply branch.
This is a read-time view, not a lock spanning subsequent selection/execution.

An open, structurally complete finding yields `:repair-finding-proposed`,
`:proposed`, `:target-type :T`. Its evidence contains the finding id, schema,
class, failure kind/stage, original target (including nil), opened-at and the
verbatim discharge contract. The original finding bytes are hash-pinned.
A retained backtrace is pointed to by that pin plus `:edn-path [:backtrace]`;
an absent backtrace is explicitly absent, never reconstructed. There is no
pattern, reading, guard, produces mapping, or observation locator inference.

Missing schema, failure identity, timestamp or contract structure produces a
`:repair-finding-evidence-missing` decline naming the missing fields. Contracts
are not replaced with a generic four-part contract: environmental holds and
other artifact shapes retain their own requirements. The historical
schema-less control remains in tests even after parallel WONTFIX dispositions.

The runner combines retained retrieval proposals and fresh finding proposals,
includes proposed targets in assembly enumeration, and retains the trail in
its existing proposal-supply certificate and decline channel. The declaration
loader is unchanged.

## Closure evidence and admission boundary

`repair-obligation/resolve!` writes unversioned
`data/wm-repair-obligations/resolutions/<repair-id>.edn`. Existing C3–C6 check
Git revisions. These store bytes are therefore not currently observable through
an admitted locator. The proposal names the real record path/id with
`:closure-observation {:status :unavailable ...}`; no HEAD path or future witness
is invented.

Per Claude-12's explicit ruling, repair declarations are withheld while that
witness is unavailable, even when a declaration already exists. The specific
`:repair-closure-observation-unavailable` reason is recorded as a decline;
assembly uses the existing `:universe-not-admitted` refusal with missing
`:locators`. This preserves the decision gate's existing vocabulary.

Codex-8's discharge lane owns producing a committed per-finding receipt at a
stable, declared path. A follow-up then admits its real locator. That follow-up
must carry the contract verbatim and validate the admitting agent's explicit
requirement-to-want/observation mapping. This packet deliberately admits no
repair declaration before that producer exists; it does not claim the repair
execution/closure loop is complete. Coordination was sent in
`invoke-1789964122607-22882-7cf7f1f5`.

## Verification

Final pre-commit runs: **24 tests / 158 assertions, zero failures/errors**:
repair-proposals 5/31, cascade-proposals 6/36, cascade-decision 13/91.
Each ran separately in a fresh tooling JVM. `pre-validation.json` records the
base SHA and changed-file hashes. Controls use the real finding, dismissal,
resolution, store reader, proposal recorder and decision gate, with temporary
stores only. No production store was written by these tests.

Exploratory logs are retained separately. The first failed because the test
passed a File object to the store writer's string-root API; the fixture now
passes the expected string. The next caught an unsupported refusal kind at the
real decision gate; the correction uses the existing universe/locator refusal
and preserves the specific cause as its reason. The unchanged declaration
loader and decision gate were not loosened. Final validations follow these
corrections; no failed run is described as a warrant.

clj-kondo: zero errors/warnings (one existing informational str diagnostic).
check-parens: OK on all changed Clojure files. git diff --check: clean.
`live-census.edn` retains the real read-time proposal/decline census and verifies
no proposed finding id appears among the read resolution/dismissal records.

No reload, author dispatch, resolution, dismissal, or admission was performed
against production by this packet. Committed is not loaded. Registered
post-commit evidence follows in the receipt commit.
