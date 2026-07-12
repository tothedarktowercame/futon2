# N — The Peircean codebook: signs whose interpretants co-evolve with the operator

**Status: NOTE 2026-07-12 (claude-2, from Joe: "start to be properly semiotic
about it"). Not armed; a design contribution FOR [[M-points-de-fuite]] and
[[M-marks-to-labels]] (which stays claude-5's draft — this note adds the
semiotic layer both can cite, changes neither).**

## The move

The marks channel currently treats a symbol as a *decode*: ✘ means
correction, ✓ approval, 💡 idea. Joe's observation: be properly semiotic —
each mark is a **Peircean triple**, and the third leg is where the value
lives:

- **sign** — the glyph as typed in the flow (💡)
- **object** — the referent the mark judges/annotates: exactly what
  M-marks-to-labels L1 resolves (explicit :ref > quoted-span > in-reply-to
  turn)
- **interpretant** — the *effect the sign has in the system*: the coded tag,
  the typed event minted, the downstream routing — **versioned, revisable,
  and expected to drift as operator and system co-evolve**

L0/L1 already build the first two legs. The codebook makes the third leg
first-class instead of a constant baked into a recognizer.

## Codebook entry shape (EDN; one file, append-only revisions)

```clojure
{:sign "💡"
 :code :idea                    ; FTS-visible word — the D1 unicode61
                                ; tokenizer STRIPS glyphs, so :code is the
                                ; queryable name (the practical reason
                                ; "coded as idea" matters at all)
 :object-resolution :marks-to-labels/L1   ; cite, don't duplicate
 :interpretant
 {:version 1
  :emits :evidence/idea-event   ; typed event shape
  :routes [:mission-log :gamma-ledger]   ; where it flows
  :lane nil                     ; optional Q1–Q5 tag (see Q5 note)
  :adopted "2026-07-12" :rationale "Joe, in-flow"}
 :interpretant-history []}      ; prior versions stay; append-only
```

**Revision mechanics (the co-evolution, made mechanical):** an interpretant
revision is itself a typed event (`:codebook/revised`, carrying sign, old
version, new version, evidence). Two triggers, both measurable:
1. **Misfire** — the operator marks-then-overrides, or the L2 measurement
   (does the channel move S3/LOMO?) shows a sign's labels hurt: the
   interpretant is mispredicting the operator. A misfire is a *prediction
   error at the semiotic layer* — same arithmetic as every other channel.
2. **Drift-by-use** — recurrence analysis shows the operator using a sign in
   a pattern the current interpretant doesn't cover (the B7-F1 lesson:
   recurrence IS ratification, and it is computable from the corpus).

Bateson's constraint (points-de-fuite §4) is thereby honored *structurally*:
the codebook cannot fossilize into an ontology because its entries carry
their own revision protocol and history.

## Re-grounding the legacy collection

Points-de-fuite's large symbol collection (incl. Table 25, "A critical
apparatus," CORNELI thesis App. F — the same move, 2013, aimed at live
developer discussions) was pointed at a **codebase-commons**: signs whose
objects were artifacts and whose interpretants served the commons' shared
record. The re-grounding for the operator-system dyad:

- import each legacy sign as a codebook entry at `:interpretant {:version 0
  :legacy :codebase-commons}` — candidates, not live;
- a legacy sign goes live only by **use-ratification** (the operator actually
  reaches for it in flow; recurrence threshold as in B7-F1), at which point
  its interpretant is re-authored for the dyad;
- signs never ratified stay in the book as history, not vocabulary.

This is the semiotic form of the pattern-mint lane: new signs enter at the
uniform prior and earn status through use.

## The Q5 connection ("things I am working on in myself")

The audit (joe-loop-r-contract-audit.md) needs an R2 observation channel for
the reflexive lane that costs nearly nothing in-flow. Signs are exactly that:
**one glyph = one typed self-observation at precision 1**, no stride broken.
Candidate Q5 signs (candidates only — ratification by use, per the above):

| sign | :code | interpretant sketch | lane |
|---|---|---|---|
| 💡 | :idea | idea-event → mission-log + γ-ledger | any |
| 🧘 (or 拳) | :practice | life/practice event, :kind inferred from context | q5 |
| 間 | :interval-noticed | "I am in the gap between articulation and salience" — a mode observation, the foraging/depositing check made one-keystroke | q5 |
| ⚖ | :mode-check | explicit self-regulation event (the mode-violation sorry's channel) | q5 |
| ☯ | :tended | the weekly relational/practice :tended? check, inline | q5 |

The Peircean point lands hardest here: for Q5 signs the operator IS part of
the object — the sign's use *changes the signer* (marking 間 is itself a
salience practice). That is the co-evolution stated plainly: the interpretant
updates the system's model of Joe, and the habit of signing updates Joe. A
codebook whose entries know their own version history is the honest
instrument for a semiosis that is expected to move.

## Non-goals (this note)

No recognizer changes (L0 is claude-5's), no new glyph parsing, no live
codebook file yet — the seed codebook should be authored by the operator
in-flow (five entries above are candidates), because authoring the
interpretant is itself the first act of the co-evolution.
