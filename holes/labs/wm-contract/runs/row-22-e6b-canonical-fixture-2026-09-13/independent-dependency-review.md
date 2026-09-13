# Independent review of d11d2917

The added call edges and pins are correct. `machine-budget-mapping` calls
`hierarchical-budget-adapter/select-ranked-proposal-fields`, whose adapter calls
`hierarchical-budget/arbitrate`; omitting either source declaration would make
the transitive E1 replay inventory incomplete. The correction remains a source
inventory only and establishes neither installed-code identity nor authority.
