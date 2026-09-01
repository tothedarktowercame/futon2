# C383 — evidence is bound to its producer

Date: 2026-09-01.

The quiet-run state machine no longer treats shape-compatible JSON as bounded
test evidence. `tested-commit` takes three durable bounded-job IDs. It resolves
each through the Futon3c producer registry, requires its terminal systemd unit
and on-disk receipt to agree, requires all three records to carry the current
fence ID as their attempt identity, and requires the workspace gate and Futon2
suite to name the same clean commit. Futon3 is a different Git repository, so
its commit is joined by attempt rather than equated to Futon2's commit.

Quiescence and writer-fence evidence are produced by direct state-machine
invocation of their authoritative checks. The fence artifact and attestation
recorded at `fence-held` are the exact pair later consumed by `tested-commit`.
Caller substitutions are not accepted.

At every command, the complete ledger is revalidated: genesis must be
`initial`, rows must follow the declared order, the fence ID cannot change,
and every evidence path must remain readable with its recorded SHA-256. JSON
parsing rejects a valid prefix followed by trailing content.

Freshness is measured when the machine consumes the durable job records, not
from caller-supplied `started-at`. A fence over 300 seconds old at ingestion
refuses. Thus a presenter cannot freshen day-old evidence by editing JSON.
Systemd's boot-relative monotonic start is not used because the fence record
has no comparable boot identity and monotonic observation.

`restored` authenticates its records and independently observes every changed
target through the live restoration backend. Records are necessary but no
longer sufficient for `FENCE-RELEASE`.

Focused controls (`python3 -m unittest -v test_wm_quiet_run_state.py`) reject a
synthesized ledger, changed recorded evidence, handwritten producer evidence,
a day-old fence with a fresh asserted timestamp, mismatched gate/suite commits,
and incomplete restoration. The producer binding is substrate-specific: a
field merely named `producer` is not provenance.
