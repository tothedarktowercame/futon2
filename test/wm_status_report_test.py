import importlib.util
import json
import pathlib
import unittest


REPORTER_PATH = (pathlib.Path(__file__).parents[1] / "scripts" /
                 "wm_status_report.py")
SPEC = importlib.util.spec_from_file_location("wm_status_report", REPORTER_PATH)
REPORTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORTER)


class ReceiptAgreementTest(unittest.TestCase):
    def test_receipt_agrees_with_text_and_rejects_corruption(self):
        text = """WAR MACHINE STATUS — 2026-09-02T00:00:00+00:00

CONTRACT
declarations=15 closed=9 holes=6 source=contract_lint-live-report
:=sorry-terms=4 sorry-classification={}
NEW component=workspace-gate signature={"exit": 1} reason=test

OVERALL
DEGRADED-NEW exit=1 convention=OK-0/DEGRADED-AS-EXPECTED-0/DEGRADED-NEW-1/DECISION-DUE-3
"""
        serialized = json.dumps({
            "contract": {"declaration-count": 15, "closed": 9, "hole": 6},
            "lean-sorry": {"count": 4},
            "overall": {"verdict": "DEGRADED-NEW", "exit": 1},
            "components": [
                {"component": "workspace-gate", "red": True,
                 "signature": {"exit": 1}, "classification": "new-red"},
                {"component": "strict-lint", "red": False,
                 "signature": {"exit": 0}, "classification": "green"},
            ],
        })
        receipt = json.loads(serialized)
        self.assertTrue(REPORTER.receipt_agrees_with_text(receipt, text))

        corrupted = json.loads(serialized)
        corrupted["contract"]["hole"] += 1
        self.assertFalse(REPORTER.receipt_agrees_with_text(corrupted, text))


if __name__ == "__main__":
    unittest.main()
