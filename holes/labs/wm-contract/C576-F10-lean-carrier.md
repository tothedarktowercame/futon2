# F10 slice 3 — ruled outcome carrier

## Measured declaration

`FlightDisposition` has the twelve constructors parsed from `outcome-kinds`, in
the source's written order (`src/futon2/aif/full_loop_cohort.clj:31-33`); the
same set is enforced at `src/futon2/aif/full_loop_cohort.clj:317` and
`src/futon2/aif/tripwire.clj:189`.  The dated correction says the earlier
fourteen counted literal syntax rather than a vocabulary
(`holes/labs/wm-contract/aif-equations.edn:214`; artifact
`:facts :authority-keywords`).  `FlightDisposition.all` preserves declaration
order (`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:22-25`).

The ruled family is `Obs EvidenceObs`; organisations map to the disposition
type, people and money to named-empty abbreviations, and evidence remains a
parameter (`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:30-55`).  The
four vertex tags come from `holes/problems/P-validated-R5.md:116-120`, and that
record says the machine has no money vertex of its own at
`holes/problems/P-validated-R5.md:129-133`.

The single `PreferenceDistribution` is over the tagged sum, matching the
kernel surface at `mathlib4:DarkTower/WarMachine/Holes.lean:6904-6906`, rather
than the refused per-vertex function at
`mathlib4:DarkTower/WarMachine/Holes.lean:152-153`.  Its support contains all
twelve organisation outcomes and its five positive masses reproduce
`:C-seeded` (`holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:10-11`;
artifact `:facts :declared-positive-masses`).  The source says this seed is an
illustration, not a ruling
(`holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:15`).

## Seven named obligations

The seven zero-mass names are `groundedNoChange`, `artifactOnly`, `abstained`,
`guardrailRefusal`, `dispatchFailed`, `substrateUnavailable`, and `cancelled`
(artifact `:facts :named-zeros-derived`).  They are proved zero while the five
observed names are proved positive
(`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:103-109`).  The risk API
requires strict positive preference on every predictive-support member
(`mathlib4:DarkTower/WarMachine/Holes.lean:6993-6997`), so the exact equivalence
at `mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:111-137` leaves one
support-avoidance obligation for each named zero.  C574 identifies named zeros
as support claims at `holes/labs/wm-contract/C574-F10-disposition-enumeration.md:127-135`.

## Deliberately not declared

No evidence enumeration was chosen.  The source search found only a prose
mention (`src/futon2/aif/arguing_worlds.clj:219`, artifact not found beyond
that match), while the ruling describes an evidence certification/update and
epistemic-validity region at `holes/labs/wm-contract/aif-equations.edn:211`.
The seed therefore uses a documented named-empty evidence specialization while
the reusable family remains polymorphic.

No predictive `Q` was manufactured: its kernel type is at
`mathlib4:DarkTower/WarMachine/Holes.lean:6883-6885`, and the seven obligations
remain visible in the equivalence above.  No `Holes.lean` amendment was made;
the existential is staged in the lane file
(`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:139-142`), following the
two-phase precedent at `mathlib4:DarkTower/WarMachine/F12RuledCarrier.lean:72-74`.

`C_ser` is a named-empty organisation-support region, not new constructors
(`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:71-74`); its occasion-side
meaning is described at `holes/labs/wm-contract/C537-serendipity-shapes-C.md:49-59`.
`C_int` stays outside the sum at channel/tick grain
(`src/futon2/aif/preferences.clj:9-24`;
`mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:76-77`).

## Scope limits

This slice declares the carrier and seed only.  It does not attest the evidence
vocabulary, build `Q`, amend `Holes.lean`, validate the seed by a live run, or
make a registry decision (`holes/labs/wm-contract/worklist.edn:1377-1381`).
The checker records four landed false-verdict plants under artifact `:plants`;
the elaboration controls separately reject three type/proof mutations.
