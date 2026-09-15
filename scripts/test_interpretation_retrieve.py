"""Exercise real retrieval APIs with a fixed embedding vector; no model download."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import interpretation_retrieve as port


class RetrievalOnly(unittest.TestCase):
    def test_loader_executes_captured_bytes(self):
        with tempfile.TemporaryDirectory() as tmp:
            live = Path(tmp) / 'live.py'
            live.write_text("raise AssertionError('mutable source executed')")
            snapshot = Path(tmp) / 'captured.source'
            snapshot.write_text('value = 42')
            module = port.load_module(str(live), 'snapshot_fixture', str(snapshot))
            self.assertEqual(42, module.value)
            self.assertEqual(str(live), module.__file__)

    def test_existing_embedding_api_without_constructor(self):
        module = port.load_module('/home/joe/code/futon3a/holes/labs/M-memes-arrows/cascade_construct.py', 'embedding_fixture')
        with tempfile.TemporaryDirectory() as tmp:
            index = Path(tmp) / 'index.json'
            index.write_text(json.dumps([{'id': 'first', 'vector': [1, 0]}, {'id': 'second', 'vector': [0, 1]}]))
            with patch.object(module, '_embed', return_value=[1, 0]), patch.object(module, 'construct_cascade', side_effect=AssertionError('constructor invoked')) as constructor:
                rows = port.retrieve({'kind': 'embedding', 'implementation': 'unused', 'index': str(index), 'query': 'tension', 'k': 2}, lambda *_: module)
                self.assertEqual(['first', 'second'], [r['pattern_id'] for r in rows])
                constructor.assert_not_called()

    def test_existing_whole_index_api_uses_captured_library(self):
        module = port.load_module('/home/joe/code/futon6/scripts/cas_select.py', 'tier0_fixture')
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            index = root / 'index.tsv'
            index.write_text('family/p\tx\ty\tOriginal title\toriginal\n')
            source = root / 'p.source'
            source.write_text('@flexiarg family/p\n@title Witness receipt\n@keywords witness\n')
            with patch.object(module, 'retrieve_all', wraps=module.retrieve_all) as retrieve_all:
                rows = port.retrieve({'kind': 'tier0', 'implementation': 'unused', 'index': str(index), 'query': 'witness', 'k': 8,
                                      'attempt_dir': tmp, 'library': [{'relative': 'family/p.flexiarg', 'snapshot': str(source)}]}, lambda *_: module)
                self.assertEqual('p', rows[0]['pattern'])
                retrieve_all.assert_called_once()
                self.assertEqual((), retrieve_all.call_args.kwargs['extra_library_dirs'])


if __name__ == '__main__':
    unittest.main()
