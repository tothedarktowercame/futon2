# C429 — attempt-identity authority census

Date: 2026-09-01

## Result

No existing value binds a bounded job to a run without a caller-selected link.
There are substrate-assigned identities on both sides, but no substrate assigns
their relationship.  C410 therefore validates internal consistency of an
attempt label, not independent attempt identity.

This is a local authority limit, not a pending field-addition repair.  Merely
copying the current fence id, job id, unit id, or receipt digest into the run
record would make the relationship more inspectable while leaving the party
that selects the job able to author the relationship.

## Candidate census

| Candidate | What assigns it | Can the job selector author/select it? | Does it bind job to run? |
|---|---|---|---|
| bounded `job-id` | `bg.py` constructs a timestamp-and-label id at admission | The exact timestamp is substrate-generated, but the caller supplies the label and later selects the resulting id | No; the run record never contains it |
| systemd unit name | `bg.py` derives it from the generated job id and systemd instantiates it | The caller cannot choose the complete generated name directly, but can choose which completed unit/job to present | No; no unit identity appears in the run or click binding |
| `ExecMainStartTimestampMonotonic` | systemd | Not authorable as a value by the submitting caller | No; it establishes when that unit started, not which later click/run belongs to it, and it is not joined to the run |
| bounded receipt | the bounded runner measures command, repository basis, times, exits, and resources | The caller chooses the command and chooses which registry job to present; it cannot directly author a valid receipt | No; the receipt contains neither click id nor run id, and its measured interval is the test interval |
| bounded registry `agent-id` | caller supplies `bg.py launch-test --agent`; `bg.py` persists it | Yes, directly | No independent binding; this is the C423 duplicated label |
| fence receipt observations | `writer_fence_evidence.py` observes the world twice and validates attestations | Observations cannot be bought by the id, but the caller supplies the fence id and selects the receipt | It establishes a real observed window, not membership of either the bounded job or the subsequent run |
| fence id | caller supplies it to the state-machine initialization and fence observer | Yes, directly | No; equality only joins copies of the caller label |
| click id | serving click endpoint issues/retains it and the observer joins it to terminal status and the run record | The ordinary observer does not invent the accepted click id | It binds the click to the run, but neither to a bounded job nor to the tested phase |
| run id | the serving JVM normally generates a UUID, although `run-opportunity!` also accepts `:run-id` in raw options | Producer-generated on the ordinary path, caller-influenceable at the lower seam | It identifies the run and is durably joined to the click, but no bounded receipt contains it |
| run start time | serving JVM records current time | Not normally caller-authored | It cannot establish attempt membership; comparing it with a prior test/fence interval proves ordering at most |

## Existing joins

The bounded producer registry, receipt file, systemd unit, and public record are
cross-checked.  That proves the selected job is a real registered bounded job.
The state machine additionally restricts certificate production to the three
job ids recorded at its `tested-commit` transition.  Those properties prevent
receipt-file substitution and late selection of an unrecorded fourth job.

Separately, serving terminal status, the click receipt, the durable click/run
binding, and the run record are cross-checked.  That proves the selected run is
the run produced by the observed click.

The missing edge is between those two sound subgraphs:

```text
systemd unit -- bounded registry -- bounded receipt
                                           X
fence observations -- click -- binding -- run record
```

Today the `X` is represented only by equality with the caller-authored fence
label.

## Would adding an attempt field to the run record close it?

Not by itself.  A run-record field populated from the click payload, state
machine, environment, or current fence id would still be caller-authored or
caller-transported.  Comparing it with the bounded registry's caller-authored
`agent-id` would add a third agreeing copy of the same assertion.

A stronger design would require a producer-verifiable capability or join that
the job selector cannot mint: for example, an authority that issues one
attempt identity and independently binds both the admitted bounded jobs and
the serving click to it.  No such issuer or existing cross-producer join was
found.  C409/C421 already establish that the assessed local stores do not
supply an external canonical authority, so a local digest or hash chain does
not change this result.

## Classification

Recommended fifth C421 limit:

```text
attempt membership across bounded-test and serving-run producers is
unprovable from current local evidence; the only shared attempt value is
caller-authored.
```

Classification: `authority-limit-not-pending-local-repair`.

Clearing condition: an independently issued, verify-at-use attempt capability
bound by both producers, or an external authority that records the job/run
membership without job-selector rewrite or selection power.  This report does
not invent that tier and makes no implementation change.
