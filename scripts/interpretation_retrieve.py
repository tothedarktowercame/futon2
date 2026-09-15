#!/usr/bin/env python3
"""Retrieval-only port; no cascade construction, clipping, or relevance judgments."""
import importlib.util
import json
from pathlib import Path
import shutil
import sys
import tempfile


def load_module(path, name, snapshot=None):
    sys.path.insert(0, str(Path(path).parent))
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    if snapshot is None:
        spec.loader.exec_module(module)
    else:
        # Preserve the implementation's ordinary __file__/import resolution,
        # but execute the captured bytes rather than a mutable working file.
        exec(compile(Path(snapshot).read_bytes(), str(path), 'exec'), module.__dict__)
    return module


def retrieve(request, loader=load_module):
    module = loader(request['implementation'], 'interpretation_retriever', request.get('implementation_snapshot'))
    if request['kind'] == 'embedding':
        # Override the module's import-time index with the captured input.
        module.EMB = {r['id']: r['vector'] for r in json.loads(Path(request['index']).read_text())}
        return module.ranked_candidates(request['query'], pool=request['k'])
    if request['kind'] != 'tier0':
        raise ValueError('unknown retriever')
    # The real loader reads title/keywords from files, so supply captured files
    # in its ordinary filesystem layout, inside this attempt, for this call only.
    with tempfile.TemporaryDirectory(prefix='retrieval-', dir=request['attempt_dir']) as tmp:
        root = Path(tmp)
        for source in request['library']:
            relative = Path(source['relative'])
            if relative.is_absolute() or '..' in relative.parts or len(relative.parts) != 2:
                raise ValueError('invalid library relative path')
            dest = root / relative
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source['snapshot'], dest)
        return module.retrieve_all(request['query'], k=request['k'],
                                   index_path=Path(request['index']), library_root=root,
                                   extra_library_dirs=())


if __name__ == '__main__':
    print(json.dumps(retrieve(json.load(sys.stdin))))
