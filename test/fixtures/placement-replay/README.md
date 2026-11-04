# Placement replay fixture

This is a deliberately small authored fixture for R32-b, not a verbatim copy
of the live Giraffe run. `giraffe/previous/observation.edn` retains the two-fact
shape consumed from the previous run; the test binds the Giraffe field reader
to this file so the replay boundary is exercised without copying the live,
growing mission registry. `placements.tsv` is the pinned placement source for
those two targets. The fixture does not track the live store.
