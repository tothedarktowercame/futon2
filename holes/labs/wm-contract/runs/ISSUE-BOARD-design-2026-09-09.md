# Unified issue board — discovery and join design, 2026-09-09

Author: codex-17. Requested by claude-1 for Joe. **Design only; no generator, ledger, registry, Lean, or paper changes.** Source fingerprints at the end bound this census. The proposed board is a derived projection, never a second issue-authoring store.

## Consequential findings

1. **RUN4 currently asks for a value its producer cannot emit.** The meter requires `:readiness :ready` for both named holes (`futon2/holes/labs/wm-contract/run4_readiness.bb:363`, especially line 387). The accounting generator declares only `:not-ready`, `:contested`, `:witnessed-and-held-open`, and `:witnessed-under-flag` (`futon2/scripts/generate_variable_situation_accounting.bb:913`). Its two relevant assignments are fixed at `:witnessed-and-held-open` and `:not-ready` (lines 364 and 390). The audit copies those values, rather than independently qualifying a run (`futon2/holes/labs/wm-contract/refresh_hole_closability.bb:17`). Consequently re-running these producers cannot alone make RUN4 READY. This is a vocabulary/acceptance conflict requiring a reviewed contract change, **not authorization to relabel the holes or bypass the gate**. The meter's header says it blocks `:not-ready` (line 69); its executable rule blocks every value other than `:ready`.
2. **Ownership counts answer different questions.** The current accounting has 133 rows: 124 contract declarations and nine glossary paragraphs; 124 rows are `:carried-by :unowned`. Its nine carried rows are linked to at least one *open or blocked* non-J ticket, not necessarily assigned work. The worklist has 208 tickets: 199 done, eight blocked, one open. Of the nine unfinished tickets, seven have `:owner :any` and two `:owner :joe`. A glossary/problem pointer, eligibility for any worker, a carrying packet, a lane name, a current worker, and the decision owner must be separate fields. Calling carried variables “moving” falsely suggests progress on the blocked F11/F12 rows.
3. **Publish-time rendering is not source refresh.** Figure 5 deliberately has a fixed 61-instance/nine-class population; the catalogue now also has class 10. Box 7 re-renders an older failed Lean build; it does not re-run Lean on publication. The worklist `:as-of` is September 1, lane registry August 31, and accounting September 8 even though relevant code and artifacts moved afterward. Source hash, declared date, observed date, and validation date must all survive the join. A fresh publish timestamp cannot certify old evidence.

## View identities and field census

Use stable TeX labels/output names as view ids. “Figure 3/5” and “Box 2/4/6/7” below are the requested user-facing aliases, not join keys. The build-state figure is included at `p4ng/sec-case-study-vetting.tex:83`; defect repair at line 271. The defect generator still calls itself “Figure 3” in its docstring (`p4ng/empirics-futon/make_defect_tally_figure.py:2`), another reason not to join by printed ordinal. No compiled auxiliary file was available to independently verify the current numeric ordering.

### Figure 3: build state / war-room tetrahedron

Generator: `p4ng/empirics-futon/gen_war_room_tetrahedron.bb:17`; output `p4ng/war-room-tetrahedron.svg:1`. Invoked at `p4ng/build-p4ng.sh:115`.

**All direct artifact inputs** (the optional inputs remain explicit in the board):

- `futon2/holes/labs/wm-contract/variable-situation-accounting.edn:1`: `:rows`, each `:row-source`, `:content-status`, `:pointer-status`; `:authority :contract-git-sha`; `:as-of`.
- `p4ng/empirics-futon/defect-repair-tally.edn:9`: `:counting-rule-version`, `:classes[*].instances[*].status`, `:as-of`.
- `futon2/holes/labs/wm-contract/lane-registry.edn:1`: `:lanes[*].lane`, `:holding`, `:expected-by`, `:as-of`. The current clock is also an input to overdue classification.
- `p4ng/empirics-futon/control-map-edges.edn:9`: `:edges[*].status/kind`, `:drawn-not-derivable`, `:route-measured-drawn`, `:derived-undrawn`, `:decisions`, `:as-of`.
- Optional `p4ng/empirics-futon/aif-conformance.edn:1`: `:theory-edges`, `:conformant`, `:missing`, `:unexplained`, `:choices-undecided`, `:not-realised`, `:as-of`.
- Optional `futon2/holes/labs/wm-contract/workflow-report.edn:1`: `:certified`, `:totals` (`:closed-by-lanes` or `:completed-by-lanes`, `:cases-attributed`, `:pending-decisions`), `:attribution` (`:systematic-window?`, `:systematic-from`, `:systematic-run-length`, `:cases-attributed`, `:cases-in-ledger`), `:frontier`, `:as-of`. Read/branch logic: generator lines 48–58 and 155–195.

**Every rendered data field:** nouns = hole count/contract-declaration count; verbs = all-variable count/drifted-pointer count; evidence = repaired/61, partial, open; organisation = conformant/theory-edge count, missing and not-realised counts, unexplained count and undecided-choice count, or (without conformance) drawn/measured/implied/not-derivable/unresolved/decision counts. Each vertex has a lane-id/state chip (`idle`, `in-budget`, `OVERDUE`); holder identity itself is not printed. Certified workflow prints completed-or-closed/attributed cases, pending decisions, frontier, and either continuous-from/run-length attribution or sparse attributed/ledger-case totals. Uncertified workflow prints frontier, pending, aggregate lane states, and certification-withheld text; absent workflow prints a named gap. Footer prints contract prefix plus dates for accounting, tally, lanes, optional workflow, map, optional conformance. Titles, lane labels, legends and branch explanations are renderer literals. Rendering: generator lines 141–237. The build figure is an aggregate over heterogeneous subjects, not one issue population.

### Figure 5: defect repair state

Generator: `p4ng/empirics-futon/make_defect_tally_figure.py:7`; its **sole direct data input** is `p4ng/empirics-futon/defect-repair-tally.edn:9`. The catalogue pointer in that ledger is provenance, not an additional file the renderer reads. Outputs SVG/PDF; publish invokes generation and stamp check at `p4ng/build-p4ng.sh:135`.

Fields consumed: `:as-of`, `:counting-rule-version`, class `:n/:name/:instances`, instance `:id/:status/:evidence`. The rule fixes class sizes `[6,4,5,5,5,27,4,1,4]`, total 61; statuses are repaired/partial/open (generator lines 10–50). **Rendered:** class number/name; repaired, partial and open segment lengths and counts (small segments omit inner numerals); repaired/total suffix and partial annotation; status legend; as-of date and source SHA-256 prefix (lines 54–86, 180–181). Individual ids, owners and evidence are not rendered. The source catalogue is `futon2/holes/problems/P-defect-classes.md:1`, with class headings, witnessed instances, discovery/repair accounts and methods. Its class 10 is at line 526. The current fixed ledger counts 60 repaired, one partial, zero open; class 6 is 27/27 repaired and class 7 is 4/4 repaired. Do not count prose occurrences as new fixed-population instances.

### Box 2: model coverage

Generator: `p4ng/empirics-futon/gen_model_coverage.py:27`; output `p4ng/sec-model-coverage-generated.tex:1`; inclusion/caption `p4ng/sec-case-study-vetting.tex:327`.

**All direct inputs:** accounting EDN above and `mathlib4/DarkTower/WarMachine/holes-contract.json:1` (generator lines 31 and 211). The defect commentary is **hard-coded in the generator's ROWS table**, lines 42–56; it does not join/read the defect ledger despite the paper's caption describing a join.

**Every rendered field:** eight fixed area labels; four per-area readiness-band counts (named/type/formula/witnessed-or-higher); open declaration names with closability fence superscripts; per-area defect prose. Total row: four band totals; accounting contract pin and optional live-pin lag; glossary-only population, content-category counts and named glossary holes outside the declaration population; drifted-pointer count; unclassified declaration count/names; by-record/problem-record/operator-record split; pre-run/run-gated fence counts; names where the fence and owner-based partition differ; rung summary and names where named-rung and open-hole sets differ. The generated comment carries accounting `:as-of`. Source fields: `:axes.rung/closability`, row `:name/:area/:row-source/:content-status/:pointer-status/:rung/:closability`, accounting authority/date, contract source SHA, declaration `:kind/:name/:holder/:owner`. Derivations/rendering: generator lines 140–315.

Upstream accounting producer: `futon2/scripts/generate_variable_situation_accounting.bb:19`. Its named inputs are contract JSON, `p4ng/sec-glossary.tex:1`, `futon2/checks/witness-registry.edn:1`, worklist, and `mathlib4/DarkTower/WarMachine/Holes.lean:1`. It also resolves evidence licences from its declared rung/closability tables against their referenced files (lines 480–543), so a board basis manifest must retain that **dynamic licence-path set**, not just the five entry files. `:carried-by` derivation reads subject mentions from statement/acceptance/blocker of live non-J worklist rows (lines 37–72); it is not an agent assignment.

### Box 4: Box 2 as kanban, and Box 6: readiness scale

Both are emitted by `p4ng/empirics-futon/gen_variable_situation_table.bb:8` into `p4ng/sec-variable-situation-generated.tex:1`, included at `p4ng/sec-case-study-vetting.tex:369`. Stable labels are `tab:variable-kanban` and `tab:readiness-rungs`. The same output also contains `tab:variable-situations`; preserve it as a facet, rather than losing it because the request counted only six views.

**All direct inputs:** accounting EDN, contract JSON, worklist EDN (generator lines 8–16, 43). No frontier Markdown is read: dependency depth is derived directly from `:depends-on`.

**Box 4 rendered fields:** family (`:area`), moving variable count (`:carried-by` vector), unowned count (`:carried-by :unowned`), unique carrying packet ids ordered by recursive dependency depth then id; caption unowned total/all-row total. Neither ticket status nor owner nor the number of unfinished transitive dependencies is printed. The upstream carrying rule also excludes `:done-unreviewed` and `:needs-joe`, so it cannot serve as a complete in-flight/review/decision inventory. Logic at lines 137–166.

**Box 6 rendered fields:** each of all eight ordered readiness rungs, owner-pointer resolves/drifted counts, row total; caption highest occupied rung and number of empty higher rungs, with fixture-vs-machine-state explanation. Rows require valid `:rung` in accounting's declared axis; no skipped rung is promoted. Logic at lines 121–138 and 179–193. Current all-row counts: named 19, type-transcribed 61, formula-transcribed 53, all five higher rungs zero.

**Companion two-axis facet:** content-status × pointer-status cell counts/totals; zero/drifted-name examples (up to eight), registry SHA-256 prefix, CURRENT/STALE-PIN, registry/live contract prefixes. Logic at lines 93–120 and 167–177. Here “owner resolves” means a source pointer resolves, not a person has accepted an assignment.

### Box 7: Lean state

Generator: `p4ng/empirics-futon/gen_lean_state.bb:16`; output `p4ng/sec-lean-state-generated.tex:1` included at `p4ng/sec-case-study-vetting.tex:427`.

**Direct inputs:** `futon2/holes/labs/wm-contract/runs/U35-lean-state/lean-state-report.edn:1` plus current mathlib git HEAD (generator lines 17–20, 67–69). Publication does **not** refresh that report (`p4ng/build-p4ng.sh:64`). Upstream `futon2/holes/labs/wm-contract/lean_state_probe.bb:32` scans all `.lean` modules under `mathlib4/DarkTower/WarMachine/`, reads Holes.lean, contract JSON, accounting EDN, `futon2/holes/labs/wm-contract/aif-equations.edn:16`, `mathlib4/lean-toolchain:1`, and git/toolchain/build results. It records module source hashes; no new Lean build was run for this discovery.

**Every rendered field** (generator lines 67–203): report/build dates; corpus-vs-live pin state and SHAs; corpus root, module count, source lines, total declarations and counts by declaration kind, toolchain; modules checked/exit-zero/exit-nonzero/error-diagnostics, wall time and start/finish times, typecheck conclusion; source sorry count and per-file locations, axiom count and warning/token agreement; report's contract-vs-corpus authority and pin-state, source/contract hole counts, missing contract names, sorry-warned and non-sorry-hole counts/names. Equation table: registry equation key, R-node, defined quantity, resolved Lean name and kind or explicit absence, source file:line or declared status; resolution totals/no-carrier count. Glossary table: row-source, with/without Lean declaration counts and totals; source accounting date and unresolved names. Consumed report sections are `:corpus/:typecheck/:sorry-census/:axiom-census/:contract-cross-check/:equation-lean-join/:glossary-lean-join`.

### Worklist, frontier, closability and RUN4: issue-level sources

- **Worklist** `futon2/holes/labs/wm-contract/worklist.edn:10`: `:items[*].id` is the primary ticket key; `:statement` title/full description; `:class`, `:status`, `:owner` (notably `:any`/`:joe`); `:depends-on`; `:blocker` **and** `:blocked-on` (RUN4 uses the latter, line 619); `:basis/:epic/:covers-key` ancestry; `:acceptance/:evidence/:reviewed-by/:assures`; optional owner-note, progress/slice trails and rulings. Missing keys remain absent, not empty assertions. Root schema/date/steward are ledger provenance, not per-row current assignment. Status vocabulary and second-reader condition are at lines 5–8 and 34–35. F10/F12/F11 are at lines 1390/1400/1405; U80 at 1518; U88 at 1586.
- **Frontier** `futon2/holes/labs/wm-contract/dependency-frontier.md:1`: dependency-first table depth/ticket/status/depends-on, then pattern/state/ticket/violation-signature table at line 218. Producer `futon2/holes/labs/wm-contract/gen_dependency_frontier.bb:9` reads worklist and every `futon3/library/apparatus/*.flexiarg`'s `@violation-signature`; it also hashes each assured witness's referenced mechanism file (lines 63–69). Missing deps/cycles/ambiguous pattern coverage refuse. This is a worklist projection, not another dependency authority. **Read-only `--check` passed: 208 tickets, 14 pattern/signature pairs.**
- **Closability** `futon2/holes/labs/wm-contract/runs/U27-hole-closability/audit.edn:1`: schema/date; authority contract path/SHA/content hash/accounting path/count; row name, row-source, area, closability, readiness, owner, basis, optional runtime-evidence, pointers; counts split contract/glossary and both status axes. `refresh_hole_closability.bb:17` copies every open-hole accounting row and licences, emits audit EDN and AUDIT.md, inherits accounting date/pins. Current 11 rows = 10 contract + one glossary; four pre-run, seven run-gated. The audit is **not independent corroboration of its parent accounting**; comparison found no differing name/closability/readiness/owner values across their 11 joined rows.
- **RUN4** `futon2/holes/labs/wm-contract/runs/F2-run4-readiness/READINESS.edn:1`: schema, emitted-at/by, `:subject {:board "wm-contract" :item :RUN4}`, producer `:row :F2`, verdict, summary, blocked-on reasons, ordered per-line verdict/evidence/reasons, manifest/preregistration refs, non-requirements, accepts-nothing qualification. **Join to subject RUN4, not producer row F2** (`run4_readiness.bb:476`). README prints nine named verdict lines and their detailed blockers (`run4_readiness.bb:440`). `negative-controls.edn` is control evidence, not another live issue population.

RUN4's complete direct input family (`run4_readiness.bb:103`, 142–418): live `runs/*/certificate.edn` filtered to minted-awaiting-acceptance; each certificate's trace path and `lean-block.lean`; Holes.lean and mathlib Run4Preregistration.lean; control-map-edges; holes-contract; audit; preregistration `01-assertions.edn`, `02-authorities.edn`, `03-invalidators.edn`, `axiom-probe.edn`, and artifact Run4Preregistration.lean. The regeneration leg additionally compares each certificate directory's `00-source.edn/01-topology.edn/02-routes.edn/03-classification.edn/04-controls.edn/lean-block.lean`, and preregistration's `00-source.edn/01-assertions.edn/02-authorities.edn/03-invalidators.edn/04-controls.edn/Run4Preregistration.lean`, using `u49_route_transcribe.bb` and `run4_prereg_transcribe.bb` with recorded producer environments. The non-gating selection-discrimination section also reads `runs/RE7-selection-discrimination/*/01-decisions.edn` (generator line 459). Git last-touch, tracked/clean and content digests are inputs too. A production board should consume the recorded readiness artifact, not trigger these generators while rendering.

The stored certificate set names `2026-09-01-s5` and `2026-09-04-re5`, with four manifest assertions. Stored verdict is `BLOCKED-ON [closability-audit]`: eight green lines, one blocked line, **two named reasons** (wmRunConformsToWiring and enactedEqualsSelectedWhenRankOneGated). “One blocked line” is not “one remaining task.” This discovery read the committed result; it did not rerun or refresh it.

## Join design

### Record shape and identity

Proposed issue fields:

`id`, `kind`, `title`, `description`, `column`, `source-status`, `owner`, `decision-owner`, `lane`, `reviewer`, `tags`, `subjects`, `blocks`, `blocked-by`, `dependency-counts`, `freshness`, `basis`, `conflicts`, `acceptance`, `next-action`.

- Use namespaced ids: `wm-ticket/F11`, `wm-declaration/<registry-name>`, `wm-glossary/<stable-anchor>`, `wm-defect/<class-n>/<instance-id>`, `wm-run4-line/closability-audit`, `wm-choice/<choice-key>`, `wm-assurance/<pattern-id>`. Do not unify tickets and declarations merely because their text overlaps. Their link is a typed `carries` relation. Retain a namespace/module where a Lean short name is ambiguous; never silently choose the first declaration.
- `owner` means the person/seat responsible for the **next action**. `:any`, nil, missing, a bare problem-record string or a lane without a holder produces **UNOWNED**. Preserve original value in `source-owner`. `decision-owner` can be Joe while implementation ownership remains unassigned. A field explicitly assigning Joe is valid ownership; a prose mention of Joe is a candidate relation requiring review.
- `basis` is a vector of source repo/path, source key/selector, line pointer, blob/content digest, declared date and source authority. Keep source spans separate from editable titles. An issue can have contradictory basis claims, all retained.
- `tags` are evidence-bearing records `{value, relation, source-key, basis}`, not untraceable labels. Keep content status, readiness rung, hole closability, witness readiness, mathematical area, issue class, run eligibility and source-pointer health as separate facets.

### Exact join keys and precedence

| Relation | Key / source | Rule |
|---|---|---|
| Ticket → dependencies | worklist `:id`, `:depends-on` | Resolve only within this ledger; unknown target/cycle is a board derivation defect. |
| Variable → carrying ticket | accounting `:carried-by` → worklist `:id` | Preserve existing U85 subject-derived link and its provenance; revalidate it against current tickets. It is not ownership or completion. |
| Accounting → contract | `:row-source :contract-declaration` + `:name` → declaration `:name` at the pinned contract | Require unique identity and matching authority. Glossary rows do not enter the contract denominator. |
| Audit → accounting | `(:row-source, :name)` plus authority | Parent/derived comparison, not two independent witnesses; name-only works today but would collide across populations. |
| RUN4 readiness → ticket | `:subject.board/:subject.item` | `:row :F2` identifies the producer; cannot attach the verdict to F2 instead of RUN4. |
| RUN4 line → holes/certificates | line `:rows` names, certificate `:run.name` and certificate identity | Two target holes become separate required conditions; trace path/digest and manifest authority stay attached. |
| Defect → catalogue | class `:n`, instance `:id`; catalogue section | Fixed-population identity; prose evidence/C-case references suggest links but cannot certify an instance-to-ticket join without a unique resolved basis. |
| Assurance → ticket | `:assures.pattern/:signature` → apparatus `@violation-signature` | Reuse frontier's uniqueness/signature/hash checks. Render stale witness as stale, not done. |
| Equation → Lean → variable | equation `:id/:lean` and probe `:resolved-name` → unique corpus declaration → accounting name | Reuse probe's resolution; retain declared-carrier vs found-carrier distinction. |
| Issue → lane | explicit assigned lane or justified reviewed mapping | Lane status alone cannot allocate a worklist row; lane registry has no issue-id binding when idle. |

No last-writer-wins merge of assertions. Worklist owns ticket lifecycle; contract owns declaration kind; audit owns its recorded typing; equation/map registries own their respective claims; accepted reviewer record owns acceptance. Generated TeX/SVG are comparison surfaces, never primary issue authorities.

For each ticket compute unique reachable upstream ids, unique unfinished upstream ids, immediate unfinished ids, and unique downstream ids affected. Deduplicate shared ancestors. Display “blocked by N explicit unfinished tickets; M further recorded conditions; K unresolved links.” Prose-only dependencies must be listed as unresolved conditions, not silently counted as zero or promoted to approved edges. Historical done ancestors remain inspectable but do not inflate outstanding work. Transitive counts over an incomplete graph carry `incomplete` explicitly.

At the census, seven of eight blocked worklist tickets have **zero unfinished explicit ancestors**; only U80 reaches an unfinished dependency (F12). This is an incomplete graph, not proof that those seven are runnable. RUN4 illustrates the limitation: its only declared edge is RUN3 (already done), while its non-green readiness line holds two other conditions. F11 has no `:depends-on` yet its blocker names three Joe-owned choices. Derive `blocked-on-condition` relationships from these structured facts; propose canonical dependency additions for the ledger owner to review later. Do not write them during board generation.

## Source disagreements and limits found in this census

These are the complete disagreements identified across the scoped joins above; unstructured prose has not been exhaustively semantically adjudicated.

| Subject | Claims that disagree or invite a false equivalence | Required board treatment |
|---|---|---|
| RUN4 readiness | Consumer requires `:ready`; producer axis cannot emit it. Header names only `:not-ready`, code is stricter. | First-class contract-conflict issue; block readiness interpretation pending review. Never emit READY from a renamed value. |
| find / F11 | Audit pre-run-closable/not-ready; worklist blocked awaiting three choices, although evidence experiment exists. | Not a logical contradiction: closability is timing, not scheduling. Show decision dependencies and available decision sheet; no “ready-for-lane” inference. |
| organise / F12 | Pre-run-closable, yet blocker requires an exercised O4 witness absent from a passing constructor. | Passing construction and sufficient acceptance evidence are different claims. Show witness obligation, retain blocked status. |
| C | Accounting links F10 and RUN4; Lean-hole renderer prints REFUSED-BY-DESIGN before examining those links. | Preserve refusal warrant plus actionable tickets. Proposed label: “implementation held pending stated condition,” with the actual condition, rather than an unexplained terminal badge. |
| Ownership / movement | Box 4 labels vectors as moving, including blocked tickets with owner any; 124 unowned variables include already-transcribed declarations. | UNOWNED band plus separate “no carrying packet” count. Do not manufacture 124 implementation tasks or claim nine active workers. |
| U80 classification | Worklist blocker records that historical PERMANENT substrings drive a seven-attestation classification, while the audit and Joe's RUN4 ruling allow closure by evidence. | Keep U80 as a substantive interpretation conflict, not a stale counter to reset. Source: worklist line 1518; RUN4 ruling line 618 onward. |
| Box 2 vs repair ledger | Box 2 hard-coded belief note says 20 of 27 ingress boundaries repaired and class-7 notes remain open; the fixed ledger records 27/27 class-6 and 4/4 class-7 repaired. | Show the conflicting scoped/date-bearing claims. Do not assume a new recurrence or auto-close a current issue from a historical repair. There is no executable join establishing that the prose describes the same current evidence. |
| Defect population | Figure 5 fixed nine classes/61 instances vs catalogue class 10 and later examples. Box 2 defect summaries are literals, not a live ledger join. | Label cohort/cutoff; show catalogue additions as unmapped, not omitted. Retiring Figure 5 requires preserving its historical cohort facet. |
| Lean report vs live authority | Report September 8, corpus HEAD 738cae3a54, contract e239086a44, two failed modules; current mathlib HEAD 0222969e1e, current contract source fcd1261c30. Generated section says STALE-PIN but also prints “current” for the report's internal old comparison. | Scope each current/stale adjective to its comparands; do not imply old build failure establishes current build failure, or that publish revalidated it. |
| RUN4 pin vs build HEAD | Accounting/audit pin fcd1261c30 is Holes.lean authority; 0222969e1e is contract-file/checkout commit. | Different keys, not contradictory hashes. Store authority-kind and compare like with like. |
| R3a / R8 attribution | Control-stage row places R3a in BELIEVE from code, while prediction-error equation is hosted at R8; the stage basis explicitly leaves hosting undecided. | Preserve declared equation host vs observed producer placement with both tags, flag unresolved attribution. `p4ng/empirics-futon/control-stages.edn:19`; equation key `prediction-error`. |
| Source age | Worklist date September 1 and lanes August 31 can be printed next to September 8 accounting and newly published figures. | Show stale or unknown observations. An idle lane record is not current proof nobody is working. |

The accounting/audit hole fields agree exactly at this snapshot. Frontier `--check` passes. These agreements are reported to distinguish actual conflicts from mere different axes or stale renderers.

## R-node and tetrahedron tag derivation

R1–R20 are a requested filter vocabulary, **not a promise every integer currently appears in the wiring registry**. `p4ng/empirics-futon/control-map-edges.edn:11` lists R1–R17, R20, R3a and TRACE; R18/R19 are absent. `control-stages.edn:15` supplies stage/band/label for drawn/declared nodes and also lacks R18/R19. The 18 equation rows in `futon2/holes/labs/wm-contract/aif-equations.edn:16` cover only 12 distinct R-nodes. Do not drop R3a, force TRACE into an R number, or infer R19 merely from every use of C.

Derivation order:

1. A resolved equation subject inherits that row's explicit `:node`, equation id and defines/imports. E.g. machineObservation → observe → R2; machinePrecision → precision → R7; expectedFreeEnergy → expected-free-energy → R5.
2. A wiring-edge subject inherits `:from/:to` with relation `endpoint`; use `(from,to,kind,label,layer)` to distinguish parallel/derived/drawn claims. Do not equate a control edge with a task prerequisite.
3. Ticket `:covers-key`, resolved subject or explicit pinned registry reference inherits the corresponding node tags through a typed subject relation. Broader downstream impact is a different tag relation, not direct ownership.
4. Missing/ambiguous mapping is `R-node-unmapped`, with suggested text-derived candidates kept unapproved. R18/R19 filter entries read “no canonical node assignment in these inputs” until a reviewed source supplies them. Existing dossier inputs (`p4ng/empirics-futon/gen_rnode_dossiers.py:65`) include FUNDAMENTALS and ALIGN census; consult them for candidate mappings, not as authority to invent absent R-node identities.

Tetrahedron primary buckets: **nouns, verbs, organization, evidence**, from the four expected wm-* lanes (`p4ng/empirics-futon/gen_war_room_tetrahedron.bb:104`). Preserve spelling normalization (`organisation` display ↔ `wm-organization` key). Derive from an explicit lane assignment first; otherwise propose based on the issue's actual subject with a cited basis (carrier/type → nouns, executable transition/operator → verbs, wiring/dependency policy → organization, measurements/acceptance/validation → evidence). Many-to-many secondary tags are allowed; inferred classifications remain visibly proposed. Worklist class alone cannot decide the bucket.

**Fifth bucket: cross-cutting — PENDING Joe's confirmation.** Candidate members are ownership defects, source conflicts, stale evidence and requirements spanning several vertices. The figure's interior currently represents workflow; that is evidence for considering a cross-cutting facet, not an existing ruling establishing this fifth bucket. Do not silently map every unmapped item into it.

## Columns and the compliant-run view

Reacting to the proposed five columns: keep them, but add an explicit **blocked-on-prerequisite** holding column. Without it, ordinary engineering/library blockers would have to masquerade as either rulings or ready work.

| Column | Admission rule |
|---|---|
| ruling-needed | Explicit reserved decision/needs-Joe condition with a resolvable decision subject. Spelling corrects the suggested “ruled-needed.” |
| blocked-on-prerequisite | Unmet engineering/library/source-contract condition; unknown graph edges remain visible here. |
| ready-for-lane | Actionable acceptance criterion, all known prerequisites satisfied, source basis current, no reserved decision. Assignment is independent: an unassigned eligible issue is in the UNOWNED defect band here. |
| in-flight | Live dispatch/lease/working receipt names this issue and responsible seat; awaiting independent review is a visible substate. A linked open ticket alone is insufficient. |
| run-gated | The *remaining next condition* requires qualifying run evidence or its acceptance. Keep pre-run construction subtasks in their proper columns. |
| done | Source's own completion/review requirements met at the displayed basis. A declaration's rung or a passing build alone never marks an implementation issue done. |

If several conditions hold, select the next blocking action as the primary column and retain other conditions as badges/edges. Missing or conflicting evidence prevents automatic advancement. An unexplained refusal is not a column. Historical “held-open after witnessing” needs an explicit acceptance action; it is not an eternal red box or automatic done.

UNOWNED is a persistent defect band over all actionable columns with issue ids/counts and an assignment action. Distinguish no accountable next actor, no carrying packet, missing decision owner, and stale lane assignment. No blank ownership cells. For preserved historical done records, missing authorship is a provenance defect, not an automatic reopened task.

Default saved filter: **“What is needed for a compliant RUN4?”** Root at wm-ticket/RUN4, expand nine readiness lines, their two current hole conditions, certificate/trace/wiring bases and explicit prerequisite closure. Show next assignable work and ready decision sheets first; display the producer/consumer readiness conflict at the top. Separate “can execute an informative run,” “can produce a validation certificate,” and “Joe has accepted this run.” Selection-discrimination is a reported non-requirement in the current meter (`run4_readiness.bb:80`), so the board must not turn it into an acceptance gate. An acceptance remains Joe's; the board provides evidence, not a new ruling.

## Freshness and staleness

Per requirement, each issue's displayed `as-of` is the **freshest declared source as-of** among its attached facts; header prints the **oldest issue as-of it contains**, prominently. That alone can hide old evidence joined to a fresh ticket, so also display `oldest-basis-as-of` and a header “oldest supporting source” stamp. Every fact retains its own timestamp.

Store declared-as-of, generated/emitted-at, observed-at (board read), content hash, source commit, authority-kind/pin and validation-at separately. Unknown as-of remains UNKNOWN; no fallback from file mtime or today's publish date pretending to be evidence. Date-only values retain day precision. For READINESS's `:emitted-at`, label it as emission, not an invented source `:as-of`. Mark any issue/header with unknown constituent dates.

Accounting's generator hard-codes September 8 (`generate_variable_situation_accounting.bb:892`); audit inherits it. Root worklist and lane dates also lag the current inspection. A hash change under an unchanged declared date is a `date-not-maintained` finding; unchanged counts with changed identity/content hashes are real motion, not stasis. An unchanged fixed-cohort count is expected, not automatically stale.

At publish, fingerprint all direct inputs and referenced licences before/after the read. If an authority moves mid-read, refuse the snapshot as mixed-basis. Use stable sorting and unique ids so equal basis regenerates equal board bytes. Print counts by population (tickets/declarations/defect instances/readiness conditions), not a misleading sum of all joined nodes. Show diff-from-previous-published-basis: ids added/completed/changed, assignments changed, blockers added/removed, and timestamp-only changes separately.

## Retirement map and side-by-side cycle

| Existing view | Board replacement facet/columns | Evidence to preserve before retirement |
|---|---|---|
| Figure 3 build state | tetrahedron facet, assignment/UNOWNED band, in-flight, dependency/wiring and evidence summaries | All four vertex populations, lane freshness, certified vs uncertified workflow and conformance counts. |
| Figure 5 defects | defect cohort facet; done/blocked/ruling-needed | Fixed 61-instance rule, nine-class cohort, all instance evidence, partial distinction, source date/hash; catalogue additions separately. |
| Box 2 coverage | variable area × rung facet plus closability and linked issues | Distinct contract/glossary denominators, all hole names/fence markers, owner-pointer state; expose static defect-prose limitation. |
| Box 4 kanban | actual issue columns and ownership/dependency panels | Old carried/unowned totals reproducible; new assignment/in-flight counts explicitly different metrics. |
| Box 6 readiness | eight-rung evidence facet across issue columns | All zero rungs remain visible; rung licences, no implied promotion from closed tickets. |
| Box 7 Lean state | build/validation facet, stale-basis band, unresolved carrier issues | Corpus/toolchain/build receipt, module errors, sorry/axiom census, both joins and historical/live pin distinction. |

First publish shows board and all six existing views side by side at an explicitly recorded basis. Reconcile each count to its underlying id set, not just an equal total. Commission missing-owner, stale-source, unresolved-dependency, cycle, conflicting-status, ambiguous-name and missing-R-tag controls before replacement. No view retires merely because a new screenshot exists. After independent review and Joe's acceptance of semantics (including the fifth bucket), retire views individually; keep historical source artifacts and the retirement mapping available.

## Verification and boundaries

Read-only structured census of eight EDN sources, generator and TeX source inspection, and frontier `--check`. No publish, generator refresh, Lean build, readiness rerun or registry mutation. Local scratch EDN decoding initially failed while sorting mixed symbol/keyword worklist keys; JSON export had already completed, and the subsequent census used explicit fields instead. No count in this note relies on that failed sort. Final checks resolved 45 distinct explicit repo/file:line pointers and confirmed all 13 fingerprinted sources unchanged during final validation. Markdown-only change: clj-kondo/check-parens are not applicable; `git diff --check` passed. No guessed file location is used as a resolving pointer; failed exploratory filename guesses were replaced by the located paths above.

## Source fingerprints at census completion

- `futon2` HEAD: `4b297d6b1e9813dad862d816c33b906910c934c7`.
- `p4ng` HEAD: `f920a9d6b34cc37e94b2391b9fc2404708f5546c`.
- `mathlib4` HEAD: `0222969e1ed5181b374b9d04bd0031ed5ac20a08`.

These are source-content fingerprints, not freshness dates. Full SHA-256 values permit checking the exact basis even when the checkout advances.

| Source | SHA-256 |
|---|---|
| `futon2/holes/labs/wm-contract/worklist.edn` | `56ba8ec52535a5a51b2f978d6cc5ed0be2834371f6c3ee05ea076700e317a372` |
| `futon2/holes/labs/wm-contract/variable-situation-accounting.edn` | `c335127949ae33e80bbf4445c6fe8751cbaca1d873a16f294f585fc499f85dc7` |
| `futon2/holes/labs/wm-contract/runs/U27-hole-closability/audit.edn` | `ce78200decf3bb724797dc15a857f97177a9c087e5c3ad3df6bc96799c1bbaeb` |
| `futon2/holes/labs/wm-contract/runs/F2-run4-readiness/READINESS.edn` | `9bfc56ff8d8cae4823bf85fdec9bfe968f83b1caa52d012fdf24ade97c7b70ff` |
| `futon2/holes/labs/wm-contract/runs/U35-lean-state/lean-state-report.edn` | `607f4b9c4d5a6d79125c36eeb3b0b74011340cc206315b303d9aee2ad8fee615` |
| `futon2/holes/labs/wm-contract/aif-equations.edn` | `94ed156126cb04df60021db620428573ddffd4eb659eb7bbf60da84a29ea7b4d` |
| `futon2/holes/labs/wm-contract/lane-registry.edn` | `ab55f11fccaee0a553d8426cb83b5283e39943b00b4af65c5a1ad49f0a1ff69e` |
| `p4ng/empirics-futon/defect-repair-tally.edn` | `608752834e4a88e76c64542ea1c4f03000c2b7bd82192c52eeeaf9c2ac5be56c` |
| `p4ng/empirics-futon/control-map-edges.edn` | `161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec` |
| `p4ng/empirics-futon/control-stages.edn` | `ae4c4c584754d147ff32a31a9e25893bb98720815b72604c3ae48c495d91186d` |
| `p4ng/empirics-futon/aif-conformance.edn` | `028d8cc3e9c1b40442f88e7ac57aa238f177a7a464b3d1b0ceb0910571f2dcf6` |
| `futon2/holes/labs/wm-contract/workflow-report.edn` | `40050729b026dc6b1c898097ba701cd13687e3cf046ae29f2a2845fcadf6c6e1` |
| `mathlib4/DarkTower/WarMachine/holes-contract.json` | `444b9493e3a9b603b8027027b479c807eb1d377fab18e3f9f10859349b3b47ce` |
