# C514 — I4's two registry writes: source recovery and drafts

**Scope.** Discovery and drafting only. This note does not modify
`aif-equations.edn`, `control-map-edges.edn`, a conformance record, code, or the
WM board. The filing decision is claude-1's, under the I4 reviewer decision in
`EPIC-run-era.md:874-885`.

## 1. The 2026-07-06 ruling is recoverable

The original contemporaneous record is `holes/E-live-loop-3.md:2160-2165`,
introduced by futon2 commit `f93a2591042e716dbdacab31f47d4e9f21a6302d`
at `2026-07-06T11:20:42+01:00`. It labels the sentence “Operator ruling
(2026-07-06, verbatim)” and records:

> “Yes, we should accept the decision so that the machine can act on its
> decision.”

The same commit's message repeats the sentence and records the implementation
consequence: the cascade lane's first entry becomes the judge's top decision
target. The same-day pipeline card independently labels the lower-case version
“RULED (operator, verbatim)” at `holes/flight-pipeline-cards-ii.html:146-152`.
The excursion log then describes the implementation at
`holes/E-live-loop-3.md:2184-2194`: the default-on flag prepends the rank-1
decision target before the existing open-mission entries.

### Search trail

The searches were run from `/home/joe/code/futon2`; their output was not
truncated for the conclusions below.

```sh
git log --all --since='2026-07-04' --until='2026-07-09 23:59:59' \
  --date=iso --format='%h %ad %s' -- scripts/futon2/report/cascade_lane.clj

git log --all --since='2026-07-04' --until='2026-07-09 23:59:59' \
  --date=iso --format='%h %ad %s' \
  --grep='rollout\|cascade\|depth\|top-k\|gamma' -i

rg -n '2026-07-06|July 6|depth 5|top-k 3|gamma 0\.9|best-rollout' \
  holes scripts src docs -g '*.md' -g '*.edn' -g '*.clj' -g '*.bb'

git log --all \
  -S'Yes, we should accept the decision so that the machine can act on its decision' \
  --format='%h %aI %s' --pickaxe-all

find holes -type f -newermt '2026-07-04' ! -newermt '2026-07-09' -print0 \
  | xargs -0 rg -n -i \
      'accept the decision|machine can act on|decision target|gate.*decision'

git show f93a2591 -- scripts/futon2/report/cascade_lane.clj
git show f93a2591:holes/E-live-loop-3.md
git blame -L 2160,2170 -- holes/E-live-loop-3.md

rg -n -i 'accept the decision|machine can act on|gate-decision-target' \
  holes/labs/wm-contract/aif-equations.edn \
  /home/joe/code/p4ng/empirics-futon/control-map-edges.edn
```

This search found no separate source commissioning the current numeric rollout
parameters `:depth 5`, `:top-k 3`, `:gamma 0.9`, or `:authority :diagnose`.
Those current lines were added on 2026-09-02 (`git blame -L 362,390
scripts/futon2/report/cascade_lane.clj`, commits `e76c51c9` and `2be0b9ed`), so
they cannot be the implementation of a July 6 ruling. The relevant July 6
realisation is the default-on decision-target gate. C475's pointer
`cascade_lane.clj:381-389` was correct at its revision but has drifted: today
the ruling-bearing flag is at `scripts/futon2/report/cascade_lane.clj:411-419`,
and its behavior is consumed at `:434-440`; today's `:381-389` is rollout
scoring and event collection.
The last registry search returned no match, confirming that neither current
`:choices` nor `:decisions` records this ruling.

## 2. Draft correction for RUN3's conformance record

**File here:** append the following immediately after the paragraph at
`holes/labs/wm-contract/runs/2026-09-01-s1b/CONFORMANCE.md:20-26`, which is the
RUN3 prose record that currently interprets `R5→R6` as
`:ruling-unrealised`. Preserve the generated `conformance.edn` as the
historical output; this is a correction to what its classification means, not
a rewrite of the receipt.

> **Correction (2026-09-05, I4/C514).** The `R5→R6` route hop does not show
> that the `:r6-r14-order` ruling was unrealised. The hop's `R6` tag names
> `futon2.aif.policy/select-action` (the ranking-to-selection relation; current
> `scripts/futon2/report/war_machine.clj:6450-6470`), whereas the retired
> `[:R5 :R6]` edge names catalogue R6-C, the candidate action space. That
> candidate space is constructed before R5 and is not route-tagged. RUN3's
> classifier at `run3_conformance.bb:116-124` applies its grain proxy only
> when a `:code` retirement is also in `:route-measured-drawn`; it cannot
> distinguish R6-C from the Q(π)/selection R6 relation for this
> `:ruling` retirement. Therefore `:ruling-unrealised [["R5" "R6"]]` is a
> mechanically reproduced historical field but is not evidence that the
> retired catalogue relation ran. The still-unrealised clause—construct the
> cascade for the committed target—has no route signature and was not exercised
> by this run because advisory lanes were disabled. See
> `C475-cascade-order-disposition.md:54-96`.

The pointer deliberately names the current call/tag region rather than the
now-stale `war_machine.clj:5113` supplied in I4's blocker. C475 records the
same call and tag at its then-current `:5101-5113` (`C475:54-60`).

## 3. Draft registry entry for the July 6 ruling

**Recommended home: `aif-equations.edn :choices`, key
`:cascade-decision-target`.** This is a free choice about which target the
cascade/act gate evaluates, analogous to the `:policy-grain` choice at
`aif-equations.edn:175-187`. It is not a decision about adding or retiring a
control-map edge. Filing it under `control-map-edges.edn :decisions` would
make a behavioral target-selection ruling look like topology and recreate the
relation/grain confusion corrected above.

Draft, in the existing `:choices` shape:

```clojure
:cascade-decision-target
{:observed :cascade-lane-prepends-ranked-decision-target
 :status :decided
 :ruling
 {:by "joe"
  :at "2026-07-06"
  :grounds :ruling
  :channel "session, recorded contemporaneously in holes/E-live-loop-3.md:2160-2165; Joe verbatim: 'Yes, we should accept the decision so that the machine can act on its decision.'"
  :decision "The cascade gate must evaluate the machine's decision target, not only the :open-mission side-stream. Prepend the judge's rank-1 decision target as lane entry 1 regardless of action type, deduplicating it when the existing open-mission lane already contains that target."
  :consequence "The default-on *gate-decision-target?* branch was introduced by futon2 f93a2591. It prepends decision-entry before the top-n open-mission entries; binding the flag false preserves the pre-ruling composition. Later I4 discovery shows that 'rank-1 decision' here means the head of the R5 ranking, not the downstream committed R14 target, so this entry records the ruling and its historical realisation without claiming the later committed-target requirement is met."}
 :evidence "futon2 f93a2591042e716dbdacab31f47d4e9f21a6302d; holes/E-live-loop-3.md:2160-2194 (verbatim ruling, finding, change); holes/flight-pipeline-cards-ii.html:146-152 (same-day verbatim card); scripts/futon2/report/cascade_lane.clj:411-440 (current flag and lane contract); C475-cascade-order-disposition.md:76-96 (realised against ranking head, not committed target)"
 :statement "Before the ruling, cascade-lane filtered for :open-mission actions and could exclude the judge's top decision when it had another action type. The July 6 ruling closed that omission by requiring the lane to accept the decision target. The implementation exists and is default-on, but its input is the ranking head; this entry does not identify that value with the mission the later strategic selector commits to."}
```

This draft does not assign the July 6 ruling to the September 2 rollout
parameters. No recoverable commissioning text for those four parameters was
found in the requested July 4–8 search window.
