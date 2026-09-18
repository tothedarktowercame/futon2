#!/usr/bin/env python3
"""Which tripwires actually tripped, read out of the durable trip reports.

A trip report is mostly :trip/observation -- on 2026-09-18 that was 91 MB of
repair-store dump per file, 6.2 GB across the directory, and nothing reads it.
The part worth having is small and sits in the first few lines of each file:
which wire fired, when, what action was taken, and the witness kind.

This streams each file and stops as soon as it reaches the observation, so the
census costs the headers and not the 6.2 GB.  Run it before deciding anything
about the bulk: it is the thing the bulk was being kept for.

  scripts/trip_census.py                      # summary by wire
  scripts/trip_census.py --by-day             # summary by wire and day
  scripts/trip_census.py --wire T10           # every trip for one wire
  scripts/trip_census.py --json               # machine-readable rows
"""
import argparse
import json
import os
import re
import sys
from collections import Counter, defaultdict

DEFAULT_ROOT = "/home/joe/code/futon2/data/wm-tripwires/trips"

# Reports appear in two layouts: a plain map with :trip/<k> keys, and the
# namespaced-map form #:trip{:<k> ...}.  One pattern reads both.
HEAD_BYTES = 262144   # the small fields are always far above the observation
KIND = re.compile(r':kind\s+:([A-Za-z0-9._/-]+)')


def read_header(path):
    """Return the small fields of one trip report, without reading its bulk.

    Layout-agnostic on purpose. Reports written before 2026-09-18 are pprinted
    over many lines, in either a plain map or the namespaced #:trip{...} form;
    reports written after are a single pr-str line. Scanning the head as text
    rather than line by line reads all three.
    """
    size = os.path.getsize(path)
    row = {"file": os.path.basename(path), "bytes": size}
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        head = fh.read(HEAD_BYTES)

    # Everything worth reading sits above the observation, which is the whole
    # bulk of the file; cutting there also keeps nested :kind keys inside the
    # observation from being mistaken for the witness kind.
    cut = re.search(r':(?:trip/)?observation\b', head)
    above = head[:cut.start()] if cut else head

    for key, pat in (("id", r'"([^"]*)"'),
                     ("recorded-at", r'"([^"]*)"'),
                     ("wire-id", r':([A-Za-z0-9._/-]+)'),
                     ("action", r':([A-Za-z0-9._/-]+)')):
        m = re.search(r':(?:trip/)?' + key + r'\s+' + pat, above)
        if m:
            row[key] = m.group(1)

    w = re.search(r':(?:trip/)?witness\b', above)
    if w:
        k = KIND.search(above, w.end())
        if k:
            row["kind"] = k.group(1)

    # pprint sometimes placed :trip/action after the observation, on the last
    # line of the file. That is cheap to recover from the tail, and leaving it
    # blank would read as "no action taken" rather than "not looked at".
    if "action" not in row and cut:
        with open(path, "rb") as fh:
            fh.seek(max(0, size - 4096))
            tail = fh.read().decode("utf-8", errors="replace")
        m = re.search(r':(?:trip/)?action\s+:([A-Za-z0-9._/-]+)', tail)
        if m:
            row["action"] = m.group(1)
    return row


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=DEFAULT_ROOT)
    ap.add_argument("--wire", help="show every trip for this wire id")
    ap.add_argument("--by-day", action="store_true")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    if not os.path.isdir(args.root):
        sys.exit(f"trip_census: no such directory: {args.root}")

    rows = [read_header(os.path.join(args.root, f))
            for f in sorted(os.listdir(args.root)) if f.endswith(".edn")]
    rows.sort(key=lambda r: r.get("recorded-at", ""))

    if args.json:
        json.dump(rows, sys.stdout, indent=1)
        print()
        return

    total_bytes = sum(r["bytes"] for r in rows)
    if args.wire:
        want = args.wire.lstrip(":")
        for r in rows:
            if r.get("wire-id") == want:
                print(f"{r.get('recorded-at','?'):32} {r.get('action','?'):12} "
                      f"{r.get('kind','(no witness kind)'):40} "
                      f"{r['bytes']/1e6:8.2f} MB  {r['file']}")
        return

    print(f"{len(rows)} trip reports, {total_bytes/1e9:.2f} GB, "
          f"{args.root}\n")
    if args.by_day:
        grid = defaultdict(Counter)
        for r in rows:
            grid[r.get("recorded-at", "?")[:10]][r.get("wire-id", "?")] += 1
        for day in sorted(grid):
            per = ", ".join(f"{w}x{n}" for w, n in sorted(grid[day].items()))
            print(f"  {day}  {per}")
        return

    wires = Counter(r.get("wire-id", "(unparsed)") for r in rows)
    kinds = defaultdict(Counter)
    span = defaultdict(list)
    size = Counter()
    for r in rows:
        w = r.get("wire-id", "(unparsed)")
        kinds[w][r.get("kind", "(no witness kind)")] += 1
        if r.get("recorded-at"):
            span[w].append(r["recorded-at"])
        size[w] += r["bytes"]
    print(f"{'wire':10} {'trips':>6} {'bytes':>10}  {'first':11} {'last':11}  witness kinds")
    for w, n in wires.most_common():
        s = sorted(span[w])
        ks = ", ".join(f"{k}({c})" for k, c in kinds[w].most_common(3))
        print(f"{w:10} {n:6} {size[w]/1e9:9.2f}G  {s[0][:10] if s else '?':11} "
              f"{s[-1][:10] if s else '?':11}  {ks}")


if __name__ == "__main__":
    main()
