import importlib.util
import sys
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))


def load_prepare():
    spec = importlib.util.spec_from_file_location("prepare", ROOT / "scripts/wm_click_prepare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class PrepareTest(unittest.TestCase):
    def test_dependency_order_and_real_read_only_edn_response(self):
        p = load_prepare()
        names, inventory = p.declaration()
        order = [n for n, _ in p.reload.reload_order([inventory[n] for n in names])]
        self.assertLess(order.index("futon2.aif.wm.family-canonical-g"), order.index("futon2.aif.wm.family-selection"))
        response = "{:value {:namespaces {" + " ".join(n + " {:status :current}" for n in names) + "}}}"
        with mock.patch.object(p.reload, "jvm_eval", return_value={"rc": 0, "out": response}) as evaluate:
            self.assertTrue(all(r["status"] == "current" for r in p.snapshot(names).values()))
        self.assertNotIn("require", evaluate.call_args.args[0])
        self.assertNotIn("register!", evaluate.call_args.args[0])

    def test_stale_dependency_is_loaded_and_verified_before_success(self):
        p = load_prepare()
        names, inventory = p.declaration()
        scorer = "futon2.aif.wm.family-canonical-g"
        self.assertIn(scorer, names)
        current = {n: {"status": "current"} for n in names}
        stale = {**current, scorer: {"status": "stale"}}
        with mock.patch.object(p, "idle"), mock.patch.object(p, "clean"), \
             mock.patch.object(p, "snapshot", side_effect=[stale, current]), \
             mock.patch.object(p.reload, "reload_namespace", return_value={"rc": 0, "out": ":reloaded"}) as load:
            result = p.prepare()
        self.assertEqual("current", result["status"])
        load.assert_called_once_with(scorer)

    def test_current_does_not_reload(self):
        p = load_prepare()
        names, _ = p.declaration()
        with mock.patch.object(p, "idle"), mock.patch.object(p, "clean"), \
             mock.patch.object(p, "snapshot", return_value={n: {"status": "current"} for n in names}), \
             mock.patch.object(p.reload, "reload_namespace") as load:
            self.assertEqual("current", p.prepare()["status"])
        load.assert_not_called()

    def test_unregistered_loads_but_failed_load_refuses(self):
        p = load_prepare()
        names, _ = p.declaration()
        current = {n: {"status": "current"} for n in names}
        missing = {**current, names[-1]: {"status": "unregistered"}}
        for succeeds in [True, False]:
            with mock.patch.object(p, "idle"), mock.patch.object(p, "clean"), \
                 mock.patch.object(p, "snapshot", side_effect=[missing, current]), \
                 mock.patch.object(p.reload, "reload_namespace", return_value={"rc": 0 if succeeds else 1, "out": ":reloaded" if succeeds else "failed"}) as load:
                if succeeds:
                    self.assertEqual("current", p.prepare()["status"])
                else:
                    with self.assertRaisesRegex(p.PreparationRefused, "reload-failed"):
                        p.prepare()
                load.assert_called_once_with(names[-1])

    def test_git_error_refuses_but_dirty_closure_file_is_reported(self):
        p = load_prepare()
        path = p.reload.F2 / "src/scorer.clj"
        with mock.patch.object(p.reload, "sh", return_value=subprocess.CompletedProcess([], 1, "", "")):
            with self.assertRaisesRegex(p.PreparationRefused, "closure-source-status-unavailable"):
                p.clean([path])
        with mock.patch.object(p.reload, "sh", return_value=subprocess.CompletedProcess([], 0, " M src/scorer.clj", "")):
            self.assertEqual({path}, p.clean([path]))

    def test_untracked_file_outside_closure_prepares(self):
        self.assert_outside_dirty_file_is_ignored("?? src/futon2/aif/unrelated.clj")

    def test_tracked_modification_outside_closure_prepares(self):
        self.assert_outside_dirty_file_is_ignored(" M src/futon2/aif/unrelated.clj")

    def assert_outside_dirty_file_is_ignored(self, outside_status):
        p = load_prepare()
        names, inventory = p.declaration()
        closure = [inventory[n] for n in names]
        current = {n: {"status": "current"} for n in names}

        def status(*args, **kwargs):
            queried = args[-1]
            if queried == "src/futon2/aif/unrelated.clj":
                return subprocess.CompletedProcess([], 0, outside_status, "")
            return subprocess.CompletedProcess([], 0, "", "")

        with mock.patch.object(p, "idle"), \
             mock.patch.object(p.reload, "dependency_closure", side_effect=lambda files: files), \
             mock.patch.object(p.reload, "sh", side_effect=status) as git_status, \
             mock.patch.object(p, "snapshot", return_value=current), \
             mock.patch.object(p.reload, "reload_namespace") as load:
            result = p.prepare()
        self.assertEqual("current", result["status"])
        self.assertFalse(any(call.args[-1] == "src/futon2/aif/unrelated.clj"
                             for call in git_status.call_args_list))
        load.assert_not_called()

    def test_modified_closure_file_prepares_without_reload_and_records_status(self):
        p = load_prepare()
        names, inventory = p.declaration()
        changed = names[-1]
        path = inventory[changed]
        statuses = {n: {"status": "current"} for n in names}
        statuses[changed] = {"status": "stale"}

        def status(*args, **kwargs):
            output = " M " + str(path.relative_to(p.reload.F2)) if args[-1] == str(path.relative_to(p.reload.F2)) else ""
            return subprocess.CompletedProcess([], 0, output, "")

        with mock.patch.object(p, "idle"), \
             mock.patch.object(p.reload, "dependency_closure", side_effect=lambda files: files), \
             mock.patch.object(p.reload, "sh", side_effect=status), \
             mock.patch.object(p, "snapshot", return_value=statuses), \
             mock.patch.object(p.reload, "reload_namespace") as load:
            result = p.prepare()
        self.assertEqual("prepared", result["status"])
        self.assertEqual("stale", result["namespaces"][changed]["status"])
        self.assertIn(str(path), result["skipped-dirty"])
        load.assert_not_called()

    def test_still_stale_and_unavailable_refuse(self):
        for status in ["stale", "unavailable"]:
            with self.subTest(status=status):
                p = load_prepare()
                names, _ = p.declaration()
                with mock.patch.object(p, "idle"), mock.patch.object(p, "clean"), \
                     mock.patch.object(p, "snapshot", return_value={n: {"status": status} for n in names}), \
                     mock.patch.object(p.reload, "reload_namespace", return_value={"rc": 0, "out": ":reloaded"}) as load:
                    with self.assertRaises(p.PreparationRefused):
                        p.prepare()
                if status == "unavailable":
                    load.assert_not_called()

    def test_running_dirty_and_missing_status_refuse_without_load(self):
        for boundary in ["idle", "clean"]:
            p = load_prepare()
            with mock.patch.object(p, "idle"), mock.patch.object(p, "clean"), \
                 mock.patch.object(p, boundary, side_effect=p.PreparationRefused(boundary)), \
                 mock.patch.object(p.reload, "reload_namespace") as load:
                with self.assertRaises(p.PreparationRefused):
                    p.prepare()
            load.assert_not_called()
        p = load_prepare()
        for response in [{}, {"running?": True}]:
            with mock.patch.object(p.reload, "get_json", return_value=response):
                with self.assertRaises(p.PreparationRefused):
                    p.idle()

    def test_dry_run_has_no_remote_reads_or_loads(self):
        p = load_prepare()
        with mock.patch.object(p, "snapshot") as snapshot, mock.patch.object(p.reload, "reload_namespace") as load:
            self.assertEqual("not-prepared", p.prepare(dry_run=True)["status"])
        snapshot.assert_not_called()
        load.assert_not_called()

    def test_direct_script_preparation_refusal_never_posts_and_dry_run_never_loads(self):
        # Run the actual shell entrypoint. Every external HTTP/eval/preparation
        # boundary is a local executable fixture; no live server is contacted.
        for mode, run in [("refused", True), ("current", True), ("current", False)]:
            with self.subTest(mode=mode, run=run), tempfile.TemporaryDirectory() as td:
                home = Path(td)
                bin_dir = home / "bin"
                bin_dir.mkdir()
                event_path = home / "events"
                event_path.touch()
                def executable(path, body):
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(body)
                    path.chmod(0o755)
                executable(bin_dir / "python3", f'''#!{sys.executable}
import os,sys
if len(sys.argv)>1 and sys.argv[1].endswith("wm_click_prepare.py"):
    with open(os.environ["EVENTS"],"a") as f: f.write("prepare\\n")
    sys.exit(3 if os.environ["MODE"]=="refused" else 0)
os.execv({sys.executable!r}, [{sys.executable!r}]+sys.argv[1:])
''')
                executable(bin_dir / "curl", f'''#!{sys.executable}
import json,os,sys
if "POST" in sys.argv:
    with open(os.environ["EVENTS"],"a") as f: f.write("POST\\n")
    print(json.dumps({{"click-id":"fixture-click"}}))
elif "%{{http_code}}" in sys.argv: print("200")
elif sys.argv[-1].endswith("/agents"):
    print(json.dumps({{"agents":{{s:{{"status":"idle","invoke-ready?":True}} for s in ["zai-2","zai-3","zai-8","zai-4","zai-5","zai-6"]}}}}))
else: print(json.dumps({{"running?":False,"ordinary-click-budget":{{"available":2}},"click-id":"fixture-click"}}))
''')
                executable(bin_dir / "sleep", "#!/bin/sh\nexit 0\n")
                executable(home / "code/futon3c/scripts/proof-eval.sh", '#!/bin/sh\nprintf "eval\\n" >> "$EVENTS"\necho "{:tripping [] :will-defer [] :stop-lines-queued 0}"\n')
                result = subprocess.run(["bash", str(ROOT / "scripts/wm_click.sh"), *(["--run"] if run else []), "--ask-seats", "zai-4,zai-5,zai-6"],
                    env={**os.environ, "HOME": td, "TMPDIR": td, "EVENTS": str(event_path), "MODE": mode,
                         "GIRAFFE_MINT": "0",  # fixture F3C: the staleness gate has its own test (wm_click_giraffe_mint_test.py)
                         "PATH": str(bin_dir) + ":" + os.environ["PATH"]},
                    capture_output=True, text=True, timeout=15)
                events = event_path.read_text().splitlines()
                if mode == "refused":
                    self.assertEqual(3, result.returncode, result.stdout + result.stderr)
                    self.assertEqual(["prepare"], events)
                elif run:
                    self.assertEqual(1, events.count("POST"), result.stdout + result.stderr)
                    self.assertEqual(["prepare", "eval", "prepare", "POST"], events)
                    # Fake completion has no run record; retain existing alert.
                    self.assertIn("requirements NOT CHECKED", result.stdout)
                else:
                    self.assertEqual(0, result.returncode, result.stdout + result.stderr)
                    self.assertEqual([], events)


if __name__ == "__main__":
    unittest.main()
