# C439 — gate basis and writer-fence coupling

Date: 2026-09-01.

The five-repository workspace gate can certify a commit only across a stable,
clean basis. Futon3c has autonomous repository writers, including the APM
coordinators and closer. Therefore obtaining a certified commit across the
gate's actual repository scope requires the existing named writer fence to be
held for the gate interval.

C435 did not create this dependency; it made it observable by adding Futon3c
to the derived repository scope and start/finish provenance. Futon3c must not
be removed from that scope to make ordinary runs look stable.

An unfenced run remains useful. If every check returns an accepted result while
the basis moves, the receipt may report `:verdict :pass`, qualified by
`:basis-status :moved` and `:verdict-qualification
:repository-basis-moved`. It must also report `:certified-commit {:status
:absent :reason :repository-basis-not-stable}`. This means **all checks passed;
no commit was certified**. A consumer must not promote the check verdict into
a commit certificate.

This gives the C313 parking request a second operational purpose: besides
protecting the operator run, parking the named background writers is the only
current way to obtain a certified commit across all repositories reached by
the gate. The gate's exit convention is unchanged; the receipt carries the
claim boundary.
