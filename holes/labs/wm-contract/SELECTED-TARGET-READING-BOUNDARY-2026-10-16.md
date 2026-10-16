# Selected-target reading boundary repair — 2026-10-16

## Disposition

The boundary repair passes the saved-answer replay gate at 6/6.  This is an
offline producer/consumer result, not by itself authorization for a live click.

## Boundary repair

`selected-target-reading/resolve-analysis-offsets` treats the author's exact
quoted text as the input and derives Unicode-codepoint offsets inside the named
sentence and fragment.  The author-provided numbers are not trusted.  A quote
that is absent or occurs more than once in its scope is a typed refusal.  The
unchanged Python validator remains publication authority after projection.

Every contact is retained separately as `analysis-input-attempt-N.json`; every
deterministic projection is retained as `analysis-resolved-attempt-N.json`.
`analysis-input.json` remains only as a compatibility copy of the latest
projection.  A correction can no longer destroy the first response.

## Saved-answer replay

Run with:

```sh
clojure -M scripts/replay_selected_target_readings.clj \
  /home/joe/code/storage/wm-target-reading-requests
```

The replay uses the real resolver and `session_turn_analysis.py complete` in a
temporary directory; it does not publish readings or alter saved requests.

| target | result after offset projection | remaining reason |
|---|---|---|
| M-a-sorry-enterprise | validated | — |
| M-goals-and-holes | validated | — |
| M-essays-edit-cycle | validated | legacy relation names projected through the declared compatibility map |
| M-web-arxana-ui-improvements | validated | excess display cues deterministically omitted to the sentence budget |
| M-trip-journal | validated | excess display cues deterministically omitted to the sentence budget |
| M-the-futon-stack | validated | repeated `futon0` resolved by its structural position at the fragment start |

This also explains the previously unknown M-trip-journal failure: it was not an
offset failure; it violated the cue-density rule.

## Audit of the same risk at registered handoffs

The production handoff index has 16 sites.  A search for model-supplied offsets,
digests and positions found:

- `reading/selected-target`: high risk, repaired here.  Exact locations are now
  machine-derived and both attempts are retained.
- `reading/selected-policy`: repaired.  The response no longer carries a
  library SHA-256; receipts take it from the sealed issued request.
- `interpretation/legacy-action`: repaired.  The raw agent receipt is retained;
  the machine derives source-byte and interpretation-value digests into a
  separate canonical receipt before validation.
- `limb-evidence` digests and the runner's action/source digests are computed by
  code from files or bytes, not accepted on the model's authority.
- No other registered production handoff asks an agent for source offsets.

No registered production handoff now relies on an agent to calculate source
offsets or reproduce a machine-known digest.

## Gate before another click

1. Offset arithmetic is machine-owned: complete.
2. Every attempt is retained: complete for future contacts.
3. M-trip-journal is explained: complete.
4. Other registered handoffs were checked for the same risk: complete; two
   digest-transcription risks remain explicitly recorded above.
5. Saved-answer replay reaches 6/6, or 5/6 with one explained non-offset
   refusal: **passed (6/6)**.

This gate is satisfied.  A separate operator decision still governs any live
click.
