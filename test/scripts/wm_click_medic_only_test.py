import http.server
import json
import os
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CLICK = "wm-click-medic-fixture"


class ClickServer:
    """Fixture Agency: one click that POSTs to a medic-only discharge.

    The server answers the POST with a click id, reports the click finished,
    and -- as futon3c's runner_service does for a medic-only route -- retains
    a click-run-binding whose :run-id-observation is absent and whose
    :runtime-disposition records the discharge.  No run record is ever
    written: an ordinary run was never attempted.
    """

    def __init__(self, mode, f2, f3c):
        self.mode = mode
        self.f2 = f2
        self.f3c = f3c
        self.posted = threading.Event()
        self.status_after_post = 0
        self.run_id = None
        owner = self

        class Handler(http.server.BaseHTTPRequestHandler):
            def reply(self, status, body):
                data = json.dumps(body).encode()
                self.send_response(status)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                try:
                    self.wfile.write(data)
                except BrokenPipeError:
                    pass

            def do_GET(self):
                if self.path.startswith("/api/alpha/agents"):
                    agents = {seat: {"status": "idle", "invoke-ready?": True}
                              for seat in ["wm-author", "wm-reviewer",
                                           "wm-repair-reviewer",
                                           "zai-2", "zai-3", "zai-8",
                                           "zai-4", "zai-5", "zai-6"]}
                    self.reply(200, {"agents": agents})
                    return
                if self.path.startswith("/api/alpha/wm/click"):
                    if not owner.posted.is_set():
                        self.reply(200, {"running?": False,
                                         "ordinary-click-budget": {"available": 1}})
                    else:
                        owner.status_after_post += 1
                        running = owner.status_after_post == 1
                        if not running:
                            owner.write_evidence()
                        self.reply(200, {"running?": running, "click-id": CLICK,
                                         "phase": "fixture" if running else None,
                                         "ordinary-click-budget": {"available": 1}})
                    return
                self.reply(404, {})

            def do_POST(self):
                length = int(self.headers.get("Content-Length", "0"))
                payload = json.loads(self.rfile.read(length))
                run_id = payload["run-id"]
                owner.run_id = run_id
                owner.posted.set()
                time.sleep(0.05)
                self.reply(200, {"click-id": CLICK, "status": "queued"})

            def log_message(self, *_args):
                pass

        self.httpd = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)

    def write_evidence(self):
        # Shape copied from the real click-run-binding of click 45
        # (wm-click-8a818ece-..., 2026-10-29): binding-status :absent,
        # run-id-observation absent, and the medic runtime-disposition the
        # server retained as the click's one durable record.
        safe = CLICK.replace("/", "_")
        binding = self.f3c / "data/wm-click-run-bindings" / f"click-run-binding-{safe}.edn"
        binding.parent.mkdir(parents=True, exist_ok=True)
        if self.mode == "medic":
            binding.write_text(
                '{:schema :wm-click-run-binding-v1, :attempt/id nil, '
                f':click/id "{CLICK}", :outcome :unknown, '
                ':binding-status :absent, '
                ':runtime-disposition {:status :discharged, '
                ':route :open-out-of-band-medic, '
                ':ordinary-selection-called? false, '
                ':injury-id "injury/fixture"}, '
                ':run-record-absence :runner-did-not-return-run-record, '
                ':run-id-observation {:status :absent, '
                ':reason :runner-did-not-return-run-id}, '
                ':run-record-status :absent}\n')
        elif self.mode == "medic-held":
            # A held disposition on the same route is NOT a discharge: the
            # launcher must still alert, not report a medic-only success.
            binding.write_text(
                '{:schema :wm-click-run-binding-v1, '
                f':click/id "{CLICK}", :binding-status :absent, '
                ':runtime-disposition {:status :held, '
                ':reason :out-of-band/acceptance-run-unavailable, '
                ':phase :open-out-of-band, '
                ':ordinary-selection-called? false}, '
                ':run-id-observation {:status :absent, '
                ':reason :runner-did-not-return-run-id}}\n')
        elif self.mode == "medic-foreign-click":
            binding.write_text(
                '{:schema :wm-click-run-binding-v1, '
                ':click/id "foreign-click", :binding-status :absent, '
                ':runtime-disposition {:status :discharged, '
                ':route :open-out-of-band-medic, '
                ':ordinary-selection-called? false}, '
                ':run-id-observation {:status :absent, '
                ':reason :runner-did-not-return-run-id}}\n')

    def __enter__(self):
        self.thread.start()
        return f"http://127.0.0.1:{self.httpd.server_port}"

    def __exit__(self, *_args):
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join()


class MedicOnlyReportingTest(unittest.TestCase):
    def run_case(self, mode):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            f2, f3c = home / "futon2", home / "futon3c"
            bin_dir = home / "bin"
            bin_dir.mkdir(parents=True)
            temp_dir = home / "tmp"
            temp_dir.mkdir()
            events = home / "events"
            events.touch()

            def executable(path, body):
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(body)
                path.chmod(0o755)

            executable(bin_dir / "python3", f'''#!{sys.executable}
import os,sys
name = os.path.basename(sys.argv[1]) if len(sys.argv) > 1 else ""
if name == "wm_click_prepare.py": sys.exit(0)
if name == "wm_click_wait_child.py": sys.exit(0)
if name == "wm_run_alert.py":
    record = sys.argv[sys.argv.index("--run-record") + 1]
    open(record + ".requirements.edn", "w").write("{{:conforms true}}\\n")
    open(os.environ["EVENTS"], "a").write("alert\\n")
    sys.exit(0)
if name == "wm_click_record_child_verdict.py": sys.exit(0)
os.execv({sys.executable!r}, [{sys.executable!r}] + sys.argv[1:])
''')
            executable(bin_dir / "sleep", "#!/bin/sh\nexit 0\n")
            executable(f3c / "scripts/proof-eval.sh",
                       '#!/bin/sh\necho "{:tripping [] :will-defer [] :stop-lines-queued 0}"\n')

            with ClickServer(mode, f2, f3c) as base:
                result = subprocess.run(
                    ["bash", str(ROOT / "scripts/wm_click.sh"), "--run"],
                    env={**os.environ, "HOME": str(home), "F2": str(f2),
                         "F3C": str(f3c), "BASE": base, "EVENTS": str(events),
                         "TMPDIR": str(temp_dir),
                         "PATH": str(bin_dir) + ":" + os.environ["PATH"],
                         "WM_CLICK_POST_TIMEOUT_S": "5",
                         "GIRAFFE_MINT": "0",  # fixture F3C: the staleness gate has its own test (wm_click_giraffe_mint_test.py)
                         "WM_CLICK_POST_RECOVERY_MAX_S": "5",
                         "WM_CLICK_POST_RECOVERY_POLL_S": "0"},
                    capture_output=True, text=True, timeout=30)
            return result, events.read_text().splitlines()

    def test_medic_only_discharge_reports_success_without_ordinary_run(self):
        result, events = self.run_case("medic")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("medic-only discharge: no ordinary run was attempted",
                      result.stdout)
        self.assertIn("requirements not applicable", result.stdout)
        # No ordinary run record was invented: the requirements checker was
        # never invoked, and no ALERT for Joe is raised for a real discharge.
        self.assertNotIn("ALERT FOR JOE", result.stdout)
        self.assertNotIn("click binding ownership mismatch", result.stdout)
        self.assertEqual([], events)

    def test_held_disposition_still_alerts(self):
        result, events = self.run_case("medic-held")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("ALERT FOR JOE", result.stdout)
        self.assertIn("click binding ownership mismatch", result.stdout)
        self.assertEqual([], events)

    def test_foreign_click_medic_binding_still_alerts(self):
        result, events = self.run_case("medic-foreign-click")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("ALERT FOR JOE", result.stdout)
        self.assertIn("foreign-click", result.stdout)
        self.assertEqual([], events)


if __name__ == "__main__":
    unittest.main()
