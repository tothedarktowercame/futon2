#!/usr/bin/env python3
"""Per-click phase timings from the WM phase log.

Reads data/wm-full-loop-phases.edn.log (one EDN map per line, written by
full-loop-runner/emit-phase!) and prints one block per click: its start time,
each phase's duration in seconds and outcome, in the order phases ended, and
the click's wall-clock span. Phases that started but never ended are shown as
OPEN, so a click that is still running (or died) is visible.

  scripts/wm_click_timings.py            # last 10 clicks
  scripts/wm_click_timings.py -n 30
  scripts/wm_click_timings.py --since 2026-09-30
  scripts/wm_click_timings.py --tsv      # one row per phase, for spreadsheets
"""
import argparse
import re
from collections import OrderedDict
from datetime import datetime
from pathlib import Path

LOG = Path(__file__).resolve().parent.parent / "data" / "wm-full-loop-phases.edn.log"


def field(line, key, pat=r'"([^"]*)"'):
    m = re.search(r":%s %s" % (re.escape(key), pat), line)
    return m.group(1) if m else None


def parse(line):
    return {
        "opp": field(line, "opportunity-id"),
        "at": field(line, "at"),
        "phase": field(line, "phase", r":([\w-]+)"),
        "transition": field(line, "transition", r":([\w-]+)"),
        "outcome": field(line, "outcome", r":([\w-]+)"),
        "ms": field(line, "duration-ms", r"(\d+)"),
        "attempt": field(line, "attempt-id"),
    }


def ts(s):
    return datetime.fromisoformat(s[:26].rstrip("Z") if "." in s[:26] else s.rstrip("Z"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-n", type=int, default=10, help="last N clicks")
    ap.add_argument("--since", help="only clicks starting at/after this ISO prefix")
    ap.add_argument("--tsv", action="store_true")
    ap.add_argument("--log", default=str(LOG))
    a = ap.parse_args()

    clicks = OrderedDict()
    with open(a.log, errors="replace") as f:
        for line in f:
            e = parse(line)
            if not e["opp"] or not e["phase"] or not e["at"]:
                continue
            c = clicks.setdefault(e["opp"], {"first": e["at"], "last": e["at"],
                                             "open": OrderedDict(), "done": []})
            c["last"] = e["at"]
            # start/end lines of one phase do not always carry the same
            # attempt id (bare "attempt-001" vs the ea1-...--attempt-001
            # form, or none), so pair them by phase name
            key = e["phase"]
            if e["transition"] == "start":
                c["open"][key] = e["at"]
            elif e["transition"] == "end":
                c["open"].pop(key, None)
                c["done"].append((e["phase"], e["attempt"], int(e["ms"] or 0), e["outcome"]))

    items = list(clicks.items())
    if a.since:
        items = [(k, v) for k, v in items if v["first"] >= a.since]
    items = items[-a.n:]

    if a.tsv:
        print("click_start\topportunity\tattempt\tphase\tseconds\toutcome")
    for opp, c in items:
        # an end line's :at can be stale (emit-phase! merges the context
        # over the fresh timestamp), so the span also trusts the
        # opportunity phase's own duration
        span = max([(ts(c["last"]) - ts(c["first"])).total_seconds()]
                   + [ms / 1000 for ph, _, ms, _ in c["done"] if ph == "opportunity"])
        if a.tsv:
            for ph, att, ms, out in c["done"]:
                print(f"{c['first'][:19]}\t{opp}\t{att}\t{ph}\t{ms/1000:.1f}\t{out}")
            for ph, at in c["open"].items():
                print(f"{c['first'][:19]}\t{opp}\t\t{ph}\t\tOPEN")
            continue
        print(f"{c['first'][:19]}Z  span {span:7.1f}s  {opp.split('/')[-1][:8]}")
        for ph, att, ms, out in c["done"]:
            flag = "" if out == "ok" else f"  <{out}>"
            print(f"    {ph:28s} {ms/1000:8.1f}s{flag}")
        for ph, at in c["open"].items():
            print(f"    {ph:28s}     OPEN  (started {at[11:19]}Z)")


if __name__ == "__main__":
    main()
