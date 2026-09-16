# WM-01-receipts-2 — author checkpoints

Author codex-2; independent reviewer claude-3. All author-verified entries remain
pending independent review and reviewer reruns. `matrix.json` is the running
per-receipt record, in frozen manifest order. No entry is independently accepted
by this document.

## Authority and population

Dispatch SHA-256: `16746e129484779432fa2835aa8f5aba2e09cfab086238e40fe9ab1c113b8ad4`.
Frozen manifest SHA-256: `d976a45eabf8825bb64f5f29fd7a7c3a02d547c110115e41c3c2cf5d5277ab57`.
Both were read from p4ng `wm-walkthroughs/build-loop/claude-3/` and verified.
The manifest's prior baseline hash was also verified. Fresh `baseline.log`
records 26 UNATTESTED-DRIFT declarations across exactly these 13 receipts,
exit 1. The already accepted predictive kernel receipt is outside the manifest.

`population.json` records the before population. Every `attribution.json`
contains old, repaired and HEAD declaration text and full hashes. For all 26
changed pins, the predecessor pin equals the slice at 480a666ad2^ and the live
pin equals both 480a666ad2 and HEAD. This was checked for every changed slice,
not inferred from a sample. `repair-diff.json` preserves the exact output string
of `git -C /home/joe/code/mathlib4 show 480a666ad2 -- <source paths>`; paths are
the distinct `path` values in that receipt's attribution. JSON encoding retains
Git's context-line spaces without introducing trailing whitespace into the
artifact file. Two initial Clojure helper syntax errors were corrected before
any re-attestation; the retained final lint/parens runs pass. One commit check
caught raw Git diff whitespace before that evidence was encoded as JSON.

## Per-receipt evidence

Each directory is named by its canonical receipt filename and contains:

- `predecessor.edn`: exact predecessor bytes, including old timestamp.
- `fresh-predecessor-derivation.edn`: fresh basis-record invocation at repaired
  sources; this does not pretend to re-execute historical Lean sources.
- `successor.edn`: second fresh basis-record invocation with explicit dependency
  pins and a fresh timestamp. The historical result is not substituted for
  execution even where the result and toolchain remain equal.
- `derivation-diff.edn`: old-only, new-only and common EDN fields.
- `old-validation.edn`, `validation.edn`: actual validator reports.
- `result.json`: old/new byte hashes, timestamps, all old/new declaration pins,
  theorem axiom results and independent mutation results for every changed pin
  and every new pin. Its candidate successor hash is distinct from a canonical
  hash if an entry is later blocked; the matrix identifies that case.
- `semantic.json`: source reading, precise before/after meaning, adapter/fixture
  assessment, dependency additions, registered negative modes and their actual
  exit conventions. The matrix includes this disposition.
- `commands.json`: wrapper and protocol command argument vectors, cwd and exits;
  `*.log` / `*.exit` preserve output and the actual process status.
- `protocol-commands.json`: every subprocess invoked through the existing
  protocol, including cwd options, full stdout/stderr, exit and arguments for
  fresh toolchain and theorem/axiom probes. Each generated probe imports the
  receipt's module and runs `#print axioms` on its recorded theorem.
- `before-hashes.json` and `after-hashes.json`: unchanged source and fixture
  bytes. `protected-before.json` separately pins all negative Lean sources and
  the already accepted predictive kernel receipt.

EDN artifacts are the authoritative typed values. JSON views render ratios as
JSON numbers and keyword/vector map keys as strings; they are audit views, not
replacement fixtures, adapters or probability representations. No canonical
mass or fixture was converted through JSON.

## Execution and reproduction

Cwd for commands below is `/home/joe/code/futon2`. Run directory abbreviated R:
`holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-receipts-2`.

- Before gate: `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`,
  exit 1; `baseline.log` and `baseline.exit`.
- Attribution: `bb R/receipt_control.clj prepare`, exit 0; `prepare.log` and
  `prepare.exit`. This is initial population capture, **not** a reviewer rerun:
  do not overwrite preserved predecessors by running prepare after replacement.
- Each entry: `python3 R/checkpoint.py INDEX`; its commands.json records the
  exact `bb R/receipt_control.clj run FILENAME ADDITIONS` and wrapper commands.
  Checkpoint runner requires manifest order and commits each completed entry.
  For review, use basis-record/validate on the preserved or canonical receipt,
  and rerun the individually recorded wrapper/control commands in a separate
  evidence location; do not overwrite author evidence.
- `clj-kondo --lint R/receipt_control.clj`: exit 0, 0 errors/warnings; lint.log.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
  '(arxana-check-parens-cli)' -- --no-defaults R/receipt_control.clj`: exit 0;
  parens.log. File was passed after `--`.
- Python syntax: `ast.parse` on checkpoint.py, exit 0; python-syntax.log.

Lean commands are serial short-lived `lake env lean` probes in canonical
mathlib4; no build, output-artifact flag, reload, serving action, click or push.
No commands are reused across different receipt subjects, including the shared
Holes variationalFreeEnergy pin in precision and variational-free-energy.

Controls copy the receipt, fixture and declaration files into a temporary root.
The valid copy must pass. Each mutation starts afresh from that valid successor,
changes only one selected hash to 64 zeros, and invokes the real validator;
exactly `[:positive-source-drift]` is required. Canonical receipts are never
mutated for these controls. Only a fully passing protocol result is installed
for subsequent actual wrapper runs.

## Limits

Adapter and dependency-closure maps remain exactly equal as EDN values; their
declared-not-derived boundaries are unchanged. Added carrier/precision slices
make specific dependencies explicit, not a complete transitive dependency
closure. No formal verification of Clojure adapters is claimed. These are
hand-derived mathematical fixtures, not empirical observations or runtime Q.
A zero-drift gate alone would not establish per-receipt acceptance, WM-01
completion or R4 admission. R4 remains blocked. No known-stale entries, fixture
rewrites, gate suppressions or out-of-scope fixes are authorized here.

## Final disposition: 12 verified, 1 blocked

The 13-entry matrix is complete. Twelve successor receipts are installed in
separate commits; `completed-commits.json` records their full commit identities
in manifest order. The final checkpoint commits the blocked thirteenth entry
and campaign evidence, without replacing its canonical predecessor.

There were **47 individual changed/added-pin mutation controls**, all reporting
exactly source drift. All 13 candidate successors passed fresh basis-record and
real validation. All **34** registered wrapper invocations were run serially:
**33 exited 0; one exited 2**. Lint/parens and both Python syntax checks passed.
`SUMMARY.md` provides a compact per-receipt table; `final-audit.json` records
counts and the final byte checks. Protected negative fixtures and the accepted
predictive kernel receipt are unchanged, as are all 13 fixtures and the source
files checked before/after. No FUTON_POSITIVE_LEAN_OVERRIDE was set.

### Blocked: variational-free-energy-positive-receipt.edn

Its positive, --negative-value and --negative-type modes exit 0 against the
candidate successor. --negative-weakened-positive exits **2**. The real
validator reports precisely `:positive-source-drift` for its source override,
but the wrapper also requires that the weakened theorem elaborate. Its
replacement RHS still contains:

```lean
variationalFreeEnergy (fun _ => gaussianReference.precision)
```

The repaired function requires PrecisionMap. Reproducing exactly the existing
replacement in a temporary source file gives Lean exit **1**:
`gaussianReference.precision has type ℝ but is expected to have type NonnegativeReal`.
This is a stale control construction, not a failure of the positive theorem or
an escaped source-hash mutation. The wrapper's printed "mutation slipped" is
misleading here. `weakened_probe.py` retains the exact generated source, raw Lean
output and command/cwd/exit in the blocked receipt directory. Its Python runner
exits 0 after recording the child Lean exit 1; the child result is the evidence.

Repairing that replacement would edit an explicitly out-of-scope wrapper, so
no such change was made. The existing mathematical reference remains meaningful,
but its required controls cannot all pass under this dispatch. The candidate
successor is retained only in the run directory; the canonical predecessor was
restored byte-for-byte after confirming it still matched this task's installed
candidate. The blocked matrix entry distinguishes candidate and canonical hashes.
A separate dispatch is needed for the wrapper correction before this receipt can
be accepted. No blocked claim was reworded, and no gate was weakened.

### Final gate

Command, cwd `/home/joe/code/futon2`:
`bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`.
`final-gate.log` / `final-gate.exit` record **exit 1, 2 drifts**, down from 26.
Both remaining drifts are in the blocked variational-free-energy receipt:
constantGaussianReference and Holes variationalFreeEnergy. No unrelated drift
appeared. The zero-drift target was not reached; U71 and R4 remain blocked.
Independent review of the twelve valid checkpoints remains with claude-3.
