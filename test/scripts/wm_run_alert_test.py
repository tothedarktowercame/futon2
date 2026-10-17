import subprocess, tempfile, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/"scripts/wm_run_alert.py"
CLICK20=ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn"

class AlertTest(unittest.TestCase):
  def invoke(self, extra=(), record=CLICK20):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); d=Path(td.name)
    rec=d/"run.edn"; rec.write_bytes(Path(record).read_bytes())
    cmd=[str(SCRIPT),"--run-record",str(rec),"--alerts-log",str(d/"alerts.log"),"--no-evidence",*extra]
    return subprocess.run(cmd,cwd=ROOT,text=True,capture_output=True),rec

  def test_click20_alerts_and_exits_nonzero(self):
    p,rec=self.invoke(); self.assertNotEqual(p.returncode,0); self.assertTrue(p.stdout.startswith("ALERT FOR JOE\n"))
    for q in (1,2,4,5,6,7,8,9,10): self.assertRegex(p.stdout,rf"(?m)^Q{q} (violated|unverifiable):")
    self.assertNotRegex(p.stdout,r"(?m)^Q3 "); self.assertTrue(Path(str(rec)+".requirements.edn").is_file())

  def test_checker_failure_is_an_alert(self):
    p,_=self.invoke(["--mathlib-root","/definitely/missing"]); self.assertNotEqual(p.returncode,0); self.assertIn("requirements NOT CHECKED",p.stdout)

  def test_click_script_requires_checker_and_verdict(self):
    text=(ROOT/"scripts/wm_click.sh").read_text()
    self.assertIn('python3 "$F2/scripts/wm_run_alert.py"',text)
    self.assertIn('if [ ! -f "$verdict_file" ]',text)
    self.assertIn('exit "$requirements_status"',text)

if __name__ == "__main__": unittest.main()
