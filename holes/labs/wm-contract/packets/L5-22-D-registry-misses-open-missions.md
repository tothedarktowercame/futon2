# L5-22-D — Discovery: the substrate mission registry misses 15 open missions (claude-2 → codex-14, 2026-11-01)

Read-only; report only; worktree `~/worktrees/futon2/codex14-l5-22-d`, branch `codex14/l5-22-d`
(never check out a branch in `/home/joe/code/futon2`). Report to
`holes/labs/wm-contract/reconciliation/L5-22-D-registry-misses-open-missions-codex14-2026-11-01.md`.

## What Q1 actually fails on

Lean Q1: `enumeratedTasks = openTasks`. On click `wm-click-3c866bd4` (`data/wm-runs/selection-record-
2026-11-01-wm-click-3c866bd4-…edn`) the independent file census (`:world-census`, 653 open) and the
field's enumeration (639) differ by exactly these sets, computed from the record:

- census, not enumerated (15): M-a4a-structure-node, M-aif-over-the-operator, M-case-studies,
  M-descriptive-essay-of-the-stack, M-fold-ansatz, M-lab-standard, M-peeragogy-rewrite,
  M-populate-substrate-2, M-stack-essay-code-alignment, M-stack-morphogenetic-rewrite, M-text-sidecar,
  M-war-machine-aif-completion, M-wm-tripwires, M-zaif-harness, M-象-cascade. Every one has a file
  whose header says OPEN (futon2/holes, futon3c/holes/missions, futon7/holes, futon5/holes/missions,
  futon4/holes/missions), and **none appears in the record's `:registry/non-open`** either: the
  substrate registry does not know them at all.
- enumerated, not census (1): M-ukrns-wp — in the registry as live, with no file found under any
  `holes/` or `holes/missions/` of the futon repos.

The operator-gated missions are **not** the gap: both census and field already leave them out
(13 in the pass's `excluded.edn` as `:operator-gated`). The decision I had queued for Joe on them is
moot for Q1.

## Questions

1. The field's roster comes from `futon2.aif.outer-observation` `open-entries` →
   `mission-registry/load-missions` (`:registry-source :substrate`). What populates that registry
   (which ingestion, which roots, when it last ran), and why are the 15 files absent from it: roots
   not covered (`futon7/holes/*.md`? `futon3c/holes/missions/`?), a status parser that rejects their
   header shapes (`OPEN — OPEN — …`, `MAP (opened …)`, a `null` status for M-象-cascade), or ingestion
   not run since they were written? Cite the code and a live query.
2. M-ukrns-wp: where is its registry entry from and where did the file go (git log across repos)?
3. The census (`selection-world/capture-census`, `:source :files`) and the registry read different
   things. For Q1 to be meaningful the two must be independent reads of the same truth. State the
   two candidate fixes with their consequences: (a) repair the registry ingestion so it covers the
   same roots and header shapes as the census; (b) the field takes its roster from the file census
   and uses the registry only for pins. Say which keeps Q1 an independent check.
4. Count: how many of the 15 are in Giraffe cones today (e.g. M-descriptive-essay-of-the-stack carries
   the cone of 9 under the proxy-metric scaffold) — i.e. the ranking already depends on tasks the
   field cannot pick.

Bell claude-2 with the sha and the four answers in brief.
