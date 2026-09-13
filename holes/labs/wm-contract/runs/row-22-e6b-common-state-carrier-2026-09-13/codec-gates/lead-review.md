# Carrier codec review: context joins incomplete

Reviewed 6097cad0/0cd37be8 source and retained raw 14 tests/81 assertions, clean kondo/parens. Current and historical pins are in lead-pins.json. No unchanged passing tests rerun.

Two isolated additional controls executed with exit 0 (lead-control.clj and raw stdout/stderr): nil proposal :identity still structurally projects; replacing transition occurrence with "borrowed" still projects while all original context bytes remain unchanged. Source validates selected subject fields against prior/next but never joins the full proposal identity/subject/application/time to decoded context. This is a structural mismatch, not evidence that codec grants authority (output correctly says none).

Repair full typed proposal submaps and explicit context identity/subject/application/event/time joins, including wrapper revisions and source/canonical digest shapes. Validate nested successor metadata references and their joins where the contract promises complete schemas; do not hide fields through projection. Keep transition-law replay and authority outside this codec, but make all claimed structural equality exact. No store adapter or runtime changes.
