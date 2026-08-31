# C249 — resource evidence for the production click

Date: 2026-08-31. Owner: `wm-organization`. Status: design only; no producer
or certificate contract changed.

## Decision

The `run-tick-once` command-name requirement in
`checks/certify_live_run.clj` is an artifact of the only resource-receipt
producer that existed. It is not essential to the operational certificate's
claim.

The evidence is in the separation between the locator and the checker:

- `certify_live_run.clj/encloses-run?` uses the substring solely while finding
  a JSON receipt;
- its `normalized-resource` removes the command string before invoking
  `wm_operational_certificate.clj`;
- `wm_operational_certificate.clj/resource-clean?` checks resource status,
  run identity, `pids.events:max`, native-thread exhaustion, and execution
  outcome. It never reads an enclosing command;
- C97's authored design calls the input an **external resource-status
  receipt**, not a `run-tick-once` receipt.

Thus command spelling was a proxy for provenance: it prevented an unrelated
bounded test receipt whose timestamp happened to overlap the run from being
selected. Removing the substring without replacing that provenance would be
a weakening and is explicitly rejected.

## Required typed input

The production path needs a new resource-envelope type, not a relaxed
`futon-bounded-test-v1` match. A sufficient `wm-click-resource-v1` input must
carry:

1. exact `click-id`, `attempt-id`, and `run/id`, with the run ID obtained from
   the emitted `TickRunRecord`, not inferred from “latest”;
2. click accepted/start/terminal timestamps and the run start, with an
   executable enclosure check;
3. producer and observer identities, including serving JVM PID/start identity,
   service cgroup path, and the loaded Futon2 runner-code identity;
4. a typed terminal outcome for the in-process click. A click has no process
   exit code, so it must not counterfeit `command-exit=0`;
5. resource observations over the same interval: service-cgroup
   `pids.events:max` before/after, peak tasks if sampled, and native-thread
   exhaustion observations;
6. observation scope, explicitly `:shared-serving-jvm`, and a status vocabulary
   that distinguishes `:clean`, `:dirty`, and `:unavailable`;
7. atomic persistence only after both the terminal click result and exact run
   identity are known. A missing, partial, ambiguous, or mismatched envelope
   is refusal, not absence-as-clean.

`certify_live_run.clj` should then dispatch by receipt schema and require the
appropriate correlation predicate. The diagnostic schema may retain its
command check. The click schema must use IDs and lifecycle boundaries rather
than command text. Both normalize into an explicitly versioned operational
resource input; the normalizer must preserve `:source-schema` and observation
scope.

## What the long-lived JVM can honestly produce

The serving JVM can record the authoritative click lifecycle, exact IDs,
loaded-code identity, its own PID/start identity, uncaught click failure, and
snapshots of the service cgroup's counters. It can also arrange sampling of
task count and capture resource-warning events observable by its logging
boundary.

It cannot honestly claim process isolation, an outer process exit, or causal
attribution of a shared-cgroup event to this click. Other Agency work shares
the JVM and service cgroup. Therefore:

- a positive envelope means no observed serving-substrate exhaustion during
  the click interval, not that the click owned an isolated budget;
- any cgroup max event in the interval conservatively makes the envelope
  dirty even when another activity may have caused it;
- counters alone cannot prove absence of every JVM-local failure, so the
  terminal lifecycle and native-thread observations remain separate required
  fields;
- an in-JVM record is in the same failure domain as the click. For the
  certificate's existing **external** evidence claim, a small observer outside
  the serving JVM should read the click's start/terminal projection and the
  service cgroup, then atomically seal the envelope after correlating the
  emitted run ID. The JVM supplies authoritative lifecycle facts; the observer
  supplies independent resource observation.

A purely in-JVM producer would be useful instrumentation, but it would change
the certificate from “external resource receipt” to self-attestation. That is
a material contract change and is not recommended merely to make step 7 pass.

## Controls required before implementation can qualify

- an unrelated resource interval overlapping the run is rejected;
- matching time but wrong click ID or run ID is rejected;
- matching IDs but a nonterminal click is rejected;
- a max-event delta or native-thread exhaustion produces a failing
  certificate even if the click reports success;
- an unavailable observer or partial envelope is rejected loudly;
- a clean, externally observed envelope with exact IDs and enclosure can pass
  without containing the text `run-tick-once`.

The last control proves that command spelling has been replaced by stronger
identity, not simply deleted. Until such a producer and controls exist, C247's
step 7 remains `STOP`; no existing receipt may be substituted.
