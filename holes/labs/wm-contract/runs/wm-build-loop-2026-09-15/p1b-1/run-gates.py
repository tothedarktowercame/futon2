"""Run each gate directly, without a shell/pipeline; retain both streams and exit status."""
from pathlib import Path
import datetime
import hashlib
import json
import subprocess
import time

ROOT = Path(__file__).resolve().parents[6]
OUT = Path(__file__).resolve().parent
SRC = "src/futon2/aif/work_target_store.clj"
TEST = "test/futon2/aif/work_target_store_test.clj"
PROBE = str(OUT.relative_to(ROOT) / "filesystem-probe.clj")
GATES = [
    ("clj-kondo", ["clj-kondo", "--lint", SRC, TEST, PROBE]),
    ("check-parens", ["emacs", "-Q", "--batch", "-l", "/home/joe/code/futon4/dev/check-parens.el", "--eval", "(arxana-check-parens-cli)", "--", "--no-defaults", SRC, TEST, PROBE]),
    ("tests", ["clojure", "-X:test", ":nses", "[futon2.aif.work-target-store-test]"]),
    ("filesystem", ["clojure", "-M", PROBE]),
    ("mount", ["findmnt", "-T", str(ROOT), "-o", "TARGET,FSTYPE,OPTIONS"]),
    ("disk", ["df", "-h", str(ROOT)]),
]
receipts = []
for name, argv in GATES:
    start = time.monotonic()
    with (OUT / (name + ".stdout")).open("w") as stdout, (OUT / (name + ".stderr")).open("w") as stderr:
        result = subprocess.run(argv, cwd=ROOT, stdout=stdout, stderr=stderr)
    receipt = {"gate": name, "argv": argv, "cwd": str(ROOT), "exit-status": result.returncode,
               "elapsed-seconds": round(time.monotonic() - start, 3)}
    receipts.append(receipt)
    print(json.dumps(receipt), flush=True)
    if result.returncode:
        print((OUT / (name + ".stdout")).read_text())
        print((OUT / (name + ".stderr")).read_text())
        break
(OUT / "gate-receipts.json").write_text(json.dumps({
    "at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "source-sha256": {p: hashlib.sha256((ROOT / p).read_bytes()).hexdigest() for p in [SRC, TEST, PROBE]},
    "gates": receipts,
}, indent=2) + "\n")
raise SystemExit(next((r["exit-status"] for r in receipts if r["exit-status"]), 0))
