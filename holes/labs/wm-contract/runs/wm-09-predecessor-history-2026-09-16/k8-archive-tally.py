"""Bounded pinned archive copy; never invoke previous! on production roots."""
import hashlib, json, pathlib, shutil, subprocess, tempfile
repo = pathlib.Path('/home/joe/code/futon2')
r = repo / 'holes/labs/wm-contract/runs/wm-09-predecessor-history-2026-09-16'
census = pathlib.Path('/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/revision-21-inputs/archive-census.json')
data = json.loads(census.read_text())
assert data['count'] == len(data['files']) == 24
with tempfile.TemporaryDirectory(prefix='wm09-k8-archive-') as tmp:
    tmp = pathlib.Path(tmp)
    root = tmp / 'wm-full-loop'
    source_root = repo / 'data/wm-full-loop'
    records = []
    for row in data['files']:
        close = pathlib.Path(row['path'])
        rel = close.relative_to(source_root)
        assert rel.parts[:2] == ('archives', 'stop-line-2026-07-15')
        assert hashlib.sha256(close.read_bytes()).hexdigest() == row['sha256'], str(close)
        dest = root / rel.parent
        for source in close.parent.rglob('*'):
            assert not source.is_symlink(), str(source)
        shutil.copytree(close.parent, dest)
        for source in close.parent.rglob('*'):
            if source.is_file():
                copied = dest / source.relative_to(close.parent)
                assert hashlib.sha256(source.read_bytes()).digest() == hashlib.sha256(copied.read_bytes()).digest()
        records.append({'original': str(close), 'copy': str(root / rel), 'sha256': row['sha256']})
    pinned = tmp / 'receipt_construction.clj'
    pinned.write_bytes(subprocess.check_output(['git', 'show', '858aa7f7:src/futon2/aif/receipt_construction.clj'], cwd=repo))
    with (r / 'k8-archive-tally.log').open('w') as log:
        run = subprocess.run(['clojure', '-M:test', str(r / 'k8-archive-tally.clj'), str(root), str(pinned), str(r / 'K8-ARCHIVE-RESULTS.edn')], cwd=repo, stdout=log, stderr=subprocess.STDOUT)
    # Re-read pinned production closes to demonstrate no mutation by this run.
    for row in data['files']:
        assert hashlib.sha256(pathlib.Path(row['path']).read_bytes()).hexdigest() == row['sha256']
    (r / 'K8-ARCHIVE-COPY.json').write_text(json.dumps({'source_commit': '858aa7f7','census_sha256': hashlib.sha256(census.read_bytes()).hexdigest(),'verified_copied_closes': records,'exit': run.returncode,'production_close_hashes_unchanged': True,'temporary_copy_removed_on_exit': True}, indent=2)+'\n')
    assert run.returncode == 0
