# Declared preference scales

Implementation 55cab932; main merge 4e59cb79.

Both live declarations now explicitly declare lam=1 and mu=0. The loader
validates exact numbers, declared status, lam>0 and mu>=0 and retains the
parameter records per target. Assembly carries them into the lane spec.
The judge checks family agreement before scoring lanes and passes the shared
scales, with per-target provenance, to live-c/cascade-spec. Live C derives
normalized relative weights, not lambda; the declared lambda scales those
weights because explicit weights override the uniform lambda term in scoring.
The joint spec and the no-overlap branch retain the scales and provenance.

Compatibility rule: an omitted parameter receives :status :defaulted and
:reason :parameter-not-declared (lam 1, mu 0), retained in the spec provenance.
This applies to legacy declarations and hand-built callers. Neither current
production declaration defaults. Invalid supplied values never default.
Different target scales refuse :incommensurable-family.

Controls: lam=2 survives declaration read, assembly, live C and production
merge; lane and joint lam change to 2, total explicit weights change 1 -> 2,
and log preference changes. mu=3 also reaches the joint spec. lam=0/-1,
mu=-1, and inexact lambda refuse :invalid-preference-scale. Missing parameters
are explicitly defaulted, and differing family scales refuse.

Merged-state warrant run: 3 tests, 27 assertions, zero failures/errors.
clj-kondo 0 errors/0 warnings; check-parens OK. Existing good-record validity
output byte-identical to the previous declaration evidence: VALID (4/5 ok),
with the same F-not-consumed flag.

Warrant test-registry-78c34bf51ed93728a7c5ac025df5b3fb8aa05f0ce155c31131290972bcdff2c0
bound to WM-declared-preference-scales; HTTP current-validity check true.

Reload required, in order: futon2.aif.live-c, futon2.aif.cascade-sources,
futon2.aif.cascade-problems, futon2.report.war-machine.
No serving reload or WM click performed. Acceptance tick remains deferred.
