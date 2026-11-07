"""Unit tests for the pure mapping functions of giraffe_plane_export.py:
item -> payload, census row -> labels."""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "scripts"))

import giraffe_plane_export as gpe  # noqa: E402


def item(**kw):
    base = {
        "id": "T-foo",
        "kind": "ticket",
        "path": "/nowhere/T-foo.md",
        "own_status": "OPEN — <waiting> on review",
        "state_group": "started",
        "readiness": {"status": "ready", "reason": "order/ready-by-unblocks"},
    }
    base.update(kw)
    return base


class ItemLabelsTest(unittest.TestCase):
    def test_ready_gets_giraffe_ready(self):
        self.assertIn("giraffe-ready", gpe.item_labels(item(), None))

    def test_not_ready_no_giraffe_ready(self):
        it = item(readiness={"status": "not-ready", "reason": ""})
        self.assertNotIn("giraffe-ready", gpe.item_labels(it, None))

    def test_gated_on_joe(self):
        self.assertIn(
            "gated-on-joe", gpe.item_labels(item(), {"gated": "1", "live": "True"})
        )

    def test_prose_only_wait_needs_missing_gate_line(self):
        row = {"prose_only": "1", "gate_line": "0"}
        self.assertIn("prose-only-wait", gpe.item_labels(item(), row))
        row = {"prose_only": "1", "gate_line": "1"}
        self.assertNotIn("prose-only-wait", gpe.item_labels(item(), row))

    def test_close_unstated_ticket_but_not_mission(self):
        row = {"close_cond": "no-section"}
        self.assertIn("close-unstated", gpe.item_labels(item(), row))
        m = item(id="M-foo", kind="mission")
        self.assertNotIn("close-unstated", gpe.item_labels(m, row))
        self.assertNotIn("close-unstated", gpe.item_labels(item(), {"close_cond": "stated"}))

    def test_header_unread_when_either_header_label_unparsed(self):
        # Present in the file but not parsed by the summary reader.
        self.assertIn("header-unread", gpe.item_labels(
            item(), {"hdr_date": "", "date_in_file": "1", "hdr_status": "OPEN"}))
        self.assertIn("header-unread", gpe.item_labels(
            item(), {"hdr_date": "2026-11-07", "hdr_status": "", "status_in_file": "1"}))
        self.assertNotIn(
            "header-unread",
            gpe.item_labels(item(), {"hdr_date": "2026-11-07", "hdr_status": "OPEN"}),
        )

    def test_header_unread_catches_parenthetical_status_the_census_missed(self):
        # The T-kangaroo row: status_in_file 0, but own_status found
        # "**Status (triaged 2026-09-01): PARTIAL ...**".
        self.assertIn("header-unread", gpe.item_labels(item(), {
            "hdr_status": "", "status_in_file": "0",
            "own_status": "(triaged 2026-09-01): PARTIAL -- research portions obsolete."}))

    def test_absent_header_is_not_unread(self):
        # No header in the file at all: nothing for the reader to miss.
        self.assertNotIn("header-unread", gpe.item_labels(item(), {
            "hdr_date": "", "date_in_file": "0",
            "hdr_status": "", "status_in_file": "0", "own_status": ""}))

    def test_timebox_absent(self):
        self.assertIn("timebox-absent", gpe.item_labels(item(), {"timebox_in_file": "0"}))
        self.assertNotIn("timebox-absent", gpe.item_labels(item(), {"timebox_in_file": "1"}))

    def test_arxana_hardening_shape(self):
        # From the census row for M-arxana-hardening (2026-11-07 census).
        row = {
            "kind": "mission",
            "live": "True",
            "giraffe": "ready",
            "gated": "0",
            "close_cond": "no-section",
            "gate_line": "0",
            "prose_only": "1",
            "hdr_date": "",
            "hdr_status": "OPEN — IDENTIFY — evidence collected…",
            "timebox_in_file": "0",
        }
        it = item(id="M-arxana-hardening", kind="mission",
                  readiness={"status": "ready", "reason": ""})
        labels = gpe.item_labels(it, row)
        self.assertIn("prose-only-wait", labels)
        # missions close via checkboxes: no close-unstated
        self.assertNotIn("close-unstated", labels)
        self.assertIn("giraffe-ready", labels)


class ItemPayloadTest(unittest.TestCase):
    def test_payload_fields(self):
        p = gpe.item_payload(item(), {"giraffe-ready", "timebox-absent"})
        self.assertEqual(p["name"], "T-foo")
        self.assertEqual(p["external_id"], "T-foo")
        self.assertEqual(p["external_source"], "giraffe")
        self.assertEqual(p["state_name"], "Ready")
        self.assertEqual(p["label_names"], ["giraffe-ready", "timebox-absent"])
        self.assertIn("OPEN — &lt;waiting&gt; on review", p["description_html"])
        self.assertIn("&lt;", p["description_html"])  # status line is HTML-escaped
        self.assertIn("order/ready-by-unblocks", p["description_html"])

    def test_not_ready_state(self):
        p = gpe.item_payload(
            item(readiness={"status": "undetermined", "reason": ""}), set()
        )
        self.assertEqual(p["state_name"], "Not ready")

    def test_title_appended_once(self):
        gpe.item_title = lambda path: "T-foo — do the thing"
        p = gpe.item_payload(item(), set())
        self.assertEqual(p["name"], "T-foo — do the thing")


if __name__ == "__main__":
    unittest.main()
