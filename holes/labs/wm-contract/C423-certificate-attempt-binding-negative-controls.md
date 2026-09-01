# C423 — Certificate attempt-binding negative controls

Date: 2026-09-01

## Verdict

C410 closes the reported *label mismatch*: a selected bounded job whose stored
`agent-id` differs from the supplied fence id is rejected as
`:tested-job-attempt-mismatch`.  It does not establish that the label is an
independent authority for the attempt.  `futon3c/scripts/bg.py` obtains
`agent-id` from the launcher's `--agent` argument and persists it.  The value is
producer-recorded, but caller-authored.

Consequently, a job from an unrelated attempt whose head happens to match can
still pass when it was launched with a caller-chosen label and the caller
supplies the same label as `--fence-id`.  The repair proves equality between two
copies of a label; it does not prove that the selected job and the certified run
belong to one attempt.

## Focused controls

A fixture used one terminal, clean Futon2 CI receipt with head `serving-head`,
job id `unrelated-job`, and stored agent id `caller-chosen-fence`.

- Changing the expected attempt to `different-fence`, while retaining the same
  matching head, throws `:tested-job-attempt-mismatch`.  This is the C410
  negative control and it holds.
- Supplying `caller-chosen-fence` derives `serving-head`; the normalized
  resource retains `:tested-job-id "unrelated-job"` and
  `:tested-attempt "caller-chosen-fence"`; a historical run record with no
  attempt identity then receives certificate verdict `:pass` when its serving
  head is `serving-head`.  This complementary control demonstrates the open
  boundary.

Observed result:

```clojure
{:different-attempt-control :tested-job-attempt-mismatch,
 :matching-caller-label
 {:derived "serving-head",
  :retained-attempt "caller-chosen-fence",
  :retained-job "unrelated-job",
  :verdict :pass}}
```

## The two routes

The state-machine route is internally narrower.  Its `tested-commit`
transition requires all three bounded records' `agent-id` values to equal the
ledger context's fence id and records their job ids.  `produce_certificate`
then selects the Futon2 CI job only from that recorded set and passes the same
context fence id to the standalone certifier.  It therefore prevents selecting
a fourth, unrecorded job at certificate time.  The job ids and their
`agent-id`s nevertheless originated in caller submissions, so this remains an
internally consistent label join rather than independent attempt authority.

The standalone route accepts both selectors from its caller:
`--tested-job-id` chooses the durable receipt and `--fence-id` supplies the
expected label.  It verifies their equality but has no run-to-attempt join.
Thus the two routes share the same equality check, but they do not provide the
same selection constraint.

## Retained fields

`:tested-job-id` and `:tested-attempt` are copied from the normalized resource
into `:program-identity-status`.  The certificate compares the tested commit
with the loaded serving commit; it does not compare either retained field with
the run record.  The run record carries no corresponding attempt identity.
These fields make the derivation inspectable, but are not themselves verified
against the run the certificate describes.  This is the class-6b shape:
recorded provenance mistaken for verify-at-use evidence.

## Properties established

- A stored label different from the expected attempt is rejected.
- Missing attempt identity, invalid/nonterminal receipts, wrong commands,
  dirty or unstable repository bases, and missing tested commits remain
  blocking.
- Direct tested/serving commit mismatch remains blocking.
- The state-machine path cannot select a job outside its recorded
  `bounded-job-ids` set at certificate production time.
- The retained job, attempt, and commit values remain visible in the output.
- Focused certificate suites: 12 tests, 42 assertions, 0 failures, 0 errors.

## Properties not established

- That attempt identity is producer-derived rather than producer-persisted
  caller input.
- That the bounded job and the run record belong to the same attempt.
- That standalone selection has the state machine's recorded-job-set
  constraint.
- That a reader can revalidate `:tested-attempt` against the run using the
  certificate alone.

No implementation was changed in this packet.
