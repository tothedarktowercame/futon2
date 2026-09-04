# NOTE-q-pin-review — basis for the U53 row (mint at next loop pause)

Trigger: Joe, 2026-09-04, reading plop-2026: "Source state: STALE-PIN
(lean-spine, runtime-efe, runtime-selection, topology)... this is worth
looking into."

## What it is

Q-interface-completeness.edn (:as-of 2026-08-31) pins eight sources by
content sha256; gen_q_interface_table.bb re-hashes them at every paper
build and renders STALE-PIN when any differ. Four differ now. This is the
pin machinery working — the findings render but carry a visible
requires-review banner in BOTH papers.

## Why it is a review, not a re-pin

Per-file movement since the pin (git log, 2026-08-31 →):

- :lean-spine (Holes.lean): U46 find-row closures, U47 falsifier
  corrections, U49 + RE5 run-conformance certificates. None obviously
  constructs Q(o|pi) from model+belief+policy — the central :missing
  finding — but the reviewer verifies that, including whether the find/
  certificate machinery changed any :implemented-parametric status.
- :runtime-selection (policy.clj): U10 "selector consult — a selection law
  that reads the posterior it records", AC4 fallback-selector change, RUN8/
  RUN9 tau/F_pi seams. Directly claims-relevant to the runtime findings.
- :runtime-efe (efe.clj): U24 :survey-mission epistemic action — EIG-
  adjacent; the registry's :lean/Q-eig-consumer and runtime proxy findings
  may need their limitation text re-verified.
- :topology (control-map-edges.edn): D5-D10/I2/J1/U31/U48 decision entries
  and edge changes (R2->R3a among them). The registry's topology-dependent
  statements need re-reading against the current edge set.

## The row (:U53, class :V, owner :any)

Statement: re-verify every finding in Q-interface-completeness.edn whose
evidence lives in a stale-pinned file, at the CURRENT shas; update finding
statuses/limitations only where the file's change actually moved them
(each such edit citing the commit that moved it); then re-pin all eight
sources, bump :as-of, and regenerate the table.

Acceptance: gen_q_interface_table.bb renders Source state: CURRENT (bare
exit); every changed finding names the commit that changed it; findings
NOT changed are stated as re-verified-at-sha in the row evidence (not
assumed); the central :missing constructor finding explicitly re-checked
against the certificate-era Holes.lean (grep is not a check — read the
declarations); both papers rebuild with the banner gone. TN 9a applies if
any signed row covers this registry.
