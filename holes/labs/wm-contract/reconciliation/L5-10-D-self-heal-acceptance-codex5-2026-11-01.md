# L5-10-D — self-heal acceptance on an environmental injury

Read-only discovery on `codex5/l5-10-d`, based at `aa2eac2f4`. No runtime or
`data/` state was changed.

## 1. The cause-scope digest mismatch

`produce-acceptance!` mints the scope by calling `cause-scope/seal` in the
long-lived futon3c serving JVM's call into futon2
(`src/futon2/aif/meta_self_heal_production.clj:771-788`). `seal` reads and
SHA-verifies the pinned file, EDN-decodes it, selects the input path, and hashes
UTF-8 bytes of `(pr-str input)` (`src/futon2/aif/cause_scope.clj:24-40`). The
separate bounded acceptance JVM repeats the EDN read and the ambient `pr-str`
hash in `assert-repair` (`cause_scope.clj:49-61`); futon3c starts that JVM as a
systemd transient unit running the admitted test command
(`futon3c/src/futon3c/wm/meta_self_heal_adapters.clj:792-819`). Neither digest
site binds the printer.

For click 6's pinned record, input path `[]` selects the entire 54-key map. I
recomputed these values from the SHA-verified pin:

| serialization | SHA-256 |
|---|---|
| `pr-str`, `*print-namespace-maps* false` | `62517c952a5e01d2e100211f988f85c4f61d40e21f2e28c6e37e706706c94d06` |
| `pr-str`, `*print-namespace-maps* true` | `bf3d771f3fa75dff00b800c3ab338c6f6ee7f859b781dd0ce7f9b289b30808a7` |

Both retained acceptance proposals contain the first digest. A fresh
`cause-scope/seal` under ordinary CLI bindings produces the second. Binding
`*print-length*` to `1000` or leaving it `nil` produces the same second digest;
binding it to `100` produces a third digest
`15e871af058f76e97326c00f64dac48f26d7d0685985058175b0443465c033e0`.
Thus the observed mismatch is namespace-map printing, not map order, tagged
literals, print length, or print level. The production path already establishes
that ambient printing is unsafe: nearby diagnosis serialization explicitly
binds length, level, and namespace-map mode
(`meta_self_heal_production.clj:207-212`), while `seal` does not.

For input path `[]`, using the already verified file bytes (equivalently, the
pin's SHA-256) is the smallest honest repair: the carrier is exactly the whole
pinned file, so no decode/reprint is needed. It should be a named whole-record
case, with a test proving both namespace-map modes yield the same scope. For a
proper subpath, a canonical value encoding with all print bindings fixed is
still required; substituting the whole-file digest there would cease to name
the selected carrier.

## 2. Environmental acceptance and discharge

There is no environmental-specific acceptance or discharge route in the
self-heal namespaces. The only discharge in integration requires a retry
classification of `:non-injury` with reason `:successful-task-outcome` or
`:producer-qualified-already-satisfied`
(`src/futon2/aif/meta_terminal_injury.clj:395-419`). A repeated injury instead
records a retry result or remains active (`meta_terminal_injury.clj:433-518`).
The current acceptance contract invokes the declared function on the retained
carrier and tests its returned result (`cause_scope.clj:42-68`); it cannot
demonstrate that a transient external read is healthy now when the carrier is
an old terminal record.

The smallest honest design is an explicit environmental cause-entry whose
machine-owned acceptance operation re-runs the named external read with pinned
endpoint/request parameters, deadline, and expected response predicate. It
must retain a probe receipt containing request identity, start/end time,
outcome, response digest (or typed failure), and the injury/cause pin. A passing
fresh probe can then supply a typed environmental recovery observation for the
existing ledger integration, rather than pretending the historical terminal
receipt changed. Joe must authorize (a) the medic to perform that bounded
external read and (b) the exact rule by which its receipt counts as recovery;
the existing medic authority does not grant either power, so adding it without
that decision would be a new approval path.

## 3. The futon1b CPU/census observation

The quoted `census error ... HTTP connect timed out` is from the historical
per-type hyperedge migration, not a census currently executing inside the
futon1b JVM. `hx-backfill-per-type.bb` queries source `:7071` and destination
`:7073` censuses (`/home/joe/code/futon1b/hx-backfill-per-type.bb:5-35`), then
posts missing hyperedges (`hx-backfill-per-type.bb:36-74`). It was launched by
`full-backfill-job3.sh`, which starts futon1a and executes that Babashka script
(`/home/joe/code/futon1b/full-backfill-job3.sh:3-15`). The retained log says the
pass began `2026-07-12T22:13:51Z`; the timeout is under that pass. No
`full-backfill`, `hx-backfill`, Babashka, or census process was present during
this inspection.

The ~87% process is instead PID 2471981, the `futon1b-zone.service` server,
started `2026-10-28T23:37:43Z`, serving `migration-store-21` on `:7073`. Its
current journal contains ordinary concurrent evidence/hyperedge reads and
writes, and the service drop-in records why it was restarted with an 8 GiB
heap: old-gen saturation and roughly 10 million Arrow `Field`/`FieldType`
objects (`~/.config/systemd/user/futon1b-zone.service.d/heap.conf`). Therefore
the July census is not the present CPU load source. The evidence supports live
request load plus the already recorded XTDB/Arrow heap-growth problem; it does
not support attributing current CPU to an ingestion census.
