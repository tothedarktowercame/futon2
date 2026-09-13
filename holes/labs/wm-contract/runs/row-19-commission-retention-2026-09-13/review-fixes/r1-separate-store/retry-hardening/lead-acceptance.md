# Retention prerequisite review — codex-26, 2026-09-13

Subject futon3c 0d40a3594592b555bbcfe5a742d2af2860b74691, receipts
futon2 10b556db. Three source/test/runner hashes match actual bytes, retained
in lead-pins.json. Reviewed source and raw output; no passing test rerun.

The existing-file branch now validates schema, job identity, archive digest and
commission digest before equivalence comparison and repeats the directory-force
barrier. New publication also forces the archive directory and its parent.
The full retained archive projection is compared on hot/archive coexistence,
so equal commissions cannot conceal different trace/artifact joins.
The focused retained output reports 18 passing assertions; induced failure has
19 assertions/one failure/exit 1; kondo zero warnings/errors and parens OK.
Source inspection confirms a planted failure after publication preserves the
hot job; corrupt stored digest refuses before expiry; different join refuses.
The barrier counter test counts calls across two archive jobs, so alone it does
not isolate which job forced; inspection of the existing-file branch supplies
that evidence. This is not a claim of a crash/power-loss execution test.

Accept the reviewed retention prerequisite at the configured local-store scope.
This does not authenticate the operator, verify independent trace origin,
establish author/reviewer separation, accept a canonical branch artifact, or
create/admit an R9 anchor. Next packet implements a separately reviewed genesis
verifier with external authority resolution and exact retained commissions.
No live reload or deployment verification occurred in this review.
