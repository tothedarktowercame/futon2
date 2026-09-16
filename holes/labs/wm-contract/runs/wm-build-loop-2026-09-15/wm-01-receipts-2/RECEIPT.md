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
