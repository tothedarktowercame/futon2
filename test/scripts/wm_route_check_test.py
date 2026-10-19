import importlib.util, subprocess, tempfile, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT/"scripts/wm_route_check.py"
MATHLIB = Path("/home/joe/code/mathlib4")
OLEAN = MATHLIB/".lake/build/lib/lean/DarkTower/WarMachine/SelfHealRoute.olean"
# Click 2's run record is gitignored run data; the canonical checkout retains it.
CLICK2_RECORD = Path("/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-19-08771834-5add-4b3e-9459-abeaab21477c.edn")
CLICK2_PATH = ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewRefused",
               "readinessReady", "ordinarySelection", "terminalRecord"]
# Click 4's binding, verbatim (futon3c, sha256 0e3f984b...).
CLICK4 = ('{:schema :wm-click-run-binding-v1, :attempt/id nil, :click/id "wm-click-fca1c921-75a7-4ebf-8713-0c464ffbfb0f", '
          ':outcome :unknown, :binding-status :absent, :runtime-disposition {:status :refused, '
          ':reason :meta-self-heal/production-config-required, :route :dedicated-meta-self-heal, '
          ':ordinary-selection-called? false, :absent-terms [:acceptance-test]}, '
          ':run-record-absence :runner-did-not-return-run-record, :run-record-status :absent}')

def module():
  spec = importlib.util.spec_from_file_location("wm_route_check_under_test", SCRIPT)
  m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m

class RouteCheckTest(unittest.TestCase):
  def run_check(self, binding_text, lean_output=None):
    td = tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); d = Path(td.name)
    b = d/"click-run-binding-x.edn"; b.write_text(binding_text)
    cmd = ["python3", "-I", str(SCRIPT), "--binding", str(b)]
    if lean_output is not None:
      (d/"lean.out").write_text(lean_output); cmd += ["--lean-output", str(d/"lean.out")]
    ran = subprocess.run(cmd, cwd=ROOT, text=True, capture_output=True)
    self.assertNotIn("Traceback", ran.stderr)
    return ran, Path(str(b) + ".route.edn").read_text()

  @unittest.skipUnless(CLICK2_RECORD.is_file(), "click 2 run record not on this host")
  def test_click2_projects_and_violates(self):
    m = module()
    record = m.read_edn(CLICK2_RECORD, [":meta-self-heal/readiness", ":failure"])
    path, gaps, notes, reason = m.project({"run-record": str(CLICK2_RECORD)}, record)
    self.assertEqual((path, gaps, reason), (CLICK2_PATH, [], None))
    self.assertTrue(any("F2a preflight" in n["note"] for n in notes))
    canned = "VERDICT false\n" + "".join(f"EDGE {i} {'false' if i == 4 else 'true'}\n" for i in range(6))
    ran, out = self.run_check(f'{{:click/id "c2" :run-record "{CLICK2_RECORD}"}}', canned)
    self.assertIn(":verdict :violates", out)
    self.assertIn(":first-missing-edge [:readinessReady :ordinarySelection]", out)
    self.assertIn(":gaps []", out)   # the two projection notes are :notes, not gaps
    self.assertIn(":notes [{:position 0, :source [:meta-self-heal/readiness :acceptance-observation]", out)
    self.assertEqual(ran.returncode, 1)

  def test_click4_is_a_suffix_not_a_path(self):
    ran, out = self.run_check(CLICK4, "VERDICT true\nEDGE 0 true\nEDGE 1 true\n")
    self.assertIn(":path [:readinessReady :routeRefused :terminalRecord]", out)
    self.assertIn(":gaps [{:position 0, :status :not-recorded}]", out)
    self.assertIn(":notes []", out)
    self.assertIn(":verdict :conforms-suffix", out)
    self.assertNotIn(":verdict :conforms,", out)
    self.assertIn("conformsSuffix", module().lean_source(["readinessReady", "routeRefused", "terminalRecord"], True))

  def test_truncated_path_never_conforms(self):
    m = module()
    verdict = m.judge(["activeInjury", "diagnosisPrepared"], False, "VERDICT false\nEDGE 0 true\n", 0)
    self.assertEqual(verdict["verdict"], "violates")
    self.assertEqual(verdict["reason"], "every-edge-present-path-not-complete")
    self.assertIn("conformsPath [activeInjury, diagnosisPrepared]", m.lean_source(["activeInjury", "diagnosisPrepared"], False))

  @unittest.skipUnless(OLEAN.is_file(), "SelfHealRoute not built in the mathlib root")
  def test_truncated_path_live_lean(self):
    m = module(); path = ["activeInjury", "diagnosisPrepared"]
    with tempfile.TemporaryDirectory() as td:
      f = Path(td)/"T.lean"; f.write_text(m.lean_source(path, False))
      ran = subprocess.run([m.lake_executable(), "env", "lean", str(f)], cwd=MATHLIB, text=True, capture_output=True)
    self.assertEqual(m.judge(path, False, ran.stdout, ran.returncode)["verdict"], "violates")

  def test_no_verdict_line_is_not_checked(self):
    ran, out = self.run_check(CLICK4, "EDGE 0 true\nEDGE 1 true\n")
    self.assertIn(":verdict :not-checked", out)
    self.assertIn('"lean-printed-no-verdict"', out)
    self.assertEqual(module().judge(["a", "terminalRecord"], True, "VERDICT true\n", 1)["verdict"], "not-checked")

  def test_binding_without_disposition_is_not_checked(self):
    ran, out = self.run_check('{:click/id "c1" :binding-status :absent}', "VERDICT true\n")
    self.assertIn(":verdict :not-checked", out)
    self.assertIn('"no-durable-disposition"', out)

  def test_ambiguous_terms_are_not_projected(self):
    m = module()
    for d, expect in [({"status": "refused", "reason": "meta-self-heal/production-config-required",
                        "absent-terms": ["acceptance-run-port"]}, "absent-terms-ambiguous-between-readiness-and-route"),
                      ({"status": "refused", "reason": "meta-self-heal/production-config-required",
                        "absent-terms": ["diagnosis-port"], "before-ration-spend?": True}, None)]:
      self.assertEqual(m.project({"runtime-disposition": d}, None)[3], expect)
    path = m.project({"runtime-disposition": {"status": "refused", "reason": "meta-self-heal/ledger-unreadable"}}, None)[0]
    self.assertEqual(path, ["ledgerUnreadable", "terminalRecord"])

  # Click c78d0f27's binding, verbatim (futon3c data/wm-click-run-bindings, closed 19:43:49Z).
  CLICK_C78 = ('{:schema :wm-click-run-binding-v1, :attempt/id nil, :click/id "wm-click-c78d0f27-deca-4390-9b30-f36418714a54", '
               ':outcome :unknown, :binding-status :absent, :runtime-disposition {:status :refused, '
               ':reason :meta-self-heal/diagnosis-transaction-mismatch, :route :dedicated-meta-self-heal, '
               ':ordinary-selection-called? false}, :run-record-absence :runner-did-not-return-run-record, '
               ':run-id-observation {:status :absent, :reason :runner-did-not-return-run-id}, :run-record-status :absent}')

  def test_prepare_refusal_projects_after_readiness(self):
    m = module()
    path, gaps, notes, reason = m.project({"runtime-disposition": {"status": "refused", "reason": "meta-self-heal/preparation-refused"}}, None)
    self.assertEqual((path, gaps, notes, reason), (["readinessReady", "preparationRefused", "terminalRecord"], [{"position": 0, "status": "not-recorded"}], [], None))

  def test_binding_mismatch_projects_with_the_masking_caveat(self):
    # The bad case: the pre-fix click's recorded reason is the masked form. The producer
    # projects the record's word (bindingRefused) and says in :notes why that may be wrong;
    # it does not rewrite the record to preparationRefused.
    m = module()
    for reason in ("meta-self-heal/diagnosis-transaction-mismatch", "meta-self-heal/acceptance-transaction-mismatch"):
      path, gaps, notes, why = m.project({"runtime-disposition": {"status": "refused", "reason": reason}}, None)
      self.assertEqual((path, why), (["readinessReady", "bindingRefused", "terminalRecord"], None))
      self.assertEqual(len(notes), 1); self.assertIn("df2209a33", notes[0]["note"]); self.assertEqual(notes[0]["position"], 1)
    ran, out = self.run_check(self.CLICK_C78, "VERDICT true\nEDGE 0 true\nEDGE 1 true\n")
    self.assertIn(":path [:readinessReady :bindingRefused :terminalRecord]", out)
    self.assertIn(":verdict :conforms-suffix", out)
    self.assertIn("masked preparation refusal", out)

  @unittest.skipUnless(OLEAN.is_file(), "SelfHealRoute.olean not built on this host")
  def test_rev3_states_live_lean(self):
    # The two new suffixes are decided by the kernel on the actual table, not by this script.
    for binding, state in [('{:click/id "p" :runtime-disposition {:status :refused :reason :meta-self-heal/preparation-refused}}', "preparationRefused"),
                           (self.CLICK_C78, "bindingRefused")]:
      ran, out = self.run_check(binding)
      self.assertIn(f":path [:readinessReady :{state} :terminalRecord]", out)
      self.assertIn(":verdict :conforms-suffix", out)

if __name__ == "__main__":
  unittest.main()
