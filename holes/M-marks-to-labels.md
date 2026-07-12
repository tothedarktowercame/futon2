# M-marks-to-labels — operator marks, resolved to referents, minted as reward labels

**Status: DRAFT 2026-07-12 (claude-5 laptop, from Joe's synthesis: "label mass
not features" × M-points-de-fuite × the 5637/D1 text-indexing line). Not
armed.**

**Composes:** [[M-points-de-fuite]] §6.5 (✘/✓/💡 hydra, EDN long form,
illocutionary-force sketch, clustering probe) · [[M-zaif-harness]] (PZ1 base
rate 24%, B0 recognizer, B1 γ(mission) fold, U1 transcripts) ·
[[M-text-sidecar]] / XTDB#5637 (D1 FTS5, P2 candidate-generator-never-
authority) · [[M-mission-conditional-reward]] (the verdict this answers:
label mass/breadth is binding, not features).

## HEAD

The reward lane is label-starved at the grain it needs (39 flown-fold labels
in ~5 weeks; LOMO transfer unresolvable) while the operator emits judgments
at ~24% of turns (PZ1, measured) that currently evaporate. The marks channel
captures them at precision 1.0 by construction — but a mark judges a TURN'S
ACT and R̂'s labels judge a COMPOSITION AGAINST A WANT
({used pattern-ids, want, success}). THE MISSION IS THE GRAIN BRIDGE: resolve
each mark to its REFERENT; where the referent carries (:pattern-ids, want) —
deposits, proposals, psis, flights — mint a label in the loader's own shape,
tagged with its grain, quarantined from the scoreboard until measured.

WANT: marks flow → typed evidence → resolved referents → (a) γ(mission)
ledger events (B1, exists) AND (b) reward-lane label records at
:grain :operator-mark, with the channel's value MEASURED (does it move
S3/LOMO?) before anything trusts it.

## Why this is the label-mass answer (the arithmetic)

24% correction base rate × operator turns/week × even 50% marking discipline
≈ tens of operator-gold events/week, spread across every clocked mission —
vs the fold lane's ~6–10 labels per batch on 3–5 missions. Mission BREADTH
(the LOMO requirement) comes free: marks land wherever the operator actually
works. The channels compose: flown-fold labels stay the calibration anchor;
mark labels are volume.

## Slices (cheapest first; measure the instrument before trusting it)

### L0 — B0 recognizer + typed tags (JVM/Emacs window; the prerequisite)
Turn-end recognizer (sibling to agent-chat.el §6.5 clocking recognizer):
parse bare glyph + EDN long form; stamp :correction/:approval/:idea tags +
parsed :ref/payload on the chat-turn evidence entry. MUST implement the
use-vs-mention rule (b1-live-marks.edn: a mark inside a description of the
convention is not an event — the corpus's very first ✘ was a mention).
Needed regardless: D1's unicode61 tokenizer strips the glyphs, so tags are
the only queryable route. Same window: load D1 locally (code pulled, JVM
predates it).

### L1 — referent resolution (the bridge's first half)
A mark's judged artifact, resolved by a DOCUMENTED precedence:
1. explicit :ref (the EDN long form) — authoritative;
2. quoted-fragment payload → span in the in-reply-to turn via D1 candidates
   + store re-check (the P2 pattern doing SPAN ADDRESSING — this is where
   the 5637 line meets the marks line; "breaking down the text entries");
3. bare mark → the in-reply-to agent turn as a whole (U1 transcript says
   what that turn DID), at reduced confidence.
Output: mark-adjudication records {mark, referent {kind, id, span?},
mission (autoclock witness), confidence, provenance incl. the resolving
query}. Tests: hand-verified resolution on the existing corpus (tiny — that
is fine, the contract is the deliverable) + synthetic multi-item turns.

### L2 — the label adapter (the bridge's second half)
Where L1's referent is a deposit/proposal/psi (carries :pattern-ids + want):
emit a record in fold_ground_truth's exact shape, PLUS :grain
:operator-mark and :mark-confidence. Loader ingests behind a flag, DEFAULT
OFF — the scoreboard keeps reading flown-fold labels only. ✓ → success
true, ✘ → false; 💡 and other forces mint NO label (not judgments of
discharge).

### L3 — the measurement gate (PZ1 discipline, applied to ourselves)
When ≥20 mark-labels have accrued: (a) agreement with flown-fold
adjudications where both exist on the same referent (the calibration
question: does turn-grain approval predict psi-grain discharge?);
(b) S3 and LOMO with-vs-without the channel on the then-current corpus.
Preregistered before the data exists: if agreement is poor or the curves
degrade, the channel stays a γ-ledger feed (still valuable) and does NOT
enter R̂ — reported, not tuned.

### Probe (parallel, good handoff when Agency returns): the §6.5 clustering
probe — ~8.8k joe-authored turns clustered (D1 terms and/or MiniLM);
operator names clusters worth marking; each named cluster earns a glyph.
Vocabulary from the record, not a priori.

## Boundaries

- A mark judges the act it references, at the moment it lands; it never
  retro-edits flown-fold adjudications.
- Marks are operator-authored by definition; agent "mark this?" proposals
  are proposals only (M-points-de-fuite affordances list).
- The 5637 IN-CORE work stays out of scope (blocked on #3663); everything
  here is the app-layer sidecar pattern, which is also the evidence the D2
  packet wants anyway.

## Acceptance (mechanical, per slice)

- L0: a live ✘/✓ turn acquires its typed tag + parsed ref within one
  turn-cycle; a mention acquires none; both shown from the store, not the
  buffer. D1 answers locally.
- L1: precedence table exercised on real + synthetic cases; every record
  carries its resolving query (replayable provenance).
- L2: adapter output round-trips through fold_ground_truth's loader with
  the flag ON in a sandbox; flag OFF = byte-identical current behavior.
- L3: the two preregistered numbers, reported pass or fail.
