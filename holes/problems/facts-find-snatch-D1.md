# D1 facts: `find` and the running Snatch `organise`

**Date:** 2026-08-30  
**Reader:** codex-22  
**Scope:** discovery only; no implementation proposed here.

## Part 1 — `organise` graded against O1–O4

- **O1 — DOES NOT HOLD strictly.** The writer records `:acting acting` and
  `:closure (up-closure acting)` as two fields
  (`futon3/checks/playout_snatch.clj:356-362`), so the distinction is
  recoverable, but it does not record `nodes = acting` plus an explicit
  `added-by-organise` set. Instead `:closure` is the complete union, including
  actors: `up-closure` begins with the input frontier and inserts it into
  `seen` (`playout_snatch.clj:271-280`). The O1-required provenance of each
  added node is therefore not present, although it can be reconstructed as
  `closure - acting`.
- **O2 — HOLDS.** The reader matches only the exact regex
  `#"@why (.*)"` (`playout_snatch.clj:257-264`). `@how` and `@see-also` do not
  enter `why-graph`. Edges are then taken only from that authored graph between
  included nodes (`playout_snatch.clj:266-269,282-283`); no similarity,
  co-occurrence, or prose-derived edge is introduced.
- **O3 — DOES NOT HOLD.** The implementation performs transitive up-closure by
  adding every intermediate authority as a node
  (`playout_snatch.clj:271-280`), then retains only immediate authored edges
  among all closure nodes (`playout_snatch.clj:282-283,303-305`). It never
  constructs an edge between selected nodes whose path passes through omitted
  nodes. This is closure, not the O3 restriction/fast-forward operation.
- **O4 — DOES NOT HOLD.** Default precedence lives on individual pattern maps,
  and `pattern-policy` overlays a runtime `overrides` map before sorting
  (`playout_snatch.clj:170-183`). The rewired policy is constructed by passing
  one override (`playout_snatch.clj:185-189`). The cascade writer emits only
  treatment, disposition, rounds, acting, closure, and scores
  (`playout_snatch.clj:354-364`): neither precedence nor overrides are stored as
  a collection-level cascade field.

## Part 2 — recorded scenarios and tensions

The requested count is eight, but the code declares six scenarios
(`futon3/checks/playout_snatch.clj:330-332`) and the generated file contains
exactly those six (`futon3/checks/snatch-cascade.edn:1-83`). I grade all six. I
refuse to invent two absent scenario identities, actors, or negative witnesses.

### G1 / snatcher

Recorded actors are `probe-before-committing` and
`exchange-when-both-sides-gain` (`snatch-cascade.edn:2-10`). A conforming find
would need these tensions:

- Probe IF: “You face a counterpart whose disposition you do not know, and an
  offer of any size is available to you.”
  (`library/snatch/probe-before-committing.flexiarg:14-16`). HOWEVER: “A large
  first offer is unrecoverable if they snatch, and a zero offer buys nothing —
  you end the round knowing exactly what you knew at its start.” (`:18-20`).
- Exchange IF: “Both players have something the other values twice as highly
  as its holder, and an exchange remains available this round.”
  (`library/snatch/exchange-when-both-sides-gain.flexiarg:13-15`). HOWEVER:
  “Attention to seizure risk can make abstention look like the objective, even
  though holding every token leaves the game's available surplus unrealised.”
  (`:17-19`).

**F4 zero-mass pattern:** `consult-the-remedy-before-exiting` must not be
returned: its IF requires both prior defection and an arrangement-provided
mark/adjudicator/response
(`library/snatch/consult-the-remedy-before-exiting.flexiarg:15-17`), while G1
provides neither shame nor judge (`playout_snatch.clj:151-153`).

### G1 / sharer

Recorded actors are `probe-before-committing` and
`escalate-only-as-far-as-you-can-lose` (`snatch-cascade.edn:13-22`).

- Probe IF and HOWEVER are exactly those quoted above
  (`probe-before-committing.flexiarg:14-20`).
- Escalate IF: “A counterpart has accepted an offer, and larger offers are
  available.”
  (`library/snatch/escalate-only-as-far-as-you-can-lose.flexiarg:14-15`).
  HOWEVER: “One acceptance is weak evidence — a snatcher who accepts once has
  lost nothing by waiting — and an offer sized to the belief rather than to the
  loss turns a good read into an unrecoverable position.” (`:17-20`).

**F4:** `consult-the-remedy-before-exiting` must not be returned because its IF
requires a defection (`consult-the-remedy-before-exiting.flexiarg:15-17`), while
the disposition response is acceptance (`playout_snatch.clj:158-159`).

### G1 / cautious

Recorded actors are `probe-before-committing` and
`an-unmodelled-response-stops-the-line` (`snatch-cascade.edn:25-34`).

- Probe IF and HOWEVER are exactly those quoted above
  (`probe-before-committing.flexiarg:14-20`).
- Stop-line IF: “The counterpart produces an outcome to which your model
  assigned no probability.”
  (`library/snatch/an-unmodelled-response-stops-the-line.flexiarg:14-16`).
  HOWEVER: “Play continues whether or not you notice, and the cheapest next
  move is always to repeat the policy that just met a surprise — which is
  trying harder inside a model you now know is wrong.” (`:18-21`).

**F4:** `consult-the-remedy-before-exiting` must not be returned: its IF
requires defection plus an institutional response
(`consult-the-remedy-before-exiting.flexiarg:15-17`); cautious P2 refuses
(`playout_snatch.clj:158-159`) and G1 supplies no remedy (`:151-153`).

### G4 / snatcher

Recorded actors are `probe-before-committing`,
`consult-the-remedy-before-exiting`, and `re-enter-after-observed-repair`
(`snatch-cascade.edn:37-51`).

- Probe IF and HOWEVER are exactly those quoted above
  (`probe-before-committing.flexiarg:14-20`).
- Consult IF: “You have been defected against, and the arrangement provides a
  mark, an adjudicator, or any other response.”
  (`consult-the-remedy-before-exiting.flexiarg:15-17`). HOWEVER: “A policy
  written for the state of nature exits on the first defection, and exiting is
  indistinguishable — from the outside and from the policy itself — whether or
  not a remedy existed.” (`:19-22`).
- Re-enter IF: “The seized tokens were restored or compensated, the remedy is
  recorded, and a small new offer can test whether play changed.”
  (`library/snatch/re-enter-after-observed-repair.flexiarg:13-15`). HOWEVER:
  “Permanent exit discards any improvement the institution achieved, while
  re-entry on a promise alone repeats the original exposure.” (`:17-19`).

**F4:** `forced-play-needs-a-loss-floor` must not be returned: its IF requires
abstention to be unavailable
(`library/snatch/forced-play-needs-a-loss-floor.flexiarg:14-15`), but G4's
action set contains abstain (`playout_snatch.clj:154-155`).

### G2 / snatcher (12 rounds)

The file records `probe-before-committing`,
`forced-play-needs-a-loss-floor`, `exchange-when-both-sides-gain`, and synthetic
`:no-pattern` (`snatch-cascade.edn:54-69`).

- Probe and exchange require the clauses quoted under G1/snatcher
  (`probe-before-committing.flexiarg:14-20`;
  `exchange-when-both-sides-gain.flexiarg:13-19`).
- Forced-play IF: “Abstention is unavailable and a prior response makes
  seizure a live risk.”
  (`forced-play-needs-a-loss-floor.flexiarg:14-15`). HOWEVER: “An exit-based
  policy cannot execute in this treatment, but compulsion does not make a large
  exposed offer safer.” (`:17-19`).
- **Refusal:** `:no-pattern` has no file in `library/snatch/` and therefore no
  IF/HOWEVER clauses to quote. It is the policy fallback
  (`playout_snatch.clj:179-183`), later admitted into `:acting` because the
  trace's `:by` values are collected without filtering (`:356-361`). A find
  over the repository cannot return it under F1.

**F4:** `consult-the-remedy-before-exiting` must not be returned. Although a
snatch occurred, its IF also requires an arrangement-provided response
(`consult-the-remedy-before-exiting.flexiarg:15-17`); G2 provides neither shame
nor judge (`playout_snatch.clj:153-154`).

### G5 / sharer

Recorded actors are `probe-before-committing` and
`escalate-only-as-far-as-you-can-lose` (`snatch-cascade.edn:72-81`). Their exact
IF/HOWEVER clauses are those quoted under G1/sharer
(`probe-before-committing.flexiarg:14-20`;
`escalate-only-as-far-as-you-can-lose.flexiarg:14-20`).

**F4:** `re-enter-after-observed-repair` must not be returned: its IF requires
restored/compensated seized tokens and a recorded remedy
(`re-enter-after-observed-repair.flexiarg:13-15`), while a sharer accepts
(`playout_snatch.clj:158-159`) and no prior loss is repaired.

**F4 result:** a named zero-mass library pattern is established for all six
recorded scenarios. No result is possible for two unrecorded scenarios.

## Part 3 — a non-substring `find` with F2/F3 receipts

For this section, `find` can avoid substring search by taking the structured
game state as `Tension.context` and evaluating the already-encoded IF and
HOWEVER predicates: `fires?` requires both predicates and excludes P2 patterns
(`futon3/checks/playout_snatch.clj:141-148`), while `applicable` returns their
IDs (`:148`). For every returned ID, a receipt can cite the exact flexiarg
IF/HOWEVER line ranges above, record `:route :structured-antecedent`, the state
fields tested, both predicate results, and an as-of revision. That satisfies F2
without lexical overlap and F3 because the warrant is authored pattern text
plus executable antecedents, not a similarity score. The limitation must stay
visible: the guards duplicate prose by hand, and `:if-text`/`:however-text` in
the runner are not parsed from the flexiargs (`playout_snatch.clj:39-139`). This
is therefore viable for the closed Snatch fixture, but it does not establish a
general `find` for prose-only library sections; that would require an authored,
machine-readable antecedent or an independently attested compilation from text
to predicate.
