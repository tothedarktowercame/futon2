# E4-ACCEPTANCE-PUBLICATION-ISOLATION — read-only audit (zai-6, 2026-10-31)

Row: E4-ACCEPTANCE-PUBLICATION-ISOLATION (deficits tsv). Zero source edits, no click,
no reload, no merge, no canonical store or ledger write. All evidence cited by pin;
nothing here changes tracker state.

## Question

Can the acceptance path in the open-out-of-band medic route
(`:meta-acceptance-run-fn` → `run-acceptance!` / `out-of-band/compute!`) mutate
canonical tickets, stores or publication roots?

## Answer

**Yes it did, once, on 2026-10-29 17:23–17:24Z — caught and refused post-hoc.
Under the code as of futon3c `f70612064` (2026-10-29 17:31:26Z) and later, no:
retained receipts from all five subsequent 10-29 medic acceptance runs show the
three canonical surfaces unchanged. Three residual vectors are named below;
they are detected-after, not prevented-before, and none is an observed new defect.**

## Mechanism (current code)

- Wiring: `ports` (futon3c `src/futon3c/wm/meta_self_heal_adapters.clj` ~1104–1113)
  binds `:meta-acceptance-run-fn` → `run-acceptance!` (same file ~859), honoring
  `:execution-repository`. `open-out-of-band-route!` (futon2
  `src/futon2/aif/meta_self_heal_production.clj` 1807) calls
  `out-of-band/compute!` (futon2 `src/futon2/aif/out_of_band_fix.clj` 198), which
  runs the acceptance test twice: current at the canonical repo, counterfactual in
  a throwaway detached worktree with the fix reverse-applied.
- `launch-bounded-acceptance!` (adapters ~792) runs the test in a `systemd-run`
  unit with cwd = execution repository, environment from
  `acceptance-store-environment` (adapters 35–45): nine redirected roots —
  trace, run-records, phase-replay-inputs, learning-ledger, morning-brief,
  meta-injuries, **repair-obligations**, **ticket-publication** (both added by
  f70612064), plus `FUTON_CAUSE_SCOPE_RECEIPT` — all under one temp root deleted
  after the run (`remove-tree!`; receipts carry `:store-isolation {:status
  :isolated, :removed? true}`).
- Ticket destination: `futon2.aif.finding-ticket/destinations` — with
  `FUTON_WM_TICKET_PUBLICATION_ROOT` set, ticket dir and queue resolve under the
  isolated root; `commit-ticket!` discovers its git repo by upward `rev-parse
  --show-toplevel` from the ticket path, so a temp-root publication cannot commit
  anywhere.
- Guards: `compute!` refuses `:out-of-band/canonical-dirty` before running and
  `:out-of-band/canonical-head-moved` after, refusing (not reverting) if the
  canonical HEAD moved during the run. Failed/refused worktrees are retained.

## What actually happened on 2026-10-29 (pins)

- 17:18:50Z artifact `011cf4fcbe16b5c1…` retained (injury `injury/c5606345…`,
  fix `37db58d9…`, stage `:ticket-readiness-refresh`).
- **17:23:50–17:24:00Z: four ticket commits landed on canonical main** —
  `ef42d1d2f`, `d82fef29d`, `1436a8bc9`, `b83c0d203`, each "Publish repair
  ticket T-repair-occ-…" for findings `c58f3172…`, `5b321608…`, `aa95debe…`,
  `4255668d…`. Cause: at that moment the acceptance JVM environment redirected
  only **six** roots; `FUTON_WM_REPAIR_OBLIGATION_ROOT` and
  `FUTON_WM_TICKET_PUBLICATION_ROOT` were absent, so the acceptance JVM's
  repair-obligation store defaulted to canonical `data/wm-repair-obligations` and
  `finding-ticket/destinations` took the canonical branch: repo
  `/home/joe/code/futon2`, live queue path, git commit into canonical.
- 17:24:00Z receipt `08a6f6b25e62ccaf…` (out-of-band-results):
  `:refused :out-of-band/canonical-head-moved`, base `3071442490bf…` — the
  post-run guard caught the mutation that the run itself had caused.
- 17:31:26Z futon3c `f70612064` "Give acceptance JVMs isolated repair and ticket
  publication roots" — both roots added to `acceptance-store-environment`.
- 17:33:59Z futon2 `f3579f90e` "Remove 16 acceptance-generated repair tickets
  from the live field" (forward disposal approved by claude-11; provenance census
  SHA-256 `520be2cb113037b8a202793b1f1bb7984bc7365e837bdaccc78b74caf9275f34`;
  finding bytes quarantined separately; the four findings above are absent from
  the canonical findings store — ticket-link receipts for exactly those four ids
  remain, matching the quarantine).

## Retained post-fix medic runs (all :checked, all isolated)

| Receipt (out-of-band-results) | Time (mtime) | Injury | Fix | Base | Store isolation |
|---|---|---|---|---|---|
| `8f896a5d087d4145…` | 17:36:37 | c5606345 | 37db58d9 | f3579f90 | :isolated, cur+cf temp roots removed |
| `c8c6c14e0fac7cea…` | 19:34:35 | 1e3f6a62 | c31ad5e4 | 8b834460 | :isolated, removed |
| `198c734a4c8f643d…` | 19:43:41 | 1e3f6a62 | c31ad5e4 | 682b5afa | :isolated, removed |
| `5e15f8ecb80ec277…` | 20:51:11 | a46779bc | 98eb5696 | ae623faf | :isolated, removed |
| `0bb3dfe4e74b3037…` | 21:53:26 | f7e97cc6 | 5712b906 | b94329b50 | :isolated, removed (9 env keys incl. CAUSE_SCOPE) |

Each passed the `:canonical-head-moved` post-check, so canonical HEAD — and with
it `holes/tickets` and `data/wm-repair-obligations` — was unchanged across every
post-fix acceptance execution on 10-29. The later canonical "Publish repair
ticket" commits that day (`7afcc26ac`, `31d8aadb9`, `f526a800a`, `27da170aa`,
`68bfe8a40`, `8ed9ad099`, `60f87b90b`) are ordinary click-path publications whose
findings are present in the canonical findings store — distinguishable from the
17:23 acceptance-generated four, whose findings are not.

## Residual vectors (named, no observed instance, not blocking)

1. **cwd = canonical repo on the current leg.** Nothing structurally prevents an
   acceptance test from shelling `git` in its cwd; only the post-hoc
   canonical-dirty / canonical-head-moved guards would catch it (as on 10-29).
   Detection, not prevention.
2. **`FUTON_PATTERN_LIBRARY_ROOT` is not redirected** (default
   `/home/joe/code/futon3/library`). A runner test writing a pattern artifact
   would land canonically. No such write appears in any 10-29 receipt.
3. **`git worktree add` registers admin entries under the canonical
   `.git/worktrees`** (repo metadata; retained failed worktrees persist by
   design). Not tickets/stores/publication roots, recorded for completeness.

## Row closure

Closure condition — "real acceptance-process publication leaves canonical HEAD,
holes/tickets and repair-obligation store unchanged; retained test evidence and
reviewed provenance census" — is **met**: five post-fix retained receipts with
`:isolated`/removed store roots and unchanged canonical HEAD; hermetic test
evidence `acceptance-environment-includes-repair-publication-roots`
(futon3c `test/futon3c/wm/meta_self_heal_adapters_test.clj` ~1745) pins both
redirected roots; reviewed provenance census `520be2cb…` covers the one real
pre-fix mutation. No live witness is still owed by this row beyond what is
retained. Residual vectors 1–3 above are reported for the queue rule (none blocks
the next dispatch on current evidence).
