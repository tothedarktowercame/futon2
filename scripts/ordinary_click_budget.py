#!/usr/bin/env python3
"""Report issue-time budget consumption and reconcile it with durable run records."""
import re, sys, pathlib, datetime, json, argparse

HERE = pathlib.Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--runs", type=pathlib.Path, default=HERE / "data" / "wm-runs")
parser.add_argument("--ledger", type=pathlib.Path,
                    default=HERE / "data" / "wm-ordinary-clicks" / "consumption.jsonl")
args = parser.parse_args()
RUNS = args.runs
AUTH = {"path": "futon2/holes/labs/wm-contract/AUTH-ordinary-click-budget-2026-09-19.md",
        "sha": "d18e4f9c"}
entries = ([json.loads(line) for line in args.ledger.read_text().splitlines() if line.strip()]
           if args.ledger.exists() else [])
issued = [entry for entry in entries if entry["authorization"] == AUTH]
issued_ids = {entry["click-id"] for entry in issued}
GRANT = "2026-09-19T15:21:34+00:00"   # commit time of futon2 d18e4f9c
ALLOCATED = 5

grant = datetime.datetime.fromisoformat(GRANT)
click_re = re.compile(rb':click/id\s+"([^"]+)"')
start_re = re.compile(rb':startedAt\s+"([^"]+)"')

rows = []
for f in sorted(RUNS.glob("*.edn")):
    # Read the WHOLE file, not a head slice. First cut of this script read
    # 4096 bytes and found 6 of the 10 click records: in a grounded-change run
    # the record opens with :runner-execution/identity and a large :terminal,
    # so :click/id lands well past 4 KB. An enumerator that silently misses
    # records is the same defect it exists to prevent, so the cheap correct
    # read wins over the clever one (ten files, ~1.7 MB each).
    blob = f.read_bytes()
    c, s = click_re.search(blob), start_re.search(blob)
    if not c:
        continue                      # not a click-path run
    when = None
    if s:
        raw = s.group(1).decode()
        try:
            when = datetime.datetime.fromisoformat(raw.replace("Z", "+00:00"))
        except ValueError:
            pass
    rows.append((when, c.group(1).decode(), f.name))

after = [r for r in rows if r[0] and r[0] > grant]
undated = [r for r in rows if not r[0]]

print(f"grant      : {GRANT}  (futon2 d18e4f9c)")
print(f"allocated  : {ALLOCATED}")
print(f"consumed   : {len(issued)}")
print(f"remaining  : {ALLOCATED - len(issued)}")
recorded_ids = {cid for _, cid, _ in rows}
missing = issued_ids - recorded_ids
# Records without an issue entry may be separately authorized RUN4/R10 runs.
# Report the discrepancy; do not charge them to the ordinary grant by inference.
unmatched = {cid for _, cid, _ in after} - issued_ids
for entry in issued:
    print(f"  ISSUED {entry['issued-at']} {entry['click-id']} caller={entry['caller']}")
for cid in sorted(missing):
    print(f"  !! ISSUED WITHOUT RUN RECORD: {cid}")
for cid in sorted(unmatched):
    print(f"  !! POST-GRANT RUN WITHOUT ORDINARY ISSUE: {cid} — reconcile specialized authority or missing consumption")
for _, cid, name in undated:
    print(f"  !! UNDATED CLICK RECORD: {cid} {name}")
diverged = bool(missing or unmatched or undated or len(issued_ids) != len(issued))
if len(issued_ids) != len(issued):
    print("  !! DUPLICATE ISSUE IDS")
print(f"reconciliation: issued-without-record={len(missing)} post-grant-without-issue={len(unmatched)} undated={len(undated)}")
if len(issued) >= ALLOCATED:
    print(f"BUDGET EXHAUSTED — return to Joe for renewal. Authority: {AUTH['path']} @ {AUTH['sha']}")
sys.exit(2 if diverged else 1 if len(issued) >= ALLOCATED else 0)
