# FLIGHTS — Batch 7 (ten deposits, CROSS-LANE: zai-10 flies lane B, zai-9 flies lane A)

**Arming:** Joe (batch 7 armed; overnight execution delegated to claude-2,
2026-07-11 late). Scope: artifact-only — lab dirs only, no production edits,
no process launching/signaling, no substrate writes, reads via files/git/HTTP-
GET only. **OUTWARD-FACING BOUNDARY (two missions):** M-cold-chain's send
gate is the operator's permanently; M-peeragogy-rewrite's coauthor
invitations are operator sends. Flights may DRAFT and VERIFY; nothing leaves
the machine. RUNNER-CONTRACT.md in force.

**FIRST FULL EXERCISE OF PER-OBLIGATION ACCOUNTING:** every psi's obligations
are enumerated (batch-7 convention). Each flight reports per-obligation
status (discharged / attempted / out-of-scope, one line each). Adjudication
records carry :obligations-total and :obligations-discharged alongside the
boolean (boolean = ALL obligations discharged, per psi text). An obligation
counts DISCHARGED when it is a one-shot artifact that lands, or a standing
invariant WITNESSED holding today by a verifier; it does not count when it
needs operator practice, outcome windows, or outward acts.

## zai-10 flies LANE B (4):

### B7-F1 — ft-weird-modernism-001 (5 obl) — expect FALSE, rate ~1/5
Fly obligation 1's surface: `mint_lexicon_candidates.py` in
futon3/holes/labs/M-weird-modernism/ (create) — extract candidate portable
handles from the mission's essay/doc minting surfaces (§2 table + §8
glossary; locate them, STOP if absent), each with source citation and a
recurrence count. No invented handles; candidates only (quarantined — not
library entries). Obligations 2-5 are scholar/operator practice:
out-of-scope, say so per line.
**Acceptance:** candidates extracted from real text with citations (paste);
any cwd. **Adjudication:** FALSE; report the 5-line obligation ledger.

### B7-F2 — ft-weird-modernism-002 (5 obl) — expect TRUE (the batch's representational-discipline psi)
The psi asks for stewardship ARTIFACTS: (1) anchor pair named with dates +
rationale; (2) succession plan; (3) a per-cycle validity review EVENT
recorded; (4) canonical-plus-archive composition declared; (5) cycle
identifiers adopted. All five are one-shot artifacts. Write
`anchor-stewardship.md` + `anchor-review-log.edn` (first review event, dated,
citing the current anchors against the doc's current theory) in the same lab
dir, from the REAL mission doc (cite its actual anchor passages).
**Acceptance:** both artifacts land; every claim cites the real doc; the
review event is a dated EDN record passing check-parens + one-form.
**Adjudication:** TRUE iff all 5 obligations land as artifacts.

### B7-F3 — ft-cold-chain-001 (5 obl) — expect FALSE, rate ~3/5
Fly the standing-invariant witness: `rung_status_verifier.bb` in
futon7/holes/labs/M-cold-chain/ (create) — read the REAL registry + evidence
log (read-only): verify ob-2 (n_outreach equals count of typed :outreach-sent
records — the honest-counter invariant) and ob-3 (every rung's :held /
:satisfied consistent with witnessed events); report ob-4 by listing the
recorded handoff chain for the witnessed send. Obligations 1 (response feeds
next decision — needs P14D window) and 5 (anti-capture judgment): attempted/
out-of-scope lines. NO SENDS, no drafts even.
**Acceptance:** verifier runs any cwd on real files; invariant verdicts
pasted. **Adjudication:** FALSE; obligation ledger with ~3 discharged if
invariants hold.

### B7-F4 — ft-cold-chain-002 (6 obl) — expect FALSE, rate ~3/6
Fly the state-durability slice: `cold_chain_state.bb` same lab dir —
(a) ob-1: reconstruct the full chain state from disk files alone (registry +
evidence log), print it; (b) ob-2: per-rung snapshot query (--rung T2.3);
(c) ob-6: verify events are typed + append-only ordered. Ob-3 (shape
generality) = a short analysis note section in the output; ob-4 duplicates
F3's verifier (cite it, don't rebuild); ob-5 (operational-not-decorative)
= judgment, attempted line.
**Acceptance:** reconstruction + query outputs pasted from real files; any
cwd. **Adjudication:** FALSE; ledger ~3/6.

## zai-9 flies LANE A (6):

### B7-F5 — ft-hypergraph-operator-001 (5 obl) — expect FALSE, rate ~2/5
Fly the person-facing render: `jsdq_operator_view.bb` in
futon5a/holes/labs/M-hypergraph-operator/ (create) — read the REAL
alignment.edn (locate it; STOP if absent): render the 8-sector/39-item
topology with per-sector maturity (ob-1), the four SORRY-* typed holes in
person-facing terms (ob-2), and make it queryable from bb (ob-5). Ob-3
(live operator signal) and ob-4 (AIF observables binding) need the unbuilt
hooks: attempted/out-of-scope lines.
**Acceptance:** render from real data pasted; a --sector query demonstrated;
any cwd. **Adjudication:** FALSE; ledger ~2-3/5.

### B7-F6 — ft-hypergraph-operator-002 (6 obl) — expect FALSE, rate ~1/6
Fly the one-step gap D2 identified: `sector_instantiation_v0.bb` same lab
dir — the per-sector cyberant binding SKELETON (ob-2): for each of the 8
sectors, derive forage/carry/deposit/trail slots from alignment.edn's real
content, deterministic, with tests (fixture = 2 sectors). The remaining
obligations need live circulation: per-line status.
**Acceptance:** skeleton runs on real alignment.edn; tests green any cwd
(paste). **Adjudication:** FALSE; ledger ~1-2/6.

### B7-F7 — ft-a-sorry-enterprise-001 (5 obl) — expect FALSE, rate ~1/5
Fly ob-2's core: `affinity_score_v0.py` in
futon5a/holes/labs/M-a-sorry-enterprise/ (create) — implement the
sorry-pattern affinity score per the spec in stack-logic-model.edn (locate
it; STOP if the spec section is absent), demonstrated STANDALONE on the real
sorrys (registry) x a sample of real library patterns — NOT wired into the
live per-turn pipeline (that wiring is production scope). Other obligations:
per-line status.
**Acceptance:** scores computed on real sorrys/patterns, output pasted, spec
section cited; any cwd. **Adjudication:** FALSE; ledger ~1/5 (ob-2 partial —
scorer exists, pipeline wiring doesn't; count as attempted unless the psi's
wording is satisfied by the standalone demo — judge per its text).

### B7-F8 — ft-a-sorry-enterprise-002 (11 obl — the record psi) — expect FALSE, rate ~2/11
THE PER-OBLIGATION SHOWCASE: an 11-obligation psi is exactly why boolean
labels mislead. Fly ob-9: `sorry_transition_ledger.bb` same lab dir — parse
the real sorry registry's entries into provenance-bearing state-transition
records (spawned/strengthened/refined/closed where evidenced), append-only
ordered, with a validator; and ob-4's kernel: for ONE real sorry, produce the
auditable "why THIS pattern" proximity proof using the F7 scorer if landed
(cite it) or overlap counts if not. All other obligations: per-line status.
**Acceptance:** ledger from real registry pasted; validator green any cwd;
one auditable proximity proof. **Adjudication:** FALSE; ledger ~2/11.

### B7-F9 — ft-peeragogy-rewrite-001 (5 obl) — expect FALSE, rate ~1/5
Ob-1 (the essay) is the AUTHOR's coherence-forcing task and ob-5 (invitations
sent) is operator-outward: both out-of-scope BY THE MISSION'S OWN TERMS (a
runner-drafted essay would recreate the solo-drive antipattern the psi
names). Fly ob-2: mine next-wave flexiarg CANDIDATES from the real podcast
corpus (locate the 13 transcripts) — target the named candidates (calling-in,
check-in, brave-safe-space, listening-as-discipline, CLA,
register-moderation): for each, recurrence evidence from >=2 transcripts
(quoted, cited) + a drafted candidate flexiarg in
futon4/holes/labs/M-peeragogy-rewrite/candidates/ (QUARANTINED — candidates
dir, not the library; discerning-a-pattern discipline: recurrence first,
naming second).
**Acceptance:** each candidate carries real quoted recurrence evidence with
transcript citations; drafts parse (flexiarg format per any existing library
file). **Adjudication:** FALSE; ledger ~1/5.

### B7-F10 — ft-peeragogy-rewrite-002 (6 obl) — expect FALSE, rate ~2/6
Fly ob-4 + ob-6: (a) `handoff-packages.md` same lab dir — the TIERED
invitation packages (weak/medium/strong) as DRAFTS (operator sends or
doesn't; nothing leaves the machine), each tier honestly matched to
coordination cost with named example collaborator TYPES (no real names
without doc basis); (b) a verification section: the inspectable-intermediates
claim checked against the real queue v5 / annotation / synthesis files
(paths + one-line condition each). Other obligations: per-line status.
**Acceptance:** package drafts land; intermediates verification lists real
paths; <= 3 pages total. **Adjudication:** FALSE; ledger ~2/6.

## Execution
Parallel cross-lane, serial within lane. claude-2 reviews per CONTROL-LAYER.
After all ten: adjudications at :grain :deposit-psi with
:obligations-total/:obligations-discharged, bridge, scoreboard, retrain
(n up to 39), LOMO — THE FLIP-THRESHOLD VERDICT. Five new pairs enter the
paired comparison, which now runs on DISCHARGE-RATE-PER-OBLIGATION.
