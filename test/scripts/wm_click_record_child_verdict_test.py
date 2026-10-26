import importlib.util
import io
import json
import threading
import unittest
from contextlib import redirect_stderr, redirect_stdout
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def load_recorder():
    path = ROOT / "scripts/wm_click_record_child_verdict.py"
    spec = importlib.util.spec_from_file_location(
        "wm_click_record_child_verdict", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ClickServer:
    """GET /api/alpha/wm/click answers `status`; POST .../child-verdict
    answers `post` and records each POST body."""

    def __init__(self, status, post=(200, {})):
        self.posts = []
        self.gets = 0
        server = self

        class Handler(BaseHTTPRequestHandler):
            def reply(self, status_code, body):
                encoded = json.dumps(body).encode()
                self.send_response(status_code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(encoded)))
                self.end_headers()
                self.wfile.write(encoded)

            def do_GET(self):
                server.gets += 1
                self.reply(*status)

            def do_POST(self):
                length = int(self.headers.get("Content-Length", "0"))
                server.posts.append(
                    (self.path, json.loads(self.rfile.read(length))))
                self.reply(*post)

            def log_message(self, *_args):
                pass

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(
            target=self.server.serve_forever, daemon=True)

    def __enter__(self):
        self.thread.start()
        return f"http://127.0.0.1:{self.server.server_port}"

    def __exit__(self, *_args):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()


WITH_CHILD = (200, {"click-id": "parent", "status": "completed",
                    "child": {"click-id": "retry-child",
                              "completion": {"status": "complete"}}})
NO_CHILD = (200, {"click-id": "parent", "status": "completed"})
LANDED = (200, {"click-id": "parent",
                "child-verdict": {"status": "passed"},
                "verdict-refusal": None,
                "parent-verdict": {"status": "passed"}})
REFUSED = (200, {"click-id": "parent",
                 "child-verdict": {"status": "not-checked",
                                   "reason": "requirements-not-run"},
                 "verdict-refusal": {
                     "reason": "verdict-run-record-mismatch",
                     "child-run-record": "/a.edn",
                     "verdict-run-record": "/b.edn"},
                 "parent-verdict": {"status": "not-checked"}})


class RecordChildVerdictTest(unittest.TestCase):
    def run_recorder(self, server):
        recorder = load_recorder()
        output = io.StringIO()
        with server as base:
            with redirect_stdout(output), redirect_stderr(output):
                result = recorder.run(
                    ["--base", base, "--click-id", "parent",
                     "--verdict-file", "/runs/r.edn.requirements.edn"])
        return result, output.getvalue()

    def test_no_child_makes_no_post(self):
        server = ClickServer(NO_CHILD)
        result, output = self.run_recorder(server)
        self.assertEqual(0, result)
        self.assertEqual([], server.posts)
        self.assertIn("no child bound", output)

    def test_bound_child_posts_click_id_and_verdict_file(self):
        server = ClickServer(WITH_CHILD, LANDED)
        result, output = self.run_recorder(server)
        self.assertEqual(0, result)
        self.assertEqual(
            [("/api/alpha/wm/click/child-verdict",
              {"click-id": "parent",
               "verdict-file": "/runs/r.edn.requirements.edn"})],
            server.posts)
        self.assertIn("child verdict recorded on parent: passed", output)

    def test_typed_refusal_is_exit_1_and_named(self):
        server = ClickServer(WITH_CHILD, REFUSED)
        result, output = self.run_recorder(server)
        self.assertEqual(1, result)
        self.assertEqual(1, len(server.posts))
        self.assertIn("ALERT FOR JOE", output)
        self.assertIn("verdict-run-record-mismatch", output)

    def test_route_failures_are_exit_1(self):
        result, output = self.run_recorder(ClickServer((500, {})))
        self.assertEqual(1, result)
        self.assertIn("child status route failed", output)
        result, output = self.run_recorder(ClickServer(WITH_CHILD, (500, {})))
        self.assertEqual(1, result)
        self.assertIn("NOT RECORDED: route failed", output)

    def test_shell_records_after_the_requirements_check(self):
        shell = (ROOT / "scripts/wm_click.sh").read_text()
        record = shell.index("wm_click_record_child_verdict.py")
        self.assertGreater(record, shell.index("wm_run_alert.py"))
        self.assertGreater(record, shell.index("verdict file absent"))
        self.assertLess(record, shell.index('exit "$requirements_status"'))


if __name__ == "__main__":
    unittest.main()
