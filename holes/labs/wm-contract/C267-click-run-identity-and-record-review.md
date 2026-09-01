# C267 — click/run identity and run-record adversarial review

Date: 2026-09-01

Scope: focused review only. No production click, implementation change, paper
edit, binding restamp, or certificate change was made.

## Findings

### 1. One identity fact currently has two answers

`futon3c/src/futon3c/wm/runner_service.clj:182-218` derives
`:run-record-status` from the readable record and its run/click identities, but
derives `:run-id-status` independently from the runner-returned string. Hence a
record can say both:

```clojure
{:run-id-status :present
 :run-record-status :identity-mismatch
 :run-record-absence :run-record-identity-mismatch}
```

The same split permits `:run-id-status :present` with
`:run-record-status :unavailable`. `:absent` is not necessarily contradictory:
the producer may have returned an ID while returning no record. The present
field is nevertheless unsafe for a consumer that reads it as *verified
identity* rather than *producer supplied a nonblank token*.

This is a family of one class-9 shape across two failure statuses
(`:identity-mismatch`, `:unavailable`), not two unrelated defects.

**Recommended semantics (not implemented):** retain the observation as a
producer-returned ID, but do not call its status simply `:present`. Use one
authoritative compound binding verdict, for example
`:binding-status :verified|:absent|:unavailable|:identity-mismatch|:duplicate`,
and expose the raw token separately as an observation. Certificate consumers
must require `:verified`. Erasing the returned ID on mismatch would discard the
datum needed to diagnose the mismatch; treating `:present` as sufficient would
certify the contradiction.

Adjacent status audit:

- `result-summary` copies both statuses independently
  (`runner_service.clj:274-296`), so the contradiction survives into public
  terminal status.
- `:run-record-status :absent` plus a producer-returned ID can be an honest
  partial result, provided consumers do not equate token presence with a valid
  binding.
- The certificate reports both resource identity matching and resource status,
  but its pass predicate uses `resource-clean?`, which includes identity
  matching. Those two fields do not presently contradict each other.
- A second class-9-shaped ambiguity exists at the run/certificate boundary:
  resource `:status :clean` may coexist with `:execution-outcome :incomplete`,
  and the certificate calls `:resource-status-clean? true`. Here “clean” means
  resource health, not run completion, but a partial reader can reasonably read
  it as successful execution. The names need an explicit scope even if the
  underlying facts are compatible.

### 2. Duplicate run IDs are producer-capable, not stale binding retention

C262's isolated public-worker fixture ran two sequential clicks whose runner
returned the same explicit run ID. Both click-specific binding files contained
that same ID with `:run-id-status :present`. The runner boundary itself accepts
an explicitly supplied ID (`full_loop_runner.clj:3163-3165`); the binding code
does not allocate or enforce uniqueness. Thus duplicate identity can genuinely
originate at the producer boundary. It is not explained by the second click
retaining the first click's binding: targets are click-specific and both were
newly written.

`checks/certify_live_run.clj` refuses an ambiguous lookup when multiple run
records with the requested ID exist, but that downstream refusal does not make
the producer ID unique and does not protect a consumer that sees only one of
the duplicate records. Duplicate detection therefore belongs in the compound
binding verdict (or at the ID allocator), before certification.

### 3. A partial failed run can produce a record that certifies as complete

`persist-run-record!` writes a run record whenever the observed route is merely
nonempty (`full_loop_runner.clj:230-253`). The record retains neither the
result's `:outcome` nor a completion/failure status; `:traceWritten` is only a
boolean. `run-opportunity!` invokes it after incomplete initialization results
as well as successful results (`full_loop_runner.clj:3163-3263`).

An isolated fixture used:

```clojure
;; run
{:run/id "partial-failed-run"
 :click/id "partial-click"
 :startedAt "2026-09-01T00:00:00Z"
 :selectorSeam "live:validated-selection"
 :traceWritten false
 :route [{:fromNode "R1" :toNode "R4"
          :via "partial-before-failure"
          :at_ "2026-09-01T00:00:01Z"}]}

;; resource observation
{:schema 2 :run/id "partial-failed-run"
 :source-schema :wm-click-resource-v1
 :observation-scope :shared-serving-jvm
 :status :clean :execution-outcome :incomplete
 :pids-events-max-delta 0 :native-thread-exhaustion false
 :tasks-peak 1 :source-receipt "/tmp/c267-fixture"}
```

Canonical focused invocation:

```sh
bb -cp . checks/wm_operational_certificate.clj \
  --run /tmp/c267-partial-run.edn \
  --resource /tmp/c267-partial-resource.edn \
  --certificate /tmp/c267-partial-cert.edn
```

Observed: exit **0**, `wm-operational-certificate: PASS`, total hops 1,
measured hops 0, `traceWritten false`, resource execution outcome
`:incomplete`. The pass predicate requires only a nonempty declared route,
timestamp, topology pins, no undeclared hop, and resource health
(`wm_operational_certificate.clj:56-110`). It neither consumes
`:traceWritten` nor requires successful completion. Moreover,
`resource-clean?` rejects nil/unknown/service-failed but accepts `:incomplete`
(`wm_operational_certificate.clj:27-38`).

This is a failing acceptance result: the topology certificate can certify a
partial failed run. The missing TRACE hop is visible only among declared hops
not exercised and is non-failing. A failing certificate, rather than a missing
record, is required for a partial run that presents a route.

## Scope conclusion

The click/run/certificate path has at least three related but distinct defects:

1. raw ID presence is conflated with verified binding identity;
2. uniqueness is not enforced at the producer/binding boundary; and
3. route conformance is conflated with completed-run conformance.

The first repeats across mismatch and unavailable record states and merits a
single structural check: no downstream certificate input may treat a raw
producer token as verified identity. The third requires a separate certificate
negative control because field-consistency linting cannot detect an omitted
completion claim.

