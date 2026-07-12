# Campaign: C-futon1b-features — the turn-annotation & label contract on the futon1b store

*Everything we are trying to do with futon1b, rounded up — and the one thing
all of it shares.*

**Tier:** Campaign (TAI; spec `futon4/holes/campaign-lifecycle.md`).
**Date:** 2026-07-12 — **Phase: CHARTERED** (RALLY ratified by Joe,
2026-07-12, emacs-repl: "let's ratify the RALLY" — all lanes incl. the
reflection lane; the §2 criterion stands as drafted, revisable at
CONSTITUTION review).
**Principal:** Joe. **Coordination + review:** claude-5 (laptop).
**Chartered from:** operator memo (Joe, 2026-07-12, emacs-repl: "assemble a
Campaign C-futon1b-features that rounds up everything that we're trying to do
with futon1b") + the M-mission-conditional-reward verdict (label mass, not
features, is binding).
**Built to end:** dissolves when the annotation contract is verified fit for
all consumers and consumed live at least once end-to-end; the member missions
remain as residue.

## 0. Orientation — what is actually going on (read this first)

The futon1b switchover is DONE: the laptop store server runs on :7073 with
the D1 FTS5 text index LIVE (verified 2026-07-12); futon3c on :7070 consumes
it as the evidence backend; lucy is switched. On top of that store, four
lanes are in flight, and they all want the same thing from it:

**turns (and spans within turns) acquiring TYPED STRUCTURE — tags, marks,
referents, labels — that downstream consumers can query and learn from.**

| lane | mission(s) | status | next slice | gate |
|---|---|---|---|---|
| substrate | M-futon1a→1b migration | switchover DONE | juncture; IRC dedup (E-evidence-flow Q1–Q5) | Joe (quiet windows) |
| text index | M-text-sidecar / XTDB#5637 | D1 LIVE on :7073 (laptop + lucy) | D2 evidence packet; P3 sync; P4 query surface | D2 send = Joe |
| marks / tagging | M-points-de-fuite §6.5 (+ B0) | hydra built (✘/✓/💡, C-c .); recognizer UNBUILT | **L0 = B0 turn-end recognizer → typed tags** | build; then vocab growth = Joe |
| labels | M-marks-to-labels (drafted 2026-07-12) | DRAFT, not armed | L1 referent resolution; L2 label adapter; **L3 measurement gate** | arming = Joe; flag flip = Joe |
| zaif runners | M-zaif-harness | PZ1–3 closed; U1 done; B1 γ(mission) built | **Z1 query library** (pure XTQL); Z2 controller; Z3 A/B | Z-lane arming = Joe |
| reward (consumer) | M-mission-conditional-reward CLOSED (honest negative) + the batch loop | verdict = label mass binding | consume labels when L3 passes; batch-8 continues under v1.2 | scoreboard flips = Joe |
| reflection (consumer) | M-joe-reflection (drafted 2026-07-12) + N-peircean-codebook | IDENTIFY draft; lane RATIFIED 2026-07-12 with the rest | J0 self-evidence census; J1 operator-corpus retrospective (no-life-logging: the turns ARE the stream, read via D1) | arming = Joe; operator-corpus reads Joe-consented per slice |

Supporting, already live: autoclock witness (attribution of record) ·
U1 transcripts (the ✘ referent substrate) · B1 marks→γ(mission) fold ·
30-min Agency job-cap discipline (background long verifies).

**Standing fact (found 2026-07-12, Z1 review): the marks corpus is SPLIT
across stores.** Stores do not replicate; the first ✓ (e-63c25e11) lives in
lucy's store, the first 💡 in the laptop's. Any γ-ledger/label-mint read must
name its store or query both; the L3 measurement gate should state which
corpus it measured. Reconcile at the migration lane's juncture, not ad hoc.

## 1. The shared standard (the Campaign's reason to exist)

**The turn-annotation & label contract:** one agreed shape for
(a) typed tags on chat-turn evidence (:correction/:approval/… + parsed :ref
    + optional span), minted by the B0 recognizer under the use-vs-mention
    rule (b1-live-marks.edn);
(b) referent resolution (explicit :ref > quoted-span via D1
    candidates+re-check > in-reply-to turn — the P2
    candidate-generator-never-authority pattern);
(c) label records: fold_ground_truth's {used, want, success} shape + :grain
    (:operator-mark | :deposit-psi | …) + confidence + replayable provenance;
(d) the read surfaces: text-search (:7073) as candidate generator, XTQL
    hydration as authority; memory-search :text through the standard envelope.

Why no single mission can own it (the campaign-grounding test): the zaif
controller's C-inference, the γ(mission) ledger, R̂'s label loader, the
session-mode surfaces, the clustering probe — and now M-joe-reflection,
which consumes the SAME tags backward as self-model evidence (dual-use:
one mark channel, forward to agents and backward to the operator) — ALL
consume these shapes. Any
one mission defining them alone mis-fits the others; leaving them implicit
forks them (the thin-scalar-field failure, war-bulletin-10).

**Keystone:** M-marks-to-labels (delivers a–c). The D1/P4 query surface (d)
is co-keystone from M-text-sidecar. All other members are consumers.

## 2. Joint completion criterion (draft — Joe ratifies at CHARTER)

The Campaign dissolves when:
1. the contract (a–d) is STANDARD-VERIFIED against every member's stated
   requirement (CONSTITUTION survey below), AND
2. one live end-to-end consumption is witnessed: an operator mark made in
   flow → typed tag (L0) → resolved referent (L1) → BOTH a γ(mission) event
   (B1) AND a :grain :operator-mark label ingested behind the flag with the
   L3 measurement gate RUN (its verdict may be negative — the gate running
   honestly is the criterion, not the label channel winning), AND
3. Z1's query library reads the same events through the standard envelope
   (proving the contract serves the zaif lane, not just the reward lane).

## 3. RALLY — muster & lanes (RATIFIED by Joe, 2026-07-12)

- **Joe** — operator: arming, flag flips, D2 send, quiet windows, mark
  vocabulary growth (names the clusters), gold passes.
- **claude-5 (laptop)** — coordination, review gate (CONTROL-LAYER
  discipline), fix-don't-re-bell; author of last resort when Agency is quiet.
- **codex-2** — coding handoffs (L1/L2, Z1 slices); **job-cap discipline:
  any verify step >~20 min must background + report by artifact.**
- **zai fleet** — runner work per RUNNER-CONTRACT (clustering probe is a
  ready handoff; batch loop continues in parallel, unblocked).
- **lucy claude line** — B0's Emacs/elisp half lives in shared futon3c;
  either box can build it, one owns it (avoid double-build).
- **Coordination protocol:** scope-bounded handoffs with :have→:want packets
  (the H1–H4 shape); external adjudication; crossed-bell recovery = whistle.
- **Operator checkpoints (pause-and-surface, never decide):** arming any
  member slice; anything that writes to the store schema; anything outward
  (the D2 packet); scoreboard/label-flag flips; restarts.

## 4. CONSTITUTION (claude-5, 2026-07-12 — survey from the member docs, all read this session)

**Dependent-mission survey** (needs-from / contributes-to the standard):

| member | needs from the contract | contributes |
|---|---|---|
| migration (substrate) | nothing upstream — sits below the contract; schema writes remain an operator checkpoint | the store itself; append-only evidence semantics; bitemporal audit |
| M-text-sidecar (co-keystone) | tags-not-glyphs as the search key in (a) (FTS strips ✘/✓ — the tokenizer finding is normative) | D1 index (live, both boxes); the P2 pattern; P4 query surface; D2 packet evidence |
| M-points-de-fuite / B0 (producer) | a tag-vocabulary REGISTRY in (a) (grown by the clustering probe, operator-named) + use-vs-mention as a normative rule | the recognizer; the hydra; the vocabulary process |
| M-marks-to-labels (keystone) | span addressing from (d) for L1's precedence rung 2 | delivers (a)–(c); the L3 measurement protocol |
| M-zaif-harness (consumer) | (a,c) event shapes + (d) envelope for Z1's views | γ fold (B1); U1 transcripts; asymmetric admissibility; PZ1 measurements |
| reward loop (consumer) | (c) grain-tagged labels behind a flag; L3 gate before any flip | fold_ground_truth as the label-shape authority; flown-fold labels as calibration anchor |
| M-joe-reflection (consumer) | (a,d) verbatim + consent-scope RECORDED on operator-corpus reads; backward-readability (💡 events preserved though they mint no labels) | QA surfaces (the walk requirement); the dual-use argument that hardens (a) |

**Governance:** STANDARD-VERIFY ratifier = Joe · coordination = claude-5
(laptop) · dissolution = Joe · keystone owner = claude-5 until an armed
handoff reassigns it. **Coordination shape: convergent** (all lanes converge
on the keystone contract; no sequential chain between consumers).

**Ready vs missing** (campaign scope):

| ready (exists, verified) | missing (the campaign must produce) |
|---|---|
| store + D1 live on both boxes (:7073 laptop, verified 2026-07-12) | B0/L0 recognizer → typed tags |
| marks hydra; use-vs-mention rule recorded | tag-vocabulary registry (seeded ✘ ✓ 💡; grown via clustering probe) |
| autoclock witness; U1 transcripts; B1 γ fold | L1 referent resolution; L2 label adapter + flag |
| loader :grain discipline; PZ1 numbers; job-cap discipline | L3 gate run; Z1 query library; consent-scope recording; Q5 render; memory-search :text envelope verified laptop-side |

## 5. ESCROW ledger (registered at CONSTITUTION; release = held → contract-released → satisfied)

| from | on | requirement | status |
|---|---|---|---|
| M-marks-to-labels L1/L2/L3 | tag shape (a) via L0 | tags queryable, refs parsed | **held** |
| zaif Z2 controller | Z1 views + shapes (a,c) | events readable through the envelope | **held** |
| B2 kernel-vs-marks triangulation | mark accrual (L0 live) | B1 cells ∩ reward missions populated | **held** |
| R̂ label-channel ingestion | L3 gate PASS | agreement + S3/LOMO non-degradation | **held** |
| reward follow-on (LOGO τ) | label mass accruing | else it re-fights the last war | **held** |
| M-joe-reflection J-surfaces | (a) + (d) verified | contract consumed verbatim; render + consent only | **held** |

Release authority: STANDARD-VERIFY (Joe) releases contracts; live delivery
satisfies them. Armable NOW without waiting on the standard (they produce
it): L0 recognizer · clustering probe · Z1 first views · J0 census.

## 6. Log

- 2026-07-12 — Campaign assembled at RALLY (claude-5 laptop) from the
  operator memo; orientation table + shared standard + draft criterion
  written.
- 2026-07-12 (claude-2): added CANDIDATE reflection lane — M-joe-reflection
  (chartered by Joe this session: "what's-in-it-for-me" as HEAD; no-life-
  logging constraint; composes N-peircean-codebook + the Joe-loop R-audit).
- 2026-07-12 — **RALLY RATIFIED by Joe (all lanes incl. reflection) →
  CHARTERED.** CONSTITUTION survey + governance + ESCROW ledger registered
  (claude-5). Next phase: STANDARD-ARGUE (why this contract fits every
  consumer) — largely written across the member docs; needs assembly + the
  one cross-check no member ran: that ONE tag shape serves both directions
  of the dual-use read. **← Joe: arm from the armable-now list (L0 ·
  clustering probe · Z1 first views · J0 census), any subset.**
