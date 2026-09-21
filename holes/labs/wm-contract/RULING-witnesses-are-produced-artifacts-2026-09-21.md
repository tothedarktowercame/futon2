# RULING (Joe, 2026-09-21, ~03:30): witness declarations must name artifacts the workflow actually produces

Joe, on the M-expressions-of-interest C6 witnesses (AUTHORED-CHANGE.edn,
PREMISE-REFUSAL.edn, CORRECTION-PAR.edn under holes/missions/evidence/,
observed witness-present false in every live record): "I never
authorized this or asked for it, so whatever code does this is likely
entirely spurious... this is suggestive of a pathway that needs to be
deleted, has no relevance to the Lean model or any Clojure model of a
working system. The only fix I can foresee is to delete the bad
pathway... [aligning declarations to real artifacts] sounds sane but
it is meaningless without deleting, forbidding, turning off and
otherwise invalidating the bad path."

Facts at deletion time: the scheme existed in exactly one file
(introduced e4a6084d); the directory it points at does not exist; no
code reads or writes it. Deleted by emptying the declaration's
:locators (this commit). The three collaboration wants remain
declared and are now honestly UNOBSERVED (unknown), not
false-by-phantom-witness.

Standing rule going forward: an observation locator may only name an
artifact class the workflow demonstrably produces (commits, committed
receipts/ledgers/dossiers at pinned revisions). Declaring a witness
is a claim that something writes it; a locator pointing at a path
nothing produces is the facade pattern in declaration form. Real
witnesses for these three wants get declared deliberately, in
daylight, from the workflow's actual artifacts.
