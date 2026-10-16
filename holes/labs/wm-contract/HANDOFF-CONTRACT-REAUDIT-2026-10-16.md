# External handoff contract re-audit — 2026-10-16

Trigger: live run `2026-10-16-08f62de8-db1e-4fe3-9a01-ad264bcc3533` contacted
`wm-author`, but the answer was refused by a display-cue coverage limit absent
from the advertised acceptance contract.  A validator behaving as coded is not
enough: every producer-affecting rejection rule must be visible before contact.

## Production handoffs audited

| Handoff | Contract owner | Validator owner | Result |
|---|---|---|---|
| selected-target reading | `session_turn_analysis.py:acceptance_contract` | `session_turn_analysis.py:validate` | **Repaired.** Contract v2 now states cue length, aggregate per-sentence word budget, offset containment, intent-label grammar, and canonical-library requirements. `complete` again preserves the identity-bearing request bytes and publishes status only in the sibling result. |
| selected-policy reading | `selected_reading.clj/acceptance-contract` | same namespace `validate` | Acceptable: exact owed identities, verdict vocabulary, nonblank reason, lowercase SHA-256, source identity, and request identity are declared. Issued request carries the authoritative library pins. |
| want interpretation | `want_interpretation.clj/acceptance-contract` plus its reply grammar | same namespace `validate-response` | Acceptable but composite: the five semantic gates and pinned inputs are declared; the prompt supplies the exact single-form response grammar. |
| repair standing decision | `limb_evidence.clj/standing-decision-contract` | same namespace `validate-standing-decision` | Acceptable: exact required fields, decision values, independent reviewer identity, timestamp, evidence, and explanation limits share one value. |

## Required invariant

A handoff is not certified merely because its response validator rejects bad
data.  It is certified only when the request records the same material rules
the validator will enforce.  A newly added rejection rule therefore requires
a contract-version change and a producer-visible contract test.

## Evidence

- Live author job: `invoke-1792130124820-15160-66bf192c`.
- Hidden rule that fired: sentence `s7` marked 6 of 9 words; maximum 4.
- Contract control: `test_session_turn_analysis.py /
  test_acceptance_contract_discloses_enforced_display_cue_budget`.
- Immutability control: `test_publication_preserves_request_and_refuses_overwrite`.
- Cross-boundary control: `futon2.aif.selected-target-reading-test` parses and
  transmits the validator-produced contract unchanged.
