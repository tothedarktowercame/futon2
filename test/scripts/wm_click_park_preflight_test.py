#!/usr/bin/env python3
"""Regression: a WM click must refuse before budget/readiness when parks are off."""

import http.server
import os
import pathlib
import subprocess
import threading
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[2] / "scripts" / "wm_click.sh"


class DisabledParkingAgency(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        if self.path == "/api/alpha/agents":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"ok":true,"agents":[]}')
        else:
            self.send_response(500)
            self.end_headers()

    def do_POST(self):
        if self.path == "/api/alpha/park":
            self.send_response(503)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"ok":false,"error":"parked-on-disabled"}')
        else:
            self.send_response(500)
            self.end_headers()


class ParkPreflightTest(unittest.TestCase):
    def test_disabled_parking_refuses_before_click_budget(self):
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), DisabledParkingAgency)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            env = os.environ.copy()
            env["BASE"] = f"http://127.0.0.1:{server.server_port}"
            result = subprocess.run(
                ["bash", str(SCRIPT)], env=env, text=True,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=15,
            )
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)

        self.assertEqual(3, result.returncode, result.stdout)
        self.assertIn("parking", result.stdout)
        self.assertIn("CANNOT LAUNCH", result.stdout)
        self.assertIn("HTTP 503", result.stdout)
        self.assertNotIn("click budget", result.stdout)


if __name__ == "__main__":
    unittest.main()
