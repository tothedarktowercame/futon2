import json, subprocess, tempfile, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/"scripts/wm_run_alert.py"
CLICK20=ROOT/"data/wm-runs/tick-run-record-2026-09-30-1790748841.edn"

def good_facts():
    ids=lambda a,n:[str(x) for x in range(a,a+n)]
    f={"openMissions":ids(0,221),"openExcursions":ids(221,373),"openTickets":ids(594,43),
       "enumeratedTasks":ids(0,637),"targetsReachingScoring":ids(0,637),"targetsWithG":ids(0,637),
       "libraryPatternCount":1431,"targetConstruction":[{"targets":ids(0,637),"slice":ids(3000,30),"pool":ids(3000,30),"sliceFromWholeLibrary":True,"policyCount":3}],
       "constructorPatternCount":30,"constructedCascades":ids(50000,1911),"comparedPolicies":ids(60000,1911),
       "cascadesWithoutG":[],"horizonLength":4,"preferenceSteps":[0,1,2,3],
       "gTerms":{"risk":True,"ambiguity":True,"informationGain":True},"interpretationOrder":"selectionBeforeInterpretation",
       "pathAbsenceCount":0,"previousChoice":{"target":"0","cascade":"50000"},"previousOutcome":"refused",
       "previousInputDigest":"7","currentChoice":{"target":"1","cascade":"50001"},"currentInputDigest":"7",
       "seatsAvailable":ids(0,56),"seatsUsed":ids(0,10),"completionPreferencePairs":12,
       "completionPairsStrictlyPreferred":12,"differentArrangementPairs":20,"arrangementPairsDistinguishedByG":20}
    return {"facts":f,"diagnostics":{"notRecomputable":[]}}

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

  def test_good_fixture_conforms(self):
    td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); path=Path(td.name)/"good.json"; path.write_text(json.dumps(good_facts()))
    p,_=self.invoke(["--facts-json",str(path)]); self.assertEqual(p.returncode,0,p.stdout+p.stderr); self.assertTrue(p.stdout.startswith("CONFORMS\n"))
    for q in range(1,11): self.assertIn(f"Q{q} holds:",p.stdout)

  def test_checker_failure_is_an_alert(self):
    p,_=self.invoke(["--mathlib-root","/definitely/missing"]); self.assertNotEqual(p.returncode,0); self.assertIn("requirements NOT CHECKED",p.stdout)

  def test_click_script_requires_checker_and_verdict(self):
    text=(ROOT/"scripts/wm_click.sh").read_text()
    self.assertIn('python3 "$F2/scripts/wm_run_alert.py"',text)
    self.assertIn('if [ ! -f "$verdict_file" ]',text)
    self.assertIn('exit "$requirements_status"',text)

if __name__ == "__main__": unittest.main()
