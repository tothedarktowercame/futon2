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
CLICK = "wm-click-timeout-fixture"


class ClickServer:
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
                              for seat in ["wm-author", "wm-reviewer", "wm-repair-reviewer", "zai-2", "zai-3", "zai-8",
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
                if owner.mode == "rejected":
                    self.reply(200, {"rejected": "already-running",
                                     "click-id": "other-click"})
                    return
                run_id = payload["run-id"]
                owner.run_id = run_id
                owner.posted.set()
                if owner.mode == "partial":
                    # Headers and half the body leave before the limit; curl
                    # still exits 28, now with a non-empty, unparseable body.
                    data = json.dumps({"click-id": CLICK, "status": "queued"}).encode()
                    self.send_response(200)
                    self.send_header("Content-Type", "application/json")
                    self.send_header("Content-Length", str(len(data)))
                    self.end_headers()
                    try:
                        self.wfile.write(data[:8])
                        self.wfile.flush()
                        time.sleep(0.35)
                        self.wfile.write(data[8:])
                    except BrokenPipeError:
                        pass
                    return
                time.sleep(0.35)
                self.reply(200, {"click-id": CLICK, "status": "queued"})

            def log_message(self, *_args):
                pass

        self.httpd = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)

    def write_evidence(self):
        record_click = "foreign-click" if self.mode == "mismatch" else CLICK
        record_run = "foreign-run" if self.mode == "mismatch" else self.run_id
        record = self.f2 / "data/wm-runs" / f"tick-run-record-{self.run_id}.edn"
        record.parent.mkdir(parents=True, exist_ok=True)
        record.write_text(f'{{:run/id "{record_run}" :click/id "{record_click}"}}\n')
        safe = CLICK.replace("/", "_")
        binding = self.f3c / "data/wm-click-run-bindings" / f"click-run-binding-{safe}.edn"
        binding.parent.mkdir(parents=True, exist_ok=True)
        # Shape of a real futon3c click-run binding's observation (projected
        # to the keys the script reads): {:status :present, :source
        # :runner-return, :value "<run-id>"}.
        binding.write_text(
            f'{{:click/id "{CLICK}", :run-id-observation '
            f'{{:status :present, :source :runner-return, :value "{self.run_id}"}}}}\n')

    def __enter__(self):
        self.thread.start()
        return f"http://127.0.0.1:{self.httpd.server_port}"

    def __exit__(self, *_args):
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join()


class PostTimeoutTest(unittest.TestCase):
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
                         "WM_CLICK_POST_TIMEOUT_S": "0.1",
                         "GIRAFFE_MINT": "0",  # fixture F3C: the staleness gate has its own test (wm_click_giraffe_mint_test.py)
                         "WM_CLICK_POST_RECOVERY_MAX_S": "5",
                         "WM_CLICK_POST_RECOVERY_POLL_S": "0"},
                    capture_output=True, text=True, timeout=20)
            return result, events.read_text().splitlines()

    def test_timed_out_post_recovers_owned_click_and_runs_requirements(self):
        result, events = self.run_case("owned")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("admission answer not received", result.stdout)
        self.assertIn(f"recovered admitted click: {CLICK}", result.stdout)
        self.assertIn("click ownership confirmed by run-record", result.stdout)
        self.assertNotIn("no click-id returned; not accepted", result.stdout)
        self.assertEqual(["alert"], events)

    def test_partial_answer_past_limit_recovers_owned_click(self):
        result, events = self.run_case("partial")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("admission answer not received (curl exit 28", result.stdout)
        self.assertIn(f"recovered admitted click: {CLICK}", result.stdout)
        self.assertEqual(["alert"], events)

    def test_mismatched_record_refuses_without_requirements_verdict(self):
        result, events = self.run_case("mismatch")
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertIn("ALERT FOR JOE", result.stdout)
        self.assertIn("run record ownership mismatch", result.stdout)
        self.assertIn("foreign-run", result.stdout)
        self.assertIn("foreign-click", result.stdout)
        self.assertEqual([], events)

    def test_explicit_rejection_remains_exit_three(self):
        result, events = self.run_case("rejected")
        self.assertEqual(3, result.returncode, result.stdout + result.stderr)
        self.assertIn("already-running", result.stdout)
        self.assertEqual([], events)


if __name__ == "__main__":
    unittest.main()
