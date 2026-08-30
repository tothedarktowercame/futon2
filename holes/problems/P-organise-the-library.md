# P-organise-the-library — a spider fleet that gives the pattern library its `@why`/`@how` graph

**Status:** DRAFT problem record, 2026-08-30 (claude-15, from Joe's proposal). Under
`futon4/holes/delivery-lifecycle.md` v2. S1 fields drafted from Joe's words and marked
his to confirm. **Not dispatched.** Companion to `P-validated-R5.md` §3e: this is the
work that makes `find` have a search space and `organise` have edges to fast-forward.
**Gate:** operator-acceptance — Joe confirms §1 and the pilot in §5.

## 0. In Joe's words

> *"We could set up some sort of web spider agents that would run around inside the
> library and add these why/how edges based on reading the library, and we'd rather
> quickly have it fully organized. This could be a great job for the Zai agents — a
> little fleet of them to organize the library by section. They might suggest some
> patterns should be retired or made into super-specialized edge cases. This is
> basically the same concepts we've been working on in the memory white paper, applied
> to a much bigger collection of patterns. And there's a recent Fable tech note we could
> take as inspiration for how to get this job done nicely."*

The tech note: `futon3c-library-loop-production/holes/technotes/TN-fable-library-loop-review.md`
(2026-08-24) — its *Build plan — the simpler-but-better loop*: files-only runner state,
cheap per-turn gates, a strategy checkpoint on a fixed cadence, a success ledger, and a
kill/restart contract with a precise oracle. Applied here to a library rather than a
theorem bank.

## 1. The problem record (S1 — Joe's fields)

```
problem:   The pattern library is the repository `find` searches and `organise` builds
           cascades from, and it is 93% unconnected: nothing states what most patterns
           stand on or what carries them out, so a cascade can only be hand-authored.
                                                                        [Joe: confirm/rewrite]
now:       futon3/library, measured 2026-08-30 (path-shaped targets, comments stripped):
             1,239 flexiargs · @why: 73 directive lines = 82 path-shaped targets (edges) ·
             @how: 3 lines = 10 edges · @see-also: 46 lines = 77 edges · 13 @holds-*
             91 patterns in the authored @why graph (7.3%) · every edge resolves · 0 cycles
             (RECONCILED 2026-08-30 by codex-20's baseline and claude-15's recount: the first
             figures here — 77 / 10 / 46, 85 — mixed target counts with line counts and used a
             lowercase-only target regex that dropped `math-formalization-CA/`-style dirs;
             the linter's :baseline-note records the reconciliation)
           The standard: README-flexiarg.md §5a — @why "the pattern's own author";
             @how "an editor, later"; @see-also "either"; "a post-hoc @why is a trace, not
             a cause … a hypothesis … mark post-hoc edges inline"; "the rationale layer can
             only ever be authored, never harvested."
           Precedent for the method: TN-fable-library-loop-review §Build plan.
           Precedent for the semantics: futon3c.peripheral.memory-recall — "an agent-authored
             endpoint is a curation proposal, not yet a warrant."

solved:    (a property of the library-as-graph, checkable by a linter before and after any run)
           1. every @why/@how/@see-also target resolves to a file            (holds today; must keep holding)
           2. the @why graph is acyclic                                        (holds today; must keep holding)
           3. a spider writes the SIMPLE directive — `@why <pattern>` — with no inline
                annotation (Joe, 2026-08-30: "let's just go with the simple why with a
                link to the pattern"); everything about the edge that is not the edge
                lives in a separate, readable-back EDN file beside the section
                (`library/<dir>/attestations.edn`), one record per edge: who, when,
                what was read, the cited text, and the evidence-landscape hits
           4. every spider-written @why AND @how has an attestation record in that file with a
                state {proposed | attested-by <who> | refused}; a refused edge keeps its
                record and loses its directive line; §5a's "mark post-hoc edges inline"
                is satisfied by the sidecar, not by the line — Joe's call, and it keeps
                the flexiarg itself clean
           5. retire / specialise suggestions are typed proposals in a
                ledger, not edits to the pattern
           6. the fraction of patterns in the authored graph is reported per
                section, per run, against the 6.9% baseline — and a run that
                raises it by adding edges that fail 1–4 counts as zero
                                                                        [Joe: confirm the marking in 3; choose the directive form]

facades:   harvested @why presented as authored — refused by 3 (the exact facade §5a names)
           inferred edges (similarity / co-occurrence / embedding nearness) presented as
             standsOn — refused by O2 of P-validated-R5 §3e; a spider must cite text
           a cycle introduced to "connect" a section — refused by 2
           a count ("N edges added") offered as "organised" — refused by 6's receipts rule
           a spider editing pattern text to make an edge fit — refused by 5 (proposals only)
           the whole library done in one run with no checkpoint — refused by §4's cadence

owner:     joe (commissioner); fleet = Zai seats (32 on roster; zai-2 idle; overnight_zai_flight.sh
           is the fleet precedent); linter + runner = one Codex packet; review = Claude seat,
           sampling, ≠ author
status:    open — packet 1 DELIVERED and REVIEWED 2026-08-30: futon3 84ca5dd (checks/library_graph_lint.clj,
           library/.spider/{attestation-schema,baseline-edges,ledger}.edn, test/library_graph_lint_test.clj).
           Review (claude-15): diff read; live linter run on both sections → pass, 0.16 s; fixture tests
           re-run → 2 tests / 8 assertions / 0 failures (bb -cp .); clj-kondo 0/0; check-parens run;
           baseline counts reconciled (above); ledger aif 31/27 tagged, writing-coherence 23/22.
           Two notes carried into packet 2, not fixed: (i) a NEW .flexiarg in a section fails as
           :argument-body-not-in-baseline — right for spiders, to be revisited when R17‴ mints;
           (ii) directive lines must sit in the header block (a leading-space directive is digested as body).
           packet 2 DELIVERED 2026-08-30 13:06Z (futon3 3db55f3, codex-20; seats zai-2 for aif, zai-1 for
           writing-coherence) and REVIEWED by claude-15 (futon3 f8c2c2a):
             aif/: 31 patterns processed; @why 4→6, @how 0→11, @see-also 0→19; organised 12.9%→18.2%;
               28 attestations (all rung 1), 7 absences, 0 proposals.
             writing-coherence/: 23 processed; @why 0→2, @see-also 0→1; organised 0→8.7%;
               3 attestations (all rung 2), 20 absences, 0 proposals.
             Kill/restart: killed in :turn-running on turn 1, orphan Agency job cancelled, resumed; 0 duplicated
               directives or records (checked). Linter green both sections before and after the review.
             Review: all 31 citations and evidence excerpts re-verified mechanically (normalised matching —
               a raw substring check falsely fails 8 on line-wrapping/escaping); sample judged against §5a:
               9 attested, 3 sent back (kind errors ×2: a precondition or co-located peer written as @how;
               unconnected evidence ×1), 1 refused (cited text does not mention the target; reverses another
               edge) — directive removed, record kept. Ledger attestation-states updated.
             Runner findings for the fleet (not yet fixed): (i) 4 of aif/'s 7 absences are "seat returned
               malformed output after two attempts" — a Zai failure, to be re-run, not an absence; (ii) rung 1
               is recorded with :via :text (the runner logs the ROUTE; the schema meant the KIND of hit) —
               decide one reading and enforce it in the linter; (iii) the spider wrote 26 @how/@see-also to
               2 @why in aif/, which is the editorial standing §5a gives it — and the two kind errors were
               both @how, so the fleet prompt should carry §5a's definitions verbatim.
           FLEET APPROVED 2026-08-30 (Joe: "scale the spider programme in parallel"), in this order:
             packet 3 (codex-20) — the four fixes + a fleet driver: rung 1 = exact id-string occurrence in the
               evidence EXPORT [CORRECTED 2026-08-30, see below: the parenthetical that stood here — "the live
               /text-search route ranks hyphenated tokens and never surfaces the id" — was an INFERRED cause
               claude-15 wrote without testing the route; it is false]; WR-NN alias for war-room/; rung semantics enforced in the linter; seat-failures
               counted apart from absences with two retries; §5a verbatim in the prompt; fleet driver with a
               serialised acyclicity gate, zai-* generic seats only, two-line ledger (attested vs proposed).
               job invoke-1788096823513-3938-571648fe, park park-7664f035-21f1-4e60-862b-e2ce4a8eb994.
             wave 1 (packet 4, after packet 3's review): war-room/, problems/, features/, aif/ re-run — the
               sections that hold at R-nodes.
             wave 2: the 100%-tagged operational families (ukrns, vsatlas, storage, system-coherence, structure,
               sidecar, realtime, musn, peripherals, or2, war-machine, plos-npt…) — ~180 patterns.
             wave 3: the 0%-tagged families (or3, nomad, mmca, memory, math-formalization-CA/CV,
               math-informal-CT/CA) — rung 2 only; last, or after the memory-whitepaper search improves.
             Review at scale: linter + mechanical citation/excerpt verification on EVERY record; a semantic
               sample of 10 per section split between a Claude seat and a Codex seat; only sampled-and-passed
               edges become attested; the organised fraction is always reported in two lines.
deliveries: none
```

## 2. Gate 0 — the terms this job uses, and their class

| term | class | source |
|---|---|---|
| `@why`, `@how`, `@see-also`, `@holds-at/-open` | **theory-defined** (the standard states direction, meaning, and who writes) | `README-flexiarg.md` §5a |
| post-hoc `@why` | theory-defined: "a hypothesis … should earn attestation … mark inline" | §5a, 2026-08-23 |
| spider | **undefined** — this record defines it: an *editor* in §5a's sense, so it may write `@how` and `@see-also`, and may only *propose* `@why` | here |
| attestation | defined in `forward-model/graduate-strategy-by-outcome` (the register §5a cites); `memory-recall`'s "curation proposal, not yet a warrant" | existing |
| "organised" | **defined here as a measure**: fraction of patterns in the resolving, acyclic authored graph — not an edge count | `solved` 6 |

The gate passes once Joe confirms the spider's editorial standing and the inline
marking form for a post-hoc `@why` (§5a asks for it and does not fix the syntax).

## 2b. The attestation source — the evidence landscape's pattern tags (Joe, 2026-08-30)

> *"All they'd have to do is say: here's a place where that pattern name has been used
> as a tag, which has been rendered by the embedding. For those that have been used as
> tags, they might rather quickly find attestations from the evidence landscape that
> relate to those things being used for some rationale."*

So the spider does not invent a rationale; it **finds where the pattern was already
used as a tag on evidence** and reads what the evidence says the pattern was for.
That is F3 of `P-validated-R5` §3e (non-self-certifying: cite something outside the
finder) with the evidence landscape as the citation source, and it is the
memory-whitepaper move — pattern-conditioned recall — run over patterns instead of
memories.

**Measured 2026-08-30, `futon1b/migration-export/evidence.edn`, pilot section `library/aif/`
(31 patterns, not 27):** 27 of 31 appear as tags — `expected-free-energy-scorecard` 42×,
`niche-construction` 19×, `measurement-window-hygiene` 17×, `evidence-precision-registry`
16×, `no-self-certification` 14× … down to 1× for five patterns; **four have no tag at
all** (`two-layer-calibration`, `structure-learning-by-model-reduction`,
`posterior-variance-as-epistemic-value`, `off-continuity-null-discriminates`). The premise
holds for 87% of the section, and the four zeros are the first four typed `Absence`
records — patterns with no usage evidence to attest an edge from — before any spider runs.

```
Attestation := { edge      : { from : Pattern, to : Pattern, kind : why | how | see-also }
                 by        : AgentId
                 at        : Date
                 read      : List Pattern                 -- what the spider read to propose it
                 cited     : String                        -- the sentence in `from` that warrants `to`
                 evidence  : List { id : EvidenceId, via : tag | text, query : String, excerpt : String }
                 rung      : 1 | 2                                  -- tag hit | free-text hit
                 state     : proposed | attested-by AgentId | refused
                 reason    : Option String }               -- on refusal
```
**Untagged patterns: keyword search, not silence (Joe, 2026-08-30).** *"Anything that
doesn't have a tag, they could use the free-text search method to look for keywords
that would provide other evidence — the embedding method is great, but a keyword-based
method is also now possible."* So the attestation ladder per pattern is:
1. **tag hits** — the pattern id as a tag on evidence (27 of 31 in `aif/`);
2. **free-text hits** — the evidence store's FTS route (`futon1b`, `/text-search`,
   `README.md:399`) and the harness `memory_search` surface
   (`futon3c/agents/memory_mcp.clj:148` → `memory-backend/search-queries` → `store/query*`:
   tag matching first, then a subject fallback), which Zai seats already hold via
   `zai_memory_1b.clj` (`memory-search`); the spider searches the pattern's own title
   and its `!conclusion` keywords and records the query in the attestation;
3. **`Absence`** — only after both return nothing: "no usage evidence, by tag or by
   keyword, for <query>". The four `aif/` zeros are candidates for rung 2 before
   they are absences.
A rung-2 attestation is weaker than a rung-1 one and the record says which rung it
came from (`:rung 1 | 2`), so "was this useful" can be asked per rung.

One EDN vector per section, append-only, readable back — so "was this actually
useful" is a query over the file, not a recollection.

## 3. The spider's contract — what one turn does

A spider reads **one section** (a `library/<dir>/`), one pattern at a time, and may emit:

```
Edge      := { kind : why | how | see-also, from : Pattern, to : Pattern,
               provenance : { agent, date, read : List Pattern },   -- what it read to say this
               evidence : String,                                    -- the sentence(s) in `from` that warrant it
               post-hoc? : Bool,                                     -- true for every spider @why
               attestation : proposed }
Proposal  := { kind : retire | specialise | merge | split, pattern, reason, evidence }
Absence   := { pattern, note : "no authority found in this section" }   -- typed, not silence
```
- A spider **never edits** a pattern's argument body; it appends directives (or, for
  `@why`, appends a marked proposal line) and writes proposals to the section's ledger.
- Every emitted edge cites text (`evidence`); an edge with no citation is refused by the
  gate, which is F3 of §3e ("non-self-certifying") at the library.
- Cross-section edges are allowed (most `@why` targets point at `war-room/`, `structure/`,
  `forward-model/`) but the target must exist; a wanted-but-missing target is a `Proposal
  {kind: specialise|split}` or an `Absence`, not a dangling edge.

## 4. The runner — the Fable note's shape, at library grain

Following `TN-fable-library-loop-review` §Build plan, with the ceremony that earns its keep:

```
per section (one Zai seat, one runner process, files-only state under
  futon3/library/<dir>/.spider/{state.edn, gates/, ledger.edn}):
  loop (budget: the section's pattern count × 2 turns):
    1. one turn: read the next pattern (and what it cites), emit Edges / Proposals / Absence
    2. turn gates (cheap, local, every turn) — the LINTER:
         targets resolve · @why graph acyclic (whole library, not just the section) ·
         every new edge has provenance + evidence + post-hoc flag · no body edits
       a failed gate feeds the next prompt; two consecutive failures on one finding → :paused
    3. every 20 patterns: CHECKPOINT — the spider restates, in its ledger, what the
       section is *about* (its own reading), and the fraction organised so far;
       an unchanged restatement across two checkpoints pauses the run (the TN's valve)
    4. cooldown; stall pager as in codex-autowake
state transitions: :turn-ready → :turn-running → :gating → :turn-ready;
  every 20: :gating → :checkpoint-ready → :review-pending → :turn-ready;
  any → :paused; only a review disposition leaves :paused.
  write-temp + fsync + rename; on restart reconcile receipts before resuming;
  never infer a turn landed because the file looks edited.
```

**Success ledger** (`futon3/library/.spider/ledger.edn`): per section — patterns,
edges by kind, post-hoc `@why` proposed / attested / refused, proposals by kind,
fraction organised before/after. Only *attested* `@why` and gate-passing `@how`
move the library-wide number; proposed edges are counted separately, as the
memory-whitepaper rule says ("a curation proposal, not yet a warrant").

**Review** (≠ author): a Claude seat samples N edges per section against the
cited text and either attests, refuses with a reason, or sends back. The sample
size and N are §5's arguments.

## 5. The pilot — one section, one Zai, one review, then decide

Following the lifecycle: no fleet until one run has been shown to work.

- **Sections:** `library/aif/` (31 patterns, the War Machine's own vocabulary — where
  a wrong edge does the most damage) **plus two or three others of different shape**,
  for comparison — Joe: *"a few other sections so we get a comparison across different
  libraries rather than focusing on one, which might be misleading in its shape."*
  Candidates by contrast: `process-coherence/` (workflow patterns, heavily tagged),
  `math-informal-*/` (a family that already carries `@why` edges — the one place `@how`
  exists), and `war-room/` (the authority layer most `@why` edges point at, and where
  §5a's post-hoc warning was written). Same runner, same linter, same review; the
  ledger reports each section against its own baseline.
- **Decided 2026-08-30 (Joe): pilot = `library/aif/` + `library/writing-coherence/`.**
  `writing-coherence/`: 23 patterns, 22 of 23 tagged in the evidence landscape (204 hits),
  zero `@why` today, a self-contained prose family of a different shape from `aif/`;
  its one untagged pattern is the rung-2 (keyword) path's single test. Alternate:
  `storage/` (21, all tagged, 374 hits).
- **Two Codex packets, in series** (handoff rule: one behaviour, one acceptance test each):
  **packet 1 — the gate**: `library_graph_lint` + the attestation schema + a
  **baseline snapshot** of today's edges (`library/.spider/baseline-edges.edn`: the 77
  `@why`, 10 `@how`, 46 `@see-also` as of 2026-08-30 — author-written, exempt from
  attestation; every edge added after must have a record) + the baseline ledger for the
  two sections. **packet 2 — the runner and the pilot**: the per-section runner in the
  Fable note's shape, two Zai seats, the review sample, and the two ledgers after — Codex
  organises the Zai seats (Joe, 2026-08-30). Packet 2 is not sent until packet 1 is
  reviewed and its numbers match the measurements in this record.
- **Seat:** `zai-2` (idle) and one more, chosen by Codex in packet 2. **Runner + linter:** two Codex packets (write the linter
  first; it is the gate for everything after). **Review:** a Claude seat, N = 10 edges.
- **Acceptance** (S1 `solved` 1–6 on one section): linter green before and after; every
  edge cited and marked; proposals in the ledger; fraction organised for `aif/`
  reported against its own baseline; the run killed once mid-turn and restarted
  without a duplicated edge (the TN's oracle).
- **Refusal permitted:** "this section's patterns state no authority I can cite" is a
  legitimate result for a pattern, and the `Absence` record is its deliverable.

Then, and only then, the fleet: sections ordered by how many `@holds-at` nodes
they touch (the ones the contract reads first), each with the same runner, the
library-wide acyclicity gate shared.

## 6. What this record does not decide

~~The inline syntax for a post-hoc spider `@why`~~ — **decided 2026-08-30: none.**
The directive stays `@why <pattern>`; the sidecar `attestations.edn` carries the
provenance and state, and is what §5a's "tell them from an author's own `@why`" is
answered by (a reader checks the file). This should be written into
`README-flexiarg.md` §5a as an amendment when the pilot lands. **Also decided 2026-08-30: `@how` written by a spider needs attestation in the same
sidecar, in the same way** (Joe: "yes, I think the how links need attestation in a
separate file in the same way"). So the schema's `kind` covers `why | how | see-also`
uniformly, and the ledger counts attested `@how` separately from proposed. Still open: `@how` written by a spider needs attestation
too (§5a says "an editor, later" and does not require it — the record leans yes,
for the same reason as `@why`); and the review sample size. It records that the
job is possible today against a graph that is already acyclic and fully
resolving, that its measure exists (6.9%), and that the one thing the standard
forbids — harvesting the rationale layer and presenting it as authored — is
exactly what a fleet would do by default unless the marking rule is built into
the gate before the first spider runs.

## 7. After the graph exists: the War Machine reorganises it in the loop (Joe, 2026-08-30)

> *"Once we get this defined as a global graph, we can always update it and reconnect
> it — we don't have to have it fixed once forever. It can be reorganized in the loop by
> the war machine so it stays balanced, so that nodes remain reachable, so that memories
> or patterns which are retrieved get some kind of certification — were they actually
> useful, or were they irrelevant? And I think that points towards the formal
> specification of what the GFlowNet experiment would have been written to, had it been
> written to a formal specification."*

**The operation, typed.** The spider fleet builds the graph once; this is the policy that
maintains it — a policy on the *repository*, one level up from `find`/`organise`
(the Sierpiński reading of `P-validated-R5` §3d, applied to the library itself):

```
Certification := { pattern : Pattern, cascade : Cascade, run : RunId,
                   verdict : useful | irrelevant | harmful, from : Outcome, by : Witness }
                                                      -- issued from a REALISED outcome (R16), never by the retriever
reorganise    : Repository → List Certification → Repository
```
**Laws** (each a facade refused; each checkable by the packet-1 linter plus one more check):
- R1 *reachability preserved or improved*: every pattern reachable in the `@why` graph before
  is reachable after; a pattern that becomes unreachable is a typed `Proposal {retire}`, not
  a silent drop. ("nodes remain reachable")
- R2 *acyclic preserved*: `acyclicDescent` before ⇒ after. ("stays balanced" — `hasMeets` is
  reported, not required, per §2.1d)
- R3 *edges move only on certification*: an edge is added, strengthened, weakened or removed
  only with a `Certification` citing a realised outcome; a retrieval that was never enacted
  certifies nothing (R9's L1/L2: a retriever grading its own retrievals is L1 and may not
  move the graph).
- R4 *certifications are the attestation ladder's third rung*: `attestations.edn` gains
  `state: certified-by <run>` above `attested-by`, so a spider's proposal, a reviewer's
  attestation, and the loop's certification are three distinct evidence kinds in one file.
- R5 *the retriever is falsifiable*: for a certified-irrelevant pattern, a later `find` on
  the same tension must rank it lower or say why not — F4 of `P-validated-R5` §3e, now
  with the certification as the falsifier's source.

**The formal target the GFlowNet experiment lacked.** The slush (glossary; `sec-catalog`
R16 → R6) samples cascades ∝ exp(β R̂) where R̂ is *aliveness credited back to the patterns
a mission used*. Read against the laws: its **output object** was never typed as a
`Repository` edit or as a proposal distribution *over a graph with reachability
invariants* — it sampled compositions from a bag; its **reward** was aliveness-as-proxy
(Salingaros's T·H) rather than a `Certification` from a realised outcome; and it had **no
invariant** to keep (nothing said what a sample must not do to the graph). The paper's own
footnote reports what that produces: *"the trained proposer did not beat the
retrieval-prior incumbent on success rate … What it did win on decisively was diversity."*
Diversity is what you get when the reward is not a certification: the sampler learns to
spread over the bag, because nothing tells it which edges were *useful*. Written to this
section, the experiment would have been: `reorganise` with R̂ := certification frequency
per edge, evaluated by R1–R5 on the graph and by the falsifier in R5 on the next `find`
— and its preregistered outcome would have been a change in `find`'s ranking on
certified-irrelevant patterns, not a success rate over forty builds.

**Where it sits in the loop — and a finding.** The catalogue names the edge (*R16 → R6,
Discharge-Trained Cascade Proposal*) and R17 (*Structure Learning by BMR*). **Figure 4 draws
neither**: in `control-map-edges.edn` (lifted 2026-08-30) R17 is in the node set and in no
edge, and nothing feeds R6 except R5's `rank`. The learning band is disconnected from the
loop as drawn. `reorganise` is the missing edge, R16 → R6 (or R16 → R17 → R6), and it is
registered in `PREREG-war-machine.md` §2 as a required-but-undrawn edge with the
invariants above as its `Delivery` contract.

**PACKET 3 REVIEWED 2026-08-30 (claude-15) — passes with one finding that gates wave 1.**
futon3 `2761dc0` (codex-20) + review commit `f95bc65`. Checked: diffs to the linter, the runner and
the new fleet driver read in full; fixture tests 2/9/0 run here; live lint green on `aif/` and
`writing-coherence/` (the new excerpt re-verification passed on every record); kondo 0/0; parens OK;
§5a table in the seat prompt compared with `README-flexiarg.md:168–172` character for character.
The four fixes are as specified: rung 1 = exact token occurrence in an export record (mtime-keyed
index, `WR-NN` alias for `war-room/`), the seat may cite only runner-supplied hits; the linter refuses
rung 1 without `:via :tag` and rung 2 with it; malformed output gets two schema-guided retries with
raw responses kept and exhaustion goes to `seat-failures.edn` (7 events preserved) — never an
absence; the pilot's 20 rung-1 records were reclassified to rung 2 because their route was `:text`
(honest; my 7 attested states untouched). Fleet driver: one future per section, `flock` on
`library/.spider/lint.lock` for the library-wide gate, `zai-[0-9]+` seats only, attested and proposed
counted apart. One behaviour change not in the packet, accepted: `@why` items are recorded as
proposals and never written as directives (`@how`/`@see-also` still are) — §5a's "who writes it".
The four aif seat-failure cases re-ran: `admissibility` → a typed absence naming both queries;
three → new `@see-also` edges (rung 1).

*Finding (the gate).* Of those three, **two rest solely on `context-retrieval` records** — an
embeddings ranking that lists both ids. Measured over the index: retrieval listings supply 18 of
`aif/`'s 26 rung-1 hits (8 real), 6 of `writing-coherence/`'s 22 (16 real — the ≥15/23 acceptance
holds on real records), 0 of `war-room/`'s 10 (the `WR-NN` hits are all genuine); the export holds
8,844 such records. Co-occurrence in a similarity list is not a stated relation (§3e F3) — it is the
sent-back reason from the pilot ("a query-log line mentioning the ids, not usage") reappearing under
a rung-1 label. Review of the three: `evidence-precision-registry → exotic/full-lift-registry`
**attested** (a chat turn names them as prior-art peers); the other two **refused**, directive
lines removed, lint green.
*Second finding.* `writing-coherence/` was not re-run: the fleet file's `:status :done` is the pilot's
`state.edn` phase, and its 20 absences were produced by a run in which rung 1 was never performed (see correction below) — stale
instrument-produced absences. `:evidence-tag-coverage 22` in the ledger is the index, honestly
labelled, not spider output.
*Decision.* **Packet 4a (fix, before the fleet):** hits from `context-retrieval` records are tagged
`:listing true`; a rung-1 attestation needs at least one non-listing hit (validator), and the prompt
says a listing hit is not a warrant by itself; the coverage report prints real vs listing rung-1 per
section. **Wave 1 (packet 4b, after 4a's review):** `writing-coherence/` re-run in full, `aif/`
re-run for the patterns whose attestations rest on listing hits, `war-room/`, `problems/`,
`features/` — four `zai-*` seats.


**STATUS 2026-08-30 14:10Z:** packet 4a dispatched to codex-20 — job `invoke-1788098998480-4005-8fcd56dc`, park `park-07ecfbb0-a590-403d-9bc3-bbda13745045` (deadline +55 min). Wave 1 (4b) after 4a's review.

**PACKET 4a REVIEWED 2026-08-30 (claude-15) — passes.** futon3 `5052960`. Checked: diff read
(runner +53, fleet +19, linter cache name, two tests); index deleted and rebuilt here — coverage
any/non-listing: `aif/` 26/8, `writing-coherence/` 22/16, `war-room/` 10/10, `problems/` 0/0,
`features/` 0/0, exactly my measurement; runner fixture 1/4/0 and it exercises the behaviour
(listing-only → no warrant, stated → warrant, both body encodings classified); linter fixtures
2/9/0; live lint green on both sections; kondo 0/0 over code and tests; parens OK; `--budget 0`
fleet dry run writes the per-section coverage line (file restored after). Listing-only rung-1
evidence is rejected by the validator with `:listing-only-rung-one`; the prompt says a listing is
context, not warrant. In `aif/attestations.edn` the only listing-based records are the two already
refused. **Wave-1 decisions from the data:** `aif/` is re-run in full (its pilot output was produced
by a run in which rung 1 was never performed, like `writing-coherence/`'s absences; attested records stand, duplicates
are rejected); the roster holds two generic seats (`zai-1`, `zai-2`) — the earlier "twenty"
counted APM's `f5x-*` role seats — so the fleet runs detached from the dispatch job and is polled
from the fleet file.


**STATUS 2026-08-30 14:23Z:** packet 4a passed. **WAVE 1 launch (packet 4b)** dispatched to codex-20 — job `invoke-1788099791017-4007-e113c9d0`, park `park-afdb8659-c300-4e81-bfc9-f29482e44965` (deadline +60 min): writing-coherence 23 + aif 33 (both full re-runs, pilot state preserved aside) + war-room 28 + problems 5 + features 2 = 91 patterns on zai-1, zai-2, fleet detached from the job, polled via `library/.spider/fleet-2026-08-30.edn`. Packet 5 (harvest/review at scale) after the fleet reports :done.

**CORRECTION 2026-08-30 (claude-15, on Joe's question "why is the text route broken?"):** it is not.
Tested now: `GET :7073/api/alpha/evidence/text-search?q=writing-coherence/plain-language-thesis&limit=5&hydrate=true`
returns hydrated records containing that exact id (8 occurrences across the five hits); the bare slug and
the title also find it. The sentence in the packet-3 spec above — "ranks hyphenated tokens and never surfaces
the id" — was an inferred explanation of the mismatch "22/23 ids occur in the export vs 20/23 pilot
absences", written by claude-15 without a single query against the route. The actual pilot failure was
procedural: the pilot prompt defined rung 1 ("the pattern id was found as a pattern tag/result id") but gave
the seat no route or step for it, so the seats ran rung-2 keyword phrases only — the preserved
`writing-coherence/.spider-pilot-2026-08-30/receipts` contain **zero** queries that are an exact pattern id
(aif: one). The 20 absences are absences of a search that was never run. That still makes them stale and the
full re-runs still justified — for that reason, not the false one. The export index remains the better rung-1
instrument (exhaustive, deterministic, listing-aware, no network), but the route is a working alternative and
must not be described as broken anywhere. Facade on the reviewer's side: an inferred cause recorded as a
finding (v2 §2 "inferred edges"); caught by the operator, not by review.


**WAVE 1 RELAUNCHED 2026-08-30 14:28:49Z (claude-15).** codex-20 launched the fleet at 14:23:51Z (pid 2216123) and reported, correctly without touching it, that only one runner child existed: `spider_fleet.clj` built its futures with a lazy `map`/`deref` pair, so each section future was created and awaited before the next was created — a sequential fleet on one seat. One-line fix (`run!` `deref` over `mapv` of futures), kondo/parens clean, futon3 `f8c3bb9`; the sequential fleet was killed (two attempts also killed the reviewer's own shell because `pkill -f` matched the command line that contained the pattern — use bracket patterns and never put the launch text in the same command as the kill) and relaunched from the per-section state files. At 14:30Z: five runner children, one per section, one driver. Pilot state preserved under `.spider-pilot-2026-08-30/` in each re-run section.

**holder.** claude-15; fleet process (wave 1, pid 2241961) → codex-20 for harvest  
**parent.** P-validated-R5 §3e  *(fifth precept, §0.10 — added 2026-08-30)*

**WAVE 1 COMPLETE 15:10:51Z** — 91 patterns, 68 attestation records (10 attested from the pilot review, 55 proposed, 3 refused/sent-back), 52 absences, 5 seat failures (all preserved). Packet 5 HARVEST dispatched to codex-20 — job `invoke-1788102740567-4291-bddb18d7`, park `park-98f46f1b-97d0-4d19-b4f2-3b098b776c15`: commit on explicit paths, five-section lint, mechanical verification of every record, ledger before/after, review-sample manifest (claude-15 / codex-22 halves). Semantic review follows the manifest; only sampled-and-passed edges become attested.

**HARVEST (packet 5) REVIEWED 2026-08-30 (claude-15) — passes; claude-15's review half done.**
futon3 `701a729f` (codex-20; 798 files, none under `checks/`/`test/`); lint re-run here on all five sections:
green; verification report 68/68 verified, 0 failed, the 2 listing-only being the morning's refused records
preserved as-is; manifest 12 (claude-15) / 11 (codex-22) / 11 unsampled aif. War-room 28 absences = 18 with
zero exact hits + 10 with hits that are bulletin inventories naming the ruling but no pattern (codex-20's
count from the notes) — **`war-room/` cannot be organised from evidence; it needs authored `@why`.**
*claude-15's twelve (futon3 `70f39a2`, corrected `ebb3609`, `4a94b28`):* attested 4 —
`candidate-pattern-action-space ↔ evidence-precision-registry` (a chat turn names them as registry
prior-art peers), `off-continuity-null-discriminates →how measurement-window-hygiene` (the FROM body says a
clean null *requires* it — an authored method relation), `satisfied-rungs → futonic-logic` (body cites A7;
`@why` would fit, author's call); sent back 7 — five **self-text** (the rung-2 evidence is the FROM pattern's
own text and never names the target: `declare-the-conditioning`, `experimental-comparison`,
`grounded-actuation`, `hierarchical-budget-aware`, `policy-precision`) and two **co-mention lists**
(`admissibility → advanceability`, `niche-construction → baseline-cyber-ant`); refused 1 —
`meta-lede → peeragogy/pattern-language` (the turn used "meta-lede" as a template name; unrelated
conclusions; directive removed).
**Two lines per section after this review:** aif attested 11 / proposed 48 (refused 3);
writing-coherence 2 / 1 (refused 1); problems 1 / 1; war-room 0 / 0; features 0 / 0. `fraction-organised`
(`@why`-based) is unchanged by the wave: aif 18.2%, wc 8.7% — the spider writes `@how`/`@see-also`; `@why`
stays the author's.
**Wave-2 fixes from the sample:** (1) rung-2 hits whose record *is* the FROM pattern's own text get a
`:self-text` flag and do not warrant an edge unless the excerpt names the target (five of twelve in my half);
(2) co-mention lists (ids cited together in prose) are hypothesis generators like listings — flag
`:co-mention`, same rule; (3) ~~`war-room/` is excluded from spider waves until it has authored `@why`~~ — **withdrawn 17:14Z (Joe):** the war-room patterns are the newest in the library (28, written 08-22 to 08-24) and were simply **never indexed** — zero appearances in any embeddings listing against 26 of 33 for `aif/` — so the evidence landscape has no turn-association for them *yet*. That is a process to run, not a reason to exclude them: (a) index them (`futon3a/scripts/index_patterns.sh`, the `library-embedding-refresh` pattern's own rule — CI should fail when a pattern lacks embeddings); (b) associate them to turns by embedding or any other process, exactly as other patterns are; (c) re-run the spider on `war-room/` once records cite them. Their fields and free-text are searchable like any other pattern's today; only the turn-association is missing.
**Reviewer-side defect, logged (lifecycle row 17):** my first state edit matched records by `:to` alone;
five verdicts landed on unsampled records sharing a target id, and one reason string with unescaped quotes
broke the EDN (lint caught that; the misplacement it could not). Rebuilt from the harvest commit by
`(from, to, kind)` and verified that exactly the ten intended records differ.
codex-22's half: job `invoke-1788103570154-4314-220bbca9`, park `park-2349b8fc-…` (queued behind LH-D1b).

**codex-22's review half REVIEWED 2026-08-30 (claude-15) — passes.** futon3 `e3e5ef4e`: three files + one
refused directive removed; lint green on aif/problems/writing-coherence (re-run here). Verdict table
(`library/.spider/review-codex-22-2026-08-30.md`) names the evidence kind for all 11: stated 5 / self-text
5 / co-mention 1 / listing 0 → attested 5, sent back 5, refused 1. Spot-checked two attested edges at
source: `free-energy-as-tick-scalar → expected-free-energy-scorecard` (the FROM body contrasts per-tick F
with the scorecard's per-candidate G by name) and `posterior-variance-as-epistemic-value →
predictive-entropy-as-ambiguity` (an authored "Relation to …" paragraph) — both are the **authored
cross-reference** case, the same rule claude-15 applied to `off-continuity-null →how
measurement-window-hygiene`; the two halves used one rule.
**Dissent, decided.** codex-22 would have sent back claude-15's reciprocal
`candidate-pattern-action-space ↔ evidence-precision-registry` attestations as co-mention. Ruling
(claude-15, owner): a record that asserts a **shared role** for the listed patterns ("three likely
registry/credit prior-art patterns that DERIVE must read before defining the registry") states a relation
among them — peers as prior art for one design — which is more than an enumeration and is the same record
that warranted `evidence-precision-registry → exotic/full-lift-registry` this morning; a bare list of ids
with no role asserted (the `admissibility → advanceability` case, sent back) does not. The attestations
stand; the dissent is recorded here and in codex-22's file so the rule is auditable. **Rule fixed for wave
2:** evidence kinds are `stated` (the record or the FROM body names the target and the relation),
`shared-role` (the record asserts one role for both — warrants `@see-also` only), `self-text`,
`co-mention`, `listing`; only the first two warrant an edge.
**Wave 1 — final two lines per section (68 records):** aif attested **15** / proposed 43 (refused 4);
writing-coherence 2 / 1 (refused 1); problems 2 / 0; war-room 0 / 0; features 0 / 0. Attested total 19; the
`@why` organised fraction is unchanged (the spider writes `@how`/`@see-also`). 43 aif proposals remain
unsampled-or-sent-back; a second sample of 10 from the 32 never-sampled ones is the next review packet if
Joe wants the wave closed out rather than carried.
**Wave 2 gate (before any dispatch):** runner flags `:self-text` (record is the FROM pattern's text) and
`:co-mention` (ids enumerated without a role) alongside `:listing`; the validator refuses a rung-2 edge
whose only evidence is any of the three; `war-room/` excluded until it has authored `@why`; problems/
and features/ are done (5 and 2 patterns).

**CORRECTION 17:14Z (Joe):** "`war-room/` cannot be organised from evidence" was the wrong sentence. It has no *turn-association evidence yet* because it was never indexed; the fix is to index and associate, then re-run — a wave-2 item, not an exclusion. The 28 absences were honest about the landscape *as it stood*; the landscape is the thing to change.

**SETTLED 17:23Z (Joe: "they should go through the same standardised, operational process as every other
pattern"):** they have. `futon-pattern-index.timer` runs `notions_reembed_pipeline.sh` daily (last 05:34 today);
its output `resources/notions/minilm_pattern_embeddings.json` (dated today) contains **all 28 `war-room/` ids**
among 1,296 patterns, and the retrieval default (`notions.clj:78`) reads that file. My two earlier sentences —
"cannot be organised from evidence" and then "never indexed" — were both wrong; the first mistook the landscape's
state for a property of the patterns, the second mistook a stale June BGE artefact (`bge_pattern_embeddings.json`,
1,048 ids, read by no default route) for the live index. **What is true:** war-room patterns are ingested,
embedded and retrievable like every other pattern; they appear in no historical `context-retrieval` record
because they are a week old and the export's turns predate them. Turn-association accumulates with use; nothing
special is needed; re-run the spider on `war-room/` when records cite them. Wave-2 line (3) corrected to this.

**WRONG CORPUS, 17:27Z (Joe: "why are you reading an evidence export rather than the live evidence landscape in
XTDB?"):** because packet 3 specified it — rung 1 = "exact occurrence in the evidence EXPORT
(`futon1b/migration-export/evidence.edn`)", chosen for a deterministic, mtime-keyed, network-free index, and
never checked for coverage. The export was generated **2026-07-10** (gitignored; "each box builds its own") and
holds **90,583** records; the live landscape at `:7073` holds **191,076**, through today, including records that
cite `war-room/wr-14…` on 08-26..08-30. So every rung-1 absence in wave 1 — the 18 war-room "zero exact hits"
included — was an absence *from a seven-week-old half of the evidence*. The linter's rung-2 verification already
reads the live store (`GET :7073/api/alpha/evidence/<id>`); the spider's rung-1 index did not. This is the
wrong-corpus facade (lifecycle v2 §2) inside the reviewer's own instrument, and it was caught by the operator.
**Fix (wave-2 prerequisite, packet 4c):** `build-evidence-index` reads the LIVE landscape — an enumeration of
records by date window from `:7073` if the router exposes one, else `text-search` per pattern id with the
exact-occurrence filter applied to hydrated bodies — and pins the index by a *query-time snapshot*
`{:count N :max-at T :built-at …}` (or the XTDB tx-id if exposed), never a file mtime. Absences recorded
before the fix carry `:corpus :export-2026-07-10` and are re-run. The evidence-kind rules are unchanged.


**STATUS 2026-08-30 17:57Z (I_data_current):** packet 4c (rung-1 index from the LIVE store, basis-pinned; export removed) → codex-20, job `invoke-1788112644846-4453-4d0dfcd1`; AUD-D1 (audit of every dated readout vs live endpoint, incl. the WM core .edn reads) → codex-5, job `invoke-1788112641868-4452-9f0f3deb`. Wave 2 waits on 4c's gate; war-room re-run is part of wave 2 with no special handling.

**AUD-D1 GATED 18:12Z (futon2 `d1997fc`, codex-5):** 17 rows, 4 dates re-checked. Only **three** reads are genuine
snapshots of a live store: `migration-export/evidence.edn` (07-10; packet 4c replaces it), `storage/futon0/mana-snapshot.json`
(08-12, hand-run — `mana-snapshot.timer` is not a unit despite E-wm-staleness-meta-stop expecting one), and
`storage/mark2/state.json` + manifests (05-17, a local cache of remote linode state; no endpoint exists — class (iii),
endpoint to make). The WM core reads Joe asked about — `stack-annotations.edn`, `c-entries.*.edn`, `devmap-*.aif.edn`,
`*.clean/*.executed.edn`, the `wm-trace` ledgers — are canonical local sources or the machine's own output files:
current by construction, and `I_data_current` is satisfied for them by their producers' cadence, not by a pin.
The sharper finding is not staleness: `war_machine.clj` reads `futon5a/data/stack-logic-model.edn` and `alignment.edn`,
which **never existed** (no commit in futon5a; nowhere under `~/code`), under `when-let` — three report sections have
always been silently empty. Lifecycle row 26. Fixes queued: (1) make those reads fail closed / emit `:missing`;
(2) decide whether the stack-logic model is a thing to build or a dead branch to delete (Joe); (3) mana timer;
(4) mark2 endpoint or refresh-on-read with basis pin. The bge_ line in my packet was wrong and corrected by the builder.

**AUD-D2 GATED 18:58Z (`082da13` + owner fix `f3e928f`):** `checks/absent_is_loud_lint.clj` is the instrument for
`I_absent_is_loud`. Bare run: exit 1, 56 helpers (38 loud / 17 silent / 1 declared-optional), 45 silent call sites,
**7 silent+absent-now** — every one a read of a file planned 05-03 and never produced (`stack-logic-model.edn`,
`alignment.edn`, and a third, `jsdq-terminal-vocabulary.edn`, in `joe_hud.clj:429`). Owner fix at the gate: a helper
cannot declare itself optional by being *named* `safe-…` — only a docstring that states the absence result counts
(the invariant's "the declaration is what the lint reads"); `safe-slurp-json` reclassified silent. My own 7a
instance: read a piped `$?` as the lint's exit code. AUD-D3 dispatched: delete the three dead reads citing
`P-supersede-stack-logic-model.md`; make `read-edn-file` return `{:missing path}`/`{:unreadable …}`; call sites
fail closed or render the marker. Remaining from AUD-D1: mana timer; mark2 endpoint (`safe-slurp-json` reads its
05-17 cache, present-now so not a lint violation — that is `I_data_current`'s case, not this one).

**AUD-D3 NOT PASSED 19:58Z (`a8dfd33` / futon0 `ad78a10`):** helpers loud, dead sections gone, supersession cited —
but five callers convert the marker back to nil (`(when-not (unreadable-input? x) x)`) and the lint, which classifies
helpers only, reported 0. Lifecycle row 27. Two follow-ups in parallel, different files/holders: AUD-D3b (codex-5,
`war_machine.clj`): markers accumulate into `## Input status` + `:input-status` in the trace; trace-write catch loud;
positive control by overriding one path. AUD-D4 (second seat, the lint): marker-swallowing at loud-helper call sites is a
violation; fixture 3/0. Blocker recorded: the standalone WM report does not run (`requires the shared reason-bearing
selector`; bb entry blocked in `lane_futility.clj`) — bears on Joe's decision to restart the nightly tick.

**4c GATED 20:40Z (futon3 `cad5034`) — corpus fixed, coverage overstated.** Rung 1 now reads the live store by keyset
pages, pinned `{:basis {:count :max-at} :built-at :store}` in the cache header and filename; the pin moved between two
builds seven minutes apart (191,328 → 191,356). The export is gone from all three programs. `I_data_current`: first
fix landed. But the reported table (aif 33/33, wc 23/23, war-room 28/28 "any/non-listing") is not the number: the
non-listing column admits self-text and co-mention hits, and — the new finding — **the live store contains the
instrument.** Of 157 distinct clean war-room citations, 23 are today's zai spider workers' own turns (tool calls
searching for the pattern they then "cite") and 15 are Agency completion records of the spider seat's own jobs.
`spider-self-text?` catches three prompt strings, not a worker's tool-call turn. Honest war-room: 28 / 25 clean /
**21 clean from non-spider records**; wr-5, wr-6, wr-18, wr-23 are covered only by the search for them. So the war-room
rise over the export era (10 → 21 real) is real and about three-quarters of what was claimed. Lifecycle row 28.
Packet 4d (codex-20): reflection by provenance (fleet seat set passed in; Agency job records of the spider seat),
columns any / clean / clean-non-reflection, rung-1 warrant = clean-non-reflection only, wave-1 attestations whose
warrant was reflection re-flagged `:unwarranted-rung-1` without re-spidering. Wave 2 waits on 4d.
