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

    def test_timebox_absent_is_not_exported(self):
        self.assertNotIn("timebox-absent", gpe.item_labels(item(), {"timebox_in_file": "0"}))

    def test_kind_and_each_way_are_labels(self):
        labels = gpe.item_labels(
            item(way="records-carry-warrant, eat-your-own-tail"), None)
        self.assertIn("ticket", labels)
        self.assertIn("way: records-carry-warrant", labels)
        self.assertIn("way: eat-your-own-tail", labels)

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


class SourceAndOrderTest(unittest.TestCase):
    def test_worktree_paths_are_not_primary_checkouts(self):
        import tempfile, os
        root = tempfile.mkdtemp()
        os.makedirs(os.path.join(root, "futon2", ".git"))
        os.makedirs(os.path.join(root, "futon2-retraction-p1"))
        with open(os.path.join(root, "futon2-retraction-p1", ".git"), "w") as f:
            f.write("gitdir: /elsewhere\n")
        self.assertTrue(gpe.primary_checkout(
            os.path.join(root, "futon2", "holes", "tickets", "T-a.md"), root))
        # The 2026-11-07 case: stale T-repair-occ copies in a worktree.
        self.assertFalse(gpe.primary_checkout(
            os.path.join(root, "futon2-retraction-p1", "holes", "tickets", "T-repair-occ-x.md"), root))
        self.assertFalse(gpe.primary_checkout("/elsewhere/T-a.md", root))

    def test_edn_number_strings(self):
        self.assertEqual(gpe._int("12,"), 12)
        self.assertEqual(gpe._int(3), 3)
        self.assertIsNone(gpe._int(None))

    def test_rank_prefixes_name_and_sets_sort_order(self):
        it = item(readiness={"status": "ready", "reason": "x", "rank": 7,
                             "unblocks": 2, "cone": 5, "order_reason": "order/ready-by-unblocks"})
        p = gpe.item_payload(it, set())
        self.assertTrue(p["name"].startswith("#7 T-foo"))
        self.assertEqual(p["sort_order"], 7000.0)
        self.assertIn("rank 7", p["description_html"])

    def test_unranked_item_sorts_last_without_prefix(self):
        p = gpe.item_payload(item(), set())
        self.assertFalse(p["name"].startswith("#"))
        self.assertEqual(p["sort_order"], 1e9)


class TrunkTest(unittest.TestCase):
    def test_trunk_sheet_routes_items_and_unknown_is_not_on_a_trunk(self):
        import tempfile
        d = tempfile.mkdtemp()
        f = os.path.join(d, "trunks.tsv")
        with open(f, "w") as fh:
            fh.write("id\ttrunk\tharm_line\tbasis\n"
                     "T-a\tP\t\tplaced in P\n"
                     "E-b\tharm\tH5\tdisbenefits H5\n"
                     "M-c\tbogus\t\t?\n")
        tr = gpe.load_trunks(f)
        self.assertEqual("P", gpe.trunk_key(tr["T-a"]))
        self.assertEqual("H5", tr["E-b"]["harm_line"])
        self.assertEqual("notrunk", gpe.trunk_key(tr["M-c"]))
        self.assertEqual("notrunk", gpe.trunk_key(None))
        self.assertEqual({}, gpe.load_trunks(os.path.join(d, "absent.tsv")))

    def test_verdict_sheet_gives_one_verdict_label_and_paused_flag(self):
        import tempfile
        d = tempfile.mkdtemp()
        f = os.path.join(d, "v.tsv")
        with open(f, "w") as fh:
            fh.write("id\tverdict\treason\twm_paused\n"
                     "T-a\tFIX\t\tno\n"
                     "E-b\tWONTFIX\tmulti-day\tyes\n"
                     "M-c\tWONTFIX\t\t?\n")
        v = gpe.load_verdicts(f)
        self.assertEqual(["fix"], v["T-a"])
        self.assertEqual(["wontfix: multi-day", "wm-paused"], v["E-b"])
        self.assertEqual(["wontfix: unstated"], v["M-c"])
        self.assertEqual({}, gpe.load_verdicts(os.path.join(d, "absent.tsv")))

    def test_wings_become_labels_and_their_projects_are_retired(self):
        self.assertEqual("wing A: Records carry warrant",
                         gpe.wing_label({"wing": "A"}))
        self.assertEqual("wing: unplaced", gpe.wing_label(None))
        self.assertEqual("wing: none", gpe.wing_label({"wing": "none"}))
        for name, _ in gpe.WINGS.values():
            self.assertIn(name, gpe.RETIRED_PROJECTS)
        for name, _ in gpe.TRUNKS.values():
            self.assertNotIn(name, gpe.RETIRED_PROJECTS)
        self.assertNotIn(gpe.REPAIRS[0], gpe.RETIRED_PROJECTS)


class WingTest(unittest.TestCase):
    def test_wing_d_is_named_and_unknown_is_unplaced(self):
        self.assertEqual("D", gpe.wing_key({"wing": "D"}))
        self.assertIn("next action", gpe.WINGS["D"][0])

    def test_project_names_pass_planes_name_rule(self):
        import re
        for name, ident in list(gpe.TRUNKS.values()) + [gpe.REPAIRS]:
            self.assertIsNone(re.match(gpe.PLANE_FORBIDDEN_NAME, name), name)
            self.assertLessEqual(len(ident), 12)
        self.assertEqual("unplaced", gpe.wing_key({"wing": "unsure"}))
        self.assertEqual("unplaced", gpe.wing_key(None))

    def test_a1_reading_overrides_early_sheet(self):
        import tempfile
        d = tempfile.mkdtemp()
        early = os.path.join(d, "placements-1.tsv")
        a1 = os.path.join(d, "A1.tsv")
        with open(early, "w") as f:
            f.write("id\twing\nM-x\tB\nM-y\tC\n")
        with open(a1, "w") as f:
            f.write("id\twing\tway\nM-x\tD\tselection-gives-a-reason\n")
        got = gpe.load_placements(a1, os.path.join(d, "placements-[12]*.tsv"))
        self.assertEqual({"wing": "D", "way": "selection-gives-a-reason"}, got["M-x"])
        self.assertEqual("C", got["M-y"]["wing"])

    def test_repair_records_are_open_or_done(self):
        import tempfile
        d = tempfile.mkdtemp()
        for n, st in (("T-repair-a", "OPEN"), ("T-repair-b", "DONE")):
            with open(os.path.join(d, n + ".md"), "w") as f:
                f.write("# %s\n\n**Status:** %s\n" % (n, st))
        got = {i["id"]: gpe.item_state_name(i) for i in gpe.load_repairs(d)}
        self.assertEqual({"T-repair-a": "Open", "T-repair-b": "Done"}, got)


if __name__ == "__main__":
    unittest.main()
