# L5-8c — independent census, not selector self-certification

Base: fcf96f5d7 (packet-containing main). Branch: codex11/l5-8c. Worktree only.

L5-8's selector roster is retained as provenance but is no longer the Q1 open side
in either export. I accept the correction: sharing it with the enumeration made
Q1 coverage tautological.

## Producer and reader

selection-world/capture-census reuses default-task-sets (explicit file registry),
sorts its three independent sets, and retains source, code-root and captured-at.
The runtime captures this once per invocation at decision retention, after selection,
and attaches world-census to the initial retained section, pre-score refusal and
bypass records. Reselection inherits the same census through the existing initial-
section merge; each invocation clears any inherited census delay. The value never
enters scoring. Read failures retain a typed failed census rather than a fabricated
empty open set.

Both exports now prefer tick world-at-selection ids, then the immutable selection
record's world-census, then snapshot. Selection reference resolution preserves the
tick's world ids and attributes their source to the tick path. Standalone records
read their own census. Selector field-facts/open-tasks is never a source of Q1 open
ids. Diagnostics fieldFacts retains selectorRoster plus per-kind census-only and
selector-only IDs; those diagnostics add no RunFacts fields.

Regression controls deliberately omit M-gated from the selector roster while the
independent census includes it: open side and enumerated side differ, and diagnostics
name M-gated. A provenance-only record falls to snapshot. The real runtime retention
function is exercised with a temporary immutable decision and injected independent
census reader, including world precedence and one capture across repeated retention.
L5-8's old self-roster expectations were explicitly replaced; L5-8b's reference tests
now supply a world-census, not a field roster.

## Gates

| Arm | Tests | Assertions | Failures | Errors |
|---|---:|---:|---:|---:|
| Base outer + projector | 173 | 716 | 19 | 2 |
| Branch outer + projector | 175 | 732 | 19 | 2 |
| Base named record/refusal/world | 22 | 91 | 0 | 0 |
| Branch named record/refusal/world | 22 | 91 | 0 | 0 |
| Final branch named + work projector | 94 | 340 | 0 | 0 |

Broad comparison failure/error names and locations are identical.
Final named rerun follows the explicit per-invocation census reset.
Kondo zero errors/warnings; check-parens.el OK; diff whitespace check clean.
Fresh JVM data root and scratch FUTON_WM_* overrides used. All measured test runs:
canonical file counts 45865→45865.

The global hermeticity gate is NOT green: broad base/branch and named branch runs
exit 86 on concurrent wm-click-loop/click-003-1793557357.log changes. Named base
exited 0. The active claude-2 continuous loop was observed (PID 4147161); no process
was stopped, no guard bypassed and no live bytes restored. These are separate from
the assertion results above. No reload, merge, click or intentional production-data
write occurred in this packet.

## Click 6: branch export, read-only

Input:
 /home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-ca9ec295-4f4a-417c-a3ba-a7245a2bbe4b.edn

Full export: /home/joe/notes/l5-8c/click6-selection.json.
Summary with exact set differences: /home/joe/notes/l5-8c/click6-summary.json.

This historical standalone record has no world-census and no selector roster
diagnostic. It therefore reads the explicitly attributed current snapshot:
181 missions + 278 excursions + 191 tickets = **650 open**, versus **634 enumerated**.
17 open IDs are missing from enumeration; M-ukrns-wp is enumerated but absent from
the current open census. This cannot satisfy Q1's partition equality. It is NOT a
claim about a recovered historical click-time census; no historical bytes were
rewritten or census backfilled. No Lean execution is claimed for this export.

## Evidence hashes

Logs, commands, environment overrides and counts: /home/joe/notes/l5-8c/.
- baseline.log: SHA256 746be00e809a83b1e6d45709c83935084168b5b97db256aa8195d7c485284324
- changed.log: SHA256 0e75d3071ea10b5c5eca701045c4ae85021132b8ff902af85d04366273fa2ac1
- named-final.log: SHA256 99f3dfd9a38d7e53de8ee01f7c0aea32c0b6ae559062749cf6ab93d6f1cec71d
- click6-selection.json: SHA256 75bc9aa7e46e4e5f0e8cffaafa492a16291ae4abeb2cf8a05906358dcbf63dd6
- click6-summary.json: SHA256 bc9a214757cf719c39123cc142cc4457670e7d4f627ef6d45249cdc077747c85
- kondo.log: SHA256 c62898926ed0befcef701f575b6fcfd8b4bd0e8f3b23e0188b4893e963c9603f
- parens.log: SHA256 a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87
