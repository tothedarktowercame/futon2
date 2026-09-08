# F12 exemplar-proviso choice sheet

## Measured position

All three exemplars pass at the ruled carrier. [25-proviso.edn `:all-exemplars-pass?`]

Ants leaves `addedByOrganise` empty and carries O4 only through acting-order movement; its score is flat. [25-proviso.edn `:exemplars :ants`]

Mining leaves `addedByOrganise` empty, uses the degenerate `Unit` score type, and has no moved before/after pair. [25-proviso.edn `:exemplars :mining`]

Snatch alone makes the O3 subtraction non-trivial; its ruled edge set has 1 edge while its recorded set has 10. [25-proviso.edn `:exemplars :snatch`]

The seven-clause cross-exemplar coverage and its witnesses are recorded without selecting an outcome. [25-proviso.edn `:clause-coverage`]

The carrier proviso and staged existence amendment remain registry facts attributed to Joe. [25-proviso.edn `:registry-evidence`; holes/labs/wm-contract/aif-equations.edn: not found]

## Question

Does the exemplar set `{ants, mining, snatch}` discharge the item-4 proviso, and therefore release the staged item-5 amendment at `Holes.lean:861`? [25-proviso.edn `:registry-evidence`]

## Arms and measured costs

- (a) **Discharged as it stands.** Arm 6 is exercised on three passing exemplars; the cost is accepting the measured count of non-reproducing exemplars. [25-proviso.edn `:arms :a :cost`]
- (b) **Discharged by snatch alone.** Treat the only subtraction-stressing row as sufficient; the cost records whether that row reproduces its edge set. [25-proviso.edn `:arms :b :cost`]
- (c) **Not discharged.** Require one exemplar both to stress O3 and reproduce its run; the cost is leaving the measured passing exemplars insufficient. [25-proviso.edn `:arms :c :cost`]
- (d) **Discharged, with O3 reopened.** Accept the carrier and register the divergence separately; the cost is the measured number of newly open fields. [25-proviso.edn `:arms :d :cost`]
- (e) **Discharged conditionally.** Release the amendment while carrying the divergence as a separate row; the cost is the measured deferred-divergence count. [25-proviso.edn `:arms :e :cost`]

## Limits

No arm is preferred and no verdict on the proviso is taken here. [25-proviso.edn `:arms`; 25-proviso.edn `:registry-evidence`]

Nothing measured here bears on the other 5 registered organise choices. [25-proviso.edn `:registry-evidence :other-organise-choice-count`]

The mutation audit changes four relevant inputs and observes changed conclusions; its irrelevant control leaves conclusions stable, and original input digests remain identical. [25-proviso.edn `:plants`; 25-proviso.edn `:originals-byte-identical-after-plants?`]
