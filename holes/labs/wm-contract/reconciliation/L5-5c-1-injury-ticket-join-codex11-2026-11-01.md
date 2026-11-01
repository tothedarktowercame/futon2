# L5-5c-1 — injury-to-ticket join

Base `af7dd0b3f`; branch `codex11/l5-5c-1-injury-ticket-join`. No merge, serving reload, historical publication, or selection change.

## Implemented boundary

`meta_injury_store.clj` publishes after durable terminal injury ingest, including duplicate replay; medic cause reattribution also publishes its replacement opening. Publication uses the original opening's cause pin across repeated observations. Low-level pure classification/ledger functions remain effect-free. Publication failure returns `:injury-ticket-publication-failed`, retains `:ledger-result` and `:ledger-durable? true`, and is retryable by re-ingesting the same record. It does not claim the append rolled back.

`repair_obligation/publish-injury-ticket!` verifies cause bytes and run identity, locks the repair store, and reuses a finding joined by injury identity, exact cause pin, or origin run identity. Ambiguous joins refuse; failure-kind/target coincidence is not a join. A finding owned by another injury is not repurposed. New ids are deterministic `repair-injury-<injury hash>` / `T-repair-injury-<injury hash>`.

New findings carry `:injury-id`, `:cause-record-pin` and `:origin-run/id` inline. Existing finding bytes are preserved. Ruling job **9462** (`invoke-1793549616331-9462-55609bf2`) authorized immutable attachments: `injury-joins/<sha256 [injury-id finding-id]>.edn`, containing those keys plus finding id, exact finding sha, and ticket id. This uses existing `repair_obligation/write-new-durable!` (CREATE_NEW, force file and directory), following the append-only approach of `occurrence-evidence!` and administrative dispositions such as `dismiss-unexecuted!`. Attachment replay acknowledges only byte-identical bytes; conflict throws.

`injury_ticket_join/resolve-join` gives the same normalized join for inline and attachment forms. Missing evidence is typed absent; replaced finding bytes give `:injury-join-finding-sha-mismatch`. `medic_ticket_closure/own-cause-findings` uses this resolver and cannot bypass a stale attachment through origin-run fallback. The unchanged field producer admits ordinary OPEN tickets via registry and Giraffe. L5-5c-2 can use this resolver; no obligation fact or arbitration is added here.

The existing ticket publisher preserves ticket edits, appends one EDN injury-provenance block with the three keys and finding sha, and uses its existing queue/commit path. Conflicting blocks refuse. Parent lines now support M/E/T targets, already supported by the relation producer. New finding parents come only from pinned failure target or selected-event target. Absent discharge contracts stay absent; no repair authority is manufactured.

The default ledger uses the configured repair store; an explicitly isolated ledger uses a sibling `repair-obligations` directory. The existing ticket publication environment override remains supported.

## Nine-injury read-only replay

The historical `data/wm-meta-injuries/ledger.edn` has nine distinct pinned injury ids; this is **not** a claim of nine effective active injuries. All nine source byte pins verify. None joins an existing finding under the exact identity/pin/run-id rule. All nine would create a new inline-keyed finding and ticket. No historical ticket was written. Three sources (`initialization`, `run-assigned`, `stop`) are retained /tmp fixtures, not production-run evidence.

The replay supplies hypothetical ticket text only at that ticket's file-read port; `focus-receipt/classify-target` itself is unchanged. It uses current versioned focus inputs, `decision-focus` at `2026-11-01T16:00:00Z`, and live `relation-context` for `/home/joe/code`. This is current-producer replay, not historical-focus reconstruction. Full pins, focus hash and receipts: `L5-5c-1-nine-injury-replay-2026-11-01.edn`.

| Injury prefix | Kind | Retained parent | Would-be relation | Form |
|---|---|---|---|---|
| 1bc0efe3 | initialization-failed | absent | unknown: no-parent-relation | New inline |
| 42fd2c21 | open-cascade-review-answer-invalid | M-self-improvement-loop | unknown: embedding-below-floor | New inline |
| 75747f06 | required-checkpoints-missing | absent | unknown: no-parent-relation | New inline |
| 821c15d0 | no-selection | absent | unknown: no-parent-relation | New inline |
| 9c998f2e | fold-output-invalid | M-pattern-posteriors | unknown: embedding-node-not-retained | New inline |
| 9f1d0c67 | no-terminal-state | absent | unknown: no-parent-relation | New inline |
| a173047e | untyped-failure | M-stack-geometry | associated via M-aif4iad embedding neighbour | New inline |
| d755c886 | invalid-close-outcome | absent | unknown: no-parent-relation | New inline |
| ff6e0452 | machine-repair-lacks-grounded-review-evidence | absent | unknown: no-parent-relation | New inline |

One associated, eight unknown. Publication cannot turn absent relation evidence into a known relation. Initial failure-target-only inspection missed two selected-event parents; final replay uses the publisher's fallback.

## Validation

- Focused injury-ticket join, durable injury store and medic cause-reattribution: **12 tests / 71 assertions, zero failures/errors**.
- Controls: duplicate ingestion produces one registry ticket; real verified Giraffe fixture admits it; attachment preserves finding bytes; inline/attachment joins agree; appended newline conflicts with exact replay; finding tampering makes join and closure absent; injected publication failure recovers without another ledger write.
- Twelve outer namespaces plus both projectors: base and branch **169 tests / 685 assertions, 19 failures / 2 errors**, identical failure-name multiplicities.
- Existing named namespaces (meta-injury-store, finding-ticket, repair-obligation, medic-ticket-closure, cause-reattribution): base **62 / 354, 10 failures / 17 errors**; branch plus new tests **65 / 375, 10 failures / 17 errors**, identical failure-name multiplicities. The publication-root override conflicts with old fixtures expecting explicit local destinations; other retained baseline errors remain. These suites are not claimed green.
- Canonical data count **45,737 before and after every comparison/focused run**. Discovered FUTON_WM path overrides point to scratch. No serving invocation.
- clj-kondo zero errors/warnings on all changed/new Clojure files; check-parens OK; diff check clean.

Logs, environment, counts and reproduction scripts: `/home/joe/notes/l5-5c-1/`. `comparison.json` records exact base/branch failure-name counts; `replay.clj` is the read-only replay; `run-tests.py` records hermetic roots.

## Evidence hashes
- `replay.clj`: `78487c8253f0599ee43a0c624ae19bbc232e30967bf29889454961bc968b41d4`
- `replay-relations.edn`: `f00a40fd9db466f4e2396a6f6131dd40a7eb46efdd8a0bf7be1b20919bea89b9`
- `focused-final2.log`: `6d7eda41a834b41f649a2557f64cd6a775a8b9134b3d33205ce6cea566e7195d`
- `baseline.log`: `3da51649a78d431b625f1fde81cda4562121911fb6aa348dac77f1e83dd26fc1`
- `changed.log`: `357d9a36536e15294347cc3c3fb25edfdb253a598a70c5fdeb29c8eb9ebd3a06`
- `named-baseline.log`: `dc95860c9fa02d1655ad49e0b00fd07180cfbe67a790e5415ebbc2fb0575ade1`
- `named-final.log`: `c1937ccba84ba64487725bc71853d660f0427020ba4f25ca694d0bf14b66353c`
- `comparison.json`: `2ffc745098a82208d8255d83c99e53831178e1b1d7fb8341317e2931cb5c7a81`
- `kondo-final.log`: `943d72544b0dd41cec0f6347c5888deffb4e591a41d3695462766a40353c5473`
- `parens.log`: `a12b7cb43c9d9134b5bb1b35e9096b66775d9e92e7611d1cc92b02edd6782a87`
