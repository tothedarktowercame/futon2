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


class PairedRunAgreementTest(unittest.TestCase):
    """The unit test above compares two fixtures the test itself wrote, so it
    cannot see a wiring error in main()'s receipt construction.  This one uses a
    text report and a receipt emitted by the SAME reporter run, so sourcing a
    receipt field from the wrong variable makes it fail."""

    FIXTURES = pathlib.Path(__file__).parent / "fixtures"

    def setUp(self):
        self.text = (self.FIXTURES / "wm-status-report-2026-09-02.txt").read_text()
        self.receipt = json.loads(
            (self.FIXTURES / "wm-status-receipt-2026-09-02.json").read_text())

    def test_paired_run_agrees(self):
        self.assertTrue(REPORTER.receipt_agrees_with_text(self.receipt, self.text))

    def test_paired_run_rejects_a_miswired_field(self):
        """The exact defect this guards: contract.hole sourced from the closed
        count.  11 holes reported as 108 is the number the paper's figure hangs
        on, and the fixture-only test passes straight through it."""
        miswired = json.loads(json.dumps(self.receipt))
        miswired["contract"]["hole"] = miswired["contract"]["closed"]
        self.assertFalse(REPORTER.receipt_agrees_with_text(miswired, self.text))

    def test_pin_staleness_is_recorded_not_hidden(self):
        pin = self.receipt["contract-pin"]
        self.assertIn("json-sha", pin)
        self.assertIn("module-last-commit-sha", pin)
        self.assertEqual(pin["fresh?"],
                         pin["json-sha"] == pin["module-last-commit-sha"])


if __name__ == "__main__":
    unittest.main()
