# Declaration read provenance

Implementation `0499986c`, merged to main as `ff28074c`.

Each declaration is read into one UTF-8 byte snapshot. SHA-256 and EDN parsing
use that same snapshot. A file changed during locator observation cannot attach
new bytes' hash to the old parsed declaration.

`load-declared` now returns ordered `:read-occurrences`, each containing path,
snapshot SHA-256, target, and unchanged observation `:results` / `:refused`.
`:target-collisions` exposes duplicate targets. The legacy assembly maps retain
their existing semantics; they are not the provenance authority.

The judgement retains those occurrences and collision indicators. The outer
runner also binds a run-owned occurrence collector around its core, so multiple
loads (including the same path with changed bytes) survive in order and completed
reads survive a later exception. The writer emits `:declaration-reads`, schema
`:wm/declaration-reads-v1`, with occurrence-indexed target collisions.
An observed empty source scan is `:absent`, reason `:none-supplied`; a writer
without observed reads or an empty scan is `:not-observed`. Old records missing
the field likewise contain no provenance claim. This does not invent provenance
for caller-injected source maps or cover habit counts and initial beliefs.

Controls exercise the real loader, observation refusal, outer runner collection,
and run writer, with the core actuator/status ports stubbed. Tests change bytes
both between reads and during observation, corrupt the snapshot hash, collapse a
refusal to false, drop a repeated occurrence, and remove provenance. Expected
values and corrupted values are explicitly unequal. Two different files naming
one target are both retained in path order and expose a collision.

Existing cascade-sources namespace: 6 tests / 22 assertions, zero failures/errors.
New narrow namespace: 3 tests / 28 assertions, zero failures/errors.
clj-kondo 0 errors / 0 warnings and check-parens OK on every changed Clojure file.
The existing record `tick-run-record-2026-09-19-1789854206.edn` gets byte-identical
wm_run_validity output before and after.

Design recommendation: refuse distinct declarations for one target unless an
explicit merge law is adopted. Overlapping facts, wants, or interpretation maps
otherwise make directory order decide the scored problem. This packet exposes
collisions and preserves existing assembly behavior; it does not impose that
new admission policy. Repeated reads of one path are retained occurrences and
should not automatically be treated as conflicting declarations.

No click, serving reload, or manual data edit performed. A serving reload of
cascade-sources, war-machine, and full-loop-runner from canonical main is needed
for activation.

Warrant: `test-registry-9b7e0ed20a5dc84113ca39134f8e793f447c460f71f51bf1d5cc6dd13cc4f4d3`, bound to `EV-declaration-read-provenance`.
HTTP check returned `warrant? true`.
