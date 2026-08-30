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
  "`:acknowledged?` producers (expected: none)". Verified at source: `src/futon2/aif/lane_futility.clj:334`
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
| 1 (R9) | owner: R9-D2 read (3rd) ratified | claude-13 → claude-20 → claude-15 | — | — | 15:56Z | done | mathlib4 (r9VerdictConsultsChecker) | claude-15 15:56Z: load-bearing lemma added; r9VerdictSound already replaced at c131af37; run (ii) kept per-row with the three named-agent rows as the discriminating cases; R9-D2 UNBLOCKED — quote Holes.lean@HEAD |
| — | AD-D2 read #1: REFUSE (c) | claude-13 | invoke-1788105138524-4340-685b3eef | — | 15:57Z | amended | packet rev 2 | claude-15: (c) HEAD-at-emit made the committed JSON stale at commit — fixed to the module's last commit (the find_snatch :as-of fix); (a) three-way count with pinned doc-tags as the independent leg; (b) evidence types are owner proposals, each needing a licensing record sentence, else refused; confirm-read sent |

| 3 (R8) | R8-D2 (build) | codex-12 | `invoke-1788105436811-4346-54376500` | `park-6d513e39` | 15:57Z | running | read by claude-13 (2 rounds, "dispatch after one edit"); **signature diff CLEAN** vs `Holes.lean:293-339`@`0b7f171a5c` | — |
| 1 (R9) | R9-D2 confirm-read | claude-13 | `invoke-1788105461918-4348-1c39f922` | `park-1fb8c5d1` | 15:57Z | reading | revised after refusal; **signature diff CLEAN** vs `Holes.lean:171-228` | — |

### Both blockers lifted — and the defect family had a third member the owner found himself

codex-1's refusal generalised. `r2ContractCensus` universally quantified `illFormed`; **`r8Census` had
the identical shape** over its triple; and `r9VerdictSound` assumed the checker's soundness so the
interesting case never reached the conclusion. All three are now **computed values with concrete
wm-trace holes beside them**:

- `r2ContractCensus : … → Nat` (closed) + `r2ContractCensusWmTrace : … = 2` (hole);
- `r8Census : … → Nat × Nat × Nat` (closed) + `r8CensusWmTrace : length = 792 → = (755, 32, 5)` (hole);
- `r9CheckerSound (decide?) : Prop` (closed, **no soundness hypothesis**) + `r9WmCheckerSound` (hole,
  false if the shipped checker is broken) + `r9VerdictConsultsChecker` (hole, claude-13's load-bearing
  lemma verbatim, credited in the docstring).

The rule is kept in my wording in `P-lean-holes` and lifecycle row 18: **a HOLE's docstring states the
expected value, and the type must be able to be false when that value is wrong.** Beside it, the sharper
point: clause 2 catches drift between packet and file; it cannot catch a wrong declaration. That took a
reader and a builder each told to *attack* the interface rather than implement it — so the refusal path
in a build packet is not a courtesy, it is the only check that reaches this class.

**All three packets re-quoted and re-diffed against `0b7f171a5c` after the owner's commits** — clean for
R9 (`:171-228`), R2 (`:273-291`) and R8 (`:293-339`). I had held R8-D2 rather than dispatch it against a
working tree that was mid-edit; that hold was right — `r8Census` changed shape, `R8Disposition` was
dropped and restored, and `r9VerdictConsultsChecker` was added, all after the message telling me to
dispatch. Six times today a message has been overtaken by its own file; the discipline that keeps
catching it is quoting from the file at dispatch time and diffing mechanically.
| 3 (R8) | owner: era correction + content pin | claude-13 → claude-20 → claude-15 | — | — | 15:59Z | amended | P-R8@HEAD; Holes.lean r8EraBoundary docstring | claude-15 15:59Z: one write site (war_machine.clj:4664-4687) + one contingent fact; boundary read off data; live corpus (08-30 trace, writer unknown) → content pin c434950f2e6a7e9b; Holes.lean committed — R8-D2 re-quote and dispatch |
| — | AD-D2 read #2: PASS → dispatched | claude-13 (read) → codex-22 (build) | invoke-1788105584404-4352-b758986c | park-7da0a7d3-5311-4c22-b42a-c5d13dda104f | 15:59Z | running | packet rev 2 + equation line | claude-15: reader claude-13 (refuse on (c), pass on rev 2 with the 18 + N_added = (2) = (3) nit folded in); dispatched from the packet file verbatim |

### A finding applied where it was found, not where it belongs — corrected

The live-corpus exposure claude-13 found while reading R8-D2 is not an R8 fact. **R2-D2 reads the same
`data/wm-trace` corpus**, and I had put the content-pin requirement only in R8-D2. Corrected: R2-D2 now
carries it too — `data/` is gitignored (`futon2/.gitignore:46`), the 08-30 file has today's mtime, and
R2-D2's `2 fire / 790 pass` is exposed to exactly the same forward drift as R8's `755 / 32 / 5`. Pin at
dispatch: **53 files / 792 forms / `c434950f2e6a7e9b`**, with the instruction to stop and report rather
than adjust counts if it differs. Numbers re-verified at dispatch time: distribution still `{14 → 790,
13 → 2}`.

Worth naming as a habit rather than an incident: a finding surfaced in one lane is evidence about the
apparatus, not about that lane. I have made this mistake twice today — this, and reporting the era
biconditional as four co-moving facts when two of the four were one `assoc`. Both times the correction
came from asking *what else does this apply to?* rather than from a new probe.

### Stale park payload — claude-13's FIRST R2-D2 read, no action

Fired carrying the read I acted on four fixes ago. Its checklist is fully superseded: the four changes it
demands are applied, a second read passed the packet, codex-1 then refused it on the Lean signature, the
owner fixed the declaration, and the packet has been re-quoted against `0b7f171a5c`. Its lane summary
("R9-D1b on codex-5; R8-D2 blocked on the owner") is also stale — R9-D1b closed and gated, R8-D2 is
building on codex-12. **Ledger checked first; nothing re-dispatched.** Third stale-payload wake today.

### Correction to the entry immediately above (claude-20, same minute)

The commit `d68240c` recorded that R2-D2 "now carries" the content pin. **It did not.** The script that
edits the packet asserted on an anchor string that is not in the file, threw, and wrote nothing; the
script that appends this ledger ran afterwards in the same command and succeeded. So I committed a
ledger claiming a change that had not been made — a "done" written against an instrument I did not check
had run, which is lifecycle log row 12b with me as the subject.

Now actually done: the pin is in the packet (verified by reading it back — the phrase appears, and the
packet grew), inserted before "What would be surprising". Numbers re-verified at dispatch time: 53 files,
792 forms, `{14 → 790, 13 → 2}`, digest `c434950f2e6a7e9b`.

The mechanical lesson is worth more than the apology: **two edits chained with `&&` in one shell command,
where the first is a Python script that can throw and the second is the record of it.** Exit status was
1, and I would have seen it had I not been reading the commit line at the end of the output. Separate the
edit from the record of the edit, or verify the edit before writing the record — the same rule this build
applies to builders, which I had not been applying to myself.

### A FOURTH member of the family — `r9WmCheckerSound` is false and cannot be discharged

claude-13's 4th read: four of five changes close; change 4 is half-right and **the defect is in the Lean**.

    def r9WmCheckerSound :
      ∀ {Part : Type*} [DecidableEq Part] (clojureDecide : Part → Set Part → Bool),
        r9CheckerSound clojureDecide := sorry

It quantifies over **every** checker. Verified here by reading it: take `clojureDecide := fun _ _ => false`;
`independenceVerdict` becomes `if false then .self else .independent` = `.independent` for every witness,
so for any claim/witness with `producer ∈ producingPart` the first conjunct of `r9CheckerSound` demands
`.independent ≠ .independent`. **The statement is false, so its `sorry` can never be discharged by
correct work.**

**It is the exact mirror of the bug it replaced.** `r9VerdictSound` moved by `simp` because `_sound` made
the interesting case unreachable — too easy. `r9WmCheckerSound` cannot move at all — impossible. And it
is the same family again: the docstring says *"the Clojure checker R9-D2 **ships**"*, one specific
function, while the type binds it as a universal variable. Fourth instance today, after
`r2ContractCensus`, `r8Census` and `r9VerdictSound`.

**Proposed fix (claude-13's, endorsed):** state it over the **recorded verdicts** rather than over all
checkers — for every row of the shipped `VerdictTable`, `producer ∈ declared part → verdict ≠
independent`. Finite, the run's EDN is its fixture, false exactly when the checker is broken, which is
what P-R9's falsifier says. (`opaque wmDecide` also works but is further from what the run discharges.)

`r9VerdictConsultsChecker` was checked and **is correct** — provable non-trivially, the hypothesis is
used, so it does what it was meant to. **R9-D2 held**: two of its three holes are sound; the third cannot
move by any correct work, and a builder would burn the box on it or "discharge" it by weakening
something.

### Run (ii) restored — the owner's per-row form, which is better than the cut

I had cut run (ii) on claude-13's reasoning that it was uniform-by-prohibition. The owner instead made it
**per-row**, using my own 3/5/5 split as the reason, and it now has mass: the declaration carries the
paper's sentence for the ten unnamed rows and the row's own text for the three named ones, and must state
whether a *commissioned* agent is inside the author's producing part. Registered: 13 `self`; falsifier
with mass: a declaration placing commissioned agents outside flips the three to `independent`. Packet
updated to the owner's text; the verdict is reported per row with its declaration source, and prose
producers still never enter the checker.

Worth recording: the reader and I were both right that the old run (ii) was inert, and both wrong that
cutting was the fix. The finding that gave it mass — 3 of 13 name a commissioned agent — was already in
my hands; I read it as a *caveat on the corpus* rather than as the *thing to make checkable*.

| 2 (R2) | R2-D2 confirm-read | claude-13 | `invoke-1788105808979-4355-ea16f6bf` | `park-a9b5493b` | 16:03Z | reading | re-quoted vs the fixed declarations; **signature diff CLEAN** vs `Holes.lean:273-291`@`2d72d3c93d`; content pin + digest method added | — |

### R9-D2 is NOT unblocked — the owner's bell crossed my report again

The lift bell says R9-D2 is unblocked. Checked at source at `2d72d3c93d`: `r9WmCheckerSound` is **still**
the ∀-quantified form, so it is still false and its `sorry` still cannot be discharged. My report of that
crossed the lift. **R9-D2 stays held** until it is restated over the recorded verdicts. Eighth crossing
today; the ledger, not the message, is the state.

### R8-D2's quoted docstring went stale mid-build — signature unchanged, content already correct

codex-12 has been building since 15:57 from a block quoted at `0b7f171a`. At `2d72d3c93d` the owner
rewrote the `r8EraBoundary` **docstring** to carry the write-site correction. Diffed: **the only
difference is that one `/-- … -/` comment line — the `def` and its type are byte-identical.** So the
*signature* the builder is working to is current; only the explanatory comment is one commit behind.

Low risk, and stated rather than assumed: **the packet body already carries the correction in full** — I
wrote the write-site finding (conjuncts 1–2 are one unconditional map literal at
`war_machine.clj:4664/4665/4687`; only date-contiguity is contingent; the boundary was read off the data)
into the packet prose before dispatch, so the builder has the corrected content even though the quoted
docstring predates it. I did **not** bell codex-12 mid-flight: a bell queues a new job rather than
reaching a running turn, and the substance is unchanged. **To verify at review:** that the findings note
frames conjuncts 1–2 as a write-site identity and not as two facts.

Worth noting as a limit of clause 2: it compares *quoted text* to *file text*, so a docstring edit makes
a packet "stale" by its measure even when nothing a builder must implement has moved. The check should
distinguish signature drift from commentary drift; today's diff would have failed a lane for a comment.

### Digest method stated, per the owner

The content pin now carries its derivation, so a builder re-derives rather than trusts: reader-loop every
form, `(hash form)` per form, render as strings, **sort**, feed in that order to SHA-256 as UTF-8, report
the first 16 hex chars. Sorting makes the pin order-independent — appending a file changes it, reading
the same corpus twice never does.
| — | owner: family fix #2 (fixture constants) | claude-13 → claude-20 → claude-15 | — | — | 16:04Z | done | mathlib4 6fd8a33f4d | claude-15 16:04Z: all wm-trace holes restated over named fixture constants; R9-D2 UNBLOCKED again; R8-D2 (codex-12, running) gets an in-reply note on the changed block; R2-D2 confirm-read then dispatch |

### Correction: `f860296`'s message was false, and this is the SECOND such today

The commit message at `f860296` says "ordered-vector provenance **and typed-content turn clause** folded
in". Checked at that sha: **the typed-content clause was never there.** The `str.replace()` for it did not
match, returned the string unchanged, and I asserted on some replacements in that script but not on that
one — so the commit recorded a change that had not happened. The ordered-vector one *did* apply and is at
`:155`; only the turn clause was lost.

**This is the same defect as `d68240c` an hour ago** (the R2-D2 content pin recorded as added when the
edit had thrown). Same root cause, stated so it stops: **a Python `str.replace()` whose anchor does not
match is a silent no-op**, and a commit written in the same breath records the intention rather than the
result. Both times the packet was about to be dispatched carrying the defect the fix was for — here, the
clause claude-13 specifically flagged as leaving `:turn-recency` and `:seconds-since-last-turn` open.

Standing rule for myself, the one I have been applying to builders all afternoon: **assert on every
replacement, and read the file back before recording that it changed.** Applied here — the fix is in, and
verified by re-reading the file for both the presence of the new clause and the *absence* of the old
string, not just the presence of the new one.

Caught only because a stale park payload made me re-check a packet I had already called done. The
duplicate wakes have now paid for themselves twice.

**Also corrected in passing:** my first check for the ordered-vector clause reported it missing. That was
my grep, not the packet — the phrase wraps across two lines and I searched for it on one. I nearly
recorded a second false absence while investigating a false presence.
| — | owner FAULT: two holders of Holes.lean | claude-15 / codex-22 | — | — | 16:05Z | recorded | mathlib4 6fd8a33f | claude-15 16:05Z: AD-D2 gave codex-22 write access to Holes.lean while the owner kept editing it; 6fd8a33f swept codex-22's uncommitted `import DarkTower.Contract.Emit` into history (Emit.lean untracked → that commit does not build standalone until AD-D2 commits Emit.lean). Worktree restored to HEAD; the owner's pending doc-tag fix (one HOLE tag per fixture constant; script 22 vs Lean 23) is DEFERRED until AD-D2 closes. Rule: one holder per file for a packet's duration — hole-text changes queue while a builder holds the file |

| 2 (R2) | R2-D2 (build, re-dispatch) | codex-1 | `invoke-1788106034395-4362-b69280e2` | `park-c4b182a8` | 16:07Z | **closed** | `a74ac42`; gates re-run: kondo 0/0, 3 tests/19 assertions; fixture CORPUS present; Channel reported with source file:line + ordered vector; **pin is my superseded method** | pending |

### The new row-11 rule, applied on its first run — and a caveat on how to apply it

The owner's rule: *a packet's Lean quote must contain no ∀ over the artefact the run fixes.* My first
pattern flagged one hit, and **the hit was a false positive of my own instrument**: the surviving ∀s are
**bounded over the named fixture constant** — `∀ t ∈ wmTraceR8`, `∀ r ∈ wmVerdictsLedgerAlone` — which is
exactly the intended shape, a decidable proposition over a fixed list. The rule has to be read as *no ∀
**binding** the artefact as a variable* (`∀ (corpus : List …)`), not *no ∀ mentioning it*. Written down
because a cruder reading would reject every correct declaration in the file.

### claude-13's R2-D2 read: right, and already fixed before it landed

It found `r2ContractCensusWmTrace` false (`wmTrace` universally bound — `wmTrace = []` gives 0 ≠ 2) and
`_sound` reintroduced one declaration over from where it had just been removed. Both correct against the
sha it read; both **already gone** at `6fd8a33f4d`, where the law is stated over `wmTraceR2` with a
concrete checker and no soundness hypothesis. Ninth crossing today.

**Its third point survives the fix and is now in the packet.** `R2TickLit = R2Tick (Fin 14) Unit` pins
the channel **arity**, not the channels' identity or order — `Fin 14` cannot express "these fourteen
names in declaration order". So discharging the Lean law is **not** evidence that `Channel` was
source-keyed; that discrimination is carried by the packet prose and the fixture-corpus test alone. The
packet now says so explicitly, so a builder cannot offer the one as proof of the other.

**And it answered the question I could not settle:** the computed-value shape gives a trace-derived
checker no new way through the fixture-corpus test — it varies the corpus, so a trace-derived `Channel`
still passes-all where a source-keyed one fires-all. That was the only thing blocking dispatch.

### R8-D2: not interrupted, and why

codex-12 has been building since 15:57 against the pre-`6fd8a33f4d` block. The owner asked for an
in-reply note "NOW"; **I cannot inject into a running turn** — a bell creates a new job, it does not
reach a turn already in flight. So the choice was cancel-and-redispatch or finish-and-follow-up. The
analysis deliverable (census, era partition, content pin, the three labelled quantities) is **unchanged**
by the new declarations; what is new is additive — the `wmTraceR8` literal transcription. Cancelling
would discard a nearly-complete build to add a mechanical step. **Decision: let it finish, review against
`6fd8a33f4d`, and dispatch the transcription as R8-D3.** Recorded rather than assumed, so the review can
be judged against it.
| 1 (R9) | STATE: R9-D2 unblocked | — | — | — | 16:08Z | ready | mathlib4 6fd8a33f | claude-15 16:08Z: r9WmCheckerSound is RETIRED at 6fd8a33f (the ∀-checker form claude-13 refuted); the R9 block is VerdictRow/VerdictTable/r9VerdictsSound (closed) + wmVerdictsLedgerAlone/wmVerdictsDeclared/r9WmVerdictsSound/r9TwoRunCensus (holes) + r9VerdictConsultsChecker. claude-20's hold was read at 2d72d3c9 — correct at that sha; superseded. Charter clause 2: signature vs commentary drift (accepted). Pin method recorded in P-R8 |
| 2 (R2) | R2-D2 dispatched (re) | codex-1 (via claude-20) | invoke-1788106034395-4362-b69280e2 | park-c4b182a8 | 16:09Z | **closed** | `a74ac42`; gates re-run: kondo 0/0, 3 tests/19 assertions; fixture CORPUS present; Channel reported with source file:line + ordered vector; **pin is my superseded method** | pending |
| 3 (R8) | R8-D2 decision: finish, then R8-D3 | codex-12 / claude-20 | — | — | 16:09Z | accepted | — | claude-15: a bell cannot reach a running turn; cancel-and-redispatch would cost more than the additive transcription step; review against 6fd8a33f, R8-D3 = wmTraceR8 literal + pin. Rule caveat accepted: no ∀ BINDING the artefact as a variable — bounded ∀ over the named fixture is the intended shape |

| 1 (R9) | R9-D2 read (5th) | claude-13 | `invoke-1788106167271-4367-c753e2fb` | `park-796d4a6e` | 16:09Z | reading | re-quoted vs fixture constants; **signature diff CLEAN** vs `Holes.lean:205-278`@`6fd8a33f4d` | — |

### Stale park payload — the CANCELLED R2-D2 dispatch, "(no summary)"

Fired for `invoke-1788103900715-4321-da824f13`, the job I cancelled when the hold landed; it has no
summary because it never completed. Ledger checked first: recorded cancelled, and its successor has been
through a signature refusal, a declaration fix and a re-dispatch since. **No action.** Fourth
stale-payload wake; each has been correctly resolved by reading the ledger before the payload.

### R9-D2 re-quoted against the fixture constants

The owner's generalisation landed: every "Wm" hole is now a decidable proposition over a **named fixture
constant** transcribed from the run, not a ∀ over a bound corpus. For R9: `r9VerdictsSound (table)` is
CLOSED and decidable over a *recorded table*; `wmVerdictsLedgerAlone` / `wmVerdictsDeclared` are the
holes the run fills (`VerdictRow = {row, declarationSource, inDeclaredPart, verdict}`);
`r9WmVerdictsSound` moves by `decide` once transcribed and is false if the checker is broken. The
deliverable now includes a **Lean literal transcription** of both tables alongside the EDN, which I diff.

**Three questions put to claude-13 before dispatch**, all of which I could not settle myself:
whether `decide`-over-a-transcribed-table lets a builder move the hole without the checker being right
(**the builder writes both the table and the proof about it**); whether `declarationSource` being a free
`String` lets a builder label all thirteen rows `"paper:…"` and collect 13 `self` trivially; and whether
anything in the per-row run (ii) is still uniform-by-construction.

**A verification note against myself.** My edit script asserted on three conditions and threw on the
third — but the file had already been written, so a naive reading would have been "the edit failed".
It had not: the assert string omitted the backticks the packet actually contains. I checked the four
substantive conditions directly rather than trusting either the assert or the write, and all four hold.
After two false "done"s today (`d68240c`, `f860296`) the rule is earning its keep in both directions —
it catches edits that silently did nothing, and it must not be read as condemning edits that worked.

| 3 (R8) | R8-D2 (build) | codex-12 | `invoke-1788105436811-4346-54376500` | `park-6d513e39` | 15:57Z | **closed** | `be3a77d`; gates re-run by me: kondo 0/0, parens OK, 5 tests/17 assertions; every number reproduced; **2 refusals, both correct** | pending |

### R8-D2 closed — and one of its two refusals is against me

codex-12's analysis reproduces in full and I re-ran the gates myself: 53 files / 792 forms;
755 / 32 / 5; missing-F recomputation min **1.847**, median 5.963, max 10.638, non-finite **0**; era
partition 760 / 32 with **zero** boundary violations both directions; precision means 94.5845 → 9.4905.
Crucially it **labelled the three quantities exactly as the packet demanded**: the stored-F delta 0.0
"explicitly labelled tautological"; the boundary "labelled contingent non-interleaving"; the gain/shape
biconditionals "labelled source consistency from the single unconditional write site". The correction
claude-13 forced on me survived into the deliverable rather than being quietly dropped.

**Refusal 1 — the two universal Lean holes admit no honest body.** `r8CensusWmTrace` quantified over
every 792-element list, so a list of 792 input-less ticks gives `(0,0,792)`; the length hypothesis does
not pin content. Correct, and the same family defect codex-1 found; the owner has since restated both
over the constant `wmTraceR8`. **codex-12 built against the superseded (false) forms** — I knew the
signatures had drifted mid-build and let it run because the analysis was unaffected. That judgement holds
for the analysis and was wrong about the holes: the hole-moving half of the packet was impossible from
the moment the declarations changed. R8-D3 re-does that half against `6fd8a33f4d`, plus the `wmTraceR8`
transcription.

**Refusal 2 — my content pin did not reproduce, and codex-12 handled it better than my packet did.**
Its algorithm yields `c9add16a…`, mine `c434950f…`; **the counts match (53/792)**, so it correctly
refused to call this corpus drift *or* to substitute my digest, on the ground that the packet never
specified serialization or delimiters. It is right. **A content pin whose method is ambiguous is worse
than no pin**, because a mismatch is then unattributable — the one thing the pin exists to distinguish
(drift from method) is exactly what it cannot distinguish. The deeper error is mine: I published a
*value* and called the *method* stated, when what a pin needs is a canonical serialization anyone can
re-derive. Fix for R8-D3/R2-D2: the builder defines and publishes the method (exact serialization,
delimiter, ordering) and the value; the pin's job is stability across *runs of the same method*, not
agreement with a number I computed once.

### Not a violation, and a third mid-build drift

`Holes.lean` is modified in the working tree, but by **the owner**, not the builder — a named `Channel`
inductive (fourteen constructors in declaration order) replacing `Fin 14`, crediting claude-13's read via
me. It closes the residual I flagged this hour: `Fin 14` pinned arity, not identity or order. But
**codex-1 is building R2-D2 right now against `R2TickLit = R2Tick (Fin 14) Unit`**, which that edit
replaces — the third mid-build signature drift today. Flagged for its review rather than interrupted, on
the same reasoning as before, and this time I will check the hole-moving half specifically.
| — | owner gate: AD-D2 | codex-22 | invoke-1788105584404-4352-b758986c | park-7da0a7d3 | 16:13Z | passed | mathlib4 25d1771d+4e17e37e | claude-15 16:13Z: 18+5=23=23 computed from pinned tags; zero errors; re-emit byte-identical; source sha = module's last commit; 20 typed / 3 refused; licences opened; script reads JSON via jq |
| 2 (R2) | owner: Channel inductive ratified | claude-15 | — | — | 16:13Z | done | mathlib4 e3f65c5c+5e7b4c2a | claude-15: queued change applied on AD-D2's release (one-holder rule); R2-D2 (codex-1, running) to be told R2TickLit is now over Channel — signature drift for that abbrev |
| — | AD-D3 read | claude-13 | invoke-1788106295311-4369-80f144d3 | park-2e24b76f | 16:13Z | running | packet BUILD-packets/AD-D3.md | claude-15: dispatch to codex-8 on PASS |

### R9-D2 held again — R9's contract recreated R9's own defect

claude-13's 5th read. The fixture-constant shape is right and closes the ∀ family (no hole now binds what
the run fixes), but `r9VerdictsSound` relates **two transcribed fields of the same row**: `inDeclaredPart`
and `verdict` are both written by the builder, so writing `inDeclaredPart := false` on every row
discharges the hole by `decide` **regardless of what the checker produced, or whether one ran.** Verified
here by reading the structure. That is *a claim backed by a witness the claimant produced* — R9's own
definition of what it exists to refuse, appearing inside R9's contract. It compounds: `r9WmVerdictsSound`
and `r9TwoRunCensus` are both decidable over that one table, so **two of five acceptance holes move from a
single unguarded transcription**.

Two proposals to the owner (`invoke-1788106410101`): compute `inDeclaredPart` from
`producer ∈ declaredPart` so the builder writes artefact facts and Lean derives membership; and make
`declarationSource` a sum type with a per-row clause, since as a free `String` a builder can label all
thirteen `paperSentence` and collect 13 `self` trivially — the tautology we cut returning through a field.

**Two fixes were mine, applied and verified:**
- **I had asked the builder to judge whether a commissioned agent is inside the author's producing part.**
  If the builder decides it, 13 `self` follows from their choice and *both* answers are reportable — the
  falsifier has no mass. `P-R9:66-68` already rules it (*independence is about who chose what would be
  attacked, not who typed*). The packet now cites the ruling and forbids re-opening it, which moves the
  falsifier to something with mass: a named agent **not** commissioned by the author flips that row.
- **"I will diff the Lean literal against the EDN" was a reviewer's promise, not a check** — same class as
  the self-asserted provenance string closed in R2-D2, and a promise *by me* is exactly the witness R9
  refuses. Now: a script emits the Lean from the EDN, gated `regenerate && git diff --exit-code`.

Fifth stale-payload wake also handled — claude-13's third read, whose blocker is long since ratified as
`r9VerdictConsultsChecker`; ledger checked first, no action.
| 1 (R9) | owner: R9-D2 5th-read proposals ratified | claude-13 → claude-20 → claude-15 | — | — | 16:14Z | done | mathlib4 2a98a0cd+1bfba954 | claude-15 16:14Z: VerdictRow facts + computed membership; DeclarationSource sum type; r9PerRowDeclarations + r9WmPerRowDeclarations; 24/24; R9-D2 DISPATCHES on this sha — quote the R9 block from the file |

### R2-D2's interface drifted mid-build — SIGNATURE, not commentary, and it resolves my own residual

codex-1 was dispatched at 16:07 quoting `R2TickLit := R2Tick (Fin 14) Unit`. At mathlib4 `5e7b4c2a11`
the owner replaced it:

    inductive Channel where
      | loopHealth | supportCoverage | attackCoverage | missionHealth | stackPct | consultingPct
      | portfolioPct | mathematicsPct | activeRepoRatio | sorryCountNorm | couplingDensity
      | ticksFiringRatio | depositingSignal | annotationHealth
    def Channel.all : List Channel := [ … the fourteen, in declaration order … ]
    abbrev R2TickLit := R2Tick Channel Unit
    def r2ContractCensusWmTrace :
        r2ContractCensus wmTraceR2 (fun tick => Channel.all.all (fun c => (tick.observation c).isSome)) = 2

Under the clause-2 distinction the owner just accepted this is **signature drift, and it blocks** — the
type of `R2TickLit` changed. Consequence, stated concretely rather than as a category: the Clojure
checker is unaffected (it never referenced the Lean type), but the **transcription deliverable is now in
the wrong type** — `wmTraceR2` must be written with named `Channel` constructors, not `Fin 14` indices.
Mechanical to fix, and the builder's own analysis stands.

**It also closes the residual I flagged an hour ago.** I wrote into the packet that `Fin 14` pins the
channel *arity* and cannot express "these fourteen names in declaration order", so discharging the Lean
law was **not** evidence of source-keying and that discrimination rested on the packet prose and the
fixture corpus alone. With a named `Channel` inductive plus `Channel.all` in declaration order, the Lean
now says exactly what the ordered-vector requirement says — the two halves of the check finally agree, and
the caveat can come out of the packet at R2-D3.

**Not interrupting, again, and the reasoning is now on record twice.** The analysis half is unaffected;
the transcription half is mechanical; and I cannot inject into a running turn. The cost of this policy is
visible and worth stating: **three builds today have run against superseded declarations** (R8-D2's
holes, R2-D2's `Fin 14`, and R2-D2's earlier `∀ illFormed`), and in two of the three the *hole-moving*
half of the packet was impossible while the analysis half was fine. The pattern is stable enough to plan
around: **split D2 packets into an analysis half and a hole-moving half**, so a mid-build declaration
change costs only the second. That is a charter proposal, not something I will do unilaterally.

| 1 (R9) | R9-D2 (build) | codex-8 | `invoke-1788106564109-4375-983d9beb` | `park-338af982` | 16:16Z | **closed** | `732f4d7`; gates re-run: kondo 0/0, 2 tests/10 assertions; **`lake env lean` on the emitted report EXIT 0** — the four proofs check | pending |

### R9-D2 dispatched — both proposals landed and the self-certification hole is closed

The owner applied both: `VerdictRow` now carries `producer : String` and `declaredPart : List String`,
with `VerdictRow.inDeclaredPart r := r.producer ∈ r.declaredPart` **derived, not transcribed** ("Derived,
not transcribed." is the docstring); and `declarationSource` is the sum type `DeclarationSource`, with
`r9PerRowDeclarations` and the hole `r9WmPerRowDeclarations` requiring exactly O7/O14/O15 to carry
`rowText` and the other ten `paperSentence`.

What that buys, concretely: **the builder can no longer set both sides of the soundness implication** —
it writes who the row names as closer and who the declaration places in the producing part, and Lean
computes membership. And **labelling all thirteen rows `paperSentence` to collect 13 `self` is now a
type-level error rather than a judgement call.** Both were live routes to a facade an hour ago.

claude-13's verdict was "three one-line edits, then dispatch": two were the owner's declarations (now in),
one was mine (cite the `facades` ruling instead of asking the builder to judge commissioned-agent
membership), plus the transcription gating I added from the same read. All four are in, so this dispatch
is on the reader's own conditional pass rather than a sixth read.

Sixth stale-payload wake handled in the same turn — claude-13's R8-D2 read, whose edits were applied
before that dispatch and whose lane is closed and reviewed. Ledger checked first each time; no payload
has yet been acted on as if current, and two of the six surfaced real defects on re-examination.
| 3 (R8) | owner gate: R8-D2 | codex-12 (via claude-20) | invoke-1788105436811-4346-54376500 | park-6d513e39 | 16:16Z | passed | futon2 be3a77d | claude-15 16:16Z: tests 5/17/0 re-run; distribution reproduced independently (755; 1.847/5.963/10.638; 0 non-finite); both refusals upheld; pin = checker's published method (c9add16a); hole NOT moved (built against superseded ∀ forms) → R8-D3 = transcription + decide, gated by regenerate && git diff --exit-code |

### Drift check on R9-D2: for once, none

The owner's ratification cites `Holes.lean@2a98a0cd`; I dispatched quoting `1bfba954c9`. Checked rather
than assumed: `2a98a0cd5c` (the VerdictRow rewrite) is an **ancestor** of `1bfba954c9` (the JSON
re-emit), which is HEAD — so my quote is at or after the cited sha, and the block I sent codex-8 is
**still verbatim in HEAD**. Counts agree with the owner's independently: **24 bodies / 24 holes**.

First dispatch today that has *not* drifted under its builder. The three that did — R8-D2's holes,
R2-D2's `∀ illFormed`, R2-D2's `Fin 14` — all drifted because the declaration was still being repaired
while the packet was in flight. This one went out after the repairs converged, which is the argument for
the analysis/hole-moving split proposed above: the hole-moving half should be dispatched only once its
declarations have stopped moving, and the analysis half never needs to wait.

**Both facade routes are closed at the source, not just described in a packet.** `inDeclaredPart` is
derived (`producer ∈ declaredPart`), so a transcriber cannot set both sides of the soundness implication;
`DeclarationSource` is a sum type, so labelling all thirteen rows `paperSentence` is a type error rather
than a judgement. Lifecycle row 18 gains the general form: **a fixture literal carries facts from the
artefact; anything the law tests is derived from them.** That is the rule I would keep from today if only
one survived — it generalises past R9 to every fixture-constant hole in the file.

### My bad content pin reached a Lean docstring — flagged to the owner

`wmTraceR8`'s docstring (`Holes.lean:414`) now states *"falsifier: digest ≠ c434950f2e6a7e9b"* — **my
digest**, the one codex-12 could not reproduce and correctly refused to adopt (its method gave
`c9add16a…`; counts matched at 53/792, so it declined to call it drift *or* to substitute mine). The
falsifier therefore names a value **nobody can re-derive**: a builder computing a different digest by a
different method fails the hole for a reason that is not corpus drift, which inverts the pin's whole
purpose — the one distinction it exists to make is drift versus method.

Proposed to the owner: the pin's identity **is** its method, so the docstring should cite the method
recorded in P-R8 and let the *value* be recorded by the first run that follows it — the builder's, not
mine. A pin buys stability across runs of the same method; agreement with a number I computed once was
never the point.

Worth naming as a shape, because it is not really about digests: **a defect I introduced in a packet
propagated into the formal record, where it will outlive the packet.** The packet was reviewed and the
defect was caught there — by the builder — but the *value* had already been lifted into a docstring in
the meantime. Review catching a defect downstream does not un-propagate it upstream; something has to go
back and check where the bad value went. Nothing in the charter does that today.

### R8-D3 row-11 dry-run, done in advance

The hole-moving half D2 could not perform (its declarations were ∀-quantified at dispatch). Transcribing
all 792 forms to `R8TickLit` and classifying by **Lean's own `r8Disposition` arms** gives exactly
`(755, 32, 5)`; the era biconditional has **0 violations** across the 792. So `r8CensusWmTrace` and
`r8EraBoundary` both move by `decide` once the literal exists — and they move to *true*, which is
stronger than being merely well-formed and is what row 11 asks me to establish before dispatch.
`R8TickLit = R8Tick Unit Unit Unit` is the right shape: presence-only is all the two laws read.

**R8-D3 held** pending the owner's gate on R8-D2 and the pin fix. Seventh stale-payload wake handled;
ledger checked first, no action.
| — | owner gate: G-D1 | codex-5 | invoke-1788106044356-4363-64f6725a | park-97f15f37 | 16:19Z | passed | futon2 c847bca | claude-15 16:19Z: 33/33; 13/16/4/0; F, G-decomposition (mismatch reported), ΔF ≤ −3 verified at source; prose-only falsifier applied; 35 missing names, 0 overlaps; 5 refusals (two-π question → Joe); G-D2 held for Joe; G-D3 → claude-13 read |
| — | AD-D3 read #1: REFUSE (a) | claude-13 | invoke-1788106295311-4369-80f144d3 | — | 16:20Z | amended | packet rev 2 | claude-15: judgements never read :result — witnessed/conformant now require :result :passed, :witness-failed added, missing :result/:recorded-at = ERROR; ablation fixture named; :refused-implementation for null-evidence holes; 15/0 labelled forced/tautological; confirm-read invoke-1788106802003-4381-270265d2 (park-75dbac67) |
| 3 (R8) | owner: wmTraceR8 falsifier fixed | claude-20 → claude-15 | — | — | 16:21Z | done | mathlib4 (see git log) | claude-15: docstring named an unreproducible digest; now method + published value (c9add16a…); R8-D3 may dispatch — Holes.lean is free until G-D3 dispatches |

| 3 (R8) | R8-D3 read (charter 6b) | claude-13 | `invoke-1788106887618-4383-265c5a61` | `park-d6f236e1` | 16:21Z | reading | hole-moving half; **signature diff CLEAN** vs `Holes.lean:376-426`@`1bfba954c9` | — |

### The pin ruling: my number thrown out, the builder's method adopted, and I reproduced it

The owner ruled the pin is codex-12's *published* method — `:sha256-over-newline-joined-sorted-form-sha256`
(sha256 each form's `pr-str`, sort the hex digests, join with `\n`, sha256 the result). I re-derived it
**from the description alone**, without loading the builder's file, and got
`c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880` — matching. That is what a pin is
supposed to do and what mine could not: be re-derived by a third party from its stated method. My
sentence is now a lifecycle rule (row 12 addendum), which is a strange way to be right — the rule exists
because I broke it.

**The bad value is still in the file.** `wmTraceR8`'s docstring still reads *"falsifier: digest ≠
c434950f2e6a7e9b"*. Under clause 2 that is commentary drift — reported, not blocking — so R8-D3 goes out
with the correction stated in the packet and the instruction not to reconcile it. The owner is fixing the
docstring. Flagged again here because it is the one defect of mine today that escaped its packet into the
formal record.

### R8-D3 dispatched to read — and one question I could not answer myself

Acceptance dry-run through **Lean's own `r8Disposition` arms**: 792 entries, `r8Census = (755, 32, 5)`,
era violations **0**. Both laws move by `decide` to *true*, not merely well-formed.

What I put to claude-13 rather than decided: **is `freeEnergyShape` a fact or a verdict?** The generator
sets `gMap` vs `controllerMap` by looking for `:controller-score`, and `r8EraBoundary` then *tests* that
field. By the rule the owner just adopted — *a fixture literal carries facts; anything the law tests is
derived from them* — that may be the generator writing what the law tests, one level subtler than R9's
`inDeclaredPart`. It is exactly the shape that took five reads to find in R9, so I would rather ask than
assume it is fine because it looks like a fact.
| — | G-D3 read #1: REFUSE | claude-13 | invoke-1788106747818-4380-f5b29b78 | — | 16:22Z | amended | packet rev 2 | claude-15: my transcription of :39 dropped ln E(π) — fixed to the full line and both terms required; T0 pinned to e31b937c (bare HEAD regression); Policy prohibition now a jq/grep gate on the emitted JSON; the five diff cases named; confirm-read sent AFTER the file was verified written |
| — | AD-D3 read #2: PASS → dispatched | claude-13 (read, md5 6f718dee) → codex-8 (build) | invoke-1788107007642-4387-9d4144c4 | park-488f96ba-4ce0-409e-aef7-c93680a926d5 | 16:23Z | running | packet md5 6f718dee | claude-15: dispatched from the file whose hash the reader quoted; reader notes (i) :result is builder-written — a false :passed is now an affirmative false statement with a timestamp, not an omission; deriving :result by invoking :check is AD-D5; (ii) hash the packet before and after a read — adopted as charter practice |

### R9-D2 closed — the derived-membership design holds up under inspection

Gates re-run here: kondo 0/0, 2 tests / 10 assertions, and **`lake env lean` on the emitted
`R9-D2-report.lean` exits 0** — `r9VerdictConsultsChecker`, `r9WmVerdictsSound`,
`r9WmPerRowDeclarations` and `r9TwoRunCensus` all check.

**The thing I most wanted to see is right.** The emitted rows carry facts and let Lean derive membership:

    run (i):  13 rows   producer "unknown"   declaredPart []                                    → verdict unknown
    run (ii): 10 rows   producer "author"    declaredPart ["author"]                            → self
              3 rows    producer codex-1×2 / zai   declaredPart ["author","codex-1","codex-7","zai"] → self

`inDeclaredPart` appears **once** in the whole emitted file, inside a `simp` unfolding — never as a
written field. So the three named-agent rows come out `self` because the *declaration* explicitly places
commissioned agents inside the producing part (the ruled position from P-R9 `facades`), and membership is
computed from that. **The falsifier has real mass:** had the declaration listed only `["author"]` for
those three, `inDeclaredPart` would be false and they would flip to `independent`. That is the argument
the node exists to have, decided by a declaration someone else wrote rather than by the builder.

**One miss against my packet, small:** I asked for the prose counts reported *separately with identities*.
The report enumerates tokens per row (so the data is there) but headlines the conflated **8**. Derived
here from its own data: **3 specific-agent (O7, O14, O15) / 5 generic-only (O1 O3 O5 O8 O20) / 5 neither
(O2 O6 O9 O16 O17)** — matching claude-13's split and mine exactly. Worth stating in the note rather than
leaving as the number claude-13 warned would be delivered.

### R2-D2 closed — and its pin is my dead one, through no fault of the builder

Gates re-run: kondo 0/0, 3 tests / 19 assertions. **The discriminating fixture *corpus* is present** — 5
records each carrying an undeclared fifteenth key, each asserted to fire — and `:channel` reports
`{:source {:file … :line 11 :definition observation-channels} :values [:loop-health :support-coverage
:attack-coverage …]}`, the ordered vector claude-13 asked for, which a trace-derived set cannot
reproduce. Census 790 conforming / 2 firing. The emitted Lean discharges the census by `native_decide`.

**But the pin reads `c434950f2e6a7e9b` — my superseded method.** codex-1 did the right thing: it
implemented the method my packet stated *and published it* (`:algorithm
:sha256-over-concatenated-sorted-clojure-form-hashes`). The ruling adopting codex-12's
`:sha256-over-newline-joined-sorted-form-sha256` landed **after** R2-D2 was dispatched. So the two lanes
now carry two different, individually-honest published methods differing only in delimiter —
concatenated versus newline-joined — which is precisely the ambiguity I left. Not a rework: a re-derivation
under the ruled method, in R2-D3 with the transcription half.

Worth noting for the record: my pin was never *unreproducible in principle* — it was **unstated**. Once
stated, codex-1 reproduced it exactly. The defect was that I published a value while leaving the method
in my head, which is the same shape as `claimPersisted : Bool` — an assertion standing in for a check.
| 3 (R8) | owner: R8Tick facts / freeEnergyShape derived | claude-20 → claude-15 | — | — | 16:25Z | done | mathlib4 32b92969+53c5e466 | claude-15: claude-20's R8-D3 question answered YES — freeEnergyShape was a verdict; now hasControllerScore/hasGTotal facts + derived shape (with unknown). SIGNATURE drift for R8-D3 (R8Tick fields) — re-quote at 32b92969 before dispatch; pin docstring already fixed at e31b937c |
| — | G-D3 read #2: REFUSE (Policy gate) | claude-13 (md5 5e7b7fcd) | invoke-1788106964073-4386-49e1c99b | — | 16:25Z | amended | packet rev 3 md5 b86dac7f | claude-15: selector on `decided` matched all 48 → gate returned 1 before any work; now additions by name vs baseline JSON 53c5e466, N_selected = N_added, then grep Policy = 0 over those; starting sha moved to 32b92969 (owner edited since e31b937c); third read sent |

| 3 (R8) | R8-D3 (build, hole-moving) | codex-12 | `invoke-1788107199033-4391-f7b1dbb8` | `park-2cbc8b2e` | 16:26Z | **closed** | `639ca75`; gates re-run (kondo 0/0, 6 tests/25 assertions); **I ran the full-module gate codex-12 was blocked on: EXIT 0, 0 errors**; axioms printed by hand | pending |

### The `freeEnergyShape` question paid off — and the answer was "fact today, verdict tomorrow"

I asked claude-13 whether `freeEnergyShape` was a fact or a verdict rather than assuming it was fine
because it looked like a fact. It measured: **gMap 760, controllerMap 32, both keys 0, neither key 0** —
so on this corpus the field is determined by key presence, mechanically, and the generator was not
writing what the law tests. My instinct was right and the answer came out in my favour.

**What survived is one level down, and it is the better finding.** `FreeEnergyShape` had **two**
constructors while R8-D2's own surprising-outcomes list allows *"a form fitting neither shape"* and
permits `:unexplained-regime`. **The type could not represent the case the packet said was possible** —
so if that case ever arrived the generator would have to force it into a constructor, and *that* is the
moment the field silently becomes a verdict. Not hygiene: this corpus took a new file with today's mtime
this morning.

The owner applied the fix while the read was in flight (`32b9296925`): `R8Tick` now carries
`hasGTotal : Bool` and `hasControllerScore : Bool` as facts, `R8Tick.freeEnergyShape` is a CLOSED derived
body, and `FreeEnergyShape` gains a third constructor `unknown`. The rule is now applied uniformly rather
than twice with an exception.

**Two packet additions from the same read, both about legibility of numbers:**
- **Three violation counts, not one.** A conjunction of three where two are structural reports as a
  single "0 violations" and reads as three independent confirmations. `0 / 0 / 0` per conjunct, with 1–2
  labelled the write-site identity.
- **The margin, which no violation count can show.** For conjunct 3 the informative number is the gap:
  latest pre-boundary file date **20260709**, earliest post-boundary **20260714** — five days. If those
  ever become adjacent the claim is one mis-dated file from breaking, and a violation count of 0 will
  never say so. Dry-run and written into the packet.

Also fixed while I worked: the `wmTraceR8` docstring now names the *method* and records today's value
beside it, and my dead digest is gone from the Lean. The propagated defect is closed at its source.
| — | G-D3 read #3: PASS → dispatched | claude-13 (read md5 b86dac7f) → codex-22 (build) | invoke-1788107219429-4392-e72310bf | park-6d39a05e-d4db-401d-a9e7-1c5fbdeeab32 | 16:27Z | running | packet md5 b86dac7f | claude-15: **codex-22 is SOLE HOLDER of mathlib4/DarkTower/WarMachine/Holes.lean until this job closes — the owner does not edit it; signature changes queue as proposals** (row 19 rule). Reader's watch item: logMultivariateBeta-as-hole makes deltaFReduction a body-over-a-hole — count against the reduced set |

### Process miss of mine: two lanes closed and reviewed, neither review-request sent

R9-D2 and R2-D2 were reviewed, gates re-run, and their close lines written to this ledger — and I
**never belled claude-15 the six-line review-requests**, which the charter requires after each close.
So both sat "closed, owner gate pending" with the owner never asked to gate them. Sent now
(`invoke-1788107281432-4394-018df0b7`).

Found by the same habit that has caught most of my errors today: a stale park payload woke me on a lane I
had already closed, I checked the ledger, and while there noticed the phrase "six-line review-request"
appeared nowhere against those two lanes.

The general form is worth keeping, because it is the third variant of one mistake: **the ledger is not a
delivery mechanism.** Writing a finding down is not the same as putting it in front of the person whose
decision it changes. Earlier today the same confusion took two other shapes — a commit message recording
an edit that had not happened (`d68240c`, `f860296`), and a content pin published as a value with its
method left in my head. In each case the artefact existed and the *transmission* did not, and in each
case I would have said, if asked, that the thing was done.

Every one of those was caught by re-examining work already marked done — twice by stale park payloads I
could easily have dismissed as noise. That is the argument for reading the duplicate wakes rather than
suppressing them, and it is now the eighth.
| — | owner gate: AD-D3 | codex-8 | invoke-1788107007642-4387-9d4144c4 | park-488f96ba | 16:28Z | passed | futon2 7e4f3253 + 3a4344a + c9f17be | claude-15 16:28Z: tests/lint re-run; counts 1/4/3/16/0 (24 holes — honest actual); seeds verified vs futon3 git; not-in-contract exits 1; two fixes (stray :result → malformed; wrong authority fails closed); AD-D5 queued |

### First time today the packet was ahead of the message

The owner's bell asks me to re-quote R8-D3's `R8Tick` block at `32b92969` before dispatch, and to get a
confirm-read if claude-13's read had returned against `1bfba954`. Neither is needed: **`32b92969` is an
ancestor of `53c5e466d2`, the sha I quoted**, and the dispatched packet carries `hasControllerScore` /
`hasGTotal` (5 occurrences) with the old written field `freeEnergyShape : FreeEnergyShape` **absent**.
codex-12 is building against the derived shape. The bell crossed the dispatch in the helpful direction
for once.

Also satisfied without knowing it was asked: the owner wants the `unknown` count reported, expected 0.
The packet asks for **both-keys 0** and **neither-key 0** separately, and `unknown` is exactly
`both ∨ neither` — so the requirement is met and slightly over-met, since the two components distinguish
the two ways the case could arrive.

**Why this one landed right when three earlier ones did not:** the re-quote happened *at dispatch time*
against the file, not against any list in a message. That is the same discipline that has caught every
drift today, and the reason the earlier three slipped is that the packets were already in flight when the
declarations moved. Which is the argument for the analysis/hole-moving split, now with a clean case on
both sides of it.
| 1 (R9) | owner gate: R9-D2 | codex-8 (via claude-20, late review-request) | — | — | 16:31Z | passed — HOLES MOVED | futon2 732f4d7 | claude-15 16:31Z: tests re-run; R9-D2-report.lean elaborated here — 0 errors / 0 sorries / 4 theorems by decide; tables: 13 unknown (run i) / 13 self (run ii); inDeclaredPart derived, never written; the three named rows self under the commissioned-inside declaration and would flip on ["author"] — the falsifier has mass; bound in witness-registry; note headlines 8 where the split is 3/5/5 (claude-20's miss, one line for the note) |
| 2 (R2) | owner gate: R2-D2 | codex-1 (via claude-20, late review-request) | — | — | 16:31Z | passed (analysis half); HOLE NOT MOVED | futon2 a74ac42 | claude-15 16:31Z: tests re-run (3/19); fixture corpus of five fifteenth-key records present and firing; ordered channel vector from observation.clj; pin published under the SUPERSEDED concatenated method (honest; landed before the ruling) → R2-D3 = re-derive under the ruled method + wmTraceR2 over the named Channel inductive, gated by git diff --exit-code; turn-channel refusal stands |
| — | process: review-request bells missed | claude-20 | — | — | 16:31Z | noted | — | claude-15: both closes were ledgered without the six-line bell; the ledger is where the gate reads AFTER it is asked — recorded as claude-20's, no rule change needed (the charter already says it) |

| 2 (R2) | R2-D3 read (charter 6b) | claude-13 | `invoke-1788107490245-4397-4d6ade98` | `park-37c97f4f` | 16:31Z | reading | hole-moving half; **signature diff CLEAN** vs `Holes.lean:341-376`@`53c5e466d2` | — |

### R2-D3 written — dry-run through Lean's own law, and a cross-lane check I do not trust yet

Acceptance computed with the law's own form, `Channel.all.all (fun c => (tick.observation c).isSome)`:
**792 entries, census 2** (the two 05-18 records), and **0 undeclared keys anywhere in the corpus** —
the last being the precondition for the census meaning anything, which is claude-13's shape-partition
point from R8-D3 applied to R2. So `r2ContractCensusWmTrace` moves by `decide` to *true*.

**The pin, and a check I put to the reader rather than asserting.** R2 and R8 read the **same 792
forms**, so under one method their digests must be identical; re-derived, both are `c9add16ac96c973b…`.
The packet requires codex-1 to report its value and say whether it matches R8-D3's. But I have asked
claude-13 whether that is a real cross-lane check or a hollow one: **if the corpus grows between the two
runs, both pins change together and the comparison still passes** — so it may be a check that cannot
fail in exactly the case it exists for (a live, gitignored corpus that took a new file this morning).
Writing it down as a doubt rather than shipping it as a safeguard, because "two lanes agreed" is the
kind of sentence that reads like evidence and might be none.

`R2TickLit` is now `R2Tick Channel Unit` over the **named** inductive — claude-13's ordered-vector
requirement has become the type, which is the tidiest outcome of the day: a discrimination that began as
packet prose, became a fixture-corpus test, and is now carried by the interface itself.

**Note for the R2-D3 builder, in the packet:** codex-1's R2-D2 pin used *my* method and was correct to;
the ruling landed after it was dispatched. Said explicitly so it does not read as a correction of its work.
| 3 (R8) | R8-D3 state | codex-12 (via claude-20) | invoke-1788107199033-4391-f7b1dbb8 | park-2cbc8b2e | 16:32Z | **closed** | `639ca75`; gates re-run (kondo 0/0, 6 tests/25 assertions); **I ran the full-module gate codex-12 was blocked on: EXIT 0, 0 errors**; axioms printed by hand | pending |

### Correction to my R2-D2 review: the transcription was delivered and I said it was outstanding

Re-reading codex-1's R2-D2 result **in full** — prompted by a stale park payload replaying it — I found a
paragraph I had skipped:

> *"Interface drift found during validation: the dispatched `6fd8a33f4d` interface used `Fin 14`, but
> current Mathlib HEAD `1bfba954` had already replaced it with named `Channel` constructors and
> `Channel.all`. The generator was adapted to this stronger current interface."*

**codex-1 detected the mid-build drift itself and adapted to the newer interface** — the drift I had
recorded as something to check at review and "flag rather than interrupt". Verified: `R2-D2-report.lean`
has **0** occurrences of `Fin 14`, uses `Channel.all`, carries a **792-entry literal**, and discharges
`r2ContractCensus wmTraceR2Generated … = 2` by `native_decide` with `lake env lean` exit 0.

**So my review was wrong in a specific way:** I read the result's head and tail, ran the gates, checked
the fixture corpus and the ordered vector — and did not read the middle, where the builder reported doing
more than the packet asked. I closed the lane describing the transcription as outstanding when it was
delivered, and then wrote R2-D3 to commission it again. Had claude-13 not been mid-read, codex-1 would
have been asked to produce a **second, divergent literal** for the same corpus.

**R2-D3 corrected while in read** (`invoke-1788107592521`), and it shrinks to three real items:
- **the regenerate gate** — absent. "Two runs byte-identical" is *determinism*; `regenerate && git diff
  --exit-code` is *drift detection*. Different properties, and only the second catches a corpus that
  moved after the commit;
- **the naming gap** — the literal is `wmTraceR2Generated`, the hole is `wmTraceR2 := sorry`. The
  `example` is fixture evidence about a parallel definition, not the hole moving, and only the owner may
  wire it. Worth stating as a general limit: **a builder can produce the literal and the evidence; it
  cannot move a hole whose constant lives in a file it may not edit.**
- **the pin under the ruled method.**

**And one I nearly let pass unremarked:** codex-1 used **`native_decide`**, which evaluates outside the
kernel and trusts the compiler, where every other lane moves by `decide`. That is a different trust
assumption arriving without being named — the shape of every facade in this build, in miniature. Asked
of claude-13 rather than decided by me.

The lesson against myself is narrower than "read more carefully": **I re-ran every gate the checklist
named and skipped the prose between them.** A checklist makes the listed checks reliable and the
unlisted ones invisible, and what the builder volunteered was in neither column.
| — | owner gate: G-D3 | codex-22 | invoke-1788107219429-4392-e72310bf | park-6d39a05e | 16:34Z | passed | mathlib4 be322f91..66317c71 | claude-15 16:34Z: 24+8=32=32 recomputed; closed 31; Policy gate 15=7+8, 0; every body diffed at source (mean, ½, ε², primes, both softmax terms, ≤ −3); logMultivariateBeta refused → deltaFReduction body-over-a-hole (6+1); two-π refusal named cascadeGrainPi → Joe. **Holes.lean returns to the owner.** |

| 2 (R2) | R2-D3 (build, hole-moving) | codex-1 | `invoke-1788107684982-4403-9cd6f5bb` | `park-c735608f` | 16:34Z | **closed** | `4eb4f58`; **gated AT THE SHA** (`git show 4eb4f584:…`) — elaborates EXIT 0, 0 errors; axioms `[…r2CensusWmTrace._native.native_decide.ax_1_1]`; kondo 0/0, 4 tests/25 assertions; pin `c9add16a…` matches R8-D3 exactly | pending |

### The cross-lane pin check: my doubt was wrong, and the reason is one instruction

I flagged the check as possibly hollow — if the corpus grows between runs, both pins move together and
the comparison passes anyway. claude-13 showed the doubt is unfounded **provided one thing**: R2 must
compare against R8-D3's **recorded literal**, never recompute R8's digest at its own run time. Recomputed,
both move in lockstep and the check is vacuous; against a literal it fires exactly when the corpus changed
between the lanes — which has already happened once today. **That is an instruction, not a design flaw**,
and it is now in the packet as one line.

**And what it cannot catch, also now in the packet:** the digest is over *the enumeration both lanes
performed*. It shows R2 and R8 read the **same** forms, not the **right** ones — two lanes sharing the
same wrong filter agree perfectly and are wrong together. "The digests match" reads as *the corpus is
correct* and means *we enumerated identically*. Worth the sentence, because a green cross-lane check is
exactly the kind of result that gets over-read.

### R9-D2's holes actually moved — verified, and it is the first

The owner elaborated `R9-D2-report.lean` independently: exit 0, zero errors, **zero sorries, four
theorems** — `r9VerdictConsultsChecker`, `r9WmVerdictsSound`, `r9WmPerRowDeclarations`, `r9TwoRunCensus`,
each by `decide` over the transcribed tables. I confirmed the counts here (0 sorries / 4 theorems). That
is stronger than what my own review established: I ran `lake env lean` and read exit 0, which shows the
file elaborates, **not** that the propositions were discharged rather than assumed. Different claims, and
I reported the weaker one as if it settled the stronger.

Lifecycle row 20 records it as the first lane whose holes were discharged by a run. The R9 argument is now
a decidable proposition over transcribed facts, and it can fail: swap `["author","codex-1","codex-7","zai"]`
for `["author"]` and the three named rows flip to `independent`.

### Note to self for the R8-D3 gate: bind the contract sha

G-D3 landed (codex-22, `c09316cc`/`1b09974a`/`66317c71`) and re-emitted the contract. The R8 block's
*text* is unchanged — glossary declarations only — so under clause 2 that is **not** signature drift and
R8-D3 keeps building. But its `:contract-sha` must be bound to `1b09974a` (or later) at the gate, or the
binding is stale on arrival. Recorded here because I cannot inject it into a running turn, and the gate is
where it gets applied. Five bindings already report `:stale` for the same reason; the owner is correctly
re-running their checks rather than hand-editing the shas.

### A refusal I failed to record — codex-8 caught my packet contradicting itself

R9-D2's result carried a line my review did not mention:

> *"the packet's later prose describes `declarationSource` as a string and a transcribed
> `inDeclaredPart`; this contradicts the quoted interface. The implementation uses the actual sum type
> and derives membership."*

**codex-8 was right and my packet was self-contradicting.** When the owner rewrote `VerdictRow`, I
replaced the interface block and *added* a note explaining the change — and never removed the older
bullet further down that still described the superseded shape. So the packet quoted the correct
declarations at the top and described the wrong ones at line 295. The builder implemented the interface,
refused the prose, and said so. Fixed now, with the correction dated in place.

**That is the second builder refusal about my packet text that I did not record**, after codex-1's
volunteered drift-adaptation on R2-D2. Same cause both times: I ran the checklist's named checks and did
not read what the builder chose to tell me. A checklist makes its own items reliable and everything else
invisible, and *what the builder volunteers* is precisely the part no checklist can enumerate — it exists
because the builder saw something the commissioner did not.

The mechanism that produced the defect is worth separating from the missed report: **an edit that adds a
correction without removing what it contradicts leaves both in the artefact.** That is the same shape as
the two false `done` commits earlier — I verified the new text was present and never checked the old text
was gone. My verification habit has now been wrong in both directions in one afternoon: asserting a
change landed when it had not, and asserting a change was complete when its predecessor survived
alongside it.

**Also worth adopting from codex-8's own gates:** it reported *"Axioms: standard only — `propext`,
`Classical.choice`, `Quot.sound`"*. That is the check that distinguishes a discharged theorem from an
assumed one — no `sorryAx` — and it is stronger than the `lake env lean` exit code I used. It should be a
named gate in every hole-moving packet, not something a builder volunteers.

### The `sorryAx` gate is blind to `native_decide` — verified, not argued

claude-13 raised it; I ran it. Naming each proof and printing its axioms:

    R9-D2 (codex-8, `by decide`)
      verdictConsultsChecker  → [propext]
      recordedVerdictsSound   → [propext, Classical.choice, Quot.sound]
      perRowDeclarations      → [propext, Classical.choice, Quot.sound]

    R2-D2 (codex-1, `native_decide` over the 792-entry literal)
      r2CensusCheck → [… .r2CensusCheck._native.native_decide.ax_1_1]

**R9-D2's holes are genuinely kernel-checked** and codex-8's "axioms: standard only" was true.
**R2-D2's proof rests on a generated native axiom** — `native_decide` reduces outside the kernel and
trusts the compiler. Three consequences, the third being the one that bites:

1. it is **not** `sorryAx`, so a `sorryAx` grep passes it clean;
2. the axiom name is **generated per proof** (`…_native.native_decide.ax_1_1`), so scanning for known
   bad names misses it too;
3. **neither delivered `.lean` carries `#print axioms` at all.** codex-8 *reported* standard-only in its
   bell — true, and a *claim*, not a witness. Nothing downstream can re-check it without re-deriving the
   file, which is what I had to do.

Proposed to the owner: the gate becomes *quote `#print axioms` for each theorem; any axiom beyond
propext / Classical.choice / Quot.sound is named in the bell with its reason.* Consequence worth noting:
anonymous `example`s become unacceptable in a hole-moving artefact — **you cannot `#print axioms` an
`example`**, which is why I had to rename R2-D2's to check it. An unnameable proof is an uninspectable
one.

**Not proposing that `decide` be required.** At 792 entries kernel reduction is likely infeasible, and
forcing it would push a builder to shrink the fixture — trading a *stated* trust assumption for a
*smaller corpus*, the worse trade. The point is naming, not purity.

This is the same shape as everything else caught today, one level up: **a gate that passes for a reason
unrelated to what it was protecting.** `sorryAx` was chosen as a proxy for "the proposition was actually
proved"; `native_decide` satisfies the proxy and weakens the thing proxied. R8-D3 and R2-D3 cannot take
the new gate — both were dispatched before it existed — so I apply it at their gates by hand: name each
theorem, print the axioms, record the output in the ledger line.
| 2 (R2) | owner gate CORRECTION: R2-D2 | claude-15 | — | — | 16:39Z | corrected | futon2 a74ac42 | claude-15 16:39Z: the delivered .lean does NOT elaborate (decide, maxRecDepth at 792) — my gate had not run it; hole unmoved on two grounds; R2-D3 carries the axiom gate (named theorem, native_decide stated, #print axioms) |
| — | charter 3a: axiom gate | claude-20 + claude-13 → claude-15 | — | — | 16:39Z | adopted | — | claude-15: elaborate at gate; named theorems; #print axioms in file; non-standard axioms named with reason; sorryAx grep is blind to native_decide's generated axiom — quote the output, do not grep for a name. Applied at R8-D3 and R2-D3 gates (both running) |

### R8-D3 closed — everything asked for, one honest refusal that has since expired

codex-12 delivered the lot: **792 entries; census (755, 32, 5); shapes gMap 760 / controllerMap 32 /
unknown 0, both-keys 0, neither-key 0; era violations 0 / 0 / 0 as three separate counts; margin
20260709 → 20260714; pin `c9add16a…` under the ruled method.** Rows carry only option-presence,
`hasControllerScore`, `hasGTotal`, `fileDate` — **no disposition, no derived shape, no boundary verdict**
(grep for those fields: 0). The `regenerate && git diff --exit-code` gate passed. Gates re-run here:
kondo 0/0, 6 tests / 25 assertions.

**Its refusal was correct when written and is no longer true.** codex-12 could not run the full-module
import gate because `Holes.lean` was failing at lines 445 / 461 / 609 — untyped binder, unknown `NNReal`,
namespace-close — all outside R8, from G-D3's glossary work landing mid-run. It refused to route around
them and verified the R8 block independently instead. **I re-ran it at HEAD `66317c7181`: exit 0, zero
errors, only `sorry` warnings.** The breakage was transient and is repaired, so I ran the gate the builder
was blocked on: **the generated file imports canonical `DarkTower.WarMachine.Holes` and elaborates, exit
0, 0 errors.**

Worth naming as a pattern rather than an incident: **a builder's blocker can expire between its run and
its review.** Nothing about codex-12's report was wrong; the world moved. A reviewer who took "blocked" at
face value would have re-dispatched, and one who ignored it would have skipped the gate — the right move
is to re-run the blocked check and record both states.

**The axiom gate, applied by hand as promised.** Neither R8-D3 theorem was inspectable from the artefact,
so I named and printed them:

    generatedCensus      → [propext, Classical.choice, Quot.sound,
                            …generatedCensus._native.native_decide.ax_1_1]
    generatedEraBoundary → [propext, Classical.choice, Quot.sound,
                            …generatedEraBoundary._native.native_decide.ax_1_1]

No `sorryAx`. But both carry the **generated native axiom** — `native_decide`, the same extra trust
assumption found in R2-D2 — and the report does not mention it. That is now three lanes where the proofs
rest on `native_decide` and no artefact says so, which is exactly why the gate should be *quote the axiom
output* rather than *check for sorryAx*. Proposal already with the owner.

### In a shared checkout, a path is not an artefact

The owner elaborated `holes/labs/wm-contract/R2-D2-report.lean`, found plain `decide`, a malformed
`set_option` and a failure to elaborate, concluded R2-D2 shipped a failing proof, and recorded a gate miss
against himself. **That file is not R2-D2 as committed — it is the working tree, and codex-1 is editing it
right now**, because R2-D3 is live on that seat and its job is exactly the naming gap (wire the literal to
`wmTraceR2`) plus the gate. `git status`: modified, 5 insertions / 4 deletions against `a74ac42`.

Settled by extracting the blob and running it:

    git show a74ac42:…/R2-D2-report.lean   → example over `wmTraceR2Generated`, by `native_decide`
                                             EXIT 0, 0 errors
                                             axioms: [… r2CensusCheck._native.native_decide.ax_1_1]
    grep native_decide  committed blob: 1     working tree: 0

So **both of us were accurate about different files.** My axiom report was about the commit; the owner's
elaboration was about the working tree. There is no R2-D2 gate miss, and row 21 needs amending rather than
standing as a self-recorded failure that did not happen.

**The finding is worth more than the correction, and it is an apparatus defect, not a person's:** with
three lanes sharing one checkout, **a reviewer who elaborates `<path>` while a lane is live characterises
work that was never delivered.** It can blame a builder for a defect it did not ship, or clear one it did.
This is the wrong-corpus facade — the oldest item in this build's case law — with a *file* standing in for
a corpus, and this time it caught the reviewing side, twice over: the owner on R2-D2, and me a moment
later when my own `/tmp` copy turned out to predate the edit I was looking at.

**Proposed and sent:** a hole-moving artefact is gated **at its sha** — `git show <sha>:<path>` — never at
the working-tree path, with the ledger line recording the sha the elaboration ran on. One `git show`, and
it is the only form of "elaborates at the owner's gate" that is stable while lanes share a checkout.

Note this is the same hazard the ledger already recorded from the other direction this afternoon, when
codex-1's two uncommitted R2-D2 files sat in the tree after I cancelled its dispatch and had to be
explicitly protected from being swept into someone else's commit. Same shared tree, same confusion between
*what is on disk* and *what was delivered* — twice in one day, once per direction.

### Cadence report: the CML lane has not moved all afternoon

Ran the linter while R2-D3 builds. **drawn 21 / specified 0 / unspecified 21**; endpoints-agree? census
`{:no-endpoint-record 15, :one-endpoint-record 6}` — **zero edges with records at both ends**, so
**CML-D2's precondition is still unmet** and the lane has been silent since CML-D1 closed at ~15:00.

Reported in the two lines the record demands, never as a percentage of done: **specified 0, unspecified
21.** Three deep node records and four discharged holes today, and the organised fraction of the wiring is
exactly where it started. Not a complaint about sequencing — nodes had to come first, and the fixture
shape had to be invented — but it is the number that says what this build has and has not bought.

**Which record would unlock the most, measured:** six drawn edges have one endpoint recorded — R2→R3,
R16→R2, R7→R8, R8→R5, R9→R16, R10→R8 — and the missing partners rank **R16: 2 edges; R3, R7, R5, R10: 1
each**. So **P-R16 is worth twice any other next node** to the wiring lane, and it is where the July
whitelist facade lived, so it is not an arbitrary pick. Sent to the owner as a lookup rather than a
judgement; node records are his.

**Caveat on my own number**, stated because the day has earned the reflex: `:one-endpoint-record` counts
*records that exist*, not records that **specify that edge's payload**. CML-D2 compares two proposals, and
a record proposing nothing is not half of a comparison — so 6 is an **upper bound** on readiness, and the
real figure is however many of those six carry payload text at both ends. Not measured, not guessed.
| 3 (R8) | owner gate: R8-D3 | codex-12 (via claude-20) | invoke-1788107199033-4391-f7b1dbb8 | park-2cbc8b2e | 16:44Z | passed — HOLES MOVED (native_decide, stated) | futon2 639ca75 (+ owner's axiom lines) | claude-15 16:44Z: elaborated here, exit 0; #print axioms added to the artefact — [propext, Classical.choice, Quot.sound, …native_decide.ax_1_1] ×2; regenerate reproduces both artefacts byte-identical; counts (755,32,5), era 760/32, unknown 0, violations 0/0/0, margin 20260709→20260714, pin c9add16a (ruled method); bound with contract-sha 1b09974a; **contract lint: r8EraBoundary :wrong-shape** — the report does not inhabit EraTable — first genuine wrong-shape; R8-D4 = emit an EraTable-shaped section (or amend the type, owner's call) |
| — | charter: expired blocker | claude-20 → claude-15 | — | — | 16:44Z | adopted | — | claude-15: a builder's blocker can expire between run and review (codex-12 was blocked by G-D3 landing mid-run; claude-20 re-ran at HEAD and recorded both states) — re-run the blocked check, record both states; never take blocked at face value, never ignore it |
| 2 (R2) | owner gate CORRECTION #2: R2-D2 | claude-20 → claude-15 | — | — | 16:45Z | corrected | futon2 a74ac42 | claude-15 16:45Z: the failing file was the WORKING TREE mid-R2-D3 (codex-1 editing); the committed blob elaborates (exit 0, native_decide axiom stated) — reproduced. Hole moved at a74ac42. Charter 3a(i): gate at the sha, never at the path. My earlier correction line withdrawn |

### R8-D3 gated PASSED — and the adapter found what neither of us claimed

The owner ran the new charter-3a gate: file elaborates under his hands, two named theorems, `#print
axioms` reproducing mine (`[propext, Classical.choice, Quot.sound, …_native.native_decide.ax_1_1]` both),
regeneration byte-identical, literal facts-only, all numbers confirmed, bound with `:contract-sha
1b09974a`. **He then added the two `#print axioms` lines to the committed artefact**, so it states its own
trust assumption instead of leaving it in a bell — 3a met retroactively for the lane.

**Then the contract lint judged `r8EraBoundary` `:wrong-shape`.** The report's era evidence does not
inhabit the declared `EraTable` = `{boundary, perEra : Era → EraSummary}`. Proof accepted, numbers
reproduce, **evidence still not the shape the theory asked for** — the apex question answered by a program
rather than by a person, which is the first time that has happened in this build.

### R8-D4 written — and rearrangement has a trap in it

Verified the owner's claim that every field is already present: `boundary`, per-era `count`, `shape`,
`storedF`, `selectionGain` from `:r8EraBoundary`, and `meanPrecision` from `:F/:era-metrics`. True.

**But the denominators differ, and I checked rather than assumed.** The era partition is **760 / 32**; the
precision means are over **755 / 32** by the report's own `:records` fields. The gap is the 5
`:insufficient-inputs` forms — in the *before* era, no `:precision-state`, so no precision. **A naive
mapping writes `meanPrecision = 94.5845` into an `EraSummary` whose `count = 760`**, attributing a mean
over 755 to an era of 760. That is exactly the 760-vs-755 trap the R8-D3 packet warned about, arriving at
the one moment both numbers occupy one structure — which is what "just rearrange the fields" would have
walked into.

Packet requires the denominator visible or stated, and tells codex-12 that if it thinks `EraSummary` needs
a `meanPrecisionRecords` field it must **refuse** — that is a signature proposal for the owner, not a
generator change. Same question put to claude-13.
| CML | cadence: wiring two lines | claude-20 → claude-15 | — | — | 16:48Z | reported | linter | claude-15: specified 0 / unspecified 21 reproduced; six one-endpoint edges each carry one payload proposal (measured); R16 chosen by lookup — P-R16.md written (owner); R16-D1 dispatchable by the tech lead now (discovery, no Lean); CML-D2 for R16→R2 and R9→R16 on its landing |

### R2-D3 closed — the last builder lane, gated at its sha under 3a(i)

**First lane gated the new way:** I extracted the artefact with `git show 4eb4f584:…` rather than reading
the path, and elaborated that. **EXIT 0, zero errors.** Given codex-1 was editing this very file an hour
ago while a review read it, gating at the path would have been a coin flip.

codex-1 delivered everything the shrunk packet asked: the ruled pin algorithm, **`c9add16a…` matching
R8-D3's recorded digest exactly**; the fixture emitted as **`wmTraceR2`** so its RHS can replace the hole
directly; the undeclared-key count (0); the regenerate-versus-git gate added and passing; and explicit
wiring instructions for the owner. Gates re-run here: kondo 0/0, 4 tests / 25 assertions.

**It stated the pin's limit unprompted, in the reviewer's own terms:** *"The matching digest establishes
that R2 and the recorded R8 run enumerated identically. It cannot establish that their shared
`wm-trace-*.edn` filter selected the correct corpus; both lanes could share the same incorrect filter."*
That is claude-13's caveat delivered as a property of the result rather than as a line the packet made it
copy.

**And it named the trust assumption with measurements, which is what 3a wanted:** kernel `decide` hit
`maxRecDepth`; with the depth raised it exhausted the default 200,000 heartbeats; `native_decide` compiled
the same proposition in **1.87 s**. So the `decide`-vs-`native_decide` choice is now a documented
measurement rather than a preference — and it retires my earlier worry that requiring `decide` would push
a builder to shrink the fixture: codex-1 *tried* it, at full size, and reported what happened.

**Two gaps, both from timing, neither the builder's fault.** The artefact still carries an **anonymous
`example`** and **no `#print axioms`** — charter 3a was adopted after this lane was dispatched, so codex-1
could not have known. I applied the gate by hand: renamed to a theorem, printed axioms, got
`[…r2CensusWmTrace._native.native_decide.ax_1_1]` — the native axiom, no `sorryAx`, as expected. Same
remedy as R8-D3, where the owner wrote the axiom lines into the committed artefact afterwards. Worth
noting the pattern rather than the instance: **a rule adopted mid-flight is met by hand for the lanes
already dispatched, and the ledger has to say which.**

### Two checklist items I had not done on R2-D3 — one clean, one genuinely missed

A stale payload replayed R2-D3's checklist and I checked the two items my close had not explicitly
covered.

**Item 3 — no second literal: clean, verified.** The committed artefact (`git show 4eb4f584:…`) contains
exactly **one** literal, `def wmTraceR2` at `:10`; `wmTraceR2Generated` is gone. codex-1 renamed rather
than added, which is what the corrected packet asked and the opposite of the duplicate-literal outcome I
was trying to prevent when I caught my own review error earlier.

**Item 8 — bind `:contract-sha`: I had not done it.** Now bound. Current contract source, read from
`mathlib4/DarkTower/WarMachine/holes-contract.json` rather than from any message:

    "source": {"git-sha": "1b09974aed1ce66dad0b728a6acc64c7b864b31e",
               "module": "DarkTower.WarMachine.Holes"}

So **R2-D3 binds at `1b09974a`**, the same source R8-D3 was bound to — consistent, since neither lane's
declarations moved after `1b09974a` (mathlib4 HEAD is `66317c71`, a contract re-emit on top of it).

Worth noting how it was missed: my close ran the *substantive* gates — elaboration at the sha, axioms,
tests, pin comparison — and skipped a *bookkeeping* item, because the substantive findings were where the
attention went. That is the mirror of this morning's failure, where I ran every listed check and missed
what the builder volunteered between them. **A checklist fails at both ends: what it does not list, and
what it lists but looks clerical.** The ledger has both instances now, an afternoon apart.

| 4 (R16) | R16-D1 (discovery, no code) | codex-2 | `invoke-1788108743427-4422-b8f1eda3` | `park-403bb305` | 16:52Z | **closed** | `b1830f5`, 43 lines; 3 pointers opened; pin matches; **selection census re-run here: 96 `:open-mission` / 696 other, inside-map 0** — exact match; refused my "over the 792" framing | pending |

### R16 opened — and my "unlocks 2" was a readiness measure, not a reach measure

The owner wrote `P-R16.md` from the artefacts rather than agreeing with the lookup, and corrected my
number: **R16 is the missing endpoint on five drawn edges** — `R11→R16`, `R14→R16`, `R15→R16`, `R9→R16`,
`R16→R2` — where I had said two. Both verified here against the linter's own output:

    drawn edges touching R16:            5
    of those, already half-recorded:     2   (R16→R2, R9→R16)

**Both numbers are right and they measure different things.** Mine counted edges *already half-recorded*,
which is the readiness question — how many edge specifications become dispatchable the moment R16 lands.
The owner's counts R16's total wiring reach. My framing — "R16 unlocks 2, twice any other" — was true and
understated the case, because restricting to half-recorded edges is a *CML-D2 readiness* filter that I
did not say I had applied. The lesson is the day's own: **a number needs its denominator stated**, and I
had just written that into R8-D4's packet about someone else's means.

**The caveat I flagged was measured and holds:** each of the six one-endpoint edges carries exactly one
payload *proposal* at its recorded end — proposals, not schemas — so 6 was an upper bound, and CML-D2
still needs a second proposal per edge before it has anything to compare. Two lines stand as given:
**specified 0 / unspecified 21.**

R16-D1 dispatched to codex-2 (fresh seat for this node). Its expectations are registered as predictions:
selected-vs-in-map **outside, all**; **no** observation channel reads an act's witness. I added a third as
an open question rather than a hypothesis — `enact.clj:205` is honest that `:enacted nil` means the
executor reproduced nothing, and **what that becomes downstream is unmeasured**: if it becomes a *score*,
R16 emits a number for an act that did not happen, which is the shape found four times today. I have not
looked, and said so, so it cannot be confirmed by someone trying to please the packet.
| 2 (R2) | owner gate: R2-D3 | codex-1 (via claude-20) | — | — | 16:53Z | passed — HOLES MOVED (native_decide, stated) | futon2 4eb4f58 | claude-15 16:53Z: gated AT THE SHA (git show) — exit 0; axioms [r2CensusWmTrace._native.native_decide.ax_1_1], no sorryAx; pin c9add16a… identical to R8-D3 under the ruled method; tests 4/25; bound (b181b54); the builder stated the pin's limit (identical enumeration ≠ correct filter) and measured the trust choice (decide: maxRecDepth then 200k heartbeats; native_decide 1.87 s). 3a(iii) remedy: generators emit the named theorem + #print axioms (R2-D4 / R8-D4) — my hand-added lines on R8-D3 reverted (58b55c0) so regenerate gates stay meaningful. **Last builder lane closed.** |

### An evidence type that cannot represent its own falsifier

claude-13's R8-D4 read found two defects in `EraSummary`, both signature-level. The second is the
sharpest thing found today.

**(1) `count` beside `meanPrecision` makes the type assert a falsehood.** The structure says the mean is
over `count`. Enumerated here:

    all era forms                   n=760   mean 94.4826
    with :prediction-errors         n=758   mean 94.4826
    with BOTH pe + :precision-state  n=755   mean 94.5845   ← the report's figure

Three populations, three defensible means. Writing 94.5845 into a summary with `count = 760` states a
falsehood, and a note annotates a false field rather than repairing it. **A mean is not a fact** — it is a
computed value over a population, and the population is part of the value. Proposed: carry `precisionSum`
and `precisionRecords` as facts, derive the mean in a closed body.

**The evidence is that I fell into it while checking it.** I set out to reproduce claude-13's arithmetic,
silently chose a different population, and got a different number. Two reviewers, same corpus, same
intent, divergent results — because the denominator was a choice rather than a type. That is a better
argument for the fix than either of our numbers.

**(2) `EraSummary` presupposes the law `r8EraBoundary` tests.** `storedF : Bool`, `selectionGain : Bool`
and `shape : FreeEnergyShape` are single values per era, so each asserts the era is *uniform* in that
property — and uniformity is exactly what the law claims. **The evidence type cannot represent the state
in which the law is false.** The lint would report `:conformant` for a table that could not have been
otherwise.

That is the same defect as an acceptance that cannot fail — `r2ContractCensus`, `r9WmCheckerSound`, the
ε-comparison — **one layer up, in the evidence rather than the proposition**. Fourth member of the family
today, and the first to appear in an evidence type. Proposed: counts and a per-era shape tally, so a
non-uniform era is representable and the law is what rules it out; it also composes with the third
`FreeEnergyShape` constructor, since a "neither" form becomes a count rather than a forced verdict.

**R8-D4 held.** Taking claude-13's advice to tell codex-12 the answer rather than leave it to refuse: the
refusal instruction is right in principle, but a 40-minute box spent rediscovering a conclusion we already
hold is waste, and a builder can only refuse once on one structure.

### A generated file can only be corrected by its generator

R2-D3 passed at its sha — the owner ran `git show 4eb4f58:…`, elaborated it, printed the axioms
(`[…r2CensusWmTrace._native.native_decide.ax_1_1]`, no `sorryAx`), confirmed the pin byte-identical to
R8-D3's, and bound the run. Last builder lane closed.

**The correction that matters is on the owner's side and I have carried it into R8-D4.** At R8-D3's gate
he added the `#print axioms` lines to the *generated* artefact by hand to satisfy 3a(iii) — and then
reverted them (`58b55c0`), because **a hand-edited generated file fails its own `regenerate && git diff
--exit-code` gate on the next run.** Verified here: the artefact is back to the generator's output, 0
occurrences of `#print axioms`.

The general form is worth keeping, because it is not obvious and it bit the most careful reader in the
build: **the regenerate gate exists to prove a file is its generator's output, so any correction applied
to the file rather than to the generator destroys the proof it was applied to satisfy.** A generated
artefact has exactly one honest edit point, and it is upstream. 3a(iii) is therefore a requirement *on
the generator*, and until R8-D4 and R2-D4 make the generators emit named theorems and print lines, the
axiom set lives in the registry binding rather than in the file.

**Also from his gate, worth recording against my own habits:** his first regenerate call *exited without
running*, which he caught by reading the exit code — the same mistake R8's gate made earlier. Two agents,
one afternoon, same silent no-op, both caught only by checking the exit status rather than the absence of
complaint. That is the third distinct shape of "an instrument that did not run" today.

**R16-D1 was already dispatched** (codex-2, `invoke-1788108743427`) before his "whenever you dispatch it"
was written — bells crossing in the harmless direction. Still running.
| 3 (R8) | owner: EraSummary ratified (facts + tallies; mean and uniformity derived) | claude-13 → claude-20 → claude-15 | — | — | 16:56Z | done | mathlib4 2f68318a+f910a405 | claude-15 16:56Z: a mean is not a fact (population carried); an evidence type must be able to represent the state in which the law is false; R8-D4 packet rewritten from the amended declarations — codex-12 told the answer |

### R16-D1 closed — it refused my framing while answering the question

codex-2 delivered in 43 lines. Verified here rather than accepted: `actuator_a3.clj:372` is
`reviewed-candidate-cleans` and I counted its entries — **exactly four**; the pin matches `c9add16a…`;
the reader loop with `:default` is stated with its command. I re-ran the selection census independently:
**792 forms, 96 `:open-mission`, 696 other, inside-map 0**, targets `M-canon-fingerprint-store` 44,
`M-capability-star-map` 29, `M-emacs-cursor-peripheral` 21, `M-futonzero-mvp` 2 — exact match.

**The refusal is against my packet and is correct.** I registered *"selected-vs-in-map over the 792
forms: expected outside, all"*. Only **96** of the 792 select an open mission at all. So the expectation
holds for the 96 and my "over the 792" framing was false — a denominator error in a registered
expectation, on the same day I twice caught denominators in other people's work and once in the R16 edge
count. That is now four denominator errors from me in one afternoon, which is a pattern rather than an
accident: **I write counts before I know which population the artefact actually has.**

**The open question came back as neither answer I offered.** `:enacted nil` **remains a nil realized
score** — not zero, not a typed absence. So R16 does not emit a number for an act that did not happen,
which is what I was worried about, and it also does not emit the typed absence the record requires: the
absence is *untyped*. Smaller than feared, real, and precisely the sort of thing that would have been
confirmed rather than measured had I stated it as a hypothesis instead of an open question.

**112 forms carry act-gate verdicts, 165 verdicts have both numeric legs** — the gate is not dark. And the
observation-channel negative is properly hedged: *"no explicit wiring reads enactment witnesses; indirect
flow through generic scan fields remains untested"* — an instrument naming its own limit.

**CML-D2 for `R16→R2` and `R9→R16` is now dispatchable** — the build's first two edge specifications,
after a day at 0 / 21.
| 4 (R16) | owner gate: R16-D1 | codex-2 (via claude-20) | invoke-1788108743427-4422-b8f1eda3 | — | 16:59Z | passed | futon2 b1830f5 | claude-15 16:59Z: pointers opened (4-entry map, its reader, observe); selection census reproduced (96 open-mission: 44/2/21/29, inside map 0); refusal upheld — the expectation was over the 96 not the 792; :enacted stays an UNTYPED nil (neither score nor typed absence) = the live refusing witness for P-R16 solved 1; witness-reading negative properly hedged; **CML-D2 for R16→R2 and R9→R16 dispatchable**; my own probes returned two spurious 0s (wrong key paths) — positive-control rule added |

### The denominator fix has a denominator ambiguity — forms vs channel values

Both `EraSummary` proposals ratified (`Holes.lean@2f68318a`): `precisionSum`/`precisionRecords` as facts
with a derived mean; `storedFCount`/`selectionGainCount`/`ShapeTally` with a derived `uniform`, so a
non-uniform era is representable. Then the row-11 dry-run **against the amended declarations** — computing
what the generator would have to write — found the next layer:

    era count (forms)                            760
    population, forms with pe + precision-state  755
    channel VALUES summed from those forms      5502
    precisionSum                          520403.9349
      sum / 755  (forms)    = 689.2767
      sum / 5502 (channels) =  94.5845   ← the figure in the docstring and every report today

**The reported mean is per-channel; the population label is per-form.** The docstring pairs
"755 → 94.5845", but `520403.9349 / 755 = 689.2767`. A generator reading `precisionRecords` as its name
suggests — forms — derives a mean **wrong by ~7.3×**, asserted with the type's full confidence.

**The fix worked exactly as designed, and this is what it caught.** The ambiguity was always there, hidden
in `:precision {:mean 94.58, :channels 5502, :records 755}` where the mean was over one field and the
label over another. Making the denominator a *carried fact* is what turned an invisible choice into a
findable one — it became findable only once the type had to state it. That is the best evidence yet for
the rule it came from.

Two rulings offered to the owner: `precisionRecords` = values summed (mean true by construction, units
honestly different from `count`), or keep records = forms and add `precisionChannels`. I lean to the
first; either way the docstring's "755 → 94.5845" pairing must be corrected, since that is the line a
builder will copy.

**Found by computing, not by reading.** I would have agreed with the declaration on a read; the numbers
disagreed. The dry-run against amended declarations *before* writing the packet is now the third time
today that step has caught something a careful reading would have passed.
| 3 (R8) | owner: EraSummary units ruled | claude-20 → claude-15 | — | — | 17:01Z | done | mathlib4 HEAD | claude-15 17:01Z: precisionValues (denominator by construction) + precisionForms (fact); docstring pair corrected; R8-D4 may be written from HEAD; found by computing what the generator would write, not by reading the type |

| CML | CML-D2 `R16→R2` (edge spec) | codex-8 | `invoke-1788109302306-4432-9a7b2fad` | `park-3157fc8c` | 17:01Z | **closed** | `031c5f2`, 101 lines; EDN and both records untouched; every field filled or `unspecified` with reason+pointer; 2 refusals | pending |

### The wiring's first line moves — and the packet says it may be one-sided

R16-D1 passed, so `R16→R2` is the first drawn edge whose both endpoints have records. CML-D2 dispatched
as a **proposal**, not a schema: the EDN is written by the owner after comparing both sides, never by a
builder or by me.

**Reading the two records before writing the packet turned up something worth stating up front:**
`P-R2.md` specifies payloads for its **outgoing** edges (`R2→R3`, `R2→R8`) and states **none for the
incoming `R16→R2`**. So this is not yet two proposals to compare — it is **one proposal and a gap**. The
packet says so explicitly and forbids manufacturing R2's side to have something to reconcile. If I am
wrong about the record, the builder is told to say so and reconcile properly; that is the likeliest
refusal in this packet and I would rather receive it than a symmetrical-looking fiction.

**And the edge carries no traffic.** R16-D1 established it: no observation channel reads an act's
witness, and `:enacted nil` is an *untyped* nil. So the packet requires the proposal to say that a schema
here is a **specification, not a description** — the distinction the whole build turns on, arriving for
the first time at an edge rather than at a node.

Charter 7a is in the packet's gates on its first use: **a probe returning nothing is reported only beside
a positive control on the same instrument.** Four silent probe failures were recorded this afternoon,
three of them mine, and the rule exists because a wrong key path is indistinguishable from an absence.

**Handed back by the owner rather than adjudicated, and now in P-R16 for R16-D2:** his any-depth probe
finds 661 gate entries, 653 carrying `:delta-F`/`:delta-G` and only 4 carrying
`:cascade-score`/`:coverage-score-delta` — while `close_loop.clj:65-108` emits the latter pair, which are
the glossary's act-gate legs. So either two gate shapes coexist in the trace, or R16-D1's "165 verdicts
with two numeric legs" counts a gate the glossary does not describe. Both instruments named, unresolved,
and it bears on R16 *solved* 3.

| 3 (R8) | R8-D4 (build, conformance) | codex-12 | see bell | park recorded at dispatch | 17:04Z | **closed** | `10826ee`; gated at the sha (EXIT 0, 0 errors); generator-emitted axioms quoted; values exact; **caught my dry-run partitioning by stored-F** | pending |

### R8-D4 dispatched — the answer told, and the dry-run done against the amended type

The units ruling landed as `(1)` with the units in the names: `precisionValues` is the denominator **by
construction**, `precisionForms` is a fact **never divided by**, `count` stays the era's form count.
Three numbers, three units, each named — and `meanPrecision` derived, so the body is true by definition.

**Dry-run against the amended declarations, which is where the last two findings came from:**

    Era.before  count 760  storedF 0   gain 0   shapes {760,0,0}
                precisionSum 520403.9349  values 5502  forms 755  ⇒ mean 94.5845  uniform true
    Era.after   count 32   storedF 32  gain 32  shapes {0,32,0}
                precisionSum 2429.5805    values 256   forms 32   ⇒ mean 9.4905   uniform true

Both eras `uniform`, so `r8EraBoundary` holds — **and the type can now express its failure**, which was
the point. A non-uniform era would show `storedFCount` strictly between 0 and `count`.

**The packet tells codex-12 the answer instead of inviting a refusal**, on claude-13's argument that I
accepted: a builder can only refuse once on one structure, and spending that refusal on a conclusion
three of us already settled is waste. It explains *why* each field exists — including that the first fix
had the family's defect one level down — so the names are not mysterious and the derivation is not
guessable-at.

Two boundaries drawn in the packet rather than left implicit: the `era-table?` **lint** check still looks
for the old field names, and that is an **AD-D3 follow-up, not codex-12's** — it is told to name it, not
patch it. And 3a(iii) is a **generator** requirement: the named theorem and the `#print axioms` lines are
emitted, never hand-added, because a hand edit fails the very regenerate gate that proves the file is
generated.

### The first edge specification — one-sided, and honest about it

CML-D2 `R16→R2` closed. The reconciliation is **one-sided**, as the packet predicted: R16 proposes
`{tick, mission, witness}`; R2 names the incoming edge but specifies payloads only for its *outgoing*
edges. **Six of the nine `Delivery` fields are `unspecified`** because neither record states them — each
with a reason and a pointer to what would settle it, and the builder's own sentence for why it did not
fill them: *"plausible operational defaults are not evidence."*

Two refusals, both right: it would not copy R2's outgoing receipt onto the incoming edge, and would not
invent defaults. That is the honest state of the wiring after a day of node work — **the edge's type is
known and its operational contract is not** — and it is worth more than a complete-looking schema.

It is also a **specification, not a description**: no observation channel can receive the payload, and
`:acknowledged?` is not a substitute since its only producer is the hard-coded `true` at
`lane_futility.clj:334`.

### `control-map-edges.edn` is untracked — the same defect as this morning's records

Found while checking the builder had not edited it: **`p4ng/empirics-futon/control-map-edges.edn` is not
in git.** Not ignored — `git check-ignore` returns nothing, and other files in `empirics-futon/` are
tracked. So the artefact the whole CML lane exists to specify, and into which the first schema is about
to be written, **has no version anchor**: nothing to gate at, no diff when a schema lands, and CML-D1's
hardcoded 21-edge baseline is checked against a file that can change without trace.

Same shape as the `P-*.md` records untracked this morning, fixed at `e01dab9` — and the third instance
today of *the thing being reasoned about not being anchored*, after the records and after the
gate-at-a-path confusion. The pattern is stable enough to state: **before a lane writes to an artefact,
check the artefact is versioned.** Reported to the owner; not staged by me, since it is his file and his
call.
| CML | owner gate: CML-D2 R16→R2 | codex (via claude-20) | — | — | 17:06Z | passed | futon2 031c5f2; p4ng 8f83901 (anchor) + schema commit | claude-15 17:06Z: note 101 lines, EDN/records untouched by the builder; both refusals upheld (no receipt copied across edges; no invented defaults); one-sided reconciliation stated as such; EDN was UNTRACKED (not ignored; 73 siblings tracked) — committed as the anchor BEFORE the first schema; R16→R2 schema written by the owner as the proposal with typed absences (6/9 unspecified) |

| CML | CML-D2 `R9→R16` (edge spec) | codex-2 | `invoke-1788109701708-4439-01b48387` | `park-e6da027e` | 17:08Z | **closed** | `ab539dd`, 52 lines; answered **one proposal endorsed twice**; 3 unspecified fields with reasons; pointers opened by exact line | pending |

### My citation was off by one, and it had already propagated

The owner caught it at his gate: `:acknowledged? true` is at `lane_futility.clj:**334**`; line 333 is
`:risk-mode? true`. **I had cited `:333` four times** — twice here, once in R2-D2's packet, once in
CML-D2's — and **codex-8 inherited it into its committed findings note**, because it came from my packet.
All four of mine corrected; the builder's note carries the inherited slip and the owner chose not to send
it back, which I agree with — the claim is right, the line is off by one.

Second time today a defect of mine propagated past its packet into someone else's artefact, after the
content pin reaching `wmTraceR8`'s docstring. Both were *values I supplied* rather than judgements I made,
which is worth noting: **the things that escape are the small transcribed particulars, not the
arguments.** A wrong argument gets refused; a wrong line number gets copied.

### Second edge dispatched — and the question it has to answer first

`R9→R16` looks two-sided where `R16→R2` was one-sided: P-R9 proposes
`{claim, witness {id, producer, layer}, verdict ∈ {independent, self, unknown}}` with `guarantee
ExactlyOnce`, `idem-key (claim-id, witness-id)`, `receipt = the verdict record`; P-R16 names the same edge
as "an act carries an independent witness of its precondition".

**But P-R16 does not propose independently — it cites P-R9's proposal.** So the packet asks first whether
this is **two proposals that agree** or **one proposal endorsed twice**, because that is the whole value
of the lane: agreement carries information only when the two sides arrived separately. **Two records
agreeing because one quotes the other carries none** — the same shape as two lanes' digests matching
because they ran the same filter, which claude-13 made me state in R2-D3's packet. The lane's own
premise, turned on the lane.

Also required: measure whether this delivery happens today rather than assuming it from the sister edge,
and whether `unknown` as a first-class verdict is a type an act's precondition can consume — a constraint
that belongs in the proposal if it holds.

### Second edge closed — and the lane's own premise turned on itself, correctly

The question the packet asked first was answered honestly: **one proposal endorsed twice.**
`P-R16.md:61-62` cites P-R9's proposal; what R16 contributes independently is exactly one thing, and it
is substantive — **the consumer constraint that an act requires an `independent` precondition witness.**

So the "agreement" between the two records carries **no confirmatory weight**, and the packet was right
to ask before reconciling. But the constraint carries real weight, and it is the kind only the receiving
end could supply. That is the distinction the lane exists to make, applied to the lane's own evidence.

**Its sharpest finding follows from the constraint:** `self` and `unknown` are *valid delivered verdicts*
and **neither authorises R16 to act** — the edge's type admits three values of which two are refusals,
which is R9's whole apparatus arriving at the acting end. And **no implementation carries an R9 verdict
into `enact!` or an act-gate today**, measured rather than inherited from the sister edge as the packet
required.

Three fields `unspecified` with reasons, what would settle them, and an explicit note that `R16→R2`'s
values were **not** imported — *different edge, different contract*. Receipt shape marked **inferred,
untested** rather than filled from the payload.

### `P-R16.md` is untracked — fourth instance of the anchoring defect today

`git ls-files holes/problems/P-R16.md` → **0**. The record written this afternoon, quoted by both CML-D2
packets and dispatched against by R16-D1, **has no sha.** After the `P-*.md` records (`e01dab9`) and
`control-map-edges.edn` (anchored an hour ago), this is the third artefact and the fourth instance —
counting the gate-at-a-path confusion, which was the same defect from the reading side.

It bites the gate-at-a-sha rule directly: a **committed** findings note (`ab539dd`) cites
`P-R16.md:58-64`, and there is no version of that file a reader can resolve the citation against. The
rule I proposed this morning — *before a lane specifies an artefact, the artefact is anchored* — needs its
companion: **before a record is quoted in a committed artefact, the record is anchored.** Reported;
not staged, as it is the owner's file.
| CML | owner gate: CML-D2 R9→R16 | codex (via claude-20) | — | — | 17:11Z | passed | futon2 ab539dd; p4ng schema commit | claude-15 17:11Z: pointers by exact line verified (Holes.lean:222-227; r9_independence.clj:13-22); one proposal endorsed twice (no confirmatory weight) + R16's consumer constraint (self/unknown are valid verdicts that authorise nothing); receipt shape marked inferred-untested; R16→R2 values not imported; 0 independence terms in enact/close_loop (measured); P-R16 and all amended records ANCHORED (futon2 e2679ee) — fourth untracked-artefact instance today, caught at the citation |

### Lane-status correction, and the anchoring confirmed

The owner's CML-D2 gate says "No lane is running." **R8-D4 is running** on codex-12
(`invoke-1788109437775-4435-c87cf785`, dispatched 17:04, checked `running`). Corrected by bell — a wrong
"nothing in flight" is how a session ends with a builder mid-turn, and R8-D4's registered dry-run values
still need gating.

Confirmed here: `P-R16.md` is now tracked (`git ls-files` → 1), and both edge schemas are in the EDN
(p4ng `891ab53` `R16→R2`, `459d33b` `R9→R16`). **He anchored the record before gating the note that cites
its lines** — the second time today the *ordering* was the fix rather than the change itself, after
anchor-then-schema on the EDN.

**What the wiring lane produced today, stated as the record demands:** two edge entries, one fully typed
(`R9→R16`, with the consumer constraint), one one-sided with typed absences (`R16→R2`) — and the two
lines still **specified 0 / unspecified 21**, because a schema whose operational values are `:unspecified`
is not specified by the linter's own rule. That number is the result, not a shortfall. Rounding it up
would have been the wiring lane's first facade, on a day the build spent refusing them everywhere else.

The remaining CML work is endpoint amendments — a paired write, a retry identity, a deadline — which are
record work and the owner's, not a packet.
| 3 (R8) | IN FLIGHT: R8-D4 | codex-12 (via claude-20) | invoke-1788109437775-4435-c87cf785 | park-d8a7321d | 17:13Z | DONE at the owner's read (was running at claude-20's check moments earlier) | vs Holes.lean@acfb3b13 | claude-15: my "no lane is running" was wrong when written — corrected by claude-20; the job then closed between its check and mine; awaiting claude-20's six-line review, then the owner gate at the sha; registered dry-run values to gate: before 760/0/0/{760,0,0}/520403.9349/5502/755 ⇒ 94.5845 uniform; after 32/32/32/{0,32,0}/2429.5805/256/32 ⇒ 9.4905 uniform; the era-table? shape check follow-up is AD-D3 territory |

### R8-D4 closed — and my dry-run carried the defect it was run to prevent

Gated at the sha: elaborates EXIT 0, zero errors; the **generator emits** both named theorems and both
`#print axioms` lines — 3a(iii) met by the generator for the first time, output
`[propext, Classical.choice, Quot.sound, …_native.native_decide.ax_1_1]` for each. kondo 0/0, 6 tests /
33 assertions. Emitted `EraTable` matches the registered values exactly. `meanPrecision` and `uniform`
never written, asserted **negatively** in its own tests — `(is (not (re-find #"meanPrecision :=|uniform
:=" lean)))`.

**The correction is against me and it is the sharpest of the four.** codex-12 partitioned the eras **by
file date**, not by stored-F presence, stating why: *so non-interleaving failures remain representable*.
**My dry-run partitioned by stored-F presence.** Under that partition `storedFCount` is necessarily 0 or
`count`, so `EraSummary.uniform` is **true by construction** — exactly the defect claude-13 found in the
type, which the whole `storedFCount`/`ShapeTally` rewrite existed to remove. My numbers were right only
because the biconditional holds on this corpus; the **method** would have reinstated the defect one level
down, in the generator, immediately after we removed it from the type.

It proved the difference instead of asserting it: a synthetic **post-boundary form with no stored F**
yields after-era `count 1, storedFCount 0` — a non-uniform era, representable, and impossible under my
partition.

**The general form, and it is a real gap in the row-11 rule:** *a dry-run can carry the defect it was run
to prevent.* I verified that the acceptance values were reachable and never asked whether the method
reaching them was sound. Row 11 checks satisfiability; it does not check the partition, the population, or
the filter that produced the number — and today those came apart three times: the 760/755 denominator, the
values-vs-forms unit, and now the era partition itself. **The dry-run needs its own denominator stated,
the same way the evidence does.**
| 3 (R8) | owner gate: R8-D4 — LAST LANE | codex-12 (via claude-20) | invoke-1788109437775-4435-c87cf785 | park-d8a7321d | 17:16Z | passed | futon2 10826ee | claude-15 17:16Z: gated at the sha — exit 0; generator emits 2 named theorems + 2 #print axioms (3a(iii) by the generator, first time); native axioms named in the artefact; meanPrecision/uniform never written (asserted negatively in its tests); eras partitioned BY FILE DATE (r8_f_contract.clj:199-200) so non-uniform eras are representable; synthetic post-boundary-no-stored-F test fails closed; table = registered values; tests 6/33; registry re-bound; lint: r8EraBoundary still :wrong-shape because era-table? reads the OLD fields — AD-D3b follow-up, reported as an actual |
| AUD | owner gate: AUD-D1 (dated readouts vs live endpoints, I_data_current) | codex-5 | invoke-1788112641868-4452-9f0f3deb | — | 18:12Z | passed | futon2 d1997fc | claude-15 18:12Z: 17 rows; 4 dates re-stat'd match to the second (mark2 state.json 05-17, evidence.edn 07-10, mana-snapshot 08-12, c-entries 06-26); tally (i)=0 (ii)=3 (iii)=1 (iv)=12 refusals=3; all 3 refusals upheld — and sharpened: `stack-logic-model.edn`/`alignment.edn` have NO commit ever in futon5a, so war_machine.clj:2075/2089/2150/2211 `when-let` sections have always rendered empty (silent-nil facade, not staleness); `mana-snapshot.timer` is not a systemd unit (E-wm-staleness-meta-stop expects it) so the 18-day-old mana snapshot is manual; packet's bge_ claim corrected by the builder (0 readers in scope). The WM core rows (stack-annotations, c-entries, devmap/clean/executed, wm-trace) are canonical local sources or the machine's own ledgers — current by construction, not snapshots; the three genuine snapshot reads are export evidence (4c fixes), mana (timer to make), mark2 (endpoint to make) |
| AUD | AUD-D2 `I_absent_is_loud` lint (helper + call-site census, fixture controls) | codex-5 | invoke-1788113563479-4463-55693e33 | park-33cd6090 | 18:26Z | running | packet 4a4606b md5 f07ef6f3…5e220 | Joe 18:18Z: stack-logic model unrecognised → not to be built either way; "that rule definitely needs to be enforced" → invariant I_absent_is_loud written (futon4 5559fd3, futon2 2d501d7) before the instrument was commissioned. References traced: M-war-machine.md 05-03 lists both files as planned inputs; never produced in any repo; belief.clj's mention reads ticks from stack-annotations, not the model |
| AUD | supersession record: stack logic model | claude-15 | — | — | 18:42Z | recorded | P-supersede-stack-logic-model.md | Joe 18:40Z: "not that we need to include it, but we need to supersede it" — the old model was a hand-written model of the operator (workstreams, jsdq-mode, pocketwatch ticks over commit ratios); its successor is the operator loop closed through R2 over live evidence (sec-operator.tex; M-formal-war-machine.md:807-814). AUD-D3 deletes the reads citing the record. Big picture noted on BUILD-status: fix AND extend |
| AUD | owner gate: AUD-D2 | codex-5 | invoke-1788113563479-4463-55693e33 | park-33cd6090 | 18:58Z | passed with owner fix | futon2 082da13 + f3e928f (owner) | claude-15 18:58Z: ran the lint bare — TRUE exit 1 with 7 silent+absent-now (my first read said exit 0: I read `$?` after a `\| tail` pipe — tail's code; charter 7a, my own instrument); the 5 known sites + read-edn-file@482 all present; 7th site is joe_hud.clj:429 `jsdq-terminal-vocabulary.edn` — a THIRD never-produced 05-03 input; controls 1/0; kondo clean. Defect (mine, in the packet): `declared-optional` by NAME regex (`safe|maybe|optional`) let `safe-slurp-json` (the mark2 cache reader) pass on its name alone — fixed at the gate: docstring-only declaration, fixture +1 case (name-only safe helper → violation; same body with a docstring → 0), controls now 2/0; census 17 silent / 1 declared-optional / 45 silent call sites / 7 absent. Refusals: none; dynamic paths reported as dynamic/refused rather than guessed |
| R19 | R19-D1 preference-stack record (discovery; via tech lead) | claude-20 → codex seat | invoke-1788115145640-4476-c3ffdba0 | park-66e0c7b5 | 19:45Z | running | packet ff6c57e md5 8cf33b31…646a | Joe 19:35Z: "R19-D1 should proceed" + amendment: R19 is a new tetrahedron — the four strata are nouns only; the deliverable is a STACK record with purpose stated at the strata level, situation-evidence, falsifier, holder/parent (P-R19 §tetrahedron). D2 (Lean PreferenceLayer/PreferenceStack + recorded fold) after the owner gate |

## Session close — the build's own numbers, stated as the record demands

All lanes gated. Nothing running. Counted rather than recalled:

    Holes.lean            34 bodies / 32 holes   (contract source acfb3b13)
    packets written       17
    findings notes        10
    ledger                2053 lines
    wiring                two edge entries; specified 0 / unspecified 21

**What the day produced.** Three node records built to the hole (R2, R8, R9) plus P-R16 for the wiring;
holes discharged by a run for the first time — four by kernel `decide` (R9), three by stated
`native_decide` (R2, R8); and the wiring's first two edge specifications, one one-sided with typed
absences, one fully typed with a consumer constraint.

**What it did not produce, said plainly:** the wiring is still **specified 0 / unspecified 21**. Two
entries exist and neither carries an operational contract, because no endpoint record states a paired
write, a retry identity or a deadline. That number is the result. Rounding it up would have been the
wiring lane's first facade.

**The count that matters most for the charter's purpose.** Commissioner-side defects: **eight**, of which
**six were mine** — "54 entries"; the `:acknowledged?` expectation; R8-D2's empty ε-comparison; R2-D2's
non-discriminating falsifier; "over the 792"; and the dry-run partitioned by stored-F. Every one was
caught by a builder or a reader **refusing rather than complying**, and not one by the review checklist
that was supposed to catch them. Two of my defects escaped their packet into someone else's artefact
before being caught — a content pin into a Lean docstring, a line number into a committed note — and both
were *transcribed particulars* rather than arguments. A wrong argument gets refused; a wrong number gets
copied.

**The rules the day bought, each from a refusal rather than from design:**
7a (a negative needs a positive control) from four silent probe failures; 3a (quote the axioms, never
grep for `sorryAx`) from claude-13 asking a question I could not answer; 3a(i) (gate at the sha) from
elaborating a path a live lane was editing; 7b (a blocker can expire between run and review) from
codex-12; row 23 (anchor the artefact before specifying it) from four unanchored artefacts; row 24 (a
dry-run can carry the defect it was run to prevent) from codex-12 again.

**The through-line, if the ledger has one:** every defect found today had the same shape — *something
that could not have come out otherwise, presented as though it had.* An acceptance that cannot fail, a
hypothesis that pins the interesting case out of reach, a quantifier over the thing the run fixes, an
evidence type that cannot represent its own falsifier, a partition that makes uniformity certain, a pin
whose method was never stated, a digest that agrees because both sides ran one filter. The apparatus that
caught them was not the checklist. It was seven agents each willing to say *this does not test what it
says it tests* — including about their own work, which is the part that cannot be delegated.

## R19 — preferences (new lane, opened 2026-08-30 ~18:40Z)

| lane | packet | seat | job-id | park-id | at | state | notes | gate |
|---|---|---|---|---|---|---|---|---|
| R19 | R19-D1 read (charter 6b) | claude-13 | `invoke-1788115276606-4477-b78b7abc` | `park-6ccecae3` | 18:41Z | reading | packet md5 `8cf33b31f0fdc03a279fab8bb9c3646a`; both files tracked and clean at HEAD | — |
| AUD | owner gate: AUD-D3 | codex-5 | invoke-1788114147464-4468-afdc6aec | park-478a22c7 | 19:58Z | **NOT passed → AUD-D3b + AUD-D4** | futon2 a8dfd33; futon0 ad78a10 | claude-15 19:58Z: commit msgs cite the supersession record ✓; three helpers loud ✓; lint bare exit 0 ✓; kondo ✓; dead sections gone ✓. DEFECT: callers at :554/:566/:616 (+:521, :593) do `(when-not (unreadable-input? x) x)` — the marker becomes nil; :3814 threads it into `:load-status`, which nothing renders. The silence moved from the helper to the call site and the lint cannot see it (classifies helpers only) — the enforcement run said 0 while the invariant was violated (row 24's shape, in a gate). Builder's "no markers rendered" had no positive control: all six inputs exist now, so no marker could appear (7a). Report build was under a stubbed selector seam — the standalone WM report is NOT runnable (`requires the shared reason-bearing selector`; bb entry blocked in lane_futility.clj) — recorded as a blocker that bears on "turn the nightly tick back on" (7b). AUD-D3b → codex-5 (markers reach `## Input status` + `:input-status` in the trace; trace-write catch made loud; positive control by path override); AUD-D4 → second seat (lint sees marker-swallowing; fixture 3/0) |
| AUD | AUD-D3b markers reach the report + trace | codex-5 | invoke-1788115343131-4479-2235db1d | park-ce5193ad | 20:02Z | running | packet f2f6ee2 md5 9718fdde…794a | holder of war_machine.clj; parallel with AUD-D4 (different file) |
| AUD | AUD-D4 lint sees marker-swallowing | codex-12 | invoke-1788115354081-4480-920608f2 | park-a402ad7a | 20:02Z | running | packet f2f6ee2 md5 722e6f7f…515f | holder of absent_is_loud_lint.clj + fixtures; must not touch war_machine.clj |
| R19 | tech-lead pre-dispatch check on R19-D1 | claude-20 | invoke-1788115145640-4476-c3ffdba0 | park-66e0c7b5 | 20:12Z | packet amended fd81b37; with claude-13 (invoke-1788115276606-4477) then dispatch | — | claude-20 found: (a) 6b′ hash mismatch on first use — MY digits mistyped in the bell, file intact (rule: paste md5sum output, never transcribe); (b) `:composition-order` template listed the habit prior as folded while the packet asked whether it is — `habit` occurs 0× in c_vector.clj — my fourth answer-inside-the-input today; (c) `[]` evidence = evidence vertex with no mass; (d) `purpose nil` forces facade-or-blank. Owner decisions: composition-order lists only folded layers with :folded-at; `:fit-status :unwitnessed` required when evidence is []; purpose split into :declared-purpose (quotation or nil+looked-at) and :observed-purpose (never refusable). Cap: two dispatchable lanes accepted |

### 6b′ caught a hash mismatch on its first use — and it was the stated hash, not the file

The dispatch bell gave the packet's md5 as `8cf33b31f0fdc40…`; the file is
`8cf33b31f0fdc03a279fab8bb9c3646a` — common prefix `8cf33b31f0fdc`, then divergent. Classified rather
than reported: both files are **tracked, clean against HEAD, mtime 18:38:48**, 48 s before my check. So
the file is intact and anchored and the bell's digits are mistyped. Benign — and it is the third
transcribed particular to go wrong today after my content pin and my `:333`, which is now enough of a
pattern to say the rule plainly: **hashes and line numbers are the things that get copied wrong; arguments
get refused.**

### A verified defect in the packet: the template presupposes the question it asks

`:composition-order [:floor :goal-outcome :overlays :habit-prior]` is given as "the order the code
actually folds them", while the same packet asks the builder to determine *"whether the habit prior is in
fact folded into C or only into g"*.

Checked: **`grep -c habit src/futon2/aif/c_vector.clj` → 0.** The fold takes the static floor
(`futon2.aif.preferences`, `:35`), derives the stated/goal-outcome channel natively, and merges the
overlays semi-live (`:196-203`). The habit prior is not in it — which is exactly what P-R19's table says.

So the template **answers the packet's own question in the builder's input**, the shape removed twice
today (from `EraSummary`, and from my own dry-run partition). It also collides with the acceptance, which
requires every `:composition-order` entry to cite a file:line that exists: as written it forces a false
citation or a refusal. Proposed fix put to claude-13: `:composition-order` lists only `:folded? true`
layers each citing its folding line, and the habit prior is described with `:folded? false` plus where it
*does* enter. Acceptance-adjacent, so the owner is told before dispatch.

**Checked and did NOT flag:** the record's heading "three sources, three authors" against a four-row table
is not a count error — the fourth row is a preference that is not a C source. After four denominator
mistakes of my own today, I verified before claiming.

| R19 | R19-D1 (discovery) | codex-10 | `invoke-1788115507705-4482-1f16538a` | `park-c3ef84e7` | 18:45Z | **closed** | `dc1dac8`; 5 layers / 4 folded; `:declared-purpose nil` with 4 searched records; `:fit-status :witnessed`; **found a fifth folded source and corrected its two readers** | pending |
| lib | owner gate: packet 4c (live rung-1 index) | codex-20 | invoke-1788112644846-4453-4d0dfcd1 | — | 20:40Z | **passed for the corpus; coverage claim NOT accepted → 4d before wave 2** | futon3 cad5034 | claude-15 20:40Z: no migration-export refs ✓; basis pin differs across two builds (191328 → 191356, 28 records) ✓; kondo ✓; tests 2+2 green ONLY under `bb --classpath .:test` (JVM alias cannot load library-graph-lint-test: babashka.process) — builder did not say which; cursor fixture ✓. NOT accepted: the table 33/33, 23/23, 28/28 — its "non-listing" column counts self-text and co-mention hits. Owner hydrated all 157 clean war-room records: 23 are today's zai spider-worker turns (author zai-1/zai-2, `zai-turn-` bodies, text-search tool calls for the very pattern) and 15 are Agency invoke-complete records of codex-20's fleet jobs — the instrument's own trace in the live store, invisible to `spider-self-text?` (three prompt strings). Honest war-room: 28 patterns / 25 clean / **21 clean from non-spider records**; wr-5, wr-6, wr-18, wr-23 covered only by the spider looking for them. Export-era 10/10 → 21 real: the rise is real and smaller than reported. 4d → codex-20: reflection by provenance; columns any/clean/clean-non-reflection; wave-1 attestations re-flagged; wave 2 held |
| lib | packet 4d reflection exclusion | codex-20 | invoke-1788115896928-4491-4dafbd47 | park-f8279fa5 | 20:42Z | running | packet md5 992265daf7a57d6fe64f3e6622a1f333 | wave 2 held behind this gate |
| lib | 4c gate addendum: test's live dependency | claude-15 | — | — | 21:02Z | recorded | test/library_graph_lint_test.clj:28-35 | Each refusal fixture shells `bb checks/library_graph_lint.clj` with no evidence-index override; post-4c the lint ensures the live index, so a cache miss = full ~7-min scan of :7073 INSIDE the test, per fixture. Builder's green run (18:4x) rode a minutes-old warm cache; owner's run at 20:5x hit a moved basis and timed out at 5 min. `spider-runner-test` 2/8 verified green by the owner; `library-graph-lint-test` unverified. Check at the 4d gate: lint gains an `--evidence-index <path>` (or offline flag) and the fixtures pass a tiny synthetic index — a unit test must not depend on how many records arrived since the last cache |

### Assert-before-write stopped me overwriting the owner's amendment

My first fold-in attempt failed an anchor assert and wrote nothing — md5 unchanged, verified. Between
that and my second attempt **the md5 moved** (`8cf33b31…` → `8b809da4…`): claude-15 had amended the
packet at `fd81b37`, crediting the `:composition-order` finding and going further than either claude-13
or I proposed. **Had the script written before asserting, I would have clobbered the owner's amendment
with my own weaker version of it.**

This is the exact inverse of this morning's failure (`d68240c`, `f860296`), where I wrote first and
asserted after, and recorded two edits that had not happened. Same rule, both directions: **assert the
anchors, then write.** In a shared checkout it is not only a correctness check on my own edit — it is a
concurrency check on everyone else's.

**The owner's amendment is better than what I was about to apply**, and worth recording why:
- `:purpose` split into **`:declared-purpose`** (a quotation with its source, or nil **with
  `:declared-purpose-looked-at`** naming what was searched) and **`:observed-purpose`** (never nil —
  "the code does something"). That is sharper than claude-13's `{:stated nil :finding …}`: it separates
  *what was claimed* from *what the fold actually does*, and makes the absence of a declaration a fact
  with a search record rather than a sentence.
- `:fit-status :witnessed|:unwitnessed`, `:unwitnessed` iff `:situation-evidence` is `[]` — the same
  absence-as-value move as my `:admissible? false`, in the record's own vocabulary.
- `:folded-at` / `:enters-at` beside `:folded?`, so the nouns/verbs split carries its citations.

### What I folded on top (claude-13's two, both verified here)

- **The habit prior's seat**: `efe.clj:698-722` — *"whose canonical seat is the habit prior ln E(π) (R12),
  not a controller summand"* — with **two modes**: `:controller-augmentation` (DEFAULT), contribution
  stays in `:controller-score`; `:habit-prior` (DARK) at `:704`, where the term **leaves**
  `:controller-score` and the layer, emitting `:habit-prior-bias` into `policy/select-action`'s log-prior
  (`:722` `habit-prior? (dissoc :structural-pressure)`). So `:enters-at` is **mode-dependent** and a
  single-mode answer is incomplete. This turns `:folded? false` from a placeholder into a located finding.
- **A cross-lane collision neither packet cited**: `Holes.lean:493` declares `softmax` as
  `Q(π) ∝ exp(ln E(π) − G(π)/τ)` — *"both the log habit prior and grade term are retained"* — and
  `CommitmentTemperature.lean:124` already has `def habitPrior : Selector`. That `ln E(π)` looks like this
  same habit prior from the Lean side. The packet now requires the builder to **state whether it is**,
  because if so R19's fourth layer already has a Lean home and R19-D2 must connect to it rather than coin
  a second name for one object.

### R19-D1 closed — it found a fifth layer and corrected both its readers

Acceptance checked here, not accepted: EDN parses; `:fit-status :witnessed` agrees with 5
`:situation-evidence` entries; `:composition-order` holds exactly the four `:folded? true` layers each
with a `:folded-at`; no empty author or basis; pointers opened **by exact line**; note exactly 15 lines;
no `.clj`/`.bb`/`.lean` touched.

**`:declared-purpose nil`** — refused rather than invented, with 4 searched records recorded and the
observation that *"P-R19:88 gives an example rather than declaring this deployment."* The owner's
declared/observed split did its job: the honest answer landed as a **finding** instead of a blank.

**A fifth folded preference source, absent from P-R19's four-row table:** capability-zone load,
`preferences.clj:140-173` → `efe.clj:586-614`. Verified here: `zone-risk` is a `pref/c-distribution`
parameterised by the **learned** `:load-weight`, and `g-risk (+ channel-risk zone-risk)` — a separate
additive fold whose author class (posterior evidence mass) differs from the hand-set floor. **The
record's own inventory undercounted**, and the packet's "if you find a FIFTH, add it and say where"
clause is what surfaced it.

**And it corrected claude-13 and me.** We both read `efe.clj:698-706` and took its
`:controller-augmentation (DEFAULT)` / `:habit-prior (DARK)` labels at face value — I verified the quote
and folded it into the packet as established. **The labels are stale.**
`war_machine.clj:247-256`: *"D-1d relocation FLIPPED LIVE by Joe 2026-07-13. Structural pressure leaves
`controller-score`; the policy layer receives a habit prior at the unscaled ln E(π) seam"*, with
`FUTON_WM_STRUCTURAL_PRESSURE_MODE=controller-augmentation` as the **rollback hatch**. So `:habit-prior`
is *today's default*, not a dark path — the operator's learned preference is live in the selection seam
right now.

Worth stating as a shape, because it is new today: **I verified a quotation and inherited its staleness.**
The line said what claude-13 said it said; what neither of us checked was whether the label `(DEFAULT)`
was still true, seven weeks after the mode was flipped live. A citation can be accurate about its text and
wrong about the world — which is the same gap as a pointer that resolves to a line that no longer means
what it says. Row 6 covers negatives naming their instrument; this needs its own clause: **a quoted label
is evidence about the comment, not about the running default.**

**Cross-lane answered:** `Holes.lean:493-499` and `CommitmentTemperature.lean:99-125,224-228` model the
same ln E(π) object, consumed at `policy.clj:82-104`; the Lean abstracts its values and adds no second
prior source. R19-D2 connects rather than coins. And the naming discrepancy is now three-way and
decidable: the record says the habit prior enters **R14** as g, `efe.clj:698` says **R12**, the live seam
is `policy/select-action` — one object, three names, owner's to settle.
