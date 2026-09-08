# Receipt: mining-l2-policy-grain (M3, cascade 3 of 6)

Target: the 3-th mapped item of runs/W1-witness-survey.md's mapped set in
survey order — **L2** (`runs/W1-witness-survey.md:129-145`): "scheduler
actions and cascade policies were scored at different grains", mission
`M-wm-aif-policy-grain-compliance`.

## Relationship to the exemplar

This mission is the exemplar's own target (runs/mining-exemplar/, futon2
e059f0a5, reviewed, format held). The first pass of this row therefore cited
the exemplar and wrote nothing; the reopen review held that the exemplar does
not satisfy THIS row's stricter contract (edges not class-marked, THEN spans
including the marker line, no :transcript). Under the RESTATED statement
(claude-1, 2026-09-08: mission-span DOCUMENTED edges with class in-band), this
pass re-derives the record rather than duplicating the exemplar:

- **Edges carry `:interpretation-class :documented` in-band**, each with a
  `:documentation` basis naming the authoring mission spans and the verified
  annotation-absence (no pattern-to-pattern @why/@how at pin ab277bdd).
- **THEN spans are content lines only at the CURRENT pin ab277bdd**:
  candidate-pattern-action-space content :27 (marker :26),
  hierarchical-and-temporal-depth content :33-38 (marker :32),
  expected-free-energy-scorecard content :28 (marker :27). The exemplar's
  marker-inclusive ranges are superseded, noted per-rule in `:span-note`.
- **`:transcript` added** from git history: mission founding at futon2
  edb50edc 2026-07-23 (mission header date), the F7 dark-mode decision record
  (9d70a8b4 2026-09-05), the :B2 adoption 27a6dd5b 2026-09-05 whose four
  items remain UNCHECKED — so the cascade honestly records the DARK-MODE
  PARTIAL solution the mission itself bounds, per the survey's own evidence
  kind ("mission records a tested partial implementation, not live
  compliance").
- Rule interpretations remain `:attested` on their mission attestations
  (unchanged from the exemplar — the reopen finding challenged edge backing,
  not the rule attestations).

## Gate reproduction

```sh
bb -e '(clojure.edn/read-string (slurp "cascade.edn"))'      # exit 0
grep -n "+ THEN:" library/aif/{candidate-pattern-action-space,hierarchical-and-temporal-depth,expected-free-energy-scorecard}.flexiarg   # 26 / 32 / 27 at ab277bdd
git log --format="%h %ad %s" --date=short --follow -- holes/missions/M-wm-aif-policy-grain-compliance.md   # edb50edc 2026-07-23, 27a6dd5b 2026-09-05
```

Trail status: complete commands, no truncation; all shas/dates read from git
output this invocation, none from memory (the M1 lesson, applied).
