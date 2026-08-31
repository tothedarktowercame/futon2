# C253 — production click envelope: identity port absent

Date: 2026-08-31. Owner: `wm-organization`. Outcome: honest stop before
producer implementation.

## Finding

An external observer can measure the serving cgroup across a click, but the
current production click cannot supply the exact `run/id` required to make
that observation certifiable.

The missing value is not merely absent from the HTTP response:

- `POST /api/alpha/wm/click` returns the click service's `click-id` and
  `started-at`;
- `runner_service.clj/result-summary` retains only `attempt-id` and `outcome`;
- `futon2.aif.full-loop-runner/run-opportunity!` returns attempt/delivery
  results and does not write a `TickRunRecord` or return its `:run/id`;
- repository search finds the `TickRunRecord` writer only in the retired
  diagnostic `scripts/futon2/run_tick_once.clj` path;
- consequently neither the click status nor an emitted record supplies a
  join from `click-id` to the certificate's `run/id`.

Temporal proximity is not an identity port. Selecting a record created during
the observed interval would recreate the C227 defect: a healthy envelope that
cannot prove which run it describes.

## Why the C230 rehearsal did not expose this

`futon3c.wm.chain-rehearsal-test` substitutes a fixture runner whose body
copies the committed diagnostic `tick-run-record-2026-08-31.edn` to a temporary
path. After the click, the test reads that copied file and constructs
`(resource run-id)` directly. The click service never transports that run ID,
and no external observer produces the resource.

Thus the rehearsal proves reload, HTTP handler, lifecycle completion, topology
certificate, and mismatched-resource rejection. It does **not** prove the
production click-to-run identity seam or an external execution envelope. The
fixture is still useful, but its boundary must not be described more broadly.

## Required upstream port before the observer can qualify

The click runner must produce a durable run record whose schema includes both
`:run/id` and the accepting `:click/id`, and return those same identifiers at
its terminal boundary. The click service must preserve them in `last-result`.
The external observer can then:

1. record cgroup counters before submitting the click;
2. bind the accepted `click-id` from the POST response;
3. observe the terminal status carrying the same `click-id` and exact
   `run/id`;
4. read the named run record and require both IDs to agree;
5. atomically write `wm-click-resource-v1` with shared scope, interval,
   counters, terminal outcome, and loaded-code identity.

Only after that port exists can the C249 falsifiers be meaningful:
non-enclosure, wrong run ID, and unavailable resource status. Building the
observer first would either make every real click `UNAVAILABLE` or tempt a
timestamp guess; neither unblocks certification.

## Step 7 status

C247 step 7 remains `STOP`. What remains is now exact: production needs a
click-bound `TickRunRecord` identity port, followed by the external observer
and the schema-specific matching controls. No receipt was fabricated or
substituted, and `certify_live_run.clj` was not relaxed.
