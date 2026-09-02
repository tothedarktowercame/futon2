# U12 / R9 finding: worker verdicts are not refused

Finding: `:u12/worker-verdict-not-refused`.

The current futon3c tree has no checked-handoff verdict-event ingress and no
type check comparing a verdict event's author with its worker seat. Therefore
there is no existing refusal path for U12 to pin. This packet does not add one.

## Search trail and executed probes

Run from `/home/joe/code/futon3c` at the current checkout:

```sh
rg -n -i 'checked.?handoff|handoff.?adjud|adjudicat.*handoff|handoff.*verdict|verdict.*handoff' src test scripts
rg -n -i 'verdict' src/futon3c/agents test/futon3c/agents src/futon3c/wm test/futon3c/wm scripts/zaif\*
rg -l ':verdict' src --glob '*.clj' | sort
rg -n 'assoc[^\n]*:verdict|:verdict\s+\(|:verdict\s+:[a-zA-Z]' src --glob '*.clj' | sort
rg -n -i 'worker.*verdict|verdict.*worker|author.*verdict|verdict.*author' src test scripts --glob '*.clj'
```

The first and last probes found no checked-handoff verdict path and no
worker-author refusal. The broad probes found the following sites relevant to
adjudication or persisted verdict-bearing observations.

## Verdict mint/copy sites and authors

- `src/futon3c/agents/tickle_orchestrate.clj:309-317` parses a review response
  into `:approve`, `:request-changes`, or `:unclear`.
  `request-review!` invokes the fixed reviewer seat `claude-1` at lines 371-397
  and writes the `:review-complete` observation at lines 398-410. The evidence
  emitter, rather than an event supplied by the worker, chooses the record
  author. There is no worker-authored verdict ingress to refuse.
- `src/futon3c/agents/tickle_orchestrate.clj:491-505` copies that parsed review
  verdict into the workflow summary and completion observation. The
  orchestrator authors the record; no author/worker comparison is performed.
- `src/futon3c/agents/tickle_work_queue.clj:257-283` conditionally copies a
  supplied `verdict` into CT extraction evidence. The record author is fixed to
  `"tickle-1"` at line 273. It neither records the verdict-producing reviewer
  as author nor rejects a worker-originated value.
- `src/futon3c/agents/arse_work_queue.clj:154-179` does the same for ArSE
  generation evidence, also forcing author `"tickle-1"` at line 169 and doing
  no producer/worker check.
- `src/futon3c/wm/outing.clj:81-85,87-113` computes a gate verdict directly
  from G1/G2/G3 results. This is an external machine computation, but it is a
  returned map, not an authored verdict event, and exposes no refusal ingress.
- `src/futon3c/apm/promotion_pipeline.clj:119-131` mechanically mints rejection
  reviews under `mechanical-reviewer`; lines 305-316 copy independently
  supplied review verdicts into promotion dispositions. These are promotion
  review records rather than checked handoffs. Validation constrains verdict
  vocabulary and review shape, not equality between event author and worker.
- `src/futon3c/apm/promotion_review_store.clj:48-58,168-176` materializes and
  persists reviewer-provided verdict fields. The reviewer identity comes from
  the review payload; this store does not know a worker seat and cannot enforce
  R9.
- `src/futon3c/peripheral/memory_lifecycle.clj:364-381` creates a mechanical
  `:approve` promotion-adjudication result. It is system-authored lifecycle
  output, not a checked-handoff event.

Other broad-search hits (`marks`, inbox-zero, diagram-prover, ftriangle smoke,
calibration, campaign trace, and HTTP projection) either classify unrelated
domain objects or project/copy an existing `:verdict`; none is a checked
worker handoff or an R9 authority boundary.

## Direct negative probe

Inspection of `tickle_orchestrate/request-review!` shows the worker result is
passed as prompt material, while the verdict is parsed only from the separate
`claude-1` invocation. That separation is useful behavior, but there is no
function to which a forged worker-authored verdict event can be submitted.
Consequently the required pair of probes (worker-authored refused,
adjudicator-authored accepted) cannot be expressed against production code
today. Treating absence of an ingress as a passing refusal test would
manufacture the R9 guarantee.

Required follow-up is a separate build packet: define a typed checked-handoff
event carrying worker seat, author seat, proposal, and adjudicator rerun
witness; make its validator return a typed error when author equals worker;
only then add the strict paired test. R16 execution witnesses must remain input
to adjudication, not authority for the verdict.

## Reviewer addendum (claude-2, 2026-09-02 ~18:55)

Adversarial spot-check that STRENGTHENS the finding: the APM receipts carry a
field literally named `:receipt/independent-review?` — and it is hardcoded
`true` at every site that writes it (futon3c
`live_learning_phases.clj:618,634,1250,1339`, `learning_loop_dry_run.clj:43`).
It is a self-asserted boolean, never computed from an author-vs-worker
comparison and never validated against seats. So the codebase does not merely
lack an R9 refusal ingress; in one place it asserts the R9 property as a
constant. The `tickle_work_queue.clj:273` forced-author claim verified
verbatim. Follow-up build minted as board row :U14.
