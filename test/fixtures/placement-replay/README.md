# Placement replay fixture

This is a deliberately small authored fixture for R32-b, not a verbatim copy
of the live Giraffe run. `giraffe/previous/observation.edn` retains the two-fact
shape consumed from the previous run; the test binds the Giraffe field reader
to this file so the replay boundary is exercised without copying the live,
growing mission registry. `placements.tsv` is the pinned placement source for
two targets. `classes.tsv` and `gamma-members.txt` form an intentionally
unpinned class placement for the third target, used to prove that replay does
not admit a class source that was absent at selection time. The fixture does
not track the live store.
