# P2 — rules-from-turns/hist: production rules from claude-1's level-1 citations

E-cascade-real packet P2 (kimi-3, 2026-09-24). Input: the HISTORICAL set,
`/home/joe/code/storage/operator-turns/batches/2026-08-22_2026-09-21_block000/`
(98 analysis records; only fragments carrying ≥1 `pattern_refs` entry were
worked). Output: 25 `<turn>.edn` files, schema `:e-cascade-real/turn-rules-v1`.

## Counts

| measure | n |
|---|---|
| turns with level-1 citations worked | 25 |
| cited (fragment, pattern) refs | 45 |
| rules written | 45 |
| not-a-rule recordings | 0 |
| tokens minted | 70 |
| tokens used by 2+ rules (chain links) | 8 |
| distinct patterns used | 28 |

The 8 reused tokens (the chains the reuse rule exists for):
`turn-117/title-states-the-problem` (propose-title → justify-by-dual-readership),
`turn-118/mlk-quote-bridges-wwii-to-paper`, `-118/war-machine-tension-staged`,
`-118/tension-is-central-design-pattern`, `-118/organise-effectively-without-state-apparatus`,
`-118/ruling-named-wr-0` (the WR-0 turn chains five rules end to end),
`turn-120/wr-0-formally-written-up` (writeup → registry),
`turn-14-turn-24/rulings-made-in-any-case` (extend-taxonomy ↔ unify-phase).

## not-a-rule reasons (grouped)

None. All 45 citations could be written as production rules over per-turn work
state without forcing. The closest calls — and why they were kept as rules
rather than discarded:

- **Stance-like patterns exercised as explicit record acts.**
  `agent/pause-is-not-failure` (turn-129) and
  `peripherals/progress-heartbeat-distinct-from-cycle-completion` (turn-127,
  codex-223) name stances or distinctions, not acts. They were kept because
  the fragment PERFORMS them on the record: tolerating R10's unclarity,
  reporting heartbeat and completion as two separate facts. The produced
  tokens are states of the work/record, and each rule's `:scope-limit` says
  the act changes the record, not the world.
- **`war-room/wr-0` cited ten times in one turn (turn-118).** All ten were
  kept because the turn is the pattern's own birth: each fragment lands a
  different part of the ruling (bridge, tension, IF+HOWEVER, THEN (a), THEN
  (b), name), and the rules chain through the reused tokens.

## Level-1 citations I disagreed with (noted, not edited)

1. **claude-14-turn-16 → `war-room/wr-25-good-news-gets-the-same-evidence-discipline-as-bad`.**
   The fragment ("Before using scary words like 'missing' please try simple
   things like this:") is about ALARMING claims, not good news. wr-25's
   conclusion letter is good-news-specific ("Let unanchored good news change
   ordering immediately but leave numerical posteriors unchanged until an
   artifact supports it"). The citation survives only through the pattern's
   title — the same evidence discipline applied to bad news — not through its
   conclusion's exact case. The rule is written with that caveat in its
   `:reading`. A cleaner citation would be a verify-before-alarming pattern;
   none was named in the record and I did not mint one.
2. **claude-14-turn-2 → `writing-coherence/scope-mush`.** The fragment does
   the OPPOSITE of mush ("let's be clear … the primary reason"): it is the
   remedy, not the defect. Defensible (patterns cover remedies), but the
   intent label "clarify" plus relations ["rationale"] gives no surface cue
   that a *remedy* is what matched. Kept; the rule's `:reading` says so.

## Conventions used where the packet left room

- **Cues** are substrings of `source_text` occurring exactly once; spans are
  codepoint offsets into it (generator-enforced).
- **`source_sha256`** in the analysis records is the SHA-256 of the
  `source_text` CONTENT (verified on this batch: identical across the `.json`
  and `.analysis.json` records). The gate checks it that way.
- **Guides followed/departed from:** condition/dependency → `:needs` and
  goal/action → `:produces` were followed throughout. `contrast` became
  `:forbids` nowhere — in every contrast case (turn-118 stage-tension,
  turn-127 two-axes, turn-14-24 unify-phase, codex-221) the contrast WAS the
  produced content, so it is a produced token, and the `:reading` says so.
  Empty `:needs` is used where the act has no state precondition (directives,
  proposals); wr-25's conditional commendation (turn-14) uses an empty need
  on purpose — the merge is NOT asserted, which is the point.
- **Generation:** the 25 files were emitted by a one-off generator (cues →
  spans, EDN printing) from hand-authored token/rule data; the generator is
  not retained (one-off), but the gate below re-derives every checkable
  property from the files themselves.

## Gate output

`check.clj` (committed here) verifies: every file reads as EDN; every
`:pattern` file exists and its `@flexiarg` line matches; every pattern sha256
matches the file now; every `source_sha256` matches the record's
`source_text`; every cue span is in range with non-empty text; every token in
a guard or `:produces` is defined in `:tokens`; no rule has empty
`:produces`; every not-a-rule has a reason.

```
$ cd /home/joe/code/futon2 && clojure -M holes/labs/wm-contract/E-cascade-real/rules-from-turns/hist/check.clj
GATE OK: 25 files, 0 violations
```

clj-kondo on `check.clj`: 0 errors, 0 warnings.
