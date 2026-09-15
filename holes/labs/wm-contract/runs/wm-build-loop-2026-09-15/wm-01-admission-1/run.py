"""Retain exact commands, outputs, statuses and scopes; never pipe gate output."""
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path('/home/joe/code/futon2')
PACKET = Path(__file__).resolve().parent
REL = str(PACKET.relative_to(ROOT))
FRAGMENT = ROOT / 'checks/witness-fragments/PredictiveOutcomeKernel.edn'
REGISTRY = ROOT / 'checks/witness-registry.edn'
CONTROL = ['bb', '-cp', 'scripts', REL + '/controls.clj']


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(name, argv, scope, expected=0, diagnostic=None, cwd=ROOT):
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    result = subprocess.run(argv, cwd=cwd, text=True, capture_output=True,
                            env={**os.environ, 'LEAN_NUM_THREADS': '1'})
    log = PACKET / 'logs'
    log.mkdir(exist_ok=True)
    (log / (name + '.stdout')).write_text(result.stdout)
    (log / (name + '.stderr')).write_text(result.stderr)
    (log / (name + '.json')).write_text(json.dumps({
        'argv': argv, 'cwd': str(cwd), 'scope': scope, 'started': started,
        'environment': {'LEAN_NUM_THREADS': '1'}, 'exit': result.returncode,
        'expected-exit': expected, 'expected-diagnostic': diagnostic,
    }, indent=2) + '\n')
    print(name, 'exit', result.returncode, flush=True)
    if result.returncode != expected or (diagnostic and diagnostic not in result.stdout + result.stderr):
        print(result.stdout, result.stderr)
        raise RuntimeError('Unexpected result: ' + name)
    return result


def initial():
    if (PACKET / 'old-fragment.edn').exists():
        raise RuntimeError('Historical capture already exists; refuse to overwrite')
    shutil.copyfile(FRAGMENT, PACKET / 'old-fragment.edn')
    # Preserve the exact lexical entry, including its original whitespace.
    text = FRAGMENT.read_text()
    start = text.index('[{:schema :wm/node-witness-v1,') + 1
    end = text.index('],\n :recorded-at', start)
    (PACKET / 'old-entry.edn').write_text(text[start:end])
    head = run('source-head', ['git', 'rev-parse', 'HEAD'], 'source identity').stdout.strip()
    (PACKET / 'before.json').write_text(json.dumps({
        'source-commit': head, 'fragment-sha256': sha(FRAGMENT),
        'registry-sha256': sha(REGISTRY), 'old-entry-sha256': sha(PACKET / 'old-entry.edn'),
    }, indent=2) + '\n')
    run('inventory-before', CONTROL + ['inventory'], 'all pinned locators in all fragments')
    with tempfile.TemporaryDirectory(prefix='wm-r4-old-') as temp:
        fragments = Path(temp) / 'fragments'
        fragments.mkdir()
        shutil.copyfile(FRAGMENT, fragments / FRAGMENT.name)
        run('old-scoped-merge', ['bb', 'scripts/merge_witnesses.bb', '--fragments', str(fragments),
                                '--output', temp + '/registry.edn'], 'unaltered historical R4 fragment only',
            1, ':node-witness-pin-mismatch')
    run('old-pins', CONTROL + ['old-pins'], 'existing resolve! on both actual stale R4 locators',
        diagnostic=':subject-artifact')
    run('prepare', CONTROL + ['prepare'], 'replace one fragment entry; prepare formal successor subject')


def gates():
    run('new-pins', CONTROL + ['new-pins'], 'existing resolve! on successor and preparation locators')
    with tempfile.TemporaryDirectory(prefix='wm-r4-new-') as temp:
        fragments = Path(temp) / 'fragments'
        fragments.mkdir()
        shutil.copyfile(FRAGMENT, fragments / FRAGMENT.name)
        output = temp + '/registry.edn'
        # Preserve identity checks against the real prior registry, even in isolation.
        shutil.copyfile(REGISTRY, output)
        args = ['bb', 'scripts/merge_witnesses.bb', '--fragments', str(fragments), '--output', output]
        run('new-scoped-merge', args, 'one proposed fragment only; NOT global publication', diagnostic=':pending')
        shutil.copyfile(output, PACKET / 'scoped-registry.edn')
        run('new-scoped-check', args + ['--check'], 'isolated generated registry roundtrip')
        run('prepare-reuse-id', CONTROL + ['reuse-id', str(fragments)], 'temporary changed-subject fixture')
        shutil.copyfile(REGISTRY, output)
        run('reuse-id', args, 'canonical merger against old admitted identity', 1,
            ':node-witness-id-revision-required')
    run('tamper-pin', CONTROL + ['tamper-pin'], 'existing resolve! directly; proposals do not enforce pins',
        1, ':node-witness-pin-mismatch')
    run('borrow-review', CONTROL + ['borrow-review'], 'existing receipt! directly on old review and new subject',
        1, ':node-witness-subject-mismatch')
    run('global-check', ['bb', 'scripts/merge_witnesses.bb', '--check'], 'full canonical registry validation',
        1, 'R17-dirichlet-accumulation-ieee-residuals-v1')
    run('global-regenerate', ['bb', 'scripts/merge_witnesses.bb'], 'attempt full canonical publication; expect refusal before write',
        1, ':node-witness-pin-mismatch')
    run('inventory-after', CONTROL + ['inventory'], 'all pinned locators in all fragments after R4 replacement')
    run('merger-controls', ['bb', '-cp', 'scripts:test', 'test/witnesses/merge_witnesses_control.bb'],
        'existing merger control suite')
    remaining_gates()


def remaining_gates():
    for mode in ['unconditional', 'softmax']:
        run('negative-' + mode, ['bb', 'checks/predictive_outcome_kernel_witness.clj', '--negative-' + mode],
            'affected wrapper negative mode; serial; canonical mathlib4')
    run('kondo', ['clj-kondo', '--lint', REL + '/controls.clj'], 'new Clojure control')
    run('parens', ['emacs', '-Q', '--batch', '-l', '/home/joe/code/futon4/dev/check-parens.el',
                   '--eval', '(arxana-check-parens-cli)', '--', REL + '/controls.clj',
                   'checks/witness-fragments/PredictiveOutcomeKernel.edn'], 'control and fragment parentheses')
    before = json.loads((PACKET / 'before.json').read_text())
    after = {'fragment-sha256': sha(FRAGMENT), 'registry-sha256': sha(REGISTRY)}
    assert after['registry-sha256'] == before['registry-sha256'], 'Global refusal must not write registry'
    (PACKET / 'after.json').write_text(json.dumps(after, indent=2) + '\n')


if __name__ == '__main__':
    {'initial': initial, 'gates': gates, 'remaining-gates': remaining_gates}[sys.argv[1]]()
