# Row 16 R6 carrier verification

The carrier and compatibility theorem elaborate, the existing
`SoftmaxWitness` elaborates unchanged, and the two affected Lake targets build
successfully. The axiom census records only Mathlib's ordinary `propext`,
`Classical.choice`, and `Quot.sound`; neither declaration depends on `sorryAx`.

For transparency, `darktower-build.txt` retains one additional exploratory
`lake build DarkTower` run. It exited 1 only because the umbrella prefix includes
the repository's existing designated rejection modules
`MemoryArmPreregistration`, `BaldwinPostRegistration`, `FoldCOrderNegative`, and
`FoldCFoldedNegative`. This is not represented as a passing gate receipt. The
scoped target build in `target-build-receipt.edn` is the passing build gate for
the declarations changed by this packet.
