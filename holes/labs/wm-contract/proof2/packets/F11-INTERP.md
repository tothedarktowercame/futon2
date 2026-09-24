# F11-INTERP — the one interpretation the first flight needs (validated proposal)

Author: kimi-6, 2026-09-24. Follows FLIGHT-TARGET-D (`proof2/packets/FLIGHT-TARGET-D.md`,
0962ed9e), which ranked M-f11-find-production-successor first: one open, faithful,
observable want with no producer. Deliverable: the proposal
`holes/labs/wm-contract/proof2/proposals/M-f11-interpretation.edn`, validated
offline. **Not merged**: `resources/wm/cascade-sources/` is untouched; promotion
is a separate explicit step. Anchors: futon2 `4dd54848003882115eef3740d9838c22250075af`,
futon3 `9c248d5229ea47ba04a2f228cf923ef20cde9169`. No clicks, no JVM loads, no
writes under `data/`.

## 1. The want

`holes/missions/M-f11-find-production-successor.md` L102 (sha256
`48033b45d9a992b1d9dfbddca8593a89823dd15e253ddeb893ce205b092fdf3b` at the anchor):

> `- [ ] Publish the strict successful successor link for repair-024, or retain the typed failure without resolution.`

Token `:hole/h2045faa0e7cc`; C4 locator already in the live sources file (decl = the
same line with `- [x]`). The mission's resolution rule (L47–50): "Repair-024 is
resolved only after this mission's ordinary gates produce strict durable terminal
evidence. A failure — especially `:feature-card-missing-or-invalid` — leaves
repair-024 awaiting validation and stops the continuous loop." The want is therefore
a **promotion decision**: publish the successor link only on qualifying evidence,
else retain a typed finding. The guard's `:needs` is `:hole/h9ab212b3281d` (F11
ordinary acceptance, the `- [x]` at L101 — observed true at HEAD), because the
mission makes resolution downstream of the ordinary gates.

## 2. Pattern choice

**Chosen: `coordination/bind-promotion-to-post-repair-replay`** (futon3 library,
revision `9c248d52`, sha256
`915d4188894a9781555b6f7c79aa792979f3de24d17f1c876cff38db5e2e293f`). Its core:
"Bind certificate promotion to an independent replay of the repaired revision…
Promote only after a successful independent receipt matches that revision and those
controls; otherwise retain a typed pending or failed finding." Both disjuncts of the
want are the pattern's two arms verbatim: publish the strict link = promote on a
revision-matched qualifying receipt; retain the typed failure without resolution =
the typed pending/failed finding. The mission even names the control
(`:feature-card-missing-or-invalid`) the receipt must match.

Runners-up, and why they did not fit:

- **`apparatus/evidence-to-disposition-once`** — close second (and already used for
  the same disjunctive shape in T-repair-occ-444fb018's sources). Its concern is
  *joining several authorities once into a closed-enum disposition at collection
  time*. Repair-024's successor judgment has one authority (the F11 gate evidence)
  and the want is not about constructing the join but about the *promotion gate*:
  what qualifies the link, and what must be retained when nothing qualifies. Wrong
  mechanism for this want.
- **`apparatus/success-must-not-resemble-failure`** — about ledger vocabulary
  (success must not be recorded in fault-shaped terms). The want presupposes the
  vocabulary distinction already; it asks for the *act* of publishing or retaining,
  not for a cleaner terminal state.
- **`contracts/holder-states-the-claim`** — correspondence holders for Lean/runtime
  contract entries; wrong domain (no Lean correspondence is claimed here).
- **`apparatus/repairs-name-defects-not-neighborhoods`** — repair scoping; the
  repair is already scoped (repair-024 is named).
- **`apparatus/done-is-observed-running`** — already declared on this target and its
  own scope-limit says it "does not produce the separate repair-024 successor
  disposition."

## 3. The interpretation (as proposed)

```clojure
{:guard {:needs #{:hole/h9ab212b3281d} :forbids #{:hole/h2045faa0e7cc}}
 :produces #{:hole/h2045faa0e7cc}}
```

Receipt (`:kind :hand-admitted :by "kimi-6" :date "2026-09-24"`) with source pin,
reading, scope-limit, target-source (hole-id
`M-f11-find-production-successor#2045faa0e7cc`, line 102), and observation-limit —
full text in the proposal file. The scope-limit states explicitly that the reading
does not author the independent replay or judge any existing record: enactment makes
that judgment against the named revision and controls.

## 4. Validation transcript (offline, own JVM process; no shared JVM, no clicks)

Method: the `test/fixtures/click2-replay/` shape — the exact input map
`constructed-from-interpretations` hands to `construct`, with the fixture-declared
budget `{:max-moves 8 :max-expansions 10000}`, horizon 4, move-cost 0, and the same
stand-in `:evaluate-g` the fixture test injects (10 for empty precedence, 0
otherwise; the assertion is about construction, not ranking). Script: `/tmp/f11_validate.clj`.

```
=== FALSIFIER: fixture as it stands (no new interpretation) ===
status: :refused  kind: :no-supported-order

=== WITH PROPOSAL merged into interpretations ===
status: :constructed  kind: nil
candidate: [:coordination/bind-promotion-to-post-repair-replay]
           receipt-kind: :machine-constructed  unreached: []

=== LOAD-DECLARED round trip from temp dir ===
targets: (M-f11-find-production-successor)
[:interpretations "M-f11-find-production-successor" :patterns]  keys:
(:apparatus/done-is-observed-running :coordination/bind-promotion-to-post-repair-replay)
pattern value: {:guard {:needs #{:hole/h9ab212b3281d} :forbids #{:hole/h2045faa0e7cc}}
                :produces #{:hole/h2045faa0e7cc}}   ; receipt present under :receipts
```

Three results, read literally:

1. **Without the interpretation the target cannot construct** — `construct` on the
   fixture as committed returns `:refused :no-supported-order` (the open want has no
   producer; the declared pattern produces only the already-true want). This is the
   falsifier: the proposal is doing the work, not the harness.
2. **With it, the target constructs** — one candidate,
   precedence `[:coordination/bind-promotion-to-post-repair-replay]`,
   `:construction-receipt :kind :machine-constructed`, `:unreached-wants []` (the
   open want is produced within horizon 4; the other want is already observed).
3. **The merged sources file loads** — `cascade-sources/load-declared` over a temp
   copy with the proposal spliced in returns the pattern and receipt under the
   target (shape: `[:interpretations <target> :patterns|:receipts]`). Malformed
   files throw in this loader; none did.

## 5. What this does not claim

The proposal is not promoted; the live sources file is unchanged. Construction here
used the fixture's stand-in G, not the judge's G — with the lane's real G the plan
must still beat the empty family (D12's caveat applies at click time, not here). The
interpretation says the pattern *applies*; whether the published link or retained
failure is faithful is the enactment's and the C4 locator's
`:observation-limit`, stated in the receipt.
