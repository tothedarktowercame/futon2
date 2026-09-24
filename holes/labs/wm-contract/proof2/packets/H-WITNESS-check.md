# H-WITNESS-check — an independent check of the exemplar's step warrants

Packet: H-witness part (ii) of `PROOF-2-STRATEGY-draft-2026-09-24.md` —
"(ii) someone other than claude-1 checks each step's warrant against the
rewrite reading, with the checks recorded." The someone is kimi-4, who
authored none of the checked artefacts. Author: kimi-4, 2026-09-24.
Read-only on futon3c and futon3/library; nothing in either was modified.
Deliverable is this one file.

## 0. Scope and method

Scope statement (AR-36): a step warrant certifies the **rewrite** reading of
a pattern — an InterpretedPattern with consumes, forbids, produces over a
finite token state — not its forces (HOWEVER) and not its conditioning of
other patterns.

The exemplar is click-001 of M-futon-seams
(`/home/joe/code/futon3c/holes/labs/M-futon-seams/`). A "step" here is one
pattern interpretation inside a candidate derivation in
`exemplar/click-001.edn`. There are 14: seven in `:cand/a-registry-first`
(source `proto/instance-4.edn`) and seven in `:cand/b-observe-first`
(source `proto/instance-4b.edn`), over 10 distinct patterns (or3,
gauntlet, translation and realtime appear in both candidates). The
enactment (`click-001-enactment.edn`, author claude-10) attempts exactly
the seven patterns of cand/a and adds no new pattern citation.

For each step I checked, against the record and the library bytes:

1. **flexiarg file exists** at the receipt's `:source :path` under
   `/home/joe/code/futon3/library/`, and its current sha256 equals the
   receipt's `:source :sha256` (computed with `sha256sum`, all ten files,
   listed in §1).
2. **which reading the step actually uses.** Rewrite = the step names what
   it consumed and produced as tokens (`:guard {:needs … :forbids …}` and
   `:produces`), and its `:reading` prose states the token-level claim.
   Forces = the reading cites the pattern's HOWEVER as the reason.
   Conditioning = the reading uses the pattern to prime another pattern.
   All ten files contain a HOWEVER block (verified by grep), so a
   forces-reading was available to every step and was taken by none.
3. **receipt present**: `:author`, `:receipt :source {:path :sha256}`,
   `:reading`. Dates are not per-receipt; the file carries
   `:authored-at "2026-09-24"` and each candidate's `:source` carries
   `:pinned-at "2026-09-24, the click"`. The pin is therefore at file and
   candidate level, not per receipt — recorded, not treated as missing.
4. **verdict**: warranted under the rewrite reading / warranted under
   another reading only / no warrant found.

The annotations (`annotations.edn`, column :pattern, author claude-1)
were read as cross-references: notes i4-sites-enumerated, i4-grain,
i4-binding, i4-one-producer, i4-convert, i4-redirect match the cand/a
steps' readings (including the i4-convert revision replacing
memory/no-tightening-while-held with gauntlet/placenta-transfer, which the
click file reflects).

## 1. Pattern files: existence and hash check

All hashes computed 2026-09-24 with `sha256sum` on
`/home/joe/code/futon3/library/<path>`; "match" = equal to the receipt's
`:sha256` in `click-001.edn` (and to the `#<prefix>` pin in
`annotations.edn` where cited).

| pattern | file exists | sha256 (current bytes) | match |
|---|---|---|---|
| or3/count-every-card-back | yes | 9de888f56525ff68af9bf7e8814f3d4eb25bc448c2339a804f2c1753db386a62 | yes |
| cascade-construction/choose-the-grain-where-state-lives | yes | 4736e7268adfa373660acb949652cc7f2af8c065e661c4f4eb0d760fb683cc45 | yes |
| coordination/assignment-binding | yes | d20124da82dd1ff934ea1067f5626c154cbcf62f2487194e65c712db195253ee | yes |
| cycle-machine/single-producer | yes | 973389525453e7e94222fe87d870dd6246dc155975a7469f6441ba7bad187e65 | yes |
| gauntlet/placenta-transfer | yes | 9771eca50e93c42de6b1ea22e188c770635d62830ca190f5ae4ca18056a069cd | yes |
| translation/test-by-reproducing-behaviour | yes | 60442e7757a1aaaddc920969e5e4d965fe199ad47c7f149a70710e97f74282c4 | yes |
| realtime/mode-gate | yes | 5e505b53101854792852c33f2c27c73c37597a8f3de02af096bdfc714e8020d9 | yes |
| agency/single-routing-authority | yes | df6bf568dee5ded23e3cf08530605b12abe375a3351676fe082305713a1dbf07 | yes |
| iching/hexagram-49-ge | yes | 6e9f317cd451a4863296c0ed5d8e8158f2ec728ed245405fdb2d512da4dc0631 | yes |
| peripherals/read-only-first-then-extend | yes | ffe10550fc9d3c85725b0a086a1e9e3c5eae2f93319a67e90a073698022a1180 | yes |

10/10 exist, 10/10 hashes match, 10/10 files contain a HOWEVER block.

## 2. Step warrant table

Step = candidate / pattern. Consumed = `:guard :needs`; produced =
`:produces` (forbids is `#{}` on all 14 steps). Receipt = author + source
pin as in §0(3). All readings were read in full; the "reading used" column
is my classification, not the author's.

| # | step | consumed → produced (tokens) | reading used | receipt | verdict |
|---|---|---|---|---|---|
| 1 | a / or3/count-every-card-back | ∅ → :sites-enumerated | rewrite ("every occurrence is counted back before any is changed") | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 2 | a / cascade-construction/choose-the-grain-where-state-lives | ∅ → :roles-named | rewrite ("state lives at the role, not the seat and not the provider") | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 3 | a / coordination/assignment-binding | :roles-named → :binding-recorded | rewrite (explicit role→seat record; conflicting binding rejected) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 4 | a / cycle-machine/single-producer | :binding-recorded → :one-producer | rewrite (exactly one producer, contract test against real counterpart) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 5 | a / gauntlet/placenta-transfer | :sites-enumerated :one-producer → :caller-converted | rewrite (transfer the one function: deciding which seat plays a role) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 6 | a / translation/test-by-reproducing-behaviour | :caller-converted → :redirect-test | rewrite (redirect the binding, confirm behaviour; falsifier requirement) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 7 | a / realtime/mode-gate | :redirect-test → :prefix-routing-retired | rewrite (branches reachable behind a switch until the test passes, then both go) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 8 | b / or3/count-every-card-back | ∅ → :sites-enumerated | rewrite (same reading as candidate A) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 9 | b / peripherals/read-only-first-then-extend | ∅ → :roles-observable | rewrite (first instantiation observes and displays, does not write) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 10 | b / realtime/mode-gate | :roles-observable :sites-enumerated → :routing-gated | rewrite (routing through the view only behind an explicit switch) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 11 | b / agency/single-routing-authority | :routing-gated → :one-routing-path | rewrite (each agent-id resolves to exactly one routing path; single source of truth) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 12 | b / gauntlet/placenta-transfer | :one-routing-path → :caller-converted | rewrite (same function as A, transferred after the authority exists) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 13 | b / translation/test-by-reproducing-behaviour | :caller-converted → :redirect-test | rewrite (unchanged between the candidates) | claude-1, path+sha ✓ | warranted under the rewrite reading |
| 14 | b / iching/hexagram-49-ge | :redirect-test :one-routing-path → :prefix-routing-retired | rewrite (replace old with new; legitimacy = one authority, timing = test passed — both stated as consumed tokens) | claude-1, path+sha ✓ | warranted under the rewrite reading |

**14/14 steps: warranted under the rewrite reading. 0 warranted under
another reading only. 0 with no warrant found.**

Notes on the classification:

- No reading cites the pattern's HOWEVER as its reason. Step 14 is the
  closest call: iching/hexagram-49-ge's reading names "timing and
  legitimacy", which are the pattern's own forces-language — but the step
  grounds both in consumed tokens (:redirect-test, :one-routing-path), so
  it is a rewrite reading that happens to reuse the pattern's vocabulary.
  The step also records that candidate A reaches the same token by a
  "weaker reading" of realtime/mode-gate, which is a scope-limit statement
  of exactly the kind AR-36 wants.
- Several readings carry explicit non-transfer limits (step 1: "the
  counting discipline only"; step 5/A: "What does NOT transfer: the
  pattern's wider list of AIF functions the operator carries" in the
  matching annotation). These strengthen the rewrite reading rather than
  leaving it.
- Conditioning between patterns lives in the separate `:containment-order`
  and `:co-application-edges` of each candidate, not in any step's
  reading — as AR-36's scoping requires. The warrants do not lean on it.
- The one recorded scope caveat is structural, not per-step: the two
  candidates share an author and an hour (`:comparison :caveat`), so the
  field's independence is limited. The warrants themselves check out.

## 3. Phase exits against the step warrants

From `lifecycle.edn` (author claude-1, DRAFT pending claude-10's review;
read against the phases' own exit criteria):

| phase | status (lifecycle) | warranted step(s) behind it? |
|---|---|---|
| HEAD | :exit-met | none required — operator intake; no pattern cited, none expected |
| IDENTIFY | :exit-met | none directly — the exit act is the target choice (click-001 clause T, a `missing-definition` score, not a pattern step); the click's candidate field it picked over is built from the 14 warranted steps of §2 |
| MAP | :exit-met | none — the exit is counting and the ready/missing table; patterns enter only as counted inventory |
| DERIVE | :exit-met | yes — its artefacts are the cascades and their receipts: all 14 steps of §2 warranted; the phase's own step-3/step-8 checks (grain_check.py, clause C) sit on top of them |
| ARGUE | :in-progress | partially — FIT is argued via the warranted steps, but the exit is not claimed met (selection margins 2/7 from the problem statement; inevitability not argued). Warrants are necessary here, not sufficient. |
| VERIFY | :in-progress | yes, in substance — cascade_check.py re-verifies exactly the receipt properties of §1 (id resolves, id line matches path, receipt sha matches bytes) and passes 5/5; the phase exit is recorded as not-met only because the checks live in commits, not in a VERIFY section of the mission |
| INSTANTIATE | :exit-met (instance 4 scope) | yes — the enactment attempts the seven cand/a patterns, each warranted in §2; each success is an observed produced token (two tests, one grep) |
| DOCUMENT | :not-started | no — and none exists to warrant |

Every phase whose substance rests on pattern steps (DERIVE, VERIFY,
INSTANTIATE, and ARGUE's FIT half) has warranted steps behind it. No phase
exit is met on the strength of an unwarranted step. The two met exits that
do not rest on steps (HEAD, MAP) and IDENTIFY's target choice do not cite
patterns and are out of this packet's scope.

## 4. Constructed bad case

A lazy author's warrant for step 5 (cand/a, gauntlet/placenta-transfer):

```clojure
:gauntlet/placenta-transfer
{:produces #{:caller-converted}
 :author "claude-1"
 :receipt {:source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"}}}
```

Pattern name, produced token, no `:guard`, no source sha, no `:reading`.
Against this packet's table:

- §1 hash check: **no warrant** — no sha256 to compare; existence alone is
  not a pin (the file could be edited after the click and nothing would
  show).
- §2 "reading used": **no warrant** — no `:guard :needs`, so nothing is
  named as consumed; under the rewrite reading a step that names only what
  it produces is not an InterpretedPattern (consumes is part of the
  definition). With no reading prose there is nothing to classify as
  rewrite/forces/conditioning, and the row cannot be marked "warranted
  under the rewrite reading".
- Verdict column: **no warrant found**.

Contrast with the real step 5, which names two consumed tokens
(:sites-enumerated, :one-producer), and the difference is the whole point:
the real warrant lets an independent checker (a) confirm the file's bytes
and (b) re-derive that the step is enabled exactly when those two tokens
hold — both done above without asking the author anything. The lazy
warrant permits neither, and this table's columns reject it on two
independent rows before verdict is even considered.

## 5. Findings

1. 14/14 exemplar steps carry warrants that hold under the rewrite
   reading; every cited file exists and every receipt hash still matches
   the library's current bytes.
2. Receipts pin date at file/candidate level, not per receipt. Sufficient
   for this exemplar (one authoring date); worth a per-receipt
   `:pinned-at` if steps ever acquire distinct dates.
3. Typed absences recorded by the click itself (selection-posterior
   `:missing-definition`; W0 construction `:met-by-replay` with
   hand-authored interpretations stated) are consistent with what I found;
   no absence was substituted by a value anywhere in the checked records.
