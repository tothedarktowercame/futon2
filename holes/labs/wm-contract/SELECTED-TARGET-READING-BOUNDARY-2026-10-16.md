# Selected-target reading boundary repair — 2026-10-16

## Disposition

No-go for another live click.  The offset boundary is structurally repaired,
but the saved-answer replay gate is 2/6, below the required 6/6 (or an explained
5/6).  This note records the evidence rather than treating another click as a
test of a known failure rate.

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
| M-essays-edit-cycle | refused | relation outside the documented vocabulary |
| M-web-arxana-ui-improvements | refused | display-cue density 6/9, budget 4 |
| M-trip-journal | refused | display-cue density 6/9, budget 4 |
| M-the-futon-stack | refused | the cue `futon0` occurs twice in its fragment |

This also explains the previously unknown M-trip-journal failure: it was not an
offset failure; it violated the cue-density rule.

## Audit of the same risk at registered handoffs

The production handoff index has 16 sites.  A search for model-supplied offsets,
digests and positions found:

- `reading/selected-target`: high risk, repaired here.  Exact locations are now
  machine-derived and both attempts are retained.
- `reading/selected-policy`: the response repeats a library SHA-256 already
  pinned by the issued request.  The machine checks it, so it cannot silently
  corrupt identity, but requiring the model to reproduce it is unnecessary and
  remains a producer-reliability risk.
- `interpretation/legacy-action`: the response is asked to reproduce source and
  value digests.  The consumer recomputes and rejects mismatches, so integrity is
  fail-closed, but liveness still depends on model transcription.
- `limb-evidence` digests and the runner's action/source digests are computed by
  code from files or bytes, not accepted on the model's authority.
- No other registered production handoff asks an agent for source offsets.

The two digest-transcription sites require separate compatibility decisions:
removing their response fields changes their published contracts.  They are not
silently declared repaired by this packet.

## Gate before another click

1. Offset arithmetic is machine-owned: complete.
2. Every attempt is retained: complete for future contacts.
3. M-trip-journal is explained: complete.
4. Other registered handoffs were checked for the same risk: complete; two
   digest-transcription risks remain explicitly recorded above.
5. Saved-answer replay reaches 6/6, or 5/6 with one explained non-offset
   refusal: **failed (2/6)**.

The next work is offline producer evaluation/correction against these saved
requests.  It is not authorization for a live click.
