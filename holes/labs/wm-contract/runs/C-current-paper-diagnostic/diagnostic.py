#!/usr/bin/env python3
"""Read-only pilot. Run from any cwd: python3 /path/to/diagnostic.py.
Requires bb for the repository's EDN inputs. No shared JVM or live endpoints.
Semantic annotations are explicit review inputs, not inferred from word matches.
"""
import hashlib
import json
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[5]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def edn(path):
    expression = "(require '[clojure.edn :as e] '[cheshire.core :as j]) (println (j/generate-string (e/read-string (slurp (first *command-line-args*)))))"
    return json.loads(subprocess.check_output(
        ['bb', '-e', expression, str(ROOT / path)], text=True))


def claim_satisfaction(claim_scope, evidence_scope):
    # These are scope categories, not a total ordering of scientific evidence.
    if claim_scope == 'outline-only' and evidence_scope in {'outline-only', 'measurement-completed'}:
        return True
    if claim_scope == 'measurement-completed' and evidence_scope == 'outline-only':
        return False
    if claim_scope == 'measurement-completed' and evidence_scope == 'measurement-completed':
        return True
    return None


def compare(before, after):
    """Partial ordering on known, binary satisfaction coordinates only.

    For each improved coordinate, KL(delta_fail||C)-KL(delta_pass||C)
    = log(p/(1-p)) > 0 throughout 1/2 < p < 1. Unchanged unknowns
    are not scored. Changed unknowns block a comparison. No joint model.
    """
    require(before.keys() == after.keys(), 'Different coordinate sets')
    if any(before[k] != after[k] and None in (before[k], after[k]) for k in before):
        return 'unresolved-measurement'
    up = [k for k in before if before[k] is False and after[k] is True]
    down = [k for k in before if before[k] is True and after[k] is False]
    if up and down:
        return 'tradeoff-unranked'
    return 'improves-known-coordinates' if up else 'worsens-known-coordinates' if down else 'unchanged'


def main():
    registry = json.loads((HERE / 'preference-registry.json').read_text())
    def read_source(path):
        if path.startswith('p4ng/'):
            return subprocess.check_output(['git', '-C', str(ROOT / 'p4ng'), 'show', registry['paper_revision'] + ':' + path.split('/', 1)[1]])
        return (ROOT / path).read_bytes()

    for path, digest in registry['source_pins'].items():
        require(hashlib.sha256(read_source(path)).hexdigest() == digest, f'Stale source: {path}')
    for entry in registry['entries']:
        for source in entry['sources']:
            p = ROOT / source['path']
            require(hashlib.sha256(read_source(source['path'])).hexdigest() == source['sha256'], f'Stale preference source: {p}')
            lo, hi = source['lines']
            require('\n'.join(read_source(source['path']).decode().splitlines()[lo-1:hi]) == source['verbatim'], f'Quote mismatch: {p}')
    grid = edn('futon0/analysis/business-models/grid.edn')
    require(len({(r['case'], r['phase']) for r in grid}) == len(grid), 'Duplicate acceptance cells')
    counts = {field: {v: sum(r[field] == v for r in grid) for v in ['y', 'n', 'unknown']} for field in ['solved', 'paid']}
    require(all(sum(c.values()) == len(grid) for c in counts.values()), 'Unknown outcome vocabulary')
    previous = edn('futon2/holes/labs/wm-contract/runs/C-realization-family/result.edn')
    divergence = previous['C']['divergence']
    a = registry['claim_annotation']
    # Ensure the explicit annotation still refers to the recorded claim.
    # The word match binds the annotation; it does not establish its truth.
    require(a['claim'] in read_source(a['source']).decode(), 'Claim annotation no longer bound')
    before = {e['id']: None for e in registry['entries']}
    before['claim-warrant'] = claim_satisfaction(a['scope'], a['available_scope'])
    before['semantic-satisfaction'] = divergence['delivered'] != divergence['explicit-denial']
    after = dict(before, **{'claim-warrant': claim_satisfaction(a['counterfactual_scope'], a['available_scope'])})
    controls = {
        'unknown-does-not-pass': claim_satisfaction('measurement-completed', 'unknown') is None,
        'unknown-does-not-become-failure': compare({'x': None}, {'x': False}) == 'unresolved-measurement',
        'opposing-improvements-unranked': compare({'x': False, 'y': True}, {'x': True, 'y': False}) == 'tradeoff-unranked',
        'reverse-change-worsens': compare(after, before) == 'worsens-known-coordinates',
        'unchanged-does-not-improve': compare(before, before) == 'unchanged',
    }
    require(all(controls.values()), 'Negative control failed')
    drift = sorted({s['path'] for e in registry['entries'] for s in e['sources'] if (ROOT / s['path']).read_bytes() != read_source(s['path'])})
    if drift:
        print('Paper working-tree drift from pinned revision: ' + ', '.join(drift), file=sys.stderr)
    print(json.dumps({
        'schema': 'c-current-paper-diagnostic-v1',
        'source-validation': 'passed',
        'paper-revision': registry['paper_revision'],
        'acceptance-grid': {'cells': len(grid), 'observations': counts, 'interpretation': 'unrun is not failure; no external usefulness estimate'},
        'current-satisfaction': before,
        'semantic-probe-origin': 'reuses pinned prior executable result; does not rerun the underlying Clojure code',
        'claim-assessment': 'conditional on explicit analyst scope annotation; independent review pending',
        'counterfactual': {'historical': False, 'action': 'revise producer thesis to claim an outline', 'satisfaction': after, 'comparison': compare(before, after)},
        'calculation': {'family': 'C_i=(p_i,1-p_i), 1/2<p_i<1', 'local-KL-improvement-nats': 'log(p_claim-warrant/(1-p_claim-warrant)) > 0', 'numeric-masses': 'not-assigned', 'joint-distribution': 'not-specified', 'future-success-probability': 'not-estimated'},
        'controls': controls,
        'next-tasks': {e['id']: {'lifecycle-phase': e['phase'], 'task': e['next_work']} for e in registry['entries']},
        'production-C': 'unchanged; this is a retrospective diagnostic and conditional comparison',
    }, indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
