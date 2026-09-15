"""Write the bounded author receipt from retained evidence, without publishing."""
import hashlib
import json
from pathlib import Path
import runpy
import shlex
import subprocess

P = Path(__file__).resolve().parent
R = Path('/home/joe/code/futon2')
helpers = runpy.run_path(str(P / 'run.py'))
before = json.loads((P / 'before.json').read_text())
fragment = R / 'checks/witness-fragments/PredictiveOutcomeKernel.edn'
registry = R / 'checks/witness-registry.edn'
sha = helpers['sha']
assert subprocess.check_output(['git', 'show', before['source-commit'] + ':checks/witness-fragments/PredictiveOutcomeKernel.edn'], cwd=R) == (P / 'old-fragment.edn').read_bytes()
assert sha(registry) == before['registry-sha256']
after = {'fragment-sha256': sha(fragment), 'registry-sha256': sha(registry)}
(P / 'after.json').write_text(json.dumps(after, indent=2) + '\n')
helpers['run']('final-artifact-check-corrected', ['bb', '-cp', 'scripts', '-e',
    '(require (quote [witnesses.node-witness :as n])) '
    '(let [p "' + str(P.relative_to(R)) + '/" '
    'old (n/read-edn (slurp (str p "old-fragment.edn"))) '
    'new (n/read-edn (slurp "checks/witness-fragments/PredictiveOutcomeKernel.edn"))] '
    '(assert (= (first (:node-witnesses old)) (n/read-edn (slurp (str p "old-entry.edn")))) ) '
    '(assert (= (dissoc old :node-witnesses) (dissoc new :node-witnesses))) '
    '(assert (= [(n/subject (first (:node-witnesses new)))] [(n/read-edn (slurp (str p "successor-subject.edn")))])) '
    '(println "PASS: old entry preserved, wrapper unchanged, successor subject matches"))'],
    'historical entry, unchanged wrapper metadata and exact successor subject')
helpers['run']('python-syntax-final', ['python3', '-B', '-c',
    'from pathlib import Path; ps=sorted(Path("' + str(P) + '").glob("*.py")); '
    '[compile(p.read_bytes(),str(p),"exec") for p in ps]; print("PASS",[p.name for p in ps])'],
    'final Python scripts syntax without bytecode writes')

commands = []
for path in sorted((P / 'logs').glob('*.json')):
    x = json.loads(path.read_text())
    commands.append((x['started'], path.stem, x))
command_text = '\n\n'.join(
    f"### {name}\n\n- Command: `{shlex.join(x['argv'])}`\n- Cwd: `{x['cwd']}`; environment: `LEAN_NUM_THREADS=1`.\n- Exit: **{x['exit']}**. Scope: {x['scope']}.\n- Raw output: [stdout](logs/{name}.stdout), [stderr](logs/{name}.stderr); [invocation](logs/{name}.json)."
    for _, name, x in sorted(commands))

receipt = f'''# WM-01-admission-1 — bounded R4 successor, publication blocked

Author: codex-4. Reviewer: claude-3; review of this successor subject is pending.

## Author verdict

The editable R4 fragment now contains only a **proposed**, formal-only successor.
Scoped preparation and binding controls pass. **Not admitted; not globally published;
required gates are not all green.** The global merger refuses the out-of-scope R17
pin. The existing merger control suite and both required witness wrappers also fail,
as detailed below. This commit is the scoped artifact/refusal handoff authorized by
the dispatch, not a completion or deployment claim.

Authority: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-admission-1.md`, SHA-256
`42b8442d5c363edcbf2fad23679a5e5b36710c4e4cb925150350fad6e031bcdc`.
Starting futon2 commit: `{before['source-commit']}`.

## Identity and retirement

`R4-forward-model-float-carried-production-pins-v1`
→ `R4-forward-model-float-carried-formal-support-v2`.

Chose **removal from the editable fragment**, preserving the historical entry verbatim
below and in `old-entry.edn`, and the entire original fragment in `old-fragment.edn`.
Demoting the old admission to a proposal would misrepresent a withdrawn production
claim as work awaiting admission. No retired status was invented. Wrapper metadata
is unchanged. The old verification and review were not copied to the successor.

The generated global registry still contains the old historical admission because
validation refuses before any write. Its old verified-binding header is **not a current
gate result**. Removal is effective in the editable inventory and isolated generated
inventory only; global retirement/publication remains blocked. No global registry was
hand-edited or assembled while excluding R17.

## Formal successor and verification preparation

`successor-subject.edn` contains the exact immutable subject. The claim is constructed
formal witness and support laws only. It refers to the unchanged-name theorem
`productionPinnedFloatCarried` as a theorem about constructed rows, without inheriting
the historical claim of production acquisition or independent runtime composition.
Support laws are discharged by the constructors against `MachineModelSpec`.

`verification-preparation.edn` binds that subject to current source pins and the already
accepted formal controls from mathlib4 `f40c936a64`, futon2 author `6f494738` and reviewer
`e99c08c4`. It is a preparation record, **not a freshly executed Lean verification receipt**.
It pins the accepted control invocations/transcripts for the reviewer's inspection.
No qualifying subject-bound verification/review pair has been asserted or manufactured.
The predecessor's formal review is evidence about its own packet, not approval of this
new registry subject. Promotion is outside this dispatch.

The retained approximate row totals `1 + 1/2^55`; its support laws and admitted bound do
not establish exact normalization. There are no fresh observations or production
composition correspondence claims.

| Source | Current SHA-256 |
|---|---|
| MachineForwardModelWitness.lean | `62b15fe45ed0cc9eb12eb659b55122c1894cbcb5778b64feec758e965d427389` |
| MachineModelSpec.lean | `020af77ed11f0b5407bd6dd22a0a9e6e81ae30132ac41508277c0567500ca80f` |
| FloatCarriedRowCorrespondence.lean | `f8e9cdacaa47f2bd4131a41bd2876c5731356cd7b33f2dea3268125360be6e22` |
| Holes.lean | `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3` |

## What the canonical validator actually checked

- Historical unmodified R4 fragment, scoped merge: exit 1,
  `:node-witness-pin-mismatch` on its actual MachineForwardModelWitness pin.
  Existing `resolve!` separately rejects both actual stale R4 locators, including Holes.
- Proposed successor, scoped merge: exit 0 with `:pending`, seeded against the original
  global registry for identity preservation. Scoped `--check` roundtrip: exit 0.
  `scoped-registry.edn` is this isolated output, **not a global publication**.
- Proposed validation checks shape/authority and reads available paths, but does not
  enforce hash equality or review admission. Therefore `new-pins` separately calls the
  existing `resolve!` over every successor/preparation locator. All resolve.
- Reusing the old ID for the changed subject: the canonical scoped merger rejects with
  `:node-witness-id-revision-required`, exit 1, against the original admitted registry.
- Tampering the new artifact pin: existing `resolve!` rejects with
  `:node-witness-pin-mismatch`, exit 1. This is explicitly a direct pin check,
  not a claim that proposed-mode merge enforces admission checks.
- Borrowing the old review: existing `receipt!` rejects the new subject with
  `:node-witness-subject-mismatch`, detail `:review`, exit 1. No fake verification
  or review was added to make this control reach the intended check.
- Full canonical `--check` and default regeneration both exit 1 before writing.

## Exact global blocker

I independently scanned **56 fragments / 212 pinned locator occurrences**. Before:
exactly three stale occurrences (R17 Holes; R4 Holes; R4 MachineForwardModelWitness).
After: exactly one stale occurrence, in `checks/witness-fragments/DirichletConcentrations.edn`:

- ID: `R17-dirichlet-accumulation-ieee-residuals-v1`.
- Subject: `mathlib4/DarkTower/WarMachine/Holes.lean`, declaration
  `DarkTower.WarMachine.Holes.DirichletConcentrations`.
- Pinned: `4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b`.
- Actual: `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3`.
- Refusal: `:node-witness-pin-mismatch`.

The complete inventories and raw global refusals are retained in `logs/`. R17 was not edited.

## Additional failed gates and bounded diagnosis

1. `bb -cp scripts:test test/witnesses/merge_witnesses_control.bb`: **exit 1** at
   `:genuine-deterministic-merge`. Its first block selects machineObservation,
   machinePrecision and machineChannelPredictionError, not this R4 fragment. Both
   of its merges succeed, but its assertion requires all 24 entries to be proposed;
   these entries are admitted. Its test and selected fragments match starting HEAD.
   The later `:stale-bytes`, `:missing-declaration`, `:nil-receipt`, `:missing-readback`
   and `:id-reuse` blocks are **not reached**. The separate R4 controls above do not
   turn this suite green. No test or admission was changed to route around the failure.
2. Both required wrapper negative commands: **exit 2**, printing `mutation slipped`.
   A separate call to the existing positive-receipt validator returns `:pass? false`,
   failures `[:positive-source-drift]`; elaboration, axioms, fixture and toolchain checks
   pass. `positive-source-drift-detail.stdout` identifies the stale declaration slice.
   Direct negative Lean fixtures, invoked serially from `/home/joe/code/mathlib4`, both
   exit **0**. The negatives still reject, but their wrapper gates remain failed.
   The positive receipt is outside scope and was not changed.
3. My initial direct Lean diagnosis used `lake -d` from futon2 and reported incompatible
   headers. That invocation selected the wrong launch toolchain; it is **not evidence
   of a broken canonical cache**. Those failed attempts remain in the logs. Correcting
   the diagnostic working directory (the same one the wrapper uses) gives the two
   passing direct fixture results above. No cache rebuild or source change was made.
4. Initial clj-kondo rejected the new control's namespace/file mismatch. Corrected the
   namespace; final lint and final check-parens both exit 0. The initial failure is retained.
5. The first final-artifact diagnostic had a delimiter typo in its inline form and exited
   1 before execution. The corrected invocation passes; both attempts are retained.

## Before and after

| Artifact | Before SHA-256 | After SHA-256 |
|---|---|---|
| PredictiveOutcomeKernel.edn | `{before['fragment-sha256']}` | `{after['fragment-sha256']}` |
| witness-registry.edn | `{before['registry-sha256']}` | `{after['registry-sha256']}` |

Historical lexical entry SHA-256: `{before['old-entry-sha256']}`.
The source commit's fragment bytes match `old-fragment.edn`; the extracted old entry
parses identically; wrapper metadata is unchanged; the new subject matches the fragment.
Raw logs are explicitly retained despite the repository's general `logs/` ignore rule.

## Remaining WM-01 clauses / return to owner

This packet does not close WM-01. Remaining: actual subject-bound verification and
independent review/admission for the proposed identity; global publication after the
R17 conflict is resolved; the other dependent proof/consumer bindings and independent
repair acceptance; shared State/Action/Outcome/model-revision and numeric semantics;
production correspondence and joined consumer evidence; numerical operations and their
error semantics. No serving action, click, push, checkbox edit or new worker task occurred.

Owner question: how should the out-of-scope R17 retirement, stale merger-suite status
expectation and predictive positive-receipt source drift be assigned? This receipt
reports them; it does not elect or start their successors.

## Historical entry (verbatim)

```edn
{(P / 'old-entry.edn').read_text()}
```

## Exact retained command index

Every invocation below retains its exit and unfiltered output. A diagnostic process exit
0 does not override a printed validation failure; expected negative exits are explained
above. Python orchestration stopped on unexpected gates; remaining independent gates
were invoked separately without changing those failed results.

{command_text}
'''
(P / 'RECEIPT.md').write_text(receipt)
files = {str(f.relative_to(P)): hashlib.sha256(f.read_bytes()).hexdigest()
         for f in sorted(P.rglob('*')) if f.is_file() and f.name != 'manifest.json'}
(P / 'manifest.json').write_text(json.dumps(files, indent=2) + '\n')
print('Receipt and manifest written; global registry unchanged.')
