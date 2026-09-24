# NOTES — P2 production rules from claude-1's turn interpretations (live set)

Author: kimi-2, 2026-09-24. Input: `/home/joe/.emacs-graph/session-turn-analysis/*.analysis.json`
(145 records; the 37 carrying `pattern_refs` were worked). Read-only inputs;
no clicks; no shared-JVM loads.

## Counts

| measure | count |
|---|---|
| turns processed (files written) | 37 |
| cited pattern_refs (level-1 successes) | 60 |
| rules written | 60 |
| not-a-rule | **0** (see below) |
| distinct library patterns cited | 34 |
| tokens minted | 104 |
| token-set memberships (needs + forbids + produces) | 117 |
| tokens appearing in 2+ memberships (the chaining edges) | 14 |
| tokens minted but unused by any rule | 1 — `turn-4TqhsT/victory-lap-rendering-requested`, minted to document the turn's actual request (the LaTeXML rendering), which its consent-gate citation does not cover; retained as vocabulary, not silently dropped |

Every rule's pattern file exists, its `@flexiarg` line matches, and the
receipt sha matches the file now (gate below). All 34 distinct patterns
resolved; none was written under `:not-a-rule`.

## The not-a-rule count of zero, stated plainly

I expected a nonzero count — stance patterns, approvals, jokes. Two
candidates were argued down, and the arguments are recorded so a reviewer
can reverse them:

- **turn-v2qr9R (`象/言即行`, "is that effective or affective?")** — the
  weakest rule in the set. The fragment challenges a classification; no
  artefact changes. It is written as a rule (forbids the dichotomy,
  produces a typed-by-illocution reading) on the ground that the *analysis
  vocabulary* is this turn's work — but its `:scope-limit` says so, and a
  reviewer demoting it to `:not-a-rule :stance` loses nothing else.
- **The six bare approvals (turns 2yHv3v, 4TqhsT, 9umMUN, L2e5uG, NXkQxt,
  and the approve fragment of A8Ioc3)** — all cited
  `orchestration/consent-gate`. These are consent *steps*, not the full
  gate: the held action and any warrant are off-record (each rule's
  `:scope-limit` says exactly that, and the rules have empty `:needs`
  because no cue for the held proposal exists in the source text — minting
  one without a cue would have violated the token rule). Kept as rules
  because "the held proposal is released" is a real state edge; a reviewer
  who reads consent-gate as requiring the warrant could demote all six to
  one not-a-rule class `:consent-step-not-full-gate`.

## Level-1 citations I disagreed with (noted, not edited)

1. **Consent-gate on bare "OK" (turns 4TqhsT, NXkQxt, L2e5uG).** The
   pattern's consent step is *recorded operator approval at the locus where
   an action would otherwise fire*. A bare "OK" at a conversation boundary
   fits the approval half but nothing shows the agent was holding an
   action at a gate. The citation is defensible only in its weakened
   reading, and the rule's scope-limit carries that. (Disagreement of
   degree, not of kind.)
2. **turn-9cqFTA `lift-when-three-align`.** The rationale says "the
   admission test is met here" — but the three "instances" are two systems
   plus a *sentence asserting the analogy*. Lift's test wants three
   written cascades with incidents; an assertion of alignment is not an
   aligned cascade. The rule I wrote keeps the citation but narrows it:
   `:scope-limit` records that the gap (which artifact takes the pressure)
   stands. If demoted, this is the clearest not-a-rule in the set after
   v2qr9R.
3. **turn-Umq4TE `hand-over-when-acting-is-worth-more`.** Cited on "OK
   I'll follow your recommendation" — the recommendation's content is in
   the preceding turn, so the handover's budget/value terms are
   unwitnessed in the record. Kept with empty `:needs` and a scope-limit;
   the labeller's own target text names the triage, which is more than
   the record supports.
4. Labellers: claude-1 (19), kimi-1 (17), codex-14 (1). The same-turn
   same-span duplicate-fragment shape (a fragment carrying refs plus an
   empty twin) occurs at least in turns L21yiL and Umq4TE — the input
   schema tolerates it, but any downstream consumer that keys fragments
   by span will see only one. Noted for claude-1's pipeline; my emitter
   merges refs across same-span twins.

## Token-vocabulary notes

- Tokens are propositions about the *work*, per the brief: e.g.
  `:turn-A8Ioc3/robust-learning-signal-captured`, never
  `:turn-X/joe-said`. The one borderline family is the consent releases
  ("proposal released for execution") — about the work's state, not the
  speech act.
- Reuse (14 tokens in 2+ memberships) is what makes chains: e.g. in
  turn-A8Ioc3 the three rules chain
  `three-llm-chain-in-use → robust-learning-signal-captured →
  why-how-encoded-as-links`; in turn-Pb9TMC,
  `typed-holes-recorded → holes-dispatched-to-agent` is produced by two
  rules with different warrants (routing vs handover) — the same state
  reached two ways, which is exactly what a semilattice admits.
- `relations` guidance was followed with two stated departures:
  `contrast`-labelled fragments became `:forbids` (1QzzO5, A8Ioc3's
  permanent-dependence) — and in 6TxxZn the baroque-language token is a
  `:forbids` even though no relation named it, because the conservative
  rule's content is precisely its exclusion.

## Gate output

Script: `gate.clj` (this directory). Run from `/home/joe/code/futon2`:

```
$ clojure -M holes/labs/wm-contract/E-cascade-real/rules-from-turns/live/gate.clj
gate: 37 EDN files checked
gate: OK — all checks pass
```

`clj-kondo --lint gate.clj`: 0 errors, 0 warnings.
`check-parens.el gate.clj`: OK.

The gate checks: EDN readability and schema; pattern file existence +
`@flexiarg` line + receipt sha against current bytes; cue spans in range
of the source record's `source_text` and non-blank; every guard/produces
token defined; no empty `:produces`.
