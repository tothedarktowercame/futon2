# P-find-over-snatch — a `find` that is not identity, on the one section where `organise` already runs

Problem record under `delivery-lifecycle.md` v2 (unit = problem record). Parent: `P-validated-R5.md` §3e.
Opened 2026-08-30 by claude-15 on Joe's go ("your D1, D2 plan sounds good. Let's chain through those with parks").

## S1

**problem.** §3e states laws F1–F4 on `find : Tension → Repository → FindResult` and O1–O4 on
`organise : Set Pattern → Repository → Cascade`, and names the futon3 library as the primary
implementation. No `find` exists anywhere: Snatch's is identity on hand-picked patterns
(§3e table, last row), and `organise` has one running instance — `playout_snatch.clj:251–321`
reads `@why` lines from `library/snatch/*.flexiarg`, takes the up-closure of the acting set,
and writes it per scenario to `checks/snatch-cascade.edn` as `:closure`. Nobody has checked
that instance against O1–O4, and nothing consumes the spider's `@why`/`@how` output.

**now.** The spider fleet (packet 3, codex-20) is producing *proposed* edges on `aif/` and
`writing-coherence/`. If the consumer is built against those, it is tuned to unattested
input and then becomes the certifier of that input (self-certification between the spider
and its reader). `snatch/` has 18 patterns with 15 `@why`, 2 `@how`, 10 `@see-also` — all
author-written — and a hand-authored ground truth (six scenarios, each with `:acting`; this record first said eight — a guess from a 60-line `head`, corrected at D1's review).
Build the consumer there; it then becomes the instrument by which a proposed edge is judged
(useful iff it changes `find`/`organise`'s answer for some tension, checkably).

**solved** (a property of the model, checked before anything runs on the fleet's output):
1. `organise` as it exists is graded against O1–O4 with line pointers, and `@see-also` is
   shown *not* to enter the edge set (it is not `standsOn`).
2. A `find` exists for `snatch/` such that, for each of the six scenarios, given the tension
   of the game state: F1 selected ⊆ library; F2 every selection carries the IF/HOWEVER
   clause it matched; F3 no score-only receipts; F4 one named pattern per scenario that must
   not be returned — and it is not; recall against `:acting` is reported as a number, not a
   claim.

**facades** (v2 §2, applied here): `find` as substring grep on tension words (similarity-as-
warrant, F3); a `find` that can return anything for anything (F4); `organise` fast-forwarding
through `@see-also` (O2: inferred edge); recall "achieved" by widening the tension until it
matches (fixture); D1 written from the summary in §3e rather than from the code (paraphrase-
as-citation).

**owner.** claude-15 (review). Builders: codex-22 (D1, D2).
**status.** validated (claimed by claude-15, 2026-08-30 — both clauses of *solved* shown in the artefact: (1) `organise` graded, `@see-also` shown not to enter edges; (2) `find` with F1–F4 witnessed on six scenarios, recall reported and its tautology named). Joe to confirm or set to did-something-else.
**holder.** claude-15 (closed; D3 done)  
**parent.** P-validated-R5 §3e  *(fifth precept, §0.10 — added 2026-08-30)*

## deliveries

- **D1 — discovery, no code** (codex-22). Read `playout_snatch.clj:251–321` against O1–O4;
  say which hold and which do not, with line pointers; whether `@see-also`/`@how` leak into
  edges; whether precedence (O4) is data on the cascade or derived; whether O3 fast-forward
  exists or only closure. For each of the six scenarios in `snatch-cascade.edn`: the
  tension (IF/HOWEVER text of the acting patterns) `find` would need, and one F4 pattern that
  must not be returned. Refusal permitted where a law is not decidable from the code.
  Output: `futon2/holes/problems/facts-find-snatch-D1.md`. Review gate before D2.
- **D2 — build** (codex-22, after D1's review). `find` for `snatch/` with the six-scenario
  acceptance in *solved* (2). Packet written from D1's findings, not before.

- **D3 — `organise` conformance, APPROVED by Joe 2026-08-30 13:58Z**, queued behind D2 because
  it edits the same two files (`playout_snatch.clj`, `snatch-cascade.edn`) on the same seat.
  Two packets, one behaviour each, a review between; dispatched on D2's wake once D2's review
  passes. Both `--mode work`, parked, deadline 2700.
  - **D3a — O1 + F1 on the writer.** In `emit-cascade-edn` (`playout_snatch.clj:354–364`):
    `:acting` drops `:no-pattern` and the number of fallback rounds is kept as
    `:fallback-rounds n` (information moved, not lost); `:closure` is replaced by `:nodes`
    (= acting) and `:added-by-organise` (= up-closure − acting), so provenance is a field, not
    a subtraction the reader must know to do. Regenerate `snatch-cascade.edn`. **Acceptance:**
    for every scenario `nodes ∪ added-by-organise` equals the old `:closure` (assert against
    the pre-change EDN, kept in the packet as a fixture); `:no-pattern` absent everywhere;
    `bb p4ng/empirics-futon/gen_snatch_cascade.bb` still runs (it reads this EDN — the figure
    must not drift; update its reader if it read `:closure`); kondo, parens, run twice + diff.
    No other change to `playout_snatch.clj`.
  - **D3b — O4: precedence is cascade data.** Each scenario row gains `:precedence`, the
    effective order the run consulted — pattern `:precedence` overlaid with the policy's
    `overrides` (`pattern-policy` `:175–183`) — as a collection-level vector of ids, plus
    `:policy` naming it; and the EDN gains a second row-set for `pi-exchange-first` (`:187`)
    over the same six scenarios. **Acceptance (S-G4, corrected after the row-11 dry-run on
    2026-08-30):** the report prints per scenario `(nodes-equal? score-patterns score-exchange-first)`;
    S-G4 holds if at least one scenario shows different `:precedence` and a different `:score`
    — the dry-run says G4/snatcher (3 vs −5) — and the report states, rather than hides, that
    the acting sets differ too (they differ in all six: changing the order changes who wins, so
    "identical nodes" is not a satisfiable clause and is dropped). Same gates. No other change.
  - After D3b: `organise` for `snatch/` satisfies O1, O2, O4 and F1; O3 (fast-forward) stays
    open — the closure design makes it vacuous here and it needs a section with omitted
    intermediates to be tested at all. Record that rather than "fix" it.

## log
- 2026-08-30 D1 dispatched (see status line below).
- 2026-08-30 **D1 reviewed by claude-15 — sound** (futon2 `85f4491`, `facts-find-snatch-D1.md`, 187 lines).
  Checked: the reader regex is exactly `#"@why (.*)"` (`playout_snatch.clj:260`) so O2 holds;
  `induced-edges` (`:282–283`) keeps immediate authored edges among closure nodes — closure, not
  fast-forward, so O3 does not hold as written; the writer (`:354–364`) emits no precedence, so O4
  does not hold; `:closure` is the full union with no added-by-organise field, so O1 holds only up
  to `closure − acting`. Scenario list `:330–332` has **six** entries; codex-22 refused to invent
  two for my "eight". `:by :no-pattern` (`:183`) leaks into `:acting` via `(keep :by trace)`
  (`:357`) — a `find` under F1 can never return it. All six F4 patterns verified against
  `treatments` (`:151–156`): each IF names a condition the treatment sets false. Part 3's route —
  `find` = evaluate the authored IF/HOWEVER predicates on the structured state (`fires?` `:143–146`)
  with receipts citing the flexiarg clause lines — is the D2 design. One fact D2 must carry: the
  runner's `:if-text` is a *paraphrase* of the flexiarg clause (`:53` vs
  `an-unmodelled-response-stops-the-line.flexiarg:14–16`), so the receipt's warrant is the file
  text and the runner text is reported as drift.
- 2026-08-30 D2 dispatched (status line below).
- 2026-08-30 **D2 reviewed by claude-15 — passes** (futon3 `f1998a58` + review fix `5941270`).
  Checked: diff read (185 lines, two files, `playout_snatch.clj` untouched); two runs
  byte-identical; kondo 0/0; parens OK; 1.3 s. F1: `find` throws on any id outside the 18
  parsed files and `:no-pattern` never appears in a selection — the one occurrence in the EDN is
  the typed absence `:no-pattern-addresses-this-tension` at a state where nothing applies
  (`find-snatch.edn:1530`). F4: all six declared zero-mass patterns `:holds true` in the data.
  Receipts: `probe-before-committing` cites IF `[15 16]` / HOWEVER `[19 20]`,
  `re-enter-after-observed-repair` IF `[14 15]` / HOWEVER `[18 19]` — each range re-read in the
  file and contains exactly the clause. Drift: 21 mismatches over 14 patterns, all genuine
  runner paraphrases with both strings recorded, none with a missing file. `:state-fields
  :not-instrumented` rather than a guess — correct refusal.
  **What the recall number can and cannot show:** 6/6 at 100% is close to tautological —
  `pi-patterns` acts only on patterns that `fires?`, and `find` *is* `fires?` with receipts, so
  `acting ⊆ selected` by construction. The informative numbers are the other way round:
  per scenario `find` selects 4–7 of the 18 patterns, of which 2–3 act (G1/snatcher 2 of 4;
  G4/snatcher 3 of 7; G5/sharer 2 of 6) — so `find` is neither identity nor "everything", and
  the patterns that fired but lost on precedence are exactly what O4 will make visible as data.
  **Review fix (mine):** `:as-of` was `HEAD`, which changes with the commit containing the
  report, so a re-run could never match the committed EDN; now the last commit touching
  `library/snatch/` (`2734ac5`), which is the repository the receipts cite.
- 2026-08-30 D3a dispatched (status line below).
- 2026-08-30 **D3a (re-dispatch) reviewed by claude-15 — passes** (futon3 `ea49ffac`, p4ng
  `d1edc1ce`, review fix futon3 `1ede1aa`). Checked: diff confined to `emit-cascade-edn` (+9/−4) and
  the figure generator's one-line reader; regenerated twice here, identical to the commit; closure
  equality vs `f1998a58` 6/6; `:no-pattern` absent; `:fallback-rounds` 0/0/0/0/2/0 with replay
  agreeing (G2 rounds 11, 12); scores unchanged; figure generator runs, SVG unchanged;
  `find-snatch` recall unchanged; kondo 0/0; parens OK. **Review fix (mine):** `:acting` had been
  sorted — the packet asked to drop `:no-pattern`, not the order of first firing, which is what
  D3b compares precedence against; restored (`:nodes` stays a sorted set). **Row-11 dry-run for
  D3b:** `pi-patterns` vs `pi-exchange-first` over the six scenarios — acting sets differ in all
  six; scores differ only in G4/snatcher (3 vs −5). So the staged clause "identical `:nodes`" was
  unsatisfiable; D3b's acceptance corrected before dispatch (see the D3b entry).
- 2026-08-30 D3b dispatched (status line below).
- 2026-08-30 **D3b reviewed by claude-15 — passes; D3 closed** (futon3 `8762ba73`, p4ng
  `76219e31`). Checked: diff confined to `emit-cascade-edn` plus two named override maps (no policy
  logic changed), a one-line `:policy` filter in `find_snatch.clj` and in the figure generator;
  regenerated twice here, identical to the commit; 12 rows, every one with `:policy` and
  `:precedence`; the six `:patterns` rows equal `1ede1aa`'s field-for-field once the two new
  fields are removed; exchange-first's precedence leads with `exchange-when-both-sides-gain`;
  `:s-g4`: nodes differ in all six scenarios, scores differ only in G4/snatcher (3 vs −5),
  verdict `:holds` — the acceptance as corrected by the dry-run, met exactly; figure SVG
  unchanged; `find-snatch` recall unchanged; kondo 0/0; parens OK. No findings.
  **Where this leaves `organise` on `snatch/`:** O1 (nodes = acting, `:added-by-organise` a
  field), O2 (edges from `@why` only), O4 (`:precedence` as collection-level data, and S-G4
  witnessed: the wiring carries the score) and F1 (`:no-pattern` gone, `:fallback-rounds`
  kept) hold in the artefact. **O3 stays open by design**: closure includes every intermediate
  as a node, so fast-forward is vacuous here; it needs a section where selected patterns have
  omitted intermediates between them, and that is the library-scale `organise` — the consumer
  the spider fleet's output will be judged by.
- 2026-08-30 **D3a REFUSED by codex-22 — correctly; no files changed.** Two of my acceptance
  conditions contradicted the data: (i) the old G2 `:closure` contains `:no-pattern` (it entered
  `up-closure` as a node; `f1998a58:checks/snatch-cascade.edn:62–69`), so "union = old closure"
  and "no `:no-pattern`" cannot both hold; (ii) `:acting` is built with `distinct`, so it holds
  one `:no-pattern` while the replay has two fallback rounds (11, 12) — "fallback-rounds sums to
  the old occurrences" expected 1 against a truthful 2. Both verified by claude-15 (replay of
  G2/snatcher). Corrected acceptance, authorised: union = `(disj old-closure :no-pattern)`;
  `:fallback-rounds` = count of `:no-pattern` rounds in the replayed trace (G2: 2). Re-dispatched. Held for a later D3, one behaviour each: O1's
  added-by-organise field; O4 precedence as cascade data; `:no-pattern` filtered from `:acting`.

**STATUS 2026-08-30 (superseded below):** D1 dispatched to codex-22 — job `invoke-1788097890836-3971-4f5e4467`,
park `park-413aa2e0-d1bf-4f5c-a92f-1066c0c839fd` (deadline +45 min). D2 written only after
D1's review. In parallel: packet 3 (spider fixes + fleet driver) on codex-20, and B′ done
(`P-snatch-microcosm.md` §1b).

**STATUS 2026-08-30 13:55Z:** D1 reviewed, sound (see log). D2 dispatched to codex-22 in
`--mode work` — job `invoke-1788098122596-3984-94663fe0`, park
`park-bf81252f-5c1c-4b7f-be32-35956107adf9` (deadline +45 min). Deliverable:
`futon3/checks/find_snatch.clj` + `find-snatch.edn`; acceptance = F1 (no `:no-pattern`, ids ⊆
library), F4 6/6 declared zero-mass patterns absent along every trajectory, recall vs
`:acting` per scenario reported as a fraction, receipts citing flexiarg clause lines, drift
report of runner paraphrases. D3 (O1/O4/`:no-pattern` in `organise`) held for Joe.
**D3 approved (Joe, 13:58Z): D2's wake dispatches D3a after D2's review; D3b after D3a's.**

**STATUS 2026-08-30 14:02Z:** D2 passed review (`f1998a58` + `5941270`). D3a dispatched to codex-22 (`--mode work`) — job `invoke-1788098537078-4002-e7e91cf2`, park `park-0b48431b-465e-49d1-9bd0-82da55f1f5bd` (deadline +45 min). D3b follows D3a's review. O3 stays open by design.

**STATUS 2026-08-30 14:04Z:** D3a refused (spec contradiction, mine) and re-dispatched with corrected acceptance — job `invoke-1788098633045-4003-02b791d0`, park `park-50e893d2-3a70-48e4-aff6-5ea66974896f` (deadline +45 min). D3b after D3a's review, with its acceptance dry-run against the artefact first.

**STATUS 2026-08-30 14:13Z:** D3a passed review (`ea49ffac` + p4ng `d1edc1ce` + review fix `1ede1aa`). D3b dispatched to codex-22 (`--mode work`, acceptance dry-run first) — job `invoke-1788099200402-4006-db15d161`, park `park-85db9e95-20c4-4d70-9ef0-64fe9a065c62` (deadline +45 min). After D3b: D3 closes; nothing further without Joe.

**STATUS 2026-08-30 14:16Z:** D3b passed review; D3 closed. No dispatch outstanding on this record. Next on this line only on Joe's word — candidates: O3 at library scale (the consumer of spider output), or a second section for `find` (a prose-only one, where D1 Part 3 says structured antecedents will not carry).
