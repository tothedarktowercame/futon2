classification.edn is the verbatim EDN value at [:value :classification] in
/home/joe/notes/b2-click/b2a-replay.edn, produced by a read-only serving replay
of run 2026-10-23-57cd4dc4-7bf2-43ad-9cbd-7549e183ab32 (source SHA
be624b0bf12828772fdb328c75c50e8551910da7c70823453835ff9f4f1af87f).
Serialization is pr-str; it is not a byte copy of the full replay response.
The test verifies the original classification seal, substitutes a fixture
record pin and the control click IDs, then reseals through the production
classification sealer. All other classification fields are retained.
The two enacted histories are built with real append-transition, not by
editing ledger entries. No live ledger or source record is read by the test.
