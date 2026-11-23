import importlib.util
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SELECTOR = ROOT / "scripts" / "wm_click_select_jvm.py"


def load_selector():
    spec = importlib.util.spec_from_file_location("wm_click_select_jvm", SELECTOR)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class SelectJvmTest(unittest.TestCase):
    def setUp(self):
        self.selector = load_selector()
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.proc = self.root / "proc"
        self.expected = self.root / "sandbox" / "futon3c"
        self.other = self.root / "live" / "futon3c"
        self.expected.mkdir(parents=True)
        self.other.mkdir(parents=True)

    def tearDown(self):
        self.temp.cleanup()

    def pid(self, number, cwd):
        directory = self.proc / str(number)
        directory.mkdir(parents=True)
        (directory / "cwd").symlink_to(cwd, target_is_directory=True)
        return str(number)

    def test_two_candidates_selects_matching_checkout(self):
        live = self.pid(101, self.other)
        sandbox = self.pid(202, self.expected)
        self.assertEqual([sandbox], self.selector.matching_pids(
            [live, sandbox], self.expected, self.proc))

    def test_two_nonmatching_candidates_refuse(self):
        one = self.pid(101, self.other)
        another = self.pid(202, self.root / "another")
        (self.root / "another").mkdir()
        self.assertEqual([], self.selector.matching_pids(
            [one, another], self.expected, self.proc))

    def test_one_nonmatching_candidate_refuses(self):
        live = self.pid(101, self.other)
        self.assertEqual([], self.selector.matching_pids(
            [live], self.expected, self.proc))


if __name__ == "__main__":
    unittest.main()
