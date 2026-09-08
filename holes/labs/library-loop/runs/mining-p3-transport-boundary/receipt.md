# Receipt: mining-p3-transport-boundary (M5, cascade 5 of 6)

Target: the 5-th mapped item of runs/W1-witness-survey.md's mapped set in
survey order — **P3** (`runs/W1-witness-survey.md:183-191`): "transport code
existed without an external request/frame boundary", solved by futon3c
`M-transport-adapters` (archived; four parts Complete, closed 06dac238
2026-02-13 with 72 tests / 207 assertions).

## Node selection — six of thirteen, and why the other seven are not nodes

The mission cross-references thirteen patterns. Only SIX are nodes here,
because a node must be a pattern actually involved in the four COMPLETED
parts. Each is named in the mission's plan (cross-ref tables, committed
3dc50a81 2026-02-11, pattern and spec authored together) and re-attested by
the closing implementation: five through code docstrings and call sites, and
loop-failure-signals through typed transport-error call sites in ws.clj
(:123, :164, :181):

connection-state-machine, rendezvous-handshake, request-param-resilience,
loop-failure-signals, structured-events-only, verify-after-start.

The seven NOT selected, each with its reason in the cascade's
`:admission-note` (CORRECTED on reopen 2026-09-08): liveness-heartbeats and
transport-pivot are marked "(future, not Part I-IV)" by the mission itself;
reconnect-with-backoff is deferred as the client's concern; listener-leases is
"(Part III cleanup)" and grep over ws.clj + ws/ for lease/TTL finds no lease
code; authoritative-transcript and single-line-transport name constraints,
not implemented mechanisms, with no committed witness of involvement; and
SINGLE-AUTHORITY-REGISTRATION moved OUT of the node set on reopen -- although
ws.clj:21's docstring names it, at the closing commit 06dac238 !connections
is keyed by CHANNEL, the only guard is the unknown-channel defense (:123),
and no code rejects or evicts a second connection with the same agent-id (the
pinned register! path still replaces via assoc). Loop-failure-signals moved
IN: the closing ws.clj implements typed transport errors (:unknown-connection
:123, :not-ready :164, :invalid-frame :181), which the mission's :48 row maps
to this pattern. The pattern's rule is classed :documented, mirroring M1's
treatment: the error-frame reading is the mission's, while the THEN's own
terms are loop-overload flagging.

## Format notes (restated statement)

- Edges: four, all `:interpretation-class :documented` with `:documentation`
  bases — the five futon3c patterns carry `@references`, not `@why/@how`
  (grep at pin f3534b93), so the readings are authored by mission spans and
  code docstrings, the ratified exemplar shape, class-marked.
- Rules: six, all `:attested` (mission rows + exit conditions + code
  docstrings attest each reading). One honest split is stated in-band: the
  rendezvous-handshake attestation covers the mission's NARROWER "readiness
  handshake before dispatch", not the THEN's full signed-hello-with-
  capabilities form — noted in the rule's `:span-note`, not smoothed.
- THEN spans: content lines only at the CURRENT pins — futon3c patterns
  marker :15 / content :16; futon3 patterns (rendezvous-handshake,
  structured-events-only) marker :18 / content :19.
- `:transcript`: 3dc50a81 2026-02-11 (spec + patterns together) → 06dac238
  2026-02-13 (close, 72/207) → 5f18bc2f 2026-05-03 (archive); later transport
  commits in the same files postdate the archive and are excluded.

## Gate reproduction

```sh
bb -e '(clojure.edn/read-string (slurp "cascade.edn"))'    # exit 0
grep -n "+ THEN:" <six flexiarg files>                      # 15/15/15/15 futon3c, 18/18 futon3
git log --format="%h %ad %s" --date=short --follow -- holes/missions/M-transport-adapters.md
```

Trail status: complete commands, no truncation; all shas/dates copied from
git output this invocation (the M1 lesson, applied).
