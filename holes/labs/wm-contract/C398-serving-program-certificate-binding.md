# C398 — serving runner identity reaches the certificate

Date: 2026-09-01.

Production-click certification now carries the terminal service's
`serving-runner-code` observation through click-resource normalization. The
tested commit is not accepted as an unauthenticated SHA argument: the operator
supplies the Futon2 CI bounded-job ID recorded by the quiet-run
`tested-commit` transition, and `certify_live_run.clj` resolves that ID through
the durable Futon3c job registry. It requires a terminal, passing, clean,
basis-stable canonical Futon2 CI receipt and derives the commit from its finish
basis.

`wm_operational_certificate.clj` compares that independently derived commit
with the Git head recorded by `load-file-recorded!` inside the serving JVM. A
production certificate passes this check only when the serving identity is
available, the reload observation is stable and clean, and the loaded head
equals the tested head.

The persisted result is explicit:

- `:program-identity-status :match` permits this condition;
- `:unavailable` names missing serving or tested identity; and
- `:mismatch` with reason
  `:serving-program-differs-from-tested-program` means the certificate concerns
  a different program from the one the bounded suite tested.

The mismatch is part of the certificate verdict, not a warning outside it.
The focused control constructs an otherwise clean production resource with
different loaded and tested commits and requires `:verdict :fail` plus the
distinguishable mismatch reason.

This comparison covers the canonical targeted runner file only. It does not
claim a closed dependency set; C396's residual limitation remains.
