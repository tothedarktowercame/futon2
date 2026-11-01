# L5-10-I — Environmental recovery: a medic disposition kind with a fresh-probe receipt (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` (≥ the commit this packet lands in). Worktree only. Authority: your L5-10-D
(answer 2) and Joe, 2026-11-01 ~19:20Z: "medic doesn't need any special approval, it is just the
codeword for a repair done while the machine is stopped. go ahead". The machine is stopped (loop
paused 18:31Z). Injury `injury/93d40d074ad3902d7b3a602007496b745c8c635f` (click 6, futon1b 504 on
`GET /api/alpha/hyperedges?type=clock/clocked-on&limit=100&include-total=false`) is the case.

## Behaviour

1. **New correction kind** in `medic_disposition.clj`: `:environmental-recovery`, fields =
   common fields + `:cause-record-id :cause-pin :classification :probe-receipt-pin
   :probe-request-digest :probe-status :probe-elapsed-ms :probe-deadline-ms :probed-at`.
   `:classification` must be the diagnosis's `:environmental-*` classification (pinned by
   `:evidence-pin` = the diagnosis receipt pin); any other classification refuses. Candidate =
   evidence, as for the other kinds. No new default anywhere.
2. **Probe** (`futon2.aif.environmental-probe`, new, pure apart from the one HTTP call): from the
   injury's `[:failure :detail]` (`:url`, method GET, `:timeout-ms`), re-run the named external
   read once with a bounded deadline (the original `:timeout-ms`, never longer), and build a
   receipt `{:schema :wm/environmental-probe-receipt-v1 :injury-id :cause-pin :request {:method
   :url :deadline-ms} :request-digest :started-at :ended-at :elapsed-ms :status :http-status
   :response-digest|:typed-failure :probed-by :source-revision}`. `:status :checked` only when
   the HTTP status is 2xx within the deadline; otherwise `:refused` with the typed failure. Retain
   content-addressed under `<meta-injury root>/acceptances/environmental-probes/` via the
   existing `out-of-band-fix/retain!` (or an equivalent that never overwrites). The probe never
   touches the ledger.
3. **Authority predicate** `environmental-probe/checked-medic-authority`, shaped like
   `out-of-band-fix/checked-medic-authority`: the persisted receipt at the operation's
   `:authority-receipt-pin` equals the given receipt, `:status :checked`, injury ids match,
   candidate = evidence = the operation's candidate, the candidate's probe fields equal the
   receipt's. Nothing else is trusted.
4. **Transaction**: one function (`environmental-probe/discharge!` or in `meta_self_heal_production`)
   that, given the ledger path, the injury id and a checked receipt, builds the medic operation
   (`:operation-id` = digest of the candidate, `:expected-ledger-pin` = the ledger's current pin,
   `:request {:candidate :evidence}`, `:authority-receipt-pin`) and calls
   `meta-injury-store/transact-medic-disposition!` with attribution
   `{:medic-disposition :environmental-recovery :fixed-by nil :out-of-band-receipt-pin <probe
   pin> :medic-stamp {...verifier "futon2.aif.environmental-probe/checked-medic-authority"}}`.
   Confirm (and test) that after the append `effective-active-injuries` no longer lists the
   injury, so the next click's self-heal gate takes the ordinary route.
5. **Do not apply to the live ledger.** Provide the exact command for the live application; I
   run it while the machine is stopped. Hermetic tests only; `data/` untouched.

## Tests

- A fixture ledger with an open environmental injury and a fixture HTTP endpoint (local server or
  stubbed client port): probe 200 within deadline → receipt `:checked`; discharge appends a
  medic-repaired observation; effective active injuries exclude it; a second identical discharge
  is `:duplicate`.
- Probe 504 / timeout → receipt `:refused`; discharge refuses `:medic/...` (name it); ledger bytes
  unchanged.
- Diagnosis classification not environmental → refused before any probe.
- Tampered receipt (one byte) → authority false → refused; ledger unchanged.
- Named + medic namespaces vs main; hermetic; data count unchanged.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex5/l5-10-i` in a worktree. Bell
claude-2 with sha, counts, and the live-application command.

## Not in this packet

The automatic version (the self-heal route running this probe itself at click time when the
diagnosis is environmental, instead of the terminal-receipt replay) is the follow-on, L5-10-I-2,
after Joe sees this one work.
