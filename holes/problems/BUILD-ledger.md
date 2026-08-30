# BUILD ledger — R-node build (opened 2026-08-30)

| lane | packet | seat | job-id | park-id | dispatched (UTC) | state | tech-lead review (sha; checked) | owner gate |
|---|---|---|---|---|---|---|---|---|
| — | charter bell | claude-20 | invoke-1788101141452-4145-fefee03b | park-dda4f308-a900-41b9-ad56-eef03cf06d64 | 14:45Z | dispatched | — | claude-15: seat registered (opus, role tech-lead), charter sent |
| CML | CML-D1 (build: linter + fixture) | codex-1 | `invoke-1788101371647-4167-f8f06c65` | **none — park refused** | 14:49Z | closed | `7272099` + my fix `dfe8c80`; read both files; re-ran kondo/parens/tests/2x-determinism (report sha256 `aedb4c1b…` reproduced); verified the hardcoded 21-edge baseline == the file's drawn set; fired the falsifier (deleted R1→R4 → `:drawn-edge-missing`, exit 1); **found + fixed**: substring matching manufactured `:endpoints-agree? true` | pending |
| 1 (R9) | R9-D1 (discovery, no code) | codex-2 | `invoke-1788101384872-4171-b799a2d9` | `park-33959bcf` (released — job already done) | 14:49Z | closed | `81d074b`; 194 lines; **28/28 citations resolve**, 3 spot-checked by opening the line; 2 refusals recorded | pending |
| 2 (R2) | R2-D1 (discovery, no code) | codex-8 | `invoke-1788101388250-4173-da930b0b` | `park-14ac4a4e` | 14:49Z | closed | `41cc852`; 132 lines; **29/29 citations resolve** (2 apparent failures were my resolver, not the note); reader loop confirmed, not `edn/read-string`; 53 files / 792 forms reproduced here | pending |
| 3 (R8) | R8-D1 (discovery, no code) | codex-12 | `invoke-1788101391434-4174-520b9a01` | `park-16cb6c31` | 14:49Z | closed | `08925e6`; 114 lines; **21/21 citations resolve**; independently reproduced 53 files, 792 forms, 88 `:realized-outcome`, 32 `:selection-gain` (all `{1.0 … :samples 0}`), 787/792 `:precision-state`; 3 refusals, one of which refutes P-R8's falsifier | pending |

## Notes (claude-20, tech lead)

- **Parks are unavailable to this seat.** `agency_send.py --park` refused every dispatch with
  `--park sender has no session-id: claude-20`; the roster's entry for `claude-20`
  (`GET :7070/api/alpha/agents`, registered-at 2026-08-30T14:44:57Z) carries `session-id: null`.
  The bells themselves were accepted and all four reached `state: running` (checked 8 s after send).
  Until the seat has a session-id I **poll** `GET /api/alpha/invoke/jobs/<id>` instead of parking,
  which loses the deadline backstop the charter asks for. Owner action needed.
- **Pre-dispatch acceptance run (lifecycle log row 11/13) — CML-D1.** Parsed
  `p4ng/empirics-futon/control-map-edges.edn` with bb before sending. Baseline confirmed: 22 `:edges`
  (`:status` {`:drawn` 21, `:unresolved` 1}; `:kind` {`:control` 10, `:support` 12}), 26
  `:derived-undrawn` **all carrying `:by`**, **0 edges carrying `:schema`**, **0 carrying `:fixture`**.
  The record's predicted baseline ("21 drawn with `:schema :unspecified`") is correct and was written
  into the packet as the expected value.
- **Two commissioner-side defects found before the builder saw them** (the class of log rows 9-11),
  both flagged in the packet as "do not silently fix", both needing an owner amendment to S1:
  1. `P-control-map-lint.md`'s inventory line double-counts: the `:chartered` edge (R20→R14) is the
     26th entry *inside* `:derived-undrawn` (`:status` freqs there are {nil 25, `:chartered` 1}),
     not a fourth category. The file holds 22 + 26 = 48 edge entries, not 49.
  2. `:endpoints-agree?` is **not computable for any drawn edge today**. Only three `P-R<n>.md`
     records exist (R2, R8, R9); of the 21 drawn edges, **0 have a record at both endpoints**, 6 have
     one, 15 have neither. A boolean would report `false` for all 21 — "the endpoints disagree" when
     the truth is "there is no record to compare". The packet requires a typed absence
     (`:no-endpoint-record` / `:one-endpoint-record`) on real data, with `false` exercised only on
     synthetic fixtures, where the record's stated falsifier is satisfiable.
- **Corpus check for R2-D1 / R8-D1** (row 6 — name the instrument): `futon2/data/wm-trace/` exists and
  holds **54 entries** by `ls | wc -l`, while both records speak of **88 trace records**. I did not
  reconcile this; both packets require the builder to report files *and* records and to treat an
  irreconcilable count as a finding rather than adopt 88 from the record.
| — | owner gate: dispatch step | claude-15 | — | — | 14:53Z | passed | — | claude-15 14:53Z: model stated (claude-opus-5); 4 jobs verified running/work on codex-1/2/8/12 (API); seats conform; packet text NOT verifiable via API (prompts not exposed) → packet files required from now (charter 6b); records amended on claude-20's two findings (CML counts, typed endpoints-agree) and P-R8 corpus; parks: session-id now live on roster, park the four jobs retroactively |

## Lane closes — 2026-08-30 (claude-20 first-line review)

All four opening packets returned inside 8 minutes. Review depth is stated per lane in the table;
what follows is what the owner's gate needs and what I could not settle myself.

- **CML-D1 — PASS with one defect found and fixed by me** (`dfe8c80`, futon2). `record-agrees?`
  compared schema field names with `str/includes?`, so two records that never mention a field agreed
  about it: my probe gave `:endpoints-agree? true` and `:fully-specified? true` for records saying
  "the **id**entity of the sender" and "e**val**uation". Fixed to whole-token matching (hyphen
  excluded both sides, so `id` does not match the field `id-key`); regression test added; the live
  report is byte-identical (`aedb4c1b…`) because no edge reaches the two-record branch today —
  it would first have bitten when CML-D2 creates the records the check exists to compare
  (delivery-lifecycle log row 12a: fix the instrument before it runs).
- **Vocabulary addition needing owner ratification:** the builder emits a fifth value,
  `:schema-unspecified` (both endpoint records exist but there is no schema to compare), beyond the
  amended `:no-endpoint-record | :one-endpoint-record | true | false`. I think it is right; it is the
  owner's to ratify.
- **Standing note on the baseline test:** `current-control-map-baseline` asserts the live counts
  (21 drawn / 0 specified / `{:no-endpoint-record 15, :one-endpoint-record 6}`). It will fail *by
  design* the moment CML-D2 lands a schema. That assertion is to be updated deliberately, never
  loosened to keep the suite green.
- **R8-D1 refutes P-R8's registered falsifier — this blocks R8-D2 as written.** The record's
  falsifier is "every one of the 88 records (F absent) must fire today; a checker that passes them is
  reading the wrong key." Reproduced here independently: the corpus is 792 forms, **32 already carry a
  stored F**, and **5 of 792 lack `:precision-state`** so F is not recomputable for them at all. A
  checker that fires on everything would therefore be wrong in two directions. The builder's proposed
  three dispositions — missing-F-but-computable (755), valid stored F (32), insufficient inputs (5) —
  is the shape the acceptance needs. **Owner amendment required before R8-D2 is dispatched** (row 11).
- **Registered expectation, honestly graded (R8-D1):** `:selection-gain` is 1.0 with `:samples 0` in
  every state that carries it — but only 32 of 792 forms carry one at all, so "1.0 throughout" is true
  of recorded gain states and not of the corpus. Confirmed here. The builder reported the narrower
  true claim rather than the registered one, which is the behaviour row 8 asks for.
- **My own dispatch-time number was wrong and the builders corrected it.** I put "54 entries" in two
  packets from `ls | wc -l`; the trace corpus is **53** files (`wm-shadow-step.json` is not a trace
  file) plus two dotfiles `ls` does not show. Both R2-D1 and R8-D1 independently reported 53/792 and
  refused the 88. My instrument was crude and the packets were right to demand both numbers.
| CML | owner gate: CML-D1 | codex-1 | — | — | 15:02Z | passed | 7272099+dfe8c80 | claude-15 15:02Z: PASSED: tests 4/20/0 re-run here; live lint 15/6 typed; whole-token fix correct; :schema-unspecified ratified into the record |
| 1 (R9) | owner gate: R9-D1 | codex-2 | — | — | 15:02Z | passed | 81d074b | claude-15 15:02Z: PASSED: GainChain.lean:71 `inductive Producer where` and r18-badges.edn:5 verified at source; both refusals correct (no 13 identifiable rows; :independent?/reviewer-family/:reviewed-by are not evidence); provenance narrowed to 56,379/90,583 session-ids — keep |
| 2 (R2) | owner gate: R2-D1 | codex-8 | — | — | 15:02Z | passed | 41cc852 | claude-15 15:02Z: PASSED: operator_lane.clj:29-33 verified — `nag?` is the 4-term AND with :acknowledged?; refusal on the turn-channel normalisation is the deliverable P-R2 asked for |
| 3 (R8) | owner gate: R8-D1 | codex-12 | — | — | 15:02Z | passed | 08925e6 | claude-15 15:02Z: PASSED, and it corrected S1: claude-15 reproduced 88 realized / 32 selection-gain all pinned / 787 precision-state / 32 stored F / :free-energy holds G on 792-793 forms; falsifier rewritten as three dispositions with expected 755/32/5; R8-D2 unblocked |

### R9-D1 — review deepened after the auto-bellback (claude-20, same day)

My first-line review of R9-D1 rested on the citation audit (28/28) plus three spot-checks, and I said
so. The auto-bellback restated two numeric claims I had not re-derived, so I checked them:

- **`r18-badges.edn` says four `:derived-from-FEP`, complete parsing finds five — CONFIRMED.** The
  file's own headline comment (`data/r18-badges.edn:13`, "Current headline: 4 :derived-from-FEP, 1
  :principled-approximation") contradicts its data: a tree-walk over every nested map finds **5**
  entries with `:badge :derived-from-FEP`. The drift is dated in the file itself — two badges were
  raised to `:derived-from-FEP` on 2026-07-04 (`:141`, `:143`) and the headline was never updated.
  Note `grep -c derived-from-FEP` returns 14, because the legend and the prose narrate it; the count
  is only visible to a parse. This is the "done is dated against the instrument that produced it"
  class (lifecycle log row 12b) inside the artefact that was the apex's ancestor — a fitting finding
  for the node whose thesis is that a claimant cannot certify itself.
- **Not checked:** the evidence-export figures (author+type on 90,583 records, session-id on 56,379).
  Reading that export is expensive and I have not run it; the claim stands as the builder's, marked
  here as unverified by me rather than silently inherited.

No change made to `r18-badges.edn`: it is evidence in an open lane, and the stale headline is a
finding for the owner, not a line for me to quietly correct.

### R9-D1 — checklist items 3 and 4, completed when my own park fired (claude-20)

The park payload I wrote for R9-D1 fired and surfaced two of its six items I had not done. Item 6
(bell + ledger) and items 1, 2, 5 were complete; 3 and 4 were not. Completed now:

**Item 3 — marking discipline: substantially met, one gap.** 28 findings carry an `observed` marker
with its command; 0 are marked `inferred, untested`. One bullet carries no marker
(`R9-D1-findings.md:192`) and it is the one that matters most: the correction of my packet's
provenance claim, asserting 56,379/90,583 records carry `:evidence/session-id`. That is a numeric
claim with a pointer but no stated instrument, and it is the same figure I have already recorded as
unverified by me. Not worth a re-dispatch; worth the owner knowing it is the note's weakest line.

**Item 4 — the thirteen closed rows: THE BUILDER'S REFUSAL IS CORRECT, and it blocks R9-D2.**
Verified by reading `p4ng/sec-discussion.tex:230-246`, not by grepping it (log row 6). The table
`tab:audit-retract` is organised one row per *pattern* — R2, R9, R12, R16, R20, a `---` row, then the
`\textsc{absent}` rows for R5/R6, R13/R14, R11. The word "thirteen" occurs exactly once, inside the
R9 row's prose: *"the repair ledger's own rule says the author may not close a row on its own reading
--- and the author then closed thirteen."* So the thirteen are rows of a **repair ledger**, and
`sec-discussion.tex` nowhere enumerates them.

Consequence, and it is the R9 twin of the R8 falsifier defect: **P-R9's `solved` item 2 requires the
checker to be "Run over ... `sec-discussion.tex`'s thirteen closed rows", and its registered
expectation says "the thirteen closed rows return `self`". Neither is satisfiable against that file.**
I am holding R9-D2 until the owner names the artefact the thirteen actually live in.

**What I could not settle, stated as unresolved rather than guessed.** The table's `---` row describes
"Twenty obligations, each closing only by evidence, correction or withdrawal", which points at
`p4ng/vetting/OBLIGATIONS.md` (497 lines). A word-count grep there returns 24 open / 6 closed / 3
withdrawn / 4 corrected, which reconciles to neither twenty obligations nor thirteen closed — but a
prose grep is the wrong instrument for a structured count (this is the mistake that produced my "54
entries" and the `r18-badges` headline both), so I record it as **unresolved** and assert nothing
about where the thirteen are. Establishing that is a small discovery packet, not a guess.

| 3 (R8) | R8-D2 packet read (charter 6b) | claude-13 | `invoke-1788102458834-4272-cbb29944` | `park-d2feb5e6` | 15:07Z | reading | packet text at `BUILD-packets/R8-D2.md`; not yet dispatched to a builder | — |

### R8-D2 — pre-dispatch state (claude-20)

- **Acceptance dry-run done before the packet was written** (charter item 4 as amended). Reproduced the
  owner's registered partition exactly: 53 files, 792 forms, `:stored-F` 32 / `:missing-F-computable` 755
  / `:insufficient-inputs` 5, sum 792; the 2 forms lacking `:prediction-errors` are a subset of the 5
  lacking `:precision-state`; 0 stored-F forms lack inputs. Written into the packet as expected values.
- **Row-10 applied to my own bar.** The 755/32/5 census is satisfiable by counting keys, so I moved the
  evidence onto the recomputed-vs-stored comparison over the 32 forms: report the difference
  distribution, choose ε from it, and treat any difference beyond float noise as the finding. What would
  be surprising is stated in the packet; if nothing could be, the bar is a smoke test.
- **Routed to claude-13 before dispatch**, per charter 6b, asking specifically whether a builder could
  satisfy my packet without solving R8's problem. Not dispatched to a builder until that read returns.

### Two record problems for the owner (not mine to fix)

1. **`P-R8.md:66` is stale and contradicts its own amended `solved`.** The `solved` section now carries
   the three-disposition census; the R8-D2 delivery bullet still reads "the F checker with the falsifier
   as acceptance (**fires on all 88 today**)" — the pre-amendment form that R8-D1 refused. My packet
   quotes the bullet verbatim, as the charter requires, and marks it superseded in a flagged block so no
   builder reconciles it silently. The record still needs the edit.
2. **None of the `P-*.md` problem records are tracked in git.** `git ls-files holes/problems/` returns
   only my `BUILD-ledger.md` and `BUILD-packets/*` (plus one `facts-*` file); `P-R2.md`, `P-R8.md`,
   `P-R9.md`, `P-control-map-lint.md` and `BUILD-tech-lead-charter.md` are all `??` untracked. So the
   records every packet quotes, and every `solved` a status is written against, have **no version
   anchor**: there is no sha that says which text a builder was held to, and an amendment leaves no
   trace. The archived packet texts are anchored and the records they quote are not — which is the wrong
   way round. This is the reason the owner asked for `BUILD-packets/`; committing the records closes the
   same gap at its source.
| — | owner: records anchored | claude-15 | — | — | 15:08Z | done | e01dab9 | claude-15: P-R8:66 stale R8-D2 bullet rewritten to the three-disposition acceptance (claude-20 finding); nine P-*.md + charter committed on explicit paths as the version anchor for every `solved` a status is written against (claude-20 finding); R9-D1b discovery approved: name the repair-ledger artefact and its shape before R9-D2 |

### Correction (claude-20, same hour) — the two 32s ARE the same 32

In my bell to claude-15 I said the 32 forms carrying a stored `:variational-free-energy` and the 32
carrying `:selection-gain` "are not the same 32-form set" and that a checker written from the
coincidence would be wrong. **I had inferred that from the two counts matching and had not run it.**
Tested since, over the 792 forms by position: the two sets are **identical** — intersection 32,
difference 0 in both directions. Corrected with claude-15.

The true fact is the more interesting one and belongs in R8's record rather than in a warning: **F and g
are instrumented in exactly the same records.** Every form that stores a variational free energy also
carries a `:selection-gain` state, and every one of those states is `{:selection-gain 1.0
:perf-history [] :mean-perf nil :samples 0}`. So the trace entered its F-recording era and its
g-recording era at the same moment (07-14 per R8-D1), and in all 32 of those records g had never moved.
For a record whose problem statement is "two nouns under one number", the two nouns appearing together
and one of them being pinned across every appearance is evidence about the delivery, not a coincidence
to route around. R8-D2 does not depend on this either way; it is for the owner's S1.

Method note on my own error: this is log row 14's shape one level up — I stated a relation between two
measurements because their counts matched, without the one-line probe that would have settled it. The
probe cost ten seconds. Recording it here because the ledger is where this build's method errors go.

### R2-D1 — checklist items 2, 4 and 6, completed when my park fired (claude-20)

Items 1, 3, 5 and 7 were done at the close; 2, 4 and 6 were not. Completed and verified independently:

- **Item 2 — reader named, correctly.** The note states it read each of the 53 files with
  `clojure.edn/read` on a `java.io.PushbackReader` looping to a unique EOF sentinel, with
  `:default (fn [_ v] v)` for tagged values, and says explicitly it did **not** use `edn/read-string`
  (`R2-D1-findings.md:14-18`). That is the log-row-1 requirement met in the form the packet demanded.
- **Item 4 — the two 13-key records confirmed, and P-R2's falsifier is satisfiable.** My own reader
  loop over the corpus gives an observation-key distribution of exactly `{13 2, 14 790}` across 792
  forms. Both 13-key records are in `wm-trace-2026-05-18.edn` at `2026-05-18T19:42:49.284838608Z` and
  `2026-05-18T20:54:12.717822372Z`, and the single missing key in both is `:annotation-health`. So
  R2-D2's acceptance — "the falsifier fires on the two records and nothing else" — is **run and
  satisfiable**, with the expected values 2 fire / 790 pass (charter item 4, done before that packet
  is written).
  - **For R2-D2:** there is no `:tick` key on these records; `:tick` reads `nil` and the time is under
    `:timestamp`. My packet asked for the records "identified by tick" and the builder correctly
    reported timestamps and flagged file/form position as the record identifier. R2-D2 must not
    require a `:tick` field that does not exist.
- **Item 6 — the builder falsified an expectation I wrote, and is right.** My packet registered
  "`:acknowledged?` producers (expected: none)". Verified at source: `src/futon2/aif/lane_futility.clj:333`
  hard-codes `:acknowledged? true` in the map built for synthetic lane-futility nags. So a producer
  exists; what does not exist is *operator-acknowledgement persistence*, which is the production-path
  comment at `needs_you.clj:156-159`. Unqualified, my expectation was false.

**This is the fourth commissioner-side defect in this build and the second of mine** (after "54
entries"): rows 9, 10, 11 of the lifecycle log, then my count, now my expectation. It is also the most
interesting one, because of what the literal is doing. `nag?` is a four-term AND whose last term is
`acknowledged?` (`futon3c .../operator_lane.clj:29-33`), and the only thing in the corpus that supplies
that term is a hard-coded `true` on a synthetic path. R2's thesis is that the one channel the machine
cannot fabricate is the one it does not read; the narrower fact is that where the channel is consumed at
all, its value is fabricated by construction. That belongs in P-R2's `now` and is the owner's to write.

| 1 (R9) | R9-D1b (discovery, no code) | codex-5 | `invoke-1788102965707-4301-472672aa` | `park-0acbe2c7` | 15:16Z | closed | note pending path check; **I independently re-parsed `OBLIGATIONS.md` at `6c288174`**: 22 sections, 13 `fixed*`, 7 open, 2 without a marker — matches codex-5 exactly | pending |

### Crossed bell, reconciled by whistle (claude-20 / claude-15, 15:12Z)

claude-15's reply to my "two problems" bell restated my **original wrong claim** — that the 32 stored-F
forms and the 32 `:selection-gain` forms are "different sets" — and proposed writing it into R8-D2's
packet. It had been sent before my retraction (`…4280`) reached them: a textbook crossed bell, where a
reply and a new message pass each other. Per CLAUDE.md I switched to a **whistle** (one side only), which
is crossing-immune because the round-trip pairs request and answer atomically.

Reconciled: claude-15 had already reproduced the probe on receiving the retraction and written the *true*
fact into `P-R8.md`'s `now` (line 27, "F and g are instrumented in exactly the same records"). Verified
here: the only occurrence of "different sets" in P-R8 is the log line recording that I corrected it. My
R8-D2 packet is silent on the two sets, so no falsehood was shipped. Agreed rule for R8-D2: the packet
may state the true fact as *context*, and it is **not** an acceptance clause either way.

Worth keeping as a method note: the durable artefact was right the whole time. The error existed only in
one agent's reply text, and the thing that settled it in one round-trip was the record plus a ten-second
probe — not further correspondence.

| 3 (R8) | R8-D2 packet read (charter 6b) | claude-13 | `invoke-1788102458834-4272-cbb29944` | `park-d2feb5e6` | 15:07Z | **REFUSED** | reader found the packet's acceptance tautological; verified here; refusal belled to owner `invoke-1788103158556-4306-701b0734` | owner decision pending |

### Charter 6b justified itself on first use — R8-D2 refused before dispatch

claude-13 refused the packet I wrote. The finding: my row-10 relocation — census is tautological, so put
the evidence on the recompute-vs-stored comparison over the 32 — **landed in a second tautology.** The
trace serialises `observed`, `predicted-mean`, `error`, `predicted-variance`, `precision` and
`weighted-error` at full double precision, each exactly the arithmetic of its neighbours, so
recomputation cannot disagree with the stored value. Two independent routes: **32 exact zeros,
max |diff| 0.0.** Reproduced here.

The packet's wording made it worse: *"if every difference is at float-noise scale, say so and give the
max"* invites a builder to report **max = 0.0**, which reads as stronger than float noise while being a
function agreeing with itself. Log row 10, reproduced by the agent quoting log row 10.

**This reaches S1, which is why it went to the owner rather than being fixed here.** P-R8's amended
disposition (b) — recompute and compare within ε — cannot fail on this corpus for any ε ≥ 0. Not a bad
clause; an empty one.

**Replacement, checked here:** move the evidence to the **755** forms with no stored F, where
recomputation produces numbers nobody has seen — min **1.847**, max **10.638**, non-finite 0, no channel
missing `:precision` or `:error`. Falsifiable in advance, which the 32 comparison is not.

**New observation, from checking rather than from either agent's report:** the two populations are
**disjoint in value** — stored F on the 32 runs **0.1903–0.5223**; recomputed F on the 755 runs
**1.847–10.638**. The only ticks where F was recorded are the ticks where F is an order of magnitude
small. Observed; cause **inferred, untested** — could be a channel-set change, a regime change at 07-14,
or selection of which ticks got instrumented. It should be explained before an acceptance is built on
either population.

**Method note against myself:** while checking the disjointness my probe threw an NPE and my first
instinct was that the data had a nil. I chased it: there are **no** nils in the 755, and the exception
was my own bug — I passed whole forms to a function expecting their `:prediction-errors`. Had I reported
the exception as a data finding it would have been a fabricated defect, in a review whose whole purpose
is catching fabricated evidence.

| 2 (R2) | R2-D2 packet read (charter 6b) | claude-13 | `invoke-1788103402360-4312-b21331ee` | `park-cb287770` | 15:23Z | **REFUSED** | falsifier did not discriminate; four fixes applied by me; confirm-read `invoke-1788103740408-4316-8c1fd3d8` / `park-9fc02602` | — |

### R2-D2 — pre-dispatch state, written after claude-13's R8-D2 refusal (claude-20)

Every count run against the artefact before the packet was written (charter item 4):

- `Channel` from `src/futon2/aif/observation.clj` `observation-channels` (def at :11): **14**.
- Trace: 53 files, 792 forms; observation key-set distribution **790 × 14 keys, 2 × 13**; the two are in
  `wm-trace-2026-05-18.edn` at `…19:42:49.284838608Z` and `…20:54:12.717822372Z`, both missing
  `:annotation-health`. **P-R2's falsifier is satisfiable: expected 2 fire / 790 pass.**
- `channels-with-likelihood` from `src/futon2/aif/belief.clj:925-926`: **8**, with an **empty**
  difference against the declaration; **6** declared channels have no likelihood —
  `:consulting-pct :depositing-signal :loop-health :mathematics-pct :portfolio-pct :stack-pct`. So the
  record's "eight modeled, six `:n-a-by-design`" is exact.
- **A live inconsistency in the declaration, found here:** `observation.clj`'s namespace docstring says
  *"a 13-channel observation"* while `observation-channels` below it enumerates 14 — v0.10 added
  `:annotation-health` and the header sentence never followed. Identical in shape to `r18-badges.edn`
  (headline 4, parse 5) and consistent with the two 13-key records predating that channel. The packet
  requires the checker to key off the vector, never the docstring, and to report the drift as a typed
  finding rather than fix it.

**Applying claude-13's R8-D2 refusal rather than restating it.** Nearly every number above passes today,
so the census is a smoke test and the packet **says so in its own text** instead of presenting it as
evidence. The evidence is relocated to two *cross-artefact* comparisons — source declaration vs trace,
and `observation.clj` vs `belief.clj` — on the reader's own ground that a check is worth something when
it compares things written by different hands at different times. The one clause intended to be able to
fail is a fixture requirement: **a synthetic record with a fifteenth, undeclared key must fire**, which a
checker whose `Channel` was derived from the corpus cannot do — it would widen instead. Whether that
clause really discriminates is the specific question put to claude-13, because I am the wrong person to
answer it about my own packet.

### R9-D1b closed — the thirteen ARE identifiable rows, and they need a sha

codex-5 answered the question and I verified the parse rather than trusting it.

- **Artefact:** `p4ng/vetting/OBLIGATIONS.md` (in the **p4ng** repo, not futon2). It declares itself a
  row ledger and carries R9's rule in its own words at `:6-13`: *"The author (claude-4) may not mark a
  row closed on the strength of its own re-reading."*
- **Shape:** Markdown sections headed `## O…`, each with an inline `**Status: …**` marker in its body.
- **Vocabulary, and why my grep was meaningless:** the declared set is `open | fixed | withdrawn |
  disputed` (`:12-13`). **`closed` is not a token of this ledger** — closure is written `fixed` or
  `fixed by withdrawal`. My earlier word-count (24 open / 6 closed / 3 withdrawn / 4 corrected) was
  counting a word the artefact does not use as a status. It asserted nothing, correctly.
- **The thirteen, verified here.** At `6c288174` — the commit that *wrote* the "closed thirteen"
  sentence — my own parse of the section/status shape gives **22 sections, 13 `fixed*`, 7 open, 2 with
  no marker**, matching codex-5's `TOTAL=22 CLOSED=13 OPEN=7 MISSING=2`. The rows are
  **O1, O2, O3, O5, O6, O7, O8, O9, O14, O15, O16, O17, O20**. So they are identifiable, and R9-D1's
  refusal was about `sec-discussion.tex`, which genuinely does not enumerate them.
- **The corpus must be sha-pinned.** In the *current* file the same parse gives 24 sections and **14**
  closed — `O1d` was added later. The thirteen exist only at `6c288174`. R9-D2's corpus is therefore
  `OBLIGATIONS.md@6c288174`, not `OBLIGATIONS.md`. This is the version-anchor lesson (`e01dab9`)
  arriving a second time from the other direction: an unanchored corpus silently becomes a different
  corpus.
- **The closer is not a row field.** The ledger records the rule and the accusation but never who closed
  each row; codex-5 recovered the identity from `git blame` and **correctly refused to present VCS
  metadata as a row field**. That matters for R9-D2: `producingPart` must be *declared*, and here it
  would have to be constructed from git history — which is exactly the "declared, never inferred"
  boundary in P-R9's `solved` (1).
- **Refusal accepted:** `futon2/data/wm-repair-obligations/` is not this corpus (30 resolved / 54 open
  `repair-attempt-*` EDN records, a different shape). Searched and reported rather than assumed.

### R8-D2 — owner's new bar verified before it can be dispatched

claude-15 accepted claude-13's refusal, retired falsifier (b) as evidence (kept, labelled tautological),
moved the evidence to the 755, and made the two-population split the bar with an attribution requirement.
I reproduced the attribution probe independently: mean per-channel `:precision` **9.49** (stored 32) vs
**94.58** (missing 755) — a ~10× gap — against mean |error| 0.272 vs 0.312 and mean channels 8.00 vs 7.29.
Since F = ½·mean(Π ε²), a 10× precision gap accounts for the 10× F gap directly (stored 0.19–0.52 vs
recomputed 1.85–10.64), and neither error magnitude nor channel count does. The attribution is
computable; the *cause* stays `inferred, untested`, which is right. R8-D2 remains held on LH-D1b.
| 1 (R9) | owner gate: R9-D1b | codex-5 (via claude-20) | — | — | 15:29Z | passed | 6075b82 | claude-15 15:29Z: parse at 6c288174 reproduced (22 sections, 13 fixed, 7 open, 2 unmarked O21/O1c; the thirteen ids match); both refusals upheld (wrong-shape corpus; git blame is not a row field); corpus sha-pinned in P-R9; producingPart from a cited declaration record — D2 reports both instruments (ledger alone → unknown; paper's own sentence → self); note transcribed by claude-20 with authorship marked — acceptable, and the builder is told next time to commit its own note |

### R2-D2 refused too — and the finding is sharper than R8-D2's

claude-13 refused the second packet I wrote. Unlike R8-D2 this one stays with me: all four findings are
in my packet text, none reaches P-R2's S1, so I fixed them directly (charter: small findings, fix and
say so).

**1. The falsifier did not discriminate at all.** I built `Channel` three ways and ran the contract:

    source-declared (14) -> [2 fires, 790 passes]
    trace-UNION     (14) -> [2 fires, 790 passes]
    trace-MODAL     (14) -> [2 fires, 790 passes]
    union == modal == declared, on this corpus, today

A builder deriving `Channel` from the corpus produces exactly my expected 2/790. **And my stated reason
was wrong in a way that matters more than the clause:** the packet said a corpus-derived `Channel` makes
the contract "true by construction". It does not — union-derivation still fires on the two short
records. It gives the right answer for the *wrong reason*, which is worse than a tautology because it
looks like it works, and it fails silently the first time a channel is added or removed. That is the
drift the record exists to catch, so the checker would break exactly when it was needed.

**2. My fixture could be passed while still trace-derived.** A synthetic record appended to the real
corpus is caught only if the derivation happens over the input under test; a checker that derives
`Channel` from a fixed corpus path and takes the record as a separate input fires on it correctly and
stays self-certifying on the real data — the natural shape for a bb script with a default path.
**Replacement, which I ran before writing it in:** a fixture *corpus* whose records all carry an
undeclared fifteenth key gives `[5 fires, 0 passes]` source-keyed and `[0 fires, 5 passes]`
union-derived. Opposite outcomes, so it discriminates both ways. Plus: the report must name the file and
line `Channel` was read from, so provenance is auditable rather than inferred from behaviour.

**3. `likelihood` as I wrote it was copying a set.** `(if (contains? channels-with-likelihood c) Some
None)` is total by construction. Now: the six `None`s enumerated explicitly and asserted equal to
`observation-channels ∖ channels-with-likelihood` — and **I had the direction wrong**. I was checking for
members of `channels-with-likelihood` absent from the declaration, which is empty and will stay empty;
the drift that will actually happen is a new channel added to `observation.clj` alone, landing in neither
list.

**4. The turn-channel prohibition was by name, not by kind.** I forbade `:operator-turn-count`; nothing
stopped `:turn-signal` or `:acknowledged-ratio`. The record's objection is *presence, not content* — a
property of the quantity, not the key — so it is now stated by kind.

**Standing count: six commissioner-side defects in this build, four of them mine** ("54 entries"; the
`:acknowledged?` expectation; R8-D2's empty comparison; R2-D2's non-discriminating falsifier). Every one
was caught by a builder or a reader refusing rather than complying, and none reached a builder's hands
except the "54", which two builders independently corrected. Charter 6b has now refused two of two build
packets it has read — which is either a very good reader or a tech lead who writes bars that pass on
today's data. On the evidence it is the second, and the reader is the control that catches it.

### R8-D1 checklist item 4, completed when my park fired — and it corrects R8-D2's new bar

Items 1, 2, 3, 5, 6 were done at the close. **Item 4** — "check F recomputation is compared against
whatever sits under `:free-energy`, with the key named" — was not, and completing it changes the
attribution the owner just wrote into P-R8.

R8-D1's note (`:60-65`) claims `:free-energy` has **two shapes**. Verified here, and the separation is
total:

    the 32 WITH a stored F   -> :free-energy is a CONTROLLER map (:controller-score) : 32/32
    the 760 WITHOUT          -> :free-energy is a G map (:G-total)                   : 760/760
    files holding the 32: 2026-07-14, 07-15, 07-16, 07-17, 07-18, 07-19, 07-21, 08-30
    files holding the rest: 2026-05-18 … 2026-07-09
    no file contains both shapes; the boundary is exactly 07-14

**So the "two populations" are two schema eras, not two samples of one process.** Everything that
distinguishes them moves at the same instant, 2026-07-14: `:variational-free-energy` appears,
`:selection-gain` appears (pinned `1.0`, `:samples 0`, and never moves), `:free-energy` is reshaped from
a G map to a controller map, and mean per-channel `:precision` drops from ~94.6 to ~9.5.

**Consequence for R8-D2, which is still held on LH-D1b, so there is time to fix it.** The owner's
acceptance (iii) asks the builder to reproduce the split and *attribute it to a per-channel field*, with
the expected attribution "precision scale, ~10×". That attribution is arithmetically true — F =
½·mean(Π ε²), so a 10× precision gap gives a 10× F gap, and neither |error| (0.272 vs 0.312) nor channel
count (8.00 vs 7.29) does — but it is the **proximate** driver, not the explanation. Precision changed
because the schema era changed; a builder asked to attribute to a per-channel field will name precision
and stop, and the record will then carry "the split is a precision-scale effect" when what the data shows
is a single dated schema change that moved four things at once. I stated the arithmetic version to the
owner earlier today and it was incomplete in exactly this way.

**Recommended amendment:** acceptance (iii) should require the builder to report the split *by era* —
partition on the `:free-energy` shape, or on the 07-14 date, and show that the F populations, the
precision scale, the presence of stored F and the presence of `:selection-gain` all move together at one
boundary. `:unexplained-regime` remains a permitted outcome. The **cause** of the era change stays
`inferred, untested`: nothing here says whether 07-14 was a precision-state reset, a channel
recalibration, or a change in which ticks were instrumented, and I am not guessing.
| — | owner: LH-D1b gate + D2 hold lifted | codex-22 | — | — | 15:31Z | passed | mathlib4 3b8e2ceb+b98b2500 | claude-15 15:31Z: 15 sorry = 15 HOLE laws/impls, 14 bodies; four bodies corrected to record text; declaration list belled to claude-20; D2 packets must quote their declarations |

| 2 (R2) | R2-D2 (build) | codex-1 | `invoke-1788103900715-4321-da824f13` | `park-e8638688` | 15:31Z | **CANCELLED — dispatched inside a hold** | cancelled by me on receipt of the hold; codex-1 had written 231+69 lines, **uncommitted**, no lane closed | — |

### R2-D2 dispatched — what the second read added for free

claude-13 confirmed all four fixes closed and flagged two residuals, both one-liners, both folded in
before dispatch:

- **The provenance requirement was self-assertable.** "The report must name the file and line `Channel`
  was read from" can be satisfied by emitting `:channel-source "observation.clj:11"` as a literal while
  deriving from the trace — the same shape as `claimPersisted : Bool`, which this stack already treats
  as a facade. Closed by requiring the report to state `Channel` **as a vector in declaration order**
  (`:loop-health :support-coverage :attack-coverage :mission-health :stack-pct …`). That order is
  neither alphabetical nor anything a set or map-union yields, so only reading the declaration
  reproduces it: the report now witnesses its own provenance instead of asserting it.
- **The turn prohibition was still enumerative.** "Derivable from turn presence or count alone" leaves
  `:turn-recency` and `:seconds-since-last-turn` open — a timestamp is neither presence nor count and
  still reads no type. Restated positively: the value must be a function of the turn's **typed content**
  (✘ / ✓ / 💡); anything derivable from turns without reading their type is forbidden. Describe the kind,
  do not enumerate the instances — which is this build's own recurring lesson, and I had now failed it
  twice in the same clause.

Both rounds cost about twenty minutes of reader time. Against that: the first version of this packet had
an acceptance that three different `Channel` derivations satisfied identically, so a builder could have
shipped a self-certifying checker with every gate green.
| 3 (R8) | owner: acceptance (iii) → by era | claude-15 | — | — | 15:34Z | amended | P-R8@HEAD | claude-15 15:34Z: two schema eras verified (760 :G-total / 32 :controller-score; boundary 07-14; no mixed file); D2 reports the era and the four co-moving facts, precision as proximate driver, cause untested; R8-D2 still waits on the Holes.lean quote |
| 2 (R2) | R2-D2 dispatched | codex-1 (via claude-20) | invoke-1788103900715-4321-da824f13 | park-e8638688 | 15:34Z | **CANCELLED — dispatched inside a hold** | cancelled by me on receipt of the hold; codex-1 had written 231+69 lines, **uncommitted**, no lane closed | claude-15: noted — two claude-13 reads (first refused: non-discriminating falsifier; fixed with a fixture corpus [5,0] vs [0,5]; declaration-order Channel vector as self-witnessing provenance); owner gate at close |

## HOLD on all D2+ dispatch (claude-15 on Joe's direction, 2026-08-30 ~15:18Z)

Joe: *"I worry about implementation running ahead of the Lean without those being coordinated … they
need to be coordinating with each other around interfaces using these tetrahedral model ideas, not just
working in parallel."* Charter gains **"Interfaces are Lean declarations"**: a D2+ packet quotes the
term's declaration from `mathlib4/DarkTower/WarMachine/Holes.lean`; the row-11 check compares the
packet's signature to the declaration mechanically; **a lane closes only when the hole moves**
(`sorry` → body, or a stated theorem whose fixture is the Clojure run); builders never edit `Holes.lean`.

### I breached the hold, and the breach is mine to record

The hold bell was **sent ~15:18Z and delivered to me after 15:31Z**. I dispatched **R2-D2 to codex-1 at
15:31Z**, inside the window and without knowledge of it. On reading the bell I cancelled the job
(`invoke-1788103900715-4321-da824f13` → `state: cancelled, finalized: true`). codex-1 had already
written `checks/r2_channel_contract.clj` (231 lines) and `test/r2_channel_contract_test.clj` (69 lines).
**Both are uncommitted and untracked; no delivery landed and no lane closed.** I did not delete them —
they are another agent's work and are probably most of R2-D2 once the hold lifts — but this checkout is
shared, so they are at risk of being swept into an unrelated `commit -a` and someone should decide their
fate deliberately. From its head, codex-1 had honoured the hard clause: `likelihood-none-channels` is
enumerated explicitly, with a comment that computing it from the difference would let a newly declared
channel classify itself.

Not an excuse, but the mechanism worth recording: **a bell can be overtaken by the work it governs.**
A hold issued at 15:18 and read at 15:35 does not stop a dispatch at 15:31. Nothing in the protocol
timestamps a directive against the actions it is meant to bind.

### Reading the declarations against the artefacts — two interface defects

`Holes.lean` landed (15:30Z, 12,668 bytes); `scripts/count-holes.sh` reports **14 with-body / 15
with-sorry** (P-R2 0/1, P-R8 1/1, P-R9 1/1, P-validated-R5 9/12). Comparing the declarations to the data:

1. **`r2ObservationKeysAreChannels` is vacuous or refuted, and the declaration does not say which.** It
   states `∀ tick, {channel | (tick.observation channel).isSome} = Set.univ`, with `Channel` an abstract
   type parameter. Instantiate `Channel` as the declared 14 and the two 05-18 records refute it;
   instantiate it as whatever keys a record carries and it is trivially true. **This is the same defect
   claude-13 caught in my R2-D2 packet, one level up** — the meaning depends entirely on where `Channel`
   comes from, and neither artefact pins it. The Clojure side is now pinned (ordered-vector provenance);
   the Lean side is not. Under charter clause 3 this hole cannot move `sorry` → body: the honest move is
   a stated theorem whose fixture is the run, where the run is the *refutation*.
2. **`r8StoredFRecomputes` encodes the clause the owner just retired.** It states that stored F agrees
   with recomputation within ε — precisely the comparison claude-13 proved cannot fail here (32 exact
   zeros) and which P-R8 removed as evidence. `Holes.lean` and the record are out of step on R8's only
   open hole. Separately, `r8Disposition` keys `insufficientInputs` off `muPre/observation/precision/
   storedF` while the Clojure disposition keys off `:prediction-errors`/`:precision-state` — a signature
   mismatch that charter clause 2's mechanical comparison should catch on R8-D2's first dry-run.

**Status of the stated blocker:** LH-D1 is `done`, `P-lean-holes.md` exists, and **codex-22 is idle with
no running invoke** — so nothing Lean-side is in flight. R8-D2 waits only on the owner's review of
`Holes.lean` and on reconciling defect 2 above.
| — | owner: hold-reply gate | claude-20 | — | — | 15:37Z | passed | mathlib4 (see next) | claude-15 15:37Z: breach acknowledged (bell latency; cancelled; no delivery landed); codex-1's uncommitted checks/r2_channel_contract.clj + test kept in place as R2-D2's starting point — never to be swept into another commit; both Holes.lean findings upheld and fixed by the owner; charter 6 (tech lead proposes hole text, owner ratifies) accepted |

### R9-D1b owner gate PASSED — and R9-D2 now has an interface problem, not a corpus problem

Both decisions written into P-R9's `solved`: **(a)** corpus is `OBLIGATIONS.md@6c288174`; **(b)** D2 runs
**twice** — against the ledger alone the thirteen return `unknown` (no per-row closer, and inferring one
from git would violate `solved` (1)); with `sec-discussion.tex:238` as the declaration record they return
`self`. Both reported. The finding the node exists for: *the artefact built to record closures cannot say
who closed them* — self-certification was undetectable from the ledger and detectable only because the
author admitted it elsewhere.

**Parser note for D2, sharper than the owner's.** The status marker is not one string. At the pinned sha
the twenty marked rows carry six distinct forms — `fixed` (7), `fixed.` (5), `fixed by withdrawal.` (1),
`open.` (4), `open` (2), `open, partial progress 2026-07-31.` (1) — and the current file adds a seventh,
`fixed (declared, not renumbered).`. An equality test on `"fixed"` returns **7**, not 13. D2's parser must
normalise: strip the trailing period, treat a trailing clause after a comma or parenthesis as commentary,
and treat `fixed by withdrawal` as closure. This is the prose-grep lesson moved inside the parser.

*(My own instrument slipped again here: a `grep | sort -rn | head -6` over the current file showed 13
closures and I briefly took codex-5's 14 for an error. The 14th form was on line 7 of the output, cut off
by my own `head`. codex-5 was right; the truncation was mine. Third instrument error of the day and the
same one twice — `head` on a distribution I had not counted first.)*

**The interface problem.** `Holes.lean` carries exactly two P-R9 declarations (`count-holes.sh`: 1 body,
1 sorry):

- `independent` — **CLOSED-BY-RECORD**, body `witness.producer ∉ claim.producingPart`;
- `valueEvidenceRequiresL2` — **the only open HOLE**: `valueEvidence w → w.layer = Layer.L2`.

Two mismatches against the acceptance just decided:

1. **R9-D2 would not move the R9 hole.** The decided work is the two-run independence verdict over the
   thirteen closures. The only open hole is about *value evidence requiring L2*, which that work does not
   touch. Under charter clause 3 — "a lane closes only when the hole moves" — **R9-D2 as specified cannot
   close its lane**, however well it is built. Either a hole is needed for the verdict property itself
   (e.g. "a claim whose only witness shares its producer returns `self`", which is P-R9's stated
   falsifier), or D2's scope has to reach the L2 property.
2. **The verdict is three-valued in the record and two-valued in the Lean.** P-R9 `solved` (2) requires
   the checker to return `independent | self | unknown` per witness; `independent` is a `Prop` — true or
   false, no third value. `unknown` is exactly the value decision (b) makes load-bearing, and it has no
   representation in the declaration. This is the signature comparison of charter clause 2 failing on its
   first real use, which is what that clause is for.

Neither is mine to fix: `Holes.lean` is the owner's and the tech lead proposes by bell. Both sent.
| 1 (R9) | owner: signature proposals ratified | claude-20 → claude-15 | — | — | 15:39Z | done | mathlib4 93f0da26 | claude-15 15:39Z: clause 6 first use — three-valued IndependenceVerdict, independenceVerdict decision procedure, holes r9VerdictSound + r9TwoRunCensus (R9-D2 moves these); parser note (six status forms) in P-R9; Holes.lean now 17 bodies / 18 holes; R9-D2 packet may be written from P-R9@HEAD |

| 1 (R9) | R9-D2 packet read (charter 6b) | claude-13 | `invoke-1788104652626-4332-7e3d432e` | `park-b700dfd7` | 15:44Z | reading | packet `BUILD-packets/R9-D2.md` (219 lines); **signature diff CLEAN** vs `Holes.lean:171-218` (one trailing blank line only) | — |

### A stale park payload woke me on R8-D2 — no action taken, and why

My own R8-D2 checklist fired carrying claude-13's refusal. Ledger checked first, per the payload's own
last line. Its steps 2 and 3 are both overtaken: the read did **not** pass (it refused), and the packet
cannot be "fixed" because the owner has since rewritten P-R8 and `r8StoredFRecomputes` — the clause it
was built around — no longer exists. The packet on disk still references it. **No dispatch.** R8-D2 must
be written afresh against `r8Census` and `r8EraBoundary`; R2-D2 likewise against `r2WellFormed` and
`r2ContractCensus`, since `r2ObservationKeysAreChannels` is also gone.

Second instance today of a message being overtaken by the work it governs (the first was the hold bell
arriving after the dispatch it forbade). A park payload is a *plan written in the past*; when it fires,
the ledger is the authority, not the payload.

### R9-D2 written and read — the pre-dispatch work

Charter item 4, all run against the artefacts: corpus pinned to `OBLIGATIONS.md@6c288174` (22 sections,
13 closed, 7 open, 2 unmarked; the thirteen ids listed) because HEAD gives 24/14. **Charter clause 2's
mechanical signature comparison, first use:** diffed the packet's quoted interface block against
`Holes.lean:171-218` — identical but for one trailing blank line my extraction dropped, no divergence in
any declaration.

**I named my own tautologies in the packet before claude-13 could.** Run (i) — ledger alone → all
thirteen `unknown` — is literally the Lean body's first match arm (`| none => .unknown`), so any correct
implementation returns it. Run (ii) — the paper's sentence as declaration → all `self` — cannot fail
either, because that one sentence supplies both the declaring part *and* every row's producer. Both are
labelled "report it, do not call it evidence", and I have asked claude-13 whether labelling is enough or
run (ii) should be cut outright.

**The evidence is moved to two things that can fail:** (A) `r9VerdictSound` on synthetic witnesses, with
a deliberately-wrong `decide?` required in the fixture so the soundness hypothesis does work; and (B) a
counted prose/declaration gap — scanning the thirteen rows for attribution tokens gives hits in **8 of
13**, including explicit `codex-1` and `codex-7` in O7 and O14. R9-D1b established there is no per-row
closer *field*; the prose is not silent. Since `solved` (1) requires `producingPart` declared and never
inferred, the packet **forbids** feeding those prose producers to the checker and asks for them counted
as a finding instead. If it holds, the node's thesis sharpens: not merely that the artefact cannot say
who closed these rows, but that it gestures at who, in a form the independence rule may not consume.
My 8-of-13 is a token scan and is marked as needing a proper count; I have explicitly asked claude-13
whether that is a real finding or a dressed-up grep.

### R8-D2 rewritten against the new declarations — written, NOT yet sent to claude-13

The owner reproduced the era finding and amended acceptance (iii) to report the split **by era**, with
precision named only as the proximate arithmetic driver and the cause left `inferred, untested`. Packet
rewritten from `P-R8.md@HEAD` against the **current** Lean declarations. **Charter clause 2:** the quoted
interface block diffs **clean** against `Holes.lean:278-320`.

**A third message overtaken by its own file.** The owner's reply says R8-D2 waits on the `Holes.lean`
quote "(`r8Disposition` closed, `r8StoredFRecomputes` hole)". `r8StoredFRecomputes` **no longer exists** —
R8's declarations are now `R8Disposition`, `R8Tick`, `r8Disposition` (body), `r8Census` (hole),
`r8EraBoundary` (hole). Already flagged in my hold-lift reply; noted again because it is the third time
today a list inside a message has been overtaken by the file it describes, after the hold bell and the
declaration list. The charter's own sentence — *quote signatures from the file, not from this list* — is
the control that keeps catching it.

**Row-11 work, all run before the packet was written.** Two findings worth keeping:

1. **The era biconditional is falsifiable in principle and true in fact.** `r8EraBoundary` asserts, for
   *every* tick, `storedF present ↔ fileDate ≥ boundary`. That is stronger than what I had measured, so I
   tested it: boundary `20260714`, **0 violations in each direction** across all 792 forms. One form
   dated after the boundary without a stored F would have broken it. This is the strongest clause in the
   packet, and it is a different epistemic animal from the retired recompute identity, which could not
   have failed for any ε ≥ 0. The packet labels all three quantities explicitly — **cannot fail**
   (recompute identity, ε reported not tuned, and on this corpus 0), **could have failed and did not**
   (the era biconditional), **can fail** (the 755 recomputation, non-finite count expected 0 but not
   guaranteed).
2. **760 and 755 are two different partitions of the same corpus, and 760 = 755 + 5.** The era partition
   splits on stored-F presence (760 / 32); the disposition census splits the 760 further into 755 with
   usable inputs and 5 without. Both numbers are in the record, so a builder can use one where it means
   the other and still look consistent. The packet requires each count to name its partition.

**Held, not sent.** claude-13 is mid-read on R9-D2 (`invoke-1788104652626-4332-7e3d432e`). Queuing a
second packet on the same reader while the first is open invites exactly the cross-threading the whistle
protocol exists to undo, and reader attention is the build's real bottleneck — not seats. R8-D2 goes over
as soon as R9-D2's read returns. R2-D2 is third, and still needs rewriting against `r2WellFormed` /
`r2ContractCensus`.

| 2 (R2) | R2-D2 (build, re-dispatch) | codex-1 | `invoke-1788104980439-4336-12173cfd` | `park-97da4143` | 15:49Z | **REFUSED** (job API says `failed`) | codex-1 refused on the signature: `r2ContractCensus` is FALSE as stated; verified here; signature proposal belled to owner | — |
| 3 (R8) | R8-D2 packet read (charter 6b, 2nd) | claude-13 | `invoke-1788104984233-4337-a785b4b0` | `park-12524473` | 15:49Z | reading | rewritten vs `r8Census`/`r8EraBoundary`; **signature diff CLEAN** vs `Holes.lean:278-320` | — |

### R9-D2 refused by claude-13 — and the blocker is in the Lean, not the packet

Third read, third refusal, and the sharpest. Two findings I am fixing in the packet, one I cannot:

- **Run (ii) should be cut, not labelled.** I had marked it tautological; claude-13 gave the better
  reason: run (ii) is uniform *because the packet forbids the one input that would break it* — the prose
  producers. So it reports a consequence of my own prohibition, not a property of the corpus, and a
  builder may read it as a corpus finding. It also buys no coverage: the only branch it exercises beyond
  run (i) is `some claim`, which (A)'s synthetic witnesses exercise with controlled inputs. Cut to one
  sentence.
- **My 8-of-13 conflated two findings and the weak one is what a builder would deliver.** Verified the
  split myself: **3 of 13 name a specific agent** — O7 `codex-1`, O14 `codex-1` + `codex-7`, O15 `zai` —
  **5 carry only generic role words** (O1 O3 O5 O8 O20), **5 neither** (O2 O6 O9 O16 O17). "Author" and
  "reviewer" are boilerplate and counting them inflated the number. The **3** is what bites, because it
  is my own "surprising" outcome having already occurred: the paper's blanket *"the author then closed
  thirteen"* is too strong in three rows. claude-13 also caught a third agent I had missed — `zai` in
  O15, next to a literal "fixed by".

**The blocker — a Lean defect, not a packet defect.** `r9VerdictSound` carries
`_sound : ∀ p S, decide? p S = true ↔ p ∈ S` as a **hypothesis**, which pins `decide?` to membership. Both
conjuncts then follow by unfolding the match — `simp [independenceVerdict, independent]` discharges it —
and **no wrong `decide?` can ever fail it**, because a wrong one does not satisfy `_sound` and the
implication is vacuously true. So my packet's instruction ("include a deliberately wrong `decide?` and
show the property fails") asks for something the signature cannot express, and the hole would move by
`simp` while the Clojure test demonstrated only that the port is faithful. Under charter clause 3 that is
a facade close. **Proposed to the owner as hole text (charter 6, first use).** R9-D2 held until ratified.

### R2-D2 refused by the BUILDER, on the Lean signature — and it is right

codex-1 changed no files and refused: **`r2ContractCensus` is false as stated.** Verified here by reading
the declaration (`Holes.lean:271-276`):

    ∀ … (illFormed : Nat), (corpus.filter (fun tick => !wellFormed? tick)).length = illFormed

`illFormed` is **universally quantified and then asserted equal to a fixed quantity**. For any corpus the
left side is one particular natural `n`, and the statement claims `n = illFormed` for *every* natural —
false as soon as `illFormed ≠ n`. codex-1's counterexample (`Channel := Empty`, empty corpus,
`illFormed := 1` ⇒ `0 = 1`) is valid, and the defect is more general than the counterexample: the
declaration is false for **every** instantiation, not only degenerate ones. The prose promised
`illFormed = 2`; the type never says so. Its proposed shape — make the census a computed `Nat` and state
a concrete fixture theorem against it — is the right one, and is belled to the owner as a signature
proposal (charter clause 4).

**This is the second Lean defect of the same family in one hour**, and worth naming as a class: in
`r9VerdictSound` a hypothesis (`_sound`) is placed so the interesting case can never reach the
conclusion; in `r2ContractCensus` a quantifier is placed so the conclusion asserts far more than the
prose. **Both are statements that do not say what their docstring says**, and neither is detectable by
reading the docstring — only by asking "what would falsify this?" Charter clause 2's mechanical diff
catches *drift between packet and file*; it does not catch *a declaration that is wrong*. What caught
both was a reader and a builder each asked to attack the interface rather than implement it.

**Process note: a refusal is reported by the job API as `state: failed`.** The lane is not broken and the
builder did exactly what the charter asks; nothing in the API distinguishes "refused on principle" from
"crashed". Anyone reading job states without opening the result would mis-read this lane as an
infrastructure failure. Worth a field, and worth remembering when triaging.
| — | AD-D2 pre-dispatch read | claude-13 | invoke-1788105138524-4340-685b3eef | park-0f91b639-9d4e-4e06-a6e9-b55eefd6ef7d | 15:52Z | running | packet BUILD-packets/AD-D2.md | claude-15: charter 6b on an owner lane; dispatch to codex-22 on PASS |
| — | owner: clause-4 proposal ratified (family fix) | codex-1 → claude-20 → claude-15 | — | — | 15:54Z | done | mathlib4 c131af37 | claude-15 15:54Z: r2ContractCensus/r8Census computed + wm-trace holes (=2; =(755,32,5)); r9CheckerSound predicate + r9WmCheckerSound hole; R2-D2 and R9-D2 UNBLOCKED — quote Holes.lean@c131af37; refusal-as-failed API finding noted |

### R9-D1b checklist items 2 and 5, completed when my park fired — and a second parser trap

The lane was closed and owner-gated; items 1, 3, 4, 6, 7 were done at the close. **Items 2 and 5 were
not**, and they mattered more than usual here because `R9-D1b-findings.md` is **my own transcription** of
codex-5's reply, so nobody had independently checked its pointers.

- **Item 5 — marking discipline: clean.** Every claim in the note carries an attribution marker
  (`[codex-5]` or `[claude-20 verified]`); no unmarked assertions.
- **Item 2 — three pointers opened, not grepped, all exact.** `OBLIGATIONS.md:6-13` carries both quoted
  sentences and the `open | fixed | withdrawn | disputed` vocabulary; `O1` heads at `:17` with
  `**Status: fixed**` at `:44`; `O20` heads at `:363` with `**Status: fixed.**` at `:393`; and the
  accusation is at `:404-407` verbatim — *"The author has since closed thirteen. There is no independent
  close-verification anywhere in the apparatus."*

**New finding from opening them — a second parser trap, now in the R9-D2 packet.** Three of the thirteen
(**O1, O2, O5**) carry a *second* bold segment on the same status line naming a residual sub-obligation
with a **different** status: `**Status: fixed** … **Residual obligation O1b: open**`, and likewise
`O2b: open`, `O5b: open`. A parser using `findall` over the bold pattern picks up both and can count
these rows as open or double-count them. The packet now says: take the first `**Status: …**` on the line
and stop; the sub-obligations are not among the thirteen and are not the builder's to classify.

That is the third distinct way this one artefact can be miscounted — after the six status-string forms
and the `"fixed"`-equality trap — and all three were found by reading rows rather than by parsing them.
The accusation row itself carries a fourth variant (`*Status: OPEN — …*`, single asterisks, italic) but
sits outside the `## O` sections and so outside the census.
