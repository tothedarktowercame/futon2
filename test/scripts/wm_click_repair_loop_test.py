import importlib.util
import contextlib
import io
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LOOP = ROOT / "scripts" / "wm_click_repair_loop.py"
DEBUGGER = ROOT / "scripts" / "wm_click_debugger.bb"


def load_loop():
    spec = importlib.util.spec_from_file_location("wm_click_repair_loop", LOOP)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class RepairLoopCastTest(unittest.TestCase):
    def test_cast_has_no_stale_or_implicit_identity(self):
        loop = load_loop()
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                loop.parse_args(["run", "--clicks", "1"])
        args = loop.parse_args([
            "run", "--clicks", "1", "--owner", "codex-68",
            "--issuing-caller", "codex-68", "--author", "wm-author",
            "--reviewer", "wm-reviewer", "--repair-reviewer", "wm-repair-reviewer",
        ])
        self.assertEqual("wm-repair-reviewer", args.repair_seat)
        self.assertNotIn("proof", repr(vars(args)))

    def test_reading_refusal_is_not_reported_as_no_policy_family(self):
        record = '''
{:selected-target-reading
 {:schema :wm/selected-target-reading-request-v1
  :status :refused
  :kind :selected-target-reading-validation-refused
  :published false
  :target "M-one"
  :detail {:stderr "display cue offsets do not match"}}
 :decision {:abstention {:status :abstained
                         :targets [{:target "M-one"
                                    :kind :no-policy-family
                                    :declines []}]}}}
'''
        with tempfile.NamedTemporaryFile("w", suffix=".edn") as fh:
            fh.write(record)
            fh.flush()
            result = subprocess.run(
                ["bb", str(DEBUGGER), fh.name], cwd=ROOT,
                text=True, capture_output=True, check=True)
        self.assertIn("selected-target-reading-validation-refused/before-cascade-construction",
                      result.stdout)
        self.assertNotIn("no-policy-family/unknown", result.stdout)
        self.assertIn('"phase":"before-cascade-construction"', result.stdout)

    def test_repair_packet_bounds_large_shared_artifacts(self):
        loop = load_loop()
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            page = root / "held.md"
            log = root / "click.log"
            page.write_text("held evidence " + "x" * 200_000)
            log.write_text("click evidence " + "y" * 200_000)
            click = {
                "log": str(log),
                "last-result": {
                    "click-id": "wm-click-test",
                    "outcome": "abstained",
                    "run-record": "/shared/full-run-record.edn",
                    "enormous-field": "z" * 200_000,
                },
            }
            packet = loop.repair_packet(
                "wm-click-loop-repair-test", "selection stopped", click,
                ["prior " + "p" * 200_000], page)
        self.assertLessEqual(len(packet), loop.REPAIR_PROMPT_MAX_CHARS)
        self.assertLessEqual(len(packet), 1_800)
        self.assertIn(str(page), packet)
        self.assertIn("/shared/full-run-record.edn", packet)
        self.assertNotIn("enormous-field", packet)
        self.assertIn("VERDICT: repaired", packet)


if __name__ == "__main__":
    unittest.main()
