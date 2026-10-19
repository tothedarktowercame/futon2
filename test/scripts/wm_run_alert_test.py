import contextlib, importlib.util, io, json, subprocess, tempfile, threading, unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest import mock
import urllib.error

ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/"scripts/wm_run_alert.py"
# Worktrees deliberately omit gitignored run artifacts; the canonical checkout
# retains this recorded fixture used by the pre-existing alert regression.
CLICK20=next((path for path in (ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn",
                                Path("/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-30-1790748841.edn"))
              if path.is_file()), ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn")

def loaded_alert_module():
  spec=importlib.util.spec_from_file_location("wm_run_alert_under_test",SCRIPT)
  module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
  return module

FACTS={"enumeratedTasks":[],"openMissions":[],"openExcursions":[],"openTickets":[],
       "targetConstruction":[],"libraryPatternCount":0,"cascadesWithoutG":[],
       "constructedCascades":[],"preferenceSteps":[],"horizonLength":0,"gTerms":0,
       "interpretationOrder":[],"previousChoice":"none","currentChoice":"none",
       "previousInputDigest":"none","currentInputDigest":"none","pathAbsenceCount":0,
       "targetsReachingScoring":[],"targetsWithG":[],"comparedPolicies":[],
       "completionPairsStrictlyPreferred":0,"completionPreferencePairs":0,
       "arrangementPairsDistinguishedByG":0,"differentArrangementPairs":0}

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

  def mocked_evaluation(self):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); root=Path(td.name)
    record=root/"run.edn"; record.write_text("{}\n")
    facts=root/"facts.json"; facts.write_text(json.dumps({"facts":FACTS,"diagnostics":{}}))
    module=loaded_alert_module()
    module.run=lambda cmd,_: subprocess.CompletedProcess(cmd,0,
      "\n".join(f"Requirement.q{q}, fixture Outcome.holds" for q in range(1,11)),"")
    module.lake_executable=lambda: "lake"; module.sha=lambda *_: "fixture-sha"
    return module,root,record,facts

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
    self.assertIn("EVIDENCE PUBLICATION FAILED: HTTPError status 403",p.stdout)
    self.assertNotIn("requirements NOT CHECKED",p.stdout)
    persisted=Path(str(rec)+".requirements.edn").read_text()
    self.assertIn('":evidence-publication"',persisted)
    self.assertIn('":evidence-http-status"',persisted)
    self.assertIn("403",persisted)

  def test_alert_log_failure_after_evaluation_is_not_unchecked(self):
    module,root,record,facts=self.mocked_evaluation(); log=root/"alerts.log"; verdict=Path(str(record)+".requirements.edn")
    original_open=Path.open
    def deny_alert_log(path,*args,**kwargs):
      if path == log: raise PermissionError("fixture log denied")
      return original_open(path,*args,**kwargs)
    out=io.StringIO()
    with mock.patch.object(Path,"open",deny_alert_log), contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),"--facts-json",str(facts),"--alerts-log",str(log),"--no-evidence"])
    self.assertEqual(3,status); self.assertIn("CONFORMS",out.getvalue())
    self.assertIn("POST-CHECK VERDICT-OR-ALERT-LOG FAILED: PermissionError",out.getvalue())
    self.assertNotIn("requirements NOT CHECKED",out.getvalue())
    self.assertIn('":verdicts"',verdict.read_text())

  def test_receipt_persistence_failure_after_evaluation_is_not_unchecked(self):
    module,root,record,facts=self.mocked_evaluation(); log=root/"alerts.log"; verdict=Path(str(record)+".requirements.edn")
    original_write=Path.write_text; writes=0
    def truncate_second_receipt_temp(path,data,*args,**kwargs):
      nonlocal writes
      if path.parent == root and path.name.endswith(".tmp"):
        writes+=1
        if writes == 2:
          original_write(path,"{")
          raise OSError("fixture receipt write interrupted")
      return original_write(path,data,*args,**kwargs)
    transport=urllib.error.HTTPError("http://fixture",403,"Forbidden",None,None); out=io.StringIO()
    with mock.patch.object(Path,"write_text",truncate_second_receipt_temp), \
         mock.patch.object(module.urllib.request,"urlopen",side_effect=transport), \
         contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),"--facts-json",str(facts),"--alerts-log",str(log)])
    self.assertEqual(3,status); self.assertIn("CONFORMS",out.getvalue())
    self.assertIn("EVIDENCE PUBLICATION FAILED: HTTPError status 403",out.getvalue())
    self.assertIn("POST-CHECK PUBLICATION-RECEIPT-PERSISTENCE FAILED: OSError",out.getvalue())
    self.assertNotIn("requirements NOT CHECKED",out.getvalue())
    self.assertIn('":pending"',verdict.read_text())
    self.assertEqual([],list(root.glob(f".{verdict.name}.*.tmp")))

  def test_receipt_replace_failure_preserves_exact_pending_sidecar_bytes(self):
    module,root,record,facts=self.mocked_evaluation(); log=root/"alerts.log"; verdict=Path(str(record)+".requirements.edn")
    original_replace=module.os.replace; replaces=0; initial={}
    def deny_second_verdict_replace(source,destination):
      nonlocal replaces
      if Path(destination) == verdict:
        replaces+=1
        if replaces == 2:
          initial["bytes"]=verdict.read_bytes()
          raise OSError("fixture replace denied")
      return original_replace(source,destination)
    transport=urllib.error.HTTPError("http://fixture",403,"Forbidden",None,None); out=io.StringIO()
    with mock.patch.object(module.os,"replace",deny_second_verdict_replace), \
         mock.patch.object(module.urllib.request,"urlopen",side_effect=transport), \
         contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),"--facts-json",str(facts),"--alerts-log",str(log)])
    self.assertEqual(3,status); self.assertIn("CONFORMS",out.getvalue())
    self.assertIn("POST-CHECK PUBLICATION-RECEIPT-PERSISTENCE FAILED: OSError",out.getvalue())
    self.assertNotIn("requirements NOT CHECKED",out.getvalue())
    self.assertEqual(initial["bytes"],verdict.read_bytes())
    self.assertEqual([],list(root.glob(f".{verdict.name}.*.tmp")))

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
