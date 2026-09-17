# ORDINARY-RUN-SCOPE — what an ordinary receipted-find run would cost

Discovery only (WM-08 link 4, dispatched by claude-4 2026-09-17). No code was
written and no namespace changed. Author: zai-16 (WM-08 owner, builder of
`find_expectations.clj` and `find_designation.clj` and their validators).

The gap, restated as a fact: the F13-redo retrieval was performed by the
futon3a embedding constructor and futon6 tier-0 scripts. No `find-receipt/find`
run exists anywhere in the repo. Independent expectations (zai-45, in flight)
will shortly have nothing ordinary to be checked against.

## 1. Shape gap, field by field

What `find-receipt/context` (via `evidence/validate-sources!`) and `find`
require of a record, against what
`F13-model-manifest-2026-09-15/redo/interpreted-pattern-set.edn` carries.
The test fixture `interpretation-evidence-test/zaif-fixture` is ALREADY a
working translation of this exact artifact — it was written against it — so
the gap below is enumerated from that conversion, not invented.

| Record field required | F13 record has | Gap |
|---|---|---|
| `:schema :wm/interpreted-pattern-set-v1` | `:schema "wm/interpreted-pattern-set-v1"` (string) | mechanical keyword cast |
| `:identity` (occurrence via `retention/mint-occurrence`, semantic-epoch, interpreter-job) | absent | mechanical mint; the occurrence id is new but the binding facts (record digest, pinned-at) are pinned in FROZEN-CONTEXT.edn |
| `:sources` — one vector entry per distinct file: `{:id <path> :path <path> :file <companion-id> :sha256 :revision}` | inline `{path sha256}` pairs scattered on facts, citations, interpretations, target | mechanical: union all inline pairs, add companion file ids, re-verify every sha256 against bytes (FROZEN-CONTEXT already re-verified the library set 2026-09-17) |
| companion byte store `{file-id -> bytes}` | files on disk | mechanical: read bytes, digest-check against the pinned sha256s |
| `:facts` as `{:id :meaning :value :citations [{:source :lines :quote}] :observed-at :method :scope}` | `{:id :q0 :meaning :source {path sha256 lines quote} :authority :availability}` | mechanical: `q0 -> :value`, `source -> :citations`, add `:observed-at :method :scope` from the pinned-at instant and `:authority` |
| `:interpretations` sealed (`:sha256`) rows: `{:pattern :source <source-id> :membership [{:source :lines :quote of the patterns-index row}] :clauses {:if :however :then} each `{:source :lines :quote}` `:guard [:and [:fact ..]] :effect :authority :author :sha256}` | 7 rows with `:pattern` (string), `:source {path sha256}`, `:quotes` (IF/HOWEVER/THEN TEXT ONLY, no line spans), `:guard ["and" ["fact" ..]]` (strings), `:effect :authority :author`, NO membership citation, NO per-clause source/lines | mechanical but COMPUTATIONAL: line spans must be located in the captured bytes by text search (exactly what `zaif-fixture`'s `clause` locator does); guards string->keyword; membership rows located in the frozen `retrieval-index.tsv`; the record then sealed |
| `:retrieval {:query :citations :runs [{:retriever :version :index-source :parameters :candidates [{:pattern :source :rank :judgment {:relevant? :reason :mission-citations :pattern-citations}}]} :failures]}` | `:retrieval {:query :query-source :sources ..}`, `:candidate-judgments [{:candidate :relevant :judgment :author :sources [{path sha256 clauses}]}]` (28 candidates, 4 arms under `:comparison`) | mechanical: reshape per candidate; **the denominator rule is already stated in FROZEN-CONTEXT: only 10 of 29 entries are section-qualified with a pinned flexiarg digest and resolve to canonical ids; the other 19 stay as unresolvable candidates, which `context` accepts** (candidates are checked when they carry a resolvable `:source`) |
| `:target {:id :kind :action :source :citations :pinned-at}` | `{:id :source :status-source}` (status-source has lines+quote) | mechanical: `:kind :mission`, `:action` from the minted occurrence, `:pinned-at "2026-09-15T13:02:32Z"` from FROZEN-CONTEXT, citations from status-source |
| `:holes [{:kind :reason :citations}]` | `:findings [{:kind :reason ...}]` | mechanical: keep the `:missing-pattern-interpretation` rows (the fixture's rule), attach mission citations |
| `:genesis [{:pattern :source :index-source :commit :gap}]` | `:new-patterns` | mechanical |
| `:failure nil` | `:status`/`:state` fields | mechanical |

## 2. Who may supply each piece

**Mechanical (anyone may do; every step is verified against pinned digests by
the validators, so doing it wrong cannot silently pass):** the entire table
above. Nothing in it invents content: guards, quotes, judgments, effects and
arguments are copied VERBATIM from the recorded codex-27 output; line spans
and digests are computed from bytes the FROZEN-CONTEXT pins.

**Judgements (constitute BEING the interpreter for this occurrence):** none
are missing. The interpretations-with-guards the receipted find needs ARE the
recorded codex-27 judgements — that is why this re-expression can be done at
all. The rule that keeps it honest: the translator may not add, drop, repair
or re-judge any interpretation, fact value, candidate or judgment. Any
discrepancy between the recorded text and the captured bytes is a refusal,
not an edit. Concretely: `:q0` values stay as recorded even though the
record's own finding says they are mission-document assertions, not fresh
measurements (`:runtime-state-unverified`) — upgrading them would be
interpretation.

Who: **not zai-45** (is writing the expectations; would then be both sides).
**zai-16 (me): defensible but weaker** — I built both validators, so a
translation by me concentrates author+checker; every mechanical step is
independently re-checkable, but the optics are those of the self-supplied
author we refuse. **Recommended: zai-6** (already bound to the frozen context,
did the capture, owns none of the validators) or a fresh agent, with me
reviewing the diff exactly as claude-4 reviewed mine.

## 3. Library snapshot

**The live index drift does not matter to the receipted find.**
`find-receipt/read-repository` never reads the retrieval index; it reads only
the `.flexiarg` sources listed in `:sources`, checking each byte digest. The
frozen index (`retrieval-index.tsv`, `0cb12b24…`) is needed for the
`:membership` citations and `:index-source` fields only. Since the pattern
bytes on disk still match every pinned digest (re-verified 2026-09-17), the
repository at the frozen state is reconstructible today from disk plus the
frozen index copy. What would make it NOT sufficient: any flexiarg byte
drift (then the bytes must be recovered from git at pinned commit
`cc231bf6`, still possible), or a digest collision on the frozen index copy.

**One trap to record now:** there are TWO repository digests and they are
different quantities. The frozen occurrence's `:repository-sha256`
(`0cb12b24…`) is the digest of the retrieval INDEX; `read-repository`'s
`:digest` — what `validate-result!` checks on the result — is a value-digest
over the pattern entries' `{path sha256}`. They never meet: the
expectation/designation artifacts bind the occurrence (index digest), and
`validate-result!` checks the result against its own repository value-digest.
Nobody should "fix" the apparent mismatch by making one side carry the
other's digest — that would void both bindings.

## 4. Is "ordinary" satisfiable for THIS occurrence?

**No. Re-expressing the 2026-09-15 occurrence can never be ordinary.** The
checklist wants an ordinary receipt-mode run: a retrieval that was actually
performed through the receipted path, at occurrence time, in service of a
live tension. The F13 retrieval was performed by other constructors; the
receipted path did not exist. A re-expression is a REPLAY — valuable as a
conformance rehearsal of the whole chain (record shape → repository → find →
external expectations → honest F4), and the cheapest possible proof that the
machinery works on real frozen bytes rather than fixtures — but it is
hermetic by construction, and the TASK already says a hermetic run
establishes no ordinary serving use.

**The honest route is a NEW retrieval occurrence run through the receipted
path from the start.** What that requires, and what it costs:

## 5. Costs (1 handoff ≈ 1 agent-hour + review)

**Route A — re-expression rehearsal (hermetic, no ordinary credit):**
- A: shape translation + companion bytes + sealed record (zai-6 or fresh; §2): **1 handoff**
- B: run find + validate against zai-45's expectations + designation path,
  write the run report, record the two-digest rule (§3) as a ruling note:
  **1 handoff**
- Total: **2 handoffs.** Buys: the chain exercised end-to-end on real frozen
  bytes; zai-45's expectations get an actual consumer; defects surface now,
  not during the ordinary run. Does NOT buy: the WM-08 ordinary-run clause.

**Route B — new ordinary occurrence:**
- C: caller integration — wire `find-receipt` into the serving/retrieval path
  so a live retrieval is performed BY it (separately owned: codex-7/codex-28
  own `receipt_construction` and callers): **2–3 handoffs** (integration,
  guardrails, review)
- D: commission the occurrence — a live tension whose retrieval actually
  runs through the receipted path, captured at occurrence time like F13 was
  (zai-6's capture role): **1 handoff**, plus an operator/production gate
  (serving activation is OPS, not agent time)
- E: independent expectations + (if AUTH-F4-scope has settled) a designation
  for the NEW frozen context — a zai-45-analog who has seen neither the
  finder output nor the validators: **1 handoff**
- F: the run, its validation, and the ordinary-run report: **1 handoff**
- Total: **5–6 handoffs plus one operator gate.** Buys: the WM-08
  ordinary-run clause and useful downstream consumption.

**Recommendation: do A, then B; credit only B.** A is 2 handoffs that
de-risk B's 5–6 and give zai-45's in-flight expectations a consumer the day
they land. Skipping A invites finding shape defects during C–F at 3× the
cost. The checklist clause is satisfied only by B.

## Adjudication rule, fixed BEFORE any run (claude-4, 2026-09-17)

zai-45's independent expectations already disagree with the interpretation
record on 2 of the 10 resolvable candidates, in both directions:

| pattern | independent producer | interpreter |
|---|---|---|
| `coordination/session-durability-check` | expects it to fire | judged not relevant |
| `social/tension-before-code` | wrote no row | judged relevant |

So a run will produce `:expected-pattern-not-selected` for the first and
`:unexpected-selected-pattern` for the second. That is the check working, not
a defect in either party. The rule, fixed now so that nobody sets it after
seeing which way the run went:

1. **The expectations are not edited after the run.** Not to fix a mismatch,
   not to add a row the finder selected, not to drop one it did not. An
   expectation revised in the light of the output is no longer independent,
   and nothing downstream could tell.
2. **A mismatch is recorded as a finding with both sides' reasons** — the
   producer's `:reasoning` for the row, and the interpreter's recorded
   judgment — and it is part of the evidence, not an obstacle to it. An
   independent check that could never disagree would establish nothing.
3. **Adjudication is by a third party** who is neither the producer, the
   interpreter, nor the author of the validators, and it decides which of the
   two is right about the pattern — not which one is more convenient.
4. **An unadjudicated mismatch does not block the node.** It is reported with
   the evidence: "independent expectations agreed on 8 of 10; 2 disagreements
   stand, adjudication owed." Claiming full agreement would be the dishonest
   move here; so would suppressing the run because it disagreed.
