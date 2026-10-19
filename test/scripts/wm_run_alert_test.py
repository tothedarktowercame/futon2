import subprocess, tempfile, threading, unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/"scripts/wm_run_alert.py"
# Worktrees deliberately omit gitignored run artifacts; the canonical checkout
# retains this recorded fixture used by the pre-existing alert regression.
CLICK20=next((path for path in (ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn",
                                Path("/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-30-1790748841.edn"))
              if path.is_file()), ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn")

class AlertTest(unittest.TestCase):
  def invoke(self, extra=(), record=CLICK20, no_evidence=True):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); d=Path(td.name)
    rec=d/"run.edn"; rec.write_bytes(Path(record).read_bytes())
    cmd=[str(SCRIPT),"--run-record",str(rec),"--alerts-log",str(d/"alerts.log"),*extra]
    if no_evidence: cmd.append("--no-evidence")
    return subprocess.run(cmd,cwd=ROOT,text=True,capture_output=True),rec

  def publisher(self, status):
    class Handler(BaseHTTPRequestHandler):
      def do_POST(self):
        self.send_response(status); self.end_headers()
      def log_message(self, *_): pass
    server=ThreadingHTTPServer(("127.0.0.1",0),Handler)
    thread=threading.Thread(target=server.serve_forever,daemon=True); thread.start()
    self.addCleanup(server.server_close); self.addCleanup(server.shutdown)
    return f"http://127.0.0.1:{server.server_port}/evidence"

  def test_click20_alerts_and_exits_nonzero(self):
    p,rec=self.invoke(); self.assertNotEqual(p.returncode,0); self.assertTrue(p.stdout.startswith("ALERT FOR JOE\n"))
    for q in (1,2,4,5,6,7,8,9,10): self.assertRegex(p.stdout,rf"(?m)^Q{q} (violated|unverifiable):")
    self.assertNotRegex(p.stdout,r"(?m)^Q3 "); self.assertTrue(Path(str(rec)+".requirements.edn").is_file())

  def test_checker_failure_is_an_alert(self):
    p,_=self.invoke(["--mathlib-root","/definitely/missing"]); self.assertNotEqual(p.returncode,0); self.assertIn("requirements NOT CHECKED",p.stdout)

  def test_http_403_preserves_computed_verdict_and_records_publication_failure(self):
    # Saved 2026-10-19 click logs show this exact transport error after Q output.
    p,rec=self.invoke(["--evidence-url",self.publisher(403)],no_evidence=False)
    self.assertEqual(3,p.returncode); self.assertIn("ALERT FOR JOE",p.stdout)
    self.assertRegex(p.stdout,r"(?m)^Q1 (violated|unverifiable):")
    self.assertIn("EVIDENCE PUBLICATION FAILED: HTTP Error 403: Forbidden",p.stdout)
    self.assertNotIn("requirements NOT CHECKED",p.stdout)
    persisted=Path(str(rec)+".requirements.edn").read_text()
    self.assertIn('":evidence-publication"',persisted)
    self.assertIn('":evidence-http-status"',persisted)
    self.assertIn("403",persisted)

  def test_successful_publication_keeps_requirements_exit_status(self):
    p,rec=self.invoke(["--evidence-url",self.publisher(201)],no_evidence=False)
    self.assertEqual(1,p.returncode)
    self.assertNotIn("EVIDENCE PUBLICATION FAILED",p.stdout)
    self.assertIn('":published"',Path(str(rec)+".requirements.edn").read_text())

  def test_click_script_requires_checker_and_verdict(self):
    text=(ROOT/"scripts/wm_click.sh").read_text()
    self.assertIn('python3 "$F2/scripts/wm_run_alert.py"',text)
    self.assertIn('if [ ! -f "$verdict_file" ]',text)
    self.assertIn('exit "$requirements_status"',text)

if __name__ == "__main__": unittest.main()
