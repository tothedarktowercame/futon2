# NOTE — Joe's view of C, consolidated

Written 2026-09-18 by claude-12 (Lean-layer owner), from the dated primary
records below plus claude-4's capture of the 2026-09-18 discussion. **This note
is for Joe to redline, not to re-explain.** Every claim is marked: [QUOTE] is
Joe verbatim, [RECORD] is a dated file that cites his direction, [INFERENCE] is
an agent's guess flagged for his correction. When this note and a ruling
conflict, the ruling wins; corrections are follow-up edits here, dated.

## 0. Where the view was already on record

Joe said (2026-09-18) that nobody had ever captured his view of C. It was
never consolidated — and none of it reached Lean — but it exists in five
dated places, which this note now indexes:

| record | dated | what it carries |
|---|---|---|
| `holes/labs/wm-contract/RULINGS-walkthrough-2026-09-08.md` Item 12; `-09-09.md` Items 18c–18d | 09-08/09 | the rulings trail |
| `holes/labs/wm-contract/C591-C-as-calculation-proposal.md` | 09-08 | the method: discovery, not invention |
| `holes/E-C-realization.md` (+ `DERIVE-ARGUE-C-realization-2026-09-09.md`, `INSTANTIATE-C-module-v1-2026-09-09.md`) | 09-09 | the interactive lane; owns the Lean deferral marker at `Holes.lean:157` |
| `holes/E-C-vector-live.md` | 09-08 | channel C_int live; the freshness exit condition |
| `src/futon2/aif/live_c.clj` | 09-17 | Joe's three named sources, derived as weighted want tokens |

## 1. What C is

- [RECORD, `live_c.clj` header, one hop from Joe] "C is preference over
  OUTCOMES; the live C contributes WEIGHTED WANTED TOKENS in the cascade
  preference spec's existing shape — no second preference object beside the
  spec."
- [QUOTE, via E-C-realization header] "I am not happy to kick the can without
  real work to make C real."
- [QUOTE, 2026-09-18] the whole-system criterion C sits inside: "one correct,
  best-of, validated model that is both AIF and is a model of what is
  effective in our setting."

## 2. The method: discovery, not invention

[RECORD, C591 opening, from Joe's direction per RULINGS Item 12]: "the
preferences are to be READ OFF the recorded evidence, with ruling as the
fixing act." Concretely:

- **Pins.** Ruled masses (seeded-C) are ground truth; discovery cannot move
  them, only a ruling can.
- **Extractor.** Reads the evidence landscape (rulings files, mission
  documents, logged operator turns) and emits proposed-C with per-mass
  provenance. The extractor never writes preferences.
- **The diff is the product.** proposed-C vs ruled-C renders as a decision
  sheet; agreement accumulates citations, disagreement surfaces for ruling.
- **Three preference objects, not one:** E (habit prior) is revealed
  preference — what the system did; ruled C is stated preference — what Joe
  ruled; proposed-C is evidence-derived. The proposed-vs-ruled gap is the
  operator-facing signal.

## 3. The three named sources (2026-09-17, "plenty to get started")

[RECORD, `live_c.clj`, commissioned from Joe's sources]:

1. **harmony/aliveness** — `futon6/data/mission-wholeness.edn`, Salingaros
   L = T·H over scope-tree centres; a want token per :alive mission with
   L > 0, weighted by L. **The dark-room guard:** the weight is the product
   L, never H alone — an empty scope-tree is perfectly harmonious, and
   L = 0 contributes no token. C must not reward inert harmony.
2. **completed missions** — a mission not yet closed wants its closure
   (token `:closed/<mission>`, weight 1).
3. **capability stars** — an unsatisfied capability is unreached (token
   `:star/<capability>`, weight 1).

Missing/unparseable sources are typed refusals, never default weights; the
derivation carries a freshness signature and says loudly when the corpus has
moved (E-C-vector-live's non-negotiable exit condition).

**Wiring status [FACT, checked 2026-09-18]:** `live_c.clj` has zero
production requirers — one test, nothing else. The most Joe-derived C
artifact in the tree is not connected to anything.

## 4. Standing rulings and rejections (2026-09-18 unless dated)

- [QUOTE] "I don't want to restore anything. I want a correct model." (Bears
  directly on the removed trace field behind
  `checks/preference_stack_binding_check.clj`'s current failure.)
- [QUOTE] "if we're going to leave something really important like the
  mission layer of C undeclared, that's a bigger liability than these
  bookkeeping issues." — Priority ruling: declaring the mission layer of C
  outranks the authority-gate bookkeeping. Note the mission layer *is* what
  `live_c.clj` derives, and it is unwired (§3).
- [QUOTE] "it sounds like we need some type of default C temperature" — a
  default is needed; no value proposed. Current code default:
  `default-c-temperature` 0.1 (`preferences.clj:128`), unratified.
- [QUOTE] "I don't have any softness proposal in mind" — nothing pending
  there.
- [RECORD, 09-09] Modular, versioned C implementation authorized, with Lean
  type correctness and WM interoperability as the criteria; speculative
  duality deferred to evidence from use.

## 5. Cross-check: the "4 vs 5 layer" question dissolves

`efe.clj:126` (`preference-stack-record`, 5 entries: :floor,
:capability-zone-load, :live-goal-outcomes, :c-vector-overlays, :habit-prior)
is a **provenance stack** — who authored which preference layer, transcribed
from R19-preference-stack.edn at commit 17d779d. `ruled_outcome_c.clj:69`
(`fold-declaration`, 4 entries: :ruled-outcome-c, :c-int, :c-ser, :c-mis,
"Declaration only") is a **fold declaration** — which risk contributions
enter G. They describe different axes of the same system and were never two
competing proposals from Joe. [INFERENCE, claude-4, endorsed by claude-12
after reading both]: asking Joe to arbitrate "4 or 5" would be asking him to
choose between two things he never proposed; his actual direction went into
discovery (§2) and the three sources (§3).

## 6. The Cτ family and the disposition bridge

- [RECORD, `Holes.lean:157` deferral] preferred observations form a
  time-indexed Cτ family whose terminal member is the ruled outcome-kind
  distribution; P(d|o) is "to be fitted from recorded
  observation-to-disposition evidence."
- [FACT] `horizonEFE` takes step-indexed `C : ℕ → O → ℝ`;
  `horizon-g-sparse` accepts `:c-fn-pointwise` (step-indexed) or `:spec`
  (declared constant at every τ). Production passes `:spec`; the
  step-indexed path exists and is unused.
- [RECORD, C591 §1] the bridge P(d|o) is a model-side object, discoverable
  from the cohort ledger; fitted 2026-09-09 with 1 of 12 outcome kinds
  having support, so disposition-risk computes but is constant across
  policies. The §1b observation-domain gap (checkpoint trajectory vs
  channel-valued observations) is recorded in E-C-realization and unresolved.
- [INFERENCE, claude-4, marked for Joe] "fitted from recorded evidence"
  (Lean), "READ OFF the recorded evidence" (C591), and the three evidence
  sources (live-c) all point one way: Cτ as a family derived from the corpus
  at each step rather than declared. Not confirmed by Joe.

## 7. Open questions for Joe (redline, don't re-explain)

1. Is §1–§3 still your view? (If a line is wrong, strike it.)
2. Should wiring `live-c` into the cascade preference spec be the concrete
   form of "declare the mission layer of C"?
3. The Cτ-derived-per-step inference in §6 — right direction, or no?
4. Default C temperature: does 0.1 get ratified, or does the default come
   from somewhere principled?
