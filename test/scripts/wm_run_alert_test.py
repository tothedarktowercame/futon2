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
       "arrangementPairsDistinguishedByG":0,"differentArrangementPairs":0,
       "kind":"work","constructorPatternCount":0,"policyG":[],"abstentionG":0,"policyPrediction":[],
       "policyGScale":1000000}
SELECTION_FACTS=dict(FACTS,kind="selection",selectionPath={
  "receipt":{"observed":"h"},"pickRelation":"absent","pickReach":"defaulted",
  "pendingRun":["run0","absent"],"consumedRuns":[["run0","absent"]],"learningApplied":True})
REQUIREMENTS=(1,2,3,4,5,6,7,9,11)

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

  def mocked_evaluation(self, lean_output=None, selection_output=None, selection_facts=SELECTION_FACTS):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); root=Path(td.name)
    record=root/"run.edn"; record.write_text("{}\n")
    work=root/"facts.json"; work.write_text(json.dumps({"facts":FACTS,"diagnostics":{"notRecomputable":["w"]}}))
    selection=root/"selection.json"
    selection.write_text(json.dumps({"facts":selection_facts,"diagnostics":{"notRecomputable":["s"]}}))
    module=loaded_alert_module()
    all_hold={q:"holds" for q in REQUIREMENTS}
    work_output=lean_output if lean_output is not None else all_hold
    other_output=selection_output if selection_output is not None else all_hold
    def fake_run(cmd,_):
      output=other_output if any("Selection" in str(c) for c in cmd) else work_output
      return subprocess.CompletedProcess(cmd,0,
        "\n".join(f"Requirement.q{q}, fixture Outcome.{v}" for q,v in output.items()),"")
    module.run=fake_run
    module.lake_executable=lambda: "lake"; module.sha=lambda *_: "fixture-sha"
    return module,root,record,["--facts-json",str(work),"--selection-facts-json",str(selection)]

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
    for q in (1,2,4,5,6,7,9,11): self.assertRegex(p.stdout,rf"(?m)^Q{q} (violated|unverifiable):")
    for q in (8,10): self.assertNotRegex(p.stdout,rf"(?m)^Q{q} ")
    self.assertRegex(p.stdout,r"(?m)^work run:$"); self.assertRegex(p.stdout,r"(?m)^selection run:$")
    self.assertTrue(Path(str(rec)+".requirements.edn").is_file())

  def evaluate(self, lean_output=None, selection_output=None, selection_facts=SELECTION_FACTS):
    module,root,record,facts=self.mocked_evaluation(lean_output,selection_output,selection_facts); out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*facts,
                          "--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    return status,out.getvalue(),Path(str(record)+".requirements.edn").read_text()

  def export_run(self, fail_selection=False):
    """A fake `run` that answers each step, for the path with no fixtures."""
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); root=Path(td.name)
    record=root/"run.edn"; record.write_text("{}\n")
    module=loaded_alert_module(); calls=[]
    def fake_run(cmd,_):
      calls.append(cmd)
      if "wm-run-facts" in cmd:
        selection="--kind" in cmd
        if selection and fail_selection: raise subprocess.CalledProcessError(1,cmd)
        facts=SELECTION_FACTS if selection else FACTS
        return subprocess.CompletedProcess(cmd,0,json.dumps({"facts":facts}),"")
      return subprocess.CompletedProcess(cmd,0,
        "\n".join(f"Requirement.q{q}, fixture Outcome.holds" for q in REQUIREMENTS),"")
    module.run=fake_run; module.lake_executable=lambda: "lake"; module.sha=lambda *_: "fixture-sha"
    out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),"--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    return status,out.getvalue(),calls,Path(str(record)+".requirements.edn")

  def test_both_exports_are_requested(self):
    status,_,calls,_=self.export_run()
    exports=[c for c in calls if "wm-run-facts" in c]
    self.assertEqual(0,status); self.assertEqual(2,len(exports))
    self.assertNotIn("--kind",exports[1]); self.assertEqual(["--kind","selection"],exports[0][-2:])
    checks=[c for c in calls if "wm-check-run-facts" in c]
    self.assertEqual(["selection","work"],[c[6] for c in checks])
    self.assertEqual(["RunCheckSelection.lean","RunCheckWork.lean"],[Path(c[5]).name for c in checks])

  def test_refused_selection_evaluated_without_work(self):
    module,root,record,args=self.mocked_evaluation()
    selection=Path(args[3]); data=json.loads(selection.read_text())
    data["diagnostics"].update(recordKind="selection-record", workRunStatus="not-run",
                               selectionOutcome={"status":"refused","kind":"tie-at-minimum"})
    selection.write_text(json.dumps(data))
    original=module.run; calls=[]
    def checked(cmd,cwd):
      calls.append(cmd)
      self.assertFalse(any("RunCheckWork" in str(c) for c in cmd))
      return original(cmd,cwd)
    module.run=checked; out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*args,
                         "--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    self.assertEqual(1,status)
    self.assertIn("work run: not-run",out.getvalue())
    self.assertIn("selection run:",out.getvalue())
    self.assertNotIn("not-checked",out.getvalue())
    verdict=Path(str(record)+".requirements.edn").read_text()
    self.assertIn('":work-status" ":not-run"',verdict)
    self.assertIn('":conforms" false',verdict)
    self.assertEqual(2,len(calls))  # checker and Lean for selection only

  def bypassed(self, work_run):
    module,root,record,args=self.mocked_evaluation()
    selection=Path(args[3]); data=json.loads(selection.read_text())
    data["diagnostics"].update(recordKind="selection-record", selectionStatus="not-exercised",
                               selectionBypass="direct-task",
                               selectionOutcome={"status":"not-exercised","path":"direct-task"})
    if not work_run: data["diagnostics"]["workRunStatus"]="not-run"
    selection.write_text(json.dumps(data))
    out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*args,
                         "--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    return status,out.getvalue(),Path(str(record)+".requirements.edn").read_text()

  def test_not_exercised_selection_with_work_never_conforms(self):
    status,out,verdict=self.bypassed(True)
    self.assertEqual(1,status)
    self.assertTrue(out.startswith("ALERT FOR JOE\nwork run:\n"))
    self.assertIn("selection run: not exercised (direct-task)",out)
    self.assertNotIn("abstain-empty-field",out)
    self.assertIn('":selection-status" ":not-exercised"',verdict)
    self.assertIn('":work-status" ":evaluated"',verdict)
    self.assertIn('":conforms" false',verdict)

  def test_not_exercised_selection_without_a_tick_reports_work_not_run(self):
    status,out,verdict=self.bypassed(False)
    self.assertEqual(1,status)
    self.assertIn("work run: not-run",out)
    self.assertNotIn("work run:\n",out)
    self.assertIn("selection run: not exercised (direct-task)",out)
    self.assertIn('":work-status" ":not-run"',verdict)
    self.assertIn('":selection-status" ":not-exercised"',verdict)

  def test_both_conforming_conforms(self):
    status,stdout,verdict=self.evaluate()
    self.assertEqual(0,status); self.assertTrue(stdout.startswith("CONFORMS\nwork run:\n"))
    self.assertIn("\nselection run:\n",stdout); self.assertIn('":conforms" true',verdict)

  def test_selection_violation_fails_the_click(self):
    status,stdout,verdict=self.evaluate(selection_output={q:"violated" if q == 7 else "holds" for q in REQUIREMENTS})
    self.assertEqual(1,status); self.assertTrue(stdout.startswith("ALERT FOR JOE\n"))
    self.assertIn('":conforms" false',verdict)
    self.assertRegex(verdict,r'":verdicts" \{[^}]*":Q7" ":holds"')
    self.assertRegex(verdict,r'":selection-verdicts" \{[^}]*":Q7" ":violated"')
    self.assertRegex(stdout,r"(?m)^Q7 violated: 0 typed path absences; receipt observed, pickRelation absent, "
                            r"pickReach defaulted, pendingRun run0 absent, "
                            r"consumedRuns 1 total / 0 observed, learningApplied true$")

  def test_selection_export_failure_is_not_checked(self):
    status,stdout,_,verdict=self.export_run(fail_selection=True)
    self.assertEqual(2,status); self.assertIn("requirements NOT CHECKED",stdout)
    self.assertFalse(verdict.exists())

  def test_one_fixture_is_not_a_one_run_verdict(self):
    module,root,record,facts=self.mocked_evaluation(); out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*facts[:2],"--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    self.assertEqual(2,status); self.assertIn("requirements NOT CHECKED",out.getvalue())
    self.assertFalse(Path(str(record)+".requirements.edn").exists())

  def test_requirement_missing_from_selection_output_is_not_checked(self):
    status,stdout,verdict=self.evaluate(selection_output={q:"holds" for q in REQUIREMENTS if q != 11})
    self.assertEqual(1,status); self.assertRegex(stdout,r"(?m)^Q11 not-checked:")
    self.assertIn('":Q11" ":not-checked"',verdict); self.assertIn('":conforms" false',verdict)

  def test_mislabelled_export_is_not_checked(self):
    module,root,record,facts=self.mocked_evaluation(selection_facts=FACTS); out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*facts,"--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    self.assertEqual(2,status); self.assertIn("selection export is labelled 'work'",out.getvalue())

  def test_scales_that_differ_fail_the_click(self):
    status,stdout,verdict=self.evaluate(selection_facts=dict(SELECTION_FACTS,policyGScale=1000))
    self.assertEqual(1,status); self.assertIn("policyGScale differs: work 1000000 / selection 1000",stdout)
    self.assertIn('":conforms" false',verdict)

  def test_readers_keys_keep_their_shapes(self):
    _,_,verdict=self.evaluate()
    self.assertIn('":run-record" "',verdict)
    self.assertRegex(verdict,r'":conforms" (true|false)')
    self.assertRegex(verdict,r'":verdicts" \{":Q1" ":holds" ":Q2" ":holds"')

  def test_two_run_keys_are_written(self):
    _,_,verdict=self.evaluate()
    self.assertIn('":runs" [":work" ":selection"]',verdict)
    self.assertIn('":policy-g-scale" 1000000',verdict)
    self.assertRegex(verdict,r'":selection-critical-parameters" \{":Q1"')
    self.assertIn('":not-recomputable" ["w"]',verdict)
    self.assertIn('":selection-not-recomputable" ["s"]',verdict)

  def test_requirement_list_is_read_from_lean(self):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); ml=Path(td.name)
    path=ml/"DarkTower/WarMachine/Requirements.lean"; path.parent.mkdir(parents=True)
    path.write_text("inductive Requirement where | q1 | q3 | q12\n  deriving DecidableEq\n")
    self.assertEqual((1,3,12),loaded_alert_module().requirement_numbers(ml))
    path.write_text("-- no such declaration\n")
    with self.assertRaises(RuntimeError): loaded_alert_module().requirement_numbers(ml)

  def test_requirement_missing_from_lean_output_is_not_checked(self):
    output={q:"holds" for q in REQUIREMENTS if q != 7}
    status,stdout,verdict=self.evaluate(output)
    self.assertEqual(1,status); self.assertTrue(stdout.startswith("ALERT FOR JOE\n"))
    self.assertRegex(stdout,r"(?m)^Q7 not-checked:")
    self.assertIn('":Q7" ":not-checked"',verdict)
    self.assertIn('":conforms" false',verdict)

  def test_unverifiable_is_not_holds(self):
    status,stdout,verdict=self.evaluate({q:"unverifiable" if q == 11 else "holds" for q in REQUIREMENTS})
    self.assertEqual(1,status); self.assertTrue(stdout.startswith("ALERT FOR JOE\n"))
    self.assertRegex(stdout,r"(?m)^Q11 unverifiable:")
    self.assertIn('":conforms" false',verdict)

  def test_empty_lean_output_conforms_to_nothing(self):
    status,stdout,verdict=self.evaluate({})
    self.assertEqual(1,status)
    for q in REQUIREMENTS: self.assertRegex(stdout,rf"(?m)^Q{q} not-checked:")
    self.assertIn('":conforms" false',verdict)

  def test_outcomes_wrapped_over_lines_are_read(self):
    module,root,record,facts=self.mocked_evaluation()
    wrapped="[" + ",\n ".join(f"(Requirements.Requirement.q{q},\n  RequirementsReader.Outcome.holds)" for q in REQUIREMENTS) + "]"
    module.run=lambda cmd,_: subprocess.CompletedProcess(cmd,0,wrapped,"")
    out=io.StringIO()
    with contextlib.redirect_stdout(out):
      status=module.main(["--run-record",str(record),*facts,
                          "--alerts-log",str(root/"alerts.log"),"--no-evidence"])
    self.assertEqual(0,status); self.assertTrue(out.getvalue().startswith("CONFORMS"))

  def test_unavailable_selection_path_prints_its_reason(self):
    module=loaded_alert_module()
    facts=dict(SELECTION_FACTS,selectionPath={"not-recomputable":"no learning"})
    self.assertEqual("0 typed path absences; selectionPath: no learning",module.numbers(7,facts))

  def test_nr_carrier_prints_its_reason(self):
    module=loaded_alert_module()
    facts=dict(FACTS,policyG={"not-recomputable":"not yet projected"})
    self.assertIn("policyG: not yet projected",module.numbers(11,facts))

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
      status=module.main(["--run-record",str(record),*facts,"--alerts-log",str(log),"--no-evidence"])
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
      status=module.main(["--run-record",str(record),*facts,"--alerts-log",str(log)])
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
      status=module.main(["--run-record",str(record),*facts,"--alerts-log",str(log)])
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

  def test_evidence_publication_carries_futon1b_penholder(self):
    module,root,record,facts=self.mocked_evaluation(); log=root/"alerts.log"
    response=mock.MagicMock(); response.status=201
    response.__enter__.return_value=response
    with mock.patch.object(module.urllib.request,"urlopen",return_value=response) as urlopen:
      status=module.main(["--run-record",str(record),*facts,"--alerts-log",str(log)])
    self.assertEqual(0,status)
    request=urlopen.call_args.args[0]
    self.assertEqual("api",request.get_header("X-penholder"))

  def test_click_script_requires_checker_and_verdict(self):
    text=(ROOT/"scripts/wm_click.sh").read_text()
    self.assertIn('python3 "$F2/scripts/wm_run_alert.py"',text)
    self.assertIn('if [ ! -f "$verdict_file" ]',text)
    self.assertIn('exit "$requirements_status"',text)

if __name__ == "__main__": unittest.main()
