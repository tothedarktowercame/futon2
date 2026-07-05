# E-live-loop-1 S2 — the escrowed fold-turn: shape, home, replay path

**Status: DESIGN (2026-07-04, late night). Driver claude-19; reviewer
claude-16; arming gate Joe (per-fold, his word alone — nothing in this
note deposits or arms anything).** Parent: `E-live-loop-1.md` S2.

## What this designs

The architecture already anticipated the fix (excursion finding 3):
`futon2.aif.fold-llm/llm-fold` takes an injected `:turn-fn` — "produced
out-of-process (a bell to an agent, or a recorded fold-turn read from
escrow)"; nil ⇒ no construction ⇒ ΔG nil ⇒ honest abstain
(`fold_llm.clj:69-85`). What has never existed is (a) the *shape* of a
recorded fold-turn, (b) its *home*, (c) the *replay seam* on the live
path. Those three, below. The escrow has had no deposits; this note is
the deposit's blueprint.

## A. The deposit record — one fold-turn, one EDN file

Not a new invention: the sortie-11 mission-triple schema
(`futon6/holes/golden-graphs/SCHEMA.md`, pinned) already carries an
escrow slot (`:arrow-candidate {… :escrow "do-not-write-meme.db"}`) and
the vocabulary a fold-turn needs (`:hole`/`:cascade`/`:wiring`,
`:via`/`:rule` provenance). A fold-turn is a new **confidence tier** of
that record — `:authored`, by an LLM turn, under operator arming — with
a new rule family `"fold-turn/…"` parallel to `"sortie-11/…"`.

```clojure
{:fold-turn/id  "ft-<mission-slug>-001"
 :mission       "<repo>-d/mission/<slug>"          ; canonical node, never an alias
 :hole     {:confidence :authored
            :have "<sorry have-text>" :want "<sorry want-text>"
            :via "E-live-loop-1 S1 ψ recipe" :rule "fold-turn/hole-from-sorry-psi"}
 :cascade  {:psi "<exact ψ string>" :psi-sha256 "<hex>"
            :pattern-ids ["…" …]                   ; the shown set, order preserved
            :constructor {:entry "construct_cascade" :epsilon 0.15
                          :budget 6 :lambda 0.25 :commit "<futon3a sha>"}
            :pattern-cites […]}                    ; sortie-11 vocabulary, for the miner/adapter
 :prompt   {:sha256 "<hex of the exact fold-prompt string>"
            :prose-sha256 {"<pattern-id>" "<hex>" …}}   ; the NL halo versions folded against
 :turn     {:agent "<agent-id>" :model "<model-id>" :edge "<bell/job id>"
            :at "<iso8601, passed in — not stamped in-process>"
            :answer {:boxes […] :wires […] :terminals […] :policy-holes […]}}
 :arming   {:operator "joe" :word "<verbatim quote>" :at "<iso8601>"
            :scope :one-fold :fold-turn/id "ft-…-001"}
 :eval     {:delta-g <fe/coverage-delta-g at deposit time> :g-grain :coverage}
 :wiring   {…}                                     ; sortie-10 :composes projection of :turn/:answer
 :arrow-candidate {:have "…" :want "…" :format :sorry :escrow "do-not-write-meme.db"}}
```

Three pins, each a K2-class assertion at write/read time:

1. **Replay-validity binding.** `:prompt/:sha256` is over the *exact*
   `fold-prompt` output — which already covers cascade ids +
   circumstance + prose texts. Replay fires only on exact hash match;
   any drift in cascade, ψ, or prose ⇒ nil ⇒ the fold abstains exactly
   as today. A recorded turn can never be replayed against a
   circumstance it wasn't constructed for.
2. **No arming, no deposit.** The writer asserts `:arming` present and
   complete (operator, verbatim word, timestamp, scoped fold-turn id)
   before the file exists. The arming record is *part of the deposit*,
   not ambient context.
3. **ΔG recomputable.** On every load, assert `:eval/:delta-g` equals
   `fe/coverage-delta-g` of `:turn/:answer` recomputed fresh. A deposit
   whose stored ΔG drifts from its recomputation is rejected loudly
   (silent-wrong-results are this stack's known hazard class).

## B. Where it lives

`futon6/data/fold-turns/ft-*.edn` — sibling of
`futon6/data/mission-triples/` (the sortie-11 corpus; cite the
directory, not a count — it drifts). Rationale:

- Same EDN discipline, same schema family, same consumers:
  `futon3c/scripts/substrate_metric_cascade_adapter.py` reads only
  `:differentiates`/`:states`/`:composes` + satiety, so a fold-turn's
  `:wiring` projection is consumable by the metric machinery unchanged.
- **Not** meme.db (the escrow marker says so verbatim); **not** a
  substrate-2 write (the :7071 canonical-id gate and no-store-writes
  discipline hold; file-first, ingest later through existing paths if
  Joe wants it queryable).
- File-per-turn keeps the deposit reviewable in a diff and revertable
  by `rm` — the right blast radius for an operator-armed artifact.

## C. Replay path into the live lane

Today (`futon2.aif.close-loop/act-gate-from-lane-entry`,
`close_loop.clj:22-40`) the ΔG reconciliation is: move-set `:G-rollout`
→ classical fold coverage-ΔG → nil (abstain). **The LLM fold is not on
the live path at all** — that is the load-bearing discovery of this
design pass: 0-for-674 is over-determined; even with escrow deposits,
today's live lane would never read one.

The seam, smallest honest version:

- New ns `futon2.aif.fold-escrow` (pure core, injected I/O):
  - `load-deposits : dir → [deposit]` — reads `ft-*.edn`, runs pin 3's
    ΔG assertion, drops-loudly any invalid file.
  - `escrow-turn-fn : deposits → (fn [prompt] …)` — sha256 the incoming
    prompt; return the matching deposit's `:turn/:answer`, else nil.
- One addition to the reconciliation in `act-gate-from-lane-entry`:
  when the classical fold abstains (no boxes / nil ΔG), try
  `llm-fold(shown, circumstance, {:turn-fn (escrow-turn-fn deposits)
  :prose-fn <the existing halo source>})`; `:delta-G/source` gains
  `:fold-escrow` so the bake-off provenance stays clean.
- Properties preserved: no LLM call on any request path (the turn-fn is
  a hash-gated table lookup); nil on any mismatch ⇒ today's behavior,
  byte-identical; deterministic and read-only, so I-0 holds; the change
  is a handful of lines in my-code-only namespaces, reloadable under
  the normal discipline (clj-kondo 0 errors, check-parens, tests, no
  restarts, no third-party defprotocol reloads).
- `fold-realized` stays staged off (`*live-wire?*` false): escrow
  replay supplies the *expected*-G leg only; realized-G still requires
  enactment, which remains Joe's separate call. No change proposed
  there.

## D. The one deposit (procedure, executed ONLY on Joe's word)

1. Pick the cascade (candidates below). Build the exact `fold-prompt`
   out-of-process from its S1 cascade + prose halo; record the sha.
2. Bell an inhabiting agent (the constructive step, paid once,
   out-of-band, under consent — this bell *is* the armed LLM turn).
   Answer must be the EDN construction shape; `parse-construction`
   non-nil; policy-holes honest per the prompt's no-fabricated-coverage
   clause.
3. Compute ΔG via `fe/coverage-delta-g`; assemble the deposit with the
   arming record; write `ft-*.edn`; re-read through `load-deposits` so
   pin 3 runs before the deposit is called real.
4. Replay on the live path (read-only eval through the seam in C) and
   record the replayed ΔG — the excursion's S2 deliverable — plus the
   act-gate's two-leg view (ΔF from the S1 row ∧ replayed ΔG), since
   the gate ANDs the currencies.

**Candidates carried from S1** (both survive the ablation):

| candidate | case for it |
|---|---|
| `autoclock-in` (F +0.813, size 4) | **recommended**: its A-next EDN is a *sealed ground-truth wiring* (blinded-study reference) — the armed turn's construction can be *scored* against what the mission actually built. The one deposit becomes an experiment with an answer key, not a demo. |
| `state-snapshot-witness/:s4` (post-ablation F +2.139, size 7, rel 0.530) | the strongest surviving cascade; richest halo for the agent to fold. No sealed reference, so quality assessment is judgment-only. |

Choice is Joe's, with the arming word; the design is identical either
way.

## E. Phase B convergence (per claude-16's review note)

M-first-flights Phase B's arming condition — "terms constructed +
metric understood" — reads satisfied via the sortie-11 triple corpus
(`futon6/data/mission-triples/`) and M-substrate-metric at R2-verdict
depth (OR-curvature over `feeds-mu?`, exact W1). This design converges
rather than parallels: the fold-turn record *is* a sortie-11-family
triple (hole + cascade + wiring) at a new confidence tier, stored as a
sibling corpus, consumable by the same cascade→metric adapter. A
fold-turn with a witnessed ΔG is exactly the "patterns that co-fired in
a witnessed discharge" event sortie 12 says the metric's golden
triplets fall out of — so each deposit simultaneously feeds Phase B's
state-action space and the metric's training pairs. Whichever way the
operator rules on arming, nothing here needs rework to serve both.

## F. Seed patterns as starting places (design added same night, operator input)

Two process-coherence seed patterns landed in the library (futon3
`d433bbb`: `process-coherence/status-refresh-before-work`,
`process-coherence/is-this-even-a-problem`; canonical instances =
tonight's arc). Operator intent: starting places for nearly every WM
cascade. Measured against the constructor as it exists, a new seed is
**triply invisible** today:

1. **Not retrievable** — `ranked_candidates` ranks over the
   `minilm_pattern_embeddings.json` artifact (1188 entries; the two
   seeds: 0 hits). Regeneration step, by name:
   `futon3a/scripts/index_patterns.sh --minilm` (re-embeds the futon3
   library, including new flexiargs, into `resources/notions/`). The
   constructor loads `EMB` at import, so short-lived dev processes pick
   it up on next run; no JVM implications.
2. **Not eligible** — `construct_cascade` shunts any ranked pattern
   whose stem is missing from `phylogeny["patterns"]`
   (`futon6/data/pattern-phylogeny-edges.json`; seeds: 0 hits) into
   `coverage_candidates`, which is *display-only*: it can never be
   chosen (`cascade_construct.py:188-197`). Seeds must be registered in
   the phylogeny patterns set even with zero edges.
3. **F-punished** — zero co-app mass ⇒ the unseen-pattern default prior
   `f/(f+K)` ≈ 0.0909 ⇒ complexity 2.398 nats each (exactly the cx S1
   measured on rare singletons), an F-cost of ~0.60 at λ=0.25 that a
   seed's own marginal coverage (~0.10–0.15) can never pay. Left as-is,
   the F-gate would *fine every cascade for following the operator's
   process discipline* — the gate turned against its owner.

**Recommendation — explicit seeding in the accretion + a
designation-prior floor.** One mechanism, two halves, both honest:

- **(c) First slot = initialize `chosen` with the designated seeds**
  (new optional `seeds=[...]` parameter; registry artifact
  `futon6/data/pattern-seeds.edn` carrying pattern-id + operator
  provenance + date, so designation is an operator act with a paper
  trail, not a code constant). The greedy loop then runs unchanged —
  and its connectivity term starts pulling patterns that co-apply with
  the seeds, which is exactly what "starting places" means
  mechanically. Seeds get a normal trajectory entry (m = rel·α, the
  zero-connectivity form) so the accuracy ledger stays consistent. A
  rel sanity floor (seed enters only if rel ≥ ~0.15 to this ψ) keeps a
  genuinely irrelevant seed from being forced in by fiat.
- **(b) Prior = designation, not history.** For registered seeds, the
  inclusion prior takes `max(learned-prior, seed-floor)` with
  seed-floor = the *median* pattern's prior (mass=K ⇒ ≈0.524, cost
  0.65 nats ⇒ F-cost ≈0.16). Reading: the base-rate prior encodes "how
  expected is this pattern"; the operator declaring "this starts nearly
  every cascade" is a legitimate prior statement, entered as such. It
  is self-retiring: once seeds actually co-fire, learned co-app mass
  overtakes the floor and the designation becomes inert. No pattern is
  ever complexity-free (rejected: exempting seeds from the ledger —
  free patterns are padding fuel and break the Occam gate).

**Rejected alternatives, with reasons:** *reserved opening box*
(prepend by fiat outside the ledger) — creates a second class of
cascade member that fold/judge/display must all special-case, and
"anchor outside the ledger" is exemption-by-another-name; *alpha boost*
(raise α for seeds) — couples a global constant to per-pattern intent,
wins or loses on the rel of whatever else is in the pool (fragile,
unpredictable), and does nothing about the prior hammer.

**Costs of the recommendation, stated:** (i) seeded and unseeded
cascades are not F-comparable — the output must carry a `:seeds`
field so the judge/notebook segment them (Flight-1 baselines stay
unseeded-lane); (ii) the designation floor is operator trust made
numeric — the S3 γ loop is its audit: if seed-opened cascades never
pay realized outcomes, the designation gets revisited (the
gamification guard, applied to ourselves); (iii) two artifacts to
touch on ingest (embeddings + phylogeny registration) — worth wrapping
as one documented "new-pattern ingest" step so future seeds don't
rediscover the triple gate. **No build tonight.**

## What S2 explicitly does NOT do until Joe's word

No `ft-*.edn` exists. No bell for a fold-turn is sent. No `fold-escrow`
namespace is written (the seam in C is design; build follows the arming
decision or Joe's separate go-ahead on the read-only seam). The live
lane is untouched.
