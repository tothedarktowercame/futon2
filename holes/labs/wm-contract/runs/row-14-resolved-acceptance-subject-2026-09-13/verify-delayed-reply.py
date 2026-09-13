"""Read-only verification of the consumed 20611 reply and its successor."""
import hashlib
import json
import subprocess
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[5]
RECEIPTS = "holes/labs/wm-contract/runs/row-14-resolved-acceptance-subject-2026-09-13"
DISPATCH = "holes/labs/wm-contract/runs/lead-dispatch-2026-09-13"
OLD = "invoke-1789270740665-20611-33e6755e"
NEW = "invoke-1789271039934-20615-eb6ce892"

def blob(revision, path):
    return subprocess.check_output(["git", "-C", str(ROOT), "show", revision + ":" + path])

def digest(data):
    return hashlib.sha256(data).hexdigest()

pins = []
for line in blob("0fc3997c", RECEIPTS + "/source-pins.sha256").decode().splitlines():
    expected, absolute = line.split(maxsplit=1)
    relative = str(Path(absolute).relative_to(ROOT))
    actual = digest(blob("46b94b94", relative))
    assert actual == expected, relative
    pins.append({"path": relative, "sha256": actual, "source_commit": "46b94b94"})
assert len(pins) == 6
review = blob("48df6680", RECEIPTS + "/lead-acceptance.md")
assert review == (ROOT / RECEIPTS / "lead-acceptance.md").read_bytes()
raw_test = blob("0fc3997c", RECEIPTS + "/test-output.txt")
assert b"Ran 7 tests containing 38 assertions." in raw_test
assert b"0 failures, 0 errors." in raw_test
assert b"errors: 0, warnings: 0" in blob("0fc3997c", RECEIPTS + "/kondo-output.txt")
assert blob("0fc3997c", RECEIPTS + "/parens-output.txt").strip() == b"OK"
manifest_bytes = (ROOT / DISPATCH / "repair-wave.json").read_bytes()
manifest = json.loads(manifest_bytes)
assert OLD not in manifest["current-awaiting"]
assert NEW in manifest["current-awaiting"]
response_path = DISPATCH + "/codex-24-exact-close-annotation-join.response.json"
response = json.loads(blob("270d63f2", response_path))
assert response["accepted"] is True and response["job-id"] == NEW
jobs = []
for job_id in manifest["current-awaiting"]:
    with urllib.request.urlopen("http://localhost:7070/api/alpha/invoke/jobs/" + job_id, timeout=10) as stream:
        job = json.load(stream)["job"]
    jobs.append({"job-id": job_id, "state": job.get("state"), "artifact-ref": job.get("artifact-ref")})
print(json.dumps({"schema": "wm/delayed-reply-verification-v1",
    "checked_at": datetime.now(timezone.utc).isoformat(),
    "consumed_job": OLD, "successor_job": NEW,
    "acceptance_commit": "48df6680", "acceptance_sha256": digest(review),
    "source_pins_verified": pins, "retained_gates_verified": True,
    "tests_rerun": False, "new_dispatches": 0,
    "manifest_sha256": digest(manifest_bytes), "current_jobs": jobs,
    "result": "already-reviewed-and-superseded", "wm_completion": "unfinished"}, indent=2))
