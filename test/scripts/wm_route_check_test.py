import hashlib, importlib.util, subprocess, tempfile, unittest
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
  def run_check(self, binding_text, lean_output=None, click_state=None, click_id=None):
    # Hermetic: the click-state directory is always a temp dir, never the live record.
    td = tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup); d = Path(td.name)
    b = d/"click-run-binding-x.edn"; b.write_text(binding_text)
    acc = d/"acceptances"; acc.mkdir()
    if click_state is not None: (acc/f"click-{click_id}.edn").write_text(click_state)
    self.acceptance_dir = acc
    cmd = ["python3", "-I", str(SCRIPT), "--binding", str(b), "--acceptance-dir", str(acc)]
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
    self.assertIn(":gaps [{:position 0, :status :acceptance-record-absent}]", out)
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

  # The acceptance observation of clicks 36b47627 / fdc08aea / 78180753 (2026-10-19, 429 at
  # review), as recorded under futon3c data/wm-click-acceptances; and click 1a6de51f's, where
  # the reviewer answered.  Both verbatim field sets.
  OBS_429 = {"status": "refused", "reason": "meta-self-heal/acceptance-review-refused",
             "review-status": "failed", "review-reason": "meta-self-heal/producer-invoke-failed"}
  OBS_REVIEWED = {"status": "refused", "reason": "meta-self-heal/acceptance-review-refused",
                  "review-status": "refused", "review-reason": "acceptance-test-did-not-fail-for-injury-reason"}

  def test_review_transport_failure_is_admission_not_review_refused(self):
    # The bad case (refusal-totality #48b): before this fix any non-nil :review-status
    # projected to reviewRefused, so a 429 at review read as a reviewer's answer.
    m = module()
    def record(obs): return {"meta-self-heal/readiness": {"status": "ready", "acceptance-observation": obs}}
    binding = {"runtime-disposition": {"ordinary-selection-called?": True}}
    path, gaps, notes, why = m.project(binding, record(self.OBS_429))
    self.assertEqual((path, gaps, why), (["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused",
                                          "readinessReady", "ordinarySelection", "terminalRecord"], [], None))
    self.assertNotIn("reviewRefused", path)
    self.assertIn("no reviewer answer", notes[0]["note"])
    path, gaps, notes, why = m.project(binding, record(self.OBS_REVIEWED))
    self.assertEqual(path[:4], ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewRefused"])
    # A :review-status the rule does not name is a gap, not a guess.
    path, gaps, notes, why = m.project(binding, record({**self.OBS_429, "review-status": "timeout"}))
    self.assertEqual(gaps, [{"position": 0, "status": "not-recorded"}]); self.assertNotIn("admissionRun", path)

  @unittest.skipUnless(OLEAN.is_file(), "SelfHealRoute.olean not built on this host")
  def test_admission_refused_path_live_lean(self):
    # The kernel decides the projected path on the actual table: the four self-heal edges
    # through admissionRefused hold, and the only missing edge is the one click 2 first
    # exposed (the runtime ran ordinary selection after the route), not anything at admission.
    m = module(); path = ["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused", "readinessReady", "ordinarySelection", "terminalRecord"]
    with tempfile.TemporaryDirectory() as td:
      f = Path(td)/"T.lean"; f.write_text(m.lean_source(path, False))
      ran = subprocess.run([m.lake_executable(), "env", "lean", str(f)], cwd=MATHLIB, text=True, capture_output=True)
    self.assertEqual(m.judge(path, False, ran.stdout, ran.returncode),
                     {"verdict": "violates", "first-missing-edge": ["readinessReady", "ordinarySelection"]}, ran.stdout + ran.stderr)
    self.assertEqual(ran.stdout.count("EDGE"), 6)
    for i in range(4): self.assertIn(f"EDGE {i} true", ran.stdout)

  @unittest.skipUnless(OLEAN.is_file(), "SelfHealRoute.olean not built on this host")
  def test_rev3_states_live_lean(self):
    # The two new suffixes are decided by the kernel on the actual table, not by this script.
    for binding, state in [('{:click/id "p" :runtime-disposition {:status :refused :reason :meta-self-heal/preparation-refused}}', "preparationRefused"),
                           (self.CLICK_C78, "bindingRefused")]:
      ran, out = self.run_check(binding)
      self.assertIn(f":path [:readinessReady :{state} :terminalRecord]", out)
      self.assertIn(":verdict :conforms-suffix", out)

  # M3a: bindings with no run record read the acceptance observation from the click-state
  # record (futon3c data/wm-click-acceptances).  Before M3a every case below was
  # {:position 0 :status :not-recorded}; that is what these tests are named for.
  C4_ID = "wm-click-fca1c921-75a7-4ebf-8713-0c464ffbfb0f"
  REQUEST_TEXT = "(ns secret-test-source (:require [clojure.test]))"
  LIVE_36B = Path("/home/joe/code/futon3c/data/wm-click-acceptances/click-wm-click-36b47627-6e08-4402-a09f-1ef09caa8bdc.edn")
  FULL_429 = ["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused",
              "readinessReady", "routeRefused", "terminalRecord"]

  def click_state(self, status, obs_edn):
    result = "" if obs_edn is None else f" :result {{:runtime-disposition {{:acceptance-observation {obs_edn}}}}}"
    return (f'{{:schema :wm/click-state-v1 :click-id "{self.C4_ID}" :status {status}'
            f' :request {{:test-source "{self.REQUEST_TEXT}"}}{result}}}')

  OBS_429_EDN = ('{:status :refused :reason :meta-self-heal/acceptance-review-refused :review-status :failed'
                 ' :review-reason :meta-self-heal/producer-invoke-failed :review-detail "Exit 1: 429 Too Many Requests"}')

  def test_click_state_bad_cases_are_typed_gaps(self):
    suffix = ":path [:readinessReady :routeRefused :terminalRecord]"
    cases = [(None, ":gaps [{:position 0, :status :acceptance-record-absent}]"),
             (self.click_state(":running", self.OBS_429_EDN),
              ":gaps [{:position 0, :status :acceptance-record-not-completed, :click-status :running}]"),
             (self.click_state(":completed", "{:repair-claimed? false :acceptance-test {:id \"t\"}}"),
              ":gaps [{:position 0, :status :acceptance-observation-not-projectable}]"),
             (self.click_state(":completed", self.OBS_429_EDN.replace(":review-status :failed", ":review-status :timeout")),
              ":gaps [{:position 0, :status :acceptance-observation-not-projectable}]"),
             (self.click_state(":completed", None), ":gaps [{:position 0, :status :not-recorded}]")]
    for state, gap in cases:
      ran, out = self.run_check(CLICK4, "VERDICT true\nEDGE 0 true\nEDGE 1 true\n", state, self.C4_ID)
      self.assertIn(gap, out); self.assertIn(suffix, out)
      self.assertIn(":verdict :conforms-suffix", out)
      self.assertNotIn("secret-test-source", out); self.assertNotIn("Too Many", out)
      self.assertNotIn(":acceptance-record {", out)

  def test_click_state_observation_gives_a_full_path(self):
    state = self.click_state(":completed", self.OBS_429_EDN)
    # M5: a placed prefix asks the kernel for PLACED too; the pin is extended, not weakened.
    canned = "VERDICT true\n" + "".join(f"EDGE {i} true\n" for i in range(6)) + "PLACED true\n"
    ran, out = self.run_check(CLICK4, canned, state, self.C4_ID)
    self.assertIn(":path [" + " ".join(":" + s for s in self.FULL_429) + "]", out)
    self.assertIn(":gaps []", out)
    self.assertIn(":verdict :conforms,", out)
    self.assertIn(":placement :placed", out)
    self.assertIn(":source [:wm/click-state :result :runtime-disposition :acceptance-observation]", out)
    pin = hashlib.sha256((self.acceptance_dir/f"click-{self.C4_ID}.edn").read_bytes()).hexdigest()
    self.assertIn(f':sha256 "{pin}"', out)
    self.assertNotIn("secret-test-source", out); self.assertNotIn("Too Many", out)
    self.assertIn("conformsPath", module().lean_source(self.FULL_429, False))
    self.assertEqual(ran.returncode, 0)

  def test_click_state_before_ordinary_selection_keeps_readiness_gap(self):
    binding = ('{:click/id "' + self.C4_ID + '" :runtime-disposition {:status :refused'
               ' :ordinary-selection-called? true}}')
    ran, out = self.run_check(binding, "VERDICT true\nEDGE 0 true\n", self.click_state(":completed", self.OBS_429_EDN), self.C4_ID)
    self.assertIn(":path [:ordinarySelection :terminalRecord]", out)
    self.assertIn(":gaps [{:position 0, :status :readiness-not-recorded}]", out)
    self.assertIn("no reviewer answer", out)
    self.assertIn(":verdict :conforms-suffix", out)

  def test_click_state_with_ambiguous_terms_stays_not_checked_with_note(self):
    binding = ('{:click/id "' + self.C4_ID + '" :runtime-disposition {:status :refused'
               ' :reason :meta-self-heal/production-config-required}}')
    ran, out = self.run_check(binding, "VERDICT true\n", self.click_state(":completed", self.OBS_429_EDN), self.C4_ID)
    self.assertIn(":verdict :not-checked", out)
    self.assertIn('"absent-terms-ambiguous-between-readiness-and-route"', out)
    self.assertIn(":source [:wm/click-state", out)

  def test_click_state_without_binding_disposition_records_the_read(self):
    # Pre-F7 bindings (07653f2a, 576d3263, e162ce2d) have no :runtime-disposition.
    binding = '{:click/id "' + self.C4_ID + '" :binding-status :absent}'
    ran, out = self.run_check(binding, "VERDICT true\n", self.click_state(":completed", self.OBS_429_EDN), self.C4_ID)
    self.assertIn('"no-durable-disposition"', out); self.assertIn(":verdict :not-checked", out)
    self.assertIn(":source [:wm/click-state", out)
    ran, out = self.run_check(binding, "VERDICT true\n")
    self.assertIn(":gaps [{:position 0, :status :acceptance-record-absent}]", out)

  def test_acceptance_file_name_is_sanitised(self):
    self.assertEqual(module().acceptance_file("/d", "wm-click a/b").name, "click-wm-click_a_b.edn")

  @unittest.skipUnless(LIVE_36B.is_file(), "click 36b47627's click-state record not on this host")
  def test_live_click_state_pin_matches_lean_witness(self):
    # Ties test, record and Lean witness (SelfHealRoute.lean click36b47627) together.
    got = module().read_click_state(self.LIVE_36B)
    self.assertEqual(got["status"], "completed")
    self.assertEqual({k: got["observation"].get(k) for k in self.OBS_429}, self.OBS_429)
    self.assertEqual(got["sha256"], "2cf7272a570702b00c9d263085bd8091b19627d4f5056dcfe994ba01e1300bb4")

  @unittest.skipUnless(OLEAN.is_file(), "SelfHealRoute.olean not built on this host")
  def test_full_429_path_live_lean(self):
    m = module()
    with tempfile.TemporaryDirectory() as td:
      f = Path(td)/"T.lean"; f.write_text(m.lean_source(self.FULL_429, False))
      ran = subprocess.run([m.lake_executable(), "env", "lean", str(f)], cwd=MATHLIB, text=True, capture_output=True)
    self.assertEqual(m.judge(self.FULL_429, False, ran.stdout, ran.returncode), {"verdict": "conforms"}, ran.stdout + ran.stderr)

  # M5 (2026-10-20): the boundary defect beside the state, judged by the kernel.
  OBS_8DF4_EDN = ('{:status :refused :reason :meta-self-heal/acceptance-unavailable :acceptance-status :failed'
                  ' :acceptance-reason :meta-self-heal/producer-response-malformed :acceptance-failure nil :repair-claimed? false}')
  EDGES6 = "".join(f"EDGE {i} true\n" for i in range(6))

  def test_429_observation_projects_noReviewerAnswer_and_kernel_places_it(self):
    ran, out = self.run_check(CLICK4, None, self.click_state(":completed", self.OBS_429_EDN), self.C4_ID)
    self.assertIn(':boundary-observation {:state :admissionRefused, :defect ".admission .noReviewerAnswer", :run nil}', out)
    self.assertIn(":placement :placed", out)
    self.assertIn(":verdict :conforms,", out)
    self.assertEqual(ran.returncode, 0, ran.stdout + ran.stderr)

  def test_unreadable_answer_projects_proposal_defect(self):
    ran, out = self.run_check(CLICK4, None, self.click_state(":completed", self.OBS_8DF4_EDN), self.C4_ID)
    self.assertIn(":path [:activeInjury :diagnosisPrepared :proposalRefused :readinessReady :routeRefused :terminalRecord]", out)
    self.assertIn(':boundary-observation {:state :proposalRefused, :defect ".proposal .unreadableAnswer", :run nil}', out)
    self.assertIn(":placement :placed", out)
    self.assertIn(":verdict :conforms,", out)

  def test_unknown_transport_reason_keeps_state_and_gaps_defect(self):
    state = self.click_state(":completed", self.OBS_429_EDN.replace(":meta-self-heal/producer-invoke-failed", ":something-else"))
    ran, out = self.run_check(CLICK4, "VERDICT true\n" + self.EDGES6, state, self.C4_ID)
    self.assertIn(":admissionRefused :readinessReady", out)
    self.assertIn(':status :defect-not-projectable', out)
    self.assertIn(':review-status "failed", :review-reason "something-else"', out)
    self.assertIn(":placement :not-evaluated", out)
    self.assertNotIn(":boundary-observation", out)
    self.assertIn(":verdict :conforms-suffix", out)
    self.assertNotIn("PLACED", module().lean_source(self.FULL_429, False, None))

  def test_misplaced_defect_is_a_violation_from_the_kernel(self):
    # The bad case this packet is named for: the M2b misplacement (a transport failure at
    # review placed at reviewRefused).  Before M5 only a Python rule caught it; now the
    # kernel refuses the pair even though every edge of the path exists.
    m = module()
    path = ["activeInjury", "diagnosisPrepared", "admissionRun", "reviewRefused", "readinessReady", "routeRefused", "terminalRecord"]
    src = m.lean_source(path, False, ("reviewRefused", ".admission .noReviewerAnswer", None))
    self.assertIn("PLACED", src)
    with tempfile.TemporaryDirectory() as td:
      f = Path(td)/"T.lean"; f.write_text(src)
      ran = subprocess.run([m.lake_executable(), "env", "lean", str(f)], cwd=MATHLIB, text=True, capture_output=True)
    self.assertIn("PLACED false", ran.stdout, ran.stdout + ran.stderr)
    self.assertEqual(m.judge(path, False, ran.stdout, ran.returncode, True),
                     {"verdict": "violates", "reason": "placement", "placed": False})
    # Hand-written kernel output: the same verdict, and an asked-for PLACED that never printed.
    self.assertEqual(m.judge(path, False, "VERDICT true\n" + self.EDGES6 + "PLACED false\n", 0, True),
                     {"verdict": "violates", "reason": "placement", "placed": False})
    self.assertEqual(m.judge(path, False, "VERDICT true\n" + self.EDGES6, 0, True),
                     {"verdict": "not-checked", "reason": "lean-printed-no-placement"})
    # And through the script: the misplacement reaches the sidecar as a placement violation.
    ran, out = self.run_check(CLICK4, "VERDICT true\n" + self.EDGES6 + "PLACED false\n",
                              self.click_state(":completed", self.OBS_429_EDN), self.C4_ID)
    self.assertIn(":verdict :violates", out); self.assertIn(':reason "placement"', out)
    self.assertIn(":placement :violates-placement", out)
    self.assertIn(':placement-violation {:state :admissionRefused, :defect ".admission .noReviewerAnswer"}', out)
    self.assertEqual(ran.returncode, 1)

  def test_run_refusals_project_admission_defects_and_run_outcome(self):
    refused = ('{:status :refused :reason :meta-self-heal/acceptance-run-refused'
               ' :acceptance-result {:status :errored} :repair-claimed? false}')
    ran, out = self.run_check(CLICK4, "VERDICT true\n" + self.EDGES6 + "PLACED true\n",
                              self.click_state(":completed", refused), self.C4_ID)
    self.assertIn(":path [:activeInjury :diagnosisPrepared :admissionRun :admissionRefused :readinessReady :routeRefused :terminalRecord]", out)
    self.assertIn(':boundary-observation {:state :admissionRefused, :defect ".admission .runRefused", :run nil}', out)
    passes = ('{:status :refused :reason :meta-self-heal/acceptance-test-passes-before-repair'
              ' :acceptance-result {:status :ran :outcome :passed :exit 0} :repair-claimed? false}')
    ran, out = self.run_check(CLICK4, None, self.click_state(":completed", passes), self.C4_ID)
    self.assertIn(':boundary-observation {:state :admissionRefused, :defect ".admission .passesBeforeRepair", :run ".passed"}', out)
    self.assertIn(":placement :placed", out)
    self.assertIn(":verdict :conforms,", out)

  def test_active_set_unavailable_is_a_full_path_from_the_read(self):
    # Rev 6: the disposition M6a returns for row #5 starts at ledgerRead, so no position-0 gap.
    binding = CLICK4.replace(":reason :meta-self-heal/production-config-required", ":reason :meta-self-heal/active-set-unavailable")
    ran, out = self.run_check(binding, None, None, self.C4_ID)
    self.assertIn(":path [:ledgerRead :activeSetUnavailable :terminalRecord]", out)
    self.assertIn(":verdict :conforms,", out)
    self.assertIn(":placement :not-evaluated", out)
    self.assertEqual(ran.returncode, 0, ran.stdout + ran.stderr)

  def test_review_refusal_run_is_read_from_the_nested_request_result_only(self):
    # M7L: the bounded run of a review refusal is under the review request; only its
    # :status :outcome :exit reach the sidecar, never the request (it carries the test).
    obs = (self.OBS_429_EDN[:-1] + ' :request {:result {:status :ran :outcome :failed :exit 1 :summary "secret-summary"}'
           ' :acceptance-test {:id "t" :source {:text "secret-nested-source"}}}}')
    ran, out = self.run_check(CLICK4, "VERDICT true\n" + self.EDGES6 + "PLACED true\n",
                              self.click_state(":completed", obs), self.C4_ID)
    self.assertIn(':boundary-observation {:state :admissionRefused, :defect ".admission .noReviewerAnswer", :run ".failed"}', out)
    self.assertNotIn("secret", out)
    m = module()
    state, defect, run, gap = m.boundary_observation({**self.OBS_429, "request": {"result": {"status": "ran", "outcome": "errored"}}},
                                                     ["activeInjury", "diagnosisPrepared", "admissionRun", "admissionRefused"])
    self.assertEqual((state, defect, run, gap), ("admissionRefused", ".admission .noReviewerAnswer", ".errored", None))

  @unittest.skipUnless(LIVE_36B.is_file(), "live 36b47627 record not on this host")
  def test_live_36b47627_run_is_failed_as_the_rev4_witness_says(self):
    got = module().read_click_state(self.LIVE_36B)
    self.assertEqual(got["observation"].get("acceptance-run"), {"status": "ran", "outcome": "failed", "exit": 1})

if __name__ == "__main__":
  unittest.main()
