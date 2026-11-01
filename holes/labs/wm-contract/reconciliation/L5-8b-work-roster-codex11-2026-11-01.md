# L5-8b — work export roster precedence

Base: a97308e8e, containing the packet and L5-8 merge. Branch: codex11/l5-8b.

For each task kind, facts-for-record now reads tick world-at-selection open ids
(including an explicitly empty vector), then the SHA-verified terminal selection
record's field-facts/open-tasks roster, then the existing snapshot.
The terminal is resolved with selection-record/read-chain, as on the selection
export. Sources identify the actual record path and key for either retained carrier.
Reference resolution is delayed until a world carrier is absent. A bad reference
refuses; it does not silently fall back to a current registry. Enumerated ids are
untouched. No embedded unpinned roster is substituted for a referenced decision.

The regression writes a two-decision chain in a temporary directory and proves
terminal (not root) roster selection, all three task kinds, source attribution,
explicit-empty world precedence, unchanged enumerated disagreement, complete old
export equality when the referenced record lacks a roster, and tampered-reference
refusal.

## Validation

| Arm | Tests | Assertions | Failures | Errors | Exit |
|---|---:|---:|---:|---:|---:|
| Pinned main | 84 | 273 | 0 | 0 | 0 |
| Branch | 85 | 286 | 0 | 0 | 0 |

Namespaces: futon2.aif.wm-run-facts-test and wm-check-run-facts-test.
Both use the fresh test-JVM root and scratch FUTON_WM_* overrides.
Canonical data file counts: baseline 45828→45828; branch 45828→45828.
Unlike L5-8's earlier runs, the global hermeticity guard also passed here.
Kondo: zero errors/warnings; check-parens.el: OK; git diff --check: clean.
No reload, merge, live click, or production data write.

## Click 5: historical contradiction is preserved

One read-only probe of run 3f903a0e found that the actual tick **does have**
world-at-selection open-task ids: missions 181, excursions 274, tickets 190,
total 645. Its enumeration has 631 ids. Its referenced immutable selection record
has no field-facts/open-tasks roster (it predates L5-8).
Therefore this tick takes **world-at-selection**, not the fallback described
in the packet's cause paragraph. This change intentionally does not repair that
historical Q1 violation or rewrite either carrier. Future ticks missing world
ids can consume the roster now written by L5-8.

Tick SHA: 27c24998035991d43fbdcf90ab5639a95eadae6951d7e4da3f3f43f9cabeca9a.
Selection SHA: 62575e28c1c83a38396adcac0a16a51b919a593d52225d4a4859f7fc2285d01c.
Selection path:
 /home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-1d0f3570-4a9b-482d-8e7a-9ece12fe224e.edn

## Retained evidence

Commands, scratch environment, counts and logs: /home/joe/notes/l5-8b/.
- baseline.log: SHA256 dd589f47994329c818f2370b3323d00362d11f7bae5d701154df98e4437047b1
- changed.log: SHA256 285f7c7913a30d02d0a43d64525abcbfa2fcd27ceabf21c737aca7129a4cc45b
- probe.edn: SHA256 eee3ba9cfefb776fc5d5fc93eb75620500520fadff546ec319b099831a169b31
- kondo.log: SHA256 f7f86ea54b07cda3316c6074d6e70222e0a9b2ae32afd53d179afc3fcd1fb418
- parens.log: SHA256 a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87
