# PROPOSAL — a provenance state for measured-but-uncalibrated rates (for Joe's morning read)

THE GAP. observation_model.clj's validate! admits only
{:status :synthetic :calibrated false}. The adopted A declaration's
rates (aae84fc3) are MEASURED — counted denominators from named
ledger populations — so they fit neither :synthetic (a lie: they are
not invented) nor calibrated (point-5's designed calibration has not
run). Rated runs are blocked on this mismatch; nothing else is.

PROPOSED. Add one state: {:status :measured :calibrated false}.
validate! admits it iff EVERY rate in the model resolves to a
:parameters entry whose :basis cites a measurement record
(MEASUREMENT-*.md path + population). Bare literal rates are NOT
admissible under :measured — the state's meaning is "every number
here has a counted denominator on record". The synthetic gate keeps
its current meaning; the calibration gate WIDENS later to require a
calibration record (per the docstring's existing promise) and is
never dropped.

WHAT THIS PRESERVES. The three-way honesty the record already uses
everywhere else: invented (:synthetic) / measured (:measured) /
calibrated (future) are distinct claims with distinct evidence
shapes, and a reader can tell which one a model makes from the
record alone.

ALTERNATIVE REJECTED. Relabeling measured rates :synthetic to pass
the gate — misdescribes provenance to satisfy a validator, the exact
move the schema-3 gate refused elsewhere tonight. Second alternative
(wait for calibration) rejected as the self-perpetuating deferral:
calibration needs rated-run residuals, which need rated runs.

DECISION. Adopt :measured as specified (one validate! extension +
docstring, small packet, both-sides controls: a measured model with
one bare literal rate must REFUSE) — or rule otherwise. Blocks only
rated runs; tonight's loop demonstration is unaffected.
