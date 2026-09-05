# C535 — U64: where the rollout parameters came from

**Row:** `worklist.edn :U64` (class `:V`). **Date:** 2026-09-05. **Scope:**
discovery and drafting only. This note writes no ruling: `aif-equations.edn
:choices` and `p4ng/empirics-futon/control-map-edges.edn :decisions` are
byte-unchanged, and the filing decision is the owner's (claude-1). Every claim
below carries a `file:line` pointer, a commit sha, or **not found**.

## 0. The row's premise is wrong about three of its four parameters

U64 says the four parameters were "added 2026-09-02 (`e76c51c9`, `2be0b9ed`)",
inheriting that from `C514-i4-registry-drafts.md:60-64`, which read them off
`git blame -L 362,390 scripts/futon2/report/cascade_lane.clj`. Blame is right
about the *lines* and wrong about the *values*: `e76c51c9` split
`policy-rollout` into `policy-rollout-result` and `2be0b9ed` re-wrapped the
call, so both re-authored a line that already carried `:depth 5 :top-k 3
:gamma 0.9`.

The diff shows it directly — `git show e76c51c9 -- scripts/futon2/report/cascade_lane.clj`
carries `best-rollout seed mv :depth 5 :top-k 3 :gamma 0.9` on **both** the `-`
and the `+` side. Only `:authority :diagnose` is new on 2026-09-02 (`2be0b9ed`,
the sole commit in the whole history whose pickaxe hits `:authority :diagnose`
in that file — search S3 below).

So the four parameters are three different provenance cases, not one, and the
draft in §4 keeps them apart.

| parameter | entered the production lane | overrides the library? | grounds |
|---|---|---|---|
| `:depth 5` | `ddcc558f` 2026-06-24 | **yes** — library default 2 (`rollout.clj:474-479`) | not found |
| `:top-k 3` | `ddcc558f` 2026-06-24 | **yes** — library default 5 (`rollout.clj:691-693`) | not found |
| `:gamma 0.9` | `ddcc558f` 2026-06-24 | **no** — restates the library default (`rollout.clj:481-485`) | not found (value); shape cited at `rollout.clj:487-488` |
| `:authority :diagnose` | `2be0b9ed` 2026-09-02 | declares what was previously the silent default | **recoverable** — see §3 |

## 1. The search trail

Run from `/home/joe/code/futon2`; no output was truncated for the conclusions
drawn from it. Searches S1–S6 are this row's; the C514 trail they extend is at
`C514-i4-registry-drafts.md:33-56`.

```sh
# S0 — which commit actually introduced the triple into the production lane
git log --all --reverse --format='%h %aI %an %s' \
  -S':depth 5 :top-k 3 :gamma 0.9' --pickaxe-all \
  -- scripts/futon2/report/cascade_lane.clj
git show e76c51c9 -- scripts/futon2/report/cascade_lane.clj    # triple on both sides
git show ddcc558f -- scripts/futon2/report/cascade_lane.clj    # triple on the + side only

# S1 — every commit in the 2026-09-02 window whose message names a parameter
git log --all --since='2026-09-01' --until='2026-09-04 23:59:59' \
  --date=iso --format='%h %ad %s' --grep='depth\|top-k\|gamma\|horizon\|discount' -i

# S2 — the triple, anywhere in the tree, in that window
git log --all --since='2026-09-01' --until='2026-09-04 23:59:59' \
  --format='%h %aI %s' -S':depth 5 :top-k 3 :gamma 0.9' --pickaxe-all

# S3 — :authority :diagnose at the call site, all time
git log --all --reverse --format='%h %aI %s' -S':authority :diagnose' \
  --pickaxe-all -- scripts/futon2/report/cascade_lane.clj

# S4 — the two registries the lanes consult
rg -n -i 'depth 5|top-k|:gamma|discount|rollout.*param|policy-depth' \
  holes/labs/wm-contract/aif-equations.edn \
  /home/joe/code/p4ng/empirics-futon/control-map-edges.edn

# S5 — the decision registers
rg -n -i 'depth|top-k|gamma|rollout' holes/problems/DECISIONS-REGISTER.md \
  holes/problems/DECISIONS-PENDING.md holes/problems/DECISIONS-WORKING-ORDER.md

# S6 — prose forms, literal and hyphenated, over every note and page
rg -n -i 'depth 5|top-k 3|gamma 0\.9|horizon 5|discount 0\.9|discount factor' holes docs -g '*.md' -g '*.html'
rg -n -i 'depth-5|depth of 5|five-step|top-3|top 3 |k=3|horizon of 5|discount of 0\.9|gamma of 0\.9' holes docs -g '*.md' -g '*.html'

# S7 — where the values were before the production lane
git log --all --reverse --format='%h %aI %s' -S':top-k 3' --pickaxe-all
git blame -L 50,56 --date=iso -- holes/labs/e-rollout-v2-e2e.clj
```

**S1 and S2 returned nothing on the parameters.** S2 is empty: no commit in
2026-09-01..09-04 introduced the triple anywhere in the tree. S1's thirteen
commits are U53, U47, U26, U35, D14, U6, U3, U2, D61a and the zaif-harness
D8/D10/D12 rows — the one `gamma` among them (`9e62d4ae`, "silent gamma
default") is the zaif harness's own `:gamma-source`, a different object at a
different seam, and is not this `gamma`.

**S4 and S5 returned nothing that commissions a value.** The decision registers
carry O8 (`holes/problems/DECISIONS-REGISTER.md:91`) and O9 (`:92`) — the *typing* of the
rollout producer, which is what `e76c51c9`/`2be0b9ed` implement — and the
2026-09-02 ruling they cite (`holes/problems/DECISIONS-PENDING.md:350-373`)
decides typed absence, malformed-stays-loud and the refuse floor. It says
nothing about search depth, branching width or discount.

## 2. Where the three numbers actually come from: a smoke-test witness

`ddcc558f` (2026-06-24, "M-wm-policies Car-3 seams 1+2") added
`rollout-g-for` with the triple already in it. The identical triple exists
fifteen days earlier in a witness script:
`holes/labs/e-rollout-v2-e2e.clj:53`, committed at `3d41d269` (2026-06-09,
"Add v2 scope-grain end-to-end witness"). The comment on the line above it
says what the 5 was for — `holes/labs/e-rollout-v2-e2e.clj:50`: "the
hypergraph-operator depth-5 chain via the actual search". The chain it means is
named in the mission log at `holes/M-wm-policies.md:764`: "a real **depth-5
policy** — `hypergraph-operator: derive→argue→verify→document→instantiate`",
i.e. the five-stage mission lifecycle, reported again at `:1041` and as
completion criterion 4 at `:1157`.

`:top-k 3` is older still: it first appears at `65f137d0` (2026-06-09, "Add
policy rollout engine") in that commit's own witness,
`holes/labs/e-rollout-witness.clj:61-62`, where it sits beside `:depth 2`.

**What this establishes and what it does not.** It establishes a chain of
transmission: the production lane's parameters are a witness's demonstration
parameters, copied forward. `:depth 5` in the witness was chosen to *reach* a
chain already known to be five long — a demonstration setting, valid for
exhibiting that the multi-step search finds it. It does **not** establish
grounds for running a live report lane at that depth. No text was found that
argues 5 is the right search horizon for the lane, or 3 the right branching
width. That absence is not for want of the habit: two lines above the
`:cascade-policies` lane's own settings, `holes/M-wm-policies.md:747` records
"Budget=6 from the marginal-coverage data" — grounds, for a neighbouring
number, in the same paragraph.

**A conflation to avoid.** `holes/M-wm-policies.md:750` says the judgement is
served "(top-3, budget-6)". That 3 is the *lane's* `top-n` — how many
`:open-mission` targets get a cascade built (`cascade_lane.clj:445`) — not the
rollout's `:top-k`. Two different threes.

**What the record does say about `top-k`, and it is about role, not value.**
`holes/E-policy-rollout-engine.md:105-107` and `holes/M-wm-policies.md:826-829`
both rule that expansion is `:prior`-weighted PUCT-style and "top-k is mere
truncation". So the *semantics* of the knob are commissioned; its setting is
not.

**`:gamma 0.9` is not a call-site choice at all.** `rollout-discount`
(`rollout.clj:481-485`) already defaults `gamma` to 0.9, so passing it or
omitting it at `cascade_lane.clj:381` gives the same number. Its own origin is
`65f137d0`, whose entire commit message is "Add policy rollout engine". The
nearest thing to a citation is `rollout.clj:487-488`, "Port of ukrn's path
accumulator shape: S(pi)=sum gamma^t g(s_t)" — a source for the **shape**
`Σ γ^t g(s_t)`, not for the **value** 0.9.

## 3. `:authority :diagnose` has recoverable grounds and is not a fiat

Its commissioning is recorded three ways and they agree.

- **The ruling it lands under:** `holes/problems/DECISIONS-PENDING.md:350-373`,
  Joe 2026-09-02, deciding all seven C130 migrations conditional on
  refusal-feedback; O9 (`holes/problems/DECISIONS-REGISTER.md:92`) is the register row.
- **The code-backed reason:** the declaration is a claim about this call site —
  it reads the score and never enacts the returned `:policy` — stated in the
  producer's own docstring at `scripts/futon2/report/cascade_lane.clj:368-371`
  and audited at `C482-unscored-move-refuse-floor.md:126-140`, which checks the
  claim against the code ("**Audited: there is no authorizing caller today**")
  and records why a default is not a claim (`:authority-declared?`).
- **The implementing commit:** `2be0b9ed`, whose message states the same and
  names the two floor conditions.

This is the one of the four that should **not** be offered to Joe as an
unadjudicated fiat, and separating it is the point of §0's table.

## 4. Draft `:choices` entry — NOT FILED

Recommended home `aif-equations.edn :choices`, key
`:rollout-search-parameters`, in the shape of `:policy-grain`
(`aif-equations.edn:206-214`). `:status :observed-not-decided` per the TN
standard's clause 3 (`holes/TN-edge-review-aif-wiring.md:38-42`): until Joe
rules, the entry records what the machine is observed to do.

It is a separate key from `:policy-depth` (`aif-equations.edn:370-372`) rather
than an amendment to it, because that entry is about *T varying by
configuration* and its evidence names only the judge's 3 and the library's 2 —
it does not name this call site's 5 at all. That omission is independently
recorded: `C525-F8-depth-lean.md:119-124` found `cascade_lane.clj:381` as a
fourth declared depth that the F8 dispatch packet had missed. Whether
`:policy-depth`'s evidence line should be corrected to include it is a registry
edit and so is the owner's, not this row's.

```clojure
:rollout-search-parameters
{:observed :witness-settings-carried-into-a-live-lane-uncommissioned
 :status :observed-not-decided
 :statement "The one production caller of best-rollout runs the search at :depth 5, :top-k 3, :gamma 0.9 (cascade_lane.clj:381-382). Two of the three override the library defaults -- horizon 2 at rollout.clj:474-479 and top-k 5 at rollout.clj:691-693 -- and the third restates the library default (rollout.clj:481-485), so passing it changes nothing. No text commissioning any of the three values was found (C535 searches S0-S7). They are the settings of a smoke-test witness, holes/labs/e-rollout-v2-e2e.clj:53 at 3d41d269 (2026-06-09), carried into the production lane fifteen days later at ddcc558f (2026-06-24) with no accompanying argument. The witness's own comment says the 5 was set to reach a chain already known to be five steps long (holes/labs/e-rollout-v2-e2e.clj:50; the chain at holes/M-wm-policies.md:764), which is a demonstration setting rather than a search-horizon choice. :top-k 3 is older, from the engine's first witness at holes/labs/e-rollout-witness.clj:61-62 (65f137d0). What IS commissioned is the knob's role, not its setting: expansion is :prior-weighted PUCT-style and 'top-k is mere truncation' (holes/E-policy-rollout-engine.md:105-107; holes/M-wm-policies.md:826-829)."
 :consequence "These three set :policy-rollout-score, which cascade-lane projects onto every lane entry (cascade_lane.clj:479 and :541) and close-loop reads as the act gate's :coverage-score-delta whenever the semilattice fold is absent, recording :coverage-score/source :policy-rollout (close_loop.clj:95-105). So the settings reach a gate. They have not yet moved a recorded gate number: over the 58 files of data/wm-trace, 0 carry policy-rollout-score and 0 carry G-rollout; :coverage-score-delta appears in one file (data/wm-trace/wm-trace-2026-07-14.edn) with all 4 occurrences nil, and no record carries a :coverage-score/source key at all."
 :reversal "Deleting :depth 5 and :top-k 3 from cascade_lane.clj:381 returns the lane to the library defaults (horizon 2, top-k 5) and changes the ΔG number the act gate would read; deleting :gamma 0.9 changes nothing. No persisted artifact would change today, by the census above."
 :not-in-this-entry ":authority :diagnose, which the same line carries, is NOT one of these. It has recoverable grounds -- Joe's 2026-09-02 ruling at holes/problems/DECISIONS-PENDING.md:350-373 (register row O9, holes/problems/DECISIONS-REGISTER.md:92), implemented at 2be0b9ed, with the code-backed claim audited at C482-unscored-move-refuse-floor.md:126-140 -- and is recorded here only so that it is not swept in with the three."
 :for-joe "Three numbers on a live lane with no argument behind them. The available dispositions are: (a) ratify the settings as they stand, which makes them a choice rather than an inheritance; (b) veto them back to the library defaults; (c) rule that the lane's horizon must be derived from something -- the cascade length it scores, a budget -- rather than set. Nothing here recommends one; the entry exists so the question is visible in the registry the lanes consult."
 :evidence "cascade_lane.clj:381-382 (the call); rollout.clj:474-479, :481-485, :691-693 (the three library defaults); rollout.clj:487-488 (the shape's citation, not the value's); futon2 ddcc558f (into the production lane), 3d41d269 (the witness), 65f137d0 (the engine and :top-k 3); holes/labs/e-rollout-v2-e2e.clj:50-53; holes/labs/e-rollout-witness.clj:61-62; holes/M-wm-policies.md:747, :764, :826-829; holes/E-policy-rollout-engine.md:105-107; close_loop.clj:95-105; cascade_lane.clj:479, :541; C535-U64-rollout-parameter-provenance.md (this note's search trail)"}
```

## 5. What this row did not do

No registry write: `aif-equations.edn` and
`/home/joe/code/p4ng/empirics-futon/control-map-edges.edn` are byte-unchanged.
No ruling. No code change — `cascade_lane.clj` and `rollout.clj` are untouched,
so the three settings still stand exactly as found. `gen_aif_dag.bb` not run
and nothing regenerated into a publish (TN §9a). No machine run, no run lock,
nothing written under `data/` — the corpus census in `:consequence` is a
read-only `grep` over `data/wm-trace`. `:policy-depth`'s evidence line is left
as it is, with its omission of this call site reported in §4 rather than
repaired.
