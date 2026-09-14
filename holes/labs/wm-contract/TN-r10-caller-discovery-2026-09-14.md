# TN: R10 production-caller discovery (2026-09-14)

Status: **READ-ONLY DISCOVERY.** No caller, schema, configuration, or JVM was
changed.  The result is a typed prerequisite finding: the serving HTTP click is
the right eventual caller boundary, but wiring is not yet honest because no R10
commission authority exists there and the current dispatch-then-record ordering
can report refusal after an asynchronous click has already started.

## Authority and pins

The DAG calls `:r10-caller` built-not-wired and makes it a hard prerequisite of
`:r10-record` (`runs/outstanding-dag-2026-09-14/dag.edn:99-100,183`).  The credit
bar is integration through `run-scheduled-dispatch!`, not mechanism-only evidence
(`TN-row24-scoping-2026-09-12.md:214-216`; `TN-row17-discovery-2026-09-12.md:101-105`).

| Repository/file | Lines used | SHA-256 |
|---|---:|---|
| futon2 `runs/outstanding-dag-2026-09-14/dag.edn` | 99-100, 183 | `0883f2336d1f12f882a72d17ed65e172017f4b81b6685d23dfeb109276a9d061` |
| futon2 `TN-row24-scoping-2026-09-12.md` | 205-219 | `052ba8a060cf158bf04267b90c1d0839ec068f085ea5f28f4e0385818b9b577d` |
| futon2 `TN-row17-discovery-2026-09-12.md` | 95-105 | `de84557a43c49d1bf9a1a8d8afccff75256fc196baaa7a1c36f71075d2f886de` |
| futon3c `social/coordination_ledger.clj` | 84-136 | `faef08569048de0637bd0e671859d4a612283323ee300e777177b87f07d539c8` |
| futon3c `social/coordination_ledger_test.clj` | 57-90 | `11050400eb32edfe44dcebdcc522b939d12785a0b9c44e73c7e7f7837e5e620d` |
| futon3c `transport/http.clj` | 8645-8697 | `d44028388a0a163fcbf2885c29dc7ea3e482fcbf018568fca40346f853cf50ef` |
| futon3c `wm/runner_service.clj` | 412-431, 451-492 | `17210a48730ab71bb6b035d8975765ddf0e00576470ca1f6577d58dd21d54e40` |
| futon3c `evidence/boundary.clj` | 269-410 | `7262eec5563cba66e7e1953b6b8fc89dbd38ca45f6f38da0fadcfce28eac673f` |
| futon2 `aif/on_demand_entrypoint.clj` | 68-105 | `a6c193d83e395ece7d61b87148ca21a135db3105c94a2b9fde8a1e219f7cdbf5` |
| futon2 `aif/full_loop_cli.clj` | 35-72, 647-668 | `3df96899eb860e1b79687262d86764a740927ba7dfcfbee76ecee2095bf72fc3` |
| futon2 `deps.edn` | 13-21 | `9389f9d24fa83a0c7c02f36419ec8870b3b7595270d8d0be74838f72eb4e0960` |
| futon2 `scripts/wm_full_loop_cron.sh` | 1-17 | `8f9f35e84badb4b43365eba97ddd204886f2d0a497dbcb3b72a67bcc853eed05` |

Repository pins inspected: futon2 `602ff8361dc2c8607956c4981a65ed9db21b9eee`;
futon3c `150d260865d7a3fbb63496a6b081eb576b8dc912`.

## 1. Actual WM start surfaces

| Surface | Production status | Commission at boundary? | Dispatch function/receipt? | Verdict |
|---|---|---|---|---|
| `POST /api/alpha/wm/click` | Current authoritative on-demand path. Futon2 posts `run-id`, personnel and trigger (`on_demand_entrypoint.clj:80-98`); Futon3c parses those and calls `runner-service/click!` (`http.clj:8645-8691`). | **No.** Neither request nor `legacy-opts` contains a commission. Optional RUN4 admission is conditional and is not a generic R10 commission (`http.clj:8666-8677`). A run id must not be silently renamed. | `click!` is the callable seam, but returns only click id/start time and creates the click id internally (`runner_service.clj:451-483`), not the required R10 echo. | Best eventual caller, blocked on commission genesis, receipt adapter, and ordering below. |
| `clojure -M:wm-scheduled` / cron wrapper | Alias is the authoritative real-actuation scheduled CLI (`deps.edn:13-15`); wrapper only locks then execs it (`wm_full_loop_cron.sh:4-17`). No installed crontab was present when `crontab -l` was read. | **No.** CLI options contain trigger/personnel/budget, not commission (`full_loop_cli.clj:35-48`). | `run-once!` calls `run-opportunity!` directly and returns its terminal result (`:60-72`); there is no R10 dispatch receipt. | Designed scheduled surface but not a currently installed live caller. Cross-repo ledger access/commission acquisition would also be new plumbing. |
| `full-loop-cli once/tick` direct | Operator/diagnostic entry; `once` and `tick` directly call the same runner (`full_loop_cli.clj:647-668`). The old runner process is explicitly retired for clicks (`deps.edn:16-20`). | No. | No separate dispatch function or linked receipt. | Not the canonical live click seam; do not wire merely because it is easy to call. |

A source census found no other futon2/futon3c production reference to
`run-scheduled-dispatch!`; the sole source definition and its test are the only
matches. No futon3c cron/scheduler starts WM: the WM start route resolves only to
the HTTP handler and runner service above. Unrelated portfolio/APM schedulers are
not R10 callers.

## 2. Semantics of the proposed seam

The boundary is not observational wrapping. It (1) validates commission/function,
(2) injects `:node :R10`, (3) calls the function synchronously, (4) validates the
returned node/commission/dispatch identities, and only then (5) appends and reads
back evidence (`coordination_ledger.clj:92-135`; durable readback contract at
`evidence/boundary.clj:269-310,369-410`). Therefore:

* invalid commission blocks before dispatch, intentionally;
* malformed/unlinked receipt refuses after the dispatch function returned;
* recording failure also refuses **after dispatch**.

Wrapping today's `click!` directly would allow its daemon thread to start at
`runner_service.clj:474-483` and then return an HTTP error if evidence persistence
fails. A retry could create another click. Thus the smallest valid parcel cannot
truthfully say it “only records” dispatch. Stop-the-line refusal is the declared
R10 behavior, but its present ordering is unsafe for this asynchronous boundary
without a pre-dispatch durable reservation/idempotency decision or an explicitly
recoverable two-phase intent/completion protocol.

## 3. Hidden prerequisites

1. **Commission genesis and authority (missing).** A commission must exist before
   dispatch, with issuer/source and exact bytes. Current request, CLI, and service
   do not provide one. Server-minting an unchecked UUID would satisfy shape but
   not assurance authority.
2. **Uniqueness/idempotency (missing).** The mechanism checks only a nonblank id;
   it neither reserves nor rejects a prior commission. `click!`'s CAS is only a
   process-local single-flight guard (`runner_service.clj:458-477`), not durable
   cross-restart/cohort uniqueness.
3. **Dispatch identity ruling (missing).** The click id is born inside `click!`.
   A reviewed adapter must establish whether that exact id is the R10 dispatch id
   and echo it with the pre-existing commission; it may not invent a parallel id.
4. **Atomicity/recovery policy (missing).** Evidence must be reservable before the
   irreversible thread start, or replay must be proven idempotent. The current
   function provides neither.
5. **Store authority (must be explicit).** `boundary/append!` verifies readback,
   but the caller must supply the serving process's configured durable backend;
   falling back to a test/default atom is not production evidence.
6. **Schema alignment (needs a ruling).** The generic ledger validates only three
   receipt identities; later R10 evidence needs an exact commission/dispatch
   schema and joins to click/run identity. This is not license to manufacture the
   broader bounded-tick route.

Consequently wiring is **not currently possible as a caller-only edit**. These are
real prerequisites, not reasons to weaken R10.

## 4. Refusal controls

The implementation exposes `:r10/invalid-commission`,
`:r10/unlinked-dispatch-receipt`, and `:r10/recording-failed`
(`coordination_ledger.clj:99-113,128-134`). Existing tests exercise a positive
record and two unlinked shapes (map without echoes and nil) at
`coordination_ledger_test.clj:57-90`. They do **not** exercise invalid commission
or recording failure. The wiring parcel needs hermetic controls for both, plus an
integration-level unlinked adapter control and, because dispatch precedes record,
an assertion about whether the dispatch side effect occurred. Recording-failure
tests must not start a real click.

## 5. Retained record for `r10-record`

The authoritative record should remain the complete EvidenceEntry written by the
production durable evidence backend: subject `{:ref/type :task :ref/id
<commission-id>}`, tags `[:coordination :scheduled-dispatch :R10]`, body containing
the complete linked commission and dispatch receipt, and session id equal to the
dispatch id (`coordination_ledger.clj:114-127`). After a real caller exists, the
evidence acquisition packet should read that exact entry back and retain an
immutable dated EDN artifact plus SHA-256, alongside the exact commission source,
HTTP click receipt, click/run binding, and terminal tick record. A mutable store
query or a trace-only inference is not the by-record claim.

## Smallest valid next packet

**Prerequisite packet, not wiring:** define and independently review a versioned
R10 click-commission/dispatch identity contract at the HTTP boundary, backed by a
durable pre-dispatch uniqueness reservation and explicit recovery semantics. Its
negative controls must prove duplicate commission, unavailable durable store, and
malformed receipt refuse without starting `click!`. Once that is accepted, a
separate one-behavior wiring packet may adapt `handle-wm-click-start`'s call to
`click!` through `run-scheduled-dispatch!`, use the server-issued click id as the
declared dispatch id, and retain/read back the linked entry. Only the subsequent
real invocation can produce `r10-record` evidence.
