import importlib.util
import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def load_waiter():
    path = ROOT / "scripts/wm_click_wait_child.py"
    spec = importlib.util.spec_from_file_location("wm_click_wait_child", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ScriptedStatus:
    def __init__(self, responses):
        self.responses = list(responses)
        self.requests = 0

    def response(self):
        index = min(self.requests, len(self.responses) - 1)
        self.requests += 1
        return self.responses[index]


class StatusServer:
    def __init__(self, responses):
        self.script = ScriptedStatus(responses)
        script = self.script

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                status, body = script.response()
                encoded = json.dumps(body).encode()
                self.send_response(status)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(encoded)))
                self.end_headers()
                self.wfile.write(encoded)

            def log_message(self, *_args):
                pass

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    def __enter__(self):
        self.thread.start()
        return f"http://127.0.0.1:{self.server.server_port}", self.script

    def __exit__(self, *_args):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()


def child(status):
    return {"click-id": "parent", "status": "completed",
            "child": {"click-id": "retry-child",
                      "completion": {"status": status}}}


class WaitChildTest(unittest.TestCase):
    def run_waiter(self, responses, *extra):
        waiter = load_waiter()
        with StatusServer(responses) as (base, script):
            result = waiter.run(["--base", base, "--click-id", "parent",
                                 "--poll-s", "0", *extra])
        return result, script.requests

    def test_no_child_returns_after_one_get(self):
        result, requests = self.run_waiter([(200, {"click-id": "parent",
                                                   "status": "completed"})])
        self.assertEqual(0, result)
        self.assertEqual(1, requests)

    def test_pending_then_complete(self):
        result, requests = self.run_waiter(
            [(200, child("pending")), (200, child("pending")),
             (200, child("complete"))])
        self.assertEqual(0, result)
        self.assertEqual(3, requests)

    def test_zero_cap_refuses_pending_child(self):
        result, requests = self.run_waiter([(200, child("pending"))],
                                           "--max-s", "0")
        self.assertEqual(1, result)
        self.assertEqual(1, requests)

    def test_two_route_failures_refuse_but_one_recovers(self):
        result, requests = self.run_waiter([(500, {}), (500, {})])
        self.assertEqual(1, result)
        self.assertEqual(2, requests)
        result, requests = self.run_waiter([(500, {}), (200, child("complete"))])
        self.assertEqual(0, result)
        self.assertEqual(2, requests)

    def test_shell_call_precedes_run_record_check(self):
        shell = (ROOT / "scripts/wm_click.sh").read_text()
        self.assertLess(shell.index("wm_click_wait_child.py"),
                        shell.index('run_record="$F2/data/wm-runs/'))


if __name__ == "__main__":
    unittest.main()
