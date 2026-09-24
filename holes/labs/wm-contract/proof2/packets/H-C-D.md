# H-C-D — an outcomes extractor, written blind, tested against the owner's hand reference

Author: claude-7 (Claude Opus 5), 2026-09-24. Packet H-C-D for PROOF-2.
Subject: `futon3c/holes/missions/M-futon-seams.md` at futon3c `3f5f44dd` (1002 lines).
Deliverables: this packet and `scripts/wm/extract-outcomes.clj`.
Read-only throughout: no mission edited, no click, no write under `data/`, no
JVM load.

## 0. What C is here, and how the blind was kept

PROOF-2a's finding (futon2 `924a6820`) sets the task:

> the outcomes are there to be read, and the War Machine should read them when
> it needs them: C is computed at click time from the mission text, as a step of
> the click with its own receipt (each outcome cued to the span it came from;
> each served-by link to the instance want it names), not required as a record
> prepared in advance.

So an **outcome** is a state of the world the mission wants to obtain, stated in
its prose and attributable to someone. It is not the same object as an
**instance want**: a want is a token a cascade produces (`:caller-converted`),
and the served-by link is the claim that producing that token advances that
outcome. A13/A9's C is a preference *over outcomes*; the wants are the machinery
that reaches them.

**Order of work, and what was not read.** §1 was written from the mission text
alone, before any code was written. §2's script was written against §1. Only
after both were fixed was `futon3c/holes/labs/M-futon-seams/item6/mission-C.edn`
opened (§3). Two further files were deliberately left unread until §3 for the
same reason:

- `futon3c/scripts/mission_c_check.py` — the reference's own checker. Reading it
  would have leaked the reference's key names into the extractor's schema, which
  is one of the three things §3 compares.
- `holes/labs/wm-contract/proof2/partial/H-C-D-kimi-5/` — the previous seat's
  unfinished packet and script. One stray file of that seat's, an untracked
  `packets/H-C-D.md`, was still sitting at this packet's own output path; it was
  moved into the partial directory **without being opened** before anything was
  written here.

Read before §1, and declared because they are inputs: the mission itself, the
five cascades under `holes/labs/M-futon-seams/proto/` (for the want tokens
only — `:want` lines), PROOF-2's A9/A13, PROOF-2a's finding, and the sibling
packet `H-EXITS-D.md` was **not** consulted, to keep the want-to-outcome
direction unprimed.

## 1. The hand extraction

Ten outcomes. Cues are `(first line, last line)` of a verbatim quote that occurs
exactly once in the file; quotes spanning a line break are marked. "Whose" is
the party the mission attributes the outcome to, not the party who wrote it
down — the mission is claude-1's recording of Rob's conversation, so most of
them are Rob's.

| id | whose | cue | verbatim quote |
|---|---|---|---|
| `O-1 provider-substitutable` | Rob | L79 | "the role Claude was playing could be served by Codex" |
| `O-2 transport-interchangeable` | Rob | L97 | "finally ran a \`matrix-ircd\` adapter to mimic IRC" |
| `O-3 editor-independent-core` | Rob | L16 | "a seam in the Emacs layer, so a VS Code / TypeScript implementation could reuse the core functionality rather than copy-pasting interactions into webhooks" |
| `O-4 store-kind-abstracted` | Joe (blocked party) | L146 | "there is no interface to a generic graph/semantic database — so Joe could not use it with XTDB out of the box" |
| `O-5 prompt-path-absorbed` | Rob | L128 | "The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing." |
| `O-6 no-separate-evolution` | Rob | L16 | "now the systems evolve separately, making their unification to a common interface even harder later on" |
| `O-7 seam-capability` | the mission (claude-1) | L289–290 ¶ | "find / an undeclared seam, choose how to declare it, do it, and check what was done" |
| `O-8 judgement-contradictable` | the mission | L709–710 ¶ | "a judgement must be recorded in a form something else can / contradict" |
| `O-9 interface-declared-and-converted` | the mission (IDENTIFY exit) | L167 | "the interface is declared, **at least one existing caller is converted to it**, and there is a test of the form" |
| `O-10 no-permanent-switch` | the mission (IDENTIFY exit) | L169 | "**Do not** mint the abstraction and leave the hardcoded path alive indefinitely." |

¶ = the quote crosses a line break; the cue is the line range it occupies.

Secondary cues, recorded because a reviewer checking an outcome will want the
second place it is stated:

| id | second cue | quote |
|---|---|---|
| `O-1` | L163 | "it unblocks provider substitution for Rob immediately" |
| `O-2` | L164 | "it retires \`matrix-ircd\` and the "don't start an IRC server" flag together" |
| `O-6` | L22 | "Two implementations that have already drifted cost more to unify than one implementation plus a stub." |

`O-5` and `O-10` are kept apart although their content overlaps. They are stated
by different parties at different scopes: L128 is Rob's note about *instance 6's
own* end state, L169 is the mission's rule constraining *every* instance's
enactment. Merging them would lose the second served-by pattern.

### Served-by: which instance wants serve which outcomes

Want tokens are read from the `:want` set of each cascade under `proto/`
(instance 4 `proto/instance-4.edn:36`, 5 `:46`, 6 `:53`, 7 `:41`). Instance 8
has no cascade at all — the mission says so at L247 — so it contributes a typed
absence, not an empty list.

| instance | want token | serves |
|---|---|---|
| 4 | `:caller-converted` | `O-1`, `O-9` |
| 4 | `:redirect-test` | `O-1`, `O-8`, `O-9` |
| 4 | `:prefix-routing-retired` | `O-1`, `O-10` |
| 5 | `:protocol-declared` | `O-2`, `O-9` |
| 5 | `:adapter-conformance-test` | `O-2`, `O-8`, `O-9` |
| 5 | `:impersonation-retired` | `O-2`, `O-6`, `O-10` |
| 6 | `:one-authority` | `O-5`, `O-6` |
| 6 | `:flag-retired` | `O-5`, `O-10` |
| 7 | `:record-schema-declared` | `O-3`, `O-9` |
| 7 | `:divergence-test` | `O-3`, `O-6`, `O-8` |
| 7 | `:writers-converted` | `O-3`, `O-6` |
| 8 | — | **typed absence**: `{:absent :no-cascade :cue L247 :outcome O-4}` |

Two outcomes are served by no instance want at all, and this is a finding rather
than a gap in the reading:

- **`O-4 store-kind-abstracted`** — the only outcome belonging to Joe rather
  than Rob, and the only one whose instance (8) has no cascade. The mission
  states it as live: L247, "**8** — live and unresolved, and the only such
  instance with **no cascade at all**". An outcome with no target that serves it
  is exactly the case Clause T's value term must not silently drop.
- **`O-7 seam-capability`** — served by the DERIVE method, not by any instance
  want. It is the mission's own subject ("a capability, not a list of
  instances", L289), so no cascade produces it and none should.

### The preference the mission states, and the one it does not

**Stated, over targets, by cost.** Two places, and they agree:

- L34, a heading that says what it is for: "## Cost ordering (use this to
  prioritise)" — declare first (free) < retrofit in code (expensive but
  mechanical) < retrofit in prompt text (worst), L36–38.
- L161, the IDENTIFY exit: "Pick one instance and declare its interface — not
  all eight. The candidates in cost order:" — then **4, 5, 7** at L163–165.

That is the 4-5-7 ranking PROOF-2a names. Three observations a value term has to
survive:

1. **It is an ordering over targets by cost, not a weighting over outcomes.**
   Nothing in the mission says `O-1` is worth more than `O-3`. Reading 4-5-7 as
   a value signal imports cost into the value term twice.
2. **Each of the three does carry a value clause beside its cost**, and they are
   not commensurable with each other: instance 4 "unblocks provider substitution
   for Rob **immediately**" (L163, urgency); instance 5 "retires `matrix-ircd`
   and the …flag **together**" (L164, two couplings for one act); instance 7
   "do **before** a VS Code implementation exists, not after" (L165, a closing
   window). One is urgency, one is scope, one is a deadline.
3. **Instances 6 and 8 are unranked.** Not omitted as worthless: under the L36–38
   cost ordering instance 6 is the third and worst tier, and instance 8 has no
   cascade to rank. The mission's ranking covers 3 of the 5 instances that have
   or need one.

**Weighting over outcomes: unstated.** No line of the mission assigns a
magnitude, a probability, or a comparison between two outcomes. The honest C for
this mission is an ordering over three targets plus a typed absence for the
outcome weighting.

## 2. The extractor

`scripts/wm/extract-outcomes.clj`. Read-only; mission path in, EDN on stdout;
runs under `clojure -M` and `bb`. Three disciplines, each carried in the output
so a reviewer checks the extractor rather than trusting it:

1. **Predeclared cues.** Eleven rules, fixed before any mission was read, each
   with an `:id` and a `:reads` saying in words what it believes it is reading.
   Every outcome names the rules that found it. A table tuned per mission would
   make the extractor a transcription of its author's reading, which is the one
   thing the reference exists to detect.
2. **Verified spans.** Every quote must occur exactly once in the file and at the
   line it claims. One failure refuses the whole output — a C whose cues are
   half-checked is worse than none, because the half that resolve make the rest
   look checked.
3. **Typed absence, never a substituted value.** No outcome found emits
   `{:absent :no-stated-outcome}` naming every section read; an instance with no
   cascade emits `{:absent :no-cascade}`, not an empty want list; an unstated
   weighting emits `{:absent :unstated}`, not a uniform prior.

Served-by is **section containment**: an outcome stated inside `### <n>. <title>`
under `## The eight instances` is served by that instance's cascade wants. That
is structural and exact. Anything outside gets
`{:unlinked :outside-instance-sections}` rather than a guess.

Both refusal paths were run, not asserted:

```
$ clojure -M scripts/wm/extract-outcomes.clj /tmp/mission-badcase.md ...
{:refused :cue-does-not-resolve,
 :failures [{:quote "The intended end state is the abstract path absorbing the ha",
             :occurrences 2, :claimed-line 128, :found-line 128}]}          exit 2
```
(the bad case: one cued sentence copied to a second place in the mission, so its
quote no longer identifies a span — nothing else changed)

```
$ clojure -M scripts/wm/extract-outcomes.clj /tmp/mission-nooutcome.md
 :outcomes {:absent :no-stated-outcome,
            :sections-read ["Mission: M-empty-test" "Background" "Status"]}  exit 0
```

Four structural defects were found and fixed while running it on the real
mission, all of which would have corrupted any mission, not just this one:

- `### <n>.` also matches DERIVE's numbered **method steps**, so steps 1–3 parsed
  as instances 1–3 and **every served-by row was emitted twice**. The instance
  scan is now scoped to the level-2 section that introduces the instances.
- A **mid-line colon** was treated as a sentence end, truncating
  `"Rob's closing ask for the future:"` and `"For whichever is chosen, the exit
  is:"` to their introducers. A colon now ends a sentence only before a newline.
- Attribution took the **first party in the table** rather than the first named
  in the sentence, so *"Joe could not use Rob's memory MCP"* came back as Rob's.
- An unbounded substring made every **`claude-10` a `claude-1`**. Word-boundary
  now.

### Output on M-futon-seams

`clojure -M scripts/wm/extract-outcomes.clj holes/missions/M-futon-seams.md --cascades holes/labs/M-futon-seams/proto`
— exit 0, mission sha256 `d13c5cfe…f6f6fd`, which is the `:mission-sha` the
cascades carry. 16 outcomes, all cues verified:

| id | cue | inst | whose | rules | quote |
|---|---|---|---|---|---|
| `:o-1` | L16 | nil | `"Rob"` | closing-ask, not-what-wanted, so-party-could | Rob's closing ask for the future: a seam in the Emacs layer, so a VS Code / TypeScript implementation could… |
| `:o-2` | L21 | nil | `"Rob"` | could-not | Rob could not use futon3c's agent roles because the code reads the provider out of the agent id. |
| `:o-3` | L21 | nil | `"Joe"` | could-not | Joe could not use Rob's memory MCP because it is neo4j-specific. |
| `:o-4` | L79 | 4 | `"Rob"` | had-to, so-party-could | Rob started with only Codex available and futon3c hardcoded to talk to Claude; configs and code had to be c… |
| `:o-5` | L97 | 5 | `"Rob"` | had-to | Rob had to add configurable room and agent names, then an option *not to start an IRC server* (futon3c pres… |
| `:o-6` | L128 | 6 | `:unattributed-in-instance-section` | intended-end | The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing. |
| `:o-7` | L132 | 7 | `:unattributed-in-instance-section` | wants-a | He is **not** asking for those adapters to be built now, especially since the Emacs code uses very specific… |
| `:o-8` | L138 | 7 | `:unattributed-in-instance-section` | had-to | The Python one already had to reimplement sentence splitting to match the elisp. |
| `:o-9` | L146 | 8 | `"Joe"` | could-not, so-party-could | The problem is that mfuton's memory MCP is **neo4j-specific** — there is no interface to a generic graph/se… |
| `:o-10` | L167 | nil | `:the-mission` | exit-is | For whichever is chosen, the exit is: the interface is declared, **at least one existing caller is converte… |
| `:o-11` | L169 | nil | `:the-mission` | do-not | **Do not** mint the abstraction and leave the hardcoded path alive indefinitely. |
| `:o-12` | L289–290 | nil | `:the-mission` | develops | What this mission develops is a **capability**, not a list of instances: find an undeclared seam, choose ho… |
| `:o-13` | L300–301 | nil | `"Rob"` | could-not | **In:** a person who could not do something — Rob had only Codex and the code was hardcoded to talk to Claude. |
| `:o-14` | L708–710 | nil | `:the-mission` | recorded-form | The mission states a constraint, in DERIVE: **a judgement must be recorded in a form something else can con… |
| `:o-15` | L778–779 | nil | `"claude-10"` | so-party-could | - `exemplar/check-ledger.edn` (claude-10) — every check run recorded with what was true, so the checks them… |
| `:o-16` | L893 | nil | `"Rob"` | so-party-could | Rob asked for the turn-annotation seam packaged so he could reuse it. |

Served-by, by section containment:

| instance | result |
|---|---|
| 1, 2, 3 | `{:absent :no-cascade}` — correct; the mission says they are retrospective (L246) |
| 4 | `[:caller-converted :redirect-test :prefix-routing-retired]` serves `:o-4` |
| 5 | `[:protocol-declared :adapter-conformance-test :impersonation-retired]` serves `:o-5` |
| 6 | `[:one-authority :flag-retired]` serves `:o-6` |
| 7 | `[:record-schema-declared :divergence-test :writers-converted]` serves `:o-7`, `:o-8` |
| 8 | `{:absent :no-cascade :outcomes-stated-here [:o-9]}` — the live instance with an outcome and no target |
| — | `:unlinked {:count 10 :absent :outside-instance-sections}` |

`:weighting {:absent :unstated}`.

## 3. The three comparisons

Only now was `futon3c/holes/labs/M-futon-seams/item6/mission-C.edn` opened. It
carries **6 outcomes and 14 served-by mappings**, and passes its own checker
(`python3 scripts/mission_c_check.py …` → "6 outcomes, 14 mappings — OK").

### 3.1 Hand table (§1, 10) against the reference (6)

| reference outcome | reference cue | hand | verdict |
|---|---|---|---|
| `:rob-can-run-the-stack` | L21 "Rob could not use futon3c's agent roles…" | `O-1 provider-substitutable` (L79) | **match, cue differs** |
| `:joe-can-use-robs-work` | L21 "Joe could not use Rob's memory MCP…" | `O-4 store-kind-abstracted` (L146) | **match, cue differs** |
| `:no-drifting-forks` | L22 "Two implementations that have already drifted…" | `O-6 no-separate-evolution` (L16, **second cue L22**) | **match; second cue identical** |
| `:vs-code-implementation-possible` | L132 "wants a seam inserted so later implementations are less painful" | `O-3 editor-independent-core` (L16) | **match, cue differs** |
| `:second-implementation-is-cheap` | L21 "every later implementation must impersonate the first" | — | **MISS** |
| `:coupling-visible-to-tooling` | L124 "With hardcoded code you can grep for the literal; with a hardcoded prompt you must match natural language at runtime" | — | **MISS** |

**4 matched, 2 missed, 6 extra.** The six extras split cleanly in two:

- **Two at a finer grain** — `O-2 transport-interchangeable` (L97) and
  `O-5 prompt-path-absorbed` (L128). The reference does not drop these; it folds
  them into `:rob-can-run-the-stack` / `:second-implementation-is-cheap` (for 5)
  and `:no-drifting-forks` / `:coupling-visible-to-tooling` (for 6), and its
  served-by rows say so. This is a grain disagreement, not an error either way,
  and §4 proposes which grain the value term should want.
- **Four of a different kind** — `O-7 seam-capability`, `O-8
  judgement-contradictable`, `O-9 interface-declared-and-converted`, `O-10
  no-permanent-switch`. These are statements about the **mission's own method
  and artefacts**, not about the world. The reference admits none of them. §4
  argues the reference is right and the definition should say so.

The two misses are the interesting half, because they are both cases where the
mission states an outcome in a voice the hand reading did not recognise:

- L21 **states the outcome as the cost of its absence** — "every later
  implementation must impersonate the first". The hand table read that line's
  three examples (`matrix-ircd`, Windows, agent roles) as evidence for instances
  and did not notice the general outcome they are evidence *for*. It is the
  reference leans on it hard: **4 of its 14 served-by mappings** point at it,
  across three different instances (4, 5 and 7).
- L124 **states it as a capability contrast** — grep finds a literal, nothing
  finds a prompt. The hand table read this as an explanation of instance 6's
  severity. It is an outcome, and the mission itself treats it as one: the
  VERIFY spike at L803–809 *tests the claim* ("22 tests, 75 assertions, 1
  failure — identical both ways. Nothing noticed"), which is what one does to an
  outcome, not to an aside.

### 3.2 Extractor (16) against the reference (6)

| reference outcome | extractor | cue |
|---|---|---|
| `:rob-can-run-the-stack` | `:o-2` | **same sentence, L21** |
| `:joe-can-use-robs-work` | `:o-3` | **same sentence, L21** |
| `:vs-code-implementation-possible` | `:o-7` | **same sentence, L132** |
| `:second-implementation-is-cheap` | — | MISS |
| `:no-drifting-forks` | — | MISS |
| `:coupling-visible-to-tooling` | — | MISS |

**3 of 6 hit, and all three hit on the reference's own sentence, not merely
nearby.** 3 missed, 13 extra.

The extractor's recall against the reference (0.50) is worse than the hand
table's (0.67), but its **cue selection is better**: where it hits, it hits the
exact span the owner chose, in 3 of 3. The hand table matched 4 outcomes and
agreed on the span for only 1 of them.

That is the packet's sharpest result, and it cuts both ways. The predeclared cue
table found the reference's spans at L21 and L132 that the hand reading walked
past — `:cue/could-not` and `:cue/wants-a` are exactly the voices the hand
reading was deaf to. What the cue table cannot do is decide **what is one
outcome**: L16, L132 and L893 all state the VS Code outcome, and the extractor
emits three rows (`:o-1`, `:o-7`, `:o-16`) where the reference emits one with one
cue.

### 3.3 Extractor (16) against the hand table (10)

**9 of 10 reproduced at the same span**, 1 partially:

| hand | extractor | span |
|---|---|---|
| `O-1` L79 | `:o-4` | same |
| `O-2` L97 | `:o-5` | same |
| `O-3` L16 | `:o-1` | same line; the extractor takes the whole sentence, the hand table a clause of it |
| `O-4` L146 | `:o-9` | same |
| `O-5` L128 | `:o-6` | same |
| `O-6` L16+L22 | `:o-1` | **partial** — the L16 clause only; the L22 secondary cue was not emitted |
| `O-7` L289–290 | `:o-12` | same |
| `O-8` L708–710 | `:o-14` | same |
| `O-9` L167 | `:o-10` | same |
| `O-10` L169 | `:o-11` | same |

Six extras over the hand table, and **three of them are the reference's cues**
(`:o-2`, `:o-3`, `:o-7`). The other three are false positives: `:o-8` (L138, the
Python splitter — evidence of drift, not an outcome), `:o-13` (L300, DERIVE step
1's input description — a restatement) and `:o-15` (L778, the check ledger — a
property of an artefact).

The near-total agreement between the extractor and the hand table is **not**
independent confirmation: the same author wrote both, hours apart, and §1 was
the specification §2 was written against. What it does establish is that the
cue rules mechanise that reading faithfully — the disagreements with the
reference in 3.1 and 3.2 are disagreements of *reading*, not artefacts of the
code.

### 3.4 Fourth comparison: kimi-5's partial

`proof2/partial/H-C-D-kimi-5/extract-outcomes.clj`, read only after the above.
It answers the same question differently, and one difference is a design
position worth recording:

- It extracts **phase exit criteria** (`**Exit criterion:**` sentences) as the
  outcomes, where this extractor and the reference read outcomes from the
  mission's prose about the world. On M-futon-seams those are disjoint sets:
  the exit criteria are about the mission's phases, not about what Rob or Joe
  can do afterwards.
- It **emits no served-by at all**, and says why in its header: *"The mission
  text never states a served-by relation, so none is emitted — linking wants to
  outcomes is a judgement, and this script does not invent it."*

That refusal is defensible and the reference contradicts it: the reference's 14
mappings each carry a `:how` paragraph arguing the link from the mission's own
measurements, and the one that cannot be argued that way — instance 8, which has
no want to link — is typed `:want nil :status :prospective` rather than dropped.
So the link is *derivable*, not stated — which means
kimi-5's position is right about the text and wrong about the task. Section
containment (this extractor's rule) is the part of it that **is** stated, which
is why 6 of 16 outcomes get a link here and 10 do not.

## 4. Which differences are defects, and whose

### Extractor defects

| # | defect | span | severity |
|---|---|---|---|
| E1 | **No outcome identity.** One row per cued sentence, so the same outcome stated three times is three outcomes. | `:o-1` L16, `:o-7` L132, `:o-16` L893 — all `:vs-code-implementation-possible` | **the one that matters**; every downstream count (C's support, the value term's normalisation) is wrong by the duplication factor |
| E2 | **Deaf to the consequence voice.** An outcome stated as the cost of its absence is not cued. | mission L21 "every later implementation must impersonate the first"; L22 "Two implementations that have already drifted cost more to unify…" | high — 4 of the reference's 14 mappings, across instances 4, 5 and 7 |
| E3 | **Deaf to the capability contrast.** | mission L124 "With hardcoded code you can grep for the literal; with a hardcoded prompt you must match natural language at runtime" | high |
| E4 | **Method statements taken as outcomes.** | `:o-10` L167, `:o-11` L169, `:o-12` L289, `:o-14` L708 | medium — see D1: partly a definitional gap, not the code's fault |
| E5 | **Evidence sentences taken as outcomes.** | `:o-8` L138, `:o-13` L300, `:o-15` L778 | medium — precision only |
| E6 | **Served-by limited to section containment**, so 10 of 16 are unlinked where the reference links an outcome stated in the HEAD to instance 7's wants. | `:o-1` L16 unlinked; reference links the same outcome to `7/:record-schema-declared` | medium — the rule is sound but covers a third of the cases |

E1 and E2 are the ones to fix first, and they are fixable without a model of the
mathematics: E1 by consolidating sentences whose cue text names the same
instance or the same party-and-artefact, E2 by adding the consequence voice to
the rule table. E4 and E6 need the definitional work below first.

### Reference defects

| # | defect | span | severity |
|---|---|---|---|
| R1 | **`:cue` offsets declare no unit, and the natural reading is wrong.** All 7 cues are **character** offsets. Read as bytes — the obvious reading for a span into a file — every one lands in the wrong place, because the mission contains multi-byte characters. `:cue [2771 2824]` as bytes is `"\n- *Cost of not:* every later implementation must imp"`, not the quote. | `mission-C.edn` all 7 `:cue` values; `scripts/mission_c_check.py:47` applies the character reading and so passes | **high for any second consumer**; nil for the existing one |
| R2 | The `:preference` note argues from the **cost ordering** (L34–38) without cueing it, although the file's own discipline is that every claim carries its span. It cues only L161. | `mission-C.edn :preference :cue [17969 18029]` | low |

R1 is the only reference finding of consequence, and it is not a content error:
every quote is right, the file passes its checker, and the owner's reading is
self-consistent. It is `[[a-name-is-not-a-specification]]` in miniature — `:cue`
names a span without saying in what. An extractor written in Clojure, Rust or
Go, or any Python consumer that opens the mission in binary mode, silently gets
a different span. **Proposed amendment: `:cue-unit :chars` required on the
schema, and `mission_c_check.py` to refuse a C that omits it.**

### Definitional gaps, proposed as amendments rather than scored as defects

- **D1. What counts as an outcome.** The reference admits only statements about
  the world; the hand table and the extractor also admitted the mission's
  statements about its own method (`O-7`–`O-10`, `:o-10`–`:o-14`). The reference
  is right for C's purpose: A9 makes C a preference over **outcomes of candidate
  universes**, and "a judgement must be recorded in a form something else can
  contradict" is a constraint on artefacts, not a universe. But nothing in A9 or
  in PROOF-2a's finding says so, and two of three readings here got it wrong.
  **Proposed: the finding's sentence should read "each outcome a state the
  mission wants to obtain, distinguished from the mission's constraints on its
  own artefacts, which are wants at mission grain."**
- **D2. Grain.** The reference states outcomes at a grain where one serves
  several instances (`:second-implementation-is-cheap` takes 4 of 14 mappings, `:no-drifting-forks` 5);
  §1 stated them at a grain where each instance has roughly its own. The coarse
  grain is better for Clause T — a value term needs outcomes that *discriminate
  between targets*, and an outcome with exactly one target serving it
  contributes nothing to a ranking. **Proposed: C's schema should record the
  grain it was extracted at, because a C at instance grain and a C at mission
  grain are not comparable and their value terms are not either.**

### On the 4-5-7 ranking

All three readings agree on the same two facts, from the same two spans, and
they matter more than the outcome counts:

1. The mission's ranking (L161–165: **4, 5, 7**) is an ordering **over targets
   by cost**, not a weighting over outcomes. The reference says it directly —
   *"the mission states no relative weight between these outcomes, and inventing
   one would decide the ranking rather than report it"* — and its stated reasons
   mix the two: "smallest surface" is cost, "unblocks provider substitution for
   Rob immediately" is value, and "do before a VS Code implementation exists" is
   a claim that cost **rises with time**.
2. **Instance 8 is unscorable, and that is a fact about the field, not about its
   value.** It is the only live unresolved instance, it has no cascade, and it
   is the only carrier of an outcome belonging to Joe rather than Rob. All three
   readings type it as an absence rather than dropping it — the reference with
   `:want nil :status :prospective`, this extractor with
   `{:absent :no-cascade :outcomes-stated-here [:o-9]}`. A Clause T that scores
   targets by served outcomes must not read either as zero.

So the answer to PROOF-2a's test — does the computed value reproduce the
mission's own 4-5-7 ranking — is that **the ranking is not a value judgement to
reproduce**. Any C that reproduces 4-5-7 is reproducing a cost ordering, and a
value term that agrees with it has most likely imported cost twice. The
reference's own proposal is the right one: report the ranking under each of
three named orderings (Rob-first, drift-first, general-second-implementation-
first) as a sensitivity, and never under a weighting nobody stated.
