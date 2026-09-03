/-! ### The pinned `find-snatch` record, transcribed (worklist `:U46`)

`futon3:checks/find-snatch.edn`, sha256 `839897ef8fe44952403700bd237389449ae4735d3da7df8239b1b94dc7ef4dfa`, whose
`:as-of` is the futon3 commit `2734ac570ed78d9bf822a5013cc48e53e68c8ff9` that last touched
`library/snatch` -- 18 authored patterns, 6 scenarios, 34 recorded rounds.
This block is GENERATED from that file by
`futon2:holes/labs/wm-contract/u46_find_transcribe.bb`; edit the fixture and
regenerate rather than editing the literals.
-/

/-- The 18 authored Snatch patterns of the pinned record, in its
sorted order. Constructor names are the recorded ids in lowerCamel. -/
inductive SnatchPattern where
  | aFreeMarkIsAlwaysWorthAssigning
  | acceptAnOfferThatBeatsHolding
  | anUnmodelledResponseStopsTheLine
  | askForSurplusNotSurrender
  | consultTheRemedyBeforeExiting
  | escalateOnlyAsFarAsYouCanLose
  | exchangeWhenBothSidesGain
  | forcedPlayNeedsALossFloor
  | institutionsVaryByPositionAndForce
  | markWithoutForce
  | nonBindingTalkStillMovesPlay
  | preserveTheRightToAbstain
  | priceTheFinalRoundAsFinal
  | probeBeforeCommitting
  | protectTheUnprotectedMove
  | reEnterAfterObservedRepair
  | revertThenInvert
  | useTalkToMakeATestableOffer
  deriving DecidableEq, Repr

/-- The recorded repository as a list -- what `findF1Containment` contains
selection within, and where `findF4Falsifiable` finds its zero-mass member. -/
def snatchRepository : List SnatchPattern :=
  [.aFreeMarkIsAlwaysWorthAssigning, .acceptAnOfferThatBeatsHolding,
  .anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
  .consultTheRemedyBeforeExiting, .escalateOnlyAsFarAsYouCanLose,
  .exchangeWhenBothSidesGain, .forcedPlayNeedsALossFloor,
  .institutionsVaryByPositionAndForce, .markWithoutForce, .nonBindingTalkStillMovesPlay,
  .preserveTheRightToAbstain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting,
  .protectTheUnprotectedMove, .reEnterAfterObservedRepair, .revertThenInvert,
  .useTalkToMakeATestableOffer]

/-- The six recorded scenarios, `treatment`/`disposition`
(`find_snatch.clj:22-24`, declaration order). -/
inductive FindSnatchScenario where
  | g1Snatcher
  | g1Sharer
  | g1Cautious
  | g4Snatcher
  | g2Snatcher
  | g5Sharer
  deriving DecidableEq, Repr

/-- The declared zero-mass pattern per scenario (`find_snatch.clj:25-31`),
as recorded in each scenario's `:f4` map. -/
def findSnatchZeroMass : FindSnatchScenario → List SnatchPattern
  | .g1Snatcher => [.consultTheRemedyBeforeExiting]
  | .g1Sharer => [.consultTheRemedyBeforeExiting]
  | .g1Cautious => [.consultTheRemedyBeforeExiting]
  | .g4Snatcher => [.forcedPlayNeedsALossFloor]
  | .g2Snatcher => [.consultTheRemedyBeforeExiting]
  | .g5Sharer => [.reEnterAfterObservedRepair]

/-- A recorded find row as a Lean literal: finite lists, so every predicate
over it is decidable. `round` is `some n` for a recorded round and `none`
for the scenario-grain row, whose `selected` is the recorded
`:selected-union`. -/
structure FindSnatchRowLit where
  scenario : FindSnatchScenario
  round : Option Nat
  selected : List SnatchPattern
  receipted : List SnatchPattern
  nonSelfCertifying : List SnatchPattern
  absence : Option TypedAbsence
  deriving DecidableEq, Repr

/-- The literal read as the `FindReceiptRow` the four declarations speak
about: each list becomes the set of its members, the repository and the
zero-mass set come from the record's own two constants. -/
def FindSnatchRowLit.toRow (r : FindSnatchRowLit) :
    FindReceiptRow FindSnatchScenario SnatchPattern where
  scenario := r.scenario
  repository := {p | p ∈ snatchRepository}
  selected := {p | p ∈ r.selected}
  receipted := {p | p ∈ r.receipted}
  nonSelfCertifying := {p | p ∈ r.nonSelfCertifying}
  zeroMass := {p | p ∈ findSnatchZeroMass r.scenario}
  absence := r.absence

/-- The 34 recorded rounds, in fixture order. The grain
`find_snatch.clj:157-171` iterates for F1, F2 and F3. -/
def findSnatchRounds : List FindSnatchRowLit :=
[
  { scenario := .g1Snatcher, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    absence := none },
  { scenario := .g1Snatcher, round := some 2
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g1Snatcher, round := some 3
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g1Snatcher, round := some 4
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g1Snatcher, round := some 5
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal]
    absence := none },
  { scenario := .g1Sharer, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    absence := none },
  { scenario := .g1Sharer, round := some 2
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g1Sharer, round := some 3
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g1Sharer, round := some 4
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g1Sharer, round := some 5
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    absence := none },
  { scenario := .g1Cautious, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    absence := none },
  { scenario := .g1Cautious, round := some 2
    selected := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    receipted := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    nonSelfCertifying := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    absence := none },
  { scenario := .g4Snatcher, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    absence := none },
  { scenario := .g4Snatcher, round := some 2
    selected := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    receipted := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g4Snatcher, round := some 3
    selected := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .reEnterAfterObservedRepair]
    receipted := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .reEnterAfterObservedRepair]
    nonSelfCertifying := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .reEnterAfterObservedRepair]
    absence := none },
  { scenario := .g4Snatcher, round := some 4
    selected := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    receipted := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g4Snatcher, round := some 5
    selected := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .reEnterAfterObservedRepair]
    receipted := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .reEnterAfterObservedRepair]
    nonSelfCertifying := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .reEnterAfterObservedRepair]
    absence := none },
  { scenario := .g2Snatcher, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting]
    absence := none },
  { scenario := .g2Snatcher, round := some 2
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 3
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 4
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 5
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 6
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 7
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 8
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 9
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor]
    absence := none },
  { scenario := .g2Snatcher, round := some 10
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain]
    absence := none },
  { scenario := .g2Snatcher, round := some 11
    selected := []
    receipted := []
    nonSelfCertifying := []
    absence := some .noPatternAddressesThisTension },
  { scenario := .g2Snatcher, round := some 12
    selected := [.priceTheFinalRoundAsFinal]
    receipted := [.priceTheFinalRoundAsFinal]
    nonSelfCertifying := [.priceTheFinalRoundAsFinal]
    absence := none },
  { scenario := .g5Sharer, round := some 1
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting, .useTalkToMakeATestableOffer]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting, .useTalkToMakeATestableOffer]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .probeBeforeCommitting, .useTalkToMakeATestableOffer]
    absence := none },
  { scenario := .g5Sharer, round := some 2
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g5Sharer, round := some 3
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g5Sharer, round := some 4
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose]
    absence := none },
  { scenario := .g5Sharer, round := some 5
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .priceTheFinalRoundAsFinal]
    absence := none }
]

/-- The 6 recorded scenarios, each with its `:selected-union`.
The grain `find_snatch.clj:172-177` iterates for F4. -/
def findSnatchScenarios : List FindSnatchRowLit :=
[
  { scenario := .g1Snatcher, round := none
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    absence := none },
  { scenario := .g1Sharer, round := none
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    absence := none },
  { scenario := .g1Cautious, round := none
    selected := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    receipted := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    nonSelfCertifying := [.anUnmodelledResponseStopsTheLine, .askForSurplusNotSurrender,
      .exchangeWhenBothSidesGain, .probeBeforeCommitting]
    absence := none },
  { scenario := .g4Snatcher, round := none
    selected := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting, .reEnterAfterObservedRepair]
    receipted := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting, .reEnterAfterObservedRepair]
    nonSelfCertifying := [.aFreeMarkIsAlwaysWorthAssigning, .askForSurplusNotSurrender,
      .consultTheRemedyBeforeExiting, .exchangeWhenBothSidesGain,
      .priceTheFinalRoundAsFinal, .probeBeforeCommitting, .reEnterAfterObservedRepair]
    absence := none },
  { scenario := .g2Snatcher, round := none
    selected := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    receipted := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    nonSelfCertifying := [.askForSurplusNotSurrender, .exchangeWhenBothSidesGain,
      .forcedPlayNeedsALossFloor, .priceTheFinalRoundAsFinal, .probeBeforeCommitting]
    absence := none },
  { scenario := .g5Sharer, round := none
    selected := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting,
      .useTalkToMakeATestableOffer]
    receipted := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting,
      .useTalkToMakeATestableOffer]
    nonSelfCertifying := [.askForSurplusNotSurrender, .escalateOnlyAsFarAsYouCanLose,
      .exchangeWhenBothSidesGain, .priceTheFinalRoundAsFinal, .probeBeforeCommitting,
      .useTalkToMakeATestableOffer]
    absence := none }
]
