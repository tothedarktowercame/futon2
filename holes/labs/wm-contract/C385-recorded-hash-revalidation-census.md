# C385 — recorded-hash revalidation census

Date: 2026-09-01.  Scope: the verification and operator paths named in
C385.  This is an assessment, not a repair.  A carrier is counted once per
protocol even when its field occurs in many records.

## Result

Eighteen logical hash/signature carriers were found:

| disposition | count | meaning |
|---|---:|---|
| revalidated at use | 11 | the consumer recomputes the value, checks an authenticated record, or compares both ends of the interval for the claim it makes |
| record-time/provenance only | 5 | the value is written but no later consumer compares it to the named subject |
| neighbour binding | 2 | the value binds the row to an authenticated manifest, but does not authenticate the row itself |

The majority are checked.  There is therefore no evidence for a generic
shared verifier over every digest format.  There is a short, typed remainder;
only two of its five record-time carriers are presently used as evidence by a
later state-machine consumer.

## Census

| path / logical carrier | disposition | what is checked, or not checked |
|---|---|---|
| `scripts/wm_quiet_run_state.py` — row `receipt-sha256` and `previous-receipt-sha256` | **revalidated at use** | `load_ledger` recomputes every row digest and the complete predecessor chain (`:115–129`). |
| same — transition evidence `file_ref.sha256` | **record-time only; load-bearing** | `file_ref` hashes the bytes while advancing (`:110–112`), but `load_ledger`/`status` never reread the referenced path. Later states trust facts derived from evidence that may since have changed or been substituted. |
| same — `parking-specification-sha256` | **record-time only; load-bearing** | `init` records `digest(parking_specification())` (`:294–301`); ledger load never recomputes the current specification. A changed parking population remains represented by the old, valid row hash. |
| `checks/writer_fence_evidence.py` — coordinator-registry SHA in the observation | **revalidated at use** | it is part of both captured observation states; start/finish equality is the interval claim. It is not claimed as durable freshness after that interval. |
| same — attestation `content-sha256` | **record-time/provenance only** | no downstream comparison uses this digest, but the attestation content itself is schema-, fence-, writer-, acknowledger-, and interval-validated at use (`:241–269` and following). The digest is not the source of trust. |
| `writer_fence_capability.clj` — returned `receipt-sha256` | **record-time/provenance only** | `verify` hashes prior receipt bytes (`:28–30,69`) but has no expected digest to compare. Trust instead comes from parsing the receipt and a fresh subprocess observation (`:43–65`). |
| `scripts/writer_fence_restore.py` — manifest HMAC | **revalidated at use** | `validate_manifest` recomputes the HMAC with the supplied owner-only key before journal/restore use. |
| same — attempt-row HMAC | **revalidated at use** | `load_attempts` removes and recomputes `attempt-hmac-sha256` over the attempt row (`:299–315`). |
| same — journal `manifest-hmac-sha256` copy | **neighbour binding** | equality binds the journal row to the authenticated manifest; it is not a signature over the journal action itself. Structural/order/live-state checks carry the remaining claim. |
| same — outcome `manifest-hmac-sha256` copy | **neighbour binding** | equality binds an outcome to the manifest (`:290–295`), not to performance of the inverse. Outcome order and observed postconditions, not this copied HMAC, must carry that event claim. |
| `checks/positive_proof_receipt.clj` — declaration-slice hashes | **revalidated at use** | live declaration text is re-sliced and hashed, then compared to `:source-basis` (`:111–127`). |
| same — fixture hash / adapter basis | **revalidated at use** | `fixture-valid?` recomputes the fixture digest; the current validator also makes the nonempty adapter and its referenced Lean fields load-bearing (`:85–109,128`). |
| same — toolchain / manifest hashes | **revalidated at use** | `live-toolchain` is recomputed and compared before acceptance (`:129,143`). |
| bounded receipts (`futon3c/scripts/bounded_test_job.py`, `scripts/run_readiness.py`) — repository basis hashes | **revalidated at use** | bounded execution compares start/end basis; readiness recomputes the current basis and compares `tracked-diff-sha256` (`run_readiness.py:53–66,128–129`). |
| bounded service — `configuration-hash` | **revalidated at use** | the service recomputes the current configuration and admits receipts to the retirement population only on hash equality (`futon3c/scripts/bg.py:99–105,213–220`). |
| workspace-gate receipt — repository basis hashes | **revalidated at use** | the gate sandwiches its run with repository provenance (`checks/wm_workspace_gate.clj:39–46`); readiness is the later current-basis consumer. The receipt does not claim coherence after its interval. |
| `p4ng/empirics-futon/gen_workflow_report.bb` — three source `sha256-prefix` fields | **record-time/provenance only** | the generator records queue, registry, and decision-source prefixes (`:304–310`); the internal report/table consumer does not recompute them. They identify the generation basis, not current freshness. The workflow report is withdrawn from publication. |
| `checks/mutable_read_set.clj` — captured/current SHA | **revalidated at use** | digest and size derive from one byte capture (`:39–52`), and `compare-current` rereads and recomputes at the interval endpoint (`:62–79`). Under its declared `:content-current` claim endpoint equality is sufficient; it explicitly does not use the digest to prove event-freedom (`:99–129`). |

## Findings

1. **The state machine contains the live defect population: two.** Its evidence
   file hashes and parking-specification hash are protected against alteration
   *inside the ledger row*, but not compared with their subjects when the
   ledger is resumed or read. A valid chain can therefore attest obsolete or
   substituted inputs. This is the C380 shape: checked at record time only.
2. **Three record-only carriers are provenance rather than validators.** The
   fence attestation digest, capability receipt digest, and workflow source
   prefixes do not currently decide acceptance. Their names should not be
   quoted as freshness evidence, but their non-revalidation does not make the
   corresponding semantic checks unsound.
3. **Two copied manifest HMACs authenticate the neighbour.** They say which
   authenticated manifest a journal/outcome names; they do not prove the row's
   event. Calling either an authenticated journal or authenticated outcome
   would overstate it.
4. **Bounded and gate basis hashes have an explicit claim interval.** Endpoint
   recomputation plus readiness's current comparison is materially different
   from merely printing a digest. They do not establish event-freedom outside
   the observed/fenced interval.

## Catalogue judgement

Do **not** create a new defect class. “Recorded but never revalidated” is a
mechanical subtype of class 6b, *access mistaken for evidence*: possession of a
digest proves that the recorder could read bytes once, not that those are the
bytes now being trusted. Sharpen 6b with the review question:

> At the trust boundary, is the recorded digest recomputed over the exact
> subject being accepted, or is it only provenance/a copied credential for a
> neighbouring object?

A shared verify-at-use helper is justified for the two state-machine file
references because they share one schema and consumer. It is not justified
across all eighteen carriers: HMAC authentication, interval comparison,
repository-basis comparison, and declaration slicing have different subjects
and failure semantics.

## Delivery inventory

Focused inventory command:

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
