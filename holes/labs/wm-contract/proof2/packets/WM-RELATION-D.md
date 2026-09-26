# WM-RELATION-D — deriving a machine-read target's relation (discovery, read-only)

claude-10, 2026-09-26, for claude-8 (packet WM-RELATION-D), on Joe's remark of
~01:50Z. Read at futon2 c7367eaa and futon3c db086d04.

Sources read:
- `resources/wm/focus/commit-facets-v1.json`;
- the structure embedding `futon6/data/mission-structure-embed/`. Its shas
  still match the facets file's pins: `57ab4aa4…` and `0c2d76be…`;
- the mission texts;
- the futon1b mission entity for M-autoclock-in (`GET :7073/api/alpha/entities?type=mission`).

**Not re-read:** futon1b's `relations` rows and the `mission-scope/map-item`
hyperedge. I rely on claude-8's reading for those.

No code, no test, no flight, nothing under `data/`.

## 1. The derivation, stated so it can be refused

**Today.** `classify-target` (`focus_receipt.clj:98-139`) reads a direct row.
For a T- target it reads the parent's row through `:derived-via`
(`{:kind :ticket-parent|:finding-target :parent p :source …}`). Otherwise it
returns `:unknown`, `:relation-not-declared`.

**Proposed, for an M- target with no row**, in this order, a hand row always
winning:

**(a) The Relations path.** The mission's stated relations, read as the
a-exits reader reads criteria: the `## Relations` section's lines naming
`M-*`/`C-*`/`E-*` ids or `[[M-…]]` links. The futon1b entity carries only
`:mission/relation "relates-to"`, not the targets, so the text is the source.

Follow them breadth-first to the first target that has a row, up to a stated
hop bound. Relations that cross campaigns and excursions are followed only
through M- targets. Result:

```
:derived-via {:kind :stated-relation
              :path [{:from "M-autoclock-in" :to "M-x"
                      :line n :quote "<the Relations line>"
                      :source {:repo … :path … :sha256 …}} …]
              :parent "M-y"}
```

The class and facet are the rowed target's. That is the same move the T-
branch makes through a ticket's Parent line.

**(b) The embedding.** If no path reaches a row, take the nearest classified
neighbour in the structure embedding, by cosine over the 161-dim rows. Result:

```
:derived-via {:kind :embedding-neighbour
              :neighbour "M-aif4iad" :cosine 0.378
              :candidates [[t cosine] …]      ; every classified target in the embedding
              :pins <the facets file's :embedding :source-pins>}
```

The class and facet are the neighbour's.

**(c) Neither.** `:unknown` with its reason, as today. The reasons are split:
- `:relation-not-declared`;
- `:no-stated-path-to-a-classified-target`;
- `:embedding-node-not-retained`, when the target has no stem among the 198;
- `:nearest-below-threshold`, carrying the cosine and the threshold.

**The receipt** is the one `:derived-via` above: the quoted Relations lines
with the text's sha, or the neighbour, its cosine and the candidate list
under the existing pins. `classification` (`:141-152`) already records
`:derived-via` on the receipt, so the shape is an extension of an existing
key.

## 2. Is this a guess?

By definitions, no.

**What the ruling excludes.** codex-20's "never guessed"
(`focus_receipt.clj:105-106`, `observation_model.clj:209-214`) rules out a
value standing in for an absence: stop-the-line, a worst case, an average, a
uniform, an exclusion. None of those carries evidence of the relation.

**Why (a) and (b) differ from that.** Each is a reading of the corpus with
its path, or its number, on the record. Anyone can check it against the
sources and refuse it. That is the footing the criteria reading and the
T- branch's `:derived-via` already stand on.

**Where the line falls: the threshold.** "Nearest" with no floor would
classify anything, since every stem has a nearest neighbour. A floor is a
new number, and no source declares one today.
- **Who declares it:** it belongs in the facets file beside the pins, as
  `:embedding :min-cosine`, stated by whoever owns that file's rules
  (improve-7's `discover.py` lineage). It should not be a literal in the code.
- **Below the floor:** `{:class :unknown :relation {:status :absent :reason
  :nearest-below-threshold :nearest "M-…" :cosine c :min-cosine m}}`.
- **With no floor declared:** (b) should not fire, and the reason is
  `:embedding-threshold-not-declared`. The number is typed absent, never
  defaulted.

**Path (a) needs no number.** It does need a hop bound. The bound governs
how far the derivation walks, not what it concludes, so a stated default
recorded on the receipt is less contentious than a similarity floor.

## 3. What (a) and (b) give for M-autoclock-in today

**(a) fails at two hops.** M-autoclock-in's `## Relations` names five
things:
- C-substrate-completion §8.1 (a campaign);
- the clock-in hydra (a UI);
- the mention map;
- E-the-dark-tower-2 (an excursion);
- `[[M-operational-vocabulary]]`.

**Hop 1:** M-operational-vocabulary (`futon2/holes/M-operational-vocabulary.md`)
has no row among the nine.

**Hop 2:** its Cross-ref line (`:7`) names M-aif-wiring, M-wm-policies,
M-populate-substrate-2 and M-a-sorry-enterprise, and its body names
M-war-machine (`:61`) and M-goals-and-holes (`:68`). None of these has a
row either.

The nine rowed targets:

| target | relation | facet |
|---|---|---|
| M-aif-policy-conditioned-eig | focus | WM |
| M-f11-find-production-successor | focus | WM |
| M-wm-08-external-f2 | focus | WM |
| M-G-wm-wiring | focus | WM |
| M-wm-aif-policy-grain-compliance | focus | WM |
| M-apm-capability-ratchet | focus | APM |
| M-action-cost-modelling | associated | WM |
| M-futonzero-generative | associated | WM |
| M-aif4iad | associated | WM |

M-f11 appears in M-autoclock-in's text only in an evidence table (`:337`),
not in its Relations, so it is not a stated relation.

The futon1b entity `38569df0-b95f-410f-bfbd-b227dbe0aaa8` carries
`:mission/relation "relates-to"` and no target list.

**(b) gives `associated`/WM via M-aif4iad at cosine 0.378.** I recomputed the
cosines over the pinned files and they match claude-8's list:
- nearest overall: substrate-metric 0.736, war-machine-vsatarcs-interop 0.683,
  war-machine-first-outing 0.549, webarxana 0.507, intent-curvature 0.496,
  the-perfect-crime 0.490;
- war-machine 0.007.

**Only three of the nine rowed targets are in the embedding:** aif4iad
0.378, futonzero-generative 0.112, action-cost-modelling 0.100. The six focus
targets have no stem among the 198 (the snapshot is 2026-06-25). So (b) can
only ever reach an `associated` class through this embedding. A focus
classification by embedding would need a newer embedding, with its own pin.

The derived class is therefore `associated`/WM through
`:embedding-neighbour` M-aif4iad 0.378, **if** the declared floor is at or
below 0.378. It is `:unknown :nearest-below-threshold` otherwise.

For scale, the neighbours above 0.5 have no rows, so they cannot decide a
class.

## 4. Cost under IDENTIFY 4

- **Refusals:** no new refusal. The existing refusal
  `:class-unknown-no-scalar-g` is reached less often, and the gate's
  `allowed-refusal-kinds` is untouched.
- **Receipt:** one new receipt shape, two new `:kind` values under the
  existing `:derived-via`.
- **Declared number:** one new declared number (`:min-cosine`), which is the
  part to refuse or accept.
- **Map:**
  - A box for `classify-target`, at `focus_receipt.clj`. The map has none
    today; the class reaches scoring through `war_machine.clj:6521-6589`, not
    through any row-9 box. It gains declared reads of the embedding files and
    the mission text.
  - `:r9-selection-law`'s declared writes do not change.
- **Substrate relations:** not read. The text is the source, since the
  entity carries none.
- **The pin:** the facets file's `:embedding :source-pins` is enough while
  the embedding files stay put. The derivation re-hashes the two embedding
  files at read time and records `:pins-match true`, or refuses (b) with
  `:embedding-pin-mismatch`. No per-click pin beyond that is needed. A
  refreshed embedding is a new facets-file version.

## 5. Recommendation and size

**Build (a) and (b) in `focus_receipt.clj`** as one packet:
- `stated-relation-path` (the Relations reader plus the bounded walk);
- `embedding-neighbour` (reads the two pinned files, checks the shas,
  computes cosines, applies `:min-cosine`);
- `classify-target`'s new branch for M- targets, after the direct row and
  before `:unknown`.

`classification` needs no change: it already records `:derived-via`.

**Put `:min-cosine` to Joe, or the facets file's owner, as a number to
declare.** Until it is declared, (b) returns
`:embedding-threshold-not-declared` and M-autoclock-in stays `:unknown`.
That is honest, but it doesn't get the click past scoring. The declaration
is the one decision this packet can't make.

**Test box `futon2.aif.relation-derivation-test`**, live-pinned on
M-autoclock-in's Relations lines and the pinned embedding's cosines. The bad
cases come first:
1. A target whose only route is the embedding, below the floor, is `:unknown
   :nearest-below-threshold`, with the cosine recorded.
2. A target with a hand row keeps it, even with a stated path to a different
   class.
3. A T- target is unchanged, with the ticket-parent derivation.
4. A pin mismatch refuses (b), typed.
5. M-autoclock-in derives `associated`/WM via M-aif4iad 0.378 at a floor
   ≤ 0.378.

**Falsifier on a fifth flight:**
- The click's classification of M-autoclock-in reads `:class :associated`
  with `:derived-via {:kind :embedding-neighbour :neighbour "M-aif4iad"
  :cosine 0.378 …}`, and the scorer class is `:related`.
- The tick then reaches selection, or refuses at the next registered kind.
  With WM-CLICK-REFUSAL-I in the serving JVM, that refusal is recorded as a
  typed abstention.
