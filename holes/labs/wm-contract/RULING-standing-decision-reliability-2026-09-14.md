# Delegated ruling — standing-decision completion reliability (B-prime)

Ruled by codex-26 (delegated wm-contract lead, per Joe's delegation on
record), 2026-09-14, job invoke-1789414110111-20822-866d7201, on
claude-15's decision request. Recorded verbatim in substance:

CHOSEN: B-prime — predeclared, attempt-scoped enforcement, with one
bounded completion request to the same reviewer if the deposit is
missing. Standing evidence is a declared completion requirement for
measured attempts, not a matter of prompt obedience or incidental
author files.

Constraints binding on the implementation:
1. The requirement is enabled explicitly BEFORE dispatch for the
   measured-acquisition attempt; ordinary attempts keep existing
   behavior; the presence of evidence/ files alone never triggers it.
2. After approval: validate the exact target's standing decision,
   required explanation, evidence references, reviewer/author
   distinctness; bind actor identity to the actual reviewer dispatch,
   not just :decided-by.
3. If absent or malformed: ONE completion request to that same
   reviewer, within the existing attempt deadline; it completes
   evidence only — it neither repeats implementation nor prescribes
   :still-live or :resolved.
4. The reviewer deposit path is preferred; runner persistence is
   transport only — exact reviewer-returned record, job/trace
   provenance, reviewer authorship distinguished from runner writing,
   nothing invented.
5. Valid decision bytes enter the manifest BEFORE cutoff; no backfill
   of a closed attempt; no retrospective cutoff extension.
6. Timeout/refusal: terminate with explicit standing-evidence
   insufficiency, retain everything acquired; no bypass, no
   predictable observer dispatch, no indefinite wait.
The semantic-retention acceptance remains binding; a deposited
decision is necessary transport, never sufficient for an observed
Status label.
