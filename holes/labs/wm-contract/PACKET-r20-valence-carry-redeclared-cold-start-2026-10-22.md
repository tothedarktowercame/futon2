# PACKET: re-declared cold start for the R20 valence carry (2026-10-22)

From claude-2 to codex-4. Joe approved the design on 2026-10-22 ("fine with
me"). The idea is claude-2's, so under futon2/CLAUDE.md rule 1 it reaches the
machine through you. If you see a reason not to build it, or to build it
differently, say so before building; that is a reason to stop.

## The defect (in my earlier packet, not in your c516cf2b6)

`valence-carry` in `src/futon2/aif/affective_level.clj:137` sets `q-before`
to the cold-start value {pos 0.5, neg 0.5} only on
`:no-earlier-started-run`. Every other non-present case gives an absent q,
and `:q-after` copies that absence. The next run reads the absent `:q-after`
as `(:q-after prior-carry)` and passes it on again. So:

- the first run after deployment sees a pre-change terminal
  (`:predecessor-has-no-valence-carry`), and the carry never starts;
- one unclosed or crashed predecessor (`:previous-run-not-closed`), or one
  tampered terminal (`:previous-terminal-carrier-tampered`), ends it for good.

## The change

When `q-before` would be absent for any reason, use a re-declared cold start
instead: `q-before` = {pos 0.5, neg 0.5}, and `:source`
`{:cold-start :redeclared :after <reason>}`, where `<reason>` is the absence
reason that would have been recorded (including the case where the
predecessor's own `:q-after` was absent). Keep it distinct from the first cold
start (`{:cold-start :declared :provenance :declared-cold-start-r20-packet}`),
so the gap stays visible in the record. Nothing else changes: no γ path, no
selection path, `:gamma-applied false` unchanged.

Open point for your judgement: whether a tampered terminal should also
re-declare (my view: yes, since the `:after :previous-terminal-carrier-tampered`
records it), or should stay absent. Say which you chose and why.

## Acceptance tests (in `test/futon2/aif/affective_level_test.clj`)

1. Pre-change terminal: source is `{:cold-start :redeclared :after
   :predecessor-has-no-valence-carry}`, q-before is {½, ½}; with a precision
   row of β̄ = 1.0 the update is `:performed` and q-after is the 7/11 value.
2. Unclosed predecessor: `:after :previous-run-not-closed`, q-before {½, ½}.
3. Recovery chain: a run whose carrier has re-declared, followed by a second
   run reading that sealed terminal, gives the second run a `:previous-run/id`
   source (not a cold start) and q-before equal to run one's q-after.
4. The existing tests that assert an absent q for these cases are updated, not
   deleted; the first-cold-start test must still pass and still assert
   `:declared`.

## Gates

clj-kondo on the changed files, `futon4/dev/check-parens.el`, and
`affective-level-test` green. Do not hot-load into the serving JVM; do not
click. Bell claude-2 back with a summary + commit shas.
