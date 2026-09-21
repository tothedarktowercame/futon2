#!/usr/bin/env python3
"""Read local Git history and the existing audit CSV; no builds or writes."""
import csv
import hashlib
import json
import pathlib
import subprocess

ROOT = pathlib.Path('/home/joe/code')

def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(ROOT / repo), *args], text=True).strip()

series = ROOT / 'futon0/analysis/audits/commit-timeseries-2026-09-21.csv'
rows = list(csv.DictReader(series.open()))
windows = {}
for lo, hi in [('2026-08-23', '2026-08-29'), ('2026-08-30', '2026-09-05'),
               ('2026-09-07', '2026-09-21')]:
    subset = [r for r in rows if lo <= r['date'] <= hi]
    windows[f'{lo}..{hi}'] = {
        'rows': len(subset),
        'sums': {k: sum(int(r[k]) for r in subset)
                 for k in ['futon2', 'war_machine', 'war_machine_lean', 'everything_else']}}
paths = ['src/futon2/aif/cascade_model_manifest.clj', 'resources/wm',
         'data/wm-repair-obligations', 'holes/labs/wm-contract/rulings']
print(json.dumps({
    'heads_at_inventory': {repo: git(repo, 'rev-parse', 'HEAD')
                           for repo in ['futon2', 'futon3c', 'mathlib4', 'p4ng', 'futon0']},
    'csv': {'path': str(series), 'sha256': hashlib.sha256(series.read_bytes()).hexdigest(),
            'first_date': rows[0]['date'], 'last_date': rows[-1]['date'], 'windows': windows,
            'around_facade': [r for r in rows if '2026-08-28' <= r['date'] <= '2026-09-02']},
    'futon2_two_weeks': git('futon2', 'log', '--since=2026-09-07T00:00:00Z',
                           '--until=2026-09-21T23:59:59Z', '--no-merges',
                           '--format=%H %aI %s').splitlines(),
    'model_and_declaration_history': git('futon2', 'log', '--since=2026-09-07T00:00:00Z',
                                        '--no-merges', '--format=%H %aI %s', '--', *paths).splitlines(),
    'lean_two_weeks': git('mathlib4', 'log', '--since=2026-09-07T00:00:00Z',
                         '--no-merges', '--format=%H %aI %s', '--', 'DarkTower/WarMachine').splitlines()
}, indent=2))
