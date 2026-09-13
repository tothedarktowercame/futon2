# Row 22 E1 mapping packet

Source pins at the tested state:

- `machine_budget_mapping.clj`: `799025d55f7b9dccd11efea02ebe2661a24f946d02d28578c06e8a8bc01edff9`
- `machine_budget_mapping_test.clj`: `023315b6103e4a9937e0782df632b33eabb4aaacf3cc78ec6b5cc85bc80fd449`
- production-shaped fixture: `606bdcf6fa0d33e86fe4df4ec796c95ad759c4c3087c715c2f03ba58fca246b9`
- readback: `058b2cc50f1134969ea2ea41ac1d8b00e1a925a38c098d3cf41dc4a141bfa8da`

The fixture copies only the action-map shape from a retained machinery record.
Its repeated semantic action is commissioned so occurrence identity can be
tested. It is not a live production E1 input and its injected cost, utility,
membership, and budget authorities do not assert production authority.

The actual production-shaped readback refuses `:r6-r11/authority-missing`:
the retained ranked support has no independently pinned cost, utility, field,
or budget sources. The positive mapping and its feasibility result are
commissioned tests of the mechanism, not evidence that E1 fired or that a run
is budget-feasible. E2 and runtime selection/enactment are untouched.

The canonical R11 arbiter required no modification: distinct proposal ids are
preserved even when action maps compare equal, and selected plus rejected rows
cover the complete proposal input. The E1 adapter additionally retains ordered
support, a candidate/proposal bijection, request, response, every disposition,
and an exact replay receipt.
