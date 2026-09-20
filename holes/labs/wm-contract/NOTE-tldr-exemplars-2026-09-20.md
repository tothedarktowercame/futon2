# Tickbox exemplars already present in registry missions

Read-only corpus study for the tl;dr pilot. Mission files are unchanged. A tick is a recorded closure **claim**, not independent proof that its underlying work happened. This note classifies the writing; it does not adjudicate those claims.

## Source, extraction, and population boundaries

The source list is the 328 entries returned by `futon2.aif.mission-registry/load-missions-from-files` over `/home/joe/code`. No recursive filesystem glob or worktree scan supplied mission paths. Retain documents containing a ticked checkbox; extract checkbox-bearing physical lines with `^\s*[-*]\s+\[([xX ])\]\s+(.*)$`. This yields **35 missions, 510 ticked and 150 unticked lines**. Repeated lines count as separate occurrences. Unticked items come only from those same 35 documents, not the entire registry. Continuation lines are not silently merged: the current want projection itself uses the checkbox line. The audit ledger records each line and classification.

The request’s two population predicates overlap. This note preserves both literally: **Closed** means registry `:complete` OR the status line contains the word DOCUMENT; **Late** means still live (not complete/inactive/draft) AND the status line contains INSTANTIATE, VERIFY, or DOCUMENT. These are status-text filters, not adjudicated lifecycle states. A reference to a future DOCUMENT pass also matches. No IDENTIFY-section filter is imposed on extraction.

| Population (documents with ticked items) | Missions | Ticked | Unticked |

| --- | --- | --- | --- |

| Closed | 12 | 224 | 35 |

| Late-phase but unmarked | 4 | 115 | 7 |

| Overlap: M-self-documenting-stack | 1 | 52 | 1 |

| Closed-only | 11 | 172 | 34 |

| Late-only | 3 | 63 | 6 |

| Neither (Other) | 20 | 223 | 109 |

| Deduplicated total | 35 | 510 | 150 |



The complete registry contains 217 live missions. The literal Closed predicate selects 59 documents before requiring ticked boxes; Late selects 36. Thus the packet’s 24/39 figures are not these scan results or these denominators. `M-evaluate-policies` is `:unknown` with “ALL PHASES THROUGH DOCUMENT complete …”, matches both predicates, but has **zero checkbox lines** and contributes no exemplars. `M-vsatarcs-invariants-integration` contributes **31 ticked / 0 unticked** despite being live. Across all selected documents, 133 ticks are in live statuses and 377 in complete/inactive statuses, reproducing the packet’s original split. Inactive missions are retained as Other, not silently relabelled Closed.

## Registers: a manual, exhaustive coding pass

Each of the 660 extracted lines receives exactly one register. These are judgments about wording, not a trained classifier or a claim of inter-rater agreement. Mixed lines use this precedence: a reported numeric result → counted-retrospective; an explicit leading action request → task-imperative; an interrogative or Q-labelled survey prompt → question; a noun/topic/artifact label without a predicate → artifact-or-topic-fragment; otherwise → fact-assertion (including reduced clauses such as “Design sketched”). Original first-line truncation is retained. The item ledger makes every judgment reviewable.

Two registers beyond the requested three are necessary: **question** and **artifact-or-topic-fragment**. A checked “Motivation” heading is neither an imperative nor an assertion that specifies what was delivered. “Why …” prompts remain questions even without a question mark. Counted-retrospective records measured/reported sizes, test totals, or inventory additions; it is not triggered merely by a version, identifier, date, or numeric acceptance threshold.

| Register | Closed ticks | Late ticks | Other ticks | All ticks, unique | Unticked, unique |

| --- | --- | --- | --- | --- | --- |

| task-imperative | 37 | 8 | 10 | 55 | 22 |

| fact-assertion | 94 | 56 | 153 | 275 | 79 |

| counted-retrospective | 44 | 36 | 39 | 95 | 0 |

| question | 19 | 0 | 1 | 20 | 0 |

| artifact-or-topic-fragment | 30 | 15 | 20 | 65 | 49 |



Closed and Late columns overlap; do not sum them. Unique tick totals are 55 imperative + 275 assertion + 95 counted retrospective + 20 question + 65 fragment = 510. Unticked totals are 22 imperative + 79 assertion + 49 fragment = 150. An assertion is the most common register, but that does not establish it as the best register: many assertions depend on local context or an open-ended inventory.

## Worked sample: 36 verbatim ticked lines

These span both requested populations and all five registers. They deliberately include annotations, fragments, subjective language, truncated lines, and a ticked deferral. Each code block is the exact extracted physical line, not an edited model item. IDs refer to the complete ledger.

**I341 — M-three-column-stack — Closed — task-imperative — line 262**

```text
- [x] Name the gap and motivate the mission
```


**I342 — M-three-column-stack — Closed — task-imperative — line 263**

```text
- [x] Define the three columns with examples
```


**I346 — M-three-column-stack — Closed — task-imperative — line 267**

```text
- [x] Review with Joe — does the framing match the vision? (approved 2026-03-04)
```


**I116 — M-IRC-stability — Closed — fact-assertion — line 196**

```text
- [x] Server sends PING to idle clients after :ping-interval-ms
```


**I121 — M-IRC-stability — Closed — fact-assertion — line 201**

```text
- [x] All catch blocks log or emit evidence (no silent swallowing)
```


**I129 — M-IRC-stability — Closed — fact-assertion — line 251**

```text
- [x] Existing tests still pass
```


**I007 — M-aif2 — Closed — fact-assertion — line 125**

```text
- [x] `niche-construction.flexiarg` carries the corrected (scoped) conclusion + the endogenous-niche `HOWEVER` block. *(Landed 2026-05-31.)*
```


**I008 — M-aif2 — Closed — counted-retrospective — line 629**

```text
- [x] **`.aif.edn` meta-model + I1–I6 audit** — `aif2-exotype.edn`, 8/8 `ct.mission/validate`.
```


**I013 — M-aif2 — Closed — fact-assertion — line 634**

```text
- [x] **R11 re-graded (RULING, 2026-06-02):** the documented re-eval trigger ("VSATARCS gains writer capability over the same substrate the WM reads from") **HAS FIRED — weakly.** `:mission-doc-sync` writes `**Status:**`/checkpoint markers to the `M-*.md` docs the WM's `mission-registry` (v0.18) reads to populate `:open-mission`. So R11's `N/A` is re-graded to **`fired-weak`: uncoordinated action on shared FILE-STATE (not shared belief)** — a real but partial firing via the mission-doc substrate (not the belief-level coupling R11 originally contemplated). The flip M-aif2 was chartered to *examine* is examined and ruled: N/A → fired-weak.
```


**I015 — M-aif-head — Closed — question — line 307**

```text
- [x] **Q1:** What is the current interface between the Mission Peripheral
```


**I023 — M-aif-head — Closed — artifact-or-topic-fragment — line 415**

```text
- [x] **Q9:** Wiring diagram: Mission Peripheral as AIF+ agent.
```


**I345 — M-three-column-stack — Closed — artifact-or-topic-fragment — line 266**

```text
- [x] Scope in/out
```


**I356 — M-three-column-stack — Closed — counted-retrospective — line 417**

```text
- [x] Entity type table (§3.1 — 4 math, 6 project, 4 code entity types
```


**I373 — M-three-column-stack — Closed — artifact-or-topic-fragment — line 1021**

```text
- [x] ≥200 hyperedges across all three columns
```


**I468 — M-codex-irc-execution — Closed — artifact-or-topic-fragment — line 7**

```text
- [x] **Motivation**
```


**I480 — M-codex-irc-execution — Closed — task-imperative — line 119**

```text
- [x] Define job entity schema (`job-id`, state, timestamps, trace-id, evidence summary, artifact ref, delivery state).
```


**I494 — M-codex-irc-execution — Closed — task-imperative — line 309**

```text
- [x] Add integration tests for accepted->terminal invariants.
```


**I507 — M-portfolio-inference — Closed — counted-retrospective — line 1041**

```text
- [x] All 6 Chapter 0 invariants satisfied at portfolio level
```


**I511 — M-portfolio-inference — Closed — counted-retrospective — line 1045**

```text
- [x] 69 portfolio-specific tests passing (929 total suite)
```


**I568 — M-self-representing-stack — Closed — fact-assertion — line 764**

```text
- [x] Hyperedge creation round-trips through XTDB (store and retrieve)
```


**I057 — M-self-documenting-stack — Closed + Late — counted-retrospective — line 500**

```text
- [x] 6 new survey questions Q1bis-Q6bis have concrete answers
```


**I058 — M-self-documenting-stack — Closed + Late — counted-retrospective — line 501**

```text
- [x] Inventory additions documented (5 ready / 4 missing beyond §2)
```


**I060 — M-self-documenting-stack — Closed + Late — fact-assertion — line 503**

```text
- [x] No design decisions made yet; design lives in DERIVE-bis
```


**I066 — M-self-documenting-stack — Closed + Late — fact-assertion — line 760**

```text
- [x] Wiring diagram updated (textual; futon5 `.edn` deferred to VERIFY-bis)
```


**I082 — M-self-documenting-stack — Closed + Late — fact-assertion — line 1092**

```text
- [x] Every survey question Q1-Q8 has a concrete answer.
```


**I234 — M-vsatarcs-writer — Late — fact-assertion — line 259**

```text
- [x] Every survey question Q1-Q8 has a concrete answer (above).
```


**I235 — M-vsatarcs-writer — Late — counted-retrospective — line 260**

```text
- [x] Ready-vs-missing table is complete (10 ready / 10 missing).
```


**I548 — M-action-cost-modelling — Late — fact-assertion — line 57**

```text
- [x] Operator-voice anchor present (Joe's "premature to greenlight new work" quote)
```


**I553 — M-action-cost-modelling — Late — fact-assertion — line 62**

```text
- [x] Provenance recorded (HEAD authored claude-1 2026-05-26 after `713c74d`)
```


**I630 — M-vsatarcs-invariants-integration — Late — task-imperative — line 122**

```text
- [x] **Inventory existing infrastructure:** initial pass recorded below.
```


**I633 — M-vsatarcs-invariants-integration — Late — task-imperative — line 125**

```text
- [x] **Answer survey questions:** Q1-Q6 answered below.
```


**I641 — M-vsatarcs-invariants-integration — Late — fact-assertion — line 329**

```text
- [x] **Wiring diagram:** deferred to VERIFY as an explicit carry-forward because
```


**I653 — M-vsatarcs-invariants-integration — Late — counted-retrospective — line 1039**

```text
- [x] **0a. Pin count strata:** VERIFY count precision pass records 47 authored
```


**I654 — M-vsatarcs-invariants-integration — Late — task-imperative — line 1042**

```text
- [x] **0b. Build repeatable read-only enumerator:** one command/script that
```


**I655 — M-vsatarcs-invariants-integration — Late — task-imperative — line 1045**

```text
- [x] **0c. Classify all 86 current queue rows:** invariant candidate, witness
```


**I660 — M-vsatarcs-invariants-integration — Late — fact-assertion — line 1060**

```text
- [x] **4. Consumer migration:** Arxana reads the one projection (Live Invariants
```


## Hazards, counted with explicit limits

Hazard labels overlap and are independent of the register. These are manually reviewed textual risk indicators, not proven failures. The ledger tags every counted occurrence. Population columns retain the overlap described above.

| Indicator | Closed ticks | Late ticks | Other ticks | All ticks, unique | Unticked, unique |

| --- | --- | --- | --- | --- | --- |

| A: result-like annotation on tick | 63 | 50 | 48 | 130 | 0 |

| B: unbounded inventory quantifier | 4 | 1 | 10 | 15 | 5 |

| C: document-bound reference | 90 | 72 | 21 | 155 | 23 |

| D: reported numeric result | 44 | 36 | 39 | 95 | 0 |

| D: numeric target/threshold | 4 | 1 | 3 | 8 | 29 |

| D: fixed structural cardinality | 3 | 2 | 6 | 11 | 5 |

| Additional: wrapped continuation | 41 | 11 | 23 | 75 | 38 |



### A. Annotation: 130 candidates, but suffix annotation is not automatically a broken C4 witness

Count A when a ticked line has a parenthetical or trailing result-like annotation: approval/date, commit or artifact receipt, measured inventory/test total, validation result, disposition, or explicit completion commentary. Exclude parentheses that only explain an interface, a condition, or the task’s intended scope. These **130** are “looks added at completion” candidates; their addition time has not been proven by 130 history audits. They include mixed and borderline cases, all listed. A plain completed predicate without a suffix is not itself counted as annotation.

The packet’s supplied example is genuinely annotate-on-tick. In futon4 commit `7297cb6`, Git shows:

```diff
-- [ ] Review with Joe — does the framing match the vision?
+- [x] Review with Joe — does the framing match the vision? (approved 2026-03-04)
```

However, the claim that this suffix breaks the witness is false for the current implementation. `mission-hole-wants/closed-form` keeps the original text, but `observation-checks/decl-present?` matches an **anchored prefix with a boundary**, not a whole-line equality. Its suffix lookahead accepts whitespace, colon, opening parenthesis/brace/bracket, or end of line. A fresh tooling JVM called the actual function against the following strings, with the unchanged question as the declaration:

| Observed line variation | C4 match |

| --- | --- |

| Unannotated ticked question | true |

| Original question + “ (approved 2026-03-04)” | true |

| Approval inserted after “Joe”, before the rest of the question | false |

| Approval inserted before the original final question mark | false |



Therefore **0 of the one history-confirmed annotate-on-tick examples inspected here is a demonstrated broken witness**. Corpus-wide broken-witness count is **not established**, not zero: present-day lines cannot reconstruct every prior unchecked declaration. An edit inside the original declaration, deletion, or altered punctuation can break the match; a permissible suffix can preserve it. It can also append a qualification or retraction that the prefix check does not understand. Put evidence and qualifications on a separate line as the safe drafting convention; do not teach whole-line equality that the code does not enforce.

Reproduction of the decisive probe (read-only):

```clojure
(require '[futon2.aif.observation-checks :as c])
(let [d "- [x] Review with Joe — does the framing match the vision?"]
  [(c/decl-present? (str d " (approved 2026-03-04)") d)
   (c/decl-present?
    "- [x] Review with Joe (approved 2026-03-04) — does the framing match the vision?"
    d)])
;; => [true false]
```

### B. Drifting domains: 20 risk-bearing lines (15 ticked, 5 unticked)

Count a universal/completeness claim over an unpinned, extensible inventory of entities, modules, layers, tests, endpoints, checks, sessions, or queue items. This is a susceptibility count, not evidence that any particular set actually grew after ticking. Include “existing tests” with a historical total when the wording still ranges over whatever tests exist. Exclude universal runtime behavior such as “Any received message resets the liveness timer”: that specifies a rule rather than asserting a completed enumeration. Also exclude explicit bounded ID ranges such as Q1–Q8 or D1–D8.

Real examples: I266, M-futon1a-workplan: `- [x] Every module has PSR + PUR pair`; I267: `- [x] Every layer has traceability chain`. Unticked I039, M-f6-ingest: `- [ ] Embeddings computed for all entities`. A bounded counterexample is I082, M-self-documenting-stack: `- [x] Every survey question Q1-Q8 has a concrete answer.` Pin the inventory or name a durable scoped artifact; do not imply a box will be automatically unchecked when membership changes.

### C. Document-bound references: 178 lines (155 ticked, 23 unticked)

Count explicit §/Section/Part references, above/below, local phase subdivisions, acceptance/pre-check references without the criteria stated, or local Q/T/D/G/shape labels whose referent must be recovered elsewhere in the document. Do not count a repository-qualified file/API name merely because it is a reference. Some named principles and technical identifiers are self-contained enough to exclude. This is a line-level portability judgment, not a ban on source links.

I070, M-self-documenting-stack: `- [x] Session phylogeny produced as concrete artifact (the tree above)`. I633, M-vsatarcs-invariants-integration: `- [x] **Answer survey questions:** Q1-Q6 answered below.` I105: `- [x] Completion-criteria pre-check complete (5.c; no closure-bar-blocking gaps).` The local text may be perfectly adequate in its document; the extracted first line alone does not carry it. **75 ticked and 38 unticked lines also have an indented continuation**, an additional measured way that a standalone token loses information. Continuation is counted separately, not automatically treated as a defect.

### D. Numbers: distinguish results, targets, and structure

There are **95 reported numeric-result lines**, all ticked; **37 target/threshold lines**, 8 ticked and 29 unticked; and **16 fixed-structure/cardinality lines**, 11 ticked and 5 unticked. These disjoint quantity-role codes omit dates, hashes, version numbers, HTTP status codes, section numbers, and identifier ordinals. A line with both a threshold and a recorded actual result is assigned result (for example “10+ new tests … (16 new)”). This is a conservative semantic count of quantity-bearing statements, not a regex count of digits.

Retrospective I058, M-self-documenting-stack: `- [x] Inventory additions documented (5 ready / 4 missing beyond §2)`. The measured additions can remain a valid historical result as the corpus grows. Its number is not intrinsically a defect. Prospective I043, M-f6-ingest: `- [ ] Expanded NER kernel incorporates math.SE tags (expect 1000+ new terms)` is an expectation written into future work. I167, M-apm-solutions: `- [ ] 10+ problems run in sequence without intervention` is a deliberate acceptance threshold and may be entirely appropriate. Fixed-structure I342: `- [x] Define the three columns with examples` names the design’s three columns, not a live corpus size.

A reported result and a target are not interchangeable. Of the 37 target lines, **29 are still open**, so their quantities are visibly prospective; the 8 ticked target lines retain requirement wording and are not silently recoded as measured inventories. Do not infer that all 37 are unstable or wrong. Actual drift of an advance count is not established by this snapshot. For a growing inventory, reference a pinned enumeration instead; keep actual execution totals in evidence prose.

## Convention a drafting packet can reuse

The evidence supports a choice of useful registers, not a universal fact-assertion mandate. The 55 checked imperatives include concrete “Name”, “Define”, and “Implement” items; the 275 checked assertions include testable runtime behavior. The 95 counted retrospectives are a substantial documentation habit, but should not be copied blindly into future wants. The following is a recommendation constrained by these observations, not a measured causal claim about which style closes missions faster:

> Append an initial `### tl;dr` at EOF with a short set of witnessable outcomes already named by IDENTIFY. Aim for roughly five items; use fewer when less is known, and append independent items as work is discovered. Do not claim the list is exhaustive.
>
> Use a bounded task imperative (“Define …”, “Implement …”, “Record …”) or a testable fact assertion, whichever names the deliverable more directly. Do not use a bare topic heading or unanswered question as the completion claim.
>
> Put the mission/domain and the scoped deliverable in the checkbox-bearing line itself. Avoid “above”, “below”, local section/shape numbers, and unnamed acceptance criteria. Put explanations and evidence on separate lines.
>
> Keep the checkbox text stable when ticking: change only `[ ]` to `[x]`. Put dates, approval notes, measured totals, and receipt links below it. The present C4 matcher tolerates some appended suffixes, but cannot interpret qualifications and does not tolerate edits within the declaration.
>
> Avoid universal completion claims over unpinned growing sets. Use a named bounded inventory or deliverable instead. Keep deliberate acceptance thresholds or fixed design cardinalities when they are part of the work; do not substitute guessed future inventory sizes for them.
>
> Use `- [ ] ` for new items. A later item gets its own line; adding it must not require rewriting older witness text.

Count basis for each clause: (1) across the 35 source documents, 139 contiguous checkbox runs (allowing indented continuations, breaking at other lines) have median **5**, range **1–19**; **101/139** contain at most six and **44** are singletons. These are local checklist runs, not pre-existing tl;dr sections; “roughly five” is a conservative design choice, not an inferred law. The 150 remaining unchecked lines alongside 510 ticks, including 34 unticked in strictly complete missions, argue against requiring author-time completeness or treating a status as proof that every possible item is covered. (2) Imperative/assertion support is 55/275; the alternative fragment/question habit accounts for 65/20 ticks and often fails to name a standalone outcome. (3) Standalone wording addresses the 178 document-bound and 113 continued lines. (4) Stable ticking addresses 130 result-like annotation candidates and the real C4 probe above. (5) Scoped quantifiers address 20 risks while respecting the 95/37/16 distinction between result counts, targets, and structure. (6) The checkbox syntax is the executable `observable-hole?` contract; incremental growth preserves independent literal declarations rather than forcing old text to change.

## Audit ledger and source snapshot

Each extracted physical line follows below with a unique ID, mission, source line, population(s), register, and indicator codes. This ledger is the full classification, not only the worked sample. Codes A/B/C/D-result/D-target/D-fixed/continuation have the definitions above; `—` means no coded indicator. Unflagged does not mean verified safe. The manifest hashes bind the read files; no mission document was edited. Re-run the registry scan to discover sources, not a filesystem glob. The extraction recipe is: registry entries → read each path → match checkbox regex → retain missions having a tick → classify each retained item.

| Mission | Registry path | Status class | File SHA-256 | Repo HEAD at read |

| --- | --- | --- | --- | --- |

| M-aif2 | futon2/holes/M-aif2.md | complete | 4f1b89f6a7c7a5a8b556b151b8995f9b8b5bc05a49915a8f5eb48da43258f660 | 1ed5006eae14ffaabed7d3dd2e0a8b5f7c06bb4e |

| M-aif-head | futon2/holes/M-aif-head.md | complete | b9d7da93cb8c6a57477c8aed5531aa3f8f87de91af9c9f1d960f5b409da1fc87 | 1ed5006eae14ffaabed7d3dd2e0a8b5f7c06bb4e |

| M-f6-ingest | futon3/holes/missions/M-f6-ingest.md | inactive | f9fa23ca55c739b26e31b1fe9729f78738f45292afa61691b71e29e222f9ef37 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-daily-scan | futon7/holes/missions/M-daily-scan.md | identify | 95276c559d4773f3dd051744c3582abc4c20018ad2154f9cb4f69d5728d3f41a | c5b72885e99ef4a08a90b875ba7b6fe8e03bd934 |

| M-codex-parity | futon3/holes/missions/M-codex-parity.md | inactive | 802209f702d90c596e362ecb932427066e80406efd6ec1a5538a9917465c5a27 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-self-documenting-stack | futon7/holes/M-self-documenting-stack.md | active | 519f224565564981d2a0780f3d20c5c7542f26be9e9ef229cba99ee3c3c580d5 | c5b72885e99ef4a08a90b875ba7b6fe8e03bd934 |

| M-fucodex-parity | futon3/holes/missions/M-fucodex-parity.md | inactive | a05cb14ca742b7326195b3c7c319e7e0fe8670f332f14972bc11445c2c16a328 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-IRC-stability | futon3c/holes/missions/M-IRC-stability.md | complete | 00270da50f0b66ffa7ee5ae6ea54b3c618a3f524885852ad5b876d6bdcb8d063 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-apm-solutions | futon3c/holes/missions/M-apm-solutions.md | inactive | 5bd9129c17fd02baaea568c6293f2f362465df0a51e035cde4f49ac3a74be9e7 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-futon1a-rebuild | futon3/holes/missions/M-futon1a-rebuild.md | inactive | ac18a4250f16d1fa4adbfef63a4e705ce163449b5b39a287e549713553cf15f7 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-social-exotype | futon3c/holes/missions/M-social-exotype.md | inactive | 4bd465e66b1d0abb0e59dc026c697b4b4fe89c2d1fb8fb4ad47e205dbcfb7ffa | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-structural-law | futon3c/holes/missions/M-structural-law.md | inactive | 4062af0596d83ec60ff84428d443e80ca99e1613a65f40e3f04f044d07dca11c | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-vsatarcs-writer | futon4/holes/missions/M-vsatarcs-writer.md | active | 7a16db264d867e43d465d635009b0216fc1ad5862ec65d8ab76dc26395f2cd69 | 5e4501fdf79d0229bb7ab528cf31d76e738f9db3 |

| M-futon1a-workplan | futon3/holes/missions/M-futon1a-workplan.md | inactive | a17be66437852e1b7992529b0f711d5df0051e16aaafc80bb1200c19379b0b9b | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-labs-integration | futon3/holes/missions/M-labs-integration.md | complete | c15a5471b52878ed3424734852575e951ce083a84950e83e11b6422fcc4aebb2 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-agency-refactor | futon3c/holes/missions/M-agency-refactor.md | inactive | f8732076670115bb232786027e1a9717ca5ccb24264a8fcfd70c35e3a44715b7 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-mission-control | futon3c/holes/missions/M-mission-control.md | complete | 9b130b4fc72651990c70cdc863151b23760f867b5e3275093d041b390ce79789 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-peripheral-model | futon3c/holes/missions/M-peripheral-model.md | inactive | 40cbb32ab65c62761f97e6c7749406d4000e7a93bf2d6d968d896050bac68ed0 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-three-column-stack | futon4/holes/missions/M-three-column-stack.md | complete | a98496e185a8b12f992b423e771070c24cdccc8efce037786f745bbcd7cedd20 | 5e4501fdf79d0229bb7ab528cf31d76e738f9db3 |

| M-mission-peripheral | futon3c/holes/missions/M-mission-peripheral.md | inactive | e2bb9133decd523620385cccfcfdc48d8cc3eeedee403a0eb2e8acfe701f4492 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-transport-adapters | futon3c/holes/missions/M-transport-adapters.md | inactive | b1a8e4053e759389da7afcb6dc03d84077ac673e82c613f7728c36efb68cd91d | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-futonzero-capability | futon0/holes/missions/M-futonzero-capability.md | complete | 8ce2b2563a59da9de973f83b4ba742e15499d53290b7b2f5212285552c1cf2e6 | f872e8f48f819a69c6cea33985358929aa1eb9bf |

| M-futonzero-generative | futon0/holes/missions/M-futonzero-generative.md | open | bd197bf02abd109a2c818ffb3f4b6097080016628cb57ef96bfa1ebb8aa00b3f | f872e8f48f819a69c6cea33985358929aa1eb9bf |

| M-codex-irc-execution | futon3c/holes/missions/M-codex-irc-execution.md | complete | 01c9b1cc199713d5690d8093ea1a49be92edf8a2de5da5d5eaf7b2a1f3fb7aa9 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-portfolio-inference | futon3c/holes/missions/M-portfolio-inference.md | complete | d857265628652955943aafaac34db1805522e6ec2504b808a4baf763164327b4 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-native-plan-coherence | futon3/holes/missions/M-native-plan-coherence.md | inactive | 785d35a64cf347fe6f0240786f031dbcdcf6caa775a44426c9d2a173e92ed0c4 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-apm-capability-ratchet | futon0/holes/missions/M-apm-capability-ratchet.md | active | f38a55efacbf286d32890d7cdca0740a255519f88bd06009335097de67d0034e | f872e8f48f819a69c6cea33985358929aa1eb9bf |

| M-action-cost-modelling | futon3c/holes/missions/M-action-cost-modelling.md | open | b545fc982855744a341d60a490be6cbbd6a8a1e3c2dcd897bc7959f470c504e9 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-par-session-punctuation | futon3/holes/missions/M-par-session-punctuation.md | complete | ce65c80c21e6a6711dfb24778dac73d68bdaccec2f3d172b9e413d9291fc183f | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-self-representing-stack | futon4/holes/missions/M-self-representing-stack.md | complete | 7c992576165826be98457560478d4429288d60b42b68fc22b845cbc67209aa04 | 5e4501fdf79d0229bb7ab528cf31d76e738f9db3 |

| M-arxana-graph-persistence | futon3/holes/missions/M-arxana-graph-persistence.md | inactive | 0d29c06d67362ffd1688965c6ebc418bae850866c81b22845f09cd111ebe6e51 | 7fc6a05004aa3e601e53eba633d62213da0af4b2 |

| M-distributed-frontiermath | futon6/holes/missions/M-distributed-frontiermath.md | unknown | be93f65092f4d339888641882dd4e490e81dd4bfcae7f5180b142798ac82a092 | e10347810d5ad2c4343da50e244bc7de68d42dfd |

| M-peripheral-phenomenology | futon3c/holes/missions/M-peripheral-phenomenology.md | inactive | 6b1ece0f3169ee0c11aeaa3e4f043caf2457c3e1e2fe14ea156110269956374c | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-dispatch-peripheral-bridge | futon3c/holes/missions/M-dispatch-peripheral-bridge.md | inactive | 879a6ad6123a5015131bffafeefe22425ef69946e9465a615570b14025fd8bf1 | 3c038bbab9cbdfee270f3a6de6fbf5f93b8e5143 |

| M-vsatarcs-invariants-integration | futon4/holes/missions/M-vsatarcs-invariants-integration.md | active | 4b5d609e35303cd779e117429223e85389c4741f892595efea9630303c5a4aa1 | 5e4501fdf79d0229bb7ab528cf31d76e738f9db3 |



<details>
<summary>Full ledger: 660 checkbox-bearing lines</summary>


### M-aif2

Status line: **CLOSED on its own criteria — DELIVERED 2026-06-02** (Joe-agreed Option A; see §8 CLOSURE). All 7 IDENTIFY criteria met (R11 ruled `fired-weak`; T4 five-source classification recorded); slice-1 (tension-proposer) + β (playful-precision) built + green; **slice-1 live-installed** in `judge` consuming the delivered E1 curvature. Was: IDENTIFY→VERIFY→INSTANTIATE under Campaign `C-substrate-completion` (E1 consumer). **Named residue = Campaign RUN/DELIVER, NOT M-aif2:** the beats-baseline value-proof (#5) + E2/E3/differentiable signal-upgrades behind the logged seam (§8). The basic `aif` model stays frozen + honest.

Population: Closed. Ticked: 8; unticked: 6.


I001 · line 119 · fact-assertion · —

```text
- [ ] A `.aif.edn` meta-model of `futon2.aif.*` exists and carries an I1–I6 audit table (PASS/PARTIAL/FAIL with `path:line` evidence) per the AIF+ Layer-2 method.
```


I002 · line 120 · fact-assertion · D-target

```text
- [ ] The `endogenous-niche-construction-criterion` shape is recorded with ≥2 sibling instances adopted as namespace IDs, OR `:special-case true` recorded with reason.
```


I003 · line 121 · fact-assertion · D-target

```text
- [ ] At least one sibling criterion is expressed as a *checkable* proposition against `futon2.aif.*` (not merely prose), and is a mechanism (B) — learning/installing a priority — not (A) plumbing.
```


I004 · line 122 · fact-assertion · C

```text
- [ ] R11's stance is re-graded against the mission-doc shared-substrate finding (below), or explicitly held with a stated reason.
```


I005 · line 123 · fact-assertion · C

```text
- [ ] T2 resolved: a recorded decision on whether `aif2` is a sibling library or an in-place extension, with the Bayesian model-reduction rationale.
```


I006 · line 124 · fact-assertion · C, D-fixed

```text
- [ ] T4 addressed: each of the five sources classified (B installable-priority / A backlog-only / prerequisite), with reasons.
```


I007 · line 125 · fact-assertion · A

```text
- [x] `niche-construction.flexiarg` carries the corrected (scoped) conclusion + the endogenous-niche `HOWEVER` block. *(Landed 2026-05-31.)*
```


I008 · line 629 · counted-retrospective · A, D-result

```text
- [x] **`.aif.edn` meta-model + I1–I6 audit** — `aif2-exotype.edn`, 8/8 `ct.mission/validate`.
```


I009 · line 630 · counted-retrospective · A, D-result

```text
- [x] **niche-construction shape + ≥2 siblings** — 3: `inventory-as-hidden-state`, `boundary-redraw-admissibility`, `playful-precision-prior`.
```


I010 · line 631 · fact-assertion · A

```text
- [x] **≥1 checkable (B) proposition** — EXCEEDED: slice-1 (tension-proposer) + β (playful-precision), both built + green; slice-1 live-installed in `judge`.
```


I011 · line 632 · fact-assertion · A, C

```text
- [x] **T2 resolved** — `futon2.aif2.*` in-place, `aif` frozen (Bayesian model-reduction).
```


I012 · line 633 · fact-assertion · A

```text
- [x] **niche-construction.flexiarg corrected** — landed 2026-05-31.
```


I013 · line 634 · fact-assertion · A

```text
- [x] **R11 re-graded (RULING, 2026-06-02):** the documented re-eval trigger ("VSATARCS gains writer capability over the same substrate the WM reads from") **HAS FIRED — weakly.** `:mission-doc-sync` writes `**Status:**`/checkpoint markers to the `M-*.md` docs the WM's `mission-registry` (v0.18) reads to populate `:open-mission`. So R11's `N/A` is re-graded to **`fired-weak`: uncoordinated action on shared FILE-STATE (not shared belief)** — a real but partial firing via the mission-doc substrate (not the belief-level coupling R11 originally contemplated). The flip M-aif2 was chartered to *examine* is examined and ruled: N/A → fired-weak.
```


I014 · line 635 · counted-retrospective · C, D-result

```text
- [x] **T4 — five sources classified:**
```


### M-aif-head

Status line: COMPLETE (2026-03-15)

Population: Closed. Ticked: 10; unticked: 0.


I015 · line 307 · question · C, continuation

```text
- [x] **Q1:** What is the current interface between the Mission Peripheral
```


I016 · line 322 · question · C

```text
- [x] **Q2:** What does the ants AIF head actually look like in code?
```


I017 · line 337 · question · C, continuation

```text
- [x] **Q3:** How is the structural law inventory best represented for
```


I018 · line 349 · question · C, continuation

```text
- [x] **Q4:** Where in the cycle engine are the natural interception
```


I019 · line 368 · question · C

```text
- [x] **Q5:** What prediction is available at `:propose` vs `:execute`?
```


I020 · line 379 · question · C

```text
- [x] **Q6:** What refusal mechanisms already exist?
```


I021 · line 393 · question · C

```text
- [x] **Q7:** What is the current "end of cycle" code path?
```


I022 · line 406 · question · C

```text
- [x] **Q8:** Can candidate families load as core.logic relations?
```


I023 · line 415 · artifact-or-topic-fragment · C

```text
- [x] **Q9:** Wiring diagram: Mission Peripheral as AIF+ agent.
```


I024 · line 442 · question · C

```text
- [x] **Q10:** What would a minimal "I'm sorry, Joe" look like?
```


### M-f6-ingest

Status line: archived

Population: Other. Ticked: 5; unticked: 15.


I025 · line 34 · fact-assertion · A

```text
- [x] Streaming XML parser handles multi-GB dumps (tested on physics.SE)
```


I026 · line 35 · counted-retrospective · D-result

```text
- [x] NER kernel: 19,236 terms from 63 PlanetMath MSC repos + SE tags
```


I027 · line 36 · artifact-or-topic-fragment · D-fixed

```text
- [x] Superpod batch job: scripts/superpod-job.py (4-stage, self-contained)
```


I028 · line 37 · counted-retrospective · D-result

```text
- [x] Pattern tagger: 25 math-informal patterns with hotword lists
```


I029 · line 38 · artifact-or-topic-fragment · —

```text
- [x] Classical term spotter: scripts/spot-terms.bb
```


I030 · line 39 · fact-assertion · —

```text
- [ ] Stage 5 integrated into superpod-job.py (NER + scope detection)
```


I031 · line 40 · fact-assertion · D-fixed

```text
- [ ] math.stackexchange dump (3.4GB) accessible
```


I032 · line 41 · fact-assertion · —

```text
- [ ] Rob confirms superpod availability and Llama-3 access
```


I033 · line 132 · fact-assertion · D-target

```text
- [ ] Stage 5 NER term spotting produces `ner-terms.json` with >95% entity coverage
```


I034 · line 133 · fact-assertion · D-target

```text
- [ ] Stage 5 scope detection finds Let/Define openers in >30% of answers
```


I035 · line 134 · fact-assertion · —

```text
- [ ] Output manifest.json is well-formed and matches expected entity count
```


I036 · line 135 · fact-assertion · D-target

```text
- [ ] Pipeline completes in <10 minutes on local hardware
```


I037 · line 198 · fact-assertion · D-target

```text
- [ ] math.SE dump fully parsed (expect 200K+ QA pairs)
```


I038 · line 199 · fact-assertion · D-target

```text
- [ ] All 25 patterns fire on math.SE data
```


I039 · line 200 · fact-assertion · B

```text
- [ ] Embeddings computed for all entities
```


I040 · line 201 · fact-assertion · D-target

```text
- [ ] NER term spotting achieves >95% entity coverage
```


I041 · line 202 · fact-assertion · D-target

```text
- [ ] Scope detection finds Let/Define openers in >30% of answers
```


I042 · line 203 · fact-assertion · —

```text
- [ ] Output blob downloads and validates locally
```


I043 · line 204 · fact-assertion · D-target

```text
- [ ] Expanded NER kernel incorporates math.SE tags (expect 1000+ new terms)
```


I044 · line 205 · fact-assertion · D-target

```text
- [ ] Total pipeline completes within 4-hour superpod window
```


### M-daily-scan

Status line: IDENTIFY

Population: Other. Ticked: 5; unticked: 2.


I045 · line 360 · counted-retrospective · A, D-result

```text
- [x] **Removed** 4 stale fellowship-candidate probes from `~/code/futon7/data/probes/` (apollo-research-org.edn, leanprover-org.edn, leanprover-community-org.edn, metr-org.edn). data/probes/ now empty.
```


I046 · line 361 · fact-assertion · C

```text
- [x] **Rewrote** `f7.probes/xtdb-ecosystem` query. Old: `xtdb OR crux-db OR "temporal database" OR "bitemporality" stars:>30 pushed:>2024-01-01` (OR-chain pulling in adjacent-DBs). New: `xtdb language:Clojure stars:>5 pushed:>2024-01-01` (language-filter narrows to Joe's stack-relevant audience). Constraint: GitHub repo-search can't filter by dep-coordinate; the `com.xtdb in:file filename:deps.edn` form is code-search-only. The repo-search rewrite is the tactical fix that fits the existing apparatus; a code-search-probe-type extension is the deeper fix and is logged as open follow-on (see below).
```


I047 · line 362 · artifact-or-topic-fragment · continuation

```text
- [ ] **Partial audit** of other static probes for named-vs-queried misalignment:
```


I048 · line 366 · counted-retrospective · A, D-result

```text
- [x] **Open follow-on (Day 6+)**: extend `f7.probes` apparatus to support a code-search probe-type — landed `f7.gh/search-code` + `:probe-type :code-search` dispatch in `run-probe` (backward-compatible) + concrete `xtdb-deps` probe (`com.xtdb in:file filename:deps.edn`); offline tests 3/8/0, clj-kondo 0/0 (codex-1, futon7 `3b97d26`; claude-3 charter-fit + fable-1 code-gate). (2026-06-11, WM-pilot cycle 10)
```


I049 · line 367 · fact-assertion · A

```text
- [x] **Open follow-on (Day 6+)**: rewrite `pattern-rule-systems` per the named-vs-queried audit — **RESOLVED 無 (Joe, 2026-06-11)**: neither arm. "Business rules" is too generic; repeatedly scanning "Alexander patterns" wastes time too. The fork's premise dissolves the static-probe model: daily probes should be **fresh each day**, every term carrying (term ← source ← warrant) with evidence ledgered (the `f7.probe-gen` seam exists; the discipline doesn't). Superseded by the fresh-probes discipline; full resolution in `futon3c/holes/PILOT-ARC-2-QUEUE.md`. (2026-06-12, WM-pilot conforming-witness flight)
```


I050 · line 368 · counted-retrospective · A, D-result

```text
- [x] **Optional (low priority)**: fix brief Day-counter — replaced the binary `1`/`2+` placeholder with a real `day-number` helper (`daily.clj`; counts scans strictly before the date, +1), so the Nth frame on any axis renders "Day N". Lint-clean, ns loads, correct on the main axis (1 scan → Day 1); frame-005's `:lean`-axis data isn't present locally to re-render, but the root cause (the binary counter) is fixed. (2026-06-11, WM-pilot cycle 9)
```


I051 · line 369 · artifact-or-topic-fragment · —

```text
- [ ] **Optional (standing-task)**: extend brief render to include README excerpt / paper-link-presence per the surface-render-shallowness promotion.
```


### M-codex-parity

Status line: archived

Population: Other. Ticked: 5; unticked: 0.


I052 · line 159 · fact-assertion · A

```text
- [x] Codex's anchor appears in `lab/anchors/index.edn` (codex-parity-20260202T211454Z)
```


I053 · line 160 · fact-assertion · A

```text
- [x] Claude can query Codex's anchor via API (verified 2026-02-02)
```


I054 · line 161 · fact-assertion · A

```text
- [x] Cross-agent link exists in `lab/links/graph.edn` (verified via fucodex-parity-test)
```


I055 · line 162 · fact-assertion · A

```text
- [x] Codex's PAR appears in RAP results (par-9e255cc4, par-636b4a99)
```


I056 · line 163 · fact-assertion · A

```text
- [x] Both agents see consistent state (verified 2026-02-02)
```


### M-self-documenting-stack

Status line: INSTANTIATE (recovery pass 2026-05-21). **POC scope: LC1 (Deliverable C — mission-discoverability surface) only.** LC1 code/tests/build are now recovered after review; manual browser walk-through + DOCUMENT pass remain before closure. A + B + chain-link applications #3-#7 are follow-on missions inheriting this mission's shape-naming + protocol-family framing.

Population: Closed + Late. Ticked: 52; unticked: 1.


I057 · line 500 · counted-retrospective · D-result

```text
- [x] 6 new survey questions Q1bis-Q6bis have concrete answers
```


I058 · line 501 · counted-retrospective · A, C, D-result

```text
- [x] Inventory additions documented (5 ready / 4 missing beyond §2)
```


I059 · line 502 · fact-assertion · C

```text
- [x] Surprises S7-S11 documented (the R12-connection + Q6-debug-surface + 4-layer-positioning + consent-gate-substitutability-bridge)
```


I060 · line 503 · fact-assertion · C

```text
- [x] No design decisions made yet; design lives in DERIVE-bis
```


I061 · line 755 · counted-retrospective · A, C, D-result

```text
- [x] Entity types named (6 additions beyond §3; 7 typed slots schema; divergence/consumer-event/sub-graph; sonnet-pilot-query)
```


I062 · line 756 · counted-retrospective · A, D-result

```text
- [x] Relation types named (5 additions: typed-slot-projection; v1-v1.1-divergence; consumer-action; mission-slice-edge; confidence-from-agreement)
```


I063 · line 757 · counted-retrospective · A, C, D-result

```text
- [x] Invariant rules I7-I11 (5 new; covering typed slots, divergence emission, consumer events, missions-slice bounding, confidence monotonicity)
```


I064 · line 758 · fact-assertion · —

```text
- [x] Data flow extended (ingestion adds typed-slots.json; query path adds parallel v1/v1.1 + divergence-as-diagnostic; v2 adds missions-slice extraction; R12-trace path added)
```


I065 · line 759 · fact-assertion · —

```text
- [x] View/UI extended (V1.A.bis CLI flags; V1.B.bis chrome filter UI; V2.bis missions-slice topology; V3.new Sonnet-pilot API)
```


I066 · line 760 · fact-assertion · A, C

```text
- [x] Wiring diagram updated (textual; futon5 `.edn` deferred to VERIFY-bis)
```


I067 · line 761 · counted-retrospective · A, C, D-result

```text
- [x] IF/HOWEVER/THEN/BECAUSE for every non-obvious decision D9-D16 (8 new)
```


I068 · line 762 · fact-assertion · A

```text
- [x] Fidelity contract addressed (N/A; explained)
```


I069 · line 763 · fact-assertion · C

```text
- [x] T15-T19 resolved; T20 carried to VERIFY-bis
```


I070 · line 870 · fact-assertion · A, C

```text
- [x] Session phylogeny produced as concrete artifact (the tree above)
```


I071 · line 871 · counted-retrospective · A, D-result

```text
- [x] Edge-taxonomy named (9 phylogeny-edge classes)
```


I072 · line 872 · fact-assertion · —

```text
- [x] Mission Landscape concept named + placed (slice of substrate-2; LC1 v2 = first reader)
```


I073 · line 873 · counted-retrospective · D-result

```text
- [x] Three options for how it ships; Option γ recommended
```


I074 · line 874 · fact-assertion · C

```text
- [x] T21 added to carried-forward tensions
```


I075 · line 983 · counted-retrospective · A, C, D-result

```text
- [x] Pattern cross-reference: 3 NEW high-relevance patterns (P9, P10, P11) on top of §4's P1-P8; all directly applicable
```


I076 · line 984 · counted-retrospective · A, D-result

```text
- [x] Theoretical coherence: 3 new anchors validated (tri-store; state-snapshot-witness; pattern-receipts); no drift
```


I077 · line 985 · counted-retrospective · A, D-result

```text
- [x] Trade-off summary: 3 new trade-offs named
```


I078 · line 986 · counted-retrospective · A, D-result

```text
- [x] Generalization notes: 1 new axis added (LC1 as first instance of queryable-self-documentation-as-tri-store)
```


I079 · line 987 · counted-retrospective · A, D-result

```text
- [x] Plain-language argument: written; 5 sentences; no jargon; captures the expanded scope without sacrificing clarity (per Joe's constraint)
```


I080 · line 988 · counted-retrospective · A, C, D-result

```text
- [x] PSR catch-ups for D9-D16: 4 new PSRs (A5, A6, A7, A8); all grounded in P9-P11 plus §4 patterns
```


I081 · line 989 · fact-assertion · C

```text
- [x] DERIVE-bis revisions: **none required**. Pattern survey grounds + enriches the design without revising it.
```


I082 · line 1092 · fact-assertion · C

```text
- [x] Every survey question Q1-Q8 has a concrete answer.
```


I083 · line 1093 · counted-retrospective · A, D-result

```text
- [x] Ready-vs-missing table complete (10 ready / 9 missing).
```


I084 · line 1094 · fact-assertion · C

```text
- [x] Surprises documented (S1-S6).
```


I085 · line 1095 · fact-assertion · C

```text
- [x] No design decisions made yet; design lives in DERIVE.
```


I086 · line 1323 · counted-retrospective · A, D-result

```text
- [x] Entity types named with identity/source (7 types).
```


I087 · line 1324 · counted-retrospective · A, D-result

```text
- [x] Relation types named (6 relations).
```


I088 · line 1325 · fact-assertion · C

```text
- [x] Invariant rules expressed as checkable propositions (I1-I6).
```


I089 · line 1326 · fact-assertion · —

```text
- [x] Data flow specified end-to-end (ingestion + v1-query + v2-query).
```


I090 · line 1327 · fact-assertion · —

```text
- [x] View/UI specifications drafted (V1.A CLI + V1.B chrome + V2 webarxana).
```


I091 · line 1328 · fact-assertion · A

```text
- [x] Wiring diagram sketched (textual).
```


I092 · line 1329 · fact-assertion · C

```text
- [x] IF/HOWEVER/THEN/BECAUSE entries for every non-obvious decision (D1-D8).
```


I093 · line 1330 · fact-assertion · A

```text
- [x] Fidelity contract addressed (N/A; explained).
```


I094 · line 1331 · fact-assertion · C

```text
- [x] All in-scope carried-forward tensions resolved (T8-T14); out-of-scope (T1-T7) carried to follow-on.
```


I095 · line 1332 · fact-assertion · C

```text
- [ ] PSRs: no `futon3/library/` patterns explicitly selected (uses existing infrastructure patterns: futon3a-notions; d3-force; arxana-vsatarcs-chrome). PSRs to be written in ARGUE/INSTANTIATE if patterns surface during application.
```


I096 · line 1486 · counted-retrospective · A, D-result

```text
- [x] Pattern cross-reference: 8 high-relevance patterns (P1-P8) + 6 lesser-relevance noted; the cross-reference is genuinely substantive (Joe's hypothesis confirmed).
```


I097 · line 1487 · counted-retrospective · A, D-result

```text
- [x] Theoretical coherence checked against all 6 IDENTIFY anchors; no drift; patterns *enrich* (especially P5).
```


I098 · line 1488 · counted-retrospective · A, D-result

```text
- [x] Trade-off summary: 6 trade-offs named with rationale.
```


I099 · line 1489 · counted-retrospective · A, D-result

```text
- [x] Generalization notes: 3 axes (other doc classes / other stacks / spatial-presentation evolution).
```


I100 · line 1490 · counted-retrospective · A, D-result

```text
- [x] Plain-language argument written (4 sentences; no jargon).
```


I101 · line 1491 · counted-retrospective · A, C, D-result

```text
- [x] PSR catch-ups for 4 load-bearing decisions (PSR-A1 through A4).
```


I102 · line 1492 · fact-assertion · C

```text
- [x] DERIVE revisions: **none required**. Pattern survey grounds + enriches the design without revising it.
```


I103 · line 1651 · fact-assertion · A, C

```text
- [x] Structural verification done against textual wiring (5.a; all checks pass).
```


I104 · line 1652 · fact-assertion · A, C

```text
- [x] Spike run on the most-spike-able DERIVE commitment (5.b; reveals D3 refinement need).
```


I105 · line 1653 · fact-assertion · A, C

```text
- [x] Completion-criteria pre-check complete (5.c; no closure-bar-blocking gaps).
```


I106 · line 1654 · counted-retrospective · A, C, D-result

```text
- [x] Decision log recorded (5.d; six VERIFY-time discoveries DL1-DL6).
```


I107 · line 1655 · fact-assertion · A, C

```text
- [x] Fidelity check addressed (5.e; N/A documented).
```


I108 · line 1656 · fact-assertion · C

```text
- [x] PUR for the spike (5.f).
```


I109 · line 1657 · fact-assertion · C

```text
- [x] DERIVE revisions surfaced (DL1: D3 fallback chain) — to land in INSTANTIATE.
```


### M-fucodex-parity

Status line: archived

Population: Other. Ticked: 6; unticked: 0.


I110 · line 145 · fact-assertion · A

```text
- [x] `fucodex-peripheral.ts` starts without errors (2026-02-02)
```


I111 · line 146 · fact-assertion · A

```text
- [x] Fucodex creates session via MUSN (session: fucodex-parity-test)
```


I112 · line 147 · counted-retrospective · A, D-result

```text
- [x] Fucodex creates anchor that persists to `lab/anchors/index.edn` (2 anchors created)
```


I113 · line 148 · fact-assertion · A

```text
- [x] Fucodex creates cross-agent link to Claude's anchor (link-0596ad55, link-6e7d8191)
```


I114 · line 149 · fact-assertion · A

```text
- [x] Fucodex generates PAR visible in RAP (par-9e255cc4, par-636b4a99)
```


I115 · line 150 · fact-assertion · A, B

```text
- [x] Claude can query all artifacts created by fucodex (verified via /lab/anchors, /lab/links)
```


### M-IRC-stability

Status line: Complete (all 6 failure modes fixed, 16 stability tests, 2026-02-23)

Population: Closed. Ticked: 27; unticked: 0.


I116 · line 196 · fact-assertion · —

```text
- [x] Server sends PING to idle clients after :ping-interval-ms
```


I117 · line 197 · fact-assertion · —

```text
- [x] Client that doesn't PONG within :ping-timeout-ms is reaped
```


I118 · line 198 · fact-assertion · —

```text
- [x] Any received message resets the liveness timer (not just PONG)
```


I119 · line 199 · fact-assertion · —

```text
- [x] Socket read timeout is set (no indefinite .readLine blocks)
```


I120 · line 200 · fact-assertion · —

```text
- [x] SocketTimeoutException is handled gracefully (not treated as disconnect)
```


I121 · line 201 · fact-assertion · B

```text
- [x] All catch blocks log or emit evidence (no silent swallowing)
```


I122 · line 202 · counted-retrospective · D-result

```text
- [x] Existing 26 IRC tests still pass
```


I123 · line 203 · artifact-or-topic-fragment · —

```text
- [x] New tests for: PING sent after interval, reap after timeout, timeout doesn't kill active connection
```


I124 · line 246 · fact-assertion · —

```text
- [x] Reconnecting client reclaims nick from ghost connection
```


I125 · line 247 · fact-assertion · —

```text
- [x] Non-ghost nick collision returns ERR_NICKNAMEINUSE (433)
```


I126 · line 248 · fact-assertion · C

```text
- [x] Ghost detection uses liveness timer from Part I
```


I127 · line 249 · fact-assertion · —

```text
- [x] Relay timeout: stalled agent doesn't block other agents
```


I128 · line 250 · fact-assertion · —

```text
- [x] Relay timeout emits tension evidence
```


I129 · line 251 · fact-assertion · B

```text
- [x] Existing tests still pass
```


I130 · line 283 · fact-assertion · —

```text
- [x] stop-fn cancels all reader futures
```


I131 · line 284 · fact-assertion · —

```text
- [x] stop-fn stops keepalive loop
```


I132 · line 285 · fact-assertion · —

```text
- [x] No orphaned threads after shutdown
```


I133 · line 286 · fact-assertion · —

```text
- [x] `clojure -X:test` passes cleanly
```


I134 · line 287 · fact-assertion · B

```text
- [x] All existing transport tests unaffected
```


I135 · line 291 · fact-assertion · —

```text
- [x] Server-initiated PING/PONG detects dead connections within 90s
```


I136 · line 292 · fact-assertion · —

```text
- [x] No reader thread can block indefinitely (SO_TIMEOUT enforced)
```


I137 · line 293 · fact-assertion · —

```text
- [x] No exception is silently discarded anywhere in irc.clj
```


I138 · line 294 · fact-assertion · —

```text
- [x] Reconnecting clients reclaim their nick from ghost connections
```


I139 · line 295 · fact-assertion · D-fixed

```text
- [x] One slow agent cannot block IRC→agent relay for other agents
```


I140 · line 296 · fact-assertion · —

```text
- [x] Server shutdown is clean: all threads stopped, all sockets closed
```


I141 · line 297 · counted-retrospective · A, B, D-result

```text
- [x] All existing tests pass; 10+ new tests for stability behavior (16 new)
```


I142 · line 298 · fact-assertion · —

```text
- [x] `clojure -X:test` passes cleanly
```


### M-apm-solutions

Status line: parked

Population: Other. Ticked: 9; unticked: 18.


I143 · line 270 · counted-retrospective · D-result

```text
- [x] All 489 files parse
```


I144 · line 271 · artifact-or-topic-fragment · —

```text
- [x] Manifest includes subject, year, session, number for each
```


I145 · line 272 · fact-assertion · —

```text
- [x] Sub-parts detected (b03J02 has (a)/(b); m96A04 has (a)/(b)/(c))
```


I146 · line 273 · fact-assertion · —

```text
- [x] Canary produces output (even if most subsystems are stubbed)
```


I147 · line 394 · fact-assertion · D-target

```text
- [ ] All four canary problems: terms spotted and checked
```


I148 · line 395 · fact-assertion · —

```text
- [ ] Coverage reports list relevant terms per subject area
```


I149 · line 396 · fact-assertion · D-target

```text
- [ ] At least one missing/incomplete definition identified and flagged
```


I150 · line 397 · fact-assertion · —

```text
- [ ] Canary includes PlanetMath coverage data
```


I151 · line 616 · fact-assertion · D-target

```text
- [ ] All four canaries complete full cycle
```


I152 · line 617 · fact-assertion · —

```text
- [ ] Frame receipts emitted with `proof/problem-id: "apm-<id>"`
```


I153 · line 618 · fact-assertion · —

```text
- [ ] Sub-part DAGs handled (b03J02: (b) depends on (a))
```


I154 · line 619 · fact-assertion · —

```text
- [ ] Failed routes recorded per canary
```


I155 · line 620 · fact-assertion · —

```text
- [ ] Canary uses real proof peripheral (not stub)
```


I156 · line 879 · fact-assertion · D-target

```text
- [ ] Namespaced ArSE questions emitted during all four canary proofs
```


I157 · line 880 · fact-assertion · —

```text
- [ ] Questions are pedagogically useful (why hard, what's the crux, connections)
```


I158 · line 881 · fact-assertion · —

```text
- [ ] `GET /api/alpha/arse/unanswered?namespace=apm/topology` works
```


I159 · line 882 · fact-assertion · —

```text
- [ ] Canary right column shows real ArSE questions
```


I160 · line 1087 · fact-assertion · —

```text
- [x] Proof evidence ingested into Arxana
```


I161 · line 1088 · fact-assertion · —

```text
- [x] Scholia link proof steps to discipline annotations
```


I162 · line 1089 · fact-assertion · D-fixed

```text
- [x] Two-column LaTeX exported and compiles
```


I163 · line 1090 · fact-assertion · —

```text
- [x] Graph contains ArSE threads + PlanetMath refs as scholia
```


I164 · line 1091 · fact-assertion · —

```text
- [x] Canary output is a real Arxana projection, not a static .tex
```


I165 · line 1234 · fact-assertion · —

```text
- [ ] Manifest loaded into task queue
```


I166 · line 1235 · fact-assertion · —

```text
- [ ] Subject/year filtering works
```


I167 · line 1236 · fact-assertion · D-target

```text
- [ ] 10+ problems run in sequence without intervention
```


I168 · line 1237 · fact-assertion · —

```text
- [ ] Resumable after interruption
```


I169 · line 1238 · fact-assertion · —

```text
- [ ] Canaries run as first items in batch
```


### M-futon1a-rebuild

Status line: archived

Population: Other. Ticked: 22; unticked: 5.


I170 · line 867 · artifact-or-topic-fragment · —

```text
- [x] Evidence document from futon1 git history
```


I171 · line 868 · artifact-or-topic-fragment · B

```text
- [x] PSR/PUR records for each coding session
```


I172 · line 869 · fact-assertion · —

```text
- [x] Module headers explain pattern and rationale
```


I173 · line 870 · fact-assertion · —

```text
- [x] README teaches architecture, not just usage
```


I174 · line 871 · artifact-or-topic-fragment · —

```text
- [x] Traceability from devmap → pattern → code → test → doc
```


I175 · line 874 · counted-retrospective · A, D-result

```text
- [x] All 5 core invariants pass their proof tests (Prototype 0, 82 tests)
```


I176 · line 875 · fact-assertion · B

```text
- [x] Each invariant traces to both futon-theory/ and storage/ patterns
```


I177 · line 876 · counted-retrospective · D-result

```text
- [x] All 9 tension resolutions implemented and tested
```


I178 · line 877 · fact-assertion · B

```text
- [x] Interface loops defined at each layer boundary with PSR/PUR governance
```


I179 · line 878 · fact-assertion · C

```text
- [x] Canonical HTTP API specified in Section 2.6 (endpoints, shapes, error contract)
```


I180 · line 879 · fact-assertion · A

```text
- [x] API serves over HTTP with system context injection (Prototype 1, f8beb11)
```


I181 · line 880 · artifact-or-topic-fragment · A

```text
- [x] Read path: entity by UUID (`GET /entity/:id`) (Prototype 1, f8beb11)
```


I182 · line 881 · artifact-or-topic-fragment · —

```text
- [x] Read path: lookup by external-id (`GET /entity?source=S&external-id=E`)
```


I183 · line 882 · fact-assertion · —

```text
- [x] Proof-path emitted on canonical pipeline writes (`run-write!` / `run-open-world!`)
```


I184 · line 883 · fact-assertion · A, B

```text
- [x] Proof-path event logging on all write operations (including aux/compat endpoints) — `futon1a` commit `0ee5a9e`
```


I185 · line 884 · fact-assertion · —

```text
- [x] Counter-ratchet invariant function + tests exist
```


I186 · line 885 · fact-assertion · A

```text
- [x] Counter-ratchet wired as a global write-path guard (not only repair/verify paths) — `futon1a` commit `479a14c`
```


I187 · line 886 · fact-assertion · D-target

```text
- [ ] Any bug diagnosable in under 10 minutes
```


I188 · line 887 · fact-assertion · C

```text
- [x] Model descriptor system rebuilt (Section 2.11) — rich descriptors, type registry
```


I189 · line 888 · fact-assertion · C

```text
- [x] Meta/model API serves descriptors and runs verify (Section 2.11.4)
```


I190 · line 889 · counted-retrospective · A, C, D-result

```text
- [x] futon1's 6 model descriptors ingested as first dataset (Section 2.11.5)
```


I191 · line 890 · counted-retrospective · A, D-result

```text
- [x] Migration from futon1 succeeds without data loss (17564 docs, checksum match)
```


I192 · line 891 · fact-assertion · D-target

```text
- [ ] futon1a runs in production for 30 days without silent failures
```


I193 · line 892 · fact-assertion · —

```text
- [x] Docbook API parity: `/api/alpha/docs/:book/*` implemented in futon1a (required by futon4 docbook sync/checkout)
```


I194 · line 931 · task-imperative · D-target

```text
- [ ] Cite at least one futon-theory axiom (A1-A5) or invariant (I0-I4)
```


I195 · line 932 · task-imperative · —

```text
- [ ] Reference a tension resolution from git evidence
```


I196 · line 933 · task-imperative · —

```text
- [ ] Trace: theory pattern → domain pattern → code module → test
```


### M-social-exotype

Status line: archived

Population: Other. Ticked: 17; unticked: 8.


I197 · line 74 · fact-assertion · —

```text
- [x] Social ARGUMENT complete (`library/social/ARGUMENT.flexiarg`)
```


I198 · line 75 · counted-retrospective · D-result

```text
- [x] Three social patterns written (`library/social/*.flexiarg`)
```


I199 · line 76 · counted-retrospective · A, D-result

```text
- [x] Coordination exotype validated (8/8 checks)
```


I200 · line 77 · counted-retrospective · A, C, D-result

```text
- [x] futon3b Part III complete (L1 glacial loop operational, 31 tests)
```


I201 · line 78 · fact-assertion · D-fixed

```text
- [x] Devmap updated for three-futon split
```


I202 · line 79 · fact-assertion · —

```text
- [ ] futon3a diagram exists (or is written as part of this mission)
```


I203 · line 297 · fact-assertion · —

```text
- [x] social-exotype.edn written with ports, components, edges
```


I204 · line 298 · counted-retrospective · A, D-result

```text
- [x] Standalone validation: 8/8 ct/mission.clj checks pass
```


I205 · line 299 · fact-assertion · C

```text
- [x] All ARGUMENT requirements (R1-R10) traceable to diagram elements
```


I206 · line 300 · artifact-or-topic-fragment · —

```text
- [ ] Reviewer sign-off
```


I207 · line 303 · fact-assertion · —

```text
- [x] `compose-parallel` (or equivalent) implemented in ct/mission.clj
```


I208 · line 304 · fact-assertion · —

```text
- [x] Cross-diagram I3 check implemented and passes
```


I209 · line 305 · fact-assertion · —

```text
- [x] Cross-diagram I4 check implemented and passes
```


I210 · line 306 · fact-assertion · —

```text
- [x] Cross-diagram I6 check implemented and passes
```


I211 · line 307 · fact-assertion · —

```text
- [x] Shared-port consistency validated (I-patterns same across diagrams)
```


I212 · line 308 · counted-retrospective · A, D-result

```text
- [x] Three-diagram composition test passes (6 tests, 12 assertions)
```


I213 · line 313 · fact-assertion · —

```text
- [x] M-social-exotype registered in futon3b mission store
```


I214 · line 314 · fact-assertion · —

```text
- [x] REPL helper submits a task and receives proof-path
```


I215 · line 315 · fact-assertion · —

```text
- [x] Proof-path contains typed PSR, PUR, PAR records
```


I216 · line 318 · fact-assertion · B, C

```text
- [ ] Every diagram component cites its ARGUMENT requirement (R1-R10)
```


I217 · line 319 · fact-assertion · B, C

```text
- [ ] Every cross-diagram check cites its Chapter 0 invariant (I1-I6)
```


I218 · line 320 · task-imperative · —

```text
- [ ] Trace: ARGUMENT → exotype port/component → ct/mission check → validation output
```


I219 · line 378 · fact-assertion · —

```text
- [ ] Social-timescale components expressible in existing edn format
```


I220 · line 379 · fact-assertion · —

```text
- [ ] Social→task boundary encodeable as typed edges
```


I221 · line 380 · fact-assertion · —

```text
- [ ] I3 and I4 hold for social timescale alone (no cross-diagram needed yet)
```


### M-structural-law

Status line: parked

Population: Other. Ticked: 12; unticked: 0.


I222 · line 895 · counted-retrospective · D-result, continuation

```text
- [x] Meta-invariant set enumerated against 5+ live domains, with firm
```


I223 · line 897 · fact-assertion · —

```text
- [x] DERIVE decisions recorded with explicit IF/HOWEVER/THEN/BECAUSE support
```


I224 · line 898 · fact-assertion · continuation

```text
- [x] Pattern ancestry, trade-offs, generalization notes, and plain-language
```


I225 · line 903 · fact-assertion · —

```text
- [x] `structural_law.clj` provides reusable combinators with direct tests
```


I226 · line 904 · fact-assertion · continuation

```text
- [x] `mission_logic.clj` exists and is demonstrably a thin extraction of the
```


I227 · line 906 · fact-assertion · D-target, continuation

```text
- [x] At least one existing domain logic file is refactored to use combinators
```


I228 · line 908 · counted-retrospective · D-result, continuation

```text
- [x] The six current families cover all six logic layers in view, or any
```


I229 · line 910 · fact-assertion · —

```text
- [x] A load-profile-aware runner handles dormant domains as dormant, not clean
```


I230 · line 914 · fact-assertion · —

```text
- [x] Violations classified by actionability
```


I231 · line 915 · fact-assertion · —

```text
- [x] Aggregate invariant runner exists and can run active domains against live state
```


I232 · line 916 · fact-assertion · —

```text
- [x] FM conductor dispatches work from invariant violations
```


I233 · line 917 · fact-assertion · —

```text
- [x] Structural law violations navigable in Arxana (Column 2 integration)
```


### M-vsatarcs-writer

Status line: INSTANTIATE (started 2026-05-20)

Population: Late. Ticked: 26; unticked: 0.


I234 · line 259 · fact-assertion · C

```text
- [x] Every survey question Q1-Q8 has a concrete answer (above).
```


I235 · line 260 · counted-retrospective · A, D-result

```text
- [x] Ready-vs-missing table is complete (10 ready / 10 missing).
```


I236 · line 261 · fact-assertion · C

```text
- [x] Surprises documented (S1-S6).
```


I237 · line 262 · fact-assertion · C

```text
- [x] No design decisions made yet; design lives in DERIVE.
```


I238 · line 510 · counted-retrospective · A, D-result

```text
- [x] Entity types named with identity/source (8 types).
```


I239 · line 511 · counted-retrospective · A, D-result

```text
- [x] Relation types named (10 relations).
```


I240 · line 512 · fact-assertion · C

```text
- [x] Invariant rules expressed as checkable propositions (I1-I7).
```


I241 · line 513 · fact-assertion · —

```text
- [x] Data flow specified end-to-end with ports + timescales.
```


I242 · line 514 · fact-assertion · —

```text
- [x] View/UI specifications drafted (V1-V3).
```


I243 · line 515 · fact-assertion · A

```text
- [x] Wiring diagram sketched (textual; futon5 exotype `.edn` form deferred to VERIFY).
```


I244 · line 516 · fact-assertion · C

```text
- [x] IF/HOWEVER/THEN/BECAUSE entries for every non-obvious decision (D1-D6).
```


I245 · line 517 · fact-assertion · A

```text
- [x] Fidelity contract addressed (N/A; explained).
```


I246 · line 518 · counted-retrospective · C, D-result

```text
- [x] All six carried-forward tensions resolved (T1-T6).
```


I247 · line 670 · counted-retrospective · A, D-result

```text
- [x] Pattern cross-reference complete: 4 high-relevance patterns (P1-P4) + 5 lesser-relevance noted.
```


I248 · line 671 · counted-retrospective · A, D-result

```text
- [x] Theoretical coherence checked against all 6 IDENTIFY anchors; no drift.
```


I249 · line 672 · counted-retrospective · A, D-result

```text
- [x] Trade-off summary: 6 trade-offs named with rationale.
```


I250 · line 673 · counted-retrospective · A, D-result

```text
- [x] Generalization notes: 3 axes (action-class / reader-agent / supervised-autonomous).
```


I251 · line 674 · counted-retrospective · A, D-result

```text
- [x] Plain-language argument written (4 sentences; no jargon).
```


I252 · line 675 · counted-retrospective · A, C, D-result

```text
- [x] PSR catch-ups for 3 load-bearing decisions (PSR-A1 / A2 / A3).
```


I253 · line 676 · fact-assertion · C

```text
- [x] DERIVE revisions surfaced (R-A1 / A2 / A3) for VERIFY integration.
```


I254 · line 814 · fact-assertion · A, C

```text
- [x] Structural verification done against textual wiring sketch (5.c).
```


I255 · line 815 · fact-assertion · A, C

```text
- [x] DERIVE revisions integrated (5.a; R-A1/A2/A3 specified as deltas).
```


I256 · line 816 · fact-assertion · A, C

```text
- [x] Dogfooding gate specified per Joe's 2026-05-20 cue (5.b).
```


I257 · line 817 · fact-assertion · A, C

```text
- [x] Completion-criteria pre-check complete (5.d; no blocking gaps).
```


I258 · line 818 · counted-retrospective · A, C, D-result

```text
- [x] Decision log recorded (5.e; six VERIFY-time discoveries DL1-DL6).
```


I259 · line 819 · fact-assertion · A, C

```text
- [x] Fidelity check addressed (5.f; N/A documented).
```


### M-futon1a-workplan

Status line: archived

Population: Other. Ticked: 7; unticked: 10.


I260 · line 219 · artifact-or-topic-fragment · —

```text
- [ ] Module code with pattern references in header
```


I261 · line 220 · artifact-or-topic-fragment · —

```text
- [ ] PSR documenting pattern selection
```


I262 · line 221 · artifact-or-topic-fragment · —

```text
- [ ] Layer tests (from 2.7.1 phases)
```


I263 · line 222 · artifact-or-topic-fragment · —

```text
- [ ] PUR documenting outcome
```


I264 · line 223 · artifact-or-topic-fragment · —

```text
- [ ] Error catalog entries
```


I265 · line 224 · artifact-or-topic-fragment · —

```text
- [ ] Invariant catalog entries
```


I266 · line 272 · fact-assertion · B

```text
- [x] Every module has PSR + PUR pair
```


I267 · line 273 · fact-assertion · B

```text
- [x] Every layer has traceability chain
```


I268 · line 274 · fact-assertion · B, C

```text
- [ ] No orphaned PSRs (all have matching PURs) — re-audit after F1-F3
```


I269 · line 275 · fact-assertion · C

```text
- [ ] Ping-pong handoffs documented for finish phases F1-F4
```


I270 · line 278 · counted-retrospective · D-result

```text
- [x] All 5 invariants pass proof tests
```


I271 · line 279 · fact-assertion · D-target

```text
- [ ] Any bug diagnosable in < 10 minutes
```


I272 · line 280 · fact-assertion · —

```text
- [x] Migration succeeds without data loss
```


I273 · line 281 · fact-assertion · D-target

```text
- [ ] 30 days production without silent failures
```


I274 · line 282 · fact-assertion · —

```text
- [x] Docbook API parity complete and verified by futon4 workflows
```


I275 · line 283 · fact-assertion · B

```text
- [x] All mutating endpoints emit proof-path (`:path/id`)
```


I276 · line 284 · fact-assertion · —

```text
- [x] Counter-ratchet is globally enforced on protected write classes
```


### M-labs-integration

Status line: COMPLETE (SUPERSEDED) — Lab capture reframed as evidence landscape in futon3c; evidence store + thread projection replace Arxana overlays (2026-03)

Population: Closed. Ticked: 2; unticked: 13.


I277 · line 81 · fact-assertion · continuation

```text
- [x] **PARs appearing at end of lab timeline** - The `futon4-lab-bridge.el` wasn't
```


I278 · line 86 · fact-assertion · continuation

```text
- [x] **PAR sequence counter bug** - `create-par!` was reading `(:events entry)`
```


I279 · line 92 · artifact-or-topic-fragment · continuation

```text
- [ ] **No validation before submission** - PARs can be submitted without all 5
```


I280 · line 96 · artifact-or-topic-fragment · continuation

```text
- [ ] **Fragile response parsing** - Agency response structure is nested
```


I281 · line 100 · artifact-or-topic-fragment · continuation

```text
- [ ] **Sidecar path resolution** - Falls back gracefully but may create `.par.edn`
```


I282 · line 106 · artifact-or-topic-fragment · continuation

```text
- [ ] **Overlay highlighting hardcoded buffer** - `arxana-hop-to-link` hardcodes
```


I283 · line 110 · artifact-or-topic-fragment · continuation

```text
- [ ] **Lab files browser navigation** - `arxana-browser-lab-browse-files` doesn't
```


I284 · line 114 · artifact-or-topic-fragment · continuation

```text
- [ ] **Enrichment async error handling** - If enrichment request times out,
```


I285 · line 118 · artifact-or-topic-fragment · continuation

```text
- [ ] **Session deduplication** - docs/guides/README-lab.md mentions dedupe needed but
```


I286 · line 123 · task-imperative · continuation

```text
- [ ] **Add affect/transition to lab timeline** - Render affect events alongside
```


I287 · line 126 · artifact-or-topic-fragment · continuation

```text
- [ ] **Affect overlay in session viewer** - Show trigger text, affect type, and
```


I288 · line 129 · artifact-or-topic-fragment · continuation

```text
- [ ] **Filter/query by affect** - "Show sessions with :joy affects" or
```


I289 · line 132 · artifact-or-topic-fragment · —

```text
- [ ] **Affect heatmap view** - Density of affect types across days/weeks
```


I290 · line 182 · artifact-or-topic-fragment · continuation

```text
- [ ] **Overlay persistence** - Overlays created dynamically but not saved/restored;
```


I291 · line 185 · artifact-or-topic-fragment · continuation

```text
- [ ] **Pattern proposal integration** - PSR/PUR data not mined for pattern
```


### M-agency-refactor

Status line: archived

Population: Other. Ticked: 23; unticked: 1.


I292 · line 77 · fact-assertion · —

```text
- [x] Shape-validated: input is AgentConnection, output is PresenceRecord|SocialError
```


I293 · line 78 · artifact-or-topic-fragment · —

```text
- [x] R7 (rendezvous-handshake): connection + explicit readiness = presence
```


I294 · line 79 · fact-assertion · —

```text
- [x] Connection → presence pipeline boundary validated
```


I295 · line 80 · counted-retrospective · A, D-result

```text
- [x] 6 tests pass (5+ met)
```


I296 · line 81 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I297 · line 97 · artifact-or-topic-fragment · —

```text
- [x] R6 (typed identifiers): transport ID → typed agent identity
```


I298 · line 98 · artifact-or-topic-fragment · —

```text
- [x] Identity → registry capability lookup
```


I299 · line 99 · fact-assertion · —

```text
- [x] Input is PresenceRecord, output is AgentIdentity|SocialError
```


I300 · line 100 · counted-retrospective · A, D-result

```text
- [x] 6 tests pass (5+ met)
```


I301 · line 101 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I302 · line 122 · fact-assertion · —

```text
- [x] R1 (delivery receipt): every message produces receipt or explicit failure
```


I303 · line 123 · fact-assertion · D-fixed

```text
- [x] R2 (single routing authority): one authority per agent-id
```


I304 · line 124 · fact-assertion · —

```text
- [x] Input includes ClassifiedMessage, output is DispatchReceipt|SocialError
```


I305 · line 125 · counted-retrospective · A, D-result

```text
- [x] 7 tests pass (5+ met)
```


I306 · line 126 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I307 · line 154 · fact-assertion · D-fixed

```text
- [x] R8 (authoritative transcript): one transcript is authoritative
```


I308 · line 155 · fact-assertion · —

```text
- [x] R9 (structured events): events are structured, machine-parseable
```


I309 · line 156 · fact-assertion · —

```text
- [x] Input flows from dispatch, output is SessionRecord|SocialError
```


I310 · line 157 · counted-retrospective · A, D-result

```text
- [x] 5+ tests pass (7 tests)
```


I311 · line 158 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I312 · line 181 · task-imperative · —

```text
- [x] Wire S-presence → S-authenticate → S-dispatch → S-persist
```


I313 · line 182 · counted-retrospective · A, D-result

```text
- [x] Pipeline integration test passes end-to-end (8 tests, 117 total)
```


I314 · line 183 · fact-assertion · D-target

```text
- [ ] At least one proof-path from gate pipeline submission (bootstrap.clj pattern)
```


I315 · line 184 · fact-assertion · C

```text
- [x] All R1-R11 invariant tests pass
```


### M-mission-control

Status line: Complete (2026-02-26)

Population: Closed. Ticked: 7; unticked: 0.


I316 · line 1085 · fact-assertion · A, C

```text
- [x] D1: Simpler peripheral — COMMITTED, justified (A2), verified (←)
```


I317 · line 1086 · fact-assertion · A, C

```text
- [x] D2: Hybrid scanning — COMMITTED, justified (A4), verified (real data)
```


I318 · line 1087 · fact-assertion · A, C

```text
- [x] D3: Read-only devmaps — COMMITTED, justified (A5), verified (real data)
```


I319 · line 1088 · fact-assertion · A, C

```text
- [x] D4: Query-only mana — COMMITTED, justified (A5), verified (G2 gap documented)
```


I320 · line 1089 · fact-assertion · A, C

```text
- [x] D5: Portfolio state shape — COMMITTED, justified (A8, A9), verified (evidence invariants)
```


I321 · line 1090 · fact-assertion · A, C

```text
- [x] D6: Filesystem + reimpl — COMMITTED, justified (A7), verified (real data)
```


I322 · line 1091 · counted-retrospective · A, C, D-result

```text
- [x] D7: Backfill legacy missions — IMPLEMENTED, tested (3 INSTANTIATE tests)
```


### M-peripheral-model

Status line: archived

Population: Other. Ticked: 18; unticked: 0.


I323 · line 112 · fact-assertion · —

```text
- [x] R10 (mode-gate): coordination and action are distinguishable
```


I324 · line 113 · fact-assertion · —

```text
- [x] Mode transitions validated: DISCUSS → DIAGNOSE → EXECUTE
```


I325 · line 114 · fact-assertion · —

```text
- [x] EXECUTE requires approval token or explicit approval
```


I326 · line 115 · fact-assertion · —

```text
- [x] Input conforms to shapes, output is ClassifiedMessage|ModeTransition|SocialError
```


I327 · line 116 · counted-retrospective · A, D-result

```text
- [x] 7 tests pass (5+ met)
```


I328 · line 117 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I329 · line 144 · counted-retrospective · D-result

```text
- [x] Five core peripherals load and validate against PeripheralSpec shape
```


I330 · line 145 · fact-assertion · —

```text
- [x] Hop validation: only allowed transitions succeed (explore → edit, edit → test, etc.)
```


I331 · line 146 · fact-assertion · —

```text
- [x] Session-id preserved across hops
```


I332 · line 147 · fact-assertion · —

```text
- [x] Entry conditions checked before allowing peripheral entry
```


I333 · line 148 · counted-retrospective · A, D-result

```text
- [x] 7 tests pass (5+ met)
```


I334 · line 149 · fact-assertion · —

```text
- [x] No EXPECTED FAIL markers
```


I335 · line 178 · fact-assertion · —

```text
- [x] Mode classification feeds into dispatch pipeline (ClassifiedMessage shape)
```


I336 · line 179 · fact-assertion · —

```text
- [x] Peripheral hop preserves session-id end-to-end
```


I337 · line 180 · fact-assertion · —

```text
- [x] Mode transition state machine prevents invalid paths
```


I338 · line 181 · artifact-or-topic-fragment · —

```text
- [x] Integration with S-presence → S-authenticate → S-mode pipeline segment
```


I339 · line 182 · task-imperative · continuation

```text
- [x] Decide: enforce mandatory summary on EXECUTE → DISCUSS transitions?
```


I340 · line 186 · task-imperative · continuation

```text
- [x] Decide: should classify use patterns for heuristic enrichment?
```


### M-three-column-stack

Status line: COMPLETE (2026-03-04)

Population: Closed. Ticked: 44; unticked: 0.


I341 · line 262 · task-imperative · —

```text
- [x] Name the gap and motivate the mission
```


I342 · line 263 · task-imperative · D-fixed

```text
- [x] Define the three columns with examples
```


I343 · line 264 · task-imperative · —

```text
- [x] Specify cross-column invariants (examples per column pair)
```


I344 · line 265 · task-imperative · —

```text
- [x] State the generalization path (futon → any Clojure → any project)
```


I345 · line 266 · artifact-or-topic-fragment · —

```text
- [x] Scope in/out
```


I346 · line 267 · task-imperative · A

```text
- [x] Review with Joe — does the framing match the vision? (approved 2026-03-04)
```


I347 · line 268 · task-imperative · continuation

```text
- [x] Identify any missing columns or invariant categories
```


I348 · line 274 · task-imperative · C, continuation

```text
- [x] Confirm completion criteria are testable and sufficient
```


I349 · line 283 · question · C, continuation

```text
- [x] **Q1 (math column schema):**
```


I350 · line 299 · question · C, continuation

```text
- [x] **Q2 (code column schema):**
```


I351 · line 313 · question · C, continuation

```text
- [x] **Q3 (project column inventory):**
```


I352 · line 327 · question · C, continuation

```text
- [x] **Q4 (core.logic relations):**
```


I353 · line 341 · question · C, continuation

```text
- [x] **Q5 (browser migration):**
```


I354 · line 352 · question · C, continuation

```text
- [x] **Q6 (implicit invariants):**
```


I355 · line 375 · artifact-or-topic-fragment · C

```text
- [x] **MAP summary:**
```


I356 · line 417 · counted-retrospective · C, D-result, continuation

```text
- [x] Entity type table (§3.1 — 4 math, 6 project, 4 code entity types
```


I357 · line 419 · counted-retrospective · C, D-result, continuation

```text
- [x] Binary relation types (§3.2 — 6 within-math, 4 within-project,
```


I358 · line 421 · counted-retrospective · C, D-result

```text
- [x] Hyperedge types (§3.3 — 5 existing + 4 new n-ary types)
```


I359 · line 422 · counted-retrospective · C, D-result, continuation

```text
- [x] Invariant rules (§3.4 — 5 invariants: INV-1 through INV-5, spanning
```


I360 · line 425 · artifact-or-topic-fragment · C, continuation

```text
- [x] Data flow (§3.5 — ASCII diagram + ingestion paths table, all
```


I361 · line 427 · counted-retrospective · C, D-result, continuation

```text
- [x] Browser view specifications (§3.6 — 5 views: trace, hypergraph,
```


I362 · line 674 · question · C

```text
- [x] Why three columns and not two or four (§4.1)
```


I363 · line 675 · question · C

```text
- [x] Why AIF+ invariants (I1–I6) are the right structural language (§4.2)
```


I364 · line 676 · question · C

```text
- [x] Why cross-column invariants are the product (not the data) (§4.3)
```


I365 · line 677 · question · C

```text
- [x] How wiring diagrams generalize beyond futon (§4.4)
```


I366 · line 678 · fact-assertion · C

```text
- [x] The monograph's thesis: "post-hoc → real-time" is a tooling gap (§4.5)
```


I367 · line 679 · artifact-or-topic-fragment · C

```text
- [x] Pattern cross-reference from futon3/library (§4.6)
```


I368 · line 680 · task-imperative · C

```text
- [x] Clarify AIF dual usage (§4.7)
```


I369 · line 681 · artifact-or-topic-fragment · C

```text
- [x] Plain-language argument (§4.8)
```


I370 · line 1010 · fact-assertion · A, continuation

```text
- [x] Math JSON → futon1a round-trip (write + read-back confirmed)
```


I371 · line 1013 · artifact-or-topic-fragment · continuation

```text
- [x] Code column entities (ns/var/dep snapshots) in futon1a
```


I372 · line 1017 · fact-assertion · continuation

```text
- [x] Project column re-ingestion idempotent and repeatable
```


I373 · line 1021 · artifact-or-topic-fragment · D-target, continuation

```text
- [x] ≥200 hyperedges across all three columns
```


I374 · line 1023 · artifact-or-topic-fragment · D-target, continuation

```text
- [x] 3+ cross-column invariants in core.logic, generating tensions
```


I375 · line 1029 · fact-assertion · continuation

```text
- [x] Invariant violations visible in trace browser or equivalent
```


I376 · line 1051 · artifact-or-topic-fragment · continuation

```text
- [x] Project↔Code loop: undocumented entry point → tension → add docstring →
```


I377 · line 1053 · artifact-or-topic-fragment · continuation

```text
- [x] Math↔Math loop: ungrounded definition → tension → add iatc reference →
```


I378 · line 1055 · artifact-or-topic-fragment · continuation

```text
- [x] Project↔Math loop: tension linked to math reasoning via cross-column
```


I379 · line 1057 · task-imperative · C

```text
- [x] Write up the demo as a reproducible walkthrough (below)
```


I380 · line 1183 · counted-retrospective · A, D-result, continuation

```text
- [x] **Docbook entries:** Created 3 new futon3x entries:
```


I381 · line 1190 · fact-assertion · A, C, continuation

```text
- [x] **Cross-references:** Updated chapter root (`futon3x-78da12c2e512`) to
```


I382 · line 1193 · fact-assertion · A, continuation

```text
- [x] **Browser integration:** Verified navigation paths:
```


I383 · line 1197 · fact-assertion · A, C, continuation

```text
- [x] **Deferred-item tickets:** Documented in INSTANTIATE deferred items
```


I384 · line 1199 · fact-assertion · A, C, D-fixed, continuation

```text
- [x] **Mission lifecycle updated:** Added DOCUMENT as phase 7 in
```


### M-mission-peripheral

Status line: archived

Population: Other. Ticked: 12; unticked: 3.


I385 · line 1257 · task-imperative · continuation

```text
- [x] Confirm that proof_dag.clj algorithms generalize to code missions
```


I386 · line 1262 · task-imperative · continuation

```text
- [x] Design code mission tool gates (what tools are available per phase)
```


I387 · line 1267 · task-imperative · continuation

```text
- [ ] Decide whether PSR/PUR skills become phase-specific or remain
```


I388 · line 1270 · task-imperative · continuation

```text
- [x] Design the auto-tagging scheme for Table 25 sigils (hardcoded
```


I389 · line 1274 · task-imperative · continuation

```text
- [ ] Decide granularity: one cycle per mission step, or one cycle
```


I390 · line 1276 · task-imperative · continuation

```text
- [x] Evaluate whether mission peripheral should be a 7th peripheral
```


I391 · line 1281 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From MAP:** Protocol vs configuration for cycle machine
```


I392 · line 1286 · question · C, continuation

```text
- [x] **From MAP:** Are the 9 phases the right decomposition for code
```


I393 · line 1292 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From MAP:** Code mission ItemStatus values.
```


I394 · line 1295 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From MAP:** Code mission EvidenceClass.
```


I395 · line 1299 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From ARGUE:** Configuration vs protocol justification.
```


I396 · line 1303 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From ARGUE:** 9-phase retention justification.
```


I397 · line 1307 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From ARGUE:** Table 25 auto-tag assignment justification.
```


I398 · line 1310 · artifact-or-topic-fragment · C, continuation

```text
- [x] **From ARGUE:** Evidence landscape integration — emit mission
```


I399 · line 1316 · artifact-or-topic-fragment · C, continuation

```text
- [ ] **From ARGUE:** Obligation-level evidence — obligations as
```


### M-transport-adapters

Status line: archived

Population: Other. Ticked: 34; unticked: 0.


I400 · line 292 · fact-assertion · —

```text
- [x] parse-dispatch-request produces valid ClassifiedMessage from well-formed JSON
```


I401 · line 293 · fact-assertion · —

```text
- [x] parse-dispatch-request returns SocialError for malformed/missing fields
```


I402 · line 294 · fact-assertion · —

```text
- [x] render-receipt produces valid JSON matching DispatchReceipt structure
```


I403 · line 295 · fact-assertion · —

```text
- [x] render-error maps :error/code to HTTP status codes
```


I404 · line 296 · fact-assertion · —

```text
- [x] extract-params works for both HTTP and WS upgrade requests (L3)
```


I405 · line 297 · fact-assertion · —

```text
- [x] extract-params falls back to :request-uri when :query-string is nil
```


I406 · line 298 · fact-assertion · —

```text
- [x] Round-trip: parse -> pipeline -> render preserves information
```


I407 · line 299 · counted-retrospective · A, D-result

```text
- [x] 31 tests (89 assertions)
```


I408 · line 339 · fact-assertion · —

```text
- [x] POST /dispatch with valid JSON -> 200 + DispatchReceipt JSON
```


I409 · line 340 · fact-assertion · —

```text
- [x] POST /dispatch with bad JSON -> 400 + SocialError JSON
```


I410 · line 341 · fact-assertion · —

```text
- [x] POST /dispatch to unknown agent -> 404
```


I411 · line 342 · fact-assertion · —

```text
- [x] POST /presence with readiness metadata -> 200 + PresenceRecord JSON
```


I412 · line 343 · fact-assertion · —

```text
- [x] GET /session/:id for existing session -> 200
```


I413 · line 344 · fact-assertion · —

```text
- [x] GET /session/:id for missing session -> 404
```


I414 · line 345 · fact-assertion · —

```text
- [x] GET /health returns agent/session counts
```


I415 · line 346 · fact-assertion · —

```text
- [x] Server startup verifies port is listening (L7)
```


I416 · line 347 · fact-assertion · —

```text
- [x] Graceful shutdown closes server and returns
```


I417 · line 348 · fact-assertion · —

```text
- [x] Content-Type is application/json on all responses
```


I418 · line 349 · counted-retrospective · A, D-result

```text
- [x] 15 tests (35 assertions)
```


I419 · line 397 · fact-assertion · —

```text
- [x] WS open without readiness handshake -> no messages processed (R7)
```


I420 · line 398 · fact-assertion · —

```text
- [x] Readiness handshake with valid agent -> ready_ack sent
```


I421 · line 399 · fact-assertion · —

```text
- [x] Readiness handshake with unknown agent -> error + close
```


I422 · line 400 · fact-assertion · —

```text
- [x] Message after handshake -> dispatched, receipt sent as frame
```


I423 · line 401 · fact-assertion · —

```text
- [x] Message before handshake -> error frame (not ready)
```


I424 · line 402 · fact-assertion · —

```text
- [x] on-close after successful handshake -> presence cleanup
```


I425 · line 403 · fact-assertion · —

```text
- [x] on-close before handshake (L2) -> no cleanup needed
```


I426 · line 404 · fact-assertion · —

```text
- [x] extract-params handles missing :query-string (L3)
```


I427 · line 405 · fact-assertion · —

```text
- [x] Channel state stored in map, not metadata (L4)
```


I428 · line 406 · artifact-or-topic-fragment · —

```text
- [x] Connection tracking: list connected agents with agent-id
```


I429 · line 407 · counted-retrospective · A, D-result

```text
- [x] 16 tests (34 assertions)
```


I430 · line 435 · counted-retrospective · D-result

```text
- [x] All 10 scenarios pass
```


I431 · line 436 · counted-retrospective · A, B, D-result

```text
- [x] All 342 existing tests still pass (352 total now)
```


I432 · line 437 · counted-retrospective · A, D-result

```text
- [x] `clojure -X:test` passes cleanly (352 tests, 1025 assertions, 0 failures)
```


I433 · line 438 · fact-assertion · —

```text
- [x] No transport logic leaks into pipeline (adapter is pure translation)
```


### M-futonzero-capability

Status line: COMPLETE (2026-03-04)

Population: Closed. Ticked: 19; unticked: 8.


I434 · line 155 · artifact-or-topic-fragment · continuation

```text
- [ ] **G5 Task Specification:** capability/functioning/conversion-factor
```


I435 · line 157 · artifact-or-topic-fragment · continuation

```text
- [ ] **G4 Agent Authorization:** observed agents and hyperedge sources are
```


I436 · line 159 · artifact-or-topic-fragment · continuation

```text
- [ ] **GF Fidelity Contract:** baseline capabilities and preserve/adapt/drop
```


I437 · line 161 · artifact-or-topic-fragment · continuation

```text
- [ ] **G3 Pattern Reference:** PSR pattern choices for observation engine and
```


I438 · line 163 · artifact-or-topic-fragment · —

```text
- [ ] **G2 Execution:** code + tests implemented in futon0
```


I439 · line 164 · artifact-or-topic-fragment · —

```text
- [ ] **GD Document:** mission updates + docbook entries
```


I440 · line 165 · artifact-or-topic-fragment · —

```text
- [ ] **G1 Validation:** acceptance tests and capability delta checks pass
```


I441 · line 166 · artifact-or-topic-fragment · —

```text
- [ ] **G0 Evidence Durability:** report artifacts and PAR notes persisted
```


I442 · line 1127 · counted-retrospective · A, D-result

```text
- [x] `agents` command lists all agents with evidence (12 found)
```


I443 · line 1128 · fact-assertion · —

```text
- [x] `observe` command produces FunctioningRecords from evidence entries
```


I444 · line 1129 · fact-assertion · continuation

```text
- [x] `profile` command computes CapabilityProfile with correct reproducibility
```


I445 · line 1131 · fact-assertion · —

```text
- [x] `profile` saves JSON snapshot to storage
```


I446 · line 1132 · fact-assertion · —

```text
- [x] `trajectory` computes delta between baseline and current windows
```


I447 · line 1133 · fact-assertion · —

```text
- [x] `trajectory` detects freedom expansion and regression
```


I448 · line 1134 · fact-assertion · A

```text
- [x] `trajectory` detects Baldwin phase (claude-1: mixed)
```


I449 · line 1135 · fact-assertion · —

```text
- [x] `trajectory` generates markdown report and saves to storage
```


I450 · line 1136 · fact-assertion · A

```text
- [x] Insufficient baseline handled correctly (rob --days 14: no baseline data)
```


I451 · line 1137 · fact-assertion · A

```text
- [x] Discipline profile reports availability flags (joe: PSR/PUR/PAR not available)
```


I452 · line 1138 · counted-retrospective · A, D-result

```text
- [x] Hyperedge observation reads futon1a EDN responses correctly (1,411 HX)
```


I453 · line 1139 · fact-assertion · A

```text
- [x] Read-only: no writes to futon1a or futon3c (D-I5 satisfied)
```


I454 · line 1356 · artifact-or-topic-fragment · C

```text
- [x] **GI Identify:** observable inventory, decisions D-I1–D-I5 (§8)
```


I455 · line 1357 · artifact-or-topic-fragment · C

```text
- [x] **GM Map:** Sen → FutonZero object mapping, constraints C1–C5 (§10)
```


I456 · line 1358 · counted-retrospective · C, D-result

```text
- [x] **GD Derive:** 7 computation rules D-1–D-7 (§12)
```


I457 · line 1359 · artifact-or-topic-fragment · C

```text
- [x] **GA Argue:** Sen justification, pattern cross-references (§14)
```


I458 · line 1360 · counted-retrospective · A, C, D-result

```text
- [x] **GV Verify:** 5 namespaces + entry point implemented (§16)
```


I459 · line 1361 · counted-retrospective · A, C, D-result

```text
- [x] **GN Instantiate:** 6 live demos against infrastructure (§18)
```


I460 · line 1362 · artifact-or-topic-fragment · C

```text
- [x] **GD Document:** docbook entries + mission update (§20)
```


### M-futonzero-generative

Status line: HEAD / IDENTIFY charter

Population: Other. Ticked: 5; unticked: 2.


I461 · line 197 · fact-assertion · A, C

```text
- [x] §4.1 Static rollout ledger — `futon0/data/futonzero-rollout-ledger.edn` (futon0 `2c820cd`, audit `ac4f7ad`, consolidation `fae1979`)
```


I462 · line 198 · fact-assertion · A, C

```text
- [x] §4.4 Calibration audit — `futon0/data/futonzero-calibration-report.edn` + canonical reader `futon3c.aif.calibration` (futon3c `976fb2c` + consolidation)
```


I463 · line 199 · counted-retrospective · A, C, D-result

```text
- [x] §4.2 Toy field fixture — `scripts/futon0/futonzero/toy_field.clj` + test (codex-1, futon0 b32b421; synthetic 4-state chain, demonstrates successful update + refusal to learn from laundered reward incl. operator-gate-is-not-fruit; 18 tests/88 assertions pass)
```


I464 · line 200 · fact-assertion · C

```text
- [x] §4.3 Policy/value vocabulary — `holes/missions/M-futonzero-generative.policy-value-vocabulary.edn` (v0, prototyping-forward; pattern-as-policy + value-estimate shapes, no actuator)
```


I465 · line 201 · counted-retrospective · A, C, D-result

```text
- [x] §4.5 Reward red-team fixture — `scripts/futon0/futonzero/reward_red_team.clj` + test (codex-1, futon0 `71b22d2`; charter-fit reviewed + code-gated). 8 laundering shapes from the WM-pilot arc-1 (censored-fallback, transient-spike, prior-value-tautology, operator-gate-as-fruit, non-independent/measured/witness, missing-return), each rejected with explicit routed reasons; legit case still accepted. 21 tests/127 assertions pass. **§4 safe work products now complete.** (2026-06-11, WM-pilot arc-2 cycle 11)
```


I466 · line 202 · artifact-or-topic-fragment · —

```text
- [ ] G-SIM clearance — field-simulator adequacy; first MEASURED calibration pairs now accruing via the WM pilot loop
```


I467 · line 203 · artifact-or-topic-fragment · —

```text
- [ ] G-REWARD clearance — anti-laundered reward; Pudding Prover G1 arrow-witness binding
```


### M-codex-irc-execution

Status line: DONE (2026-03-08)

Population: Closed. Ticked: 39; unticked: 0.


I468 · line 7 · artifact-or-topic-fragment · —

```text
- [x] **Motivation**
```


I469 · line 16 · artifact-or-topic-fragment · —

```text
- [x] **Theoretical anchoring**
```


I470 · line 22 · artifact-or-topic-fragment · —

```text
- [x] **Scope in/out**
```


I471 · line 35 · artifact-or-topic-fragment · C

```text
- [x] **Completion criteria**
```


I472 · line 42 · artifact-or-topic-fragment · —

```text
- [x] **Relationship to other missions**
```


I473 · line 52 · artifact-or-topic-fragment · —

```text
- [x] **Source material**
```


I474 · line 61 · artifact-or-topic-fragment · —

```text
- [x] **Owner and dependencies**
```


I475 · line 67 · task-imperative · —

```text
- [x] **Inventory existing infrastructure**
```


I476 · line 74 · task-imperative · —

```text
- [x] **Inventory existing data**
```


I477 · line 79 · task-imperative · —

```text
- [x] **Identify ready vs missing**
```


I478 · line 89 · task-imperative · C

```text
- [x] **Answer survey questions (Q1–Q7)**
```


I479 · line 112 · task-imperative · —

```text
- [x] **Document surprises**
```


I480 · line 119 · task-imperative · —

```text
- [x] Define job entity schema (`job-id`, state, timestamps, trace-id, evidence summary, artifact ref, delivery state).
```


I481 · line 120 · task-imperative · —

```text
- [x] Define state machine and legal transitions.
```


I482 · line 121 · task-imperative · —

```text
- [x] Define terminal-state immutability rule.
```


I483 · line 122 · task-imperative · —

```text
- [x] Define evidence gate semantics for mission/work completions.
```


I484 · line 123 · task-imperative · —

```text
- [x] Define promise/future semantics for accepted invokes.
```


I485 · line 124 · task-imperative · —

```text
- [x] Define `!job <id>` read model and output fields.
```


I486 · line 125 · task-imperative · —

```text
- [x] Define restart recovery semantics for queued/running jobs.
```


I487 · line 126 · task-imperative · —

```text
- [x] Record IF/HOWEVER/THEN/BECAUSE for key design choices.
```


I488 · line 255 · artifact-or-topic-fragment · —

```text
- [x] Pattern cross-reference against realtime and transport patterns.
```


I489 · line 256 · artifact-or-topic-fragment · C

```text
- [x] Coherence check against IDENTIFY theory and constraints.
```


I490 · line 257 · artifact-or-topic-fragment · —

```text
- [x] Trade-off summary (where enforcement lives, persistence backend, operational cost).
```


I491 · line 258 · artifact-or-topic-fragment · D-target

```text
- [x] Plain-language 3-5 sentence argument.
```


I492 · line 307 · task-imperative · —

```text
- [x] Implement job state machine and persistence.
```


I493 · line 308 · task-imperative · —

```text
- [x] Implement canonical `!job <id>` query.
```


I494 · line 309 · task-imperative · —

```text
- [x] Add integration tests for accepted->terminal invariants.
```


I495 · line 310 · task-imperative · —

```text
- [x] Add tests for no-evidence mission/work failure path.
```


I496 · line 311 · task-imperative · —

```text
- [x] Add restart/recovery tests.
```


I497 · line 312 · task-imperative · C

```text
- [x] Validate completion criteria with concrete evidence.
```


I498 · line 387 · task-imperative · —

```text
- [x] Run local end-to-end demo (API surface, assistant-driven).
```


I499 · line 388 · task-imperative · —

```text
- [x] Demonstrate terminal delivery recording with evidence.
```


I500 · line 389 · task-imperative · —

```text
- [x] Demonstrate no-evidence mission/work rejection.
```


I501 · line 390 · task-imperative · —

```text
- [x] Demonstrate restart and `!job` recovery path.
```


I502 · line 391 · task-imperative · —

```text
- [x] Run operator-driven IRC demo on live channel.
```


I503 · line 392 · task-imperative · —

```text
- [x] Append checkpoint with commands and artifacts.
```


I504 · line 501 · task-imperative · —

```text
- [x] Update README/docs with final execution contract and operator runbook.
```


I505 · line 502 · task-imperative · —

```text
- [x] Add cross-references from mission-control and IRC docs.
```


I506 · line 503 · task-imperative · —

```text
- [x] Document deferred follow-ons as candidate missions.
```


### M-portfolio-inference

Status line: DONE (TESTING)

Population: Closed. Ticked: 8; unticked: 1.


I507 · line 1041 · counted-retrospective · C, D-result

```text
- [x] All 6 Chapter 0 invariants satisfied at portfolio level
```


I508 · line 1042 · fact-assertion · —

```text
- [x] Full AIF loop operational: observe → perceive → affect → policy
```


I509 · line 1043 · artifact-or-topic-fragment · —

```text
- [x] core.logic relational layer with structural queries
```


I510 · line 1044 · fact-assertion · —

```text
- [x] Live test against real portfolio produces actionable recommendation
```


I511 · line 1045 · counted-retrospective · A, D-result

```text
- [x] 69 portfolio-specific tests passing (929 total suite)
```


I512 · line 1046 · counted-retrospective · C, D-result

```text
- [x] All 11 derivations (D-1 through D-11) instantiated in code
```


I513 · line 1047 · fact-assertion · A, C, continuation

```text
- [x] Evidence emission to durable store (D-10 — `:portfolio` added to ArtifactRefType;
```


I514 · line 1049 · artifact-or-topic-fragment · C, continuation

```text
- [x] Weekly heartbeat infrastructure (D-8 + D-11):
```


I515 · line 1054 · artifact-or-topic-fragment · —

```text
- [ ] First live weekly heartbeat cycle (bid Monday → clear Sunday → prediction error)
```


### M-native-plan-coherence

Status line: archived

Population: Other. Ticked: 3; unticked: 3.


I516 · line 132 · fact-assertion · —

```text
- [x] Native TaskCreate/TaskUpdate triggers `planning/native-detected` event
```


I517 · line 133 · fact-assertion · —

```text
- [x] Task list is converted to Mermaid diagram at turn end
```


I518 · line 134 · fact-assertion · —

```text
- [x] Plan appears in notebook viewer and WebSocket stream
```


I519 · line 135 · fact-assertion · —

```text
- [ ] Plan evaluation scores native plans (Phase 2)
```


I520 · line 136 · fact-assertion · —

```text
- [ ] HUD shows plan section when plan exists (Phase 3)
```


I521 · line 137 · fact-assertion · —

```text
- [ ] Codex plans are captured (Phase 4)
```


### M-apm-capability-ratchet

Status line: MAP — capability-topology map and evidence-observation substrate survey added 2026-07-22

Population: Other. Ticked: 3; unticked: 23.


I522 · line 672 · artifact-or-topic-fragment · continuation

```text
- [ ] **G5 — Capability specification:** packet semantics, status transitions,
```


I523 · line 674 · artifact-or-topic-fragment · continuation

```text
- [ ] **G4 — Evaluation authorization:** model/tool envelopes, accessible
```


I524 · line 676 · artifact-or-topic-fragment · continuation

```text
- [ ] **GF — Fidelity:** problem statements, formal theorem meanings, proof
```


I525 · line 678 · artifact-or-topic-fragment · continuation

```text
- [ ] **G3 — Pattern reference:** distillation names the proof/Mentor patterns
```


I526 · line 680 · artifact-or-topic-fragment · continuation

```text
- [ ] **G2 — Execution:** schema validator, first packet, projections, and
```


I527 · line 682 · artifact-or-topic-fragment · continuation

```text
- [ ] **GD — Documentation:** a human-readable packet and tutor view explain
```


I528 · line 684 · artifact-or-topic-fragment · D-fixed, continuation

```text
- [ ] **G1 — Validation:** sealed probes run in all three conditions and produce
```


I529 · line 686 · artifact-or-topic-fragment · continuation

```text
- [ ] **G0 — Evidence durability:** packet version, run manifests, outputs,
```


I530 · line 744 · task-imperative · D-fixed, continuation

```text
- [x] Locate the ratchet relative to the 14 accepted action-class zones and
```


I531 · line 746 · task-imperative · continuation

```text
- [x] Identify the existing capability-graph and
```


I532 · line 749 · task-imperative · continuation

```text
- [x] Survey the evidence-observation substrate: confirm that the zai invoke
```


I533 · line 754 · task-imperative · continuation

```text
- [ ] DERIVE `apm-capability-topology-map.v1`, including stable identities and
```


I534 · line 756 · task-imperative · D-target, continuation

```text
- [ ] Run one APM problem (e.g. `a94J05`) through the evidence-capturing zai
```


I535 · line 761 · task-imperative · continuation

```text
- [ ] Agree the mathematical adapter boundary with the capability-graph owner;
```


I536 · line 763 · task-imperative · continuation

```text
- [ ] Produce the first `a94J05` topology crosswalk using accepted `pca3-v1`
```


I537 · line 765 · task-imperative · continuation

```text
- [ ] Audit existing task/run/evidence schemas before defining packet and run
```


I538 · line 767 · task-imperative · continuation

```text
- [ ] DERIVE `mathematical-capability-packet.v1` and its status-transition
```


I539 · line 769 · task-imperative · —

```text
- [ ] Decide the initial APM-side packet path and cross-repo stable-ID shape.
```


I540 · line 770 · task-imperative · —

```text
- [ ] Implement a deterministic schema validator with negative fixtures.
```


I541 · line 771 · task-imperative · continuation

```text
- [ ] Distil the `a94J05` candidate packet from the checked bundle and proof
```


I542 · line 773 · task-imperative · continuation

```text
- [ ] Freeze and validate the first transfer probes and sealed reference
```


I543 · line 775 · task-imperative · —

```text
- [ ] Define scoring rubrics and promotion thresholds before evaluated runs.
```


I544 · line 776 · task-imperative · —

```text
- [ ] Implement baseline, raw-retrieval, and packet run manifests.
```


I545 · line 777 · task-imperative · —

```text
- [ ] Implement solver and tutor projections over the same packet version.
```


I546 · line 778 · task-imperative · —

```text
- [ ] Run the first comparison and publish the evidence-backed verdict.
```


I547 · line 779 · task-imperative · —

```text
- [ ] Register a capability-star-map node only if the verdict warrants it.
```


### M-action-cost-modelling

Status line: HEAD / IDENTIFY / MAP / DERIVE / ARGUE / VERIFY all drafted and operator-ratified through 2026-05-27. VERIFY's 10 carried-forward tensions: 4 done (T1, T5, T8, T10), 4 held for downstream INSTANTIATE (T2, T4, T6, T9 — all blocked on `E-substrate-2-sorry-typing.md`'s own INSTANTIATE which is a future cycle), 2 future-Joe-triggered (T3, T7). Mission ready for INSTANTIATE when Joe ratifies.

Population: Late. Ticked: 6; unticked: 6.


I548 · line 57 · fact-assertion · A

```text
- [x] Operator-voice anchor present (Joe's "premature to greenlight new work" quote)
```


I549 · line 58 · fact-assertion · —

```text
- [x] What's already felt to be true: WM math is correct; the gap is in action-representation, not the math
```


I550 · line 59 · fact-assertion · —

```text
- [x] Anti-glibness discipline: "the math is broken" framing rejected; the math is doing what it says it does
```


I551 · line 60 · fact-assertion · —

```text
- [x] Working-economy position: WM cannot be fit for pilot inhabitation if it presents minute-scale and months-scale work as equivalent
```


I552 · line 61 · fact-assertion · C

```text
- [x] Carried-forward tensions named (the YES/NO branch on M-live-geometric-stack)
```


I553 · line 62 · fact-assertion · A

```text
- [x] Provenance recorded (HEAD authored claude-1 2026-05-26 after `713c74d`)
```


I554 · line 110 · fact-assertion · B, C, D-target

```text
- [ ] **Cost-signal lands**: Each candidate action in the WM's ranked-actions carries a `:scale` (or equivalently expressive) annotation, sourceable from at least one of shape (a)/(b)/(c)
```


I555 · line 111 · fact-assertion · C

```text
- [ ] **Work-breakdown gate fires**: When the WM's top recommendation is `:multi-session` or larger, the live next-move-live tile renders a "needs work breakdown" CTA instead of "→ address-sorry X"
```


I556 · line 112 · fact-assertion · C

```text
- [ ] **Mission-or-Campaign decision is operator-visible**: The CTA either points to "this is a Mission" (existing futonic structure) or "this requires a Campaign" (new structure if we confirm it's needed; or the absence is operator-explicit)
```


I557 · line 113 · fact-assertion · C, D-target

```text
- [ ] **The live test case discriminates**: After the layer ships, the 5 currently-tied sorries no longer present as equivalent. At minimum, `:minute`/`:hour` ranks above `:campaign` regardless of EFE score
```


I558 · line 114 · fact-assertion · C, D-target

```text
- [ ] **M-live-geometric-stack decision recorded**: Either substrate-2 exposes per-action T-delta queries (shape (c) lands), OR a finding is filed on M-live-geometric-stack identifying the action-projection-query gap, with the 5-tied-sorries 2-orders-of-magnitude-cost-spread as the test case
```


I559 · line 115 · fact-assertion · C

```text
- [ ] **Campaign structure resolved**: Either ships as a new futon-stack structural element with documentation in `mission-lifecycle.md` (or its sibling), OR is explicitly rejected as "Missions suffice" with rationale
```


### M-par-session-punctuation

Status line: COMPLETE (SUPERSEDED) — PAR emission via futon3c peripheral/reflect.clj; detach/reattach via peripheral adapter model (2026-03)

Population: Closed. Ticked: 2; unticked: 6.


I560 · line 186 · fact-assertion · —

```text
- [x] Design sketched
```


I561 · line 187 · fact-assertion · —

```text
- [x] Peripheral detach/reattach model documented
```


I562 · line 188 · fact-assertion · —

```text
- [ ] MUSN event schema implemented
```


I563 · line 189 · artifact-or-topic-fragment · —

```text
- [ ] Forum relay logic
```


I564 · line 190 · artifact-or-topic-fragment · —

```text
- [ ] fuclient-logs PAR rendering
```


I565 · line 191 · artifact-or-topic-fragment · —

```text
- [ ] Agent prompt integration
```


I566 · line 192 · artifact-or-topic-fragment · —

```text
- [ ] Detach/reattach PAR types (`:par/detach`, `:par/reattach`)
```


I567 · line 193 · fact-assertion · —

```text
- [ ] First real PAR emitted and relayed
```


### M-self-representing-stack

Status line: COMPLETE (2026-03-04). Was RE-OPENED (2026-03-03); gaps closed by M-three-column-stack. See §Closure below.

Population: Closed. Ticked: 6; unticked: 0.


I568 · line 764 · fact-assertion · continuation

```text
- [x] Hyperedge creation round-trips through XTDB (store and retrieve)
```


I569 · line 766 · fact-assertion · continuation

```text
- [x] Tension browser surfaces known gaps from `mc-coverage`
```


I570 · line 768 · fact-assertion · continuation

```text
- [x] Narrative trail for a completed mission (e.g., M-mission-control) is
```


I571 · line 771 · fact-assertion · continuation

```text
- [x] Evidence timeline shows MC artifacts as linked structure, not flat list
```


I572 · line 773 · fact-assertion · C, D-target, continuation

```text
- [x] For at least one completed mission, strategic claims resolve to live
```


I573 · line 776 · fact-assertion · continuation

```text
- [x] Stale or unresolved reflection anchors are surfaced as tensions
```


### M-arxana-graph-persistence

Status line: archived

Population: Other. Ticked: 5; unticked: 0.


I574 · line 187 · fact-assertion · —

```text
- [x] Anchors can be recorded during MUSN turns
```


I575 · line 188 · fact-assertion · —

```text
- [x] Links can be created between anchors (same or cross-session)
```


I576 · line 189 · fact-assertion · —

```text
- [x] WebSocket stream includes anchor/link events
```


I577 · line 190 · fact-assertion · —

```text
- [x] Emacs can navigate links (forward and backward)
```


I578 · line 191 · fact-assertion · —

```text
- [x] Semantic similarity suggests links between anchors
```


### M-distributed-frontiermath

Status line: (absent)

Population: Other. Ticked: 5; unticked: 4.


I579 · line 204 · fact-assertion · —

```text
- [x] futon6 repo accessible to both codex-1 and zcodex (shared remote)
```


I580 · line 205 · fact-assertion · A

```text
- [x] FM-001 state file spec-locked (`spec_lock_status: pass`)
```


I581 · line 206 · fact-assertion · —

```text
- [x] FM-002, FM-003 also spec-locked (available for future missions)
```


I582 · line 207 · fact-assertion · —

```text
- [x] Multi-channel bridge: `#futon` + `#math` via single systemd unit
```


I583 · line 208 · fact-assertion · —

```text
- [x] claude, codex, claude-2, tickle all joined `#math` with clean nicks
```


I584 · line 209 · fact-assertion · —

```text
- [ ] claude-2 Mentor session started on workspace2 (REPL-driven, not auto-invoke)
```


I585 · line 210 · fact-assertion · —

```text
- [ ] zcodex reachable from `#math` (currently configured same as codex; may need Rob's bridge)
```


I586 · line 211 · fact-assertion · —

```text
- [ ] Rob has IRC access to `#math`
```


I587 · line 212 · fact-assertion · —

```text
- [ ] Tickle bell-driven orchestration wired for FM-001 phase transitions
```


### M-peripheral-phenomenology

Status line: archived

Population: Other. Ticked: 2; unticked: 15.


I588 · line 726 · artifact-or-topic-fragment · C

```text
- [ ] P-2 predicate: given a session start, verify context injection present
```


I589 · line 727 · artifact-or-topic-fragment · C, continuation

```text
- [ ] P-3 predicate: given a session transcript + peripheral spec, verify
```


I590 · line 729 · artifact-or-topic-fragment · C

```text
- [ ] P-4 predicate: given a session end, verify explicit exit transition
```


I591 · line 730 · artifact-or-topic-fragment · C

```text
- [ ] P-5 heuristic: given a transcript, score inhabitation evidence
```


I592 · line 731 · fact-assertion · C

```text
- [ ] P-1 and P-6 documented as architectural requirements with checklist
```


I593 · line 732 · fact-assertion · D-target

```text
- [ ] 8+ tests including positive (fuclaude-style) and negative (script-style)
```


I594 · line 787 · fact-assertion · D-fixed

```text
- [ ] render-context produces valid context for all 6 peripheral types
```


I595 · line 788 · fact-assertion · D-fixed

```text
- [ ] Context includes all 4 elements (location, state, actions, exits)
```


I596 · line 789 · fact-assertion · —

```text
- [ ] Context is parameterized (not hard-coded per peripheral)
```


I597 · line 790 · fact-assertion · —

```text
- [ ] Proof peripheral context renders current ledger state
```


I598 · line 791 · fact-assertion · C

```text
- [ ] Context passes P-2 predicate from Part I
```


I599 · line 792 · fact-assertion · D-target

```text
- [ ] 6+ tests (one per peripheral type)
```


I600 · line 842 · fact-assertion · C, D-target

```text
- [ ] At least one session transcript passes both ← and P-1..P-6
```


I601 · line 843 · fact-assertion · —

```text
- [ ] Transcript is reproducible (script or test, not ad-hoc)
```


I602 · line 844 · counted-retrospective · A, D-result

```text
- [x] Preliminary validation: alleycat race 20/20 (complete)
```


I603 · line 845 · fact-assertion · —

```text
- [x] PSR/PUR self-test: pattern carried and discharged in-session
```


I604 · line 846 · fact-assertion · C, continuation

```text
- [ ] If full game is blocked, the specific missing infrastructure is
```


### M-dispatch-peripheral-bridge

Status line: archived

Population: Other. Ticked: 25; unticked: 0.


I605 · line 265 · fact-assertion · —

```text
- [x] create-session produces shape-valid session records
```


I606 · line 266 · fact-assertion · —

```text
- [x] session-context builds correct peripheral start context
```


I607 · line 267 · fact-assertion · —

```text
- [x] close-session records fruit and evidence summary
```


I608 · line 268 · fact-assertion · —

```text
- [x] select-route uses `:msg/mode` to choose direct vs peripheral
```


I609 · line 269 · fact-assertion · —

```text
- [x] select-peripheral uses agent type with metadata override
```


I610 · line 270 · fact-assertion · —

```text
- [x] DispatchReceipt shape extended with optional session/peripheral/fruit fields
```


I611 · line 271 · fact-assertion · —

```text
- [x] Existing dispatch tests still pass (shape extension is additive)
```


I612 · line 272 · fact-assertion · D-target

```text
- [x] 8+ tests (session CRUD, route selection, peripheral selection)
```


I613 · line 330 · fact-assertion · —

```text
- [x] peripheral-dispatch creates session, runs peripheral, returns enriched receipt
```


I614 · line 331 · fact-assertion · —

```text
- [x] Root evidence entry emitted before peripheral starts
```


I615 · line 332 · fact-assertion · —

```text
- [x] Peripheral's start evidence has :in-reply-to pointing to dispatch root
```


I616 · line 333 · fact-assertion · —

```text
- [x] DispatchReceipt for action messages includes :receipt/session-id and :receipt/fruit
```


I617 · line 334 · fact-assertion · —

```text
- [x] DispatchReceipt for coordination messages unchanged (backwards compatible)
```


I618 · line 335 · fact-assertion · —

```text
- [x] Evidence store accumulates dispatch root + all peripheral evidence
```


I619 · line 336 · fact-assertion · —

```text
- [x] SocialError from peripheral propagates correctly through dispatch
```


I620 · line 337 · fact-assertion · D-target, continuation

```text
- [x] 8+ tests (peripheral dispatch happy path, error propagation, evidence linkage,
```


I621 · line 377 · fact-assertion · —

```text
- [x] Full pipeline → peripheral → receipt works end-to-end
```


I622 · line 378 · fact-assertion · —

```text
- [x] Coordination mode unchanged (regression test)
```


I623 · line 379 · fact-assertion · —

```text
- [x] Evidence thread spans dispatch + peripheral boundaries
```


I624 · line 380 · fact-assertion · —

```text
- [x] Proof-tree invariants hold on dispatch+peripheral thread
```


I625 · line 381 · fact-assertion · —

```text
- [x] Peripheral errors surface as dispatch SocialErrors
```


I626 · line 382 · fact-assertion · —

```text
- [x] ← round-trip passes on dispatched peripheral sessions
```


I627 · line 383 · counted-retrospective · A, B, D-result

```text
- [x] All 254 existing tests still pass (342 total now)
```


I628 · line 384 · counted-retrospective · A, D-result

```text
- [x] 8 integration tests (36 assertions)
```


I629 · line 385 · counted-retrospective · A, D-result

```text
- [x] `clojure -X:test` passes cleanly (342 tests, 976 assertions, 0 failures)
```


### M-vsatarcs-invariants-integration

Status line: INSTANTIATE (read-only INSTANTIATE-0 active; IDENTIFY opened 2026-06-01, MAP completed 2026-06-01, DERIVE accepted 2026-06-01, ARGUE accepted 2026-06-01, VERIFY accepted 2026-06-01 after count precision pass)

Population: Late. Ticked: 31; unticked: 0.


I630 · line 122 · task-imperative · C

```text
- [x] **Inventory existing infrastructure:** initial pass recorded below.
```


I631 · line 123 · task-imperative · C

```text
- [x] **Inventory existing data:** initial pass recorded below.
```


I632 · line 124 · task-imperative · C

```text
- [x] **Identify ready vs missing:** initial ready/missing table recorded below.
```


I633 · line 125 · task-imperative · C

```text
- [x] **Answer survey questions:** Q1-Q6 answered below.
```


I634 · line 126 · task-imperative · C

```text
- [x] **Document surprises:** scope-changing findings recorded below.
```


I635 · line 323 · artifact-or-topic-fragment · C

```text
- [x] **Entity types:** proposed below.
```


I636 · line 324 · artifact-or-topic-fragment · C

```text
- [x] **Relation types:** proposed below.
```


I637 · line 325 · artifact-or-topic-fragment · C

```text
- [x] **Invariant rules:** proposed below.
```


I638 · line 326 · artifact-or-topic-fragment · C

```text
- [x] **Data flow:** proposed below.
```


I639 · line 327 · artifact-or-topic-fragment · C

```text
- [x] **IF/HOWEVER/THEN/BECAUSE:** recorded for the non-obvious decisions.
```


I640 · line 328 · artifact-or-topic-fragment · C

```text
- [x] **View/UI specifications:** proposed below.
```


I641 · line 329 · fact-assertion · C, continuation

```text
- [x] **Wiring diagram:** deferred to VERIFY as an explicit carry-forward because
```


I642 · line 331 · artifact-or-topic-fragment · C, continuation

```text
- [x] **Fidelity contract:** draft matrix recorded below; convert to tripwire
```


I643 · line 650 · artifact-or-topic-fragment · C

```text
- [x] **Pattern cross-reference:** recorded below.
```


I644 · line 651 · artifact-or-topic-fragment · C

```text
- [x] **Theoretical coherence:** recorded below.
```


I645 · line 652 · artifact-or-topic-fragment · C

```text
- [x] **Trade-off summary:** recorded below.
```


I646 · line 653 · artifact-or-topic-fragment · C

```text
- [x] **Generalization notes:** recorded below.
```


I647 · line 654 · artifact-or-topic-fragment · C

```text
- [x] **Plain-language argument:** recorded below.
```


I648 · line 745 · fact-assertion · C

```text
- [x] **Structural verification:** wiring diagram and checks recorded below.
```


I649 · line 746 · fact-assertion · C, continuation

```text
- [x] **Prototype / spike:** Claude review flags a required read-only
```


I650 · line 749 · artifact-or-topic-fragment · C

```text
- [x] **Completion criteria pre-check:** recorded below.
```


I651 · line 750 · fact-assertion · C

```text
- [x] **Fidelity check:** tripwire matrix recorded below.
```


I652 · line 751 · artifact-or-topic-fragment · C

```text
- [x] **Decision log:** recorded below.
```


I653 · line 1039 · counted-retrospective · D-result, continuation

```text
- [x] **0a. Pin count strata:** VERIFY count precision pass records 47 authored
```


I654 · line 1042 · task-imperative · D-fixed, continuation

```text
- [x] **0b. Build repeatable read-only enumerator:** one command/script that
```


I655 · line 1045 · task-imperative · C, D-target, continuation

```text
- [x] **0c. Classify all 86 current queue rows:** invariant candidate, witness
```


I656 · line 1048 · task-imperative · B, C, continuation

```text
- [x] **0d. Produce audit ledger draft:** one row per current queue item with
```


I657 · line 1051 · artifact-or-topic-fragment · C, continuation

```text
- [x] **1. Projection schema/spec:** EDN specs for invariant-state projection,
```


I658 · line 1054 · fact-assertion · C, continuation

```text
- [x] **2. Tripwire tests:** fidelity matrix converted into projection tripwires
```


I659 · line 1058 · fact-assertion · C, continuation

```text
- [x] **3. Read-only projection builder:** build projection from current sources
```


I660 · line 1060 · fact-assertion · C, D-fixed, continuation

```text
- [x] **4. Consumer migration:** Arxana reads the one projection (Live Invariants
```


</details>
