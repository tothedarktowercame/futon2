#!/usr/bin/env python3
"""wm_click_timing.py <attempt-dir> -- read-only timing report for one WM click.

PROOF-wm-works <1>1 <2>5: timing is REPORTING ONLY. No budget, no threshold,
no gate; exit 0 on success, 2 on bad arguments or an unreadable attempt.

Wall time is decomposed over the intervals between consecutive checkpoint
events (00N-*.edn, ordered by :event/sequence, timed by :recorded-at).
Agent wait is exactly these checkpoint pairs:
  dispatch -> build        the author's turn (author-wait)
  build    -> adjudication the reviewer's turn (reviewer-wait)
Machine time is every other interval. The checkpoints are written strictly in
sequence, so the intervals do not overlap; overlap is therefore 0 and
  wall = wait + machine - overlap
holds by construction (printed with the arithmetic so it can be checked).
Inside the author's-turn interval the machinery does a small amount of work
after the author finishes (build-resolution); this report attributes the whole
interval to agent wait, which OVERCOUNTS wait slightly and never undercounts
it -- noted in the output, not corrected by guessing sub-phase boundaries.
"""
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

WAIT_PAIRS = {
    ("dispatch", "build"): "author's turn (author-wait)",
    ("build", "adjudication"): "reviewer's turn (reviewer-wait)",
}


def parse_edn_event(path: Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    seq = re.search(r":event/sequence (\d+)", text)
    ctype = re.search(r":checkpoint/type :([a-z-]+)", text)
    recorded = re.search(r':recorded-at "([^"]+)"', text)
    if not (seq and ctype and recorded):
        return None
    ts = datetime.fromisoformat(recorded.group(1).replace("Z", "+00:00"))
    return {"seq": int(seq.group(1)), "type": ctype.group(1), "at": ts, "file": path.name}


def main(argv):
    if len(argv) != 2:
        print("usage: wm_click_timing.py <attempt-dir>", file=sys.stderr)
        return 2
    d = Path(argv[1])
    if not d.is_dir():
        print(f"not a directory: {d}", file=sys.stderr)
        return 2
    events = [e for e in (parse_edn_event(p) for p in sorted(d.glob("0*.edn"))) if e]
    if len(events) < 2:
        print("fewer than two timed checkpoints", file=sys.stderr)
        return 2
    events.sort(key=lambda e: e["seq"])

    wall = (events[-1]["at"] - events[0]["at"]).total_seconds()
    wait = 0.0
    print(f"attempt {d.name}: {len(events)} checkpoints "
          f"{events[0]['at'].isoformat()} -> {events[-1]['at'].isoformat()}")
    print(f"{'interval':<38}{'seconds':>10}  class")
    intervals = []
    for a, b in zip(events, events[1:]):
        seconds = (b["at"] - a["at"]).total_seconds()
        label = WAIT_PAIRS.get((a["type"], b["type"]))
        if label:
            wait += seconds
            intervals.append((f"{a['type']}->{b['type']} ({label})", seconds, "agent wait"))
        else:
            intervals.append((f"{a['type']}->{b['type']}", seconds, "machine"))
    for label, seconds, cls in intervals:
        print(f"{label:<38}{seconds:>10.1f}  {cls}")
    machine = wall - wait
    overlap = 0.0  # sequential :recorded-at; see docstring
    print()
    print(f"wall time    {wall:>10.1f} s")
    print(f"agent wait   {wait:>10.1f} s  (checkpoint pairs named above)")
    print(f"machine      {machine:>10.1f} s  (all other intervals; "
          f"author's-turn interval includes the build-resolution machine tail, "
          f"so wait is overcounted, never undercounted)")
    print(f"overlap      {overlap:>10.1f} s  (checkpoints are sequential)")
    print("note: in recorded clicks the reviewer's turn also falls inside the "
          "dispatch->build interval, so agent wait concentrates there and the "
          "build->adjudication pair is near zero; totals are unaffected.")
    print(f"check: wall = wait + machine - overlap -> "
          f"{wait:.1f} + {machine:.1f} - {overlap:.1f} = {wait + machine - overlap:.1f} "
          f"(wall {wall:.1f})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
