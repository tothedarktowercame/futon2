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
CLICK = "wm-click-giraffe-mint-fixture"


class ClickServer:
    """Fixture Agency mirroring wm_click_medic_only_test.py's harness.

    GET /api/alpha/agents lists the cast seats idle; GET
    /api/alpha/wm/click reports the budget and, in mode "running",
    flips to running?: true after the first poll so the mint-time idle
    guard sees a live click; POST records that the click was fired.
    """

    def __init__(self, mode, f2, f3c):
        self.mode = mode
        self.f2 = f2
        self.f3c = f3c
        self.posted = threading.Event()
        self.gets = 0
        self.status_after_post = 0
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
                        owner.gets += 1
                        # mode "running": exactly the second poll -- the
                        # gate's mint-time idle check -- reports a click in
                        # flight; every other poll is idle so the launcher
                        # proceeds on the stale field.
                        running = owner.mode == "running" and owner.gets == 2
                        self.reply(200, {"running?": running,
                                         "ordinary-click-budget":
                                             {"available": 1}})
                    else:
                        owner.status_after_post += 1
                        running = owner.status_after_post == 1
                        if not running:
                            owner.write_evidence()
                        self.reply(200, {"running?": running, "click-id": CLICK,
                                         "phase": "fixture" if running else None,
                                         "ordinary-click-budget":
                                             {"available": 1}})
                    return
                self.reply(404, {})

            def do_POST(self):
                length = int(self.headers.get("Content-Length", "0"))
                payload = json.loads(self.rfile.read(length))
                owner.run_id = payload["run-id"]
                owner.posted.set()
                time.sleep(0.05)
                self.reply(200, {"click-id": CLICK, "status": "queued"})

            def log_message(self, *_args):
                pass

        self.httpd = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)

    def write_evidence(self):
        # A medic-only binding, as in the medic harness, so the post-POST
        # ownership path completes without an ordinary run record.
        safe = CLICK.replace("/", "_")
        binding = self.f3c / "data/wm-click-run-bindings" / f"click-run-binding-{safe}.edn"
        binding.parent.mkdir(parents=True, exist_ok=True)
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

    def __enter__(self):
        self.thread.start()
        return f"http://127.0.0.1:{self.httpd.server_port}"

    def __exit__(self, *_args):
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join()


class GiraffeMintTest(unittest.TestCase):
    """The staleness-gated Giraffe mint in wm_click.sh prepare_sources.

    clojure is stubbed to the probe/mint protocol of
    futon3c.giraffe.run --check-fresh; GIRAFFE_ROOT points at a temp
    root; the real shell entry scripts/wm_click.sh --run is executed.
    """

    def run_case(self, mode, clojure_state, mint_fail=False, dirty=False,
                 mint_stays_stale=False):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            f2, f3c = home / "futon2", home / "futon3c"
            bin_dir = home / "bin"
            bin_dir.mkdir(parents=True)
            temp_dir = home / "tmp"
            temp_dir.mkdir()
            events = home / "events"
            events.touch()
            clojure_log = home / "clojure.log"
            state_file = home / "clojure-state"
            state_file.write_text(clojure_state)
            root = home / "giraffe-root"
            root.mkdir()
            edges = home / "edges.json"
            edges.write_text("[]")
            readings = home / "readings"
            readings.mkdir()

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
    open(record + ".requirements.edn", "w").write("{{:conforms true}}\\\\n")
    open(os.environ["EVENTS"], "a").write("alert\\\\n")
    sys.exit(0)
if name == "wm_click_record_child_verdict.py": sys.exit(0)
os.execv({sys.executable!r}, [{sys.executable!r}] + sys.argv[1:])
''')
            executable(bin_dir / "sleep", "#!/bin/sh\nexit 0\n")
            executable(bin_dir / "clojure", f'''#!{sys.executable}
import os, sys
log = os.environ["CLOJURE_LOG"]
args = sys.argv[1:]
with open(log, "a") as fh:
    fh.write(("PROBE" if "--check-fresh" in args else "MINT") + "\\n")
if "--check-fresh" in args:
    state = open(os.environ["CLOJURE_STATE"]).read().strip()
    if state == "probe-fail":
        sys.stderr.write("boom\\n")
        sys.exit(1)
    fresh = state in ("fresh", "minted")
    newest = "run-new" if state == "minted" else "run-old"
    print("giraffe-check-fresh")
    print("adapter-input-sha256: aa")
    print("newest-verified-run-id: " + newest)
    print("newest-verified-input-sha256: " + ("aa" if fresh else "bb"))
    print("fresh: " + ("true" if fresh else "false"))
    sys.exit(0)
state = open(os.environ["CLOJURE_STATE"]).read().strip()
if os.environ.get("MINT_FAIL"):
    sys.stderr.write("mint exploded\\n")
    sys.exit(1)
if state == "stale":
    open(os.environ["CLOJURE_STATE"], "w").write(
        "mint-bad" if os.environ.get("MINT_STAYS_STALE") else "minted")
print("run dir: .../run-new")
print("run id: run-new")
sys.exit(0)
''')
            executable(f3c / "scripts/proof-eval.sh",
                       '#!/bin/sh\necho "{:tripping [] :will-defer [] :stop-lines-queued 0}"\n')

            # A real git repo in f3c so the mint-time dirty check is
            # meaningful (and can be deliberately dirtied).
            subprocess.run(["git", "init", "-q", str(f3c)], check=True)
            subprocess.run(["git", "-C", str(f3c), "add", "-A"], check=True)
            subprocess.run(["git", "-C", str(f3c), "-c", "user.email=t@t",
                            "-c", "user.name=t", "commit", "-qm", "x"],
                           check=True, capture_output=True)
            if dirty:
                (f3c / "scripts/proof-eval.sh").write_text("dirty\n")

            server = ClickServer(mode, f2, f3c)
            with server as base:
                result = subprocess.run(
                    ["bash", str(ROOT / "scripts/wm_click.sh"), "--run"],
                    env={**os.environ, "HOME": str(home), "F2": str(f2),
                         "F3C": str(f3c), "BASE": base, "EVENTS": str(events),
                         "TMPDIR": str(temp_dir),
                         "PATH": str(bin_dir) + ":" + os.environ["PATH"],
                         "WM_CLICK_POST_TIMEOUT_S": "5",
                         "WM_CLICK_POST_RECOVERY_MAX_S": "5",
                         "WM_CLICK_POST_RECOVERY_POLL_S": "0",
                         "GIRAFFE_ROOT": str(root),
                         "GIRAFFE_EDGES": str(edges),
                         "GIRAFFE_READINGS_ROOT": str(readings),
                         "CLOJURE_LOG": str(clojure_log),
                         "CLOJURE_STATE": str(state_file),
                         "MINT_FAIL": "1" if mint_fail else "",
                         "MINT_STAYS_STALE": "1" if mint_stays_stale else ""},
                    capture_output=True, text=True, timeout=60)
            calls = clojure_log.read_text().splitlines()
            posted = ["post"] if server.posted.is_set() else []
            return result, calls, posted

    def test_fresh_field_mints_nothing_and_fires(self):
        result, calls, posted = self.run_case("medic", "fresh")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(["PROBE", "PROBE"], calls)
        self.assertIn("giraffe field", result.stdout)
        self.assertIn("fresh (run-old)", result.stdout)
        self.assertIn("giraffe field id       run-old", result.stdout)
        self.assertEqual(["post"], posted)

    def test_stale_field_mints_one_run_then_fires(self):
        result, calls, posted = self.run_case("medic", "stale")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(["PROBE", "MINT", "PROBE", "PROBE"], calls)
        self.assertIn("minted run-new", result.stdout)
        self.assertIn("giraffe field id       run-new", result.stdout)
        self.assertEqual(["post"], posted)

    def test_failing_mint_is_a_finding_and_the_click_fires_on_the_stale_field(self):
        result, calls, posted = self.run_case("medic", "stale", mint_fail=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        # both gate passes probe, attempt the mint, and fail: two mints are
        # ATTEMPTED but neither writes a run (the stub fails before writing
        # and the field id stays the pre-mint one)
        self.assertEqual(["PROBE", "MINT", "PROBE", "MINT"], calls)
        self.assertIn("FINDING", result.stdout)
        self.assertIn("mint failed", result.stdout)
        self.assertIn("firing on the newest verified field run-old", result.stdout)
        self.assertIn("giraffe field id       run-old", result.stdout)
        self.assertEqual(["post"], posted)
        # no second run: the mint stub failed before any write and the
        # field id the launcher recorded is still the pre-mint one

    def test_failing_probe_is_a_finding_and_the_click_fires(self):
        result, calls, posted = self.run_case("medic", "probe-fail")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(["PROBE", "PROBE"], calls)
        self.assertIn("FINDING", result.stdout)
        self.assertIn("freshness probe failed", result.stdout)
        self.assertIn("giraffe field id       (unknown -- probe failed)", result.stdout)
        self.assertEqual(["post"], posted)

    def test_mint_refused_while_a_click_is_running_is_a_finding_and_the_click_fires(self):
        result, calls, posted = self.run_case("running", "stale")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        # pass 1: probe, finding, no mint.  pass 2 (immediately before
        # POST): the other click has finished, so the gate mints
        self.assertEqual(["PROBE", "PROBE", "MINT", "PROBE"], calls)
        self.assertIn("FINDING", result.stdout)
        self.assertIn("a click is running", result.stdout)
        self.assertIn("firing on it", result.stdout)
        self.assertIn("giraffe field id       run-old", result.stdout)
        self.assertEqual(["post"], posted)

    def test_mint_refused_from_a_dirty_checkout_is_a_finding_and_the_click_fires(self):
        result, calls, posted = self.run_case("medic", "stale", dirty=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(["PROBE", "PROBE"], calls)
        self.assertIn("FINDING", result.stdout)
        self.assertIn("futon3c checkout is dirty", result.stdout)
        self.assertIn("firing on it", result.stdout)
        self.assertIn("giraffe field id       run-old", result.stdout)
        self.assertEqual(["post"], posted)

    def test_minted_run_not_verified_is_a_finding_and_the_click_fires_on_the_old_field(self):
        result, calls, posted = self.run_case("medic", "stale", mint_stays_stale=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        # both passes attempt the mint; neither leaves a verified newer
        # field run, so both record the pre-mint field id
        self.assertEqual(["PROBE", "MINT", "PROBE", "PROBE", "MINT", "PROBE"], calls)
        self.assertIn("FINDING", result.stdout)
        self.assertIn("did not verify as the newest field run", result.stdout)
        self.assertIn("giraffe field id       run-old", result.stdout)
        self.assertEqual(["post"], posted)


if __name__ == "__main__":
    unittest.main()
