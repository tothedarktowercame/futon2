"""Run isolated gates, retaining unfiltered output and exact process metadata."""
import datetime
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path('/home/joe/code/futon2')
PACKET = Path(__file__).resolve().parent
FILES = ['src/futon2/aif/observation_authority_resolver.clj',
         'test/futon2/aif/observation_authority_resolver_test.clj',
         'test/futon2/aif/categorical_state_observation_test.clj']


def run(name, argv):
    output = PACKET / 'evidence'
    output.mkdir(exist_ok=True)
    stem = name
    attempt = 1
    while (output / (stem + '.json')).exists():
        attempt += 1
        stem = name + '-' + str(attempt)
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    p = subprocess.run(argv, cwd=ROOT, capture_output=True, text=True)
    (output / (stem + '.stdout')).write_text(p.stdout)
    (output / (stem + '.stderr')).write_text(p.stderr)
    (output / (stem + '.json')).write_text(json.dumps(
        {'command': argv, 'cwd': str(ROOT), 'started': started, 'exit': p.returncode},
        indent=2) + '\n')
    print(stem, 'exit', p.returncode, flush=True)
    print(p.stdout, p.stderr, flush=True)
    return p.returncode


if __name__ == '__main__':
    mode = sys.argv[1]
    if mode == 'syntax':
        exits = [run('kondo', ['clj-kondo', '--lint', *FILES]),
                 run('parens', ['emacs', '-Q', '--batch', '-l',
                                '/home/joe/code/futon4/dev/check-parens.el', '--eval',
                                '(arxana-check-parens-cli)', '--', *FILES])]
    else:
        ns = 'futon2.aif.' + mode
        exits = [run(mode, ['clojure', '-X:test', ':nses', '[' + ns + ']'])]
    sys.exit(0 if all(x == 0 for x in exits) else 1)
